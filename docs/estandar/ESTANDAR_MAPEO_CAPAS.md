# Estándar de Mapeo por Capas - saaBE

## Descripción General

El proyecto sigue una arquitectura en capas: **Model → DAO → Service → REST**.  
Cada módulo tiene su propio paquete en cada capa. Los módulos existentes son:
`cnt`, `crd`, `cxc`, `cxp`, `reporte`, `rhh`, `rpr`, `scp`, `tsr`, `asoprep`.

El nuevo módulo a mapear es **`rpr`** (Reporte), con schema de base de datos `RPR`.  
Los paquetes ya existen vacíos y listos para ser llenados:

| Capa | Paquete |
|---|---|
| Model | `com.saa.model.rpr` |
| DAO Interface | `com.saa.ejb.rpr.dao` |
| DAO Impl | `com.saa.ejb.rpr.daoImpl` |
| Service Interface | `com.saa.ejb.rpr.service` |
| Service Impl | `com.saa.ejb.rpr.serviceImpl` |
| REST | `com.saa.ws.rest.rpr` |

---

## Rutas de los archivos fuente

```
src/main/java/com/saa/
├── model/
│   └── rpr/
│       ├── NombreEntidadesReporte.java       ← Archivo especial (ver sección 2)
│       └── NombreEntidad.java                ← Una por tabla
├── ejb/
│   └── rpr/
│       ├── dao/
│       │   └── NombreEntidadDaoService.java
│       ├── daoImpl/
│       │   └── NombreEntidadDaoServiceImpl.java
│       ├── service/
│       │   └── NombreEntidadService.java
│       └── serviceImpl/
│           └── NombreEntidadServiceImpl.java
└── ws/
    └── rest/
        └── rpr/
            └── NombreEntidadRest.java
```

---

## Clases base que se extienden / implementan

| Clase base | Ubicación | Descripción |
|---|---|---|
| `EntityDao<T>` | `com.saa.basico.util.EntityDao` | Interface DAO genérica |
| `EntityDaoImpl<T>` | `com.saa.basico.utilImpl.EntityDaoImpl` | Implementación DAO genérica |
| `EntityService<T>` | `com.saa.basico.util.EntityService` | Interface Service genérica |
| `DatosBusqueda` | `com.saa.basico.util.DatosBusqueda` | Objeto de criterios de búsqueda |
| `IncomeException` | `com.saa.basico.util.IncomeException` | Excepción propia del sistema |

### Métodos provistos por `EntityDaoImpl` (no hay que reimplementar)
- `selectAll(String entidad)` → usa NamedQuery `EntidadAll`
- `selectById(Long id, String entidad)` → usa NamedQuery `EntidadId`
- `save(Tipo tipo, Long id)` → insert o update
- `find(Tipo clase, Long id)` → busca por PK
- `remove(Tipo clase, Long id)` → elimina por PK
- `selectByCriteria(List<DatosBusqueda> datos, String entidad)` → búsqueda dinámica

### Métodos requeridos por `EntityService<T>`
- `void remove(List<Long> id)`
- `void save(List<T> object)`
- `T saveSingle(T object)`
- `List<T> selectAll()`
- `T selectById(Long id)`
- `List<T> selectByCriteria(List<DatosBusqueda> datos)`

---

## 1. Model — Entidad JPA

**Archivo:** `com.saa.model.rpr.NombreEntidad`

### Anotaciones obligatorias en la clase
```java
@SuppressWarnings("serial")
@Entity
@Table(name = "XXXX", schema = "RPR")
@SequenceGenerator(name = "SQ_XXXXCDGO", sequenceName = "RPR.SQ_XXXXCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "NombreEntidadAll", query = "select e from NombreEntidad e"),
    @NamedQuery(name = "NombreEntidadId",  query = "select e from NombreEntidad e where e.codigo = :id")
})
public class NombreEntidad implements Serializable {
```

> ⚠️ El nombre en los `@NamedQuery` debe coincidir EXACTAMENTE con la constante en `NombreEntidadesReporte`.

### Campo PK estándar (con secuencia)
```java
@Basic
@Id
@Column(name = "XXXXCDGO", precision = 0)
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_XXXXCDGO")
private Long codigo;
```

### Campo PK alternativo (con IDENTITY, cuando la tabla no tiene secuencia propia)
```java
@Id
@Basic
@Column(name = "XXXXCDGO", precision = 0)
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long codigo;
```

### Tipos de campos
| Tipo Oracle | Tipo Java | Ejemplo anotación |
|---|---|---|
| `NUMBER` | `Long` | `@Column(name = "XXXXCDGO")` |
| `NUMBER` (decimal) | `Double` o `BigDecimal` | `@Column(name = "XXXXVLOR")` |
| `VARCHAR2(N)` | `String` | `@Column(name = "XXXXNMBR", length = N)` |
| `DATE` | `LocalDate` | `@Column(name = "XXXXFCHA")` |
| `TIMESTAMP` | `LocalDateTime` | `@Column(name = "XXXXFCPR")` |
| FK a otra tabla | Objeto referenciado | `@ManyToOne` + `@JoinColumn` |

