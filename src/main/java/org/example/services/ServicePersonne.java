package org.example.services;

import org.example.entities.personne;
import org.example.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class ServicePersonne implements IService<personne>{
    private Connection connection;
    public ServicePersonne(){
        connection = MyDataBase.getInstance().getMyConnection();
    }
    @Override
    public void ajouter(personne personne) throws SQLException {
    String sql="INSERT INTO `personne`(`nom`, `prenom`, `age`) VALUES ('"+personne.getNom()+"','"+personne.getPrenom()+"',"+personne.getAge()+")";
    Statement statement=connection.createStatement();
    statement.executeUpdate(sql);
    }

    @Override
    public void modifier(personne personne) throws SQLException {
        String sql="UPDATE `personne` SET `nom`=?,`prenom`=?,`age`=? WHERE `id`=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1,personne.getNom());
        ps.setString(2,personne.getPrenom());
        ps.setInt(3,personne.getAge());
        ps.setInt(4,personne.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {

    }

    @Override
    public List<personne> afficher() throws SQLException {
        return List.of();
    }
}
