# API de credenciales de la app movil (`CRD.USAP`) — 7 endpoints

**Fecha:** 2026-09-03 · **Estado:** implementado en SaaBE, pendiente de prueba E2E contra BD local.
**DDL:** `docs/logica-negocio/crd/sql/DDL-USUARIO-APP-MOVIL.sql`

## Para que existe esto

La app movil "Asoprep Contigo" deja Azure y pasa a consultar el SAA a traves de un WAR de
borde (`SaaMovilBE`) desplegado en un WildFly expuesto a internet. **SaaBE queda en la
intranet y no se expone.** `CRD.USAP` es el almacen de credenciales de los participes que
usan la app — no es la tabla de usuarios del sistema.

```
App movil  --HTTPS-->  SaaMovilBE (192.168.2.3, internet)  --HTTP intranet-->  SaaBE (192.168.2.4)
```

## Los 7 endpoints

Los 6 primeros son `POST`; el 7 es un `GET`. Todos devuelven `application/json`. Ruta base: `/SaaBE/rest/usap`.

| # | Endpoint | Body | Quien lo llama | Respuesta OK |
|---|---|---|---|---|
| 1 | `validarCredencial` | `{identificacion, clave}` | SaaMovilBE (login de la app) | `ValidarCredencialResponse` |
| 2 | `cambiarClave` | `{identificacion, claveActual, claveNueva}` | SaaMovilBE (participe autenticado) | `{"mensaje": "Clave actualizada"}` |
| 3 | `crear` | `{identificacion, claveTemporal, usuario}` | **Intranet** (oficina, enrola) | `UsuarioAppDTO`, 201 |
| 4 | `resetearClave` | `{identificacion, claveTemporal, usuario}` | **Intranet** (desbloqueo de rutina) | `UsuarioAppDTO`, 200 |
| 5 | `reactivar` | `{identificacion, claveTemporal, usuario}` | **Intranet** (revertir borrado de cuenta) | `UsuarioAppDTO`, 200 |
| 6 | `desactivar` | `{identificacion, clave}` | SaaMovilBE (borrado de cuenta) | `{"mensaje": "Cuenta desactivada"}` |
| 7 | `GET estado/{identificacion}` | — (cédula en la URL) | **Intranet** (oficina, consulta el estado) | `UsuarioAppDTO`, 200 · **404** `{"mensaje"}` si no hay cuenta |

> ⛔ **Los 3, 4, 5 y 7 no se exponen jamas hacia internet.** El WAR de borde no los enruta. Son
> de intranet, para la pantalla de oficina. Si aparecen en `SaaMovilBE`, es un defecto grave.

### `ValidarCredencialResponse` — contrato congelado con SaaMovilBE

```json
{ "idEntidad": 12345, "identificacion": "0102030405",
  "nombres": "JUAN CARLOS", "apellidos": "PEREZ GOMEZ", "debeCambiarClave": false }
```

`idEntidad: Long`, `identificacion: String`, `nombres: String`, `apellidos: String`,
`debeCambiarClave: Boolean`. **Este contrato esta congelado**: cualquier cambio se coordina
con el equipo de `SaaMovilBE` antes, no despues.

`nombres` y `apellidos` **pueden venir null** — salen de `CRD.PRSN`, y una entidad sin fila
ahi (persona juridica) los deja vacios. No es error y no bloquea el login.

### `UsuarioAppDTO` — solo intranet, no congelado (lo devuelven 3, 4, 5 y 7)

`codigo, idEntidad, identificacion, estado (1/2/3), intentosFallidos, bloqueadoHasta,
debeCambiarClave, fechaCreacion, fechaUltimoAcceso, usuarioRegistro`.

**No incluye el hash de la clave, a proposito.** Ver la seccion de seguridad.

⚠️ Las tres fechas viajan como **arreglo Jackson** (`[2026,9,3,14,30,0]`), no como ISO. Es el
comportamiento de todo el sistema (ver `CLAUDE.md`, seccion Serializacion), no una rareza de
este endpoint.

## Codigos de estado — desviacion deliberada del estilo de la casa

El estilo de SaaBE es `catch (Throwable)` → 500 para todo. **Estos endpoints se apartan de
eso a proposito**, y no hay que "corregirlo":

| Codigo | Cuando | En que endpoints |
|---|---|---|
| **401** | credenciales invalidas | 1, 2 (clave **actual** mala), 6 |
| **400** | regla de negocio / validacion | 2 (clave **nueva** debil), 3, 4, 5 |
| **404** | la identificacion no tiene cuenta | 7 |
| **500** | fallo real (Oracle caido, bug) | todos |

**Por que importa el 400 en `cambiarClave`.** La app tiene un manejador global de 401 que
borra la sesion y vuelve al login. Si la clave nueva debil devolviera 401, pasaria esto:

