# CLAUDE.md

Este archivo proporciona guía a Claude Code (claude.ai/code) al trabajar con código en este repositorio.

## Qué es esto

`saa-backend` (artefacto `SaaBE`): un WAR de Jakarta EE 10 / Java 21 desplegado en WildFly, que da soporte a un sistema tipo ERP (contabilidad, tesorería, créditos, cuentas por cobrar/pagar, RRHH, reportes) contra una base de datos Oracle. ~2,200 archivos Java, sin fuentes de test (`src/test` está vacío y no hay ningún framework de testing configurado). El código, comentarios, documentación y mensajes de commit están en español.

## Build y ejecución

```powershell
mvn clean package          # -> target/SaaBE.war  (context root /SaaBE)
```

Nota: `mvn` **no está en el PATH** en este entorno. La compilación y el despliegue del día a día ocurren a través de Eclipse (m2e + WTP → adaptador de servidor WildFly); `recompilar-proyecto.bat` y `refresh-project.bat` existen para forzar recompilaciones/copias de recursos de Eclipse. No asumas que puedes compilar para verificar un cambio — dilo explícitamente en vez de adivinar.

- `pom.xml` también configura `wildfly-maven-plugin` (`saa-wildfly:9990`) para `mvn wildfly:deploy`.
- `docker-compose.yml` levanta Oracle 23ai Free (`saa-oracle-23ai`, puertos 1521/5500, `FREEPDB1`, contraseña `saa123`) para desarrollo local. La aplicación en sí accede a la BD a través del datasource de WildFly `java:/jdbc/SaaDS` (unidad de persistencia `SaaPU`, JTA, Hibernate `OracleDialect`).
- `config/standalone-cors.cli/standalone-cors.cli` — script de jboss-cli que instala los filtros de response-header CORS de Undertow.
- La raíz de subida de archivos viene de la propiedad de sistema `saa.upload.dir` (con fallback a una ruta según el SO bajo `user.home`), ver `basico/ejbImpl/FileServiceImpl.java`.
- No hay `web.xml`; solo `beans.xml` y `jboss-deployment-structure.xml` bajo `src/main/webapp/WEB-INF`.

## Arquitectura en capas (lo único que hay que interiorizar)

Cada tabla persistida se mapea a través de los mismos cinco archivos más una interfaz de constantes compartida. `docs/estandar/ESTANDAR_MAPEO_CAPAS.md` y `docs/estandar/GUIA-MAPEO-TABLA-COMPLETO.md` son las plantillas canónicas — seguirlas literalmente al agregar una tabla; copiar una entidad existente del mismo módulo en vez de inventar la estructura.

```
com.saa.model.{mod}.{Entidad}                     entidad JPA  (@Table(name="XXXX", schema="MOD"))
com.saa.model.{mod}.NombreEntidades{Modulo}       interface de constantes de nombres de entidad (una por módulo)
com.saa.ejb.{mod}.dao.{Entidad}DaoService         @Local     extends EntityDao<T>
com.saa.ejb.{mod}.daoImpl.{Entidad}DaoServiceImpl @Stateless extends EntityDaoImpl<T>
com.saa.ejb.{mod}.service.{Entidad}Service        @Local     extends EntityService<T>
com.saa.ejb.{mod}.serviceImpl.{Entidad}ServiceImpl @Stateless
com.saa.ws.rest.{mod}.{Entidad}Rest               @Path("xxxx")  (código de tabla, en minúsculas)
```

Módulos / schemas de BD: `cnt` (contabilidad), `crd` (créditos), `cxc` (por cobrar), `cxp` (por pagar), `tsr` (tesorería), `rhh` (RRHH), `rpr` (reportes), `scp`/`basico` (núcleo: Empresa, Usuario, Jerarquia, Rubro), `asoprep` (integración de carga de archivos Petro), `reporte` (JasperReports, transversal).

### El DAO genérico

`com.saa.basico.utilImpl.EntityDaoImpl<T>` provee `selectAll`, `selectById`, `find`, `save`, `remove`, `selectByCriteria`. Dos consecuencias que vale la pena conocer:

- `selectAll`/`selectById` ejecutan **NamedQueries resueltas por concatenación de strings**: `entidad + "All"` / `entidad + "Id"`. Así que los nombres de `@NamedQuery` en la entidad, la constante en `NombreEntidades{Modulo}`, y el valor pasado desde Service/REST deben coincidir exactamente, o se obtiene un `IllegalArgumentException` en tiempo de ejecución. `selectById` usa `getSingleResult()`, así que una fila faltante lanza `NoResultException`, no `null`.
- `selectByCriteria(List<DatosBusqueda>, entidad)` construye JPQL dinámicamente, y los **strings de operadores (`and`, `like`, `between`, paréntesis, …) se leen de la base de datos** vía `DetalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.TIPO_COMANDOS_BUSQUEDA, …)`. La búsqueda por criterios depende silenciosamente de que existan las filas del catálogo `Rubro`/`DetalleRubro`.

Cada `*DaoServiceImpl` sobreescribe `obtieneCampos()` devolviendo los nombres de campo Java de la entidad; las queries personalizadas se declaran en la interfaz DAO con JavaDoc y se implementan con un `@PersistenceContext EntityManager em` inyectado.

### Capa REST

El application path de JAX-RS es `/rest` (`com.saa.ws.rest.ApplicationConfig`), así que las URLs reales son `/SaaBE/rest/{tabla}/...` — **no** el `/api/...` que muestran algunos docs antiguos. Conjunto estándar de endpoints por entidad:

