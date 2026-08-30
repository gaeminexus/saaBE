package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.NotaCreditoCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class NotaCreditoCompraServiceImpl implements NotaCreditoCompraService {
	@EJB private NotaCreditoCompraDaoService notaCreditoCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@EJB private AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;
	@EJB private AplicacionPagoCxpService aplicacionPagoCxpService;
	@PersistenceContext private EntityManager em;
	@Override
	public NotaCreditoCompra selectById(Long id) throws Throwable {
		return notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		NotaCreditoCompra entidad = new NotaCreditoCompra();
		for (Long registro : id) { notaCreditoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<NotaCreditoCompra> lista) throws Throwable {
		for (NotaCreditoCompra registro : lista) { notaCreditoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<NotaCreditoCompra> selectAll() throws Throwable {
		List<NotaCreditoCompra> result = notaCreditoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total NotaCreditoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public NotaCreditoCompra saveSingle(NotaCreditoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		NotaCreditoCompra resultado = notaCreditoCompraDaoService.save(entidad, entidad.getId());
		try {
			sustentoTributarioService.resolverSiFaltaNotaCredito(resultado);
		} catch (Throwable e) {
			System.out.println("ATENCION: fallo la resolucion de codSustento de la nota de credito "
					+ resultado.getId() + ": " + e.getMessage());
		}
		return resultado;
	}
	@Override
	public List<NotaCreditoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<NotaCreditoCompra> result = notaCreditoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio NotaCreditoCompra no devolvio ningun registro");
		return result;
	}

	@Override
	public java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaCredito(Long idNotaCredito)
			throws Throwable {
		System.out.println("=== movimientosRelacionadosNotaCredito | id=" + idNotaCredito + " ===");
		java.util.List<java.util.Map<String, Object>> lista = new java.util.ArrayList<>();
		if (idNotaCredito == null) {
			return lista;
		}
		for (AplicacionPagoCxp aplicacion : aplicacionPagoCxpDaoService.selectActivasByDocumento(
				"NOTA_CREDITO", idNotaCredito)) {
			java.util.Map<String, Object> fila = new java.util.HashMap<>();
			fila.put("idAplicacion", aplicacion.getId());
			fila.put("idFacturaCompra", aplicacion.getFacturaCompra() != null
					? aplicacion.getFacturaCompra().getId() : null);
			fila.put("montoAplicado", aplicacion.getMontoAplicado());
			fila.put("fechaAplicacion", aplicacion.getFechaAplicacion() != null
					? aplicacion.getFechaAplicacion().toString() : null);
			lista.add(fila);
		}
		return lista;
	}

	@Override
	public java.util.Map<String, Object> anularNotaCreditoCompra(Long idNotaCredito, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable {
		System.out.println("=== anularNotaCreditoCompra | id=" + idNotaCredito + " | usuario=" + usuario
				+ " | anularEnCascada=" + anularEnCascada + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		NotaCreditoCompra nota = notaCreditoCompraDaoService.selectById(
				idNotaCredito, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
		if (nota == null) {
			resultado.put("mensaje", "Nota de crédito de compra con ID " + idNotaCredito + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(3L).equals(nota.getEstadoEmision())) {
			resultado.put("mensaje", "La nota de crédito de compra ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// Movimientos relacionados (ítem 13): esta nota puede estar pagando una o más facturas.
		java.util.List<AplicacionPagoCxp> movimientos =
				aplicacionPagoCxpDaoService.selectActivasByDocumento("NOTA_CREDITO", idNotaCredito);
		if (!movimientos.isEmpty()) {
			if (!anularEnCascada) {
				StringBuilder detalle = new StringBuilder();
				for (AplicacionPagoCxp m : movimientos) {
					if (detalle.length() > 0) detalle.append("; ");
					detalle.append("factura ").append(m.getFacturaCompra() != null ? m.getFacturaCompra().getId() : "?")
							.append(" por $").append(m.getMontoAplicado()).append(" (id aplicación ").append(m.getId()).append(")");
				}
				throw new IncomeException("No se puede anular la nota de crédito de compra " + idNotaCredito
						+ ": está pagando " + movimientos.size() + " factura(s) sin reversar: " + detalle
						+ ". Reenvíe la anulación con anularEnCascada=true para reversar esos cruces junto con la nota.");
			}
			int reversados = 0;
			for (AplicacionPagoCxp m : movimientos) {
				aplicacionPagoCxpService.revertirAplicacion(m.getId(),
						"Anulación en cascada de la nota de crédito de compra " + idNotaCredito + ": " + motivoFinal,
						idUsuario);
				reversados++;
			}
			resultado.put("movimientosReversados", reversados);
			System.out.println("✓ " + reversados + " cruce(s) reversados antes de anular la nota de crédito.");
		}

		if (nota.getAsiento() != null && nota.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(
						com.saa.model.cnt.Asiento.class, nota.getAsiento().getCodigo());
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
						"La nota de crédito de compra fue anulada pero ocurrió un error al anular el asiento: "
						+ e.getMessage());
			}
		}

		nota.setEstado(Long.valueOf(Estado.INACTIVO));
		nota.setEstadoEmision(3L);
		nota.setMotivoAnulacion(motivoFinal);
		nota.setFechaAnulacion(ahora);
		nota.setUsuarioAnulacion(usuarioAnulacion);
		notaCreditoCompraDaoService.save(nota, nota.getId());
		em.flush();

		System.out.println("✓ Nota de crédito de compra anulada: " + idNotaCredito
				+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

		resultado.put("exito", true);
		resultado.put("mensaje", "Nota de crédito de compra N° " + (nota.getNumero() != null
				? nota.getNumero() : String.valueOf(idNotaCredito)) + " anulada correctamente.");
		resultado.put("idNotaCredito", idNotaCredito);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}
}
