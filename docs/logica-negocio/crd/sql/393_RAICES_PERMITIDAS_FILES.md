# `saa.files.raicesAdicionales` — raíces permitidas de `/rest/file/*` (lote 9, #10)

`FileRest` (`download`, `view`, `delete`, `list`, `info`, `upload/custom`) ya no acepta cualquier ruta del servidor: la normaliza
(`FileService.resolverPermitida`) y responde **400** si queda fuera de las raíces permitidas. `upload` y `upload/custom` además rechazan un
`fileName` con `/`, `\` o `..`. Los servicios internos (Petro, firmas, cuentas bancarias, cxp, tsr…) **no** pasan por este control.

| Propiedad | Formato | Qué es |
|---|---|---|
| `saa.upload.dir` | ruta | Raíz de subidas (ya existía). Siempre permitida. Una ruta relativa se resuelve contra ella. |
| `saa.files.raicesAdicionales` | rutas separadas por `;` | Carpetas adicionales permitidas, si hay archivos legítimos grabados fuera de la raíz. Opcional. |

```
/system-property=saa.files.raicesAdicionales:add(value="/ruta/uno;/ruta/dos")
```

## Orden de despliegue (obligatorio)
1. **Correr `393_MEDICION_RUTAS_FUERA_DE_UPLOAD.sql` en producción ANTES de desplegar** (solo lectura; cambiar `RAIZ` por el valor real de `saa.upload.dir`).
2. El bloque 1 lista, por tabla y carpeta, las rutas ya grabadas que quedarían fuera: cada carpeta que deba seguir sirviéndose va en `saa.files.raicesAdicionales`.
3. El bloque 2 lista rutas con `..` o `//`: esas se rechazan siempre (corregir el dato, no la propiedad).
4. Fijar la propiedad, desplegar y probar `GET /rest/file/view?filePath=<un comprobante real>` (200) y con una ruta fuera de la raíz (400).

El bloque 3 es informativo (TSR, CXP, RHH): esas rutas también salen por `/file/*` si el frontend las pide.
