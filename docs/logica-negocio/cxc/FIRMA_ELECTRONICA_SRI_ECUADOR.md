# Firma Electrónica para Documentos del SRI - Ecuador

## 1. Estándar requerido

| Parámetro | Valor |
|-----------|-------|
| Estándar de firma | **XAdES-BES** |
| Versión del esquema | 1.3.2 (`http://uri.etsi.org/01903/v1.3.2#`) |
| Codificación | **UTF-8** |
| Tipo de firma | **ENVELOPED** (`http://www.w3.org/2000/09/xmldsig#enveloped-signature`) |
| Algoritmo de firmado | **RSA-SHA1** |
| Longitud de clave | **2048 bits** |
| Formato del certificado | **PKCS12** (extensión `.p12`) |

---

## 2. Estructura de la firma dentro del XML

La firma **es un nodo más** dentro del comprobante electrónico `.xml`. El nivel de seguridad cubre **tres partes** de la trama:

1. **Todos los nodos del comprobante** (`<factura id="comprobante">`, etc.)
2. **El contenedor `SignedProperties`** (propiedades de la firma XAdES)
3. **El certificado digital** incluido en el elemento `<ds:KeyInfo>`

### 2.1 Estructura general del nodo `<ds:Signature>`

```xml
<ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
              xmlns:etsi="http://uri.etsi.org/01903/v1.3.2#"
              Id="SignatureXXXXXX">

  <!-- 1. SignedInfo: lista de lo que se firma y sus hashes -->
  <ds:SignedInfo Id="Signature-SignedInfoXXXXXX">
    <ds:CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
    <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>

    <!-- Referencia 1: hash de SignedProperties -->
    <ds:Reference Id="SignedPropertiesIDXXXXXX"
                  Type="http://uri.etsi.org/01903#SignedProperties"
                  URI="#SignatureXXXXXX-SignedPropertiesXXXXXX">
      <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
      <ds:DigestValue><!-- SHA1 en Base64 del bloque SignedProperties --></ds:DigestValue>
    </ds:Reference>

    <!-- Referencia 2: hash del elemento KeyInfo (certificado) -->
    <ds:Reference URI="#CertificateXXXXXX">
      <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
      <ds:DigestValue><!-- SHA1 en Base64 del bloque KeyInfo --></ds:DigestValue>
    </ds:Reference>

    <!-- Referencia 3: hash del comprobante XML completo -->
    <ds:Reference Id="Reference-ID-XXXXXX" URI="#comprobante">
      <ds:Transforms>
        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
      </ds:Transforms>
      <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
      <ds:DigestValue><!-- SHA1 en Base64 del XML del comprobante --></ds:DigestValue>
    </ds:Reference>
  </ds:SignedInfo>

  <!-- 2. Valor de la firma RSA-SHA1 del SignedInfo -->
  <ds:SignatureValue Id="SignatureValueXXXXXX">
    <!-- Firma RSA-SHA1 en Base64 del SignedInfo (con namespaces inline) -->
  </ds:SignatureValue>

  <!-- 3. KeyInfo: certificado y clave pública -->
  <ds:KeyInfo Id="CertificateXXXXXX">
    <ds:X509Data>
      <ds:X509Certificate><!-- Certificado X.509 en Base64 --></ds:X509Certificate>
    </ds:X509Data>
    <ds:KeyValue>
      <ds:RSAKeyValue>
        <ds:Modulus><!-- Módulo RSA en Base64 --></ds:Modulus>
        <ds:Exponent><!-- Exponente RSA en Base64 --></ds:Exponent>
      </ds:RSAKeyValue>
    </ds:KeyValue>
  </ds:KeyInfo>

  <!-- 4. Object: propiedades XAdES-BES -->
  <ds:Object Id="SignatureXXXXXX-ObjectXXXXXX">
    <etsi:QualifyingProperties Target="#SignatureXXXXXX">
      <etsi:SignedProperties Id="SignatureXXXXXX-SignedPropertiesXXXXXX">
        <etsi:SignedSignatureProperties>
          <etsi:SigningTime>2026-07-14T00:54:17-05:00</etsi:SigningTime>
          <etsi:SigningCertificate>
            <etsi:Cert>
              <etsi:CertDigest>
                <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
                <ds:DigestValue><!-- SHA1 del certificado DER en Base64 --></ds:DigestValue>
              </etsi:CertDigest>
              <etsi:IssuerSerial>
                <ds:X509IssuerName><!-- DN del emisor del certificado --></ds:X509IssuerName>
                <ds:X509SerialNumber><!-- Número de serie decimal --></ds:X509SerialNumber>
              </etsi:IssuerSerial>
            </etsi:Cert>
          </etsi:SigningCertificate>
        </etsi:SignedSignatureProperties>
        <etsi:SignedDataObjectProperties>
          <etsi:DataObjectFormat ObjectReference="#Reference-ID-XXXXXX">
            <etsi:Description>contenido comprobante</etsi:Description>
            <etsi:MimeType>text/xml</etsi:MimeType>
          </etsi:DataObjectFormat>
        </etsi:SignedDataObjectProperties>
      </etsi:SignedProperties>
    </etsi:QualifyingProperties>
  </ds:Object>

</ds:Signature>
```

