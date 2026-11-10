# API — Consola de pruebas paso a paso de Security Data (`/rest/sdpr`)

2026-11-10 · Diseño: `DISENO-PANTALLA-PRUEBAS-SECURITY-DATA.md` · Referencia del proveedor: `REFERENCIA-API-SECURITY-DATA-DATAGREE.md`.
Base: `/SaaBE/rest/sdpr/...`. **El navegador nunca habla con Security Data**: el backend usa las credenciales de WildFly
(`saa.firma.securitydata.*`). **Las pruebas no graban nada** (ni `FRCN`, ni `FRFR`, ni adjuntos).

## Reglas generales
- **Interruptor:** propiedad de sistema `saa.firma.securitydata.pruebas=true`. Sin ella (o en `false`) todo responde
  **403 `PRUEBAS_DESHABILITADAS`**, salvo `configuracion`, que responde siempre.
- `usuario` (query) obligatorio y validado (U-2): sin usuario válido → 400 y no se hace nada.
- Todas las rutas son `POST /rest/sdpr/{ruta}?usuario=`. Cuerpo JSON (`Content-Type: application/json`; **`autenticar` también
  necesita un cuerpo: mande `{}`**). Las rutas con archivos son **multipart/form-data** con la parte **`datos`** (JSON, como
  texto) y la parte **`archivo`** (uno o varios PDF; el nombre del archivo viaja en el `filename` de la parte).
- ⚠️ `crear-tarea` **crea la tarea de verdad**: envía correo y WhatsApp reales al firmante. Pedir confirmación en la pantalla.

## Respuesta común (200)
```json
{ "paso": 2, "metodo": "POST", "ruta": "/signersValidation",
  "peticion": { "cuerpo": { … }, "archivos": [ { "nombre": "x.pdf", "bytes": 1234 } ] },
  "httpStatus": 200, "duracionMs": 1660, "respuesta": { … },
  "resumen": null, "pdfBase64": null, "pdfNombre": null }
```
- `peticion` y `respuesta` van **enmascaradas** (abajo). **Un HTTP de error del proveedor (400, 404…) NO es un error de esta API:**
  se devuelve 200 con su `httpStatus` y su `respuesta`, para verlo en pantalla.
- `resumen`: solo en `autenticar` → `{exito, tipo:"Bearer", tokenEnmascarado, expira}` (`expira` = instante ISO, del propio JWT).
- `pdfBase64` / `pdfNombre`: solo en `descargar-firmado` y `certificado-trazabilidad`. **Decisión:** el PDF completo viaja en
  el mismo cuerpo, como base64 en `pdfBase64` (en `respuesta.result.base64` va enmascarado como «[PDF base64, N bytes]»); el FE
  arma el `Blob` y descarga. No hay endpoint de descarga aparte ni estado en el servidor.

## Enmascarado (peticion y respuesta)
`password`, `passSignature`, `signatureKey` → `******` · `fingerprintCode` → `******` · token (y cualquier JWT) → 12 caracteres + `…` ·
`signerToken` (enlace del partícipe) → solo `https://dominio/…` · todo texto base64 de más de 200 caracteres → `[PDF base64, N bytes]`.
En el log del servidor solo van el nombre de la operación, el HTTP y la duración (nunca cuerpos, token ni código dactilar).

## Rutas
| Paso | Ruta | Entrada | Llamada al proveedor |
|---|---|---|---|
| 0 | `configuracion` | — | local |
| 1 | `autenticar` | `{}` | `POST /authenticateUser` |
| 2 | `validar-firmante` | JSON | `POST /signersValidation` |
| 3 | `completar-datos` | multipart | `POST /completeSignatoryData` |
| 4 | `firma-representante` | JSON | `POST /findSignatureReferences` o `/consultSDSignature` |
| 5 | `crear-tarea` | multipart | `POST /createTask` |
| 6 | `detalle-tarea` | `{processId}` | `GET /taskDetail/{processId}` |
| 7 | `descargar-firmado` | `{pdfId}` | `POST /base64?pdfId=` |
| 8 | `certificado-trazabilidad` | `{processId}` | `GET /traceabilityCertificate/{processId}` |
| 9 | `cancelar-tarea` | `{processId}` | `GET /cancelTask/{processId}` |

`processId` y `pdfId`: solo letras, números, guion y guion bajo (si no, 400 `PARAMETRO_INVALIDO`).

### 0 · `configuracion` (devuelve esto, no la forma común)
`{paso:0, habilitadas, url, usuario, claveConfigurada, ruc, cedulaRepresentante (solo informativa), referenciaRepresentanteConfigurada,
metodoNotificacion, propiedadesFaltantes:[…]}`. Nunca trae la clave.

### 2 · `validar-firmante`
Campos con los nombres de Security Data. Obligatorio: `dni`. Defaults: `tpSignature=SC_TP_F_AVANZADA_PN`,
`tpIdentification=TIPO_IDENTIFICACION_CEDULA`, `countryCode=+593`, `ruc=""`, `personPosition=""`, `regulatedSuper=false`,
`reuseSignature=false`. Además: `fingerprintCode`, `email`, `numPhone`, `biometrixType`, `biometrixParam`, `signatureSource`.
**«Llenar desde socio»:** `GET /rest/sdpr/socio/{cedula}?usuario=` → `{encontrado, nombre, correo, celular}` (no llama al proveedor;
`encontrado:false` si no existe). El código dactilar no se guarda.

