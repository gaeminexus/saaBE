# Guion de julio de 2026 — réplica en producción

> **Dónde vive cada cosa:** los `.sql` que este guion cita viven **sólo** en
> `saaBE/docs/logica-negocio/rhh/sql/`. Los `.md` sí están espejados en `saaFE/docs/rrh/`.
>
> **El guion apunta, no copia** (§4 ter de `PLAN-PASO-A-PRODUCCION.md`). Ningún importe del
> cliente se transcribe aquí a mano: ya está en `RHH.CTRL`, cargado por `sql/51`. Donde hace
> falta un valor para escribir una novedad, este guion trae **la consulta que lo lista**, y el
> resultado de correrla el 2026-08-25 al escribir este guion — para que sea usable sin dejar de
> ser trazable. Si el número de la tabla y el de la consulta alguna vez discrepan, **gana la
> consulta**.

**Escrito el 2026-08-25, ANTES de ejecutarlo — igual que se hizo con junio.** Nadie ha corrido
julio en ninguna base con el motor final. Lo que no esté aquí es hallazgo y se reporta sin
interpretarlo. **No crea el período. No registra novedades.** Es un plan para que julio arranque
tan medido como arrancó junio.

---

## ⚠ Julio es distinto de los seis anteriores, y hay que decirlo antes de tocar nada

**1 · Vacaciones pagadas a tres personas en el rol del cliente** (concepto 12), las tres con menos
de 30 días trabajados: **Caiza** (29 días), **Nieto** (29 días) y **Pardo** (26 días). **Nuestro
motor no genera vacaciones pagadas y tampoco reduce los días trabajados** — verificado en código,
ver §3 bis. Las dos omisiones se cancelan y el total por persona coincide igual: es un aviso de
bloque 1, no un hallazgo, y no se ajusta nada.

**2 · Fondo de reserva a SEIS, no a cinco.** Se suma **Rodríguez Valencia**, que cumple el año el
**16-07-2026** y va prorrateada. Verificado por REST el 2026-08-25: `MPLD` código 62,
`fechaIngreso [2025,7,16]`, `CNTE.modalidadFondosReserva = 1` (normal, no ACUMULADO — la única en
ACUMULADO sigue siendo Viteri López). El bloque completo, ya arbitrado, está en §5.

**3 · Viteri cobra y se le descuenta el mismo fondo de reserva.** 183,26 en las dos columnas del
rol del cliente: está en modalidad ACUMULADO, así que el cliente se lo abona y se lo retiene para
remitirlo al IESS. **Nuestro motor no hace ni lo uno ni lo otro: le genera provisión** (ver la
provisión de 30,54/366,67 en junio, mismo mecanismo). Así que el rol del cliente trae 183,26 más
de ingresos y 183,26 más de descuentos que el nuestro para ella, y en su líquido se anulan. Esto
es además la explicación retroactiva de la contradicción que se vio en junio, cuando parecía que
Viteri cobraba y a la vez se le declaraba: son las dos patas del mismo movimiento, vistas primero
del lado equivocado.

**4 · Los 44,60 de junio vuelven, como D:OTROS en el rol del cliente**: Bárcenas 1,95 · Muñoz
1,53 · Nieto 2,50 · Pardo 1,95 · Viteri 36,67. Es la devolución de la diferencia de junio, y por
eso los dos meses se anulan entre sí. **Los cinco están explicados, y ya no es pregunta para
Steven.** Bárcenas, Muñoz, Nieto y Pardo son idénticos al bloque 1 de junio —el día de más de
fondo de reserva que el cliente les pagó ese mes—; Viteri son sus 36,67 de fondo de reserva de
junio. **Este guion NO instruye registrarlos como novedad**, y ahora con motivo, no sólo por
prudencia: nosotros no pagamos de más en junio —nuestro motor ya salió exacto—, así que no tenemos
nada que recuperar en julio. Registrarlos nos haría descontar 44,60 que nunca dimos.

**Y ojo, porque es fácil de leer al revés:** que no se registren **no** significa que no aparezcan
en el contraste. `sql/57` (no `sql/51`) **sí** carga estos cinco como fila de concepto 31 en
`CTRL` —del lado del cliente—, así que el bloque 1 los va a traer los cinco, marcados como que
nuestro sistema no los generó. Es correcto que salgan: es la prueba de que la transcripción del
rol del cliente está completa y de que nuestro motor, con razón, no tiene nada que oponerles. Ver
§0 (falta comprobar que `sql/57` insertó las cinco filas de julio) y §6.

**5 · Robayo estrena quirografario** — primera cuota, concepto 23.

