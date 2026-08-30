# API de Productos — Pruebas de Integración con Spring Boot

[![CI](https://github.com/Stiven-Valencia/Automatizaci-n-de-pruebas-13-/actions/workflows/ci.yml/badge.svg)](https://github.com/Stiven-Valencia/Automatizaci-n-de-pruebas-13-/actions/workflows/ci.yml)

API REST para la gestión de productos, construida con Spring Boot 3.5 y base de datos
H2 en memoria. El proyecto es un caso de estudio de **pruebas de integración
automatizadas**: cubre sus tres capas —controlador, servicio y repositorio— con 15
pruebas que se ejecutan en cada cambio y producen evidencia verificable de lo que la
API respondió.

**Contenido:** [Qué hace](#qué-hace-la-aplicación) · [Arquitectura](#arquitectura) ·
[Requisitos](#requisitos) · [Ejecución](#ejecutar-la-aplicación) ·
[Pruebas](#estrategia-de-pruebas) · [Reportes](#reportes) ·
[Integración continua](#integración-continua) · [Convenciones](#convenciones-del-proyecto)

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

Reglas de negocio, implementadas en `ProductoService`:

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
> excepción a una respuesta HTTP adecuada.

---

## Arquitectura

La aplicación sigue la separación clásica en tres capas. Cada petición las atraviesa
en orden y cada capa tiene una única responsabilidad:

```
   HTTP                                            
    │                                              
    ▼                                              
┌─────────────────────┐                            
│ ProductoController  │  Traduce HTTP ↔ objetos:   
│ @RestController     │  lee el cuerpo, elige el   
│                     │  código de respuesta y     
│                     │  convierte NotFoundException
│                     │  en un 404                 
└──────────┬──────────┘                            
           ▼                                       
┌─────────────────────┐                            
│ ProductoService     │  Reglas de negocio:        
│ @Service            │  valida precio y stock,    
│ @Transactional      │  lanza NotFoundException   
└──────────┬──────────┘                            
           ▼                                       
┌─────────────────────┐                            
│ ProductoRepository  │  Acceso a datos, sin SQL   
│ JpaRepository       │  escrito a mano            
└──────────┬──────────┘                            
           ▼                                       
┌─────────────────────┐                            
│ H2 en memoria       │  Se crea al arrancar y     
│ Entidad Producto    │  se pierde al detener      
└─────────────────────┘                            
```

El punto clave del diseño: **las excepciones son del dominio, no de HTTP**. El servicio
lanza `NotFoundException` sin saber que existe un código 404; es el controlador quien
decide esa traducción. Por eso el servicio se puede probar sin levantar un servidor web.

---

## Requisitos

- Java 17 o superior
- Maven 3.9 o superior
- Python 3.8 o superior (solo para generar el reporte unificado de pruebas)

Verifica tu instalación antes de empezar:

```bash
java -version
mvn -v
python --version
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

---

## Estrategia de pruebas

```bash
mvn clean verify
```

> ⚠️ Usa `verify`, **no** `mvn test`. Las pruebas están repartidas entre dos plugins y
> `test` solo ejecuta poco más de la mitad.

| Plugin | Clases que ejecuta | Pruebas | Se ejecutan con |
|---|---|---|---|
| surefire | `*Test` | 9 | `mvn test` y `mvn verify` |
| failsafe | `*IT` | 6 | solo `mvn verify` |

Esta separación es deliberada: surefire es el corredor de pruebas rápidas y failsafe el
de pruebas de integración, que además garantiza que se ejecute la fase de limpieza
aunque alguna falle.

### Los tres niveles

Cada nivel prueba una cosa distinta y usa la herramienta adecuada para ella:

| Clase | Anotación | Nº | Qué prueba y cómo |
|---|---|---|---|
| `ProductoRepositoryTest` | `@DataJpaTest` | 4 | Persistencia real contra H2. Levanta **solo** la capa JPA, no el contexto completo, así que arranca en milisegundos |
| `ProductoServiceTest` | `@SpringBootTest` | 5 | Reglas de negocio y excepciones con el contexto completo de Spring, sin pasar por HTTP |
| `ProductoControllerIT` | `@SpringBootTest` + `@AutoConfigureMockMvc` | 6 | Los cuatro endpoints extremo a extremo. MockMvc simula las peticiones sin abrir un puerto real |

### Aislamiento entre pruebas

Tres mecanismos garantizan que ninguna prueba dependa de otra ni del orden en que se
ejecuten:

1. **Perfil `test` propio.** Las pruebas usan `application-test.properties`, que apunta
   a la base `prodapp_test` —distinta de la de la aplicación— con `ddl-auto=create-drop`:
   el esquema se crea al arrancar y se destruye al terminar.
2. **Rollback automático.** Las tres clases son transaccionales, así que todo lo que una
   prueba escribe se deshace al terminar. La siguiente encuentra la base igual que la
   dejó el arranque.
3. **Datos iniciales conocidos.** `src/test/resources/data.sql` inserta tres productos al
   crear el contexto, y `@BeforeEach` prepara los datos específicos de cada clase.

> **Detalle de configuración:** el `data.sql` requiere
> `spring.jpa.defer-datasource-initialization=true`. Sin esa propiedad, Spring ejecuta
> el script **antes** de que Hibernate cree las tablas y falla con *"Table not found"*.

---

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
descripción legible y la cobertura por clase. Cuando una prueba falla, muestra además el
mensaje de la aserción, sin necesidad de revisar el log de Maven. Lo genera
`tools/reporte.py` a partir de los XML de surefire y failsafe y del CSV de JaCoCo.

Para las pruebas del controlador incluye la **evidencia real de cada petición**: el verbo
y la ruta, el cuerpo enviado, el código HTTP devuelto y el JSON de respuesta, sin alterar
ningún valor:

```
GET /productos
→ 200 OK
[
  { "id": 4, "nombre": "Laptop", "precio": 2500.00, "stock": 2 },
  { "id": 5, "nombre": "Mouse",  "precio": 80.00,   "stock": 5 }
]
```

Esa evidencia la registra la clase de apoyo `EvidenciaHttp`, enganchada a cada llamada de
MockMvc con `.andDo(...)` **antes** de las aserciones: así queda registrada la respuesta
aunque la prueba falle después.

Las pruebas de repositorio y de servicio muestran su equivalente —el objeto devuelto o la
excepción lanzada— mediante la clase `Evidencia`:

```
repository.findAll()
→ [
  { id=1, nombre="cable test",  precio=1000.00,  stock=10 },
  { id=2, nombre="cable test2", precio=20000.00, stock=20 },
  { id=3, nombre="cable test3", precio=30000.00, stock=30 }
]

service.eliminar(999)
→ NotFoundException: Producto no encontrado: 999
```

De este modo ninguna fila del reporte se sostiene solo en su descripción: las 15 pruebas
muestran qué devolvió realmente el código.

### Cobertura

| Clase | Cobertura |
|---|---|
| `ProductoService` | 100 % |
| `ProductoController` | 100 % |
| `NotFoundException` | 100 % |
| `Producto` | 53 % |
| `Application` | 33 % |
| **Total del proyecto** | **80 %** |

La lógica de negocio está cubierta al 100 %. El resto son los `equals`/`hashCode` de la
entidad y el `main()` de Spring Boot: código sin decisiones, cuya cobertura subiría el
porcentaje sin proteger de ningún error real.

---

## Integración continua

El workflow `.github/workflows/ci.yml` ejecuta `mvn clean verify` en cada push y pull
request, sobre una máquina Ubuntu limpia con JDK 21 y Python 3.12. Al terminar publica
todos los reportes como un artefacto descargable desde la pestaña **Actions** del
repositorio, incluso si alguna prueba falla.

Que el entorno sea limpio en cada ejecución es parte del valor: descarta los fallos del
tipo "en mi máquina funciona", porque ahí no existe nada instalado localmente.

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
      ├─ java/com/example/productos/
      │  ├─ repository/ service/ controller/   # Pruebas de las tres capas
      │  └─ soporte/                            # Registro de evidencias
      │     ├─ Evidencia.java                   # Objetos y excepciones devueltos
      │     └─ EvidenciaHttp.java               # Peticiones y respuestas HTTP
      └─ resources/
         ├─ application-test.properties # Perfil test: H2 propia, create-drop
         └─ data.sql                    # Datos iniciales de prueba
```

---

## Convenciones del proyecto

Reglas a respetar al añadir código, para que la suite y los reportes sigan funcionando:

**Nombres de las clases de prueba.** El sufijo decide qué plugin la ejecuta, así que no
es decorativo:

- `...Test` → repositorio y servicio (surefire)
- `...IT` → pruebas que atraviesan la API REST (failsafe)

Una prueba de API llamada `...Test` se ejecutaría en la fase equivocada; una de servicio
llamada `...IT` no se ejecutaría con `mvn test`.

**Cada prueba lleva `@DisplayName`** con una frase que describa qué valida. Ese texto es
el que aparece en el reporte; sin él se muestra el nombre del método, que se lee peor.

**Estructura del cuerpo:** patrón Arrange / Act / Assert, y aserciones con AssertJ
(`assertThat(...)`), como el resto de la suite.

**Registra la evidencia.** Toda prueba debe dejar constancia de lo que obtuvo:

- En MockMvc, encadena `.andDo(EvidenciaHttp.registrar(pruebaActual))` justo después del
  `perform(...)` y **antes** de las aserciones.
- En repositorio y servicio, llama a `Evidencia.registrar(pruebaActual, operacion, resultado)`
  con el objeto o la excepción obtenidos. Para capturar una excepción usa `catchThrowable(...)`
  de AssertJ en lugar de `assertThatThrownBy(...)`, y asegura después sobre ella.

**Prefiere aserciones robustas.** `hasSizeGreaterThanOrEqualTo(3)` en lugar de un tamaño
exacto, y `containsInAnyOrder(...)` en lugar de comparar por posición: el orden de
`findAll()` no está garantizado y una prueba frágil termina borrándose.

**No persigas el 100 % de cobertura.** Cubre las decisiones —condiciones, validaciones,
manejo de errores—, no los getters ni el `main()`.

---

## Ramas

| Rama | Propósito |
|---|---|
| `master` | Rama principal. Contiene el código estable del proyecto. |
| `pruebas-integracion` | Configuración de failsafe y perfil de test, datos iniciales, cobertura con JaCoCo, reporte unificado y pipeline de CI. |

Flujo de trabajo:

1. Crear una rama a partir de `master` para cada tarea.
2. Confirmar los cambios y subir la rama al repositorio.
3. Abrir un pull request hacia `master`: el pipeline ejecuta las pruebas automáticamente.
4. Integrar una vez que la revisión y el pipeline estén en verde.
