# Diseño — "olvidé mi clave" en la app móvil, con código al correo (2026-10-21)

> Equipo de la app móvil (`app1`). Escrito por el árbitro `omen-app-1-arb`.
>
> **Esto revierte una decisión del plan.** `saaAPP/docs/PLAN-IMPLEMENTACION-APP-MOVIL.md` §2 decía
> que el "olvidé mi clave" lo resolvía la oficina. El usuario lo cambió el 2026-10-21. Eligió:
> - **código de 6 dígitos al correo**, no clave temporal;
> - **solo el correo personal** (`CRD.ENTD.ENTDCRPR`), **midiendo antes** si sirve.
>
> **⛔ Estado: diseño. No se construye nada hasta tener el resultado de
> `sql/MEDICION-CORREO-PARTICIPES-APP-MOVIL.sql`.** Si pocos partícipes tienen un correo personal
> propio y único, la función no le sirve a casi nadie, y hay que decidirlo antes de escribir código.
>
> La oficina sigue pudiendo resetear la clave con la pantalla «Acceso App Móvil» (`saaFE`
> `bc3468a`). Esto **se suma** a esa vía, no la reemplaza: un partícipe sin correo sigue yendo a la
> oficina.

## Por qué código y no clave temporal

Con una clave temporal al correo, **cualquiera que sepa una cédula** (es dato público) le anula la
clave al partícipe cada vez que quiera, y la clave viaja legible en el correo. Con un código **no
cambia nada hasta que alguien escribe el código**. Si un tercero pide un código con tu cédula, tu
clave sigue funcionando y el código vence solo.

## Prerrequisito: el bloqueo por intentos tiene que funcionar primero

`UsuarioAppServiceImpl.autenticar` hace `save` del contador y después lanza `IncomeException`, que
tiene `@ApplicationException(rollback = true)`. El rollback deshace el `save` y **ningún contador
se graba nunca** (`saaAPP/docs/ESTADO-EQUIPO-APP-MOVIL.md` §10, hallazgo 1).

Esta función agrega **otro** contador, el de los intentos del código. Si se escribe con el mismo
patrón, se rompe igual: alguien podría probar el millón de códigos posibles sin límite. **Por eso
la corrección del bloqueo va primero, y el contador de esta función usa la misma solución.**

La forma de corregirlo está en la orden del bloqueo; esta sección solo fija la dependencia. Lo
que la orden tiene que garantizar: el incremento del contador queda grabado aunque la operación
termine en error.

## Flujo

```
App: "¿Olvidaste tu clave?" en el login
  → pantalla 1: cédula
      POST borde /rest/auth/recuperar/solicitar  {identificacion}
        → saaBE POST /movil/auth/solicitarRecuperacion
      SIEMPRE 200 y el MISMO mensaje, haya o no cuenta y haya o no correo:
      "Si tu cédula tiene cuenta en la app y un correo personal registrado en ASOPREP,
       te enviamos un código. Si no te llega, acércate a la oficina."
  → pantalla 2: código + clave nueva + confirmación
      POST borde /rest/auth/recuperar/confirmar  {identificacion, codigo, claveNueva}
        → saaBE POST /movil/auth/confirmarRecuperacion
      200 → "Clave actualizada", y la app vuelve al login
      400 → "Código inválido o vencido"  (genérico; no dice cuál de las dos cosas)
      400 → clave nueva débil, con el MISMO texto y la MISMA regla que cambiarClave
```

### `solicitarRecuperacion` en `saaBE`, paso a paso

1. Busca USAP por identificación. **En cualquiera de estos casos no hace nada y responde igual que
   en el caso bueno**:
   - no hay cuenta;
   - la cuenta está ELIMINADA (3);
   - el correo está vacío o no tiene forma de correo;
   - el correo está **repetido** en otra entidad (ver el bloque B de la medición): un correo
     compartido le mandaría el código a otra persona.
2. **Freno de envíos**:
   - no se manda un código nuevo si el último se mandó hace menos de 60 segundos;
   - máximo 5 envíos por cuenta en 24 horas.

   Pasado el límite, responde igual que en el caso bueno, sin enviar nada.
3. Genera un código de 6 dígitos con `SecureRandom`. Guarda **el hash** del código, nunca el
   código en claro, con el mismo PBKDF2 de las claves. Guarda también el vencimiento (ahora + 15
   minutos), pone los intentos del código en 0 y anota la hora del envío.
4. Manda el correo (ver "Correo").
5. **Una cuenta BLOQUEADA (2) sí puede pedir código.** Demostrar acceso al correo es justamente
   la salida de un bloqueo.

Si el envío de correo falla (SMTP caído), se registra en el log del servidor y el partícipe recibe
el mismo mensaje de siempre. No se le dice "falló el correo", porque eso revelaría que la cédula
tiene cuenta.

### `confirmarRecuperacion` en `saaBE`, paso a paso

1. Busca USAP. Si no hay cuenta, si no hay código vigente o si ya venció → 400 genérico.
2. Si los intentos del código llegan a 5 → se anula el código (vuelve a nulo) y 400 genérico. El
   partícipe tiene que pedir otro.
3. Si el código no coincide (verificación en tiempo constante, como la clave) → incrementa los
   intentos, **grabado aunque la respuesta sea error** (ver el prerrequisito), y 400 genérico.
4. Valida la clave nueva con **la misma regla** de `cambiarClave`: mínimo 8 caracteres, al menos
   una letra y un número. Si no la cumple → 400 con el texto de esa regla. No consume el código
   ni suma un intento: el partícipe ya demostró que tiene el código.
