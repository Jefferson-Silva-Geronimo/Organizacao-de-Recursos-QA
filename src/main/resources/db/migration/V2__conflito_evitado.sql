-- V2: registra cada tentativa de reserva recusada pela constraint de exclusão (RN-04),
-- para alimentar o relatório de conflitos evitados (seção 6/Onda 4 do plano de migração).

-- solicitante_id é só informativo (sem FK): a gravação ocorre em transação própria
-- (REQUIRES_NEW, ver ConflitoEvitadoService) para sobreviver ao rollback da tentativa recusada,
-- e nesse ponto o solicitante já existe de fato no banco (a FK só evitaria problema hipotético).
CREATE TABLE conflito_evitado (
    id             BIGSERIAL PRIMARY KEY,
    tipo_recurso   VARCHAR(20)  NOT NULL,
    recurso_id     BIGINT       NOT NULL,
    solicitante_id BIGINT,
    tentado_em     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_conflito_evitado_recurso ON conflito_evitado (tipo_recurso, recurso_id, tentado_em);
