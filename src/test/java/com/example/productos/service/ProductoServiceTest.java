package com.example.productos.service;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductoServiceTest {

    @Autowired
    private ProductoService service;
    @Autowired
    private ProductoRepository repository;

    @Test
    void crearYObtenerProducto() {
        Producto creado = service.crear("Monitor", new BigDecimal("599.90"), 3);
        Producto obtenido = service.obtenerPorId(creado.getId());
        assertThat(obtenido.getNombre()).isEqualTo("Monitor");
    }

    @Test
    void eliminarProductoNoExistenteLanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void precioNegativoLanzaIllegalArgument() {
        assertThatThrownBy(() -> service.crear("Tablet", new BigDecimal("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
