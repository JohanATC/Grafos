// StatisticsService.java
package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionGraph;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class StatisticsService {
    private final TransactionGraph transactionGraph;

    public StatisticsService(TransactionGraph transactionGraph) {
        this.transactionGraph = transactionGraph;
    }

    // Estadísticas generales
    public TransactionStatistics getGeneralStatistics() {
        return new TransactionStatistics(
                transactionGraph.getAccountCount(),
                transactionGraph.getTransactionCount(),
                transactionGraph.getTotalAmountTransferred(),
                calculateAverageTransactionAmount(),
                calculateAverageTransactionsPerAccount()
        );
    }

    // Estadísticas por período
    public TransactionStatistics getStatisticsForPeriod(LocalDateTime start, LocalDateTime end) {
        List<Transaction> periodTransactions = transactionGraph.getTransactionsInPeriod(start, end);

        BigDecimal totalAmount = periodTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = periodTransactions.isEmpty() ?
                BigDecimal.ZERO :
                totalAmount.divide(BigDecimal.valueOf(periodTransactions.size()), 2, RoundingMode.HALF_UP);

        Set<String> activeAccounts = periodTransactions.stream()
                .flatMap(t -> List.of(t.getSourceAccount().getAccountId(),
                        t.getDestinationAccount().getAccountId()).stream())
                .collect(Collectors.toSet());

        return new TransactionStatistics(
                activeAccounts.size(),
                periodTransactions.size(),
                totalAmount,
                averageAmount,
                activeAccounts.isEmpty() ? 0.0 :
                        (double) periodTransactions.size() / activeAccounts.size()
        );
    }

    // Top cuentas más activas
    public List<AccountActivity> getMostActiveAccounts(int limit) {
        return transactionGraph.getAllAccounts().stream()
                .map(this::calculateAccountActivity)
                .sorted((a1, a2) -> Integer.compare(a2.getTransactionCount(), a1.getTransactionCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Top cuentas por volumen de transferencias
    public List<AccountActivity> getHighestVolumeAccounts(int limit) {
        return transactionGraph.getAllAccounts().stream()
                .map(this::calculateAccountActivity)
                .sorted((a1, a2) -> a2.getTotalVolume().compareTo(a1.getTotalVolume()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Distribución por tipo de transacción
    public Map<String, Integer> getTransactionTypeDistribution() {
        return transactionGraph.getAllAccounts().stream()
                .flatMap(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).stream())
                .collect(Collectors.groupingBy(
                        Transaction::getTransactionType,
                        Collectors.reducing(0, t -> 1, Integer::sum)
                ));
    }

    // Estadísticas por banco
    public Map<String, BankStatistics> getStatisticsByBank() {
        Map<String, List<Account>> accountsByBank = transactionGraph.getAllAccounts().stream()
                .collect(Collectors.groupingBy(Account::getBankName));

        Map<String, BankStatistics> bankStats = new HashMap<>();

        for (Map.Entry<String, List<Account>> entry : accountsByBank.entrySet()) {
            String bankName = entry.getKey();
            List<Account> accounts = entry.getValue();

            int totalTransactions = accounts.stream()
                    .mapToInt(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).size())
                    .sum();

            BigDecimal totalVolume = accounts.stream()
                    .flatMap(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).stream())
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            bankStats.put(bankName, new BankStatistics(
                    bankName, accounts.size(), totalTransactions, totalVolume
            ));
        }

        return bankStats;
    }

    // Métodos auxiliares
    private BigDecimal calculateAverageTransactionAmount() {
        List<Transaction> allTransactions = transactionGraph.getAllAccounts().stream()
                .flatMap(account -> transactionGraph.getTransactionsForAccount(account.getAccountId()).stream())
                .distinct()
                .collect(Collectors.toList());

        if (allTransactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = allTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(allTransactions.size()), 2, RoundingMode.HALF_UP);
    }

    private double calculateAverageTransactionsPerAccount() {
        if (transactionGraph.getAccountCount() == 0) {
            return 0.0;
        }
        return (double) transactionGraph.getTransactionCount() / transactionGraph.getAccountCount();
    }

    private AccountActivity calculateAccountActivity(Account account) {
        List<Transaction> accountTransactions = transactionGraph.getTransactionsForAccount(account.getAccountId());

        BigDecimal totalVolume = accountTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AccountActivity(account, accountTransactions.size(), totalVolume);
    }

    // Clases de datos para estadísticas
    public static class TransactionStatistics {
        private final int accountCount;
        private final int transactionCount;
        private final BigDecimal totalAmount;
        private final BigDecimal averageTransactionAmount;
        private final double averageTransactionsPerAccount;

        public TransactionStatistics(int accountCount, int transactionCount, BigDecimal totalAmount,
                                     BigDecimal averageTransactionAmount, double averageTransactionsPerAccount) {
            this.accountCount = accountCount;
            this.transactionCount = transactionCount;
            this.totalAmount = totalAmount;
            this.averageTransactionAmount = averageTransactionAmount;
            this.averageTransactionsPerAccount = averageTransactionsPerAccount;
        }

        // Getters
        public int getAccountCount() { return accountCount; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getAverageTransactionAmount() { return averageTransactionAmount; }
        public double getAverageTransactionsPerAccount() { return averageTransactionsPerAccount; }
    }

    public static class AccountActivity {
        private final Account account;
        private final int transactionCount;
        private final BigDecimal totalVolume;

        public AccountActivity(Account account, int transactionCount, BigDecimal totalVolume) {
            this.account = account;
            this.transactionCount = transactionCount;
            this.totalVolume = totalVolume;
        }

        // Getters
        public Account getAccount() { return account; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getTotalVolume() { return totalVolume; }
    }

    public static class BankStatistics {
        private final String bankName;
        private final int accountCount;
        private final int transactionCount;
        private final BigDecimal totalVolume;

        public BankStatistics(String bankName, int accountCount, int transactionCount, BigDecimal totalVolume) {
            this.bankName = bankName;
            this.accountCount = accountCount;
            this.transactionCount = transactionCount;
            this.totalVolume = totalVolume;
        }

        // Getters
        public String getBankName() { return bankName; }
        public int getAccountCount() { return accountCount; }
        public int getTransactionCount() { return transactionCount; }
        public BigDecimal getTotalVolume() { return totalVolume; }
    }
}