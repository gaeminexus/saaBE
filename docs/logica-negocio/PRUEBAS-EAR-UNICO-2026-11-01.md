# Pruebas del EAR único (2026-11-01): qué probar, con qué datos y qué esperar

**Escrito por omen-saa-1-be, 2026-11-01. Solo documentación: no se tocó código.** Lo ejecuta el usuario; nada de esto lo corrió el
backend contra una base (sólo `mvn -q compile`).

## 0. Reglas de estas pruebas
1. **Nunca se prueba generando asientos para anularlos.** Cada prueba es de **LECTURA**, o se hace con **datos reales que igual
   se iban a procesar**. Si no hay un dato real que sirva, la prueba no se hace: se anota como «no verificada» y se vigila el
   primer caso real.
2. **Orden del despliegue (sin esto, nada de lo de abajo funciona):** primero los SQL, después el EAR.
   - `crd/sql/357` (tabla `CRD.DSSB`, **con `DSSBVLAH`/`DSSBVLPR`**, y la línea 61 de la plantilla 21).
   - `crd/sql/361` (rubro 237, detalles 2 → 517 pensión y 3 → 516 seguro) y `crd/sql/363` (rubro 237, detalles 4 → 1222 devolución
     y 5 → 976 desembolso; columna `RHH.CFNM.CFNMPRPG` y su semilla 345).
   - `cnt/sql/c20-04` (usuario técnico `REPROCESO`) y `cnt/sql/c20-06` (plantilla 38, línea 2, gasto sin clasificar).
   - **Activar el producto de pago 1222** antes de la primera devolución del sobrante.
   - El usuario **`REPROCESO`** tiene que existir antes de cualquier reproceso de periodo ≤ 2026-08.
3. **Un error de «usuario» es normal y esperado** cuando se llama sin sesión o con un usuario que no existe. Las respuestas son:
   - **400** con el texto «No se puede … sin un usuario válido: falta el usuario» o «el usuario 'x' no existe en el sistema» (REST de
     escritura de crd, cnt, cxc, cxp, tsr, rhh).
   - En unos cuantos REST sin `catch` propio (anulaciones de compra) el mismo texto sale con el código que ese REST ya usaba.
   Esto **no** es un defecto: es la regla nueva. Lo que **sí** sería un defecto es que un proceso grabe algo con ese usuario inválido.
4. **Qué mirar siempre al terminar una prueba real:** en el asiento generado, el campo usuario es el de la persona (nunca
   «SISTEMA»); y las puentes 2.3.02.05 / 2.3.02.10 quedan en cero (D-024).

## 1. Los flujos de dinero, primero

### 1.1 Carga Petro/ARCH con destino del sobrante (B8) — `crd/sql/357` corrido
- **Qué cambió:** el operador elige un destino por partícipe y carga (1 siguiente cuota, 2 aporte, 3 devolución) y el sobrante ya no
  queda como novedad bloqueante. Procesar la carga exige `?usuario=`.
- **Datos reales:** una carga que **de todos modos se iba a procesar** y que tenga al menos un partícipe con sobrante (novedad de
  sobrante bloqueante en la vista previa: excedente del motor sin cuota, aporte que no cabe en ningún mes, HS sin seguro o HS huérfano).
- **Prueba en LECTURA (antes de procesar):**
  1. `GET /rest/dssb/selectByCarga/{idCarga}` → lista (vacía si nadie eligió).
  2. Elegir el destino del partícipe con sobrante (`POST /rest/dssb?usuario=<usuario>`) y repetir el GET: queda **un** registro
     activo (`estado = 1`); elegir otro destino para el mismo partícipe anula el anterior (`estado = 0`).
  3. Sin usuario: `POST /rest/dssb` → **400**.
  4. `GET` de las novedades de la carga: la del sobrante de ese partícipe **ya no bloquea** «valores sin destino».
