# API — Pensión complementaria de un mes pasado (`POST /pgpc/reprocesarPeriodo`, C20)

Aprobado por el líder el 2026-10-28 (cambio C20 del re-proceso jun-2025 → nov-2026; decisiones D-026 y D-027 punto 4:
devengo en el mes del rol, pago en la fecha del banco). Análisis que lo motiva: expediente `cuadreSuper`,
`03-trabajo/meses/2025-06/sql/14-pension-complementaria-junio.md` §6. Contrato del motor mensual:
`API-PAGO-PENSION-COMPLEMENTARIA.md` y `API-DOS-PROCESOS-MENSUALES-JUBILADOS.md`.

## 1. Por qué el motor mensual no sirve para un mes pasado

| Rasgo de `/pgpc/pensiones/generar` | Efecto en un mes pasado (junio 2025) |
|---|---|
| **Ancla**: genera desde el mes siguiente al último negativo vigente del aporte 23 | 206 de 208 jubilados tienen negativos posteriores (hasta los PGPC reales de 2026): junio sale `AL_DIA` |
| **Universo**: `selectByIdEstado(3)` con el estado de HOY | Deja fuera 32 jubilados del rol de junio (estado 7, 5, o 3 sin VPPC) |
| **Valor**: la VPPC de hoy | No hay VPPC para 23 del rol |
| **Cruce**: `min(pensión, deuda exigible, saldo)` contra todo lo exigible | No reproduce lo que el rol descontó de cada préstamo; no admite préstamos hoy cancelados |
| **Seguro**: exige el proceso de seguro del período (`CRJB`) | Para junio 2025 no existe |

## 2. Qué hace `reprocesarPeriodo`

**Universo.** Las `filas` de la solicitud: el rol del mes es el documento y sus valores mandan sobre la VPPC. Sin
filas (modo **padrón**): los jubilados con una VPPC activa cuyo traslado de jubilación al aporte 23
(`APRTTPMV = JUBILACION`) es de a más tardar el fin del mes, con la VPPC como valor. El estado de hoy no decide en
ninguno de los dos modos. La VPPC no tiene fecha de inicio ni de baja (su `VPPCFCRG` es la fecha de carga, no la de
jubilación), por eso el padrón usa el traslado; las bajas anteriores al mes no se pueden detectar con los datos de hoy:
el modo padrón es un respaldo, el rol es lo que hay que usar.

**Por jubilado, paso 1 — devengo (su propia transacción, fecha = fin del período, `fechaDelPeriodo`):**

1. **Cruces** (`filas[].cruces[]`): cada descuento de préstamo del rol se aplica como **cobro con aportes de pensión
   complementaria** (`ProcesoPagoPrestamoService.pagarConAportes`, tipo 23) contra su préstamo, con fecha del período:
   - el préstamo tiene que ser del jubilado y haber estado **vigente al corte** (fin del mes). Si hoy está cancelado,
     **D-019**: se reabre como en Petro y en CBCR (`ReaperturaVigenteAlCorteService`, marca
     `REPROCESO PGPC AAAA-MM entidad N: cancelación posterior pendiente de re-aplicar (estado anterior E)`); las cuotas
     del mes saldadas sin pago vuelven a pendiente;
   - la mora se recalcula a la fecha del período (la misma corrección H42 del CBCR);
   - se cruza `min(valor del rol, exigible al fin del mes, pensión − seguro − lo ya cruzado)`. El motor aplica a la
     cuota pendiente más antigua (si hubiera mora residual anterior, se paga primero, como en Petro) y nunca a cuotas
     que vencen después del mes;
   - lo que el rol descontó y **no** se pudo cruzar (sin préstamo, préstamo no vigente al corte, excede la cuota o la
     pensión) **no se descarta**: no sale del aporte 23 (queda en la cuenta individual del jubilado) y se informa en
     `novedades` (`DESCUENTO_SIN_PRESTAMO`, `PRESTAMO_NO_VIGENTE_AL_CORTE`, `DESCUENTO_EXCEDE_CUOTA`,
     `DESCUENTO_EXCEDE_PENSION`, `PRESTAMO_DE_OTRA_ENTIDAD`, `CRUCE_PARCIAL`).
2. **Neto** = `pensión − seguro − cruzado`, topado en el neto del rol (`netoRol`, si viene) y nunca negativo. Un neto
   negativo del rol (el descuento supera la pensión) deja neto 0 y la cuota con el saldo que no se cubrió.
3. **Aporte 23**: dos negativos con fecha del período — seguro (glosa `... (REPROCESO C20) - SEGURO MEDICO`) y neto
   (`PAGO PENSION COMPLEMENTARIA M/AAAA - Entidad N (REPROCESO C20)`). El cruce ya generó el suyo en `pagarConAportes`.
