# PLAN DE SOLUCIÓN — Listado de solicitudes del 2026-08-27

> ## ✅ ESTADO AL 2026-08-29 — 12 de 15 cerrados
>
> **Cerrados** (detalle en `ESTADO-CXP-CXC-TSR-RHH-SRI.md`): 3, 4, 5, 6, 7, 8, 9, 10a, 11, 12,
> 13 y 14. Los tres grandes —el rediseño de pagos (14), el ATS con sus formularios (13) y la
> conciliación con partidas en tránsito (12)— quedaron completos.
>
> **Siguen pendientes, sin empezar:**
>
> | # | Solicitud | Por qué sigue abierta |
> |---|---|---|
> | **1** | Consulta de cobros CxC con anulación | El reverso (`/aplc/revertir/{id}`) existe desde antes; falta el endpoint de listado y la pantalla de consulta |
> | **2** | Mensaje "el titular ya existe" | Sigue reventando con `ORA-00001` crudo. El plan de §1.4 sigue siendo válido tal cual |
> | **10b** | Novedades del período: campos | Sigue esperando que el usuario aclare **qué campo concreto** echan de menos — `NVNM` nunca tuvo columna de días |
>
> Nada de lo cerrado está desplegado todavía. Lo de abajo es el plan original del 27, conservado
> como referencia de por qué se decidió cada cosa.

**Rol:** análisis de impacto y plan de trabajo por el orquestador. Esquema: orquestador (analiza, DDL, prompts) → agente BACKEND → agente FRONTEND → usuario (DDL, compila, despliega, prueba).
**Complementa a:** `tsr/PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md` (fases D, A, B, C ya en curso).
**Base de la verificación:** código de `saaBE`/`saaFE` y la BD local, que es copia de producción. Lo marcado *(por verificar)* necesita una revisión más profunda que no pude hacer hoy.

---

## 0. Resumen: los 15 puntos, agrupados por estado real

| # | Solicitud | Estado real hoy | Tamaño | Bloque |
|---|---|---|---|---|
| 3 | Estado de cuenta cliente/proveedor, anticipos y estados | **✅ HECHO y probado** (fase D) | — | — |
| 4 | Pagos con cheque en facturas, anticipos, egresos | **✅ HECHO y probado** (fase A) | — | — |
| 6 | Caja chica | BE ✅ · FE ✅ · **prueba bloqueada por un `.trim()`** (fix en curso) | S | 1 |
| 5 | Emisión de liquidaciones de compra | BE ✅ + correcciones en curso · FE pendiente (prompt 07) · `.jasper` ✅ | M | 1 |
| 9 | Consulta y anulación de anticipos de cliente, ingresos y egresos | **Ya existe todo** en BE y FE (`/antc/anular`, `/ingr/anular`, `/egrs/anular`, pestañas de consulta). Solo falta **probarlo** | XS | 1 |
| 2 | Mensaje "el titular ya existe" | Hay `UNIQUE UK_TTLR_IDNT_ESTD` en BD: hoy revienta con `ORA-00001` sin mensaje útil | XS | 1 |
| 1 | Consulta de cobros CxC y anulación | Registro de cobro y **reverso** (`/aplc/revertir/{id}`) existen; **no hay pantalla de consulta** ni botón de anular | M | 2 |
| 7 | Probar carga automática de documentos CxP | **Implementada y probada en vivo el 2026-08-23** (11/11 documentos). Quedan 5 caminos sin ejercitar, listados en `cxp/PRODUCCION-CARGA-AUTOMATICA-SRI.md §7` | S (prueba guiada) | 2 |
| 8 | Reembolsos de gastos: XML con `<reembolsos>` o carga manual | **Implementado**: BE detecta `<reembolsoDetalle>`, tabla `PGS.RMBF` existe, `OrigenReembolso.MANUAL=2`, pantalla `cxp/procesos/reembolsos-factura`. Falta **probar los dos caminos** | S (prueba) | 2 |
| 10b | Novedades del período: recuperar campos | La pantalla hoy captura `cantidad`, `descripcion` y `valor` (rediseño en commit `47b5d06`). La entidad `NVNM` solo tiene esas columnas — **nunca hubo "días"** como columna. *(por verificar qué campo concreto echan de menos)* | S | 2 |
| 12 | Conciliación bancaria completa | **N:M ya funciona completo** (BE y FE) — era el corazón del pedido. Pero **el mes no puede cerrarse si hay una partida en tránsito**: candado estructural verificado. Dos pantallas del menú son maquetas con datos falsos | **L** | 3 |
| 10a | Vacaciones: registro, cálculo de días, novedad automática, provisión | `SolicitudVacaciones` y `SaldoVacaciones` existen, pero **el servicio es CRUD puro**: no hay aprobación, no descuenta saldo, no crea la novedad, no baja la provisión. El FE tiene el diálogo de aprobación **contra un endpoint que no existe** | L | 3 |
| 11 | Anticipos a trabajadores: entrega + descuento en cuotas + pago | **No existe la entrega**. Solo el descuento vía `DescuentoRecurrente` y el concepto `ANTICIPO_DE_SUELDO=14`. RRHH no genera pagos programados | L | 3 |
| 14 | Rediseño de pagos: solicitud sin cuenta, aprobación con cuenta + saldo + lote multi-módulo + cheque | Hoy la cuenta es obligatoria al registrar (`PGTRCNBC NOT NULL`), el lote es por cuenta, `obtieneSaldoFecha` existe, solo CRD y caja chica generan pagos externos. Es un **rediseño del circuito** | XL | 4 |
| 13 | ATS, 103 y 104 | **No existe nada**. Los datos sí (RTV2 con códigos, FCTC con IVA, FCTR) | XL | 4 |