- **Procesar la carga real:** `POST /rest/asgn/aplicarPagosArchivoPetro/{id}?usuario=<usuario>`.
  - **Esperar:** sin novedad bloqueante nueva; `recibido = aplicado` (el mensaje del invariante suma ahora «+ devoluciones»).
    Destino 1: pago al préstamo; destino 2: aporte en el tipo elegido (9 jubilación u 11 cesantía) con devengo; destino 3: ninguna
    cuota ni aporte, y el valor sale por «socios por pagar» (línea 61).
  - **Asiento de aplicación:** una línea HABER en la cuenta de la línea 61 (hoy 2.3.90.90.10) por el total devuelto; el DEBE se
    reparte en las puentes 2.3.02.05 (lo que entró por filas AH) y 2.3.02.10 (por filas de préstamo); **ambas quedan en cero**.
  - **Tesorería (solo destino 3):** una orden de pago POR_APROBAR con origen `CRD_DEVOLUCION_DESCUENTO` e `idOrigen` = el código de
    DSSB, beneficiario el partícipe y producto 1222. `GET /rest/dssb/getId/{id}` trae `idPagoProgramado` e `idAsiento`.
- **Errores normales:**
  - 409 «La carga ya fue procesada…» al registrar o anular un destino en una carga PROCESADA.
  - 409 «El préstamo … no es del partícipe …» o «solo aporte de jubilación (9) o cesantía (11)».
  - `PRODUCTO_PAGO_DEVOLUCION_NO_RESUELTO` si falta el parámetro 4 del rubro 237 o el 1222 sigue INACTIVO: es la señal para
    activarlo; no se procesa nada a medias (la transacción vuelve atrás).
  - «La plantilla alterno 21 no tiene la línea de devolución al partícipe (aux1=61)»: falta la semilla del 357.
- **No se prueba:** crear destinos de prueba y procesar para después anular.
- **Reproceso (DU1):** solo con una carga que de todos modos haya que reprocesar. Una devolución ya pagada por Tesorería queda viva y
  no se repite; la no pagada se anula con la carga. El reproceso **no** vuelve a aplicar el sobrante por el destino (usa su
  mecanismo propio, D-010): si la carga tenía un destino sin pagar, el invariante puede fallar y se resuelve con D-010.

### 1.2 Usuario del reproceso (B8-r)
- **Qué cambió:** el reproceso ya no graba «SISTEMA» ni «SAA_REPROCESO»: usa `REPROCESO` si el periodo de la carga es ≤ 2026-08, y el
  `usuarioCarga` de la carga si es ≥ 2026-09.
- **Prueba en LECTURA:** en `SCP.PJRQ` comprobar que `REPROCESO` existe (si no, el reproceso de una carga ≤ 2026-08 falla con «graba con el
  usuario 'REPROCESO' y no es válido»; es lo esperado hasta crearlo).
- **Con un reproceso real:** los asientos de reparto y aplicación, los `PagoPrestamo` y el `PagoAporte` quedan con ese usuario.
  **Los montos no cambian.**

### 1.3 Desembolso de préstamos (parámetro 5, producto 976)
- **Qué cambió:** el producto de pago del desembolso sale del parámetro 5 del rubro 237 (976), verificado contra la cuenta
  SOCIOS POR PAGAR de la plantilla de entrega; ya no se busca por cuenta. Aprobar exige el usuario (el id de Tesorería sale del nombre).
- **Datos reales:** el primer préstamo que de todos modos haya que aprobar y desembolsar. No se aprueban préstamos de prueba.
- **Esperar:** aprobación OK; orden de pago POR_APROBAR con producto 976; asiento de entrega con el usuario de quien aprueba.
- **Errores normales:** 400 sin usuario; `PRODUCTO_PAGO_DESEMBOLSO_NO_RESUELTO` si falta el parámetro 5 (afecta **toda** aprobación con
  orden de pago: por eso va la semilla primero).
