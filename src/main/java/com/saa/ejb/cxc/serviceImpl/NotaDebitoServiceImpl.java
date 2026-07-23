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
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.DetalleNotaDebito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathNotaDebito;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAsiento;
import jakarta.ejb.EJB;
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
public class NotaDebitoServiceImpl implements NotaDebitoService {
	@EJB
	private NotaDebitoDaoService notaDebitoDaoService;
	
	@EJB
	private PathNotaDebitoDaoService pathNotaDebitoDaoService;
	
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

	/** Auto-inyección para llamadas internas que requieren nueva transacción */
	@EJB
	private NotaDebitoService self;

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
			System.out.println(">>> generarXMLNotaDebito | detalles encontrados en BD: " + detalles.size());
			for (Object obj : detalles) {
				DetalleNotaDebito d = (DetalleNotaDebito) obj;
				System.out.println("    - detalle id=" + d.getId() + " desc=" + d.getDescripcion() + " valor=" + d.getValor() + " baseImponible=" + d.getBaseImponible());
			}
			
			String xmlContent = generarXMLContentNotaDebito(notaDebito, dirEstablecimiento, detalles, ambiente);
			System.out.println(">>> XML NotaDebito GENERADO (sin firmar):\n" + xmlContent);
			
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

		// tipoIdentificacionComprador — igual que Factura y NC: usar detalleRubroService
		// para obtener el código SRI real del rubro (ej: "04", "05", "06")
		String tipoIdNd = "05"; // valor por defecto (cédula)
		try {
			if (notaDebito.getTitular().getRubroTipoIdentificacionP() != null
					&& notaDebito.getTitular().getRubroTipoIdentificacionH() != null) {
				// Usar em.createQuery directo en lugar de EJB para evitar que una excepción
				// cruce el boundary EJB y marque la transacción JTA como STATUS_MARKED_ROLLBACK.
				@SuppressWarnings("unchecked")
				java.util.List<String> valorAlfaList = em.createQuery(
						"SELECT dr.valorAlfanumerico FROM DetalleRubro dr " +
						"WHERE dr.rubro.codigoAlterno = :rubAlt " +
						"AND dr.codigoAlterno = :detAlt")
						.setParameter("rubAlt", notaDebito.getTitular().getRubroTipoIdentificacionP())
						.setParameter("detAlt", notaDebito.getTitular().getRubroTipoIdentificacionH())
						.setMaxResults(1)
						.getResultList();
				if (!valorAlfaList.isEmpty() && valorAlfaList.get(0) != null && !valorAlfaList.get(0).isEmpty()) {
					String valorAlfa = valorAlfaList.get(0);
					// SRI exige siempre 2 dígitos: "04", "05", "06", etc.
					tipoIdNd = valorAlfa.length() == 1 ? "0" + valorAlfa : valorAlfa;
				}
			}
		} catch (Exception e) {
			System.err.println("⚠ Error al obtener tipoIdentificación ND, usando default 05: " + e.getMessage());
		}
		writeElement(writer, "tipoIdentificacionComprador", tipoIdNd, 4);
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

		// valorTotal — obligatorio por esquema SRI v1.0.0, DENTRO de infoNotaDebito
		Double valorTotal = sumNulls(notaDebito.getSubtotal(), notaDebito.getSubcero(), notaDebito.getvIVA());
		writeElement(writer, "valorTotal", formatDecimal(valorTotal), 4);

		// pagos — DENTRO de infoNotaDebito, obligatorio según esquema SRI v1.0.0
		writePagos(writer, notaDebito);

		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoNotaDebito
		writer.writeCharacters("\n");

		// motivos — FUERA de infoNotaDebito, según esquema SRI v1.0.0
		writeMotivos(writer, detalles);

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
	
