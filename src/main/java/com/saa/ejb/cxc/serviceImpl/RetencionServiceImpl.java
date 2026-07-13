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
import com.saa.ejb.cxc.dao.RetencionDaoService;
import com.saa.ejb.cxc.dao.PathRetencionDaoService;
import com.saa.ejb.cxc.service.RetencionService;
import com.saa.model.cxc.Retencion;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathRetencion;
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
public class RetencionServiceImpl implements RetencionService {
	@EJB
	private RetencionDaoService retencionDaoService;
	
	@EJB
	private PathRetencionDaoService pathRetencionDaoService;
	
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
		
		// Si es una nueva retención, generar campos automáticos
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Validar que tenga los datos necesarios
			if (entidad.getPtoEmision() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la retención");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la retención");
			}
			
			// Constantes según SRI
			String tipoComprobante = "07"; // Retención
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
				System.out.println("Número de retención generado: " + numero);
				
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
				System.err.println("ERROR al generar campos automáticos de retención: " + e.getMessage());
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
			
			if (retencion == null) {
				throw new IncomeException("Retencion con clave " + clave + " no encontrada");
			}
			
			Long idFacturador = retencion.getFacturador().getId();
			
			String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe " +
					"JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
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
			
			System.out.println("Guardando XML Retencion en: " + pathAbsoluto);
			
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
		
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		
		writer.writeStartElement("comprobanteRetencion");
		writer.writeAttribute("id", retencion.getClave());  // Usar clave de acceso como ID
		writer.writeAttribute("version", "1.0.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writeInfoTributaria(writer, retencion, "07", ambiente);
		
		// infoCompRetencion
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
		
		// impuestos
		writeImpuestos(writer, detalles);
		
		// infoAdicional
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
	
	private void writeImpuestos(XMLStreamWriter writer, List<Object> detalles) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, Retencion retencion) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(retencion.getObservacion(), "") + "]");
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
	
	/*private String formatDecimal(Double value) {
		if (value == null) {
			return "0.00";
		}
		return String.format("%.2f", value);
	}*/
	
	@Override
	public String autorizarRetencion(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarRetencion con clave: " + clave);
		
		String respuesta = "";
		// Usar directorio base de uploads en lugar de user.dir
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado
			String strXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
			Path pathFirmado = Paths.get(resourcesPath + "/rtnc/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, strXML.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla ptrt (alterno=3)
			PathRetencion pathF = new PathRetencion();
			Retencion retencion = retencionDaoService.selectById(codigoRetencion, NombreEntidadesCobro.RETENCION);
			pathF.setRetencion(retencion);
			pathF.setPath("resources/" + idFacturador + "/rtnc/f/" + clave + ".xml");
			pathF.setAlterno(3L);
			pathRetencionDaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
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
					
					String contenidoXML = new String(Files.readAllBytes(pathFirmado), "UTF-8");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, contenidoXML, logWriter1);
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/rtnc/e/" + clave + ".xml");
					Files.write(pathEnviado, contenidoXML.getBytes("UTF-8"));
					
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
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
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
	
	@Override
	public java.util.Map<String, Object> procesarRetencionCompleta(Retencion retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		
		System.out.println("=== INICIO procesarRetencionCompleta ===");
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
			
			// 2. Grabar la retención - USAR EL MISMO OBJETO (genera campos automáticos)
			System.out.println("Paso 1: Grabando retención...");
			retencion = this.saveSingle(retencion);
			resultado.put("retencion", retencion);
			resultado.put("paso1_grabacion", "OK");
			System.out.println("✓ Retención grabada con ID: " + retencion.getId());
			System.out.println("✓ Clave generada: " + retencion.getClave());
			System.out.println("✓ Número generado: " + retencion.getNumero());
			System.out.println("✓ Secuencial generado: " + retencion.getSecuencial());
			
			// 2.5. Guardar los detalles de la retención
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("Paso 1.5: Guardando " + detalles.size() + " detalles de retención...");
				for (com.saa.model.cxc.DetalleRetencion detalle : detalles) {
					detalle.setRetencion(retencion);
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
			
			// 3. Obtener destinatario del proveedor si no se proporcionó
			if (destinatario == null && retencion.getProveedor() != null) {
				destinatario = retencion.getProveedor().getEmail();
				System.out.println("Destinatario obtenido del proveedor: " + destinatario);
			}
			
			// 4. Obtener la clave de acceso
			String clave = retencion.getClave();
			if (clave == null || clave.isEmpty()) {
				throw new Exception("La retención no tiene clave de acceso");
			}
			resultado.put("claveAcceso", clave);
			System.out.println("Clave de acceso: " + clave);
			
			// 5. Generar XML
			System.out.println("Paso 2: Generando XML...");
			String[] resultadoXML = this.generarXMLRetencion(clave, ambiente);
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
			Long idFacturador = retencion.getFacturador() != null ? 
				retencion.getFacturador().getId() : null;
			
			if (idFacturador == null) {
				throw new Exception("La retención no tiene facturador asociado");
			}
			
			String resultadoAutorizacion = this.autorizarRetencion(
				idFacturador, ambiente, conectaSRI, clave, 
				retencion.getId(), xmlContent, destinatario, pathLogo);
			
			resultado.put("paso3_autorizacion", "OK");
			resultado.put("autorizacionMensaje", resultadoAutorizacion);
			System.out.println("Autorización completada: " + resultadoAutorizacion);
			
			// 8. Resultado final
			resultado.put("exito", true);
			resultado.put("mensaje", "Retención procesada completamente");
			resultado.put("idRetencion", retencion.getId());
			resultado.put("ambiente", ambiente);
			resultado.put("conectaSRI", conectaSRI);
			resultado.put("destinatario", destinatario);
			
			System.out.println("=== FIN procesarRetencionCompleta - EXITOSO ===");
			
		} catch (Throwable e) {
			System.err.println("ERROR en procesarRetencionCompleta: " + e.getMessage());
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
}
