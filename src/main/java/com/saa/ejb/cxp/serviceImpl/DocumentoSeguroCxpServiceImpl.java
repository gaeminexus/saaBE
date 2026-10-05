package com.saa.ejb.cxp.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.DocumentoSeguroCxpService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
import com.saa.basico.ejb.FileService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoSeguroDocumentoCxp;
import com.saa.rubros.TipoDocPagoAplicacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Documentos de seguros en CxP (docs/logica-negocio/cxp/API-DOCUMENTOS-SEGUROS-CXP.md).
 * Ver el javadoc de {@link DocumentoSeguroCxpService} para el contrato con crédito (§5.0).
 */
@Stateless
public class DocumentoSeguroCxpServiceImpl implements DocumentoSeguroCxpService {

	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@EJB private NotaDebitoCompraDaoService notaDebitoCompraDaoService;
	@EJB private NotaCreditoCompraDaoService notaCreditoCompraDaoService;
	@EJB private DocumentoCxpDaoService documentoCxpDaoService;
	@EJB private PagoProgramadoDaoService pagoProgramadoDaoService;
	@EJB private AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;
	@EJB private AplicacionPagoCxpService aplicacionPagoCxpService;
	@EJB private ProcesoCargaDocumentosService procesoCargaDocumentosService;
	@EJB private FileService fileService;

	@PersistenceContext
	private EntityManager em;

	// =====================================================================
	// §5.1 — Buscar
	// =====================================================================

