# ESTADO GENERAL DEL TRABAJO EN CURSO — todos los módulos

> ⛔ **DOCUMENTO DIVIDIDO. Ninguna sesión lo mantiene ya.** Cada equipo lleva el suyo:
>
> | Módulos | Documento vigente |
> |---|---|
> | `crd` | **`ESTADO-CRD.md`** |
> | `cxp` `cxc` `pagos` `tsr` `rhh` `sri` | **`ESTADO-CXP-CXC-TSR-RHH-SRI.md`** |
>
> Los frentes A a G de §1 (módulo CRD) están **desactualizados desde el 2026-08-28** y su
> contenido vive ahora en `ESTADO-CRD.md`, que además cubre el frente H (alimentación contable),
> inexistente aquí. **Para `crd`, no leer este archivo.**

> ## 🗄️ DOCUMENTO DADO DE BAJA — 2026-08-28
>
> **Los dos equipos ya tienen su propio documento. Ninguna sesión mantiene este.**
> Lo de abajo queda solo como **registro histórico** de la jornada del 28 de agosto.
>
> | Equipo | Módulos | Documento vigente |
> |---|---|---|
> | `saabe-bc` | `cxp` `cxc` `pagos` `tsr` `rhh` `sri` | **`ESTADO-CXP-CXC-TSR-RHH-SRI.md`** |
> | `saabe-4b` | `crd` | **`ESTADO-CRD.md`** |
>
> **No leer este archivo para saber el estado actual** — está congelado y sus tableros ya no
> reflejan la realidad. Ir a los dos de arriba.
>
> El esquema de trabajo del equipo (roles y prompts iniciales) está en **`ESQUEMA-DE-TRABAJO.md`**.

**Escrito por el árbitro el 2026-08-28**, cruzando la documentación de cada módulo contra el
working tree real (`git status`/`git diff`) de `saaBE` y `saaFE`, y contra lo reportado por las
sesiones de backend (`saabe-bf`) y frontend (`saafe-77`).

**Por qué existe este documento:** `docs/logica-negocio/crd/ESTADO-TRABAJO-EN-CURSO.md` decía ser
"el primer archivo que se lee" al retomar, pero solo cubre 3 frentes de `crd` y no se actualizó
desde el 2026-08-25 — no menciona la ola de Devengo de Aportes, que ya lleva un plan completo
implementado en el working tree. Este documento es ahora el punto de entrada real, a nivel de todo
el sistema. Los documentos de cada módulo siguen siendo la fuente de detalle; este es el índice y
el cruce contra el código.

**Nada de lo que sigue está commiteado, compilado ni desplegado**, salvo donde se indica
explícitamente lo contrario.

---

## 0. Los frentes, de un vistazo

**Corte del 2026-08-28, final del día.** De los 16 frentes originales (A-Q), **todos los de
`cxp`/`pagos`/`tsr`/`rhh`/`sri` quedaron cerrados en código**; los de `crd` (A-G) son de otro
equipo (`saabe-4b` + sus agentes) y no los toca esta sesión. Se agregó el frente R, que nació de un
gap encontrado al construir el generador del ATS.

