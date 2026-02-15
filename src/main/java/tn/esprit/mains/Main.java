package tn.esprit.mains;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import tn.esprit.entities.Forum;
import tn.esprit.entities.Poste;
import tn.esprit.services.ServiceForum;
import tn.esprit.services.ServicePoste;

public class Main {
    public static void main(String[] args) {

        ServicePoste sp = new ServicePoste();
        try {
            // Test post orphelin
            sp.ajouter(new Poste(1, "Poste Orphelin Test", "STATUS", null));
            System.out.println("Poste orphelin ajouté avec succès!");

            // Test post dans un forum
            sp.ajouter(new Poste(1, 1, "Poste dans Forum Test", "MEDIA", "path/to/img"));
            System.out.println("Poste forum ajouté avec succès!");
            sp.supprimer(1);
            System.out.println("Poste supprimé avec succès!");
            System.out.println(sp.afficher());

            System.out.println("---------- Forum CRUD ----------");
            ServiceForum sf = new ServiceForum();
            Timestamp now = new Timestamp(System.currentTimeMillis());

            // Ajouter
            sf.ajouter(new Forum("Java Forum", "java.png", 1, 0, 10, "A forum for Java developers", now, false));
            System.out.println("Forum ajouté avec succès!");

            // Modifier
            List<Forum> forums = sf.afficher();
            if (!forums.isEmpty()) {
                Forum lastForum = forums.get(0);
                lastForum.setName("Updated Java Forum");
                sf.modifier(lastForum);
                System.out.println("Forum modifié avec succès!");

                // Supprimer
                // sf.supprimer(lastForum.getForum_id());
                // System.out.println("Forum supprimé avec succès!");
            }

            // Afficher
            System.out.println("Liste des forums :");
            System.out.println(sf.afficher());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}