**6 · Los aportes del cliente están mal, y nuestro motor acierta.** Los calculó sobre días
trabajados dejando fuera las vacaciones; según la norma las vacaciones son imponibles.
**NO se ajusta el motor.** Se registra lo que se pagó, que es la regla que gobierna todo lo demás
(`ESTADO-RRHH.md`, «El motor responde a la norma, los datos responden a lo que pasó»).

**Y una séptima cosa que no viene en la lista de arriba y que este guion encontró leyendo
`sql/51` con cuidado — sin decidirla:** los totales de cabecera de Calderón dan **INGRESOS
700,10 / DESCUENTOS 700,10 / LÍQUIDO 0,00**. Los descuentos cuadran exactos (66,15 aporte +
13,85 quirografario + 620,10 anticipo = 700,10), pero los **ingresos no**: su único concepto de
ingreso cargado es el sueldo, 700,00 (`CTRL` concepto 1), y no hay ningún otro concepto de
ingreso para ella en la carga. **De dónde salen los 0,10 de más en INGRESOS no está explicado en
`sql/51` ni en la lista de novedades del mes.** Puede ser el mismo tipo de redondeo de cabecera
que el céntimo de Muñoz (§6.b), o puede ser un dato que falta cargar. **No se inventa un concepto
para cuadrarlo.** Queda como pregunta abierta — ver el final del guion.

---

## 0. Precondiciones — cada una con su consulta (§4 ter del PLAN)

| Precondición | Cómo se comprueba | Verificado 2026-08-25 |
|---|---|---|
| `sql/51` ejecutado | `SELECT CTRLFNTE, COUNT(*), COUNT(DISTINCT CTRLIDNT) FROM RHH.CTRL WHERE CTRLANOO=2026 AND CTRLMESS=7 GROUP BY CTRLFNTE;` | **Pendiente** — lo ejecuta Mike. Esperado: `ROL 125 / 20`, **ninguna fila `PLANILLA`** (julio no tiene planilla del IESS, `REF-06 §11`) |
| `sql/57` para el OTROS de julio | **Sí aplica, y hay que comprobarlo, no darlo por hecho.** `57_CTRL_OTROS_ABRIL_JUNIO_JULIO.sql` inserta en un solo `INSERT` las siete filas de concepto 31 de los tres meses —abril 175,00, junio 0,10, julio sus cinco (44,60)— con una guarda `WHERE NOT EXISTS (SELECT 1 FROM RHH.CTRL WHERE CTRLALTR = 31)` **todo-o-nada**: si al correrlo ya existía una sola fila de concepto 31, no insertó **ninguna** de las siete. La de junio (0,10, Calderón) ya se vio al correr junio; **las cinco de julio nadie las ha comprobado todavía**. Sin ellas, 44,60 de los 31,43 atribuidos en §6 no existen |
| Las cinco filas de concepto 31 de julio existen en `CTRL` | `SELECT CTRLIDNT, CTRLVLOR FROM RHH.CTRL WHERE CTRLANOO=2026 AND CTRLMESS=7 AND CTRLALTR=31 ORDER BY CTRLIDNT;` | **Pendiente de comprobar.** Esperado: 5 filas — Bárcenas 1,95 · Muñoz 1,53 · Nieto 2,50 · Pardo 1,95 · Viteri 36,67, suma 44,60 |
| `CTRL_PARAM` en el mes 7 | `SELECT ANIO, MES FROM RHH.CTRL_PARAM;` | Hoy en **2026 · 6** (se dejó así al cerrar junio, correcto). **Mike lo mueve a 7 justo antes de contrastar**, no antes — con `CTRL_PARAM` en 6 el instrumento contrastaría junio otra vez con datos completos y saldría verde del mes equivocado, igual que advertía el guion de junio |
| Enero a junio intactos | `SELECT p.PRDNMSEE, COUNT(*), COUNT(DISTINCT v.MPLDCDGO), SUM(v.PVNMVLOR) FROM RHH.PVNM v JOIN RHH.PRDN p ON p.PRDNCDGO=v.PRDNCDGO WHERE p.PRDNANOO=2026 AND v.PVNMTPPR=4 GROUP BY p.PRDNMSEE ORDER BY 1;` (patrón `sql/58`) | **Verificado por REST el 2026-08-25** (endpoint `/pvnm`, concepto alterno 53 «Provisión fondos de reserva»): enero a mayo **183,26 con 1 persona (Viteri López) cada uno**; junio **30,54 sobre base 366,67, también 1 persona**. Los seis `PRDN` de enero a junio en `estado=7`. Ninguno se movió |
| Aniversario de Rodríguez Valencia | `SELECT MPLDFCIN FROM RHH.MPLD WHERE MPLDIDNT='0801999855';` | **Verificado por REST**: `fechaIngreso [2025,7,16]` → cumple el año el **16-07-2026**. Es el dato del que cuelga su prorrateo de FR (14 días de 30, base 326,67) |
| Colaboradores de julio | `SELECT COUNT(*) FROM RHH.CTRL WHERE CTRLANOO=2026 AND CTRLMESS=7 AND CTRLALTR=1;` | Esperado **20**, contando las filas de sueldo (concepto 1) que carga `sql/51`. Mismas 20 personas que junio |

