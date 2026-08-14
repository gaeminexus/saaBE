# ESPECIFICACIÓN — SERVICIOS DE PAGO DE PRÉSTAMOS (módulo crd)

**Documento de diseño para implementación. Escrito el 2026-08-13, verificado contra el código.**

Este documento especifica, con nivel de detalle de implementación, los servicios y endpoints
nuevos de pago de préstamos. Está pensado para ser implementado **por fases, en orden** (§10).
Leer antes: `docs/logica-negocio/petro/REGLAS-GENERALES-PETRO.md` y `REGLAS-CARGA-PETRO.md`
(el motor de pagos nuevo replica esas reglas), y
`docs/logica-negocio/crd/ACTUALIZACION-SALDOOTROS-CANCELADOS-ANTICIPADOS.md` (precedente del uso
de `saldoOtros` como capital precancelado).

## Índice
1. Contexto y alcance
2. Decisiones de negocio ya tomadas
3. Pre-requisitos obligatorios (Fase 0)
4. DDL — tablas nuevas y alteraciones
5. Arquitectura de servicios y DTOs
6. El motor de pagos (`MotorPagoPrestamoService`)
7. Procesos de negocio (reglas + casos borde)
8. Contratos REST
9. Hooks de contabilidad
10. Orden de implementación por fases y pruebas
11. Verificación end-to-end

---

## 1. Contexto y alcance

### 1.1 Situación actual (verificada en código)

- **No existe pago manual de cuotas.** `PagoPrestamoRest` (`@Path("pgpr")`) es CRUD crudo: un
  `POST /pgpr` inserta un `PagoPrestamo` pero no aplica nada a la cuota ni al préstamo.
- Toda la lógica de aplicación de pagos vive **privada** en
  `com.saa.ejb.asoprep.serviceImpl.CargaArchivoPetroServiceImpl` (proceso Petro), acoplada a
  `CargaArchivo`/`ParticipeXCargaArchivo`.
- `PrestamoServiceImpl.aplicarAbonoCapital(...)` existe pero es **defectuoso** (§3.3) y se
  reemplaza por completo.
- El frontend calcula saldos de aportes descargando TODA la tabla `CRD.APRT` (~980.000 filas) con
  `GET /aprt/getAll` — causa del `OutOfMemoryError` documentado en
  `docs/general/infraestructura/AUMENTAR_MEMORIA_WILDFLY.md`.

### 1.2 En alcance

| # | Proceso | Servicio |
|---|---|---|
| 1 | Pago de cuota(s) con valor (parcial / exacto / con excedente en cascada) | `ProcesoPagoPrestamoService.pagarCuota` |
| 2 | Abono a capital con re-amortización (2 modalidades) + históricos | `AbonoCapitalPrestamoService` |
| 3 | Pago de cuotas con aportes + saldo de aportes por entidad servido por backend | `ProcesoPagoPrestamoService.pagarConAportes`, `SaldoAporteService` |
| 4 | Precancelación total (valor, aportes o mixto) | `ProcesoPagoPrestamoService.precancelar` (+ simulación) |
| 5 | Reverso / anulación de operaciones | `ProcesoPagoPrestamoService.anularOperacion` |
| — | Motor de pagos compartido | `MotorPagoPrestamoService` |
| — | Hooks de contabilidad (no-op inicial) | `ContabilidadPrestamoService` |

### 1.3 Fuera de alcance (dejar TODOs, no implementar)

- Refactor del proceso Petro para que consuma el motor nuevo (dejar comentario TODO en
  `CargaArchivoPetroServiceImpl`).
- Contabilidad real (solo hooks no-op; pre-requisitos contables listados en §9.3).
- Servicio nocturno de cálculo de interés de mora diario (futuro; el motor YA cobra lo que ese
  proceso escriba en `DTPRMRAA`/`DTPRINVN`).
- Condonación de deuda y novación (identificados como tipos de pago futuros).
- Prelación parametrizada vía `CRD.OAVP` (fase futura; hardcodeada por ahora).

---

## 2. Decisiones de negocio ya tomadas (NO re-preguntar)

1. **Precancelación**: se cobra la deuda **exigible** (cuotas pendientes con vencimiento hasta la
   fecha, incluida la cuota del mes en curso, con su total real incluyendo mora) **más SOLO el
   capital pendiente** de las cuotas futuras. Intereses, desgravamen y seguros futuros se
   condonan.
2. **Cuotas históricas al re-amortizar**: tabla espejo nueva **`CRD.HDTP`** + tabla de evento
   **`CRD.EVPR`**. Las cuotas reemplazadas se copian a HDTP y se borran de DTPR en la misma
   transacción (patrón `MayorizacionServiceImpl.respaldaAsientosMayorizacion` /
   `CNT.ASNH`). **Prohibido** dejarlas en DTPR con un estado nuevo: rompería
   `contarCuotasPendientesByPrestamo` y todos los reportes que suman DTPR.
3. **Mora**: SÍ se cobra. El motor incluye mora (`DTPRMRAA`) e interés vencido (`DTPRINVN`) en
   los saldos y en la prelación. Un proceso nocturno futuro los alimentará a diario; hoy pueden
   estar en 0 y el motor simplemente no encuentra nada que cobrar por esos conceptos.
4. **Reverso**: SÍ se especifica, para los 4 tipos de operación, agrupado por evento (§7.5).

---

## 3. Pre-requisitos obligatorios (Fase 0 — sin esto el resto NO funciona)

### 3.1 Fix de `PrestamoServiceImpl.generarTablaAmortizacion`

**Bug actual**: el generador Java NO llena `saldoInicialCapital` (DTPRSICP), `total` (DTPRTTLL),
y deja `desgravamen`/`valorSeguroIncendio` implícitos. El motor de pagos depende de esos campos
(`calcularSaldosRealesCuota` usa `cuota.getTotal()` como pendiente base cuando no hay pagos, y
todos los cierres calculan `saldoCapital = max(0, saldoInicialCapital − capitalPagado)`), así que
hoy los pagos SOLO funcionan sobre tablas cargadas por Excel.

En ambos generadores (francesa, línea ~158, y alemana, línea ~294) y en la cuota 0 de gracia,
al construir cada `DetallePrestamo` agregar:

```java
detalle.setSaldoInicialCapital(redondear(saldoCapitalAntesDeLaCuota)); // capital pendiente ANTES de pagar esta cuota
detalle.setDesgravamen(0.0);                 // ya estaba; dejar explícito
detalle.setValorSeguroIncendio(0.0);         // NUEVO (hoy queda null)
detalle.setTotal(redondear(cuotaCalculada)); // NUEVO: total = cuota + desgravamen + valorSeguroIncendio (con 0.0 = cuota)
detalle.setTotalConSeguro(detalle.getTotal());
// estado ya se setea a 1; verificar que idEstado también quede en 1 (espejo)
```

Debe cumplirse el **invariante de la carga Excel**:
`saldoInicialCapital = capital + saldoCapital + saldoOtros` (con `saldoOtros = 0` al generar).
`saldoCapitalAntesDeLaCuota` es el acumulador que el bucle ya maneja ANTES de restarle
`capitalCuota` (cuidado con el orden: capturarlo antes del `saldoCapital -= capitalCuota`).

### 3.2 Espejo `DTPRIDST` = `DTPRESTD`

- Regla permanente: **toda escritura de estado de cuota setea ambos campos** con el mismo valor.
  Revisar `PrestamoServiceImpl` (generadores, carga Excel, recálculos) y corregir donde falte.
- Script de datos previo al paso a producción (documentar en el commit, estilo MD revisable):
  ```sql
  -- control previo
  SELECT COUNT(*) FROM CRD.DTPR WHERE NVL(DTPRIDST,-1) <> NVL(DTPRESTD,-1);
  -- sincronización
  UPDATE CRD.DTPR SET DTPRIDST = DTPRESTD
  WHERE  NVL(DTPRIDST,-1) <> NVL(DTPRESTD,-1) AND DTPRESTD IS NOT NULL;
  ```

### 3.3 Eliminar el abono a capital viejo

