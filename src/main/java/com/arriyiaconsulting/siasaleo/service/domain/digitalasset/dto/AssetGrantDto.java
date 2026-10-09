package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import jakarta.json.bind.annotation.JsonbProperty;
public record AssetGrantDto(@JsonbProperty("account_id") Long accountId, String permission) {}
