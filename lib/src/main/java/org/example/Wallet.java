package org.example;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReentrantLock;

public class Wallet {
    private final String id;
    final ReentrantLock lock = new ReentrantLock();
    private BigDecimal balance;

    public Wallet(String id) {
        this.id = id;
        this.balance = BigDecimal.ZERO;
    }

    public BigDecimal getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        lock.lock();
        try {
            balance = balance.add(amount);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        lock.lock();
        try {
            if (balance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient funds");
            }
            balance = balance.subtract(amount);
        } finally {
            lock.unlock();
        }
    }
}
