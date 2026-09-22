# ESTADO — equipo `omen-saa-1` (CRD · EQUIPO B: ciclo del crédito y seguros)

**Árbitro:** `omen-saa-1-arb` (máquina **omen**) · **Agentes:** `omen-saa-1-be`, `omen-saa-1-fe`
**Creado:** 2026-09-01 · **Marcador de commit:** `eqB` · **Este documento lo mantiene SOLO este equipo.**

---

# ⛔ FRENTE DE JUBILADOS: CERRADO Y PAGADO — 2026-09-07

**Lo confirmó el usuario: «listo, todo ok».** La corrida de agosto 2026 corrió contra produccion,
los pagos se confirmaron y el dinero salio al banco. **No queda nada pendiente de este frente.**

Este bloque esta escrito para alguien que **no participo de nada de esto**: los equipos que
siguen arrancan con memoria limpia y este documento es todo lo que van a tener.

## Lo que quedo funcionando, y donde mirar si algo falla

| Pieza | Archivo | Contrato |
|---|---|---|
| Corrida mensual de pension | `crd/serviceImpl/PagoPensionComplementariaServiceImpl.java` | `crd/API-PAGO-PENSION-COMPLEMENTARIA.md` |
| Auditoria de bandas (el cuadre contra contabilidad) | origen `PAGO_PENSION` -> `CRD.PGPC` | `crd/API-AUDITORIA-BANDAS.md` |
| Reporte de la corrida | `rep/crd/RPRT_PGPC_CRRD.jrxml` + `.jasper` | `API-PAGO-PENSION...md` §8 |
| Verificacion contra el mayor | `crd/sql/203` | — |
| Producto de pago 517 | creado en produccion con `crd/sql/202` | — |

## Las cinco correcciones, todas verificadas con asientos reales de produccion

1. **Banda del dia de vencimiento** — el dia del vencimiento es POR VENCER, y **sin `+1`**. La
   cuota paso de `1.3.04.05` a `1.3.01.05`. ⚠️ **Las dos cuentas se llaman «DE 1 A 30 DIAS»**: si
   alguien verifica por nombre concluye que no cambio nada. Arreglado por `lap-saa-1`.
2. **El devengo iba por el nominal y duplicaba saldo** — el cruce debita la MISMA cuenta
   (`2.1.02.25.01`). Ahora va por `remanente`: 107,39 + 481,78 = 589,17.
3. **Cierre de cuentas de apertura** — `D 2.3.02.10 / H 1.4.05.10`. Arreglado por `lap-saa-1`.
4. **Una autorizacion por jubilado** — **no era defecto**: la pantalla soporta seleccion multiple.
   El operador marca todos y aprueba **una sola vez**.
5. **Faltaba el asiento de pago de cada jubilado** — la orden iba sin `desglose` y CXP no arma
   asiento sin el (**su diseño, no un defecto ajeno**). Ahora lleva el producto **517**.

## ⛔ Lo unico peligroso que sigue vivo: H46

**Si una corrida falla a mitad de camino, NO se puede reintentar.** El movimiento negativo de
`CRD.APRT` sobrevive a cualquier reverso (la tabla es append-only y el contra-movimiento solo
agrega un positivo), asi que el ancla queda envenenada y **el reintento informa «al dia» y no le
paga a nadie, sin lanzar ningun error**.

**La unica salida hoy es restaurar la base.** Medido: `anularOperacion` tampoco lo resuelve, y una
solucion generica **pide DDL**. El detalle completo esta mas abajo en la seccion H46 — la
investigacion ya esta pagada, no hay que rehacerla.

⇒ **Regla para la corrida de cada mes: tener el respaldo listo ANTES de correr.**

## Lo que queda abierto y hereda quien siga

| # | Qué | Estado |
|---|---|---|
| **P22** | Pantallas de calificación de riesgo (`/rest/cfcr`, `/rest/escr` + FE). **Código listo de las dos puntas, NUNCA probado contra producción.** Contrato en `crd/API-CALIFICACION-RIESGO.md` | a probar |
| **H46** | El ancla envenenada (arriba). Pide DDL para resolverse de verdad | medido, sin abrir |
| **P18–P21** | `valorPension` duplicado; marcar proveedor y grupo de producto por rol; dos fuentes de verdad de la cuenta; precancelación sin cuotas pagadas previas | sin arrancar |
| **—** | **Verificar contra producción** que existan las FK de `DDL-COBRO-PETRO-DOS-PASOS.sql` y `DDL-COBROS-APROBACION-CONTABILIDAD.sql`. Los scripts traen la verificación, **pero eso no prueba que alguien la haya corrido** | preguntado al usuario, sin respuesta |

## Grabar el motivo de bloqueo — pendiente que nadie tomó

Hoy **nadie puede contestar «¿por qué este jubilado no cobró en agosto?» tres meses después.** El
motivo existe solo en la respuesta HTTP del momento en que corre el proceso y **no se persiste en
ninguna tabla**. El reporte lo dice en una nota al pie porque no hay de dónde sacarlo. Es la
primera pregunta que hace un auditor.

## Herencia de `omen-saa-2` (tesorería) — cuatro cosas que nos rozan

Ese equipo también cerró. Lo suyo queda en `ESTADO-EQUIPO-OMEN-2.md`; esto es lo que nos toca:

1. **El arreglo de `com.saa.basico` quedó ESPECIFICADO, NO HECHO.** `DetalleRubroDaoServiceImpl:77`
   debería convertir un catálogo faltante en un mensaje que **nombre el rubro y el detalle**, en
   vez del `NoResultException` opaco de hoy. Su §29/§29bis/§29ter tiene la especificación completa,
   el criterio de aceptación y el análisis de riesgo **de los dos equipos**. ⚠️ **No rehacer el
   análisis**: los 23 consumidores, el `catch (PersistenceException)` de `EntityDaoImpl:95` que no
   aplica, y las dos formas del defecto (fila ausente **y** fila con `PDTRVLRV` vacío, que mete la
   palabra literal `"null"` en el JPQL) ya están medidos. Nunca se aprobó, no llegó ningún diff.
2. **`TSR.DTCN` puede no tener su FK a `CNT.DTAS`** — mismo defecto del `GRANT` comentado, **sin
   medir**. Toca `cnt`, que está en nuestro alcance.
3. **El RUC con espacio crea titulares DUPLICADOS** en la carga SRI. El caso del titular 156 que
   encontramos era la punta: `buscarTitularPorRuc` no trimea y **su llamador crea el titular si no
   lo encuentra**.
4. ⚠️ **La guarda de `aprobar` enumera orígenes.** Cualquier origen nuevo de `crd` **pasa la
   aprobación y revienta al generar el archivo del banco, con el lote ya aprobado**. Es lo que casi
   pasa con `CRD_SEGURO_JUBILADOS`. Tenerlo presente al agregar un origen.

---


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

### ⛔ 2026-09-05 — LA CORRIDA SE EJECUTÓ. Cinco hallazgos abiertos, y el 5 es grave

**La corrida de agosto 2026 corrió en producción.** Antes hubo dos intentos fallidos, los dos
revertidos por el usuario, y de cada uno salió un defecto que hoy está corregido:

| Intento | Falla | Causa real | Estado |
|---|---|---|---|
| 21:13 | 137 de 181 con error | **`idOrigen=null`**: la orden a CXP se pedía con el PGPC **sin grabar**, así que `getCodigo()` daba null. Es la razón de fondo por la que `CRD.PGPC` estaba vacía — el proceso nunca había llegado tan lejos | ✅ `62cfc1b` |
| 21:33 | 137 de 181 con error | **`aux1ParaTipoAporte` no conocía el tipo 23.** Mapeaba solo 9/11/2. Reventaba **después** de aplicar el cruce | ✅ `9e36de4` + línea `aux1` 53 → `2.1.02.25.01` en `CNT.DTPL` |

**Verificado y tranquilizador:** el `REQUIRES_NEW` por jubilado revierte limpio —`IncomeException`
es `@ApplicationException(rollback=true)`— así que las corridas fallidas **no dejaron cuotas
pagadas huérfanas**. Lo confirmó el agente BE en tres puntos de la cadena.

**Las cuentas del aporte 23, con el catálogo como prueba.** El usuario resolvió una discrepancia
que el agente hizo bien en no decidir solo: al **jubilar** nace el pasivo (`2.3.01.10.03`,
PENSIONES COMPLEMENTARIAS POR PAGAR); al **cruzar** contra el préstamo no se paga nada, se da de
baja el saldo individual (`2.1.02.25.01`). Los datos lo confirman — las cuatro líneas del cluster
son de la misma familia:

| `aux1` | Cuenta | Nombre |
|---|---|---|
| 50 | `2.1.01.05.01` | APORTES PERSONALES CESANTIA |
| 51 | `2.1.02.05.01` | APORTES PERSONALES JUBILACION |
| 52 | `2.1.02.15` | APORTE ADICIONAL PERSONAL |
| **53** | **`2.1.02.25.01`** | **CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS** |

Todas `2.1.0X` — cuentas individuales del partícipe. La de jubilación es `2.3` — pasivo. **Que
alguien no "corrija" una de las dos creyendo que la otra está mal:** está en el §4quinquies del
contrato con la cita textual del usuario.

#### Los cinco hallazgos de la corrida ejecutada — TODOS ABIERTOS

### ✅ CERRADO 2026-09-05 — las cinco novedades verificadas EN PRODUCCIÓN, con asientos reales

No «verificado en código»: el usuario desplegó, corrió agosto 2026 contra producción y confirmó
los pagos. Estado final de cada una:

| # | Novedad | Cómo se verificó |
|---|---|---|
| **1** | Fecha de cruce / banda | El asiento del cruce pasó de `1.3.04.05` (vencidos) a **`1.3.01.05`** (por vencer). ⚠️ **Las dos cuentas se llaman «DE 1 A 30 DIAS»** — la diferencia está en la familia (`1.3.01.xx` = por vencer, `1.3.04.xx` = vencido), no en el nombre. Fácil de dar por no corregido si se mira solo la etiqueta |
| **2** | Saldo duplicado | Devengo de GARCIA SALTOS en **107,39** (era 589,17). Con el cruce de 481,78, `2.1.02.25.01` recibe 589,17 exactos |
| **3** | Cierre de apertura | `D 2.3.02.10 / H 1.4.05.10` por **481,78**, descripción «Cierre de apertura - vencido y cuota del mes - cruce de valores evento 396». Asiento cuadrado en 963,56 |
| **4** | Autorizaciones | No era defecto — paso de operación |
| **5** | Asiento de pago por jubilado | **Confirmado por el usuario: los pagos se confirmaron y los asientos salieron correctos** |

**Después de verificar, el usuario revirtió la base a propósito**, para que quede sin asiento de
pago: **el pago real al banco se hace el lunes**. O sea que lo probado es el código, no la corrida
definitiva — **agosto se vuelve a correr el lunes** y ésa es la que va contra el banco.

#### ⚠️ Lo que hay que tener presente para la corrida del lunes

1. **El WAR ya es el correcto** (el usuario desplegó y verificó con él). No hace falta redesplegar
   salvo que entre algo nuevo — y si entra, **compilar el árbol completo antes**, no solo lo
   propio: es lo que atajó dos veces código de otros equipos sin compilar.
2. **Marcar TODOS los pagos y aprobar UNA sola vez** (novedad 4). Aprobarlos de a uno genera una
   autorización por jubilado.
3. ⛔ **Tener el respaldo listo ANTES de correr, por H46.** Si la corrida falla a mitad de camino,
   **no se puede reintentar**: el ancla queda envenenada y el reintento diría «al día» sin pagar a
   nadie, **sin lanzar ningún error**. La única salida hoy es restaurar. Este mes se pudo hacer
   tres veces; el lunes también tiene que poder.
4. **Cuadrar el reporte contra los mayores** con `sql/203` antes de firmar.

| # | Qué | Dueño | Gravedad |
|---|---|---|---|
| **H41** | ⛔ **Al confirmar el pago solo se generó el asiento del SEGURO, no el de cada jubilado.** Los pagos quedarían sin respaldo contable. **Causa medida (ver abajo): la orden del jubilado va sin `desglose`** | **nuestro** | **máxima** |
| **H42** | El cruce toma **fecha de hoy** (`corte: 2026-09-04`) en vez del **fin de mes**, y marca cuotas de agosto como en mora. **Viola D1/§6bis**, que ya estaba cerrada: la cartera va con fin de mes, solo el pago con fecha actual | nuestro | alta |
| **H43** | ⛔ **CONFIRMADO CON CAPTURAS DE PRODUCCIÓN, y no estaba donde se buscaba: el defecto está en el DEVENGO, no en el asiento del pago.** Devengo y cruce **debitan la misma cuenta** `2.1.02.25.01` → saldo duplicado. Ver abajo | **nuestro** | **máxima** |
| ~~**H44**~~ | ~~Se generó **una autorización por jubilado**, no una sola por el total~~ · ✅ **NO ES DEFECTO — cerrado 2026-09-05.** Es paso de operación, ver abajo | — | — |
| **H45** | Falta el **asiento de cierre de las cuentas de apertura** del mes. ✅ **Contestado por `lap-saa-1` (ver abajo): sí es el mismo mecanismo, y es un defecto real y medido de nuestro camino.** ✅ **Autorizado por el usuario el 2026-09-05: lo toma `lap-saa-1`** | `lap-saa-1` | alta (se ve al cerrar el mes) |
| **H46** | ⛔⛔ **NO reportado por el usuario — hallado por el agente BE.** Tras revertir, reintentar la corrida diría **«al día» y no pagaría a nadie**, sin lanzar error: el negativo de `CRD.APRT` sobrevive al reverso y envenena el ancla. **Bloquea el camino de «revertir y regenerar»** | **nuestro** | **máxima** |
| ~~**H47**~~ | ~~La mora nunca se recalcula a la fecha del pago~~ · ⛔ **NO ES DEFECTO — la mora está congelada A PROPÓSITO.** Ver abajo: **no lo "arreglen"** | — | — |

#### H41 — confirmado y medido por las dos puntas (2026-09-05)

Se planteó como hipótesis que la única diferencia entre la orden del seguro (que sí generó
asiento) y las de los jubilados (que no) era el **`desglose`**. Quedó confirmada, y **ninguna de
las dos puntas es memoria: las dos son lectura de código.**

**Lo que midió `omen-saa-2` (tesorería) de su lado:**
`contabilizarSegunOrigen` (`PagoProgramadoServiceImpl:2179-2185`) documenta en su propio javadoc
que **devuelve null si el pago de origen externo no trae desglose**. Es deliberado: el diseño
asume que el módulo de origen se contabiliza solo — la misma decisión D1 que se tomó con la
nómina de RRHH. **Su null no es el defecto.**

**Lo que medimos de nuestro lado, que es lo que decide:**

- `PagoPensionComplementariaServiceImpl` invoca `asientoContableService.generarAsiento` en **un
  solo punto**: `generarAsientoDevengoPension` (:1869, llamado en :1408). Es el asiento de
  **devengo**, uno por jubilado, emitido en la corrida. **No existe ningún asiento de pago en
  todo el archivo.**
- La orden del jubilado se pide en **:1436** con desglose en `null`, y el comentario que dejó
  nuestro propio equipo ahí lo dice literal:
  `null, // sin desglose contable — mismo estado que la devolución hoy (§6.5.b)`
- La orden del seguro arma su `List<LineaContablePago>` en **:1690-1691** y la manda en **:1697**.

⇒ **La respuesta a la pregunta decisiva de tesorería —«¿`crd` genera su propio asiento del pago,
por su lado?»— es NO.** Hay agujero, y es nuestro. El devengo abre la cuenta de pensiones por
pagar y **nada la cierra**: el pasivo queda abierto y el banco sin acreditar, sobre la corrida de
~$113.000 que ya salió.

**Salida elegida, de las dos excluyentes que planteó tesorería:** mandar `desglose` en la orden
del jubilado. **No** contabilizar de nuestro lado al estilo RRHH. Si se hacen las dos, **el
asiento sale duplicado**. Cuatro razones, la última aportada por tesorería: reusa el camino ya
probado en producción este mes con el seguro; emite el asiento en el momento real de la
confirmación del pago, que es la fecha contable correcta; evita que `crd` tenga dos caminos
contables distintos para dos órdenes del mismo proceso; y **emitir el asiento en la confirmación
es lo que hace el resto del sistema** — RRHH es la excepción por decisión explícita de su usuario,
no el patrón.

**⚠️ Qué valor lleva la línea de desglose — dato de `omen-saa-1-be`, y evita reintroducir H43:**
el valor que hoy viaja en la orden del jubilado (`generarOrdenPagoPension`) es **`remanente`**
(`remanenteMes = ollaTrasSeguro`, ya neto de cruce y de seguro desde el cambio de «olla
compartida»). O sea que **la orden ya sale por el valor transferido, no por el total**. Pero eso
es distinto de `pago.getValorPension()`, que es el **nominal mensual completo** y difiere cuando
hay cruce parcial. **La línea de desglose tiene que llevar `remanente`, el mismo valor que ya
viaja como `valor` de la orden — nunca un campo del PGPC.** Armarla con `getValorPension()`
reintroduce H43 exacto.

##### (a) Cuántos asientos: uno por pago, siempre — respuesta medida de tesorería

Listaron los once llamadores de `contabilizarSegunOrigen` (:333, 383, 545, 588, 741, 784, 1028,
1281, 1311, 1666, 1771). **Todos reciben UN `pago`**; los de 1281/1311/1666/1771 están dentro de
loops por pago. **No existe ninguna firma que tome un lote ni una lista.**

⇒ **Nuestra corrida generaría 181 asientos de pago, uno por jubilado.**

Y hay un precedente que contesta si eso es lo esperado, mejor que una opinión: cuando se aprueba
con `agruparEnUnCheque=true`, **un solo cheque respalda N pagos y el asiento se sigue emitiendo
por pago**; lo único que se agrupa es el `MovimientoBanco`, que sale una vez por el total
(javadoc :2163-2168 y `docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md` §5.3.3).
**El diseño ya enfrentó esta misma disyuntiva y eligió el asiento por pago aun con instrumento
único.** No es descuido: es la decisión tomada. Pedir el agrupado sería un frente nuevo que la
contradice.

##### (b) Lo ya pagado NO se puede reprocesar — y la razón es dura

`contabilizarSegunOrigen` arma el asiento **a partir de las filas de `PGS.DPGT`** (javadoc
:2394-2406). **Nuestros 181 pagos no tienen filas en DPGT**, porque mandamos `desglose = null`.

⛔ **Volver a disparar la contabilización no sirve: devolvería `null` otra vez, por la misma
razón que la primera. No es que se haya perdido el resultado — el dato de entrada no existe.**

Los tres caminos reales, según tesorería:

1. **Backfill de `PGS.DPGT` + contabilizar.** Hoy **no existe endpoint que contabilice un pago ya
   confirmado**: habría que construirlo, y decidir con qué fecha contable salen los asientos.
   **Necesita código de `tsr`, y eso lo autoriza el usuario de ellos, no el árbitro.**
2. **Asiento manual de regularización**, hecho por contabilidad. Cero código. Puede ser **uno solo
   por el total**, no 181 — el detalle por jubilado ya está en el devengo.
3. **Revertir y volver a confirmar.** ⛔ **Desaconsejado por tesorería:** la plata ya salió al
   banco; revertir en el sistema un pago ya ejecutado ensucia la conciliación bancaria y obliga a
   deshacer el `MovimientoBanco` ya emitido.

**Lectura de tesorería (lectura, no indicación):** la **2** para lo ya pagado, y el **desglose**
para las corridas de acá en adelante. **Son dos decisiones separadas: arreglar el futuro no
arregla el pasado, y no hace falta que la misma solución cubra las dos.** ⇒ **Decisión del
usuario.**

**La devolución de aportes arrastra el mismo agujero, y también está medido:** el único asiento de
`DevolucionAporteServiceImpl` es `generarAsientoReclasificacion` (:1062, llamado en :591) —
reclasificación, **no pago**. El comentario del §6.5.b citado arriba lo dice. Anotado como
pendiente aparte de los dos lados; **no se toca en esta urgencia de jubilados**.

#### H43 — el devengo duplica lo que el cruce ya debita (confirmado en producción, 2026-09-05)

**Se planteó como hipótesis que no era defecto** —que el usuario había visto el asiento de
devengo, que por diseño va por el total, y lo había leído como asiento de pago—. **La hipótesis
era razonable y estaba equivocada.** El usuario mandó las capturas de los dos asientos reales,
del mismo jubilado (GARCIA SALTOS MILTON RENEE, PGPC 4, préstamo 7747, corrida 8/2026):

`CRE-2026-08-0168` — devengo:

```
D  2.1.02.25.01  CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS   589,17
H  2.3.01.10.03  PENSIONES COMPLEMENTARIAS POR PAGAR                    589,17
```

`CRE-2026-08-0167` — cruce:

```
D  2.1.02.25.01  CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS   481,78
H  1.3.04.05     DE 1 A 30 DIAS (banda 1)                               454,16
H  1.4.02.05     INTERESES POR PRÉSTAMOS QUIROGRAFARIOS                  24,38
H  1.4.90.90.10  CUENTA POR COBRAR SEGURO DE DESGRAVAMEN                  3,24
```

**Las dos debitan `2.1.02.25.01`.** La cuenta individual del jubilado queda debitada
589,17 + 481,78 = **1.070,95** sobre una pensión de 589,17. Los 481,78 del cruce **ya habían
salido de esa cuenta dentro del devengo**, y el cruce los vuelve a sacar.

**Por qué se escapó, y conviene no perderlo.** El comentario de `generarAsientoDevengoPension`
dice que va por el total *«independiente de cuánto se haya cruzado — ese tramo lo contabiliza
aparte `contabilizarPagoConAportes`»*. **Es cierto: el cruce sí se contabiliza aparte.** Lo que el
comentario no vio es que ese asiento aparte **debita la misma cuenta**. Los dos asientos son
correctos por separado y se pisan juntos. ⭐ **El defecto no está en ninguna de las dos piezas:
está en que nadie las miró al mismo tiempo.** Es el mismo modo de falla que el `SELECT *` de los
`.jrxml` — cada parte defendible, el conjunto roto.

**La corrección, indicada por el usuario y verificada contra los números:** la línea de pensión
del devengo (aux1=1/2 de la plantilla 35) debe ir por **`remanente`**, no por el total. En este
caso 589,17 − 481,78 = **107,39**. Cuadra por los dos lados:

- `2.1.02.25.01` recibe 107,39 (devengo) + 481,78 (cruce) = **589,17**, la pensión exacta.
- `2.3.01.10.03` queda en **107,39** — lo único realmente exigible en dinero. Lo cruzado no es
  pasivo: se fue al préstamo.

⭐ **H41 y H43 SÍ eran el mismo defecto, y el mismo valor resuelve los dos:** `remanente` en la
línea de pensión del devengo, y `remanente` en el desglose de la orden. Y encaja con la plantilla
35: el pago debitará `2.3.01.10.03` por 107,39 contra banco, cerrando exactamente lo que el
devengo dejó abierto. **Las tres piezas cierran con el mismo número** — la confirmación más fuerte
de que el diseño es correcto.

⚠️ **Al implementar, no tocar la línea de seguro** (aux1=3/4, contrapartida `2.3.90.90.06`): esa
está bien. Verificar que `remanente = pensión − cruce − seguro` y que **remanente + cruce + seguro
= pensión completa**, sin sobrar ni faltar un centavo. Si no cierra exacto, hay un cuarto destino
que no estamos viendo.

**Consecuencia sobre el pasado:** hay contabilidad **mal emitida**, no solo faltante — 181
devengos con la cuenta individual sobredebitada. Eso descarta el asiento manual de regularización
como salida (regularizar lo que falta no arregla lo que sobra) y refuerza el reverso.
**Pregunta abierta y decisiva: ¿el contra-movimiento de `sincronizarPagos` reversa el devengo, o
solo el cruce?** Si solo el cruce, el devengo inflado sobrevive al reverso.

⛔ **Y `omen-saa-2` acotó esa pregunta a una sola posibilidad de su lado: cero.**
`revertirPagoConfirmado` anula **el asiento que cuelga del pago** (`PGTRASNT`) y su
`MovimientoBanco`, nada más. El devengo lo emitió `crd` y **no cuelga de ningún pago de CXP**:
ellos no saben que existe y no tienen forma de alcanzarlo. Textual:

> «Revertir los 181 pagos es, contablemente, **una operación nula**. Lo único que hace es liberar
> el estado para que puedan regenerar. El descuadre de 1.070,95 sobre 589,17 lo tienen que
> limpiar ustedes, sí o sí, por su lado. El reverso les habilita el camino; no les arregla el
> saldo.»

⇒ **El reverso NO limpia el devengo inflado.** Los 181 pagos no tienen asiento, así que su reverso
no mueve una línea contable. **El único de los 182 donde el reverso hace trabajo contable real es
el del seguro.** Limpiar los 181 devengos es trabajo nuestro, pase lo que pase con el reverso.

##### La lección transversal, que vale más que el defecto

> **Los dos asientos son correctos por separado y se pisan juntos: nadie los había mirado al mismo
> tiempo. No falló ninguna revisión — falló que las dos revisiones fueron por separado.**

`omen-saa-2` reportó que venían encontrando la misma familia de fallas todo el día desde su lado,
con esta formulación complementaria: *«cuando un valor puede llegar por dos caminos, o los
comparás o derivás uno del otro; lo que no funciona es el punto medio»*. Es además el mismo modo
de falla que el `SELECT *` de los `.jrxml` documentado en `CLAUDE.md`: cada parte defendible, el
conjunto roto.

#### H42 — causa raíz real: la mora nunca se recalcula a la fecha del pago

La primera pista (`regularizarPrestamoSiSinMora` con `LocalDate.now()` clavado) **era un defecto
real pero distinto**: regulariza el *estado* del préstamo, no el monto contabilizado. El que causó
lo que vio el usuario es otro.

`pagarConAportes` → `aplicarPago` → `aplicarPagoACuota` → `calcularSaldosRealesCuota`
(`MotorPagoPrestamoServiceImpl`) **nunca recalcula la mora a la fecha del cruce**: solo lee
`DetallePrestamo.getMora()`, un campo **ya persistido** por el proceso de mora con **su** fecha de
corte. El mecanismo correcto existe —`recalcularMoraALaFecha` → `calcularMoraCuota(cuota,
tasaDiaria, fecha)`, que recibe la fecha y no usa `now()`— pero **solo lo llaman la precancelación
y el acuerdo de condonación**.

**Agravante:** el timer automático de mora está **desactivado desde el 2026-08-31** a pedido del
usuario. Cualquier corrida manual sin `?fecha=` dejó `DTPRMRAA` calculado a fecha de septiembre, y
de ahí quedó congelado.

⚠️ **Blast radius mucho mayor que jubilados:** `aplicarPagoACuota`/`calcularSaldosRealesCuota` los
usa **cualquier pago normal de cuota**. Es decir que **cualquier pago de préstamo hecho con fecha
distinta a la del último cálculo de mora está cobrando la mora equivocada, hoy, en producción**.
No es exclusivo del proceso de jubilados. **Avisado a `lap-saa-1` el 2026-09-05** por estar fuera
de nuestro alcance asignado y tocar un archivo que ellos vienen mirando.

### ⛔⛔ H46 — el ancla se envenena: reintentar la corrida diría «al día» y no pagaría a nadie

**Hallado por `omen-saa-1-be` el 2026-09-05 sin que se lo preguntaran, verificado por el árbitro
antes de llevarlo al usuario.** Es el hallazgo más peligroso de toda la corrida, y no salió de
ninguna de las cinco novedades: salió de preguntarse qué pasaría *después* de revertir.

**El mecanismo.** `resolverAnclaRetroactivo:1725` resuelve el ancla en dos pasos: primero
`selectFechaUltimoMovimientoNegativo`, y **solo si eso da null** cae a
`selectFechaMovimientoJubilacion`. Y la primera query es (`AporteDaoServiceImpl:1062-1074`):

```java
select max(a.fechaTransaccion) from Aporte a
where  a.entidad.codigo = :idEntidad
and    a.tipoAporte.codigo = :idTipoAporte
and    a.valor < 0
```

**Sin filtro de estado y sin excluir movimientos ya reversados.** Y `CRD.APRT` es **append-only
por diseño**: `generarContraMovimiento` nunca borra ni edita el negativo original — solo **agrega
un positivo al lado**.

**La consecuencia.** Si se revierte y se vuelve a correr `generarPagosDelMes` para agosto 2026,
cada jubilado sigue teniendo su negativo del 31/08 como el más reciente:

```
ancla = 31/08/2026  →  desde = septiembre  →  desde.isAfter(corrida = agosto)  →  "AL_DIA"
```

Sale por esa rama **sin entrar al bucle y sin tocar un solo PGPC**, para los 181.

⭐ **Y acá está lo peor, que es el modo de falla y no el defecto:** no lanza error. **Informa
«al día» y no hace nada.** Una corrida que explota se ve; una que dice que todo está bien se firma
y se archiva. Para un proceso que mueve $113.000 es el peor desenlace posible — peor que el choque
contra el `UNIQUE`, que al menos hace ruido.

**Lo que el reverso a nivel aplicación NO arregla** (verificado por el BE, con el código citado):

