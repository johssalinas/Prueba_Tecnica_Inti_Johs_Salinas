package com.inventario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

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
