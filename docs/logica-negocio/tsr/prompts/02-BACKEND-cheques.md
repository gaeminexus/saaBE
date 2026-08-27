# PROMPT 02 — AGENTE BACKEND — Pago con cheque (chequeras + integración con PagoProgramado)

**Agente:** BACKEND (`C:\work\saaBE\v1\saaBE`, Jakarta EE 10 / WildFly / JPA Oracle). **No tocar el frontend.**
**Prerrequisito:** el usuario ya ejecutó `docs/logica-negocio/tsr/sql/01-cheques-pago-programado.sql` en la BD local (columnas `TSR.CNBC.CNBCCHQR`, `PGS.PGTR.PGTRFPAG`, `PGS.PGTR.PGTRDTCH`; detalles 3 y 4 del rubro 38). Léelo antes de empezar.
**Leer primero:** `CLAUDE.md` del repo (capas, convenciones, trazas `System.out.println`, estilo de error), `docs/logica-negocio/tsr/PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md` §2.

## Lo que ya existe (usar, no duplicar)
- Entidades `com.saa.model.tsr.Chequera` (TSR.CHQR) y `Cheque` (TSR.DTCH) con DAO/Service/REST CRUD (`ChequeraRest @Path("chqr")`, `ChequeRest @Path("dtch")`).
- Rubros Java: `com.saa.rubros.EstadoCheque` (ACTIVO=1, ANULADO=2, GENERADO=3, IMPRESO=4, DANIADO=5, ENTREGADO=6), `EstadoChequera` (ACTIVA=1, INACTIVA=2, SOLICITADA=3, TERMINADA=4, PERDIDA=5, ANULADA=6), `MotivoAnulacionCheque` (ERROR_DE_TIPEO=1, ERROR_DE_USUARIO=2, CHEQUERA_ANULADA=3) — **agregar `PAGO_REVERSADO = 4`**.
- `ejb/tsr/serviceImpl/ChequeServiceImpl.java` tiene lógica legada (`procesoImpresionCheques`, etc.) que **no se usa ni se debe reutilizar**. Agregar métodos nuevos; no borrar los viejos.
- `ejb/cxp/serviceImpl/PagoProgramadoServiceImpl.java`: orquestador de todos los pagos (`registrarPago` l.~169, `registrarPagoDeEgreso` l.~279, `registrarPagoDeAnticipo` l.~389, `registrarPagoDeOrigenExterno` l.~500, `generarLote` l.~709, `procesarRespuestaBanco` l.~834, `confirmarPagosManual` l.~944, `revertirPagoConfirmado` l.~1110). El switch contable está en dos sitios (l.~886-902 y l.~995-1007): `origenExterno → contabilizarPagoOrigenExterno`, `anticipo → contabilizarPagoAnticipo`, `egreso → contabilizarPagoEgreso`, `else → aplicacionPagoCxpService.aplicarPagoTransferencia`.
- Movimiento bancario: `MovimientoBanco` (TSR.MVCB) ya tiene `cheque` (`DTCHCDGO`) y `numeroCheque` (`MVCBCHQN`). `TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS = 2`.
- Catálogo de forma de pago vigente: `1=Efectivo, 2=Transferencia, 3=Cheque, 4=Débito automático` (constantes `FORMA_PAGO_*` en `AplicacionPagoCxpServiceImpl`). **No usar** `com.saa.rubros.TipoFormaPago` (está mal y no se usa).

## Tareas

### T1. Entidades
1. `CuentaBancaria`: agregar `@Column(name = "CNBCCHQR") private Long manejaChequera;` con getter/setter.
2. `PagoProgramado`: agregar `@Column(name = "PGTRFPAG") private Long formaPago;` y `@ManyToOne @JoinColumn(name = "PGTRDTCH", referencedColumnName = "DTCHCDGO") private Cheque cheque;`. Actualizar `obtieneCampos()` de `PagoProgramadoDaoServiceImpl` con `formaPago`, `cheque`.
3. Crear `com.saa.rubros.FormaPagoProgramado` (interface) con `EFECTIVO=1L, TRANSFERENCIA=2L, CHEQUE=3L, DEBITO_AUTOMATICO=4L` y usarla en el código nuevo. Reemplazar las constantes locales de `AplicacionPagoCxpServiceImpl` por esta interface (mismos valores).
4. Corregir `ChequeDaoServiceImpl.obtieneCampos()`: cambiar `"persona"` por `"titular"`.

