package com.example.productos.controller;

import com.example.productos.domain.Producto;
import com.example.productos.repository.ProductoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ProductoRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    private Long existingId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Producto p = new Producto("Laptop", new BigDecimal("2500.00"), 2);
        existingId = repository.save(p).getId();
    }

    @Test
    void listarProductosDevuelve200() throws Exception {
        mockMvc.perform(get("/productos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void crearProductoDevuelve201() throws Exception {
        var body = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("nombre", "Auriculares");
            put("precio", "199.99");
            put("stock", 15);
        }});
        mockMvc.perform(post("/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Auriculares"));
    }

    @Test
    void obtenerProductoPorIdExistenteDevuelve200() throws Exception {
        mockMvc.perform(get("/productos/{id}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId));
    }

    @Test
    void obtenerProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/productos/{id}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProductoDevuelve204() throws Exception {
        mockMvc.perform(delete("/productos/{id}", existingId))
                .andExpect(status().isNoContent());
    }
}
