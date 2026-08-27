# PROMPT 04 — AGENTE BACKEND — Caja chica (fondo fijo con gastos, reposición, adjuntos, alerta y cierre)

**Agente:** BACKEND (`C:\work\saaBE\v1\saaBE`). **No tocar el frontend.**
**Prerrequisito:** el usuario ejecutó `docs/logica-negocio/tsr/sql/02-caja-chica.sql` (bloques 1-5) en la BD local: tablas `TSR.CJCH`, `TSR.MVCH`, `TSR.CRCH`, `TSR.PTCH` y rubros 232/233. **Léelo completo: ahí están todas las columnas, tipos y FKs; mapear exactamente esas columnas.** El prompt 02 (cheques) debe estar terminado porque la reposición se paga por `PagoProgramado` y puede ir con cheque.
**Leer:** `CLAUDE.md`, `docs/estandar/GUIA-MAPEO-TABLA-COMPLETO.md`, plan §3 en `docs/logica-negocio/tsr/PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md`.

## Referencias de código a imitar
- Contabilización de un egreso: `AsientoContableServiceImpl.generarAsientoEgresoTesoreria` (l.~895) y `obtenerCuentaGrupoProductoPago` (l.~969). Tipo de asiento `TipoAsientos.EGRESO_TESORERIA` (código alterno 5, plantilla T-EGRESOS), módulo `ModuloSistema.TESORERIA`.
- Pago de origen externo: `PagoProgramadoServiceImpl.registrarPagoDeOrigenExterno` (l.~500) y `contabilizarPagoOrigenExterno` (l.~1370); `com.saa.rubros.OrigenPagoExterno`; entidad `DetallePagoOrigenExterno` (PGS.DPGT).
- Adjuntos: `model/cxp/PathNegociacion` + `basico/ejb/FileService` (`uploadFileToPath`), REST `ws/rest/files/FileRest` (`POST /file/upload/custom?fileName=&uploadPath=`).
- Anulación de asiento: `asientoService.anulaAsiento(...)` como en `PagoProgramadoServiceImpl.revertirContabilidadEgreso` (l.~1309).

## Tareas

### T1. Modelo (paquete `com.saa.model.tsr`, `NombreEntidadesTesoreria`)
Cuatro entidades con `GenerationType.IDENTITY`, NamedQueries `XxxAll`/`XxxId`, getters/setters a mano:
- `CajaChica` (TSR.CJCH): `codigo`, `empresa` (`@ManyToOne Empresa`, PJRQCDGO), `nombre`, `planCuenta` (`@ManyToOne PlanCuenta`, PLNNCDGO), `montoFondo` (CJCHMNTO, Double), `montoMaximoGasto` (CJCHMXGS), `porcentajeAlerta` (CJCHPRAL), `responsable` (CJCHRSPN), `custodio` (`@ManyToOne Usuario`, CJCHUSCS), `observacion`, `estado`, `fechaRegistro` (LocalDateTime), `usuario` (CJCHUSAR, Long).
- `MovimientoCajaChica` (TSR.MVCH): `codigo`, `cajaChica`, `tipo` (MVCHTPOO), `fecha` (LocalDate), `valor`, `descripcion`, `observacion`, `producto` (`@ManyToOne ProductoPago`, MVCHPRDP → PRDP.ID), `titular` (`@ManyToOne Titular`, TTLRCDGO), `numeroDocumento` (MVCHNDOC), `asiento` (`@ManyToOne Asiento`), `pagoProgramado` (`@ManyToOne PagoProgramado`, PGTRCDGO), `cierre` (`@ManyToOne CierreCajaChica`, CRCHCDGO), `estado`, `motivoAnulacion` (MVCHMTAN), `fechaRegistro`, `usuario`.
- `CierreCajaChica` (TSR.CRCH): `codigo`, `cajaChica`, `fecha` (LocalDate), `fechaInicio`, `fechaFin`, `saldoInicial`, `totalGastos`, `totalReposiciones`, `totalAjustes`, `saldoLibros` (CRCHSLDO), `saldoFisico` (CRCHSLFS), `diferencia`, `observacion`, `estado`, `asiento`, `fechaRegistro`, `usuario`.
- `PathCajaChica` (TSR.PTCH): `codigo`, `movimiento` (`@ManyToOne MovimientoCajaChica`), `path`, `nombreDoc` (PTCHNMDC), `tipoDoc` (PTCHTPDC), `fechaRegistro`, `usuario`.
Rubros Java nuevos en `com.saa.rubros`: `TipoMovimientoCajaChica` (APERTURA=1, GASTO=2, REPOSICION=3, AJUSTE_POSITIVO=4, AJUSTE_NEGATIVO=5), `EstadoCierreCajaChica` (BORRADOR=1, CERRADO=2, ANULADO=3), y en `Rubros` `TIPO_MOVIMIENTO_CAJA_CHICA = 232`, `ESTADO_CIERRE_CAJA_CHICA = 233`. En `OrigenPagoExterno` agregar `TSR_CAJA_CHICA = "TSR_CAJA_CHICA"`.
DAO/DaoImpl/Service/ServiceImpl/Rest para las cuatro (`@Path("cjch")`, `("mvch")`, `("crch")`, `("ptch")`) siguiendo la guía de mapeo.

