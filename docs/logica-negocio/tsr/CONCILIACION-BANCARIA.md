# Conciliación bancaria

**Fecha:** 2026-08-28 (primer documento de esta área — no existía ninguno).

## 1. Los dos mundos que hay que distinguir

Hay **dos motores de conciliación** en el código, y sólo uno está vivo:

| | Motor legado | Motor vivo |
|---|---|---|
| Clase | `ConciliacionServiceImpl` (`actualizaEstadoMovimiento`) | `ConciliacionContableMatchServiceImpl` |
| Entidad de cabecera | `Conciliacion` (`TSR.CNCL`) | `ConciliacionContable` |
| REST / pantalla | **Ninguno** | `ConciliacionContableRest` (`/rest/cnct/...`) |
| Qué hace | Marca `MovimientoBanco` como definitivo | Empareja `DetalleExtractoBancario` (extracto) contra `DetalleAsiento` (contabilidad) por monto+fecha |

El legado nunca se retiró del código, pero nadie lo llama: no tiene REST ni
pantalla. El vivo es al que entra el usuario, pero **hasta este cambio** no
tocaba `MovimientoBanco` en ninguna línea — por eso los 120 movimientos de la
base estaban todos "en tránsito" pese a que hay conciliaciones activas.
Este documento describe el motor vivo, ya conectado a `MovimientoBanco`.

## 2. `MovimientoBanco` (TSR.MVCB) — libro auxiliar de bancos

Es la fuente del saldo de una cuenta bancaria a una fecha:

- `CuentaBancariaServiceImpl.obtieneSaldoFecha` = cierre de `SaldoBanco` del
  período anterior + `MovimientoBancoService.saldoCuentaRangoFechas` del resto.
- `SaldoBancoServiceImpl` recorre los movimientos del período para armar el
  cierre mensual.

Cada movimiento nace con un tipo "en tránsito" (`TipoMovimientoConciliacion`,
rubro 37) cuando se genera desde el circuito de pagos/cobros — cheques
girados, transferencias, débitos/créditos bancarios — y se enlaza al
`Asiento` contable que lo originó (`MovimientoBanco.asiento`, FK a
`CNT.ASNT`). El saldo de la cuenta ya cuenta estos movimientos desde que
nacen: **el tipo "en tránsito" vs. "definitivo" no afecta el saldo**, es
información de conciliación (¿el banco ya lo reconoció?), no de contabilidad.

## 3. El flujo del motor vivo

```
ConciliacionContableMatchServiceImpl
 │
 ├─ obtenerPendientesExtracto / obtenerPendientesAsiento
 │     Filas de DetalleExtractoBancario / DetalleAsiento que aún no
 │     pertenecen a ningún GrupoConciliacionContable ACTIVO.
 │
 ├─ sugerirCoincidencias
 │     Auto-match por monto+fecha: 1:1, N:1 (varias filas de extracto suman
 │     una línea contable) y 1:N (una fila de extracto se reparte en varias
 │     líneas). No crea nada, sólo sugiere.
 │
 ├─ conciliarGrupo(idCuentaBancaria, idPeriodo, idsExtracto[], idsAsiento[], usuario)
 │     Valida: periodo no CERRADO, filas no ya conciliadas, monto cuadra
 │     (tolerancia $0.01), fechas dentro de la tolerancia configurada
 │     (rubro ASP_TOLERANCIA_DIAS_CONCILIACION_CONTABLE).
 │     Crea GrupoConciliacionContable + GrupoConciliacionExtracto (N) +
 │     GrupoConciliacionAsiento (N).
 │     ── Desde este cambio (2026-08-28) ──
 │     · Cierra los MovimientoBanco de los asientos involucrados (§4).
 │     · DetalleExtractoBancario.estadoRevision → CONCILIADA (§5).
 │
 ├─ deshacerGrupo(idGrupo, usuario)
 │     Simétrico: reabre los MovimientoBanco y devuelve estadoRevision a
 │     PENDIENTE_REVISION antes de desactivar el grupo.
 │
 └─ cerrarMes / reabrirMes
       Cierra el período de conciliación bancaria si todas las cuentas
       quedaron VERIFICADO (EstadoConciliacionContable). No relacionado con
       el cierre contable general.
```

REST: `/rest/cnct/...` (`cabecera`, `pendientesExtracto`, `pendientesAsiento`,
`resumenPorPeriodo`, `grupos`, `sugerencias`, `conciliar`, `deshacer`, …).

## 4. Cierre de `MovimientoBanco` al conciliar (nuevo, 2026-08-28)

**No hay FK directo** de `GrupoConciliacionContable`/`GrupoConciliacionAsiento`
a `MovimientoBanco`. Se llega por el asiento: cada `DetalleAsiento`
conciliado pertenece a un `Asiento`, y `MovimientoBancoDaoService.selectByAsiento`
resuelve los `MovimientoBanco` de ese asiento (puede haber más de uno, o
ninguno — un asiento manual sin movimiento bancario asociado no es error,
simplemente no hay nada que cerrar).

Para cada `MovimientoBanco` encontrado, `conciliarGrupo` marca:

- `conciliado = 1`
- `fechaConciliacion` = la del grupo (`GrupoConciliacionContable.fechaConciliacion`)
- `rubroTipoMovimientoH` pasa de "en tránsito" a "definitivo" según:

