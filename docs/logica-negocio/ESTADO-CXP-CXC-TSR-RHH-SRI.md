# ESTADO — equipo CXP · CXC · PAGOS · TSR · RHH · SRI

**Este documento cubre SOLO los módulos de este equipo. `crd` tiene el suyo.**

**Última actualización: 2026-08-29.** Escrito por el árbitro de este equipo, cruzando la
documentación de cada módulo contra el working tree real de `saaBE` y `saaFE`, y contra lo
reportado por las sesiones de backend y frontend.

**El esquema de trabajo del equipo** (roles, reglas y los tres prompts iniciales) está en
`ESQUEMA-DE-TRABAJO.md`.

## Por qué este documento existe separado

Hasta el 2026-08-28 los dos equipos compartían `ESTADO-GENERAL-TRABAJO-EN-CURSO.md`. Con dos
equipos escribiendo sobre el mismo archivo empezaron a cruzarse los estados (un equipo marcando
como cerrado un frente del otro, referencias a frentes ajenos). **Decisión del usuario: cada equipo
mantiene su propio archivo.** El de `crd` es `ESTADO-CRD.md`; el compartido quedó dado de baja.

**Nada de lo que sigue está compilado ni desplegado.**

⚠️ **La frase «los DDL sí están corridos» que estuvo aquí era falsa.** El 2026-08-29 se verificó
contra la base y **dos scripts no se habían ejecutado nunca** — ver §7. Se corrigió lo encontrado;
la lección de proceso está en §7.3.

---

## 1. Frentes de este equipo — de un vistazo

| # | Módulo | Frente | Estado |
|---|---|---|---|
| H | cxp | Carga automática desde el SRI | ✅ Cerrado |
| I | cxp | Reembolsos de gastos (bandeja) | ✅ Cerrado — no había nada pendiente |
| J | pagos/cxp | Rediseño de aprobación de pagos | ✅ Cerrado |
| K | cxp | Fallback de cuenta contable sin filtro de rol | ✅ Cerrado — ya estaba bien, sin código nuevo |
| L | cxp | `SustentoTributarioService` | ✅ Resuelto — pertenece al frente Q, no era huérfano |
| M | tsr | Cheques / Caja chica / Liquidaciones / Estado de cuenta | ✅ Cerrado BE+FE |
| N | tsr | Conciliación de partidas en tránsito | ✅ Cerrado |
| O | rhh | Ciclo de aprobación de vacaciones | ✅ Cerrado (T3 diferido a propósito) |
| P | rhh | Cierre de cuotas de descuento | ✅ Cerrado |
| Q | sri | ATS + cuadre 103/104 | ✅ Fases 1-6 cerradas |
| R | cxp/cxc | Anulación con auditoría y cascada | ✅ Cerrado BE+FE (9 documentos) |

**Sin pendientes activos:** `reportes`, `cnt` — documentación de API estable.

---

## 2. Lo realizado (28-08)

### H — Carga automática CXP desde el SRI
`PLAN-CARGA-AUTOMATICA-SRI.md`. Casi todo ya estaba construido; el único gap real era el flag
`REGISTRO_LOTE_DISPONIBLE = false` en `gestion-documentos.component.ts`, con un comentario
desactualizado que decía que `/registrarLote` no existía (sí existe desde el 23-08). **Activado por
decisión del usuario.** Fase 4 (bandeja de atención) resultó estar ya construida.

### I — Reembolsos de gastos
El plan lo listaba como "commit pendiente del usuario, 99 líneas sin compilar". Verificado contra
`git status`/`git log`: **no hay nada uncommitted**; la entrega (`8e173fd`) ya está en el
historial. La nota del plan quedó desactualizada. Sin trabajo pendiente.

### J — Rediseño de aprobación de pagos
`PLAN-REDISENO-APROBACION-PAGOS.md`. **Resolvió un bloqueo de producción:** el WAR desplegado ya no
pedía cuenta bancaria al registrar (nace `POR_APROBAR`), pero `PGS.PGTR.PGTRCNBC` seguía `NOT NULL`
→ `ORA-01400` en cada registro de pago. Corregido con `pagos/sql/01-aprobacion-pagos.sql`, corrido
por el usuario en producción.

Entregado además:
- `validaDisponibilidad` real — saldo contable vía `PlanCuentaService.saldoCuentaFechaEmpresa`
  menos lo comprometido en `REGISTRADO`/`EN_ARCHIVO`. **No** usa `MovimientoBanco`, que cubre solo
  1-5% del movimiento real (ver frente N §7bis).
