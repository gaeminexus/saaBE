# CAMBIO: Facturas de reembolso de gastos en la bandeja electrónica (BACKEND)

**Fecha:** 2026-08-19 (v2 — incluye contabilización por grupos de producto de los reembolsos)
**Módulo:** CXP (schema Oracle `PGS`)
**Contraparte frontend:** `C:\work\saaFE\v1\saaFE\docs\cxp\CAMBIO-REEMBOLSO-GASTOS-FRONTEND.md`

> **Instrucciones para el agente implementador:** este documento es autosuficiente; no hace falta
> re-explorar el repo salvo para confirmar anclajes (los números de línea son del 2026-08-19 —
> buscar por nombre de método, no por número). **No compilar con mvn/javac**: la compilación la
> hace el usuario en Eclipse. **El DDL ya está escrito por el orquestador** en
> `docs/logica-negocio/cxp/sql/07-reembolso-gastos.sql` y lo ejecuta el usuario en la BD — NO
> modificarlo; el mapeo JPA de este documento corresponde exactamente a ese script.
> **Regla del repo:** todo cambio en `ProcesoCargaDocumentosServiceImpl` obliga a actualizar
> `docs/logica-negocio/cxp/proceso-carga-documentos.md` en el mismo cambio (§11).
> Otro equipo trabaja en RRHH y CNT — no tocar nada de esos módulos salvo lo puntual indicado en
> §8 (rama nueva dentro de la generación del asiento de factura de compra).
> Al terminar: listar archivos creados/modificados y dudas abiertas.

---

## 1. Contexto y objetivo

Los proveedores intermediarios emiten **facturas de reembolso de gastos** (Ficha Técnica SRI,
ANEXO 5 — ver `docs/referencias/pdf/SRI.md` líneas ~4929-5560). Además del detalle normal, el XML
puede traer:

- En `<infoFactura>`: `<codDocReembolso>` (normalmente `41`), `<totalComprobantesReembolso>`,
  `<totalBaseImponibleReembolso>`, `<totalImpuestoReembolso>`.
- Un bloque `<reembolsos>` con N `<reembolsoDetalle>`: identificación del proveedor original del
  gasto, documento sustento (codDoc/estab/ptoEmi/secuencial/fecha/autorización) y sus impuestos
  (`<detalleImpuestos><detalleImpuesto>` con `baseImponibleReembolso`/`impuestoReembolso`).

Hoy el parser (`ProcesoCargaDocumentosServiceImpl`) ignora esos tags por completo. Además muchos
emisores generan mal el XML: mandan la factura sin `<reembolsos>` y entregan los sustentos aparte.

**Objetivo:**
1. **XML bien generado:** detectar reembolso, leer `<reembolsos>` y grabar los sustentos en la
   tabla nueva `PGS.RMBF`, resolviendo/creando el producto de cada sustento.
2. **XML mal generado:** el usuario marca el documento como reembolso (desde la bandeja o desde
   gestión de documentos, ANTES o DESPUÉS de subir el XML) y captura los sustentos manualmente;
   la contabilización queda diferida hasta completar la captura.
3. **Contabilización (regla de negocio confirmada por el usuario):** los detalles de reembolso se
   tratan como el detalle de la factura: cada sustento resuelve su producto (si no existe se crea
   en el grupo **POR CLASIFICAR**), y **solo cuando todos los productos de los sustentos están
   clasificados** se genera el asiento, con el DEBE agrupado por los grupos de los productos de
   los **sustentos de reembolso** (NO por los del detalle normal, para no duplicar). El HABER al
   proveedor es el total de la factura, igual que hoy.

**Fuera de alcance:** reportes ATS/anexos; retenciones sobre reembolsos; facturas "mixtas"
(fee del intermediario + reembolso en el mismo documento) — si aparece un caso real se decide
después; por ahora se asume `importeTotal ≈ totalComprobantesReembolso`.

---

## 2. Decisión de diseño: tabla nueva `PGS.RMBF`, NO reusar `DetalleFacturaCompra`

Se evaluó extender `PGS.DFCC` con campos adicionales y se **descartó**: el grano es distinto (una
fila de reembolso = un documento sustento de un tercero, no una línea de producto), los campos
casi no se solapan, y mezclar filas de reembolso en `DFCC` duplicaría valores en el asiento que
hoy se agrupa por `GrupoProductoPago` sobre `DFCC`.

`RMBF` tiene su propia columna `RMBFPRDC` (id de `PGS.PRDP`, columna plana sin FK JPA, igual que
`DFCC.PRODUCTO`) porque la contabilización del reembolso se hace por el grupo del producto de
cada sustento.

Código de tabla `RMBF` verificado libre contra los 62 códigos existentes en `PGS`.

---

## 3. DDL — YA ESCRITO, NO MODIFICAR

`docs/logica-negocio/cxp/sql/07-reembolso-gastos.sql` (lo ejecuta el usuario en Oracle). Resumen
de lo que crea, para que el mapeo JPA coincida:

- `PGS.FCTC` + columnas: `FCTCESRM NUMBER(1) DEFAULT 0`, `FCTCCDRM VARCHAR2(2)`,
  `FCTCTCRM/FCTCTBRM/FCTCTIRM NUMBER(14,2)`.
