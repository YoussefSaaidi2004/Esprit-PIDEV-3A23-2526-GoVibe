#!/usr/bin/env python3
"""
GoVibe Voice Agent — Three-tier intent classifier + Ollama LLM.

Classification pipeline (fastest to most powerful):
  Tier 1 — sentence-transformers (all-MiniLM-L6-v2)  : ~30 ms, high accuracy
  Tier 2 — Ollama local LLM (llama3.2 / qwen2.5)     : ~400 ms, understands
             natural language, extracts destination/date params — only called
             when tier-1 confidence < threshold
  Tier 3 — scikit-learn TF-IDF                        : zero-download fallback

Protocol (line-delimited JSON over stdin / stdout):
  Java → Python   stdin  :  {"text": "recognised user speech"}
  Python → Java   stdout :  {
                               "intent":"BOOK",
                               "response":"...",
                               "action":"BOOK",
                               "confidence":0.95,
                               "destination":"Paris",   ← new (may be null)
                               "date":"2026-03-15",     ← new (may be null)
                               "engine":"sentence-transformers"  ← tier used
                             }

On startup the agent writes ONE ready-line to stdout:
  {"status":"ready","engine":"sentence-transformers+ollama"}
"""

import sys
import json
import os
import unicodedata
import urllib.request
import urllib.error

# Force UTF-8 on all platforms (critical on Windows where stdout defaults to cp1252).
if sys.stdout.encoding != "utf-8":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", line_buffering=True)
if sys.stderr.encoding != "utf-8":
    import io
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", line_buffering=True)


# ─────────────────────────────────────────────────────────────────────────────
# Text normalisation — strips accents and lowercases.
# Used so "réserver" == "reserver" == "RESERVER" in all comparisons.
# This is critical when the English Vosk model (en-us) transcribes French
# speech: it strips accents, so we must normalise both sides before comparing.
# ─────────────────────────────────────────────────────────────────────────────
def _normalize(text: str) -> str:
    """Lowercase + strip all diacritics (é→e, à→a, ç→c, ô→o …)."""
    nfd = unicodedata.normalize("NFD", text.lower())
    return "".join(c for c in nfd if unicodedata.category(c) != "Mn")


# ─────────────────────────────────────────────────────────────────────────────
# Tier-0  — direct accent-insensitive phrase match.
# Runs BEFORE any ML model.  Handles common cases instantly and is robust to
# accent-stripping by English STT engines (Vosk en-us, SAPI en-US).
# ─────────────────────────────────────────────────────────────────────────────
def _direct_match(text: str):
    """
    Returns (intent_key, confidence=1.0) when *text* (after normalisation)
    exactly equals, contains, or is contained in any training phrase.
    Returns ("UNKNOWN", 0.0) when no phrase matches.
    """
    t_norm = _normalize(text.strip())
    if not t_norm:
        return "UNKNOWN", 0.0
    for intent_key, data in INTENTS.items():
        for phrase in data["phrases"]:
            p_norm = _normalize(phrase)
            # Exact match OR the text is a clean sub/super-string of a phrase
            if t_norm == p_norm or t_norm in p_norm or p_norm in t_norm:
                return intent_key, 1.0
    return "UNKNOWN", 0.0