---

## 1. Fichas: nada que tocar, salvo confirmar lo que ya está

Nadie entra, nadie sale, nadie cambia de sueldo ni de jornada. **La modalidad de fondos de
reserva de Viteri López sigue en 2 · ACUMULADO** (verificado 2026-08-25); **la de Rodríguez
Valencia es 1 · normal** (también verificado) — no es un cambio, es la ficha que ya tenía y que
ahora empieza a importar porque cumple el año este mes.

No hay finiquitos ni salidas pendientes para julio (las cuatro del §4 ter del PLAN son de enero y
marzo, ya ejecutadas).

---

## 2. Crear el período

| Campo | Valor |
|---|---|
| Año / Mes | **2026 / 7** |
| Fecha de inicio | **01/07/2026** |
| Fecha de fin | **31/07/2026** |
| Tipo de período | **MENSUAL** |
| Modo | **1 · HISTÓRICO SIN CONTABILIZAR** |

Fechas en **`dd/mm/aaaa`**. **La comprobación del rango, inmediatamente después de guardar y
antes de la primera novedad** — es el defecto D15: una fecha inválida se sustituye en silencio
por la de HOY sin marcar error, y un período mal armado calcula sin quejarse. Verificar por REST
(`GET /rest/prdn/getAll`, filtrar `anio=2026 && mes=7`) que `fechaInicio=[2026,7,1]` y
`fechaFin=[2026,7,31]` antes de seguir. Si sale mal, se borra y se rehace; `modo`/`tipoPeriodo` se
corrigen en sitio si hace falta.

---

## 3. Novedades del período: DIEZ

**Tres quirografarios, tres hipotecarios, cuatro anticipos.** Todas nacen con «Aprobada para el
cálculo» = **No**; las diez pasan a **Sí**.

**La consulta que las lista** (correr contra `RHH.CTRL`, `CTRLFNTE='ROL'`, los tres conceptos de
préstamo):

```sql
SELECT CTRLIDNT, m.MPLDAPLL, c.CTRLALTR, c.CTRLVLOR
  FROM RHH.CTRL c LEFT JOIN RHH.MPLD m ON m.MPLDIDNT = c.CTRLIDNT
 WHERE c.CTRLANOO = 2026 AND c.CTRLMESS = 7 AND c.CTRLALTR IN (23, 24, 25)
 ORDER BY c.CTRLALTR, c.CTRLIDNT;
```

**Salida al correrla el 2026-08-25** (leída directamente de `sql/51`, líneas 144-156 — no
inventada, y hay que releerla contra la consulta de verdad antes de tipear, no contra esta tabla):

| Concepto (alterno) | Cédula | Colaborador | Valor |
|---|---|---|---:|
| 23 · Quirografario | 1719624809 | CALDERON PARRAGA | **13,85** |
| 23 · Quirografario | 1716120769 | MANOSALVAS LLERENA | **157,21** |
| 23 · Quirografario | **1725996498** | **ROBAYO RUEDA** | **84,70** — primera cuota, novedad |
| 24 · Hipotecario | 1715156574 | COSSIO CAICEDO | **490,00** |
| 24 · Hipotecario | 1716120769 | MANOSALVAS LLERENA | **379,85** |
| 24 · Hipotecario | 0909917759 | PAZMIÑO JARAMILLO | **145,29** |
| 25 · Anticipo | 1753528379 | CAIZA REMACHE | **150,00** |
| 25 · Anticipo | 1719624809 | CALDERON PARRAGA | **620,10** |
| 25 · Anticipo | 0103179537 | MOSCOSO NOVILLO | **850,00** |
| 25 · Anticipo | 1717649873 | MUÑOZ SANTOS | **150,00** |
| | | quirografarios | **255,76** |
| | | hipotecarios | **1 015,14** |
| | | anticipos | **1 770,10** |

**Ojo con Pazmiño Jaramillo (0909917759):** sigue siendo el único con homónimo — Pazmiño Moreno,
2100192463. Filtrar por cédula completa hasta que quede un solo candidato antes de bajar con
teclado, la misma regla de siempre.

**No se registra un concepto 31 · Otros descuentos para nadie este mes, como novedad** — a
diferencia de junio. Ver el punto 4 de la cabecera: los 44,60 de OTROS son la devolución de junio
y ya están explicados, pero **no son nuestros para recuperar** — nosotros no pagamos de más en
junio, así que no hay nada que descontarle a nadie en julio. El de Viteri tampoco se tipea, porque
su descuento de FR ya lo representa (por ausencia) la provisión del motor.

