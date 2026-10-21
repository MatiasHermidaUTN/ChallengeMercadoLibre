package com.enigma.incidents;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired MockMvc mvc;
    @MockBean IncidentRepository repository;

    @Test
    void keepsResponseContract() throws Exception {
        when(repository.findResolved(LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31)))
                .thenReturn(List.of(new IncidentResponse(1, "Caida API", "RESOLVED", "FIXED",
                        OffsetDateTime.parse("2025-03-10T10:00:00Z"))));
        mvc.perform(get("/api/v1/incidents/resolved").param("from", "2025-03-01").param("to", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Caida API"))
                .andExpect(jsonPath("$[0].status").value("RESOLVED"))
                .andExpect(jsonPath("$[0].resolutionCode").value("FIXED"))
                .andExpect(jsonPath("$[0].resolvedAt").exists());
    }

    @Test
    void rejectsInvalidDate() throws Exception {
        mvc.perform(get("/api/v1/incidents/resolved").param("from", "hoy").param("to", "2025-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsFromAfterTo() throws Exception {
        mvc.perform(get("/api/v1/incidents/resolved").param("from", "2025-04-01").param("to", "2025-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingParams() throws Exception {
        mvc.perform(get("/api/v1/incidents/resolved")).andExpect(status().isBadRequest());
    }
}
