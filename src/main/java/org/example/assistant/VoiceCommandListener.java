package org.example.assistant;

/**
 * Callback interface for the voice assistant.
 * Implement this to receive recognised commands from {@link VoiceAssistantService}.
 */
public interface VoiceCommandListener {

    /**
     * Called on the caller thread (NOT the JavaFX thread) whenever Vosk
     * successfully decodes a phrase.
     *
     * @param command  upper-cased, trimmed recognised text (e.g. "RESERVER")
     * @param rawText  the original JSON string returned by Vosk
     */
    void onCommand(String command, String rawText);
}
