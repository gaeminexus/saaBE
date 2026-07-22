# CJBM — CreditoJubiladosMensual

> ⚠️ Si se modifica la lógica de este reporte, actualizar este archivo.
> **La lógica base de CJBM es idéntica a G44.** Si cambia G44, revisar si este archivo necesita ajustes.

**Tabla destino:** `RPR.CJBM`
**Servicio:** `GeneracionCJBMServiceImpl.java`
**G equivalente:** G44 (`GeneracionG44ServiceImpl.java`)

## Lógica base
Idéntica a G44. Ver `G44.md`.

## Diferencias respecto a G44

| Aspecto | G44 | CJBM |
|---|---|---|
| Tabla control ejecución | `RPR.EJRC` / `RPR.EJRD` | `RPR.EJCC` |
| Tabla destino | `RPR.CG44` | `RPR.CJBM` |

## Optimización batch
- `ValorPagoPensionComplementariaService.selectByEntidadesIn()` — un solo query para todos los jubilados.
- `HistoricoCJBMService.selectByIdentificacionesIn()` — un solo query para historial.

## Historial de cambios
| Fecha | Cambio |
|---|---|
| — | Sin cambios desde implementación inicial |
