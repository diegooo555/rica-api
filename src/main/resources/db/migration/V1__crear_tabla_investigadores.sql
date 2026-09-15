CREATE TABLE investigadores (
    id                     BIGSERIAL PRIMARY KEY,
    nombre_completo        VARCHAR(150) NOT NULL,
    correo_institucional   VARCHAR(150) NOT NULL,
    grupo_investigacion    VARCHAR(150),
    CONSTRAINT uk_investigadores_correo UNIQUE (correo_institucional)
);