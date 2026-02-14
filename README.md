# 🏨 GoVibe Travel - Système de Gestion Hôtelière

## 📋 Description
GoVibe Travel est une application JavaFX moderne pour la gestion complète d'hôtels, chambres et réservations clients. L'application offre une interface utilisateur professionnelle avec un design moderne inspiré du thème voyage.

## ✨ Fonctionnalités

### 🏨 Gestion des Hôtels
- ➕ Ajouter de nouveaux hôtels avec toutes les informations (nom, adresse, ville, étoiles, budget, description, photo)
- ✏️ Modifier les informations des hôtels existants
- 🗑️ Supprimer des hôtels (suppression en cascade des chambres et réservations)
- 🔍 Rechercher des hôtels par nom ou ville
- 📊 Affichage en cartes modernes avec toutes les informations

### 🛏️ Gestion des Chambres
- ➕ Ajouter des chambres avec type, capacité, équipements
- 💰 Gestion des prix par saison (standard, haute saison, basse saison)
- 🏨 Association des chambres aux hôtels
- ✏️ Modifier les informations des chambres
- 🗑️ Supprimer des chambres
- 🔍 Rechercher des chambres
- 🎯 Filtrer les chambres par hôtel

### 📅 Gestion des Réservations Client
- ➕ Créer de nouvelles réservations
- 👤 Informations client (nom, email, téléphone)
- 🏨 Sélection d'hôtel et de chambre
- 📆 Gestion des dates de séjour
- 💰 Calcul automatique du prix total
- 🎯 Statuts de réservation (EN_ATTENTE, CONFIRMEE, ANNULEE)
- ✏️ Modifier les réservations
- ❌ Annuler des réservations
- 🗑️ Supprimer des réservations
- 🔍 Rechercher par nom de client ou email
- 🎯 Filtrer par statut

## 🎨 Design & Interface

### Palette de Couleurs
- **#D1F2EB** - Mint Cream (Arrière-plan doux)
- **#50C878** - Emerald Green (Couleur principale)
- **#0B6E4F** - Dark Green (Accents)
- **#013220** - Very Dark Green (Texte sombre)
- **#F5F3E7** - Beige (Fond de contenu)
- **#A0E0C9** - Light Green (Accents secondaires)
- **#D84E36** - Red Orange (Actions dangereuses)

### Caractéristiques UI/UX
- ✨ Interface moderne avec cartes (cards)
- 🎨 Dégradés de couleurs harmonieux
- 🖼️ Ombres portées pour la profondeur
- 📱 Layout responsive
- 🎯 Navigation intuitive avec sidebar
- 🔄 Transitions fluides
- 🎭 Thème voyage professionnel

## 🗄️ Base de Données

### Tables
1. **hotel** - Informations sur les hôtels
2. **chambre** - Informations sur les chambres avec tarification saisonnière
3. **reservation** - Réservations clients avec statuts

### Relations
- Une chambre appartient à un hôtel (relation 1:N)
- Une réservation concerne une chambre et un hôtel (relations 1:N)
- Suppression en cascade configurée

## 🚀 Installation

### Prérequis
- Java 17 ou supérieur
- Maven 3.6+
- MySQL 8.0+
- JavaFX 21

### Étapes d'Installation

1. **Cloner le projet**
```bash
cd salma-pidev
```

2. **Créer la base de données**
```sql
CREATE DATABASE govibe_travel;
USE govibe_travel;
```

3. **Exécuter le script SQL**
```bash
mysql -u root -p govibe_travel < database-schema.sql
```

4. **Configurer la connexion**
Modifier `src/main/java/org/example/utils/MyDataBase.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/govibe_travel";
private static final String USER = "root";
private static final String PASSWORD = "votre_mot_de_passe";
```

5. **Compiler et exécuter**
```bash
mvn clean install
mvn javafx:run
```

## 📁 Structure du Projet