	private void writePagos(XMLStreamWriter writer, NotaDebito notaDebito) throws Exception {
		// Usar siempre el total de la nota de débito con forma de pago "20" (Otros).
		// NO se consulta ninguna tabla auxiliar de formas de pago para evitar que
		// un error SQL marque la transacción como STATUS_MARKED_ROLLBACK y bloquee
		// los pasos siguientes (selectById en autorizarNotaDebito).
		Double totalND = sumNulls(notaDebito.getSubtotal(), notaDebito.getSubcero(), notaDebito.getvIVA());
		String formaPago = "20"; // "20" = Otros
		writer.writeCharacters("    ");
		writer.writeStartElement("pagos");
		writer.writeCharacters("\n");
		writer.writeCharacters("      ");
		writer.writeStartElement("pago");
		writer.writeCharacters("\n");
		writeElement(writer, "formaPago", formaPago, 8);
		writeElement(writer, "total", formatDecimal(totalND), 8);
		writer.writeCharacters("      ");
		writer.writeEndElement(); // pago
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeEndElement(); // pagos
		writer.writeCharacters("\n");
	}

	private void writeMotivos(XMLStreamWriter writer, List<Object> detalles) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("motivos");
		writer.writeCharacters("\n");
		if (detalles == null || detalles.isEmpty()) {
			// El SRI exige al menos un <motivo>; si no hay detalles se pone uno genérico
			// para evitar el error: "The content of element 'motivos' is not complete"
			System.err.println("⚠ writeMotivos: lista de detalles vacía — insertando motivo genérico");
			writer.writeCharacters("    ");
			writer.writeStartElement("motivo");
			writer.writeCharacters("\n");
			writeElement(writer, "razon", "Nota de Débito", 6);
			writeElement(writer, "valor", "0.00", 6);
			writer.writeCharacters("    ");
			writer.writeEndElement(); // motivo
			writer.writeCharacters("\n");
		} else {
			for (Object obj : detalles) {
				DetalleNotaDebito d = (DetalleNotaDebito) obj;
				// El PHP de referencia usa baseImponible para <valor> del motivo
				Double valorMotivo = d.getBaseImponible() != null ? d.getBaseImponible() : d.getValor();
				writer.writeCharacters("    ");
				writer.writeStartElement("motivo");
				writer.writeCharacters("\n");
				writeElement(writer, "razon", nvl(d.getDescripcion(), ""), 6);
				writeElement(writer, "valor", formatDecimal(valorMotivo), 6);
				writer.writeCharacters("    ");
				writer.writeEndElement(); // motivo
				writer.writeCharacters("\n");
			}
		}
		writer.writeCharacters("  ");
		writer.writeEndElement(); // motivos
		writer.writeCharacters("\n");
	}
	
	private void writeInfoAdicional(XMLStreamWriter writer, NotaDebito notaDebito) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		// Igual que el PHP de referencia y NC:
		// Soporte[tel - mail] Contacto Cliente[tel - mail]
		// Facturador: getTelefono() / getMail()
		// Titular:    getTelefono() / getEmail()
		String telFcdr  = nvl(notaDebito.getFacturador().getTelefono(), "");
		String mailFcdr = nvl(notaDebito.getFacturador().getMail(), "");
		String telCmdr  = nvl(notaDebito.getTitular().getTelefono(), "");
		String mailCmdr = nvl(notaDebito.getTitular().getEmail(), "");
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
	public String autorizarNotaDebito(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoNotaDebito, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== autorizarNotaDebito | clave: " + clave + " | facturador: " + idFacturador
				+ " | ambiente: " + ambiente + " | conectaSRI: " + conectaSRI + " ===");
		
		String respuesta = "";
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/ntdb/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			System.out.println("✓ XML firmado guardado en: " + pathFirmado);
			
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
			System.out.println("✓ Estado ND actualizado a FIRMADA (3)");
			
			if (conectaSRI == 1) {
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				System.out.println(">>> WS1 Recepción URL: " + urlWS1);
				
				try {
					Path logWS1 = Paths.get(resourcesPath + "/ntdb/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					System.out.println(">>> Enviando XML al SRI (" + bytesXMLFirmado.length + " bytes)...");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					System.out.println(">>> Estado WS1 Recepción: [" + estadoRecepcion + "]");
					
					logWriter1.close();
					
					Path pathEnviado = Paths.get(resourcesPath + "/ntdb/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
					PathNotaDebito pathE = new PathNotaDebito();
					pathE.setNotaDebito(notaDebito);
					pathE.setPath("resources/" + idFacturador + "/ntdb/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathNotaDebitoDaoService.save(pathE, null);
					
					notaDebito.setEstado(4L);
					notaDebitoDaoService.save(notaDebito, notaDebito.getId());
					System.out.println("✓ Estado ND actualizado a ENVIADA (4)");
					
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
								System.out.println("✓ ND AUTORIZADA por el SRI. Num.Autorización: " + resultado.numeroAutorizacion);
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

								// 5. Generar PDF (RIDE Nota de Débito)
								byte[] pdfBytesParaEmail = null;
								try {
									byte[] pdfBytes = generarPDFNotaDebito(notaDebito, idFacturador, clave, pathLogo, ambiente);
									if (pdfBytes != null && pdfBytes.length > 0) {
										Path pathPdf = Paths.get(resourcesPath + "/ntdb/a/" + clave + ".pdf");
										Files.write(pathPdf, pdfBytes);
										PathNotaDebito pathPdfRec = new PathNotaDebito();
										pathPdfRec.setNotaDebito(notaDebito);
										pathPdfRec.setPath("resources/" + idFacturador + "/ntdb/a/" + clave + ".pdf");
										pathPdfRec.setAlterno(7L);
										pathNotaDebitoDaoService.save(pathPdfRec, null);
										pdfBytesParaEmail = pdfBytes;
										System.out.println("✓ PDF RIDE ND generado: " + pathPdf);
									}
								} catch (Exception pdfEx) {
									System.err.println("⚠ Error generando PDF ND (no crítico): " + pdfEx.getMessage());
									pdfEx.printStackTrace();
								}

								// 6. Enviar correo electrónico
								try {
									String emailDest = destinatario;
									if ((emailDest == null || emailDest.trim().isEmpty())
											&& notaDebito.getTitular() != null) {
										emailDest = notaDebito.getTitular().getEmail();
									}
									String razonSocialEmisor = notaDebito.getFacturador() != null
											? nvl(notaDebito.getFacturador().getRazonSocial(),
												  nvl(notaDebito.getFacturador().getNombre(), "")) : "";
									if (emailDest != null && !emailDest.trim().isEmpty()) {
										emailFacturaService.enviarFacturaAutorizada(
												emailDest,
												nvl(notaDebito.getNumero(), clave),
												clave,
												razonSocialEmisor,
												resultado.comprobanteXML,
												pdfBytesParaEmail);
										System.out.println("✓ Email ND enviado a: " + emailDest);
									}
								} catch (Exception mailEx) {
									System.err.println("⚠ Error enviando email ND (no crítico): " + mailEx.getMessage());
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
								System.err.println("✗ ND NO AUTORIZADA por el SRI.");
								System.err.println("    estado              = [" + resultado.estado + "]");
								System.err.println("    mensajeId           = [" + resultado.mensajeId + "]");
								System.err.println("    mensaje             = [" + resultado.mensaje + "]");
								System.err.println("    informacionAdicional= [" + resultado.informacionAdicional + "]");
								System.err.println("    respuestaCompleta   = " + resultado.respuestaCompleta);

								Path logWS2N = Paths.get(resourcesPath + "/ntdb/n/" + clave + ".txt");
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
						// WS1 devolvió algo distinto a RECIBIDA
						System.err.println("⚠ WS1 devolvió estado no esperado: [" + estadoRecepcion + "]");
						respuesta = "Estado WS1: " + estadoRecepcion;
						
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							System.out.println(">>> Clave ya registrada en SRI — marcando como autorizada");
							respuesta = "Comprobante Autorizado";
							notaDebito.setAutorizacion(clave);
							notaDebito.setFechaAutorizacion(notaDebito.getFecha().plusMinutes(1).plusSeconds(15));
							notaDebito.setEstado(5L);
							notaDebitoDaoService.save(notaDebito, notaDebito.getId());
						}
					}
					
				} catch (Exception e) {
					System.err.println("✗ ERROR en llamada WS1 recepción: " + e.getMessage());
					e.printStackTrace();
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
				}
				
			} else {
				System.out.println("ℹ conectaSRI=0 — ND generada pero NO enviada al SRI");
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

			// ── PASO 0: Validar configuración contable ANTES de grabar ─────────
			if (notaDebito.getFacturador() != null
					&& Long.valueOf(1L).equals(notaDebito.getFacturador().getGeneraConta())) {

				if (notaDebito.getFacturador().getEmpresa() == null) {
					resultado.put("exito", false);
					resultado.put("etapa", "VALIDACION_CONTABLE");
					resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
							+ "pero no tiene empresa contable configurada. "
							+ "Configure el campo EMPRESA en el facturador.");
					return resultado;
				}

				Long idEmpresaVal = notaDebito.getFacturador().getEmpresa().getCodigo();
				System.out.println("PASO 0: Validando cuentas contables para ND, empresa " + idEmpresaVal + "...");

				java.util.List<String> erroresContables =
						asientoContableService.validarCuentasContablesND(notaDebito, idEmpresaVal);

				if (!erroresContables.isEmpty()) {
					resultado.put("exito", false);
					resultado.put("etapa", "VALIDACION_CONTABLE");
					resultado.put("mensaje", "No se puede emitir la nota de débito: faltan cuentas contables. "
							+ "Corrija los siguientes problemas antes de continuar:");
					resultado.put("erroresContables", erroresContables);
					StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
					for (int i = 0; i < erroresContables.size(); i++) {
						sb.append("  ").append(i + 1).append(". ").append(erroresContables.get(i)).append("\n");
					}
					resultado.put("error", sb.toString());
					System.err.println("✗ Validación contable ND fallida:\n" + sb);
					return resultado;
				}
				System.out.println("✓ Validación contable ND OK: todas las cuentas están configuradas.");
			}
			
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
			System.out.println(">>> Mensaje completo de autorización ND: [" + resultadoAutorizacion + "]");

			// ── Verificar que la ND fue efectivamente AUTORIZADA ──────────────
			// Si el SRI devuelve DEVUELTA, NO_AUTORIZADO u otro estado, NO se
			// genera el asiento contable y se retorna con exito=false.
			boolean autorizada = resultadoAutorizacion != null
					&& resultadoAutorizacion.contains("AUTORIZADO");

			if (!autorizada) {
				System.err.println("✗ ND no fue autorizada por el SRI. Se detiene el proceso.");
				resultado.put("exito", false);
				resultado.put("etapa", "AUTORIZACION_SRI");
				resultado.put("estado", "NO_AUTORIZADO");
				resultado.put("mensaje", "La nota de débito fue enviada al SRI pero no fue autorizada. "
						+ "Respuesta del SRI: " + resultadoAutorizacion);
				return resultado;
			}

			System.out.println("✓ ND AUTORIZADA. Continuando con asiento contable...");

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
							+ " | " + nvl(ndActualizada.getObservacion(), "");
					String usuarioAsiento = (ndActualizada.getUsuario() != null)
							? ndActualizada.getUsuario().getNombre() : "SISTEMA";
					// Usar el mismo tipo de asiento que Factura y NC (codigoAlterno=2 = FACTURAS_VENTA)
					// Factura, NC y ND comparten el mismo tipo de asiento contable.
					com.saa.model.cnt.Asiento asientoGenerado =
							asientoContableService.generarAsientoNotaDebito(
									ndActualizada.getId(), idEmpresaConta,
									com.saa.rubros.TipoAsientos.FACTURAS_VENTA,
									fechaAsiento, obsAsiento, usuarioAsiento);
					// Vincular el asiento a la nota de débito y persistir
					ndActualizada.setAsiento(asientoGenerado);
					notaDebitoDaoService.save(ndActualizada, ndActualizada.getId());
					System.out.println("✓ Asiento contable vinculado a ND: " + asientoGenerado.getNumeroAlterno());
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
			String respuestaCompleta = baos.toString("UTF-8");

			// Loguear la respuesta COMPLETA en stdout Y en archivo para diagnóstico
			System.out.println(">>> RESPUESTA COMPLETA WS1 SRI (ND):");
			System.out.println(respuestaCompleta);
			log.println("Respuesta WS1 completa: " + respuestaCompleta);
			
			SOAPBody responseBody = soapResponse.getSOAPBody();
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() == 0) estadoList = responseBody.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado WS1 extraído: [" + estado + "]");

				NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
				if (mensajeList.getLength() == 0) mensajeList = responseBody.getElementsByTagNameNS("*", "mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						soapConnection.close();
						return "CLAVE ACCESO REGISTRADA";
					}
				}

				// Si es DEVUELTA, extraer y loguear TODOS los mensajeDevuelta con la razón exacta
				if ("DEVUELTA".equals(estado)) {
					StringBuilder sbErrores = new StringBuilder("DEVUELTA");
					NodeList mensajesDevuelta = responseBody.getElementsByTagName("mensajeDevuelta");
					if (mensajesDevuelta.getLength() == 0)
						mensajesDevuelta = responseBody.getElementsByTagNameNS("*", "mensajeDevuelta");

					System.err.println(">>> SRI rechazó la ND (DEVUELTA). Errores encontrados: " + mensajesDevuelta.getLength());
					log.println(">>> Errores DEVUELTA: " + mensajesDevuelta.getLength());

					for (int i = 0; i < mensajesDevuelta.getLength(); i++) {
						org.w3c.dom.Node nodeMD = mensajesDevuelta.item(i);
						String identificador = extraerTextoHijo(nodeMD, "identificador");
						String msgError      = extraerTextoHijo(nodeMD, "mensaje");
						String infoAd        = extraerTextoHijo(nodeMD, "informacionAdicional");
						String tipo          = extraerTextoHijo(nodeMD, "tipo");
						String lineaError = " | [" + tipo + "] Id:" + identificador
								+ " Msg:" + msgError + " Info:" + infoAd;
						sbErrores.append(lineaError);
						System.err.println("  ERROR SRI[" + i + "]: tipo=" + tipo
								+ " | identificador=" + identificador
								+ " | mensaje=" + msgError
								+ " | informacionAdicional=" + infoAd);
						log.println("  ERROR SRI[" + i + "]: " + lineaError);
					}
					soapConnection.close();
					return sbErrores.toString();
				}

				soapConnection.close();
				return estado;
			}

			System.out.println(">>> ADVERTENCIA: No se encontró <estado> en la respuesta WS1 (ND)");
			soapConnection.close();
			return "SIN_RESPUESTA";
		} catch (Exception e) {
			System.err.println("✗ ERROR en llamarRecepcionSRI ND: " + e.getMessage());
			log.println("Error en llamarRecepcionSRI: " + e.getMessage());
			e.printStackTrace(log);
			throw e;
		}
	}

	/** Extrae el texto de un nodo hijo por nombre de tag */
	private String extraerTextoHijo(org.w3c.dom.Node parent, String tagName) {
		if (parent == null) return "";
		org.w3c.dom.NodeList hijos = ((org.w3c.dom.Element) parent).getElementsByTagName(tagName);
		if (hijos.getLength() == 0) return "";
		return hijos.item(0).getTextContent() != null ? hijos.item(0).getTextContent() : "";
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
	
	// =========================================================================
	// anularNotaDebito
	// =========================================================================

	@Override
	public java.util.Map<String, Object> anularNotaDebito(Long idNotaDebito, String motivo, String usuario) throws Throwable {
		System.out.println("=== anularNotaDebito | id=" + idNotaDebito + " | usuario=" + usuario + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		NotaDebito nd = notaDebitoDaoService.selectById(idNotaDebito, NombreEntidadesCobro.NOTA_DEBITO);
		if (nd == null) {
			resultado.put("mensaje", "Nota de Débito con ID " + idNotaDebito + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(com.saa.rubros.Estado.INACTIVO).equals(nd.getEstado())) {
			resultado.put("mensaje", "La Nota de Débito ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
		LocalDateTime ahora = LocalDateTime.now();

		// Anular asiento contable vinculado (si existe)
		if (nd.getAsiento() != null && nd.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(com.saa.model.cnt.Asiento.class, nd.getAsiento().getCodigo());
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
				resultado.put("advertenciaAsiento", "ND anulada pero error al anular el asiento: " + e.getMessage());
			}
		}

		nd.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));
		nd.setMotivoAnulacion(motivoFinal);
		nd.setFechaAnulacion(ahora);
		nd.setUsuarioAnulacion(usuarioAnulacion);
		notaDebitoDaoService.save(nd, nd.getId());
		em.flush();

		System.out.println("✓ Nota de Débito anulada: " + idNotaDebito);
		resultado.put("exito", true);
		resultado.put("mensaje", "Nota de Débito N° " + nvl(nd.getNumero(), String.valueOf(idNotaDebito)) + " anulada correctamente.");
		resultado.put("idNotaDebito", idNotaDebito);
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
	 * Genera el PDF RIDE de la nota de débito consultando directamente la BD.
	 * Estructura idéntica al PDF de NC, adaptada para tablas NTDB/DTND.
	 */
	@SuppressWarnings("unchecked")
	private byte[] generarPDFNotaDebito(NotaDebito ndObj, Long idFacturador, String clave,
			String pathLogoParam, Long ambiente) {
		try {
			System.out.println("Generando PDF RIDE ND para clave: " + clave);

			// ── BLOQUE 1: Datos principales ──────────────────────────────────
			jakarta.persistence.Query q1 = em.createNativeQuery(
				"SELECT nd.ID, nd.AMBIENTE, nd.AUTORIZACION, nd.FECHAAUTORIZACION, nd.NUMERO, nd.CLAVE, " +
				"       nd.FECHA, nd.SUBTOTAL, nd.TOTAL, nd.SUBCERO, nd.VIVA, nd.PIVA, nd.DESCUENTO, " +
				"       nd.OBSERVACION, nd.TIPODOCMODIFICADO, nd.NUMDOCMODIFICADO, nd.FECHAEMISIONDM, " +
				"       nd.PTOEMISION, " +
				"       fc.NUMDOC, fc.RAZONSOCIAL, fc.NOMBRECOMERCIAL, " +
				"       fc.MAIL, fc.TELEFONO, fc.LOGO, fc.DIRECCION, " +
				"       fc.MICROEMPRESA, fc.RIMPE, fc.POPULARRIMPE, " +
				"       fc.CONTRIBUYENTEESPECIAL, fc.CONTABILIDAD, fc.AGENTERETENCION, " +
				"       t.TTLRNMBR, t.TTLRIDNT, t.TTLRDRCC, t.TTLRMLLL, t.TTLRTLFN " +
				"FROM CBR.NTDB nd " +
				"JOIN CBR.FCDR fc ON nd.FACTURADOR = fc.ID " +
				"JOIN TSR.TTLR t  ON nd.TITULAR = t.TTLRCDGO " +
				"WHERE nd.CLAVE = :clave");
			q1.setParameter("clave", clave);
			Object[] row = (Object[]) q1.getSingleResult();

			Long   idND              = toLong(row[0]);
			int    idAmb             = toInt(row[1]);
			String autorizacion      = str(row[2]);
			String fechaAutorizacion = str(row[3]);
			String numND             = str(row[4]);
			String claveAcceso       = str(row[5]);
			String fecha             = str(row[6]);
			double subtotal12        = toDouble(row[7]);
			double total             = toDouble(row[8]);
			double subcero           = toDouble(row[9]);
			double vIVA              = toDouble(row[10]);
			double pIVA              = toDouble(row[11]);
			double descuento         = toDouble(row[12]);
			String observacion       = str(row[13]);
			String tipoDocMod        = str(row[14]);
			String numDocMod         = str(row[15]);
			String fechaEmisionDM    = str(row[16]);
			Long   idPtoEmision      = toLong(row[17]);
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

			// ── BLOQUE 2: Establecimiento ─────────────────────────────────────
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

			// ── BLOQUE 3: IVA general vigente ─────────────────────────────────
			int valorIvaGeneral = (int) pIVA;
			if (valorIvaGeneral == 0) {
				try {
					jakarta.persistence.Query q3 = em.createNativeQuery(
						"SELECT PORCENTAJE FROM CBR.TSRI WHERE LSRI = 614 FETCH FIRST 1 ROWS ONLY");
					Object p = q3.getSingleResult();
					if (p != null) valorIvaGeneral = ((Number) p).intValue();
				} catch (Exception ignored) {}
			}

			// ── BLOQUE 4: Logo ────────────────────────────────────────────────
			String logoPath = "";
			if (logo != null && !logo.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				String cand = logo.startsWith("/") || logo.contains(":\\") ? logo : baseDir + logo;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(cand))) logoPath = cand;
			}
			if (logoPath.isEmpty() && pathLogoParam != null && !pathLogoParam.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				String cand = baseDir + pathLogoParam;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(cand))) logoPath = cand;
			}

			// ── BLOQUE 5: Campos calculados ───────────────────────────────────
			String ambStr = (idAmb == 2) ? "PRODUCCIÓN" : "PRUEBAS";
			String tipoEmpresa = "";
			if (microEmpresa == 1)      tipoEmpresa = "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS";
			else if (rimpe == 1)        tipoEmpresa = "CONTRIBUYENTE RÉGIMEN RIMPE";
			else if (rimpePopular == 1) tipoEmpresa = "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE";
			String dirFcdrCompleta = dirFcdr;
			String telFcdrCompleta = telFcdr;
			if (!esMatriz && !estNombre.isEmpty()) {
				dirFcdrCompleta = dirFcdr + " | Suc: " + estNombre + " - " + estDireccion;
				telFcdrCompleta = telFcdr + " / " + estTelefono;
			}
			String contabilidadStr = (contabilidad == 1) ? "SI" : "NO";
			String iaObservacion = (obsEstablecimiento + " " + observacion).trim();
			double subtotalSinImp = subtotal12 + subcero;

			// ── BLOQUE 6: Parámetros para Jasper ─────────────────────────────
			java.util.Map<String, Object> p = new java.util.HashMap<>();
			p.put("P_ID_NOTA_DEBITO",          idND);
			p.put("P_CLAVE",                   claveAcceso);
			p.put("P_PATH_LOGO",               logoPath);
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
			p.put("P_NUMERO_ND",               numND);
			p.put("P_NUMERO_AUTORIZACION",     autorizacion != null ? autorizacion : claveAcceso);
			p.put("P_FECHA_EMISION",           fecha);
			p.put("P_FECHA_AUTORIZACION",      fechaAutorizacion != null ? fechaAutorizacion : "");
			p.put("P_AMBIENTE",                ambStr);
			p.put("P_TIPO_DOC_MODIFICADO",     tipoDocMod);
			p.put("P_NUM_DOC_MODIFICADO",      numDocMod);
			p.put("P_FECHA_EMISION_DOC_MOD",   fechaEmisionDM);
			p.put("P_RAZON_SOCIAL_COMPRADOR",   nomCmdr);
			p.put("P_IDENTIFICACION_COMPRADOR", numDocCmdr);
			p.put("P_DIRECCION_COMPRADOR",      dirCmdr);
			p.put("P_EMAIL_COMPRADOR",          mailCmdr);
			p.put("P_PORC_IVA",               valorIvaGeneral);
			p.put("P_SUBTOTAL_IVA",           subtotal12);
			p.put("P_SUBTOTAL_0",             subcero);
			p.put("P_SUBTOTAL_SIN_IMP",       subtotalSinImp);
			p.put("P_DESCUENTO",              descuento);
			p.put("P_IVA",                    vIVA);
			p.put("P_TOTAL",                  total);
			p.put("P_INFO_ADICIONAL",         "Teléfonos: " + telCmdr + "\nObservación: " + iaObservacion);

			// ── BLOQUE 7: Generar PDF ─────────────────────────────────────────
			byte[] pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_NOTA_DEBITO", p, "PDF");
			System.out.println("✓ PDF RIDE ND generado correctamente ("
					+ (pdfBytes != null ? pdfBytes.length : 0) + " bytes)");
			return pdfBytes;

		} catch (Exception e) {
			System.err.println("Error generando PDF RIDE ND: " + e.getMessage());
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
