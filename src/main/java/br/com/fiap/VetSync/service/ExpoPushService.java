package br.com.fiap.VetSync.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Envia push pela API do Expo. Nunca lança exceção: falha de rede não pode derrubar
 * o fluxo de negócio (cancelar, reagendar, confirmar).
 */
@Slf4j
@Service
public class ExpoPushService {

    private static final int LIMITE_POR_REQUISICAO = 100;

    public record MensagemPush(String token, String titulo, String corpo, Map<String, Object> dados) {}

    /** enviados = mensagens aceitas pelo Expo; tokensInvalidos = aparelhos que devem ser desativados. */
    public record ResultadoEnvio(int enviados, Set<String> tokensInvalidos) {}

    private final RestClient restClient;
    private final boolean habilitado;
    private final String url;

    public ExpoPushService(
            @Value("${app.notificacoes.push-habilitado:true}") boolean habilitado,
            @Value("${app.notificacoes.expo-url:https://exp.host/--/api/v2/push/send}") String url) {
        this.habilitado = habilitado;
        this.url = url;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(8_000);
        this.restClient = RestClient.builder().requestFactory(fabrica).build();
    }

    public ResultadoEnvio enviar(List<MensagemPush> mensagens) {
        Set<String> invalidos = new HashSet<>();
        if (!habilitado || mensagens == null || mensagens.isEmpty()) {
            return new ResultadoEnvio(0, invalidos);
        }

        List<MensagemPush> validas = mensagens.stream().filter(m -> tokenExpoValido(m.token())).toList();
        int enviados = 0;
        for (int i = 0; i < validas.size(); i += LIMITE_POR_REQUISICAO) {
            List<MensagemPush> lote = validas.subList(i, Math.min(i + LIMITE_POR_REQUISICAO, validas.size()));
            enviados += enviarLote(lote, invalidos);
        }
        return new ResultadoEnvio(enviados, invalidos);
    }

    private int enviarLote(List<MensagemPush> lote, Set<String> invalidos) {
        try {
            List<Map<String, Object>> corpo = new ArrayList<>();
            for (MensagemPush m : lote) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("to", m.token());
                item.put("title", m.titulo());
                item.put("body", m.corpo());
                item.put("sound", "default");
                item.put("priority", "high");
                if (m.dados() != null) item.put("data", m.dados());
                corpo.add(item);
            }

            JsonNode resposta = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(JsonNode.class);

            return interpretarResposta(lote, resposta, invalidos);
        } catch (Exception e) {
            log.warn("Falha ao enviar push pelo Expo: {}", e.getMessage());
            return 0;
        }
    }

    private int interpretarResposta(List<MensagemPush> lote, JsonNode resposta, Set<String> invalidos) {
        if (resposta == null || !resposta.has("data") || !resposta.get("data").isArray()) {
            return 0;
        }
        JsonNode tickets = resposta.get("data");
        int ok = 0;
        for (int i = 0; i < tickets.size() && i < lote.size(); i++) {
            JsonNode ticket = tickets.get(i);
            if ("ok".equals(ticket.path("status").asText())) {
                ok++;
            } else if ("DeviceNotRegistered".equals(ticket.path("details").path("error").asText())) {
                invalidos.add(lote.get(i).token());
            } else {
                log.warn("Expo recusou o push: {}", ticket.path("message").asText());
            }
        }
        return ok;
    }

    private static boolean tokenExpoValido(String token) {
        return token != null && (token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken["));
    }
}