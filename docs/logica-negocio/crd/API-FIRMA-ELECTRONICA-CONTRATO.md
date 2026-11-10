# API — Firma electrónica del contrato de adhesión

2026-11-10 · Diseño: `DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md` · DDL: `sql/376_DDL_FIRMA_ELECTRONICA_CONTRATO.sql` (va ANTES del EAR).
Base: `/SaaBE/rest/frcn/...`. **Fase 1:** la integración con Security Data aún no existe: todo envío/consulta/descarga/anulación
contra el proveedor responde `PROVEEDOR_NO_CONFIGURADO` (faltan propiedades de WildFly) o `PROVEEDOR_NO_DISPONIBLE`. Además
el formato del contrato todavía no existe: **enviar responde `FORMATO_CONTRATO_PENDIENTE`** hasta que esté `RPRT_CNTR_ADHS`.

## Reglas
- Firmante: **solo el partícipe** (rol `PARTICIPE`). La firma del representante legal de ASOPREP la pone Security Data en su
  plataforma; el PDF firmado que se descarga ya trae las dos firmas.
- Correo del firmante: el personal o, si no hay, el institucional; celular: el móvil. Se copian a `FRFR` al enviar (foto de lo enviado).
- **Un solo trámite vivo** (estado 1 o 2) por contrato. Para volver a enviar hay que anular o esperar el resultado.
- La firma no cambia el estado del contrato.
- `usuario` es obligatorio y se valida (U-2): sin usuario válido → 400 y no se hace nada.

## Estados
Trámite (`firma.estado`): 1 PENDIENTE_ENVIO (creado, el proveedor falló: reintentable) · 2 ENVIADO · 3 FIRMADO (ya hay PDF firmado) · 4 RECHAZADO · 5 VENCIDO · 6 ANULADO.
Firmante (`firmantes[].estado`): 1 PENDIENTE · 2 FIRMADO · 3 RECHAZADO.

Botones: **Enviar a firmar** (sin trámite vivo) · **Reintentar** (estado 1) · **Actualizar estado** (estado 2) · **Anular** (1 o 2) ·
**Descargar original** (si `idAdjuntoOriginal`) · **Descargar firmado** (si `idAdjuntoFirmado`).

## Endpoints
### POST `/enviar?idContrato=&usuario=`
Crea el trámite, guarda el PDF (adjunto «CONTRATO DE ADHESION») con su SHA-256, copia el firmante y llama al proveedor.
### POST `/reintentar/{id}?usuario=`
Solo estado 1. Reenvía el mismo PDF (se verifica el SHA-256).
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
| 400 | (sin código) | usuario faltante o inexistente |
| 404 | `CONTRATO_NO_ENCONTRADO`, `TRAMITE_NO_ENCONTRADO` | |
| 409 | `CONTRATO_NO_VIGENTE` | contrato inactivo o terminado |
| 409 | `TRAMITE_VIVO_EXISTENTE` | ya hay un trámite en estado 1 o 2 |
| 409 | `FIRMANTE_SIN_CONTACTO` | al partícipe le falta cédula, nombre, o correo/celular (el mensaje dice qué) |
| 409 | `FORMATO_CONTRATO_PENDIENTE` | todavía no existe el reporte `RPRT_CNTR_ADHS` |
| 409 | `ESTADO_INVALIDO` | la operación no corresponde al estado del trámite |
| 409 | `TIPO_ADJUNTO_NO_CONFIGURADO` | falta (o está repetido) «CONTRATO DE ADHESION» / «… FIRMADO» en `CRD.TPDJ` (sql/376) |
| 409 | `ARCHIVO_NO_DISPONIBLE` | no hay PDF de ese tipo, no se puede leer, o su SHA-256 no coincide |

Errores del proveedor (502): `PROVEEDOR_NO_CONFIGURADO` (faltan `saa.firma.securitydata.url|usuario|clave|ruc` en WildFly),
`PROVEEDOR_NO_DISPONIBLE`, `RESPUESTA_PROVEEDOR_INVALIDA`.

## Mensajes sugeridos para el usuario
`FORMATO_CONTRATO_PENDIENTE`: «El formato del contrato aún no está disponible.» · `PROVEEDOR_NO_DISPONIBLE`: «El servicio de firma
electrónica aún no está habilitado.» · `PROVEEDOR_NO_CONFIGURADO`: «Falta configurar el servicio de firma electrónica en el servidor.»
