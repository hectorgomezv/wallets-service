package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
