# Traspaso — equipo `omen-saa-2` (cierre 2026-10-28)

Este archivo es para la próxima sesión del equipo 2 de SAA en la OMEN. Léelo primero. El historial largo
de frentes está en `docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md` (§1–§60); el trabajo de octubre está
en los documentos de diseño que se citan abajo.

## Alcance

- Árbitro: `omen-saa-2-arb`. Ejecutores: `omen-saa-2-be` (solo saaBE) y `omen-saa-2-fe` (solo saaFE).
  Se les despacha por SendMessage.
- Módulos: rhh, cxp, pagos, cnt, tsr, cxc y sri (sri se sumó el 2026-09-28). **No se toca: crd.**
  - rhh se comparte con `omen-saa-3`.
  - cnt y tsr se comparten con `lap-saa-1`.
  - Antes de tocar un archivo compartido: `git status` + `git log -3`.
- Prefijo de scripts: `e2-`. **El próximo libre es `e2-113`.** Marcador de commit: `omen2`. Índice:
  `docs/logica-negocio/INDICE-SCRIPTS-EQUIPO-2.md`.
- No son nuestros y no se tocan:
  - saaBE `docs/logica-negocio/crd/modelos/` (untracked);
  - saaFE `routes.json` (untracked).

## Terminado y verificado

- **NC de compra que excede el saldo** (correo de Katherine 27-10, GENERALI 002-002-000035370).
  - Spec: `docs/logica-negocio/cxp/NC-EXCEDE-SALDO-FACTURA.md`.
  - Código: saaBE `b170ee2b` y `d44618b8`; saaFE `ca3f269` y `5fa8d2d`.
  - `e2-109` corrido.
  - Ya está en producción: Katherine llegó a registrarla y le salió el error del usuario 1 (ver abajo).
- **Caso 1294 MRFOX** (retención anulada por el emisor): cerrado con `e2-108` + `e2-110`, los dos corridos.
  La factura de venta 830 quedó con saldo 1.531,86.
- **Botón Anular en Gestión de Documentos**: spec `cxp/ANULAR-DOCUMENTO-DESDE-BANDEJA.md`, saaBE `a4586614`,
  saaFE `90c1896`.
- **`e2-111` BLOQUE 1 corrido.** El BLOQUE 3 mostró `TSR.BNCO.BNCOREAL` en los 18 bancos.

## Terminado pero SIN verificar (solo compila / build)

- **Usuario real en los asientos de la bandeja de cxp** (`b1191051`). Si no llega usuario, rechaza.
  También mete el concepto de caja chica en las observaciones del asiento.
- **`idAsiento` en las respuestas** (`83e7a59f`) y botones «Imprimir asiento» (saaFE `647dd38`, `6727338`).
- **P5: tipos IN-A/EG-A/DI-A y módulo origen desde el 01-11-2026.**
  - Diseño: `cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md`.
  - Código: saaBE `e7103b60`; saaFE `b6824e4` (check «Banco real»).
  - **No se ha probado ningún asiento de noviembre.** El primer día hay que mirar: tipo, `ASNTMDOR`,
    `ASNTNMMD` y `ASNTNAMD`.
- **Usuario de sesión sin relleno**.
  - Diseño: `cxp/USUARIO-DE-SESION-PANTALLAS-CXP.md`.
  - Código: saaFE `6b88b20` (5 pantallas de cxp: arregla el «No se encontró el usuario con ID 1» de
    Katherine) y `5e20987` (28 pantallas de cxc, cxp y tsr).
  - Sin usuario en la sesión, la pantalla no graba y pide volver a ingresar.
  - **Falta desplegar el FE.**

## A medias

