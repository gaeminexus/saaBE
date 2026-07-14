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
import com.saa.ejb.cxc.dao.NotaCreditoDaoService;
import com.saa.ejb.cxc.dao.PathNotaCreditoDaoService;
import com.saa.ejb.cxc.service.NotaCreditoService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathNotaCredito;
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
public class NotaCreditoServiceImpl implements NotaCreditoService {
	@EJB
	private NotaCreditoDaoService notaCreditoDaoService;
	
	@EJB
	private PathNotaCreditoDaoService pathNotaCreditoDaoService;
	
	@EJB
	private SignatureService signatureService;
	
	@PersistenceContext
	private EntityManager em;
	
	@Override
	public NotaCredito selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById NotaCredito con id: " + id);
		return notaCreditoDaoService.selectById(id, NombreEntidadesCobro.NOTA_CREDITO);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de NotaCreditoService");
		NotaCredito entidad = new NotaCredito();
		for (Long registro : id) {
			notaCreditoDaoService.remove(entidad, registro);
		}
	}
	@Override
	public void save(List<NotaCredito> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de NotaCreditoService");
		for (NotaCredito registro : lista) {
			notaCreditoDaoService.save(registro, registro.getId());
		}
	}
	@Override
	public List<NotaCredito> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll NotaCreditoService");
		List<NotaCredito> result = notaCreditoDaoService.selectAll(NombreEntidadesCobro.NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total NotaCredito no devolvio ningun registro");
		}
		return result;
	}
	@Override
	public NotaCredito saveSingle(NotaCredito entidad) throws Throwable {
		System.out.println("saveSingle - NotaCredito");
		
		// Si es una nueva nota de crédito, generar campos automáticos
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Validar que tenga los datos necesarios
			if (entidad.getPtoEmision() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la nota de crédito");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la nota de crédito");
			}
			
			// Constantes según SRI
			String tipoComprobante = "04"; // Nota de Crédito
			Long ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
			String tipoEmision = "1"; // Emisión Normal
			
			try {
				// 1. Obtener secuencial
				String secuencial = obtenerSecuencial(entidad.getPtoEmision().getId(), tipoComprobante);
				entidad.setSecuencial(secuencial);
				
				// 2. Generar número
				String numero = entidad.getNumEstablecimiento() + "-" + 
						entidad.getNumPtoEmision() + "-" + secuencial;
				entidad.setNumero(numero);
				System.out.println("Número de nota de crédito generado: " + numero);
				
				// 3. Generar clave de acceso
				String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
				entidad.setClave(clave);
				System.out.println("Clave de acceso generada: " + clave);
				
				// 4. Establecer tipo de comprobante
				entidad.setTipoComprobante(tipoComprobante);
				
				// 5. Establecer estado de emisión inicial
				if (entidad.getEstadoEmision() == null) {
					entidad.setEstadoEmision(1L);
				}
				
			} catch (Exception e) {
				System.err.println("ERROR al generar campos automáticos de nota de crédito: " + e.getMessage());
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la nota de crédito: " + e.getMessage());
			}
		}
		
		entidad = notaCreditoDaoService.save(entidad, entidad.getId());
		return entidad;
	}
	@Override
	public List<NotaCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria NotaCreditoService");
		List<NotaCredito> result = notaCreditoDaoService.selectByCriteria(datos, NombreEntidadesCobro.NOTA_CREDITO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio NotaCredito no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public String[] generarXMLNotaCredito(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLNotaCredito con clave: " + clave + " y ambiente: " + ambiente);
		
		try {
			String sqlNotaCredito = "SELECT nc FROM NotaCredito nc WHERE nc.clave = :clave";
			Query query = em.createQuery(sqlNotaCredito);
			query.setParameter("clave", clave);
			NotaCredito notaCredito = (NotaCredito) query.getSingleResult();
			
			if (notaCredito == null) {
				throw new IncomeException("NotaCredito con clave " + clave + " no encontrada");
			}
			
			Long idFacturador = notaCredito.getFacturador().getId();
			
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe " +
					"JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
			Query queryEstab = em.createQuery(sqlEstab);
			queryEstab.setParameter("ptoEmisionId", notaCredito.getPtoEmision().getId());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();
			
			String sqlDetalle = "SELECT d FROM DetalleNotaCredito d WHERE d.notaCredito.id = :notaCreditoId";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("notaCreditoId", notaCredito.getId());
			@SuppressWarnings("unchecked")
			List<Object> detalles = queryDetalle.getResultList();
			
			String xmlContent = generarXMLContentNotaCredito(notaCredito, dirEstablecimiento, detalles, ambiente);
			
			String pathRelativo = "resources/" + idFacturador + "/ntcr/g/" + clave + ".xml";
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto = baseUploadDir + pathRelativo;
			
			System.out.println("Guardando XML NotaCredito en: " + pathAbsoluto);
			
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			System.out.println("✓ XML NotaCredito generado correctamente en: " + pathAbsoluto);
			
			return new String[]{"OK", pathRelativo, pathAbsoluto};
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error al generar XML NotaCredito: " + e.getMessage());
		}
	}
	
	private String generarXMLContentNotaCredito(NotaCredito notaCredito, String dirEstablecimiento,
			List<Object> detalles, Long ambiente) throws Exception {
		
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		// NO escribir declaración XML: el proceso de firma la agrega automáticamente
		
		writer.writeStartElement("notaCredito");
		writer.writeAttribute("id", "comprobante");  // SIEMPRE "comprobante" según estándar SRI
		writer.writeAttribute("version", "1.0.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writeInfoTributaria(writer, notaCredito, "04", ambiente);
		
		// infoNotaCredito
		writer.writeCharacters("  ");
		writer.writeStartElement("infoNotaCredito");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", notaCredito.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		writeElement(writer, "tipoIdentificacionComprador", String.valueOf(notaCredito.getTitular().getRubroTipoIdentificacionH()), 4);
		writeElement(writer, "razonSocialComprador", nvl(notaCredito.getTitular().getNombre(), ""), 4);
		writeElement(writer, "identificacionComprador", nvl(notaCredito.getTitular().getIdentificacion(), ""), 4);
		
		if (notaCredito.getFacturador().getContribuyenteEspecial() != null && 
				!notaCredito.getFacturador().getContribuyenteEspecial().isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", notaCredito.getFacturador().getContribuyenteEspecial(), 4);
		}
		
		String obligado = (notaCredito.getFacturador().getContabilidad() != null && 
				notaCredito.getFacturador().getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligado, 4);
		
		writeElement(writer, "codDocModificado", nvl(notaCredito.getTipoDocModificado(), ""), 4);
		writeElement(writer, "numDocModificado", nvl(notaCredito.getNumDocModificado(), ""), 4);
		writeElement(writer, "fechaEmisionDocSustento", notaCredito.getFechaEmisionDM().format(dateFormatter), 4);
		
		Double totalSinImpuestos = sumNulls(notaCredito.getSubtotal(), notaCredito.getSubcero());
		writeElement(writer, "totalSinImpuestos", formatDecimal(totalSinImpuestos), 4);
		writeElement(writer, "valorModificacion", formatDecimal(notaCredito.getTotal()), 4);
		writeElement(writer, "moneda", "DOLAR", 4);
		
		// totalConImpuestos
		writeTotalConImpuestos(writer, notaCredito);
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoNotaCredito
		writer.writeCharacters("\n");
		
		// detalles
		writeDetalles(writer, detalles);
		
		// infoAdicional
		writeInfoAdicional(writer, notaCredito);
		
		writer.writeEndElement(); // notaCredito
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeInfoTributaria(XMLStreamWriter writer, NotaCredito notaCredito, 
			String tipoDoc, Long ambiente) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", "1", 4);
		writeElement(writer, "razonSocial", nvl(notaCredito.getFacturador().getRazonSocial(), ""), 4);
		writeElement(writer, "nombreComercial", nvl(notaCredito.getFacturador().getNombre(), ""), 4);
		writeElement(writer, "ruc", nvl(notaCredito.getFacturador().getNumDoc(), ""), 4);
		writeElement(writer, "claveAcceso", nvl(notaCredito.getClave(), ""), 4);
		writeElement(writer, "codDoc", tipoDoc, 4);
		writeElement(writer, "estab", nvl(notaCredito.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi", nvl(notaCredito.getNumPtoEmision(), ""), 4);
		writeElement(writer, "secuencial", nvl(notaCredito.getSecuencial(), ""), 4);
		writeElement(writer, "dirMatriz", nvl(notaCredito.getFacturador().getDireccion(), ""), 4);
		
		if (notaCredito.getFacturador().getMicroEmpresa() != null && notaCredito.getFacturador().getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (notaCredito.getFacturador().getAgenteRetencion() != null && !notaCredito.getFacturador().getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", notaCredito.getFacturador().getAgenteRetencion(), 4);
		}
		if (notaCredito.getFacturador().getRimpe() != null && notaCredito.getFacturador().getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (notaCredito.getFacturador().getPopularRimpe() != null && notaCredito.getFacturador().getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeTotalConImpuestos(XMLStreamWriter writer, NotaCredito notaCredito) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("totalConImpuestos");
		writer.writeCharacters("\n");
		
		if (notaCredito.getSubcero() != null && notaCredito.getSubcero() > 0) {
			writeTotalImpuesto(writer, "2", "0", notaCredito.getSubcero(), 0.0);
		}
		if (notaCredito.getvIVA() != null && notaCredito.getvIVA() > 0) {
			writeTotalImpuesto(writer, "2", "4", notaCredito.getSubtotal(), notaCredito.getvIVA());
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
	
	private void writeDetalles(XMLStreamWriter writer, List<Object> detalles) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("detalles");
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, NotaCredito notaCredito) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(notaCredito.getObservacion(), "") + "]");
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
	public String autorizarNotaCredito(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoNotaCredito, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarNotaCredito con clave: " + clave);
		
		String respuesta = "";
		// Usar directorio base de uploads en lugar de user.dir
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/ntcr/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla ptnc (alterno=3)
			PathNotaCredito pathF = new PathNotaCredito();
			NotaCredito notaCredito = notaCreditoDaoService.selectById(codigoNotaCredito, NombreEntidadesCobro.NOTA_CREDITO);
			pathF.setNotaCredito(notaCredito);
			pathF.setPath("resources/" + idFacturador + "/ntcr/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathNotaCreditoDaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			notaCredito.setEstado(3L);
			notaCreditoDaoService.save(notaCredito, notaCredito.getId());
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/ntcr/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					// Leer bytes crudos del XML firmado (NO convertir a String, preserva la firma)
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					
					logWriter1.close();
					
					// Guardar copia exacta del XML enviado
					Path pathEnviado = Paths.get(resourcesPath + "/ntcr/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
					PathNotaCredito pathE = new PathNotaCredito();
					pathE.setNotaCredito(notaCredito);
					pathE.setPath("resources/" + idFacturador + "/ntcr/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathNotaCreditoDaoService.save(pathE, null);
					
					notaCredito.setEstado(4L);
					notaCreditoDaoService.save(notaCredito, notaCredito.getId());
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						Thread.sleep(2000);
						
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								Path logWS2A = Paths.get(resourcesPath + "/ntcr/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								Path pathAutorizado = Paths.get(resourcesPath + "/ntcr/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								PathNotaCredito pathA = new PathNotaCredito();
								pathA.setNotaCredito(notaCredito);
								pathA.setPath("resources/" + idFacturador + "/ntcr/a/" + clave + ".xml");
								pathA.setAlterno(5L);
								pathNotaCreditoDaoService.save(pathA, null);
								
								notaCredito.setEstado(5L);
								notaCredito.setEstadoEmision(1L);
								notaCredito.setAutorizacion(resultado.numeroAutorizacion);
								notaCredito.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								notaCreditoDaoService.save(notaCredito, notaCredito.getId());
								
								respuesta = resultado.estado;
								
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								Path logWS2N = Paths.get(resourcesPath + "/ntcr/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									Path pathNoAutorizado = Paths.get(resourcesPath + "/ntcr/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									PathNotaCredito pathN = new PathNotaCredito();
									pathN.setNotaCredito(notaCredito);
									pathN.setPath("resources/" + idFacturador + "/ntcr/n/" + clave + ".xml");
									pathN.setAlterno(6L);
									pathNotaCreditoDaoService.save(pathN, null);
								}
								
								notaCredito.setEstado(6L);
								notaCredito.setEstadoEmision(2L);
								notaCreditoDaoService.save(notaCredito, notaCredito.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							Path logWS2Error = Paths.get(resourcesPath + "/ntcr/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/ntcr/n/" + clave + ".xml"));
							
							notaCredito.setEstado(6L);
							notaCredito.setEstadoEmision(2L);
							notaCreditoDaoService.save(notaCredito, notaCredito.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						respuesta = "Estado: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							notaCredito.setAutorizacion(clave);
							notaCredito.setFechaAutorizacion(notaCredito.getFecha().plusMinutes(1).plusSeconds(15));
							notaCredito.setEstado(5L);
							notaCreditoDaoService.save(notaCredito, notaCredito.getId());
						}
					}
					
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
				
			} else {
				respuesta = "Nota de Credito Generada pero no enviada";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarNotaCredito: " + e.getMessage());
		}
		
		return respuesta;
	}
	
	@Override
	public java.util.Map<String, Object> procesarNotaCreditoCompleta(NotaCredito notaCredito,
			java.util.List<com.saa.model.cxc.DetalleNotaCredito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		
		System.out.println("=== INICIO procesarNotaCreditoCompleta ===");
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
			
			// 2. Grabar la nota de crédito - USAR EL MISMO OBJETO (genera campos automáticos)
			System.out.println("Paso 1: Grabando nota de crédito...");
			notaCredito = this.saveSingle(notaCredito);
			resultado.put("notaCredito", notaCredito);
			resultado.put("paso1_grabacion", "OK");
			System.out.println("✓ Nota de crédito grabada con ID: " + notaCredito.getId());
			System.out.println("✓ Clave generada: " + notaCredito.getClave());
			System.out.println("✓ Número generado: " + notaCredito.getNumero());
			System.out.println("✓ Secuencial generado: " + notaCredito.getSecuencial());
			
			// 2.5. Guardar los detalles de la nota de crédito
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("Paso 1.5: Guardando " + detalles.size() + " detalles de nota de crédito...");
				for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
					detalle.setNotaCredito(notaCredito);
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
			if (destinatario == null && notaCredito.getTitular() != null) {
				destinatario = notaCredito.getTitular().getEmail();
				System.out.println("Destinatario obtenido del comprador: " + destinatario);
			}
			
			// 4. Obtener la clave de acceso
			String clave = notaCredito.getClave();
			if (clave == null || clave.isEmpty()) {
				throw new Exception("La nota de crédito no tiene clave de acceso");
			}
			Long idFacturador = notaCredito.getFacturador() != null ? notaCredito.getFacturador().getId() : null;
			if (idFacturador == null) {
				throw new Exception("La nota de crédito no tiene facturador asociado");
			}
			resultado.put("claveAcceso", clave);
			System.out.println("Clave de acceso: " + clave);
			
			// 5. Generar XML
			System.out.println("Paso 2: Generando XML...");
			String[] resultadoXML = this.generarXMLNotaCredito(clave, ambiente);
			resultado.put("paso2_xml", "OK");
			resultado.put("xmlPathAbsoluto", resultadoXML[2]);
			System.out.println("XML generado: " + resultadoXML[0]);
			
			// 6. Leer XML sin firmar y firmarlo
			System.out.println("Paso 3: Firmando XML electrónicamente...");
			String xmlSinFirmar = new String(java.nio.file.Files.readAllBytes(
				java.nio.file.Paths.get(resultadoXML[2])), java.nio.charset.StandardCharsets.UTF_8);
			String xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
			resultado.put("paso3_firma", "OK");
			System.out.println("✓ XML firmado electrónicamente");
			
			// 7. Autorizar ante el SRI
			System.out.println("Paso 4: Autorizando ante el SRI...");
			String resultadoAutorizacion = this.autorizarNotaCredito(
				idFacturador, ambiente, conectaSRI, clave, 
				notaCredito.getId(), xmlFirmado, destinatario, pathLogo);
			
			resultado.put("paso3_autorizacion", "OK");
			resultado.put("autorizacionMensaje", resultadoAutorizacion);
			System.out.println("Autorización completada: " + resultadoAutorizacion);
			
			// 8. Resultado final
			resultado.put("exito", true);
			resultado.put("mensaje", "Nota de crédito procesada completamente");
			resultado.put("idNotaCredito", notaCredito.getId());
			resultado.put("ambiente", ambiente);
			resultado.put("conectaSRI", conectaSRI);
			resultado.put("destinatario", destinatario);
			
			System.out.println("=== FIN procesarNotaCreditoCompleta - EXITOSO ===");
			
		} catch (Throwable e) {
			System.err.println("ERROR en procesarNotaCreditoCompleta: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("mensaje", "ERROR");
			resultado.put("error", e.getMessage());
			throw e;
		}
		
		return resultado;
	}
	
	private String llamarRecepcionSRI(String url, byte[] xmlBytes, PrintWriter log) throws Exception {
		try {
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			
			SOAPEnvelope envelope = soapPart.getEnvelope();
			
			SOAPBody soapBody = envelope.getBody();
			SOAPElement validarComprobante = soapBody.addChildElement("validarComprobante", "", "http://ec.gob.sri.ws.recepcion");
			SOAPElement xml = validarComprobante.addChildElement(envelope.createName("xml", "", ""));
			
			// Codificar bytes raw en Base64 (NO convertir a String: preserva la firma)
			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			xml.addTextNode(xmlBase64);
			
			soapMessage.saveChanges();
			
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			soapResponse.writeTo(baos);
			String respuestaCompleta = baos.toString("UTF-8");
			log.println("Respuesta WS1: " + respuestaCompleta);
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() == 0) {
				estadoList = responseBody.getElementsByTagNameNS("*", "estado");
			}
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				
				NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
				if (mensajeList.getLength() == 0) {
					mensajeList = responseBody.getElementsByTagNameNS("*", "mensaje");
				}
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						soapConnection.close();
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				
				soapConnection.close();
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
			SOAPBody soapBody = envelope.getBody();
			SOAPElement autorizacionComprobante = soapBody.addChildElement("autorizacionComprobante", "", "http://ec.gob.sri.ws.autorizacion");
			SOAPElement claveAccesoElement = autorizacionComprobante.addChildElement(envelope.createName("claveAccesoComprobante", "", ""));
			claveAccesoElement.addTextNode(claveAcceso);
			
			soapMessage.saveChanges();
			
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			soapResponse.writeTo(baos);
			String respuestaCompleta = baos.toString("UTF-8");
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;
			
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() == 0) estadoList = responseBody.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() > 0) resultado.estado = estadoList.item(0).getTextContent();
			
			NodeList numAutList = responseBody.getElementsByTagName("numeroAutorizacion");
			if (numAutList.getLength() == 0) numAutList = responseBody.getElementsByTagNameNS("*", "numeroAutorizacion");
			if (numAutList.getLength() > 0) resultado.numeroAutorizacion = numAutList.item(0).getTextContent();
			
			NodeList fechaAutList = responseBody.getElementsByTagName("fechaAutorizacion");
			if (fechaAutList.getLength() == 0) fechaAutList = responseBody.getElementsByTagNameNS("*", "fechaAutorizacion");
			if (fechaAutList.getLength() > 0) resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();
			
			NodeList comprobanteList = responseBody.getElementsByTagName("comprobante");
			if (comprobanteList.getLength() == 0) comprobanteList = responseBody.getElementsByTagNameNS("*", "comprobante");
			if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();
			
			NodeList mensajeIdList = responseBody.getElementsByTagName("identificador");
			if (mensajeIdList.getLength() == 0) mensajeIdList = responseBody.getElementsByTagNameNS("*", "identificador");
			if (mensajeIdList.getLength() > 0) resultado.mensajeId = mensajeIdList.item(0).getTextContent();
			
			NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
			if (mensajeList.getLength() == 0) mensajeList = responseBody.getElementsByTagNameNS("*", "mensaje");
			if (mensajeList.getLength() > 0) resultado.mensaje = mensajeList.item(0).getTextContent();
			
			NodeList infoAdicionalList = responseBody.getElementsByTagName("informacionAdicional");
			if (infoAdicionalList.getLength() == 0) infoAdicionalList = responseBody.getElementsByTagNameNS("*", "informacionAdicional");
			if (infoAdicionalList.getLength() > 0) resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();
			
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
		
		String secuencial = String.format("%09d", numeroActual);
		System.out.println("Secuencial generado: " + secuencial);
		
		return secuencial;
	}
	
	/**
	 * Genera la clave de acceso usando el algoritmo módulo 11
	 */
	private String generarClaveAcceso(NotaCredito notaCredito, String tipoComprobante, Long ambiente, 
			String tipoEmision, String secuencial) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = notaCredito.getFecha().format(formatter);
		
		String ruc = notaCredito.getFacturador().getNumDoc();
		String codClave = notaCredito.getFacturador().getCodClave();
		
		System.out.println("RUC: " + ruc);
		System.out.println("CLAVE: " + codClave);
		
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente + 
				notaCredito.getNumEstablecimiento() + notaCredito.getNumPtoEmision() + 
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
}
