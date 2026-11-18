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
- **Reparto:** cuentas de movimiento activas de la empresa, nunca por regularizar; Σ = monto al centavo; sentido por defecto el
  del valor en la cuenta por regularizar. Varias líneas con un reparto = proporcional por línea (la última fila absorbe el redondeo).
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
