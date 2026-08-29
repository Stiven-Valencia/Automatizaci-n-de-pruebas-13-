# Pruebas de Integración con Spring Boot Test

Proyecto de práctica de **pruebas de integración** en Spring Boot sobre un CRUD de `Producto`:
- **Capas**: controlador, servicio, repositorio, entidad `Producto`.
- **Base de datos en memoria H2** (perfil de test).
- **Tipos de prueba** incluidos:
  - `@DataJpaTest` (repositorio)
  - `@SpringBootTest` (servicio)
  - `@SpringBootTest` + `@AutoConfigureMockMvc` (controlador REST)

## Requisitos
- Java 17 o superior (probado con Temurin 21)
- Maven 3.9+

## Ejecutar la app
```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080/productos` y la consola H2 en `http://localhost:8080/h2-console`.

## Ejecutar pruebas
```bash
mvn clean verify
```

Se usa `verify` y no `test` porque las pruebas están repartidas en dos plugins:

| Plugin | Clases que ejecuta | Pruebas |
|---|---|---|
| surefire | `*Test` | repositorio (4) y servicio (4) |
| failsafe | `*IT` | controlador REST (6) |

`mvn test` solo ejecutaría las 8 de surefire; `mvn verify` ejecuta las 14.

## Reportes

Tras `mvn clean verify` se generan automáticamente:

| Reporte | Ruta |
|---|---|
| Pruebas de repositorio y servicio | `target/reports/surefire.html` |
| Pruebas del controlador REST | `target/reports/failsafe.html` |
| Cobertura de código (JaCoCo) | `target/site/jacoco/index.html` |

Cobertura actual: **100%** en `ProductoService` y `ProductoController`.

## Integración continua

El workflow `.github/workflows/ci.yml` ejecuta `mvn clean verify` en cada push y pull
request, y publica los tres reportes como artefacto descargable desde la pestaña
**Actions** del repositorio.

## Estructura clave
```
src/
 ├─ main/
 │   ├─ java/com/example/productos/...   # código fuente App
 │   └─ resources/application.properties # configuración H2 runtime
 └─ test/
     ├─ java/com/example/productos/...   # pruebas (repo, servicio, controller)
     └─ resources/
         ├─ application-test.properties  # perfil test: H2 propia + create-drop
         └─ data.sql                     # datos iniciales de prueba
```

## Actividades sugeridas
1. Agregar validaciones (Bean Validation) y probar errores 400.
2. Añadir endpoints PUT/PATCH y sus pruebas.
3. ~~Medir cobertura con JaCoCo.~~ ✅ implementado
4. ~~Integrar un pipeline CI (GitHub Actions) que ejecute las pruebas.~~ ✅ implementado
