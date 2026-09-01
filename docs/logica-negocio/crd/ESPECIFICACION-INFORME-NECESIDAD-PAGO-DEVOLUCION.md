# Especificación — Informe de necesidad de pago (devolución individual)

**Fecha:** 2026-09-01 · **Equipo:** CRD · EQUIPO B (`omen-saa-1`) · **Escrito por:** el árbitro
**Origen:** formato en Word `INFORME DE NECESIDAD DE PAGO CESANTIAS 27.docx`, entregado por el usuario.
**Decisiones del usuario:** 2026-09-01, §1.

> **Qué se construye.** El Word es un informe **grupal**: un cronograma con 19 partícipes, una fila
> cada uno. Se necesita el mismo documento **para un solo partícipe**, imprimible desde la pantalla
> de devolución de aportes, con el desglose de lo que se le devuelve.

---

## 1. Decisiones del usuario — 2026-09-01

| # | Decisión |
|---|---|
| **U1** | Lo construye el **equipo B**. El resto de equipos se cerró; no hay que coordinar con nadie |
| **U2** | La tabla muestra **desglose por cuenta + el cruce contra préstamos + el neto**, no una fila resumen |
| **U3** | El **número de informe lo digita** el operador. El **firmante va fijo** en la plantilla |
| **U4** | Los textos normativos van **fijos** en la plantilla. **Observaciones** es lo único que se digita |
| **U5** | El cruce se reconstruye de **los aportes consumidos en pagos de préstamo**, más la **deuda que queda vigente** |
| **U6** | Se imprime **al registrar** la devolución **y** desde el **histórico**, para reimprimir una anterior |

---

## 2. ⛔ El hallazgo que define el diseño: el neto del Word no existe en el sistema

**Verificado contra el código el 2026-09-01.** En el sistema, cruzar y devolver son **dos
operaciones separadas, y no quedan enlazadas entre sí**:

| Operación | Pantalla | Endpoint | Qué deja |
|---|---|---|---|
| **Cruce contra préstamos** | `forms/cruce-de-valores` | `POST /rest/prst/pagarConAportes` | Consume aportes (movimientos negativos) y genera `PagoPrestamo` |
| **Devolución en efectivo** | `forms/devolucion-aportes` | `POST /rest/dvap/registrar` | `CRD.DVAP` + `CRD.DDVA` con el desglose por tipo de aporte |

**No hay ninguna FK entre las dos**, y la devolución **ni siquiera valida la deuda**:
`GET /rest/dvap/deudaVigente/{idEntidad}` existe, pero por decisión explícita del 2026-08-24 es
**un aviso en el diálogo de confirmación**, no un bloqueo ni un descuento.

O sea: el *«menos sus haberes correspondientes»* del Word es real como práctica operativa —se cruza
primero, se devuelve el remanente— pero **el sistema no guarda ese neto en ninguna columna**.
`DVAPVLRR` es la suma de lo que el operador eligió devolver, no un resultado de restar préstamos.

### Cómo se reconstruye (decisión U5)

**Lo que salva el diseño es una columna que sí existe:** `CRD.APRT.APRTTPMV` (rubro 235,
`CrdTipoMovimientoAporte`) marca la **naturaleza** de cada movimiento de aporte:

| Valor | Constante | Qué es |
|---|---|---|
| `3` | `DEVOLUCION` | El movimiento que genera la devolución. Además `APRT.APRTIDDV` **es FK a `DVAP.DVAPCDGO`** |
| `4` | `PAGO_PRESTAMO` | **Aportes consumidos para pagar un préstamo** — exactamente el cruce que el informe tiene que mostrar |

Así que el informe se arma con tres bloques, **los tres de datos reales**:

| Bloque | Fuente | Qué afirma |
|---|---|---|
| **A — Lo que se devuelve** | `CRD.DDVA` de esta devolución, por tipo de aporte | Exacto. Es el objeto del informe |
| **B — Aportes ya aplicados a préstamos** | `CRD.APRT` con `APRTTPMV = 4` de ese partícipe, desde `P_FECHA_DESDE` | Lo que el socio ya usó para pagar. **No dice que se dedujo de esta devolución** — dice qué se cruzó |
| **C — Deuda vigente que queda** | `CRD.PRST` del partícipe con `PRSTIDST NOT IN (3,4,5)` | Lo que sigue debiendo al momento del informe |

