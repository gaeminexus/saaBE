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

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

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
			
			if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la liquidación de compra");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la liquidación de compra");
			}
			
			String tipoComprobante = "03"; // Liquidación de Compra
			Long ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
			String tipoEmision = "1";
			
			try {
				String secuencial = obtenerSecuencial(entidad.getPtoEmision().getId(), tipoComprobante);
				entidad.setSecuencial(secuencial);
				
				String numero = entidad.getNumEstablecimiento() + "-" +
						entidad.getNumPtoEmision() + "-" + secuencial;
				entidad.setNumero(numero);
				System.out.println("Número de liquidación generado: " + numero);
				
				String clave = generarClaveAcceso(entidad, tipoComprobante, ambiente, tipoEmision, secuencial);
				entidad.setClave(clave);
				System.out.println("Clave de acceso generada: " + clave);
				
				entidad.setTipoComprobante(tipoComprobante);
				
				if (entidad.getEstadoEmision() == null) {
					entidad.setEstadoEmision(1L);
				}
			} catch (Exception e) {
				System.err.println("ERROR al generar campos automáticos de liquidación: " + e.getMessage());
				e.printStackTrace();
				throw new IncomeException("Error al generar datos de la liquidación: " + e.getMessage());
			}
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
			
			boolean autorizada = resultadoAutorizacion != null &&
				(resultadoAutorizacion.contains("AUTORIZADO") || resultadoAutorizacion.equals("AUTORIZADO"));

			if (autorizada) {
				// ── PASO 5: Generar asiento contable ─────────────────────────
				// Solo si el facturador tiene generaConta=1 y empresa contable configurada.
				if (liquidacion.getFacturador() != null
						&& liquidacion.getFacturador().getEmpresa() != null
						&& Long.valueOf(1L).equals(liquidacion.getFacturador().getGeneraConta())) {
					System.out.println("PASO 5: Generando asiento contable para Liquidación de Compra...");
					try {
						Long idEmpresaConta = liquidacion.getFacturador().getEmpresa().getCodigo();
						LiquidacionCompra lqActualizada = liquidacionCompraDaoService.selectById(
								liquidacion.getId(), NombreEntidadesCobro.LIQUIDACION_COMPRA);
						java.time.LocalDate fechaAsiento = lqActualizada.getFecha() != null
								? lqActualizada.getFecha().toLocalDate() : java.time.LocalDate.now();
						String obsAsiento = "Liquidación de Compra N° " + nvl(lqActualizada.getNumero(), clave)
								+ " | Proveedor: " + (lqActualizada.getTitular() != null ? lqActualizada.getTitular().getNombre() : "")
								+ " | Aut: " + nvl(lqActualizada.getAutorizacion(), clave);
						String usuarioAsiento = (lqActualizada.getUsuario() != null)
								? lqActualizada.getUsuario().getNombre() : "SISTEMA";
						// TODO: Reemplazar TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS con el
						//       codigoAlterno correcto una vez que se defina la plantilla en BD.
						com.saa.model.cnt.Asiento asientoGenerado =
								asientoContableService.generarAsientoLiquidacionCompra(
										lqActualizada.getId(), idEmpresaConta,
										com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS,
										fechaAsiento, obsAsiento, usuarioAsiento);
						resultado.put("asiento", asientoGenerado.getNumeroAlterno());
						System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());
					} catch (Exception e) {
						resultado.put("advertenciaAsiento",
								"Liquidación autorizada pero ocurrió un error al generar el asiento: "
								+ e.getMessage()
								+ ". Genere el asiento manualmente desde Contabilidad.");
						System.err.println("⚠ Error en asiento contable de Liquidación de Compra: " + e.getMessage());
						e.printStackTrace();
					}
				}
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
			String baseUploadDir = getBaseUploadDirectory();
			String pathAbsoluto = baseUploadDir + pathRelativo;
			
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
		
		// NO escribir declaración XML: el proceso de firma la agrega automáticamente
		
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
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;
		
		try {
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma)
			Path pathFirmado = Paths.get(resourcesPath + "/lqcs/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
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
					
					// Leer bytes crudos del XML firmado (NO convertir a String, preserva la firma)
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					
					logWriter1.close();
					
					// Guardar copia exacta del XML enviado
					Path pathEnviado = Paths.get(resourcesPath + "/lqcs/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
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
			log.println("Respuesta WS1: " + respuestaCompleta);
			
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
	
	/**
	 * Llama al servicio de autorización del SRI
	 */
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
	
	private String obtenerSecuencial(Long idPtoEmision, String tipoDoc) throws Exception {
		String sql = "SELECT n FROM NumeracionPuntoEmision n WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query query = em.createQuery(sql);
		query.setParameter("ptoEmision", idPtoEmision);
		query.setParameter("tipoDoc", tipoDoc);
		@SuppressWarnings("unchecked")
		List<Object> resultados = query.getResultList();
		if (resultados.isEmpty()) {
			throw new IncomeException("No existe numeración para el punto de emisión " + idPtoEmision + " y tipo " + tipoDoc);
		}
		com.saa.model.cxc.NumeracionPuntoEmision numeracion = (com.saa.model.cxc.NumeracionPuntoEmision) resultados.get(0);
		Long numeroActual = numeracion.getNumActual();
		String sqlUpdate = "UPDATE NumeracionPuntoEmision n SET n.numActual = :nuevo WHERE n.ptoEmision.id = :ptoEmision AND n.tipoDoc = :tipoDoc";
		Query updateQuery = em.createQuery(sqlUpdate);
		updateQuery.setParameter("nuevo", numeroActual + 1);
		updateQuery.setParameter("ptoEmision", idPtoEmision);
		updateQuery.setParameter("tipoDoc", tipoDoc);
		updateQuery.executeUpdate();
		return String.format("%09d", numeroActual);
	}
	
	private String generarClaveAcceso(LiquidacionCompra lc, String tipoComprobante, Long ambiente,
			String tipoEmision, String secuencial) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
		String fechaClave = lc.getFecha().format(formatter);
		String ruc = lc.getFacturador().getNumDoc();
		String codClave = lc.getFacturador().getCodClave();
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente +
				lc.getNumEstablecimiento() + lc.getNumPtoEmision() +
				secuencial + codClave + tipoEmision;
		System.out.println(">>> GENERADOR CLAVE cadena[" + claveSinDV + "]");
		int dv = calcularModulo11(claveSinDV);
		String claveCompleta = claveSinDV + dv;
		System.out.println(">>> CLAVE COMPLETA [" + claveCompleta + "]");
		return claveCompleta;
	}
	
	private int calcularModulo11(String cadena) {
		String invertida = new StringBuilder(cadena).reverse().toString();
		int suma = 0;
		int factor = 2;
		for (int i = 0; i < invertida.length(); i++) {
			suma += Character.getNumericValue(invertida.charAt(i)) * factor;
			if (factor == 7) factor = 2; else factor++;
		}
		int dv = 11 - (suma % 11);
		if (dv == 10) return 1;
		else if (dv == 11) return 0;
		return dv;
	}
	
	/**
	 * Obtiene el directorio base de uploads desde la variable de sistema
	 */
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
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("windows")) {
			return userHome + "/saa-uploads/";
		} else {
			return "/opt/saa-uploads/";
		}
	}
}
