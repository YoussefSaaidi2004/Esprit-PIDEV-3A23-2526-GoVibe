package tn.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.entities.Membre;
import tn.esprit.utils.MyDataBase;

public class ServiceMembre {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    public void ajouter(Membre m) throws SQLException {
        if (estMembre(m.getForum_id(), m.getUser_id())) {
            return; // Already a member
        }

        String sql = "INSERT INTO membre_forum (forum_id, user_id, date_adhesion) VALUES (?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, m.getForum_id());
        ps.setInt(2, m.getUser_id());
        ps.executeUpdate();

        // Update member count in forum
        updateForumMemberCount(m.getForum_id());
    }

    public void supprimer(int forumId, int userId) throws SQLException {
        String sql = "DELETE FROM membre_forum WHERE forum_id = ? AND user_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ps.setInt(2, userId);
        ps.executeUpdate();

        // Update member count in forum
        // Ideally we should decrement, but logic might vary. For now let's just insert.
    }

    public boolean estMembre(int forumId, int userId) {
        String sql = "SELECT 1 FROM membre_forum WHERE forum_id = ? AND user_id = ?";
        try {
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

    public List<Membre> afficherParForum(int forumId) throws SQLException {
        List<Membre> membres = new ArrayList<>();
        String sql = "SELECT * FROM membre_forum WHERE forum_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Membre m = new Membre();
            m.setForum_id(rs.getInt("forum_id"));
            m.setUser_id(rs.getInt("user_id"));
            m.setDate_adhesion(rs.getTimestamp("date_adhesion"));
            membres.add(m);
        }
        return membres;
    }

    private void updateForumMemberCount(int forumId) throws SQLException {
        String updateSql = "UPDATE forum SET nbr_members = nbr_members + 1 WHERE forum_id = ?";
        PreparedStatement psUpdate = cnx.prepareStatement(updateSql);
        psUpdate.setInt(1, forumId);
        psUpdate.executeUpdate();
    }
}
