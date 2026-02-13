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
        String sql = "INSERT INTO poste (user_id, likes, date_creation, date_modification, url, type, contenu, forum_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getUser_id());
        ps.setInt(2, p.getLikes());
        ps.setTimestamp(3, p.getDate_creation());
        ps.setTimestamp(4, p.getDate_modification());
        ps.setString(5, p.getUrl());
        ps.setString(6, p.getType());
        ps.setString(7, p.getContenu());
        if (p.getForum_id() != null) {
            ps.setInt(8, p.getForum_id());
        } else {
            ps.setNull(8, Types.INTEGER);
        }
        ps.executeUpdate();
    }

    @Override
    public void modifier(Poste p) throws SQLException {
        String sql = "UPDATE poste SET user_id=?, likes=?, date_modification=?, url=?, type=?, contenu=?, forum_id=? WHERE post_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, p.getUser_id());
        ps.setInt(2, p.getLikes());
        ps.setTimestamp(3, p.getDate_modification());
        ps.setString(4, p.getUrl());
        ps.setString(5, p.getType());
        ps.setString(6, p.getContenu());
        if (p.getForum_id() != null) {
            ps.setInt(7, p.getForum_id());
        } else {
            ps.setNull(7, Types.INTEGER);
        }
        ps.setInt(8, p.getPost_id());
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
            p.setUser_id(rs.getInt("user_id"));
            p.setLikes(rs.getInt("likes"));
            p.setDate_creation(rs.getTimestamp("date_creation"));
            p.setDate_modification(rs.getTimestamp("date_modification"));
            p.setUrl(rs.getString("url"));
            p.setType(rs.getString("type"));
            p.setContenu(rs.getString("contenu"));
            int forumId = rs.getInt("forum_id");
            if (rs.wasNull()) {
                p.setForum_id(null);
            } else {
                p.setForum_id(forumId);
            }
            postes.add(p);
        }
        return postes;
    }

    public List<Poste> afficherOrphelins() throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste WHERE forum_id IS NULL";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Poste p = new Poste();
            p.setPost_id(rs.getInt("post_id"));
            p.setUser_id(rs.getInt("user_id"));
            p.setLikes(rs.getInt("likes"));
            p.setDate_creation(rs.getTimestamp("date_creation"));
            p.setDate_modification(rs.getTimestamp("date_modification"));
            p.setUrl(rs.getString("url"));
            p.setType(rs.getString("type"));
            p.setContenu(rs.getString("contenu"));
            p.setForum_id(null);
            postes.add(p);
        }
        return postes;
    }

    public List<Poste> afficherParForum(int forumId) throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste WHERE forum_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Poste p = new Poste();
            p.setPost_id(rs.getInt("post_id"));
            p.setUser_id(rs.getInt("user_id"));
            p.setLikes(rs.getInt("likes"));
            p.setDate_creation(rs.getTimestamp("date_creation"));
            p.setDate_modification(rs.getTimestamp("date_modification"));
            p.setUrl(rs.getString("url"));
            p.setType(rs.getString("type"));
            p.setContenu(rs.getString("contenu"));
            p.setForum_id(forumId);
            postes.add(p);
        }
        return postes;
    }
}
