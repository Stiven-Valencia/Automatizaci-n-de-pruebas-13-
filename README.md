# Pruebas de Integración con Spring Boot Test — Esqueleto

Este proyecto es un **esqueleto listo** para que los estudiantes practiquen **pruebas de integración** en Spring Boot:
- **Capas**: controlador, servicio, repositorio, entidad `Producto`.
- **Base de datos en memoria H2** (perfil de test).
- **Tipos de prueba** incluidos:
  - `@DataJpaTest` (repositorio)
  - `@SpringBootTest` (servicio)
  - `@SpringBootTest` + `@AutoConfigureMockMvc` (controlador REST)

## Requisitos
- Java 17+
- Maven 3.9+

## Ejecutar la app
```bash
mvn spring-boot:run
```

## Ejecutar pruebas
```bash
mvn clean test
```

## Estructura clave
```
src/
 ├─ main/
 │   ├─ java/com/example/productos/...   # código fuente App
 │   └─ resources/application.properties # configuración H2 runtime
 └─ test/
     ├─ java/com/example/productos/...   # pruebas (repo, servicio, controller)
     └─ resources/application-test.properties
```

## Actividades sugeridas
1. Agregar validaciones (Bean Validation) y probar errores 400.
2. Añadir endpoints PUT/PATCH y sus pruebas.
3. Medir cobertura con JaCoCo.
4. Integrar un pipeline CI (GitHub Actions) que ejecute `mvn test`.
