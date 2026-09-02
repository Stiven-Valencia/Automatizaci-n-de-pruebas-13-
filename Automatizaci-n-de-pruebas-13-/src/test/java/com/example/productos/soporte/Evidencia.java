package com.example.productos.soporte;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.productos.domain.Producto;

/**
 * Registra el resultado real de las pruebas de repositorio y de servicio.
 *
 * Es el equivalente de {@link EvidenciaHttp} para las capas que no pasan por HTTP:
 * en lugar de una peticion y su respuesta, anota la operacion ejecutada y el objeto
 * o la excepcion que devolvio. tools/reporte.py lee estas lineas y las muestra
 * dentro del reporte HTML.
 */
public final class Evidencia {

    private static final Path ARCHIVO = Paths.get("target", "evidencias-datos.tsv");
    private static final String SALTO = "\\n";

    private Evidencia() {
    }

    /** Anota el resultado de una operacion dentro de la prueba indicada. */
    public static void registrar(String prueba, String operacion, Object resultado) {
        String linea = String.join("\t",
                limpiar(prueba),
                limpiar(operacion),
                describir(resultado));
        escribir(linea + System.lineSeparator());
    }

    /** Convierte cualquier resultado en un texto legible y fiel al valor original. */
    static String describir(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof Producto producto) {
            return String.format("{ id=%s, nombre=\"%s\", precio=%s, stock=%s }",
                    producto.getId(), producto.getNombre(),
                    producto.getPrecio(), producto.getStock());
        }
        if (valor instanceof Optional<?> opcional) {
            return opcional.map(contenido -> "Optional[ " + describir(contenido) + " ]")
                    .orElse("Optional.empty");
        }
        if (valor instanceof Collection<?> coleccion) {
            if (coleccion.isEmpty()) {
                return "[]";
            }
            return coleccion.stream()
                    .map(Evidencia::describir)
                    .collect(Collectors.joining("," + SALTO + "  ", "[" + SALTO + "  ", SALTO + "]"));
        }
        if (valor instanceof Throwable error) {
            String mensaje = error.getMessage() == null ? "" : ": " + error.getMessage();
            return error.getClass().getSimpleName() + mensaje;
        }
        return String.valueOf(valor);
    }

    private static void escribir(String linea) {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            Files.write(ARCHIVO, linea.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String limpiar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }
}
