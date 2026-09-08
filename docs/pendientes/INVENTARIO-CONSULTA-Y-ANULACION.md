# Inventario — Consulta, filtros y anulación en tsr / cxc / cxp

**Escrito por:** `omen-saa-2-fe` · **Fecha:** 2026-09-08 · **Encargo:** `omen-saa-2-arb`

**⚠️ Nota de ubicación.** El árbitro pidió este archivo en
`saaBE/docs/logica-negocio/pendientes/INVENTARIO-CONSULTA-Y-ANULACION.md`. Mi rol tiene una regla
dura: *"ALCANCE: solo saaFE. No edites saaBE nunca."* — no la puedo saltar aunque me lo pida un
árbitro. Este documento queda en `saaFE/docs/transversal/` en su lugar. **El árbitro tiene que
copiarlo él mismo a la ruta de `saaBE` que quiere**, o decidir que se quede acá.

**Pedido original del usuario** (textual): *"tengo muchas pantallas que no tienen consulta
correcta y proceso de anulación, por ejemplo los anticipos están en la misma pantalla de
generación, los ingresos, egresos, los cobros de cxc a facturas o los pagos de cxp de facturas,
etc. Revisa todas estas pantallas, depura el menú, y crea la anulación en caso de ser necesario.
Las pantallas de consulta deben permitir buscar por fecha, titular, etc. Todo filtro que ayude a
encontrar los registros, que sean amigables, tengan exportación a csv y muy intuitivas para el
usuario."*

**Cómo se hizo:** cinco investigaciones en paralelo (una por área), cada una verificando
componente por componente en `saaFE` y, para cada anulación, el `*Rest.java`/`*ServiceImpl.java`
real en `saaBE` — nunca se dio por sentado nada por el nombre de una pantalla o de un ítem de
menú. `crd` quedó fuera de alcance a propósito.

**Clasificación usada en cada fila:**
- **(A)** El backend ya tiene el endpoint de anulación; la pantalla no lo expone. Barato y de bajo
  riesgo — es cablear un botón a un servicio que ya existe.
- **(B)** No existe anulación ni en frontend ni en backend. Hay que diseñarla; si el documento se
  contabiliza al registrarse, reversar contabilidad es una decisión de negocio del usuario, no
  técnica.
- **(C)** Existe algo, pero está incompleto, duplicado o mal ubicado (usa un `DELETE` genérico en
  vez del endpoint semántico, o la anulación funciona pero el listado que la contiene no tiene
  filtros).
- **resuelto** — ya cumple lo que pidió el usuario, no hace falta tocarlo.

---

## 1. Anticipos

### 1.1 Tesorería — Clientes, Proveedores, Seguimiento

| Pantalla | Ruta | Registra/Consulta | Filtros hoy | Servidor/cliente | CSV | Anulación (frontend) | Endpoint backend | Clase |
|---|---|---|---|---|---|---|---|---|
| Anticipos — Clientes | `/menutesoreria/procesos/anticipos/clientes` | **Mezclada** (alta + historial del titular en un componente) | Solo por titular (diálogo selector); sin fecha ni estado | Servidor (`POST /antc/selectByCriteria/`) | No | Sí, completa: `anularAnticipo()` → diálogo → `anularCliente()` | `GET /antc/verificarAnulacion/{id}` + `POST /antc/anular/{id}` | resuelto* |
| Anticipos — Proveedores | `/menutesoreria/procesos/anticipos/proveedores` | **Mezclada**, mismo patrón | Solo por titular; sin fecha ni estado | Servidor (`POST /antp/selectByCriteria/`) | No | Sí, completa, mismo patrón | `GET /antp/verificarAnulacion/{id}` + `POST /antp/anular/{id}` | resuelto* |
| Anticipos — Seguimiento | `/menutesoreria/procesos/anticipos/seguimiento` | Consulta pura (estado de cuenta: anticipos + cruces + asientos) | Toggle Cliente/Proveedor + titular; sin fecha ni estado | Servidor (`GET /antc\|antp/seguimiento/{idTitular}/{idEmpresa}`, un solo llamado) | No | Sí, mismo patrón, más "Solicitar devolución" (solo clientes) | Mismos endpoints + `POST /antc/solicitarDevolucion` | resuelto* |

