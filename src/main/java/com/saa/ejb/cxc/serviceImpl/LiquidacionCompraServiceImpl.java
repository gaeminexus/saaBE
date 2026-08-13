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
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== INICIANDO PROCESO COMPLETO DE LIQUIDACION DE COMPRA ===");

		// ── Emisión ante el SRI, en UNA transacción propia ────────────────────
		java.util.Map<String, Object> resultado = self().emitirLiquidacionAnteSRI(
				liquidacion, detalles, ambiente, conectaSRI, destinatario, pathLogo);

		if (!Boolean.TRUE.equals(resultado.get("emitida"))) {
			return resultado;
		}

		Long idLiquidacion = (Long) resultado.get("idLiquidacion");

		// ── PASO 5: Generar asiento contable (transacción propia) ─────────────
		System.out.println("PASO 5: Generando asiento contable para Liquidación de Compra...");
		try {
			java.util.Map<String, Object> resAsiento = self().generarContabilidadLiquidacion(idLiquidacion);
			if (Boolean.TRUE.equals(resAsiento.get("aplica"))) {
				resultado.put("asiento", resAsiento.get("numeroAlterno"));
			}
		} catch (Throwable e) {
			resultado.put("contabilidadPendiente", true);
			resultado.put("advertenciaAsiento",
					"Liquidación autorizada pero ocurrió un error al generar el asiento: "
					+ e.getMessage() + ". Genere el asiento manualmente desde Contabilidad.");
			System.err.println("⚠ Error en asiento contable de Liquidación de Compra: " + e.getMessage());
			e.printStackTrace();
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
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable {
		System.out.println("=== emitirLiquidacionAnteSRI (BD tras RECIBIDA) ===");

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
				// Formas de pago en memoria (vacío para XML, se guarda como default)
				java.util.List<Object> formasPagoMem = new java.util.ArrayList<>();
				String xmlContent = generarXMLContentLiquidacion(liquidacion, dirEstablecimiento,
						new java.util.ArrayList<>(detalles != null ? detalles : java.util.Collections.emptyList()),
						formasPagoMem, ambiente);
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
				liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
				resultado.put("estado", "AUTORIZADO"); resultado.put("exito", true);
				resultado.put("etapa", "COMPLETADO"); resultado.put("mensaje", "Liquidación ya registrada en el SRI. Autorizada.");
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
				liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
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
			// BD. El asiento contable lo genera el orquestador fuera de esta
			// transacción.
			resultado.put("emitida", true);
			resultado.put("estado",  "AUTORIZADO");

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
	 * Genera y vincula el asiento contable de una liquidación de compra en
	 * transacción propia (REQUIRES_NEW). Idempotente: si ya tiene asiento no
	 * genera otro.
	 * @param idLiquidacion : Id de la liquidación ya autorizada
	 * @return : Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public java.util.Map<String, Object> generarContabilidadLiquidacion(Long idLiquidacion) throws Throwable {
		System.out.println("Ingresa al metodo generarContabilidadLiquidacion con id: " + idLiquidacion);

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("generado", false);
		resultado.put("aplica", false);

		LiquidacionCompra liquidacion = em.find(LiquidacionCompra.class, idLiquidacion);
		if (liquidacion == null) {
			throw new IncomeException("Liquidación de Compra con ID " + idLiquidacion + " no encontrada.");
		}
		if (liquidacion.getFacturador() == null
				|| liquidacion.getFacturador().getEmpresa() == null
				|| !Long.valueOf(1L).equals(liquidacion.getFacturador().getGeneraConta())) {
			System.out.println("ℹ El facturador no genera contabilidad: se omite el asiento.");
			return resultado;
		}
		resultado.put("aplica", true);

		if (liquidacion.getAsiento() != null) {
			resultado.put("yaExistia", true);
			resultado.put("idAsiento", liquidacion.getAsiento().getCodigo());
			resultado.put("numeroAlterno", liquidacion.getAsiento().getNumeroAlterno());
			System.out.println("ℹ La liquidación ya tiene asiento: "
					+ liquidacion.getAsiento().getNumeroAlterno());
			return resultado;
		}

		// Etapa atómica: se marca el rollback a mano porque IncomeException es
		// una application exception y por sí sola no reversaría esta transacción.
		try {
			Long idEmpresaConta = liquidacion.getFacturador().getEmpresa().getCodigo();
			java.time.LocalDate fechaAsiento = liquidacion.getFecha() != null
					? liquidacion.getFecha().toLocalDate() : java.time.LocalDate.now();
			String obsAsiento = "Liquidación de Compra N° " + nvl(liquidacion.getNumero(), liquidacion.getClave())
					+ " | Proveedor: " + (liquidacion.getTitular() != null ? liquidacion.getTitular().getNombre() : "")
					+ " | Aut: " + nvl(liquidacion.getAutorizacion(), liquidacion.getClave());
			String usuarioAsiento = liquidacion.getUsuario() != null
					? liquidacion.getUsuario().getNombre() : "SISTEMA";

			com.saa.model.cnt.Asiento asientoGenerado =
					asientoContableService.generarAsientoLiquidacionCompra(
							liquidacion.getId(), idEmpresaConta,
							com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS,
							fechaAsiento, obsAsiento, usuarioAsiento);

			// Vincular el asiento a la liquidación — antes no se hacía, y por
			// eso la anulación no encontraba el asiento que debía anular.
			com.saa.model.cnt.Asiento asientoAttached =
					em.find(com.saa.model.cnt.Asiento.class, asientoGenerado.getCodigo());
			if (asientoAttached == null) asientoAttached = em.merge(asientoGenerado);
			liquidacion.setAsiento(asientoAttached);
			liquidacionCompraDaoService.save(liquidacion, liquidacion.getId());
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