# ─────────────────────────────────────────────────────────────────────────────
# Intent catalogue — rich training phrases (FR + EN).
# Each entry has:
#   phrases : list[str]  — training examples fed to the classifier
#   response: str        — TTS sentence spoken to the user
#   action  : str        — action code forwarded to CommandRouter in Java
# ─────────────────────────────────────────────────────────────────────────────
INTENTS = {

    "BOOK": {
        "phrases": [
            # French
            "réserver", "reserver", "réservation", "reservation",
            "nouvelle réservation", "nouvelle reservation",
            "je veux réserver", "je veux reserver",
            "réserver un vol", "reserver un vol",
            "réserver un billet", "reserver un billet",
            "faire une réservation", "faire une reservation",
            "prendre un billet", "acheter un billet",
            "je voudrais voyager", "planifier un voyage",
            "ouvrir réservation", "ajouter réservation",
            # English
            "book", "book a flight", "book flight", "book ticket",
            "i want to book", "make a reservation", "create booking",
            "new reservation", "reserve", "reserve a seat",
            "reserve a flight", "i'd like to book", "get a ticket",
            "buy a ticket", "fly to", "book a trip",
        ],
        "response": "Ooh la la! Let me find you a flight. Pick one from the list and we'll make it official.",
        "action": "BOOK",
    },
    "VIEW_BOOKINGS": {
        "phrases": [
            # French
            "mes réservations", "mes reservations",
            "voir mes réservations", "voir mes reservations",
            "afficher mes réservations", "afficher réservations",
            "liste des réservations", "mes voyages",
            "voir mes voyages", "historique des réservations",
            "consulter mes réservations", "mes billets",
            # English
            "my bookings", "show my bookings", "view bookings",
            "my trips", "see my reservations", "check my bookings",
            "reservation list", "booking history", "past bookings",
            "show reservations", "list my flights",
        ],
        "response": "Let me pull up all your grand travel plans.",
        "action": "MES RESERVATIONS",
    },

    "SEARCH": {
        "phrases": [
            # French
            "rechercher", "chercher", "trouver un vol", "chercher un vol",
            "rechercher des vols", "afficher les vols", "voir les vols",
            "vols disponibles", "vols", "destinations",
            "liste des vols", "explorer les destinations",
            # English
            "search", "search flights", "find flights", "available flights",
            "show flights", "list flights", "look for flights",
            "flights", "find a flight", "explore destinations",
        ],
        "response": "Scanning the skies for available flights. Won't be a moment.",
        "action": "RECHERCHER",
    },

    "CANCEL": {
        "phrases": [
            # French
            "annuler", "fermer", "revenir", "retour", "non merci",
            "annulation", "arrêter", "stopper",
            "précédent", "precedent", "quitter le formulaire",
            # English
            "cancel", "go back", "close", "back", "previous",
            "exit modal", "dismiss", "never mind", "stop",
        ],
        "response": "Alright, pretend that never happened.",
        "action": "ANNULER",
    },

    "PAY": {
        "phrases": [
            # French
            "payer", "paiement", "confirmer", "valider", "procéder au paiement",
            "proceder au paiement", "valider le paiement", "effectuer le paiement",
            "lancer le paiement", "régler", "passer au paiement",
            "finaliser la réservation",
            # English
            "pay", "payment", "confirm", "checkout", "make payment",
            "pay now", "complete payment", "proceed to payment",
            "validate payment", "finish booking",
        ],
        "response": "Time to make it official. Taking you to payment.",
        "action": "PAYER",
    },

    "HELP": {
        "phrases": [
            # French
            "aide", "aide-moi", "aidez-moi", "commandes", "liste des commandes",
            "que peux-tu faire", "que puis-je dire",
            "quelles commandes", "j'ai besoin d'aide", "comment utiliser",
            "mode d'emploi", "assistant",
            # English
            "help", "help me", "commands", "what commands",
            "what can you do", "how to use", "i need help",
            "list of commands", "assistance", "guide me",
        ],
        "response": (
            "Here's the magic spell list. "
            "On login: Email, Password, Login, Sign up. "
            "In the app: Book, My Bookings, Search, Pay, Cancel, Describe, Logout."
        ),
        "action": "AIDE",
    },

    "DESCRIBE": {
        "phrases": [
            # French
            "décrire", "decrire", "qu'est-ce que c'est",
            "que vois-je", "explique", "dis-moi ce que je vois", "quoi",
            "quel écran", "c'est quoi", "où suis-je",
            # English
            "describe", "what is this", "what am i looking at",
            "what's on screen", "explain", "what", "tell me what's here",
            "where am i", "describe screen",
        ],
        "response": "Let me narrate your surroundings like a nature documentary.",
        "action": "DECRIRE",
    },

    "LOGOUT": {
        "phrases": [
            # French
            "déconnexion", "deconnexion", "se déconnecter", "se deconnecter",
            "déconnecter", "quitter", "au revoir",
            "fermer la session", "mettre fin à la session",
            # English
            "logout", "log out", "sign out", "exit", "bye",
            "goodbye", "quit", "leave", "end session", "disconnect",
        ],
        "response": "Poof \u2014 off you go. Come back when the travel bug bites again!",
        "action": "DECONNEXION",
    },

    "GREET": {
        "phrases": [
            "bonjour", "bonsoir", "salut", "coucou", "hey govibe",
            "bonne nuit", "hello", "hi", "hey", "good morning",
            "good evening", "good night", "greetings",
        ],
        "response": "Hey there! I'm GoVibe \u2014 your slightly sarcastic travel assistant. Say 'help' to see what I can do.",
        "action": "NONE",
    },

    "LOGIN": {
        "phrases": [
            # French
            "connexion", "se connecter", "connecter", "entrer",
            "me connecter", "ouvrir session", "accéder", "acceder",
            "valider connexion", "confirmer connexion", "se loguer",
            # English
            "login", "log in", "sign in", "connect", "authenticate",
            "submit login", "enter credentials",
        ],
        "response": "Let me check if you're you. One moment.",
        "action": "LOGIN",
    },

    "FOCUS_EMAIL": {
        "phrases": [
            # French
            "email", "adresse email", "mon email", "adresse",
            "nom d'utilisateur", "nom utilisateur", "identifiant",
            "saisir email", "entrer email", "champ email",
            # English
            "email field", "username", "user name", "email address",
            "my email", "enter email",
        ],
        "response": "Email field \u2014 your keyboard awaits.",
        "action": "FOCUS_EMAIL",
    },

    "FOCUS_PASSWORD": {
        "phrases": [
            # French
            "mot de passe", "mon mot de passe", "mdp",
            "saisir mot de passe", "entrer mot de passe", "champ mot de passe",
            # English
            "password", "password field", "my password", "enter password", "pass",
        ],
        "response": "Password field. No peeking.",
        "action": "FOCUS_PASSWORD",
    },

    "SIGNUP": {
        "phrases": [
            # French
            "créer un compte", "creer un compte", "s'inscrire", "inscrire",
            "nouveau compte", "créer compte", "creer compte",
            "inscription", "enregistrer", "créer profil",
            # English
            "register", "sign up", "signup", "create account",
            "new account", "create a new account", "join",
        ],
        "response": "Another traveler joins GoVibe. Welcome aboard!",
        "action": "SIGNUP",
    },

    "CHECKOUT": {
        "phrases": [
            # French
            "checkout", "mes paiements", "voir mes paiements",
            "paiements effectues", "mes achats", "mes commandes",
            "voir mes commandes", "historique paiements",
            "mes billets payes", "reservations confirmees",
            # English
            "checkouts", "my checkouts", "show checkouts",
            "view checkouts", "checkout history",
            "my payments", "show my payments", "payment history",
            "my orders", "confirmed bookings", "paid bookings",
        ],
        "response": "Let me pull up your checkout history. Hope you didn't break the bank.",
        "action": "MES RESERVATIONS",
    },
}