| # | Módulo | Frente | Código | DDL | Producción |
|---|---|---|---|---|---|
| A | crd | Devengo de aportes + Contratos-vigencias | BE 5/5 fases **confirmadas** por `saabe-bf`; FE avanzado. Desvío sin reportar en `ContratoRest.porEntidad` | **Ejecutado en pruebas y producción** (28-08) | ❌ falta compilar/desplegar el WAR |
| B | crd | Devolución de aportes a partícipes | **9/9 fases entregadas** (Fase 5 confirmada cerrada, doc de estado estaba desactualizado) | Ejecutado **en producción** | ❌ |
| C | crd | Fix proceso diario de mora | Terminado y verificado | n/a | ⚠️ **urgente, no desplegado** |
| D | crd | Sacar `Pais` de `crd` | Terminado | n/a (tabla no se mueve) | ❌ |
| E | crd | Simuladores de préstamos | Completo en código | n/a | ❌ solo falta desplegar |
| F | crd | `saldoOtros` cancelados anticipados | Documento sin ejecutar | Sin ejecutar | ❌ decisión abierta |
| G | crd | Segunda ola (saldo capital 6/8, simuladores 2/3, duplicados Petro) | Arrancada a medias | — | ❌ |
| H | cxp | Carga automática CXP desde el SRI | **CERRADO (28-08)** — `REGISTRO_LOTE_DISPONIBLE` activado por decisión del usuario | — | ❌ "pendiente de ejecutar" explícito |
| I | cxp | Reembolsos de gastos (bandeja) | **CERRADO** — no había nada uncommitted, ya estaba resuelto | — | — |
| J | pagos/cxp | Rediseño aprobación de pagos | **CERRADO** — Fases 1-2 + disponibilidad + origen CXC + pantalla de respuesta del banco, todo entregado 28-08. Fase 3 (agrupación) **descartada por el usuario**, no hace falta | Todos los scripts **corridos en local y producción** (28-08) | ✅ |
| K | cxp | Fallback cuenta contable sin filtro de rol | **CERRADO (28-08)** — consulta §4 dio 0 filas en riesgo; verificado que `existeCuentaConRolEstricto` ya está activo en los 3 puntos, sin código nuevo | — | ✅ |
| L | cxp | `SustentoTributarioService` | **RESUELTO** — es del frente Q (ATS/103-104), no huérfano | — | ver Q |
| M | tsr | Cheques / Caja chica / Liquidaciones / Estado de cuenta | **CERRADO BE+FE (28-08)** | Todos los scripts **corridos en local y producción** (28-08) | ✅ |
| N | tsr | Conciliación de partidas en tránsito | **CERRADO (28-08)** — Fase 3 corregida contra el contrato real (bug de contrato viejo encontrado y arreglado), Fase 4 enganchada en `tablero-cumplimiento-extractos`, ubicación aprobada por el usuario | Aplicado **solo en local** | ❌ |
| O | rhh | Ciclo de aprobación de vacaciones | **CERRADO** — T1/T2/T4 + `revertirAcreditacion` (ya existía, FE lo conectó 28-08); T3 diferido a propósito | — | ❌ |
| P | rhh | Cierre de cuotas de descuento (anticipos) | **CERRADO por el usuario (28-08)** | — | ✅ |
| Q | sri | ATS 103/104 | **Fases 1-6 CERRADAS (28-08)** — generador XML, reporte de cuadre 103/104 y pantalla (`/menucuentasxcobrar/reportes/ats`). Catálogo oficial transcrito en `sri/CATALOGO-ATS.md`. `<anulados>` ya cubre compra y venta (ver frente R). Correcciones encontradas y arregladas antes de entregar: tipo de identificación (Tabla 2, dos rangos según dirección) y filas heterogéneas del cuadre 104 | Todos los scripts corridos | ⚠️ **nunca validado contra el XSD/validador oficial del SRI — bloqueante antes de un envío real.** Falta una 103/104 real para el mapeo de casillas con sufijo (303A, 304B) |
| R | cxp/cxc | Anulación de documentos con auditoría y cascada | **COMPRA CERRADO BE+FE.** **VENTA: BE cerrado para 4 de 5 tipos**, retención en construcción; FE en curso. Corrige un **defecto real de producción**, ver §7 | `cxp/sql/add-anulacion-documentos-compra.sql` — **pendiente de correr (BLOQUE 0 primero)** | ❌ |

**Sin pendientes activos:** `reportes`, `cnt` (documentación de API estable; el único
cambio reciente en `reportes` ya está cerrado, es parte del frente A).

---

## 1. Módulo CRD

### A. Devengo de aportes + Contratos-vigencias — `PLAN-APORTES-DEVENGO-CONTRATOS.md` (2026-08-27)

Prompts: `prompts/PROMPT-BACKEND-APORTES-DEVENGO.md` / `PROMPT-FRONTEND-APORTES-DEVENGO.md`.

- **Backend — CONFIRMADO fase por fase por `saabe-bf` (28-08), las 5 están:**
  - Fase 1 (flag contable + `valor=recibido`): `ConfiguracionContabilidadService/Impl` +
    `ConfiguracionRest./cnfg/contabilidadCrd` calzan con §4.3. Marcado en
    `CargaArchivoPetroServiceImpl:337,3378`.
  - Fase 2 (`Aporte.periodoDevengo`/`tipoMovimiento` + prelación por mes incompleto): prelación en
    `CargaArchivoPetroServiceImpl:3218-3410`. Backfill `sql/63`.
  - Fase 3 (`Contrato`+`VigenciaContrato`, migración desde `HSTR` 99): 5 capas completas,
    `VigenciaContratoRest` calza literal con §4.1. Migración solo por SQL (`sql/64`), sin código
    Java — correcto para algo de una sola vez.
  - Fase 4 (generación cobra solo el faltante, entregada apagada): `recopilarAportesPorFaltante`
    gateado por `ConfiguracionGeneracionAportesService`/`/cnfg/generacionPorFaltanteAh`; el camino
    viejo sigue siendo el default, tal como pide el plan.
  - Fase 5 (consultas de cartera a `NVL(devengo)`): confirmado en
    `EntidadDaoServiceImpl:406-412` y en `REGLAS-PADRON-PARTICIPES.md:74-94`, usando
    `PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL`.
  - **Aun así, ningún reporte en el formato §5 del plan quedó escrito en ningún .md** — esta
    confirmación vive solo en este documento, no en el protocolo de reporte que el propio plan
    exige.
