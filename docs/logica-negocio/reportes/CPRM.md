# CPRM — CreditoParticipesMensual

> ⚠️ Si se modifica la lógica de este reporte, actualizar este archivo.
> **La lógica base de CPRM es idéntica a G42.** Si cambia G42, revisar si este archivo necesita ajustes.

**Tabla destino:** `RPR.CPRM`
**Servicio:** `GeneracionCPRMServiceImpl.java`
**G equivalente:** G42 (`GeneracionG42ServiceImpl.java`)

## Lógica base
Idéntica a G42. Ver `G42.md`.

## Diferencias respecto a G42

| Aspecto | G42 | CPRM |
|---|---|---|
| Tabla control ejecución | `RPR.EJRC` / `RPR.EJRD` | `RPR.EJCC` |
| Tabla destino | `RPR.CG42` | `RPR.CPRM` |

## Optimización batch
- Todas las entidades se cargan en una sola consulta con `findByCodigosIn()`.
- `Map<Long, Entidad>` en memoria para acceso O(1).
- **Total: 2 consultas** (1 suma agrupada + 1 batch de entidades).

## Historial de cambios
| Fecha | Cambio |
|---|---|
| — | Sin cambios desde implementación inicial |