- `GET /pgtr/disponibilidad/{idCuenta}` + su consumo en la pantalla de aprobación.
- Origen externo nuevo `CXC_DEVOLUCION_CLIENTE` (`AnticipoClienteService.solicitarDevolucion` +
  `POST /antc/solicitarDevolucion`), con reconciliación de saldo (`sincronizarDevolucion`/
  `sincronizarDevoluciones`, eager antes de listar, sin timer) y DDL
  `cxc/sql/add-anticipo-cliente-devolucion.sql` (`CBR.ANTC.ANTCIDPG`/`ANTCAPLC`).
- Pantalla de carga de respuesta del banco — existía la pestaña construida pero deshabilitada.
- **Fase 3 (agrupación por beneficiario) descartada por el usuario:** una línea por pago es lo
  correcto, no se fusiona nada.

**Riesgo cruzado avisado al equipo `crd`:** `CRD_DEVOLUCION_APORTE` puede registrar con cuenta
nula; esas devoluciones ahora quedan sujetas a la validación real de saldo al aprobarse. No es
regresión, es el efecto esperado — pero ellos usan `PagoProgramadoServiceImpl` sin tocarlo.

### K — Fallback de cuenta contable sin filtro de rol
`ANALISIS-FALLBACK-CUENTA-CONTABLE-ROL.md`. Las dos consultas de §4 (uso real, no configuración)
dieron **0 filas** — ningún titular en riesgo. Verificado que `existeCuentaConRolEstricto` ya está
activo en los tres puntos (`validarCuentasContables`, `validarCuentasContablesLiquidacion`,
`verificarCuentaContableProveedor`), y que es genuinamente estricto hasta la query JPQL, sin
fallback escondido. **Sin código nuevo — el documento describía el estado real, no un objetivo.**

### M — Cheques / Caja chica / Liquidaciones / Estado de cuenta
`PLAN-CHEQUES-CAJA-CHICA-LIQUIDACIONES-ESTADO-CUENTA.md`. Auditoría completa confirmó que
**todo estaba entregado**: prompts 02/02b (cheques) y 06 (liquidaciones) con sus 9 hallazgos de
§4.1 y T1-T3 resueltos; FE de cheques y liquidaciones completos.

- La nota "PENDIENTE 02c (3 importantes)" del tablero resultó **huérfana**: sin rastro en código,
  git ni docs. Se dio de baja.
- Detalle que vale registrar: **B2 (condición de carrera al tomar un cheque) se resolvió distinto
  a como pedía el prompt** — no con `setLockMode` en el SELECT (Oracle no admite `FETCH FIRST` +
  `FOR UPDATE` juntos), sino con `em.refresh(cheque, PESSIMISTIC_WRITE)` más la captura de la
  violación del índice único `UQ_PGTR_DTCH`. Mismo objetivo, mecanismo correcto.
- La validación de cuenta contable en liquidaciones quedó **más estricta de lo pedido**: bloquea
  con `PRODUCTOS_SIN_CLASIFICAR` y sin el fallback débil del frente K.

### N — Conciliación de partidas en tránsito
`DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md`. Fases 3 y 4 cerradas en frontend.

**Bug real encontrado y corregido:** la pantalla de cierre ya existía pero estaba construida contra
el contrato **viejo** (§10.3), anterior a la corrección del mismo día (§10.4) — anclaba las
partidas de libros en `idMovimientoBanco` (presente en ~8% de los casos) en vez de
`idDetalleAsiento` (siempre presente), y mandaba `usuario` string donde el backend espera
`idUsuario` numérico. Corregido.

Fase 4 (aviso de partidas >60 días) enganchada en `tablero-cumplimiento-extractos`, ubicación
aprobada por el usuario.

### O — Ciclo de aprobación de vacaciones
T1/T2/T4 ya cerrados el 27-08. Confirmado que el frontend **consume endpoints reales, no mock**
(los `of([])` que aparecen son fallback de `catchError`, no simulación).

`revertirAcreditacion` resultó **ya existir** en backend (`SaldoVacacionesRest:225-243`,
`AcreditacionVacacionesServiceImpl:170-241`) — el comentario del FE que decía "el backend todavía
no lo construyó" quedó desactualizado el mismo día en que se escribió. Contrato real: por
`(idEmpresa, año)`, no por id de acreditación (la acreditación es un proceso batch, no una entidad
con id propio). Valida consumo todo-o-nada. **Conectado en el FE**, con input de año explícito
(no inferido de `fechaCorte`) y confirmación tipo "danger".

**T3 (provisión de vacaciones) sigue diferido a propósito** — requiere tocar
`ContabilizacionNominaServiceImpl`, que está congelado. Decisión pendiente del usuario sobre cuándo
abordarlo; no es un olvido.

### P — Cierre de cuotas de descuento
T1-T6 implementados, el ciclo cierra solo. Cerrado por el usuario el 28-08.

