package com.example.productos.service;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import com.example.productos.soporte.Evidencia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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

    private String pruebaActual;

    @BeforeEach
    void setUp(TestInfo info) {
        pruebaActual = info.getDisplayName();
    }

    @Test
    @DisplayName("Crear un producto permite recuperarlo por su id")
    void crearYObtenerProducto() {
        Producto creado = service.crear("Monitor", new BigDecimal("599.90"), 3);
        Producto obtenido = service.obtenerPorId(creado.getId());
        Evidencia.registrar(pruebaActual,
                "service.crear(Monitor, 599.90, 3) y service.obtenerPorId(" + creado.getId() + ")", obtenido);

        assertThat(obtenido.getNombre()).isEqualTo("Monitor");
    }

    @Test
    @DisplayName("Eliminar un id inexistente lanza NotFoundException")
    void eliminarProductoNoExistenteLanzaExcepcion() {
        Throwable error = catchThrowable(() -> service.eliminar(999L));
        Evidencia.registrar(pruebaActual, "service.eliminar(999)", error);

        assertThat(error).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Un precio negativo es rechazado por el servicio")
    void precioNegativoLanzaIllegalArgument() {
        Throwable error = catchThrowable(() -> service.crear("Tablet", new BigDecimal("-1.00"), 1));
        Evidencia.registrar(pruebaActual, "service.crear(Tablet, -1.00, 1)", error);

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un stock negativo es rechazado por el servicio")
    void stockNegativoLanzaIllegalArgument() {
        Throwable error = catchThrowable(() -> service.crear("Impresora", new BigDecimal("150.00"), -1));
        Evidencia.registrar(pruebaActual, "service.crear(Impresora, 150.00, -1)", error);

        assertThat(error).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Consultar un id inexistente lanza NotFoundException")
    void obtenerProductoNoExistenteLanzaExcepcion() {
        Throwable error = catchThrowable(() -> service.obtenerPorId(999L));
        Evidencia.registrar(pruebaActual, "service.obtenerPorId(999)", error);

        assertThat(error).isInstanceOf(NotFoundException.class);
    }


}
