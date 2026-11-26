# C46-01 · Cargas de Petro con diferencia entre lo cargado y el reporte (producción)

**Solo lectura.** No modifica nada. Corre en una sesión interactiva de producción.
Va con la corrección C46 (`C46-LINEA-SIN-ROL-RETORNO-PETRO.md`): el cargador descartaba la segunda línea de un
partícipe que tiene dos descuentos en el mismo producto (la que Petro imprime sin rol ni nombre, seguida de
«TOTAL EMPLEADO»). Decisiones D-217, D-218 y D-220 del proyecto de depuración.

## 1. Por qué hay que poner los totales a mano

La base no guarda lo que dice el reporte de Petro: `CRD.CRAR.CRARTTDO` y `CRD.DTCA.DTCATTDO` son la **suma de lo
que se cargó**, no el «TOTAL ==>» ni los subtotales del archivo. Por eso el control lleva los totales del reporte
escritos en el `WITH`. Los de marzo a agosto de 2026 ya están, leídos de los retornos de Petro (son totales por
sección, no traen datos de partícipes). Para septiembre de 2026 en adelante, agrega una fila por mes con los valores
de las líneas «TOTAL <producto>» y «TOTAL ==>» del archivo, en la columna **T O T A L** descontado.

Desde que esté en producción el EAR con la C46 y el script `crd/sql/411`, cada carga nueva deja la diferencia en
`CRD.CRAR.CRARALCD` (alerta de cuadre), y basta con la consulta de la sección 4.

Totales del reporte (columna total descontado):

| Mes 2026 | AH | HS | PE | PH | PQ | TOTAL ==> | Falta en la carga actual |
|---|---:|---:|---:|---:|---:|---:|---|
| Marzo | 123.792,32 | 448,00 | 118.133,90 | 118.632,27 | 9.570,71 | 370.577,20 | 20,00 en PE: lo trae el reporte de Petro y ninguna línea lo explica. No es de la C46. |
| Abril | 120.396,26 | 2.392,00 | 110.587,39 | 113.433,61 | 8.689,00 | 355.498,26 | 866,00 en 2 líneas (PE 177,00; PH 689,00) |
| Mayo | 121.284,08 | 1.459,00 | 111.204,49 | 115.262,15 | 9.359,00 | 358.568,72 | 966,00 en 3 líneas (PE 277,00; PH 689,00) |
| Junio | 121.117,42 | 1.528,99 | 108.851,76 | 111.714,29 | 8.793,35 | 352.005,81 | 276,67 en 2 líneas (PE) |
| Julio | 120.657,06 | 1.337,31 | 116.337,10 | 113.868,92 | 10.524,63 | 362.725,02 | 115,82 en 1 línea (PE) |
| Agosto | 116.804,80 | 2.419,36 | 114.795,81 | 110.156,67 | 10.314,73 | 354.491,37 | nada |

De junio de 2025 a febrero de 2026 la carga cuadra con el reporte.

## 2. Control por carga y por producto

