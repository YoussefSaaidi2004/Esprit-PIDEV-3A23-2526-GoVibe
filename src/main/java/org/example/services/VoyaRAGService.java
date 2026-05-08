package org.example.services;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🌍 Voya RAG Fallback — GoVibe Forum Chat Module
 * Pure Java RAG (Retrieval-Augmented Generation) 
 * Used automatically when the Gemini API is unavailable.
 *
 * Architecture:
 *   1. INDEXING  — Static in-memory knowledge base of travel destinations & tips
 *   2. RETRIEVAL — BM25-style keyword matching + category routing
 *   3. INTENT    — Classify question type (greeting, destination, tips, food, budget...)
 *   4. GENERATION— Build a warm, personality-rich answer from retrieved snippets
 */
public class VoyaRAGService {

    // ─── Knowledge Document ───────────────────────────────────────────────────

    private record TravelDoc(
            String region,
            String country,
            String[] keywords,
            String overview,
            String bestTime,
            String highlights,
            String food,
            String budget,
            String tips
    ) {}

    // ─── Static Knowledge Base ────────────────────────────────────────────────

    private static final List<TravelDoc> KB = List.of(

        // ── EUROPE ────────────────────────────────────────────────────────────

        new TravelDoc("Europe", "France",
            new String[]{"france", "paris", "eiffel", "provence", "marseille", "lyon", "nice", "versailles", "louvre", "bordeaux"},
            "La France est la destination touristique numéro 1 mondiale, mêlant art, gastronomie exceptionnelle et paysages diversifiés allant des Alpes aux plages méditerranéennes.",
            "Printemps (avril-juin) et automne (sept-oct) sont idéaux. Évitez août à Paris (très touristique). Noël à Strasbourg est magique.",
            "Tour Eiffel, Musée du Louvre, Château de Versailles, Côte d'Azur, vignobles de Bordeaux, Mont-Saint-Michel, Gorges du Verdon, Châteaux de la Loire.",
            "Croissants, coq au vin, ratatouille, bœuf bourguignon, plus de 400 fromages, macarons, crêpes bretonnes, bouillabaisse marseillaise, foie gras, escargots.",
            "Moyen à élevé. Hôtel budget ~50-80€/nuit, restaurant ~15-30€/repas. Paris est 30% plus cher que la province.",
            "⚡ Achetez les billets d'entrée en ligne. Carte Navigo pour transports parisiens. Musées gratuits le 1er dimanche du mois. Évitez les restos autour de la Tour Eiffel."
        ),

        new TravelDoc("Europe", "Italie",
            new String[]{"italie", "rome", "venise", "florence", "milan", "sicile", "naples", "toscane", "colisée", "vatican"},
            "L'Italie est le berceau de la Renaissance, de la gastronomie mondiale et possède le plus grand nombre de sites UNESCO.",
            "Avril-juin et septembre-octobre. L'été est caniculaire à Rome et Naples. Venise en hiver est magique et peu fréquentée.",
            "Colisée, Vatican, Galerie des Offices à Florence, Canal Grande de Venise, Côte Amalfitaine, Dolomites, Pompéi.",
            "Pizza napolitaine, pasta carbonara/cacio e pepe, risotto milanese, gelato artisanal, tiramisu, bistecca fiorentina.",
            "Moyen. Hostel ~25-40€, hôtel ~70-130€. Rome coûte plus cher que le sud. Train Trenitalia abordable entre villes.",
            "⚡ Réservez le Vatican et la Chapelle Sixtine 2 semaines à l'avance. Le café au bar coûte 1€, assis 3-5€ — grand différence!"
        ),

        new TravelDoc("Europe", "Espagne",
            new String[]{"espagne", "barcelone", "madrid", "séville", "grenade", "ibiza", "majorque", "gaudi", "sagrada"},
            "L'Espagne allie histoire moorish, architecture moderniste et fêtes vibrantes dans un pays de contrastes saisissants.",
            "Mars-mai et septembre-novembre. L'été est intense (40°C à Séville). Les Canaries sont parfaites en hiver.",
            "Sagrada Família, Alhambra de Grenade, Musée du Prado, Parc Güell, La Rambla, Flamenco à Séville, Camino de Santiago.",
            "Tapas (patatas bravas, jamón ibérico), paella valenciana, gazpacho andalou, churros, sangria, tortilla española.",
            "Abordable. Menu del día ~10-15€. Hôtel ~50-90€. Le pintxo à San Sebastián est un rituel incontournable!",
            "⚡ Les horaires espagnols décalent tout: dîner à 21h-23h, sieste 14h-17h. Réservez l'Alhambra des semaines à l'avance!"
        ),

        new TravelDoc("Afrique", "Maroc",
            new String[]{"maroc", "marrakech", "fès", "casablanca", "sahara", "atlas", "essaouira", "chefchaouen", "medina"},
            "Le Maroc est une immersion totale dans un monde de couleurs, d'épices et d'hospitalité légendaire aux portes de l'Europe.",
            "Mars-mai et octobre-novembre. Évitez juillet-août (40°C dans les villes), idéal pour le désert en novembre-février.",
            "Majorelle à Marrakech, Médina de Fès, désert de Merzouga, Chefchaouen la ville bleue, Cascades d'Ouzoud, Vallée du Draa.",
            "Tajine, couscous du vendredi, pastilla au pigeon, harira soup, bstilla, mint tea, msemen, amlou (beurre d'argan et miel).",
            "Très abordable. Riad ~40-80€/nuit. Repas au souk ~5-10€. Marchandage obligatoire dans les souks (commencez à 1/3 du prix).",
            "⚡ Négociez TOUT dans les souks. Emportez du cash — peu de CB acceptées. Un guide local enrichit enormément la visite de Fès."
        ),

        new TravelDoc("Afrique", "Tunisie",
            new String[]{"tunisie", "tunis", "carthage", "djerba", "sousse", "monastir", "sahara tunisien", "sidi bou said"},
            "La Tunisie concentre méditerranée cristalline, sites antiques puniques et portes sahariennes dans un pays chaleureux.",
            "Avril-juin et septembre-novembre. Été balnéaire parfait. Décembre pour le désert du Sud.",
            "Sidi Bou Saïd, Carthage, Amphithéâtre d'El Jem, Djerba, Matmata (décors de Star Wars), Tozeur et ses oasis.",
            "Brik à l'œuf, couscous au poisson, lablabi, chakchouka, merguez, fricassé, makroudh, citronnade à la menthe.",
            "Très abordable. Hôtel ~30-60€. Repas ~4-8€. Dinar tunisien avantageux pour les visiteurs européens.",
            "⚡ Le tramway de Tunis est pratique et bon marché. Negociez au souk. La SNCFT relie les grandes villes à petit prix."
        ),

        new TravelDoc("Asie", "Japon",
            new String[]{"japon", "tokyo", "kyoto", "osaka", "fuji", "nara", "hiroshima", "shibuya", "geisha", "temple"},
            "Le Japon fascine par son équilibre parfait entre tradition millénaire et hypermodernité dans une société d'une politesse rare.",
            "Mars-avril (cerisiers) et novembre (érables). Évitez Golden Week (fin avril-début mai) et Obon (mi-août).",
            "Mont Fuji, temples de Kyoto (Fushimi Inari, Kinkaku-ji), Shibuya Crossing, Cerfs de Nara, Hiroshima, Île de Miyajima.",
            "Sushi, ramen, tempura, takoyaki, yakitori, tonkatsu, okonomiyaki, matcha, mochi, wagyu beef.",
            "Élevé. ~100€/nuit en capsule/hostel, ~150-300€ hôtel. Japan Rail Pass (21 jours ~550€) très rentable pour voyager.",
            "⚡ Achetez le JR Pass AVANT d'atterrir — il n'est plus vendu sur place. IC Card rechargeable pour les transports locaux."
        ),

        new TravelDoc("Asie", "Thaïlande",
            new String[]{"thaïlande", "bangkok", "phuket", "chiang mai", "koh", "temple", "bouddha", "muay thai", "elephant"},
            "La Thaïlande est le sourire de l'Asie: temples dorés, plages turquoise, nourriture explosive et budget ultra-accessible.",
            "Novembre-février (saison sèche). Évitez juin-octobre (mousson). Chiang Mai est superbe en novembre.",
            "Grand Palais de Bangkok, Temples Doi Suthep à Chiang Mai, Îles Phi Phi, Sanctuary of Truth, marché flottant.",
            "Pad thai, green curry, tom yum soup, som tum (salade papaye), mango sticky rice, khao pad, massaman curry.",
            "Très abordable. Hostel ~8-15€, hôtel ~25-60€. Repas de rue ~1-3€. Tuk-tuk ~2-5€. Budget total ~40-80€/jour.",
            "⚡ Couvrez épaules et genoux pour entrer dans les temples. Tuk-tuk: négociez AVANT de monter. SIM tourist en aéroport = indispensable."
        ),

        new TravelDoc("Amérique", "États-Unis",
            new String[]{"usa", "états-unis", "new york", "los angeles", "miami", "chicago", "las vegas", "yellowstone", "grand canyon"},
            "Les États-Unis offrent une diversité phénoménale: mégapoles iconiques, parcs nationaux grandioses et Routes légendaires.",
            "Toute l'année selon région. New York: printemps/automne. Floride: octobre-avril. Parcs Ouest: mai-septembre.",
            "Grand Canyon, Yellowstone, Times Square, Route 66, Golden Gate, Monument Valley, Disney World, Niagara Falls.",
            "Burger, BBQ texan, lobster roll, clam chowder, New York pizza, cheesecake, gumbo louisianais, tacos californiens.",
            "Élevé. Hôtel ~80-200€. Restaurant ~15-40€. Voiture indispensable hors des grandes villes. Pourboire 15-20% obligatoire.",
            "⚡ ESTA obligatoire avant vol (~20€, valable 2 ans). Cuisinez parfois — portions énormes, pratique de partager les plats!"
        ),

        new TravelDoc("Amérique", "Mexique",
            new String[]{"mexique", "cancun", "mexico", "oaxaca", "yucatan", "tulum", "chichen itza", "pyramide", "mariachi"},
            "Le Mexique est une civilisation vivante: ruines mayas majestueuses, plages des Caraïbes et gastronomie classée UNESCO.",
            "Décembre-avril (saison sèche). Cancun parfait l'hiver. Intérieur des terres agréable toute l'année.",
            "Chichén Itzá, Cenotes du Yucatán, Teotihuacan, Oaxaca colonial, Plages de Tulum, Musée National d'Anthropologie.",
            "Tacos al pastor, mole negro d'Oaxaca, chiles en nogada, tamales, guacamole, elote, aguas frescas, mezcal.",
            "Abordable. Hostel ~10-20€, hôtel ~35-80€. Repas de rue ~2-5€. Très bon rapport qualité-prix hors zones tourist.",
            "⚡ Ne buvez PAS l'eau du robinet. Peso mexicain préféré au dollar. Les cenotes valent absolument le détour!"
        ),

        new TravelDoc("Océanie", "Australie",
            new String[]{"australie", "sydney", "melbourne", "uluru", "great barrier reef", "cairns", "kangaroo", "koala", "outback"},
            "L'Australie est un continent-pays d'une biodiversité unique avec des paysages qui vont de la jungle tropicale à l'Outback rouge.",
            "Septembre-novembre (printemps austral). Évitez le Nord en été (40°C + cyclones). Grande Barrière en juin-octobre.",
            "Grande Barrière de Corail, Uluru (Ayers Rock), Opéra de Sydney, Great Ocean Road, Kakadu National Park, Bondi Beach.",
            "Barramundi, Tim Tams, meat pie, Vegemite toast, pavlova, lamingtons, fish & chips, BBQ australien.",
            "Très élevé. Hôtel ~90-180€. Repas ~15-35€. Working Holiday Visa possible pour moins de 35 ans.",
            "⚡ Distances énormes — prévoyez vols intérieurs. Conduite à gauche. Soleil + UV extrêmes: crème 50+ obligatoire!"
        ),

        new TravelDoc("Europe", "Grèce",
            new String[]{"grèce", "athènes", "santorin", "mykonos", "acropole", "crète", "rhodes", "méditerranée", "olympe"},
            "La Grèce est le berceau de la civilisation occidentale, baigné par une mer cristalline et parsemé d'îles paradisiaques.",
            "Mai-juin et septembre-octobre. Évitez juillet-août (foules + 38°C). Les îles sont désertes hors saison — charme authentique.",
            "Acropole et Parthénon, Santorin (coucher soleil à Oia), Meteora, Delphes, Palais de Knossos, Temple de Poséidon.",
            "Moussaka, souvlaki, tzatziki, spanakopita, grillades sur braises, feta AOP, baklava, ouzo, frappé grec.",
            "Moyen. Hôtel ~50-120€. Repas taverne ~10-20€. Ferries inter-îles très abordables. Athènes moins chère que les îles.",
            "⚡ Réservez les ferries à l'avance en juillet-août. Visitez l'Acropole tôt le matin (ouverture 8h) avant la chaleur et les groupes."
        ),

        new TravelDoc("Moyen-Orient", "Émirats Arabes Unis",
            new String[]{"dubai", "abu dhabi", "émirats", "burj khalifa", "desert safari", "gold souk", "louvre abu dhabi"},
            "Dubaï et Abu Dhabi représentent le futurisme absolu: gratte-ciels records, luxe extrême et désert à 30 minutes du centre.",
            "Octobre-avril (25-30°C). Été insupportable (45°C+ avec humidité). Décembre-janvier saison haute.",
            "Burj Khalifa (828m), Palm Jumeirah, Musée du Futur, Louvre Abu Dhabi, Safari désert, Gold Souk, Abra (bateau traditionnel).",
            "Shawarma, hummus, manousheh, baba ghanoush, luqaimat (beignets au miel), karak chai, dates Medjool.",
            "Élevé à très élevé. Hôtel luxe ~150-500€. Malls avec food courts abordables (~8-15€). Alcool uniquement en hôtels/bars licenciés.",
            "⚡ Code vestimentaire strict dans les lieux publics et mosquées. Photos de personnes: demandez permission. Taxi Uber très pratique."
        ),

        new TravelDoc("Asie", "Indonésie",
            new String[]{"indonésie", "bali", "jakarta", "java", "borobudur", "komodo", "lombok", "ubud", "temple"},
            "L'Indonésie, l'archipel de 17 000 îles, offre temples hindous mythiques, rizières en terrasses et fonds marins parmi les plus riches.",
            "Avril-octobre (saison sèche). Bali est magnifique toute l'année. Komodo: avril-août.",
            "Temple de Borobudur, Rizières de Tegalalang à Ubud, Dragons de Komodo, Gili Islands, Volcan Bromo, Tanah Lot.",
            "Nasi goreng, mie goreng, satay, rendang, gado-gado, tempeh, babi guling (Bali), pisang goreng, es cendol.",
            "Très abordable. Hostel ~8-15€, villa Bali ~30-80€. Repas ~2-5€. Moteur scooter loué ~5€/jour = liberté totale.",
            "⚡ VoA (visa à l'arrivée) 30 jours ~35€. Marchandez au marché Ubud. Respectez les cérémonies hindoues — s'habiller sobrement."
        )
    );

