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
import com.saa.ejb.signature.service.SignatureService;
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
	
	@EJB
	private SignatureService signatureService;

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

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
		
		// Si es una nueva nota de débito, generar campos automáticos
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Validar que tenga los datos necesarios
			if (entidad.getPtoEmision() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la nota de débito");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la nota de débito");
			}
			
			// Constantes según SRI
			String tipoComprobante = "05"; // Nota de Débito
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
				System.out.println("Número de nota de débito generado: " + numero);
				
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
				System.err.println("ERROR al generar campos automáticos de nota de débito: " + e.getMessage());
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la nota de débito: " + e.getMessage());
			}
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
			queryEstab.setParameter("ptoEmisionId", notaDebito.getPtoEmision().getId());
			String dirEstablecimiento = (String) queryEstab.getSingleResult();
			
			String sqlDetalle = "SELECT d FROM DetalleNotaDebito d WHERE d.notaDebito.id = :notaDebitoId";
			Query queryDetalle = em.createQuery(sqlDetalle);
			queryDetalle.setParameter("notaDebitoId", notaDebito.getId());
			@SuppressWarnings("unchecked")
			List<Object> detalles = queryDetalle.getResultList();
			
			String xmlContent = generarXMLContentNotaDebito(notaDebito, dirEstablecimiento, detalles, ambiente);
			
			String pathRelativo = "resources/" + idFacturador + "/ntdb/g/" + clave + ".xml";
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto = baseUploadDir + pathRelativo;
			
			System.out.println("Guardando XML NotaDebito en: " + pathAbsoluto);
			
			Path path = Paths.get(pathAbsoluto);
			Files.createDirectories(path.getParent());
			Files.write(path, xmlContent.getBytes("UTF-8"));
			
			System.out.println("✓ XML NotaDebito generado correctamente en: " + pathAbsoluto);
			
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
		
		// NO escribir declaración XML: el proceso de firma la agrega automáticamente
		
		writer.writeStartElement("notaDebito");
		writer.writeAttribute("id", "comprobante");  // SIEMPRE "comprobante" según estándar SRI
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
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/ntdb/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
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
					
					// Leer bytes crudos del XML firmado (NO convertir a String, preserva la firma)
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					
					logWriter1.close();
					
					// Guardar copia exacta del XML enviado
					Path pathEnviado = Paths.get(resourcesPath + "/ntdb/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
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
			if (ambiente == null) ambiente = 1L;
			if (conectaSRI == null) conectaSRI = 1L;
			if (pathLogo == null) pathLogo = "resources/logos/logo_aso.png";
			
			System.out.println("Paso 1: Grabando nota de débito...");
			notaDebito = this.saveSingle(notaDebito);
			resultado.put("notaDebito", notaDebito);
			System.out.println("✓ Nota de débito grabada con ID: " + notaDebito.getId());
			System.out.println("✓ Clave generada: " + notaDebito.getClave());
			
			if (detalles != null && !detalles.isEmpty()) {
				for (com.saa.model.cxc.DetalleNotaDebito detalle : detalles) {
					detalle.setNotaDebito(notaDebito);
					if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(Estado.ACTIVO));
					em.persist(detalle);
				}
				em.flush();
			}
			
			if (destinatario == null && notaDebito.getTitular() != null) {
				destinatario = notaDebito.getTitular().getEmail();
			}
			
			String clave = notaDebito.getClave();
			if (clave == null || clave.isEmpty()) throw new Exception("La nota de débito no tiene clave de acceso");
			Long idFacturador = notaDebito.getFacturador() != null ? notaDebito.getFacturador().getId() : null;
			if (idFacturador == null) throw new Exception("La nota de débito no tiene facturador asociado");
			resultado.put("claveAcceso", clave);
			
			// Paso 2: Generar XML
			System.out.println("Paso 2: Generando XML...");
			String[] resultadoXML = this.generarXMLNotaDebito(clave, ambiente);
			resultado.put("paso2_xml", "OK");
			System.out.println("XML generado: " + resultadoXML[0]);
			
			// Paso 3: Firmar XML
			System.out.println("Paso 3: Firmando XML electrónicamente...");
			String xmlSinFirmar = new String(java.nio.file.Files.readAllBytes(
				java.nio.file.Paths.get(resultadoXML[2])), java.nio.charset.StandardCharsets.UTF_8);
			String xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
			resultado.put("paso3_firma", "OK");
			System.out.println("✓ XML firmado electrónicamente");
			
			// Paso 4: Autorizar ante el SRI
			System.out.println("Paso 4: Autorizando ante el SRI...");
			String resultadoAutorizacion = this.autorizarNotaDebito(
				idFacturador, ambiente, conectaSRI, clave,
				notaDebito.getId(), xmlFirmado, destinatario, pathLogo);
			
			resultado.put("autorizacionMensaje", resultadoAutorizacion);

			// ── PASO 5: Generar asiento contable ───────────────────────────────
			// Solo si el facturador tiene generaConta=1 y empresa contable configurada.
			if (notaDebito.getFacturador() != null
					&& notaDebito.getFacturador().getEmpresa() != null
					&& Long.valueOf(1L).equals(notaDebito.getFacturador().getGeneraConta())) {
				System.out.println("PASO 5: Generando asiento contable para Nota de Débito...");
				try {
					Long idEmpresaConta = notaDebito.getFacturador().getEmpresa().getCodigo();
					NotaDebito ndActualizada = notaDebitoDaoService.selectById(
							notaDebito.getId(), NombreEntidadesCobro.NOTA_DEBITO);
					java.time.LocalDate fechaAsiento = ndActualizada.getFecha() != null
							? ndActualizada.getFecha().toLocalDate() : java.time.LocalDate.now();
					String obsAsiento = "Nota de Débito N° " + nvl(ndActualizada.getNumero(), clave)
							+ " | Cliente: " + (ndActualizada.getTitular() != null ? ndActualizada.getTitular().getNombre() : "")
							+ " | Aut: " + nvl(ndActualizada.getAutorizacion(), clave);
					String usuarioAsiento = (ndActualizada.getUsuario() != null)
							? ndActualizada.getUsuario().getNombre() : "SISTEMA";
					// TODO: Reemplazar TipoAsientos.NOTAS_DEBITO_VENTA con el codigoAlterno
					//       correcto una vez que se defina la plantilla en BD.
					com.saa.model.cnt.Asiento asientoGenerado =
							asientoContableService.generarAsientoNotaDebito(
									ndActualizada.getId(), idEmpresaConta,
									com.saa.rubros.TipoAsientos.NOTAS_DEBITO_VENTA,
									fechaAsiento, obsAsiento, usuarioAsiento);
					resultado.put("asiento", asientoGenerado.getNumeroAlterno());
					System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());
				} catch (Exception e) {
					resultado.put("advertenciaAsiento",
							"Nota de Débito autorizada pero ocurrió un error al generar el asiento: "
							+ e.getMessage()
							+ ". Genere el asiento manualmente desde Contabilidad.");
					System.err.println("⚠ Error en asiento contable de Nota de Débito: " + e.getMessage());
					e.printStackTrace();
				}
			}

			resultado.put("exito", true);
			resultado.put("mensaje", "Nota de débito procesada completamente");
			resultado.put("idNotaDebito", notaDebito.getId());
			
		} catch (Throwable e) {
			System.err.println("ERROR en procesarNotaDebitoCompleta: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
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
			
			// Codificar bytes raw en Base64 (preserva la firma)
			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			xml.addTextNode(xmlBase64);
			soapMessage.saveChanges();
			
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			soapResponse.writeTo(baos);
			log.println("Respuesta WS1: " + baos.toString("UTF-8"));
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() == 0) estadoList = responseBody.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
				if (mensajeList.getLength() == 0) mensajeList = responseBody.getElementsByTagNameNS("*", "mensaje");
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
	private String generarClaveAcceso(NotaDebito notaDebito, String tipoComprobante, Long ambiente, 
			String tipoEmision, String secuencial) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = notaDebito.getFecha().format(formatter);
		
		String ruc = notaDebito.getFacturador().getNumDoc();
		String codClave = notaDebito.getFacturador().getCodClave();
		
		System.out.println("RUC: " + ruc);
		System.out.println("CLAVE: " + codClave);
		
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente + 
				notaDebito.getNumEstablecimiento() + notaDebito.getNumPtoEmision() + 
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
