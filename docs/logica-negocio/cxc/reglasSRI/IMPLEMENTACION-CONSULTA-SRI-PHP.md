# Implementación — Servicios de Consulta de Documentos al SRI (Sistema PHP de Facturación)

> **Objetivo:** exponer en el sistema PHP de facturación electrónica dos servicios del SRI que hoy **no** están implementados:
> 1. `consultarEstadoAutorizacionComprobante` — estado de autorización de cualquier comprobante electrónico.
> 2. `consultarEstadoConfirmacionFacturaComercialNegociable` — si una factura fue aceptada como factura comercial negociable.
>
> Además, **sincronizar** el resultado de la consulta contra la base de datos local (recuperar documentos que quedaron colgados en estado `4 = ENVIADA` o `6 = NO AUTORIZADA`), y consumirlos desde las pantallas Angular.
>
> **Fuentes de verdad usadas para este documento:**
> - `docs/logica-negocio/cxc/reglasSRI/ConsultaDocumentosSRI.txt` — ficha técnica oficial del SRI (sección 8).
> - `src/main/java/com/saa/ws/rest/cxc/ConsultaSRIRest.java` — implementación de referencia ya funcionando en el backend Java.
> - `src/main/java/com/saa/ejb/cxc/util/SriHttpUtil.java` — manejo de SSL/timeouts contra el SRI.
> - `docs/referencias/autorizacion/gn_autorizacion*.php` — patrón de llamada SOAP **vigente** en el sistema PHP (`SoapClient` nativo).
> - `docs/referencias/logicaDocs/{fctr,lqcs,utils}.php` — convenciones de BD, respuestas, autenticación y estructura de carpetas.

---

## Índice