- `PGS.DCXP` + columna: `DCXPESRM NUMBER(1) DEFAULT 0`.
- `PGS.RMBF`: PK identity `RMBFCDGO`; FK `RMBFFCTC → PGS.FCTC(ID)`; campos
  `RMBFTIPR(2), RMBFIDPR(20) NOT NULL, RMBFCDPS(3) DEFAULT '593', RMBFTPPR(2), RMBFCDDC(2),
  RMBFESTB(3), RMBFPTEM(3), RMBFSCNL(9), RMBFFEMS DATE, RMBFNAUT(49),
  RMBFBSCR/RMBFBSGR NUMBER(14,2) DEFAULT 0, RMBFTRIV NUMBER(5,2),
  RMBFVLIV/RMBFVLIC/RMBFTTAL NUMBER(14,2) DEFAULT 0, RMBFPRDC NUMBER(11),
  RMBFORGN NUMBER(1) DEFAULT 1, RMBFESTD NUMBER(1) DEFAULT 1, RMBFOBSR(500)`.

---

## 4. Capa de persistencia nueva (los 5 archivos estándar + constante)

Seguir `docs/estandar/ESTANDAR_MAPEO_CAPAS.md`. Plantilla de referencia:
`DetalleRetencionCompraV2` (hija con FK a cabecera) y `DetalleFacturaCompra*` para DAO/Service/
Rest. Nombres EXACTOS (la resolución de NamedQuery es por concatenación: `entidad + "All"` /
`entidad + "Id"`).

### 4.1 Entidad — `src/main/java/com/saa/model/cxp/ReembolsoFacturaCompra.java`

```java
package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

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
import jakarta.persistence.Table;

/**
 * Entity ReembolsoFacturaCompra.
 * Detalle de reembolsos de gastos de una factura de compra (tabla PGS.RMBF).
 * Un registro por documento sustento (tag reembolsoDetalle del XML SRI, ANEXO 5 Ficha Tecnica).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RMBF", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "ReembolsoFacturaCompraAll", query = "select e from ReembolsoFacturaCompra e"),
    @NamedQuery(name = "ReembolsoFacturaCompraId", query = "select e from ReembolsoFacturaCompra e where e.id = :id"),
    @NamedQuery(name = "ReembolsoFacturaCompraByFactura",
        query = "select e from ReembolsoFacturaCompra e where e.factura.id = :idFactura and e.estado = 1 order by e.id")
})
public class ReembolsoFacturaCompra implements Serializable {

    @Basic @Id @Column(name = "RMBFCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Factura de compra a la que pertenece el reembolso */
    @ManyToOne @JoinColumn(name = "RMBFFCTC", referencedColumnName = "ID")
    private FacturaCompra factura;

    /** Tipo identificacion del proveedor del gasto (tabla 6 SRI: 04=RUC 05=Cedula ...) */
    @Basic @Column(name = "RMBFTIPR", length = 2)
    private String tipoIdentificacionProveedor;

    /** Identificacion del proveedor del gasto reembolsado */
    @Basic @Column(name = "RMBFIDPR", length = 20)
    private String identificacionProveedor;

    /** Codigo pais de pago (tabla 25 SRI, 593=Ecuador) */
    @Basic @Column(name = "RMBFCDPS", length = 3)
    private String codPaisPago;

    /** Tipo proveedor reembolso (tabla 26 SRI: 01=Persona natural 02=Sociedad) */
    @Basic @Column(name = "RMBFTPPR", length = 2)
    private String tipoProveedor;

    /** Tipo de documento sustento (tabla 3 SRI: 01=Factura 03=Liquidacion ...) */
    @Basic @Column(name = "RMBFCDDC", length = 2)
    private String codDoc;

    /** Establecimiento del documento sustento (estabDocReembolso) */
    @Basic @Column(name = "RMBFESTB", length = 3)
    private String establecimiento;

    /** Punto de emision del documento sustento (ptoEmiDocReembolso) */
    @Basic @Column(name = "RMBFPTEM", length = 3)
    private String puntoEmision;

    /** Secuencial del documento sustento (secuencialDocReembolso) */
    @Basic @Column(name = "RMBFSCNL", length = 9)
    private String secuencial;

    /** Fecha de emision del documento sustento */
    @Basic @Column(name = "RMBFFEMS")
    private LocalDate fechaEmision;

    /** Numero de autorizacion / clave de acceso del documento sustento */
    @Basic @Column(name = "RMBFNAUT", length = 49)
    private String numeroAutorizacion;

    /** Base imponible tarifa 0 / no objeto / exento */
    @Basic @Column(name = "RMBFBSCR")
    private Double baseImponibleCero;

    /** Base imponible gravada */
    @Basic @Column(name = "RMBFBSGR")
    private Double baseImponibleGravada;

    /** Tarifa IVA de la base gravada (15/12/8/5) */
    @Basic @Column(name = "RMBFTRIV")
    private Double tarifaIva;

    /** Valor IVA */
    @Basic @Column(name = "RMBFVLIV")
    private Double valorIva;

    /** Valor ICE */
    @Basic @Column(name = "RMBFVLIC")
    private Double valorIce;

    /** Total del documento sustento (bases + impuestos) */
    @Basic @Column(name = "RMBFTTAL")
    private Double total;

    /** Id de producto PGS.PRDP para la contabilizacion por grupo (sin FK, igual que DFCC.PRODUCTO) */
    @Basic @Column(name = "RMBFPRDC")
    private Long producto;

    /** Origen del registro: 1=Leido del XML 2=Ingresado manualmente (OrigenReembolso) */
    @Basic @Column(name = "RMBFORGN")
    private Long origen;

    /** Estado: 1=Activo 0=Anulado */
    @Basic @Column(name = "RMBFESTD")
    private Long estado;

    @Basic @Column(name = "RMBFOBSR", length = 500)
    private String observacion;

    // Getters y setters escritos a mano para TODOS los campos (sin Lombok),
    // siguiendo el estilo de DetalleRetencionCompraV2.
}
```

