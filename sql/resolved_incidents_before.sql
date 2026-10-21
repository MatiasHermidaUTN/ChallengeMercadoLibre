-- ANTES (hipótesis del bug, a confirmar contra el código real de Enigma).
-- Se cuenta todo incidente que tenga closed_at, sin mirar el motivo del cierre.
-- Los descartados en triaje también se "cierran", por eso inflan el número.
SELECT date_trunc('week', closed_at) AS week,
       COUNT(*)                      AS resolved_count
FROM incident
WHERE closed_at IS NOT NULL
GROUP BY 1
ORDER BY 1;