**Esto no los saca del contraste.** `sql/57` ya los cargó en `CTRL` del lado del cliente (§0); el
bloque 1 los va a traer los cinco, marcados «el sistema no lo generó» — es el esperado, no un
hallazgo.

**Robayo estrena quirografario, y no hace falta nada más antes de tipear la novedad.**
Confirmado en código: `NovedadNomina` no referencia nada del módulo `crd`. El concepto 23 lleva un
valor y ya — no hay que crear ni asociar ningún crédito preexistente en `Créditos` para que la
novedad se acepte.

**Antes de calcular, la comprobación de que las diez entran** —`NVNMAPRB = 'S'` y
`NVNMESTD = 1`— por REST (`GET /rest/nvnm/getAll`, filtrar `periodoNomina.mes===7`), la misma del
§3 de los guiones anteriores.

---

## 3 bis. Vacaciones de Caiza, Nieto y Pardo — registro documental, INERTE para el cálculo

**Corregido por el árbitro sobre la primera versión de este guion, que la dejaba como pregunta
bloqueante. No lo es.** Verificado en código: `SolicitudVacaciones` sólo la tocan su DAO y su
servicio CRUD. `ProcesoNominaServiceImpl` **no la lee**, y aprobarla **no escribe en
`RHH.RSMN`**, que es la única tabla que `calculaDiasTrabajados` mira —y sólo sus tipos 1 y 3—.
**Registrar las tres solicitudes es inerte para el cálculo del período.** Las fechas exactas de
cada tramo no importan porque nada las lee para calcular: se pueden registrar por completitud del
historial de vacaciones (`Personal → Vacaciones`, con cualquier fecha dentro de julio que respete
el número de días), pero **no bloquean nada si se dejan sin registrar**, y no hace falta resolver
antes de correr julio dónde caen exactamente.

**Lo que sí importa, y es la consecuencia real de que el motor no lea `SLCT`:** el concepto 12
tiene `CPNMROLM` **vacío** (visible en el bloque 5 del contraste) y no existe ningún rol
`VACACIONES_PAGADAS`. **Nuestro motor no genera vacaciones pagadas, y tampoco reduce los días
trabajados** de Caiza, Nieto ni Pardo. Las dos omisiones se cancelan, y el total por persona
coincide igual que si el motor hiciera las dos cosas bien:

```
Nieto  cliente 870,00 (sueldo) + 30,00 (vacaciones) = 900,00   nuestro 900,00 de sueldo, sin vacaciones
Pardo  cliente 606,67 (sueldo) + 93,33 (vacaciones) = 700,00   nuestro 700,00 de sueldo, sin vacaciones
Caiza  cliente 465,93 (sueldo) + 16,07 (vacaciones) = 482,00   nuestro 482,00 de sueldo, sin vacaciones
```

**Esto hay que escribirlo en el esperado del contraste:** el bloque 1 va a traer **seis filas
más** por esto —una de concepto 1 con nuestro importe **mayor** que el del cliente, y una de
concepto 12 marcada **«el sistema no lo generó»**, por cada una de las tres personas. **Las tres
parejas suman cero. No son un hallazgo:** son la consecuencia visible, y esperada, de que las dos
omisiones del motor se cancelen entre sí.

> ⚠ **NO registrar las vacaciones como novedad de concepto 12.** Es el atajo natural en cuanto se
> ve que el motor no las genera solo, y es exactamente el error a evitar: **inflaría los ingresos
> del período en 139,40** (16,07 + 30,00 + 93,33), porque nuestros días trabajados no bajan —el
> motor seguiría calculando el sueldo entero, y encima de eso se sumaría la vacación tipeada a
> mano. El sueldo entero **ya** es el equivalente correcto a sueldo-por-días-más-vacaciones del
> cliente; sumarle algo más lo duplica.

`GET /rest/slct/getAll` estuvo roto hasta el 2026-08-20 (`ORA-00904` por `SLCTFHAP`/`SLCTAPRB`
sin columna) y se cerró ese mismo día — verificar que sigue devolviendo `[]` sin error antes de
crear cualquier solicitud, por si algo lo reabrió. Es sólo para el registro documental; no
condiciona el cálculo.

---

## 4. El filo de Calderón, otra vez — y esta vez sí dispara el recorte

**Corregido por el árbitro: la primera versión de este guion aplicó la aritmética de la columna
del cliente a la nuestra sin rehacerla — el mismo error que el 1,94 de junio.** En la columna del
**cliente** el neto cuadra en cero sin recorte (700,10 − 700,10). **En la nuestra, no:**