1. [Contexto y decisiones de diseño](#1-contexto-y-decisiones-de-diseño)
2. [Especificación de los WS del SRI](#2-especificación-de-los-ws-del-sri)
3. [Modelo de datos del sistema PHP](#3-modelo-de-datos-del-sistema-php)
4. [Cambios en base de datos (DDL)](#4-cambios-en-base-de-datos-ddl)
5. [Archivo 1 — `lib/sri_consulta.php` (librería)](#5-archivo-1--libsri_consultaphp-librería)
6. [Archivo 2 — `documents/gn_consulta_sri.php` (endpoint)](#6-archivo-2--documentsgn_consulta_sriphp-endpoint)
7. [Contrato de la API (request / response)](#7-contrato-de-la-api-request--response)
8. [Reglas de sincronización](#8-reglas-de-sincronización)
9. [Frontend Angular](#9-frontend-angular)
10. [Integración en las pantallas](#10-integración-en-las-pantallas)
11. [Errores conocidos y cómo manejarlos](#11-errores-conocidos-y-cómo-manejarlos)
12. [Pruebas y checklist de aceptación](#12-pruebas-y-checklist-de-aceptación)

---

## 1. Contexto y decisiones de diseño

### 1.1 Qué existe hoy en el sistema PHP

El sistema ya llama a **dos** web services del SRI, en `gn_autorizacion*.php` (uno por tipo de documento):

| WS | Método | Cuándo se usa |
|---|---|---|
| `RecepcionComprobantesOffline` | `validarComprobante(xml)` | Al firmar y enviar el documento (estado `3 → 4`) |
| `AutorizacionComprobantesOffline` | `autorizacionComprobante(claveAccesoComprobante)` | Inmediatamente después de `RECIBIDA` (estado `4 → 5` o `4 → 6`) |

El patrón vigente es `SoapClient` nativo de PHP con `["trace" => 1]` y `try/catch (SoapFault $e)`:

```php
$client = new SoapClient($url, [ "trace" => 1 ]);
$result = $client->autorizacionComprobante([ "claveAccesoComprobante" => $clave ]);
$estadoXml = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->estado;
```

**Lo que NO existe:** una forma de preguntar al SRI *a posteriori* por el estado de un comprobante. Si el proceso de autorización falló (timeout, SoapFault, caída del SRI), el documento queda en `estado = 4` o `estado = 6` para siempre aunque el SRI sí lo haya autorizado.

### 1.2 Decisiones tomadas

| Decisión | Elección | Justificación |
|---|---|---|
| **Cliente SOAP** | `SoapClient` nativo, idéntico a `gn_autorizacion*.php` | Consistencia con el código existente. El fallback cURL queda documentado en §11.3 solo como contingencia. |
| **Estructura** | **Un endpoint genérico** `gn_consulta_sri.php` + librería `lib/sri_consulta.php` | Los 6 `gn_autorizacion_*.php` son ~178 líneas duplicadas cada uno. Aquí el único delta entre tipos de documento es un mapa de 6 filas. |
| **Alcance** | **Consulta + sincronización** de BD y archivos | Es el caso de uso real: rescatar documentos colgados. |
| **Frontend** | Angular (service + modal + acción de grilla) | El sistema es Angular (ver `$pathAngular` en `gn_autorizacion.php`). |
| **`estado` local** | **No se sobrecarga** con valores nuevos | Se añaden columnas `estadoSRI` y `fechaConsultaSRI`. `ANULADO`/`PENDIENTE DE ANULAR` no tienen equivalente en la numeración actual (1..6) y romperían los filtros existentes de las pantallas. |
| **SQL** | `prepare` + `bindValue` en todo el código nuevo | Los scripts existentes interpolan variables en el SQL (`WHERE clave='$clave'`). El código nuevo no debe replicar eso; la clave de acceso llega por HTTP. |

### 1.3 Ubicación de los archivos

Los includes de `gn_autorizacion.php` son `../lib/config.php`, `../lib/utils.php`, `../reports/fn_rprt.php` y `gn_mail.php`; los de `logicaDocs/fctr.php` apuntan a `../documents/gn_xml_11.php`. Es decir, la carpeta que contiene los `gn_autorizacion*.php` y los `gn_xml_*.php` es hermana de `lib/` y `reports/` (en las referencias aparece como `documents/`).

```
<raíz>/
├── lib/
│   ├── config.php            (existente — define $db)
│   ├── utils.php             (existente — connect, salir, crearDirectorio, ...)
│   ├── auth.php              (existente — intentarAutenticacionJWT, validarPermisoFacturador)
│   └── sri_consulta.php      ◄── NUEVO (librería)
├── documents/
│   ├── gn_autorizacion*.php  (existente)
│   ├── gn_xml_*.php          (existente)
│   └── gn_consulta_sri.php   ◄── NUEVO (endpoint HTTP)
└── resources/{idFacturador}/{carpeta}/{f,e,a,n,c}/
                                              └── ◄── NUEVA subcarpeta `c` = consultas
```

> ⚠️ **Verificar el nombre real de la carpeta `documents/` en el servidor antes de crear el archivo** — en las referencias aparece por los includes, no por la ruta física.

---

## 2. Especificación de los WS del SRI

### 2.1 Endpoints

| Ambiente | Base URL |
|---|---|
| `1` — Pruebas / Certificación | `https://celcer.sri.gob.ec/comprobantes-electronicos-ws/` |
| `2` — Producción | `https://cel.sri.gob.ec/comprobantes-electronicos-ws/` |

| Servicio | WSDL | Método SOAP |
|---|---|---|
| Consulta de validez | `ConsultaComprobante?wsdl` | `consultarEstadoAutorizacionComprobante(claveAcceso)` |
| Factura comercial negociable | `ConsultaFactura?wsdl` | `consultarEstadoConfirmacionFacturaComercialNegociable(claveAcceso)` |
| Autorización (ya usado) | `AutorizacionComprobantesOffline?wsdl` | `autorizacionComprobante(claveAccesoComprobante)` |

Namespace de ambos servicios de consulta: `http://ec.gob.sri.ws.consultas`.

### 2.2 `consultarEstadoAutorizacionComprobante`

**Request SOAP:**

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ec="http://ec.gob.sri.ws.consultas">
  <soapenv:Header/>
  <soapenv:Body>
    <ec:consultarEstadoAutorizacionComprobante>
      <claveAcceso>0211202401050306179800120010020000000677300995216</claveAcceso>
    </ec:consultarEstadoAutorizacionComprobante>
  </soapenv:Body>
</soapenv:Envelope>
```

**Response OK** (`@WebResult(name = "EstadoAutorizacionComprobante")` → en PHP: `$result->EstadoAutorizacionComprobante`):

```xml
<EstadoAutorizacionComprobante>
  <claveAcceso>2111202405176001321000110010010000001241234567810</claveAcceso>
  <mensajes/>
  <estadoAutorizacion>AUTORIZADO</estadoAutorizacion>
  <tipoComprobante>Nota de Débito</tipoComprobante>
  <rucEmisor>1760013210001</rucEmisor>
  <fechaAutorizacion>2024-12-12T10:49:37-05:00</fechaAutorizacion>
</EstadoAutorizacionComprobante>
```

**Valores posibles de `estadoAutorizacion`:** `AUTORIZADO`, `NO AUTORIZADO`, `PENDIENTE DE ANULAR`, `ANULADO`.

**Response de rechazo** — aparece `estadoConsulta = RECHAZADA` **y no aparece `estadoAutorizacion`**:

```xml
<EstadoAutorizacionComprobante>
  <estadoConsulta>RECHAZADA</estadoConsulta>
  <claveAcceso>1510202407099313057500110010010000103591234567816</claveAcceso>
  <mensajes>
    <mensaje>
      <identificador>99</identificador>
      <mensaje>ERROR AL CONSULTAR DATOS DEL SERVICIO WEB</mensaje>
      <informacionAdicional>No es posible validar la clave de acceso ya que la fecha de emisión está fuera del rango permitido.</informacionAdicional>
      <tipo>ERROR</tipo>
    </mensaje>
  </mensajes>
</EstadoAutorizacionComprobante>
```

Dos causas distintas con el **mismo** identificador `99`, distinguibles solo por `informacionAdicional`:

| `informacionAdicional` | Significado |
|---|---|
| `No es posible validar la clave de acceso ya que la fecha de emisión está fuera del rango permitido.` | El comprobante es más antiguo que el rango que el SRI mantiene en línea. **No implica que no esté autorizado.** |
| `No existen datos para los parámetros ingresados` | La clave no existe en la base del SRI (nunca se recibió, o la clave está mal formada). |

> 🔴 **Regla crítica de sincronización:** ante `RECHAZADA` **nunca** se modifica el estado local. Un documento autorizado hace 2 años responde `RECHAZADA` por rango de fechas; marcarlo como no autorizado sería un error de datos grave.

### 2.3 `consultarEstadoConfirmacionFacturaComercialNegociable`

Solo aplica a **facturas** (`codDoc = 01`). Response OK (`$result->EstadoConfirmacionFacturaComercialNegociable`):

```xml
<EstadoConfirmacionFacturaComercialNegociable>
  <claveAcceso>1111202401099338176200110020010000003961234567815</claveAcceso>
  <mensajes/>
  <estadoConfirmacion>SI</estadoConfirmacion>
</EstadoConfirmacionFacturaComercialNegociable>
```

Solo devuelve `SI` si la factura fue **notificada y aceptada** en el portal del SRI (Comprobantes Electrónicos → Factura Comercial Negociable). En cualquier otro caso devuelve `estadoConsulta = RECHAZADA` con identificador `99` — incluyendo facturas perfectamente autorizadas que simplemente no son negociables. **`RECHAZADA` aquí NO es un error del sistema.**

### 2.4 Lo que estos WS *no* devuelven

`consultarEstadoAutorizacionComprobante` **no devuelve el XML autorizado ni el número de autorización**. Si se quiere guardar el XML autorizado en `resources/.../a/{clave}.xml` hay que llamar **además** a `autorizacionComprobante` del WS `AutorizacionComprobantesOffline` — el mismo que ya usa `gn_autorizacion.php`. Por eso la librería incluye `sriDescargarXmlAutorizado()` y el endpoint acepta `descargarXml=1`.

Para comprobantes offline, **`numeroAutorizacion` == `claveAcceso`**. El código existente ya asume esto en la rama "CLAVE ACCESO REGISTRADA" (`SET autorizacion = '$clave'`).

---

## 3. Modelo de datos del sistema PHP

### 3.1 Mapa de tipos de documento

Extraído de los seis `gn_autorizacion*.php`:

| `tipoDoc` | `codDoc` SRI | Tabla documento | Tabla path | Campo FK en tabla path | Carpeta en `resources/` | Etiqueta |
|---|:---:|---|---|---|---|---|
| `fctr` | `01` | `fctr` | `ptfc` | `factura` | `docs` | Factura |
| `lqcs` | `03` | `lqcs` | `ptlc` | `liquidacion` | `lqcs` | Liquidación de Compra |
| `ntcr` | `04` | `ntcr` | `ptnc` | `notaCredito` | `ntcr` | Nota de Crédito |
| `ntdb` | `05` | `ntdb` | `ptnd` | `notaDebito` | `ntdb` | Nota de Débito |
| `rtnc` | `07` | `rtnc` | `ptrt` | `retencion` | `rtnc` | Retención |
| `rtv2` | `07` | `rtv2` | `prt2` | `retencionv2` | `rtv2` | Retención v2 |

> ⚠️ `rtnc` y `rtv2` comparten `codDoc = 07`. **No se puede deducir el `tipoDoc` a partir de la clave de acceso.** Por eso la resolución automática busca la clave en cada tabla (§5.6), y el frontend siempre debe enviar `tipoDoc` explícito cuando lo conoce.

### 3.2 Estados de documento (columna `estado`)

| Valor | Significado | Dónde se asigna hoy |
|:---:|---|---|
| `1` | Activo / registrado | INSERT inicial |
| `2` | XML generado | `logicaDocs/fctr.php` tras `gn_xml_factura_1()` |
| `3` | FIRMADA | `gn_autorizacion*.php` línea ~31 |
| `4` | ENVIADA (recibida por el SRI) | tras `validarComprobante` = `RECIBIDA` |
| `5` | AUTORIZADA | tras `autorizacionComprobante` = `AUTORIZADO` |
| `6` | NO AUTORIZADA | tras `autorizacionComprobante` ≠ `AUTORIZADO`, o `SoapFault` |

Columna `estadoEmision`: `1` = emitida, `2` = pendiente.

### 3.3 Códigos `alterno` de la tabla de paths

| `alterno` | Carpeta | Contenido |
|:---:|:---:|---|
| `2` | (generado) | XML generado sin firmar |
| `3` | `f/` | XML firmado |
| `4` | `e/` | XML enviado al SRI |
| `5` | `a/` | XML autorizado |
| `6` | `n/` | XML no autorizado |
| `7` | `c/` | **NUEVO** — log de consultas al SRI |

### 3.4 Estructura de la clave de acceso (49 dígitos)

Derivada de `getMod11Dv()` en `lib/utils.php`:

| Posición (1-based) | Long. | Campo |
|---|:---:|---|
| 1–8 | 8 | Fecha de emisión `ddmmaaaa` |
| 9–10 | 2 | `codDoc` (tipo de comprobante) |
| 11–23 | 13 | RUC del emisor |
| 24 | 1 | **Ambiente** (`1` pruebas, `2` producción) |
| 25–27 | 3 | Establecimiento |
| 28–30 | 3 | Punto de emisión |
| 31–39 | 9 | Secuencial |
| 40–47 | 8 | Código numérico (`fcdr.codClave`) |
| 48 | 1 | Tipo de emisión (`1` = normal) |
| 49 | 1 | Dígito verificador módulo 11 |

> 💡 **El ambiente está dentro de la clave (posición 24).** Si se consulta con el ambiente equivocado, el SRI responde `RECHAZADA / No existen datos`. Por eso la librería **deriva el ambiente de la clave** cuando el parámetro no llega, y advierte cuando el parámetro contradice a la clave.

---

## 4. Cambios en base de datos (DDL)

Dos columnas nuevas por tabla de documento. No se altera ninguna columna existente.

```sql
-- MySQL / MariaDB
ALTER TABLE fctr
  ADD COLUMN estadoSRI        VARCHAR(25) NULL COMMENT 'Ultimo estado devuelto por el WS de consulta del SRI',
  ADD COLUMN fechaConsultaSRI DATETIME    NULL COMMENT 'Fecha/hora de la ultima consulta al SRI';

ALTER TABLE ntcr
  ADD COLUMN estadoSRI VARCHAR(25) NULL, ADD COLUMN fechaConsultaSRI DATETIME NULL;
ALTER TABLE ntdb
  ADD COLUMN estadoSRI VARCHAR(25) NULL, ADD COLUMN fechaConsultaSRI DATETIME NULL;
ALTER TABLE lqcs
  ADD COLUMN estadoSRI VARCHAR(25) NULL, ADD COLUMN fechaConsultaSRI DATETIME NULL;
ALTER TABLE rtnc
  ADD COLUMN estadoSRI VARCHAR(25) NULL, ADD COLUMN fechaConsultaSRI DATETIME NULL;
ALTER TABLE rtv2
  ADD COLUMN estadoSRI VARCHAR(25) NULL, ADD COLUMN fechaConsultaSRI DATETIME NULL;

-- Solo facturas: resultado de la consulta de factura comercial negociable
ALTER TABLE fctr
  ADD COLUMN negociableSRI VARCHAR(15) NULL COMMENT 'SI | RECHAZADA';

-- Índice para la consulta por clave (la sincronización busca por clave, no por id)
CREATE INDEX idx_fctr_clave ON fctr (clave);
CREATE INDEX idx_ntcr_clave ON ntcr (clave);
CREATE INDEX idx_ntdb_clave ON ntdb (clave);
CREATE INDEX idx_lqcs_clave ON lqcs (clave);
CREATE INDEX idx_rtnc_clave ON rtnc (clave);
CREATE INDEX idx_rtv2_clave ON rtv2 (clave);
```

> Si alguna de esas tablas ya tiene índice único/normal sobre `clave`, omitir el `CREATE INDEX` correspondiente (`SHOW INDEX FROM fctr;` para verificar).

**Valores posibles de `estadoSRI`:** `AUTORIZADO`, `NO AUTORIZADO`, `ANULADO`, `PENDIENTE DE ANULAR`, `RECHAZADA`, `ERROR`.

---

## 5. Archivo 1 — `lib/sri_consulta.php` (librería)

Contiene toda la lógica reutilizable. **No emite salida HTTP.** Debe poder incluirse desde el endpoint, desde un cron, o desde `gn_autorizacion*.php` si en el futuro se quiere reintentar.

```php
<?php
/**
 * lib/sri_consulta.php
 *
 * Consulta de comprobantes electrónicos al SRI (Ecuador).
 *
 *   - consultarEstadoAutorizacionComprobante            (ConsultaComprobante?wsdl)
 *   - consultarEstadoConfirmacionFacturaComercialNegociable (ConsultaFactura?wsdl)
 *   - autorizacionComprobante                           (AutorizacionComprobantesOffline?wsdl)
 *     ^ solo para recuperar el XML autorizado, que los WS de consulta NO devuelven.
 *
 * Usa SoapClient nativo, igual que gn_autorizacion*.php.
 *
 * Referencia: docs/logica-negocio/cxc/reglasSRI/ConsultaDocumentosSRI.txt (seccion 8)
 */

ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);
ini_set('default_socket_timeout', 60);
date_default_timezone_set('America/Guayaquil');

// ─────────────────────────────────────────────────────────────────────────────
// 1. Constantes
// ─────────────────────────────────────────────────────────────────────────────

define('SRI_URL_PRUEBAS',     'https://celcer.sri.gob.ec/comprobantes-electronicos-ws/');
define('SRI_URL_PRODUCCION',  'https://cel.sri.gob.ec/comprobantes-electronicos-ws/');

define('SRI_WS_CONSULTA_COMPROBANTE', 'ConsultaComprobante?wsdl');
define('SRI_WS_CONSULTA_FACTURA',     'ConsultaFactura?wsdl');
define('SRI_WS_AUTORIZACION',         'AutorizacionComprobantesOffline?wsdl');

// Estados locales (columna `estado`)
define('EST_FIRMADA',        3);
define('EST_ENVIADA',        4);
define('EST_AUTORIZADA',     5);
define('EST_NO_AUTORIZADA',  6);

// Codigos `alterno` de las tablas de paths
define('ALT_FIRMADO',       3);
define('ALT_ENVIADO',       4);
define('ALT_AUTORIZADO',    5);
define('ALT_NO_AUTORIZADO', 6);
define('ALT_CONSULTA',      7);

/**
 * Poner en true SOLO si el servidor no logra completar el handshake TLS
 * contra cel.sri.gob.ec por CA bundle desactualizado. Equivale al
 * X509ExtendedTrustManager permisivo de SriHttpUtil.java del backend.
 */
define('SRI_SSL_PERMISIVO', false);

// ─────────────────────────────────────────────────────────────────────────────
// 2. Mapa de tipos de documento
// ─────────────────────────────────────────────────────────────────────────────

function sriTiposDocumento()
{
    return [
        'fctr' => ['codDoc' => '01', 'tabla' => 'fctr', 'tablaPath' => 'ptfc',
                   'campoFK' => 'factura',     'carpeta' => 'docs', 'etiqueta' => 'Factura'],
        'lqcs' => ['codDoc' => '03', 'tabla' => 'lqcs', 'tablaPath' => 'ptlc',
                   'campoFK' => 'liquidacion', 'carpeta' => 'lqcs', 'etiqueta' => 'Liquidacion de Compra'],
        'ntcr' => ['codDoc' => '04', 'tabla' => 'ntcr', 'tablaPath' => 'ptnc',
                   'campoFK' => 'notaCredito', 'carpeta' => 'ntcr', 'etiqueta' => 'Nota de Credito'],
        'ntdb' => ['codDoc' => '05', 'tabla' => 'ntdb', 'tablaPath' => 'ptnd',
                   'campoFK' => 'notaDebito',  'carpeta' => 'ntdb', 'etiqueta' => 'Nota de Debito'],
        'rtnc' => ['codDoc' => '07', 'tabla' => 'rtnc', 'tablaPath' => 'ptrt',
                   'campoFK' => 'retencion',   'carpeta' => 'rtnc', 'etiqueta' => 'Retencion'],
        'rtv2' => ['codDoc' => '07', 'tabla' => 'rtv2', 'tablaPath' => 'prt2',
                   'campoFK' => 'retencionv2', 'carpeta' => 'rtv2', 'etiqueta' => 'Retencion v2'],
    ];
}

/** Devuelve la config de un tipoDoc o null si no esta en la whitelist. */
function sriConfigTipoDoc($tipoDoc)
{
    $mapa = sriTiposDocumento();
    $tipoDoc = strtolower(trim((string)$tipoDoc));
    return isset($mapa[$tipoDoc]) ? array_merge($mapa[$tipoDoc], ['tipoDoc' => $tipoDoc]) : null;
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Clave de acceso: validacion y desglose
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Digito verificador modulo 11 sobre los 48 primeros digitos.
 * Misma implementacion que getMod11Dv() de lib/utils.php.
 */
function sriDigitoVerificador($clave48)
{
    $digits = strrev($clave48);
    if (!ctype_digit($digits)) {
        return false;
    }
    $sum = 0;
    $factor = 2;
    for ($i = 0; $i < strlen($digits); $i++) {
        $sum += ((int) substr($digits, $i, 1)) * $factor;
        $factor = ($factor == 7) ? 2 : $factor + 1;
    }
    $dv = 11 - ($sum % 11);
    if ($dv == 10) return '1';
    if ($dv == 11) return '0';
    if ($dv < 10)  return (string) $dv;
    return 'K';
}

/**
 * @return array ['valida'=>bool, 'mensaje'=>string]
 */
function sriValidarClaveAcceso($clave)
{
    $clave = trim((string) $clave);
    if ($clave === '') {
        return ['valida' => false, 'mensaje' => 'La clave de acceso es obligatoria.'];
    }
    if (!ctype_digit($clave)) {
        return ['valida' => false, 'mensaje' => 'La clave de acceso solo puede contener digitos.'];
    }
    if (strlen($clave) != 49) {
        return ['valida' => false, 'mensaje' => 'La clave de acceso debe tener 49 digitos (recibidos: ' . strlen($clave) . ').'];
    }
    $dvEsperado = sriDigitoVerificador(substr($clave, 0, 48));
    if ($dvEsperado === false || $dvEsperado !== substr($clave, 48, 1)) {
        return ['valida' => false, 'mensaje' => 'Digito verificador incorrecto (esperado: ' . $dvEsperado . ').'];
    }
    return ['valida' => true, 'mensaje' => 'OK'];
}

/** Desglosa la clave de acceso en sus campos. Asume clave ya validada. */
function sriPartesClaveAcceso($clave)
{
    $clave = trim((string) $clave);
    if (strlen($clave) != 49) return null;
    return [
        'fechaEmision'      => substr($clave,  6, 2) . '-' . substr($clave, 2, 2) . '-' . substr($clave, 0, 2), // aa-mm-dd visual
        'fechaEmisionRaw'   => substr($clave,  0, 8),
        'codDoc'            => substr($clave,  8, 2),
        'ruc'               => substr($clave, 10, 13),
        'ambiente'          => (int) substr($clave, 23, 1),
        'establecimiento'   => substr($clave, 24, 3),
        'puntoEmision'      => substr($clave, 27, 3),
        'secuencial'        => substr($clave, 30, 9),
        'codigoNumerico'    => substr($clave, 39, 8),
        'tipoEmision'       => substr($clave, 47, 1),
        'digitoVerificador' => substr($clave, 48, 1),
    ];
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Helpers SOAP
// ─────────────────────────────────────────────────────────────────────────────

function sriUrlBase($ambiente)
{
    return ((int) $ambiente === 2) ? SRI_URL_PRODUCCION : SRI_URL_PRUEBAS;
}

function sriNombreAmbiente($ambiente)
{
    return ((int) $ambiente === 2) ? 'PRODUCCION' : 'PRUEBAS';
}

/** Opciones del SoapClient — mismas que gn_autorizacion*.php mas timeouts. */
function sriOpcionesSoap()
{
    $opciones = [
        'trace'              => 1,
        'exceptions'         => true,
        'connection_timeout' => 30,
        'cache_wsdl'         => WSDL_CACHE_MEMORY,
        'keep_alive'         => false,
    ];
    if (SRI_SSL_PERMISIVO === true) {
        $opciones['stream_context'] = stream_context_create([
            'ssl' => ['verify_peer' => false, 'verify_peer_name' => false, 'allow_self_signed' => true],
            'http' => ['timeout' => 60],
        ]);
    }
    return $opciones;
}

/**
 * Normaliza el nodo <mensajes> del SRI a un array plano.
 * SoapClient devuelve un objeto si hay 1 mensaje y un array si hay varios.
 */
function sriNormalizarMensajes($nodoMensajes)
{
    $lista = [];
    if (empty($nodoMensajes)) return $lista;

    $mensajes = isset($nodoMensajes->mensaje) ? $nodoMensajes->mensaje : $nodoMensajes;
    if (!is_array($mensajes)) $mensajes = [$mensajes];

    foreach ($mensajes as $m) {
        if (!is_object($m)) continue;
        $lista[] = [
            'identificador'        => isset($m->identificador)        ? (string) $m->identificador        : '',
            'mensaje'              => isset($m->mensaje)              ? (string) $m->mensaje              : '',
            'informacionAdicional' => isset($m->informacionAdicional) ? (string) $m->informacionAdicional : '',
            'tipo'                 => isset($m->tipo)                 ? (string) $m->tipo                 : '',
        ];
    }
    return $lista;
}

/** Concatena los mensajes del SRI en una sola linea legible para la UI. */
function sriResumirMensajes($mensajes)
{
    $partes = [];
    foreach ($mensajes as $m) {
        $t = '[' . $m['identificador'] . '] ' . $m['mensaje'];
        if ($m['informacionAdicional'] !== '') $t .= ' / ' . $m['informacionAdicional'];
        $partes[] = $t;
    }
    return implode(' | ', $partes);
}

/** Convierte 2024-12-12T10:49:37-05:00 a DATETIME de MySQL. */
function sriFechaAMysql($fechaIso)
{
    if (empty($fechaIso)) return null;
    $ts = strtotime((string) $fechaIso);
    return ($ts === false) ? null : date('Y-m-d H:i:s', $ts);
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Llamadas a los WS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * WS 1: consultarEstadoAutorizacionComprobante
 *
 * @return array [
 *   exito, ambiente, claveAcceso, estadoAutorizacion, estadoConsulta,
 *   tipoComprobante, rucEmisor, fechaAutorizacion, mensajes[], resumenMensajes,
 *   respuestaCompleta, mensaje
 * ]
 */
function sriConsultarEstadoAutorizacion($clave, $ambiente)
{
    $clave = trim((string) $clave);
    $url   = sriUrlBase($ambiente) . SRI_WS_CONSULTA_COMPROBANTE;

    $salida = [
        'exito'              => false,
        'servicio'           => 'ConsultaComprobante',
        'ambiente'           => sriNombreAmbiente($ambiente),
        'claveAcceso'        => $clave,
        'estadoAutorizacion' => '',
        'estadoConsulta'     => '',
        'tipoComprobante'    => '',
        'rucEmisor'          => '',
        'fechaAutorizacion'  => '',
        'mensajes'           => [],
        'resumenMensajes'    => '',
        'respuestaCompleta'  => '',
        'mensaje'            => '',
    ];

    error_log(">>> SRI consultarEstadoAutorizacionComprobante clave[$clave] ambiente[$ambiente] url[$url]");

    try {
        $client = new SoapClient($url, sriOpcionesSoap());
        $result = $client->consultarEstadoAutorizacionComprobante(['claveAcceso' => $clave]);

        $salida['respuestaCompleta'] = $client->__getLastResponse();

        $nodo = isset($result->EstadoAutorizacionComprobante)
              ? $result->EstadoAutorizacionComprobante
              : $result;

        if (isset($nodo->claveAcceso) && (string) $nodo->claveAcceso !== '') {
            $salida['claveAcceso'] = (string) $nodo->claveAcceso;
        }
        $salida['estadoAutorizacion'] = isset($nodo->estadoAutorizacion) ? trim((string) $nodo->estadoAutorizacion) : '';
        $salida['estadoConsulta']     = isset($nodo->estadoConsulta)     ? trim((string) $nodo->estadoConsulta)     : '';
        $salida['tipoComprobante']    = isset($nodo->tipoComprobante)    ? (string) $nodo->tipoComprobante          : '';
        $salida['rucEmisor']          = isset($nodo->rucEmisor)          ? (string) $nodo->rucEmisor                : '';
        $salida['fechaAutorizacion']  = isset($nodo->fechaAutorizacion)  ? (string) $nodo->fechaAutorizacion        : '';
        $salida['mensajes']           = sriNormalizarMensajes(isset($nodo->mensajes) ? $nodo->mensajes : null);
        $salida['resumenMensajes']    = sriResumirMensajes($salida['mensajes']);
        $salida['exito']              = true;

        if ($salida['estadoAutorizacion'] !== '') {
            $salida['mensaje'] = 'Estado en el SRI: ' . $salida['estadoAutorizacion'];
        } elseif ($salida['estadoConsulta'] !== '') {
            $salida['mensaje'] = 'Consulta ' . $salida['estadoConsulta'] . '. ' . $salida['resumenMensajes'];
        } else {
            $salida['mensaje'] = 'El SRI no devolvio estado para la clave consultada.';
        }

    } catch (SoapFault $e) {
        error_log('ERROR SoapFault consultarEstadoAutorizacionComprobante: ' . $e->getMessage());
        $salida['exito']   = false;
        $salida['mensaje'] = 'Error al llamar al WS de consulta del SRI: ' . $e->getMessage();
    } catch (Exception $e) {
        error_log('ERROR consultarEstadoAutorizacionComprobante: ' . $e->getMessage());
        $salida['exito']   = false;
        $salida['mensaje'] = 'Error inesperado al consultar al SRI: ' . $e->getMessage();
    }

    return $salida;
}

/**
 * WS 2: consultarEstadoConfirmacionFacturaComercialNegociable
 * Solo aplica a facturas (codDoc 01).
 */
function sriConsultarFacturaNegociable($clave, $ambiente)
{
    $clave = trim((string) $clave);
    $url   = sriUrlBase($ambiente) . SRI_WS_CONSULTA_FACTURA;

    $salida = [
        'exito'              => false,
        'servicio'           => 'ConsultaFactura',
        'ambiente'           => sriNombreAmbiente($ambiente),
        'claveAcceso'        => $clave,
        'estadoConfirmacion' => '',
        'estadoConsulta'     => '',
        'esNegociable'       => false,
        'mensajes'           => [],
        'resumenMensajes'    => '',
        'respuestaCompleta'  => '',
        'mensaje'            => '',
    ];

    error_log(">>> SRI consultarEstadoConfirmacionFacturaComercialNegociable clave[$clave] ambiente[$ambiente]");

    try {
        $client = new SoapClient($url, sriOpcionesSoap());
        $result = $client->consultarEstadoConfirmacionFacturaComercialNegociable(['claveAcceso' => $clave]);

        $salida['respuestaCompleta'] = $client->__getLastResponse();

        $nodo = isset($result->EstadoConfirmacionFacturaComercialNegociable)
              ? $result->EstadoConfirmacionFacturaComercialNegociable
              : $result;

        if (isset($nodo->claveAcceso) && (string) $nodo->claveAcceso !== '') {
            $salida['claveAcceso'] = (string) $nodo->claveAcceso;
        }
        $salida['estadoConfirmacion'] = isset($nodo->estadoConfirmacion) ? trim((string) $nodo->estadoConfirmacion) : '';
        $salida['estadoConsulta']     = isset($nodo->estadoConsulta)     ? trim((string) $nodo->estadoConsulta)     : '';
        $salida['mensajes']           = sriNormalizarMensajes(isset($nodo->mensajes) ? $nodo->mensajes : null);
        $salida['resumenMensajes']    = sriResumirMensajes($salida['mensajes']);
        $salida['esNegociable']       = (strtoupper($salida['estadoConfirmacion']) === 'SI');
        $salida['exito']              = true;

        $salida['mensaje'] = $salida['esNegociable']
            ? 'La factura esta aceptada como factura comercial negociable.'
            : 'La factura NO consta como factura comercial negociable. ' . $salida['resumenMensajes'];

    } catch (SoapFault $e) {
        error_log('ERROR SoapFault consultarFacturaNegociable: ' . $e->getMessage());
        $salida['mensaje'] = 'Error al llamar al WS de factura negociable del SRI: ' . $e->getMessage();
    } catch (Exception $e) {
        error_log('ERROR consultarFacturaNegociable: ' . $e->getMessage());
        $salida['mensaje'] = 'Error inesperado: ' . $e->getMessage();
    }

    return $salida;
}

/**
 * WS 3 (auxiliar): autorizacionComprobante — unico modo de recuperar el XML
 * autorizado, porque ConsultaComprobante no lo devuelve.
 * Es el mismo WS que ya usa gn_autorizacion*.php.
 */
function sriDescargarXmlAutorizado($clave, $ambiente)
{
    $clave = trim((string) $clave);
    $url   = sriUrlBase($ambiente) . SRI_WS_AUTORIZACION;

    $salida = [
        'exito' => false, 'estado' => '', 'comprobante' => '',
        'numeroAutorizacion' => '', 'fechaAutorizacion' => '',
        'mensajes' => [], 'mensaje' => '',
    ];

    error_log(">>> SRI autorizacionComprobante (descarga XML) clave[$clave] ambiente[$ambiente]");

    try {
        $client = new SoapClient($url, sriOpcionesSoap());
        $result = $client->autorizacionComprobante(['claveAccesoComprobante' => $clave]);

        if (!isset($result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion)) {
            $salida['mensaje'] = 'El SRI no devolvio autorizaciones para la clave.';
            return $salida;
        }

        // Si hay varias autorizaciones el SRI devuelve un array; tomamos la ultima AUTORIZADO.
        $autorizaciones = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion;
        if (!is_array($autorizaciones)) $autorizaciones = [$autorizaciones];

        $elegida = $autorizaciones[0];
        foreach ($autorizaciones as $a) {
            if (isset($a->estado) && strtoupper(trim((string) $a->estado)) === 'AUTORIZADO') {
                $elegida = $a;
            }
        }

        $salida['estado']             = isset($elegida->estado)             ? trim((string) $elegida->estado) : '';
        $salida['comprobante']        = isset($elegida->comprobante)        ? (string) $elegida->comprobante  : '';
        $salida['numeroAutorizacion'] = isset($elegida->numeroAutorizacion) ? (string) $elegida->numeroAutorizacion : '';
        $salida['fechaAutorizacion']  = isset($elegida->fechaAutorizacion)  ? (string) $elegida->fechaAutorizacion  : '';
        $salida['mensajes']           = sriNormalizarMensajes(isset($elegida->mensajes) ? $elegida->mensajes : null);
        $salida['exito']              = true;

    } catch (SoapFault $e) {
        error_log('ERROR SoapFault autorizacionComprobante (descarga): ' . $e->getMessage());
        $salida['mensaje'] = 'Error al descargar el XML autorizado: ' . $e->getMessage();
    } catch (Exception $e) {
        error_log('ERROR autorizacionComprobante (descarga): ' . $e->getMessage());
        $salida['mensaje'] = 'Error inesperado al descargar el XML: ' . $e->getMessage();
    }

    return $salida;
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Resolucion del documento local
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Busca el documento local a partir de la clave de acceso.
 * Si $tipoDoc viene, busca solo en esa tabla. Si no, recorre el mapa
 * (necesario porque rtnc y rtv2 comparten codDoc 07).
 *
 * @return array|null ['tipoDoc','config','registro']
 */
function sriBuscarDocumentoPorClave($dbConn, $clave, $tipoDoc = null)
{
    $candidatos = [];
    if ($tipoDoc !== null && $tipoDoc !== '') {
        $cfg = sriConfigTipoDoc($tipoDoc);
        if ($cfg === null) return null;
        $candidatos[] = $cfg;
    } else {
        $partes = sriPartesClaveAcceso($clave);
        foreach (sriTiposDocumento() as $k => $cfg) {
            // Filtra por codDoc cuando se puede; rtnc/rtv2 quedan ambos.
            if ($partes !== null && $cfg['codDoc'] !== $partes['codDoc']) continue;
            $cfg['tipoDoc'] = $k;
            $candidatos[] = $cfg;
        }
    }

    foreach ($candidatos as $cfg) {
        $tabla = $cfg['tabla']; // seguro: viene de la whitelist
        $sql = $dbConn->prepare("SELECT * FROM $tabla WHERE clave = :clave LIMIT 1");
        $sql->bindValue(':clave', $clave);
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $registro = $sql->fetch();
        if (!empty($registro)) {
            return ['tipoDoc' => $cfg['tipoDoc'], 'config' => $cfg, 'registro' => $registro];
        }
    }
    return null;
}

/** true si ya existe una fila de path con ese alterno para el documento. */
function sriExistePath($dbConn, $cfg, $idDocumento, $alterno)
{
    $tablaPath = $cfg['tablaPath'];
    $campoFK   = $cfg['campoFK'];
    $sql = $dbConn->prepare("SELECT COUNT(*) AS n FROM $tablaPath WHERE $campoFK = :id AND alterno = :alterno");
    $sql->bindValue(':id', $idDocumento, PDO::PARAM_INT);
    $sql->bindValue(':alterno', $alterno, PDO::PARAM_INT);
    $sql->execute();
    $r = $sql->fetch(PDO::FETCH_ASSOC);
    return ((int) $r['n']) > 0;
}

/** Inserta la fila de path si no existe (idempotente). */
function sriRegistrarPath($dbConn, $cfg, $idDocumento, $pathAngular, $alterno)
{
    if (sriExistePath($dbConn, $cfg, $idDocumento, $alterno)) {
        error_log("Path ya registrado alterno[$alterno] doc[$idDocumento] — no se duplica");
        return false;
    }
    $tablaPath = $cfg['tablaPath'];
    $campoFK   = $cfg['campoFK'];
    $sql = $dbConn->prepare("INSERT INTO $tablaPath (id, $campoFK, path, alterno) VALUES (0, :id, :path, :alterno)");
    $sql->bindValue(':id', $idDocumento, PDO::PARAM_INT);
    $sql->bindValue(':path', $pathAngular);
    $sql->bindValue(':alterno', $alterno, PDO::PARAM_INT);
    $sql->execute();
    return true;
}

/** Guarda el log de la consulta en resources/{facturador}/{carpeta}/c/ */
function sriGuardarLogConsulta($idFacturador, $carpeta, $clave, $contenido)
{
    $dir = '../../resources/' . $idFacturador . '/' . $carpeta . '/c';
    if (function_exists('crearDirectorio')) {
        crearDirectorio($dir);
    } elseif (!is_dir($dir)) {
        mkdir($dir, 0755, true);
    }
    $archivo = $dir . '/' . $clave . '_' . date('YmdHis') . '.txt';
    file_put_contents($archivo, $contenido);
    return 'resources/' . $idFacturador . '/' . $carpeta . '/c/' . basename($archivo);
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Sincronizacion
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Aplica el resultado de la consulta sobre la BD local y el filesystem.
 *
 * @param array $doc      resultado de sriBuscarDocumentoPorClave()
 * @param array $consulta resultado de sriConsultarEstadoAutorizacion()
 * @param bool  $descargarXml  si true y el estado es AUTORIZADO, baja el XML autorizado
 * @return array ['realizada','motivo','estadoAnterior','estadoNuevo','cambios'[],'pathXml']
 */
function sriSincronizarDocumento($dbConn, $doc, $consulta, $descargarXml = true)
{
    $sync = ['realizada' => false, 'motivo' => '', 'estadoAnterior' => null,
             'estadoNuevo' => null, 'cambios' => [], 'pathXml' => null];

    if ($doc === null) {
        $sync['motivo'] = 'El comprobante no existe en la base de datos local.';
        return $sync;
    }
    if (!$consulta['exito']) {
        $sync['motivo'] = 'No se pudo consultar al SRI; no se modifica nada.';
        return $sync;
    }

    $cfg      = $doc['config'];
    $registro = $doc['registro'];
    $tabla    = $cfg['tabla'];
    $idDoc    = (int) $registro['id'];
    $clave    = $registro['clave'];
    $estadoLocal = isset($registro['estado']) ? (int) $registro['estado'] : null;
    $sync['estadoAnterior'] = $estadoLocal;
    $sync['estadoNuevo']    = $estadoLocal;

    $estadoSRI = strtoupper(trim($consulta['estadoAutorizacion']));
    $ahora     = date('Y-m-d H:i:s');

    // Caso RECHAZADA: NO tocar estado local (puede ser solo "fuera de rango de fechas")
    if ($estadoSRI === '') {
        $rechazo = strtoupper(trim($consulta['estadoConsulta']));
        $sql = $dbConn->prepare("UPDATE $tabla SET estadoSRI = :est, fechaConsultaSRI = :fecha WHERE id = :id");
        $sql->bindValue(':est', ($rechazo !== '' ? $rechazo : 'ERROR'));
        $sql->bindValue(':fecha', $ahora);
        $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
        $sql->execute();
        $sync['motivo']  = 'El SRI respondio ' . ($rechazo !== '' ? $rechazo : 'sin estado') .
                           '. No se modifica el estado local (podria ser solo fuera de rango de fechas).';
        $sync['cambios'][] = 'estadoSRI = ' . $rechazo;
        return $sync;
    }

    switch ($estadoSRI) {

        case 'AUTORIZADO':
            $fechaAut = sriFechaAMysql($consulta['fechaAutorizacion']);
            if ($estadoLocal === EST_AUTORIZADA) {
                // Ya estaba autorizado localmente: solo se sella la consulta.
                $sql = $dbConn->prepare("UPDATE $tabla SET estadoSRI = :est, fechaConsultaSRI = :f WHERE id = :id");
                $sql->bindValue(':est', 'AUTORIZADO');
                $sql->bindValue(':f', $ahora);
                $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
                $sql->execute();
                $sync['motivo']    = 'El documento ya estaba AUTORIZADO localmente.';
                $sync['cambios'][] = 'fechaConsultaSRI';
                $sync['realizada'] = true;
            } else {
                $sql = $dbConn->prepare(
                    "UPDATE $tabla
                        SET estado = :estado, estadoEmision = 1, autorizacion = :aut,
                            fechaAutorizacion = :fecha, estadoSRI = 'AUTORIZADO', fechaConsultaSRI = :ahora
                      WHERE id = :id");
                $sql->bindValue(':estado', EST_AUTORIZADA, PDO::PARAM_INT);
                $sql->bindValue(':aut', $clave); // offline: numeroAutorizacion == claveAcceso
                $sql->bindValue(':fecha', $fechaAut);
                $sql->bindValue(':ahora', $ahora);
                $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
                $sql->execute();

                $sync['estadoNuevo'] = EST_AUTORIZADA;
                $sync['realizada']   = true;
                $sync['motivo']      = 'Documento marcado como AUTORIZADO segun el SRI.';
                $sync['cambios']     = ['estado ' . $estadoLocal . ' -> ' . EST_AUTORIZADA,
                                        'estadoEmision = 1',
                                        'autorizacion = ' . $clave,
                                        'fechaAutorizacion = ' . $fechaAut];
            }

            // Descarga del XML autorizado (ConsultaComprobante no lo devuelve)
            if ($descargarXml && !sriExistePath($dbConn, $cfg, $idDoc, ALT_AUTORIZADO)) {
                $idFacturador = isset($registro['facturador']) ? (int) $registro['facturador'] : 0;
                $ambiente     = isset($registro['ambiente']) ? (int) $registro['ambiente']
                                                             : (int) substr($clave, 23, 1);
                $descarga = sriDescargarXmlAutorizado($clave, $ambiente);
                if ($descarga['exito'] && $descarga['comprobante'] !== '') {
                    $dir = '../../resources/' . $idFacturador . '/' . $cfg['carpeta'] . '/a';
                    if (function_exists('crearDirectorio')) { crearDirectorio($dir); }
                    elseif (!is_dir($dir)) { mkdir($dir, 0755, true); }

                    $rutaFisica  = $dir . '/' . $clave . '.xml';
                    $pathAngular = 'resources/' . $idFacturador . '/' . $cfg['carpeta'] . '/a/' . $clave . '.xml';
                    try {
                        $xml = new SimpleXMLElement($descarga['comprobante']);
                        $xml->asXml($rutaFisica);
                    } catch (Exception $e) {
                        file_put_contents($rutaFisica, $descarga['comprobante']);
                    }
                    sriRegistrarPath($dbConn, $cfg, $idDoc, $pathAngular, ALT_AUTORIZADO);
                    $sync['pathXml']   = $pathAngular;
                    $sync['cambios'][] = 'XML autorizado descargado';
                }
            }
            break;

        case 'NO AUTORIZADO':
            $sql = $dbConn->prepare(
                "UPDATE $tabla
                    SET estado = :estado, estadoEmision = 2,
                        estadoSRI = 'NO AUTORIZADO', fechaConsultaSRI = :ahora
                  WHERE id = :id");
            $sql->bindValue(':estado', EST_NO_AUTORIZADA, PDO::PARAM_INT);
            $sql->bindValue(':ahora', $ahora);
            $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
            $sql->execute();
            $sync['estadoNuevo'] = EST_NO_AUTORIZADA;
            $sync['realizada']   = true;
            $sync['motivo']      = 'El SRI reporta NO AUTORIZADO.';
            $sync['cambios']     = ['estado ' . $estadoLocal . ' -> ' . EST_NO_AUTORIZADA, 'estadoEmision = 2'];
            break;

        case 'ANULADO':
        case 'PENDIENTE DE ANULAR':
            // No hay estado local equivalente: se registra sin tocar `estado`.
            $sql = $dbConn->prepare("UPDATE $tabla SET estadoSRI = :est, fechaConsultaSRI = :ahora WHERE id = :id");
            $sql->bindValue(':est', $estadoSRI);
            $sql->bindValue(':ahora', $ahora);
            $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
            $sql->execute();
            $sync['realizada'] = true;
            $sync['motivo']    = 'Estado ' . $estadoSRI . ' registrado en estadoSRI (no altera el estado local).';
            $sync['cambios'][] = 'estadoSRI = ' . $estadoSRI;
            break;

        default:
            $sql = $dbConn->prepare("UPDATE $tabla SET estadoSRI = :est, fechaConsultaSRI = :ahora WHERE id = :id");
            $sql->bindValue(':est', substr($estadoSRI, 0, 25));
            $sql->bindValue(':ahora', $ahora);
            $sql->bindValue(':id', $idDoc, PDO::PARAM_INT);
            $sql->execute();
            $sync['motivo'] = 'Estado no reconocido devuelto por el SRI: ' . $estadoSRI;
            break;
    }

    return $sync;
}

/** Persiste el resultado de la consulta de factura comercial negociable. */
function sriSincronizarNegociable($dbConn, $doc, $consulta)
{
    if ($doc === null || !$consulta['exito']) return false;
    if ($doc['config']['tabla'] !== 'fctr') return false;

    $valor = $consulta['esNegociable'] ? 'SI'
           : (trim($consulta['estadoConsulta']) !== '' ? strtoupper(trim($consulta['estadoConsulta'])) : 'NO');

    $sql = $dbConn->prepare("UPDATE fctr SET negociableSRI = :val, fechaConsultaSRI = :ahora WHERE id = :id");
    $sql->bindValue(':val', $valor);
    $sql->bindValue(':ahora', date('Y-m-d H:i:s'));
    $sql->bindValue(':id', (int) $doc['registro']['id'], PDO::PARAM_INT);
    $sql->execute();
    return true;
}
?>
```

---

## 6. Archivo 2 — `documents/gn_consulta_sri.php` (endpoint)

```php
<?php
/**
 * documents/gn_consulta_sri.php
 *
 * Endpoint generico de consulta de comprobantes electronicos al SRI.
 *
 *   GET  ?accion=estado      &clave=...&tipoDoc=fctr[&ambiente=1][&sincronizar=1][&descargarXml=1]
 *   GET  ?accion=negociable  &clave=...[&ambiente=1][&sincronizar=1]
 *   POST  accion=estadoLote  claves=["...","..."]  [tipoDoc] [ambiente] [sincronizar] [descargarXml]
 *
 * Responde siempre JSON.
 */

include "../lib/config.php";
include "../lib/utils.php";
include "../lib/auth.php";
include "../lib/sri_consulta.php";

ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);
date_default_timezone_set('America/Guayaquil');

header('Content-Type: application/json; charset=utf-8');

define('SRI_LOTE_MAXIMO', 50);   // tope de claves por lote
define('SRI_LOTE_PAUSA',  1);    // segundos entre consultas del lote

$dbConn = connect($db);

// ── Autenticacion (mismo patron que logicaDocs/fctr.php) ────────────────────
$usuarioJWT = intentarAutenticacionJWT();

function responder($httpCode, $payload)
{
    http_response_code($httpCode);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE);
}

/** Verifica que el usuario JWT pueda ver el documento encontrado. */
function autorizadoParaDocumento($usuarioJWT, $doc)
{
    if (!$usuarioJWT) return true;                       // web tradicional
    if ($doc === null) return true;                      // no hay documento local que proteger
    if (!isset($doc['registro']['facturador'])) return true;
    return validarPermisoFacturador($usuarioJWT, $doc['registro']['facturador']);
}

/**
 * Resuelve el ambiente: parametro explicito > ambiente del documento local >
 * digito 24 de la clave.
 */
function resolverAmbiente($ambienteParam, $doc, $clave)
{
    if ($ambienteParam !== null && $ambienteParam !== '') return (int) $ambienteParam;
    if ($doc !== null && isset($doc['registro']['ambiente']) && $doc['registro']['ambiente'] !== null) {
        return (int) $doc['registro']['ambiente'];
    }
    $partes = sriPartesClaveAcceso($clave);
    return ($partes !== null) ? (int) $partes['ambiente'] : 1;
}

/** Ejecuta consulta de estado (+ sincronizacion opcional) para una clave. */
function procesarEstado($dbConn, $usuarioJWT, $clave, $tipoDoc, $ambienteParam, $sincronizar, $descargarXml)
{
    $clave = trim((string) $clave);

    $val = sriValidarClaveAcceso($clave);
    if (!$val['valida']) {
        return ['http' => 400, 'body' => ['exito' => false, 'claveAcceso' => $clave, 'mensaje' => $val['mensaje']]];
    }

    $doc = sriBuscarDocumentoPorClave($dbConn, $clave, $tipoDoc);

    if (!autorizadoParaDocumento($usuarioJWT, $doc)) {
        return ['http' => 403, 'body' => ['exito' => false, 'claveAcceso' => $clave,
                                          'mensaje' => 'No tiene permisos para consultar este comprobante.']];
    }

    $ambiente = resolverAmbiente($ambienteParam, $doc, $clave);
    $consulta = sriConsultarEstadoAutorizacion($clave, $ambiente);

    // Datos del documento local (para que la pantalla compare)
    $consulta['documentoLocal'] = null;
    if ($doc !== null) {
        $consulta['documentoLocal'] = [
            'tipoDoc'       => $doc['tipoDoc'],
            'etiqueta'      => $doc['config']['etiqueta'],
            'id'            => (int) $doc['registro']['id'],
            'numero'        => isset($doc['registro']['numero']) ? $doc['registro']['numero'] : null,
            'fecha'         => isset($doc['registro']['fecha']) ? $doc['registro']['fecha'] : null,
            'estado'        => isset($doc['registro']['estado']) ? (int) $doc['registro']['estado'] : null,
            'autorizacion'  => isset($doc['registro']['autorizacion']) ? $doc['registro']['autorizacion'] : null,
            'facturador'    => isset($doc['registro']['facturador']) ? (int) $doc['registro']['facturador'] : null,
        ];
    }
    $consulta['partesClave'] = sriPartesClaveAcceso($clave);

    if ($sincronizar) {
        $consulta['sincronizacion'] = sriSincronizarDocumento($dbConn, $doc, $consulta, $descargarXml);
    }

    // Log en resources/{facturador}/{carpeta}/c/
    if ($doc !== null && isset($doc['registro']['facturador'])) {
        sriGuardarLogConsulta(
            (int) $doc['registro']['facturador'],
            $doc['config']['carpeta'],
            $clave,
            "CONSULTA ESTADO SRI\n" . print_r($consulta, true)
        );
    }

    return ['http' => ($consulta['exito'] ? 200 : 502), 'body' => $consulta];
}

// ─────────────────────────────────────────────────────────────────────────────
// GET
// ─────────────────────────────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] == 'GET') {

    $accion       = isset($_GET['accion']) ? strtolower(trim($_GET['accion'])) : 'estado';
    $clave        = isset($_GET['clave']) ? trim($_GET['clave']) : '';
    $tipoDoc      = isset($_GET['tipoDoc']) && $_GET['tipoDoc'] !== '' ? strtolower(trim($_GET['tipoDoc'])) : null;
    $ambiente     = isset($_GET['ambiente']) ? $_GET['ambiente'] : null;
    $sincronizar  = isset($_GET['sincronizar'])  && (int) $_GET['sincronizar']  === 1;
    $descargarXml = !isset($_GET['descargarXml']) || (int) $_GET['descargarXml'] === 1;

    if ($tipoDoc !== null && sriConfigTipoDoc($tipoDoc) === null) {
        responder(400, ['exito' => false, 'mensaje' => 'tipoDoc no valido. Valores: ' .
                        implode(', ', array_keys(sriTiposDocumento()))]);
        salir($dbConn);
    }

    if ($accion === 'estado') {
        $r = procesarEstado($dbConn, $usuarioJWT, $clave, $tipoDoc, $ambiente, $sincronizar, $descargarXml);
        responder($r['http'], $r['body']);
        salir($dbConn);
    }

    if ($accion === 'negociable') {
        $val = sriValidarClaveAcceso($clave);
        if (!$val['valida']) {
            responder(400, ['exito' => false, 'claveAcceso' => $clave, 'mensaje' => $val['mensaje']]);
            salir($dbConn);
        }
        $partes = sriPartesClaveAcceso($clave);
        if ($partes['codDoc'] !== '01') {
            responder(400, ['exito' => false, 'claveAcceso' => $clave,
                            'mensaje' => 'La consulta de factura comercial negociable solo aplica a facturas (codDoc 01). Recibido: ' . $partes['codDoc']]);
            salir($dbConn);
        }
        $doc = sriBuscarDocumentoPorClave($dbConn, $clave, 'fctr');
        if (!autorizadoParaDocumento($usuarioJWT, $doc)) {
            responder(403, ['exito' => false, 'mensaje' => 'No tiene permisos para consultar este comprobante.']);
            salir($dbConn);
        }
        $ambienteFinal = resolverAmbiente($ambiente, $doc, $clave);
        $consulta = sriConsultarFacturaNegociable($clave, $ambienteFinal);
        if ($sincronizar) {
            $consulta['sincronizacion'] = ['realizada' => sriSincronizarNegociable($dbConn, $doc, $consulta)];
        }
        responder($consulta['exito'] ? 200 : 502, $consulta);
        salir($dbConn);
    }

    responder(400, ['exito' => false, 'mensaje' => "accion no valida. Use 'estado' o 'negociable'."]);
    salir($dbConn);
}

// ─────────────────────────────────────────────────────────────────────────────
// POST — consulta en lote
// ─────────────────────────────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] == 'POST') {

    $accion = isset($_POST['accion']) ? strtolower(trim($_POST['accion'])) : 'estadolote';

    if ($accion !== 'estadolote') {
        responder(400, ['exito' => false, 'mensaje' => "accion no valida para POST. Use 'estadoLote'."]);
        salir($dbConn);
    }

    $claves = isset($_POST['claves']) ? json_decode($_POST['claves'], true) : null;
    if (!is_array($claves) || count($claves) === 0) {
        responder(400, ['exito' => false, 'mensaje' => 'Debe enviar el parametro claves como un arreglo JSON.']);
        salir($dbConn);
    }
    if (count($claves) > SRI_LOTE_MAXIMO) {
        responder(400, ['exito' => false, 'mensaje' => 'Maximo ' . SRI_LOTE_MAXIMO . ' claves por lote. Recibidas: ' . count($claves)]);
        salir($dbConn);
    }

    $tipoDoc      = isset($_POST['tipoDoc']) && $_POST['tipoDoc'] !== '' ? strtolower(trim($_POST['tipoDoc'])) : null;
    $ambiente     = isset($_POST['ambiente']) ? $_POST['ambiente'] : null;
    $sincronizar  = isset($_POST['sincronizar'])  && (int) $_POST['sincronizar']  === 1;
    $descargarXml = !isset($_POST['descargarXml']) || (int) $_POST['descargarXml'] === 1;

    $resultados = [];
    $resumen = ['total' => count($claves), 'autorizados' => 0, 'noAutorizados' => 0,
                'rechazados' => 0, 'errores' => 0, 'sincronizados' => 0];

    $i = 0;
    foreach ($claves as $clave) {
        if ($i > 0 && SRI_LOTE_PAUSA > 0) sleep(SRI_LOTE_PAUSA); // no saturar al SRI
        $i++;

        $r = procesarEstado($dbConn, $usuarioJWT, $clave, $tipoDoc, $ambiente, $sincronizar, $descargarXml);
        $item = $r['body'];
        // El lote no devuelve el XML crudo de cada respuesta (payload enorme)
        unset($item['respuestaCompleta']);
        $resultados[] = $item;

        if (!isset($item['exito']) || !$item['exito']) {
            $resumen['errores']++;
        } else {
            $est = strtoupper(trim(isset($item['estadoAutorizacion']) ? $item['estadoAutorizacion'] : ''));
            if     ($est === 'AUTORIZADO')     $resumen['autorizados']++;
            elseif ($est === 'NO AUTORIZADO')  $resumen['noAutorizados']++;
            elseif ($est === '')               $resumen['rechazados']++;
            if (isset($item['sincronizacion']) && $item['sincronizacion']['realizada']) $resumen['sincronizados']++;
        }
    }

    responder(200, ['exito' => true, 'resumen' => $resumen, 'resultados' => $resultados]);
    salir($dbConn);
}

responder(405, ['exito' => false, 'mensaje' => 'Metodo no permitido.']);
salir($dbConn);
?>
```

---

## 7. Contrato de la API (request / response)

### 7.1 `GET ?accion=estado`

| Parámetro | Tipo | Obligatorio | Default | Descripción |
|---|---|:---:|---|---|
| `accion` | string | No | `estado` | `estado` \| `negociable` |
| `clave` | string(49) | **Sí** | — | Clave de acceso |
| `tipoDoc` | enum | No | auto | `fctr`\|`lqcs`\|`ntcr`\|`ntdb`\|`rtnc`\|`rtv2`. Obligatorio en la práctica para retenciones (`rtnc`/`rtv2` comparten `codDoc`) |
| `ambiente` | `1`\|`2` | No | del documento local, o dígito 24 de la clave | Ambiente del SRI |
| `sincronizar` | `0`\|`1` | No | `0` | Si `1`, actualiza BD y archivos |
| `descargarXml` | `0`\|`1` | No | `1` | Solo aplica con `sincronizar=1` y estado `AUTORIZADO` |

**Ejemplo:**

```
GET /documents/gn_consulta_sri.php?accion=estado&clave=2111202405176001321000110010010000001241234567810&tipoDoc=fctr&sincronizar=1
```

**200 — Autorizado y sincronizado:**

```json
{
  "exito": true,
  "servicio": "ConsultaComprobante",
  "ambiente": "PRODUCCION",
  "claveAcceso": "2111202405176001321000110010010000001241234567810",
  "estadoAutorizacion": "AUTORIZADO",
  "estadoConsulta": "",
  "tipoComprobante": "Factura",
  "rucEmisor": "1760013210001",
  "fechaAutorizacion": "2024-12-12T10:49:37-05:00",
  "mensajes": [],
  "resumenMensajes": "",
  "respuestaCompleta": "<soap:Envelope ...>",
  "mensaje": "Estado en el SRI: AUTORIZADO",
  "documentoLocal": {
    "tipoDoc": "fctr", "etiqueta": "Factura", "id": 1234,
    "numero": "001-002-000000124", "fecha": "2024-11-21 09:15:00",
    "estado": 4, "autorizacion": null, "facturador": 7
  },
  "partesClave": {
    "fechaEmisionRaw": "21112024", "codDoc": "01", "ruc": "1760013210001",
    "ambiente": 2, "establecimiento": "001", "puntoEmision": "001",
    "secuencial": "000000124", "codigoNumerico": "12345678",
    "tipoEmision": "1", "digitoVerificador": "0"
  },
  "sincronizacion": {
    "realizada": true,
    "motivo": "Documento marcado como AUTORIZADO segun el SRI.",
    "estadoAnterior": 4,
    "estadoNuevo": 5,
    "cambios": [
      "estado 4 -> 5", "estadoEmision = 1",
      "autorizacion = 2111202405176001321000110010010000001241234567810",
      "fechaAutorizacion = 2024-12-12 10:49:37",
      "XML autorizado descargado"
    ],
    "pathXml": "resources/7/docs/a/2111202405176001321000110010010000001241234567810.xml"
  }
}
```

**200 — Rechazada por rango de fechas (no se toca el estado local):**

```json
{
  "exito": true,
  "estadoAutorizacion": "",
  "estadoConsulta": "RECHAZADA",
  "mensajes": [{
    "identificador": "99",
    "mensaje": "ERROR AL CONSULTAR DATOS DEL SERVICIO WEB",
    "informacionAdicional": "No es posible validar la clave de acceso ya que la fecha de emisión está fuera del rango permitido.",
    "tipo": "ERROR"
  }],
  "mensaje": "Consulta RECHAZADA. [99] ERROR AL CONSULTAR DATOS DEL SERVICIO WEB / No es posible validar la clave de acceso ya que la fecha de emisión está fuera del rango permitido.",
  "sincronizacion": {
    "realizada": false,
    "motivo": "El SRI respondio RECHAZADA. No se modifica el estado local (podria ser solo fuera de rango de fechas).",
    "estadoAnterior": 5, "estadoNuevo": 5,
    "cambios": ["estadoSRI = RECHAZADA"], "pathXml": null
  }
}
```

### 7.2 `GET ?accion=negociable`

```
GET /documents/gn_consulta_sri.php?accion=negociable&clave=1111202401099338176200110020010000003961234567815
```

```json
{
  "exito": true,
  "servicio": "ConsultaFactura",
  "ambiente": "PRUEBAS",
  "claveAcceso": "1111202401099338176200110020010000003961234567815",
  "estadoConfirmacion": "SI",
  "estadoConsulta": "",
  "esNegociable": true,
  "mensajes": [],
  "mensaje": "La factura esta aceptada como factura comercial negociable."
}
```

### 7.3 `POST accion=estadoLote`

```
POST /documents/gn_consulta_sri.php
Content-Type: application/x-www-form-urlencoded

accion=estadoLote&tipoDoc=fctr&sincronizar=1&claves=["2111...810","1510...816"]
```

```json
{
  "exito": true,
  "resumen": { "total": 2, "autorizados": 1, "noAutorizados": 0,
               "rechazados": 1, "errores": 0, "sincronizados": 1 },
  "resultados": [ { "...": "un objeto por clave, sin respuestaCompleta" } ]
}
```

### 7.4 Códigos HTTP

| Código | Cuándo |
|:---:|---|
| `200` | Consulta ejecutada (aunque el SRI responda `RECHAZADA` — eso es una respuesta válida) |
| `400` | Clave ausente/mal formada/DV inválido, `tipoDoc` fuera de la whitelist, `accion` inválida, lote > 50 |
| `403` | El JWT no tiene permiso sobre el facturador dueño del documento |
| `405` | Método HTTP no soportado |
| `502` | `SoapFault` / el SRI no respondió (`exito: false`) |

---

## 8. Reglas de sincronización

| Respuesta del SRI | `estado` local | Acción sobre la BD | Archivos |
|---|---|---|---|
| `AUTORIZADO` | ≠ 5 | `estado=5`, `estadoEmision=1`, `autorizacion=clave`, `fechaAutorizacion`, `estadoSRI`, `fechaConsultaSRI` | Descarga XML autorizado → `a/{clave}.xml` + fila `alterno=5` (si no existe) |
| `AUTORIZADO` | = 5 | Solo `estadoSRI` + `fechaConsultaSRI` | Ninguno |
| `NO AUTORIZADO` | cualquiera | `estado=6`, `estadoEmision=2`, `estadoSRI`, `fechaConsultaSRI` | Ninguno |
| `ANULADO` | cualquiera | Solo `estadoSRI='ANULADO'` + `fechaConsultaSRI`. **No toca `estado`** | Ninguno |
| `PENDIENTE DE ANULAR` | cualquiera | Solo `estadoSRI` + `fechaConsultaSRI` | Ninguno |
| `estadoConsulta=RECHAZADA` | cualquiera | Solo `estadoSRI='RECHAZADA'` + `fechaConsultaSRI`. **No toca `estado`** | Ninguno |
| `SoapFault` / error | cualquiera | **Nada** | Ninguno |

**Invariantes que el código debe respetar:**

1. Un documento **nunca** pasa de `estado = 5` a otro estado por efecto de una consulta.
2. `RECHAZADA` **nunca** degrada el estado local (§2.2).
3. La inserción en la tabla de paths es **idempotente** (`sriExistePath()` antes de `INSERT`) — a diferencia de `gn_autorizacion*.php`, que puede duplicar filas si se reintenta.
4. Todo `UPDATE` usa `WHERE id = :id`, nunca `WHERE clave = '$clave'` interpolada.
5. Los nombres de tabla salen del mapa whitelist, nunca directo del request.

---

## 9. Frontend Angular

### 9.1 Modelos — `src/app/models/sri-consulta.model.ts`

```typescript
export interface MensajeSRI {
  identificador: string;
  mensaje: string;
  informacionAdicional: string;
  tipo: string;
}

export interface DocumentoLocalSRI {
  tipoDoc: string;
  etiqueta: string;
  id: number;
  numero: string | null;
  fecha: string | null;
  estado: number | null;
  autorizacion: string | null;
  facturador: number | null;
}

export interface SincronizacionSRI {
  realizada: boolean;
  motivo: string;
  estadoAnterior: number | null;
  estadoNuevo: number | null;
  cambios: string[];
  pathXml: string | null;
}

export interface RespuestaEstadoSRI {
  exito: boolean;
  servicio: string;
  ambiente: 'PRUEBAS' | 'PRODUCCION';
  claveAcceso: string;
  estadoAutorizacion: string;   // AUTORIZADO | NO AUTORIZADO | ANULADO | PENDIENTE DE ANULAR | ''
  estadoConsulta: string;       // RECHAZADA | ''
  tipoComprobante: string;
  rucEmisor: string;
  fechaAutorizacion: string;
  mensajes: MensajeSRI[];
  resumenMensajes: string;
  respuestaCompleta?: string;
  mensaje: string;
  documentoLocal?: DocumentoLocalSRI | null;
  partesClave?: any;
  sincronizacion?: SincronizacionSRI;
}

export interface RespuestaNegociableSRI {
  exito: boolean;
  ambiente: string;
  claveAcceso: string;
  estadoConfirmacion: string;   // SI | ''
  estadoConsulta: string;       // RECHAZADA | ''
  esNegociable: boolean;
  mensajes: MensajeSRI[];
  mensaje: string;
}

export interface ResumenLoteSRI {
  total: number;
  autorizados: number;
  noAutorizados: number;
  rechazados: number;
  errores: number;
  sincronizados: number;
}

export interface RespuestaLoteSRI {
  exito: boolean;
  resumen: ResumenLoteSRI;
  resultados: RespuestaEstadoSRI[];
}

export type TipoDocSRI = 'fctr' | 'lqcs' | 'ntcr' | 'ntdb' | 'rtnc' | 'rtv2';
```

### 9.2 Servicio — `src/app/services/sri-consulta.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  RespuestaEstadoSRI, RespuestaNegociableSRI, RespuestaLoteSRI, TipoDocSRI
} from '../models/sri-consulta.model';

@Injectable({ providedIn: 'root' })
export class SriConsultaService {

  // Ajustar al nombre real de la carpeta donde viven los gn_*.php
  private readonly url = `${environment.apiUrl}/documents/gn_consulta_sri.php`;

  constructor(private http: HttpClient) {}

  /** Consulta el estado de autorizacion de un comprobante. */
  consultarEstado(
    clave: string,
    tipoDoc?: TipoDocSRI,
    opciones: { ambiente?: number; sincronizar?: boolean; descargarXml?: boolean } = {}
  ): Observable<RespuestaEstadoSRI> {
    let params = new HttpParams()
      .set('accion', 'estado')
      .set('clave', clave);

    if (tipoDoc)                        params = params.set('tipoDoc', tipoDoc);
    if (opciones.ambiente != null)      params = params.set('ambiente', String(opciones.ambiente));
    if (opciones.sincronizar)           params = params.set('sincronizar', '1');
    if (opciones.descargarXml === false) params = params.set('descargarXml', '0');

    return this.http.get<RespuestaEstadoSRI>(this.url, { params });
  }

  /** Consulta si una factura fue aceptada como factura comercial negociable. */
  consultarNegociable(
    clave: string,
    opciones: { ambiente?: number; sincronizar?: boolean } = {}
  ): Observable<RespuestaNegociableSRI> {
    let params = new HttpParams()
      .set('accion', 'negociable')
      .set('clave', clave);

    if (opciones.ambiente != null) params = params.set('ambiente', String(opciones.ambiente));
    if (opciones.sincronizar)      params = params.set('sincronizar', '1');

    return this.http.get<RespuestaNegociableSRI>(this.url, { params });
  }

  /** Consulta en lote (maximo 50 claves; el backend pausa 1s entre cada una). */
  consultarLote(
    claves: string[],
    tipoDoc?: TipoDocSRI,
    opciones: { ambiente?: number; sincronizar?: boolean; descargarXml?: boolean } = {}
  ): Observable<RespuestaLoteSRI> {
    const body = new FormData();
    body.append('accion', 'estadoLote');
    body.append('claves', JSON.stringify(claves));
    if (tipoDoc)                         body.append('tipoDoc', tipoDoc);
    if (opciones.ambiente != null)       body.append('ambiente', String(opciones.ambiente));
    if (opciones.sincronizar)            body.append('sincronizar', '1');
    if (opciones.descargarXml === false) body.append('descargarXml', '0');

    return this.http.post<RespuestaLoteSRI>(this.url, body);
  }

  // ── Helpers de presentacion ───────────────────────────────────────────────

  /** Clase CSS / severidad para el badge de estado. */
  severidadEstado(r: RespuestaEstadoSRI): 'success' | 'danger' | 'warning' | 'info' {
    const e = (r.estadoAutorizacion || '').toUpperCase();
    if (e === 'AUTORIZADO')          return 'success';
    if (e === 'NO AUTORIZADO')       return 'danger';
    if (e === 'ANULADO')             return 'danger';
    if (e === 'PENDIENTE DE ANULAR') return 'warning';
    return 'info';   // RECHAZADA o sin estado
  }

  /** Texto para mostrar en la UI. */
  etiquetaEstado(r: RespuestaEstadoSRI): string {
    if (r.estadoAutorizacion) return r.estadoAutorizacion;
    if (r.estadoConsulta)     return r.estadoConsulta;
    return 'SIN RESPUESTA';
  }

  /**
   * Distingue "fuera de rango de fechas" de "no existe en el SRI".
   * Ambos llegan con identificador 99 (ver ConsultaDocumentosSRI.txt).
   */
  esFueraDeRango(r: RespuestaEstadoSRI): boolean {
    return (r.mensajes || []).some(m =>
      (m.informacionAdicional || '').toLowerCase().includes('fuera del rango permitido'));
  }

  esInexistenteEnSRI(r: RespuestaEstadoSRI): boolean {
    return (r.mensajes || []).some(m =>
      (m.informacionAdicional || '').toLowerCase().includes('no existen datos'));
  }
}
```

### 9.3 Modal de consulta — `consulta-sri-dialog.component.ts`

Con Angular Material (`MatDialog`); si el proyecto usa PrimeNG o ng-bootstrap, adaptar únicamente el wrapper.

```typescript
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SriConsultaService } from '../../services/sri-consulta.service';
import { RespuestaEstadoSRI, RespuestaNegociableSRI, TipoDocSRI } from '../../models/sri-consulta.model';

export interface DatosConsultaSRI {
  clave: string;
  tipoDoc: TipoDocSRI;
  ambiente?: number;
  permiteSincronizar?: boolean;   // segun rol del usuario
}

@Component({
  selector: 'app-consulta-sri-dialog',
  templateUrl: './consulta-sri-dialog.component.html',
})
export class ConsultaSriDialogComponent {

  cargando = false;
  sincronizando = false;
  resultado: RespuestaEstadoSRI | null = null;
  negociable: RespuestaNegociableSRI | null = null;
  error = '';
  hubocambios = false;   // el padre recarga la grilla si es true

  constructor(
    public dialogRef: MatDialogRef<ConsultaSriDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DatosConsultaSRI,
    private sri: SriConsultaService
  ) {
    this.consultar();
  }

  consultar(sincronizar = false): void {
    this.cargando = true;
    this.error = '';
    this.sincronizando = sincronizar;

    this.sri.consultarEstado(this.data.clave, this.data.tipoDoc, {
      ambiente: this.data.ambiente,
      sincronizar
    }).subscribe({
      next: r => {
        this.resultado = r;
        this.cargando = false;
        this.sincronizando = false;
        if (r.sincronizacion?.realizada) this.hubocambios = true;
      },
      error: err => {
        this.error = err?.error?.mensaje || 'No se pudo consultar el estado en el SRI.';
        this.cargando = false;
        this.sincronizando = false;
      }
    });
  }

  sincronizar(): void { this.consultar(true); }

  consultarNegociable(): void {
    this.cargando = true;
    this.sri.consultarNegociable(this.data.clave, { sincronizar: true }).subscribe({
      next: r => { this.negociable = r; this.cargando = false; },
      error: err => {
        this.error = err?.error?.mensaje || 'No se pudo consultar la factura negociable.';
        this.cargando = false;
      }
    });
  }

  get severidad(): string { return this.resultado ? this.sri.severidadEstado(this.resultado) : 'info'; }
  get etiqueta(): string  { return this.resultado ? this.sri.etiquetaEstado(this.resultado)  : ''; }
  get fueraDeRango(): boolean { return !!this.resultado && this.sri.esFueraDeRango(this.resultado); }
  get inexistente(): boolean  { return !!this.resultado && this.sri.esInexistenteEnSRI(this.resultado); }

  /** Solo tiene sentido sincronizar si el estado local difiere del del SRI. */
  get puedeSincronizar(): boolean {
    if (!this.resultado || !this.data.permiteSincronizar) return false;
    if (!this.resultado.estadoAutorizacion) return false;          // RECHAZADA
    const local = this.resultado.documentoLocal?.estado ?? null;
    if (local === null) return false;
    if (this.resultado.estadoAutorizacion === 'AUTORIZADO')     return local !== 5;
    if (this.resultado.estadoAutorizacion === 'NO AUTORIZADO')  return local !== 6;
    return true;
  }

  cerrar(): void { this.dialogRef.close(this.hubocambios); }
}
```

```html
<!-- consulta-sri-dialog.component.html -->
<h2 mat-dialog-title>Consulta de comprobante en el SRI</h2>

<mat-dialog-content>

  <p class="clave"><strong>Clave de acceso:</strong> <code>{{ data.clave }}</code></p>

  <div *ngIf="cargando" class="cargando">
    <mat-spinner diameter="32"></mat-spinner>
    <span>{{ sincronizando ? 'Consultando y sincronizando…' : 'Consultando al SRI…' }}</span>
  </div>

  <div *ngIf="error" class="alerta alerta-danger">{{ error }}</div>

  <ng-container *ngIf="resultado && !cargando">

    <div class="badge" [ngClass]="'badge-' + severidad">{{ etiqueta }}</div>
    <p class="ambiente">Ambiente consultado: <strong>{{ resultado.ambiente }}</strong></p>

    <table class="tabla-detalle" *ngIf="resultado.estadoAutorizacion">
      <tr><th>Tipo de comprobante</th><td>{{ resultado.tipoComprobante || '—' }}</td></tr>
      <tr><th>RUC emisor</th><td>{{ resultado.rucEmisor || '—' }}</td></tr>
      <tr><th>Fecha de autorización</th><td>{{ resultado.fechaAutorizacion || '—' }}</td></tr>
    </table>

    <!-- Comparación local vs SRI -->
    <div class="comparacion" *ngIf="resultado.documentoLocal">
      <table class="tabla-detalle">
        <tr><th>Documento</th>
            <td>{{ resultado.documentoLocal.etiqueta }} {{ resultado.documentoLocal.numero }}</td></tr>
        <tr><th>Estado local</th><td>{{ resultado.documentoLocal.estado }}</td></tr>
      </table>
    </div>

    <!-- Aclaraciones para los dos casos de identificador 99 -->
    <div class="alerta alerta-info" *ngIf="fueraDeRango">
      El SRI no puede validar esta clave porque la fecha de emisión está fuera del rango que
      mantiene en línea. <strong>Esto no significa que el comprobante no esté autorizado.</strong>
      El estado local no fue modificado.
    </div>
    <div class="alerta alerta-warning" *ngIf="inexistente">
      El SRI no tiene registro de esta clave de acceso. Verifique que el comprobante haya sido
      enviado y que el ambiente sea el correcto.
    </div>

    <div class="mensajes" *ngIf="resultado.mensajes?.length">
      <h4>Mensajes del SRI</h4>
      <ul>
        <li *ngFor="let m of resultado.mensajes">
          <strong>[{{ m.identificador }}]</strong> {{ m.mensaje }}
          <em *ngIf="m.informacionAdicional"> — {{ m.informacionAdicional }}</em>
        </li>
      </ul>
    </div>

    <div class="sincronizacion alerta"
         [ngClass]="resultado.sincronizacion?.realizada ? 'alerta-success' : 'alerta-info'"
         *ngIf="resultado.sincronizacion">
      <strong>Sincronización:</strong> {{ resultado.sincronizacion.motivo }}
      <ul *ngIf="resultado.sincronizacion.cambios?.length">
        <li *ngFor="let c of resultado.sincronizacion.cambios">{{ c }}</li>
      </ul>
    </div>

    <!-- Factura comercial negociable: solo facturas -->
    <div class="negociable" *ngIf="data.tipoDoc === 'fctr'">
      <button mat-stroked-button (click)="consultarNegociable()" *ngIf="!negociable">
        Consultar si es factura comercial negociable
      </button>
      <div class="alerta" *ngIf="negociable"
           [ngClass]="negociable.esNegociable ? 'alerta-success' : 'alerta-info'">
        {{ negociable.mensaje }}
      </div>
    </div>

  </ng-container>

</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="consultar()" [disabled]="cargando">Volver a consultar</button>
  <button mat-raised-button color="primary" (click)="sincronizar()"
          [disabled]="cargando || !puedeSincronizar">
    Sincronizar con el SRI
  </button>
  <button mat-button (click)="cerrar()">Cerrar</button>
</mat-dialog-actions>
```

---

## 10. Integración en las pantallas

### 10.1 Botón por fila en las grillas de documentos

Aplica a las 6 pantallas: **Facturas, Notas de Crédito, Notas de Débito, Liquidaciones de Compra, Retenciones, Retenciones v2**.

```html
<!-- Columna de acciones de la grilla -->
<button mat-icon-button matTooltip="Consultar estado en el SRI"
        (click)="abrirConsultaSRI(fila)"
        [disabled]="!fila.clave">
  <mat-icon>cloud_done</mat-icon>
</button>
```

```typescript
abrirConsultaSRI(fila: any): void {
  const ref = this.dialog.open(ConsultaSriDialogComponent, {
    width: '640px',
    data: {
      clave: fila.clave,
      tipoDoc: 'fctr',                              // 'ntcr' | 'ntdb' | 'lqcs' | 'rtnc' | 'rtv2'
      ambiente: fila.ambiente,
      permiteSincronizar: this.usuario.esAdministrador
    }
  });
  ref.afterClosed().subscribe(huboCambios => {
    if (huboCambios) this.cargarDocumentos();       // recarga la grilla
  });
}
```

> ⚠️ En las pantallas de retenciones, `tipoDoc` **debe** enviarse explícito (`'rtnc'` o `'rtv2'`) — la autodetección no puede distinguirlas porque ambas usan `codDoc = 07`.

### 10.2 Columna "Estado SRI" en la grilla

Aprovecha las columnas nuevas `estadoSRI` / `fechaConsultaSRI`:

```html
<ng-container matColumnDef="estadoSRI">
  <th mat-header-cell *matHeaderCellDef>Estado SRI</th>
  <td mat-cell *matCellDef="let f">
    <span class="badge" [ngClass]="'badge-' + claseEstadoSRI(f.estadoSRI)"
          [matTooltip]="f.fechaConsultaSRI ? 'Consultado: ' + f.fechaConsultaSRI : 'Nunca consultado'">
      {{ f.estadoSRI || '—' }}
    </span>
  </td>
</ng-container>
```

```typescript
claseEstadoSRI(estado: string): string {
  switch ((estado || '').toUpperCase()) {
    case 'AUTORIZADO':          return 'success';
    case 'NO AUTORIZADO':
    case 'ANULADO':             return 'danger';
    case 'PENDIENTE DE ANULAR': return 'warning';
    case 'RECHAZADA':           return 'info';
    default:                    return 'secondary';
  }
}
```

### 10.3 Pantalla "Sincronización con el SRI" (documentos pendientes)

Nueva opción de menú. Lista los documentos en estados `3` (firmada), `4` (enviada) y `6` (no autorizada) — que son exactamente los que pueden estar desactualizados — y permite consultarlos en bloque.

```typescript
export class SincronizacionSriComponent implements OnInit {

  tipoDoc: TipoDocSRI = 'fctr';
  documentos: any[] = [];
  seleccionados = new Set<string>();
  procesando = false;
  resumen: ResumenLoteSRI | null = null;
  resultados: RespuestaEstadoSRI[] = [];

  readonly ESTADOS_PENDIENTES = [3, 4, 6];
  readonly MAX_LOTE = 50;

  ngOnInit(): void { this.cargarPendientes(); }

  cargarPendientes(): void {
    // Reutiliza el selectByCriteria existente del endpoint del documento
    this.documentoService.buscarPorEstados(this.tipoDoc, this.ESTADOS_PENDIENTES)
      .subscribe(docs => this.documentos = docs);
  }

  toggle(clave: string): void {
    this.seleccionados.has(clave) ? this.seleccionados.delete(clave) : this.seleccionados.add(clave);
  }

  sincronizarSeleccionados(): void {
    const claves = Array.from(this.seleccionados);
    if (!claves.length) return;
    if (claves.length > this.MAX_LOTE) {
      alert(`Seleccione un máximo de ${this.MAX_LOTE} documentos por lote.`);
      return;
    }
    this.procesando = true;
    this.sri.consultarLote(claves, this.tipoDoc, { sincronizar: true }).subscribe({
      next: r => {
        this.resumen = r.resumen;
        this.resultados = r.resultados;
        this.procesando = false;
        this.seleccionados.clear();
        this.cargarPendientes();
      },
      error: () => { this.procesando = false; }
    });
  }
}
```

> ⏱️ **El lote es lento por diseño:** el backend hace `sleep(1)` entre consultas para no ser bloqueado por el SRI. 50 claves ≈ 60–90 segundos. Mostrar barra de progreso indeterminada y **subir el timeout del interceptor HTTP a ≥ 180 s para este endpoint**, o el navegador cortará la petición.

### 10.4 Detalle del documento

En la vista de detalle, un panel fijo:

```html
<div class="panel-sri">
  <h4>Estado en el SRI</h4>
  <p><strong>Último estado consultado:</strong> {{ documento.estadoSRI || 'Nunca consultado' }}</p>
  <p *ngIf="documento.fechaConsultaSRI"><strong>Fecha de consulta:</strong> {{ documento.fechaConsultaSRI | date:'dd/MM/yyyy HH:mm' }}</p>
  <p *ngIf="documento.negociableSRI"><strong>Factura comercial negociable:</strong> {{ documento.negociableSRI }}</p>
  <button mat-stroked-button (click)="abrirConsultaSRI(documento)">Consultar ahora</button>
</div>
```

---

## 11. Errores conocidos y cómo manejarlos

### 11.1 `SoapFault: Could not connect to host` / `failed to load external entity`

El servidor no alcanza el WSDL del SRI. Causas ordenadas por frecuencia:

1. El SRI está caído o en mantenimiento (habitual de madrugada). → reintentar.
2. Firewall de salida del hosting bloquea el puerto 443 hacia `*.sri.gob.ec`.
3. `allow_url_fopen = Off` en `php.ini` — `SoapClient` no puede bajar el WSDL. → activar.
4. `extension=soap` no cargada. → verificar con `php -m | grep soap`.

### 11.2 Timeouts

`ConsultaComprobante` responde típicamente en 1–4 s, pero puede pasar de 30 s en horas pico. La librería fija `connection_timeout = 30` y `default_socket_timeout = 60`. Si el hosting tiene `max_execution_time = 30`, el lote morirá a mitad de camino: subirlo a `300` para este endpoint (`ini_set('max_execution_time', 300);` al inicio de `gn_consulta_sri.php`).

### 11.3 Fallo de handshake SSL

El backend Java tuvo que instalar un `X509ExtendedTrustManager` permisivo porque el certificado de `cel.sri.gob.ec` no valida contra la IP (ver el comentario extenso de `SriHttpUtil.java`). En PHP esto normalmente **no** ocurre porque `SoapClient` resuelve por nombre de dominio. Si aun así falla:

1. Actualizar el CA bundle del servidor (`curl.cainfo` / `openssl.cafile` en `php.ini`).
2. Solo si lo anterior no es posible: poner `SRI_SSL_PERMISIVO` en `true` en `lib/sri_consulta.php`.

**Fallback cURL (contingencia, no es el camino por defecto).** Si el WSDL se vuelve inaccesible pero el endpoint SOAP sí responde, se puede replicar lo que hace `ConsultaSRIRest.java`: armar el envelope a mano y postearlo. Agregar a la librería y llamarlo solo desde el `catch (SoapFault)`:

```php
function sriConsultarEstadoAutorizacionCurl($clave, $ambiente)
{
    $url = str_replace('?wsdl', '', sriUrlBase($ambiente) . SRI_WS_CONSULTA_COMPROBANTE);
    $envelope =
        '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" ' .
        'xmlns:ec="http://ec.gob.sri.ws.consultas">' .
        '<soapenv:Header/><soapenv:Body>' .
        '<ec:consultarEstadoAutorizacionComprobante>' .
        '<claveAcceso>' . htmlspecialchars($clave, ENT_XML1) . '</claveAcceso>' .
        '</ec:consultarEstadoAutorizacionComprobante>' .
        '</soapenv:Body></soapenv:Envelope>';

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_POSTFIELDS     => $envelope,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 30,
        CURLOPT_TIMEOUT        => 60,
        CURLOPT_HTTPHEADER     => ['Content-Type: text/xml; charset=UTF-8', 'SOAPAction: ""'],
    ]);
    if (SRI_SSL_PERMISIVO === true) {
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
    }
    $xml = curl_exec($ch);
    curl_close($ch);
    // Parsear con DOMDocument buscando por getElementsByTagNameNS('*', 'tag'),
    // igual que parsearRespuestaEstado() de ConsultaSRIRest.java.
    return $xml;
}
```

### 11.4 Ambiente equivocado

Consultar una clave de producción contra `celcer` (o viceversa) devuelve `RECHAZADA / No existen datos`. Por eso el ambiente se deriva de la clave cuando no se envía. Si el usuario fuerza un `ambiente` distinto al dígito 24 de la clave, la pantalla debería advertirlo antes de llamar.

### 11.5 Mensajes duplicados o ausentes

`SoapClient` devuelve `mensajes->mensaje` como **objeto** si hay uno solo y como **array** si hay varios. `sriNormalizarMensajes()` cubre ambos casos; el código existente en `gn_autorizacion.php` **no** lo hace (accede a `->mensajes->mensaje->mensaje` directo), por eso ahí a veces sale vacío cuando el SRI envía varios mensajes.

### 11.6 Bloqueo por exceso de consultas

El SRI limita la frecuencia de consultas por IP. Reglas ya incorporadas: tope de 50 claves por lote y `sleep(1)` entre consultas. **No** exponer una consulta automática al abrir la grilla; debe ser siempre una acción explícita del usuario o un proceso programado nocturno.

---

## 12. Pruebas y checklist de aceptación

### 12.1 Pruebas manuales con `curl`

```bash
# 1. Estado de un comprobante (sin sincronizar)
curl -s "https://<host>/documents/gn_consulta_sri.php?accion=estado&clave=<CLAVE_49>&tipoDoc=fctr" | jq

# 2. Estado + sincronización
curl -s "https://<host>/documents/gn_consulta_sri.php?accion=estado&clave=<CLAVE_49>&tipoDoc=fctr&sincronizar=1" | jq '.sincronizacion'

# 3. Factura comercial negociable
curl -s "https://<host>/documents/gn_consulta_sri.php?accion=negociable&clave=<CLAVE_FACTURA_49>" | jq

# 4. Clave inválida → 400
curl -s -o /dev/null -w "%{http_code}\n" "https://<host>/documents/gn_consulta_sri.php?accion=estado&clave=123"

# 5. tipoDoc inválido → 400
curl -s "https://<host>/documents/gn_consulta_sri.php?accion=estado&clave=<CLAVE_49>&tipoDoc=zzzz" | jq

# 6. Lote
curl -s -X POST "https://<host>/documents/gn_consulta_sri.php" \
     -d 'accion=estadoLote' -d 'tipoDoc=fctr' -d 'sincronizar=1' \
     --data-urlencode 'claves=["<CLAVE_A>","<CLAVE_B>"]' | jq '.resumen'
```

### 12.2 Casos de prueba obligatorios

| # | Escenario | Resultado esperado |
|:--:|---|---|
| 1 | Factura autorizada, `estado` local = 5 | `AUTORIZADO`; sincronización solo actualiza `fechaConsultaSRI` |
| 2 | Factura autorizada en SRI, `estado` local = 4 | `estado` pasa a 5, `autorizacion` = clave, XML descargado a `a/`, fila `alterno=5` creada |
| 3 | Repetir el caso 2 | No se duplica la fila en `ptfc`; `estado` sigue 5 |
| 4 | Comprobante antiguo (fuera de rango) | `estadoConsulta = RECHAZADA`; **`estado` local intacto**; UI muestra el aviso de rango de fechas |
| 5 | Clave inexistente en el SRI | `RECHAZADA / No existen datos`; `estado` local intacto |
| 6 | Clave con DV incorrecto | HTTP 400, no se llama al SRI |
| 7 | Clave de 48 o 50 dígitos | HTTP 400 |
| 8 | Retención `rtv2` con `tipoDoc` explícito | Resuelve contra la tabla `rtv2`, no `rtnc` |
| 9 | Retención sin `tipoDoc` | Resuelve por búsqueda en ambas tablas; si no está en ninguna, `documentoLocal = null` |
| 10 | Factura negociable aceptada | `estadoConfirmacion = SI`, `fctr.negociableSRI = 'SI'` |
| 11 | Factura normal (no negociable) | `RECHAZADA`; la UI **no** lo presenta como error |
| 12 | SRI caído (apagar red / URL falsa) | HTTP 502, `exito: false`, **cero** cambios en BD |
| 13 | JWT de otro facturador | HTTP 403 |
| 14 | Lote de 51 claves | HTTP 400 |
| 15 | Documento inexistente en BD local | Consulta se ejecuta, `documentoLocal: null`, `sincronizacion.realizada: false` |

### 12.3 Checklist de despliegue

- [ ] `extension=soap` activa (`php -m | grep -i soap`)
- [ ] `allow_url_fopen = On`
- [ ] Salida HTTPS hacia `celcer.sri.gob.ec` y `cel.sri.gob.ec` permitida en el firewall
- [ ] DDL de §4 aplicado en todas las tablas (6 × `estadoSRI`/`fechaConsultaSRI`, + `fctr.negociableSRI`, + índices sobre `clave`)
- [ ] Permisos de escritura en `resources/{idFacturador}/{carpeta}/c/` y `/a/`
- [ ] `max_execution_time ≥ 300` para el endpoint (o `ini_set` al inicio del archivo)
- [ ] Timeout del interceptor HTTP de Angular ≥ 180 s para el endpoint de lote
- [ ] Probado primero en ambiente `1` (celcer) antes de tocar producción
- [ ] Sincronización probada sobre un documento de prueba **antes** de habilitar el botón masivo a usuarios finales