    // ─── Conversation Memory ───────────────────────────────────────────────────

    private final Deque<String[]> history = new ArrayDeque<>();
    private String lastDestination = null;

    // ─── Public API ────────────────────────────────────────────────────────────

    public String chat(String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            return "❓ Posez-moi une question sur votre prochaine destination ! 🌍";

        String msg = userMessage.toLowerCase().trim();
        history.addLast(new String[]{"user", userMessage});

        String response = route(msg, userMessage);

        history.addLast(new String[]{"assistant", response});
        if (history.size() > 20) history.pollFirst();

        return response;
    }

    // ─── Intent Routing ───────────────────────────────────────────────────────

    private String route(String msg, String original) {
        // Greetings
        if (matches(msg, "bonjour", "salut", "hello", "coucou", "bonsoir", "allo", "hi")) {
            return greeting();
        }

        // Help / capabilities
        if (matches(msg, "aide", "help", "que peux-tu", "que sais-tu", "comment", "qui es-tu", "qui es tu")) {
            return capabilities();
        }

        // Budget questions
        if (matches(msg, "budget", "prix", "coût", "coute", "cher", "pas cher", "économique", "économiser", "cheap")) {
            return budgetAnswer(msg);
        }

        // Food questions
        if (matches(msg, "manger", "cuisine", "nourriture", "gastronomie", "plat", "food", "restaurant", "spécialité")) {
            return foodAnswer(msg);
        }

        // Best time to visit
        if (matches(msg, "quand partir", "quelle période", "meilleur moment", "saison", "météo", "climat", "quand visiter")) {
            return bestTimeAnswer(msg);
        }

        // Recommendations
        if (matches(msg, "recommande", "suggère", "conseille", "où aller", "idée", "destination", "voyage")) {
            return recommendAnswer(msg);
        }

        // Destination-specific lookup
        List<TravelDoc> hits = retrieve(msg);
        if (!hits.isEmpty()) {
            lastDestination = hits.get(0).country();
            return buildAnswer(hits.get(0), msg);
        }

        // Fallback to last destination context
        if (lastDestination != null) {
            List<TravelDoc> ctx = retrieve(lastDestination.toLowerCase());
            if (!ctx.isEmpty()) return contextualAnswer(ctx.get(0), original);
        }

        return noMatch(original);
    }

