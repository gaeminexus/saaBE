package com.saa.ejb.signature.serviceImpl;

import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio de firma electrónica compatible con SRI Ecuador.
 * Genera firmas XAdES-BES con el mismo algoritmo de la referencia TypeScript (xades-firma-docs.ts).
 * 
 * Algoritmo clave: los digests de las referencias se calculan sobre las representaciones
 * en cadena de texto de los elementos (con namespaces añadidos en línea), NO sobre la
 * forma canónica C14N. Esto coincide con la validación del SRI.
 */
@Stateless
public class SignatureServiceImpl implements SignatureService {

	@EJB
	private FacturadorDaoService facturadorDaoService;

	@Override
	public String firmarXML(String xmlSinFirmar, String pathCertificado, String passwordCertificado) throws Exception {
		System.out.println("Iniciando proceso de firma electrónica XML");

		try {
			// --- 1. Preparar el documento (sin declaración XML) ---
			// Equivalente al TypeScript: documento = documento.replace('<?xml...?>', '')
			String documento = xmlSinFirmar;
			if (documento.startsWith("<?xml")) {
				int endDecl = documento.indexOf("?>") + 2;
				documento = documento.substring(endDecl);
				// Eliminar salto de línea inmediato después de la declaración si existe
				if (documento.startsWith("\n")) documento = documento.substring(1);
				else if (documento.startsWith("\r\n")) documento = documento.substring(2);
			}

			// Obtener nombre del elemento raíz (ej: "factura")
			String tagDoc = extraerTagRaiz(documento);

			// Eliminar trailing newline después del tag de cierre (como hace el TypeScript)
			documento = documento.replace("</" + tagDoc + ">\n", "</" + tagDoc + ">");
			documento = documento.replace("</" + tagDoc + ">\r\n", "</" + tagDoc + ">");

			// --- 2. Cargar certificado PKCS12 ---
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			FileInputStream fis = new FileInputStream(pathCertificado);
			keyStore.load(fis, passwordCertificado.toCharArray());
			fis.close();

			String alias = keyStore.aliases().nextElement();
			PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passwordCertificado.toCharArray());
			X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);

			// --- 3. Extraer datos del certificado ---
			String certificateX509 = getCertificateBase64(cert);
			String modulus = getModulusBase64(cert);
			String exponent = getExponentBase64(cert);
			String x509SerialNumber = cert.getSerialNumber().toString(10);
			String issuerName = getIssuerName(cert);
			// certificateX509DerHash: SHA1 de los bytes DER del certificado
			String certDigest = sha1Base64(cert.getEncoded());

			// --- 4. Generar IDs aleatorios ---
			String xmlns = "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:etsi=\"http://uri.etsi.org/01903/v1.3.2#\"";
			String CertificateNumber     = generarIdAleatorio();
			String SignatureNumber       = generarIdAleatorio();
			String SignedPropertiesNumber= generarIdAleatorio();
			String SignedInfoNumber      = generarIdAleatorio();
			String SignedPropertiesIDNumber = generarIdAleatorio();
			String ReferenceIDNumber     = generarIdAleatorio();
			String SignatureValueNumber  = generarIdAleatorio();
			String ObjectNumber          = generarIdAleatorio();

			// --- 5. SHA1 del documento (UTF-8) ---
			// Equivalente a sha1_base64_fact(documento) en TypeScript
			String sha1Factura = sha1Base64Utf8(documento);

