package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

    @Test
    void concurrentTransfersInBothDirectionsKeepFundsConsistent() throws InterruptedException {
        WalletService service = new WalletService();
        int transfersPerThread = 5_000;
        // fund enough that either thread can run to completion alone
        BigDecimal funding = new BigDecimal(transfersPerThread);
        service.deposit("w1", funding);
        service.deposit("w2", funding);
        CountDownLatch start = new CountDownLatch(1);

        Runnable forward = transferLoop(service, "w1", "w2", transfersPerThread, start);
        Runnable backward = transferLoop(service, "w2", "w1", transfersPerThread, start);

        Thread t1 = new Thread(forward);
        Thread t2 = new Thread(backward);
        t1.start();
        t2.start();
        start.countDown();
        t1.join(30_000);
        t2.join(30_000);

        assertFalse(t1.isAlive() || t2.isAlive(), "transfer threads did not finish: possible deadlock");
        assertEquals(funding, service.balance("w1"));
        assertEquals(funding, service.balance("w2"));
    }

    @Test
    void transferToSameWalletIsRejected() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        assertThrows(
                IllegalArgumentException.class, () -> service.transfer("w1", "w1", BigDecimal.ONE));
        assertEquals(new BigDecimal("10"), service.balance("w1"));
    }

    @Test
    void ringTransfersOnThreeThreadsKeepFundsConsistent() throws InterruptedException {
        WalletService service = new WalletService();
        int transfersPerThread = 2_000;
        BigDecimal funding = new BigDecimal(transfersPerThread);
        service.deposit("a", funding);
        service.deposit("b", funding);
        service.deposit("c", funding);
        CountDownLatch start = new CountDownLatch(1);

        Thread ab = new Thread(transferLoop(service, "a", "b", transfersPerThread, start));
        Thread bc = new Thread(transferLoop(service, "b", "c", transfersPerThread, start));
        Thread ca = new Thread(transferLoop(service, "c", "a", transfersPerThread, start));
        ab.start();
        bc.start();
        ca.start();
        start.countDown();
        ab.join(30_000);
        bc.join(30_000);
        ca.join(30_000);

        assertFalse(
                ab.isAlive() || bc.isAlive() || ca.isAlive(),
                "transfer threads did not finish: possible deadlock");
        assertEquals(funding, service.balance("a"));
        assertEquals(funding, service.balance("b"));
        assertEquals(funding, service.balance("c"));
    }

    @Test
    void depositIsRecorded() {
        WalletService service = new WalletService();

        service.deposit("w1", new BigDecimal("10"));

        List<Operation> log = List.copyOf(service.operations);
        assertEquals(1, log.size());
        Operation op = log.get(0);
        assertEquals("w1", op.walletId());
        assertNull(op.counterpartyWalletId());
        assertEquals(OperationType.DEPOSIT, op.type());
        assertEquals(new BigDecimal("10"), op.amount());
        assertNotNull(op.timestamp());
    }

    @Test
    void withdrawIsRecorded() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        service.withdraw("w1", new BigDecimal("4"));

        List<Operation> log = List.copyOf(service.operations);
        assertEquals(2, log.size());
        Operation op = log.get(1);
        assertEquals("w1", op.walletId());
        assertNull(op.counterpartyWalletId());
        assertEquals(OperationType.WITHDRAWAL, op.type());
        assertEquals(new BigDecimal("4"), op.amount());
    }

    @Test
    void transferIsRecorded() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        service.transfer("w1", "w2", new BigDecimal("4"));

        List<Operation> log = List.copyOf(service.operations);
        assertEquals(2, log.size());
        Operation op = log.get(1);
        assertEquals("w1", op.walletId());
        assertEquals("w2", op.counterpartyWalletId());
        assertEquals(OperationType.TRANSFER, op.type());
        assertEquals(new BigDecimal("4"), op.amount());
    }

    @Test
    void operationsAreRecordedInOrder() {
        WalletService service = new WalletService();

        service.deposit("w1", new BigDecimal("10"));
        service.withdraw("w1", new BigDecimal("4"));
        service.transfer("w1", "w2", new BigDecimal("2"));
        service.deposit("w2", new BigDecimal("7"));

        List<Operation> log = List.copyOf(service.operations);
        assertEquals(4, log.size());
        assertEquals(
                List.of(
                        OperationType.DEPOSIT,
                        OperationType.WITHDRAWAL,
                        OperationType.TRANSFER,
                        OperationType.DEPOSIT),
                log.stream().map(Operation::type).toList());
        assertEquals(
                List.of(
                        new BigDecimal("10"),
                        new BigDecimal("4"),
                        new BigDecimal("2"),
                        new BigDecimal("7")),
                log.stream().map(Operation::amount).toList());
    }

    @Test
    void failedOperationsAreNotRecorded() {
        WalletService service = new WalletService();

        assertThrows(
                IllegalStateException.class, () -> service.withdraw("w1", new BigDecimal("1")));
        assertThrows(IllegalArgumentException.class, () -> service.deposit("w1", BigDecimal.ZERO));
        assertThrows(
                IllegalStateException.class,
                () -> service.transfer("w1", "w2", new BigDecimal("1")));

        assertTrue(service.operations.isEmpty());
    }

    @Test
    void historyRejectsNullWalletId() {
        WalletService service = new WalletService();

        assertThrows(IllegalArgumentException.class, () -> service.history(null));
    }

    @Test
    void historyReturnsOperationsForOwnAndCounterpartySide() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));
        service.transfer("w1", "w2", new BigDecimal("4"));
        service.deposit("w3", new BigDecimal("5"));

        List<Operation> w1History = service.history("w1");
        assertEquals(2, w1History.size());
        assertEquals(OperationType.DEPOSIT, w1History.get(0).type());
        assertEquals(OperationType.TRANSFER, w1History.get(1).type());

        // w2 only appears as the counterparty of the transfer
        List<Operation> w2History = service.history("w2");
        assertEquals(1, w2History.size());
        assertEquals(OperationType.TRANSFER, w2History.get(0).type());
        assertEquals("w1", w2History.get(0).walletId());
    }

    @Test
    void historyIsEmptyForUnknownWallet() {
        WalletService service = new WalletService();
        service.deposit("w1", new BigDecimal("10"));

        assertTrue(service.history("nope").isEmpty());
    }

    @Test
    void balanceEqualityIsScaleSensitive() {
        WalletService service = new WalletService();

        service.deposit("w1", new BigDecimal("0.10"));
        service.deposit("w1", new BigDecimal("0.20"));

        assertEquals(new BigDecimal("0.30"), service.balance("w1"));
        // same value, different scale: equals() says no, compareTo() says yes
        assertNotEquals(new BigDecimal("0.3"), service.balance("w1"));
        assertEquals(0, service.balance("w1").compareTo(new BigDecimal("0.3")));
    }

    private Runnable transferLoop(
            WalletService service, String from, String to, int times, CountDownLatch start) {
        return () -> {
            try {
                start.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (int i = 0; i < times; i++) {
                service.transfer(from, to, BigDecimal.ONE);
            }
        };
    }
}
