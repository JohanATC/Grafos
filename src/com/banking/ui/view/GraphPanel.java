package com.banking.ui.view;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionGraph;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Panel interactivo para la visualización del grafo de transacciones bancarias
 */
public class GraphPanel extends VBox {
    private TransactionGraph transactionGraph;
    private Canvas canvas;
    private GraphicsContext gc;
    private ScrollPane scrollPane;
    private ToolBar toolBar;

    // Configuración de visualización
    private double zoomLevel = 1.0;
    private final double MIN_ZOOM = 0.1;
    private final double MAX_ZOOM = 3.0;
    private final double ZOOM_FACTOR = 0.1;

    // Posiciones de nodos
    private Map<String, Point2D> nodePositions;
    private final double NODE_RADIUS = 30.0;
    private final double CANVAS_WIDTH = 1200.0;
    private final double CANVAS_HEIGHT = 800.0;

    // Colores del tema
    private final Color BACKGROUND_COLOR = Color.web("#f5f5f5");
    private final Color NODE_COLOR = Color.web("#4a90e2");
    private final Color NODE_BORDER_COLOR = Color.web("#2c5aa0");
    private final Color EDGE_COLOR = Color.web("#666666");
    private final Color TEXT_COLOR = Color.web("#333333");
    private final Color HIGHLIGHT_COLOR = Color.web("#ff6b6b");

    // Estado de selección
    private String selectedNodeId = null;
    private Transaction selectedTransaction = null;

    private DecimalFormat moneyFormatter = new DecimalFormat("$#,##0.00");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public GraphPanel(TransactionGraph transactionGraph) {
        this.transactionGraph = transactionGraph;
        this.nodePositions = new HashMap<>();

        initializeComponents();
        setupLayout();
        setupEventHandlers();
        calculateNodePositions();
        redrawGraph();
    }

    private void initializeComponents() {
        // Canvas principal
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        // ScrollPane para navegación
        scrollPane = new ScrollPane(canvas);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Barra de herramientas
        createToolBar();
    }

    private void createToolBar() {
        toolBar = new ToolBar();

        // Botones de zoom
        Button zoomInBtn = new Button("Zoom +");
        Button zoomOutBtn = new Button("Zoom -");
        Button resetZoomBtn = new Button("Reset Zoom");

        // Botón para centrar vista
        Button centerBtn = new Button("Centrar Vista");

        // Botón para reorganizar nodos
        Button reorganizeBtn = new Button("Reorganizar");

        // ComboBox para filtros
        Label filterLabel = new Label("Filtrar por monto:");
        ComboBox<String> filterCombo = new ComboBox<>();
        filterCombo.getItems().addAll("Todos", "> $1,000", "> $5,000", "> $10,000");
        filterCombo.setValue("Todos");

        // Configurar eventos de botones
        zoomInBtn.setOnAction(e -> zoomIn());
        zoomOutBtn.setOnAction(e -> zoomOut());
        resetZoomBtn.setOnAction(e -> resetZoom());
        centerBtn.setOnAction(e -> centerView());
        reorganizeBtn.setOnAction(e -> reorganizeNodes());
        filterCombo.setOnAction(e -> applyFilter(filterCombo.getValue()));

        toolBar.getItems().addAll(
                zoomInBtn, zoomOutBtn, resetZoomBtn,
                new Separator(),
                centerBtn, reorganizeBtn,
                new Separator(),
                filterLabel, filterCombo
        );
    }