# ─────────────────────────────────────────────────────────────────────────────
# Ollama LLM tier — natural language understanding + parameter extraction
# ─────────────────────────────────────────────────────────────────────────────
OLLAMA_URL           = "http://localhost:11434/api/generate"
OLLAMA_CHAT_URL      = "http://localhost:11434/api/chat"
OLLAMA_MODEL         = "llama3.2"          # change to "qwen2.5" or "mistral" if preferred
OLLAMA_TIMEOUT_S     = 6                   # generous but bounded

# ── DeepSeek API ────────────────────────────────────────────────────────────────────
DEEPSEEK_API_KEY     = "sk-bdcdc8e442fe41fca9af53a67075b025"
DEEPSEEK_API_URL     = "https://api.deepseek.com/v1/chat/completions"
DEEPSEEK_MODEL       = "deepseek-chat"
DEEPSEEK_TIMEOUT_S   = 8

# Maps intent names back to action codes (same as INTENTS["xxx"]["action"]).
_INTENT_TO_ACTION = {k: v["action"] for k, v in
                     {"BOOK":          {"action": "BOOK"},
                      "VIEW_BOOKINGS": {"action": "MES RESERVATIONS"},
                      "CHECKOUT":      {"action": "MES RESERVATIONS"},
                      "SEARCH":        {"action": "RECHERCHER"},
                      "CANCEL":        {"action": "ANNULER"},
                      "PAY":           {"action": "PAYER"},
                      "HELP":          {"action": "AIDE"},
                      "DESCRIBE":      {"action": "DECRIRE"},
                      "LOGOUT":        {"action": "DECONNEXION"},
                      "LOGIN":         {"action": "LOGIN"},
                      "SIGNUP":        {"action": "SIGNUP"},
                      "GREET":         {"action": "NONE"},
                      "FOCUS_EMAIL":   {"action": "FOCUS_EMAIL"},
                      "FOCUS_PASSWORD":{"action": "FOCUS_PASSWORD"},
                      }.items()}