| Qué | Estado tras `revertirConfirmado` + `sincronizarPagos` |
|---|---|
| Movimiento negativo de `CRD.APRT` | **Queda** — solo se agrega un positivo. Envenena el ancla |
| Cuotas del préstamo (`CRD.DTPR`/`PGPR`) | **Quedan PAGADAS.** `generarContraMovimiento:2231` nunca las toca ni llama al motor de pago |
| `numeroAsientoDevengo` | **Queda apuntando al mismo asiento**, que con H43 sin corregir está **inflado** |
| Asiento de pago de los 181 | No existe — nada que reversar (`omen-saa-2`: *«contablemente una operación nula»*) |
| Asiento + movimiento bancario del seguro | ✅ Sí se anulan, es el único con trabajo contable real |

⇒ **«Revertir y regenerar» a nivel aplicación no llega a buen puerto** sin tres arreglos previos
e independientes: corregir H41+H43; reversar de verdad el cruce contra préstamo; y desenvenenar el
ancla — este último tocando **la misma función que usa cualquier corrida futura**, no un rincón.

**Las dos salidas, y ninguna es gratis:**

| | A favor | En contra |
|---|---|---|
| **Limpieza a nivel de base** | Rápida, resuelve las tres cosas de una | **Irreversible**, sin rastro, y exige garantizar que nada más tocó esas filas desde la corrida — cosa que ni el agente ni el árbitro pueden saber |
| **Anulación por aplicación** | Auditable, deja rastro, reusa lo que existe | Más código, y **falta medir si alcanza** |

**⭐ La pista que mantiene vivo el camino de aplicación, y sale de las capturas del usuario.** El BE
descartó `ProcesoPagoPrestamoServiceImpl.anularOperacion(idEvento)` —que **sí** revierte cuotas,
`PagoPrestamo` y aportes— porque `PagoPensionComplementaria` no guarda el `idEvento`. Cierto que
no lo guarda. Pero la observación del asiento del cruce dice, textual:

> `... préstamo 7747 - **evento 396**: Pago retroactivo pensión complementaria 8/2026 ...`

**El número está escrito, y a propósito.** Si se puede llegar de `(préstamo, período)` al
`idEvento` con una consulta —**no parseando la descripción del asiento, eso no cuenta**— y si
`anularOperacion` además neutraliza el negativo de `APRT`, **el camino de aplicación revive entero
y resuelve el envenenamiento de paso**. En investigación al 2026-09-05.

**Criterio del árbitro, por si se pierde el contexto:** no se descarta el camino auditable por el
irreversible sin haberlo medido. Si al final es el rollback, que sea **porque medimos que el otro
no alcanza**, no porque no lo miramos.

##### Resultado de la medición: el camino auditable NO alcanza. Las tres respuestas

Se midió, y la conclusión es que **`anularOperacion` tampoco resuelve el ancla**. Se deja el
detalle completo porque es la referencia para cuando se retome H46 — la investigación ya está
pagada y no hay que rehacerla.

**(1) `(idPrestamo, fecha) → idEvento` se puede resolver sin parsear texto, pero NO es unívoco.**
`CRD.EVPR` tiene FK real a `Prestamo` (`PRSTCDGO`), `EVPRFCHA` con la fecha que se le pasó, y
`EVPRTPOO` con el tipo. O sea que
`WHERE PRSTCDGO=:p AND EVPRTPOO='PAGO_APORTES' AND TRUNC(EVPRFCHA)=:f AND EVPRESTD=1` es viable.
**Pero el bucle retroactivo multi-mes lo rompe:** en
`PagoPensionComplementariaServiceImpl:1122-1132`, `fecha` es **fija** para toda la corrida (D1: la
fecha del pago al préstamo es la de la corrida, no la del mes M) mientras `mesM/anioM` varía en
cada vuelta. Un jubilado con varios meses de mora contra el mismo préstamo genera **varios
`EventoPrestamo` con el mismo préstamo, mismo tipo y misma fecha** — solo distinguibles por el
texto libre de `EVPROBSR`. ⇒ la resolución estructurada sirve solo cuando hubo **un único mes**
en esa corrida; no se puede asumir en general.

**(2) `anularOperacion` (`ProcesoPagoPrestamoServiceImpl:1201-1417`) hace bien dos de tres cosas.**

- ✅ `PagoPrestamo`: soft delete real (`:1261-1266`, `setAnulado(1L)` + huella).
- ✅ **Cuotas**: vuelven a su estado real vía `recalcularCuotaDesdePagos`
  (`MotorPagoPrestamoServiceImpl:678-762`) — suma los `PagoPrestamo` vigentes restantes y
  reconstruye PAGADA/PARCIAL/PENDIENTE correctamente. **Esto sí resuelve lo que
  `generarContraMovimiento` no hacía.**
- ⛔ **`CRD.APRT`: NO.** `revertirAportes` (`:1419-1470`) lee el `Aporte` negativo **solo para
  clonar sus datos** y guarda uno nuevo positivo con `tipoMovimiento = REVERSO`. **No hay ningún
  `original.set...()` ni `save(original, ...)` en todo el método.** El negativo queda con el mismo
  valor, mismo estado, mismo `tipoMovimiento = PAGO_PRESTAMO(4)` y misma `fechaTransaccion` —
  **indistinguible de un negativo vigente**. Lo único que cambia de estado es
  `PagoAporte.estado = 0` en la tabla puente `CRD.PGAP`, no el `Aporte` en sí.

⇒ **Confirmado: revertir con `anularOperacion` no limpia el ancla, exactamente igual que
`generarContraMovimiento`.** El camino auditable llega más lejos (recupera las cuotas) pero
**muere en el mismo lugar**.

**(3) No existe columna para excluir un negativo reversado; una solución genérica pide DDL.**
`Aporte` completo: `codigo, filial, entidad, contrato, tipoAporte, cargaArchivo, devolucion,
fechaTransaccion, glosa, valor, valorPagado, saldo, idAsoprep, fechaRegistro, usuarioRegistro,
estado, periodoDevengo, tipoMovimiento`. **Ninguna dice «esto fue reversado»** — ni flag ni
`idAporteReverso` (el patrón que se parecería a `devolucion`, si existiera).

Hay un camino **parcial**: como `revertirAportes` marca `PagoAporte.estado = 0`, se podría filtrar
con `NOT EXISTS (SELECT 1 FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO AND p.PGAPIDST = 0)`.
**Pero cubre solo la vía `PAGO_PRESTAMO`** —los únicos negativos con fila en `PGAP`— y de todas
formas **exige cambiar la query del ancla, no es un arreglo de datos**. Para cualquier tipo de
negativo hace falta **una columna nueva: DDL**.

**Ownership:** `EventoPrestamo.java`, `ProcesoPagoPrestamoServiceImpl.java` y `Aporte.java` no
tienen marcador de equipo en ningún commit y su autor único es `xeonpotato` — código
pre-convención, **no es de `lap-saa-1` ni de `eq2`**.

**Qué queda, entonces.** Para esta corrida no importa: el usuario restauró la base y eso limpia
todo de una. **Para un mes normal de producción sí importa y no hay salida barata:** el arreglo
mínimo es cambiar la query del ancla (parcial, solo vía préstamo) y el completo pide DDL. Es
trabajo real, no un parche — y hasta que exista, **una corrida que falle a mitad de camino no se
puede reintentar sin restaurar la base.** Y en cualquiera de los dos casos **los 181 devengos
inflados se reversan con asiento** — borrar filas de `CRD`/`PGS` no toca los libros de `CNT`.

**Preguntas abiertas al usuario, y la primera decide la viabilidad del rollback:**
(1) ¿alguien más tocó esos jubilados, sus préstamos o sus aportes desde la corrida?
(2) ¿hay respaldo de la base previo a la corrida?

### ⛔ H47 — la mora congelada NO es un defecto. NO LA "ARREGLEN"

> ✅ **LEVANTADA el 2026-09-09 por orden directa del usuario**, no por el aviso de `lap-saa-1`
> que esta sección esperaba. El timer volvió a las 02:00. Lo de abajo queda como registro de
> **por qué estuvo apagada**, no como estado vigente. Ver la entrada del 2026-09-09 al final
> de este documento.

**Se reportó como defecto a `lap-saa-1` el 2026-09-05 y su usuario lo devolvió.** Textual:

> *«dejemos la mora como está. Está desactivada de forma consciente y deliberada porque se sigue
> actualizando pagos de agosto con fecha de fin de agosto y no queremos que se genere mora por las
> cuotas todavía»*.

**El análisis técnico era correcto:** `aplicarPagoACuota` sí lee `DetallePrestamo.getMora()`
persistido en vez de llamar a `recalcularMoraALaFecha`, y ése es el mecanismo que congela el
valor. Lo que estaba equivocado era la conclusión de que había que arreglarlo. El contexto que
faltaba: **están cuadrando agosto todavía**, y recalcular a la fecha del pago haría aparecer mora
sobre cuotas que están cerrando con fecha de fin de agosto a propósito.

**Se reactiva cuando el usuario de `lap-saa-1` avise. No hay fecha** — depende del cuadre. Ellos
notifican.

⭐ **Por qué queda escrito acá y no solo en el tablero de ellos: desde afuera se ve idéntico a un
defecto.** Sin este registro, el próximo que lo mire —de cualquiera de los dos equipos— lo
"corrige" y empieza a cobrar mora sobre agosto. Es el mismo patrón que el `PAGO_APORTES` sin rama:
**una ausencia deliberada es indistinguible de un olvido si nadie la escribe.** Van dos casos en
dos días; parece una categoría propia y no una coincidencia.

#### ⚠️ Pero esto NO cierra la novedad 1 del usuario — queda una pregunta abierta

Que la mora esté congelada a propósito explica por qué **no se debe recalcular**. No explica lo
que el usuario reportó. Mirando el asiento del cruce (`CRE-2026-08-0167`) **no hay ninguna línea
de mora**: solo banda 1 (454,16), interés ordinario (24,38) y seguro de desgravamen (3,24).

⇒ **Puede que la queja no fuera por un monto de mora cobrado, sino por la BANDA.** La cuota cayó
en `1.3.04.05 — DE 1 A 30 DIAS`, que es la banda de vencido de 1 a 30 días. Si el cruce se hubiera
hecho al 31/08 en vez de al 04/09, esa misma cuota podría clasificar en otra banda —o como
vigente—. El usuario lo dijo así: *«tomó la fecha de cruce de septiembre y tomó como que la cuota
estaba en mora, cuando en realidad el cruce debió hacerse como que se hubiera cruzado el fin de
mes»*. **«Como que estaba en mora» puede ser la banda, no un cargo.**

**Pendiente de verificar:** de dónde sale la banda del asiento del cruce, y si depende de la fecha
de cruce o de la de vencimiento. Si depende de la fecha de cruce, la novedad 1 **sigue viva** y es
independiente de la mora congelada — y se arregla pasando fin de mes, sin tocar el cálculo de mora
que el usuario de `lap-saa-1` quiere quieto. **Las dos cosas no se contradicen.**

#### H44 — cerrado: no es defecto, es un paso de operación

`omen-saa-2` lo verificó: `aprobar(List<Long> idsPagos, ...)` (`PagoProgramadoServiceImpl:1133-1380`,
`POST /pgtr/aprobar`) **aprueba N pagos en un solo acto**, y `generarLote` (:1487, crea el
`LotePago` en :1537) es una operación aparte. La pantalla de aprobación soporta **selección
múltiple** (columna `sel`).

> «Si salió una autorización por jubilado, es porque se aprobaron de a uno.»

**No hay nada que corregir en el código.** El operador debe marcar **todos** los pagos del período
y darle aprobar **una sola vez**. Queda como instrucción de operación.

#### H45 — contestado por `lap-saa-1`: sí es el mismo mecanismo, y es un defecto real nuestro

El usuario de `lap-saa-1` les pidió explicárnoslo porque nuestro cruce tiene que aplicar el mismo
concepto. Lo que informan, verificado por ellos en código:

**El concepto.** A principio de mes la apertura de cartera (`armaApertura`, sub-proceso ③) manda a
contabilidad todo lo que se espera cobrar: `D 1.4.05.10 (préstamos por cobrar) → H 2.3.02.10
(préstamos por aplicar)`. **`2.3.02.10` queda abierta con lo esperado, y cada cobro la va
cerrando**; a fin de mes `armaNeteo` reversa lo que quedó sin cobrar. En el circuito normal ese
cierre lo hace `generarAsientoDefinitivo` con `lineasAplicacionPorAplicar`.

**🔴 El defecto.** Verificaron que **`contabilizarPagoConAportes` NO toca las cuentas de apertura**
— cero menciones de `lineasAplicacionPorAplicar` ni de `PRESTAMOS_POR_APLICAR`. Solo arma
`D cuentas de aporte consumido → H bandas`. **Y nuestra corrida entra justo por ahí:**
`PagoPensionComplementariaServiceImpl:1137` llama a `procesoPagoPrestamoService.pagarConAportes(...)`
**directo**, sin pasar por `procesarCobro`, y sin setear `idCobroCredito`.

**Consecuencia con nuestros números:** las cuotas cruzadas **sí se cobraron**, pero `2.3.02.10` no
se entera. A fin de mes `armaNeteo` reversará como «no cobrado» un universo que ya no las incluye
—`selectCobrable...` las ve pagadas— y **la cuenta de apertura queda descuadrada por el total
cruzado (~$16.231,60)**. No rompe la corrida ni afecta a los jubilados: **aparece al cerrar el
mes**, lejos de la operación que lo causó.

**La regla a aplicar:** del cruce, la parte que paga **cuotas vencidas y la cuota del mes SÍ debe
cerrar `2.3.02.10`**. Si alguna vez alcanzara **capital de cuotas posteriores al corte**, ese
tramo **no toca la apertura** — ni la abre ni la cierra, porque nunca se abrió. El corte que
separa las dos cosas es **`DTPRFCVN <= fechaCorteApertura`** de la corrida de cierre viva. Todo
escrito en `docs/logica-negocio/crd/DISENO-CIERRE-APERTURA-SOLO-LO-ABIERTO.md` (§2 cuentas reales
de producción, §3 circuito completo).

**Molde de referencia:** en `procesarCobro` calculan el tramo futuro una sola vez
(`calcularCapitalFuturoDelCobro`) y lo pasan a los asientos 2 y 3, para que las dos restas salgan
del mismo número. Commits `d033d5f2`, `2ffaccac`, `ccd774fd` — ⚠️ **revisados pero sin compilar ni
ejecutar**: referencia de diseño, no código probado.

**Ofrecimiento de `lap-saa-1`:** cuando cierre nuestra corrida pueden tomarlo ellos —
`contabilizarPagoConAportes` está en `ContabilidadPrestamoServiceImpl`, que ya vienen tocando, y
va en la misma familia que el guard de `esTipoAporteContabilizable` que nos deben. **Decisión del
usuario.**

**Instrucción dada al agente BE:** mirar H41, H42 y H43 **juntos** antes de proponer nada. Los
tres tocan el asiento del momento del pago; si hay una causa raíz común, va un arreglo y no tres
parches.

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

### D6 — El seguro nunca fue plata del jubilado: sale a un proveedor, aparte

**Decisión del usuario, 2026-09-04, textual:** *«el seguro medico es un valor que debe bajar
también de la pensión, así como el valor abonado a préstamos, pero ese valor no debe ir incluido
en el valor a pagar al partícipe, sino debe salir como un pago aparte al TITULAR con un RUC […]
este proceso debe sacar dos pagos, uno el total a pagar de todos los jubilados y otro el total a
pagar por seguros que va a un proveedor específico»*. Y lo remató: *«por esta razón es que no es
necesario el certificado bancario para poder descontar a un jubilado su seguro»*.

**Esto supersede a D5 en su forma, no en su fondo.** D5 había resuelto el caso «sin certificado»
mandando el seguro a un traspaso interno; D6 dice que eso pasa **siempre**. Con D6 desaparece la
asimetría que había quedado —el seguro terminaba en dos destinos distintos según el certificado— y
la regla del certificado vuelve a ser una sola cosa: gobierna **la salida de la pensión al banco**,
nada más.

| Salida de la corrida | A quién | Cómo |
|---|---|---|
| Pensión | Cada jubilado con certificado | Una orden por jubilado, como hoy |
| **Seguro** | **Proveedor RUC `1768153530001`** | **UNA sola orden por el total del período** |

**Contabilidad, cerrada por el usuario:** el pago al proveedor **debita** la misma cuenta que la
corrida **acredita** — `2.3.90.90.06 SEGURO POR PAGAR JUBILADOS`, la del apartado de seguro de la
plantilla alterno 35 (`aux1` 3/4). Si todo se paga, la cuenta **neta a cero cada período**, lo que
da un control de conciliación gratis que conviene usar.

⚠️ **Instrucción de implementación que decide si eso sigue siendo cierto:** las dos puntas leen la
cuenta del **mismo lugar**. Si el lado del pago la lleva quemada y alguien cambia la plantilla, la
corrida se movería a una cuenta nueva y el pago seguiría cerrando la vieja — sin ningún error
visible, solo dos cuentas descuadradas y alguien conciliando a mano durante meses. Es la misma
forma del defecto de los `SELECT *` en los `.jrxml` que ya costó caro en este repo.

**Prioridad cuando el saldo no alcanza — decisión del árbitro, no del usuario:** préstamo → seguro
→ pensión. Préstamo y seguro son obligaciones con terceros y el usuario dijo «como pasa con los
valores de préstamos»; la pensión es lo único que puede quedar corto. **Queda marcada como mía
para que el usuario pueda revocarla sin arqueología.**

**Búsqueda del proveedor: solo por RUC** («solo validalo por RUC»), con dos condiciones de alto
que el agente tiene prohibido resolver solo — que el RUC no exista, y que devuelva más de una
fila. Lo segundo no es paranoia: el mismo día encontramos `CRD.TPDJ` con dos «CERTIFICADO
BANCARIO» y un `get(0)` sin `ORDER BY` eligiendo en silencio. Ver **P19**: identificar al
proveedor por RUC es provisional; lo que corresponde es marcarlo en la base y buscarlo por rol.

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
| **P19** | **Marcar por ROL, en la base, las dos puntas que hoy están quemadas como números.** Decisión del usuario 2026-09-04: *«solo por esta ocasión hagámoslo por RUC, deja anotado que debemos marcar posteriormente al titular como aquel que recibe el pago de los seguros de jubilados»* y, sobre el producto, *«anota también que luego debemos marcar el grupo del producto como pago de seguros como se va a hacer con el proveedor»*. Son **dos literales**: el RUC `1768153530001` (proveedor) y el `idProductoPago` **516** (producto de CXP). Cuando existan las marcas, las dos constantes se borran y la búsqueda pasa a ser por rol. ⚠️ Mientras tanto son literales que **enrutan plata a un tercero y eligen la cuenta contable**: si cambia el proveedor o el producto y nadie toca las constantes, el pago sale al equivocado o cierra la cuenta equivocada, y **no hay error que lo delate** | **decidible, pero no dejarlo dormir** |
| **P20** | **Dos fuentes de verdad para la misma cuenta.** El HABER del devengo resuelve `2.3.90.90.06` desde la plantilla (alterno 35, `aux1=4`); el DEBE del pago al proveedor la toma del **producto 516** configurado en CXP. Si alguien mueve la plantilla, el producto se queda donde está: el devengo se muda de cuenta, el pago sigue cerrando la vieja, **sin ningún error visible**. Despachado al BE un control que compare las dos y falle ruidoso; si no se puede leer la cuenta del producto desde `crd`, queda un `.sql` de verificación y el acoplamiento documentado | va con la corrida |
| **P21** | **Precancelar un préstamo SIN ninguna cuota pagada previa tira el asiento completo.** Reportado por `lap-saa-1-arb` el 2026-09-04 y **verificado por mí contra el código**: `ProcesoPagoPrestamoServiceImpl:1079` cae al fallback `anclaEsFutura`, y el `continue` de :1124 deja esa cuota en `PAGADA(4)` en vez de `CANCELADA_ANTICIPADA(7)`; entonces el cuadre de `lineasBandaCapitalFuturoPrecancelacion` (`ContabilizacionIndividualCreditoServiceImpl:316`) queda corto y lanza `IncomeException`, tirando la generación entera. Escenario real: un desembolso reciente precancelado antes del primer vencimiento. ⚠️ **Antes de corregir hay que preguntarle al usuario** si sigue siendo cierto el requisito que dice el comentario de :1120 (que el ancla quede `PAGADA`): si sigue, la corrección va del lado del cuadre; si no, del lado de `precancelar`. No se decide por deducción | no arranca hasta cerrar jubilados |
| **P22** | **La calificación de riesgo es parametrizable pero NO administrable: no hay pantalla.** El modelo está bien hecho —`CRD.ESCR` + `ConfiguracionCalificacionRiesgo`, por producto, **por empresa** y con vigencia por fecha— y `GeneracionG48ServiceImpl` la lee de ahí, no la tiene clavada. Pero **no existe NINGÚN endpoint REST** que la administre: se verificó buscando `CalificacionRiesgoService` en todo `src/main/java/com/saa/ws/` y no aparece (`TipoCalificacionCreditoRest` es otro catálogo). Se parametriza corriendo `sql/177` contra producción. Consecuencia: un usuario funcional no puede cambiar la escala, no queda historial de quién la cambió, y nada valida que los tramos no se solapen ni dejen huecos. **Contraste que lo hace evidente: las bandas SÍ tienen las dos pantallas** (`/rest/bnpr` y `/rest/cbpr`, con CRUD completo más `/vigente`, `/listado` e `/historial`). El molde para replicar es `cbpr` —mismo modelo de vigencia y de configuración por empresa—, así que es trabajo de copiar un patrón probado, no de investigar. ⚠️ **Y no confundir las dos parametrizaciones:** bandas = clasificación **contable** (→ cuenta del asiento); escala de riesgo = calificación **regulatoria** A2/A3/B1…E (→ provisión). Se parecen y son distintas a propósito; "unificarlas" rompe el reporte regulatorio | preguntado al usuario 2026-09-07, sin abrir todavía |
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

---

## Lección convergente — dónde tiene que vivir un aviso (2026-09-07)

Dos equipos, seis casos, sin comunicación entre ellos hasta el final, y la misma conclusión.

**Nuestros tres**, todos «ausencias deliberadas» — cosas que están bien porque alguien decidió
que faltaran, y que **son indistinguibles de un olvido**: la mora congelada a propósito, el
`PAGO_APORTES` sin rama, y el `+1` que no debe reponerse. Las tres corrían el riesgo de que el
próximo que las mirara las «corrigiera» y rompiera algo.

**Los tres de `omen-saa-2`**, todos `GRANT REFERENCES` comentados: el `ALTER` moría con
`ORA-01031`, **el resto del script pasaba**, y la aplicación funcionaba — lo único que faltaba era
la integridad referencial, que por definición no se ve hasta que algo la necesita. Lo
descubrieron tres días después, en producción.

> ⭐ **El comentario tiene que estar en el punto donde alguien podría equivocarse, no en un
> documento aparte.** Quien corre un script lee el script, no la nota de pre-despliegue de tres
> días antes.

**Y el medio paso más, que salió de que ellos revisaran su propio arreglo:**

> ⭐ **Un control que vive lejos del paso riesgoso tampoco protege — y tiene que ser ejecutable.**
> Su control de privilegio estaba en el bloque de diagnóstico de arriba, y en uno además estaba
> roto (`ALL_TAB_PRIVS.OWNER`, columna que no existe; es `TABLE_SCHEMA`). **Nunca dio la cara,
> porque el usuario no llegó a correr ese bloque.** Un control que no se ejecuta se parece
> muchísimo a uno que pasó.

**Nuestros DDL ya cumplían el patrón** (`DDL-COBROS-APROBACION-CONTABILIDAD.sql §0.3`): `GRANT`
comentado **a propósito** —lo tiene que correr otra sesión, promoverlo a ejecutable no lo
arreglaría, fallaría igual en el propio grant—, el porqué escrito al lado, y la verificación
ejecutable inmediatamente debajo con su «esperado: 1 fila». Su aviso encontró **cero defectos**
acá, y ellos adoptaron este patrón citándolo.

⚠️ **Lo que queda abierto y no se cierra con documentación:** que el script traiga la verificación
**no prueba que alguien la haya leído**. Pendiente empírico: confirmar contra producción que las
FK de `DDL-COBRO-PETRO-DOS-PASOS` y `DDL-COBROS-APROBACION-CONTABILIDAD` existen de verdad.
**No preguntado al usuario todavía — está en la corrida de pago.**

---

# 2026-09-07 — Cobros de crédito: el centavo, y el reverso que no existía

Jornada entera sobre `CRD.CBCR`. Empezó con un cobro que no se podía procesar y terminó
destapando dos defectos vivos en producción que nadie había visto.

## H48 — El centavo: la cascada de pagos abandona un residuo que sí tenía dónde ir

**Síntoma:** `POST /cbcr/54/procesar` → 500, *«El asiento NNNN no está cuadrado. DEBE=171,86 |
HABER=171,85 | DIFERENCIA=0,01»*. Tres intentos previos ya se habían anulado (cobros 38, 46, 49
en estado ANULADO, mismo préstamo, misma cuota, mismo centavo).

**Las dos causas, medidas contra el código, no deducidas:**

1. `MotorPagoPrestamoServiceImpl:286` → `while (valorRestante > TOLERANCIA)`, con
   `TOLERANCIA = 0.01` (`:54`). Pagada la cuota #18 con 171,85, queda `valorRestante = 0.01`, y
   `0.01 > 0.01` es **falso**. El bucle sale. **Había 43 cuotas pendientes por delante**: el
   centavo tenía perfectamente adónde irse. Lo frena la comparación, no la falta de cuotas.
2. `CobroCreditoServiceImpl:1799` → el asiento definitivo arma sus dos lados de **fuentes
   distintas**: el DEBE de `totalesAportesPrestamos(detalles)` (o sea `CRD.DCBC`, 171,86) y el
   HABER de los `PagoPrestamo` realmente grabados (171,85). El asiento asume *lo cobrado == lo
   aplicado*. **Cualquier excedente del motor descuadra el asiento por ese monto exacto.**

⚠️ **No arreglar el `>` sin mirar el blindaje de `:298`.** Si se cambia a `>=`, el bucle entra con
0,01, pero `if (detalle.getTotalAplicado() <= TOLERANCIA) break` corta **después** de haber
agregado el detalle y **antes** de restar el aplicado: el `PagoPrestamo` del centavo queda grabado
y el `resultado` igual reporta excedente 0,01. Son dos off-by-one que interactúan. No es un
cambio de cinco minutos, y `MotorPagoPrestamoServiceImpl` lo usan Petro, jubilados y los pagos
manuales.

**Lo bueno, verificado empíricamente y no por lectura:** `IncomeException` es
`@ApplicationException(rollback = true)`, así que **los reintentos no dejaron basura** — `sql/204`
bloque 2 devolvió 0 pagos grabados y `CBCRASRP`/`CBCRASN2` en NULL.

**Radio de impacto (`sql/204` bloque 3): un solo préstamo.** Los demás candidatos eran cobros ya
PROCESADOS que pagan varias cuotas — ruido del filtro, no casos.

**Entregado:** `sql/204` (diagnóstico) y `sql/205` (corrección puntual: baja el cobro 54 a 171,85,
con el valor actual en el `WHERE`, control antes y después, COMMIT y reverso comentados). El 0,01
queda vivo en la transitoria contra el asiento 8572 — **es correcto**: dinero recibido y no
aplicado. No confundirlo con el incidente del 2026-08-31, donde la transitoria acumulaba porque
el asiento 2 nunca corría.

**La causa sigue abierta.** Volverá a pasar con el próximo cobro que no calce al centavo.

## ⛔ H49 — La redirección que el propio código recomienda produce un descuadre silencioso

Es el hallazgo más caro del día, y estaba escrito como consejo en un mensaje de error.

`CobroCreditoServiceImpl:453` bloquea anular un cobro PROCESADO de tipo `PAGO_CUOTA`,
`ABONO_CAPITAL` o `PRECANCELACION` y dice *«use la anulación de la operación sobre el préstamo
(anularOperacion)»*. Por ese camino:

1. `anularOperacion` anula los `PagoPrestamo`, recalcula cuotas y revierte aportes. ✅
2. Llama a `contabilidadPrestamoService.contabilizarReverso(evento)`.
3. `ContabilidadPrestamoServiceImpl:745` ve `eventoAnulado.getNumeroAsiento() == null` y
   **retorna sin hacer nada** — CASO B, documentado a propósito en `:734-737`: el asiento es
   `CBCRASN2`, vive en `CobroCredito.asientoDefinitivo`, y **lo reversa `anularCobro`**.
4. Pero por este camino `anularCobro` nunca corre.

**Resultado: la cartera queda revertida y los tres asientos del cobro quedan vivos, con el cobro
en PROCESADO.** La contabilidad dice que se cobró y la cartera dice que no. Sin error, sin log.

⭐ **La lección, que es la de siempre en otra forma:** el comentario de `:734-737` es correcto y
está bien puesto. Lo que estaba mal era **el consejo del otro lado**, a 300 líneas de distancia,
que mandaba por un camino que ese comentario ya explicaba que no cierra el circuito. Los dos
textos son verdad por separado y juntos producen un descuadre.

## ⛔ H50 — `anularCobro` nunca borró la distribución de bandas

