# ESTADO — equipo `omen-saa-1` (CRD · EQUIPO B: ciclo del crédito y seguros)

**Árbitro:** `omen-saa-1-arb` (máquina **omen**) · **Agentes:** `omen-saa-1-be`, `omen-saa-1-fe`
**Creado:** 2026-09-01 · **Marcador de commit:** `eqB` · **Este documento lo mantiene SOLO este equipo.**

> ## Por qué nace este archivo
>
> Hasta hoy el equipo B no tenía documento de estado propio: usaba `ESTADO-CRD.md`, que **lo
> mantiene el árbitro del equipo A (`saabe-25`)**, más `crd/PLAN-CICLO-OTORGAMIENTO.md`, que es un
> plan de un frente y no un tablero. Dos equipos escribiendo el mismo tablero es exactamente lo que
> hundió a `ESTADO-GENERAL-TRABAJO-EN-CURSO.md`, dado de baja el 2026-08-28 por ese motivo.
>
> | Equipo | Documento de estado |
> |---|---|
> | CRD · EQUIPO A (`saabe-25`, otra máquina) | `ESTADO-CRD.md` |
> | **CRD · EQUIPO B (`omen-saa-1`, OMEN) — este equipo** | **este archivo** |
> | `omen-saa-2` (OMEN, clon `F:\work\equipo2`) | `ESTADO-EQUIPO-OMEN-2.md` |
> | `omen-saa-3` (OMEN) | `ESTADO-CXP-CXC-TSR-RHH-SRI.md` |
>
> `ESTADO-CRD.md` se sigue **leyendo** (es donde el equipo A anota lo que despliega y lo que corre
> en producción), pero este equipo **no lo escribe más**.

---

## 0. Dónde trabaja este equipo

| Repo | Ruta |
|---|---|
| `saaBE` | `F:\work\saaBE\v1\saaBE` — checkout compartido de `v1` |
| `saaFE` | `F:\work\saaFE\v1\saaFE` — solo lectura, salvo `docs/crd/` |

**Reservas vigentes** (`REGISTRO-RESERVAS-EQUIPOS.md`): `PRBR` 270–289 · `PDTR` 1300–1399 ·
scripts `crd/sql/` **150–199** · plantilla contable alterno **34**.
**Ninguno consumido todavía** salvo el número de script 150 y 151, y el alterno 34 (reservado, sin escribir).

**Archivos con dueño exclusivo de este equipo** (§4 del registro):
`CalculadoraAmortizacionServiceImpl`, `SimulacionPrestamoServiceImpl`, `PrestamoServiceImpl`;
FE `forms/simulador-*`, `forms/asignacion-seguros/*`, `forms/prestamo/*`.
Compartido: `service/prestamo.service.ts` — modificar solo el método propio.

**Entorno verificado el 2026-09-01 en OMEN:** Maven **3.9.8**, JDK **21.0.8**.
`mvn -q clean compile` sobre `80566a4` → **exit 0**. Toda entrega de Java se compila antes de cerrarse.

---

## 1. Los tres frentes, en serie

`ALCANCE-EQUIPOS-CRD.md` ordena: **1) otorgamiento · 2) reestructuración · 3) seguros.**
No se arranca el siguiente hasta cerrar el anterior.

| # | Frente | Estado al 2026-09-01 |
|---|---|---|
| 1 | **Otorgamiento** | **Código completo BE+FE, commiteado, NADA desplegado.** Bloqueado por un gate de producción que no se corrió: `sql/151` |
| 2 | **Reestructuración** | **Sin empezar.** Verificado hoy: solo existen los DTOs de simulación, ningún método que la aplique |
| 3 | **Seguros por pólizas** | **Sin empezar.** Verificado hoy: no existe ninguna entidad, DAO, service ni endpoint de póliza/aseguradora |

---

## 2. Frente 1 — otorgamiento · ENTREGADO, SIN DESPLEGAR

Verificado contra el código el 2026-09-01, no contra el plan.

| Pieza | Verificado en |
|---|---|
| Préstamo nuevo nace en `PENDIENTE_DE_APROBACION (6)` | `PrestamoServiceImpl:107` |
| Generar tabla lleva 6 ó 1 → `GENERADO (1)` | `PrestamoServiceImpl:227-233` |
| `aprobar` / `rechazar` con validación de estado de origen | `PrestamoServiceImpl:265`, `:300` |
| `POST /rest/prst/aprobar/{id}` y `/rechazar/{id}`, cuerpo `SolicitudDecisionPrestamo {usuario, observacion}` | `PrestamoRest:362`, `:393` |
| FE: `aprobar()`/`rechazar()` en el service, botones con confirmación y habilitación por estado | `crd/service/prestamo.service.ts:97,104`; `forms/prestamo/prestamo-edit` |

### 2.1 ⛔ El gate: `sql/151` sigue sin correr

`PLAN-CICLO-OTORGAMIENTO.md` §5.b. El ciclo nuevo le da a `PRSTIDST = 1` el significado
*"tiene tabla, falta aprobar"*, pero `prestamo-edit` venía mandando `idEstado: 1` al dar de alta la
**cartera migrada**. Si hay préstamos vivos sentados en el estado 1, el ciclo les ofrece aprobar
(empiezan a devengar mora) o rechazar (dejan de devengar), **las dos en silencio**.

`sql/151` es **solo `SELECT`**, se puede correr en horario laboral. **Hasta que no se lea su
bloque 1, este frente no se despliega.**

### 2.2 La auditoría del motor quedó cerrada — y verificada

`REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md` dejó cinco defectos para corregir antes de generar la
primera tabla real. **Los cinco están aplicados en el código** (verificado hoy, línea por línea):

| Defecto | Verificado en |
|---|---|
| **N1** idempotencia de `generarTablaAmortizacion` | guarda con `countByIdDetallePrestamo` (método del equipo A: cuenta anulados y propaga la excepción) |
| **D5** `DTPRSLDO` = total por cobrar, no capital | `PrestamoServiceImpl:426` — `setSaldo(getTotal())` |
| **N2** `PRSTVLCT` contaminado por el interés proporcional | `:462` — `calcularValorCuotaRepresentativa(...)` |
| **N3** seguros en el generador real (decisión U1) | `:371-374` — desgravamen por fórmula sobre saldo, incendio en `0.0` a propósito |
| **N5** estado de la cuota del catálogo correcto | `:430-431` — `EstadoCuotaPrestamo.PENDIENTE`, no `Estado.ACTIVO` |

**El camino de carga por Excel quedó intacto a propósito** (`:546-703` conserva la semántica vieja):
es por donde entró toda la cartera migrada y tocarlo sería retroactivo sobre producción.

---

## 2b. Frente lateral — Informe de necesidad de pago (devolución individual)

**Pedido por el usuario el 2026-09-01**, fuera del orden de los tres frentes. Reporte Jasper
`RPRT_INFR_DVAP`: el informe que hoy se hace a mano en Word, para **un solo partícipe**, impreso
desde la pantalla de devolución de aportes.

**El usuario cerró el resto de equipos ese día**, así que la devolución de aportes —que era del
equipo A— pasó a este equipo. Las reservas por archivo del `REGISTRO-RESERVAS-EQUIPOS.md` §4 ya no
tienen a quién proteger.

| Pieza | Estado |
|---|---|
| Especificación + contrato, espejado a `saaFE/docs/crd/` | ✅ `c895305`, `3e457ff` |
| `.jrxml` + `.jasper` compilado | ✅ `01779ee` |
| FE: diálogo, botón al registrar y botón en el histórico | ✅ `32587a3` (saaFE) |
| Correcciones de la sección 2 (signo + leyenda) | despachadas al BE |
| Prueba contra el servidor | ⛔ **pendiente del usuario** |

### Hallazgos

**H5 — El neto del informe no existe en el sistema.** Cruzar contra préstamos
(`prst/pagarConAportes`) y devolver aportes (`dvap/registrar`) son operaciones **separadas y sin
ninguna FK entre ellas**; la devolución ni siquiera valida la deuda (`deudaVigente` es un aviso, por
decisión del 2026-08-24). `DVAPVLRR` es lo que el operador eligió devolver, no el resultado de
restar préstamos. Se reconstruye con `APRT.APRTTPMV = 4` (`PAGO_PRESTAMO`), que marca los aportes
consumidos en cruces. **El informe pone los tres bloques uno al lado del otro y no afirma una resta
que el sistema no hizo.**

