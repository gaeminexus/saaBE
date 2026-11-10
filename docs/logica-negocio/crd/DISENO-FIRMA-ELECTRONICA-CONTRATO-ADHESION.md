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
