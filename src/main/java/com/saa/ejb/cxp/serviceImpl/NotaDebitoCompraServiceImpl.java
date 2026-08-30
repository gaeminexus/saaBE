package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.NotaDebitoCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class NotaDebitoCompraServiceImpl implements NotaDebitoCompraService {
	@EJB private NotaDebitoCompraDaoService notaDebitoCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@EJB private AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;
	@EJB private AplicacionPagoCxpService aplicacionPagoCxpService;
	@PersistenceContext private EntityManager em;
	@Override
	public NotaDebitoCompra selectById(Long id) throws Throwable {
		return notaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		NotaDebitoCompra entidad = new NotaDebitoCompra();
		for (Long registro : id) { notaDebitoCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<NotaDebitoCompra> lista) throws Throwable {
		for (NotaDebitoCompra registro : lista) { notaDebitoCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<NotaDebitoCompra> selectAll() throws Throwable {
		List<NotaDebitoCompra> result = notaDebitoCompraDaoService.selectAll(NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total NotaDebitoCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public NotaDebitoCompra saveSingle(NotaDebitoCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		NotaDebitoCompra resultado = notaDebitoCompraDaoService.save(entidad, entidad.getId());
		try {
			sustentoTributarioService.resolverSiFaltaNotaDebito(resultado);
		} catch (Throwable e) {
			System.out.println("ATENCION: fallo la resolucion de codSustento de la nota de debito "
					+ resultado.getId() + ": " + e.getMessage());
		}
		return resultado;
	}
	@Override
	public List<NotaDebitoCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<NotaDebitoCompra> result = notaDebitoCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio NotaDebitoCompra no devolvio ningun registro");
		return result;
	}

	@Override
	public java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaDebito(Long idNotaDebito)
			throws Throwable {
		System.out.println("=== movimientosRelacionadosNotaDebito | id=" + idNotaDebito + " ===");
		java.util.List<java.util.Map<String, Object>> lista = new java.util.ArrayList<>();
		if (idNotaDebito == null) {
			return lista;
		}
		for (AplicacionPagoCxp aplicacion : aplicacionPagoCxpDaoService.selectActivasByDocumento(
				"NOTA_DEBITO", idNotaDebito)) {
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
	public java.util.Map<String, Object> anularNotaDebitoCompra(Long idNotaDebito, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable {
		System.out.println("=== anularNotaDebitoCompra | id=" + idNotaDebito + " | usuario=" + usuario
				+ " | anularEnCascada=" + anularEnCascada + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		NotaDebitoCompra nota = notaDebitoCompraDaoService.selectById(
				idNotaDebito, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
		if (nota == null) {
			resultado.put("mensaje", "Nota de débito de compra con ID " + idNotaDebito + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(3L).equals(nota.getEstadoEmision())) {
			resultado.put("mensaje", "La nota de débito de compra ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		// Movimientos relacionados (ítem 13): esta nota puede estar afectando una o más facturas.
		java.util.List<AplicacionPagoCxp> movimientos =
				aplicacionPagoCxpDaoService.selectActivasByDocumento("NOTA_DEBITO", idNotaDebito);
		if (!movimientos.isEmpty()) {
			if (!anularEnCascada) {
				StringBuilder detalle = new StringBuilder();
				for (AplicacionPagoCxp m : movimientos) {
					if (detalle.length() > 0) detalle.append("; ");
					detalle.append("factura ").append(m.getFacturaCompra() != null ? m.getFacturaCompra().getId() : "?")
							.append(" por $").append(m.getMontoAplicado()).append(" (id aplicación ").append(m.getId()).append(")");
				}
				throw new IncomeException("No se puede anular la nota de débito de compra " + idNotaDebito
						+ ": está afectando " + movimientos.size() + " factura(s) sin reversar: " + detalle
						+ ". Reenvíe la anulación con anularEnCascada=true para reversar esos cruces junto con la nota.");
			}
			int reversados = 0;
			for (AplicacionPagoCxp m : movimientos) {
				aplicacionPagoCxpService.revertirAplicacion(m.getId(),
						"Anulación en cascada de la nota de débito de compra " + idNotaDebito + ": " + motivoFinal,
						idUsuario);
				reversados++;
			}
			resultado.put("movimientosReversados", reversados);
			System.out.println("✓ " + reversados + " cruce(s) reversados antes de anular la nota de débito.");
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
						"La nota de débito de compra fue anulada pero ocurrió un error al anular el asiento: "
						+ e.getMessage());
			}
		}

		nota.setEstado(Long.valueOf(Estado.INACTIVO));
		nota.setEstadoEmision(3L);
		nota.setMotivoAnulacion(motivoFinal);
		nota.setFechaAnulacion(ahora);
		nota.setUsuarioAnulacion(usuarioAnulacion);
		notaDebitoCompraDaoService.save(nota, nota.getId());
		em.flush();

		System.out.println("✓ Nota de débito de compra anulada: " + idNotaDebito
				+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

		resultado.put("exito", true);
		resultado.put("mensaje", "Nota de débito de compra N° " + (nota.getNumero() != null
				? nota.getNumero() : String.valueOf(idNotaDebito)) + " anulada correctamente.");
		resultado.put("idNotaDebito", idNotaDebito);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}
}
