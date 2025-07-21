package com.banking.ui.view;

import com.banking.model.Account;
import com.banking.service.StatisticsService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Panel para mostrar estadísticas y análisis de transacciones bancarias
 */
public class StatisticsPanel extends VBox {

    private StatisticsService statisticsService;
    private TabPane tabPane;

    // Controles para filtros de fecha
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Button updateStatsButton;

    // Paneles para diferentes tipos de estadísticas
    private VBox generalStatsPanel;
    private VBox chartPanel;
    private VBox accountStatsPanel;

    public StatisticsPanel(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        initializeComponents();
        setupLayout();
        updateAllStatistics();
    }

    private void initializeComponents() {
        // Configurar el panel principal
        this.setSpacing(10);
        this.setPadding(new Insets(15));
        this.setStyle("-fx-background-color: #f8f9fa;");

        // Crear controles de filtro de fecha
        createDateFilters();

        // Crear tabs para diferentes vistas
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab de estadísticas generales
        Tab generalTab = new Tab("Estadísticas Generales");
        generalStatsPanel = new VBox(10);
        generalStatsPanel.setPadding(new Insets(15));
        ScrollPane generalScrollPane = new ScrollPane(generalStatsPanel);
        generalScrollPane.setFitToWidth(true);
        generalTab.setContent(generalScrollPane);

        // Tab de gráficos
        Tab chartsTab = new Tab("Gráficos");
        chartPanel = new VBox(15);
        chartPanel.setPadding(new Insets(15));
        ScrollPane chartScrollPane = new ScrollPane(chartPanel);
        chartScrollPane.setFitToWidth(true);
        chartsTab.setContent(chartScrollPane);

        // Tab de estadísticas por cuenta
        Tab accountsTab = new Tab("Análisis por Cuenta");
        accountStatsPanel = new VBox(10);
        accountStatsPanel.setPadding(new Insets(15));
        ScrollPane accountScrollPane = new ScrollPane(accountStatsPanel);
        accountScrollPane.setFitToWidth(true);
        accountsTab.setContent(accountScrollPane);

        tabPane.getTabs().addAll(generalTab, chartsTab, accountsTab);
    }

    private void createDateFilters() {
        Label titleLabel = new Label("Análisis de Transacciones Bancarias");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox dateFilterBox = new HBox(10);
        dateFilterBox.setAlignment(Pos.CENTER_LEFT);
        dateFilterBox.setPadding(new Insets(10));
        dateFilterBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        Label startLabel = new Label("Fecha Inicio:");
        startDatePicker = new DatePicker(LocalDate.now().minusMonths(1));

        Label endLabel = new Label("Fecha Fin:");
        endDatePicker = new DatePicker(LocalDate.now());

        updateStatsButton = new Button("Actualizar Estadísticas");
        updateStatsButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        updateStatsButton.setOnAction(e -> updateAllStatistics());

        Button exportButton = new Button("Exportar Reporte");
        exportButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        exportButton.setOnAction(e -> exportReport());

        dateFilterBox.getChildren().addAll(
                startLabel, startDatePicker,
                new Separator(),
                endLabel, endDatePicker,
                new Separator(),
                updateStatsButton, exportButton
        );

        this.getChildren().addAll(titleLabel, dateFilterBox);
    }

    private void setupLayout() {
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        this.getChildren().add(tabPane);
    }