```
GET    /rest/{tabla}/getAll
GET    /rest/{tabla}/getId/{id}
POST   /rest/{tabla}                       (saveSingle)
PUT    /rest/{tabla}                       (saveSingle)
DELETE /rest/{tabla}/{id}
POST   /rest/{tabla}/selectByCriteria      (body: List<DatosBusqueda>)
```

Las clases REST inyectan tanto el DAO como el Service con `@EJB`; las rutas de lectura usualmente llaman al DAO directamente, las escrituras pasan por el Service. El estilo de error de la casa es `catch (Throwable e)` → `Response.status(INTERNAL_SERVER_ERROR).entity("Error ...: " + e.getMessage())`. Los métodos de Service declaran `throws Throwable` y lanzan `IncomeException` cuando una búsqueda no devuelve filas. Los métodos empiezan con una línea de traza `System.out.println` — mantener esa convención en código nuevo en estas capas.

Los procesos de negocio que no encajan en CRUD (cargas de archivos, conciliaciones, retenciones, anticipos) viven como métodos adicionales en el `*ServiceImpl` del módulo con `@TransactionAttribute` explícito, ej. `ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java` (`@Stateful`), `ejb/tsr/serviceImpl/ConciliacionContableMatchServiceImpl.java`. En bucles de procesamiento por lotes largos, las búsquedas del DAO deliberadamente absorben errores de BD y devuelven listas vacías/null para que una fila mala no aborte toda la ejecución (ver `docs/general/CORRECCION_MANEJO_EXCEPCIONES_DAO.md`) — preservar ese comportamiento al tocar esas rutas.

## Convenciones de nomenclatura (impulsadas por la BD)

- Tabla = 4 letras mayúsculas (`ASNT` = Asiento, `BIPR` = BaseInicialParticipes). Columna = 8 caracteres: código de tabla + código de campo de 4 letras (`ASNTCDGO`, `PRDCNMBR`).
- La PK es `XXXXCDGO`, mapeada a `Long codigo`, vía `@SequenceGenerator(sequenceName = "MOD.SQ_XXXXCDGO")` o `GenerationType.IDENTITY`.
- Tipos Oracle: `NUMBER`, `NUMBER(18,2)`, `VARCHAR2(n)`, `DATE`→`LocalDate`, `TIMESTAMP`→`LocalDateTime`. Reglas DDL en `docs/estandar/ESTANDARES-CREACION-TABLAS-ORACLE.md`.
- Las entidades/tablas `Temp*` reflejan sus contrapartes reales y almacenan documentos en progreso (CXC/CXP/TSR); `Hist*` son tablas de historial.
- `com.saa.rubros` contiene más de 120 interfaces de constantes (`Estado`, `TipoComandosBusqueda`, `EstadoAsiento`, …) — usar estas en vez de códigos literales.

## Reportes (JasperReports 7.0.3)

`POST /rest/rprt/generar` con `{modulo, nombreReporte, formato, parametros}` → `ejb/reporte/serviceImpl/ReporteServiceImpl`. Las plantillas viven en `src/main/resources/rep/{modulo}/{nombre}.jrxml`, con `.jasper` precompilados junto a ellas (ambos están en el commit; se intenta primero el `.jasper`, y el `.jrxml` se compila en tiempo de ejecución como fallback).

Esta área es frágil en formas que son fáciles de romper de nuevo:

- El llenado de reportes usa una **conexión JDBC cruda** encontrada mediante sondeo JNDI de una lista de candidatos (`java:jboss/datasources/SaaDS`, `java:/SaaDS`, `java:/jdbc/SaaDS`, …), no el `EntityManager` de JPA.
- La compilación en tiempo de ejecución fuerza `JRJaninoCompiler` e intercambia temporalmente el classloader del contexto del hilo — `JRJdtCompiler` (lo que `jasperreports.properties` todavía declara) falla dentro de WildFly debido al aislamiento de classloaders.
- Los parámetros numéricos que llegan como JSON se coercionan a los tipos declarados en el `.jasper` (`convertirTiposParametros`); un desajuste de tipos es una falla común.
- `jboss-deployment-structure.xml` excluye los módulos `org.apache.batik` / `org.apache.xml` de WildFly, y `libs-repo/` es un repositorio Maven basado en archivos que contiene el `com.saa.thirdparty:w3c-dom-css-shim` construido a mano que necesitan Batik/barcode4j. El comentario largo en `pom.xml` explica por qué un `xml-apis` completo rompe WildFly — leerlo antes de cambiar cualquiera de esas dependencias. `libs-repo/**/*.jar` está deliberadamente exceptuado de `.gitignore`.

## Docs

`docs/` es extenso y está organizado como `estandar/` (estándares de código y DDL — autoritativo), `general/` (arquitectura, changelog, ajuste de timeouts/memoria de WildFly), `logica-negocio/{cnt,crd,cxc,cxp,petro,reportes}/` (guías de API y SQL por módulo), `pendientes/`, `referencias/`, `scripts/`. Tratarlos como notas históricas: varios están desactualizados (referencian un `web.xml`, un `MultipartConfigServlet`, y rutas `/api/...` que ya no existen). Verificar contra el código fuente antes de confiar en un doc.

## Serialización

Se prefiere JSON-B sobre Jackson (`resteasy.preferJacksonOverJsonB=false` en `META-INF/microprofile-config.properties`); `jackson-datatype-jsr310` es `provided` para soporte de `java.time`. Las entidades son POJOs planos con getters/setters escritos a mano — sin Lombok, sin MapStruct, sin capa de DTO: las entidades JPA se serializan directamente a JSON.
