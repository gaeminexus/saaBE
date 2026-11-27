# C50 — Las G40–G51 atadas a la corrida de cartera vigente

Equipo omen-saa-1 · 2026-11-27 · lote 29, punto 1.2 del encargo de controles de producción. Va también a `ear/siguiente` por cherry-pick (sin C19b).

## 1. El defecto (verificado en el código antes de programar)
- `CierreCarteraServiceImpl.reversar` no menciona `RPR.EJRC`/`EJRD` en ninguna parte (comprobado con búsqueda en el archivo): reversar la corrida de cartera no toca las G ya generadas.
- `GeneracionReportesServiceImpl.ejecutarGeneracion` sin `regenerar:true` toma la ejecución más reciente del mes y, si está `COMPLETO`, responde «ya fueron generados».
- El semáforo (`ControlCierreServiceImpl.ultimaEstructura`) toma «la última G OK» sin mirar si es anterior a la corrida vigente.
- Caso real (re-proceso de julio 2026): se reversó la corrida 266, se ejecutó la 267 y las G 865 y 866 siguieron pareciendo válidas; hubo que regenerar a mano (867).
- Defecto accesorio encontrado: `EjecucionReporteDaoServiceImpl.selectByMesAnio` ordenaba por `fechaGeneracion desc` (un `LocalDate`): con varias generaciones el mismo día, «la última» salía en orden indeterminado. Se agregó `, e.codigo desc`.

## 2. Qué cambia
### 2.1 Columnas nuevas en `RPR.EJRC` (nullable; `sql/413`)
| Columna | Campo (`EjecucionReporte`) | Contenido |
|---|---|---|
| `EJRCCRCT` | `corridaCartera` | corrida de cartera (CRD.CRCT) vigente del mes al generar: `EJECUTADA` y activa |
| `EJRCFCUA` | `ultimoAsientoRegistro` | fecha de ingreso (`ASNTFCPR`) del último asiento vigente (estado 1 o 3) del mes con líneas en 1.3, 1.4 o 2.1, al generar |
| `EJRCFCHG` | `generadaEn` | instante de generación (`EJRCFCGN` solo trae la fecha) |
Se llenan en `GeneracionUnReporteServiceImpl.crearEjecucion` (ya `REQUIRES_NEW`), **antes** de generar: un asiento que entre durante la generación cuenta como posterior (lado seguro).

### 2.2 Estado de una ejecución: se calcula al consultar, sin triggers (`ReglaActualizacionG`, regla pura)
- **SIN_DATO**: la ejecución no tiene corrida grabada (generada antes del C50). Ni vigente ni desactualizada: el histórico no se rompe.
- **DESACTUALIZADA (a)**: su corrida ya no es la vigente del mes (reversada, o reemplazada por otra).
- **DESACTUALIZADA (b)**: existe un asiento vigente del mes en 1.3, 1.4 o 2.1 con fecha de ingreso posterior al último que había al generar (si no había ninguno, posterior al instante de generación). El motivo trae el número y la fecha contable de ese asiento.
- **VIGENTE**: ninguna de las dos.
`ActualizacionEjecucionGService` (`evaluar`, `evaluarUltima`, `atar`) hace las consultas: corrida vigente = `MAX(CRCTCDGO)` con `CRCTESTD = 1` y `CRCTIDST = EJECUTADA` (igual que `estadoDelMes` del semáforo); último asiento = el de mayor `ASNTFCPR` del mes (por `ASNTFCHA`).
No se marca nada al reversar: la regla (a) lo detecta sola al consultar (reversar la 267 deja la ejecución atada a ella desactualizada). No hay empresa en `RPR.EJRC`; como el resto de la generación de las G, el control es del mes (ASOPREP opera con una sola empresa).

### 2.3 `/ejrc/ejecutar`
Si la última ejecución del mes está DESACTUALIZADA (cualquier estado: completa, con novedades o en proceso), **regenera sin pedir `regenerar:true`**: crea una ejecución nueva y su `observaciones` empieza con «REGENERADA SIN PEDIRLO: la ejecucion N estaba DESACTUALIZADA. <motivo>». La respuesta ya devuelve la ejecución, así que el aviso llega sin cambiar el contrato. Si está vigente o sin dato, sigue como hoy («ya fueron generados»; corrección de pendientes).