```sql
-- C46-01 · cargas Petro (filial 1) con diferencia contra el reporte, por producto. Solo lectura.
WITH reporte (anio, mes, producto, total_reporte) AS (
          SELECT 2026, 3, 'AH', 123792.32 FROM dual UNION ALL SELECT 2026, 3, 'HS',    448.00 FROM dual
UNION ALL SELECT 2026, 3, 'PE', 118133.90 FROM dual UNION ALL SELECT 2026, 3, 'PH', 118632.27 FROM dual
UNION ALL SELECT 2026, 3, 'PQ',   9570.71 FROM dual
UNION ALL SELECT 2026, 4, 'AH', 120396.26 FROM dual UNION ALL SELECT 2026, 4, 'HS',   2392.00 FROM dual
UNION ALL SELECT 2026, 4, 'PE', 110587.39 FROM dual UNION ALL SELECT 2026, 4, 'PH', 113433.61 FROM dual
UNION ALL SELECT 2026, 4, 'PQ',   8689.00 FROM dual
UNION ALL SELECT 2026, 5, 'AH', 121284.08 FROM dual UNION ALL SELECT 2026, 5, 'HS',   1459.00 FROM dual
UNION ALL SELECT 2026, 5, 'PE', 111204.49 FROM dual UNION ALL SELECT 2026, 5, 'PH', 115262.15 FROM dual
UNION ALL SELECT 2026, 5, 'PQ',   9359.00 FROM dual
UNION ALL SELECT 2026, 6, 'AH', 121117.42 FROM dual UNION ALL SELECT 2026, 6, 'HS',   1528.99 FROM dual
UNION ALL SELECT 2026, 6, 'PE', 108851.76 FROM dual UNION ALL SELECT 2026, 6, 'PH', 111714.29 FROM dual
UNION ALL SELECT 2026, 6, 'PQ',   8793.35 FROM dual
UNION ALL SELECT 2026, 7, 'AH', 120657.06 FROM dual UNION ALL SELECT 2026, 7, 'HS',   1337.31 FROM dual
UNION ALL SELECT 2026, 7, 'PE', 116337.10 FROM dual UNION ALL SELECT 2026, 7, 'PH', 113868.92 FROM dual
UNION ALL SELECT 2026, 7, 'PQ',  10524.63 FROM dual
UNION ALL SELECT 2026, 8, 'AH', 116804.80 FROM dual UNION ALL SELECT 2026, 8, 'HS',   2419.36 FROM dual
UNION ALL SELECT 2026, 8, 'PE', 114795.81 FROM dual UNION ALL SELECT 2026, 8, 'PH', 110156.67 FROM dual
UNION ALL SELECT 2026, 8, 'PQ',  10314.73 FROM dual
-- septiembre 2026 en adelante: agregar aquí las filas del archivo de cada mes
),
cargado AS (
  SELECT c.CRARCDGO, c.CRARANAF anio, c.CRARMSAF mes, c.CRARESTD estado_carga, c.FLLLCDGO filial,
         TRIM(d.DTCACDPP) producto, COUNT(p.PXCACDGO) lineas, ROUND(NVL(SUM(p.PXCADSDO), 0), 2) total_cargado
    FROM CRD.CRAR c
    JOIN CRD.DTCA d ON d.CRARCDGO = c.CRARCDGO
    LEFT JOIN CRD.PXCA p ON p.DTCACDGO = d.DTCACDGO
   WHERE c.FLLLCDGO = 1
     AND (c.CRARANAF * 100 + c.CRARMSAF) >= 202603
   GROUP BY c.CRARCDGO, c.CRARANAF, c.CRARMSAF, c.CRARESTD, c.FLLLCDGO, TRIM(d.DTCACDPP)
)
SELECT k.CRARCDGO, k.anio, k.mes, k.estado_carga, k.producto, k.lineas,
       k.total_cargado, r.total_reporte,
       ROUND(NVL(r.total_reporte, 0) - k.total_cargado, 2) diferencia
  FROM cargado k
  LEFT JOIN reporte r ON r.anio = k.anio AND r.mes = k.mes AND r.producto = k.producto
 ORDER BY k.anio, k.mes, k.CRARCDGO, k.producto;
```

Qué esperar:
- Abril–julio: diferencia positiva en PE y/o PH igual a la columna «Falta» de la tabla. En AH, HS y PQ, 0,00.
- Marzo: PE con 20,00. Es del reporte de Petro, no de la carga.
- Si un mes tiene más de una carga (anuladas, reprocesos), mira `estado_carga` y quédate con la vigente.
- `total_reporte` nulo quiere decir que falta poner los totales de ese mes en el `WITH`.

## 3. Total de la carga contra «TOTAL ==>»

