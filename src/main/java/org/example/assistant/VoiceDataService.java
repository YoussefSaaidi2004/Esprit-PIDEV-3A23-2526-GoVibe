package org.example.assistant;

import org.example.entities.Activite;
import org.example.entities.Hotel;
import org.example.entities.Voiture;
import org.example.services.ServiceActivite;
import org.example.services.ServiceHotel;
import org.example.services.ServiceVoiture;

import java.util.List;
import java.util.logging.Logger;

/**
 * Builds a compact JSON snapshot of the GoVibe database that is fed to the
 * Python voice agent at startup so Go can answer questions about available
 * activities, cars, and hotels without hitting the DB on every utterance.
 *
 * <p>Protocol: The JSON is sent to {@link PythonVoiceAgent#sendDbContext(String)}
 * which writes it to Python stdin as a {@code {type:"db_context",...}} line.
 * Python stores the snapshot in {@code _db_context} and includes it in every
 * DeepSeek / Ollama system prompt for data-driven answers.
 */
public class VoiceDataService {

    private static final Logger LOG = Logger.getLogger(VoiceDataService.class.getName());

    /** Maximum items to include per category (prevents oversized payloads). */
    private static final int MAX_PER_CATEGORY = 30;

    /**
     * Queries activities, cars, and hotels from the GoVibe DB and returns a
     * single JSON string in the {@code db_context} protocol format expected by
     * {@code voice_agent.py}.
     *
     * <p>Each category is loaded independently — a single category failure does
     * NOT abort the whole payload; the failed section simply appears as an empty
     * array and the voice agent degrades gracefully.
     *
     * @return line-ready JSON string (no trailing newline)
     */
    public static String buildDbContextJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"db_context\"");

        // ── Activities ────────────────────────────────────────────────────────
        sb.append(",\"activities\":[");
        try {
            List<Activite> acts = new ServiceActivite().getAll();
            int count = 0;
            boolean first = true;
            for (Activite a : acts) {
                if (count++ >= MAX_PER_CATEGORY) break;
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(a.getId())
                  .append(",\"name\":").append(jStr(a.getName()))
                  .append(",\"description\":").append(jStr(a.getDescription()))
                  .append(",\"type\":").append(jStr(a.getType()))
                  .append(",\"localisation\":").append(jStr(a.getLocalisation()))
                  .append(",\"prix\":").append(a.getPrix() != null ? a.getPrix().toPlainString() : "0")
                  .append(",\"status\":").append(jStr(a.getStatus()))
                  .append("}");
            }
            LOG.info("[VoiceData] Activities loaded: " + Math.min(acts.size(), MAX_PER_CATEGORY));
        } catch (Exception e) {
            LOG.warning("[VoiceData] Could not load activities: " + e.getMessage());
        }
        sb.append("]");

        // ── Cars ──────────────────────────────────────────────────────────────
        sb.append(",\"cars\":[");
        try {
            List<Voiture> cars = new ServiceVoiture().getAll();
            int count = 0;
            boolean first = true;
            for (Voiture v : cars) {
                if (count++ >= MAX_PER_CATEGORY) break;
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(v.getIdVoiture())
                  .append(",\"marque\":").append(jStr(v.getMarque()))
                  .append(",\"modele\":").append(jStr(v.getModele()))
                  .append(",\"annee\":").append(v.getAnnee())
                  .append(",\"prixJour\":").append(v.getPrixJour())
                  .append(",\"statut\":").append(jStr(v.getStatut() != null ? v.getStatut().name() : "DISPONIBLE"))
                  .append(",\"adresseAgence\":").append(jStr(v.getAdresseAgence()))
                  .append(",\"description\":").append(jStr(v.getDescription()))
                  .append(",\"typeCarburant\":").append(jStr(v.getTypeCarburant() != null ? v.getTypeCarburant().name() : ""))
                  .append("}");
            }
            LOG.info("[VoiceData] Cars loaded: " + Math.min(cars.size(), MAX_PER_CATEGORY));
        } catch (Exception e) {
            LOG.warning("[VoiceData] Could not load cars: " + e.getMessage());
        }
        sb.append("]");

        // ── Hotels ────────────────────────────────────────────────────────────
        sb.append(",\"hotels\":[");
        try {
            List<Hotel> hotels = new ServiceHotel().show();
            int count = 0;
            boolean first = true;
            for (Hotel h : hotels) {
                if (count++ >= MAX_PER_CATEGORY) break;
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"id\":").append(h.getId())
                  .append(",\"nom\":").append(jStr(h.getNom()))
                  .append(",\"ville\":").append(jStr(h.getVille()))
                  .append(",\"nombreEtoiles\":").append(h.getNombreEtoiles())
                  .append(",\"budget\":").append(h.getBudget())
                  .append(",\"description\":").append(jStr(h.getDescription()))
                  .append("}");
            }
            LOG.info("[VoiceData] Hotels loaded: " + Math.min(hotels.size(), MAX_PER_CATEGORY));
        } catch (Exception e) {
            LOG.warning("[VoiceData] Could not load hotels: " + e.getMessage());
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Minimal JSON string escaper — handles the characters that realistically
     * appear in DB text (names, descriptions, addresses).
     */
    private static String jStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
