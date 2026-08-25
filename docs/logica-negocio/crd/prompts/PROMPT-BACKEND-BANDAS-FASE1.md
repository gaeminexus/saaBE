# PROMPT — Agente BACKEND · Fase 1: modelo dinámico de bandas por producto

> **Etiqueta: BACKEND** (repo `saaBE`). **Orden:** este prompt corre PRIMERO; el prompt
> FRONTEND (`PROMPT-FRONTEND-BANDAS-FASE1.md`) depende del documento de API que este
> agente va llenando. **La BD local ya está lista**: las tablas `CRD.CBPR`/`CRD.BNDP`
> existen y están cargadas (28 configuraciones, 143 bandas) en el Oracle de docker
> `saa-oracle-23ai` — NO ejecutes DDL.

---

Implementa la Fase 1 del modelo dinámico de bandas de cartera del módulo de créditos.

## Lectura obligatoria antes de tocar código

1. `CLAUDE.md` (raíz) — convenciones del proyecto. En particular: no puedes compilar
   (`mvn` no está en PATH; la compilación la hace el usuario en Eclipse); serialización
   Jackson y formato de fechas; patrón de capas.
2. `docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` — TODO el
   contexto de negocio. Autoritativo: §2 (cuentas), §5 (reglas), §6.3 (algoritmo de
   clasificación), §8 (modelo de bandas y decisiones cerradas), §9.1 (decisiones — no
   re-preguntar nada de esa lista).
3. `docs/logica-negocio/crd/sql/DDL-BANDAS-PRODUCTO.sql` — estructura exacta de las
   tablas que vas a mapear (columnas, FKs, CHECKs, semántica de cada campo).
4. `docs/estandar/GUIA-MAPEO-TABLA-COMPLETO.md` y `docs/estandar/ESTANDAR_MAPEO_CAPAS.md`
   — plantilla canónica de los 5 archivos por tabla. Copia una entidad existente del
   módulo (p.ej. `com.saa.model.crd.Producto`) en vez de inventar estructura.

## Alcance de esta fase (y nada más)

**SÍ:** mapeo de las dos tablas, servicios de parametrización y clasificación, endpoints
REST, y el documento de contrato de API. **NO:** los procesos contables (apertura/cierre,
asiento de vencidos, cambio de bandas, integración con pagos) — son fases posteriores; no
los implementes ni los esboces.

### 1. Mapeo de tablas (patrón de 5 archivos + constantes)

- `CRD.CBPR` → `com.saa.model.crd.ConfiguracionBandaProducto`
- `CRD.BNDP` → `com.saa.model.crd.BandaProducto`
- PKs por `GenerationType.IDENTITY` (las tablas usan IDENTITY, no secuencia).
- FKs: `ConfiguracionBandaProducto.producto` → `Producto`; `.empresa` → `com.saa.model.scp.Empresa`;
  `BandaProducto.configuracion` → `ConfiguracionBandaProducto`; `.planCuenta` → `com.saa.model.cnt.PlanCuenta`.
- `CBPRTPCR` (tipo cartera) se mapea como `Long tipoCartera` (1 = por vencer, 2 = vencido).
  Crea la interfaz de constantes `com.saa.rubros.TipoCarteraBanda` con esos dos valores.
- Registra las constantes de nombre de entidad en `com.saa.model.crd.NombreEntidadesCredito`
  y las `@NamedQuery` `XxxAll`/`XxxId` EXACTAMENTE con esos nombres (el DAO genérico las
  resuelve por concatenación; un desajuste revienta en runtime).
- Campos de auditoría: sigue el patrón de `Producto` (FCRG/USRG/IPRG + FCMD/USMD/IPMD + ESTD).

### 2. Capa REST estándar

`com.saa.ws.rest.crd.ConfiguracionBandaProductoRest` con `@Path("cbpr")` y
`BandaProductoRest` con `@Path("bndp")`: `getAll`, `getId/{id}`, `POST`/`PUT` saveSingle,
`DELETE /{id}`, `selectByCriteria`. Lecturas contra el DAO, escrituras por el Service,
`@EJB` para inyectar, línea de traza `System.out.println` al inicio de cada método,
`catch (Throwable e)` → 500 con `"Error ...: " + e.getMessage()` — el estilo de la casa.

### 3. Servicio de parametrización (lo que consume la pantalla)

En `ConfiguracionBandaProductoService/Impl` + endpoints en su Rest:

- **Configuración vigente**: dado producto + empresa + tipoCartera (+ fecha opcional,
  default hoy) devolver la configuración con `CBPRFCFN IS NULL` (o vigente a la fecha) y
  sus bandas ordenadas por número, enriquecidas con el rango derivado en días:
  `diaInicio(k) = 30*Σ periodos(1..k−1) + 1`, `diaFin(k) = 30*Σ periodos(1..k)`,
  última banda (periodos NULL) = abierta (`diaFin` null, etiqueta "resto").
- **Listado para la pantalla**: por empresa, todos los productos (activos e inactivos,
  marcando el estado) con sus configuraciones vigentes de ambos tipos de cartera y sus
  bandas; los productos sin configuración también deben aparecer (hoy: PRENDARIO NOVACION
  e HIPOTECARIO NOVACION no tienen la de por vencer — ver §4 de
  `CARGA-INICIAL-BANDAS-PRODUCTO.md`).
- **Guardado de configuración completa** (cabecera + lista de bandas en una sola
  transacción) con validaciones que lanzan `IncomeException` con mensaje claro:
  números de banda consecutivos desde 1; exactamente UNA banda con periodos NULL y debe
  ser la última; las demás con periodos >= 1; cuenta contable obligatoria, existente,
  activa y de la misma empresa; tipoCartera en {1,2}; única configuración vigente por
  (producto, empresa, tipoCartera).
- **Cierre de vigencia**: operación que fija `CBPRFCFN` de la configuración vigente y
  crea la nueva a partir de una fecha dada (así se modelará el cambio normativo). La
  edición de una configuración cuya vigencia ya empezó debe hacerse por esta vía, no
  mutando bandas en caliente; una configuración cuya vigencia aún no empieza sí se puede
  editar en el lugar.

### 4. Servicio de clasificación (corazón del modelo — lo reusarán todas las fases)

Método de negocio, p.ej. en un `ClasificadorBandaService`:
`clasificar(idProducto, idEmpresa, tipoCartera, dias, fecha)` → devuelve la banda
(número, periodos, rango) y su `PlanCuenta`. Regla: primera banda k tal que
`dias <= diaFin(k)`; la abierta captura el resto; `dias < 1` es error de negocio.
Exponer un endpoint GET de prueba (documéntalo como "de verificación") para que QA y el
frontend puedan validar la parametrización.

### 5. Documento de contrato de API — OBLIGACIÓN CONTINUA

`docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md` ya existe con la plantilla. **Cada vez
que un endpoint quede implementado, regístralo ahí en el mismo cambio**: método, ruta,
request y response con JSON REALES (llama al endpoint o construye el JSON exacto que
produce Jackson — recuerda que las fechas `LocalDate` serializan como el formato que ya
usa el sistema), errores y validaciones. El agente FRONTEND va a construir la pantalla
leyendo SOLO ese documento: si un endpoint no está documentado, para el frontend no existe.

### 6. Verificación

No puedes compilar: revisa dos veces imports, nombres de NamedQueries y firmas contra la
guía de mapeo. Deja al final un resumen de: archivos creados, endpoints expuestos, y
cualquier decisión que hayas tenido que tomar sin respaldo en los documentos (márcala
como PENDIENTE DE VALIDAR, no la escondas).