> ⚠️ **El encabezado de cada bloque tiene que decir qué es, sin sugerir una aritmética que el
> sistema no hizo.** El bloque B **no** es "deducciones de esta devolución": es el histórico de
> cruces del partícipe en el rango consultado. Si el operador cruzó en otra fecha, el bloque abarca
> más de lo que este informe cubre — por eso `P_FECHA_DESDE` es un parámetro y no una constante.
> Un informe que sume A + B y lo presente como "neto" estaría **inventando un cálculo**, y es
> justamente lo que no hay que hacer.

---

## 3. ✅ Cómo se genera el `.jasper` — resuelto, y NO hace falta Jaspersoft Studio

**Este es el riesgo que hunde una entrega de reportes en este repositorio, así que va primero.**

`CLAUDE.md` lo dice con todas las letras y es correcto: en JasperReports 7.0.3 **no hay compilación
en runtime que funcione** (`JRJaninoCompiler` no existe, `JRJdtCompiler` se movió a un artefacto que
no está en el `pom`), y `ReporteServiceImpl:110` busca el `.jasper` y **solo cae al `.jrxml` si no
lo encuentra** — un respaldo muerto. **Un reporte con solo `.jrxml` compila, pasa la revisión, entra
al commit y revienta la primera vez que un usuario lo ejecuta.** Ya pasó con los siete de `rhh`.

⚠️ **Y hay documentación que dice lo contrario:**
`docs/logica-negocio/reportes/REGLAS_GENERACION_REPORTES_G.md:306-307` afirma que *«se entrega solo
el `.jrxml`; `ReporteServiceImpl` lo compila en runtime con Janino (no hay Jaspersoft Studio / `mvn`
en el entorno para precompilar)»*. **Las dos mitades son falsas hoy** — no hay Janino, y en esta
máquina sí hay Maven. No seguir ese documento.

### El procedimiento verificado (2026-09-01, en OMEN)

**Probado de punta a punta compilando `RPRT_CRTF_APRT.jrxml`**: produjo un `.jasper` de 28.736
bytes contra los 28.642 del commiteado, con la misma cabecera de objeto serializado
(`aced 0005 7372 0028 net.sf.j...`) y la misma versión 7.0.3 del `pom`. **Funciona sin Jaspersoft
Studio**, porque fuera de WildFly el compilador del JDK 21 sí está disponible.

```bash
# 1. Classpath del proyecto (una vez)
mvn -q dependency:build-classpath -Dmdep.outputFile=<tmp>/cp.txt

# 2. Compilador de un solo uso (fuera de src/, en un temporal)
cat > <tmp>/CompilarJasper.java <<'EOF'
import net.sf.jasperreports.engine.JasperCompileManager;
public class CompilarJasper {
    public static void main(String[] a) throws Exception {
        JasperCompileManager.compileReportToFile(a[0], a[1]);
        System.out.println("OK -> " + a[1]);
    }
}
EOF
javac -cp "$(cat <tmp>/cp.txt)" <tmp>/CompilarJasper.java

# 3. Compilar el reporte
java -cp "<tmp>;$(cat <tmp>/cp.txt)" CompilarJasper \
     src/main/resources/rep/crd/RPRT_INFR_DVAP.jrxml \
     src/main/resources/rep/crd/RPRT_INFR_DVAP.jasper
```

**El `.jrxml` y el `.jasper` se commitean los dos.** El `.java` de compilación **no**: es una
herramienta de un solo uso y vive en un temporal, no en `src/`.

**Si el paso 3 falla, el reporte no se entrega**: se reporta el error y se para. Un `.jrxml` sin su
`.jasper` no es media entrega, es una entrega que revienta en producción.

---

## 4. La consulta — reglas obligatorias

### 4.1 ⛔ Nada de `SELECT *`, y alias únicos

`CLAUDE.md` lo documenta con un caso real (`RPRT_MVMN_APXT.jrxml`, 2026-08-27): cuando dos tablas
del `FROM` comparten un nombre de columna, Jaspersoft Studio renombra la segunda a `COLUMN_n` y la
resuelve **por posición**. Un `ALTER TABLE ... ADD` en cualquier tabla del `FROM` corre todo una
posición y el reporte imprime la columna equivocada, **sin ningún error**.

