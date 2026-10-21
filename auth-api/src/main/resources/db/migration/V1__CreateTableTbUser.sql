CREATE TABLE tb_user
(
    id       SERIAL       NOT NULL,
    name     VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    type     INTEGER      NOT NULL, -- 0 = Admin, 1 = Common
    CONSTRAINT pk_tb_user PRIMARY KEY (id),
    CONSTRAINT uk_tb_user_email UNIQUE (email)
);