Tamaños: XS < ½ día · S 1 día · M 2-3 días · L 1 semana · XL 2+ semanas (trabajo de los agentes + revisión + prueba).

---

## 1. BLOQUE 1 — Cerrar lo que está a un paso (esta semana)

### 1.1 Caja chica (6) — probar y documentar
Pendiente el fix del `.trim()` en `cajas-chicas.component.ts` (prompt ya entregado). Luego: prueba completa → crear caja → apertura por cheque → gasto con adjunto → alerta → reposición → cierre con diferencia → manual `tsr/manuales/CAJA-CHICA.md`.
**Decisión ya tomada:** custodio = titular o colaborador de RRHH (no rol proveedor). Entra en el mismo prompt de corrección.

### 1.2 Liquidaciones de compra (5)
BE: correcciones entregadas (Locale.US, estado anulado = 0, formas de pago en LQCC). FE: prompt 07, ya actualizado con la decisión de **catálogo de producto de CxP** y las rutas REST reales. `.jasper` ya compilado. Luego prueba en el sandbox del SRI.

### 1.3 Anulaciones de anticipos de cliente, ingresos y egresos (9) — solo probar
Todo existe. Casos a probar: anticipo de cliente confirmado con y sin cruces (`/antc/verificarAnulacion` debe avisar), ingreso contabilizado (revierte asiento y movimiento bancario), egreso pagado (debe rechazar: "reverse el pago") y egreso pendiente (anula). Documentar en `tsr/manuales/`.

### 1.4 Titular duplicado (2)
**Causa:** `TSR.TTLR` tiene `UNIQUE (TTLRIDNT, TTLRESTD)`; `TitularServiceImpl.saveSingle` no valida antes y el `ORA-00001` llega crudo. `validaIdentificacion` solo valida el formato.
**Prompt BACKEND (XS):** en `saveSingle`, si es alta (`codigo == null`), buscar por identificación + estado activo y lanzar `IncomeException("Ya existe un titular activo con la identificación X: <nombre>")`; además capturar `ORA-00001` en el REST como red de seguridad. Devolver el titular existente en la respuesta para que el FE ofrezca "usar el existente".
**Prompt FRONTEND (XS):** mostrar el mensaje y ofrecer cargar el titular existente (recordar: no hay tabla de proveedores, es un titular al que se le asigna rol).

---

## 2. BLOQUE 2 — Consultas y pruebas de lo implementado (semana siguiente)

### 2.1 Consulta de cobros CxC con anulación (1)
**Existe:** registro de cobro (`cxc/forms/cobros/registrar-cobro` → `POST /aplc/cobroTransferencia`), aplicación de anticipos y retenciones, y `POST /aplc/revertir/{id}` que devuelve el saldo a la factura y anula asiento y movimiento.
**Falta:** (BE, S) endpoint `GET /aplc/listar?idEmpresa&idTitular&desde&hasta&tipo&estado` que devuelva las aplicaciones con factura, titular, forma de pago, valor, asiento y estado; (FE, M) pantalla *CxC → Cobros → Consulta* con filtros, detalle por factura y botón **Anular** → `/aplc/revertir/{id}` con motivo. Reutilizar el patrón de consultas de cheques.

