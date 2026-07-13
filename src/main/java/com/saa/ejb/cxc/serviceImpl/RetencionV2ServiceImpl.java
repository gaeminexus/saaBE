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
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathRetencionV2;
import com.saa.rubros.Estado;
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
			// 1. Obtener datos principales de la retención V2
			String sqlRetencion = "SELECT r.id, r.clave, r.fecha, r.numEstablecimiento, r.numPtoEmision, r.secuencial, " +
					"r.periodoFiscal, r.observacion, " +
					"f.id as facturadorId, f.numDoc as facturadorRUC, f.nombre as facturadorNombre, " +
					"f.razonSocial as facturadorRazonSocial, f.direccion as facturadorDireccion, " +
					"f.telefono as facturadorTelefono, f.mail as facturadorMail, f.microEmpresa, " +
					"f.rimpe, f.popularRimpe, f.agenteRetencion, f.contribuyenteEspecial, f.contabilidad, " +
					"p.tipoId as proveedorTipoId, p.numdoc as proveedorNumDoc, p.nombre as proveedorNombre, " +
					"p.direccion as proveedorDireccion, p.telefono as proveedorTelefono, p.mail as proveedorMail " +
					"FROM RetencionV2 r " +
					"JOIN r.facturador f " +
					"JOIN r.proveedor p " +
					"WHERE r.clave = :clave";
			Query queryRetencion = em.createQuery(sqlRetencion);
			queryRetencion.setParameter("clave", clave);
			Object[] retencionData = (Object[]) queryRetencion.getSingleResult();
			
			if (retencionData == null) {
				throw new IncomeException("RetencionV2 con clave " + clave + " no encontrada");
			}
			
			Long idRetencion = (Long) retencionData[0];
			Long idFacturador = (Long) retencionData[8];
			
			// 2. Obtener dirección del establecimiento
			String sqlEstablecimiento = "SELECT e.direccion FROM RetencionV2 r " +
					"JOIN PuntoEmision pe ON r.ptoEmision = pe.id " +
					"JOIN pe.establecimiento e WHERE r.id = :id";
			Query queryEstablecimiento = em.createQuery(sqlEstablecimiento);
			queryEstablecimiento.setParameter("id", idRetencion);
			String dirEstablecimiento = (String) queryEstablecimiento.getSingleResult();
			
			// 3. Obtener documentos sustento agrupados
			String sqlDocumentos = "SELECT d.tipoDocReten, d.numDocReten, d.fechaEmiDoc, " +
					"d.docResTSinImpuestos, d.docResTotal, d.docResIVACero, d.docResTotalIVA, " +
					"d.docResPorIVA, d.docResForPago " +
					"FROM DetalleRetencionV2 d " +
					"WHERE d.retencionv2.id = :retencionId " +
					"GROUP BY d.tipoDocReten, d.numDocReten, d.fechaEmiDoc, " +
					"d.docResTSinImpuestos, d.docResTotal, d.docResIVACero, d.docResTotalIVA, " +
					"d.docResPorIVA, d.docResForPago";
			Query queryDocumentos = em.createQuery(sqlDocumentos);
			queryDocumentos.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<Object[]> documentos = queryDocumentos.getResultList();
			
			// 4. Obtener todas las retenciones del detalle
			String sqlRetenciones = "SELECT d FROM DetalleRetencionV2 d WHERE d.retencionv2.id = :retencionId";
			Query queryRetenciones = em.createQuery(sqlRetenciones);
			queryRetenciones.setParameter("retencionId", idRetencion);
			@SuppressWarnings("unchecked")
			List<Object> retenciones = queryRetenciones.getResultList();
			
			// 5. Generar XML
			String xmlContent = generarXMLContentRetencionV2(retencionData, dirEstablecimiento, 
					documentos, retenciones, ambiente);
			
			// 6. Guardar archivo XML
			String pathRelativo = "resources/" + idFacturador + "/rtv2/g/" + clave + ".xml";
			String pathAbsoluto = System.getProperty("user.dir") + "/" + pathRelativo;
			
			// Crear directorios si no existen
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			
			// Guardar archivo
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			return new String[]{"OK", pathRelativo, pathAbsoluto};
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML RetencionV2: " + e.getMessage());
		}
	}
	
	/**
	 * Genera el contenido XML de la retención V2 electrónica según estándares del SRI v2.0.0
	 */
	private String generarXMLContentRetencionV2(Object[] retencionData, String dirEstablecimiento,
			List<Object[]> documentos, List<Object> retenciones, Long ambiente) throws Exception {
		
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		
		// Constantes
		String TIPO_DOC = "07"; // Retención
		String TIPO_EMISION = "1"; // Normal
		String COD_IVA = "2";
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		// Extraer datos de la retención
		String claveAcceso = (String) retencionData[1];
		java.time.LocalDate fecha = (java.time.LocalDate) retencionData[2];
		String numEstablecimiento = (String) retencionData[3];
		String numPtoEmision = (String) retencionData[4];
		String secuencial = (String) retencionData[5];
		String periodoFiscal = (String) retencionData[6];
		String observacion = nvl((String) retencionData[7], "");
		
		// Datos del facturador
		String facturadorRUC = (String) retencionData[9];
		String facturadorNombre = (String) retencionData[10];
		String facturadorRazonSocial = (String) retencionData[11];
		String facturadorDireccion = (String) retencionData[12];
		String facturadorTelefono = nvl((String) retencionData[13], "");
		String facturadorMail = nvl((String) retencionData[14], "");
		Integer microEmpresa = (Integer) retencionData[15];
		Integer rimpe = (Integer) retencionData[16];
		Integer popularRimpe = (Integer) retencionData[17];
		String agenteRetencion = (String) retencionData[18];
		String contribuyenteEspecial = (String) retencionData[19];
		Integer contabilidad = (Integer) retencionData[20];
		
		// Datos del proveedor
		String proveedorTipoId = (String) retencionData[21];
		String proveedorNumDoc = (String) retencionData[22];
		String proveedorNombre = (String) retencionData[23];
		String proveedorTelefono = nvl((String) retencionData[25], "");
		String proveedorMail = nvl((String) retencionData[26], "");
		
		String obligadoContabilidad = (contabilidad != null && contabilidad == 1) ? "SI" : "NO";
		
		// Inicio del documento
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		
		// Elemento raíz: comprobanteRetencion
		writer.writeStartElement("comprobanteRetencion");
		writer.writeAttribute("id", "comprobante");
		writer.writeAttribute("version", "2.0.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", TIPO_EMISION, 4);
		writeElement(writer, "razonSocial", nvl(facturadorRazonSocial, ""), 4);
		writeElement(writer, "nombreComercial", nvl(facturadorNombre, ""), 4);
		writeElement(writer, "ruc", nvl(facturadorRUC, ""), 4);
		writeElement(writer, "claveAcceso", nvl(claveAcceso, ""), 4);
		writeElement(writer, "codDoc", TIPO_DOC, 4);
		writeElement(writer, "estab", nvl(numEstablecimiento, ""), 4);
		writeElement(writer, "ptoEmi", nvl(numPtoEmision, ""), 4);
		writeElement(writer, "secuencial", nvl(secuencial, ""), 4);
		writeElement(writer, "dirMatriz", nvl(facturadorDireccion, ""), 4);
		
		// Regímenes especiales
		if (microEmpresa != null && microEmpresa == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (agenteRetencion != null && !agenteRetencion.isEmpty()) {
			writeElement(writer, "agenteRetencion", agenteRetencion, 4);
		}
		if (rimpe != null && rimpe == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (popularRimpe != null && popularRimpe == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoTributaria
		writer.writeCharacters("\n");
		
		// infoCompRetencion
		writer.writeCharacters("  ");
		writer.writeStartElement("infoCompRetencion");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", fecha.format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		
		if (contribuyenteEspecial != null && !contribuyenteEspecial.isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", contribuyenteEspecial, 4);
		}
		
		writeElement(writer, "obligadoContabilidad", obligadoContabilidad, 4);
		writeElement(writer, "tipoIdentificacionSujetoRetenido", proveedorTipoId, 4);
		writeElement(writer, "parteRel", "NO", 4); // Valor por defecto
		writeElement(writer, "razonSocialSujetoRetenido", nvl(proveedorNombre, ""), 4);
		writeElement(writer, "identificacionSujetoRetenido", nvl(proveedorNumDoc, ""), 4);
		writeElement(writer, "periodoFiscal", periodoFiscal, 4);
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoCompRetencion
		writer.writeCharacters("\n");
		
		// docsSustento
		writer.writeCharacters("  ");
		writer.writeStartElement("docsSustento");
		writer.writeCharacters("\n");
		
		for (Object[] doc : documentos) {
			writer.writeCharacters("    ");
			writer.writeStartElement("docSustento");
			writer.writeCharacters("\n");
			
			String tipoDocReten = (String) doc[0];
			String numDocReten = (String) doc[1];
			java.time.LocalDate fechaEmiDoc = (java.time.LocalDate) doc[2];
			Double docResTSinImpuestos = (Double) doc[3];
			Double docResTotal = (Double) doc[4];
			Double docResIVACero = (Double) doc[5];
			Double docResTotalIVA = (Double) doc[6];
			Double docResPorIVA = (Double) doc[7];
			String docResForPago = (String) doc[8];
			
			writeElement(writer, "codSustento", "02", 6); // Valor por defecto
			writeElement(writer, "codDocSustento", tipoDocReten, 6);
			writeElement(writer, "numDocSustento", numDocReten, 6);
			writeElement(writer, "fechaEmisionDocSustento", fechaEmiDoc.format(dateFormatter), 6);
			writeElement(writer, "pagoLocExt", "01", 6); // Residente
			writeElement(writer, "totalSinImpuestos", formatDecimal(docResTSinImpuestos), 6);
			writeElement(writer, "importeTotal", formatDecimal(docResTotal), 6);
			
			// impuestosDocSustento
			writer.writeCharacters("      ");
			writer.writeStartElement("impuestosDocSustento");
			writer.writeCharacters("\n");
			
			if (docResIVACero != null && docResIVACero > 0) {
				writeImpuestoDocSustento(writer, COD_IVA, "0", docResIVACero, "0", 0.0);
			}
			if (docResTotalIVA != null && docResTotalIVA > 0) {
				Double baseIVA = docResTSinImpuestos - nvl(docResIVACero, 0.0);
				writeImpuestoDocSustento(writer, COD_IVA, COD_POR_IVA_15, baseIVA,
						formatDecimal(nvl(docResPorIVA, 0.0)), docResTotalIVA);
			}
			
			writer.writeCharacters("      ");
			writer.writeEndElement(); // impuestosDocSustento
			writer.writeCharacters("\n");
			
			// retenciones
			writer.writeCharacters("      ");
			writer.writeStartElement("retenciones");
			writer.writeCharacters("\n");
			
			for (@SuppressWarnings("unused") Object retencion : retenciones) {
				// Aquí debes extraer los campos del detalle de retención
				// Asumiendo que retencion es un objeto DetalleRetencionV2
				writer.writeCharacters("        ");
				writer.writeStartElement("retencion");
				writer.writeCharacters("\n");
				
				// Los campos específicos dependerán de tu modelo DetalleRetencionV2
				// Por ahora, dejo la estructura básica
				writeElement(writer, "codigo", "2", 10); // Ejemplo
				writeElement(writer, "codigoRetencion", "1", 10); // Ejemplo
				writeElement(writer, "baseImponible", "100.00", 10); // Ejemplo
				writeElement(writer, "porcentajeRetener", "1", 10); // Ejemplo
				writeElement(writer, "valorRetenido", "1.00", 10); // Ejemplo
				
				writer.writeCharacters("        ");
				writer.writeEndElement(); // retencion
				writer.writeCharacters("\n");
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
			writeElement(writer, "total", formatDecimal(docResTotal), 10);
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
		
		// infoAdicional
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		String infoAdicional = "Soporte[" + facturadorTelefono + " - " + facturadorMail + "] " +
				"Contacto Cliente[" + proveedorTelefono + " - " + proveedorMail + "] " +
				"Observacion[" + observacion + "]";
		writer.writeCharacters(infoAdicional);
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
		writer.writeCharacters("  ".repeat(indent / 2));
		writer.writeStartElement(name);
		writer.writeCharacters(value);
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeImpuestoDocSustento(XMLStreamWriter writer, String codigo, String codigoPorcentaje,
			Double baseImponible, String tarifa, Double valorImpuesto) throws Exception {
		writer.writeCharacters("        ");
		writer.writeStartElement("impuestoDocSustento");
		writer.writeCharacters("\n");
		writeElement(writer, "codImpuestoDocSustento", codigo, 10);
		writeElement(writer, "codigoPorcentaje", codigoPorcentaje, 10);
		writeElement(writer, "baseImponible", formatDecimal(baseImponible), 10);
		writeElement(writer, "tarifa", tarifa, 10);
		writeElement(writer, "valorImpuesto", formatDecimal(valorImpuesto), 10);
		writer.writeCharacters("        ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private String nvl(String value, String defaultValue) {
		return value != null ? value : defaultValue;
	}
	
	private Double nvl(Double value, Double defaultValue) {
		return value != null ? value : defaultValue;
	}
	
	private String formatDecimal(Double value) {
		if (value == null) {
			return "0.00";
		}
		return String.format("%.2f", value);
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
}