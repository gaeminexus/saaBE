# DISEÑO — Bien en garantía al emitir y provisión de cartera en el cierre

**Fecha:** 2026-10-13 · **Equipo:** `omen-saa-1` · **Árbitro:** `omen-saa-1-arb`
**Relacionado:** `DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md` §6.1 (asiento de entrega),
`API-CIERRE-CARTERA.md`, `DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md` (paso ⑦),
`PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md` y `API-CALIFICACION-RIESGO.md` (CRD.CFCR / CRD.ESCR).

## 1. Pregunta del usuario y hallazgos (2026-10-13)

> «Necesitamos asegurarnos que al emitir los préstamos se estén emitiendo los asientos de garantía y
> provisiones.»

- **Garantía:** el asiento de entrega sí lleva las cuentas de orden (cartera y documentos en
  garantía). Pero la línea **«bien en garantía»** (aux1 8, alternos 9 y 13) sale de `PRSTVLAS`, y en el
  ingreso **nadie la llena**: solo se carga después, desde pólizas. Con `PRSTVLAS` nulo, la línea **se
  omite sin aviso** y el asiento cuadra igual.
- **Provisión de cartera:** se **calcula** (G48 y CCPM, con `CalificacionRiesgoService`), pero **no
  existe ningún asiento** que la registre. La de intereses sí existe, en el paso ⑦ del cierre.

## 2. Decisiones del usuario (2026-10-13). NO re-litigar

| # | Decisión |
|---|---|
| GP1 | En **hipotecarios**, el valor del bien en garantía es la **suma asegurada**. |
| GP2 | La provisión de cartera se hace **en el cierre**, no al emitir. |

⚠️ **Supuesto del árbitro, pendiente de confirmar:** en **prendarios** también se usa la suma
asegurada, porque es la misma columna y el mismo mecanismo de póliza. Si el usuario dice otra cosa, cambia
solo GA1.

---

## 3. Parte A — Bien en garantía (se construye ya)

| # | Regla |
|---|---|
| GA1 | **Ingreso:** en un producto de familia **PRENDARIO o HIPOTECARIO**, el campo **«Suma asegurada del bien»** (`valorAsegurado`, `PRSTVLAS`) es **obligatorio y > 0**. Se muestra solo para esas familias, y `saveSingle` lo guarda y no lo pisa. |
| GA2 | **Aprobar** un prendario o hipotecario sin `PRSTVLAS > 0` devuelve **422**: «Falta la suma asegurada del bien: sin ella no se registra la garantía en cuentas de orden». Va **antes** de numerar o contabilizar, así que no se pierde ningún idAsoprep. |
| GA3 | `contabilizarEntrega`, como defensa: si la plantilla tiene línea de bien y `valorBien ≤ 0`, **falla con mensaje**, en vez de omitir la línea. Se actualiza el comentario viejo, que dice que `PRSTVLAS` «no tiene escritor»: hoy lo escriben pólizas y, desde este cambio, el ingreso. |
| GA4 | La **anulación** no cambia: el reverso del asiento de entrega ya incluye la línea del bien. |
| GA5 | **Fuera de alcance, sin prisa:** si la suma asegurada cambia después de aprobar (`SumaAseguradaService.actualizar`), las cuentas de orden no se ajustan. Haría falta un asiento de ajuste, y se diseña si el usuario lo pide. |
| GA6 | **Fuera de alcance:** las plantillas de reestructuración y novación (10, 11, 14, 15, 2, 3) no tienen línea de bien. El bien del original sigue en orden y el nuevo préstamo lo hereda; no se mueve nada. |

## 4. Parte B — Provisión de cartera en el cierre (diseño; se construye cuando estén las cuentas)

| # | Regla |
|---|---|
| PV1 | Es un **paso nuevo del cierre de cartera, el ⑧ «Provisión de cartera»**, después del ⑦. Se rige igual que los demás pasos: subproceso, asiento y reverso del cierre. |
| PV2 | **Provisión requerida**, por préstamo vivo al corte: el **valor sujeto a provisión** × el **% de provisión** (`ESCRPRVS`) de su calificación. La calificación y el valor salen de **`CalificacionRiesgoService`**, el mismo servicio y la misma base del G48. **Ninguna fórmula nueva:** el asiento y el reporte a la Superintendencia tienen que dar lo mismo. |
| PV3 | Se agrupa **por tipo de préstamo**, como el paso ⑦ (R4), porque cada familia puede tener su cuenta de provisión. |
| PV4 | **Asiento por diferencia («ajuste a saldo»).** Para cada tipo: diferencia = provisión requerida − **saldo contable** de la cuenta de provisión al corte. **Diferencia > 0:** D gasto por provisión / H provisión. **Diferencia < 0:** D provisión / H la cuenta de reverso o recuperación que diga el contador. Usar el saldo contable, y no solo lo que registró el sistema, hace que se reconozcan las provisiones manuales anteriores y que el cierre se autocorrija. |
| PV5 | **Plantilla nueva** (alterno libre, lo fija el árbitro con el `sql/328`) con las líneas por tipo de préstamo (aux2): gasto (DEBE), provisión (HABER) y reverso o recuperación. **Las cuentas las da el contador:** ninguna va en el código. |
| PV6 | **Rastro por préstamo:** cuánto se provisionó de cada préstamo en cada corrida, en el libro `CRD.MVIC` si encaja (lo mide el BE), para auditoría y para que la anulación (AN5) sepa que el préstamo ya pasó por una provisión. |
| PV7 | **Primera corrida:** si las cuentas ya tienen saldo de provisiones manuales, PV4 solo registra la diferencia. Hay que mirar el bloque 3 del `sql/328` antes de la primera corrida real. |

**Bloqueante de la Parte B:** la salida del `sql/328` (las cuentas candidatas, las plantillas
existentes, cómo se provisiona hoy y la escala vigente) y que **el contador elija las cuentas** por tipo
de préstamo.
