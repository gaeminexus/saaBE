# DISEÑO — Firma electrónica del contrato de adhesión (Security Data / DatAgree)

omen-saa-1-arb · 2026-11-10 · Pedido del usuario: «Para la firma de contratos de adhesión vamos a usar un servicio de
firma electrónica otorgado por una empresa llamada Security Data […] implementa la firma electrónica del contrato de
adhesión». 2026-11-10: «programemos todo lo que compete de nuestra parte».
Ejecutores: omen-saa-1-be (saaBE) y omen-saa-1-fe (saaFE).

## 0. Estado del proveedor (por qué va en fases)
- Security Data entregó un PDF de credenciales del ambiente de **desarrollo** (portal web, base de la API
  `https://portal-bpmdesarrollo.securitydata.net.ec/microservices-security-contrato` y su documentación
  `.../app-security-contrato/api-docs`). **Las credenciales NO van al repo, ni a docs, ni a prompts** (§6).
- El 2026-11-10 **todo `securitydata.net.ec` corta la conexión** (TLS reset) desde la OMEN, desde ASOPREP
  (186.4.211.24) y desde un celular con datos móviles: ni su página pública abre. No contestan los correos.
- No hay documentación pública de la API (búsqueda en internet: solo la página comercial de DatAgree, que dice que
  ofrece API sin publicarla).
- **Consecuencia:** no se conoce ningún endpoint, ni el formato del login, ni cómo se suben documento y firmantes.
  **No se programa la llamada adivinando.** Todo lo nuestro se construye ahora contra una interfaz de proveedor
  (§4); la implementación Security Data se completa en la fase 2 con su documentación.

## 1. Hoy (verificado en código, main 2026-11-10)
- `CRD.CNTR` (`Contrato`): contrato de adhesión del partícipe (filial, tipo, entidad, fechas, porcentajes y montos,
  estado `CNTRESTD`/`CNTRIDST`, aprobación `CNTRFCAP`/`CNTRUSAP`). Vigencias en `CRD.VGCN`.
- **No existe un impreso del contrato de adhesión** (`rep/crd/` no tiene ninguno). El usuario dice que había un
  formato preliminar: no está en el repo ni en las carpetas de la OMEN → se le pide (fase 1b).
- Adjuntos: `CRD.ADJN` (entidad, préstamo opcional, `ADJNIDRF`, tipo `CRD.TPDJ`, ruta `ADJNURLA`, nombre, mime) con
  archivos bajo `saa.upload.dir` (`FileServiceImpl`). El certificado bancario ya usa este patrón
  (`CuentaBancariaParticipeServiceImpl`, tipo resuelto por nombre en `TPDJ`).
- Datos del firmante en `CRD.ENTD`: cédula `ENTDNMID`, `razonSocial`, `correoPersonal`, `correoInstitucional`,
  `movil` (`ENTDMVLI`).
- Llamadas HTTP salientes: solo `cxc/util/SriHttpUtil` (SRI). Se usa `java.net.http.HttpClient` del JDK 21: sin
  dependencias nuevas en el `pom.xml`.

## 2. Flujo
> **Aclaración del usuario (2026-11-10), manda sobre el resto de esta sección:** «debemos mandar el pdf del contrato
> y datos del partícipe y ellos nos devuelven firmado por los dos». Security Data **firma por el partícipe** (con los
> datos que le enviamos, emitiendo su certificado) **y** con la firma del representante legal, y devuelve el PDF ya
> firmado por los dos. El SAA no gestiona firmantes ni envía enlaces: arma el paquete PDF + datos del partícipe.
> - La respuesta puede ser **inmediata** (el envío ya devuelve el PDF firmado → el trámite pasa directo a `3 FIRMADO`)
>   o **diferida** (devuelven un id y el firmado se obtiene después → `2 ENVIADO` y «Actualizar estado»). La interfaz
>   cubre las dos: `ResultadoEnvio` trae opcionalmente el PDF firmado; la fase 2 dice cuál aplica.
> - **Datos del partícipe que pide Security Data:** desconocidos hasta la documentación. Hoy se envía cédula, nombres,
>   correo y celular (foto en FRFR). Si piden más (dirección, fecha de nacimiento, foto de cédula…), la fase 2 los
>   agrega al DTO de envío y, si hace falta guardar la foto de lo enviado, a FRFR con un DDL aparte.
> - FRFR sigue siendo una fila por trámite (el partícipe) para guardar qué datos se mandaron; la firma del
>   representante no tiene fila.