**\*resuelto en cuanto a anulación** — el problema real de las tres es otro: **Clientes y
Proveedores mezclan alta y consulta**, y **ninguna de las tres tiene filtro por fecha/estado ni
exportación CSV**. `solicitarDevolucion` está marcada en el propio código como "no confirmada
contra el backend" pese a que el método sí existe en `AnticipoClienteRest.java` — vale la pena
probarla en runtime antes de confiar en ella.

### 1.2 RRHH — Anticipos a empleados

| Pantalla | Ruta | Registra/Consulta | Filtros hoy | Servidor/cliente | CSV | Anulación (frontend) | Endpoint backend | Clase |
|---|---|---|---|---|---|---|---|---|
| Anticipos a empleados | `/menurecursoshumanos/procesos/anticipos` | **Separada** (listado + diálogo de alta aparte) | Empleado, Estado; sin fecha | Servidor (`GET /ante/listar`) | No | Sí, `anular()` → `AnticipoTrabajadorService.anular()` | `POST /ante/anular/{id}` — exige motivo, solo SOLICITADO/APROBADO sin pago confirmado | resuelto* |

**\*** Ya separa registro de consulta (el único de los cuatro anticipos que lo hace bien). Falta
fecha y CSV. Si el pago ya está `CONFIRMADO`, hay que revertirlo antes vía `POST
/pgtr/revertirConfirmado/{id}` — la pantalla no lo hace por sí sola, pero tampoco lo necesita: ese
caso ya lo cubre "Consulta y gestión de pagos" (§3).

**Anticipos de CLIENTE en cxc (`gestionar/anticipos`) — distinto del de Tesorería, ver §2.**

---

## 2. Ingresos, Egresos y Cobros/Pagos aplicados a facturas

### 2.1 tsr — Registrar Ingresos / Egresos

| Pantalla | Ruta | Registra/Consulta | Filtros hoy | Servidor/cliente | CSV | Anulación (frontend) | Endpoint backend | Clase |
|---|---|---|---|---|---|---|---|---|
| Ingresos | `/menutesoreria/procesos/registrar/ingresos` | **Mezclada** (mismo componente, tabs "Registrar"/"Consulta") | Solo Estado | Servidor (`GET /ingr/listar`) | No | Sí, `anular()` → reversa asiento + movimiento bancario | `POST /ingr/anular/{id}` — exige motivo, solo ACTIVO | resuelto* |
| Egresos | `/menutesoreria/procesos/registrar/egresos` | **Mezclada**, mismo patrón | Estado (servidor) + Beneficiario/Concepto/Tipo de pago/Fecha (**cliente**) | **Híbrido** — 🔴 el filtro que de verdad importa (fecha, beneficiario) es cliente | No | Sí, `anular()` | `POST /egrs/anular/{id}` — exige motivo, solo PENDIENTE_PAGO | resuelto* |

**\*** Anulación resuelta en las dos. Faltan: separar registro de consulta (son tabs del mismo
componente, no pantallas distintas), CSV, y en Egresos pasar los filtros de fecha/beneficiario al
servidor.

### 2.2 cxc — Cobros aplicados a facturas

| Pantalla | Ruta | Registra/Consulta | Filtros hoy | Servidor/cliente | CSV | Anulación (frontend) | Endpoint backend | Clase |
|---|---|---|---|---|---|---|---|---|
| Registrar Cobro | `cobros/registrar` | Solo registro | N/A | N/A | No | No aplica (por diseño) | — | resuelto |
| Consulta de Cobros | `cobros/consulta` | **Separada** | Titular, forma de pago, estado, fecha desde/hasta | **Servidor** (`GET /aplc/listar`) | No | Sí, "Anular" → `revertir()` | `POST /aplc/revertir/{id}` — reversa asiento + movimiento bancario | resuelto |
| Abonos de Factura | `cobros/abonos-factura` | Consulta (historial de una factura) | Por factura | Servidor | No | Sí, "Revertir abono" | `POST /aplc/revertir/{id}` | resuelto |
| Cruce de Anticipo (cliente) | `cobros/cruce-anticipo` | Solo registro | N/A | N/A | No | Indirecta (vía Consulta de Cobros) | — | resuelto |