- **El re-proceso de desembolsos del cuadre** (`/prst/contabilizarDesembolso(s)`) pasa a exigir `usuario` (ya no hay «CUADRE»).

### 1.4 Pago de jubilados (pensión y seguro) — parámetros 2 y 3, `crd/sql/361`
- **Qué cambió:** los ids de producto 517 y 516 dejaron de estar escritos: salen de los parámetros 2 y 3, y se verifica que la cuenta del
  grupo del producto sea la de la línea aux1 2 (pensión) y aux1 4 (seguro) de la plantilla `PAGO_PENSION_COMPLEMENTARIA`.
- **Prueba en LECTURA:** `previsualizarCorrida` (`POST /rest/pgpc/previsualizarCorrida`, no graba) del mes: los guards corren sin
  bloquear y avisan si algo no cuadra.
- **Con la corrida real del mes:** `PRODUCTO_PAGO_PENSION_NO_RESUELTO` / `PRODUCTO_PAGO_SEGURO_NO_RESUELTO` son las señales de
  parámetro faltante, producto inactivo o cuenta que no coincide. Esperar órdenes POR_APROBAR con los productos 517 y 516.
- **Usuario:** la corrida y el reproceso de un jubilado exigen `usuario`; el id para Tesorería sale de ese nombre.

### 1.5 Nómina por empleado (N1) — `RHH.CFNM.CFNMPRPG`, `crd/sql/363`
- **Qué cambió:** el pago individualizado ya no usa el producto «NOMINA» por su código: toma el de `CFNMPRPG` (345) y verifica que su
  grupo tenga la cuenta de la línea 50 (2.5.01 REMUNERACIONES POR PAGAR) de la plantilla de pago.
- **Prueba en LECTURA:** consultar `RHH.CFNM.CFNMPRPG` (debe ser 345) y que el grupo de 345 apunte a 2.5.01.
- **Con el primer pago de nómina real:** la orden de cada empleado debe quedar con producto 345 y, al confirmarse, **debitar 2.5.01,
  nunca 2.3.90.15 HONORARIOS POR PAGAR**. Los 20 pagos del 28-10 se corrigen por SQL aparte; este cambio solo protege los siguientes.
- **Errores normales:** `PRODUCTO_PAGO_NOMINA_NO_RESUELTO` (sin `CFNMPRPG`, producto inactivo o cuenta distinta).

### 1.6 Nota de crédito de compra (N2) — espejo de la factura
- **Qué cambió:** el asiento de la NC acredita según la factura que modifica: intermediario → la cuenta del grupo del producto
  intermediario de **esa factura**, por el total, sin IVA; factura normal → la cuenta del grupo del producto de cada detalle, y el IVA
  de la tabla 17. Nunca la cuenta por defecto.
- **Datos reales:** la próxima NC de compra que de todos modos haya que registrar; una sobre factura de intermediario y una sobre
  factura normal si las hay. Las dos NC del correo ya se corrigieron en producción (no se vuelven a tocar).
- **Esperar:** intermediario → 2 líneas (DEBE proveedor / HABER cuenta del grupo del intermediario) por el total de la NC. Normal → gasto
  a la cuenta del producto del detalle + IVA + proveedor. La descripción de la línea de gasto es exactamente «NC Compra reverso gasto».
- **Errores normales (aclaran el dato, no son defectos):** «No existe (viva) la factura de compra N° … que modifica la NC» si la factura
  no está cargada o está anulada; «no hay cuenta de IVA crédito tributario … (PGS.TSRI tabla 17)»; ND de compra o liquidación con detalle
  sin producto → «línea aux1 2 de la plantilla de compras (alterno 38)» si falta `cnt/sql/c20-06`.
- **NC sobre factura ya pagada:** no es error; la NC queda con su saldo (documento `NC-EXCEDE-SALDO-FACTURA.md`).

