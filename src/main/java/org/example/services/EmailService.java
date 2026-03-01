package org.example.services;

import org.example.config.ConfigLoader;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Service d'envoi d'emails pour GoVibe.
 * Supporte l'envoi de texte brut et de contenu HTML.
 * Les credentials sont chargés depuis config/oauth2.properties.
 */
public class EmailService {

    private final String username;
    private final String password;
    private final String fromAddress;
    private final Session session;

    public EmailService() {
        // Charger la configuration depuis ConfigLoader
        this.username = ConfigLoader.getMailUsername();
        this.password = ConfigLoader.getMailPassword();
        this.fromAddress = ConfigLoader.getMailFrom();

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", ConfigLoader.getMailHost());
        props.put("mail.smtp.port", String.valueOf(ConfigLoader.getMailPort()));

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    /**
     * Envoie un email en texte brut.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("✅ [Email] Envoyé avec succès à " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ [Email] Échec de l'envoi à " + toEmail + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie un email en HTML (format riche).
     * Permet des emails plus professionnels avec mise en forme,
     * images, liens, etc.
     *
     * @param toEmail   Adresse du destinataire
     * @param subject   Sujet de l'email
     * @param htmlBody  Contenu HTML de l'email
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("✅ [Email HTML] Envoyé avec succès à " + toEmail);
        } catch (MessagingException e) {
            System.err.println("❌ [Email HTML] Échec de l'envoi à " + toEmail + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie un email de bienvenue pour un nouvel utilisateur Google OAuth2.
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "🎉 Bienvenue sur GoVibe !";
        String htmlBody = "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:30px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:28px;'>GoVibe</h1>"
                + "</div>"
                + "<div style='padding:30px;'>"
                + "<h2 style='color:#333;'>Bonjour " + userName + " 👋</h2>"
                + "<p style='color:#666;font-size:16px;line-height:1.6;'>Votre compte a été créé avec succès via Google. "
                + "Vous pouvez désormais profiter de toutes les fonctionnalités de GoVibe !</p>"
                + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'>"
                + "<p style='color:#999;font-size:12px;text-align:center;'>Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>"
                + "</div></div></body></html>";

        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    /**
     * Envoie un email de réinitialisation de mot de passe.
     */
    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        String subject = "🔐 Réinitialisation de votre mot de passe GoVibe";
        String htmlBody = "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;'>"
                + "<div style='max-width:600px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                + "<div style='background:linear-gradient(135deg,#f093fb 0%,#f5576c 100%);padding:30px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:28px;'>GoVibe</h1>"
                + "</div>"
                + "<div style='padding:30px;text-align:center;'>"
                + "<h2 style='color:#333;'>Réinitialisation de mot de passe</h2>"
                + "<p style='color:#666;font-size:16px;'>Votre code de vérification est :</p>"
                + "<div style='background:#f0f0f0;border-radius:8px;padding:20px;margin:20px 0;'>"
                + "<span style='font-size:32px;font-weight:bold;letter-spacing:8px;color:#333;'>" + resetCode + "</span>"
                + "</div>"
                + "<p style='color:#999;font-size:14px;'>Ce code expire dans 15 minutes.</p>"
                + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'>"
                + "<p style='color:#999;font-size:12px;'>Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.</p>"
                + "</div></div></body></html>";

        sendHtmlEmail(toEmail, subject, htmlBody);
    }
}
