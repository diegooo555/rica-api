-- Bloqueo semántico de la Saga de eliminación: mientras un investigador está EN_ELIMINACION
-- no acepta publicaciones nuevas, pero todavía no se ha borrado.
ALTER TABLE investigadores ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';