### 1.7 Cierre de cartera y controles de cierre
- **Qué cambió:** `ejecutar`, `reversar` y `reabrir` exigen un usuario que exista; los controles de cierre leen las cuentas de la plantilla
  y de los parámetros (sin cuentas escritas); el grupo de cartera sale del tipo de préstamo del producto de cada banda.
- **Prueba en LECTURA (siempre primero):** `POST /rest/cierrecartera/previsualizar` y `GET /rest/cierrecartera/consultar`. No graban.
  Los controles que no pueden medir devuelven **ADVERTENCIA «No se pudo medir»**, no un falso OK.
- **Con el cierre real del mes:** `POST /rest/cierrecartera/ejecutar` con `usuario`; el asiento y la bitácora quedan con ese usuario.
  `reversar` / `reabrir` solo si de todos modos había que reabrir el mes.
- **Errores normales:** 400 sin usuario; el FE de cierre de cartera mandaba `usuario` nulo si no había sesión (ahora no debe enviar la
  petición).

## 2. Resto de los módulos (cada uno: qué probar, qué esperar)
**Regla común:** todas las pantallas que graban mandan el usuario de la sesión (`usuarioSesion()` / `idUsuarioSesion()`); con una sesión
perdida o con el `0` de `appState.getIdUsuario()` el backend responde 400 y no graba. **Probar el camino feliz solo con el primer caso
real del día; el camino de error (sin usuario) se prueba sin generar nada, porque el 400 ocurre antes de tocar la base.**

### 2.1 crd
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Pagos de préstamo (cuota, múltiple, con aportes, abono, precancelación, anular operación) | Un cobro real del día | Mismo resultado contable de siempre; el usuario del asiento es el de la persona. 400 sin usuario. |
| Registrar cobro CBCR y su aprobación/proceso | Un cobro real | Asiento transitorio con el usuario. El rechazo automático al procesar ahora queda a nombre de quien procesa. |
| Recepción Petro (transferencias, confirmar, reversar) | La recepción real del mes | Asiento a nombre de quien confirma; 400 sin usuario. |
| Archivo Petro (crear cabecera, generar, descargar) | La generación real del mes | `POST /rest/gnap` ya valida el usuario al **crear** la cabecera. |
| Aportes, devoluciones de aportes, pensión, plazo vencido, acuerdos de condonación, valor de seguro, certificados | Un caso real | 400 sin usuario; el id de Tesorería sale del nombre ya validado. |
| Mora manual (`/prst/calcularMora`) y recalcular distribución de banda | **Lectura/recálculo** que se haría igual | `?usuario=` ahora obligatorio. El timer de las 02:00 sigue con `SAA_MORA` (no crea asientos). |
| Tabla de amortización desde Excel | Una carga real | `?usuario=` obligatorio; los pagos de migración quedan con ese usuario. |
| Afectaciones manuales (`/avpc`) | Un reparto real | `usuarioRegistro` de **todas** las filas se valida antes de guardar la primera. |
| `revalidarCarga` | Una carga real | `?usuario=` obligatorio. |

### 2.2 cnt
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Asiento manual (crear/editar) | Uno real | `nombreUsuario` debe existir; 400 si no. |
| Anular asiento | Uno real que haya que anular | `?usuario=` obligatorio y motivo; 400 sin ellos. |
| Reversión y copia de asiento | Una real | `?usuario=` obligatorio; **ambas GRABAN**, el reverso/copia queda a nombre de quien lo pide. |
| Cerrar/reabrir periodo; cargador de asientos manuales | El cierre real | Ya validaban; la bitácora ya no rellena «SISTEMA». |
| Rellenos centrales (`AsientoContableServiceImpl`, `AsientoServiceImpl`) | — | **Siguen** (U-5 no está activo): un llamador que aún llegue sin usuario graba «SISTEMA» en vez de fallar. |