---

## 3. Algoritmo de cálculo de hashes (CRÍTICO)

> ⚠️ **El error más común** es usar la forma canónica C14N para calcular los SHA1. El SRI **NO** valida con C14N; valida con el **hash del texto plano en UTF-8** de cada bloque de string, con los namespaces añadidos inline.

### 3.1 Hash del comprobante (`sha1Factura`)

```
sha1_utf8(documentoXML_sin_declaracion_xml)
```

- El documento debe estar **sin** la declaración `<?xml ...?>`.
- Debe estar **sin** el trailing newline final.
- Codificación: **UTF-8**.

### 3.2 Hash de `KeyInfo` (`sha1Certificado`)

```
sha1_utf8(KeyInfoString_con_namespaces_inline)
```

Donde `KeyInfoString_con_namespaces_inline` es el bloque `<ds:KeyInfo>` con los namespaces `xmlns:ds` y `xmlns:etsi` añadidos **directamente** en el elemento raíz:

```xml
<ds:KeyInfo xmlns:ds="..." xmlns:etsi="..." Id="CertificateXXXXXX">
  ...
</ds:KeyInfo>
```

### 3.3 Hash de `SignedProperties` (`sha1SignedProperties`)

```
sha1_utf8(SignedPropertiesString_con_namespaces_inline)
```

Mismo principio: los namespaces se añaden al elemento `<etsi:SignedProperties>`.

### 3.4 Firma del `SignedInfo`

```
RSA_SHA1(SignedInfoString_con_namespaces_inline)
```

El valor firmado (en Base64) es la firma RSA-SHA1 del string del bloque `<ds:SignedInfo>` con namespaces inline, codificado en **UTF-8**.

---

## 4. Consideraciones sobre el certificado

### 4.1 Certificado en `<ds:X509Certificate>`

- Extraer del PKCS12 en formato **DER → Base64**.
- El Base64 debe estar en **líneas de 76 caracteres**.
- Sin cabeceras `-----BEGIN CERTIFICATE-----` ni `-----END CERTIFICATE-----`.

### 4.2 Módulo RSA (`<ds:Modulus>`)

- Convertir el BigInteger `n` a hexadecimal → bytes → Base64.
- Base64 en **líneas de 76 caracteres**.

### 4.3 Exponente RSA (`<ds:Exponent>`)

- Normalmente el valor es `AQAB` (65537 = 0x010001).
- Convertir hex `010001` → bytes `[1, 0, 1]` → Base64 `AQAB`.

### 4.4 Hash del certificado DER (`<ds:DigestValue>` en `CertDigest`)

```
sha1(certificado_en_bytes_DER)  → Base64
```

Este es el hash de los **bytes raw** del certificado, **NO** del string Base64.

### 4.5 Nombre del emisor (`<ds:X509IssuerName>`)

- Usar formato **RFC2253** sin espacios después de comas.
- Decodificar valores hexadecimales (ej: `#0c0f...` → texto legible).
- Ejemplo: `2.5.4.97=VATES-A66721499,CN=UANATACA CA2 2016,OU=TSP-UANATACA,...`

### 4.6 Número de serie (`<ds:X509SerialNumber>`)

- Valor **decimal** (no hexadecimal).
- Ej: `1781273665491384260`

---

## 5. Namespaces requeridos en `<ds:Signature>`

El elemento raíz de la firma **debe declarar** ambos namespaces:

```xml
<ds:Signature
  xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
  xmlns:etsi="http://uri.etsi.org/01903/v1.3.2#"
  Id="SignatureXXXXXX">
```

---

## 6. El atributo `id` del comprobante

El elemento raíz del comprobante (ej: `<factura>`) **debe tener** el atributo `id="comprobante"`:

```xml
<factura id="comprobante" version="1.1.0">
```