    // ─── Retrieval ────────────────────────────────────────────────────────────

    private List<TravelDoc> retrieve(String query) {
        record Scored(TravelDoc doc, int score) {}
        List<Scored> scored = new ArrayList<>();

        for (TravelDoc doc : KB) {
            int score = 0;
            for (String kw : doc.keywords()) {
                if (query.contains(kw)) score += 3;
            }
            if (query.contains(doc.country().toLowerCase())) score += 5;
            if (query.contains(doc.region().toLowerCase()))  score += 1;
            if (score > 0) scored.add(new Scored(doc, score));
        }

        scored.sort((a, b) -> b.score() - a.score());
        return scored.stream().map(Scored::doc).collect(Collectors.toList());
    }

    // ─── Answer Builders ──────────────────────────────────────────────────────

    private String buildAnswer(TravelDoc doc, String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("🌍 **").append(doc.country()).append("** — ").append(doc.region()).append("\n\n");
        sb.append(doc.overview()).append("\n\n");

        if (matches(query, "voir", "visiter", "découvrir", "attraction", "monument", "incontournable", "highlight")) {
            sb.append("✨ **Incontournables :** ").append(doc.highlights()).append("\n\n");
        }
        if (matches(query, "manger", "cuisine", "food", "gastronomie", "plat", "restaurant")) {
            sb.append("🍽️ **Gastronomie :** ").append(doc.food()).append("\n\n");
        }
        if (matches(query, "budget", "prix", "coût", "cher", "argent")) {
            sb.append("💰 **Budget :** ").append(doc.budget()).append("\n\n");
        }
        if (matches(query, "quand", "saison", "période", "météo", "climat")) {
            sb.append("📅 **Meilleure période :** ").append(doc.bestTime()).append("\n\n");
        }

        // Always show highlights if nothing specific matched
        if (!sb.toString().contains("Incontournables")) {
            sb.append("✨ **À voir absolument :** ").append(doc.highlights()).append("\n\n");
        }

        sb.append(doc.tips()).append("\n\n");
        sb.append(nextQuestion(doc));

        return sb.toString().trim();
    }