### 2.3 cxc (y sri)
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Emitir factura, NC, ND, retención v1/v2, liquidación de compra | La emisión real del día | Valida primero el usuario del documento. El asiento queda con su nombre. |
| Contabilizar / consultar estado / reenviar SRI | **Lectura** del estado de un documento real | Usan el usuario del documento: si no tiene uno válido, el asiento no se genera y responde error. |
| **Reintentar autorización (recepción del SRI)** | Un caso real pendiente | **El cuerpo gana `usuario` obligatorio.** El asiento sale con ese usuario (antes, literal «SISTEMA»). |
| Anular documentos | Una anulación real | `usuario` debe existir; con `anularEnCascada` además `idUsuario`. El `'sistema'` que rellenaba el FE ahora da 400. |
| Anticipo de cliente: confirmar | Uno real | **El cuerpo gana `usuario` obligatorio** (hoy el FE manda solo `idAnticipo`). |
| Cobros y cruces (`/aplc/*`) | Uno real | `idUsuario` obligatorio y existente. |
| ATS, cuadre SRI, consulta SRI | **Lectura** | Sin cambio (sri no graba). |

### 2.4 cxp y pagos
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Carga de documentos del SRI (cargar, registrar BD, recontabilizar, revertir, anular, reembolsos) | La carga real del día | `idUsuario` obligatorio en todas; el id `0` del FE da 400. |
| Lotes (`descargarXmlLote`, `registrarLote`) | Un lote real | El usuario se valida **antes** de lanzar el hilo; sin usuario, 400 inmediato (antes el fallo se perdía en el hilo). |
| Anular factura, NC, ND y liquidación de compra | Una anulación real | `usuario` debe existir. |
| Pago programado: registrar, aprobar, lote/archivo de banco, respuesta del banco, confirmar manual, anular, revertir | Los pagos reales del día | `idUsuario` obligatorio y existente. Un pago anulado/revertido anula su asiento **con el usuario** (ya no hay anulaciones de un argumento). |
| Anticipos/devoluciones de proveedor y cruces | Uno real | Igual. |

### 2.5 tsr
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Ingresos, egresos (también contra anticipo), caja chica (gasto, reposición, apertura, cierre) | Los movimientos reales del día | `idUsuario` obligatorio; el FE de caja chica, cheques y chequeras manda `getIdUsuario()` (0 sin sesión) → 400 hasta que pase a `idUsuarioSesion()`. |
| Cheques y chequeras | Un movimiento real | Ídem. |
| Conciliación contable (conciliar, deshacer, verificar, cerrar mes) | La conciliación real | `usuario` debe existir; el FE mandaba `''` sin sesión → 400. |
| Código muerto (cheque, cierre de caja, cobro, débito/crédito, depósito, pago, transferencia) | — | **No se probó ni se tocó**: no tiene llamadores. |

### 2.6 rhh
| Flujo | Qué probar | Qué esperar / error normal |
|---|---|---|
| Nómina (calcular, aprobar, contabilizar rol y provisiones, descontabilizar, cerrar) | El cierre real del periodo | `usuarioRegistro` obligatorio y existente. |
| Simular nómina | **Lectura** (aunque puede crear la proyección de renta si no existe) | **El cuerpo de `/prdn/simular` gana `usuario` obligatorio.** |
| Orden de pago de nómina, beneficios sociales, liquidaciones, anticipos de empleado, planillas del IESS | Un caso real | `idUsuario`/`usuario` obligatorio. |
| **Liquidación externa: lista** | `GET /rest/lqex/getAll` | **Ya no genera asientos ni cambia estados.** Para refrescar el estado de una liquidación EN_TESORERIA: `POST /rest/lqex/sincronizarPago/{id}?usuario=`. |

## 3. Qué errores NO son normales (avisar al backend)
- Cualquier asiento nuevo con usuario «SISTEMA», «SAA_REPROCESO», «CUADRE» o vacío.
- Una puente (2.3.02.05 / 2.3.02.10) distinta de cero después de procesar una carga.
- Una carga con destino del sobrante que sigue bloqueando por «valores sin destino».
- Un pago de nómina por empleado que debite 2.3.90.15.
- Una NC de compra de factura de intermediario con líneas de IVA o con una cuenta distinta a la del grupo del producto intermediario.
- Un 500 en lugar de un 400 al llamar sin usuario (en los REST de escritura listados en cada contrato).