### T2. Chequeras — `ChequeraService`/`ChequeraServiceImpl` (nuevos métodos, `throws Throwable`)
- `Long sugerirNumeroInicial(Long idCuentaBancaria)`: `max(finaliza)+1` de las chequeras de la cuenta con estado ≠ ANULADA; si no hay, `1`. Query nueva en `ChequeraDaoService`.
- `Chequera registrarRecepcion(Long idCuentaBancaria, Long comienza, Long finaliza, LocalDateTime fechaEntrega, Long idUsuario)` (`@TransactionAttribute(REQUIRED)`): valida cuenta existe, `manejaChequera == 1`, `comienza >= 1`, `finaliza >= comienza`, sin solape con chequeras no anuladas de la misma cuenta (query `existeSolape(idCuenta, comienza, finaliza)`). Graba `Chequera` (numeroCheques = finaliza−comienza+1, fechaSolicitud = ahora si null, estado ACTIVA en `rubroEstadoChequeraP = Rubros.ESTADO_CHEQUERA`/`rubroEstadoChequeraH = EstadoChequera.ACTIVA` — seguir el patrón P/H que use `ChequeServiceImpl.crearChequesDeChequera`). Crea un `Cheque` por número con estado ACTIVO y `chequera` asignada (puede reutilizar `crearChequesDeChequera` si funciona; verificar que setee P/H igual).
- `Map<String,Object> resumen(Long idChequera)`: `{comienza, finaliza, total, disponibles, generados, impresos, entregados, anulados, siguiente}` con una sola query agrupada por estado.
- `void anularChequera(Long idChequera, String motivo, Long idUsuario)`: solo si no tiene cheques en GENERADO/IMPRESO/ENTREGADO; cheques ACTIVO → ANULADO con motivo 3 y `fechaAnulacion`; chequera → ANULADA.
- Regla automática: método `void cerrarSiTerminada(Long idChequera)` que pasa la chequera a TERMINADA cuando no quedan cheques ACTIVO; llamarlo cada vez que se consume un cheque.

### T3. Cheques — `ChequeService`/`ChequeServiceImpl` (nuevos métodos)
- `Cheque siguienteDisponible(Long idCuentaBancaria)`: cheque ACTIVO de menor `numero` entre las chequeras ACTIVAS de la cuenta; `IncomeException("La cuenta no tiene cheques disponibles")` si no hay.
- `Cheque asignarAPago(Long idCuentaBancaria, Double valor, Titular titular, String beneficiario, Long idUsuario)`: toma `siguienteDisponible`, setea `valor, titular, beneficiario, fechaUso = ahora`, estado GENERADO; llama `cerrarSiTerminada`. Devuelve el cheque.
- `void anularChequeSuelto(Long idCheque, Long motivo, Long idUsuario)`: solo si estado ACTIVO; → ANULADO, `rubroMotivoAnulacionH = motivo`, `fechaAnulacion`. Si el cheque tiene un `PagoProgramado` asociado (query por `pago.cheque.codigo`), rechazar con mensaje "El cheque está asociado al pago N; reverse el pago".
- `void anularPorReverso(Long idCheque)`: → ANULADO, motivo `PAGO_REVERSADO (4)`, `fechaAnulacion`.
- `void marcarImpresos(List<Long> ids, Long idUsuario)`: GENERADO → IMPRESO + `fechaImpresion`. `void marcarEntregados(List<Long> ids, Long idUsuario)`: IMPRESO → ENTREGADO + `fechaEntrega`. Cualquier id en otro estado aborta toda la operación con `IncomeException` que indique el número.
- `List<Map<String,Object>> listar(Long idEmpresa, Long idCuentaBancaria, Long estado, LocalDate desde, LocalDate hasta)`: devuelve `{idCheque, numero, estado, valor, beneficiario, fechaUso, fechaImpresion, fechaEntrega, idPago, tipoPago ("FACTURA"/"EGRESO"/"ANTICIPO"/"EXTERNO"), referenciaPago, numeroCuenta, banco}`; el join con `PagoProgramado` es por `pago.cheque`. Filtros opcionales (null = sin filtro). La empresa se filtra por `pago.empresa` cuando hay pago; si no, por `chequera.cuentaBancaria` (CNBC no tiene empresa: no filtrar por empresa cuando no hay pago).