`procesarCobro:651` hace `eliminarDistribucion(COBRO_INDIVIDUAL, idCobro)` al empezar.
`anularCobro` **no la borra jamás** — las únicas dos referencias en todo `CobroCreditoServiceImpl`
son la `:651` y la `:1366`.

**Todo `PAGO_MULTIPLE` / `COBRO_MIXTO` / `REGISTRO_APORTE` anulado hasta hoy dejó sus filas de
`CRD.DSBN` vivas**, y la auditoría de bandas —la que cuadra los reportes contra el mayor— cuenta
plata revertida. **Avisado a `omen-saa-2-arb`** el 2026-09-07 porque le toca por `cnt`.

La limpieza del histórico **no la tomó nadie**: es un `.sql` con diagnóstico propio.

## Frente abierto — reverso de un cobro PROCESADO

**Pedido del usuario, 2026-09-07.** Contrato completo en
`crd/API-REVERSO-COBRO-CREDITO.md` (`af282d0`), espejado a `saaFE/docs/crd/` (`0db97b2`).
DDL previo en `crd/sql/206`. **Diseñado y NO implementado.**

Lo primero que hay que entender antes de tocarlo: **`anularCobro` ya existe y ya admite
PROCESADO** para `PAGO_MULTIPLE`, `COBRO_MIXTO` y `REGISTRO_APORTE`. Lo que no existe es
**reversar**, que es otra cosa:

| | Anular (existe) | Reversar (falta) |
|---|---|---|
| Qué pasó | El depósito nunca llegó | El depósito sí llegó, se aplicó mal |
| Estado final | ANULADO (5), terminal | APROBADO (2), reprocesable |
| Transitorio | Se anula y no vuelve | Se anula **y se regenera** |

**Decisiones del usuario:** vuelve a APROBADO · el transitorio se anula y se regenera (⚠️ el
asiento nuevo lleva fecha de hoy: si el reverso cae en otro mes, cambia de período contable) ·
cubre los seis tipos con evento o aporte · coordinar con el equipo A antes de despachar.

⭐ **El paso que solo aparece porque el cobro vuelve a vivir:** hay que desenganchar el detalle
(`DCBC.EVPRCDGO` a NULL). `anularCobro` no lo hace y no lo necesita —allá el cobro muere—, pero
acá, sin eso, al reprocesar `enlazarEvento:948` deja la línea apuntando a un evento **anulado**.

**`ACUERDO_CONDONACION` queda fuera a propósito** (son **siete** tipos, no seis — error mío al
plantear la decisión, corregido antes de escribir el contrato): su reverso exigiría reabrir un
acuerdo anulado, operación que no existe, y no se inventa de paso.

## Estado de la coordinación

⛔ **El árbitro del equipo A (`saabe-25`) no es alcanzable desde esta máquina.** No aparece en
`ListAgents`; commitea `crd(eqA)` en saaFE desde otro lado. `lap-saa-1` tampoco aparece.
El frente del reverso está **frenado esperando su visto bueno** sobre `CobroCreditoServiceImpl`.

⚠️ **Y `crd` está solapado en vivo, no en el histórico:** el equipo A commiteó cuatro cambios de
`crd` en saaFE el 2026-09-07 (escala de riesgo, bandas). Además aparecieron y desaparecieron
archivos modificados de `cnt` en el árbol durante la jornada, y hay cuatro de `cxp`/`tsr` vivos en
saaFE. Ninguno se tocó ni se commiteó.

⚠️ **Marcadores de commit inconsistentes el mismo día:** `eqB`, `omen1` y `usap` en saaBE, `eqA`
y `fe` en saaFE. El §2d del registro dice que el marcador es lo único que distingue equipos —
hoy, desde el log, **no se puede afirmar quién hizo el FE de P22**.

## ✅ CERRADO 2026-09-07 — el cobro 54 se proceso

El usuario corrio `sql/205` y proceso el cobro. **El centavo quedo corregido y el pago afectado.**
El caso operativo esta cerrado; **la causa (H48) sigue abierta** y va a repetirse con el proximo
cobro que no calce al centavo.

## ⛔ H51 — Un agente trabajando deja el arbol compartido sin compilar, y eso frena a los otros equipos

**2026-09-07.** `omen-saa-2-arb` me escribio diciendo que mis cuatro archivos de `crd` estaban sin
commitear y que `mvn compile` fallaba con
`cannot find symbol: reversarLineasProcesadas(CobroCredito, String, String)`. Tenia razon: era el
momento exacto en que mi agente ya habia puesto la **llamada** en `anularCobro` y todavia no habia
escrito el **metodo**. Con eso el arbol no compila **para todos**, y a el ademas lo dejaba sin poder
hacer `git pull --rebase` (*«cannot pull with rebase: You have unstaged changes»* — las unstaged
eran las mias).

**Lo que hizo bien, y conviene copiarlo:** no stasheo mi trabajo para desatascar el suyo, y lo dijo
explicitamente. Guardar trabajo ajeno a medio hacer en un stash es exactamente el intercambio que
este esquema no acepta. Pidio en vez de resolver por su cuenta.

⭐ **La leccion, que no es sobre el defecto sino sobre el esquema:** en un arbol compartido, la
ventana entre que un agente empieza a editar y que el arbitro commitea **es una ventana en la que
nadie mas puede compilar ni rebasear**. No se elimina, pero se acorta: **commitear apenas el
agente cierra, no al final de la jornada**. Cuanto mas tarda el arbitro en guardar, mas tiempo
estan frenados los otros equipos — y el arbitro ni se entera, porque el si tiene el arbol completo.

**Corolario del rebase:** al empujar, el commit `0e5f772` de `omen-saa-2` —que estaba commiteado y
sin subir en el arbol compartido— viajo con el mio y **cambio de hash a `a8d0a67`**. Avisado, porque
si su dueño busca el hash viejo no lo encuentra y podria recommitear encima. **En una rama
compartida, un rebase le cambia el hash a los commits de otro equipo.**

## ✅ 2026-09-07 — `crd/sql/206` CORRIDO EN PRODUCCION y su control LEIDO

El usuario corrio el DDL del reverso y **paso la salida del bloque 2.1**, que devolvio las cuatro
columnas con los tipos exactos que mapea la entidad:

| Columna | Tipo en la base | Mapeo en `CobroCredito` |
|---|---|---|
| `CBCRFCRV` | `TIMESTAMP(6)` | `LocalDateTime fechaReverso` |
| `CBCRMTRV` | `VARCHAR2(2000)` | `String motivoReverso`, `length = 2000` |
| `CBCRNMRV` | `NUMBER(22)` | `Long numeroReversos` |
| `CBCRUSRV` | `VARCHAR2(50)` | `String usuarioReverso`, `length = 50` |

Las cuatro nullable, como se diseño: las 5.664 filas historicas nunca se reversaron y un `NOT NULL`
habria exigido un `DEFAULT` que mentiria sobre ellas.

⭐ **Se registra que el control se LEYO, no solo que el script se corrio.** Es la distincion del
§29ter de `ESTADO-EQUIPO-OMEN-2.md` y de nuestra propia leccion convergente: *que el script traiga
la verificacion no prueba que alguien la haya mirado*. Hubo que pedirla seis veces; la proxima vez,
pedirla junto con el "corrí el script" y no despues.

**Con esto el WAR puede subir.** Sin estas cuatro columnas, Hibernate las incluye en el `SELECT` y
rompe toda lectura de `CRD.CBCR` con ORA-00904 — la pantalla de cobros entera, no solo el reverso.

---

## 2026-09-07, tarde — cuatro defectos que solo aparecieron AL EJECUTAR

Todos compilaban. Ninguno lo podia atrapar el arbitro ni los agentes. Los cuatro los encontro
el usuario abriendo la pantalla o procesando un cobro real.

| # | Que | Donde | Commit |
|---|---|---|---|
| 1 | El invariante cobrado-vs-aplicado bloqueaba acuerdos y precancelaciones **mixtos** | mio, de H48 | `c254e6f` |
| 2 | El JPQL del seguimiento aliaseaba un `join fetch` — JPA lo prohibe | `CobroCreditoDaoServiceImpl` | `a546999` |
| 3 | El buscador del seguimiento no filtraba: propiedad comun leida en un `computed` | FE `seguimiento-cobros` | `870d315` |
| 4 | Las lineas de banda del asiento de condonacion se grababan **sin la cuenta** | `AcuerdoCondonacionServiceImpl` | `31df053` + `a7b948f` |

### 1 — Mi propio control freno produccion, y del lado correcto

`DCBC.valor` guarda **solo el deposito** en PRECANCELACION y ACUERDO_CONDONACION; los aportes
viven en `CRD.DAPR`/`CRD.DAAP`, fuera del detalle. El `PagoPrestamo` lleva el total. Comparar uno
contra otro da negativo por exactamente el monto cruzado con aportes (28.405,72 + 13.657,67 =
42.063,39 en el cobro 68).

⭐ **El control fallo en el criterio, no en el mecanismo:** freno antes de generar ningun asiento y
mostro los numeros. Se equivoco frenando de mas, que es el lado correcto cuando hay contabilidad.

Y la exclusion por tipo es **estructural, no circunstancial**: ni `SolicitudPagoCuota` ni
`SolicitudAbonoCapital` tienen un solo campo por donde recibir un aporte. No hay tercer camino.

### 3 — Dos parentesis entre que ande y que no

`filtroTipo` y `filtroEstado` eran signals; `filtroTexto` una propiedad comun. Los tres leidos en
el mismo `computed`. **Un `computed` solo se recalcula por lo reactivo que leyo**, asi que el texto
cambiaba y nadie se enteraba — por eso los otros dos filtros andaban.

⚠️ **`consulta-cobros` tiene el MISMO defecto** (`:72` propiedad comun, leida en el `computed` de
`:78`), verificado por el arbitro. Su buscador nunca filtro en vivo. **No corregido**, a decision
del usuario.

### 4 — Una linea que se cayo en una copia

Dos bloques calcados que arman la linea de banda del asiento:

    CobroPetroContableServiceImpl:915   linea.setPlanCuenta(...)   ✅
    AcuerdoCondonacionServiceImpl:806   (no estaba)                ⛔

El `DetalleAsiento` quedaba sin la FK `PLNNCDGO`; la pantalla resuelve la cuenta por esa relacion
y mostraba **N/A** aunque el asiento cuadrara en totales. **No era la parametrizacion de bandas**:
la guarda de `:801` ya verifica que la banda tenga cuenta y no reventó.

Barrido del modulo: `new DetalleAsiento()` esta en 8 archivos de `crd` y con el arreglo **todos**
setean la cuenta. Era el unico caso.

`sql/209` repara lo grabado. ⛔ **El cruce va por cuenta Y EMPRESA** — `CNT.PLNN` esta scopeado por
`PJRQCDGO` igual que `CNT.ASNT`, y cruzar solo por numero engancharia la cuenta de otra empresa en
silencio. El usuario confirmo que **agosto no esta mayorizado**, asi que alcanza con correr el 209
antes de mayorizar: no hay que remayorizar.

---

## ⛔ H52 — El cuello real: nadie puede ejecutar antes que el usuario

Los cuatro defectos de arriba, mas el `ORA-00904` del `207`, tienen una sola causa comun de
proceso: **los agentes no pueden levantar WildFly ni tocar la base desde su sesion, y el proyecto
no tiene tests.** La cadena de verificacion termina en "compila y el codigo dice lo que debe decir".

**La primera ejecucion real siempre es el usuario, en produccion.**

Eso no se arregla con mas cuidado — los cuatro se revisaron linea por linea antes de commitear. Se
arregla con un ambiente donde probar, que hoy no existe.

⭐ **Lo unico que si esta en nuestras manos, y funciono:** cuando un agente entrega algo que no pudo
ejecutar, lo dice con esas palabras. El agente BE escribio *«lo que entrego es la verificacion
estatica; "deberia" y "medido" no son lo mismo»* antes del defecto 2 — y el defecto cayo
exactamente ahi. El aviso no lo evito, pero hizo que se commiteara **sabiendo el riesgo** en vez de
creerlo probado. Se mantiene esa practica y se traslada al usuario tal cual.

### ✅ 2026-09-07 — `sql/210` CORRIDO EN PRODUCCION: las cuentas quedaron reparadas

El usuario confirmo "listo, arregladas las cuentas". Las cuatro lineas de banda de los dos
asientos de condonacion (8527 / CRE-2026-09-0003 del acuerdo 1, y 9245 / CRE-2026-08-0459 del
acuerdo 7) ya tienen su `PLNNCDGO`: `1.3.12.20` -> 10552 y `1.3.12.15` -> 10551, empresa 1236,
$2.691,94 en total.

**Ninguno de los dos periodos estaba mayorizado**, asi que los valores entran al mayor en su
cuenta desde la primera mayorizacion: no hubo que remayorizar. Se corrio a tiempo por eso.

⭐ **Leccion de la jornada con los `.sql`, y es la segunda vez el mismo dia:** el `209` fallo con
`ORA-30926` porque escribi la reparacion en forma GENERICA —un `UPDATE` con un `JOIN` dentro de la
subconsulta correlacionada— para un conjunto de **cuatro filas ya medidas**. El `210` las repara
con dos `UPDATE` explicitos por id y funciono a la primera. **Cuando el conjunto ya esta medido y
es chico, el `UPDATE` explicito es mas seguro Y mas rapido de revisar.** La version generica no
compro nada y costo un viaje al usuario.

Antes, el mismo dia, el `207` habia fallado por dos nombres de columna que escribi de memoria en
vez de cotejarlos contra su `@Column`. Los dos errores son la misma familia: **deducir en vez de
medir, pero en SQL, donde el costo lo paga el usuario y no yo.**

---

## ⛔ H53 — El informe CCPM no es reproducible: filtra por el estado de HOY, no por el del corte

**2026-09-07.** El usuario pregunto si los reportes de *Reportes → Creditos → Informes mensuales*
(CPRM · CJBM · CCPM) y los G calculan los saldos correctos **a la fecha de corte**. Medido:

### Lo que SI esta bien

| Reporte | Como saca el saldo | Veredicto |
|---|---|---|
| **CPRM** | `sum(aportes) where fechaTransaccion <= fechaCorte` | ✅ reconstruye a la fecha |
| **CJBM** | mismo patron, aportes acumulados hasta el corte | ✅ reconstruye |
| **CCPM** — mora | `calcularInteresMoraBatch(cuotas, fechaFin)` | ✅ calculada al corte, no a hoy |
| **G40–G51** | reciben mes+anio y derivan con `YearMonth.lengthOfMonth()` | ✅ imposible pasarles media fecha |

Los 8 generadores G que NO usan `lengthOfMonth` (G41, G45, G47, G50, G51 y los orquestadores) son
de **padron o catalogo** —fecha de nacimiento, de ingreso, de novacion— y no filtran por fecha
porque no tienen movimientos que cortar. Verificado en G41, G45 y G47.

### ⛔ Lo que NO — `selectCuotasDelMesGlobal`

```java
where d.estado = 4                          // ← PAGADA, el estado de HOY
  and d.fechaVencimiento >= :fechaInicio
  and d.fechaVencimiento <= :fechaFin
```

**Filtra por el estado ACTUAL de la cuota, no por el que tenia al cierre.** Una cuota que vencia en
agosto y se pago recien en septiembre figura hoy como PAGADA y **entra en el informe de agosto**.

⇒ **El informe CCPM de un mes cerrado cambia segun cuando se genere.** No es reproducible, y es un
reporte que va a la Superintendencia.

⚠️ **Medido a medias, y hay que decirlo:** se verifico `selectCuotasDelMesGlobal`. La segunda
consulta de seleccion, `selectMenorCuotaAnteriorAlMesGlobal`, **no se abrio** — CCPM combina las
dos, asi que el alcance real puede ser mayor o compensarse. **No arreglar sin terminar de medir.**

### La decision que falta, y es del usuario

¿El informe de un mes debe ser **una foto congelada de ese mes** (reproducible) o **la situacion
actual de las cuotas de ese mes**? Hoy es lo segundo **sin que nadie lo haya decidido** — puede
incluso ser lo que siempre se quiso. No se toca hasta tenerlo definido: cambiar el criterio mueve
numeros ya reportados a un ente de control.

### Como medir el impacto de un mes antes de mandarlo

```sql
SELECT COUNT(*) AS CUOTAS_QUE_SE_COLARIAN, ROUND(SUM(NVL(d.DTPRCPTL,0)),2) AS CAPITAL
  FROM CRD.DTPR d
 WHERE d.DTPRESTD = 4
   AND d.DTPRFCVN BETWEEN DATE '2026-08-01' AND DATE '2026-08-31'
   AND EXISTS (SELECT 1 FROM CRD.PGPR g
                WHERE g.DTPRCDGO = d.DTPRCDGO
                  AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)
                  AND g.PGPRFCHA > DATE '2026-08-31');
```

Cero o casi cero ⇒ el defecto no muerde ese mes y el informe se puede mandar.

---

## H54 — El corte de los informes cierra en `23:59:59`, no en fin de dia

`GeneracionCPRMServiceImpl:51`, `CJBM:57`, `CCPM:51` y `G48:64` arman el corte como
`LocalDateTime.of(anio, mes, ultimoDia, 23, 59, 59)`. **Excluye el ultimo segundo del dia**: un
movimiento con marca de tiempo dentro de `23:59:59.000000001`–`23:59:59.999999999` queda fuera.

Es improbable que muerda, pero es **la misma familia** que los dos defectos que hoy costaron el
centavo del cobro 54 y el cierre de cartera: un borde escrito con "casi" en vez de exacto. Lo
correcto es `.atTime(LocalTime.MAX)`.

**Autorizado por el usuario, pendiente de despachar**: va junto con H53 porque son los mismos
archivos y no conviene tocarlos dos veces.

### 📌 2026-09-07 — Se genero y ENVIO un informe con el comportamiento actual (H53 sin corregir)

El usuario saco el informe y lo mando, sabiendo lo de H53. Queda registrado porque tiene una
consecuencia que no se ve hoy:

⚠️ **Si mas adelante se cambia el criterio de H53 —de "situacion actual" a "foto congelada del
mes"— los informes dejarian de ser comparables entre si:** el ya enviado sigue la regla vieja y los
siguientes seguirian la nueva. Al decidir el criterio hay que decidir tambien **desde cuando
aplica** y si hace falta reemitir algo.

**No es un problema hoy y no urge.** Se anota para que la decision se tome con este dato a la
vista, y no se descubra cuando alguien compare dos periodos y no cuadren.

---

## ✅ 2026-09-07 — CIERRE DE CARTERA DE AGOSTO EJECUTADO. "Salio muy bien"

Confirmado por el usuario. Cerro el frente que ocupo toda la tarde y que arranco con dos asientos
de reclasificacion descuadrados y la pregunta correcta: *«¿las bandas se generaron mal o el
algoritmo de validacion esta equivocado?»*. **Ninguna de las dos.**

Fueron dos defectos encadenados, los dos de borde:

1. **`calculaVencidosDelMes` contaba `(desde, hasta]`** cuando lo que cruza la frontera es
   `[desde, hasta)`. La distribucion clasifica con la regla del proyecto —el dia del vencimiento es
   POR VENCER— y ese calculo no. Los dos extremos corridos un dia. Confirmado al centavo:
   `209.243,30 − 60.779,06 = 148.464,24` y `60.779,06 − 45.909,37 = 14.869,69`, exactamente los dos
   descuadres.
2. **El neteo contaba como cobranza de planilla cualquier movimiento positivo de aportes**, sin
   filtrar el tipo. Migrados, ajustes y reversos tapaban un faltante REAL de 5.495,13 y el sistema
   veia un exceso falso de 686,53 y neteaba cero. Ahora cuentan solo aporte mensual y excedente
   Petro, por decision del usuario, y el neteo reversa 5.442,87.

⭐ **La leccion de metodo, y es la del dia entero:** falle DOS hipotesis antes de acertar —falta de
parametrizacion, y despues un solo borde— y las dos veces por deducir leyendo codigo. **La tercera
salio de SUMAR LAS LINEAS REALES de la previsualizacion**: ahi se vio que todo el descuadre caia en
la banda 1 de cada producto —el unico lugar con tratamiento especial— y que las bandas 2 en
adelante cuadraban en cero exacto en los cinco productos. De ahi fue aritmetica, no intuicion.

**Y la validacion que rechazaba la ejecucion hizo exactamente lo que tenia que hacer.** Sin ella se
habrian contabilizado dos asientos descuadrados.

## ✅ 2026-09-07 — El acuerdo 43 SI reversaba los aportes. Cerrado sin defecto

El usuario lo confirmo: *«fue un error mio, si se reversaban»*.

**Queda cerrado el hilo entero que abrio ese reporte, y con el mi propio error:**

- El `sql/207` daba por hecho que `AcuerdoCondonacionServiceImpl:566` dejaba la FK `PGAP.PGPRCDGO`
  en NULL. **Era falso** — `EntityDaoImpl.save()` usa `persist()` para entidades nuevas, no
  `merge()`, y `persist()` si escribe el id en el objeto que recibe. Ya estaba ANULADO en su
  encabezado; **queda confirmado que ademas no habia nada que reparar**.
- El cambio `pago = pagoPrestamoService.saveSingle(pago)` que quedo commiteado es **inofensivo pero
  inerte**: alinea el estilo con el resto del proyecto y no arregla nada.
- El `sql/208` (diagnostico) queda sin correr y sin necesidad.

⭐ **Lo caro no fue el error del usuario sino el mio:** deduje la semantica de `save()` desde una
nota del CLAUDE.md sobre el "merge desnudo" —que es real, pero para el caso de UPDATE— y la extendi
al INSERT **sin abrir el archivo**. Escribi un script de REPARACION sobre esa deduccion. Correrlo
le habria devuelto el dinero al socio por segunda vez. Lo detuvo que el usuario corriera primero el
bloque de control y devolviera cero filas.

> **Un script que escribe sobre plata no se entrega sin haber leido el codigo que se afirma roto.**

---

## ⭐ 2026-09-07 — Apretar la guarda de cuadre destapo un defecto REAL en otro modulo

`omen-saa-2` respondio al aviso y **fue a buscar y encontro**: `PagoProgramadoServiceImpl`, en el
camino de contabilizacion de pagos de **origen externo** (anticipos a empleado, caja chica, debito
automatico), arma las lineas del DEBE desde el desglose y la del HABER desde `pago.getValor()`.
**Si difieren en un centavo, la guarda vieja lo toleraba y el asiento se grababa descuadrado** —
exactamente el caso `1.487,02` contra `1.487,03` del cobro 29. Y `creaLineaAsiento` no redondea lo
que recibe, asi que un decimal sucio entraba tal cual.

Con la comparacion en centavos enteros eso pasa a **fallar**. Lo corrigen de su lado: la linea del
HABER tiene que cuadrar exacta con el desglose redondeado, y si no cuadra que reviente **al
registrar, no al contabilizar**.

> ⭐ **El cambio convirtio un defecto silencioso en uno ruidoso, y esa era exactamente la
> intencion.** No fue un falso positivo ni una molestia para otro equipo: fue encontrar algo que
> llevaba tiempo grabando asientos mal sin que nadie lo viera.

**Y valida el orden en que se hizo:** primero medir toda la base (cero descuadrados ⇒ nadie
dependia de la tolerancia), despues apretar. Sin esa medicion previa el cambio habria sido una
apuesta.

### 🔧 Practica que adoptamos de ellos

Antes de cada push: **`fetch` + `rebase` + RECOMPILAR sobre el codigo de los otros**, no solo sobre
el propio. Es barato y evita el ida y vuelta de descubrir en el push que el arbol combinado no
compila. Hoy nos cruzamos tres veces por esto.

### Sobre el `equals()` de `validaDebeHaberAsientoContable`

Los dos equipos coinciden en que es el **defecto espejo** —cero tolerancia, podria rechazar un
asiento que cuadra en centavos— y **ninguno lo toca**: no hay evidencia de que dispare ni de que
tenga llamadores activos. Si aparecen, se mira entre los dos.

### La observacion de ellos que conviene no perder

*«Los tres los encontro el usuario mirando la salida, no una revision de codigo.»* Dicho de su lado
sobre sus tres defectos del dia — tipo de cuenta invertido, naturaleza del mayor, el centavo — y
vale igual para los nuestros. **Es la misma conclusion a la que llego este tablero por separado
(H52): la primera ejecucion real siempre es el usuario.**

---

## ✅ 2026-09-08 — H55: los reportes nunca supieron convertir un parámetro de fecha

**Commit `2e866ae`.** El informe de devolución de pago (`RPRT_INFR_DVAP`) reventaba en producción:

    ClassCastException: class java.lang.String cannot be cast to class java.util.Date
        at JRJdbcQueryExecuter.setDate(JRJdbcQueryExecuter.java:886)

`ReporteServiceImpl.convertirTiposParametros` existe justamente para coercionar los parámetros
del JSON a los tipos que declara el `.jasper`. Cubría `Long`, `Integer`, `Double`, `Float`,
`BigDecimal`, `Short` y `String`. **Ninguna rama de fecha.** La plantilla declara `P_FECHA_DESDE`
como `java.sql.Date`, el JSON manda el String `"2026-01-01"`, nada lo convertía, y explotaba al
ejecutar la consulta.

Se agregaron las tres ramas (`java.util.Date`, `java.sql.Date`, `java.sql.Timestamp`) detrás de
`isTipoFecha()`, con `parsearFechaParametro()` que acepta `yyyy-MM-dd` o ISO local con hora.

**Dos decisiones que vale la pena no revertir:**

- **Si el String no parsea, se deja tal cual y falla como antes.** No se inventa un valor por
  defecto ni un "hoy". Un reporte financiero que sale con una fecha silenciosamente distinta de la
  pedida es peor que uno que no sale: nadie lo revisa, y el número mal llega al informe.
- **`java.sql.Date`/`Timestamp` usan `valueOf()`, que es literal.** Nada de desplazamiento de zona.
  Es exactamente la trampa que el `CLAUDE.md` documenta con `LocalDateTime` y Jackson, donde la
  fecha se corría un día sin ningún error.

Es **puramente aditivo**: un parámetro de fecha declarado así ya fallaba siempre, así que la rama
nueva no puede romper ningún reporte que hoy funcione.

### ⚠️ Lo que queda medido y sin tocar: `Boolean`

Inventario de los `<parameter class="...">` de **todos** los `.jrxml` de `src/main/resources/rep/`:

| Tipo | Parámetros | Estado |
|---|---:|---|
| `java.lang.String` | 176 | cubierto |
| `java.lang.Long` | 48 | cubierto |
| `java.awt.Image` | 37 | se maneja aparte, no pasa por este método |
| `java.lang.Double` | 26 | cubierto |
| `java.lang.Integer` | 20 | cubierto |
| **`java.lang.Boolean`** | **2** | **⚠️ SIN CUBRIR** |
| `java.sql.Date` | 1 | cubierto hoy |

Los dos `Boolean` son `P_RECIBIO_CESANTIA` y `P_JUB_SIN_MOVIMIENTOS`, los dos en
**`RPRT_CRTF_PTRN.jrxml`**. **No está medido si falla hoy**, y depende enteramente de qué manda el
frontend: si van como boolean JSON, Jackson los deserializa a `Boolean` y no rompe; si van como el
String `"true"`, revientan con el mismo `ClassCastException`. **No se tocó** — el usuario no lo
pidió y no hay reporte de falla. Queda anotado para cuando alguien ejecute ese certificado.

**Lección, que es la misma de siempre en este archivo:** el método tenía un javadoc que explicaba
por qué existía —"Jackson deserializa números JSON como Integer"— y ese javadoc describía
exactamente el subconjunto de casos que alguien había vivido. Los tipos cubiertos no eran un
diseño: eran una lista de incidentes pasados. **Un `grep` de treinta segundos sobre los `.jrxml`
daba el mapa completo de lo que el método tenía que soportar.** Nadie lo había corrido.

---

## ⛔ H56 — Leí la llamada y no lo que la llamada devuelve (2026-09-08)

**El usuario está cuadrando el cierre de cartera de agosto contra contabilidad y pidió un select
de pagos de préstamo sin asiento. El `sql/216` que le entregué listaba como huecos casi todos los
pagos del mes. Su respuesta: «muchos de esos pagos sí tienen asiento contable, qué pasó».**

Tenía razón. El defecto no estaba en el SQL: estaba en el mapa que construí leyendo el código.

### Lo que hice mal, con nombre y apellido

Para saber qué caminos estampan `PGPR.PGPRASNT` seguí los `setAsiento(...)` y llegué a esto:

```java
// ProcesoPagoPrestamoServiceImpl:216
Long numeroAsiento = contabilidadPrestamoService.contabilizarPagoCuota(resultado, ctx);
aplicarAsiento(evento, resultado, numeroAsiento);
```

Vi que `pagarCuota` llama a `aplicarAsiento`, anoté «pagarCuota **sí** estampa», y seguí. **Nunca
abrí `contabilizarPagoCuota`.** Si lo hubiera abierto:

```java
// ContabilidadPrestamoServiceImpl:443
public Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) {
    System.out.println("... diferido a Fase 1bis ...");
    return null;              // SIEMPRE. Y a propósito.
}
```

