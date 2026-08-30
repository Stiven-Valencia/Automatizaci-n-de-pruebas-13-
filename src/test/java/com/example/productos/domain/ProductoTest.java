package com.example.productos.domain;

import java.math.BigDecimal;

import com.example.productos.soporte.Evidencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de la entidad Producto.
 *
 * Producto define su identidad por el id, de modo que su equals decide cuando dos
 * instancias representan el mismo registro. Ese comportamiento afecta a cualquier
 * coleccion basada en igualdad, asi que conviene tenerlo verificado.
 */
class ProductoTest {

    private String pruebaActual;

    @BeforeEach
    void setUp(TestInfo info) {
        pruebaActual = info.getDisplayName();
    }

    @Test
    @DisplayName("Dos productos con el mismo id son iguales aunque cambien sus datos")
    void productosConMismoIdSonIguales() {
        // Arrange
        Producto uno = new Producto("Teclado", new BigDecimal("99.99"), 10);
        Producto otro = new Producto("Monitor", new BigDecimal("599.90"), 3);
        uno.setId(7L);
        otro.setId(7L);

        // Act
        boolean sonIguales = uno.equals(otro);
        Evidencia.registrar(pruebaActual,
                "new Producto(Teclado).setId(7).equals(new Producto(Monitor).setId(7))", sonIguales);

        // Assert
        assertThat(uno).isEqualTo(otro);
        assertThat(uno).hasSameHashCodeAs(otro);
    }

    @Test
    @DisplayName("Dos productos con ids distintos no son iguales")
    void productosConIdsDistintosNoSonIguales() {
        // Arrange
        Producto uno = new Producto("Teclado", new BigDecimal("99.99"), 10);
        Producto otro = new Producto("Teclado", new BigDecimal("99.99"), 10);
        uno.setId(1L);
        otro.setId(2L);

        // Act
        boolean sonIguales = uno.equals(otro);
        Evidencia.registrar(pruebaActual, "producto(id=1).equals(producto(id=2))", sonIguales);

        // Assert
        assertThat(uno).isNotEqualTo(otro);
    }

    @Test
    @DisplayName("Un producto solo es igual a otro producto")
    void productoNoEsIgualAOtroTipo() {
        // Arrange
        Producto producto = new Producto("Teclado", new BigDecimal("99.99"), 10);
        producto.setId(1L);

        // Act
        Object textoSuelto = "Teclado";
        boolean igualATexto = producto.equals(textoSuelto);
        Evidencia.registrar(pruebaActual, "producto.equals(\"Teclado\")", igualATexto);

        // Assert
        assertThat(producto).isNotEqualTo("Teclado");
        assertThat(producto).isEqualTo(producto);
    }

    @Test
    @DisplayName("Dos productos sin guardar se consideran iguales entre si")
    void productosSinIdSeConsideranIguales() {
        // Arrange: ningun producto se ha guardado todavia, asi que ambos tienen id nulo
        Producto teclado = new Producto("Teclado", new BigDecimal("99.99"), 10);
        Producto monitor = new Producto("Monitor", new BigDecimal("599.90"), 3);

        // Act
        boolean sonIguales = teclado.equals(monitor);
        Evidencia.registrar(pruebaActual,
                "new Producto(Teclado).equals(new Producto(Monitor)) con ambos id=null", sonIguales);

        // Assert: comportamiento actual, no deseable. Al comparar solo por id, dos
        // productos distintos aun sin persistir resultan iguales; si se agregaran a un
        // HashSet se colapsarian en uno solo. Queda documentado hasta que se decida
        // cambiar el criterio de igualdad de la entidad.
        assertThat(teclado).isEqualTo(monitor);
    }

    @Test
    @DisplayName("Los mutadores actualizan los datos del producto")
    void mutadoresActualizanLosDatos() {
        // Arrange
        Producto producto = new Producto("Teclado", new BigDecimal("99.99"), 10);

        // Act
        producto.setId(3L);
        producto.setNombre("Teclado inalambrico");
        producto.setPrecio(new BigDecimal("149.50"));
        producto.setStock(25);
        Evidencia.registrar(pruebaActual, "setId(3), setNombre(...), setPrecio(...), setStock(25)", producto);

        // Assert
        assertThat(producto.getId()).isEqualTo(3L);
        assertThat(producto.getNombre()).isEqualTo("Teclado inalambrico");
        assertThat(producto.getPrecio()).isEqualByComparingTo("149.50");
        assertThat(producto.getStock()).isEqualTo(25);
    }
}