Eliminar `PrestamoServiceImpl.aplicarAbonoCapital`, `recalcularMantenPlazoCuotaMenor`,
`recalcularReducePlazoCuotaIgual`, la declaración en `PrestamoService` y el endpoint
`POST /prst/aplicarAbonoCapital/{id}/{valorAbono}/{opcionRecalculo}` de `PrestamoRest`.
Defectos que motivan el reemplazo (para el mensaje de commit): usa `saldoInicialCapital` (NULL en
tablas Java), considera "pendientes" solo estados 2/5, recalcula siempre en francés aunque el
préstamo sea alemán, borra cuotas físicamente sin respaldo, no crea `PagoPrestamo`, no actualiza
`Prestamo.plazo`, no llena los campos del invariante en las cuotas nuevas.

### 3.4 Ejecutar el DDL de §4

Los índices sobre `CRD.APRT` crearlos `ONLINE` y en ventana de bajo uso (980k filas).

---

## 4. DDL — tablas nuevas y alteraciones

Seguir `docs/estandar/ESTANDARES-CREACION-TABLAS-ORACLE.md` (comentarios de tabla/columna
incluidos, grants a `ROLE_CRD`).

### 4.1 CRD.EVPR — EventoPrestamo (cabecera de TODA operación de pago)

**Concepto clave**: cada llamada de negocio (pago manual, pago con aportes, abono a capital,
precancelación) crea UN `EventoPrestamo`. Todos los `PagoPrestamo`, movimientos de aportes y
cuotas historizadas de esa llamada cuelgan del evento. Esto da: huella de auditoría en el
préstamo, agrupación natural para el reverso (§7.5) y punto único para el asiento contable.

```sql
CREATE TABLE CRD.EVPR (
    EVPRCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    PRSTCDGO NUMBER NOT NULL,                  -- FK CRD.PRST
    EVPRTPOO VARCHAR2(30) NOT NULL,            -- PAGO_MANUAL | PAGO_APORTES | ABONO_CAPITAL | PRECANCELACION
    EVPRVLRR NUMBER(18,2) NOT NULL,            -- valor total de la operación
    EVPRMDLD NUMBER,                           -- abono: 1=reduce plazo, 2=reduce cuota. NULL en otros tipos
    EVPRPLZA NUMBER,                           -- plazo anterior (abono modalidad 1)
    EVPRPLZN NUMBER,                           -- plazo nuevo
    EVPRCTAA NUMBER(18,2),                     -- valor cuota anterior (abono modalidad 2)
    EVPRCTNN NUMBER(18,2),                     -- valor cuota nuevo
    EVPRFCHA DATE NOT NULL,                    -- fecha de negocio de la operación
    EVPRNMAS NUMBER,                           -- número de asiento contable (hook futuro; NULL por ahora)
    EVPROBSR VARCHAR2(2000),                   -- observación del usuario
    EVPRUSAR VARCHAR2(50) NOT NULL,            -- usuario que ejecutó
    EVPRFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    EVPRESTD NUMBER DEFAULT 1 NOT NULL,        -- 1 = vigente, 0 = anulado
    EVPRUSAN VARCHAR2(50),                     -- usuario anulación
    EVPRFCAN TIMESTAMP,                        -- fecha anulación
    EVPRMTAN VARCHAR2(500),                    -- motivo anulación
    CONSTRAINT PK_EVPR PRIMARY KEY (EVPRCDGO),
    CONSTRAINT FK_EVPR_PRST FOREIGN KEY (PRSTCDGO) REFERENCES CRD.PRST(PRSTCDGO),
    CONSTRAINT CK_EVPR_TIPO CHECK (EVPRTPOO IN ('PAGO_MANUAL','PAGO_APORTES','ABONO_CAPITAL','PRECANCELACION'))
);
CREATE INDEX IDX_EVPR_PRST ON CRD.EVPR(PRSTCDGO);
```

Entidad JPA: `com.saa.model.crd.EventoPrestamo` (patrón estándar 5 archivos:
entidad + constante en `NombreEntidadesCredito` + `EventoPrestamoDaoService`/Impl +
`EventoPrestamoService`/Impl + `EventoPrestamoRest @Path("evpr")` con CRUD de solo lectura —
getAll/getId/selectByCriteria; los eventos SOLO se crean/anulan por los procesos).

### 4.2 CRD.HDTP — HistDetallePrestamo (cuotas historizadas)

Espejo de **todas** las columnas de `CRD.DTPR` **con sus mismos nombres** (facilita el copy campo
a campo y las consultas comparativas), más las columnas propias:

```sql
CREATE TABLE CRD.HDTP (
    HDTPCDGO NUMBER GENERATED BY DEFAULT AS IDENTITY (START WITH 1 INCREMENT BY 1) NOT NULL,
    DTPRCDGO NUMBER NOT NULL,        -- código que tenía la cuota en DTPR (sin FK: la fila viva se borra)
    EVPRCDGO NUMBER NOT NULL,        -- FK CRD.EVPR: evento que la historizó
    HDTPMTVO VARCHAR2(30) NOT NULL,  -- ABONO_CAPITAL | PRECANCELACION | REVERSO (redundante con EVPR, útil en consultas)
    HDTPFCRG TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    HDTPUSAR VARCHAR2(50) NOT NULL,
    -- ▼ espejo 1:1 de DTPR (mismos nombres, mismos tipos):
    PRSTCDGO NUMBER NOT NULL,
    DTPRNMCT NUMBER, DTPRFCVN DATE, DTPRCPTL NUMBER(18,2), DTPRINTR NUMBER(18,2),
    DTPRMRAA NUMBER(18,2), DTPRINVN NUMBER(18,2), DTPRSLCP NUMBER(18,2), DTPRSLIN NUMBER(18,2),
    DTPRSLMR NUMBER(18,2), DTPRSLIV NUMBER(18,2), DTPRFCPG DATE, DTPRABNO NUMBER(18,2),
    DTPRCPPG NUMBER(18,2), DTPRINPG NUMBER(18,2), DTPRDSGR NUMBER(18,2), DTPRCTAA NUMBER(18,2),
    DTPRSLDO NUMBER(18,2), DTPRSLOT NUMBER(18,2), DTPRDSFR NUMBER(18,2), DTPRDSDF NUMBER(18,2),
    DTPRDSOR NUMBER(18,2), DTPRVLDF NUMBER(18,2), DTPRTTLL NUMBER(18,2), DTPRMRPG NUMBER(18,2),
    DTPRDSPG NUMBER(18,2), DTPRINVP NUMBER(18,2), DTPRMRCL NUMBER(18,2), DTPRDSMR NUMBER,
    DTPRESTD NUMBER, DTPRFCRG DATE, DTPRUSRG VARCHAR2(200), DTPRIDST NUMBER, DTPRCDEX NUMBER,
    DTPROTSG NUMBER(18,2), DTPRTTCS NUMBER(18,2), DTPRVLSI NUMBER(18,2), DTPRSICP NUMBER(18,2),
    CONSTRAINT PK_HDTP PRIMARY KEY (HDTPCDGO),
    CONSTRAINT FK_HDTP_EVPR FOREIGN KEY (EVPRCDGO) REFERENCES CRD.EVPR(EVPRCDGO)
);
CREATE INDEX IDX_HDTP_PRST ON CRD.HDTP(PRSTCDGO);
CREATE INDEX IDX_HDTP_EVPR ON CRD.HDTP(EVPRCDGO);
```

⚠️ Al implementar, **contrastar la lista de columnas contra la entidad
`com.saa.model.crd.DetallePrestamo` real** (es la autoridad); si la entidad tiene algún campo no
listado aquí, agregarlo al espejo. Entidad JPA `HistDetallePrestamo` con los mismos nombres de
campo Java que `DetallePrestamo` + `codigoOriginal`, `eventoPrestamo`, `motivo`,
`fechaRegistroHist`, `usuarioHist`.

### 4.3 ALTER de tablas existentes