La referencia de firma apunta a ese id:

```xml
<ds:Reference Id="Reference-ID-XXXXXX" URI="#comprobante">
```

---

## 7. Flujo completo de firma

```
1. Cargar XML sin firmar (sin declaración <?xml?>)
2. Calcular sha1Factura = SHA1_UTF8(documento)
3. Cargar certificado PKCS12 (.p12)
4. Extraer: cert X.509, clave privada RSA
5. Construir bloque SignedProperties (con signing time)
6. Calcular sha1SignedProperties = SHA1_UTF8(SignedProperties + namespaces inline)
7. Construir bloque KeyInfo (con cert y módulo/exponente)
8. Calcular sha1Certificado = SHA1_UTF8(KeyInfo + namespaces inline)
9. Construir bloque SignedInfo (con los 3 hashes anteriores)
10. Calcular firmaSignedInfo = RSA_SHA1(SignedInfo + namespaces inline) → Base64 líneas 76 chars
11. Ensamblar xadesBes = <ds:Signature> + SignedInfo + firmaSignedInfo + KeyInfo + SignedProperties
12. Insertar xadesBes ANTES del tag de cierre del comprobante (</factura>)
13. Agregar declaración <?xml version="1.0" encoding="UTF-8" standalone="no"?> al inicio
14. Enviar al SRI como bytes UTF-8 en Base64
```

---

## 8. Envío al SRI (WS1 Recepción)

El XML firmado se envía **codificado en Base64** al Web Service SOAP:

```xml
<validarComprobante xmlns="http://ec.gob.sri.ws.recepcion">
  <xml><!-- XML_FIRMADO_EN_BASE64 --></xml>
</validarComprobante>
```

- **Ambiente de pruebas:** `https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl`
- **Ambiente de producción:** `https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl`

Respuesta esperada: `<estado>RECIBIDA</estado>`

---

## 9. Consulta de autorización (WS2)

Tras esperar ~2 segundos:

```xml
<autorizacionComprobante xmlns="http://ec.gob.sri.ws.autorizacion">
  <claveAccesoComprobante>CLAVE_48_DIGITOS</claveAccesoComprobante>
</autorizacionComprobante>
```

- **Ambiente de pruebas:** `https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl`
- **Ambiente de producción:** `https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl`

Respuesta esperada: `<estado>AUTORIZADO</estado>`

---

## 10. Estructura de carpetas para almacenamiento

```
resources/{idFacturador}/
  docs/
    g/   → XML generado (sin firmar)          alterno=2
    f/   → XML firmado                        alterno=3
    e/   → XML enviado al SRI (WS1)          alterno=4
    a/   → XML y TXT autorizado (WS2 OK)     alterno=5
    n/   → XML y TXT no autorizado (WS2 NO)  alterno=6
  pdf/   → RIDE PDF generado después de autorización
```

---

## 11. Errores comunes y soluciones

| Error SRI | Causa | Solución |
|-----------|-------|----------|
| `FIRMA INVALIDA [firma y/o certificados alterados]` | El XML fue modificado después de firmar (saltos de línea, reemplazos, re-serialización DOM) | No modificar el string del XML después de `firmarXML()` |
| `FIRMA INVALIDA [firma y/o certificados alterados]` | Los hashes se calcularon con C14N en vez de SHA1 sobre string plano | Usar el algoritmo string-based igual al código TypeScript de referencia |
| `CLAVE DE ACCESO REGISTRADA` | El comprobante ya fue enviado anteriormente | Consultar directamente el WS2 de autorización |
| Error de namespace `etsi` no declarado | El elemento `<ds:Signature>` no tiene `xmlns:etsi` | Añadir el namespace explícitamente al elemento Signature |
| Hash incorrecto de `KeyInfo` | Se hizo hash solo del contenido sin los namespaces | Añadir `xmlns:ds` y `xmlns:etsi` inline antes de hashear |

---

## 12. Librerías utilizadas por el SRI (referencia)

- `MITyCLibXADES`
- `MITyCLibTSA`
- `MITyCLibAPI`
- `MITyCLibOCSP`
- `MITyCLibTrust`

Referencias técnicas:
- XAdES: http://webapp.etsi.org/workprogram/Report_WorkItem.asp?WKI_ID=21353
- RSA Encryption RFC: http://www.ietf.org/rfc/rfc2313.txt
- NIST Key Length: http://csrc.nist.gov/publications/nistpubs/800-57/sp800-57-Part1-revised2_Mar08-2007.pdf
