package org.example;

import org.example.entites.Activite;
import org.example.entites.Session;
import org.example.services.ServiceActivite;
import org.example.services.ServiceSession;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ServiceActivite serviceActivite = new ServiceActivite();
        ServiceSession serviceSession = new ServiceSession();

        while (true) {
            System.out.println("\n========================");
            System.out.println("        MENU CRUD");
            System.out.println("========================");
            System.out.println("1. CRUD Activité");
            System.out.println("2. CRUD Session (choisir d'abord l'ID activité)");
            System.out.println("0. Quitter");
            System.out.print("➡ Votre choix : ");

            int choix = sc.nextInt();

            switch (choix) {
                case 1:
                    crudActivite(sc, serviceActivite);
                    break;

                case 2:
                    // Afficher les activités et demander l'ID activité AVANT d'entrer dans le CRUD session
                    try {
                        System.out.println("\nListe des activités :");
                        serviceActivite.afficher();
                    } catch (SQLException e) {
                        System.out.println("Erreur lors de l'affichage des activités : " + e.getMessage());
                        break; // on ne continue pas si on n'a pas pu afficher
                    }

                    System.out.print("\n➡ Entrez l’ID de l’activité pour travailler sur ses sessions : ");
                    int idActivite = sc.nextInt();

                    crudSession(sc, serviceSession, idActivite);
                    break;

                case 0:
                    System.out.println("👋 Au revoir !");
                    return;

                default:
                    System.out.println("⚠️ Choix invalide !");
            }
        }
    }

    // ===========================
    // CRUD ACTIVITE
    // ===========================
    private static void crudActivite(Scanner sc, ServiceActivite sa) {
        System.out.println("\n=== CRUD ACTIVITE ===");
        System.out.println("1. Ajouter");
        System.out.println("2. Afficher");
        System.out.println("3. Modifier");
        System.out.println("4. Supprimer");
        System.out.print("Choix : ");

        int ch = sc.nextInt();
        sc.nextLine(); // consomme le \n

        try {
            switch (ch) {
                case 1: // Ajouter
                    System.out.print("Nom : ");
                    String nom = sc.nextLine();

                    System.out.print("Description : ");
                    String desc = sc.nextLine();

                    System.out.print("Type : ");
                    String type = sc.nextLine();

                    System.out.print("Localisation : ");
                    String loc = sc.nextLine();

                    System.out.print("Prix : ");
                    BigDecimal prix = sc.nextBigDecimal();

                    sa.ajouter(new Activite(nom, desc, type, loc, prix));
                    System.out.println("✔ Activité ajoutée !");
                    break;

                case 2: // Afficher
                    sa.afficher();
                    break;

                case 3: // Modifier
                    System.out.print("ID activité à modifier : ");
                    int idm = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Nom : ");
                    String nomM = sc.nextLine();

                    System.out.print("Description : ");
                    String descM = sc.nextLine();

                    System.out.print("Type : ");
                    String typeM = sc.nextLine();

                    System.out.print("Localisation : ");
                    String locM = sc.nextLine();

                    System.out.print("Prix : ");
                    BigDecimal prixM = sc.nextBigDecimal();

                    sa.modifierParId(idm, nomM, descM, typeM, locM, prixM);
                    System.out.println("✔ Activité modifiée !");
                    break;

                case 4: // Supprimer
                    System.out.print("ID activité à supprimer : ");
                    int ids = sc.nextInt();
                    sa.supprimerParId(ids);
                    System.out.println("✔ Activité supprimée (si l'ID existait) !");
                    break;

                default:
                    System.out.println("⚠️ Choix invalide !");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur SQL (Activité) : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Erreur (Activité) : " + e.getMessage());
        }
    }

    // ===========================
    // CRUD SESSION (avec ID activité imposé)
    // ===========================
    private static void crudSession(Scanner sc, ServiceSession ss, int activiteID) {
        System.out.println("\n=== CRUD SESSION POUR ACTIVITE ID = " + activiteID + " ===");
        System.out.println("1. Ajouter une session");
        System.out.println("2. Afficher les sessions");
        System.out.println("3. Modifier une session");
        System.out.println("4. Supprimer une session");
        System.out.print("Choix : ");

        int ch = sc.nextInt();

        try {
            switch (ch) {
                case 1: // Ajouter
                    System.out.print("Date (YYYY-MM-DD) : ");
                    Date d = Date.valueOf(sc.next());

                    System.out.print("Heure (HH:MM:SS) : ");
                    Time h = Time.valueOf(sc.next());

                    System.out.print("Capacité : ");
                    int c = sc.nextInt();

                    System.out.print("Places restantes : ");
                    int p = sc.nextInt();

                    Session newSession = new Session(d, h, c, p, activiteID);
                    ss.ajouter(newSession);
                    System.out.println("✔ Session ajoutée !");
                    break;

                case 2: // Afficher
                    ss.afficher();
                    break;

                case 3: // Modifier
                    System.out.print("ID session à modifier : ");
                    int idm = sc.nextInt();

                    System.out.print("Nouvelle date (YYYY-MM-DD) : ");
                    Date dM = Date.valueOf(sc.next());

                    System.out.print("Nouvelle heure (HH:MM:SS) : ");
                    Time hM = Time.valueOf(sc.next());

                    System.out.print("Nouvelle capacité : ");
                    int capM = sc.nextInt();

                    System.out.print("Nouvelles places restantes : ");
                    int plM = sc.nextInt();

                    Session sM = new Session(idm, dM, hM, capM, plM, activiteID);
                    ss.modifier(sM);
                    System.out.println("✔ Session modifiée !");
                    break;

                case 4: // Supprimer
                    System.out.print("ID session à supprimer : ");
                    int ids = sc.nextInt();
                    ss.supprimer(ids);
                    System.out.println("✔ Session supprimée (si l'ID existait) !");
                    break;

                default:
                    System.out.println("⚠️ Choix invalide !");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur SQL (Session) : " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Erreur (Session) : " + e.getMessage());
        }
    }
}