package org.example.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.utils.SceneNavigator;
import org.example.utils.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;

public class UserProfileController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label profileInitials;

    @FXML
    private MenuButton profileMenu;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label createdAtLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private Label successLabel;

    @FXML
    private StackPane avatarContainer;

    private personne currentUser;
    private final ServicePersonne servicePersonne = new ServicePersonne();

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            loadUserData();
            updateProfileInitials();
        } else {
            showError("Veuillez vous connecter pour accéder à votre profil");
        }
    }

    private void loadUserData() {
        nomField.setText(currentUser.getNom());
        prenomField.setText(currentUser.getPrenom());
        emailField.setText(currentUser.getEmail());

        if (currentUser.getCreated_at() != null) {
            createdAtLabel.setText("Membre depuis: " + currentUser.getCreated_at().toLocalDateTime().toLocalDate().toString());
        } else {
            createdAtLabel.setText("Membre depuis: Inconnu");
        }

        String role = currentUser.getRole();
        if (role != null) {
            roleLabel.setText("Rôle: " + role.substring(0, 1).toUpperCase() + role.substring(1));
        } else {
            roleLabel.setText("Rôle: Utilisateur");
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText("Mon Profil - " + currentUser.getPrenom() + " " + currentUser.getNom());
        }
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
    private void handleSaveProfile() {
        clearMessages();

        if (currentUser == null) {
            showError("Veuillez vous connecter");
            return;
        }

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showError("Veuillez entrer une adresse email valide");
            return;
        }

        // Check if email is already used by another user
        if (!email.equals(currentUser.getEmail())) {
            try {
                if (servicePersonne.emailExists(email)) {
                    showError("Cette adresse email est déjà utilisée");
                    return;
                }
            } catch (SQLException e) {
                showError("Erreur lors de la vérification de l'email");
                return;
            }
        }

        // Update user data
        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);

        // Handle password change if requested
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!newPass.isEmpty()) {
            if (currentPass.isEmpty()) {
                showError("Veuillez entrer votre mot de passe actuel");
                return;
            }

            // Verify current password
            if (!BCrypt.checkpw(currentPass, currentUser.getPassword())) {
                showError("Mot de passe actuel incorrect");
                return;
            }

            if (newPass.length() < 6) {
                showError("Le nouveau mot de passe doit contenir au moins 6 caractères");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                showError("Les nouveaux mots de passe ne correspondent pas");
                return;
            }

            currentUser.setPassword(newPass);
        }

        try {
            servicePersonne.modifier(currentUser);

            // Update session
            SessionManager.setCurrentUser(currentUser);

            // Clear password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            showSuccess("Profil mis à jour avec succès !");
            updateProfileInitials();

        } catch (Exception e) {
            showError("Erreur lors de la mise à jour du profil: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        clearMessages();
        loadUserData();
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    @FXML
    private void handleHome() {
        org.example.mains.MainApp.switchScene("/org/example/UserHomeView.fxml", "Accueil");
    }

    @FXML
    private void handleActivities() {
        SceneNavigator.switchTo("/UserHome.fxml", welcomeLabel);
    }

    @FXML
    private void handleLocations() {
        org.example.mains.MainApp.switchScene("/LocationListView.fxml", "Voitures");
    }

    @FXML
    private void handleFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "User Dashboard");
    }

    @FXML
    private void handleChambres() {
        org.example.mains.MainApp.switchScene("/views/room-booking.fxml", "Chambres Disponibles");
    }

    @FXML
    private void handleForums() {
        SceneNavigator.switchTo("/poste-forumviews/ListForum.fxml", welcomeLabel);
    }

    @FXML
    private void handleReclamation() {
        SceneNavigator.switchTo("/ReclamationView.fxml", welcomeLabel, controller -> {
            if (controller instanceof ReclamationController) {
                ((ReclamationController) controller).initData(currentUser);
            }
        });
    }

    @FXML
    private void handleProfile() {
        // Already on profile page
    }

    @FXML
    private void handleMyReservations() {
        SceneNavigator.switchTo("/views/room-booking.fxml", welcomeLabel);
    }

    @FXML
    private void handleMyLocations() {
        SceneNavigator.switchTo("/LocationListView.fxml", welcomeLabel);
    }

    @FXML
    private void handleMyFlights() {
        org.example.mains.MainApp.switchScene("/views/user-dashboard.fxml", "Mes Vols");
    }

    @FXML
    private void handleSettings() {
        showInfoAlert("Paramètres", "Fonctionnalité à venir : Paramètres utilisateur");
    }

    @FXML
    private void handleHelp() {
        showInfoAlert("Aide", "Besoin d'aide ? Contactez-nous à support@govibe.tn");
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage;
            if (welcomeLabel != null && welcomeLabel.getScene() != null) {
                stage = (Stage) welcomeLabel.getScene().getWindow();
            } else if (profileMenu != null && profileMenu.getScene() != null) {
                stage = (Stage) profileMenu.getScene().getWindow();
            } else {
                stage = new Stage();
            }
            stage.setTitle("GoVibe - Connexion");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearMessages() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
        if (successLabel != null) {
            successLabel.setText("");
            successLabel.setVisible(false);
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }

    private void showSuccess(String message) {
        if (successLabel != null) {
            successLabel.setText(message);
            successLabel.setVisible(true);
        }
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