```
INGRESOS (nuestro)     700,00   (no generamos el 0,10 sin explicar de Calderón)
DESCUENTOS (nuestro)   700,10   (66,15 aporte + 13,85 quirografario + 620,10 anticipo)
NETO                    −0,10   → recortaDescuentos SÍ SE DISPARA
```

**De los tres descuentos, sólo el anticipo es recortable.** `CPNMRCRT = 'S'`, orden 120; el aporte
y el quirografario están en `'N'`. El motor recorta **0,10 del anticipo**: queda en **620,00** en
vez de 620,10, y el líquido aterriza en 0,00 — pero por un camino distinto al del cliente.

**La trampa: el neto da 0,00 igual, así que un detector que sólo mire el neto no ve nada.** El
detector real es el **anticipo dentro del bloque 1 del contraste: 620,00 (nuestro) contra 620,10
(cliente)**. Si esa fila NO aparece, es cuando hay que preocuparse — significa que el recorte no
se disparó y el 0,10 quedó sin explicar en algún otro sitio, o que Calderón no llegó al filo como
se esperaba.

**Efecto en los totales del período, y por qué no mueve el líquido:** por este solo caso, nuestros
INGRESOS y DESCUENTOS del período quedan cada uno **0,10 por debajo** de los del cliente (700,00
contra 700,10 en las dos columnas). Como baja lo mismo en las dos columnas, **el líquido no se
mueve**: sigue sumando cero a la diferencia total del §6, que permanece en **+31,43**.

---

## 5. El bloque del fondo de reserva — YA ARBITRADO, se copia tal cual

**No se vuelve a derivar. Aquí es donde se cuelan los errores.** Verificado el 2026-08-25 con la
aritmética exacta de `RedondeoNomina`:

```
Bárcenas    30 días   base   700,00 ->  58,31   cliente  58,31
Muñoz       30 días   base   550,00 ->  45,82   cliente  45,81   <- +0,01
Nieto       29 días   base   900,00 ->  74,97   cliente  74,97
Pardo       26 días   base   700,00 ->  58,31   cliente  58,31
Rodríguez V 14 días   base   326,67 ->  27,21   cliente  27,21
Viteri      30 días   base 2.200,00 -> 183,26   cliente 183,26
                                       -------          -------
                                        447,88           447,87
```

**Dos cosas que no se ven a simple vista, y hay que escribirlas:**

**a) Nieto y Pardo cuadran PORQUE LAS VACACIONES ENTRAN EN LA BASE.** El concepto 12 lleva
`CPNMAPFR = 'S'`, así que las vacaciones reponen exactamente lo que los días no trabajados quitan
del sueldo:

```
Nieto  sueldo 870,00 + vacaciones 30,00 = 900,00 (el sueldo entero)
Pardo  sueldo 606,67 + vacaciones 93,33 = 700,00 (el sueldo entero)
```

Si esa bandera estuviera en `'N'`, Nieto saldría −2,50 y Pardo −7,77 **sin ninguna señal**.
Verificado en producción que está en `'S'` (`sql/60`, bloque 4, ya corrido antes de junio — no
hace falta repetirlo, pero si algo en julio sale con esa forma de error, es lo primero que se
mira).

**b) El centavo de Muñoz es del cliente, y su propio rol lo delata.** Su línea de FR dice 45,81,
pero su total de cabecera imprime **595,82**, y **550 + 45,81 = 595,81**. Su propio total implica
45,82, que es justo lo que da nuestro motor: **550 × 8,33 % = 45,815**, y la regla del módulo es
redondear cada renglón antes de sumarlo. **No se ajusta.**

**El bloque 1 es un `FULL OUTER JOIN`: sólo lista lo que difiere.** De las seis personas de fondo
de reserva de arriba, **cuatro coinciden exactas con el cliente y no aparecen** —Bárcenas, Nieto,
Pardo y Rodríguez Valencia—. **Sólo salen dos: Muñoz (por su céntimo, §5.b) y Viteri** (porque a
ella el motor no le paga fondo de reserva, le genera provisión — §3 de la cabecera). Que las otras
cuatro NO aparezcan en el bloque 1 es la confirmación de que cuadran, no un vacío sospechoso.

Y el bloque trae más filas todavía este mes, por otros motivos ajenos al fondo de reserva —la
vacación que el motor no genera (§3 bis), el anticipo recortado de Calderón (§4) y el OTROS de
junio que el cliente cargó y nosotros no generamos (punto 4 de la cabecera). El recuento completo,
con sus 17 filas, está en §6.

---

## 6. Calcular y contrastar

**El orden no se invierte** (§4 ter del PLAN): `CTRL_PARAM` a 7 va **justo antes** de contrastar,
nunca antes de calcular — y **contabilizarRol antes de cerrarPeriodo**, nunca al revés, porque
`contabilizarRol` pisa `PRDNOBSR` y si se cierra primero el aviso de novedades sin declarar se
pierde en silencio.

