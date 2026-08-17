# REGLAS DEL PADRÓN DE PARTÍCIPES

**Vigente al 2026-08-17 · módulo CRD**

Genera el listado de partícipes con su estado de aportes, si está habilitado para votar y si es
elegible como miembro. Es la fuente para el padrón electoral de la asociación.

Todo se resuelve en **una sola consulta nativa** dentro de
`EntidadDaoServiceImpl.selectPadronParticipes` — a propósito, para no hacer N+1 sobre `CRD.APRT`
ni sobre `CRD.DTPR`. Este documento es la referencia vigente; cualquier cambio en ese método debe
actualizarlo en el mismo commit.

---

## 1. Endpoint

```
GET /SaaBE/rest/entd/padron-participes
```

| Parámetro | Tipo | Obligatorio | Default |
|---|---|---|---|
| `fechaEjecucion` | `yyyy-MM-dd` | No | Hoy |
| `calidadId` | `Long` | No | null = todas las calidades |
| `minimoAportes` | `Long` | No | `90` |

Respuesta `200 OK` con un array de `PadronParticipeDTO`. Los errores devuelven `500` con texto
plano `Error al obtener el padrón de partícipes: <mensaje>` (estilo de la casa).

Capas: `EntidadRest` → `EntidadService` (aplica los defaults) → `EntidadDaoService` (toda la
lógica está en el SQL). Un padrón vacío es un resultado válido: **no** lanza `IncomeException`.

---

## 2. La fecha de corte: hay dos, y son distintas a propósito

| Bloque de columnas | Corte | Por qué |
|---|---|---|
| Aportes y mora de aportes | **Cierre del mes anterior** a `fechaEjecucion` | Los aportes se graban con fecha del último día del mes, así que el mes en curso todavía no tiene su carga procesada. Contarlo daría a todo el mundo como si no hubiera aportado. |
| Préstamos en mora y cuotas en mora | **Inicio del día** de `fechaEjecucion` | El estado del préstamo (`PRSTIDST`) es un dato vivo: no se puede reconstruir al mes pasado. Se usa el mismo corte del proceso diario de mora. |
| `habilitadoVoto` y `elegibleMiembro` | **Los dos** | Cada uno combina una condición de aportes con el tope de cuotas en mora, así que hereda ambos cortes. |

Consecuencia buscada del primer corte: **el bloque de aportes devuelve lo mismo se corra el día 5
o el 28 del mes.** El bloque de préstamos sí cambia día a día, y desde el 2026-08-17 arrastra con
él al voto y a la elegibilidad.

Con `fechaEjecucion = 2026-08-17`:

```
mesReferencia   = 2026-07-01   (el mes que se evalúa)
corteAportes    < 2026-08-01   (aportes hasta el 31-jul inclusive)
primerMesAlDia  = 2026-02-01   (mesReferencia − 5 meses)
corteCuotas     < 2026-08-17 00:00
```

---

## 3. Universo de filas

`CRD.ENTD` completa, con dos filtros:

| Filtro | Detalle |
|---|---|
| Cédula válida | `NVL(TRIM(ENTDNMID), '0') <> '0'` — descarta registros sin identificación |
| Calidad | `ENTDIDST = :calidadId` cuando se envía el parámetro; sin él entran todas |

Ordenado por nombre (`UPPER(ENTDRZNS)`) y, a igual nombre, por `ENTDCDGO`. El campo `numero` es
un `ROW_NUMBER()` sobre ese mismo orden: es el número de fila del padrón, **no** un id estable.

---

## 4. Número de aportes

```sql
COUNT(DISTINCT TRUNC(APRTFCTR, 'MM'))
FROM CRD.APRT
WHERE TPAPCDGO IN (9, 11) AND APRTVLRR > 0 AND APRTFCTR < corteAportes
```

- Tipos de aporte: **9 = JUBILACIÓN, 11 = CESANTÍA**. Ningún otro tipo cuenta.
- Solo aportes con valor **positivo**.
- Se cuentan **meses distintos**, no filas: un mes con tres aportes cuenta como uno.
- Sin aportes: `0` (no null).

---

## 5. Estado de mora de aportes

```
mesesEnMora = MONTHS_BETWEEN(mesReferencia, último mes con aporte)
estadoMora  = 'EN MORA' si el último aporte es anterior a primerMesAlDia
```

La ventana es de **6 meses** (`MESES_VENTANA_MORA = 6`): se cae EN MORA al acumular **6 meses
consecutivos sin aportar** respecto del mes de referencia.

Con referencia julio 2026 (`primerMesAlDia` = febrero 2026):

| Último aporte | Meses sin aportar | `estadoMora` | `mesesEnMora` |
|---|---|---|---|
| Julio 2026 | 0 | AL DIA | 0 |
| Marzo 2026 | 4 | AL DIA | 4 |
| Febrero 2026 | 5 | AL DIA | 5 |
| Enero 2026 | **6** | **EN MORA** | 6 |
| Nunca aportó | — | **EN MORA** | `null` |

