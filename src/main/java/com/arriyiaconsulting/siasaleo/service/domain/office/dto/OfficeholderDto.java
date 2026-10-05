package com.arriyiaconsulting.siasaleo.service.domain.office.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;
import java.math.BigDecimal;
import com.arriyiaconsulting.siasaleo.service.domain.governance.shared.entity.*;

public record OfficeholderDto(
    @JsonbProperty("id") Long id,
    @JsonbProperty("seat_id") Long seatId,
    @JsonbProperty("person_id") Long personId,
    @JsonbProperty("candidacy_id") Long candidacyId,
    @JsonbProperty("start_date") LocalDate startDate,
    @JsonbProperty("end_date") LocalDate endDate,
    @JsonbProperty("term_id") Long termId,
    @JsonbProperty("entry_reason_id") Long entryReasonId,
    @JsonbProperty("exit_reason_id") Long exitReasonId
) {}
