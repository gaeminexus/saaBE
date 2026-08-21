# Qué pedirle a ASOPREP para verificar el motor de cálculo

**Fecha:** 2026-08-19 · **Bloquea:** la verificación del motor de cálculo (fase 4)

> **Cambio del 2026-08-19: el mes de verificación fuerte es ABRIL de 2026, no enero.** El
> cliente informa que abril es el mes con la información adicional completa; enero lo enviará
> «como lo tenían». No es un retroceso, es una mejora: abril prueba todo lo que enero probaba
> **más la cadena de acumulación** —los `ACMN` que escribe `cerrarPeriodo`, la proyección de IR
> con meses realizados y el avance de las cuotas de descuentos—, que es justo la parte del motor
> que el caso a mano no cubre. La condición es que enero, febrero y marzo se calculen y
> **cierren** en el sistema, en orden, antes de calcular abril; eso ya era el plan de la carga
> histórica. La consecuencia operativa: un descuadre en abril puede tener su causa en un mes
> anterior, así que hacen falta los puntos de control intermedios del §«Qué cambia con abril».
> El enero agregado sirve como control del mes 1.

Este documento existe para pedir el insumo **una sola vez y completo**. Un rol en PDF sin el
detalle de contrato no permite verificar nada: si un valor no cuadra, no se puede saber si el
motor está mal o si el empleado tenía otra modalidad de décimos.

---

## Qué cambia con abril

Los cuatro puntos del pedido original se mantienen, **referidos a abril**. Se agregan tres
cosas, todas por la misma razón: abril depende de la historia de enero a marzo, y sin puntos de
control intermedios un descuadre no se puede localizar.

| # | Insumo adicional | Para qué |
|---|---|---|
| A | **Movimientos de enero a abril completos**, no solo los de abril: cambios de sueldo con fecha de vigencia, ingresos, salidas y ausencias sin sueldo de los cuatro meses | Un cambio de sueldo en marzo que no conozcamos hace que abril no cuadre sin que el motor tenga culpa |
| B | **Planillas del IESS de enero, febrero y marzo** | Punto de control por mes: si la planilla de febrero cuadra, el descuadre de abril no viene de febrero |
| C | Si algún empleado tiene **retención de IR**: fecha de presentación del anexo de gastos personales y si recalcularon la proyección a mitad de año | Es el renglón más sensible a la historia previa: la retención de abril depende de lo realizado en enero–marzo |

**Orden de la corrida:** aplicar saldos de apertura al 31-dic-2025 → calcular y **cerrar** enero
→ febrero → marzo (en modo histórico, cuadrando cada mes contra su planilla del IESS) → calcular
abril → comparar renglón por renglón contra el rol real de abril.

---

## Lo que hay que pedir

### 1. El rol de pagos de enero de 2026 — **en Excel, el archivo original**

No una impresión ni un PDF: el `.xlsx` con el que se pagó, tal como lo tiene contabilidad. Si
solo existe en PDF, sirve, pero hay que decirlo para revisarlo a mano.

Una fila por empleado y **una columna por cada concepto, sin agrupar**. Lo que importa es que los
ingresos y los descuentos vengan **desglosados**, no sumados:

| Bloque | Columnas |
|---|---|
| Identificación | Cédula, apellidos y nombres, cargo |
| Tiempo | Días trabajados en el mes, horas extra si las hubo |
| Ingresos | Sueldo del mes · horas suplementarias (50 %) · horas extraordinarias (100 %) · recargo nocturno · décimo tercero mensualizado · décimo cuarto mensualizado · fondos de reserva · bonos, comisiones, subsidios y cualquier otro ingreso, **cada uno con su nombre** |
| Descuentos | Aporte personal IESS · impuesto a la renta · préstamo quirografario · préstamo hipotecario · anticipo de sueldo · préstamo interno · retención judicial · cualquier otro descuento, **cada uno con su nombre** |
| Totales | Total ingresos · total descuentos · **neto pagado** |
| Patronal | Aporte patronal IESS · IECE · SECAP, aunque no aparezcan en el rol que firma el empleado |