### Q — ATS + cuadre 103/104
`LEVANTAMIENTO-ATS-103-104.md` §10, `CATALOGO-ATS.md`. **Fases 1 a 6 cerradas.**

- **Fase 2 extendida:** `codSustento` ya no solo en factura de compra — también `LQCC`/`NTCC`/
  `NTDC`. Verificado con `VERIFICACION-SUSTENTO-LQCC-NTCC-NTDC.sql`: **0 filas pendientes en las
  tres**, sin backfill necesario. Backfill de factura corrido por el usuario
  (`BACKFILL-SUSTENTO-TRIBUTARIO-UPDATE.sql`).
- **Fase 3:** `parteRel`/`tipoProv` en el titular, `fechaRegistro` contable en los 4 documentos.
  DDL `sri/sql/03-partereal-tipoprov-fecharegistro.sql`, corrido.
- **Fase 4:** generador del XML en `com.saa.ejb.sri` (paquete nuevo), StAX + `ZipOutputStream`.
  `POST /rest/ats/generar` con `{idFacturador, anio, mes}` → ZIP en `contenidoBase64` + `avisos`.
- **Fase 5:** pantalla en `/menucuentasxcobrar/reportes/ats`.
- **Fase 6:** `GET /rest/cuadresri/104/{idFacturador}` y `/103/{idFacturador}`.
- **Catálogo oficial transcrito** a `sri/CATALOGO-ATS.md` desde el Excel del SRI — incluye la
  clave primaria exacta de cada campo XML y qué `codSustento` acepta cada tipo de comprobante.

**Tres correcciones encontradas antes de entregar, cada una habría sido un defecto silencioso:**
1. **Tipo de identificación (Tabla 2):** el plan era reusar `Titular.rubroTipoIdentificacionH` tal
   cual. El ATS usa **dos rangos según dirección** (compras 01-03, ventas 04-07) y el rubro interno
   tiene otro orden — habría mandado RUC y Cédula invertidos.
2. **Filas heterogéneas del cuadre 104:** unas traen `bruto/neto/impuesto`, otras solo `valor`.
   Asumirlas uniformes habría roto la pantalla.
3. **IVA 12% vs 15%:** el catálogo del SRI está desactualizado en ese punto. No afecta — cada
   documento guarda su propio `porcentajeIVA`, el generador nunca consulta esa tabla.

### R — Anulación con auditoría y cascada
Ver §4, tiene su propia sección por el defecto de producción que corrige.

---

## 3. Frente R — Anulación con auditoría y cascada

**Nació de un gap del ATS** (no se podía armar `<anulados>` del lado compra porque no había cómo
saber cuándo ni por qué se anuló un documento) y terminó descubriendo un **defecto real en
producción**, más grave que el gap original.

### 3.1 El defecto de producción — CONFIRMADO, verificado archivo:línea

**Anular un documento de venta reversa TODOS los cobros aplicados, en silencio.** No pregunta, no
bloquea, no avisa.

| Dónde | Verificado |
|---|---|
| `FacturaServiceImpl.anularFactura` | 2601-2615 — reversa todas las `AplicacionPagoCxc` activas siempre |
| `NotaCreditoServiceImpl.anularNotaCredito` | 1495-1508 — mismo patrón |
| `NotaDebitoServiceImpl.anularNotaDebito` | verificado línea por línea, mismo patrón |
| `RetencionV2ServiceImpl.anularRetencionV2` | ~1659 — reversaba en un try/catch que solo advertía |
| `LiquidacionCompraServiceImpl.anularLiquidacion` | 1970-1984 — **peor**: ni reversaba, anulaba dejando aplicaciones huérfanas |

**La regla que pidió el usuario (textual):** *"no debe permitir anular una factura si ya se cruzó un
anticipo o se hizo un pago o se cruzó una retención, salvo que se anulen todos los movimientos
relacionados con esa factura"*, y cuando los hay, preguntar si se anula todo en cascada.

**Corrección:** el comportamiento por defecto pasa a **409 Conflict** — deliberadamente, no es
regresión. La firma y la ruta se mantienen (para no romper llamadores), pero dejan de reversar en
silencio. Con `anularEnCascada: true` reversan y anulan.

### 3.2 Dos trampas encontradas en el camino

**a) Un comentario correcto en su contexto, falso al extrapolarlo.**
`LiquidacionCompraServiceImpl:1970-1983` decía que `AplicacionPagoCxc.liquidacion` "existe pero
nada lo escribe". Cierto para el lado compra (`AplicacionPagoCxp`, sin FK a liquidación), **falso
para venta**, donde `AplicacionPagoCxcServiceImpl.recalcularEstadoPagoLiquidacion:852` sí la usa.
Casi deja el gap sin corregir.