1. **Generar** el PDF del contrato (reporte `RPRT_CNTR_ADHS`, fase 1b, con el formato del cliente).
2. **Enviar a firmar** (botón en el contrato): crea el trámite `CRD.FRCN` en `1 PENDIENTE_ENVIO`, guarda el PDF
   original como adjunto (TPDJ «CONTRATO DE ADHESION»), arma los firmantes (`CRD.FRFR`) y llama al proveedor →
   `2 ENVIADO` con el identificador externo del trámite. Si el proveedor falla: queda `1` con el error, reintentable.
3. **Seguimiento:** botón «Actualizar estado» (consulta al proveedor) y, si Security Data ofrece aviso (webhook),
   endpoint de recepción (fase 2). Estados por firmante y del trámite.
4. **Firmado:** se descarga el PDF firmado, se guarda como adjunto (TPDJ «CONTRATO DE ADHESION FIRMADO»), el
   trámite pasa a `3 FIRMADO` con fecha.
5. **Rechazado / vencido / anulado:** `4 RECHAZADO`, `5 VENCIDO`, `6 ANULADO` (anular = cancelar en el proveedor si
   lo permite y cerrar el trámite). Se puede volver a enviar: un trámite nuevo; **solo un trámite vivo (1 o 2) por
   contrato** (índice único parcial, §3).
- El contrato **no cambia de estado** por la firma (su aprobación sigue igual). La firma es información del trámite;
  si el usuario quiere que la aprobación exija firma, se decide aparte (D3).

## 3. Datos — DDL `crd/sql/376` (aditivo, tablas nuevas, nombres verificados libres)
**`CRD.FRCN` — trámite de firma del contrato**
| Columna | Tipo | |
|---|---|---|
| `FRCNCDGO` | NUMBER PK (`CRD.SQ_FRCNCDGO`) | |
| `CNTRCDGO` | NUMBER FK `CRD.CNTR` NOT NULL | contrato |
| `FRCNPRVD` | VARCHAR2(30) NOT NULL | proveedor (`SECURITY_DATA`) |
| `FRCNIDEX` | VARCHAR2(200) | id del trámite en el proveedor |
| `FRCNESTD` | NUMBER(2) NOT NULL | 1 PENDIENTE_ENVIO, 2 ENVIADO, 3 FIRMADO, 4 RECHAZADO, 5 VENCIDO, 6 ANULADO |
| `FRCNADOR` | NUMBER FK `CRD.ADJN` | PDF original enviado |
| `FRCNADFR` | NUMBER FK `CRD.ADJN` | PDF firmado |
| `FRCNHASH` | VARCHAR2(64) | SHA-256 del PDF enviado (integridad) |
| `FRCNFCEN` / `FRCNFCFR` / `FRCNFCUC` | TIMESTAMP | envío / firma completa / última consulta |
| `FRCNERRR` | VARCHAR2(2000) | último error del proveedor |
| `FRCNFCRG` / `FRCNUSRG` | TIMESTAMP / VARCHAR2(50) | registro |
| `FRCNFCAN` / `FRCNUSAN` / `FRCNMTAN` | TIMESTAMP / VARCHAR2(50) / VARCHAR2(500) | anulación |

Índice único parcial: `CREATE UNIQUE INDEX UX_FRCN_VIVO ON CRD.FRCN (CASE WHEN FRCNESTD IN (1,2) THEN CNTRCDGO END)`.

