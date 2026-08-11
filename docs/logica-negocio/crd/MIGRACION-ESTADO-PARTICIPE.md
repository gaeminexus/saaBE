# Migración de CRD.ENTD.ENTDIDST — línea base y equivalencias

> Registro de respaldo tomado el **2026-08-11**, antes de ejecutar la migración
> de `ENTDIDST` de PK de `CRD.ESPR` a código alterno (`ESPRCDEX`).
>
> Este archivo es la referencia contra la que se verifica el paso 3.3 del
> script de migración. Si los conteos posteriores no cuadran contra esta tabla,
> hay que hacer ROLLBACK.

## Catálogo CRD.ESPR

| PK `ESPRCDGO` | `ESPRNMBR` | Código alterno `ESPRCDEX` |
|---|---|---|
| 10 | ACTIVO | 1 |
| 2 | CESANTE | 2 |
| 30 | JUBILADO COMPLEMENTARIO | 3 |
| 23 | CESANTE DESAFILIADO | 4 |
| 40 | CESANTE FALLECIDO | 5 |
| 41 | JUBILADO APORTANTE | 6 |
| 42 | JUBILADO PASIVO | 7 |
| 62 | ACTIVO EN MORA | 8 |
| 63 | NUEVO | 9 |

## Línea base — distribución de entidades por estado (2026-08-11)

Resultado del control 3.0, **antes** de cualquier cambio:

```sql
SELECT e.ENTDIDST AS PK, esp.ESPRNMBR, esp.ESPRCDEX AS COD_ALTERNO, COUNT(*) AS ENTIDADES
FROM   CRD.ENTD e
       LEFT JOIN CRD.ESPR esp ON esp.ESPRCDGO = e.ENTDIDST
GROUP BY e.ENTDIDST, esp.ESPRNMBR, esp.ESPRCDEX
ORDER BY 1;
```

| PK | Estado | Cód. alterno | Entidades |
|---|---|---|---|
| 2 | CESANTE | 2 | 3304 |
| 10 | ACTIVO | 1 | 1695 |
| 23 | CESANTE DESAFILIADO | 4 | 2093 |
| 30 | JUBILADO COMPLEMENTARIO | 3 | 190 |
| 42 | JUBILADO PASIVO | 7 | 18 |
| | **TOTAL** | | **7300** |

## Lo que dice esta línea base

- **No hay entidades huérfanas ni con `ENTDIDST` nulo.** Todas las filas
  resolvieron contra `CRD.ESPR`, así que el control 3.0b debe dar 0 y el
  remapeo no va a dejar ningún NULL.

- **No hay ninguna entidad con `ENTDIDST = 1`.** El `UPDATE` que mueve el
  marcador colgado al estado NUEVO (63) es hoy un **no-op**: afecta 0 filas.
  El cambio en el código sigue siendo necesario para las entidades que se creen
  de aquí en adelante, pero no hay dato histórico que corregir.

- **Cuatro estados del catálogo están vacíos**: CESANTE FALLECIDO (40),
  JUBILADO APORTANTE (41), ACTIVO EN MORA (62) y NUEVO (63). ACTIVO EN MORA se
  poblará con las 75 entidades detectadas por falta de descuento AH.

- **G41 hoy no reporta nada.** Al no existir entidades en el estado marcador, la
  generación del G41 devuelve 0 registros. Es coherente con el flujo (G41 pasa
  las procesadas a ACTIVO), pero conviene confirmarlo con el área usuaria.

- **De los tres estados de jubilado, solo dos tienen datos** (30 con 190, 42 con
  18). Los reportes G44, CJBM, G45, G48 y CCPM solo contemplan el 30, así que
  hoy están dejando fuera 18 entidades en JUBILADO PASIVO. Está marcado como
  `PENDIENTE` en el código.

## Estado esperado después de la migración

Corriendo primero las 75 a ACTIVO EN MORA y luego el remapeo global:

| Cód. alterno | Estado | Entidades esperadas |
|---|---|---|
| 1 | ACTIVO | 1620 |
| 2 | CESANTE | 3304 |
| 3 | JUBILADO COMPLEMENTARIO | 190 |
| 4 | CESANTE DESAFILIADO | 2093 |
| 7 | JUBILADO PASIVO | 18 |
| 8 | ACTIVO EN MORA | 75 |
| | **TOTAL** | **7300** |

El total debe mantenerse en 7300 en todos los pasos. Cualquier desviación es
motivo de ROLLBACK.

## Asignación automática de ACTIVO EN MORA

A partir de este cambio, el estado ACTIVO EN MORA (8) lo asigna el **proceso de
carga Petro**, no un update manual.

Al procesar el producto AH, cuando un partícipe llega sin descuento (valor 0 o
nulo), `CargaArchivoPetroServiceImpl.evaluarMoraPorFaltaDeAporte()` revisa el
periodo inmediatamente anterior. Si tampoco hubo descuento, son dos periodos
consecutivos sin aportar y la entidad pasa a ACTIVO EN MORA.

Condiciones para que se marque:
- La entidad debe estar en ACTIVO. No se tocan cesantes, jubilados,
  desafiliados ni las que ya están en mora.
- El periodo anterior debe tener carga de AH. Si no se ha cargado, no se puede
  afirmar que no aportó y no se evalúa.
- Si la consulta falla, no se marca mora.

El proceso **no revierte** el estado: si el partícipe vuelve a aportar, sacarlo
de ACTIVO EN MORA es una decisión administrativa.

### Limitación conocida

La regla solo alcanza a los partícipes que **vienen en el archivo con valor 0**.
Un partícipe activo que no aparece del todo en el detalle AH no es evaluado,
porque el proceso itera sobre lo que trae la carga.

El query de diagnóstico que se usó para detectar las 75 entidades iniciales sí
cubre ambos casos (ausente o en cero). Conviene correrlo periódicamente como
control de contraste.

## Alerta de regresión

Los cuatro endpoints `/rest/entd/resumen-*-por-estado` usan por defecto
ACTIVO, CESANTE, JUBILADO COMPLEMENTARIO, CESANTE DESAFILIADO y JUBILADO PASIVO
(`EntidadRest.ESTADOS_RESUMEN_POR_DEFECTO`). **ACTIVO EN MORA no está en esa
lista**, así que apenas se muevan las 75 entidades, esas desaparecen de los
resúmenes salvo que el frontend mande el estado explícitamente.

Hay que decidir si se agrega al default o si lo maneja el frontend.
