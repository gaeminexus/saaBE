# C34 — Seguimiento de Novedades v2 (REST `/nvdd2`)

Pantalla aparte de la v1 (C25) para que el usuario de ASOPREP siga cada novedad por sí mismo. Diseño aprobado por el líder
el 18-nov-2026: expediente `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/C34-NOVEDADES-V2-DISENO.md`.

- DDL: `sql/C34-01-novedades-v2.sql` (va después de C25-01 y C25-02, antes del WAR).
- Semillas (expediente, `03-trabajo/cambios-saa/c34-novedades-v2/sql/`): `C34-02` líneas por justificar, `C34-03*` detalle de
  origen por familia.
- Servicio: `ejb/cnt/service/SeguimientoNovedadV2Service` + `serviceImpl/SeguimientoNovedadV2ServiceImpl`; REST
  `ws/rest/cnt/NovedadV2Rest`; DTO `AccionNovedadV2`; entidades `LineaNovedad`, `JustificacionLinea`, `RepartoNovedad`,
  `DetalleOrigenNovedad` (+ columnas C34 en `NovedadSeguimiento`, `RequerimientoNovedad`, `RespuestaNovedad`, `AdjuntoNovedad`).

## Reglas

| Grupo (calculado) | Cuándo | Cómo se cierra |
|---|---|---|
| `POR_REGULARIZAR` | La novedad tiene líneas en `CNT.LNNV` | Cada línea justificada hasta saldo 0 (total o parcial), con su reparto a cuentas |
| `PEDIDO` | Sin líneas, con requerimientos vigentes | Cada pedido respondido (queda ACEPTADO al registrar) |
| `INFORMATIVA` | Sin líneas ni requerimientos | «Dar por conocida» (individual o en bloque) |

- **Sin cuatro ojos** (aprobación del líder): lo registrado vale de inmediato; se corrige anulando con motivo.
- **Cerrar** = resolución completa y estructurada para la pasada v8 del re-proceso; no genera asientos ni toca saldos. Al cerrar,
  los requerimientos abiertos quedan APLICADO (la v1 la ve cerrada).
- **Reparto:** cuentas de movimiento activas de la empresa, nunca por regularizar; cada fila con valor > 0 y sentido `D`/`H`.
  - **Al justificar líneas (C34b) es el asiento de regularización:** la línea de la cuenta por regularizar (cuenta de la línea,
    monto justificado, sentido contrario al movimiento) la agrega el sistema; las filas del reparto pueden ir al debe o al haber
    y el asiento completo cuadra al centavo (salida `D`: Σ D − Σ H del reparto = monto; entrada `H`: Σ H − Σ D = monto).
    Si no cuadra → 400 «El asiento no cuadra: el debe suma … y el haber …». El monto puede ser una parte del saldo.
  - Varias líneas con un asiento = proporcional por línea; el centavo de redondeo va a la última fila del sentido del movimiento,
    así el asiento de cada justificación cuadra con su monto.
  - **Al responder un pedido** no hay línea implícita: Σ reparto = monto al centavo (sin cambios).
- **Cruce:** dos líneas de sentido contrario (una salida y una entrada, en cualquiera de las cuentas por regularizar: p. ej. una transferencia y su devolución); justifica ambas; anular una anula las dos.
- **Confirmación/aprobación:** SÍ o NO; NO exige texto y además reparto, decisión o documento. Aprobación y decisión exigen la
  decisión (`DCSNCDGO`). Documento: adjunto, número o texto; «NO_EXISTE» exige texto y decisión.
- Errores de negocio: 400 con mensaje para el usuario final.

## Rutas

