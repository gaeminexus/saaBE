# Entrega del cuadre (laptop) para fusionar con `main`

**31-oct-2026.** Lo escribió el agente del cuadre de la Superintendencia (laptop), a pedido de omen-saa-1-arb.
Rama `cuadre` en saaBE y en saaFE. En saaBE la rama **ya viene fusionada con `origin/main`** por dos `git pull`
automáticos (ver §1). En saaFE no se hizo merge. La revisión y la integración a `main` las hace omen-saa-1-arb.

El inventario de origen es el del expediente del cuadre (`cuadreSuper/01-seguimiento/PASE-A-PRODUCCION.md`). Ese
repositorio es privado por confidencialidad del contrato. Lo que hace falta para fusionar y desplegar está copiado
en este documento.

---

## 1. Punto de partida

| Repo | Base | Commits propios (`origin/main..cuadre`, sin merges) | Compila |
|---|---|---|---|
| saaBE | **Ya fusionada con `origin/main` = `bb36d423`** (0 commits atrás). Antes de la fusión, el merge-base era `136d1f06` | 61 (lista en §8) | ✅ `mvn -q compile` del resultado fusionado (Maven 3.9.9 de `C:\tools`), sin errores |
| saaFE | merge-base `5e209876`. **Sin fusionar**: le falta `080e7da` (ARCH), que no choca | 5 (lista en §8) | ✅ `npx ng build --configuration development`, solo avisos de `darken()` que ya existían |

**⚠ La fusión de saaBE ya está hecha, y no por decisión de esta entrega.** El 31-oct, entre las 21:29 y las 21:35, se
hizo `git pull` sobre el `main` de la laptop. Quedaron dos merges automáticos sin conflictos textuales, `36e5867b` y
`f8b0a809`, que traen los 8 commits de `origin/main` desde `136d1f06` y los 3 de esta noche (`74f5f719`, `8ac1d97a`,
`bb36d423`). El líder decidió subir la rama así, ya fusionada, para no rehacer la fusión. **Lo que hay que revisar son
esos dos merges.** Revisado por el agente del cuadre:
- `aplicarAporteAH` quedó en el orden correcto: sin partícipe (reproceso) → excedente D-010 (reproceso) → **AVPC de
  `main` (`364dc1c8`)** → reparto por devengo;
- el resultado compila.

**No probado:** ninguna corrida de la carga sobre el resultado fusionado. Los choques de §2 son los que resolvió git.

No se sube nada ajeno al código ni a su documentación. Quedan fuera dumps, salidas de SQL, credenciales y datos de
partícipes. En los documentos de bancos reales (`c12-02`) aparecen números de cuenta institucionales de ASOPREP.

---

## 2. Archivos que chocaban con `origin/main` (git los fusionó en `36e5867b` y `f8b0a809`; revisar)

### saaBE

| Archivo | Qué le cambió el cuadre | Qué le cambió `main` | Cómo fusionar |
|---|---|---|---|
| `src/main/java/com/saa/ejb/asoprep/serviceImpl/CargaArchivoPetroServiceImpl.java` | +1.581/−33. Modo reproceso (`modoReproceso`, `aplicarPagosArchivoPetroReproceso`) con D-010 (excedentes devueltos a aporte `EXCEDENTE_PETRO`), D-019 (cuota del mes saldada sin pago vuelve a pendiente), D-022 (contrato y vigencia al corte, red de seguridad del tope de 60 meses), D-023 (HS sobre cuota sin seguro), filas sin partícipe o sin pago nombradas, invariante recibido = aplicado con resumen por categoría. **También en la carga normal:** C4e (los pagos anulados no saldan cuotas), C4f (HS sobre cuota sin seguro → novedad bloqueante), D-024/D-025 (complemento de apertura), `204a7950` (el AH nunca va a rendimiento y todo aporte tiene línea en el asiento), C17 (b) (`validarMovimientoCartera` del mes de afectación) | `364dc1c8`: AVPC en filas AH y en HS huérfano (= C21 §A1 del cuadre). `5b01f7e8`: carga ARCH | **Tomar el A1 de `main`**, no hay versión del cuadre de A1. Hay que revisar `aplicarAporteAH` y `procesarSeguroIncendioHuerfano`: los dos lados los tocaron. El cuadre insertó en `aplicarAporteAH` el registro sin partícipe (reproceso) y el excedente D-010 antes del reparto por devengo. Orden recomendado: sin partícipe → excedente D-010 → **AVPC de `main`** → reparto por devengo |
| `src/main/java/com/saa/ejb/crd/daoImpl/EntidadDaoServiceImpl.java` | Una línea (C15): en la consulta de aportes por entidad agrega `AND AporteVigenteSql.VIGENTE_SQL` para excluir los aportes anulados por un reproceso | ARCH (rol técnico 9000000 + `ENTDCDGO`) | Conservar las dos |
| `docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md` | §7 nueva (reproceso de una carga ya aplicada) y notas «en modo reproceso» en la tabla de omisiones | A1 y ARCH | Conservar las dos |

