package tn.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.entities.Poste;
import tn.esprit.utils.MyDataBase;

public class ServicePoste implements iPoste<Poste> {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    @Override
    public void ajouter(Poste p) throws SQLException {
        String sql = "INSERT INTO poste (likes, date_creation, date_modification, url, type, status) VALUES ("+p.getLikes()+", "+p.getDate_creation()+", "+p.getDate_modification()+", "+p.getUrl()+", "+p.getType()+", "+p.getStatus()+")";
        Statement statement = cnx.createStatement();
        statement.executeUpdate(sql);
    }

    @Override
    public void modifier(Poste p) throws SQLException {
        String sql = "UPDATE poste SET likes=?, date_modification=?, url=?, type=?, status=? WHERE post_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getLikes());
        ps.setTimestamp(2, p.getDate_modification());
        ps.setString(3, p.getUrl());
        ps.setString(4, p.getType());
        ps.setString(5, p.getStatus());
        ps.setInt(6, p.getPost_id());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM poste WHERE post_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Poste> afficher() throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Poste p = new Poste();
            p.setPost_id(rs.getInt("post_id"));
            p.setLikes(rs.getInt("likes"));
            p.setDate_creation(rs.getTimestamp("date_creation"));
            p.setDate_modification(rs.getTimestamp("date_modification"));
            p.setUrl(rs.getString("url"));
            p.setType(rs.getString("type"));
            p.setStatus(rs.getString("status"));
            postes.add(p);
        }
        return postes;
    }
}
