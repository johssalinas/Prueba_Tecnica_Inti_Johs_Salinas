package com.inventario.service;

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

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductoService {
    
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "nombre", "categoria", "proveedor", "precio", "stock", "fechaRegistro"
    );
    private static final String DEFAULT_SORT_FIELD = "fechaRegistro";
    private static final String DEFAULT_SORT_DIR = "desc";
    private static final int MAX_PAGE_SIZE = 100;
    
    private final ProductoRepository productoRepository;
    
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
}