### 4.2 Constante — modificar `src/main/java/com/saa/model/cxp/NombreEntidadesCompra.java`

```java
	String REEMBOLSO_FACTURA_COMPRA = "ReembolsoFacturaCompra";
```

### 4.3 DAO — `src/main/java/com/saa/ejb/cxp/dao/ReembolsoFacturaCompraDaoService.java`

```java
package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.ReembolsoFacturaCompra;

import jakarta.ejb.Local;

@Local
public interface ReembolsoFacturaCompraDaoService extends EntityDao<ReembolsoFacturaCompra> {

	/**
	 * Devuelve los reembolsos ACTIVOS (estado=1) de una factura de compra, ordenados por id.
	 * Devuelve lista vacia si no hay registros (no lanza excepcion).
	 * @param idFactura id de la factura de compra (PGS.FCTC.ID)
	 */
	List<ReembolsoFacturaCompra> selectByFactura(Long idFactura);
}
```

### 4.4 DAO impl — `src/main/java/com/saa/ejb/cxp/daoImpl/ReembolsoFacturaCompraDaoServiceImpl.java`

```java
package com.saa.ejb.cxp.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.ReembolsoFacturaCompraDaoService;
import com.saa.model.cxp.ReembolsoFacturaCompra;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ReembolsoFacturaCompraDaoServiceImpl extends EntityDaoImpl<ReembolsoFacturaCompra>
		implements ReembolsoFacturaCompraDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[] { "id", "factura", "tipoIdentificacionProveedor", "identificacionProveedor",
				"codPaisPago", "tipoProveedor", "codDoc", "establecimiento", "puntoEmision", "secuencial",
				"fechaEmision", "numeroAutorizacion", "baseImponibleCero", "baseImponibleGravada",
				"tarifaIva", "valorIva", "valorIce", "total", "producto", "origen", "estado", "observacion" };
	}

	@Override
	public List<ReembolsoFacturaCompra> selectByFactura(Long idFactura) {
		try {
			return em.createNamedQuery("ReembolsoFacturaCompraByFactura", ReembolsoFacturaCompra.class)
					.setParameter("idFactura", idFactura).getResultList();
		} catch (Exception e) {
			System.out.println("Error en selectByFactura ReembolsoFacturaCompra: " + e.getMessage());
			return new ArrayList<>();
		}
	}
}
```

### 4.5 Service + impl

`src/main/java/com/saa/ejb/cxp/service/ReembolsoFacturaCompraService.java`:

```java
@Local
public interface ReembolsoFacturaCompraService extends EntityService<ReembolsoFacturaCompra> {
}
```

`src/main/java/com/saa/ejb/cxp/serviceImpl/ReembolsoFacturaCompraServiceImpl.java`: copiar
EXACTAMENTE la estructura de `DetalleFacturaCompraServiceImpl` cambiando el tipo y usando
`NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA`. En `saveSingle`, si `id == null`: poner
`estado = Long.valueOf(Estado.ACTIVO)` y **si `origen == null` poner
`origen = Long.valueOf(OrigenReembolso.MANUAL)`**. Mantener las trazas `System.out.println`.

### 4.6 Constantes — `src/main/java/com/saa/rubros/OrigenReembolso.java`

```java
package com.saa.rubros;

/** Origen de un registro de reembolso de gastos (PGS.RMBF.RMBFORGN). */
public interface OrigenReembolso {
	int XML = 1;
	int MANUAL = 2;
}
```

### 4.7 REST — `src/main/java/com/saa/ws/rest/cxp/ReembolsoFacturaCompraRest.java`

`@Path("rmbf")`. Copiar la estructura de `DetalleFacturaCompraRest` (DAO+Service con `@EJB`,
lecturas por DAO, escrituras por Service, `catch (Throwable)`). Endpoints estándar:

```
GET    /rest/rmbf/getAll
GET    /rest/rmbf/getId/{id}
POST   /rest/rmbf/selectByCriteria
POST   /rest/rmbf            (saveSingle, 201)
PUT    /rest/rmbf            (saveSingle)
DELETE /rest/rmbf/{id}
```

Más el endpoint de conveniencia:

```java
	@GET
	@Path("getByFactura/{idFactura}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getByFactura(@PathParam("idFactura") Long idFactura) {
		System.out.println("getByFactura ReembolsoFacturaCompra: " + idFactura);
		try {
			return Response.ok(reembolsoFacturaCompraDaoService.selectByFactura(idFactura)).build();
		} catch (Throwable e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error consultando reembolsos: " + e.getMessage()).build();
		}
	}
```

> Registro JAX-RS: si `com.saa.ws.rest.ApplicationConfig` lista clases explícitamente, agregar la
> nueva; si escanea por paquete, no hace falta.

---

## 5. Cambios en entidades existentes

### 5.1 `FacturaCompra.java` — 5 campos nuevos (con getters/setters)