1. Participe recien enrolado entra → `debeCambiarClave = true` → la app lo obliga a cambiarla.
2. Escribe una clave floja → 401 → la app lo interpreta como sesion vencida y **lo saca al login**.
3. Vuelve a entrar, `debeCambiarClave` sigue en true, y cae en el mismo lugar.

Queda encerrado sin poder activar su cuenta nunca, y le pasaria a **todos** los usuarios el
dia del lanzamiento. Por eso `ValidacionException extends IncomeException` (mismo molde que
`com.saa.ejb.cxp.service.ConflictoNegocioException`) y el `catch` de la subclase va **antes**.

El cuerpo de error es siempre `{"mensaje": "..."}`. En 1, 2 (clave actual) y 6 el mensaje es
**generico** — identificacion inexistente, clave mala, usuario bloqueado y usuario eliminado
dan exactamente el mismo texto, para no revelar si una identificacion esta registrada. El
401 no desambigua nada: solo separa "credenciales" de "el servidor se rompio". En los de
intranet los mensajes **si** son especificos: es personal de oficina, no hay nada que ocultar.

## Bloqueo por intentos fallidos

5 fallos consecutivos → bloqueo 15 minutos (`USAPBLHS`). El contador vive en `USAPINTF` y lo
comparten los tres endpoints que autentican (1, 2 y 6): no se puede evadir el bloqueo probando
claves por `cambiarClave`. Un bloqueo vencido se levanta solo en el siguiente intento. El
login exitoso resetea el contador y graba `USAPFCUA`.

**⛔ El intento fallido se graba en transaccion propia — no lo "simplifiques" a un `save`.**
`UsuarioAppDaoService.registrarIntentoFallido` corre con `REQUIRES_NEW`, relee la fila con
`PESSIMISTIC_WRITE`, suma el intento y bloquea al llegar a 5. Hasta 2026-10-21 el contador se
guardaba con un `save` dentro de la misma transaccion del metodo publico, y despues se lanzaba
`IncomeException`, que esta anotada `@ApplicationException(rollback = true)`: el rollback
deshacia el `save`. Resultado: `USAPINTF` nunca subia, ninguna cuenta se bloqueaba y se podian
probar claves sin limite (confirmado en vivo por el usuario: varias claves malas y la cuenta
seguia en 0 intentos). Reglas para no reintroducirlo:

- Vive en el **DAO** (otro EJB) y se llama por la referencia `@EJB`. Un metodo `REQUIRES_NEW`
  en el mismo Service llamado como `this.metodo()` no pasa por el proxy del contenedor y **no
  abre transaccion nueva**, sin ningun aviso.
- En el camino de clave mala, `autenticar` **no modifica la entidad ni hace `save`**: un cambio
  pendiente de la transaccion de afuera sobre esa fila podria dejar esperando al `PESSIMISTIC_WRITE`
  de la de adentro. El desbloqueo por vencimiento en memoria solo se aplica en el camino de clave
  buena; para clave mala lo resuelve el metodo del DAO.
- El lock evita que dos intentos en paralelo lean el mismo contador y se salten el limite.
- El metodo del DAO no absorbe errores: si falla, que falle.

`SaaMovilBE` agrega ademas su propio rate limiting por IP en `/auth/login` — es una capa
distinta y adicional, no reemplaza a esta.

## Recuperación de clave con código al correo ("olvidé mi clave")

Diseño completo: `DISENO-RECUPERACION-CLAVE-CORREO-APP-MOVIL.md`. Solo por `/movil/auth`
(`solicitarRecuperacion`, `confirmarRecuperacion`); **no están en `/rest/usap`**.

**Columnas nuevas de `CRD.USAP`** (DDL: `sql/DDL-USUARIO-APP-MOVIL-RECUPERACION.sql`, todas nullable;
`UsuarioAppDTO` **no** las lleva):

| Columna | Campo de la entidad | Qué es |
|---|---|---|
| `USAPCRHS` | `codigoRecuperacionHash` | hash PBKDF2 del código vigente (mismo formato que `USAPCLVE`); nulo = sin código |
| `USAPCRVN` | `codigoRecuperacionVence` | vencimiento (envío + 15 min) |
| `USAPCRIN` | `codigoRecuperacionIntentos` | intentos fallidos sobre ese código |
| `USAPCRUE` | `recuperacionUltimoEnvio` | hora del último envío (freno de 60 s) |
| `USAPCRVE` | `recuperacionVentanaInicio` | inicio de la ventana de 24 h de envíos |
| `USAPCRNE` | `recuperacionEnvios` | envíos dentro de esa ventana (máximo 5) |

⛔ **El DDL va ANTES del WAR.** La entidad mapea estas columnas: un WAR desplegado sin ellas hace
fallar **todo** acceso a USAP (login de la app y pantalla de oficina), no solo la recuperación.

**Ciclo del código:**

