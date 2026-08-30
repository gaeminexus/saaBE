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

### Trampa: qué columna lleva realmente el estado

Muchas entidades tienen **dos** columnas que parecen de estado, y la que vale cambia según la tabla.
Verificar en el código antes de escribir una consulta o un `UPDATE` — elegir la equivocada devuelve
resultados vacíos o silenciosamente incorrectos.

| Entidad | Estado vigente | La otra columna | Por qué no usarla |
|---|---|---|---|
| `Prestamo` (CRD.PRST) | **`PRSTIDST`** (`idEstado`) | `ESPSCDGO` (`estadoPrestamo`) | Es la FK al catálogo `CRD.ESPS`, no el estado operativo. `ProcesoCargaPetroServiceImpl` escribe los valores del rubro `EstadoPrestamo` en `PRSTIDST`. |
| `DetallePrestamo` (CRD.DTPR) | **`DTPRESTD`** (`estado`) | `DTPRIDST` (`idEstado`) | Se escribe como copia de `estado` (`DetallePrestamoServiceImpl`) y puede quedar desfasada. |

Precedente relacionado: en `CRD.ENTD` la FK `ENTDIDST` apuntaba al **PK** del catálogo mientras el
rubro usaba el **código alterno** (`ESPRCDEX`) — ver `docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md`.
Al filtrar por estado, contrastar primero la distribución de ambas columnas contra el catálogo.

## Reportes (JasperReports 7.0.3)

`POST /rest/rprt/generar` con `{modulo, nombreReporte, formato, parametros}` → `ejb/reporte/serviceImpl/ReporteServiceImpl`. Las plantillas viven en `src/main/resources/rep/{modulo}/{nombre}.jrxml`, con `.jasper` precompilados junto a ellas (ambos están en el commit; se intenta primero el `.jasper`, y el `.jrxml` se compila en tiempo de ejecución como fallback).

Esta área es frágil en formas que son fáciles de romper de nuevo:

- El llenado de reportes usa una **conexión JDBC cruda** encontrada mediante sondeo JNDI de una lista de candidatos (`java:jboss/datasources/SaaDS`, `java:/SaaDS`, `java:/jdbc/SaaDS`, …), no el `EntityManager` de JPA.
- **⛔ NO HAY COMPILACIÓN EN TIEMPO DE EJECUCIÓN QUE FUNCIONE. Verificado el 2026-08-25 listando el contenido de los jar desplegados, y este párrafo decía lo contrario.** En JasperReports **7.0.3** `JRJaninoCompiler` **no existe** —Janino se retiró del producto— y `JRJdtCompiler` **tampoco**: en 7.x el JDT se movió al artefacto aparte `jasperreports-jdt`, que no está en el `pom`. Lo único que trae el jar es `JRJavacCompiler` y `JRJdk13Compiler`, los dos con compilador externo y sin acceso al classloader del deployment. **Cualquier valor de `net.sf.jasperreports.compiler.class` da el mismo `Could not instantiate report compiler` cambiando sólo el nombre de la clase** — se han probado los dos y se ha perdido una tarde en cada uno.
- **Por eso el `.jasper` no es opcional: es parte de añadir un reporte.** `ReporteServiceImpl:110` busca `/rep/{modulo}/{nombre}.jasper` y **sólo cae al `.jrxml` si no lo encuentra** — y ese respaldo está muerto. **Un reporte con sólo `.jrxml` compila en el IDE, pasa la revisión, entra en el commit y revienta la primera vez que un usuario lo ejecuta.** Fue exactamente lo que pasó con los siete de `rhh`, que se añadieron sin compilar mientras `cnt`, `crd`, `cxc` y `tsr` sí llevaban su par. **Al añadir o modificar un `.jrxml`, generar el `.jasper` con Jaspersoft Studio 7.0.3 —la misma versión— y commitear los dos.** Y si un reporte usara subreportes, los subreportes también van compilados: `SUBREPORT_DIR` apunta a la carpeta del módulo y el motor los carga por `.jasper`.
- Los parámetros numéricos que llegan como JSON se coercionan a los tipos declarados en el `.jasper` (`convertirTiposParametros`); un desajuste de tipos es una falla común.
- **⛔ NINGÚN `.jrxml` CONSULTA CON `SELECT *`.** Cuando dos tablas del `FROM` comparten un nombre de columna (p. ej. `TPAPCDGO` en `APRT` y en `TPAP`), Jaspersoft Studio no puede declarar dos `<field>` con el mismo nombre: renombra la segunda ocurrencia a `COLUMN_n` (`n` = posición ordinal en el resultado) y ese campo se resuelve luego **por posición**, no por nombre. Un `ALTER TABLE ... ADD` en cualquiera de las tablas del `FROM` — incluso una que el reporte ni imprime — corre todo lo que viene después una columna a la derecha, y el mapa posicional queda apuntando a la columna equivocada: `JRException: Unable to get value for result set field "COLUMN_n"` (o peor, ningún error, y el reporte imprime el dato de otra columna). Pasó el 2026-08-27 con `RPRT_MVMN_APXT.jrxml`: el DDL de `CRD.APRT` agregó `APRTPRDV`/`APRTTPMV` y corrió dos posiciones todo lo que el `SELECT *` traía de `CRD.TPAP` y `CRD.ENTD`, con cero avisos hasta que un usuario lo ejecutó. **Al escribir o corregir la `query` de un `.jrxml`: listar las columnas explícitamente, y cuando dos tablas comparten un nombre, alias la segunda a algo único (`t.TPAPCDGO AS TPAP_TPAPCDGO`) en vez de dejar que Jaspersoft Studio invente un `COLUMN_n`.** Si un `.jrxml` ya tiene campos `COLUMN_n`, no basta con renumerarlos — sigue igual de frágil ante el próximo `ALTER`; hay que reemplazar el `SELECT *` por la lista explícita y renombrar esos campos a algo real (verificando primero en el layout qué columna es cada uno, no adivinando).
- `jboss-deployment-structure.xml` excluye los módulos `org.apache.batik` / `org.apache.xml` de WildFly, y `libs-repo/` es un repositorio Maven basado en archivos que contiene el `com.saa.thirdparty:w3c-dom-css-shim` construido a mano que necesitan Batik/barcode4j. El comentario largo en `pom.xml` explica por qué un `xml-apis` completo rompe WildFly — leerlo antes de cambiar cualquiera de esas dependencias. `libs-repo/**/*.jar` está deliberadamente exceptuado de `.gitignore`.

