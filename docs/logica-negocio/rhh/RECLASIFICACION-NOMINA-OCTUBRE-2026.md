# Reclasificación: pagos de nómina individualizados de octubre 2026 en honorarios por pagar

> **REEMPLAZADO (2026-11-01):** el usuario autorizó corregir los 20 asientos con un **UPDATE directo**
> (`rhh/sql/omen1-05`) en lugar de una reclasificación. Este documento queda solo como detalle de lo que
> se corrigió. El grupo «PAGO DE NOMINA» ya está corregido (`omen1-04`, corrido: GRPP 38 → 9689 2.5.01).

**omen-saa-1-arb, 2026-11-01.** Diagnóstico: `rhh/sql/omen1-03`, corrido en producción.

## Qué pasó
Los 20 pagos de nómina individualizados (un pago por empleado desde la bandeja de Tesorería, origen
`RHH_NOMINA_EMPLEADO`) del **28-10-2026** se contabilizaron así:

| Cuenta | Debe | Haber |
|---|---|---|
| 2.3.90.15 HONORARIOS POR PAGAR | 17.244,97 | |
| 1.1.02.05.80 (banco) | | 17.244,97 |

Lo correcto era debitar **2.5.01 REMUNERACIONES POR PAGAR**, que es la cuenta que acreditó el asiento del
rol y la que usa la plantilla de pago de nómina (alterno 165, línea 50).

**Efecto en saldos:**
- 2.5.01 quedó con 17.244,97 acreedor de más (la obligación con los empleados figura sin cancelar).
- 2.3.90.15 quedó con 17.244,97 deudor (un «anticipo» de honorarios que no existe).

**Causa:** el grupo de producto de pago «PAGO DE NOMINA» apuntaba a una cuenta provisional (la
marcadora) que resultó ser honorarios por pagar. Se corrige con `rhh/sql/omen1-04`, que el usuario
corre, y con el código N1 del EAR único.

## La reclasificación (la registra contabilidad, con su usuario, por el asiento manual del sistema)
| Cuenta | Debe | Haber |
|---|---|---|
| 2.5.01 REMUNERACIONES POR PAGAR | 17.244,97 | |
| 2.3.90.15 HONORARIOS POR PAGAR | | 17.244,97 |

- **Fecha:** dentro de octubre 2026 (sugerido 31-10-2026); el período está abierto.
- **Glosa sugerida:** «Reclasificación de pagos de nómina individualizados del 28-10-2026 (20 pagos,
  asientos 12129 a 12148) registrados en honorarios por pagar en lugar de remuneraciones por pagar».
- No se anulan los 20 asientos: el banco y los pagos están bien; solo cambia la cuenta del debe.

## Detalle (asiento: valor)
12129: 692,16 · 12130: 633,85 · 12131: 406,64 · 12132: 1.811,00 · 12133: 289,00 · 12134: 819,92 ·
12135: 633,85 · 12136: 1.647,39 · 12137: 457,86 · 12138: 441,88 · 12139: 488,03 · 12140: 889,92 ·
12141: 192,16 · 12142: 1.308,76 · 12143: 493,01 · 12144: 1.390,17 · 12145: 692,16 · 12146: 1.479,04 ·
12147: 1.992,10 · 12148: 486,07 — **total 17.244,97**.

## Resultado (2026-11-01)
- `omen1-04` corrido: GRPP 38 «PAGO DE NOMINA» → 9689 2.5.01.
- `omen1-05` corrido: las 20 líneas DEBE pasaron de 2.3.90.15 a 2.5.01 (PLNNCDGO, DTASCNTA y DTASNMCT). Los
  asientos siguen cuadrados. No estaban mayorizados (`MYRZCDGO` vacío), así que no hace falta volver a
  mayorizar. Respaldo en `CNT.BK_DTAS_NOMINA_OCT26`.
- Código: N1 (`d302666b`) en el EAR único.
