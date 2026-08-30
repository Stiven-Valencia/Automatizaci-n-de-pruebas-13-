# API de Productos — Pruebas de Integración con Spring Boot

API REST para la gestión de productos, construida con Spring Boot 3.5 y base de datos
H2 en memoria. El proyecto sirve como caso de estudio de **pruebas de integración
automatizadas**: cubre las tres capas de la aplicación (controlador, servicio y
repositorio) con 15 pruebas que se ejecutan automáticamente en cada cambio.

## Tecnologías

| Componente | Versión |
|---|---|
| Java | 17 o superior (probado con Temurin 21) |
| Spring Boot | 3.5.16 |
| Base de datos | H2 en memoria |
| Persistencia | Spring Data JPA / Hibernate |
| Pruebas | JUnit 5, AssertJ, MockMvc |
| Cobertura | JaCoCo 0.8.14 |
| Construcción | Maven 3.9+ |

---

## Qué hace la aplicación

Expone un CRUD de productos sobre una base de datos en memoria. Cada producto tiene:

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | Long | Generado por la base de datos |
| `nombre` | String | Obligatorio y único |
| `precio` | BigDecimal | Obligatorio, 12 dígitos con 2 decimales, no negativo |
| `stock` | Integer | Obligatorio, no negativo |

Reglas de negocio implementadas en `ProductoService`:

- El precio no puede ser negativo → `IllegalArgumentException`
- El stock no puede ser negativo → `IllegalArgumentException`
- Consultar o eliminar un id inexistente → `NotFoundException`

### Endpoints

| Método | Ruta | Códigos | Descripción |
|---|---|---|---|
| `GET` | `/productos` | `200` | Lista todos los productos |
| `POST` | `/productos` | `201` | Crea un producto |
| `GET` | `/productos/{id}` | `200` · `404` | Consulta un producto por id |
| `DELETE` | `/productos/{id}` | `204` · `404` | Elimina un producto por id |

Ejemplos de uso:

```bash
# Listar productos
curl http://localhost:8080/productos

# Crear un producto
curl -X POST http://localhost:8080/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Teclado","precio":120500,"stock":10}'

# Consultar por id
curl http://localhost:8080/productos/1

# Eliminar
curl -X DELETE http://localhost:8080/productos/1
```

> **Limitación conocida:** enviar un precio o stock negativo devuelve `500`, no `400`.
> Las validaciones existen en el servicio, pero no hay un manejador que traduzca la
> excepción a una respuesta HTTP adecuada. Lo resuelve la actividad pendiente 1.

---

## Requisitos

- Java 17 o superior
- Maven 3.9 o superior
- Python 3.8 o superior (solo para generar el reporte unificado de pruebas)

Verifica tu instalación antes de empezar:

```bash
java -version
mvn -v
```

## Ejecutar la aplicación

```bash
mvn spring-boot:run
```

| Recurso | URL |
|---|---|
| API | http://localhost:8080/productos |
| Consola H2 | http://localhost:8080/h2-console |

Para entrar a la consola H2: JDBC URL `jdbc:h2:mem:prodapp`, usuario `sa`, contraseña
vacía. Al ser una base de datos en memoria, **los datos se pierden al detener la
aplicación**.

## Ejecutar las pruebas

```bash
mvn clean verify
```

> ⚠️ Usa `verify`, **no** `mvn test`. Las pruebas están repartidas entre dos plugins y
> `test` solo ejecuta poco más de la mitad.

| Plugin | Clases que ejecuta | Pruebas | Se ejecutan con |
|---|---|---|---|
| surefire | `*Test` | 9 | `mvn test` y `mvn verify` |
| failsafe | `*IT` | 6 | solo `mvn verify` |

### Suite de pruebas

| Clase | Tipo | Nº | Qué valida |
|---|---|---|---|
| `ProductoRepositoryTest` | `@DataJpaTest` | 4 | Guardado, consulta por id, eliminación y carga de los datos iniciales |
| `ProductoServiceTest` | `@SpringBootTest` | 5 | Lógica de negocio y manejo de excepciones |
| `ProductoControllerIT` | `@SpringBootTest` + `MockMvc` | 6 | Los cuatro endpoints, incluidos sus códigos de error |