**H6 — El `.jasper` se puede compilar en esta máquina, sin Jaspersoft Studio.** Classpath de Maven
más el JDK 21, fuera de WildFly. Verificado compilando `RPRT_CRTF_APRT.jrxml`: 28.736 bytes contra
28.642 del commiteado, misma cabecera serializada, misma versión 7.0.3. Procedimiento en el §3 de la
especificación. **Levanta el mayor riesgo de cualquier entrega de reportes de este repositorio.**

**H7 — `REGLAS_GENERACION_REPORTES_G.md:306-307` es falso y peligroso.** Afirma que basta el
`.jrxml` porque hay compilación runtime con Janino. No hay Janino en 7.0.3 y sí hay Maven acá. Un
agente que lo lea entrega un reporte que revienta al primer uso — es lo que pasó con los siete de
`rhh`. **Sin corregir; no es de esta entrega.**

**H8 — Los movimientos de cruce se graban NEGATIVOS.** `consumirAportes:735` hace
`setValor(-valor)`, mientras `DDVAVLRR` es positivo. El reporte los imprimía con su signo crudo, o
sea negativos junto a positivos. Corregido con `ABS()`. **No era visible sin leer `consumirAportes`:
las dos tablas parecen simétricas y no lo son.**

**H9 — Un cruce reversado se sigue listando.** El contra-movimiento se graba aparte con
`APRTTPMV = 5` y **sin FK al aporte original** (`ProcesoPagoPrestamoServiceImpl:1444-1456`), y el
tipo 5 lo escriben también las devoluciones y las pensiones. Filtrarlo exigiría adivinar por glosa.
**Se resolvió con una leyenda que lo declara**, no con una heurística frágil. Limitación conocida.

### Fallo de proceso propio, registrado

La primera versión de la especificación decía «los cinco bloques del Word, literales» **sin
transcribirlos**, y el Word no está ni puede estar en el repositorio (lista 19 partícipes con
cédula, nombre y monto). El agente de BE quedó bloqueado con razón y **paró en vez de inventar una
cita legal**. Es la regla 7 —lo que un agente va a implementar tiene que estar en disco antes— y
«transcribir del Word» no la cumple cuando el Word no está. Corregido con el Anexo A.

---

## 2e. 2026-09-02 — Producción: por qué no aparecían los préstamos en mora. RESUELTO

**Tres días de diagnóstico, dos causas equivocadas —las dos mías— y la real encontrada con
datos.** Vale registrar el recorrido entero porque el error de método se repitió.

### La causa real (H16): una búsqueda sin resultados lanza excepción

`sql/159` sobre el préstamo del caso: **49 cuotas y CERO pagos**. Y:

```java
// PagoPrestamoServiceImpl:90-92
if (result.isEmpty()) {
    throw new IncomeException("Busqueda por criterio PagoPrestamo no devolvio ningun registro");
}
```

`PagoPrestamoRest:117` lo devuelve como **400**, el frontend lo recibe como fallo, marca
`cargaFallida` y **el préstamo desaparece de la lista**.

> **El préstamo desaparecía porque NO TENÍA PAGOS.** No por tener demasiados —esa fue mi
> hipótesis, y el promedio de la cartera es 38 pagos con máximo 168; éste tenía 0—.
>
> **Y explica el síntoma original completo:** un préstamo en mora que nunca recibió un pago **no
> podía aparecer nunca**. Los que tienen pagos cargaban bien. Por eso parecía que «solo salen los
> vigentes»: los vigentes normalmente ya pagaron algo.

**Corregido en `9343c43` (saaFE), sin tocar backend:** si falla la consulta de pagos el préstamo
**se muestra igual** con `pagosPorCuota = {}`. Si el error dice «no devolvio ningun registro» va
**sin aviso** —es el caso normal y el total de cada cuota ES el saldo real—; cualquier otro error
lleva aviso, porque ahí el saldo podría estar inflado.

⚠️ **Distinguir por el texto del mensaje es frágil y es un puente, no el destino.** El arreglo
limpio es que `selectByCriteria` devuelva lista vacía. Ver H17.

### H17 — ⛔ `if (result.isEmpty()) throw` está en 255 archivos

```
grep -rl "if (result.isEmpty())" saaBE/src/main/java/com/saa/ejb  →  255 archivos
```

`DetallePrestamoServiceImpl:95-97` tiene el mismo patrón, idéntico. **Es la convención de la casa**
(`CLAUDE.md` la documenta) y es la cuarta deuda transversal — la más costosa de las cuatro:

> Las otras tres hacen que **un fallo parezca dato**. Ésta hace que **un dato normal parezca
> fallo**. «No hay filas» es una respuesta válida en la enorme mayoría de las consultas, y acá
> viaja como error hasta el cliente.

**No se toca desde un módulo.** Va al registro §6.

### H18 — `CRD.AVPC`: el `CHECK` permitía lo que la columna prohibía

Al mandar el excedente **solo a un aporte**: `ORA-01400` sobre `AVPC.PRSTCDGO`.

`AVPC` nació cuando toda afectación iba contra una cuota (`PRSTCDGO`/`DTPRCDGO` `NOT NULL`). El
script **87** agregó `TPAPCDGO` para afectar a un aporte, **diseñó el caso bien** —su comentario
dice *«De aporte: TPAPCDGO presente, PRSTCDGO/DTPRCDGO NULL»*— y creó
`CK_AVPC_PRST_XOR_TPAP` para formalizarlo. **Pero nunca quitó las dos `NOT NULL`.**

**Las dos reglas se contradicen y gana la de la columna: la rama de aporte del `CHECK` era
imposible de satisfacer desde el día uno.** No se detectó porque hasta hoy nadie mandó un excedente
solo a un aporte. Corregido con `sql/160` (dos `MODIFY`, sin tocar filas, sin desplegar nada).

### La lección de método, que es la que importa

**Dos diagnósticos equivocados seguidos, y el mismo error las dos veces:** encontré un mecanismo
que *podía* producir el síntoma, lo verifiqué contra el código —donde era correcto— y **lo di por
causa antes de medirlo contra los datos**.

| Hipótesis | Verificada en código | Medida contra datos | Resultado |
|---|---|---|---|
| `PRSTSLTT` muerto escondía los préstamos | ✅ correcta | ❌ no, antes de despachar | Escondía **1 de 338** |
| El préstamo tenía demasiados pagos | ✅ plausible | ❌ no | Tenía **0** |
| Una búsqueda vacía lanza excepción | ✅ | ✅ `sql/159` | **Era ésta** |

**Un mecanismo plausible y verificado en el código no es una causa hasta que los datos muestran que
ocurre con la frecuencia del síntoma.** Las dos veces el script que lo habría desmentido lo escribí
yo mismo, y las dos veces despaché sin esperar su resultado.

Lo que sí funcionó, y hay que repetirlo: **cuando dos causas se ven idénticas en pantalla, hacer
primero que la pantalla las distinga.** El aviso de `10142d5` es lo que convirtió «no aparece» en
«falló la consulta de pagos del préstamo N», y de ahí salió la causa en una medición.

---

## 2d. Frente lateral — desembolso del préstamo (CRD alimenta a TSR)

**Pedido del usuario el 2026-09-01**, cierra el §6.1 de `PLAN-CICLO-OTORGAMIENTO.md`.
Diseño en `crd/PLAN-DESEMBOLSO-PRESTAMO.md`. Código en `7bca171` y `b9750d4`.

**Decisión del usuario:** el desembolso lo ejecuta **TSR**; CRD lo **alimenta**. La contabilidad se
escribe al alimentar TSR, **excepto el asiento contra bancos**, que se genera al confirmarse el pago.

### ⭐ El diseño ya estaba implícito en las plantillas, y eso lo valida

Las plantillas 9 y 13 **no tienen cuenta de bancos**. Su contrapartida es `2.3.90.90.10 SOCIOS POR
PAGAR`, que es la cuenta puente:

```
al aprobar → DEBE cartera por tramo   HABER socios por pagar    (CRD)
al pagar   → DEBE socios por pagar    HABER bancos              (CXP, solo)
```

**La separación que pidió el usuario es la que las plantillas ya suponían.** Y casi todo el
mecanismo existe: `registrarPagoDeOrigenExterno` ya tiene dos consumidores de `crd` en producción.
**Este frente no modifica un solo archivo de `cxp` ni de `tsr`.**

### ⛔ Estado: BLOQUEA EL DESPLIEGUE DEL OTORGAMIENTO