```sql
-- CRD.PGPR (PagoPrestamo): evento + asiento + anulación
ALTER TABLE CRD.PGPR ADD (
    EVPRCDGO NUMBER,                 -- FK evento; NULL en pagos petro/migración existentes
    PGPRASNT NUMBER,                 -- código de asiento contable (hook futuro)
    PGPRANUL NUMBER DEFAULT 0,       -- 0 = vigente, 1 = anulado
    PGPRUSAN VARCHAR2(50),
    PGPRFCAN TIMESTAMP,
    PGPRMTAN VARCHAR2(500)
);
ALTER TABLE CRD.PGPR ADD CONSTRAINT FK_PGPR_EVPR FOREIGN KEY (EVPRCDGO) REFERENCES CRD.EVPR(EVPRCDGO);
CREATE INDEX IDX_PGPR_EVPR ON CRD.PGPR(EVPRCDGO);

-- CRD.PGAP (PagoAporte): vínculo al pago de préstamo que consumió el aporte
ALTER TABLE CRD.PGAP ADD (PGPRCDGO NUMBER);
ALTER TABLE CRD.PGAP ADD CONSTRAINT FK_PGAP_PGPR FOREIGN KEY (PGPRCDGO) REFERENCES CRD.PGPR(PGPRCDGO);
CREATE INDEX IDX_PGAP_PGPR ON CRD.PGAP(PGPRCDGO);

-- CRD.APRT: índices para saldo agregado y FIFO (hoy la tabla no tiene índices funcionales)
CREATE INDEX IDX_APRT_ENTD_TPAP      ON CRD.APRT(ENTDCDGO, TPAPCDGO) ONLINE;
CREATE INDEX IDX_APRT_ENTD_TPAP_SLDO ON CRD.APRT(ENTDCDGO, TPAPCDGO, APRTSLDO) ONLINE;
```

Actualizar entidades Java: `PagoPrestamo` += `eventoPrestamo` (ManyToOne EVPRCDGO), `asiento`
(Long PGPRASNT), `anulado` (Long PGPRANUL), `usuarioAnulacion`, `fechaAnulacion`,
`motivoAnulacion`; `PagoAporte` += `pagoPrestamo` (ManyToOne PGPRCDGO).

---

## 5. Arquitectura de servicios y DTOs

Paquetes: interfaces `@Local` en `com.saa.ejb.crd.service`, impls `@Stateless` en
`com.saa.ejb.crd.serviceImpl`, DTOs POJO (sin anotaciones JPA, getters/setters a mano — estilo de
la casa, sin Lombok) en `com.saa.ejb.crd.service.dto`. Convenciones: métodos con línea de traza
`System.out.println` al inicio; `throws Throwable`; errores de negocio con `IncomeException`.

```
MotorPagoPrestamoService        (§6)  núcleo compartido de aplicación de pagos
ProcesoPagoPrestamoService      (§7)  orquestador: pagarCuota, pagarConAportes,
                                      simularPrecancelacion, precancelar, anularOperacion
AbonoCapitalPrestamoService     (§7.3) simular + aplicar
SaldoAporteService              (§7.4) saldos por entidad/tipo (query agregada)
ContabilidadPrestamoService     (§9)  hooks no-op
```

### 5.1 DTOs

```java
public class ContextoPago {
    private String tipoPago;          // "PAGO_MANUAL" | "PAGO_APORTES" | "PRECANCELACION" | "ABONO_CAPITAL"
    private String usuario;           // obligatorio
    private String observacion;
    private LocalDateTime fechaPago;  // si null → LocalDateTime.now()
    private Long idEvento;            // EVPRCDGO de la operación (siempre presente en los procesos nuevos)
}

public class SaldosCuota {            // TODOS los componentes, en el orden de prelación
    private double saldoDesgravamen;
    private double saldoMora;
    private double saldoInteresVencido;
    private double saldoInteres;
    private double saldoCapital;
    private double saldoSeguroIncendio;
    private double totalPendiente;    // suma de los 6
}

public class DetalleAplicacionCuota {
    private Long idCuota; private Double numeroCuota;
    private Long estadoAnterior, estadoNuevo;      // rubro EstadoCuotaPrestamo
    private double aplicadoDesgravamen, aplicadoMora, aplicadoInteresVencido,
                   aplicadoInteres, aplicadoCapital, aplicadoSeguro, totalAplicado;
    private Long idPagoPrestamo;                   // PGPR creado para esta cuota
}

public class ResultadoAplicacionPago {
    private Long idPrestamo; private Long idEvento;
    private double valorRecibido, valorAplicado, excedenteNoAplicado;
    private boolean prestamoCancelado; private Long estadoFinalPrestamo;
    private List<DetalleAplicacionCuota> cuotasAfectadas;
}

public class DesgloseAporte { private Long idTipoAporte; private Double valor; }
public class MovimientoAporte { private Long idAporte; private Long idTipoAporte;
                                private Double valor; /* negativo */ private Long idPagoAporte; }

// Solicitudes (bodies REST):
public class SolicitudPagoCuota      { Long idPrestamo; Double valor; String usuario, observacion; LocalDate fechaPago; }
public class SolicitudPagoConAportes { Long idPrestamo; List<DesgloseAporte> aportes; String usuario, observacion; LocalDate fechaPago; }
public class SolicitudAbonoCapital   { Long idPrestamo; Double valor; Integer modalidad; String usuario, observacion; LocalDate fecha; }
public class SolicitudPrecancelacion { Long idPrestamo; Double valorEfectivo; List<DesgloseAporte> aportes;
                                       String usuario, observacion; LocalDate fecha; }
public class SolicitudAnulacion      { Long idEvento; String usuario; String motivo; }
```

### 5.2 Métodos DAO nuevos

`DetallePrestamoDaoService` (+Impl; seguir el estilo: absorber excepciones devolviendo
vacío/null, JavaDoc en la interfaz):
```java
/** Cuotas pendientes (estado IS NULL OR estado NOT IN (4,7)) ordenadas por numeroCuota ASC. */
List<DetallePrestamo> selectCuotasPendientesByPrestamoOrdenadas(Long codigoPrestamo);
/** Cuotas del préstamo con numeroCuota > :desde, cualquier estado, orden ASC (re-amortización). */
List<DetallePrestamo> selectCuotasByPrestamoDesdeNumero(Long codigoPrestamo, Double numeroCuotaExclusivo);
/** Cuota PAGADA (estado=4) con mayor numeroCuota, o null. */
DetallePrestamo selectUltimaCuotaPagada(Long codigoPrestamo);
/** Cuotas pendientes con fechaVencimiento <= :fecha (deuda exigible), orden numeroCuota ASC. */
List<DetallePrestamo> selectCuotasExigibles(Long codigoPrestamo, LocalDateTime fechaCorte);
```

`AporteDaoService`:
```java
/** {codigoTipoAporte, nombreTipoAporte, SUM(valor)} de la entidad, solo TipoAporte.estado=1,
 *  GROUP BY tipo. Una sola query agregada (nunca traer filas). */
List<Object[]> sumValorPorTipoAporteByEntidad(Long codigoEntidad);
/** SUM(valor) de la entidad para un tipo (0.0 si no hay filas). */
Double sumValorByEntidadYTipo(Long codigoEntidad, Long codigoTipoAporte);
```

`PagoPrestamoDaoService`:
```java
/** Pagos VIGENTES (anulado IS NULL OR anulado=0) de una cuota. REEMPLAZA el uso directo de
 *  selectByIdDetallePrestamo en el motor nuevo. */
List<PagoPrestamo> selectVigentesByIdDetallePrestamo(Long codigoDetallePrestamo);
/** Pagos de un evento (para anulación/consulta). */
List<PagoPrestamo> selectByEvento(Long codigoEvento);
```

`PagoAporteDaoService`: `List<PagoAporte> selectByPagoPrestamo(Long codigoPagoPrestamo);`
`EventoPrestamoDaoService`: CRUD + `List<EventoPrestamo> selectByPrestamo(Long)`.
`HistDetallePrestamoDaoService`: `save` + `List<HistDetallePrestamo> selectByEvento(Long)` +
`selectByPrestamo(Long)`.

---

## 6. El motor de pagos — `MotorPagoPrestamoService`

Extracción **desacoplada** (código nuevo, copiado y adaptado; NO tocar
`CargaArchivoPetroServiceImpl`) de: `calcularSaldosRealesCuota`, `procesarPagoCuota`,
`procesarExcedenteASiguienteCuota`, `verificarYActualizarEstadoPrestamo`, `crearRegistroPago`.

