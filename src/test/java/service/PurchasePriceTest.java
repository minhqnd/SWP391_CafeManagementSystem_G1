package service;

import model.PurchaseOrderItem;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PurchasePriceTest {
    private void roundTrip(String quantity, String total) {
        BigDecimal qty=new BigDecimal(quantity), amount=new BigDecimal(total);
        BigDecimal unit=InventoryRules.purchaseUnitPrice(qty,amount);
        assertEquals(0,amount.compareTo(new PurchaseOrderItem(1,qty,unit).getSubtotal()));
        assertEquals(12,unit.scale());
        assertTrue(unit.precision()<=26);
    }
    @Test void preservesBatchTotalForFractionalUnitPrices() {
        roundTrip("3000","100000");
        roundTrip("3","100.00");
        roundTrip("7","0.01");
        roundTrip("2.555","20000.01");
    }
    @Test void preservesTotalsAtSupportedQuantityAndMoneyBounds() {
        roundTrip("999999999.999","0.01");
        roundTrip("999999999.999","9999999999.99");
        roundTrip("0.001","9999999999.99");
        roundTrip("3000","0");
    }
    @Test void rejectsInvalidMoneyAndQuantity() {
        for(String total:new String[]{"-1","0.001","10000000000.00"})
            assertThrows(IllegalArgumentException.class,()->InventoryRules.purchaseUnitPrice(BigDecimal.ONE,new BigDecimal(total)));
        for(String qty:new String[]{"0","-1","0.0001","1000000000"})
            assertThrows(IllegalArgumentException.class,()->InventoryRules.purchaseUnitPrice(new BigDecimal(qty),BigDecimal.ONE));
    }
}
