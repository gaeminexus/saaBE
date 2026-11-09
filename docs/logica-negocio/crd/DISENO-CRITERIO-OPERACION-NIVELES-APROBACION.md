# DISEÑO — Criterio «Operación» en las reglas de niveles de aprobación

omen-saa-1-arb · 2026-11-08 · Pedido del usuario: «sí sería bueno agregar el criterio operación para las reglas de
aprobación». Ejecutores: omen-saa-1-be (después del encargo 6) y omen-saa-1-fe.

## Hoy
`CRD.RGAP` filtra por tipo de préstamo (`TPPRCDGO`, nulo = cualquiera), rango de monto y de plazo, con prioridad
(`NivelAprobacionServiceImpl.resolverPorDatos`; empate de prioridad → 422; reglas activas pero ninguna aplica → 422;
sin reglas activas → un paso). Una novación quirografaria cae en la misma regla que un quirografario nuevo.

## Nuevo
- Columna `RGAPTPOP NUMBER` (script `crd/sql/372_DDL_RGAP_OPERACION.sql`): **nulo = cualquiera**, 1 = NUEVO,
  2 = NOVACION, 3 = REESTRUCTURACION. CHECK `IN (1, 2, 3)`.
- Operación del préstamo: sin préstamo original (`PRSTPROR` nulo) → 1; con original → 2 o 3 según la misma lógica que
  ya decide novación vs reestructuración en `SustitucionPrestamoServiceImpl` (el ejecutor la reutiliza, no la
  reinventa, y reporta cuál usó).
- Resolución: una regla aplica si `RGAPTPOP` es nulo o igual a la operación, además de tipo/monto/plazo. La prioridad y
  el 422 por empate no cambian. Las reglas existentes quedan con nulo (= cualquiera): nada cambia hasta que crédito
  las edite.
- Las aprobaciones ya registradas (APPR) siguen usando su regla (no cambia).
- `simular(idTipoPrestamo, monto, plazo)` y `GET /rgap/simular` reciben `operacion` opcional (nulo = NUEVO para el
  «Probar»; el FE lo envía siempre).
- `RPRT_PRST_SLCT` sección 7 (NIVEL_APROBACION, fallback por regla cuando aún no hay APPR): sumar la condición de
  operación a la subconsulta de RGAP (`RGAPTPOP IS NULL OR RGAPTPOP = <operación del préstamo>`). compilar-jasper +
  verificar-fill.
- FE `niveles-aprobacion`: columna y selector «Operación» (Cualquiera / Nuevo / Novación / Reestructuración) en la lista,
  el formulario y el «Probar».
- Docs: `crd/API-CICLO-OTORGAMIENTO.md` §12 y `DISENO-SOLICITUD-V1-...` §B4.
- **Orden de despliegue:** 372 antes del EAR (la entidad mapea la columna).
