# El usuario de sesión en las pantallas de `cxp` (sin relleno con el ID 1)

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-28 por `omen-saa-2-arb`, antes de despachar (regla 7). **Solo FE.**

## 1. El caso

Al registrar desde Gestión de Documentos la nota de crédito con saldo excedente, el usuario recibe *«No se
encontró el usuario con ID 1: no se puede registrar la operación.»* Ese mensaje lo da
`ProcesoCargaDocumentosServiceImpl.nombreUsuarioObligatorio` desde el `b1191051`: el asiento ya no se graba
con SISTEMA y exige un usuario real.

## 2. Lo medido (2026-10-28)

- `gestion-documentos.component.ts:482` obtiene el usuario así:
  `JSON.parse(localStorage.getItem('usuario') || sessionStorage.getItem('usuario') || '{}')` → `u.codigo || u.id || 1`,
  y en el `catch` también devuelve `1`. **El 1 es un relleno, no un usuario.**
- El login (`app-state.service.ts:223-225`) guarda el usuario **solo en `sessionStorage`**: `usuario` (JSON
  con `codigo`) e `idUsuario`. `sessionStorage` es por pestaña. Una pestaña nueva, o una sesión
  restaurada sin `usuario`, cae al relleno 1. Además el getter lee **primero** `localStorage`: un `usuario`
  viejo guardado ahí sin `codigo` también da 1.
- Antes del `b1191051` el error no se veía porque el backend grababa SISTEMA sin mirar el ID.
- El mismo getter, idéntico, está en `negociaciones`, `detalle-negociacion`, `bandeja-electronica` y
  `carga-txt` de `cxp`.

## 3. La regla

En las **cinco** pantallas, el usuario se toma de la sesión: `sessionStorage['idUsuario']`, y si no está,
`sessionStorage['usuario'].codigo`. Después se prueba lo mismo en `localStorage`. **Sin relleno.** Si no hay
usuario, la operación **no se envía** y la pantalla muestra:
*«No se pudo identificar su usuario en esta sesión. Cierre sesión y vuelva a ingresar.»*
El backend no cambia.

## 4. Mientras se despliega

El usuario que vea el error cierra sesión y vuelve a ingresar en esa misma pestaña. No abre el módulo en
una pestaña nueva.

## 5. Hecho y hallazgos (2026-10-28)

**Hecho:** saaFE `6b88b20`. Se agregó `shared/utils/usuario-sesion.util.ts` (`idUsuarioSesion()`) y se aplicó a
las cinco pantallas. Se compiló con `ng build`. Falta desplegar.

**Hallazgos del barrido (sin tocar).** Hoy no fallan, porque esos endpoints no validan al usuario, pero
grabarían un usuario falso.

- Relleno `'SISTEMA'`/`'sistema'`:
  - `cxc` anticipo:582;
  - `cxc` liquidaciones:1118;
  - `cxp` consulta-documentos:596;
  - `tsr` titulares-v2:1315.
- Relleno con 0, con el getter `+(… || '0')`:
  - `cxp`: cruce-anticipo-proveedor, cruce-nota-credito, devolucion-anticipo, historial-abonos-factura,
    pagos-transferencia, solicitud-pago;
  - `tsr`: anticipos-clientes, anticipos-proveedores, seguimiento-anticipos, archivo-banco, confirmacion,
    consulta, seguimiento, registro-egreso, registro-ingreso;
  - `cxc`: abonos-factura, cruce-anticipo-cliente, registrar-cobro.
- `{ codigo: 0 }` en el `catch`: `cxc` facturas-ingreso, liquidaciones, notas-credito, notas-debito.
- `tsr` cierre-caja:197 lee **solo** `localStorage`.

**Regla para el futuro:** cuando un backend empiece a exigir el usuario real (como hizo `b1191051`), su pantalla
pasa primero a `idUsuarioSesion()`.

## 6. Segunda pasada: las pantallas del §5 (el usuario lo pidió el 2026-10-28)

Se aplica la misma regla del §3 a **todas** las pantallas del §5, de `cxc`, `cxp` y `tsr`.

- **Si la pantalla manda un ID:** usa `idUsuarioSesion()`. Si da `null`, no envía y muestra `MENSAJE_SIN_USUARIO_SESION`.
- **Si manda el nombre** (los rellenos `'SISTEMA'`/`'sistema'`): se agrega `nombreUsuarioSesion(): string | null`
  en el mismo util, con este orden de búsqueda:
  1. `sessionStorage['usuario'].nombre`;
  2. `sessionStorage['username']`;
  3. `sessionStorage['userName']`;
  4. lo mismo en `localStorage`.

  Sin relleno. Si da `null`, la pantalla no envía y muestra el mismo mensaje.
- **Si manda un objeto `Usuario`** (los `{ codigo: 0 }` del `catch`): usa el `usuario` de la sesión. Si no hay, no envía.
- **`tsr` cierre-caja:** lee del util; deja de leer solo de `localStorage`.
- **Solo las lecturas, no las escrituras.** Si una pantalla usa el usuario solo para mostrarlo o para filtrar,
  sin mandarlo a una escritura, se deja como está y se reporta.
- **Sin backend.** `tsr` se comparte con `lap-saa-1`: antes del commit se revisa el `git status` de cada archivo.