### 2.2 Carga automática de documentos CxP (7) — prueba guiada
Ya probada en vivo. Plan de prueba: una carga pequeña del mes en curso (la ventana del SRI es de un mes), revisar contadores, registrar por lote, y ejercitar a propósito los cinco caminos sin cubrir (`FUERA_VENTANA`, segunda corrida, lote completo, pre-marca de reembolso, `NO_ENCONTRADO`). Yo hago la pasada en el navegador y documento en `cxp/manuales/`.

### 2.3 Reembolsos de gastos (8) — prueba de los dos caminos
Implementado según `cxp/CAMBIO-REEMBOLSO-GASTOS-BACKEND.md` (con los ajustes del §13). Probar: (a) XML con `<reembolsos>` → los sustentos se graban solos en `RMBF`; (b) XML sin el bloque → la factura queda marcada y la pantalla `reembolsos-factura` permite cargar el detalle a mano (`origen = MANUAL`). Verificar que el asiento use las cuentas de los grupos de sustento. Manual en `cxp/manuales/`.

### 2.4 Novedades del período (10b) — aclarar y reponer
La pantalla hoy captura **cantidad, descripción y valor**; la entidad `NVNM` no tiene más columnas de captura. *(por verificar)*: ¿el campo que falta es "días" para vacaciones? Si es así, **no era una columna**: era `cantidad` con etiqueta según el concepto (días para vacaciones, horas para extras, unidades para el resto). Propuesta: que la etiqueta y la validación de `cantidad` dependan del concepto, y que para conceptos "por días" el valor se calcule (valor día × cantidad). Pregunta al usuario antes de prompt.

---

## 3. BLOQUE 3 — Funcionalidad nueva de tamaño medio/grande

### 3.1 Conciliación bancaria (12) — VERIFICADO A FONDO 2026-08-27

**Lo que YA funciona y no hay que construir:**
- **N:M completo**, backend y frontend. `conciliarGrupo(idCuenta, idPeriodo, List<Long> idsExtracto, List<Long> idsAsiento, usuario)` acepta N contra M, valida el cuadre por suma agregada con tolerancia 0,01 y crea N filas `GCEX` + M filas `GCAS` sobre el mismo grupo. La pantalla permite marcar varios de cada lado con checkbox y muestra suma extracto / suma asiento / diferencia en vivo. **Era el corazon del pedido y ya esta.**
- Importacion de extracto con 11 parsers por banco, deteccion de archivo duplicado por hash, sugerencias automaticas 1:1, N:1 y 1:N, deshacer grupo, cerrar y reabrir mes.
- La pantalla viva es `conciliacion-contable`.

**El bloqueante real — las partidas en transito no se pueden conciliar NI dejar pendientes:**
- `GrupoConciliacionExtractoDaoServiceImpl.selectPendientes` filtra `d.periodo.codigo = :idPeriodo` y el de asientos filtra `fechaAsiento between primerDia and ultimoDia`. Un deposito registrado en libros el 30/abr y acreditado por el banco el 02/may **nunca puede conciliarse**: cada lado aparece en un periodo distinto y no hay pantalla donde coexistan.
- Y `verificar` exige **cero** pendientes de ambos lados, y `cerrarMes` exige todas las cuentas verificadas: con una sola partida en transito **el mes no cierra nunca**. Candado duro.
- `TipoMovimientoConciliacion` (DEPOSITO_EN_TRANSITO, CHEQUES_GIRADOS_Y_NO_COBRADOS, …) se sigue escribiendo al originar los movimientos, pero **quien lo consumia era el motor legado**, que no tiene REST desde hace tiempo. `TSR.MVCB` acumula movimientos en transito que nunca se cierran.
- El cuadre clasico completo (saldo libros vs saldo banco, con deposito en transito, cheques girados, ND/NC no registradas) **ya esta modelado** en la entidad legado `Conciliacion` y calculado en `ConciliacionServiceImpl.insertaConciliacion`. Sin REST ni pantalla. El diseno correcto se penso una vez y quedo huerfano.
- `resumenPorPeriodo` devuelve solo contadores: ningun importe, ningun saldo, ninguna diferencia.

**Riesgo inmediato:** las pantallas `conciliacion` y `consulta-conciliacion` son **maquetas con datos inventados** (`Banco A`, `MOV-001`) y siguen en el menu y en las rutas. Un usuario de produccion entra y ve datos falsos. Quitarlas del menu es XS y urgente.