`EntidadDaoService.java`, `GeneracionArchivoPetroDaoService.java` y su Impl: **el cuadre no los tocó**. No chocan.

### saaFE

**No choca.** El cuadre no tocó `crd/forms/archivos-petro/carga/carga-aporte-back/`. Toca `detalle-consulta-carga`,
`cierre-cartera`, `cnt/forms/cierre-periodo`, `reporte-balance-general`, `asientos-contables-dinamico`,
`listado-asientos`, `reporte-listado-asientos`, `rpr/forms/reportes-super-bancos`,
`tsr/forms/generales/conciliacion-contable` y los servicios y modelos de cnt y rpr.

### Numeración de scripts que se repite (no da conflicto en git, pero confunde)

| Número | En el cuadre | En `main` |
|---|---|---|
| `crd/sql/332_` | `332_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql` (DDL + plantilla, va a producción) | `332_MEDICION_CARGA_492_ROL_1625_Y_ARCH.sql` (medición) |
| `crd/sql/335_` | `335_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql` (semilla, va a producción) | `335_PUENTE_RESPUESTA_ARCH_OCTUBRE_2026.sql` |
| `336_` | `reportes/sql/336_D035_REGISTRO_SUJETOS_G45.sql` | `crd/sql/336_APORTES_SIN_DEVENGO_CARGA_492.sql` |

Sugerencia: al fusionar, renombrar los del cuadre con el siguiente número libre y referirse a ellos siempre por el
nombre completo en el plan de pase.

---

## 3. DDL y semillas que necesita el código del cuadre

Rutas relativas a `docs/logica-negocio/`. «Ambiente del cuadre» es la base `saa-oracle-23ai` de la laptop (dump de
producción del 28-oct 09:12). Lo que se corrió ahí **no estaba en ese dump**, así que hay que suponer que **producción
no lo tiene**. Confírmalo con el control de cada script antes de correrlo.