## 4. Contratos del frontend (lista consolidada)
Documentos de cambio de firma, en `saaFE/docs/`:
- `crd/API-PROCESAR-CARGA-CON-USUARIO.md` — B6-b: `POST /rest/asgn/aplicarPagosArchivoPetro/{id}?usuario=`.
- `crd/API-DESTINO-SOBRANTE.md` — B8: `/rest/dssb` (selectByCarga, getId, POST/PUT `?usuario=`, `anular/{id}?usuario=`).
- `crd/API-USUARIO-OBLIGATORIO-U2.md` — crd, cnt y asoprep.
- `cxc/API-USUARIO-OBLIGATORIO-CXC.md`.
- `cxp/API-USUARIO-OBLIGATORIO-CXP.md`.
- `tsr/API-USUARIO-OBLIGATORIO-TSR.md`.
- `rrh/API-USUARIO-OBLIGATORIO-RHH.md`.

**Parámetros nuevos u obligatorios más probables de olvidar en el FE** (todos con `usuario` o `idUsuario` de la sesión; sin sesión, no se envía):
- Nuevos en query: `POST /rest/prst/cargarTablaExcel/{id}?usuario=`, `POST /rest/asgn/procesarCargaPetro/{id}?usuario=` (sin llamador), `POST /rest/asgn/revalidarCarga/{id}?usuario=`,
  `GET /rest/asnt/generaReversion/{id}?usuario=` y `generaCopia/{id}?usuario=`, `POST /rest/lqex/sincronizarPago/{id}?usuario=`.
- Nuevos en el cuerpo: `usuario` en `POST /rest/fctr/reintentarAutorizacion`, `POST /rest/antc/confirmar` y `POST /rest/prdn/simular`.
- Antes opcionales, ahora obligatorios: `?usuario=` de `/prst/calcularMora*`, `/dsbn/recalcularDistribucion`, `/cierrecartera/reversar` y `/reabrir`, `/asnt/anular`;
  `idUsuario` de `/pgtr/lote/{id}/respuesta`, de las anulaciones con cascada y de todos los pagos, cobros y cruces de cxc, cxp, tsr y rhh.
- **Rellenos del FE que hay que quitar:** `'sistema'` en `consulta-facturas.component.ts` y `consulta-documentos-electronicos.component.ts`; `appState.getIdUsuario()`
  (devuelve `0` sin sesión) en aprobación de pagos, anulación de documentos, nota de venta, caja chica, cheques, chequeras, anticipos de empleado y planillas
  (pasar a `idUsuarioSesion()` y no enviar si es `null`); `usuarioService.getUsuarioLog()?.nombre || ''` en conciliación contable; `|| ''` en afectaciones manuales;
  `@Input() idUsuario = 1` en `reembolsos-factura.component.ts`.
- El `CargaArchivoService.procesarCargaPetro` del FE apunta a `/crar/procesarCargaPetro`, que no existe: se puede borrar.

## 5. Pendientes que NO están en este EAR
- **U-5** (defensa central de `grabarAsiento`): sigue apagada. Hasta activarla, un llamador que llegue sin usuario graba «SISTEMA» en cnt.
- **P10** (cuentas por concepto de la liquidación de excolaboradores): espera la decisión del usuario sobre las cuentas de los conceptos 1-4.
- Resolutor de seguimiento en cxp para la devolución del sobrante (DU3): el pago funciona; solo falta ver el estado del documento desde el seguimiento.
- `SAA_AH` (usuario de los aportes creados por la carga) no cambió: hay SQL de diagnóstico que filtra por ese valor.
- Los cargadores históricos (usuario 38) no se tocaron.