```java
	/** Es factura de reembolso de gastos: 0=No 1=Si */
	@Basic @Column(name = "FCTCESRM")
	private Long esReembolso;

	/** codDocReembolso del XML (tabla 3 SRI, normalmente 41) */
	@Basic @Column(name = "FCTCCDRM", length = 2)
	private String codDocReembolso;

	/** totalComprobantesReembolso (del XML o recalculado desde PGS.RMBF) */
	@Basic @Column(name = "FCTCTCRM")
	private Double totalComprobantesReembolso;

	/** totalBaseImponibleReembolso */
	@Basic @Column(name = "FCTCTBRM")
	private Double totalBaseImponibleReembolso;

	/** totalImpuestoReembolso */
	@Basic @Column(name = "FCTCTIRM")
	private Double totalImpuestoReembolso;
```

**OBLIGATORIO:** agregar los 5 nombres de campo Java al `obtieneCampos()` de
`FacturaCompraDaoServiceImpl`.

### 5.2 `DocumentoCxp.java` — 1 campo nuevo

```java
	/** Marcado como factura de reembolso de gastos: 0=No 1=Si */
	@Basic @Column(name = "DCXPESRM")
	private Long esReembolso;
```

**OBLIGATORIO:** agregar `esReembolso` al `obtieneCampos()` de `DocumentoCxpDaoServiceImpl`.

---

## 6. Cambios en `ProcesoCargaDocumentosServiceImpl`

Archivo: `src/main/java/com/saa/ejb/cxp/serviceImpl/ProcesoCargaDocumentosServiceImpl.java`.
Inyectar:

```java
	@EJB private ReembolsoFacturaCompraDaoService reembolsoFacturaCompraDaoService;
```

### 6.1 Persistir el flag al cargar el XML — `cargarXmlYRegistrar` (~línea 501) y `cargarXml`

Después de setear `pathXml`/`fechaCargaXml`/`usuarioCargaXml` en el `DocumentoCxp` (antes del
save del doc):

```java
		// Flag de reembolso: lo marca el usuario en pantalla, o se autodetecta del XML
		boolean marcadoReembolso = leerFlagReembolso(params);
		boolean xmlTieneReembolsos = contenidoXml != null && contenidoXml.contains("<reembolsoDetalle>");
		if (marcadoReembolso || xmlTieneReembolsos) {
			doc.setEsReembolso(1L);
		} else if (doc.getEsReembolso() == null) {
			doc.setEsReembolso(0L);
		}
		// OJO: si doc.getEsReembolso() ya era 1 (marcado antes desde la bandeja), NO pisarlo a 0.
```

Helper:

```java
	/** Lee el flag esReembolso del payload REST (acepta Boolean, Number o String). */
	private boolean leerFlagReembolso(Map<String, Object> params) {
		Object v = params.get("esReembolso");
		if (v == null) return false;
		if (v instanceof Boolean) return (Boolean) v;
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
```

> No tocar el despacho por tipo (`TIPO_FACTURA = "Factura"`): un reembolso llega igualmente como
> "Factura" en el TXT; el tratamiento especial ocurre DENTRO de `registrarFacturaCompra`.

### 6.2 `registrarFacturaCompra` (~línea 921) — detección, productos y grabación

**(a) Detección** — tras `Document xmlDoc = parsearXmlComprobante(...)`:

```java
		// --- Reembolso de gastos (SRI ANEXO 5) ---
		// OJO: <codDocReembolso> existe tambien DENTRO de cada <reembolsoDetalle>; leerlo desde
		// <infoFactura> especificamente, NO con getXmlValue(xmlDoc, ...) que toma la primera
		// ocurrencia del documento.
		String codDocReembolsoCab = "";
		NodeList infoFacturaList = xmlDoc.getElementsByTagName("infoFactura");
		Element infoFactura = infoFacturaList.getLength() > 0 ? (Element) infoFacturaList.item(0) : null;
		if (infoFactura != null) {
			codDocReembolsoCab = getElementValue(infoFactura, "codDocReembolso");
		}
		NodeList reembolsosXml = xmlDoc.getElementsByTagName("reembolsoDetalle");
		boolean marcadoReembolso = doc.getEsReembolso() != null && doc.getEsReembolso() == 1L;
		boolean esReembolso = marcadoReembolso
				|| (codDocReembolsoCab != null && !codDocReembolsoCab.isEmpty())
				|| reembolsosXml.getLength() > 0;
```

**(b) PASO 1 — resolución de productos de los sustentos** (junto a la resolución de productos de
los `<detalle>` con `obtenerOAutoCrearProducto`, ~línea 626). Por cada `<reembolsoDetalle>`:

- Buscar `ProductoPago` por **código = `identificacionProveedorReembolso`** (mismo mecanismo de
  búsqueda que usa `obtenerOAutoCrearProducto` — revisar cómo busca por código/nombre y replicar
  el criterio, incluido el scope por empresa si lo tiene).
- Si no existe, crearlo: `nombre = "REEMBOLSO " + identificacionProveedorReembolso`,
  `codigo = identificacionProveedorReembolso`, grupo = `obtenerOCrearGrupoPendienteClasificar`
  (POR_CLASIFICAR), precio unitario = total del sustento. Reutilizar el mismo método de creación
  que usan los detalles normales si su firma lo permite; si no, factorizar.
- Guardar la lista de productos resueltos indexada igual que `productosDetalle` para asignarlos
  en el PASO 3.

