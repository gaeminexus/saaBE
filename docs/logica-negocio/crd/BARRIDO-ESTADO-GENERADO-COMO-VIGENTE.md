# BARRIDO — Dónde un préstamo no desembolsado (estado 1 GENERADO, y 6/7) se trata como vigente

omen-saa-1-arb · 2026-11-08 · Barrido de solo lectura (agente Explore). Regla del usuario: «Todos estos generados si
no están desembolsados o vigentes, no se los debe tomar en cuenta en ningún lado, el préstamo es vigente desde que se
entrega el dinero». Con el estado nuevo APROBADO (diseño aparte) la misma lista aplica a APROBADO.

**Criterio de corrección:** cambiar listas negras («no terminal» = no 3/4/5…) por la lista blanca de préstamos
vivos **{2 VIGENTE, 8 DE PLAZO VENCIDO, 10 VIGENTE POR REVISAR, 11 EN MORA}** (o subconjunto donde ya lo hay).
Líneas aproximadas.

## A. Hay que cambiar (backend `src/main/java/com/saa/`)

| Lugar | Qué hace hoy | Cambio |
|---|---|---|
| `ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl:6629` `validarNovedadesFase2` | Cuota mínima pendiente acepta 1/2/8/11 | Quitar GENERADO (y valorar sumar 10) |
| `ejb/crd/daoImpl/PrestamoDaoServiceImpl:97` `selectByEntidadYProductoActivosById` | `IN (1,2,8,10,11)` para casar pagos Petro (aplicar pago, seguro HS, marcar mora) | `IN (2,8,10,11)` |
| `PrestamoDaoServiceImpl:124` `selectByEntidadYProductoVigentesAlCorte` | Primera rama `IN (1,2,8,10,11)` (reproceso, reapertura al corte) | Quitar el 1 |
| `PrestamoDaoServiceImpl:193` `selectVigentesByEntidad` | «Vigentes» = `NOT IN (3,4,5)` → devolución de aportes suma deuda de un GENERADO; app móvil lo muestra | `IN (2,8,10,11)` |
| `resources/rep/crd/RPRT_INFR_DVAP.jrxml:80` | Deuda vigente `NOT IN (3,4,5)` | `IN (2,8,10,11)` (+ .jasper) |
| `ejb/crd/serviceImpl/CertificadoServiceImpl:720` `bloqueoNoCancelado` | Un préstamo no 3/4/5 bloquea el certificado de no adeudar | 1, 6, 7, 12 no bloquean |
| `ejb/crd/serviceImpl/ProcesoMoraPrestamoServiceImpl:371` `calcularMoraPrestamo` | Llamada individual (`PrestamoRest:1904`) puede pasar un GENERADO a EN_MORA | Salir si no está en {2,8,10,11} |
| `ejb/crd/serviceImpl/MotorPagoPrestamoServiceImpl:297, 686` | Acepta pagos en GENERADO y luego puede cancelarlo | Rechazar 1, 6, 7 |
| `ejb/crd/serviceImpl/ProcesoPagoPrestamoServiceImpl:2243` `validarPrestamoOperable` | Pago, pago con aportes, precancelación sobre GENERADO | Rechazar 1, 6, 7 |
| `ProcesoPagoPrestamoServiceImpl:824, 1000` simular precancelación / desglose | Sin guarda de estado | Rechazar 1, 6, 7, 12 |
| `ejb/crd/serviceImpl/AbonoCapitalPrestamoServiceImpl:619, 892` | Abono a capital sobre GENERADO | Rechazar 1, 6, 7 |
| `ejb/crd/serviceImpl/SimulacionPrestamoServiceImpl:~235` `simularReestructuracion` | Solo rechaza 3/4/5 | Misma regla 2/11 que `SustitucionPrestamoServiceImpl:314` |
| `ejb/crd/serviceImpl/CobroCreditoServiceImpl:~1993` `validar` | Cobro sobre GENERADO | Rechazar 1, 6, 7 |
| `CargaArchivoPetroServiceImpl:5989` `verificarYActualizarEstadoPrestamo` | Podría cancelar un GENERADO | Saltar 1/6/7 (blindaje) |
| `ejb/crd/serviceImpl/ProcesoCargaPetroServiceImpl:591` `actualizarPrestamo` | Reescribiría un GENERADO a 2/11 (hoy inalcanzable) | Blindaje |
| `ws/movil/MovilMappers:73, 183` | App muestra GENERADO con saldo | Ocultar 1, 6, 7 |
| `ejb/crd/daoImpl/EntidadDaoServiceImpl:~213, ~288` resúmenes por estado | Totales del dashboard sin filtro de estado | Filtrar cartera `IN (2,8,10,11)` (confirmar qué mide el KPI) |

## A'. Frontend (`src/app/`)

| Lugar | Qué hace hoy | Cambio |
|---|---|---|
| `modules/crd/model/pagos/catalogos-pago.ts:73` `admiteOperaciones` | Lista negra (3,4,5,12,13): GENERADO admite pagos (cobros personales, cruce de valores, simulador de préstamo) | Lista blanca [2, 8, 10, 11] |
| `modules/crd/forms/entidad-participe/participe-dash/participe-dash.component.ts:2910` `recalcularEstadoPrestamo` | Escribe 1 como «Vigente»; sin llamadores | Borrar |

## B. Uso de flujo, correcto (no tocar)

Edición/aprobación/rechazo en 1/6/7 (`PrestamoServiceImpl` 201, 208, 219, 312, 366-372, 451, 705, 1219;
`NivelAprobacionServiceImpl:157`; `GarantePrestamoServiceImpl:190`; `EvaluacionOtorgamientoServiceImpl:153`), etiquetas
(Certificado, DevolucionAporteRest, ResolutorDesembolsoPrestamo, RPRT_TBLA_ACML), FE `prestamo-edit`, `garante-prestamo`.

**Ya excluyen el 1:** cierre de cartera (`ESTADOS_FUERA_DE_CARTERA "0,1,6,7,12"`), provisiones/mora de DTPR, G46,
pólizas, evaluación de otorgamiento, sustitución, pensiones, solicitud V1, y varias pantallas FE.