```
src/
├── main/
│   ├── java/
│   │   └── org/example/
│   │       ├── controllers/
│   │       │   ├── MainLayoutController.java
│   │       │   ├── HotelViewController.java
│   │       │   ├── ChambreViewController.java
│   │       │   └── ReservationViewController.java
│   │       ├── entities/
│   │       │   ├── Hotel.java
│   │       │   ├── Chambre.java
│   │       │   └── Reservation.java
│   │       ├── services/
│   │       │   ├── IService.java
│   │       │   ├── ServiceHotel.java
│   │       │   ├── ServiceChambre.java
│   │       │   └── ServiceReservation.java
│   │       ├── utils/
│   │       │   └── MyDataBase.java
│   │       └── mains/
│   │           └── MainApp.java
│   └── resources/
│       ├── main-layout.fxml
│       ├── hotel-view.fxml
│       ├── chambre-view.fxml
│       ├── reservation-view.fxml
│       └── styles.css
└── database-schema.sql
```

## 🔧 Technologies Utilisées

- **JavaFX 21** - Framework UI
- **MySQL 8** - Base de données
- **JDBC** - Connectivité base de données
- **Maven** - Gestion de dépendances
- **Java 17** - Langage de programmation

## 📊 Schéma de Base de Données

```sql
hotel (id, nom, adresse, ville, nombre_etoiles, budget, description, photo_url)
  ↓
chambre (id, type, capacite, equipements, hotel_id, prix_standard, prix_haute_saison, prix_basse_saison)
  ↓
reservation (id, client_nom, client_email, client_telephone, chambre_id, hotel_id, date_debut, date_fin, prix_total, statut)
```

## 🎯 Fonctionnalités Futures Suggérées

- 🔐 Système d'authentification avec rôles (Admin, Réceptionniste, Client)
- 📧 Envoi d'emails de confirmation de réservation
- 📊 Tableau de bord avec statistiques
- 💳 Intégration de paiement en ligne
- 📱 Application mobile
- 🌍 Support multilingue
- 📸 Upload d'images pour les hôtels
- ⭐ Système d'avis clients
- 📅 Calendrier de disponibilité interactif
- 🧾 Génération de factures PDF

## 👥 Utilisation

### Navigation
- Utilisez la **sidebar** pour naviguer entre les modules
- **Hôtels** - Gérer les établissements
- **Chambres** - Gérer les chambres
- **Réservations Client** - Gérer les réservations

### Actions CRUD
- **Ajouter** - Cliquez sur le bouton "➕ Ajouter" en haut à droite
- **Modifier** - Cliquez sur "✏️ Modifier" sur une carte
- **Supprimer** - Cliquez sur "🗑️" sur une carte
- **Rechercher** - Utilisez le champ de recherche en haut
- **Filtrer** - Utilisez les ComboBox de filtres

## 📝 Notes Importantes

1. **Données de Test** - Le script SQL inclut des données de démonstration
2. **Suppression en Cascade** - Supprimer un hôtel supprime ses chambres et réservations
3. **Validation** - Les formulaires incluent une validation de base
4. **Format de Date** - Format ISO (YYYY-MM-DD)
5. **Prix** - En dinars tunisiens (DT)

## 🐛 Dépannage

### Erreur de Connexion MySQL
- Vérifiez que MySQL est démarré
- Vérifiez les credentials dans `MyDataBase.java`
- Vérifiez que la base de données existe

### JavaFX Non Trouvé
- Vérifiez que JavaFX est bien configuré dans `pom.xml`
- Utilisez `mvn javafx:run` au lieu de `java -jar`

### Problèmes d'Affichage
- Vérifiez que tous les fichiers FXML sont dans `resources/`
- Vérifiez que les contrôleurs sont bien référencés dans les FXML

## 📞 Support

Pour toute question ou problème, veuillez créer une issue sur le repository.

## 📄 Licence

Ce projet est développé dans un cadre éducatif.

---

**GoVibe Travel** - Votre partenaire pour la gestion hôtelière moderne 🏨✨

