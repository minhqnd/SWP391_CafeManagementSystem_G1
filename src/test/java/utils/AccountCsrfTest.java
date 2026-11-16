package utils;
import org.junit.jupiter.api.Test;
import support.ServletMocks;
import static org.junit.jupiter.api.Assertions.*;
class AccountCsrfTest {
    @Test void rejectsMissingWrongAndOldTokens()throws Exception{
        ServletMocks mock=new ServletMocks();
        assertFalse(AccountCsrf.require(mock.request,mock.response));assertEquals(403,mock.error);
        mock.csrf();assertTrue(AccountCsrf.require(mock.request,mock.response));
        AccountCsrf.rotate(mock.session);assertFalse(AccountCsrf.require(mock.request,mock.response));
        mock.csrf();assertTrue(AccountCsrf.require(mock.request,mock.response));
    }
}
