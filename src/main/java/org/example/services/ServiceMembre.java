package org.example.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.example.entities.Membre;
import org.example.config.UnifiedDatabaseManager;

public class ServiceMembre {

    public void ajouter(Membre m) throws SQLException {
        if (estMembreSimple(m.getForum_id(), m.getUser_id())) {
            return; // Already exists
        }

        String sql = "INSERT INTO membre_forum (forum_id, user_id, date_adhesion, status) VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, m.getForum_id());
        ps.setInt(2, m.getUser_id());
        ps.setString(3, m.getStatus());
        ps.executeUpdate();

        // Update member count in forum only if accepted
        if ("ACCEPTED".equalsIgnoreCase(m.getStatus())) {
            updateForumMemberCount(m.getForum_id());
        }
    }

    public void supprimer(int forumId, int userId) throws SQLException {
        String sql = "DELETE FROM membre_forum WHERE forum_id = ? AND user_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ps.setInt(2, userId);
        ps.executeUpdate();

        // Update member count in forum
        decrementForumMemberCount(forumId);
    }

    public boolean estMembreSimple(int forumId, int userId) {
        String sql = "SELECT 1 FROM membre_forum WHERE forum_id = ? AND user_id = ?";
        try {
            Connection cnx = UnifiedDatabaseManager.getConnection();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, forumId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean estMembre(int forumId, int userId) {
        String sql = "SELECT 1 FROM membre_forum WHERE forum_id = ? AND user_id = ? AND status = 'ACCEPTED'";
        try {
            Connection cnx = UnifiedDatabaseManager.getConnection();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, forumId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Membre getMembre(int forumId, int userId) {
        String sql = "SELECT * FROM membre_forum WHERE forum_id = ? AND user_id = ?";
        try {
            Connection cnx = UnifiedDatabaseManager.getConnection();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, forumId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Membre m = new Membre();
                m.setForum_id(rs.getInt("forum_id"));
                m.setUser_id(rs.getInt("user_id"));
                m.setDate_adhesion(rs.getTimestamp("date_adhesion"));
                m.setStatus(rs.getString("status"));
                return m;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void accepterMembre(int forumId, int userId) throws SQLException {
        String sql = "UPDATE membre_forum SET status = 'ACCEPTED' WHERE forum_id = ? AND user_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ps.setInt(2, userId);
        ps.executeUpdate();
        updateForumMemberCount(forumId);
    }

    public List<Membre> afficherParForum(int forumId) throws SQLException {
        List<Membre> membres = new ArrayList<>();
        String sql = "SELECT * FROM membre_forum WHERE forum_id = ? AND status = 'ACCEPTED'";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Membre m = new Membre();
            m.setForum_id(rs.getInt("forum_id"));
            m.setUser_id(rs.getInt("user_id"));
            m.setDate_adhesion(rs.getTimestamp("date_adhesion"));
            m.setStatus(rs.getString("status"));
            membres.add(m);
        }
        return membres;
    }

    public List<Membre> afficherDemandesParForum(int forumId) throws SQLException {
        List<Membre> membres = new ArrayList<>();
        String sql = "SELECT * FROM membre_forum WHERE forum_id = ? AND status = 'PENDING'";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Membre m = new Membre();
            m.setForum_id(rs.getInt("forum_id"));
            m.setUser_id(rs.getInt("user_id"));
            m.setDate_adhesion(rs.getTimestamp("date_adhesion"));
            m.setStatus(rs.getString("status"));
            membres.add(m);
        }
        return membres;
    }

    private void updateForumMemberCount(int forumId) throws SQLException {
        String updateSql = "UPDATE forum SET nbr_members = nbr_members + 1 WHERE forum_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement psUpdate = cnx.prepareStatement(updateSql);
        psUpdate.setInt(1, forumId);
        psUpdate.executeUpdate();
    }

    private void decrementForumMemberCount(int forumId) throws SQLException {
        String updateSql = "UPDATE forum SET nbr_members = GREATEST(0, nbr_members - 1) WHERE forum_id = ?";
        Connection cnx = UnifiedDatabaseManager.getConnection();
        PreparedStatement psUpdate = cnx.prepareStatement(updateSql);
        psUpdate.setInt(1, forumId);
        psUpdate.executeUpdate();
    }
}