| Método y ruta | Qué |
|---|---|
| `GET /nvdd2/resumen?idEmpresa&anio&mes` | Contadores por grupo (abiertas, listas, cerradas; valor y saldo de por regularizar) |
| `GET /nvdd2/bandeja?idEmpresa&anio&mes&grupo&area&estado&texto` | Filas con `estado` (PENDIENTE, EN_PROCESO, LISTA, CERRADA, POR_CONOCER; filtro `ABIERTAS`), `siguientePaso`, `puedeCerrar`, `motivoNoCierra`. `texto` busca también idAsoprep y cédula/nombre del detalle de origen |
| `GET /nvdd2/{id}/fila` · `GET /nvdd2/{id}/detalle` | Fila; detalle (textos, cuentas, líneas con justificaciones/reparto/adjuntos, pedidos, detalle de origen con idAsoprep/cédula/nombre, decisiones, historial) |
| `GET /nvdd2/buscar/{decisiones,cuentas,prestamos,participes}` · `GET /nvdd2/prestamo/{id}/cuotas` | Buscadores |
| `POST /nvdd2/justificar` | `{usuario, lineas:[{idLinea, monto?}], tipo, idLineaCruce?, reparto:[{idCuenta, valor, sentido?, idPrestamo?, idCuota?, idEntidad?, glosa?}], idDecision?, fechaHecho?, tipoDocumento?, numeroDocumento?, texto?, adjuntos?}` |
| `POST /nvdd2/justificaciones/{id}/anular` · `POST /nvdd2/respuestas/{id}/anular` | `{usuario, motivo}` |
| `POST /nvdd2/pedidos/{id}/responder` | `{usuario, opcion?, texto?, idDecision?, monto?, reparto?, filasDetalle?, adjuntos?, ...}` |
| `POST /nvdd2/{id}/cerrar` · `/{id}/reabrir` · `/informativas/conocidas` | Devuelven `fila`/`filas` actualizadas |
| `GET /nvdd2/exportar?idEmpresa&anio&mes&formato=reproceso\|asoprep` | Una fila por cuenta destino de cada justificación o respuesta; `reproceso` sin cédula ni nombre |

## Prueba (18-nov-2026, WildFly local 8080, base de trabajo)

Script `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34.js` (usuario de ensayo «USUARIO 1»):
29 comprobaciones OK (parcial con reparto a dos cuentas, resto con decisión por el mismo usuario, anulación, errores de
reparto/monto/decisión, cruce y su anulación, confirmación NO/SÍ, cierre y reapertura, informativa conocida visible en la v1,
exportaciones). Datos de prueba limpiados con `sql/C34-90-limpieza-pruebas-18112026.sql`.

## C34b (18-nov-2026): asiento de regularización con debe y haber

Pedido de ASOPREP: al justificar, ingresar cómo debe quedar el asiento (debe y haber), también cuando solo se justifica una
parte del movimiento. `validarReparto` recibe el sentido de la línea y valida el cuadre con la línea implícita de la cuenta por
regularizar; `persistirReparto` ajusta el redondeo para que cada justificación cuadre. Sin DDL. Prueba en la base de trabajo:
`cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34b.js` (7/7 OK).

## C34d (18-nov-2026): responder con un hecho de crédito y elegir el origen del dinero

DDL `sql/C34-07-hechos-credito.sql`: `CNT.HCNV` (hecho), `CNT.OHNV` (orígenes), `HCNVCDGO` en `JSNV`/`RSNV`/`ADNV`, tipo de
justificación `HECHO` y acciones de historial `HECHO_CREDITO`/`ANULA_HECHO`. Diseño:
`cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/C34D-HECHOS-DE-CREDITO-DISENO.md`.

