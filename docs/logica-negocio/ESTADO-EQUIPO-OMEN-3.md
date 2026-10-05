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
| 2 | rhh — vacaciones: 14 activos sin saldo 2026 → **modalidad parametrizable, ASOPREP en devengo mensual 1,25/mes** | ✅ BE+FE entregados; e3-07 corrido el 2026-10-05 (en DBeaver hizo falta la variante sin PL/SQL). **Falta:** reparar las 6 solicitudes de agosto sin consumir, y acreditar al 30/09 | `fa587127`, `943b08c2`, `de5767db`, `b4a0e3eb`, saaFE `c12c07a` |
| 3 | rhh — crear un contrato fallaba con ORA-02290 (estado y fecha de registro nulos) | ✅ BE+FE entregados; espera despliegue | `9ead911b`, saaFE `ece5e22` |
| 4 | rhh — archivo bancario de la nómina: formato del Internacional y código BCE en vez del nombre del banco | ✅ SQL corrido por el usuario (e3-04), BE entregado; espera despliegue y revisión del archivo | `40df8e87`, `806d9c97` |
| 5 | Documento comercial del SAA para otros fondos | ✅ entregado (Claude Docs) | — |
| 6 | rhh — liquidaciones de ex-colaboradores de la administración anterior | ✅ BE+FE, revisado con el documento de Contabilidad (cuenta por concepto, asiento en RRHH al confirmar el pago); e3-05 y e3-10 corridos. **Falta del contador:** cuentas de aporte personal, retención, FR, utilidades, salario digno | `87c4ffd4`, `048d97b3`, saaFE `6e48984`, `0b17f8a`, `5999015` |
| 7 | rol de pagos: fondo de reserva acumulado en el IESS como ingreso y egreso | ✅ entregado (solo reporte) | `1c2a1b87` |
| 8 | rhh — nómina pagada por empleado desde Tesorería, como jubilados | ✅ BE+FE. **Falta:** que el contador apunte el producto NOMINA a Sueldos por pagar; decisión sobre cerrar periodo con rechazados sin reenviar | `10b0dcb9`, `04215778`, saaFE `7def402` |
| 9 | Página comercial de SAA para gaeminexus.com | ✅ prototipo y `index.html` entregados; publicar es del usuario | — |
| 10 | regulatorio — `CAMBIOS-SAA.md` (cuadre con la Superintendencia de Bancos) | 🔵 C1 entregado (periodos ordenados por fecha), C6 inventariado. **Bloqueado:** carpeta `cuadreSuper` y coordinación con `omen-saa-1` para C3/C4/C6/C8. Plan en `docs/regulatorio/PLAN-CAMBIOS-SAA-OMEN3.md` | `d88ba7e2`, saaFE `b448367`, e3-08/e3-09 |

## Hallazgos (lo que costaba ver)

- **DBeaver se saltó en silencio un bloque PL/SQL** (`e3-07`, con un comentario `q'[...]'`) y el control posterior salió «vacío» sin dar error. Antes de dar por corrido un script, mirar que el control traiga las filas esperadas. Ante la duda, entregar la variante con sentencias sueltas.
- **Los periodos contables se ordenaban por `PRDOCDGO`**, no por fecha. Insertar meses anteriores (2025 del cuadre) los habría puesto «después» de 2026 y roto mayorización, saldos bancarios y balances. Además, **un periodo MAYORIZADO no bloquea asientos nuevos**: el control es código muerto (diagnosticado, sin activar, decide el usuario).
- **Las 6 solicitudes de vacaciones aprobadas en agosto nunca se descontaron del saldo** (`SLDVUSDO` en 0 para todos): 39 días inflados.
- **Beneficios sociales contabiliza el banco desde la plantilla de nómina**, no desde la cuenta con que pagó Tesorería. Liquidaciones anteriores usa la cuenta real del pago.
- **El RDEP del SAA es un XML propio de tres montos**, no el del SRI: exentos, FR, utilidades y salario digno no tienen dónde salir.
- **El árbol compartido de saaBE no compila el 2026-10-05** por `ProvisionInteresServiceImpl` de crd, sin commitear y ajeno: lo nuestro se verifica compilando `origin/main` + nuestros archivos en un worktree aparte.

- **Los NOT NULL de `RHH.CNTE` son CHECK con nombre de sistema y dan ORA-02290, no ORA-01400.** La consulta de `REFERENCIA-CHECKS-RHH.md` excluía `SYS_%`, dándolos por inofensivos: por eso `SYS_C009213` no estaba documentado.
- **Tesorería y RRHH tienen dos motores de archivo bancario.** El formato del Internacional estaba cargado en el de Tesorería y el de RRHH (`RHH.FMBN`) estaba vacío.
- **Re-correr «Acreditar vacaciones» después del aniversario duplicaría la apertura** de quien ingresó en 2025: 15 días completos más su apertura proporcional del mismo año de servicio.
- **Ninguna liquidación de haberes se paga por Tesorería**: la cuenta 70 `LIQUIDACIONES_POR_PAGAR` se acredita y nada la debita. Y el finiquito escribe el RDEP con el año de la fecha de salida, no la de pago. Detalle en `rhh/API-LIQUIDACION-EXCOLABORADORES.md` §9.
- **La nómina consolidada (`RHH_NOMINA`) se aprueba con Débito automático**, no por transferencia: el archivo por empleado lo arma RRHH, no Tesorería. El mensaje de la guarda no lo dice (mejora pendiente, `PagoProgramadoServiceImpl` es territorio común).