### T2. `CajaChicaService` (lógica)
- `Map<String,Object> saldo(Long idCaja)`: `{idCaja, nombre, fondo, saldo, porcentaje, alerta, montoSugeridoReposicion, ultimoCierre}` donde `saldo = Σ(tipo 1,3,4) − Σ(tipo 2,5)` de movimientos con `estado=1` (una query con `SUM(CASE ...)`), `porcentaje = saldo/fondo*100`, `alerta = saldo <= fondo*porcentajeAlerta/100`, `montoSugeridoReposicion = fondo − saldo` (mínimo 0), `ultimoCierre` = fecha del último cierre CERRADO o null.
- `List<Map<String,Object>> saldos(Long idEmpresa)`: lo anterior para todas las cajas activas de la empresa (para la alerta global).
- `CajaChica registrar(CajaChica c, Double saldoInicialMigrado, Long idUsuario)`: valida nombre único por empresa, fondo > 0, planCuenta obligatorio; graba; si `saldoInicialMigrado > 0` crea un `MovimientoCajaChica` tipo APERTURA, `descripcion = "SALDO INICIAL MIGRADO"`, **sin asiento** (el saldo ya está en la cuenta contable; caso de las cuentas bancarias 428/429 que se retiran).

### T3. `MovimientoCajaChicaService`
- `MovimientoCajaChica registrarGasto(Long idCaja, LocalDate fecha, Double valor, String descripcion, String observacion, Long idProducto, Long idTitular, String numeroDocumento, Long idUsuario)` (`REQUIRED`):
  validaciones con `IncomeException`: caja activa; `observacion` no vacía; `valor > 0`; `valor <= saldo`; si `montoMaximoGasto != null` → `valor <= montoMaximoGasto`; producto obligatorio y con grupo con planCuenta; `fecha` posterior a `fechaFin` del último cierre CERRADO.
  Asiento: nuevo método `AsientoContableServiceImpl.generarAsientoGastoCajaChica(Long idProducto, String concepto, Double valor, Long idPlanCuentaCaja, Long idEmpresa, LocalDate fecha, String observaciones, String usuario)` copiado de `generarAsientoEgresoTesoreria`, con HABER = `planCuenta` de la caja (en vez de la cuenta del banco), tipo `TipoAsientos.EGRESO_TESORERIA`, módulo TESORERIA. Observación de cabecera: `"Gasto caja chica " + nombreCaja + " | " + descripcion + " | Doc: " + nvl(numeroDocumento,"") + " | Valor: $" + format`. Líneas: DEBE `"Gasto caja chica: " + descripcion`; HABER `"Caja chica " + nombreCaja + ": " + descripcion`.
  Graba el movimiento con `asiento`, estado 1.
- `Map<String,Object> registrarReposicion(Long idCaja, Double valor, Long idCuentaBancariaOrigen, Long formaPago, boolean debitoAutomatico, String referencia, LocalDate fecha, String descripcion, Long idUsuario)` y `registrarApertura(...)` (misma firma; tipo 1): crean el `MovimientoCajaChica` (tipo 3 o 1, estado 1, sin asiento aún) y luego llaman `pagoProgramadoService.registrarPagoDeOrigenExterno(OrigenPagoExterno.TSR_CAJA_CHICA, idMovimiento, idCuentaBancariaOrigen, null, valor, fecha, idEmpresa, idUsuario, descripcion, debitoAutomatico, referencia, formaPago)` con `beneficiarioNombre = nombre de la caja`. Guardan `pagoProgramado` en el movimiento. Devuelven `{idMovimiento, idPago, estadoPago, numeroCheque}`.
  Validación: `valor <= fondo − saldo` para reposición (no sobrepasar el fondo); para apertura `saldo == 0`.
- **Contabilización de la reposición** — en `PagoProgramadoServiceImpl.contabilizarPagoOrigenExterno`: si `pago.getOrigenExterno().equals(OrigenPagoExterno.TSR_CAJA_CHICA)` → **no** usar DPGT; llamar a `AsientoContableServiceImpl.generarAsientoReposicionCajaChica(idPlanCuentaCaja, idCuentaBancaria, valor, idEmpresa, fecha, observaciones, usuario)` nuevo: DEBE `planCuenta` de la caja / HABER `cuentaBancaria.getPlanCuenta()`, tipo `EGRESO_TESORERIA`, módulo TESORERIA. Observación: `"Reposición caja chica " + nombreCaja + " | " + descripcion + " | Ref: " + ref + " | Valor: $x"` (para apertura, `"Apertura caja chica ..."`). Si el pago lleva cheque, aplican las reglas de glosa del prompt 02 (` | Cheque N° n Cta c`). Crear el movimiento bancario igual que en el resto de `contabilizar*`. Guardar el asiento en `pago.asiento` **y** en `movimiento.asiento` (buscar el `MovimientoCajaChica` por `pago.getIdOrigen()`).
  En `revertirContabilidadOrigenExterno`, para este origen: anular el asiento y el movimiento bancario como hoy, y además `movimiento.estado = 2`, `motivoAnulacion = "PAGO REVERSADO: " + motivo`, `asiento = null`.
