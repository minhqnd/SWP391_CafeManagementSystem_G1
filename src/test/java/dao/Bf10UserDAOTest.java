package dao;

import java.sql.*;
import java.util.*;
import model.User;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import utils.PasswordHasher;
import static org.junit.jupiter.api.Assertions.*;

class Bf10UserDAOTest {
    private final Map<String,Map<Integer,Object>> bindings=new LinkedHashMap<>();
    private final List<String> writes=new ArrayList<>();
    private String stored=PasswordHasher.hash("oldSecret123");
    private boolean duplicate=false;private int accountRows=1,commits,rollbacks;
    private SQLException writeFailure;
    private UserDAO dao(){return new UserDAO(){
        @Override protected Connection getConnection(){return ServletMocks.proxy(Connection.class,(p,m,a)->{
            if(m.getName().equals("commit")){commits++;return null;}
            if(m.getName().equals("rollback")){rollbacks++;return null;}
            if(!m.getName().equals("prepareStatement"))return null;
            String sql=(String)a[0];Map<Integer,Object> args=new HashMap<>();bindings.put(sql,args);
            return ServletMocks.proxy(PreparedStatement.class,(sp,sm,sa)->{
                if(sm.getName().startsWith("set") && sa!=null && sa.length>=2){args.put((Integer)sa[0],sa[1]);return null;}
                if(sm.getName().equals("executeUpdate")){if(writeFailure!=null)throw writeFailure;writes.add(sql);return 1;}
                if(sm.getName().equals("executeQuery")){
                    int[] cursor={0};int rows=sql.contains("id<>?")?(duplicate?1:0):accountRows;
                    return ServletMocks.proxy(ResultSet.class,(rp,rm,ra)->switch(rm.getName()){
                        case "next"->++cursor[0]<=rows;
                        case "getInt"->7;
                        case "getString"->{
                            if(ra[0] instanceof Integer)yield stored;
                            yield switch(ra[0].toString()){
                                case "password_hash"->stored;case "status"->"ACTIVE";case "role"->"CUSTOMER";
                                case "email"->"qa@example.com";case "phone"->"0987111222";case "full_name"->"QA";default->null;
                            };
                        }
                        case "getMetaData"->ServletMocks.proxy(ResultSetMetaData.class,(mp,mm,ma)->mm.getName().equals("getColumnCount")?0:null);
                        default->null;
                    });
                }
                return null;
            });
        });}
    };}
    @Test void registrationWritesSaltedHashInsteadOfRawPassword(){
        User user=new User(0,"QA","0987111222","qa@example.com","CUSTOMER",null,"ACTIVE");
        assertTrue(dao().register(user," secret123 "));
        String encoded=(String)bindings.values().iterator().next().get(6);
        assertTrue(PasswordHasher.encoded(encoded));assertTrue(PasswordHasher.verify(" secret123 ",encoded));assertNotEquals(" secret123 ",encoded);
    }
    @Test void resetBindsUserIdEmailAndActiveStatus(){
        assertTrue(dao().resetPasswordById(7," QA@EXAMPLE.COM ","newSecret123"));
        String sql=writes.get(0);assertTrue(sql.contains("id=?"));assertTrue(sql.contains("email"));assertTrue(sql.contains("status='ACTIVE'"));
        assertEquals(7,bindings.get(sql).get(2));assertEquals("qa@example.com",bindings.get(sql).get(3));assertTrue(PasswordHasher.verify("newSecret123",(String)bindings.get(sql).get(1)));
    }
    @Test void ambiguousRecoveryEmailFailsClosed(){accountRows=2;assertThrows(utils.RecoveryFailure.class,()->dao().findActiveAccountByEmail("qa@example.com"));assertTrue(writes.isEmpty());}
    @Test void existingRecoveryEmailResolvesItsAccountWithoutRegistrationDuplicateChecks(){
        User user=dao().findActiveAccountByEmail("qa@example.com");
        assertNotNull(user);assertEquals(7,user.getId());assertEquals("qa@example.com",user.getEmail());
        assertTrue(writes.isEmpty());
    }
    @Test void databaseFailureIsNotReportedAsMissingEmail(){
        UserDAO unavailable=new UserDAO(){@Override protected Connection getConnection()throws SQLException{throw new SQLException("Mock unavailable database");}};
        java.util.logging.Logger logger=java.util.logging.Logger.getLogger(UserDAO.class.getName());
        boolean parents=logger.getUseParentHandlers();
        List<java.util.logging.LogRecord> logged=new ArrayList<>();
        java.util.logging.Handler capture=new java.util.logging.Handler(){
            @Override public void publish(java.util.logging.LogRecord record){logged.add(record);}
            @Override public void flush(){}
            @Override public void close(){}
        };
        logger.setUseParentHandlers(false);logger.addHandler(capture);
        try{
            utils.RecoveryFailure error=assertThrows(utils.RecoveryFailure.class,()->unavailable.findActiveAccountByEmail("qa@example.com"));
            assertNull(error.field());assertFalse(error.getMessage().contains("Không tìm thấy"));
            assertEquals(1,logged.size());assertInstanceOf(SQLException.class,logged.get(0).getThrown());
        }finally{logger.removeHandler(capture);logger.setUseParentHandlers(parents);}
    }
    @Test void duplicateProfileEmailDoesNotWrite(){duplicate=true;assertNotEquals("success",dao().updateUserProfile(7,"QA","0987111222","other@example.com","",""));assertTrue(writes.isEmpty());}
    @Test void profileOnlyReportsDuplicateContactForAnActualDuplicateKey(){
        java.util.logging.Logger logger=java.util.logging.Logger.getLogger(UserDAO.class.getName());
        boolean parents=logger.getUseParentHandlers();logger.setUseParentHandlers(false);
        try {
            writeFailure=new SQLIntegrityConstraintViolationException("private SQL", "23000", 1062);
            assertTrue(dao().updateUserProfile(7,"QA","0987111222","qa@gmail.com","","").contains("Thông tin liên hệ"));
            for(int code:new int[]{1048,1452}) {
                writeFailure=new SQLIntegrityConstraintViolationException("private SQL", "23000", code);
                assertEquals("Không thể lưu hồ sơ. Vui lòng thử lại sau.",dao().updateUserProfile(7,"QA","0987111222","qa@gmail.com","",""));
            }
            assertTrue(writes.isEmpty());
        } finally {logger.setUseParentHandlers(parents);}
    }
    @Test void passwordChangeVerifiesAndLocksCurrentUserBeforeWriting(){
        assertTrue(dao().changePasswordById(7,"oldSecret123","newSecret123"));assertEquals(1,commits);
        assertTrue(bindings.keySet().stream().anyMatch(s->s.contains("FOR UPDATE")));
        String sql=writes.get(0);assertFalse(sql.contains("email"));assertEquals(7,bindings.get(sql).get(2));assertTrue(PasswordHasher.verify("newSecret123",(String)bindings.get(sql).get(1)));
    }
    @Test void wrongCurrentPasswordRollsBackWithoutWriting(){assertFalse(dao().changePasswordById(7,"wrong","newSecret123"));assertTrue(writes.isEmpty());assertEquals(1,rollbacks);}
    @Test void successfulLegacyLoginUpgradesOnlyTheMatchedUser(){
        stored="legacySecret123";assertNotNull(dao().login("qa@example.com","legacySecret123"));
        String sql=writes.get(0);assertTrue(sql.contains("id=? AND password_hash=?"));assertEquals(7,bindings.get(sql).get(2));assertEquals(stored,bindings.get(sql).get(3));assertTrue(PasswordHasher.encoded((String)bindings.get(sql).get(1)));
    }
    @Test void placeholdersAndStoredHashCannotBeUsedAsPasswords(){
        for(String marker:new String[]{"demo_hash","pending_hash"}){stored=marker;assertNull(dao().login("qa@example.com",marker));}
        stored=PasswordHasher.hash("secret123");assertNull(dao().login("qa@example.com",stored));assertTrue(writes.isEmpty());
    }
}
