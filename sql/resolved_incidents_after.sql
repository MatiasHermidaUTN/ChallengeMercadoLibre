-- DESPUÉS: solo cuentan los incidentes con estado 'resolved'.
-- Los 'discarded' (descartados en triaje) quedan afuera.
-- Parámetros: $1 = from (inclusive), $2 = to (exclusivo), $3 = organization_id
SELECT date_trunc('week', resolved_at) AS week,
       COUNT(*)                        AS resolved_count
FROM incident
WHERE organization_id = $3
  AND status = 'resolved'
  AND resolved_at IS NOT NULL
  AND resolved_at >= $1
  AND resolved_at <  $2
GROUP BY 1
ORDER BY 1;

-- Índice sugerido para la consulta (migración aparte):
-- CREATE INDEX CONCURRENTLY idx_incident_org_status_resolved_at
--   ON incident (organization_id, status, resolved_at);
