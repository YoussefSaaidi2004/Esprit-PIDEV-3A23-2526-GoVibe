package org.example.services;

import org.example.entities.Chambre;
import org.example.entities.Hotel;
import org.example.utils.MyDataBase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🤖 Hotel RAG Chatbot — GoVibe Hotel Module
 * Pure Java RAG (Retrieval-Augmented Generation) — NO external API.
 *
 * Architecture:
 *   1. INDEXING  — Load all hotel/chambre/reservation data from DB into memory
 *   2. RETRIEVAL — Find relevant documents matching the user's query keywords
 *   3. GENERATION— Build a natural, coherent answer from the retrieved data
 *
 * Intent detection: greetings, price queries, availability, amenities, location,
 *                   booking, loyalty, promo codes, weather, recommendations.
 */
public class HotelChatbotRAG {

    // ─── Knowledge Base (In-Memory Index) ─────────────────────────────────────

    private record HotelDocument(Hotel hotel, List<Chambre> chambres) {}

    private final List<HotelDocument> knowledgeBase = new ArrayList<>();
    private boolean loaded = false;

    // ─── Conversation Memory ───────────────────────────────────────────────────

    private final List<String[]> conversationHistory = new ArrayList<>();
    private String lastMentionedHotel = null;
    private String lastMentionedType  = null;

    // ─── Init ─────────────────────────────────────────────────────────────────

    public HotelChatbotRAG() {
        try {
            loadKnowledgeBase();
        } catch (Exception e) {
            System.err.println("⚠️ Chatbot RAG: Could not load knowledge base — " + e.getMessage());
        }
    }

    private void loadKnowledgeBase() throws SQLException {
        Connection conn = MyDataBase.getInstance().getMyConnection();
        knowledgeBase.clear();

        // Load all hotels
        Map<Integer, Hotel> hotels = new LinkedHashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM hotel")) {
            while (rs.next()) {
                Hotel h = new Hotel(
                    rs.getInt("id"),
                    rs.getString("nom"),
                    rs.getString("adresse"),
                    rs.getString("ville"),
                    rs.getInt("nombre_etoiles"),
                    rs.getString("description"),
                    rs.getString("photo_url"),
                    rs.getDouble("budget")
                );
                hotels.put(h.getId(), h);
            }
        }

