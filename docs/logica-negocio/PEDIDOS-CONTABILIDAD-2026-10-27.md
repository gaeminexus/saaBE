# PEDIDOS DE CONTABILIDAD — barrido de octubre 2026 (correos de Ana, Katherine y el área de contabilidad)

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-27 por `omen-saa-2-arb`, antes de despachar (regla 7).
**Fuente:** 27 hilos de octubre de `ana.lalangui@`, `katherine.pardo@` y `contabilidad@` (Steven), leídos con sus
capturas. Sigue a `PEDIDOS-CONTABILIDAD-2026-10-12.md` (P1–P7). Cada pedido se contrastó con `main` el 2026-10-27.

## 1. Estado de cada pedido

| # | Pedido | Quién / fecha | Alcance | Estado al 27-10 |
|---|---|---|---|---|
| Q1 | NC de compra que excede el saldo de su factura | Katherine 27-10 | cxp | ✅ código en `main` (`b170ee2b`, FE `ca3f269`); falta desplegar |
| Q2 | Anular un documento que el emisor anuló en el SRI (1294 MRFOX) | Ana 22-10 | cxp | ✅ botón en `main` (`a4586614`, FE `90c1896`); 1294 cerrada con `e2-108` + `e2-110` |
| Q3 | **El asiento de la factura registrada desde la bandeja sale con usuario SISTEMA** | Katherine 26-10 | cxp | 🔴 **despachar** — §2.1 |
| Q4 | **El concepto del gasto de caja chica no sale en el asiento impreso** | Ana 22-10 | tsr/cxp | 🔴 **despachar** — §2.2 |
| Q5 | **Imprimir el asiento desde el registro de cobro** (y «de todos los módulos») | Ana 20-10 y 22-10 | cxc (y otros) | 🔴 **despachar** — §2.3. Ya tienen botón: registro de ingreso, egreso, devolución de saldo a favor, listado de asientos (`d45b670`) |
| Q6 | Desde el **01-11**, Ingreso/Egreso solo si interviene BANCOS (= P5) | Ana 06-10, recordado 22-10 | cnt + todos | 🔴 **bloqueado**: el `e2-99` sigue sin correr y hay 4 preguntas abiertas (§3). **Fecha límite: 01-11** |
| Q7 | Detalle del ATS sigue mostrando «factura» en notas de venta | Ana 22-10 | sri/cxc FE | 🟡 código en `main` desde el 12-10 (FE `2a640c9`). O no está desplegado, o las compras están grabadas con `01`: lo dice el `e2-100`, sin correr |
| Q8 | Estados financieros en Excel (Jimena, = P4) | Jimena 08-10, Ana 22-10 | cnt FE | 🟡 código en `main` (`760b167`, `660dc54`); falta desplegar, comparar contra el PDF y **contestarle a Jimena** |
| Q9 | Mejorar el formato de impresión de las conciliaciones | Ana 06-10 (no hay correo con el detalle) | tsr | ⚪ **sin especificación**: hace falta saber qué cambiar |
| Q10 | Liquidación de ex-colaborador: buscar dentro del combo «tipo» y que el tipo traiga la cuenta contable | Ana 22-10 | rhh | ⛔ **pantalla de `omen-saa-3`** (`RHH.LQEX`). Se avisa con autorización del usuario |
| Q11 | ATS septiembre: retenciones 332 al 0% que el validador rechaza | Ana 15-10 | sri | 🟡 se le respondió el 15-10; **espera la decisión de contabilidad** (`e2-101` sin correr) |
| Q12 | Facturas de julio de CNEL (4,02 y 16,66) con saldo | Ana 02-10 | cxp | 🟡 Ana dijo que busca el pago; `e2-87` sin correr |
| Q13 | Centros de costo (operativo / arrendamientos), IVA con y sin crédito tributario, parámetros en el registro de facturas | Steven 06-10 + auditor 16-10 | cxp/sri/cnt | ⚪ **frente grande**; espera el pronunciamiento formal del auditor. Se diseña aparte |
| Q14 | Cierre de planillas: cuentas 1.4.05.05 / 2.3.02.05 / 1.4.05.10 / 2.3.02.10 no quedan en 0 | Steven 06-10 | **crd** | ⛔ de `omen-saa-1` |
| Q15 | Cuenta puente 2.3.01.10.03 Pensiones complementarias por pagar con 35.789,20 | Steven 07-10 | **crd** (y la bandeja de pagos) | ⛔ de `omen-saa-1`; si la causa es la confirmación de pagos, vuelve a nosotros |
| Q16 | Reporte financiero de crédito no cuadra con el balance | Steven 06-10 | **crd/rpr** | ⛔ `omen-saa-1` / `lap-saa-1` (ya trabajan el resumen por bandas) |
| — | Informativos: transferencias Internacional/Pacífico, alcance del reproceso, contrato, firma del acta, cuentas de RRHH para el RDEP | varios | — | sin acción de código. **Firma del acta de la Superintendencia (15-10): pendiente del usuario** |