- **`e2-111` BLOQUE 2** (`cnt/sql/e2-111-…sql`, commit `c8127908`): marca como no reales los bancos 425, 427,
  432, 433, 435 y 438. El usuario los confirmó el 28-10.
  - Falta que lo corra y pase el control (esperado: 6 en 0, 12 en 1).
  - **Faltan también las salidas del BLOQUE 0:**
    - **0a:** los tipos 7/8/9 con `PLNTSSTM = 1` en cada empresa. Si no están así, desde el 01-11 no se graba
      ningún asiento automático. También dice si el 7 choca con el viejo TRANSFERENCIAS.
    - **0b:** la empresa de los bancos. `clasificarTipoAutomatico` busca bancos de la misma empresa del
      asiento; si no coincide, todo sale DI-A.
    - **0c:** asientos de noviembre (esperado 0).
- **VARFILES** (RUC 1793168450001, titular 86): Titulares muestra 5.000 de anticipos y el estado de cuenta no.
  - Script: `cxp/sql/e2-112-medir-anticipo-varfiles.sql` (solo lectura, `2add3fc1`). **Sin correr.**
  - Sospecha: el saldo de PRCC tipo 2 se cargó a mano y no tiene su fila en `PGS.ANTP`. Mismo patrón que
    `pagos/MIGRACION-CRUCES-ANTICIPO.md` §5.
  - Si se confirma, el arreglo es un `e2-113` que crea el ANTP «SALDO INICIAL MIGRADO» de 5.000, sin asiento.
    **Antes, contabilidad confirma que los 5.000 están en la 1.9.01.10.**
  - Mientras tanto, no se le paga el total de sus facturas: el anticipo no se descontaría.
- **Dos rellenos de usuario que quedaron fuera** (cxp/USUARIO-DE-SESION-PANTALLAS-CXP.md):
  - `app-state.service.ts:313-315` `getIdUsuario()` cae a 0. Lo usan la anulación de liquidaciones de cxc y la
    de consulta-documentos de cxp. Es un servicio compartido con los otros equipos: se les avisa antes de
    tocarlo.
  - `tsr` cierre-caja `getNombreUsuarioLocal()`: lee el JSON crudo de `usuario` como nombre.

## No empezado (pedidos de octubre, `docs/logica-negocio/PEDIDOS-CONTABILIDAD-2026-10-27.md`)

- **Q7 (ATS, notas de venta):** falta correr `e2-100` y desplegar.
- **Q8 (Excel de estados financieros):** comparar contra el PDF y contestarle a Jimena.
- **Q9 (formato de conciliaciones):** Ana no dijo qué cambiar. Hay que preguntarle.
- **Q11 (ATS 332):** espera la decisión de contabilidad (`e2-101`).
- **Q12 (CNEL julio):** espera a Ana (`e2-87`).
- **Q13 (centros de costo / IVA):** espera el pronunciamiento formal del auditor.
- **De otros equipos, avisados el 27-10 con autorización:** Q10 es de omen-saa-3; Q14, Q15 y Q16 son de
  omen-saa-1. No hubo respuesta registrada.
- La anulación desde la bandeja para factura, NC, ND y liquidación (hoy solo retención) podría tener el mismo
  defecto de dejar el documento activo. **Sin medir.**

## Decisiones tomadas hablando (no están en otro archivo)

- **NC que excede:** nunca deja la factura en negativo. El saldo de la NC se cruza contra otra factura o contra
  una ND del mismo proveedor, o el proveedor lo devuelve («Devolución Saldo a Favor»). La devolución registra
  a qué cuenta bancaria propia llegó el dinero. El saldo de la NC se ve en el estado de cuenta.
- **P5:**
  - Caja chica sale DI-A.
  - Los reversos conservan el tipo y el módulo del original.
  - crd entra en la regla desde el 01-11 (no se toca su código: alterno 1 → CRE).
  - La transferencia entre cuentas propias (neto 0 con banco en los dos lados) sale EG-A. Es default del
    árbitro; el usuario no lo objetó.
  - Neto 0 sin los dos lados sale DI-A.
  - El módulo de una liquidación de compra lo decide la existencia de DCXP: si existe, CXP; si no, CXC.