`aprobar` **falla ruidoso** hasta que se resuelva `sql/157`: el desglose contable queda tras
`ID_PRODUCTO_PAGO_SOCIOS_POR_PAGAR = null`. Es deliberado —con el producto equivocado tesorería
paga y el asiento descarga otra cuenta, cuadrado igual y sin error— pero **el otorgamiento pasó de
"listo para desplegar" a "esperando el 157"**, y así se le comunicó al usuario.

### H14 — `PRSTVLAS` (valor asegurado) tampoco tiene escritor

El asiento de entrega alimenta la **línea del bien en garantía** (aux 8 de las plantillas 9 y 13)
con `Prestamo.valorAsegurado`. Verificado: el **único lector en todo el backend** es el código del
desembolso recién escrito. Es una de las cuatro columnas de seguro que `ESTADO-EQUIPO-SEGUROS.md`
§1.3 ya había marcado como mapeadas y sin escritor.

**Un prendario o un hipotecario quedaría sin registrar su garantía en cuentas de orden, y el asiento
cuadra igual** — el mismo modo de falla silenciosa que este equipo pasó el día entero persiguiendo.
No bloquea el quirografario. Documentado en `ContabilidadPrestamoServiceImpl:566`.

### H15 — No existe ninguna forma establecida de saber la familia de un producto

El asiento elige plantilla por familia (prendario 9 / hipotecario 13 / quirografario 34), y **no hay
ningún precedente en el código que clasifique productos por familia**. La implementación compara
`TPPRNMBR` contra los tres literales, y la familia quirografaria incluye EMERGENTE, CENAPRO,
EXPRESS, SUST. BIESS y las variantes RESTR./NOVACION — que casi seguro no se llaman así.

**Rechaza en vez de clasificar mal**, que es lo correcto, pero significa que esos productos no se
podrían otorgar. `sql/157` bloque 5 dice si sirve `TPPRNMBR`, si sirve `TPPRTPOO` (un `VARCHAR2(50)`
llamado «tipo» que podría ser el agrupador), o si hace falta una tabla de mapeo.

> **Los dos supuestos los planteó el agente de BE y frenó en vez de resolverlos solo.** El otro —el
> orden de los auxiliares de las plantillas 9 y 13— lo dedujo del levantamiento contable y **acertó
> exacto**, confirmado después contra el output real del `153`. Marcar un acierto como supuesto es
> lo que permite verificarlo; darlo por hecho es lo que deja un asiento mal clasificado y cuadrado.

---

## 2c. Corrección urgente — préstamos en mora en las novedades bloqueantes de Petro

**Pedido del usuario el 2026-09-01.** En `archivo-petro/carga/detalle`, pestaña descuentos, al
afectar una novedad BLOQUEANTE solo aparecían los préstamos vigentes y no los que están en mora.

**Entregado:** `b3873b3` (saaFE), un solo archivo. Diagnóstico en `crd/sql/154`. Trampa documentada
en `petro/REGLAS-GENERALES-PETRO.md` §9.10.

### ⛔ EL REPORTE ORIGINAL SIGUE ABIERTO — el diagnóstico del árbitro era incorrecto

**Medido con `sql/154` en producción el 2026-09-01, y los datos desmienten la explicación:**

| Estado | Préstamos | Los veía el filtro viejo | No los veía |
|---|---|---|---|
| 2 VIGENTE | 866 | 866 | 0 |
| 8 DE PLAZO VENCIDO | 106 | 106 | 0 |
| 11 EN MORA | 338 | **337** | **1** |
| 3 + 4 CANCELADOS | 4.354 | **4.012** | 342 |

**337 de 338 préstamos en mora ya aparecían** con el filtro viejo. El defecto de `PRSTSLTT`
escondía **un solo** préstamo en mora (el 4926, partícipe 4308), así que **no explica un síntoma
descrito como "solo saca los vigentes"**. La causa real está sin encontrar.

**Pedido al usuario:** un caso concreto (partícipe y carga) donde lo haya visto. Sin eso se vuelve a
cambiar a ciegas, que es lo que ya pasó una vez acá.

### H10 — `PRSTSLTT` y `PRSTSLCP` son campos muertos, y uno de ellos filtraba una pantalla

Esto **sí** es real y se corrigió, pero es un defecto **distinto** del que se estaba buscando.

`Prestamo.setSaldoTotal()` existe en el backend con **cero llamadas**: `PRSTSLTT` es el valor que
dejó la migración y nunca se movió. La pestaña de descuentos filtraba por `saldoTotal > 0`
(`detalle-consulta-carga.component.ts:2408`), y el resultado medido es **el inverso del que se
supuso**: casi no escondía préstamos vivos, y en cambio **ofrecía 4.012 préstamos YA CANCELADOS**
—con casi 30 millones de saldo congelado— como candidatos válidos para aplicarles un descuento del
archivo Petro. Nadie lo había reportado.

Corregido filtrando por `idEstado` (2 y 11). **El cambio se sostiene por este motivo, no por el
reporte original.**

> **Lección, y es sobre mí, no sobre los agentes:** encontré un mecanismo que *podía* producir el
> síntoma, lo verifiqué contra el código —donde era correcto— y **lo di por causa antes de medirlo
> contra los datos**. El script que lo habría desmentido lo escribí yo mismo, y despaché la
> corrección sin esperar su resultado. **Un mecanismo plausible y verificado en el código no es una
> causa hasta que los datos muestran que ocurre con la frecuencia del síntoma.**

**Precedente que confirmó el diagnóstico:** `cobros-personales.component.ts:292` ya documentaba que
`saldoTotal`/`saldoCapital` de `PRST` no son fiables, y esa pantalla las había abandonado a favor de
calcular desde cuotas y pagos. La de Petro nunca se actualizó.

> **Lección de método, y es la que vale:** el agente de BE reportó **«el filtro no existe»** en vez
> de cambiar el candidato más parecido. Si hubiera "arreglado" el más plausible, hoy habría un
> cambio en la carga Petro que no arregla nada y que hay que revertir. **Pedir explícitamente que un
> agente pueda contestar "no está donde decís" es lo que hizo que esto se resolviera bien.**

### H12 — El caso 401: el préstamo pasa los tres filtros y aun así no aparece

**Medido con `sql/155`.** Partícipe `ENTDCDGO 4113` (código Petro 401), tres préstamos:

| Préstamo | Estado | `PRSTSLTT` | Cuotas pendientes | |
|---|---|---|---|---|
| 4411 | 3 Cancelado | 3.900,85 | 0 | correcto que no salga |
| 4412 | 3 Cancelado | 5.800,00 | 0 | correcto que no salga |
| **7991** | **11 EN MORA** | **5.028,69** | **47** | **debería salir, y no sale** |

`PASAN_FILTRO_NUEVO = 1` y **`PASAN_FILTRO_VIEJO = 1`**: el 7991 pasaba también con el filtro de
`saldoTotal`. **Ningún filtro de los revisados explica el síntoma.** La causa está antes, en
`cargarContextoAfectacionFinanciera`, y es algo que falla sin avisar.

### H13 — ⛔ `handleError` convierte un fallo en «sin datos», en 316 servicios del frontend

Detectado por el agente de FE al buscar dónde se pierde el préstamo 7991. Todos los servicios del
frontend comparten, byte por byte:

```ts
private handleError(error: HttpErrorResponse): Observable<null> {
  if (+error.status === 200) { return of(null); }
  else { return throwError(() => error.error); }
}
```

**`grep -rln "if (+error.status === 200)" src/app` → 316 archivos.**

Un `HttpErrorResponse` con status 200 es un **fallo de parseo** del cuerpo. Ese caso no llega como
error de RxJS: llega como una emisión **exitosa** con valor `null`, y en el consumidor
`Array.isArray(x) ? x : x ? [x] : []` lo colapsa a `[]`. **Una consulta que falló y una que no
devolvió filas terminan siendo el mismo valor.** No pasa por ningún `catchError`, no deja rastro en
consola, y la pantalla dice «no hay préstamos».

**No se toca.** Son 316 servicios y es exactamente el caso que `CLAUDE.md` pide no arreglar en un
servicio compartido sin ver a quién más le pasa por debajo. **Se distingue en el punto de consumo**,
y se registra acá como deuda transversal sin dueño.

> **Lo que este día enseñó, y vale más que los tres hallazgos:** en esa pantalla *"la consulta
> falló"* y *"no hay préstamos"* se ven **idénticos**. Sobre esa ambigüedad se construyeron tres
> diagnósticos, **dos equivocados —uno de ellos mío—**, y cada uno consumió una medición contra
> producción para descartarse. **Antes de seguir buscando la causa, la pantalla tiene que poder
> decir cuál de las dos cosas le pasó.** Eso es lo que se implementó: no es un parche mientras se
> busca, es el instrumento que faltaba para buscar.

