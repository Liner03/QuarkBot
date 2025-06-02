create table duanju
(
    id          int auto_increment
        primary key,
    name        varchar(512)                        null,
    url         varchar(255)                        null,
    create_time timestamp default CURRENT_TIMESTAMP null,
    constraint duanju_name
        unique (name),
    constraint duanju_url
        unique (url)
);

create fulltext index idx_name_ngram
    on quark_bot.duanju (name);

create table quark_bot.lin_category
(
    id          int auto_increment
        primary key,
    name        varchar(512)            not null,
    description text                    null,
    url         varchar(255)            not null,
    password    varchar(128) default '' null,
    valid       tinyint(1)   default 1  not null comment '是否有效',
    ending      tinyint(1)   default 1  not null comment '是否完结'
)
    row_format = DYNAMIC;

create index idx_name
    on quark_bot.lin_category (name);

create table quark_bot.process_log
(
    id         int auto_increment
        primary key,
    task_id    int                                 not null,
    status     varchar(20)                         null,
    response   text                                null,
    created_at timestamp default CURRENT_TIMESTAMP null
);

create table quark_bot.sent_posts
(
    id            int auto_increment
        primary key,
    platform_url  varchar(255)                        not null,
    discussion_id varchar(255)                        not null,
    post_id       varchar(255)                        not null,
    duanju_id     int                                 not null,
    title         varchar(255)                        null,
    created_at    timestamp default CURRENT_TIMESTAMP null
);

create index idx_platform_created
    on quark_bot.sent_posts (platform_url, created_at);

create table quark_bot.task_queue
(
    id                int auto_increment
        primary key,
    record_id         int                                                                             not null comment '关联的影片记录ID（对应duanju表id）',
    status            enum ('pending', 'processing', 'completed', 'failed') default 'pending'         not null,
    processed_at      datetime                                                                        null comment '开始处理时间',
    end_process       datetime                                                                        null comment '结束处理时间',
    retry_count       int                                                   default 0                 not null comment '重试次数',
    error_info        varchar(255)                                                                    null comment '错误信息',
    result            varchar(255)                                                                    null comment '处理结果',
    created_at        datetime                                              default CURRENT_TIMESTAMP not null comment '任务创建时间',
    platform_statuses json                                                                            null,
    constraint task_queue_ibfk_1
        foreign key (record_id) references quark_bot.duanju (id)
            on delete cascade
);

create table quark_bot.task_platform_status
(
    id           int auto_increment
        primary key,
    task_id      int                                                                     not null,
    platform_url varchar(255)                                                            not null,
    status       enum ('pending', 'processing', 'completed', 'failed') default 'pending' null,
    started_at   datetime                                                                null,
    completed_at datetime                                                                null,
    result       text                                                                    null,
    error_info   text                                                                    null,
    constraint task_platform_status_ibfk_1
        foreign key (task_id) references quark_bot.task_queue (id)
            on delete cascade
);

create index idx_task_platform
    on quark_bot.task_platform_status (task_id, platform_url);

create index idx_processed
    on quark_bot.task_queue (processed_at);

create index idx_status
    on quark_bot.task_queue (status);

create index record_id
    on quark_bot.task_queue (record_id);

create table quark_bot.task_site_status
(
    id            int auto_increment
        primary key,
    task_id       int                                                                     not null comment '关联的任务ID（task_queue.id）',
    site_url      varchar(255)                                                            not null comment '站点URL',
    status        enum ('pending', 'processing', 'completed', 'failed') default 'pending' not null,
    publish_time  datetime                                                                null comment '发布时间',
    error_info    varchar(255)                                                            null comment '错误信息',
    discussion_id varchar(255)                                                            null comment '讨论ID',
    constraint fk_task_queue
        foreign key (task_id) references quark_bot.task_queue (id)
            on delete cascade
);

create index idx_task_site
    on quark_bot.task_site_status (task_id, site_url);

create table quark_bot.update_list
(
    id          int auto_increment
        primary key,
    name        varchar(512)                         null comment '名字',
    tree        text                                 null comment '上次的tree',
    url         varchar(255)                         null comment '链接',
    share       varchar(255)                         null comment '分享链接',
    ending      tinyint(1) default 0                 null comment '是否完结 1 完结 0 未完结',
    create_time timestamp  default CURRENT_TIMESTAMP null comment '创建时间',
    update_time timestamp  default CURRENT_TIMESTAMP null comment '更新时间'
)
    row_format = DYNAMIC;

create table quark_bot.user_setting
(
    id                  tinyint auto_increment
        primary key,
    wxid                varchar(32)                                          null comment '微信id',
    nick_name           varchar(32)                                          null comment '微信昵称',
    admin_email         varchar(32)                                          null comment '通知邮箱',
    admin_wxid          varchar(32)                                          null comment '管理员微信id',
    auth_password       varchar(32)                                          null comment '管理员认证密码',
    auto_accept_friends tinyint(1)                                           null comment '是否自动同意添加好友',
    add_welcome_content varchar(2048)                                        null comment '添加好友欢迎语',
    match_keywords      varchar(128) default '进群'                          not null comment '匹配进群关键词,支持正则表达式',
    invite_group_name   varchar(1024)                                        null comment '自动邀请群聊列表,以英文,隔开',
    active_group        varchar(1024)                                        null comment '机器人生效群组,以英文,隔开',
    search_keywords     varchar(128) default '^(搜短剧|搜索|搜剧|搜)s*(.+)$' null comment '搜索关键词,支持正则表达式',
    cookie              varchar(2048)                                        null comment '夸克网盘COOKIE',
    cookie_valid        tinyint(1)   default 1                               null comment 'cookie有效性',
    save_path_fid       varchar(32)  default '0'                             not null comment '转存文件夹fid,默认为0(根目录)',
    appid               varchar(32)                                          null comment 'appId',
    uuid                varchar(32)                                          null comment 'uuid',
    token               varchar(32)                                          null comment 'X-GEWE-TOKEN',
    remind_msg          tinyint(1)   default 0                               null comment '是否开启提醒',
    chatrooms           varchar(2048)                                        null comment '群组列表',
    constraint wxid
        unique (wxid)
);

