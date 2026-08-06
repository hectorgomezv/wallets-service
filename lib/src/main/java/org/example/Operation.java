package org.example;

import java.math.BigDecimal;
import java.time.Instant;

// counterpartyWalletId is null for DEPOSIT and WITHDRAWAL
public record Operation(
        String walletId,
        String counterpartyWalletId,
        OperationType type,
        BigDecimal amount,
        Instant timestamp) {}