Las pruebas usan el perfil `test` (`application-test.properties`), que apunta a una base
de datos H2 independiente de la aplicación y la recrea en cada ejecución. Los datos
iniciales están en `src/test/resources/data.sql`.

## Reportes

Tras `mvn clean verify` se generan automáticamente:

| Reporte | Ruta |
|---|---|
| **Reporte unificado** (resumen, detalle de cada prueba y cobertura) | **`target/reporte-pruebas.html`** |
| Pruebas de repositorio y servicio | `target/reports/surefire.html` |
| Pruebas del controlador REST | `target/reports/failsafe.html` |
| Cobertura de código | `target/site/jacoco/index.html` |

El **reporte unificado** es el que conviene abrir primero: reúne en una sola página el
total de pruebas, cuántas pasaron, la duración, el detalle de cada prueba con su
descripción legible (declarada con `@DisplayName`) y la cobertura por clase. Cuando una
prueba falla, muestra además el mensaje de la aserción, sin necesidad de revisar el log
de Maven. Lo genera `tools/reporte.py` a partir de los XML de surefire y failsafe y del
CSV de JaCoCo.

Para las pruebas del controlador incluye la **evidencia real de cada petición**: el
verbo y la ruta, el cuerpo enviado, el código HTTP devuelto y el JSON de respuesta,
sin alterar ningún valor. Por ejemplo:

```
GET /productos
→ 200 OK
[
  { "id": 4, "nombre": "Laptop", "precio": 2500.00, "stock": 2 },
  { "id": 5, "nombre": "Mouse",  "precio": 80.00,   "stock": 5 }
]
```

Esa evidencia la registra la clase de apoyo `EvidenciaHttp`, enganchada a cada llamada
de MockMvc con `.andDo(...)`, que vuelca lo ocurrido en `target/evidencias.tsv`.

Cobertura actual:

| Clase | Cobertura |
|---|---|
| `ProductoService` | 100 % |
| `ProductoController` | 100 % |
| `NotFoundException` | 100 % |
| **Total del proyecto** | **80 %** |

El 20 % restante corresponde a los métodos `equals`/`hashCode` de la entidad y al
`main()` de Spring Boot: código sin lógica de negocio, cuya cobertura no aportaría valor.

## Integración continua

El workflow `.github/workflows/ci.yml` ejecuta `mvn clean verify` en cada push y pull
request, sobre una máquina Ubuntu limpia con JDK 21 y Python 3.12. Al terminar publica
todos los reportes como un artefacto descargable desde la pestaña **Actions** del
repositorio, incluso si alguna prueba falla.

---

## Estructura del proyecto

```
├─ .github/workflows/ci.yml            # Pipeline de integración continua
├─ tools/reporte.py                    # Genera el reporte unificado de pruebas
├─ pom.xml                             # Dependencias y plugins de construcción
└─ src/
   ├─ main/
   │  ├─ java/com/example/productos/
   │  │  ├─ Application.java           # Punto de entrada
   │  │  ├─ controller/                # Endpoints REST
   │  │  ├─ service/                   # Lógica de negocio y excepciones
   │  │  ├─ repository/                # Acceso a datos (Spring Data JPA)
   │  │  └─ domain/                    # Entidad Producto
   │  └─ resources/
   │     └─ application.properties     # Configuración H2 de ejecución
   └─ test/
      ├─ java/com/example/productos/   # Pruebas de las tres capas
      └─ resources/
         ├─ application-test.properties # Perfil test: H2 propia, create-drop
         └─ data.sql                    # Datos iniciales de prueba
```

## Ramas

| Rama | Propósito |
|---|---|
| `master` | Rama principal. Contiene el código estable del proyecto. |
| `pruebas-integracion` | Configuración de failsafe y perfil de test, datos iniciales, cobertura con JaCoCo y pipeline de CI. |

Flujo de trabajo recomendado:

1. Crear una rama a partir de `master` para cada tarea.
2. Confirmar los cambios y subir la rama al repositorio.
3. Abrir un pull request hacia `master`: el pipeline ejecuta las pruebas automáticamente.
4. Integrar una vez que la revisión y el pipeline estén en verde.

## Actividades pendientes

1. Agregar validaciones con Bean Validation y probar que devuelvan `400`.
2. Añadir endpoints `PUT`/`PATCH` y sus pruebas correspondientes.
