package org.example;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WalletService {

    final ConcurrentHashMap<String, Wallet> wallets = new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    public void deposit(String walletId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(walletId);
        wallet.deposit(amount);
        record(walletId, null, OperationType.DEPOSIT, amount);
    }

    public void withdraw(String walletId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(walletId);
        wallet.withdraw(amount);
        record(walletId, null, OperationType.WITHDRAWAL, amount);
    }

    public BigDecimal balance(String walletId) {
        Wallet wallet = getOrCreateWallet(walletId);
        return wallet.getBalance();
    }

    public void transfer(String fromWalletId, String toWalletId, BigDecimal amount) {
        if (fromWalletId != null && fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        Wallet from = getOrCreateWallet(fromWalletId);
        Wallet to = getOrCreateWallet(toWalletId);

        // lock in id order so two opposite transfers cannot deadlock
        Wallet first = fromWalletId.compareTo(toWalletId) <= 0 ? from : to;
        Wallet second = first == from ? to : from;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                from.withdraw(amount);
                to.deposit(amount);
                record(fromWalletId, toWalletId, OperationType.TRANSFER, amount);
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    public List<Operation> history(String walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet id must not be null");
        }
        return operations.stream()
                .filter(
                        op -> walletId.equals(op.walletId())
                                || walletId.equals(op.counterpartyWalletId()))
                .toList();
    }

    private void record(
            String walletId, String counterpartyWalletId, OperationType type, BigDecimal amount) {
        operations.add(
                new Operation(walletId, counterpartyWalletId, type, amount, Instant.now()));
    }

    private Wallet getOrCreateWallet(String walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet id must not be null");
        }
        return wallets.computeIfAbsent(walletId, Wallet::new);
    }
} 
                                