package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class WalletServiceTest {
    @Test
    void depositCreditsTheWallet() {
        WalletService service = new WalletService();

        service.deposit("w1", new BigDecimal("10.50"));

        assertEquals(new BigDecimal("10.50"), service.balance("w1"));
    }

    @Test
    void withdrawDebitsTheWallet() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10.50"));

        service.withdraw("w1", new BigDecimal("4.50"));

        assertEquals(new BigDecimal("6.00"), service.balance("w1"));
    }

    @Test
    void unknownWalletStartsAtZero() {
        WalletService service = new WalletService();

        assertEquals(BigDecimal.ZERO, service.balance("w1"));
    }

    @Test
    void walletsAreIndependent() {
        WalletService service = new WalletService();

        service.deposit("w1", new BigDecimal("10"));
        service.deposit("w2", new BigDecimal("25"));

        assertEquals(new BigDecimal("10"), service.balance("w1"));
        assertEquals(new BigDecimal("25"), service.balance("w2"));
    }

    @Test
    void nullWalletIdIsRejected() {
        WalletService service = new WalletService();

        assertThrows(IllegalArgumentException.class, () -> service.deposit(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.withdraw(null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.balance(null));
    }

    @Test
    void transferMovesFunds() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10.50"));
        service.deposit("w2", new BigDecimal("1.00"));

        service.transfer("w1", "w2", new BigDecimal("4.50"));

        assertEquals(new BigDecimal("6.00"), service.balance("w1"));
        assertEquals(new BigDecimal("5.50"), service.balance("w2"));
    }

    @Test
    void transferRejectsAmountAboveBalance() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        assertThrows(
                IllegalStateException.class,
                () -> service.transfer("w1", "w2", new BigDecimal("10.01")));
        assertEquals(new BigDecimal("10"), service.balance("w1"));
        assertEquals(BigDecimal.ZERO, service.balance("w2"));
    }

    @Test
    void transferRejectsNonPositiveAmount() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        assertThrows(
                IllegalArgumentException.class, () -> service.transfer("w1", "w2", BigDecimal.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.transfer("w1", "w2", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> service.transfer("w1", "w2", null));
    }

    @Test
    void transferRejectsNullWalletIds() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        assertThrows(
                IllegalArgumentException.class, () -> service.transfer(null, "w2", BigDecimal.ONE));
        assertThrows(
                IllegalArgumentException.class, () -> service.transfer("w1", null, BigDecimal.ONE));
    }

    @Test
    void transferCreatesMissingWallets() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        service.transfer("w1", "w2", new BigDecimal("10"));

        assertTrue(service.wallets.containsKey("w1"));
        assertTrue(service.wallets.containsKey("w2"));
        assertEquals(new BigDecimal("10"), service.balance("w2"));
    }
}
