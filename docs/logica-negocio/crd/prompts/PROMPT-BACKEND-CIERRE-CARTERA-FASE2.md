# PROMPT — Agente BACKEND · Fase 2: proceso mensual de apertura / cierre de cartera

> **Etiqueta: BACKEND** (repo `saaBE`). **Depende de la Fase 1**, que ya está entregada:
> `CRD.CBPR`/`CRD.BNDP` existen y están cargadas en la BD local de docker
> `saa-oracle-23ai` (28 configuraciones, 143 bandas), con `ClasificadorBandaService`
> funcionando. **NO rehagas nada de Fase 1.** El prompt de FRONTEND de esta fase se
> escribe después, cuando el contrato de API esté lleno.

---

Implementa el **proceso mensual de apertura/cierre de cartera** del módulo de créditos: el
proceso que hoy no tiene pantalla y que alimenta contabilidad desde CRD cada mes.

## Lectura obligatoria antes de tocar código

1. `CLAUDE.md` (raíz) — convenciones. Recuerda: **no puedes compilar con Maven** (`mvn` no
   está en PATH; compila el usuario en Eclipse); serialización Jackson y formato de fechas;
   estilo de error; trazas `System.out.println` en REST/Service.
2. `docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` — **fuente de
   verdad del negocio**. Para esta fase, obligatorio: §3.2 completo (los seis sub-procesos
   ①–⑥ con sus asientos y ejemplos numéricos), §5 (reglas transversales), §6.3 (algoritmo de
   reclasificación de bandas), §2 (plan de cuentas), §9.1 (decisiones cerradas — **no
   re-preguntes nada de esa lista**).
3. `docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md` — contrato ya escrito en Fase 1 y el
   formato al que debes añadir los endpoints nuevos.
4. `src/main/java/com/saa/ejb/rhh/serviceImpl/ContabilizacionNominaServiceImpl.java` —
   **el patrón de referencia de extremo a extremo** para generar asientos desde otro
   módulo: `resuelvePlantilla(codigoAlterno, ...)`, `selectByPlantillaYAuxiliar`,
   comprobación de cuadre, línea de ajuste por redondeo, y previsualización.
5. `src/main/java/com/saa/ejb/cnt/service/AsientoContableService.java` — usa
   `generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones, usuario,
   List<DetalleAsiento> lineas)`. **No construyas `Asiento` a mano.**

## Alcance

**SÍ — los seis sub-procesos del cierre mensual (§3.2):**

| # | Sub-proceso | Resumen |
|---|---|---|
| ① | Asiento de vencidos | capital no pagado del mes anterior: sale de la banda 1 de POR VENCER, entra a la banda 1 de VENCIDO |
| ② | Cambio de bandas (por vencer) | reclasificación por diferencias entre la distribución nueva y la contabilizada |
| ①.1 | Reclasificación de vencido | igual que ②, sobre las bandas de VENCIDO |
| ③ | Apertura del período de crédito | D `1.4.05.05`/`1.4.05.10` → H `2.3.02.05`/`2.3.02.10` |
| ④ | Devengo de intereses | D `1.4.02.xx` (ordinario y mora) → H `5.1.02.xx`, por producto |
| ⑥ | Neteo de planillas / cierre | reversa lo NO cobrado: D `2.3.02.xx` → H `1.4.05.xx`, fecha = último día del mes anterior |

**NO en esta fase:** ⑤ Seguros (entra por CxP como factura, no lo genera CRD); pagos Petro y
manuales, jubilación, cruces, abonos (Fase 3); saneamiento completo de plantillas (Fase 4,
salvo el recorte que se indica abajo).

---

## 1. Decisión de diseño que debes respetar: el saldo previo sale de un SNAPSHOT, no del mayor

Los asientos ② y ①.1 registran **diferencias** entre la distribución nueva por banda y la
anterior. Esa "anterior" **NO** se lee de la mayorización contable (`CNT.DTMY`): el mayor
mezcla lo que escriben otros procesos sobre las mismas cuentas (pagos, entregas,
novaciones), y usarlo haría que el asiento de reclasificación arrastre movimientos ajenos.

**Guarda un snapshot por corrida**: la distribución de capital por (producto, tipo de
cartera, banda) que el proceso calculó y contabilizó ese mes. La corrida siguiente compara
contra ese snapshot. Ventajas: reproducible, auditable, y permite reversar.

## 2. Tablas nuevas (DDL como entregable)

Diseña dos tablas siguiendo `docs/estandar/ESTANDARES-CREACION-TABLAS-ORACLE.md`:

- **Cabecera de corrida** — una fila por (empresa, año, mes): estado de la corrida
  (preparada / ejecutada / reversada), fecha de proceso, fecha de corte, usuario, y
  auditoría estándar.