### 3 · `completar-datos` (multipart)
`datos` = `{ "signatoryId": "…", "campos": { "CP_EMI_NOMBRES": "…", "CP_EMI_APE_PATERNO": "…" } }` (`campos` es un objeto clave→valor).
`archivo` = 0..n PDF llamados `{NEMONICO}_{DNI}.pdf`.

### 4 · `firma-representante`
`{ "accion": "BUSCAR", "dni": "…", "names": "…" }` → `findSignatureReferences` (dni y/o names). `{ "accion": "CONSULTAR",
"tpSignature": "SC_TP_F_JURIDICA_RL", "dni": "…", "ruc": "…" }` → `consultSDSignature`. `accion` es opcional: sin ella, si viene
`tpSignature` es CONSULTAR y si no BUSCAR. (Una sola llamada por ejecución: `metodo`/`ruta` dicen cuál se hizo.)

### 5 · `crear-tarea` (multipart)
`datos` (campos planos; todos opcionales salvo los firmantes y la posición):
`socioSignatoryId`, `socioTipo` (`TP_SIGNER_NORMAL`), `socioPagina`, `socioX`, `socioY`, `socioAncho`, `socioAlto`;
`representanteSignatoryId`, `representanteTipo` (`TP_SIGNER_EMBEDDED`), `representantePagina`, `representanteX`, `representanteY`,
`representanteAncho`, `representanteAlto`; `diasVigencia` (7), `horasRenotificacion` (24; el proveedor limita a 23 h 59 min),
`typeNotification` (`TIPO_ORDEN_CADENA_SECU_CONTRATO`), `methodNotification` (`ENVIO_OTPN_CORREO_WSS`), `nickname`, `showPdf` (true),
`idContrato` (opcional).
- **Extensión sobre el acuerdo:** para firmar en dos sitios (contrato + Anexo I) se puede mandar `socioPosiciones` /
  `representantePosiciones`, arreglos de `{pagina, x, y, ancho, alto}`, que reemplazan a la posición única.
- **PDF:** parte `archivo` (un PDF cargado) **o** `datos.idContrato` (sin `archivo`): el backend genera `RPRT_CNTR_ADHS` de ese
  contrato en memoria (no se guarda). Sin ninguno de los dos: 400 `PARAMETRO_INVALIDO`.
- **«Calcular posiciones para el contrato»:** `GET` o `POST /rest/sdpr/posiciones-contrato?idContrato=&usuario=` →
  `{ "participe": [ {numPage, x, y, width, height}, … ], "representante": [ … ] }` con la página y altura reales del PDF de ese
  contrato (`participe` trae 2: bloque del contrato y Anexo I; `representante` trae 1). Equivalencia con el formulario:
  `numPage→…Pagina`, `x→…X`, `y→…Y`, `width→…Ancho`, `height→…Alto`. Las posiciones son la esquina inferior izquierda del recuadro,
  en puntos PDF con origen abajo a la izquierda (confirmar con el «Simulador de posiciones» del portal).

### 6 a 9
Un solo campo (`processId` o `pdfId`). En 7 y 8 el PDF viene en `pdfBase64` / `pdfNombre`.

## Errores de esta API
Cuerpo `{ "exito": false, "mensaje": "CODIGO: texto", "error": "CODIGO" }`.
| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `PARAMETRO_INVALIDO` | falta un campo obligatorio, JSON inválido, id con caracteres no permitidos, ruta desconocida |
| 400 | (sin código) | usuario faltante o inexistente |
| 403 | `PRUEBAS_DESHABILITADAS` | falta `saa.firma.securitydata.pruebas=true` |
| 404 | `CONTRATO_NO_ENCONTRADO` | `idContrato` / posiciones de un contrato que no existe |
| 409 | `PROVEEDOR_NO_CONFIGURADO` | faltan propiedades `saa.firma.securitydata.*` (el mensaje las nombra) |
| 409 | `FORMATO_CONTRATO_PENDIENTE`, `POSICION_FIRMA_NO_ENCONTRADA` | falta el reporte `RPRT_CNTR_ADHS` o cambió su formato |
| 502 | `PROVEEDOR_NO_DISPONIBLE`, `RESPUESTA_PROVEEDOR_INVALIDA` | no hay comunicación con Security Data o su respuesta no es interpretable |

## Verificado contra desarrollo (2026-11-10, cédula de ejemplo 0987654321)
`autenticar` 200 (token enmascarado, `expira` ≈ 15 min) · `validar-firmante` 200 con `STY-0003` (**el plan de desarrollo no permite
firmas de persona natural**: esto no es un error del SAA) · `firma-representante` (BUSCAR) 200, `result: []` ·
`detalle-tarea` con un `processId` inventado → el proveedor responde **HTTP 400** «No se encontro una tarea…» (no 404), visible en `httpStatus`.
`crear-tarea` y `cancelar-tarea` no se ejecutaron (notifican a personas reales).