**Este es el único de los cinco frentes del pedido original que ya está completamente bien
resuelto**, incluido el filtrado en servidor. Solo falta CSV, en las dos pantallas de consulta.

### 2.3 cxp — Pagos aplicados a facturas (circuito de transferencia)

| Pantalla | Ruta | Registra/Consulta | Filtros hoy | Servidor/cliente | CSV | Anulación (frontend) | Endpoint backend | Clase |
|---|---|---|---|---|---|---|---|---|
| Solicitud de Pago (CxP) | `/menucuentaxpagar/pagos/solicitud` | Solo registro | N/A | N/A | No | No aplica (por diseño — dirige a Tesorería) | — | resuelto |
| Consulta y Gestión de Pagos | `/menutesoreria/pagos/consulta` | **Separada** | Proveedor, tipo de pago, estado, fecha, concepto | **Híbrido** — 🔴 solo `estado` va al servidor; proveedor/tipo/fecha/concepto se filtran en **cliente** sobre todo lo ya traído | No | Sí, "Anular" y "Revertir" (confirmado), ambos con motivo | `POST /pgtr/anular/{id}` + `POST /pgtr/revertirConfirmado/{id}` | resuelto* |
| Historial de abonos (factura de compra) | embebido, sin ítem de menú propio | Consulta | Por documento | Servidor | No | Sí, "Revertir" | `POST /aplp/revertir/{id}` | resuelto |
| Cruce de Anticipo (proveedor) | `/menucuentaxpagar/pagos/cruce-anticipo` | Solo registro | N/A | N/A | No | Indirecta (vía historial de abonos) | — | resuelto |

**\*** Anulación completa y bien ubicada. El defecto real es el mismo que ya se corrigió en esta
pantalla para *Recepción y confirmación* (§ítems 1-2 de esa tarea, ya cerrada): acá **no se llegó
a aplicar todavía** — proveedor/tipo/fecha/concepto siguen en cliente. Sin CSV.

---

## 3. Hallazgos nuevos — casos (A): backend listo, pantalla no lo usa

Es la categoría de más valor por menor riesgo. **Seis pantallas:**

| Pantalla | Módulo | Ruta | Lo que falta cablear | Detalle |
|---|---|---|---|---|
| Cheques generados | tsr | `.../pagos/procesos/cheques-generados` | Botón "Anular cheque" | `ChequeService.anular()` ya existe en el frontend y apunta a `POST /dtch/anular/{id}` — lo usa otra pantalla (Chequera, §5) pero ninguna de estas cuatro |
| Cheques impresos | tsr | `.../pagos/procesos/cheques-impresos` | Ídem | Ídem |
| Cheques entregados | tsr | `.../pagos/procesos/cheques-entregados` | Ídem | Es la etapa donde más se esperaría poder anular un cheque ya girado |
| Consulta de cheques | tsr | `.../pagos/consulta/cheques` | Ídem | Es la pantalla "natural" para anular un cheque activo, y tampoco lo tiene |
| Conciliación — Cierre | tsr | `/menutesoreria/procesos/conciliacion/cierre` | Botón "Anular cierre" | `ConciliacionCierreService.anular()` en el frontend ya apunta a `POST /cnct/transito/anular/{idCierre}` — **confirmado por grep que ningún componente del repo lo llama**. Además la pantalla no muestra histórico de cierres pasados, así que ni hay dónde pararse para elegir cuál anular — eso también hay que agregarlo |
| Anticipos de Cliente (CxC) | cxc | `gestionar/anticipos` | Botón "Anular" completo | El backend (`AnticipoClienteRest.java`) tiene `GET /antc/verificarAnulacion/{id}` + `POST /antc/anular/{id}` con reversión en cascada de cruces contra facturas — el frontend no llama a ninguno de los dos. Esta pantalla además no tiene NINGÚN filtro (ni servidor ni cliente): `getAll()` a secas |

