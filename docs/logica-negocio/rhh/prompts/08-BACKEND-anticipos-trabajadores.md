# PROMPT 08 — AGENTE BACKEND — Anticipos a trabajadores: la entrega del anticipo

**Agente:** BACKEND (`C:\work\saaBE\v1\saaBE`). **No tocar el frontend.**
**Prerrequisito:** el usuario ejecutó `docs/logica-negocio/rhh/sql/01-anticipo-empleado.sql` en local — tabla `RHH.ANTE` y rubro 234. **Léelo antes de empezar: ahí están todas las columnas, tipos y FK, y hay que mapear exactamente esas.**
**Leer:** `CLAUDE.md`, `docs/estandar/GUIA-MAPEO-TABLA-COMPLETO.md`, y `docs/logica-negocio/tsr/CAJA-CHICA.md` — el flujo de caja chica es el modelo a imitar, porque resolvió el mismo problema (un módulo que necesita que salga dinero del banco sin depender de CxP).

## Contexto: qué existe hoy y qué falta

Tu propio análisis lo dejó claro y lo confirmo:

- **No existe la entrega.** Un "anticipo" hoy es una fila de `RHH.DSRC` (`DescuentoRecurrente`) creada a mano, con `tipoDescuento = RhhTipoDescuentoRecurrente.ANTICIPO_DE_SUELDO (3)`. No hay documento, ni pago, ni asiento de que el dinero salió.
- **El descuento del rol ya funciona**: `ContabilizacionNominaServiceImpl.acumulaRenglon` → `lineaDeDescuento` cae en el `default` y usa `RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS` (línea 14 del rubro 214), resuelta a cuenta real por la plantilla de `RHH.CFNM`.

## Decisiones ya tomadas (no re-preguntar)

1. **Cuenta contable: la misma.** La entrega usa `CUENTAS_POR_COBRAR_EMPLEADOS`, la que ya usa el descuento del rol. Así el ciclo cuadra solo: la entrega **debita** y el descuento del rol **acredita** la misma cuenta, y el saldo de esa cuenta es "lo que los empleados deben". No crear una línea nueva en el rubro 214.
2. **Beneficiario: desde `Empleado`.** `Empleado` no tiene FK a `Titular`, así que se arma el beneficiario ocasional con `identificacion` y `nombres`, igual que caja chica hizo con su nombre. **No** referenciar `CuentaBancariaEmpleado` en esta fase: la cuenta destino se elige a mano al pagar. (Reutilizar la del sueldo es una mejora posterior y hay que decidir antes qué pasa si el empleado la cambia.)
3. **El `DescuentoRecurrente` se crea al CONFIRMARSE EL PAGO**, no al aprobar. Si el pago se revierte no puede quedar un descuento cobrando un dinero que nunca se entregó.

## Tareas

### T1. Modelo (paquete `com.saa.model.rhh`)
Entidad `AnticipoEmpleado` sobre `RHH.ANTE`, `GenerationType.IDENTITY`, NamedQueries `AnticipoEmpleadoAll`/`AnticipoEmpleadoId`, getters/setters a mano, y la constante en `NombreEntidadesRhh` (o el equivalente del módulo). Campos: `codigo`, `empleado` (`@ManyToOne Empleado`), `fecha` (LocalDate), `valor`, `numeroCuotas`, `valorCuota`, `saldo`, `fechaInicioDescuento`, `motivo`, `observacion`, `estado`, `pagoProgramado` (`@ManyToOne PagoProgramado`), `descuentoRecurrente` (`@ManyToOne DescuentoRecurrente`), `usuarioAprueba`, `fechaAprobacion`, `motivoAnulacion`, `fechaRegistro`, `usuarioRegistro`.

Rubro Java nuevo `com.saa.rubros.EstadoAnticipoEmpleado`: `SOLICITADO=1, APROBADO=2, PAGADO=3, EN_DESCUENTO=4, CANCELADO=5, ANULADO=6`; y en `Rubros`, `RHH_ESTADO_ANTICIPO_EMPLEADO = 234`.
En `OrigenPagoExterno` agregar `RHH_ANTICIPO_EMPLEADO = "RHH_ANTICIPO_EMPLEADO"`.

DAO/DaoImpl/Service/ServiceImpl/Rest siguiendo la guía de mapeo. REST `@Path("ante")`.

### T2. `AnticipoEmpleadoService` — el ciclo
- `AnticipoEmpleado solicitar(Long idEmpleado, Double valor, Integer numeroCuotas, LocalDate fechaInicioDescuento, String motivo, String observacion, Long idUsuario)`: valida empleado activo, `valor > 0`, `numeroCuotas >= 1`; calcula `valorCuota = round(valor / numeroCuotas, 2)` **ajustando la última cuota** para que la suma dé exacto el valor (guarda `valorCuota` y deja que el descuento cobre el residuo al final); `saldo = valor`; estado SOLICITADO.
  Validación de negocio: rechazar si el empleado ya tiene un anticipo en estado SOLICITADO, APROBADO, PAGADO o EN_DESCUENTO (un anticipo vivo a la vez). Mensaje claro con el código del anticipo abierto.
