# API — Dirección de domicilio única y ciudad (`/rest/drcc`)

2026-11-16 · Diseño: `DISENO-DIRECCION-DOMICILIO-UNICA-Y-CIUDAD.md` · DDL: `sql/379_DDL_DIRECCION_CIUDAD.sql` (va ANTES del EAR:
sin la columna `CDDDCDGO`, toda lectura de `Direccion` da ORA-00904).
Base: `/SaaBE/rest/drcc/...`. Los endpoints de siempre (`getAll`, `getId`, `getByParent`, POST, PUT, DELETE, `selectByCriteria`) no cambian de ruta.

## Regla del domicilio vigente (la única, para pantalla, solicitud, pagaré y contrato de adhesión)
Entre las direcciones de la entidad: **activas** (`estado` nulo o 1), **no de trabajo** (`trabajo` distinto de 1) y que **no estén en
`CRD.DRTR`**: la de `porDefecto = 1` de **menor `codigo`**; si ninguna está por defecto, la de menor `codigo`.

## Endpoint nuevo
### GET `/rest/drcc/domicilioVigente/{idEntidad}`
- 200 con la `Direccion` (JSON de siempre, con `ciudad`), o **204 sin cuerpo** si la entidad no tiene domicilio.
- La pantalla de datos del partícipe debe mostrar **esta** dirección (en vez de elegir ella la primera de `getByParent`).

## Cambios en lo existente
- **`ciudad`** (nuevo, opcional en el JSON, FK a `CRD.CDDD`): se envía como `"ciudad": { "codigo": 123 }` en POST/PUT; viene en las
  lecturas como el objeto `Ciudad` (con `nombre` y `provincia`). El combo sale de `ciudad.service` (`/rest/cddd`): «CIUDAD (PROVINCIA)»,
  ordenado por nombre. El BE **no** exige la ciudad (el diseño la pide obligatoria en el FE al guardar).
- **`getByParent/{idEntidad}`** ahora devuelve ordenado: `porDefecto` descendente (nulo cuenta como 0) y `codigo` ascendente.
- **Al guardar** (POST o PUT) una dirección **de domicilio** (activa, no de trabajo) con `porDefecto = 1`, las demás domiciliarias activas de
  esa entidad que estén por defecto quedan en `porDefecto = 0`, en la misma transacción. El FE no debe desmarcarlas a mano. Las de trabajo
  no se tocan. (`PUT/POST ?usuario=` sigue sellando la actualización de la entidad, como antes.)
- El impreso de la solicitud (`RPRT_PRST_SLCT`) y el pagaré (`RPRT_PRST_PGRE`) usan la misma regla (menor código) y la **ciudad efectiva**
  `NVL(direccion.ciudad, parroquia.ciudad)`. La solicitud imprime «calle número y secundaria, barrio, descripción, CIUDAD», saltando lo
  vacío. El contrato de adhesión (Anexo I) también.

## Qué NO cambia
`parroquia` sigue existiendo y siendo opcional; es solo el respaldo de la ciudad en los impresos de las direcciones viejas.