### H11 — Hay DOS implementaciones de la "fase 2" de la carga Petro

Detectado por el agente de BE. `CargaArchivoPetroServiceImpl` (`asoprep`,
`POST /asgn/aplicarPagosArchivoPetro`) y `ProcesoCargaPetroServiceImpl` (`crd`,
`POST /crar/procesarCargaPetro`) conviven, **y las dos escriben «FASE 2» en el log**. La pantalla
usa la primera (verificado en `detalle-consulta-carga.component.ts:993`); la segunda tiene TODOs sin
resolver y escribe en un campo único en vez de en `NovedadParticipeCarga`. Parece un resto sin
desconectar. **Sin resolver, no urgente** — pero es exactamente cómo se diagnostica mal un problema
de producción leyendo la consola.

---

## 2f. 2026-09-02 — La jornada de la carga Petro 449

**El día más largo del equipo.** El usuario no podía procesar el archivo del mes; se destrabó en
etapas y aparecieron defectos que estaban ahí desde antes. Se registra completo porque **la mitad
del valor está en el recorrido**, no solo en el resultado.

### Lo entregado, en orden

| Commit | Qué |
|---|---|
| `d7aef68` | El tipo 4 deja de bloquear en Petrocomercial · normalización de tildes/eñes · toda novedad que bloquee genera su fila `NVPC` |
| `f94532b` → **`9e55edf`** | La fecha del asiento. **El segundo corrige al primero** — ver H21 |
| `8a142db` · `53ed29f` | Las dos optimizaciones de rendimiento |
| `sql/163` · `sql/165` | Parches de datos: novedades faltantes para poder repartir |

### H19 — ⛔ El proceso DESCARTA dinero en silencio. Lo más grave de todo el día

`procesarExcedenteASiguienteCuota:2802-2819`. Cuando detecta que la cuota más antigua **todavía
tiene saldo**, no la paga y no falla: **loguea una advertencia y hace `return`, perdiendo el
excedente**. No queda en ningún `PagoPrestamo`, no queda en ninguna cuota, no genera novedad. Solo
una línea entre miles en el log.

> El comentario del propio código dice *«Tiene saldo pero es cuota anterior/actual - no debería
> pasar»*. **Sí pasa**, y ahí está exactamente el punto donde vive la regla que pidió el usuario:
> *nunca dejar una cuota parcial y pasar a la siguiente*. El código **ya detecta la condición** y
> en vez de resolverla tira el dinero. Está a un `if` de hacer lo correcto.

**Sin corregir.** Es el pendiente más importante que deja este día.

### H20 — El proceso saltea cuotas, medido

`sql/167` bloque 3. Préstamo **4456**: pagó las cuotas **64 a 73** con la **63** sin pagar — diez
salteadas. Préstamo 3267: pagó 103-105 con la 102 sin pagar. **279 cuotas quedaron PARCIAL** contra
813 pagadas.

Y **no es por la mora**: la mora pendiente de toda la carga son **$1.284,42 en 200 cuotas** —
centavos por cuota—, mientras a las parciales les falta muchísimo más (una cuota de $947 con $80
aplicados). La mora cobrada fue **$0,00**, que es el diseño documentado (`totalBaseCuota:888` la
resta a propósito).

### H21 — La fecha de efecto es el ÚLTIMO DÍA DEL MES DE CARGA

**Decisión del usuario, y corrige una instrucción mía.** El dinero se descuenta del sueldo, así que
está pagado a tiempo aunque el archivo se procese al mes siguiente.

**El daño de no tenerlo:** el proceso fechó los pagos el 2 de septiembre y la clasificación de
bandas mandó **casi toda la cartera a vencidos**. Un préstamo al día contabilizado como vencido.

Yo había hecho poner la **fecha de autorización de contabilidad** (`f94532b`) — también incorrecta.
`9e55edf` lo corrige: los **tres** asientos, el `PagoPrestamo` y `fechaPagado` al último día del mes,
y **la fecha real de recepción se conserva en la observación** del asiento y en cada línea de
transferencia.

### H22 — El seguro de incendio se pierde en las cascadas

`procesarPagoCuota`: `seguroIncendioPagar` se calcula bien y **nunca se usa** — a `crearRegistroPago`
se le pasa el parámetro, que en las cuotas pagadas **por cascada** llega en `0.0`. Resultado: la fila
queda con el **total correcto** y el **seguro en cero**, y como el asiento suma componentes mientras
el reparto usa el total, **el asiento cierra más bajo**. Explica en forma y en signo el descuadre de
**$2.563,42** entre reparto y aplicación.

⚠️ La línea tiene encima un comentario `✅ CORRECCIÓN: Usar el valor real del seguro (HS)`:
**alguien la puso así a propósito**. Por eso el arreglo exige verificar antes que
`capital + interés + desgravamen + seguro == total` en las dos llamadas, no solo en la de cascada.

### H23 — Auditoría de rendimiento: 22 minutos por carga

| # | Qué | Costo | Estado |
|---|---|---|---|
| 1 | `esperadoPorEntidad` en doble bucle | hasta **~120 consultas por partícipe** con aportes | ✅ `53ed29f` |
| 3 | El producto se consulta una vez por partícipe, siempre igual | miles por carga | ✅ `8a142db` |
| 2 | Entidad/producto/préstamo resueltos **dos veces** (validar y aplicar) | la mayoría de los 2.500 | ⬜ riesgo medio-alto |
| 4 | Trae la tabla de amortización completa para buscar una cuota | menor | ⬜ riesgo medio |

> **El hallazgo del #1 no fue la optimización, fue lo que apareció al verificarla.**
> `esperadoEnLotePorFilial` filtra además por **estado de la entidad** (`ACTIVO`, `ACTIVO_EN_MORA`)
> y el per-entidad no. Un partícipe en otro estado habría salido con «esperado 0» y **su plata se
> habría anticipado a meses futuros** en vez de cubrir lo que debía. Se resolvió con fallback.
> **Y el caso no es imposible:** la generación mira el estado al generar el archivo, la carga se
> procesa un ciclo mensual después — una jubilación en el medio abre la ventana.

### H24 — ⛔ La brecha de $2.906,52 NO está en las cuotas: está al grabar el pago

**Medido, no deducido** (`sql/170`, 2026-09-02). Sobre las 1.092 cuotas que tocó la carga 449:

| | |
|---|---|
| `DTPRTTLL` − mora − interés vencido | 275.464,51 |
| capital + interés + desgravamen + seguro | 275.464,50 |
| **brecha** | **0,01** |

**Las cuotas cuadran perfecto.** La hipótesis que sostuve durante media jornada — que
`calcularSaldosRealesCuota` calcula el total de dos formas distintas según la cuota tenga o no
pagos previos, y que en la cartera migrada esas dos formas no coinciden — **queda descartada con
datos**. Las dos ramas existen y siguen siendo feas, pero no producen este descuadre.

La brecha nace **al escribir `CRD.PGPR`**: `PGPRVLRR` queda más alto que la suma de los
componentes que se graban al lado. 237.746,62 registrados contra 234.840,10 desglosados.

**Los dos puntos que pueden generarla, los dos en `aplicarAfectacionManualConRegistroPago`** —
la ruta que se usó para desbloquear la 449, y por eso aparece justo en esta carga:

- **(A) El seguro de incendio no existe en la afectación manual.** `:3081`
  `double seguroIncendioAfectar = 0.0; // Por ahora no se maneja seguro`. Si el operador digitó
  desglose, ese `0.0` sobrevive hasta el grabado — pero el total que se graba es `valorAfectar`
  **completo**. El propio código lo dice en voz alta en `:3197`: *«Cuota tiene seguro de incendio
  pero NO se puede afectar manualmente (campo no existe en tabla AVPC)»*. Encaja con que el seguro
  grabado (893,49) sea menos de la mitad del de las cuotas (2.008,65).
- **(B) El sobrante de la distribución automática se descarta en silencio.** `:3152` imprime
  `⚠️ Excedente no aplicado: $X` y después graba `valorTotalAfectar` **completo**. Lo que no
  encontró destino no se resta del total: se vuelve brecha. Es el mismo patrón que H19, en otro
  método.

