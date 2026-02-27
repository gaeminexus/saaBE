# Guía Completa de Mapeo de Tablas - Model hasta REST

## 📋 Índice
1. [Introducción](#introducción)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Información Necesaria Antes de Comenzar](#información-necesaria-antes-de-comenzar)
4. [Paso 1: Crear el Model (Entidad JPA)](#paso-1-crear-el-model-entidad-jpa)
5. [Paso 2: Crear DAO Interface](#paso-2-crear-dao-interface)
6. [Paso 3: Crear DAO Implementation](#paso-3-crear-dao-implementation)
7. [Paso 4: Crear Service Interface](#paso-4-crear-service-interface)
8. [Paso 5: Crear Service Implementation](#paso-5-crear-service-implementation)
9. [Paso 6: Crear REST Endpoint](#paso-6-crear-rest-endpoint)
10. [Paso 7: Actualizar Constantes](#paso-7-actualizar-constantes)
11. [Checklist Final](#checklist-final)
12. [Convenciones de Nomenclatura](#convenciones-de-nomenclatura)
13. [Patrones y Estándares](#patrones-y-estándares)

---

## Introducción

Este documento describe el proceso completo y estandarizado para mapear una tabla de base de datos en el sistema **saaBE**, desde la capa de modelo (JPA) hasta la exposición de servicios REST.

### Flujo de Capas
```
Base de Datos (Oracle)
    ↓
Model (JPA Entity)
    ↓
DAO Interface + Implementation
    ↓
Service Interface + Implementation
    ↓
REST Endpoint
    ↓
Cliente (JSON)
```

---

## Arquitectura del Sistema

### Estructura de Paquetes
```
com.saa
├── model.{modulo}              # Entidades JPA
├── ejb.{modulo}.dao           # Interfaces DAO
├── ejb.{modulo}.daoImpl       # Implementaciones DAO
├── ejb.{modulo}.service       # Interfaces Service
├── ejb.{modulo}.serviceImpl   # Implementaciones Service
└── ws.rest.{modulo}           # Endpoints REST
```

### Módulos Principales
- **cnt** - Contabilidad
- **tsr** - Tesorería
- **cxc** - Cuentas por Cobrar
- **cxp** - Cuentas por Pagar
- **rhh** - Recursos Humanos

---

## Información Necesaria Antes de Comenzar

### 1. Script SQL de Creación de Tabla
Debes tener el script completo que incluya:
- Nombre de la tabla y esquema
- Todos los campos con tipos de datos
- Llaves primarias y foráneas
- Secuencias para PKs
- Constraints y defaults
- Comentarios (COMMENT ON)

### 2. Identificar el Módulo
Determina a qué módulo pertenece la tabla:
- CNT (Contabilidad)
- TSR (Tesorería)
- CXC (Cuentas por Cobrar)
- CXP (Cuentas por Pagar)
- RHH (Recursos Humanos)

### 3. Identificar Relaciones
- ¿Tiene relaciones con otras tablas?
- ¿Es una tabla de detalle (1:N)?
- ¿Tiene llaves foráneas?

### 4. Nomenclatura Oracle → Java
| Oracle | Java | Tipo Java |
|--------|------|-----------|
| NUMBER(18,0) | Long | Long |
| NUMBER(18,2) | Double | Double |
| NUMBER(5,0) | Integer | Integer |
| VARCHAR2(n) | String | String |
| DATE | LocalDate | LocalDate |
| TIMESTAMP | LocalDateTime | LocalDateTime |

---

## Paso 1: Crear el Model (Entidad JPA)

### Ubicación
```
src/main/java/com/saa/model/{modulo}/{NombreEntidad}.java
```

### Estructura Completa

```java
package com.saa.model.{modulo};

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;

@SuppressWarnings("serial")
@Entity
@Table(name = "NOMBRE_TABLA", schema = "ESQUEMA")
@SequenceGenerator(name = "SQ_NOMBRE", sequenceName = "ESQUEMA.SQ_NOMBRE", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "NombreEntidadAll", query = "select e from NombreEntidad e"),
	@NamedQuery(name = "NombreEntidadId", query = "select e from NombreEntidad e where e.codigo = :id")
})
public class NombreEntidad implements Serializable {

	/**
	 * ID de la tabla (Llave Primaria).
	 */
	@Basic
	@Id
	@Column(name = "CAMPO_PK", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_NOMBRE")	
	private Long codigo;
	
	/**
	 * Relación ManyToOne - Llave foránea.
	 */
	@ManyToOne
	@JoinColumn(name = "CAMPO_FK", referencedColumnName = "CAMPO_PK_PADRE")
	private EntidadPadre entidadPadre;
	
	/**
	 * Campo String - Descripción del campo.
	 */
	@Basic
	@Column(name = "CAMPO_STR", length = 100)
	private String campoString;
	
	/**
	 * Campo Double - Descripción del campo.
	 */
	@Basic
	@Column(name = "CAMPO_NUM")
	private Double campoNumerico;
	
	/**
	 * Campo Integer - Descripción del campo.
	 */
	@Basic
	@Column(name = "CAMPO_INT")
	private Integer campoEntero;
	
	/**
	 * Campo Date - Descripción del campo.
	 */
	@Basic
	@Column(name = "CAMPO_FEC")
	private LocalDate campoFecha;
	
	/**
	 * Relación OneToMany - Colección de hijos (OPCIONAL).
	 */
	@OneToMany(mappedBy = "entidadPadre")
	private List<EntidadHija> listaHijos;

	// Constructor vacío (requerido por JPA)
	public NombreEntidad() {
	}

	// Getters and Setters para TODOS los campos
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	// ... resto de getters y setters
}
```

### Checklist del Model
- ✅ Anotación `@Entity`
- ✅ Anotación `@Table` con nombre y esquema correctos
- ✅ `@SequenceGenerator` si tiene secuencia
- ✅ `@NamedQueries` con queries básicas
- ✅ Implementa `Serializable`
- ✅ Campo PK con `@Id` y `@GeneratedValue`
- ✅ Relaciones `@ManyToOne` / `@OneToMany` correctamente mapeadas
- ✅ Todos los campos con `@Column` y nombre correcto
- ✅ Tipos de datos Java correctos
- ✅ Javadoc en todos los campos
- ✅ Getters y setters para todos los campos

---

## Paso 2: Crear DAO Interface

### Ubicación
```
src/main/java/com/saa/ejb/{modulo}/dao/{NombreEntidad}DaoService.java
```

### Estructura Completa

```java
/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.{modulo}.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.{modulo}.NombreEntidad;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad NombreEntidad.
 */
@Local
public interface NombreEntidadDaoService extends EntityDao<NombreEntidad> {

	/**
	 * Método personalizado 1 - Descripción clara
	 * @param parametro1	: Descripción del parámetro
	 * @return				: Descripción del retorno
	 * @throws Throwable	: Excepcion
	 */
	List<NombreEntidad> selectByParametro1(Long parametro1) throws Throwable;
	
	/**
	 * Método personalizado 2 - Descripción clara
	 * @param parametro2	: Descripción del parámetro
	 * @return				: Descripción del retorno
	 * @throws Throwable	: Excepcion
	 */
	List<NombreEntidad> selectByParametro2(String parametro2) throws Throwable;
	
	// Agregar métodos de consulta específicos según necesidad del negocio
}
```

### Métodos Comunes en DAO
- `selectByIdEntidadPadre(Long id)` - Buscar por FK
- `selectByEstado(String estado)` - Buscar por estado
- `selectByFechas(LocalDate inicio, LocalDate fin)` - Buscar por rango de fechas
- `selectByCodigo(String codigo)` - Buscar por código único

### Checklist del DAO Interface
- ✅ Anotación `@Local`
- ✅ Extiende `EntityDao<NombreEntidad>`
- ✅ Copyright y Javadoc completo
- ✅ Métodos específicos del negocio documentados
- ✅ Todos los métodos lanzan `Throwable`

---

## Paso 3: Crear DAO Implementation

### Ubicación
```
src/main/java/com/saa/ejb/{modulo}/daoImpl/{NombreEntidad}DaoServiceImpl.java
```

### Estructura Completa

```java
package com.saa.ejb.{modulo}.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.{modulo}.dao.NombreEntidadDaoService;
import com.saa.model.{modulo}.NombreEntidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class NombreEntidadDaoServiceImpl extends EntityDaoImpl<NombreEntidad> 
	implements NombreEntidadDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) NombreEntidad");
		return new String[]{
			"codigo",
			"entidadPadre",
			"campoString",
			"campoNumerico",
			"campoEntero",
			"campoFecha"
			// Listar TODOS los campos de la entidad
		};
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<NombreEntidad> selectByParametro1(Long parametro1) throws Throwable {
		System.out.println("Ingresa al metodo selectByParametro1 con valor: " + parametro1);
		Query query = em.createQuery(
			"select e from NombreEntidad e " +
			"where e.campoRelacionado = :parametro1 " +
			"order by e.codigo"
		);
		query.setParameter("parametro1", parametro1);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<NombreEntidad> selectByParametro2(String parametro2) throws Throwable {
		System.out.println("Ingresa al metodo selectByParametro2 con valor: " + parametro2);
		Query query = em.createQuery(
			"select e from NombreEntidad e " +
			"where e.campoString = :parametro2 " +
			"order by e.codigo"
		);
		query.setParameter("parametro2", parametro2);
		return query.getResultList();
	}
}
```

### Estándares de JPQL
- Usar alias `e` para la entidad principal
- Usar `order by e.codigo` al final de las consultas
- Parametrizar siempre con `:nombreParametro`
- Usar `@SuppressWarnings("unchecked")` en métodos que retornan listas
- Log al inicio de cada método con `System.out.println`

### Checklist del DAO Implementation
- ✅ Anotación `@Stateless`
- ✅ Extiende `EntityDaoImpl<NombreEntidad>`
- ✅ Implementa la interfaz DAO
- ✅ Inyección de `EntityManager` con `@PersistenceContext`
- ✅ Método `obtieneCampos()` con todos los campos
- ✅ Implementación de todos los métodos de la interfaz
- ✅ Queries JPQL correctas y parametrizadas
- ✅ Logs en cada método

---

## Paso 4: Crear Service Interface

### Ubicación
```
src/main/java/com/saa/ejb/{modulo}/service/{NombreEntidad}Service.java
```

### Estructura Completa

```java
package com.saa.ejb.{modulo}.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.{modulo}.NombreEntidad;

import jakarta.ejb.Local;

@Local
public interface NombreEntidadService extends EntityService<NombreEntidad> {
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	NombreEntidad selectById(Long id) throws Throwable;
	
	/**
	 * Recupera entidades por parámetro específico
	 * @param parametro		: Descripción del parámetro
	 * @return				: Lista de entidades
	 * @throws Throwable	: Excepcion
	 */
	List<NombreEntidad> selectByParametro(Long parametro) throws Throwable;
	
	/**
	 * Método de negocio específico 1
	 * @param entidad		: Entidad a procesar
	 * @return				: Resultado del procesamiento
	 * @throws Throwable	: Excepcion
	 */
	Long guardarEntidad(NombreEntidad entidad) throws Throwable;
	
	/**
	 * Método de cálculo o validación específica
	 * @param entidad		: Entidad a validar/calcular
	 * @return				: Resultado del cálculo
	 * @throws Throwable	: Excepcion
	 */
	Double calcularValor(NombreEntidad entidad) throws Throwable;
	
	// Métodos de lógica de negocio específicos
}
```

### Métodos Comunes en Service
- `selectById(Long id)` - **SIEMPRE incluir**
- `selectByParametro(...)` - Delegados del DAO
- `guardar...()` - Métodos de guardado con lógica
- `calcular...()` - Métodos de cálculo
- `validar...()` - Métodos de validación
- `generar...()` - Métodos de generación de datos

### Checklist del Service Interface
- ✅ Anotación `@Local`
- ✅ Extiende `EntityService<NombreEntidad>`
- ✅ Método `selectById` incluido
- ✅ Métodos de negocio documentados
- ✅ Todos los métodos lanzan `Throwable`

---

## Paso 5: Crear Service Implementation

### Ubicación
```
src/main/java/com/saa/ejb/{modulo}/serviceImpl/{NombreEntidad}ServiceImpl.java
```

### Estructura Completa

```java
package com.saa.ejb.{modulo}.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.{modulo}.dao.NombreEntidadDaoService;
import com.saa.ejb.{modulo}.service.NombreEntidadService;
import com.saa.model.{modulo}.NombreEntidad;
import com.saa.model.{modulo}.NombreEntidades{Modulo};

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NombreEntidadServiceImpl implements NombreEntidadService {

	@EJB
	private NombreEntidadDaoService nombreEntidadDaoService;
	
	// Inyectar otros DAOs o Services si son necesarios
	// @EJB
	// private OtroDaoService otroDaoService;
	
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de NombreEntidad service");
		NombreEntidad entidad = new NombreEntidad();
		for (Long registro : id) {
			nombreEntidadDaoService.remove(entidad, registro);	
		}		
	}	
	
	@Override
	public void save(List<NombreEntidad> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de NombreEntidad service");
		for (NombreEntidad entidad : lista) 		
			nombreEntidadDaoService.save(entidad, entidad.getCodigo());
	}	
	
	@Override
	public List<NombreEntidad> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll NombreEntidadService");
		List<NombreEntidad> result = nombreEntidadDaoService.selectAll(
			NombreEntidades{Modulo}.NOMBRE_ENTIDAD
		); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda de NombreEntidad no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public List<NombreEntidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) NombreEntidad");
		List<NombreEntidad> result = nombreEntidadDaoService.selectByCriteria(
			datos, 
			NombreEntidades{Modulo}.NOMBRE_ENTIDAD
		);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda de NombreEntidad no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public NombreEntidad saveSingle(NombreEntidad entidad) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) NombreEntidad Service");
		
		// Realizar cálculos o validaciones antes de guardar
		// if (entidad.getCampo() != null) {
		//     Double calculado = calcularValor(entidad);
		//     entidad.setCampoCalculado(calculado);
		// }
		
		nombreEntidadDaoService.save(entidad, entidad.getCodigo());
		return entidad;
	}
	
	@Override
	public NombreEntidad selectById(Long id) throws Throwable {
		System.out.println("Ingresa al metodo (selectById) de NombreEntidad con id: " + id);
		return nombreEntidadDaoService.selectById(id, NombreEntidades{Modulo}.NOMBRE_ENTIDAD);
	}
	
	@Override
	public List<NombreEntidad> selectByParametro(Long parametro) throws Throwable {
		System.out.println("Ingresa al metodo (selectByParametro) con valor: " + parametro);
		return nombreEntidadDaoService.selectByParametro1(parametro);
	}
	
	@Override
	public Long guardarEntidad(NombreEntidad entidad) throws Throwable {
		System.out.println("Ingresa al metodo (guardarEntidad) de NombreEntidad");
		
		// Lógica de negocio específica
		// Validaciones, cálculos, etc.
		
		nombreEntidadDaoService.save(entidad, entidad.getCodigo());
		return entidad.getCodigo();
	}
	
	@Override
	public Double calcularValor(NombreEntidad entidad) throws Throwable {
		System.out.println("Calculando valor para NombreEntidad");
		
		// Implementar lógica de cálculo
		Double valor1 = entidad.getCampoNumerico() != null ? entidad.getCampoNumerico() : 0.0;
		Double valor2 = entidad.getOtroCampo() != null ? entidad.getOtroCampo() : 0.0;
		
		return valor1 + valor2;
	}
}
```

### Métodos Obligatorios del EntityService
1. `remove(List<Long> id)` - Eliminar múltiples registros
2. `save(List<Tipo> lista)` - Guardar múltiples registros
3. `selectAll()` - Obtener todos los registros
4. `selectByCriteria(List<DatosBusqueda>)` - Búsqueda dinámica
5. `saveSingle(Tipo objeto)` - Guardar un registro
6. `selectById(Long id)` - Obtener por ID (no está en EntityService pero es estándar)

### Checklist del Service Implementation
- ✅ Anotación `@Stateless`
- ✅ Implementa la interfaz Service
- ✅ Inyección del DAO con `@EJB`
- ✅ Implementación de los 5 métodos obligatorios
- ✅ Uso de constantes de `NombreEntidades{Modulo}`
- ✅ Validación de resultados vacíos con `IncomeException`
- ✅ Logs en cada método
- ✅ Lógica de negocio en métodos específicos

---

## Paso 6: Crear REST Endpoint

### Ubicación
```
src/main/java/com/saa/ws/rest/{modulo}/{NombreEntidad}Rest.java
```

### Estructura Completa

```java
package com.saa.ws.rest.{modulo};

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.{modulo}.dao.NombreEntidadDaoService;
import com.saa.ejb.{modulo}.service.NombreEntidadService;
import com.saa.model.{modulo}.NombreEntidad;
import com.saa.model.{modulo}.NombreEntidades{Modulo};

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

@Path("abreviatura")  // Usar abreviatura de 4 letras de la tabla
public class NombreEntidadRest {

    @EJB
    private NombreEntidadDaoService nombreEntidadDaoService;

    @EJB
    private NombreEntidadService nombreEntidadService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public NombreEntidadRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NombreEntidad> lista = nombreEntidadDaoService.selectAll(
                NombreEntidades{Modulo}.NOMBRE_ENTIDAD
            );
            return Response.status(Response.Status.OK)
                .entity(lista)
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registros: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            NombreEntidad entidad = nombreEntidadDaoService.selectById(
                id, 
                NombreEntidades{Modulo}.NOMBRE_ENTIDAD
            );
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("NombreEntidad con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
            }
            return Response.status(Response.Status.OK)
                .entity(entidad)
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * GET - Obtener por parámetro específico
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByParametro/{parametro}")
    public Response getByParametro(@PathParam("parametro") Long parametro) {
        try {
            List<NombreEntidad> lista = nombreEntidadService.selectByParametro(parametro);
            return Response.status(Response.Status.OK)
                .entity(lista)
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registros: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * PUT - Actualizar registro existente
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NombreEntidad registro) {
        System.out.println("LLEGA AL SERVICIO PUT - NOMBRE_ENTIDAD");
        try {
            NombreEntidad resultado = nombreEntidadService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                .entity(resultado)
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * POST - Crear nuevo registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NombreEntidad registro) {
        System.out.println("LLEGA AL SERVICIO POST - NOMBRE_ENTIDAD");
        try {
            NombreEntidad resultado = nombreEntidadService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                .entity(resultado)
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * POST - Búsqueda por criterios dinámicos
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de NOMBRE_ENTIDAD");
        try {
            return Response.status(Response.Status.OK)
                .entity(nombreEntidadService.selectByCriteria(registros))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }

    /**
     * DELETE - Eliminar registro
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - NOMBRE_ENTIDAD");
        try {
            NombreEntidad elimina = new NombreEntidad();
            nombreEntidadDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON)
                .build();
        }
    }
}
```

### Endpoints REST Estándar
| Método | Path | Descripción | Status Code |
|--------|------|-------------|-------------|
| GET | `/getAll` | Obtener todos | 200 OK |
| GET | `/getId/{id}` | Obtener por ID | 200 OK / 404 NOT FOUND |
| GET | `/getBy.../{param}` | Obtener por parámetro | 200 OK |
| POST | `/` | Crear nuevo | 201 CREATED |
| PUT | `/` | Actualizar | 200 OK |
| POST | `/selectByCriteria` | Búsqueda dinámica | 200 OK |
| DELETE | `/{id}` | Eliminar | 204 NO CONTENT |

### Checklist del REST Endpoint
- ✅ Anotación `@Path` con abreviatura de tabla
- ✅ Inyección de DAO y Service con `@EJB`
- ✅ Constructor vacío
- ✅ `@Context` para UriInfo
- ✅ Los 7 endpoints estándar implementados
- ✅ Manejo de errores con try-catch
- ✅ Respuestas con códigos HTTP correctos
- ✅ `MediaType.APPLICATION_JSON` en todas las respuestas
- ✅ Logs en métodos de modificación

---

## Paso 7: Actualizar Constantes

### Ubicación
```
src/main/java/com/saa/model/{modulo}/NombreEntidades{Modulo}.java
```

### Ejemplo
```java
public interface NombreEntidadesContabilidad {
    // ... existentes ...
    
    String DETALLE_ASIENTO = "DetalleAsiento";
    String SUBDETALLE_ASIENTO = "SubdetalleAsiento";  // ← AGREGAR NUEVA
    String DETALLE_MAYORIZACION = "DetalleMayorizacion";
    
    // ... resto ...
}
```

### Checklist de Constantes
- ✅ Agregar constante con el nombre de la entidad
- ✅ Mantener orden alfabético o lógico
- ✅ Valor debe coincidir exactamente con el nombre de la clase

---

## Checklist Final

### Antes de Finalizar, Verificar:

#### 1. Compilación
```bash
# Verificar que no hay errores de compilación
mvn clean compile
```

#### 2. Archivos Creados
- ✅ Model: `{NombreEntidad}.java`
- ✅ DAO Interface: `{NombreEntidad}DaoService.java`
- ✅ DAO Impl: `{NombreEntidad}DaoServiceImpl.java`
- ✅ Service Interface: `{NombreEntidad}Service.java`
- ✅ Service Impl: `{NombreEntidad}ServiceImpl.java`
- ✅ REST: `{NombreEntidad}Rest.java`
- ✅ Constante agregada en `NombreEntidades{Modulo}.java`

#### 3. Estándares de Código
- ✅ Todos los imports correctos
- ✅ Sin warnings de compilación
- ✅ Javadoc en todos los métodos públicos
- ✅ Logs en métodos importantes
- ✅ Manejo de excepciones apropiado

#### 4. Documentación
- ✅ Crear archivo MD con documentación de la API
- ✅ Incluir ejemplos de JSON
- ✅ Documentar endpoints REST
- ✅ Explicar cálculos o lógica especial

---

## Convenciones de Nomenclatura

### Oracle → Java

#### Tabla a Clase
```
SDAS → SubdetalleAsiento
DTAS → DetalleAsiento
ASNT → Asiento
PLCT → PlanCuenta
```

#### Columna a Atributo
```
SDASCDGO → codigo
SDASNMBR → nombreBien
SDASFCAD → fechaAdquisicion
SDASCDAC → codigoActivo
```

**Regla**: 
- Remover prefijo de tabla (ej: SDAS)
- Convertir a camelCase
- Usar nombres descriptivos en español

#### Secuencia
```
CNT.SQ_SDASCDGO
```

### Path REST
Usar los 4 caracteres del nombre de tabla en minúsculas:
```
SDAS → /sdas
DTAS → /dtas
ASNT → /asnt
```

---

## Patrones y Estándares

### 1. Patrón de Capas
```
REST → Service → DAO → Database
```
- **REST**: Solo manejo de HTTP, delegación a Service
- **Service**: Lógica de negocio, transacciones
- **DAO**: Solo acceso a datos, queries

### 2. Inyección de Dependencias
```java
@EJB
private NombreService nombreService;
```
**Nunca** usar `new` para crear instancias de Services o DAOs.

### 3. Manejo de Transacciones
Las transacciones se manejan automáticamente con `@Stateless`.
Para transacciones específicas, usar `@TransactionAttribute`.

### 4. Manejo de Errores

#### En DAO y Service
```java
throw new IncomeException("Mensaje descriptivo");
```

#### En REST
```java
try {
    // código
} catch (Throwable e) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity("Error: " + e.getMessage())
        .build();
}
```

### 5. Logs
```java
System.out.println("Descripción de lo que hace el método");
```
Agregar logs al inicio de métodos importantes.

### 6. Validaciones
```java
if (result.isEmpty()) {
    throw new IncomeException("No se encontraron registros");
}
```

### 7. Retornos REST
```java
// Éxito con datos
return Response.status(Response.Status.OK)
    .entity(datos)
    .type(MediaType.APPLICATION_JSON)
    .build();

// Éxito sin contenido
return Response.status(Response.Status.NO_CONTENT).build();

// Creación exitosa
return Response.status(Response.Status.CREATED)
    .entity(datos)
    .build();

// No encontrado
return Response.status(Response.Status.NOT_FOUND)
    .entity("Mensaje")
    .build();
```

---

## Ejemplo Completo: Paso a Paso

### Escenario: Mapear tabla CNT.SDAS (SubdetalleAsiento)

#### Información de la Tabla
```sql
CREATE TABLE CNT.SDAS (
    SDASCDGO NUMBER(18,0) NOT NULL,
    DTASCDGO NUMBER(18,0) NOT NULL,
    SDASNMBR VARCHAR2(200) NOT NULL,
    SDASCSAD NUMBER(18,2) NOT NULL,
    ...
);
CREATE SEQUENCE CNT.SQ_SDASCDGO;
```

#### 1. Crear Model
```java
// Ubicación: src/main/java/com/saa/model/cnt/SubdetalleAsiento.java
@Entity
@Table(name = "SDAS", schema = "CNT")
@SequenceGenerator(name = "SQ_SDASCDGO", sequenceName = "CNT.SQ_SDASCDGO", allocationSize = 1)
public class SubdetalleAsiento implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_SDASCDGO")
    @Column(name = "SDASCDGO")
    private Long codigo;
    
    @ManyToOne
    @JoinColumn(name = "DTASCDGO", referencedColumnName = "DTASCDGO")
    private DetalleAsiento detalleAsiento;
    
    @Column(name = "SDASNMBR", length = 200)
    private String nombreBien;
    
    @Column(name = "SDASCSAD")
    private Double costoAdquisicion;
    
    // ... resto de campos
}
```

#### 2. Crear DAO Interface
```java
// Ubicación: src/main/java/com/saa/ejb/cnt/dao/SubdetalleAsientoDaoService.java
@Local
public interface SubdetalleAsientoDaoService extends EntityDao<SubdetalleAsiento> {
    List<SubdetalleAsiento> selectByIdDetalleAsiento(Long idDetalleAsiento) throws Throwable;
}
```

#### 3. Crear DAO Implementation
```java
// Ubicación: src/main/java/com/saa/ejb/cnt/daoImpl/SubdetalleAsientoDaoServiceImpl.java
@Stateless
public class SubdetalleAsientoDaoServiceImpl extends EntityDaoImpl<SubdetalleAsiento> 
    implements SubdetalleAsientoDaoService {
    
    @PersistenceContext
    EntityManager em;
    
    public String[] obtieneCampos() {
        return new String[]{"codigo", "detalleAsiento", "nombreBien", ...};
    }
    
    public List<SubdetalleAsiento> selectByIdDetalleAsiento(Long id) throws Throwable {
        Query query = em.createQuery("select s from SubdetalleAsiento s where s.detalleAsiento.codigo = :id");
        query.setParameter("id", id);
        return query.getResultList();
    }
}
```

#### 4. Crear Service Interface
```java
// Ubicación: src/main/java/com/saa/ejb/cnt/service/SubdetalleAsientoService.java
@Local
public interface SubdetalleAsientoService extends EntityService<SubdetalleAsiento> {
    SubdetalleAsiento selectById(Long id) throws Throwable;
    List<SubdetalleAsiento> selectByIdDetalleAsiento(Long id) throws Throwable;
    Double calcularValorNetoLibros(SubdetalleAsiento subdetalle) throws Throwable;
}
```

#### 5. Crear Service Implementation
```java
// Ubicación: src/main/java/com/saa/ejb/cnt/serviceImpl/SubdetalleAsientoServiceImpl.java
@Stateless
public class SubdetalleAsientoServiceImpl implements SubdetalleAsientoService {
    @EJB
    private SubdetalleAsientoDaoService dao;
    
    public SubdetalleAsiento saveSingle(SubdetalleAsiento s) throws Throwable {
        if (s.getCostoAdquisicion() != null) {
            Double valorNeto = calcularValorNetoLibros(s);
            s.setValorNetoLibros(valorNeto);
        }
        dao.save(s, s.getCodigo());
        return s;
    }
    
    public Double calcularValorNetoLibros(SubdetalleAsiento s) throws Throwable {
        Double costo = s.getCostoAdquisicion() != null ? s.getCostoAdquisicion() : 0.0;
        Double depreciacion = s.getDepreciacionAcumulada() != null ? s.getDepreciacionAcumulada() : 0.0;
        return costo - depreciacion;
    }
    
    // ... resto de métodos obligatorios
}
```

#### 6. Crear REST
```java
// Ubicación: src/main/java/com/saa/ws/rest/cnt/SubdetalleAsientoRest.java
@Path("sdas")
public class SubdetalleAsientoRest {
    @EJB
    private SubdetalleAsientoDaoService dao;
    
    @EJB
    private SubdetalleAsientoService service;
    
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SubdetalleAsiento> lista = dao.selectAll(NombreEntidadesContabilidad.SUBDETALLE_ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
    
    // ... resto de endpoints
}
```

#### 7. Actualizar Constantes
```java
// Ubicación: src/main/java/com/saa/model/cnt/NombreEntidadesContabilidad.java
public interface NombreEntidadesContabilidad {
    String DETALLE_ASIENTO = "DetalleAsiento";
    String SUBDETALLE_ASIENTO = "SubdetalleAsiento";  // ← AGREGAR
}
```

---

## Resumen de Tiempos Estimados

| Paso | Tiempo Estimado |
|------|----------------|
| 1. Model | 15-20 min |
| 2. DAO Interface | 5 min |
| 3. DAO Implementation | 10-15 min |
| 4. Service Interface | 5 min |
| 5. Service Implementation | 15-20 min |
| 6. REST Endpoint | 10-15 min |
| 7. Constantes | 2 min |
| **TOTAL** | **60-90 min** |

---

## Checklist de Verificación Final

Antes de dar por terminado el mapeo, verificar:

- [ ] **Compilación exitosa** sin errores
- [ ] **Todos los archivos creados** (7 archivos)
- [ ] **Imports correctos** en todos los archivos
- [ ] **Anotaciones JPA correctas** en el Model
- [ ] **Relaciones correctamente mapeadas**
- [ ] **Métodos obligatorios implementados** en Service
- [ ] **Endpoints REST funcionando** (al menos getAll)
- [ ] **Constante agregada** en NombreEntidades
- [ ] **Documentación creada** (archivo MD)
- [ ] **Logs agregados** en métodos importantes
- [ ] **Manejo de errores** en todos los endpoints REST

---

## Recursos Adicionales

### Archivos de Referencia
- `DetalleAsiento.java` - Model de ejemplo
- `DetalleAsientoDaoService.java` - DAO Interface de ejemplo
- `DetalleAsientoDaoServiceImpl.java` - DAO Implementation de ejemplo
- `DetalleAsientoService.java` - Service Interface de ejemplo
- `DetalleAsientoServiceImpl.java` - Service Implementation de ejemplo
- `DetalleAsientoRest.java` - REST de ejemplo

### Comandos Útiles
```bash
# Compilar proyecto
mvn clean compile

# Verificar errores
mvn clean install -DskipTests

# Ver estructura de archivos
tree src/main/java/com/saa
```

---

## Notas Finales

1. **Seguir siempre este orden**: Model → DAO → Service → REST
2. **No saltarse pasos**: Cada capa depende de la anterior
3. **Validar continuamente**: Compilar después de cada paso
4. **Documentar mientras se desarrolla**: No dejar la documentación para el final
5. **Reutilizar código de referencia**: Usar archivos existentes como plantilla
6. **Mantener consistencia**: Seguir los mismos patrones en todo el código

---

**Última actualización**: 2026-02-26  
**Versión**: 1.0  
**Autor**: GitHub Copilot  
**Proyecto**: saaBE v1