			// --- 6. Construir SignedProperties ---
			String signingTime = java.time.ZonedDateTime.now().format(
				java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

			StringBuilder sp = new StringBuilder();
			sp.append("<etsi:SignedProperties Id=\"Signature").append(SignatureNumber)
			  .append("-SignedProperties").append(SignedPropertiesNumber).append("\">");
			sp.append("<etsi:SignedSignatureProperties>");
			sp.append("<etsi:SigningTime>").append(signingTime).append("</etsi:SigningTime>");
			sp.append("<etsi:SigningCertificate>");
			sp.append("<etsi:Cert>");
			sp.append("<etsi:CertDigest>");
			sp.append("<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\">");
			sp.append("</ds:DigestMethod>");
			sp.append("<ds:DigestValue>").append(certDigest).append("</ds:DigestValue>");
			sp.append("</etsi:CertDigest>");
			sp.append("<etsi:IssuerSerial>");
			sp.append("<ds:X509IssuerName>").append(issuerName).append("</ds:X509IssuerName>");
			sp.append("<ds:X509SerialNumber>").append(x509SerialNumber).append("</ds:X509SerialNumber>");
			sp.append("</etsi:IssuerSerial>");
			sp.append("</etsi:Cert>");
			sp.append("</etsi:SigningCertificate>");
			sp.append("</etsi:SignedSignatureProperties>");
			sp.append("<etsi:SignedDataObjectProperties>");
			sp.append("<etsi:DataObjectFormat ObjectReference=\"#Reference-ID-").append(ReferenceIDNumber).append("\">");
			sp.append("<etsi:Description>contenido comprobante</etsi:Description>");
			sp.append("<etsi:MimeType>text/xml</etsi:MimeType>");
			sp.append("</etsi:DataObjectFormat>");
			sp.append("</etsi:SignedDataObjectProperties>");
			sp.append("</etsi:SignedProperties>");

			String SignedProperties = sp.toString();
			// Añadir namespaces para calcular el hash (como hace el TypeScript)
			String SignedPropertiesParaHash = SignedProperties.replace("<etsi:SignedProperties", "<etsi:SignedProperties " + xmlns);
			String sha1SignedProperties = sha1Base64(SignedPropertiesParaHash.getBytes("UTF-8"));

			// --- 7. Construir KeyInfo ---
			StringBuilder ki = new StringBuilder();
			ki.append("<ds:KeyInfo Id=\"Certificate").append(CertificateNumber).append("\">");
			ki.append("\n<ds:X509Data>");
			ki.append("\n<ds:X509Certificate>\n");
			ki.append(certificateX509);
			ki.append("\n</ds:X509Certificate>");
			ki.append("\n</ds:X509Data>");
			ki.append("\n<ds:KeyValue>");
			ki.append("\n<ds:RSAKeyValue>");
			ki.append("\n<ds:Modulus>\n");
			ki.append(modulus);
			ki.append("\n</ds:Modulus>");
			ki.append("\n<ds:Exponent>");
			ki.append(exponent);
			ki.append("</ds:Exponent>");
			ki.append("\n</ds:RSAKeyValue>");
			ki.append("\n</ds:KeyValue>");
			ki.append("\n</ds:KeyInfo>");

			String KeyInfo = ki.toString();
			// Añadir namespaces para calcular el hash (como hace el TypeScript)
			String KeyInfoParaHash = KeyInfo.replace("<ds:KeyInfo", "<ds:KeyInfo " + xmlns);
			String sha1Certificado = sha1Base64(KeyInfoParaHash.getBytes("UTF-8"));

			// --- 8. Construir SignedInfo ---
			StringBuilder sinfo = new StringBuilder();
			sinfo.append("<ds:SignedInfo Id=\"Signature-SignedInfo").append(SignedInfoNumber).append("\">");
			sinfo.append("\n<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\">");
			sinfo.append("</ds:CanonicalizationMethod>");
			sinfo.append("\n<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\">");
			sinfo.append("</ds:SignatureMethod>");
			sinfo.append("\n<ds:Reference Id=\"SignedPropertiesID").append(SignedPropertiesIDNumber)
			     .append("\" Type=\"http://uri.etsi.org/01903#SignedProperties\" URI=\"#Signature")
			     .append(SignatureNumber).append("-SignedProperties").append(SignedPropertiesNumber).append("\">");
			sinfo.append("\n<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\">");
			sinfo.append("</ds:DigestMethod>");
			sinfo.append("\n<ds:DigestValue>").append(sha1SignedProperties).append("</ds:DigestValue>");
			sinfo.append("\n</ds:Reference>");
			sinfo.append("\n<ds:Reference URI=\"#Certificate").append(CertificateNumber).append("\">");
			sinfo.append("\n<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\">");
			sinfo.append("</ds:DigestMethod>");
			sinfo.append("\n<ds:DigestValue>").append(sha1Certificado).append("</ds:DigestValue>");
			sinfo.append("\n</ds:Reference>");
			sinfo.append("\n<ds:Reference Id=\"Reference-ID-").append(ReferenceIDNumber).append("\" URI=\"#comprobante\">");
			sinfo.append("\n<ds:Transforms>");
			sinfo.append("\n<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\">");
			sinfo.append("</ds:Transform>");
			sinfo.append("\n</ds:Transforms>");
			sinfo.append("\n<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\">");
			sinfo.append("</ds:DigestMethod>");
			sinfo.append("\n<ds:DigestValue>").append(sha1Factura).append("</ds:DigestValue>");
			sinfo.append("\n</ds:Reference>");
			sinfo.append("\n</ds:SignedInfo>");

			String SignedInfo = sinfo.toString();
			// Añadir namespaces para firmar (como hace el TypeScript)
			String SignedInfoParaFirma = SignedInfo.replace("<ds:SignedInfo", "<ds:SignedInfo " + xmlns);

			// --- 9. Firmar SignedInfo con RSA-SHA1 ---
			java.security.Signature sig = java.security.Signature.getInstance("SHA1withRSA");
			sig.initSign(privateKey);
			sig.update(SignedInfoParaFirma.getBytes("UTF-8"));
			byte[] signatureBytes = sig.sign();
			// Base64 con saltos cada 76 chars (igual que TypeScript: .match(/.{1,76}/g).join('\n'))
			String firmaSignedInfo = Base64.getMimeEncoder(76, new byte[]{'\n'}).encodeToString(signatureBytes);

			// --- 10. Ensamblar xadesBes ---
			StringBuilder xades = new StringBuilder();
			xades.append("<ds:Signature ").append(xmlns).append(" Id=\"Signature").append(SignatureNumber).append("\">");
			xades.append("\n").append(SignedInfo);
			xades.append("\n<ds:SignatureValue Id=\"SignatureValue").append(SignatureValueNumber).append("\">\n");
			xades.append(firmaSignedInfo);
			xades.append("\n</ds:SignatureValue>");
			xades.append("\n").append(KeyInfo);
			xades.append("\n<ds:Object Id=\"Signature").append(SignatureNumber).append("-Object").append(ObjectNumber).append("\">");
			xades.append("<etsi:QualifyingProperties Target=\"#Signature").append(SignatureNumber).append("\">");
			xades.append(SignedProperties);
			xades.append("</etsi:QualifyingProperties>");
			xades.append("</ds:Object>");
			xades.append("</ds:Signature>");

			// --- 11. Inyectar firma en el documento (antes del tag de cierre) ---
			String xmlFirmado = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
				+ documento.replace("</" + tagDoc + ">", xades.toString() + "</" + tagDoc + ">");

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

			System.out.println("Usando certificado en path: " + pathCertificado);
			return firmarXML(xmlSinFirmar, pathCertificado, passwordCertificado);

		} catch (Throwable e) {
			System.err.println("Error al firmar XML para facturador: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error al firmar XML: " + e.getMessage(), e);
		}
	}

	@Override
	public boolean verificarFirma(String xmlFirmado) {
		return false;
	}

	// -------------------------------------------------------------------------
	// Métodos auxiliares
	// -------------------------------------------------------------------------

	/**
	 * Extrae el nombre del elemento raíz del XML (ej: "factura" de "<factura id=...>")
	 */
	private String extraerTagRaiz(String xml) {
		int start = xml.indexOf('<');
		if (start < 0) return "factura";
		int spaceIdx = xml.indexOf(' ', start);
		int gtIdx    = xml.indexOf('>', start);
		int end = (spaceIdx > 0 && spaceIdx < gtIdx) ? spaceIdx : gtIdx;
		return xml.substring(start + 1, end);
	}

	/**
	 * SHA-1 de un array de bytes → Base64 estándar.
	 * Equivale a sha1_base64() del TypeScript.
	 */
	private String sha1Base64(byte[] data) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(data);
		return Base64.getEncoder().encodeToString(digest);
	}

	/**
	 * SHA-1 de una cadena en UTF-8 → Base64.
	 * Equivale a sha1_base64_fact() del TypeScript (con 'utf8').
	 */
	private String sha1Base64Utf8(String text) throws Exception {
		return sha1Base64(text.getBytes("UTF-8"));
	}

	/**
	 * Certifcado X.509 codificado en Base64 con líneas de 76 chars.
	 * Equivale al procesamiento PEM → base64 limpio del TypeScript.
	 */
	private String getCertificateBase64(X509Certificate cert) throws Exception {
		byte[] encoded = cert.getEncoded();
		String base64 = Base64.getEncoder().encodeToString(encoded);
		return base64.replaceAll("(.{76})", "$1\n");
	}

	/**
	 * Módulo RSA en Base64 con líneas de 76 chars.
	 * Equivale a bigint2base64(key.n) del TypeScript.
	 */
	private String getModulusBase64(X509Certificate cert) {
		RSAPublicKey rsaKey = (RSAPublicKey) cert.getPublicKey();
		BigInteger modulus = rsaKey.getModulus();
		String hex = modulus.toString(16);
		if (hex.length() % 2 != 0) hex = "0" + hex;
		byte[] bytes = hexToBytes(hex);
		String base64 = Base64.getEncoder().encodeToString(bytes);
		return base64.replaceAll("(.{76})", "$1\n");
	}

	/**
	 * Exponente público RSA en Base64.
	 * Equivale a hexToBase64(key.e.data[0].toString(16)) del TypeScript.
	 */
	private String getExponentBase64(X509Certificate cert) {
		RSAPublicKey rsaKey = (RSAPublicKey) cert.getPublicKey();
		BigInteger exp = rsaKey.getPublicExponent();
		String hex = exp.toString(16);
		if (hex.length() % 2 != 0) hex = "0" + hex;
		byte[] bytes = hexToBytes(hex);
		return Base64.getEncoder().encodeToString(bytes);
	}

	/**
	 * Nombre del emisor del certificado en el formato esperado por el SRI.
	 * Usa RFC2253 (orden inverso, más específico primero) y decodifica valores hex.
	 */
	private String getIssuerName(X509Certificate cert) {
		String issuerDN = cert.getIssuerX500Principal()
			.getName(javax.security.auth.x500.X500Principal.RFC2253);
		issuerDN = issuerDN.replaceAll(", ", ",");
		issuerDN = issuerDN.replaceAll("OID\\.", "");
		issuerDN = decodificarValoresHexadecimales(issuerDN);
		return issuerDN;
	}

	/**
	 * Genera un ID numérico aleatorio de 6 dígitos (como p_obtener_aleatorio() del TypeScript).
	 */
	private String generarIdAleatorio() {
		return String.valueOf((int)(Math.random() * 999000) + 990);
	}

	/**
	 * Convierte una cadena hexadecimal a array de bytes.
	 */
	private byte[] hexToBytes(String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
		}
		return bytes;
	}

	/**
	 * Decodifica valores hexadecimales en el Distinguished Name a texto legible.
	 * Ej: 2.5.4.97=#0c0f56415445532d413636373231343939 → 2.5.4.97=VATES-A66721499
	 */
	private String decodificarValoresHexadecimales(String dn) {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([^,=]+)=#([0-9a-fA-F]+)");
		java.util.regex.Matcher matcher = pattern.matcher(dn);
		StringBuffer resultado = new StringBuffer();
		while (matcher.find()) {
			String atributo = matcher.group(1);
			String valorHex = matcher.group(2);
			try {
				if (valorHex.length() > 4) {
					String datosHex = valorHex.substring(4);
					byte[] bytes = hexToBytes(datosHex.length() % 2 == 0 ? datosHex : "0" + datosHex);
					String valorLegible = new String(bytes, "UTF-8");
					matcher.appendReplacement(resultado, atributo + "=" + java.util.regex.Matcher.quoteReplacement(valorLegible));
				} else {
					matcher.appendReplacement(resultado, matcher.group(0));
				}
			} catch (Exception e) {
				matcher.appendReplacement(resultado, matcher.group(0));
			}
		}
		matcher.appendTail(resultado);
		return resultado.toString();
	}
}