Devuelve `null` siempre, así que `aplicarAsiento` estampa `null`, así que **ningún pago de cuota
tiene jamás `PGPRASNT`**. Y está bien que así sea: el asiento de esa plata es `CBCRASN2`, el
definitivo **del cobro**, y encender el hook por pago la duplicaría. El comentario de doce líneas
que hay justo encima del método lo explica entero. Lo mismo con `contabilizarPagoConAportes` y
`contabilizarPrecancelacion` cuando la operación nació de un cobro (`ContextoPago.idCobroCredito`
con valor): devuelven `null` sin tocar nada.

**Seguí la cañería y no el agua.** Un `setAsiento(x)` no prueba nada si nunca miré qué es `x`.

### El eslabón que faltaba

```
CRD.PGPR.EVPRCDGO  →  CRD.DCBC.EVPRCDGO  →  DCBC.CBCRCDGO  →  CRD.CBCR.CBCRASN2
```

El enlace lo escribe `CobroCreditoServiceImpl.enlazarEvento:1150`. Ahí está la plata de casi todo
agosto, y mi select ni la miraba.

### Los cinco lugares donde vive el asiento de un pago

| # | Dónde | Qué origen |
|---|---|---|
| 1 | `PGPR.PGPRASNT` | abono a capital (asiento de **reclasificación de bandas**, no del dinero); pago con aportes / precancelación **directos**, sin cobro |
| 2 | `EVPR.EVPRNMAS` | condonaciones |
| 3 | **`CBCR.CBCRASN2`** vía `DCBC` | **todo lo que pasó por la pantalla de cobros** |
| 4 | `ANCP` subproceso 3 | Petro: asiento agregado **por carga** |
| 5 | ninguno, y está bien | `MIGRACION`: saldo histórico |

### Lo que cambié en el método, no sólo en el script

El `sql/217` abre con un **bloque 0 que muestra en cuál de los cinco lugares apareció el asiento
de cada pago**, y dice explícitamente: *«si la mayoría no cae en "3 cobro CBCRASN2", avisame antes
de seguir: querría decir que la corrección tampoco es la buena»*. Es decir, **la salida verifica mi
diagnóstico antes de que el usuario le crea al resto del script.** Eso es lo que le faltaba al 216:
entregué un mapa deducido sin ningún control que lo pudiera desmentir.

**Es la misma lección de la jornada del cierre de cartera, otra vez y en otro disfraz:** las tres
veces que fallé fue deduciendo del código, y las veces que acerté fue midiendo. Acá la medición
barata existía y no la hice — bastaba abrir el método que devuelve el valor.

**Y una segunda, aparte:** un `return null` deliberado con doce líneas de comentario explicando por
qué, es exactamente el tipo de decisión que un lector apurado lee como «todavía no implementado».
No lo era.

---

## H57 — Un reverso de asiento saca "hoy" de TRES lugares distintos (2026-09-08)

**Pedido de `omen-saa-2` (tesorería):** un pago que rebota en el banco hay que **reversarlo**, no
anularlo — si se anula, en los libros no queda con qué conciliar el movimiento que el extracto
muestra en menos y después en más. Eso ya funciona. Lo que faltaba es poder fechar el reverso el
día del rebote y no hoy.

`AsientoServiceImpl.generaCabeceraReversion:414` no deriva la fecha una vez: **la deriva tres veces
por caminos distintos**, y sólo coinciden porque las tres dicen "ahora".

```java
asientoReversion.setFechaAsiento(LocalDate.now());          // (1) la fecha
...
Calendar calendario = Calendar.getInstance();               // (2) mes y año, por otro lado
Long mes  = Long.valueOf(calendario.get(Calendar.MONTH)) + 1;
Long anio = Long.valueOf(calendario.get(Calendar.YEAR));
Periodo periodo = periodoService.recuperaByMesAnioEmpresa(..., mes, anio);   // (3) el período
```

**Por qué importa:** el agente de ellos había propuesto llamar `reversionAsiento` y después pisar
`fechaAsiento` con un merge. Su árbitro lo frenó por instinto — «poco elegante». Es peor que eso:
habría dejado el asiento **fechado en un mes y numerado y periodizado en otro, sin un solo error**.
La familia de siempre en este archivo: el sistema no falla, contesta mal.

Por eso la instrucción al agente dice explícitamente que las tres salen de la misma fecha y que el
`Calendar` se va de la ruta nueva.

### La guarda que el pedido no traía

Hoy el reverso cae siempre en el período de hoy, que en la práctica está abierto. **Eso es lo único
que protege los meses cerrados**, y es por accidente. Con fecha libre, un rebote de un mes ya
mayorizado mete un asiento en un mes cuadrado — y `verificaAnulacionReversion:382` valida
mayorizado **sólo para ANULAR, no para REVERSAR**: no hay nada que lo frene.

La sobrecarga nueva lleva dos guardas: período inexistente → `IncomeException` (hoy sería un
`setPeriodo(null)`), y período MAYORIZADO (2) o CERRADO (4) → `IncomeException` pidiendo
desmayorizar. Tesorería confirmó que su caso normal ni las toca y que el caso mayorizado **quieren**
que frene.

**Lo que NO se hizo, y es tan importante como lo que sí:** la validación de período **no** se le
agregó al `reversionAsiento(id)` de siempre. Por ahí ya pasa gente con períodos en cualquier estado;
apretarlo como efecto colateral de otro pedido rompería producción sin que nadie lo hubiera
decidido. Si algún día conviene, que sea medido y avisado.

---

## ⛔ H58 — El WAR subió sin su DDL: `CRD.CFCR`/`ESCR` no existen, y por eso no salen los Gs (2026-09-08)

**Síntoma que veía el usuario** al generar los G40–G51 de agosto:

    could not prepare statement [JI031070: Transaction cannot proceed:
    STATUS_MARKED_ROLLBACK] [update RPR.EJRD set ... where EJRDCDGO=?]

**Causa real, a dos capas de distancia:** `GeneracionG48ServiceImpl` consulta `CRD.CFCR`. Esa tabla
**no existe en la base**. Oracle responde `ORA-00942`, el contenedor marca la transacción para
rollback, y lo que explota es el `update` con que el orquestador intentaba dejar constancia del
fallo — ver [[H-los-Gs-escondian-el-error]] (commit `0140477`).

**Cómo pasó.** El otro equipo de `crd` parametrizó la calificación de riesgo del G48 el 2026-09-02:
código en `e384000`, DDL en `crd/sql/177_PARAMETRIZACION_CALIFICACION_RIESGO.sql`. El script arranca
con *«ESTE SCRIPT ESCRIBE (CREATE TABLE + INSERT). Correrlo ANTES de desplegar el WAR.»*
**El código se desplegó. El script nunca corrió.**

Es el mismo accidente que `CBCRASRP` el 2026-08-31, y el mismo que el `sql/212` de jubilados estuvo
a punto de repetir. **Tercera vez en nueve días.** No es mala suerte: es que *«correr el script antes
del WAR»* vive en un comentario dentro del script que sólo lee quien ya decidió correrlo.

**Alcance mayor que el G48:** `ConfiguracionCalificacionRiesgoRest` y `EscalaCalificacionRiesgoRest`
están expuestos y también fallan mientras las tablas no existan.

**Se arregla corriendo el `177`, sin tocar código y sin esperar el WAR.** Pero los controles A.2/A.3
del script piden ojo humano: los productos **7, 8 y 21** están cableados como hipotecarios y la carga
inicial los congela. Si algún hipotecario quedó fuera de ese literal, hoy se califica mal y el script
convertiría el error en parametrización — en un reporte **al regulador**.

### La lección, y no es sobre este script

**Mi diagnóstico previo tenía la forma equivocada.** Deduje del código que el G48 fallaba por
«productos sin configuración vigente», y armé el `sql/219` para medir eso. El código nunca llega a
esa comprobación: la consulta revienta antes. **La medición correcta la hizo el usuario en un
segundo, corriendo el select y leyendo el error de Oracle** — otra vez, medir le ganó a deducir.

Lo que sí funcionó del `219`: mandarlo a correr **antes** de tocar ningún generador. Si hubiera
"arreglado" el G48 por mi hipótesis, habría cambiado código sano para un problema que era de DDL.

---

## ⛔ H59 — `git add` por ruta NO protege el árbol compartido: `git commit` se lleva todo el índice

**El commit `ce3ce9bd`** (H54 tanda 2, cinco generadores) se llevó puestos **tres archivos de
`omen-saa-2`** que no tienen nada que ver: `CLAUDE.md`, `compilar-jasper.bat` y
`tools/jasper/src/tools/jasper/CompilarJasper.java` (87 líneas). Lo detectó su árbitro.

**Lo que corrí fue correcto:**

```
git add src/.../GeneracionG42ServiceImpl.java  ... (cinco rutas explícitas)
git commit -q -F <mensaje>
```

Nada de `-A`, ni `.`, ni `-u`, ni un directorio — exactamente lo que el `settings.json` exige.

⭐ **El agujero está en el `commit`, no en el `add`. `git commit` commitea TODO EL ÍNDICE**, no lo
que uno acaba de agregar. `omen2` tenía sus archivos staged, y mi commit se los llevó sin que yo
tocara nada de ellos. **En un árbol compartido el índice es estado compartido, igual que el working
tree.** Fechable: en mi commit anterior (`0140477`, veinte minutos antes) `tools/` figuraba como
`??` untracked.

**Lo que sí lo previene:**

```
git commit -- <las rutas>          # limita el commit a esas rutas, ignora el resto del índice
git diff --cached --name-only      # control barato ANTES: qué se va a llevar de verdad
```

**Por qué importa la corrección:** su árbitro lo atribuyó a un `git add` de directorio. Si lo
anotáramos así, el próximo haría exactamente lo que hice yo —con cuidado, por ruta— y volvería a
pasar. Va al `REGISTRO-RESERVAS-EQUIPOS.md`, que lo leen los cuatro equipos.

Historia no reescrita: los archivos ya están en `origin/main` y compilan. Lo único perdido es la
trazabilidad del mensaje.

---

## ✅ 2026-09-09 — Los Gs de agosto SALIERON. Cerrado H58

**El usuario confirmó: «Ya salieron los Gs de agosto».**

Secuencia completa del incidente, para que quede el hilo entero:

1. La pantalla mostraba `STATUS_MARKED_ROLLBACK` sobre un `update RPR.EJRD`.
2. Ese mensaje era el síntoma. La causa estaba **dos capas más abajo**: `CRD.CFCR` no existía.
3. El DDL era `crd/sql/177`, del 2026-09-02, con *«correrlo ANTES de desplegar el WAR»* en la
   primera línea. **El código se desplegó, el script no corrió.**
4. El usuario corrió el `177` y después el `219` de control.

**El control A.2 salió limpio, y era el riesgo real del script:** los productos cableados como
hipotecarios —**7, 8 y 21**— resultaron ser exactamente HIPOTECARIO, HIPOTECARIO RESTRUCTURADO e
HIPOTECARIO NOVACION. Ningún otro producto de los quince tiene naturaleza hipotecaria. **El literal
del código estaba bien y la parametrización no congeló ningún error** — que era la única forma en
que este script podía hacer daño, y en un reporte al regulador.

Bloques 1 y 2 del `219`: vacíos. Quince productos configurados, todos vigentes al 2026-08-31.

**Confirmación indirecta del diagnóstico:** el bloque 4 mostró que la última corrida exitosa era
**julio 2026** (ejecución 522), y de agosto **no quedó ninguna fila** — porque la transacción se
revirtió entera, exactamente como predecía el análisis. Y el 4b vacío: nunca quedó registrado el
motivo de ningún fallo, que es el defecto que corrigió `0140477`.

### Lo que se llevó puesto de paso

En la misma jornada, y disparado por este incidente:

- **`0140477`** — cada reporte en su propia transacción, el error visible, y el truncado defensivo
  contra `ORA-12899`. Incluye el segundo defecto que encontró el agente: el paso 7 leía copias
  viejas del persistence context y **toda corrida habría dicho «con novedades» aunque los doce
  salieran OK**.
- **`ce3ce9b`** — H54 tanda 2: cinco generadores más que cortaban el mes en `23:59:59`.
- **`c651bbb`** — §2e del registro compartido: el índice de git también es estado compartido.

**Nada de esto estaba desplegado cuando los Gs salieron.** Los Gs de agosto se destrabaron
**sólo con el `177`**, sin WAR. Vale anotarlo: el arreglo de código es para la próxima vez, no fue
lo que resolvió ésta.

---

## 📋 2026-09-09 — Revisión del frente de SEGUROS (a pedido del usuario)

El usuario pidió revisar «todo el proceso y la pantalla de seguros» y **empezar las correcciones**.
Este equipo toma el frente. Estado medido hoy, no heredado del `.md`:

### ⚠️ Hay DOS cosas llamadas «seguros» y están en estados opuestos

| | Estado |
|---|---|
| **Seguro médico de jubilados** | ✅ terminado (proceso dual, pantalla, scripts corridos). Sólo espera WAR |
| **Seguros de préstamo** (desgravamen / incendio / prendario) | ⛔ diagnosticado, **cero construido** |

La pantalla `asignacion-seguros` **no se toca desde el 2026-08-12**. Todos los commits con
«seguro» posteriores son del seguro médico de jubilados, que es otro frente.

### La corrección que el tablero de seguros NO tiene

`ESTADO-EQUIPO-SEGUROS.md` §1.5 dice que el alta normal fija desgravamen **y** incendio en cero.
**Ya no es cierto para el desgravamen**, y cambió por decisión del usuario (U1) el 2026-08-31,
un día después de escrito ese tablero:

`PrestamoServiceImpl:475-486` — el generador real ahora calcula el desgravamen **sobre el saldo**
con `1.12/1000`, la misma fórmula del simulador, *«para que la tabla que el sistema genera coincida
con la simulación que el socio firma»*. El seguro de incendio **sí** sigue en cero, y también por
decisión explícita: *«no se cobra mientras no exista la póliza que lo respalde… No es un pendiente
por implementar.»*

**Lección de método, otra vez la misma:** un tablero de equipo de hace nueve días describía el
código con precisión el día que se escribió y ya tenía un dato falso. Se verificó antes de
resumírselo al usuario y por eso la corrección salió a tiempo. [[H56]] es la versión cara de esto.

### Lo verificado hoy, que es lo que sostiene cualquier corrección

- **Prelación de cobro** (`MotorPagoPrestamoServiceImpl:367-388`, confirmada por negocio 2026-08-14):
  incendio → desgravamen → mora → interés vencido → interés ordinario → capital. **Los seguros se
  cobran antes que la mora.**
- **Asimetría del acumulado:** el desgravamen tiene `DTPRDSPG` en la cuota; el incendio **no tiene
  campo pagado** y se reconstruye desde `PGPR.valorSeguroIncendio`. Dos caminos según el tipo.
- **`PRSTVLAS` ya no está muerta:** `ContabilidadPrestamoServiceImpl:929` la lee. Como nadie la
  escribe, un prendario o hipotecario **nunca registra su garantía en cuentas de orden y el asiento
  cuadra igual**. Ya anotado en el propio código el 2026-09-01; sigue abierto.
- **No existe póliza, aseguradora ni inscripción**: ni tabla, ni entidad, ni endpoint. Re-verificado
  hoy, no heredado.

### Pendiente del usuario — son de negocio, no se deducen

Preguntas planteadas y **sin responder al cierre de esta entrada**: si el `1.12/1000` es tarifa
negociada y cambia al renovar; quién decide qué préstamos entran al incendio y si los 131 del
`sql/60` son una regla o una lista puntual; si la póliza es anual por cartera o una por préstamo;
qué pasa hoy con el seguro no consumido cuando alguien precancela; y **si el `sql/60` llegó a
correrse en producción**.

Ofrecido y no pedido todavía: el script que mide cuánto seguro hay cargado hoy en la cartera.

---

## ✅ 2026-09-09 — Reactivado el timer diario de mora. H47 levantada

**Orden directa del usuario:** *«Necesito activar nuevamente para que el proceso de cálculo de
mora se ejecute nuevamente a las 2am»*.

`ProcesoMoraPrestamoTimer:71` — el `@Schedule(hour = "2", minute = "0", second = "0",
persistent = false)` volvió a quedar descomentado. El cálculo (`ProcesoMoraPrestamoServiceImpl`)
**no se tocó**: nunca estuvo roto, sólo apagado el disparador.

### Lo que hay que entender antes de tocar esto de nuevo

**El apagado no era un olvido ni un defecto.** Del 2026-08-31 al 2026-09-09 estuvo comentado a
pedido del usuario porque *se seguían cerrando pagos de agosto con fecha de fin de agosto y no se
quería generar mora sobre esas cuotas*. Eso está ahora escrito **dentro del propio javadoc del
método**, que es donde lo va a leer quien tenga la mano encima del interruptor — no sólo acá.
Es la lección convergente del 2026-09-07 aplicada a un caso nuevo: el aviso vive en el punto donde
alguien se puede equivocar.

**El mecanismo que se destapa al prenderlo, y que sigue vivo:** en cuanto corra, el proceso escribe
`DTPRMRAA` en todas las cuotas vencidas con la fecha de esa corrida. `aplicarPagoACuota` **lee ese
campo persistido y no lo recalcula a la fecha del pago** (sólo la precancelación y el acuerdo de
condonación llaman a `recalcularMoraALaFecha`). Así que cualquier pago aplicado después cobra esa
mora. **Si quedaba algo de agosto cerrándose con fecha de fin de agosto, se le cobra mora que no
corresponde.** La causa de fondo está avisada a `lap-saa-1` desde el 2026-09-05 y sigue abierta.

### Lo que se verificó antes, y por qué importaba

**La mina del 2026-08-24 ya no está.** Ese día el proceso reclasificó a `EN_MORA(11)` todos los
préstamos que estaban en `DE_PLAZO_VENCIDO(8)`, en producción. Se comprobó **contra el código, no
contra el documento**, que la exclusión del 8 sigue puesta en los **dos** niveles:
`DetallePrestamoDaoServiceImpl:841` (`idEstado IN (:vigente, :enMora)`) y la guarda de
`ProcesoMoraPrestamoServiceImpl:207`. Prenderlo **no** repite aquello. La duplicación es
deliberada: `POST /prst/calcularMora/{idPrestamo}` entra directo al método y se saltea la consulta
del universo, así que sin la segunda guarda un préstamo en 8 invocado a mano se vuelve a romper.
Se le dijo explícitamente al agente que **no la unifique**.

### ⛔ No surte efecto hasta el próximo WAR

`persistent = false` ⇒ el timer se reconstruye desde la anotación **en cada arranque**.
Descomentar no prende nada por sí solo: queda enganchado al despliegue pendiente (otorgamiento,
reverso de cobro, Gs en transacción propia, H54, calificación de riesgo), que además necesita el
`crd/sql/218` corrido antes. **Mientras tanto, la mora se pone al día a mano** con
`POST /SaaBE/rest/prst/calcularMora` (idempotente, acepta `?fecha=` y `?usuario=`).

### Dos controles que quedan del lado del usuario

1. **La zona horaria del servidor.** `hour = "2"` es la hora del reloj de la JVM, y `LocalDate.now()`
   toma esa misma zona. Si WildFly corre en UTC, las 02:00 caen a las **21:00 de Ecuador del día
   anterior** — dentro del día hábil y con la fecha corrida un día. Nunca se verificó; se planteó
   hoy y está sin responder.
2. **Avisarle a `lap-saa-1`.** Ellos pidieron el congelamiento y el acuerdo escrito era que
   avisaran ellos cuando terminara el cuadre. El levantamiento vino por el otro lado. **Pendiente
   de autorización del usuario** para escribirle a su árbitro.

### Nota de proceso — modo directo autorizado

El usuario cortó la costumbre de pasarle los prompts para que él los copiara: *«tu mismo debes
enviarle los mensajes a tus agentes»*. Desde el 2026-09-09 el despacho a `omen-saa-1-be` y
`omen-saa-1-fe` va por `SendMessage`. Al usuario se le llevan decisiones de negocio, scripts para
correr y el permiso para hablarle a otro árbitro — no el reparto de trabajo del propio equipo.
**Es la misma corrección que el usuario ya le había hecho al árbitro de `omen-saa-2`** (su §31.3),
o sea que no era una preferencia de esa sesión: es cómo quiere que funcione el esquema.

---

## ✅ 2026-09-09 — CCPM: tres columnas nuevas en el informe mensual de préstamos

**Pedido del usuario, urgente:** agregar al informe financiero mensual de préstamos el nombre del
partícipe (razón social), la fecha de vencimiento del préstamo y el monto original.

| Pieza | Commit |
|---|---|
| DDL `crd/sql/220` + contrato `reportes/API-CCPM-COLUMNAS-NUEVAS.md` | `8bc41011` |
| BE — entidad + generador + `reportes/CCPM.md` | `6bb7db5f` |
| FE — modelo + columnas de la pantalla (saaFE) | `8729773` |

### Cuál era el reporte, y por qué no era obvio

Hay **tres** informes financieros mensuales y sólo uno es de préstamos: `CPRM` (aportes por
partícipe), `CJBM` (jubilados) y **`CCPM` = Crédito Cuotas Préstamos Mensual**. Es el único que
reporta operaciones de crédito. Tabla `RPR.CCPM`, generador `GeneracionCCPMServiceImpl`, pantalla
`rpr/forms/informes-mensuales-credito`.

⚠️ **El CCPM comparte lógica base con el G48 y NO es el G48.** El G48 es el reporte **regulatorio**,
con estructura fija, que va a la Superintendencia; el CCPM es el informe **interno** con campos
adicionales. Agregar columnas acá es seguro; replicarlas allá rompería una entrega al regulador.
Quedó escrito en `CCPM.md` para que nadie los "sincronice".

### ⛔ El dato que puede salir vacío, y es decisión tomada

`montoSolicitado` (`PRST.PRSTMNSL`) **no lo escribe ninguna línea del backend**. Se buscó
`setMontoSolicitado(` en todo `src/main/java`: aparece sólo en la entidad y en un mapper del app
móvil que lo *lee*. Llega tal cual del JSON del frontend al dar de alta, y la cartera migrada entró
por carga de Excel, que tampoco lo setea.

**Se le ofrecieron al usuario cuatro opciones —incluida medir primero— y eligió `montoSolicitado`
con la advertencia sobre la mesa.** Por eso el `sql/220` trae un **bloque 1 de medición** que
cuenta, sobre los préstamos vivos (estados 2 y 11), cuántos lo tienen en NULL o en 0. **Pendiente
de que el usuario lo corra.** Si sale mayoritariamente vacío, la decisión se revisa antes de que el
informe salga con una columna en blanco.

⭐ **Y por eso el null viaja como null hasta la pantalla, a propósito, en las dos puntas:** el
generador setea sin guarda y sin convertir a `0.0`, y `getCellValue` corta en null antes de
formatear. **Un cero en un informe financiero se lee como un dato real; un vacío se lee como un
faltante.** Convertirlo "defensivamente" habría hecho que la cartera migrada informara préstamos
solicitados por cero. Está anotado en el código de los dos lados.

### Lo que verificó el árbitro y no los agentes (regla 11)

- **Que el generador fuera el único punto que arma filas de CCPM.** Hay un segundo
  `new CreditoCuotasPrestamosMensual()` en `CreditoCuotasPrestamosMensualServiceImpl:30`, pero es
  un objeto vacío para el `remove` y no copia campos. Si hubiera sido un segundo camino real, el
  informe habría salido con parte de las filas en blanco **y sin ningún error**.
- **El manejo del null**, que el agente FE verificó sobre su propio código: `getCellValue:330-337`
  corta en `null`/`undefined` antes de las ramas de `esFecha`/`esNumero`, y es el único camino de
  la tabla (`html:242`) y de los dos exportadores a CSV (`:775`, `:810`). Correcto.
- **La fecha de vencimiento sale de `PRSTFCFN`**, que `PrestamoServiceImpl:568-575` calcula como el
  vencimiento de la **última cuota**, recorriendo los detalles. Se reescribe tanto en el alta como
  en la carga por Excel, así que está poblada en la cartera migrada. Verificado, no deducido.
- `mvn -q compile` exit 0 y `ng build --configuration development` limpio, corridos por el árbitro.

### ⛔ Ahora son DOS scripts los que van antes del WAR

`crd/sql/218` (control de 212/213, jubilados) **y** `crd/sql/220` (las tres columnas). El 220 no es
opcional: Hibernate mete toda columna `@Column` en el `SELECT`, así que con el WAR arriba y la
tabla sin las columnas se cae **la pantalla entera de informes mensuales** con `ORA-00904`, no sólo
lo nuevo. Es el accidente de `CRD.CFCR` del 2026-09-08. Cuarta vez que este patrón aparece en diez
días.

### 🔧 Hallazgo de entorno — cómo se compila el frontend en esta máquina

**El árbitro no pudo verificar el build hasta encontrar esto, y es transversal a todos los equipos.**

`node` **no resuelve** en Git Bash en esta OMEN. Angular sólo compila con la v22 de nvm, cuya
carpeta **no tiene `node.exe`: sólo `node64.exe`**. Por eso no alcanza con agregarla al `PATH` ni
usar `npm.cmd`/`npx.cmd` de ahí — esos `.cmd` buscan `node` a secas, que resuelve al v12 global.
Hay que invocar el binario directo contra el bin de la herramienta:

```bash
"/c/Users/xeonp/AppData/Roaming/nvm/v22.12.0/node64.exe" node_modules/@angular/cli/bin/ng.js build --configuration development
```

Vale la pena llevarlo al `CLAUDE.md` de `saaFE` o al §8 del registro compartido: cualquier árbitro
que intente cumplir la regla de "verificar que compila antes de commitear" se traba en lo mismo.
**No se toca desde acá sin consultar** — los dos son archivos compartidos.

### ✅ `crd/sql/220` CORRIDO Y SU CONTROL LEÍDO — 2026-09-09

El usuario corrió el script y pasó la salida del **bloque 3**, que es el control que decide si se
puede desplegar:

```
CCPMFCVN   DATE       7               Y
CCPMMNSL   NUMBER    22    18    2    Y
CCPMRZSC   VARCHAR2  2000            Y
```

**Las tres columnas, con el tipo esperado y todas nullable.** `RPR.CCPM` está lista.

⇒ **El CCPM ya no bloquea el WAR.** El orden se respetó: DDL primero, despliegue después, que es
justamente lo que falló el 2026-09-08 con `CRD.CFCR` y dejó los Gs de agosto sin salir.
El `sql/218` (jubilados) **sigue pendiente** y va antes del mismo despliegue.

⚠️ **El bloque 1 no se reportó.** Era la medición de cuántos préstamos vivos tienen `PRSTMNSL` en
NULL o en 0 — no bloquea nada y el usuario ya confirmó el campo por segunda vez
(*«usa el campo monto solicitado sin problema»*), así que la decisión está firme. Lo único que se
pierde es saber de antemano cuántas celdas van a salir vacías cuando alguien abra el informe.
Queda como dato disponible, no como pendiente.

---

## ✅ 2026-09-09 — Padrón de partícipes: voto estricto y «Mantiene Calidad»

**Pedido del usuario, urgente.** Especificación en `crd/ESPEC-PADRON-VOTO-Y-CALIDAD.md` (`1ac47c64`),
escrita **antes** de despachar. BE `48135290`, FE `7982640` (saaFE).

| | Antes | Ahora |
|---|---|---|
| `estadoMora` | EN MORA sólo a los 6 meses | **EN MORA con cualquier mes de atraso** |
| `habilitadoVoto` | ACTIVO + al día + hasta 6 cuotas | **ACTIVO + 0 meses + 0 cuotas + sin préstamo marcado en mora** |
| `mantieneCalidadParticipe` | — | **SI hasta 6 meses, NO desde 7** |

**Sin DDL:** el padrón se calcula al vuelo, no se persiste. Es el primer frente de esta serie que
no necesita un script antes del WAR.

### El nombre, y por qué no es el que pidió el usuario

Pidió llamarla «calidad de partícipe». **Ya existe una columna `calidadParticipe`** con valores
`ACTIVO / CESANTE`. Dos columnas de nombre casi igual y contenido distinto en el mismo Excel es una
confusión que después nadie desarma; se le ofrecieron alternativas y eligió **`Mantiene Calidad`**.
⇒ Antes de agregar una columna, buscar si el nombre ya está tomado con otro significado.

### ⛔ El defecto que el agente encontró sin que se lo pidieran, y que no compila mal

Al reescribir `estado_mora`, el parámetro **`:primerMesAlDia` dejó de aparecer en el SQL**, pero
su `setParameter` seguía ahí. **Hibernate lanza `IllegalArgumentException` al setear un parámetro
que no está en el texto de la consulta**: el padrón entero habría devuelto **500 en toda llamada**,
no sólo en los casos de mora. `mvn compile` no lo detecta — un SQL nativo es un `String`.

⭐ **La familia del defecto, que es lo que hay que llevarse:** al cambiar una condición de un SQL
nativo, el `setParameter` correspondiente queda huérfano y **rompe la consulta completa, no la
condición que se tocó**. El error simétrico también existe: dejar un `:param` en el SQL sin su
`setParameter`. Se verificó la otra punta —`:maxCuotasMoraElegible` sigue emparejado (SQL:538 ↔
setParameter:556) porque `elegible_miembro` lo usa— y estaba bien.

### Lo que verificó el árbitro y no el agente (regla 11)