### Relaciones FK
```java
@ManyToOne
@JoinColumn(name = "XXXXCDGO", referencedColumnName = "XXXXCDGO")
private OtraEntidad otraEntidad;
```

### Imports comunes para el model
```java
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
```

---

## 2. NombreEntidadesReporte — Archivo especial del módulo

**Archivo:** `com.saa.model.rpr.NombreEntidadesReporte`

Es una **interface** (no clase) que centraliza las constantes con los nombres de las entidades.  
Se usa en el Service y en el REST para invocar los NamedQuery. Una constante por cada entidad del módulo.

```java
package com.saa.model.rpr;

public interface NombreEntidadesReporte {
    String NOMBRE_ENTIDAD_1 = "NombreEntidad1";
    String NOMBRE_ENTIDAD_2 = "NombreEntidad2";
    // ... una entrada por cada entidad del módulo rpr
}
```

> **Equivalente en otros módulos:**
> - `com.saa.model.crd.NombreEntidadesCredito`
> - `com.saa.model.cnt.NombreEntidadesContabilidad` (si existe)

---

## 3. DAO Interface

**Archivo:** `com.saa.ejb.rpr.dao.NombreEntidadDaoService`

Si la entidad no necesita queries propias, la interface queda vacía (solo extiende):

```java
package com.saa.ejb.rpr.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.NombreEntidad;
import jakarta.ejb.Local;

@Local
public interface NombreEntidadDaoService extends EntityDao<NombreEntidad> {

}
```

Si necesita queries propias, se declaran aquí con su documentación JavaDoc:

```java
@Local
public interface NombreEntidadDaoService extends EntityDao<NombreEntidad> {

    /**
     * Recupera registros por empresa.
     * @param empresa : Id de la empresa
     * @return        : Listado de resultados
     * @throws Throwable : Excepcion
     */
    List<NombreEntidad> selectByEmpresa(Long empresa) throws Throwable;
}
```

---

## 4. DAO Impl

**Archivo:** `com.saa.ejb.rpr.daoImpl.NombreEntidadDaoServiceImpl`

Si no hay queries propias:

```java
package com.saa.ejb.rpr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.NombreEntidadDaoService;
import com.saa.model.rpr.NombreEntidad;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class NombreEntidadDaoServiceImpl extends EntityDaoImpl<NombreEntidad>
        implements NombreEntidadDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "campo1",
            "campo2"
            // ... todos los campos Java de la entidad
        };
    }
}
```

Si hay queries propias se agregan con `@PersistenceContext EntityManager em` y se usan `Query`:

```java
@Override
public List<NombreEntidad> selectByEmpresa(Long empresa) throws Throwable {
    System.out.println("Ingresa al metodo selectByEmpresa con empresa: " + empresa);
    Query query = em.createQuery(
        " select b from NombreEntidad b " +
        " where  b.empresa.codigo = :empresa");
    query.setParameter("empresa", empresa);
    return query.getResultList();
}
```

> ⚠️ Siempre agregar `System.out.println` al inicio de cada método con los parámetros recibidos.

---

## 5. Service Interface

**Archivo:** `com.saa.ejb.rpr.service.NombreEntidadService`

Si no hay métodos propios adicionales:

```java
package com.saa.ejb.rpr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.NombreEntidad;
import jakarta.ejb.Local;

@Local
public interface NombreEntidadService extends EntityService<NombreEntidad> {

}
```

Si hay métodos propios, se declaran aquí con JavaDoc.

---

## 6. Service Impl

**Archivo:** `com.saa.ejb.rpr.serviceImpl.NombreEntidadServiceImpl`

```java
package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.NombreEntidadDaoService;
import com.saa.ejb.rpr.service.NombreEntidadService;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NombreEntidad;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NombreEntidadServiceImpl implements NombreEntidadService {

    @EJB
    private NombreEntidadDaoService nombreEntidadDaoService;

    @Override
    public NombreEntidad selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById NombreEntidad con id: " + id);
        return nombreEntidadDaoService.selectById(id, NombreEntidadesReporte.NOMBRE_ENTIDAD);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de NombreEntidadService");
        NombreEntidad entidad = new NombreEntidad();
        for (Long registro : id) {
            nombreEntidadDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<NombreEntidad> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de NombreEntidadService");
        for (NombreEntidad registro : lista) {
            nombreEntidadDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<NombreEntidad> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll NombreEntidadService");
        List<NombreEntidad> result = nombreEntidadDaoService.selectAll(NombreEntidadesReporte.NOMBRE_ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total NombreEntidad no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public NombreEntidad saveSingle(NombreEntidad entidad) throws Throwable {
        System.out.println("saveSingle - NombreEntidad");
        if (entidad.getCodigo() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        entidad = nombreEntidadDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<NombreEntidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria NombreEntidadService");
        List<NombreEntidad> result = nombreEntidadDaoService.selectByCriteria(datos, NombreEntidadesReporte.NOMBRE_ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NombreEntidad no devolvio ningun registro");
        }
        return result;
    }
}
```

