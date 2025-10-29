package com.inventario.repository;

import com.inventario.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    
    @Query("SELECT m FROM MovimientoStock m WHERE m.productoId = :productoId ORDER BY m.fecha DESC")
    List<MovimientoStock> findByProductoIdOrderByFechaDesc(@Param("productoId") Long productoId);
}
