# DISEÑO — Pantalla de pruebas paso a paso de Security Data (DatAgree)

omen-saa-1-arb · 2026-11-10 · Pedido del usuario: «Hagamos una pantalla de pruebas del paso a paso de llamados a
security data. En parametrización de créditos, donde podamos ir viendo paso a paso el llamado, cargar un archivo
desde ahí y probar las respuestas». Ejecutores: omen-saa-1-be y omen-saa-1-fe.
Referencias: `crd/REFERENCIA-API-SECURITY-DATA-DATAGREE.md`, `crd/DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md` §10.

## 1. Idea
Una consola de diagnóstico: cada paso de la API es una tarjeta con un formulario, el botón «Ejecutar», y abajo la
**petición enviada** (método, ruta, cuerpo) y la **respuesta** (HTTP, JSON completo, tiempo). Los valores que devuelve
un paso (signatoryId, processId, pdfId, referenceCode) se copian solos a los pasos siguientes, editables.
**El navegador nunca habla con Security Data**: todo pasa por el backend, que usa las credenciales de WildFly
(`saa.firma.securitydata.*`). El token y la clave nunca llegan al navegador.

## 2. Pasos (en este orden en la pantalla)
| # | Paso | Llamada | Entrada en pantalla |
|---|---|---|---|
| 0 | Configuración | (local) | muestra URL base, usuario API (sin clave), cédula del representante y si las pruebas están habilitadas |
| 1 | Autenticar | `POST /authenticateUser` | — (muestra éxito, tipo y vigencia; el token **enmascarado**: primeros 12 caracteres) |
| 2 | Validar firmante | `POST /signersValidation` | tipo de firma, tipo de identificación, cédula, código dactilar, correo, +593, celular, reuseSignature; botón «llenar desde socio» (busca la entidad por cédula) |
| 3 | Completar datos | `POST /completeSignatoryData` | signatoryId, campos clave/valor, adjuntos PDF (`{NEMONICO}_{DNI}.pdf`) |
| 4 | Firma del representante | `POST /findSignatureReferences`, `POST /consultSDSignature` | cédula y/o nombres; tipo de firma y RUC |
| 5 | Crear tarea | `POST /createTask` (multipart) | **cargar un PDF desde la pantalla** (o «usar el contrato N»: genera `RPRT_CNTR_ADHS`), firmantes (signatoryId + tipo NORMAL/EMBEDDED) con posiciones (página, x, y, ancho, alto; botón «calcular para el contrato»), vigencia, renotificación, orden, método de notificación, nickname, showPdf |
| 6 | Detalle de tarea | `GET /taskDetail/{processId}` | processId |
| 7 | Descargar firmado | `POST /base64?pdfId=` | pdfId → botón «Descargar PDF» |
| 8 | Certificado de trazabilidad | `GET /traceabilityCertificate/{processId}` | processId → descargar PDF |
| 9 | Cancelar tarea | `GET /cancelTask/{processId}` | processId (confirmación) |

No se incluyen `storeSignatureWithDelegate` ni `revokeDelegate` (manejan la clave del P12 del representante: lo hace
Security Data) ni `customization`/`reactivateTask`.

## 3. Backend
- REST `@Path("sdpr")` (Security Data pruebas): un `POST` por paso, `sdpr/{paso}`, cuerpo JSON (multipart en 3 y 5),
  `usuario` validado (U-2). Respuesta común: `{paso, metodo, ruta, peticion, httpStatus, duracionMs, respuesta}`.
- Reusa el cliente de `SecurityDataProveedor` (token, URL, multipart), sin pasar por FRCN/FRFR: **las pruebas no
  graban trámites ni adjuntos**.
- **Enmascarar** en `peticion` y `respuesta`: password, token (12 caracteres + «…»), `fingerprintCode` («******»),
  `signerToken` (el enlace de firma del socio: se muestra solo el dominio + «…»), y todo base64 de más de 200
  caracteres (se reemplaza por «[PDF base64, N bytes]» y el archivo va aparte para descargar).
- **Interruptor:** propiedad `saa.firma.securitydata.pruebas` = `true` para habilitar. Sin ella o en `false`, todos los
  pasos responden 403 `PRUEBAS_DESHABILITADAS` — en producción queda apagado salvo que se decida prenderlo.
- Nada se escribe en el log salvo ruta, HTTP y duración.

## 4. Frontend
- Opción nueva en el menú de **Parametrización de créditos**: «Pruebas Security Data».
- Tarjetas en columna por paso (o pasos tipo stepper), cada una con su formulario, «Ejecutar», y paneles plegables
  «Petición» y «Respuesta» con JSON formateado y botón copiar; chip con HTTP y duración.
- Aviso fijo arriba: «Crear tarea envía correo y WhatsApp reales al firmante. Use solo datos autorizados».
  Confirmación antes de los pasos 5 y 9.
- El código dactilar no se guarda (como en la tarjeta de firma).

## 5. Riesgos
- Crear tarea notifica a personas reales y consume el plan: confirmación y aviso; las pruebas no se hacen con socios
  sin su autorización.
- El interruptor apagado por defecto evita que la consola quede abierta en producción por descuido.
