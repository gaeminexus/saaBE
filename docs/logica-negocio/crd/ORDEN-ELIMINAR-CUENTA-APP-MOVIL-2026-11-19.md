# Orden: «Eliminar cuenta» de la app borra la credencial — 2026-11-19

**Árbitro `omen-app-1-arb` → `omen-app-1-saa-be-app`.** Decisión del usuario del 2026-11-19 (opción A).

## Por qué

Para publicar en Google Play y App Store, la app tiene que permitir **eliminar la cuenta**. Google
dice expresamente que *desactivar, deshabilitar o congelar la cuenta no cuenta como eliminarla*.
Hoy `UsuarioAppServiceImpl.desactivar` solo pone `USAPESTD = 3` (ELIMINADO). La fila y la clave
(`USAPCLVE`) se conservan, y la oficina la puede **reactivar**. Eso es desactivar, y es motivo de
rechazo en la revisión.

Los datos financieros del partícipe (`ENTD`, `PRTC`, aportes y préstamos) **NO se tocan**: son
registros de un fondo regulado y se conservan por obligación legal. La política de privacidad lo va a
decir así. Lo que desaparece es **la cuenta de la app**, es decir, la fila de `CRD.USAP`.

## Qué cambia

1. **`desactivar`** (`POST /rest/usap/desactivar` y su equivalente en `/movil`): después de
   `autenticar` (que no cambia; sigue exigiendo cédula y clave correctas), **borra la fila de
   `CRD.USAP`** en vez de marcarla con el estado 3. Así desaparecen la clave, el código de
   recuperación y los datos de acceso. La respuesta HTTP queda **igual**, para que el borde y la app
   no cambien.
   - Antes de escribir el `remove`, verifica que **ninguna tabla tenga FK a `CRD.USAP`**: revisa
     `DDL-USUARIO-APP-MOVIL.sql` y busca referencias en `model/`. Si alguna la tiene, **frena y
     avisa**: no la resuelvas por tu cuenta.
   - Deja la traza en el log (`System.out.println`) con la identificación y **sin la clave**, como
     el resto del servicio.
2. **Volver a enrolarse:** después del borrado, `crear` con la misma cédula tiene que funcionar (ya
   no hay fila). Verifica en el código que nada lo impida.
3. **`reactivar` se queda** para las filas que ya estén en estado 3 de antes. No se borra el
   endpoint. Para una cuenta borrada con el código nuevo, `estado/{identificacion}` responde 404
   («sin cuenta») y la pantalla «Acceso App Móvil» ofrece **crear**, que es lo correcto.
4. **Documentación** en el mismo cambio: `docs/logica-negocio/crd/API-USUARIO-APP-MOVIL.md`, la
   semántica de `desactivar`.

## Fuera de alcance

- `saaFE` (la pantalla «Acceso App Móvil»): no se toca.
- `saaBEMobile`: no cambia, porque la respuesta es la misma.
- Las filas que ya estén en estado 3 en producción: las mide el árbitro con
  `sql/MEDICION-USAP-ELIMINADOS-APP-MOVIL.sql`, y el usuario decide si se purgan.

## Entrega

`mvn -q compile` OK, y la **lista exacta de archivos** tocados. Sin git: lo commitea el árbitro.
