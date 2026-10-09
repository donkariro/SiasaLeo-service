package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;

import jakarta.validation.constraints.*;
import jakarta.json.bind.annotation.JsonbProperty;
import java.time.*;

public record CreateDigitalAssetVersionRequest(
    @JsonbProperty("source_url") @Size(max = 2048) String sourceUrl,
    @JsonbProperty("author_party_id") @Positive Long authorPartyId,
    @JsonbProperty("publisher_party_id") @Positive Long publisherPartyId,
    @JsonbProperty("source_published_on") LocalDate sourcePublishedOn,
    @JsonbProperty("provenance") @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String provenance
) {}