```java
@Local
public interface MotorPagoPrestamoService {
    SaldosCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable;
    double calcularTotalPendientePrestamo(Long idPrestamo) throws Throwable;
    ResultadoAplicacionPago aplicarPago(Long idPrestamo, double valor, ContextoPago ctx) throws Throwable;
    DetalleAplicacionCuota aplicarPagoACuota(DetallePrestamo cuota, double valor, ContextoPago ctx) throws Throwable;
    boolean verificarYActualizarEstadoPrestamo(Prestamo prestamo) throws Throwable;
    void recalcularCuotaDesdePagos(DetallePrestamo cuota) throws Throwable;   // para el reverso
}
```

### 6.1 Constantes y principios

- `TOLERANCIA = 0.01` (un centavo) para TODA comparación de aplicación. (La tolerancia de $1 es
  exclusiva de la validación del proceso Petro; aquí no aplica.)
- Aritmética: `BigDecimal` con `RoundingMode.HALF_UP` a 2 decimales en los cálculos; `Double` solo
  al setear entidades. Helper `redondear(double)` equivalente al existente.
- **Estados espejo**: toda escritura hace `cuota.setEstado(x)` **y** `cuota.setIdEstado(x)`.
- **PGPR es la fuente de verdad**: nunca confiar en los `*Pagado` persistidos de la cuota para
  decidir; siempre reconstruir con `selectVigentesByIdDetallePrestamo` (excluye anulados).
- Persistir cuotas vía `detallePrestamoService.saveSingle(...)` (mantiene el espejo) o DAO +
  espejo manual — elegir UNA vía y usarla consistentemente (recomendado: `saveSingle`).
- `PagoPrestamo` nuevo SIEMPRE con `setIdEstado(1L)` (PGPRIDST es NOT NULL) y `setEstado(1L)`.
- Rubros `int` → castear `(long)` al asignar/comparar con campos `Long`.

### 6.2 `calcularSaldosRealesCuota` (extendido con mora e interés vencido)

```
pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.codigo)
saldoDesgravamen     = max(0, nvl(cuota.desgravamen)        − Σ nvl(pago.desgravamen))
saldoMora            = max(0, nvl(cuota.mora)               − Σ nvl(pago.moraPagada))
saldoInteresVencido  = max(0, nvl(cuota.interesVencido)     − Σ nvl(pago.interesVencidoPagado))
saldoInteres         = max(0, nvl(cuota.interes)            − Σ nvl(pago.interesPagado))
saldoCapital         = max(0, nvl(cuota.capital)            − Σ nvl(pago.capitalPagado))
saldoSeguroIncendio  = max(0, nvl(cuota.valorSeguroIncendio) − Σ nvl(pago.valorSeguroIncendio))
totalPendiente       = Σ de los 6
```
- Si la lista de pagos está vacía: los saldos son los valores originales de la cuota y
  `totalPendiente = nvl(cuota.total) + nvl(cuota.mora) + nvl(cuota.interesVencido)`; si
  `cuota.total` es null (dato legacy), fallback: suma de los 6 componentes originales.
  (Nota: DTPRTTLL no incluye mora/IV porque nacen en 0 y los escribe el proceso nocturno después.)
- **Autocorrección**: si `totalPendiente <= 0.01` y la cuota no está PAGADA(4) ni
  CANCELADA_ANTICIPADA(7): pasarla a PAGADA (ambos estados), sincronizar los `*Pagado` con las
  sumas, `saldoCapital = max(0, saldoInicialCapital − capitalPagado)`, `saldoInteres = 0`,
  `saldoMora = 0`, `saldoInteresVencido = 0`, recalcular `saldo` global, **respetar `fechaPagado`
  existente** (solo setear si es null), y persistir.

### 6.3 `aplicarPago` (cascada) y prelación

```
1. prestamo = selectById; validar no-terminal (3/4/5) → IncomeException si terminal.
2. iteraciones = 0; mientras valorRestante > 0.01 e iteraciones < 100:
     cuota = mínima cuota pendiente (selectCuotasPendientesByPrestamoOrdenadas → primera con
             saldos reales.totalPendiente > 0.01; las de saldo insignificante se van
             autocorrigiendo a PAGADA por calcularSaldosRealesCuota)
     si no hay cuota → romper (excedenteNoAplicado = valorRestante)
     detalle = aplicarPagoACuota(cuota, valorRestante, ctx)   // aplica min(valorRestante, pendiente)
     valorRestante −= detalle.totalAplicado
3. verificarYActualizarEstadoPrestamo(prestamo)
4. armar ResultadoAplicacionPago
```

`aplicarPagoACuota(cuota, valorDisponible, ctx)`:
```
saldos = calcularSaldosRealesCuota(cuota); si totalPendiente <= 0.01 → retornar sin aplicar (0)
montoAplicar = min(valorDisponible, saldos.totalPendiente)

PRELACIÓN (imputación secuencial, cada componente toma min(restante, saldoComponente)):
   1. Desgravamen  2. Mora  3. Interés vencido  4. Interés  5. Capital  6. Seguro de incendio
(la deuda vieja —mora e interés vencido— se cobra antes que el interés corriente;
 parametrizable vía CRD.OAVP en fase futura, hardcodeada por ahora)

Acumular en la cuota (nunca reemplazar):
   capitalPagado += capitalAplicado; interesPagado += interesAplicado;
   desgravamenPagado += desgravamenAplicado; moraPagado += moraAplicado;
   interesVendidoPagado += ivAplicado   // (sic: el campo Java se llama interesVendidoPagado)
Recalcular saldos persistidos:
   saldoCapital = max(0, saldoInicialCapital − capitalPagado)
   saldoInteres = max(0, interes − interesPagado); saldoMora = max(0, mora − moraPagado)
   saldoInteresVencido = max(0, interesVencido − interesVendidoPagado)
   saldo = max(0, totalConMoraIV − totalPagadoAcumulado)
Estado:
   si |montoAplicar − saldos.totalPendiente| <= 0.01 → PAGADA(4) + fechaPagado = ctx.fechaPago
   si no → PARCIAL(6)
Persistir cuota (saveSingle) y crear PagoPrestamo:
   prestamo, detallePrestamo, numeroCuota, fecha = ctx.fechaPago, valor = montoAplicar,
   capitalPagado/interesPagado/moraPagada/interesVencidoPagado/desgravamen/valorSeguroIncendio =
   lo aplicado por componente, saldoOtros = 0, tipo = ctx.tipoPago,
   observacion = ctx.observacion + " [Evento: " + ctx.idEvento + "]",
   usuarioRegistro = ctx.usuario, fechaRegistro = now, estado = 1L, idEstado = 1L,
   eventoPrestamo = ctx.idEvento, anulado = 0
```

### 6.4 `verificarYActualizarEstadoPrestamo`

Idéntico al vigente de petro (copiar la lógica):
- Estado se lee/escribe en `idEstado` (PRSTIDST); **nunca** tocar `ESPSCDGO`.
- Salir si terminal (3/4/5). Exigir `contarCuotasByPrestamo > 0`.
- Cancelar (**CANCELADO = 3**) solo si `contarCuotasPendientesByPrestamo == 0`
  (pendiente = `estado IS NULL OR estado NOT IN (4,7)`).
- Escribir `fechaModificacion = now`; **NUNCA** tocar `fechaFin`.
- Capturar `Throwable` internamente: un fallo aquí no aborta el pago.

### 6.5 `recalcularCuotaDesdePagos` (para el reverso)

Reconstruye la cuota íntegramente desde los pagos VIGENTES:
```
sumas = Σ por componente de selectVigentesByIdDetallePrestamo
*Pagado = sumas; saldos = max(0, original − pagado) por componente;
saldo global recalculado; saldoCapital = max(0, saldoInicialCapital − capitalPagado)
Estado: totalPendiente <= 0.01 → PAGADA (conservar fechaPagado si existe)
        totalPagado > 0.01     → PARCIAL, fechaPagado = null
        totalPagado <= 0.01    → estado base: EN_MORA(5) si fechaVencimiento < hoy, si no PENDIENTE(1);
                                 fechaPagado = null
Ambos campos de estado, persistir.
```

---

## 7. Procesos de negocio