```sql
WITH reporte_total (anio, mes, total_reporte) AS (
          SELECT 2026, 3, 370577.20 FROM dual UNION ALL SELECT 2026, 4, 355498.26 FROM dual
UNION ALL SELECT 2026, 5, 358568.72 FROM dual UNION ALL SELECT 2026, 6, 352005.81 FROM dual
UNION ALL SELECT 2026, 7, 362725.02 FROM dual UNION ALL SELECT 2026, 8, 354491.37 FROM dual
)
SELECT c.CRARCDGO, c.CRARANAF anio, c.CRARMSAF mes, c.CRARESTD estado_carga,
       ROUND(c.CRARTTDO, 2) total_carga, r.total_reporte,
       ROUND(NVL(r.total_reporte, 0) - c.CRARTTDO, 2) diferencia
  FROM CRD.CRAR c
  LEFT JOIN reporte_total r ON r.anio = c.CRARANAF AND r.mes = c.CRARMSAF
 WHERE c.FLLLCDGO = 1 AND (c.CRARANAF * 100 + c.CRARMSAF) >= 202603
 ORDER BY c.CRARANAF, c.CRARMSAF, c.CRARCDGO;
```

## 4. Después del EAR con la C46 (y el 411)

```sql
SELECT CRARCDGO, CRARANAF, CRARMSAF, CRARESTD, CRARALCD
  FROM CRD.CRAR
 WHERE CRARALCD IS NOT NULL
 ORDER BY CRARANAF, CRARMSAF, CRARCDGO;
```

## 5. Qué hacer con las líneas que faltaron en las cargas ya procesadas

Lo que hay que saber:
- Petro sí descontó y transfirió ese dinero. Entró con la transferencia del mes, pero ninguna línea de la carga
  lo explica, así que quedó en **depósitos por confirmar sin dueño**.
- El partícipe tiene ese descuento en su rol de pagos, y en el SAA la cuota correspondiente sigue impaga o parcial.
  Puede que hasta le haya generado mora.

Lo que hay que hacer, mes por mes, de abril a julio de 2026:

1. **No volver a cargar ni reprocesar el archivo del mes en producción.** Eso volvería a aplicar las 2.700 líneas
   que ya están bien, no solo las que faltan.
2. Ubica cada línea que falta con el archivo de Petro: es la línea sin rol que está justo debajo de la del rol, en
   la sección del producto, con su «TOTAL EMPLEADO». Anota el rol, el producto, el valor descontado y el plazo
   o saldo que imprime Petro, para identificar el préstamo cuando el partícipe tiene dos del mismo tipo. El cuadre
   ya las tiene identificadas por rol; usa su lista.
3. Registra cada línea como **cobro de crédito al préstamo y a la cuota de ese mes**:
   - fecha del depósito de Petro de ese mes, valor igual al descontado de la línea;
   - la contrapartida es el depósito por confirmar de esa transferencia, no un ingreso nuevo de caja.
   Así la cuota queda pagada con la fecha en que Petro descontó, y el depósito por confirmar baja por lo mismo.
4. Si al pagarla aparece mora calculada **después** de la fecha de descuento, esa mora no corresponde: el dinero
   llegó a tiempo. Se trata con el criterio de carga tardía sin mora (B1, `DISENO-CARGA-TARDIA-SIN-MORA.md`).
5. Marzo 2026: los 20,00 no tienen línea. Pregúntale a Petro a qué partícipe corresponden. Mientras no conteste,
   se quedan en depósitos por confirmar con ese motivo.
6. Control después de cada mes:
   - vuelve a correr las secciones 2 y 3: la diferencia sigue igual, porque la carga no cambia, y eso es lo esperado;
   - lo que tiene que bajar a 0 es el saldo del depósito por confirmar de esa transferencia, por el valor de las
     líneas;
   - la cuota del rol tiene que quedar pagada.

**Coordinación con el re-proceso.** El re-proceso aplica estas mismas líneas mes a mes por su lado, sobre la base
de trabajo, y producción se reemplaza con su resultado. Antes de registrar algo a mano en producción, confirma con
el líder del cuadre si esos meses de producción se van a reemplazar. Si se reemplazan, no se toca nada en
producción y basta con que el re-proceso las aplique. Si el reemplazo no llega a esos meses, se hace lo del punto 3.

Tampoco hay que reprocesar con el EAR nuevo (C46) una carga de abril a julio que el re-proceso ya completó a mano:
con la C46, el lector ya trae esas líneas y quedarían aplicadas dos veces.