**b) El paquete no indica la dirección del dinero.**
`RetencionV2` vive en `cxc` y se emite como documento electrónico, pero **reduce facturas de
compra** vía `AplicacionPagoCxp`. Igual `LiquidacionCompra` (cxc, sí afecta cobros) frente a
`LiquidacionCompraCompra` (cxp, no tiene nada que cascadear). **`cxc`/`cxp` clasifica por quién
emite el documento ante el SRI, no por si entra o sale plata.** Esta confusión estuvo a punto de
causar dos errores distintos el mismo día.

### 3.3 Alcance final — 9 documentos

| Lado | Tipos | Estado |
|---|---|---|
| Compra | `FCTC`, `LQCC`, `NTCC`, `NTDC` | ✅ BE + FE. `LQCC` sin cascada (verificado: no tiene movimientos) |
| Venta | `Factura`, `NotaCredito`, `NotaDebito`, `LiquidacionCompra`, `RetencionV2` | ✅ BE + FE |

**Pantallas:** compra en `cxp/forms/procesos/consulta-documentos` (vista DETALLE — la fila de la
lista es una proyección de la ingesta SRI y no trae `estadoEmision`, no se puede gatear ahí);
venta en `consulta-documentos-electronicos` + `consulta-facturas` + `liquidaciones` (vista LISTA,
donde `estadoEmision` sí viene en la fila). Diálogo único reutilizado en los dos lados.

**Limpieza hecha al cerrar:** se borraron `MotivoAnulacionDialogComponent` y
`AdvertenciaNcDialogComponent`, huérfanos tras el cambio. El pre-check ad-hoc de notas de crédito
(`_verificarNotasCreditoRelacionadas`) también se eliminó: miraba solo NC —no cobros, retenciones
ni anticipos— y su rama de "continuar" habría terminado en 409 igual. Un chequeo parcial que da
falsa sensación de cobertura es peor que ninguno.

**Forma heterogénea de `movimientosRelacionados`, verificada en los 9:** `Factura` y
`LiquidacionCompra` devuelven `tipoDocPago`/`tipoDocPagoTexto`; **las notas de crédito y débito
—de los dos lados— devuelven `idFactura`/`idFacturaCompra`** (a qué factura se aplicó la nota), no
`tipoDocPago`. El modelo del frontend tiene todos los campos opcionales salvo `idAplicacion`,
`montoAplicado` y `fechaAplicacion`, y resuelve la etiqueta según cuál venga. **No asumir la forma
de `Factura` para los nueve.**

### 3.4 Contrato

- `GET /<ruta>/movimientosRelacionados/{id}` — la lista para armar el diálogo antes de anular.
- `POST /<ruta>/anular` con `{idXxx, motivo, usuario, idUsuario, anularEnCascada}`.
- **Asimetría deliberada:** compra usa `/anular/{id}`; venta usa `/anular` con el id **en el body**.
  Se respetó la convención preexistente de cada lado en vez de unificarla y romper llamadores.
  **No "arreglar" esto.**
- **409 Conflict** cuando hay movimientos y no viene cascada.
- **200 con `exito: false`** para documento inexistente o ya anulado — no alcanza con mirar el
  status HTTP.
- Ruta de retención: **`/rtv2`**. Registro corregido: el frontend **siempre apuntó bien** ahí; el
  botón fallaba porque el endpoint no existía en backend, no por un error de ruteo.
- El diálogo de retención dice facturas de **compra** afectadas, no de venta (ver §3.2b).

---

## 4. Pendientes del usuario

### 🔴 Bloqueante
Ninguno. Todos los bloqueos de esta tanda se resolvieron.

### ✅ Scripts ya corridos (29-08)
- ~~`cxp/sql/add-anulacion-documentos-compra.sql`~~ — **corrido en local y producción.** El BLOQUE 0
  confirmó contra datos reales lo que el backend solo había verificado por código: **ninguna fila
  tiene `ESTADOEMISION=3`** en las cuatro tablas, así que el valor estaba libre y no reinterpreta
  nada histórico. `FCTC` tiene 138 filas, todas en estado 2; `LQCC`/`NTCC`/`NTDC` están vacías.
  *(Consecuencia para las pruebas: la anulación de liquidación y notas de compra no se puede
  ejercitar con datos reales hasta que existan documentos de esos tipos.)*
- ~~`cxp/sql/add-fk-producto-detalle-compras.sql`~~ — **corrido.** Ya no se puede borrar un producto
  en uso; las 5 facturas con producto huérfano se toleran (`ENABLE NOVALIDATE`) sin permitir casos
  nuevos.