Todos los procesos: `@TransactionAttribute(REQUIRED)` (una sola transacción; cualquier
`IncomeException`/`Throwable` revierte todo), crean su `EventoPrestamo` al inicio (para tener
`EVPRCDGO`), llaman al hook contable al final (§9), y dejan huella en el préstamo:
`prestamo.observacion` += `"\n[<fecha> <usuario>] <TIPO> $<valor> - <obs>"` (truncar por la
IZQUIERDA si supera 2000 caracteres, conservando lo más reciente) + `fechaModificacion = now`.

### 7.1 Pago de cuota — `pagarCuota(SolicitudPagoCuota)`

Validaciones en orden (cada una con su `etapa`):
1. `idPrestamo` existe → si no, error 404 ("PRESTAMO_NO_ENCONTRADO").
2. Estado no terminal (idEstado ∉ {3,4,5}) → 409.
3. `valor > 0` (tras redondear a 2 dec) → 422.
4. `fechaPago` no futura (si null → hoy) → 422.
5. `valor <= calcularTotalPendientePrestamo(id) + 0.01` → si excede, 422 con mensaje
   `"El valor $X excede la deuda total $Y del préstamo; use la precancelación"`.
   (Prohibimos excedentes huérfanos a priori.)

Ejecución: crear EVPR (tipo PAGO_MANUAL, valor, usuario, obs, fecha) →
`motor.aplicarPago(id, valor, ctx)` → huella → hook → respuesta con `ResultadoAplicacionPago`.

Casos borde:
- Valor == deuda total exacta → todas las cuotas PAGADAs → préstamo CANCELADO(3) en el mismo
  llamado (permitido: pagar la deuda completa NO es precancelación, no condona nada).
- Préstamo inconsistente (estado 2 pero sin cuotas con saldo): `aplicarPago` no encuentra cuota →
  422 "sin cuotas pendientes" y el motor ya intentó la autocorrección de estados.
- Cuota legacy sin `total`: cubierto por el fallback de §6.2.

### 7.2 Saldo de aportes — `SaldoAporteService`

```java
@Local
public interface SaldoAporteService {
    /** Saldos netos por tipo de aporte vigente (TipoAporte.estado=1) de una entidad.
     *  saldo = SUM(APRTVLRR) — los pagos son filas negativas, la suma neta ES el saldo. */
    List<SaldoTipoAporte> saldosPorEntidad(Long idEntidad) throws Throwable;
    double saldoPorEntidadYTipo(Long idEntidad, Long idTipoAporte) throws Throwable;
}
// SaldoTipoAporte { Long idTipoAporte; String nombre; Double saldo; }
```

**Justificación (análisis del punto 3 del pedido)**: se adopta el modelo "campo `valor` con
movimientos negativos" y se descarta llevar el saldo en `valorPagado`/`saldo` por registro:
- TODOS los consumidores existentes (G42, G43, G44, CJBM, CPRM→CCPM, dashboard de aportes,
  padrón de partícipes, resúmenes por estado) calculan con `SUM(APRTVLRR)` neto, y G43 liquida
  cesantes leyendo explícitamente los negativos del mes. Cambiar de modelo obligaría a reescribir
  todos esos reportes.
- `valorPagado`/`saldo` tienen OTRA semántica: son la mecánica del FIFO del proceso Petro
  (deuda de aportes del partícipe), no el saldo disponible. Se dejan intactos.
- El problema real es DÓNDE se calcula: el frontend baja 980k filas. La solución es este servicio
  agregado + los índices de §4.3. El frontend debe migrar a `GET /aprt/saldosPorEntidad/{id}` y
  **dejar de usar `GET /aprt/getAll`** para estados de cuenta (documentarlo como deprecado para
  ese uso).

### 7.3 Abono a capital — `AbonoCapitalPrestamoService`

```java
@Local
public interface AbonoCapitalPrestamoService {
    SimulacionAbonoCapital simular(Long idPrestamo, double valor, int modalidad) throws Throwable;
    ResultadoAbonoCapital aplicar(SolicitudAbonoCapital solicitud) throws Throwable;
}
// SimulacionAbonoCapital { saldoCapitalActual, valorAbono, modalidad, plazoActual, plazoNuevo,
//   cuotaActual, cuotaNueva, ahorroIntereses, List<CuotaProyectada> tablaProyectada }
// CuotaProyectada { numeroCuota, fechaVencimiento, capital, interes, cuota, saldoCapital }
// ResultadoAbonoCapital { idEvento, idPagoPrestamo, plazoAnterior, plazoNuevo, cuotaAnterior,
//   cuotaNueva, cuotasHistorizadas, cuotasGeneradas }
```

Modalidades: `1` = mantener valor de cuota / **reducir plazo**; `2` = mantener plazo / **reducir
valor de cuota**.

Validaciones:
1. Préstamo existe / no terminal.
2. `modalidad ∈ {1,2}`; `valor > 0`.
3. **Préstamo al día**: no deben existir cuotas pendientes con `fechaVencimiento < fecha` ni
   cuotas PARCIAL(6) → 422 `"El préstamo tiene cuotas vencidas o parciales; regularícelas antes
   de abonar a capital"`. (Evita re-amortizar sobre base sucia y garantiza que las cuotas a
   historizar no tengan `PagoPrestamo` asociados.)
4. `saldoCapitalPendiente` = Σ `calcularSaldosRealesCuota(c).saldoCapital` de las cuotas
   pendientes. `valor < saldoCapitalPendiente − 0.01`, si no → 422 `"El abono cubre todo el
   capital; use la precancelación"`.

Algoritmo de `aplicar` (una transacción):
```
1. Crear EVPR (tipo ABONO_CAPITAL, valor, modalidad, plazoAnterior = prestamo.plazo,
   cuotaAnterior = valorCuota vigente, fecha, usuario, obs).
2. Cuota ANCLA = selectUltimaCuotaPagada(id). Si existe: ancla.saldoOtros += valor (ACUMULATIVO,
   nunca pisar — precedente ACTUALIZACION-SALDOOTROS) y persistir.
   Si NO existe (préstamo sin cuotas pagadas): el abono se registrará en saldoOtros de la PRIMERA
   cuota nueva generada (paso 6).
3. Crear PagoPrestamo del abono: prestamo, detallePrestamo = ancla (o primera nueva, se setea en
   el paso 6), fecha, valor = montoAbono, saldoOtros = montoAbono,
   capitalPagado/interes/mora/IV/desgravamen/seguro = 0   ← IMPORTANTE: el abono va en saldoOtros,
   NO en capitalPagado, para no contaminar la reconstrucción de saldos por cuota (§6.2),
   tipo = "ABONO_CAPITAL", evento, usuario, estado/idEstado = 1.
4. cuotasFuturas = selectCuotasPendientesByPrestamoOrdenadas(id)  (por la validación 3, ninguna
   está vencida ni parcial y ninguna tiene PGPR). Copiarlas TODAS a HDTP (motivo ABONO_CAPITAL,
   evento) y borrarlas de DTPR (remove) — misma transacción.
5. nuevoCapital = saldoCapitalPendiente − valor.
6. RE-AMORTIZAR respetando prestamo.tipoAmortizacion (1 = francesa, 2 = alemana):
   - i = prestamo.tasa / 100 / 12  (PRSTTSAA anual %)
   - numeración: continúa desde (última cuota que QUEDA en DTPR).numeroCuota + 1
   - primer vencimiento: último día del mes SIGUIENTE al mes de la primera cuota historizada
     (equivale a conservar el calendario original de vencimientos)
   - MODALIDAD 1 (cuota fija C = valor de cuota vigente, reducir n):
       francesa: n = ceil( −ln(1 − nuevoCapital·i/C) / ln(1+i) )
                 si nuevoCapital·i >= C → 422 "la cuota vigente no cubre el interés"
       alemana:  capitalPorCuota = el capital fijo vigente; n = ceil(nuevoCapital / capitalPorCuota)
       actualizar prestamo.plazo = (cuotas no historizadas existentes) + n
   - MODALIDAD 2 (n = cantidad de cuotas historizadas, reducir C):
       francesa: C = nuevoCapital · i / (1 − (1+i)^−n)
       alemana:  capitalPorCuota = nuevoCapital / n; cuota_k = capitalPorCuota + saldo_k · i
       prestamo.plazo no cambia
   - Cada cuota nueva llena TODOS los campos (invariante Excel):
       saldoInicialCapital = capital pendiente antes de la cuota; cuota = capital + interés;
       desgravamen y valorSeguroIncendio = COPIA de los de la última cuota historizada (los montos
       por cuota son fijos en la práctica) ⚠ SUPUESTO A CONFIRMAR CON NEGOCIO — si no se confirma,
       dejarlos en 0.0; total = cuota + desgravamen + valorSeguroIncendio;
       estado = idEstado = 1 (PENDIENTE); mora/interesVencido/…Pagado/abono/saldoOtros = 0
       (la PRIMERA nueva lleva saldoOtros = montoAbono si no hubo ancla, ver paso 2);
       saldo = total; saldoCapital = saldoInicialCapital − capital; redondear todo HALF_UP 2 dec;
       la ÚLTIMA cuota absorbe el residuo (su capital = saldo remanente exacto).
7. actualizarCamposPrestamo(...) (reutilizar el método existente: valorCuota, fechaFin, totales,
   tasas) + EVPR con plazoNuevo/cuotaNueva + huella en observacion + fechaModificacion.
8. Hook contabilizarAbonoCapital(evento).
```

