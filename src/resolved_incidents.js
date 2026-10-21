import { readFileSync } from 'node:fs';

const QUERY = readFileSync(new URL('../sql/resolved_incidents_after.sql', import.meta.url), 'utf8');

// Valida y normaliza los parámetros de GET /api/v1/incidents/resolved/weekly
export function parseParams(query) {
  const from = new Date(query.from);
  const to = new Date(query.to);
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime())) {
    return { error: 'from y to deben ser fechas ISO-8601 válidas' };
  }
  if (from >= to) {
    return { error: 'from debe ser anterior a to' };
  }
  return { from, to };
}

// db: cualquier objeto con query(sql, params) -> { rows } (por ej. un pg.Pool)
export async function getResolvedByWeek(db, { from, to, organizationId }) {
  const { rows } = await db.query(QUERY, [from, to, organizationId]);
  return rows.map((r) => ({ week: r.week, resolvedCount: Number(r.resolved_count) }));
}

// Handler estilo Express: (req, res)
export function resolvedWeeklyHandler(db) {
  return async (req, res) => {
    const parsed = parseParams(req.query);
    if (parsed.error) {
      return res.status(400).json({ error: { code: 'invalid_params', message: parsed.error } });
    }
    const data = await getResolvedByWeek(db, { ...parsed, organizationId: req.user.organizationId });
    return res.status(200).json({ data });
  };
}