    private String contextualAnswer(TravelDoc doc, String question) {
        return "🗺️ En parlant de la **" + doc.country() + "**, voici ce qui pourrait vous aider :\n\n"
                + doc.overview() + "\n\n"
                + "✨ " + doc.highlights() + "\n\n"
                + doc.tips() + "\n\n"
                + nextQuestion(doc);
    }

    private String greeting() {
        String[] greetings = {
            "Bonjour ! Je suis Voya, votre guide de voyage passionné ! 🌍✈️\n\nJe connais de nombreuses destinations dans le monde. Dites-moi simplement le nom d'un pays ou d'une ville, et je vous dévoile ses secrets ! Quelle destination vous fait rêver ?",
            "Salut ! Prêt pour une nouvelle aventure ? 🗺️\n\nJe suis Voya — demandez-moi tout sur Paris, Tokyo, Marrakech, Bali... Je suis votre encyclopédie du voyage ! Où voulez-vous aller ?",
            "Bienvenue ! Je suis ravi de vous guider vers vos prochaines aventures ! 😊\n\nMon savoir couvre l'Europe, l'Asie, l'Afrique, les Amériques et l'Océanie. Dites-moi une destination !"
        };
        return greetings[new Random().nextInt(greetings.length)];
    }

    private String capabilities() {
        return "Je suis **Voya**, votre guide de voyage IA ! 🌍\n\n" +
               "Voici ce que je peux faire :\n" +
               "• 📍 Incontournables par destination (France, Japon, Maroc, Thaïlande...)\n" +
               "• 🍽️ Gastronomie locale et spécialités\n" +
               "• 📅 Meilleure période pour voyager\n" +
               "• 💰 Conseils budget et coûts estimés\n" +
               "• ⚡ Astuces pratiques de voyageur expérimenté\n" +
               "• 🗺️ Recommandations personnalisées\n\n" +
               "Demandez-moi simplement : *\"Parle-moi de l'Italie\"* ou *\"Quand partir au Japon ?\"* 😊\n\nQuelle est votre prochaine destination rêvée ?";
    }