---

## 4. Casos (B): no existe en ningún lado

| Pantalla | Módulo | Qué falta | Detalle |
|---|---|---|---|
| Consulta de Extractos Bancarios | tsr | Anulación/reverso de un extracto cargado | Solo existe `DELETE /exbc` genérico (borra por lista de IDs, sin motivo, sin chequear si el extracto ya se usó en una conciliación). Si se carga un extracto con el banco o el período equivocado, hoy no hay forma segura de deshacerlo desde la UI |
| Retenciones de compra (RETENCION_COMPRA / RETENCION_COMPRA_V2) | cxp | Anulación de esos dos tipos de documento | Dentro de "Consulta Documentos" (que sí anula factura/liquidación/NC/ND de compra), el propio código documenta explícitamente: *"las retenciones quedan fuera: sin endpoint"*. Confirmado: no hay `anular` para estos dos tipos ni en el frontend ni en el backend — **corrijo acá una clasificación que un fork marcó como (A) por error: es (B), no hay nada que cablear porque el backend tampoco lo tiene** |

---

## 5. Casos (C): existe pero incompleto, duplicado o mal ubicado

| Pantalla | Módulo | Qué pasa |
|---|---|---|
| Solicitud Chequera | tsr | Usa `DELETE /chqr/{id}` genérico en vez de `POST /chqr/anular/{id}` (que sí tiene motivo y es el que usa correctamente la pantalla "Chequera" de al lado, §Anexo) — mismo backend, dos caminos, esta pantalla eligió el que no corresponde |
| Gastos Caja Chica | tsr | Anulación (`POST /mvch/anular/{id}`) ya funciona bien, con motivo. Falta: separar registro de consulta, filtrar también por beneficiario/documento, CSV |
| Cierre Caja Chica | tsr | Igual que arriba: anulación (`POST /crch/anular/{id}`) resuelta, falta fecha en el histórico y CSV |
| Conciliación Contable | tsr | La única "casi perfecta" del lote: "deshacer" grupo y "reabrir mes" ya resueltos (`POST /cnct/deshacer/{idGrupo}`, `POST /cnct/reabrirMes/...`). Solo falta CSV |
| Liquidaciones (emitir, cxc) | cxc | La anulación (`POST /lqcs/anular`) funciona y coincide con el contrato. Pero el listado interno de la propia pantalla (para elegir qué editar/anular) **no tiene ningún filtro**, ni servidor ni cliente — `getAll()` puro, sin CSV. Además duplica lo que ya cubre "Documentos Electrónicos" |
| Consulta de Facturas (cxc) | cxc | Redundante con "Documentos Electrónicos" (§Anexo): mismos filtros, misma anulación de factura, implementada dos veces por separado. Sin CSV (a diferencia de la centralizada, que sí tiene) |
| Grupos de Productos / Datos SRI / Documentos de reembolso / Negociaciones | cxp | **Patrón repetido cuatro veces**: "anular" en realidad es un `DELETE` físico con `confirm()` nativo del navegador, sin motivo ni auditoría — no es una anulación con trazabilidad, es un borrado |
| Bandeja Electrónica / Gestión de Documentos | cxp | Tienen su propia "reversión" (de la fase de carga: revierte el registro o el asiento), pero no es la anulación formal del documento — esa vive únicamente en "Consulta Documentos". Filtros incompletos (Bandeja: solo período; Gestión: filtra en cliente pese a tener 4 campos) |

---

## 6. Enlaces de menú rotos (fuera del patrón consulta/anulación, pero "depurar el menú" lo pidió el usuario explícito)

| Ítem de menú | Módulo | Problema |
|---|---|---|
| Proposición de Pago | cxp | El componente existe en disco (`proposicion-pago.component.ts`) pero **no está registrado en `app.routes.ts`** — clic en el menú da error de ruta. Verificado con grep exhaustivo, cero coincidencias |
| Consulta de CxP | cxp | Entrada de menú sin ruta **ni componente candidato en todo el árbol de `cxp/forms`** — a diferencia del caso anterior, acá no hay ni siquiera código huérfano detrás. Es un enlace completamente muerto |

