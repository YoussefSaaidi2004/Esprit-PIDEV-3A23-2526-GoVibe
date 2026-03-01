package org.example.services;

import org.example.config.UnifiedDatabaseManager;
import org.example.entities.Commentaire;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire {

    public ServiceCommentaire() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS commentaire (" +
                "commentaire_id INT AUTO_INCREMENT PRIMARY KEY," +
                "post_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "contenu TEXT NOT NULL," +
                "parent_id INT DEFAULT 0," +
                "likes INT DEFAULT 0," +
                "dislikes INT DEFAULT 0," +
                "date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (post_id) REFERENCES poste(post_id) ON DELETE CASCADE" +
                ")";
        try {
            Connection cnx = UnifiedDatabaseManager.getConnection();
            cnx.createStatement().executeUpdate(sql);
            // migrate existing tables
            try { cnx.createStatement().executeUpdate(
                "ALTER TABLE commentaire CHANGE `id` `commentaire_id` INT NOT NULL AUTO_INCREMENT"); } catch (SQLException ignored) {}
            try { cnx.createStatement().executeUpdate(
                "ALTER TABLE commentaire ADD COLUMN date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (SQLException ignored) {}
            try { cnx.createStatement().executeUpdate(
                "ALTER TABLE commentaire ADD COLUMN parent_id INT DEFAULT 0"); } catch (SQLException ignored) {}
            try { cnx.createStatement().executeUpdate(
                "ALTER TABLE commentaire ADD COLUMN likes INT DEFAULT 0"); } catch (SQLException ignored) {}
            try { cnx.createStatement().executeUpdate(
                "ALTER TABLE commentaire ADD COLUMN dislikes INT DEFAULT 0"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void ajouter(Commentaire c) throws SQLException {
        String sql = "INSERT INTO commentaire (post_id, user_id, contenu, parent_id) VALUES (?, ?, ?, ?)";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, c.getPost_id());
        ps.setInt(2, c.getUser_id());
        ps.setString(3, c.getContenu());
        ps.setInt(4, c.getParent_id());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        // Delete replies first, then the comment itself
        String delReplies = "DELETE FROM commentaire WHERE parent_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps1 = cnx.prepareStatement(delReplies);
        ps1.setInt(1, id);
        ps1.executeUpdate();
        PreparedStatement ps2 = cnx.prepareStatement("DELETE FROM commentaire WHERE commentaire_id = ?");
        ps2.setInt(1, id);
        ps2.executeUpdate();
    }

    private Commentaire mapRow(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setCommentaire_id(rs.getInt("commentaire_id"));
        c.setPost_id(rs.getInt("post_id"));
        c.setUser_id(rs.getInt("user_id"));
        c.setContenu(rs.getString("contenu"));
        try { c.setParent_id(rs.getInt("parent_id")); } catch (SQLException ignored) {}
        try { c.setLikes(rs.getInt("likes")); } catch (SQLException ignored) {}
        try { c.setDislikes(rs.getInt("dislikes")); } catch (SQLException ignored) {}
        return c;
    }

    /** Top-level comments for a post (parent_id = 0 or NULL) */
    public List<Commentaire> getByPost(int postId) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE post_id = ? AND (parent_id IS NULL OR parent_id = 0) ORDER BY commentaire_id ASC";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    /** Replies to a comment */
    public List<Commentaire> getReplies(int parentId) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire WHERE parent_id = ? ORDER BY commentaire_id ASC";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, parentId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    public void likeComment(int id) throws SQLException {
        String sql = "UPDATE commentaire SET likes = likes + 1 WHERE commentaire_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void unlikeComment(int id) throws SQLException {
        String sql = "UPDATE commentaire SET likes = GREATEST(likes - 1, 0) WHERE commentaire_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void dislikeComment(int id) throws SQLException {
        String sql = "UPDATE commentaire SET dislikes = dislikes + 1 WHERE commentaire_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void undislikeComment(int id) throws SQLException {
        String sql = "UPDATE commentaire SET dislikes = GREATEST(dislikes - 1, 0) WHERE commentaire_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public int countByPost(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commentaire WHERE post_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, postId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }
}