Dos detalles que confunden si no se tienen presentes:

- **`mesesEnMora` es el desfase real, no la mora.** Una fila puede decir `AL DIA` y mostrar 5
  meses: son cosas distintas, el campo no depende de la tolerancia.
- **`mesesEnMora` viene `null` cuando el partícipe nunca aportó**, porque no hay un último aporte
  desde el cual contar. JSON-B omite los null, así que el campo **no aparece** en el JSON: el
  frontend debe tratar "ausente" como "nunca aportó", no como 0. El estado igual es `EN MORA`.
- El `−1` de `mesReferencia.minusMonths(MESES_VENTANA_MORA − 1)` es lo que pone el borde donde
  debe: sin él, quien lleva exactamente 5 meses sin aportar ya saldría EN MORA.

---

## 6. Préstamos en mora (2 columnas)

| Columna | Valor |
|---|---|
| `tienePrestamoMora` | `SI` si el partícipe tiene **algún** `CRD.PRST` con `PRSTIDST IN (11 EN_MORA, 8 DE_PLAZO_VENCIDO)`; `NO` en caso contrario |
| `maximoCuotasMora` | Cuotas en mora del **peor** de esos préstamos; `0` si no tiene ninguno |

Es `PRSTIDST`, **no** `ESPSCDGO` — ver la tabla de "qué columna lleva el estado" en `CLAUDE.md`.
`ESPSCDGO` es la FK al catálogo `CRD.ESPS`, no el estado operativo.

**Qué cuenta como cuota en mora** (mismo criterio de `selectCuotasVencidasByPrestamo` y del
proceso diario de mora, para que los números coincidan):

```sql
(DTPRESTD IS NULL OR DTPRESTD NOT IN (4 PAGADA, 7 CANCELADA_ANTICIPADA))
AND DTPRFCVN < corteCuotas
```

La cuota que vence hoy **todavía no está en mora**. Las cuotas `PARCIAL` sí cuentan: recibieron un
pago pero siguen impagas.

**Se toma el máximo, no la suma.** Ejemplo del caso típico: un partícipe con dos préstamos, uno
EN MORA con 8 cuotas y otro DE PLAZO VENCIDO con 4, devuelve `SI` y **8**.

Un préstamo marcado en mora que no tenga ninguna cuota vencida (datos inconsistentes) igual marca
`SI` con `0` cuotas: el `LEFT JOIN` del SQL es deliberado, la bandera sale del estado del préstamo
y el conteo se deriva aparte.

---

## 7. Habilitado para voto

```
habilitadoVoto = SI  ⟺  calidad ACTIVO (ENTDIDST = 1)
                     Y  estadoMora = 'AL DIA'
                     Y  maximoCuotasMora <= 6
```

El `1` es el **código alterno** (`ESPRCDEX`), no el PK del catálogo (que para ACTIVO es 10). Ver
§10 y `MIGRACION-ESTADO-PARTICIPE.md`.

El tercer requisito se agregó el **2026-08-17**: estar al día en aportes no alcanza si se
arrastran **más de 6 cuotas en mora** de préstamo. Es el mismo tope de la elegibilidad (§8), la
misma constante `MAXIMO_CUOTAS_MORA_ELEGIBLE = 6`.

| `estadoMora` | `maximoCuotasMora` | Calidad | `habilitadoVoto` |
|---|---|---|---|
| AL DIA | 0 | ACTIVO | SI |
| AL DIA | 6 | ACTIVO | SI |
| AL DIA | 7 | ACTIVO | **NO** (por préstamos) |
| EN MORA | 0 | ACTIVO | NO (por aportes) |
| AL DIA | 0 | CESANTE | NO (por calidad) |

---

## 8. Elegible como miembro

```
elegibleMiembro = SI  ⟺  calidad ACTIVO (ENTDIDST = 1)
                      Y  numeroAportes >= minimoAportes   (default 90)
                      Y  maximoCuotasMora <= 6
```

El tercer requisito se agregó el **2026-08-17** y **manda sobre el segundo**: quien cumple el
mínimo de aportes pero arrastra **más de 6 cuotas en mora** (7 en adelante) deja de ser elegible.
La elegibilidad se pierde por deuda, no solo por aportes. El mismo tope aplica al voto (§7).

Tope en `MAXIMO_CUOTAS_MORA_ELEGIBLE = 6` (`EntidadDaoServiceImpl`), una sola constante para las
dos columnas. Con exactamente 6 cuotas en mora **sigue siendo elegible**; el corte es estricto
(`> 6` descalifica).

| `numeroAportes` | `maximoCuotasMora` | Calidad | `elegibleMiembro` |
|---|---|---|---|
| 142 | 0 | ACTIVO | SI |
| 142 | 6 | ACTIVO | SI |
| 142 | 7 | ACTIVO | **NO** (por préstamos) |
| 142 | 8 | ACTIVO | **NO** (por préstamos) |
| 40 | 0 | ACTIVO | NO (por aportes) |
| 142 | 0 | CESANTE | NO (por calidad) |

