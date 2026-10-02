package com.organizacao_de_recursos;

import com.organizacao_de_recursos.domain.Usuario;
import com.organizacao_de_recursos.dto.LoginRequest;
import com.organizacao_de_recursos.dto.LoginResponse;
import com.organizacao_de_recursos.dto.ReservaResponse;
import com.organizacao_de_recursos.model.SalaEntity;
import com.organizacao_de_recursos.model.UsuarioEntity;
import com.organizacao_de_recursos.repository.SalaRepository;
import com.organizacao_de_recursos.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste caixa-preta de ponta a ponta (seção 7 do plano de migração): login → buscar sala →
 * reservar recurso restrito → aprovar → iniciar → concluir → consultar histórico e relatório.
 * Retirada/devolução de materiais não entram (ReservaService ainda só suporta sala - pendência
 * documentada no relatório final).
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FluxoCompletoReservaTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Fluxo completo: login -> buscar -> reservar -> aprovar -> iniciar -> concluir -> histórico -> relatório")
    void fluxoCompletoDeReserva() {
        usuarioRepository.save(new UsuarioEntity("solicitante.e2e", passwordEncoder.encode("senha123"), Usuario.Perfil.SOLICITANTE));
        UsuarioEntity responsavel = usuarioRepository.save(
                new UsuarioEntity("responsavel.e2e", passwordEncoder.encode("senha123"), Usuario.Perfil.RESPONSAVEL));
        SalaEntity salaRestrita = salaRepository.save(new SalaEntity("Auditório E2E", true, responsavel.getId()));

        String tokenSolicitante = login("solicitante.e2e", "senha123");
        String tokenResponsavel = login("responsavel.e2e", "senha123");

        // buscar: a listagem de salas responde (autenticado)
        ResponseEntity<List> salas = restTemplate.exchange("/api/v1/salas", HttpMethod.GET,
                autenticado(tokenSolicitante), List.class);
        assertThat(salas.getStatusCode()).isEqualTo(HttpStatus.OK);

        // reservar
        OffsetDateTime inicio = OffsetDateTime.now().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime fim = inicio.plusHours(1);
        Map<String, Object> corpoReserva = Map.of("salaId", salaRestrita.getId(), "inicio", inicio.toString(), "fim", fim.toString());
        ResponseEntity<ReservaResponse> criada = restTemplate.exchange("/api/v1/reservas", HttpMethod.POST,
                autenticado(tokenSolicitante, corpoReserva), ReservaResponse.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long reservaId = criada.getBody().id();
        assertThat(criada.getBody().estado()).isEqualTo("SOLICITADA");

        // aprovar (Responsável no escopo)
        ResponseEntity<ReservaResponse> aprovada = restTemplate.exchange("/api/v1/reservas/" + reservaId + "/aprovar",
                HttpMethod.POST, autenticado(tokenResponsavel), ReservaResponse.class);
        assertThat(aprovada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aprovada.getBody().estado()).isEqualTo("APROVADA");

        // iniciar
        ResponseEntity<ReservaResponse> emUso = restTemplate.exchange("/api/v1/reservas/" + reservaId + "/iniciar",
                HttpMethod.POST, autenticado(tokenResponsavel), ReservaResponse.class);
        assertThat(emUso.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emUso.getBody().estado()).isEqualTo("EM_USO");

        // concluir
        ResponseEntity<ReservaResponse> concluida = restTemplate.exchange("/api/v1/reservas/" + reservaId + "/concluir",
                HttpMethod.POST, autenticado(tokenResponsavel), ReservaResponse.class);
        assertThat(concluida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(concluida.getBody().estado()).isEqualTo("CONCLUIDA");

        // histórico: SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA
        ResponseEntity<List> historico = restTemplate.exchange("/api/v1/reservas/" + reservaId + "/historico",
                HttpMethod.GET, autenticado(tokenResponsavel), List.class);
        assertThat(historico.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historico.getBody()).hasSize(4);

        // relatório de utilização: a sala aparece com pelo menos 1 reserva no período
        String de = inicio.minusHours(1).toString();
        String ate = fim.plusHours(1).toString();
        ResponseEntity<Map> utilizacao = restTemplate.exchange(
                "/api/v1/relatorios/utilizacao?de=" + de + "&ate=" + ate,
                HttpMethod.GET, autenticado(tokenResponsavel), Map.class);
        assertThat(utilizacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(utilizacao.getBody()).containsKey(salaRestrita.getId().toString());
    }

    private String login(String username, String senha) {
        ResponseEntity<LoginResponse> resposta = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(username, senha), LoginResponse.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody().token();
    }

    private HttpEntity<Void> autenticado(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> autenticado(String token, T corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(corpo, headers);
    }
}
