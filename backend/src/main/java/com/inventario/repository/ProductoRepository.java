package com.inventario.repository;

import com.inventario.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    @Query("SELECT p FROM Producto p WHERE " +
           "(:search IS NULL OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categoria IS NULL OR LOWER(p.categoria) LIKE LOWER(CONCAT('%', :categoria, '%')))")
    Page<Producto> findByFilters(
        @Param("search") String search, 
        @Param("categoria") String categoria, 
        Pageable pageable
    );
    
    boolean existsByNombreAndIdNot(String nombre, Long id);
    
    boolean existsByNombre(String nombre);
    
    List<Producto> findByNombreIn(Collection<String> nombres);
}
