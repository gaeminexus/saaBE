# Auditoría del pago de décimos contra la normativa

**Equipo:** `lap-saa-1` · **2026-09-08** · Módulo `rhh`
Pedido del usuario: *«revisa con la normativa si el desarrollo de pago de décimos estuvo correcto»*.
Verificado contra el código en `75054a89` y contra el Código del Trabajo (Arts. 95, 111 y 113).

---

## 0. Veredicto

**El cálculo está bien construido y es normativamente correcto en lo esencial.** Períodos, bases,
prorrateo, tope, regiones y mensualización coinciden con la ley. Ningún valor normativo está
escrito en el código: SBU, tasas y días base salen de `RHH.PRNM`, que es como debe ser.

**Un defecto real**, en un caso de borde que sí ocurre: el **cambio de modalidad a mitad de año**
(§2). **Un punto a confirmar** con la contadora (§3) y **una observación menor** (§4).

---

## 1. Lo que está correcto — verificado uno por uno

### 1.1 Décimo tercero (Art. 111)

| Qué pide la norma | Qué hace el sistema |
|---|---|
| Doceava parte de las remuneraciones percibidas | `base / 12` (`calcularDecimoTercero:231`) |
| Período 1-dic del año anterior al 30-nov | `MES_INICIO_DECIMO_TERCERO` a `MES_FIN_DECIMO_TERCERO`, ventana exacta |
| Proporcional si trabajó menos del año | Sale solo: la base acumulada es menor, y dividir para 12 da el proporcional |

**La base incluye lo que debe** y excluye lo que debe. El catálogo (`08_INSERT_CONCEPTOS_NOMINA.sql`)
marca `CPNMBSDT`:

| Entra a la base (`S`) | No entra (`N`) |
|---|---|
| Sueldo · Horas suplementarias 50 % · Horas extraordinarias 100 % · Recargo nocturno 25 % · Bono de responsabilidad · Comisiones · Vacaciones pagadas | Décimo tercero mensualizado · Décimo cuarto mensualizado · Fondos de reserva · Movilización · Alimentación · Subsidio IESS · Reintegro · **Utilidades** · Honorarios profesionales |

Contrastado con el Art. 95: la remuneración incluye *«lo que percibiere por trabajos extraordinarios
y suplementarios, a destajo, comisiones, participación en beneficios… o cualquier otra retribución
que tenga carácter normal»*, y **se exceptúan** el porcentaje legal de utilidades, los viáticos o
subsidios ocasionales, y la decimotercera y decimocuarta remuneraciones. **Todo cuadra** —
incluidas las exclusiones de movilización y alimentación como subsidios, y la de fondos de reserva.

### 1.2 Décimo cuarto (Art. 113)

| Qué pide la norma | Qué hace el sistema |
|---|---|
| Una SBU | `prnm.getSbu()`, del año del beneficio |
| Proporcional al tiempo trabajado | `SBU × días / díasAño`, con `PRNMDANO = 360` (año comercial) |
| Tope de una SBU | `if (valor > sbu) valor = sbu` |
| Sierra y Amazonía: ago–jul | `MES_INICIO_SIERRA` / `MES_FIN_SIERRA` |
| Costa e Insular: mar–feb | `MES_INICIO_COSTA` / `MES_FIN_COSTA`, con el último día de febrero resuelto por `lengthOfMonth()` — cubre bisiestos |
| Sólo quien tiene derecho | Exige `contrato.getDerechoDecimoCuarto() = 'S'` |

**El SBU es el correcto en las dos regiones.** Sierra paga el 15-ago del año N con el período que
cierra en jul del año N; Costa paga el 15-mar del año N con el período que cierra en feb del año N.
En ambos casos el SBU del año del beneficio es el vigente a la fecha de pago.

### 1.3 Mensualización (Art. 111 y 113, opción del trabajador)

- Décimo tercero mensualizado: `baseDec3 / 12` sobre la remuneración **del mes** — correcto.
- Décimo cuarto mensualizado: `(SBU / 12) × (días del mes / días base)` — correcto, prorratea el mes
  incompleto.
- Quien está mensualizado no entra al generador anual, y quien está acumulado genera provisión
  mensual. Los dos caminos existen y no se pisan.

---

## 2. 🔴 El defecto: cambiar de modalidad a mitad de año paga de más o de menos

`calcularDecimoTercero:238-244` tiene esta guarda, con su comentario:

```java
// Si el empleado cambio de modalidad a mitad de anio, ya cobro una parte en el
// rol. Ese valor esta en LQBSVLMN y se descuenta para no pagarlo dos veces.
Double mensualizado = beneficio.getValorMensualizado() != null ? ... : 0D;
beneficio.setValor(calculado - mensualizado);
```

