package com.inventario.client;

import com.inventario.dto.FakeStoreProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests para FakeStoreClient - Consumo de API Externa
 * Cobertura: Timeouts, manejo de errores HTTP, validación de respuestas
 */
@DisplayName("FakeStoreClient - API Externa")
class FakeStoreClientTest {

    private RestTemplate restTemplate;
    private FakeStoreClient fakeStoreClient;
    private RestTemplateBuilder restTemplateBuilder;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        restTemplateBuilder = mock(RestTemplateBuilder.class);
        
        // Simular la configuración del RestTemplateBuilder
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.defaultHeader(anyString(), anyString())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        fakeStoreClient = new FakeStoreClient(restTemplateBuilder);
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar productos exitosamente con respuesta 2xx")
    void getAllProducts_DebeRetornarProductosExitosamente() {
        // Arrange
        FakeStoreProductDto[] productos = new FakeStoreProductDto[2];
        FakeStoreProductDto producto1 = new FakeStoreProductDto();
        producto1.setId(1L);
        producto1.setTitle("Test Product");
        producto1.setPrice(29.99);
        producto1.setCategory("electronics");
        productos[0] = producto1;
        productos[1] = new FakeStoreProductDto();
        
        ResponseEntity<FakeStoreProductDto[]> response = ResponseEntity.ok(productos);
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenReturn(response);

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Product");
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar lista vacía cuando API retorna 404 (HttpClientErrorException)")
    void getAllProducts_DebeRetornarListaVaciaCon404() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar lista vacía cuando API retorna 500 (HttpServerErrorException)")
    void getAllProducts_DebeRetornarListaVaciaCon500() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar lista vacía cuando hay timeout (ResourceAccessException)")
    void getAllProducts_DebeRetornarListaVaciaConTimeout() {
        // Arrange
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenThrow(new ResourceAccessException("Read timeout"));

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar lista vacía cuando respuesta no es 2xx")
    void getAllProducts_DebeRetornarListaVaciaConRespuestaNo2xx() {
        // Arrange
        ResponseEntity<FakeStoreProductDto[]> response = ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).build();
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenReturn(response);

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }

    @Test
    @DisplayName("getAllProducts - Debe retornar lista vacía cuando body es null")
    void getAllProducts_DebeRetornarListaVaciaConBodyNull() {
        // Arrange
        ResponseEntity<FakeStoreProductDto[]> response = ResponseEntity.ok().build(); // body null
        when(restTemplate.getForEntity(anyString(), eq(FakeStoreProductDto[].class)))
            .thenReturn(response);

        // Act
        List<FakeStoreProductDto> result = fakeStoreClient.getAllProducts();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(restTemplate).getForEntity(anyString(), eq(FakeStoreProductDto[].class));
    }
}