### 🟡 Por verificar
3. **Compilar en Eclipse** — ningún agente puede compilar backend (`mvn` no está en el PATH). Los
   ~15 archivos nuevos/modificados del frente R no tienen verificación de compilación.
   *(El frontend sí quedó verificado: `ng build` completo, limpio. Nota de proceso: el chequeo
   rápido `tsc --noEmit -p tsconfig.json` que se usó durante la sesión **no valida nada** en este
   repo — el `tsconfig.json` es solution-style, `files: []` + `references`, y sin `-b` compila cero
   archivos con exit 0 siempre. Lo que sí validó fue el `ng build` de cada tanda. Para chequeos
   rápidos usar `tsconfig.app.json`.)*
4. **Declaraciones 103/104 reales** — para cerrar el mapeo de casillas con sufijo del ATS (`303A`,
   `304B`), que quedaron sin sugerir a propósito.

### 🟡 Decidible (hay recomendación, se sigue si no se objeta)
5. Frente M §6 ítems 4-7: caja chica contabiliza al acto, CxP para liquidaciones, cheque al girar,
   cuenta de ajuste elegida en pantalla.
6. Frente O — cuándo abordar T3 (provisión de vacaciones en el motor de nómina).

### ⛔ Bloqueante antes de un envío real al SRI
7. **El XML del ATS nunca se validó contra el XSD ni el validador oficial del SRI.** La estructura
   sigue el esquema público estable, pero nadie lo probó contra la herramienta real. **No presentar
   un ATS generado por este servicio sin pasar por ahí primero.**

### ⚪ Sin urgencia
8. `MIGRACION-CRUCES-ANTICIPO.md` (20-08) — "PENDIENTE DE EJECUTAR", sin fecha límite.
9. `PLAN-TECNICO-PAGOS-COBROS.md` §7.5 — el "segundo asiento al generar el pago", sin definir.
   §7.2-3 (formato del TXT bancario, `BEXTCDIF`) dependen de especificación externa del banco.
10. Decidir si se borra `ESTADO-GENERAL-TRABAJO-EN-CURSO.md`. Ya está marcado como dado de baja y
    ninguna sesión lo mantiene, pero **no está en git**: borrarlo pierde el registro de la jornada
    del 28-08 de forma irreversible. Por eso no se hizo sin confirmación.

---

## 5. Limitaciones conocidas, aceptadas

- **Respuesta del banco:** el lector (`LectorRespuestaBancoExcelImpl`) espera un **Excel de 4
  columnas armado a mano** (id de pago, resultado, referencia, motivo), no el formato nativo del
  banco — que todavía no se entregó. Confirmado con el usuario: la pantalla de confirmación manual
  es el camino principal, no algo temporal.
- **`<anulados>` del ATS:** no distingue una anulación interna de una baja hecha en el portal del
  SRI. El aviso de "revisar antes de enviar, puede haber duplicados" cubre las 7 tablas.
- **`parteRel`/`tipoProv`/`fechaRegistro`:** nacen en `NULL` y se capturan a mano. No hay regla para
  inferirlos retroactivamente (a diferencia de `codSustento`, que sí la tenía).
- **Devolución a cliente (`CXC_DEVOLUCION_CLIENTE`):** solo trackea la última devolución por
  anticipo, sin historial (Opción A elegida por el usuario). Si el pago termina `RECHAZADO`/
  `ANULADO`, el anticipo se libera igual para permitir una solicitud nueva.
- **5 facturas con producto huérfano** (ids 122, 159, 189, 190, 191): se dejan como están por
  decisión del usuario. La FK nueva las tolera (`ENABLE NOVALIDATE`) pero impide casos nuevos.

---

## 6. Trabajo no empezado — lo que queda del listado del 27-08

De las 15 solicitudes de `PLAN-SOLICITUDES-2026-08-27.md`, **12 quedaron cerradas**. Estas tres no
se tocaron y siguen con su plan original vigente:

| # | Solicitud | Estado | Tamaño |
|---|---|---|---|
| **2** | **Mensaje "el titular ya existe"** — hoy revienta con `ORA-00001` crudo. Existe `UNIQUE UK_TTLR_IDNT_ESTD` pero `TitularServiceImpl.saveSingle` no valida antes. Plan completo en §1.4 de ese documento | Sin empezar | XS |
| **1** | **Consulta de cobros CxC con anulación** — falta `GET /aplc/listar` y la pantalla de consulta. El reverso (`POST /aplc/revertir/{id}`) ya existe y funciona | Sin empezar | BE S + FE M |
| **10b** | **Novedades del período: campos** — **bloqueada**: hace falta que el usuario diga qué campo concreto echan de menos. `RHH.NVNM` solo tiene `cantidad`/`descripcion`/`valor` y **nunca tuvo columna de días** | Bloqueada | S |

