# Reenviar al SRI una retención electrónica que quedó atascada

**Equipo:** `omen-saa-2` · **Fecha:** 2026-09-09 · **Encargo:** el usuario, urgente.
**Módulo:** `cxc` (retención V2, `CBR.RTV2`).

---

## 1. Por qué existe

**2026-09-09, caso real.** El usuario emitió una retención (clave
`0909202607179136759600120010010000073381234567811`) y el servicio de **recepción** del SRI
contestó con un fault de su propia infraestructura:

```xml
<faultcode>soap:Server</faultcode>
<faultstring>org.hibernate.exception.GenericJDBCException: Could not open connection</faultstring>
```

Es decir: **el SRI no pudo abrir conexión contra su propia base**. Nada del lado nuestro.

El problema es lo que pasó después:

1. `RetencionV2ServiceImpl:911` graba **`estado = 4` (enviada) sin mirar** qué contestó recepción.
   La retención quedó marcada como enviada cuando el SRI nunca la recibió.
2. El único camino de emisión que el frontend usa es **`POST /rtv2/procesarCompleta`**, que
   **crea una retención desde cero** — no reenvía una existente.
3. El backend **sí tiene** `POST /rtv2/autorizar`, que reenvía… y **ninguna pantalla lo llama**
   (verificado por grep sobre todo `saaFE`: cero llamadas).
4. El botón **"Reintentar Estado"** de la pantalla de consulta llama a
   `/consultarYActualizarEstado`, que **solo consulta autorización**. Mientras el SRI no la haya
   recibido devuelve *"No existen datos para los parámetros ingresados"* — para siempre.

**Resultado: la retención quedó sin salida.** Es la misma forma de fallo que este equipo ya
registró tres veces en septiembre: **la función existe, le falta la puerta.**

> **Y el SRI se va a volver a caer.** Esto no es un arreglo para un caso: es la puerta que faltaba.

---

## 2. El endpoint nuevo

```
POST /rest/rtv2/reenviarSRI/{idRetencion}
```

Sin cuerpo. Todo lo que hace falta se deriva de la retención.

### 2.1 Qué hace, en orden

⚠️ **Corregido el 2026-09-09 (ítem 16). La versión anterior de este punto (3-4-5-6 abajo) decía
que este endpoint "no regenera ni re-firma en ningún caso", y era falso para el estado 6.** Se
escribió pensando sólo en el caso de una retención que nunca llegó al SRI (3/4); el caso real que
lo destapó fue la retención 216, en estado 6 (no autorizada) — reenviar el mismo XML firmado que
el SRI ya rechazó por contenido es garantía de que lo vuelva a rechazar. Queda así:

1. Carga la `RetencionV2` por id. Si no existe → **404**.
2. **Guarda de estado** (§3).
3. Resuelve `idFacturador`, `ambiente` y `clave` **de la propia retención**, nunca de parámetros
   del cliente. El `ambiente` sale del facturador en base, como ya hace
   `RetencionV2ServiceImpl:131`.
4. **El XML a enviar depende de POR QUÉ quedó atascada** (detalle y por qué en §3):
   - **Estados 3 (firmada) y 4 (enviada):** el contenido nunca se puso a prueba ante el SRI.
     Busca el **XML firmado** en `PathRetencionV2` con `alterno = 3` y lo lee del disco tal cual
     está. Si no hay fila `alterno = 3`, o el archivo no está en disco → **409** con mensaje
     explícito. ⛔ **No regenerar ni re-firmar en esta rama**: el contenido no se probó y la
     firma vale; regenerarlo la cambiaría sin que nadie lo pida.
   - **Estado 6 (no autorizada):** el SRI YA opinó sobre el contenido y lo rechazó. Se
     **regenera** el XML (`generarXMLRetencionV2`, que reaplica la validación del tipo de
     identificación del ítem 13) y se **re-firma** (`signatureService.firmarXMLFacturador`). Si
     regenerar o firmar falla → **409** con el mensaje real, nunca un 500 genérico. La clave de
     acceso no cambia (sale de fecha de emisión + secuencial, que no se tocan), así que
     regenerar no quema ningún secuencial.
5. Llama a `retencionV2Service.autorizarRetencionV2(idFacturador, ambiente, 1L /*conectaSRI*/,
   clave, idRetencion, xmlFirmado, null, null)` — el método que ya existe y ya funciona. Esa
   misma llamada inserta la fila nueva de `PathRetencionV2` alterno=3 (su paso 1-2, sin
   condición), así que el XML regenerado de la rama del 6 queda registrado sin que este
   endpoint tenga que hacerlo aparte.
6. Devuelve el resultado (§4).

### 2.2 Lo que hace, y lo que NO — corregido el 2026-09-10 (ítem 23, segunda corrección de este documento)

