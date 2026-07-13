package com.saa.ejb.cxc.serviceImpl;

import java.io.ByteArrayOutputStream;
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

import jakarta.persistence.Query;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.FacturaDaoService;
import com.saa.ejb.cxc.dao.PathFacturaDaoService;
import com.saa.ejb.cxc.service.DetalleFacturaService;
import com.saa.ejb.cxc.service.FacturaService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathFactura;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPElement;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

@Stateless
public class FacturaServiceImpl implements FacturaService {

	@EJB
	private FacturaDaoService facturaDaoService;
	
	@EJB
	private PathFacturaDaoService pathFacturaDaoService;
	
	@EJB
	private DetalleFacturaService detalleFacturaService;
	
	@EJB
	private com.saa.ejb.cxc.service.FormaPagoFacturaService formaPagoFacturaService;
	
	@EJB
	private SignatureService signatureService;
	
	@PersistenceContext
	private EntityManager em;

	@Override
	public Factura selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById Factura con id: " + id);
		return facturaDaoService.selectById(id, NombreEntidadesCobro.FACTURA);
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de FacturaService");
		Factura entidad = new Factura();
		for (Long registro : id) {
			facturaDaoService.remove(entidad, registro);
		}
	}

	@Override
	public void save(List<Factura> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de FacturaService");
		for (Factura registro : lista) {
			facturaDaoService.save(registro, registro.getId());
		}
	}

	@Override
	public List<Factura> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll FacturaService");
		List<Factura> result = facturaDaoService.selectAll(NombreEntidadesCobro.FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda total Factura no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public Factura saveSingle(Factura entidad) throws Throwable {
		System.out.println("saveSingle - Factura");
		
		// Si es una nueva factura, generar campos automáticos
		if (entidad.getId() == null) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Validar que tenga los datos necesarios
			if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la factura");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la factura");
			}
			
			// Constantes según SRI
			String tipoComprobante = "01"; // Factura
			Long ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L; // 1=Pruebas, 2=Producción
			String tipoEmision = "1"; // 1=Emisión Normal
			
			try {
				// 1. Obtener y actualizar el secuencial
				String secuencial = obtenerSecuencial(entidad.getPtoEmision().getId(), tipoComprobante);
				entidad.setSecuencial(secuencial);
				
				// 2. Generar el número de factura (formato: 001-001-000000001)
				String numero = entidad.getNumEstablecimiento() + "-" + 
						entidad.getNumPtoEmision() + "-" + secuencial;
				entidad.setNumero(numero);
				System.out.println("Número de factura generado: " + numero);
				
				// 3. Generar la clave de acceso
				String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
				entidad.setClave(clave);
				System.out.println("Clave de acceso generada: " + clave);
				
				// 4. Establecer tipo de comprobante
				entidad.setTipoComprobante(tipoComprobante);
				
				// 5. Establecer estado de emisión inicial (1=Pendiente)
				if (entidad.getEstadoEmision() == null) {
					entidad.setEstadoEmision(1L);
				}
				
			} catch (Exception e) {
				System.err.println("ERROR al generar campos automáticos de factura: " + e.getMessage());
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la factura: " + e.getMessage());
			}
		}
		
		entidad = facturaDaoService.save(entidad, entidad.getId());
		return entidad;
	}

	@Override
	public List<Factura> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByCriteria FacturaService");
		List<Factura> result = facturaDaoService.selectByCriteria(datos, NombreEntidadesCobro.FACTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio Factura no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public java.util.Map<String, Object> procesarFacturaCompleta(Factura factura, 
			java.util.List<DetalleFactura> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== INICIANDO PROCESO COMPLETO DE FACTURA ===");
		
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);
		
		try {
			// PASO 1: Grabar la factura en la base de datos con generación automática de campos
			System.out.println("PASO 1: Grabando factura en base de datos...");
			// Usar saveSingle para que genere automáticamente secuencial, clave y número
			factura = this.saveSingle(factura);
			resultado.put("factura", factura);
			resultado.put("idFactura", factura.getId());
			System.out.println("✓ Factura grabada con ID: " + factura.getId());
			System.out.println("✓ Clave generada: " + factura.getClave());
			System.out.println("✓ Número generado: " + factura.getNumero());
			System.out.println("✓ Secuencial generado: " + factura.getSecuencial());
			
			// PASO 1.5: Guardar los detalles de la factura
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 1.5: Guardando " + detalles.size() + " detalles de factura...");
				for (DetalleFactura detalle : detalles) {
					// Asignar la factura recién guardada al detalle
					detalle.setFactura(factura);
					// Asignar estado activo si no tiene
					if (detalle.getEstado() == null) {
						detalle.setEstado(Long.valueOf(Estado.ACTIVO));
					}
					// Guardar el detalle usando el servicio
					detalleFacturaService.saveSingle(detalle);
				}
				System.out.println("✓ Detalles guardados correctamente");
			} else {
				System.out.println("⚠ No hay detalles para guardar");
			}
			
			// PASO 1.6: Guardar la forma de pago de la factura (necesaria para XML según SRI)
			System.out.println("PASO 1.6: Guardando forma de pago de la factura...");
			com.saa.model.cxc.FormaPagoFactura formaPago = new com.saa.model.cxc.FormaPagoFactura();
			formaPago.setFactura(factura);
			
			// Forma de pago por defecto si no está especificada
			String codigoFormaPago = factura.getFormaPago() != null ? 
					String.valueOf(factura.getFormaPago()) : "01"; // 01 = Sin utilización del sistema financiero
			formaPago.setFormaPago(codigoFormaPago);
			
			// Valor total de la factura
			formaPago.setValor(factura.getTotal());
			
			// Plazo por defecto: 0 días (pago inmediato)
			formaPago.setPlazo(0L);
			formaPago.setUnidadTiempo("dias");
			
			formaPagoFacturaService.saveSingle(formaPago);
			System.out.println("✓ Forma de pago guardada correctamente - Código: " + codigoFormaPago);
			
			// Obtener datos necesarios
			String clave = factura.getClave();
			Long idFacturador = factura.getFacturador().getId();
			Double subsidio = factura.getSubsidio();
			
			// Configuración por defecto
			ambiente = 1L; // Siempre PRUEBA por ahora
			conectaSRI = 1L; // Siempre SI
			
			// Obtener email del titular para destinatario
			if (factura.getTitular() != null && factura.getTitular().getEmail() != null) {
				destinatario = factura.getTitular().getEmail();
			}
			
			// Path estándar del logo
			pathLogo = "resources/logos/logo_aso.png";
			
			resultado.put("clave", clave);
			resultado.put("idFacturador", idFacturador);
			resultado.put("ambiente", ambiente);
			resultado.put("destinatario", destinatario);
			
			// PASO 2: Generar XML sin firmar
			System.out.println("PASO 2: Generando XML sin firmar...");
			String[] resultadoXML = generarXMLFactura(clave, ambiente);
			String pathXMLGenerado = resultadoXML[2]; // Path absoluto
			resultado.put("xmlGenerado", resultadoXML[0]);
			resultado.put("pathXMLGenerado", pathXMLGenerado);
			System.out.println("✓ XML generado en: " + pathXMLGenerado);
			
			// Leer el XML generado
			String xmlSinFirmar = new String(Files.readAllBytes(Paths.get(pathXMLGenerado)), "UTF-8");
			
			// PASO 3: Firmar el XML
			System.out.println("PASO 3: Firmando XML electrónicamente...");
			String xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
			resultado.put("xmlFirmado", "XML firmado correctamente");
			System.out.println("✓ XML firmado electrónicamente");
			
			// PASO 4: Autorizar ante el SRI
			System.out.println("PASO 4: Autorizando ante el SRI...");
			String resultadoAutorizacion = autorizarFactura(
				idFacturador, 
				ambiente, 
				conectaSRI, 
				clave, 
				factura.getId(), 
				subsidio, 
				xmlFirmado, 
				destinatario, 
				pathLogo
			);
			
			resultado.put("autorizacion", resultadoAutorizacion);
			System.out.println("✓ Resultado autorización: " + resultadoAutorizacion);
			
			// Actualizar estado final en el resultado
			if (resultadoAutorizacion != null && 
				(resultadoAutorizacion.contains("AUTORIZADO") || resultadoAutorizacion.equals("AUTORIZADO"))) {
				resultado.put("exito", true);
				resultado.put("mensaje", "Factura procesada y autorizada exitosamente");
				resultado.put("estado", "AUTORIZADO");
			} else {
				resultado.put("exito", false);
				resultado.put("mensaje", "Factura procesada pero no autorizada");
				resultado.put("estado", "NO_AUTORIZADO");
			}
			
			System.out.println("=== PROCESO COMPLETO FINALIZADO ===");
			
		} catch (Exception e) {
			System.err.println("ERROR en procesarFacturaCompleta: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("error", e.getMessage());
			resultado.put("mensaje", "Error al procesar factura: " + e.getMessage());
			throw e;
		}
		
		return resultado;
	}
	
	@Override
	public String[] generarXMLFactura(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLFactura con clave: " + clave + " y ambiente: " + ambiente);
		
		// 1. Obtener datos principales de la factura
		String sqlFactura = "SELECT f FROM Factura f WHERE f.clave = :clave";
		Query queryFactura = em.createQuery(sqlFactura);
		queryFactura.setParameter("clave", clave);
		Factura factura = (Factura) queryFactura.getSingleResult();
		
		if (factura == null) {
			throw new IncomeException("Factura con clave " + clave + " no encontrada");
		}
		
		Long idFactura = factura.getId();
		Facturador facturador = factura.getFacturador();
		Titular titular = factura.getTitular();
		
		// 2. Obtener dirección del establecimiento
		String sqlEstablecimiento = "SELECT e.direccion FROM PuntoEmision p JOIN p.establecimiento e " +
				"WHERE p.id = :ptoEmisionId";
		Query queryEstablecimiento = em.createQuery(sqlEstablecimiento);
		queryEstablecimiento.setParameter("ptoEmisionId", factura.getPtoEmision().getId());
		String dirEstablecimiento = (String) queryEstablecimiento.getSingleResult();
		
		// 3. Obtener detalle de la factura
		String sqlDetalle = "SELECT d FROM DetalleFactura d WHERE d.factura.id = :facturaId";
		Query queryDetalle = em.createQuery(sqlDetalle);
		queryDetalle.setParameter("facturaId", idFactura);
		@SuppressWarnings("unchecked")
		List<DetalleFactura> detalles = queryDetalle.getResultList();
		
		// 4. Obtener formas de pago
		String sqlFormasPago = "SELECT fp FROM FormaPagoFactura fp WHERE fp.factura.id = :facturaId";
		Query queryFormasPago = em.createQuery(sqlFormasPago);
		queryFormasPago.setParameter("facturaId", idFactura);
		@SuppressWarnings("unchecked")
		List<com.saa.model.cxc.FormaPagoFactura> formasPago = queryFormasPago.getResultList();
		
		// 5. Generar XML
		String xmlContent = generarXMLContent(factura, facturador, titular, dirEstablecimiento, 
				detalles, formasPago, ambiente);
		
		// 6. Guardar archivo XML usando el directorio base de uploads
		String pathRelativo = "resources/" + facturador.getId() + "/docs/g/" + clave + ".xml";
		String baseUploadDir = getBaseUploadDirectory();
		String pathAbsoluto = baseUploadDir + pathRelativo;
		
		System.out.println("Guardando XML en: " + pathAbsoluto);
		
		// Crear directorios si no existen
		Path path = Paths.get(pathAbsoluto);
		Files.createDirectories(path.getParent());
		
		// Guardar archivo
		Files.write(path, xmlContent.getBytes("UTF-8"));
		
		System.out.println("✓ XML generado correctamente en: " + pathAbsoluto);
		
		return new String[]{"OK", pathRelativo, pathAbsoluto};
	}
	
	/**
	 * Genera el contenido XML de la factura electrónica según estándares del SRI v1.1.0
	 */
	private String generarXMLContent(Factura factura, Facturador facturador, Titular titular,
			String dirEstablecimiento, List<DetalleFactura> detalles, 
			List<com.saa.model.cxc.FormaPagoFactura> formasPago,
			Long ambiente) throws Exception {
		
		StringWriter stringWriter = new StringWriter();
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(stringWriter);
		
		// Constantes
		String TIPO_DOC = "01"; // Factura
		String TIPO_EMISION = "1"; // Normal
		String COD_IVA = "2";
		String COD_ICE = "3";
		String COD_IRBPNR = "5";
		String COD_POR_IVA_CERO = "0";
		String COD_POR_IVA_15 = "4";
		String COD_POR_IVA_5 = "5";
		String COD_POR_IVA_8 = "8";
		String MONEDA = "DOLAR";
		
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		// Inicio del documento
		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeCharacters("\n");
		
		// Elemento raíz: factura
		writer.writeStartElement("factura");
		writer.writeAttribute("id", factura.getClave());  // Usar clave de acceso como ID
		writer.writeAttribute("version", "1.1.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writer.writeCharacters("  ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 4);
		writeElement(writer, "tipoEmision", TIPO_EMISION, 4);
		writeElement(writer, "razonSocial", nvl(facturador.getRazonSocial(), ""), 4);
		writeElement(writer, "nombreComercial", nvl(facturador.getNombre(), ""), 4);
		writeElement(writer, "ruc", nvl(facturador.getNumDoc(), ""), 4);
		writeElement(writer, "claveAcceso", nvl(factura.getClave(), ""), 4);
		writeElement(writer, "codDoc", TIPO_DOC, 4);
		writeElement(writer, "estab", nvl(factura.getNumEstablecimiento(), ""), 4);
		writeElement(writer, "ptoEmi", nvl(factura.getNumPtoEmision(), ""), 4);
		writeElement(writer, "secuencial", nvl(factura.getSecuencial(), ""), 4);
		writeElement(writer, "dirMatriz", nvl(facturador.getDireccion(), ""), 4);
		
		// Regímenes especiales
		if (facturador.getMicroEmpresa() != null && facturador.getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 4);
		}
		if (facturador.getAgenteRetencion() != null && !facturador.getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", facturador.getAgenteRetencion(), 4);
		}
		if (facturador.getRimpe() != null && facturador.getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 4);
		}
		if (facturador.getPopularRimpe() != null && facturador.getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 4);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoTributaria
		writer.writeCharacters("\n");
		
		// infoFactura
		writer.writeCharacters("  ");
		writer.writeStartElement("infoFactura");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", factura.getFecha().format(dateFormatter), 4);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 4);
		
		if (facturador.getContribuyenteEspecial() != null && !facturador.getContribuyenteEspecial().isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", facturador.getContribuyenteEspecial(), 4);
		}
		
		String obligadoContabilidad = (facturador.getContabilidad() != null && facturador.getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligadoContabilidad, 4);
		writeElement(writer, "tipoIdentificacionComprador", String.valueOf(titular.getRubroTipoIdentificacionH()), 4);
		writeElement(writer, "razonSocialComprador", nvl(titular.getNombre(), ""), 4);
		writeElement(writer, "identificacionComprador", nvl(titular.getIdentificacion(), ""), 4);
		writeElement(writer, "direccionComprador", nvl(titular.getDireccion(), ""), 4);
		
		// Totales
		Double totalSinImpuestos = sumNulls(factura.getSubtotal(), factura.getSubcero(), 
				factura.getSubtotal5(), factura.getSubtotal8());
		writeElement(writer, "totalSinImpuestos", formatDecimal(totalSinImpuestos), 4);
		
		if (factura.getSubsidio() != null && factura.getSubsidio() > 0) {
			writeElement(writer, "totalSubsidio", formatDecimal(factura.getSubsidio()), 4);
		}
		
		writeElement(writer, "totalDescuento", formatDecimal(nvl(factura.getDescuento(), 0.0)), 4);
		
		// totalConImpuestos
		writer.writeCharacters("    ");
		writer.writeStartElement("totalConImpuestos");
		writer.writeCharacters("\n");
		
		// IVA 0%
		if (factura.getSubcero() != null && factura.getSubcero() > 0) {
			writeTotalImpuesto(writer, COD_IVA, COD_POR_IVA_CERO, factura.getSubcero(), 0.0);
		}
		
		// IVA 15%
		if (factura.getvIVA() != null && factura.getvIVA() > 0) {
			writeTotalImpuesto(writer, COD_IVA, COD_POR_IVA_15, 
					nvl(factura.getSubtotal(), 0.0), factura.getvIVA());
		}
		
		// IVA 5%
		if (factura.getvIVA5() != null && factura.getvIVA5() > 0) {
			writeTotalImpuesto(writer, COD_IVA, COD_POR_IVA_5, 
					nvl(factura.getSubtotal5(), 0.0), factura.getvIVA5());
		}
		
		// IVA 8%
		if (factura.getvIVA8() != null && factura.getvIVA8() > 0) {
			writeTotalImpuesto(writer, COD_IVA, COD_POR_IVA_8, 
					nvl(factura.getSubtotal8(), 0.0), factura.getvIVA8());
		}
		
		// ICE
		if (factura.getvICE() != null && factura.getvICE() > 0) {
			writeTotalImpuesto(writer, COD_ICE, "xxx", 
					nvl(factura.getSubtotal(), 0.0), factura.getvICE());
		}
		
		// IRBPNR
		if (factura.getvIRBPNR() != null && factura.getvIRBPNR() > 0) {
			writeTotalImpuesto(writer, COD_IRBPNR, "xxx", 
					nvl(factura.getSubtotal(), 0.0), factura.getvIRBPNR());
		}
		
		writer.writeCharacters("    ");
		writer.writeEndElement(); // totalConImpuestos
		writer.writeCharacters("\n");
		
		writeElement(writer, "propina", formatDecimal(nvl(factura.getPropina(), 0.0)), 4);
		writeElement(writer, "importeTotal", formatDecimal(nvl(factura.getTotal(), 0.0)), 4);
		writeElement(writer, "moneda", MONEDA, 4);
		
		// Formas de pago
		writer.writeCharacters("    ");
		writer.writeStartElement("pagos");
		writer.writeCharacters("\n");
		
		// Si no hay formas de pago, agregar una por defecto (01 = Sin utilización del sistema financiero)
		if (formasPago == null || formasPago.isEmpty()) {
			String codigoFormaPago = factura.getFormaPago() != null ? 
					String.valueOf(factura.getFormaPago()) : "01";
			writePago(writer, codigoFormaPago, factura.getTotal(), "0", "dias");
		} else {
			// Iterar sobre las formas de pago y agregarlas al XML
			for (com.saa.model.cxc.FormaPagoFactura fp : formasPago) {
				String plazoStr = fp.getPlazo() != null ? String.valueOf(fp.getPlazo()) : null;
				writePago(writer, 
						fp.getFormaPago(), 
						fp.getValor(), 
						plazoStr, 
						fp.getUnidadTiempo());
			}
		}
		
		writer.writeCharacters("    ");
		writer.writeEndElement(); // pagos
		writer.writeCharacters("\n");
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoFactura
		writer.writeCharacters("\n");
		
		// detalles
		writer.writeCharacters("  ");
		writer.writeStartElement("detalles");
		writer.writeCharacters("\n");
		
		for (DetalleFactura detalle : detalles) {
			writeDetalle(writer, detalle, COD_IVA);
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // detalles
		writer.writeCharacters("\n");
		
		// infoAdicional
		writer.writeCharacters("  ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		
		writer.writeCharacters("    ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(factura.getObservacion(), "") + "]");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // infoAdicional
		writer.writeCharacters("\n");
		
		writer.writeEndElement(); // factura
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
		writer.writeCharacters("    ".repeat(indent / 2));
		writer.writeStartElement(name);
		writer.writeCharacters(value);
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
	
	private void writePago(XMLStreamWriter writer, String formaPago, Double total, 
			String plazo, String unidadTiempo) throws Exception {
		writer.writeCharacters("      ");
		writer.writeStartElement("pago");
		writer.writeCharacters("\n");
		writeElement(writer, "formaPago", formaPago, 8);
		writeElement(writer, "total", formatDecimal(total), 8);
		if (plazo != null) {
			writeElement(writer, "plazo", plazo, 8);
		}
		if (unidadTiempo != null) {
			writeElement(writer, "unidadTiempo", unidadTiempo, 8);
		}
		writer.writeCharacters("      ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeDetalle(XMLStreamWriter writer, DetalleFactura detalle, String codIVA) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("detalle");
		writer.writeCharacters("\n");
		
		// Incluir codigoPrincipal solo si el producto tiene código
		if (detalle.getProducto() != null && detalle.getProducto().getCodigo() != null 
				&& !detalle.getProducto().getCodigo().trim().isEmpty()) {
			writeElement(writer, "codigoPrincipal", detalle.getProducto().getCodigo(), 6);
		}
		
		// Incluir codigoAuxiliar solo si existe
		if (detalle.getProducto() != null && detalle.getProducto().getCodigoAux() != null 
				&& !detalle.getProducto().getCodigoAux().trim().isEmpty()) {
			writeElement(writer, "codigoAuxiliar", detalle.getProducto().getCodigoAux(), 6);
		}
		
		writeElement(writer, "descripcion", nvl(detalle.getDescripcion(), ""), 6);
		writeElement(writer, "cantidad", formatDecimal(detalle.getCantidad()), 6);
		writeElement(writer, "precioUnitario", formatDecimal(detalle.getValor()), 6);
		
		if (detalle.getPrecioSinSub() != null && detalle.getPrecioSinSub() > 0) {
			writeElement(writer, "precioSinSubsidio", formatDecimal(detalle.getPrecioSinSub()), 6);
		}
		
		writeElement(writer, "descuento", formatDecimal(nvl(detalle.getDescuento(), 0.0)), 6);
		writeElement(writer, "precioTotalSinImpuesto", formatDecimal(detalle.getBaseImponible()), 6);
		
		// Impuestos del detalle
		writer.writeCharacters("      ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");
		
		writer.writeCharacters("        ");
		writer.writeStartElement("impuesto");
		writer.writeCharacters("\n");
		writeElement(writer, "codigo", codIVA, 10);
		writeElement(writer, "codigoPorcentaje", String.valueOf(detalle.getCodigoIVASRI()), 10);
		writeElement(writer, "tarifa", String.valueOf(detalle.getPorcentajeIVA()), 10);
		writeElement(writer, "baseImponible", formatDecimal(detalle.getBaseImponible()), 10);
		writeElement(writer, "valor", formatDecimal(nvl(detalle.getValorIVA(), 0.0)), 10);
		writer.writeCharacters("        ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		
		writer.writeCharacters("      ");
		writer.writeEndElement(); // impuestos
		writer.writeCharacters("\n");
		
		writer.writeCharacters("    ");
		writer.writeEndElement(); // detalle
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
	public String autorizarFactura(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoFactura, Double subsidio, String xml, String destinatario, String pathLogo) throws Throwable {
		System.out.println("Ingresa al metodo autorizarFactura con clave: " + clave);
		
		String respuesta = "";
		// Usar directorio base de uploads en lugar de user.dir
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado
			String strXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
			Path pathFirmado = Paths.get(resourcesPath + "/docs/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, strXML.getBytes("UTF-8"));
			
			// 2. Insertar path firmado en tabla ptfc (alterno=3)
			PathFactura pathF = new PathFactura();
			Factura factura = facturaDaoService.selectById(codigoFactura, NombreEntidadesCobro.FACTURA);
			pathF.setFactura(factura);
			pathF.setPath("resources/" + idFacturador + "/docs/f/" + clave + ".xml");
			pathF.setAlterno(3L); // 3 = XML firmado
			pathFacturaDaoService.save(pathF, null);
			
			// 3. Actualizar estado a FIRMADA (estado=3)
			factura.setEstado(3L);
			facturaDaoService.save(factura, factura.getId());
			
			if (conectaSRI == 1) {
				// 4. Llamar al Web Service 1 - Recepción
				String urlWS1 = ambiente == 1 
						? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
						: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
				
				try {
					// Crear archivo de log WS1
					Path logWS1 = Paths.get(resourcesPath + "/docs/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					
					// Llamar servicio de recepción
					String contenidoXML = new String(Files.readAllBytes(pathFirmado), "UTF-8");
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, contenidoXML, logWriter1);
					
					logWriter1.close();
					
					// Guardar XML enviado
					Path pathEnviado = Paths.get(resourcesPath + "/docs/e/" + clave + ".xml");
					Files.write(pathEnviado, contenidoXML.getBytes("UTF-8"));
					
					// Insertar path enviado en tabla ptfc (alterno=4)
					PathFactura pathE = new PathFactura();
					pathE.setFactura(factura);
					pathE.setPath("resources/" + idFacturador + "/docs/e/" + clave + ".xml");
					pathE.setAlterno(4L); // 4 = XML enviado
					pathFacturaDaoService.save(pathE, null);
					
					// Actualizar estado a ENVIADA (estado=4)
					factura.setEstado(4L);
					facturaDaoService.save(factura, factura.getId());
					
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
								Path logWS2A = Paths.get(resourcesPath + "/docs/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								// Guardar XML autorizado
								Path pathAutorizado = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								// Insertar path autorizado en tabla ptfc (alterno=5)
								PathFactura pathA = new PathFactura();
								pathA.setFactura(factura);
								pathA.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".xml");
								pathA.setAlterno(5L); // 5 = XML autorizado
								pathFacturaDaoService.save(pathA, null);
								
								// Actualizar estado a AUTORIZADA (estado=5, estadoEmision=1)
								factura.setEstado(5L);
								factura.setEstadoEmision(1L);
								factura.setAutorizacion(resultado.numeroAutorizacion);
								factura.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								facturaDaoService.save(factura, factura.getId());
								
								respuesta = resultado.estado;
								
								// Generar PDF
								// TODO: Implementar generación de PDF según subsidio
								// if (subsidio != null && subsidio.compareTo(BigDecimal.ZERO) > 0) {
								//     crearPDFSubsidio(clave);
								// } else {
								//     crearPDF(clave);
								// }
								
								// Si es producción, actualizar contador y enviar email
								if (ambiente == 2) {
									// Actualizar contador de documentos emitidos
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
									
									// TODO: Enviar email
									// if (destinatario != null && !destinatario.isEmpty()) {
									//     String resultadoMail = enviarMail(destinatario, idFacturador, clave, pathLogo);
									//     respuesta = respuesta + " " + resultadoMail;
									// }
								}
								
							} else {
								// NO AUTORIZADA
								Path logWS2N = Paths.get(resourcesPath + "/docs/n/" + clave + ".txt");
								Files.createDirectories(logWS2N.getParent());
								PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
								logWriter2N.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2N.close();
								
								if (resultado.comprobanteXML != null) {
									// Guardar XML no autorizado
									Path pathNoAutorizado = Paths.get(resourcesPath + "/docs/n/" + clave + ".xml");
									Files.write(pathNoAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
									
									// Insertar path no autorizado en tabla ptfc (alterno=6)
									PathFactura pathN = new PathFactura();
									pathN.setFactura(factura);
									pathN.setPath("resources/" + idFacturador + "/docs/n/" + clave + ".xml");
									pathN.setAlterno(6L); // 6 = XML no autorizado
									pathFacturaDaoService.save(pathN, null);
								}
								
								// Actualizar estado a NO AUTORIZADA (estado=6, estadoEmision=2)
								factura.setEstado(6L);
								factura.setEstadoEmision(2L); // 2 = PENDIENTE
								facturaDaoService.save(factura, factura.getId());
								
								respuesta = "Estado: " + resultado.estado + 
										" Id: " + nvl(resultado.mensajeId, "") +
										" Mensaje: " + nvl(resultado.mensaje, "") +
										" / " + nvl(resultado.informacionAdicional, "");
							}
							
						} catch (Exception e) {
							// Error en autorización
							Path logWS2Error = Paths.get(resourcesPath + "/docs/n/" + clave + ".txt");
							Files.createDirectories(logWS2Error.getParent());
							PrintWriter logWriter2E = new PrintWriter(new FileWriter(logWS2Error.toFile()));
							logWriter2E.println("Error al llamar SRI_2: " + e.getMessage());
							e.printStackTrace(logWriter2E);
							logWriter2E.close();
							
							// Guardar XML en carpeta de no autorizados
							Files.copy(pathFirmado, Paths.get(resourcesPath + "/docs/n/" + clave + ".xml"));
							
							// Actualizar estado a NO AUTORIZADA (estado=6, estadoEmision=2)
							factura.setEstado(6L);
							factura.setEstadoEmision(2L);
							facturaDaoService.save(factura, factura.getId());
							
							respuesta = "Error al llamar SRI_2: " + e.getMessage();
						}
						
					} else {
						// Estado diferente a RECIBIDA (puede ser CLAVE REGISTRADA)
						respuesta = "Estado: " + estadoRecepcion;
						
						// Verificar si es clave ya registrada
						if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
							respuesta = "Comprobante Autorizado";
							factura.setAutorizacion(clave);
							// Convertir LocalDate a LocalDateTime y agregar tiempo
							factura.setFechaAutorizacion(factura.getFecha().atStartOfDay().plusMinutes(1).plusSeconds(15));
							factura.setEstado(5L);
							facturaDaoService.save(factura, factura.getId());
						}
					}
					
				} catch (Exception e) {
					respuesta = "Error al llamar SRI_1: " + e.getMessage();
					e.printStackTrace();
				}
				
			} else {
				respuesta = "Factura Generada pero no enviada";
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new IncomeException("Error en autorizarFactura: " + e.getMessage());
		}
		
		return respuesta;
	}
	
	/**
	 * Llama al servicio de recepción del SRI
	 */
	private String llamarRecepcionSRI(String url, String xmlContent, PrintWriter log) throws Exception {
		try {
			System.out.println(">>> Llamando al WS1 de RECEPCIÓN del SRI: " + url);
			log.println(">>> Llamando al WS1 de RECEPCIÓN del SRI: " + url);
			
			// Crear conexión SOAP
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			// Crear mensaje SOAP
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			
			// SOAP Envelope
			SOAPEnvelope envelope = soapPart.getEnvelope();
			
			// SOAP Body
			SOAPBody soapBody = envelope.getBody();
			
			// Crear elemento validarComprobante CON namespace
			SOAPElement validarComprobante = soapBody.addChildElement("validarComprobante", "", "http://ec.gob.sri.ws.recepcion");
			// Crear elemento xml SIN namespace usando createElementNS con namespace vacío
			SOAPElement xml = validarComprobante.addChildElement(envelope.createName("xml", "", ""));
			xml.addTextNode(xmlContent);
			
			soapMessage.saveChanges();
			
			System.out.println(">>> Mensaje SOAP Request creado correctamente");
			log.println(">>> Mensaje SOAP Request creado correctamente");
			
			// Log del request completo
			ByteArrayOutputStream requestBaos = new ByteArrayOutputStream();
			soapMessage.writeTo(requestBaos);
			String requestXml = requestBaos.toString("UTF-8");
			System.out.println(">>> REQUEST SOAP enviado al SRI:");
			System.out.println(requestXml);
			log.println(">>> REQUEST SOAP enviado al SRI:");
			log.println(requestXml);
			
			// Llamar al servicio
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			System.out.println(">>> Respuesta recibida del SRI");
			log.println(">>> Respuesta recibida del SRI");
			
			// Procesar respuesta - Convertir SOAP a String XML correctamente
			SOAPBody responseBody = soapResponse.getSOAPBody();
			
			// Convertir el SOAPMessage completo a String XML
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			soapResponse.writeTo(baos);
			String respuestaCompleta = baos.toString("UTF-8");
			
			System.out.println(">>> Respuesta WS1 completa:");
			System.out.println(respuestaCompleta);
			log.println(">>> Respuesta WS1 completa:");
			log.println(respuestaCompleta);
			
			// Extraer estado
			// Primero intentar buscar por getElementsByTagName (sin namespace)
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			System.out.println(">>> Nodos <estado> encontrados (sin NS): " + estadoList.getLength());
			log.println(">>> Nodos <estado> encontrados (sin NS): " + estadoList.getLength());
			
			// Si no encuentra, intentar con namespace
			if (estadoList.getLength() == 0) {
				estadoList = responseBody.getElementsByTagNameNS("*", "estado");
				System.out.println(">>> Nodos <estado> encontrados (con NS): " + estadoList.getLength());
				log.println(">>> Nodos <estado> encontrados (con NS): " + estadoList.getLength());
			}
			
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado extraído: " + estado);
				log.println(">>> Estado extraído: " + estado);
				
				// Verificar si hay mensaje de clave registrada
				NodeList mensajeList = responseBody.getElementsByTagName("mensaje");
				if (mensajeList.getLength() == 0) {
					mensajeList = responseBody.getElementsByTagNameNS("*", "mensaje");
				}
				
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					System.out.println(">>> Mensaje extraído: " + mensaje);
					log.println(">>> Mensaje extraído: " + mensaje);
					
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						System.out.println(">>> Clave de acceso ya registrada");
						log.println(">>> Clave de acceso ya registrada");
						soapConnection.close();
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				
				soapConnection.close();
				return estado;
			}
			
			System.out.println(">>> ADVERTENCIA: No se encontró nodo <estado> en la respuesta");
			log.println(">>> ADVERTENCIA: No se encontró nodo <estado> en la respuesta");
			
			soapConnection.close();
			return "SIN_RESPUESTA";
			
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarRecepcionSRI: " + e.getMessage());
			e.printStackTrace();
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
			// Crear conexión SOAP
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();
			
			// Crear mensaje SOAP
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage soapMessage = messageFactory.createMessage();
			SOAPPart soapPart = soapMessage.getSOAPPart();
			
			// SOAP Envelope
			SOAPEnvelope envelope = soapPart.getEnvelope();
			envelope.addNamespaceDeclaration("ec", "http://ec.gob.sri.ws.autorizacion");
			
			// SOAP Body
			SOAPBody soapBody = envelope.getBody();
			SOAPElement autorizacionComprobante = soapBody.addChildElement("autorizacionComprobante", "ec");
			SOAPElement claveAccesoElement = autorizacionComprobante.addChildElement("claveAccesoComprobante", "ec");
			claveAccesoElement.addTextNode(claveAcceso);
			
			soapMessage.saveChanges();
			
			// Llamar al servicio
			SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
			
			// Procesar respuesta
			SOAPBody responseBody = soapResponse.getSOAPBody();
			String respuestaCompleta = soapResponse.getSOAPPart().getEnvelope().toString();
			
			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;
			
			// Extraer estado
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				resultado.estado = estadoList.item(0).getTextContent();
			}
			
			// Extraer número de autorización
			NodeList numAutList = responseBody.getElementsByTagName("numeroAutorizacion");
			if (numAutList.getLength() > 0) {
				resultado.numeroAutorizacion = numAutList.item(0).getTextContent();
			}
			
			// Extraer fecha de autorización
			NodeList fechaAutList = responseBody.getElementsByTagName("fechaAutorizacion");
			if (fechaAutList.getLength() > 0) {
				resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();
			}
			
			// Extraer comprobante XML
			NodeList comprobanteList = responseBody.getElementsByTagName("comprobante");
			if (comprobanteList.getLength() > 0) {
				resultado.comprobanteXML = comprobanteList.item(0).getTextContent();
			}
			
			// Extraer mensajes de error si existen
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
			// Formato: dd/MM/yyyy HH:mm:ss
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
			return LocalDateTime.parse(fechaStr, formatter);
		} catch (Exception e) {
			// Si falla, retornar fecha actual
			return LocalDateTime.now();
		}
	}
	
	/**
	 * Clase interna para almacenar resultado de autorización
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
	
	/**
	 * Obtiene y actualiza el secuencial para un punto de emisión y tipo de documento
	 */
	private String obtenerSecuencial(Long idPtoEmision, String tipoDoc) throws Exception {
		System.out.println(">>> OBTENER SECUENCIAL PtoEmision[" + idPtoEmision + "] TipoComprobante[" + tipoDoc + "]");
		
		// Consultar numeración actual
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
		
		// Actualizar numeración
		String sqlUpdate = "UPDATE NumeracionPuntoEmision n SET n.numActual = :nuevoNumero " +
				"WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query updateQuery = em.createQuery(sqlUpdate);
		updateQuery.setParameter("nuevoNumero", nuevoNumero);
		updateQuery.setParameter("ptoEmision", idPtoEmision);
		updateQuery.setParameter("tipoDoc", tipoDoc);
		updateQuery.executeUpdate();
		
		// Formatear secuencial a 9 dígitos
		String secuencial = String.format("%09d", numeroActual);
		System.out.println("Secuencial generado: " + secuencial);
		
		return secuencial;
	}
	
	/**
	 * Genera la clave de acceso usando el algoritmo módulo 11
	 */
	private String generarClaveAcceso(Factura factura, String tipoComprobante, Long ambiente, 
			String tipoEmision, String secuencial) {
		
		// Formato de fecha ddMMyyyy
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = factura.getFecha().format(formatter);
		
		// Obtener datos del facturador
		String ruc = factura.getFacturador().getNumDoc();
		String codClave = factura.getFacturador().getCodClave();
		
		System.out.println("RUC: " + ruc);
		System.out.println("CLAVE: " + codClave);
		System.out.println("AMBIENTE: " + ambiente);
		System.out.println("ESTABLECIMIENTO: " + factura.getNumEstablecimiento());
		System.out.println("PTO: " + factura.getNumPtoEmision());
		
		// Armar clave sin dígito verificador
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente + 
				factura.getNumEstablecimiento() + factura.getNumPtoEmision() + 
				secuencial + codClave + tipoEmision;
		
		System.out.println(">>> GENERADOR CLAVE cadena[" + claveSinDV + "]");
		
		// Calcular dígito verificador con módulo 11
		int digitoVerificador = calcularModulo11(claveSinDV);
		
		String claveCompleta = claveSinDV + digitoVerificador;
		System.out.println(">>> CLAVE COMPLETA [" + claveCompleta + "]");
		
		return claveCompleta;
	}
	
	/**
	 * Calcula el dígito verificador usando módulo 11
	 */
	private int calcularModulo11(String cadena) {
		// Invertir la cadena
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
		
		// Casos especiales
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
