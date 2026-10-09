package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import jakarta.json.bind.annotation.JsonbProperty;
public record AssetAuditDto(Long id, @JsonbProperty("actor_account_id") Long actorAccountId,
 String action, @JsonbProperty("before_json") String beforeJson,
 @JsonbProperty("after_json") String afterJson, @JsonbProperty("recorded_at") String recordedAt) {}
