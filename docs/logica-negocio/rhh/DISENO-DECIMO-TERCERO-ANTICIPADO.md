# Décimo tercero pagado por anticipado (equipo omen-saa-3, 2026-11-16)

Pedido del usuario: poder pagar el décimo tercero **antes del 30 de noviembre**, calculado **hasta el 30 de
noviembre** (la ley lo permite). Si el empleado deja de trabajar entre el pago y el 30 de noviembre, lo pagado por
anticipado **se le descuenta en su liquidación**.

Decisiones del usuario (2026-11-16):
1. Los meses sin cerrar se completan con el **rol calculado**: para pagar por anticipado el rol de noviembre tiene que
   estar al menos CALCULADO.
2. Si al cerrar noviembre el décimo real difiere de lo pagado, la diferencia va en el **rol de diciembre** (ingreso si
   falta, descuento si sobra), en una línea propia.
3. En el finiquito: el décimo proporcional como hoy **y** una línea de descuento por todo lo pagado por anticipado;
   la liquidación puede quedar en negativo (saldo por cobrar, mecanismo existente).

## Cómo está hoy (verificado en el código)
- `BeneficioSocialServiceImpl.calcularDecimoTercero` (~230): base = Σ `RHH.ACMN` tipo 3 (`BASE_DECIMO_TERCERO`) de
  dic(año-1) a nov(año), /12. ACMN se escribe **solo** al CERRAR el periodo (`ProcesoNominaServiceImpl.cerrarPeriodo`
  ~757-783), y cerrar exige PAGADO. **A mitad de noviembre falta noviembre (y octubre si no está cerrado): la base
  sale corta sin aviso.**
- El rol CALCULADO ya dejó la provisión del mes en `RHH.PVNM` (tipo 1 `DECIMO_TERCERO`, `PVNMBSCL` = base del mes)
  para los de modalidad ACUMULADO (`ProcesoNominaServiceImpl` ~1045-1057, `generaProvision`).
- Re-generar (`nuevoBeneficio` ~356-372) **sobrescribe `LQBSVLRR` aunque la LQBS esté PAGADA**.
- `OrdenBeneficioSocialServiceImpl.generar` solo toma LQBS sin orden y en estado 1; `confirmarPago` pone
  `LQBSVLPG`, estado 2, la baja de provisión y la novedad informativa (rol 32).
- Finiquito, `LiquidacionHaberesServiceImpl.calculaFiniquito` (~443-456): décimo proporcional = (Σ ACMN 3 de dic
  hasta el mes anterior a la salida + remuneración pendiente)/12, rol 23. **No lee `RHH.LQBS`**: quien sale tras el
  anticipo cobraría el décimo dos veces.

## A0 — El generador no pisa una LQBS ya pagada
En `generarDecimoTercero` (y el del cuarto, misma rutina `nuevoBeneficio`): si la LQBS existente está en estado
PAGADA (2) **o** tiene orden (`LQBSODBS` no nulo), **no se recalcula**: se deja como está y se cuenta en la respuesta
como «ya pagada / en orden, no se recalcula». Hoy eso es un defecto silencioso; con el anticipo se volvería frecuente.

## A1 — Cálculo anticipado
`POST /rest/lqbs/generarDecimoTercero/{anio}?idEmpresa&usuarioRegistro&anticipado=true` (parámetro nuevo,
opcional; sin él, idéntico a hoy).

Con `anticipado=true`:
1. **Precondición de periodos** (empresa, tipo de periodo mensual): para cada mes de la ventana dic(año-1)..nov(año)
   el periodo debe estar CERRADO (7) **o** al menos CALCULADO (3, 4, 5, 6). Noviembre es obligatorio (existir y estar
   ≥ CALCULADO). Si falta alguno → `IncomeException` «Para pagar el décimo por anticipado los roles de <meses> deben
   estar calculados» y no se genera nada.
2. **Base por empleado**: Σ ACMN tipo 3 de los meses CERRADOS **+** Σ `PVNM.PVNMBSCL` (tipo provisión 1, estado
   vigente) de los meses **no cerrados** de la ventana. Nunca los dos para un mismo mes (el mes cerrado va solo por
   ACMN). Valor = base/12, como hoy. Un empleado sin PVNM en noviembre (ingresó o salió) usa lo que tenga.
