package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Charge les fichiers de configuration (oauth2.properties, etc.)
 * de manière centralisée pour tout le projet GoVibe.
 */
public class ConfigLoader {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    private ConfigLoader() {
    }

    /**
     * Charge le fichier config/oauth2.properties depuis le classpath.
     * Appelé une seule fois (lazy loading).
     */
    private static synchronized void loadIfNeeded() {
        if (loaded) return;
        
        // Try multiple classloaders (fixes issues with javafx:run)
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config/oauth2.properties");
        if (is == null) {
            is = ConfigLoader.class.getResourceAsStream("/config/oauth2.properties");
        }
        if (is == null) {
            is = ConfigLoader.class.getClassLoader().getResourceAsStream("config/oauth2.properties");
        }

        try {
            if (is != null) {
                properties.load(is);
                System.out.println("✅ [ConfigLoader] oauth2.properties chargé avec succès");
            } else {
                System.err.println("⚠️ [ConfigLoader] Fichier config/oauth2.properties introuvable dans le classpath !");
            }
        } catch (IOException e) {
            System.err.println("❌ [ConfigLoader] Erreur lors du chargement de la configuration : " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {}
            }
        }
        
        // --- Custom `.env` loading logic without dotenv dependency ---
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (FileInputStream fis = new FileInputStream(envFile)) {
                    Properties envProps = new Properties();
                    envProps.load(fis);
                    for (String key : envProps.stringPropertyNames()) {
                        properties.setProperty(key, envProps.getProperty(key));
                    }
                    System.out.println("✅ [ConfigLoader] .env chargé avec succès");
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ [ConfigLoader] Erreur de lecture de .env : " + e.getMessage());
        }
        // -----------------------------------------------------------

        loaded = true;
    }

    /**
     * Récupère une propriété par sa clé.
     * @param key La clé de la propriété (ex: "google.client.id")
     * @return La valeur ou null si absente
     */
    public static String get(String key) {
        loadIfNeeded();
        return properties.getProperty(key);
    }

    /**
     * Récupère une propriété avec une valeur par défaut.
     */
    public static String get(String key, String defaultValue) {
        loadIfNeeded();
        return properties.getProperty(key, defaultValue);
    }

    // --- Raccourcis pour les propriétés fréquentes ---

    public static String getGoogleClientId() {
        return get("google.client.id");
    }

    public static String getGoogleClientSecret() {
        return get("GOOGLE_CLIENT_SECRET", get("google.client.secret"));
    }

    public static String getGoogleRedirectUri() {
        return get("google.redirect.uri", "http://localhost:8888/oauth2callback");
    }

    public static String getMailHost() {
        return get("mail.smtp.host", "smtp.gmail.com");
    }

    public static int getMailPort() {
        return Integer.parseInt(get("mail.smtp.port", "587"));
    }

    public static String getMailUsername() {
        return get("mail.username");
    }

    public static String getMailPassword() {
        return get("MAIL_PASSWORD", get("mail.password"));
    }

    public static String getMailFrom() {
        return get("mail.from", get("mail.username"));
    }
}
