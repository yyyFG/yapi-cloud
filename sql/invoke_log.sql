CREATE TABLE invoke_log (
    id BIGINT NOT NULL COMMENT '主键',
    userId BIGINT NOT NULL COMMENT '调用用户',
    interfaceId BIGINT NOT NULL COMMENT '被调接口',
    requestTime DATETIME NOT NULL COMMENT '调用时间',
    success TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功 0-失败 1-成功',
    errorMsg VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    isDelete TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id)
) COMMENT '接口调用日志';