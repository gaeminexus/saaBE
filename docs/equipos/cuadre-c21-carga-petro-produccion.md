> **Nota (31-oct-2026):** copia del expediente del cuadre para omen-saa-1-arb. **§A1 ya está resuelto en `main` (`364dc1c8`)**: no reimplementar. Las rutas `03-trabajo/…` y `01-seguimiento/…` son del repositorio privado `cuadreSuper`.

# C21 — Carga Petro de producción: defectos, controles del re-proceso y pantalla

**Versión 1 — 30-oct-2026.** Para el equipo que hará los cambios en `saaBE` y `saaFE`. Sale de una revisión del flujo
de afectación de la carga Petro y de lo medido en las cargas re-procesadas 352–354 y en las cargas de producción
449, 450 y 492. Cada punto indica si **se verificó** (código o base) o si viene **reportado** y debe confirmarse antes
de corregir.

Rutas Java relativas a `saaBE/src/main/java/com/saa/`. La clase central es
`ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java` (en adelante **CAP**).

---

## 0. Reglas de trabajo para este cambio

1. **Una sola línea de código: `main` de saaBE y saaFE.** Rama `cuadre/c21-carga-petro-produccion`, merge a `main` al
   terminar. No se abre una rama sobre el WAR que hoy corre en producción (ver §6).
2. **El modo reproceso no cambia de comportamiento** salvo donde este documento lo pida (A1, A4). Con ese modo se
   ensayaron jun–ago 2025 y se hará el re-proceso definitivo (D-049). **Prueba de regresión obligatoria** antes del merge:
   restaurar `antes-agosto-29102026.dmp` en un contenedor desechable, re-procesar la carga 354 con la rama y comparar
   contra la base de trabajo los pagos (`CRD.PGPR`) y los aportes (`CRD.APRT`) de la carga, sumados por entidad, tipo
   de aporte y mes de devengo. Única diferencia admitida: las AVPC 701/702 (§A1). Si hay otra diferencia, no se integra.
3. Todo DDL o semilla nueva va en el momento al §4 de `01-seguimiento/PASE-A-PRODUCCION.md`.
4. Criterio de aceptación de cada punto **verificado con una consulta** sobre la base de trabajo (`saa-oracle-23ai`).
5. Nada se prueba en producción.

---

## A. Defectos (afectan a los dos modos o a producción)

### A1 — La afectación manual se ignora en las filas de aportes (AH) y en el seguro huérfano (HS) · ⛔ verificado

- **Código.** Las filas AH van a `aplicarAporteAH` (CAP `:1724` → `:6006`), que reparte por devengo
  (`distribuirAportePorDevengo`) y **nunca lee `CRD.AVPC`**. La afectación manual solo se aplica dentro de
  `aplicarPagoParticipe` → `verificarYAplicarAfectacionesManualesTotales` (`:4255`, `:5026`; el destino a aporte está en
  `aplicarAfectacionAAporte`, `:5121`), que no atiende las filas AH. Las filas HS huérfanas van a
  `procesarSeguroIncendioHuerfano` (`:1751`), que tampoco lee AVPC.
- **Validación incoherente.** La validación previa da por resuelta la novedad si tiene AVPC (`tieneAfectacionManual`,
  `:2327`, `:2357`). Resultado: la carga no se bloquea, el operador cree que su decisión rige y el motor hace otra cosa.
- **Evidencia** (`sql/01-avpc-aportes.sql`, `resultados/01-avpc-aportes-0{1,2}.csv`):

  | Carga | Rol | Afectación del operador | Lo que aplicó el motor | Efecto |
  |---|---|---|---|---|
  | 450 (sep-2026) | 1032 | 296,50 jubilación + 118,60 cesantía | 148,25 jub + 266,85 ces (ene–may 2026) | 148,25 en cesantía que iban a jubilación |
  | 450 | 32615 | 159,70 jub + 51,58 ces | 122,33 jub + 88,95 ces | 37,37 en cesantía que iban a jubilación |
  | 449 (ago-2026) | 3344 | 120,00 ces | 120,00 ces repartido feb–abr 2026 | tipo correcto; los meses los decide el devengo |
  | 354 (re-proceso ago-2025) | 43585 | 91,00 jub + 91,00 ces | 90,75 + 90,75 + 0,50 jub adelantado a sep-2025 | 0,25 entre tipos |
  | 492 (oct-2026) | 1625 y otros 4 | cesantía | sin aportes: la carga 492 aún no se procesa | con el código actual también se ignoraría |

  El total por partícipe siempre llega a su cuenta individual. Lo que falla es **el tipo de aporte y el mes**.
