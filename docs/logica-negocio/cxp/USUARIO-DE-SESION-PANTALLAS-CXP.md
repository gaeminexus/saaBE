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
