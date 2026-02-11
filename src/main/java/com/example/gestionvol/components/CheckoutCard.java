package com.example.gestionvol.components;

import com.example.gestionvol.entities.Checkout;
import com.example.gestionvol.entities.Flight;
import com.example.gestionvol.service.FlightService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.function.Consumer;

public class CheckoutCard extends VBox {
    private final FlightService flightService = new FlightService();

    public CheckoutCard(Checkout checkout, Consumer<Checkout> onApprove, Consumer<Checkout> onReject, Consumer<Checkout> onCancel) {
        setSpacing(10);
        setPadding(new Insets(20));
        getStyleClass().add("checkout-card");
        setPrefWidth(350);

        Flight flight = flightService.getFlightById(checkout.getFlightId());
        String routeStr = (flight != null) ? flight.getDepartureAirport() + " ✈ " + flight.getDestination() : "Unknown Flight";

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblRoute = new Label(routeStr);
        lblRoute.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.getChildren().add(lblRoute);

        HBox info = new HBox(15);
        Label lblDate = new Label("📅 " + checkout.getReservationDate().toLocalDate());
        Label lblPax = new Label("👥 " + checkout.getPassengerNbr() + " pax");
        info.getChildren().addAll(lblDate, lblPax);

        HBox details = new HBox(15);
        Label lblPrice = new Label("💰 " + checkout.getTotalPrix() + " DT");
        lblPrice.getStyleClass().add("card-price-badge");
        String status = checkout.getStatusReservation();
        Label lblStatus = new Label(status.toUpperCase());
        lblStatus.getStyleClass().addAll("status-badge", "status-" + status.toLowerCase());
        details.getChildren().addAll(lblPrice, lblStatus);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        if ("Pending".equalsIgnoreCase(status)) {
            Button btnApprove = new Button("✅ Approve");
            btnApprove.getStyleClass().add("btn-secondary");
            btnApprove.setOnAction(e -> onApprove.accept(checkout));

            Button btnReject = new Button("❌ Reject");
            btnReject.getStyleClass().add("btn-danger");
            btnReject.setOnAction(e -> onReject.accept(checkout));
            
            actions.getChildren().addAll(btnApprove, btnReject);
        } else if ("Confirmed".equalsIgnoreCase(status)) {
            Button btnCancel = new Button("🛑 Cancel");
            btnCancel.getStyleClass().add("btn-cancel");
            btnCancel.setOnAction(e -> onCancel.accept(checkout));
            actions.getChildren().add(btnCancel);
        }

        getChildren().addAll(header, info, details, new Separator(), actions);
    }
}