| Ruta | Qué hace |
|---|---|
| `GET /nvdd2/origenes?idEmpresa&fuente&idPrestamo&idEntidad&texto&desde&hasta&valor&fecha` | Buscador de orígenes. `fuente`: `SUGERIDOS` (necesita `idPrestamo`: mismo partícipe, cónyuge `CRD.CNYG` y referencias familiares `CRD.RRFF` por cédula, y valor ± una cuota), `EXTRACTO`, `APORTE` o `PENDIENTE`. Incluye lo ya procesado o conciliado con «aplicado hoy» (asiento de la conciliación, partida pendiente y posible aporte del mismo día y valor) y lo disponible (descontado lo usado en hechos vigentes). Ordena por días/30 + diferencia de valor / cuota. |
| `GET /nvdd2/hechos/candidatos?idEmpresa&idPrestamo` | Movimientos por regularizar y pedidos abiertos de la operación o de su titular (marcas `{prestamo:N}` / `{entidad:N}`, glosa con la operación, detalle de origen). |
| `POST /nvdd2/hechos` | `{usuario, idEmpresa, idPrestamo, tipo, fecha, valor, idCuota?, texto?, tipoDocumento?, numeroDocumento?, origenes:[{tipo, idFilaExtracto?, idAporte?, idLinea?, valor, conceptoReal?, texto?}], lineas:[id], pedidos:[id], adjuntos?}`. Reglas: Σ orígenes = valor; un movimiento no se usa por encima de su valor; `SIN_DINERO` exige texto; los orígenes `PENDIENTE` quedan resueltos solos; una justificación `HECHO` por movimiento y una respuesta por pedido; las novedades que quedan sin nada pendiente se cierran («Resuelta por el hecho de crédito N.º …»). |
| `POST /nvdd2/hechos/{id}/anular` | `{usuario, motivo}`: anula el hecho, sus justificaciones y respuestas (los pedidos vuelven a pendiente). Si una novedad quedó cerrada, hay que reabrirla antes. Una justificación `HECHO` no se anula sola. |
| `GET /nvdd2/exportar/hechos?idEmpresa` | Hechos vigentes para la pasada v8: operación (`idPrestamo`, `idAsoprep`), tipo, fecha, valor, cuota, orígenes («estaba como» con marcas `{entidad:N}`, «en realidad es», entidad anterior) y movimientos/pedidos que resuelve. Sin nombres ni cédulas. |

Además: el detalle muestra el hecho en cada justificación o respuesta que lo usa; cada movimiento trae `idPrestamoSugerido`
(la operación de su texto); en `POST /nvdd2/justificar` el reparto (asiento) ahora es opcional: la pantalla solo lo pide con el
permiso 1569 REGISTRAR AJUSTE. Prueba: `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34d.js` (25/25).

### C34d ampliado por D-123 (C34-07b)

| Ruta | Qué hace |
|---|---|
| `GET /nvdd2/hechos/calcular?idPrestamo&tipo&desde&fecha` | Para `PRECANCELACION` y `SERIE_PRECANCELACION`: reparto por mes desde `CRD.DTPR` (sin cuotas anuladas): total de cada cuota con vencimiento entre el primer día del mes `desde` y `fecha`, más el capital de las cuotas que vencen después de `fecha`. Devuelve `filas`, `numeroCuotas`, `totalCuotas`, `saldoCapital`, `valor` y, como referencia, `simulador` (`simularPrecancelacion`, que parte del estado de hoy de las cuotas) cuando da un valor. |
| `POST /nvdd2/hechos` (campos nuevos) | `desde` (mes desde); `tratamientoDiferencia` (`NO_SE_DEBITA` / `SE_DEVUELVE` si sobra, `PARTICIPE_DEBE` si falta; obligatorio cuando Σ orígenes ≠ valor); `pedidosParciales` (el hecho queda como respuesta `RSNVOPCN = PARCIAL` y el pedido sigue pendiente); `idNota` (nota que se convierte). En los tipos calculados el valor lo pone el SAA y se guarda el reparto en `HCNVDTLL`. |
| `POST /nvdd2/{id}/notas` · `POST /nvdd2/notas/{id}/anular` | Nota de ASOPREP (`CNT.NTNV`): texto y adjuntos, sin cuentas; no cambia saldos ni estados. El detalle la trae en `notas`. |

Prueba: `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34d-d123.js` (18/18).

### C34e (C34-07c): lo que falta se cruza con los aportes

`tratamientoDiferencia = CRUCE_APORTES` con `tipoAporteDiferencia` (tipo de aporte de la cuenta individual del partícipe,
`HCNVTPAP`). Los candidatos traen `saldosAportes` (`SaldoAporteService.saldosPorEntidad`). La v8 lo aplica con el motor de
crédito en la fecha del hecho.

### C34f (C34-07d, diseño C34D §10): lo que falta se condona

