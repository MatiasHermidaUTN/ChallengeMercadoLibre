package com.enigma.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IncidentRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:incidents;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
class IncidentRepositoryTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired IncidentRepository repository;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM incidents");
        insert(1, "RESOLVED", "FIXED", "2025-03-10 10:00:00");
        insert(2, "CLOSED", "FIXED", "2025-03-11 10:00:00");
        insert(3, "RESOLVED", "CANCELLED", "2025-03-12 10:00:00");
        insert(4, "CLOSED", "DUPLICATE", "2025-03-12 11:00:00");
        insert(5, "OPEN", null, "2025-03-12 12:00:00");
        insert(6, "RESOLVED", null, "2025-03-13 09:00:00");
        insert(7, "RESOLVED", "FIXED", "2025-04-20 09:00:00");
    }

    @Test
    void returnsOnlyResolvedOrClosedWithoutCancelledNorDuplicate() {
        var out = repository.findResolved(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));
        assertThat(out).extracting(IncidentResponse::id).containsExactly(1L, 2L, 6L);
    }

    @Test
    void nullResolutionCodeCountsAsResolved() {
        assertThat(repository.findResolved(LocalDate.of(2025, 3, 13), LocalDate.of(2025, 3, 14)))
                .extracting(IncidentResponse::id).containsExactly(6L);
    }

    @Test
    void respectsDateRange() {
        assertThat(repository.findResolved(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30)))
                .extracting(IncidentResponse::id).containsExactly(7L);
    }

    @Test
    void emptyWhenNothingMatches() {
        assertThat(repository.findResolved(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))).isEmpty();
    }

    private void insert(long id, String status, String code, String resolvedAt) {
        jdbc.update("INSERT INTO incidents VALUES (?, ?, ?, ?, ?)",
                id, "t" + id, status, code, Timestamp.valueOf(resolvedAt));
    }
}