1. Calcular el período. Verificar por REST (`GET /rest/prdn/getId/{codigo}`), nunca por pantalla
   (D11): `estado = 3`.
2. **CTRL_PARAM a 7**, y comprobarlo leyendo de vuelta (`SELECT ANIO, MES FROM RHH.CTRL_PARAM;`).
   Lo hace Mike.
3. `CONTRASTE_MES_CONTRA_ROL_REAL.sql`, bloque 4 primero, luego 3, luego 1 y 2, y el 1B con sus
   dos consultas. `PERIODO_LEIDO = 2026-07` en cada bloque antes de mirar ninguna cifra.

**Qué esperar en cada bloque — y qué NO es hallazgo:**

- **Bloque 1: DIECISIETE filas, no trece ni seis.** Es un `FULL OUTER JOIN` sobre `CTRLALTR`
  —sólo trae lo que difiere entre nuestro cálculo y `CTRL`—, y por eso los totales de cabecera
  (`INGRESOS`/`DESCUENTOS`/`LIQUIDO`) **no** aparecen aquí: viven en la columna `CTRLTOTL`, no en
  `CTRLALTR`, y el bloque filtra `CTRLALTR IS NOT NULL`. Esos totales sólo se ven en el bloque 2.
  El desglose, por concepto:

  | Concepto | Filas | Quiénes, y qué dicen |
  |---|---:|---|
  | 1 · Sueldo | 3 | Caiza, Nieto, Pardo — nuestro importe **mayor** (§3 bis: el motor no reduce los días) |
  | 7 · Fondos de reserva | 2 | Muñoz (+0,01, §5.b) · Viteri («el sistema no lo generó» — va a provisión, no a rol) |
  | 12 · Vacaciones pagadas | 3 | Caiza, Nieto, Pardo — «el sistema no lo generó» (§3 bis) |
  | 20 · Aporte personal | 3 | Caiza, Nieto, Pardo — nuestro importe **mayor** (§6 de la cabecera: aporte sin vacaciones) |
  | 25 · Anticipo | 1 | Calderón — 620,00 nuestro contra 620,10 cliente (§4, el recorte) |
  | 31 · Otros descuentos | 5 | Bárcenas, Muñoz, Nieto, Pardo, Viteri — «el sistema no lo generó» (punto 4 de la cabecera; cargados por `sql/57`, no por nosotros) |
  | | **17** | |

  **Dos formas fáciles de leer esto al revés, y las dos son errores:**
  - **Pensar que el concepto 31 no debería aparecer porque no lo registramos.** Es al revés:
    precisamente porque no lo registramos, el `FULL OUTER JOIN` lo trae desde el lado del cliente.
    Si las cinco filas de concepto 31 **no** aparecen, lo que falla no es nuestro motor: es que
    `sql/57` no insertó las filas de julio (ver §0, la guarda todo-o-nada).
  - **Esperar las seis del fondo de reserva.** Sólo salen **dos** —Muñoz y Viteri—; las otras
    cuatro (Bárcenas, Nieto, Pardo, Rodríguez Valencia) coinciden exactas y por eso no aparecen.
    Verlas ausentes es la confirmación de que cuadran, no un hallazgo.

  Si aparece cualquier fila fuera de estas 17, o si falta alguna de las 17, **eso sí es hallazgo**.
- **Bloque 1B:** la provisión de FR pasa a **2 personas** — Viteri (30,54 sobre 366,67, como
  junio) y ahora también **Rodríguez Valencia** (su primer mes con provisión). Si sale sólo una o
  ninguna, es hallazgo.
- **Bloque 2:** revisar fila por fila contra `RHH.CTRL` (la consulta del §0), no contra un total.
  Esperar diferencias en Caiza/Nieto/Pardo (aporte sin vacaciones, §6 de la cabecera — no se
  ajusta) y en Calderón: **ingresos y descuentos los dos 0,10 por debajo del cliente** (§4 —
  no generamos el 0,10 sin explicar, y el recorte deja el descuento en 700,00 en vez de 700,10).
  El líquido de Calderón no difiere; sus dos totales sí.