- **⚠️ Desvío sin reportar, detectado por `saabe-bf`:** `ContratoRest.porEntidad` (líneas 140-146)
  cambió de contrato el 27-08 — una entidad sin contrato activo ya **no da 404, da 200 con el DTO
  en blanco**. No hay ningún reporte `Impacto en el otro:` de este cambio en ningún .md. **Riesgo
  real: si el frontend construyó su manejo de error contra el 404 original, esto lo rompe en
  silencio.** Hay que confirmar con `saafe-77` si el FE ya espera el 200-en-blanco o sigue
  manejando el 404 viejo.
- **Frontend:** `interruptor-contabilidad`, `vigencia-contrato` (service+model),
  `estado-cuenta-aportes` (con el **mock eliminado** — señal fuerte de que ya conecta al backend
  real), `contrato-edit`, `aporte.service.ts` modificados.
- **Scripts SQL de la ola** (`sql/62` a `sql/64`, más `65` a `77` que van bastante más allá de lo
  que el plan documenta — reconstrucción de junio 2025, comparación de reportes G,
  corrección de fecha de nacimiento, roles sin entidad, limpieza de mora de plazo vencido).
- **DDL base — EJECUTADO en pruebas y producción (confirmado por el usuario, 28-08).**
  `DDL-APORTES-DEVENGO-CONTRATOS.sql` ya corrió en ambos ambientes. El bloque 7.4 (encender la
  contabilidad de CRD) sigue **sin ejecutar a propósito**: se activa aparte, cuando el cierre de
  cartera esté verificado en producción — no confundir "DDL corrido" con "contabilidad activa".
- **Pendiente real:** resolver el desvío de `ContratoRest.porEntidad` con el frontend, y
  actualizar `PROMPT-BACKEND-APORTES-DEVENGO.md`/`PROMPT-FRONTEND-...` con el protocolo de reporte
  §5 realmente completado (para que quede escrito, no solo verificado de palabra).

### B. Devolución de aportes — `PLAN-DEVOLUCION-APORTES.md`, tablero §11 — **CERRADO, doc de estado desactualizado**

`ESTADO-TRABAJO-EN-CURSO.md` (25-08) decía: 8/9 fases entregadas, Fase 5 (FE, `origenExterno` en
`pagos-transferencia`) pendiente, y faltaba "la línea gris" del aviso de deuda. **Resuelto por
`saafe-77` (28-08), verificado con `git log -S`/`git log --all`: las dos cosas ya estaban
commiteadas el 25-08 en `112dbc3 "Cambios cxp"`, el mismo día en que el documento de estado se
escribió** — el documento simplemente no reflejó ese commit.

- `pagos-transferencia.component.ts` → `conceptoPago(pago)` (~línea 1085): si `pago.origenExterno`
  existe, usa `etiquetaOrigenPagoExterno()` (`cxp/model/origen-pago-externo.ts`, mapea
  `CRD_DEVOLUCION_APORTE` → "Devolución de aportes") y arma
  `"Devolución de aportes #<idOrigen>"` en la columna de la lista de pagos.
  `nombreBeneficiario()` cae a `beneficiarioNombre` denormalizado cuando el titular no está en el
  maestro — el caso de una devolución.
- La línea gris del aviso de deuda (`confirmar-devolucion-dialog.component.ts`) también está,
  mismo commit.

**Las 9 fases de este plan están entregadas. No queda pendiente de agente en este frente** — solo
falta compilar y desplegar, como el resto de lo que trae `ESTADO-TRABAJO-EN-CURSO.md` §0.

DDL ya ejecutado **en producción**. `TPAPPRDP` deliberadamente vacío (decisión 2026-08-24: sin
contabilidad todavía).

### C. Fix proceso de mora — ⚠️ URGENTE

Código terminado y verificado por el árbitro (ver `ESTADO-TRABAJO-EN-CURSO.md` §3 para el detalle
técnico). **No desplegado.** Cada noche a las 02:00 que pase sin el fix arriba, el defecto se
repite. Documento de limpieza de datos ya escrito y listo para el usuario:
`LIMPIEZA-MORA-PLAZO-VENCIDO.md` (script `sql/77`) — pero **solo se corre después de** restituir
`PRSTIDST=8` y confirmar que el fix está desplegado.

### D. `Pais` fuera de `crd` — cerrado en código, sin desplegar.

### E. Simuladores de préstamos — `PLAN-SIMULADORES-PRESTAMOS.md`

"El proyecto está completo en código. Todo lo que queda es despliegue" (textual del plan,
25-08). Los 3 `.jasper` ya compilados y commiteados con MD5 distinto (no cayeron en el defecto del
clon). Sin pendientes de agente.

### F. `saldoOtros` cancelados anticipados

Documento escrito 2026-08-12, **sin ejecutar**, con una decisión de negocio todavía abierta sobre
`saldoCapital = 0`. Nadie lo ha tocado desde entonces — confirmar con el usuario si sigue vigente
o quedó superado por el trabajo de la segunda ola sobre saldo de capital (frente G).

### G. Segunda ola — `PENDIENTES-SEGUNDA-OLA.md` (27-08)

