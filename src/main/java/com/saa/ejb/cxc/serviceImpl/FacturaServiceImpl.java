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
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.ejb.signature.service.SignatureService;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxc.PathFactura;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
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
	
	@EJB
	private com.saa.basico.ejb.DetalleRubroService detalleRubroService;
	
	@EJB
	private ReporteService reporteService;

	@EJB
	private com.saa.ejb.cxc.service.EmailFacturaService emailFacturaService;

	@EJB
	private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

	@Resource
	private SessionContext sessionContext;

	/**
	 * Referencia al propio bean pasando por el contenedor, para que los
	 * @TransactionAttribute de las etapas se apliquen de verdad. Una llamada
	 * directa a this.metodo() se salta los interceptores y correría en la
	 * transacción del llamador.
	 * @return : Vista local de este mismo EJB
	 */
	private FacturaService self() {
		return sessionContext.getBusinessObject(FacturaService.class);
	}

	@EJB
	private com.saa.ejb.cxc.service.AplicacionPagoCxcService aplicacionPagoCxcService;
	
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
		
		// Si es una nueva factura y aún no tiene clave, generar campos automáticos
		if (entidad.getId() == null && (entidad.getClave() == null || entidad.getClave().isEmpty())) {
			entidad.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Validar que tenga los datos necesarios
			if (entidad.getPtoEmision() == null || entidad.getPtoEmision().getId() == null) {
				throw new IncomeException("Debe especificar un punto de emisión para la factura");
			}
			if (entidad.getFacturador() == null || entidad.getFacturador().getId() == null) {
				throw new IncomeException("Debe especificar un facturador para la factura");
			}
			if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
				throw new IncomeException("Debe especificar un titular para la factura");
			}

			// Constantes según SRI
			String tipoComprobante = "01"; // Factura
			String tipoEmision = "1"; // 1=Emisión Normal
			
			// Obtener ambiente desde el facturador (BD) — fuente autoritativa por seguridad
			Facturador facturadorDB = em.find(Facturador.class, entidad.getFacturador().getId());
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
	
	/**
	 * Orquesta el proceso completo SIN transacción propia (NOT_SUPPORTED).
	 * <p>
	 * El envío al SRI es irreversible, así que la emisión se confirma en su
	 * propia transacción y las etapas posteriores (asiento contable, email)
	 * corren aparte: un fallo tardío NUNCA puede reversar una factura ya
	 * autorizada por el SRI.
	 * <p>
	 * Antes esto corría todo en una sola transacción y el {@code catch} del
	 * asiento no servía de nada: cuando un EJB anidado lanza una excepción de
	 * sistema el contenedor marca la transacción del llamador como
	 * rollback-only y el commit final reversa TODO.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> procesarFacturaCompleta(Factura factura,
			java.util.List<DetalleFactura> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== INICIANDO PROCESO COMPLETO DE FACTURA (nuevo flujo: BD tras RECIBIDA) ===");

		// ── PASO 0: Validar cuentas contables (sin escribir en BD) ────────────
		java.util.Map<String, Object> validacion = validarContabilidadFactura(factura, detalles);
		if (validacion != null) {
			return validacion;
		}

		// ── PASOS 1 a 4: emisión ante el SRI, en UNA transacción propia ───────
		// Al volver de aquí, lo que el SRI aceptó ya está confirmado en BD.
		java.util.Map<String, Object> resultado =
				self().emitirFacturaAnteSRI(factura, detalles, ambiente, conectaSRI, destinatario, pathLogo);

		if (!Boolean.TRUE.equals(resultado.get("emitida"))) {
			// No se autorizó: el mapa ya trae etapa, estado y mensaje.
			return resultado;
		}

		Long idFactura      = (Long)   resultado.get("idFactura");
		Long idFacturador   = (Long)   resultado.get("idFacturador");
		String clave        = (String) resultado.get("clave");
		byte[] pdfBytesParaEmail = (byte[]) resultado.get("pdfBytes");
		Factura facturaEmitida   = (Factura) resultado.get("factura");
		destinatario = (String) resultado.get("destinatario");

		System.out.println("✓ Factura AUTORIZADA por el SRI.");
		resultado.put("estado", "AUTORIZADO");

		// ── PASO 5: Generar asiento contable (transacción propia) ─────────────
		System.out.println("PASO 5: Generando asiento contable...");
		try {
			java.util.Map<String, Object> resAsiento = self().generarContabilidadFactura(idFactura);
			if (Boolean.TRUE.equals(resAsiento.get("aplica"))) {
				resultado.put("asiento", resAsiento.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"La factura fue autorizada pero ocurrió un error al generar el asiento contable: "
					+ e.getMessage() + ". Genere el asiento manualmente desde Contabilidad.");
			System.err.println("⚠ Error en asiento contable: " + e.getMessage());
			e.printStackTrace();
		}

		// ── PASO 6: Enviar correo electrónico ─────────────────────────────────
		System.out.println("PASO 6: Enviando email...");
		try {
			if (destinatario != null && !destinatario.trim().isEmpty()) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				try {
					java.nio.file.Path pXml = java.nio.file.Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
					if (java.nio.file.Files.exists(pXml)) xmlAutorizado = new String(java.nio.file.Files.readAllBytes(pXml), "UTF-8");
				} catch (Exception ioEx) {
					System.err.println("⚠ No se pudo leer el XML para el email: " + ioEx.getMessage());
				}
				String razonSocial = facturaEmitida.getFacturador() != null
						? nvl(facturaEmitida.getFacturador().getRazonSocial(), nvl(facturaEmitida.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(destinatario, nvl(facturaEmitida.getNumero(), clave),
						clave, razonSocial, "Factura", xmlAutorizado, pdfBytesParaEmail);
				resultado.put("emailEnviado", true);
				System.out.println("✓ Email enviado a: " + destinatario);
			} else {
				resultado.put("emailEnviado", false);
				System.out.println("ℹ Email omitido: no hay dirección de correo del cliente.");
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail", "La factura fue autorizada pero no se pudo enviar el email: "
					+ mailEx.getMessage() + ". Reenvíe el email manualmente.");
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

		// ── FIN ───────────────────────────────────────────────────────────────
		boolean hayPendientes = Boolean.TRUE.equals(resultado.get("contabilidadPendiente"));
		resultado.put("exito",   true);
		resultado.put("etapa",   hayPendientes ? "COMPLETADO_CON_PENDIENTES" : "COMPLETADO");
		resultado.put("mensaje", hayPendientes
				? "Factura autorizada por el SRI, pero quedaron etapas pendientes. Revise las advertencias."
				: "Factura procesada y autorizada exitosamente.");
		System.out.println("=== PROCESO COMPLETO FINALIZADO"
				+ (hayPendientes ? " (CON PENDIENTES)" : "") + " ===");
		return resultado;
	}

	/**
	 * Valida que estén configuradas las cuentas contables necesarias, antes de
	 * grabar o enviar nada al SRI.
	 * @param factura  : Factura a emitir
	 * @param detalles : Detalles de la factura
	 * @return : null si todo está OK, o el mapa de error listo para devolver
	 */
	private java.util.Map<String, Object> validarContabilidadFactura(Factura factura,
			java.util.List<DetalleFactura> detalles) throws Throwable {

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// ── PASO 0: Validar configuración contable ANTES de grabar ─────────────
		// Solo si el facturador tiene generaConta = 1
		if (factura.getFacturador() != null
				&& Long.valueOf(1L).equals(factura.getFacturador().getGeneraConta())) {

			if (factura.getFacturador().getEmpresa() == null) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "El facturador tiene habilitada la generación contable "
						+ "pero no tiene empresa contable configurada. "
						+ "Configure el campo EMPRESA en el facturador.");
				return resultado;
			}

			Long idEmpresa = factura.getFacturador().getEmpresa().getCodigo();
			System.out.println("PASO 0: Validando cuentas contables para empresa " + idEmpresa + "...");

			java.util.List<String> erroresContables = asientoContableService.validarCuentasContables(
					factura.getTitular(), detalles, idEmpresa);

			if (!erroresContables.isEmpty()) {
				resultado.put("etapa", "VALIDACION_CONTABLE");
				resultado.put("mensaje", "No se puede emitir la factura: faltan cuentas contables. "
						+ "Corrija los siguientes problemas antes de continuar:");
				resultado.put("erroresContables", erroresContables);
				StringBuilder sb = new StringBuilder("Faltan cuentas contables configuradas:\n");
				for (int i = 0; i < erroresContables.size(); i++) {
					sb.append("  ").append(i + 1).append(". ")
					  .append(erroresContables.get(i)).append("\n");
				}
				resultado.put("error", sb.toString());
				System.err.println("✗ Validación contable fallida:\n" + sb);
				return resultado;
			}
			System.out.println("✓ Validación contable OK: todas las cuentas están configuradas.");
		}
		return null;
	}

	/**
	 * Emite la factura ante el SRI en UNA transacción propia (REQUIRES_NEW):
	 * prepara los campos en memoria, genera y firma el XML, envía a recepción
	 * y —sólo si el SRI la acepta— graba factura, detalles, forma de pago y
	 * paths, y persiste el resultado de la autorización.
	 * <p>
	 * Al confirmarse esta transacción el documento queda en BD tal como el SRI
	 * lo conoce. Las etapas siguientes (asiento contable, email) corren fuera.
	 * @return : Mapa con clave, idFactura, idFacturador, factura, destinatario,
	 *           pdfBytes y emitida=true si el SRI la autorizó
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> emitirFacturaAnteSRI(Factura factura,
			java.util.List<DetalleFactura> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);
		// ──────────────────────────────────────────────────────────────────────
		// FLUJO:
		//  PASO 1: Preparar campos (clave, secuencial, número) en MEMORIA — sin BD
		//  PASO 2-3: Generar XML y firmar (datos en memoria)
		//  PASO 4a: WS1 → si RECIBIDA → grabar factura + detalles + forma de pago en BD
		//  PASO 4e: WS2 → persistir el resultado de la autorización
		// ──────────────────────────────────────────────────────────────────────

		try {
			if (conectaSRI == null) conectaSRI = 1L;
			pathLogo = "resources/logos/logo_aso.png";
			if (factura.getTitular() != null && factura.getTitular().getEmail() != null) {
				destinatario = factura.getTitular().getEmail();
			}

			// Variable para almacenar los bytes del PDF generado y pasarlos directamente al email
			byte[] pdfBytesParaEmail = null;

			// ── PASO 1: Preparar campos en MEMORIA (sin guardar en BD) ──────────
			System.out.println("PASO 1: Preparando campos de la factura en memoria...");
			if (factura.getEstado() == null) {
				factura.setEstado(Long.valueOf(com.saa.rubros.Estado.ACTIVO));
			}
			if (factura.getPtoEmision() == null || factura.getPtoEmision().getId() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un punto de emisión.");
				return resultado;
			}
			if (factura.getFacturador() == null || factura.getFacturador().getId() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un facturador.");
				return resultado;
			}
			if (factura.getTitular() == null || factura.getTitular().getCodigo() == null) {
				resultado.put("etapa", "VALIDACION"); resultado.put("mensaje", "Debe especificar un titular.");
				return resultado;
			}

			String tipoComprobante = "01";
			String tipoEmision = "1";
			Facturador facturadorDB = em.find(Facturador.class, factura.getFacturador().getId());
			Long ambienteFacturador;
			if (facturadorDB != null && facturadorDB.getAmbiente() != null) {
				ambienteFacturador = facturadorDB.getAmbiente();
			} else {
				ambienteFacturador = factura.getAmbiente() != null ? factura.getAmbiente() : 1L;
			}
			factura.setAmbiente(ambienteFacturador);
			ambiente = ambienteFacturador;
			System.out.println(">>> AMBIENTE: " + ambiente + (ambiente == 2L ? " (PRODUCCIÓN)" : " (PRUEBAS)") + " | CONECTA_SRI: " + conectaSRI);

			try {
				String secuencial = obtenerSecuencial(factura.getPtoEmision().getId(), tipoComprobante);
				factura.setSecuencial(secuencial);
				String numero = factura.getNumEstablecimiento() + "-" + factura.getNumPtoEmision() + "-" + secuencial;
				factura.setNumero(numero);
				String clave = generarClaveAcceso(factura, tipoComprobante, ambienteFacturador, tipoEmision, secuencial);
				factura.setClave(clave);
				factura.setTipoComprobante(tipoComprobante);
				if (factura.getEstadoEmision() == null) factura.setEstadoEmision(1L);
				System.out.println("✓ Campos preparados en memoria. Clave: " + clave + " | Número: " + numero);
			} catch (Exception e) {
				resultado.put("etapa", "PREPARACION_CAMPOS");
				resultado.put("mensaje", "Error al preparar campos de la factura: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// ── PASO 2-3: Generar y firmar XML con datos en memoria ─────────────
			String clave = factura.getClave();
			Long idFacturador = factura.getFacturador().getId();
			Double subsidio = factura.getSubsidio();
			resultado.put("clave", clave);

			// Preparar forma de pago en memoria para el XML (sin guardar aún en BD)
			com.saa.model.cxc.FormaPagoFactura formaPagoMemoria = new com.saa.model.cxc.FormaPagoFactura();
			String codigoFormaPago = "01";
			if (factura.getFormaPago() != null) {
				try {
					String sqlTsri = "SELECT t.codigo FROM Tsri t WHERE t.id = :idFormaPago";
					Query queryTsri = em.createQuery(sqlTsri);
					queryTsri.setParameter("idFormaPago", factura.getFormaPago());
					String codigoEncontrado = (String) queryTsri.getSingleResult();
					if (codigoEncontrado != null && !codigoEncontrado.isEmpty()) codigoFormaPago = codigoEncontrado;
				} catch (Exception e) {
					System.err.println("⚠ Error al obtener código de forma de pago, usando default 01.");
				}
			}
			formaPagoMemoria.setFormaPago(codigoFormaPago);
			formaPagoMemoria.setValor(factura.getTotal());
			formaPagoMemoria.setPlazo(0L);
			formaPagoMemoria.setUnidadTiempo("dias");
			java.util.List<com.saa.model.cxc.FormaPagoFactura> formasPagoXML = java.util.Arrays.asList(formaPagoMemoria);

			String xmlFirmado;
			try {
				System.out.println("PASO 2: Generando XML en memoria...");
				String[] resultadoXML = generarYGuardarXML(factura, detalles, formasPagoXML, ambiente);
				String xmlSinFirmar = resultadoXML[3];
				System.out.println("PASO 3: Firmando XML...");
				xmlFirmado = signatureService.firmarXMLFacturador(xmlSinFirmar, idFacturador);
				System.out.println("✓ XML generado y firmado.");
			} catch (Exception e) {
				resultado.put("etapa", "GENERACION_XML");
				resultado.put("mensaje", "Error al generar o firmar el XML de la factura: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}

			// ── PASO 4a: Enviar al SRI (WS1) ────────────────────────────────────
			// El documento se graba en BD SOLO si el SRI responde RECIBIDA
			System.out.println("PASO 4a: Enviando XML al SRI (WS1 - Recepción)...");
			String estadoRecepcion = "NO_ENVIADO";
			if (conectaSRI == 1) {
				String baseUploadDir = getBaseUploadDirectory();
				String resourcesPath = baseUploadDir + "resources/" + idFacturador;
				try {
					Path pathFirmado = Paths.get(resourcesPath + "/docs/f/" + clave + ".xml");
					Files.createDirectories(pathFirmado.getParent());
					Files.write(pathFirmado, xmlFirmado.getBytes("UTF-8"));

					String urlWS1 = ambiente == 1
							? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
							: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
					Path logWS1 = Paths.get(resourcesPath + "/docs/e/" + clave + ".txt");
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
				estadoRecepcion = "RECIBIDA"; // simulado cuando no se conecta al SRI
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
			System.out.println("PASO 4b: SRI respondió RECIBIDA. Guardando factura en base de datos...");
			try {
				factura = facturaDaoService.save(factura, null);
			} catch (Exception e) {
				resultado.put("etapa", "GRABADO_FACTURA");
				resultado.put("mensaje", "Error al grabar la factura: " + e.getMessage());
				resultado.put("error", e.getMessage());
				return resultado;
			}
			resultado.put("factura", factura);
			resultado.put("idFactura", factura.getId());
			System.out.println("✓ Factura grabada ID: " + factura.getId() + " | Clave: " + factura.getClave());

			// Guardar detalles
			if (detalles != null && !detalles.isEmpty()) {
				System.out.println("PASO 4c: Guardando " + detalles.size() + " detalles...");
				try {
					for (DetalleFactura detalle : detalles) {
						detalle.setFactura(factura);
						if (detalle.getEstado() == null) detalle.setEstado(Long.valueOf(com.saa.rubros.Estado.ACTIVO));
						detalleFacturaService.saveSingle(detalle);
					}
					System.out.println("✓ Detalles guardados.");
				} catch (Exception e) {
					resultado.put("etapa", "GRABADO_DETALLES");
					resultado.put("mensaje", "Error al grabar los detalles: " + e.getMessage());
					resultado.put("error", e.getMessage());
					return resultado;
				}
			}

			// Guardar forma de pago
			try {
				formaPagoMemoria.setFactura(factura);
				formaPagoFacturaService.saveSingle(formaPagoMemoria);
				System.out.println("✓ Forma de pago guardada: " + codigoFormaPago);
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
				PathFactura pathF = new PathFactura();
				pathF.setFactura(factura);
				pathF.setPath("resources/" + idFacturador + "/docs/f/" + clave + ".xml");
				pathF.setAlterno(3L);
				pathFacturaDaoService.save(pathF, null);
				factura.setEstado(3L);
				facturaDaoService.save(factura, factura.getId());
				if (conectaSRI == 1) {
					Path pathEnviado = Paths.get(resourcesPath + "/docs/e/" + clave + ".xml");
					byte[] bytesXMLFirmado = Files.readAllBytes(Paths.get(resourcesPath + "/docs/f/" + clave + ".xml"));
					Files.write(pathEnviado, bytesXMLFirmado);
					PathFactura pathE = new PathFactura();
					pathE.setFactura(factura);
					pathE.setPath("resources/" + idFacturador + "/docs/e/" + clave + ".xml");
					pathE.setAlterno(4L);
					pathFacturaDaoService.save(pathE, null);
					factura.setEstado(4L);
					facturaDaoService.save(factura, factura.getId());
				}
			} catch (Exception e) {
				System.err.println("⚠ Error registrando paths (no crítico): " + e.getMessage());
			}

			// ── PASO 4d: Si era CLAVE ACCESO REGISTRADA, marcar como autorizada directamente
			if (estadoRecepcion != null && estadoRecepcion.contains("CLAVE ACCESO REGISTRADA")) {
				factura.setAutorizacion(clave);
				factura.setFechaAutorizacion(factura.getFecha().atStartOfDay().plusMinutes(1).plusSeconds(15));
				factura.setEstado(5L);
				facturaDaoService.save(factura, factura.getId());
				em.flush();
				resultado.put("estado", "AUTORIZADO");
				resultado.put("autorizacion", "AUTORIZADO");
				resultado.put("mensaje", "Factura ya registrada en el SRI. Autorizada.");
				// La factura queda autorizada: el orquestador debe generar el
				// asiento igual que en el flujo normal.
				resultado.put("emitida",      true);
				resultado.put("idFacturador", idFacturador);
				resultado.put("destinatario", destinatario);
				resultado.put("pdfBytes",     pdfBytesParaEmail);
				return resultado;
			}

			// ── PASO 4e: WS2 - Autorización ─────────────────────────────────────
			System.out.println("PASO 4e: Consultando autorización al SRI (WS2)...");
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
						Path logWS2A = Paths.get(resourcesPath + "/docs/a/" + clave + ".txt");
						Files.createDirectories(logWS2A.getParent());
						PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
						logWriter2.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2.close();
						Path pathAutorizado = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
						Files.write(pathAutorizado, ra.comprobanteXML.getBytes("UTF-8"));
						PathFactura pathA = new PathFactura();
						pathA.setFactura(factura);
						pathA.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".xml");
						pathA.setAlterno(5L);
						pathFacturaDaoService.save(pathA, null);
						factura.setEstado(5L);
						factura.setEstadoEmision(1L);
						factura.setAutorizacion(ra.numeroAutorizacion);
						factura.setFechaAutorizacion(parseFechaAutorizacion(ra.fechaAutorizacion));
						facturaDaoService.save(factura, factura.getId());
						resultadoAutorizacion = ra.estado;
						autorizada = true;
						// Generar PDF: hacer flush primero para que reporteService (REQUIRES_NEW)
						// pueda ver los datos de la factura ya guardados en esta transacción
						try { em.flush(); } catch (Exception flushEx) {
							System.err.println("⚠ flush antes de PDF: " + flushEx.getMessage());
						}
						// Generar PDF
						try {
							byte[] pdfBytes = generarPDFFactura(factura, idFacturador, clave, pathLogo, ambiente);
							if (pdfBytes != null && pdfBytes.length > 0) {
								Path pathPdf = Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
								Files.write(pathPdf, pdfBytes);
								PathFactura pathPdfRec = new PathFactura();
								pathPdfRec.setFactura(factura);
								pathPdfRec.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".pdf");
								pathPdfRec.setAlterno(7L);
								pathFacturaDaoService.save(pathPdfRec, null);
								// Guardar los bytes del PDF para enviarlos por email sin tener que leerlos del disco
								pdfBytesParaEmail = pdfBytes;
								System.out.println("✓ PDF RIDE generado (" + pdfBytes.length + " bytes).");
							} else {
								System.err.println("⚠ WARNING: generarPDFFactura retornó null o array vacío. El email se enviará sin PDF.");
								System.err.println("   Verifique los logs anteriores para identificar el error en la generación del PDF.");
							}
						} catch (Exception pdfEx) {
							System.err.println("⚠ ERROR generando PDF (no crítico - email se enviará sin PDF): " + pdfEx.getMessage());
							pdfEx.printStackTrace();
						}
						if (ambiente == 2) {
							em.createQuery("UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos,0)+1 WHERE f.id = :id")
								.setParameter("id", idFacturador).executeUpdate();
						}
					} else {
						// NO AUTORIZADO
						Path logWS2N = Paths.get(resourcesPath + "/docs/n/" + clave + ".txt");
						Files.createDirectories(logWS2N.getParent());
						PrintWriter logWriter2N = new PrintWriter(new FileWriter(logWS2N.toFile()));
						logWriter2N.println("Respuesta WS2: " + ra.respuestaCompleta);
						logWriter2N.close();
						if (ra.comprobanteXML != null) {
							Files.write(Paths.get(resourcesPath + "/docs/n/" + clave + ".xml"), ra.comprobanteXML.getBytes("UTF-8"));
							PathFactura pathN = new PathFactura();
							pathN.setFactura(factura);
							pathN.setPath("resources/" + idFacturador + "/docs/n/" + clave + ".xml");
							pathN.setAlterno(6L);
							pathFacturaDaoService.save(pathN, null);
						}
						factura.setEstado(6L);
						factura.setEstadoEmision(2L);
						facturaDaoService.save(factura, factura.getId());
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
				// No conectar SRI — simular autorizado
				autorizada = true;
				resultadoAutorizacion = "AUTORIZADO";
				factura.setEstado(5L);
				facturaDaoService.save(factura, factura.getId());
				
				// Generar PDF también en modo simulación
				try {
					em.flush(); // flush para que reporteService pueda ver los datos
					byte[] pdfBytes = generarPDFFactura(factura, idFacturador, clave, pathLogo, ambiente);
					if (pdfBytes != null && pdfBytes.length > 0) {
						String baseUploadDir = getBaseUploadDirectory();
						String resourcesPath = baseUploadDir + "resources/" + idFacturador;
						Path pathPdf = Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
						Files.createDirectories(pathPdf.getParent());
						Files.write(pathPdf, pdfBytes);
						PathFactura pathPdfRec = new PathFactura();
						pathPdfRec.setFactura(factura);
						pathPdfRec.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".pdf");
						pathPdfRec.setAlterno(7L);
						pathFacturaDaoService.save(pathPdfRec, null);
						pdfBytesParaEmail = pdfBytes;
						System.out.println("✓ PDF RIDE generado (modo simulación) (" + pdfBytes.length + " bytes).");
					} else {
						System.err.println("⚠ WARNING: generarPDFFactura retornó null en modo simulación. El email se enviará sin PDF.");
					}
				} catch (Exception pdfEx) {
					System.err.println("⚠ ERROR generando PDF en modo simulación (no crítico): " + pdfEx.getMessage());
					pdfEx.printStackTrace();
				}
			}

			resultado.put("autorizacion", resultadoAutorizacion);

			if (!autorizada) {
				resultado.put("etapa", "WS2_AUTORIZACION");
				resultado.put("exito", false);
				resultado.put("estado", "NO_AUTORIZADO");
				resultado.put("mensaje", "La factura fue recibida por el SRI pero no fue autorizada. "
						+ "Respuesta del SRI: " + resultadoAutorizacion);
				return resultado;
			}

			// Emisión terminada: la factura está autorizada y confirmada en BD.
			// El asiento contable y el email los ejecuta el orquestador fuera de
			// esta transacción.
			resultado.put("emitida",      true);
			resultado.put("idFacturador", idFacturador);
			resultado.put("destinatario", destinatario);
			resultado.put("pdfBytes",     pdfBytesParaEmail);

		} catch (Exception e) {
			System.err.println("ERROR inesperado en emitirFacturaAnteSRI: " + e.getMessage());
			e.printStackTrace();
			resultado.put("exito", false);
			resultado.put("etapa", "ERROR_INESPERADO");
			resultado.put("error", e.getMessage());
			resultado.put("mensaje", "Error inesperado al procesar la factura: " + e.getMessage());
			sessionContext.setRollbackOnly();
			throw e;
		}

		return resultado;
	}

	/**
	 * Marca la factura como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, número y fecha de autorización, y XML autorizado
	 * en disco. Idempotente.
	 * @param idFactura          : Id de la factura
	 * @param numeroAutorizacion : Número de autorización devuelto por el SRI
	 * @param fechaAutorizacion  : Fecha de autorización devuelta por el SRI
	 * @param comprobanteXML     : XML autorizado (puede ser null)
	 * @return : true si actualizó el estado, false si ya estaba autorizada
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean marcarFacturaAutorizada(Long idFactura, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable {
		System.out.println("Ingresa al metodo marcarFacturaAutorizada con id: " + idFactura);

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("Factura con ID " + idFactura + " no encontrada.");
		}
		if (Long.valueOf(5L).equals(factura.getEstado())) {
			System.out.println("ℹ Factura ya estaba en estado 5 (emitida). Solo se verificó la autorización.");
			return false;
		}

		factura.setEstado(5L);
		factura.setEstadoEmision(1L);
		if (numeroAutorizacion != null && !numeroAutorizacion.isEmpty()) {
			factura.setAutorizacion(numeroAutorizacion);
		}
		if (fechaAutorizacion != null && !fechaAutorizacion.isEmpty()) {
			factura.setFechaAutorizacion(parseFechaAutorizacion(fechaAutorizacion));
		}

		Long idFacturador = factura.getFacturador() != null ? factura.getFacturador().getId() : null;
		if (comprobanteXML != null && !comprobanteXML.isEmpty() && idFacturador != null) {
			try {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				Path pathAutorizado = Paths.get(resourcesPath + "/docs/a/" + factura.getClave() + ".xml");
				Files.createDirectories(pathAutorizado.getParent());
				Files.write(pathAutorizado, comprobanteXML.getBytes("UTF-8"));
				PathFactura pathA = new PathFactura();
				pathA.setFactura(factura);
				pathA.setPath("resources/" + idFacturador + "/docs/a/" + factura.getClave() + ".xml");
				pathA.setAlterno(5L);
				pathFacturaDaoService.save(pathA, null);
				System.out.println("✓ XML autorizado guardado en disco.");
			} catch (Exception xmlEx) {
				System.err.println("⚠ Error guardando XML autorizado (no crítico): " + xmlEx.getMessage());
			}
		}

		facturaDaoService.save(factura, factura.getId());
		em.flush();
		System.out.println("✓ Factura actualizada a estado EMITIDA (5). Aut: " + numeroAutorizacion);
		return true;
	}

	/**
	 * Genera y vincula el asiento contable de una factura en transacción propia
	 * (REQUIRES_NEW). Es idempotente: si la factura ya tiene asiento no genera
	 * otro. Sólo aplica si el facturador tiene generaConta=1 y empresa contable.
	 * @param idFactura : Id de la factura ya autorizada
	 * @return : Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> generarContabilidadFactura(Long idFactura) throws Throwable {
		System.out.println("Ingresa al metodo generarContabilidadFactura con id: " + idFactura);

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("generado", false);
		resultado.put("aplica", false);

		Factura factura = em.find(Factura.class, idFactura);
		if (factura == null) {
			throw new IncomeException("Factura con ID " + idFactura + " no encontrada.");
		}
		if (factura.getFacturador() == null
				|| factura.getFacturador().getEmpresa() == null
				|| !Long.valueOf(1L).equals(factura.getFacturador().getGeneraConta())) {
			System.out.println("ℹ El facturador no genera contabilidad: se omite el asiento.");
			return resultado;
		}
		resultado.put("aplica", true);

		if (factura.getAsiento() != null) {
			resultado.put("yaExistia", true);
			resultado.put("idAsiento", factura.getAsiento().getCodigo());
			resultado.put("numeroAlterno", factura.getAsiento().getNumeroAlterno());
			System.out.println("ℹ La factura ya tiene asiento: " + factura.getAsiento().getNumeroAlterno());
			return resultado;
		}

		// Etapa atómica: se marca el rollback a mano porque IncomeException es
		// una application exception y por sí sola no reversaría esta transacción.
		try {
			Long idEmpresa = factura.getFacturador().getEmpresa().getCodigo();
			String obsAsiento = "Factura N° " + nvl(factura.getNumero(), factura.getClave())
					+ " | Cliente: " + (factura.getTitular() != null ? factura.getTitular().getNombre() : "")
					+ " | " + nvl(factura.getObservacion(), "");
			String usuarioAsiento = factura.getUsuario() != null
					? factura.getUsuario().getNombre() : "SISTEMA";

			com.saa.model.cnt.Asiento asientoGenerado =
					asientoContableService.generarAsientoFactura(
							factura.getId(), idEmpresa,
							com.saa.rubros.TipoAsientos.FACTURAS_VENTA,
							factura.getFecha(), obsAsiento, usuarioAsiento);

			com.saa.model.cnt.Asiento asientoAttached =
					em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
			if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
			factura.setAsiento(asientoAttached);
			facturaDaoService.save(factura, factura.getId());
			em.flush();

			resultado.put("generado", true);
			resultado.put("idAsiento", asientoAttached.getCodigo());
			resultado.put("numeroAlterno", asientoAttached.getNumeroAlterno());
			System.out.println("✓ Asiento contable generado: " + asientoAttached.getNumeroAlterno());
		} catch (Throwable e) {
			sessionContext.setRollbackOnly();
			throw e;
		}
		return resultado;
	}
	
	@Override
	public String[] generarXMLFactura(String clave, Long ambiente) throws Throwable {
		System.out.println("Ingresa al metodo generarXMLFactura con clave: " + clave + " y ambiente: " + ambiente);

		// Consultar factura desde BD (llamada externa, sin datos en memoria)
		String sqlFactura = "SELECT f FROM Factura f WHERE f.clave = :clave";
		Query queryFactura = em.createQuery(sqlFactura);
		queryFactura.setParameter("clave", clave);
		Factura factura = (Factura) queryFactura.getSingleResult();
		if (factura == null) {
			throw new IncomeException("Factura con clave " + clave + " no encontrada");
		}

		String sqlDetalle = "SELECT d FROM DetalleFactura d WHERE d.factura.id = :facturaId";
		Query queryDetalle = em.createQuery(sqlDetalle);
		queryDetalle.setParameter("facturaId", factura.getId());
		@SuppressWarnings("unchecked")
		List<DetalleFactura> detalles = queryDetalle.getResultList();

		String sqlFormasPago = "SELECT fp FROM FormaPagoFactura fp WHERE fp.factura.id = :facturaId";
		Query queryFormasPago = em.createQuery(sqlFormasPago);
		queryFormasPago.setParameter("facturaId", factura.getId());
		@SuppressWarnings("unchecked")
		List<com.saa.model.cxc.FormaPagoFactura> formasPago = queryFormasPago.getResultList();

		return generarYGuardarXML(factura, detalles, formasPago, ambiente);
	}

	/**
	 * Genera y guarda el XML a disco usando datos ya cargados en memoria.
	 * Retorna: [0]=OK, [1]=pathRelativo, [2]=pathAbsoluto, [3]=xmlContent
	 */
	private String[] generarYGuardarXML(Factura factura,
			List<DetalleFactura> detalles,
			List<com.saa.model.cxc.FormaPagoFactura> formasPago,
			Long ambiente) throws Throwable {

		Facturador facturador = factura.getFacturador();
		Titular titular = factura.getTitular();
		String clave = factura.getClave();

		// Dirección del establecimiento (única query necesaria si no viene en la entidad)
		String dirEstablecimiento = "";
		try {
			dirEstablecimiento = (String) em.createQuery(
					"SELECT e.direccion FROM PuntoEmision p JOIN p.establecimiento e WHERE p.id = :id")
				.setParameter("id", factura.getPtoEmision().getId())
				.getSingleResult();
		} catch (Exception e) {
			System.err.println("⚠ No se pudo obtener dirección del establecimiento: " + e.getMessage());
		}

		String xmlContent = generarXMLContent(factura, facturador, titular,
				dirEstablecimiento, detalles, formasPago, ambiente);

		String pathRelativo = "resources/" + facturador.getId() + "/docs/g/" + clave + ".xml";
		String baseUploadDir = getBaseUploadDirectory();
		String pathAbsoluto = baseUploadDir + pathRelativo;

		Path path = Paths.get(pathAbsoluto);
		Files.createDirectories(path.getParent());
		Files.write(path, xmlContent.getBytes("UTF-8"));

		System.out.println("✓ XML generado correctamente en: " + pathAbsoluto);

		// [3] = xmlContent para evitar releer de disco en el paso de firma
		return new String[]{"OK", pathRelativo, pathAbsoluto, xmlContent};
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
		
		// NO escribir la declaración XML aquí
		// El proceso de firma (documentToString) agregará automáticamente la declaración XML
		// al convertir el Document firmado de vuelta a String
		
		// Elemento raíz: factura
		writer.writeStartElement("factura");
		writer.writeAttribute("id", "comprobante");  // SIEMPRE debe ser "comprobante" según estándar del SRI
		writer.writeAttribute("version", "1.1.0");
		writer.writeCharacters("\n");
		
		// infoTributaria
		writer.writeCharacters(" ");
		writer.writeStartElement("infoTributaria");
		writer.writeCharacters("\n");
		
		writeElement(writer, "ambiente", String.valueOf(ambiente), 2);
		writeElement(writer, "tipoEmision", TIPO_EMISION, 2);
		writeElement(writer, "razonSocial", nvl(facturador.getRazonSocial(), ""), 2);
		writeElement(writer, "nombreComercial", nvl(facturador.getNombre(), ""), 2);
		writeElement(writer, "ruc", nvl(facturador.getNumDoc(), ""), 2);
		writeElement(writer, "claveAcceso", nvl(factura.getClave(), ""), 2);
		writeElement(writer, "codDoc", TIPO_DOC, 2);
		writeElement(writer, "estab", nvl(factura.getNumEstablecimiento(), ""), 2);
		writeElement(writer, "ptoEmi", nvl(factura.getNumPtoEmision(), ""), 2);
		writeElement(writer, "secuencial", nvl(factura.getSecuencial(), ""), 2);
		writeElement(writer, "dirMatriz", nvl(facturador.getDireccion(), ""), 2);
		
		// Regímenes especiales
		if (facturador.getMicroEmpresa() != null && facturador.getMicroEmpresa() == 1) {
			writeElement(writer, "regimenMicroempresas", "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS", 2);
		}
		if (facturador.getAgenteRetencion() != null && !facturador.getAgenteRetencion().isEmpty()) {
			writeElement(writer, "agenteRetencion", facturador.getAgenteRetencion(), 2);
		}
		if (facturador.getRimpe() != null && facturador.getRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE RÉGIMEN RIMPE", 2);
		}
		if (facturador.getPopularRimpe() != null && facturador.getPopularRimpe() == 1) {
			writeElement(writer, "contribuyenteRimpe", "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE", 2);
		}
		
		writer.writeCharacters(" ");
		writer.writeEndElement(); // infoTributaria
		writer.writeCharacters("\n");
		
		// infoFactura
		writer.writeCharacters(" ");
		writer.writeStartElement("infoFactura");
		writer.writeCharacters("\n");
		
		writeElement(writer, "fechaEmision", factura.getFecha().format(dateFormatter), 2);
		writeElement(writer, "dirEstablecimiento", nvl(dirEstablecimiento, ""), 2);
		
		if (facturador.getContribuyenteEspecial() != null && !facturador.getContribuyenteEspecial().isEmpty()) {
			writeElement(writer, "contribuyenteEspecial", facturador.getContribuyenteEspecial(), 2);
		}
		
		String obligadoContabilidad = (facturador.getContabilidad() != null && facturador.getContabilidad() == 1) ? "SI" : "NO";
		writeElement(writer, "obligadoContabilidad", obligadoContabilidad, 2);
		
		// Obtener tipoIdentificacionComprador desde DetalleRubro (debe ser siempre 2 dígitos: "04", "05", etc.)
		String tipoIdentificacionComprador = "05"; // Valor por defecto
		try {
			if (titular.getRubroTipoIdentificacionP() != null && titular.getRubroTipoIdentificacionH() != null) {
				String valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
					titular.getRubroTipoIdentificacionP().intValue(), 
					titular.getRubroTipoIdentificacionH().intValue()
				);
				if (valorAlfa != null && !valorAlfa.isEmpty()) {
					// El SRI exige siempre 2 dígitos: "04", "05", "06", etc.
					// Si el rubro devuelve "4" se convierte a "04"
					tipoIdentificacionComprador = valorAlfa.length() == 1
							? "0" + valorAlfa : valorAlfa;
				}
			}
		} catch (Throwable e) {
			System.err.println("Error al obtener tipo identificación: " + e.getMessage());
			// Usar valor por defecto si falla
		}
		writeElement(writer, "tipoIdentificacionComprador", tipoIdentificacionComprador, 2);
		
		writeElement(writer, "razonSocialComprador", nvl(titular.getNombre(), ""), 2);
		writeElement(writer, "identificacionComprador", nvl(titular.getIdentificacion(), ""), 2);
		writeElement(writer, "direccionComprador", nvl(titular.getDireccion(), ""), 2);
		
		// Totales
		Double totalSinImpuestos = sumNulls(factura.getSubtotal(), factura.getSubcero(), 
				factura.getSubtotal5(), factura.getSubtotal8());
		writeElement(writer, "totalSinImpuestos", formatDecimal(totalSinImpuestos), 2);
		
		if (factura.getSubsidio() != null && factura.getSubsidio() > 0) {
			writeElement(writer, "totalSubsidio", formatDecimal(factura.getSubsidio()), 2);
		}
		
		writeElement(writer, "totalDescuento", formatDecimal(nvl(factura.getDescuento(), 0.0)), 2);
		
		// totalConImpuestos
		writer.writeCharacters("  ");
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
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // totalConImpuestos
		writer.writeCharacters("\n");
		
		writeElement(writer, "propina", formatDecimal(nvl(factura.getPropina(), 0.0)), 2);
		writeElement(writer, "importeTotal", formatDecimal(nvl(factura.getTotal(), 0.0)), 2);
		writeElement(writer, "moneda", MONEDA, 2);
		
		// Formas de pago
		writer.writeCharacters("  ");
		writer.writeStartElement("pagos");
		writer.writeCharacters("\n");
		
		// Si no hay formas de pago, agregar una por defecto (01 = Sin utilización del sistema financiero)
		if (formasPago == null || formasPago.isEmpty()) {
			// factura.getFormaPago() contiene el ID de la tabla TSRI; hay que obtener el CODIGO
			String codigoFormaPago = "01";
			if (factura.getFormaPago() != null) {
				try {
					String sqlTsri = "SELECT t.codigo FROM Tsri t WHERE t.id = :idFormaPago";
					Query queryTsri = em.createQuery(sqlTsri);
					queryTsri.setParameter("idFormaPago", factura.getFormaPago());
					String codigoEncontrado = (String) queryTsri.getSingleResult();
					if (codigoEncontrado != null && !codigoEncontrado.isEmpty()) {
						codigoFormaPago = codigoEncontrado;
					}
				} catch (Exception e) {
					System.err.println("⚠ Error al obtener código de forma de pago desde TSRI en XML, usando default 01. ID=" + factura.getFormaPago());
				}
			}
			// El SRI exige siempre 2 dígitos: "01", "02", etc.
			if (codigoFormaPago.length() == 1) codigoFormaPago = "0" + codigoFormaPago;
			writePago(writer, codigoFormaPago, factura.getTotal(), "1", "dias");
		} else {
			// Iterar sobre las formas de pago y agregarlas al XML
			for (com.saa.model.cxc.FormaPagoFactura fp : formasPago) {
				// El plazo siempre debe ser al menos 1, nunca 0
				String plazoStr = (fp.getPlazo() != null && fp.getPlazo() > 0) ?
						String.valueOf(fp.getPlazo()) : "1";
				// El SRI exige siempre 2 dígitos en formaPago
				String codFP = fp.getFormaPago();
				if (codFP != null && codFP.length() == 1) codFP = "0" + codFP;
				writePago(writer, codFP, fp.getValor(), plazoStr, fp.getUnidadTiempo());
			}
		}
		
		writer.writeCharacters("  ");
		writer.writeEndElement(); // pagos
		writer.writeCharacters("\n");
		
		writer.writeCharacters(" ");
		writer.writeEndElement(); // infoFactura
		writer.writeCharacters("\n");
		
		// detalles
		writer.writeCharacters(" ");
		writer.writeStartElement("detalles");
		writer.writeCharacters("\n");
		
		for (DetalleFactura detalle : detalles) {
			writeDetalle(writer, detalle, COD_IVA);
		}
		
		writer.writeCharacters(" ");
		writer.writeEndElement(); // detalles
		writer.writeCharacters("\n");
		
		// infoAdicional
		writer.writeCharacters(" ");
		writer.writeStartElement("infoAdicional");
		writer.writeCharacters("\n");
		
		writer.writeCharacters("  ");
		writer.writeStartElement("campoAdicional");
		writer.writeAttribute("nombre", "Datos Adicionales");
		writer.writeCharacters("Observ.[" + nvl(factura.getObservacion(), "") + "]");
		writer.writeEndElement();
		writer.writeCharacters("\n");
		
		writer.writeCharacters(" ");
		writer.writeEndElement(); // infoAdicional
		writer.writeCharacters("\n");
		
		writer.writeEndElement(); // factura
		writer.writeEndDocument();
		writer.close();
		
		return stringWriter.toString();
	}
	
	private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
		// indent representa el nivel de indentación: 2 = 2 espacios, 3 = 3 espacios, etc.
		for (int i = 0; i < indent; i++) {
			writer.writeCharacters(" ");
		}
		writer.writeStartElement(name);
		writer.writeCharacters(value);
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeTotalImpuesto(XMLStreamWriter writer, String codigo, String codigoPorcentaje, 
			Double baseImponible, Double valor) throws Exception {
		writer.writeCharacters("   ");
		writer.writeStartElement("totalImpuesto");
		writer.writeCharacters("\n");
		writeElement(writer, "codigo", codigo, 4);
		writeElement(writer, "codigoPorcentaje", codigoPorcentaje, 4);
		writeElement(writer, "baseImponible", formatDecimal(baseImponible), 4);
		writeElement(writer, "valor", formatDecimal(valor), 4);
		writer.writeCharacters("   ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writePago(XMLStreamWriter writer, String formaPago, Double total, 
			String plazo, String unidadTiempo) throws Exception {
		writer.writeCharacters("   ");
		writer.writeStartElement("pago");
		writer.writeCharacters("\n");
		writeElement(writer, "formaPago", formaPago, 4);
		writeElement(writer, "total", formatDecimal(total), 4);
		if (plazo != null) {
			writeElement(writer, "plazo", plazo, 4);
		}
		if (unidadTiempo != null) {
			writeElement(writer, "unidadTiempo", unidadTiempo, 4);
		}
		writer.writeCharacters("   ");
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	private void writeDetalle(XMLStreamWriter writer, DetalleFactura detalle, String codIVA) throws Exception {
		writer.writeCharacters("  ");
		writer.writeStartElement("detalle");
		writer.writeCharacters("\n");
		
		// Incluir codigoPrincipal solo si el producto tiene código
		if (detalle.getProducto() != null && detalle.getProducto().getCodigo() != null 
				&& !detalle.getProducto().getCodigo().trim().isEmpty()) {
			writeElement(writer, "codigoPrincipal", detalle.getProducto().getCodigo(), 3);
		}
		
		// Incluir codigoAuxiliar solo si existe
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
		writeElement(writer, "precioTotalSinImpuesto", formatDecimal(detalle.getBaseImponible()), 3);
		
		// Impuestos del detalle
		writer.writeCharacters("   ");
		writer.writeStartElement("impuestos");
		writer.writeCharacters("\n");
		
		writer.writeCharacters("    ");
		writer.writeStartElement("impuesto");
		writer.writeCharacters("\n");
		writeElement(writer, "codigo", codIVA, 5);
		writeElement(writer, "codigoPorcentaje", String.valueOf(detalle.getCodigoIVASRI()), 5);
		writeElement(writer, "tarifa", String.valueOf(detalle.getPorcentajeIVA()), 5);
		writeElement(writer, "baseImponible", formatDecimal(detalle.getBaseImponible()), 5);
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
		// Usar Locale.US para asegurar que siempre use punto decimal (no coma)
		// El SRI requiere formato estándar XML con punto decimal
		return String.format(java.util.Locale.US, "%.2f", value);
	}
	
	/**
	 * Limpia declaraciones XML duplicadas del contenido XML.
	 * Si encuentra múltiples declaraciones <?xml...?>, conserva solo la primera.
	 * También normaliza los saltos de línea entre la declaración y el contenido.
	 * @param xmlContent Contenido XML que puede tener declaraciones duplicadas
	 * @return Contenido XML con una sola declaración
	 */
	@SuppressWarnings("unused")
	private String limpiarDeclaracionesXMLDuplicadas(String xmlContent) {
		if (xmlContent == null || xmlContent.isEmpty()) {
			return xmlContent;
		}
		
		// Patrón para encontrar declaraciones XML: <?xml version="..." encoding="..."?>
		// Puede tener o no el atributo standalone
		String patronDeclaracion = "<\\?xml\\s+version\\s*=\\s*[\"'][^\"']+[\"']\\s*(?:encoding\\s*=\\s*[\"'][^\"']+[\"'])?\\s*(?:standalone\\s*=\\s*[\"'][^\"']+[\"'])?\\s*\\?>";
		
		// Contar cuántas declaraciones hay
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patronDeclaracion);
		java.util.regex.Matcher matcher = pattern.matcher(xmlContent);
		
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		
		if (count > 1) {
			// Hay duplicados, conservar solo la primera declaración
			System.out.println("⚠ ADVERTENCIA: Se encontraron " + count + " declaraciones XML. Limpiando duplicados...");
			
			// Encontrar la primera declaración
			matcher.reset();
			if (matcher.find()) {
				String primeraDeclaracion = matcher.group();
				
				// Remover TODAS las declaraciones del contenido
				String contenidoSinDeclaraciones = xmlContent.replaceAll(patronDeclaracion, "");
				
				// Limpiar espacios y saltos de línea al inicio del contenido
				contenidoSinDeclaraciones = contenidoSinDeclaraciones.trim();
				
				// Agregar solo la primera declaración al inicio con UN SOLO salto de línea
				xmlContent = primeraDeclaracion + "\n" + contenidoSinDeclaraciones;
				
				System.out.println("✓ XML limpiado: ahora tiene 1 sola declaración XML");
			}
		}
		
		// Normalizar saltos de línea múltiples después de la declaración XML
		// El patrón busca: declaración XML seguida de múltiples saltos de línea
		xmlContent = xmlContent.replaceAll("(<\\?xml[^?]+\\?>)\\s*\\n\\s*\\n+", "$1\n");
		
		// Eliminar espacios en blanco al final del contenido
		xmlContent = xmlContent.trim();
		
		return xmlContent;
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
			// 1. Grabar XML firmado TAL CUAL viene (NO modificar nada post-firma,
			//    cualquier cambio invalida la firma electrónica)
			Path pathFirmado = Paths.get(resourcesPath + "/docs/f/" + clave + ".xml");
			Files.createDirectories(pathFirmado.getParent());
			Files.write(pathFirmado, xml.getBytes("UTF-8"));
			
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
					
					// Leer bytes crudos del XML firmado (NO convertir a String ni modificar nada)
					byte[] bytesXMLFirmado = Files.readAllBytes(pathFirmado);

					String estadoRecepcion = llamarRecepcionSRI(urlWS1, bytesXMLFirmado, logWriter1);
					
					logWriter1.close();
					
					// Guardar XML enviado (copia exacta del firmado)
					Path pathEnviado = Paths.get(resourcesPath + "/docs/e/" + clave + ".xml");
					Files.write(pathEnviado, bytesXMLFirmado);
					
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
								// 1. Guardar TXT de log WS2 (autorizado) - igual que el PHP
								Path logWS2A = Paths.get(resourcesPath + "/docs/a/" + clave + ".txt");
								Files.createDirectories(logWS2A.getParent());
								PrintWriter logWriter2 = new PrintWriter(new FileWriter(logWS2A.toFile()));
								logWriter2.println("Respuesta WS2: " + resultado.respuestaCompleta);
								logWriter2.close();
								
								// 2. Guardar XML autorizado
								Path pathAutorizado = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
								Files.write(pathAutorizado, resultado.comprobanteXML.getBytes("UTF-8"));
								
								// 3. Insertar path autorizado en tabla ptfc (alterno=5)
								PathFactura pathA = new PathFactura();
								pathA.setFactura(factura);
								pathA.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".xml");
								pathA.setAlterno(5L); // 5 = XML autorizado
								pathFacturaDaoService.save(pathA, null);
								
								// 4. Actualizar estado a AUTORIZADA (estado=5, estadoEmision=1)
								factura.setEstado(5L);
								factura.setEstadoEmision(1L);
								factura.setAutorizacion(resultado.numeroAutorizacion);
								factura.setFechaAutorizacion(parseFechaAutorizacion(resultado.fechaAutorizacion));
								facturaDaoService.save(factura, factura.getId());
								
								respuesta = resultado.estado;
								
								// 5. Generar PDF (RIDE) - flush primero para que reporteService
								// (REQUIRES_NEW) vea los datos ya guardados en esta transacción
								try { em.flush(); } catch (Exception flushEx) {
									System.err.println("⚠ flush antes de PDF: " + flushEx.getMessage());
								}
								byte[] pdfBytesParaEmail = null;
								try {
									byte[] pdfBytes = generarPDFFactura(factura, idFacturador, clave, pathLogo, ambiente);
									if (pdfBytes != null && pdfBytes.length > 0) {
										Path pathPdf = Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
										Files.write(pathPdf, pdfBytes);
										
										// Insertar path PDF en tabla ptfc (alterno=7)
										PathFactura pathPdfRec = new PathFactura();
										pathPdfRec.setFactura(factura);
										pathPdfRec.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".pdf");
										pathPdfRec.setAlterno(7L); // 7 = PDF RIDE
										pathFacturaDaoService.save(pathPdfRec, null);
										System.out.println("✓ PDF RIDE generado: " + pathPdf);
										pdfBytesParaEmail = pdfBytes;
									}
								} catch (Exception pdfEx) {
									System.err.println("⚠ Error generando PDF (no crítico): " + pdfEx.getMessage());
									pdfEx.printStackTrace();
								}

								// 7. Si es producción, actualizar contador de documentos emitidos
								if (ambiente == 2) {
									String sqlUpdate = "UPDATE Facturador f SET f.docEmitidos = COALESCE(f.docEmitidos, 0) + 1 WHERE f.id = :idFacturador";
									Query updateQuery = em.createQuery(sqlUpdate);
									updateQuery.setParameter("idFacturador", idFacturador);
									updateQuery.executeUpdate();
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
	private String llamarRecepcionSRI(String url, byte[] xmlBytes, PrintWriter log) throws Exception {
		try {
			System.out.println(">>> Llamando al WS1 de RECEPCIÓN del SRI: " + url);
			log.println(">>> Llamando al WS1 de RECEPCIÓN del SRI: " + url);

			String xmlBase64 = java.util.Base64.getEncoder().encodeToString(xmlBytes);
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:rec=\"http://ec.gob.sri.ws.recepcion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<rec:validarComprobante><xml>" + xmlBase64 + "</xml></rec:validarComprobante>" +
				"</soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			log.println(">>> Respuesta WS1 completa:\n" + respuestaCompleta);
			System.out.println(">>> XML RESPUESTA WS1 (Recepción SRI):\n" + respuestaCompleta);

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");

			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado WS1: " + estado);
				log.println(">>> Estado extraído: " + estado);

				NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
				if (mensajeList.getLength() == 0) mensajeList = docEl.getElementsByTagName("mensaje");
				if (mensajeList.getLength() > 0) {
					String mensaje = mensajeList.item(0).getTextContent();
					if (mensaje != null && mensaje.contains("CLAVE ACCESO REGISTRADA")) {
						System.out.println(">>> Clave de acceso ya registrada");
						log.println(">>> Clave de acceso ya registrada");
						return "CLAVE ACCESO REGISTRADA";
					}
				}
				return estado;
			}

			System.err.println(">>> ADVERTENCIA: No se encontró nodo <estado> en la respuesta WS1");
			log.println(">>> ADVERTENCIA: No se encontró nodo <estado> en la respuesta");
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
	 * Llama al servicio de autorización del SRI (WS2)
	 */
	private ResultadoAutorizacion llamarAutorizacionSRI(String url, String claveAcceso) throws Exception {
		System.out.println(">>> Llamando al WS2 de AUTORIZACIÓN del SRI: " + url);
		try {
			String soapEnvelope =
				"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">" +
				"<soapenv:Header/><soapenv:Body>" +
				"<aut:autorizacionComprobante>" +
				"<claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>" +
				"</aut:autorizacionComprobante>" +
				"</soapenv:Body></soapenv:Envelope>";

			String respuestaCompleta = com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(url, soapEnvelope);
			System.out.println(">>> XML RESPUESTA WS2 (Autorización SRI):\n" + respuestaCompleta);

			ResultadoAutorizacion resultado = new ResultadoAutorizacion();
			resultado.respuestaCompleta = respuestaCompleta;

			javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			org.w3c.dom.Element docEl = dbf.newDocumentBuilder()
					.parse(new java.io.ByteArrayInputStream(respuestaCompleta.getBytes("UTF-8")))
					.getDocumentElement();

			NodeList estadoList = docEl.getElementsByTagNameNS("*", "estado");
			if (estadoList.getLength() == 0) estadoList = docEl.getElementsByTagName("estado");
			if (estadoList.getLength() > 0) {
				resultado.estado = estadoList.item(0).getTextContent();
				System.out.println(">>> Estado WS2: " + resultado.estado);
			} else {
				System.err.println(">>> ADVERTENCIA: No se encontró nodo <estado> en respuesta WS2");
			}

			NodeList numAutList = docEl.getElementsByTagNameNS("*", "numeroAutorizacion");
			if (numAutList.getLength() == 0) numAutList = docEl.getElementsByTagName("numeroAutorizacion");
			if (numAutList.getLength() > 0) resultado.numeroAutorizacion = numAutList.item(0).getTextContent();

			NodeList fechaAutList = docEl.getElementsByTagNameNS("*", "fechaAutorizacion");
			if (fechaAutList.getLength() == 0) fechaAutList = docEl.getElementsByTagName("fechaAutorizacion");
			if (fechaAutList.getLength() > 0) resultado.fechaAutorizacion = fechaAutList.item(0).getTextContent();

			NodeList comprobanteList = docEl.getElementsByTagNameNS("*", "comprobante");
			if (comprobanteList.getLength() == 0) comprobanteList = docEl.getElementsByTagName("comprobante");
			if (comprobanteList.getLength() > 0) resultado.comprobanteXML = comprobanteList.item(0).getTextContent();

			NodeList mensajeIdList = docEl.getElementsByTagNameNS("*", "identificador");
			if (mensajeIdList.getLength() == 0) mensajeIdList = docEl.getElementsByTagName("identificador");
			if (mensajeIdList.getLength() > 0) resultado.mensajeId = mensajeIdList.item(0).getTextContent();

			NodeList mensajeList = docEl.getElementsByTagNameNS("*", "mensaje");
			if (mensajeList.getLength() == 0) mensajeList = docEl.getElementsByTagName("mensaje");
			if (mensajeList.getLength() > 0) resultado.mensaje = mensajeList.item(0).getTextContent();

			NodeList infoAdicionalList = docEl.getElementsByTagNameNS("*", "informacionAdicional");
			if (infoAdicionalList.getLength() == 0) infoAdicionalList = docEl.getElementsByTagName("informacionAdicional");
			if (infoAdicionalList.getLength() > 0) resultado.informacionAdicional = infoAdicionalList.item(0).getTextContent();

			return resultado;
		} catch (Exception e) {
			System.err.println(">>> ERROR en llamarAutorizacionSRI: " + e.getMessage());
			e.printStackTrace();
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

		// Armar clave sin dígito verificador
		String claveSinDV = fechaClave + tipoComprobante + ruc + ambiente + 
				factura.getNumEstablecimiento() + factura.getNumPtoEmision() + 
				secuencial + codClave + tipoEmision;

		// Calcular dígito verificador con módulo 11
		int digitoVerificador = calcularModulo11(claveSinDV);

		String claveCompleta = claveSinDV + digitoVerificador;
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
	/**
	 * Genera el PDF RIDE de la factura consultando directamente la BD,
	 * replicando exactamente el flujo del PHP gn_rprt.php.
	 */
	@SuppressWarnings("unchecked")
	private byte[] generarPDFFactura(Factura facturaObj, Long idFacturador, String clave,
			String pathLogoParam, Long ambiente) {
		try {
			System.out.println("Generando PDF RIDE para factura: " + clave);

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 1: Equivale al primer SELECT del PHP
			//   select b.*, c.*, d.* from fctr b, fcdr c, cmpr d where ...
			// Tablas: CBR.FCTR, CBR.FCDR, TSR.TTLR
			// Columnas según @Column de cada modelo Java
			// ─────────────────────────────────────────────────────────────────────
			jakarta.persistence.Query q1 = em.createNativeQuery(
				"SELECT f.ID, f.AMBIENTE, f.AUTORIZACION, f.FECHAAUTORIZACION, f.NUMERO, f.CLAVE, " +
				"       f.FECHA, f.SUBTOTAL, f.TOTAL, f.SUBCERO, f.SUBTOTAL5, f.SUBTOTAL8, " +
				"       f.VIVA, f.VIVA5, f.VIVA8, f.PROPINA, f.DESCUENTO, f.OBSERVACION, " +
				"       f.PTOEMISION, " +
				"       fc.ID, fc.NUMDOC, fc.RAZONSOCIAL, fc.NOMBRECOMERCIAL, " +
				"       fc.MAIL, fc.TELEFONO, fc.LOGO, fc.DIRECCION, " +
				"       fc.MICROEMPRESA, fc.RIMPE, fc.POPULARRIMPE, fc.ARTESANO, " +
				"       fc.CONTRIBUYENTEESPECIAL, fc.CONTABILIDAD, fc.AGENTERETENCION, " +
				"       COALESCE(NULLIF(t.TTLRRZSC, ''), t.TTLRNMBR), t.TTLRIDNT, t.TTLRDRCC, t.TTLRMLLL, t.TTLRTLFN " +
				"FROM CBR.FCTR f " +
				"JOIN CBR.FCDR fc ON f.FACTURADOR = fc.ID " +
				"JOIN TSR.TTLR t  ON f.COMPRADOR = t.TTLRCDGO " +
				"WHERE f.CLAVE = :clave");
			q1.setParameter("clave", clave);
			Object[] row = (Object[]) q1.getSingleResult();

			// Posiciones del SELECT (índice → columna):
			//  0=f.ID  1=AMBIENTE  2=AUTORIZACION  3=FECHAAUTORIZACION  4=NUMERO  5=CLAVE
			//  6=FECHA  7=SUBTOTAL  8=TOTAL  9=SUBCERO  10=SUBTOTAL5  11=SUBTOTAL8
			// 12=VIVA  13=VIVA5  14=VIVA8  15=PROPINA  16=DESCUENTO  17=OBSERVACION
			// 18=PTOEMISION  19=fc.ID  20=NUMDOC  21=RAZONSOCIAL  22=NOMBRECOMERCIAL
			// 23=MAIL  24=TELEFONO  25=LOGO  26=DIRECCION  27=MICROEMPRESA  28=RIMPE
			// 29=POPULARRIMPE  30=ARTESANO  31=CONTRIBUYENTEESPECIAL  32=CONTABILIDAD
			// 33=AGENTERETENCION  34=TTLRNMBR  35=TTLRIDNT  36=TTLRDRCC  37=TTLRMLLL  38=TTLRTLFN
			Long   idFactura           = toLong(row[0]);
			int    idAmb               = toInt(row[1]);
			String autorizacion        = str(row[2]);
			String fechaAutorizacion   = str(row[3]);
			String numFactura          = str(row[4]);
			String claveAcceso         = str(row[5]);
			String fecha               = str(row[6]);
			double subtotal12          = toDouble(row[7]);
			double total               = toDouble(row[8]);
			double subcero             = toDouble(row[9]);
			double subtotal5           = toDouble(row[10]);
			double subtotal8           = toDouble(row[11]);
			double vIVA                = toDouble(row[12]);
			double vIVA5               = toDouble(row[13]);
			double vIVA8               = toDouble(row[14]);
			double propina             = toDouble(row[15]);
			double descuento           = toDouble(row[16]);
			String observacion         = str(row[17]);
			Long   idPtoEmision        = toLong(row[18]);
			// fc.ID = row[19] (no se usa)
			String ruc                 = str(row[20]);
			String razonSocial         = str(row[21]);
			String nombreComercial     = str(row[22]);
			String mailFcdr            = str(row[23]);
			String telFcdr             = str(row[24]);
			String logo                = str(row[25]);
			String dirFcdr             = str(row[26]);
			int    microEmpresa        = toInt(row[27]);
			int    rimpe               = toInt(row[28]);
			int    rimpePopular        = toInt(row[29]);
			// row[30] artesano (no se usa en tipo empresa)
			String contribuyenteEspecial = str(row[31]);
			int    contabilidad          = toInt(row[32]);
			String agenteRetencion       = str(row[33]);
			// comprador (cmpr en PHP = TSR.TTLR)
			String nomCmdr             = str(row[34]);
			String numDocCmdr          = str(row[35]);
			String dirCmdr             = str(row[36]);
			String mailCmdr            = str(row[37]);
			String telCmdr             = str(row[38]); // $iaFonos en PHP = $primero['telefono'] = cmpr.telefono

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 2: Establecimiento — CBR.PTEM, CBR.ESTB
			// ─────────────────────────────────────────────────────────────────────
			@SuppressWarnings("unused")
			String estNombre = "", estDireccion = "", estTelefono = "", estMail = "";
			boolean esMatriz = true;
			String obsEstablecimiento = "";
			try {
				jakarta.persistence.Query q2 = em.createNativeQuery(
					"SELECT b.NOMBRE, b.DIRECCION, b.TELEFONO, b.MAIL, b.MATRIZ, a.OBSERVACION " +
					"FROM CBR.PTEM a JOIN CBR.ESTB b ON a.ESTABLECIMIENTO = b.ID " +
					"WHERE a.ID = :id");
				q2.setParameter("id", idPtoEmision);
				Object[] est = (Object[]) q2.getSingleResult();
				estNombre          = str(est[0]);
				estDireccion       = str(est[1]);
				estTelefono        = str(est[2]);
				estMail            = str(est[3]);
				esMatriz           = toInt(est[4]) == 1;
				obsEstablecimiento = str(est[5]);
			} catch (Exception ignored) {}

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 3: Formas de pago (PHP: select from fpfc, tsri where lSRI=24)
			// ─────────────────────────────────────────────────────────────────────
			StringBuilder sbFormasPago = new StringBuilder();
			try {
				jakarta.persistence.Query q3 = em.createNativeQuery(
					"SELECT c.detalle, a.valor, a.plazo, a.unidadTiempo " +
					"FROM CBR.FPFC a " +
					"JOIN CBR.TSRI c ON c.LSRI = 24 AND c.CODIGO = a.FORMAPAGO " +
					"WHERE a.FACTURA = :id");
				q3.setParameter("id", idFactura);
				java.util.List<Object[]> fps = q3.getResultList();
				for (Object[] fp : fps) {
					String detalle      = str(fp[0]);
					double valorFP      = toDouble(fp[1]);
					int    plazoFP      = toInt(fp[2]);
					String unidadTiempo = str(fp[3]);
					String numDias = (plazoFP != 0) ? plazoFP + " " + unidadTiempo : "";
					sbFormasPago.append(detalle)
						.append("   $ ").append(String.format(java.util.Locale.US, "%.2f", valorFP))
						.append("   ").append(numDias).append("\n");
				}
			} catch (Exception e) {
				System.err.println("⚠ Formas de pago PDF: " + e.getMessage());
			}

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 4: IVA general vigente (PHP: select from tsri where lSRI=614)
			// Tabla: CBR.TSRI
			// ─────────────────────────────────────────────────────────────────────
			int valorIvaGeneral = 15;
			try {
				jakarta.persistence.Query q4 = em.createNativeQuery(
					"SELECT PORCENTAJE FROM CBR.TSRI WHERE LSRI = 614 FETCH FIRST 1 ROWS ONLY");
				Object porcObj = q4.getSingleResult();
				if (porcObj != null) valorIvaGeneral = ((Number) porcObj).intValue();
			} catch (Exception ignored) {}

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 5: Logo — ruta absoluta
			// ─────────────────────────────────────────────────────────────────────
			String logoPath = "";
			if (logo != null && !logo.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				// El campo 'logo' puede ser ruta relativa o absoluta
				String candidato = logo.startsWith("/") || logo.contains(":\\")
						? logo : baseDir + logo;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(candidato))) {
					logoPath = candidato;
				}
			}
			// fallback: logo pasado como parámetro
			if (logoPath.isEmpty() && pathLogoParam != null && !pathLogoParam.isEmpty()) {
				String baseDir = getBaseUploadDirectory();
				String candidato = baseDir + pathLogoParam;
				if (java.nio.file.Files.exists(java.nio.file.Paths.get(candidato))) {
					logoPath = candidato;
				}
			}

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 6: Construir campos calculados (equivalentes al PHP)
			// ─────────────────────────────────────────────────────────────────────
			String ambStr = (idAmb == 2) ? "PRODUCCIÓN" : "PRUEBAS";

			// Tipo empresa
			String tipoEmpresa = "";
			if (microEmpresa == 1)  tipoEmpresa = "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS";
			else if (rimpe == 1)    tipoEmpresa = "CONTRIBUYENTE RÉGIMEN RIMPE";
			else if (rimpePopular == 1) tipoEmpresa = "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE";

			// Leyendas opcionales
			String leyendaAgente       = (agenteRetencion       != null && !agenteRetencion.isEmpty())       ? agenteRetencion       : "";
			String leyendaContribuyente= (contribuyenteEspecial != null && !contribuyenteEspecial.isEmpty()) ? contribuyenteEspecial : "";

			// Info del facturador: si es sucursal, agregar datos de sucursal
			String dirFcdrCompleta = dirFcdr;
			String telFcdrCompleta = telFcdr;
			if (!esMatriz && !estNombre.isEmpty()) {
				dirFcdrCompleta = dirFcdr + " | Suc: " + estNombre + " - " + estDireccion;
				telFcdrCompleta = telFcdr + " / " + estTelefono;
			}

			// Contabilidad
			String contabilidadStr = (contabilidad == 1) ? "SI" : "NO";

			// Información adicional (PHP: $iaFonos=$primero['telefono'] = comprador.telefono, $iaObaservacion=$establecimiento['observacion'].' '.$primero['observacion'])
			String iaFonos = telCmdr;
			String iaObservacion = (obsEstablecimiento + " " + observacion).trim();
			String infoAdicional = "Teléfonos: " + iaFonos + "\nObservación: " + iaObservacion;

			// Totales (PHP: $subtotal_sin_impuestos)
			double subtotalSinImp = subtotal12 + subcero + subtotal5 + subtotal8;

			// Fecha de autorización formateada
			String fechaAutoStr = fechaAutorizacion != null ? fechaAutorizacion : "";

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 7: Construir mapa de parámetros para Jasper
			// ─────────────────────────────────────────────────────────────────────
			java.util.Map<String, Object> p = new java.util.HashMap<>();
			// CRÍTICO: P_ID_FACTURA es el parámetro principal que usa el JRXML en su
			// cláusula WHERE para obtener todos los datos. Sin este el PDF sale en blanco.
			p.put("P_ID_FACTURA",              idFactura);
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
			p.put("P_AGENTE_RETENCION",        leyendaAgente);
			p.put("P_CONTRIBUYENTE_ESPECIAL",  leyendaContribuyente);
			p.put("P_OBLIGADO_CONTABILIDAD",   contabilidadStr);
			// Factura
			p.put("P_NUMERO_FACTURA",          numFactura);
			p.put("P_NUMERO_AUTORIZACION",     autorizacion != null ? autorizacion : claveAcceso);
			p.put("P_FECHA_EMISION",           fecha);
			p.put("P_FECHA_AUTORIZACION",      fechaAutoStr);
			p.put("P_AMBIENTE",                ambStr);
			p.put("P_GUIA_REMISION",           "");
			// Comprador
			p.put("P_RAZON_SOCIAL_COMPRADOR",   nomCmdr);
			p.put("P_IDENTIFICACION_COMPRADOR", numDocCmdr);
			p.put("P_DIRECCION_COMPRADOR",      dirCmdr);
			p.put("P_EMAIL_COMPRADOR",          mailCmdr);
			// Totales (exactamente como el PHP)
			p.put("P_PORC_IVA",               valorIvaGeneral);
			p.put("P_SUBTOTAL_IVA",           subtotal12);
			p.put("P_SUBTOTAL_5",             subtotal5);
			p.put("P_SUBTOTAL_0",             subcero);
			p.put("P_SUBTOTAL_8",             subtotal8);
			p.put("P_SUBTOTAL_SIN_IMP",       subtotalSinImp);
			p.put("P_DESCUENTO",              descuento);
			p.put("P_IVA",                    vIVA);
			p.put("P_IVA_5",                  vIVA5);
			p.put("P_IVA_8",                  vIVA8);
			p.put("P_PROPINA",                propina);
			p.put("P_TOTAL",                  total);
			// Pie
			p.put("P_FORMAS_PAGO",            sbFormasPago.toString());
			p.put("P_INFO_ADICIONAL",         infoAdicional);

			// ─────────────────────────────────────────────────────────────────────
			// BLOQUE 8: Generar PDF con JasperReports
			// ─────────────────────────────────────────────────────────────────────
			byte[] pdfBytes = reporteService.generarReporte("cxc", "RPRT_RIDE_FACTURA", p, "PDF");
			System.out.println("✓ PDF RIDE generado correctamente ("
					+ (pdfBytes != null ? pdfBytes.length : 0) + " bytes)");
			return pdfBytes;

		} catch (Exception e) {
			System.err.println("Error generando PDF RIDE: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	// ── Helpers de conversión de tipos para resultados de native query ───────
	private String str(Object o)       { return o != null ? o.toString() : ""; }
	private double toDouble(Object o)  { return o != null ? ((Number) o).doubleValue() : 0.0; }
	private int    toInt(Object o)     { return o != null ? ((Number) o).intValue()    : 0; }
	private Long   toLong(Object o)    { return o != null ? ((Number) o).longValue()   : 0L; }

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
	
	/**
	 * MÉTODO DE PRUEBA: Envía un XML correcto al SRI para verificar la comunicación
	 */
	@Override
	public String probarEnvioXMLCorrecto(String xmlCorrecto) throws Exception {
		System.out.println("=== PRUEBA: Enviando XML correcto al SRI ===");
		
		try {
			// Extraer ambiente del XML para seleccionar el WS correcto (igual que PHP de referencia)
			// ambiente=1 → celcer.sri.gob.ec (certificación/pruebas)
			// ambiente=2 → cel.sri.gob.ec (producción)
			int ambienteXML = 1; // default certificación
			try {
				javax.xml.parsers.DocumentBuilder db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
				org.w3c.dom.Document docXML = db.parse(new java.io.ByteArrayInputStream(xmlCorrecto.getBytes("UTF-8")));
				NodeList ambienteNodes = docXML.getElementsByTagName("ambiente");
				if (ambienteNodes.getLength() > 0) {
					ambienteXML = Integer.parseInt(ambienteNodes.item(0).getTextContent().trim());
				}
			} catch (Exception ex) {
				System.out.println("⚠ No se pudo extraer ambiente del XML, usando certificación por defecto: " + ex.getMessage());
			}
			
			String urlWS1 = ambienteXML == 1
					? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl"
					: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
			String urlWS2 = ambienteXML == 1
					? "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
					: "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
			
			System.out.println(">>> PRUEBA: Ambiente detectado: " + ambienteXML + " → URL WS1: " + urlWS1);
			
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
			// Crear elemento xml SIN namespace
			SOAPElement xml = validarComprobante.addChildElement(envelope.createName("xml", "", ""));
			xml.addTextNode(xmlCorrecto);
			
			soapMessage.saveChanges();
			
			System.out.println(">>> PRUEBA: Mensaje SOAP Request creado");
			
			// Log del request
			ByteArrayOutputStream requestBaos = new ByteArrayOutputStream();
			soapMessage.writeTo(requestBaos);
			String requestXml = requestBaos.toString("UTF-8");
			System.out.println(">>> PRUEBA: REQUEST SOAP (primeros 1000 caracteres):");
			System.out.println(requestXml.substring(0, Math.min(1000, requestXml.length())));
			
			// Llamar al servicio
			SOAPMessage soapResponse = soapConnection.call(soapMessage, urlWS1);
			
			System.out.println(">>> PRUEBA: Respuesta recibida del SRI");
			
			// Convertir respuesta a String
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			soapResponse.writeTo(baos);
			String respuestaCompleta = baos.toString("UTF-8");
			
			System.out.println(">>> PRUEBA: Respuesta WS1 completa:");
			System.out.println(respuestaCompleta);
			
			// Extraer estado
			SOAPBody responseBody = soapResponse.getSOAPBody();
			NodeList estadoList = responseBody.getElementsByTagName("estado");
			
			if (estadoList.getLength() > 0) {
				String estado = estadoList.item(0).getTextContent();
				System.out.println(">>> PRUEBA: Estado WS1 extraído: " + estado);
				
				if ("RECIBIDA".equals(estado)) {
					soapConnection.close();
					// Esperar 2 segundos igual que el PHP de referencia
					System.out.println(">>> PRUEBA: Comprobante RECIBIDO. Esperando 2s para llamar WS2 autorización...");
					Thread.sleep(2000);
					
					// Extraer clave de acceso del XML
					String claveAcceso = "";
					try {
						javax.xml.parsers.DocumentBuilder db2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
						org.w3c.dom.Document docXML2 = db2.parse(new java.io.ByteArrayInputStream(xmlCorrecto.getBytes("UTF-8")));
						NodeList claveNodes = docXML2.getElementsByTagName("claveAcceso");
						if (claveNodes.getLength() > 0) {
							claveAcceso = claveNodes.item(0).getTextContent().trim();
						}
					} catch (Exception ex) {
						System.out.println("⚠ No se pudo extraer claveAcceso del XML: " + ex.getMessage());
					}
					System.out.println(">>> PRUEBA: Llamando WS2 autorización con clave: " + claveAcceso);
					System.out.println(">>> PRUEBA: URL WS2: " + urlWS2);
					
					ResultadoAutorizacion resultado = llamarAutorizacionSRI(urlWS2, claveAcceso);
					System.out.println(">>> PRUEBA: Estado WS2 autorización: " + resultado.estado);
					System.out.println(">>> PRUEBA: Número autorización: " + resultado.numeroAutorizacion);
					System.out.println(">>> PRUEBA: Fecha autorización: " + resultado.fechaAutorizacion);
					return "Estado WS1: " + estado + " | Estado WS2: " + resultado.estado 
						+ " | NumAutorizacion: " + resultado.numeroAutorizacion 
						+ " | FechaAutorizacion: " + resultado.fechaAutorizacion;
				}
				
				soapConnection.close();
				return "Estado WS1: " + estado + " | Respuesta completa en logs";
			}
			
			soapConnection.close();
			return "NO SE ENCONTRÓ ESTADO EN LA RESPUESTA | Ver logs para detalles";
			
		} catch (Exception e) {
			System.err.println(">>> PRUEBA ERROR: " + e.getMessage());
			e.printStackTrace();
			throw new Exception("Error en prueba de envío XML: " + e.getMessage(), e);
		}
	}

	// =========================================================================
	// reintentarAutorizacion
	// =========================================================================

	@Override
	public java.util.Map<String, Object> reintentarAutorizacion(Long idFactura) throws Throwable {
		System.out.println("=== reintentarAutorizacion | idFactura=" + idFactura + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// 1. Cargar la factura
		com.saa.model.cxc.Factura factura =
				facturaDaoService.selectById(idFactura, com.saa.model.cxc.NombreEntidadesCobro.FACTURA);
		if (factura == null) {
			resultado.put("mensaje", "No se encontró la factura con ID: " + idFactura);
			return resultado;
		}

		// 2. Validar que tenga clave de acceso
		String clave = factura.getClave();
		if (clave == null || clave.trim().isEmpty()) {
			resultado.put("mensaje", "La factura no tiene clave de acceso. No se puede reintentar la autorización.");
			return resultado;
		}

		// 3. Validar estado: solo se puede reintentar si NO está ya AUTORIZADA (estado=5)
		if (Long.valueOf(5L).equals(factura.getEstado())) {
			resultado.put("exito", true);
			resultado.put("estado", "YA_AUTORIZADA");
			resultado.put("mensaje", "La factura ya está autorizada. Número de autorización: "
					+ nvl(factura.getAutorizacion(), clave));
			resultado.put("numeroAutorizacion", factura.getAutorizacion());
			return resultado;
		}

		// 4. Determinar ambiente
		Long ambiente = 1L; // default pruebas
		if (factura.getFacturador() != null && factura.getFacturador().getAmbiente() != null) {
			ambiente = factura.getFacturador().getAmbiente();
		}

		Long idFacturador = factura.getFacturador().getId();
		String baseUploadDir = getBaseUploadDirectory();
		String resourcesPath = baseUploadDir + "resources/" + idFacturador;

		// 5. Llamar solo al WS2 de autorización
		String urlWS2 = ambiente == 2
				? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
				: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

		System.out.println(">>> Reintentando autorización con clave: " + clave);
		System.out.println(">>> URL WS2: " + urlWS2);

		try {
			ResultadoAutorizacion ra = llamarAutorizacionSRI(urlWS2, clave);

			if ("AUTORIZADO".equals(ra.estado)) {
				// Guardar XML autorizado
				Path pathAutorizado = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
				Files.createDirectories(pathAutorizado.getParent());
				Files.write(pathAutorizado, ra.comprobanteXML.getBytes("UTF-8"));

				// Insertar path autorizado si no existe
				PathFactura pathA = new PathFactura();
				pathA.setFactura(factura);
				pathA.setPath("resources/" + idFacturador + "/docs/a/" + clave + ".xml");
				pathA.setAlterno(5L);
				pathFacturaDaoService.save(pathA, null);

				// Actualizar factura
				factura.setEstado(5L);
				factura.setEstadoEmision(1L);
				factura.setAutorizacion(ra.numeroAutorizacion);
				factura.setFechaAutorizacion(parseFechaAutorizacion(ra.fechaAutorizacion));
				facturaDaoService.save(factura, factura.getId());

				// Generar asiento si corresponde
				if (factura.getFacturador().getEmpresa() != null
						&& Long.valueOf(1L).equals(factura.getFacturador().getGeneraConta())
						&& factura.getAsiento() == null) {
					try {
						Long idEmpresa = factura.getFacturador().getEmpresa().getCodigo();
						String obsAsiento = "Factura N° " + nvl(factura.getNumero(), clave)
								+ " | Cliente: " + factura.getTitular().getNombre()
								+ " | Aut: " + nvl(ra.numeroAutorizacion, clave);
						com.saa.model.cnt.Asiento asientoGenerado =
								asientoContableService.generarAsientoFactura(
										factura.getId(), idEmpresa,
										com.saa.rubros.TipoAsientos.FACTURAS_VENTA,
										factura.getFecha(), obsAsiento, "SISTEMA");
						factura.setAsiento(asientoGenerado);
						facturaDaoService.save(factura, factura.getId());
						resultado.put("asiento", asientoGenerado.getNumeroAlterno());
						System.out.println("✓ Asiento contable generado: " + asientoGenerado.getNumeroAlterno());
					} catch (Exception ae) {
						resultado.put("advertenciaAsiento",
								"Autorizada pero error en asiento contable: " + ae.getMessage());
						System.err.println("⚠ Error en asiento: " + ae.getMessage());
					}
				}

				resultado.put("exito", true);
				resultado.put("estado", "AUTORIZADO");
				resultado.put("numeroAutorizacion", ra.numeroAutorizacion);
				resultado.put("fechaAutorizacion", ra.fechaAutorizacion);
				resultado.put("mensaje", "Factura autorizada correctamente.");
				System.out.println("✓ Factura autorizada en reintento: " + ra.numeroAutorizacion);

			} else {
				resultado.put("exito", false);
				resultado.put("estado", ra.estado != null ? ra.estado : "NO_AUTORIZADO");
				resultado.put("mensaje", "El SRI no autorizó el comprobante. "
						+ "Estado: " + ra.estado
						+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
				resultado.put("respuestaSRI", ra.respuestaCompleta);
				System.out.println("✗ Reintento no autorizado: " + ra.estado);
			}

		} catch (Exception e) {
			resultado.put("mensaje", "Error al comunicarse con el SRI: " + e.getMessage());
			resultado.put("error", e.getMessage());
			System.err.println("✗ Error en reintentarAutorizacion: " + e.getMessage());
			e.printStackTrace();
		}

		return resultado;
	}

	// =========================================================================
	// reenviarEmail
	// =========================================================================

	@Override
	public java.util.Map<String, Object> reenviarEmail(Long idFactura, String destinatarios) throws Throwable {
		System.out.println("=== reenviarEmail | idFactura=" + idFactura
				+ " | destinatarios=" + destinatarios + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// 1. Validar destinatarios
		if (destinatarios == null || destinatarios.trim().isEmpty()) {
			resultado.put("mensaje", "Debe especificar al menos un correo electrónico destinatario.");
			return resultado;
		}

		// 2. Cargar la factura
		com.saa.model.cxc.Factura factura =
				facturaDaoService.selectById(idFactura, com.saa.model.cxc.NombreEntidadesCobro.FACTURA);
		if (factura == null) {
			resultado.put("mensaje", "No se encontró la factura con ID: " + idFactura);
			return resultado;
		}

		// 3. Validar que esté autorizada
		if (!Long.valueOf(5L).equals(factura.getEstado())) {
			resultado.put("mensaje", "Solo se puede reenviar el email de facturas autorizadas. "
					+ "Estado actual de la factura: " + factura.getEstado());
			return resultado;
		}

		String clave        = factura.getClave();
		Long idFacturador   = factura.getFacturador().getId();
		String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;

		// 4. Leer XML autorizado y PDF desde disco
		String xmlAutorizado = null;
		byte[] pdfBytes = null;
		try {
			Path pXml = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
			if (Files.exists(pXml)) {
				xmlAutorizado = new String(Files.readAllBytes(pXml), "UTF-8");
			} else {
				System.err.println("⚠ XML autorizado no encontrado en: " + pXml);
			}
		} catch (Exception e) {
			System.err.println("⚠ Error leyendo XML autorizado: " + e.getMessage());
		}
		try {
			Path pPdf = Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
			if (Files.exists(pPdf)) {
				pdfBytes = Files.readAllBytes(pPdf);
				System.out.println("✓ PDF leído desde disco: " + pPdf);
			} else {
				// PDF no existe en disco (documentos anteriores al fix) → regenerar al vuelo
				System.out.println("ℹ PDF no encontrado en disco. Regenerando PDF para factura: " + clave);
				try {
					pdfBytes = generarPDFFactura(factura, idFacturador, clave, null, factura.getAmbiente());
					if (pdfBytes != null && pdfBytes.length > 0) {
						// Guardar en disco para futuros reenvíos
						Files.createDirectories(pPdf.getParent());
						Files.write(pPdf, pdfBytes);
						System.out.println("✓ PDF regenerado y guardado en disco: " + pPdf);
					}
				} catch (Exception pdfEx) {
					System.err.println("⚠ No se pudo regenerar el PDF: " + pdfEx.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("⚠ Error leyendo/regenerando PDF RIDE: " + e.getMessage());
		}

		// 5. Procesar lista de destinatarios separados por ;
		String[] listaDestinatarios = destinatarios.split(";");
		java.util.List<String> enviados  = new java.util.ArrayList<>();
		java.util.List<String> fallidos  = new java.util.ArrayList<>();
		String razonSocial = factura.getFacturador() != null
				? nvl(factura.getFacturador().getRazonSocial(),
					  nvl(factura.getFacturador().getNombre(), "")) : "";
		String numeroFactura = nvl(factura.getNumero(), clave);

		for (String mail : listaDestinatarios) {
			String mailLimpio = mail.trim();
			if (mailLimpio.isEmpty()) continue;
			try {
				emailFacturaService.enviarFacturaAutorizada(
						mailLimpio, numeroFactura, clave,
						razonSocial, "Factura", xmlAutorizado, pdfBytes);
				enviados.add(mailLimpio);
				System.out.println("✓ Email enviado a: " + mailLimpio);
			} catch (Exception e) {
				fallidos.add(mailLimpio + " (error: " + e.getMessage() + ")");
				System.err.println("✗ Error enviando a " + mailLimpio + ": " + e.getMessage());
			}
		}

		// 6. Construir respuesta
		resultado.put("emailsEnviados", enviados);
		resultado.put("emailsFallidos", fallidos);
		resultado.put("numeroFactura",  numeroFactura);
		resultado.put("clave",          clave);

		if (!enviados.isEmpty() && fallidos.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado correctamente a " + enviados.size()
					+ " destinatario(s): " + String.join(", ", enviados));
		} else if (!enviados.isEmpty()) {
			resultado.put("exito", true);
			resultado.put("mensaje", "Email enviado a " + enviados.size()
					+ " destinatario(s). Fallaron " + fallidos.size() + ": "
					+ String.join(", ", fallidos));
		} else {
			resultado.put("exito", false);
			resultado.put("mensaje", "No se pudo enviar el email a ningún destinatario. "
					+ "Verifique las direcciones de correo y la configuración del servidor.");
		}

		return resultado;
	}

	// =========================================================================
	// anularFactura
	// =========================================================================

	@Override
	public java.util.Map<String, Object> anularFactura(Long idFactura, String motivo, String usuario) throws Throwable {
		System.out.println("=== anularFactura | idFactura=" + idFactura + " | usuario=" + usuario + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// 1. Cargar la factura
		Factura factura = facturaDaoService.selectById(idFactura, NombreEntidadesCobro.FACTURA);
		if (factura == null) {
			resultado.put("mensaje", "Factura con ID " + idFactura + " no encontrada.");
			return resultado;
		}

		// 2. Validar que no esté ya anulada
		if (Long.valueOf(com.saa.rubros.Estado.INACTIVO).equals(factura.getEstado())) {
			resultado.put("mensaje", "La factura ya se encuentra anulada.");
			return resultado;
		}

		// 3. Datos de anulación
		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal      = (motivo  != null && !motivo.trim().isEmpty())  ? motivo.trim()  : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// 3.5. Reversar todos los cobros y abonos aplicados a esta factura
		// (retenciones recibidas, notas, anticipos y transferencias).
		try {
			int reversadas = aplicacionPagoCxcService.revertirAplicacionesDeFactura(
					idFactura, motivoFinal, null);
			if (reversadas > 0) {
				resultado.put("aplicacionesReversadas", reversadas);
				System.out.println("✓ Aplicaciones de cobro reversadas: " + reversadas);
			}
		} catch (Exception e) {
			System.err.println("⚠ Error al reversar las aplicaciones de cobro: " + e.getMessage());
			resultado.put("advertenciaAplicacion",
					"La factura fue anulada pero ocurrió un error al reversar los cobros "
					+ "aplicados: " + e.getMessage());
		}

		// 4. Anular asiento contable vinculado (si existe)
		if (factura.getAsiento() != null && factura.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(
						com.saa.model.cnt.Asiento.class, factura.getAsiento().getCodigo());
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
				System.err.println("⚠ Error al anular asiento contable: " + e.getMessage());
				resultado.put("advertenciaAsiento",
						"La factura fue anulada pero ocurrió un error al anular el asiento: " + e.getMessage());
			}
		}

		// 5. Anular la factura y registrar datos de anulación
		factura.setEstado(Long.valueOf(com.saa.rubros.Estado.INACTIVO));
		factura.setEstadoEmision(3L); // 3 = ANULADA (tsri lsri 603)
		factura.setMotivoAnulacion(motivoFinal);
		factura.setFechaAnulacion(ahora);
		factura.setUsuarioAnulacion(usuarioAnulacion);
		facturaDaoService.save(factura, factura.getId());
		em.flush();

		System.out.println("✓ Factura anulada: " + idFactura
				+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

		resultado.put("exito", true);
		resultado.put("mensaje", "Factura N° " + nvl(factura.getNumero(), String.valueOf(idFactura))
				+ " anulada correctamente.");
		resultado.put("idFactura", idFactura);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);

		return resultado;
	}

	// =========================================================================
	// consultarYActualizarEstadoFactura
	// Consulta estado al SRI y si devuelve AUTORIZADO:
	//   - Pasa la factura a estado 5 (emitida) si estaba pendiente
	//   - Guarda número de autorización y fecha
	//   - Si no tiene asiento contable y el facturador tiene generaConta=1, lo genera
	//   - Envía el email con XML autorizado y PDF (si existe en disco)
	// =========================================================================
	/**
	 * Punto de recuperación: consulta el estado en el SRI y completa lo que haya
	 * quedado pendiente (estado, asiento contable). Sin transacción propia —
	 * cada etapa se confirma por separado.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public java.util.Map<String, Object> consultarYActualizarEstadoFactura(Long idFactura) throws Throwable {
		System.out.println("=== consultarYActualizarEstadoFactura | idFactura=" + idFactura + " ===");
		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		// 1. Cargar la factura
		Factura factura = facturaDaoService.selectById(idFactura, NombreEntidadesCobro.FACTURA);
		if (factura == null) {
			resultado.put("mensaje", "Factura con ID " + idFactura + " no encontrada.");
			return resultado;
		}
		if (factura.getClave() == null || factura.getClave().isEmpty()) {
			resultado.put("mensaje", "La factura no tiene clave de acceso registrada.");
			return resultado;
		}

		Long ambiente = factura.getAmbiente() != null ? factura.getAmbiente() : 1L;
		String clave  = factura.getClave();
		Long idFacturador = factura.getFacturador() != null ? factura.getFacturador().getId() : null;
		resultado.put("clave", clave);
		resultado.put("estadoActual", factura.getEstado());

		// 2. Consultar estado al SRI (WS AutorizacionComprobante)
		String urlWS2 = ambiente == 2L
				? "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl"
				: "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
		System.out.println(">>> Consultando estado al SRI: " + urlWS2);

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
		System.out.println(">>> Estado SRI: " + ra.estado);

		if (!"AUTORIZADO".equals(ra.estado)) {
			resultado.put("mensaje", "El SRI indica que la factura NO está autorizada. Estado: " + ra.estado
					+ " | " + nvl(ra.mensaje, "") + " " + nvl(ra.informacionAdicional, ""));
			return resultado;
		}

		// 3. SRI devuelve AUTORIZADO → actualizar factura (transacción propia)
		boolean actualizada;
		try {
			actualizada = self().marcarFacturaAutorizada(
					idFactura, ra.numeroAutorizacion, ra.fechaAutorizacion, ra.comprobanteXML);
		} catch (Throwable e) {
			resultado.put("mensaje", "El SRI autorizó la factura pero no se pudo actualizar su estado: "
					+ e.getMessage());
			resultado.put("error", e.getMessage());
			System.err.println("⚠ Error actualizando estado de la factura: " + e.getMessage());
			return resultado;
		}
		resultado.put("facturaActualizada", actualizada);

		// 4. Generar asiento contable si no tiene (transacción propia)
		boolean asientoGenerado = false;
		System.out.println("PASO 4: Generando asiento contable...");
		try {
			java.util.Map<String, Object> resAsiento = self().generarContabilidadFactura(idFactura);
			asientoGenerado = Boolean.TRUE.equals(resAsiento.get("generado"));
			if (Boolean.TRUE.equals(resAsiento.get("yaExistia"))) {
				resultado.put("asientoExistente", resAsiento.get("numeroAlterno"));
				System.out.println("ℹ La factura ya tiene asiento contable: " + resAsiento.get("numeroAlterno"));
			} else if (asientoGenerado) {
				resultado.put("asiento", resAsiento.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"Factura autorizada pero error al generar asiento: "
					+ e.getMessage() + ". Genere el asiento manualmente.");
			System.err.println("⚠ Error en asiento contable: " + e.getMessage());
		}
		resultado.put("asientoGenerado", asientoGenerado);

		// 5. Enviar email con XML autorizado y PDF (si existen en disco)
		System.out.println("PASO 5: Enviando email al cliente...");
		String destinatario = null;
		if (factura.getTitular() != null) destinatario = factura.getTitular().getEmail();
		try {
			if (destinatario != null && !destinatario.trim().isEmpty() && idFacturador != null) {
				String resourcesPath = getBaseUploadDirectory() + "resources/" + idFacturador;
				String xmlAutorizado = null;
				byte[] pdfBytes = null;
				try {
					java.nio.file.Path pXml = Paths.get(resourcesPath + "/docs/a/" + clave + ".xml");
					if (Files.exists(pXml))
						xmlAutorizado = new String(Files.readAllBytes(pXml), "UTF-8");
					java.nio.file.Path pPdf = Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf");
					if (Files.exists(pPdf)) {
						pdfBytes = Files.readAllBytes(pPdf);
					} else {
						// PDF no en disco → intentar regenerar
						System.out.println("ℹ PDF no encontrado, regenerando...");
						try {
							pdfBytes = generarPDFFactura(factura, idFacturador, clave, null, ambiente);
							if (pdfBytes != null && pdfBytes.length > 0) {
								Files.createDirectories(Paths.get(resourcesPath + "/docs/a/"));
								Files.write(Paths.get(resourcesPath + "/docs/a/" + clave + ".pdf"), pdfBytes);
								System.out.println("✓ PDF regenerado y guardado.");
							}
						} catch (Exception pdfEx) {
							System.err.println("⚠ No se pudo regenerar el PDF: " + pdfEx.getMessage());
						}
					}
				} catch (Exception ioEx) {
					System.err.println("⚠ Error leyendo archivos para email: " + ioEx.getMessage());
				}
				String razonSocial = factura.getFacturador() != null
						? nvl(factura.getFacturador().getRazonSocial(), nvl(factura.getFacturador().getNombre(), "")) : "";
				emailFacturaService.enviarFacturaAutorizada(
						destinatario, nvl(factura.getNumero(), clave),
						clave, razonSocial, "Factura", xmlAutorizado, pdfBytes);
				resultado.put("emailEnviado", true);
				resultado.put("emailDestinatario", destinatario);
				System.out.println("✓ Email enviado a: " + destinatario);
			} else {
				resultado.put("emailEnviado", false);
				System.out.println("ℹ Email omitido: no hay dirección de correo del cliente.");
			}
		} catch (Exception mailEx) {
			resultado.put("advertenciaEmail",
					"Factura autorizada pero no se pudo enviar el email: "
					+ mailEx.getMessage() + ". Reenvíe el email manualmente.");
			resultado.put("emailEnviado", false);
			System.err.println("⚠ Error enviando email: " + mailEx.getMessage());
		}

		resultado.put("exito", true);
		resultado.put("mensaje", "Factura verificada en el SRI: AUTORIZADA."
				+ (actualizada ? " Estado actualizado a emitida." : "")
				+ (asientoGenerado ? " Asiento contable generado." : "")
				+ (Boolean.TRUE.equals(resultado.get("emailEnviado")) ? " Email enviado a " + destinatario + "." : ""));
		System.out.println("=== consultarYActualizarEstadoFactura COMPLETADO ===");
		return resultado;
	}
}
