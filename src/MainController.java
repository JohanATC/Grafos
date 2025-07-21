// MainController.java
package com.banking.ui.controller;

import com.banking.BankingAnalyzerApp;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.GraphService;
import com.banking.service.QueryService;
import com.banking.service.StatisticsService;
import com.banking.ui.view.GraphPanel;
import com.banking.ui.view.StatisticsPanel;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class MainController implements Initializable {

    @FXML private BorderPane mainBorderPane;
    @FXML private TabPane mainTabPane;
    @FXML private Tab graphTab;
    @FXML private Tab statisticsTab;
    @FXML private Tab queriesTab;

    // Controles de consultas
    @FXML private TextField sourceAccountField;
    @FXML private TextField destinationAccountField;
    @FXML private TextField amountMinField;
    @FXML private TextField amountMaxField;
    @FXML private ComboBox<String> transactionTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button searchButton;
    @FXML private Button clearButton;

    // Tabla de resultados
    @FXML private TableView<Transaction> resultsTable;
    @FXML private TableColumn<Transaction, String> transactionIdColumn;
    @FXML private TableColumn<Transaction, String> sourceColumn;
    @FXML private TableColumn<Transaction, String> destinationColumn;
    @FXML private TableColumn<Transaction, String> amountColumn;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;

    // Panel de información
    @FXML private TextArea infoTextArea;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    // Paneles personalizados
    private GraphPanel graphPanel;
    private StatisticsPanel statisticsPanel;

    // Servicios
    private GraphService graphService;
    private QueryService queryService;
    private StatisticsService statisticsService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            initializeServices();
            initializeComponents();
            setupEventHandlers();
            loadInitialData();

            updateStatus("Aplicación inicializada correctamente");
        } catch (Exception e) {
            showError("Error de inicialización", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeServices() {
        graphService = BankingAnalyzerApp.getGraphService();
        queryService = BankingAnalyzerApp.getQueryService();
        statisticsService = BankingAnalyzerApp.getStatisticsService();
    }

    private void initializeComponents() {
        // Inicializar paneles personalizados
        graphPanel = new GraphPanel(graphService);
        statisticsPanel = new StatisticsPanel(statisticsService);

        // Agregar paneles a las pestañas
        graphTab.setContent(graphPanel);
        statisticsTab.setContent(statisticsPanel);

        // Configurar tabla de resultados
        setupResultsTable();

        // Configurar combo de tipos de transacción
        transactionTypeCombo.setItems(FXCollections.observableArrayList(
                "TRANSFER", "DEPOSIT", "WITHDRAWAL", "PAYMENT"
        ));

        // Configurar campos de texto numéricos
        setupNumericFields();

        // Configurar área de información
        infoTextArea.setEditable(false);
        infoTextArea.setWrapText(true);

        // Configurar barra de progreso
        progressBar.setVisible(false);

        updateStatus("Componentes inicializados");
    }

    private void setupResultsTable() {
        transactionIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionId()));

        sourceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getSourceAccount().getAccountNumber()));

        destinationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDestinationAccount().getAccountNumber()));

        amountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty("$" + cellData.getValue().getAmount().toString()));

        dateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimestamp().toString()));

        typeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTransactionType()));

        // Configurar selección de fila
        resultsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showTransactionDetails(newSelection);
            }
        });
    }

    private void setupNumericFields() {
        // Permitir solo números en campos de monto
        amountMinField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                amountMinField.setText(oldValue);
            }
        });

        amountMaxField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                amountMaxField.setText(oldValue);
            }
        });
    }

    private void setupEventHandlers() {
        searchButton.setOnAction(e -> performSearch());
        clearButton.setOnAction(e -> clearSearchFields());

        // Actualizar paneles cuando cambie de pestaña
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == statisticsTab) {
                statisticsPanel.refreshStatistics();
            } else if (newTab == graphTab) {
                graphPanel.refreshGraph();
            }
        });
    }

    private void loadInitialData() {
        updateInfoPanel();
    }

    @FXML
    private void performSearch() {
        CompletableFuture.runAsync(() -> {
            Platform.runLater(() -> {
                progressBar.setVisible(true);
                searchButton.setDisabled(true);
                updateStatus("Realizando búsqueda...");
            });

            try {
                List<Transaction> results = executeQuery();

                Platform.runLater(() -> {
                    ObservableList<Transaction> observableResults = FXCollections.observableArrayList(results);
                    resultsTable.setItems(observableResults);

                    updateStatus("Búsqueda completada. " + results.size() + " resultados encontrados");
                    updateSearchInfo(results);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error en búsqueda", e.getMessage());
                    updateStatus("Error en búsqueda");
                });
            } finally {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    searchButton.setDisabled(false);
                });
            }
        });
    }

    private List<Transaction> executeQuery() {
        // Obtener parámetros de búsqueda
        String sourceAccount = sourceAccountField.getText().trim();
        String destinationAccount = destinationAccountField.getText().trim();
        String transactionType = transactionTypeCombo.getValue();

        // Filtros de monto
        BigDecimal minAmount = null;
        BigDecimal maxAmount = null;

        if (!amountMinField.getText().trim().isEmpty()) {
            minAmount = new BigDecimal(amountMinField.getText().trim());
        }

        if (!amountMaxField.getText().trim().isEmpty()) {
            maxAmount = new BigDecimal(amountMaxField.getText().trim());
        }

        // Ejecutar consulta basada en los parámetros
        List<Transaction> results;

        if (!sourceAccount.isEmpty() && !destinationAccount.isEmpty()) {
            // Búsqueda entre cuentas específicas
            if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                LocalDateTime start = startDatePicker.getValue().atStartOfDay();
                LocalDateTime end = endDatePicker.getValue().atTime(23, 59, 59);
                results = queryService.getTransactionsBetweenAccountsInPeriod(
                        sourceAccount, destinationAccount, start, end);
            } else {
                Account source = findAccountByNumber(sourceAccount);
                Account dest = findAccountByNumber(destinationAccount);
                if (source != null && dest != null) {
                    results = graphService.getTransactionGraph().getTransactionsBetween(source, dest);
                } else {
                    results = List.of();
                }
            }
        } else if (transactionType != null) {
            // Búsqueda por tipo de transacción
            results = queryService.getTransactionsByType(transactionType);
        } else if (minAmount != null || maxAmount != null) {
            // Búsqueda por rango de monto
            BigDecimal min = minAmount != null ? minAmount : BigDecimal.ZERO;
            BigDecimal max = maxAmount != null ? maxAmount : new BigDecimal("999999999");
            results = queryService.getTransactionsByAmountRange(min, max);
        } else {
            // Sin filtros específicos, devolver todas las transacciones
            results = graphService.getTransactionGraph().getAllAccounts().stream()
                    .flatMap(account -> graphService.getTransactionGraph()
                            .getTransactionsForAccount(account.getAccountId()).stream())
                    .distinct()
                    .toList();
        }

        return results;
    }

    private Account findAccountByNumber(String accountNumber) {
        return graphService.getTransactionGraph().getAllAccounts().stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst()
                .orElse(null);
    }

    @FXML
    private void clearSearchFields() {
        sourceAccountField.clear();
        destinationAccountField.clear();
        amountMinField.clear();
        amountMaxField.clear();
        transactionTypeCombo.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        resultsTable.getItems().clear();
        infoTextArea.clear();
        updateStatus("Campos de búsqueda limpiados");
    }

    private void showTransactionDetails(Transaction transaction) {
        StringBuilder details = new StringBuilder();
        details.append("=== DETALLES DE LA TRANSACCIÓN ===\n\n");
        details.append("ID: ").append(transaction.getTransactionId()).append("\n");
        details.append("Tipo: ").append(transaction.getTransactionType()).append("\n");
        details.append("Monto: $").append(transaction.getAmount()).append("\n");
        details.append("Fecha: ").append(transaction.getTimestamp()).append("\n");
        details.append("Estado: ").append(transaction.getStatus()).append("\n\n");

        details.append("--- CUENTA ORIGEN ---\n");
        Account source = transaction.getSourceAccount();
        details.append("Número: ").append(source.getAccountNumber()).append("\n");
        details.append("Propietario: ").append(source.getOwnerName()).append("\n");
        details.append("Banco: ").append(source.getBankName()).append("\n\n");

        details.append("--- CUENTA DESTINO ---\n");
        Account dest = transaction.getDestinationAccount();
        details.append("Número: ").append(dest.getAccountNumber()).append("\n");
        details.append("Propietario: ").append(dest.getOwnerName()).append("\n");
        details.append("Banco: ").append(dest.getBankName()).append("\n\n");

        if (transaction.getDescription() != null && !transaction.getDescription().isEmpty()) {
            details.append("Descripción: ").append(transaction.getDescription()).append("\n");
        }

        infoTextArea.setText(details.toString());
    }

    private void updateSearchInfo(List<Transaction> results) {
        if (results.isEmpty()) {
            infoTextArea.setText("No se encontraron transacciones que coincidan con los criterios de búsqueda.");
            return;
        }

        BigDecimal totalAmount = results.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder info = new StringBuilder();
        info.append("=== RESUMEN DE BÚSQUEDA ===\n\n");
        info.append("Transacciones encontradas: ").append(results.size()).append("\n");
        info.append("Monto total: $").append(totalAmount).append("\n");

        if (!results.isEmpty()) {
            BigDecimal avgAmount = totalAmount.divide(BigDecimal.valueOf(results.size()), 2, BigDecimal.ROUND_HALF_UP);
            info.append("Monto promedio: $").append(avgAmount).append("\n");
        }

        infoTextArea.setText(info.toString());
    }

    private void updateInfoPanel() {
        StringBuilder info = new StringBuilder();
        info.append("=== INFORMACIÓN DEL SISTEMA ===\n\n");
        info.append("Total de cuentas: ").append(graphService.getTransactionGraph().getAccountCount()).append("\n");
        info.append("Total de transacciones: ").append(graphService.getTransactionGraph().getTransactionCount()).append("\n");
        info.append("Monto total transferido: $").append(graphService.getTransactionGraph().getTotalAmountTransferred()).append("\n\n");

        Account mostActive = graphService.getTransactionGraph().getMostActiveAccount();
        if (mostActive != null) {
            info.append("Cuenta más activa: ").append(mostActive.getAccountNumber())
                    .append(" (").append(mostActive.getOwnerName()).append(")\n");
        }

        info.append("\n=== INSTRUCCIONES ===\n");
        info.append("• Use la pestaña 'Visualización del Grafo' para ver la red de transacciones\n");
        info.append("• Use la pestaña 'Estadísticas' para ver métricas detalladas\n");
        info.append("• Use esta pestaña para realizar consultas específicas\n");

        infoTextArea.setText(info.toString());
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}