- **Pedidos 6+8 (saldo de capital):** el propio documento dice "nada implementado, es la pista de
  arranque" — pero el diff de `ProcesoPagoPrestamoServiceImpl` ya agrega
  `calcularSaldoCapitalPendiente` a la simulación de precancelación, con un comentario que dice
  explícitamente "mismo cálculo que la reestructuración (pedido 8, segunda ola)". **El trabajo ya
  empezó, sin que el documento lo refleje.**
- **Pedidos 2+3 (simuladores):** sin verificar en este barrido — revisar si ya está cubierto por
  `PLAN-SIMULADORES-PRESTAMOS.md` antes de abrir trabajo nuevo, como el propio documento pide.
  **El pedido 6 (saldo de capital) debe re-medirse recién después de que el frente A cierre** — su
  lado de aportes cambia de valor con `valor = lo recibido`.
- **Duplicados Petro (`sql/61`):** A0/A2/A6 ya corrieron en local y producción (confirmado por el
  usuario, 28-08). **Falta revisar los resultados y decidir la limpieza** — el análisis ya no
  bloquea, pero el saneamiento en sí (sobre `CRD.APRT` y, si A0 confirma cargas reprocesadas,
  sobre `CRD.PGPR`) sigue sin diseñarse.

---

## 2. Módulo CXP / PAGOS

### H. Carga automática CXP desde el SRI — `PLAN-CARGA-AUTOMATICA-SRI.md` / `PRODUCCION-...md`

Fases 0.1-0.3, 1-3 entregadas (sin desplegar/probar); **Fase 4 (bandeja de atención) y 0.2 (commit
del fix de reembolso) pendientes**. El documento de producción dice **"pendiente de ejecutar"**
explícitamente — checklist de despliegue completo, sin evidencia de que se haya corrido.

### I. Reembolsos de gastos — `CAMBIO-REEMBOLSO-GASTOS-BACKEND.md`

Código escrito, **sin commitear** — es justo la Fase 0.2 del frente H.

### J. Rediseño de aprobación de pagos — `PLAN-REDISENO-APROBACION-PAGOS.md` (27-08)

**Documento desactualizado respecto al código, confirmado por `saafe-77`:**
- El doc (§7) dice: solo Fase 1 hecha (DDL, estado `POR_APROBAR`, `/pgtr/porAprobar`,
  `/pgtr/aprobar`).
- El código real ya tiene la **Fase 2 completa**: `PagoPorAprobar.java`, `OrigenPagoCxp.java` (BE,
  sin commitear); `aprobacion-pagos/` completo (FE, sin trackear, ya registrado en
  `app.routes.ts`) con bandeja, selección y aprobación en lote — sin `TODO`/`FIXME` ni marcas de
  bloqueo, según `saafe-77`. `pagos-transferencia` ya no pide cuenta ni forma de pago, delegando
  esa elección a la aprobación en lote, tal como describe el plan §3.1.
- **Actualizado 28-08 — bloqueante de producción y siguiente tramo, entregados por `saabe-bf`:**
  el DDL urgente (`sql/01-aprobacion-pagos.sql`, relajar `PGTRCNBC`) ya lo corrió el usuario en
  producción. Backend entregó además: `validaDisponibilidad` real (saldo contable vía
  `PlanCuentaService.saldoCuentaFechaEmpresa` menos comprometido en `REGISTRADO`/`EN_ARCHIVO` —
  la decisión de saldo de §6/frente N ya estaba tomada, ver abajo), `GET
  /pgtr/disponibilidad/{idCuenta}`, y el origen nuevo `CXC_DEVOLUCION_CLIENTE` (pedido del usuario
  el 28-08, no estaba en el plan original — `AnticipoClienteService.solicitarDevolucion` + `POST
  /antc/solicitarDevolucion`, registra el pago `POR_APROBAR` **sin descontar el saldo del anticipo
  todavía** — reportado como pendiente explícito, sin hook de confirmación al que engancharlo, no
  inventado). Sin commitear, sin compilar.
- **⚠️ Riesgo cruzado con el frente A/B (equipo `crd`), señalado por `saabe-bf`:** activar
  `validaDisponibilidad` es transversal. `TSR_CAJA_CHICA` y `RHH_ANTICIPO_EMPLEADO` siempre
  registran con cuenta ya asignada (nunca pasan por `/pgtr/aprobar`, sin impacto). Pero
  `CRD_DEVOLUCION_APORTE` puede registrar con `idCuentaBancariaOrigen` nulo según lo que mande el
  llamador de `crd` — si eso pasa, esas devoluciones ahora quedan sujetas a la validación real de
  saldo al aprobarse. No es una regresión, es el efecto esperado de la Fase 1, pero el equipo de
  `crd` no toca `PagoProgramadoServiceImpl` y necesita saberlo porque sí lo **usa**. Avisado al
  árbitro de `crd` (`saabe-4b`) el 28-08.
