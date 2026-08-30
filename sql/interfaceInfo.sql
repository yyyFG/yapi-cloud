DROP table if exists `interface_info`;
-- 接口信息
create table if not exists `interface_info`
(
    `id` bigint not null auto_increment comment '主键' primary key,
    `interfaceName` varchar(256) not null comment '名称',
    `description` varchar(256) null comment '描述',
    `url` varchar(512) not null comment '接口地址',
    `path` varchar(256) NOT NULL COMMENT '对外调用路径（平台自动生成，唯一）',
    `requestHeader` text null comment '请求头',
    `requestParams` text null comment '请求参数',
    `responseHeader` text null comment '响应头',
    `status` tinyint default 0 not null comment '接口状态（0-关闭，1-发布。2-管理员下架）',
    `method` varchar(16) not null comment '请求类型',
    `userId` bigint not null comment '创建人',
    `createTime` datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `updateTime` datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    `isDelete` tinyint default 0 not null comment '是否删除(0-未删, 1-已删)',
    key `idx_user_status_delete` (`userId`, `status`, `isDelete`),
    key `idx_status_delete` (`status`, `isDelete`),
    unique key `uk_user_method_url_delete` (`userId`, `method`, `url`, `isDelete`),
    UNIQUE KEY uk_path (path)
) comment '接口信息';