    private String budgetAnswer(String query) {
        List<TravelDoc> hits = retrieve(query);
        if (!hits.isEmpty()) {
            TravelDoc doc = hits.get(0);
            return "💰 **Budget pour " + doc.country() + "**\n\n" +
                   doc.budget() + "\n\n" + doc.tips() + "\n\nVoulez-vous des infos sur les incontournables ou la gastronomie locale ?";
        }
        return "💰 En termes de budget voyage, tout dépend de la destination !\n\n" +
               "🟢 **Abordables :** Maroc, Tunisie, Thaïlande, Indonésie, Mexique (~40-80€/jour)\n" +
               "🟡 **Moyens :** Espagne, Grèce, Italie, France (~80-150€/jour)\n" +
               "🔴 **Élevés :** Japon, Australie, États-Unis, Émirats (~150-300€/jour)\n\n" +
               "Quelle destination vous intéresse ? Je vous donnerai un budget détaillé !";
    }

    private String foodAnswer(String query) {
        List<TravelDoc> hits = retrieve(query);
        if (!hits.isEmpty()) {
            TravelDoc doc = hits.get(0);
            return "🍽️ **Gastronomie de " + doc.country() + "**\n\n" + doc.food() + "\n\n" +
                   "💡 " + doc.tips() + "\n\nVoulez-vous savoir quand partir ou quoi voir sur place ?";
        }
        return "🍽️ Chaque destination a sa cuisine unique ! Voici quelques joyaux gastronomiques :\n\n" +
               "🇫🇷 **France** — Croissants, fromages, coq au vin\n" +
               "🇯🇵 **Japon** — Sushi, ramen, tempura\n" +
               "🇲🇦 **Maroc** — Tajine, couscous, pastilla\n" +
               "🇹🇭 **Thaïlande** — Pad thaï, curry vert, mango sticky rice\n" +
               "🇮🇹 **Italie** — Pizza napolitaine, carbonara, gelato\n\n" +
               "Dites-moi votre destination et je vous guide vers les meilleures saveurs ! 😋";
    }