    private void setupLayout() {
        this.setSpacing(5);
        this.setPadding(new Insets(10));

        // Añadir componentes al panel principal
        this.getChildren().addAll(toolBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }

    private void setupEventHandlers() {
        // Eventos del canvas
        canvas.setOnMouseClicked(this::handleCanvasClick);
        canvas.setOnMouseMoved(this::handleCanvasMouseMove);

        // Eventos de scroll para zoom
        canvas.setOnScroll(e -> {
            if (e.isControlDown()) {
                if (e.getDeltaY() > 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
            }
        });
    }

    private void calculateNodePositions() {
        Set<Account> accounts = transactionGraph.getAccounts();
        if (accounts.isEmpty()) return;

        nodePositions.clear();

        // Disposición circular para mejor visualización
        double centerX = CANVAS_WIDTH / 2;
        double centerY = CANVAS_HEIGHT / 2;
        double radius = Math.min(CANVAS_WIDTH, CANVAS_HEIGHT) / 3;

        int nodeCount = accounts.size();
        double angleStep = 2 * Math.PI / nodeCount;

        int index = 0;
        for (Account account : accounts) {
            double angle = index * angleStep;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            nodePositions.put(account.getAccountId(), new Point2D(x, y));
            index++;
        }
    }

    private void redrawGraph() {
        // Limpiar canvas
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        gc.setFill(BACKGROUND_COLOR);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Aplicar zoom
        gc.save();
        gc.scale(zoomLevel, zoomLevel);

        // Dibujar aristas (transacciones)
        drawEdges();

        // Dibujar nodos (cuentas)
        drawNodes();

        gc.restore();
    }

    private void drawEdges() {
        gc.setStroke(EDGE_COLOR);
        gc.setLineWidth(2.0);

        for (Transaction transaction : transactionGraph.getTransactions()) {
            Point2D fromPos = nodePositions.get(transaction.getFromAccount());
            Point2D toPos = nodePositions.get(transaction.getToAccount());

            if (fromPos != null && toPos != null) {
                // Configurar color basado en el monto
                double amount = transaction.getAmount();
                if (amount > 10000) {
                    gc.setStroke(Color.RED);
                    gc.setLineWidth(4.0);
                } else if (amount > 5000) {
                    gc.setStroke(Color.ORANGE);
                    gc.setLineWidth(3.0);
                } else {
                    gc.setStroke(EDGE_COLOR);
                    gc.setLineWidth(2.0);
                }

                // Destacar transacción seleccionada
                if (selectedTransaction != null && selectedTransaction.equals(transaction)) {
                    gc.setStroke(HIGHLIGHT_COLOR);
                    gc.setLineWidth(5.0);
                }

                // Dibujar línea con flecha
                drawArrow(fromPos, toPos);

                // Dibujar etiqueta con monto
                drawEdgeLabel(fromPos, toPos, moneyFormatter.format(amount));
            }
        }
    }

    private void drawArrow(Point2D from, Point2D to) {
        // Calcular puntos ajustados para que no se superpongan con los nodos
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance == 0) return;

        double unitX = dx / distance;
        double unitY = dy / distance;

        Point2D adjustedFrom = new Point2D(
                from.getX() + unitX * NODE_RADIUS,
                from.getY() + unitY * NODE_RADIUS
        );

        Point2D adjustedTo = new Point2D(
                to.getX() - unitX * NODE_RADIUS,
                to.getY() - unitY * NODE_RADIUS
        );

        // Dibujar línea principal
        gc.strokeLine(adjustedFrom.getX(), adjustedFrom.getY(),
                adjustedTo.getX(), adjustedTo.getY());

        // Dibujar punta de flecha
        double arrowLength = 15;
        double arrowAngle = Math.PI / 6;

        double angle = Math.atan2(dy, dx);

        double arrowX1 = adjustedTo.getX() - arrowLength * Math.cos(angle - arrowAngle);
        double arrowY1 = adjustedTo.getY() - arrowLength * Math.sin(angle - arrowAngle);
        double arrowX2 = adjustedTo.getX() - arrowLength * Math.cos(angle + arrowAngle);
        double arrowY2 = adjustedTo.getY() - arrowLength * Math.sin(angle + arrowAngle);

        gc.strokeLine(adjustedTo.getX(), adjustedTo.getY(), arrowX1, arrowY1);
        gc.strokeLine(adjustedTo.getX(), adjustedTo.getY(), arrowX2, arrowY2);
    }

    private void drawEdgeLabel(Point2D from, Point2D to, String label) {
        double midX = (from.getX() + to.getX()) / 2;
        double midY = (from.getY() + to.getY()) / 2;

        gc.setFill(Color.WHITE);
        gc.fillRoundRect(midX - 20, midY - 8, 40, 16, 5, 5);

        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        gc.fillText(label, midX - 15, midY + 3);
    }

    private void drawNodes() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        for (Account account : transactionGraph.getAccounts()) {
            Point2D pos = nodePositions.get(account.getAccountId());
            if (pos == null) continue;

            // Configurar colores
            Color nodeColor = NODE_COLOR;
            Color borderColor = NODE_BORDER_COLOR;

            if (account.getAccountId().equals(selectedNodeId)) {
                nodeColor = HIGHLIGHT_COLOR;
                borderColor = HIGHLIGHT_COLOR.darker();
            }

            // Dibujar círculo del nodo
            gc.setFill(nodeColor);
            gc.fillOval(pos.getX() - NODE_RADIUS, pos.getY() - NODE_RADIUS,
                    NODE_RADIUS * 2, NODE_RADIUS * 2);

            gc.setStroke(borderColor);
            gc.setLineWidth(3.0);
            gc.strokeOval(pos.getX() - NODE_RADIUS, pos.getY() - NODE_RADIUS,
                    NODE_RADIUS * 2, NODE_RADIUS * 2);

            // Dibujar ID de la cuenta
            gc.setFill(Color.WHITE);
            gc.fillText(account.getAccountId(),
                    pos.getX() - 15, pos.getY() + 5);

            // Dibujar información adicional debajo del nodo
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
            gc.fillText(account.getOwner(),
                    pos.getX() - 20, pos.getY() + NODE_RADIUS + 15);
            gc.fillText(moneyFormatter.format(account.getBalance()),
                    pos.getX() - 25, pos.getY() + NODE_RADIUS + 30);
        }
    }