	@Override
	public Map<String, Object> porClave(String claveAcceso, Long idEmpresa) throws Throwable {
		System.out.println("=== DocumentoSeguroCxp.porClave | clave=" + claveAcceso
				+ " | empresa=" + idEmpresa + " ===");

		if (claveAcceso == null || claveAcceso.trim().isEmpty()) {
			throw new IncomeException("Debe indicar la clave de acceso.");
		}

		List<FacturaCompra> facturas = facturaCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!facturas.isEmpty()) {
			return construirRespuestaRegistrado("FACTURA", facturas.get(0));
		}
		List<NotaDebitoCompra> nds = notaDebitoCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!nds.isEmpty()) {
			return construirRespuestaRegistrado("NOTA_DEBITO", nds.get(0));
		}
		List<NotaCreditoCompra> ncs = notaCreditoCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!ncs.isEmpty()) {
			return construirRespuestaRegistrado("NOTA_CREDITO", ncs.get(0));
		}

		DocumentoCxp doc = documentoCxpDaoService.selectByClave(claveAcceso);
		if (doc != null) {
			return construirRespuestaPendiente(doc);
		}

		Map<String, Object> r = new HashMap<>();
		r.put("encontrado", false);
		return r;
	}

	// =====================================================================
	// §5.2 — Crear desde el XML, ya marcado (D2)
	// =====================================================================

	@Override
	public Map<String, Object> registrarDesdeXml(String contenidoXml, Long idEmpresa, String usuario,
			Long idDocumentoSeguro) throws Throwable {

		System.out.println("=== DocumentoSeguroCxp.registrarDesdeXml | empresa=" + idEmpresa
				+ " | usuario=" + usuario + " | idDocumentoSeguro=" + idDocumentoSeguro + " ===");

		Usuario usr = resolverUsuario(usuario);

		if (contenidoXml == null || contenidoXml.trim().isEmpty()) {
			throw new IncomeException("Debe enviar el contenido del XML.");
		}
		if (idEmpresa == null) {
			throw new IncomeException("Debe indicar la empresa.");
		}

		Map<String, String> camposXml = procesoCargaDocumentosService.extraerCamposBasicosXml(contenidoXml);
		String claveAcceso = camposXml.get("claveAcceso");
		if (claveAcceso == null || claveAcceso.isEmpty()) {
			throw new IncomeException("No se pudo leer la clave de acceso del XML.");
		}

		// 2. Si ya hay un documento REGISTRADO con esa clave: no lo duplica.
		List<FacturaCompra> facturas = facturaCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!facturas.isEmpty()) {
			Map<String, Object> r = construirRespuestaRegistrado("FACTURA", facturas.get(0));
			r.put("yaExistia", true);
			return r;
		}
		List<NotaDebitoCompra> nds = notaDebitoCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!nds.isEmpty()) {
			Map<String, Object> r = construirRespuestaRegistrado("NOTA_DEBITO", nds.get(0));
			r.put("yaExistia", true);
			return r;
		}
		List<NotaCreditoCompra> ncs = notaCreditoCompraDaoService.selectByClaveEmpresa(claveAcceso, idEmpresa);
		if (!ncs.isEmpty()) {
			Map<String, Object> r = construirRespuestaRegistrado("NOTA_CREDITO", ncs.get(0));
			r.put("yaExistia", true);
			return r;
		}

		// 3. Si está en DCXP sin registrar, usa esa fila. 4. Si no, la crea con los datos del XML.
		DocumentoCxp doc = documentoCxpDaoService.selectByClave(claveAcceso);
		if (doc == null) {
			doc = new DocumentoCxp();
			doc.setEmpresa(em.find(Empresa.class, idEmpresa));
			doc.setClaveAcceso(claveAcceso);
			doc.setRucEmisor(camposXml.get("rucEmisor"));
			doc.setRazonSocialEmisor(camposXml.get("razonSocialEmisor"));
			doc.setTipoComprobante(camposXml.get("tipoComprobante"));
			doc.setSerieComprobante(camposXml.get("serieComprobante"));
			doc.setEstadoDocumento(1L); // LEIDO
			doc = documentoCxpDaoService.save(doc, null);
		}

		// §5.2 trampa: la marca se llena ANTES de intentar el registro -- sobrevive aunque
		// quede pendiente de clasificar productos (D5).
		doc.setEsSeguro(1L);
		doc.setIdDocumentoSeguro(idDocumentoSeguro);
		doc = documentoCxpDaoService.save(doc, doc.getId());

		String nombreArchivo = doc.getClaveAcceso() + ".xml";
		String pathDestino = fileService.uploadFileToPath(
				new java.io.ByteArrayInputStream(contenidoXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				nombreArchivo, "docs/xml/cxp");

		Map<String, Object> resultadoBD = procesoCargaDocumentosService.cargarXmlYRegistrar(
				doc.getId(), contenidoXml, pathDestino, idEmpresa, usr.getCodigo(), true, idDocumentoSeguro);

		if (resultadoBD.containsKey("error")) {
			throw new IncomeException(String.valueOf(resultadoBD.get("mensaje")));
		}

		if (Boolean.TRUE.equals(resultadoBD.get("pendienteClasificacion"))) {
			DocumentoCxp docActualizado = documentoCxpDaoService.selectById(doc.getId(),
					NombreEntidadesCompra.DOCUMENTO_CXP);
			Map<String, Object> r = construirRespuestaPendiente(docActualizado);
			r.put("yaExistia", false);
			r.put("pendienteClasificacion", true);
			r.put("bloqueantes", resultadoBD.get("bloqueantes"));
			r.put("productosPendientes", resultadoBD.get("productosPendientes"));
			return r;
		}

		Long idDocumentoBD = (Long) resultadoBD.get("idDocumentoBD");
		String tipoTablaDestino = (String) resultadoBD.get("tipoTablaDestino");
		Map<String, Object> r = construirRespuestaRegistradoPorTabla(tipoTablaDestino, idDocumentoBD);
		r.put("yaExistia", false);
		return r;
	}

	// =====================================================================
	// §5.3 — Enlazar
	// =====================================================================

	@Override
	public Map<String, Object> enlazar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable {

		System.out.println("=== DocumentoSeguroCxp.enlazar | tipo=" + tipoDocumento + " | documento="
				+ idDocumento + " | idDocumentoSeguro=" + idDocumentoSeguro + " ===");

		resolverUsuario(usuario);
		if (idDocumentoSeguro == null) {
			throw new IncomeException("Debe indicar el documento de seguro (idDocumentoSeguro) a enlazar.");
		}

		Object doc = resolverDocumento(tipoDocumento, idDocumento);
		Long enlaceActual = idDocumentoSeguroDe(doc);
		if (enlaceActual != null && !enlaceActual.equals(idDocumentoSeguro)) {
			throw new IncomeException("El documento ya está enlazado al documento de seguro "
					+ enlaceActual);
		}

		setIdDocumentoSeguro(doc, idDocumentoSeguro);

		String aviso = null;
		long estadoActual = nvlEstado(estadoSeguroDe(doc));
		if (estadoActual == EstadoSeguroDocumentoCxp.NO_ES_SEGURO) {
			if ("FACTURA".equals(tipoDocumento)) {
				FacturaCompra f = (FacturaCompra) doc;
				if (tienePagos(f)) {
					setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.LIBERADO);
					aviso = "La factura ya tenía pagos: queda enlazada pero no se puede bloquear.";
				} else {
					setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
				}
			} else if ("NOTA_DEBITO".equals(tipoDocumento)) {
				NotaDebitoCompra nd = (NotaDebitoCompra) doc;
				if (estaAplicada(nd)) {
					setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.LIBERADO);
					aviso = "La nota de débito ya estaba aplicada a la factura: queda enlazada "
							+ "pero no se puede bloquear.";
				} else {
					setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
				}
			} else {
				setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
			}
		}

		doc = guardar(doc);

		Map<String, Object> r = construirRespuestaRegistrado(tipoDocumento, doc);
		r.put("aviso", aviso);
		return r;
	}

	// =====================================================================
	// §5.4 — Liberar a pago
	// =====================================================================

	@Override
	public Map<String, Object> liberar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable {

		System.out.println("=== DocumentoSeguroCxp.liberar | tipo=" + tipoDocumento + " | documento="
				+ idDocumento + " | idDocumentoSeguro=" + idDocumentoSeguro + " ===");

		Usuario usr = resolverUsuario(usuario);
		Object doc = resolverDocumento(tipoDocumento, idDocumento);
		validarEnlaceYBloqueado(doc, idDocumentoSeguro, idDocumento);

		if ("NOTA_DEBITO".equals(tipoDocumento)) {
			NotaDebitoCompra nd = (NotaDebitoCompra) doc;
			Long idEmpresa = (nd.getEmpresa() != null) ? nd.getEmpresa().getCodigo() : null;
			aplicacionPagoCxpService.aplicarNotaDebito(nd, nd.getAsiento(), idEmpresa, usr.getNombre());
		}

		setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.LIBERADO);
		doc = guardar(doc);

		return construirRespuestaRegistrado(tipoDocumento, doc);
	}

	// =====================================================================
	// §5.5 — Desenlazar y volver a bloquear (D8)
	// =====================================================================

	@Override
	public Map<String, Object> desenlazar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable {

		System.out.println("=== DocumentoSeguroCxp.desenlazar | tipo=" + tipoDocumento + " | documento="
				+ idDocumento + " | idDocumentoSeguro=" + idDocumentoSeguro + " ===");

		Usuario usr = resolverUsuario(usuario);
		Object doc = resolverDocumento(tipoDocumento, idDocumento);

		Long enlaceActual = idDocumentoSeguroDe(doc);
		if (idDocumentoSeguro == null || enlaceActual == null || !enlaceActual.equals(idDocumentoSeguro)) {
			throw new IncomeException("El documento " + idDocumento + " no está enlazado al documento "
					+ "de seguro " + idDocumentoSeguro + ".");
		}

		if ("FACTURA".equals(tipoDocumento)) {
			FacturaCompra f = (FacturaCompra) doc;
			if (tienePagos(f)) {
				throw new IncomeException("La factura ya tiene pagos: no se puede volver a bloquear.");
			}
			setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
			setIdDocumentoSeguro(doc, null);

		} else if ("NOTA_DEBITO".equals(tipoDocumento)) {
			NotaDebitoCompra nd = (NotaDebitoCompra) doc;
			if (nvlEstado(nd.getEstadoSeguro()) == EstadoSeguroDocumentoCxp.LIBERADO) {
				FacturaCompra facturaAfectada = resolverFacturaDeNotaDebito(nd);
				if (facturaAfectada != null && tienePagos(facturaAfectada)) {
					throw new IncomeException("La factura afectada por esta nota de débito ya tiene "
							+ "pagos: no se puede volver a bloquear.");
				}
				aplicacionPagoCxpService.revertirAplicacionesDeDocumento("NOTA_DEBITO", nd.getId(),
						"Desenlace de seguros: vuelve a bloquear", usr.getCodigo());
			}
			setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
			setIdDocumentoSeguro(doc, null);

		} else {
			setEstadoSeguro(doc, EstadoSeguroDocumentoCxp.BLOQUEADO);
			setIdDocumentoSeguro(doc, null);
		}

		doc = guardar(doc);
		return construirRespuestaRegistrado(tipoDocumento, doc);
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	/**
	 * §5.0.2: los métodos @Local reciben el nombre; se resuelve acá, IncomeException si no
	 * existe. Consulta directa con {@code em} propio del bean, NO {@code UsuarioDaoService}: su
	 * {@code NoResultException} (RuntimeException sin @ApplicationException) cruza el límite de
	 * ese otro EJB envuelta en EJBException/TransactionRolledbackLocalException, y un catch acá
	 * nunca la vería.
	 */
	private Usuario resolverUsuario(String usuario) throws Throwable {
		if (usuario == null || usuario.trim().isEmpty()) {
			throw new IncomeException("Debe indicar el usuario.");
		}
		List<Usuario> l = em.createQuery("select u from Usuario u where u.nombre = :nombre", Usuario.class)
				.setParameter("nombre", usuario.trim())
				.getResultList();
		if (l.isEmpty()) {
			throw new IncomeException("No se encontró el usuario '" + usuario + "'.");
		}
		return l.get(0);
	}

	private void validarEnlaceYBloqueado(Object doc, Long idDocumentoSeguro, Long idDocumento) throws Throwable {
		Long enlaceActual = idDocumentoSeguroDe(doc);
		if (idDocumentoSeguro == null || enlaceActual == null || !enlaceActual.equals(idDocumentoSeguro)) {
			throw new IncomeException("El documento " + idDocumento + " no está enlazado al documento "
					+ "de seguro " + idDocumentoSeguro + ".");
		}
		long estado = nvlEstado(estadoSeguroDe(doc));
		if (estado != EstadoSeguroDocumentoCxp.BLOQUEADO) {
			throw new IncomeException("El documento " + idDocumento + " no está BLOQUEADO "
					+ "(estado de seguros actual: " + estado + ").");
		}
	}

	private long nvlEstado(Long estado) {
		return (estado != null) ? estado : EstadoSeguroDocumentoCxp.NO_ES_SEGURO;
	}

	private Object resolverDocumento(String tipoDocumento, Long idDocumento) throws Throwable {
		if ("FACTURA".equals(tipoDocumento)) {
			FacturaCompra f = em.find(FacturaCompra.class, idDocumento);
			if (f == null) throw new IncomeException("No se encontró la factura con ID: " + idDocumento);
			return f;
		}
		if ("NOTA_DEBITO".equals(tipoDocumento)) {
			NotaDebitoCompra n = em.find(NotaDebitoCompra.class, idDocumento);
			if (n == null) throw new IncomeException("No se encontró la nota de débito con ID: " + idDocumento);
			return n;
		}
		if ("NOTA_CREDITO".equals(tipoDocumento)) {
			NotaCreditoCompra n = em.find(NotaCreditoCompra.class, idDocumento);
			if (n == null) throw new IncomeException("No se encontró la nota de crédito con ID: " + idDocumento);
			return n;
		}
		throw new IncomeException("tipoDocumento debe ser FACTURA, NOTA_DEBITO o NOTA_CREDITO.");
	}

	private Long estadoSeguroDe(Object doc) {
		if (doc instanceof FacturaCompra) return ((FacturaCompra) doc).getEstadoSeguro();
		if (doc instanceof NotaDebitoCompra) return ((NotaDebitoCompra) doc).getEstadoSeguro();
		return ((NotaCreditoCompra) doc).getEstadoSeguro();
	}

	private Long idDocumentoSeguroDe(Object doc) {
		if (doc instanceof FacturaCompra) return ((FacturaCompra) doc).getIdDocumentoSeguro();
		if (doc instanceof NotaDebitoCompra) return ((NotaDebitoCompra) doc).getIdDocumentoSeguro();
		return ((NotaCreditoCompra) doc).getIdDocumentoSeguro();
	}

	private void setEstadoSeguro(Object doc, long valor) {
		if (doc instanceof FacturaCompra) { ((FacturaCompra) doc).setEstadoSeguro(valor); return; }
		if (doc instanceof NotaDebitoCompra) { ((NotaDebitoCompra) doc).setEstadoSeguro(valor); return; }
		((NotaCreditoCompra) doc).setEstadoSeguro(valor);
	}

	private void setIdDocumentoSeguro(Object doc, Long valor) {
		if (doc instanceof FacturaCompra) { ((FacturaCompra) doc).setIdDocumentoSeguro(valor); return; }
		if (doc instanceof NotaDebitoCompra) { ((NotaDebitoCompra) doc).setIdDocumentoSeguro(valor); return; }
		((NotaCreditoCompra) doc).setIdDocumentoSeguro(valor);
	}

	private Object guardar(Object doc) throws Throwable {
		if (doc instanceof FacturaCompra) {
			FacturaCompra f = (FacturaCompra) doc;
			return facturaCompraDaoService.save(f, f.getId());
		}
		if (doc instanceof NotaDebitoCompra) {
			NotaDebitoCompra n = (NotaDebitoCompra) doc;
			return notaDebitoCompraDaoService.save(n, n.getId());
		}
		NotaCreditoCompra n = (NotaCreditoCompra) doc;
		return notaCreditoCompraDaoService.save(n, n.getId());
	}

	/** §3: tipo 1 (cobro directo), 4 (anticipo) o 6 (caja chica) activo, o un PagoProgramado vigente. */
	private boolean tienePagos(FacturaCompra factura) throws Throwable {
		List<com.saa.model.cxp.PagoProgramado> vigentes =
				pagoProgramadoDaoService.selectVigentesByFactura(factura.getId());
		if (!vigentes.isEmpty()) return true;

		List<AplicacionPagoCxp> activas = aplicacionPagoCxpDaoService.selectActivasByFactura(factura.getId());
		for (AplicacionPagoCxp a : activas) {
			if (a.getTipoDocPago() == null) continue;
			int t = a.getTipoDocPago().intValue();
			if (t == TipoDocPagoAplicacion.COBRO_DIRECTO || t == TipoDocPagoAplicacion.ANTICIPO
					|| t == TipoDocPagoAplicacion.CAJA_CHICA) {
				return true;
			}
		}
		return false;
	}

	/** Si ya hay una AplicacionPagoCxp activa tipo NOTA_DEBITO para esta ND, ya está aplicada a su factura. */
	private boolean estaAplicada(NotaDebitoCompra nd) throws Throwable {
		return !aplicacionPagoCxpDaoService.selectActivasByDocumento("NOTA_DEBITO", nd.getId()).isEmpty();
	}

	private FacturaCompra resolverFacturaDeNotaDebito(NotaDebitoCompra nd) throws Throwable {
		List<AplicacionPagoCxp> activas = aplicacionPagoCxpDaoService.selectActivasByDocumento("NOTA_DEBITO", nd.getId());
		for (AplicacionPagoCxp a : activas) {
			if (a.getFacturaCompra() != null) return a.getFacturaCompra();
		}
		return null;
	}

	private Map<String, Object> construirRespuestaRegistradoPorTabla(String tipoTablaDestino, Long idDocumentoBD)
			throws Throwable {
		if ("FACTURA_COMPRA".equals(tipoTablaDestino)) {
			return construirRespuestaRegistrado("FACTURA", em.find(FacturaCompra.class, idDocumentoBD));
		}
		if ("NOTA_DEBITO_COMPRA".equals(tipoTablaDestino)) {
			return construirRespuestaRegistrado("NOTA_DEBITO", em.find(NotaDebitoCompra.class, idDocumentoBD));
		}
		if ("NOTA_CREDITO_COMPRA".equals(tipoTablaDestino)) {
			return construirRespuestaRegistrado("NOTA_CREDITO", em.find(NotaCreditoCompra.class, idDocumentoBD));
		}
		throw new IncomeException("El documento se registró en '" + tipoTablaDestino
				+ "', que no es un tipo de seguros (factura, ND o NC de compra).");
	}

	/** Forma de respuesta de §5.1 para un documento YA registrado (FCTC/NTDC/NTCC). */
	private Map<String, Object> construirRespuestaRegistrado(String tipoDocumento, Object doc) throws Throwable {
		Map<String, Object> r = new HashMap<>();
		r.put("encontrado", true);
		r.put("registrado", true);
		r.put("tipoDocumento", tipoDocumento);

		Long id; String numero; Titular titular; Double total;
		java.time.LocalDateTime fechaEmision; Long estadoSeguro; Long idDocumentoSeguro;

		if ("FACTURA".equals(tipoDocumento)) {
			FacturaCompra f = (FacturaCompra) doc;
			id = f.getId(); numero = f.getNumero(); titular = f.getTitular(); total = f.getTotal();
			fechaEmision = f.getFecha(); estadoSeguro = f.getEstadoSeguro();
			idDocumentoSeguro = f.getIdDocumentoSeguro();
		} else if ("NOTA_DEBITO".equals(tipoDocumento)) {
			NotaDebitoCompra n = (NotaDebitoCompra) doc;
			id = n.getId(); numero = n.getNumero(); titular = n.getTitular(); total = n.getTotal();
			fechaEmision = n.getFecha(); estadoSeguro = n.getEstadoSeguro();
			idDocumentoSeguro = n.getIdDocumentoSeguro();
		} else {
			NotaCreditoCompra n = (NotaCreditoCompra) doc;
			id = n.getId(); numero = n.getNumero(); titular = n.getTitular(); total = n.getTotal();
			fechaEmision = n.getFecha(); estadoSeguro = n.getEstadoSeguro();
			idDocumentoSeguro = n.getIdDocumentoSeguro();
		}

		r.put("idDocumento", id);
		r.put("numero", numero);
		r.put("idTitular", (titular != null) ? titular.getCodigo() : null);
		r.put("proveedor", (titular != null) ? titular.getNombre() : null);
		r.put("rucProveedor", (titular != null) ? titular.getIdentificacion() : null);
		r.put("fechaEmision", fechaEmision);
		r.put("total", total);
		r.put("estadoSeguro", estadoSeguro);
		r.put("idDocumentoSeguro", idDocumentoSeguro);
		r.put("estadoDocumentoCxp", null);
		r.put("pendienteClasificacion", false);

		if ("FACTURA".equals(tipoDocumento)) {
			FacturaCompra f = (FacturaCompra) doc;
			r.put("tienePagos", tienePagos(f));
			r.put("saldo", aplicacionPagoCxpService.saldoFactura(f.getId()).get("saldoPendiente"));
		}
		return r;
	}

	/** Forma de respuesta de §5.1 para un documento sin registrar, todavía en la bandeja (PGS.DCXP). */
	private Map<String, Object> construirRespuestaPendiente(DocumentoCxp doc) {
		Map<String, Object> r = new HashMap<>();
		r.put("encontrado", true);
		r.put("registrado", false);
		r.put("estadoDocumentoCxp", doc.getEstadoDocumento());
		boolean pendienteClasificacion = doc.getEstadoDocumento() != null && doc.getEstadoDocumento() == 2L
				&& doc.getObservacion() != null
				&& doc.getObservacion().contains("Productos pendientes de clasificación");
		r.put("pendienteClasificacion", pendienteClasificacion);
		r.put("tipoDocumento", null);
		r.put("idDocumento", null);
		r.put("numero", doc.getSerieComprobante());
		r.put("idTitular", null);
		r.put("proveedor", doc.getRazonSocialEmisor());
		r.put("rucProveedor", doc.getRucEmisor());
		r.put("fechaEmision", doc.getFechaEmision());
		r.put("total", doc.getImporteTotal());
		r.put("estadoSeguro", doc.getEsSeguro());
		r.put("idDocumentoSeguro", doc.getIdDocumentoSeguro());
		return r;
	}

}
