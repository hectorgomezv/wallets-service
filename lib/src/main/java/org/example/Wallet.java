package org.example;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public class Wallet {
    private final String id;
    private final AtomicReference<BigDecimal> balance;

    public Wallet(String id) {
        this.id = id;
        this.balance = new AtomicReference<>(BigDecimal.ZERO);
    }

    public BigDecimal getBalance() {
        return balance.get();
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        BigDecimal current;
        BigDecimal updated;
        do {
            current = balance.get();
            updated = current.add(amount);
        } while (!balance.compareAndSet(current, updated));
    }
}