### 2.4 Semáforo `/prdo/controlCierre`
En `controlesB17G`, antes de leer la «última G OK»: si la última ejecución del mes está desactualizada, sale un control `G_DESACTUALIZADAS` (grupo B17_G) en **ERROR** con el motivo, y no se comparan G42/G48 contra el B17 (compararían contra una G vieja). Vigente o sin dato: todo igual que antes.

### 2.5 Lo que NO hace
No bloquea `/prdo/cerrar` (el candado es el punto 1.1, en el lote siguiente, y se apoyará en `ActualizacionEjecucionGService.evaluarUltima`). No anula asientos ni toca las G: solo las marca y las regenera a pedido. Un asiento que se **anula** después de generar no cambia su fecha de ingreso y no se detecta (el que lo anula con otro asiento, sí).

## 3. Archivos
- `model/rpr/EjecucionReporte.java` (tres campos); `ejb/rpr/daoImpl/EjecucionReporteDaoServiceImpl.java` (desempate por código).
- Nuevos: `ejb/rpr/service/ReglaActualizacionG.java`, `ActualizacionEjecucionGService.java`, `ejb/rpr/serviceImpl/ActualizacionEjecucionGServiceImpl.java`.
- `GeneracionUnReporteServiceImpl` (ata al crear), `GeneracionReportesServiceImpl` (regenera sola), `cnt/serviceImpl/ControlCierreServiceImpl` (control).
- `docs/logica-negocio/rpr/sql/413_C50_EJRC_CORRIDA_Y_ULTIMO_ASIENTO.sql` (**va antes del EAR**, no ejecutado); prueba `src/test/java/com/saa/ejb/rpr/service/ReglaActualizacionGPruebas.java` y `tools/pruebas/correr-pruebas-c50.sh`.

## 4. Compatibilidad con `origin/ear/siguiente`
Verificado con `git diff origin/main origin/ear/siguiente -- <archivo>`: `EjecucionReporte`, `EjecucionReporteDaoServiceImpl`, `GeneracionUnReporteServiceImpl` y `GeneracionReportesServiceImpl` son idénticos. `ControlCierreServiceImpl` difiere en 3 hunks (C19b: líneas ~45, ~500 y ~536); este lote toca solo dos bloques fuera de esos hunks (el `@EJB` junto al de `controlCuotasSinFechaService` y el bloque antes de `Long ejrdG42 = ultimaEstructura(…)`), cuyo contexto es igual en las dos ramas. El `sql/413` es independiente.

## 5. Prueba
`mvn -o -q compile`; `sh tools/pruebas/correr-pruebas-c50.sh` (13 verificaciones: sin dato, corrida reversada, 266 → 267, vigente con y sin asientos, asiento posterior); C33 (109/0), C46 (30/0) y C48 (11/0) siguen pasando.
**Aceptación del cuadre (en la copia de julio, no corrida aquí):**
1. Con el `sql/413` aplicado y el código desplegado, `POST /ejrc/ejecutar` de julio deja una ejecución con `EJRCCRCT` = la corrida 267 vigente.
2. Reversar la corrida 267: `GET /prdo/controlCierre/{empresa}/2026/7` trae `G_DESACTUALIZADAS` en ERROR («se genero con la corrida de cartera 267 y el mes ya no tiene una corrida de cartera vigente»).
3. Ejecutar de nuevo el cierre de cartera (corrida nueva) y `POST /ejrc/ejecutar` sin `regenerar`: crea una ejecución nueva atada a la corrida nueva, con «REGENERADA SIN PEDIRLO…» en las observaciones.
4. Registrar un asiento en 1.3/1.4/2.1 de julio después de generar: la ejecución pasa a DESACTUALIZADA con el número y la fecha de ese asiento.

## 6. Pendiente / no verificado
- Nada se corrió contra base ni WildFly. Las consultas nativas (corrida vigente, último asiento con `ROWNUM`) se escribieron contra las columnas que ya usan otras consultas del repo (`CRCTCDGO/ANOO/MESS/ESTD/IDST`, `ASNTCDGO/NMRO/FCHA/FCPR/ESTD`, `DTASCNTA`), pero no corrieron.
- `ASNTFCPR` (fecha de ingreso) se asume siempre llena; los asientos con `NULL` no cuentan para la regla (b).
- El candado de `/prdo/cerrar` (punto 1.1) queda para el lote siguiente.
