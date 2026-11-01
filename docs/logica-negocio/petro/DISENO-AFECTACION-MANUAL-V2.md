# Afectación manual de la carga Petro — diagnóstico y rediseño (v2)

**Estado: PROPUESTA con decisiones abiertas (§5).** omen-saa-1-arb, 2026-10-30, a pedido del
usuario: «que permita cualquier saldo de un partícipe pagarlo a la siguiente cuota, ponerlo como
aporte o indicar que se procese como devolución; y que el seguro de incendio no se trate como
novedad si no lo es».

Fuente: revisión de solo lectura de saaBE y saaFE (dos revisores) + verificación del árbitro de
los hallazgos marcados ✔. Caso disparador: carga 492, rol 1625 (`crd/sql/332`, `333`).

## 1. Cómo funciona hoy (resumen)

- Una novedad **BLOQUEANTE** (`FamiliaNovedadCarga.TIPOS_QUE_EXIGEN_AFECTACION`, tipos 1-4, 7,
  9-13, 18-20, 22) habilita «Afectar cuotas» en la pantalla.
- La afectación (`CRD.AVPC`) cuelga de **una novedad** y tiene dos destinos posibles: una **cuota**
  (`DTPRCDGO`; el motor aplica en cascada desde la más antigua pendiente de ESE préstamo) o un
  **tipo de aporte** (`TPAPCDGO`, crea un APRT `EXCEDENTE_PETRO` sin devengo). No hay un tercero.
- Al procesar, si la fila tiene AVPC, se aplican SOLO las AVPC y el automático no corre para esa
  fila (`CargaArchivoPetroServiceImpl` ~3249).

## 2. Defectos (✔ = verificado por el árbitro en el código)

| # | Defecto | Efecto | Dónde |
|---|---|---|---|
| F1 ✔ | **Las AVPC solo se aplican en filas de préstamo.** La fila AH va directo a `aplicarAporteAH` → `distribuirAportePorDevengo` sin mirar AVPC; el HS huérfano tampoco. | La afectación del operador se **ignora en silencio**, pero las validaciones previas la cuentan como «con destino». Carga 492: la AVPC 688 (334,75 a cesantía) nunca se aplicó. | ~1418, ~3249, `procesarSeguroIncendioHuerfano` ~3424 |
| F2 ✔ | **El anticipo de aportes no funciona** para entidades del lote: el esperado en lote llega solo hasta el mes de la carga y los meses siguientes dan 0. | Todo pago que exceda lo pendiente hasta el mes de la carga queda «sin aplicar» con el texto FALSO «vigencia no cubre». | ~1365, `esperadoMensual`, `VigenciaContratoServiceImpl.esperadoEnLotePorFilial` |
| F3 ✔ | El desglose «manual / automático» del mensaje de error sale solo de los pagos de préstamo. | Un partícipe solo de aportes muestra siempre «manual $0, automático $0». | `DistribucionBandaServiceImpl` ~585 |
| F4 | El aplicador no filtra por `AVPCESTD`. | Una AVPC anulada se aplicaría igual. | ~3901, ~3154 |
| F5 | Guardar una AVPC no valida nada (tope, que la cuota sea del partícipe, XOR). | Solo se descubre al procesar, 20 minutos después. | `AfectacionValoresParticipeCargaServiceImpl.saveSingle`, `…Rest` |
| F6 | El tipo 22 (aportes inconsistentes) compara contra UN mes de `HistorialSueldo`, mientras que la aplicación reparte por vigencias, con atrasos y anticipo. | Todo partícipe que paga atrasos sale BLOQUEANTE aunque el devengo lo absorba. | `validarAporteAH` ~4443 vs ~5036 |
| F7 | Una AVPC que cubre solo el excedente de una fila de préstamo deja sin aplicar la parte esperada. | El control final (recibido = aplicado) tumba el proceso. | ~3250 |
| F8 | Las novedades que suman al pozo sin `codigoPrestamo` se suman entre sí. | El tope puede mostrar más dinero del realmente libre (rol 1625: 405,25 sobre 334,75); solo lo contiene el límite al total descontado. | ~2554 |
| F9 | `MONTO_INCONSISTENTE` registrado al procesar pisa los montos de la novedad tipo 13 de la fase 2 de la misma fila (idempotente por fila+tipo), aunque tenga AVPC colgadas. | Las AVPC quedan colgando de una novedad con otros montos. | ~1126, ~3662, ~5148 |

