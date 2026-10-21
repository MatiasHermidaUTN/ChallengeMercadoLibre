import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseParams, getResolvedByWeek, resolvedWeeklyHandler } from '../src/resolved_incidents.js';

const fakeRes = () => {
  const res = { code: null, body: null };
  res.status = (c) => { res.code = c; return res; };
  res.json = (b) => { res.body = b; return res; };
  return res;
};

test('la consulta filtra por status resolved y por organización', () => {
  const sql = readFileSync(new URL('../sql/resolved_incidents_after.sql', import.meta.url), 'utf8');
  assert.match(sql, /status\s*=\s*'resolved'/);
  assert.match(sql, /organization_id\s*=\s*\$3/);
});

test('parseParams rechaza fechas inválidas y rangos invertidos', () => {
  assert.ok(parseParams({ from: 'x', to: '2025-01-01' }).error);
  assert.ok(parseParams({ from: '2025-02-01', to: '2025-01-01' }).error);
  assert.ok(parseParams({ from: '2025-01-01', to: '2025-02-01' }).from instanceof Date);
});

test('getResolvedByWeek pasa organization_id y convierte el conteo a número', async () => {
  let received;
  const db = { query: async (sql, params) => { received = params; return { rows: [{ week: '2025-01-06', resolved_count: '3' }] }; } };
  const out = await getResolvedByWeek(db, { from: new Date('2025-01-01'), to: new Date('2025-02-01'), organizationId: 'org-a' });
  assert.equal(received[2], 'org-a');
  assert.deepEqual(out, [{ week: '2025-01-06', resolvedCount: 3 }]);
});

test('handler: 400 con parámetros inválidos, 200 con válidos', async () => {
  const db = { query: async () => ({ rows: [] }) };
  const h = resolvedWeeklyHandler(db);
  const bad = fakeRes();
  await h({ query: { from: 'x', to: 'y' }, user: { organizationId: 'a' } }, bad);
  assert.equal(bad.code, 400);
  const ok = fakeRes();
  await h({ query: { from: '2025-01-01', to: '2025-02-01' }, user: { organizationId: 'a' } }, ok);
  assert.equal(ok.code, 200);
  assert.deepEqual(ok.body, { data: [] });
});

test('handler: el organization_id sale del usuario autenticado, no del query', async () => {
  let params;
  const db = { query: async (_s, p) => { params = p; return { rows: [] }; } };
  await resolvedWeeklyHandler(db)(
    { query: { from: '2025-01-01', to: '2025-02-01', organizationId: 'org-b' }, user: { organizationId: 'org-a' } },
    fakeRes(),
  );
  assert.equal(params[2], 'org-a');
});