- `void anularGasto(Long idMovimiento, String motivo, Long idUsuario)`: solo tipo 2 y estado 1; rechazar si `cierre != null` (ya está en un cierre CERRADO); `asientoService.anulaAsiento`; estado 2, `motivoAnulacion`. Para tipos 1/3 el mensaje es "Reverse el pago programado N".
- `List<MovimientoCajaChica> listar(Long idCaja, LocalDate desde, LocalDate hasta, Long tipo, Long estado)` ordenado por fecha, código.
- Adjuntos: `PathCajaChicaRest` CRUD estándar + `GET /ptch/porMovimiento/{id}`. La subida del archivo la hace el FE con `FileRest /file/upload/custom` (`uploadPath = "caja-chica/{idCaja}/{idMovimiento}"`) y luego `POST /ptch`. Al borrar un `PathCajaChica` borrar también el archivo físico con `fileService.deleteFile(path)`.

### T4. `CierreCajaChicaService`
- `CierreCajaChica prepararCierre(Long idCaja, LocalDate fecha, Long idUsuario)`: rechaza si existe un cierre BORRADOR de la caja; `fechaInicio` = día siguiente al `fechaFin` del último CERRADO (o fecha del primer movimiento); `fechaFin = fecha`; calcula `saldoInicial` (saldo hasta fechaInicio−1), `totalGastos`, `totalReposiciones` (tipos 1+3), `totalAjustes` (4−5), `saldoLibros`; estado BORRADOR. Devuelve el cierre con la lista de movimientos del periodo (`Map` `{cierre, movimientos}`).
- `CierreCajaChica confirmarCierre(Long idCierre, Double saldoFisico, String observacion, Long idPlanCuentaDiferencia, Long idUsuario)`: `diferencia = saldoFisico − saldoLibros`. Si `diferencia != 0`: `idPlanCuentaDiferencia` obligatorio; crea un movimiento tipo 4 (sobrante) o 5 (faltante) por `|diferencia|`, `descripcion = "AJUSTE POR ARQUEO " + fecha`, y su asiento `generarAsientoAjusteCajaChica`: sobrante → DEBE caja / HABER cuenta diferencia; faltante → DEBE cuenta diferencia / HABER caja; tipo `EGRESO_TESORERIA`; guarda el asiento en el cierre y en el movimiento. Marca `cierre` en todos los movimientos activos del periodo (incluido el ajuste). Estado CERRADO.
- `void anularCierre(Long idCierre, String motivo, Long idUsuario)`: solo el último cierre CERRADO de la caja; desmarca `cierre` de los movimientos; si hubo ajuste, anula su asiento y el movimiento; estado ANULADO; guarda motivo en observación.
- `List<CierreCajaChica> listar(Long idCaja)`.

### T5. REST
- `CajaChicaRest` (`cjch`): CRUD + `POST /registrar` (body `{cajaChica:{...}, saldoInicialMigrado, idUsuario}`), `GET /saldo/{id}`, `GET /saldos/{idEmpresa}`, `GET /activas/{idEmpresa}`.
- `MovimientoCajaChicaRest` (`mvch`): CRUD + `POST /gasto` (body con los parámetros de `registrarGasto`, fecha `yyyy-MM-dd`), `POST /reposicion`, `POST /apertura`, `POST /anular/{id}` (`{motivo, idUsuario}`), `GET /listar?idCaja&desde&hasta&tipo&estado`.
- `CierreCajaChicaRest` (`crch`): CRUD + `POST /preparar` (`{idCaja, fecha, idUsuario}`), `POST /confirmar/{id}` (`{saldoFisico, observacion, idPlanCuentaDiferencia, idUsuario}`), `POST /anular/{id}`, `GET /listar/{idCaja}`, `GET /movimientos/{idCierre}`.
- `PathCajaChicaRest` (`ptch`): CRUD + `GET /porMovimiento/{idMovimiento}`.
Estilo de trazas y errores según `CLAUDE.md`.

### T6. Documentación
Crear `docs/logica-negocio/tsr/CAJA-CHICA.md`: modelo, reglas, asientos (con glosas exactas), endpoints con ejemplos, y el procedimiento de migración desde las cuentas bancarias 428/429 (crear caja con `saldoInicialMigrado` = saldo contable de 10029/10033; luego el usuario ejecuta el bloque 6 del DDL).

## Restricciones
- No compilar con mvn; revisar imports/firmas. No crear cuentas bancarias ni bancos ficticios. No tocar las tablas de cajas de cobro (CJAA/CJCN/CRCJ...).
- Mantener la firma vieja de `registrarPagoDeOrigenExterno` funcionando (sobrecarga).
- Entregar: archivos creados/modificados, endpoints con ejemplos, y dudas.
