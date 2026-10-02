package com.organizacao_de_recursos.config;

import com.organizacao_de_recursos.notification.Notificador;
import com.organizacao_de_recursos.notification.NotificadorHttp;
import com.organizacao_de_recursos.notification.NotificadorSimulado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class NotificacaoConfig {

    @Bean
    @Profile("dev")
    public Notificador notificadorSimulado() {
        return new NotificadorSimulado();
    }

    @Bean
    @Profile("!dev")
    public Notificador notificadorHttp(RestClient.Builder restClientBuilder,
                                        @Value("${app.notificacao.base-url}") String baseUrl,
                                        @Value("${app.notificacao.timeout-conexao-ms:2000}") long timeoutConexaoMs,
                                        @Value("${app.notificacao.timeout-leitura-ms:3000}") long timeoutLeituraMs) {
        return new NotificadorHttp(restClientBuilder, baseUrl,
                Duration.ofMillis(timeoutConexaoMs), Duration.ofMillis(timeoutLeituraMs));
    }
}