**`CRD.FRFR` — firmantes del trámite**
| Columna | Tipo | |
|---|---|---|
| `FRFRCDGO` | NUMBER PK (`CRD.SQ_FRFRCDGO`) | |
| `FRCNCDGO` | NUMBER FK `CRD.FRCN` NOT NULL | |
| `FRFRORDN` | NUMBER(2) NOT NULL | orden de firma |
| `FRFRROLL` | VARCHAR2(30) NOT NULL | `PARTICIPE`, `REPRESENTANTE_ASOPREP` |
| `FRFRIDNT` / `FRFRNMBR` / `FRFRCRRO` / `FRFRCLLR` | VARCHAR2 | cédula, nombre, correo, celular **copiados al enviar** (foto de lo enviado) |
| `ENTDCDGO` | NUMBER FK `CRD.ENTD` | solo si el firmante es el partícipe |
| `FRFRESTD` | NUMBER(2) NOT NULL | 1 PENDIENTE, 2 FIRMADO, 3 RECHAZADO |
| `FRFRFCFR` | TIMESTAMP | fecha de firma |
| `FRFRIDEX` | VARCHAR2(200) | id del firmante en el proveedor |

Estados como constantes planas (estado técnico de proceso, como `CRJB`/`RVSG`): no consumen `PRBR`/`PDTR`.
Semilla: dos tipos en `CRD.TPDJ` («CONTRATO DE ADHESION», «CONTRATO DE ADHESION FIRMADO») si no existen.

## 4. Backend
- `model/crd/FirmaContrato` (FRCN) y `FirmanteContrato` (FRFR) con sus cinco capas, según `ESTANDAR_MAPEO_CAPAS`.
- **Interfaz de proveedor** `ejb/crd/firma/ProveedorFirmaElectronica` (no EJB de negocio, una pieza aislada):
  `enviar(documentoPdf, nombreDocumento, List<Firmante>) → ResultadoEnvio(idExterno, idsFirmantes)`,
  `consultar(idExterno) → EstadoTramite(estado, firmantes con estado y fecha)`, `descargarFirmado(idExterno) → byte[]`,
  `anular(idExterno, motivo)`.
- `SecurityDataProveedor` implementa la interfaz. **Fase 1: cada método lanza
  `PROVEEDOR_NO_DISPONIBLE: integración con Security Data pendiente de su documentación`.** Fase 2 la completa.
  Configuración por propiedades de sistema de WildFly (nunca en el código ni en el repo):
  `saa.firma.securitydata.url`, `saa.firma.securitydata.usuario`, `saa.firma.securitydata.clave`,
  `saa.firma.securitydata.ruc`. Si falta una: error claro `PROVEEDOR_NO_CONFIGURADO`, sin valor por defecto.
  Nunca escribir la clave ni el token en el log.
- `FirmaContratoServiceImpl` (orquesta, `REQUIRES_NEW` por operación, usuario validado como el resto de crd —
  U-2):
  - `enviarAFirmar(idContrato, usuario)`: valida contrato existente y vigente (no terminado), sin trámite vivo,
    partícipe con cédula y al menos un correo o celular (si no: error `FIRMANTE_SIN_CONTACTO` con qué falta);
    genera el PDF (`RPRT_CNTR_ADHS` vía `generarReporteDesdeColeccion`/conexión según el formato, fase 1b; mientras
    no exista el reporte: error `FORMATO_CONTRATO_PENDIENTE`), guarda adjunto + hash, crea FRCN/FRFR y llama al
    proveedor. Error del proveedor → FRCN queda en 1 con `FRCNERRR`, se devuelve el error (no se pierde nada).
  - `reintentarEnvio(idFirma, usuario)` (solo estado 1), `actualizarEstado(idFirma, usuario)`,
    `anular(idFirma, motivo, usuario)`, `descargar(idFirma, tipo=ORIGINAL|FIRMADO)`.
  - Al pasar a FIRMADO: descarga, guarda adjunto firmado, marca firmantes y fecha. Idempotente: si ya está 3, no
    repite.
- REST `@Path("frcn")`: CRUD de lectura estándar + `POST frcn/enviar?idContrato&usuario`,
  `POST frcn/reintentar/{id}?usuario`, `POST frcn/actualizar/{id}?usuario`, `POST frcn/anular/{id}?motivo&usuario`,
  `GET frcn/contrato/{idContrato}` (historial de trámites con firmantes), `GET frcn/descargar/{id}?tipo`.
  `GET frfr/...` lectura estándar.
- Contrato del FE en `crd/API-FIRMA-ELECTRONICA-CONTRATO.md` (lo escribe el BE).

