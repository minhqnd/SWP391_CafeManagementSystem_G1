package utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountValidationTest {
    @Test void trimsBrowserWhitespaceWithoutRemovingControlsOrChangingInternalText(){
        for (int space : new int[]{0x09,0x0A,0x0B,0x0C,0x0D,0x20,0xA0,0x1680,0x2000,0x2003,0x200A,0x2028,0x2029,0x202F,0x205F,0x3000,0xFEFF})
            assertEquals("QA",AccountValidation.text((char)space + "QA" + (char)space));
        String internal="Q" + " ".repeat(100000) + "A";
        assertEquals(internal,AccountValidation.text("\u00a0" + internal + "\ufeff"));
        assertEquals("\u0000QA\u0000",AccountValidation.text("\u0000QA\u0000"));
    }
    @Test void requiresMatchingConfirmationAndBoundsPasswords(){
        for(String bad:new String[]{null,"","12345","Abc1234","12345678","abcdefgh","!!!!!!!!","      ","x".repeat(256)})assertNotNull(AccountValidation.passwordError(bad,bad));
        assertNull(AccountValidation.passwordError("Abcd1234","Abcd1234"));
        assertNotNull(AccountValidation.passwordError("secret123",null));
        assertNotNull(AccountValidation.passwordError("secret123",""));
        assertNotNull(AccountValidation.passwordError("secret123","different"));
        assertNull(AccountValidation.passwordError(" secret123 "," secret123 "));
    }
    @Test void collectsBothPasswordErrorsAtOnce(){
        java.util.Map<String,String> errors=AccountValidation.passwordErrors("1234567","different","password","confirm_password");
        assertEquals(java.util.Set.of("password","confirm_password"),errors.keySet());
    }
    @Test void validatesContactsAndNormalizesConsistently(){
        assertEquals("qa@gmail.com",AccountValidation.email(" QA@GMAIL.COM "));
        assertEquals("0987111222",AccountValidation.phone("0987.111-222"));
        for(String bad:new String[]{"", "a@b", "a@@example.com",".a@example.com","a..b@example.com","a@-bad.com","a@bad_.com","a".repeat(151)+"@example.com"})assertNotNull(AccountValidation.emailError(bad),bad);
        assertNull(AccountValidation.emailError("qa+test@gmail.com"));
        assertNull(AccountValidation.phoneError("0987111222"));
        assertNotNull(AccountValidation.phoneError("12345"));
        for(String invalid:new String[]{"098711122","09871112223","09871112a2","+84987111222"})
            assertNotNull(AccountValidation.phoneError(invalid),invalid);
    }
    @Test void acceptsOnlyExactGmailDomainAndKeepsLengthErrorsSeparate(){
        for(String bad:new String[]{"nguyen@gmaill.comm","nguyen@gmail.comm","nguyen@gmaill.com",
                "nguyen@gmail.com.evil.com","nguyen@sub.gmail.com","nguyen@yahoo.com",
                "nguyen@gmailcom","nguyen@@gmail.com",".nguyen@gmail.com","nguyen..a@gmail.com","nguyen@gmail.com\n"})
            assertEquals("Sai định dạng email.",AccountValidation.emailError(bad),bad);
        assertNull(AccountValidation.emailError(AccountValidation.email(" NGUYEN@GMAIL.COM ")));
        assertNull(AccountValidation.emailError("a".repeat(140)+"@gmail.com"));
        assertEquals("Email tối đa 150 ký tự.",AccountValidation.emailError("a".repeat(141)+"@gmail.com"));
    }
    @Test void rejectsOverlongNameInvalidDateAndFutureBirthday(){
        assertNotNull(AccountValidation.nameError(""));assertNotNull(AccountValidation.nameError("x".repeat(121)));
        assertNull(AccountValidation.nameError("Nguyễn Quốc Nguyên"));
        assertNotNull(AccountValidation.birthdayError("2025-02-30"));
        assertNotNull(AccountValidation.birthdayError("2999-01-01"));
        assertNotNull(AccountValidation.birthdayError("1899-01-01"));
        assertNull(AccountValidation.birthdayError("2000-02-29"));assertNull(AccountValidation.birthdayError(""));
    }
    @Test void hashesWithUniqueSaltAndPreservesPasswordCharacters(){
        String one=PasswordHasher.hash(" secret123 "),two=PasswordHasher.hash(" secret123 ");
        assertNotEquals(one,two);assertTrue(PasswordHasher.verify(" secret123 ",one));
        assertFalse(PasswordHasher.verify("secret123",one));assertFalse(PasswordHasher.verify("bad",one));
        assertFalse(PasswordHasher.verify("demo_hash","demo_hash"));assertFalse(PasswordHasher.verify("pending_hash","pending_hash"));
        assertFalse(PasswordHasher.verify("secret123","pbkdf2_sha256$999999999$broken$broken"));
    }
}