⚠️ **Corregido el 2026-08-29:** los puntos **2 y 1 ya estaban hechos**, BE y FE, cuando esta tabla
decía «sin empezar». El punto 2 se commiteó el 27-08 (`5594f8a`): `TitularServiceImpl.saveSingle`
valida en el alta, `TitularRest:125-174` devuelve 409, y el FE lo consume en `titulares-v2`. El
punto 1 tiene `GET /aplc/listar` (`AplicacionPagoCxcRest:293`) y la pantalla `consulta-cobros`
ruteada y en el menú. Solo **10b** sigue abierto, y sigue bloqueado por el usuario.

**Hallazgo pendiente del punto 2:** el FE descarta el campo `titularExistente` que el REST **sí**
devuelve, y saca el código del titular duplicado con un regex sobre el texto del mensaje. Funciona
hoy porque el regex casa con el texto que el propio REST arma, pero se rompe en silencio si alguien
reformula el mensaje. El comentario del FE (`titulares-v2.component.ts:533-538`) afirma que ese
campo «no existe»: se verificó contra `TitularServiceImpl` —que efectivamente solo lanza texto
plano— sin seguir hasta `TitularRest`, que captura la excepción y la enriquece.

---

## 7. Verificación de DDL contra la base — 2026-08-29

### 7.1 Por qué se hizo

Al correr el control final de `tsr/sql/08-rubros-partidas-transito.sql`, Oracle respondió
`ORA-00942` sobre `TSR.DTCN`. La tabla no existía: **`tsr/sql/07-conciliacion-transito.sql` nunca
se había ejecutado**, y el frente N figuraba como cerrado en este mismo documento.

Se escribieron dos verificaciones de solo lectura, ambas en `docs/logica-negocio/`:

| Archivo | Qué compara |
|---|---|
| `VERIFICACION-DDL-EQUIPO-CXP-CXC-TSR-RHH-SRI.sql` | 42 sondas, una por script de `cxp`/`cxc`/`pagos`/`tsr`/`sri` |
| `VERIFICACION-ENTIDADES-VS-ESQUEMA-CXC-CXP-TSR.sql` | 179 entidades, 2.310 columnas `@Column`/`@JoinColumn` contra `ALL_TAB_COLUMNS` |

### 7.2 Resultado: 40 OK de 42, dos hallazgos

**a) `cxc/sql/add-anticipo-cliente-devolucion.sql` no se corrió — bloqueante de despliegue.**
Faltan `CBR.ANTC.ANTCIDPG` y `ANTCAPLC`. Lo grave no es el frente J: **las dos columnas están
mapeadas en `model/cxc/AnticipoCliente.java:166,176`**, e Hibernate incluye toda columna `@Column`
básica en el `SELECT` que genera. Así que su ausencia no rompe solo la devolución a cliente —
rompe **cualquier lectura de `AnticipoCliente`** con `ORA-00904`: la pantalla de anticipos y el
cruce de anticipos, que son funciones que ya existían. El propio encabezado del script dice «ningún
código hoy lee ANTCIDPG/ANTCAPLC»: era cierto el 28-08 y dejó de serlo cuando se mapeó la entidad.

**b) `TSR.TSRD` no existe — defecto latente, sin impacto hoy.**
`TitularDaoServiceImpl:101` (`buscarPorNombreSimilar`) lanza una consulta **nativa** contra
`TSR.TSRD` con columnas `TSRDNMCM`/`TSRDSTDO`. Ese nombre no aparece en ningún DDL del repositorio:
es lo que quedó del renombrado `Persona → Titular` (commit `e8df43f`), que actualizó las clases
Java pero no el string de la query. **No tiene ningún llamador** — ni Service ni REST — así que hoy
es código muerto y no puede fallar en producción. El riesgo es el siguiente que lo conecte: se
lleva un `ORA-00942` que ningún compilador detecta, porque vive dentro de un string.

### 7.3 La lección de proceso

El documento afirmaba «los DDL sí están corridos» porque **lo escribió quien escribió los scripts,
no quien los ejecutó**. Es el patrón que este equipo ya tiene registrado: el control y lo
controlado compartiendo origen. Y no se detectó antes porque el backend no está compilado ni
desplegado: la primera señal habría sido un `ORA-00942`/`ORA-00904` en producción.

Dos causas concretas, las dos evitables:

1. **`tsr/sql/07` no está listado en `tsr/sql/README-ORDEN-PRODUCCION.md`**, que llega hasta el
   `06`. El `07` se escribió después y nadie lo agregó, así que no aparecía en la lista que se
   sigue al desplegar. Lo mismo vale ahora para el `08`.
