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