_OLLAMA_SYSTEM = (
    "You are GoVibe Assistant — a helpful but sarcastic travel assistant. "
    "Your personality: witty, slightly sarcastic, but always helpful. "
    "Be genuinely helpful first, sarcastic second. Use mild sarcasm like a friend would, "
    "not mean-spirited. For routine tasks add a witty remark. "
    "The user speaks French or English. "
    "IMPORTANT: the speech-to-text engine may be an English acoustic model, so "
    "French words may arrive phonetically transcribed without accents "
    "(e.g. 'reserver' instead of 'réserver'). Treat such words as their French equivalents. "
    "Classify the user's intent into exactly one of: "
    "BOOK, VIEW_BOOKINGS, CHECKOUT, SEARCH, CANCEL, PAY, HELP, DESCRIBE, LOGOUT, "
    "LOGIN, SIGNUP, GREET, FOCUS_EMAIL, FOCUS_PASSWORD, UNKNOWN. "
    "Also extract: destination (city name, or null), date (YYYY-MM-DD, or null). "
    "Write the 'response' field as short, witty, conversational English — "
    "it will be read aloud by a neural TTS voice. Keep it concise. "
    "Reply ONLY with minified JSON — no markdown, no explanation:\n"
    '{"intent":"BOOK","confidence":0.95,"destination":"Paris","date":null,'
    '"response":"Ooh la la! Paris it is. Let me see what\'s flying..."}'
)

_ollama_available = None   # None = not yet checked


def _check_ollama() -> bool:
    """Returns True if Ollama HTTP API is reachable."""
    global _ollama_available
    if _ollama_available is not None:
        return _ollama_available
    try:
        req = urllib.request.Request(
            "http://localhost:11434/",
            headers={"Accept": "text/plain"})
        with urllib.request.urlopen(req, timeout=2):
            pass
        _ollama_available = True
        _log(f"Ollama reachable at localhost:11434 — model '{OLLAMA_MODEL}'.")
    except Exception:
        _ollama_available = False
        _log("Ollama not reachable — tier-2 LLM disabled (start Ollama to enable).")
    return _ollama_available


