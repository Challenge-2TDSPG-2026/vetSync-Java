ALTER TABLE TB_TUTOR ADD (
    nr_cep          VARCHAR2(8),
    ds_logradouro   VARCHAR2(150),
    nr_endereco     VARCHAR2(20),
    ds_complemento  VARCHAR2(100),
    ds_bairro       VARCHAR2(100),
    nm_cidade       VARCHAR2(100),
    sg_uf           CHAR(2)
);