- **Los 15 índices.** Insertar una columna en medio del SELECT corre todos los `row[N]` siguientes
  y **no da error de compilación**: daría un padrón con el correo en la columna del voto. Se contó
  a mano: 15 columnas en el SELECT, 15 `row[N]` en el mismo orden, 15 parámetros del constructor en
  ese orden. Es la misma familia que el `COLUMN_n` de los `.jrxml` que documenta `CLAUDE.md`.
- **Las dos copias de `ROUND(MONTHS_BETWEEN(:mesReferencia, ap.ultimo_mes_aporte))`.** Oracle no
  deja reusar un alias del mismo SELECT, así que la fórmula quedó duplicada: una para `estado_mora`,
  otra para `meses_en_mora`. Se compararon: idénticas, así que las dos columnas no se pueden
  contradecir. **Es deuda latente**: el día que alguien toque una sola de las dos, el padrón informa
  «AL DIA» con meses de atraso a la vista, sin ningún error.
- **Un comentario que quedó mintiendo.** Decía *«una fila puede estar AL DIA y aun así mostrar 1
  mes»* — cierto con la regla vieja, falso con la nueva, y **pegado al código que lo cambió**. Se
  devolvió al agente y se corrigió antes de commitear. Sin eso, el próximo lee el comentario,
  concluye que `estado_mora` está mal calculado y lo «arregla».

### Consecuencia visible que NO es un error

Van a aparecer muchas filas con **`Estado Mora = EN MORA` y `Mantiene Calidad = SI`** a la vez. Se
cae en mora con un mes; la calidad se pierde recién pasando los seis. Queda anotado en el código,
en `REGLAS-PADRON-PARTICIPES.md` §7bis y en la especificación, porque desde afuera parece
contradicción.

### Pendientes que deja

- ⛔ **Descargar el CSV de elegibles ANTES de desplegar.** Es la única foto del padrón con la regla
  vieja; después del WAR deja de ser reproducible. Y con ese mismo CSV se mide el impacto sin
  escribir SQL: filtrar `ACTIVO` + `Meses en Mora = 0` + `CUOTAS EN MORA = 0` +
  `PRESTAMOS EN MORA = No` da los votantes de la regla nueva. **Avisado al usuario tres veces, sin
  confirmar que lo haya hecho.**
- **Asimetría abierta:** votar exige **cero** cuotas en mora; ser `elegibleMiembro` tolera **6**.
  Deliberado —el usuario no pidió tocar la elegibilidad— pero nadie lo decidió explícitamente.
- El cambio **reduce** el padrón de votantes y puede reducirlo mucho. Sin medir todavía.

---

## 📄 2026-09-10 — Acta de entrega-recepción definitiva del SAA, redactada y entregada

**Pedido del usuario:** el acta que cierra el contrato con ASOPREP y se firma hoy. Fuente:
`docs/contractual/ACTA-ENTREGA-RECEPCION-DEFINITIVA-SAA.md` (`49de7df7` borrador, `36844093`
final); el Word se generó desde ese `.md` con un conversor propio en el scratchpad y se le entregó
al usuario por archivo.

### Lo que gobierna el documento, verificado contra los cuatro instrumentos firmados

- **Contrato 2025-10-13**, USD 132.000: la garantía de 6 meses corre desde el «Acta – Entrega
  Recepción Definitiva» (Décima Segunda). Por eso el acta se titula exactamente así **y** se
  declara también el acta del saldo del **adendum 2026-07-03** (USD 23.016 neto, plazo al
  2026-09-16). Un solo documento, dos efectos.
- **Anexo 1**: el único entregable exigible son las **fuentes**; no pide manuales ni
  documentación. La arquitectura que enuncia (SQL Server / WildFly 32 / Angular 19) no es la
  entregada; se fijó la real como «definitiva» apoyándose en que el informe
  INF-ASOPREP-SRV-2025-01 (base del segundo pago) ya decía Oracle.
- **La app ASOPREP CONTIGO es contractual** por el Anexo 1. Consultado `omen-app-1-arb` con
  autorización del usuario: **no está en producción** (sin ISP ruteando la IP pública, sin
  `CRD.USAP` en producción, sin usuarios, sin tiendas). Se puso como «desarrollada, integrada y
  validada en pruebas» con tres hitos externos, por recomendación coincidente de los dos árbitros.
  Verificadas sus dos correcciones: **13** endpoints (no 17 — yo conté rutas de clase) y tabla
  **`CRD.USAP`**.

### Decisiones del usuario que cambiaron el texto

1. **No mencionar los ocho frentes** sin desplegar: «puntos de mejora normales», se prueban
   mañana. Se eliminó el anexo de observaciones; la garantía cubre «cualquier corrección».
2. **Incluir manuales de usuario** (había pedido el mínimo; cambió de idea). Compromiso voluntario
   → acotado a **uno por módulo**, versión estabilizada, mismo mecanismo diferido que las fuentes,
   con tope al vencimiento de la garantía.
3. Cuentas de tiendas **de ASOPREP**, accesos entregados a NEXUS para la carga.

### Hallazgos de método

- ⭐ **Un `.docx` es un ZIP, y `ZipFile.CreateFromDirectory` de .NET Framework escribe las
  entradas con barra invertida** (`word\document.xml`). Word lo abre como dañado. Hay que crear las
  entradas a mano con `/`. Y en Windows PowerShell los `Add-Type` de `System.IO.Compression` van
  **antes** de cualquier literal de tipo, porque los resuelve al analizar el script.
- El lector de PDF de la máquina no renderiza escaneos (`pdftoppm` ausente); `pdfbox-app` por
  Maven + `render` a PNG lo resolvió. El «52 páginas» que reportó la herramienta eran 4.
- **Lo que se dejó fuera a propósito y por qué:** las mejoras identificadas por este equipo
  (seguros por póliza, estados financieros SBS faltantes, asimetría voto/elegibilidad). En un acta,
  listarlas las vuelve garantía gratuita; son Décima Primera.

---

## ✅ 2026-09-10 — La consulta de préstamos mostraba un saldo muerto

**Pedido del usuario, urgente:** «el valor del saldo capital que se muestra en la tabla al
consultar no es correcto». Confirmado con él: el saldo **del préstamo**, no el de la cuota.

| Pieza | Commit |
|---|---|
| Contrato `crd/API-SALDOS-PRESTAMO.md` (+ espejo `saaFE/docs/crd/`) | `c4cb4da4` · `336bbd2` |
| BE — `POST /rest/prst/saldos` (`calcularSaldosEnLote`) | `20b10b49` |
| FE — consulta, export CSV/PDF, diálogo de detalle y su PDF | `0c14e11` (saaFE) |

### Qué era

La pantalla leía **`Prestamo.saldoTotal` (`PRSTSLTT`)** en la tabla, en el export y en el diálogo.
**Ninguna línea del backend escribe esa columna** (ni `saldoCapital`, `saldoPorVencer`,
`saldoVencido`, `saldoInteres`, `totalPagado` de la cabecera): conservan el valor de la migración
o del alta. Ya estaba documentado como «campo muerto» en `petro/REGLAS-GENERALES-PETRO.md` §10
(2026-09-01, 28,5 M de saldo en cancelados) y `cobros-personales` lo esquivaba reconstruyendo desde
las cuotas. **La consulta de préstamos era la pantalla que nadie había migrado.**

### Dos hipótesis descartadas antes de despachar — vale registrarlas

1. **La tabla de cuotas.** `DTPRSLCP` por cuota es coherente en todo el flujo vivo (generación,
   Excel, motor, carga Petro productiva escriben `saldoInicialCapital − capitalPagado`).
2. **`ProcesoCargaPetroServiceImpl.procesarPrestamo:373-388`**, que pone `saldoCapital = 0` en cuotas
   pagadas — semántica *por cuota*, distinta del resto. Resultó ser la **vía alterna** de Petro,
   sin llamadores, que el flujo productivo no usa. ⚠️ **Mina dormida:** si alguien la conecta,
   corrompe `DTPRSLCP`. No se tocó (regla de los docs de Petro). Anotada acá.

### Por qué no se «arregló» la columna

Mantener `PRSTSLTT` viva exige tocar todos los caminos que mueven capital (motor, abono,
precancelación, condonación, reversos, Petro, mora diaria) y el primero que se olvide la vuelve a
congelar **sin ningún error**. Se expuso en lote el saldo calculado desde las cuotas con la lógica
del motor, que es la autoritativa. Costo aceptado: una lectura de cuotas por préstamo por página.

### Lo que verificó el árbitro y no los agentes

- **Efecto colateral evitado.** `calcularSaldosRealesCuota` **corrige y persiste** el estado de
  la cuota si la encuentra liquidada según los pagos. Una consulta no debe escribir: se exigió la
  variante pura `calcularSaldosCuota` (javadoc: *«nunca modifica ni persiste»*) y se verificó en
  el diff. Por lo mismo no se reusó `calcularTotalPendientePrestamo`, que llama a la impura.
- **Mi prompt contradecía mi contrato** (ítem f: «sin cuotas → se omite» vs §3: «cancelado →
  0,00»). El agente lo vio, resolvió a favor del contrato —que es lo que el FE consume— y lo
  reportó. Correcto: el FE muestra la omisión como error, y un cancelado en 0,00 es un dato.
- El FE **quitó el orden** por «Saldo Total» (ordenaría por la columna muerta con el valor
  calculado a la vista) y **reportó** que el PDF del diálogo leía el mismo campo muerto en vez de
  decidirlo solo. Se corrigió antes de commitear.

### Lo que queda, y no es de este cambio

- **`Prestamo.totalPagado`** tiene el mismo problema (cero escritores) y el diálogo de detalle lo
  sigue mostrando en «Total Pagado». Misma familia, sin arreglar: el pedido fue el saldo.
- El filtro **«Saldo desde/hasta»** sigue operando en el backend sobre `PRSTSLTT`. Documentado.
- **La app móvil** sirve las cinco columnas muertas (`MovilMappers:34-38`). Avisado a
  `omen-app-1-arb` con autorización del usuario, con el contrato del endpoint para reusar.
- `PRSTSLTT`/`PRSTSLCP` siguen en la entidad. Cualquier pantalla que las lea es un defecto aparte;
  una búsqueda `grep -rn "\.saldoTotal\|\.saldoCapital" saaFE/src` sobre `Prestamo` es la forma de
  encontrar la próxima.

### ⛔ 2026-09-10, tarde — la primera versión de `/prst/saldos` dejó la consulta inusable

**Error del árbitro.** El contrato decía «una lectura de cuotas por préstamo, aceptado a
propósito». Verifiqué que `calcularSaldosCuota` no escribía nada —leí la javadoc y la firma— y
**no leí dos líneas más abajo**: consulta los pagos de **cada cuota** (`MotorPago:116`). Una
página de 100 préstamos eran miles de consultas. El usuario lo vio en minutos.

⭐ **Lección:** «reusar la lógica existente» no exime de leer el cuerpo de lo que se reusa. La
firma dice qué devuelve; el costo está adentro. Y un costo escrito en un contrato con la palabra
«aceptado» es una afirmación que otros construyen encima.

**Arreglo:** tres consultas en lote (existentes, cuotas, pagos; `IN` de a 900) + sobrecarga pura
`calcularSaldosCuota(cuota, pagosVigentes)` extraída mecánicamente del motor — verificado con el
diff que ninguna línea aritmética cambió. Los tres DAO nuevos **no** atrapan excepciones, a
diferencia de sus vecinos: una falla real de base sale como 500, no como ceros silenciosos.
Contrato actualizado en `6bcb84d9` para que nadie vuelva al bucle.

---

## ⛔ 2026-09-11 — Los tres saldos que el motor pone en cero al cobrar (y la pantalla creía)

**Reportado por el usuario con captura**, cuota #36 del préstamo 70226: cuota PARCIAL con un pago
de $2,93 que sólo alcanzó al desgravamen, y el diálogo mostraba **Interés: pactado 29,60, pagado
0,00, pendiente 0,00**, con saldo de la cuota $68,08 en vez de $97,68 (= 100,61 − 2,93).
Arreglado en `saaFE 9f7f938`.

### La causa, y es una trampa para cualquier pantalla

`MotorPagoPrestamoServiceImpl:242-244`, dentro de la aplicación de un pago:

```java
// saldoCapital = saldoInicialCapital − capitalPagado (NO se pone en 0)
cuota.setSaldoCapital(Math.max(0, redondear(nullSafe(cuota.getSaldoInicialCapital()) - capitalPagado)));
cuota.setSaldoInteres(0.0);
cuota.setSaldoMora(0.0);
cuota.setSaldoInteresVencido(0.0);
```

⇒ **En cuanto una cuota recibe CUALQUIER pago, `DTPRSLIN`, `DTPRSLMR` y `DTPRSLIV` quedan en 0 en
la base, aunque esos conceptos no se hayan cobrado.** Sólo `saldoCapital` se calcula de verdad —y
el comentario de esa línea, «NO se pone en 0», está justo encima de las tres que sí lo hacen.

El diálogo leía esos tres campos como «lo que falta de esta cuota». La **Mora se salvaba de
casualidad**: en el caso reportado su saldo venía nulo (se acumuló después del pago) y el código
caía al cálculo correcto. Por eso el defecto se veía en el interés y no en la mora, lo que lo hacía
parecer un caso puntual.

### El arreglo, y por qué no fue en el backend

El pendiente de un concepto pasa a ser **siempre `pactado − pagado`**. No depende de campos que el
backend deja inconsistentes, y la tarjeta de saldo cuadra **por construcción**: Σpactado − Σpagado
= total de la cuota − total cobrado.

**No se tocó el motor a propósito:** ese comportamiento lo consumen los procesos de pago, y
cambiarlo tendría un alcance enorme comparado con el defecto. Queda anotado en el código del
diálogo, citando la línea, para que nadie vuelva a conectar esos campos.

⚠️ **Deuda abierta:** cualquier otra pantalla o proceso que lea `DTPRSLIN` / `DTPRSLMR` / `DTPRSLIV`
de una cuota **con pagos** está leyendo un cero que no significa «no se debe». En el frontend se
barrió y no hay otra que los use para mostrar un pendiente; **en el backend no se barrió**.

### Patrón que ya va tres veces en dos días

`PRSTSLTT`/`PRSTSLCP` (nadie las escribe), `PRSTTTPG` (ídem), y ahora `DTPRSLIN`/`DTPRSLMR`/
`DTPRSLIV` (se escriben mal). ⭐ **Un campo de saldo persistido en este sistema no es confiable
hasta que se demuestre quién lo escribe y con qué criterio.** Lo confiable es reconstruirlo desde
las cuotas y los pagos, que es lo que ya hacían `SaldoPrestamoService` y el motor.

---

# 🔒 CIERRE DE SESIÓN — 2026-09-14 — TRASPASO PARA QUIEN ARRANQUE

**Estado de los árboles al cerrar:** `saaBE` y `saaFE` limpios y al día con `origin/main`. Nada de
este equipo sin entregar. Los dos ejecutores (`omen-saa-1-be`, `omen-saa-1-fe`) confirmaron «nada
pendiente» y quedaron cerrados.

## Lo que se entregó entre el 2026-09-09 y el 2026-09-11 (todo en `origin/main`)

| Frente | saaBE | saaFE | Estado |
|---|---|---|---|
| Timer diario de mora reactivado a las 02:00 (H47 levantada) | `6c2d904d` | — | ⛔ **pendiente confirmar despliegue** |
| CCPM: nombre, vencimiento y monto solicitado | `6bb7db5f` | `8729773` | DDL `sql/220` **corrido** ✅ · desplegar |
| Padrón: voto estricto + «Mantiene Calidad» | `48135290` | `7982640` | sin DDL · desplegar |
| Acta de entrega-recepción definitiva del SAA | `36844093` (`docs/contractual/`) | — | entregada en Word; **confirmar si se firmó el 10-sep** |
| Consulta de préstamos: saldos calculados en lote, «Capital Pagado» por estado | `dc198b91` | `6a7b9a7` | sin DDL · desplegar |
| Diálogo de resumen de pago: pendiente por concepto = pactado − pagado | — | `9f7f938` | desplegar |
| Memorando de respuesta al requerimiento de documentación TI + 5 anexos | `0f938003` (`docs/regulatorio/`) | — | entregado en Word; **espera los datos de Paúl** |

**El WAR y el build pendientes arrastran TODO lo anterior** más lo de la semana previa
(otorgamiento, reverso de cobro, Gs en transacción propia, H54, calificación de riesgo).
**Antes del WAR: `crd/sql/218`** (control de 212/213, jubilados). **Antes de desplegar el
padrón: descargar el CSV de elegibles** — es la única foto con la regla vieja.
⚠️ El usuario dijo el 2026-09-10 que la consulta «ya no está lenta» → desplegó al menos hasta
`96fb57bd`; **no consta** que haya desplegado `dc198b91` ni el build con `6a7b9a7`/`9f7f938`.
Preguntárselo primero.

## Decisiones del usuario que gobiernan el código (no re-litigar)

- **Capital pagado** (`/prst/saldos`): préstamo en cualquiera de los tres estados cancelados → todo
  el capital; cuota 4/7 → capital de la cuota; cuota 5/6 → abonado en PGPR; PENDIENTE y el resto,
  incluido el nulo → 0 aunque tenga pagos. Contrato: `crd/API-SALDOS-PRESTAMO.md` §3bis y §7.
- **La mora se prendió por orden directa** el 2026-09-09; `lap-saa-1` había pedido congelarla.
  **Aviso a ese árbitro: pendiente de autorización del usuario.**
- **Los ocho frentes de la semana previa NO se mencionan en el acta** (son mejoras normales).
- **El acta incluye manuales de usuario** (uno por módulo, diferido con las fuentes, tope al
  vencimiento de la garantía) — compromiso voluntario, el contrato no lo exige.

## Supuestos del árbitro que faltan confirmar (con qué dato se cierran)

| Supuesto | Cómo se cierra |
|---|---|
| Cuota `VENCIDA` (8) y cuota sin estado aportan 0 al capital pagado | **Bloque 5 del `crd/sql/221`** — si hay volumen ahí, sumarlas como 5 y 6 |
| Sólo la app usa los saldos muertos de `Prestamo` fuera de las pantallas ya corregidas | grep sobre `saaFE/src` hecho (limpio); **sobre el backend, no hecho** |
| Nadie más lee `DTPRSLIN`/`DTPRSLMR`/`DTPRSLIV` de cuotas con pagos | **Barrido del backend, no hecho** — ofrecido al usuario, sin respuesta |

## Deudas registradas, sin dueño asignado

1. **Cinco columnas de saldo de `Prestamo` muertas** (`PRSTSLTT`, `PRSTSLCP`, `PRSTTTPG`,
   `PRSTSLPV`, `PRSTSLVN`): nadie las escribe. El filtro «Saldo desde/hasta» de la consulta sigue
   operando sobre `PRSTSLTT`. La **app móvil las sirve al teléfono** (`MovilMappers:33-38`):
   avisado a `omen-app-1-arb` el 2026-09-10 con el contrato del endpoint para reusar.
2. **`MotorPago:242-244` pone en cero los saldos de interés, mora e IV de la cuota al cobrar
   cualquier cosa.** El diálogo ya no los lee; el resto del backend no se barrió.
3. **`ProcesoCargaPetroServiceImpl.procesarPrestamo`** escribe `DTPRSLCP` con semántica por cuota,
   distinta del resto. Vía alterna sin llamadores; **mina dormida**.
4. **Asimetría voto / elegibilidad**: votar exige 0 cuotas en mora, ser elegible tolera 6. Nadie lo
   decidió explícitamente.
5. De antes: **H46** (corrida de jubilados no reintentable → respaldo antes de correr), **P18–P21**,
   **H48** (el centavo), seguros de préstamo por póliza (frente 3, cero construido), motivo de
   bloqueo de jubilados no persistido, estados financieros SBS faltantes.

## Herramientas que esta sesión dejó (y dónde)

- **Compilar el frontend en esta OMEN:** `node` no resuelve en Git Bash; la v22 de nvm no tiene
  `node.exe`, sólo `node64.exe` →
  `"/c/Users/xeonp/AppData/Roaming/nvm/v22.12.0/node64.exe" node_modules/@angular/cli/bin/ng.js build --configuration development`
- **Markdown → Word:** `tools/docx/md2docx.ps1` (README al lado). Así se hicieron el acta y el
  memorando.
- **PDF escaneado → imagen:** `pdfbox-app-3.0.3.jar` en `~/.m2` (`render` a PNG); el lector de
  PDF de la máquina no renderiza escaneos.
- **`git commit -- <rutas>`** siempre, y `git diff --cached --name-only` antes: el índice es
  compartido (H59). Y mirar los `??`: `commit -- rutas` no incluye archivos nuevos si no se
  agregaron (§39 de omen2).
- **Heredocs largos en Bash fallan en esta herramienta** con «unexpected EOF while looking for
  matching `''`»; para archivos largos usar el editor (Write) y `cat archivo >> destino`.

## Contexto contractual que no está en el código

`contrato-saa-asoprep` en la memoria del árbitro y `docs/contractual/`: contrato 2025-10-13, adendum
2026-07-03 (plazo al 2026-09-16, saldo USD 23.016 neto al firmar el acta), garantía 6 meses desde
el «Acta – Entrega Recepción Definitiva». El Anexo 1 sólo exige **fuentes**; manuales no. La app
**ASOPREP CONTIGO** es contractual y **no está en producción** (ISP, `CRD.USAP`, tiendas).

## Lección de la serie, para no repetirla

Tres vueltas sobre `/prst/saldos` en un día: (1) acepté un costo sin leer el cuerpo del método
reusado —consultaba pagos por cuota—; (2) el lote no alcanzó porque **todos los `@ManyToOne` son
EAGER** y cada entidad arrastra su grafo; (3) la fórmula cambió dos veces por aclaraciones del
usuario que un `.sql` de medición habría anticipado. ⭐ **Reusar no exime de leer; una entidad
JPA en este sistema nunca es «una fila»; y ante un campo ambiguo, medir antes que deducir.**

---

# ⛔⛔ 2026-09-14 — H60: el pago a jubilados sobregira la pensión complementaria

**Reportado por el usuario al generar los G de agosto 2026:** un jubilado con su cuenta de pensión
complementaria (aporte 23) **en negativo**. El proceso le pagó la pensión completa cuando el saldo
sólo alcanzaba para una parte. Regla del usuario, textual: *«Jamás debería ser así, el sistema no
debe permitir devolver más dinero o cruzarlo con préstamos del que un partícipe tenga.»*

## La causa: un `min` que se perdió, y un comentario que decía que no hacía falta

`PagoPensionComplementariaServiceImpl.generarMesesRetroactivos` (corrida real), por mes:

```java
double ollaTrasCruce   = valorTotal - aplicadoEsteMes;              // NO topada por saldo
double seguroInternoMes = min(valorSeguroMes, min(ollaTrasCruce, saldoTrasCruce));   // sí topado
double ollaTrasSeguro  = ollaTrasCruce - seguroInternoMes;
double remanenteMes    = ollaTrasSeguro;                            // ⛔ sin min contra saldoTrasSeguro
```

Con el comentario encima: *«Ya viene topada por saldo (saldoTrasSeguro >= ollaTrasSeguro siempre)»*.
**Es falso** en cuanto el saldo es menor que la olla. Ejemplo: saldo 100, pensión+seguro 500,
seguro 50, sin préstamo → seguro 50, pensión **450** al banco, saldo final **−400**. El único corte
por saldo es el `if (saldoRestante <= TOLERANCIA) break` al **empezar** el mes, así que el mes en que
el saldo es parcial sale entero.

`previsualizarJubilado` (prevuelo) tiene **el mismo defecto** sobre el acumulado
(`pensionNominal = ollaTrasSeguro`), con el mismo comentario falso. Por eso el prevuelo y la corrida
**coincidían** y nadie vio una diferencia: los dos estaban mal igual.

**Es una regresión, no un defecto de origen.** Antes de `a18b1b80` (2026-09-04, «el seguro médico
pasa a ser un pago a un proveedor») el código tenía
`remanenteMes = min(remanenteNominal, saldoLibreParaRemanente)` y el prevuelo
`remanenteProcesable = min(montoADineroNominal, saldoLibreParaDinero)`. Al reordenar la olla por
prioridades (cruce → seguro → pensión) se toparon el cruce y el seguro, y la pensión —la única que
sale al banco— quedó sin tope. **El contrato seguía diciendo `min(remanente nominal, saldo libre)`:
el documento tenía razón y el código no.**

## Por qué costaba verlo

1. La prueba de la corrida de agosto se verificó contra **el mayor** (asientos cuadrados), y el
   asiento cuadra igual: el devengo va por `remanente`, la orden por `remanente`. Un sobrepago
   consistente en todos lados no descuadra nada.
2. `crearMovimientoNegativo` **no valida saldo**. Los otros dos caminos que sacan dinero de un aporte
   sí: `ProcesoPagoPrestamoServiceImpl.consumirAportes` (cruce, revalida dentro de la transacción) y
   `DevolucionAporteServiceImpl` (dos veces). El pago de pensión era **el único camino sin
   guardarraíl**.
3. Sólo se manifiesta en el mes en que el saldo es **parcial**, que es justo el último de cada
   jubilado: poco frecuente y siempre al final.

## Segundo hueco, del mismo origen: el seguro al proveedor

`generarSeguroDelMes` → `generarSeguroIndividual` fija `PGPCVLSG` con el seguro **nominal** del VPPC y
emite la orden al proveedor por la **suma de nominales**, sin mirar el saldo. Después la pensión
descuenta `seguroInternoMes` **topado por saldo tras el cruce**. Si el saldo no cubría cruce +
seguro, al proveedor se le pagó **más de lo que salió de la cuenta del jubilado** (va contra
`2.3.90.90.06`, dinero del fondo). El bloque 4 del `sql/222` lo mide. **La corrección necesita una
decisión del usuario** (qué cede cuando no alcanza para cruce + seguro), así que NO va en el primer
despacho.

## Verificado que el resto de caminos sí respeta el saldo

| Camino que resta de un aporte | Guarda de saldo |
|---|---|
| Cruce contra préstamo (`consumirAportes`) | ✅ `ProcesoPagoPrestamoServiceImpl:688` y `:717`, dentro de la transacción |
| Devolución de aportes | ✅ `DevolucionAporteServiceImpl:295` y `:420` |
| Traslado de jubilación | ✅ traslada exactamente el saldo (`AporteServiceImpl:466-487`) |
| **Pago de pensión (`crearMovimientoNegativo`)** | ⛔ **ninguna** — se agrega en esta corrección |
| **Reverso de un aporte positivo (`AporteServiceImpl.reversarAporte`)** | ⚠️ ninguna: reversar un aporte ya consumido deja el saldo negativo. Es una corrección de datos, no una salida de dinero — **decisión del usuario** si se bloquea |

## Diseño de la corrección (despacho 1, BE)

1. **Corrida:** `remanenteMes = min(ollaTrasSeguro, saldoTrasSeguro)`, nunca negativo.
2. **Prevuelo:** `pensionNominal = min(ollaTrasSeguro, saldoTrasSeguro)`.
3. **Guardarraíl:** `crearMovimientoNegativo` revalida `sumValorByEntidadYTipo(entidad, 23)` y lanza
   `ERR_SALDO_INSUFICIENTE` si `saldo < valor − TOLERANCIA`. Mismo patrón que `consumirAportes`.
   Tumba **sólo a ese jubilado** (su transacción es `REQUIRES_NEW`) y queda en `errores`: una falla
   ruidosa en vez de un sobrepago.
4. Corregir los dos comentarios falsos.

Sin cambios de contrato de forma, sin DDL, sin frontend. Contrato actualizado con el invariante en
`crd/API-PAGO-PENSION-COMPLEMENTARIA.md`.

## Lo que el código NO arregla

El dinero de agosto **ya salió**. Recuperarlo (descontar de la pensión futura, pedir devolución,
asumirlo) es una decisión de negocio. Mientras tanto el saldo negativo **frena solo** al jubilado en
las próximas corridas (`saldoRestante <= TOLERANCIA → SALDO_AGOTADO`): no se le vuelve a pagar.

## ✅ Despacho 1 entregado — 2026-09-14

Ítems 1-3 aplicados por `omen-saa-1-be` en `PagoPensionComplementariaServiceImpl` (corrida, prevuelo
y guardarraíl en `crearMovimientoNegativo`). **Revisado por el árbitro sobre el diff**, no sobre el
reporte: `saldoTrasSeguro` no puede ser negativo en ninguno de los dos puntos (`saldoTrasCruce` sale
de `max(0, …)` y el seguro ya viene topado por él), y los dos únicos llamadores del guardarraíl
(`:1904` seguro, `:1909` pensión) reciben montos ya topados, así que en operación normal no dispara:
es la red. `mvn -q compile` → exit 0 (árbitro). **Surte efecto con el próximo WAR: no correr la
pensión de septiembre antes.** Queda abierto: el seguro al proveedor (decisión A/B/C del usuario), el
reverso de aportes, y qué hacer con lo ya sobrepagado (`sql/222`).

## 2026-09-14 — Seguro: opción A decidida, y lo que el ítem 4 midió

**Decisión del usuario:** opción **A** — seguro topado por saldo al fijarlo, y descontado PRIMERO en la
corrida (seguro → cruce → pensión). Diseño completo en `crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md`
§11, escrito antes de despachar.

