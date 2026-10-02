package com.organizacao_de_recursos.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Canal de notificação via HTTP (RF-20). Falha do canal externo (erro, timeout ou ausência de
 * resposta) é logada e nunca propaga - a reserva permanece confirmada independentemente do
 * resultado da notificação.
 */
public class NotificadorHttp implements Notificador {

    private static final Logger log = LoggerFactory.getLogger(NotificadorHttp.class);

    private final RestClient restClient;

    public NotificadorHttp(RestClient.Builder builder, String baseUrl, Duration timeoutConexao, Duration timeoutLeitura) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutConexao.toMillis());
        requestFactory.setReadTimeout((int) timeoutLeitura.toMillis());
        this.restClient = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public void notificar(Long reservaId, String tipo) {
        try {
            restClient.post()
                    .uri("/notificacoes")
                    .body(new NotificacaoPayload(reservaId, tipo))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException falhaDeComunicacao) {
            log.warn("Falha ao notificar a reserva {} ({}): {}", reservaId, tipo, falhaDeComunicacao.getMessage());
        }
    }

    private record NotificacaoPayload(Long reservaId, String tipo) {
    }
}