def _classify_ollama(text: str):
    """
    Calls the local Ollama LLM to classify *text* and extract parameters.
    Returns (intent, confidence, destination, date, response_text) or None on failure.
    """
    if not _check_ollama():
        return None
    try:
        # Use /api/chat (supported by Ollama ≥ 0.1.14) for cleaner system+user split.
        payload = json.dumps({
            "model": OLLAMA_MODEL,
            "stream": False,
            "format": "json",
            "messages": [
                {"role": "system", "content": _OLLAMA_SYSTEM},
                {"role": "user",   "content": text},
            ],
        }, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(
            OLLAMA_CHAT_URL,
            data=payload,
            headers={"Content-Type": "application/json; charset=utf-8"})
        with urllib.request.urlopen(req, timeout=OLLAMA_TIMEOUT_S) as resp:
            raw = json.loads(resp.read())

        # /api/chat response: raw["message"]["content"]
        content_str = raw.get("message", {}).get("content", "")
        if not content_str:
            # Fallback: try /api/generate field
            content_str = raw.get("response", "")
        parsed = json.loads(content_str)

        intent  = parsed.get("intent",      "UNKNOWN").upper()
        conf    = float(parsed.get("confidence", 0.0))
        dest    = parsed.get("destination") or None
        date    = parsed.get("date")        or None
        resp_t  = parsed.get("response",    "") or ""

        # Validate — reject hallucinated intent names
        valid = set(_INTENT_TO_ACTION.keys()) | {"UNKNOWN"}
        if intent not in valid:
            intent = "UNKNOWN"
            conf   = 0.0

        _log(f"  [Ollama] intent={intent}  conf={conf:.2f}  dest={dest}  date={date}")
        return intent, conf, dest, date, resp_t

    except Exception as exc:
        _log(f"  [Ollama] Error: {exc}")
        return None


def _classify_deepseek(text: str):
    """
    Calls the DeepSeek API using **function calling** to classify user intent
    and extract structured travel parameters.

    Returns a 6-tuple:
        (intent, confidence, destination, date, response_text, passengers)
    or None on failure.

    Function calling forces DeepSeek to return a well-formed JSON argument
    object instead of free-form text, giving us reliable parameter extraction
    (destination city, ISO date, passenger count) even from messy speech.
    """
    try:
        # Build the allowed intent enum dynamically from the intent table.
        valid_intents = sorted(set(_INTENT_TO_ACTION.keys()) | {"UNKNOWN"})

        # Single function definition — DeepSeek will always call this.
        tools = [
            {
                "type": "function",
                "function": {
                    "name": "govibe_intent",
                    "description": (
                        "Classify the user's voice command into a GoVibe intent "
                        "and extract any travel parameters mentioned."
                    ),
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "intent": {
                                "type": "string",
                                "enum": valid_intents,
                                "description": "The GoVibe intent that best matches the user command.",
                            },
                            "confidence": {
                                "type": "number",
                                "description": "Confidence score 0.0 – 1.0.",
                            },
                            "destination": {
                                "type": "string",
                                "description": "Destination city name, or null if not mentioned.",
                            },
                            "date": {
                                "type": "string",
                                "description": "Travel date as YYYY-MM-DD, or null if not mentioned.",
                            },
                            "passengers": {
                                "type": "integer",
                                "description": "Number of passengers (seats). Default 1 if not mentioned.",
                            },
                            "response": {
                                "type": "string",
                                "description": "Short, sarcastic English reply to speak back to the user.",
                            },
                        },
                        "required": ["intent", "confidence", "response"],
                    },
                },
            }
        ]

        system_msg = (
            "You are the GoVibe travel assistant voice agent. "
            "Classify the user's spoken command into one of the allowed intents. "
            "Extract destination city, travel date (ISO format), and passenger count if mentioned. "
            "Always call the govibe_intent function — never reply with plain text."
        )

        payload = json.dumps({
            "model":       DEEPSEEK_MODEL,
            "stream":      False,
            "temperature": 0.1,
            "max_tokens":  250,
            "messages": [
                {"role": "system", "content": system_msg},
                {"role": "user",   "content": text},
            ],
            "tools":       tools,
            "tool_choice": {"type": "function", "function": {"name": "govibe_intent"}},
        }, ensure_ascii=False).encode("utf-8")

        req = urllib.request.Request(
            DEEPSEEK_API_URL,
            data=payload,
            headers={
                "Content-Type":  "application/json; charset=utf-8",
                "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
                "Accept":        "application/json",
            })
        with urllib.request.urlopen(req, timeout=DEEPSEEK_TIMEOUT_S) as resp:
            raw = json.loads(resp.read())

        message = raw.get("choices", [{}])[0].get("message", {})

        # ── Parse function-call arguments (preferred) ────────────────────────
        tool_calls = message.get("tool_calls") or []
        if tool_calls:
            args_str = tool_calls[0].get("function", {}).get("arguments", "{}")
            parsed   = json.loads(args_str)
        else:
            # Fallback: try to parse content as JSON (older API behaviour).
            content_str = message.get("content", "")
            if not content_str:
                return None
            parsed = json.loads(content_str)

        intent     = (parsed.get("intent") or "UNKNOWN").upper()
        conf       = float(parsed.get("confidence", 0.0))
        dest       = parsed.get("destination") or None
        date       = parsed.get("date")        or None
        resp_t     = parsed.get("response",    "") or ""
        passengers = int(parsed.get("passengers") or 1)
        if passengers < 1:
            passengers = 1

        valid = set(_INTENT_TO_ACTION.keys()) | {"UNKNOWN"}
        if intent not in valid:
            intent = "UNKNOWN"
            conf   = 0.0

        _log(f"  [DeepSeek-fn] intent={intent}  conf={conf:.2f}  dest={dest}"
             f"  date={date}  pax={passengers}")
        return intent, conf, dest, date, resp_t, passengers

    except Exception as exc:
        _log(f"  [DeepSeek] Error: {exc}")
        return None


