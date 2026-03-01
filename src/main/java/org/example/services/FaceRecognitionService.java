package org.example.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class FaceRecognitionService {

    private final ObjectMapper mapper = new ObjectMapper();
    private String pythonCommand = null;

    private String getPythonCommand() {
        if (pythonCommand != null) return pythonCommand;

        // Prefer py -3.11 since face scripts need opencv + mediapipe (installed under 3.11)
        // face_recognition / dlib do NOT build on Python 3.14
        String[][] candidates = {
            {"py", "-3.11"},
            {"py", "-3.12"},
            {"py", "-3.10"},
            {"python3.11"},
            {"python3.12"},
            {"python3.10"},
            {"python"},
            {"python3"},
            {"py"}
        };
        for (String[] cmd : candidates) {
            try {
                java.util.List<String> args = new java.util.ArrayList<>();
                args.add(cmd[0]);
                if (cmd.length > 1) args.add(cmd[1]);
                args.add("--version");
                Process p = new ProcessBuilder(args).start();
                if (p.waitFor() == 0) {
                    // Check that cv2 and mediapipe are available in this interpreter
                    java.util.List<String> checkArgs = new java.util.ArrayList<>();
                    checkArgs.add(cmd[0]);
                    if (cmd.length > 1) checkArgs.add(cmd[1]);
                    checkArgs.add("-c");
                    checkArgs.add("import cv2, mediapipe");
                    Process chk = new ProcessBuilder(checkArgs).start();
                    if (chk.waitFor() == 0) {
                        // Build a single command string: "py -3.11" or "python3.11"
                        pythonCommand = String.join(" ", args.subList(0, args.size() - 1));
                        System.out.println("✅ Face ID Python: " + pythonCommand + " (cv2 + mediapipe OK)");
                        return pythonCommand;
                    }
                }
            } catch (Exception ignored) {}
        }
        System.err.println("❌ No suitable Python with cv2+mediapipe found. Face ID will not work.");
        pythonCommand = "py -3.11";
        return pythonCommand;
    }

    private ProcessBuilder buildProcess(String scriptPath, String... extraArgs) {
        // Split "py -3.11" into ["py", "-3.11"] for ProcessBuilder
        java.util.List<String> cmd = new java.util.ArrayList<>();
        for (String part : pythonCommand.split(" ")) cmd.add(part);
        cmd.add(scriptPath);
        for (String a : extraArgs) cmd.add(a);
        return new ProcessBuilder(cmd);
    }

    /**
     * Appelle le script Python pour enregistrer un visage.
     * @return L'encodage JSON du visage sous forme de String, ou null si échec.
     */
    public String registerFaceEncoding() {
        try {
            getPythonCommand(); // ensure pythonCommand is set
            String scriptPath = Paths.get("ai_scripts", "register_face.py").toAbsolutePath().toString();
            ProcessBuilder pb = buildProcess(scriptPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();

            String jsonOutput = output.toString();
            if (jsonOutput.trim().isEmpty()) {
                System.err.println("❌ Face ID script returned no output. Check that Python 3.11 has cv2 and mediapipe installed.");
                return null;
            }

            // Extract the JSON line (mediapipe/cv2 may print debug lines before the JSON)
            String jsonLine = null;
            for (String l : jsonOutput.split("\n")) {
                String t = l.trim();
                if (t.startsWith("{")) { jsonLine = t; }
            }
            if (jsonLine == null) {
                System.err.println("❌ No JSON found in Python output: " + jsonOutput);
                return null;
            }

            // Parsing output
            try {
                JsonNode rootNode = mapper.readTree(jsonLine);
                if ("success".equals(rootNode.path("status").asText())) {
                    return rootNode.path("encoding").toString();
                } else {
                    System.err.println("❌ Face ID Error: " + rootNode.path("message").asText());
                    return null;
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to parse Python output: " + jsonOutput);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Appelle le script Python pour vérifier un visage par rapport à un encodage sauvegardé.
     * @param savedEncodingJson L'encodage JSON sauvegardé dans la BDD.
     * @return true si le visage correspond, false sinon.
     */
    public boolean verifyFace(String savedEncodingJson) {
        if (savedEncodingJson == null || savedEncodingJson.trim().isEmpty()) {
            return false;
        }

        try {
            getPythonCommand(); // ensure pythonCommand is set
            String scriptPath = Paths.get("ai_scripts", "login_face.py").toAbsolutePath().toString();
            ProcessBuilder pb = buildProcess(scriptPath, "--encoding", savedEncodingJson);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();

            String jsonOutput = output.toString();
            if (jsonOutput.trim().isEmpty()) {
                return false;
            }

            // Extract the JSON line (mediapipe/cv2 may print debug lines before the JSON)
            String jsonLine = null;
            for (String l : jsonOutput.split("\n")) {
                String t = l.trim();
                if (t.startsWith("{")) { jsonLine = t; }
            }
            if (jsonLine == null) {
                System.err.println("❌ No JSON found in Python output: " + jsonOutput);
                return false;
            }

            // Parsing output
            try {
                JsonNode rootNode = mapper.readTree(jsonLine);
                if ("success".equals(rootNode.path("status").asText())) {
                    double distance = rootNode.path("distance").asDouble();
                    System.out.println("✅ Face ID matched with distance: " + distance);
                    return true;
                } else {
                    System.err.println("❌ Face ID Verification Failed: " + rootNode.path("message").asText());
                    return false;
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to parse Python output: " + jsonOutput);
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
