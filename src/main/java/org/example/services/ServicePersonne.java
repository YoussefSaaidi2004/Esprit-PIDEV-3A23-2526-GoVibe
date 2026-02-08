package org.example.services;

import org.example.entities.personne;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
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
        String sql="DELETE FROM `personne` WHERE `id`=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1,id);
        ps.executeUpdate();
    }

    @Override
    public List<personne> afficher() throws SQLException {
        List<personne> personnes = new ArrayList<>();
        String sql = "SELECT * FROM personne";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        while (rs.next()) {
            personne p = new personne();
            p.setId(rs.getInt("id"));
            p.setNom(rs.getString("nom"));
            p.setPrenom(rs.getString("prenom"));
            p.setAge(rs.getInt("age"));
            personnes.add(p);
        }
        return personnes;
    }
}