_engine = "none"

# sentence-transformers state
_st_model       = None   # SentenceTransformer
_st_intent_vecs = {}     # intent → np.ndarray of normalised phrase embeddings

# sklearn state
_tfidf_vec  = None
_tfidf_mat  = None
_tfidf_lbl  = None


def _all_phrase_pairs():
    """Yields (phrase, intent_key) for every training sample."""
    for intent_key, data in INTENTS.items():
        for phrase in data["phrases"]:
            yield phrase, intent_key


def _try_sentence_transformers():
    """Load multilingual model (best for French), fall back to English MiniLM."""
    global _engine, _st_model, _st_intent_vecs
    try:
        from sentence_transformers import SentenceTransformer
        import numpy as np

        # Prefer the multilingual model — natively supports French, Spanish, German.
        # Falls back to English-only all-MiniLM-L6-v2 if not available/downloaded.
        for name in (
            "paraphrase-multilingual-MiniLM-L12-v2",  # ~470 MB, 50+ languages
            "all-MiniLM-L6-v2",                        # ~90 MB, English-first fallback
        ):
            try:
                _log(f"Trying sentence-transformers model '{name}'...")
                _st_model = SentenceTransformer(name)
                _log(f"Loaded '{name}'.")
                break
            except Exception as e:
                _log(f"Could not load '{name}': {e}")
                _st_model = None

        if _st_model is None:
            return False

        for intent_key, data in INTENTS.items():
            vecs = _st_model.encode(
                data["phrases"], convert_to_numpy=True,
                show_progress_bar=False, normalize_embeddings=True)
            _st_intent_vecs[intent_key] = vecs  # shape (n_phrases, dim)

        total = sum(v.shape[0] for v in _st_intent_vecs.values())
        _engine = "sentence-transformers"
        _log(f"sentence-transformers ready — {total} phrases across {len(_st_intent_vecs)} intents.")
        return True
    except Exception as exc:
        _log(f"sentence-transformers unavailable: {exc}")
        return False


def _load_sklearn():
    global _engine, _tfidf_vec, _tfidf_mat, _tfidf_lbl
    from sklearn.feature_extraction.text import TfidfVectorizer
    phrases, labels = zip(*_all_phrase_pairs())
    _tfidf_vec = TfidfVectorizer(
        analyzer="char_wb", ngram_range=(2, 4),
        sublinear_tf=True, min_df=1)
    # --- KEY FIX: normalise accents so English-STT output (e.g. "reserver")
    # matches the French training phrase ("réserver") — otherwise char n-grams
    # for é/e differ and the cosine distance is too high to classify correctly.
    _tfidf_mat = _tfidf_vec.fit_transform([_normalize(p) for p in phrases])
    _tfidf_lbl = list(labels)
    _engine = "sklearn-tfidf"
    _log(f"TF-IDF engine ready — {len(phrases)} phrases across {len(INTENTS)} intents. (accent-normalised)")