**Nota sobre el commit `a09732f`** (el arreglo del seguro hecho por deducción algebraica): salió
**peor**. El seguro grabado bajó de 1.124,28 a 893,49 y la brecha **subió 230,79**, exactamente lo
mismo. El razonamiento sólo valía para la rama con pagos previos. Pendiente de revertir.

**`sql/171` mide cuánto aporta cada ruta** antes de tocar una línea más. Si el grueso cae en
`PAGO_NORMAL`, hay un tercer defecto sin identificar y no se corrige nada todavía.

### ⛔ Lo que este día enseña sobre mi propio método

**Cuatro diagnósticos míos equivocados en el mismo problema**, y siempre el mismo error:

| Hipótesis | Verificada en código | Medida contra datos | Resultado real |
|---|---|---|---|
| El filtro `PRSTSLTT` escondía los préstamos en mora | ✅ | ❌ | escondía **1 de 338** |
| El préstamo tenía demasiados pagos | ✅ plausible | ❌ | tenía **0** |
| La pantalla debía repartir el excedente, no el recibido | ✅ | ❌ | al revés: la aplicación manual **reemplaza** al flujo automático |
| Faltaba el asiento de transitoria por el flag apagado | ✅ | ❌ | **el asiento existía**, con otra fecha |

**El patrón:** encontrar un mecanismo que *puede* producir el síntoma, confirmarlo en el código
—donde efectivamente es correcto— y **darlo por causa antes de medirlo**. Tres de las cuatro veces
el script que lo desmentía **lo había escrito yo mismo** y despaché sin esperar su resultado.

**Lo que sí funcionó, y hay que repetirlo:** cuando dos causas se ven idénticas en pantalla, **hacer
primero que la pantalla las distinga**. El aviso de `10142d5` convirtió «no aparece» en «falló la
consulta de pagos del préstamo N», y de ahí la causa salió en una sola medición.

Y las correcciones más valiosas del día no vinieron de mí: **el usuario** corrigió dos —que hay que
procesar según lo ingresado en pantalla, y que la fecha es el fin de mes de carga— y **el agente de
BE** frenó dos veces ante diferencias que yo le había dado por equivalentes.

---

## 2g. Frente URGENTE — pago mensual a jubilados (2026-09-04)

**Pedido del usuario el 2026-09-04:** *«necesitamos sacar pagos de agosto»*. Es el frente que ya
venía marcado como el más urgente desde el 2026-09-02.

### El diagnóstico: el proceso existía y no tenía quién lo llamara

```
grep -rln 'pgpc' saaFE/src/app  ->  CERO archivos
```

El backend tiene el proceso mensual **completo** desde `554b5f5` (genera, cruza contra préstamos,
contabiliza el devengo y manda órdenes a tesorería) y **ningún servicio de Angular le habla**. La
pantalla `proceso-pago-jubilados` (439 líneas) es solo parametrización de `VPPC`: administra cuánto
cobra cada jubilado y no dispara nada. El proceso solo se podía correr por API cruda.

**Esto solo se ve mirando los dos repositorios a la vez**, que es exactamente para lo que el árbitro
tiene los dos.

### H25 — El plan decía que faltaba lo que ya estaba hecho

`PLAN-PAGO-JUBILADOS.md` seguía con el encabezado *«pendiente de implementar»* y sus §3 y §4 decían
*«falta el cruce»* y *«falta el asiento»*. `554b5f5`, del mismo día, los implementó y **el documento
no se actualizó**. Verificado contra el código: `cruzarContraPrestamos:478` y
`generarAsientoDevengoPension:549` existen. Regla 1 del árbitro, otra vez: el plan describía un
estado que ya no existía.

### H26 — El contrato de API afirmaba tres cosas que el código no hace

Encontradas verificando línea por línea **antes** de dejar arrancar al frontend (regla 6). Corregido
en `b964780`, espejado en `saaFE` en `cbf89da`.

| Lo que decía | Lo que pasa |
|---|---|
| §3 `porEntidad` trae los campos de cruce y orden de pago | **Falso.** Devuelve la entidad JPA cruda; esos tres campos existen solo en `DetallePagoPension`, el DTO de la corrida. El frontend habría armado tres columnas que nunca se llenan |
| Las fechas viajan como `yyyy-MM-dd` | Vale para lo que se **envía**. Lo que **llega** son arreglos de Jackson. Mismo defecto ya corregido en `API-AUDITORIA-BANDAS.md` el 09-03 — se coló en el contrato de al lado |
| (no lo decía) | `estado` llega como número 1..5 sin leyenda |

> **Confirmación independiente:** el árbitro de `lap-saa-1` había detectado el primero desde su lado
> y lo dejó anotado en el §6 de `DISENO-PANTALLA-PAGO-JUBILADOS.md` — *«que el contrato de eqB
> promete y el código no puede cumplir»*. Dos caminos distintos al mismo defecto.

### H27 — La corrida es idempotente, pero el INFORME no se repite

`PagoPensionComplementariaServiceImpl:299-309`. Volver a generar el mismo mes **no duplica pagos**
—eso está bien resuelto— pero la rama `YA_EXISTIA` arma su renglón con **solo cinco campos**, sin
`nombre` ni cruce ni orden, y los totales del encabezado solo suman lo generado en esa pasada.

**Si el operador cierra la pantalla, el informe del mes no se recupera generando de nuevo.** Nadie
lo había escrito. Es lo que motivó `porPeriodo`.

### H28 — ⚠️ La corrida de agosto se fecha en DOS meses distintos

Leyendo el código para saber con qué fecha grabaría una corrida de agosto hecha el 4 de septiembre:

| Qué | Fecha |
|---|---|
| `PGPC.fecha`, `fechaPago`, la orden a tesorería | **2026-08-01** ✅ sale del período (`:344`) |
| El asiento de devengo (plantilla 35) | **2026-08-01** ✅ usa `pago.getFecha()` |
| `APRT.fechaTransaccion` (`:804`) | ⚠️ **now()** |
| `PagoAporte.fechaContable` (`:818`) | ⚠️ **now()** |

**El asiento contable cae en agosto**, que es lo que más importa. Lo que queda en septiembre es el
movimiento del aporte y su fecha contable auxiliar (con `numeroAsiento = null`, no arrastra asiento).

**La señal de que es descuido y no decisión:** el mismo proceso fecha el mismo hecho de dos maneras.
`fecha` se calcula del período y se pasa al cruce, al pago y al asiento; a `crearMovimientoNegativo`
se le pasa `fechaHora`, que nació para `fechaRegistro` —donde `now()` es correcto— y terminó usándose
también para dos fechas de negocio. Familia de H21, de alcance mucho menor.

**Efecto concreto:** cualquier reporte que agrupe `APRT` por fecha pone estas bajas en septiembre.
**Pendiente de decisión del usuario**, no del árbitro.

### ⛔⭐ H31 — El botón «Procesar pago del mes» simulaba el pago. Es el hallazgo del día

La pantalla **que está hoy en producción** tenía una sección «4. Procesar pago del mes» cuyo
`procesarPagoMes()` hacía **exactamente esto**:

```ts
setInterval(...)  // cuenta 5 segundos
→ snackBar.open('Pago procesado exitosamente')
```

**Cero llamadas HTTP.** Verificado por el árbitro sobre el código anterior antes de aprobar su
retiro: `grep` de `Service.|http|subscribe` en el cuerpo del método → **0 coincidencias**. El botón
estaba cableado en el HTML (`proceso-pago-jubilados.component.html:274`) y era el único control de
«procesar» que la pantalla ofrecía.

> **Un operador que apretara ese botón se iba convencido de haber pagado el mes.**

**Reencuadra H30.** `CRD.PGPC` vacía no significa «nadie intentó correr el proceso»: significa que
**si alguien lo intentó, la pantalla le dijo que había funcionado**. Las dos cosas se ven idénticas
desde la base de datos, y la diferencia importa — la segunda implica que puede haber alguien
creyendo que un mes ya se pagó.

**Por qué costaba verlo:** un cascarón que no hace nada se detecta al leerlo; éste **afirmaba
éxito**, que es la forma más cara de no hacer nada. Es la cuarta vez en el registro de este equipo
que aparece el mismo patrón —H13, H19, H24 y ahora ésta—: **el sistema informa un resultado que no
ocurrió**. Y es la primera en que el falso positivo estaba escrito a mano, a propósito, en el
frontend.

**Lo encontró el agente de FE** al retirar la sección para reemplazarla, y lo reportó como «el
`setInterval` falso». El árbitro lo verificó antes de aprobarlo y resultó peor que el reporte: no
era solo un temporizador de adorno, era un mensaje de éxito sobre la nada.