## 5. Frontend (contrato-edit)
- Tarjeta «Firma electrónica» en el contrato existente: estado del trámite vivo (chip), firmantes con su estado,
  fechas, último error; botones **Enviar a firmar** / **Reintentar** / **Actualizar estado** / **Anular** según
  estado; **Descargar original** y **Descargar firmado**. Historial de trámites anteriores (plegable).
- Antes de enviar: muestra los datos del firmante (cédula, nombre, correo, celular) que se van a mandar, con aviso si
  falta contacto.
- Mensajes de error del backend tal cual (`PROVEEDOR_NO_DISPONIBLE`, `FORMATO_CONTRATO_PENDIENTE`, …) traducidos a
  texto claro.

## 6. Seguridad
- Credenciales (desarrollo y producción) solo como propiedades de sistema del WildFly; el PDF de credenciales del
  proveedor no se commitea ni se copia a docs/prompts. Las de producción se cargan directo en el servidor.
- El PDF firmado y el original quedan en `saa.upload.dir` como cualquier adjunto; no se suben PDFs con datos reales a
  ningún otro lado.
- Pruebas: **solo contra el ambiente de desarrollo de Security Data** y con datos de prueba; en producción no se
  envían trámites de prueba (sin pruebas en producción).
- Red: Security Data restringe por IP; producción sale por ASOPREP (186.4.211.24). La fase 2 confirma con ellos qué
  IP autorizar para desarrollo.

## 7. Fases
- **Fase 1 (ahora):** DDL 376, capas FRCN/FRFR, interfaz + `SecurityDataProveedor` que responde
  `PROVEEDOR_NO_DISPONIBLE`, servicio y REST, pantalla. Compila y arma; sin llamadas reales.
- **Fase 1b (con el formato del cliente):** `RPRT_CNTR_ADHS.jrxml` + `.jasper`, compilado y con fill verificado y
  PDF de muestra revisado (reglas del CLAUDE.md: sin `fontName="Tahoma"`, sin `SELECT *`).
- **Fase 2 (con la documentación y el acceso de Security Data):** completar `SecurityDataProveedor`, webhook si
  existe, pruebas en su desarrollo.

## 8. Decisiones del usuario (con valor por defecto para no frenar la fase 1)
- **D1 — Firmantes (DECIDIDO 2026-11-10):** «Nosotros mandamos la firma del socio, pero en los servicios de Security
  Data ellos firman el documento con la firma del representante legal». El SAA envía **solo al partícipe** como
  firmante; la firma del representante legal de ASOPREP la pone **Security Data** en su plataforma (configurada de su
  lado), no es un firmante que el SAA arme ni gestione. El PDF firmado que se descarga trae las dos firmas. El rol
  `REPRESENTANTE_ASOPREP` del CHECK de FRFR queda sin uso (no se borra: el DDL no corrió y no estorba). La fase 2
  confirma con la documentación si el trámite expone esa firma como un paso (estado intermedio) o solo al final.
- **D2 — Canal:** por defecto el partícipe recibe el enlace por **correo** (personal, si no institucional) y
  **celular**; lo que permita DatAgree se confirma en la fase 2.
- **D3 — ¿La aprobación del contrato exige firma?** Por defecto **no** (la firma se registra aparte).
- **D4 — Alcance:** contratos nuevos y también existentes (el botón está en cualquier contrato vigente).

## 9. Fase 1b — impreso `RPRT_CNTR_ADHS` (modelo del cliente recibido 2026-11-10)
Modelo: `MODELO_CONTRATO_DE_ADHESION_V2.docx` (+ `.txt` extraído) en `crd/modelos/2026-11-10/` — **local, sin
commitear** (carpeta de archivos del cliente). Es plantilla en blanco, sin datos de socios. El texto legal se copia
**literal**; el árbitro no redacta cláusulas.

**Estructura:** título; cláusulas PRIMERA a DÉCIMO SEXTA; bloque de firmas (Gerente ASOPREP-FCPC / PARTÍCIPE con C.C.);
ANEXO I «Ficha de información del partícipe» con firma del partícipe y fecha. Un solo PDF (el contrato y el anexo).
Reporte con conexión (`<query>` con columnas explícitas, sin `SELECT *`) por `idContrato`; sin `fontName="Tahoma"`.

