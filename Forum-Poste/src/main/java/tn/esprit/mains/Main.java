package tn.esprit.mains;

import java.sql.SQLException;
import tn.esprit.entities.Poste;
import tn.esprit.services.ServicePoste;

public class Main {
    public static void main(String[] args) {

        ServicePoste sp = new ServicePoste();
        try {
            sp.ajouter(new Poste(15, null, null, "url", "type", "status"));
            System.out.println("Poste ajouté avec succès!");
            sp.modifier(new Poste(1,33, null, null, "C:\\Users\\pc\\Desktop\\pictures", "type", "status"));
            System.out.println("Poste modifié avec succès!");
            sp.supprimer(1);
            System.out.println("Poste supprimé avec succès!");
            System.out.println(sp.afficher());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}