package org.example.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.dao.CheckoutDAO;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class PaymentCallbackServer {
    private HttpServer server;
    private int port;
    private boolean running = false;
    private Runnable onSuccessCallback;
    private final CheckoutDAO checkoutDAO = new CheckoutDAO();

    /** Called by the controller so it gets notified when Stripe redirects to /success */
    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void start() throws IOException {
        if (running) return;   // already running — reuse same port
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/success", new SuccessHandler());
        server.createContext("/cancel", new CancelHandler());
        server.setExecutor(null);
        server.start();
        running = true;
        System.out.println("[PaymentServer] Started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
        }
    }

    public int getPort() {
        return port;
    }

    private class SuccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String bookingIdStr = params.get("id");

            if (bookingIdStr != null) {
                try {
                    int bookingId = Integer.parseInt(bookingIdStr);
                    checkoutDAO.updateStatus(bookingId, "CONFIRMED");
                    System.out.println("[PaymentServer] Updated booking " + bookingId + " to CONFIRMED");
                } catch (NumberFormatException e) {
                    System.err.println("[PaymentServer] Invalid booking ID: " + bookingIdStr);
                }
            }

            // Notify the JavaFX controller so the modal closes + dashboard refreshes
            if (onSuccessCallback != null) {
                javafx.application.Platform.runLater(onSuccessCallback);
            }

            String response = "<html><body style='font-family:sans-serif;text-align:center;padding-top:60px;background:#f0fdf4;'>" +
                              "<h1 style='color:#00b894;'>\u2705 Payment Successful!</h1>" +
                              "<p style='color:#636e72;'>You can close this window and return to GoVibe.</p>" +
                              "</body></html>";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private class CancelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                              "<h1 style='color:#EF476F;'>Payment Cancelled</h1>" +
                              "<p>You can close this window now.</p>" +
                              "</body></html>";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
