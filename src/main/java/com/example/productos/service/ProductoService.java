package com.example.productos.service;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> listar() {
        return repository.findAll();
    }

    public Producto crear(String nombre, BigDecimal precio, Integer stock) {
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        Producto p = new Producto(nombre, precio, stock);
        return repository.save(p);
    }

    public Producto obtenerPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Producto no encontrado: " + id));
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Producto no encontrado: " + id);
        }
        repository.deleteById(id);
    }
}
