// QueryService.java
package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionGraph;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class QueryService {
    private final TransactionGraph transactionGraph;

    public QueryService(TransactionGraph transactionGraph) {
        this.transactionGraph = transactionGraph;
    }

    // Consultas por monto
    public BigDecimal getTotalTransferredBetweenAccounts(String sourceId, String destinationId) {
        Account source = transactionGraph.getAccount(sourceId);
        Account destination = transactionGraph.getAccount(destinationId);

        if (source == null || destination == null) {
            return BigDecimal.ZERO;
        }

        return transactionGraph.getTotalTransferredBetween(source, destination);
    }

    public Account getAccountWithHighestTransferAmount() {
        return transactionGraph.getAllAccounts().stream()
                .max((a1, a2) -> {
                    BigDecimal total1 = getTotalTransferredByAccount(a1.getAccountId());
                    BigDecimal total2 = getTotalTransferredByAccount(a2.getAccountId());
                    return total1.compareTo(total2);
                })
                .orElse(null);
    }

    private BigDecimal getTotalTransferredByAccount(String accountId) {
        return transactionGraph.getTransactionsForAccount(accountId).stream()
                .filter(t -> t.getSourceAccount().getAccountId().equals(accountId))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Consultas por período de tiempo
    public List<Transaction> getTransactionsBetweenAccountsInPeriod(
            String sourceId, String destinationId,
            LocalDateTime start, LocalDateTime end) {

        Account source = transactionGraph.getAccount(sourceId);
        Account destination = transactionGraph.getAccount(destinationId);

        if (source == null || destination == null) {
            return List.of();
        }

        return transactionGraph.getTransactionsBetween(source, destination).stream()
                .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    public int getTransactionCountBetweenAccounts(String sourceId, String destinationId) {
        Account source = transactionGraph.getAccount(sourceId);
        Account destination = transactionGraph.getAccount(destinationId);

        if (source == null || destination == null) {
            return 0;
        }

        return transactionGraph.getTransactionsBetween(source, destination).size();
    }

    // Consultas por tipo de transacción
    public List<Transaction> getTransactionsByType(String transactionType) {
        return transactionGraph.getAllAccounts().stream()
                .flatMap(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).stream())
                .filter(t -> t.getTransactionType().equalsIgnoreCase(transactionType))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionsByAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        return transactionGraph.getAllAccounts().stream()
                .flatMap(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).stream())
                .filter(t -> t.getAmount().compareTo(minAmount) >= 0 &&
                        t.getAmount().compareTo(maxAmount) <= 0)
                .distinct()
                .collect(Collectors.toList());
    }

    // Búsqueda por patrones
    public List<Account> findAccountsByOwnerName(String ownerName) {
        return transactionGraph.getAllAccounts().stream()
                .filter(account -> account.getOwnerName().toLowerCase()
                        .contains(ownerName.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Account> findAccountsByBank(String bankName) {
        return transactionGraph.getAllAccounts().stream()
                .filter(account -> account.getBankName().toLowerCase()
                        .contains(bankName.toLowerCase()))
                .collect(Collectors.toList());
    }
}