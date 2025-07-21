// TransactionGraph.java
package com.banking.model;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TransactionGraph {
    private final Graph<Account, TransactionEdge> graph;
    private final Map<String, Account> accounts;
    private final Map<String, List<Transaction>> transactions;
    private final Map<TransactionEdge, List<Transaction>> edgeTransactions;

    public TransactionGraph() {
        this.graph = new DefaultDirectedWeightedGraph<>(TransactionEdge.class);
        this.accounts = new ConcurrentHashMap<>();
        this.transactions = new ConcurrentHashMap<>();
        this.edgeTransactions = new ConcurrentHashMap<>();
    }

    // Clase interna para representar las aristas del grafo
    public static class TransactionEdge extends DefaultWeightedEdge {
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int transactionCount = 0;
        private LocalDateTime lastTransactionDate;

        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getTransactionCount() { return transactionCount; }
        public LocalDateTime getLastTransactionDate() { return lastTransactionDate; }

        public void addTransaction(BigDecimal amount, LocalDateTime date) {
            this.totalAmount = this.totalAmount.add(amount);
            this.transactionCount++;
            this.lastTransactionDate = date;
        }

        @Override
        public String toString() {
            return String.format("TransactionEdge{amount=%s, count=%d, lastDate=%s}",
                    totalAmount, transactionCount, lastTransactionDate);
        }
    }

    // Gestión de cuentas
    public void addAccount(Account account) {
        accounts.put(account.getAccountId(), account);
        graph.addVertex(account);
        transactions.put(account.getAccountId(), new ArrayList<>());
    }

    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    // Gestión de transacciones
    public void addTransaction(Transaction transaction) {
        Account source = transaction.getSourceAccount();
        Account destination = transaction.getDestinationAccount();

        // Asegurar que las cuentas estén en el grafo
        if (!accounts.containsKey(source.getAccountId())) {
            addAccount(source);
        }
        if (!accounts.containsKey(destination.getAccountId())) {
            addAccount(destination);
        }

        // Agregar o actualizar la arista
        TransactionEdge edge = graph.getEdge(source, destination);
        if (edge == null) {
            edge = new TransactionEdge();
            graph.addEdge(source, destination, edge);
            edgeTransactions.put(edge, new ArrayList<>());
        }

        // Actualizar la arista con la nueva transacción
        edge.addTransaction(transaction.getAmount(), transaction.getTimestamp());
        graph.setEdgeWeight(edge, edge.getTotalAmount().doubleValue());

        // Almacenar la transacción
        transactions.get(source.getAccountId()).add(transaction);
        transactions.get(destination.getAccountId()).add(transaction);
        edgeTransactions.get(edge).add(transaction);
    }

    // Consultas del grafo
    public List<Transaction> getTransactionsBetween(Account source, Account destination) {
        TransactionEdge edge = graph.getEdge(source, destination);
        return edge != null ? new ArrayList<>(edgeTransactions.get(edge)) : new ArrayList<>();
    }

    public BigDecimal getTotalTransferredBetween(Account source, Account destination) {
        TransactionEdge edge = graph.getEdge(source, destination);
        return edge != null ? edge.getTotalAmount() : BigDecimal.ZERO;
    }

    public List<Transaction> getTransactionsForAccount(String accountId) {
        return transactions.getOrDefault(accountId, new ArrayList<>());
    }

    public List<Transaction> getTransactionsInPeriod(LocalDateTime start, LocalDateTime end) {
        return transactions.values().stream()
                .flatMap(List::stream)
                .filter(t -> !t.getTimestamp().isBefore(start) && !t.getTimestamp().isAfter(end))
                .collect(Collectors.toList());
    }

    // Estadísticas básicas
    public int getAccountCount() {
        return graph.vertexSet().size();
    }

    public int getTransactionCount() {
        return transactions.values().stream().mapToInt(List::size).sum() / 2; // Dividido por 2 porque cada transacción se cuenta dos veces
    }

    public Account getMostActiveAccount() {
        return accounts.values().stream()
                .max(Comparator.comparing(account ->
                        transactions.get(account.getAccountId()).size()))
                .orElse(null);
    }

    public BigDecimal getTotalAmountTransferred() {
        return graph.edgeSet().stream()
                .map(edge -> ((TransactionEdge) edge).getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Acceso directo al grafo para visualización
    public Graph<Account, TransactionEdge> getGraph() {
        return graph;
    }
}