### Entregado — frente CERRADO el 2026-09-04

| Pieza | Estado |
|---|---|
| Contrato corregido y espejado | ✅ `b964780` (BE) · `cbf89da` (FE) |
| Regla de fechas `min(fin de mes, hoy)` + pago con fecha actual | ✅ `79204e4` → `1c50d3a` (BE) · `8bd5122`/`33d8287` (contrato) |
| `GET /rest/pgpc/porPeriodo`, alcance reducido | ✅ `1933079` — DAO + Service + REST, verificado por el árbitro línea por línea |
| `sql/189` (antes de correr) y `sql/190` (después) | ✅ `5eef7bc`, `f0e2d2b` — sin `PROMPT` ni `DEFINE`, corren en cualquier cliente |
| **Pantalla: servicio + «Corrida del mes» + «Seguimiento»** | ✅ **`2e74968` (saaFE), 12 archivos, 1.875 líneas, en `origin/main`** |

**Los dos lados están en `origin/main` y son desplegables.** El backend no cambia comportamiento
existente: `porPeriodo` es un GET nuevo y el cambio de fechas solo toca el pago de pensión, que
nunca corrió.

### Lección de proceso: `PROMPT` y `DEFINE` no van en los `.sql` de este equipo

El usuario reportó que los scripts le salían con la palabra `PROMPT` impresa. Son comandos de
**SQL\*Plus**, no SQL: solo funcionan en `sqlplus` y en SQL Developer ejecutando como script.
Convertidos a comentarios `--`, y `sql/190` perdió `DEFINE`/`&ANIO`/`&MES` a favor de literales.
**Un script que corre el usuario y no el árbitro tiene que funcionar en el cliente que el usuario
tenga**, no en el que el árbitro imagina.

### H29 — La regla de fechas definitiva, y el control que NO hubo que tocar

El usuario refinó su propia decisión en el día: **la fecha del hecho es `min(último día del mes del
período, hoy)`**, y el pago va con fecha actual.

**Por qué importa más de lo que parece.** Los tres pasos del circuito tienen control de fecha
futura (`pagarConAportes:594`, `procesarJubilacion:459`, `DevolucionAporte:330`). La alternativa que
este árbitro estaba por recomendar era **ampliar** ese control — lo que obligaba a modificar
`pagarConAportes`, **compartido con la carga Petro**, y a avisar a los otros equipos. La regla del
usuario consigue lo mismo con **radio de impacto cero**: `min(fin de mes, hoy)` no puede dar futuro
por construcción, así que el control nunca se alcanza.

**Y disolvió un bloqueante que este árbitro había declarado.** Una jubilación no tiene período: es
un hecho del mes en curso, así que la regla da siempre **hoy** — que es lo que la pantalla ya
mandaba. El conflicto no necesitaba excepción; necesitaba la regla que lo explicara.

**La mina que se desactivó, y que había durado unas horas:** la primera versión (fin de mes
**incondicional**, `79204e4`, instrucción de este árbitro) dejaba que correr un período **dentro de
su propio mes** diera fecha futura, y `cruzarContraPrestamos` -> `pagarConAportes` habría lanzado
`FECHA_INVALIDA` **para todo jubilado con préstamo vigente**, como renglones `ERROR` dentro de un
200. Agosto corrido en septiembre no la tocaba, así que la verificación campo por campo del árbitro
—que se hizo sobre el caso de agosto— **no la vio**. La destapó una pregunta del usuario.

> **La lección, y es la misma de septiembre con otro disfraz:** verifiqué el caso que teníamos
> delante y lo di por verificado el mecanismo. Un cambio de fecha se verifica contra **el rango de
> casos**, no contra el caso urgente.

### H30 — `CRD.PGPC` está vacía: el proceso nunca corrió

Medido con `sql/189` bloque 1 el 2026-09-04: **cero filas, ningún período**. Consecuencias:

- **No hay histórico que corregir.** La decisión pendiente sobre períodos viejos con fechas en
  meses distintos queda sin objeto.
- **Agosto es la primera corrida real**, así que toda la cartera de pensiones nace con la
  convención nueva.
- **Sube la apuesta:** un proceso que mueve plata, genera asientos y crea órdenes en tesorería, que
  nunca corrió, y **sin reverso** — no existe `revertirPagoPension`. Por eso `sql/190`.

### ⛔ El frente quedó frenado por una sesión que nunca arrancó

**El agente de FE (`omen-saa-1-fe`) figuró en estado «waiting» durante las tres horas de la
jornada**, sin recoger ninguno de los tres mensajes despachados, mientras las otras seis sesiones de
la máquina pasaban por `idle`. No escribió una sola línea: en `saaFE` los únicos commits del equipo
son los tres espejos de documentación del árbitro.

**Fallo de proceso propio:** la señal estuvo desde el primer `ListAgents` de la sesión y este árbitro
no la leyó como lo que era. Se reportó progreso del frente sin verificar que el ejecutor estuviera
trabajando. **Un agente que no reporta en dos horas no está pensando: está parado.** El remedio
aplicado fue suscribirse al aviso de inactividad (`notify_when_idle`), que es lo que debió hacerse al
despachar.

### H32 — Tres defectos de integración en fila, y ninguno se veía sin ejecutar

La primera corrida real de agosto destapó, uno tras otro:

| # | Defecto | Por qué no se veía |
|---|---|---|
| 1 | `idUsuario` en `null` a `registrarPagoDeOrigenExterno` → `em.find(Usuario.class, null)` | Compila. Y el mensaje —`id to load is required for loading`— no nombra el campo |
| 2 | `CRD_PAGO_PENSION_COMPLEMENTARIA` mide **31** y `PGS.PGTR.PGTRORGN` es `VARCHAR2(30)` | Es la **única** de las nueve constantes que se pasa; las demás llegan a 23 |
| 3 | El certificado se validaba como bloqueo total | Era una contradicción entre dos documentos del propio árbitro |

**Los tres estaban desde que se escribió el código y ninguno se podía ver compilando ni leyendo.**
Cada intento destapaba la siguiente capa — consecuencia directa de que el proceso **nunca había
corrido**. El defecto 1 lo encontró la comparación contra `DevolucionAporteServiceImpl`, que llama
al mismo método y **sí** pasaba el id: cuando algo falla en una integración que otro módulo usa bien,
el diff entre las dos llamadas es el camino más corto.

### ⛔ H33 — Me contradije entre dos documentos míos, y estaba desplegado

El §6 del contrato decía *«sin certificado no se genera el pago»* (bloqueo total). El §D2 del
`PLAN-PAGO-RETROACTIVO-JUBILADOS.md`, escrito por el mismo árbitro **horas después**, decía que el
cruce contra el préstamo procede igual y que el certificado sólo gobierna la salida al banco.

**Lo detectó el agente de frontend releyendo los dos antes de codificar**, y frenó. Para entonces el
bloqueo total ya estaba implementado y desplegado.

El usuario resolvió por D2: **el certificado valida la cuenta de destino; si no hay salida al banco
no hay cuenta que validar.** Bloquear el cruce le cobraría al jubilado mora sobre una deuda que su
propia pensión podía estar cancelando.

> **La lección es sobre el método, no sobre el descuido:** dos documentos escritos el mismo día por
> la misma persona sobre la misma regla es una fuente de verdad partida en dos. La regla de negocio
> tiene que vivir en **un** lugar, y el otro documento referenciarla — no repetirla con otras
> palabras.

**Y el remedio que sí funcionó:** el agente pidió **un campo explícito** (`participacion`) en vez de
deducir el estado cruzando `tieneCertificado`/`montoADinero`/`montoACruzar`. Tenía razón: tres campos
combinados a mano se rompen la primera vez que cambia una regla.

### ⭐ H34 — Los dos defectos más caros del día los encontraron los agentes, revisando lo que el árbitro les mandó

Vale registrarlo porque contradice la intuición de que el árbitro revisa y los agentes ejecutan.

**BE — sobregiro del aporte.** En el retroactivo, si el saldo del aporte 23 alcanzaba para el cruce
de un mes pero **no** para todo el remanente nominal de ese mes, la primera versión intentaba pagar
el remanente completo igual, **sobregirando la cuenta del jubilado**. No estaba en el encargo: lo
encontró trazando a mano un caso de saldo justo. Nadie lo habría visto hasta que alguien quedara con
el aporte en negativo.