- `Map<String,Object> aprobar(Long idAnticipo, Long idCuentaBancariaOrigen, Long formaPago, boolean debitoAutomatico, String referencia, Long idUsuario)`: solo desde SOLICITADO. Marca `usuarioAprueba`/`fechaAprobacion`, estado APROBADO, y **registra el pago**: `pagoProgramadoService.registrarPagoDeOrigenExterno(OrigenPagoExterno.RHH_ANTICIPO_EMPLEADO, idAnticipo, idCuentaBancariaOrigen, null, valor, fecha, idEmpresa, idUsuario, descripcion, debitoAutomatico, referencia, formaPago)` con `beneficiarioNombre` = nombres del empleado y `beneficiarioIdentificacion` = su identificación. Guarda el pago en el anticipo. Devuelve `{idAnticipo, idPago, estadoPago, numeroCheque}`.
- `void anular(Long idAnticipo, String motivo, Long idUsuario)`: solo SOLICITADO o APROBADO **sin pago confirmado**. Si ya está PAGADO, rechazar indicando que hay que revertir el pago.
- `List<AnticipoEmpleado> listar(Long idEmpresa, Long idEmpleado, Long estado)` y `AnticipoEmpleado consultarPorEmpleado(Long idEmpleado)` para saber si tiene uno vivo.

### T3. Contabilización e integración con el pago
En `PagoProgramadoServiceImpl.contabilizarPagoOrigenExterno`, agregar la rama de `RHH_ANTICIPO_EMPLEADO` **igual que hiciste con `TSR_CAJA_CHICA`** (guarda de salida temprana, sin que CXP importe nada de RRHH más allá de lo imprescindible):

- Asiento nuevo `AsientoContableService.generarAsientoAnticipoEmpleado(idEmpleado, valor, idCuentaBancaria, idEmpresa, fecha, observaciones, usuario)`: **DEBE** la cuenta que resuelve `RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS` por la plantilla de `RHH.CFNM` de esa empresa (reutiliza el mismo resolutor que usa la contabilización de nómina; **no** hardcodees una cuenta) / **HABER** `cuentaBancaria.getPlanCuenta()`. Tipo `TipoAsientos.EGRESO_TESORERIA`, módulo `ModuloSistema.TESORERIA`. Observación: `"Anticipo a colaborador " + nombreEmpleado + " | " + numeroCuotas + " cuotas | Ref: " + ref + " | Valor: $x"`. Si el pago lleva cheque, aplican las reglas de glosa de cheques que ya implementaste.
- Crear el movimiento bancario igual que el resto de `contabilizar*`.
- **Al confirmarse el pago**: anticipo a estado PAGADO y **crear el `DescuentoRecurrente`**: empleado, concepto de nómina de anticipo, `tipoDescuento = ANTICIPO_DE_SUELDO`, `valor` total, `numeroCuotas`, `valorCuota`, `saldoDescuento = valor`, `fechaInicio` = `fechaInicioDescuento` del anticipo, `beneficiario` = nombre del empleado, estado activo. Guardar la FK en el anticipo y pasarlo a EN_DESCUENTO.
- En `revertirContabilidadOrigenExterno`, rama de anticipo: anular el asiento y el movimiento como el resto, **anular el `DescuentoRecurrente` si ya se creó** (solo si no tiene cuotas cobradas: si ya cobró alguna, rechazar con mensaje claro) y devolver el anticipo a APROBADO con el motivo.

### T4. Cierre del ciclo en el rol
Cuando el motor de rol cobra una cuota del `DescuentoRecurrente` de tipo anticipo, hay que bajar `ANTE.ANTESLDD`. Busca dónde se descuenta el saldo del `DescuentoRecurrente` al procesar el rol y, si el descuento tiene un anticipo asociado (consulta inversa por `DSRCCDGO`), resta la cuota del saldo del anticipo; al llegar a cero, estado CANCELADO. **Si esa integración resulta invasiva, no la fuerces**: repórtalo y lo resolvemos con una consulta derivada (el saldo del anticipo se calcula del `DescuentoRecurrente`) en vez de con un campo redundante.

### T5. REST `@Path("ante")`
CRUD estándar + `POST /ante/solicitar`, `POST /ante/aprobar/{id}` (body `{idCuentaBancariaOrigen, formaPago, debitoAutomatico, referencia, idUsuario}`), `POST /ante/anular/{id}` (`{motivo, idUsuario}`), `GET /ante/listar?idEmpresa&idEmpleado&estado`, `GET /ante/vigente/{idEmpleado}`. Estilo de trazas y errores según `CLAUDE.md`.

### T6. Documentación
`docs/logica-negocio/rhh/ANTICIPOS-TRABAJADORES.md`: el ciclo completo, la decisión de la cuenta compartida y por qué el ciclo cuadra solo, los estados, los endpoints con ejemplos, y qué pasa al revertir el pago.

## Restricciones
- No compilar con mvn. No tocar el motor de nómina más allá de lo que pide T4.
- **No dupliques la lógica de pagos**: reutiliza `registrarPagoDeOrigenExterno` tal como hizo caja chica.
- Entrega: archivos, endpoints con ejemplos, confirmación punto por punto y lo que hayas dejado fuera con su razón.