## 2. Lo que se despacha ya

### 2.1 Q3 — Usuario real en los asientos de la bandeja de `cxp`

**Lo medido:** `ProcesoCargaDocumentosServiceImpl` pasa el literal `"SISTEMA"` como usuario a los generadores de asiento
y a las aplicaciones (`registrarAplicacionPagoCxp` ~l.5040, `recontabilizarDocumento`, y el registro de cada tipo).
El FE ya manda `idUsuario` en `registrarBD`. Es el mismo defecto que `omen-saa-1-arb` reportó en otros módulos el
26-10 (el usuario dijo que es grave).

**Regla:** toda operación de la bandeja que la dispara un usuario (registrar, recontabilizar, anular contabilidad,
anular) graba como usuario del asiento y de la aplicación **el nombre del usuario de `idUsuario`** (el mismo campo
que muestra el asiento en pantalla, p. ej. `ALALANGUI`, `KPARDO`). Si `idUsuario` llega nulo o no existe → 400 con
mensaje, **no** `"SISTEMA"`. `"SISTEMA"` queda solo para procesos sin usuario (si los hay, el ejecutor los lista).
No se corrigen los asientos ya grabados.

### 2.2 Q4 — El concepto de caja chica en el asiento

**Lo medido (captura de Ana):** el gasto de caja chica que paga la factura 072-110-000308880 tiene concepto
*«PAGO CAJA CHICA # 020 MEMO ASOPREP-ASIS-GER-020-2026 ALIMENTCION CHEQUE #8121 VALE #3725»*; el asiento T-E-2026-10-0265
imprime en Observaciones solo *«Pago con caja chica | Proveedor: … | Factura: … | Valor: $22.46»*.

**Regla:** los asientos de caja chica (`generarAsientoGastoCajaChica`, `generarAsientoAplicacionCajaChica`, y el de
ajuste si recibe concepto) llevan en **Observaciones** el texto actual **más** ` | {concepto}`, y ` | {observación}` si
es distinta del concepto, recortado al largo de la columna. La descripción de las líneas no cambia.

### 2.3 Q5 — Imprimir el asiento desde la pantalla que lo genera

**Patrón existente:** `shared/services/imprimir-asiento.service.ts` (`d45b670`), usado en registro de ingreso/egreso y
devolución: botón «Imprimir asiento» en el aviso de éxito y en la consulta.

**Regla:** el mismo botón en **Registrar cobro** (`cxc/forms/cobros/registrar-cobro`), que es el pedido explícito.
Después, el ejecutor **inventaría** todas las pantallas de nuestro alcance que generan un asiento y no tienen el botón
(cruces de anticipo de cliente y proveedor, abonos a factura, anticipos, caja chica, …) y las reporta; se agregan en
la misma pasada las que solo necesitan el id del asiento que ya devuelve el backend. Si una no lo recibe, se reporta y
no se toca el backend sin pedirlo.

## 3. Lo bloqueado y por qué

- **Q6 / P5 (fecha 01-11):** el diseño necesita el `e2-99` (qué cuentas son «bancos» y qué asientos de octubre
  incumplen) y cuatro decisiones: (a) asiento con banco en el debe **y** en el haber; (b) qué tipo lleva la caja chica,
  que no toca banco y no tiene tipo de tesorería propio; (c) los reversos de asientos anteriores; (d) `crd` usa
  `CREDITOS` aunque toque banco: el cambio es de `omen-saa-1`.
- **Q9:** no hay detalle de qué mejorar en la impresión de conciliaciones.
- **Q10, Q14, Q15, Q16:** son de otros equipos; el aviso necesita la autorización del usuario.