⚠️ **Este punto decía "no vuelve a generar contabilidad ni a aplicar el pago: eso ya se hizo
cuando se emitió". Era falso en TODOS los casos que este endpoint atiende, no en uno.**
`procesarRetencionV2Completa` genera el asiento contable (su PASO 5) y registra el cruce con la
factura de compra (su PASO 5.1) **DESPUÉS** de autorizar ante el SRI, y **retorna temprano si el
SRI no autoriza en el primer intento** — ninguno de los dos pasos corre. Los tres estados que
este endpoint reenvía (3 firmada, 4 enviada, 6 no autorizada) son exactamente los tres casos en
los que el SRI **todavía no autorizó** en el primer intento. Si el reenvío consigue la
autorización, sin este punto corregido la retención quedaba autorizada ante el SRI pero **sin
afectar el saldo de la factura relacionada** — el síntoma real que reportó el usuario.

- **Si el reenvío termina en AUTORIZADO, SÍ genera el asiento contable y SÍ registra el cruce**
  con la factura de compra — el mismo PASO 5 / 5.1 de `procesarRetencionV2Completa`, extraído a
  `cerrarContabilidadYCruceRetencionV2` para que los dos caminos usen el mismo código. Los dos
  pasos son idempotentes (si ya existe asiento o cruce, no se duplican).
- **Un error en el asiento o en el cruce NUNCA tumba el reenvío** — el comprobante ya está
  autorizado ante el SRI y eso es irreversible. Si falla, queda en `avisos`/`advertenciaAsiento`/
  `advertenciaAplicacion` de la respuesta (§4), nunca como un 500.
- La respuesta dice **explícitamente** si el asiento y el cruce quedaron hechos: `asiento`
  (número alterno) y `aplicacionPago` (id) cuando salió bien; `contabilidadPendiente` +
  `advertenciaAsiento`, o `cruceFacturaPendiente` + `advertenciaAplicacion`, cuando no.
- No crea ni modifica detalles.
- No re-firma **en la rama 3/4**. (En la rama 6 sí regenera y re-firma — ver §2.1 punto 4.)

### 2.3 Nota sobre el acoplamiento asiento↔cruce (ítem 22, 2026-09-10 — sin resolver, reportado)

El cruce con la factura (`aplicarPagoRetencionV2`) exige que la retención ya tenga asiento
contable, y `generarContabilidadRetencionV2` no genera asiento si el facturador tiene
`generaConta ≠ 1` — con lo cual **un facturador que no genera contabilidad tampoco cruza la
retención contra la factura**, aunque el cruce es un hecho de cuentas por pagar, no de
contabilidad. Medido contra el código (no contra la base): la guarda es una decisión de diseño
de `aplicarPagoRetencionV2` (línea propia, `if (retencion.getAsiento() == null) throw ...`), no
una necesidad técnica de `AplicacionPagoCxpServiceImpl.aplicarRetencionEmitida` — ese método sólo
guarda el asiento como referencia (`aplicacion.setAsiento(asiento)`), y la columna
`PGS.APLP.APLPASNT` no está mapeada como `nullable=false` en la entidad. Si el cruce pudiera
correr sin asiento previo (sujeto a que la columna en Oracle realmente admita `NULL`, no
verificado desde código) es una decisión de negocio, pendiente de que el usuario la tome — no se
tocó nada de esto.

---

## 3. Guarda de estado — y por qué así

`estado` en `RetencionV2` **no es el flag genérico**: es el flujo de emisión electrónica
(ver `CriterioVentaVigente`, commit `282c3361`).

| `estado` | Significa | ¿Reenviar? | ¿Regenerar XML? |
|---|---|---|---|
| 1 | creada | ❌ **409** — nunca se firmó; el camino es `procesarCompleta` | — |
| **3** | firmada | ✅ **sí** | ❌ **no** — reusa el firmado de disco |
| **4** | enviada | ✅ **sí** | ❌ **no** — reusa el firmado de disco |
| 5 | **autorizada** | ❌ **409** — *"La retención ya está autorizada (Aut. XXX). No se reenvía."* | — |
| **6** | no autorizada | ✅ **sí** — el SRI la rechazó | ✅ **sí** — regenera y re-firma |

⛔ **El 5 se rechaza sin excepción.** Reenviar una autorizada no la duplica en el SRI (contestaría
`CLAVE ACCESO REGISTRADA`), pero sí volvería a disparar el guardado de XML y la generación del
RIDE sobre un comprobante cerrado. No hay razón para permitirlo.

### 3.1 ⚠️ Por qué el 6 se regenera y el 3/4 no — no es lo mismo "no llegó" que "llegó y lo rechazaron"

**3 y 4 son "el SRI todavía no dijo nada del contenido."** El XML pudo no haber llegado nunca
(estado 3, nunca se intentó enviar), o haber llegado a recepción sin que autorización confirmara
nada todavía (estado 4). En los dos casos el contenido nunca se sometió a juicio: regenerarlo
podría cambiarlo (y cambiar la firma) sin que nadie lo pida, sobre un documento que quizás el SRI
ya tiene tal cual está.