**Ítem 4 del ejecutor, contrastado por el árbitro:**
- (a) confirmado: `generarSeguroIndividual:1001,1022` fija el nominal y `generarSeguroDelMes:1125-1129`
  paga la suma, sin saldo.
- (b) `sincronizarPagos` y `generarContraMovimiento` sin riesgo; `generarUnMesSinPrestamo` muerto y con
  guarda. **Pero el ejecutor afirmó que `generarPagosDelMes` (deprecado) tiene el mismo defecto, y no
  es así:** su `totalSeguro` suma `seguroInternoMes`, que ya viene topado, y la orden al proveedor sale
  DESPUÉS de los descuentos (`:827-829`, `:873-875`). Lo que engaña es el comentario «NOMINAL» de
  `:826`, que quedó viejo. Leyó el comentario y no la variable: el mismo error de H56.

**Un agujero más que apareció al diseñar la A, y que nadie había medido:** el proceso de seguro fija
seguro a todo `JUBILADO_COMPLEMENTARIO`, pero la corrida de pensiones sale temprano como `SIN_ANCLA` o
`AL_DIA` sin descontar nada. Ese seguro se paga al proveedor y no sale de la cuenta de nadie. Cerrado
en el diseño (§11.1: seguro fijado 0 en esos casos). El bloque 4 del `sql/222` lo muestra como filas
con `PGPCVLPN` nulo en períodos cuya pensión ya corrió.

## ✅ 2026-09-14 — H60 cerrado en código: pensión, seguro y el mes en cero

| Commit | Qué |
|---|---|
| `1dbd4e0c` | Pensión topada por saldo (corrida y prevuelo) + guardarraíl en `crearMovimientoNegativo` |
| `612de4ba` | Opción A: seguro topado al fijarlo (0 sin ancla / al día), reservado y descontado primero |
| `77f41d34` | Contrato §11.2.3 corregido: con saldo agotado, un mes sin seguro propio se saltea |
| este commit | Implementación de ese salteo |

**Error del árbitro, registrado:** la primera redacción del §11.2.3 mandaba procesar un mes con
seguro, cruce y pensión en 0 cuando quedaba seguro reservado más adelante. Eso dejaba un PGPC `PAGADA`
por $0 y un asiento de devengo sin líneas. Lo vi al revisar el diff, no el ejecutor: implementó fiel
lo que yo había escrito. **Una especificación con fórmulas no reemplaza recorrer el caso borde hasta el
asiento.**

**Verificación:** los 4 casos (C1, C2, C3 y el del salteo) los recorrió el ejecutor contra el código
final, y el árbitro los contrastó contra el diff. Compilación: `javac` dirigido, exit 0. El
`mvn compile` del árbol completo falla hoy por un frente de `cxp` sin commitear de otro equipo
(`PagoProgramadoServiceImpl` y 10 archivos más), ajeno a esto.

**Pendiente para que surta efecto:** WAR. No correr seguro ni pensión de septiembre antes. `sql/222`
sin correr. Sin decidir: el sobrepago de agosto y el reverso de aportes ya consumidos.

## 2026-09-14 — H61: la devolución valida bien, pero ninguna validación resiste dos operaciones a la vez

A pedido del usuario («asegurarnos que jamás vuelva a pasar»), revisión de la devolución de aportes y
de todo lo que resta de un aporte. Detalle y diseño en `crd/INVARIANTE-SALDO-APORTES.md`.

- **La devolución está bien.** Valida dos veces y descuenta al registrar, así que la orden sale por
  exactamente lo descontado.
- ⛔ **Ningún camino bloquea.** Las "revalidaciones anti-carrera" sólo protegen dentro de la misma
  transacción. Dos operaciones simultáneas sobre el mismo partícipe leen el mismo saldo y pasan las
  dos. No hay un solo `FOR UPDATE` en `crd` (el único precedente del repo es `ChequeServiceImpl:459`).
- ⛔ **Puertas traseras abiertas:** `PUT /rest/aprt` acepta cualquier `valor`, `POST` acepta negativos y
  `DELETE /rest/aprt/{id}` borra cualquier fila. Borrar el negativo de una devolución ya pagada
  devuelve el saldo y permite cobrar dos veces.
- ⛔ **`reversarAporte`** puede dejar el saldo negativo, y lo usa la anulación de cobros.

**Por qué costaba verlo:** cada camino, leído solo, tiene su validación y su comentario de
"anti-carrera". El agujero está en lo que la palabra promete y el nivel de aislamiento no cumple, y en
las rutas que no son procesos (el CRUD genérico), que nadie revisa porque "nadie las usa".

## ✅ 2026-09-14 — H61 implementado

Los cuatro ítems de `crd/INVARIANTE-SALDO-APORTES.md` aplicados por `omen-saa-1-be`, **revisados por
el árbitro sobre el diff**:
- `AporteDaoServiceImpl.bloquearAportesEntidad`: nativa, `FOR UPDATE WAIT 30`, sólo traduce `ORA-30006`
  y relanza el resto.
- Bloqueo antes de leer saldo en `consumirAportes`, `registrarDevolucion`, `generarMesesRetroactivos`,
  `generarSeguroIndividual`, `crearMovimientoNegativo`, `procesarJubilacion` y `reversarAporte`. Todos
  corren en `REQUIRED` o dentro de un `REQUIRES_NEW` por jubilado: el ejecutor lo confirmó método por
  método y ninguno queda fuera de una transacción.
- `reversarAporte` rechaza si deja el saldo bajo cero. **Cambia la anulación de cobros**: un cobro cuyo
  dinero ya se devolvió o se cruzó no se anula hasta anular primero eso.
- `/rest/aprt`: `DELETE` rechazado (antes llamaba al DAO directo, **saltándose el service**); `POST` sin
  negativos; `PUT` no cambia valor, tipo ni entidad (compara contra copias locales tomadas antes del
  merge). Los errores de validación salen como `400`, no como `500`. El cambio de estado de
  `participe-dash` sigue pasando porque reenvía el mismo valor.

`mvn -q compile` del árbol completo → exit 0 (árbitro). Surte efecto con el próximo WAR.

## 2026-09-14 — H62: «Cobros personales» muestra el valor mensual de aportes de una tabla congelada

**Reportado por el usuario:** un socio con **129,95** en `CRD.CNTR` y en `CRD.HSTR`, y la pantalla
mostrando **51,98**.

**Causa, verificada en el código:** `cobros-personales.component.ts:668-672` (desde `saaFE 438f3e7`,
2026-08-01) lee el valor mensual de **`CRD.HDAP`** (`HistoricoDesgloseAporteParticipe`), buscando por
cédula y tomando el registro de mayor `idCarga`. **Nadie escribe `HDAP`**: ni el backend (el único
`new HistoricoDesgloseAporteParticipe()` está en `remove`) ni el frontend (ninguna pantalla llama a su
`POST`/`PUT`). Es una foto cargada desde afuera, por migración o SQL, y nunca se actualiza. Cualquier
cambio de sueldo o de porcentaje posterior a esa carga no aparece.

**La fuente que usa el resto del sistema** es `CRD.HSTR` con `estado = 99`, el más reciente por
`fechaIngreso` (`HistorialSueldoDaoServiceImpl.selectByEntidadYEstadoActivo`). La usan la carga Petro
(`CargaArchivoPetroServiceImpl:4462`, `:4913`) y la generación del archivo
(`GeneracionArchivoPetroServiceImpl:1115`).

**Impacto:** sólo visual. El valor se muestra en la columna «Valor mensual» y alimenta el mensaje de
cobertura («este pago cubre N meses», `:1174`). No entra en el monto cobrado ni en el devengo que
registra el backend. Pero el operador decide cuánto cobrar mirando ese número.

**Corrección propuesta (sólo FE, sin tocar el backend):** leer `/rest/hstr/selectByCriteria` por
`entidad.codigo` y `estado = 99`, el más reciente por `fechaIngreso`, y usar `montoCesantia` /
`montoJubilacion`. **Sin despachar**: espera el visto bueno del usuario.

### H62 — decisión y despacho (2026-09-14)

**El usuario decidió la fuente: el CONTRATO**, «como la generación del archivo Petro». Contrastado
contra el código antes de despachar (regla 12): **la premisa es cierta sólo a medias.** La generación
lee vigencias de contrato **únicamente si el flag del rubro 242 está encendido**, y está **apagado por
defecto**. Apagada, lee `HSTR` estado 99. La carga Petro lee `HSTR` siempre. Se le planteó al usuario.
La decisión de negocio (contrato) no cambia por eso, pero si el flag sigue apagado, la pantalla y el
archivo pueden diferir.

Contrato: `crd/API-VALOR-MENSUAL-APORTE-COBROS-PERSONALES.md`. Se usa la **vigencia que rige al último
día del mes**, no el espejo `CNTRMNAC`/`CNTRMNAJ`: el espejo toma la vigencia abierta aunque arranque en
un mes futuro. Despachado a `omen-saa-1-fe`.

### ✅ H62 entregado — `saaFE 79e75a3`

`omen-saa-1-fe` contrastó el contrato contra el backend antes de programar y no halló discrepancias.
La pantalla ya no lee `HDAP`: consulta `/rest/cntr/porEntidad` y aplica la regla de la vigencia que
rige al último día del mes, con cuatro estados visibles (monto / Sin vigencia / Sin contrato / No
disponible). Revisado por el árbitro sobre el diff, cinco casos recorridos, `ng build` limpio
(árbitro). **Queda del usuario:** confirmar si el flag del rubro 242 está encendido. Si no lo está, el
archivo Petro sigue saliendo de `HSTR`.

## 2026-09-14 — H63 y H64: el lápiz de contratos y el interruptor del rubro 242

**H63 — el lápiz de «Consulta de contratos» manda al menú principal.** Reportado por el usuario.
Causa: las seis navegaciones del módulo de contratos del frontend apuntan a
`/menucontabilidad/menucreditos/...`, pero `menucreditos` es una ruta **hermana** de
`menucontabilidad`, no hija. La URL no existe y el comodín `**` la manda a la raíz sin ningún error.
**Por qué costaba verlo:** un comentario en `contrato-edit.component.ts:193` afirma que la ida «ya se
verifica». Se verificó leyendo el `navigate`, no navegando. Es otro caso de verificar por lectura lo
que sólo se verifica ejecutando.
**Consecuencia de negocio:** el usuario tenía **apagado el rubro 242 porque la pantalla de contratos
no funcionaba**. Sin poder revisar las vigencias, no podía encender la generación por contratos.

**H64 — interruptor del rubro 242.** Pedido del usuario: en la misma pantalla que el de contabilidad.
El backend ya lo tenía completo (`GET/PUT /rest/cnfg/generacionPorFaltanteAh`, misma forma que
`contabilidadCrd`). Sólo faltaba el frontend. Trampa registrada y no corregida: con el catálogo del
rubro ausente, el `GET` de **los dos** flags responde «apagado» en vez de fallar.

Contrato de los dos: `crd/API-INTERRUPTOR-GENERACION-POR-FALTANTE.md`. Despachado a `omen-saa-1-fe`.

**✅ H63 y H64 entregados** — `saaFE 96df8f3` (navegación de contratos + tarjeta del rubro 242 en
«Parámetros de créditos (CRD)»), contrato espejado en `a635f8d`/`72cd77b`.

---

# 2026-09-14, noche — PRODUCCIÓN = `origin/main`. Arranque de sesión nueva del árbitro

**Dato del usuario, textual:** *«todo lo que está hecho commit y push está en producción en este
momento. Está subido al servidor de producción.»* Al momento de decirlo, `origin/main` estaba en
`saaBE 94980005` y `saaFE 17cf124`. **Todo lo que este tablero marcaba «desplegar» o «surte efecto con
el próximo WAR» ya está vivo:** timer de mora 02:00, CCPM, padrón, `/prst/saldos`, H60 (pensión y
seguro topados por saldo), H61 (bloqueo `FOR UPDATE`, `/rest/aprt` cerrado, `reversarAporte` rechaza
saldo negativo), H62, H63, H64, otorgamiento, reverso de cobro, Gs, H54, calificación de riesgo.

**Lo que eso cambia, y no está confirmado:**
- `crd/sql/218` iba **antes** del WAR y no consta que haya corrido. Sigue sirviendo después: es de solo
  lectura y su bloque 3 dice si queda algún período de `PGPC` sin cabecera `CRJB` (pagable dos veces).
- `VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql` es del **2026-08-30** y cubre 98 entidades; hoy
  `model/crd` tiene **107** `@Table`. **No incluye `CRJB`, `CFCR`, `ESCR` ni `USAP`.** Tal como está, da
  una tranquilidad que no corresponde (regla 9).
- El timer de mora corre esta noche a las 02:00 **con la zona horaria del servidor sin verificar**.
- «Descargar el CSV de elegibles antes de desplegar el padrón»: si no se hizo, esa foto ya no existe.
- La restricción de H60 (no correr seguro ni pensión de septiembre antes del WAR) **queda levantada**.

**Defectos encontrados en la revisión de arranque, vivos en producción (verificados en código):**
- `crd` — **«Pago Cuota» simula el pago**: `forms/pago-cuotas/pago-cuotas.component.ts` muestra «Pago de
  $X registrado exitosamente» sin llamar al backend (`TODO: Enviar datos al backend`), con cuentas mock.
  Está en el menú (`menucreditos.component.ts:264`). Mismo patrón que H31.
- `cnt` — **la limpieza de temporales borra por PK**: `reporte-myan.service.ts:68` → `DELETE /myan/{sec}`
  y `reporte-balance.service.ts:44` → `DELETE /dtmt/{idEjecucion}` pegan al borrado por PK
  (`MayorAnaliticoRest:308`, `TempReportesRest:309`), no a `/resultado/{…}` (`:293`/`:294`). Los temporales
  no se limpian y, si el secuencial coincide con el PK de otra fila, se borra esa fila. Error silenciado.
- `cnt` — **mayorizar/desmayorizar desde `periodo.service.ts:143-155`** hace POST a `/prdo/mayorizar/{id}`,
  que no existe en `PeriodoRest`. La mayorización real va por `/myrz`.
- `crd` — desembolso: `PrestamoServiceImpl:296` `ID_PRODUCTO_PAGO_SOCIOS_POR_PAGAR = null` ⇒ `aprobar`
  falla siempre, y el FE no manda `idEmpresa`/`idUsuario`.
- Corrección a este tablero: `validaDebeHaberAsientoContable` (la guarda espejo, sin redondeo) **sí tiene
  llamadores** — `AsientoServiceImpl:306` (cierre) y `TransferenciaServiceImpl:128,237`.

---

# 2026-09-15 — Revisión contra CÓDIGO (no contra docs) y lote urgente — EN PAUSA, sin despachar

**Alcance de la sesión: `crd` + `cnt`.** Cuatro barridos de solo lectura contra el código actual
(BE `de9add7f`, FE `c4136e3`), con los hallazgos graves reverificados por el árbitro. **Nada
despachado**: el usuario pidió corregir urgente y enseguida cambió a una revisión específica. Este
bloque es lo que hay que retomar.

## Correcciones a la revisión de arranque del 14-09 (medidas, no deducidas)

- **La guarda espejo `validaDebeHaberAsientoContable` NO es defecto vivo en `cnt`:** su único llamador
  de `cnt`, `AsientoServiceImpl.generaAsientoCierre`, **no tiene llamadores** (grep). Los vivos son
  `tsr` (`TransferenciaServiceImpl:128` lanza; `:237` ignora el retorno), fuera de alcance.
- **La limpieza de temporales de `cnt` que borra por PK es de baja severidad:** sólo toca `CNT.MYAN`/
  `CNT.DTMT`, que son temporales; el efecto real es que no se limpian nunca. **Arreglarla choca con dos
  cosas:** los balances imprimen desde `DTMT` por `P_DTMTSCRP` (si se limpia al salir, el `idEjecucion`
  del pie no reproduce nada) y el secuencial es `max+1` sin bloqueo (dos usuarios pueden compartir
  número y la limpieza de uno borraría el del otro). Además `MayorAnaliticoServiceImpl
  .eliminaConsultasAnteriores:385` borra una sola cabecera usando el secuencial como PK. **Pide
  decisión antes de tocar.**
- **Aprobar préstamo no es defecto escondido:** falla a propósito hasta resolver `crd/sql/157` bloque 2
  (`ID_PRODUCTO_PAGO_SOCIOS_POR_PAGAR = null`). El FE además no manda `idEmpresa`/`idUsuario`, y
  `API-CICLO-OTORGAMIENTO.md` tampoco los documenta (contrato viejo frente al código de `7bca1713`).
- **Cantón → `/btpc`:** ninguna pantalla usa Cantón. Código muerto.
- **H48 (el centavo) está corregido** (`0838f7ad`, `MEDIO_CENTAVO`). Queda un residuo: `MotorPago:485`
  marca PAGADA con 0,01 pendiente (condona el centavo en silencio).
- **H50 corregido** (`anularCobro` y `reversarProceso` borran DSBN). **El reverso de cobro está
  implementado** BE+FE (`/cbcr/{id}/reversar`).

## Lote urgente — defectos VIVOS en producción, silenciosos, alcanzables desde la UI

| # | Qué | Evidencia | Corrección decidida por el árbitro |
|---|---|---|---|
| U1 | **El seguro de jubilados nunca se paga al proveedor si falla la orden.** `generarSeguroDelMes` (NOT_SUPPORTED) fija cada seguro en su `REQUIRES_NEW`; si `generarOrdenPagoProveedorSeguro` lanza, CRJB no se graba. Al reintentar, `generarSeguroIndividual:1008` devuelve null para todos («ya fijado»), total $0, sin orden (`:2501`), y CRJB queda `estadoSeguro=1` → no se puede volver a correr | `PPCS:1168-1236`, `:1008-1012`, `:2501-2514` | BE: el total de la orden = Σ seguro fijado de los jubilados evaluados, **nuevo o preexistente**. La orden ya es idempotente por `(CRD_SEGURO_JUBILADOS, anio*100+mes)` |
| U2 | **«Anular» en Historial de operaciones descuadra un cobro CBCR.** `/prst/anularOperacion` anula PGPR y recalcula cuotas de un evento enlazado a `DCBC.EVPRCDGO`; `contabilizarReverso:744` no hace nada (el asiento vive en el CBCR); el cobro queda PROCESADO con asientos y DSBN vivos, y después `reversarProceso` queda bloqueado (`ERR_EVENTO_YA_ANULADO`). Sin error | FE `historial-operaciones-dialog.component.ts:96-141`; BE `ProcesoPagoPrestamoServiceImpl:1210-1236`, `ContabilidadPrestamoServiceImpl:744-749` | BE: rechazar **en `PrestamoRest.anularOperacion`**, no en el service, un evento enlazado a un `DetalleCobroCredito`, con código de negocio y mensaje que remita a `/cbcr/{id}/reversar`. ⛔ **No en el service:** su único otro llamador es `CobroCreditoServiceImpl:574` (`reversarLineasProcesadas`), que corre con el enlace todavía puesto. Falta la query `DetalleCobroCreditoDaoService.selectByEvento`. FE sin cambio obligatorio: el diálogo ya muestra `mensajeDeRespuesta` |
| U3 | **«Pago Cuota» simula el pago** (éxito sin HTTP, cuentas mock) | `pago-cuotas.component.ts:565-611`; menú `menucreditos:260-265`; ruta `app.routes.ts:1101` | FE: retirar del menú y de la ruta (conservar archivos). El cobro real es Cobros Personales |
| U4 | **«Asignación de Seguros» simula** («asignado correctamente (simulado)») | `asignacion-seguros.component.ts:298-314`; menú `:191-195`; ruta `app.routes.ts:1281` | FE: retirar del menú y de la ruta. No hay backend de pólizas |
| U5 | **«Plantilla general» (cnt) finge guardar**: ante cualquier error del POST mete la fila en la grilla con «guardado localmente para demostración»; el update hace lo mismo («modo demo»); «duplicar» sólo copia en memoria; «crear asiento desde plantilla» navega a una ruta inexistente. Las plantillas gobiernan los asientos automáticos | `plantilla-general.component.ts:700-766, 892-910, 957-968, 972-1003, 1260-1269` | FE: quitar fallbacks locales/demo y mostrar el error real; quitar (o persistir) duplicar; quitar el botón de asiento desde plantilla. **`cnt` es compartido** (registro §4, bloque de `omen-saa-2`): avisar con autorización del usuario |

## Segundo lote — sin UI que los alcance, o fallan con aviso, o piden decisión

- **Puertas REST sin contabilidad ni guardas** (sólo por HTTP directo): `/prst/pagarCuota`,
  `/prst/pagarMultiplesCuotas` (`contabilizarPagoCuota` devuelve null), `/prst/precancelar` 100 %
  efectivo, `/pgpc/generarPagosDelMes` (deprecado, **se salta las guardas de CRJB** → pago doble
  posible), `/asgn/procesarCargaPetro` (reescribe `DTPRSLCP` con otra semántica), POST/PUT genérico de
  `/cfcr` (vigencias solapadas).
- **Cobros personales, rama débito:** `registrarAportesDelSocio` → `POST /aprt/registrarAporte` directo,
  sin CBCR ni asiento (`cobros-personales:1392,1607-1650`; `AporteServiceImpl:379`). **Decisión del
  usuario:** mandarlo por CBCR o bloquear aportes en débito.
- **Precancelación sin cuotas pagadas (P21) confirmada:** la ancla fallback queda PAGADA(4) y el
  re-bandeo suma sólo estado 7 → `IncomeException` y rollback. Pide la decisión ya anotada en P21.
- **USAP:** el bloqueo por 5 intentos nunca persiste (`save` + `throw IncomeException` en la misma
  transacción, rollback). La app no está en producción. Tampoco hay pantalla de oficina.
- **Desembolso:** además de `sql/157` y el FE, no hay sincronización CRD←CXP: si tesorería anula la
  orden, el préstamo queda VIGENTE con asiento de entrega vivo.
- **Jubilados:** ancla envenenada (H46) confirmada y ampliada (cualquier negativo del tipo 23 la mueve,
  incluido `reversarAporte`); motivo de bloqueo no persistido; sin mecanismo de recuperación del
  sobrepago; rechazo del pago no revierte el devengo; la reserva del seguro no la respetan
  `registrarDevolucion`/`consumirAportes`/`reversarAporte`; ~11 comentarios que contradicen el código.
- **Timer de reconciliación de devoluciones apagado desde el 08-27**; sin UI para `/dvap/sincronizar`.
- **`cnt`:** botones mayorizar/desmayorizar de Período contable a `/prdo/...` inexistente (404 con
  aviso); mayor analítico viejo con rótulos mal y sin la opción 2; posible cabecera `codigo=0L` en la
  distribución 2 del mayor; balances sin RUC, sin línea A = P + Pat, sin marca de provisional;
  Estado de Resultados / Patrimonio / Flujos inexistentes.
- **FE crd varios:** Preview de Generar archivo Petro → `/pdga/preview` inexistente (404 con aviso);
  `corregirDuplicado` no-op silencioso en detalle de carga; filtro «Saldo desde/hasta» sobre
  `PRSTSLTT` muerta; `MovilMappers:46` sirve `PRSTTTPG` muerta a la app.

## Scripts sin constancia de ejecución (preguntar al usuario)

`211` (masivo, escribe), `212`/`213`/`218` (CRJB), `DDL-USUARIO-APP-MOVIL`, `221`, `222`, `157`,
`158`, `156`, `81`. El verificador de esquema de `crd` no cubre 9 entidades: CFCR, CRJB, CTAP, DAAP,
DAPR, DSBN, ESCR, PGPC, USAP.

---

# ✅ 2026-09-15 — H65: reemitir el pago de una devolución rebotada · H66: la anulación dejaba viva la orden

**Pedido del usuario:** una devolución se pagó a una cuenta mal digitada y la transferencia rebotó.
Quería reprocesar **sólo la salida del pago**, sin revertir el aporte negativo.

**Por qué no se podía (medido):** la cuenta viaja copiada en `PGS.PGTR` (`PGTRBFCT`), una orden
rechazada no tiene vuelta, y `sincronizarDevolucion` revertía todo sola **cada vez que alguien abría
las devoluciones del partícipe** (`listarPorEntidad` reconcilia antes de listar). Lo que lo hizo
posible sin tocar CXP: `selectVigentesByOrigen` sólo mira órdenes `0-3`.

**Decisiones del usuario:** créditos corrige la cuenta en la ficha del partícipe y presiona «REEMITIR
PAGO»; con la orden sin confirmar el botón anula y reemite en un paso; confirmada, tesorería la reversa
primero (tiene movimiento bancario y asiento, y la fecha del rebote la sabe tesorería); una orden
rechazada ya no revierte la devolución sola.

**H66, encontrado al diseñar:** `anularDevolucion` sólo anulaba la orden en `REGISTRADO(1)`, y desde el
2026-08-29 toda orden nace `POR_APROBAR(0)`. Anular una devolución antes de la aprobación dejaba el
aporte devuelto **y la orden viva y pagable**. `crd/sql/224` mide si ya pasó.

**Error del árbitro, registrado:** el contrato decía «por defecto la cuenta actual» en el diálogo. En un
rebote la cuenta actual es la errada. Lo vi al revisar el diff del FE, que lo había implementado fiel;
corregido a «sin preselección».

| Commit | Qué |
|---|---|
| `saaBE 3b191715` | `sql/223` — en qué punto quedó un caso (cuatro situaciones A-D) |
| `saaBE 0766c327`, `28033a8d` · `saaFE 092fc0b` | Contrato `crd/API-REEMITIR-PAGO-DEVOLUCION.md` + `sql/224` |
| `saaFE 3a768ff` | Botón, diálogo sin preselección, estado de la orden y aviso; el botón sólo aparece con el WAR nuevo |
| `saaBE 04d96086` | `POST /dvap/{id}/reemitirPago`, sincronización sin reversión automática, anulación de `POR_APROBAR` y de `PAGADA` con orden reversada, `estadoPago` en `/porEntidad` |

Verificado por el árbitro sobre el diff (las 12 reglas en orden, una sola transacción, forma de la
respuesta contra lo que consume el FE), `mvn -q compile` exit 0 y `ng build` exit 0. **Sin DDL.**
**Pendiente:** WAR + build; salida de `sql/223` (el caso real) y de `sql/224`.

**Derivado a `omen-saa-2-arb` por orden del usuario (2026-09-15):** pantalla de seguimiento de un pago;
auditoría de los estados de cuenta de titulares; número de pago visible y buscable en tesorería
(medido: la consulta de pagos de tesorería no muestra el número ni filtra por él, y el filtro de
proveedor no encuentra pagos de origen externo).

---

# ✅ 2026-09-16 — H67: el «Estado» del informe mensual de partícipes salía como número

**Reportado por el usuario** (Reportes → Créditos → Informes Mensuales, pestaña Partícipes).

**Causa, medida:** `GeneracionCPRMServiceImpl` armaba el mapa de nombres con la **PK** del catálogo
(`ESPRCDGO`) y lo consultaba con `ENTDIDST`, que desde la migración del 2026-08-11 guarda el **código
alterno** (`ESPRCDEX`). El `getOrDefault` no encontraba nada y escribía su respaldo, `"Estado N"`.
Es la trampa que ya registra el `CLAUDE.md` de la raíz, esta vez del lado de los reportes.

⭐ **Por qué costaba verlo: CESANTE salía bien.** Su PK y su alterno valen los dos 2, así que la
columna parecía funcionar a medias y se leía como un problema de datos de algunos partícipes, no como
un defecto de la generación. **Una coincidencia numérica es la mejor forma de esconder un mapeo mal
indexado** — el mismo patrón que las dos cuentas «DE 1 A 30 DÍAS» de la banda del vencimiento.

| Commit | Qué |
|---|---|
| `7da305bd` | Diseño (`reportes/CORRECCION-ESTADO-PARTICIPE-CPRM.md`) y `crd/sql/227` para los meses ya generados |
| `04c548b4` | El mapa se indexa por `ESPRCDEX`; respaldo `"SIN ESTADO (N)"` para que una próxima ruptura se lea como falla |

**Barrido del ejecutor, contrastado:** `getIdEstado()` en `rpr` se usa en cuatro puntos y los otros
tres ya comparan contra las constantes del rubro (alterno). En el resto del sistema no hay otro mapa
indexado por la PK; `CertificadoServiceImpl.nombreEstadoParticipe` ya usaba el alterno.

**Pendiente del usuario:** decidir si `sql/227` corre sobre todos los meses o sólo sobre los no
entregados — deja el estado de HOY, porque `CPRM` guarda el nombre y no el código.

---

# ✅ 2026-09-16 — H68: jubilar un partícipe reventaba con ORA-01400 · y el panel que no se cerraba

**Reportado por el usuario con el log de producción:** `POST /rest/vppc` →
`ORA-01400: no se puede realizar una inserción NULL en ("CRD"."VPPC"."VPPCVLSR")`. La transacción se
revertía entera: **el jubilado no quedaba registrado, y no quedaba ni a medias.**

**Causa:** las dos pantallas de jubilados (`proceso-pago-jubilados` — la que usaba el usuario — y
`jubilar-participe`) mandan `valorSeguro: null` cuando el campo queda vacío, y la columna es `NOT NULL`.
«Sin seguro médico» en este sistema se representa con **0**: así lo lee
`PagoPensionComplementariaServiceImpl` y así lo graba el proceso de seguro cuando no corresponde
cobrarlo (§11.1). **Rodeo que se le dio al usuario en el momento: escribir 0 en el campo.**

