package dao;

import model.ShopSettings;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LoyaltyPersistenceTest {
    @Test void subtractingExactlyTheBalanceCommitsZeroAndOneHistoryRow() throws Exception {
        Store store=new Store(10);
        assertTrue(store.loyalty().adjustPoints(7,-10));
        assertEquals(0,store.pendingBalance);assertEquals(-10,store.historyDelta);
        assertEquals(1,store.commits);assertEquals(0,store.rollbacks);
    }

    @Test void insufficientBalanceAndOverflowNeverUpdateOrInsert() {
        Store insufficient=new Store(10);
        assertThrows(IllegalArgumentException.class,()->insufficient.loyalty().adjustPoints(7,-11));
        assertEquals(0,insufficient.writes);assertEquals(1,insufficient.rollbacks);
        Store zero=new Store(0);
        assertThrows(IllegalArgumentException.class,()->zero.loyalty().adjustPoints(7,-1));
        assertEquals(0,zero.writes);assertEquals(1,zero.rollbacks);
        Store full=new Store(Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class,()->full.loyalty().adjustPoints(7,1));
        assertEquals(0,full.writes);assertEquals(1,full.rollbacks);
    }

    @Test void exactMaximumBalanceAndMaximumAdjustmentAreAllowed() throws Exception {
        Store almostFull=new Store(Integer.MAX_VALUE-1);
        assertTrue(almostFull.loyalty().adjustPoints(7,1));assertEquals(Integer.MAX_VALUE,almostFull.pendingBalance);
        Store empty=new Store(0);assertTrue(empty.loyalty().adjustPoints(7,1_000_000));
        assertEquals(1_000_000,empty.pendingBalance);
    }

    @Test void historyFailureRollsBackTheBalanceChange() {
        Store store=new Store(10);store.failHistory=true;
        assertThrows(SQLException.class,()->store.loyalty().adjustPoints(7,5));
        assertEquals(0,store.commits);assertEquals(1,store.rollbacks);
        assertEquals(10,store.committedBalance);
    }

    @Test void missingCustomerAndNoUpdatedRowCannotReportSuccess() {
        Store missing=new Store(0);missing.exists=false;
        assertThrows(IllegalArgumentException.class,()->missing.loyalty().adjustPoints(7,1));
        assertEquals(0,missing.writes);
        Store disappeared=new Store(0);disappeared.updateCount=0;
        assertThrows(SQLException.class,()->disappeared.loyalty().adjustPoints(7,1));
        assertEquals(1,disappeared.rollbacks);assertEquals(0,disappeared.commits);
    }

    @Test void invalidDaoArgumentsNeverConnectToDatabase() {
        Store store=new Store(0);
        for(int points:new int[]{0,1_000_001,-1_000_001,Integer.MIN_VALUE,Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class,()->store.loyalty().adjustPoints(7,points));
        assertThrows(IllegalArgumentException.class,()->store.loyalty().adjustPoints(0,1));
        assertEquals(0,store.connections);
    }

    @Test void loyaltyRulesWriteExactlyThreeKeysAndCommitTogether() {
        Store store=new Store(0);
        assertTrue(store.settings().saveLoyaltyRules(100_000_000,1_000_000,0));
        assertEquals(Set.of(ShopSettings.KEY_POINTS_EARN_UNIT,ShopSettings.KEY_POINTS_STEP,
                ShopSettings.KEY_POINTS_STEP_VALUE),store.keys);
        assertEquals(3,store.batchRows);assertEquals(1,store.commits);
    }

    @Test void rulesBatchFailureRollsBackAndReturnsFalse() {
        Store store=new Store(0);store.failBatch=true;
        assertFalse(store.settings().saveLoyaltyRules(1,1,0));
        assertEquals(0,store.commits);assertEquals(1,store.rollbacks);
    }

    @Test void rulesDaoAlsoRejectsOutOfRangeInputsBeforeConnecting() {
        Store store=new Store(0);
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(0,1,0));
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(100_000_001,1,0));
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(1,0,0));
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(1,1_000_001,0));
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(1,1,-1));
        assertThrows(IllegalArgumentException.class,()->store.settings().saveLoyaltyRules(1,1,100_000_001));
        assertEquals(0,store.connections);
    }

    private static final class Store {
        int committedBalance,pendingBalance,historyDelta,commits,rollbacks,writes,connections,batchRows;
        int updateCount=1;
        boolean exists=true,failHistory,failBatch;
        Set<String> keys=new HashSet<>();
        Store(int balance){committedBalance=balance;pendingBalance=balance;}
        LoyaltyTransactionDAO loyalty(){return new LoyaltyTransactionDAO(){@Override protected Connection getConnection(){connections++;return connection();}};}
        ShopSettingDAO settings(){return new ShopSettingDAO(){@Override protected Connection getConnection(){connections++;return connection();}};}
        Connection connection(){return proxy(Connection.class,(p,m,a)->{
            switch(m.getName()){
                case "getAutoCommit":return true;
                case "prepareStatement":return statement((String)a[0]);
                case "commit":commits++;committedBalance=pendingBalance;return null;
                case "rollback":rollbacks++;pendingBalance=committedBalance;return null;
                default:return null;
            }
        });}
        PreparedStatement statement(String sql){
            Map<Integer,Object> values=new HashMap<>();
            return proxy(PreparedStatement.class,(p,m,a)->{
                switch(m.getName()){
                    case "setInt":case "setString":values.put((Integer)a[0],a[1]);return null;
                    case "executeQuery":
                        assertTrue(sql.contains("FOR UPDATE"));
                        boolean[] first={true};
                        return proxy(ResultSet.class,(x,y,z)->{
                            if(y.getName().equals("next")){boolean result=first[0]&&exists;first[0]=false;return result;}
                            if(y.getName().equals("getInt"))return committedBalance;
                            return null;
                        });
                    case "executeUpdate":
                        writes++;
                        if(sql.startsWith("UPDATE users")){pendingBalance=(Integer)values.get(1);return updateCount;}
                        if(failHistory)throw new SQLException("history insert failure");
                        historyDelta=(Integer)values.get(2);return 1;
                    case "addBatch":keys.add((String)values.get(1));batchRows++;return null;
                    case "executeBatch":if(failBatch)throw new SQLException("batch failure");return new int[]{1,1,1};
                    default:return null;
                }
            });
        }
    }
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,InvocationHandler handler){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);
    }
}