**Huecos por tamano:** arrastre de pendientes entre periodos (L, bloqueante) · marcar partida en transito y excluirla del bloqueo de verificar (M) · `verificar` que solo exija cero pendientes *sin clasificar* (XS) · cuadre real con saldos e importes en `resumenPorPeriodo` (M) · desglose de la diferencia, portando la formula del motor legado (M) · acta de conciliacion imprimible (M) · sugerencias por referencia y numero de cheque, hoy solo monto y fecha (S) · `estadoRevision` del detalle nunca se actualiza (XS) · `CON_DIFERENCIAS` se pinta pero nunca se asigna (XS) · `deshacerGrupo` sin auditoria (XS) · decidir que hacer con `MovimientoBanco` en transito (L, decidir antes de lo demas).

**Tamano revisado: L** (no M). Orden: decidir MovimientoBanco -> arrastre -> marcar transito -> verificar -> cuadre -> desglose -> acta.

### 3.2 Vacaciones (10a) — L
**Diagnóstico:** `SolicitudVacacionesServiceImpl` es CRUD (`save/remove/select`), sin lógica. `SaldoVacaciones` tiene lo necesario (`asignados, usados, pendientes, díasDerecho, díasRestantes, díasPagados, valorDía`). El FE tiene `vacaciones-form` (guarda por `POST /slct`, CRUD) y `vacaciones-aprobacion-dialog`, pero no hay endpoint de aprobación. *(por verificar)* la causa exacta del "no funciona el registro": probablemente un campo del payload que el CRUD no acepta o la ausencia del saldo del año.
**Diseño propuesto** (regla ecuatoriana: 15 días por año cumplido, acumulables hasta 3 años, pagados a razón de 1/24 de lo percibido en el año):
1. `POST /slct/solicitar`: valida saldo disponible (`/sldv/disponible/{idEmpleado}` ya existe), graba en estado SOLICITADA.
2. `POST /slct/aprobar/{id}`: descuenta del `SaldoVacaciones` del año más antiguo con saldo, y **genera automáticamente la `NovedadNomina`** del período que contiene las fechas, concepto VACACIONES, `cantidad = días`, `valor = valorDía × días`.
3. Al procesar el rol: el colaborador cobra el mes completo (las vacaciones son remuneradas) y se **da de baja la provisión** de vacaciones por el valor gozado (`ProvisionNomina`, concepto vacaciones). *(por verificar)* cómo está modelada la provisión mensual hoy.
4. `POST /slct/anular/{id}`: devuelve el saldo y anula la novedad si el período no está cerrado.
Documentar la regla en `rhh/VACACIONES.md` citando Código del Trabajo art. 69-76.

### 3.3 Anticipos a trabajadores (11) — L
**No existe la entrega.** Diseño: entidad `RHH.ANTE` (AnticipoEmpleado): empleado, fecha, valor, número de cuotas, cuota mensual, saldo, estado (SOLICITADO, APROBADO, PAGADO, EN_DESCUENTO, CANCELADO, ANULADO), pago programado. Flujo: solicitar → aprobar → **`PagoProgramado` de origen externo `RHH_ANTICIPO_EMPLEADO`** (hereda transferencia, débito automático y cheque; asiento DEBE anticipos a empleados / HABER banco) → al confirmarse el pago se crea el `DescuentoRecurrente` con concepto `ANTICIPO_DE_SUELDO` por N cuotas → cada rol descuenta y baja el saldo → al llegar a cero, CANCELADO. DDL nuevo (lo escribe el orquestador). Depende del bloque 4 solo si se quiere que la cuenta se elija al aprobar.

---

## 4. BLOQUE 4 — Rediseños grandes