- **Snapshot / detalle** — una fila por (corrida, producto, tipo de cartera, banda):
  el capital contabilizado en esa banda, más la referencia al asiento generado.

Guarda también, por corrida, **qué asiento se generó para cada sub-proceso** (①, ②, ①.1,
③, ④, ⑥) para poder reversar y para trazabilidad.

**Nomenclatura — regla que ya nos costó una corrección:** los descriptores de 4 letras NO
se inventan. Antes de nombrar cada columna, busca el concepto en las entidades existentes
(`grep -rho 'name = "[A-Z]\{4\}XXXX"' src/main/java/com/saa/model`) y usa el descriptor
dominante. Verificados: vigencia = `FCIN`/`FCFN`; cantidad = `CNTD`; número = `NMRO`;
nombre = `NMBR`; período (id) = `PRDO`; año = `ANOO`; mes = `MESS`; estado = `ESTD`;
auditoría CRD = `FCRG/USRG/IPRG` + `FCMD/USMD/IPMD`. **Estado: 1 = activo, 0 = inactivo**
(`com.saa.rubros.Estado`), no 2. Propón los nombres de tabla de 4 letras y **verifica que
no colisionen** (`SELECT owner, table_name FROM all_tables WHERE table_name IN (...)`).

**Quién escribe el DDL — regla del proyecto:** el DDL que va a pruebas y producción **NO lo
entregas tú**. Lo escribe y lo verifica el orquestador, porque es el único artefacto que el
usuario ejecuta a mano en producción y necesita un solo camino de revisión (el estándar real
de descriptores vive en las entidades, no en el documento de estándar, y un defecto
silencioso ahí no se detecta leyendo un resumen).

Lo que sí te toca:
1. **Propón el modelo**: lista de tablas con su propósito, y por cada una las columnas con
   tipo, obligatoriedad, semántica y las FKs. Propón nombres de 4 letras y verifica que no
   colisionen (`SELECT owner, table_name FROM all_tables WHERE table_name IN (...)`).
   Justifica cada columna: si no la usa el código, sobra.
2. **Crea las tablas en la BD local de docker** (`docker exec saa-oracle-23ai ... sqlplus -s
   system/saa123@FREEPDB1`) para poder implementar y probar. Ese `CREATE TABLE` es tuyo y es
   desechable: sirve para trabajar, no es el entregable.
3. **Deja el modelo documentado** en tu informe final. El orquestador escribe a partir de él
   el `DDL-CIERRE-CARTERA.sql` definitivo (SQL puro, sin comandos SQL*Plus, con controles),
   lo contrasta contra la BD y contra el estándar, y lo entrega al usuario.

Si tu implementación necesita cambiar el modelo sobre la marcha, cámbialo en local y
**dilo explícitamente en el informe**: la diferencia entre lo que propusiste y lo que
acabaste usando es justo lo que se pierde si no se reporta.

## 3. Lógica del proceso

### Reglas de negocio que no puedes cambiar (§5 del levantamiento)

- **Fecha de corte:** cuotas/intereses pendientes con fecha `<=` último día del mes a cerrar.
- **Fecha del asiento de neteo/cierre:** último día del mes anterior.
- **Solo el CAPITAL se distribuye por bandas.** Intereses, mora y seguros van a cuentas
  propias del producto, resueltas por plantilla.
- **Interés de mora:** existe un proceso diario que lo calcula (02:00), pero el asiento se
  genera **con el cierre**; contabilidad solo necesita, a fin de mes, lo generado global en
  el mes, lo cobrado y lo pendiente.
- Todo asiento debe **cuadrar D = H**; comprueba el cuadre antes de llamar a
  `generarAsiento` y ajusta el descuadre por redondeo contra la línea de cuadre, como hace
  RHH. Si no cuadra por otra razón, falla con `IncomeException` explicando la diferencia.

### Clasificación

Reutiliza `ClasificadorBandaService` de Fase 1 — **no reimplementes la derivación de
rangos**. Días: POR VENCER = del corte al vencimiento; VENCIDO = del vencimiento al corte.
El producto sale del préstamo (`Prestamo.producto`), la empresa del contexto de la corrida.

### Trampa conocida del módulo (CLAUDE.md)

El estado vigente de `Prestamo` es **`PRSTIDST`** (`idEstado`), no `ESPSCDGO`; el de
`DetallePrestamo` es **`DTPRESTD`** (`estado`), no `DTPRIDST`. Elegir la columna equivocada
devuelve resultados vacíos o silenciosamente incorrectos. Contrasta la distribución de
ambas columnas antes de fijar el filtro.

