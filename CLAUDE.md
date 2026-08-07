# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`saa-backend` (artifact `SaaBE`): a Jakarta EE 10 / Java 21 WAR deployed to WildFly, backing an ERP-style system (accounting, treasury, credit, AR/AP, HR, reports) against an Oracle database. ~2,200 Java files, no test sources (`src/test` is empty and no test framework is configured). Code, comments, docs and commit messages are in Spanish.

## Build & run

```powershell
mvn clean package          # -> target/SaaBE.war  (context root /SaaBE)
```

Note: `mvn` is **not on PATH** in this environment. Day-to-day compilation and deployment happen through Eclipse (m2e + WTP → WildFly server adapter); `recompilar-proyecto.bat` and `refresh-project.bat` exist to force Eclipse rebuilds/resource copies. Don't assume you can compile to verify a change — say so instead of guessing.

- `pom.xml` also configures `wildfly-maven-plugin` (`saa-wildfly:9990`) for `mvn wildfly:deploy`.
- `docker-compose.yml` starts Oracle 23ai Free (`saa-oracle-23ai`, ports 1521/5500, `FREEPDB1`, pwd `saa123`) for local development. The app itself reaches the DB through the WildFly datasource `java:/jdbc/SaaDS` (persistence unit `SaaPU`, JTA, Hibernate `OracleDialect`).
- `config/standalone-cors.cli/standalone-cors.cli` — jboss-cli script that installs the Undertow CORS response-header filters.
- Uploads root comes from the `saa.upload.dir` system property (falls back to a per-OS path under `user.home`), see `basico/ejbImpl/FileServiceImpl.java`.
- There is no `web.xml`; only `beans.xml` and `jboss-deployment-structure.xml` under `src/main/webapp/WEB-INF`.

## Layered architecture (the one thing to internalize)

Every persisted table is mapped through the same five files plus a shared constants interface. `docs/estandar/ESTANDAR_MAPEO_CAPAS.md` and `docs/estandar/GUIA-MAPEO-TABLA-COMPLETO.md` are the canonical templates — follow them literally when adding a table; copy an existing entity of the same module rather than inventing structure.

```
com.saa.model.{mod}.{Entidad}                     JPA entity  (@Table(name="XXXX", schema="MOD"))
com.saa.model.{mod}.NombreEntidades{Modulo}       interface of entity-name constants (one per module)
com.saa.ejb.{mod}.dao.{Entidad}DaoService         @Local     extends EntityDao<T>
com.saa.ejb.{mod}.daoImpl.{Entidad}DaoServiceImpl @Stateless extends EntityDaoImpl<T>
com.saa.ejb.{mod}.service.{Entidad}Service        @Local     extends EntityService<T>
com.saa.ejb.{mod}.serviceImpl.{Entidad}ServiceImpl @Stateless
com.saa.ws.rest.{mod}.{Entidad}Rest               @Path("xxxx")  (table code, lowercase)
```

Modules / DB schemas: `cnt` (contabilidad), `crd` (créditos), `cxc` (por cobrar), `cxp` (por pagar), `tsr` (tesorería), `rhh` (RRHH), `rpr` (reportes), `scp`/`basico` (core: Empresa, Usuario, Jerarquia, Rubro), `asoprep` (Petro file-load integration), `reporte` (JasperReports, transversal).

### The generic DAO

`com.saa.basico.utilImpl.EntityDaoImpl<T>` supplies `selectAll`, `selectById`, `find`, `save`, `remove`, `selectByCriteria`. Two consequences worth knowing:

- `selectAll`/`selectById` run **NamedQueries resolved by string concatenation**: `entidad + "All"` / `entidad + "Id"`. So the `@NamedQuery` names on the entity, the constant in `NombreEntidades{Modulo}`, and the value passed from Service/REST must match exactly, or you get a runtime `IllegalArgumentException`. `selectById` uses `getSingleResult()`, so a missing row throws `NoResultException`, not `null`.
- `selectByCriteria(List<DatosBusqueda>, entidad)` builds JPQL dynamically, and the **operator strings (`and`, `like`, `between`, parentheses, …) are read from the database** via `DetalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.TIPO_COMANDOS_BUSQUEDA, …)`. Criteria search silently depends on the `Rubro`/`DetalleRubro` catalog rows being present.

Each `*DaoServiceImpl` overrides `obtieneCampos()` returning the entity's Java field names; custom queries are declared on the DAO interface with JavaDoc and implemented with an injected `@PersistenceContext EntityManager em`.

### REST layer

JAX-RS application path is `/rest` (`com.saa.ws.rest.ApplicationConfig`), so live URLs are `/SaaBE/rest/{tabla}/...` — **not** the `/api/...` shown in some older docs. Standard endpoint set per entity:

```
GET    /rest/{tabla}/getAll
GET    /rest/{tabla}/getId/{id}
POST   /rest/{tabla}                       (saveSingle)
PUT    /rest/{tabla}                       (saveSingle)
DELETE /rest/{tabla}/{id}
POST   /rest/{tabla}/selectByCriteria      (body: List<DatosBusqueda>)
```

REST classes inject both the DAO and the Service with `@EJB`; read paths usually call the DAO directly, writes go through the Service. The house error style is `catch (Throwable e)` → `Response.status(INTERNAL_SERVER_ERROR).entity("Error ...: " + e.getMessage())`. Service methods declare `throws Throwable` and throw `IncomeException` when a search returns no rows. Methods start with a `System.out.println` tracing line — keep that convention in new code in these layers.

Business processes that don't fit CRUD (file loads, conciliations, retentions, anticipos) live as extra methods on the module's `*ServiceImpl` with explicit `@TransactionAttribute`, e.g. `ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java` (`@Stateful`), `ejb/tsr/serviceImpl/ConciliacionContableMatchServiceImpl.java`. In long batch loops, DAO lookups deliberately swallow DB errors and return empty lists/null so one bad row doesn't abort the run (see `docs/general/CORRECCION_MANEJO_EXCEPCIONES_DAO.md`) — preserve that behaviour when touching those paths.

## Naming conventions (DB-driven)

- Table = 4 uppercase letters (`ASNT` = Asiento, `BIPR` = BaseInicialParticipes). Column = 8 chars: table code + 4-letter field code (`ASNTCDGO`, `PRDCNMBR`).
- PK is `XXXXCDGO`, mapped to `Long codigo`, via `@SequenceGenerator(sequenceName = "MOD.SQ_XXXXCDGO")` or `GenerationType.IDENTITY`.
- Oracle types: `NUMBER`, `NUMBER(18,2)`, `VARCHAR2(n)`, `DATE`→`LocalDate`, `TIMESTAMP`→`LocalDateTime`. DDL rules in `docs/estandar/ESTANDARES-CREACION-TABLAS-ORACLE.md`.
- `Temp*` entities/tables mirror their real counterparts and stage in-progress documents (CXC/CXP/TSR); `Hist*` are history tables.
- `com.saa.rubros` holds 120+ constant interfaces (`Estado`, `TipoComandosBusqueda`, `EstadoAsiento`, …) — use these instead of literal codes.

## Reports (JasperReports 7.0.3)

`POST /rest/rprt/generar` with `{modulo, nombreReporte, formato, parametros}` → `ejb/reporte/serviceImpl/ReporteServiceImpl`. Templates live in `src/main/resources/rep/{modulo}/{nombre}.jrxml`, with pre-compiled `.jasper` next to them (both are committed; the `.jasper` is tried first, and the `.jrxml` is compiled at runtime as fallback).

This area is fragile in ways that are easy to re-break:

- Report filling uses a **raw JDBC connection** found by JNDI probing a candidate list (`java:jboss/datasources/SaaDS`, `java:/SaaDS`, `java:/jdbc/SaaDS`, …), not the JPA `EntityManager`.
- Runtime compilation forces `JRJaninoCompiler` and temporarily swaps the thread context classloader — `JRJdtCompiler` (what `jasperreports.properties` still declares) fails inside WildFly due to classloader isolation.
- Numeric parameters arriving as JSON are coerced to the types declared in the `.jasper` (`convertirTiposParametros`); a type mismatch is a common failure.
- `jboss-deployment-structure.xml` excludes the WildFly `org.apache.batik` / `org.apache.xml` modules, and `libs-repo/` is a file-based Maven repo holding the hand-built `com.saa.thirdparty:w3c-dom-css-shim` needed by Batik/barcode4j. The long comment in `pom.xml` explains why a full `xml-apis` breaks WildFly — read it before changing any of those dependencies. `libs-repo/**/*.jar` is deliberately exempted from `.gitignore`.

## Docs

`docs/` is extensive and organized as `estandar/` (coding & DDL standards — authoritative), `general/` (architecture, changelog, WildFly timeout/memory tuning), `logica-negocio/{cnt,crd,cxc,cxp,petro,reportes}/` (per-module API guides and SQL), `pendientes/`, `referencias/`, `scripts/`. Treat them as historical notes: several are stale (they reference a `web.xml`, a `MultipartConfigServlet`, and `/api/...` routes that no longer exist). Verify against source before relying on a doc.

## Serialization

JSON-B is preferred over Jackson (`resteasy.preferJacksonOverJsonB=false` in `META-INF/microprofile-config.properties`); `jackson-datatype-jsr310` is `provided` for `java.time` support. Entities are plain POJOs with hand-written getters/setters — no Lombok, no MapStruct, no DTO layer: JPA entities are serialized straight to JSON.