### 4.1 Pagos multi-módulo con aprobación y cuenta al aprobar (14) — XL
**Hoy:** la cuenta se elige al registrar (`PGTRCNBC NOT NULL`), el lote es por cuenta, y solo CRD (devolución de aportes) y caja chica generan pagos externos. Existen `obtieneSaldoFecha` y `selectSaldoCuentasByFecha` para validar saldo. Las entidades `AprobacionXProposicionPago`, `MontoAprobacion`, `UsuarioXAprobacion` son del circuito legado.
**Diseño por fases:**
- **Fase 1 (DDL + BE):** `PGTRCNBC` pasa a nullable; estado nuevo `POR_APROBAR` antes de `REGISTRADO`; `registrar*` acepta cuenta nula; endpoint `POST /pgtr/aprobar` con `{idsPagos, idCuentaBancaria, formaPago}` que asigna la cuenta, valida saldo (`obtieneSaldoFecha` − pagos aprobados pendientes ≥ suma), gira cheque si `formaPago=3`, y deja los pagos en `REGISTRADO` (listos para lote) o `CONFIRMADO` (cheque/débito). Origen externo `RHH_NOMINA`/`RHH_ANTICIPO` para que RRHH entregue solicitudes.
- **Fase 2 (FE):** pantalla *Aprobación de pagos*: bandeja de solicitudes de todos los módulos (CxP, RRHH, CRD, caja chica), selección múltiple, elegir cuenta, ver saldo disponible vs. total seleccionado, forma de pago, aprobar → genera lote o cheques. Las pantallas de origen dejan de pedir cuenta.
- **Fase 3:** "varios pagos en una sola transferencia": agrupar pagos aprobados del mismo beneficiario y cuenta destino en una línea del archivo del banco (`FormateadorArchivoBanco`).
Incompatibilidad a decidir: los pagos con cheque de la fase A se confirman al registrar; en el rediseño se confirmarían **al aprobar**. Es coherente, pero cambia el flujo recién probado.

### 4.2 ATS, 103 y 104 (13) — XL
Nada existe. Los datos sí: retenciones emitidas RTV2 (códigos de retención, sustento), compras FCTC (base 0, base 12/15, IVA, código de sustento), ventas FCTR, anulados. Diseño: módulo `sri` con (a) generador del **XML del ATS** según la ficha técnica vigente (compras, ventas, anulados, exportaciones), validable contra el XSD del SRI; (b) **reporte 103** (retenciones en la fuente por código de retención, con base y valor); (c) **reporte 104** (ventas por tarifa, compras por tarifa, retenciones de IVA, crédito tributario). Antes de escribir: confirmar con contabilidad las casillas exactas del 103 y 104 que declaran y el catálogo de códigos de sustento. Skill `sri` disponible para la normativa. Requiere iterar contra el validador del SRI.

---

## 5. Orden propuesto y dependencias

```
Bloque 1 ─ caja chica ─ liquidaciones ─ anulaciones (prueba) ─ titular duplicado
Bloque 2 ─ consulta de cobros ─ carga automática (prueba) ─ reembolsos (prueba) ─ novedades
Bloque 3 ─ conciliación (prueba + huecos) ─ vacaciones ─ anticipos a trabajadores
Bloque 4 ─ rediseño de pagos ─ ATS/103/104
```
- Anticipos a trabajadores puede hacerse antes del rediseño de pagos (usa el circuito actual con cuenta al registrar) o después (cuenta al aprobar). Recomiendo **antes**: entrega valor ya y el rediseño lo absorbe.
- El rediseño de pagos toca el mismo `PagoProgramadoServiceImpl` que cheques y caja chica: **no arrancarlo hasta que A, B y C estén en producción**.
- ATS/103/104 es independiente de todo lo demás y se puede paralelizar con un agente BE dedicado cuando haya capacidad.

---

## 6. Preguntas para el usuario

1. **Novedades del período:** ¿qué campo concreto echan de menos? ¿"días" para vacaciones? La entidad solo tuvo `cantidad`, `descripción` y `valor`.
2. **Vacaciones:** ¿qué error exacto da hoy al registrar (mensaje en pantalla o en el log)? Ahorra el diagnóstico.
3. **Rediseño de pagos:** ¿los pagos con cheque deben confirmarse al **aprobar** (nuevo flujo) en vez de al registrar (como quedó en la fase A)?
4. **ATS/103/104:** ¿tienen las casillas del 103 y 104 que declaran hoy (el formulario que llenan a mano)? Con eso el reporte sale calcado.
5. **Conciliación:** ¿un extracto bancario real de un mes para la sesión de prueba?
6. **Plazo:** el listado completo son entre 6 y 8 semanas de trabajo con este equipo. ¿Cuál es el orden de prioridad real para el cliente, además de lo de hoy?

---

## 7. Pendientes del usuario ahora

1. Avisar cuando el FE termine el fix de caja chica → pruebo y documento.
2. Lanzar el prompt 07 (FE liquidaciones) cuando termine el de caja chica.
3. Responder las preguntas del §6 (la 1 y la 2 destraban el bloque 2 y 3 de RRHH).
4. Los agentes exploradores están limitados hasta las 4:20am; las partes *(por verificar)* las reviso a fondo después.