| Orden | Script | Qué hace | Si falta | Producción | Ambiente del cuadre |
|---|---|---|---|---|---|
| 1.0 | `crd/sql/DDL-ANCP-COMPLEMENTO-APERTURA.sql` (`CK_ANCP_TPOO` 1..4) | Tipos de asiento de la carga | El asiento del complemento falla con ORA-02290 | ✅ corrido el 28-oct | ✅ |
| 1.1 | `../equipos/cuadre-ddl-produccion-29102026.sql` bloque 1 (`CK_DCBC_MDLD`) | Abono sin regenerar tabla (C16b) guarda 0 o −N | ORA-02290 en el abono C16b | ⏳ | ✅ |
| 1.2 | mismo archivo, bloque 2 (`UK_ANCP_VIGENTE`) | Índice único solo sobre las filas vigentes | Un re-proceso repetido choca con las filas reversadas | ⏳ | ✅ |
| 1.3 | `cnt/sql/C17-01-cierre-contable-y-bitacora.sql` | `CNT.PRDO.PRDOCNCR/PRDOFCCN/PRDOUSCN` + tabla `CNT.HCPR` | **ORA-00904 al leer cualquier `Periodo`**: rompe toda la contabilidad | ⏳ | ✅ |
| 1.4 | `crd/sql/332_C15_PLANTILLA_37_Y_SUBPROCESOS_8_9_10.sql` | `CK_ANCC_TPOO` 1..10 + plantilla 37 (provisión ⑧ y bandas ⑨) | Cierre de cartera al corte: sin provisión y ORA-02290 en el ⑨ | ⏳ | ✅ |
| 1.5 | `rpr/sql/C19-01-ddl-estructuras-de-renglones.sql` | Estructuras de renglones (B17 y flujo como configuración) | `/esrp` y `/esfo` responden error; `/dtmt/generarBalance` sigue igual | ⏳ | ✅ |
| 1.6 | `reportes/sql/336_D035_REGISTRO_SUJETOS_G45.sql` | `RPR.SG45` + semilla + HM45 | La G45 no se genera | ⏳ | ✅ (29-oct) |
| 1.7 | `reportes/sql/337_D038_REGISTRO_G41.sql` | `RPR.SG41` + semilla | La G41 no se genera | ⏳ | ✅ (29-oct) |
| 1.8 | `reportes/sql/338_D038_TIPO_PRESTACION_TPAP.sql` | `CRD.TPAP.TPAPTPPR` (J/C) | ORA-00904 en la consulta nativa de saldos por grupo J/C (`AporteDaoServiceImpl`, ~`:941`), que usa la generación de las G | ⏳ | ✅ (29-oct) |
| 1.9 | Dato: `UPDATE CRD.IGFN SET IGFNCMNM = 'SI'` o `'NO'` (lo decide ASOPREP) | El manual de catastros solo admite SI/NO | La G40 no se genera | ⏳ | ✅ |
| 1.10 | `cnt/sql/D043-01-asnh-observacion-2000.sql` | `CNT.ASNH.ASNHOBSR` a 2000 | ORA-12899 en la desmayorización | ⏳ | ✅ (29-oct) |
| 4.1 | `seguridad/sql/C17-FE-01-permisos-cierres.sql` | Permisos 1558–1561 | No aparecen las opciones de cierre y reapertura | ⏳ | ✅ |
| 4.2 | `crd/sql/335_LINEAS_RENDIMIENTOS_PLANTILLA_21.sql` | Plantilla 21: aux1 56 → 2.1.01.05.02 y 57 → 2.1.02.05.02 | **El asiento de la carga Petro se detiene** si llega un tipo de aporte sin línea (`204a7950`). **Imprescindible para la 492** | ⏳ | ⏳ **sin evidencia de que se haya corrido en ninguna base** |
| 4.3 | `rpr/sql/C19-02-semilla-b17.sql`, `C19-03-semilla-flujo-de-caja.sql` | Semillas del B17 (534 renglones) y del flujo de caja | Sin B17 ni flujo desde el sistema | ⏳ | ✅ |
| 4.4 | `rpr/sql/C19-04-estados-financieros-oficiales.sql` | Títulos y rubro 250 (razón social, RUC, firmantes) | Estados oficiales sin encabezado | ⏳ (faltan los firmantes) | ✅ |
| 4.5 | `cnt/sql/c11-01-centros-de-costo-asoprep.sql` | Centros de costo (solo los usa el cargador histórico C11) | — | opcional | ✅ |
| 4.6 | `cnt/sql/c12-01-vigencia-tipos-automaticos.sql` + `c12-02-bancos-reales.sql` | Vigencia de la regla IN-A/EG-A/DI-A por empresa (rubro 249) | Sin el rubro rige `TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO` = **2026-11-01** (igual que hoy en `main`) | **solo con decisión del líder** | ✅ (1236 = 2025-06-01) |
| 4.7 | `cnt/sql/c5-01-apertura-alterno-10.sql` | `TipoAsientos.APERTURA = 10` (7, 8 y 9 quedan para IN-A/EG-A/DI-A) | Solo lo usa el cargador de asientos manuales C5 | verificar si existe APERTURA | ✅ |

**No van a producción en este pase:** los scripts de control (`C17-02`, `333`, `334`, `petro/sql/reproceso-carga-352-controles.sql`,
`C14B_…`), `C19-generar-semilla-b17.js` (generador de la semilla, ya corrido) y C19-05 (de la rama C19b, que no se
sube; ver §6).

---

## 4. Propiedades JVM

