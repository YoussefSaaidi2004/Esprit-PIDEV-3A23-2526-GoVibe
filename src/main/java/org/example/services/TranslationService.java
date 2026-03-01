package org.example.services;

import java.util.HashMap;
import java.util.Map;

/**
 * 🌍 Service Traduction — GoVibe Hotel Module
 * Lightweight built-in translation (FR / EN / AR) for hotel descriptions.
 * Uses a term dictionary — no external API key required.
 * Supports: French (FR), English (EN), Arabic (AR)
 */
public class TranslationService {

    public enum Language { FR, EN, AR }

    /** Built-in translation dictionary (FR → EN, FR → AR) for hotel terminology */
    private static final Map<String, String[]> DICTIONARY = new HashMap<>();

    static {
        // Format: FR_term → [EN_term, AR_term]
        // Room types
        DICTIONARY.put("chambre simple",    new String[]{"single room",    "غرفة فردية"});
        DICTIONARY.put("chambre double",    new String[]{"double room",    "غرفة مزدوجة"});
        DICTIONARY.put("chambre triple",    new String[]{"triple room",    "غرفة ثلاثية"});
        DICTIONARY.put("suite",             new String[]{"suite",           "جناح"});
        DICTIONARY.put("suite présidentielle", new String[]{"presidential suite", "جناح رئاسي"});
        DICTIONARY.put("studio",            new String[]{"studio",          "ستوديو"});
        // Amenities
        DICTIONARY.put("piscine",          new String[]{"swimming pool",   "مسبح"});
        DICTIONARY.put("spa",              new String[]{"spa",             "سبا"});
        DICTIONARY.put("restaurant",       new String[]{"restaurant",      "مطعم"});
        DICTIONARY.put("wifi",             new String[]{"wifi",            "واي فاي"});
        DICTIONARY.put("climatisation",    new String[]{"air conditioning", "تكييف"});
        DICTIONARY.put("parking",          new String[]{"parking",         "موقف سيارات"});
        DICTIONARY.put("petit-déjeuner",   new String[]{"breakfast",       "إفطار"});
        DICTIONARY.put("demi-pension",     new String[]{"half board",      "إقامة نصفية"});
        DICTIONARY.put("pension complète", new String[]{"full board",      "إقامة كاملة"});
        DICTIONARY.put("jacuzzi",          new String[]{"jacuzzi",         "جاكوزي"});
        DICTIONARY.put("terrasse",         new String[]{"terrace",         "تراس"});
        DICTIONARY.put("vue mer",          new String[]{"sea view",        "إطلالة بحرية"});
        DICTIONARY.put("vue montagne",     new String[]{"mountain view",   "إطلالة جبلية"});
        DICTIONARY.put("salle de sport",   new String[]{"gym",             "صالة رياضية"});
        DICTIONARY.put("sauna",            new String[]{"sauna",           "ساونا"});
        // General hotel terms
        DICTIONARY.put("hôtel",            new String[]{"hotel",           "فندق"});
        DICTIONARY.put("hotel",            new String[]{"hotel",           "فندق"});
        DICTIONARY.put("chambre",          new String[]{"room",            "غرفة"});
        DICTIONARY.put("réservation",      new String[]{"reservation",     "حجز"});
        DICTIONARY.put("disponible",       new String[]{"available",       "متاح"});
        DICTIONARY.put("luxe",             new String[]{"luxury",          "فاخر"});
        DICTIONARY.put("confort",          new String[]{"comfort",         "راحة"});
        DICTIONARY.put("élégant",          new String[]{"elegant",         "أنيق"});
        DICTIONARY.put("moderne",          new String[]{"modern",          "عصري"});
        DICTIONARY.put("traditionnel",     new String[]{"traditional",     "تقليدي"});
        DICTIONARY.put("bienvenue",        new String[]{"welcome",         "أهلاً وسهلاً"});
        DICTIONARY.put("excellent",        new String[]{"excellent",       "ممتاز"});
        DICTIONARY.put("magnifique",       new String[]{"magnificent",     "رائع"});
        DICTIONARY.put("idéal",            new String[]{"ideal",           "مثالي"});
        DICTIONARY.put("situé",            new String[]{"located",         "يقع"});
        DICTIONARY.put("centre-ville",     new String[]{"city center",     "وسط المدينة"});
        DICTIONARY.put("bord de mer",      new String[]{"seaside",         "على شاطئ البحر"});
        DICTIONARY.put("étoiles",          new String[]{"stars",           "نجوم"});
        DICTIONARY.put("nuit",             new String[]{"night",           "ليلة"});
        DICTIONARY.put("prix",             new String[]{"price",           "سعر"});
        DICTIONARY.put("offre",            new String[]{"offer",           "عرض"});
        DICTIONARY.put("promotion",        new String[]{"promotion",       "ترويج"});
    }

    /**
     * Translate a text from French to the target language.
     * Uses dictionary-based term replacement (best effort).
     */
    public String translate(String text, Language targetLang) {
        if (text == null || text.isBlank()) return text;
        if (targetLang == Language.FR) return text;

        String result = text.toLowerCase();
        int langIndex = (targetLang == Language.EN) ? 0 : 1;

        // Replace each known term
        for (Map.Entry<String, String[]> entry : DICTIONARY.entrySet()) {
            String fr = entry.getKey();
            String translated = entry.getValue()[langIndex];
            result = result.replace(fr, translated);
        }

        // Restore original case (simple approach: capitalize first letter)
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        if (targetLang == Language.AR) {
            // Wrap in RTL direction hint
            return "\u202B" + result; // RIGHT-TO-LEFT EMBEDDING character
        }

        return result;
    }

    /**
     * Get all three translations of a text.
     */
    public Map<Language, String> translateAll(String text) {
        Map<Language, String> results = new HashMap<>();
        results.put(Language.FR, text);
        results.put(Language.EN, translate(text, Language.EN));
        results.put(Language.AR, translate(text, Language.AR));
        return results;
    }

    public String getLanguageEmoji(Language lang) {
        switch (lang) {
            case EN: return "🇬🇧 EN";
            case AR: return "🇸🇦 AR";
            default: return "🇫🇷 FR";
        }
    }
}