| Commit | Qué |
|---|---|
| `saaBE 854e0f05` | Diseño `crd/CORRECCION-VPPC-SEGURO-NULO.md` |
| `saaBE 54ff7a01` | El service normaliza el nulo a 0 (no sólo la pantalla: `/rest/vppc` es CRUD genérico, criterio de H61) + `sql/229` |
| `saaFE 5a100b1` | Las dos pantallas mandan 0; el campo arranca en `$0.00`; y el panel de asignación se cierra al guardar bien |

**Pedido del usuario en el mismo día:** que la tarjeta «2. Asignar valor de pago» se cierre tras un
guardado exitoso. Hecho sólo en la rama de éxito — **si el guardado falla, el panel queda abierto con
los datos**, para corregir sin volver a escribir todo.

**Error del árbitro, registrado:** despaché la corrección apuntando a `jubilar-participe`, que tiene el
mismo defecto pero **no es la pantalla donde el usuario lo sufrió**. Lo vi al leer el HTML buscando el
botón «Guardar asignación» de su pedido siguiente. El ejecutor ya había llegado solo a la pantalla
correcta. **Leer el nombre del endpoint en el log no alcanza para saber qué pantalla lo llamó cuando
hay dos que llaman al mismo.**

## ⚠️ Lo que queda medido y sin tocar: `numeroCuotas`

Las **dos** pantallas mandan también `numeroCuotas` nulo con el campo vacío. No se corrigió porque el
nulo ahí **probablemente es legítimo** («pago mensual fijo, sin número de cuotas»), y rellenarlo con
cero cambiaría el significado del dato. Lo decide `crd/sql/229` (obligatoriedad real de las columnas
de `CRD.VPPC` y cuántas filas tienen nulo hoy): **sólo si `VPPCNMCT` resulta `NOT NULL` hay algo que
hacer.**

⭐ **Y la lección de método que dejó el ejecutor:** el mapeo JPA **no** sirve para saber si una columna
es obligatoria — `VPPCVLSR` no declara `nullable=false` y lo es. En el repo tampoco hay DDL de `VPPC`.
La obligatoriedad real sólo la contesta `ALL_TAB_COLUMNS`.

---

# ✅ 2026-09-16 — H69: cobrar el aporte de PENSIÓN COMPLEMENTARIA · y con eso, cómo se recupera un sobrepago

**Pedido del usuario:** poder cobrar también pensión complementaria en «Cobros personales», **de forma
temporal, para registrar valores que se les pagaron de más a los jubilados**.

⭐ **Lo que el pedido resuelve sin decirlo:** H60 dejó anotado que **no existía ningún mecanismo para
recuperar un sobrepago** — se cerró como «decisión de negocio, sin herramienta». Esta es la
herramienta: el aporte positivo de tipo 23 devuelve el dinero a la cuenta individual y la corrida del
mes siguiente vuelve a tener de dónde descontar. **No toca el ancla de H46**, que la mueven sólo los
movimientos negativos.

**Decisiones del usuario (2026-09-16):** sólo cobrar, no pagar (eso va por la pantalla de jubilados);
misma cuenta individual `2.1.02.25.01` (aux1 53 de la plantilla 21); sólo a jubilados; columna «Valor
mensual» vacía.

| Commit | Qué |
|---|---|
| `saaBE abe01524`, `5648f209` · `saaFE a266bbd` | Contrato `crd/API-COBRO-APORTE-PENSION-COMPLEMENTARIA.md` |
| `saaBE 5648f209` | Tipo 23 en la lista blanca de tipos contabilizables + guarda: sólo `JUBILADO_COMPLEMENTARIO`, rechazado en el registro |
| `saaFE a771b44`, `7728595` | Fila nueva sólo para jubilados, con saldo, sin valor mensual ni cobertura; «pagar con aportes» sin cambios |

⛔ **Gate de despliegue: `crd/sql/199`.** La plantilla 21 necesita la línea `aux1 = 53`. Sin ella el
cobro se registra y **no se puede procesar**.

## Dos correcciones al contrato que salieron de los ejecutores, no del árbitro

1. **El contrato se contradecía** (§5.2 vs §5.6): resolver el tipo sólo desde los saldos del partícipe
   y, a la vez, permitir cobrar a quien no tiene saldo. El listado sale de un `GROUP BY` sobre
   `CRD.APRT`, así que **sólo trae tipos con movimientos** — y la pantalla bloqueaba el cobro ENTERO,
   no sólo esa línea. Corregido: fallback al catálogo `/rest/tpap/getAll` y, si aun así no resuelve,
   se excluye sólo la línea de pensión. **Medido para dimensionarlo:** `procesarJubilacion` crea el
   movimiento positivo de tipo 23 al jubilar, así que el hueco es de los jubilados migrados por SQL.
2. **Yo escribí que si falta la línea 53 el dinero queda adentro sin asiento. Es falso.** Todo corre
   en la misma transacción (`procesarCobro` REQUIRED + `registrarAporte` REQUIRED +
   `IncomeException(rollback = true)`): el fallo revierte el aporte, su `PagoAporte` y el enlace, y el
   cobro queda `APROBADO`, reintentable. **Fallo limpio, no descuadre.**

⭐ **Las dos las encontraron los agentes revisando lo que el árbitro les mandó** — el mismo patrón que
H34. Un contrato con dos reglas que se contradicen sólo se nota cuando alguien intenta programarlas.

---

# ⛔⛔ 2026-09-21 — H70: el «Saldo total del préstamo» de los diálogos de cobro ofrece pagar el interés futuro

**Reportado por el usuario funcional** (`LCALDERON`) en `NOVEDADES_BOTONES_SISTEMA_SAA.xlsx`, con
tres capturas del préstamo **#70828** (OSPINA GONGORA JUAN ALEXIS, C.I. 0801910514, EMERGENTE, en
mora). Textual: *«EN EL BOTON DE "PAGAR CON APORTES" Y EL VALOR QUE INDICA EN EL APARTADO DE SALDO
TOTAL DEL CREDITO NO TOMA EN CUENTA EL VALOR A LA FECHA DEL ULTIMO PAGO»*, que en precancelación el
valor sí está correcto, y que *«REGISTRO PAGO DE CUOTAS POSEE EL MISMO INCONVENIENTE»*.

## Los números de la evidencia, y qué es esa diferencia

| Pantalla | Rótulo | Valor |
|---|---|---|
| Pagar con aportes · Registrar pago de cuotas | «Saldo total del préstamo» | **$16.246,61** |
| Precancelar crédito | «Total a cobrar» al 2026-09-21 | **$13.183,86** |
| Precancelar crédito | «Intereses condonados» | **$3.062,73** |

$2.713,22 (deuda exigible, 12 cuotas) + $10.470,64 (capital futuro, 63 cuotas) = **$13.183,86**.
Y $13.183,86 + $3.062,73 = $16.246,59 ≈ **$16.246,61**.

⇒ **La diferencia ES, exactamente, el interés futuro que la precancelación condona.**

## Causa, medida

`saldoTotal` sale de `MotorPagoPrestamoServiceImpl.calcularTotalPendientePrestamo:262`, que suma
`getTotalPendiente()` de **TODAS** las cuotas pendientes — las 12 vencidas **y las 63 futuras, con
su interés**. Es la magnitud correcta para *«cuánto pagará el socio si sigue pagando cuota a cuota
hasta el final»*, y la equivocada para *«cuánto cuesta cancelar el crédito hoy»*, que es lo que el
partícipe de la novedad quería hacer con sus aportes.

**No es un error de cálculo.** Es un número correcto, mal rotulado y ofrecido en la pantalla
equivocada. Por eso precancelación da bien: usa otro camino (exigible + capital futuro, sin interés
no devengado).

⭐ **El propio código ya lo advertía y nadie lo trasladó a la pantalla.**
`PagoPensionComplementariaServiceImpl:2615`: *«⛔ NO usa `calcularTotalPendientePrestamo`: ese método
suma TODAS las cuotas pendientes, exigibles o no — exactamente lo que `buscarSiguienteCuotaConSaldo`
prepagaría si se le entregara de más»*. Y hay **precedente del 2026-09-04**, en ese mismo JavaDoc: el
usuario ya había detectado descuento de más comparando contra cobros personales. Es la tercera vez
que esta distinción muerde.

## ⛔ Lo que el reporte NO dice, y es peor que lo reportado

El diálogo **no sólo muestra** el número: **lo ofrece como botón**.
`pago-prestamo-dialog.component.ts:142` arma la sugerencia **«Saldo total · $16.246,61»** junto a
«1 cuota», «2 cuotas» y «3 cuotas».

Un operador que quiere cancelar el crédito aprieta ese atajo y **registra un cobro de $16.246,61
cuando cancelar hoy cuesta $13.183,86**: le cobra al socio **$3.062,75 de interés que no debía
pagar**, y el motor lo aplica **prepagando cuotas futuras con su interés completo** en vez de
condonarlo — que es literalmente lo que advierte el JavaDoc citado.

**Alcance:** los dos diálogos que usan `pago-prestamo-dialog` (Pagar cuotas y Pagar con aportes),
desde Cobros Personales. Precancelación **no** está afectada.

## Corrección propuesta — PENDIENTE DE DECISIÓN DEL USUARIO, sin despachar

1. **Mitigación inmediata (FE, barata):** quitar el atajo «Saldo total» de `sugerencias()`. Deja los
   múltiplos de cuota, que sí son lo que esa pantalla sabe cobrar. Corta el camino al cobro de más.
2. **Corrección de fondo (FE + BE):** el diálogo muestra **dos** magnitudes con rótulos que no se
   confundan — «Total pendiente si paga cuota a cuota» y «Cuesta cancelarlo hoy» — y cuando el
   operador quiere lo segundo, la pantalla lo **manda a precancelación**, que es el camino que
   condona bien. El segundo número ya existe: es lo que calcula el diálogo de precancelación.
3. ⛔ **Lo que NO hay que hacer:** cambiar `calcularTotalPendientePrestamo` para que reste el interés
   futuro. Tiene **tres llamadores más** (`ProcesoPagoPrestamoServiceImpl:187` y `:597`,
   `DevolucionAporteRest:529`) que dependen de la semántica actual. El defecto está en qué se
   muestra y qué se ofrece, no en el cálculo.

---

# ✅ 2026-09-21 — Jornada completa: lote U1–U5, H70, y sepelio fase 1 entregada

**Todo en `origin/main` en los dos repos.** Compilación reverificada por el árbitro en cada commit
(`mvn -q compile` y `ng build --configuration development`, los dos exit 0), no por los ejecutores
— regla 11.

| Frente | `saaBE` | `saaFE` |
|---|---|---|
| U1 seguro de jubilados · U2 guarda de `anularOperacion` | `ed081231` | — |
| U3 Pago Cuota · U4 Asignación de Seguros · U5 Plantilla general | — | `8849296` |
| H70 saldo total vs. cancelar hoy | — | `e18713a` |
| Sepelio fase 1 — backend | `d0c12eb3` · ajuste `bf995296` | — |
| Sepelio fase 1 — frontend | — | `34cd7f7` |
| SQL 230 / 231 y contratos | `cd70d6bd`, `1039aecc` | espejos |

## Lo que se cerró del lote urgente (levantado el 15-09, despachado recién hoy)

- **U1:** el CRJB se cerraba **en el reintento**, no en el intento fallido. Ahí el total daba $0,
  `generarOrdenPagoProveedorSeguro` retornaba null **sin lanzar**, y por eso sí corría el cierre.
- **U2:** guarda en `PrestamoRest`, **no** en el service — su otro llamador lo invoca con el enlace
  `EVPRCDGO` puesto.
- **U3/U4/U5:** tres pantallas que decían «guardado» sin guardar nada.

## ⭐ El patrón de la jornada: los ejecutores corrigieron al árbitro CINCO veces

No es anécdota, es el control de calidad que de hecho funciona. **Un contrato mal escrito sólo se
nota cuando alguien intenta programarlo** (mismo patrón que H34 y H69).

| # | Qué decía el árbitro | Qué estaba mal |
|---|---|---|
| 1 | U1: lanzar `IncomeException` dentro de la rama `else` del bucle | Ahí la atrapaba el `catch (Throwable)` del propio bucle: habría sumado a `conError` y seguido, dejando el total corto y el CRJB cerrado — **el defecto que se estaba corrigiendo** |
| 2 | RVSG: `anular` reversa el asiento y el aporte | La tabla **no guardaba el id del aporte**. `CRD.APRT` es append-only: buscarlo por glosa y valor sería adivinar. Se agregó `APRTCDGO` |
| 3 | RVSG: nada sobre el flag de contabilidad | Con `contabilidadActiva()` en false, `CBCR` registra igual y se saltea el asiento. Copiarlo habría metido el valor en el saldo **sin asiento** |
| 4 | RVSG: reversar con `reversionAsiento` | El método de la casa es **`anulaAsiento(id, usuario, motivo)`** (`AsientoService:270`), que deja usuario y motivo auditados |
| 5 | Despacho: «escribí los `.sql` 230 y 231» | **Va contra la regla del ejecutor Y contra el rol del árbitro.** El SQL lo escribe el árbitro. Error del árbitro, corregido |

⭐ Y el sexto, que no fue corrección sino pregunta: **«¿querés chequeo de referencia duplicada?»**
Esa pregunta destapó que **sin índice único, registrar dos veces el mismo depósito mete el valor DOS
VECES en el saldo del partícipe**.

## El precedente que se reusó en vez de reinventar

Buscando por qué `CBCR` tenía índice único de referencia apareció `crd/sql/106`: **el usuario ya
había decidido esto el 2026-09-01**, con tres reglas — la referencia no se repite, `'9'`/`'09'`
significan «sin referencia» y sí pueden repetirse, y una operación anulada libera la suya.

Se trasladó tal cual a `UX_RVSG_REFERENCIA`. ⚠️ **Con un cambio obligatorio: acá los estados que
liberan son 3 y 4, no el 5 de `CBCR`.** Ese mismo script documenta que su primera versión se
equivocó justo en ese número y dio un falso «todo limpio».

## Sepelio — dónde quedó

**Fase 1 (recepción) COMPLETA en código.** No corre hasta que el usuario ejecute `crd/sql/230` y
`231`. Registrar → contabilidad aprueba → **un** asiento D banco / H `2.3.90.90.11` → el valor entra
al saldo del partícipe.

⚠️ **Dos cosas de configuración pueden trabarla el primer día, y no son código:**
1. **La contabilidad de CRD tiene que estar activa** (rubro 237 en 1), o `aprobar` responde 409 a
   propósito — no se deja entrar dinero al saldo sin asiento.
2. **La cuenta ASOPREP donde entró el dinero tiene que tener `CNBCCBCR=1`**, o no aparece en el
   combo de la pantalla. Lo levantó el ejecutor FE.

**Fase 2 (pagar a los beneficiarios) NO EMPEZADA.** Necesita `CRD.CBBP` (autorizada, sin DDL escrito
todavía) y **un producto de pago de CXP contra `2.3.90.90.11`**, que es de otro equipo.

### Límites conocidos, medidos y aceptados

- **La carrera del chequeo de referencia:** el chequeo previo y el índice no son atómicos. Dos
  registros simultáneos con la misma referencia pueden pasar los dos y el segundo cae en ORA-00001
  (500). No se cierra con bloqueo porque **no hay fila que bloquear antes de insertar**. El dato
  queda protegido por el índice; lo feo es el 500 en esa carrera.
- **La entrada de menú no tiene `idPermiso`**: no existe el nodo en el árbol de permisos. Lo asigna
  el frente de seguridad, que es de otro equipo.
- **`anular` no tenía UI** hasta el ajuste despachado al cierre de la jornada.

## Deudas con otros equipos que siguen SIN AVISAR (esperan autorización del usuario)

1. **`omen-saa-2-arb`** — U5 tocó `plantilla-general` de `cnt`, que es compartido.
2. **`omen-saa-2-arb`** — hace falta un **producto de pago de CXP contra `2.3.90.90.11`** para la
   fase 2 de sepelio. Mismo acoplamiento que los productos 516 y 517 (P19/P20).
3. **Frente de seguridad de `laptop1`** — `docs/seguridad/ITEM7-MAPEO-BOTONES-PERMISOS.md` quedó
   desactualizado en los dos botones retirados de `plantilla-general`. **No se tocó esa carpeta.**

## ✅ 2026-09-21 — `CRD.RVSG` creada en producción y verificada

El usuario corrió `crd/sql/230` y `231`, y después el verificador `232`. **Todo OK**, con un solo
tropiezo que fue del script, no del esquema:

| Control | Resultado |
|---|---|
| Las 22 columnas, en los dos sentidos (`1.1`/`1.2`/`1.3`) | ✅ sin faltantes ni sobrantes |
| Secuencia `SQ_RVSGCDGO` (`2.1`) | ✅ |
| Índice único `UX_RVSG_REFERENCIA` (`2.2`) | ✅ — no se puede duplicar un depósito |
| Constraints (`2.3`) | ✅ `PK_RVSG`, los dos `CK_`, y **9 `SYS_C` de `NOT NULL`**, que son exactamente las 9 columnas obligatorias del diseño |
| Tipo de aporte y su cuenta en `CTAP` (`3.1`/`3.2`) | ✅ |
| Cuentas ASOPREP del combo (`4`) | ✅ |

⭐ **Las CINCO FK quedaron creadas, incluidas `FK_RVSG_ASNT` y `FK_RVSG_CNBC`, que cruzan de
esquema.** O sea, `CRD` **sí** tiene `REFERENCES` sobre `TSR.CNBC` y `CNT.ASNT` en producción.
Vale anotarlo porque en esta base ya hubo FK entre esquemas que quedaron comentadas por falta de
`GRANT` (el caso `TSR.DTCN` → `CNT.DTAS`): resulta que el permiso existe, y la próxima tabla que lo
necesite no tiene por qué asumir que no.

## ⛔ El error del árbitro en el `232`, y la trampa que destapó

El bloque `3.3` consultaba **`SCP.RUBR`** y reventó con `ORA-00942`. Las tablas de parametría son
**`SCP.PRBR`** (rubro) y **`SCP.PDTR`** (detalle): el código de 4 letras es **`PRBR`**, no `RUBR`.
Lo escribí de memoria en vez de leerlo del modelo — el mismo error que este tablero le viene
reprochando a los demás.

⚠️ **Y al corregirlo apareció algo que importa más que el nombre de la tabla:** la búsqueda **no es
por nombre, es por CÓDIGO ALTERNO**. `DetalleRubroDaoServiceImpl:268` filtra por
`t.rubro.codigoAlterno` (`PRBRALTR = 237`) y `t.codigoAlterno` (`PDTRALTR = 1`), y devuelve
`valorNumerico` (`PDTRVLRN`).

**Cero filas significa APAGADA, y eso es fácil de leer mal:** el DAO usa `getSingleResult()`, así
que si la fila no existe lanza `NoResultException`, y
`ConfiguracionContabilidadServiceImpl:61-67` lo atrapa y devuelve `false` **a propósito** («apagado
es el lado seguro»). Lo mismo si hubiera más de una fila. Es decir: **si el rubro 237/1 no existe,
`aprobar` responde 409 aunque nadie haya apagado nada**, y el mensaje va a hablar de contabilidad
inactiva sin decir que lo que falta es una fila de catálogo.

Es la misma familia de defecto que `omen-saa-2` dejó especificado y sin hacer en
`DetalleRubroDaoServiceImpl:77`: un catálogo faltante que se presenta como otra cosa.

---

# ⚡ 2026-09-21, 18:20 — SE FUE LA LUZ EN LA OMEN. Qué se perdió: NADA de código

**Sesión nueva del árbitro, retomando.** Este bloque existe porque el tablero se había cerrado a las
**16:59** (`dc2bca35`) y el equipo **siguió entregando hasta las 18:19**. Esa hora y veinte no estaba
registrada en ningún lado, y el equipo que retome después del corte sólo tiene este documento.

## Lo primero que se midió al volver, antes de tocar nada

| Control | Resultado |
|---|---|
| `git status` en `saaBE` y en `saaFE` | **limpio, cero archivos modificados** — ni míos ni de otro equipo |
| `git fetch` + comparación con `origin/main` | **al día, sin commits nuevos de la otra máquina** |
| `omen-saa-1-be` y `omen-saa-1-fe` preguntados por archivos sin reportar | **ninguno**, los dos arrancaron limpios y lo verificaron en disco, no de memoria |

⇒ **El corte se llevó el contexto del chat, no el trabajo.** Todo lo entregado estaba commiteado y
pusheado. El esquema de "el árbitro commitea apenas cierra un ítem" es exactamente lo que hizo que
un corte de luz costara cero líneas.

⭐ **Y al revés, la lección que sí deja:** lo único que se perdió fue **lo que vivía sólo en el chat**
— el registro de la última hora y media. Un ítem cerrado y commiteado sobrevive; un ítem cerrado y
no escrito, no. Por eso este bloque se escribe antes de cualquier trabajo nuevo.

## Los tres commits de `saaFE` que el tablero no registraba

| Commit | Hora | Qué |
|---|---|---|
| `5a29fff` | 17:02 | Sepelio: el tipo de valor de seguro viene **preseleccionado**, y la recepción se puede **anular** desde la UI (el ajuste que el bloque de sepelio menciona como «despachado al cierre de la jornada», sin el hash) |
| `bc54cba` | 17:26 | La entrada de menú pasa a llamarse **«Valores de Seguro»** (17 caracteres), porque «Recepción de Valores de Seguro» (31) se veía **sólo como `...`** |
| `4e74754` | 18:19 | Otras **dos** entradas de `crd` con el mismo defecto, ya en producción, + el patrón escrito para todos los módulos |

## ⛔ H71 — Un nombre de menú largo no se recorta: DESAPARECE ENTERO

**Reportado por el usuario con captura de producción.** No era sólo nuestra pantalla nueva: en
«Parametrización de Créditos» había **dos entradas más que se veían como `...`, sin una sola letra**.

| Antes | car. | Ahora | car. |
|---|---|---|---|
| Escala de Calificación de Riesgo | 33 | **Calificación de Riesgo** | 22 |
| Cuentas por Tipo de Aporte | 26 | **Cuentas por Aporte** | 18 |
| Recepción de Valores de Seguro | 31 | **Valores de Seguro** | 17 |

**El límite, medido contra las capturas, está entre 24 y 26 caracteres**: «Repote Valores Insolutos»
(24) se ve entero y «Cuentas por Tipo de Aporte» (26) no se ve nada.

⭐ **Por qué es peor que un truncado normal y por qué nadie lo vio antes:** un texto recortado a la
mitad se lee igual y alguien lo reporta. Éste **no deja ni una letra**, así que la entrada parece un
separador decorativo o un ítem deshabilitado — **no parece un nombre roto, parece que la pantalla no
existe**. Dos entradas de `crd` llevaban así vaya a saber cuánto tiempo, en producción, y el reporte
llegó recién cuando el usuario fue a buscar una pantalla nueva y tampoco la encontró.

⛔ **No se arregla en el SCSS compartido de `shared/basics/menu/`, a propósito**: lo usan los siete
módulos, y tocarlo para acomodar un ítem propio es como se rompe el menú de otro equipo sin que
nadie se entere. Se arregló en `menucreditos.component.ts`, que es nuestro.

**Dónde quedó la regla, que es lo que evita la próxima vez:** `saaFE docs/patrones/NOMBRES-DE-MENU-LATERAL.md`
(máximo 24 caracteres, apuntar a 22 — los dos últimos son margen porque el ancho cambia con la
fuente de cada entorno; a mayor profundidad hay menos espacio; trae un `grep` de una línea para
verificarlo antes de commitear). Y la línea que lo hace visible de verdad está en el **`CLAUDE.md` de
`saaFE`:96**, que se carga solo — un documento de patrones que nadie abre no sirve de nada.

## Deuda con `omen-saa-2`, ésta SÍ avisada

El intento de fondo de ellos (`saaFE eb45fd6`, 2026-09-08) puso las reglas sobre `.menu-text` **sin
tocar `.mdc-list-item__primary-text`**, que es donde Angular Material hace el recorte de verdad. Por
eso su arreglo no alcanzó. **Ya se les avisó, con autorización del usuario.** ⚠️ Si lo arreglan de
fondo, `NOMBRES-DE-MENU-LATERAL.md` hay que actualizarlo: hoy documenta un **límite de diseño**, y
pasaría a ser una cicatriz.

---

# 🔍 2026-09-21, 19:10 — H72: el mock de certificados sobrevivió tres semanas a su propio backend

**Salió de una corrección cruzada, y vale registrar cómo:** el ejecutor FE reportó en su inventario
que «`certificados-participe` corre con mock». Lo verifiqué sobre el componente, no encontré nada y
se lo corregí. **Tenía razón él y el que miró en el lugar equivocado fui yo**: el mock no está en el
componente, está en el servicio. Me lo devolvió con archivo y línea, releído en disco.

⭐ **La lección de método, que es la misma de H34, H69 y de las cinco correcciones del 21-09:** el
que reporta algo impreciso no necesariamente reporta algo falso. Si lo hubiera descartado con mi
primer grep, este frente seguía sin verse.

## Lo que hay, medido en las dos puntas

| Pieza | Dónde | Estado |
|---|---|---|
| Mock en memoria de los 6 tipos | `saaFE crd/service/certificado-participe.service.ts:22-28`, consultado en `:47,64,78,89,109` | **vivo** |
| Flag en desarrollo | `saaFE src/environments/environment.ts:22` → `mockCertificadosParticipe: true` | **mock ON** |
| Flag en producción | `saaFE src/environments/environment.prod.ts:7` → `false` | **backend real** |
| Backend `@Path("crtf")` | `saaBE ws/rest/crd/CertificadoRest.java` — 8 endpoints | **existe desde `f08e92b2`, 2026-08-30** |

**El comentario del servicio dice «el backend todavía no publica `/rest/crtf/*`». Es falso desde
hace tres semanas.** No nació mentiroso: envejeció. El backend llegó después del mock y nadie volvió
a apagar el flag.

## El contrato coincide — verificado pieza por pieza por el árbitro, no por los agentes

| FE llama | BE publica | ✓ |
|---|---|---|
| `GET /crtf/precarga/{idEntidad}/{tipo}` + `?idPrestamo&idLiquidacion` | `:145` con los dos `@QueryParam` | ✅ |
| `POST /crtf/emitir` | `:171` | ✅ |
| `GET /crtf/getByEntidad/{idEntidad}` | `:82` | ✅ |
| `GET /crtf/getByAnio/{anio}` | `:97` | ✅ |
| `GET /crtf/pdf/{id}` | `:115` | ✅ |
| `POST /crtf/anular/{id}?motivo&usuario` | `:204` con los dos `@QueryParam` | ✅ |

⚠️ **Una falsa alarma propia, anotada para que nadie la repita:** el FE tipa la respuesta de `anular`
como `ResultadoAnulacionCertificado` y el BE devuelve `Certificado`. Parecía un desajuste de forma
hasta leer `model/certificado-participe.ts:192`: **`export type ResultadoAnulacionCertificado =
Certificado`**, un alias. No hay desajuste. Un nombre de tipo distinto no es un contrato distinto.

## Por qué importa, si en producción funciona

**No hay defecto vivo en producción**: allá el flag está en `false` y pega contra el backend real,
que existe y cuyo contrato coincide. Lo que hay es peor de aguantar a largo plazo:

⇒ **La pantalla de certificados hoy SÓLO se puede probar en producción.** En desarrollo el mock
intercepta las seis llamadas y devuelve datos en memoria, así que la integración real nunca se
ejercita: **un desajuste futuro entre FE y BE no se detectaría hasta que un usuario emita un
certificado de verdad.** Es la misma familia que **P22** (calificación de riesgo: «código listo de
las dos puntas, nunca probado contra producción»).

## Lo que falta antes de apagarlo, y no está hecho

Verifiqué rutas, path params, query params y la forma de la respuesta de `anular`. **NO** comparé
campo por campo los tres DTOs (`PrecargaCertificado`, `SolicitudEmisionCertificado`,
`ResultadoEmisionCertificado`), que existen con el mismo nombre en los dos repos — eso es lo que
hay que medir **antes** de apagar el flag, porque es justo lo que el mock viene tapando.

**Corrección propuesta, sin despachar (FE puro, barata):** apagar `mockCertificadosParticipe` en
`environment.ts`, borrar el mock del servicio y corregir el comentario. Queda pendiente de la
decisión del usuario y de la comparación de DTOs de arriba.

---

# 🚀 2026-09-21, 19:20 — WAR DESPLEGADO. Qué quedó vivo y qué NO viaja en el WAR

**Dato del usuario, textual: «ya desplegue el war».** Al momento de decirlo `origin/main` estaba en
`saaBE 3e32aa35` (el último commit con **código** de este equipo es `bf995296`) y
`saaFE 4e74754`.

## ⛔ Lo primero, porque es lo que se olvida: el WAR NO LLEVA EL FRONTEND

Son dos artefactos y dos despliegues distintos. **De los frentes cerrados el 21-09, la mitad es
frontend puro y no se activa con el WAR**, por más que el backend esté arriba.

