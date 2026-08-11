# Corrección: Préstamo no pasaba a CANCELADO al pagar la última cuota

**Fecha:** 2026-08-11
**Archivos:**
- `src/main/java/com/saa/ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java`
- `src/main/java/com/saa/ejb/crd/dao/DetallePrestamoDaoService.java`
- `src/main/java/com/saa/ejb/crd/daoImpl/DetallePrestamoDaoServiceImpl.java`

## Síntoma

Al aplicar los pagos de una carga Petro, cuando el descuento cubría **completamente la última
cuota** del préstamo, la cuota quedaba correctamente en `PAGADA` (4) pero el préstamo seguía en
`VIGENTE` (2) en lugar de pasar a `CANCELADO` (3).

## Causa raíz

`verificarYActualizarEstadoPrestamo()` existía (ver `CORRECCION-ESTADO-PRESTAMO-Y-RENDIMIENTO.md`)
pero estaba invocada **en un solo punto**: la cola de `procesarPagoCuota()`, que solo se alcanza en
el caso de *pago exacto* y *pago parcial*. Todas las demás rutas que marcan una cuota como `PAGADA`
salían con `return` antes de llegar ahí:

| Ruta | Línea (antes) | ¿Verificaba? |
|---|---|---|
| `procesarPagoCuota` – pago exacto / parcial | cola del método | ✅ sí |
| `procesarPagoCuota` – **pago con excedente** | `return` tras `procesarExcedenteASiguienteCuota` | ❌ **no** |
| `procesarPagoCuota` – cuota ya PAGADA según `PagoPrestamo` | `return` temprano | ❌ no |
| `aplicarAfectacionManualConRegistroPago` (AVPC) | fin del método | ❌ no |
| `buscarCuotaAPagar` – cuota con saldo insignificante | marca PAGADA y sigue | ❌ no |
| `procesarExcedenteASiguienteCuota` – cuotas ya saldadas | marca PAGADA y sigue | ❌ no |
| `calcularSaldosRealesCuota` – recálculo desde `PagoPrestamo` | marca PAGADA y guarda | ❌ no |

La ruta que fallaba con más frecuencia es la del **pago con excedente**, que es justamente el caso
típico de la última cuota: Petro descuenta el valor mensual completo, la cuota final se salda y
sobra un excedente que ya no tiene cuota siguiente donde aplicarse
(`procesarExcedenteASiguienteCuota` loguea "No hay más cuotas pendientes" y retorna). El préstamo
nunca se reevaluaba.

Problemas secundarios detectados en la misma función:

1. **`prestamo.setFechaFin(now())`** — `PRSTFCFN` es la fecha de vencimiento de la última cuota
   (fin del plazo, la fija `PrestamoServiceImpl` al generar la tabla de amortización), **no** una
   fecha de cancelación. Sobrescribirla destruía el plazo original del préstamo. La entidad
   `Prestamo` no tiene campo `fechaCancelacion`.
2. **`selectCuotasNoPagadasByPrestamo` traga las excepciones y devuelve lista vacía**, y una lista
   vacía se interpretaba como "todo pagado" → un error de BD podía cancelar un préstamo vigente.
3. El JPQL usaba `d.estado NOT IN (:pagada, :canceladaAnticipada)`: en SQL una cuota con
   `estado = NULL` no entra en el resultado (`NULL NOT IN (...)` → `NULL`), es decir, una cuota sin
   estado se contaba como pagada.
4. No se validaba que el préstamo tuviera cuotas: un préstamo sin tabla de amortización daba
   "0 pendientes" y se habría cancelado.

## Solución aplicada

### 1. Nuevos métodos de conteo en `DetallePrestamoDaoService` / `Impl`

```java
Long contarCuotasByPrestamo(Long codigoPrestamo) throws Throwable;
Long contarCuotasPendientesByPrestamo(Long codigoPrestamo) throws Throwable;
```

- Usan `SELECT COUNT(d)` — no traen filas a memoria.
- El conteo de pendientes incluye explícitamente el estado nulo:
  `AND (d.estado IS NULL OR d.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada))`
- **No** atrapan la excepción: si la consulta falla se propaga y el llamador simplemente no cancela
  el préstamo (fail-safe), en vez de cancelarlo por un falso "0 pendientes".

### 2. `verificarYActualizarEstadoPrestamo()` reescrita

- Sale temprano si el préstamo ya está en un estado terminal (`CANCELADO`,
  `CANCELADO_ANTICIPADO`, `CANCELADO_POR_NOVACION`).
- Valida `contarCuotasByPrestamo(...) > 0` antes de evaluar la cancelación.
- Cancela solo si `contarCuotasPendientesByPrestamo(...) == 0`.
- **Ya no toca `fechaFin`**; sella la actualización con `fechaModificacion = now()`.
- Sigue atrapando `Throwable` para no abortar el procesamiento de la carga.

### 3. Nueva `verificarYActualizarEstadoPrestamos(List<Prestamo>)`

Verifica una lista de préstamos sin repetir el mismo código (deduplica por `codigo`).

### 4. Puntos de invocación agregados

| Ubicación | Motivo |
|---|---|
| `procesarPagoCuota` — tras el `procesarExcedenteASiguienteCuota` del bloque *pago con excedente* | **Corrección principal**: la última cuota se paga completa y sobra excedente |
| `procesarPagoCuota` — tras el `procesarExcedenteASiguienteCuota` de la rama "cuota ya PAGADA" | El excedente puede liquidar la última cuota |
| `verificarYAplicarAfectacionesManualesTotales` — tras aplicar todas las AVPC | Una afectación manual puede liquidar la última cuota |
| `aplicarPagoParticipe` — al final, sobre la lista `prestamos` | Red de seguridad: `buscarCuotaAPagar` y `calcularSaldosRealesCuota` también marcan cuotas `PAGADA` sin pasar por `procesarPagoCuota` |

## Verificación en BD

```sql
-- Préstamos con todas las cuotas pagadas pero aún NO cancelados (deben ser 0 tras el cambio)
SELECT p.PRSTCDGO, p.ESPSCDGO
  FROM CRD.PRST p
 WHERE p.ESPSCDGO NOT IN (3, 4, 5)
   AND EXISTS (SELECT 1 FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO)
   AND NOT EXISTS (SELECT 1
                     FROM CRD.DTPR d
                    WHERE d.PRSTCDGO = p.PRSTCDGO
                      AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7)));
```

Los préstamos ya afectados por el bug se pueden regularizar con un `UPDATE` puntual usando ese
mismo predicado (`SET ESPSCDGO = 3, PRSTFCMD = SYSDATE`).

> Nota: los nombres de columna del SQL de arriba deben confirmarse contra el DDL real de
> `CRD.PRST` / `CRD.DTPR` antes de ejecutarlo.

## Pendiente / no incluido

Un préstamo cancelado sigue conservando sus `saldoPorVencer` / `saldoVencido` / `saldoTotal`
anteriores. No se pusieron en `0` porque queda fuera del alcance del reporte del problema; si los
reportes de cartera lo requieren, es un cambio de una línea en
`verificarYActualizarEstadoPrestamo()`.