    private void handleCanvasClick(MouseEvent event) {
        double x = event.getX() / zoomLevel;
        double y = event.getY() / zoomLevel;

        // Verificar si se hizo clic en un nodo
        String clickedNodeId = getNodeAtPosition(x, y);
        if (clickedNodeId != null) {
            selectedNodeId = clickedNodeId;
            showNodeDetails(clickedNodeId);
            redrawGraph();
            return;
        }

        // Verificar si se hizo clic en una arista
        Transaction clickedTransaction = getTransactionAtPosition(x, y);
        if (clickedTransaction != null) {
            selectedTransaction = clickedTransaction;
            showTransactionDetails(clickedTransaction);
            redrawGraph();
            return;
        }

        // Limpiar selección
        selectedNodeId = null;
        selectedTransaction = null;
        redrawGraph();
    }

    private void handleCanvasMouseMove(MouseEvent event) {
        double x = event.getX() / zoomLevel;
        double y = event.getY() / zoomLevel;

        // Cambiar cursor si está sobre un elemento interactivo
        String nodeId = getNodeAtPosition(x, y);
        Transaction transaction = getTransactionAtPosition(x, y);

        if (nodeId != null || transaction != null) {
            canvas.setCursor(javafx.scene.Cursor.HAND);
        } else {
            canvas.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private String getNodeAtPosition(double x, double y) {
        for (Map.Entry<String, Point2D> entry : nodePositions.entrySet()) {
            Point2D pos = entry.getValue();
            double distance = Math.sqrt(Math.pow(x - pos.getX(), 2) + Math.pow(y - pos.getY(), 2));
            if (distance <= NODE_RADIUS) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Transaction getTransactionAtPosition(double x, double y) {
        for (Transaction transaction : transactionGraph.getTransactions()) {
            Point2D fromPos = nodePositions.get(transaction.getFromAccount());
            Point2D toPos = nodePositions.get(transaction.getToAccount());

            if (fromPos != null && toPos != null) {
                double distance = distanceToLine(x, y, fromPos, toPos);
                if (distance <= 10) { // 10 pixels de tolerancia
                    return transaction;
                }
            }
        }
        return null;
    }

    private double distanceToLine(double px, double py, Point2D p1, Point2D p2) {
        double A = px - p1.getX();
        double B = py - p1.getY();
        double C = p2.getX() - p1.getX();
        double D = p2.getY() - p1.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = (lenSq != 0) ? dot / lenSq : -1;

        double xx, yy;

        if (param < 0) {
            xx = p1.getX();
            yy = p1.getY();
        } else if (param > 1) {
            xx = p2.getX();
            yy = p2.getY();
        } else {
            xx = p1.getX() + param * C;
            yy = p1.getY() + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void showNodeDetails(String accountId) {
        Account account = transactionGraph.getAccounts().stream()
                .filter(acc -> acc.getAccountId().equals(accountId))
                .findFirst().orElse(null);

        if (account == null) return;

        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.setTitle("Detalles de la Cuenta");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
                new Label("ID de Cuenta: " + account.getAccountId()),
                new Label("Propietario: " + account.getOwner()),
                new Label("Balance: " + moneyFormatter.format(account.getBalance())),
                new Label("Tipo: " + account.getAccountType())
        );

        Button closeBtn = new Button("Cerrar");
        closeBtn.setOnAction(e -> detailsStage.close());
        content.getChildren().add(closeBtn);

        Scene scene = new Scene(content, 300, 200);
        detailsStage.setScene(scene);
        detailsStage.show();
    }

    private void showTransactionDetails(Transaction transaction) {
        Stage detailsStage = new Stage();
        detailsStage.initModality(Modality.APPLICATION_MODAL);
        detailsStage.setTitle("Detalles de la Transacción");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        content.getChildren().addAll(
                new Label("ID: " + transaction.getTransactionId()),
                new Label("De: " + transaction.getFromAccount()),
                new Label("Para: " + transaction.getToAccount()),
                new Label("Monto: " + moneyFormatter.format(transaction.getAmount())),
                new Label("Fecha: " + transaction.getTimestamp().format(dateFormatter)),
                new Label("Tipo: " + transaction.getTransactionType()),
                new Label("Descripción: " + transaction.getDescription())
        );

        Button closeBtn = new Button("Cerrar");
        closeBtn.setOnAction(e -> detailsStage.close());
        content.getChildren().add(closeBtn);

        Scene scene = new Scene(content, 350, 250);
        detailsStage.setScene(scene);
        detailsStage.show();
    }

    // Métodos de control de zoom y vista
    private void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_FACTOR);
            redrawGraph();
        }
    }

    private void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_FACTOR);
            redrawGraph();
        }
    }

    private void resetZoom() {
        zoomLevel = 1.0;
        redrawGraph();
    }

    private void centerView() {
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
    }

    private void reorganizeNodes() {
        calculateNodePositions();
        redrawGraph();
    }

    private void applyFilter(String filterType) {
        // Implementar lógica de filtrado
        // Por ahora solo redibuja el grafo
        redrawGraph();
    }

    // Método público para actualizar el grafo
    public void updateGraph(TransactionGraph newGraph) {
        this.transactionGraph = newGraph;
        this.selectedNodeId = null;
        this.selectedTransaction = null;
        calculateNodePositions();
        redrawGraph();
    }

    // Métodos públicos para interacción externa
    public void highlightAccount(String accountId) {
        this.selectedNodeId = accountId;
        redrawGraph();
    }

    public void highlightTransaction(Transaction transaction) {
        this.selectedTransaction = transaction;
        redrawGraph();
    }

    public void clearSelection() {
        this.selectedNodeId = null;
        this.selectedTransaction = null;
        redrawGraph();
    }
}