# API — Firma electrónica del contrato de adhesión

2026-11-10 · Diseño: `DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md` · DDL: `sql/376_DDL_FIRMA_ELECTRONICA_CONTRATO.sql` (va ANTES del EAR).
Base: `/SaaBE/rest/frcn/...`. **Fase 2 (integración real con Security Data, flujo «enlace al socio»).** Si faltan propiedades de
WildFly, todo contra el proveedor responde `PROVEEDOR_NO_CONFIGURADO`.

## Reglas
- El SAA manda a Security Data el PDF del contrato y los datos del partícipe (cédula, correo, celular y **código dactilar**).
  Security Data le envía **al partícipe** un enlace (y el OTP) por correo/WhatsApp; el partícipe firma desde ahí. La tarea lleva
  **solo al partícipe**: la firma del representante legal de ASOPREP la estampa Security Data y el SAA no interviene (no la busca
  ni la exige). El SAA no gestiona enlaces: el enlace del partícipe no se guarda ni se devuelve. `firmantes` trae una sola fila (el partícipe, rol `PARTICIPE`);
  su `idExterno` es el `signatoryId` del proveedor (vale 3 días).
- Al enviar el trámite queda **ENVIADO** (estado 2) y «Actualizar estado» consulta al proveedor hasta que esté firmado (no hay webhook).
- **Código dactilar:** 6 caracteres alfanuméricos (reverso de la cédula), lo digita el funcionario. **Va en el CUERPO JSON** de
  `enviar` y de `reintentar`, nunca en la URL, y no se guarda en ninguna tabla ni en el log. Cada reintento lo vuelve a pedir.
- **Posiciones de firma:** el SAA ubica solo la firma del partícipe (bloque PARTÍCIPE del contrato y «Firma PARTICIPE» del Anexo I)
  (y la del Gerente solo si hay `referenciaRepresentante`), en la página real del PDF generado.
- El PDF sale del reporte `RPRT_CNTR_ADHS` (contrato + Anexo I). `FORMATO_CONTRATO_PENDIENTE` solo si falta ese reporte en el servidor.
- Correo del firmante: el personal o, si no hay, el institucional; celular: el móvil. Se copian a `FRFR` al enviar (foto de lo enviado).
- **Un solo trámite vivo** (estado 1 o 2) por contrato. Para volver a enviar hay que anular o esperar el resultado.
- La firma no cambia el estado del contrato.
- `usuario` es obligatorio y se valida (U-2): sin usuario válido → 400 y no se hace nada.

## Estados
Trámite (`firma.estado`): 1 PENDIENTE_ENVIO (creado, el proveedor falló: reintentable) · 2 ENVIADO · 3 FIRMADO (ya hay PDF firmado) · 4 RECHAZADO · 5 VENCIDO · 6 ANULADO.
Firmante (`firmantes[].estado`): 1 PENDIENTE · 2 FIRMADO · 3 RECHAZADO.

Mapeo desde Security Data (`taskStatus` de «Actualizar estado»): `…COMPLETADO` → 3 FIRMADO (se descarga el PDF y queda como adjunto) ·
`…RECHAZADO` / `…RECHAZADO_BIOMETRIX` → 4 RECHAZADO (el motivo queda en `firma.error`, «RECHAZADO: …») · `…CADUCADO` → 5 VENCIDO ·
`…CANCELADO` → 6 ANULADO · `…PROCESO` / `…PRE_COMPLETADO` → sigue en 2 ENVIADO. Firmante: `FIRMADO` → 2 (con su fecha), `RECHAZADO` → 3, el resto 1.
Un trámite VENCIDO (5) no se reactiva: se envía uno nuevo.

Botones: **Enviar a firmar** (sin trámite vivo) · **Reintentar** (estado 1) · **Actualizar estado** (estado 2) · **Anular** (1 o 2) ·
**Descargar original** (si `idAdjuntoOriginal`) · **Descargar firmado** (si `idAdjuntoFirmado`).