- **Frontend, entregado por `saafe-77` (28-08), los 4 ítems del prompt urgente:**
  - Ítem 1: las 3 pantallas de origen (`pagos-transferencia`, `registro-egreso`,
    `anticipos-proveedores`) ya estaban migradas, sin selector de cuenta/forma de pago en el
    registro. Único resto: el selector de "cuenta de origen" en la pestaña "Generar Archivo" de
    `pagos-transferencia` es post-aprobación (agrupa el archivo del banco), correcto que se quede.
    Dead code menor sin corregir (fuera del pedido): `anticipos-proveedores.component.ts` sigue
    chequeando `resp.numeroCheque` en el mensaje de éxito, ya no aplica porque el cheque no se gira
    al registrar.
  - Ítem 2: consumido `GET /pgtr/disponibilidad` en `aprobacion-pagos.component.ts` — muestra los
    tres números, "Disponibilidad desconocida" si falla (nunca inventa 0), advierte sin bloquear si
    el total supera lo disponible.
  - Ítem 3: agregó `CXC_DEVOLUCION_CLIENTE` al catálogo. **Decisión de ubicación con criterio
    propio, pendiente de confirmar:** el botón "Solicitar devolución" no fue a
    `cxc/forms/gestionar/anticipo/` (esa pantalla solo tiene el saldo agregado del cliente) sino a
    `tsr/forms/anticipos/seguimiento-anticipos/` (dentro del alcance del prompt), que sí tiene el
    `saldo` real por anticipo. El "en curso" de la devolución queda como flag de sesión, no
    persistido — correcto, el backend todavía no expone ese estado.
  - Ítem 4: `tsc --noEmit` y `ng build development` limpios. Sin cambios de más en las 3 pantallas
    del ítem 1.
  - **El contrato de `POST /antc/solicitarDevolucion` que `saafe-77` armó a ciegas coincide
    exactamente con lo que `saabe-bf` implementó** (`{idAnticipo, valor, usuario}`) — confirmarlo
    entre ambos, no hace falta que lo retrabajen.
- **Cerrado 28-08 por `saabe-bf` — reconciliación del saldo de `AnticipoCliente`:** DDL
  `cxc/sql/add-anticipo-cliente-devolucion.sql` (Opción A elegida por el usuario:
  `CBR.ANTC.ANTCIDPG`/`ANTCAPLC`, sin tabla nueva, sin historial). `sincronizarDevolucion`/
  `sincronizarDevoluciones` calcados del reconciliador de `DevolucionAporteServiceImpl` (CRD),
  eager antes de listar, sin timer nuevo. `solicitarDevolucion` ahora rechaza una segunda solicitud
  mientras la anterior siga sin aplicar (cierra el hueco de `POR_APROBAR` que el propio `saabe-bf`
  había señalado). **Decisión de diseño no pedida explícitamente, tomada por el agente dentro del
  margen razonable:** si el pago de la devolución termina `RECHAZADO`/`ANULADO`, igual marca
  `aplicado=1` (sin descontar saldo) para liberar el anticipo y permitir una nueva solicitud — sin
  esto, `idPagoDevolucion` quedaría apuntando a un pago muerto para siempre. Parece correcto, sin
  objeción del árbitro; el usuario puede pedir otro criterio si lo prefiere.
- **Pendiente real: Fase 3 (agrupación de archivo) — DESCARTADA por el usuario (28-08): "no debemos
  agrupar... con esto está bien que salga una línea por cada pago".** Frente J queda cerrado salvo
  lo de abajo. Falta actualizar el tablero del documento `PLAN-REDISENO-APROBACION-PAGOS.md`, que
  subestima el avance real. **Pendiente del usuario:** correr
  `cxc/sql/add-anticipo-cliente-devolucion.sql` en local (y luego producción).
- **Gap nuevo encontrado 28-08: no hay pantalla para subir la respuesta del banco.**
  `POST /pgtr/lote/{idLote}/respuesta` existe (`application/octet-stream`, sin multipart) pero
  ningún componente de `saaFE` lo llama. El lector detrás (`LectorRespuestaBancoExcelImpl`) es
  **explícitamente provisional**: espera un Excel de 4 columnas armado a mano (id de pago,
  resultado, referencia del banco, motivo), no el formato real que entregue el banco — el propio
  código dice "formato oficial todavía no fue entregado". **Confirmado por el usuario (28-08): no
  tiene el formato real todavía, así que la pantalla de confirmación manual con ese Excel se queda
  como el camino principal, no como algo temporal.** `saafe-77` está construyendo esa pantalla.
- La decisión de validación de saldo (§6) **ya está tomada y aplicada**: `saldoCuentaFechaEmpresa`
  gana, `obtieneSaldoFecha`/`saldoSegunMovimientosBanco` queda solo para conciliación — ver
  `tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md` §7bis (frente N). No es un pendiente.

### K. Fallback de cuenta contable sin filtro de rol — `ANALISIS-FALLBACK-CUENTA-CONTABLE-ROL.md`

