package controller;

import dao.LoyaltyTransactionDAO;
import dao.ShopSettingDAO;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ShopSettings;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class AdminLoyaltyControllerTest {
    private final Map<String, Object> sessionValues = new ConcurrentHashMap<>();
    private final Map<String, String> params = new HashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private final AtomicInteger adjustments = new AtomicInteger();
    private final AtomicInteger saves = new AtomicInteger();
    private int delta, customerId, phoneLookups, defaultLookups;
    private boolean failRules, failAdjustment;
    private long[] savedRules;
    private CountDownLatch entered, release;
    private AdminLoyaltyController controller;
    private HttpSession session;

    @BeforeEach void setup() {
        User admin = new User(); admin.setRole("ADMIN"); admin.setStatus("ACTIVE");
        sessionValues.put("user", admin);
        sessionValues.put("loyaltyCsrfToken", "csrf");
        sessionValues.put("loyaltyOperationToken", "operation");
        params.put("csrfToken", "csrf"); params.put("operationToken", "operation");
        params.put("action", "adjust"); params.put("customerId", "7");
        params.put("points", "10"); params.put("adjustType", "add");
        session = proxy(HttpSession.class, (p,m,a) -> {
            switch (m.getName()) {
                case "getAttribute": return sessionValues.get(a[0]);
                case "setAttribute": sessionValues.put((String)a[0], a[1]); return null;
                case "removeAttribute": sessionValues.remove(a[0]); return null;
                default: return null;
            }
        });
        LoyaltyTransactionDAO loyalty = new LoyaltyTransactionDAO() {
            @Override public boolean adjustPoints(int id, int change) throws SQLException {
                adjustments.incrementAndGet(); customerId = id; delta = change;
                if (entered != null) {
                    entered.countDown();
                    try { if (!release.await(5, TimeUnit.SECONDS)) throw new SQLException("timeout"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new SQLException(e); }
                }
                if (failAdjustment) throw new SQLException("private database details");
                return true;
            }
            @Override public long getTotalPoints() { return 0; }
            @Override public long countCustomersWithPoints() { return 0; }
            @Override public long countTransactions() { return 0; }
            @Override public List<model.LoyaltyTransaction> findRecent(int limit) { return List.of(); }
            @Override public User findDefaultCustomer() { defaultLookups++; return null; }
            @Override public User findCustomerByPhone(String phone) { phoneLookups++; return null; }
        };
        ShopSettingDAO settings = new ShopSettingDAO() {
            @Override public ShopSettings getSettings() { return new ShopSettings(); }
            @Override public boolean saveSettings(ShopSettings s) { fail("Must not save unrelated settings"); return false; }
            @Override public boolean saveLoyaltyRules(long earn, int step, long value) {
                saves.incrementAndGet(); savedRules = new long[]{earn,step,value}; return !failRules;
            }
        };
        ServletContext context = proxy(ServletContext.class, (p,m,a) -> null);
        controller = new AdminLoyaltyController(loyalty, settings) {
            @Override public ServletContext getServletContext() { return context; }
        };
    }

    @Test void acceptsAdjustmentBoundariesAndSubtraction() throws Exception {
        post(); assertEquals(10,delta); assertEquals(7,customerId);
        renew(); params.put("points","1"); params.put("adjustType","subtract");
        post(); assertEquals(-1,delta);
        renew(); params.put("points","1000000"); params.put("customerId","2147483647");
        post(); assertEquals(-1_000_000,delta); assertEquals(Integer.MAX_VALUE,customerId);
    }

    @Test void rejectsEveryInvalidNumericBoundaryBeforeDao() throws Exception {
        for (String field : List.of("points","customerId")) {
            String original=params.get(field);
            for (String raw : List.of("", "-1", "-4294967295", "-9223372036854775808", "0",
                    "2147483648", "9223372036854775808", "1.5", "1e2", "NaN", "+1", "abc")) {
                renew(); params.put(field,raw); post();
                assertEquals(0,adjustments.get(),field+"="+raw);
                assertNotNull(sessionValues.get("loyaltyError"));
            }
            params.put(field,original);
        }
        renew(); params.put("points","1000001"); post(); assertEquals(0,adjustments.get());
    }

    @Test void rejectsInvalidActionAndAdjustmentType() throws Exception {
        params.put("adjustType","multiply"); post(); assertEquals(0,adjustments.get());
        renew(); params.put("action","unknown"); post(); assertEquals(0,adjustments.get());
    }

    @Test void rejectsNonAdminInactiveAndBadTokens() throws Exception {
        params.put("csrfToken","wrong"); post(); assertEquals(0,adjustments.get());
        params.put("csrfToken","csrf"); params.put("operationToken","wrong"); post();
        assertEquals(0,adjustments.get());
        params.put("operationToken","operation");
        User u=(User)sessionValues.get("user"); u.setRole("CUSTOMER"); post();
        u.setRole("ADMIN"); u.setStatus("INACTIVE"); post();
        sessionValues.remove("user"); post(); assertEquals(0,adjustments.get());
    }

    @Test void sameTokenCannotAdjustTwiceEvenWhileFirstRequestIsStillWriting() throws Exception {
        entered=new CountDownLatch(1); release=new CountDownLatch(1);
        ExecutorService executor=Executors.newSingleThreadExecutor();
        try {
            Future<?> first=executor.submit(() -> { try { post(); } catch(Exception e) { throw new RuntimeException(e); } });
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            post(); assertEquals(1,adjustments.get());
            release.countDown(); first.get(5,TimeUnit.SECONDS);
            post(); assertEquals(1,adjustments.get());
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test void validatesAllRuleFieldsAndKeepsDraftAfterRedirect() throws Exception {
        params.put("action","updateRules"); params.put("pointsEarnUnit","0");
        params.put("pointsStep","1000001"); params.put("pointsStepValue","-1");
        post(); assertEquals(0,saves.get());
        assertEquals(3,((Map<?,?>)sessionValues.get("loyaltyFieldErrors")).size());
        controller.doGet(request(),response());
        assertEquals("0",((Map<?,?>)attributes.get("loyaltyRulesDraft")).get("pointsEarnUnit"));
        assertFalse(sessionValues.containsKey("loyaltyRulesDraft"));
    }

    @Test void acceptsRuleBoundariesAndOnlyCallsDedicatedLoyaltySave() throws Exception {
        params.put("action","updateRules"); params.put("pointsEarnUnit","1");
        params.put("pointsStep","1"); params.put("pointsStepValue","0"); post();
        assertArrayEquals(new long[]{1,1,0},savedRules);
        renew(); params.put("pointsEarnUnit","100000000"); params.put("pointsStep","1000000");
        params.put("pointsStepValue","100000000"); post();
        assertArrayEquals(new long[]{100000000,1000000,100000000},savedRules);
    }

    @Test void rulesOverMaximumAndMalformedDoNotSave() throws Exception {
        params.put("action","updateRules");
        for(String field:List.of("pointsEarnUnit","pointsStep","pointsStepValue")) {
            for(String value:List.of("", "-4294967295", "100000001", "1.5", "1e2", "9223372036854775808")) {
                renew(); params.put("pointsEarnUnit","10000");params.put("pointsStep","10");params.put("pointsStepValue","2000");
                params.put(field,value);post();assertEquals(0,saves.get(),field+"="+value);
            }
        }
    }

    @Test void ruleDatabaseFailureKeepsInputAndNoSuccessMessage() throws Exception {
        failRules=true; params.put("action","updateRules");params.put("pointsEarnUnit","12345");
        params.put("pointsStep","20"); params.put("pointsStepValue","3000");post();
        assertNotNull(sessionValues.get("loyaltyRulesDraft"));
        assertNull(sessionValues.get("loyaltyMessage"));assertNotNull(sessionValues.get("loyaltyError"));
    }

    @Test void invalidPhoneNeverQueriesCustomerOrSelectsDefaultCustomer() throws Exception {
        for(String raw:List.of("", "01111111111", "1234567890", "09123", "abc", "0".repeat(100))) {
            params.put("phone",raw);controller.doGet(request(),response());
            assertEquals("Số điện thoại sai định dạng.",attributes.get("lookupError"));
        }
        assertEquals(0,phoneLookups);assertEquals(0,defaultLookups);
        params.put("phone","(096) 591-6512");controller.doGet(request(),response());assertEquals(1,phoneLookups);
    }

    @Test void databaseErrorDoesNotLeakSqlDetails() throws Exception {
        failAdjustment=true;post();
        assertFalse(sessionValues.get("loyaltyError").toString().contains("private database details"));
        assertNull(sessionValues.get("loyaltyMessage"));
    }

    private void renew(){String token=UUID.randomUUID().toString();sessionValues.put("loyaltyOperationToken",token);params.put("operationToken",token);}
    private void post() throws Exception { controller.doPost(request(),response()); }
    private HttpServletRequest request(){return proxy(HttpServletRequest.class,(p,m,a)->{
        switch(m.getName()){
            case "getSession":return session;
            case "getParameter":return params.get(a[0]);
            case "getContextPath":return "/cafe";
            case "setAttribute":attributes.put((String)a[0],a[1]);return null;
            case "getRequestDispatcher":return proxy(RequestDispatcher.class,(x,y,z)->null);
            default:return null;
        }
    });}
    private HttpServletResponse response(){return proxy(HttpServletResponse.class,(p,m,a)->null);}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,InvocationHandler h){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);}
}