**Campos (verificados en el modelo Java; el ejecutor confirma joins y nulos):**
| Lugar en el modelo | Fuente |
|---|---|
| PRIMERA: nombre, cédula | `ENTD.razonSocial`, `ENTD.numeroIdentificacion` |
| PRIMERA: «Trabajador de», «Rol No» | `ENTD.tipoHidrocarburifica` (nombre) o `filial` — el ejecutor verifica cuál es la empresa; `ENTD.rolPetroComercial` |
| CUARTA: opción y porcentajes | **solo la opción del contrato** según `CNTR.tipoContrato` (jubilación / cesantía / ambas); mínimo fijo del modelo (Jub 5 %, Ces 5 %; opción 3: Jub 5 %, Ces 2 %); «Porcentaje adicional» desde `CNTRPRAI`/`CNTRPRAJ`/montos del contrato — el ejecutor mide qué columna guarda qué y lo reporta antes de fijarlo |
| DÉCIMO SEXTA: ciudad y fecha | fecha = día de generación del PDF (fecha larga en español); ciudad = parámetro `ciudad`, por defecto «Quito» |
| Firmas | Gerente: nombre y cargo **en un solo lugar del `.jrxml`** (parámetro con valor por defecto del modelo), no repetido; Partícipe: nombre y C.C. — sin líneas de firma manuscrita pintadas encima del área que use Security Data (posición a confirmar en fase 2) |
| ANEXO I datos personales | `razonSocial`, `numeroIdentificacion`, `estadoCivil`, `fechaNacimiento`, `cargasFamiliares`, `rolPetroComercial`, empresa, `tipoVivienda`, `telefono`, `movil`, `correoPersonal`, `correoInstitucional` |
| ANEXO I dirección | `CRD.Direccion` por defecto no laboral (`porDefecto`, `trabajo`): parroquia → cantón → provincia, `callePrincipal`, `calleSecundaria`, `numero`, `referencia` (barrio/sector) |
| ANEXO I datos profesionales | `Participe`: `remuneracionUnificada`, `fechaIngresoTrabajo`, `lugarTrabajo`, `unidadAdministrativa`, `cargoActual`, `nivelEstudios` (título), `ingresoAdicionalMensual`, `ingresoAdicionalActividad` |
| Sin fuente en el sistema | **Sexo** y **Nombramiento/Contrato** (modalidad laboral): se imprimen las casillas vacías; se reporta al usuario |

Casillas (sexo, estado civil, vivienda, modalidad): marcar «X» en la que corresponda; las demás vacías.
Dato faltante: se deja el espacio en blanco (no puntos suspensivos ni «null»).

**Erratas del modelo a confirmar con el cliente** (no se corrigen sin su visto bueno): «CONTRARO» (título),
«TERECERA», «ASOPREP-FCPCP» (DÉCIMO SEGUNDA), «EI PARTÍCIPE» (DÉCIMO TERCERA), «su el economista» (PRIMERA). Y dos
frases que no calzan con firma electrónica: «suscriben en unidad de acto en TRES ejemplares» (DÉCIMO SEXTA) y
«conforme se acredita con los documentos que se agregan como habilitantes».

**Verificación:** `compilar-jasper.bat` + `verificar-fill-jasper.bat` y **PDF de muestra exportado y mirado** con un
contrato de desarrollo (cuerpo completo, negritas, anexo, saltos de página). El PDF de muestra con datos reales no se
commitea ni se comparte.

## 10. Fase 2 — integración real con Security Data (2026-11-10)
Referencia de la API: `crd/REFERENCIA-API-SECURITY-DATA-DATAGREE.md` (bajada y verificada el 2026-11-10).

**Decisión del usuario: flujo A, «enlace al socio»** (`createTask`), no la firma directa (que exige biometría).
1. `authenticateUser` → token (el `result` llega como **texto**; aceptar también `{token}`), 15 min.
2. `signersValidation` del socio: `SC_TP_F_AVANZADA_PN`, cédula, **código dactilar**, correo, `+593` + celular,
   `regulatedSuper=false`, `reuseSignature=false`. STY-0001 → `signatoryId` (3 días, se guarda en `FRFRIDEX`).
   STY-0002 → error `FIRMANTE_REQUIERE_DATOS` con `listFields`/`listFiles` (sin paso 2 por ahora). STY-0003/0013/0014
   → error con `errorReason` literal.
