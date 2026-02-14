package com.example.gestionvol.controller.user;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.service.CheckoutService;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Checkout CRUD operations (card-style UI)
 */
public class CheckoutController {

    @FXML private FlowPane cardsFlow;
    @FXML private StackPane checkoutCenterStack;
    @FXML private TextField searchField;
    @FXML private Button btnAddCheckout;
    @FXML private Button btnRefreshCheckout;

    private final CheckoutService checkoutService = new CheckoutService();
    private final ObservableList<Checkout> checkoutList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadCheckouts();
        setupSearch();
        // Responsive: adjust FlowPane wrap length to available width
        if (checkoutCenterStack != null && cardsFlow != null) {
            checkoutCenterStack.widthProperty().addListener((obs, oldW, newW) -> {
                double w = newW.doubleValue();
                cardsFlow.setPrefWrapLength(Math.max(320, w - 40));
            });
        }
        System.out.println("✅ Checkout Controller initialized");
    }

    private void loadCheckouts() {
        checkoutList.clear();
        List<Checkout> all = checkoutService.getAllCheckouts();
        checkoutList.addAll(all);
        renderCards();
    }

    private void renderCards() {
        if (cardsFlow == null) return;
        cardsFlow.getChildren().clear();
        for (Checkout c : checkoutList) {
            cardsFlow.getChildren().add(createCardForCheckout(c));
        }
    }

    private VBox createCardForCheckout(Checkout c) {
        VBox card = new VBox(8);
        card.getStyleClass().add("checkout-card");
        card.setPadding(new Insets(25));
        // Responsive card width: full-width on small screens, fixed on larger
        card.setPrefWidth(350);
        if (checkoutCenterStack != null) {
            card.prefWidthProperty().bind(Bindings.when(checkoutCenterStack.widthProperty().lessThan(700))
                .then(checkoutCenterStack.widthProperty().subtract(40))
                .otherwise(320));
        }



        Label lblDate = new Label("Reservation: " + c.getReservationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        lblDate.getStyleClass().add("card-field");

        Label lblPassengers = new Label("Passengers: " + c.getPassengerNbr());
        lblPassengers.getStyleClass().add("card-field");

        Label lblStatus = new Label("Status: " + c.getStatusReservation());
        lblStatus.getStyleClass().add("card-status");

        Label lblTotal = new Label("Total: " + c.getTotalPrix() + " DT");
        lblTotal.getStyleClass().add("card-total");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button edit = new Button("✏️");
        edit.getStyleClass().addAll("button","btn-edit");
        edit.setOnAction(e -> handleEditCheckout(c));

        Button del = new Button("🗑️");
        del.getStyleClass().addAll("button","btn-delete");
        del.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Delete this checkout?");
            a.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    if (checkoutService.deleteCheckout(c.getCheckoutId())) {
                        loadCheckouts();
                    }
                }
            });
        });

        actions.getChildren().addAll(edit, del);

        // Hover Animation
        javafx.animation.ScaleTransition scaleTransition = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), card);
        card.setOnMouseEntered(e -> {
            scaleTransition.setToX(1.05);
            scaleTransition.setToY(1.05);
            scaleTransition.playFromStart();
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 8); -fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 25; -fx-border-width: 0 0 0 6px; -fx-border-color: #50C878;");
        });
        card.setOnMouseExited(e -> {
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.playFromStart();
            card.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 12, 0, 0, 5); -fx-background-color: white; -fx-background-radius: 15; -fx-border-radius: 15; -fx-padding: 25; -fx-border-width: 0 0 0 6px; -fx-border-color: #50C878;");
        });

        card.getChildren().addAll(lblDate, lblPassengers, lblStatus, lblTotal, actions);
        return card;
    }

    private void setupSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.trim().isEmpty()) {
                renderCards();
            } else {
                String q = newValue.toLowerCase();
                ObservableList<Checkout> filtered = checkoutList.filtered(checkout ->
                        checkout.getStatusReservation().toLowerCase().contains(q) ||
                        String.valueOf(checkout.getCheckoutId()).contains(q) ||
                        String.valueOf(checkout.getIdUser()).contains(q)
                );
                cardsFlow.getChildren().clear();
                for (Checkout c : filtered) cardsFlow.getChildren().add(createCardForCheckout(c));
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadCheckouts();
        // Optional: Show brief alert or notification
    }

    @FXML
    private void handleAddCheckout() {
        navigateToForm(null);
    }
    
    private void handleEditCheckout(Checkout checkout) {
        navigateToForm(checkout);
    }

    private void navigateToForm(Checkout checkout) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/checkout-form-view.fxml"));
            Node formView = loader.load();
            
            CheckoutFormController controller = loader.getController();
            if (checkout != null) {
                controller.setCheckoutForEdit(checkout);
            }

            StackPane pageContainer = (StackPane) cardsFlow.getScene().lookup("#pageContainer");
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(formView);
            } else {
                System.err.println("Could not find #pageContainer to navigate");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setContentText("Could not load checkout form: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Confirm and submit the booking
     */
    @FXML
    private void handleConfirm() {
        System.out.println("✅ Booking confirmed");
        // TODO: Implement booking submission logic
    }

    /**
     * Go back to previous screen
     */
    @FXML
    private void handleBack() {
        System.out.println("⬅️ Back clicked");
        // TODO: Implement navigation back
    }

    /**
     * Static field to hold selected flight for checkout
     */
    private static com.example.gestionvol.entities.Flight selectedFlight;

    /**
     * Set the selected flight for checkout
     * @param flight The flight to select
     */
    public static void setSelectedFlight(com.example.gestionvol.entities.Flight flight) {
        selectedFlight = flight;
    }

    /**
     * Get the selected flight for checkout
     * @return The selected flight
     */
    public static com.example.gestionvol.entities.Flight getSelectedFlight() {
        return selectedFlight;
    }
}
