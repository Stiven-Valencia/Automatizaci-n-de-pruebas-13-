package com.example.productos.repository;

import com.example.productos.domain.Producto;
import com.example.productos.soporte.Evidencia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;

    private Producto productoPrueba;
    private String pruebaActual;

    @BeforeEach
    void setUp(TestInfo info) {
        pruebaActual = info.getDisplayName();

        // Datos iniciales de prueba
        productoPrueba = new Producto("Teclado Mecánico", new BigDecimal("99.99"), 10);
    }

    @Test
    @DisplayName("Guardar un producto lo persiste con un id generado")
    void guardarProducto_DebePersistirEnBaseDeDatos() {
        // Arrange & Act
        Producto guardado = repository.save(productoPrueba);
        Evidencia.registrar(pruebaActual, "repository.save(producto)", guardado);

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("Teclado Mecánico");
    }

    @Test
    @DisplayName("Buscar por id devuelve el producto guardado")
    void buscarPorId_DebeRetornarProducto() {
        // Arrange
        Producto guardado = repository.save(productoPrueba);

        // Act
        Optional<Producto> encontrado = repository.findById(guardado.getId());
        Evidencia.registrar(pruebaActual, "repository.findById(" + guardado.getId() + ")", encontrado);

        // Assert
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Teclado Mecánico");
    }

    @Test
    @DisplayName("Eliminar un producto lo borra de la base de datos")
    void eliminarProducto_DebeRemoverDeBaseDeDatos() {
        // Arrange
        Producto guardado = repository.save(productoPrueba);

        // Act
        repository.deleteById(guardado.getId());
        Optional<Producto> eliminado = repository.findById(guardado.getId());
        Evidencia.registrar(pruebaActual,
                "repository.deleteById(" + guardado.getId() + ") y findById(" + guardado.getId() + ")", eliminado);

        // Assert
        assertThat(eliminado).isEmpty();
    }

    @Test
    @DisplayName("Los datos iniciales de data.sql se cargan al arrancar")
    void datosInicialesFueronCargados() {
        // Act
        List<Producto> productos = repository.findAll();
        Evidencia.registrar(pruebaActual, "repository.findAll()", productos);

        // Assert
        assertThat(productos).hasSizeGreaterThanOrEqualTo(3);
        assertThat(productos).extracting(Producto::getNombre)
                .contains("cable test", "cable test2", "cable test3");
    }

}