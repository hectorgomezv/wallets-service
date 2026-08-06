package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

public class WalletTest {
    @Test
    void newWalletHasZeroBalance() {
        Wallet wallet = new Wallet("w1");

        assertEquals(BigDecimal.ZERO, wallet.getBalance());
    }

    @Test
    void depositIncreasesBalance() {
        Wallet wallet = new Wallet("w1");

        wallet.deposit(new BigDecimal("10.50"));
        wallet.deposit(new BigDecimal("4.50"));

        assertEquals(new BigDecimal("15.00"), wallet.getBalance());
    }

    @Test
    void depositRejectsNullAmount() {
        Wallet wallet = new Wallet("w1");

        assertThrows(IllegalArgumentException.class, () -> wallet.deposit(null));
    }

    @Test
    void depositRejectsZeroAmount() {
        Wallet wallet = new Wallet("w1");

        assertThrows(IllegalArgumentException.class, () -> wallet.deposit(BigDecimal.ZERO));
    }

    @Test
    void depositRejectsNegativeAmount() {
        Wallet wallet = new Wallet("w1");

        assertThrows(IllegalArgumentException.class, () -> wallet.deposit(new BigDecimal("-1")));
    }

    @Test
    void concurrentDepositsFromTwoThreadsAreConsistent() throws InterruptedException {
        Wallet wallet = new Wallet("w1");
        int depositsPerThread = 10_000;
        CountDownLatch start = new CountDownLatch(1);

        Runnable depositor = () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < depositsPerThread; i++) {
                wallet.deposit(BigDecimal.ONE);
            }
        };

        Thread t1 = new Thread(depositor);
        Thread t2 = new Thread(depositor);
        t1.start();
        t2.start();
        start.countDown();
        t1.join();
        t2.join();

        assertEquals(new BigDecimal(2 * depositsPerThread), wallet.getBalance());
    }

}