**6 es "el SRI ya dijo que no."** El rechazo es sobre el CONTENIDO del XML que está en disco —
caso real: retención 216, `identificador 69 — ERROR EN LA IDENTIFICACION DEL RECEPTOR`, porque el
XML llevaba `tipoIdentificacionSujetoRetenido=05` (cédula) con una identificación de 13 dígitos
(RUC). Ese XML **tiene el defecto adentro**. Reenviarlo tal cual es simplemente pedirle al SRI que
lo rechace otra vez. La única forma de que un reenvío del estado 6 tenga sentido es que el
contenido cambie — y el único contenido que puede cambiar sin que nadie toque un dato a mano es el
que sale de una corrección de código ya aplicada (como el ítem 13: la 216 se arregla sola al
regenerar, porque ahora `tipoIdentificacionSujetoRetenido` resuelve `04` en vez de `05`).

⛔ **No unificar las dos ramas.** Es la corrección de un error del propio contrato original de
este documento (que decía "no regenerar ni re-firmar" sin distinguir por qué estaba atascada):
unificarlas de nuevo repite ese error.

> **Regla que este equipo aprendió el 2026-09-08 (§37) y aplica acá:** antes de exigir un estado,
> comprobar que algo lo produzca. Los tres estados permitidos (3, 4, 6) los escribe
> `RetencionV2ServiceImpl` en el flujo de emisión — verificado en sus `setEstado`.

### 3.2 Nota: un corte de transporte también puede terminar en estado 6

Un fallo de RED al llamar al servicio de autorización (ej. `Connection reset`, no un rechazo del
SRI) cae en el mismo `catch` que un rechazo real y también deja la retención en estado 6
(`RetencionV2ServiceImpl`, alrededor de la línea 1093). Un reenvío posterior de esa retención
entra igual por la rama de regeneración — no hace daño (el contenido se vuelve a armar igual si no
había ningún defecto), pero vale saber que "estado 6" no siempre significa "el SRI juzgó el
contenido y lo rechazó": a veces significa "no se supo qué contestó el SRI, y el sistema lo trató
como rechazo por prudencia".

---

## 4. Respuestas

**200 — el reenvío corrió.** El contenido dice cómo terminó:

```json
{ "exito": true,  "estado": 5, "mensaje": "Comprobante Autorizado", "autorizacion": "09092026...", "clave": "0909..." }
{ "exito": false, "estado": 4, "mensaje": "Estado: DEVUELTA | [ERROR] Id:39 Msg:...", "clave": "0909..." }
```

⚠️ **`200` no significa autorizado.** Significa que se pudo reenviar y el SRI contestó. El
frontend **debe** mirar `exito`/`estado`, nunca el código HTTP.

| Código | Cuándo |
|---|---|
| **200** | Se reenvió y el SRI contestó (autorizado o no) |
| **404** | No existe esa retención |
| **409** | Estado no reenviable (1 o 5), o falta el XML firmado |
| **500** | Error inesperado. El mensaje lleva el texto del fault |

### 4.1 🔴 El mensaje tiene que decir la causa real

En este incidente el usuario vio *"No existen datos para los parámetros ingresados"* —que describe
una consecuencia— mientras la causa (`Could not open connection` del servidor del SRI) quedaba
enterrada en un `.txt` del disco que nada le señalaba.

**Cuando recepción falle, el `mensaje` de la respuesta tiene que traer el `faultstring` o el
`<mensaje>`/`<informacionAdicional>` que devolvió el SRI**, no un texto genérico. Es la diferencia
entre "el SRI está caído, reintentá en un rato" y una tarde perdida.

---

## 5. Frontend

**Pantalla:** `cxc/forms/emitir/retencionesv2` (y, si la fila lo permite,
`cxc/forms/gestionar/consulta-documentos-electronicos`).

- **Botón "Reenviar al SRI"**, visible **solo** si `estado ∈ {3, 4, 6}`. Con `estado = 5` no se
  muestra; con `estado = 1` tampoco.
- Confirmación previa: *"Se volverá a enviar el comprobante al SRI. ¿Continuar?"*
- Mientras corre, deshabilitado con spinner: el reenvío llama a dos web services del SRI y puede
  demorar varios segundos.
- Al volver: refrescar la fila y **mostrar el `mensaje` del backend tal cual**, sin reemplazarlo por
  un texto propio. Verde si `exito = true`, ámbar si `false`.
- ⛔ **No duplicar la regla de estados en el frontend** más allá de mostrar u ocultar el botón. La
  decisión la toma el backend; el botón solo evita el clic obvio.

---

## 6. Lo que este frente NO resuelve

- **El `estado = 4` que se graba aunque recepción falle** (`RetencionV2ServiceImpl:911`). Sigue
  igual: es frente aparte, y hasta que se arregle una retención puede figurar como "enviada" sin
  estarlo. Este botón es justamente lo que la saca de ahí.
- **El ambiente fijo en `1L`** de `RetencionV2Rest:161` (`procesarCompleta`), con el comentario
  *"cambiar a 2L para producción"*. Hoy queda tapado porque el servicio lo pisa con el valor del
  facturador — pero un facturador sin ambiente configurado mandaría comprobantes al ambiente de
  pruebas en silencio.
- Facturas, notas de crédito y notas de débito: tienen el mismo hueco. Si esto funciona, se replica.
