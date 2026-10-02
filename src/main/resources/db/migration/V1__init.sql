-- V1: schema inicial — Organização de Recursos
-- Decisões aplicadas: D1-D8 (prompt de migração) + decisões complementares
-- (perfil único por usuário; auditoria só em mudanças efetivas; desempate ocorrido_em+id).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL CHECK (perfil IN ('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR')),
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE sala (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    capacidade     INTEGER,
    localizacao    VARCHAR(255),
    restrito       BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    responsavel_id BIGINT       REFERENCES usuario (id)
);

CREATE TABLE professor (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    usuario_id     BIGINT       REFERENCES usuario (id),
    restrito       BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    responsavel_id BIGINT       REFERENCES usuario (id)
);

CREATE TABLE competencia (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL UNIQUE
);

CREATE TABLE professor_competencia (
    professor_id   BIGINT NOT NULL REFERENCES professor (id),
    competencia_id BIGINT NOT NULL REFERENCES competencia (id),
    PRIMARY KEY (professor_id, competencia_id)
);

CREATE TABLE material (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    tipo           VARCHAR(100),
    quantidade     INTEGER      NOT NULL DEFAULT 1,
    restrito       BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    responsavel_id BIGINT       REFERENCES usuario (id)
);

CREATE TABLE reserva (
    id             BIGSERIAL PRIMARY KEY,
    solicitante_id BIGINT       NOT NULL REFERENCES usuario (id),
    aprovador_id   BIGINT       REFERENCES usuario (id),
    estado         VARCHAR(20)  NOT NULL CHECK (estado IN (
        'SOLICITADA', 'APROVADA', 'EM_USO', 'CONCLUIDA',
        'REJEITADA', 'CANCELADA', 'NAO_COMPARECEU'
    )),
    inicio         TIMESTAMPTZ  NOT NULL,
    fim            TIMESTAMPTZ  NOT NULL,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_reserva_periodo CHECK (fim > inicio)
);

-- Uma linha por recurso (sala, professor ou material) envolvido na reserva.
-- "ocupa" indica se a reserva, no estado atual, deve bloquear o recurso (D3).
CREATE TABLE reserva_recurso (
    id           BIGSERIAL PRIMARY KEY,
    reserva_id   BIGINT       NOT NULL REFERENCES reserva (id),
    tipo_recurso VARCHAR(20)  NOT NULL CHECK (tipo_recurso IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    recurso_id   BIGINT       NOT NULL,
    periodo      TSTZRANGE    NOT NULL,
    ocupa        BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Garantia de dupla-reserva (RN-04): o próprio banco rejeita qualquer
-- sobreposição real ([início,fim) semiaberto — adjacentes não conflitam, D7)
-- para o mesmo tipo_recurso+recurso_id enquanto "ocupa" for verdadeiro.
ALTER TABLE reserva_recurso
    ADD CONSTRAINT ex_sem_sobreposicao
        EXCLUDE USING gist (tipo_recurso WITH =, recurso_id WITH =, periodo WITH &&)
        WHERE (ocupa);

CREATE INDEX idx_reserva_recurso_reserva ON reserva_recurso (reserva_id);

CREATE TABLE bloqueio (
    id           BIGSERIAL PRIMARY KEY,
    tipo_recurso VARCHAR(20)  NOT NULL CHECK (tipo_recurso IN ('SALA', 'PROFESSOR', 'MATERIAL')),
    recurso_id   BIGINT       NOT NULL,
    periodo      TSTZRANGE    NOT NULL,
    motivo       VARCHAR(20)  NOT NULL CHECK (motivo IN ('MANUTENCAO', 'ADMINISTRATIVO')),
    criado_por   BIGINT       REFERENCES usuario (id),
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_bloqueio_recurso ON bloqueio (tipo_recurso, recurso_id);

CREATE TABLE movimentacao_material (
    id           BIGSERIAL PRIMARY KEY,
    reserva_id   BIGINT       NOT NULL REFERENCES reserva (id),
    material_id  BIGINT       NOT NULL REFERENCES material (id),
    tipo         VARCHAR(20)  NOT NULL CHECK (tipo IN ('RETIRADA', 'DEVOLUCAO')),
    ator_id      BIGINT       REFERENCES usuario (id),
    ocorrido_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    observacao   VARCHAR(500)
);

-- Append-only: toda mudança de estado de reserva gera exatamente um registro,
-- gravado na mesma transação (D2/ADR-004). Ordenação canônica = (ocorrido_em, id),
-- o id sequencial desempata timestamps iguais.
CREATE TABLE evento_auditoria (
    id              BIGSERIAL PRIMARY KEY,
    reserva_id      BIGINT       NOT NULL REFERENCES reserva (id),
    estado_anterior VARCHAR(20),
    estado_novo     VARCHAR(20)  NOT NULL,
    ator_id         BIGINT       REFERENCES usuario (id),
    ocorrido_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    detalhe         VARCHAR(500)
);

CREATE INDEX idx_evento_auditoria_reserva_ordem ON evento_auditoria (reserva_id, ocorrido_em, id);
