# Enigma — fix del conteo de incidentes resueltos

Corrige el conteo semanal de incidentes resueltos, que incluía descartados en triaje.

- `sql/` consulta antes/después
- `src/resolved_incidents.js` handler `GET /api/v1/incidents/resolved/weekly`
- `test/` pruebas (`npm test`, Node 20+, sin dependencias)
- `docs/DECISIONES_Y_DUDAS.md` diagnóstico, decisiones, dudas y estrategia de Git