**(c) PASO 2 — bloqueantes:** el bloqueante `PRODUCTOS_SIN_CLASIFICAR` (y
`GRUPOS_SIN_CUENTA_CONTABLE`) debe evaluarse así cuando `esReembolso`:
- **Los productos de los sustentos de reembolso SÍ bloquean** (entran a la lista de pendientes
  con su nombre/código para que el panel de clasificación del frontend los muestre y
  `crearProductosYRegistrar` pueda asignarles grupo — verificar que el matching de
  `crearProductosYRegistrar` funcione con estos productos: van con
  `nombre = "REEMBOLSO {identificacion}"`, `codigo = identificacion`).
- **Los productos de los `<detalle>` normales NO bloquean ni cuentan como pendientes** cuando
  `esReembolso=1` (no participan del asiento y exigir clasificarlos sería doble trabajo). Se
  siguen creando y asignando en `DFCC` como hoy, solo se excluyen de los bloqueantes/pendientes.
- Mismo criterio en `obtenerProductosPendientesDeClasificar` (usado por
  `GET /productosPendientes/{idFacturaCompra}`): si la factura es reembolso, los pendientes se
  calculan desde `RMBF`, no desde `DFCC`.

**(d) Cabecera** — donde se arma `FacturaCompra`:

```java
		if (esReembolso) {
			factura.setEsReembolso(1L);
			factura.setCodDocReembolso(codDocReembolsoCab == null || codDocReembolsoCab.isEmpty()
					? "41" : codDocReembolsoCab);
			if (infoFactura != null) {
				factura.setTotalComprobantesReembolso(parseDouble(getElementValue(infoFactura, "totalComprobantesReembolso")));
				factura.setTotalBaseImponibleReembolso(parseDouble(getElementValue(infoFactura, "totalBaseImponibleReembolso")));
				factura.setTotalImpuestoReembolso(parseDouble(getElementValue(infoFactura, "totalImpuestoReembolso")));
			}
		} else {
			factura.setEsReembolso(0L);
		}
```

**(e) PASO 3 — grabación de los sustentos** (tras grabar los `DetalleFacturaCompra`). Método
privado nuevo; recibe la lista de productos resueltos en (b):

```java
	/**
	 * Lee los <reembolsoDetalle> del XML y los graba en PGS.RMBF (uno por documento sustento).
	 * Los <detalleImpuesto> se aplanan: codigo=2 (IVA) con tarifa>0 suma a base gravada, con
	 * tarifa=0 a base cero; codigo=3 (ICE) suma a valor ICE; otros codigos suman su base a base
	 * cero. Si hay varias tarifas gravadas se conserva la del bloque de mayor impuesto (mismo
	 * criterio que leerIvaCabecera).
	 * @param productosReembolso productos resueltos en el PASO 1, indexados igual que los nodos
	 * @return numero de registros grabados
	 */
	private int grabarReembolsosDesdeXml(Document xmlDoc, FacturaCompra factura,
			List<ProductoPago> productosReembolso) throws Throwable {
		NodeList reembolsos = xmlDoc.getElementsByTagName("reembolsoDetalle");
		int grabados = 0;
		for (int i = 0; i < reembolsos.getLength(); i++) {
			Element el = (Element) reembolsos.item(i);
			ReembolsoFacturaCompra r = new ReembolsoFacturaCompra();
			r.setFactura(factura);
			r.setTipoIdentificacionProveedor(getElementValue(el, "tipoIdentificacionProveedorReembolso"));
			r.setIdentificacionProveedor(getElementValue(el, "identificacionProveedorReembolso"));
			r.setCodPaisPago(getElementValue(el, "codPaisPagoProveedorReembolso"));
			r.setTipoProveedor(getElementValue(el, "tipoProveedorReembolso"));
			r.setCodDoc(getElementValue(el, "codDocReembolso"));
			r.setEstablecimiento(getElementValue(el, "estabDocReembolso"));
			r.setPuntoEmision(getElementValue(el, "ptoEmiDocReembolso"));
			r.setSecuencial(getElementValue(el, "secuencialDocReembolso"));
			// El tag oficial es "numeroautorizacionDocReemb" (minuscula, ver XSD); tolerar la
			// variante "numeroAutorizacionDocReemb" de algunos emisores.
			String numAut = getElementValue(el, "numeroautorizacionDocReemb");
			if (numAut == null || numAut.isEmpty()) numAut = getElementValue(el, "numeroAutorizacionDocReemb");
			r.setNumeroAutorizacion(numAut);
			java.time.LocalDateTime fe = parseFechaHora(getElementValue(el, "fechaEmisionDocReembolso"));
			r.setFechaEmision(fe != null ? fe.toLocalDate() : null);

			double baseCero = 0.0, baseGravada = 0.0, valorIva = 0.0, valorIce = 0.0;
			double tarifaIva = 0.0, mayorImpuesto = -1.0;
			NodeList imps = el.getElementsByTagName("detalleImpuesto");
			for (int j = 0; j < imps.getLength(); j++) {
				Element impEl = (Element) imps.item(j);
				String codigo = getElementValue(impEl, "codigo");
				double base = parseDouble(getElementValue(impEl, "baseImponibleReembolso"));
				double imp = parseDouble(getElementValue(impEl, "impuestoReembolso"));
				double tarifa = parseDouble(getElementValue(impEl, "tarifa"));
				if ("2".equals(codigo)) {           // IVA
					if (tarifa > 0) {
						baseGravada += base;
						valorIva += imp;
						if (imp > mayorImpuesto) { mayorImpuesto = imp; tarifaIva = tarifa; }
					} else {
						baseCero += base;
					}
				} else if ("3".equals(codigo)) {    // ICE
					valorIce += imp;
				} else {
					baseCero += base;
				}
			}
			r.setBaseImponibleCero(baseCero);
			r.setBaseImponibleGravada(baseGravada);
			r.setTarifaIva(baseGravada > 0 ? tarifaIva : null);
			r.setValorIva(valorIva);
			r.setValorIce(valorIce);
			r.setTotal(baseCero + baseGravada + valorIva + valorIce);
			r.setProducto(productosReembolso.get(i).getId());
			r.setOrigen(Long.valueOf(OrigenReembolso.XML));
			r.setEstado(Long.valueOf(Estado.ACTIVO));
			reembolsoFacturaCompraDaoService.save(r, null);
			grabados++;
		}
		return grabados;
	}
```