- **Cambio.** Antes del reparto por devengo, `aplicarAporteAH` (y `procesarSeguroIncendioHuerfano`) aplica las AVPC de
  las novedades de esa fila con la misma regla de `aplicarPagoParticipe`: si hay afectación, se aplica **solo** eso y el
  remanente sin afectar sigue el flujo normal (no se descarta). Un solo método compartido, no una copia.
- **Aceptación.** En un contenedor desechable con el dump previo a la carga 450, procesarla con la rama: los roles 1032
  y 32615 quedan con los aportes por tipo que dicen sus AVPC (466/467 y 470/471). Regresión de §0.2 en verde.

### A2 — El desglose «manual $0, automático $0» es engañoso para aportes · verificado en parte

- El mensaje se arma en CAP `:2928` con `getAplicadoManual()` / `getAplicadoAutomatico()`, que solo suman préstamos
  (reportado). Un partícipe que solo tiene AH sale siempre en 0.
- **Cambio.** Incluir los aportes (y el HS) en el desglose, o decir explícitamente «solo préstamos».

### A3 — Seguro de incendio con PH y PP: bloqueante falso · reportado

- **Al aplicar no se cobra dos veces** (verificado: `montoConHsMergeado`, `:3982`: el HS va con PH siempre, con PP solo
  si no hay PH). Lo reportado es que **la validación previa** suma el HS a las dos filas y una sale bloqueante.
  `CAMBIOS-SAA.md` (C4) ya lo tenía abierto: «HS sumado dos veces si hay PH y PP con seguro».
- **Cambio.** La validación usa la misma regla que la aplicación (llamar a `montoConHsMergeado` / `hsDeLaFila`).

### A4 — Fila PH en $0: el seguro se pierde sin novedad en producción · verificado

- `aplicarPagoParticipe` (`:4237`): con monto 0 marca mora y sale. Solo **en modo reproceso** registra el HS como fila sin
  pago (`registrarFilaSinPagoReproceso`); en producción el HS descontado desaparece sin aviso.
- **Cambio.** Registrar la novedad también en producción (familia bloqueante o informativa a decidir; propuesta:
  bloqueante, porque hay dinero sin destino).

### A5 — HS huérfano con varios préstamos posibles · reportado

- Sale bloqueante aunque el valor del HS coincida con el seguro de la cuota de un solo préstamo.
- **Cambio.** Si exactamente un préstamo tiene una cuota pendiente con `DTPRVLSI` igual al HS, asignarlo ahí; si hay cero
  o más de uno, sigue bloqueante.

### A6 — Las novedades del HS aparecen recién al procesar · reportado

- **Cambio.** Llevar A3–A5 a la vista previa (`obtenerPrevueloAfectacionManual`, `:3179`) para que el operador las vea
  antes de procesar.

### A7 — El sobrante de aportes no se adelanta cuando la vigencia no cubre: novedad falsa · verificado en parte

- `CAMBIOS-SAA.md` (C4, §4.10 de la doc del re-proceso) lo tiene abierto: el lote de esperados llega solo hasta el mes de
  la carga. En la 354 el rol 43585 **sí** adelantó 0,50 a sep-2025, así que el defecto se da cuando no hay vigencia para
  el mes siguiente. En modo reproceso lo cubre la «red de seguridad» (B3): 569 casos por 849,33 en la 354.
- **Cambio.** Ver B3: en producción no se decide solo, se propone en la vista previa.

---

## B. Controles del re-proceso que deben regir en producción

El modo reproceso (`modoReproceso`) tiene controles que no dependen de la historia y que producción no recibe. Se
sacan de la bandera. La bandera queda **solo** para las reglas de migración de §D.

| # | Control | Hoy | En producción debe |
|---|---|---|---|
| B1 | **Invariante recibido = aplicado + novedades nombradas**, con resumen por categoría (excedentes, sin partícipe, sin vigencia, contrato al corte, HS sin seguro…) | Solo reproceso (`ResultadoReprocesoCargaPetro`) | Calcularse en toda carga y mostrarse en pantalla al terminar. Si no cuadra, la carga no se confirma |
| B2 | **Nada se omite en silencio**: sin partícipe, producto no mapeado, HS sin fila con descuento | Reproceso: novedad con monto. Producción: aborta (sin partícipe) o se pierde (A4) | Novedad con monto en la vista previa, antes de procesar |
| B3 | **Sobrante sin vigencia** (red de seguridad: va al tipo de la vigencia anterior, en el mes) | Reproceso: automático | **Propuesta** en la vista previa; el operador acepta o lo afecta a otro destino |
| B4 | **Destino «devolución»** (excedente que ASOPREP devuelve: D-010, aporte `EXCEDENTE_PETRO`, contabilizado por 2.3.02.10 según D-024/D-025) | Reproceso: lista JSON en la llamada REST | Destino elegible al afectar (`AVPC`), con su asiento. Requiere DDL o semilla: registrar en `PASE-A-PRODUCCION.md` §4 |
| B5 | **Contrato inactivo hoy y vigente al corte** (D-022) | Reproceso: aplica en silencio | Advertencia en la vista previa |
| B6 | **«Siguiente cuota»**: mandar el saldo a la próxima cuota pendiente sin escribir valor por valor | No existe | Opción al afectar. Excluye préstamos de plazo vencido (decisión del 01-09) |

