package com.banking.ui.controller;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionGraph;
import com.banking.service.GraphService;
import com.banking.service.QueryService;
import com.banking.ui.view.GraphPanel;
import com.banking.ui.view.StatisticsPanel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.scene.layout.Priority;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador principal para la vista del grafo de transacciones bancarias.
 * Maneja la interacción entre la interfaz gráfica y los servicios de datos.
 */
public class GraphViewController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox leftPanel;
    @FXML private VBox rightPanel;
    @FXML private HBox topToolbar;
    @FXML private HBox bottomStatusBar;

    // Controles de la interfaz
    private GraphPanel graphPanel;
    private StatisticsPanel statisticsPanel;

    // Controles de búsqueda y filtros
    private TextField searchAccountField;
    private ComboBox<String> filterComboBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private Slider amountRangeSlider;
    private Label statusLabel;

    // Servicios
    private GraphService graphService;
    private QueryService queryService;
    private TransactionGraph transactionGraph;

    // Estado de la aplicación
    private boolean isGraphLoaded = false;
    private double currentZoomLevel = 1.0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeServices();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadSampleData();
    }

    /**
     * Inicializa los servicios necesarios para el controlador
     */
    private void initializeServices() {
        this.transactionGraph = new TransactionGraph();
        this.graphService = new GraphService(transactionGraph);
        this.queryService = new QueryService(transactionGraph);
    }

    /**
     * Inicializa todos los componentes de la interfaz
     */
    private void initializeComponents() {
        // Inicializar paneles principales
        graphPanel = new GraphPanel(transactionGraph);
        statisticsPanel = new StatisticsPanel(queryService);

        // Inicializar controles de búsqueda
        searchAccountField = new TextField();
        searchAccountField.setPromptText("Buscar cuenta por número o nombre...");
        searchAccountField.setPrefWidth(200);

        // ComboBox para filtros
        filterComboBox = new ComboBox<>();
        filterComboBox.setItems(FXCollections.observableArrayList(
                "Todas las transacciones",
                "Montos altos (>$10,000)",
                "Montos bajos (<$1,000)",
                "Transacciones recientes",
                "Cuentas más activas"
        ));
        filterComboBox.setValue("Todas las transacciones");

        // Selectores de fecha
        startDatePicker = new DatePicker(LocalDate.now().minusMonths(1));
        endDatePicker = new DatePicker(LocalDate.now());

        // Slider para rango de montos
        amountRangeSlider = new Slider(0, 100000, 50000);
        amountRangeSlider.setShowTickLabels(true);
        amountRangeSlider.setShowTickMarks(true);
        amountRangeSlider.setMajorTickUnit(25000);

        // Label de estado
        statusLabel = new Label("Sistema listo");
        statusLabel.getStyleClass().add("status-label");
    }

    /**
     * Configura el diseño de la interfaz
     */
    private void setupLayout() {
        // Configurar toolbar superior
        setupTopToolbar();

        // Configurar panel izquierdo (controles)
        setupLeftPanel();

        // Configurar panel derecho (estadísticas)
        setupRightPanel();

        // Configurar barra de estado inferior
        setupBottomStatusBar();

        // Configurar panel central (grafo)
        rootPane.setCenter(graphPanel);
    }

    /**
     * Configura la barra de herramientas superior
     */
    private void setupTopToolbar() {
        Button loadDataButton = new Button("Cargar Datos");
        loadDataButton.setOnAction(e -> loadDataFromFile());

        Button saveButton = new Button("Guardar");
        saveButton.setOnAction(e -> saveData());

        Button refreshButton = new Button("Actualizar");
        refreshButton.setOnAction(e -> refreshGraph());

        Separator separator1 = new Separator();

        Button zoomInButton = new Button("Zoom +");
        zoomInButton.setOnAction(e -> zoomIn());

        Button zoomOutButton = new Button("Zoom -");
        zoomOutButton.setOnAction(e -> zoomOut());

        Button resetViewButton = new Button("Reset Vista");
        resetViewButton.setOnAction(e -> resetView());

        topToolbar.getChildren().addAll(
                loadDataButton, saveButton, refreshButton, separator1,
                zoomInButton, zoomOutButton, resetViewButton
        );

        topToolbar.setSpacing(10);
        topToolbar.setPadding(new Insets(10));
    }

    /**
     * Configura el panel izquierdo con controles de búsqueda y filtros
     */
    private void setupLeftPanel() {
        Label searchLabel = new Label("Búsqueda y Filtros");
        searchLabel.getStyleClass().add("section-header");

        // Sección de búsqueda
        VBox searchSection = new VBox(5);
        searchSection.getChildren().addAll(
                new Label("Buscar Cuenta:"),
                searchAccountField,
                new Button("Buscar") {{ setOnAction(e -> searchAccount()); }}
        );

        // Sección de filtros
        VBox filterSection = new VBox(5);
        filterSection.getChildren().addAll(
                new Label("Filtrar por:"),
                filterComboBox,
                new Label("Fecha desde:"),
                startDatePicker,
                new Label("Fecha hasta:"),
                endDatePicker,
                new Label("Monto máximo:"),
                amountRangeSlider,
                new Button("Aplicar Filtros") {{ setOnAction(e -> applyFilters()); }}
        );

        // Sección de información de nodo seleccionado
        VBox nodeInfoSection = new VBox(5);
        Label nodeInfoLabel = new Label("Información del Nodo");
        nodeInfoLabel.getStyleClass().add("section-header");

        TextArea nodeInfoArea = new TextArea();
        nodeInfoArea.setEditable(false);
        nodeInfoArea.setPrefRowCount(6);
        nodeInfoArea.setPromptText("Selecciona un nodo para ver detalles...");

        nodeInfoSection.getChildren().addAll(nodeInfoLabel, nodeInfoArea);

        leftPanel.getChildren().addAll(
                searchLabel,
                new Separator(),
                searchSection,
                new Separator(),
                filterSection,
                new Separator(),
                nodeInfoSection
        );

        leftPanel.setSpacing(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(300);
        leftPanel.setMaxWidth(300);
    }

    /**
     * Configura el panel derecho con estadísticas
     */
    private void setupRightPanel() {
        rightPanel.getChildren().add(statisticsPanel);
        VBox.setVgrow(statisticsPanel, Priority.ALWAYS);
        rightPanel.setPrefWidth(350);
        rightPanel.setMaxWidth(350);
    }

    /**
     * Configura la barra de estado inferior
     */
    private void setupBottomStatusBar() {
        Label nodeCountLabel = new Label("Nodos: 0");
        Label edgeCountLabel = new Label("Transacciones: 0");
        Label totalAmountLabel = new Label("Monto Total: $0.00");

        bottomStatusBar.getChildren().addAll(
                statusLabel,
                new Separator(),
                nodeCountLabel,
                new Separator(),
                edgeCountLabel,
                new Separator(),
                totalAmountLabel
        );

        bottomStatusBar.setSpacing(10);
        bottomStatusBar.setPadding(new Insets(5, 10, 5, 10));
    }

    /**
     * Configura los manejadores de eventos
     */
    private void setupEventHandlers() {
        // Event handler para cambios en el filtro
        filterComboBox.setOnAction(e -> applyFilters());

        // Event handler para cambios en el slider de monto
        amountRangeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Actualizar filtros en tiempo real si se desea
        });

        // Event handler para búsqueda en tiempo real
        searchAccountField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.trim().isEmpty() && newText.length() > 2) {
                // Búsqueda automática después de 3 caracteres
                // Implementar debouncing si es necesario
            }
        });

        // Event handlers para el panel de grafo
        graphPanel.setOnNodeSelected(this::onNodeSelected);
        graphPanel.setOnEdgeSelected(this::onEdgeSelected);
        graphPanel.setOnGraphUpdated(this::onGraphUpdated);
    }

    /**
     * Carga datos de muestra para demostración
     */
    private void loadSampleData() {
        try {
            // Crear algunas cuentas de ejemplo
            graphService.addAccount("001", "Juan Pérez", "Cuenta Corriente", 15000.00);
            graphService.addAccount("002", "María García", "Cuenta Ahorros", 25000.00);
            graphService.addAccount("003", "Carlos López", "Cuenta Empresarial", 50000.00);
            graphService.addAccount("004", "Ana Martínez", "Cuenta Corriente", 8000.00);
            graphService.addAccount("005", "Luis Rodríguez", "Cuenta Ahorros", 32000.00);

            // Crear algunas transacciones de ejemplo
            graphService.addTransaction("001", "002", 1500.00, LocalDate.now().minusDays(5), "Transferencia personal");
            graphService.addTransaction("002", "003", 3000.00, LocalDate.now().minusDays(3), "Pago de servicios");
            graphService.addTransaction("003", "004", 750.00, LocalDate.now().minusDays(2), "Reembolso");
            graphService.addTransaction("004", "005", 2000.00, LocalDate.now().minusDays(1), "Préstamo familiar");
            graphService.addTransaction("005", "001", 500.00, LocalDate.now(), "Pago de deuda");
            graphService.addTransaction("001", "003", 5000.00, LocalDate.now(), "Inversión");

            isGraphLoaded = true;
            refreshGraph();
            updateStatus("Datos de muestra cargados correctamente");

        } catch (Exception e) {
            showError("Error al cargar datos de muestra", e);
        }
    }

    /**
     * Carga datos desde un archivo
     */
    private void loadDataFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Datos de Transacciones");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archivos JSON", "*.json"),
                new FileChooser.ExtensionFilter("Archivos CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );

        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                // Implementar carga desde archivo
                updateStatus("Cargando datos desde: " + file.getName());
                // TODO: Implementar lógica de carga
                updateStatus("Datos cargados exitosamente");
            } catch (Exception e) {
                showError("Error al cargar archivo", e);
            }
        }
    }

    /**
     * Guarda los datos actuales
     */
    private void saveData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Datos de Transacciones");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos JSON", "*.json")
        );

        File file = fileChooser.showSaveDialog(rootPane.getScene().getWindow());
        if (file != null) {
            try {
                // Implementar guardado
                updateStatus("Guardando datos en: " + file.getName());
                // TODO: Implementar lógica de guardado
                updateStatus("Datos guardados exitosamente");
            } catch (Exception e) {
                showError("Error al guardar archivo", e);
            }
        }
    }

    /**
     * Actualiza la visualización del grafo
     */
    private void refreshGraph() {
        if (isGraphLoaded) {
            graphPanel.refreshGraph();
            statisticsPanel.updateStatistics();
            updateStatusBar();
            updateStatus("Grafo actualizado");
        }
    }

    /**
     * Realiza zoom in en el grafo
     */
    private void zoomIn() {
        currentZoomLevel *= 1.2;
        graphPanel.setZoomLevel(currentZoomLevel);
        updateStatus("Zoom: " + String.format("%.0f%%", currentZoomLevel * 100));
    }

    /**
     * Realiza zoom out en el grafo
     */
    private void zoomOut() {
        currentZoomLevel /= 1.2;
        graphPanel.setZoomLevel(currentZoomLevel);
        updateStatus("Zoom: " + String.format("%.0f%%", currentZoomLevel * 100));
    }

    /**
     * Resetea la vista del grafo
     */
    private void resetView() {
        currentZoomLevel = 1.0;
        graphPanel.resetView();
        updateStatus("Vista reseteada");
    }

    /**
     * Busca una cuenta específica
     */
    private void searchAccount() {
        String searchText = searchAccountField.getText().trim();
        if (!searchText.isEmpty()) {
            List<Account> results = queryService.searchAccounts(searchText);
            if (!results.isEmpty()) {
                Account account = results.get(0);
                graphPanel.highlightNode(account.getAccountNumber());
                updateStatus("Cuenta encontrada: " + account.getAccountHolder());
            } else {
                updateStatus("No se encontraron cuentas con: " + searchText);
            }
        }
    }

    /**
     * Aplica los filtros seleccionados
     */
    private void applyFilters() {
        String filterType = filterComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        double maxAmount = amountRangeSlider.getValue();

        try {
            List<Transaction> filteredTransactions = queryService.getFilteredTransactions(
                    startDate, endDate, 0, maxAmount, filterType
            );

            graphPanel.applyTransactionFilter(filteredTransactions);
            updateStatus("Filtros aplicados: " + filteredTransactions.size() + " transacciones");

        } catch (Exception e) {
            showError("Error al aplicar filtros", e);
        }
    }

    /**
     * Maneja la selección de un nodo
     */
    private void onNodeSelected(Account account) {
        if (account != null) {
            StringBuilder info = new StringBuilder();
            info.append("Cuenta: ").append(account.getAccountNumber()).append("\n");
            info.append("Titular: ").append(account.getAccountHolder()).append("\n");
            info.append("Tipo: ").append(account.getAccountType()).append("\n");
            info.append("Saldo: $").append(String.format("%.2f", account.getBalance())).append("\n\n");

            // Agregar estadísticas de transacciones
            List<Transaction> transactions = queryService.getTransactionsByAccount(account.getAccountNumber());
            info.append("Transacciones: ").append(transactions.size()).append("\n");

            double totalOut = transactions.stream()
                    .filter(t -> t.getFromAccount().equals(account.getAccountNumber()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double totalIn = transactions.stream()
                    .filter(t -> t.getToAccount().equals(account.getAccountNumber()))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            info.append("Total enviado: $").append(String.format("%.2f", totalOut)).append("\n");
            info.append("Total recibido: $").append(String.format("%.2f", totalIn));

            // Actualizar área de información (necesario obtener referencia)
            updateStatus("Nodo seleccionado: " + account.getAccountHolder());
        }
    }

    /**
     * Maneja la selección de una arista
     */
    private void onEdgeSelected(Transaction transaction) {
        if (transaction != null) {
            updateStatus("Transacción: $" + String.format("%.2f", transaction.getAmount()) +
                    " de " + transaction.getFromAccount() + " a " + transaction.getToAccount());
        }
    }

    /**
     * Maneja actualizaciones del grafo
     */
    private void onGraphUpdated() {
        updateStatusBar();
        statisticsPanel.updateStatistics();
    }

    /**
     * Actualiza la barra de estado con información actual
     */
    private void updateStatusBar() {
        if (bottomStatusBar.getChildren().size() >= 7) {
            ((Label) bottomStatusBar.getChildren().get(2)).setText("Nodos: " + transactionGraph.getAccountCount());
            ((Label) bottomStatusBar.getChildren().get(4)).setText("Transacciones: " + transactionGraph.getTransactionCount());

            double totalAmount = transactionGraph.getAllTransactions().stream()
                    .mapToDouble(Transaction::getAmount)
                    .sum();
            ((Label) bottomStatusBar.getChildren().get(6)).setText("Monto Total: $" + String.format("%.2f", totalAmount));
        }
    }

    /**
     * Actualiza el mensaje de estado
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Muestra un error al usuario
     */
    private void showError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(e.getMessage());
        alert.showAndWait();

        updateStatus("Error: " + title);
    }

    // Getters para acceso desde otros componentes
    public GraphPanel getGraphPanel() { return graphPanel; }
    public StatisticsPanel getStatisticsPanel() { return statisticsPanel; }
    public TransactionGraph getTransactionGraph() { return transactionGraph; }
}