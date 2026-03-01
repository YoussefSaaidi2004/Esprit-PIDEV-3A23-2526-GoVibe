package org.example.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import org.example.entities.Checkout;
import org.example.entities.personne;
import org.example.services.CheckoutService;
import org.example.utils.SessionManager;

import java.util.List;

/**
 * Controller for the user-facing "Mes Réservations" page.
 * Loads only the current user's flight reservations (not all users).
 */
public class UserCheckoutsController {

    @FXML private StackPane rootStackPane;
    @FXML private ImageView bgImageView;
    @FXML private FlowPane checkoutGrid;
    @FXML private Label lblTotal;

    private final CheckoutService checkoutService = new CheckoutService();

    @FXML
    public void initialize() {
        setupBackground();
        loadUserCheckouts();
    }

    @FXML
    public void handleRefresh() {
        loadUserCheckouts();
    }

    private void setupBackground() {
        if (bgImageView != null && rootStackPane != null) {
            bgImageView.fitWidthProperty().bind(rootStackPane.widthProperty());
            bgImageView.fitHeightProperty().bind(rootStackPane.heightProperty());
            var url = getClass().getResource("/messages/home-hero5.png");
            if (url != null) {
                Image img = new Image(url.toExternalForm(), true);
                bgImageView.setImage(img);
                bgImageView.setEffect(new GaussianBlur(22));
            }
        }
    }

    private void loadUserCheckouts() {
        if (checkoutGrid == null) return;
        checkoutGrid.getChildren().clear();

        personne user = SessionManager.getCurrentUser();
        if (user == null) {
            System.err.println("[UserCheckoutsController] No user logged in.");
            return;
        }

        int userId = user.getId();

        new Thread(() -> {
            try {
                final List<Checkout> list = checkoutService.getCheckoutsByUserId(userId);
                Platform.runLater(() -> {
                    if (lblTotal != null) lblTotal.setText(String.valueOf(list.size()));
                    for (Checkout c : list) {
                        try {
                            FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/views/checkout-card.fxml"));
                            VBox card = loader.load();
                            CheckoutCardController controller = loader.getController();
                            // isAdmin = false → shows user-appropriate controls only
                            controller.setData(c, false,
                                ch -> {}, // approve (n/a for user)
                                ch -> {}, // reject (n/a for user)
                                ch -> loadUserCheckouts()); // cancel → refresh
                            checkoutGrid.getChildren().add(card);
                        } catch (Exception e) {
                            System.err.println("Error loading checkout card: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "UserCheckout-Load-Thread").start();
    }
}
