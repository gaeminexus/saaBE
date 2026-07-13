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
		Element signingTime = doc.createElementNS(xadesNamespace, "etsi:SigningTime");
		String timestamp = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
		x509IssuerName.setTextContent(cert.getIssuerX500Principal().getName());
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
	 * Convierte un Document XML a String con formato adecuado
	 */
	private String documentToString(Document doc) throws Exception {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
		
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		
		return writer.getBuffer().toString();
	}
}