**(f) Resultado hacia el REST** (tras grabar):

```java
		if (esReembolso) {
			resultado.put("esReembolso", true);
			resultado.put("reembolsosLeidos", reembolsosLeidos);   // valor devuelto por (e)
			if (reembolsosLeidos == 0) {
				resultado.put("reembolsoManualPendiente", true);
				resultado.put("advertenciaReembolso",
					"La factura fue marcada como reembolso de gastos pero el XML no contiene el bloque <reembolsos>. "
					+ "Ingrese los documentos sustento desde Gestión de Documentos y luego contabilice.");
			}
		}
```

### 6.3 Contabilización — regla central de este cambio

**Regla:** cuando `factura.esReembolso == 1`, el asiento se genera con el DEBE agrupado por los
**grupos de los productos de los sustentos (`RMBF`)**, NO por los del detalle (`DFCC`). El resto
del asiento se mantiene espejo del actual: la línea de IVA (crédito tributario) usa
`sum(RMBF.valorIva)`, el ICE sigue el tratamiento que tenga hoy el asiento de factura de compra
(si hoy el ICE va al costo del grupo, sumar `valorIce` al DEBE del grupo del sustento), y el
HABER al proveedor es `factura.total`.

**Dónde:** el asiento lo construye `asientoContableService.generarAsientoFacturaCompra(idDocBD,
idEmpresa, TipoAsientos.FACTURAS_COMPRA, fechaDoc, descripcion, "SISTEMA")` (módulo CNT, invocado
desde `generarAsientoCxp` ~línea 2804). Localizar la implementación (buscar
`generarAsientoFacturaCompra` en `com.saa.ejb.cnt`), y agregar la rama: si la factura tiene
`esReembolso == 1`, construir las líneas de DEBE desde `ReembolsoFacturaCompra` (traer con el DAO
nuevo o con un JPQL local) agrupando por el `GrupoProductoPago` del producto de cada sustento:
`DEBE(grupo) = sum(baseImponibleCero + baseImponibleGravada)` de sus filas. **Cambio mínimo y
quirúrgico: solo la fuente de las líneas de DEBE cambia; cuentas, redondeos, validaciones y
estructura del asiento se reutilizan tal cual.**

**Precondiciones para generar el asiento de una factura reembolso** (validar ANTES de llamar a
la generación, dentro de `generarAsientoCxp` o del método nuevo `contabilizarReembolso`):

1. Existe al menos un `RMBF` activo.
2. Todos los productos de los `RMBF` activos tienen grupo distinto de POR_CLASIFICAR y ese grupo
   tiene cuenta contable (reutilizar las mismas comprobaciones que hoy generan los bloqueantes
   `PRODUCTOS_SIN_CLASIFICAR` / `GRUPOS_SIN_CUENTA_CONTABLE`).
3. **Cuadratura:** `|sum(RMBF.total) − factura.total| <= 0.01`.

**Comportamiento cuando NO se cumplen las precondiciones (flujo XML):** NO abortar el registro ni
hacer rollback. La factura y sus `RMBF` quedan grabados, pero el asiento no se genera:
`DocumentoCxp` queda en **estado 2 (XML_CARGADO)** con `observacion` describiendo el motivo
(ej. `"REEMBOLSO: pendiente contabilización — descuadre de 3.50"` o
`"REEMBOLSO: pendiente ingreso de documentos sustento"`), y el resultado lleva
`contabilizacionPendiente: true` + `motivoContabilizacionPendiente`. Excepción: productos sin
clasificar siguen usando el mecanismo actual de bloqueantes 422 + `crearProductosYRegistrar`
(que al clasificar re-registra y ahí sí genera el asiento) — no duplicar mecanismos.

**Empresas con `Facturador.generaConta == 0`:** no generan asiento (comportamiento actual);
las facturas reembolso pasan a estado 3 directamente y la cuadratura queda solo informativa.

### 6.4 Reversión — `revertirRegistrosBD` (~línea 2039)

En el `case "FACTURA_COMPRA"`, **antes** del delete del detalle y de la cabecera:

```java
			em.createQuery("delete from ReembolsoFacturaCompra r where r.factura.id = :id")
					.setParameter("id", idDocumentoBD).executeUpdate();
```

**Si esto falta, revertir una factura reembolso falla por la FK `FK_RMBF_FACTURA`.**

### 6.5 Métodos de negocio nuevos (agregar también a la interfaz del service)

