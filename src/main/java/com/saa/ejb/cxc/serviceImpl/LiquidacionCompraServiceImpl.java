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

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
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
public class LiquidacionCompraServiceImpl implements LiquidacionCompraService {

	@EJB
	private LiquidacionCompraDaoService liquidacionCompraDaoService;

	@Resource
	private SessionContext sessionContext;

	/**
	 * Referencia al propio bean pasando por el contenedor, para que los
	 * @TransactionAttribute de las etapas se apliquen de verdad. Una llamada
	 * directa a this.metodo() se salta los interceptores y correría en la
	 * transacción del llamador.
	 * @return : Vista local de este mismo EJB
	 */
	private LiquidacionCompraService self() {
		return sessionContext.getBusinessObject(LiquidacionCompraService.class);
	}
	
	@EJB
	private PathLiquidacionCompraDaoService pathLiquidacionCompraDaoService;

	@EJB
	private SignatureService signatureService;

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

	@EJB
	private com.saa.ejb.reporte.service.ReporteService reporteService;

	@EJB
	private com.saa.ejb.cxc.service.EmailFacturaService emailFacturaService;

	@EJB
	private com.saa.ejb.cxp.service.SustentoTributarioService sustentoTributarioService;

	@EJB
	private com.saa.ejb.cxc.service.AplicacionPagoCxcService aplicacionPagoCxcService;

	@EJB
	private com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService aplicacionPagoCxcDaoService;

	/** Catálogo de rubros: traduce el tipo de identificación interno al código del SRI. Ver
	 *  {@link #resolverTipoIdentificacionSRI(com.saa.model.tsr.Titular)}. */
	@EJB
	private com.saa.basico.ejb.DetalleRubroService detalleRubroService;

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
			String tipoEmision = "1";
			