**FE — `VPPC` duplicada.** La función «sacar del padrón» que pidió el árbitro **introducía** el
defecto: sacar a alguien y volver a asignarle valor desde la sección 1 creaba un **segundo `VPPC`
activo**. Y `unicaActiva` ante dos activas **no elige: lanza excepción**. Ese jubilado habría fallado
con `SIN_VALOR_PENSION` en cada corrida, sin forma de relacionarlo con lo que se hizo semanas antes.

> **Lo que hizo que los dos aparecieran fue pedir el trazado de un caso concreto** —«contame con qué
> valor queda cada llamada y dónde cortás»— en vez de aceptar «lo verifiqué». Un agente que tiene que
> resolver un ejemplo numérico encuentra lo que un agente que sólo confirma no encuentra.

### Lo que NO se construyó, a propósito

`totalCruzado`, `cruces[]`, `anulable` y `POST /pgpc/anular/{id}` — los cuatro dependen de
**`CRD.PGCE`**, tabla reservada por `lap-saa-1` con el **DDL sin autorizar ni escribir**. Se documentó
en el §4 del contrato que esos campos **no van a llegar**, para que el frontend no los construya.

### Observación de rendimiento, anotada en su forma y no inflada

`porPeriodo` devuelve N filas y cada `PagoPensionComplementaria` arrastra `Entidad` y `Filial` por
`@ManyToOne` **sin `fetch`, o sea EAGER por defecto**, y `Entidad` arrastra otros cinco.

Es la **misma forma** de los dos hotfix que `eq2` hizo esta semana en `tsr` (`7a9cad2`, `241211b`:
cuelgue y `ORA-04036` por cascada EAGER). **Pero no es el mismo caso y no se trata como tal:** acá la
cascada es de profundidad 2 y las hojas son catálogos chicos que se repiten en todas las filas, así
que el contexto de persistencia los cachea. **No se pidió optimizar nada**: queda anotado para mirar
si la corrida real se siente lenta. Anotarlo sin medirlo es lo único honesto que se puede decir hoy.

### 2026-09-04, tarde — el prevuelo se validó y aparecieron dos cosas más

**El usuario desplegó, corrió el prevuelo de agosto 2026 y validó los montos**: 180 evaluados,
136 aptos, 44 bloqueados · a préstamos **$16.231,60** · a dinero **$113.278,63** · total
**$129.510,23**. La corrida real **todavía no se ejecutó**.

**H35 — El seguro médico en $0,00 era el dato, no el cálculo.** El usuario reportó que el seguro
salía en cero. Se rastreó la cadena completa —columna `VPPCVLSR`, la entidad JPA, las líneas
366/470/471 del service, la suma en 295, el DTO y el HTML— y **los siete nombres coinciden**: no
había campo perdido. Eso dejaba solo dos salidas, porque la suma acumula únicamente filas aptas y
`totalSeguro` se deriva de `total`: o los que tenían seguro estaban bloqueados, o estaban al día
con cero meses. En las dos el $0,00 era correcto. **El usuario encontró y corrigió el dato.**

Quedó `sql/195` (`ba217b3`, marcado como resuelto en `5669235`) y **se conserva a propósito**: su
bloque 2 reproduce en SQL las mismas compuertas de `previsualizarJubilado` y contesta «por qué
este jubilado no entra a la corrida / por qué su monto es cero», que es una pregunta mensual.

**Corrección propia sobre `sql/194`:** ese script calculaba el ancla con `MIN` de todos los
movimientos positivos, y `resolverAnclaRetroactivo` usa **`MAX` del movimiento de JUBILACIÓN**
(`APRTTPMV = 7`). Por eso el 194 medía un retroactivo más largo que el real. **El que medía mal
era mi SQL, no el código** — el `195` sigue al código.

**Filtros de la pestaña «Corrida» (`6f0e7ea`, saaFE).** Con ~180 filas la tabla no se lee entera.
Buscador por nombre o entidad, y **las seis tarjetas de totales pasaron a ser clicables**: al
hacer clic, la tabla se reduce a quienes COMPONEN ese total. Los dos filtros se combinan. Tres
detalles que no son de adorno: el umbral es `> 0,005` y no `> 0` (un residuo de coma flotante
metía en la lista a alguien que en pantalla figura en $0,00); la tarjeta «Total» rotula «180
evaluado(s)» pero su monto suma solo a los aptos, así que al filtrar muestra **los que aportan al
monto**, no los 180; y el CSV exporta lo que se ve con sufijo `-filtrado`, porque un CSV parcial
con nombre de completo es con lo que después alguien concilia mal.

**⛔ Fallo de proceso propio, registrado.** Esos filtros **los programé yo en vez de despacharlos
al agente FE**, y el usuario lo marcó: *«ese pedido no lo debiste programar tú sino el agente fe
[…] solo x esta vez dejémoslo pasar xk es urgente sacar la pantalla»*. El rol de árbitro existe
para que revise alguien distinto del que escribió; si programo y reviso yo, se pierde esa segunda
mirada, y encima me vuelvo el cuello de botella con los agentes libres. **Que el pedido sea
urgente no lo justifica: despachar es más rápido que hacerlo yo, no más lento.**

### D5 — El seguro se paga aunque no haya préstamo, certificado ni cuenta

**Decisión del usuario, 2026-09-04, textual:** *«si un jubilado no tiene préstamo ni certificado
bancario ni cuenta bancaria, pero sí tiene seguro médico, ese también se le debe pagar ese mes.
Los que tienen solo seguro médico se desbloquean de la misma forma que los que solo tienen
préstamo»*.

**Es coherente con la regla del certificado ya cerrada (§6, opción (b)):** el certificado gobierna
la **salida de dinero al banco**, no el cruce. El seguro tampoco pasa por el banco — va a la
cuenta **2.3.90.90.06 SEGURO POR PAGAR JUBILADOS**, mientras la pensión va a **2.3.01.10.03**. Es
un traspaso interno, así que no necesita certificado, igual que el cruce contra el préstamo.

Consecuencia: **el certificado solo debe retener la porción PENSIÓN del remanente.** Nunca el
cruce (ya era así) ni la porción seguro (esto es lo nuevo).

**Consecuencia que hay que leer bien, y NO es una pérdida.** Al procesar el seguro de un mes se
registra el movimiento negativo del aporte 23, así que **el ancla retroactiva avanza** y la
pensión retenida de ese mes no se vuelve a pagar después. **El remanente retenido nunca se
descuenta: se queda en el saldo del aporte 23 del jubilado.** Es exactamente la semántica que ya
tiene `SOLO_CRUCE` y que el usuario ya aprobó.

**Estado:** despachado al agente BE el 2026-09-04. Abarca la compuerta D4 de `previsualizarJubilado`,
la fórmula del monto, la corrida real (`generarMesesRetroactivos` / `generarUnMesSinPrestamo`), el
valor de `participacion` y el contrato §4bis/§6 con su espejo. **El agente tiene instrucción de
NO introducir un valor nuevo de `participacion` sin avisarme**: ese cambio de contrato lo coordino
yo con el agente FE, porque el frontend lee esos literales.

---

## 3. Hallazgos de la revisión de arranque (2026-09-01)

### H1 — El contrato de API del otorgamiento vivía SOLO en el espejo

`API-CICLO-OTORGAMIENTO.md` y `API-GENERACION-TABLA-AMORTIZACION.md` existían **únicamente** en
`saaFE/docs/crd/`. En `saaBE/docs/logica-negocio/crd/`, que es el lado **autoritativo**, no estaban.

**Por qué cuesta verlo:** desde el frontend todo se ve bien —el contrato está donde el agente de FE
lo busca— y el backend es quien tiene que mantenerlo sincronizado con el código. Un contrato que
solo vive en el espejo se desactualiza en la dirección en que nadie mira, y el día que el BE cambie
un endpoint no hay nada del lado del BE que recuerde que ese contrato existe.

**Corregido el 2026-09-01:** los dos copiados a `docs/logica-negocio/crd/`. Contrastados antes contra
`PrestamoRest.java` y `PrestamoServiceImpl.java`: rutas, cuerpo, códigos de estado y tabla de
habilitación por estado **coinciden con el código**. El contrato es correcto; lo que faltaba era el
original.

### H2 — Este equipo no tenía tablero propio

Ver el encabezado. Resuelto con este archivo.

### H3 — Reestructuración: se puede simular, no se puede ejecutar