**El documento más nuevo de todo el barrido (28-08, hoy).** Es un análisis de impacto, no un plan
de fases: mide que 85/87 titulares quedarían en riesgo si se "estrictara"
`existeCuentaConRolEstricto`, con 24 titulares "solo Cliente" usados como proveedor. **No dice si
el chequeo estricto ya se implementó.** Tratar como **decisión bloqueante pendiente del usuario**
antes de que cualquier agente toque esa validación.

### L. `SustentoTributarioService` — **RESUELTO, no es huérfano**

`SustentoTributarioService(Impl)`, `FacturaSustentoPendiente.java`, `SustentoTributarioSri.java`
están en `cxp` pero **son del plan ATS/103-104 (frente Q)**, confirmado por `saabe-bf` (28-08):
`docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md` §4.2 y §6, respaldado por
`sri/sql/BACKFILL-SUSTENTO-TRIBUTARIO.sql` que referencia la misma regla (el IVA de la factura
decide 01/02, `GRPPCSUS` solo para las excepciones 03/04/06/07/08). Se movió a `cxp` por dónde
vive el código, no por el módulo del plan que lo gobierna — ver detalle en el frente Q.
Nota menor: el javadoc de la interfaz lleva `@author GaemiSoft` en vez de `@author Sistema SAA`
(inconsistente con el resto de la ola, probablemente plantilla vieja copiada — no bloqueante).

### Referencia — `PLAN-TECNICO-PAGOS-COBROS.md` (07-08, la foto más vieja)

D11 "superada para proveedores por D14". Dos decisiones sin tomar, ninguna bloqueada por trabajo
de agente: el "segundo asiento al generar el pago" (§7.5, pendiente de definir con el usuario) y
el formato del TXT bancario / `BEXTCDIF` (bloqueados por especificación externa del banco).

### Migraciones SQL de anticipos — pendientes de ejecutar

`MIGRACION-CRUCES-ANTICIPO.md` (20-08, "PENDIENTE DE EJECUTAR" explícito) reemplaza
conceptualmente a `CORRECCION-MOVIMIENTO-CRUCE-ANTICIPO.md` (14-08, también sin ejecutar). No hay
evidencia de que ninguno se haya corrido.

---

## 3. Módulo TSR

### M. Cheques / Caja chica / Liquidaciones / Estado de cuenta — `PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md`, tablero §5

| Fase | DDL | Backend | Frontend | Verificado |
|---|---|---|---|---|
| D — Estado de cuenta | n/a | n/a | Hecho | **Probado 27-08** en navegador; queda un aviso falso de "incompleto" |
| A — Cheques | Ejecutado en local | **CERRADO (28-08)** — auditoría completa de `saabe-bf` confirmó 02 y 02b 100% aplicados; "02c (3 importantes)" resultó ser una nota huérfana sin rastro en código/git/docs, no bloquea nada | Confirmado 28-08 por `saafe-77` | Pendiente confirmar en navegador |
| B — Caja chica | Ejecutado en local | Hecho y revisado | Hecho | **Probado 27-08** de punta a punta |
| C — Liquidaciones | **Sin confirmar si corrió** — `add-liquidacion-compra-emision.sql` no tiene marcador de ejecución, a diferencia de otros DDL del repo | **CERRADO (28-08)** — auditoría de `saabe-bf` confirmó los 9 hallazgos de §4.1 y T1-T3 resueltos, código listo asumiendo el DDL aplicado | `saafe-77` verificando (ítem 1 en curso) | Pendiente |

**Pendientes bloqueantes del usuario (§6):** ejecutar en local los 3 scripts SQL de esta ola antes
de lanzar cada prompt correspondiente (01-cheques, 02-caja-chica, add-liquidacion-compra-emision).
Hay 4 decisiones "decidibles" con recomendación ya tomada (§6, ítems 4-7) que se siguen si el
usuario no objeta.

### N. Conciliación de partidas en tránsito — `DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`, §9

| Fase | Qué | Estado |
|---|---|---|
| 1 | DDL (`TSR.DTCN` + columnas en `CNCL`) | **Hecho, aplicado solo en local** |
| 2 | Servicio de cierre (`ConciliacionCierreService`) | **Hecho** |
| 3 | Pantalla de cierre (3 bloques + ecuación en vivo) | Pendiente (frontend) |
| 4 | Aviso de partidas >60 días en el tablero de Tesorería | Backend hecho; falta engancharlo en el frontend |

Corrección importante del 27-08 (§7bis): `MovimientoBanco` no es el mayor auxiliar — cambia el
ancla de los tipos 1 y 2 de `MVCBCDGO` a `DTCNDTAS` (FK a `CNT.DTAS`). Ya aplicada en el DDL local.

---

## 4. Módulo RHH

**No confundir con la calibración mensual de RRHH** (enero-marzo cerrados con diferencia cero,
abril siguiente — eso sigue su propio proceso, sin relación con lo de abajo).