    public void updateAllStatistics() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate.isAfter(endDate)) {
            showAlert("Error", "La fecha de inicio debe ser anterior a la fecha de fin.");
            return;
        }

        updateGeneralStatistics(startDate, endDate);
        updateCharts(startDate, endDate);
        updateAccountStatistics(startDate, endDate);
    }

    private void updateGeneralStatistics(LocalDate startDate, LocalDate endDate) {
        generalStatsPanel.getChildren().clear();

        // Estadísticas generales
        Map<String, Object> generalStats = statisticsService.getGeneralStatistics(startDate, endDate);

        VBox statsBox = createStatsBox("Resumen General", Arrays.asList(
                new StatItem("Total de Transacciones", generalStats.get("totalTransactions").toString()),
                new StatItem("Monto Total Transferido", String.format("$%.2f", (Double) generalStats.get("totalAmount"))),
                new StatItem("Monto Promedio por Transacción", String.format("$%.2f", (Double) generalStats.get("averageAmount"))),
                new StatItem("Número de Cuentas Activas", generalStats.get("activeCounts").toString()),
                new StatItem("Transacciones por Día (Promedio)", String.format("%.1f", (Double) generalStats.get("transactionsPerDay")))
        ));

        // Estadísticas de flujo de dinero
        Map<String, Double> flowStats = statisticsService.getMoneyFlowStatistics(startDate, endDate);
        VBox flowBox = createStatsBox("Flujo de Dinero", Arrays.asList(
                new StatItem("Total Enviado", String.format("$%.2f", flowStats.get("totalSent"))),
                new StatItem("Total Recibido", String.format("$%.2f", flowStats.get("totalReceived"))),
                new StatItem("Diferencia de Flujo", String.format("$%.2f", flowStats.get("netFlow"))),
                new StatItem("Transacciones de Alto Valor (>$10,000)", flowStats.get("highValueCount").toString())
        ));

        generalStatsPanel.getChildren().addAll(statsBox, flowBox);
    }

    private void updateCharts(LocalDate startDate, LocalDate endDate) {
        chartPanel.getChildren().clear();

        // Gráfico de transacciones por día
        LineChart<String, Number> dailyChart = createDailyTransactionsChart(startDate, endDate);

        // Gráfico de montos por categoría
        PieChart amountCategoryChart = createAmountCategoryChart(startDate, endDate);

        // Gráfico de barras de cuentas más activas
        BarChart<String, Number> topAccountsChart = createTopAccountsChart(startDate, endDate);

        chartPanel.getChildren().addAll(
                new Label("Transacciones por Día"),
                dailyChart,
                new Separator(),
                new Label("Distribución de Montos por Categoría"),
                amountCategoryChart,
                new Separator(),
                new Label("Top 10 Cuentas Más Activas"),
                topAccountsChart
        );
    }

    private void updateAccountStatistics(LocalDate startDate, LocalDate endDate) {
        accountStatsPanel.getChildren().clear();

        // Tabla de estadísticas por cuenta
        TableView<AccountStatistic> accountTable = createAccountStatisticsTable(startDate, endDate);

        Label tableLabel = new Label("Estadísticas Detalladas por Cuenta");
        tableLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        accountStatsPanel.getChildren().addAll(tableLabel, accountTable);
    }

    private VBox createStatsBox(String title, List<StatItem> items) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        box.setPadding(new Insets(15));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        for (int i = 0; i < items.size(); i++) {
            StatItem item = items.get(i);

            Label keyLabel = new Label(item.key + ":");
            keyLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");

            Label valueLabel = new Label(item.value);
            valueLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");

            grid.add(keyLabel, 0, i);
            grid.add(valueLabel, 1, i);
        }

        box.getChildren().addAll(titleLabel, new Separator(), grid);
        return box;
    }

    private LineChart<String, Number> createDailyTransactionsChart(LocalDate startDate, LocalDate endDate) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Fecha");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Número de Transacciones");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Transacciones Diarias");
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Transacciones");

        Map<LocalDate, Integer> dailyStats = statisticsService.getDailyTransactionCount(startDate, endDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        dailyStats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getKey().format(formatter),
                            entry.getValue()
                    ));
                });

        chart.getData().add(series);
        return chart;
    }

    private PieChart createAmountCategoryChart(LocalDate startDate, LocalDate endDate) {
        PieChart chart = new PieChart();
        chart.setTitle("Distribución de Montos por Categoría");
        chart.setPrefHeight(300);

        Map<String, Double> categories = statisticsService.getAmountByCategory(startDate, endDate);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        categories.forEach((category, amount) -> {
            pieChartData.add(new PieChart.Data(
                    category + " ($" + String.format("%.0f", amount) + ")",
                    amount
            ));
        });

        chart.setData(pieChartData);
        return chart;
    }

    private BarChart<String, Number> createTopAccountsChart(LocalDate startDate, LocalDate endDate) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Cuenta");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Número de Transacciones");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Top 10 Cuentas Más Activas");
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Transacciones");

        List<Account> topAccounts = statisticsService.getTopActiveAccounts(startDate, endDate, 10);

        for (Account account : topAccounts) {
            int transactionCount = statisticsService.getTransactionCountForAccount(account, startDate, endDate);
            series.getData().add(new XYChart.Data<>(
                    account.getAccountNumber().substring(0, Math.min(8, account.getAccountNumber().length())) + "...",
                    transactionCount
            ));
        }

        chart.getData().add(series);
        return chart;
    }

    private TableView<AccountStatistic> createAccountStatisticsTable(LocalDate startDate, LocalDate endDate) {
        TableView<AccountStatistic> table = new TableView<>();
        table.setPrefHeight(400);

        // Columnas de la tabla
        TableColumn<AccountStatistic, String> accountCol = new TableColumn<>("Número de Cuenta");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("accountNumber"));
        accountCol.setPrefWidth(150);

        TableColumn<AccountStatistic, String> nameCol = new TableColumn<>("Titular");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("accountName"));
        nameCol.setPrefWidth(200);

        TableColumn<AccountStatistic, Integer> transactionsCol = new TableColumn<>("Transacciones");
        transactionsCol.setCellValueFactory(new PropertyValueFactory<>("transactionCount"));
        transactionsCol.setPrefWidth(100);

        TableColumn<AccountStatistic, String> totalSentCol = new TableColumn<>("Total Enviado");
        totalSentCol.setCellValueFactory(new PropertyValueFactory<>("totalSent"));
        totalSentCol.setPrefWidth(120);

        TableColumn<AccountStatistic, String> totalReceivedCol = new TableColumn<>("Total Recibido");
        totalReceivedCol.setCellValueFactory(new PropertyValueFactory<>("totalReceived"));
        totalReceivedCol.setPrefWidth(120);

        TableColumn<AccountStatistic, String> balanceCol = new TableColumn<>("Balance Neto");
        balanceCol.setCellValueFactory(new PropertyValueFactory<>("netBalance"));
        balanceCol.setPrefWidth(120);

        table.getColumns().addAll(accountCol, nameCol, transactionsCol, totalSentCol, totalReceivedCol, balanceCol);

        // Obtener datos y llenar la tabla
        List<AccountStatistic> accountStats = getAccountStatistics(startDate, endDate);
        table.setItems(FXCollections.observableArrayList(accountStats));

        return table;
    }

    private List<AccountStatistic> getAccountStatistics(LocalDate startDate, LocalDate endDate) {
        List<AccountStatistic> stats = new ArrayList<>();
        List<Account> accounts = statisticsService.getAllAccounts();

        for (Account account : accounts) {
            Map<String, Object> accountData = statisticsService.getAccountStatistics(account, startDate, endDate);

            AccountStatistic stat = new AccountStatistic(
                    account.getAccountNumber(),
                    account.getAccountHolderName(),
                    (Integer) accountData.get("transactionCount"),
                    String.format("$%.2f", (Double) accountData.get("totalSent")),
                    String.format("$%.2f", (Double) accountData.get("totalReceived")),
                    String.format("$%.2f", (Double) accountData.get("netBalance"))
            );
            stats.add(stat);
        }

        return stats.stream()
                .sorted((a, b) -> Integer.compare(b.getTransactionCount(), a.getTransactionCount()))
                .toList();
    }

    private void exportReport() {
        // Crear ventana de exportación
        Stage exportStage = new Stage();
        exportStage.initModality(Modality.APPLICATION_MODAL);
        exportStage.setTitle("Exportar Reporte");

        VBox exportBox = new VBox(15);
        exportBox.setPadding(new Insets(20));
        exportBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Exportar Reporte de Estadísticas");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("PDF", "Excel", "CSV");
        formatCombo.setValue("PDF");

        Button exportBtn = new Button("Exportar");
        exportBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        exportBtn.setOnAction(e -> {
            // Aquí iría la lógica de exportación
            showAlert("Información", "Función de exportación en desarrollo...");
            exportStage.close();
        });

        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setOnAction(e -> exportStage.close());

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(exportBtn, cancelBtn);

        exportBox.getChildren().addAll(titleLabel, formatCombo, buttonBox);

        Scene exportScene = new Scene(exportBox, 300, 200);
        exportStage.setScene(exportScene);
        exportStage.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Clase auxiliar para elementos de estadística
    private static class StatItem {
        final String key;
        final String value;

        StatItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    // Clase para estadísticas de cuenta
    public static class AccountStatistic {
        private String accountNumber;
        private String accountName;
        private Integer transactionCount;
        private String totalSent;
        private String totalReceived;
        private String netBalance;

        public AccountStatistic(String accountNumber, String accountName, Integer transactionCount,
                                String totalSent, String totalReceived, String netBalance) {
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.transactionCount = transactionCount;
            this.totalSent = totalSent;
            this.totalReceived = totalReceived;
            this.netBalance = netBalance;
        }

        // Getters
        public String getAccountNumber() { return accountNumber; }
        public String getAccountName() { return accountName; }
        public Integer getTransactionCount() { return transactionCount; }
        public String getTotalSent() { return totalSent; }
        public String getTotalReceived() { return totalReceived; }
        public String getNetBalance() { return netBalance; }

        // Setters
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
        public void setTotalSent(String totalSent) { this.totalSent = totalSent; }
        public void setTotalReceived(String totalReceived) { this.totalReceived = totalReceived; }
        public void setNetBalance(String netBalance) { this.netBalance = netBalance; }
    }
}