```java
	/**
	 * Marca o desmarca como reembolso de gastos un documento de la bandeja (DCXP), en cualquier
	 * estado. Si el documento ya esta registrado como FACTURA_COMPRA, cascadea a la factura.
	 * Reglas al MARCAR una factura ya registrada:
	 *   - si tiene pagos aplicados -> IncomeException (mismo criterio que revertir)
	 *   - si tiene asiento activo -> anularlo (anularAsientoDeDocumento) porque fue construido
	 *     desde DFCC y debe reconstruirse desde RMBF
	 *   - FCTC.esReembolso=1, codDocReembolso='41' si estaba null
	 *   - DCXP pasa a estado 2 con observacion "REEMBOLSO: pendiente ingreso de documentos
	 *     sustento y contabilizacion" (si generaConta==1; si ==0 se queda en 3)
	 * Reglas al DESMARCAR:
	 *   - si hay RMBF activos -> IncomeException ("elimine primero los documentos de reembolso")
	 *   - limpiar los 5 campos de reembolso de FCTC
	 *   - si el documento estaba en estado 2 por reembolso pendiente y generaConta==1 ->
	 *     regenerar el asiento normal (desde DFCC) reutilizando generarAsientoCxp y pasar a 3
	 * Si el documento esta registrado en otra tabla destino -> IncomeException
	 * ("solo aplica a facturas").
	 */
	public Map<String, Object> marcarReembolso(Long idDocumentoCxp, boolean esReembolso, Long idUsuario) throws Throwable

	/**
	 * Contabiliza una factura de reembolso cuya contabilizacion quedo pendiente (flujo manual o
	 * descuadre corregido). Valida las 3 precondiciones de §6.3; si alguna falla lanza
	 * IncomeException con mensaje descriptivo (el REST la convierte en 422 con
	 * {error, bloqueantes?}). Si pasan: genera el asiento (rama reembolso), actualiza DCXP a
	 * estado 3 (fechaRegistroBD/usuarioRegistroBD si estaban null, observacion=null) y devuelve
	 * {idFacturaCompra, asiento, cuadratura...}.
	 */
	public Map<String, Object> contabilizarReembolso(Long idFacturaCompra, Long idEmpresa, Long idUsuario) throws Throwable

	/**
	 * Recalcula los totales de reembolso de la cabecera desde los RMBF activos y devuelve la
	 * cuadratura contra factura.total. El frontend lo invoca despues de cada alta/edicion/
	 * borrado manual. Devuelve {idFacturaCompra, cantidadReembolsos, totalComprobantesReembolso,
	 * totalBaseImponibleReembolso, totalImpuestoReembolso, importeTotalFactura, diferencia,
	 * cuadra}. totalBase = sum(baseCero+baseGravada); totalImpuesto = sum(valorIva+valorIce);
	 * totalComprobantes = sum(total). Persiste los 3 totales en FCTC.
	 */
	public Map<String, Object> recalcularTotalesReembolso(Long idFacturaCompra) throws Throwable

	/**
	 * Crea un ProductoPago en el grupo POR_CLASIFICAR (para el alta manual de sustentos desde la
	 * pantalla). Reutiliza obtenerOCrearGrupoPendienteClasificar y la misma logica de creacion
	 * de productos del PASO 1. Si ya existe un producto con ese codigo, lo devuelve sin crear.
	 * @return el ProductoPago existente o creado
	 */
	public ProductoPago crearProductoPorClasificar(String nombre, String codigo, Long idEmpresa) throws Throwable
```

---

## 7. Cambios en `ProcesoCargaDocumentosRest` (`@Path("carga-documentos")`)

1. **`POST /procesarXml/{id}` y `POST /cargarXml/{id}`**: el body ya es un `Map`; solo garantizar
   que la clave `esReembolso` del payload llega al service (§6.1). Payload:
   `{contenidoXml, idEmpresa, idUsuario, pathDestino?, esReembolso?: 0|1}`.

2. **Endpoints nuevos** (mismo estilo de la clase: trazas, `catch (Throwable)`,
   `IncomeException` → 422 con cuerpo JSON):

```
POST /carga-documentos/marcarReembolso/{idDocumentoCxp}
     body {esReembolso: 0|1, idUsuario}
     200 {idDocumentoCxp, esReembolso, idFacturaCompra?, estadoDocumento}
     422 {error} (pagos aplicados / RMBF activos / tabla destino no soportada)

POST /carga-documentos/contabilizarReembolso/{idFacturaCompra}
     body {idEmpresa, idUsuario}
     200 {idFacturaCompra, asiento?, cantidadReembolsos, diferencia, cuadra}
     422 {error, bloqueantes?}

POST /carga-documentos/recalcularTotalesReembolso/{idFacturaCompra}
     (sin body o body vacio)
     200 {idFacturaCompra, cantidadReembolsos, totalComprobantesReembolso,
          totalBaseImponibleReembolso, totalImpuestoReembolso, importeTotalFactura,
          diferencia, cuadra}

POST /carga-documentos/crearProductoPorClasificar
     body {nombre, codigo?, idEmpresa}
     200/201 ProductoPago (JSON completo de la entidad)
```

---

## 8. Cambio puntual en módulo CNT

Único cambio fuera de CXP: la rama `esReembolso` dentro de la implementación de
`generarAsientoFacturaCompra` (ver §6.3). No tocar nada más de CNT (otro equipo trabaja ahí);
si el método vive en una clase con cambios en curso, coordinar con el usuario antes de editar.

---

## 9. Contrato API acordado con el frontend

**Copia idéntica en el documento frontend — cualquier desviación al implementar debe reportarse.**