    private String bestTimeAnswer(String query) {
        List<TravelDoc> hits = retrieve(query);
        if (!hits.isEmpty()) {
            TravelDoc doc = hits.get(0);
            return "📅 **Meilleure période pour " + doc.country() + "**\n\n" + doc.bestTime() + "\n\n" +
                   doc.tips() + "\n\nVoulez-vous découvrir les incontournables ou le budget estimé ?";
        }
        return "📅 La période idéale varie selon la destination !\n\n" +
               "🌸 **Printemps (mars-mai)** — Japon (cerisiers), Europe méridionale\n" +
               "☀️ **Été (juin-août)** — Scandinavie, Canada, parcs américains\n" +
               "🍂 **Automne (sept-nov)** — Asie du Sud-Est, Maroc, Méditerranée\n" +
               "❄️ **Hiver (déc-fév)** — Émirats, Thaïlande, Australie, Antilles\n\n" +
               "Dites-moi votre destination et je vous indiquerai la fenêtre parfaite ! ✈️";
    }

    private String recommendAnswer(String query) {
        boolean beach   = matches(query, "plage", "mer", "beach", "island", "île", "soleil");
        boolean culture = matches(query, "culture", "histoire", "musée", "temple", "ancien", "patrimoine");
        boolean budget  = matches(query, "pas cher", "économique", "budget", "cheap", "abordable");
        boolean luxury  = matches(query, "luxe", "luxury", "premium", "5 étoiles", "resort");
        boolean nature  = matches(query, "nature", "montagne", "forêt", "safari", "wildlife", "parc");

        if (beach)
            return "🏖️ **Pour les amoureux des plages :**\n\n" +
                   "🌴 **Thaïlande** — Koh Phi Phi, plages de sable blanc cristallin\n" +
                   "🌊 **Grèce** — Santorin, Mýkonos, eaux azurées de la Méditerranée\n" +
                   "🏝️ **Indonésie (Bali)** — Plages de rêve + temples + rizières\n" +
                   "🌅 **Maroc** — Essaouira, Agadir, ambiance unique\n\n" +
                   "Laquelle vous inspire ? Je vous donne tous les détails ! 😊";
        if (culture)
            return "🏛️ **Pour les passionnés de culture et d'histoire :**\n\n" +
                   "🗼 **France** — Louvre, Versailles, Normandie, histoire vivante\n" +
                   "🏺 **Italie** — Rome, Florence, Pompéi, berceau de la Renaissance\n" +
                   "⛩️ **Japon** — Kyoto, temples zen, arts martiaux, ikebana\n" +
                   "🕌 **Maroc** — Médinas de Fès et Marrakech, architecture islamique\n\n" +
                   "Une destination vous parle ? Je vous guide ! 🗺️";
        if (budget)
            return "💸 **Top destinations budget (< 60€/jour) :**\n\n" +
                   "🥇 **Indonésie** — Bali magnifique pour une bouchée de pain\n" +
                   "🥈 **Maroc** — Dépaysement total, prix très doux\n" +
                   "🥉 **Thaïlande** — L'Asie à petit prix avec grand confort\n" +
                   "🏅 **Mexique** — Cenotes et plages à moindres frais\n" +
                   "🏅 **Tunisie** — La Méditerranée à prix local\n\n" +
                   "Voulez-vous des détails sur l'une d'elles ? ✈️";
        if (luxury)
            return "💎 **Destinations de luxe inoubliables :**\n\n" +
                   "✨ **Émirats Arabes Unis** — Hôtels records, expériences uniques\n" +
                   "🇫🇷 **Côte d'Azur** — Saint-Tropez, Monaco, French Riviera\n" +
                   "🇬🇷 **Santorin** — Villas à débordement, couchers de soleil légendaires\n" +
                   "🇯🇵 **Japon** — Ryokan traditionnel, kaiseki, expérience parfaite\n\n" +
                   "Budget et expérience ? Je personnalise votre itinéraire ! 💼";
        if (nature)
            return "🌿 **Pour les amoureux de la nature :**\n\n" +
                   "🦁 **Kenya / Afrique** — Safari, Big Five, nature sauvage\n" +
                   "🌋 **Indonésie** — Volcans, jungle, orangs-outans de Bornéo\n" +
                   "🏔️ **Suisse / Autriche** — Alpes majestueuses, lacs cristallins\n" +
                   "🦘 **Australie** — Grande Barrière, Outback rougeoyant, koalas\n\n" +
                   "Quelle expérience nature vous attire ? 🌏";

        return "✈️ **Inspirations pour votre prochain voyage :**\n\n" +
               "🏖️ Plages paradisiaques → Bali, Thaïlande, Grèce\n" +
               "🏛️ Culture & histoire → France, Italie, Japon, Maroc\n" +
               "💸 Petit budget → Tunisie, Mexique, Indonésie\n" +
               "💎 Luxe & prestige → Émirats, Maldives, Côte d'Azur\n" +
               "🌿 Nature sauvage → Australie, Kenya, Islande\n\n" +
               "Dites-moi vos préférences (budget, type de voyage, durée) et je vous trouve la destination parfaite ! 🌍";
    }

