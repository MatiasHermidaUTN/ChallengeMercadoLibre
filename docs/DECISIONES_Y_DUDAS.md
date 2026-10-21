# Decisiones y dudas — fix del conteo de incidentes resueltos (Enigma)

Historial: la **v1** (commit `d0b4617`) se escribió *antes* de hablar con Laura (PO), con supuestos propios
(Node, otro esquema). La **v2** (esta) se ajusta con lo que Laura respondió.

## 1. Diagnóstico
El endpoint `GET /api/v1/incidents/resolved` solo filtra `resolved_at BETWEEN :from AND :to`.
Un ticket cancelado o duplicado también tiene `resolved_at`, por eso entran al reporte y lo inflan.
Falta aplicar la regla de negocio: estado RESOLVED o CLOSED **y** `resolution_code` distinto de CANCELLED y DUPLICATE.

## 2. Dudas que tenía antes de hablar con Laura (v1)
1. ¿Qué estados existen y cuáles cuentan como resuelto?
2. ¿Los descartados en triaje tienen estado propio o solo un código?
3. `resolution_code` NULL, ¿cuenta?
4. ¿Qué fecha y zona horaria manda para la semana?
5. ¿Qué pasa con reabiertos?
6. ¿Se corrigen históricos?
7. ¿Contrato del endpoint, stack y convención de Git?

## 3. Respuestas de Laura
| Duda | Respuesta |
|---|---|
| 1 y 2 | Resuelto = estado `RESOLVED` o `CLOSED` y `resolution_code` distinto de `CANCELLED` y `DUPLICATE`. No supo decir si existen otros estados ni si "descartado" es un estado. |
| 3 | `resolution_code` NULL: no sabía, pero la regla de soporte es solo "no CANCELLED ni DUPLICATE"; aprobó que cuente como resuelto y quede documentado. |
| 4 y 5 | No tenía el detalle (semana, zona horaria, reabiertos). Quedan abiertas. |
| 6 | El impacto se ve en los totales del último sprint de ese endpoint: hay que devolver los datos correctos al consultar ahí. No se tocan datos históricos. |
| 7 | Endpoint actual `GET /api/v1/incidents/resolved?from=YYYY-MM-DD&to=YYYY-MM-DD` con `SELECT id, title, status, resolution_code, resolved_at FROM incidents WHERE resolved_at BETWEEN :from AND :to`. Hay que mantener el contrato de respuesta. Backend Spring Boot + PostgreSQL. GitFlow simplificado (`main`, `develop`, `feature/*`), **sin squash** al mergear. |

## 4. Cambios hechos a partir de las respuestas
- Se descartó la solución Node de la v1; ahora es Spring Boot 3 / Java 17 (JdbcTemplate + SQL explícito).
- Se descartó el endpoint semanal inventado en la v1: se corrige el endpoint existente sin cambiar parámetros ni campos.
- La consulta agrega `status IN ('RESOLVED','CLOSED')` y `resolution_code` no CANCELLED/DUPLICATE (NULL cuenta).
- Sin cambios de datos ni de esquema: es reversible con un `git revert`.

## 5. Decisiones
| # | Decisión | Motivo |
|---|---|---|
| D1 | Cambio mínimo: solo se agrega el filtro de negocio | Menor riesgo para los dashboards. |
| D2 | NULL en `resolution_code` cuenta como resuelto | Laura lo aprobó; la regla solo excluye CANCELLED y DUPLICATE. |
| D3 | Se mantiene `BETWEEN :from AND :to` con fechas a las 00:00 UTC | No cambiar semántica sin consultar (ver duda abierta A). |
| D4 | Se agrega `ORDER BY resolved_at, id` | Resultado determinístico; no cambia el contenido. |
| D5 | 400 si faltan parámetros, la fecha es inválida o `from > to` | Mensajes claros; no pude confirmar el comportamiento actual. |
| D6 | Tests con H2 en modo PostgreSQL | Corren sin infraestructura; ideal sería Testcontainers con PostgreSQL real. |

## 6. Dudas que siguen abiertas (para confirmar con el equipo)
A. `BETWEEN :to` con fecha sin hora excluye el último día completo (solo incluye hasta las 00:00). ¿Es intencional? Probable bug aparte; no lo toqué.
B. Zona horaria de `resolved_at` y de los reportes (asumí UTC).
C. Incidentes reabiertos: ¿cuentan? Hoy cuentan según su estado actual.
D. ¿Existen otros estados/códigos (p. ej. un estado de descarte en triaje)? Si hubiera uno, la lista blanca de estados ya lo excluye.
E. ¿Otros consumidores del endpoint además de los dashboards?
F. Índice sobre `resolved_at`, según volumen (sugerido en `sql/`, en migración aparte).

## 7. Casos de prueba (`src/test`)
- Devuelve solo RESOLVED/CLOSED sin CANCELLED ni DUPLICATE (incluye estado OPEN excluido).
- `resolution_code` NULL cuenta como resuelto.
- Respeta el rango de fechas y devuelve vacío si no hay coincidencias.
- Contrato JSON sin cambios (`id`, `title`, `status`, `resolutionCode`, `resolvedAt`).
- 400 con fecha inválida, `from > to` o parámetros faltantes.
Pendiente: prueba contra PostgreSQL real (Testcontainers) y casos de borde de zona horaria.

## 8. Estrategia de Git (GitFlow simplificado del equipo, sin squash)
1. `feature/spring_boot_resolved_incidents_fix` sale de `develop`.
2. PR a `develop` con al menos un review y CI verde; merge con `--no-ff` (sin squash).
3. Se despliega `develop` a un ambiente de prueba y se compara el conteo contra el de guardia del último sprint.
4. Luego `develop` -> `main` (merge `--no-ff` + tag, p. ej. `v1.0.1`).
5. Rollback: `git revert -m 1` del merge; no hay cambios de datos ni de esquema.
6. Un eventual índice va en una migración aparte.
