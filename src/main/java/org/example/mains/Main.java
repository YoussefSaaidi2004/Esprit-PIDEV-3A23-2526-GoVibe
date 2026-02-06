package org.example.mains;

import org.example.entites.Activite;
import org.example.services.ServiceActivite;

import java.math.BigDecimal;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ServiceActivite service = new ServiceActivite();
        Scanner sc = new Scanner(System.in);

        try {
            // ✅ (Optionnel) AJOUT
            System.out.println("=== AJOUT (optionnel) ===");
            System.out.print("Nom: ");
            String name = sc.nextLine();

            System.out.print("Description: ");
            String desc = sc.nextLine();

            System.out.print("Type: ");
            String type = sc.nextLine();

            System.out.print("Localisation: ");
            String loc = sc.nextLine();

            System.out.print("Prix: ");
            BigDecimal prix = new BigDecimal(sc.nextLine());

            Activite a = new Activite(name, desc, type, loc, prix);
            service.ajouter(a);
            System.out.println("✅ Ajout OK, ID = " + a.getId());

            // ✅ AFFICHER
            System.out.println("\n📌 Liste des activités :");
            service.afficher();

            // ✅ MODIFIER : choisir l'id
            System.out.println("\n=== MODIFICATION ===");
            System.out.print("Donne l'ID à modifier: ");
            int idModif = Integer.parseInt(sc.nextLine());

            System.out.print("Nouveau Nom: ");
            String newName = sc.nextLine();

            System.out.print("Nouvelle Description: ");
            String newDesc = sc.nextLine();

            System.out.print("Nouveau Type: ");
            String newType = sc.nextLine();

            System.out.print("Nouvelle Localisation: ");
            String newLoc = sc.nextLine();

            System.out.print("Nouveau Prix: ");
            BigDecimal newPrix = new BigDecimal(sc.nextLine());

            service.modifierParId(idModif, newName, newDesc, newType, newLoc, newPrix);

            System.out.println("\n📌 Liste après modification :");
            service.afficher();

            // 🗑️ SUPPRIMER : choisir l'id
            System.out.println("\n=== SUPPRESSION ===");
            System.out.print("Donne l'ID à supprimer: ");
            int idSupp = Integer.parseInt(sc.nextLine());

            service.supprimerParId(idSupp);

            System.out.println("\n📌 Liste après suppression :");
            service.afficher();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }
}