2. **Una sonda por script no encuentra el fallo del tipo `ANTC`.** Por eso se agregó la segunda
   verificación, que compara entidad contra esquema columna por columna. Conviene volver a pasarla
   antes de cada despliegue, y regenerarla cuando se agreguen entidades.

### 7.4 Lo que encontró la verificación entidad-vs-esquema: 29 hallazgos, ninguno de esta ola

`CBR.ANTC` ya **no** aparece: el script de §7.2a se corrió y ese bloqueante está cerrado. Los 29
hallazgos restantes son **deriva previa**, ajena al trabajo de este equipo. Tres grupos:

**a) `TTLRCDGO` inexistente en 9 tablas — el renombrado `Persona → Titular` a medias.**
`CBR.DCMC`, `CBR.TDCC`, `PGS.DCMP`, `PGS.TDCP`, `TSR.CBRO`, `TSR.PDRC`, `TSR.PGSS`, `TSR.TCBR`,
`TSR.TPGS`. Son **exactamente** las entidades que tocó el commit `e8df43f` (2026-02-05). Ese commit
cambió `@JoinColumn(name = "PRSNCDGO", …)` por `name = "TTLRCDGO"` sin renombrar nada en la base.

**El arreglo correcto ya está en el repositorio, en dos archivos que el mismo commit sí resolvió
bien:** `Cheque.java:128` y `PersonaRol.java:58` quedaron como
`@JoinColumn(name = "PRSNCDGO", referencedColumnName = "TTLRCDGO")` — la columna física conserva su
nombre viejo y solo la referencia apunta al PK nuevo. Por eso esas dos no aparecen en el informe.
En las otras nueve se cambiaron **los dos** atributos.

**Es un arreglo de Java, no de base de datos** — cambiar `name` de vuelta a `"PRSNCDGO"` en 9
líneas. Renombrar columnas en Oracle sería mucho más arriesgado. Comprobado además que
`PRSNCDGO` **no aparece en ningún `.jrxml`**, así que la trampa del `SELECT *` no entra aquí.

**b) `CBR.TDCC` (13 columnas) y `CBR.TFDC` (8 columnas).** Las tablas existen pero mucho más
angostas que la entidad. Ninguna pantalla las usa.

**c) `PGS.DTDP` no existe.** Es `cxp/DetalleDocumentoPago`. Ojo: `TSR.DTDP`
(`tsr/DetalleDeposito`) **sí** existe — mismo código de tabla en dos esquemas distintos.

**Alcance real, verificado contra `saaFE`:** de los 11 endpoints afectados, solo uno es alcanzable
y vivo — `procesos/cobros/ingresar` llama `tempCobroService.add()`
(`cobros-ingresar.component.ts:291`) contra `TSR.TCBR`. `proposicion-pago` no está ruteado, y en
`header.component.ts` el `DetalleDocumentoPagoService` es un **import sin usar**. El resto no tiene
componente.

**Pregunta abierta que no se puede cerrar desde el código:** si `cobros-ingresar` estuviera en uso,
llevaría fallando desde febrero. O la pantalla no se usa, o falla y nadie lo reportó. Lo decide el
usuario, no un `grep`.

**Nota que evita un susto:** nada de esto afecta el **alta de titulares**. Esa escribe en
`TSR.TTLR`, que está intacta. Lo roto son tablas que *apuntan* a Titular.

---

## 8. Frente S — Todos los módulos pasan por la bandeja de aprobación de pagos

**Decisión del usuario, 2026-08-30:** todo módulo que origine un pago pasa por la bandeja de
tesorería. Backend entregado el mismo día (sin compilar).

### 8.1 Cómo funciona, en una línea

`PagoProgramadoServiceImpl:957` pone el pago en `POR_APROBAR` **si y solo si**
`idCuentaBancariaOrigen` es `null`. Tesorería asigna cuenta y forma de pago después con
`POST /pgtr/aprobar`. No hay ninguna otra bandera.

**El fallo que esto corrige era silencioso:** un módulo que mandaba la cuenta registraba el pago
sin error alguno — simplemente nacía `REGISTRADO` y nunca aparecía en la bandeja. Se veía como una
bandeja vacía, sin nada que explicara por qué.

| Módulo | Endpoint de solicitud | Etiqueta de origen | Estado |
|---|---|---|---|
| cxp — factura de compra | `POST /rest/pgtr` | `FACTURA_COMPRA` | Ya cumplía |
| cxp — egreso tesorería | vía egreso | `EGRESO_TESORERIA` | Ya cumplía |
| cxc — devolución cliente | `POST /rest/antc/solicitarDevolucion` | `CXC_DEVOLUCION_CLIENTE` | Ya cumplía |
| crd — devolución aportes | `POST /rest/dvap/registrar` | `CRD_DEVOLUCION_APORTE` | Corregido por el equipo crd el 29-08 |
| rhh — anticipo trabajador | `POST /rest/ante/aprobar/{id}` | `RHH_ANTICIPO_EMPLEADO` | ✅ BE corregido 30-08 |
| tsr — caja chica (apertura y reposición) | `POST /rest/mvch/apertura` · `/reposicion` | `TSR_CAJA_CHICA` | ✅ BE corregido 30-08 |