### O. Ciclo de aprobación de vacaciones — `CICLO-APROBACION-VACACIONES.md` (27-08, sin trackear)

T1 (aprobar) y T2 (rechazar/anular) implementados y cerrados 27-08, con `RHH.DVAC`
(`DetalleConsumoVacaciones`) ya enganchada. T4 (endpoints REST) implementado. **T3 (provisión de
vacaciones) queda deliberadamente sin implementar**: requiere tocar el motor de nómina
(`ContabilizacionNominaServiceImpl`, congelado) — **decisión pendiente del usuario sobre cuándo
abordarlo**, no un olvido.

**Frontend confirmado por `saafe-77` (28-08): consume endpoints reales, no mock.**
`saldo-vacaciones.service.ts.acreditar()` → `POST /sldv/acreditar` real, usado por
`acreditar-vacaciones.component.ts` con confirmación previa. `permiso-licencia.service.ts` y
`solicitud-vacaciones.service.ts` también contra `ServiciosRhh.RS_*` reales; los `of([])` que
existen son solo fallback de `catchError` en error 400, no simulación.

**Gap nuevo encontrado (28-08), genuino y sin relación con lo anterior:**
`revertirAcreditacion()` en `saldo-vacaciones.service.ts` (`POST /sldv/revertirAcreditacion`) está
**deliberadamente aislado** — el propio código FE trae un comentario de que el backend todavía no
lo construyó (27-08) y nada en la UI lo llama; el botón "Revertir" queda deshabilitado hasta que
se confirme el contrato. **Pendiente de backend: construir `POST /sldv/revertirAcreditacion`** (o
decidir que no hace falta y quitar el botón deshabilitado del FE).

### P. Cierre de cuotas de descuento — `ANTICIPOS-TRABAJADORES.md` §6 "T4"

"T1 a T6 implementados. El ciclo cierra solo." `CierreCuotasDescuentoServiceImpl` (nuevo,
`@Stateless` con `REQUIRES_NEW` deliberado) enganchado al final de
`ContabilizacionNominaServiceImpl.contabilizarPago`. **No se pudo probar contra un rol real en la
sesión que lo escribió** (no bloqueante según el doc, pero sí una verificación pendiente).

**No auditado en este barrido:** `ORDENES-BACKEND-FASES-5-9.md` (el maestro de fases 1-9 de RRHH)
en sí mismo.

---

## 5. Módulo SRI

### Q. ATS 103/104 — `LEVANTAMIENTO-ATS-103-104.md`, tablero §4.3

| Fase | Estado |
|---|---|
| 1 | Hecho |
| 2 | Backend hecho, **backfill real pendiente** (análisis entregado, `UPDATE` aún sin escribir) |
| 3 | Pendiente |
| 4 | **Pendiente — bloqueada hasta que cierre el backfill de la Fase 2** |
| 5 | Pendiente |
| 6 | Pendiente |

Es el único de los módulos "tranquilos" (junto con `reportes`, `cxc`, `cnt`) con trabajo real
todavía por hacer.

---

## 6. Tareas del usuario — consolidado y priorizado

### 🔴 Bloqueante / urgente

1. **Desplegar el fix de mora (frente C) ya** — cada noche sin él, el defecto se repite. Es
   independiente de todo lo demás y no tiene por qué esperar a las otras olas.
2. **Decidir K** (fallback cuenta contable, `ANALISIS-FALLBACK-CUENTA-CONTABLE-ROL.md`) antes de
   que cualquier agente toque `existeCuentaConRolEstricto`.
3. Ejecutar los scripts SQL locales que bloquean los prompts de TSR (frente M, §6 del plan:
   `01-cheques-pago-programado.sql`, `02-caja-chica.sql`, `add-liquidacion-compra-emision.sql`).
4. **Nuevo (28-08):** resolver si el cambio de contrato en `ContratoRest.porEntidad`
   (404→200-en-blanco, frente A) rompe algo en el frontend — nadie lo reportó con el formato §5
   del plan cuando se hizo.

### 🟡 Decidible (hay recomendación tomada, se sigue si no se objeta)

5. Frente M, §6 ítems 4-7 (caja chica al acto, CxP para liquidaciones, cheque al girar, cuenta de
   ajuste elegida en pantalla).
6. Frente O — cuándo abordar T3 (provisión de vacaciones en el motor de nómina); y si hace falta
   backend para `POST /sldv/revertirAcreditacion` (botón ya construido y deshabilitado en el FE).
7. Frente G — confirmar que el pedido 6 (saldo de capital) se re-mide después del frente A, no
   antes.

### ⚪ Correr cuando convenga (no bloquea nada mientras tanto)

8. `saldoOtros` cancelados anticipados (frente F) — sigue esperando desde el 12-08; confirmar si
   sigue vigente.
