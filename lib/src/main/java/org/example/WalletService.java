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

    private Wallet getOrCreateWallet(String walletId) {
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet id must not be null");
        }
        return wallets.computeIfAbsent(walletId, Wallet::new);
    }
}
