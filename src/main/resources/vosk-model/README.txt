===========================================================================
GoVibe — Vosk offline speech model setup
===========================================================================

The GoVibe voice assistant uses Vosk for 100% offline speech recognition
(no internet required after setup).

---------------------------------------------------------------------------
STEP 1 — Download the model
---------------------------------------------------------------------------
Go to: https://alphacephei.com/vosk/models

Recommended English model (~50 MB):
  vosk-model-small-en-us-0.15.zip

French model (~96 MB):
  vosk-model-small-fr-0.22.zip

Download and extract the zip. You will get a folder such as:
  vosk-model-small-en-us-0.15/

---------------------------------------------------------------------------
STEP 2 — Place the model
---------------------------------------------------------------------------
GoVibe checks the following locations (in order):

  A) %USERPROFILE%\.govibe\vosk-model\
     e.g.  C:\Users\YourName\.govibe\vosk-model\

  B) vosk-model\  (in the current working directory)

Rename / copy the extracted folder to match one of those paths exactly.

Quickest option (Windows PowerShell):

  mkdir "$env:USERPROFILE\.govibe"
  Move-Item vosk-model-small-en-us-0.15 "$env:USERPROFILE\.govibe\vosk-model"

---------------------------------------------------------------------------
STEP 3 — Run the app
---------------------------------------------------------------------------
Start GoVibe normally:

  mvnw.cmd javafx:run

After login you will hear:
  "Bienvenue dans GoVibe. L'assistant vocal est actif."

---------------------------------------------------------------------------
AVAILABLE VOICE COMMANDS
---------------------------------------------------------------------------
  FRENCH                   ENGLISH equivalent
  -------                  ------------------
  Réserver                 Book / New reservation
  Mes réservations         My bookings / Reservations
  Rechercher / Vols        Search / Flights
  Payer / Confirmer        Pay / Payment / Confirm
  Annuler / Retour         Cancel / Go back
  Aide / Commandes         Help
  Décrire / Quoi           Describe / What
  Déconnexion              Logout / Log out

---------------------------------------------------------------------------
NO MODEL? — TTS still works
---------------------------------------------------------------------------
If no model is found, Vosk STT is disabled but Windows SAPI text-to-speech
still works. You can call VoiceAssistantService.getInstance().speak("text")
from any controller at any time.

---------------------------------------------------------------------------
TROUBLESHOOTING
---------------------------------------------------------------------------
• "Vosk model not found" in console → model path is wrong (check STEP 2).
• "Microphone line not supported" → another app holds the microphone
  exclusively; close it and restart GoVibe.
• Slow/no recognition → use the small model listed above (faster than the
  full model on regular PCs).
===========================================================================