3. La LQBS queda marcada: `LQBSANTC = 'S'` y `LQBSFCCR` = fecha del cálculo. `fechaFin` sigue siendo 30-nov.
4. Mensualizados: igual que hoy, no generan LQBS.
5. Respuesta: la misma de hoy + cuántos ya pagados se saltaron (A0).

El resto del ciclo (orden, tesorería, `confirmarPago`) **no cambia**.

## A2 — Ajuste en el rol de diciembre
En el cálculo del rol (`ProcesoNominaServiceImpl`, el paso de renglones del empleado) cuando el periodo es
**diciembre** del año Y:
1. Si el empleado tiene LQBS tipo 1 (décimo tercero), año Y, `LQBSANTC = 'S'`, estado PAGADA y `LQBSVLAJ` nulo:
   - Si el periodo de **noviembre** Y no está CERRADO → el cálculo de diciembre **falla** con «Cierre el rol de
     noviembre antes de calcular diciembre: hay décimo tercero pagado por anticipado por conciliar».
   - real = `calcularDecimoTercero(empleado, Y)` (ya con noviembre cerrado, base exacta por ACMN).
   - dif = real − `LQBSVLPG`. Si |dif| ≥ 0,01: renglón con concepto rol **37** (dif > 0, ingreso) o **38** (dif < 0,
     descuento, valor = |dif|). Descripción «Ajuste décimo tercero <Y> pagado por anticipado».
2. Al **cerrar** diciembre: se graba en la LQBS `LQBSVLAJ` = dif (con signo, 0 si no hubo) y `LQBSPRAJ` = periodo de
   diciembre. Recalcular diciembre antes de cerrar vuelve a calcular el ajuste (el renglón se regenera como los demás).
3. Los conceptos 37/38 **no son base de nada** (IESS, IR, fondos, décimos, vacaciones, utilidades): el décimo no es
   aportable. Contabilidad: los dos van a la línea de plantilla de la provisión de décimo tercero por pagar
   (`RhhLineaAsiento.PROVISION_DECIMO_TERCERO_POR_PAGAR`), el 37 al debe de la provisión y el 38 al haber (lo
   inverso del 37), por la misma vía que hoy usa el rol 23. **Ninguna cuenta en el código.**

## A3 — Finiquito
En `calculaFiniquito`, después de la línea rol 23:
1. LQBS del décimo tercero del periodo que se está liquidando: tipo 1, **año = `anioPeriodo13` + 1** (la LQBS se
   identifica por el año del 30-nov), estado PAGADA, `LQBSVLPG` > 0.
2. Si existe: línea rol **39** «Décimo tercero pagado por anticipado» (concepto EGRESO) por `LQBSVLPG` (menos
   `LQBSVLAJ` si ya tuviera ajuste aplicado). Se aplica **aunque la causal no pague proporcionales** (lo pagado se
   descuenta igual).
3. Neto negativo: el mecanismo existente (saldo por cobrar, ~626-632) sin cambios.
4. Salida en **diciembre** (después del 30-nov, `anioPeriodo13` ya es el siguiente periodo): no se toca la LQBS
   anterior por A3; si tiene `LQBSANTC='S'` y `LQBSVLAJ` nulo, el finiquito agrega el ajuste de A2 (rol 37/38 con
   la misma regla) y lo graba en la LQBS al ejecutar la salida.
5. Contabilidad del rol 39: misma línea de plantilla que el rol 23, al haber de la provisión (inverso del 23):
   la provisión, que el anticipo debitó por el año completo, vuelve a su saldo devengado.

### Contabilidad — cómo quedó (2026-11-16, tras medir el código)
«La misma vía que el rol 23» no cabía tal cual: en el finiquito el lado lo fija la plantilla por línea y todo EGRESO
va a cuentas por cobrar empleados; en el rol mensual la provisión de décimo está en la plantilla de PROVISIÓN, no en
la del rol. Se resuelve así:
- **Finiquito (`contabilizarLiquidacion`):** el rol 39 va a `PROVISION_DECIMO_TERCERO_POR_PAGAR` con **valor
  negativo**, y `armaLineas`/`construyeLinea` pasan un valor negativo al **lado contrario** con `abs(valor)`
  (hoy no admiten negativos). Resultado: el haber de la provisión por lo pagado por anticipado.
