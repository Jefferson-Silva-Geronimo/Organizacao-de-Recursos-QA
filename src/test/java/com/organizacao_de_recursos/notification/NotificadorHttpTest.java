package com.organizacao_de_recursos.notification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * NotificadorHttp contra um servidor WireMock real: sucesso, erro 500 e timeout nunca propagam
 * (a reserva permanece confirmada independentemente do resultado da notificação).
 */
class NotificadorHttpTest {

    private WireMockServer wireMockServer;
    private NotificadorHttp notificador;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        notificador = new NotificadorHttp(RestClient.builder(), wireMockServer.baseUrl(),
                Duration.ofMillis(500), Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Sucesso: canal responde 200, notificar não lança exceção")
    void notificar_sucesso() {
        wireMockServer.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(200)));

        assertThatNoException().isThrownBy(() -> notificador.notificar(1L, "APROVADA"));

        wireMockServer.verify(1, WireMock.postRequestedFor(urlEqualTo("/notificacoes")));
    }

    @Test
    @DisplayName("Erro 500: logado, não propaga - a reserva permanece confirmada")
    void notificar_erro500_naoPropaga() {
        wireMockServer.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(500)));

        assertThatNoException().isThrownBy(() -> notificador.notificar(2L, "REJEITADA"));
    }

    @Test
    @DisplayName("Timeout: logado, não propaga - a reserva permanece confirmada")
    void notificar_timeout_naoPropaga() {
        wireMockServer.stubFor(post(urlEqualTo("/notificacoes"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

        assertThatNoException().isThrownBy(() -> notificador.notificar(3L, "CANCELADA"));
    }

    @Test
    @DisplayName("Payload enviado contém o id da reserva e o tipo do evento")
    void notificar_enviaPayloadCorreto() {
        wireMockServer.stubFor(post(urlEqualTo("/notificacoes")).willReturn(aResponse().withStatus(200)));

        notificador.notificar(42L, "APROVADA");

        wireMockServer.verify(WireMock.postRequestedFor(urlEqualTo("/notificacoes"))
                .withRequestBody(WireMock.matchingJsonPath("$.reservaId", WireMock.equalTo("42")))
                .withRequestBody(WireMock.matchingJsonPath("$.tipo", WireMock.equalTo("APROVADA"))));
    }
}
