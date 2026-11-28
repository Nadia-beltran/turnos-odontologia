package ar.turnos.shared;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cliente mínimo de la API de Mailpit: listar, leer y vaciar mensajes (design D8). */
public class MailpitClient {

    private final String base;
    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    public MailpitClient(String apiUrl) {
        this.base = apiUrl + "/api/v1";
    }

    /** Mensajes recibidos, cada uno con su detalle completo (HTML, Text, From, To, ReplyTo, Subject). */
    public List<JsonNode> mensajes() {
        List<JsonNode> r = new ArrayList<>();
        for (JsonNode resumen : llamar("GET", "/messages").get("messages")) {
            r.add(llamar("GET", "/message/" + resumen.get("ID").asString()));
        }
        return r;
    }

    public void vaciar() {
        llamar("DELETE", "/messages");  // responde "ok" en texto plano
    }

    private JsonNode llamar(String metodo, String ruta) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + ruta))
                    .method(metodo, HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                throw new IllegalStateException("Mailpit respondió " + res.statusCode());
            }
            return "DELETE".equals(metodo) ? null : json.readTree(res.body());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo hablar con Mailpit", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