    private String noMatch(String original) {
        return "🗺️ Je n'ai pas trouvé d'informations précises sur *\"" + original + "\"*.\n\n" +
               "Je connais bien ces destinations :\n" +
               "🇫🇷 France • 🇮🇹 Italie • 🇪🇸 Espagne • 🇬🇷 Grèce\n" +
               "🇲🇦 Maroc • 🇹🇳 Tunisie • 🇯🇵 Japon • 🇹🇭 Thaïlande\n" +
               "🇺🇸 États-Unis • 🇲🇽 Mexique • 🇦🇺 Australie • 🇮🇩 Indonésie • 🇦🇪 Émirats\n\n" +
               "Demandez-moi **\"Parle-moi du Japon\"** ou **\"Conseils pour la Thaïlande\"** — je suis là ! 😊";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String nextQuestion(TravelDoc doc) {
        String[] questions = {
            "Quelle durée de séjour envisagez-vous pour " + doc.country() + " ? 📅",
            "Voyagez-vous seul(e), en couple ou en famille vers " + doc.country() + " ? 👨‍👩‍👧",
            "Y a-t-il un aspect particulier de " + doc.country() + " qui vous attire le plus ? 🌟",
            "Souhaitez-vous un itinéraire détaillé pour " + doc.country() + " ? 🗺️"
        };
        return "*" + questions[new Random().nextInt(questions.length)] + "*";
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
