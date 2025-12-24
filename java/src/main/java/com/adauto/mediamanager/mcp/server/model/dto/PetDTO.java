package com.adauto.mediamanager.mcp.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetDTO
{
    @JsonProperty("_id")
    private String id;

    private String name;

    private String breed;

    private Integer age;
}