3. `createTask`: el PDF del contrato; participantes: socio `TP_SIGNER_NORMAL` + representante `TP_SIGNER_EMBEDDED`
   (`referenceCode` de la propiedad `saa.firma.securitydata.referenciaRepresentante`), con sus posiciones;
   `typeNotification` `TIPO_ORDEN_CADENA_SECU_CONTRATO`; `methodNotification` por propiedad, por defecto
   `ENVIO_OTPN_CORREO_WSS`; `dateEnd` = ahora + días de propiedad (por defecto 7); renotificación por propiedad (por
   defecto 24 h); `nickname` «Contrato de adhesión ASOPREP-FCPC»; `showPdf=true`. `result` = `processId` →
   `FRCNIDEX`, trámite `2 ENVIADO`.
4. «Actualizar estado» → `taskDetail`: `SC_ESTADO_CONTRATO_COMPLETADO` → `base64?pdfId` → adjunto FIRMADO, trámite
   `3`; `…RECHAZADO` / `…RECHAZADO_BIOMETRIX` → `4` con `rejectionReason`; `…CADUCADO` → `5`; `…CANCELADO` → `6`;
   `…PROCESO` / `…PRE_COMPLETADO` → sigue `2`. Firmante: `signatoryStatus` y `evidence` (último estado y fecha); el
   enlace `signerToken` **no se guarda ni se muestra** (es la llave del socio).
5. Anular → `cancelTask` (solo en PROCESO). Reactivar (caducado) queda fuera por ahora: se envía un trámite nuevo.
6. Webhook: no (el servidor está dentro de ASOPREP); consulta manual y, si se pide, programada.

**Código dactilar (verificado 2026-11-10):** el usuario creía que Security Data lo pide en su parte; la prueba en
desarrollo sin el código respondió `STY-0003 «Los dígitos del código dactilar es incorrecto»`: **lo exige en la
validación que hace el SAA**. Se pide **en la ventana de «Enviar a firmar»** (6 caracteres alfanuméricos, lo digita
el funcionario desde la cédula del socio), viaja a Security Data y **no se guarda** en la base ni en el log. Si
Security Data confirma después que su enlace lo puede pedir al socio, el campo pasa a opcional.

**Firma del representante:** no hay ninguna delegada todavía (`findSignatureReferences` vacío). ASOPREP la carga una
vez con `storeSignatureWithDelegate` (la clave del P12 la digita ASOPREP, nunca el SAA) y el `referenceCode` va a la
propiedad. Sin la propiedad: `PROVEEDOR_NO_CONFIGURADO`.

**Posiciones:** del `.jrxml` (A4 595 × 842 pt, origen abajo-izquierda): socio en el bloque «PARTÍCIPE» del contrato y en
«Firma PARTICIPE» del Anexo I; representante en el bloque del Gerente. Constantes en un solo lugar.

**Propiedades nuevas:** `saa.firma.securitydata.referenciaRepresentante` (obligatoria),
`saa.firma.securitydata.metodoNotificacion`, `.diasVigencia`, `.horasRenotificacion` (opcionales con default).

**Actualización 2026-11-10 (usuario): «la firma del gerente general la sube y procesa Security Data».** El SAA no
recibe ni maneja el `referenceCode`: lo **busca** en cada envío con `findSignatureReferences {dni}` usando la cédula
del gerente (propiedad `saa.firma.securitydata.cedulaRepresentante`). Elige la firma de tipo Representante Legal en
estado ACTIVO y no vencida; si hay más de una, la de vencimiento más lejano. Sin ninguna → error
`REPRESENTANTE_SIN_FIRMA` («Security Data todavía no tiene cargada/delegada la firma del representante legal»).
`referenciaRepresentante` queda como propiedad **opcional** que, si viene, manda sobre la búsqueda. Requisito para
Security Data: la delegación tiene que quedar **a nombre del usuario API** del SAA (si no, la búsqueda no la ve).
