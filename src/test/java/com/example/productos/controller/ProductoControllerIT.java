package com.example.productos.controller;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import com.example.productos.soporte.EvidenciaHttp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProductoControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductoRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    private Long existingId;
    private String pruebaActual;

    @BeforeEach
    void setUp(TestInfo info) {
        pruebaActual = info.getDisplayName();
        repository.deleteAll();
        Producto p = new Producto("Laptop", new BigDecimal("2500.00"), 2);
        existingId = repository.save(p).getId();
        repository.save(new Producto("Mouse", new BigDecimal("80.00"), 5));

    }

    @Test
    @DisplayName("GET /productos devuelve 200 con la lista completa")
    void listarProductosDevuelve200() throws Exception {
        mockMvc.perform(get("/productos"))
                .andDo(EvidenciaHttp.registrar(pruebaActual))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("Laptop", "Mouse")));

    }

    @Test
    @DisplayName("POST /productos crea el producto y devuelve 201")
    void crearProductoDevuelve201() throws Exception {
        var body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("nombre", "Auriculares");
            put("precio", "199.99");
            put("stock", 15);
        }});
        mockMvc.perform(post("/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andDo(EvidenciaHttp.registrar(pruebaActual))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Auriculares"));
    }

    @Test
    @DisplayName("GET /productos/{id} devuelve 200 con el producto correcto")
    void obtenerProductoPorIdExistenteDevuelve200() throws Exception {
        mockMvc.perform(get("/productos/{id}", existingId))
                .andDo(EvidenciaHttp.registrar(pruebaActual))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId));
    }

    @Test
    @DisplayName("GET /productos/{id} devuelve 404 si no existe")
    void obtenerProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/productos/{id}", 9999))
                .andDo(EvidenciaHttp.registrar(pruebaActual))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /productos/{id} elimina y devuelve 204")
    void eliminarProductoDevuelve204() throws Exception {
        mockMvc.perform(delete("/productos/{id}", existingId))
                .andDo(EvidenciaHttp.registrar(pruebaActual))
                .andExpect(status().isNoContent());
                
        mockMvc.perform(get("/productos/{id}", existingId))
        .andDo(EvidenciaHttp.registrar(pruebaActual))
        .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /productos/{id} devuelve 404 si no existe")
    void eliminarProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(delete("/productos/{id}", 9999))
            .andDo(EvidenciaHttp.registrar(pruebaActual))
            .andExpect(status().isNotFound());
    }

}