### Cuotas y montos

`CRD.DTPR` (`DetallePrestamo`) trae: `DTPRNMCT` (número de cuota), `DTPRFCVN` (vencimiento),
`DTPRCPTL` (capital), `DTPRINTR` (interés), `DTPRMRAA` (mora), `DTPRDSGR` (desgravamen),
`DTPRVLSI` (seguro incendio), `DTPRSLCP` (saldo capital), `DTPRCPPG`/`DTPRINPG` (pagado),
`DTPRFCPG` (fecha de pago), `DTPRESTD` (estado). Determina "no pagado" por los campos de
pago/saldo y el estado, no por suposición: verifica primero contra datos reales de la BD
local qué combinación identifica una cuota pendiente.

## 4. Idempotencia, previsualización y reverso — obligatorio

- **Previsualizar** una corrida sin grabar: devuelve, por sub-proceso, las líneas que se
  generarían (cuenta, descripción, debe, haber) y sus totales. Es lo que contabilidad va a
  revisar antes de autorizar; el patrón está en RHH.
- **Ejecutar**: transaccional; si un sub-asiento falla, no queda la corrida a medias.
  Ejecutar dos veces el mismo mes debe fallar con mensaje claro, no duplicar asientos.
- **Reversar**: anula los asientos de la corrida y devuelve el snapshot al estado anterior,
  dejando rastro (no borres filas).
- **Consultar**: estado de la corrida de un mes, con los asientos generados y sus totales.

## 5. Plantillas contables — recorte de Fase 4 que necesitas ahora

Las líneas que NO son de banda (por cobrar `1.4.05.xx`, por aplicar `2.3.02.xx`, intereses
`1.4.02.xx`, ingresos `5.1.02.xx`) deben resolverse por **plantilla**, no cableadas:
`PlantillaDaoService.selectByAlterno(alterno, empresa)` +
`DetallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, auxiliar1)`.

Problema: hoy los `auxiliar1` de las plantillas CRD son **posicionales** (1..N por orden),
no semánticos. Necesitas:

1. Crear la interfaz de constantes del catálogo de líneas de CRD (equivalente al rubro 214
   `RHH_LINEA_ASIENTO`), p.ej. `com.saa.rubros.CrdLineaAsiento`, con un código por papel:
   aportes por cobrar, préstamos por cobrar, aportes por aplicar, préstamos por aplicar,
   interés ordinario, interés mora, ingreso ordinario, ingreso mora, línea de cuadre…
2. Entregar un **MD revisable** (`docs/logica-negocio/crd/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md`)
   con los `SELECT` de control primero y luego los `UPDATE`/`INSERT` que fijan esos
   `auxiliar1` en las plantillas que usa este proceso (alternos 1 y 33 al menos, más las
   que crees para ① ② ④), y las filas del rubro si el catálogo se guarda en BD. **No
   ejecutes cambios de datos en producción; el usuario los corre.** Puedes aplicarlos a la
   BD local para probar, diciéndolo.
3. Documenta qué plantillas faltan y cuáles creas.

## 6. Capa REST y contrato

Endpoints de proceso (ruta a tu criterio siguiendo el patrón de la casa, p.ej.
`/rest/cierrecartera/...`): previsualizar, ejecutar, consultar, reversar. Estilo de la
casa: `@EJB`, traza al inicio, `catch (Throwable e)` → 500 con `"Error ...: " + mensaje`;
las validaciones lanzan `IncomeException`.

**Obligación continua:** registra cada endpoint en `API-BANDAS-PRODUCTO.md` (o en un
documento hermano `API-CIERRE-CARTERA-PRODUCTO.md` si lo prefieres, enlazándolo desde el
primero) **en el mismo cambio**, con request y response en JSON reales construidos sobre
datos de la BD local, errores y validaciones. El frontend construirá la pantalla leyendo
solo eso.

## 7. Verificación y entrega

- Compila como puedas (hay `javac 21`; el classpath se arma con `~/.m2` + los módulos de
  WildFly) para descartar imports y firmas — no sustituye el build de Eclipse del usuario.
- **Prueba el cálculo contra datos reales de la BD local**: corre la previsualización de un
  mes con cartera real y verifica que cada sub-asiento cuadra D = H y que las cuentas de
  banda coinciden con las de `CRD.BNDP`. Reporta los totales que obtuviste.
- Actualiza §10 del levantamiento con el estado de la Fase 2.
- Cierra con: archivos creados/modificados, endpoints, scripts entregados, resultados de la
  prueba con datos reales, y **toda decisión que tomaste sin respaldo documental marcada
  como PENDIENTE DE VALIDAR** — no la escondas.
