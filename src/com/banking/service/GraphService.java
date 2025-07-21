// GraphService.java
package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionGraph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class GraphService {
    private final TransactionGraph transactionGraph;

    public GraphService() {
        this.transactionGraph = new TransactionGraph();
    }

    public TransactionGraph getTransactionGraph() {
        return transactionGraph;
    }

    // Operaciones básicas del grafo
    public void addAccount(Account account) {
        transactionGraph.addAccount(account);
    }

    public void addTransaction(Transaction transaction) {
        transactionGraph.addTransaction(transaction);
    }

    // Análisis de conectividad
    public boolean areAccountsConnected(Account source, Account destination) {
        ConnectivityInspector<Account, TransactionGraph.TransactionEdge> inspector =
                new ConnectivityInspector<>(transactionGraph.getGraph());
        return inspector.pathExists(source, destination);
    }

    // Caminos más cortos (basado en número de transacciones)
    public List<Account> getShortestPath(Account source, Account destination) {
        DijkstraShortestPath<Account, TransactionGraph.TransactionEdge> dijkstra =
                new DijkstraShortestPath<>(transactionGraph.getGraph());

        var path = dijkstra.getPath(source, destination);
        return path != null ? path.getVertexList() : new ArrayList<>();
    }

    // Análisis de flujo de efectivo
    public Map<Account, BigDecimal> getNetFlowForAllAccounts() {
        Map<Account, BigDecimal> netFlow = new HashMap<>();

        for (Account account : transactionGraph.getAllAccounts()) {
            BigDecimal inflow = getInflowForAccount(account);
            BigDecimal outflow = getOutflowForAccount(account);
            netFlow.put(account, inflow.subtract(outflow));
        }

        return netFlow;
    }

    private BigDecimal getInflowForAccount(Account account) {
        return transactionGraph.getGraph().incomingEdgesOf(account).stream()
                .map(edge -> ((TransactionGraph.TransactionEdge) edge).getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getOutflowForAccount(Account account) {
        return transactionGraph.getGraph().outgoingEdgesOf(account).stream()
                .map(edge -> ((TransactionGraph.TransactionEdge) edge).getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Cuentas más conectadas
    public List<Account> getMostConnectedAccounts(int limit) {
        return transactionGraph.getAllAccounts().stream()
                .sorted((a1, a2) -> Integer.compare(
                        transactionGraph.getGraph().degreeOf(a2),
                        transactionGraph.getGraph().degreeOf(a1)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Detección de clusters/grupos de cuentas
    public Set<Set<Account>> getConnectedComponents() {
        ConnectivityInspector<Account, TransactionGraph.TransactionEdge> inspector =
                new ConnectivityInspector<>(transactionGraph.getGraph());
        return inspector.connectedSets();
    }
}