## Endpoints
### POST `/enviar?idContrato=&usuario=`
Cuerpo JSON (obligatorio): `{ "codigoDactilar": "XXXXXX" }` (6 alfanuméricos; se acepta en minúsculas).
Crea el trámite, guarda el PDF (adjunto «CONTRATO DE ADHESION») con su SHA-256, copia el firmante y llama al proveedor
(validar firmante → crear tarea). Para firmar hace falta que el partícipe tenga **correo y celular** (`FIRMANTE_SIN_CONTACTO` si falta alguno).
### POST `/reintentar/{id}?usuario=`
Cuerpo JSON (obligatorio): `{ "codigoDactilar": "XXXXXX" }` (el código no se guarda: hay que digitarlo de nuevo).
Solo estado 1. Reenvía el mismo PDF (se verifica el SHA-256) y vuelve a validar al firmante.
### POST `/actualizar/{id}?usuario=`
Solo estado 2 (un trámite ya firmado responde éxito sin consultar). Si quedó firmado: descarga el PDF firmado (adjunto
«CONTRATO DE ADHESION FIRMADO») y pasa a estado 3. Si la descarga falla, sigue en 2 con el error y se reintenta al actualizar de nuevo.
### POST `/anular/{id}?motivo=&usuario=`
`motivo` obligatorio. Estado 1: se cierra directo. Estado 2: se cancela primero en el proveedor; **si el proveedor falla el trámite sigue vivo** (no se cierra a ciegas).
### GET `/contrato/{idContrato}`
Historial, el más reciente primero. Cada elemento: `{ "firma": {FRCN}, "firmantes": [ {FRFR} ] }`.
### GET `/descargar/{id}?tipo=ORIGINAL|FIRMADO`
`application/pdf` con `Content-Disposition: attachment`.
### Lectura estándar
`GET /frcn/getAll`, `GET /frcn/getId/{id}`, `GET /frfr/getAll`, `GET /frfr/getId/{id}`, `GET /frfr/firma/{idFirma}`.

## Respuestas
Las cuatro operaciones POST devuelven el mismo cuerpo:
```json
{ "exito": true, "mensaje": "Enviado a firmar", "error": null,
  "tramite": {
    "firma": { "codigo": 1, "idContrato": 55, "proveedor": "SECURITY_DATA", "idExterno": "…", "estado": 2,
               "idAdjuntoOriginal": 90, "idAdjuntoFirmado": null, "hash": "…sha256…",
               "fechaEnvio": [2026,11,10,9,0,0], "fechaFirma": null, "fechaUltimaConsulta": null, "error": null,
               "fechaRegistro": [2026,11,10,9,0,0], "usuarioRegistro": "…",
               "fechaAnulacion": null, "usuarioAnulacion": null, "motivoAnulacion": null },
    "firmantes": [ { "codigo": 1, "idFirma": 1, "orden": 1, "rol": "PARTICIPE", "identificacion": "…", "nombre": "…",
                     "correo": "…", "celular": "…", "idEntidad": 123, "estado": 1, "fechaFirma": null, "idExterno": "…" } ] } }
```
Fechas en el formato de siempre del sistema (arreglo de Jackson).

**Falla del proveedor → HTTP 502** con el mismo cuerpo: `exito: false`, `error` con el código, `mensaje` y el `tramite` tal como quedó
(con `firma.error`). El trámite NO se pierde: sigue en 1 (enviar/reintentar) o 2 (actualizar/anular).

## Errores de validación (no tocan nada)
Cuerpo `{ "exito": false, "mensaje": "CODIGO: texto", "error": "CODIGO" }`.