```
POST /SaaBE/rest/carga-documentos/procesarXml/{idDocumentoCxp}
  body: {contenidoXml, idEmpresa, idUsuario, pathDestino?, esReembolso?: 0|1}
  200 (campos NUEVOS sumados a los existentes):
       {..., esReembolso?: true, reembolsosLeidos?: number,
        advertenciaReembolso?: string, reembolsoManualPendiente?: true,
        contabilizacionPendiente?: true, motivoContabilizacionPendiente?: string}

POST /SaaBE/rest/carga-documentos/marcarReembolso/{idDocumentoCxp}
POST /SaaBE/rest/carga-documentos/contabilizarReembolso/{idFacturaCompra}
POST /SaaBE/rest/carga-documentos/recalcularTotalesReembolso/{idFacturaCompra}
POST /SaaBE/rest/carga-documentos/crearProductoPorClasificar
  (payloads y respuestas: ver §7)

CRUD estándar tabla nueva:
  GET    /SaaBE/rest/rmbf/getAll
  GET    /SaaBE/rest/rmbf/getId/{id}
  GET    /SaaBE/rest/rmbf/getByFactura/{idFactura}    (solo activos, ordenados por id)
  POST   /SaaBE/rest/rmbf                             (alta; origen autocompleta a 2=MANUAL)
  PUT    /SaaBE/rest/rmbf                             (edición)
  DELETE /SaaBE/rest/rmbf/{id}                        (borrado físico)
  POST   /SaaBE/rest/rmbf/selectByCriteria

JSON de ReembolsoFacturaCompra (JSON-B, entidad directa):
  {id, factura: {id, ...}, tipoIdentificacionProveedor, identificacionProveedor, codPaisPago,
   tipoProveedor, codDoc, establecimiento, puntoEmision, secuencial, fechaEmision ("YYYY-MM-DD"),
   numeroAutorizacion, baseImponibleCero, baseImponibleGravada, tarifaIva, valorIva, valorIce,
   total, producto, origen, estado, observacion}
  - En altas el frontend envía factura: {id: <idFacturaCompra>} (stub) y producto: <idProducto>.

Campos NUEVOS serializados en FacturaCompra: esReembolso, codDocReembolso,
  totalComprobantesReembolso, totalBaseImponibleReembolso, totalImpuestoReembolso.
Campo NUEVO en DocumentoCxp: esReembolso.
```

---

## 10. Orden de implementación sugerido

1. Entidad `ReembolsoFacturaCompra` + constante + `OrigenReembolso`.
2. Campos nuevos en `FacturaCompra`/`DocumentoCxp` + sus `obtieneCampos()`.
3. DAO/DaoImpl/Service/ServiceImpl/Rest de `ReembolsoFacturaCompra`.
4. §6.1, §6.2 y §6.4 (parseo, productos de sustentos, reversión).
5. §6.3 y §8 (contabilización) + §6.5 y §7 (endpoints de negocio).
6. §11 (documentación).

## 11. Actualización de documentación (obligatoria)

En `docs/logica-negocio/cxp/proceso-carga-documentos.md` agregar sección "Facturas de reembolso
de gastos": detección (flag usuario + autodetección), tabla `PGS.RMBF`, campos nuevos FCTC/DCXP,
resolución de productos de sustentos, regla de contabilización desde RMBF con sus 3
precondiciones, estados (estado 2 con contabilización pendiente), endpoints nuevos, delete en
`revertirRegistrosBD`.

## 12. Criterios de aceptación (el usuario prueba en Eclipse + WildFly)

1. **XML con `<reembolsos>`, productos de sustentos ya clasificados** → factura registrada con
   `esReembolso=1`, N filas RMBF `origen=1` con producto asignado, asiento generado con DEBE por
   los grupos de los sustentos (no por los del detalle), DCXP estado 3.
2. **XML con `<reembolsos>`, sustentos con productos nuevos** → 422 con bloqueantes
   PRODUCTOS_SIN_CLASIFICAR listando los productos "REEMBOLSO {identificación}"; el panel de
   clasificación existente los asigna y `crearProductosYRegistrar` completa registro + asiento.
3. **XML con `<reembolsos>` descuadrado** (sum sustentos ≠ total factura) → factura + RMBF
   grabados, SIN asiento, DCXP estado 2 con observación de descuadre; tras corregir los RMBF por
   pantalla, `contabilizarReembolso` genera el asiento y pasa a estado 3.
4. **XML sin `<reembolsos>` marcado como reembolso** → factura grabada, `reembolsoManualPendiente`,
   DCXP estado 2; alta manual de sustentos + `recalcularTotalesReembolso` + `contabilizarReembolso`
   → asiento desde RMBF, estado 3.
5. **XML normal sin marcar** → comportamiento idéntico al actual (regresión cero).
6. **Marcar reembolso un documento en estado 1/2 (bandeja o gestión)** → solo cambia DCXPESRM; al
   subir luego el XML el flag se respeta.
7. **Marcar reembolso una factura ya registrada y contabilizada** → asiento anulado, estado 2,
   pendiente de sustentos; con pagos aplicados → 422.
8. **Desmarcar** con RMBF activos → 422; sin RMBF → limpia campos y, si estaba pendiente por
   reembolso, regenera el asiento normal y vuelve a estado 3.
9. **Revertir** una factura reembolso → no falla por FK; RMBF eliminados; asiento anulado.
10. **Empresa con generaConta=0** → reembolsos se graban, no hay asiento, estado 3 directo.