`simular` ejecuta los mismos cálculos SIN escribir nada y devuelve la tabla proyectada.

### 7.4 Pago con aportes — `pagarConAportes(SolicitudPagoConAportes)`

Validaciones: las 1-4 de §7.1 con `valorTotal = Σ desglose`; además:
- Desglose no vacío; cada `valor > 0`; sin tipos repetidos (→ 422 "desglose con tipo duplicado").
- Cada `idTipoAporte` con `TipoAporte.estado = 1` → 422 si no vigente.
- Por cada tipo: `saldoPorEntidadYTipo(entidadDelPrestamo, tipo) >= valor − 0.01` → 422
  detallando tipo y disponible. La entidad es `prestamo.getEntidad()`.

Secuencia (una transacción):
```
1. Crear EVPR (tipo PAGO_APORTES, valorTotal, ...).
2. resultado = motor.aplicarPago(idPrestamo, valorTotal, ctx)
3. Por cada elemento del desglose:
   a. GUARDARRAÍL anti-carrera: revalidar sumValorByEntidadYTipo(entidad, tipo) >= valor − 0.01
      dentro de la transacción; si falla → IncomeException (rollback total).
   b. Crear la FILA NEGATIVA en APRT:
        entidad, filial = entidad.getFilial(), tipoAporte,
        valor = −montoUsado                      ← negativo
        valorPagado = 0.0, saldo = 0.0           ← CRÍTICO: con saldo=0 la fila es invisible para
                                                    el FIFO petro (selectMinAporteConSaldo filtra
                                                    saldo > 0.01); nunca dejar null
        estado (APRTIDST) = 4 (PAGADA)           ← fuera de los estados que consume el FIFO
        idAsoprep = null, fechaTransaccion = fechaPago,
        glosa = "PAGO PRESTAMO " + idPrestamo + " - Evento " + idEvento,
        usuarioRegistro = usuario, fechaRegistro = now
   c. Crear PagoAporte (PGAP):
        aporte = la fila negativa recién creada, filial,
        valor = +montoUsado (positivo: magnitud del pago),
        fechaContable = fechaPago, numeroAsiento = null,
        concepto = misma glosa, usuario, estado = 1,
        pagoPrestamo = primer PGPR del resultado (los demás se recuperan por el evento)
4. Huella + hook contabilizarPagoConAportes(resultado, movimientos, ctx).
```

Respuesta: `ResultadoAplicacionPago` + `movimientosAporte: List<MovimientoAporte>`.

**Por qué así** (resumen del análisis): los reportes (G42/G43/G44/CJBM/CPRM/CCPM/dashboard/padrón)
suman `APRTVLRR` neto y esperan pagos como filas negativas; los campos `valorPagado`/`saldo` de
las filas POSITIVAS no se tocan porque pertenecen al FIFO petro (otra semántica). El saldo
disponible es y seguirá siendo `SUM(valor)`.

### 7.5 Precancelación — `simularPrecancelacion` + `precancelar`

**Cálculo canónico** (método privado compartido por la simulación y la validación del POST):
```
fecha = parámetro (default hoy)
exigibles      = selectCuotasExigibles(id, finDelDia(fecha))       // pendientes con venc <= fecha
valorExigible  = Σ calcularSaldosRealesCuota(c).totalPendiente     // incluye mora/IV si las hay
futuras        = cuotas pendientes con fechaVencimiento > fecha
capitalFuturo  = Σ calcularSaldosRealesCuota(c).saldoCapital de las futuras
valorTotal     = redondear(valorExigible + capitalFuturo)
interesCondonado = Σ (totalPendiente − saldoCapital) de las futuras   // informativo
```

`simularPrecancelacion(id, fecha)` devuelve: exigibles (lista con número, vencimiento y
pendiente), capitalFuturo, valorTotal, cuotasAAnular, interesCondonado.

`precancelar(SolicitudPrecancelacion)`:

Validaciones: préstamo existe / no terminal; `futuras` no vacía → si está vacía, 422
`"No hay cuotas futuras que precancelar; use pagarCuota"`; desglose de aportes con las reglas de
§7.4 (si viene); `valorEnviado = valorEfectivo + Σ aportes`;
`|valorEnviado − valorTotal| <= 0.01` → si no, 422 devolviendo `valorTotal` correcto en la
respuesta (el frontend simula primero; el backend siempre re-verifica).

Secuencia (una transacción):
```
1. Crear EVPR (tipo PRECANCELACION, valorTotal, fecha, usuario, obs).
2. Pagar la deuda exigible: por cada cuota exigible en orden,
   motor.aplicarPagoACuota(cuota, totalPendiente, ctx)  → quedan PAGADAs con su PGPR.
   (Sin cascada: se paga exactamente el pendiente de cada una.)
3. Cuotas futuras → estado = idEstado = 7 (CANCELADA_ANTICIPADA), fechaPagado = null.
   NO se borran NI se historizan: quedan en DTPR como constancia (el estado 7 está excluido de
   contarCuotasPendientesByPrestamo). Coherente con el precedente documentado.
4. capitalFuturo → DTPRSLOT: ultimaPagada = selectUltimaCuotaPagada(id) (tras el paso 2 será la
   última exigible pagada; si no hubo exigibles, la última pagada histórica).
   ultimaPagada.saldoOtros += capitalFuturo (ACUMULATIVO). Persistir.
   ⚠ Nota: esto rompe deliberadamente la invariante saldoInicialCapital = capital + saldoCapital
   + saldoOtros en esa fila — ya documentado en ACTUALIZACION-SALDOOTROS-CANCELADOS-ANTICIPADOS.md
   §8; cualquier recálculo futuro debe excluir préstamos en estado 4.
   Si NO existe ninguna cuota pagada (caso extremo: precancelar un préstamo recién generado sin
   pagos): registrar el capital en la PRIMERA cuota futura antes de marcarla en 7.
5. Crear PagoPrestamo de la precancelación sobre ultimaPagada:
   valor = capitalFuturo, saldoOtros = capitalFuturo, resto de componentes = 0
   (mismo criterio que el abono: NO usar capitalPagado), tipo = "PRECANCELACION", evento.
6. Componente aportes (si existe): filas negativas + PGAP exactamente como §7.4 pasos 3a-3c.
   El monto en efectivo no genera registro adicional (queda implícito: valorTotal − aportes).
7. Préstamo: idEstado = 4 (CANCELADO_ANTICIPADO — NUNCA tocar ESPSCDGO), fechaModificacion = now,
   NUNCA fechaFin. Huella en observacion.
8. Hook contabilizarPrecancelacion(evento).
```

### 7.6 Reverso — `anularOperacion(SolicitudAnulacion)`

Anula un `EventoPrestamo` completo (los 4 tipos). Validaciones comunes: evento existe y
`estado = 1` → 404/409; **no deben existir eventos vigentes POSTERIORES sobre el mismo préstamo**
(`EVPRCDGO` mayor con `EVPRESTD = 1`) → 409 `"Anule primero las operaciones posteriores"`
(garantiza reversos en orden LIFO y estados consistentes).

Por tipo:

**PAGO_MANUAL / PAGO_APORTES**:
```
1. pagos = selectByEvento(idEvento); marcar cada uno anulado = 1 + usuario/fecha/motivo.
2. Por cada cuota afectada (distintas): motor.recalcularCuotaDesdePagos(cuota).
3. Si el préstamo está CANCELADO(3) y ahora hay cuotas pendientes → reabrir a VIGENTE(2)
   (NUNCA reabrir automáticamente 4 ni 5).
4. Si el evento es PAGO_APORTES: por cada PGAP del evento (selectByPagoPrestamo de los PGPR
   anulados): crear CONTRA-MOVIMIENTO POSITIVO en APRT (valor = +monto, mismos tipo/entidad,
   saldo = 0, valorPagado = 0, estado = 4, glosa = "REVERSO EVENTO " + idEvento) y marcar el
   PGAP con estado = 0. (Se inserta contra-movimiento en lugar de borrar: la tabla APRT es
   append-only para los reportes.)
5. EVPRESTD = 0 + auditoría de anulación; huella en prestamo.observacion; hook contabilizarReverso.
```

**ABONO_CAPITAL**:
```
1-3. Igual que arriba (el PGPR del abono se anula; no hay componentes en cuotas).
4. Borrar de DTPR las cuotas GENERADAS por el evento (son las cuotas pendientes actuales con
   numeroCuota > MAX(numeroCuota) de HDTP del evento... — implementación robusta: las cuotas de
   DTPR del préstamo con numeroCuota >= MIN(HDTP.DTPRNMCT del evento)); validar que NINGUNA tenga
   PGPR vigente (si tiene → 409 "hay pagos sobre la tabla recalculada; anúlelos primero").
5. Restaurar desde HDTP: re-insertar en DTPR cada fila del evento con sus valores originales
   (nuevo DTPRCDGO — el original se guarda como referencia; documentar que el código de cuota
   cambia al restaurar). Borrar (o conservar — conservar: son historia) las filas HDTP; se
   conservan y el evento queda anulado.
6. Revertir ancla.saldoOtros −= valorAbono; restaurar prestamo.plazo/valorCuota/fechaFin/totales
   con actualizarCamposPrestamo sobre la tabla restaurada.
```

**PRECANCELACION**:
```
1-3. Anular los PGPR del evento (exigibles + capitalFuturo) y recalcular esas cuotas desde pagos.
4. Cuotas en estado 7 del préstamo → recalcularCuotaDesdePagos (vuelven a PENDIENTE/EN_MORA
   según vencimiento).
5. ultimaPagada.saldoOtros −= capitalFuturo.
6. Préstamo: idEstado = 4 → VIGENTE(2) (o EN_MORA si recalcularCuotaDesdePagos dejó cuotas en
   mora — recomendación simple: VIGENTE(2) y dejar que el flujo normal lo lleve a mora).
7. Contra-movimientos de aportes si los hubo. EVPRESTD = 0, huella, hook.
```

### 7.7 Punto 5 del pedido — tipos de pago analizados

Cubiertos: pago de cuota, abono a capital (2 modalidades), pago con aportes, precancelación
(valor/aportes/mixto), reverso. Identificados y dejados FUERA de alcance (futuros): condonación
de deuda, novación (existe `CANCELADO_POR_NOVACION = 5` sin proceso), cobro de mora nocturno
(alimentará `DTPRMRAA`), pago mixto efectivo+aportes de cuotas normales (se resuelve con dos
llamadas consecutivas `pagarConAportes` + `pagarCuota`; solo la precancelación exige mixto
atómico porque valida el total).

---

## 8. Contratos REST

Endpoints nuevos en `PrestamoRest` (`@Path("prst")`) y `AporteRest` (`@Path("aprt")`).
El REST **solo valida parámetros y delega** (patrón GeneracionArchivoPetroRest). Montos SIEMPRE
en el body JSON, nunca en el path. Formato de respuesta y mapeo de errores:

```json
{ "exito": true|false, "etapa": "VALIDACION|APLICACION|...", "mensaje": "...",
  "error": "solo en fallos", "resultado": { } }
```
| Condición | HTTP |
|---|---|
| OK | 200 (201 en creaciones) |
| Parámetro faltante/malformado | 400 |
| Préstamo/evento no encontrado | 404 |
| Estado no permite la operación (terminal, evento posterior, ya anulado) | 409 |
| Regla de negocio (valor excede, saldo insuficiente, no al día, monto no coincide) | 422 |
| Error inesperado | 500 |
Mapear por tipo de excepción/contenido de mensaje como hace `GeneracionArchivoPetroRest`.

```
POST /rest/prst/pagarCuota
  { "idPrestamo": 123, "valor": 250.00, "usuario": "jperez",
    "observacion": "Pago ventanilla", "fechaPago": "2026-08-13" }
  → 200 { exito, mensaje, resultado: ResultadoAplicacionPago }     (ver DTO §5.1)

GET  /rest/prst/simularAbonoCapital/{idPrestamo}?valor=5000&modalidad=1
  → 200 { exito, resultado: SimulacionAbonoCapital }               (incluye tablaProyectada)

POST /rest/prst/abonarCapital
  { "idPrestamo": 123, "valor": 5000.00, "modalidad": 1, "usuario": "jperez",
    "observacion": "Abono extraordinario", "fecha": "2026-08-13" }
  → 201 { exito, mensaje, resultado: ResultadoAbonoCapital }

GET  /rest/aprt/saldosPorEntidad/{idEntidad}
  → 200 { "exito": true, "resultado": [
        { "idTipoAporte": 9,  "nombre": "APORTE JUBILACION", "saldo": 12345.67 },
        { "idTipoAporte": 11, "nombre": "APORTE CESANTIA",   "saldo":  8100.00 } ] }
  (lista vacía es 200 con [], NO error — no replicar el patrón IncomeException-si-vacío)

POST /rest/prst/pagarConAportes
  { "idPrestamo": 123, "usuario": "jperez", "observacion": "Pago con cesantía",
    "fechaPago": "2026-08-13",
    "aportes": [ { "idTipoAporte": 11, "valor": 300.00 }, { "idTipoAporte": 9, "valor": 150.00 } ] }
  → 200 { exito, mensaje, resultado: ResultadoAplicacionPago, movimientosAporte: [...] }

GET  /rest/prst/simularPrecancelacion/{idPrestamo}?fecha=2026-08-13
  → 200 { exito, resultado: { fecha, exigibles: [{numeroCuota, fechaVencimiento, pendiente}],
          valorExigible, capitalFuturo, valorTotalPrecancelacion, cuotasAAnular, interesCondonado } }

POST /rest/prst/precancelar
  { "idPrestamo": 123, "valorEfectivo": 1000.00,
    "aportes": [ { "idTipoAporte": 11, "valor": 3350.00 } ],
    "usuario": "jperez", "observacion": "Precancelación por retiro", "fecha": "2026-08-13" }
  → 200 { exito, mensaje, resultado: { idEvento, valorExigiblePagado, capitalPrecancelado,
          cuotasCanceladasAnticipadas, estadoFinalPrestamo: 4, idCuotaConSaldoOtros,
          movimientosAporte: [...] } }
  → 422 si el monto no coincide: { exito:false, etapa:"VALIDACION",
          mensaje:"El valor enviado $4300.00 no coincide con el valor de precancelación $4350.00",
          "valorTotalPrecancelacion": 4350.00 }

POST /rest/prst/anularOperacion
  { "idEvento": 55, "usuario": "jperez", "motivo": "Pago aplicado a préstamo equivocado" }
  → 200 { exito, mensaje, resultado: { idEvento, tipoOperacion, pagosAnulados,
          cuotasRecalculadas, estadoFinalPrestamo } }

GET  /rest/evpr/getAll | /rest/evpr/getId/{id} | POST /rest/evpr/selectByCriteria   (solo lectura)
```

---

## 9. Hooks de contabilidad

### 9.1 Interfaz y no-op

```java
@Local
public interface ContabilidadPrestamoService {
    /** Devuelven el código de asiento creado, o null si la contabilidad no está activa. */
    Long contabilizarPagoCuota(ResultadoAplicacionPago r, ContextoPago ctx) throws Throwable;
    Long contabilizarPagoConAportes(ResultadoAplicacionPago r, List<MovimientoAporte> movs, ContextoPago ctx) throws Throwable;
    Long contabilizarAbonoCapital(EventoPrestamo evento) throws Throwable;
    Long contabilizarPrecancelacion(EventoPrestamo evento) throws Throwable;
    Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable;
}

@Stateless
public class ContabilidadPrestamoNoOpImpl implements ContabilidadPrestamoService {
    // Todos los métodos: System.out.println("Contabilidad de préstamos no activa - hook no-op"); return null;
}
```

