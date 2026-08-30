package com.saa.ejb.cxp.serviceImpl;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.service.LiquidacionCompraCompraService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class LiquidacionCompraCompraServiceImpl implements LiquidacionCompraCompraService {
	@EJB private LiquidacionCompraCompraDaoService liquidacionCompraCompraDaoService;
	@EJB private SustentoTributarioService sustentoTributarioService;
	@PersistenceContext private EntityManager em;
	@Override
	public LiquidacionCompraCompra selectById(Long id) throws Throwable {
		return liquidacionCompraCompraDaoService.selectById(id, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		LiquidacionCompraCompra entidad = new LiquidacionCompraCompra();
		for (Long registro : id) { liquidacionCompraCompraDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<LiquidacionCompraCompra> lista) throws Throwable {
		for (LiquidacionCompraCompra registro : lista) { liquidacionCompraCompraDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<LiquidacionCompraCompra> selectAll() throws Throwable {
		List<LiquidacionCompraCompra> result = liquidacionCompraCompraDaoService.selectAll(NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda total LiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}
	@Override
	public LiquidacionCompraCompra saveSingle(LiquidacionCompraCompra entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		LiquidacionCompraCompra resultado = liquidacionCompraCompraDaoService.save(entidad, entidad.getId());
		try {
			sustentoTributarioService.resolverSiFaltaLiquidacion(resultado);
		} catch (Throwable e) {
			System.out.println("ATENCION: fallo la resolucion de codSustento de la liquidacion "
					+ resultado.getId() + ": " + e.getMessage());
		}
		return resultado;
	}
	@Override
	public List<LiquidacionCompraCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<LiquidacionCompraCompra> result = liquidacionCompraCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio LiquidacionCompraCompra no devolvio ningun registro");
		return result;
	}

	@Override
	public java.util.Map<String, Object> anularLiquidacionCompra(Long idLiquidacion, String motivo, String usuario)
			throws Throwable {
		System.out.println("=== anularLiquidacionCompra | id=" + idLiquidacion + " | usuario=" + usuario + " ===");

		java.util.Map<String, Object> resultado = new java.util.HashMap<>();
		resultado.put("exito", false);

		LiquidacionCompraCompra liquidacion = liquidacionCompraCompraDaoService.selectById(
				idLiquidacion, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
		if (liquidacion == null) {
			resultado.put("mensaje", "Liquidación de compra con ID " + idLiquidacion + " no encontrada.");
			return resultado;
		}
		if (Long.valueOf(3L).equals(liquidacion.getEstadoEmision())) {
			resultado.put("mensaje", "La liquidación de compra ya se encuentra anulada.");
			return resultado;
		}

		String usuarioAnulacion = (usuario != null && !usuario.trim().isEmpty()) ? usuario.trim() : "SISTEMA";
		String motivoFinal = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : "Anulación manual";
		java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

		if (liquidacion.getAsiento() != null && liquidacion.getAsiento().getCodigo() != null) {
			try {
				com.saa.model.cnt.Asiento asiento = em.find(
						com.saa.model.cnt.Asiento.class, liquidacion.getAsiento().getCodigo());
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
						"La liquidación de compra fue anulada pero ocurrió un error al anular el asiento: "
						+ e.getMessage());
			}
		}

		liquidacion.setEstado(Long.valueOf(Estado.INACTIVO));
		liquidacion.setEstadoEmision(3L);
		liquidacion.setMotivoAnulacion(motivoFinal);
		liquidacion.setFechaAnulacion(ahora);
		liquidacion.setUsuarioAnulacion(usuarioAnulacion);
		liquidacionCompraCompraDaoService.save(liquidacion, liquidacion.getId());
		em.flush();

		System.out.println("✓ Liquidación de compra anulada: " + idLiquidacion
				+ " | Motivo: " + motivoFinal + " | Usuario: " + usuarioAnulacion);

		resultado.put("exito", true);
		resultado.put("mensaje", "Liquidación de compra N° " + (liquidacion.getNumero() != null
				? liquidacion.getNumero() : String.valueOf(idLiquidacion)) + " anulada correctamente.");
		resultado.put("idLiquidacion", idLiquidacion);
		resultado.put("motivoAnulacion", motivoFinal);
		resultado.put("fechaAnulacion", ahora.toString());
		resultado.put("usuarioAnulacion", usuarioAnulacion);
		return resultado;
	}
}