**Acá el riesgo es concreto: `TPAPCDGO` está en `CRD.DDVA` y en `CRD.TPAP`.** Es exactamente el
caso que produjo el bug. **Listar las columnas explícitamente y aliasar toda coincidencia.**

### 4.2 Una sola query, con discriminador de sección

El reporte tiene tres bloques con formas distintas. Se resuelve con **una query única con
`UNION ALL` y una columna `SECCION`**, agrupada por ella — no con subdatasets ni subreportes.

**Por qué así:** ningún reporte del repositorio usa subdatasets ni componentes `table`/`list`, y si
alguna vez hicieran falta subreportes, cada subreporte necesitaría **también** su `.jasper`
compilado (`SUBREPORT_DIR` los carga por `.jasper`), o sea el mismo riesgo del §3 multiplicado. Una
query plana no tiene esa dependencia.

**Forma de las columnas del `UNION ALL`** — homogéneas en las tres ramas:

| Columna | Tipo | Contenido |
|---|---|---|
| `SECCION` | NUMBER | `1` = lo que se devuelve · `2` = aportes aplicados a préstamos · `3` = deuda vigente |
| `ORDEN` | NUMBER | orden dentro de la sección |
| `CONCEPTO` | VARCHAR2 | tipo de aporte (`TPAP.TPAPNMBR`), o el préstamo |
| `DETALLE` | VARCHAR2 | fecha, glosa, estado del préstamo — según la sección |
| `VALOR` | NUMBER | el importe de la línea |

Los datos de cabecera (partícipe, fecha, total) se traen como **subconsultas escalares** repetidas
en cada rama, para que estén disponibles en la primera fila. Es el patrón que ya usa
`ESPECIFICACION-REPORTE-JASPER-COMPROBANTE-PAGOS-CUOTA.md`.

### 4.3 Las tres ramas

**Sección 1 — lo que se devuelve** (`CRD.DDVA` ⨝ `CRD.TPAP`), filtrada por `DDVA.DVAPCDGO = $P{P_ID_DEVOLUCION}`.
Alias obligatorio en el `TPAPCDGO` de la segunda tabla.

**Sección 2 — aportes aplicados a préstamos**: `CRD.APRT` de la entidad de la devolución, con
`APRT.APRTTPMV = 4` y `APRT.APRTFCTR >= $P{P_FECHA_DESDE}` y `<=` la fecha de la devolución.
`CONCEPTO` = tipo de aporte, `DETALLE` = `APRTGLSA` y fecha, `VALOR` = `APRTVLRR`.

**Sección 3 — deuda vigente**: `CRD.PRST` de la entidad, con
`(PRSTIDST IS NULL OR PRSTIDST NOT IN (3,4,5))`.

> ⚠️ **El `IS NULL` no es decorativo.** En Oracle, `NULL NOT IN (3,4,5)` evalúa a UNKNOWN y la fila
> **se cae en silencio**: un préstamo con estado sin poblar desaparecería del informe. Es la misma
> ratificación del 2026-08-24 para `deudaVigente`: en un aviso, subreportar deuda es peor que
> sobrereportarla.
>
> ⚠️ **Es `PRSTIDST`, nunca `ESPSCDGO`** (`CLAUDE.md`, §"qué columna lleva realmente el estado").

**El saldo del préstamo:** el endpoint `deudaVigente` lo calcula con
`motorPagoPrestamoService.calcularTotalPendientePrestamo(id)`, que reconstruye desde los pagos
vigentes, y su propia especificación dice **«no sumar columnas de `DTPR` a mano»**. En un `.jrxml`
no se puede llamar a ese motor. **Consecuencia aceptada y que hay que declarar en el informe:** la
sección 3 usa la suma de saldos pendientes de `CRD.DTPR` y por eso lleva al pie la leyenda
*«Saldos referenciales a la fecha de emisión»*. Si el número tiene que ser exacto, la alternativa
es un endpoint que arme el modelo en Java — **no está en esta entrega**.

---

## 5. Parámetros del reporte

⚠️ **Los parámetros numéricos que llegan como JSON se coercionan a los tipos declarados en el
`.jasper`** (`convertirTiposParametros`); un desajuste de tipos es la falla más común de esta área.

