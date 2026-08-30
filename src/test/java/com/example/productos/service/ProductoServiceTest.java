package com.example.productos.service;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ProductoServiceTest {

    @Autowired
    private ProductoService service;
    @Autowired
    private ProductoRepository repository;

    @Test
    @DisplayName("Crear un producto permite recuperarlo por su id")
    void crearYObtenerProducto() {
        Producto creado = service.crear("Monitor", new BigDecimal("599.90"), 3);
        Producto obtenido = service.obtenerPorId(creado.getId());
        assertThat(obtenido.getNombre()).isEqualTo("Monitor");
    }

    @Test
    @DisplayName("Eliminar un id inexistente lanza NotFoundException")
    void eliminarProductoNoExistenteLanzaExcepcion() {
        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Un precio negativo es rechazado por el servicio")
    void precioNegativoLanzaIllegalArgument() {
        assertThatThrownBy(() -> service.crear("Tablet", new BigDecimal("-1.00"), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un stock negativo es rechazado por el servicio")
    void stockNegativoLanzaIllegalArgument() {
        assertThatThrownBy(() -> service.crear("Impresora", new BigDecimal("150.00"), -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Consultar un id inexistente lanza NotFoundException")
    void obtenerProductoNoExistenteLanzaExcepcion() {
        assertThatThrownBy(() -> service.obtenerPorId(999L))
            .isInstanceOf(NotFoundException.class);
    }


}
