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
import com.saa.ejb.cxc.dao.RetencionV2DaoService;
import com.saa.ejb.cxc.dao.PathRetencionV2DaoService;
import com.saa.ejb.cxc.service.RetencionV2Service;
import com.saa.ejb.cxc.service.EmailFacturaService;
import com.saa.model.cxc.DetalleRetencionV2;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathRetencionV2;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoAsientos;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
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
public class RetencionV2ServiceImpl implements RetencionV2Service {
	
	// Constantes para códigos del SRI
	// private static final String COD_IVA = "2";
	private static final String COD_POR_IVA_15 = "2";
	
	@EJB
	private RetencionV2DaoService retencionV2DaoService;
	
	@EJB
	private PathRetencionV2DaoService pathRetencionV2DaoService;

	@EJB
	private com.saa.ejb.signature.service.SignatureService signatureService;

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

	@EJB
	private EmailFacturaService emailFacturaService;

	@PersistenceContext
	private EntityManager em;
	@Override
	public RetencionV2 selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById RetencionV2 con id: " + id);
		return retencionV2DaoService.selectById(id, NombreEntidadesCobro.RETENCION_V2);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de RetencionV2Service");
		RetencionV2 entidad = new RetencionV2();
		for (Long registro : id) {
			retencionV2DaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<RetencionV2> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de RetencionV2Service");
		for (RetencionV2 registro : lista) {
			retencionV2DaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<RetencionV2> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll RetencionV2Service");
		List<RetencionV2> result = retencionV2DaoService.selectAll(NombreEntidadesCobro.RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total RetencionV2 no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public RetencionV2 saveSingle(RetencionV2 entidad) throws Throwable {
		System.out.println("saveSingle - RetencionV2");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = retencionV2DaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<RetencionV2> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria RetencionV2Service");
		List<RetencionV2> result = retencionV2DaoService.selectByCriteria(datos, NombreEntidadesCobro.RETENCION_V2);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio RetencionV2 no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public String[] generarXMLRetencionV2(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLRetencionV2 con clave: " + clave + " y ambiente: " + ambiente);
		try {
			// 1. Obtener la retención V2 por clave
			String sqlRetencion = "SELECT r FROM RetencionV2 r WHERE r.clave = :clave";
			Query queryRetencion = em.createQuery(sqlRetencion);
			queryRetencion.setParameter("clave", clave);
			RetencionV2 retencion = (RetencionV2) queryRetencion.getSingleResult();
			if (retencion == null) throw new IncomeException("RetencionV2 con clave " + clave + " no encontrada");

			Long idRetencion  = retencion.getId();
			Long idFacturador = retencion.getFacturador().getId();

			// 2. Obtener dirección del establecimiento
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", retencion.getPtoEmision().getId());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();

			// 3. Obtener documentos sustento agrupados (un registro por numDocReten)
			String sqlDocumentos = "SELECT DISTINCT d.tipoDocReten, d.numDocReten, d.fechaEmiDoc, " +
					"d.docResTotalSinImpuestos, d.docResTotal, d.docResIvaCero, d.docResTotalIva, " +
					"d.docResPorIva, d.docResForPago " +
					"FROM DetalleRetencionV2 d " +
					"WHERE d.retencionV2.id = :retencionId " +
					"ORDER BY d.numDocReten";
			Query queryDocumentos = em.createQuery(sqlDocumentos);
			queryDocumentos.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<Object[]> documentos = queryDocumentos.getResultList();

			// 4. Obtener todos los detalles de retención
			String sqlDetalle = "SELECT d FROM DetalleRetencionV2 d WHERE d.retencionV2.id = :retencionId ORDER BY d.id";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<DetalleRetencionV2> detalles = queryDetalle.getResultList();

			// 5. Generar XML
			String xmlContent = generarXMLContentRetencionV2(retencion, dirEstablecimiento, documentos, detalles, ambiente);

			// 6. Guardar archivo XML
			String pathRelativo = "resources/" + idFacturador + "/rtv2/g/" + clave + ".xml";
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto  = baseUploadDir + pathRelativo;

			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));

			System.out.println("✓ XML RetencionV2 generado en: " + pathAbsoluto);
			return new String[]{"OK", pathRelativo, pathAbsoluto};

		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML RetencionV2: " + e.getMessage());
		}
	}

	/**
	 * Genera el contenido XML de la retención V2 según estándares del SRI v2.0.0.
	 * Basado en gn_xml_rtv2.php.
	 */
	private String generarXMLContentRetencionV2(RetencionV2 retencion, String dirEstablecimiento,
			List<Object[]> documentos, List<DetalleRetencionV2> detalles, Long ambiente) throws Exception {

		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory  = XMLOutputFactory.newInstance();
		XMLStreamWriter writer    = factory.createXMLStreamWriter(stringWriter);

		final String TIPO_DOC    = "07";
		final String TIPO_EMISION = "1";
		final String COD_IVA     = "2";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		com.saa.model.cxc.Facturador f  = retencion.getFacturador();
		com.saa.model.tsr.Titular    pr = retencion.getProveedor();

		String obligadoContabilidad = (f.getContabilidad() != null && f.getContabilidad() == 1) ? "SI" : "NO";
		String facturadorTelefono   = nvl(f.getTelefono(), "");
		String facturadorMail       = nvl(f.getMail(), "");
		String proveedorTelefono    = pr != null ? nvl(pr.getTelefono(), "") : "";
		String proveedorMail        = pr != null ? nvl(pr.getEmail(), "")    : "";

		// ── Raíz ──────────────────────────────────────────────────────────────
		writer.writeStartElement("comprobanteRetencion");
		writer.writeAttribute("id", "comprobante");
		writer.writeAttribute("version", "2.0.0");
		writer.writeCharacters("\n");

		// ── infoTributaria ────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		writeElement(writer, "ambiente",        String.valueOf(ambiente),          4);
		writeElement(writer, "tipoEmision",     TIPO_EMISION,                      4);
		writeElement(writer, "razonSocial",     nvl(f.getRazonSocial(), ""),       4);
		writeElement(writer, "nombreComercial", nvl(f.getNombre(), ""),            4);
		writeElement(writer, "ruc",             nvl(f.getNumDoc(), ""),            4);
		writeElement(writer, "claveAcceso",     nvl(retencion.getClave(), ""),     4);
		writeElement(writer, "codDoc",          TIPO_DOC,                          4);
		writeElement(writer, "estab",           nvl(retencion.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi",          nvl(retencion.getNumPtoEmision(), ""),      4);
		writeElement(writer, "secuencial",      nvl(retencion.getSecuencial(), ""),         4);
		writeElement(writer, "dirMatriz",       nvl(f.getDireccion(), ""),         4);
		if (f.getMicroEmpresa() != null && f.getMicroEmpresa() == 1)
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		if (f.getAgenteRetencion() != null && !f.getAgenteRetencion().isEmpty())
			writeElement(writer, "agenteRetencion", f.getAgenteRetencion(), 4);
		if (f.getRimpe() != null && f.getRimpe() == 1)
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		if (f.getPopularRimpe() != null && f.getPopularRimpe() == 1)
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoTributaria
		writer.writeCharacters("\n");

		// ── infoCompRetencion ─────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoCompRetencion");
		writer.writeCharacters("\n");
		writeElement(writer, "fechaEmision", retencion.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		if (f.getContribuyenteEspecial() != null && !f.getContribuyenteEspecial().isEmpty())
			writeElement(writer, "contribuyenteEspecial", f.getContribuyenteEspecial(), 4);
		writeElement(writer, "obligadoContabilidad", obligadoContabilidad, 4);
		writeElement(writer, "tipoIdentificacionSujetoRetenido",
				pr != null ? String.valueOf(pr.getRubroTipoIdentificacionH()) : "", 4);
		writeElement(writer, "parteRel", "NO", 4); // Tabla 14 ATS: NO = no vinculado
		writeElement(writer, "razonSocialSujetoRetenido", pr != null ? nvl(pr.getNombre(), "") : "", 4);
		writeElement(writer, "identificacionSujetoRetenido", pr != null ? nvl(pr.getIdentificacion(), "") : "", 4);
		writeElement(writer, "periodoFiscal", nvl(retencion.getPeriodoFiscal(), ""), 4);
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoCompRetencion
		writer.writeCharacters("\n");

		// ── docsSustento ──────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("docsSustento");
		writer.writeCharacters("\n");

		for (Object[] doc : documentos) {
			String  tipoDocReten           = (String)  doc[0];
			String  numDocReten            = (String)  doc[1];
			java.time.LocalDate fechaEmiDoc = (java.time.LocalDate) doc[2];
			Double  docResTotalSinImpuestos = (Double)  doc[3];
			Double  docResTotal            = (Double)  doc[4];
			Double  docResIvaCero          = (Double)  doc[5];
			Double  docResTotalIva         = (Double)  doc[6];
			Double  docResPorIva           = (Double)  doc[7];
			String  docResForPago          = (String)  doc[8];

			writer.writeCharacters("    ");
			writer.writeStartElement("docSustento");
			writer.writeCharacters("\n");

			writeElement(writer, "codSustento",              "02", 6); // Tabla 5 ATS: 02=Factura
			writeElement(writer, "codDocSustento",           nvl(tipoDocReten, ""), 6);
			writeElement(writer, "numDocSustento",           nvl(numDocReten, ""), 6);
			writeElement(writer, "fechaEmisionDocSustento",
					fechaEmiDoc != null ? fechaEmiDoc.format(dateFormatter) : "", 6);
			writeElement(writer, "pagoLocExt",               "01", 6); // Tabla 15 ATS: 01=Residente
			writeElement(writer, "totalSinImpuestos",        fmt(docResTotalSinImpuestos), 6);
			writeElement(writer, "importeTotal",             fmt(docResTotal), 6);

			// impuestosDocSustento
			writer.writeCharacters("      ");
			writer.writeStartElement("impuestosDocSustento");
			writer.writeCharacters("\n");
			if (docResIvaCero != null && docResIvaCero > 0) {
				writeImpuestoDocSustento(writer, COD_IVA, "0", docResIvaCero, "0", 0.0);
			}
			if (docResTotalIva != null && docResTotalIva > 0) {
				double baseIva = nvl(docResTotalSinImpuestos, 0.0) - nvl(docResIvaCero, 0.0);
				writeImpuestoDocSustento(writer, COD_IVA, COD_POR_IVA_15,
						baseIva, fmt(nvl(docResPorIva, 0.0)), docResTotalIva);
			}
			writer.writeCharacters("      ");
			writer.writeEndElement(); // impuestosDocSustento
			writer.writeCharacters("\n");

			// retenciones — filtradas por numDocReten del documento sustento actual
			writer.writeCharacters("      ");
			writer.writeStartElement("retenciones");
			writer.writeCharacters("\n");
			for (DetalleRetencionV2 det : detalles) {
				if (numDocReten != null && numDocReten.equals(det.getNumDocReten())) {
					writer.writeCharacters("        ");
					writer.writeStartElement("retencion");
					writer.writeCharacters("\n");
					writeElement(writer, "codigo",            nvl(det.getCodImpuesto(), ""),  10);
					writeElement(writer, "codigoRetencion",   nvl(det.getCodRetencion(), ""), 10);
					writeElement(writer, "baseImponible",     fmt(det.getBaseImponible()),     10);
					writeElement(writer, "porcentajeRetener", fmt(det.getPorcentajeReten()),   10);
					writeElement(writer, "valorRetenido",     fmt(det.getValorReten()),        10);
					writer.writeCharacters("        ");
					writer.writeEndElement(); // retencion
					writer.writeCharacters("\n");
				}
			}
			writer.writeCharacters("      ");
			writer.writeEndElement(); // retenciones
			writer.writeCharacters("\n");

			// pagos
			writer.writeCharacters("      ");
			writer.writeStartElement("pagos");
			writer.writeCharacters("\n");
			writer.writeCharacters("        ");
			writer.writeStartElement("pago");
			writer.writeCharacters("\n");
			writeElement(writer, "formaPago", nvl(docResForPago, "01"), 10);
			writeElement(writer, "total",     fmt(docResTotal),         10);
			writer.writeCharacters("        ");
			writer.writeEndElement(); // pago
			writer.writeCharacters("\n");
			writer.writeCharacters("      ");
			writer.writeEndElement(); // pagos
			writer.writeCharacters("\n");

			writer.writeCharacters("    ");
			writer.writeEndElement(); // docSustento
			writer.writeCharacters("\n");
		}

		writer.writeCharacters("  ");
		writer.writeEndElement(); // docsSustento
		writer.writeCharacters("\n");

		// ── infoAdicional ─────────────────────────────────────────────────────
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Soporte[" + facturadorTelefono + " - " + facturadorMail + "] " +
				"Contacto Cliente[" + proveedorTelefono + " - " + proveedorMail + "] " +
				"Observacion[" + nvl(retencion.getObservacion(), "") + "]");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoAdicional
		writer.writeCharacters("\n");

		writer.writeEndElement(); // comprobanteRetencion
		writer.writeEndDocument();
		writer.close();

		return stringWriter.toString();
	}
	
	private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
		for (int i = 0; i < indent / 2; i++) writer.writeCharacters("  ");
		writer.writeStartElement(name);
		writer.writeCharacters(value != null ? value : "");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private void writeImpuestoDocSustento(XMLStreamWriter writer, String codigo, String codigoPorcentaje,
			Double baseImponible, String tarifa, Double valorImpuesto) throws Exception {
		writer.writeCharacters("        ");
		writer.writeStartElement("impuestoDocSustento");
		writer.writeCharacters("\n");
		writeElement(writer, "codImpuestoDocSustento", codigo,               10);
		writeElement(writer, "codigoPorcentaje",        codigoPorcentaje,     10);
		writeElement(writer, "baseImponible",            fmt(baseImponible),   10);
		writeElement(writer, "tarifa",                   tarifa,               10);
		writeElement(writer, "valorImpuesto",            fmt(valorImpuesto),   10);
		writer.writeCharacters("        ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private String nvl(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}

	private double nvl(Double value, double defaultValue) {
		return value != null ? value : defaultValue;
	}

	private String fmt(Double value) {
		if (value == null) return "0.00";
		return String.format(java.util.Locale.US, "%.2f", value);
	}

	private String fmt(double value) {
		return String.format(java.util.Locale.US, "%.2f", value);
	}

	private String getBaseUploadDirectory() {
		String uploadDir = System.getProperty("saa.upload.dir");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}
		uploadDir = System.getenv("SAA_UPLOAD_DIR");
		if (uploadDir != null && !uploadDir.trim().isEmpty()) {
			return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
		}
		String userHome = System.getProperty("user.home");
		String osName   = System.getProperty("os.name").toLowerCase();
		return osName.contains("windows") ? userHome + "/saa-uploads/" : "/opt/saa-uploads/";
	}
	
	@Override
	public String autorizarRetencionV2(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarRetencionV2 con clave: " + clave);
		
		String respuesta = "";
		String baseDir = System.getProperty("user.dir");
		String resourcesPath = baseDir + "/resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado
			String strXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
			Path pathFirmado = Paths.get(resourcesPath + "/rtv2/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, strXML.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla prt2 (alterno=3)
			PathRetencionV2 pathF = new PathRetencionV2();
			RetencionV2 retencion = retencionV2DaoService.selectById(codigoRetencion, NombreEntidadesCobro.RETENCION_V2);
			pathF.setRetencionV2(retencion);
			pathF.setPath("resources/" + idFacturador + "/rtv2/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathRetencionV2DaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			retencion.setEstado(3L);
			retencionV2DaoService.save(retencion, retencion.getId());
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/rtv2/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					String contenidoXML = new String(Files.readAllBytes(pathFirmado), "UTF-8");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, contenidoXML, logWriter1);
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/rtv2/e/" + clave + ".xml");
					Files.write(pathEnviado, contenidoXML.getBytes("UTF-8"));
					
					PathRetencionV2 pathE = new PathRetencionV2();
					pathE.setRetencionV2(retencion);
					pathE.setPath("resources/" + idFacturador + "/rtv2/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathRetencionV2DaoService.save(pathE, null);
					
					retencion.setEstado(4L);
					retencionV2DaoService.save(retencion, retencion.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						Thread.sleep(2000);
						
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								Path logWS2A = Paths.get(resourcesPath + "/rtv2/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								Path pathAutorizado = Paths.get(resourcesPath + "/rtv2/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								PathRetencionV2 pathA = new PathRetencionV2();
								pathA.setRetencionV2(retencion);
								pathA.setPath("resources/" + idFacturador + "/rtv2/a/" + clave + ".xml");
								pathA.setAlterno(5L);
								pathRetencionV2DaoService.save(pathA, null);
								
								retencion.setEstado(5L);
								retencion.setEstadoEmision(1L);
								retencion.setAutorizacion(resultado.numeroAutorizacion);
								retencion.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								retencionV2DaoService.save(retencion, retencion.getId());
								
								respuesta = resultado.estado;
								
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								Path logWS2N = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									Path pathNoAutorizado = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									PathRetencionV2 pathN = new PathRetencionV2();
									pathN.setRetencionV2(retencion);
									pathN.setPath("resources/" + idFacturador + "/rtv2/n/" + clave + ".xml");
									pathN.setAlterno(6L);
									pathRetencionV2DaoService.save(pathN, null);
								}
								
								retencion.setEstado(6L);
								retencion.setEstadoEmision(2L);
								retencionV2DaoService.save(retencion, retencion.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							Path logWS2Error = Paths.get(resourcesPath + "/rtv2/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/rtv2/n/" + clave + ".xml"));
							
							retencion.setEstado(6L);
							retencion.setEstadoEmision(2L);
							retencionV2DaoService.save(retencion, retencion.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						respuesta = "Estado: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							retencion.setAutorizacion(clave);
							retencion.setFechaAutorizacion(retencion.getFecha().plusMinutes(1).plusSeconds(15));
							retencion.setEstado(5L);
							retencionV2DaoService.save(retencion, retencion.getId());
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
			throw new IncomeException("Error en autorizarRetencionV2: " + e.getMessage());
		}
		
		return respuesta;
	}
	
	private String llamarRecepcionSRI(String url, String xmlContent, PrintWriter log) throws Exception {
		try {
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			
			SOAPEnvelope envelope = soapPart.getEnvelope();
			envelope.addNamespaceDeclaration("ec", "http://ec.gob.sri.ws.recepcion");
			
			SOAPBody soapBody = envelope.getBody();
			SOAPElement validarComprobante = soapBody.addChildElement("validarComprobante", "ec");
			SOAPElement xml = validarComprobante.addChildElement("xml", "ec");
			xml.addTextNode(xmlContent);
			
			soapMessage.saveChanges();
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			log.println("Respuesta WS1: " + soapResponse.getSOAPPart().getEnvelope().toString());
			
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				return estado;
			}
			
			soapConnection.close();
			return "SIN_RESPUESTA";
		} catch (Exception e) {
			log.println("Error en llamarRecepcionSRI: " + e.getMessage());
			e.printStackTrace(log);
			throw e;
		}
	}
	
	private ResultadoAutorizacion llamarAutorizacionSRI(String url, String claveAcceso) throws Exception {
		try {
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			
			SOAPEnvelope envelope = soapPart.getEnvelope();
			envelope.addNamespaceDeclaration("ec", "http://ec.gob.sri.ws.autorizacion");
			
			SOAPBody soapBody = envelope.getBody();
			SOAPElement autorizacionComprobante = soapBody.addChildElement("autorizacionComprobante", "ec");
			SOAPElement claveAccesoElement = autorizacionComprobante.addChildElement("claveAccesoComprobante", "ec");
			claveAccesoElement.addTextNode(claveAcceso);
			
			soapMessage.saveChanges();
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			String respuestaCompleta = soapResponse.getSOAPPart().getEnvelope().toString();
			
			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;
			
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				resultado.estado = estadoList.item(0).getTextContent();
			}
			
			NodeList numAutList = responseBody.getElementsByTagName("numeroAutorizacion");
			if (numAutList.getLength() > 0) {
				resultado.numeroAutorizacion = numAutList.item(0).getTextContent();
			}
			
			NodeList fechaAutList = responseBody.getElementsByTagName("fechaAutorizacion");
			if (fechaAutList.getLength() > 0) {
				resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();
			}
			
			NodeList comprobanteList = responseBody.getElementsByTagName("comprobante");
			if (comprobanteList.getLength() > 0) {
				resultado.comprobanteXML = comprobanteList.item(0).getTextContent();
			}
			
			NodeList mensajeIdList = responseBody.getElementsByTagName("identificador");
			if (mensajeIdList.getLength() > 0) {
				resultado.mensajeId = mensajeIdList.item(0).getTextContent();
			}
			
			NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
			if (mensajeList.getLength() > 0) {
				resultado.mensaje = mensajeList.item(0).getTextContent();
			}
			
			NodeList infoAdicionalList = responseBody.getElementsByTagName("informacionAdicional");
			if (infoAdicionalList.getLength() > 0) {
				resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();
			}
			
			soapConnection.close();
			return resultado;
		} catch (Exception e) {
			throw e;
		}
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

	@Override
	public java.util.Map<String, Object> procesarRetencionV2Completa(RetencionV2 retencion,
			java.util.List<DetalleRetencionV2> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {

		System.out.println("=== INICIANDO PROCESO COMPLETO DE RETENCIÓN V2 ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// ── PASO 0: Validar configuración contable ANTES de grabar ─────────────
		if (retencion.getFacturador() != null
				&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {

			if (retencion.getFacturador().getEmpresa() == null) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
						+ "pero no tiene empresa contable configurada.");
				return resultado;
			}

			Long idEmpresa = retencion.getFacturador().getEmpresa().getCodigo();
			System.out.println("PASO 0: Validando cuentas contables para empresa " + idEmpresa + "...");

			// Convertir DetalleRetencionV2 → DetalleRetencion para reutilizar validador
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalesParaValidar = new java.util.ArrayList<>();
			if (detalles != null) {
				for (DetalleRetencionV2 d : detalles) {
					com.saa.model.cxc.DetalleRetencion dr = new com.saa.model.cxc.DetalleRetencion();
					dr.setCodRetencion(d.getCodRetencion());
					dr.setValorReten(d.getValorReten());
					detalesParaValidar.add(dr);
				}
			}
			// Construir objeto Retencion "dummy" para reutilizar validarCuentasContablesRetencion
			com.saa.model.cxc.Retencion retencionDummy = new com.saa.model.cxc.Retencion();
			retencionDummy.setProveedor(retencion.getProveedor());

			java.util.List<String> erroresContables =
					asientoContableService.validarCuentasContablesRetencion(retencionDummy, detalesParaValidar, idEmpresa);

			if (!erroresContables.isEmpty()) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "No se puede emitir la retención V2: faltan cuentas contables.");
				resultado.put("erroresContables", erroresContables);
				StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
				for (int i = 0; i < erroresContables.size(); i++)
					sb.append("  ").append(i + 1).append(". ").append(erroresContables.get(i)).append("\n");
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

			// ── PASO 1: Grabar retención V2 ────────────────────────────────────
			System.out.println("PASO 1: Grabando retención V2 en base de datos...");
			try {
				retencion = this.saveSingle(retencion);
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_RETENCION");
				resultado.put("mensaje", "Error al grabar la retención V2: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}
			resultado.put("retencion",   retencion);
			resultado.put("idRetencion", retencion.getId());
			System.out.println("✓ Retención V2 grabada ID: " + retencion.getId()
					+ " | Clave: " + retencion.getClave());

			// ── PASO 1.5: Guardar detalles ─────────────────────────────────────
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 1.5: Guardando " + detalles.size() + " detalles de retención V2...");
				try {
					for (DetalleRetencionV2 detalle : detalles) {
						detalle.setRetencionV2(retencion);
						if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(Estado.ACTIVO));
						em.persist(detalle);
					}
					em.flush();
					System.out.println("✓ Detalles V2 guardados correctamente.");
				} catch (Exception e) {
					resultado.put("etapa", "GRABADO_DETALLES");
					resultado.put("mensaje", "Error al grabar los detalles de la retención V2: " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			}

			if (destinatario == null && retencion.getProveedor() != null)
				destinatario = retencion.getProveedor().getEmail();

			String clave = retencion.getClave();
			if (clave == null || clave.isEmpty())
				throw new Exception("La retención V2 no tiene clave de acceso");
			Long idFacturador = retencion.getFacturador() != null ? retencion.getFacturador().getId() : null;
			if (idFacturador == null)
				throw new Exception("La retención V2 no tiene facturador asociado");

			resultado.put("claveAcceso", clave);
			ambiente   = 1L; // FORZADO PRUEBAS — cambiar a 2L para producción
			conectaSRI = 1L;

			// ── PASO 2 y 3: Generar y firmar XML ──────────────────────────────
			String xmlFirmado;
			try {
				System.out.println("PASO 2: Generando XML de retención V2...");
				String[] resultadoXML = this.generarXMLRetencionV2(clave, ambiente);
				String xmlSinFirmar = new String(
						java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(resultadoXML[2])),
						java.nio.charset.StandardCharsets.UTF_8);
				System.out.println("PASO 3: Firmando XML...");
				xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
				System.out.println("✓ XML generado y firmado.");
			} catch (Exception e) {
				resultado.put("etapa", "GENERACION_XML");
				resultado.put("mensaje", "Error al generar o firmar el XML de la retención V2: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// ── PASO 4: Autorizar ante el SRI ──────────────────────────────────
			System.out.println("PASO 4: Autorizando ante el SRI...");
			String resultadoAutorizacion;
			try {
				resultadoAutorizacion = this.autorizarRetencionV2(
						idFacturador, ambiente, conectaSRI, clave,
						retencion.getId(), xmlFirmado, destinatario, pathLogo);
			} catch (Exception e) {
				resultado.put("etapa", "AUTORIZACION_SRI");
				resultado.put("mensaje", "Error al comunicarse con el SRI: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			resultado.put("autorizacion", resultadoAutorizacion);
			boolean autorizada = resultadoAutorizacion != null
					&& resultadoAutorizacion.contains("AUTORIZADO");

			if (!autorizada) {
				resultado.put("etapa",   "AUTORIZACION_SRI");
				resultado.put("exito",   false);
				resultado.put("estado",  "NO_AUTORIZADO");
				resultado.put("mensaje", "La retención V2 fue enviada al SRI pero no fue autorizada. "
						+ "Respuesta: " + resultadoAutorizacion);
				return resultado;
			}

			System.out.println("✓ Retención V2 AUTORIZADA por el SRI.");
			resultado.put("estado", "AUTORIZADO");

			// ── PASO 5: Generar asiento contable ──────────────────────────────
			if (retencion.getFacturador().getEmpresa() != null
					&& Long.valueOf(1L).equals(retencion.getFacturador().getGeneraConta())) {
				System.out.println("PASO 5: Generando asiento contable de Retención V2...");
				try {
					Long idEmpresaConta = retencion.getFacturador().getEmpresa().getCodigo();
					RetencionV2 rtActualizada = retencionV2DaoService.selectById(
							retencion.getId(), NombreEntidadesCobro.RETENCION_V2);
					java.time.LocalDate fechaAsiento = rtActualizada.getFecha() != null
							? rtActualizada.getFecha().toLocalDate() : java.time.LocalDate.now();
					String obsAsiento = "Retención V2 N° " + nvl(rtActualizada.getNumero(), clave)
							+ " | Proveedor: " + (rtActualizada.getProveedor() != null
									? rtActualizada.getProveedor().getNombre() : "")
							+ " | Aut: " + nvl(rtActualizada.getAutorizacion(), clave);
					String usuarioAsiento = (rtActualizada.getUsuario() != null)
							? rtActualizada.getUsuario().getNombre() : "SISTEMA";
					com.saa.model.cnt.Asiento asientoGenerado =
							asientoContableService.generarAsientoRetencionV2(
									rtActualizada.getId(), idEmpresaConta,
									TipoAsientos.RETENCIONES_EMITIDAS_V2,
									fechaAsiento, obsAsiento, usuarioAsiento);
					resultado.put("asiento", asientoGenerado.getNumeroAlterno());
					System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());
				} catch (Exception e) {
					resultado.put("advertenciaAsiento",
							"Retención V2 autorizada pero ocurrió un error al generar el asiento: "
							+ e.getMessage() + ". Genere el asiento manualmente desde Contabilidad.");
					System.err.println("⚠ Error en asiento contable de Retención V2: " + e.getMessage());
					e.printStackTrace();
				}
			}

			// ── PASO 6: Enviar correo electrónico ─────────────────────────────
			System.out.println("PASO 6: Enviando email al proveedor...");
			try {
				if (destinatario != null && !destinatario.trim().isEmpty()) {
					String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
					String xmlAutorizado = null;
					byte[] pdfBytes = null;
					try {
						java.nio.file.Path pXml = java.nio.file.Paths.get(
								resourcesPath + "/rtv2/a/" + clave + ".xml");
						if (java.nio.file.Files.exists(pXml))
							xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
						java.nio.file.Path pPdf = java.nio.file.Paths.get(
								resourcesPath + "/rtv2/a/" + clave + ".pdf");
						if (java.nio.file.Files.exists(pPdf))
							pdfBytes = java.nio.file.Files.readAllBytes(pPdf);
					} catch (Exception ioEx) {
						System.err.println("⚠ No se pudieron leer archivos para el email: " + ioEx.getMessage());
					}
					String razonSocial = retencion.getFacturador() != null
							? nvl(retencion.getFacturador().getRazonSocial(),
								  nvl(retencion.getFacturador().getNombre(), "")) : "";
					emailFacturaService.enviarFacturaAutorizada(
							destinatario, nvl(retencion.getNumero(), clave),
							clave, razonSocial, xmlAutorizado, pdfBytes);
					resultado.put("emailEnviado", true);
					System.out.println("✓ Email enviado a: " + destinatario);
				} else {
					resultado.put("emailEnviado", false);
					System.out.println("ℹ Email omitido: sin dirección de correo del proveedor.");
				}
			} catch (Exception mailEx) {
				resultado.put("advertenciaEmail",
						"Retención V2 autorizada pero no se pudo enviar el email: "
						+ mailEx.getMessage() + ". Reenvíe manualmente.");
				System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
			}

			// ── FIN ────────────────────────────────────────────────────────────
			resultado.put("exito",   true);
			resultado.put("etapa",   "COMPLETADO");
			resultado.put("mensaje", "Retención V2 procesada y autorizada exitosamente.");
			System.out.println("=== PROCESO COMPLETO DE RETENCIÓN V2 FINALIZADO ===");

		} catch (Exception e) {
			System.err.println("ERROR inesperado en procesarRetencionV2Completa: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito",   false);
			resultado.put("etapa",   "ERROR_INESPERADO");
			resultado.put("error",   e.getMessage());
			resultado.put("mensaje", "Error inesperado al procesar la retención V2: " + e.getMessage());
			throw e;
		}
		return resultado;
	}
}