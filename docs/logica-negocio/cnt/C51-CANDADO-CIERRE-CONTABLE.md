# C51 — Candado en `POST /prdo/cerrar`

Equipo omen-saa-1 · 2026-11-27 · lote 30, punto 1.1 del encargo de controles de producción. Se apoya en el C50 (G atadas a la corrida). Va a `ear/siguiente` por cherry-pick.

## 1. Qué hace
Propiedad del servidor **`saa.cierres.candadoCierre`** (apagada por defecto; definida en `CandadoCierreContableService`, no en `ModoCorteCartera`). Con el candado **encendido**, `cerrar` exige, además de lo de siempre (mayorizado, cartera cerrada, orden, lote 22):
- **(a)** el semáforo del período (`ControlCierreService.controlar`, el mismo de `GET /prdo/controlCierre`) **sin ningún control en ERROR**;
- **(b)** la última ejecución de las G40–G51 del mes **COMPLETA** (`EJRCESTD = 3`) y con la **validación previa sin errores**: se vuelve a correr `ValidacionEstructurasGService.validar(codigoEjrc)` y se cuentan las observaciones de nivel ERROR (no se lee `EJRCOBSR`, que es un texto de cuando se generó);
- **(c)** esa ejecución **VIGENTE** según el C50 (`ActualizacionEjecucionGService.evaluarUltima`); `SIN_DATO` cuenta como no vigente.
Sin ejecución de G en el mes: falla `G_NO_GENERADAS`. El rechazo (`CIERRE_CANDADO: …`) nombra cada falla con su código, descripción y valor.

## 2. Excepciones
- Cuerpo **opcional** de `POST /prdo/cerrar` (los parámetros siguen en la URL, como hoy): lista JSON `[{"codigoControl": "...", "motivo": "..."}]`. Sin cuerpo, como hoy.
- Una excepción cubre un control en ERROR solo con **motivo no vacío**; sin motivo o sin código: `CIERRE_CANDADO_EXCEPCION` (400). El código no distingue mayúsculas.
- **(b) y (c) no admiten excepción** (`G_NO_GENERADAS`, `G_NO_COMPLETA`, `G_VALIDACION_ERRORES`, `G_NO_VIGENTE`): pedirla se rechaza. El control `G_DESACTUALIZADAS` del C50 se omite de la lista de controles porque lo cubre (c).
- Una excepción para un código que no está fallando se ignora (el semáforo pudo limpiarse entre la consulta y el cierre).
- **Registro, sin DDL**: el motivo de la fila `CIERRE` de `CNT.HCPR` (`HCPRMTVO`, 1000 caracteres) lleva el motivo del usuario (hasta 200) y luego `CANDADO EXCEPCIONES: <código> (<descripción>) valor V EXCEPCION: <motivo>; …`; el usuario y la IP ya son columnas de la fila. No se agregaron acciones nuevas a la bitácora porque `CK_HCPR_ACCION` solo admite `CIERRE`/`REAPERTURA`/`DESMAYORIZAR` (otra acción exigiría DDL). Si el texto pasa de 1000 caracteres, `registrarBitacora` lo trunca por el final.

## 3. Candado apagado
`cerrar` queda igual que hoy, pero la fila `CIERRE` de `CNT.HCPR` lleva `CANDADO APAGADO, habria bloqueado: <falla>; …` con lo que habría rechazado, para ver mes a mes en producción si ya puede encenderse. Esa evaluación **nunca corta** el cierre: si falla (error de lectura), la bitácora dice «CANDADO APAGADO: no se pudo evaluar (…)».

## 4. Consulta: `GET /prdo/candado/{idEmpresa}/{anio}/{mes}`
Solo lectura; no depende de que el candado esté encendido. Devuelve `{idEmpresa, anio, mes, candadoActivo, bloquearia, fallas[{origen CONTROL|G, codigo, descripcion, valor, detalle, excepcionable, cubierta, motivoExcepcion}]}`. Para la pantalla y para decidir cuándo encenderlo.

## 5. Archivos
- Nuevos: `cnt/service/CandadoCierreContableService.java`, `ReglaCandadoCierre.java` (regla pura), `dto/ExcepcionCandado.java`, `dto/FallaCandado.java`, `dto/ResultadoCandado.java`, `cnt/serviceImpl/CandadoCierreContableServiceImpl.java`.
- Modificados: `CierrePeriodoContableService` (sobrecarga con excepciones), `CierrePeriodoContableServiceImpl` (el candado y la nota de bitácora; el `cerrar` de 6 parámetros queda y delega), `PeriodoRest` (cuerpo opcional y el GET).
- Pruebas: `src/test/java/com/saa/ejb/cnt/service/ReglaCandadoCierrePruebas.java` y `tools/pruebas/correr-pruebas-c51.sh`.
- **Sin DDL.** No se tocó `ControlCierreServiceImpl` (los hunks de la C19b quedan intactos).

