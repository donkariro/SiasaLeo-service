package com.arriyiaconsulting.siasaleo.service.domain.nomination.dto;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** row_version names the saved draft being submitted; accepted_declarations lists declaration keys. */
public record SubmitApplicationRequest(
    @JsonbProperty("row_version") @NotNull Long rowVersion,
    @JsonbProperty("accepted_declarations") List<String> acceptedDeclarations
) {}
