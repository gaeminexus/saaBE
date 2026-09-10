# Especificación — padrón de partícipes: voto estricto y «Mantiene Calidad»

**Equipo:** `omen-saa-1` (omen1) · **Fecha:** 2026-09-09 · **Pedido por:** el usuario, urgente.
**Decisiones tomadas por el usuario en esta misma fecha.** Complementa
`REGLAS-PADRON-PARTICIPES.md`, que es la referencia vigente y debe actualizarse en el mismo commit.

Todo vive en **una sola consulta nativa**: `EntidadDaoServiceImpl.selectPadronParticipes`.
El padrón no tiene tabla en pantalla: se descarga como CSV desde `entidad-consulta`.

---

## 1. Los tres cambios

### 1.1 `estadoMora` — EN MORA con CUALQUIER atraso

```
ANTES:  estadoMora = 'EN MORA'  ⟺  último aporte anterior a primerMesAlDia (6 meses)
AHORA:  estadoMora = 'EN MORA'  ⟺  mesesEnMora > 0   (o nunca aportó)
        estadoMora = 'AL DIA'   ⟺  mesesEnMora = 0
```

Hoy un partícipe con 5 meses sin aportar figura **AL DIA**. Con el cambio figura **EN MORA**, que
es lo que el usuario pidió: *«si el partícipe está en mora la columna de estado de mora indique
que está en mora»*.

### 1.2 `habilitadoVoto` — sin un solo mes ni una sola cuota de atraso

Decisión textual del usuario: *«para poder votar, tiene que estar activo y debe tener todas sus
obligaciones al día. Si está atrasado en aportes o préstamo así sea un mes ya no puede votar»*.

```
habilitadoVoto = 'SI'  ⟺  calidad ACTIVO (ENTDIDST = 1, el alterno ESPRCDEX)
                       Y  mesesEnMora = 0                (cero atraso en aportes)
                       Y  maximoCuotasMora = 0           (cero cuotas de préstamo en mora)
                       Y  tienePrestamoMora = 'NO'       (ningún préstamo marcado en mora)
```

**Las dos últimas condiciones no son redundantes.** `REGLAS-PADRON-PARTICIPES.md` §6 documenta un
caso real: un préstamo marcado `EN_MORA`/`DE_PLAZO_VENCIDO` que **no tiene ninguna cuota vencida**
—dato inconsistente— sale hoy con `tienePrestamoMora = SI` y `maximoCuotasMora = 0`. Con sólo la
condición de cuotas, ese partícipe votaría teniendo un préstamo marcado en mora. Se exigen las dos
porque el usuario pidió «todas sus obligaciones al día», y ese caso no lo está.

⚠️ **Esto reemplaza el tope de 6 cuotas que regía desde el 2026-08-17.** Antes se toleraban hasta
6 cuotas en mora para votar; ahora no se tolera ninguna.

### 1.3 Columna nueva `mantieneCalidadParticipe`

```
mantieneCalidadParticipe = 'SI'  ⟺  mesesEnMora <= 6
                         = 'NO'  ⟺  mesesEnMora > 6  ó  nunca aportó (null)
```

Encabezado en el CSV: **`Mantiene Calidad`**.

**Por qué no se llama «Calidad de Partícipe», que es como la pidió el usuario:** ya existe una
columna `calidadParticipe` con valores `ACTIVO / CESANTE / …` (de `ESPRNMBR`), más su
`calidadParticipeId`. Dos columnas con nombres casi idénticos y contenidos distintos (una de
catálogo, otra SI/NO) en el mismo Excel es una confusión que después nadie desarma. El usuario
eligió `Mantiene Calidad` el 2026-09-09.

## 2. Casos borde — resolverlos así, no por criterio propio

| Caso | `estadoMora` | `habilitadoVoto` | `mantieneCalidad` |
|---|---|---|---|
| Nunca aportó (`mesesEnMora` **null**) | EN MORA | NO | **NO** |
| 0 meses, ACTIVO, sin préstamos en mora | AL DIA | **SI** | SI |
| 0 meses, ACTIVO, **1** cuota en mora | AL DIA | **NO** | SI |
| 1 mes de atraso, ACTIVO, sin préstamos | **EN MORA** | **NO** | SI |
| 6 meses de atraso | EN MORA | NO | **SI** (el 6 entra) |
| 7 meses de atraso | EN MORA | NO | **NO** |
| 0 meses, **CESANTE**, sin préstamos | AL DIA | NO (por calidad) | SI |

⚠️ **`mesesEnMora` null es «nunca aportó», no «cero»**, y el SQL debe tratarlo explícitamente. Un
`NULL <= 6` en Oracle no es verdadero ni falso: es desconocido, y el `CASE` cae al `ELSE`. Sale
`NO`, que es lo correcto acá — pero conviene que sea explícito y no un accidente feliz.

## 3. Consecuencia visible, y NO es un error

Van a aparecer muchas filas que digan **`Estado Mora = EN MORA`** y a la vez
**`Mantiene Calidad = SI`**. Es correcto y es el punto de tener dos columnas: se cae en mora con
un mes de atraso, pero la condición de partícipe sólo se pierde pasando los 6. Quien lea el Excel
esperando que las dos digan lo mismo va a reportarlo como defecto.

## 4. Lo que NO cambia

- **`elegibleMiembro`** conserva su regla: ACTIVO + `numeroAportes >= minimoAportes` (default 90)
  + `maximoCuotasMora <= 6`. El usuario no pidió tocarla.
  ⚠️ Queda una asimetría deliberada: para **votar** no se tolera ninguna cuota en mora, para ser
  **elegible como miembro** se toleran hasta 6. Si eso no es lo querido, es una decisión aparte.
- **`mesesEnMora`** sigue siendo el desfase real en meses. No se toca su cálculo.
- **`numeroAportes`**, los dos cortes de fecha (§2 de las reglas) y el universo de filas.
- La constante `MESES_VENTANA_MORA = 6` **sigue usándose** para `mantieneCalidadParticipe`; lo que
  deja de usarse es su rol en `estadoMora`.

## 5. Impacto — se mide HOY, sin SQL y sin desplegar

El cambio **reduce** la cantidad de habilitados para votar, y puede reducirla mucho: hoy vota
quien tenga hasta 5 meses de atraso en aportes y hasta 6 cuotas de préstamo en mora.

**No hace falta ningún script para saber cuánto.** El CSV que la pantalla ya descarga trae todas
las columnas necesarias para simular la regla nueva sobre el padrón actual:

1. Descargar el padrón **antes** de desplegar (botón de elegibles en `entidad-consulta`).
2. Contar las filas con `Habilitado Voto = SI` → **votantes de hoy**.
3. Contar las filas con `Calidad Partícipe = ACTIVO` **y** `Meses en Mora = 0` **y**
   `CUOTAS EN MORA = 0` **y** `PRESTAMOS EN MORA = No` → **votantes con la regla nueva**.

La diferencia es exactamente la gente que deja de votar. Se resuelve con un filtro de Excel.

⭐ **Se descartó escribir un `.sql` de impacto a propósito.** Habría que replicar en SQL el cálculo
de aportes por periodo efectivo y de `mesesEnMora` que hoy vive en el Java de
`selectPadronParticipes`; una réplica que se desvíe un poco del original da un número que parece
medido y no lo es. El CSV sale del mismo código que se va a cambiar, así que es exacto por
construcción. Guardar ese CSV **antes** del despliegue es además la única forma de comparar
después.
