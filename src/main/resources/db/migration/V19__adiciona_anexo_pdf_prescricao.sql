-- V19__adiciona_anexo_pdf_prescricao.sql
ALTER TABLE TB_PRESCRICAO ADD (
    ds_anexo_pdf      BLOB,
    ds_anexo_pdf_nome VARCHAR2(255),
    ds_anexo_pdf_tipo VARCHAR2(100)
);