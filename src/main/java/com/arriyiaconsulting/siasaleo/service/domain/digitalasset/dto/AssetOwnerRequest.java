package com.arriyiaconsulting.siasaleo.service.domain.digitalasset.dto;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.validation.constraints.Positive;
public record AssetOwnerRequest(@JsonbProperty("owner_account_id") @Positive Long ownerAccountId,
 @JsonbProperty("owner_party_id") @Positive Long ownerPartyId) {
 public void validate() {
  if ((ownerAccountId == null) == (ownerPartyId == null))
   throw new IllegalArgumentException("Specify exactly one account or party owner");
 }
}
