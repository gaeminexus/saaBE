package com.saa.ejb.signature.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio de firma electrónica compatible con SRI Ecuador.
 * Genera firmas XAdES-BES con el formato exacto requerido por el SRI.
 */
@Stateless
public class SignatureServiceImpl implements SignatureService {
	
	@EJB
	private FacturadorDaoService facturadorDaoService;
	
	static {
		// Registrar proveedor BouncyCastle
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Override
	public String firmarXML(String xmlSinFirmar, String pathCertificado, String passwordCertificado) throws Exception {
		System.out.println("Iniciando proceso de firma electrónica XML");
		
		try {
			// 1. Parsear el XML sin firmar
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new ByteArrayInputStream(xmlSinFirmar.getBytes("UTF-8")));
			
			// 2. Cargar certificado PKCS#12
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			FileInputStream fis = new FileInputStream(pathCertificado);
			keyStore.load(fis, passwordCertificado.toCharArray());
			fis.close();
			
			// 3. Obtener clave privada y certificado
			String alias = keyStore.aliases().nextElement();
			PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passwordCertificado.toCharArray());
			X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
			
			// 4. Crear la firma XML con el formato exacto del SRI
			String xmlFirmado = crearFirmaXAdES(doc, privateKey, cert);
			
			System.out.println("Firma electrónica completada exitosamente");
			return xmlFirmado;
			
		} catch (Exception e) {
			System.err.println("Error al firmar XML: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error al firmar documento electrónico: " + e.getMessage(), e);
		}
	}

	@Override
	public String firmarXMLFacturador(String xmlSinFirmar, Long idFacturador) throws Exception {
		System.out.println("Firmando XML para facturador ID: " + idFacturador);
		
		try {
			// Obtener datos del facturador
			Facturador facturador = facturadorDaoService.selectById(idFacturador, NombreEntidadesCobro.FACTURADOR);
			
			if (facturador == null) {
				throw new IncomeException("Facturador no encontrado con ID: " + idFacturador);
			}
			
		String pathCertificado = facturador.getFirma();
		String passwordCertificado = facturador.getClaveFirma();
		
		if (pathCertificado == null || pathCertificado.isEmpty()) {
			throw new IncomeException("El facturador no tiene certificado digital configurado");
		}
		
		if (passwordCertificado == null || passwordCertificado.isEmpty()) {
			throw new IncomeException("El facturador no tiene contraseña de certificado configurada");
		}
		
		// El path de la firma YA viene completo desde el campo del facturador
		// FileService retorna el path absoluto completo cuando el frontend sube el archivo
		System.out.println("Usando certificado en path: " + pathCertificado);
		
		// Firmar el XML usando directamente el path completo
		return firmarXML(xmlSinFirmar, pathCertificado, passwordCertificado);
			
		} catch (Throwable e) {
			System.err.println("Error al firmar XML para facturador: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error al firmar XML: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean verificarFirma(String xmlFirmado) {
		// TODO: Implementar verificación de firma si es necesario
		return false;
	}
	
	/**
	 * Crea una firma XAdES-BES con el formato exacto requerido por el SRI de Ecuador
	 */
	private String crearFirmaXAdES(Document doc, PrivateKey privateKey, X509Certificate cert) throws Exception {
		
		// Generar IDs únicos para la firma (similar al formato del ejemplo)
		String signatureId = "Signature" + generarIdAleatorio();
		String signedInfoId = "Signature-SignedInfo" + generarIdAleatorio();
		String signedPropertiesId = signatureId + "-SignedProperties" + generarIdAleatorio();
		String certificateId = "Certificate" + generarIdAleatorio();
		String signatureValueId = "SignatureValue" + generarIdAleatorio();
		String objectId = signatureId + "-Object" + generarIdAleatorio();
		String referenceId = "Reference-ID-" + generarIdAleatorio();
		String signedPropertiesReferenceId = "SignedPropertiesID" + generarIdAleatorio();
		
		// Obtener el elemento raíz (factura, notaCredito, etc.)
		Element root = doc.getDocumentElement();
		String comprobanteId = root.getAttribute("id");
		if (comprobanteId == null || comprobanteId.isEmpty()) {
			throw new Exception("El documento XML no tiene atributo 'id' en el elemento raíz. El ID debe ser la clave de acceso del documento.");
		}
		// IMPORTANTE: Registrar el atributo id como ID del documento para que pueda ser resuelto
		root.setIdAttribute("id", true);
		
		// Crear XMLSignatureFactory con proveedor DOM
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
		
		// 1. Crear Reference al documento (con enveloped-signature transform)
		List<Transform> transforms = new ArrayList<>();
		transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
		
		Reference refDoc = fac.newReference(
			"#" + comprobanteId,
			fac.newDigestMethod(DigestMethod.SHA1, null),
			transforms,
			null,
			referenceId
		);
		
		// 2. Crear Reference al certificado
		Reference refCert = fac.newReference(
			"#" + certificateId,
			fac.newDigestMethod(DigestMethod.SHA1, null),
			null,
			null,
			null
		);
		
		// 3. Crear Reference a SignedProperties (XAdES)
		Reference refSignedProps = fac.newReference(
			"#" + signedPropertiesId,
			fac.newDigestMethod(DigestMethod.SHA1, null),
			null,
			"http://uri.etsi.org/01903#SignedProperties",
			signedPropertiesReferenceId
		);
		
		// Lista de referencias en el orden correcto
		List<Reference> references = new ArrayList<>();
		references.add(refSignedProps);
		references.add(refCert);
		references.add(refDoc);
		
		// Crear SignedInfo
		SignedInfo si = fac.newSignedInfo(
			fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
			fac.newSignatureMethod("http://www.w3.org/2000/09/xmldsig#rsa-sha1", null),
			references,
			signedInfoId
		);
		
		// Crear KeyInfo
		KeyInfoFactory kif = fac.getKeyInfoFactory();
		
		// X509Data con el certificado
		List<Object> x509Content = new ArrayList<>();
		x509Content.add(cert);
		X509Data xd = kif.newX509Data(x509Content);
		
		// KeyValue con la clave pública
		KeyValue kv = kif.newKeyValue(cert.getPublicKey());
		
		List<javax.xml.crypto.XMLStructure> keyInfoContent = new ArrayList<>();
		keyInfoContent.add(xd);
		keyInfoContent.add(kv);
		
		KeyInfo ki = kif.newKeyInfo(keyInfoContent, certificateId);
		
		// Crear XMLObject con QualifyingProperties (XAdES)
		String xadesNamespace = "http://uri.etsi.org/01903/v1.3.2#";
		Element qualifyingProperties = crearQualifyingProperties(doc, cert, signatureId, signedPropertiesId, referenceId, xadesNamespace);
		
		javax.xml.crypto.dom.DOMStructure domStructure = new javax.xml.crypto.dom.DOMStructure(qualifyingProperties);
		javax.xml.crypto.dsig.XMLObject obj = fac.newXMLObject(Collections.singletonList(domStructure), objectId, null, null);
		
		// Crear XMLSignature
		XMLSignature signature = fac.newXMLSignature(si, ki, Collections.singletonList(obj), signatureId, signatureValueId);
		
		// Crear DOMSignContext y firmar
		DOMSignContext dsc = new DOMSignContext(privateKey, root);
		
		// Registrar namespace ETSI para XAdES
		dsc.putNamespacePrefix(xadesNamespace, "etsi");
		dsc.putNamespacePrefix(XMLSignature.XMLNS, "ds");
		
		// Firmar el documento
		signature.sign(dsc);
		
		// CRÍTICO: Agregar namespace xmlns:etsi al elemento ds:Signature
		// El XMLSignatureFactory no lo agrega automáticamente aunque esté en DOMSignContext
		// El SRI requiere que este namespace esté declarado en el elemento raíz de la firma
		NodeList signatureNodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (signatureNodes.getLength() > 0) {
			Element signatureElement = (Element) signatureNodes.item(0);
			signatureElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:etsi", xadesNamespace);
		}
		
		// Convertir el documento firmado a String
		return documentToString(doc);
	}
	
	/**
	 * Crea el elemento QualifyingProperties de XAdES-BES
	 */
	private Element crearQualifyingProperties(Document doc, X509Certificate cert, String signatureId, 
			String signedPropertiesId, String referenceId, String xadesNamespace) throws Exception {
		
		// Crear elemento QualifyingProperties
		Element qualifyingProperties = doc.createElementNS(xadesNamespace, "etsi:QualifyingProperties");
		qualifyingProperties.setAttribute("Target", "#" + signatureId);
		
		// SignedProperties
		Element signedProperties = doc.createElementNS(xadesNamespace, "etsi:SignedProperties");
		signedProperties.setAttribute("Id", signedPropertiesId);
		// IMPORTANTE: Registrar el atributo Id como ID del documento para que pueda ser resuelto
		signedProperties.setIdAttribute("Id", true);
		
		// SignedSignatureProperties
		Element signedSignatureProperties = doc.createElementNS(xadesNamespace, "etsi:SignedSignatureProperties");
		
		// SigningTime (fecha y hora actual en formato ISO 8601 con timezone)
		// CORRECCIÓN: El formato correcto del SRI es yyyy-MM-dd'T'HH:mm:ssXXX (sin microsegundos)
		Element signingTime = doc.createElementNS(xadesNamespace, "etsi:SigningTime");
		java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
		String timestamp = java.time.ZonedDateTime.now().format(formatter);
		signingTime.setTextContent(timestamp);
		signedSignatureProperties.appendChild(signingTime);
		
		// SigningCertificate
		Element signingCertificate = doc.createElementNS(xadesNamespace, "etsi:SigningCertificate");
		Element certElement = doc.createElementNS(xadesNamespace, "etsi:Cert");
		
		// CertDigest
		Element certDigest = doc.createElementNS(xadesNamespace, "etsi:CertDigest");
		Element digestMethod = doc.createElementNS(XMLSignature.XMLNS, "ds:DigestMethod");
		digestMethod.setAttribute("Algorithm", DigestMethod.SHA1);
		Element digestValue = doc.createElementNS(XMLSignature.XMLNS, "ds:DigestValue");
		digestValue.setTextContent(calcularDigestCertificado(cert));
		certDigest.appendChild(digestMethod);
		certDigest.appendChild(digestValue);
		certElement.appendChild(certDigest);
		
		// IssuerSerial
		Element issuerSerial = doc.createElementNS(xadesNamespace, "etsi:IssuerSerial");
		Element x509IssuerName = doc.createElementNS(XMLSignature.XMLNS, "ds:X509IssuerName");
		// CORRECCIÓN CRÍTICA: El SRI requiere formato RFC2253 pero SIN espacios, sin prefijo OID, y SIN valores hexadecimales
		// Obtener el DN y limpiar el formato para que coincida con el ejemplo del SRI
		String issuerDN = cert.getIssuerX500Principal().getName(javax.security.auth.x500.X500Principal.RFC2253);
		// Remover espacios después de las comas (RFC2253 puede agregarlos)
		issuerDN = issuerDN.replaceAll(", ", ",");
		// Remover prefijo "OID." que puede aparecer en algunos atributos
		issuerDN = issuerDN.replaceAll("OID\\.", "");
		// CRÍTICO: Decodificar valores hexadecimales a texto legible
		// Los valores hexadecimales tienen el formato #XXXXXX donde XXXX son bytes en hex
		// Ejemplo: #0c0f56415445532d413636373231343939 debe convertirse a VATES-A66721499
		issuerDN = decodificarValoresHexadecimales(issuerDN);
		x509IssuerName.setTextContent(issuerDN);
		Element x509SerialNumber = doc.createElementNS(XMLSignature.XMLNS, "ds:X509SerialNumber");
		x509SerialNumber.setTextContent(cert.getSerialNumber().toString());
		issuerSerial.appendChild(x509IssuerName);
		issuerSerial.appendChild(x509SerialNumber);
		certElement.appendChild(issuerSerial);
		
		signingCertificate.appendChild(certElement);
		signedSignatureProperties.appendChild(signingCertificate);
		
		signedProperties.appendChild(signedSignatureProperties);
		
		// SignedDataObjectProperties
		Element signedDataObjectProperties = doc.createElementNS(xadesNamespace, "etsi:SignedDataObjectProperties");
		Element dataObjectFormat = doc.createElementNS(xadesNamespace, "etsi:DataObjectFormat");
		dataObjectFormat.setAttribute("ObjectReference", "#" + referenceId);
		
		Element description = doc.createElementNS(xadesNamespace, "etsi:Description");
		description.setTextContent("contenido comprobante");
		dataObjectFormat.appendChild(description);
		
		Element mimeType = doc.createElementNS(xadesNamespace, "etsi:MimeType");
		mimeType.setTextContent("text/xml");
		dataObjectFormat.appendChild(mimeType);
		
		signedDataObjectProperties.appendChild(dataObjectFormat);
		signedProperties.appendChild(signedDataObjectProperties);
		
		qualifyingProperties.appendChild(signedProperties);
		
		return qualifyingProperties;
	}
	
	/**
	 * Calcula el digest SHA1 del certificado en Base64
	 */
	private String calcularDigestCertificado(X509Certificate cert) throws Exception {
		java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(cert.getEncoded());
		return java.util.Base64.getEncoder().encodeToString(digest);
	}
	
	/**
	 * Genera un ID aleatorio de 6 dígitos
	 */
	private String generarIdAleatorio() {
		return String.valueOf((int)(Math.random() * 900000) + 100000);
	}
	
	/**
	 * Convierte un Document XML a String preservando el formato original del SRI
	 */
	private String documentToString(Document doc) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		
		// Configuración básica del XML
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.STANDALONE, "no");
		
		// CRÍTICO: NO indentar para preservar el formato original del XML
		// El XML sin firmar ya tiene su estructura con 1 espacio de indentación
		// Si habilitamos INDENT=yes, el Transformer re-indenta todo con espacios adicionales
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
		
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		
		String resultado = writer.getBuffer().toString();
		
		// LIMPIEZA: Remover caracteres escapados que pueden aparecer
		resultado = resultado.replace("&#13;", "");
		resultado = resultado.replace("&#10;", "");
		
		// FORMATO DE LA FIRMA: Agregar saltos de línea a los elementos principales de la firma
		// El SRI requiere que la firma tenga saltos de línea en ciertos elementos
		resultado = formatearFirmaXML(resultado);
		
		return resultado;
	}
	
	/**
	 * Formatea la firma digital replicando EXACTAMENTE el formato del código TypeScript de referencia.
	 * Basado en xades-firma-docs.ts líneas 54-106
	 */
	private String formatearFirmaXML(String xml) {
		// El código TypeScript construye el XML con saltos de línea MUY específicos
		// Debemos replicar exactamente ese patrón
		
		// 1. Salto de línea después de <ds:Signature...>
		xml = xml.replaceFirst("(<ds:Signature[^>]*>)", "$1\n");
		
		// 2. Formatear SignedInfo (líneas 80-99 del TypeScript)
		xml = xml.replaceFirst("(<ds:SignedInfo[^>]*>)", "$1\n");
		
		// CORRECCIÓN: Salto de línea entre CanonicalizationMethod y SignatureMethod
		xml = xml.replaceFirst("(<ds:CanonicalizationMethod[^>]*/>)", "$1\n");
		
		// CORRECCIÓN 1: Salto de línea después de SignatureMethod antes del primer Reference
		xml = xml.replaceFirst("(<ds:SignatureMethod[^>]*/>)(<ds:Reference)", "$1\n$2");
		
		// 3. Formatear cada Reference
		// Para el primer Reference (SignedProperties) - debe tener salto después de la apertura
		xml = xml.replaceFirst("(<ds:Reference Id=\"SignedPropertiesID[^>]*>)(<ds:DigestMethod)", "$1\n$2");
		
		// CORRECCIÓN 3: DigestMethod debe tener salto antes del DigestValue en los Reference dentro de SignedInfo
		// Aplicar a TODOS los Reference (no solo el primero)
		xml = xml.replaceAll("(<ds:DigestMethod Algorithm=\"[^\"]*\"/>)(<ds:DigestValue>)", "$1\n$2");
		
		// CORRECCIÓN: Salto después de cada </ds:DigestValue> dentro de Reference
		xml = xml.replaceAll("(</ds:DigestValue>)(</ds:Reference>)", "$1\n$2");
		
		// CORRECCIÓN 2: Salto de línea antes del segundo Reference (Certificate)
		xml = xml.replaceFirst("(</ds:Reference>)(<ds:Reference URI=\"#Certificate)", "$1\n$2");
		
		// Para el segundo Reference (Certificate) - debe tener salto después de la apertura
		xml = xml.replaceFirst("(<ds:Reference URI=\"#Certificate[^>]*>)(<ds:DigestMethod)", "$1\n$2");
		
		// CORRECCIÓN: Salto de línea antes del tercer Reference (con Transforms)
		xml = xml.replaceFirst("(</ds:Reference>)(<ds:Reference Id=\"Reference-ID-)", "$1\n$2");
		
		// CORRECCIÓN: Reference con Transforms debe tener salto después de apertura antes de Transforms
		xml = xml.replaceFirst("(<ds:Reference Id=\"Reference-ID-[^>]*>)(<ds:Transforms>)", "$1\n$2");
		
		// Transforms
		xml = xml.replace("<ds:Transforms>", "<ds:Transforms>\n");
		
		// CORRECCIÓN: Salto entre Transform y cierre de Transforms
		xml = xml.replaceAll("(<ds:Transform[^>]*/>)(</ds:Transforms>)", "$1\n$2");
		xml = xml.replace("</ds:Transforms>", "</ds:Transforms>\n");
		
		// 4. Cierre de SignedInfo
		xml = xml.replace("</ds:SignedInfo>", "\n</ds:SignedInfo>");
		
		// 5. SignatureValue
		// CORRECCIÓN: El SignatureValue debe iniciar en nueva línea y su contenido también
		xml = xml.replaceFirst("(<ds:SignatureValue[^>]*>)([^<]+)", "\n$1\n$2");
		xml = xml.replace("</ds:SignatureValue>", "\n</ds:SignatureValue>");
		
		// 6. KeyInfo
		xml = xml.replace("<ds:KeyInfo ", "\n<ds:KeyInfo ");
		xml = xml.replace("<ds:X509Data>", "\n<ds:X509Data>\n");
		xml = xml.replace("<ds:X509Certificate>", "<ds:X509Certificate>\n");
		xml = xml.replace("</ds:X509Certificate>", "\n</ds:X509Certificate>");
		xml = xml.replace("</ds:X509Data>", "\n</ds:X509Data>");
		
		// KeyValue
		xml = xml.replace("<ds:KeyValue>", "\n<ds:KeyValue>\n");
		xml = xml.replace("<ds:RSAKeyValue>", "<ds:RSAKeyValue>\n");
		xml = xml.replace("<ds:Modulus>", "<ds:Modulus>\n");
		xml = xml.replace("</ds:Modulus>", "\n</ds:Modulus>\n");
		xml = xml.replace("<ds:Exponent>", "<ds:Exponent>");
		xml = xml.replace("</ds:Exponent>", "</ds:Exponent>\n");
		xml = xml.replace("</ds:RSAKeyValue>", "</ds:RSAKeyValue>\n");
		xml = xml.replace("</ds:KeyValue>", "</ds:KeyValue>\n");
		xml = xml.replace("</ds:KeyInfo>", "</ds:KeyInfo>");
		
		// 7. Object
		xml = xml.replace("<ds:Object ", "\n<ds:Object ");
		
		// 8. CORRECCIÓN CRÍTICA: En los elementos XAdES (etsi:CertDigest), 
		// NO debe haber salto de línea entre DigestMethod y DigestValue
		// Primero eliminar salto entre </ds:DigestValue> y </etsi:CertDigest>
		xml = xml.replaceAll("(</ds:DigestValue>)\\s+(</etsi:CertDigest>)", "$1$2");
		
		// CORRECCIÓN: Eliminar salto entre DigestMethod y DigestValue SOLO dentro de etsi:CertDigest
		// Este regex busca el contexto de etsi:CertDigest y elimina saltos internos
		xml = xml.replaceAll("(<etsi:CertDigest>\\s*<ds:DigestMethod[^>]*/>)\\s+(<ds:DigestValue>)", "$1$2");
		
		// Los elementos etsi quedan compactos
		xml = xml.replace("</ds:Object>", "</ds:Object>");
		xml = xml.replace("</ds:Signature>", "</ds:Signature>");
		
		return xml;
	}
	
	/**
	 * Decodifica valores hexadecimales en el Distinguished Name a texto legible.
	 * Los valores hexadecimales tienen el formato #XXXXXX donde XXXX son bytes en hexadecimal.
	 * Ejemplo: 2.5.4.97=#0c0f56415445532d413636373231343939 
	 *       -> 2.5.4.97=VATES-A66721499
	 */
	private String decodificarValoresHexadecimales(String dn) {
		// Patrón para encontrar valores hexadecimales en el DN: atributo=#hexvalor
		// Ejemplo: 2.5.4.97=#0c0f56415445532d413636373231343939
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([^,=]+)=#([0-9a-fA-F]+)");
		java.util.regex.Matcher matcher = pattern.matcher(dn);
		
		StringBuffer resultado = new StringBuffer();
		while (matcher.find()) {
			String atributo = matcher.group(1);
			String valorHex = matcher.group(2);
			
			try {
				// Los primeros 2 bytes son el tipo y longitud (ASN.1 encoding)
				// 0c = UTF8String, siguiente byte es la longitud
				// Saltar los primeros 4 caracteres hex (2 bytes)
				if (valorHex.length() > 4) {
					String datosHex = valorHex.substring(4);
					
					// Convertir hex a bytes
					byte[] bytes = new byte[datosHex.length() / 2];
					for (int i = 0; i < bytes.length; i++) {
						bytes[i] = (byte) Integer.parseInt(datosHex.substring(i * 2, i * 2 + 2), 16);
					}
					
					// Convertir bytes a String UTF-8
					String valorLegible = new String(bytes, "UTF-8");
					
					// Reemplazar el valor hexadecimal con el valor legible
					matcher.appendReplacement(resultado, atributo + "=" + valorLegible);
				} else {
					// Si el hex es muy corto, dejarlo como está
					matcher.appendReplacement(resultado, matcher.group(0));
				}
			} catch (Exception e) {
				// Si hay error al decodificar, dejar el valor original
				matcher.appendReplacement(resultado, matcher.group(0));
			}
		}
		matcher.appendTail(resultado);
		
		return resultado.toString();
	}
}