- **Bancos no reales:** los 6 del BLOQUE 2. Solo pesa para asientos con fecha ≥ 01-11.
- **Usuario de sesión:** nunca un relleno (1, 0, 'SISTEMA'). Sin usuario no se graba. El backend que exige
  usuario real (`nombreUsuarioObligatorio` en `ProcesoCargaDocumentosServiceImpl`) se queda así.

## Roto, y cómo se nota

- Hasta desplegar el FE `6b88b20`, registrar en Gestión de Documentos puede dar *«No se encontró el usuario
  con ID 1»*. **Workaround:** cerrar sesión y volver a ingresar en la misma pestaña.
- Si el 0a sale mal, desde el 01-11 todo asiento automático falla con un mensaje de tipo de asiento no
  encontrado.

## Pendiente del usuario

- **Bloqueante:**
  - Correr el BLOQUE 2 del `e2-111` y pasar el control.
  - Pasar el 0a, el 0b (con empresa) y el 0c.
  - Correr el `e2-112`.
  - Desplegar el FE (`6b88b20` y `5e20987`).
- **Decidible:**
  - Autorizar el aviso a omen-saa-1 y lap-saa-1 sobre el cambio central de asientos (P5).
  - Los dos rellenos de usuario que quedan.
  - Que contabilidad confirme los 5.000 de VARFILES.
- **Sin prisa:**
  - `e2-100`.
  - Contestarles a Jimena y a Katherine.
  - Preguntarle a Ana por las conciliaciones.
  - Firma del acta de la Superintendencia.

## Lo que reportaron los ejecutores al cierre (2026-10-28)

Los dos dijeron que **no les queda nada sin entregar**. El FE de Bancos («Banco real») está en saaFE `b6824e4`.

- **BE — nada se probó contra la base.** Las JPQL nuevas solo se validan en ejecución:
  - `ntcc/disponibles`;
  - `ntdc/pendientes`;
  - el `left join` de `dvpr/listar`;
  - la clasificación bancaria (`CuentaBancaria → banco.bancoReal`);
  - `selectMaxNumeroMesModulo`;
  - `selectByAlternoSistema`.

  Si una revienta, revienta al primer uso.
- **BE — sin `idAsiento`, fuera de alcance:** `recontabilizarDocumento` y `FacturaServiceImpl` ~l.2479. Los
  códigos HTTP 422/409 quedaron como estaban.
- **BE — la heurística de la liquidación de compra** depende de que cxc enlace su DCXP **después** de generar el
  asiento.
- **FE — botón «Imprimir asiento» que falta:**
  - en cxc `gestionar/anticipo`, tsr `anticipos-clientes` y en los gastos y el cierre de caja chica;
  - en las consultas que ya traen el PK: cxc `consulta-cobros`, tsr `seguimiento-anticipos` y
    `pagos-transferencia/seguimiento`;
  - en PagoProgramado y recontabilizar, que esperan `idAsiento` del backend.
- **FE — pregunta sin contestar:** si la rama «PENDIENTE DE CLASIFICAR» de `registrarBD` lleva también el botón
  del asiento y la `advertenciaAplicacion`.
- **FE — decisiones propias, avisadas y no objetadas:**
  - Anular desde la bandeja va tras `Permisos.CXP_MOTIVO`.
  - «Devolución Saldo a Favor» cambió solo el título y el menú; el *path* de la ruta es el mismo.
- **FE — inventario sin tocar:**
  - 7 componentes de tsr sin ruta;
  - rutas sin menú (cxc retenciones/gestionar, cxp transferencias-legacy);
  - en cnt, el Estado de Resultados y el Balance de Prueba están comentados en el menú;
  - rhh `procesos/aportes` es un esqueleto;
  - cxc retenciones v1 tiene la guardia pero no tiene menú.
- **FE — entorno:**
  - Build con `npx ng build --configuration development`.
  - Las ediciones masivas van con scripts Python desde el scratchpad. Una regex borró por error un bloque de
    `facturas-ingreso` y se restauró. **Hay que revisar el diff de cualquier edición masiva.**
