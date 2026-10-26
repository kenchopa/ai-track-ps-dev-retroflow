package com.stackcraft.retroflow.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.stackcraft.retroflow.config.IsoLocalDateDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRetrospectiveRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull @JsonDeserialize(using = IsoLocalDateDeserializer.class) LocalDate date
) {
}