1. `solicitarRecuperacion`: responde **siempre** el mismo 200. Solo envía si hay cuenta no ELIMINADA
   (una BLOQUEADA sí), `ENTD.ENTDCRPR` con forma de correo y **una sola entidad** con ese correo
   (`lower/trim`), no hay un envío de hace menos de 60 s y van menos de 5 envíos en 24 h. Genera 6 dígitos
   con `SecureRandom`, guarda **solo el hash** y manda el correo.
2. `confirmarRecuperacion`: lo que no sean 6 dígitos, cuenta inexistente o ELIMINADA, sin código o vencido
   → 400 genérico `Codigo invalido o vencido`. Código que no coincide → suma un intento y 400 genérico; al
   5.º intento el código se anula. Con el código bueno se valida la clave nueva (misma regla que
   `cambiarClave`; si es débil → 400 con ese texto y **el código no se consume**). Si todo está bien:
   clave nueva, `debeCambiarClave = 0`, cuenta ACTIVA, intentos de login en 0, `bloqueadoHasta` nulo,
   código anulado (un solo uso). **No toca `USAPUSAR`**: ese campo es el rastro de la oficina.

⛔ **Los intentos fallidos del código se graban en transacción propia**, por el mismo motivo que los del
login (ver arriba): `ValidacionException` hereda `IncomeException` (`rollback = true`) y desharía un
`save`. `UsuarioAppDaoService.registrarIntentoCodigoRecuperacion` es `REQUIRES_NEW` con
`PESSIMISTIC_WRITE`, y se llama por la referencia `@EJB`. No lo cambies a un `save` en el Service.
Corolario: `confirmarRecuperacion` **no bloquea la fila** en su transacción (el lock de la transacción
propia esperaría al de la de afuera), así que dos confirmaciones simultáneas con el código correcto
podrían ganar las dos; el efecto es el mismo (la clave la fija quien tiene el código).

**Correo:** destino solo `ENTD.ENTDCRPR` (nunca el institucional). La sesión se busca con
`InitialContext.lookup("java:jboss/mail/AppMovil")` **al enviar, nunca con `@Resource`**: con `@Resource`,
si la sesión no está en el servidor falla el despliegue del WAR entero y se cae el SAA. Si el lookup o el
envío fallan, se escribe la causa en `System.err` (sin el código) y la respuesta sigue siendo el mismo 200.
El texto no lleva enlaces ni la cédula. La sesión `AppMovil` la configura el usuario en el `.4`.

## Estados y el ciclo de la cuenta

| Estado | Valor | Como se llega | Como se sale |
|---|---|---|---|
| Activo | 1 | `crear`, o `resetearClave`/`reactivar` | — |
| Bloqueado | 2 | 5 intentos fallidos | solo, a los 15 min; o `resetearClave` |
| Eliminado | 3 | el participe borra su cuenta desde la app (6) | **solo** `reactivar` (5) |

**`resetearClave` rechaza a un usuario ELIMINADO** y deriva a `reactivar`. Es deliberado:
revertir un borrado de cuenta debe ser un acto explicito y auditado en `USAPUSAR`, no el
efecto lateral invisible de un desbloqueo de rutina. El empleado tiene que elegir "reactivar".

El borrado de cuenta (6) es requisito de Google Play y App Store. **Elimina el acceso a la
app, no los datos del participe**: la relacion con el fondo se rige por el contrato.

## Seguridad — lo que no se debe romper

1. **La clave se guarda como hash PBKDF2WithHmacSHA256**, 100.000 iteraciones, salt de 16
   bytes, formato `iteraciones$saltBase64$hashBase64`. Verificacion en tiempo constante con
   `MessageDigest.isEqual`. El formato lleva las iteraciones adelante para poder subirlas mas
   adelante sin invalidar los hashes viejos.
2. **`UsuarioAppRest` NO publica `getAll` ni `getId`**, contra el patron estandar de la casa.
   Publicarlos expondria el hash de todas las claves en un GET sin filtro, en un backend que
   ademas no tiene autenticacion. **No los agregues.**
3. Por el mismo motivo, 3/4/5/7 devuelven `UsuarioAppDTO` y **no la entidad `UsuarioApp`** —
   serializar la entidad devolveria `claveHash` al cliente. Ya paso una vez durante el
   desarrollo y se corrigio.
4. Cualquier endpoint nuevo sobre esta tabla: revisar lo anterior **antes** de publicarlo.

## ⚠️ Pendiente que bloquea el lanzamiento

**No existe la pantalla de intranet** que llame a `crear` / `resetearClave` / `reactivar`.
Los endpoints estan; la pantalla no, y no esta asignada a nadie.

`CRD.USAP` nace vacia y **no se puede poblar por INSERT directo** (el hash lo genera el
Service en Java; una clave insertada en claro nunca valida). Sin esa pantalla, el dia que la
app salga a las tiendas **ningun participe puede entrar**. Hay que resolverlo antes del
lanzamiento.
