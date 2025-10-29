package com.inventario.service;

import com.inventario.client.FakeStoreClient;
import com.inventario.dto.FakeStoreProductDto;
import com.inventario.dto.PageResponse;
import com.inventario.dto.ProductoRequest;
import com.inventario.dto.ProductoResponse;
import com.inventario.exception.ResourceNotFoundException;
import com.inventario.exception.DuplicateResourceException;
import com.inventario.model.Producto;
import com.inventario.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductoService {
    
    private static final String DEFAULT_SORT_FIELD = "fechaRegistro";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "nombre", "categoria", "proveedor", "precio", "stock", DEFAULT_SORT_FIELD
    );
    private static final String DEFAULT_SORT_DIR = "desc";
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SYNC_BATCH_SIZE = 1000;
    
    private final ProductoRepository productoRepository;
    private final FakeStoreClient fakeStoreClient;
    
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> getAllProductos(
            String search,
            String categoria,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        log.info("Obteniendo productos - search: {}, categoria: {}, page: {}, size: {}", 
                 search, categoria, page, size);
        
        search = normalizeSearch(search);
        page = Math.max(0, page);
        size = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        sortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
        sortDir = DEFAULT_SORT_DIR.equalsIgnoreCase(sortDir) ? DEFAULT_SORT_DIR : "asc";
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Producto> productosPage = productoRepository.findByFilters(search, categoria, pageable);
        Page<ProductoResponse> responsePage = productosPage.map(ProductoResponse::fromEntity);
        
        log.info("Encontrados {} productos", responsePage.getTotalElements());
        return PageResponse.fromPage(responsePage);
    }
    
    private String normalizeSearch(String search) {
        if (search == null) return null;
        search = search.trim();
        return search.isEmpty() ? null : search;
    }
    
    @Transactional(readOnly = true)
    public ProductoResponse getProductoById(Long id) {
        log.info("Obteniendo producto con ID: {}", id);
        
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        
        return ProductoResponse.fromEntity(producto);
    }
    
    @Transactional
    public ProductoResponse createProducto(ProductoRequest request) {
        log.info("Creando nuevo producto: {}", request.getNombre());
        
        String nombre = request.getNombre().trim();
        if (productoRepository.existsByNombre(nombre)) {
            throw new DuplicateResourceException("Ya existe un producto con el nombre: " + nombre);
        }
        
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setCategoria(request.getCategoria().trim());
        producto.setProveedor(request.getProveedor() != null ? request.getProveedor().trim() : null);
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        
        Producto savedProducto = productoRepository.save(producto);
        log.info("Producto creado exitosamente con ID: {}", savedProducto.getId());
        
        return ProductoResponse.fromEntity(savedProducto);
    }
    
    @Transactional
    public ProductoResponse updateProducto(Long id, ProductoRequest request) {
        log.info("Actualizando producto con ID: {}", id);
        
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
        
        String nuevoNombre = request.getNombre().trim();
        if (!Objects.equals(producto.getNombre(), nuevoNombre) && 
            productoRepository.existsByNombreAndIdNot(nuevoNombre, id)) {
            throw new DuplicateResourceException("Ya existe otro producto con el nombre: " + nuevoNombre);
        }
        
        producto.setNombre(nuevoNombre);
        producto.setCategoria(request.getCategoria().trim());
        producto.setProveedor(request.getProveedor() != null ? request.getProveedor().trim() : null);
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        
        Producto updatedProducto = productoRepository.save(producto);
        log.info("Producto actualizado exitosamente");
        
        return ProductoResponse.fromEntity(updatedProducto);
    }
    
    @Transactional
    public void deleteProducto(Long id) {
        log.info("Eliminando producto con ID: {}", id);
        
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto", "id", id);
        }
        
        productoRepository.deleteById(id);
        log.info("Producto eliminado exitosamente");
    }
    
    private String sanitizeInput(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return HtmlUtils.htmlEscape(input.trim());
    }
    
    @Transactional
    public int syncProductsFromFakeStore() {
        log.info("Iniciando sincronización de productos desde FakeStore API");
        
        List<FakeStoreProductDto> fakeStoreProducts = fakeStoreClient.getAllProducts();
        
        if (fakeStoreProducts.isEmpty()) {
            log.warn("No se obtuvieron productos para sincronizar");
            return 0;
        }
        
        log.debug("Productos obtenidos de FakeStore: {}", fakeStoreProducts.size());
        
        Set<String> nombresExistentes = obtenerNombresExistentes(fakeStoreProducts);
        List<Producto> productosNuevos = convertirAProductosNuevos(fakeStoreProducts, nombresExistentes);
        
        if (productosNuevos.isEmpty()) {
            log.info("No se insertaron productos nuevos. Todos ya existían en la base de datos");
            return 0;
        }
        
        log.debug("Productos nuevos a insertar: {}", productosNuevos.size());
        int totalInsertados = insertarProductosEnBatch(productosNuevos);
        
        log.info("Sincronización completada exitosamente. Total productos nuevos insertados: {}", totalInsertados);
        return totalInsertados;
    }
    
    private Set<String> obtenerNombresExistentes(List<FakeStoreProductDto> fakeStoreProducts) {
        Set<String> nombresABuscar = fakeStoreProducts.stream()
                .filter(dto -> dto != null && dto.getTitle() != null && !dto.getTitle().isBlank())
                .map(dto -> sanitizeInput(dto.getTitle()))
                .collect(Collectors.toSet());
        
        if (nombresABuscar.isEmpty()) {
            log.warn("No hay productos válidos para sincronizar después del filtrado");
            return Set.of();
        }
        
        Set<String> nombresExistentes = productoRepository.findByNombreIn(nombresABuscar)
                .stream()
                .map(Producto::getNombre)
                .collect(Collectors.toSet());
        
        log.debug("Productos ya existentes en DB: {}", nombresExistentes.size());
        return nombresExistentes;
    }
    
    private List<Producto> convertirAProductosNuevos(List<FakeStoreProductDto> fakeStoreProducts, Set<String> nombresExistentes) {
        List<Producto> productosNuevos = new ArrayList<>();
        
        for (FakeStoreProductDto dto : fakeStoreProducts) {
            Producto producto = convertirDtoAProducto(dto, nombresExistentes);
            if (producto != null) {
                productosNuevos.add(producto);
            }
        }
        
        return productosNuevos;
    }
    
    private Producto convertirDtoAProducto(FakeStoreProductDto dto, Set<String> nombresExistentes) {
        if (!esProductoValido(dto)) {
            return null;
        }
        
        String nombreSanitizado = sanitizeInput(dto.getTitle());
        
        if (nombreSanitizado == null || nombresExistentes.contains(nombreSanitizado)) {
            log.debug("Producto omitido: {}", nombreSanitizado);
            return null;
        }
        
        Producto producto = new Producto();
        producto.setNombre(nombreSanitizado);
        producto.setCategoria(obtenerCategoria(dto));
        producto.setProveedor("FakeStore API");
        producto.setPrecio(obtenerPrecio(dto, nombreSanitizado));
        producto.setStock(0);
        
        return producto;
    }
    
    private boolean esProductoValido(FakeStoreProductDto dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            log.debug("Producto inválido sin título, omitiendo: {}", dto);
            return false;
        }
        return true;
    }
    
    private String obtenerCategoria(FakeStoreProductDto dto) {
        String categoriaSanitizada = sanitizeInput(dto.getCategory());
        return categoriaSanitizada != null ? categoriaSanitizada : "Sin categoría";
    }
    
    private BigDecimal obtenerPrecio(FakeStoreProductDto dto, String nombreProducto) {
        if (dto.getPrice() != null && dto.getPrice() >= 0) {
            return BigDecimal.valueOf(dto.getPrice());
        }
        log.debug("Precio inválido para producto '{}', usando 0", nombreProducto);
        return BigDecimal.ZERO;
    }
    
    private int insertarProductosEnBatch(List<Producto> productosNuevos) {
        if (productosNuevos.size() <= MAX_SYNC_BATCH_SIZE) {
            List<Producto> guardados = productoRepository.saveAll(productosNuevos);
            return guardados.size();
        }
        
        return insertarProductosEnChunks(productosNuevos);
    }
    
    private int insertarProductosEnChunks(List<Producto> productosNuevos) {
        log.info("Procesando {} productos en chunks de {}", productosNuevos.size(), MAX_SYNC_BATCH_SIZE);
        int totalInsertados = 0;
        
        for (int i = 0; i < productosNuevos.size(); i += MAX_SYNC_BATCH_SIZE) {
            int end = Math.min(i + MAX_SYNC_BATCH_SIZE, productosNuevos.size());
            List<Producto> chunk = productosNuevos.subList(i, end);
            
            List<Producto> guardados = productoRepository.saveAll(chunk);
            totalInsertados += guardados.size();
            
            log.debug("Batch {}/{} procesado: {} productos insertados", 
                      (i / MAX_SYNC_BATCH_SIZE) + 1, 
                      (productosNuevos.size() / MAX_SYNC_BATCH_SIZE) + 1,
                      guardados.size());
        }
        
        return totalInsertados;
    }
}
