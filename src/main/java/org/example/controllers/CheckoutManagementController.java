package org.example.controllers;

import org.example.entities.Checkout;
import org.example.services.CheckoutService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class CheckoutManagementController {
    @FXML private FlowPane checkoutGrid;
    @FXML private StackPane rootStackPane;
    @FXML private ImageView bgImageView;

    private final CheckoutService checkoutService = new CheckoutService();

    @FXML
    public void initialize() {
        setupBackground();
        loadCheckouts();
    }

    @FXML
    public void handleRefresh() {
        loadCheckouts();
    }

    private void setupBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            
            var resourcePath = "/messages/home-hero5.png";
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                bgImageView.setEffect(new GaussianBlur(30));
            }
        }
    }

    private void loadCheckouts() {
        if (checkoutGrid == null) return;
        checkoutGrid.getChildren().clear();
        
        new Thread(() -> {
            try {
                final List<Checkout> list = checkoutService.getAllCheckouts();
                Platform.runLater(() -> {
                    for (Checkout c : list) {
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/checkout-card.fxml"));
                            VBox card = loader.load();
                            CheckoutCardController controller = loader.getController();
                            controller.setData(c, true, this::handleApprove, this::handleReject, this::handleCancel);
                            checkoutGrid.getChildren().add(card);
                        } catch (Exception e) { 
                            System.err.println("Error loading checkout card: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Checkout-Load-Thread").start();
    }

    @FXML
    public void handleDashboard() {
        org.example.utils.SceneNavigator.switchTo("/org/example/AdminDashboardView.fxml", checkoutGrid);
    }

    @FXML
    public void handlePersonnes() {
        org.example.utils.SceneNavigator.switchTo("/org/example/PersonneView.fxml", checkoutGrid);
    }

    @FXML
    public void handleVoitures() {
        org.example.utils.SceneNavigator.switchTo("/VoitureListView.fxml", checkoutGrid);
    }

    @FXML
    public void handleLocations() {
        org.example.utils.SceneNavigator.switchTo("/AdminLocationListView.fxml", checkoutGrid);
    }

    @FXML
    public void handleFlights() {
        org.example.utils.SceneNavigator.switchTo("/views/flight-management.fxml", checkoutGrid);
    }

    @FXML
    public void handleLogout() {
        org.example.utils.SessionManager.clear();
        org.example.utils.SceneNavigator.switchTo("/org/example/LoginView.fxml", checkoutGrid);
    }

    private void handleApprove(Checkout c) {
        new Thread(() -> {
            if (checkoutService.approveCheckout(c.getCheckoutId(), 1)) {
                Platform.runLater(this::loadCheckouts);
            }
        }).start();
    }

    private void handleReject(Checkout c) {
        new Thread(() -> {
            if (checkoutService.rejectCheckout(c.getCheckoutId(), 1, "Rejected by Admin")) {
                Platform.runLater(this::loadCheckouts);
            }
        }).start();
    }

    private void handleCancel(Checkout c) {
        new Thread(() -> {
            if (checkoutService.cancelCheckout(c.getCheckoutId(), 1)) {
                Platform.runLater(this::loadCheckouts);
            }
        }).start();
    }
}
