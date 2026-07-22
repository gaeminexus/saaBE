# Contexto General — Reportes G y Cartera

> ⚠️ Actualizar este archivo si cambia el orquestador, los estados o el patrón de implementación.

## Endpoints de ejecución

| Sistema | Endpoint | Body |
|---|---|---|
| Reportes G (SBS) | `POST /saaBE/rest/ejrc/ejecutar` | `{ "mes": 5, "anio": 2026, "usuario": "jperez" }` |
| Reportes Cartera | `POST /saaBE/rest/ejcc/ejecutar` | `{ "mes": 5, "anio": 2026, "usuario": "jperez" }` |

## Tablas de control

| Tabla | Entidad Java | Uso |
|---|---|---|
| `RPR.EJRC` | `EjecucionReporte` | Cabecera ejecución reportes G |
| `RPR.EJRD` | `DetalleEjecucionReporte` | Detalle por cada G |
| `RPR.EJCC` | `EjecucionReporteCartera` | Cabecera ejecución reportes cartera |

## Estados EJRC / EJRD

| Valor | EJRC | EJRD |
|---|---|---|
| 1 | En proceso | OK |
| 2 | Con novedades | Con novedades / Error |
| 3 | Completo | Pendiente |

## Comportamiento del orquestador (Reportes G)

1. Recibe `{mes, anio, usuario}`
2. Si ya existe EJRC y todos los EJRD están en estado 1 → retorna "ya generados"
3. Si existe EJRC con EJRD en estado 2 o 3 → re-ejecuta solo esos Gs
4. Si no existe → crea EJRC (estado=1) + 12 EJRD (estado=3) → ejecuta todos
5. Por cada G: marca EJRD estado=3 → ejecuta → si OK: estado=1 + cantidadRegistros; si falla: estado=2 + mensaje
6. Al final: si todos OK → EJRC estado=3; si alguno falló → EJRC estado=2

## Orquestadores
- **Reportes G:** `GeneracionReportesServiceImpl.java`
- **Reportes Cartera:** ejecuta secuencialmente CPRM → CJBM → CCPM

## Patrón estándar para implementar un G nuevo

1. Crear `service/GeneracionGxxService.java` → método `long generar(DetalleEjecucionReporte detalle) throws Throwable`
2. Crear `serviceImpl/GeneracionGxxServiceImpl.java` con la lógica
3. Agregar `@EJB private GeneracionGxxService gxxService` en el orquestador
4. Activar `case "Gxx": return gxxService.generar(ejrd)` en el orquestador
5. Crear el archivo `Gxx.md` en este directorio con la lógica
6. Registrar el archivo en `_INDICE.md`

## Estados de entidad relevantes

| idEstado | Significado |
|---|---|
| 1 | Nuevo (pendiente de procesar en G41) |
| 10 | Procesado por G41 |
| 30 | Jubilado voluntario |
