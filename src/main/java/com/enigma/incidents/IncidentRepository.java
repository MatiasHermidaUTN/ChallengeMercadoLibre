package com.enigma.incidents;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IncidentRepository {

    // Antes: WHERE resolved_at BETWEEN :from AND :to (colaba cancelados y duplicados).
    // Ahora se agrega la regla de negocio que dio Laura (PO).
    // resolution_code NULL cuenta como resuelto: supuesto que Laura aprobo.
    static final String FIND_RESOLVED = """
            SELECT id, title, status, resolution_code, resolved_at
            FROM incidents
            WHERE resolved_at BETWEEN :from AND :to
              AND status IN ('RESOLVED', 'CLOSED')
              AND (resolution_code IS NULL OR resolution_code NOT IN ('CANCELLED', 'DUPLICATE'))
            ORDER BY resolved_at, id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public IncidentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IncidentResponse> findResolved(LocalDate from, LocalDate to) {
        var params = new MapSqlParameterSource()
                .addValue("from", Timestamp.from(from.atStartOfDay().toInstant(ZoneOffset.UTC)))
                .addValue("to", Timestamp.from(to.atStartOfDay().toInstant(ZoneOffset.UTC)));
        return jdbc.query(FIND_RESOLVED, params, (rs, i) -> new IncidentResponse(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getString("resolution_code"),
                rs.getTimestamp("resolved_at").toInstant().atOffset(ZoneOffset.UTC)));
    }
}
