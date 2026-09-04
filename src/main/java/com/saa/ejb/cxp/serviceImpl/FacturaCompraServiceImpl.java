package com.saa.ejb.cxp.serviceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DetalleFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.FormaPagoFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.FacturaCompraService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.ejb.cxp.service.dto.SolicitudNotaVentaCompra;
import com.saa.ejb.cxp.service.dto.SolicitudNotaVentaCompraDetalle;
import com.saa.ejb.cxp.service.dto.SolicitudNotaVentaCompraFormaPago;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.DetalleFacturaCompra;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.FormaPagoFacturaCompra;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoDocPagoAplicacion;
import com.saa.rubros.TipoGrupoProductos;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FacturaCompraServiceImpl implements FacturaCompraService {

	/**
	 * Tipo de comprobante SRI para la nota de venta de compra. ⚠️ SIN CONFIRMAR
	 * contra la tabla de tipos de comprobante del SRI — ver
	 * docs/logica-negocio/cxp/PLAN-NOTA-VENTA-COMPRA-MANUAL.md §1. Constante única:
	 * si contabilidad confirma otro código, cambia en esta sola línea.
	 */
	private static final String TIPO_COMPROBANTE_NOTA_VENTA = "02";

	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@EJB private DetalleFacturaCompraDaoService detalleFacturaCompraDaoService;
	@EJB private FormaPagoFacturaCompraDaoService formaPagoFacturaCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@EJB private AplicacionPagoCxpService aplicacionPagoCxpService;
	@EJB private PagoProgramadoDaoService pagoProgramadoDaoService;
	@EJB private PagoProgramadoService pagoProgramadoService;
	@EJB private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;
	@EJB private com.saa.ejb.cnt.dao.TipoAsientoDaoService tipoAsientoDaoService;
	@PersistenceContext private EntityManager em;
	@Override
	public FacturaCompra selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById FacturaCompra con id: " + id);
		return facturaCompraDaoService.selectById(id, NombreEntidadesCompra.FACTURA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		FacturaCompra entidad = new FacturaCompra();
		for (Long registro : id) { facturaCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<FacturaCompra> lista) throws Throwable {
		for (FacturaCompra registro : lista) { facturaCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<FacturaCompra> selectAll() throws Throwable {
		List<FacturaCompra> result = facturaCompraDaoService.selectAll(NombreEntidadesCompra.FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total FacturaCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public FacturaCompra saveSingle(FacturaCompra entidad) throws Throwable {
		System.out.println("saveSingle - FacturaCompra");
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		FacturaCompra resultado = facturaCompraDaoService.save(entidad, entidad.getId());
		// Camino "manual" de nacimiento de una factura de compra (T3, ver
		// docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md #4.2). En este flujo cabecera y
		// detalle se guardan en llamadas REST separadas (fctc y dfcc), asi que la primera vez
		// que esto corre normalmente no hay lineas todavia y queda sin resolver; se vuelve a
		// intentar solo -sin pisar nada- cada vez que se guarda una linea (ver
		// DetalleFacturaCompraServiceImpl) y en cualquier guardado posterior de la cabecera.
		// Nunca debe poder tumbar el guardado de la factura: se atrapa y se registra.
		try {
			sustentoTributarioService.resolverSiFalta(resultado);
		} catch (Throwable e) {
			System.out.println("ATENCION: fallo la resolucion de codSustento de la factura "
					+ resultado.getId() + ": " + e.getMessage());
		}
		return resultado;
	}
	@Override
	public List<FacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<FacturaCompra> result = facturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.FACTURA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio FacturaCompra no devolvio ningun registro");
		return result;
	}

	@Override
	public java.util.List<java.util.Map<String, Object>> movimientosRelacionadosFactura(Long idFactura)
			throws Throwable {
		System.out.println("=== movimientosRelacionadosFactura | idFactura=" + idFactura + " ===");
		java.util.List<java.util.Map<String, Object>> lista = new java.util.ArrayList<>();
		if (idFactura == null) {
			return lista;
		}
		for (AplicacionPagoCxp aplicacion : aplicacionPagoCxpService.consultarPorFactura(idFactura, true)) {
			java.util.Map<String, Object> fila = new java.util.HashMap<>();
			fila.put("idAplicacion", aplicacion.getId());
			fila.put("tipoDocPago", aplicacion.getTipoDocPago());
			fila.put("tipoDocPagoTexto", textoTipoDocPago(aplicacion.getTipoDocPago()));
			fila.put("montoAplicado", aplicacion.getMontoAplicado());
			fila.put("fechaAplicacion", aplicacion.getFechaAplicacion() != null
					? aplicacion.getFechaAplicacion().toString() : null);
			lista.add(fila);
		}
		return lista;
	}

	private String textoTipoDocPago(Long tipoDocPago) {
		if (tipoDocPago == null) {
			return "Desconocido";
		}
		int tipo = tipoDocPago.intValue();
		if (tipo == TipoDocPagoAplicacion.COBRO_DIRECTO) return "Pago directo";
		if (tipo == TipoDocPagoAplicacion.NOTA_CREDITO) return "Nota de crédito";
		if (tipo == TipoDocPagoAplicacion.RETENCION) return "Retención";
		if (tipo == TipoDocPagoAplicacion.ANTICIPO) return "Anticipo";
		if (tipo == TipoDocPagoAplicacion.NOTA_DEBITO) return "Nota de débito";
		if (tipo == TipoDocPagoAplicacion.CAJA_CHICA) return "Caja chica";
		return "Tipo " + tipoDocPago;
	}

	@Override
	public java.util.Map<String, Object> anularFacturaCompra(Long idFactura, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable {
		System.out.println("=== anularFacturaCompra | idFactura=" + idFactura + " | usuario=" + usuario
				+ " | anularEnCascada=" + anularEnCascada + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		FacturaCompra factura = facturaCompraDaoService.selectById(idFactura, NombreEntidadesCompra.FACTURA_COMPRA);
		if (factura == null) {
			resultado.put("mensaje", "Factura de compra con ID " + idFactura + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(3L).equals(factura.getEstadoEmision())) {
			resultado.put("mensaje", "La factura de compra ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// Movimientos relacionados (2026-08-28, ítem 13): decisión del usuario, no se permite
		// anular una factura con pagos/notas/retenciones/anticipos cruzados salvo que se pida
		// explícitamente reversarlos todos primero.
		java.util.List<AplicacionPagoCxp> movimientos = aplicacionPagoCxpService.consultarPorFactura(idFactura, true);
		if (!movimientos.isEmpty()) {
			if (!anularEnCascada) {
				StringBuilder detalle = new StringBuilder();
				for (AplicacionPagoCxp m : movimientos) {
					if (detalle.length() > 0) detalle.append("; ");
					detalle.append(textoTipoDocPago(m.getTipoDocPago())).append(" $")
							.append(m.getMontoAplicado()).append(" (id ").append(m.getId()).append(")");
				}
				throw new IncomeException("No se puede anular la factura de compra " + idFactura
						+ ": tiene " + movimientos.size() + " movimiento(s) relacionado(s) sin reversar: "
						+ detalle + ". Reverselos uno por uno, o reenvíe la anulación con "
						+ "anularEnCascada=true para reversarlos todos junto con la factura.");
			}
			int reversados = 0;
			for (AplicacionPagoCxp m : movimientos) {
				if (TipoDocPagoAplicacion.COBRO_DIRECTO == m.getTipoDocPago().intValue()) {
					// El pago directo tiene su propio PagoProgramado: hay que reversar por ahí
					// para que su estado quede sincronizado (ver el javadoc del DAO).
					PagoProgramado pago = pagoProgramadoDaoService.selectByAplicacion(m.getId());
					if (pago != null) {
						pagoProgramadoService.revertirPagoConfirmado(pago.getId(),
								"Anulación en cascada de la factura de compra " + idFactura + ": " + motivoFinal,
								idUsuario);
					} else {
						// No debería pasar (todo COBRO_DIRECTO nace de un PagoProgramado), pero
						// si el vínculo se perdió, se reversa la aplicación igual para no dejar
						// la factura bloqueada por un movimiento huérfano.
						aplicacionPagoCxpService.revertirAplicacion(m.getId(),
								"Anulación en cascada de la factura de compra " + idFactura + ": " + motivoFinal,
								idUsuario);
					}
				} else {
					aplicacionPagoCxpService.revertirAplicacion(m.getId(),
							"Anulación en cascada de la factura de compra " + idFactura + ": " + motivoFinal,
							idUsuario);
				}
				reversados++;
			}
			resultado.put("movimientosReversados", reversados);
			System.out.println("✓ " + reversados + " movimiento(s) relacionado(s) reversados antes de anular la factura.");
		}

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
						"La factura de compra fue anulada pero ocurrió un error al anular el asiento: "
						+ e.getMessage());
			}
		}

		factura.setEstado(Long.valueOf(Estado.INACTIVO));
		factura.setEstadoEmision(3L);
		factura.setMotivoAnulacion(motivoFinal);
		factura.setFechaAnulacion(ahora);
		factura.setUsuarioAnulacion(usuarioAnulacion);
		facturaCompraDaoService.save(factura, factura.getId());
		em.flush();

		System.out.println("✓ Factura de compra anulada: " + idFactura
				+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

		resultado.put("exito", true);
		resultado.put("mensaje", "Factura de compra N° " + (factura.getNumero() != null ? factura.getNumero()
				: String.valueOf(idFactura)) + " anulada correctamente.");
		resultado.put("idFactura", idFactura);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}

	// =====================================================================
	// Nota de venta de compra — ingreso manual, sin XML
	// docs/logica-negocio/cxp/PLAN-NOTA-VENTA-COMPRA-MANUAL.md
	// docs/logica-negocio/cxp/API-NOTA-VENTA-COMPRA-MANUAL.md (contrato)
	// =====================================================================

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> registrarNotaVentaManual(SolicitudNotaVentaCompra solicitud) throws Throwable {
		System.out.println("=== registrarNotaVentaManual | idEmpresa="
				+ (solicitud != null ? solicitud.getIdEmpresa() : null)
				+ " | idTitular=" + (solicitud != null ? solicitud.getIdTitular() : null) + " ===");

		// ── Validación estructural: cuerpo inválido -> 400, no bloqueante de negocio ──
		if (solicitud == null)
			throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio.");
		if (solicitud.getIdEmpresa() == null)
			throw new IllegalArgumentException("idEmpresa es obligatorio.");
		if (solicitud.getIdUsuario() == null)
			throw new IllegalArgumentException("idUsuario es obligatorio.");
		if (solicitud.getIdTitular() == null)
			throw new IllegalArgumentException("idTitular es obligatorio.");
		if (esVacio(solicitud.getNumEstablecimiento()))
			throw new IllegalArgumentException("numEstablecimiento es obligatorio.");
		if (esVacio(solicitud.getNumPtoEmision()))
			throw new IllegalArgumentException("numPtoEmision es obligatorio.");
		if (esVacio(solicitud.getSecuencial()))
			throw new IllegalArgumentException("secuencial es obligatorio.");
		if (solicitud.getFecha() == null)
			throw new IllegalArgumentException("fecha es obligatoria.");
		if (solicitud.getSubtotal() == null)
			throw new IllegalArgumentException("subtotal es obligatorio.");
		if (solicitud.getTotal() == null)
			throw new IllegalArgumentException("total es obligatorio.");
		if (solicitud.getDetalles() == null || solicitud.getDetalles().isEmpty())
			throw new IllegalArgumentException("detalles no puede estar vacío.");
		if (solicitud.getFormasPago() != null) {
			for (int i = 0; i < solicitud.getFormasPago().size(); i++) {
				SolicitudNotaVentaCompraFormaPago fp = solicitud.getFormasPago().get(i);
				if (esVacio(fp.getFormaPago()) || fp.getValor() == null)
					throw new IllegalArgumentException(
							"formasPago[" + i + "] necesita formaPago y valor.");
			}
		}

		Empresa empresa = em.find(Empresa.class, solicitud.getIdEmpresa());
		if (empresa == null)
			throw new IllegalArgumentException("No se encontró la empresa con ID: " + solicitud.getIdEmpresa());
		Usuario usuario = em.find(Usuario.class, solicitud.getIdUsuario());
		if (usuario == null)
			throw new IllegalArgumentException("No se encontró el usuario con ID: " + solicitud.getIdUsuario());
		Titular titular = em.find(Titular.class, solicitud.getIdTitular());
		if (titular == null)
			throw new IllegalArgumentException("No se encontró el titular con ID: " + solicitud.getIdTitular());

		// Resolución + validación de cada línea ANTES de grabar nada: un
		// idProducto inexistente o un codigoIVASRI no numérico son cuerpo
		// inválido (400), no un bloqueante de negocio — y no se puede dejar
		// para el medio del bucle que graba, porque ahí ya se grabó la cabecera.
		List<ProductoPago> productos = new ArrayList<>();
		List<Long> codigosIVA = new ArrayList<>();
		for (int i = 0; i < solicitud.getDetalles().size(); i++) {
			SolicitudNotaVentaCompraDetalle d = solicitud.getDetalles().get(i);
			if (d.getIdProducto() == null)
				throw new IllegalArgumentException("detalles[" + i + "].idProducto es obligatorio.");
			if (esVacio(d.getDescripcion()))
				throw new IllegalArgumentException("detalles[" + i + "].descripcion es obligatorio.");
			if (d.getCantidad() == null)
				throw new IllegalArgumentException("detalles[" + i + "].cantidad es obligatorio.");
			if (d.getValor() == null)
				throw new IllegalArgumentException("detalles[" + i + "].valor es obligatorio.");
			if (d.getBaseImponible() == null)
				throw new IllegalArgumentException("detalles[" + i + "].baseImponible es obligatorio.");
			if (d.getTotal() == null)
				throw new IllegalArgumentException("detalles[" + i + "].total es obligatorio.");

			ProductoPago producto = em.find(ProductoPago.class, d.getIdProducto());
			if (producto == null)
				throw new IllegalArgumentException("No se encontró el producto con ID: " + d.getIdProducto()
						+ " (detalles[" + i + "]).");
			productos.add(producto);

			Long codigoIVA = null;
			if (!esVacio(d.getCodigoIVASRI())) {
				try {
					codigoIVA = Long.valueOf(d.getCodigoIVASRI().trim());
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("detalles[" + i + "].codigoIVASRI debe ser numérico: '"
							+ d.getCodigoIVASRI() + "'.");
				}
			}
			codigosIVA.add(codigoIVA);
		}

		// ══════════════════════════════════════════════════════════════════
		// Bloqueantes de negocio — si hay alguno, no se graba nada (contrato §1)
		// ══════════════════════════════════════════════════════════════════
		List<Map<String, Object>> bloqueantes = new ArrayList<>();

		// 2a. Cuenta contable del proveedor, rol PROVEEDOR estricto — igual
		// criterio que ProcesoCargaDocumentosServiceImpl.verificarCuentaContableProveedor,
		// que es privado en esa clase y no se puede reusar desde acá.
		if (!verificarCuentaContableProveedor(titular.getCodigo(), solicitud.getIdEmpresa())) {
			Map<String, Object> b = new HashMap<>();
			b.put("tipo", "PROVEEDOR_SIN_CUENTA");
			b.put("detalle", "El proveedor '" + titular.getNombre() + "' (RUC: " + titular.getIdentificacion()
					+ ") no tiene cuenta contable CxP asignada. Configúrela en Contabilidad → Cuentas por Titular.");
			bloqueantes.add(b);
		}

		// 2b. TipoAsiento de factura de compra configurado, solo si la empresa genera contabilidad
		boolean generaConta = verificarGeneraConta(solicitud.getIdEmpresa());
		if (generaConta && !existeTipoAsientoFacturaCompra(solicitud.getIdEmpresa())) {
			Map<String, Object> b = new HashMap<>();
			b.put("tipo", "TIPO_ASIENTO_FALTANTE");
			b.put("detalle", "No existe el Tipo de Asiento de Facturas de Compra para esta empresa. "
					+ "Configúrelo en Contabilidad → Tipos de Asiento.");
			bloqueantes.add(b);
		}

		// 2c. Cada producto: no POR_CLASIFICAR, y su grupo con cuenta contable —
		// una entrada de bloqueante POR LÍNEA (contrato §2), no agregada como
		// hace la carga automática.
		for (ProductoPago producto : productos) {
			GrupoProductoPago grupo = producto.getGrupoProducto();
			if (grupo == null || (grupo.getRubroTipoGrupoH() != null
					&& grupo.getRubroTipoGrupoH() == TipoGrupoProductos.POR_CLASIFICAR)) {
				Map<String, Object> b = new HashMap<>();
				b.put("tipo", "PRODUCTO_SIN_CLASIFICAR");
				b.put("detalle", "El producto '" + producto.getNombre() + "' está en el grupo POR_CLASIFICAR.");
				bloqueantes.add(b);
			} else if (grupo.getPlanCuenta() == null) {
				Map<String, Object> b = new HashMap<>();
				b.put("tipo", "GRUPO_SIN_CUENTA");
				b.put("detalle", "El grupo '" + grupo.getNombre() + "' del producto '" + producto.getNombre()
						+ "' no tiene cuenta contable asignada.");
				bloqueantes.add(b);
			}
		}

		// 2d. Duplicado: misma nota de venta (establecimiento+ptoEmisión+secuencial)
		// del mismo proveedor. Se excluyen las anuladas (estadoEmision=3): una nota
		// de venta anulada libera su número para volver a ingresarse si hizo falta
		// corregir un error de digitación.
		long duplicados = ((Number) em.createQuery(
				"select count(f) from FacturaCompra f where f.titular.codigo = :idTitular "
				+ "and f.tipoComprobante = :tipo and f.numEstablecimiento = :estab "
				+ "and f.numPtoEmision = :pto and f.secuencial = :sec and f.estadoEmision <> 3")
				.setParameter("idTitular", titular.getCodigo())
				.setParameter("tipo", TIPO_COMPROBANTE_NOTA_VENTA)
				.setParameter("estab", solicitud.getNumEstablecimiento())
				.setParameter("pto", solicitud.getNumPtoEmision())
				.setParameter("sec", solicitud.getSecuencial())
				.getSingleResult()).longValue();
		if (duplicados > 0) {
			Map<String, Object> b = new HashMap<>();
			b.put("tipo", "DOCUMENTO_DUPLICADO");
			b.put("detalle", "Ya existe una nota de venta " + solicitud.getNumEstablecimiento() + "-"
					+ solicitud.getNumPtoEmision() + "-" + solicitud.getSecuencial() + " del proveedor '"
					+ titular.getNombre() + "'.");
			bloqueantes.add(b);
		}

		if (!bloqueantes.isEmpty()) {
			System.out.println("⚠ Registro de nota de venta detenido. Bloqueantes: " + bloqueantes);
			Map<String, Object> r = new HashMap<>();
			r.put("exito", false);
			r.put("bloqueantes", bloqueantes);
			return r;
		}

		// ══════════════════════════════════════════════════════════════════
		// Todo OK -> grabar. NO se recalculan los totales de cabecera desde el
		// detalle (contrato §1): se graba lo que llega, manda el documento físico.
		// ══════════════════════════════════════════════════════════════════
		FacturaCompra factura = new FacturaCompra();
		factura.setEmpresa(empresa);
		factura.setTitular(titular);
		factura.setUsuario(usuario);
		factura.setTipoComprobante(TIPO_COMPROBANTE_NOTA_VENTA);
		factura.setNumEstablecimiento(solicitud.getNumEstablecimiento());
		factura.setNumPtoEmision(solicitud.getNumPtoEmision());
		factura.setSecuencial(solicitud.getSecuencial());
		factura.setNumero(solicitud.getNumEstablecimiento() + "-" + solicitud.getNumPtoEmision()
				+ "-" + solicitud.getSecuencial());
		factura.setAutorizacion(solicitud.getAutorizacion());
		factura.setFecha(solicitud.getFecha());
		factura.setObservacion(solicitud.getObservacion());
		factura.setSubtotal(solicitud.getSubtotal());
		factura.setSubcero(nvlDouble(solicitud.getSubcero()));
		factura.setDescuento(nvlDouble(solicitud.getDescuento()));
		factura.setpIVA(nvlDouble(solicitud.getpIVA()));
		factura.setvIVA(nvlDouble(solicitud.getvIVA()));
		factura.setTotal(solicitud.getTotal());
		factura.setEstado(Long.valueOf(Estado.ACTIVO));
		factura.setEstadoEmision(2L);
		// clave, ambiente, pathGen y fechaAutorizacion quedan NULL a propósito:
		// no hay XML detrás de una nota de venta (§1 del plan).
		factura = facturaCompraDaoService.save(factura, null);

		for (int i = 0; i < solicitud.getDetalles().size(); i++) {
			SolicitudNotaVentaCompraDetalle d = solicitud.getDetalles().get(i);
			DetalleFacturaCompra df = new DetalleFacturaCompra();
			df.setFactura(factura);
			df.setDescripcion(d.getDescripcion());
			df.setCantidad(d.getCantidad());
			df.setValor(d.getValor());
			df.setSubTotal(d.getBaseImponible());
			df.setDescuento(nvlDouble(d.getDescuento()));
			df.setBaseImponible(d.getBaseImponible());
			df.setPorcentajeIVA(d.getPorcentajeIVA() != null ? Long.valueOf(d.getPorcentajeIVA().longValue()) : null);
			df.setValorIVA(nvlDouble(d.getValorIVA()));
			df.setCodigoIVASRI(codigosIVA.get(i));
			df.setTotal(d.getTotal());
			df.setProducto(productos.get(i).getId());
			df.setEstado(Long.valueOf(Estado.ACTIVO));
			detalleFacturaCompraDaoService.save(df, null);
		}

		if (solicitud.getFormasPago() != null) {
			for (SolicitudNotaVentaCompraFormaPago fpSol : solicitud.getFormasPago()) {
				FormaPagoFacturaCompra fp = new FormaPagoFacturaCompra();
				fp.setFactura(factura);
				fp.setFormaPago(fpSol.getFormaPago());
				fp.setValor(fpSol.getValor());
				fp.setPlazo(fpSol.getPlazo() != null ? fpSol.getPlazo() : Long.valueOf(0L));
				fp.setUnidadTiempo(fpSol.getUnidadTiempo());
				formaPagoFacturaCompraDaoService.save(fp, null);
			}
		}

		// Sustento tributario (ATS, FCTCCSUS) — igual que la carga automática: no
		// bloquea el registro si falla, la nota de venta queda pendiente de corregir
		// con PUT /rest/fctc/sustento/{id}.
		String sustento = null;
		try {
			sustento = sustentoTributarioService.resolverSiFalta(factura);
		} catch (Throwable e) {
			System.out.println("ATENCION: fallo la resolucion de codSustento de la nota de venta "
					+ factura.getId() + ": " + e.getMessage());
		}

		// Asiento contable — mismo generador que la factura de compra electrónica.
		// Sin asiento si generaConta=0: no es error (contrato §1).
		com.saa.model.cnt.Asiento asiento = null;
		if (generaConta) {
			asiento = asientoContableService.generarAsientoFacturaCompra(
					factura.getId(), solicitud.getIdEmpresa(),
					com.saa.rubros.TipoAsientos.FACTURAS_COMPRA,
					factura.getFecha() != null ? factura.getFecha().toLocalDate() : java.time.LocalDate.now(),
					"Nota de venta compra: " + factura.getNumero(),
					usuario.getNombre() != null ? usuario.getNombre() : "SISTEMA");
			factura.setAsiento(asiento);
			factura = facturaCompraDaoService.save(factura, factura.getId());
		}

		Map<String, Object> r = new HashMap<>();
		r.put("exito", true);
		r.put("idFactura", factura.getId());
		r.put("numero", factura.getNumero());
		r.put("asiento", asiento != null ? asiento.getNumeroAlterno() : null);
		r.put("sustento", sustento);
		r.put("mensaje", "Nota de venta registrada correctamente.");

		System.out.println("✓ Nota de venta de compra registrada: id=" + factura.getId()
				+ " | numero=" + factura.getNumero()
				+ " | asiento=" + (asiento != null ? asiento.getNumeroAlterno() : "null"));
		return r;
	}

	/**
	 * Verifica si el titular tiene cuenta contable CxP (tipoCuenta=1) EN SU ROL DE
	 * PROVEEDOR ESTRICTO. Mismo criterio que
	 * {@code ProcesoCargaDocumentosServiceImpl.verificarCuentaContableProveedor}
	 * (privado en esa clase, no reusable desde acá): usa
	 * {@code existeCuentaConRolEstricto} para no caer en el fallback "sin filtro de
	 * rol" que devolvía la cuenta de CLIENTE cuando faltaba la de PROVEEDOR.
	 */
	private boolean verificarCuentaContableProveedor(Long codigoTitular, Long idEmpresa) {
		try {
			return asientoContableService.existeCuentaConRolEstricto(
					codigoTitular, idEmpresa, 1L, RolPersona.PROVEEDOR);
		} catch (Throwable e) {
			System.err.println("⚠ verificarCuentaContableProveedor: " + e.getMessage());
			return false;
		}
	}

	/** Verifica si la empresa tiene generación contable habilitada (Facturador.generaConta=1). */
	private boolean verificarGeneraConta(Long idEmpresa) {
		try {
			@SuppressWarnings("unchecked")
			List<Long> lista = em.createQuery(
					"SELECT f.generaConta FROM Facturador f WHERE f.empresa.codigo = :idEmpresa AND f.estado = 1")
					.setParameter("idEmpresa", idEmpresa)
					.setMaxResults(1).getResultList();
			return !lista.isEmpty() && Long.valueOf(1L).equals(lista.get(0));
		} catch (Exception e) {
			System.err.println("⚠ verificarGeneraConta: " + e.getMessage());
			return false;
		}
	}

	/** ¿Existe la plantilla de asiento de Facturas de Compra para esta empresa? */
	private boolean existeTipoAsientoFacturaCompra(Long idEmpresa) {
		try {
			List<com.saa.model.cnt.TipoAsiento> tipos = tipoAsientoDaoService.selectByAlterno(
					com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, idEmpresa);
			return tipos != null && !tipos.isEmpty();
		} catch (Throwable e) {
			System.err.println("⚠ existeTipoAsientoFacturaCompra (empresa=" + idEmpresa + "): " + e.getMessage());
			return false;
		}
	}

	private boolean esVacio(String valor) {
		return valor == null || valor.trim().isEmpty();
	}

	private double nvlDouble(Double valor) {
		return (valor != null) ? valor : 0.0;
	}
}