- **Bloque 3: en julio sale VACÍO, y NO porque cuadre.** Corrección del árbitro sobre el guion de
  junio, que no aplica aquí: la fila de Muñoz que salía todos los meses **este mes no sale**, y no
  por ausencia de diferencia — por ausencia de datos. `sql/51` no carga ni una sola fila
  `PLANILLA`: **«JULIO NO TIENE PLANILLA DEL IESS. `REF-06 §11`: falta la del periodo 2026-07. Así
  que no se cargan filas PLANILLA y el bloque 3 saldrá vacío por ausencia de datos, no por cuadre.
  Que nadie lo lea como verde.»** **En julio ese bloque NO ES UN CONTROL. Está apagado.** Un vacío
  que parece un éxito es exactamente el fallo contra el que existe el bloque 4, y aquí se da en el
  bloque 3 por una razón que ningún control detecta — hay que saberlo de antemano porque nada en
  la pantalla lo va a decir.

  **Consecuencia que se sigue de esto, y que hay que nombrar en vez de dejar como hueco:** julio
  **no tiene forma de contrastar el lado del IESS.** La comprobación cruzada de aportes que en los
  otros seis meses daba el bloque 3 —rol contra planilla— no existe este mes. Los aportes de julio
  se juzgan **sólo contra el rol** (bloque 2), sin la segunda fuente independiente que en otros
  meses confirmaba o desmentía la primera.
- **Bloque 4:** `PERIODO_LEIDO = 2026-07`, 20/20 colaboradores, sin sorpresas de conteo.

**Y lo más importante de todo el guion:**

> **EL ESPERADO DE JULIO NO SE PUEDE FIJAR AL CÉNTIMO DE ANTEMANO, y hay que decirlo en vez de
> fingir que sí.** Junio pudo fijarse porque su diferencia tenía dos causas y las dos estaban
> medidas. Julio no: el rol del cliente trae **al menos tres inconsistencias propias** —el aporte
> sin vacaciones, el centavo de Muñoz, y los 44,60 de OTROS sin clasificar— y se cruzan entre sí,
> más la séptima cosa sin explicar de Calderón. **No hay un total esperado que escribir aquí.**
>
> **El criterio de aceptación de julio es este, y no otro:** se contrasta, **se atribuye
> diferencia por diferencia** —cada una a su causa conocida, o marcada explícitamente como sin
> explicar—, **y se cierra con lo atribuido documentado**, no con un total en verde. Un esperado
> inventado para que julio se parezca a los otros seis guiones es peor que no tener esperado:
> convierte el contraste en un trámite de confirmación en vez de en la comprobación que es.
>
> Este guion no apunta a `ESPERADO-CONTRASTE-JULIO.md` porque **ese archivo no existe y no se
> escribe antes de correr** — a diferencia de enero a mayo. Se escribe **después**, con lo que el
> contraste real atribuyó, fila por fila.

**Pero «no se puede fijar al céntimo» no es lo mismo que «no se puede atribuir».** El árbitro
derivó una diferencia esperada para julio, con dos caminos independientes que llegan al mismo
número — eso es lo que la hace creíble, no una garantía. **No sustituye al contraste real: es lo
que hay que ver si las condiciones de las que depende se cumplen, y dónde mirar primero si no.**

> **Julio debe salir 31,43 POR ENCIMA del cliente:**
>
> ```
> FR en el rol         nuestro 264,62   cliente 447,87    ingresos    −183,25
> FR descontado a Viteri                                  descuentos  −183,26
> OTROS, devolución de junio                               descuentos   −44,60
> Aporte personal, el nuestro mayor                        descuentos   +13,18
>                                                          LÍQUIDO      +31,43
> ```
>
> **Segundo camino, independiente del primero — llegar al mismo número por otra vía es lo que lo
> hace creíble:**
>
> ```
>  44,60  (la devolución de junio: el cliente se la descuenta, nosotros no)
> −13,18  (los aportes que nosotros descontamos bien y el cliente no)
>  +0,01  (el centavo de Muñoz en el fondo de reserva, §5.b)
> ──────
>  31,43
> ```
>
> **El desglose de los aportes, verificado al céntimo con la aritmética del motor** — son
> exactamente los tres que tomaron vacaciones, y la fórmula del cliente las deja fuera de la base
> imponible:
>
> | | Base nuestra | Nuestro | Base del cliente | Del cliente | Diferencia |
> |---|---:|---:|---:|---:|---:|
> | Caiza | 482,00 | 45,55 | 465,93 (29 días) | 44,03 | 1,52 |
> | Nieto | 900,00 | 85,05 | 870,00 (29 días) | 82,21 | 2,84 |
> | Pardo | 700,00 | 66,15 | 606,67 (26 días) | 57,33 | 8,82 |
>
> **No es un error de tecleo: es una fórmula que deja las vacaciones fuera de la base imponible.
> Nuestro motor acierta y no se toca** — es el mismo punto 6 de la cabecera, ahora con el céntimo
> puesto.
>
> **De qué depende esto, dicho antes y no después:** de que nuestro motor genere las vacaciones
> pagadas iguales a las del cliente —16,07 / 30,00 / 93,33, las mismas de §3 bis— y de que el
> sueldo por días cuadre. **Si eso se cumple, +31,43.** Si no, la diferencia se va a mover, y el
> desglose de arriba dice exactamente dónde mirar primero — que es el valor de tenerlo escrito
> antes de correr, no después de que algo no cuadre.
>
> **Un aviso aparte, que se anota y no se persigue:** los propios totales del cliente no cuadran
> consigo mismos. `21.482,32 − 5.198,61 = 16.283,71`, pero su rol imprime **16.283,70**. Ese
> céntimo es del cliente, va en la misma familia que el de Muñoz, y no se ajusta el motor para
> perseguirlo.