### T4. Integración en `PagoProgramadoServiceImpl`
1. Agregar el parámetro `Long formaPago` a los cuatro `registrar*` (mantener las firmas viejas delegando con `formaPago = debitoAutomatico ? 4 : 2` para no romper llamadores como `EgresoServiceImpl` y `AnticipoProveedorServiceImpl`; luego actualizar esos llamadores para que pasen el `formaPago` que reciban del REST).
2. Validación común (método privado `validarFormaPago(cuentaOrigen, formaPago, debitoAutomatico, idCuentaDestino)`): `formaPago ∈ {2,3,4}`; si 3 → `cuentaOrigen.manejaChequera == 1` y `debitoAutomatico == false`, y **no** exigir cuenta destino; si 4 → `debitoAutomatico` true; si 2 → como hoy.
3. Cuando `formaPago == 3`, en el mismo `registrar*` y dentro de la misma transacción: `cheque = chequeService.asignarAPago(idCuentaOrigen, valor, titularBeneficiario, nombreBeneficiario, idUsuario)`; `pago.setCheque(cheque); pago.setFormaPago(3L); pago.setEstado(CONFIRMADO); pago.setFechaRespuesta(ahora); pago.setReferenciaBanco("CHQ-" + cheque.getNumero())`; y ejecutar **el mismo switch contable** que hoy usa la rama de débito automático (extraer el switch a un método privado `contabilizarSegunOrigen(pago, idUsuario)` y llamarlo desde los tres sitios: registro con débito automático, registro con cheque, `procesarRespuestaBanco`, `confirmarPagosManual`). El beneficiario: factura → titular de la factura; egreso → `egreso.getTitular()` si existe, si no `beneficiarioNombre` del pago; anticipo → titular del anticipo; origen externo → `beneficiarioNombre`.
4. Glosa: en `AplicacionPagoCxpServiceImpl.aplicarPagoTransferencia`, `contabilizarPagoEgreso`, `contabilizarPagoAnticipo` y `contabilizarPagoOrigenExterno`, si `pago.getCheque() != null`: (a) el texto `tipoTexto`/sufijo pasa a ser `"cheque"` en lugar de `"transferencia"`; (b) anexar a la observación de cabecera ` | Cheque N° {numero} Cta {cuenta.numeroCuenta}`; (c) en la descripción de la línea HABER anexar ` | Cheque N° {numero}`. Para el anticipo, que hoy no recibe observación de cabecera, pasar la observación por el método `generarAsientoAnticipoProveedor` agregando un parámetro `String observaciones` (sobrecarga; la firma vieja delega con null).
5. Movimiento bancario: en los cuatro `contabilizar*`, si hay cheque: `mov.setCheque(cheque); mov.setNumeroCheque(cheque.getNumero());` y tipo `TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS` en lugar de `TRANSFERENCIAS_DEBITOS_EN_TRANSITO`. Descripción: reemplazar `" | Ref: "` por `" | Cheque N° {n}"`.
6. `generarLote`: rechazar pagos con `formaPago == 3` con el mismo mensaje que los de débito automático.
7. `revertirPagoConfirmado`: tras la reversión contable existente, si `pago.getCheque() != null` → `chequeService.anularPorReverso(idCheque)` y `pago.setEstado(ANULADO)` (no RECHAZADO). No desasociar el cheque del pago (queda como histórico).
8. `anularPago` (pagos no confirmados): un pago con cheque nunca está en REGISTRADO, así que no aplica; dejar una validación defensiva.

### T5. REST
`ChequeraRest` (`chqr`): `GET /sugerirInicio/{idCuenta}`, `POST /registrarRecepcion` (body JSON `{idCuentaBancaria, comienza, finaliza, fechaEntrega:"yyyy-MM-ddTHH:mm:ss", idUsuario}`), `GET /resumen/{idChequera}`, `POST /anular/{id}` (body `{motivo, idUsuario}`), `GET /porCuenta/{idCuenta}`.
`ChequeRest` (`dtch`): `GET /siguiente/{idCuenta}` (devuelve `{idCheque, numero}` o 404 con `{mensaje}`), `GET /listar?idEmpresa&idCuenta&estado&desde&hasta`, `POST /anular/{id}` (body `{motivo, idUsuario}`), `POST /imprimir` (body `{ids:[...], idUsuario}`), `POST /entregar` (body `{ids:[...], idUsuario}`).
`PagoProgramadoRest`, `EgresoRest /procesar`, `AnticipoProveedorRest /procesar`: aceptar `formaPago` (Long, opcional; default 2 o 4 según `debitoAutomatico`). La respuesta de registro debe incluir `numeroCheque` cuando aplique.
Estilo: trazas `System.out.println` al inicio, `catch (Throwable)` → 500 con `"Error ...: " + e.getMessage()` (el filtro `MensajeErrorJsonFilter` lo envuelve en `{mensaje}`).

### T6. Documentación
Actualizar `docs/logica-negocio/pagos/INGRESOS-EGRESOS-TESORERIA.md` y `ENDPOINTS-FRONTEND-PAGOS-COBROS.md` con `formaPago`, los endpoints nuevos y las glosas. Crear `docs/logica-negocio/tsr/CHEQUES.md` con el ciclo de estados, reglas y endpoints (breve, verificado contra lo que implementaste).

## Restricciones
- No compilar con mvn (no está en el PATH); el usuario compila en Eclipse. Revisar imports y firmas con cuidado.
- No cambiar glosas ni comportamiento de los pagos por transferencia/débito automático existentes.
- No tocar `TSR.PGSS`/`TempPago` ni el circuito legado.
- Entregar al final: lista de archivos creados/modificados, endpoints nuevos con ejemplo de body y respuesta, y dudas que hayan quedado.
