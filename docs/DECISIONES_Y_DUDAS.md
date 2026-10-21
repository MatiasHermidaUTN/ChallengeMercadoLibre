# Decisiones y dudas — fix del conteo de incidentes resueltos (Enigma)

> Versión 1: escrita **antes** de hablar con Laura (PO). Todo lo marcado como supuesto debe confirmarse.

## 1. Diagnóstico (hipótesis)
El dashboard muestra más "resueltos" de los reales y aparecen ítems descartados en triaje.
Hipótesis más probable: la consulta cuenta como resuelto todo incidente "cerrado"
(`closed_at IS NOT NULL`) sin distinguir el motivo del cierre, y los descartados en triaje también se cierran.
Ver `sql/resolved_incidents_before.sql` (hipótesis) y `sql/resolved_incidents_after.sql` (fix).
No tengo acceso al código ni al esquema real de Enigma, por eso el esquema es un supuesto.

## 2. Decisiones
| # | Decisión | Motivo |
|---|----------|--------|
| D1 | Contar solo `status = 'resolved'` | Es la definición de negocio más directa de "resuelto". |
| D2 | Agrupar por semana de `resolved_at`, no de `closed_at` | La fecha de resolución es la que importa al SLA. |
| D3 | Endpoint `GET /api/v1/incidents/resolved/weekly?from=&to=` (solo lectura, sin romper contrato) | Mantener el contrato; solo cambia el cálculo. |
| D4 | `organization_id` sale del usuario autenticado, nunca del query | Evitar fuga entre organizaciones. |
| D5 | `to` exclusivo, `from` inclusive | Evita contar dos veces en los bordes. |
| D6 | Node + `node:test`, sin dependencias externas | Simple de leer; no necesita instalar nada. |
| D7 | Fix detrás de un cambio chico de SQL, sin tocar datos históricos | Reversible; no se reescribe nada. |

## 3. Casos de prueba (ver `test/`)
- La consulta filtra por `resolved` y por organización.
- Parámetros inválidos / rango invertido -> 400.
- Conteo se devuelve como número; el `organization_id` viaja a la consulta.
- El `organization_id` del query se ignora.
Pendiente (necesitan DB real): incidente `discarded` con `closed_at` no suma; `resolved` y luego reabierto; borde de semana.

## 4. Estrategia de Git
- Rama `feature/fix_resolved_incidents_count` desde `main` -> PR con 1 review y CI verde.
- Commits chicos: SQL, handler, tests, docs. Sin tocar migraciones ya aplicadas.
- Despliegue gradual (feature flag o endpoint v2 en paralelo), comparar números nuevos vs. conteo manual de guardia antes de apagar el viejo.
- Rollback: `git revert` del merge; no hay cambios de datos.
- Si hace falta un índice, va en una migración separada.

## 5. Dudas para Laura
1. ¿Qué estados existen y cuál(es) cuentan como "resuelto"? (¿`closed`, `done`, `resolved`?)
2. ¿Los descartados en triaje tienen estado propio o solo un motivo de cierre?
3. ¿Un incidente reabierto cuenta como resuelto? ¿Y si se resuelve dos veces?
4. ¿La semana empieza lunes? ¿Qué zona horaria usan los reportes?
5. ¿Qué fecha manda para el SLA: resolución, cierre o creación?
6. ¿Hay que corregir los históricos o solo el cálculo hacia adelante?
7. ¿Qué otros consumidores usan este conteo (tableros, reportes, exportaciones)?
8. ¿Hay un stack/convención obligatoria (lenguaje, framework, versionado de API)?
9. ¿Cuál es el volumen de datos aproximado (para decidir índices)?
10. ¿Cómo se valida contra el conteo de guardia y qué diferencia es aceptable?
