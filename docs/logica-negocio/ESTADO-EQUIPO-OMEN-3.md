# ESTADO — equipo `omen-saa-3` (sesión iniciada el 2026-09-28)

**Árbitro:** `omen-saa-3-arb` (OMEN) · **Agentes:** `omen-saa-3-be`, `omen-saa-3-fe` · **Marcador de commit:** `omen3` · **Scripts:** prefijo `e3-`
**Alcance dado por el usuario:** rhh, cxp, pagos, cnt, tsr, cxc, crd. **NO TOCAR:** sin definir todavía.
**Lo mantiene SOLO este equipo.** No confundir con `ESTADO-CXP-CXC-TSR-RHH-SRI.md`, que es del `omen-saa-3` anterior (relevado el 2026-09-01) y está congelado al 2026-08-31.

> ⚠️ **Convivencia:** `omen-saa-2` estaba activo el 2026-09-28 sobre rhh/cxp/cxc/tsr/pagos/cnt/sri, y
> `omen-saa-1` es dueño de crd. Antes de tocar un archivo: `git status` + `git log -3` sobre él.

## Frentes

| # | Frente | Estado | Commits |
|---|---|---|---|
| 1 | rhh — crear un colaborador fallaba (estado 'A' en un Long) y la lista marcaba «Inactivo» a todos | ✅ FE entregado | saaFE `b4fa930` |
| 2 | rhh — vacaciones: 14 activos sin saldo 2026 | 🟡 medido, **espera decisión del usuario** (A por aniversario descontando la apertura / B devengo proporcional) antes del 2026-10-01 | `fa587127` (e3-03), `943b08c2` |
| 3 | rhh — crear un contrato fallaba con ORA-02290 (estado y fecha de registro nulos) | ✅ BE+FE entregados; espera despliegue | `9ead911b`, saaFE `ece5e22` |
| 4 | rhh — archivo bancario de la nómina: formato del Internacional y código BCE en vez del nombre del banco | ✅ SQL corrido por el usuario (e3-04), BE entregado; espera despliegue y revisión del archivo | `40df8e87`, `806d9c97` |
| 5 | Documento comercial del SAA para otros fondos | ✅ entregado (Claude Docs) | — |
| 6 | rhh — liquidaciones de ex-colaboradores de la administración anterior | 🔵 **diseño y contrato escritos, DDL escrito sin correr**; despachado BE+FE | ver abajo |

## Hallazgos (lo que costaba ver)

- **Los NOT NULL de `RHH.CNTE` son CHECK con nombre de sistema y dan ORA-02290, no ORA-01400.** La consulta de `REFERENCIA-CHECKS-RHH.md` excluía `SYS_%`, dándolos por inofensivos: por eso `SYS_C009213` no estaba documentado.
- **Tesorería y RRHH tienen dos motores de archivo bancario.** El formato del Internacional estaba cargado en el de Tesorería y el de RRHH (`RHH.FMBN`) estaba vacío.
- **Re-correr «Acreditar vacaciones» después del aniversario duplicaría la apertura** de quien ingresó en 2025: 15 días completos más su apertura proporcional del mismo año de servicio.
- **Ninguna liquidación de haberes se paga por Tesorería**: la cuenta 70 `LIQUIDACIONES_POR_PAGAR` se acredita y nada la debita. Y el finiquito escribe el RDEP con el año de la fecha de salida, no la de pago. Detalle en `rhh/API-LIQUIDACION-EXCOLABORADORES.md` §9.
- **La nómina consolidada (`RHH_NOMINA`) se aprueba con Débito automático**, no por transferencia: el archivo por empleado lo arma RRHH, no Tesorería. El mensaje de la guarda no lo dice (mejora pendiente, `PagoProgramadoServiceImpl` es territorio común).