**Aceptación de B.** Con una carga de producción (la 449 o la 450 en contenedor desechable), el resumen B1 cuadra al
centavo y cada novedad trae su monto. Ninguna fila con descuento queda sin destino ni novedad.

---

## C. Pantalla (saaFE, proceso Petro)

| # | Hoy (reportado) | Debe |
|---|---|---|
| C1 | «Procesar Archivo» se habilita con novedades bloqueantes | Deshabilitado mientras haya bloqueantes, **y** el backend rechaza el proceso en ese caso (no solo la pantalla) |
| C2 | El diálogo de error lista las 132 novedades sin marcar cuál bloquea | Agrupadas por familia (bloqueante / informativa), bloqueantes primero, con conteo y monto |
| C3 | «Afectar cuotas» solo aparece en las bloqueantes | Disponible en toda novedad con dinero (incluidas las de aportes y las informativas) |
| C4 | La vista previa no muestra el destino que tomará cada fila | Para cada partícipe: destino propuesto por tipo y mes (aportes), cuota (préstamos), HS, y las propuestas de B3 |
| C5 | No existen los destinos «devolución» ni «siguiente cuota» | Los de B4 y B6 |

---

## D. Lo que NO pasa a producción (reglas de limpieza de la migración)

Quedan bajo `modoReproceso`: anulación de pagos MIGRACION que duplican la carga, reapertura de cuotas saldadas sin pago
(D-019), ANCLA al 31-may, complemento de apertura del re-proceso, D-023 (subir `DTPRVLSI` de una cuota migrada sin
seguro: en producción una cuota sin seguro es un error de originación y ya va a novedad bloqueante, C4f).

---

## E. Prioridad

| Orden | Puntos | Por qué |
|---|---|---|
| 1 — antes de procesar la carga 492 | A1, A4 | Hay decisiones del operador que hoy no rigen y dinero que se pierde |
| 2 | B1, B2, C1, C2 | Que ningún error vuelva a pasar inadvertido |
| 3 | A3, A5, A6, B3, C3, C4 | Falsos bloqueantes y vista previa |
| 4 | B4, B5, B6, C5, A2, A7 | Destinos nuevos |

---

## F. Datos que habrá que corregir (no es parte del desarrollo)

- Carga 450: roles 1032 (148,25) y 32615 (37,37) con el reparto de jubilación y cesantía distinto al afectado. Se
  corrige al llegar a sep-2026 en el cuadre, con decisión registrada en `DECISIONES-Y-PARTIDAS.md`.
- Carga 492: procesarla después de A1 (ver §6).

---

## 6. Versiones: qué queda en producción

- **El código es uno solo: `main`.** El re-proceso no es otra versión del sistema: es un modo del mismo motor
  (`modoReproceso`) y todo lo del cuadre ya está en `main`, junto con lo que el equipo sigue desarrollando. Producción
  corre un WAR anterior; en el pase recibe `main` completo (`PASE-A-PRODUCCION.md` §4).
- **No hay parche sobre el WAR de producción.** Subir solo A1 a producción antes del pase obligaría a mantener una
  segunda línea de código, o a subir `main` antes de tiempo, que arrastra el DDL pendiente (I-5) y los cierres
  bloqueantes (C17). Mientras tanto, regla operativa: **no registrar afectaciones sobre filas AH o HS esperando que
  rijan**, y no procesar la carga 492 en producción hasta tener A1 (o procesarla y corregirla después con constancia).
- **Los datos son lo que tiene dos versiones**: producción sigue operando sobre su base y la base de trabajo tiene el
  re-proceso. Decidido el 30-oct (D-049): la base re-procesada reemplaza a producción, pero el re-proceso **se vuelve a
  hacer completo desde junio 2025** sobre una copia nueva de producción, a una fecha de corte que se está definiendo con
  ASOPREP y con archivos actualizados. Jun–ago 2025 en la base del 28-oct quedan como ensayo.
- **Efecto en este cambio:** el re-proceso definitivo correrá con el motor que haya en `main` en ese momento. Conviene
  que **A1, A4, B1 y B2 estén integrados antes de que empiece**, para que las afectaciones sobre AH rijan desde junio.
  La regresión de §0.2 sigue siendo obligatoria: compara contra el ensayo, y toda diferencia tiene que estar explicada
  por un punto de este documento.
