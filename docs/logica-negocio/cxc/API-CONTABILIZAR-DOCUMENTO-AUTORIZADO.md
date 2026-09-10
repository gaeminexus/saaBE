# Contabilizar un documento ya autorizado — el botón que repara

**Equipo:** `omen-saa-2` · **Fecha:** 2026-09-10 · **Encargo:** el usuario.
**Módulo:** `cxc`. **Alcance de esta primera entrega: retención V2.**

---

## 1. Por qué existe, y por qué la validación previa no alcanza

El usuario reportó: *"las retenciones ya se emiten pero no están afectando al saldo de la factura
relacionada."*

Y preguntó cuál de dos caminos convenía: **(A)** impedir la emisión cuando falten cuentas
contables, o **(B)** un botón que contabilice después. La respuesta medida:

**La (A) ya existe, en los seis emisores.** Verificado archivo por archivo: factura, liquidación,
nota de crédito, nota de débito y las dos retenciones validan las cuentas antes de grabar y
devuelven `VALIDACION_CONTABLE` con la lista de las que faltan. No hay nada que construir ahí.

**Y aun así no alcanza, por dónde está el punto de no retorno:**

```
PASO 0:   validar cuentas contables      ← acá previene la validación que ya existe
PASO 1:   grabar
PASO 4:   autorizar ante el SRI          ← IRREVERSIBLE
PASO 5:   asiento contable               ← acá todavía puede fallar
PASO 5.1: cruce con la factura
```

Si el asiento falla en el PASO 5 —base caída, una cuenta borrada entre la validación y la emisión,
el servidor reiniciado— **el comprobante ya está autorizado ante el SRI y no se puede des-emitir.**
Queda autorizado, sin asiento y sin cruce. Ninguna validación previa cubre eso, porque el fallo
ocurre después del punto de no retorno.

> **Es el patrón que este equipo ya registró tres veces en septiembre: una guarda sin puerta de
> vuelta es una pared, no una guarda.** La validación previa sola deja documentos autorizados sin
> forma de completarlos.

**Y hay un caso concreto abierto hoy:** las retenciones que se autorizaron por el botón "Reenviar
al SRI" antes del commit `18a37d2b` quedaron sin asiento y sin cruce. **El botón de reenviar no las
alcanza**: su guarda las rechaza con 409 porque ya están autorizadas, y hace bien.

---

## 2. El endpoint

```
POST /rest/rtv2/contabilizar/{idRetencion}
```

Sin cuerpo. Todo se deriva de la retención.

### 2.1 Qué hace

1. Carga la `RetencionV2`. No existe → **404**.
2. **Guarda de estado: sólo `estado = 5` (autorizada).** Cualquier otro → **409** con el motivo.
   ⛔ **No es un atajo para contabilizar algo que no se emitió.** Un documento no autorizado se
   completa por el camino de emisión (`procesarCompleta`) o por el de reenvío, no por acá.
3. Llama a **`cerrarContabilidadYCruceRetencionV2(idRetencion, resultado)`** — el mismo método que
   ya usan la emisión y el reenvío desde el commit `18a37d2b`. **No se escribe lógica nueva.**
4. Devuelve qué quedó hecho (§3).

### 2.2 Idempotente por diseño, no por un chequeo del botón

Los dos pasos ya cortan solos si el trabajo estaba hecho — verificado en el código, no supuesto:

- `generarContabilidadRetencionV2` → sale con `yaExistia = true` si `retencion.getAsiento() != null`
- `aplicarPagoRetencionV2` → sale con `yaExistia = true` si ya hay una `AplicacionPagoCxp` activa
  para `("RETENCION_V2", idRetencion)`

**Apretar el botón dos veces no duplica nada.** Por eso el frontend no tiene que averiguar si falta
algo antes de mostrarlo (§4).

---

## 3. Respuestas

**200** siempre que el proceso haya corrido, con el detalle de lo que pasó:

```json
{ "exito": true,  "asiento": 12345, "aplicacionPago": 678, "mensaje": "Asiento y cruce registrados." }
{ "exito": true,  "yaEstabaCompleto": true, "mensaje": "La retención ya tenía asiento y cruce." }
{ "exito": false, "contabilidadPendiente": true, "erroresContables": ["El código de retención '304A' (Renta) no tiene cuenta contable asignada en TSRI. Configure la cuenta en Facturación → Tipos SRI."], "mensaje": "..." }
```

| Código | Cuándo |
|---|---|
| **200** | El proceso corrió. **Mirar `exito`, nunca el HTTP** |
| **404** | No existe esa retención |
| **409** | No está autorizada (`estado != 5`) |
| **500** | Error inesperado |

### 3.1 🔴 Cuando faltan cuentas, el mensaje tiene que decir CUÁLES

`validarCuentasContablesRetencion` ya devuelve la lista con el texto listo para el usuario
(*"El proveedor 'X' no tiene cuenta contable configurada. Configure la cuenta en Tesorería → Persona
→ Cuentas Contables (Tipo: Facturas, Rol: Proveedor)"*). **Esa lista tiene que llegar al frontend en
`erroresContables`**, igual que ya lo hace `procesarCompleta` en su etapa `VALIDACION_CONTABLE`.

Un *"no se pudo contabilizar"* a secas obliga al usuario a adivinar qué cuenta falta. El dato existe;
no perderlo es la mitad del valor de este botón.

---

## 4. Frontend

**Pantalla:** `cxc/forms/gestionar/consulta-documentos-electronicos` — la misma donde vive
"Reenviar al SRI", que es donde el usuario ya va a buscar estas cosas.

- **Botón "Contabilizar"**, visible sólo para retenciones con **`estado = 5`**.
- ⛔ **No calcules en el frontend si falta el asiento o el cruce.** El backend es idempotente y
  responde `yaEstabaCompleto` cuando no había nada que hacer. Duplicar esa regla en la pantalla es
  cómo se desincronizan las dos.
- Confirmación previa: *"Se generará el asiento contable y el cruce con la factura. ¿Continuar?"*
- Mientras corre: deshabilitado con spinner.
- Al volver: refrescar la fila y **mostrar el mensaje del backend tal cual**. Si viene
  `erroresContables`, listarlos uno por línea — el mismo tratamiento que ya le da
  `retencionesv2.component.ts` a la etapa `VALIDACION_CONTABLE`.

---

## 5. Lo que esta entrega NO hace

- **Sólo retención V2.** Factura, nota de crédito, nota de débito y liquidación de compra pueden
  quedar autorizadas sin asiento por el mismo motivo, y cada una tiene su propio método de
  contabilización. Extenderlo es el paso siguiente y el endpoint queda con forma de poder hacerlo.
- **No toca la guarda que ata el cruce al asiento.** Hoy `aplicarPagoRetencionV2` exige que exista
  el asiento, y el asiento se salta entero cuando `facturador.getGeneraConta() != 1`. Medido que el
  acoplamiento **no es técnico** (`aplicarRetencionEmitida` sólo guarda el asiento como referencia y
  la FK no exige no-nulo en el mapeo), pero soltarlo es decisión de negocio del usuario y está
  pendiente. **Mientras siga así, un facturador sin contabilidad no va a poder cruzar ni con este
  botón.**
- **No repara en lote.** Es de a un documento. Si aparecen muchos, se decide si vale un proceso
  masivo.
