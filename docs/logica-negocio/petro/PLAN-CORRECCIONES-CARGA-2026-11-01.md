# Plan de correcciones de la carga y pendientes del equipo 1 (2026-11-01)

omen-saa-1-arb, por orden del usuario: «procesemos todos los pendientes de una vez». Este documento
es el contrato para los ejecutores. Cada ítem remite a su diseño, que manda en el detalle. Si el
código contradice lo que dice aquí, **el ejecutor para y reporta**, no improvisa.

**Base:** `main` fusionado (saaBE `2428217a` o posterior), con el código del cuadre. En
`CargaArchivoPetroServiceImpl` conviven el modo normal y el **modo reproceso** (`modoReproceso`,
cuadre). **Regla general: ningún ítem cambia el comportamiento del modo reproceso.** Si un cambio
del modo normal tocara un camino compartido con el reproceso, se limita con `!modoReproceso` y se
reporta.

## Decisiones del usuario que este plan da por tomadas

- **Mora (DISENO-CARGA-TARDIA-SIN-MORA.md §5), 2026-11-01:** MA sí, MB sí, MC sí, MD sí. ME sí:
  la mora sobre capital ya pagado se corrige **aparte**; no entra en este plan.
- **Rediseño de la afectación manual (DISENO-AFECTACION-MANUAL-V2.md §5)**, tomadas con la
  recomendación del árbitro por la orden «todos de una vez»:
  - **U1:** «destino del sobrante» por partícipe como camino principal; el reparto manual queda
    para casos raros.
  - **U2:** «siguiente cuota» = la cuota pendiente más antigua (vencimiento) entre los préstamos
    VIGENTE y EN_MORA del partícipe. Plazo vencido sigue fuera (decisión del 2026-09-01).
  - **U4:** el tipo 22 deja de bloquear cuando el reparto por vigencias absorbe el monto.
  - **U3 (devolución): DECIDIDA el 2026-11-01**: cuenta existente `2.3.90.90.10 SOCIOS POR PAGAR`, la misma para aportes y préstamos; pago en el mismo mes o acumulado (DISENO-AFECTACION-MANUAL-V2.md §7). Entra en el B8. (Texto anterior: «falta la cuenta contable del contador. No se implementa el
    destino devolución en este plan.
- **Estados 12/13:** pendiente de medir (`crd/sql/350`); no se toca el motor en este plan.

---

## BACKEND (omen-saa-1-be), en este orden

### B1 — Mora en la carga tardía (DISENO-CARGA-TARDIA-SIN-MORA.md §3)
- En `CargaArchivoPetroServiceImpl`, **antes** de cada llamada a
  `motorPagoPrestamoService.aplicarPago(...)` en `aplicarPagoNormalConMotor` (~5176) y en
  `aplicarAfectacionManualConRegistroPago` (~5620): si `fechaEfectiva < LocalDate.now()`, llamar
  `procesoMoraPrestamoService.recalcularMoraALaFechaDePagoDetalle(idPrestamo, fechaEfectiva)`,
  con `fechaEfectiva` = la fecha de `crearContextoPagoCarga` (~5206), es decir, el último día del
  mes de afectación.
- **Una vez por préstamo y por carga**: llevar un `Set<Long>` de préstamos ya recalculados en esa
  ejecución, local al proceso, nunca en un campo del bean `@Stateful`.
- **MC, contabilidad:** si `configuracionContabilidadService.contabilidadActiva()`, después del
  recálculo llamar `provisionInteresService.reversarExcesoProvisionPorCobroTardio(...)` y
  `reversarExcesoDevengoPorCobroTardio(...)` con el `detalleMora` devuelto, la empresa de la carga,
  la fecha de afectación = `fechaEfectiva`, el origen `"PETRO_TARDIO"` y el código de la carga.
  **Copiar exactamente el bloque de `CobroCreditoServiceImpl`** (búsqueda
  `recalcularMoraALaFechaDePagoDetalle` en ese archivo), incluida la acumulación de la mora
  eliminada y el `System.out` `[H42]`. Reportar si la reclasificación de bandas (tipo 8) aplica a
  la carga o solo a la caja.
- **Modo reproceso:** reportar si ya resuelve la mora por su cuenta (D-019/C4). Si sí, el B1 solo
  corre con `!modoReproceso`.
- **Concurrencia con el timer de las 02:00:** no se implementa; se reporta si se ve un riesgo
  concreto.

### B2 — Anticipo de aportes (F2) y afectación manual a aporte por mes
- **F2** (DISENO-AFECTACION-MANUAL-V2.md §2, `REGLAS-CARGA-PETRO.md` defecto abierto): en
  `esperadoMensual`, un mes posterior al mes de la carga **no** se responde desde el lote
  (`esperadoEnLote`, construido solo hasta el mes de la carga). Va a `esperadoDesdeCache`. Mismo
  resultado para los meses dentro del lote.
- **AVPC a aporte por mes**: `aplicarAfectacionAAporte` hoy crea UNA fila con `periodoDevengo =
  null` y tipo `EXCEDENTE_PETRO`. Pasa a repartir el `valorAfectar` **por mes, solo en el tipo de
  aporte de la AVPC**, con la misma regla que `distribuirAportePorDevengo`: desde el primer mes
  incompleto desde `ALCANCE_MINIMO_DEVENGO`, hasta el esperado de cada mes, anticipando lo que
  sobre. Cada tramo es una fila `APORTE_MENSUAL` con su devengo. Reusar la lógica de
  `distribuirAportePorDevengo` parametrizándola por tipo (un método compartido), no copiarla.
- Lo que no se pueda ubicar en ningún mes (sin vigencia del tipo) sigue la regla actual del
  sobrante: novedad bloqueante.
- **Modo reproceso:** el D-010 (excedente devuelto a aporte `EXCEDENTE_PETRO`) del cuadre **no
  cambia**.

### B3 — Aportes de la carga por carga real (no por `APRTIDAS`)
- `AporteDaoServiceImpl.selectByCarga` (control recibido = aplicado) y
  `sumValorPorTipoAporteByCarga` (asiento de aplicación) filtran hoy `a.idAsoprep = :idCarga`. Se
  agrega `and a.cargaArchivo is not null`, o se cambia a `a.cargaArchivo.codigo = :idCarga` si el
  javadoc del «porqué transitorio» no lo impide (leerlo y reportar cuál se eligió y por qué).
- **Objetivo:** un aporte sin carga real (migrado, cobros personales) nunca se suma a una carga.
- Los datos ya se limpiaron con `crd/sql/342`; esto es para que no se repita.

### B4 — Pendientes de la carga ARCH (DISENO-CARGA-RESPUESTA-ARCH.md §9.5)
- **A1 `.csv`:** aceptar `.csv` **solo** en la carga ARCH, en el REST de asoprep y al guardar el
  archivo físico (sobrecarga de `uploadFileToPath` o validación local en asoprep). **No** se toca
  `FileService.EXTENSIONES_PERMITIDAS`, que es compartido.
- **A6 parcial bloqueante:** en el reparto parcial (fila AH con todo lo descontado), registrar una
  novedad BLOQUEANTE explícita (`MONTO_INCONSISTENTE`, `montoEsperado` = lo enviado,
  `montoRecibido` = lo descontado), para que el operador reparta siempre, sea cual sea el aporte
  esperado.
- **A5 préstamo de la línea PDGA:** cuando el descuento es completo, la fila del préstamo tiene que
  aplicar al préstamo de su línea PDGA (`linea.getPrestamo()`), no al primero del producto. Si la
  `ParticipeXCargaArchivo` no tiene dónde guardarlo, reportar la opción mínima y **parar** antes de
  agregar una columna (sería DDL).
- **A4 PE con atraso:** para la filial ARCH con descuento completo, la fase 2 no compara la fila
  del préstamo contra una sola cuota (el monto es, por definición, lo enviado). Ninguna novedad de
  monto por eso.
- **A8 cédula:** antes de validar, quitar espacios (también NBSP), puntos, comas y guiones. Si una
  fila trae montos y la identificación sigue sin ser válida, **abortar** nombrando la fila (hoy se
  ignora en silencio).
- **Tarjeta:** el reparto parcial llena también `capitalDescontado` con lo descontado, para que la
  tarjeta muestre el total.

### B5 — Seguro de incendio (DISENO-AFECTACION-MANUAL-V2.md §2 H1-H6, §3.4)
- **H1:** con filas PH y PP, el HS se asigna a UNA fila: la del préstamo cuya cuota tiene el
  `valorSeguroIncendio` más cercano al HS; si empatan, PH antes que PP, como
  `montoConHsMergeado`. La otra fila se compara y se aplica sin HS.
- **H2:** una fila PH en $0 con HS aplica el HS igual.
- **H4:** HS huérfano con varios préstamos candidatos: se desempata por el valor del seguro de la
  cuota; solo si sigue ambiguo es novedad.
- **H6:** las novedades del HS huérfano se calculan en la fase 2 (vista previa), no al procesar.
- **Modo reproceso:** el D-023 del cuadre (HS sobre cuota sin seguro) y el C4f no cambian;
  reportar si alguno de H1-H6 ya quedó cubierto por ellos.

### B6 — Usuario SISTEMA en los asientos (`docs/equipos/saa-1.md`, «A medias: el usuario SISTEMA»)
- **Paso 1, solo lectura:** rastrear quién llama a la creación de asientos
  (`AsientoContableServiceImpl`, relleno `usuario != null ? usuario : "SISTEMA"`) y a la anulación
  (`AsientoServiceImpl.marcaAuditoriaAnulacion`), y clasificar el origen del usuario de cada
  llamador: viene del cliente, es el literal «SISTEMA» de un proceso automático, o puede llegar
  nulo. **Reportar la lista y PARAR**: hay un compromiso con el equipo 2 de pasarle sus llamadores
  (cxc/cxp/tsr) antes de subir el rechazo.
- Los pasos 2 y 3 (`IncomeException` si el usuario es nulo o vacío; 400 en el REST de cnt con
  «SISTEMA»/«SYSTEM» del cliente) se despachan después de la confirmación.

### B7 — Menores
- `CrdLineaAsiento.java` (~163): el comentario nombra `335_LINEAS_RENDIMIENTOS_…`, que ahora es
  `crd/sql/347_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql`.
- `validarArchivoPetro` (~1482 antes de la fusión): sacar el `selectByNombrePetro35("ALVAREZ
  TOAPANTA DAYUMA")` de depuración que corre en cada carga.

### B8 — Rediseño, parte 1: destino del sobrante (U1, U2, U4), SIN devolución
- **U4:** `validarAporteAH` (tipo 22) no marca BLOQUEANTE cuando el reparto por vigencias puede
  absorber el monto (atrasos o anticipo). Compara contra lo pendiente por vigencias, no contra un
  mes de `HistorialSueldo`.
- **Destino del sobrante (§3.1 M2):** **diseño de datos primero.** Hace falta guardar por
  partícipe y carga el destino elegido («siguiente cuota» o «aporte jubilación/cesantía»). El
  ejecutor **propone** dónde (columna o tabla, con el DDL exacto) y **para**: el DDL lo escribe el
  árbitro y lo corre el usuario antes de implementar.

---

## FRONTEND (omen-saa-1-fe)

### F1 — Usuario SISTEMA (`docs/equipos/saa-1.md`, ítems FE 1-8)
1. Diagnosticar por qué se pierde el usuario de sesión (`session-timeout.service.ts` ~507-512,
   `app-state.service.ts` ~339-341 borran `logged` y `username`).
2. `shared/services/usuario-sesion.ts` → `usuarioSesion()` sin el relleno `'SYSTEM'`: si no hay
   usuario, limpia la sesión y lanza error.
3. `authGuard` exige `logged` + usuario; `canActivateChild` en los menús padre.
4. Interceptor HTTP: un POST/PUT/PATCH/DELETE sin usuario se cancela, limpia la sesión, avisa «Tu
   sesión se perdió; vuelve a ingresar» y manda a /login. Excluye el login. Escucha `storage` y
   `visibilitychange`.
5. El login (`tryAutoRestoreFromSharedSession`) no entra sin username.
6. Quitar los rellenos `|| 'sistema'` en cnt y crd: `asientos-contables-dinamico.ts` (3),
   `generar-archivo-petro.component.ts`, `entidad-participe-info.component.ts` y
   `acciones-generacion-petro.service.ts`.
7. Antes de tocar `app-state.service.ts` (`getIdUsuario()` cae a 0, compartido con cxc/cxp),
   **reportar**: es un servicio compartido con el equipo 2.
8. `ng build`.

### F2 — Carga ARCH: plantilla en Excel
- El botón «Descargar plantilla» de `carga-aporte-back` sigue en CSV (con B4-A1, el CSV pasa a
  aceptarse). Agregar al recuadro el aviso: «la cédula como texto, para no perder el 0».

---

## Mediciones del árbitro (SQL de solo lectura, los corre el usuario)
- `crd/sql/350`: préstamos en estado 12 (ANULADO) y 13 (CANCELADO_POR_REESTRUCTURACION) y su peso
  en el motor de saldos.
- `crd/sql/351`: aportes que las cargas 448, 449, 450 y 492 mandaron a junio 2026: ¿legítimos o
  corridos?

## Cómo se entrega
- Un commit por ítem, hecho por el árbitro después de revisar el diff y compilar.
- `REGLAS-CARGA-PETRO.md` se actualiza en el mismo cambio que toca la carga (regla del CLAUDE.md).
- Después de B1-B5 se arma un EAR y se despliega **sin cargas en proceso**. El ensayo de la carga
  se hace con la de noviembre, o con una copia, nunca reprocesando en producción.

## Hallazgo para el reproceso (crd/sql/351 y 352, 2026-11-01): meses de aporte pagados DE MÁS
- **Qué se encontró:** meses de aporte (9/11) cuyo total por período efectivo supera el esperado
  de la vigencia, con filas puestas por cargas (`SAA_AH`). Por mes:
  - 2025-07: 2; 2025-08: 3; 2025-09 a 2025-12: 1 por mes; 2026-01: 4; 2026-02: 7; 2026-03: 8;
    2026-04: 13; 2026-05: 23;
  - **2026-06: 68 casos (3.501,88)**;
  - **2026-07: 616 casos (13.858,49)**;
  - 2026-08: 1.
- **Dos patrones:**
  1. **Duplicado exacto:** la carga 427 llenó julio y la 448 lo volvió a llenar con el mismo monto
     (p. ej. 115,55 + 115,55; 133,50 + 133,50). Lo mismo con 360/363/364/366/367/407/427 en meses
     anteriores.
  2. **Pequeño sobrante:** la 427 pagó lo esperado y la 448 agregó entre 0,50 y 5,00.
- **Decisión del usuario:** NO se corrigen en producción; se corrigen con el **reproceso
  definitivo** (escenario A, D-049), que vuelve a aplicar todas las cargas desde jun-2025 sobre la
  copia nueva.
- **Para el agente del reproceso, antes de reprocesar:** averiguar la causa. Si es un defecto del
  reparto que sigue en el código, el reproceso lo repite. Hipótesis a descartar:
  - que una carga se haya procesado o reprocesado fuera de orden (427 después de 448);
  - que la vigencia haya cambiado entre cargas (el patrón de 0,50 a 5,00 lo sugiere);
  - que el aportado del reparto no viera las filas de la carga anterior.

  El B2 (`71a08381`, anticipo con la vigencia) cambia ese mismo reparto en el modo normal, no en el
  reproceso.

## Regla transversal (usuario, 2026-11-01)
**Todas las cuentas contables que use cualquier ítem de este plan salen de una plantilla (`CNT.PLNS` /
`CNT.DTPL`, por `aux1`), nunca escritas en el código.** Si un ítem necesita una cuenta que la
plantilla todavía no tiene, el ejecutor PARA y pide la línea; el árbitro escribe la semilla y el
usuario la corre.

## ⛔ Prerrequisito de base para CUALQUIER WAR compilado de `main` desde el commit del A5
Desde el A5, la entidad `ParticipeXCargaArchivo` mapea `CRD.PXCA.PRSTCDGO`. **Antes de desplegar
cualquier WAR de `main` (de cualquier equipo) hay que haber corrido `crd/sql/354_DDL_PXCA_PRESTAMO_DE_LA_LINEA.sql`**;
si no, toda lectura de `CRD.PXCA`, también en las cargas de Petrocomercial, falla con ORA-00904.
Lista de prerrequisitos del próximo EAR (en orden): `353` (verificación, ya dio OK) → `354` (DDL) →
EAR.

## EAR de correcciones (2026-11-01 01:29)
- `F:\ears\asoprep\correcciones-1101\SaaEAR.ear`, SHA-256 `41BDDCC324C779439AD0A05230FF84955E522A5E67F77CD8E4708C8A70544998`.
  Se armó sobre el EAR de la fusión (`fusion\SaaEAR.ear`); solo cambian los dos WAR.
  - `saaBE.war` `2CE9D44598D877DBC120D33867517F21A872CA402BDC2E49672D512D96723D13`: saaBE `main` en `f7a5299c`
    (B1, B2, B3, B4 con A5, B5, B7).
  - `SaaFE.war` `9AA629027C0BB53C04F03B534D3A6053868252886AEDC2BC03CB716A6B520129`: saaFE `main` en `6e4e92d`
    (F1, F2). `web.xml` igual al de producción.
  - Copia partida en `correcciones-1101\partes`.
- **Prerrequisitos:** `353` (OK) y `354` (corrido y verificado: columna NUMBER nullable y la FK habilitada).
- **Desplegar sin ninguna carga en proceso.** Vuelta atrás: `fusion\SaaEAR.ear`. Ojo: con el EAR de
  la fusión, la columna `PRSTCDGO` sobra pero no estorba.
- **Después de desplegar, verificar el hash** de los dos WAR desplegados (procedimiento en
  `PASE-PRODUCCION-FUSION-2026-11-01.md` §5).