> ⚠️ Si la entidad no tiene campo `estado`, omitir el bloque `setEstado` en `saveSingle`.

---

## 7. REST

**Archivo:** `com.saa.ws.rest.rpr.NombreEntidadRest`

```java
package com.saa.ws.rest.rpr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.NombreEntidadDaoService;
import com.saa.ejb.rpr.service.NombreEntidadService;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.NombreEntidad;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("xxxx")   // nombre de la tabla en minúsculas
public class NombreEntidadRest {

    @EJB
    private NombreEntidadDaoService nombreEntidadDaoService;

    @EJB
    private NombreEntidadService nombreEntidadService;

    @Context
    private UriInfo context;

    public NombreEntidadRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NombreEntidad> lista = nombreEntidadDaoService.selectAll(NombreEntidadesReporte.NOMBRE_ENTIDAD);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NombreEntidad entidad = nombreEntidadDaoService.selectById(id, NombreEntidadesReporte.NOMBRE_ENTIDAD);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("NombreEntidad con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NombreEntidad registro) {
        System.out.println("LLEGA AL SERVICIO PUT NombreEntidad");
        try {
            NombreEntidad resultado = nombreEntidadService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NombreEntidad registro) {
        System.out.println("LLEGA AL SERVICIO POST NombreEntidad");
        try {
            NombreEntidad resultado = nombreEntidadService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE NombreEntidad con id: " + id);
        try {
            List<Long> ids = new java.util.ArrayList<>();
            ids.add(id);
            nombreEntidadService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("NombreEntidad eliminado correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria NombreEntidad");
        try {
            return Response.status(Response.Status.OK)
                    .entity(nombreEntidadService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria NombreEntidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
```

### Convención del `@Path`
El path REST es el **nombre de la tabla en minúsculas** (ej: tabla `RPRX` → `@Path("rprx")`).

---

## 8. Convenciones generales de nombres

| Elemento | Convención | Ejemplo |
|---|---|---|
| Nombre de entidad Java | PascalCase | `TipoReporte` |
| Nombre de campo Java | camelCase | `nombreReporte` |
| Columna DB | `XXXXNMBR` (prefijo tabla + sufijo) | `TPRPNMBR` |
| NamedQuery | `EntidadAll` / `EntidadId` | `TipoReporteAll` |
| Constante en NombreEntidades | SNAKE_UPPER | `TIPO_REPORTE` |
| Path REST | tabla en minúsculas | `@Path("tprp")` |
| Secuencia Oracle | `RPR.SQ_XXXXCDGO` | `RPR.SQ_TPRPCDGO` |
| Package model | `com.saa.model.rpr` | — |
| Package dao | `com.saa.ejb.rpr.dao` | — |
| Package daoImpl | `com.saa.ejb.rpr.daoImpl` | — |
| Package service | `com.saa.ejb.rpr.service` | — |
| Package serviceImpl | `com.saa.ejb.rpr.serviceImpl` | — |
| Package rest | `com.saa.ws.rest.rpr` | — |

---

## 9. Checklist por entidad

Para cada tabla nueva del esquema `RPR`, crear los siguientes **7 archivos**:

- [ ] `model/rpr/NombreEntidad.java` — Entidad JPA
- [ ] `model/rpr/NombreEntidadesReporte.java` — Actualizar con la nueva constante (archivo compartido)
- [ ] `ejb/rpr/dao/NombreEntidadDaoService.java` — Interface DAO
- [ ] `ejb/rpr/daoImpl/NombreEntidadDaoServiceImpl.java` — Implementación DAO
- [ ] `ejb/rpr/service/NombreEntidadService.java` — Interface Service
- [ ] `ejb/rpr/serviceImpl/NombreEntidadServiceImpl.java` — Implementación Service
- [ ] `ws/rest/rpr/NombreEntidadRest.java` — Endpoint REST

> El archivo `NombreEntidadesReporte.java` es **uno solo por módulo** y se actualiza con cada nueva entidad.

---

## 10. Referencia a módulos existentes como ejemplo

| Módulo | NombreEntidades | Ejemplo de entidad simple |
|---|---|---|
| `crd` | `NombreEntidadesCredito` | `TipoAporte` |
| `cnt` | *(revisar)* | `TipoAsiento` |
| `tsr` | *(revisar)* | *(revisar)* |

### Ruta del ejemplo más completo revisado (TipoAporte - CRD):
- Model: `src/main/java/com/saa/model/crd/TipoAporte.java`
- DAO: `src/main/java/com/saa/ejb/crd/dao/TipoAporteDaoService.java`
- DAO Impl: `src/main/java/com/saa/ejb/crd/daoImpl/TipoAporteDaoServiceImpl.java`
- Service: `src/main/java/com/saa/ejb/crd/service/TipoAporteService.java`
- Service Impl: `src/main/java/com/saa/ejb/crd/serviceImpl/TipoAporteServiceImpl.java`
- REST: `src/main/java/com/saa/ws/rest/crd/TipoAporteRest.java`
- NombreEntidades: `src/main/java/com/saa/model/crd/NombreEntidadesCredito.java`
