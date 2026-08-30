package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.FacturaCompraService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoDocPagoAplicacion;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FacturaCompraServiceImpl implements FacturaCompraService {
	@EJB private FacturaCompraDaoService facturaCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@EJB private AplicacionPagoCxpService aplicacionPagoCxpService;
	@EJB private PagoProgramadoDaoService pagoProgramadoDaoService;
	@EJB private PagoProgramadoService pagoProgramadoService;
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
}
