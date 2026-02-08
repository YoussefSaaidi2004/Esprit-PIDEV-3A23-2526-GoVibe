package org.example.mains;

import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.utils.MyDataBase;

import java.sql.SQLException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws SQLException {
        ServicePersonne sp=new ServicePersonne();
        try {
            sp.ajouter(new personne("aziz", "mchala", 23));
            System.out.println("personne ajouter");
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
}