---

## 7. Al cerrar

1. **Aprobar.** No debería lanzar `IncomeException` — los porcentajes están medidos y coinciden
   (concepto 8,33 % y parametría 8,33 %) — pero si salta, se para y se reporta el mensaje literal,
   no se reintenta a ciegas.
2. **Contabilizar rol.** NO pulsar «Contabilizar provisiones» — julio sigue en modo histórico, los
   tres asientos quedan en `null`, igual que abril, mayo y junio.
3. **Cerrar.**
4. `PRDNOBSR` **debe quedar en el texto de carga histórica SÓLO si julio no deja `NVIS`
   pendientes**:
   ```
   Calculado sin contabilizacion (carga historica).
   ```
   **Si julio deja avisos de novedades sin declarar, el motor sobrescribe la observación a
   propósito, y ESO NO SE CORRIGE.** Enero y marzo están así y es evidencia —de los 208,22 que
   ASOPREP pagó de más y de los avisos de enero que tampoco se presentaron—, no un defecto de
   pantalla ni de motor. Si julio sale con la observación distinta al texto de arriba, **no se
   fuerza a que diga otra cosa**: se reporta tal cual salió.

**Después de cerrar, todo por base de datos, nunca por pantalla (D11):**

- `PRDN` de julio en **estado 7**.
- `PRDNOBSR` — el texto exacto, o el aviso de `NVIS` si eso es lo que salió (ver el punto 4 de
  arriba).
- Los tres asientos (`asientoRol`, `asientoProvisiones`, `asientoPago`) en `null`.
- Cuántas filas de `ACMN` escribió julio y cuántas hay en el año — **reportar el número que salga,
  no compararlo contra una expectativa.** Abril, mayo y junio dieron 120 cada uno; julio es el
  primer mes con seis personas de fondo de reserva en vez de cinco (o una en vez de ninguna, según
  se mire), así que no se da por sabido que siga en 120. Si difiere, lo juzga quien lea el
  reporte con el número delante, no este guion.
- La comprobación que no es de julio sino de los meses cerrados: enero a junio tienen que **seguir
  exactos** en sus provisiones de FR — enero a mayo en 183,26/1 persona, junio en 30,54 sobre
  366,67/1 persona. Si alguno cambió, se recalculó un mes cerrado, y con el WAR ya corregido eso
  **no mueve ningún líquido**, así que ningún total lo delataría — sólo esta consulta lo ve.

**Y pulsar por DOM, no por coordenadas.** En esta sesión el clic por coordenadas aterriza en la
fila equivocada de una tabla —la captura va a media escala del viewport real—; es del entorno, no
de la aplicación. Localizar la fila por su texto (`querySelectorAll('tr')` y filtrar por el código
de período) y hacer `.click()` sobre el elemento, verificando el destino por la URL resultante
antes de seguir.

---

## Preguntas — estado tras la corrección del árbitro del 2026-08-25

**Resueltas, ya no bloquean:**

- ~~Las fechas exactas de las tres vacaciones.~~ **Inerte para el cálculo** (§3 bis): nada en
  `ProcesoNominaServiceImpl` lee `SLCT`, así que las fechas no importan y no hace falta fijarlas
  antes de correr julio.
- ~~El concepto 31 de julio.~~ **Explicado**: los cuatro D:OTROS sin clasificar son el día de más
  de fondo de reserva que el cliente pagó a cada uno en junio (idénticos al bloque 1 de junio); con
  los 36,67 de Viteri, los cinco son la misma devolución. Se confirma no registrarlos — no son
  nuestros para recuperar.
- ~~Robayo y su primera cuota de quirografario.~~ **Confirmado**: `NovedadNomina` no referencia el
  módulo `crd`. No hay que crear nada antes.

**Sigue abierta, para Mike, antes de arrancar julio:**

1. **Los 700,10 de ingresos de Calderón** (la séptima cosa de la cabecera). No hay concepto que
   los explique en la carga de `sql/51`. ¿Es un redondeo de cabecera del cliente (como el céntimo
   de Muñoz) que simplemente se documenta, o falta un dato por cargar? Mientras no se resuelva,
   nuestro motor calcula sobre 700,00 (§4) y la diferencia queda documentada, no perseguida.

Nada de esto se resuelve inventando un dato. Lo que sigue abierto queda escrito para resolverse
antes de que julio arranque, no durante.
