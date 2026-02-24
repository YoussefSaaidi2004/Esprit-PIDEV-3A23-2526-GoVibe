package org.example.services;

import org.example.entities.Conversation;
import org.example.entities.Message;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConversation {
    
    private final Connection connection;
    
    public ServiceConversation() {
        connection = MyDataBase.getInstance().getConnection();
    }
    
    // ==================== CONVERSATIONS ====================
    
    public Conversation creerConversation(int clientId, String sujet) throws SQLException {
        String sql = "INSERT INTO conversation (client_id, sujet) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, clientId);
            ps.setString(2, sujet);
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Conversation c = new Conversation();
                    c.setId(rs.getInt(1));
                    c.setClientId(clientId);
                    c.setSujet(sujet);
                    c.setStatus(Conversation.STATUS_ACTIF);
                    return c;
                }
            }
        }
        throw new SQLException("Échec création conversation");
    }
    
    public List<Conversation> getConversationsClient(int clientId) throws SQLException {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prenom, p.email, " +
                     "(SELECT COUNT(*) FROM message m WHERE m.conversation_id = c.id AND m.sender_type = 'ADMIN' AND m.status != 'LU') as non_lus " +
                     "FROM conversation c " +
                     "JOIN personne p ON c.client_id = p.id " +
                     "WHERE c.client_id = ? AND c.status != 'FERME' " +
                     "ORDER BY c.last_message_at DESC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapConversation(rs));
                }
            }
        }
        return list;
    }
    
    public List<Conversation> getAllConversationsAdmin() throws SQLException {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom, p.prenom, p.email, " +
                     "(SELECT COUNT(*) FROM message m WHERE m.conversation_id = c.id AND m.sender_type = 'CLIENT' AND m.status != 'LU') as non_lus " +
                     "FROM conversation c " +
                     "JOIN personne p ON c.client_id = p.id " +
                     "WHERE c.status = 'ACTIF' " +
                     "ORDER BY c.last_message_at DESC";
        
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapConversation(rs));
            }
        }
        return list;
    }
    
    public Conversation getConversationById(int id) throws SQLException {
        String sql = "SELECT c.*, p.nom, p.prenom, p.email FROM conversation c " +
                     "JOIN personne p ON c.client_id = p.id WHERE c.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapConversation(rs);
                }
            }
        }
        return null;
    }
    
    // ==================== MESSAGES ====================
    
    public Message envoyerMessage(Message msg) throws SQLException {
        String sql = "INSERT INTO message (conversation_id, sender_id, sender_type, content, type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getConversationId());
            ps.setInt(2, msg.getSenderId());
            ps.setString(3, msg.getSenderType());
            ps.setString(4, msg.getContent());
            ps.setString(5, msg.getType());
            ps.executeUpdate();
            
            // Mettre à jour last_message_at
            updateLastMessageTime(msg.getConversationId());
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    msg.setId(rs.getInt(1));
                    msg.setStatus(Message.STATUS_ENVOYE);
                    return msg;
                }
            }
        }
        throw new SQLException("Échec envoi message");
    }
    
    public List<Message> getMessagesConversation(int conversationId) throws SQLException {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT m.*, p.nom, p.prenom FROM message m " +
                     "JOIN personne p ON m.sender_id = p.id " +
                     "WHERE m.conversation_id = ? ORDER BY m.created_at ASC";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMessage(rs));
                }
            }
        }
        return list;
    }
    
    public void marquerMessagesLus(int conversationId, String readerType) throws SQLException {
        String sql = "UPDATE message SET status = 'LU', lu_at = NOW() " +
                     "WHERE conversation_id = ? AND sender_type != ? AND status != 'LU'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setString(2, readerType);
            ps.executeUpdate();
        }
    }
    
    public int countMessagesNonLusAdmin() throws SQLException {
        String sql = "SELECT COUNT(*) FROM message WHERE sender_type = 'CLIENT' AND status != 'LU'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public int countMessagesNonLusClient(int clientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM message m " +
                     "JOIN conversation c ON m.conversation_id = c.id " +
                     "WHERE c.client_id = ? AND m.sender_type = 'ADMIN' AND m.status != 'LU'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    // ==================== GESTION ====================
    
    public void fermerConversation(int conversationId) throws SQLException {
        String sql = "UPDATE conversation SET status = 'FERME' WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }
    
    public void archiverConversation(int conversationId) throws SQLException {
        String sql = "UPDATE conversation SET status = 'ARCHIVE' WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }
    
    public void supprimerConversation(int conversationId) throws SQLException {
        String sql = "DELETE FROM conversation WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }
    
    // ==================== UTILS ====================
    
    private void updateLastMessageTime(int conversationId) throws SQLException {
        String sql = "UPDATE conversation SET last_message_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }
    
    private Conversation mapConversation(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getInt("id"));
        c.setClientId(rs.getInt("client_id"));
        c.setSujet(rs.getString("sujet"));
        c.setStatus(rs.getString("status"));
        c.setLastMessageAt(rs.getTimestamp("last_message_at"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setClientNom(rs.getString("nom"));
        c.setClientPrenom(rs.getString("prenom"));
        c.setClientEmail(rs.getString("email"));
        try {
            c.setNonLusCount(rs.getInt("non_lus"));
        } catch (SQLException e) {
            c.setNonLusCount(0);
        }
        return c;
    }
    
    private Message mapMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setConversationId(rs.getInt("conversation_id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setSenderType(rs.getString("sender_type"));
        m.setContent(rs.getString("content"));
        m.setType(rs.getString("type"));
        m.setStatus(rs.getString("status"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setLuAt(rs.getTimestamp("lu_at"));
        m.setSenderNom(rs.getString("nom"));
        m.setSenderPrenom(rs.getString("prenom"));
        return m;
    }
}