## Docs

`docs/` es extenso y está organizado como `estandar/` (estándares de código y DDL — autoritativo), `general/` (arquitectura, changelog, ajuste de timeouts/memoria de WildFly), `logica-negocio/{cnt,crd,cxc,cxp,petro,reportes}/` (guías de API y SQL por módulo), `pendientes/`, `referencias/`, `scripts/`. Tratarlos como notas históricas: varios están desactualizados (referencian un `web.xml`, un `MultipartConfigServlet`, y rutas `/api/...` que ya no existen). Verificar contra el código fuente antes de confiar en un doc.

### Excepción: los procesos Petro tienen docs consolidados y mantenidos

Para todo lo relacionado con los archivos Petrocomercial/ASOPREP (carga de descuentos, aplicación de pagos, generación del archivo), **leer PRIMERO** estos tres documentos — son el resumen vigente verificado contra el código (2026-08-13) y resuelven las contradicciones de los `CORRECCION-*`/`REVISION_*` históricos de esa carpeta:

- `docs/logica-negocio/petro/REGLAS-GENERALES-PETRO.md` — ciclo completo, tablas, catálogos de rubros vigentes, endpoints, tolerancias, trampas.
- `docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md` — fases 1/2/3 de `CargaArchivoPetroServiceImpl` (asoprep).
- `docs/logica-negocio/petro/REGLAS-GENERACION-PETRO.md` — `GeneracionArchivoPetroServiceImpl` (crd), formatos por filial, ciclo GNAP.

Del mismo modo, para pagos de préstamos (pago de cuotas, abono a capital, pago con aportes, precancelación, reverso) la referencia vigente es `docs/logica-negocio/crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md` (especificación de diseño 2026-08-13, pendiente de implementación por fases).

**Regla obligatoria:** cualquier cambio en `CargaArchivoPetroServiceImpl`, `ProcesoCargaPetroServiceImpl`, `GeneracionArchivoPetroServiceImpl` o sus DAOs/REST asociados debe actualizar el documento correspondiente en el mismo cambio. Los ~29 `.md` históricos de esa carpeta (`CORRECCION-*`, `REVISION_*`, etc.) se eliminaron el 2026-08-13 al consolidarse aquí; si hace falta consultarlos están en el historial de git. Se conservan la carpeta `sql/` (DDL), los `.txt` de muestra del formato real y la guía frontend.

## Serialización

`META-INF/microprofile-config.properties` declara `resteasy.preferJacksonOverJsonB=false`, **pero esa propiedad no está surtiendo efecto: quien serializa es Jackson.** Verificado sobre el cable el 2026-08-20 — las respuestas traen `[2026,8,10]` y `[2026,8,20,9,36,47,579023000]`, que es el formato de arreglo de Jackson; Yasson emitiría `"2026-08-10"`. Y en la entrada el servidor acepta las dos formas, ISO y arreglo, mientras que JSON-B rechaza el arreglo. **El mecanismo exacto lo dice WildFly en el log del arranque**, `WFLYRS0018`: *«Explicit usage of Jackson annotation in a Jakarta RESTful Web Services deployment; the system will disable Jakarta JSON Binding processing»*. Basta **una anotación de Jackson en cualquier parte del código** para que WildFly desactive JSON-B en todo el despliegue. La propiedad de MicroProfile no tiene nada que hacer contra eso — y sin `web.xml` tampoco hay `context-param` donde se leería. Coherente además con que el WAR empaquete su propio Jackson 2.18.2 en `WEB-INF/lib/` y ninguna dependencia de Yasson.

Consecuencia práctica: **volver a JSON-B no es cambiar una propiedad, es retirar todas las anotaciones de Jackson del proyecto** y después migrar el formato de todas las fechas en las dos direcciones.

**No cambiar el proveedor sin un plan.** Pasar a JSON-B cambiaría el formato de **todas** las fechas del sistema, en todos los módulos, en las dos direcciones. Es una migración, no un ajuste de configuración.

**Trampa de `LocalDateTime`, y es silenciosa:** Jackson acepta un instante con zona y **descarta el offset en vez de convertirlo** — `"2026-08-20T13:30:00.000Z"` se graba como `13:30`. Un `Date` de JavaScript de las 08:30 en Ecuador viaja como `13:30Z` y queda cinco horas adelantado sin ningún error. **Regla para los clientes: `LocalDate` va como `yyyy-MM-dd` y `LocalDateTime` como ISO local sin zona; nunca un `Date` crudo ni nada terminado en `Z`.**

`jackson-datatype-jsr310` es `provided` para soporte de `java.time`. Las entidades son POJOs planos con getters/setters escritos a mano — sin Lombok, sin MapStruct, sin capa de DTO: las entidades JPA se serializan directamente a JSON.