### Seguro de incendio (HS)

| # | Caso | ¿Es novedad real? | Hoy |
|---|---|---|---|
| H1 | Partícipe con filas PH **y** PP: las dos suman el mismo HS. | **No** (falso positivo) | La que no corresponde sale `MONTO_INCONSISTENTE` BLOQUEANTE por el valor del HS. Al aplicar, riesgo de cobrar el HS dos veces. |
| H2 | Fila PH en $0 con HS. | No debería perderse | Al aplicar, `return` antes de sumar el HS: queda sin aplicar y sin novedad; el control final tumba la carga. |
| H3 | La cuota no tiene `valorSeguroIncendio`. | Sí (sobrante real) | BLOQUEANTE, pero aunque haya AVPC el camino la pierde. |
| H4 | HS huérfano (sin fila PH/PP) con **más de un** préstamo candidato. | **No** (se puede desempatar por el valor del seguro de cada cuota) | Tipo 13 BLOQUEANTE, nacido recién al procesar. |
| H5 | HS huérfano con **cero** candidatos (préstamo pagado o precancelado). | Sí: es dinero sin destino → **devolución** o aporte | BLOQUEANTE al procesar, y la AVPC no se aplica (F1). |
| H6 | Las novedades del HS huérfano nacen al **procesar**, no en la vista previa. | — | El operador no las ve hasta que falla el proceso. |

### Pantalla (saaFE)

| # | Defecto |
|---|---|
| P1 ✔ | **No existe el destino «devolución»** en ningún diálogo. |
| P2 | No hay «siguiente cuota pendiente»: el operador escribe valor por cuota, préstamo por préstamo. Los préstamos de plazo vencido quedan fuera (decisión del usuario del 2026-09-01). |
| P3 ✔ | «Afectar cuotas» solo aparece en novedades BLOQUEANTE: un saldo en una novedad de COBRANZA o INFORMATIVA no se puede destinar. |
| P4 ✔ | **«Procesar Archivo» se habilita con bloqueantes pendientes**: el operador se entera por el error del backend. |
| P5 | El diálogo de error lista **todas** las novedades de la carga (132), sin marcar cuáles bloquean; la «diferencia monetaria total» es una suma con signo que no sirve; «Total registros» cuenta filas ocultas. |
| P6 | Montos sin formato en los textos que arma el backend («$70.49999999999999»). |
| P7 | El diálogo rotula «Valor recibido Petro» al pozo del tope, y «Saldo total» del préstamo usa `PRSTSLTT`, que el propio código declara no confiable. |
| P8 | La pantalla «Afectación por partícipe» está marcada «Pieza en prueba» y promete un «se aplica automáticamente» que no existe. |

## 3. Propuesta

### 3.1 Un destino de saldo por partícipe

Lo que pide el usuario es decidir **qué hacer con el saldo** de un partícipe, no repartir peso por
peso cada fila. Hay dos maneras de darle forma:

- **M1 — reparto manual detallado (lo de hoy, corregido):** el operador reparte el 100 % del
  descontado del partícipe entre cuotas, aportes y devolución. Útil para casos raros.