| Parámetro | Tipo en el `.jrxml` | Origen |
|---|---|---|
| `P_ID_DEVOLUCION` | `java.lang.Long` | la devolución recién registrada, o la elegida del histórico |
| `P_NUMERO_INFORME` | `java.lang.String` | **lo digita el operador** (U3) |
| `P_OBSERVACIONES` | `java.lang.String` | **lo digita el operador** (U4), puede venir vacío |
| `P_FECHA_DESDE` | `java.sql.Date` | corte del bloque B. Se precarga con el **1 de enero del año de la devolución** |

**Nada de `idEmpresa`**: este reporte no genera asientos.

---

## 6. Contenido fijo de la plantilla (U3, U4)

Transcrito del Word. **Va literal en el `.jrxml`**, salvo Observaciones.

- **Entidad:** `ASOPREP - FCPC` · **Área responsable:** `Área de Crédito`
- **Asunto:** *Informe de justificación de pagos y devoluciones de cesantías a personas cesantes, jubilados por el IESS y desafiliados del fondo*
- **Antecedentes, Objetivo, Justificación, Conclusiones, Recomendaciones:** **el texto completo está
  en el Anexo A de este documento.** Transcribirlo desde ahí, literal. La **Justificación** cita la
  Resolución No. 280-2016-F, Capítulo II, Artículo 55: **esa cita no se toca ni se reescribe.**
- **Observaciones:** `$P{P_OBSERVACIONES}`. Si viene vacío, la sección **se colapsa sin dejar hueco**
  (`removeLineWhenBlank="true"` + `blankWhenNull="true"`).
- **Firma:** `Lic. Gabriel Robayo` / `Jefe de Crédito`, y la razón social completa:
  *ASOCIACIÓN DEL FONDO COMPLEMENTARIO PREVISIONAL CERRADO ASOPREP-FCPC DE JUBILACIÓN Y CESANTÍA, DE
  LAS EMPRESAS PÚBLICAS DEL SECTOR HIDROCARBURÍFERO*

> **El firmante fijo es deuda conocida** (U3): el día que cambie la persona hay que reeditar el
> `.jrxml` y recompilar el `.jasper`. Se acepta a cambio de no tocar el backend ni agregar columnas.

**Cabecera del documento:** `INFORME No. $P{P_NUMERO_INFORME}`, ciudad y fecha
(`Quito, <fecha de la devolución en texto largo>`).

**Datos del partícipe:** cédula (`ENTD.ENTDNMID`), nombres (`ENTD.ENTDRZNS`), fecha de la devolución
(`DVAP.DVAPFCHA`) y fecha de compromiso de pago (`DVAP.DVAPFCPG`; si es `NULL`, se imprime la fecha
de la devolución).

---

## 7. Nombre y ubicación

| | |
|---|---|
| Reporte | **`RPRT_INFR_DVAP`** — verificado libre en `src/main/resources/rep/crd/` |
| Archivos | `src/main/resources/rep/crd/RPRT_INFR_DVAP.jrxml` **y** `.jasper`, **los dos commiteados** |
| Invocación | `POST /rest/rprt/generar` con `{modulo:"crd", nombreReporte:"RPRT_INFR_DVAP", formato:"PDF", parametros:{...}}` |

**No hace falta ningún endpoint nuevo.** `ReporteRest` (`@Path("rprt")`) ya es genérico y el
frontend ya tiene `JasperReportesService.generar(modulo, nombreReporte, parametros, formato)`
devolviendo `blob`.

---

## 8. Lo que NO entra

- **No se toca `DevolucionAporteServiceImpl`, ni `DVAP`, ni `DDVA`.** El reporte lee, no escribe.
- **No hay DDL.** Ninguna tabla ni columna nueva.
- **No se enlaza el cruce con la devolución.** Era la tercera opción de U5 y se descartó: es DDL más
  cambios en un proceso que ya corre en producción. Si algún día se quiere el neto **auditable** y
  no reconstruido, esa es la entrega que hay que hacer.
- **No se toca el aviso de `deudaVigente`** ni su diálogo.

---

## 9. Pendiente registrado

**`REGLAS_GENERACION_REPORTES_G.md:306-307` está desactualizado y es peligroso** — afirma que basta
el `.jrxml` porque hay compilación runtime con Janino. Un agente que lo lea entrega un reporte roto.
Corregirlo con el procedimiento del §3. **No es de esta entrega**; queda anotado acá y en
`ESTADO-EQUIPO-OMEN-1.md`.

