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
import com.saa.ejb.cxc.dao.NotaDebitoDaoService;
import com.saa.ejb.cxc.dao.PathNotaDebitoDaoService;
import com.saa.ejb.cxc.service.NotaDebitoService;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathNotaDebito;
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
public class NotaDebitoServiceImpl implements NotaDebitoService {
	@EJB
	private NotaDebitoDaoService notaDebitoDaoService;
	
	@EJB
	private PathNotaDebitoDaoService pathNotaDebitoDaoService;
	
	@PersistenceContext
	private EntityManager em;
	
	@Override
	public NotaDebito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById NotaDebito con id: " + id);
		return notaDebitoDaoService.selectById(id, NombreEntidadesCobro.NOTA_DEBITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de NotaDebitoService");
		NotaDebito entidad = new NotaDebito();
		for (Long registro : id) {
			notaDebitoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<NotaDebito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de NotaDebitoService");
		for (NotaDebito registro : lista) {
			notaDebitoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<NotaDebito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll NotaDebitoService");
		List<NotaDebito> result = notaDebitoDaoService.selectAll(NombreEntidadesCobro.NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total NotaDebito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public NotaDebito saveSingle(NotaDebito entidad) throws Throwable {
		System.out.println("saveSingle - NotaDebito");
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		}
		entidad = notaDebitoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<NotaDebito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria NotaDebitoService");
		List<NotaDebito> result = notaDebitoDaoService.selectByCriteria(datos, NombreEntidadesCobro.NOTA_DEBITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio NotaDebito no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public String[] generarXMLNotaDebito(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLNotaDebito con clave: " + clave + " y ambiente: " + ambiente);
		
		try {
			String sqlNotaDebito = "SELECT nd FROM NotaDebito nd WHERE nd.clave = :clave";
			Query query = em.createQuery(sqlNotaDebito);
			query.setParameter("clave", clave);
			NotaDebito notaDebito = (NotaDebito) query.getSingleResult();
			
			if (notaDebito == null) {
				throw new IncomeException("NotaDebito con clave " + clave + " no encontrada");
			}
			
			Long idFacturador = notaDebito.getFacturador().getId();
			
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe " +
					"JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", notaDebito.getPtoEmision());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();
			
			String sqlDetalle = "SELECT d FROM DetalleNotaDebito d WHERE d.notaDebito.id = :notaDebitoId";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("notaDebitoId", notaDebito.getId());
			@SuppressWarnings("unchecked")
			List<Object> detalles = queryDetalle.getResultList();
			
			String xmlContent = generarXMLContentNotaDebito(notaDebito, dirEstablecimiento, detalles, ambiente);
			
			String pathRelativo = "resources/" + idFacturador + "/ntdb/g/" + clave + ".xml";
			String pathAbsoluto = System.getProperty("user.dir") + "/" + pathRelativo;
			
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			return new String[]{"OK", pathRelativo, pathAbsoluto};
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML NotaDebito: " + e.getMessage());
		}
	}
	
	private String generarXMLContentNotaDebito(NotaDebito notaDebito, String dirEstablecimiento,
			List<Object> detalles, Long ambiente) throws Exception {
		
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		
		writer.writeStartElement("notaDebito");
		writer.writeAttribute("id", "comprobante");
		writer.writeAttribute("version", "1.0.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writeInfoTributaria(writer, notaDebito, "05", ambiente);
		
		// infoNotaDebito
		writer.writeCharacters("  ");
		writer.writeStartElement("infoNotaDebito");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", notaDebito.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		writeElement(writer, "tipoIdentificacionComprador", String.valueOf(notaDebito.getTitular().getRubroTipoIdentificacionH()), 4);
		writeElement(writer, "razonSocialComprador", nvl(notaDebito.getTitular().getNombre(), ""), 4);
		writeElement(writer, "identificacionComprador", nvl(notaDebito.getTitular().getIdentificacion(), ""), 4);
		
		if (notaDebito.getFacturador().getContribuyenteEspecial() != null && 
				!notaDebito.getFacturador().getContribuyenteEspecial().isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", notaDebito.getFacturador().getContribuyenteEspecial(), 4);
		}
		
		String obligado = (notaDebito.getFacturador().getContabilidad() != null && 
				notaDebito.getFacturador().getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligado, 4);
		
		writeElement(writer, "codDocModificado", nvl(notaDebito.getTipoDocModificado(), ""), 4);
		writeElement(writer, "numDocModificado", nvl(notaDebito.getNumDocModificado(), ""), 4);
		writeElement(writer, "fechaEmisionDocSustento", notaDebito.getFechaEmisionDM().format(dateFormatter), 4);
		
		Double totalSinImpuestos = sumNulls(notaDebito.getSubtotal(), notaDebito.getSubcero());
		writeElement(writer, "totalSinImpuestos", formatDecimal(totalSinImpuestos), 4);
		
		// impuestos
		writeImpuestos(writer, notaDebito);
		
		// motivos
		writeMotivos(writer, detalles);
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoNotaDebito
		writer.writeCharacters("\n");
		
		// infoAdicional
		writeInfoAdicional(writer, notaDebito);
		
		writer.writeEndElement(); // notaDebito
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeInfoTributaria(XMLStreamWriter writer, NotaDebito notaDebito, 
			String tipoDoc, Long ambiente) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", "1", 4);
		writeElement(writer, "razonSocial", nvl(notaDebito.getFacturador().getRazonSocial(), ""), 4);
		writeElement(writer, "nombreComercial", nvl(notaDebito.getFacturador().getNombre(), ""), 4);
		writeElement(writer, "ruc", nvl(notaDebito.getFacturador().getNumDoc(), ""), 4);
		writeElement(writer, "claveAcceso", nvl(notaDebito.getClave(), ""), 4);
		writeElement(writer, "codDoc", tipoDoc, 4);
		writeElement(writer, "estab", nvl(notaDebito.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi", nvl(notaDebito.getNumPtoEmision(), ""), 4);
		writeElement(writer, "secuencial", nvl(notaDebito.getSecuencial(), ""), 4);
		writeElement(writer, "dirMatriz", nvl(notaDebito.getFacturador().getDireccion(), ""), 4);
		
		if (notaDebito.getFacturador().getMicroEmpresa() != null && notaDebito.getFacturador().getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (notaDebito.getFacturador().getAgenteRetencion() != null && !notaDebito.getFacturador().getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", notaDebito.getFacturador().getAgenteRetencion(), 4);
		}
		if (notaDebito.getFacturador().getRimpe() != null && notaDebito.getFacturador().getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (notaDebito.getFacturador().getPopularRimpe() != null && notaDebito.getFacturador().getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeImpuestos(XMLStreamWriter writer, NotaDebito notaDebito) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");
		
		if (notaDebito.getSubcero() != null && notaDebito.getSubcero() > 0) {
			writeImpuesto(writer, "2", "0", "0.00", notaDebito.getSubcero(), 0.0);
		}
		if (notaDebito.getvIVA() != null && notaDebito.getvIVA() > 0) {
			writeImpuesto(writer, "2", "4", String.valueOf(notaDebito.getpIVA()), 
					notaDebito.getSubtotal(), notaDebito.getvIVA());
		}
		
		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeImpuesto(XMLStreamWriter writer, String codigo, String codigoPorcentaje, String tarifa,
			Double baseImponible, Double valor) throws Exception {
		writer.writeCharacters("      ");
		writer.writeStartElement("impuesto");
		writer.writeCharacters("\n");
		writeElement(writer, "codigo", codigo, 8);
		writeElement(writer, "codigoPorcentaje", codigoPorcentaje, 8);
		writeElement(writer, "tarifa", tarifa, 8);
		writeElement(writer, "baseImponible", formatDecimal(baseImponible), 8);
		writeElement(writer, "valor", formatDecimal(valor), 8);
		writer.writeCharacters("      ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeMotivos(XMLStreamWriter writer, List<Object> detalles) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("motivos");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, NotaDebito notaDebito) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(notaDebito.getObservacion(), "") + "]");
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
	
	/*private Double nvl(Double value, Double defaultValue) {
		return value != null ? value : defaultValue;
	}*/
	
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
	public String autorizarNotaDebito(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoNotaDebito, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarNotaDebito con clave: " + clave);
		
		String respuesta = "";
		String baseDir = System.getProperty("user.dir");
		String resourcesPath = baseDir + "/resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado
			String strXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
			Path pathFirmado = Paths.get(resourcesPath + "/ntdb/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, strXML.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla ptnd (alterno=3)
			PathNotaDebito pathF = new PathNotaDebito();
			NotaDebito notaDebito = notaDebitoDaoService.selectById(codigoNotaDebito, NombreEntidadesCobro.NOTA_DEBITO);
			pathF.setNotaDebito(notaDebito);
			pathF.setPath("resources/" + idFacturador + "/ntdb/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathNotaDebitoDaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			notaDebito.setEstado(3L);
			notaDebitoDaoService.save(notaDebito, notaDebito.getId());
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/ntdb/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					String contenidoXML = new String(Files.readAllBytes(pathFirmado), "UTF-8");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, contenidoXML, logWriter1);
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/ntdb/e/" + clave + ".xml");
					Files.write(pathEnviado, contenidoXML.getBytes("UTF-8"));
					
					PathNotaDebito pathE = new PathNotaDebito();
					pathE.setNotaDebito(notaDebito);
					pathE.setPath("resources/" + idFacturador + "/ntdb/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathNotaDebitoDaoService.save(pathE, null);
					
					notaDebito.setEstado(4L);
					notaDebitoDaoService.save(notaDebito, notaDebito.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						Thread.sleep(2000);
						
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								Path logWS2A = Paths.get(resourcesPath + "/ntdb/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								Path pathAutorizado = Paths.get(resourcesPath + "/ntdb/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								PathNotaDebito pathA = new PathNotaDebito();
								pathA.setNotaDebito(notaDebito);
								pathA.setPath("resources/" + idFacturador + "/ntdb/a/" + clave + ".xml");
								pathA.setAlterno(5L);
								pathNotaDebitoDaoService.save(pathA, null);
								
								notaDebito.setEstado(5L);
								notaDebito.setEstadoEmision(1L);
								notaDebito.setAutorizacion(resultado.numeroAutorizacion);
								notaDebito.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								notaDebitoDaoService.save(notaDebito, notaDebito.getId());
								
								respuesta = resultado.estado;
								
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								Path logWS2N = Paths.get(resourcesPath + "/ntdb/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									Path pathNoAutorizado = Paths.get(resourcesPath + "/ntdb/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									PathNotaDebito pathN = new PathNotaDebito();
									pathN.setNotaDebito(notaDebito);
									pathN.setPath("resources/" + idFacturador + "/ntdb/n/" + clave + ".xml");
									pathN.setAlterno(6L);
									pathNotaDebitoDaoService.save(pathN, null);
								}
								
								notaDebito.setEstado(6L);
								notaDebito.setEstadoEmision(2L);
								notaDebitoDaoService.save(notaDebito, notaDebito.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							Path logWS2Error = Paths.get(resourcesPath + "/ntdb/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/ntdb/n/" + clave + ".xml"));
							
							notaDebito.setEstado(6L);
							notaDebito.setEstadoEmision(2L);
							notaDebitoDaoService.save(notaDebito, notaDebito.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						respuesta = "Estado: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							notaDebito.setAutorizacion(clave);
							notaDebito.setFechaAutorizacion(notaDebito.getFecha().plusMinutes(1).plusSeconds(15));
							notaDebito.setEstado(5L);
							notaDebitoDaoService.save(notaDebito, notaDebito.getId());
						}
					}
					
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
				
			} else {
				respuesta = "Nota de Debito Generada pero no enviada";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarNotaDebito: " + e.getMessage());
		}
		
		return respuesta;
	}
	
	@Override
	public java.util.Map<String, Object> procesarNotaDebitoCompleta(NotaDebito notaDebito,
			java.util.List<com.saa.model.cxc.DetalleNotaDebito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		
		System.out.println("=== INICIO procesarNotaDebitoCompleta ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		
		try {
			// 1. Configurar valores por defecto
			if (ambiente == null) {
				ambiente = 1L; // PRUEBA
				System.out.println("Ambiente configurado automáticamente: PRUEBA (1)");
			}
			if (conectaSRI == null) {
				conectaSRI = 1L; // SI
				System.out.println("ConectaSRI configurado automáticamente: SI (1)");
			}
			if (pathLogo == null) {
				pathLogo = "resources/logos/logo_aso.png";
				System.out.println("PathLogo configurado automáticamente: " + pathLogo);
			}
			
			// 2. Grabar la nota de débito - USAR EL MISMO OBJETO
			System.out.println("Paso 1: Grabando nota de débito...");
			notaDebito = this.saveSingle(notaDebito);
			resultado.put("notaDebito", notaDebito);
			resultado.put("paso1_grabacion", "OK");
			System.out.println("Nota de débito grabada con ID: " + notaDebito.getId());
			
			// 2.5. Guardar los detalles de la nota de débito
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("Paso 1.5: Guardando " + detalles.size() + " detalles de nota de débito...");
				for (com.saa.model.cxc.DetalleNotaDebito detalle : detalles) {
					detalle.setNotaDebito(notaDebito);
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
			
			// 3. Obtener destinatario del comprador si no se proporcionó
			if (destinatario == null && notaDebito.getTitular() != null) {
				destinatario = notaDebito.getTitular().getEmail();
				System.out.println("Destinatario obtenido del comprador: " + destinatario);
			}
			
			// 4. Obtener la clave de acceso
			String clave = notaDebito.getClave();
			if (clave == null || clave.isEmpty()) {
				throw new Exception("La nota de débito no tiene clave de acceso");
			}
			resultado.put("claveAcceso", clave);
			System.out.println("Clave de acceso: " + clave);
			
			// 5. Generar XML
			System.out.println("Paso 2: Generando XML...");
			String[] resultadoXML = this.generarXMLNotaDebito(clave, ambiente);
			resultado.put("paso2_xml", "OK");
			resultado.put("xmlMensaje", resultadoXML[0]);
			resultado.put("xmlPathRelativo", resultadoXML[1]);
			resultado.put("xmlPathAbsoluto", resultadoXML[2]);
			System.out.println("XML generado: " + resultadoXML[0]);
			
			// 6. Leer el contenido del XML generado
			String xmlContent = new String(java.nio.file.Files.readAllBytes(
				java.nio.file.Paths.get(resultadoXML[2])), java.nio.charset.StandardCharsets.UTF_8);
			
			// 7. Autorizar ante el SRI
			System.out.println("Paso 3: Autorizando ante el SRI...");
			Long idFacturador = notaDebito.getFacturador() != null ? 
				notaDebito.getFacturador().getId() : null;
			
			if (idFacturador == null) {
				throw new Exception("La nota de débito no tiene facturador asociado");
			}
			
			String resultadoAutorizacion = this.autorizarNotaDebito(
				idFacturador, ambiente, conectaSRI, clave, 
				notaDebito.getId(), xmlContent, destinatario, pathLogo);
			
			resultado.put("paso3_autorizacion", "OK");
			resultado.put("autorizacionMensaje", resultadoAutorizacion);
			System.out.println("Autorización completada: " + resultadoAutorizacion);
			
			// 8. Resultado final
			resultado.put("exito", true);
			resultado.put("mensaje", "Nota de débito procesada completamente");
			resultado.put("idNotaDebito", notaDebito.getId());
			resultado.put("ambiente", ambiente);
			resultado.put("conectaSRI", conectaSRI);
			resultado.put("destinatario", destinatario);
			
			System.out.println("=== FIN procesarNotaDebitoCompleta - EXITOSO ===");
			
		} catch (Throwable e) {
			System.err.println("ERROR en procesarNotaDebitoCompleta: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("mensaje", "ERROR");
			resultado.put("error", e.getMessage());
			throw e;
		}
		
		return resultado;
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