`/mvch/gasto` **no** entra: el gasto sale del efectivo de la caja y no genera `PGS.PGTR`.

### 8.2 El hallazgo que casi convierte una mejora en una regresión

`rhh` y caja chica **prohibían la transferencia a propósito**, y lo validaban al registrar:
`AnticipoEmpleadoServiceImpl:180` («no hay datos bancarios del empleado capturados para generar una
transferencia») y `MovimientoCajaChicaServiceImpl` (la caja chica no tiene cuenta bancaria de
destino).

`POST /pgtr/aprobar` **no tenía esa restricción**: aplica una sola forma de pago a todo el lote
(`:1170`) sin mirar el origen. Mover la elección a tesorería sin llevarse la regla la habría
borrado — y peor, aprobar en un mismo lote una factura y una caja chica con `formaPago=2` habría
roto la segunda.

La regla se trasladó a `aprobar` (`PagoProgramadoServiceImpl:1162-1179`): rechaza con
`IncomeException` listando los ids afectados, **antes** de `validaDisponibilidad` y de cualquier
mutación. Verificado archivo:línea.

**Generalización que vale registrar:** al mover una decisión de una capa a otra, las validaciones
que dependían de ella no viajan solas. La que se queda atrás no falla — desaparece.

### 8.3 Dos entidades que parecían el bug de §7.4a y no lo son

El barrido del agente encontró dos `@JoinColumn(name = "TTLRCDGO")` que quedaron fuera de las
nueve: `CuentaBancariaTitular` (`TSR.CTBN`) y `MovimientoCajaChica` (`TSR.MVCH`). No pudo
verificarlas contra la base y las dejó sin tocar, que fue lo correcto.

**Están bien**, y no hizo falta consultar nada nuevo: las dos entraron en la verificación
entidad-vs-esquema de §7.2 con `TTLRCDGO` en su lista de columnas, y **ninguna apareció** en el
informe de faltantes. O sea: esa columna existe en la base con ese nombre. Son tablas creadas
*después* del renombrado, así que nacieron con la nomenclatura nueva.

*Que una verificación corrida el 29-08 respondiera sin ejecutar nada una pregunta surgida el 30-08
es el mejor argumento para volver a pasarla después de cada tanda.*

### 8.4 Frontend — entregado y verificado con `ng build` limpio

Las dos pantallas de solicitud dejaron de elegir cuenta y forma de pago:

- **`aprobar-anticipo-dialog`** (rhh): fuera el selector de cuenta, el de forma de pago, el campo
  `referencia` y `onCambioCuentaOrigen()`. El diálogo es ahora lo que de verdad es —RRHH autoriza
  el anticipo, tesorería paga después—, y el aviso lo dice: *«Anticipo aprobado. Queda pendiente de
  pago en tesorería»*. Antes decía «Anticipo aprobado y pagado», que había dejado de ser cierto.
- **`reposicion-caja-chica`** (tsr): mismo cambio para apertura y reposición.

**Ampliación de alcance del agente, justificada y reportada:** al quitar la forma de pago de la
pantalla de caja chica, toda la lógica de cheque (`chequeS`, `regChequeSiguiente/Error`,
`cuentaOrigenManejaChequera`) quedaba sin nada que la disparara, y la retiró. Verificado: no queda
ninguna referencia a cheque en ese componente. **Consecuencia visible para el usuario final:** esa
pantalla ya no muestra el número de cheque al registrar, porque el cheque lo asigna tesorería al
aprobar. Es coherente con la decisión, pero es un cambio que se va a notar.

La bandeja (`aprobacion-pagos.component.ts`) **no se tocó**: ya armaba el filtro desde
`ORIGEN_PAGO_LABELS` con los siete orígenes y mostraba la etiqueta en español, nunca el código.

`titulares-v2` ahora lee `err.error.titularExistente.codigo` y **conserva el regex como fallback**
para la rama de `TitularRest:165-172` que devuelve solo `mensaje`. El comentario falso que motivó
el hallazgo (§6) quedó corregido explicando que el campo lo agrega el REST, no el Service.

**Huérfanos declarados, no borrados:** `tsr/service/temp-cobro.service.ts` y su `.spec.ts`, tras
retirarse la pantalla «Cobros - Ingresar». El endpoint del backend sigue existiendo.
