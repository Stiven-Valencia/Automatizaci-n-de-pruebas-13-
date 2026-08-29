package com.example.productos.repository;

import com.example.productos.domain.Producto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    void setUp() {
        // Datos iniciales de prueba
        productoPrueba = new Producto("Teclado Mecánico", new BigDecimal("99.99"), 10);
    }

    @Test
    void guardarProducto_DebePersistirEnBaseDeDatos() {
        // Arrange & Act
        Producto guardado = repository.save(productoPrueba);

        // Assert
        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getNombre()).isEqualTo("Teclado Mecánico");
    }

    @Test
    void buscarPorId_DebeRetornarProducto() {
        // Arrange
        Producto guardado = repository.save(productoPrueba);

        // Act
        Optional<Producto> encontrado = repository.findById(guardado.getId());

        // Assert
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getNombre()).isEqualTo("Teclado Mecánico");
    }

    @Test
    void eliminarProducto_DebeRemoverDeBaseDeDatos() {
        // Arrange
        Producto guardado = repository.save(productoPrueba);

        // Act
        repository.deleteById(guardado.getId());
        Optional<Producto> eliminado = repository.findById(guardado.getId());

        // Assert
        assertThat(eliminado).isEmpty();
    }

    @Test
    void datosInicialesFueronCargados() {
        // Act
        List<Producto> productos = repository.findAll();

        // Assert
        assertThat(productos).hasSizeGreaterThanOrEqualTo(3);
        assertThat(productos).extracting(Producto::getNombre)
                .contains("cable test", "cable test2", "cable test3");
    }
}