**La cédula es imprescindible**: es lo que permite cruzar cada fila contra el empleado en el
sistema. Sin ella la comparación se hace a ojo por nombre y deja de ser verificación.

### 2. La planilla del IESS de enero de 2026

El archivo o el comprobante de la planilla efectivamente pagada. Es el **control independiente**:
si el aporte personal y el patronal cuadran contra la planilla, la base imponible del motor es
correcta, sin depender de que el rol esté bien.

### 3. Los datos de contrato de cada empleado

Es la parte que se olvida y sin la cual el rol no se puede reproducir. Una fila por empleado:

| Dato | Por qué se necesita |
|---|---|
| Fecha de ingreso real | Decide si ya cumplió el año y por tanto si cobra fondos de reserva |
| Sueldo nominal vigente en enero de 2026 | Es la base de todo el cálculo |
| Décimo tercero: **mensualizado o acumulado** | Si es acumulado no hay renglón en el rol, hay provisión |
| Décimo cuarto: **mensualizado o acumulado** | Igual |
| Fondos de reserva: **mensualizado o acumulado en el IESS** | Si se acumula en el IESS no hay renglón |
| ¿Aporta al IESS? | Los contratos de servicios profesionales no aportan |
| Cargas familiares declaradas | Entra en el tope de gastos personales del impuesto a la renta |
| ¿Presentó anexo de gastos personales para 2026? Y por cuánto | Sin esto la proyección de IR no se puede contrastar |

### 4. Movimientos de enero, si los hubo

- Alguien que **ingresó** durante enero de 2026: fecha exacta.
- Alguien que **salió**: fecha exacta y su liquidación.
- Faltas injustificadas o permisos sin sueldo: empleado y número de días.
- Cambios de sueldo dentro del mes: fecha de vigencia.

Si no hubo ninguno, **decirlo explícitamente**. Un mes limpio es la mejor primera prueba, y
saber que lo es ahorra buscar diferencias que no existen.

---

## Cómo se va a usar

Se comparan los tres bloques contra lo que calcula el motor, empleado por empleado y renglón por
renglón. Cuando algo no coincide **no se corrige el motor de entrada**: primero se determina cuál
de los dos está mal. El rol de un cliente también puede tener errores, y de hecho es una de las
cosas que esta comparación suele encontrar.

Con 18 a 25 empleados esto es perfectamente abarcable a mano.

---

## Texto para enviarle al cliente

> Para terminar de calibrar el módulo de nómina necesitamos, de **enero de 2026**:
>
> 1. **El rol de pagos en Excel** (el archivo original, no impreso), con una fila por empleado y
>    los ingresos y descuentos **desglosados uno por uno**, incluyendo la cédula de cada persona
>    y el neto pagado. Si en el rol constan, también los aportes patronales (IESS patronal, IECE
>    y SECAP).
> 2. **La planilla del IESS** de ese mes, tal como se pagó.
> 3. **Un listado del personal** con: fecha de ingreso, sueldo vigente en enero, y para cada uno
>    si el **décimo tercero**, el **décimo cuarto** y los **fondos de reserva** se le pagan
>    mensualizados o acumulados. Añadir las cargas familiares declaradas y si presentó anexo de
>    gastos personales para 2026, con el monto.
> 4. **Los movimientos del mes**, si los hubo: ingresos, salidas, faltas sin sueldo o cambios de
>    sueldo, con sus fechas. Si no hubo ninguno, basta con que nos lo confirmen.
>
> El punto 3 es el que suele faltar y sin él no podemos reproducir el rol: dos empleados con el
> mismo sueldo cobran distinto según cómo tengan configurados los décimos y los fondos de
> reserva.