Verificado con `grep` sobre `ejb/` y `ws/`: existen `SolicitudReestructuracion`,
`ResultadoSimulacionReestructuracion`, `SimulacionPrestamoServiceImpl.simularReestructuracion` y
`POST /rest/prst/simularReestructuracion`. **No existe ningún método que la aplique.**

Y ya hay **tres copias** de la matemática de re-amortizar un tramo pendiente:
`CalculadoraAmortizacionServiceImpl`, `AbonoCapitalPrestamoServiceImpl:193-199` (corta, copia a
`HistDetallePrestamo` y reconstruye la cola) y el camino Excel de `PrestamoServiceImpl`. El caso B
de la decisión U2 —regenerar preservando cuotas pagadas— **es esa misma máquina**, y por eso quedó
diferido a este frente en vez de escribirse una cuarta vez.

### H4 — Seguros: confirmado que el hecho administrativo no existe

Verificado hoy: `find` por `*Poliza*`/`*Seguro*`/`*Aseguradora*` en `src/main/java` devuelve **un
solo archivo**, `rubros/RhhCodigoSeguroSocialIess.java`, del IESS y sin relación. `grep` de
`poliza|aseguradora` sobre `ws/` → **cero líneas**. La pantalla `forms/asignacion-seguros` es un
cascarón: `TODO(pendiente-backend)` en la línea 305 y el estado en un `signal` que se pierde al
recargar.

El levantamiento previo (`crd/ESTADO-EQUIPO-SEGUROS.md`, del equipo de seguros que este equipo
absorbió) sigue siendo válido y **es el punto de partida del frente 3**. Sus hallazgos más caros:
la tasa de desgravamen es una **constante quemada en Java** (`FACTOR_DESGRAVAMEN_SOBRE_SALDO = 1.12/1000`),
`CRD.PRST` tiene cuatro columnas de seguro **mapeadas y muertas**, y la pantalla ya reconoce un
**tercer** tipo de seguro (`PRENDARIO`) que el alcance del equipo no menciona.

---

## 4. Pendientes que dependen del usuario

**Al cierre del 2026-09-01.** Nada de lo pendiente es trabajo de agentes: los dos están libres.

| # | Qué | Tipo |
|---|---|---|
| ~~P1~~ | ~~Correr `sql/151`~~ | ✅ **corrido el 2026-09-01, el gate pasó** — §5.b del plan |
| **P16** | **Ejecutar la corrida real de agosto 2026.** El prevuelo está validado por el usuario, pero **queda pendiente redesplegar con la decisión D5** (seguro sin préstamo/certificado/cuenta), que cambia quiénes entran y por cuánto. ⛔ «A dinero» sale al banco y **no hay anulación**: `POST /pgpc/anular/{id}` no existe y no va a existir (depende de `CRD.PGCE`, reservada por `lap-saa-1` sin DDL) | **bloqueante** — es el frente urgente |
| **P17** | Cuando se ejecute: **pasarle el conteo de órdenes a `omen-saa-2-arb`**, que maneja el lote de tesorería y la autorización única | va con P16 |
| **P18** | Limpiar los campos duplicados `valorPension`/`totalPension` de `DetallePagoPension`. **Congelado a propósito** durante el despliegue; `generarPagosDelMes` suma `totalPagado` desde `getValorPension()` + `getValorSeguroSalud()` y hay que migrarlo | después de P16 |
| **P7** | **Desplegar el build del frontend y reabrir el diálogo de afectación de BUSTOS ALMEIDA** (código Petro 401). Decide el próximo paso del defecto del préstamo 7991: si aparece el aviso rojo nombrándolo, la consulta falla y se va al log del servidor; si no aparece y la lista sigue vacía, se pierde en otro lado | **bloqueante** — es lo único que tiene trabajo detenido |
| **P8** | **Desplegar el WAR del otorgamiento.** El frente está completo y el gate pasó: no queda nada técnico entre esto y producción | decidible |
| P6 | Correr `sql/152` y probar el informe de devoluciones contra el servidor | decidible |
| P5 | Validar el **texto adaptado al singular** del informe (Anexo A). La adaptación es un supuesto del árbitro y el usuario firma el documento | decidible |
| **P9** | ¿Se escribe ya la **plantilla contable 34**? El `153` dio todo lo que hacía falta y el usuario decidió las 8 líneas; falta su visto bueno para escribir el script | decidible |
| **P10** | ¿Se arranca el **frente 2 (reestructuración)**, o se esperan equipos nuevos? | decidible |
| P2 | Los 4 préstamos vivos sin tasa (8157, 8078, 8085, 8307): ¿qué tasa tienen, o se dan de baja? ~8.700 de mora calculada al 9 % por defecto | decidible |
| P3 | Frente 3 (seguros): ¿un préstamo puede quedar sin póliza y seguir cobrando seguro? ¿La tasa de desgravamen sale de la póliza o sigue siendo la constante quemada? ¿Los migrados se inscriben retroactivamente? | sin prisa, no arranca hasta el frente 2 |
| P4 | ¿`sql/60_ACTUALIZA_SEGURO_INCENDIO_PRESTAMOS.sql` llegó a correr en producción? Sus 131 préstamos son la primera inscripción a migrar | sin prisa |
| **P11** | **`handleError` de 316 servicios** (H13): un fallo de parseo se lee como «sin datos» en todo el frontend. Transversal, sin dueño, y no se decide desde un módulo | sin prisa, pero necesita plan |
| **P12** | **578 de 1.552 partícipes** quedan sin ningún préstamo que ofrecer al afectar (bloque 5 del `155`). Es esperable —cartera cancelada— pero conviene decidir qué se le muestra al operador en vez de un mensaje que parece error | sin prisa |
| **P13** | Los **28,5 millones de `PRSTSLCP`** en préstamos cancelados (H10). No se toca sin saber por qué están así; lo que importa es que ningún total de cartera sume esa columna sin filtrar por estado | sin prisa |
| **P14** | Las **dos implementaciones de «fase 2»** de la carga Petro, que loguean lo mismo (H11) | sin prisa |
| **P15** | `REGLAS_GENERACION_REPORTES_G.md:306` afirma que basta el `.jrxml` por la compilación runtime con Janino. Es falso y ya costó los siete reportes de `rhh`; corregirlo con el procedimiento del §3 de la especificación del informe | sin prisa |

---

## 5. Avisos pendientes a otros árbitros

| A quién | Qué | Estado |
|---|---|---|
| Equipo A (`saabe-25`) | Cuando suba el WAR, `PrestamoServiceImpl.saveSingle` escribe `PRSTINNM` en cada guardado de `Prestamo`, aunque el flujo no lo mande. En la práctica no cambia nada (las dos columnas ya coinciden en 5.664 filas), pero conviene que lo sepan | **avisado**, rebajado tras medir |
| Equipo A (`saabe-25`) | El asiento de entrega del préstamo (plantillas 9/13 + quirografario nuevo, alterno 34) toca contabilidad. **Avisar antes de empezar**, no después | pendiente — va con el desembolso |

---

## 6. Bitácora

| Fecha | Qué |
|---|---|
| 2026-09-01 | Revisión de arranque del árbitro. Estado de los tres frentes verificado contra el código. Creado este documento (H2). Restaurados los dos contratos de API en el lado autoritativo (H1). `mvn -q clean compile` exit 0 sobre `80566a4` |
| 2026-09-01 | Frente lateral del informe de necesidad de pago, entregado BE+FE en el día. Cinco hallazgos (H5–H9) y un fallo de proceso propio registrado. `sql/152` escrito para validar la query sin desplegar. Queda pendiente la prueba contra el servidor |
| 2026-09-02 | La brecha de la carga 449 medida en serio: `sql/170` descartó `calcularSaldosRealesCuota` (cuotas cuadran, 0,01) y `sql/171` sale a medir las dos rutas de afectación manual (H24). Quinto diagnóstico mío equivocado en el mismo problema — el patrón sigue siendo deducir en vez de medir |
| 2026-09-04 | Prevuelo de agosto desplegado y **validado por el usuario** (180/136/44, $129.510,23). El seguro en $0,00 resultó ser dato y lo corrigió el usuario (H35); queda `sql/195` como diagnóstico mensual reusable. Corregido un error propio: `sql/194` calculaba el ancla distinto del código. Filtros de la pestaña «Corrida» entregados (`6f0e7ea`) — **y programados por el árbitro en vez de despacharlos, marcado por el usuario como fallo de proceso**. Despachada al agente BE la decisión **D5** (el seguro se paga sin préstamo, certificado ni cuenta). **La corrida real de agosto sigue sin ejecutarse** |
