# REFERENCIA — API de Security Data (DatAgree) para la firma del contrato de adhesión

omen-saa-1-arb · 2026-11-10. Fuente: la documentación oficial del proveedor, que el portal
`app-security-contrato/api-docs` carga desde `GET .../microservices-security-contrato/api/web/api-docs-data/{ruc}`
(v1.0, bajada el 2026-11-10). Aquí va **lo que el SAA necesita**, no la copia completa. **Sin credenciales**: van solo
en las propiedades de WildFly (`saa.firma.securitydata.*`, diseño §4).

## 0. Ambiente y verificación hecha (2026-11-10, desde la OMEN)
- URL base (desarrollo): `https://portal-bpmdesarrollo.securitydata.net.ec/microservices-security-contrato/api/dtagree`
  (la doc dice `baseUrl: /api/dtagree`).
- `POST /authenticateUser` con el usuario API del PDF del proveedor → **200**. ⚠️ **Distinto de la doc:** `result` trae
  **el token como texto** (JWT), no un objeto `{token, type}`. El cliente debe aceptar las dos formas.
- `POST /findSignatureReferences` con el token → 200, `result: []`: **no hay ninguna firma delegada** todavía (la del
  representante legal no está cargada, §3).
- `GET /planAvailability/{ruc}` → 403 «No autorizado para consultar la disponibilidad del plan» (permiso que el
  usuario API no tiene; no se usa).
- Respuesta estándar: `{status, statusCode, message, result, detail}`. Errores HTTP: 400 validación, 401 token
  inválido/vencido (se renueva y se reintenta una vez), 403 sin permiso, 404 recurso, 500.
- Token: vigencia por defecto **15 minutos** → pedir uno por operación (o cachear con margen).

## 1. Firmante: `POST /signersValidation` (paso 1) — JSON, arreglo de firmantes
| Campo | Para el socio |
|---|---|
| `tpSignature` | `SC_TP_F_AVANZADA_PN` (persona natural; la simple no tiene robustez legal) |
| `tpIdentification` | `TIPO_IDENTIFICACION_CEDULA` |
| `dni` | cédula, 10 dígitos |
| `fingerprintCode` | **código dactilar (6 caracteres, reverso de la cédula). Obligatorio** para firma no simple sin biometría externa. **El SAA no lo guarda hoy.** |
| `email` | correo del socio (obligatorio) |
| `countryCode` / `numPhone` | `+593` / celular `09XXXXXXXX` (obligatorios) |
| `regulatedSuper` | `false` |
| `reuseSignature` | `false` (el socio no tiene firma propia: se le emite una de un solo uso) |
| `biometrixType` / `biometrixParam` | solo si se usa la biometría de Security Data (`BIOMETRIX_SD` + `processUuid` de la sesión) |

Respuesta por firmante (`result[]`): `code` + `signatoryId` + `listFields` + `listFiles` + `errorReason`.
- **STY-0001** listo; el `signatoryId` **vale 3 días** (después hay que volver a validar).
- **STY-0002** faltan datos → paso 2 `POST /completeSignatoryData` (multipart: part `signatories` JSON con
  `listFields` clave/valor y part `files` con PDFs llamados `{NEMONICO}_{DNI}.pdf`). Campos posibles:
  `CP_EMI_NOMBRES`, `CP_EMI_APE_PATERNO`, `CP_EMI_APE_MATERNO`, `CP_EMI_RAZON_SOCIAL`; adjuntos posibles
  `CP_EMI_ADJU_CEDULA`, … El SAA guarda el nombre completo en `razonSocial`, **no nombres y apellidos por separado**.
- **STY-0003** error (`errorReason`); **STY-0013** cédula no habilitada en el Registro Civil, **STY-0014** cédula
  expirada (reintentar no sirve: el socio regulariza en el Registro Civil). STY-0004…0012 errores del paso 2.

## 2. Dos maneras de firmar (decisión del usuario, diseño §10)
**A. Tarea con enlace — `POST /createTask` (asíncrona).** Multipart: `files` (PDF) + `detail` (JSON):
`lisPdfDetail[{pdfName, listparticipants[{signatoryId, typeSigner, listPositions[{posX, posY, numPage, width?, height?}]}]}]`,
`notification{dateEnd 'yyyy-MM-dd HH:mm:ss', hourNoti, minNoti (≥15 min), typeNotification}`, `nickname` (asunto del
correo), `showPdf`, `methodNotification` (`ENVIO_OTPN_CORREO`, `ENVIO_OTPN_WSS`, `ENVIO_OTPN_CORREO_WSS`, o sin OTP),
`signedPdfNoti` opcional. Devuelve el `processId` (`TASK-…`).
- El socio (`TP_SIGNER_NORMAL`) recibe el enlace y el OTP por correo/WhatsApp y firma; el representante
  (`TP_SIGNER_EMBEDDED`, §3) firma **automáticamente al final**, sin notificación.
- Seguimiento: `GET /taskDetail/{processId}` (estado, firmantes con evidencias Enviado/Leído/Pre-Firmado/Firmado/
  Rechazado, `rejectionReason`; `pdfId` **solo** en `SC_ESTADO_CONTRATO_COMPLETADO`) → `POST /base64?pdfId=` → PDF
  firmado. Webhook opcional (lo registra Security Data con nuestro endpoint; el servidor está dentro de ASOPREP, así
  que por ahora consulta, no webhook).
- `GET /cancelTask/{processId}` (solo en PROCESO), `POST /reactivateTask/{processId}` (CADUCADO o RECHAZADO_BIOMETRIX,
  con `motivo`), `GET /traceabilityCertificate/{processId}` (certificado de trazabilidad en base64, solo COMPLETADO).

**B. Firma directa — `POST /singleUseSignature` (síncrona).** Multipart `files` + `detail` (arreglo
`[{pdfName, listparticipants[…]}]`, sin notificación). Devuelve `result.signedPdfs[{pdfName, base64}]` **en la misma
llamada**. ⚠️ Para un socio **sin firma propia** (firma de un solo uso) la doc exige que en el paso 1 haya
**biometría externa aprobada** (`BIOMETRIX_SD` con `processUuid` de una sesión biométrica de Security Data: prueba de
vida contra el Registro Civil). Sin esa sesión, la firma directa del socio no procede.

## 3. Firma del representante legal (embebida)
- Se carga **una sola vez** con `POST /storeSignatureWithDelegate` {dni del representante, `SC_TP_F_JURIDICA_RL`,
  `passSignature` (clave del P12), `ruc` de ASOPREP, `dateEnd` opcional (por defecto 1 año)} → `referenceCode`.
- Ese `referenceCode` va como `signatoryId` con `typeSigner: TP_SIGNER_EMBEDDED` en cada tarea.
- **La clave del P12 no pasa por el SAA**: la operación la hace ASOPREP una vez (portal o una llamada asistida) y el
  SAA solo guarda el `referenceCode` como propiedad `saa.firma.securitydata.referenciaRepresentante`. Se puede
  verificar con `POST /findSignatureReferences {dni}`. Vence: renovar antes de `expirationDate`.

## 4. Posiciones de firma
`posX`/`posY` en puntos PDF, **origen abajo a la izquierda**, `numPage` desde 1; `width`/`height` opcionales. Se sacan
de las coordenadas del `.jrxml` (A4 = 595 × 842 pt): bloque de firmas del contrato (Gerente / Partícipe) y firma del
partícipe en el Anexo I. El portal tiene un «Simulador de posiciones» para comprobarlas.
