package com.saa.ejb.cxc.serviceImpl;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.NodeList;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cxc.dao.RetencionDaoService;
import com.saa.ejb.cxc.dao.PathRetencionDaoService;
import com.saa.ejb.cxc.service.RetencionService;
import com.saa.ejb.cxc.service.EmailFacturaService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.DetalleRetencion;
import com.saa.model.cxc.Retencion;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathRetencion;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoAsientos;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
@Stateless
public class RetencionServiceImpl implements RetencionService {
	@EJB
	private RetencionDaoService retencionDaoService;

	@Resource
	private SessionContext sessionContext;

	/**
	 * Referencia al propio bean pasando por el contenedor, para que los
	 * @TransactionAttribute de las etapas se apliquen de verdad. Una llamada
	 * directa a this.metodo() se salta los interceptores y correría en la
	 * transacción del llamador.
	 * @return : Vista local de este mismo EJB
	 */
	private RetencionService self() {
		return sessionContext.getBusinessObject(RetencionService.class);
	}
	
	@EJB
	private PathRetencionDaoService pathRetencionDaoService;
	
	@EJB
	private SignatureService signatureService;

	@EJB
	private AsientoContableService asientoContableService;

	@EJB
	private EmailFacturaService emailFacturaService;
	
	@PersistenceContext
	private EntityManager em;
	