			// Obtener ambiente desde el facturador (BD) — fuente autoritativa por seguridad
			com.saa.model.cxc.Facturador facturadorDB = em.find(com.saa.model.cxc.Facturador.class, entidad.getFacturador().getId());
			Long ambiente;
			if (facturadorDB != null && facturadorDB.getAmbiente() != null) {
				ambiente = facturadorDB.getAmbiente();
				System.out.println("Ambiente tomado del facturador (BD): " + ambiente
						+ (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)"));
			} else {
				ambiente = entidad.getAmbiente() != null ? entidad.getAmbiente() : 1L;
				System.out.println("⚠ Facturador sin ambiente configurado en BD, usando valor recibido: " + ambiente);
			}
			entidad.setAmbiente(ambiente); // sincronizar el valor correcto en la entidad
			
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
	
	/**
	 * Orquesta el proceso completo SIN transacción propia (NOT_SUPPORTED).
	 * <p>
	 * El envío al SRI es irreversible, así que la emisión se confirma en su
	 * propia transacción y el asiento contable corre aparte: un fallo tardío
	 * NUNCA puede reversar una liquidación ya autorizada por el SRI.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> procesarLiquidacionCompleta(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== INICIANDO PROCESO COMPLETO DE LIQUIDACION DE COMPRA ===");

		// ── PASO 0: Validar cuentas contables (sin escribir en BD) ────────────
		// La liquidación emitida no genera su propia cuenta por pagar: al
		// autorizarse crea un documento CXP (crearDocumentoCxp) que se
		// contabiliza como liquidación recibida. Validar ANTES de emitir para
		// no descubrir una cuenta faltante después de que el SRI ya autorizó
		// el comprobante, momento en el que ya no se puede revertir.
		if (liquidacion.getFacturador() != null
				&& Long.valueOf(1L).equals(liquidacion.getFacturador().getGeneraConta())) {
			if (liquidacion.getFacturador().getEmpresa() == null) {
				java.util.Map<String, Object> resultado = new java.util.HashMap<>();
				resultado.put("exito", false);
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
						+ "pero no tiene empresa contable configurada. "
						+ "Configure el campo EMPRESA en el facturador.");
				return resultado;
			}
			Long idEmpresa = liquidacion.getFacturador().getEmpresa().getCodigo();
			System.out.println("PASO 0: Validando cuentas contables para empresa " + idEmpresa + "...");
			java.util.List<String> erroresContables = asientoContableService
					.validarCuentasContablesLiquidacion(liquidacion, detalles, idEmpresa);
			if (!erroresContables.isEmpty()) {
				java.util.Map<String, Object> resultado = new java.util.HashMap<>();
				resultado.put("exito", false);
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "No se puede emitir la liquidación: faltan cuentas contables. "
						+ "Corrija los siguientes problemas antes de continuar:");
				resultado.put("erroresContables", erroresContables);
				System.err.println("✗ Validación contable fallida: " + erroresContables);
				return resultado;
			}
			System.out.println("✓ Validación contable OK: todas las cuentas están configuradas.");
		}

		// ── Emisión ante el SRI, en UNA transacción propia ────────────────────
		java.util.Map<String, Object> resultado = self().emitirLiquidacionAnteSRI(
				liquidacion, detalles, formasPago, ambiente, conectaSRI, destinatario, pathLogo);

		if (!Boolean.TRUE.equals(resultado.get("emitida"))) {
			return resultado;
		}

		Long idLiquidacion = (Long) resultado.get("idLiquidacion");
		Long idFacturador  = (Long) resultado.get("idFacturador");
		String clave       = (String) resultado.get("clave");
		byte[] pdfBytesParaEmail = (byte[]) resultado.get("pdfBytes");
		destinatario = (String) resultado.get("destinatario");

		// ── PASO 5: Crear el documento CXP y contabilizarlo (transacción propia) ──
		System.out.println("PASO 5: Creando documento CXP para la Liquidación de Compra...");
		try {
			java.util.Map<String, Object> resCxp = self().crearDocumentoCxp(idLiquidacion);
			if (Boolean.TRUE.equals(resCxp.get("aplica"))) {
				resultado.put("documentoCxp", resCxp.get("idDocumentoCxp"));
				resultado.put("asiento", resCxp.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"Liquidación autorizada pero ocurrió un error al crear el documento CXP / asiento: "
					+ e.getMessage() + ". Use POST /lqcs/crearDocumentoCxp/" + idLiquidacion + " para reintentar.");
			System.err.println("⚠ Error creando documento CXP de Liquidación de Compra: " + e.getMessage());
			e.printStackTrace();
		}

		// ── PASO 6: Enviar correo electrónico ─────────────────────────────────
		System.out.println("PASO 6: Enviando email...");
		try {
			if (destinatario != null && !destinatario.trim().isEmpty()) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				try {
					java.nio.file.Path pXml = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".xml");
					if (Files.exists(pXml)) xmlAutorizado = new String(Files.readAllBytes(pXml), "UTF-8");
				} catch (Exception ioEx) {
					System.err.println("⚠ No se pudo leer el XML para el email: " + ioEx.getMessage());
				}
				String razonSocial = liquidacion.getFacturador() != null
						? nvl(liquidacion.getFacturador().getRazonSocial(),
							  nvl(liquidacion.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(destinatario, nvl(liquidacion.getNumero(), clave),
						clave, razonSocial, "Liquidación de Compra", xmlAutorizado, pdfBytesParaEmail);
				resultado.put("emailEnviado", true);
				System.out.println("✓ Email enviado a: " + destinatario);
			} else {
				resultado.put("emailEnviado", false);
				System.out.println("ℹ Email omitido: no hay dirección de correo del proveedor.");
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail", "La liquidación fue autorizada pero no se pudo enviar el email: "
					+ mailEx.getMessage() + ". Reenvíe el email manualmente.");
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

		boolean hayPendientes = Boolean.TRUE.equals(resultado.get("contabilidadPendiente"));
		resultado.put("exito",  true);
		resultado.put("estado", "AUTORIZADO");
		resultado.put("etapa",  hayPendientes ? "COMPLETADO_CON_PENDIENTES" : "COMPLETADO");
		resultado.put("mensaje", hayPendientes
				? "Liquidación autorizada por el SRI, pero quedaron etapas pendientes. Revise las advertencias."
				: "Liquidación procesada y autorizada exitosamente");
		System.out.println("=== PROCESO COMPLETO DE LIQUIDACION FINALIZADO"
				+ (hayPendientes ? " (CON PENDIENTES)" : "") + " ===");
		return resultado;
	}

	/**
	 * Emite la liquidación de compra ante el SRI en UNA transacción propia
	 * (REQUIRES_NEW): prepara campos, genera y firma el XML, envía a recepción
	 * y —sólo si el SRI la acepta— graba el documento y persiste la autorización.
	 * @return : Mapa con clave, idLiquidacion y emitida=true si el SRI la autorizó
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> emitirLiquidacionAnteSRI(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== emitirLiquidacionAnteSRI (BD tras RECIBIDA) ===");
		byte[] pdfBytesParaEmail = null;

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		try {
			if (ambiente  == null) ambiente  = 1L;
			if (conectaSRI == null) conectaSRI = 1L;
			if (pathLogo  == null) pathLogo  = "resources/logos/logo_aso.png";
			if (destinatario == null && liquidacion.getTitular() != null)
				destinatario = liquidacion.getTitular().getEmail();

			// ── PASO 1: Preparar campos en MEMORIA (sin guardar en BD) ──────────
			System.out.println("PASO 1: Preparando campos de la liquidación en memoria...");
			if (liquidacion.getEstado() == null) liquidacion.setEstado(Long.valueOf(Estado.ACTIVO));
			if (liquidacion.getPtoEmision() == null || liquidacion.getPtoEmision().getId() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un punto de emisión.");
				return resultado;
			}
			if (liquidacion.getFacturador() == null || liquidacion.getFacturador().getId() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un facturador.");
				return resultado;
			}

			String tipoComprobante = "03";
			String tipoEmision = "1";
			com.saa.model.cxc.Facturador facturadorDB = em.find(com.saa.model.cxc.Facturador.class, liquidacion.getFacturador().getId());
			Long ambienteFacturador;
			if (facturadorDB != null && facturadorDB.getAmbiente() != null) {
				ambienteFacturador = facturadorDB.getAmbiente();
			} else {
				ambienteFacturador = liquidacion.getAmbiente() != null ? liquidacion.getAmbiente() : 1L;
			}
			liquidacion.setAmbiente(ambienteFacturador);
			ambiente = ambienteFacturador;
			System.out.println(">>> AMBIENTE: " + ambiente + (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)") + " | CONECTA_SRI: " + conectaSRI);

			try {
				String secuencial = obtenerSecuencial(liquidacion.getPtoEmision().getId(), tipoComprobante);
				liquidacion.setSecuencial(secuencial);
				String numero = liquidacion.getNumEstablecimiento() + "-" + liquidacion.getNumPtoEmision() + "-" + secuencial;
				liquidacion.setNumero(numero);
				String clave = generarClaveAcceso(liquidacion, tipoComprobante, ambienteFacturador, tipoEmision, secuencial);
				liquidacion.setClave(clave);
				liquidacion.setTipoComprobante(tipoComprobante);
				if (liquidacion.getEstadoEmision() == null) liquidacion.setEstadoEmision(1L);
				System.out.println("✓ Campos preparados en memoria. Clave: " + clave + " | Número: " + numero);
			} catch (Exception e) {
				resultado.put("etapa", "PREPARACION_CAMPOS");
				resultado.put("mensaje", "Error al preparar campos de la liquidación: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			String clave = liquidacion.getClave();
			Long idFacturador = liquidacion.getFacturador().getId();
			resultado.put("clave", clave);

			// ── PASO 2-3: Generar y firmar XML con datos en memoria ─────────────
			System.out.println("PASO 2: Generando XML en memoria...");
			String xmlFirmado;
			try {
				String dirEstablecimiento = "";
				try {
					String sqlEstab = "SELECT e.direccion FROM PuntoEmision pe JOIN pe.establecimiento e WHERE pe.id = :ptoEmisionId";
					dirEstablecimiento = (String) em.createQuery(sqlEstab)
							.setParameter("ptoEmisionId", liquidacion.getPtoEmision().getId())
							.getSingleResult();
				} catch (Exception e) {
					System.err.println("⚠ No se pudo obtener dirección del establecimiento: " + e.getMessage());
				}
				// El XSD declara dirEstablecimiento con minLength>=1: un comprobante
				// con este campo vacío el SRI lo devuelve. Mejor abortar la emisión
				// aquí, con un mensaje claro, que gastar clave de acceso y XML
				// firmado en un envío que sabemos que va a ser rechazado.
				if (dirEstablecimiento == null || dirEstablecimiento.trim().isEmpty()) {
					resultado.put("etapa", "VALIDACION");
					resultado.put("mensaje", "No se pudo obtener la dirección del establecimiento del punto de "
							+ "emisión (ID " + liquidacion.getPtoEmision().getId() + "). El SRI exige este campo "
							+ "y rechazaría el comprobante. Configure la dirección del establecimiento antes de emitir.");
					return resultado;
				}
				// Si vienen formas de pago explícitas, su suma debe cuadrar con el
				// total de la liquidación — si no, el <pagos> del XML queda
				// descuadrado contra <importeTotal> sin que nadie lo note hasta
				// que el SRI (o un tercero leyendo el comprobante) lo detecte.
				if (formasPago != null && !formasPago.isEmpty()) {
					double sumaPagos = 0.0;
					for (com.saa.model.cxc.FormaPagoLiquidacion fp : formasPago) {
						sumaPagos += fp.getValor() != null ? fp.getValor() : 0.0;
					}
					double totalLiquidacion = liquidacion.getTotal() != null ? liquidacion.getTotal() : 0.0;
					if (Math.abs(sumaPagos - totalLiquidacion) > 0.01) {
						resultado.put("etapa", "VALIDACION");
						resultado.put("mensaje", String.format(java.util.Locale.US,
								"La suma de las formas de pago (%.2f) no coincide con el total de la "
								+ "liquidación (%.2f).", sumaPagos, totalLiquidacion));
						return resultado;
					}
				}
				String xmlContent = generarXMLContentLiquidacion(liquidacion, dirEstablecimiento,
						detalles != null ? detalles : java.util.Collections.emptyList(),
						formasPago != null ? formasPago : java.util.Collections.emptyList(), ambiente);
				String pathRelativo = "resources/" + idFacturador + "/lqcs/g/" + clave + ".xml";
				String pathAbsoluto = getBaseUploadDirectory() + pathRelativo;
				Path path = Paths.get(pathAbsoluto);
				Files.createDirectories(path.getParent());
				Files.write(path, xmlContent.getBytes("UTF-8"));
				System.out.println("PASO 3: Firmando XML...");
				xmlFirmado = signatureService.firmarXMLFacturador(xmlContent, idFacturador);
				System.out.println("✓ XML generado y firmado.");
			} catch (Exception e) {
				resultado.put("etapa", "GENERACION_XML");
				resultado.put("mensaje", "Error al generar o firmar el XML de la liquidación: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// ── PASO 4a: Enviar al SRI (WS1 - Recepción) ───────────────────────
			System.out.println("PASO 4a: Enviando XML al SRI (WS1 - Recepción)...");
			String estadoRecepcion = "NO_ENVIADO";
			if (conectaSRI == 1) {
				String baseUploadDir = getBaseUploadDirectory();
				String resourcesPath = baseUploadDir + "resources/" + idFacturador;
				try {
					Path pathFirmado = Paths.get(resourcesPath + "/lqcs/f/" + clave + ".xml");
					Files.createDirectories(pathFirmado.getParent());
					Files.write(pathFirmado, xmlFirmado.getBytes("UTF-8"));
					String urlWS1 = ambiente == 1
							? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
							: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
					Path logWS1 = Paths.get(resourcesPath + "/lqcs/e/" + clave + ".txt");
					Files.createDirectories(logWS1.getParent());
					PrintWriter logWriter1 = new PrintWriter(new FileWriter(logWS1.toFile()));
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);
					estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					logWriter1.close();
					System.out.println(">>> Estado WS1 Recepción: [" + estadoRecepcion + "]");
				} catch (Exception e) {
					resultado.put("etapa", "WS1_RECEPCION");
					resultado.put("mensaje", "Error al comunicarse con el SRI (WS1): " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			} else {
				estadoRecepcion = "RECIBIDA";
				System.out.println("ℹ conectaSRI=0 — simulando RECIBIDA para guardar en BD.");
			}

			if (!"RECIBIDA".equals(estadoRecepcion)
					&& !(estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA"))) {
				resultado.put("etapa", "WS1_RECEPCION");
				resultado.put("exito", false);
				resultado.put("estado", estadoRecepcion);
				resultado.put("mensaje", "El SRI no aceptó el comprobante. Estado WS1: " + estadoRecepcion);
				return resultado;
			}

			// ── PASO 4b: WS1=RECIBIDA → Guardar en BD ───────────────────────────
			System.out.println("PASO 4b: SRI respondió RECIBIDA. Guardando liquidación en base de datos...");
			try {
				liquidacion = liquidacionCompraDaoService.save(liquidacion, null);
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_LIQUIDACION");
				resultado.put("mensaje", "Error al grabar la liquidación: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}
			resultado.put("liquidacion", liquidacion);
			resultado.put("idLiquidacion", liquidacion.getId());
			System.out.println("✓ Liquidación grabada ID: " + liquidacion.getId() + " | Clave: " + liquidacion.getClave());

			// Guardar detalles
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 4c: Guardando " + detalles.size() + " detalles...");
				try {
					for (com.saa.model.cxc.DetalleLiquidacionCompra detalle : detalles) {
						detalle.setLiquidacion(liquidacion);
						if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(Estado.ACTIVO));
						em.persist(detalle);
					}
					em.flush();
					System.out.println("✓ Detalles guardados.");
				} catch (Exception e) {
					resultado.put("etapa", "GRABADO_DETALLES");
					resultado.put("mensaje", "Error al grabar los detalles: " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			}

			// Guardar formas de pago (si no viene ninguna, se registra "01" —
			// Sin utilización del sistema financiero— para que quede trazable
			// aunque el XML ya haya usado el mismo default al construirse)
			try {
				java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPagoAGuardar =
						(formasPago != null && !formasPago.isEmpty()) ? formasPago
								: java.util.Arrays.asList(nuevaFormaPagoDefault(liquidacion.getTotal()));
				for (com.saa.model.cxc.FormaPagoLiquidacion fp : formasPagoAGuardar) {
					fp.setLiquidacion(liquidacion);
					em.persist(fp);
				}
				em.flush();
				System.out.println("✓ Formas de pago guardadas: " + formasPagoAGuardar.size());
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_FORMA_PAGO");
				resultado.put("mensaje", "Error al grabar la forma de pago: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// Registrar paths y estado FIRMADA/ENVIADA en BD
			try {
				String baseUploadDir = getBaseUploadDirectory();
				String resourcesPath = baseUploadDir + "resources/" + idFacturador;
				PathLiquidacionCompra pathF = new PathLiquidacionCompra();
				pathF.setLiquidacion(liquidacion);
				pathF.setPath("resources/" + idFacturador + "/lqcs/f/" + clave + ".xml");
				pathF.setAlterno(3L);
				pathLiquidacionCompraDaoService.save(pathF, null);
				liquidacion.setEstado(3L);
				liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
				if (conectaSRI == 1) {
					Path pathEnviado = Paths.get(resourcesPath + "/lqcs/e/" + clave + ".xml");
					byte[] bytesXMLFirmado = Files.readAllBytes(Paths.get(resourcesPath + "/lqcs/f/" + clave + ".xml"));
					Files.write(pathEnviado, bytesXMLFirmado);
					PathLiquidacionCompra pathE = new PathLiquidacionCompra();
					pathE.setLiquidacion(liquidacion);
					pathE.setPath("resources/" + idFacturador + "/lqcs/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathLiquidacionCompraDaoService.save(pathE, null);
					liquidacion.setEstado(4L);
					liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
				}
			} catch (Exception e) {
				System.err.println("⚠ Error registrando paths (no crítico): " + e.getMessage());
			}

			// ── PASO 4c: Si era CLAVE ACCESO REGISTRADA ─────────────────────────
			if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
				liquidacion.setAutorizacion(clave);
				liquidacion.setFechaAutorizacion(liquidacion.getFecha().plusMinutes(1).plusSeconds(15));
				liquidacion.setEstado(5L);
				liquidacion.setEstadoEmision(1L);
				liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
				try { em.flush(); } catch (Exception flushEx) { /* no crítico */ }
				pdfBytesParaEmail = generarPDFLiquidacion(liquidacion, idFacturador, clave, pathLogo, ambiente);
				resultado.put("estado", "AUTORIZADO"); resultado.put("autorizacion", "AUTORIZADO");
				resultado.put("etapa", "COMPLETADO"); resultado.put("mensaje", "Liquidación ya registrada en el SRI. Autorizada.");
				resultado.put("emitida",      true);
				resultado.put("idFacturador", idFacturador);
				resultado.put("destinatario", destinatario);
				resultado.put("pdfBytes",     pdfBytesParaEmail);
				return resultado;
			}

			// ── PASO 4d: WS2 - Autorización ─────────────────────────────────────
			System.out.println("PASO 4d: Consultando autorización al SRI (WS2)...");
			Thread.sleep(2000);
			String resultadoAutorizacion = "";
			boolean autorizada = false;
			if (conectaSRI == 1) {
				try {
					String baseUploadDir = getBaseUploadDirectory();
					String resourcesPath = baseUploadDir + "resources/" + idFacturador;
					String urlWS2 = ambiente == 1
							? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
							: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
					ResultadoAutorizacion ra = llamarAutorizacionSRI(urlWS2, clave);
					System.out.println(">>> Estado WS2: [" + ra.estado + "]");

					if ("AUTORIZADO".equals(ra.estado)) {
						Path logWS2A = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".txt");
						Files.createDirectories(logWS2A.getParent());
						PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
						logWriter2.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2.close();
						Path pathAutorizado = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".xml");
						Files.write(pathAutorizado, ra.comprobanteXML.getBytes("UTF-8"));
						PathLiquidacionCompra pathA = new PathLiquidacionCompra();
						pathA.setLiquidacion(liquidacion);
						pathA.setPath("resources/" + idFacturador + "/lqcs/a/" + clave + ".xml");
						pathA.setAlterno(5L);
						pathLiquidacionCompraDaoService.save(pathA, null);
						liquidacion.setEstado(5L);
						liquidacion.setEstadoEmision(1L);
						liquidacion.setAutorizacion(ra.numeroAutorizacion);
						liquidacion.setFechaAutorizacion(parseFechaAutorizacion(ra.fechaAutorizacion));
						liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
						resultadoAutorizacion = ra.estado;
						autorizada = true;
						// Generar PDF: flush primero para que reporteService (REQUIRES_NEW)
						// vea los datos ya guardados en esta transacción.
						try { em.flush(); } catch (Exception flushEx) {
							System.err.println("⚠ flush antes de PDF: " + flushEx.getMessage());
						}
						try {
							pdfBytesParaEmail = generarPDFLiquidacion(liquidacion, idFacturador, clave, pathLogo, ambiente);
							if (pdfBytesParaEmail != null && pdfBytesParaEmail.length > 0) {
								Path pathPdf = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".pdf");
								Files.write(pathPdf, pdfBytesParaEmail);
								PathLiquidacionCompra pathPdfRec = new PathLiquidacionCompra();
								pathPdfRec.setLiquidacion(liquidacion);
								pathPdfRec.setPath("resources/" + idFacturador + "/lqcs/a/" + clave + ".pdf");
								pathPdfRec.setAlterno(7L);
								pathLiquidacionCompraDaoService.save(pathPdfRec, null);
								System.out.println("✓ PDF RIDE generado (" + pdfBytesParaEmail.length + " bytes).");
							} else {
								System.err.println("⚠ generarPDFLiquidacion retornó null o vacío. "
										+ "El email se enviará sin PDF (¿falta compilar el .jasper?).");
							}
						} catch (Exception pdfEx) {
							System.err.println("⚠ Error generando PDF RIDE (no crítico): " + pdfEx.getMessage());
						}
						if (ambiente == 2) {
							em.createQuery("UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos,0)+1 WHERE f.id = :id")
								.setParameter("id", idFacturador).executeUpdate();
						}
					} else {
						Path logWS2N = Paths.get(resourcesPath + "/lqcs/n/" + clave + ".txt");
						Files.createDirectories(logWS2N.getParent());
						PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
						logWriter2N.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2N.close();
						if (ra.comprobanteXML != null) {
							Files.write(Paths.get(resourcesPath + "/lqcs/n/" + clave + ".xml"), ra.comprobanteXML.getBytes("UTF-8"));
							PathLiquidacionCompra pathN = new PathLiquidacionCompra();
							pathN.setLiquidacion(liquidacion);
							pathN.setPath("resources/" + idFacturador + "/lqcs/n/" + clave + ".xml");
							pathN.setAlterno(6L);
							pathLiquidacionCompraDaoService.save(pathN, null);
						}
						liquidacion.setEstado(6L);
						liquidacion.setEstadoEmision(2L);
						liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
						resultadoAutorizacion = "Estado: " + ra.estado
								+ " Id: " + nvl(ra.mensajeId, "")
								+ " Mensaje: " + nvl(ra.mensaje, "")
								+ " / " + nvl(ra.informacionAdicional, "");
					}
				} catch (Exception e) {
					resultado.put("etapa", "WS2_AUTORIZACION");
					resultado.put("mensaje", "Error al consultar autorización al SRI (WS2): " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			} else {
				autorizada = true;
				resultadoAutorizacion = "AUTORIZADO";
				liquidacion.setEstado(5L);
				liquidacion.setEstadoEmision(1L);
				liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
				// Generar PDF también en modo simulación (conectaSRI=0)
				try {
					em.flush();
					pdfBytesParaEmail = generarPDFLiquidacion(liquidacion, idFacturador, clave, pathLogo, ambiente);
					if (pdfBytesParaEmail != null && pdfBytesParaEmail.length > 0) {
						String baseUploadDir = getBaseUploadDirectory();
						String resourcesPath = baseUploadDir + "resources/" + idFacturador;
						Path pathPdf = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".pdf");
						Files.createDirectories(pathPdf.getParent());
						Files.write(pathPdf, pdfBytesParaEmail);
						PathLiquidacionCompra pathPdfRec = new PathLiquidacionCompra();
						pathPdfRec.setLiquidacion(liquidacion);
						pathPdfRec.setPath("resources/" + idFacturador + "/lqcs/a/" + clave + ".pdf");
						pathPdfRec.setAlterno(7L);
						pathLiquidacionCompraDaoService.save(pathPdfRec, null);
						System.out.println("✓ PDF RIDE generado (modo simulación) (" + pdfBytesParaEmail.length + " bytes).");
					}
				} catch (Exception pdfEx) {
					System.err.println("⚠ Error generando PDF en modo simulación (no crítico): " + pdfEx.getMessage());
				}
			}

			resultado.put("autorizacion", resultadoAutorizacion);

			if (!autorizada) {
				resultado.put("exito", false);
				resultado.put("estado", "NO_AUTORIZADO");
				resultado.put("mensaje", "La liquidación fue recibida por el SRI pero no fue autorizada. "
						+ "Respuesta del SRI: " + resultadoAutorizacion);
				return resultado;
			}

			// Emisión terminada: la liquidación está autorizada y confirmada en
			// BD. El documento CXP / asiento contable y el email los ejecuta el
			// orquestador fuera de esta transacción.
			resultado.put("emitida",      true);
			resultado.put("estado",       "AUTORIZADO");
			resultado.put("idFacturador", idFacturador);
			resultado.put("destinatario", destinatario);
			resultado.put("pdfBytes",     pdfBytesParaEmail);

		} catch (Exception e) {
			System.err.println("ERROR en emitirLiquidacionAnteSRI: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("error", e.getMessage());
			resultado.put("mensaje", "Error al procesar liquidación: " + e.getMessage());
			sessionContext.setRollbackOnly();
			throw e;
		}

		return resultado;
	}

	/**
	 * Crea el documento CXP (PGS.LQCC) a partir de una liquidación (CXC) ya
	 * autorizada por el SRI, copia sus detalles (con producto, ya clasificado
	 * por {@code validarCuentasContablesLiquidacion}) y el path del XML
	 * autorizado, y lo contabiliza como liquidación de compra recibida.
	 * Transacción propia (REQUIRES_NEW). Idempotente.
	 * @param idLiquidacion : Id de la liquidación (CXC) ya autorizada
	 * @return : Mapa con aplica, generado, yaExistia, idDocumentoCxp, idAsiento, numeroAlterno
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> crearDocumentoCxp(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo crearDocumentoCxp con id: " + idLiquidacion);

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("generado", false);
		resultado.put("aplica", false);

		LiquidacionCompra liquidacion = em.find(LiquidacionCompra.class, idLiquidacion);
		if (liquidacion == null) {
			throw new IncomeException("Liquidación de Compra con ID " + idLiquidacion + " no encontrada.");
		}
		if (liquidacion.getFacturador() == null || liquidacion.getFacturador().getEmpresa() == null) {
			System.out.println("ℹ El facturador no tiene empresa contable: se omite el documento CXP.");
			return resultado;
		}
		resultado.put("aplica", true);

		if (liquidacion.getDocumentoCxp() != null) {
			com.saa.model.cxp.LiquidacionCompraCompra lqccExistente = liquidacion.getDocumentoCxp();
			resultado.put("yaExistia", true);
			resultado.put("idDocumentoCxp", lqccExistente.getId());
			if (lqccExistente.getAsiento() != null) {
				resultado.put("idAsiento", lqccExistente.getAsiento().getCodigo());
				resultado.put("numeroAlterno", lqccExistente.getAsiento().getNumeroAlterno());
			}
			System.out.println("ℹ La liquidación ya tiene documento CXP: " + lqccExistente.getId());
			return resultado;
		}

		// Etapa atómica: se marca el rollback a mano porque IncomeException es
		// una application exception y por sí sola no reversaría esta transacción.
		try {
			Long idEmpresaConta = liquidacion.getFacturador().getEmpresa().getCodigo();

			@SuppressWarnings("unchecked")
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles = em.createQuery(
					"SELECT d FROM DetalleLiquidacionCompra d WHERE d.liquidacion.id = :id AND d.estado = 1")
					.setParameter("id", idLiquidacion).getResultList();
			if (detalles.isEmpty()) {
				throw new IncomeException("La liquidación " + idLiquidacion + " no tiene detalles activos: "
						+ "no se puede crear el documento CXP.");
			}

			com.saa.model.cxp.LiquidacionCompraCompra lqcc = new com.saa.model.cxp.LiquidacionCompraCompra();
			lqcc.setEmpresa(em.find(com.saa.model.scp.Empresa.class, idEmpresaConta));
			lqcc.setTipoComprobante(liquidacion.getTipoComprobante());
			lqcc.setTitular(liquidacion.getTitular());
			lqcc.setNumero(liquidacion.getNumero());
			lqcc.setNumEstablecimiento(liquidacion.getNumEstablecimiento());
			lqcc.setNumPtoEmision(liquidacion.getNumPtoEmision());
			lqcc.setSecuencial(liquidacion.getSecuencial());
			lqcc.setAmbiente(liquidacion.getAmbiente());
			lqcc.setClave(liquidacion.getClave());
			lqcc.setFecha(liquidacion.getFecha());
			lqcc.setObservacion(liquidacion.getObservacion());
			lqcc.setSubtotal(liquidacion.getSubtotal());
			lqcc.setSubcero(liquidacion.getSubcero());
			lqcc.setpIVA(liquidacion.getpIVA());
			lqcc.setvIVA(liquidacion.getvIVA());
			lqcc.setvICE(liquidacion.getvICE());
			lqcc.setvIRBPNR(liquidacion.getvIRBPNR());
			lqcc.setDescuento(liquidacion.getDescuento());
			lqcc.setPorDescuento(liquidacion.getPorDescuento());
			lqcc.setPropina(liquidacion.getPropina());
			lqcc.setSubsidio(liquidacion.getSubsidio());
			lqcc.setTotalSinSub(liquidacion.getTotalSinSub());
			lqcc.setAhorroSub(liquidacion.getAhorroSub());
			lqcc.setTotal(liquidacion.getTotal());
			lqcc.setPtoEmision(liquidacion.getPtoEmision() != null ? liquidacion.getPtoEmision().getId() : null);
			lqcc.setUsuario(liquidacion.getUsuario());
			lqcc.setPathGen(liquidacion.getPathGen());
			lqcc.setAutorizacion(liquidacion.getAutorizacion());
			lqcc.setFechaAutorizacion(liquidacion.getFechaAutorizacion());
			lqcc.setEstado(Long.valueOf(Estado.ACTIVO));
			lqcc.setEstadoEmision(1L); // 1 = autorizado (mismo esquema que LQCS)
			em.persist(lqcc);
			em.flush();

			for (com.saa.model.cxc.DetalleLiquidacionCompra d : detalles) {
				com.saa.model.cxp.DetalleLiquidacionCompraCompra dc = new com.saa.model.cxp.DetalleLiquidacionCompraCompra();
				dc.setLiquidacion(lqcc);
				dc.setDescripcion(d.getDescripcion());
				dc.setCantidad(d.getCantidad());
				dc.setValor(d.getValor());
				dc.setSubTotal(d.getSubTotal());
				dc.setPorcentajeIVA(d.getPorcentajeIVA());
				dc.setValorIVA(d.getValorIVA());
				dc.setPorcentajeICE(d.getPorcentajeICE());
				dc.setValorICE(d.getValorICE());
				dc.setSubsidio(d.getSubsidio());
				dc.setPrecioSinSub(d.getPrecioSinSub());
				dc.setDescuento(d.getDescuento());
				dc.setTotal(d.getTotal());
				dc.setProducto(d.getProducto());
				dc.setEstado(Long.valueOf(Estado.ACTIVO));
				em.persist(dc);
			}

			// Formas de pago: la pantalla CxP → Consulta de documentos las lee
			// (consulta-documentos.component.ts) directo del documento CXP, no
			// de la liquidación de CXC — sin esta copia el documento se vería
			// sin ninguna forma de pago.
			@SuppressWarnings("unchecked")
			java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago = em.createQuery(
					"SELECT fp FROM FormaPagoLiquidacion fp WHERE fp.liquidacion.id = :id")
					.setParameter("id", idLiquidacion).getResultList();
			for (com.saa.model.cxc.FormaPagoLiquidacion fp : formasPago) {
				com.saa.model.cxp.FormaPagoLiquidacionCompraCompra fpc = new com.saa.model.cxp.FormaPagoLiquidacionCompraCompra();
				fpc.setLiquidacion(lqcc);
				fpc.setFormaPago(fp.getFormaPago());
				fpc.setValor(fp.getValor());
				fpc.setPlazo(fp.getPlazo());
				fpc.setUnidadTiempo(fp.getUnidadTiempo());
				em.persist(fpc);
			}

			// Path del XML autorizado: mismo archivo físico que ya quedó
			// grabado por la emisión (resources/{facturador}/lqcs/a/{clave}.xml),
			// registrado ahora también del lado CXP (alterno=1, igual que
			// ProcesoCargaDocumentosServiceImpl.registrarLiquidacionCompraCompra).
			com.saa.model.cxp.PathLiquidacionCompraCompra pathCxp = new com.saa.model.cxp.PathLiquidacionCompraCompra();
			pathCxp.setLiquidacion(lqcc);
			pathCxp.setPath("resources/" + liquidacion.getFacturador().getId() + "/lqcs/a/" + liquidacion.getClave() + ".xml");
			pathCxp.setAlterno(1L);
			em.persist(pathCxp);
			em.flush();

			resultado.put("idDocumentoCxp", lqcc.getId());

			// codSustento (ATS, Tabla 5) extendido a LQCC (2026-08-28): todas las lineas ya
			// estan persistidas con su producto (copiado de CXC arriba), asi que este es el
			// primer momento en que la excepcion por grupo tiene sentido. No bloquea la
			// creacion del documento CXP si falla -mismo criterio que FacturaCompra-.
			try {
				sustentoTributarioService.resolverSiFaltaLiquidacion(lqcc);
			} catch (Throwable e) {
				System.out.println("ATENCION: fallo la resolucion de codSustento de la liquidacion "
						+ lqcc.getId() + ": " + e.getMessage());
			}

			if (Long.valueOf(1L).equals(liquidacion.getFacturador().getGeneraConta())) {
				java.time.LocalDate fechaAsiento = liquidacion.getFecha() != null
						? liquidacion.getFecha().toLocalDate() : java.time.LocalDate.now();
				String obsAsiento = "Liquidación de Compra N° " + nvl(liquidacion.getNumero(), liquidacion.getClave())
						+ " | Proveedor: " + (liquidacion.getTitular() != null ? liquidacion.getTitular().getNombre() : "")
						+ " | Aut: " + nvl(liquidacion.getAutorizacion(), liquidacion.getClave());
				String usuarioAsiento = liquidacion.getUsuario() != null
						? liquidacion.getUsuario().getNombre() : "SISTEMA";

				com.saa.model.cnt.Asiento asientoGenerado =
						asientoContableService.generarAsientoLiquidacionCompraCompra(
								lqcc.getId(), idEmpresaConta,
								com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS,
								fechaAsiento, obsAsiento, usuarioAsiento);

				com.saa.model.cnt.Asiento asientoAttached =
						em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
				if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
				lqcc.setAsiento(asientoAttached);
				em.merge(lqcc);

				resultado.put("generado", true);
				resultado.put("idAsiento", asientoAttached.getCodigo());
				resultado.put("numeroAlterno", asientoAttached.getNumeroAlterno());
				System.out.println("✓ Asiento contable generado: " + asientoAttached.getNumeroAlterno());
			} else {
				System.out.println("ℹ El facturador no genera contabilidad: documento CXP creado sin asiento.");
			}

			// Enlazar LQCS → LQCC (CBR.LQCS.LQCSLQCC)
			liquidacion.setDocumentoCxp(lqcc);
			liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
			em.flush();

			System.out.println("✓ Documento CXP creado: LQCC id=" + lqcc.getId()
					+ " para LQCS id=" + idLiquidacion);
		} catch (Throwable e) {
			sessionContext.setRollbackOnly();
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
					"JOIN l.titular p " +
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
			List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles = queryDetalle.getResultList();

			// 4. Obtener formas de pago
			String sqlFormasPago = "SELECT fp FROM FormaPagoLiquidacion fp WHERE fp.liquidacion.id = :liquidacionId";
			Query queryFormasPago = em.createQuery(sqlFormasPago);
			queryFormasPago.setParameter("liquidacionId", liquidacion.getId());
			@SuppressWarnings("unchecked")
			List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago = queryFormasPago.getResultList();
			
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
			List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago, Long ambiente) throws Exception {
		
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
		writeElement(writer, "tipoIdentificacionProveedor", resolverTipoIdentificacionSRI(liquidacion.getTitular()), 4);
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
	
	private void writePagos(XMLStreamWriter writer, List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago,
			Double total) throws Exception {
		writer.writeCharacters("    ");
		writer.writeStartElement("pagos");
		writer.writeCharacters("\n");

		if (formasPago == null || formasPago.isEmpty()) {
			writer.writeCharacters("      ");
			writer.writeStartElement("pago");
			writer.writeCharacters("\n");
			writeElement(writer, "formaPago", "01", 8);
			writeElement(writer, "total", formatDecimal(total), 8);
			writer.writeCharacters("      ");
			writer.writeEndElement();
			writer.writeCharacters("\n");
		} else {
			for (com.saa.model.cxc.FormaPagoLiquidacion fp : formasPago) {
				// El SRI exige siempre 2 dígitos en formaPago
				String codFP = fp.getFormaPago();
				if (codFP != null && codFP.length() == 1) codFP = "0" + codFP;
				// El XSD admite plazo=0 (minInclusive="0"): no forzar a "1" cuando
				// el pago viene explícitamente al contado.
				String plazoStr = fp.getPlazo() != null ? String.valueOf(fp.getPlazo()) : "0";
				writer.writeCharacters("      ");
				writer.writeStartElement("pago");
				writer.writeCharacters("\n");
				writeElement(writer, "formaPago", nvl(codFP, "01"), 8);
				writeElement(writer, "total", formatDecimal(fp.getValor()), 8);
				writeElement(writer, "plazo", plazoStr, 8);
				writeElement(writer, "unidadTiempo", nvl(fp.getUnidadTiempo(), "dias"), 8);
				writer.writeCharacters("      ");
				writer.writeEndElement();
				writer.writeCharacters("\n");
			}
		}

		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	/**
	 * Código de IVA del SRI (impuesto) usado en el detalle. La liquidación
	 * no guarda el código directamente (a diferencia de Factura), sólo el
	 * porcentaje (12, 15, 5, 8, 0…): se mapea con la MISMA tabla que usa
	 * {@code AsientoContableServiceImpl.mapPorcentajeIVAaCodigo} — a propósito
	 * duplicada (métodos privados de otra clase no son reutilizables) — para
	 * que el código emitido en el XML sea el mismo que usaría la
	 * contabilización si tuviera que resolverlo desde este código en vez del
	 * porcentaje crudo.
	 */
	private String codigoPorcentajeIVA(Long porcentajeIVA) {
		if (porcentajeIVA == null) return "0";
		switch (porcentajeIVA.intValue()) {
			case 0:  return "0";
			case 5:  return "5";
			case 8:  return "8";
			case 12: return "2";
			case 14: return "3";
			case 15: return "4";
			default: return String.valueOf(porcentajeIVA);
		}
	}

	private void writeDetalles(XMLStreamWriter writer, List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("detalles");
		writer.writeCharacters("\n");
		if (detalles != null) {
			for (com.saa.model.cxc.DetalleLiquidacionCompra detalle : detalles) {
				writeDetalleLiquidacion(writer, detalle);
			}
		}
		writer.writeCharacters("  ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}

	private void writeDetalleLiquidacion(XMLStreamWriter writer, com.saa.model.cxc.DetalleLiquidacionCompra detalle) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("detalle");
		writer.writeCharacters("\n");

		if (detalle.getProducto() != null && detalle.getProducto().getCodigo() != null
				&& !detalle.getProducto().getCodigo().trim().isEmpty()) {
			writeElement(writer, "codigoPrincipal", detalle.getProducto().getCodigo(), 3);
		}
		if (detalle.getProducto() != null && detalle.getProducto().getCodigoAux() != null
				&& !detalle.getProducto().getCodigoAux().trim().isEmpty()) {
			writeElement(writer, "codigoAuxiliar", detalle.getProducto().getCodigoAux(), 3);
		}

		writeElement(writer, "descripcion", nvl(detalle.getDescripcion(), ""), 3);
		writeElement(writer, "cantidad", formatDecimal(detalle.getCantidad()), 3);
		writeElement(writer, "precioUnitario", formatDecimal(detalle.getValor()), 3);
		if (detalle.getPrecioSinSub() != null && detalle.getPrecioSinSub() > 0) {
			writeElement(writer, "precioSinSubsidio", formatDecimal(detalle.getPrecioSinSub()), 3);
		}
		writeElement(writer, "descuento", formatDecimal(nvl(detalle.getDescuento(), 0.0)), 3);
		writeElement(writer, "precioTotalSinImpuesto", formatDecimal(detalle.getSubTotal()), 3);

		writer.writeCharacters("   ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");
		writer.writeCharacters("    ");
		writer.writeStartElement("impuesto");
		writer.writeCharacters("\n");
		String codPorcentaje = codigoPorcentajeIVA(detalle.getPorcentajeIVA());
		writeElement(writer, "codigo", "2", 5);
		writeElement(writer, "codigoPorcentaje", codPorcentaje, 5);
		writeElement(writer, "tarifa", formatDecimal(detalle.getPorcentajeIVA() != null
				? detalle.getPorcentajeIVA().doubleValue() : 0.0), 5);
		writeElement(writer, "baseImponible", formatDecimal(detalle.getSubTotal()), 5);
		writeElement(writer, "valor", formatDecimal(nvl(detalle.getValorIVA(), 0.0)), 5);
		writer.writeCharacters("    ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		writer.writeCharacters("   ");
		writer.writeEndElement(); // impuestos
		writer.writeCharacters("\n");

		writer.writeCharacters("  ");
		writer.writeEndElement(); // detalle
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
		// Locale.US explícito: con la JVM en es_EC "%.2f" imprime "15,00" con
		// coma decimal, y el SRI rechaza el comprobante. Mismo criterio que
		// FacturaServiceImpl.formatDecimal.
		return String.format(java.util.Locale.US, "%.2f", value);
	}

	/**
	 * Forma de pago por defecto cuando el frontend no envía ninguna: "01" —
	 * Sin utilización del sistema financiero, por el total de la liquidación,
	 * plazo 0 días. Se guarda igual que la que ya usa el XML por defecto
	 * (writePagos), para que quede trazada en BD.
	 */
	private com.saa.model.cxc.FormaPagoLiquidacion nuevaFormaPagoDefault(Double total) {
		com.saa.model.cxc.FormaPagoLiquidacion fp = new com.saa.model.cxc.FormaPagoLiquidacion();
		fp.setFormaPago("01");
		fp.setValor(total);
		fp.setPlazo(0L);
		fp.setUnidadTiempo("dias");
		return fp;
	}

	/**
	 * Genera el PDF RIDE de la liquidación con JasperReports. A diferencia de
	 * {@code FacturaServiceImpl.generarPDFFactura}, el reporte
	 * {@code RPRT_RIDE_LIQUIDACION.jrxml} trae su propia consulta SQL
	 * (parametrizada sólo por {@code P_ID_LIQUIDACION}): no hace falta
	 * replicar aquí los datos, sólo pasar el id.
	 * <p>
	 * El {@code .jasper} de este reporte no está compilado (no hay
	 * compilación en tiempo de ejecución en JasperReports 7.0.3 — ver
	 * CLAUDE.md): si no existe, {@code reporteService.generarReporte} falla y
	 * este método captura el error, devuelve {@code null} y deja la
	 * liquidación sin RIDE, SIN abortar la emisión.
	 * @return : bytes del PDF, o null si no se pudo generar (no crítico)
	 */
	private byte[] generarPDFLiquidacion(LiquidacionCompra liquidacionObj, Long idFacturador, String clave,
			String pathLogoParam, Long ambiente) {
		try {
			System.out.println("Generando PDF RIDE para liquidación de compra: " + clave);
			java.util.Map<String, Object> p = new java.util.HashMap<>();
			p.put("P_ID_LIQUIDACION", liquidacionObj.getId());
			byte[] pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_LIQUIDACION", p, "PDF");
			System.out.println("✓ PDF RIDE generado correctamente ("
					+ (pdfBytes != null ? pdfBytes.length : 0) + " bytes)");
			return pdfBytes;
		} catch (Exception e) {
			System.err.println("⚠ Error generando PDF RIDE de liquidación (no crítico — ¿falta compilar "
					+ "RPRT_RIDE_LIQUIDACION.jasper con Jaspersoft Studio 7.0.3?): " + e.getMessage());
			return null;
		}
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
			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:rec=\"http://ec.gob.sri.ws.recepcion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<rec:validarComprobante><xml>" + xmlBase64 + "</xml></rec:validarComprobante>" +
				"</soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			log.println("Respuesta WS1: " + respuestaCompleta);

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
				if (mensajeList.getLength() == 0) mensajeList = docEl.getElementsByTagName("mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				return estado;
			}
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
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<aut:autorizacionComprobante><claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>" +
				"</aut:autorizacionComprobante></soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			System.out.println(">>> XML RESPUESTA WS2 (Autorización SRI - LC):\n" + respuestaCompleta);

			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) resultado.estado = estadoList.item(0).getTextContent();

			NodeList numAutList = docEl.getElementsByTagNameNS("*", "numeroAutorizacion");
			if (numAutList.getLength() > 0) resultado.numeroAutorizacion = numAutList.item(0).getTextContent();

			NodeList fechaAutList = docEl.getElementsByTagNameNS("*", "fechaAutorizacion");
			if (fechaAutList.getLength() > 0) resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();

			NodeList comprobanteList = docEl.getElementsByTagNameNS("*", "comprobante");
			if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();

			NodeList mensajeIdList = docEl.getElementsByTagNameNS("*", "identificador");
			if (mensajeIdList.getLength() > 0) resultado.mensajeId = mensajeIdList.item(0).getTextContent();

			NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
			if (mensajeList.getLength() > 0) resultado.mensaje = mensajeList.item(0).getTextContent();

			NodeList infoAdicionalList = docEl.getElementsByTagNameNS("*", "informacionAdicional");
			if (infoAdicionalList.getLength() > 0) resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();

			return resultado;
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarAutorizacionSRI LC: " + e.getMessage());
			e.printStackTrace();
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
	
	/**
	 * Traduce el tipo de identificación <b>interno</b> del titular al código que exige el SRI.
	 *
	 * <p><b>Por qué existe este método.</b> El XSD del SRI declara {@code tipoIdentificacionProveedor}
	 * con el patrón <code>[0][4-8]</code> — o sea {@code 04} RUC, {@code 05} cédula, {@code 06}
	 * pasaporte, {@code 07} consumidor final, {@code 08} identificación del exterior. El rubro
	 * interno usa <b>otros valores y otro orden</b>. Hasta el 2026-08-31 acá se mandaba
	 * {@code String.valueOf(titular.getRubroTipoIdentificacionH())}, o sea el código interno crudo,
	 * y el SRI devolvía el comprobante con:
	 *
	 * <pre>
	 * identificador 35 — ARCHIVO NO CUMPLE ESTRUCTURA XML
	 * Value '1' is not facet-valid with respect to pattern '[0][4-8]'
	 * </pre>
	 *
	 * <p>Es la misma trampa que ya se había corregido en el generador del ATS y que no se buscó en
	 * los generadores de comprobantes. El patrón de referencia es
	 * {@code FacturaServiceImpl:1073-1092}, que sí resuelve contra el catálogo — y hay que pasarle
	 * los <b>dos</b> rubros, el padre y el hijo, no sólo el hijo.
	 *
	 * <p><b>Por qué aborta en vez de asumir un valor.</b> El generador de facturas cae a {@code "05"}
	 * cuando el catálogo no resuelve. Acá no se copia esa decisión: emitir un comprobante fiscal con
	 * un tipo de identificación adivinado es <b>peor</b> que no emitirlo, porque el SRI lo acepta y
	 * queda mal declarado sin ningún error visible. Mismo criterio que ya se aplicó a
	 * {@code dirEstablecimiento} vacío en {@code emitirLiquidacionAnteSRI}.
	 *
	 * @param titular el proveedor de la liquidación
	 * @return el código de dos dígitos del SRI, garantizado dentro de {@code 04..08}
	 * @throws IncomeException si el titular no tiene tipo de identificación, si el catálogo no lo
	 *         resuelve, o si el valor resuelto cae fuera del rango que acepta el SRI
	 */
	private String resolverTipoIdentificacionSRI(com.saa.model.tsr.Titular titular) throws Exception {
		if (titular == null) {
			throw new IncomeException("La liquidación no tiene proveedor asignado.");
		}
		String nombre = nvl(titular.getNombre(), "(sin nombre)");
		if (titular.getRubroTipoIdentificacionP() == null || titular.getRubroTipoIdentificacionH() == null) {
			throw new IncomeException("El proveedor " + nombre + " no tiene configurado el tipo de "
					+ "identificación. El SRI lo exige para emitir la liquidación de compra.");
		}

		String valorAlfa;
		try {
			valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
					titular.getRubroTipoIdentificacionP().intValue(),
					titular.getRubroTipoIdentificacionH().intValue());
		} catch (Throwable e) {
			throw new IncomeException("No se pudo resolver el tipo de identificación del proveedor "
					+ nombre + " contra el catálogo de rubros: " + e.getMessage());
		}

		if (valorAlfa == null || valorAlfa.trim().isEmpty()) {
			throw new IncomeException("El tipo de identificación del proveedor " + nombre
					+ " (rubro " + titular.getRubroTipoIdentificacionP() + "/"
					+ titular.getRubroTipoIdentificacionH() + ") no tiene equivalencia en el catálogo "
					+ "para el SRI. Configurar el detalle de rubro antes de emitir.");
		}

		// El SRI exige siempre dos dígitos: un catálogo que devuelva "4" se normaliza a "04".
		String codigo = valorAlfa.trim();
		if (codigo.length() == 1) codigo = "0" + codigo;

		// Se valida acá y no en el SRI: el rechazo del WS1 no dice qué titular lo causó, y este
		// mensaje sí. El patrón es el mismo que declara el XSD.
		if (!codigo.matches("[0][4-8]")) {
			throw new IncomeException("El tipo de identificación del proveedor " + nombre
					+ " resolvió a '" + codigo + "', que el SRI no acepta para una liquidación de "
					+ "compra (sólo admite 04 RUC, 05 cédula, 06 pasaporte, 07 consumidor final, "
					+ "08 exterior). Revisar el catálogo de rubros.");
		}
		return codigo;
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

	// =========================================================================
	// marcarLiquidacionAutorizada
	// =========================================================================

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean marcarLiquidacionAutorizada(Long idLiquidacion, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable {
		System.out.println("Ingresa al metodo marcarLiquidacionAutorizada con id: " + idLiquidacion);

		LiquidacionCompra liquidacion = em.find(LiquidacionCompra.class, idLiquidacion);
		if (liquidacion == null) {
			throw new IncomeException("Liquidación de Compra con ID " + idLiquidacion + " no encontrada.");
		}
		if (Long.valueOf(5L).equals(liquidacion.getEstado())) {
			System.out.println("ℹ Liquidación ya estaba en estado 5 (autorizada). Solo se verificó la autorización.");
			return false;
		}

		liquidacion.setEstado(5L);
		liquidacion.setEstadoEmision(1L);
		if (numeroAutorizacion != null && !numeroAutorizacion.isEmpty()) {
			liquidacion.setAutorizacion(numeroAutorizacion);
		}
		if (fechaAutorizacion != null && !fechaAutorizacion.isEmpty()) {
			liquidacion.setFechaAutorizacion(parseFechaAutorizacion(fechaAutorizacion));
		}

		Long idFacturador = liquidacion.getFacturador() != null ? liquidacion.getFacturador().getId() : null;
		if (comprobanteXML != null && !comprobanteXML.isEmpty() && idFacturador != null) {
			try {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				Path pathAutorizado = Paths.get(resourcesPath + "/lqcs/a/" + liquidacion.getClave() + ".xml");
				Files.createDirectories(pathAutorizado.getParent());
				Files.write(pathAutorizado, comprobanteXML.getBytes("UTF-8"));
				PathLiquidacionCompra pathA = new PathLiquidacionCompra();
				pathA.setLiquidacion(liquidacion);
				pathA.setPath("resources/" + idFacturador + "/lqcs/a/" + liquidacion.getClave() + ".xml");
				pathA.setAlterno(5L);
				pathLiquidacionCompraDaoService.save(pathA, null);
				System.out.println("✓ XML autorizado guardado en disco.");
			} catch (Exception xmlEx) {
				System.err.println("⚠ Error guardando XML autorizado (no crítico): " + xmlEx.getMessage());
			}
		}

		liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
		em.flush();
		System.out.println("✓ Liquidación actualizada a estado AUTORIZADA (5). Aut: " + numeroAutorizacion);
		return true;
	}

	// =========================================================================
	// reintentarAutorizacionLiquidacion
	// =========================================================================

	@Override
	public java.util.Map<String, Object> reintentarAutorizacionLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("=== reintentarAutorizacionLiquidacion | idLiquidacion=" + idLiquidacion + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		LiquidacionCompra liquidacion =
				liquidacionCompraDaoService.selectById(idLiquidacion, NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (liquidacion == null) {
			resultado.put("mensaje", "No se encontró la liquidación con ID: " + idLiquidacion);
			return resultado;
		}

		String clave = liquidacion.getClave();
		if (clave == null || clave.trim().isEmpty()) {
			resultado.put("mensaje", "La liquidación no tiene clave de acceso. No se puede reintentar la autorización.");
			return resultado;
		}

		if (Long.valueOf(5L).equals(liquidacion.getEstado())) {
			resultado.put("exito", true);
			resultado.put("estado", "YA_AUTORIZADA");
			resultado.put("mensaje", "La liquidación ya está autorizada. Número de autorización: "
					+ nvl(liquidacion.getAutorizacion(), clave));
			resultado.put("numeroAutorizacion", liquidacion.getAutorizacion());
			return resultado;
		}

		Long ambiente = 1L;
		if (liquidacion.getFacturador() != null && liquidacion.getFacturador().getAmbiente() != null) {
			ambiente = liquidacion.getFacturador().getAmbiente();
		}
		Long idFacturador = liquidacion.getFacturador().getId();

		String urlWS2 = ambiente == 2
				? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
				: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

		System.out.println(">>> Reintentando autorización con clave: " + clave);
		try {
			ResultadoAutorizacion ra = llamarAutorizacionSRI(urlWS2, clave);

			if ("AUTORIZADO".equals(ra.estado)) {
				boolean actualizada = self().marcarLiquidacionAutorizada(
						idLiquidacion, ra.numeroAutorizacion, ra.fechaAutorizacion, ra.comprobanteXML);
				resultado.put("liquidacionActualizada", actualizada);

				try {
					java.util.Map<String, Object> resCxp = self().crearDocumentoCxp(idLiquidacion);
					if (Boolean.TRUE.equals(resCxp.get("aplica"))) {
						resultado.put("documentoCxp", resCxp.get("idDocumentoCxp"));
						resultado.put("asiento", resCxp.get("numeroAlterno"));
					}
				} catch (Exception ae) {
					resultado.put("advertenciaAsiento",
							"Autorizada pero error al crear el documento CXP / asiento: " + ae.getMessage());
					System.err.println("⚠ Error creando documento CXP: " + ae.getMessage());
				}

				resultado.put("exito", true);
				resultado.put("estado", "AUTORIZADO");
				resultado.put("numeroAutorizacion", ra.numeroAutorizacion);
				resultado.put("fechaAutorizacion", ra.fechaAutorizacion);
				resultado.put("mensaje", "Liquidación autorizada correctamente.");
				System.out.println("✓ Liquidación autorizada en reintento: " + ra.numeroAutorizacion);

			} else {
				resultado.put("exito", false);
				resultado.put("estado", ra.estado != null ? ra.estado : "NO_AUTORIZADO");
				resultado.put("mensaje", "El SRI no autorizó el comprobante. Estado: " + ra.estado
						+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
				resultado.put("respuestaSRI", ra.respuestaCompleta);
				System.out.println("✗ Reintento no autorizado: " + ra.estado);
			}
		} catch (Exception e) {
			resultado.put("mensaje", "Error al comunicarse con el SRI: " + e.getMessage());
			resultado.put("error", e.getMessage());
			System.err.println("✗ Error en reintentarAutorizacionLiquidacion: " + e.getMessage());
			e.printStackTrace();
		}

		return resultado;
	}

	// =========================================================================
	// reenviarEmailLiquidacion
	// =========================================================================

	@Override
	public java.util.Map<String, Object> reenviarEmailLiquidacion(Long idLiquidacion, String destinatarios) throws Throwable {
		System.out.println("=== reenviarEmailLiquidacion | idLiquidacion=" + idLiquidacion
				+ " | destinatarios=" + destinatarios + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		if (destinatarios == null || destinatarios.trim().isEmpty()) {
			resultado.put("mensaje", "Debe especificar al menos un correo electrónico destinatario.");
			return resultado;
		}

		LiquidacionCompra liquidacion =
				liquidacionCompraDaoService.selectById(idLiquidacion, NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (liquidacion == null) {
			resultado.put("mensaje", "No se encontró la liquidación con ID: " + idLiquidacion);
			return resultado;
		}

		if (!Long.valueOf(5L).equals(liquidacion.getEstado())) {
			resultado.put("mensaje", "Solo se puede reenviar el email de liquidaciones autorizadas. "
					+ "Estado actual: " + liquidacion.getEstado());
			return resultado;
		}

		String clave = liquidacion.getClave();
		Long idFacturador = liquidacion.getFacturador().getId();
		String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;

		String xmlAutorizado = null;
		byte[] pdfBytes = null;
		try {
			Path pXml = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".xml");
			if (Files.exists(pXml)) {
				xmlAutorizado = new String(Files.readAllBytes(pXml), "UTF-8");
			}
		} catch (Exception e) {
			System.err.println("⚠ Error leyendo XML autorizado: " + e.getMessage());
		}
		try {
			Path pPdf = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".pdf");
			if (Files.exists(pPdf)) {
				pdfBytes = Files.readAllBytes(pPdf);
			} else {
				System.out.println("ℹ PDF no encontrado en disco. Regenerando PDF para liquidación: " + clave);
				pdfBytes = generarPDFLiquidacion(liquidacion, idFacturador, clave, null, liquidacion.getAmbiente());
				if (pdfBytes != null && pdfBytes.length > 0) {
					Files.createDirectories(pPdf.getParent());
					Files.write(pPdf, pdfBytes);
				}
			}
		} catch (Exception e) {
			System.err.println("⚠ Error leyendo/regenerando PDF RIDE: " + e.getMessage());
		}

		String[] listaDestinatarios = destinatarios.split(";");
		java.util.List<String> enviados = new java.util.ArrayList<>();
		java.util.List<String> fallidos = new java.util.ArrayList<>();
		String razonSocial = liquidacion.getFacturador() != null
				? nvl(liquidacion.getFacturador().getRazonSocial(),
					  nvl(liquidacion.getFacturador().getNombre(), "")) : "";
		String numeroLiquidacion = nvl(liquidacion.getNumero(), clave);

		for (String mail : listaDestinatarios) {
			String mailLimpio = mail.trim();
			if (mailLimpio.isEmpty()) continue;
			try {
				emailFacturaService.enviarFacturaAutorizada(
						mailLimpio, numeroLiquidacion, clave,
						razonSocial, "Liquidación de Compra", xmlAutorizado, pdfBytes);
				enviados.add(mailLimpio);
			} catch (Exception e) {
				fallidos.add(mailLimpio + " (error: " + e.getMessage() + ")");
			}
		}

		resultado.put("emailsEnviados", enviados);
		resultado.put("emailsFallidos", fallidos);
		resultado.put("numeroLiquidacion", numeroLiquidacion);
		resultado.put("clave", clave);

		if (!enviados.isEmpty() && fallidos.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado correctamente a " + enviados.size()
					+ " destinatario(s): " + String.join(", ", enviados));
		} else if (!enviados.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado a " + enviados.size()
					+ " destinatario(s). Fallaron " + fallidos.size() + ": " + String.join(", ", fallidos));
		} else {
			resultado.put("exito", false);
			resultado.put("mensaje", "No se pudo enviar el email a ningún destinatario.");
		}

		return resultado;
	}

	// =========================================================================
	// anularLiquidacion
	// =========================================================================

	/**
	 * Corregido el 2026-08-28 (ítem 14) — ver el javadoc de la interfaz para el gap que tenía
	 * esto: {@code AplicacionPagoCxc.liquidacion} existe y se usa activamente, a diferencia de
	 * {@code AplicacionPagoCxp} (que no tiene FK a {@code LiquidacionCompraCompra}, confirmado
	 * en el ítem 13 para el lado compra puro). Ahora sí se verifica antes de anular.
	 */
	@Override
	public java.util.List<java.util.Map<String, Object>> movimientosRelacionadosLiquidacion(Long idLiquidacion)
			throws Throwable {
		System.out.println("=== movimientosRelacionadosLiquidacion | id=" + idLiquidacion + " ===");
		java.util.List<java.util.Map<String, Object>> lista = new java.util.ArrayList<>();
		if (idLiquidacion == null) {
			return lista;
		}
		for (com.saa.model.cxc.AplicacionPagoCxc aplicacion
				: aplicacionPagoCxcDaoService.selectActivasByLiquidacion(idLiquidacion)) {
			java.util.Map<String, Object> fila = new java.util.HashMap<>();
			fila.put("idAplicacion", aplicacion.getId());
			fila.put("tipoDocPago", aplicacion.getTipoDocPago());
			fila.put("montoAplicado", aplicacion.getMontoAplicado());
			fila.put("fechaAplicacion", aplicacion.getFechaAplicacion() != null
					? aplicacion.getFechaAplicacion().toString() : null);
			lista.add(fila);
		}
		return lista;
	}

	@Override
	public java.util.Map<String, Object> anularLiquidacion(Long idLiquidacion, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable {
		System.out.println("=== anularLiquidacion | idLiquidacion=" + idLiquidacion + " | usuario=" + usuario
				+ " | anularEnCascada=" + anularEnCascada + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		LiquidacionCompra liquidacion =
				liquidacionCompraDaoService.selectById(idLiquidacion, NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (liquidacion == null) {
			resultado.put("mensaje", "Liquidación con ID " + idLiquidacion + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(Estado.INACTIVO).equals(liquidacion.getEstado())) {
			resultado.put("mensaje", "La liquidación ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// Movimientos relacionados (ítem 14): cobros/pagos cruzados contra esta liquidación.
		// No hay un revertirAplicacionesDeLiquidacion bulk -- se loopea con revertirAplicacion,
		// mismo criterio que se usó para FacturaCompra en el ítem 13.
		java.util.List<com.saa.model.cxc.AplicacionPagoCxc> movimientos =
				aplicacionPagoCxcDaoService.selectActivasByLiquidacion(idLiquidacion);
		if (!movimientos.isEmpty()) {
			if (!anularEnCascada) {
				StringBuilder detalle = new StringBuilder();
				for (com.saa.model.cxc.AplicacionPagoCxc m : movimientos) {
					if (detalle.length() > 0) detalle.append("; ");
					detalle.append("tipo ").append(m.getTipoDocPago()).append(" $")
							.append(m.getMontoAplicado()).append(" (id ").append(m.getId()).append(")");
				}
				throw new IncomeException("No se puede anular la liquidación " + idLiquidacion
						+ ": tiene " + movimientos.size() + " movimiento(s) relacionado(s) sin reversar: "
						+ detalle + ". Reenvíe la anulación con anularEnCascada=true para reversarlos "
						+ "todos junto con la liquidación.");
			}
			int reversados = 0;
			for (com.saa.model.cxc.AplicacionPagoCxc m : movimientos) {
				aplicacionPagoCxcService.revertirAplicacion(m.getId(),
						"Anulación en cascada de la liquidación " + idLiquidacion + ": " + motivoFinal, idUsuario);
				reversados++;
			}
			resultado.put("movimientosReversados", reversados);
			System.out.println("✓ " + reversados + " movimiento(s) relacionado(s) reversados antes de anular la liquidación.");
		}

		com.saa.model.cxp.LiquidacionCompraCompra lqcc = liquidacion.getDocumentoCxp();
		if (lqcc != null) {
			com.saa.model.cxp.LiquidacionCompraCompra lqccManaged =
					em.find(com.saa.model.cxp.LiquidacionCompraCompra.class, lqcc.getId());
			if (lqccManaged != null) {
				if (lqccManaged.getAsiento() != null && lqccManaged.getAsiento().getCodigo() != null) {
					try {
						com.saa.model.cnt.Asiento asiento = em.find(
								com.saa.model.cnt.Asiento.class, lqccManaged.getAsiento().getCodigo());
						if (asiento != null && !Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO).equals(asiento.getEstado())) {
							asiento.setEstado(Long.valueOf(com.saa.rubros.EstadoAsiento.ANULADO));
							asiento.setMotivoAnulacion(motivoFinal);
							asiento.setFechaAnulacion(ahora);
							asiento.setUsuarioAnulacion(usuarioAnulacion);
							em.merge(asiento);
							resultado.put("asientoAnulado", asiento.getCodigo());
							System.out.println("✓ Asiento del documento CXP anulado: " + asiento.getCodigo());
						}
					} catch (Exception e) {
						resultado.put("advertenciaAsiento",
								"La liquidación fue anulada pero ocurrió un error al anular el asiento: " + e.getMessage());
						System.err.println("⚠ Error al anular asiento: " + e.getMessage());
					}
				}
				lqccManaged.setEstado(Long.valueOf(Estado.INACTIVO));
				em.merge(lqccManaged);
				resultado.put("documentoCxpAnulado", lqccManaged.getId());
			}
		}

		liquidacion.setEstado(Long.valueOf(Estado.INACTIVO));
		liquidacion.setEstadoEmision(3L); // 3 = ANULADA, mismo esquema que Factura
		liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
		em.flush();

		System.out.println("✓ Liquidación anulada: " + idLiquidacion + " | Motivo: " + motivoFinal);

		resultado.put("exito", true);
		resultado.put("mensaje", "Liquidación N° " + nvl(liquidacion.getNumero(), String.valueOf(idLiquidacion))
				+ " anulada correctamente.");
		resultado.put("idLiquidacion", idLiquidacion);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}

	// =========================================================================
	// consultarYActualizarEstadoLiquidacion
	// =========================================================================

	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> consultarYActualizarEstadoLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("=== consultarYActualizarEstadoLiquidacion | idLiquidacion=" + idLiquidacion + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		LiquidacionCompra liquidacion =
				liquidacionCompraDaoService.selectById(idLiquidacion, NombreEntidadesCobro.LIQUIDACION_COMPRA);
		if (liquidacion == null) {
			resultado.put("mensaje", "Liquidación con ID " + idLiquidacion + " no encontrada.");
			return resultado;
		}
		if (liquidacion.getClave() == null || liquidacion.getClave().isEmpty()) {
			resultado.put("mensaje", "La liquidación no tiene clave de acceso registrada.");
			return resultado;
		}

		Long ambiente = liquidacion.getAmbiente() != null ? liquidacion.getAmbiente() : 1L;
		String clave  = liquidacion.getClave();
		Long idFacturador = liquidacion.getFacturador() != null ? liquidacion.getFacturador().getId() : null;
		resultado.put("clave", clave);
		resultado.put("estadoActual", liquidacion.getEstado());

		String urlWS2 = ambiente == 2L
				? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
				: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

		ResultadoAutorizacion ra;
		try {
			ra = llamarAutorizacionSRI(urlWS2, clave);
		} catch (Exception e) {
			resultado.put("mensaje", "Error al consultar el estado en el SRI: " + e.getMessage());
			resultado.put("error", e.getMessage());
			return resultado;
		}

		resultado.put("estadoSRI", ra.estado);
		resultado.put("numeroAutorizacion", ra.numeroAutorizacion);
		resultado.put("fechaAutorizacion", ra.fechaAutorizacion);

		if (!"AUTORIZADO".equals(ra.estado)) {
			resultado.put("mensaje", "El SRI indica que la liquidación NO está autorizada. Estado: " + ra.estado
					+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
			return resultado;
		}

		boolean actualizada;
		try {
			actualizada = self().marcarLiquidacionAutorizada(
					idLiquidacion, ra.numeroAutorizacion, ra.fechaAutorizacion, ra.comprobanteXML);
		} catch (Throwable e) {
			resultado.put("mensaje", "El SRI autorizó la liquidación pero no se pudo actualizar su estado: "
					+ e.getMessage());
			resultado.put("error", e.getMessage());
			return resultado;
		}
		resultado.put("liquidacionActualizada", actualizada);

		boolean documentoCxpCreado = false;
		System.out.println("PASO 4: Creando documento CXP...");
		try {
			java.util.Map<String, Object> resCxp = self().crearDocumentoCxp(idLiquidacion);
			documentoCxpCreado = Boolean.TRUE.equals(resCxp.get("generado"));
			if (Boolean.TRUE.equals(resCxp.get("yaExistia"))) {
				resultado.put("documentoCxpExistente", resCxp.get("idDocumentoCxp"));
			} else if (documentoCxpCreado) {
				resultado.put("documentoCxp", resCxp.get("idDocumentoCxp"));
				resultado.put("asiento", resCxp.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"Liquidación autorizada pero error al crear el documento CXP: " + e.getMessage());
			System.err.println("⚠ Error creando documento CXP: " + e.getMessage());
		}
		resultado.put("documentoCxpCreado", documentoCxpCreado);

		System.out.println("PASO 5: Enviando email al proveedor...");
		String destinatario = null;
		if (liquidacion.getTitular() != null) destinatario = liquidacion.getTitular().getEmail();
		try {
			if (destinatario != null && !destinatario.trim().isEmpty() && idFacturador != null) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				byte[] pdfBytes = null;
				try {
					java.nio.file.Path pXml = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".xml");
					if (Files.exists(pXml)) xmlAutorizado = new String(Files.readAllBytes(pXml), "UTF-8");
					java.nio.file.Path pPdf = Paths.get(resourcesPath + "/lqcs/a/" + clave + ".pdf");
					if (Files.exists(pPdf)) {
						pdfBytes = Files.readAllBytes(pPdf);
					} else {
						pdfBytes = generarPDFLiquidacion(liquidacion, idFacturador, clave, null, ambiente);
						if (pdfBytes != null && pdfBytes.length > 0) {
							Files.createDirectories(Paths.get(resourcesPath + "/lqcs/a/"));
							Files.write(Paths.get(resourcesPath + "/lqcs/a/" + clave + ".pdf"), pdfBytes);
						}
					}
				} catch (Exception ioEx) {
					System.err.println("⚠ Error leyendo archivos para email: " + ioEx.getMessage());
				}
				String razonSocial = liquidacion.getFacturador() != null
						? nvl(liquidacion.getFacturador().getRazonSocial(), nvl(liquidacion.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(
						destinatario, nvl(liquidacion.getNumero(), clave),
						clave, razonSocial, "Liquidación de Compra", xmlAutorizado, pdfBytes);
				resultado.put("emailEnviado", true);
				resultado.put("emailDestinatario", destinatario);
			} else {
				resultado.put("emailEnviado", false);
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail",
					"Liquidación autorizada pero no se pudo enviar el email: " + mailEx.getMessage());
			resultado.put("emailEnviado", false);
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

		resultado.put("exito", true);
		resultado.put("mensaje", "Liquidación verificada en el SRI: AUTORIZADA."
				+ (actualizada ? " Estado actualizado a autorizada." : "")
				+ (documentoCxpCreado ? " Documento CXP y asiento creados." : "")
				+ (Boolean.TRUE.equals(resultado.get("emailEnviado")) ? " Email enviado a " + destinatario + "." : ""));
		System.out.println("=== consultarYActualizarEstadoLiquidacion COMPLETADO ===");
		return resultado;
	}
}