## 6. Cuerpo de `POST /prdo/cerrar`
`POST /prdo/cerrar?idEmpresa=&anio=&mes=&usuario=&ip=&motivo=` + cuerpo opcional `[{"codigoControl":"BANCO_xxxxxxx","motivo":"conciliacion pendiente, extracto del banco"}]`. El código del control de un banco es `BANCO_<cuenta contable>`; el de saldos negativos, `B17_SALDOS_NEGATIVOS`; el de cuotas 7 sin fecha, `CARTERA_CUOTAS7_SIN_FECHA`.

## 7. Aceptación del cuadre (copia de julio; NO corrida aquí)
1. Con el candado encendido y **sin** excepción: el cierre de julio se rechaza y nombra el control de la diferencia del banco Internacional (−2.351,20): `BANCO_<cuenta>` (compara el saldo contable con el extracto). **Con** la excepción `{BANCO_<cuenta>, motivo}`: cierra, y `CNT.HCPR` guarda la excepción con su motivo y usuario. Además tienen que estar las G de julio completas, sin errores de validación y vigentes (C50), y el resto del semáforo sin ERROR.
2. Anular RRH-2026-07-REG1 (2.5.01 deudora 850,13): ¿qué control actual lo detecta? **El de saldo contrario por cuenta (punto 2.5) todavía no existe.** Hoy lo vería, si acaso, `B17_SALDOS_NEGATIVOS` (C39: avisos «SALDO NEGATIVO» del B17 preliminar, nivel ERROR), pero solo si el renglón del B17 de esa cuenta aparece negativo en el B17; si la 2.5.01 se netea dentro de un renglón padre con saldo positivo, ese control no la ve. Tampoco lo ve `ECUACION_CONTABLE` ni los controles de puentes/cartera/bancos. **No lo pude comprobar sin base:** la aceptación 2 depende de lo que dé `B17_SALDOS_NEGATIVOS` sobre esa cuenta en la copia; si no sale, queda para el punto 2.5.
3. Consulta previa: `GET /prdo/candado/{empresa}/2026/7` debe listar las mismas fallas que el rechazo.

## 8. Propiedades para el PASE
| Propiedad | Valor al desplegar | Efecto |
|---|---|---|
| `saa.cierres.candadoCierre` | **no definirla (apagado)**; `true` cuando el semáforo de un mes real salga limpio | candado de `/prdo/cerrar` (este lote) |
| `saa.cartera.bloqueoCuotas7SinFecha` | apagada | C48: cuotas 7 sin fecha bloquean el ejecutar del cierre de cartera |
| `saa.cartera.bloqueoCierreProvision`, `saa.cartera.ajusteUltimoRecurso` | apagadas | lotes 22/24 (solo si la rama los lleva) |
Además, para que el candado pueda evaluar las G: el `sql/413` del C50 aplicado (columnas de `RPR.EJRC`); sin él, la ejecución nueva falla al guardarse.

## 9. Compatibilidad con `origin/ear/siguiente`
`CierrePeriodoContableServiceImpl`, `CierrePeriodoContableService` y `PeriodoRest` son idénticos en `main` y `ear/siguiente` (`git diff origin/main origin/ear/siguiente`); los nuevos archivos dependen del C50 (ya aplicado en `ear/siguiente`) y de `ControlCierreService`/`ValidacionEstructurasGService`, que existen en las dos ramas.

## 10. Transacción y cuerpo (corrección previa al commit)
- `CandadoCierreContableServiceImpl.consultar` es `REQUIRES_NEW`: es solo lectura y no comparte la transacción del cierre; si adentro una `RuntimeException` (controlar, validar, una consulta) marca rollback, solo cae esa transacción y el cierre sigue (con el candado apagado la bitácora dice «no se pudo evaluar»). Se eligió sobre `NOT_SUPPORTED` porque `controlar()` usa otros EJB y consultas JPA que conviene que corran dentro de una transacción. `controlar` y `validar` se revisaron: no escriben (el B17 preliminar usa `calculaRenglones`, que no graba; `reporteRenglones`, que sí graba en `TEMP`, no se llama). No hay tope de tiempo: solo el aislamiento.
- El cuerpo de `POST /prdo/cerrar` se recibe como **texto** (sin `@Consumes`, sin depender del Content-Type) y se parsea como lista JSON solo si trae algo: el FE hace `post(url, null)` sin cuerpo ni cabecera; vacío = sin excepciones. Un cuerpo que no es una lista válida: 400 `CIERRE_CANDADO_EXCEPCION`.

## 11. No verificado
Nada corrió contra base ni WildFly. Pendiente en la copia: la aceptación 7.1 y 7.2, el costo de `controlar()` dentro de `cerrar` (arma el B17 preliminar) y la lectura de `RPR.EJRC` (consulta nativa sobre columnas que ya usa el repo).