# ─────────────────────────────────────────────────────────────────────────────
# Classification
# ─────────────────────────────────────────────────────────────────────────────
def _classify_st(text):
    """
    Intent-level max-pooling: for each intent take the max cosine sim across
    all its training phrases, then return the intent with the highest score.
    """
    import numpy as np
    query = _st_model.encode(
        [text], convert_to_numpy=True, normalize_embeddings=True)  # (1, dim)

    best_intent = "UNKNOWN"
    best_score  = -1.0
    for intent_key, vecs in _st_intent_vecs.items():
        # L2-normalised vectors → dot product == cosine similarity
        sims    = (vecs @ query.T).flatten()
        max_sim = float(np.max(sims))
        if max_sim > best_score:
            best_score  = max_sim
            best_intent = intent_key
    return best_intent, best_score


def _classify_tfidf(text):
    from sklearn.metrics.pairwise import cosine_similarity
    import numpy as np
    # Normalise query the same way training phrases were normalised so that
    # accent differences (é→e) never tank the cosine score.
    vec  = _tfidf_vec.transform([_normalize(text)])
    sims = cosine_similarity(vec, _tfidf_mat).flatten()
    # Aggregate: max similarity per intent
    intent_scores = {}
    for idx, label in enumerate(_tfidf_lbl):
        score = float(sims[idx])
        if label not in intent_scores or score > intent_scores[label]:
            intent_scores[label] = score
    best_intent = max(intent_scores, key=intent_scores.get)
    return best_intent, intent_scores[best_intent]


def classify(text):
    """Returns (intent_key, confidence_float)."""
    text = text.strip()
    if not text:
        return "UNKNOWN", 0.0
    try:
        if _engine == "sentence-transformers":
            intent, conf = _classify_st(text)
            threshold = 0.55   # raised: garbled TTS echo scores 0.50-0.67, must reject
        else:
            intent, conf = _classify_tfidf(text)
            threshold = 0.30
        if conf < threshold:
            return "UNKNOWN", conf
        return intent, conf
    except Exception as exc:
        _log(f"classify error: {exc}")
        return "UNKNOWN", 0.0


# ─────────────────────────────────────────────────────────────────────────────
# Response builder
# ─────────────────────────────────────────────────────────────────────────────
def build_response(text, intent_key, confidence):
    if intent_key not in INTENTS:
        return {
            "intent": "UNKNOWN",
            "response": (
                f'I did not understand: "{text}". '
                "Say Help for the list of commands."
            ),
            "action": "UNKNOWN",
            "confidence": round(confidence, 3),
            "destination": None,
            "date": None,
            "engine": _engine,
        }
    data = INTENTS[intent_key]
    return {
        "intent": intent_key,
        "response": data["response"],
        "action": data["action"],
        "confidence": round(confidence, 3),
        "destination": None,
        "date": None,
        "engine": _engine,
    }


# ─────────────────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────────────────
def _log(msg):
    sys.stderr.write(f"[VoiceAgent] {msg}\n")
    sys.stderr.flush()


def _send(obj):
    sys.stdout.write(json.dumps(obj, ensure_ascii=False) + "\n")
    sys.stdout.flush()


