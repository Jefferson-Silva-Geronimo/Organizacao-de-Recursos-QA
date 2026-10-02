package com.organizacao_de_recursos.exception;

import com.organizacao_de_recursos.domain.AcessoNegadoException;
import com.organizacao_de_recursos.domain.ReservaAgendaProfessorException;
import com.organizacao_de_recursos.domain.ReservaApagamentoException;
import com.organizacao_de_recursos.domain.ReservaAprovacaoException;
import com.organizacao_de_recursos.domain.ReservaAuditoriaException;
import com.organizacao_de_recursos.domain.ReservaCriacaoException;
import com.organizacao_de_recursos.domain.ReservaFluxoEstadosException;
import com.organizacao_de_recursos.domain.ReservaManutencaoException;
import com.organizacao_de_recursos.domain.ReservaMovimentacaoException;
import com.organizacao_de_recursos.domain.ReservaSobreposicaoException;
import com.organizacao_de_recursos.domain.ReservaTemporalException;
import com.organizacao_de_recursos.domain.TradutorErros;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Traduz exceções para {@code ProblemDetail} (RFC 9457) sem nunca expor stack trace, SQL ou nome
 * de classe (RNF-15/17) - reaproveita {@link TradutorErros} para a mensagem segura.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final TradutorErros tradutorErros = new TradutorErros();

    @ExceptionHandler(ReservaSobreposicaoException.class)
    public ProblemDetail conflito(ReservaSobreposicaoException ex) {
        return problem(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ReservaFluxoEstadosException.class)
    public ProblemDetail transicaoInvalida(ReservaFluxoEstadosException ex) {
        return problem(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler({ReservaCriacaoException.class, ReservaManutencaoException.class,
            ReservaAprovacaoException.class, ReservaApagamentoException.class,
            ReservaAgendaProfessorException.class, ReservaMovimentacaoException.class,
            ReservaAuditoriaException.class, ReservaTemporalException.class})
    public ProblemDetail regraDeNegocio(RuntimeException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ProblemDetail acessoNegado(AcessoNegadoException ex) {
        return problem(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegadoSpring() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail autenticacao() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail naoEncontrado() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        ProblemDetail problema = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problema.setTitle("Erro de validação");
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail erroInesperado(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, tradutorErros.mensagemParaUsuario(ex));
    }

    private ProblemDetail problem(HttpStatus status, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(status, tradutorErros.mensagemParaUsuario(ex));
    }
}
