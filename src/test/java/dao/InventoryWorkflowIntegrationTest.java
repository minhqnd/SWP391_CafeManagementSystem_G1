package dao;

import model.*;
import model.PurchaseReceipt.Item;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: use only an isolated MySQL schema, never the application's configured DB. */
@EnabledIfSystemProperty(named="inventory.test.url",matches=".+")
class InventoryWorkflowIntegrationTest {
    Connection connection()throws SQLException{return DriverManager.getConnection(System.getProperty("inventory.test.url"),"root","");}
    RestockRequestDAO requests=new RestockRequestDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
    PurchaseOrderDAO orders=new PurchaseOrderDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
    PurchaseReceiptDAO receipts=new PurchaseReceiptDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
    int ingredient,barista,otherBarista;BigDecimal baseline=new BigDecimal("100");
    BigDecimal n(String value){return new BigDecimal(value);}
    @BeforeEach void setup()throws Exception {
        try(Connection c=connection();Statement s=c.createStatement()){
            s.executeUpdate("INSERT INTO ingredients(name,unit,current_stock,min_stock_level) VALUES('Integration stock','kg',100,1)",Statement.RETURN_GENERATED_KEYS);try(ResultSet r=s.getGeneratedKeys()){r.next();ingredient=r.getInt(1);}
            barista=user(s);otherBarista=user(s);
        }
    }
    int user(Statement s)throws SQLException {s.executeUpdate("INSERT INTO users(full_name,phone,password_hash,role,position,status) VALUES('Test Barista','"+UUID.randomUUID().toString().replace("-", "").substring(0,14)+"','demo','STAFF','BARISTA','ACTIVE')",Statement.RETURN_GENERATED_KEYS);try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getInt(1);}}
    int po()throws Exception {
        PurchaseOrder po=new PurchaseOrder();po.setSupplierId(1);po.setCreatorId(1);
        int id=orders.create(po,List.of(new PurchaseOrderItem(ingredient,n("10"),n("100"))));orders.changeStatus(id,1,"order","");return id;
    }
    int line(int po)throws Exception {try(Connection c=connection();PreparedStatement p=c.prepareStatement("SELECT id FROM purchase_order_items WHERE po_id=?")){p.setInt(1,po);try(ResultSet r=p.executeQuery()){r.next();return r.getInt(1);}}}
    int submit(int po,String delivered,String accepted)throws Exception {return receipts.submit(po,barista,UUID.randomUUID().toString(),"Kiểm hàng",List.of(new Item(line(po),n(delivered),n(accepted))));}
    BigDecimal scalar(String sql)throws Exception {try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getBigDecimal(1);}}
    String state(String table,int id)throws Exception {try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT status FROM "+table+" WHERE id="+id)){r.next();return r.getString(1);}}
    BigDecimal stock()throws Exception{return scalar("SELECT current_stock FROM ingredients WHERE id="+ingredient);}
    void stock(String expected)throws Exception {assertEquals(0,n(expected).compareTo(stock()));}
    @Test void partialThenSupplementOnlyAddsAcceptedAndRecordsRejected()throws Exception {
        int po=po(),r=submit(po,"10","8");stock("100");receipts.review(r,1,true,"2kg hỏng, chờ giao bù");stock("108");assertEquals("PARTIALLY_RECEIVED",state("purchase_orders",po));
        int second=submit(po,"2","2");receipts.review(second,1,true,"Giao bù đủ");stock("110");assertEquals("RECEIVED",state("purchase_orders",po));
        assertThrows(IllegalArgumentException.class,()->receipts.review(second,1,true,"Bấm lại"));stock("110");
    }
    @Test void rejectedReceiptDoesNotChangeStockAndCanBeReplaced()throws Exception {
        int po=po(),r=submit(po,"10","10");receipts.review(r,1,false,"Sai biên bản");stock("100");assertEquals("ORDERED",state("purchase_orders",po));
        int replacement=submit(po,"8","8");receipts.review(replacement,1,true,"Xác nhận");stock("108");
    }
    @Test void excessRequiresExplicitManagerAcceptanceAndRollsBack()throws Exception {
        int po=po(),r=submit(po,"12","12");assertThrows(IllegalArgumentException.class,()->receipts.review(r,1,true,"Thừa"));stock("100");assertEquals("PENDING",state("purchase_receipts",r));
        receipts.review(r,1,true,"Đồng ý nhận 2kg thừa",true);stock("112");
    }
    @Test void employeeCannotReviewAndCannotEditAnotherPersonsRequest()throws Exception {
        int id=requests.createRestockRequest(barista,"DRAFT","Sắp hết","",List.of(new RestockRequest.RestockItem(ingredient,"","",n("10"))));
        assertThrows(IllegalArgumentException.class,()->requests.edit(id,otherBarista,false,"Sửa","",List.of(new RestockRequest.RestockItem(ingredient,"","",n("8"))),"Sửa"));
        requests.transition(id,barista,false,"submit","");
        assertThrows(IllegalArgumentException.class,()->requests.transition(id,barista,false,"approve","Duyệt"));
        requests.transition(id,1,true,"requestChanges","Giảm số lượng");
        requests.edit(id,barista,false,"Mua đủ","",List.of(new RestockRequest.RestockItem(ingredient,"","",n("8"))),"Theo yêu cầu");requests.transition(id,barista,false,"submit","");
        requests.edit(id,1,true,"Đã kiểm tra","",List.of(new RestockRequest.RestockItem(ingredient,"","",n("7"))),"Quản lý sửa");requests.transition(id,1,true,"approve","Đồng ý");
        assertEquals("APPROVED",state("restock_requests",id));assertTrue(scalar("SELECT COUNT(*) FROM inventory_workflow_history WHERE entity_type='RESTOCK' AND entity_id="+id).intValue()>=7);
    }
    @Test void managerCannotApproveOwnRequestAndReasonIsRequired()throws Exception {
        int id=requests.createRestockRequest(1,"PENDING_APPROVAL","Mua","",List.of(new RestockRequest.RestockItem(ingredient,"","",n("10"))));
        assertThrows(IllegalArgumentException.class,()->requests.transition(id,1,true,"approve",""));
        int po=po(),r=submit(po,"1","1");assertThrows(IllegalArgumentException.class,()->receipts.review(r,1,false,""));assertThrows(IllegalArgumentException.class,()->receipts.review(r,barista,true,"Duyệt"));
    }
    @Test void correctionsKeepOriginalReceiptAndRejectOverReturn()throws Exception {
        int po=po(),r=submit(po,"10","10");receipts.review(r,1,true,"Đủ");
        int item=scalar("SELECT id FROM purchase_receipt_items WHERE receipt_id="+r).intValue();String key=UUID.randomUUID().toString();
        receipts.adjust(item,1,key,n("-2"),"Trả 2kg hỏng");stock("108");
        InventoryDAO inventory=new InventoryDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
        assertTrue(inventory.getRecentTransactions(1,"ADJUSTMENT").get(0).getFormattedQuantity().startsWith("-2"));receipts.adjust(item,1,key,n("-2"),"Bấm lại");stock("108");
        assertEquals("PARTIALLY_RECEIVED",state("purchase_orders",po));assertEquals(0,n("10").compareTo(scalar("SELECT accepted_quantity FROM purchase_receipt_items WHERE id="+item)));
        assertThrows(IllegalArgumentException.class,()->receipts.adjust(item,1,UUID.randomUUID().toString(),n("-9"),"Trả quá số nhận"));stock("108");
        receipts.adjust(item,1,UUID.randomUUID().toString(),n("2"),"Ghi thiếu 2kg");stock("110");
    }
    @Test void submissionIsIdempotentAndForeignLineIsRejected()throws Exception {
        int po=po();String key=UUID.randomUUID().toString();List<Item> items=List.of(new Item(line(po),n("3"),n("3")));
        assertEquals(receipts.submit(po,barista,key,"Giao",items),receipts.submit(po,barista,key,"Giao",items));
        int another=po();assertThrows(IllegalArgumentException.class,()->receipts.submit(another,barista,UUID.randomUUID().toString(),"Sai đơn",items));stock("100");
    }
    @Test void pendingReceiptMustBeResolvedBeforeClosingShortRemainder()throws Exception {
        int po=po(),r=submit(po,"8","8");assertThrows(IllegalArgumentException.class,()->orders.changeStatus(po,1,"close","Không giao nữa"));receipts.review(r,1,true,"Nhận 8");
        orders.changeStatus(po,1,"close","Không giao 2kg còn lại");assertEquals("CLOSED",state("purchase_orders",po));stock("108");assertThrows(IllegalArgumentException.class,()->submit(po,"2","2"));
    }
    @Test void inactiveSupplierCannotBeUsed()throws Exception {
        int supplier;try(Connection c=connection();Statement s=c.createStatement()){s.executeUpdate("INSERT INTO suppliers(name,status) VALUES('Inactive test','INACTIVE')",Statement.RETURN_GENERATED_KEYS);try(ResultSet r=s.getGeneratedKeys()){r.next();supplier=r.getInt(1);}}
        PurchaseOrder po=new PurchaseOrder();po.setSupplierId(supplier);po.setCreatorId(1);assertThrows(IllegalArgumentException.class,()->orders.create(po,List.of(new PurchaseOrderItem(ingredient,n("10"),n("1")))));
    }
    @Test void draftCanBeEditedButOrderedCannot()throws Exception {
        PurchaseOrder po=new PurchaseOrder();po.setSupplierId(1);po.setCreatorId(1);int id=orders.create(po,List.of(new PurchaseOrderItem(ingredient,n("10"),n("100"))));
        orders.editDraft(id,1,1,n("8"),n("800"),"","Giảm nhu cầu");assertEquals(0,n("8").compareTo(scalar("SELECT quantity FROM purchase_order_items WHERE po_id="+id)));
        orders.changeStatus(id,1,"order","");assertThrows(IllegalArgumentException.class,()->orders.editDraft(id,1,1,n("7"),n("700"),"","Sửa muộn"));
    }
    @Test void listAndCostReadsIncludePartialReceiptsAndHistory()throws Exception {
        int po=po(),id=submit(po,"6","5");receipts.review(id,1,true,"1kg từ chối");
        List<PurchaseOrder> list=orders.findAll("ALL");receipts.populateReceived(list);
        PurchaseOrder loaded=list.stream().filter(p->p.getId()==po).findFirst().orElseThrow();assertEquals(0,n("5").compareTo(loaded.getItems().get(0).getReceivedQuantity()));
        PurchaseReceipt receipt=receipts.findAll().stream().filter(r->r.getId()==id).findFirst().orElseThrow();assertEquals(0,n("1").compareTo(receipt.getItems().get(0).getRejected()));
        IngredientDAO ingredients=new IngredientDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
        assertEquals(0,n("100").compareTo(ingredients.getLatestPurchasePrices().get(ingredient)));
        InventoryWorkflowDAO history=new InventoryWorkflowDAO(){@Override protected Connection getConnection()throws SQLException{return connection();}};
        assertFalse(history.history("PURCHASE",null).isEmpty());
    }
    @Test void failedSecondStockMovementRollsBackWholeReceipt()throws Exception {
        int second;try(Connection c=connection();Statement s=c.createStatement()){
            s.executeUpdate("INSERT INTO ingredients(name,unit,current_stock,min_stock_level) VALUES('Full stock','kg',999999999.998,0)",Statement.RETURN_GENERATED_KEYS);try(ResultSet r=s.getGeneratedKeys()){r.next();second=r.getInt(1);}}
        PurchaseOrder order=new PurchaseOrder();order.setSupplierId(1);order.setCreatorId(1);
        int po=orders.create(order,List.of(new PurchaseOrderItem(ingredient,n("2"),n("1")),new PurchaseOrderItem(second,n("2"),n("1"))));orders.changeStatus(po,1,"order","");
        List<Item> items=orders.findItems(po).stream().map(i->new Item(i.getId(),n("2"),n("2"))).toList();int r=receipts.submit(po,barista,UUID.randomUUID().toString(),"Giao đủ",items);
        assertThrows(SQLException.class,()->receipts.review(r,1,true,"Kiểm tra rollback"));stock("100");assertEquals("PENDING",state("purchase_receipts",r));assertEquals(0,scalar("SELECT COUNT(*) FROM inventory_transactions WHERE reference_type='PURCHASE_ORDER' AND reference_id="+po).intValue());
    }
    @Test void correctionCannotReturnStockAlreadyConsumed()throws Exception {
        int po=po(),r=submit(po,"10","10");receipts.review(r,1,true,"Đủ");int item=scalar("SELECT id FROM purchase_receipt_items WHERE receipt_id="+r).intValue();
        try(Connection c=connection();Statement s=c.createStatement()){s.executeUpdate("UPDATE ingredients SET current_stock=1 WHERE id="+ingredient);}
        assertThrows(IllegalArgumentException.class,()->receipts.adjust(item,1,UUID.randomUUID().toString(),n("-2"),"Trả 2"));stock("1");assertEquals(0,scalar("SELECT COUNT(*) FROM purchase_receipt_adjustments WHERE receipt_item_id="+item).intValue());
    }
    @Test void concurrentApprovalOnlyCommitsOnce()throws Exception {
        int po=po(),id=submit(po,"4","4");ExecutorService pool=Executors.newFixedThreadPool(2);
        try{Callable<Boolean> task=()->{try{receipts.review(id,1,true,"Xác nhận");return true;}catch(IllegalArgumentException e){return false;}};
            List<Future<Boolean>> results=pool.invokeAll(List.of(task,task));int successes=0;for(Future<Boolean> result:results)if(result.get())successes++;assertEquals(1,successes);stock("104");
        }finally{pool.shutdownNow();}
    }
}
