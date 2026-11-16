package controller;

import jakarta.servlet.http.HttpServletRequest;
import model.PurchaseOrderItem;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminPurchaseControllerTest {
    private HttpServletRequest request(String[] ids,String[] quantities,String[] totals) {
        Map<String,String[]> values=new HashMap<>();
        values.put("ingredientId",ids);values.put("quantity",quantities);values.put("totalPrice",totals);
        return (HttpServletRequest)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HttpServletRequest.class},
                (proxy,method,args)->"getParameterValues".equals(method.getName())?values.get(args[0]):null);
    }
    @Test void parsesMultipleIngredientsAndPreservesEachEnteredTotal() {
        List<PurchaseOrderItem> items=AdminPurchaseController.parseItems(request(
                new String[]{"1","2"},new String[]{"3000","2.5"},new String[]{"100000","20000.01"}));
        assertEquals(List.of(1,2),items.stream().map(PurchaseOrderItem::getIngredientId).toList());
        assertEquals(new BigDecimal("100000.00"),items.get(0).getSubtotal());
        assertEquals(new BigDecimal("20000.01"),items.get(1).getSubtotal());
    }
    @Test void rejectsMissingMisalignedEmptyAndOversizedArrays() {
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(null,null,null)));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[0],new String[0],new String[0])));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[]{"1","2"},new String[]{"1"},new String[]{"10"})));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[101],new String[101],new String[101])));
    }
    @Test void rejectsDuplicateInvalidAndNonNumericValues() {
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[]{"1","1"},new String[]{"1","2"},new String[]{"10","20"})));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[]{"0"},new String[]{"1"},new String[]{"10"})));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[]{"1"},new String[]{"abc"},new String[]{"10"})));
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(new String[]{"1"},new String[]{"1"},new String[]{"0.001"})));
    }
    @Test void rejectsCombinedTotalBeyondDatabaseMoneyRange() {
        assertThrows(IllegalArgumentException.class,()->AdminPurchaseController.parseItems(request(
                new String[]{"1","2"},new String[]{"1","1"},new String[]{"9999999999.99","0.01"})));
    }
}