# ─────────────────────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────────────────────
def main():
    _log("GoVibe Voice Agent starting...")

    if not _try_sentence_transformers():
        _load_sklearn()

    # Check Ollama availability asynchronously (don't block startup).
    _check_ollama()

    ready_engine = _engine + "+deepseek" + ("+ollama" if _ollama_available else "")
    _send({"status": "ready", "engine": ready_engine})
    _log(f"Ready. Engine: {_engine}. DeepSeek: YES. Ollama: {'YES' if _ollama_available else 'NO'}. Listening on stdin...")

    for raw_line in sys.stdin:
        raw_line = raw_line.strip()
        if not raw_line:
            continue
        try:
            if raw_line.startswith("{"):
                msg  = json.loads(raw_line)
                text = msg.get("text", "").strip()
            else:
                text = raw_line

            if not text:
                continue

            _log(f"Processing: \"{text}\"")

            destination = None
            date        = None
            passengers  = 1      # default; DeepSeek may override
            used_engine = _engine

            # ── Tier 0: direct accent-insensitive phrase lookup (instant) ─
            # Handles the common case where the English Vosk/SAPI STT model
            # strips accents from French speech so "réserver" becomes
            # "reserver" — we normalise both sides and do a direct string
            # comparison before touching any ML model.
            intent_key, confidence = _direct_match(text)
            if intent_key != "UNKNOWN":
                _log(f"  Tier-0 direct match → {intent_key} (conf=1.0)")
                used_engine = "direct-match"

            # ── Tier 1: sentence-transformers (fast) ───────────────────────
            if intent_key == "UNKNOWN":
                intent_key, confidence = classify(text)

            # ── Tier 2: DeepSeek API with function calling (if tier-1 uncertain) ──
            if intent_key == "UNKNOWN":
                _log("  Tier-1 uncertain — escalating to DeepSeek function-calling API...")
                ds_result = _classify_deepseek(text)
                if ds_result is not None:
                    ds_intent, ds_conf, ds_dest, ds_date, ds_resp, ds_pax = ds_result
                    if ds_intent != "UNKNOWN" and ds_conf >= 0.60:
                        intent_key  = ds_intent
                        confidence  = ds_conf
                        destination = ds_dest
                        date        = ds_date
                        passengers  = ds_pax
                        used_engine = "deepseek"
                        if ds_resp and intent_key in INTENTS:
                            INTENTS[intent_key]["_llm_response"] = ds_resp
                # Fall back to Ollama only if DeepSeek also failed
                if intent_key == "UNKNOWN" and _check_ollama():
                    _log("  DeepSeek failed — trying Ollama...")
                    ollama_result = _classify_ollama(text)
                    if ollama_result is not None:
                        ol_intent, ol_conf, ol_dest, ol_date, ol_resp = ollama_result
                        if ol_intent != "UNKNOWN" and ol_conf >= 0.65:
                            intent_key  = ol_intent
                            confidence  = ol_conf
                            destination = ol_dest
                            date        = ol_date
                            used_engine = "ollama"
                            if ol_resp and intent_key in INTENTS:
                                INTENTS[intent_key]["_llm_response"] = ol_resp

            result = build_response(text, intent_key, confidence)

            # Inject LLM-extracted parameters.
            if destination:
                result["destination"] = destination
            if date:
                result["date"] = date
            # Always include passengers so Java can read it without null-checks.
            result["passengers"] = passengers
            result["engine"]     = used_engine

            # Use LLM-generated response if present.
            _llm_resp = INTENTS.get(intent_key, {}).pop("_llm_response", None)
            if _llm_resp:
                result["response"] = _llm_resp

            _log(f"  → intent={intent_key}  confidence={confidence:.3f}  "
                 f"action={result['action']}  engine={used_engine}  "
                 f"dest={destination}  date={date}  pax={passengers}")
            _send(result)

        except json.JSONDecodeError:
            text = raw_line
            intent_key, confidence = classify(text)
            result = build_response(text, intent_key, confidence)
            result["passengers"] = 1
            result["engine"]     = _engine
            _send(result)
        except Exception as exc:
            _log(f"Error processing: {exc}")
            _send({"intent": "ERROR", "response": "", "action": "UNKNOWN",
                   "confidence": 0.0, "destination": None, "date": None,
                   "passengers": 1, "engine": "error"})


if __name__ == "__main__":
    main()
