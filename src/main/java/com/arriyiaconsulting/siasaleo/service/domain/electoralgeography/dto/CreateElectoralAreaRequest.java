package com.arriyiaconsulting.siasaleo.service.domain.electoralgeography.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateElectoralAreaRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String areaCode,
        @NotBlank String areaType,
        @NotNull Long parentId) {
}
