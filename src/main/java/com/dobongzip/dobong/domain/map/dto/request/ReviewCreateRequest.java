package com.dobongzip.dobong.domain.map.dto.request;

import jakarta.validation.constraints.*;

public record ReviewCreateRequest(
        @NotNull @DecimalMin("0.5") @DecimalMax("5.0") Double rating,
        @NotBlank @Size(max = 5000) String text
) {}