| Vive con el WAR (backend) | Necesita el build de `saaFE` |
|---|---|
| **U1** el seguro de jubilados se paga aunque la corrida falle a medias | **U3** «Pago Cuota» retirado del menú y la ruta |
| **U2** `anularOperacion` rechaza un evento enlazado a un cobro | **U4** «Asignación de Seguros» retirada |
| **H65** `POST /dvap/{id}/reemitirPago` y la sincronización sin reversión automática | **U5** «Plantilla general» (`cnt`) deja de fingir que guarda |
| **H66** anular una devolución anula también la orden `POR_APROBAR` | ⛔ **H70** el atajo «Saldo total» retirado del diálogo de cobro |
| **H67** el Estado de CPRM se resuelve por el código alterno | **Sepelio fase 1 — la pantalla** de recepción, aprobación y anulación |
| **H68** el service normaliza `valorSeguro` nulo a 0 | **H68** las dos pantallas mandan 0 y el panel se cierra al guardar |
| **H69** tipo 23 contabilizable, sólo a jubilados | **H71** los tres nombres de menú que se veían como `...` |
| **Sepelio fase 1 — el backend** de `CRD.RVSG` | **H65** el botón «REEMITIR PAGO» y su diálogo |

⚠️ **Las dos consecuencias que importan si el build del FE no subió:**

1. **H70 sigue vivo en producción.** El atajo «Saldo total · $16.246,61» sigue ahí, y quien lo
   aprieta para cancelar un crédito **le cobra al socio ~$3.062 de interés futuro que no debía
   pagar**. La corrección es 100 % frontend: el WAR no la trae.
2. **Sepelio fase 1 queda inservible aunque esté completo.** La tabla `CRD.RVSG` está creada y
   verificada, el backend está desplegado… y **no hay pantalla desde donde registrar nada**. Las
   tres piezas tienen que estar para que el frente exista.

## Lo que el WAR levanta, y queda habilitado desde ahora

- **La corrida de jubilados ya no se rompe entera si falla el pago al proveedor de seguro** (U1).
- **«Anular» en Historial de operaciones ya no descuadra un cobro CBCR** (U2): ahora responde con un
  código de negocio que remite a `/cbcr/{id}/reversar`.
- **Se puede cobrar pensión complementaria a un jubilado** (H69) ⇒ con eso existe, por fin, la
  herramienta para **recuperar un sobrepago**, que H60 había cerrado como «sin herramienta».
- **Jubilar un partícipe con el seguro vacío ya no revienta con ORA-01400** (H68).
- **Reemitir el pago de una devolución rebotada** (H65) y la anulación que dejaba viva la orden (H66).

## ⛔ Los dos gates de configuración del primer caso de sepelio — NO son código

Cuando se registre la primera recepción, si falla va a ser por acá y no por el WAR:

1. **La contabilidad de CRD tiene que estar activa** — rubro `237` / detalle `1` en `SCP.PRBR`/`SCP.PDTR`,
   buscado **por código alterno** (`PRBRALTR=237`, `PDTRALTR=1`), con `PDTRVLRN = 1`. Si esa fila
   **no existe**, `aprobar` responde **409 igual que si alguien la hubiera apagado**, y el mensaje va
   a hablar de contabilidad inactiva sin decir que lo que falta es una fila de catálogo.
2. **La cuenta bancaria ASOPREP donde entró el dinero tiene que tener `CNBCCBCR = 1`**, o no aparece
   en el combo de la pantalla.

## Lo que sigue sin verificar, ahora que el WAR está arriba

- **`crd/sql/218`** — sigue sin constancia de ejecución. Es de solo lectura y su bloque 3 dice si
  quedó algún período de `PGPC` **sin cabecera `CRJB`, o sea pagable dos veces**.
- **El verificador entidad-vs-esquema de `crd` es del 2026-08-30 y cubre 98 entidades; hoy
  `model/crd` tiene 107.** No cubre `CRJB`, `CFCR`, `ESCR`, `USAP` ni `RVSG` (ésta tiene su propio
  `232`, ya corrido y limpio). Tal como está da una tranquilidad que no corresponde — regla 9.
- **P22, calificación de riesgo**: código de las dos puntas, tablas creadas, **nunca probado contra
  producción**. Sigue igual desde el 09-09.

---

# ✅ 2026-09-22 — SEPELIO FASE 2a ENTREGADA: los beneficiarios del partícipe

**El usuario autorizó `CRD.CBBP` e informó que `CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql` ya
corrió** ⇒ se cerraron **S1** y **S4** del diseño.

## Decisión de árbitro: la fase 2 se partió en dos

El pago depende de un producto de CXP que es de otro equipo. Esperarlo habría dejado el frente
entero parado, así que:

| Fase | Qué | Estado |
|---|---|---|
| **2a — Beneficiarios** | cargar quién cobra, con cuenta, porcentaje y certificado | ✅ **entregada** |
| **2b — Entrega** | repartir el valor en N órdenes | ⛔ bloqueada (ver abajo) |

**La 2a no paga, no asienta y no llama a CXP.** Por eso se puede desplegar sola.

| Commit | Qué |
|---|---|
| `saaBE a9d05751` | `sql/233` (DDL de `CBBP`), contrato `crd/API-BENEFICIARIOS-PARTICIPE.md`, reserva actualizada |
| `saaBE 8c953e78` | Las 7 capas + `@Path("cbbp")` |
| `saaFE edcecc0`, `aa853b3` | Espejo del contrato y su corrección |
| `saaFE 58a1580` | La pestaña «Beneficiarios» dentro de la ficha del partícipe |

**Verificado por el árbitro, no por los ejecutores (regla 11):** las 11 columnas de la entidad
contra el DDL una por una (coinciden exactas), `mvn -q compile` exit 0, `ng build` exit 0, el
`disabled` del botón que no mira el acumulado, SCSS con clases locales y cero `::ng-deep`, y
`ws-crd.ts` con una sola línea agregada.

## ⛔ H73 — Un `forkJoin` muere entero, y con él una pantalla que no es la suya

**Encontrado por el árbitro revisando el diff, antes de commitear.**

`cargarDatosIniciales()` de la ficha del partícipe agregó los beneficiarios al `forkJoin` que trae
entidad, partícipe, cónyuge, referencias y cuentas bancarias. El servicio nuevo **propagaba** el
error (`throwError`), mientras su vecino `CuentaBancariaParticipeService` devuelve **`of(null)`**.

⇒ **Un error de `/cbbp/porEntidad` dejaba la ficha completa sin cargar NADA.** No la pestaña nueva:
**la pantalla entera**, incluidas las cinco cosas que hoy funcionan bien.

⭐ **Y no era hipotético.** `CRD.CBBP` no existe hasta que se corra el `233`, así que ese endpoint
**falla seguro** si el WAR llega antes que el script — que es literalmente el **H58** de este mismo
tablero («el WAR subió sin su DDL»). El defecto se habría manifestado como «la ficha del partícipe
no abre», sin ninguna pista de que la causa era una pestaña nueva.

**Regla que queda:** *una llamada nueva dentro de un `forkJoin` existente no puede propagar su
error.* El `catchError` de la carga inicial no es el mismo que el de guardar: en la carga se absorbe
(`of([])`), al guardar se propaga para mostrar el mensaje del backend. Los dos conviven en el mismo
servicio, a propósito.

## ⛔ Error del árbitro: escribí un control MÁS LAXO que el código que pretendía controlar

El contrato decía que el tipo de adjunto se resuelve con `LIKE '%CERTIFICADO%BANCARIO%'`. **Falso:**
`TipoAdjuntoDaoServiceImpl.selectByNombre:25-31` hace `UPPER(t.nombre) = UPPER(:nombre)` **y** filtra
por estado activo. Lo levantó el ejecutor BE **programando contra el código en vez de contra mi
prosa** — que es exactamente para lo que sirve que lo lean.

⭐ **Lo caro es lo que destapó:** el bloque `0.5` del `233` —mi script de control— usaba el mismo
`LIKE`. Habría devuelto «1 fila, todo bien» con un nombre como `CERTIFICADO BANCARIO DIGITALIZADO`,
o con la fila **inactiva**, y el certificado habría fallado igual el primer día. **Un control más
laxo que el código que verifica no verifica nada: da tranquilidad falsa**, que es peor que no tener
control. Corregido a igualdad exacta + estado, con un `0.5b` de diagnóstico para cuando dé cero.

Es la tercera vez que un ejecutor corrige al árbitro por leer el código donde el árbitro escribió de
memoria (H34, H69, y el bloque `3.3` del `232`).

## Dos precisiones del contrato que salieron al implementar

1. **El estado es `1` activo / `2` inactivo** — rubro `EstadoCuentasBancarias`, no el genérico
   `Estado`, cuyo `INACTIVO` vale **0** y habría violado el `CK_CBBP_ESTADO`.
2. **El alta responde 400, no 404**, si el partícipe o el banco no existen: la tabla de códigos no
   enumeraba un 404 y el ejecutor respetó el listado en vez de inventarlo. El 404 sólo vive en el `PUT`.

## Lo que falta

- ⛔ **Correr `crd/sql/233`, y ANTES del próximo WAR.** Sin la tabla, la pestaña no funciona; y sin
  el arreglo de H73 desplegado, además se llevaría puesta la ficha entera.
- **Fase 2b bloqueada por dos cosas:** el **producto de pago de CXP contra `2.3.90.90.11`**
  (`omen-saa-2`, el aviso sigue esperando autorización del usuario) y **la decisión del asiento de
  reclasificación**: el `231` dejó `CTAPPLNL = CTAPPLNP`, con lo que la reclasificación saldría
  **neutra**, así que hay que **medir si esa rama se invoca** antes de elegir entre agregar
  `CTAPRCLS` (dos asientos exactos) o dejar el tercero neutro (cero código). **Sin medir, no se
  despacha.**
- **Alcance que se amplía solo:** la devolución de aportes normal también debe pagar a beneficiarios
  cuando el partícipe está fallecido (decisión del usuario). Eso toca `registrarDevolucion`, que ya
  está en producción: la 2b modifica código vivo, no sólo agrega código nuevo.

---

# ⛔⛔ 2026-09-22 — H74: los 410 certificados bancarios se volvieron invisibles por UNA fila de catálogo

**Reportado por el usuario:** *«los certificados de las cuentas bancarias de los partícipes que ya
se habían cargado están saliendo como que no se hubieran cargado»*.

## La cadena, medida en producción

`CRD.TPDJ` tenía **dos filas activas** llamadas `CERTIFICADO BANCARIO`: la **4** y la **38**.

El backend resuelve ese tipo **por nombre** y —desde la corrección del 2026-09-04— exige
**exactamente una** fila activa. Con dos lanza `ERR_TIPO_ADJUNTO_NO_CONFIGURADO`. Y
`obtenerCertificado:239` llama a ese resolutor **antes** de buscar el adjunto ⇒ **falla para TODAS
las cuentas**, y la pantalla las muestra todas sin certificado.

| Tipo | Adjuntos | Qué era |
|---|---|---|
| **4** | **410** (394 activos, **273 cuentas**), desde 2025-02-05 **hasta el 21-09 16:09** | el bueno |
| 38 | **0** | lo creó el `INSERT` del 22-09 |
| 37 | — | el duplicado del 04-09, ya inactivo desde `sql/193` |

⭐ **El `HASTA` del 4 es la prueba que cierra el diagnóstico sin ambigüedad:** el sistema venía
grabando certificados con el 4 hasta el día anterior. **Nada que reapuntar, nada que migrar** — ni
un adjunto se tocó. Bastó desactivar el 38.

## ⛔ La causa de fondo, y es la TERCERA vez

`CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql` traía un control previo que dice, textual:
*«esperado: 0 filas. Si devuelve algo, NO correr el INSERT — ya existe»*… seguido de un **`INSERT`
plano, sin guarda**. Corrido de corrido, duplica.

- **2026-09-04** → creó el id 37 (`sql/193`).
- **2026-09-22** → creó el id 38 (`sql/234`).

**Las dos veces el control estaba bien escrito. Las dos veces falló que alguien lo leyera.**

⇒ **La lección no es «leer mejor».** Un script que escribe no puede depender de que una persona
interprete un comentario. **Mientras la base lo permita, va a volver a pasar.**

## Error del árbitro, registrado

**Yo disparé este incidente.** Le pedí al usuario que confirmara si ese script había corrido (ítem
S4 de sepelio) **sin advertirle que su `INSERT` no tenía guarda**. Lo corrió, como correspondía a lo
que le pedí, y rompió los certificados de 273 cuentas.

## Las tres correcciones, en capas

| Script | Qué |
|---|---|
| `sql/234` | El diagnóstico (sin ids quemados) y el `UPDATE` que desactiva el 38 |
| `CARGA-TIPO-ADJUNTO-...` | El `INSERT` pasa a `INSERT ... WHERE NOT EXISTS`. Correrlo mil veces ya no duplica |
| `sql/235` | ⭐ **`UX_TPDJ_NOMBRE_ACTIVO`**: índice único **funcional y parcial** sobre `UPPER(TRIM(nombre))` **sólo para activas** |

**Por qué el índice y no sólo el script arreglado:** existe **`POST /rest/tpdj`**
(`TipoAdjuntoRest:84`, CRUD genérico **sin validación de nombre repetido**) y su pantalla en
Parametrización de Créditos. **Cualquier operador puede crear otro «CERTIFICADO BANCARIO» desde la
UI y romper 273 cuentas sin enterarse.** Arreglar el script cerraba un camino de tres.

**Detalles del índice que no son adorno:**
- `TRIM` porque un espacio al final burlaría el índice **y el backend igual no encontraría la fila**
  (su comparación es exacta): una fila que parece cargada y no funciona, peor que el duplicado.
- **Parcial (`CASE WHEN estado = 1`)** porque la expresión da `NULL` para las inactivas y Oracle no
  indexa claves todo-`NULL`: el 37 y el 38 conviven con el 4 sin conflicto. Un `UNIQUE` sobre el
  nombre a secas habría obligado a **borrar** tipos con adjuntos colgando.

Mismo criterio que `UX_RVSG_REFERENCIA`: **el chequeo previo informa, el índice impide.**

## ⛔ H75 — El WAR subió otra vez sin su DDL (`ORA-00942` sobre `CRD.CBBP`)

Mismo día, con la fase 2a recién entregada: el WAR se desplegó **antes** de correr el `233`, así que
Hibernate consultaba una tabla inexistente.

⭐ **Y acá se cobró sola la corrección de H73.** Como `porEntidad` ya devolvía lista vacía en vez de
propagar, **la ficha del partícipe siguió abriendo** y el ORA-00942 quedó como ruido en el log en
lugar de dejar una pantalla muerta para todos los partícipes. Ese `catchError` se escribió 24 h
antes justamente para este escenario, y el escenario ocurrió.

⚠️ **Precisión que evitó perseguir el defecto equivocado:** el error apareció «al procesar seguros
médicos», pero **el proceso de seguros no consulta beneficiarios**. Verificado en las dos puntas:
ninguna clase del backend fuera de las nuestras usa `CuentaBancariaBeneficiario`, y en el frontend
sólo la usa `entidad-participe-info`. El ORA-00942 salía de **abrir la ficha de un partícipe**. Lo
que sí rompía seguros médicos era H74: el pago a jubilados depende del certificado.

## Estado al cierre

El usuario corrió **234 (con el `UPDATE`), 233 y 235**. Queda `sql/236` —solo lectura— que da el
veredicto de los seis controles en una fila, **y la confirmación en pantalla, que ningún SELECT
puede dar**: que los certificados vuelvan a verse.

## Lo que queda abierto de este incidente

1. **`TipoAdjuntoRest` debería responder 409** con un mensaje claro ante un nombre repetido, como ya
   hace `/rest/cbbp`. Hoy el índice lo rechaza con un ORA-00001 crudo: **feo pero seguro**.
2. ⚠️ **Hay 21 puntos en el sistema que resuelven catálogos por nombre.** Este patrón —catálogo
   duplicable + resolución por nombre— **no es exclusivo de `TPDJ`**. Nadie barrió los otros.

---

# 📌 2026-09-22, tarde — La jornada del primer despliegue: cinco defectos que sólo aparecen EJECUTANDO

**El hilo conductor del día, y es la lección que más vale:** se desplegó el WAR después de ocho días
de trabajo acumulado, y **todo lo que se rompió estaba escrito hacía días**. Compilaba en las dos
puntas. Ninguno de estos defectos se ve compilando; todos aparecieron con un usuario adelante.

| # | Qué | Cómo se manifestó |
|---|---|---|
| **H76** | Los dos procesos mensuales de jubilados esperaban `@QueryParam` y el frontend manda el cuerpo | «Debe indicar idEmpresa», `Periodo: null/null` |
| **H77** | La cabecera de corrida nacía con los estados en `NULL` | `ORA-01400` sobre `CRJBESPN` |
| **H78** | El seguro médico calcula $0 donde la pantalla promete $450,40 | **abierto**, ver abajo |
| **H79** | El certificado del beneficiario iba a pisar el de las cuentas bancarias | encontrado antes de que mordiera |
| H74/H75 | Certificados invisibles y `ORA-00942` de `CBBP` | ya registrados arriba |

## ⛔ H76 — El backend estaba mal contra su propio contrato

`PagoPensionComplementariaRest` declaraba `idEmpresa`/`anio`/`mes`/`usuario` como `@QueryParam` en
`/seguro/generar` y `/pensiones/generar`. El frontend los manda **en el cuerpo**, que es lo que
`API-DOS-PROCESOS-MENSUALES-JUBILADOS.md` §4.1 pidió siempre.

⭐ **Por qué nadie lo vio:** el frente figuraba terminado hacía días, «esperando WAR». **Las dos
puntas compilaban y ninguna se había ejecutado nunca contra la otra.** Es el mismo riesgo que sigue
abierto en **P22**.

Y `/pensiones/generar` tenía el **mismo** defecto, sin haber fallado todavía porque se corre a fin de
mes. **Se corrigieron los dos juntos**, que es lo que evitó que reapareciera con las pensiones de
septiembre. `saaBE 3d87e029`.

⚠️ Límite anotado: con `@Consumes(APPLICATION_JSON)`, una llamada **sin** `Content-Type` puede dar
415 antes de mirar los query params. El respaldo sirve para un cliente que igual manda JSON, no para
un `curl` pelado.

## ⛔ H77 — Un `DEFAULT 0 NOT NULL` que no defiende de nada

`CRJBESPN` tiene `DEFAULT 0 NOT NULL` en el DDL **y aun así** falló con `ORA-01400`.

⭐ **La razón, que no es obvia y conviene no volver a aprender:** Hibernate **incluye la columna en
el INSERT con `NULL` explícito**, y un `DEFAULT` de Oracle **sólo actúa cuando la columna se OMITE**.
El valor por defecto tiene que vivir en el objeto, no en la tabla. Corregido inicializando los dos
estados en la entidad — y no en el service — para cubrir cualquier creador, incluido el CRUD
genérico por REST (criterio de H61). `saaBE 175e7cbb`.

**Corrección del ejecutor al árbitro:** mi despacho decía que **dos** métodos creaban esa cabecera.
Falso: uno está dentro de `remove()` y esa instancia es una plantilla para borrar, nunca se persiste.
Hay **un solo** punto de inserción.

## ⛔ H78 — ABIERTO: el seguro médico de 9/2026 da $0 y la pantalla promete $450,40

**Medido, y con una hipótesis fuerte sin confirmar.**

| Medición | Resultado |
|---|---|
| `CRD.PGPC` de 9/2026 | **cero filas** ⇒ no se fijó nada, ni siquiera ceros ⇒ **H46 DESCARTADO** |
| Configuraciones `VPPC` activas | **190** |
| Jubilados en el padrón (`ENTDIDST = 3`) | **182** |
| Con seguro configurado (`VPPCVLSR > 0`) | **8**, y suman exactamente **$450,40** |
| Esos ocho | **todos en el padrón**, todos **con saldo** ($12.162 a $31.919), último movimiento 2026-08-31 |

⇒ **Los $450,40 son de OCHO jubilados, no de los 139 «aptos» que muestra la pantalla.**

⭐ **La hipótesis, que sale de una resta: 190 − 182 = 8.** `generarSeguroIndividual:998` llama a
`unicaActiva`, que **lanza** si la entidad tiene **más de una VPPC activa**. Esa excepción la atrapa
el catch del bucle, que **suma a `conError` y sigue**. Si los ocho únicos que aportan al total lanzan
antes de fijar nada, el total queda en $0 **sin que el proceso falle** — y explica que no haya ni una
fila en `PGPC`. **Lo confirma `crd/sql/239`, pendiente de correr.**

⚠️ **Y lo tercero, que es lo peor y no depende de la hipótesis:** ocho jubilados sin cobrar su seguro
**no detuvieron nada ni avisaron en pantalla**. El proceso informó «$0» como un resultado normal.
**Un error por jubilado no puede terminar en un contador que nadie mira.**

## ⛔ H79 — Dos tablas distintas compartiendo la clave de sus adjuntos

Encontrado por el árbitro **al ir a implementar** la descarga del certificado que pidió el usuario.

`selectByReferenciaYTipo` resuelve los adjuntos **sólo** por `(ADJNIDRF, TPDJCDGO)`, y `CRD.ADJN`
**no tiene ninguna columna que diga de qué tabla viene la referencia**. La fase 2a guardaba el
certificado del beneficiario con **el mismo tipo** que usa `CNBP` ⇒ el beneficiario 1 y la cuenta
bancaria 1 eran indistinguibles.

**Culpa del árbitro:** el contrato dijo «igual que `crearConCertificado`» sin ver que compartir el
tipo los vuelve indistinguibles.

**Corregido con un tipo propio** (`CERTIFICADO BANCARIO BENEFICIARIO`, id 58, `sql/240`): una fila de
catálogo, cero cambios de estructura, cero líneas tocadas en la búsqueda que usa todo el módulo. Se
descartó agregar una columna de origen a `ADJN` por ser DDL sobre una tabla de varios frentes.

### El control que se saltó, y por qué esta vez salió barato

El bloque 0.2 del `240` decía **«si hay beneficiarios cargados, PARAR»**. Devolvió **10** y el script
siguió. Los diez certificados quedaron con el tipo viejo.

⭐ **Lo que hizo la reparación posible y determinista:** las dos rutinas guardan en **carpetas
distintas** (`crd/certificados-bancarios` vs `crd/certificados-beneficiarios`), así que `ADJNURLA`
los distingue **sin adivinar por fecha ni por código**. `sql/241` los reapuntó.

**Y la alarma que levanté no se materializó:** medido, **no existe ninguna cuenta bancaria con código
1 a 10**, así que ningún partícipe llegó a ver un certificado ajeno. Queda escrito con la medición al
lado. ⚠️ Pero `CNBP` tiene cientos de filas y `CBBP` suma de a una desde el 1: **la colisión era
cuestión de tiempo, no de suerte.**

## Lo entregado esta tarde

| Commit | Qué |
|---|---|
| `saaBE 3d87e029` | H76 — los dos procesos aceptan el cuerpo |
| `saaBE 175e7cbb` | H77 — los estados de la cabecera nacen en 0 |
| `saaBE bbe5807f` · `saaFE 21d923d` | Beneficiarios: descargar certificado, **borrado real**, cédula editable, tipo de adjunto propio |
| `saaBE 27014565`, `fb3a2b7f`, `329a8835` | `sql/240` y `241` — el tipo propio y la migración de los diez |
| `saaBE 69418e84`, `bdb327c9`, `60cdaa02` | `sql/237`, `238`, `239` — la investigación de H78 |

**Dos decisiones del usuario que cambiaron el contrato de beneficiarios (no re-litigar):** un
beneficiario **se puede eliminar**, no sólo inactivar (borra también su certificado y el archivo); y
**la cédula se puede corregir** con el `PUT`, con el 409 de duplicado corriendo también al editar.

## Errores del árbitro de esta jornada, los cuatro

1. **Inventé nombres de columna dos veces**: `APRTFCMV`/`APRTVLMV` en el `237` (los reales son
   `APRTVLRR`/`APRTFCTR`). Es la segunda vez en la serie después de `SCP.RUBR` por `SCP.PRBR`.
2. **Comentarios intercalados dentro de un `SELECT`** en el `236`: DBeaver normaliza los saltos de
   línea y el primer `--` se comió la sentencia entera (`ORA-00936`).
3. **El contrato de beneficiarios** mandó a compartir el tipo de adjunto (H79).
4. **Dije que dos métodos creaban la cabecera** de corrida cuando era uno (H77).

⭐ Los cuatro los destapó **ejecutar**: dos el usuario corriendo los scripts y dos el ejecutor
leyendo el código en vez de mi prosa.

---

# ✅ 2026-09-22, 16:06 — EL SEGURO MÉDICO DE 9/2026 SE GENERÓ: $450,40, orden 478

**Cerrado el frente que estuvo bloqueado toda la jornada.** Resultado final, textual de la pantalla:
*«180 jubilados nuevos y 0 que ya tenían el seguro fijado, $450.4 en total hacia el proveedor
(orden 478), 2 con error, de 182 evaluados»*. Cabecera verificada en la base: `CRJBESSG = 1`,
`CRJBVLSG = 450.4`, `CRJBIDSG = 478`.

## ⭐ Eran CUATRO defectos encadenados, y cada uno tapaba al siguiente

Esto es lo que hay que entender de esta jornada: **no era un defecto, era una pila.** Cada corrección
destapaba el próximo, y ninguno se veía compilando.

| # | Defecto | Cómo se manifestaba | Corrección |
|---|---|---|---|
| 1 | Los parámetros iban por `@QueryParam` y el frontend manda el cuerpo (**H76**) | «Debe indicar idEmpresa», `Periodo: null/null` | `saaBE 3d87e029` |
| 2 | La cabecera nacía con los estados en `NULL` (**H77**) | `ORA-01400` sobre `CRJBESPN` | `saaBE 175e7cbb` |
| 3 | El seguro no completaba el valor total del pago | `ORA-01400` sobre `PGPCVLRR`, **182 de 182** | `saaBE ec1a5767` |
| 4 | `CK_PGPC_VLRR` exige `> 0` y el diseño fija 0 a propósito | habría sido `ORA-02290` en **174 de 182** | `crd/sql/243` |

⭐ **El cuarto nunca llegó a manifestarse** porque el ejecutor BE leyó el DDL entero en vez de sólo la
columna que yo le había señalado. Sin eso: WAR desplegado, corrida reabierta, reintento… y **falla de
nuevo para 174 de 182**, con una vuelta completa de diagnóstico encima.

## ⛔ El defecto de diseño que convirtió un error de una línea en un bloqueo del período

Con los 182 fallando, el bucle **atrapó cada excepción, la sumó a `conError` y siguió** — y al
terminar **cerró la cabecera igual**, con total $0. A partir de ahí la guarda respondía *«ya se
generó, no se puede generar dos veces»* y **el período quedó trabado**, sin haber pagado un centavo.

Hizo falta `crd/sql/242` para reabrirlo a mano.

⇒ **El bucle no debería cerrar la corrida si no generó ni un solo seguro.** Es la misma familia de
**H78** y **H46**: errores que mueren en un contador que nadie mira. **Pendiente de decisión del
usuario, sin despachar.**

## La secuencia que lo resolvió, y el orden era obligatorio

1. `sql/243` — relajar el `CHECK` a `>= 0`.
2. WAR con el fix de `PGPCVLRR`.
3. `sql/242` — reabrir la corrida (medido antes: cerrada con total 0, sin filas en `PGPC`, sin
   movimientos negativos de aporte).
4. Reintentar desde la pantalla.

⚠️ **Precisión sobre el `242`, y es una corrección de criterio mía:** su bloque 3 decía «parar si hay
movimientos de tipo 23 en septiembre». Devolvió **2, pero ninguno negativo** y los dos del **14-09**,
ocho días antes de los intentos. **El ancla se resuelve por el último movimiento NEGATIVO**, así que
dos positivos no la mueven. **El umbral del control era más grueso que la regla que pretendía
proteger** — la lectura fina la daba la columna de negativos, no el conteo de filas.

## ⭐ CORRECCIÓN DEL DIAGNÓSTICO DE H78 — la hipótesis era equivocada

El `$0` de los intentos anteriores **no venía del ancla envenenada ni de las `VPPC` duplicadas**.
Venía de que **todos** los jubilados fallaban al insertar su fila. Lo probó la propia pantalla por dos
lados: *«0 jubilado(s) con el seguro topado por saldo, sin ancla o al día»* (el ancla no descartó a
nadie) y *«182 con error»* (el total $0 se explica entero por los errores).

⇒ **`crd/sql/239` pasa de urgente a informativo.** Pero **sigue teniendo sentido correrlo**: de 182
evaluados quedaron **2 con error**, y la resta 190 configuraciones − 182 jubilados sigue sin cerrar.
Ese script mide exactamente eso.

## Lo que queda abierto de este frente

1. **Los 2 jubilados que fallaron** — falta el motivo textual del log. Sospecha: sin `VPPC` activa, o
   con dos (lo mide el `239`).
2. **El cierre del bucle con errores** — decisión del usuario.
3. **Las pensiones de fin de mes** todavía no se generaron (`CRJBESPN = 0`), que es lo correcto: son
   de fin de mes. Ojo que **ese proceso tenía el mismo defecto de los `@QueryParam`** y ya está
   corregido en el mismo WAR.
