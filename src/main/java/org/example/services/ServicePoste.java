package org.example.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.example.entities.Poste;
import org.example.config.UnifiedDatabaseManager;

public class ServicePoste implements iPoste<Poste> {

    @Override
    public void ajouter(Poste p) throws SQLException {
        String sql = "INSERT INTO poste (user_id, likes, date_creation, date_modification, url, type, contenu, forum_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection cnx = UnifiedDatabaseManager.getConnection();
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
        Connection cnx = UnifiedDatabaseManager.getConnection();
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
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Poste> afficher() throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste";
        Connection cnx = UnifiedDatabaseManager.getConnection();
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
        Connection cnx = UnifiedDatabaseManager.getConnection();
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
        Connection cnx = UnifiedDatabaseManager.getConnection();
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

    /**
     * Returns the top N posts sorted by likes descending.
     */
    public List<Poste> getTopPostsByLikes(int limit) throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste ORDER BY likes DESC LIMIT ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, limit);
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
            int forumId = rs.getInt("forum_id");
            p.setForum_id(rs.wasNull() ? null : forumId);
            postes.add(p);
        }
        return postes;
    }

    /**
     * Increments the like count of a post by 1.
     */
    public void likePost(int postId) throws SQLException {
        String sql = "UPDATE poste SET likes = likes + 1 WHERE post_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.executeUpdate();
    }

    /**
     * Decrements the like count of a post by 1 (minimum 0).
     */
    public void unlikePost(int postId) throws SQLException {
        String sql = "UPDATE poste SET likes = GREATEST(likes - 1, 0) WHERE post_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ps.executeUpdate();
    }

    /**
     * Full-text search across all posts by content keyword.
     */
    public List<Poste> searchByContenu(String keyword) throws SQLException {
        List<Poste> postes = new ArrayList<>();
        String sql = "SELECT * FROM poste WHERE contenu LIKE ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
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
            int forumId = rs.getInt("forum_id");
            p.setForum_id(rs.wasNull() ? null : forumId);
            postes.add(p);
        }
        return postes;
    }
}
