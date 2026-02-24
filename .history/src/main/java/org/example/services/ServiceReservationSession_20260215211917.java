package org.example.services;

import org.example.utils.MyDataBase;

import java.sql.*;

public class ServiceReservationSession {

    private final Connection connection;

    public ServiceReservationSession() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void reserver(int sessionId, int nbPlaces, String userRef) throws SQLException {

        connection.setAutoCommit(false);
        try {
            // lock + check places
            String checkSql = "SELECT nbr_places_restant FROM sessions WHERE id_session = ? FOR UPDATE";
            int restant;

            try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                ps.setInt(1, sessionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Session introuvable (ID=" + sessionId + ")");
                    restant = rs.getInt("nbr_places_restant");
                }
            }

            if (restant < nbPlaces) {
                throw new SQLException("Places insuffisantes (restant=" + restant + ")");
            }

            // insert reservation
            String insertSql = "INSERT INTO reservation_session(session_id, nb_places, user_ref) VALUES (?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setInt(1, sessionId);
                ps.setInt(2, nbPlaces);
                ps.setString(3, userRef);
                ps.executeUpdate();
            }

            // update remaining places
            String updateSql = "UPDATE sessions SET nbr_places_restant = nbr_places_restant - ? WHERE id_session = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, nbPlaces);
                ps.setInt(2, sessionId);
                ps.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }
}