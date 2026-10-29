# Reportes a la Superintendencia y estados financieros — inventario contra el SAA

C19 · 2026-10-28. Fuentes revisadas: lo presentado en mayo 2025 (B17, G41–G51 y Q01, cada uno con su `.ACU`);
`08-Cartera/May25` (G40S y `G45M_FORMATO.xlsx`); `07-Formatos` (`B17M396831102025_FORMATO.xlsx` y
`FLUJO DE CAJA_12_2025_FORMATO.xlsx`).

Convención de nombres: estructura + periodicidad + código de entidad **3968** + fecha de corte ddmmaaaa
(`B17M396831052025.txt` = B17 · M · 3968 · 31052025).

| Reporte | Periodicidad | ¿Lo genera el SAA? | ¿Coincide con el formato? | Qué falta |
|---|---|---|---|---|
| B17 | Mensual, `.txt` + `.ACU` | Sí desde C19: `/esrp/generar` y `/esrp/archivo` (estructura B17, alterno 6) | Idéntico byte a byte a mayo 2025 | Correr C19-01/02; decidir las 31 asignaciones y las cuentas 3.3.90.01 y 4.8.02.1; vigencia del catálogo de ene-2026; pantalla en el FE |
| G40 | Semestral (G40S) | Sí: `/ejrc/ejecutar` → `RPR.CG40`; el `.txt` lo arma el FE (`rpr/reportes-super-bancos`) | Según lo presentado | Verificar contra el manual de tablas |
| G41 | Mensual | Sí (`CG41`) | Sí | No reproduce meses pasados (bandera de `CRD.ENTD`) |
| G42 | Mensual | Sí | Sí | Usa la fecha de caja (`APRTFCTR`), no el devengo; cuadre cuenta 21 del B17 = Σ G42 |
| G43 | Mensual | Sí | Sí | — |
| G44 | Mensual | Sí | Sí | Toma los jubilados de hoy |
| G45 | Mensual | Sí | Formato feb-2026 con 14 campos | Actividad económica `009` vs `9` |
| G46 | Mensual | Sí | Sí | Cuadre contra el movimiento de cartera |
| G47 | Mensual | Sí | Sí | — |
| G48 | Mensual | Sí | **Orden de columnas distinto al del Excel oficial: verificar** | Estado actual de la cuota y defectos de la cartera migrada; cuadre 13−1399 / 1399 |
| G49 | Mensual | Sí | Sí | — |
| G50 | Mensual | Stub (0 registros) | Coincide con mayo 2025 (vacío) | — |
| G51 | Mensual | Sí, con `A21` fijo | Sí | — |
| Q01 Quejas | Mensual | No | Presentado vacío | Definir si se incorpora |
| Flujo de caja (formato BIESS) | Mensual | Estructura FLC desde C19 (provisional) | Filas y fórmulas del formato; solo las 9 reglas de su columna O | Falta el mapeo de cuentas (flujo de efectivo ene-2026); la columna O lleva las provisiones (47) a egresos; salida Excel |
| Flujo de efectivo NIIF | Mensual | No | — | Mismo motor, cuando llegue el formato |
| Comparativo dic vs mes | Mensual | Sí desde C19: `/esfo` con `comparativo=CIERRE_ANIO_ANTERIOR` | Por confirmar con el formato | Ajustar al formato |
| Estado de situación financiera / Estado de resultados | Mensual | Sí, y oficiales desde C19 | Encabezado, logo, nombre propio, resultado del ejercicio, PRELIMINAR/DEFINITIVO | — |
| Cambios en el patrimonio y notas | Juego completo | No (las notas son texto libre) | — | Pendiente |
| Ecuación contable y cuadres B17↔G | Control | El estado oficial imprime la línea de descuadre; los cuadres B17↔G están en `10-cuadratura-b17-g.js` del expediente | — | Llevar C9 al SAA |

## Hallazgos

1. El B17 ya es una configuración del motor de estructuras de renglones (C19), no un generador aparte.
2. El catálogo del B17 cambió en 2026 (trae 160225, 230501 y 440525 y ya no 160290 ni 440520): por eso renglones y
   reglas tienen vigencia. Falta confirmar desde qué corte rige.
3. Las cuentas nuevas del plan entran por mapeo automático (prefijo más largo) y quedan advertidas.
4. Los generadores de las G no reproducen cortes pasados (G41, G44, G48); el cierre de cartera al corte (C15) lo
   corrige para G48/CCPM.
5. Los `.txt` de las G salen del FE; el del B17, del backend.