	@Override
	public Retencion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById Retencion con id: " + id);
		return retencionDaoService.selectById(id, NombreEntidadesCobro.RETENCION);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de RetencionService");
		Retencion entidad = new Retencion();
		for (Long registro : id) {
			retencionDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<Retencion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de RetencionService");
		for (Retencion registro : lista) {
			retencionDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<Retencion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll RetencionService");
		List<Retencion> result = retencionDaoService.selectAll(NombreEntidadesCobro.RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total Retencion no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public Retencion saveSingle(Retencion entidad) throws Throwable {
		System.out.println("saveSingle - Retencion");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			if (entidad.getPtoEmision() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la retención");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la retención");
			}
			String tipoComprobante = "07"; // Retención
			String tipoEmision = "1";
			
			// Obtener ambiente desde el facturador (BD) — fuente autoritativa por seguridad
			com.saa.model.cxc.Facturador facturadorDB = em.find(com.saa.model.cxc.Facturador.class, entidad.getFacturador().getId());
			Long ambiente;
			if (facturadorDB != null && facturadorDB.getAmbiente() != null) {
				ambiente = facturadorDB.getAmbiente();
				System.out.println("Ambiente tomado del facturador (BD): " + ambiente
						+ (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)"));
			} else {
				ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
				System.out.println("⚠ Facturador sin ambiente configurado en BD, usando valor recibido: " + ambiente);
			}
			entidad.setAmbiente(ambiente); // sincronizar el valor correcto en la entidad
			try {
				String secuencial = obtenerSecuencial(entidad.getPtoEmision().getId(), tipoComprobante);
				entidad.setSecuencial(secuencial);
				String numero = entidad.getNumEstablecimiento() + "-" + entidad.getNumPtoEmision() + "-" + secuencial;
				entidad.setNumero(numero);
				String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
				entidad.setClave(clave);
				entidad.setTipoComprobante(tipoComprobante);
				if (entidad.getEstadoEmision() == null) entidad.setEstadoEmision(1L);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la retención: " + e.getMessage());
			}
		}
		entidad = retencionDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<Retencion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria RetencionService");
		List<Retencion> result = retencionDaoService.selectByCriteria(datos, NombreEntidadesCobro.RETENCION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio Retencion no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public String[] generarXMLRetencion(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLRetencion con clave: " + clave + " y ambiente: " + ambiente);
		try {
			String sqlRetencion = "SELECT r FROM Retencion r WHERE r.clave = :clave";
			Query query = em.createQuery(sqlRetencion);
			query.setParameter("clave", clave);
			Retencion retencion = (Retencion) query.getSingleResult();
			if (retencion == null) throw new IncomeException("Retencion con clave " + clave + " no encontrada");
			
			Long idFacturador = retencion.getFacturador().getId();
			
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", retencion.getPtoEmision().getId());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();
			
			String sqlDetalle = "SELECT d FROM DetalleRetencion d WHERE d.retencion.id = :retencionId";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("retencionId", retencion.getId());
			@SuppressWarnings("unchecked")
			List<Object> detalles = queryDetalle.getResultList();
			
			String xmlContent = generarXMLContentRetencion(retencion, dirEstablecimiento, detalles, ambiente);
			
			String pathRelativo = "resources/" + idFacturador + "/rtnc/g/" + clave + ".xml";
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto = baseUploadDir + pathRelativo;
			
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			System.out.println("✓ XML Retencion generado correctamente en: " + pathAbsoluto);
			return new String[]{"OK", pathRelativo, pathAbsoluto};
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML Retencion: " + e.getMessage());
		}
	}
	
	private String generarXMLContentRetencion(Retencion retencion, String dirEstablecimiento,
			List<Object> detalles, Long ambiente) throws Exception {
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		// NO escribir declaración XML: el proceso de firma la agrega automáticamente
		
		writer.writeStartElement("comprobanteRetencion");
		writer.writeAttribute("id", "comprobante");  // SIEMPRE "comprobante" según estándar SRI
		writer.writeAttribute("version", "1.0.0");
		writer.writeCharacters("\n");
		
		writeInfoTributaria(writer, retencion, "07", ambiente);
		
		writer.writeCharacters("  ");
		writer.writeStartElement("infoCompRetencion");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", retencion.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		if (retencion.getFacturador().getContribuyenteEspecial() != null && 
				!retencion.getFacturador().getContribuyenteEspecial().isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", retencion.getFacturador().getContribuyenteEspecial(), 4);
		}
		String obligado = (retencion.getFacturador().getContabilidad() != null && 
				retencion.getFacturador().getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligado, 4);
		writeElement(writer, "tipoIdentificacionSujetoRetenido", String.valueOf(retencion.getProveedor().getRubroTipoIdentificacionH()), 4);
		writeElement(writer, "razonSocialSujetoRetenido", nvl(retencion.getProveedor().getNombre(), ""), 4);
		writeElement(writer, "identificacionSujetoRetenido", nvl(retencion.getProveedor().getIdentificacion(), ""), 4);
		writeElement(writer, "periodoFiscal", nvl(retencion.getPeriodoFiscal(), ""), 4);
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoCompRetencion
		writer.writeCharacters("\n");
		
		writeImpuestos(writer, detalles);
		writeInfoAdicional(writer, retencion);
		
		writer.writeEndElement(); // comprobanteRetencion
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeInfoTributaria(XMLStreamWriter writer, Retencion retencion,
			String tipoDoc, Long ambiente) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", "1", 4);
		writeElement(writer, "razonSocial", nvl(retencion.getFacturador().getRazonSocial(), ""), 4);
		writeElement(writer, "nombreComercial", nvl(retencion.getFacturador().getNombre(), ""), 4);
		writeElement(writer, "ruc", nvl(retencion.getFacturador().getNumDoc(), ""), 4);
		writeElement(writer, "claveAcceso", nvl(retencion.getClave(), ""), 4);
		writeElement(writer, "codDoc", tipoDoc, 4);
		writeElement(writer, "estab", nvl(retencion.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi", nvl(retencion.getNumPtoEmision(), ""), 4);
		writeElement(writer, "secuencial", nvl(retencion.getSecuencial(), ""), 4);
		writeElement(writer, "dirMatriz", nvl(retencion.getFacturador().getDireccion(), ""), 4);
		if (retencion.getFacturador().getMicroEmpresa() != null && retencion.getFacturador().getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (retencion.getFacturador().getAgenteRetencion() != null && !retencion.getFacturador().getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", retencion.getFacturador().getAgenteRetencion(), 4);
		}
		if (retencion.getFacturador().getRimpe() != null && retencion.getFacturador().getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (retencion.getFacturador().getPopularRimpe() != null && retencion.getFacturador().getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeImpuestos(XMLStreamWriter writer, List<Object> detallesRaw) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		for (Object obj : detallesRaw) {
			com.saa.model.cxc.DetalleRetencion det = (com.saa.model.cxc.DetalleRetencion) obj;

			writer.writeCharacters("    ");
			writer.writeStartElement("impuesto");
			writer.writeCharacters("\n");

			writeElement(writer, "codigo",           nvl(det.getCodImpuesto(),  ""), 6);
			writeElement(writer, "codigoRetencion",  nvl(det.getCodRetencion(), ""), 6);
			writeElement(writer, "baseImponible",    det.getBaseImponible()  != null
					? String.format(java.util.Locale.US, "%.2f", det.getBaseImponible()) : "0.00", 6);
			writeElement(writer, "porcentajeRetener", det.getPorcentajeReten() != null
					? String.format(java.util.Locale.US, "%.2f", det.getPorcentajeReten()) : "0.00", 6);
			writeElement(writer, "valorRetenido",    det.getValorReten() != null
					? String.format(java.util.Locale.US, "%.2f", det.getValorReten()) : "0.00", 6);
			writeElement(writer, "codDocSustento",   nvl(det.getTipoDocReten(), ""), 6);
			writeElement(writer, "numDocSustento",   nvl(det.getNumDocReten(),  ""), 6);
			// fechaEmiDoc puede ser null si el doc sustento no tiene fecha
			String fechaDoc = "";
			if (det.getFechaEmiDoc() != null) {
				fechaDoc = det.getFechaEmiDoc().format(dateFormatter);
			}
			writeElement(writer, "fechaEmisionDocSustento", fechaDoc, 6);

			writer.writeCharacters("    ");
			writer.writeEndElement(); // impuesto
			writer.writeCharacters("\n");
		}

		writer.writeCharacters("  ");
		writer.writeEndElement(); // impuestos
		writer.writeCharacters("\n");
	}

	private void writeInfoAdicional(XMLStreamWriter writer, Retencion retencion) throws Exception {
		// Recuperar datos del facturador para infoAdicional (igual que el PHP de referencia)
		String telefonoFcdr = retencion.getFacturador() != null
				? nvl(retencion.getFacturador().getTelefono(), "") : "";
		String mailFcdr = retencion.getFacturador() != null
				? nvl(retencion.getFacturador().getMail(), "") : "";
		String telefonoProveedor = retencion.getProveedor() != null
				? nvl(retencion.getProveedor().getTelefono(), "") : "";
		String mailProveedor = retencion.getProveedor() != null
				? nvl(retencion.getProveedor().getEmail(), "") : "";
		String observacion = nvl(retencion.getObservacion(), "");

		String infoAdicionalTexto = "Soporte[" + telefonoFcdr + " - " + mailFcdr + "] "
				+ "Contacto Proveedor[" + telefonoProveedor + " - " + mailProveedor + "] "
				+ "Observacion[" + observacion + "]";

		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters(infoAdicionalTexto);
		writer.writeEndElement();
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
		writer.writeCharacters("  ".repeat(indent / 2));
		writer.writeStartElement(name);
		writer.writeCharacters(value);
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private String nvl(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}

	@Override
	public java.util.Map<String, Object> reenviarEmail(Long idRetencion, String destinatarios) throws Throwable {
		System.out.println("=== reenviarEmail RET | id=" + idRetencion + " | destinatarios=" + destinatarios + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		if (destinatarios == null || destinatarios.trim().isEmpty()) {
			resultado.put("mensaje", "Debe especificar al menos un correo electrónico destinatario.");
			return resultado;
		}

		Retencion retencion = retencionDaoService.selectById(idRetencion, NombreEntidadesCobro.RETENCION);
		if (retencion == null) {
			resultado.put("mensaje", "No se encontró la retención con ID: " + idRetencion);
			return resultado;
		}
		if (!Long.valueOf(5L).equals(retencion.getEstado())) {
			resultado.put("mensaje", "Solo se puede reenviar el email de retenciones autorizadas. Estado actual: " + retencion.getEstado());
			return resultado;
		}

		String clave       = retencion.getClave();
		Long idFacturador  = retencion.getFacturador().getId();
		String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;

		// Leer XML autorizado
		String xmlAutorizado = null;
		try {
			java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/rtnc/a/" + clave + ".xml");
			if (java.nio.file.Files.exists(pXml))
				xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
		} catch (Exception e) { System.err.println("⚠ Error leyendo XML RET: " + e.getMessage()); }

		// No existe JRXML para retención simple → email se envía solo con XML
		System.out.println("ℹ Retención simple: el email se envía con XML adjunto (sin PDF — no existe JRXML para retención simple).");

		String razonSocial = retencion.getFacturador() != null
				? nvl(retencion.getFacturador().getRazonSocial(), nvl(retencion.getFacturador().getNombre(), "")) : "";
		String numeroDoc = nvl(retencion.getNumero(), clave);

		String[] lista = destinatarios.split(";");
		java.util.List<String> enviados = new java.util.ArrayList<>();
		java.util.List<String> fallidos = new java.util.ArrayList<>();
		for (String mail : lista) {
			String m = mail.trim();
			if (m.isEmpty()) continue;
			try {
				emailFacturaService.enviarFacturaAutorizada(m, numeroDoc, clave, razonSocial, "Retención", xmlAutorizado, null);
				enviados.add(m);
			} catch (Exception e) { fallidos.add(m + " (" + e.getMessage() + ")"); }
		}

		resultado.put("emailsEnviados", enviados);
		resultado.put("emailsFallidos", fallidos);
		resultado.put("clave", clave);
		if (!enviados.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado a " + enviados.size() + " destinatario(s).");
		} else {
			resultado.put("mensaje", "No se pudo enviar el email a ningún destinatario.");
		}
		return resultado;
	}
	
	@SuppressWarnings("unused")
	private String formatDecimal(Double value) {
		if (value == null) return "0.00";
		return String.format(java.util.Locale.US, "%.2f", value);
	}
	
	@Override
	public String autorizarRetencion(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarRetencion con clave: " + clave);
		String respuesta = "";
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/rtnc/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
			PathRetencion pathF = new PathRetencion();
			Retencion retencion = retencionDaoService.selectById(codigoRetencion, NombreEntidadesCobro.RETENCION);
			pathF.setRetencion(retencion);
			pathF.setPath("resources/" + idFacturador + "/rtnc/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathRetencionDaoService.save(pathF, null);
			
			retencion.setEstado(3L);
			retencionDaoService.save(retencion, retencion.getId());
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				try {
					Path logWS1 = Paths.get(resourcesPath + "/rtnc/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					// Leer bytes crudos del XML firmado (NO convertir a String, preserva la firma)
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/rtnc/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
					PathRetencion pathE = new PathRetencion();
					pathE.setRetencion(retencion);
					pathE.setPath("resources/" + idFacturador + "/rtnc/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathRetencionDaoService.save(pathE, null);
					
					retencion.setEstado(4L);
					retencionDaoService.save(retencion, retencion.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						Thread.sleep(2000);
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							if ("AUTORIZADO".equals(resultado.estado)) {
								Path logWS2A = Paths.get(resourcesPath + "/rtnc/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								Path pathAutorizado = Paths.get(resourcesPath + "/rtnc/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								PathRetencion pathA = new PathRetencion();
								pathA.setRetencion(retencion);
								pathA.setPath("resources/" + idFacturador + "/rtnc/a/" + clave + ".xml");
								pathA.setAlterno(5L);
								pathRetencionDaoService.save(pathA, null);
								
								retencion.setEstado(5L);
								retencion.setEstadoEmision(1L);
								retencion.setAutorizacion(resultado.numeroAutorizacion);
								retencion.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								retencionDaoService.save(retencion, retencion.getId());
								respuesta = resultado.estado;
								
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
							} else {
								Path logWS2N = Paths.get(resourcesPath + "/rtnc/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								if (resultado.comprobanteXML != null) {
									Path pathNoAutorizado = Paths.get(resourcesPath + "/rtnc/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									PathRetencion pathN = new PathRetencion();
									pathN.setRetencion(retencion);
									pathN.setPath("resources/" + idFacturador + "/rtnc/n/" + clave + ".xml");
									pathN.setAlterno(6L);
									pathRetencionDaoService.save(pathN, null);
								}
								retencion.setEstado(6L);
								retencion.setEstadoEmision(2L);
								retencionDaoService.save(retencion, retencion.getId());
								respuesta = "Estado: " + resultado.estado + " Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") + " / " + nvl(resultado.informacionAdicional, "");
							}
						} catch (Exception e) {
							Path logWS2Error = Paths.get(resourcesPath + "/rtnc/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/rtnc/n/" + clave + ".xml"));
							retencion.setEstado(6L);
							retencion.setEstadoEmision(2L);
							retencionDaoService.save(retencion, retencion.getId());
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
					} else {
						respuesta = "Estado: " + estadoRecepcion;
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							retencion.setAutorizacion(clave);
							retencion.setFechaAutorizacion(retencion.getFecha().plusMinutes(1).plusSeconds(15));
							retencion.setEstado(5L);
							retencionDaoService.save(retencion, retencion.getId());
						}
					}
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
			} else {
				respuesta = "Retencion Generada pero no enviada";
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarRetencion: " + e.getMessage());
		}
		return respuesta;
	}
	
	/**
	 * Orquesta el proceso completo SIN transacción propia (NOT_SUPPORTED).
	 * <p>
	 * El envío al SRI es irreversible, así que la emisión se confirma en su
	 * propia transacción y el asiento contable corre aparte: un fallo tardío
	 * NUNCA puede reversar una retención ya autorizada por el SRI.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> procesarRetencionCompleta(Retencion retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {

		System.out.println("=== INICIANDO PROCESO COMPLETO DE RETENCIÓN ===");

		// ── Emisión ante el SRI, en UNA transacción propia ────────────────────
		java.util.Map<String, Object> resultado = self().emitirRetencionAnteSRI(
				retencion, detalles, ambiente, conectaSRI, destinatario, pathLogo);

		if (!Boolean.TRUE.equals(resultado.get("emitida"))) {
			return resultado;
		}

		Long idRetencion = (Long) resultado.get("idRetencion");

		// ── PASO 5: Generar asiento contable (transacción propia) ─────────────
		System.out.println("PASO 5: Generando asiento contable de retención...");
		try {
			java.util.Map<String, Object> resAsiento = self().generarContabilidadRetencion(idRetencion);
			if (Boolean.TRUE.equals(resAsiento.get("aplica"))) {
				resultado.put("asiento", resAsiento.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"La retención fue autorizada pero ocurrió un error al generar el asiento contable: "
					+ e.getMessage() + ". Genere el asiento manualmente desde Contabilidad.");
			System.err.println("⚠ Error en asiento contable de Retención: " + e.getMessage());
			e.printStackTrace();
		}

		boolean hayPendientes = Boolean.TRUE.equals(resultado.get("contabilidadPendiente"));
		resultado.put("exito", true);
		resultado.put("etapa", hayPendientes ? "COMPLETADO_CON_PENDIENTES" : "COMPLETADO");
		resultado.put("mensaje", hayPendientes
				? "Retención autorizada por el SRI, pero quedaron etapas pendientes. Revise las advertencias."
				: "Retención procesada y autorizada exitosamente.");
		System.out.println("=== PROCESO COMPLETO DE RETENCIÓN FINALIZADO"
				+ (hayPendientes ? " (CON PENDIENTES)" : "") + " ===");
		return resultado;
	}

	/**
	 * Emite la retención ante el SRI en UNA transacción propia (REQUIRES_NEW):
	 * valida cuentas, prepara campos, genera y firma el XML, envía a recepción
	 * y —sólo si el SRI la acepta— graba el documento, persiste la autorización
	 * y envía el email.
	 * @return : Mapa con clave, idRetencion y emitida=true si el SRI la autorizó
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> emitirRetencionAnteSRI(Retencion retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {

		System.out.println("=== emitirRetencionAnteSRI (BD tras RECIBIDA) ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// ── PASO 0: Validar configuración contable ANTES de grabar ─────────────
		if (retencion.getFacturador() != null
				&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {

			if (retencion.getFacturador().getEmpresa() == null) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
						+ "pero no tiene empresa contable configurada. "
						+ "Configure el campo EMPRESA en el facturador.");
				return resultado;
			}

			Long idEmpresa = retencion.getFacturador().getEmpresa().getCodigo();
			System.out.println("PASO 0: Validando cuentas contables para empresa " + idEmpresa + "...");

			java.util.List<String> erroresContables =
					asientoContableService.validarCuentasContablesRetencion(retencion, detalles, idEmpresa);

			if (!erroresContables.isEmpty()) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "No se puede emitir la retención: faltan cuentas contables. "
						+ "Corrija los siguientes problemas antes de continuar:");
				resultado.put("erroresContables", erroresContables);
				StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
				for (int i = 0; i < erroresContables.size(); i++) {
					sb.append("  ").append(i + 1).append(". ").append(erroresContables.get(i)).append("\n");
				}
				resultado.put("error", sb.toString());
				System.err.println("✗ Validación contable fallida:\n" + sb);
				return resultado;
			}
			System.out.println("✓ Validación contable OK.");
		}

		try {
			if (ambiente  == null) ambiente  = 1L;
			if (conectaSRI == null) conectaSRI = 1L;
			if (pathLogo  == null) pathLogo  = "resources/logos/logo_aso.png";
			if (destinatario == null && retencion.getProveedor() != null)
				destinatario = retencion.getProveedor().getEmail();

			// ── PASO 1: Preparar campos en MEMORIA (sin guardar en BD) ──────────
			System.out.println("PASO 1: Preparando campos de la retención en memoria...");
			if (retencion.getEstado() == null) retencion.setEstado(Long.valueOf(Estado.ACTIVO));
			if (retencion.getPtoEmision() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un punto de emisión.");
				return resultado;
			}
			if (retencion.getFacturador() == null || retencion.getFacturador().getId() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un facturador.");
				return resultado;
			}

			String tipoComprobante = "07";
			String tipoEmision = "1";
			com.saa.model.cxc.Facturador facturadorDB = em.find(com.saa.model.cxc.Facturador.class, retencion.getFacturador().getId());
			Long ambienteFacturador;
			if (facturadorDB != null && facturadorDB.getAmbiente() != null) {
				ambienteFacturador = facturadorDB.getAmbiente();
			} else {
				ambienteFacturador = retencion.getAmbiente() != null ? retencion.getAmbiente() : 1L;
			}
			retencion.setAmbiente(ambienteFacturador);
			ambiente = ambienteFacturador;
			System.out.println(">>> AMBIENTE: " + ambiente + (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)") + " | CONECTA_SRI: " + conectaSRI);

			try {
				String secuencial = obtenerSecuencial(retencion.getPtoEmision().getId(), tipoComprobante);
				retencion.setSecuencial(secuencial);
				String numero = retencion.getNumEstablecimiento() + "-" + retencion.getNumPtoEmision() + "-" + secuencial;
				retencion.setNumero(numero);
				String clave = generarClaveAcceso(retencion, tipoComprobante, ambienteFacturador, tipoEmision, secuencial);
				retencion.setClave(clave);
				retencion.setTipoComprobante(tipoComprobante);
				if (retencion.getEstadoEmision() == null) retencion.setEstadoEmision(1L);
				System.out.println("✓ Campos preparados en memoria. Clave: " + clave + " | Número: " + numero);
			} catch (Exception e) {
				resultado.put("etapa", "PREPARACION_CAMPOS");
				resultado.put("mensaje", "Error al preparar campos de la retención: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			String clave = retencion.getClave();
			if (clave == null || clave.isEmpty()) throw new Exception("La retención no tiene clave de acceso");
			Long idFacturador = retencion.getFacturador().getId();
			resultado.put("claveAcceso", clave);

			// ── PASO 2-3: Generar y firmar XML ──────────────────────────────────
			// El XML se genera con los datos en memoria (retención + detalles no persistidos aún)
			String xmlFirmado;
			try {
				System.out.println("PASO 2: Generando XML de retención en memoria...");
				// Para generar el XML necesitamos que la retención tenga un ID de PtoEmision
				// Usamos el método generarXMLRetencion que ya consulta desde BD los datos del
				// establecimiento; pero la retención aún no está en BD, así que generamos el
				// XML directamente con los datos en memoria.
				String dirEstablecimiento = "";
				try {
					String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
					Query queryEstab = em.createQuery(sqlEstab);
					queryEstab.setParameter("ptoEmisionId", retencion.getPtoEmision().getId());
					dirEstablecimiento = (String) queryEstab.getSingleResult();
				} catch (Exception e) {
					System.err.println("⚠ No se pudo obtener dirección del establecimiento: " + e.getMessage());
				}
				String xmlContent = generarXMLContentRetencion(retencion, dirEstablecimiento,
						new java.util.ArrayList<>(detalles != null ? detalles : java.util.Collections.emptyList()), ambiente);
				String pathRelativo = "resources/" + idFacturador + "/rtnc/g/" + clave + ".xml";
				String pathAbsoluto = getBaseUploadDirectory() + pathRelativo;
				Path path = Paths.get(pathAbsoluto);
				Files.createDirectories(path.getParent());
				Files.write(path, xmlContent.getBytes("UTF-8"));
				System.out.println("PASO 3: Firmando XML...");
				xmlFirmado = signatureService.firmarXMLFacturador(xmlContent, idFacturador);
				System.out.println("✓ XML generado y firmado.");
			} catch (Exception e) {
				resultado.put("etapa", "GENERACION_XML");
				resultado.put("mensaje", "Error al generar o firmar el XML de la retención: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// ── PASO 4a: Enviar al SRI (WS1 - Recepción) ───────────────────────
			System.out.println("PASO 4a: Enviando XML al SRI (WS1 - Recepción)...");
			String estadoRecepcion = "NO_ENVIADO";
			if (conectaSRI == 1) {
				String baseUploadDir = getBaseUploadDirectory();
				String resourcesPath = baseUploadDir + "resources/" + idFacturador;
				try {
					Path pathFirmado = Paths.get(resourcesPath + "/rtnc/f/" + clave + ".xml");
					Files.createDirectories(pathFirmado.getParent());
					Files.write(pathFirmado, xmlFirmado.getBytes("UTF-8"));

					String urlWS1 = ambiente == 1
							? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
							: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
					Path logWS1 = Paths.get(resourcesPath + "/rtnc/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					logWriter1.close();
					System.out.println(">>> Estado WS1 Recepción: [" + estadoRecepcion + "]");
				} catch (Exception e) {
					resultado.put("etapa", "WS1_RECEPCION");
					resultado.put("mensaje", "Error al comunicarse con el SRI (WS1): " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			} else {
				estadoRecepcion = "RECIBIDA"; // simulado cuando no se conecta al SRI
				System.out.println("ℹ conectaSRI=0 — simulando RECIBIDA para guardar en BD.");
			}

			if (!"RECIBIDA".equals(estadoRecepcion)
					&& !(estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA"))) {
				resultado.put("etapa", "WS1_RECEPCION");
				resultado.put("exito", false);
				resultado.put("estado", estadoRecepcion);
				resultado.put("mensaje", "El SRI no aceptó el comprobante. Estado WS1: " + estadoRecepcion);
				return resultado;
			}

			// ── PASO 4b: WS1=RECIBIDA → Guardar en BD ───────────────────────────
			System.out.println("PASO 4b: SRI respondió RECIBIDA. Guardando retención en base de datos...");
			try {
				retencion = retencionDaoService.save(retencion, null);
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_RETENCION");
				resultado.put("mensaje", "Error al grabar la retención: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}
			resultado.put("retencion", retencion);
			resultado.put("idRetencion", retencion.getId());
			System.out.println("✓ Retención grabada ID: " + retencion.getId() + " | Clave: " + retencion.getClave());

			// Guardar detalles
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 4c: Guardando " + detalles.size() + " detalles de retención...");
				try {
					for (DetalleRetencion detalle : detalles) {
						detalle.setRetencion(retencion);
						if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(Estado.ACTIVO));
						em.persist(detalle);
					}
					em.flush();
					System.out.println("✓ Detalles guardados.");
				} catch (Exception e) {
					resultado.put("etapa", "GRABADO_DETALLES");
					resultado.put("mensaje", "Error al grabar los detalles: " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			}

			// Registrar paths y estado FIRMADA/ENVIADA en BD
			try {
				String baseUploadDir = getBaseUploadDirectory();
				String resourcesPath = baseUploadDir + "resources/" + idFacturador;
				PathRetencion pathF = new PathRetencion();
				pathF.setRetencion(retencion);
				pathF.setPath("resources/" + idFacturador + "/rtnc/f/" + clave + ".xml");
				pathF.setAlterno(3L);
				pathRetencionDaoService.save(pathF, null);
				retencion.setEstado(3L);
				retencionDaoService.save(retencion, retencion.getId());
				if (conectaSRI == 1) {
					Path pathEnviado = Paths.get(resourcesPath + "/rtnc/e/" + clave + ".xml");
					byte[] bytesXMLFirmado = Files.readAllBytes(Paths.get(resourcesPath + "/rtnc/f/" + clave + ".xml"));
					Files.write(pathEnviado, bytesXMLFirmado);
					PathRetencion pathE = new PathRetencion();
					pathE.setRetencion(retencion);
					pathE.setPath("resources/" + idFacturador + "/rtnc/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathRetencionDaoService.save(pathE, null);
					retencion.setEstado(4L);
					retencionDaoService.save(retencion, retencion.getId());
				}
			} catch (Exception e) {
				System.err.println("⚠ Error registrando paths (no crítico): " + e.getMessage());
			}

			// ── PASO 4c: Si era CLAVE ACCESO REGISTRADA, marcar directamente ────
			if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
				retencion.setAutorizacion(clave);
				retencion.setFechaAutorizacion(retencion.getFecha().plusMinutes(1).plusSeconds(15));
				retencion.setEstado(5L);
				retencionDaoService.save(retencion, retencion.getId());
				resultado.put("estado", "AUTORIZADO");
				resultado.put("exito", true);
				resultado.put("etapa", "COMPLETADO");
				resultado.put("mensaje", "Retención ya registrada en el SRI. Autorizada.");
				return resultado;
			}

			// ── PASO 4d: WS2 - Autorización ─────────────────────────────────────
			System.out.println("PASO 4d: Consultando autorización al SRI (WS2)...");
			Thread.sleep(2000);
			String resultadoAutorizacion = "";
			boolean autorizada = false;
			if (conectaSRI == 1) {
				try {
					String baseUploadDir = getBaseUploadDirectory();
					String resourcesPath = baseUploadDir + "resources/" + idFacturador;
					String urlWS2 = ambiente == 1
							? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
							: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
					ResultadoAutorizacion ra = llamarAutorizacionSRI(urlWS2, clave);
					System.out.println(">>> Estado WS2: [" + ra.estado + "]");

					if ("AUTORIZADO".equals(ra.estado)) {
						Path logWS2A = Paths.get(resourcesPath + "/rtnc/a/" + clave + ".txt");
						Files.createDirectories(logWS2A.getParent());
						PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
						logWriter2.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2.close();
						Path pathAutorizado = Paths.get(resourcesPath + "/rtnc/a/" + clave + ".xml");
						Files.write(pathAutorizado, ra.comprobanteXML.getBytes("UTF-8"));
						PathRetencion pathA = new PathRetencion();
						pathA.setRetencion(retencion);
						pathA.setPath("resources/" + idFacturador + "/rtnc/a/" + clave + ".xml");
						pathA.setAlterno(5L);
						pathRetencionDaoService.save(pathA, null);
						retencion.setEstado(5L);
						retencion.setEstadoEmision(1L);
						retencion.setAutorizacion(ra.numeroAutorizacion);
						retencion.setFechaAutorizacion(parseFechaAutorizacion(ra.fechaAutorizacion));
						retencionDaoService.save(retencion, retencion.getId());
						resultadoAutorizacion = ra.estado;
						autorizada = true;
						if (ambiente == 2) {
							em.createQuery("UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos,0)+1 WHERE f.id = :id")
								.setParameter("id", idFacturador).executeUpdate();
						}
					} else {
						Path logWS2N = Paths.get(resourcesPath + "/rtnc/n/" + clave + ".txt");
						Files.createDirectories(logWS2N.getParent());
						PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
						logWriter2N.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2N.close();
						if (ra.comprobanteXML != null) {
							Files.write(Paths.get(resourcesPath + "/rtnc/n/" + clave + ".xml"), ra.comprobanteXML.getBytes("UTF-8"));
							PathRetencion pathN = new PathRetencion();
							pathN.setRetencion(retencion);
							pathN.setPath("resources/" + idFacturador + "/rtnc/n/" + clave + ".xml");
							pathN.setAlterno(6L);
							pathRetencionDaoService.save(pathN, null);
						}
						retencion.setEstado(6L);
						retencion.setEstadoEmision(2L);
						retencionDaoService.save(retencion, retencion.getId());
						resultadoAutorizacion = "Estado: " + ra.estado
								+ " Id: " + nvl(ra.mensajeId, "")
								+ " Mensaje: " + nvl(ra.mensaje, "")
								+ " / " + nvl(ra.informacionAdicional, "");
					}
				} catch (Exception e) {
					resultado.put("etapa", "WS2_AUTORIZACION");
					resultado.put("mensaje", "Error al consultar autorización al SRI (WS2): " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			} else {
				autorizada = true;
				resultadoAutorizacion = "AUTORIZADO";
				retencion.setEstado(5L);
				retencionDaoService.save(retencion, retencion.getId());
			}

			resultado.put("autorizacion", resultadoAutorizacion);

			if (!autorizada) {
				resultado.put("etapa", "WS2_AUTORIZACION");
				resultado.put("exito", false);
				resultado.put("estado", "NO_AUTORIZADO");
				resultado.put("mensaje", "La retención fue recibida por el SRI pero no fue autorizada. "
						+ "Respuesta del SRI: " + resultadoAutorizacion);
				return resultado;
			}

			System.out.println("✓ Retención AUTORIZADA por el SRI.");
			resultado.put("estado", "AUTORIZADO");

			// El asiento contable lo genera el orquestador fuera de esta
			// transacción, ya con la retención confirmada en BD.

			// ── PASO 6: Enviar correo electrónico ─────────────────────────────
			System.out.println("PASO 6: Enviando email al proveedor...");
			try {
				if (destinatario != null && !destinatario.trim().isEmpty()) {
					String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
					String xmlAutorizado = null;
					byte[] pdfBytes = null;
					try {
						java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/rtnc/a/" + clave + ".xml");
						if (java.nio.file.Files.exists(pXml))
							xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
					} catch (Exception ioEx) {
						System.err.println("⚠ No se pudieron leer archivos para el email: " + ioEx.getMessage());
					}
					String razonSocial = retencion.getFacturador() != null
							? nvl(retencion.getFacturador().getRazonSocial(), nvl(retencion.getFacturador().getNombre(), "")) : "";
					emailFacturaService.enviarFacturaAutorizada(destinatario, nvl(retencion.getNumero(), clave),
							clave, razonSocial, "Retención", xmlAutorizado, pdfBytes);
					resultado.put("emailEnviado", true);
					System.out.println("✓ Email enviado a: " + destinatario);
				} else {
					resultado.put("emailEnviado", false);
					System.out.println("ℹ Email omitido: no hay dirección de correo del proveedor.");
				}
			} catch (Exception mailEx) {
				resultado.put("advertenciaEmail", "La retención fue autorizada pero no se pudo enviar el email: "
						+ mailEx.getMessage() + ". Reenvíe el email manualmente.");
				System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
			}

			// Emisión terminada: la retención está autorizada y confirmada en BD.
			resultado.put("emitida", true);

		} catch (Exception e) {
			System.err.println("ERROR inesperado en emitirRetencionAnteSRI: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("etapa", "ERROR_INESPERADO");
			resultado.put("error", e.getMessage());
			resultado.put("mensaje", "Error inesperado al procesar la retención: " + e.getMessage());
			sessionContext.setRollbackOnly();
			throw e;
		}

		return resultado;
	}

	/**
	 * Marca la retención como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, número y fecha de autorización, y XML autorizado
	 * en disco. Idempotente.
	 * @param idRetencion        : Id de la retención
	 * @param numeroAutorizacion : Número de autorización devuelto por el SRI
	 * @param fechaAutorizacion  : Fecha de autorización devuelta por el SRI
	 * @param comprobanteXML     : XML autorizado (puede ser null)
	 * @return : true si actualizó el estado, false si ya estaba autorizada
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean marcarRetencionAutorizada(Long idRetencion, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable {
		System.out.println("Ingresa al metodo marcarRetencionAutorizada con id: " + idRetencion);

		Retencion retencion = em.find(Retencion.class, idRetencion);
		if (retencion == null) {
			throw new IncomeException("Retención con ID " + idRetencion + " no encontrada.");
		}
		if (Long.valueOf(5L).equals(retencion.getEstado())) {
			System.out.println("ℹ Retención ya estaba en estado 5 (autorizada).");
			return false;
		}

		retencion.setEstado(5L);
		retencion.setEstadoEmision(1L);
		if (numeroAutorizacion != null && !numeroAutorizacion.isEmpty()) {
			retencion.setAutorizacion(numeroAutorizacion);
		}
		if (fechaAutorizacion != null && !fechaAutorizacion.isEmpty()) {
			retencion.setFechaAutorizacion(parseFechaAutorizacion(fechaAutorizacion));
		}

		Long idFacturador = retencion.getFacturador() != null ? retencion.getFacturador().getId() : null;
		if (comprobanteXML != null && !comprobanteXML.isEmpty() && idFacturador != null) {
			try {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				java.nio.file.Path pathAutorizado = java.nio.file.Paths.get(
						resourcesPath + "/docs/a/" + retencion.getClave() + ".xml");
				java.nio.file.Files.createDirectories(pathAutorizado.getParent());
				java.nio.file.Files.write(pathAutorizado, comprobanteXML.getBytes("UTF-8"));
				PathRetencion pathA = new PathRetencion();
				pathA.setRetencion(retencion);
				pathA.setPath("resources/" + idFacturador + "/docs/a/" + retencion.getClave() + ".xml");
				pathA.setAlterno(5L);
				pathRetencionDaoService.save(pathA, null);
				System.out.println("✓ XML autorizado guardado en disco.");
			} catch (Exception xmlEx) {
				System.err.println("⚠ Error guardando XML autorizado: " + xmlEx.getMessage());
			}
		}

		retencionDaoService.save(retencion, retencion.getId());
		em.flush();
		System.out.println("✓ Retención actualizada a estado AUTORIZADA (5). Aut: " + numeroAutorizacion);
		return true;
	}

	/**
	 * Genera y vincula el asiento contable de una retención en transacción
	 * propia (REQUIRES_NEW). Idempotente: si ya tiene asiento no genera otro.
	 * @param idRetencion : Id de la retención ya autorizada
	 * @return : Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> generarContabilidadRetencion(Long idRetencion) throws Throwable {
		System.out.println("Ingresa al metodo generarContabilidadRetencion con id: " + idRetencion);

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("generado", false);
		resultado.put("aplica", false);

		Retencion retencion = em.find(Retencion.class, idRetencion);
		if (retencion == null) {
			throw new IncomeException("Retención con ID " + idRetencion + " no encontrada.");
		}
		if (retencion.getFacturador() == null
				|| retencion.getFacturador().getEmpresa() == null
				|| !Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {
			System.out.println("ℹ El facturador no genera contabilidad: se omite el asiento.");
			return resultado;
		}
		resultado.put("aplica", true);

		if (retencion.getAsiento() != null) {
			resultado.put("yaExistia", true);
			resultado.put("idAsiento", retencion.getAsiento().getCodigo());
			resultado.put("numeroAlterno", retencion.getAsiento().getNumeroAlterno());
			System.out.println("ℹ La retención ya tiene asiento: " + retencion.getAsiento().getNumeroAlterno());
			return resultado;
		}

		// Etapa atómica: se marca el rollback a mano porque IncomeException es
		// una application exception y por sí sola no reversaría esta transacción.
		try {
			Long idEmpresa = retencion.getFacturador().getEmpresa().getCodigo();
			java.time.LocalDate fechaAsiento = retencion.getFecha() != null
					? retencion.getFecha().toLocalDate() : java.time.LocalDate.now();
			String obsAsiento = "Retención N° " + nvl(retencion.getNumero(), retencion.getClave())
					+ " | Proveedor: " + (retencion.getProveedor() != null ? retencion.getProveedor().getNombre() : "")
					+ " | " + nvl(retencion.getObservacion(), "");
			String usuarioAsiento = retencion.getUsuario() != null
					? retencion.getUsuario().getNombre() : "SISTEMA";

			com.saa.model.cnt.Asiento asientoGenerado =
					asientoContableService.generarAsientoRetencion(
							retencion.getId(), idEmpresa,
							TipoAsientos.RETENCIONES_EMITIDAS,
							fechaAsiento, obsAsiento, usuarioAsiento);

			// Vincular el asiento a la retención — antes no se hacía, y por eso
			// la anulación no encontraba el asiento que debía anular.
			com.saa.model.cnt.Asiento asientoAttached =
					em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
			if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
			retencion.setAsiento(asientoAttached);
			retencionDaoService.save(retencion, retencion.getId());
			em.flush();

			resultado.put("generado", true);
			resultado.put("idAsiento", asientoAttached.getCodigo());
			resultado.put("numeroAlterno", asientoAttached.getNumeroAlterno());
			System.out.println("✓ Asiento contable generado: " + asientoAttached.getNumeroAlterno());
		} catch (Throwable e) {
			sessionContext.setRollbackOnly();
			throw e;
		}
		return resultado;
	}

	private String llamarRecepcionSRI(String url, byte[] xmlBytes, PrintWriter log) throws Exception {
		try {
			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:rec=\"http://ec.gob.sri.ws.recepcion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<rec:validarComprobante><xml>" + xmlBase64 + "</xml></rec:validarComprobante>" +
				"</soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			log.println("Respuesta WS1: " + respuestaCompleta);

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
				if (mensajeList.getLength() == 0) mensajeList = docEl.getElementsByTagName("mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				return estado;
			}
			return "SIN_RESPUESTA";
		} catch (Exception e) {
			log.println("Error en llamarRecepcionSRI: " + e.getMessage());
			e.printStackTrace(log);
			throw e;
		}
	}
	
	private ResultadoAutorizacion llamarAutorizacionSRI(String url, String claveAcceso) throws Exception {
		try {
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<aut:autorizacionComprobante><claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>" +
				"</aut:autorizacionComprobante></soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			System.out.println(">>> XML RESPUESTA WS2 (Autorización SRI - RET):\n" + respuestaCompleta);

			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) resultado.estado = estadoList.item(0).getTextContent();

			NodeList numAutList = docEl.getElementsByTagNameNS("*", "numeroAutorizacion");
			if (numAutList.getLength() > 0) resultado.numeroAutorizacion = numAutList.item(0).getTextContent();

			NodeList fechaAutList = docEl.getElementsByTagNameNS("*", "fechaAutorizacion");
			if (fechaAutList.getLength() > 0) resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();

			NodeList comprobanteList = docEl.getElementsByTagNameNS("*", "comprobante");
			if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();

			NodeList mensajeIdList = docEl.getElementsByTagNameNS("*", "identificador");
			if (mensajeIdList.getLength() > 0) resultado.mensajeId = mensajeIdList.item(0).getTextContent();

			NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
			if (mensajeList.getLength() > 0) resultado.mensaje = mensajeList.item(0).getTextContent();

			NodeList infoAdicionalList = docEl.getElementsByTagNameNS("*", "informacionAdicional");
			if (infoAdicionalList.getLength() > 0) resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();

			return resultado;
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarAutorizacionSRI RET: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	// =========================================================================
	// anularRetencion
	// =========================================================================

	@Override
	public java.util.Map<String, Object> anularRetencion(Long idRetencion, String motivo, String usuario) throws Throwable {
		System.out.println("=== anularRetencion | id=" + idRetencion + " | usuario=" + usuario + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		Retencion rtn = retencionDaoService.selectById(idRetencion, NombreEntidadesCobro.RETENCION);
		if (rtn == null) {
			resultado.put("mensaje", "Retención con ID " + idRetencion + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(com.saa.rubros.Estado.INACTIVO).equals(rtn.getEstado())) {
			resultado.put("mensaje", "La Retención ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
		LocalDateTime ahora = LocalDateTime.now();

		// Anular asiento contable vinculado (si existe)
		if (rtn.getAsiento() != null && rtn.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(com.saa.model.cnt.Asiento.class, rtn.getAsiento().getCodigo());
				if (asiento != null && !Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO).equals(asiento.getEstado())) {
					asiento.setEstado(Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO));
					asiento.setMotivoAnulacion(motivoFinal);
					asiento.setFechaAnulacion(ahora);
					asiento.setUsuarioAnulacion(usuarioAnulacion);
					em.merge(asiento);
					em.flush();
					System.out.println("✓ Asiento contable anulado: " + asiento.getCodigo());
					resultado.put("asientoAnulado", asiento.getCodigo());
				}
			} catch (Exception e) {
				System.err.println("⚠ Error al anular asiento: " + e.getMessage());
				resultado.put("advertenciaAsiento", "Retención anulada pero error al anular el asiento: " + e.getMessage());
			}
		}

		rtn.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));
		rtn.setEstadoEmision(3L); // 3 = ANULADA (tsri lsri 603)
		rtn.setMotivoAnulacion(motivoFinal);
		rtn.setFechaAnulacion(ahora);
		rtn.setUsuarioAnulacion(usuarioAnulacion);
		retencionDaoService.save(rtn, rtn.getId());
		em.flush();

		System.out.println("✓ Retención anulada: " + idRetencion);
		resultado.put("exito", true);
		resultado.put("mensaje", "Retención N° " + nvl(rtn.getNumero(), String.valueOf(idRetencion)) + " anulada correctamente.");
		resultado.put("idRetencion", idRetencion);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}

	private LocalDateTime parseFechaAutorizacion(String fechaStr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			return LocalDateTime.parse(fechaStr, formatter);
		} catch (Exception e) {
			return LocalDateTime.now();
		}
	}
	
	private static class ResultadoAutorizacion {
		String estado;
		String numeroAutorizacion;
		String fechaAutorizacion;
		String comprobanteXML;
		String mensajeId;
		String mensaje;
		String informacionAdicional;
		String respuestaCompleta;
	}
	
	/**
	 * Obtiene y actualiza el secuencial para un punto de emisión y tipo de documento
	 */
	private String obtenerSecuencial(Long idPtoEmision, String tipoDoc) throws Exception {
		System.out.println(">>> OBTENER SECUENCIAL PtoEmision[" + idPtoEmision + "] TipoComprobante[" + tipoDoc + "]");
		
		String sql = "SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query query = em.createQuery(sql);
		query.setParameter("ptoEmision", idPtoEmision);
		query.setParameter("tipoDoc", tipoDoc);
		
		@SuppressWarnings("unchecked")
		List<Object> resultados = query.getResultList();
		
		if (resultados.isEmpty()) {
			throw new IncomeException("No existe numeración para el punto de emisión " + idPtoEmision + " y tipo de documento " + tipoDoc);
		}
		
		com.saa.model.cxc.NumeracionPuntoEmision numeracion = (com.saa.model.cxc.NumeracionPuntoEmision) resultados.get(0);
		Long numeroActual = numeracion.getNumActual();
		Long nuevoNumero = numeroActual + 1;
		
		String sqlUpdate = "UPDATE NumeracionPuntoEmision n SET n.numActual = :nuevoNumero " +
				"WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query updateQuery = em.createQuery(sqlUpdate);
		updateQuery.setParameter("nuevoNumero", nuevoNumero);
		updateQuery.setParameter("ptoEmision", idPtoEmision);
		updateQuery.setParameter("tipoDoc", tipoDoc);
		updateQuery.executeUpdate();
		
		return String.format("%09d", numeroActual);
	}
	
	/**
	 * Genera la clave de acceso usando el algoritmo módulo 11
	 */
	private String generarClaveAcceso(Retencion retencion, String tipoComprobante, Long ambiente, 
			String tipoEmision, String secuencial) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = retencion.getFecha().format(formatter);
		
		String ruc = retencion.getFacturador().getNumDoc();
		String codClave = retencion.getFacturador().getCodClave();
		
		System.out.println("RUC: " + ruc);
		System.out.println("CLAVE: " + codClave);
		
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente + 
				retencion.getNumEstablecimiento() + retencion.getNumPtoEmision() + 
				secuencial + codClave + tipoEmision;
		
		System.out.println(">>> GENERADOR CLAVE cadena[" + claveSinDV + "]");
		
		int digitoVerificador = calcularModulo11(claveSinDV);
		String claveCompleta = claveSinDV + digitoVerificador;
		System.out.println(">>> CLAVE COMPLETA [" + claveCompleta + "]");
		
		return claveCompleta;
	}
	
	/**
	 * Calcula el dígito verificador usando módulo 11
	 */
	private int calcularModulo11(String cadena) {
		String invertida = new StringBuilder(cadena).reverse().toString();
		
		int suma = 0;
		int factor = 2;
		
		for (int i = 0; i < invertida.length(); i++) {
			int digito = Character.getNumericValue(invertida.charAt(i));
			suma += digito * factor;
			
			if (factor == 7) {
				factor = 2;
			} else {
				factor++;
			}
		}
		
		int dv = 11 - (suma % 11);
		
		if (dv == 10) {
			return 1;
		} else if (dv == 11) {
			return 0;
		} else {
			return dv;
		}
	}
	
	/**
	 * Obtiene el directorio base de uploads desde la variable de sistema
	 * (misma lógica que FileService)
	 */
	private String getBaseUploadDirectory() {
		// Verificar si hay una variable de sistema configurada
		String uploadDir = System.getProperty("saa.upload.dir");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}

		// Verificar variable de entorno
		uploadDir = System.getenv("SAA_UPLOAD_DIR");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}

		// Directorio por defecto basado en el sistema operativo
		String userHome = System.getProperty("user.home");
		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("windows")) {
			return userHome + "/saa-uploads/";
		} else {
			return "/opt/saa-uploads/";
		}
	}

	// =========================================================================
	// consultarYActualizarEstadoRetencion
	// Consulta estado al SRI y si devuelve AUTORIZADO:
	//   - Pasa la retención a estado autorizada si estaba pendiente
	//   - Guarda número de autorización y fecha
	//   - Si no tiene asiento contable y el facturador tiene generaConta=1, lo genera
	//   - Envía el email con XML autorizado
	// =========================================================================
	/**
	 * Punto de recuperación: consulta el estado en el SRI y completa lo que haya
	 * quedado pendiente (estado, asiento contable). Sin transacción propia —
	 * cada etapa se confirma por separado.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> consultarYActualizarEstadoRetencion(Long idRetencion) throws Throwable {
		System.out.println("=== consultarYActualizarEstadoRetencion | idRetencion=" + idRetencion + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// 1. Cargar la retención
		Retencion retencion = retencionDaoService.selectById(idRetencion, NombreEntidadesCobro.RETENCION);
		if (retencion == null) {
			resultado.put("mensaje", "Retención con ID " + idRetencion + " no encontrada.");
			return resultado;
		}
		if (retencion.getClave() == null || retencion.getClave().isEmpty()) {
			resultado.put("mensaje", "La retención no tiene clave de acceso registrada.");
			return resultado;
		}

		Long ambiente = retencion.getAmbiente() != null ? retencion.getAmbiente() : 1L;
		String clave = retencion.getClave();
		Long idFacturador = retencion.getFacturador() != null ? retencion.getFacturador().getId() : null;
		resultado.put("clave", clave);
		resultado.put("estadoActual", retencion.getEstado());

		// 2. Consultar estado al SRI
		String urlWS2 = ambiente == 2L
				? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
				: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
		System.out.println(">>> Consultando estado al SRI: " + urlWS2);

		ResultadoAutorizacion ra;
		try {
			ra = llamarAutorizacionSRI(urlWS2, clave);
		} catch (Exception e) {
			resultado.put("mensaje", "Error al consultar el estado en el SRI: " + e.getMessage());
			resultado.put("error", e.getMessage());
			return resultado;
		}

		resultado.put("estadoSRI", ra.estado);
		resultado.put("numeroAutorizacion", ra.numeroAutorizacion);
		resultado.put("fechaAutorizacion", ra.fechaAutorizacion);
		System.out.println(">>> Estado SRI: " + ra.estado);

		if (!"AUTORIZADO".equals(ra.estado)) {
			resultado.put("mensaje", "El SRI indica que la retención NO está autorizada. Estado: " + ra.estado
					+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
			return resultado;
		}

		// 3. SRI devuelve AUTORIZADO → actualizar retención (transacción propia)
		boolean actualizada;
		try {
			actualizada = self().marcarRetencionAutorizada(
					idRetencion, ra.numeroAutorizacion, ra.fechaAutorizacion, ra.comprobanteXML);
		} catch (Throwable e) {
			resultado.put("mensaje", "El SRI autorizó la retención pero no se pudo actualizar su estado: "
					+ e.getMessage());
			resultado.put("error", e.getMessage());
			System.err.println("⚠ Error actualizando estado de la retención: " + e.getMessage());
			return resultado;
		}
		resultado.put("retencionActualizada", actualizada);

		// 4. Generar asiento contable si no tiene (transacción propia)
		boolean asientoGenerado = false;
		System.out.println("PASO 4: Generando asiento contable...");
		try {
			java.util.Map<String, Object> resAsiento = self().generarContabilidadRetencion(idRetencion);
			asientoGenerado = Boolean.TRUE.equals(resAsiento.get("generado"));
			if (Boolean.TRUE.equals(resAsiento.get("yaExistia"))) {
				resultado.put("asientoExistente", resAsiento.get("numeroAlterno"));
				System.out.println("ℹ La retención ya tiene asiento contable: " + resAsiento.get("numeroAlterno"));
			} else if (asientoGenerado) {
				resultado.put("asiento", resAsiento.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"Retención autorizada pero error al generar asiento: " + e.getMessage());
			System.err.println("⚠ Error en asiento contable: " + e.getMessage());
		}
		resultado.put("asientoGenerado", asientoGenerado);

		// 5. Enviar email con XML autorizado (las retenciones no tienen PDF RIDE)
		System.out.println("PASO 5: Enviando email al proveedor...");
		String destinatario = null;
		if (retencion.getProveedor() != null) destinatario = retencion.getProveedor().getEmail();
		try {
			if (destinatario != null && !destinatario.trim().isEmpty() && idFacturador != null) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				try {
					java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
					if (java.nio.file.Files.exists(pXml))
						xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
				} catch (Exception ioEx) {
					System.err.println("⚠ Error leyendo XML para email: " + ioEx.getMessage());
				}
				String razonSocial = retencion.getFacturador() != null
						? nvl(retencion.getFacturador().getRazonSocial(), nvl(retencion.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(
						destinatario, nvl(retencion.getNumero(), clave),
						clave, razonSocial, "Retención", xmlAutorizado, null);
				resultado.put("emailEnviado", true);
				resultado.put("emailDestinatario", destinatario);
				System.out.println("✓ Email enviado a: " + destinatario);
			} else {
				resultado.put("emailEnviado", false);
				System.out.println("ℹ Email omitido: no hay dirección de correo del proveedor.");
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail",
					"Retención autorizada pero no se pudo enviar el email: " + mailEx.getMessage());
			resultado.put("emailEnviado", false);
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

		resultado.put("exito", true);
		resultado.put("mensaje", "Retención verificada en el SRI: AUTORIZADA."
				+ (actualizada ? " Estado actualizado." : "")
				+ (asientoGenerado ? " Asiento contable generado." : "")
				+ (Boolean.TRUE.equals(resultado.get("emailEnviado")) ? " Email enviado a " + destinatario + "." : ""));
		System.out.println("=== consultarYActualizarEstadoRetencion COMPLETADO ===");
		return resultado;
	}
}
