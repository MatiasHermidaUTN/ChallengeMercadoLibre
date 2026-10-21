package com.enigma.incidents;

import java.time.OffsetDateTime;

// Mismo contrato de respuesta que el endpoint actual: no se agregan ni quitan campos.
public record IncidentResponse(
        long id,
        String title,
        String status,
        String resolutionCode,
        OffsetDateTime resolvedAt) {
}