- **M2 — «destino del sobrante» (recomendado como camino principal):** el automático aplica lo
  esperado (cuotas, aportes por devengo, seguro) y **lo que sobre** va a un destino que el
  operador elige en la vista previa, sin escribir montos:
  1. **Siguiente cuota pendiente** — de un préstamo elegido o, por defecto, de la cuota más
     antigua entre todos los préstamos activos del partícipe (el motor ya aplica en cascada).
  2. **Aporte** — jubilación o cesantía (por devengo, anticipando; requiere F2 corregido).
  3. **Devolución** — queda registrado como valor a devolver al partícipe (§3.3).

  M2 no necesita predecir el monto en la vista previa (que es simular todo el proceso, como ya
  advierte el código): la decisión es el destino, y el monto exacto se conoce al procesar. Si el
  operador no eligió nada y sobra dinero, la carga se detiene como hoy, pero diciendo exactamente
  qué partícipes y cuánto, y la vista previa ya los marca como «puede sobrar».

### 3.2 Afectación por partícipe, no por fila

Las AVPC (M1) y el destino del sobrante (M2) se aplican **por partícipe (rol)**, sobre la suma de
todas sus filas (AH, HS, PE/PH/PQ/PP), en un único paso posterior a la aplicación automática.
Resuelve F1, F7 y F9 de raíz: ya no importa de qué fila cuelga la afectación.

### 3.3 Devolución como destino

- **DDL:** `CRD.AVPC` agrega un destino «devolución» (columna de tipo de destino 1 cuota, 2 aporte,
  3 devolución, y el CHECK pasa a «exactamente un destino»). Para M2, una tabla o columna del
  destino del sobrante por partícipe y carga.
- **Al procesar:** se crea un registro de «valor a devolver» por partícipe y carga, con su asiento
  (DEBE la cuenta transitoria de la carga / HABER una cuenta por pagar al partícipe) y un pago
  programado a la bandeja de Tesorería (un origen nuevo), igual que otros pagos a partícipes.
- **Se necesita del contador:** la cuenta por pagar de devoluciones de descuentos Petro (decisión
  U3).

### 3.4 Seguro de incendio

- H1: el HS se asigna a UNA sola fila (la del préstamo cuya cuota tiene `valorSeguroIncendio` más
  cercano al HS; desempate PH antes que PP, igual que `montoConHsMergeado`); la otra fila se compara
  sin HS.
- H2: una fila PH en $0 con HS aplica el HS igual.
- H4: el huérfano con varios candidatos se desempata por el valor del seguro de la cuota; solo si
  sigue ambiguo es novedad.
- H5 y H3: el sobrante real del seguro entra al destino del sobrante (M2) como cualquier otro saldo.
- H6: las novedades del huérfano se calculan en la vista previa (fase 2), no al procesar.
- Regla general: **el HS que encuentra su cuota no genera novedad.**

### 3.5 Otros arreglos

F2 (anticipo), F3 (desglose con aportes), F4 (filtrar AVPC activas), F5 (validar al guardar), F6
(el tipo 22 deja de bloquear si el devengo lo absorbe: compara contra lo pendiente por vigencias),
F8 (pozo sin duplicados), P4-P7 en la pantalla.

## 4. Fases

| Fase | Contenido | DDL | Tamaño |
|---|---|---|---|
| **0 — urgente (destraba la carga 492)** | F2 anticipo + F6 + montos redondeados en los textos (P6) | No | Chico, solo BE |
| 1 | F1/F7/F9 afectación por partícipe (M1 corregido) + F3, F4, F5, F8 | No | Medio, BE |
| 2 | Destino del sobrante (M2) + devolución (§3.3) | **Sí** | Grande, BE + FE + contabilidad |
| 3 | Seguro de incendio H1-H6 | No | Medio, BE |
| 4 | Pantalla: P2-P5, P7, P8 | No | Medio, FE |

**Carga 492 con la fase 0:** con F2 corregido, el devengo aplica los 334,75 a cesantía
(los 264,25 pendientes hasta octubre más 70,50 anticipados a noviembre-diciembre). La AVPC 688
seguiría ignorada (F1), pero su intención era la misma (cesantía); el resultado es correcto y,
además, queda con devengo. La AVPC 688 se puede dejar: no se aplica y las validaciones la cuentan
como «con destino». Hay que confirmarlo con el `333` (total faltante = 264,25).

