package controller;

import model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerRoleAccessTest {

    @Test
    void onlyCustomerRoleCanUseCustomerCheckoutFlow() {
        assertTrue(CartController.isCustomer(user("CUSTOMER")));
        assertTrue(CartController.isCustomer(user("customer")));
        assertFalse(CartController.isCustomer(user("ADMIN")));
        assertFalse(CartController.isCustomer(user("STAFF")));
        assertFalse(CartController.isCustomer(user(null)));
        assertFalse(CartController.isCustomer(null));
    }

    private User user(String role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}
