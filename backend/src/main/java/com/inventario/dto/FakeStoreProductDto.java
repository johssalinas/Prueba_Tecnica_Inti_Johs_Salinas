package com.inventario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para mapear la respuesta de la API de FakeStore
 * Usa @JsonProperty para mapeo explícito y @JsonIgnoreProperties para tolerancia a cambios
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FakeStoreProductDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("price")
    private Double price;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("category")
    private String category;
    
    @JsonProperty("image")
    private String image;
}