4. **PGPC** del período (completa el stub del seguro si existiera) y **asiento de devengo** (plantilla 35) por el neto
   y el seguro. `PGPCESTD` = 1 REGISTRADA si hay neto (el pago es el paso 2), 3 PAGADA si el neto es 0.
   D-034 (29-oct-2026): si el cruce absorbió toda la pensión y no hay seguro que devengar aquí, **no se crea asiento**
   (`idAsientoDevengo` nulo); antes quedaba una cabecera sin líneas (asientos 12767 y 12974 de junio 2025). Lo mismo en
   el camino normal (`registrarPgpcDelMes`). Además `AsientoContableService` rechaza todo asiento sin líneas
   (`ASIENTO_SIN_LINEAS`).
5. Auditoría de bandas (`CRD.DSBN`, origen `PAGO_PENSION`) de los pagos del cruce.

**Por jubilado, paso 2 — pago (OTRA transacción: si falla, el devengo queda):** orden de CxP
(`CRD_PENSION_COMPLEMENTARIA`, `idOrigen` = PGPC, producto 517) por el neto:

- con `fechaPagoBanco` y cuenta (`idCuentaBancariaPago` de la fila o de la solicitud): la orden nace **CONFIRMADA**
  (débito automático) y CxP contabiliza el egreso **D 2.3.01.10.03 / H banco** y su movimiento bancario en la
  **fecha del banco**, con la referencia (`referenciaPago`). Mismo camino que el desembolso histórico (C15/C16). El
  PGPC queda PAGADA con `PGPCFCPG` = fecha del banco y `PGPCNMAS` = asiento del pago;
- sin fecha: orden **pendiente de aprobación** (sin cuenta, como la corrida normal) y PGPC EN_PAGO. Se confirma
  después en tesorería con `/pgtr/aprobar` + `/pgtr/confirmarManual` y su `fechaPago` (FECHA-CONTABLE-REPROCESO 3.6);
  el reconciliador (`/pgpc/sincronizarPagos`) pasa el PGPC a PAGADA.

**Idempotente.** Un PGPC del período ya completo no se vuelve a devengar (`YA_EXISTIA`); un PGPC con orden vigente
no genera otra. Mandar el mismo lote dos veces no duplica nada; si el paso 2 falló, volver a mandarlo solo crea el
pago. **No toca meses posteriores**: no mira el ancla, no crea `CRD.CRJB` y no genera la orden al proveedor del seguro.

**Prevalidación total** (antes del primer jubilado, sin escribir nada): período en el pasado; usuario; cuenta del
aporte 23 en la plantilla 21 y producto 517 (los guards del motor mensual); plantilla 35; período contable **abierto**
del fin de mes y de **cada** `fechaPagoBanco`; una fila por entidad; fecha de pago no anterior al devengo y con cuenta.

## 3. Contabilidad que deja

| Momento | Fecha | Asiento |
|---|---|---|
| Devengo (plantilla 35) | fin del período | D 2.1.02.25.01 cuenta individual / H 2.3.01.10.03 pensiones por pagar (neto); D 2.1.02.25.01 / H 2.3.90.90.06 seguro por pagar (seguro) |
| Cruce con préstamos (plantilla 21, `pagarConAportes`) | fin del período | D cuenta individual (aporte 23) / H cartera de la cuota (capital, interés, mora, seguros) |
| Pago (CxP) | fecha del banco | D 2.3.01.10.03 / H banco de la cuenta del extracto |

El saldo de 2.3.01.10.03 al fin del período es el neto aún no pagado; cae a 0 con los pagos de cada fecha del banco.
El seguro devengado queda en 2.3.90.90.06 hasta que se registre el pago al proveedor (este proceso no lo genera; la
respuesta lo muestra en `seguroSinOrdenProveedor`).

## 4. Endpoint

```http
POST /SaaBE/rest/pgpc/reprocesarPeriodo
Content-Type: application/json
```

```jsonc
{
  "idEmpresa": 1236, "anio": 2025, "mes": 6,
  "usuario": "CUADRE-SB",          // queda en PGPC/APRT/EVPR
  "idUsuario": 38,                 // SCP.PJRQ que registra las órdenes en CxP (opcional: si falta, se busca 'usuario' por nombre)
  "idCuentaBancariaPago": 418,     // cuenta por defecto de las filas con fechaPagoBanco
  "referenciaPago": "Ru-858739",   // referencia por defecto
  "filas": [
    { "idEntidad": 6486, "pension": 1342.61, "seguroSalud": 0, "netoRol": 1077.30,
      "fechaPagoBanco": "2025-09-29",
      "cruces": [ { "producto": "PE", "valorRol": 265.31, "idPrestamo": 7746, "idCuotaJunio": 569818 } ] },
    { "idEntidad": 6519, "pension": 1476.10, "seguroSalud": 0, "netoRol": 1476.10,
      "fechaPagoBanco": "2025-10-22", "idCuentaBancariaPago": 417, "referenciaPago": "TW 9005701", "cruces": [] },
    { "idEntidad": 6540, "pension": 940.39, "seguroSalud": 0, "netoRol": 940.39, "fechaPagoBanco": null, "cruces": [] }
  ]
}
```