| Propiedad | Si no está definida | Producción |
|---|---|---|
| `saa.cierres.permitirFueraDeOrden` | `false`: cerrar o reabrir fuera de orden da `CIERRE_FUERA_DE_ORDEN` | **No definir.** Es solo del re-proceso |
| `saa.cartera.alCorte` | `auto`: el cierre es «al corte» solo si el corte es anterior al fin del mes pasado, o si la solicitud manda `estadoAlCorte`/`arranque`. **El cierre mensual normal no corre ⑧ (provisión 1399), ⑨ (bandas) ni ⑩ (devengo de mora)**, igual que hoy | Decisión del líder (I-3). Recomendado: `siempre` |
| `saa.cartera.inicioDevengoMora` | El ⑩ cuenta la mora desde el vencimiento de cada cuota, sin piso | Recomendado `2025-05-31`. Mal formado da 500. Solo importa si corre el ⑩ |
| `saa.cartera.devengoMoraCierre` | `false`: el ⑩ solo corre al corte o en el arranque | Según I-3 |
| `saa.cartera.corteInclusivo` | `true`: en modo al corte, la cuota que vence el día del corte es vencida | Sin cambio |
| `saa.cartera.neteoPorPrestamo` | `auto`: neteo y complemento por préstamo solo en meses al corte; si no, D-024 por cuenta | Según I-3 |
| `saa.cartera.moraNoDevengadaAIngreso` | **`true`**: la mora cobrada que no estaba devengada va directo a ingreso (D-017) | **Cambia lo de hoy** (ver §5). Para mantener el comportamiento actual: `false` |

Las banderas por petición (`modoReproceso`, `reproceso`, `paraReproceso`, `estadoAlCorte`, `arranque`, `omitir*`,
`preliminar`, `regenerar`, `fechaContable`) no son configuración. Sin ellas rige el comportamiento de producción.

---

## 5. Lo que cambia en producción con solo desplegar

1. **Cierres bloqueantes (C17).**
   - Los meses con corrida de cierre de cartera **ejecutada y vigente** (`CRD.CRCT`, `CRCTIDST = 2`, `CRCTESTD = 1`)
     quedan cerrados para cartera. Cobros, abonos, cargas Petro y pensión con afectación en esos meses responden
     `CARTERA_CERRADA`. Revisar la lista con:
     `SELECT CRCTANOO, CRCTMESS, CRCTCDGO FROM CRD.CRCT WHERE PJRQCDGO = 1236 AND CRCTIDST = 2 AND CRCTESTD = 1 ORDER BY 1, 2;`
     Esperado: jul, ago y sep-2026. La carga 492 (oct-2026) pasa si octubre no tiene corrida ejecutada.
   - El período contable **no se cierra solo**. Después de C17-01, todos los períodos quedan con `PRDOCNCR = 0`
     (abiertos) hasta que alguien los cierre desde la pantalla.
   - **Las G40–G51 solo se generan para meses con la cartera cerrada** (C17 e,
     `GeneracionReportesServiceImpl.ejecutarGeneracion` → `validarReporteCartera`). Un mes abierto responde
     `REPORTE_MES_NO_CERRADO`. **Es un cambio de operación:** para generar las G de octubre hay que ejecutar antes el
     cierre de cartera de octubre.
   - Las anulaciones de asientos de un período cerrado se hacen por reverso en el primer período abierto.
2. **Carga Petro normal.**
   - Los pagos anulados ya no saldan cuotas (C4e).
   - Si se descuenta HS sobre una cuota sin seguro, sale novedad bloqueante (C4f).
   - Complemento de apertura en los cobros (D-024/D-025).
   - Todo aporte de la carga necesita línea en la plantilla 21; sin el script 4.2 el asiento se detiene.
   - Se valida `CARTERA_CERRADA` contra el mes de afectación.
3. **Cobros.**
   - D-017: la mora no devengada va a ingreso (propiedad `moraNoDevengadaAIngreso`, por defecto `true`).
   - Anulación de los pagos migrados que duplican un cobro (D-025).
4. **Estructuras G según los manuales de la Superintendencia (D-034, D-035, D-038, D-039).** Cambian:
   - el formato y el contenido de G40–G49;
   - la G45 compara contra `RPR.SG45` y la G41 contra `RPR.SG41`;
   - el orden de generación pasa a G46 → G45;
   - la G49 se calcula al corte.

   Requieren 1.6–1.9.
5. **Pantallas (saaFE):**
   - cierre y reapertura contable;
   - reapertura de cartera con motivo;
   - cierre de cartera para todo usuario con el permiso 1412 (antes, solo el usuario 1);
   - botón «Regenerar G»;
   - semáforo de controles en Cierre de Períodos;
   - leyenda PRELIMINAR/DEFINITIVO en el Balance General;
   - el listado de asientos muestra `ASNTCDGO` (D-036).
