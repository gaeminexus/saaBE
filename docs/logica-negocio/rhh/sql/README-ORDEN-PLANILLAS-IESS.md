# Orden de ejecución — planillas del IESS (`lap-saa-1`)

**2026-09-07.** Los tres scripts y el WAR tienen dependencias entre sí. Ejecutarlos fuera de este
orden no da un error claro en el momento: rompe una pantalla o la contabilización de un período,
más tarde y lejos de la causa.

---

## El orden

| # | Qué | Depende de | Si se saltea |
|---|---|---|---|
| 1 | `lap1-08-planilla-iess.sql` | nada | — |
| 2 | `lap1-10-linea-asiento-prestamos-hipotecarios.sql` | nada | **La contabilización de nómina falla** en cualquier período con descuento hipotecario, apenas suba el WAR |
| 3 | `lap1-09-productos-pago-iess.sql` | del **2**, para la cuenta de `IESS-PRSH` | El pago de una planilla se rechaza por falta de producto |
| 4 | **WAR** (`b4eb234d` o posterior) | de **1** y **2** | Ver abajo |
| 5 | Frontend | del **4** | Pantallas contra endpoints que no existen |

---

## Por qué el WAR va después de 1 y 2

**Después de `lap1-08`**, porque las entidades `PlanillaIess` y `DetallePlanillaIess` mapean columnas
de `RHH.PLIS` y `RHH.DLIS`. Hibernate nombra **toda** columna `@Column` en el `SELECT` que genera, así
que con el WAR primero cualquier lectura de esas tablas muere con `ORA-00904` — y no al desplegar,
sino cuando un usuario abra la pantalla.

**Después de `lap1-10`**, y ésta es la que muerde más lejos: el WAR trae la línea de asiento 19 para
los préstamos hipotecarios. `ContabilizacionNominaServiceImpl.exigeLinea:1265-1274` lanza
`IncomeException` si el período tiene valores para una línea que la plantilla contable no define. O
sea que **contabilizar un período con al menos un descuento hipotecario falla** hasta que la 19 esté
parametrizada. Falla con mensaje claro, pero falla — y lo hace en el cierre de nómina, que es el peor
momento para descubrirlo.

Si en ese período no hay ningún descuento hipotecario, no pasa nada. Por eso puede parecer que todo
está bien durante un mes entero.

---

## Los tres tienen bloques comentados a propósito

`lap1-09` y `lap1-10` **no se corren de corrido**: sus `INSERT` dependen de códigos de cuenta que
cambian por instalación —esto es un producto multicliente, no la base de un solo cliente— y de dos
decisiones contables. Cada uno arranca con sus `CONTROL` en `SELECT`: correr ésos primero, revisar lo
que devuelven, completar los valores y recién entonces descomentar.

`lap1-08` sí corre de corrido, pero **frenar en el CONTROL 2** y confirmar que el bloque de rubros
330-349 / 1600-1699 siga libre antes de seguir.

---

## Lo que queda pendiente de la contadora

Ninguna de las dos frena la ejecución de arriba; las dos frenan que el sistema quede **completo**.

1. **Seguro de salud de tiempo parcial** (4,41 % sobre `SBU − sueldo real`): no se provisiona en la
   nómina — verificado. ¿Se reconoce como gasto al pagarlo, o se provisiona? Hasta que se decida, el
   grupo de `IESS-STP` no se crea y el pago de una planilla de rol con ese renglón se rechaza
   citándolo, que es lo correcto: mejor eso que debitar una cuenta elegida al azar.
2. **El saldo histórico de la cuenta de préstamos**: todo lo acumulado hasta hoy está mezclado entre
   quirografarios e hipotecarios. Separar de aquí en adelante no lo separa. ¿Se reclasifica o se deja
   arrastrar hasta consumirse?

Y una que **no** es de la contadora y ya está resuelta: el **CCC** (Contribución de Fomento de
Capacidades y Conocimientos Ciudadanos, 1 % de la masa) **sí se provisiona**, con el nombre de sus dos
destinatarios legales — IECE 0,5 % y SECAP 0,5 % —, y acredita la cuenta del aporte patronal. Su
producto apunta a esa misma cuenta.
