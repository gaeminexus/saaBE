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