6. **Endpoints nuevos:**
   - `/crar/reprocesar/{id}`;
   - `/cierrecartera/*` con modo al corte;
   - `/prdo/cerrar|reabrir|estado|bitacora|controlCierre`;
   - `/asnt/cargarManuales*`;
   - `/historico/*` (cargas históricas C11 y C13);
   - `/pgpc/reprocesarPeriodo`;
   - `/esfo`.

   Además, `fechaContable` (opcional) en `/prst/aprobar` y en `/pgtr/lote/{id}/respuesta`. Ninguno lo llama la operación
   normal salvo las pantallas del punto 5.

---

## 6. Lo que está a medias o no debe ir todavía

- **C19b** (`cuadre/c19-balances-oficiales`, `6e85db88`): **no se sube**, por decisión del líder. `main` del cuadre
  queda con `/esfo` y `/esrp`, que es lo que llama el FE.
- **C6** (fecha contable en motores): compila y corre con la fecha de hoy cuando no se manda `fechaContable`, pero
  **sin prueba integral**.
- **`/prdo/controlCierre`**: parcial. Faltan cobros CBCR no procesados, cuotas saldadas sin pago y cuentas
  individuales. Con ERROR pide confirmación, **no bloquea**.
- **Decisiones del líder antes del pase:**
  - I-3 (modo del cierre de cartera);
  - vigencia c12;
  - roles de los permisos 1412, 1494 y 1558–1561;
  - `IGFNCMNM`;
  - firmantes de C19-04;
  - `moraNoDevengadaAIngreso`.
- **C21** (carga Petro de producción, `cuadre-c21-carga-petro-produccion.md` en esta carpeta): A1 ya lo resolvió
  `main` (`364dc1c8`). Quedan A2–A7 y los grupos B (controles del re-proceso en producción) y C (pantalla). No es parte
  de esta fusión.

---

## 7. Orden sugerido del pase

1. Respaldo (dump) de producción.
2. DDL 1.1–1.10.
3. WAR (verificar el hash del contenido desplegado contra `target/SaaBE.war`).
4. FE.
5. Semillas 4.1–4.4, más 4.6 y 4.7 solo con decisión.
6. Arranque limpio, sin ORA-00904 ni `ClassCastException`.
7. Pruebas de humo con un usuario que no sea el 1.
8. Carga 492 desde la pantalla.

Cada script se entrega al usuario como `.md` revisable con SELECT de control antes y después. **Ensayar primero
sobre un dump de producción de hoy, en un contenedor propio**, no en `saa-oracle-23ai`.

---

## 8. Commits

### saaBE (`origin/main..cuadre`, sin merges)