*(Nota: el enlace roto de "Solicitud de pago" en el menú de Cheques de `tsr` que se encontró y
corrigió hoy más temprano en la sesión —era un mock, ya se borró del menú y del código— no se
repite acá.)*

---

## 7. Dos temas transversales, más grandes que cualquier pantalla individual

**7.1 — Casi ninguna pantalla exporta a CSV.** De las ~25 pantallas relevadas en total, solo
**tres** tienen exportación real: "Documentos Electrónicos" (cxc), "Chequera" (tsr, CSV y PDF), y
"Consulta de Extractos" (tsr, pero solo del detalle de un extracto ya elegido, no del listado). El
pedido del usuario ("que tengan exportación a csv") aplica prácticamente a todo el inventario, no
es una carencia puntual.

**7.2 — El filtrado en cliente está en casi todos lados, no es un caso aislado.** Encontrado en:
Egresos (tsr), Consulta y Gestión de Pagos (cxp), Anticipos de Cliente/Liquidaciones/Documentos
Electrónicos/Consulta de Facturas/Financiar Factura (cxc), Grupos de Productos/Datos SRI/Bandeja
Electrónica/Gestión de Documentos/Negociaciones (cxp), Consulta de Extractos Bancarios (tsr). En
casi todos estos casos **ya existen filtros reales en la pantalla** — el problema no es la falta
de campos de búsqueda, es que el filtro se aplica sobre un array ya traído completo en vez de
mandarse como parámetro al servidor. Es exactamente el defecto que ya se corrigió en "Recepción y
confirmación de pagos" hoy — este inventario muestra que ese arreglo fue el primero de una lista
larga, no un caso único.

---

## 8. Recomendación — por relación valor/riesgo, de mayor a menor

1. **Los seis casos (A) del §3.** Cablear un botón a un endpoint que ya existe y ya está probado
   del lado del backend es el trabajo más barato y de menor riesgo de todo este inventario. Dentro
   de estos, **Conciliación — Cierre** merece ir primero por severidad: hoy no hay forma de
   deshacer un cierre de mes mal declarado, y encima falta agregarle el histórico de cierres para
   poder elegir cuál anular — son dos cosas, pero las dos ya tienen su lado de backend resuelto.
2. **El filtrado cliente→servidor del §7.2**, empezando por **Consulta y Gestión de Pagos (cxp)**
   y **Egresos (tsr)**: son las dos pantallas de mayor volumen de datos de esta lista, y el
   patrón de arreglo ya está probado (se hizo hoy mismo en Recepción y confirmación). Bajo riesgo
   técnico, alto impacto en la queja original de lentitud del usuario.
3. **Los dos enlaces de menú rotos del §6.** Baratísimo de diagnosticar la causa raíz (ya está
   diagnosticada acá) — falta decidir si se registra la ruta que falta o se retira la entrada de
   menú, como ya se hizo hoy con "Solicitud de pago".
4. **CSV transversal (§7.1).** Es mecánico y repetitivo (mismo patrón en cada pantalla, `ExportService.exportToCSV` ya existe y se usa en otros módulos), pero son ~20 pantallas — conviene agruparlo en lotes por módulo en vez de ítem por ítem.
5. **Los casos (C) del §5** — priorizando los que además involucran contabilidad
   (Gastos/Cierre Caja Chica) sobre los puramente de catálogo (Grupos de Productos, Datos SRI).
6. **El caso (B) de retenciones de compra (cxp)** y **Consulta de Extractos (tsr)** van al final
   a propósito: ambos necesitan una decisión de negocio del usuario antes de diseñar nada —
   reversar un extracto ya usado en conciliación, o anular una retención ya declarada al SRI, no
   son decisiones técnicas.
7. **El patrón de "registro y consulta mezclados"** (Anticipos tsr, Ingresos/Egresos,
   prácticamente todo `cxp`) es el cambio de mayor tamaño y menor urgencia real: ninguna de esas
   pantallas está rota, solo incómoda. Encaja mejor como un frente de UX aparte, después de
   cerrar los puntos 1-6, no como parte de este mismo lote.
