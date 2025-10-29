package com.inventario.client;

import com.inventario.dto.FakeStoreProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FakeStoreClient {
    
    private static final String FAKE_STORE_API_URL = "https://fakestoreapi.com/products";
    private final RestTemplate restTemplate;
    
    public FakeStoreClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .defaultHeader(HttpHeaders.USER_AGENT, "SistemaInventario/1.0")
                .build();
    }
    
    public List<FakeStoreProductDto> getAllProducts() {
        try {
            log.info("Consultando productos desde FakeStore API: {}", FAKE_STORE_API_URL);
            
            ResponseEntity<FakeStoreProductDto[]> response = restTemplate.getForEntity(
                    FAKE_STORE_API_URL,
                    FakeStoreProductDto[].class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("FakeStore API retornó status no exitoso: {}", response.getStatusCode());
                return Collections.emptyList();
            }
            
            FakeStoreProductDto[] body = response.getBody();
            if (body == null || body.length == 0) {
                log.warn("La respuesta de FakeStore API es nula o vacía");
                return Collections.emptyList();
            }
            
            log.info("Se obtuvieron {} productos desde FakeStore API", body.length);
            return Arrays.asList(body);
            
        } catch (HttpClientErrorException e) {
            log.error("Error del cliente al consumir FakeStore API [{}]: {}", 
                      e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
            
        } catch (HttpServerErrorException e) {
            log.error("Error del servidor externo FakeStore API [{}]: {}", 
                      e.getStatusCode(), e.getMessage());
            return Collections.emptyList();
            
        } catch (ResourceAccessException e) {
            log.error("Timeout o error de red al conectar con FakeStore API: {}", e.getMessage());
            return Collections.emptyList();
            
        } catch (RestClientException e) {
            log.error("Error inesperado al consumir FakeStore API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
