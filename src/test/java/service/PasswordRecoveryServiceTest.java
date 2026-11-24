package service;

import dao.UserDAO;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import static org.junit.jupiter.api.Assertions.*;

class PasswordRecoveryServiceTest {
    private final AtomicLong now=new AtomicLong(1000000L);
    private ServletMocks mock;private PasswordRecoveryService service;
    private String delivered,resetEmail;private int resetId,sends;private boolean mailWorks=true,resetWorks=true;
    private final AtomicInteger updates=new AtomicInteger();
    @BeforeEach void setup(){
        mock=new ServletMocks();
        UserDAO users=new UserDAO(){
            @Override public User findActiveAccountByEmail(String email){return new User(email.startsWith("a@")?1:2,"QA","0987111222",email,"CUSTOMER",null,"ACTIVE");}
            @Override public boolean resetPasswordById(int id,String email,String password){updates.incrementAndGet();resetId=id;resetEmail=email;return resetWorks;}
        };
        service=new PasswordRecoveryService(users,(email,code)->{sends++;delivered=code;return mailWorks;},now::get);
    }
    private void start(){service.start(mock.session,"a@gmail.com","ip");}
    private void verified(){start();service.verify(mock.session,delivered);}
    @Test void rejectsMistypedEmailBeforeLookupOrSendingMail(){
        PasswordRecoveryService guarded=new PasswordRecoveryService(new UserDAO(){
            @Override public User findActiveAccountByEmail(String email){fail("Invalid email reached lookup");return null;}
        },(email,code)->{fail("Invalid email sent OTP");return true;},now::get);
        for(String email:new String[]{"nguyen@gmaill.comm","nguyen@gmail.comm","nguyen@yahoo.com"})
            assertEquals("Sai định dạng email.",assertThrows(IllegalArgumentException.class,()->guarded.start(mock.session,email,"ip")).getMessage());
    }
    @Test void bindsResetToVerifiedAccountAndConsumesGrant(){
        verified();mock.sessionValues.put("forgotEmail","b@gmail.com");
        service.reset(mock.session,"newSecret1","newSecret1");
        assertEquals(1,resetId);assertEquals("a@gmail.com",resetEmail);
        assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret2","newSecret2"));assertEquals(1,updates.get());
    }
    @Test void newTargetRevokesPreviousGrant(){
        verified();service.start(mock.session,"b@gmail.com","ip");
        assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));assertEquals(0,updates.get());
    }
    @Test void rejectedNewRequestAlsoRevokesGrant(){
        verified();assertThrows(IllegalArgumentException.class,()->service.start(mock.session,"a@gmail.com","ip"));
        assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));assertEquals(0,updates.get());
    }
    @Test void expiresOtpAtFiveMinutes(){start();now.addAndGet(PasswordRecoveryService.TTL);assertThrows(IllegalArgumentException.class,()->service.verify(mock.session,delivered));}
    @Test void expiresVerifiedGrantSeparately(){verified();now.addAndGet(PasswordRecoveryService.TTL);assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));assertEquals(0,updates.get());}
    @Test void locksAfterFiveWrongAttempts(){
        start();String code=delivered;
        String wrong = "000000".equals(code) ? "111111" : "000000";
        for(int i=0;i<5;i++)assertThrows(IllegalArgumentException.class,()->service.verify(mock.session,wrong));
        assertThrows(IllegalArgumentException.class,()->service.verify(mock.session,code));assertEquals(0,updates.get());
    }
    @Test void resendAndInitialRequestsShareCooldownAcrossSessions(){
        start();assertThrows(IllegalArgumentException.class,()->service.resend(mock.session,"ip"));
        assertThrows(IllegalArgumentException.class,()->service.start(new ServletMocks().session,"a@gmail.com","other-ip"));
        assertEquals(1,sends);now.addAndGet(60000);service.resend(mock.session,"ip");assertEquals(2,sends);
    }
    @Test void resendRevokesVerifiedGrant(){verified();now.addAndGet(60000);service.resend(mock.session,"ip");assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));}
    @Test void mailFailureNeverExposesCodeOrCreatesGrant(){
        mailWorks=false;IllegalArgumentException e=assertThrows(IllegalArgumentException.class,this::start);
        assertFalse(e.getMessage().contains(delivered));assertThrows(IllegalArgumentException.class,()->service.verify(mock.session,delivered));
    }
    @Test void invalidPasswordDoesNotConsumeValidGrant(){
        verified();assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"short","short"));assertEquals(0,updates.get());
        service.reset(mock.session,"newSecret1","newSecret1");assertEquals(1,updates.get());
    }
    @Test void lockedOrChangedAccountCannotReuseGrant(){verified();resetWorks=false;assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));resetWorks=true;assertThrows(IllegalArgumentException.class,()->service.reset(mock.session,"newSecret1","newSecret1"));assertEquals(1,updates.get());}
    @Test void parallelResetIsAppliedOnce()throws Exception{
        verified();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        Callable<Boolean> action=()->{gate.await();try{service.reset(mock.session,"newSecret1","newSecret1");return true;}catch(IllegalArgumentException e){return false;}};
        try{Future<Boolean> a=pool.submit(action),b=pool.submit(action);gate.countDown();assertNotEquals(a.get(3,TimeUnit.SECONDS),b.get(3,TimeUnit.SECONDS));assertEquals(1,updates.get());}finally{pool.shutdownNow();}
    }
    @Test void limitsRequestsFromOneSource(){
        for(int i=0;i<10;i++)service.start(new ServletMocks().session,"qa"+i+"@gmail.com","same-ip");
        assertThrows(IllegalArgumentException.class,()->service.start(new ServletMocks().session,"extra@gmail.com","same-ip"));assertEquals(10,sends);
    }
    @Test void malformedOtpHasFormatMessageWithoutConsumingAttempts(){
        start();
        for(int i=0;i<10;i++)assertEquals("Mã OTP phải gồm đúng 6 chữ số.",
                assertThrows(IllegalArgumentException.class,()->service.verify(mock.session,"abc")).getMessage());
        service.verify(mock.session,delivered);
        assertEquals("reset_password",service.nextTab(mock.session));
    }
    @Test void missingOtpIsRequiredAndDoesNotConsumeAttempts(){
        start();
        for (String value : new String[]{null, "", "   "}) {
            assertEquals("Vui lòng nhập mã OTP.", assertThrows(IllegalArgumentException.class,
                    () -> service.verify(mock.session, value)).getMessage());
        }
        service.verify(mock.session, delivered);
        assertEquals("reset_password", service.nextTab(mock.session));
    }
    @Test void expiredOtpReturnsToEmailEntryAndAllowsANewRequest(){
        start();now.addAndGet(PasswordRecoveryService.TTL);
        assertEquals("forgot",service.nextTab(mock.session));
        service.start(mock.session,"a@gmail.com","ip");
        assertEquals("otp",service.nextTab(mock.session));
    }
}
