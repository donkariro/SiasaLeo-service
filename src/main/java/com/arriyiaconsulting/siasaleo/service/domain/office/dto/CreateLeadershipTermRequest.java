package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record CreateLeadershipTermRequest(
    @JsonbProperty("seat_id") @NotNull @Positive Long seatId,
    @JsonbProperty("cycle_id") @Positive Long cycleId,
    @JsonbProperty("start_date") @NotNull LocalDate startDate,
    @JsonbProperty("end_date") LocalDate endDate
) {}