## 4bis. Arreglo urgente del 2026-10-31 (autorizado por el usuario): solo F1 en AH y HS huérfano

Para destrabar la carga 492 antes del pase grande. Coincide con el «A1» del equipo de cuadre.

- `aplicarAporteAH` (~4833): justo después de `restaurarActivoPorPago(entidad, montoRecibido)` y
  antes de buscar el `HistorialSueldo`, llamar
  `verificarYAplicarAfectacionesManualesTotales(participe, CODIGO_PRODUCTO_APORTES, cargaArchivo)`.
  Si devuelve `true`, la fila se dio por aplicada con sus AVPC: no se corre el devengo y se devuelve
  un valor > 0, para que el resumen la cuente como éxito.
- `procesarSeguroIncendioHuerfano` (~3424): después del control `montoHS <= 0.01` y antes de buscar
  la entidad y los préstamos candidatos, la misma llamada con `CODIGO_PRODUCTO_HS`. Si devuelve
  `true`, la fila HS queda resuelta (con el mismo valor de retorno que «procesado» tenga para el
  llamador; hay que verificarlo, no suponerlo).
- **Misma semántica que en las filas de préstamo:** si la fila tiene AVPC, se aplican SOLO las AVPC
  (todo o nada por fila). Lo que la afectación no cubra queda sin aplicar y el control
  recibido = aplicado lo detecta. Por eso, antes de procesar, `crd/sql/334` verifica que las AVPC de
  la 492 cubran el 100 % de su fila. No se cambia a «AVPC + automático por el resto» esta noche: eso
  es la fase 1 (§3.2).
- **Fuera de este arreglo:** F2 (anticipo), F4 (filtro de estado), el seguro de incendio H1-H6, la
  pantalla y el «A4» del equipo de cuadre.

## 5. Decisiones del usuario

- **U1 — modelo:** M2 (destino del sobrante) como camino principal, manteniendo M1 para casos
  raros. *Recomendación del árbitro: sí.*
- **U2 — «siguiente cuota» por defecto:** ¿la cuota más antigua entre TODOS los préstamos activos
  del partícipe, o el operador elige el préstamo? ¿Entran los de plazo vencido (hoy excluidos por
  la decisión del 2026-09-01)?
- **U3 — devolución:** ¿se paga por la bandeja de Tesorería a la cuenta del partícipe? ¿Qué
  cuenta contable (contador)? ¿Se puede devolver dentro del mismo mes o se acumula?
- **U4 — tipo 22:** que deje de bloquear cuando el reparto por vigencias puede absorber el monto
  (atrasos y anticipo). *Recomendación: sí.*
- **U5 — fase 0:** autorizar el despacho ya, para reprocesar la carga 492.

## 6. Hallazgo del 2026-10-31: el mes del aporte no se ve

- `crearNuevoAporte` arma la glosa **siempre con el mes de la carga** («Mes 10/2026»), también en
  el reparto automático por devengo. La pantalla de aportes muestra la fecha de caja (APRTFCTR,
  último día del mes de la carga, por diseño) y la glosa. Resultado: un aporte que paga julio, o
  uno anticipado a noviembre, se ve como de octubre. El mes real está en `APRTPRDV`.
- Datos de la 492 corregidos con `crd/sql/338`.
- **Corrección de código pendiente:** (1) la glosa usa el mes de devengo cuando existe: «Aporte X -
  Mes {devengo} (pagado en {mes carga}) - CargaArchivo: N»; (2) la pantalla de aportes muestra una
  columna «Mes de devengo» (`periodoDevengo`); (3) la afectación manual a aporte reparte por mes
  (F1 bis), junto con el arreglo del anticipo (F2).

### 6.1 Especificación (orden del usuario del 2026-10-31: «que en la glosa salga el mes al que corresponde el aporte y el mes en que se corrió, y poder ver la fecha de caja en pantalla, aunque sea con un botón de ojo»)