| En tránsito | | Definitivo |
|---|---|---|
| 1 `DEPOSITO_EN_TRANSITO` | → | 3 `DEPOSITO` |
| 2 `CHEQUES_GIRADOS_Y_NO_COBRADOS` | → | 4 `CHEQUE_COBRADO` |
| 5 `DEBITO_BANCARIO_EN_TRANSITO` | → | 7 `DEBITO_BANCARIO` |
| 6 `CREDITO_BANCARIO_EN_TRANSITO` | → | 8 `CREDITO_BANCARIO` |
| 9 `TRANSFERENCIAS_DEBITOS_EN_TRANSITO` | → | 11 `TRANSFERENCIAS_DEBITOS` |
| 10 `TRANSFERENCIAS_CREDITOS_EN_TRANSITO` | → | 12 `TRANSFERENCIAS_CREDITOS` |

`deshacerGrupo` aplica el mapeo inverso y limpia `conciliado`/`fechaConciliacion`.

**Idempotencia:** un movimiento cuyo tipo actual no está en la tabla de
arriba (porque ya es definitivo, o porque quedó en un valor no reconocido)
se deja **completamente intacto** — ni tipo, ni `conciliado`, ni
`fechaConciliacion`. Esto importa porque un mismo `Asiento` puede tener
varias líneas (`DetalleAsiento`) conciliadas en grupos distintos, en
momentos distintos: cerrar/reabrir un grupo no debe pisar el cierre de otro
grupo que comparte el mismo asiento.

**Limitación conocida:** como no hay FK grupo→movimiento, `deshacerGrupo`
identifica "qué movimientos tocar" por el mismo camino (asiento) que
`conciliarGrupo`, no por un registro explícito de "esto fue lo que este
grupo cerró". Si dos grupos distintos conciliaran el mismo asiento en
momentos distintos (posible sólo si el asiento tiene múltiples líneas), y
uno de ellos se deshace, la reversión toca los movimientos de ese asiento
que sigan en un tipo definitivo — que en el caso normal (una línea de
`DetalleAsiento` por movimiento bancario) es exactamente lo que ese grupo
cerró, pero no está garantizado por un FK.

No se reutilizó el mapeo del motor legado
(`MovimientoBancoServiceImpl.actualizaEstadoMovimiento`): tenía la misma
tabla pero un bug — el `UPDATE` grababa siempre `Estado.ACTIVO` en la
columna del tipo en vez del tipo calculado — y además exige una entidad
`Conciliacion` (legada) que el motor vivo no crea.

## 5. `DetalleExtractoBancario.estadoRevision` (nuevo, 2026-08-28)

Rubro 173 (`ASPEstadoRevisionExtracto`): `1` Pendiente de Revisión, `2`
Conciliada, `3` Descartada. Antes de este cambio, `conciliarGrupo` nunca lo
tocaba — una fila quedaba en el grupo (conciliada de hecho) pero la pantalla
la seguía mostrando "Pendiente de revisión" para siempre.

`conciliarGrupo` la pasa a `CONCILIADA` (2) al crear cada
`GrupoConciliacionExtracto`; `deshacerGrupo` la devuelve a
`PENDIENTE_REVISION` (1) al desactivar el grupo. El estado `DESCARTADA` (3)
no lo toca este flujo — es de otra pantalla/proceso.

## 6. Verificación

Antes / después de conciliar un grupo, contar movimientos por estado de
tránsito vs. definitivo:

```sql
SELECT
    CASE WHEN m.MVCBRZZA IN (1,2,5,6,9,10) THEN 'EN TRANSITO'
         WHEN m.MVCBRZZA IN (3,4,7,8,11,12) THEN 'DEFINITIVO'
         ELSE 'OTRO' END                       AS grupo_tipo,
    m.MVCBRZZA                                  AS tipo_codigo,
    m.MVCBCNCL                                  AS conciliado,
    COUNT(*)                                    AS cantidad
FROM TSR.MVCB m
GROUP BY m.MVCBRZZA, m.MVCBCNCL,
    CASE WHEN m.MVCBRZZA IN (1,2,5,6,9,10) THEN 'EN TRANSITO'
         WHEN m.MVCBRZZA IN (3,4,7,8,11,12) THEN 'DEFINITIVO'
         ELSE 'OTRO' END
ORDER BY 1, 2;
```

`MVCBRZZA` es el código alterno del rubro `TipoMovimientoConciliacion` (37);
para traer también la etiqueta legible habría que unir contra `SCP.PDTR` por
`PDTRALTR` (código alterno) filtrando por el rubro 37, no por `PDTRCDGO`
directo — se omite aquí porque los códigos de la tabla del §4 ya identifican
cada tipo sin ambigüedad.

Para verificar un grupo puntual recién conciliado (reemplazar `:idGrupo`):

```sql
SELECT m.MVCBCDGO, m.MVCBRZZA AS tipo, m.MVCBCNCL AS conciliado, m.MVCBFCCN AS fecha_conciliacion
FROM TSR.MVCB m
WHERE m.ASNTCDGO IN (
    SELECT da.ASNTCDGO
    FROM TSR.GCAS gca
    JOIN CNT.DTAS da ON da.DTASCDGO = gca.DTASCDGO
    WHERE gca.GRCCCDGO = :idGrupo
);
```

Y para `DetalleExtractoBancario.estadoRevision` del mismo grupo:

```sql
SELECT dex.DEXBCDGO, dex.DEXBESTR AS estado_revision
FROM TSR.DEXB dex
JOIN TSR.GCEX gce ON gce.DEXBCDGO = dex.DEXBCDGO
WHERE gce.GRCCCDGO = :idGrupo;
```

Todos los nombres de tabla/columna de estas consultas (`TSR.MVCB`, `TSR.GCAS`,
`TSR.GCEX`, `TSR.GRCC`, `CNT.DTAS`, `TSR.DEXB`, `CNT.ASNT`) están verificados
contra las anotaciones `@Table`/`@Column`/`@JoinColumn` de las entidades JPA
correspondientes.