9. Migraciones de cruces de anticipo (`MIGRACION-CRUCES-ANTICIPO.md`) — sin fecha límite conocida.

### Resuelto desde la primera versión de este documento (28-08)

- ~~Frente B, Fase 5~~ — confirmado cerrado, el doc de estado del 25-08 no reflejaba un commit del
  mismo día.
- ~~Frente L, huérfano~~ — confirmado que pertenece al frente Q (ATS/103-104), no a esta ola.
- ~~RHH frontend en mock~~ — confirmado que consume API real, no mock.
- ~~Frente A, verificación fase por fase~~ — confirmadas las 5 fases backend por `saabe-bf`.
- ~~Frente A, DDL en producción~~ — confirmado por el usuario (28-08): ya corrió en pruebas y
  producción.
- ~~Frente G, duplicados Petro (`sql/61`)~~ — confirmado por el usuario (28-08): A0/A2/A6 ya
  corrieron en local y producción. Con los resultados en mano, sigue pendiente decidir la
  limpieza sobre `CRD.APRT` (y sobre `CRD.PGPR` si A0 confirma cargas reprocesadas) — eso no
  está resuelto, solo el paso de análisis que lo bloqueaba.

---

## 7. Frente R — Anulación con auditoría y cascada (2026-08-28)

**Nació de un gap del ATS** (el generador no podía armar `<anulados>` del lado compra porque no
había forma de saber cuándo ni por qué se anuló un documento) y terminó descubriendo un **defecto
real en producción**, más grave que el gap original.

### 7.1 El defecto de producción — CONFIRMADO, verificado archivo:línea

**Anular un documento de venta reversa TODOS los cobros aplicados, en silencio.** No pregunta, no
bloquea, no avisa. Si alguien anula hoy una factura ya cobrada, esos cobros desaparecen sin
confirmación de nadie.

| Dónde | Verificado |
|---|---|
| `FacturaServiceImpl.anularFactura` | líneas 2601-2615 — reversa todas las `AplicacionPagoCxc` activas siempre |
| `NotaCreditoServiceImpl.anularNotaCredito` | líneas 1495-1508 — mismo patrón |
| `NotaDebitoServiceImpl.anularNotaDebito` | verificado línea por línea, mismo patrón |
| `LiquidacionCompraServiceImpl.anularLiquidacion` | líneas 1970-1984 — **peor**: ni siquiera reversa, anula dejando `AplicacionPagoCxc` huérfanas |

**La regla que pidió el usuario (textual):** *"no debe permitir anular una factura si ya se cruzó un
anticipo o se hizo un pago o se cruzó una retención, salvo que se anulen todos los movimientos
relacionados con esa factura"*, y cuando los hay, preguntar si se anula todo en cascada.

**Corrección aplicada:** el comportamiento por defecto cambia a **409 Conflict** — deliberadamente,
no es una regresión. La firma y la ruta se mantienen (para no romper a los llamadores), pero dejan
de reversar en silencio. Con `anularEnCascada: true` reversan y anulan como antes.

### 7.2 Trampa documental encontrada en el camino

`LiquidacionCompraServiceImpl:1970-1983` tenía un comentario de una sesión anterior diciendo que
`AplicacionPagoCxc.liquidacion` "existe pero nada lo escribe". **Era cierto para el lado compra
(`AplicacionPagoCxp`, sin FK a liquidación) y falso para el lado venta**, donde
`AplicacionPagoCxcServiceImpl.recalcularEstadoPagoLiquidacion:852` sí la usa activamente. Un
comentario correcto en su contexto original, extrapolado a otro, casi deja el gap sin corregir.

Es otra instancia del patrón «el dato no viene de donde parece»: **verificar el lado que importa,
no el análogo.**

### 7.3 Alcance final

| Lado | Tipos | Estado |
|---|---|---|
| Compra | `FCTC`, `LQCC`, `NTCC`, `NTDC` | ✅ BE + FE cerrados. `LQCC` sin cascada (verificado: no tiene movimientos que cascadear) |
| Venta | `Factura`, `NotaCredito`, `NotaDebito`, `LiquidacionCompra` | ✅ BE cerrado. FE en curso |
| Venta | `Retención` | 🔨 En construcción — **no existía anulación en absoluto**, pero el frontend ya tenía el botón llamándola (fallaba). Se construye completa + cascada, aprobado por el usuario |

### 7.4 Contrato (los dos lados)

- `GET /<ruta>/movimientosRelacionados/{id}` — lista para armar el diálogo antes de anular.
- `POST /<ruta>/anular` con `{idXxx, motivo, usuario, idUsuario, anularEnCascada}`.
  **Compra usa `/anular/{id}`; venta usa `/anular` con el id en el body** — se respetó la
  convención preexistente de cada lado en vez de unificarla y romper llamadores.
- **409 Conflict** cuando hay movimientos y no viene cascada. **200 con `exito: false`** para
  documento inexistente o ya anulado — no alcanza con mirar el status HTTP.
