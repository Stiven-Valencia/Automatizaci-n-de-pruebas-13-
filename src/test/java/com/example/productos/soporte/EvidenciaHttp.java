package com.example.productos.soporte;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultHandler;

/**
 * Registra la peticion y la respuesta reales de cada llamada de MockMvc.
 *
 * Cada linea que se escribe en target/evidencias.tsv la lee despues
 * tools/reporte.py para mostrar la evidencia dentro del reporte HTML.
 */
public final class EvidenciaHttp {

    private static final Path ARCHIVO = Paths.get("target", "evidencias.tsv");

    private EvidenciaHttp() {
    }

    /** Devuelve un manejador que anota lo ocurrido en la peticion indicada. */
    public static ResultHandler registrar(String prueba) {
        return resultado -> {
            MockHttpServletRequest peticion = resultado.getRequest();
            MockHttpServletResponse respuesta = resultado.getResponse();

            String linea = String.join("\t",
                    limpiar(prueba),
                    peticion.getMethod(),
                    rutaCompleta(peticion),
                    limpiar(peticion.getContentAsString()),
                    String.valueOf(respuesta.getStatus()),
                    limpiar(respuesta.getContentAsString()));

            escribir(linea + System.lineSeparator());
        };
    }

    private static String rutaCompleta(MockHttpServletRequest peticion) {
        String consulta = peticion.getQueryString();
        return consulta == null ? peticion.getRequestURI()
                : peticion.getRequestURI() + "?" + consulta;
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
