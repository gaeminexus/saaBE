# Traspaso — equipo `omen-saa-3` (cierre 2026-10-28)

Para la próxima sesión del equipo 3 de SAA en la OMEN. El historial completo de frentes y
hallazgos está en `docs/logica-negocio/ESTADO-EQUIPO-OMEN-3.md` (lo mantiene solo este equipo):
**leerlo después de este archivo.**

## Alcance

- Árbitro `omen-saa-3-arb`; ejecutores `omen-saa-3-be` (saaBE) y `omen-saa-3-fe` (saaFE).
- Módulos: rhh, cxp, pagos, cnt, tsr, cxc, crd. **NO TOCAR: ninguno** — el usuario autorizó
  el 2026-10-27 tocar cualquier módulo. Igual: `git status` + `git log -3` antes de tocar un
  archivo de otro equipo (`omen-saa-2` trabaja activo en cxp/cxc/cnt/tsr; `omen-saa-1` en crd).
- Prefijo de scripts `e3-`; **el próximo libre es `e3-15`**. Marcador de commit `omen3`.
- No son nuestros y no se tocan: `saaBE/docs/logica-negocio/crd/modelos/` (untracked, de omen1)
  y `saaFE/routes.json` (untracked).

## Terminado y verificado por el usuario (2026-10-27/28)

- `e3-12` (bitácora RHH.CRVC) corrido: 6 constraints OK. `e3-13` (anticipo 41 CANCELADO,
  DSRC 41 ANULADO) corrido con el resultado esperado.
- El usuario dice (2026-10-28) que desplegó WAR y FE y que hizo **todos** los bloqueantes:
  `e3-14`, recálculo de octubre, borrado de los `.txt` del levantamiento en .3/.4, agenda de la
  clave SCP, período de noviembre. **No reportó las salidas** (bloque 2 de e3-14, nómina de
  octubre): si algo sale raro, empezar por pedirlas.

## Terminado pero SIN verificar contra BD/pantalla (solo compila)

- `b1d467d7` — vacaciones dentro de los días de sueldo + anulación de la VARIACION_POR_EXTRAS
  PENDIENTE al recalcular. Esperado en octubre (PRDN 12): la empleada con 8 días de vacación
  cobra sueldo × 22/30 + 412,27 de «Vacaciones pagadas», NMNADITR sigue en 30, su variación
  IESS pasa a ANULADA (o queda un residual pequeño legítimo). Un empleado sin vacaciones debe
  salir idéntico a antes.
- `cc994001` + saaFE `1390aa0` — alerta de acreditación, botón «Acreditar ahora», acreditación
  automática al cerrar un período MENSUAL (mirar la fila de RHH.CRVC tras el primer cierre).
- saaFE `0e0fab3` — menú «Horas extra» bajo «Importación de marcaciones».
- saaFE `e880518`, `a4e73ae`, `1d75411` — combo de colaboradores IESS, registro de novedad IESS,
  LQEX.

## A medias / no empezado

Nada a medias en archivos. No empezado (todo espera decisión del usuario):
- Reparar las 6 solicitudes de vacaciones de agosto aprobadas sin descontar (39 días inflados).
  Existe `SolicitudVacacionesServiceImpl.consumirSaldoSinNovedad` para eso; falta el sí.
- Medir si meses anteriores (desde que `aprobar` crea la NVNM de vacaciones) pagaron
  vacaciones doble, como octubre. Hay que escribir la consulta (NVNM concepto alterno 12 en
  períodos PAGADO/CERRADO).
- Docs de saaFE que listan el permiso 1538 (si e3-14 lo borró): `docs/seguridad/CODIGOS-PERMISOS-SAA.md`
  (~305, ~317), `API-PERMISOS-FRONTEND.md` (374, 386), `ITEM5-MAPEO-MENUS-PERMISOS.md` (212, 222)
  y el conteo «305 nodos». Sin prisa.

## Decisiones tomadas hablando (no están en otro lado)

- **Antedatar documentos: NO.** El usuario pidió fechar el informe DELTA 21 consolidado al
  10-oct-2025; se negó (contrato firmado 13-oct-2025, contenido de 2026, destino: auditor).
  Se entregó fechado 26-oct-2026 v2.0 citando las fechas originales.
- **Vacaciones:** NMNADITR = días de servicio (30), no días de sueldo; mostrar «22» como el
  rol del cliente requiere columna nueva (DDL) — decisión pendiente del usuario. Una solicitud
  que cruza de mes descuenta todos sus días en el período de `fechaDesde` (no se parte).
- **Acreditación automática:** va en `PeriodoNominaRest.cerrar` después del commit del cierre
  (no en `cerrarPeriodo` de omen2); si falla, el cierre no se revierte y queda ERROR en CRVC;
  modalidad aniversario → OMITIDA.
- **Horas extra** se registran por «Novedades del período»; la pantalla «Horas extra» (HREX)
  es solo bandeja y hoy nada la alimenta. Las dos fuentes se suman en el motor: nunca cargar
  la misma hora en ambas.
- **La novedad IESS de variación nace al recalcular el período**, no al aprobar la novedad.
- **Anticipos:** RRHH debe registrar el anticipo en el módulo, no crear descuentos recurrentes
  a mano (eso dejó el anticipo 41 vivo).
- **Commits:** sin nombres de empleados ni montos en el mensaje. El clasificador de auto mode
  bloqueó justo esos dos (`20f80b13`, `b1d467d7`) y el usuario tuvo que correrlos a mano.
  Cuando se bloquee, dar al usuario los comandos `!` con rutas completas; no rodearlo.
- Las subidas del levantamiento de servidores traen claves codificadas: nunca commitearlas ni
  compartirlas.

## Roto

Nada conocido roto por este equipo. Riesgo vigente: `hibernate.max_fetch_depth=2` en
producción (frente 14) sin reporte del usuario.

## Pendiente del usuario

- **Decidible:** permiso fijo de commit/push para el árbitro (`/permissions`:
  `Bash(git commit:*)`, `Bash(git push:*)`); acortar «Importación de marcaciones» (26 > 24
  caracteres) si se ve truncado; columna «días de sueldo» en el rol; revisión de meses
  anteriores por vacaciones doble; las 6 solicitudes de agosto; consolidado DELTA 21 para el
  numeral 12; cuentas de LQEX tipos 1–4 (contador).
- **Sin prisa:** numeral 11 de la documentación de auditoría; contratos de los numerales 1, 3,
  4, 8 y 9; sección 5.6 (brechas) del documento de auditoría.
