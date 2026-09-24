-- Foto do pet (perfil e carteirinha). Use o próximo número livre da pasta db/migration.
ALTER TABLE TB_PET ADD (
    ds_foto      BLOB,
    ds_foto_tipo VARCHAR2(100)
);