Los campos de más (los descriptivos del lote del expediente, `estadoEntidadHoy`, `nota`, `operacion`...) se ignoran,
así que el lote se manda tal cual. `idCuotaJunio` (alias de `idCuota`) es informativo.

Respuesta 200 `{exito, mensaje, resultado}`; `resultado` (`ResultadoReprocesoPensionPeriodo`): `origenUniverso`
(ROL/PADRON), `evaluados`, `generados`, `yaExistian`, `conError`, `pagosConfirmados`, `ordenesPendientes`,
`totalPension`, `totalSeguro`, `totalCruzado`, `totalNeto`, `totalPagadoBanco`, `totalExcedenteNoCruzado`,
`seguroSinOrdenProveedor`, `errores[]` y `detalle[]` por jubilado (`idPago` = PGPC, `estado`
GENERADO/YA_EXISTIA/ERROR, `pension`, `seguro`, `cruzado`, `neto`, `excedenteNoCruzado`, `idAsientoDevengo`,
`idPagoProgramado`, `estadoPago` CONFIRMADO/PENDIENTE/SIN_PAGO/ERROR, `fechaPago`, `idAsientoPago`, `novedades[]`,
`reaperturaAlCorte[]`). Un error de un jubilado no detiene a los demás. 4xx/422 solo por la prevalidación
(`PARAMETRO_INVALIDO`, `USUARIO_NO_ENCONTRADO`, `PERIODO_CERRADO`, plantillas).

## 5. Ejemplo: junio 2025

Lote: `cuadreSuper/03-trabajo/cartera/resultados/14-lote-pgpc-junio-2025.json` (208 jubilados), ya con
`idEmpresa/anio/mes/usuario/idUsuario/idCuentaBancariaPago/referenciaPago` y la cuenta 417 en las dos filas de
Internacional. Prerrequisitos del expediente: borrado de los 186 aportes migrados de pensión de junio y reapertura de
las 36 cuotas (`14-pension-complementaria-junio.md` pasos 2 y 3), re-proceso 352 corrida 3, período 2025-06 abierto, y
también **sep-2025 y oct-2025 abiertos** (fechas de los pagos del banco) o la prevalidación lo rechaza.

Qué debe salir:

| Concepto | Valor |
|---|---|
| PGPC generados | 208 |
| Pensión | 146.217,18 |
| Seguro de salud (8 × 56,30) | 450,40 — sin orden al proveedor (novedad del expediente) |
| Descuentos del rol | 12.301,46 |
| Cruzado | ≈ 10.795,20: 12.301,46 − 454,08 (6516, sin préstamo) − 453,28 (6572) − 598,90 (6674), menos los excesos de centavos sobre la cuota que informe `novedades` |
| Neto devengado | 134.517,50 (netos positivos del rol; 6572 y 6674 en 0) |
| Pagos confirmados | 185 × cuenta 418 al 29-sep-2025 (Ru-858739) = 118.183,81; 6519 (TW 9005701) 1.476,10 y 6596 (CHQ 8043) 58,60 × cuenta 417 al 22-oct-2025 |
| Órdenes pendientes | 19 = 14.798,99 (12 "OK PAGADO" fuera del lote, 4 + 1 FALTA, orden 9252803, pago parcial de 6692) |
| Sin pago | 6572 y 6674 (neto del rol negativo) |

El lote del banco del 29-sep es de 121.017,30: la diferencia de 2.833,49 con los 185 no se puede asignar sin el
detalle del pago masivo (`14-pension-complementaria-junio.md` §3.2). El cheque 8043 entra como débito con referencia
`CHQ 8043` (no se emite un cheque nuevo de la chequera).

## 6. Control

- `sql/349_CONTROL_REPROCESO_PENSION_PERIODO.sql` (saaBE): por jubilado (PGPC, neto, seguro, cruzado, asientos,
  orden), totales del período, asiento de devengo por cuenta, asientos del cruce por cuenta, órdenes de pago por
  estado/cuenta/fecha/referencia, asiento del pago por fecha y cuenta, préstamos reabiertos por D-019.
- `cuadreSuper/03-trabajo/meses/2025-06/sql/14b-control-reproceso-pension-junio.sql` (expediente): **rol vs SAA por
  jubilado** con los valores del rol embebidos (solo ids y montos): diferencias de pensión, seguro, descuento y neto,
  y estado del pago.

## 7. Límites conocidos

- La orden al proveedor del seguro médico del período no se genera (no hay pago identificado en el banco para junio
  2025); el seguro queda devengado en 2.3.90.90.06.
- El pago parcial (6692: rol 326,91, pagado 185,62) se registra como orden pendiente por el neto del rol; el ajuste lo
  decide el responsable de ASOPREP.
- `CRD.CRJB` no se toca: el seguimiento de la corrida mensual (`/pgpc/corrida/{anio}/{mes}`) de un mes re-procesado
  sigue diciendo PENDIENTE. Es a propósito: con un `CRJB` con asiento propio del seguro, el devengo no incluiría el
  seguro.
