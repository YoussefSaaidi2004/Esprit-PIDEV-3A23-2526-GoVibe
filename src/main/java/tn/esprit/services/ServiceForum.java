package tn.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.entities.Forum;
import tn.esprit.utils.MyDataBase;

public class ServiceForum implements iForum<Forum> {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    @Override
    public void ajouter(Forum f) throws SQLException {
        String sql = "INSERT INTO forum (name, image, created_by, post_count, nbr_members, description, date_creation, is_private) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getName());
        ps.setString(2, f.getImage());
        ps.setInt(3, f.getCreated_by());
        ps.setInt(4, f.getPost_count());
        ps.setInt(5, f.getNbr_members());
        ps.setString(6, f.getDescription());
        ps.setTimestamp(7, f.getDate_creation());
        ps.setBoolean(8, f.isIs_private());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Forum f) throws SQLException {
        String sql = "UPDATE forum SET name=?, image=?, created_by=?, post_count=?, nbr_members=?, description=?, date_creation=?, is_private=? WHERE forum_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, f.getName());
        ps.setString(2, f.getImage());
        ps.setInt(3, f.getCreated_by());
        ps.setInt(4, f.getPost_count());
        ps.setInt(5, f.getNbr_members());
        ps.setString(6, f.getDescription());
        ps.setTimestamp(7, f.getDate_creation());
        ps.setBoolean(8, f.isIs_private());
        ps.setInt(9, f.getForum_id());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM forum WHERE forum_id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Forum> afficher() throws SQLException {
        List<Forum> forums = new ArrayList<>();
        String sql = "SELECT * FROM forum";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Forum f = new Forum();
            f.setForum_id(rs.getInt("forum_id"));
            f.setName(rs.getString("name"));
            f.setImage(rs.getString("image"));
            f.setCreated_by(rs.getInt("created_by"));
            f.setPost_count(rs.getInt("post_count"));
            f.setNbr_members(rs.getInt("nbr_members"));
            f.setDescription(rs.getString("description"));
            f.setDate_creation(rs.getTimestamp("date_creation"));
            f.setIs_private(rs.getBoolean("is_private"));
            forums.add(f);
        }
        return forums;
    }

    // --- Membership Methods ---

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

    public void ajouterMembre(int forumId, int userId) throws SQLException {
        if (estMembre(forumId, userId)) {
            return; // Already a member
        }

        String sql = "INSERT INTO membre_forum (forum_id, user_id, date_adhesion) VALUES (?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, forumId);
        ps.setInt(2, userId);
        ps.executeUpdate();

        // Increment member count in forum table
        String updateSql = "UPDATE forum SET nbr_members = nbr_members + 1 WHERE forum_id = ?";
        PreparedStatement psUpdate = cnx.prepareStatement(updateSql);
        psUpdate.setInt(1, forumId);
        psUpdate.executeUpdate();
    }

    public void ajouterMembreParEmail(int forumId, String email) throws SQLException {
        tn.esprit.services.ServicePersonne sp = new tn.esprit.services.ServicePersonne();
        tn.esprit.entities.Personne p = sp.getOneByEmail(email);
        if (p != null) {
            ajouterMembre(forumId, p.getId());
        } else {
            throw new SQLException("Utilisateur introuvable avec l'email : " + email);
        }
    }
}
