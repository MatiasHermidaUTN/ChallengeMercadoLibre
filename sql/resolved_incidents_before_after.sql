-- ANTES (confirmado por Laura, PO): solo filtraba resolved_at.
SELECT id, title, status, resolution_code, resolved_at
FROM incidents
WHERE resolved_at BETWEEN :from AND :to;

-- DESPUES: agrega la regla de negocio (RESOLVED/CLOSED y resolution_code no CANCELLED/DUPLICATE).
SELECT id, title, status, resolution_code, resolved_at
FROM incidents
WHERE resolved_at BETWEEN :from AND :to
  AND status IN ('RESOLVED', 'CLOSED')
  AND (resolution_code IS NULL OR resolution_code NOT IN ('CANCELLED', 'DUPLICATE'))
ORDER BY resolved_at, id;

-- Opcional, en una migracion aparte, si el volumen lo justifica:
-- CREATE INDEX CONCURRENTLY idx_incidents_resolved_at ON incidents (resolved_at);