- **Backend, glosa** (`CargaArchivoPetroServiceImpl.crearNuevoAporte`):
  - Con `periodoDevengo` presente: «Aporte {tipo} - Mes {M/AAAA del devengo} (pagado en
    {M/AAAA de la carga}) - CargaArchivo: {n}». Va igual aunque los dos meses coincidan.
  - Con `periodoDevengo` nulo (excedente de la afectación manual): «Aporte {tipo} - Sin mes de
    devengo (pagado en {M/AAAA de la carga}) - CargaArchivo: {n}».
  - Montos, fechas y lógica no cambian.
- **Backend, datos para la pantalla:** el endpoint que alimenta la tabla de aportes de
  `participe-dash` tiene que traer `periodoDevengo` (APRTPRDV). Si devuelve la entidad `Aporte`,
  ya viene, y Jackson lo serializa como arreglo `[2026,7,1]`. Si es un DTO, se agrega el campo.
- **Frontend** (`crd/forms/entidad-participe/participe-dash`), tabla de aportes:
  - La primera columna pasa a ser **«Mes del aporte»** = `periodoDevengo` en MM/AAAA. Si es
    nulo, muestra «—» con tooltip «sin mes de devengo». El ordenamiento usa ese campo.
  - La fecha de caja (`fechaTransaccion`) se ve en el **detalle del ojo** («Ver pagos de este
    aporte»): «Mes del aporte: MM/AAAA · Fecha de caja: dd/MM/aaaa», y como tooltip de la celda
    del mes.
  - El modelo `aporte.ts` agrega `periodoDevengo`. La fecha llega como arreglo de Jackson o como
    texto ISO: hay que aceptar las dos.

## 7. U3, devolución: DECIDIDA por el usuario (2026-11-01)
- **Cuenta:** la existente **`2.3.90.90.10 SOCIOS POR PAGAR`** (creada con `crd/sql/322`, la misma
  que usan los desembolsos). La misma cuenta para una devolución de aportes y una de préstamos.
- **Asiento al procesar la carga:** DEBE la cuenta puente de donde vino el dinero (`2.3.02.05`
  aportes por aplicar, o `2.3.02.10` préstamos por aplicar, según el producto de la fila) / HABER
  `2.3.90.90.10`. La puente queda en cero (control D-024 del cierre).
- **Pago:** se registra un pago programado al partícipe en la bandeja de Tesorería (un origen nuevo
  de la carga, como los demás pagos a partícipes). Tesorería lo paga **en el mismo mes o lo
  acumula**, cuando decida. El pago debita `2.3.90.90.10` contra el banco, por el circuito existente.
- **Implementación:** junto con el destino del sobrante (B8 del plan), después de que el usuario
  corra el DDL que lo soporta.
- **⛔ Ninguna cuenta fija en el código (regla del usuario, 2026-11-01): todas parametrizables por
  plantilla.** La devolución usa una **línea nueva de la plantilla 21** (aplicación Petro, la misma
  de `CrdLineaAsiento`/`ACTUALIZACION-PLANTILLA-21-PETRO-APLICACION.md`), con el siguiente `aux1`
  libre y una constante nueva en `CrdLineaAsiento` (p. ej. `DEVOLUCION_DESCUENTO_PARTICIPE`), que
  hoy apunta a `2.3.90.90.10`. Las puentes de origen salen de las líneas que ya existen
  (`APORTES_POR_APLICAR` aux1 3 → `2.3.02.05`, `PRESTAMOS_POR_APLICAR` aux1 4 → `2.3.02.10`). La
  semilla de la línea nueva la escribe el árbitro (patrón de `crd/sql/347`) y la corre el usuario
  antes del EAR. Si la línea falta, el asiento se detiene con un mensaje claro, igual que con las
  demás líneas de la 21 (`204a7950`), nunca con una cuenta de respaldo escrita en el código.
