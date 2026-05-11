package org.example.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import org.example.entities.personne;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;

public class UserHeaderController {

    @FXML
    private MenuButton profileMenu;

    @FXML
    private Label profileInitials;

    private personne currentUser;
    
    // We need a reference node to get the scene/stage for navigation if SceneNavigator requires existing node
    // Since this is a component, any node will do. We can use profileMenu.
    private javafx.scene.Node getContextNode() {
        return profileMenu;
    }

    @FXML
    public void initialize() {
        // Load user from SessionManager immediately if possible
        this.currentUser = SessionManager.getCurrentUser();
        updateProfileInitials();
    }

    private void updateProfileInitials() {
        if (currentUser != null && profileInitials != null) {
            String initials = "";
            if (currentUser.getPrenom() != null && !currentUser.getPrenom().isEmpty()) {
                initials += currentUser.getPrenom().charAt(0);
            }
            if (currentUser.getNom() != null && !currentUser.getNom().isEmpty()) {
                initials += currentUser.getNom().charAt(0);
            }
            if (initials.isEmpty()) {
                initials = "U";
            }
            profileInitials.setText(initials.toUpperCase());
        }
    }

    @FXML
    private void handleHome() {
        System.out.println("[Nav] Home clicked");
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleActivities() {
        System.out.println("[Nav] Activities clicked");
        org.example.mains.MainApp.switchScene("/org/example/MapActivitiesView.fxml", "Découvrir le Monde");
    }

    @FXML
    private void handleLocations() {
        System.out.println("[Nav] Locations clicked");
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
    }

    @FXML
    private void handleFlights() {
        System.out.println("[Nav] Flights clicked");
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }

    @FXML
    private void handleChambres() {
        System.out.println("[Nav] Chambres clicked");
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Chambres Disponibles");
    }

    @FXML
    private void handleMyReservations() {
        System.out.println("[Nav] My Reservations clicked");
        org.example.mains.MainApp.switchScene("/views/user-checkouts.fxml", "Mes Réservations");
    }

    @FXML
    private void handleForums() {
        System.out.println("[Nav] Forums clicked");
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", getContextNode());
    }

    @FXML
    private void handleReclamation() {
        System.out.println("[Nav] Reclamation clicked");
        SceneNavigator.switchTo("/ReclamationView.fxml", getContextNode(), controller -> {
            if (controller instanceof ReclamationController) {
                ((ReclamationController) controller).initData(currentUser);
            }
        });
    }

    @FXML
    private void openProposeActivity() {
        System.out.println("[Nav] Propose Activity clicked");
        SceneNavigator.switchTo("/ProposeActivity.fxml", getContextNode());
    }

    @FXML
    private void handleProfile() {
        System.out.println("[Nav] Profile clicked - navigating to UserProfileView.fxml");
        org.example.mains.MainApp.switchScene("/org/example/UserProfileView.fxml", "Mon Profil");
    }

    @FXML
    private void handleMyLocations() {
        System.out.println("[Nav] My Locations clicked");
        SceneNavigator.switchTo("/LocationListView.fxml", getContextNode());
    }

    @FXML
    private void handleMyFlights() {
        System.out.println("[Nav] My Flights clicked");
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleMyHotelReservations() {
        System.out.println("[Nav] My Hotel Reservations clicked");
        org.example.mains.MainApp.switchScene("/views/my-reservations.fxml", "Mes Réservations Hôtels");
    }

    @FXML
    private void handleSettings() {
        System.out.println("[Nav] Settings clicked");
    }

    @FXML
    private void handleHelp() {
        System.out.println("[Nav] Help clicked");
    }

    @FXML
    private void handleLogout() {
        System.out.println("[Nav] Logout clicked");
        // Clear the session
        SessionManager.clear();
        
        // Navigate to Login View
        // Note: Using MainApp.switchScene with the absolute path to LoginView.fxml
        // We assume LoginView.fxml is in /org/example/LoginView.fxml based on MainApp.java
        org.example.mains.MainApp.switchScene("/org/example/LoginView.fxml", "GoVibe - Connexion");
    }
}
