// BankingAnalyzerApp.java
package com.banking;

import com.banking.service.GraphService;
import com.banking.service.QueryService;
import com.banking.service.StatisticsService;
import com.banking.util.DataGenerator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class BankingAnalyzerApp extends Application {

    private static GraphService graphService;
    private static QueryService queryService;
    private static StatisticsService statisticsService;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Inicializar servicios
            initializeServices();

            // Generar datos de ejemplo
            generateSampleData();

            // Cargar la interfaz principal
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // Configurar CSS
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());

            // Configurar la ventana principal
            primaryStage.setTitle("Sistema de Análisis de Transacciones Bancarias");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);

            // Agregar icono si existe
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/banking-icon.png")));
            } catch (Exception e) {
                System.out.println("Icono no encontrado, continuando sin icono...");
            }

            // Configurar evento de cierre
            primaryStage.setOnCloseRequest(e -> {
                System.out.println("Cerrando aplicación de análisis bancario...");
                System.exit(0);
            });

            primaryStage.show();

        } catch (IOException e) {
            showErrorAlert("Error al inicializar la aplicación",
                    "No se pudo cargar la interfaz principal: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showErrorAlert("Error inesperado",
                    "Ocurrió un error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeServices() {
        graphService = new GraphService();
        queryService = new QueryService(graphService.getTransactionGraph());
        statisticsService = new StatisticsService(graphService.getTransactionGraph());

        System.out.println("Servicios inicializados correctamente");
    }

    private void generateSampleData() {
        try {
            DataGenerator.generateSampleData(graphService);
            System.out.println("Datos de ejemplo generados correctamente");
            System.out.println("Cuentas creadas: " + graphService.getTransactionGraph().getAccountCount());
            System.out.println("Transacciones creadas: " + graphService.getTransactionGraph().getTransactionCount());
        } catch (Exception e) {
            showErrorAlert("Error al generar datos",
                    "No se pudieron generar los datos de ejemplo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Getters estáticos para acceder a los servicios desde los controladores
    public static GraphService getGraphService() {
        return graphService;
    }

    public static QueryService getQueryService() {
        return queryService;
    }

    public static StatisticsService getStatisticsService() {
        return statisticsService;
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Aplicación detenida correctamente");
        super.stop();
    }

    public static void main(String[] args) {
        System.setProperty("javafx.platform.implicitExit", "false");

        // Configurar propiedades del sistema para mejor rendimiento
        System.setProperty("prism.vsync", "false");
        System.setProperty("javafx.animation.pulse", "60");

        System.out.println("Iniciando Sistema de Análisis de Transacciones Bancarias...");
        launch(args);
    }
}