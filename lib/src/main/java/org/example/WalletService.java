package org.example;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class WalletService {

    final ConcurrentHashMap<String, Wallet> wallets = new ConcurrentHashMap<>();

    public void deposit(String walletId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(walletId);
        wallet.deposit(amount);
    }

    public void withdraw(String walletId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(walletId);
        wallet.withdraw(amount);
    }

    public BigDecimal balance(String walletId) {
        Wallet wallet = getOrCreateWallet(walletId);
        return wallet.getBalance();
    }

    public void transfer(String fromWalletId, String toWalletId, BigDecimal amount) {
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
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    private Wallet getOrCreateWallet(String walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet id must not be null");
        }
        return wallets.computeIfAbsent(walletId, Wallet::new);
    }
}