| HTTP | Código | Cuándo |
|---|---|---|
| 400 | `PARAMETRO_INVALIDO` | falta id, motivo o `tipo` distinto de ORIGINAL/FIRMADO |
| 400 | `CODIGO_DACTILAR_INVALIDO` | `codigoDactilar` ausente en el cuerpo o que no tiene 6 caracteres alfanuméricos |
| 409 | `POSICION_FIRMA_NO_ENCONTRADA` | no se pudo ubicar la firma en el PDF (el formato `RPRT_CNTR_ADHS` cambió) |
| 400 | (sin código) | usuario faltante o inexistente |
| 404 | `CONTRATO_NO_ENCONTRADO`, `TRAMITE_NO_ENCONTRADO` | |
| 409 | `CONTRATO_NO_VIGENTE` | contrato inactivo o terminado |
| 409 | `TRAMITE_VIVO_EXISTENTE` | ya hay un trámite en estado 1 o 2 |
| 409 | `FIRMANTE_SIN_CONTACTO` | al partícipe le falta cédula, nombre, o correo/celular (el mensaje dice qué) |
| 409 | `FORMATO_CONTRATO_PENDIENTE` | todavía no existe el reporte `RPRT_CNTR_ADHS` |
| 409 | `ESTADO_INVALIDO` | la operación no corresponde al estado del trámite |
| 409 | `TIPO_ADJUNTO_NO_CONFIGURADO` | falta (o está repetido) «CONTRATO DE ADHESION» / «… FIRMADO» en `CRD.TPDJ` (sql/376) |
| 409 | `ARCHIVO_NO_DISPONIBLE` | no hay PDF de ese tipo, no se puede leer, o su SHA-256 no coincide |

Errores del proveedor (502; el trámite NO se pierde, queda en 1 con el error; `mensaje` = «CODIGO: texto»):
- `PROVEEDOR_NO_CONFIGURADO`: faltan propiedades de WildFly `saa.firma.securitydata.url|usuario|clave|ruc`. Opcionales:
  `referenciaRepresentante` (solo si viene, el representante se agrega como firmante embebido en la posición del Gerente),
  `metodoNotificacion` (`ENVIO_OTPN_CORREO_WSS`), `diasVigencia` (7), `horasRenotificacion` (24).
- `PROVEEDOR_NO_DISPONIBLE`: no hay comunicación con Security Data. `ERROR_PROVEEDOR`: Security Data respondió un error (el mensaje trae el suyo).
  `RESPUESTA_PROVEEDOR_INVALIDA`: respuesta que no se puede interpretar.
- `FIRMANTE_REQUIERE_DATOS`: Security Data pide más datos del firmante (STY-0002; el mensaje lista campos y adjuntos). No hay paso 2 todavía.
- `FIRMANTE_RECHAZADO`: Security Data no aceptó al firmante (STY-0003, STY-0013 cédula no habilitada, STY-0014 cédula expirada);
  el mensaje trae el `errorReason` literal del proveedor. Reintentar sirve solo si el motivo es el código dactilar o un dato corregible.

## Mensajes sugeridos para el usuario
`FORMATO_CONTRATO_PENDIENTE`: «El formato del contrato aún no está disponible.» · `PROVEEDOR_NO_DISPONIBLE`: «No hay comunicación con el
servicio de firma electrónica.» · `PROVEEDOR_NO_CONFIGURADO`: «Falta configurar el servicio de firma electrónica en el servidor.» ·
`FIRMANTE_RECHAZADO` /
`FIRMANTE_REQUIERE_DATOS`: mostrar el mensaje tal cual (trae el motivo del proveedor).

## Campos nuevos de la ficha del partícipe (Anexo I del contrato) — DDL `sql/377`
No existe un doc API de la ficha; los campos nuevos viajan en los JSON de siempre (`/rest/entd`, `/rest/prtc`, `/rest/drcc`), todos opcionales:
| Entidad | Campo JSON | Valores |
|---|---|---|
| `Entidad` (ENTD) | `sexo` | `"M"`, `"F"` o null (otro valor: el mensaje de error empieza con `SEXO_INVALIDO`, por el manejo de errores de siempre del REST) |
| `Participe` (PRTC) | `modalidadLaboral` | `1` Nombramiento, `2` Contrato o null (otro valor: `MODALIDAD_LABORAL_INVALIDA`) |
| `Participe` (PRTC) | `tituloProfesional` | texto (hasta 300); el impreso usa `nivelEstudios` si está vacío |
| `Direccion` (DRCC) | `barrio` | texto (hasta 300); el impreso NO usa `referencia` como respaldo |
