package org.example.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.example.config.ConfigLoader;
import org.example.entities.personne;
import org.example.utils.MyDataBase;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service d'authentification OAuth2 Google pour application JavaFX Desktop.
 *
 * Flux :
 * 1. Ouvre le navigateur web par défaut sur la page de connexion Google
 * 2. Lance un mini serveur HTTP local (port 8888) pour recevoir le callback
 * 3. Échange le code d'autorisation contre un access_token
 * 4. Récupère les infos utilisateur (email, nom, photo) via l'API Google
 * 5. Crée ou retrouve l'utilisateur dans la BDD
 * 6. Retourne l'objet personne connecté
 */
public class GoogleOAuth2Service {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final Connection connection;
    
    // Garder une référence statique pour pouvoir arrêter un ancien serveur si l'utilisateur relance l'action
    private static HttpServer activeServer;

    public GoogleOAuth2Service() {
        this.clientId = ConfigLoader.getGoogleClientId();
        this.clientSecret = ConfigLoader.getGoogleClientSecret();
        this.redirectUri = ConfigLoader.getGoogleRedirectUri();
        this.connection = MyDataBase.getInstance().getConnection();
    }

    /**
     * Lance le processus complet d'authentification Google.
     * @return CompletableFuture contenant l'utilisateur authentifié, ou null en cas d'échec.
     */
    public CompletableFuture<personne> authenticate() {
        CompletableFuture<personne> future = new CompletableFuture<>();

        if (clientId == null || clientSecret == null || redirectUri == null) {
            System.err.println("❌ [OAuth2] Configuration Google manquante. Vérifiez oauth2.properties.");
            future.completeExceptionally(new IllegalStateException("Configuration Google manquante."));
            return future;
        }

        try {
            // Arrêter proprement un éventuel serveur précédent toujours actif en mémoire
            if (activeServer != null) {
                activeServer.stop(0);
                activeServer = null;
            }

            // 1. Démarrer le serveur HTTP local pour recevoir le callback
            HttpServer server;
            try {
                server = HttpServer.create(new InetSocketAddress(8888), 0);
                activeServer = server;
            } catch (BindException e) {
                System.err.println("❌ [OAuth2] Port 8888 déjà utilisé. Fermez les autres instances de l'application.");
                future.completeExceptionally(new IllegalStateException("Le port 8888 est déjà utilisé par une autre instance de l'application. Veuillez fermer complétement GoVibe puis réessayer."));
                return future;
            }

            server.createContext("/oauth2callback", exchange -> {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String code = extractParam(query, "code");

                    if (code != null) {
                        // Répondre au navigateur
                        String successHtml = "<html><body style='font-family:Arial;text-align:center;padding:50px;background:#1a1a2e;color:#e0e0e0;'>"
                                + "<h1 style='color:#00d4aa;'>✅ Connexion réussie !</h1>"
                                + "<p>Vous pouvez fermer cette fenêtre et retourner à GoVibe.</p>"
                                + "</body></html>";
                        byte[] response = successHtml.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.getResponseBody().close();

                        // 2. Échanger le code contre un access_token
                        String accessToken = exchangeCodeForToken(code);

                        if (accessToken != null) {
                            // 3. Récupérer les infos utilisateur
                            JsonObject userInfo = getUserInfo(accessToken);

                            // 4. Créer ou retrouver l'utilisateur en BDD
                            personne user = findOrCreateUser(userInfo);
                            future.complete(user);
                        } else {
                            future.complete(null);
                        }
                    } else {
                        // Erreur (ex: utilisateur a refusé)
                        String error = extractParam(query, "error");
                        String errorHtml = "<html><body style='font-family:Arial;text-align:center;padding:50px;background:#1a1a2e;color:#e0e0e0;'>"
                                + "<h1 style='color:#ff6b6b;'>❌ Connexion annulée</h1>"
                                + "<p>Erreur : " + (error != null ? error : "inconnue") + "</p>"
                                + "<p>Vous pouvez fermer cette fenêtre.</p>"
                                + "</body></html>";
                        byte[] response = errorHtml.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        exchange.getResponseBody().write(response);
                        exchange.getResponseBody().close();
                        future.complete(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                } finally {
                    // Arrêter le serveur après traitement
                    server.stop(1);
                }
            });

            server.start();
            System.out.println("✅ [OAuth2] Serveur callback démarré sur http://localhost:8888");

            // 5. Ouvrir le navigateur pour la connexion Google
            String authUrl = buildAuthUrl();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                // Fallback : essayer la commande système
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", authUrl});
            }

            // Timeout de 2 minutes
            CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES).execute(() -> {
                if (!future.isDone()) {
                    future.complete(null);
                    if (server != null) {
                        server.stop(0);
                        if (activeServer == server) activeServer = null;
                    }
                    System.out.println("⚠️ [OAuth2] Timeout : l'utilisateur n'a pas complété la connexion Google.");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Construit l'URL d'autorisation Google.
     */
    private String buildAuthUrl() {
        return AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&access_type=offline"
                + "&prompt=consent";
    }

    /**
     * Échange le code d'autorisation contre un access_token.
     */
    private String exchangeCodeForToken(String code) {
        try {
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "code=" + encode(code)
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&grant_type=authorization_code";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                String responseBody = readStream(conn.getInputStream());
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                String accessToken = json.get("access_token").getAsString();
                System.out.println("✅ [OAuth2] Access token obtenu avec succès");
                return accessToken;
            } else {
                String errorBody = readStream(conn.getErrorStream());
                System.err.println("❌ [OAuth2] Erreur token exchange : " + conn.getResponseCode() + " - " + errorBody);
            }
        } catch (Exception e) {
            System.err.println("❌ [OAuth2] Erreur lors de l'échange du code : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Récupère les informations de l'utilisateur via l'API Google UserInfo.
     */
    private JsonObject getUserInfo(String accessToken) {
        try {
            URL url = new URL(USERINFO_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            if (conn.getResponseCode() == 200) {
                String responseBody = readStream(conn.getInputStream());
                JsonObject userInfo = JsonParser.parseString(responseBody).getAsJsonObject();
                System.out.println("✅ [OAuth2] Infos utilisateur récupérées : " + userInfo.get("email").getAsString());
                return userInfo;
            } else {
                System.err.println("❌ [OAuth2] Erreur userinfo : " + conn.getResponseCode());
            }
        } catch (Exception e) {
            System.err.println("❌ [OAuth2] Erreur lors de la récupération des infos utilisateur : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cherche l'utilisateur en BDD par email. S'il n'existe pas, le crée.
     * Gère le cas de double inscription (compte local existant).
     */
    private personne findOrCreateUser(JsonObject userInfo) {
        if (userInfo == null) return null;

        String email = userInfo.has("email") ? userInfo.get("email").getAsString() : null;
        String name = userInfo.has("name") ? userInfo.get("name").getAsString() : "";
        String sub = userInfo.has("sub") ? userInfo.get("sub").getAsString() : "";
        String picture = userInfo.has("picture") ? userInfo.get("picture").getAsString() : null;

        if (email == null || email.isEmpty()) {
            System.err.println("❌ [OAuth2] Pas d'email retourné par Google !");
            return null;
        }

        try {
            // Chercher si l'utilisateur existe déjà (par email)
            String selectQuery = "SELECT * FROM personne WHERE email = ?";
            PreparedStatement ps = connection.prepareStatement(selectQuery);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // L'utilisateur existe déjà → le retourner
                personne p = new personne();
                p.setId(rs.getInt("id"));
                p.setNom(rs.getString("nom"));
                p.setPrenom(rs.getString("prenom"));
                p.setEmail(rs.getString("email"));
                p.setPassword(rs.getString("password"));
                p.setRole(rs.getString("role"));

                // Mettre à jour le provider si c'était un compte local
                String currentProvider = rs.getString("provider");
                if (currentProvider == null || "local".equals(currentProvider)) {
                    // L'utilisateur avait un compte classique → mettre à jour le provider
                    String updateQuery = "UPDATE personne SET provider = ?, provider_id = ?, photo_url = ? WHERE id = ?";
                    PreparedStatement updatePs = connection.prepareStatement(updateQuery);
                    updatePs.setString(1, "google");
                    updatePs.setString(2, sub);
                    updatePs.setString(3, picture);
                    updatePs.setInt(4, p.getId());
                    updatePs.executeUpdate();
                    System.out.println("✅ [OAuth2] Compte local existant lié à Google : " + email);
                }

                p.setProvider("google");
                p.setProviderId(sub);
                p.setPhotoUrl(picture);
                return p;

            } else {
                // Nouvel utilisateur → Créer en BDD
                String[] nameParts = name.split(" ", 2);
                String prenom = nameParts.length > 0 ? nameParts[0] : "";
                String nom = nameParts.length > 1 ? nameParts[1] : "";

                String insertQuery = "INSERT INTO personne (nom, prenom, email, password, role, provider, provider_id, photo_url) VALUES (?, ?, ?, NULL, 'USER', 'google', ?, ?)";
                PreparedStatement insertPs = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                insertPs.setString(1, nom);
                insertPs.setString(2, prenom);
                insertPs.setString(3, email);
                insertPs.setString(4, sub);
                insertPs.setString(5, picture);
                insertPs.executeUpdate();

                ResultSet generatedKeys = insertPs.getGeneratedKeys();
                int newId = 0;
                if (generatedKeys.next()) {
                    newId = generatedKeys.getInt(1);
                }

                personne p = new personne();
                p.setId(newId);
                p.setNom(nom);
                p.setPrenom(prenom);
                p.setEmail(email);
                p.setRole("USER");
                p.setProvider("google");
                p.setProviderId(sub);
                p.setPhotoUrl(picture);

                System.out.println("✅ [OAuth2] Nouvel utilisateur créé via Google : " + email);
                return p;
            }

        } catch (SQLException e) {
            System.err.println("❌ [OAuth2] Erreur BDD lors de la gestion de l'utilisateur : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // --- Utilitaires ---

    private String extractParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && pair[0].equals(paramName)) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
