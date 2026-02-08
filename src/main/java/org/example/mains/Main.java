package org.example.mains;

import org.example.entities.personne;
import org.example.services.ServicePersonne;
import org.example.utils.MyDataBase;

import java.sql.SQLException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        ServicePersonne sp=new ServicePersonne();
        try {
            /*sp.ajouter(new personne("yassine", "marzouki", 10));  //ajouter personne
            System.out.println("personne ajouter");*/
            
            //modifier personne
            sp.modifier(new personne(1,"flen", "fouleni", 25));
            System.out.println("personne Modifier");
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }
}