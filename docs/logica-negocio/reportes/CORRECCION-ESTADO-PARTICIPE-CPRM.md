# CPRM — la columna «Estado» muestra «Estado N» en vez del nombre del estado

**Fecha:** 2026-09-16 · **Equipo:** `omen-saa-1` · **Reportado por el usuario** (Reportes → Créditos →
Informes Mensuales, pestaña Partícipes) · **Estado:** diseño congelado, sin implementar

---

## 1. La causa, verificada en el código

`GeneracionCPRMServiceImpl:94-105` arma el mapa de nombres **con la PK del catálogo**:

```java
for (EstadoParticipe ep : estados) {
    mapaEstados.put(ep.getCodigo(), ep.getNombre());   // ESPRCDGO (PK)
}
...
nuevo.setNombreEstado(mapaEstados.getOrDefault(entidad.getIdEstado(),
        "Estado " + entidad.getIdEstado()));          // ENTDIDST
```

Pero **`CRD.ENTD.ENTDIDST` ya no guarda la PK: guarda el código alterno `ESPRCDEX`** desde la
migración del 2026-08-11 (`docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md`). Es la misma
trampa que registra el `CLAUDE.md` de la raíz. El resto del sistema ya trabaja con el alterno: las
constantes de `com.saa.rubros.EstadoParticipeEntidad` son 1..9, y `GeneracionG45ServiceImpl:103` y
`GeneracionG48ServiceImpl:319` comparan `getIdEstado()` contra ellas.

Entonces el `getOrDefault` no encuentra la clave y cae al texto de respaldo.

### Por qué la pantalla parecía funcionar a medias

| Estado | Alterno (lo que hay en `ENTDIDST`) | PK del catálogo | Qué muestra hoy |
|---|---|---|---|
| ACTIVO | 1 | 10 | `Estado 1` |
| **CESANTE** | **2** | **2** | **CESANTE** ✅ por casualidad |
| JUBILADO COMPLEMENTARIO | 3 | 30 | `Estado 3` |
| CESANTE DESAFILIADO | 4 | 23 | `Estado 4` |
| CESANTE FALLECIDO | 5 | 40 | `Estado 5` |
| JUBILADO APORTANTE | 6 | 41 | `Estado 6` |
| JUBILADO PASIVO | 7 | 42 | `Estado 7` |
| ACTIVO EN MORA | 8 | 62 | `Estado 8` |
| NUEVO | 9 | 63 | `Estado 9` |

**Un solo estado coincide, CESANTE, y coincide por accidente**: su alterno y su PK valen 2. Por eso
la columna no se veía rota del todo.

## 2. La corrección (backend, `rpr`)

En `GeneracionCPRMServiceImpl`:

1. El mapa se arma con **`ep.getCodigoExterno()`** (`ESPRCDEX`), no con `ep.getCodigo()`. Los estados
   con `codigoExterno` nulo se saltean y se loguean: sin alterno no hay forma de resolverlos.
2. El texto de respaldo deja de ser `"Estado " + id`: pasa a `"SIN ESTADO (" + id + ")"`, para que la
   próxima vez que esto se rompa se vea que es un fallo y no un nombre.
3. Comentario de una línea citando la migración, para que nadie lo "arregle" de vuelta a la PK.

**Nada más cambia.** No se toca el cálculo de montos, ni CJBM, ni CCPM, ni el frontend: la columna ya
muestra `nombreEstado` tal cual viene.

**Verificación de que no hay más lugares con el mismo error:** `getIdEstado()` se usa en `rpr` sólo en
`GeneracionCPRMServiceImpl:150`, `GeneracionCCPMServiceImpl:343`, `GeneracionG45ServiceImpl:103` y
`GeneracionG48ServiceImpl:319`; los tres últimos ya comparan contra las constantes del rubro
(alterno), que es lo correcto.

## 3. Los datos ya generados

La corrección arregla las generaciones **futuras**. Para los meses ya generados hay dos caminos:

- **`crd/sql/227`** (recomendado): un `UPDATE` que sólo reescribe `RPR.CPRM.CPRMSTEN` resolviendo el
  nombre por el alterno. No toca ningún monto, así que un informe ya entregado no cambia de cifras.
- Regenerar el mes (`regenerarGeneracion` de `GeneracionReportesCarteraServiceImpl:136`). Recalcula
  todo, y por eso es la opción que **no** conviene para un mes ya entregado a la Superintendencia.

⚠️ El `UPDATE` deja el estado **de hoy**, no el que tenía el partícipe en el mes del informe: `CPRM`
guarda el nombre pero no el código, y `CRD.ENTD` sólo tiene el estado vigente. Para las filas que hoy
dicen `Estado N` es una mejora clara (hoy no dicen nada), pero conviene saberlo antes de correrlo
sobre meses viejos.
