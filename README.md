# Enigma — fix de incidentes resueltos

Corrige `GET /api/v1/incidents/resolved?from=YYYY-MM-DD&to=YYYY-MM-DD`, que contaba cancelados y duplicados.
Resuelto = estado `RESOLVED`/`CLOSED` y `resolution_code` distinto de `CANCELLED` y `DUPLICATE`.

- `src/main/java/com/enigma/incidents/` controller, repositorio (SQL explícito) y respuesta (contrato sin cambios)
- `src/test/` pruebas con H2 en modo PostgreSQL y MockMvc (`mvn test`, Java 17)
- `sql/` consulta antes/después
- `docs/DECISIONES_Y_DUDAS.md` diagnóstico, decisiones, respuestas de Laura (PO), dudas abiertas y estrategia de Git
