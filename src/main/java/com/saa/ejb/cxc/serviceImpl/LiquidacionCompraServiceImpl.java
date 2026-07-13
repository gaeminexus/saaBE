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
import com.saa.ejb.cxc.dao.LiquidacionCompraDaoService;
import com.saa.ejb.cxc.dao.PathLiquidacionCompraDaoService;
import com.saa.ejb.cxc.service.LiquidacionCompraService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.LiquidacionCompra;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathLiquidacionCompra;
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
public class LiquidacionCompraServiceImpl implements LiquidacionCompraService {

	@EJB
	private LiquidacionCompraDaoService liquidacionCompraDaoService;
	
	@EJB
	private PathLiquidacionCompraDaoService pathLiquidacionCompraDaoService;
	
	@EJB
	private SignatureService signatureService;
	
	@PersistenceContext
	private EntityManager em;

	@Override
	public LiquidacionCompra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById LiquidacionCompra con id: " + id);
		return liquidacionCompraDaoService.selectById(id, NombreEntidadesCobro.LIQUIDACION_COMPRA);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de LiquidacionCompraService");
		LiquidacionCompra entidad = new LiquidacionCompra();
		for (Long registro : id) {
			liquidacionCompraDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<LiquidacionCompra> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de LiquidacionCompraService");
		for (LiquidacionCompra registro : lista) {
			liquidacionCompraDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<LiquidacionCompra> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll LiquidacionCompraService");
		List<LiquidacionCompra> result = liquidacionCompraDaoService.selectAll(NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total LiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public LiquidacionCompra saveSingle(LiquidacionCompra entidad) throws Throwable {
		System.out.println("saveSingle - LiquidacionCompra");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = liquidacionCompraDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<LiquidacionCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria LiquidacionCompraService");
		List<LiquidacionCompra> result = liquidacionCompraDaoService.selectByCriteria(datos, NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio LiquidacionCompra no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public java.util.Map<String, Object> procesarLiquidacionCompleta(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== INICIANDO PROCESO COMPLETO DE LIQUIDACION DE COMPRA ===");
		
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);
		
		try {
			// PASO 1: Grabar la liquidación
			System.out.println("PASO 1: Grabando liquidación en base de datos...");
			if (liquidacion.getId() == null) {
				liquidacion.setEstado(Long.valueOf(Estado.ACTIVO));
			}
			// Usar el MISMO objeto liquidacion - no crear nueva referencia
			liquidacion = liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
			resultado.put("liquidacion", liquidacion);
			resultado.put("idLiquidacion", liquidacion.getId());
			System.out.println("✓ Liquidación grabada con ID: " + liquidacion.getId());
			
			// PASO 1.5: Guardar los detalles de la liquidación
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 1.5: Guardando " + detalles.size() + " detalles de liquidación...");
				for (com.saa.model.cxc.DetalleLiquidacionCompra detalle : detalles) {
					detalle.setLiquidacion(liquidacion);
					if (detalle.getEstado() == null) {
						detalle.setEstado(Long.valueOf(Estado.ACTIVO));
					}
					em.persist(detalle);
				}
				em.flush();
				System.out.println("✓ Detalles guardados correctamente");
			} else {
				System.out.println("⚠ No hay detalles para guardar");
			}
			
			String clave = liquidacion.getClave();
			Long idFacturador = liquidacion.getFacturador().getId();
			
			// Configuración automática
			ambiente = 1L; // PRUEBA
			conectaSRI = 1L; // SI
			
			// Obtener email del proveedor
			if (liquidacion.getTitular() != null && liquidacion.getTitular().getEmail() != null) {
				destinatario = liquidacion.getTitular().getEmail();
			}
			
			pathLogo = "resources/logos/logo_aso.png";
			
			resultado.put("clave", clave);
			resultado.put("idFacturador", idFacturador);
			resultado.put("ambiente", ambiente);
			resultado.put("destinatario", destinatario);
			
			// PASO 2: Generar XML sin firmar
			System.out.println("PASO 2: Generando XML sin firmar...");
			String[] resultadoXML = generarXMLLiquidacion(clave, ambiente);
			String pathXMLGenerado = resultadoXML[2];
			resultado.put("xmlGenerado", resultadoXML[0]);
			resultado.put("pathXMLGenerado", pathXMLGenerado);
			System.out.println("✓ XML generado en: " + pathXMLGenerado);
			
			String xmlSinFirmar = new String(Files.readAllBytes(Paths.get(pathXMLGenerado)), "UTF-8");
			
			// PASO 3: Firmar el XML
			System.out.println("PASO 3: Firmando XML electrónicamente...");
			String xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
			resultado.put("xmlFirmado", "XML firmado correctamente");
			System.out.println("✓ XML firmado electrónicamente");
			
			// PASO 4: Autorizar ante el SRI
			System.out.println("PASO 4: Autorizando ante el SRI...");
			String resultadoAutorizacion = autorizarLiquidacion(
				idFacturador, ambiente, conectaSRI, clave, 
				liquidacion.getId(), xmlFirmado, destinatario, pathLogo
			);
			
			resultado.put("autorizacion", resultadoAutorizacion);
			System.out.println("✓ Resultado autorización: " + resultadoAutorizacion);
			
			if (resultadoAutorizacion != null && 
				(resultadoAutorizacion.contains("AUTORIZADO") || resultadoAutorizacion.equals("AUTORIZADO"))) {
				resultado.put("exito", true);
				resultado.put("mensaje", "Liquidación procesada y autorizada exitosamente");
				resultado.put("estado", "AUTORIZADO");
			} else {
				resultado.put("exito", false);
				resultado.put("mensaje", "Liquidación procesada pero no autorizada");
				resultado.put("estado", "NO_AUTORIZADO");
			}
			
			System.out.println("=== PROCESO COMPLETO FINALIZADO ===");
			
		} catch (Exception e) {
			System.err.println("ERROR en procesarLiquidacionCompleta: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("error", e.getMessage());
			resultado.put("mensaje", "Error al procesar liquidación: " + e.getMessage());
			throw e;
		}
		
		return resultado;
	}
	
	@Override
	public String[] generarXMLLiquidacion(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLLiquidacion con clave: " + clave + " y ambiente: " + ambiente);
		
		try {
			// 1. Obtener datos principales de la liquidación
			String sqlLiquidacion = "SELECT l, f, p FROM LiquidacionCompra l " +
					"JOIN l.facturador f " +
					"JOIN l.comprador p " +
					"WHERE l.clave = :clave";
			Query query = em.createQuery(sqlLiquidacion);
			query.setParameter("clave", clave);
			Object[] result = (Object[]) query.getSingleResult();
			
			LiquidacionCompra liquidacion = (LiquidacionCompra) result[0];
			Long idFacturador = liquidacion.getFacturador().getId();
			
			// 2. Obtener dirección del establecimiento
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe " +
					"JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", liquidacion.getPtoEmision());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();
			
			// 3. Obtener detalle de la liquidación
			String sqlDetalle = "SELECT d FROM DetalleLiquidacionCompra d WHERE d.liquidacion.id = :liquidacionId";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("liquidacionId", liquidacion.getId());
			@SuppressWarnings("unchecked")
			List<Object> detalles = queryDetalle.getResultList();
			
			// 4. Obtener formas de pago
			String sqlFormasPago = "SELECT fp FROM FormaPagoLiquidacion fp WHERE fp.liquidacion.id = :liquidacionId";
			Query queryFormasPago = em.createQuery(sqlFormasPago);
			queryFormasPago.setParameter("liquidacionId", liquidacion.getId());
			@SuppressWarnings("unchecked")
			List<Object> formasPago = queryFormasPago.getResultList();
			
			// 5. Generar XML
			String xmlContent = generarXMLContentLiquidacion(liquidacion, dirEstablecimiento, 
					detalles, formasPago, ambiente);
			
			// 6. Guardar archivo XML
			String pathRelativo = "resources/" + idFacturador + "/lqcs/g/" + clave + ".xml";
			String pathAbsoluto = System.getProperty("user.dir") + "/" + pathRelativo;
			
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			return new String[]{"OK", pathRelativo, pathAbsoluto};
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML Liquidacion: " + e.getMessage());
		}
	}
	
	private String generarXMLContentLiquidacion(LiquidacionCompra liquidacion, String dirEstablecimiento,
			List<Object> detalles, List<Object> formasPago, Long ambiente) throws Exception {
		
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		
		writer.writeStartElement("liquidacionCompra");
		writer.writeAttribute("id", "comprobante");
		writer.writeAttribute("version", "1.0.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writeInfoTributaria(writer, liquidacion, "03", ambiente);
		
		// infoLiquidacionCompra
		writer.writeCharacters("  ");
		writer.writeStartElement("infoLiquidacionCompra");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", liquidacion.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		
		String obligado = (liquidacion.getFacturador().getContabilidad() != null && 
				liquidacion.getFacturador().getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligado, 4);
		writeElement(writer, "tipoIdentificacionProveedor", String.valueOf(liquidacion.getTitular().getRubroTipoIdentificacionH()), 4);
		writeElement(writer, "razonSocialProveedor", nvl(liquidacion.getTitular().getNombre(), ""), 4);
		writeElement(writer, "identificacionProveedor", nvl(liquidacion.getTitular().getIdentificacion(), ""), 4);
		writeElement(writer, "direccionProveedor", nvl(liquidacion.getTitular().getDireccion(), ""), 4);
		
		Double totalSinImpuestos = sumNulls(liquidacion.getSubtotal(), liquidacion.getSubcero());
		writeElement(writer, "totalSinImpuestos", formatDecimal(totalSinImpuestos), 4);
		writeElement(writer, "totalDescuento", formatDecimal(nvl(liquidacion.getDescuento(), 0.0)), 4);
		
		// totalConImpuestos
		writeTotalConImpuestos(writer, liquidacion);
		
		writeElement(writer, "importeTotal", formatDecimal(liquidacion.getTotal()), 4);
		writeElement(writer, "moneda", "DOLAR", 4);
		
		// pagos
		writePagos(writer, formasPago, liquidacion.getTotal());
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoLiquidacionCompra
		writer.writeCharacters("\n");
		
		// detalles
		writeDetalles(writer, detalles);
		
		// infoAdicional
		writeInfoAdicional(writer, liquidacion);
		
		writer.writeEndElement(); // liquidacionCompra
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeInfoTributaria(XMLStreamWriter writer, LiquidacionCompra liquidacion, 
			String tipoDoc, Long ambiente) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", "1", 4);
		writeElement(writer, "razonSocial", nvl(liquidacion.getFacturador().getRazonSocial(), ""), 4);
		writeElement(writer, "nombreComercial", nvl(liquidacion.getFacturador().getNombre(), ""), 4);
		writeElement(writer, "ruc", nvl(liquidacion.getFacturador().getNumDoc(), ""), 4);
		writeElement(writer, "claveAcceso", nvl(liquidacion.getClave(), ""), 4);
		writeElement(writer, "codDoc", tipoDoc, 4);
		writeElement(writer, "estab", nvl(liquidacion.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi", nvl(liquidacion.getNumPtoEmision(), ""), 4);
		writeElement(writer, "secuencial", nvl(liquidacion.getSecuencial(), ""), 4);
		writeElement(writer, "dirMatriz", nvl(liquidacion.getFacturador().getDireccion(), ""), 4);
		
		if (liquidacion.getFacturador().getMicroEmpresa() != null && liquidacion.getFacturador().getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (liquidacion.getFacturador().getAgenteRetencion() != null && !liquidacion.getFacturador().getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", liquidacion.getFacturador().getAgenteRetencion(), 4);
		}
		if (liquidacion.getFacturador().getRimpe() != null && liquidacion.getFacturador().getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (liquidacion.getFacturador().getPopularRimpe() != null && liquidacion.getFacturador().getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeTotalConImpuestos(XMLStreamWriter writer, LiquidacionCompra liquidacion) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("totalConImpuestos");
		writer.writeCharacters("\n");
		
		if (liquidacion.getSubcero() != null && liquidacion.getSubcero() > 0) {
			writeTotalImpuesto(writer, "2", "0", liquidacion.getSubcero(), 0.0);
		}
		if (liquidacion.getvIVA() != null && liquidacion.getvIVA() > 0) {
			writeTotalImpuesto(writer, "2", "4", liquidacion.getSubtotal(), liquidacion.getvIVA());
		}
		
		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeTotalImpuesto(XMLStreamWriter writer, String codigo, String codigoPorcentaje, 
			Double baseImponible, Double valor) throws Exception {
		writer.writeCharacters("      ");
		writer.writeStartElement("totalImpuesto");
		writer.writeCharacters("\n");
		writeElement(writer, "codigo", codigo, 8);
		writeElement(writer, "codigoPorcentaje", codigoPorcentaje, 8);
		writeElement(writer, "baseImponible", formatDecimal(baseImponible), 8);
		writeElement(writer, "valor", formatDecimal(valor), 8);
		writer.writeCharacters("      ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writePagos(XMLStreamWriter writer, List<Object> formasPago, Double total) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("pagos");
		writer.writeCharacters("\n");
		
		if (formasPago.isEmpty()) {
			writer.writeCharacters("      ");
			writer.writeStartElement("pago");
			writer.writeCharacters("\n");
			writeElement(writer, "formaPago", "01", 8);
			writeElement(writer, "total", formatDecimal(total), 8);
			writer.writeCharacters("      ");
			writer.writeEndElement();
			writer.writeCharacters("\n");
		}
		
		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeDetalles(XMLStreamWriter writer, List<Object> detalles) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("detalles");
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, LiquidacionCompra liquidacion) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(liquidacion.getObservacion(), "") + "]");
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
	
	private Double nvl(Double value, Double defaultValue) {
		return value != null ? value : defaultValue;
	}
	
	private Double sumNulls(Double... values) {
		Double sum = 0.0;
		for (Double value : values) {
			if (value != null) {
				sum = sum + value;
			}
		}
		return sum;
	}
	
	private String formatDecimal(Double value) {
		if (value == null) {
			return "0.00";
		}
		return String.format("%.2f", value);
	}

	@Override
	public String autorizarLiquidacion(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoLiquidacion, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarLiquidacion con clave: " + clave);
		
		String respuesta = "";
		String baseDir = System.getProperty("user.dir");
		String resourcesPath = baseDir + "/resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado
			String strXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
			Path pathFirmado = Paths.get(resourcesPath + "/lqcs/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, strXML.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla ptlc (alterno=3)
			PathLiquidacionCompra pathF = new PathLiquidacionCompra();
			LiquidacionCompra liquidacion = liquidacionCompraDaoService.selectById(codigoLiquidacion, NombreEntidadesCobro.LIQUIDACION_COMPRA);
			pathF.setLiquidacion(liquidacion);
			pathF.setPath("resources/" + idFacturador + "/lqcs/f/" + clave + ".xml");
			pathF.setAlterno(3L); // 3 = XML firmado
			pathLiquidacionCompraDaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			liquidacion.setEstado(3L);
			liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
			
			if (conectaSRI == 1) {
				// 4. Llamar al Web Service 1 - Recepción
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					// Crear archivo de log WS1
					Path logWS1 = Paths.get(resourcesPath + "/lqcs/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					// Llamar servicio de recepción
					String contenidoXML = new String(Files.readAllBytes(pathFirmado), "UTF-8");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, contenidoXML, logWriter1);
					
					logWriter1.close();
					
					// Guardar XML enviado
					Path pathEnviado = Paths.get(resourcesPath + "/lqcs/e/" + clave + ".xml");
					Files.write(pathEnviado, contenidoXML.getBytes("UTF-8"));
					
					// Insertar path enviado en tabla ptlc (alterno=4)
					PathLiquidacionCompra pathE = new PathLiquidacionCompra();
					pathE.setLiquidacion(liquidacion);
					pathE.setPath("resources/" + idFacturador + "/lqcs/e/" + clave + ".xml");
					pathE.setAlterno(4L); // 4 = XML enviado
					pathLiquidacionCompraDaoService.save(pathE, null);
					
					// Actualizar estado a ENVIADA (estado=4)
					liquidacion.setEstado(4L);
					liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						// 5. Esperar 2 segundos
						Thread.sleep(2000);
						
						// 6. Llamar al Web Service 2 - Autorización
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								// Crear archivo de log WS2 autorizado
								Path logWS2A = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								// Guardar XML autorizado
								Path pathAutorizado = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								// Insertar path autorizado en tabla ptlc (alterno=5)
								PathLiquidacionCompra pathA = new PathLiquidacionCompra();
								pathA.setLiquidacion(liquidacion);
								pathA.setPath("resources/" + idFacturador + "/lqcs/a/" + clave + ".xml");
								pathA.setAlterno(5L); // 5 = XML autorizado
								pathLiquidacionCompraDaoService.save(pathA, null);
								
								// Actualizar estado a AUTORIZADA (estado=5, estadoEmision=1)
								liquidacion.setEstado(5L);
								liquidacion.setEstadoEmision(1L);
								liquidacion.setAutorizacion(resultado.numeroAutorizacion);
								liquidacion.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
								
								respuesta = resultado.estado;
								
								// Si es producción, actualizar contador de documentos emitidos
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								// NO AUTORIZADA
								Path logWS2N = Paths.get(resourcesPath + "/lqcs/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									// Guardar XML no autorizado
									Path pathNoAutorizado = Paths.get(resourcesPath + "/lqcs/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									// Insertar path no autorizado en tabla ptlc (alterno=6)
									PathLiquidacionCompra pathN = new PathLiquidacionCompra();
									pathN.setLiquidacion(liquidacion);
									pathN.setPath("resources/" + idFacturador + "/lqcs/n/" + clave + ".xml");
									pathN.setAlterno(6L); // 6 = XML no autorizado
									pathLiquidacionCompraDaoService.save(pathN, null);
								}
								
								// Actualizar estado a NO AUTORIZADA (estado=6, estadoEmision=2)
								liquidacion.setEstado(6L);
								liquidacion.setEstadoEmision(2L);
								liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							// Error en autorización
							Path logWS2Error = Paths.get(resourcesPath + "/lqcs/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							// Guardar XML en carpeta de no autorizados
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/lqcs/n/" + clave + ".xml"));
							
							// Actualizar estado a NO AUTORIZADA (estado=6, estadoEmision=2)
							liquidacion.setEstado(6L);
							liquidacion.setEstadoEmision(2L);
							liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						// Estado diferente a RECIBIDA
						respuesta = "Estado: " + estadoRecepcion;
						
						// Verificar si es clave ya registrada
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							liquidacion.setAutorizacion(clave);
							liquidacion.setFechaAutorizacion(liquidacion.getFecha().plusMinutes(1).plusSeconds(15));
							liquidacion.setEstado(5L);
							liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
						}
					}
					
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
				
			} else {
				respuesta = "Liquidacion Generada pero no enviada";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarLiquidacion: " + e.getMessage());
		}
		
		return respuesta;
	}
	
	/**
	 * Llama al servicio de recepción del SRI
	 */
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
	
	/**
	 * Llama al servicio de autorización del SRI
	 */
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
	
	/**
	 * Parsea la fecha de autorización del SRI
	 */
	private LocalDateTime parseFechaAutorizacion(String fechaStr) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			return LocalDateTime.parse(fechaStr, formatter);
		} catch (Exception e) {
			return LocalDateTime.now();
		}
	}
	
	/**
	 * Clase interna para resultado de autorización
	 */
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