```
eb000501 2026-10-29 D-048: abono a capital en re-proceso evalua "al dia" a la fecha del abono
031c3014 2026-10-29 D-043: historial de la desmayorizacion por cierre contable y periodo DESMAYORIZADO abierto
7869890d 2026-10-29 D-044: cuota saldada sin cubrir su capital y G49 de junio contra la ejecucion en curso
be33fe77 2026-10-29 D-038/D-039: estructuras G segun los manuales de la Superintendencia (G40-G49)
91f2dccc 2026-10-29 D-035: G45 = sujetos nuevos contra el registro RPR.SG45 (base acumulada de la Superintendencia); tipo de participe P
c896042f 2026-10-29 D-034: G45 despues de G46 (orden de generacion), G49 al corte no borra de la G48 operaciones con saldo, sin asientos vacios
cc959fc4 2026-10-29 D-032: neteo 6 de cuotas por prestamo contra lo abierto y complemento de apertura por prestamo al cobrar
85a31c62 2026-10-29 pase a produccion: GET /prdo/controlCierre/{idEmpresa}/{anio}/{mes} (semaforo de cierre de mes, solo lectura)
6204ad5c 2026-10-29 C14b: el PGPRSLOT del abono a capital no paga la cuota en la cartera al corte
71383331 2026-10-29 C16b (abono sin regenerar tabla): regenerarTabla=false en el abono a capital del CBCR y del camino directo
38fe7c01 2026-10-29 C17 soporte FE: GET /prdo/bitacora/{idEmpresa} (bitácora CNT.HCPR, solo lectura) y permisos 1558-1561
a4d6842c 2026-10-29 C19: columnas CHAR(1) de la definicion de estructuras se leen como texto (ClassCastException Character→String al generar el B17)
204a7950 2026-10-29 C4 (rendimiento con asiento): el AH nunca va a un rendimiento y todo aporte de la carga tiene su linea
84c9cbb1 2026-10-28 C20 (pension mes pasado): POST /pgpc/reprocesarPeriodo - pension complementaria de un periodo pasado con el rol, D-019 en los cruces y pago con la fecha del banco
b3d6a57c 2026-10-28 C19: razon social del rubro 250 cabe en PDTRVLRV (100 caracteres)
e13c0b40 2026-10-28 C19: integracion con C17 (estado del periodo y B17 con mes cerrado) y documentacion
317b9fd8 2026-10-28 C4 (D-019 cobros): modo re-proceso en el motor de cobros - el prestamo se evalua vigente al corte del mes del cobro
a84bcc6f 2026-10-28 C19: estados financieros oficiales (/esfo): nombre propio, encabezado institucional, resultado del ejercicio, PRELIMINAR/DEFINITIVO
19fe6362 2026-10-28 C17: diseno CIERRES-BLOQUEANTES.md, SQL de control C17-02 y nota de contrato en API-CIERRE-CARTERA.md
b4e54852 2026-10-28 C17 (e): reportes regulatorios y de cartera solo para meses cerrados; estado del periodo para EEFF
03357c5c 2026-10-28 C17 (d): cierre y reapertura del periodo contable, reapertura de cartera y reglas de orden
947da679 2026-10-28 C17 (c): anulaciones de periodos cerrados por reverso en el primer periodo abierto
2b152696 2026-10-28 C17 (b): validaciones tempranas de mes cerrado en los procesos de modulo
a239ee32 2026-10-28 C19: semilla provisional del flujo de caja (formato BIESS) como estructura de renglones
2b851c02 2026-10-28 C19: motor de estructuras de renglones (B17 como configuracion) y semilla del B17
81ab7276 2026-10-28 C17 (a): estado de cierre central (contable y cartera) y barrera en la creacion de asientos
e31e0bb1 2026-10-28 C4 (D-024/D-025): complemento de apertura en los cobros, excedentes D-010 por 2.3.02.10 y anulacion de pagos migrados que duplican la carga
b3eaa3e5 2026-10-28 C15 (aportes netos en el cierre): lectura unica de aportes vigentes (AporteVigenteSql) en cierre, cuenta individual, G42/G43/G44, CJBM, CPRM y lecturas de produccion
2ddce695 2026-10-28 C4 (HS sin seguro en la cuota): el HS descontado sobre una cuota sin seguro de incendio se aplica como seguro (D-023) y ya no se pierde en silencio
0e6e4506 2026-10-28 C4 (D-022 1b): el caso 1b se decide con las vigencias del contrato, no con el lote; red de seguridad en el tope de 60 meses
2af81e0e 2026-10-28 C4 (D-022 vigencias): en reproceso, contrato activo sin vigencia del mes aplica el AH completo al mes de la carga
2a8d5eef 2026-10-28 C4 (pagos anulados): los pagos anulados no saldan cuotas; §4.9 sin condicion de fecha; filas sin pago nombradas; novedades del reproceso en la transaccion
f1364368 2026-10-28 C4 (D-022 aportes): en reproceso el contrato del aportante se resuelve al corte; sin contrato se crea
b8e56851 2026-10-28 C16 (D-021): precancelación con préstamo nuevo admite el tercer patrón migrado (SLOT y futuras en 7, total con el futuro)
11ce38c7 2026-10-28 C4 (rendimiento): reproceso de carga Petro lineal - contexto de persistencia acotado por unidad de trabajo
e81cf9b7 2026-10-28 C16 (D-021): desembolso neto con precancelación del préstamo anterior pagada con el préstamo nuevo
3c7345c6 2026-10-28 C4 (D-019 ampliada): en reproceso, la cuota del mes saldada sin pago vigente vuelve a pendiente al corte antes de validar
997ab14a 2026-10-28 docs (cuadre C15): CIERRE-CARTERA-A-FECHA-DE-CORTE.md, diseno, parametros, orden de un mes, SELECT de control y pendientes
022abdc5 2026-10-28 creditos (cuadre C15, D-018/H6): contabilizar el desembolso de prestamos aprobados sin asiento de entrega
ad7b6c3f 2026-10-28 reproceso Petro (cuadre C4, D-019): la operación se busca por su estado a la fecha de corte y se reabre si su cancelación es posterior
dea44022 2026-10-28 reportes (cuadre C15): G48 y CCPM a fecha de corte con el calculo de la provision del cierre; /ejrc/ejecutar con regenerar
66ea491f 2026-10-28 cartera (cuadre C15, D-017/H4): la mora cobrada no devengada va directo a ingreso
9c71a821 2026-10-28 cartera (cuadre C15): cierre de cartera a fecha de corte (H1), fechas al corte (H3), provision de cartera (H2/D-015/D-017), devengo de mora al corte (H4) y corrida de arranque (H5/
bcefc940 2026-10-28 historico(cuadre C13): carga histórica de nómina desde los roles del contador — /historico/validarHistoricoNomina y /cargarHistoricoNomina; por mes crea empleados faltantes, pe
fc7e28b6 2026-10-28 reproceso Petro (cuadre C4): D-010 excedentes devueltos a aporte, paso 1 sobre carga ya procesada y mora a fecha de negocio
31b8cd3d 2026-10-28 historico(cuadre C11, D-013): saldos iniciales de clientes y proveedores al 31-may-2025 y su reasignacion
d2dea66e 2026-10-28 historico(cuadre C11): referencia bancaria de 50 caracteres en pagos y cobros y sustento NULL en los grupos creados
fdefc0b3 2026-10-28 historico(cuadre C11, fase 3): notas de credito de compra y de venta + documento CARGA-HISTORICA-CXC-CXP.md
2fe2820d 2026-10-28 historico(cuadre C11, fase 2): ventas, cobros y retenciones recibidas de la carga historica
d049ddad 2026-10-28 historico(cuadre C11, fase 1): cargador historico de CxC/CxP - compras, retenciones emitidas y pagos por los motores
43c8202a 2026-10-28 cnt(cuadre C12): decision del lider (28-oct): Creditos y Nomina tambien salen IN-A/EG-A/DI-A con numeracion por modulo; se retira el conjunto de modulos excluidos. c12-02 pasa a se
ed53e8b7 2026-10-28 cnt(cuadre C12): la vigencia de la regla IN-A/EG-A/DI-A y numeracion por modulo deja de ser la fecha fija 2026-11-01 y se lee por empresa del rubro 249 (CNT_PARAMETROS_ASIENTOS_AUT
1f008a8b 2026-10-28 petro(cuadre C4): reproceso de una carga Petro ya aplicada — POST /rest/crar/reprocesar/{idCarga}
fc9c9e5e 2026-10-28 docs(c6): FECHA-CONTABLE-REPROCESO.md, auditoria de la fecha de negocio en los motores y como re-procesar un mes pasado
dbc652b4 2026-10-28 cnt(cuadre C5): APERTURA pasa al codigo alterno 10 (7, 8 y 9 quedan para IN-A/EG-A/DI-A)
90afd06f 2026-10-28 tsr(c6): el movimiento bancario de conciliacion se fecha con la fecha del asiento que lo origina, no con la de hoy
67599b21 2026-10-28 cxp(c6): POST /pgtr/lote/{id}/respuesta acepta fechaContable: la fecha de respuesta del banco deja de ser siempre hoy
3545e57a 2026-10-28 crd(c6): POST /prst/aprobar/{id} acepta fechaContable opcional para la orden de pago del desembolso
f0fd3728 2026-10-28 crd(c6): pension complementaria con una sola regla de fecha del periodo; el asiento del seguro medico y las ordenes de pago dejan de fecharse hoy
14337d9b 2026-10-28 crd(c6): la cuota que queda PAGADA al autocorregir o reconstruir desde PGPR toma la fecha del ultimo pago vigente, no la de hoy
90948b14 2026-10-28 cnt(cuadre C5): cargador de asientos manuales desde CSV con rechazo total y trazabilidad
```

### saaFE (`origin/main..cuadre`, sin merges)

```
9f3343b 2026-10-29 D-038: la base de calculo de la G41 es Caracter(1) segun el manual de catastros; se exporta como texto (antes salia NaN)
6e944d7 2026-10-29 D-036: listado y reporte de asientos muestran el codigo del asiento (ASNTCDGO) en lugar del consecutivo interno ASNTNMRO, que no se re-numera
c29285b 2026-10-29 pase a produccion: pantallas y controles del cierre (sobre la integracion de c17)
6a6000c 2026-10-29 C17 FE: pantallas de cierre y reapertura contable, reapertura de cartera, anulación con reverso y leyenda PRELIMINAR/DEFINITIVO
7a44a07 2026-10-28 C19: estado financiero oficial (PDF/Excel) desde el balance y PDF con el nombre del estado
```
