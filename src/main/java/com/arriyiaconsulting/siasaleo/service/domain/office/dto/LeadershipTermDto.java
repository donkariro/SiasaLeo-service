package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record LeadershipTermDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("seat_id") Long seatId,
    @JsonbProperty("cycle_id") Long cycleId,
    @JsonbProperty("start_date") LocalDate startDate,
    @JsonbProperty("end_date") LocalDate endDate
) {}
