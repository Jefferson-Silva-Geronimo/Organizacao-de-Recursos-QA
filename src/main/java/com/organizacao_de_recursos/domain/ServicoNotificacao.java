package com.organizacao_de_recursos.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Notificação de eventos do fluxo de reservas (RF-20).
 *
 * Produz a notificação pelo canal configurado. Se o canal falha (por exemplo, API externa que não responde),
 * a falha é registrada como resultado observável, sem propagar e sem expor detalhes internos da exceção.
 *
 * Quais eventos disparam notificação, o canal real, o destinatário e o conteúdo seguem pendentes
 * (arquitetura, J15); este serviço não decide nenhum deles.
 */
public class ServicoNotificacao {
    private static final String MENSAGEM_ENTREGUE = "Notificação enviada";
    private static final String MENSAGEM_FALHA = "Não foi possível enviar a notificação";

    private final CanalNotificacao canal;
    private final List<ResultadoNotificacao> registros = new CopyOnWriteArrayList<>();

    public ServicoNotificacao(CanalNotificacao canal) {
        if (canal == null) {
            throw new IllegalArgumentException("Canal de notificação é obrigatório");
        }
        this.canal = canal;
    }

    /** Produz a notificação do evento pelo canal e registra o resultado. */
    public ResultadoNotificacao notificar(EventoNotificacao evento) {
        if (evento == null) {
            throw new IllegalArgumentException("Evento é obrigatório");
        }
        ResultadoNotificacao resultado;
        try {
            canal.enviar(evento);
            resultado = new ResultadoNotificacao(evento, true, MENSAGEM_ENTREGUE);
        } catch (RuntimeException falhaDoCanal) {
            resultado = new ResultadoNotificacao(evento, false, MENSAGEM_FALHA);
        }
        registros.add(resultado);
        return resultado;
    }

    /** Resultados registrados, em ordem de ocorrência. */
    public List<ResultadoNotificacao> registros() {
        return List.copyOf(registros);
    }
}