        // Load all chambres
        Map<Integer, List<Chambre>> chambresByHotel = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM chambre")) {
            while (rs.next()) {
                Chambre c = new Chambre(
                    rs.getInt("id"),
                    rs.getString("type"),
                    rs.getInt("capacite"),
                    rs.getString("equipements"),
                    rs.getInt("hotel_id"),
                    rs.getDouble("prix_standard"),
                    rs.getDouble("prix_haute_saison"),
                    rs.getDouble("prix_basse_saison")
                );
                chambresByHotel.computeIfAbsent(c.getHotelId(), k -> new ArrayList<>()).add(c);
            }
        }

        // Build knowledge base documents
        for (Hotel h : hotels.values()) {
            List<Chambre> ch = chambresByHotel.getOrDefault(h.getId(), new ArrayList<>());
            knowledgeBase.add(new HotelDocument(h, ch));
        }
        loaded = true;
        System.out.println("✅ Chatbot RAG: Indexed " + knowledgeBase.size() + " hotels");
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Main method — process user question and return a response.
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "❓ Pouvez-vous reformuler votre question ?";

        String msg = userMessage.trim().toLowerCase();
        conversationHistory.add(new String[]{"user", userMessage});

        String response = processQuery(msg, userMessage);
        conversationHistory.add(new String[]{"assistant", response});
        return response;
    }

    /**
     * Reload knowledge base from DB.
     */
    public void reload() {
        try {
            loadKnowledgeBase();
        } catch (Exception e) {
            System.err.println("⚠️ Chatbot RAG reload error: " + e.getMessage());
        }
    }

    public void clearHistory() {
        conversationHistory.clear();
        lastMentionedHotel = null;
        lastMentionedType = null;
    }

    // ─── Query Processing ─────────────────────────────────────────────────────

    private String processQuery(String msg, String original) {
        // 1. Handle greetings
        if (isGreeting(msg)) return generateGreeting();

        // 2. Handle goodbyes
        if (isGoodbye(msg)) return "Au revoir ! 🙌 Merci d'utiliser GoVibe. À bientôt ! 🌟";

        // 3. Handle help/capabilities
        if (isHelp(msg)) return generateHelp();

        // 4. Load knowledge if not loaded
        if (!loaded || knowledgeBase.isEmpty()) {
            return "⚠️ Je n'ai pas encore accès aux données hôtelières. Veuillez réessayer dans quelques instants.";
        }

        // 5. Detect intent and extract entities
        Intent intent = detectIntent(msg);
        List<HotelDocument> relevant = retrieveRelevantDocs(msg);

        // 6. Generate response based on intent
        return generateResponse(intent, msg, relevant);
    }

    // ─── Intent Detection ─────────────────────────────────────────────────────

    private enum Intent {
        HOTEL_LIST, HOTEL_INFO, CHAMBRE_INFO, PRICE_QUERY,
        AVAILABILITY, AMENITY_QUERY, LOCATION_QUERY,
        BOOKING_INFO, LOYALTY_INFO, PROMO_INFO,
        RECOMMENDATION, RATING, GENERAL
    }

    private Intent detectIntent(String msg) {
        if (contains(msg, "liste", "tous", "hôtels", "hotels", "disponibles")) return Intent.HOTEL_LIST;
        if (contains(msg, "chambre", "chambres", "type", "suite", "studio", "simple", "double")) return Intent.CHAMBRE_INFO;
        if (contains(msg, "prix", "tarif", "coût", "combien", "budget", "cher", "moins cher")) return Intent.PRICE_QUERY;
        if (contains(msg, "disponible", "disponibilité", "libre", "réserver", "reservation")) return Intent.AVAILABILITY;
        if (contains(msg, "piscine", "spa", "wifi", "restaurant", "parking", "jacuzzi",
                         "équipement", "service", "climatisation", "petit-déjeuner")) return Intent.AMENITY_QUERY;
        if (contains(msg, "où", "situé", "ville", "adresse", "location", "région")) return Intent.LOCATION_QUERY;
        if (contains(msg, "réservation", "réserver", "booking", "confirmer")) return Intent.BOOKING_INFO;
        if (contains(msg, "fidélité", "points", "silver", "gold", "platinum", "bonus", "réduction")) return Intent.LOYALTY_INFO;
        if (contains(msg, "promo", "code promo", "coupon", "remise", "hotel10", "summer")) return Intent.PROMO_INFO;
        if (contains(msg, "recommande", "recommandation", "meilleur", "top", "conseille")) return Intent.RECOMMENDATION;
        if (contains(msg, "étoiles", "note", "rating", "classement", "avis")) return Intent.RATING;
        if (contains(msg, "hôtel", "hotel")) return Intent.HOTEL_INFO;
        return Intent.GENERAL;
    }

    private boolean contains(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ─── Document Retrieval ───────────────────────────────────────────────────

    private List<HotelDocument> retrieveRelevantDocs(String query) {
        if (knowledgeBase.isEmpty()) return new ArrayList<>();

        String[] queryTokens = query.toLowerCase().split("\\s+");

        // Score each document
        Map<HotelDocument, Double> scores = new HashMap<>();
        for (HotelDocument doc : knowledgeBase) {
            double score = scoreDocument(doc, queryTokens, query);
            if (score > 0) scores.put(doc, score);
        }

        // Sort by score descending
        return scores.entrySet().stream()
                .sorted(Map.Entry.<HotelDocument, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double scoreDocument(HotelDocument doc, String[] queryTokens, String fullQuery) {
        Hotel h = doc.hotel();
        double score = 0;

        // Build a searchable text for this document
        String docText = (
            h.getNom() + " " + h.getVille() + " " + h.getAdresse() + " " +
            h.getDescription() + " " +
            doc.chambres().stream()
               .map(c -> c.getType() + " " + c.getEquipements())
               .collect(Collectors.joining(" "))
        ).toLowerCase();

        // Token matching
        for (String token : queryTokens) {
            if (token.length() < 3) continue;
            if (docText.contains(token)) score += 1.0;
        }

        // Bonus for exact hotel name match
        if (h.getNom() != null && fullQuery.contains(h.getNom().toLowerCase())) score += 5.0;

        // Bonus for city match
        if (h.getVille() != null && fullQuery.contains(h.getVille().toLowerCase())) score += 3.0;

        // Remember last mentioned hotel for context
        if (score > 2 && h.getNom() != null) lastMentionedHotel = h.getNom();

        return score;
    }

    // ─── Response Generation ──────────────────────────────────────────────────

    private String generateResponse(Intent intent, String msg, List<HotelDocument> docs) {
        switch (intent) {
            case HOTEL_LIST:      return generateHotelList();
            case PRICE_QUERY:     return generatePriceInfo(docs, msg);
            case CHAMBRE_INFO:    return generateChambreInfo(docs, msg);
            case AMENITY_QUERY:   return generateAmenityInfo(docs, msg);
            case LOCATION_QUERY:  return generateLocationInfo(docs);
            case RECOMMENDATION:  return generateRecommendation(msg);
            case RATING:          return generateRatingInfo(docs);
            case LOYALTY_INFO:    return generateLoyaltyInfo();
            case PROMO_INFO:      return generatePromoInfo();
            case BOOKING_INFO:    return generateBookingInfo();
            case HOTEL_INFO:      return docs.isEmpty() ? generateHotelList() : generateHotelDetail(docs.get(0));
            default:              return docs.isEmpty() ? generateGeneralResponse(msg) : generateHotelDetail(docs.get(0));
        }
    }

    private String generateHotelList() {
        if (knowledgeBase.isEmpty()) return "❌ Aucun hôtel trouvé dans la base de données.";

        StringBuilder sb = new StringBuilder();
        sb.append("🏨 **Hôtels disponibles sur GoVibe :**\n\n");
        for (HotelDocument doc : knowledgeBase) {
            Hotel h = doc.hotel();
            sb.append(String.format("• **%s** — %s\n  %s⭐ | À partir de %.0f DT/nuit | %d chambres\n\n",
                h.getNom(), h.getVille(),
                "⭐".repeat(Math.max(0, Math.min(h.getNombreEtoiles(), 5))),
                doc.chambres().stream().mapToDouble(Chambre::getPrixStandard).min().orElse(0),
                doc.chambres().size()
            ));
        }
        sb.append("💬 Posez-moi des questions sur un hôtel spécifique pour plus de détails !");
        return sb.toString();
    }

    private String generateHotelDetail(HotelDocument doc) {
        Hotel h = doc.hotel();
        lastMentionedHotel = h.getNom();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🏨 **%s**\n", h.getNom()));
        sb.append(String.format("📍 %s, %s\n", h.getVille(), h.getAdresse()));
        sb.append("⭐".repeat(Math.max(0, Math.min(h.getNombreEtoiles(), 5)))).append("\n\n");
        if (h.getDescription() != null && !h.getDescription().isBlank()) {
            sb.append("📄 ").append(h.getDescription()).append("\n\n");
        }
        sb.append(String.format("💰 Budget estimé: %.0f DT\n\n", h.getBudget()));

        if (!doc.chambres().isEmpty()) {
            sb.append("🛏️ **Chambres disponibles:**\n");
            for (Chambre c : doc.chambres()) {
                sb.append(String.format("  • %s | 👥 %d pers. | %.0f DT/nuit\n",
                    c.getType(), c.getCapacite(), c.getPrixStandard()));
            }
        }
        return sb.toString();
    }

    private String generatePriceInfo(List<HotelDocument> docs, String msg) {
        if (docs.isEmpty()) return generateGeneralPriceOverview();

        StringBuilder sb = new StringBuilder();
        sb.append("💰 **Tarifs hôteliers:**\n\n");
        for (HotelDocument doc : docs) {
            Hotel h = doc.hotel();
            sb.append(String.format("🏨 **%s** (%s)\n", h.getNom(), h.getVille()));
            for (Chambre c : doc.chambres()) {
                sb.append(String.format(
                    "  🛏 %s (👥%d) | Standard: %.0f DT | Haute Saison: %.0f DT | Basse: %.0f DT\n",
                    c.getType(), c.getCapacite(),
                    c.getPrixStandard(), c.getPrixHauteSaison(), c.getPrixBasseSaison()
                ));
            }
            sb.append("\n");
        }
        sb.append("💡 *Utilisez le code **HOTEL10** pour -10% ou **SUMMER2026** pour -15% !*");
        return sb.toString();
    }

    private String generateGeneralPriceOverview() {
        if (knowledgeBase.isEmpty()) return "Aucune donnée de prix disponible.";
        double minPrice = knowledgeBase.stream()
            .flatMap(d -> d.chambres().stream())
            .mapToDouble(Chambre::getPrixStandard)
            .min().orElse(0);
        double maxPrice = knowledgeBase.stream()
            .flatMap(d -> d.chambres().stream())
            .mapToDouble(Chambre::getPrixStandard)
            .max().orElse(0);
        return String.format(
            "💰 **Gamme de prix GoVibe:**\n\nLes tarifs varient de **%.0f DT** à **%.0f DT** par nuit.\n\n" +
            "💡 Codes promo actifs:\n• **HOTEL10** — -10%% sur votre réservation\n• **SUMMER2026** — -15%% (offre été)\n",
            minPrice, maxPrice
        );
    }

    private String generateChambreInfo(List<HotelDocument> docs, String msg) {
        if (docs.isEmpty()) {
            // Show all room types across all hotels
            StringBuilder sb = new StringBuilder("🛏️ **Types de chambres disponibles:**\n\n");
            for (HotelDocument doc : knowledgeBase) {
                if (!doc.chambres().isEmpty()) {
                    sb.append(String.format("🏨 %s:\n", doc.hotel().getNom()));
                    for (Chambre c : doc.chambres()) {
                        sb.append(String.format("  • %s — 👥 %d pers. | 💰 %.0f DT/nuit\n    ✨ %s\n",
                            c.getType(), c.getCapacite(), c.getPrixStandard(), c.getEquipements()));
                    }
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (HotelDocument doc : docs) {
            sb.append(String.format("🏨 **%s** — Chambres:\n\n", doc.hotel().getNom()));
            for (Chambre c : doc.chambres()) {
                sb.append(String.format(
                    "🛏 **%s**\n  👥 Capacité: %d personnes\n  💰 %.0f DT/nuit (std) | 🌞 %.0f DT (haute) | 🍂 %.0f DT (basse)\n  ✨ %s\n\n",
                    c.getType(), c.getCapacite(), c.getPrixStandard(),
                    c.getPrixHauteSaison(), c.getPrixBasseSaison(), c.getEquipements()
                ));
            }
        }
        return sb.toString();
    }

    private String generateAmenityInfo(List<HotelDocument> docs, String msg) {
        StringBuilder sb = new StringBuilder("✨ **Services & Équipements:**\n\n");
        List<HotelDocument> targets = docs.isEmpty() ? knowledgeBase : docs;
        for (HotelDocument doc : targets) {
            boolean hasEquipment = false;
            for (Chambre c : doc.chambres()) {
                if (c.getEquipements() != null && !c.getEquipements().isBlank()) {
                    if (!hasEquipment) {
                        sb.append(String.format("🏨 **%s:**\n", doc.hotel().getNom()));
                        hasEquipment = true;
                    }
                    sb.append(String.format("  🛏 %s: %s\n", c.getType(), c.getEquipements()));
                }
            }
            if (hasEquipment) sb.append("\n");
        }
        return sb.toString();
    }

    private String generateLocationInfo(List<HotelDocument> docs) {
        if (docs.isEmpty()) {
            StringBuilder sb = new StringBuilder("📍 **Nos hôtels par ville:**\n\n");
            Map<String, List<Hotel>> byCity = new LinkedHashMap<>();
            for (HotelDocument doc : knowledgeBase) {
                String city = doc.hotel().getVille() != null ? doc.hotel().getVille() : "Ville inconnue";
                byCity.computeIfAbsent(city, k -> new ArrayList<>()).add(doc.hotel());
            }
            for (Map.Entry<String, List<Hotel>> entry : byCity.entrySet()) {
                sb.append(String.format("📌 **%s:**\n", entry.getKey()));
                for (Hotel h : entry.getValue()) {
                    sb.append(String.format("  • %s — %s\n", h.getNom(), h.getAdresse()));
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        StringBuilder sb = new StringBuilder("📍 **Localisation:**\n\n");
        for (HotelDocument doc : docs) {
            Hotel h = doc.hotel();
            sb.append(String.format("🏨 **%s**\n📌 %s\n🏙 %s\n\n", h.getNom(), h.getAdresse(), h.getVille()));
        }
        return sb.toString();
    }

    private String generateRecommendation(String msg) {
        // Recommend based on rating/budget
        boolean luxe = msg.contains("luxe") || msg.contains("premium") || msg.contains("meilleur");
        boolean economic = msg.contains("pas cher") || msg.contains("économique") || msg.contains("budget");

        List<HotelDocument> sorted;
        if (luxe) {
            sorted = knowledgeBase.stream()
                .sorted((a, b) -> Integer.compare(b.hotel().getNombreEtoiles(), a.hotel().getNombreEtoiles()))
                .limit(3)
                .collect(Collectors.toList());
        } else if (economic) {
            sorted = knowledgeBase.stream()
                .filter(d -> !d.chambres().isEmpty())
                .sorted(Comparator.comparingDouble(d -> d.chambres().stream()
                    .mapToDouble(Chambre::getPrixStandard).min().orElse(Double.MAX_VALUE)))
                .limit(3)
                .collect(Collectors.toList());
        } else {
            sorted = knowledgeBase.stream().limit(3).collect(Collectors.toList());
        }

        StringBuilder sb = new StringBuilder("⭐ **Nos recommandations:**\n\n");
        int rank = 1;
        for (HotelDocument doc : sorted) {
            Hotel h = doc.hotel();
            double minPrice = doc.chambres().stream().mapToDouble(Chambre::getPrixStandard).min().orElse(0);
            sb.append(String.format("#%d 🏨 **%s** — %s\n  %s⭐ | Dès %.0f DT/nuit\n\n",
                rank++, h.getNom(), h.getVille(),
                "⭐".repeat(Math.max(0, Math.min(h.getNombreEtoiles(), 5))),
                minPrice
            ));
        }
        return sb.toString();
    }

    private String generateRatingInfo(List<HotelDocument> docs) {
        List<HotelDocument> targets = docs.isEmpty() ? knowledgeBase : docs;
        StringBuilder sb = new StringBuilder("⭐ **Classement des hôtels:**\n\n");
        targets.stream()
            .sorted((a, b) -> Integer.compare(b.hotel().getNombreEtoiles(), a.hotel().getNombreEtoiles()))
            .forEach(doc -> {
                Hotel h = doc.hotel();
                sb.append(String.format("🏨 **%s** — %s\n",  h.getNom(), h.getVille()));
                sb.append("⭐".repeat(Math.max(0, Math.min(h.getNombreEtoiles(), 5)))).append("\n\n");
            });
        return sb.toString();
    }

    private String generateLoyaltyInfo() {
        return "🎯 **Programme Fidélité GoVibe:**\n\n" +
               "Gagnez des points à chaque réservation (1 point = 1 DT dépensé) !\n\n" +
               "**Niveaux:**\n" +
               "🥉 **Bronze**   — 0 à 499 pts\n" +
               "🥈 **Silver**   — 500 à 1499 pts → -5% sur réservations\n" +
               "🥇 **Gold**     — 1500 à 2999 pts → -10% sur réservations\n" +
               "💎 **Platinum** — 3000+ pts → -15% + priorité réservation\n\n" +
               "💡 Vos points s'accumulent automatiquement à chaque séjour !";
    }

    private String generatePromoInfo() {
        return "🏷️ **Codes Promo GoVibe:**\n\n" +
               "✅ **HOTEL10**   — -10% sur votre réservation hôtel\n" +
               "✅ **SUMMER2026** — -15% (offre spéciale été 2026)\n" +
               "✅ **WELCOME50** — -50 DT bienvenue (montant fixe)\n\n" +
               "📌 Comment utiliser:\n" +
               "  1. Lors de votre réservation\n" +
               "  2. Entrez le code dans le champ 'Code Promo'\n" +
               "  3. La réduction s'applique automatiquement !\n\n" +
               "⚠️ *Les codes sont limités et ont une date d'expiration.*";
    }

    private String generateBookingInfo() {
        return "📋 **Comment réserver sur GoVibe:**\n\n" +
               "1️⃣ Choisissez votre hôtel dans la liste\n" +
               "2️⃣ Sélectionnez les dates de séjour\n" +
               "3️⃣ Le système choisit automatiquement la meilleure chambre disponible\n" +
               "4️⃣ Entrez un code promo si vous en avez un (ex: HOTEL10)\n" +
               "5️⃣ Confirmez et recevez votre QR code de check-in !\n\n" +
               "🔔 **Liste d'attente:** Si l'hôtel est complet, inscrivez-vous ! Vous serez notifié dès qu'une chambre se libère.\n\n" +
               "💎 **Fidélité:** Chaque réservation vous donne des points pour des réductions futures !";
    }

    private String generateGeneralResponse(String msg) {
        String base = "🤖 Je suis l'assistant hôtelier GoVibe ! Je peux vous aider avec :\n\n" +
                      "• 🏨 Informations sur nos hôtels\n" +
                      "• 💰 Tarifs et disponibilités des chambres\n" +
                      "• 🎯 Programme de fidélité\n" +
                      "• 🏷️ Codes promotionnels\n" +
                      "• 📍 Localisations et équipements\n" +
                      "• 📋 Aide à la réservation\n\n" +
                      "Que puis-je faire pour vous ? 😊";

        if (lastMentionedHotel != null) {
            return "Je n'ai pas bien compris votre question concernant **" + lastMentionedHotel +
                   "**.\n\n" + base;
        }
        return base;
    }

    // ─── Greeting / Help ──────────────────────────────────────────────────────

    private boolean isGreeting(String msg) {
        return msg.matches(".*(bonjour|bonsoir|salut|hello|hi|salam|yā|مرحبا|coucou).*");
    }

    private boolean isGoodbye(String msg) {
        return msg.matches(".*(au revoir|bye|merci|goodbye|à bientôt|bonne journée|bonne nuit).*");
    }

    private boolean isHelp(String msg) {
        return msg.matches(".*(aide|help|que peux.tu|comment|quoi faire|fonctionnalité).*");
    }

    private String generateGreeting() {
        String[] greetings = {
            "Bonjour ! 👋 Bienvenue chez GoVibe Hotels ! Comment puis-je vous aider ?",
            "Bonsoir ! 🌙 Je suis votre assistant hôtelier GoVibe. Que recherchez-vous ?",
            "Salut ! 🏨 Je suis ici pour vous aider à trouver le séjour parfait ! Quelle est votre question ?",
        };
        return greetings[new Random().nextInt(greetings.length)] + "\n\n" +
               "Je peux vous aider avec les **hôtels**, **chambres**, **prix**, **codes promo** et plus encore ! 😊";
    }

    private String generateHelp() {
        return "🤖 **GoVibe Hotel Assistant — Ce que je sais faire:**\n\n" +
               "🏨 *Hôtels*: 'Liste tous les hôtels', 'Tell me about Hotel XYZ'\n" +
               "🛏️ *Chambres*: 'Types de chambres disponibles', 'chambres pour 2 personnes'\n" +
               "💰 *Prix*: 'Quels sont les tarifs?', 'Hôtel le moins cher'\n" +
               "📍 *Localisation*: 'Hôtels à Tunis', 'Où est situé tel hôtel'\n" +
               "✨ *Équipements*: 'Hôtels avec piscine', 'chambres avec spa'\n" +
               "🎯 *Fidélité*: 'Programme de points', 'Statut Silver Gold Platinum'\n" +
               "🏷️ *Promos*: 'Codes de réduction', 'HOTEL10'\n" +
               "📋 *Réservation*: 'Comment réserver', 'Liste d'attente'\n" +
               "⭐ *Recommandations*: 'Meilleur hôtel', 'Top hôtel luxe'\n\n" +
               "💬 Posez vos questions naturellement !";
    }
}