- **Rol de diciembre (`importesDelRol` y los tres consumidores de la línea «fuera de plantilla del rol»:
  `contabilizarRol`, previsualizar, `validarCuentasContables`):** se generaliza el patrón de la baja de vacaciones
  gozadas (rol 36) a un valor **con signo**: neto = Σ37 − Σ38 contra la línea de provisión de décimo tercero de la
  plantilla de provisión; DEBE si es positivo, HABER si es negativo. El rol 36 sigue igual (siempre DEBE).
- En los dos casos el asiento cuadra por el neto/remuneraciones por pagar como hoy. Ninguna cuenta en el código.

### Precisiones de la implementación
- Precondición A1: noviembre es obligatorio (existir y ≥ CALCULADO); un mes ABIERTO/EN_CÁLCULO bloquea; un mes
  **sin periodo** en `RHH.PRDN` no bloquea (la apertura de dic-2025 se cargó por ACMN, sin periodo).
- «real» en A2/A3 se calcula con `valorDecimoTerceroReal` (lectura de ACMN), no con `calcularDecimoTercero`, que
  modifica la LQBS gestionada y pisaría `LQBSVLRR` de la ya pagada.
- Reabrir un periodo de diciembre limpia `LQBSVLAJ`/`LQBSPRAJ` de las LQBS ajustadas en él.
- A3: lo recibido por el empleado es `LQBSVLPG + LQBSVLAJ` (VLAJ con signo, nulo = 0).

Fuera de alcance, **para medir aparte**: el finiquito no mira la modalidad, y `cerrarPeriodo` escribe ACMN tipo 3
también para mensualizados, así que un mensualizado que sale podría cobrar el proporcional por segunda vez. Se
verifica con datos antes de tocarlo.

## SQL — `rhh/sql/e3-21-decimo-tercero-anticipado.sql` (antes del WAR)
- `RHH.LQBS`: `LQBSANTC VARCHAR2(1) DEFAULT 'N'`, `LQBSFCCR DATE`, `LQBSVLAJ NUMBER(18,2)`, `LQBSPRAJ NUMBER`
  (FK a `RHH.PRDN`).
- Rubro 221 (rol del concepto en el motor): detalles 37, 38, 39.
- `RHH.CPNM` por empresa que corre nómina, copiando del rol 6 (patrón `e2-26`): 37 INGRESO, 38 EGRESO, 39 EGRESO;
  todas las bases en 'N', no patronal, no provisiona, no obligatorio, no recortable, RDEP/IESS nulos.

## E — Frontend (`rrh/forms/procesos/pago-beneficios-sociales`)
- Casilla **«Pago anticipado (calcula hasta el 30 de noviembre)»**, visible solo con tipo = décimo tercero.
  Marcada → `anticipado=true` en «1. Calcular liquidaciones», con un aviso fijo: «Requiere el rol de noviembre
  calculado. Si cambia algo de noviembre, la diferencia se ajusta en el rol de diciembre. Si el empleado sale antes
  del 30 de noviembre, lo pagado se descuenta en su liquidación.»
- En la lista de liquidaciones: etiqueta «Anticipado» cuando `anticipado = 'S'`, y el aviso de la respuesta «N ya
  pagadas, no se recalculan».
- Liquidación (finiquito) y rol: sin cambio de pantalla (las líneas nuevas salen solas).
- Contrato: `saaFE/docs/rrh/API-PAGO-BENEFICIOS-SOCIALES.md`, sección nueva.

## Verificación
- `mvn -q compile`, `npx ng build`.
- Lectura del árbitro: A1 nunca suma ACMN y PVNM del mismo mes; A0 no recalcula pagadas; A3 busca año+1.
- Humo (solo lectura en producción): generar anticipado con noviembre calculado y comparar 2-3 empleados contra
  (Σ ACMN dic–oct + base del rol de noviembre)/12. El pago y el ajuste se verifican en el ciclo real.

## Autoría
RRHH en el alcance de omen-saa-3; sin trabajo de otro equipo en curso (confirmado por el usuario).