**`LQBSVLMN` nunca se llena.** El único `setValorMensualizado` de todo el proyecto es el `0D` de
`nuevoBeneficio:337`, al crear el registro. Ningún proceso lo actualiza: ni el motor de nómina
cuando paga el décimo mensualizado, ni el generador anual. **La resta siempre resta cero.**

Y el generador filtra por la modalidad **actual** del contrato (`generarDecimoTercero:115-118`), no
por la que tuvo cada mes. De ahí salen dos casos, los dos silenciosos:

| Cambió de… | Qué pasa | A quién perjudica |
|---|---|---|
| **Mensualizado → acumulado** | El generador lo toma, calcula sobre los **12 meses** de la ventana —incluidos los que ya cobró en el rol— y le paga todo otra vez | **La empresa paga de más** |
| **Acumulado → mensualizado** | El generador lo **omite entero**, así que los meses en que fue acumulado no se le liquidan nunca | **El trabajador cobra de menos** |

**Lo mismo aplica al décimo cuarto**, que tiene la misma estructura (`calcularDecimoCuarto:302-306`).

**Por qué cuesta verlo:** el comentario del código describe correctamente lo que *debería* pasar, y
quien lo lee da por hecho que el campo se llena en algún lado. Es el patrón de siempre — **un lector
apuntando a donde nadie escribe** — pero agravado: acá hay una línea de código que *parece* resolver
el problema, así que nadie vuelve a mirarlo.

**Cuánto pesa depende de si el caso ocurre.** Si en esta empresa nadie cambió de modalidad, el
defecto está latente y no ha causado daño. `sql/lap1-12-verifica-decimos.sql` lo mide.

**Dos formas de corregirlo**, y la elección no me corresponde del todo porque cambia el esfuerzo:

- **Reconstruir lo mensualizado desde los roles**: sumar lo pagado con los conceptos de rol
  `DECIMO_TERCERO (6)` / `DECIMO_CUARTO (7)` dentro de la ventana, y restarlo. No necesita que nadie
  llene nada y funciona retroactivamente. Es lo que recomiendo.
- **Llenar `LQBSVLMN` desde el motor de nómina** cada vez que paga un décimo mensualizado. Más
  barato de escribir, pero sólo sirve de aquí en adelante: los años ya corridos quedan sin el dato.

---

## 3. 🟡 A confirmar con la contadora: «Vacaciones pagadas» en la base

El concepto **Vacaciones pagadas** está marcado `BSDT = 'S'`, o sea que suma a la base del décimo
tercero. **El Art. 95 no lo excluye**, y varias fuentes secundarias afirman que «las vacaciones no
forman parte del cálculo del décimo tercero».

La contradicción es aparente y depende de **qué representa ese concepto acá**:

- Si es **la remuneración del período de vacaciones gozadas** —el sueldo que se sigue pagando
  mientras el trabajador descansa—, incluirlo es **correcto y necesario**: excluirlo haría que
  tomarse vacaciones redujera el décimo tercero, que sería absurdo y contrario a la norma.
- Si es la **compensación por vacaciones no gozadas** que se paga en la liquidación, ahí sí es
  discutible y la práctica habitual la excluye.

**No lo toqué.** Es una pregunta de una línea para la contadora y no un defecto demostrado.

---

## 4. ⚪ Observación menor: un acumulado que nadie lee

`RhhTipoAcumulado.BASE_DECIMO_CUARTO` se escribe todos los meses
(`ProcesoNominaServiceImpl:715`) y **vale cero para todos**, porque ningún concepto tiene
`CPNMBSDC = 'S'` — ni siquiera el sueldo.

**No es un defecto**: el décimo cuarto no se calcula con esa base, sino con SBU y días trabajados,
que es lo que manda el Art. 113. Pero es un dato que se guarda, siempre en cero, y que invita a que
alguien lo use creyendo que significa algo. Vale documentarlo o dejar de escribirlo.

---

## 5. Lo que esta auditoría NO cubre

- **Los plazos legales de pago** (24-dic el décimo tercero; 15-ago y 15-mar el cuarto). El sistema
  calcula y liquida, pero no vigila la fecha. No es obligatorio que lo haga.
- **La declaración al MDT** de los pagos, que es un trámite aparte.
- **Los valores concretos ya pagados en producción.** Esto es una revisión del método, no un recálculo
  de los meses cerrados. Si querés contrastar lo pagado contra lo que la norma da, es otro trabajo y
  se hace con los acumulados reales.