5. Si todo está bien:
   - guarda el hash de la clave nueva y pone `debeCambiarClave = 0`, porque la eligió él mismo;
   - deja la cuenta ACTIVA (1), con los intentos de login en 0 y `bloqueadoHasta` nulo;
   - **anula el código**: es de un solo uso;
   - **no toca `USAPUSAR`**: ese campo es el rastro de la oficina, y esto lo hizo el partícipe.

### Códigos HTTP

Siguen la regla de USAP que ya existe: **400, no 401**, para que la app no lo tome como sesión
vencida. Los dos endpoints son **públicos**: no llevan token, porque quien los usa no tiene sesión.

## DDL: columnas nuevas en `CRD.USAP`

| Columna | Tipo | Qué es |
|---|---|---|
| `USAPCRHS` | `VARCHAR2(200)` | hash PBKDF2 del código vigente; nulo si no hay código |
| `USAPCRVN` | `TIMESTAMP` | vencimiento del código |
| `USAPCRIN` | `NUMBER` | intentos fallidos del código vigente |
| `USAPCRUE` | `TIMESTAMP` | hora del último envío (freno de 60 s) |
| `USAPCRVE` | `TIMESTAMP` | inicio de la ventana de 24 h de envíos |
| `USAPCRNE` | `NUMBER` | envíos dentro de esa ventana |

- El script es `crd/sql/DDL-USUARIO-APP-MOVIL-RECUPERACION.sql`, con controles previos y reverso,
  igual que el DDL de USAP.
- Los nombres siguen la regla de 8 caracteres de `CLAUDE.md`; **antes de escribir el DDL, verificar
  que no choquen** con columnas existentes de USAP.
- Son columnas nuevas en una tabla nuestra, y no consumen ningún `PRBR`/`PDTR`.
- `CRD` es el esquema de `omen-saa-1`: se le avisa cuando el DDL esté escrito, igual que con la
  tabla.
- **`UsuarioAppDTO` NO lleva ninguna de estas columnas**: la pantalla de oficina no necesita el hash
  del código.

## Correo

- **Destino:** solo `ENTD.ENTDCRPR`, con `trim()`. Nunca el institucional.
- **Remitente:** sesión de correo JNDI **`java:jboss/mail/AppMovil`** en el WildFly del `.4`. Se
  configura en el servidor, no en el código. La única sesión existente es
  `java:jboss/mail/Facturacion` (`EmailFacturaServiceImpl`); reusarla haría que el partícipe reciba
  el código desde la dirección de facturación. **Qué cuenta va de remitente lo decide el usuario.**
- **Contenido:** el nombre del partícipe, el código, "vale 15 minutos", y "si no lo pediste, ignora
  este correo: tu clave sigue igual". **Sin enlaces**, porque un enlace en un correo de "recupera tu
  clave" es el molde exacto del phishing, y no queremos que los partícipes se acostumbren a hacer
  clic en esos. Sin la cédula completa.
- **La app no muestra a qué correo se mandó**, ni siquiera enmascarado. Cualquiera con una cédula
  podría ver parte del correo de otro.

## Borde (`saaBEMobile`)

- Dos rutas nuevas **públicas**: `POST /rest/auth/recuperar/solicitar` y `POST /rest/auth/recuperar/confirmar`.
  - Se agregan a la lista de rutas públicas del filtro **por igualdad exacta**, igual que `auth/login`.
  - La lista blanca pasa de 10 GET + 4 POST a **10 GET + 6 POST**. Hay que recontarla en el
    contrato y en el estado.
- **Rate limiting** por IP, con la misma lógica de `X-Forwarded-For` que ya se corrigió.
  - Además, un límite por cédula en el borde, porque un atacante puede repartir los intentos
    entre varias IP.
- En `saaBE`, los dos endpoints nuevos van en `/movil`, con `@ClaveMovilRequerida`, y se recuentan
  en `MovilApplicationConfig`: hoy son 9 GET + 4 POST y pasarían a **9 GET + 6 POST**.
- **Nunca** se enruta `/rest/usap/*` desde el borde.

## App (`saaAPP`)

- Enlace «¿Olvidaste tu clave?» en el login.
- Dos pantallas: cédula; y código + clave nueva + confirmación.
- El texto de la pantalla 1 es **exactamente** el mensaje genérico. La app no deduce nada de la
  respuesta.
- El error de red se distingue del 400. Es el mismo defecto #10 que ya pasó con el login: un
  error de red no puede decir "código inválido".

## Contrato

`saaBEMobile/docs/CONTRATO-MOVIL-V1.md` suma las dos rutas, como V1.1.
- Lo congela el árbitro con ejemplos **reales** contra `saaBE`, antes de que la app se recablee.
- El contrato de intranet (`CONTRATO-INTRANET-MOVIL.md`) suma los dos endpoints de `/movil`.
- `API-USUARIO-APP-MOVIL.md` documenta las columnas nuevas y el ciclo del código.

## Orden de ejecución

0. **Medición** (usuario) → decisión de seguir o no.
1. **Corrección del bloqueo de intentos** (`omen-app-1-saa-be-app`). Va antes de todo.
2. DDL de las columnas (árbitro) → lo corre el usuario en local.
3. `saaBE`: los dos endpoints `/movil` y el envío de correo (`omen-app-1-saa-be-app`).
4. Borde: las dos rutas públicas y el rate limiting por cédula (`omen-app-1-be-app`).
5. El árbitro congela el contrato V1.1 con ejemplos reales.
6. App: el enlace y las dos pantallas (`omen-app-1-app`).
7. Infraestructura (usuario): la sesión `java:jboss/mail/AppMovil` en el `.4`, el DDL en
   producción, y el redespliegue de `saaBE`, el borde y un APK nuevo.

## Lo que decide el usuario antes del paso 3

- Con qué cuenta de correo sale el código (la sesión `AppMovil`).
- Si los números de la medición alcanzan para seguir.