| Ruta | Qué hace |
|---|---|
| `GET /nvdd2/hechos/componentes?idPrestamo&tipo&cuotas=csv&desde&fecha` | Lo adeudado por cuota y concepto a la fecha del hecho. **`PAGO_CUOTAS`** con `cuotas` (DTPRCDGO): `SEGURO`, `DESGRAVAMEN`, `MORA` (fórmula de `ProcesoMoraPrestamoService.calcularMoraCuota` con la tasa diaria del préstamo), `INTERES` (interés + interés vencido) y `CAPITAL`, desde los valores de la tabla (`MotorPagoPrestamoService.calcularSaldosCuota`), como si la cuota estuviera impaga. **`PRECANCELACION` / `SERIE_PRECANCELACION`**: el mismo cálculo de `/hechos/calcular`. Devuelve `filas`, `totales`, `total`, `fecha` y `fuente`. |
| `POST /nvdd2/hechos` (campos nuevos) | `cuotas` (pago de cuotas: el SAA calcula su valor y guarda los componentes en `HCNVDTLL`). Con `tratamientoDiferencia = CONDONACION` (solo si falta) también van `conceptosCondonados` (subconjunto de `MORA`, `INTERES`, `CAPITAL`) e `idDecision` (decisión de ASOPREP, `CNT.DCSN`), más al menos un adjunto. Sin alguno de los tres responde 400. |

Reparto (`repartoCondonacion`):
1. El dinero de los orígenes paga primero lo no condonable, cuota por cuota en el orden de pago del SAA (seguro, desgravamen,
   mora, interés, capital). Si no alcanza, responde 400 con lo que falta.
2. Con lo que queda paga los conceptos marcados.
3. Lo que queda sin pagar de los conceptos marcados es lo condonado. Tiene que ser igual a lo que falta (± 0,02).

Se guarda en `HCNVCNDN` (JSON):
- `conceptos` y `fecha`;
- `filas`: por cuota, `adeudado`, `pagado` y `condonado`;
- `condonadoPorConcepto` y `totalCondonado`;
- `referenciaCuenta {plantillaAlterno: 25, lineaAuxiliar: 70}`;
- `cuentaAlRegistrar`: informativa. La v8 resuelve la cuenta al registrar.

El detalle y la exportación de hechos traen `condonacion`, `idDecision`, `numeroDecision` y `tituloDecision`. La pantalla
muestra la opción solo con el permiso `CRD_CONDONACION_DE_VALORES` (1488).

Este cambio no crea asientos ni cambia cuotas. El motor «condonar conceptos de una cuota» es un pendiente de la v8: se hará
después de fusionar C35, porque toca `MotorPagoPrestamoService` y `ProcesoPagoPrestamoServiceImpl`.

Prueba: `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34f.js` (12/12).

### C34g (D-125): movimientos resueltos por una regla general y el hecho que manda

D-125: los depósitos de partícipes con préstamo vigente se aplican a sus cuotas con la prelación de la carga Petro. El script
`cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/sql/C34-09b-D125-resueltos-por-regla.sql` los marca con una
justificación `DECISION` de D-125 por todo su saldo. Un movimiento así está «resuelto solo por la regla»
(`SQL_SOLO_REGLA`: todas sus justificaciones vigentes son DECISION de D-125).

- `SQL_LNNV` (orígenes pendientes y «qué resuelve») también trae esos movimientos, con su valor disponible y el aviso «hoy
  resuelto por la regla D-125 (el hecho la reemplaza)».
- `registrarHecho`: si un movimiento elegido como origen o en «qué resuelve» está resuelto solo por la regla, `liberarRegla`
  anula sus justificaciones D-125 con el motivo «Excepción a la regla D-125: la reemplaza un hecho de crédito» y deja historial.
  Si el hecho falla, la transacción se revierte. Si después se anula el hecho, el movimiento queda pendiente; para volver a la
  regla se corre otra vez C34-09b.
- El texto de las justificaciones resuelve las marcas `{entidad:N}` / `{prestamo:N}`, como los demás textos.

Prueba: `cuadreSuper/03-trabajo/cambios-saa/c34-novedades-v2/scripts/prueba-rest-c34g.js` (7/7).