### 9.2 Uso desde los orquestadores

Al FINAL de cada proceso, **en la misma transacción REQUIRED**:
```java
Long asiento = contabilidadPrestamoService.contabilizarXxx(...);
if (asiento != null) { evento.setNumeroAsiento(asiento); /* y PGPRASNT en los pagos, PGAPNMAS en pagos de aporte */ }
```
Con el no-op el costo es cero. Cuando exista la implementación real, un fallo del asiento
(`IncomeException`: período inexistente o MAYORIZADO, debe≠haber) **revierte el pago completo** —
comportamiento correcto para operaciones online (a diferencia del lote petro).

### 9.3 Pre-requisitos contables futuros (NO implementar ahora; dejar listados)

- La impl real usará `com.saa.ejb.cnt.service.AsientoContableService.generarAsiento(idEmpresa,
  codigoAltTipoAsiento, fecha, obs, usuario, List<DetalleAsiento>, moduloSistema)`.
- Falta: constante de módulo crédito en `com.saa.rubros.ModuloSistema`; constantes nuevas en
  `com.saa.rubros.TipoAsientos` (+ filas `TipoAsiento` con `codigoAlterno` y `sistema=1` en BD
  por empresa); resolver `idEmpresa` desde crd (ninguna entidad crd referencia
  `com.saa.model.scp.Empresa`; camino candidato: configuración por `Filial`).

---

## 10. Orden de implementación por fases (con criterios de aceptación)

Cada fase termina con compilación limpia en Eclipse y sus pruebas manuales; los datos de prueba
deben incluir un préstamo con tabla **generada en Java** (post-fix) y uno con tabla **cargada de
Excel**.

- **Fase 0 — Cimientos**: DDL §4; entidades nuevas (`EventoPrestamo`, `HistDetallePrestamo`) y
  campos nuevos en `PagoPrestamo`/`PagoAporte`; DAOs nuevos; fixes §3.1/§3.2; borrado §3.3.
  ✔ Generar una tabla de amortización y verificar por SQL que DTPRSICP/DTPRTTLL vienen llenos y
  cumplen el invariante.
- **Fase 1 — Motor**: `MotorPagoPrestamoService` completo + DTOs.
  ✔ Sin REST aún; probar vía un endpoint temporal o esperar a Fase 2.
- **Fase 2 — Pago manual**: `pagarCuota` + `POST /prst/pagarCuota` + EVPR + hook no-op.
  ✔ Casos: parcial (PARCIAL + desglose correcto en PGPR), exacto (PAGADA + fechaPagado),
  excedente 2 cuotas, pago total (préstamo CANCELADO 3), cuota con mora manual en DTPRMRAA
  (prelación: desgravamen→mora→IV→interés→capital→seguro), rechazos 404/409/422.
- **Fase 3 — Aportes**: `SaldoAporteService` + `GET /aprt/saldosPorEntidad` (entregable
  independiente: desbloquea al frontend del OOM) → después `pagarConAportes`.
  ✔ Saldo por endpoint == suma manual por SQL; pago genera fila negativa invisible al FIFO
  (verificar `selectMinAporteConSaldo` no la devuelve) y PGAP enlazado; reportes de aportes de un
  partícipe de control cambian exactamente en −monto.
- **Fase 4 — Abono a capital**: `simular` → `aplicar`.
  ✔ Modalidad 1 francés y alemán (plazo se reduce, cuota igual), modalidad 2 (cuota se reduce,
  plazo igual), HDTP contiene las cuotas viejas, DTPRSLOT del ancla acumula, invariante en las
  cuotas nuevas, `Prestamo.plazo/valorCuota/fechaFin` actualizados, rechazo si no está al día.
- **Fase 5 — Precancelación**: `simularPrecancelacion` → `precancelar`.
  ✔ Con cuota en curso + vencidas; sin cuota en curso; mixto aportes+efectivo; validación de
  monto exacto (±0.01); estados finales 4/7; DTPRSLOT con capitalFuturo; 422 si no hay futuras.
- **Fase 6 — Reverso**: `anularOperacion` para los 4 tipos.
  ✔ Anular pago manual restaura estados de cuota; anular pago con aportes crea contra-movimiento
  (saldo del tipo vuelve al valor previo); anular abono restaura tabla desde HDTP y plazo;
  anular precancelación reabre el préstamo a VIGENTE; bloqueo LIFO (no anular con eventos
  posteriores vigentes).
- **Futuro (fuera de alcance)**: prelación por OAVP, refactor petro→motor, contabilidad real,
  servicio nocturno de mora, condonación, novación.

---

## 11. Verificación end-to-end

Tras desplegar cada fase (Eclipse + WildFly, contexto `/SaaBE`):

```bash
# Fase 2
curl -X POST http://localhost:8080/SaaBE/rest/prst/pagarCuota -H "Content-Type: application/json" \
  -d '{"idPrestamo":123,"valor":50.00,"usuario":"prueba","observacion":"parcial"}'
# Fase 3
curl http://localhost:8080/SaaBE/rest/aprt/saldosPorEntidad/456
curl -X POST http://localhost:8080/SaaBE/rest/prst/pagarConAportes -H "Content-Type: application/json" \
  -d '{"idPrestamo":123,"usuario":"prueba","aportes":[{"idTipoAporte":11,"valor":100.00}]}'
# Fase 4/5
curl "http://localhost:8080/SaaBE/rest/prst/simularAbonoCapital/123?valor=1000&modalidad=1"
curl "http://localhost:8080/SaaBE/rest/prst/simularPrecancelacion/123?fecha=2026-08-31"
```

SELECTs de control (estilo de la casa — correr antes/después de cada operación):

```sql
-- Estado de la tabla de amortización del préstamo
SELECT DTPRNMCT, DTPRESTD, DTPRIDST, DTPRCPTL, DTPRINTR, DTPRMRAA, DTPRINVN, DTPRDSGR, DTPRVLSI,
       DTPRCPPG, DTPRINPG, DTPRMRPG, DTPRINVP, DTPRDSPG, DTPRSLCP, DTPRSLOT, DTPRSICP, DTPRTTLL, DTPRFCPG
FROM   CRD.DTPR WHERE PRSTCDGO = :id ORDER BY DTPRNMCT;

-- Pagos y su desglose (los componentes deben sumar el valor)
SELECT PGPRCDGO, EVPRCDGO, PGPRTPOO, PGPRVLRR, PGPRCPPG, PGPRINPG, PGPRMRPG, PGPRINVP, PGPRDSGR,
       PGPRVLSI, PGPRSLOT, PGPRANUL
FROM   CRD.PGPR WHERE PRSTCDGO = :id ORDER BY PGPRCDGO;

-- Invariante en cuotas vivas no precanceladas (debe devolver 0 filas)
SELECT DTPRCDGO FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST NOT IN (4) AND NVL(d.DTPRSLOT,0) = 0
AND    ABS(NVL(d.DTPRSICP,0) - (NVL(d.DTPRCPTL,0) + NVL(d.DTPRSLCP,0))) > 0.02;

-- Saldo de aportes por tipo (debe cuadrar con GET /aprt/saldosPorEntidad)
SELECT a.TPAPCDGO, SUM(a.APRTVLRR) FROM CRD.APRT a JOIN CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
WHERE  a.ENTDCDGO = :entidad AND t.TPAPIDST = 1 GROUP BY a.TPAPCDGO;

-- Evento e historial
SELECT * FROM CRD.EVPR WHERE PRSTCDGO = :id ORDER BY EVPRCDGO;
SELECT EVPRCDGO, DTPRNMCT, DTPRCPTL, DTPRINTR, DTPRSLCP FROM CRD.HDTP WHERE PRSTCDGO = :id ORDER BY DTPRNMCT;
```

Además: correr los reportes de aportes (G42 o dashboard) sobre una entidad de control antes y
después de un pago con aportes y verificar que la diferencia sea exactamente el monto pagado.