---

## Anexo A — Texto literal de los bloques fijos

**Transcrito del Word `INFORME DE NECESIDAD DE PAGO CESANTIAS 27.docx`** (informe
FCPC-CRE-GR-137-2026, Quito, 22 de mayo de 2026), entregado por el usuario el 2026-09-01.

> ### ⛔ El Word NO se commitea, y por eso este anexo existe
>
> El original lista **19 partícipes con cédula, nombre completo y monto**. Son datos personales de
> personas reales: **no entra al repositorio**, ni como adjunto ni convertido. Este anexo transcribe
> **solo los bloques de texto**, que son los que la plantilla necesita, y ningún dato de partícipe.
>
> Fue un hueco real: la primera versión de esta especificación decía «los cinco bloques del Word,
> literales» sin transcribirlos, y el agente de backend quedó bloqueado con razón — no podía
> inventar una cita legal, y **hizo bien en parar en vez de rellenar con un placeholder.**

### ⚠️ Adaptación al singular — supuesto declarado, revisar antes de dar por bueno

El Word es un informe **grupal** y está redactado en plural: «los ex partícipes», «este grupo», «el
cronograma de pagos». En un informe de **un solo partícipe** ese plural es incorrecto.

**Los textos de abajo están adaptados al singular**, con dos límites:

1. **La cita textual del Artículo 55 queda EXACTA**, con su plural, sus comillas y sus puntos
   suspensivos finales. Es una transcripción normativa: adaptarla sería falsificarla.
2. Se conserva la redacción del original en todo lo demás — se cambia el número gramatical y las
   referencias al cronograma grupal, nada más. No se mejora la prosa ni se corrige su puntuación.

**El usuario firma este documento: la adaptación es un supuesto del árbitro y hay que validarla con
él antes de dar el reporte por cerrado.** Si prefiere el plural literal del original, es cambiar el
texto del `.jrxml` y recompilar el `.jasper`; no cambia nada más.

### A.1 — Antecedentes

> El presente informe tiene como finalidad justificar la devolución de la cuenta individual,
> conforme a la solicitud ingresada por el ex partícipe.

### A.2 — Objetivo

> Dar atención al requerimiento del ex partícipe relacionado con el pago de su cuenta individual y
> los cruces con préstamos pendientes de cancelación, posterior a las reuniones mantenidas con el
> mismo y llegar a un acuerdo de cancelación de sus préstamos, esto con el propósito de apoyar sus
> necesidades económicas derivadas de la situación que presenta.

### A.3 — Detalle del partícipe (encabezado de la tabla)

> De acuerdo a la autorización de la Gerencia General se procede a listar los datos con los que se
> efectuará el pago correspondiente del ex partícipe que ingresó la solicitud correspondiente:

*(El original terminaba con «mismas que estarán bajo el cronograma del día 15 de mayo de 2026». La
referencia al cronograma grupal se retira: en el informe individual la fecha de compromiso de pago
ya va en los datos del partícipe.)*

### A.4 — Justificación

> El presente proceso se ejecuta en cumplimiento de lo dispuesto en la Resolución No. 280-2016-F de
> la Junta de Política y Regulación Monetaria y Financiera, Capítulo II – Liquidación de la cuenta
> individual, Artículo 55, el cual establece:

**Cita textual — EXACTA, no se modifica:**

> “La liquidación de la cuenta individual de un Fondo Complementario Previsional Cerrado de Cesantía
> se da cuando un participe termine su relación laboral con la institución pública, privada o mixta,
> bajo la que se constituyó el ente previsional y se cumplan las condiciones previstas en la presente
> norma, En este caso, se le entregará el saldo de su cuenta individual debiendo efectuarse
> previamente las deducciones que correspondan (…)”

**Cierre del bloque:**

> En este contexto, y atendiendo a la solicitud expresa del ex partícipe, se ha establecido el pago
> que permite la ejecución ordenada y transparente de la devolución correspondiente, por lo cual se
> le realizará el pago del 100% de sus cuentas de cesantía y jubilación personal menos sus haberes
> correspondientes.

### A.5 — Observaciones

**Este bloque es `$P{P_OBSERVACIONES}`, lo digita el operador y NO va fijo.** Los tres párrafos del
Word se transcriben acá **solo como referencia** — el frontend los puede ofrecer como texto
sugerido, pero la plantilla no los lleva quemados:

> De acuerdo con las particularidades del caso, el pago efectuado es parcial, dado que el saldo de
> capital pendiente de los préstamos no alcanza a ser cubierto en su totalidad con los valores de
> cesantías.

> Los rendimientos de las cuentas individuales no han sido considerados en el presente proceso,
> debido a que la actual administración se encuentra en proceso de transición y auditoria y atado al
> artículo 195 de la Resolución No. JPRFM-2025-005-F, de fecha 31 de octubre de 2025, emitida por la
> Junta de Política y Regulación Financiera y Monetaria.

> En este mismo contexto, los valores pendientes de pago a favor del partícipe serán cancelados una
> vez que se cumplan los tiempos establecidos por el ente de control para cumplir la transición del
> fondo, la conciliación de cartera y la auditoría correspondiente determinen de manera favorable los
> saldos definitivos de su cuenta individual.

### A.6 — Conclusiones

> El pago avanza conforme se recibe la solicitud del partícipe y los requerimientos del ente de
> control, encontrándonos al día en las atenciones con el presente informe.

> Asimismo, se ha procurado mantener la transparencia y trazabilidad en cada etapa del proceso,
> considerando las observaciones y verificaciones necesarias para garantizar una gestión responsable
> de los recursos del fondo, por lo que es óptimo verificar las cuentas patronales del partícipe,
> teniendo en cuenta que el sistema Delta 21 no es lo 100% operativo.

### A.7 — Recomendaciones

> Continuar con el proceso de migración y auditoría de las cuentas individuales y sus respectivos
> rendimientos, a fin de esclarecer los saldos reales de cada partícipe.

> Fortalecer los mecanismos de control interno y conciliación periódica entre las áreas contable,
> financiera y administrativa, para evitar discrepancias en los registros y agilizar futuras
> devoluciones.

### A.8 — Cabecera y firma

Ya estaban en §6; se repiten acá para que todo lo transcribible salga de un solo lugar.

| Campo | Texto |
|---|---|
| Título | `INFORME DE NECESIDAD DE PAGO` |
| Entidad | `ASOPREP - FCPC` |
| Área responsable | `Área de Crédito` |
| Elaborado por | `Gabriel Robayo` |
| Asunto | `Informe de justificación de pagos y devoluciones de cesantías a personas cesantes, jubilados por el IESS y desafiliados del fondo` |
| Firma | `Atentamente,` / `Lic. Gabriel Robayo` / `Jefe de Crédito` |
| Razón social | `ASOCIACIÓN DEL FONDO COMPLEMENTARIO PREVISIONAL CERRADO ASOPREP-FCPC DE JUBILACIÓN Y CESANTÍA, DE LAS EMPRESAS PÚBLICAS DEL SECTOR HIDROCARBURÍFERO` |

**El Asunto queda en plural**: describe el tipo de trámite, no a este partícipe.

---

## Anexo B — Decisiones sobre la query resueltas con el backend (2026-09-01)

Tres desvíos que el agente de backend propuso al armar la consulta. **Los tres aprobados**, y se
anotan porque mejoran lo que decía el §4:

| Desvío | Resolución |
|---|---|
| **No proyectar `TPAPCDGO`** de ninguna de las dos tablas en la sección 1 — el join va por el `ON` y de `TPAP` solo se saca `TPAPNMBR` | **Mejor que aliasar.** Una columna que no se selecciona no puede convertirse en un `COLUMN_n`. El §4.1 pedía aliasar; no proyectarla elimina el problema en vez de administrarlo |
| **`CASE` literal sobre `PRSTIDST`** para el texto del estado, en vez de unir contra `CRD.ESPS` | **Correcto.** Y usó `8 = DE_PLAZO_VENCIDO`, `11 = EN_MORA`, que es lo bueno: el JavaDoc de `PrestamoDaoService.countVigentesMoraVencidosByEntidad` tiene **esas dos etiquetas cruzadas** y el agente no cayó en la trampa |
| **`APRTFCTR < DVAPFCHA + 1`** en vez de `<= DVAPFCHA` como tope de la sección 2 | **Acierto, y corrige el §4.3.** `APRTFCTR` es `TIMESTAMP` y `DVAPFCHA` es `DATE`: con `<=` se perderían los movimientos del mismo día de la devolución con hora distinta de medianoche |