---

## 9. Estructura de la respuesta

`com.saa.model.crd.dto.PadronParticipeDTO`:

| Campo | Tipo | Origen |
|---|---|---|
| `numero` | Long | `ROW_NUMBER()` sobre el orden del padrón |
| `entidadId` | Long | `ENTDCDGO` (trazabilidad contra la base) |
| `cedula` | String | `ENTDNMID` |
| `nombresApellidos` | String | `ENTDRZNS` |
| `calidadParticipeId` | Long | `ENTDIDST` |
| `calidadParticipe` | String | `ESPRNMBR`, o `SIN ESTADO` si no hay match en `CRD.ESPR` |
| `numeroAportes` | Long | §4 |
| `estadoMora` | String | `AL DIA` / `EN MORA`, §5 |
| `mesesEnMora` | Long | §5. **null si nunca aportó** |
| `habilitadoVoto` | String | `SI` / `NO`, §7 |
| `elegibleMiembro` | String | `SI` / `NO`, §8 |
| `correo` | String | `ENTDCRIN` y `ENTDCRPR` unidos por `"; "`, o el que exista |
| `tienePrestamoMora` | String | `SI` / `NO`, §6 |
| `maximoCuotasMora` | Long | §6, `0` si no aplica |

Ejemplo (JSON-B ordena las propiedades alfabéticamente):

```json
[
  {
    "calidadParticipe": "ACTIVO",
    "calidadParticipeId": 1,
    "cedula": "1712345678",
    "correo": "jperez@petroecuador.ec; juan.perez@gmail.com",
    "elegibleMiembro": "NO",
    "entidadId": 4521,
    "estadoMora": "AL DIA",
    "habilitadoVoto": "NO",
    "maximoCuotasMora": 8,
    "mesesEnMora": 0,
    "nombresApellidos": "PEREZ LOPEZ JUAN CARLOS",
    "numero": 1,
    "numeroAportes": 142,
    "tienePrestamoMora": "SI"
  }
]
```

Esa fila resume el caso que más se pregunta: partícipe ACTIVO, al día en aportes y con 142
aportes acumulados que, pese a todo eso, **ni vota ni es elegible**, porque arrastra 8 cuotas en
mora (más de 6). El 8 sale del peor de sus préstamos, no de la suma.

---

## 10. Trampas

- **Las columnas de préstamos no se pueden reproducir hacia atrás.** Si se reejecuta el padrón con
  una `fechaEjecucion` pasada, los aportes salen iguales pero los préstamos reflejan el estado de
  **hoy**. No sirve como evidencia histórica de la deuda — y como el voto y la elegibilidad ahora
  dependen de las cuotas en mora, **un padrón reimpreso puede no coincidir con el que se usó el
  día de la elección**. Si el padrón electoral tiene que ser inmutable, hay que guardar el
  resultado, no reejecutarlo.
- **`ESPRCDEX`, no `ESPRCDGO`.** El join con el catálogo de calidades es
  `esp.ESPRCDEX = e.ENTDIDST`: `ENTDIDST` guarda el **código alterno** (ACTIVO = 1), no el PK
  (ACTIVO = 10). Filtrar `calidadId` con el PK devuelve 0 filas sin dar error. Es el mismo
  precedente documentado en `MIGRACION-ESTADO-PARTICIPE.md`, y por eso `calidadParticipeId` en
  el DTO es el alterno; el javadoc del campo que dice `ESPRCDGO` está desactualizado.
- **El conteo de cuotas depende de que el proceso diario de mora esté corriendo**, porque de él
  sale el `PRSTIDST = 11`. Si el proceso no corrió, préstamos con cuotas vencidas pueden seguir en
  `VIGENTE`: esas filas salen con `NO` / `0` y el partícipe conserva voto y elegibilidad que no le
  tocan. Antes de generar un padrón oficial conviene verificar que el proceso corrió. Ver
  `PROCESO-DIARIO-INTERES-MORA.md`.
- **`minimoAportes` se ignora si llega ≤ 0**; el service cae al default 90.

---

## 11. Historial

| Fecha | Cambio |
|---|---|
| 2026-08-17 | `habilitadoVoto` pasa a `NO` con más de 6 cuotas en mora, mismo tope que la elegibilidad (§7) |
| 2026-08-17 | `elegibleMiembro` pasa a `NO` con más de 6 cuotas en mora (§8) |
| 2026-08-17 | Nuevas columnas `tienePrestamoMora` y `maximoCuotasMora` (§6) |
| 2026-08-17 | Ventana de mora de aportes de 1 a **6 meses** (§5) |
| 2026-08-16 | Ventana de mora de 2 a 1 mes |
| — | Versión inicial del padrón (commit `55a8457` "Reporte padron") |
