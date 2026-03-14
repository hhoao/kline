-- ----------------------------
-- Table structure for ai_api_key
-- ----------------------------
DROP TABLE IF EXISTS ai_api_key;

CREATE TABLE ai_api_key
(
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(255) NOT NULL,
    api_key     VARCHAR(255) NOT NULL,
    platform    VARCHAR(255) NOT NULL,
    url         VARCHAR(255)          DEFAULT NULL,
    status      INTEGER      NOT NULL,
    creator     VARCHAR(64)           DEFAULT '',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)           DEFAULT '',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    version     INTEGER      NOT NULL DEFAULT 1,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_api_key IS 'AI API密钥表';
COMMENT ON COLUMN ai_api_key.id IS '主键ID';
COMMENT ON COLUMN ai_api_key.name IS 'API密钥名称';
COMMENT ON COLUMN ai_api_key.api_key IS 'API密钥值';
COMMENT ON COLUMN ai_api_key.platform IS '平台名称（如：OpenAI、YiYan等）';
COMMENT ON COLUMN ai_api_key.url IS 'API地址';
COMMENT ON COLUMN ai_api_key.status IS '状态：0=禁用，1=启用';

-- ----------------------------
-- Table structure for ai_chat_conversation
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_conversation;

CREATE TABLE ai_chat_conversation
(
    id             BIGSERIAL        NOT NULL,
    user_id        BIGINT           NOT NULL,
    role_id        BIGINT                    DEFAULT NULL,
    title          VARCHAR(256)     NOT NULL,
    model_id       BIGINT           NOT NULL,
    model          VARCHAR(32)      NOT NULL,
    pinned         BOOLEAN          NOT NULL,
    pinned_time    TIMESTAMP                 DEFAULT NULL,
    system_message VARCHAR(1024)             DEFAULT NULL,
    temperature    DOUBLE PRECISION NOT NULL,
    max_tokens     INTEGER          NOT NULL,
    max_contexts   INTEGER          NOT NULL,
    creator        VARCHAR(64)               DEFAULT '',
    create_time    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        VARCHAR(64)               DEFAULT '',
    update_time    TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    version        INTEGER          NOT NULL DEFAULT 1,
    tenant_id      BIGINT                    DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_chat_conversation IS 'AI聊天会话表';

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_message;

CREATE TABLE ai_chat_message
(
    id              BIGSERIAL     NOT NULL,
    conversation_id BIGINT        NOT NULL,
    reply_id        BIGINT                 DEFAULT NULL,
    user_id         BIGINT        NOT NULL,
    role_id         BIGINT                 DEFAULT NULL,
    type            VARCHAR(16)   NOT NULL,
    model           VARCHAR(32)   NOT NULL,
    model_id        BIGINT        NOT NULL,
    content         TEXT NOT NULL,
    segment_ids     VARCHAR(256)            DEFAULT NULL,
    use_context     BOOLEAN       NOT NULL DEFAULT TRUE,
    metadata        TEXT           DEFAULT NULL,
    creator         VARCHAR(64)            DEFAULT '',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)            DEFAULT '',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    version         INTEGER       NOT NULL DEFAULT 1,
    tenant_id       BIGINT                 DEFAULT NULL,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_chat_message IS 'AI聊天消息表';

-- ----------------------------
-- Table structure for ai_chat_role
-- ----------------------------
DROP TABLE IF EXISTS ai_chat_role;

CREATE TABLE ai_chat_role
(
    id             BIGSERIAL    NOT NULL,
    user_id        BIGINT                DEFAULT NULL,
    model_id       BIGINT                DEFAULT NULL,
    name           VARCHAR(128) NOT NULL,
    avatar         VARCHAR(256) NOT NULL,
    category       VARCHAR(32)           DEFAULT NULL,
    sort           INTEGER      NOT NULL DEFAULT 0,
    description    VARCHAR(256) NOT NULL,
    system_message VARCHAR(1024)         DEFAULT NULL,
    knowledge_ids  VARCHAR(255)          DEFAULT NULL,
    tool_ids       VARCHAR(255)          DEFAULT NULL,
    public_status  BOOLEAN      NOT NULL,
    status         INTEGER               DEFAULT NULL,
    creator        VARCHAR(64)           DEFAULT '',
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        VARCHAR(64)           DEFAULT '',
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    version        INTEGER      NOT NULL DEFAULT 1,
    tenant_id      BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_chat_role IS 'AI聊天角色表';

-- ----------------------------
-- Table structure for ai_knowledge
-- ----------------------------
DROP TABLE IF EXISTS ai_knowledge;

CREATE TABLE ai_knowledge
(
    id                   BIGSERIAL    NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    description          VARCHAR(1024),
    embedding_model_id   BIGINT,
    embedding_model      VARCHAR(255),
    top_k                INTEGER,
    similarity_threshold  DOUBLE PRECISION,
    status               INTEGER,
    creator              VARCHAR(64)           DEFAULT '',
    create_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              VARCHAR(64)           DEFAULT '',
    update_time          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version              INTEGER      NOT NULL DEFAULT 1,
    tenant_id            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_knowledge IS 'AI知识库表';

-- ----------------------------
-- Table structure for ai_model
-- ----------------------------
DROP TABLE IF EXISTS ai_model;

CREATE TABLE ai_model
(
    id           BIGSERIAL    NOT NULL,
    key_id       BIGINT,
    name         VARCHAR(255) NOT NULL,
    model        VARCHAR(255) NOT NULL,
    platform     VARCHAR(255) NOT NULL,
    type         INTEGER,
    sort         INTEGER,
    status       INTEGER,
    temperature  DOUBLE PRECISION,
    max_tokens   INTEGER,
    max_contexts INTEGER,
    creator      VARCHAR(64)           DEFAULT '',
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      VARCHAR(64)           DEFAULT '',
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version      INTEGER      NOT NULL DEFAULT 1,
    tenant_id    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_model IS 'AI模型表';

-- ----------------------------
-- Table structure for ai_tool
-- ----------------------------
DROP TABLE IF EXISTS ai_tool;

CREATE TABLE ai_tool
(
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512)          DEFAULT NULL,
    status      INTEGER      NOT NULL DEFAULT 0,
    creator     VARCHAR(64)           DEFAULT '',
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     VARCHAR(64)           DEFAULT '',
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    version     INTEGER      NOT NULL DEFAULT 1,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_tool IS 'AI工具表';

-- ----------------------------
-- Table structure for ai_knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS ai_knowledge_document;

CREATE TABLE ai_knowledge_document
(
    id                 BIGSERIAL    NOT NULL,
    knowledge_id       BIGINT       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    url                VARCHAR(1024)         DEFAULT NULL,
    content            TEXT,
    content_length     INTEGER               DEFAULT NULL,
    tokens             INTEGER               DEFAULT NULL,
    segment_max_tokens INTEGER               DEFAULT NULL,
    retrieval_count    INTEGER               DEFAULT 0,
    status             INTEGER      NOT NULL DEFAULT 1,
    creator            VARCHAR(64)           DEFAULT '',
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            VARCHAR(64)           DEFAULT '',
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    version            INTEGER      NOT NULL DEFAULT 1,
    tenant_id          BIGINT                DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_knowledge_document IS 'AI知识库文档表';
CREATE INDEX idx_ai_knowledge_document_knowledge_id ON ai_knowledge_document (knowledge_id);

-- ----------------------------
-- Table structure for ai_knowledge_segment
-- ----------------------------
DROP TABLE IF EXISTS ai_knowledge_segment;

CREATE TABLE ai_knowledge_segment
(
    id              BIGSERIAL NOT NULL,
    knowledge_id    BIGINT    NOT NULL,
    document_id     BIGINT    NOT NULL,
    content         TEXT,
    content_length  INTEGER            DEFAULT NULL,
    vector_id       VARCHAR(255)       DEFAULT NULL,
    tokens          INTEGER            DEFAULT NULL,
    retrieval_count INTEGER            DEFAULT 0,
    status          INTEGER            DEFAULT NULL,
    creator         VARCHAR(64)        DEFAULT '',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         VARCHAR(64)        DEFAULT '',
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    version         INTEGER   NOT NULL DEFAULT 1,
    tenant_id       BIGINT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

COMMENT ON TABLE ai_knowledge_segment IS 'AI知识库分片表';
CREATE INDEX idx_ai_knowledge_segment_knowledge_id ON ai_knowledge_segment (knowledge_id);
CREATE INDEX idx_ai_knowledge_segment_document_id ON ai_knowledge_segment (document_id);


-- ----------------------------
-- Table structure for infra_api_error_log
-- ----------------------------
DROP TABLE IF EXISTS infra_api_error_log;

CREATE TABLE infra_api_error_log
(
    id                           BIGSERIAL     NOT NULL,
    trace_id                     varchar(64)   NOT NULL,
    user_id                      BIGINT          NOT NULL DEFAULT 0,
    application_name             varchar(50)   NOT NULL,
    request_method               varchar(16)   NOT NULL,
    request_url                  varchar(255)  NOT NULL,
    request_params               varchar(8000) NOT NULL,
    user_ip                      varchar(50)   NOT NULL,
    user_agent                   varchar(512)  NOT NULL,
    exception_time               timestamp     NOT NULL,
    exception_name               varchar(128)  NOT NULL DEFAULT '',
    exception_message            text          NOT NULL,
    exception_root_cause_message text          NOT NULL,
    exception_stack_trace        text          NOT NULL,
    exception_class_name         varchar(512)  NOT NULL,
    exception_file_name          varchar(512)  NOT NULL,
    exception_method_name        varchar(512)  NOT NULL,
    exception_line_number        int4          NOT NULL,
    process_status               int2          NOT NULL,
    process_time                 timestamp     NULL     DEFAULT NULL,
    process_user_id              int8          NULL     DEFAULT 0,
    creator                      varchar(64)   NULL     DEFAULT '',
    create_time                  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                      varchar(64)   NULL     DEFAULT '',
    update_time                  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                      int2          NOT NULL DEFAULT 0,
    tenant_id                    int8          NOT NULL DEFAULT 0
);

ALTER TABLE infra_api_error_log
    ADD CONSTRAINT pk_infra_api_error_log PRIMARY KEY (id);

COMMENT ON COLUMN infra_api_error_log.id IS '编号';
COMMENT ON COLUMN infra_api_error_log.trace_id IS '链路追踪编号
     *
     * 一般来说，通过链路追踪编号，可以将访问日志，错误日志，链路追踪日志，logger 打印日志等，结合在一起，从而进行排错。';
COMMENT ON COLUMN infra_api_error_log.user_id IS '用户编号';
COMMENT ON COLUMN infra_api_error_log.application_name IS '应用名
     *
     * 目前读取 spring.application.name';
COMMENT ON COLUMN infra_api_error_log.request_method IS '请求方法名';
COMMENT ON COLUMN infra_api_error_log.request_url IS '请求地址';
COMMENT ON COLUMN infra_api_error_log.request_params IS '请求参数';
COMMENT ON COLUMN infra_api_error_log.user_ip IS '用户 IP';
COMMENT ON COLUMN infra_api_error_log.user_agent IS '浏览器 UA';
COMMENT ON COLUMN infra_api_error_log.exception_time IS '异常发生时间';
COMMENT ON COLUMN infra_api_error_log.exception_name IS '异常名
     *
     * {@link Throwable#getClass()} 的类全名';
COMMENT ON COLUMN infra_api_error_log.exception_message IS '异常导致的消息
     *
     * {@link cn.iocoder.common.framework.util.ExceptionUtil#getMessage(Throwable)}';
COMMENT ON COLUMN infra_api_error_log.exception_root_cause_message IS '异常导致的根消息
     *
     * {@link cn.iocoder.common.framework.util.ExceptionUtil#getRootCauseMessage(Throwable)}';
COMMENT ON COLUMN infra_api_error_log.exception_stack_trace IS '异常的栈轨迹
     *
     * {@link cn.iocoder.common.framework.util.ExceptionUtil#getServiceException(Exception)}';
COMMENT ON COLUMN infra_api_error_log.exception_class_name IS '异常发生的类全名
     *
     * {@link StackTraceElement#getClassName()}';
COMMENT ON COLUMN infra_api_error_log.exception_file_name IS '异常发生的类文件
     *
     * {@link StackTraceElement#getFileName()}';
COMMENT ON COLUMN infra_api_error_log.exception_method_name IS '异常发生的方法名
     *
     * {@link StackTraceElement#getMethodName()}';
COMMENT ON COLUMN infra_api_error_log.exception_line_number IS '异常发生的方法所在行
     *
     * {@link StackTraceElement#getLineNumber()}';
COMMENT ON COLUMN infra_api_error_log.process_status IS '处理状态';
COMMENT ON COLUMN infra_api_error_log.process_time IS '处理时间';
COMMENT ON COLUMN infra_api_error_log.process_user_id IS '处理用户编号';
COMMENT ON COLUMN infra_api_error_log.creator IS '创建者';
COMMENT ON COLUMN infra_api_error_log.create_time IS '创建时间';
COMMENT ON COLUMN infra_api_error_log.updater IS '更新者';
COMMENT ON COLUMN infra_api_error_log.update_time IS '更新时间';
COMMENT ON COLUMN infra_api_error_log.deleted IS '是否删除';
COMMENT ON COLUMN infra_api_error_log.tenant_id IS '租户编号';
COMMENT ON TABLE infra_api_error_log IS '系统异常日志';

DROP SEQUENCE IF EXISTS infra_api_error_log_seq;
CREATE SEQUENCE infra_api_error_log_seq
    START 1;


-- ----------------------------
-- Table structure for infra_api_access_log
-- ----------------------------
DROP TABLE IF EXISTS infra_api_access_log;
CREATE TABLE infra_api_access_log
(
    id               BIGSERIAL         NOT NULL,
    trace_id         varchar(64)  NOT NULL DEFAULT '',
    user_id          BIGINT         NOT NULL DEFAULT 0,
    application_name varchar(50)  NOT NULL,
    request_method   varchar(16)  NOT NULL DEFAULT '',
    request_url      varchar(255) NOT NULL DEFAULT '',
    request_params   text         NULL,
    response_body    text         NULL,
    user_ip          varchar(50)  NOT NULL,
    user_agent       varchar(512) NOT NULL,
    operate_module   varchar(50)  NULL     DEFAULT NULL,
    operate_name     varchar(50)  NULL     DEFAULT NULL,
    operate_type     int2         NULL     DEFAULT 0,
    begin_time       timestamp    NOT NULL,
    end_time         timestamp    NOT NULL,
    duration         int4         NOT NULL,
    result_code      int4         NOT NULL DEFAULT 0,
    result_msg       varchar(512) NULL     DEFAULT '',
    creator          varchar(64)  NULL     DEFAULT '',
    create_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater          varchar(64)  NULL     DEFAULT '',
    update_time      timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          int2         NOT NULL DEFAULT 0,
    tenant_id        int8         NOT NULL DEFAULT 0
);

ALTER TABLE infra_api_access_log
    ADD CONSTRAINT pk_infra_api_access_log PRIMARY KEY (id);

CREATE INDEX idx_infra_api_access_log_01 ON infra_api_access_log (create_time);

COMMENT ON COLUMN infra_api_access_log.id IS '日志主键';
COMMENT ON COLUMN infra_api_access_log.trace_id IS '链路追踪编号';
COMMENT ON COLUMN infra_api_access_log.user_id IS '用户编号';
COMMENT ON COLUMN infra_api_access_log.application_name IS '应用名';
COMMENT ON COLUMN infra_api_access_log.request_method IS '请求方法名';
COMMENT ON COLUMN infra_api_access_log.request_url IS '请求地址';
COMMENT ON COLUMN infra_api_access_log.request_params IS '请求参数';
COMMENT ON COLUMN infra_api_access_log.response_body IS '响应结果';
COMMENT ON COLUMN infra_api_access_log.user_ip IS '用户 IP';
COMMENT ON COLUMN infra_api_access_log.user_agent IS '浏览器 UA';
COMMENT ON COLUMN infra_api_access_log.operate_module IS '操作模块';
COMMENT ON COLUMN infra_api_access_log.operate_name IS '操作名';
COMMENT ON COLUMN infra_api_access_log.operate_type IS '操作分类';
COMMENT ON COLUMN infra_api_access_log.begin_time IS '开始请求时间';
COMMENT ON COLUMN infra_api_access_log.end_time IS '结束请求时间';
COMMENT ON COLUMN infra_api_access_log.duration IS '执行时长';
COMMENT ON COLUMN infra_api_access_log.result_code IS '结果码';
COMMENT ON COLUMN infra_api_access_log.result_msg IS '结果提示';
COMMENT ON COLUMN infra_api_access_log.creator IS '创建者';
COMMENT ON COLUMN infra_api_access_log.create_time IS '创建时间';
COMMENT ON COLUMN infra_api_access_log.updater IS '更新者';
COMMENT ON COLUMN infra_api_access_log.update_time IS '更新时间';
COMMENT ON COLUMN infra_api_access_log.deleted IS '是否删除';
COMMENT ON COLUMN infra_api_access_log.tenant_id IS '租户编号';
COMMENT ON TABLE infra_api_access_log IS 'API 访问日志表';