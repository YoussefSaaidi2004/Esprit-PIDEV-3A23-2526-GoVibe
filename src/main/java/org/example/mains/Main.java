package org.example.mains;

import org.example.entities.Hotel;
import org.example.entities.Chambre;
import org.example.services.ServiceHotel;
import org.example.services.ServiceChambre;
import org.example.utils.MyDataBase;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        MyDataBase myDataBase = MyDataBase.getInstance();
        ServiceHotel sh = new ServiceHotel();
        ServiceChambre sc = new ServiceChambre();

        try {
            // ================== HOTELS ==================
            sh.insert(new Hotel("ElMouradi", "Kantaoui", "Sousse", 5, "Expérience agréable", "uploads/mouradi.jpg", 100000));
            System.out.println("Hotel is inserted");

            sh.update(new Hotel(2, "ElMouradi", "Kantaoui", "Sousse", 5, "Le plus reconnu.", "uploads/mouradi.jpg", 120000));
            System.out.println("Hotel is modified");

            sh.delete(3);
            System.out.println("Hotel is deleted");

            System.out.println("Hotels list:");
            sh.show().forEach(System.out::println);

            // ================== CHAMBRES ==================
            sc.insert(new Chambre("Simple", 1, "WiFi, TV", 1, 120, 150, 100));
            sc.insert(new Chambre("Double", 2, "WiFi, TV, Clim", 1, 200, 250, 180));
            sc.insert(new Chambre("Suite", 4, "WiFi, TV, Jacuzzi, Balcon", 1, 400, 500, 350));
            System.out.println("\nChambres inserted");

            // Update chambre 1
            Chambre c1 = sc.show().get(0);
            c1.setPrixStandard(130);
            c1.setPrixHauteSaison(160);
            sc.update(c1);
            System.out.println("Chambre 1 updated");

            // Delete chambre 2
            Chambre c2 = sc.show().get(1);
            sc.delete(c2.getId());
            System.out.println("Chambre 2 deleted");

            System.out.println("Chambres list:");
            sc.show().forEach(System.out::println);

        } catch (SQLException e) {
            System.out.println("Error while operating on database: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}
