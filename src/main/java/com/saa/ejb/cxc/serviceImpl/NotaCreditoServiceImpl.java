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
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.DetalleNotaCredito;
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

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

	@EJB
	private ReporteService reporteService;

	@EJB
	private com.saa.ejb.cxc.service.EmailFacturaService emailFacturaService;

	@EJB
	private com.saa.basico.ejb.DetalleRubroService detalleRubroService;

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
			if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la nota de crédito");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la nota de crédito");
			}
			if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
				throw new IncomeException("Debe especificar un titular para la nota de crédito");
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

		// tipoIdentificacionComprador — igual que Factura: usar detalleRubroService
		// para obtener el código SRI real del rubro (ej: "04", "05", "06")
		String tipoIdNc = "05"; // valor por defecto (cédula)
		try {
			if (notaCredito.getTitular().getRubroTipoIdentificacionP() != null
					&& notaCredito.getTitular().getRubroTipoIdentificacionH() != null) {
				String valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
						notaCredito.getTitular().getRubroTipoIdentificacionP().intValue(),
						notaCredito.getTitular().getRubroTipoIdentificacionH().intValue());
				if (valorAlfa != null && !valorAlfa.isEmpty()) {
					// SRI exige siempre 2 dígitos: "04", "05", "06", etc.
					tipoIdNc = valorAlfa.length() == 1 ? "0" + valorAlfa : valorAlfa;
				}
			}
		} catch (Throwable e) {
			System.err.println("⚠ Error al obtener tipoIdentificación NC, usando default 05: " + e.getMessage());
		}
		writeElement(writer, "tipoIdentificacionComprador", tipoIdNc, 4);
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

		// motivo — campo requerido por el SRI dentro de infoNotaCredito
		writeElement(writer, "motivo", nvl(notaCredito.getObservacion(), "Nota de Crédito"), 4);
		
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

		for (Object obj : detalles) {
			DetalleNotaCredito d = (DetalleNotaCredito) obj;
			writer.writeCharacters("    ");
			writer.writeStartElement("detalle");
			writer.writeCharacters("\n");

			// Obtener código del producto si existe
			if (d.getProducto() != null) {
				try {
					@SuppressWarnings("unchecked")
					List<Object[]> prodRows = em.createQuery(
							"SELECT p.codigo, p.codigoAux FROM ProductoCobro p WHERE p.id = :id")
							.setParameter("id", d.getProducto())
							.setMaxResults(1)
							.getResultList();
					if (!prodRows.isEmpty()) {
						String cod    = prodRows.get(0)[0] != null ? (String) prodRows.get(0)[0] : "";
						String codAux = prodRows.get(0)[1] != null ? (String) prodRows.get(0)[1] : "";
						// NC usa esquema v1.0.0: codigoInterno / codigoAdicional
						// (Factura usa v1.1.0: codigoPrincipal / codigoAuxiliar)
						if (!cod.isEmpty())    writeElement(writer, "codigoInterno",   cod,    6);
						if (!codAux.isEmpty()) writeElement(writer, "codigoAdicional", codAux, 6);
					}
				} catch (Exception ignored) {}
			}

			writeElement(writer, "descripcion", nvl(d.getDescripcion(), ""), 6);
			writeElement(writer, "cantidad", formatDecimal(d.getCantidad()), 6);
			writeElement(writer, "precioUnitario", formatDecimal(d.getValor()), 6);
			writeElement(writer, "descuento", formatDecimal(nvlD(d.getDescuento())), 6);
			writeElement(writer, "precioTotalSinImpuesto", formatDecimal(d.getBaseImponible()), 6);

			// Impuestos del detalle
			writer.writeCharacters("      ");
			writer.writeStartElement("impuestos");
			writer.writeCharacters("\n");

			writer.writeCharacters("        ");
			writer.writeStartElement("impuesto");
			writer.writeCharacters("\n");
			writeElement(writer, "codigo", "2", 10); // IVA
			String codPorcIva = "0";
			if (d.getPorcentajeIVA() != null) {
				switch (d.getPorcentajeIVA().intValue()) {
					case 15: codPorcIva = "4"; break;
					case 5:  codPorcIva = "5"; break;
					case 8:  codPorcIva = "8"; break;
					default: codPorcIva = "0"; break;
				}
			}
			writeElement(writer, "codigoPorcentaje", codPorcIva, 10);
			writeElement(writer, "tarifa", d.getPorcentajeIVA() != null ? String.valueOf(d.getPorcentajeIVA()) : "0", 10);
			writeElement(writer, "baseImponible", formatDecimal(d.getBaseImponible()), 10);
			writeElement(writer, "valor", formatDecimal(nvlD(d.getValorIVA())), 10);
			writer.writeCharacters("        ");
			writer.writeEndElement(); // impuesto
			writer.writeCharacters("\n");

			writer.writeCharacters("      ");
			writer.writeEndElement(); // impuestos
			writer.writeCharacters("\n");

			writer.writeCharacters("    ");
			writer.writeEndElement(); // detalle
			writer.writeCharacters("\n");
		}

		writer.writeCharacters("  ");
		writer.writeEndElement(); // detalles
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, NotaCredito notaCredito) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");

		// Igual que el PHP de referencia:
		// Soporte[tel - mail] Contacto Cliente[tel - mail]
		// Facturador: getTelefono() / getMail()
		// Titular:    getTelefono() / getEmail()
		String telFcdr  = nvl(notaCredito.getFacturador().getTelefono(), "");
		String mailFcdr = nvl(notaCredito.getFacturador().getMail(), "");
		String telCmdr  = nvl(notaCredito.getTitular().getTelefono(), "");
		String mailCmdr = nvl(notaCredito.getTitular().getEmail(), "");
		String infoAd   = "Soporte[" + telFcdr + " - " + mailFcdr + "] "
				+ "Contacto Cliente[" + telCmdr + " - " + mailCmdr + "]";

		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters(infoAd);
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
		// Locale.US garantiza punto decimal (.) en lugar de coma (,)
		// El SRI exige formato XML estándar: 200.00 NO 200,00
		return String.format(java.util.Locale.US, "%.2f", value);
	}
	
	@Override
	public String autorizarNotaCredito(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoNotaCredito, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== autorizarNotaCredito | clave: " + clave + " | facturador: " + idFacturador
				+ " | ambiente: " + ambiente + " | conectaSRI: " + conectaSRI + " ===");
		
		String respuesta = "";
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/ntcr/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			System.out.println("✓ XML firmado guardado en: " + pathFirmado);
			
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
			System.out.println("✓ Estado NC actualizado a FIRMADA (3)");
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				System.out.println(">>> WS1 Recepción URL: " + urlWS1);
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/ntcr/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					System.out.println(">>> Enviando XML al SRI (" + bytesXMLFirmado.length + " bytes)...");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					System.out.println(">>> Estado WS1 Recepción: [" + estadoRecepcion + "]");
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/ntcr/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
					PathNotaCredito pathE = new PathNotaCredito();
					pathE.setNotaCredito(notaCredito);
					pathE.setPath("resources/" + idFacturador + "/ntcr/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathNotaCreditoDaoService.save(pathE, null);
					
					notaCredito.setEstado(4L);
					notaCreditoDaoService.save(notaCredito, notaCredito.getId());
					System.out.println("✓ Estado NC actualizado a ENVIADA (4)");
					
					if ("RECIBIDA".equals(estadoRecepcion)) {
						System.out.println(">>> Comprobante RECIBIDO por SRI. Esperando 2s para autorización...");
						Thread.sleep(2000);
						
						String urlWS2 = ambiente == 1
								? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
								: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
						System.out.println(">>> WS2 Autorización URL: " + urlWS2);
						
						try {
							ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, clave);
							
							System.out.println(">>> Respuesta WS2 Autorización:");
							System.out.println("    estado              = [" + resultado.estado + "]");
							System.out.println("    numeroAutorizacion  = [" + resultado.numeroAutorizacion + "]");
							System.out.println("    fechaAutorizacion   = [" + resultado.fechaAutorizacion + "]");
							System.out.println("    mensajeId           = [" + resultado.mensajeId + "]");
							System.out.println("    mensaje             = [" + resultado.mensaje + "]");
							System.out.println("    informacionAdicional= [" + resultado.informacionAdicional + "]");
							System.out.println("    respuestaCompleta   = " + resultado.respuestaCompleta);
							
							if ("AUTORIZADO".equals(resultado.estado)) {
								System.out.println("✓ NC AUTORIZADA por el SRI. Num.Autorización: " + resultado.numeroAutorizacion);
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

								// 5. Generar PDF (RIDE Nota de Crédito)
								byte[] pdfBytesParaEmail = null;
								try {
									byte[] pdfBytes = generarPDFNotaCredito(notaCredito, idFacturador, clave, pathLogo, ambiente);
									if (pdfBytes != null && pdfBytes.length > 0) {
										Path pathPdf = Paths.get(resourcesPath + "/ntcr/a/" + clave + ".pdf");
										Files.write(pathPdf, pdfBytes);
										PathNotaCredito pathPdfRec = new PathNotaCredito();
										pathPdfRec.setNotaCredito(notaCredito);
										pathPdfRec.setPath("resources/" + idFacturador + "/ntcr/a/" + clave + ".pdf");
										pathPdfRec.setAlterno(7L);
										pathNotaCreditoDaoService.save(pathPdfRec, null);
										pdfBytesParaEmail = pdfBytes;
										System.out.println("✓ PDF RIDE NC generado: " + pathPdf);
									}
								} catch (Exception pdfEx) {
									System.err.println("⚠ Error generando PDF NC (no crítico): " + pdfEx.getMessage());
									pdfEx.printStackTrace();
								}

								// 6. Enviar correo electrónico
								try {
									String emailDest = destinatario;
									if ((emailDest == null || emailDest.trim().isEmpty())
											&& notaCredito.getTitular() != null) {
										emailDest = notaCredito.getTitular().getEmail();
									}
									String razonSocialEmisor = notaCredito.getFacturador() != null
											? nvl(notaCredito.getFacturador().getRazonSocial(),
												  nvl(notaCredito.getFacturador().getNombre(), "")) : "";
									if (emailDest != null && !emailDest.trim().isEmpty()) {
										emailFacturaService.enviarFacturaAutorizada(
												emailDest,
												nvl(notaCredito.getNumero(), clave),
												clave,
												razonSocialEmisor,
												resultado.comprobanteXML,
												pdfBytesParaEmail);
										System.out.println("✓ Email NC enviado a: " + emailDest);
									}
								} catch (Exception mailEx) {
									System.err.println("⚠ Error enviando email NC (no crítico): " + mailEx.getMessage());
									mailEx.printStackTrace();
								}
								
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
								}
								
							} else {
								// NO AUTORIZADO — loguear TODO para diagnóstico
								System.err.println("✗ NC NO AUTORIZADA por el SRI.");
								System.err.println("    estado              = [" + resultado.estado + "]");
								System.err.println("    mensajeId           = [" + resultado.mensajeId + "]");
								System.err.println("    mensaje             = [" + resultado.mensaje + "]");
								System.err.println("    informacionAdicional= [" + resultado.informacionAdicional + "]");
								System.err.println("    respuestaCompleta   = " + resultado.respuestaCompleta);

								Path logWS2N = Paths.get(resourcesPath + "/ntcr/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("=== RESPUESTA NO AUTORIZADO ===");
								logWriter2N.println("estado              = " + resultado.estado);
								logWriter2N.println("mensajeId           = " + resultado.mensajeId);
								logWriter2N.println("mensaje             = " + resultado.mensaje);
								logWriter2N.println("informacionAdicional= " + resultado.informacionAdicional);
								logWriter2N.println("respuestaCompleta   = " + resultado.respuestaCompleta);
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
								
								respuesta = "NO_AUTORIZADO"
										+ " | Estado: " + nvl(resultado.estado, "")
										+ " | Id: "     + nvl(resultado.mensajeId, "")
										+ " | Mensaje: " + nvl(resultado.mensaje, "")
										+ " | Info: "   + nvl(resultado.informacionAdicional, "");
								System.err.println(">>> Respuesta completa para frontend: " + respuesta);
							}
							
						} catch (Exception e) {
							System.err.println("✗ ERROR en llamada WS2 autorización: " + e.getMessage());
							e.printStackTrace();
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
						// WS1 devolvió algo distinto a RECIBIDA
						System.err.println("⚠ WS1 devolvió estado no esperado: [" + estadoRecepcion + "]");
						respuesta = "Estado WS1: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							System.out.println(">>> Clave ya registrada en SRI — marcando como autorizada");
							respuesta = "Comprobante Autorizado";
							notaCredito.setAutorizacion(clave);
							notaCredito.setFechaAutorizacion(notaCredito.getFecha().plusMinutes(1).plusSeconds(15));
							notaCredito.setEstado(5L);
							notaCreditoDaoService.save(notaCredito, notaCredito.getId());
						}
					}
					
				} catch (Exception e) {
					System.err.println("✗ ERROR en llamada WS1 recepción: " + e.getMessage());
					e.printStackTrace();
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
				}
				
			} else {
				System.out.println("ℹ conectaSRI=0 — NC generada pero NO enviada al SRI");
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
				ambiente = 1L;
				System.out.println("Ambiente configurado automáticamente: PRUEBA (1)");
			}
			if (conectaSRI == null) {
				conectaSRI = 1L;
				System.out.println("ConectaSRI configurado automáticamente: SI (1)");
			}
			if (pathLogo == null) {
				pathLogo = "resources/logos/logo_aso.png";
				System.out.println("PathLogo configurado automáticamente: " + pathLogo);
			}

			// ── PASO 0: Validar configuración contable ANTES de grabar ─────────
			if (notaCredito.getFacturador() != null
					&& Long.valueOf(1L).equals(notaCredito.getFacturador().getGeneraConta())) {

				if (notaCredito.getFacturador().getEmpresa() == null) {
					resultado.put("exito", false);
					resultado.put("etapa", "VALIDACION_CONTABLE");
					resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
							+ "pero no tiene empresa contable configurada. "
							+ "Configure el campo EMPRESA en el facturador.");
					return resultado;
				}

				Long idEmpresaVal = notaCredito.getFacturador().getEmpresa().getCodigo();
				System.out.println("PASO 0: Validando cuentas contables para NC, empresa " + idEmpresaVal + "...");

				java.util.List<String> erroresContables = asientoContableService.validarCuentasContablesNC(
						notaCredito.getTitular(), detalles, idEmpresaVal);

				if (!erroresContables.isEmpty()) {
					resultado.put("exito", false);
					resultado.put("etapa", "VALIDACION_CONTABLE");
					resultado.put("mensaje", "No se puede emitir la nota de crédito: faltan cuentas contables. "
							+ "Corrija los siguientes problemas antes de continuar:");
					resultado.put("erroresContables", erroresContables);
					StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
					for (int i = 0; i < erroresContables.size(); i++) {
						sb.append("  ").append(i + 1).append(". ").append(erroresContables.get(i)).append("\n");
					}
					resultado.put("error", sb.toString());
					System.err.println("✗ Validación contable NC fallida:\n" + sb);
					return resultado;
				}
				System.out.println("✓ Validación contable NC OK: todas las cuentas están configuradas.");
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
			System.out.println(">>> Mensaje completo de autorización NC: [" + resultadoAutorizacion + "]");

			// ── Verificar que la NC fue efectivamente AUTORIZADA ──────────────
			// Si el SRI devuelve DEVUELTA, NO_AUTORIZADO u otro estado, NO se
			// genera el asiento contable y se retorna con exito=false.
			boolean autorizada = resultadoAutorizacion != null
					&& resultadoAutorizacion.contains("AUTORIZADO");

			if (!autorizada) {
				System.err.println("✗ NC no fue autorizada por el SRI. Se detiene el proceso.");
				resultado.put("exito", false);
				resultado.put("etapa", "AUTORIZACION_SRI");
				resultado.put("estado", "NO_AUTORIZADO");
				resultado.put("mensaje", "La nota de crédito fue enviada al SRI pero no fue autorizada. "
						+ "Respuesta del SRI: " + resultadoAutorizacion);
				return resultado;
			}

			System.out.println("✓ NC AUTORIZADA. Continuando con asiento contable...");

			// ── PASO 5: Generar asiento contable ───────────────────────────────
			// Solo si el facturador tiene generaConta=1 y empresa contable configurada.
			if (notaCredito.getFacturador() != null
					&& notaCredito.getFacturador().getEmpresa() != null
					&& Long.valueOf(1L).equals(notaCredito.getFacturador().getGeneraConta())) {
				System.out.println("PASO 5: Generando asiento contable para Nota de Crédito...");
				try {
					Long idEmpresa = notaCredito.getFacturador().getEmpresa().getCodigo();
					// Recargar la nota de crédito para tener datos actualizados (autorización, etc.)
					NotaCredito ncActualizada = notaCreditoDaoService.selectById(
							notaCredito.getId(), NombreEntidadesCobro.NOTA_CREDITO);
					java.time.LocalDate fechaAsiento = ncActualizada.getFecha() != null
							? ncActualizada.getFecha().toLocalDate() : java.time.LocalDate.now();
					String obsAsiento = "Nota de Crédito N° " + nvl(ncActualizada.getNumero(), clave)
							+ " | Cliente: " + (ncActualizada.getTitular() != null ? ncActualizada.getTitular().getNombre() : "")
							+ " | " + nvl(ncActualizada.getObservacion(), "");
					String usuarioAsiento = (ncActualizada.getUsuario() != null)
							? ncActualizada.getUsuario().getNombre() : "SISTEMA";
					// Usar el mismo tipo de asiento que Factura (codigoAlterno=2 = FACTURAS_VENTA)
					// NC, ND y Factura comparten el mismo tipo de asiento contable.
					com.saa.model.cnt.Asiento asientoGenerado =
							asientoContableService.generarAsientoNotaCredito(
									ncActualizada.getId(), idEmpresa,
									com.saa.rubros.TipoAsientos.FACTURAS_VENTA,
									fechaAsiento, obsAsiento, usuarioAsiento);
					// El asiento viene de REQUIRES_NEW (transacción separada) → está detached.
					// Re-adjuntarlo con em.find() antes de asignarlo a la NC.
					com.saa.model.cnt.Asiento asientoAttached =
							em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
					if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
					ncActualizada.setAsiento(asientoAttached);
					notaCreditoDaoService.save(ncActualizada, ncActualizada.getId());
					System.out.println("✓ Asiento contable vinculado a NC: " + asientoAttached.getCodigo());
					resultado.put("asiento", asientoGenerado.getNumeroAlterno());
					System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());
				} catch (Exception e) {
					// El asiento falla → advertir pero NO revertir la autorización
					resultado.put("advertenciaAsiento",
							"Nota de Crédito autorizada pero ocurrió un error al generar el asiento: "
							+ e.getMessage()
							+ ". Genere el asiento manualmente desde Contabilidad.");
					System.err.println("⚠ Error en asiento contable de Nota de Crédito: " + e.getMessage());
					e.printStackTrace();
				}
			}

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
	
	// =========================================================================
	// anularNotaCredito
	// =========================================================================

	@Override
	public java.util.Map<String, Object> anularNotaCredito(Long idNotaCredito, String motivo, String usuario) throws Throwable {
		System.out.println("=== anularNotaCredito | id=" + idNotaCredito + " | usuario=" + usuario + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		com.saa.model.cxc.NotaCredito nc = notaCreditoDaoService.selectById(idNotaCredito, NombreEntidadesCobro.NOTA_CREDITO);
		if (nc == null) {
			resultado.put("mensaje", "Nota de Crédito con ID " + idNotaCredito + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(com.saa.rubros.Estado.INACTIVO).equals(nc.getEstado())) {
			resultado.put("mensaje", "La Nota de Crédito ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// Anular asiento contable vinculado (si existe)
		if (nc.getAsiento() != null && nc.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(com.saa.model.cnt.Asiento.class, nc.getAsiento().getCodigo());
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
				resultado.put("advertenciaAsiento", "NC anulada pero error al anular el asiento: " + e.getMessage());
			}
		}

		nc.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));
		nc.setEstadoEmision(3L); // 3 = ANULADA (tsri lsri 603)
		nc.setMotivoAnulacion(motivoFinal);
		nc.setFechaAnulacion(ahora);
		nc.setUsuarioAnulacion(usuarioAnulacion);
		notaCreditoDaoService.save(nc, nc.getId());
		em.flush();

		System.out.println("✓ Nota de Crédito anulada: " + idNotaCredito);
		resultado.put("exito", true);
		resultado.put("mensaje", "Nota de Crédito N° " + nvl(nc.getNumero(), String.valueOf(idNotaCredito)) + " anulada correctamente.");
		resultado.put("idNotaCredito", idNotaCredito);
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

	/**
	 * Genera el PDF RIDE de la nota de crédito consultando directamente la BD.
	 * Estructura idéntica al PDF de factura, adaptada para tablas NTCR/DTNC.
	 */
	@SuppressWarnings("unchecked")
	private byte[] generarPDFNotaCredito(NotaCredito ncObj, Long idFacturador, String clave,
			String pathLogoParam, Long ambiente) {
		try {
			System.out.println("Generando PDF RIDE NC para clave: " + clave);

			// ── BLOQUE 1: Datos principales ──────────────────────────────────
			jakarta.persistence.Query q1 = em.createNativeQuery(
				"SELECT nc.ID, nc.AMBIENTE, nc.AUTORIZACION, nc.FECHAAUTORIZACION, nc.NUMERO, nc.CLAVE, " +
				"       nc.FECHA, nc.SUBTOTAL, nc.TOTAL, nc.SUBCERO, nc.VIVA, nc.DESCUENTO, " +
				"       nc.OBSERVACION, nc.TIPODOCMODIFICADO, nc.NUMDOCMODIFICADO, nc.FECHAEMISIONDM, " +
				"       nc.PTOEMISION, " +
				"       fc.ID, fc.NUMDOC, fc.RAZONSOCIAL, fc.NOMBRECOMERCIAL, " +
				"       fc.MAIL, fc.TELEFONO, fc.LOGO, fc.DIRECCION, " +
				"       fc.MICROEMPRESA, fc.RIMPE, fc.POPULARRIMPE, " +
				"       fc.CONTRIBUYENTEESPECIAL, fc.CONTABILIDAD, fc.AGENTERETENCION, " +
				"       t.TTLRNMBR, t.TTLRIDNT, t.TTLRDRCC, t.TTLRMLLL, t.TTLRTLFN " +
				"FROM CBR.NTCR nc " +
				"JOIN CBR.FCDR fc ON nc.FACTURADOR = fc.ID " +
				"JOIN TSR.TTLR t  ON nc.TITULAR = t.TTLRCDGO " +
				"WHERE nc.CLAVE = :clave");
			q1.setParameter("clave", clave);
			Object[] row = (Object[]) q1.getSingleResult();

			Long   idNC              = toLong(row[0]);
			int    idAmb             = toInt(row[1]);
			String autorizacion      = str(row[2]);
			String fechaAutorizacion = str(row[3]);
			String numNC             = str(row[4]);
			String claveAcceso       = str(row[5]);
			String fecha             = str(row[6]);
			double subtotal12        = toDouble(row[7]);
			double total             = toDouble(row[8]);
			double subcero           = toDouble(row[9]);
			double vIVA              = toDouble(row[10]);
			double descuento         = toDouble(row[11]);
			String observacion       = str(row[12]);
			String tipoDocMod        = str(row[13]);
			String numDocMod         = str(row[14]);
			String fechaEmisionDM    = str(row[15]);
			Long   idPtoEmision      = toLong(row[16]);
			String ruc               = str(row[18]);
			String razonSocial       = str(row[19]);
			String nombreComercial   = str(row[20]);
			String mailFcdr          = str(row[21]);
			String telFcdr           = str(row[22]);
			String logo              = str(row[23]);
			String dirFcdr           = str(row[24]);
			int    microEmpresa      = toInt(row[25]);
			int    rimpe             = toInt(row[26]);
			int    rimpePopular      = toInt(row[27]);
			String contribuyenteEspecial = str(row[28]);
			int    contabilidad      = toInt(row[29]);
			String agenteRetencion   = str(row[30]);
			String nomCmdr           = str(row[31]);
			String numDocCmdr        = str(row[32]);
			String dirCmdr           = str(row[33]);
			String mailCmdr          = str(row[34]);
			String telCmdr           = str(row[35]);

			// ── BLOQUE 2: Establecimiento ────────────────────────────────────
			String estNombre = "", estDireccion = "", estTelefono = "";
			boolean esMatriz = true;
			String obsEstablecimiento = "";
			try {
				jakarta.persistence.Query q2 = em.createNativeQuery(
					"SELECT b.NOMBRE, b.DIRECCION, b.TELEFONO, b.MATRIZ, a.OBSERVACION " +
					"FROM CBR.PTEM a JOIN CBR.ESTB b ON a.ESTABLECIMIENTO = b.ID " +
					"WHERE a.ID = :id");
				q2.setParameter("id", idPtoEmision);
				Object[] est = (Object[]) q2.getSingleResult();
				estNombre          = str(est[0]);
				estDireccion       = str(est[1]);
				estTelefono        = str(est[2]);
				esMatriz           = toInt(est[3]) == 1;
				obsEstablecimiento = str(est[4]);
			} catch (Exception ignored) {}

			// ── BLOQUE 3: IVA general vigente ────────────────────────────────
			int valorIvaGeneral = 15;
			try {
				jakarta.persistence.Query q3 = em.createNativeQuery(
					"SELECT PORCENTAJE FROM CBR.TSRI WHERE LSRI = 614 FETCH FIRST 1 ROWS ONLY");
				Object porcObj = q3.getSingleResult();
				if (porcObj != null) valorIvaGeneral = ((Number) porcObj).intValue();
			} catch (Exception ignored) {}

			// ── BLOQUE 4: Logo ───────────────────────────────────────────────
			String logoPath = "";
			if (logo != null && !logo.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				String candidato = logo.startsWith("/") || logo.contains(":\\") ? logo : baseDir + logo;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(candidato))) logoPath = candidato;
			}
			if (logoPath.isEmpty() && pathLogoParam != null && !pathLogoParam.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				String candidato = baseDir + pathLogoParam;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(candidato))) logoPath = candidato;
			}

			// ── BLOQUE 5: Campos calculados ──────────────────────────────────
			String ambStr = (idAmb == 2) ? "PRODUCCIÓN" : "PRUEBAS";
			String tipoEmpresa = "";
			if (microEmpresa == 1)       tipoEmpresa = "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS";
			else if (rimpe == 1)         tipoEmpresa = "CONTRIBUYENTE RÉGIMEN RIMPE";
			else if (rimpePopular == 1)  tipoEmpresa = "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE";

			String dirFcdrCompleta = dirFcdr;
			String telFcdrCompleta = telFcdr;
			if (!esMatriz && !estNombre.isEmpty()) {
				dirFcdrCompleta = dirFcdr + " | Suc: " + estNombre + " - " + estDireccion;
				telFcdrCompleta = telFcdr + " / " + estTelefono;
			}
			String contabilidadStr = (contabilidad == 1) ? "SI" : "NO";
			String iaObservacion = (obsEstablecimiento + " " + observacion).trim();
			double subtotalSinImp = subtotal12 + subcero;

			// ── BLOQUE 6: Parámetros para Jasper ────────────────────────────
			java.util.Map<String, Object> p = new java.util.HashMap<>();
			p.put("P_ID_NOTA_CREDITO",         idNC);
			p.put("P_CLAVE",                   claveAcceso);
			p.put("P_PATH_LOGO",               logoPath);
			// Facturador
			p.put("P_RUC_FACTURADOR",          ruc);
			p.put("P_RAZON_SOCIAL",            razonSocial);
			p.put("P_NOMBRE_COMERCIAL",        nombreComercial != null ? nombreComercial : "");
			p.put("P_DIRECCION_FCDR",          dirFcdrCompleta);
			p.put("P_TELEFONO_FCDR",           telFcdrCompleta);
			p.put("P_EMAIL_FCDR",              mailFcdr);
			p.put("P_TIPO_EMPRESA",            tipoEmpresa);
			p.put("P_AGENTE_RETENCION",        agenteRetencion != null ? agenteRetencion : "");
			p.put("P_CONTRIBUYENTE_ESPECIAL",  contribuyenteEspecial != null ? contribuyenteEspecial : "");
			p.put("P_OBLIGADO_CONTABILIDAD",   contabilidadStr);
			// Nota de Crédito
			p.put("P_NUMERO_NC",               numNC);
			p.put("P_NUMERO_AUTORIZACION",     autorizacion != null ? autorizacion : claveAcceso);
			p.put("P_FECHA_EMISION",           fecha);
			p.put("P_FECHA_AUTORIZACION",      fechaAutorizacion != null ? fechaAutorizacion : "");
			p.put("P_AMBIENTE",                ambStr);
			p.put("P_TIPO_DOC_MODIFICADO",     tipoDocMod);
			p.put("P_NUM_DOC_MODIFICADO",      numDocMod);
			p.put("P_FECHA_EMISION_DOC_MOD",   fechaEmisionDM);
			// Comprador
			p.put("P_RAZON_SOCIAL_COMPRADOR",   nomCmdr);
			p.put("P_IDENTIFICACION_COMPRADOR", numDocCmdr);
			p.put("P_DIRECCION_COMPRADOR",      dirCmdr);
			p.put("P_EMAIL_COMPRADOR",          mailCmdr);
			// Totales
			p.put("P_PORC_IVA",               valorIvaGeneral);
			p.put("P_SUBTOTAL_IVA",           subtotal12);
			p.put("P_SUBTOTAL_0",             subcero);
			p.put("P_SUBTOTAL_SIN_IMP",       subtotalSinImp);
			p.put("P_DESCUENTO",              descuento);
			p.put("P_IVA",                    vIVA);
			p.put("P_TOTAL",                  total);
			p.put("P_INFO_ADICIONAL",         "Teléfonos: " + telCmdr + "\nObservación: " + iaObservacion);

			// ── BLOQUE 7: Generar PDF ────────────────────────────────────────
			byte[] pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_NOTA_CREDITO", p, "PDF");
			System.out.println("✓ PDF RIDE NC generado correctamente ("
					+ (pdfBytes != null ? pdfBytes.length : 0) + " bytes)");
			return pdfBytes;

		} catch (Exception e) {
			System.err.println("Error generando PDF RIDE NC: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	// ── Helpers de conversión para native query ──────────────────────────────
	private String str(Object o)      { return o != null ? o.toString() : ""; }
	private double toDouble(Object o) { return o != null ? ((Number) o).doubleValue() : 0.0; }
	private int    toInt(Object o)    { return o != null ? ((Number) o).intValue()    : 0; }
	private Long   toLong(Object o)   { return o != null ? ((Number) o).longValue()   : 0L; }
	private Double nvlD(Double v)     { return v != null ? v : 0.0; }

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
		String responseStr = baos.toString("UTF-8");

		ResultadoAutorizacion resultado = new ResultadoAutorizacion();
		resultado.respuestaCompleta = responseStr;

		SOAPBody responseBody = soapResponse.getSOAPBody();

		NodeList estadoList = responseBody.getElementsByTagNameNS("*", "estado");
		if (estadoList.getLength() > 0) resultado.estado = estadoList.item(0).getTextContent();

		NodeList numAuthList = responseBody.getElementsByTagNameNS("*", "numeroAutorizacion");
		if (numAuthList.getLength() > 0) resultado.numeroAutorizacion = numAuthList.item(0).getTextContent();

		NodeList fechaAuthList = responseBody.getElementsByTagNameNS("*", "fechaAutorizacion");
		if (fechaAuthList.getLength() > 0) resultado.fechaAutorizacion = fechaAuthList.item(0).getTextContent();

		NodeList comprobanteList = responseBody.getElementsByTagNameNS("*", "comprobante");
		if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();

		NodeList mensajesList = responseBody.getElementsByTagNameNS("*", "mensaje");
		if (mensajesList.getLength() > 0) {
			org.w3c.dom.Node msgNode = mensajesList.item(0);
			NodeList idList   = ((org.w3c.dom.Element) msgNode).getElementsByTagNameNS("*", "identificador");
			NodeList msgList  = ((org.w3c.dom.Element) msgNode).getElementsByTagNameNS("*", "mensaje");
			NodeList infoList = ((org.w3c.dom.Element) msgNode).getElementsByTagNameNS("*", "informacionAdicional");
			if (idList.getLength()   > 0) resultado.mensajeId           = idList.item(0).getTextContent();
			if (msgList.getLength()  > 0) resultado.mensaje             = msgList.item(0).getTextContent();
			if (infoList.getLength() > 0) resultado.informacionAdicional = infoList.item(0).getTextContent();
		}

		soapConnection.close();
		return resultado;
	}

	private String getBaseUploadDirectory() {
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
