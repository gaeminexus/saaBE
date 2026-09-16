package com.saa.ejb.cxp.serviceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.CargaArchivoTxtDaoService;
import com.saa.ejb.cxp.service.CargaArchivoTxtService;
import com.saa.model.cxp.CargaArchivoTxt;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
@Stateless
public class CargaArchivoTxtServiceImpl implements CargaArchivoTxtService {
	@EJB private CargaArchivoTxtDaoService cargaArchivoTxtDaoService;
	@PersistenceContext private EntityManager em;
	@Override
	public CargaArchivoTxt selectById(Long id) throws Throwable {
		return cargaArchivoTxtDaoService.selectById(id, NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
	}
	@Override
	public void remove(List<Long> id) throws Throwable {
		CargaArchivoTxt entidad = new CargaArchivoTxt();
		for (Long registro : id) { cargaArchivoTxtDaoService.remove(entidad, registro); }
	}
	@Override
	public void save(List<CargaArchivoTxt> lista) throws Throwable {
		for (CargaArchivoTxt registro : lista) { cargaArchivoTxtDaoService.save(registro, registro.getId()); }
	}
	@Override
	public List<CargaArchivoTxt> selectAll() throws Throwable {
		List<CargaArchivoTxt> result = cargaArchivoTxtDaoService.selectAll(NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda total CargaArchivoTxt no devolvio ningun registro");
		return result;
	}
	@Override
	public CargaArchivoTxt saveSingle(CargaArchivoTxt entidad) throws Throwable {
		if (entidad.getId() == null) entidad.setEstado(Long.valueOf(Estado.ACTIVO));
		return cargaArchivoTxtDaoService.save(entidad, entidad.getId());
	}
	@Override
	public List<CargaArchivoTxt> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		List<CargaArchivoTxt> result = cargaArchivoTxtDaoService.selectByCriteria(datos, NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
		if (result.isEmpty()) throw new IncomeException("Busqueda por criterio CargaArchivoTxt no devolvio ningun registro");
		return result;
	}
	@Override
	@SuppressWarnings("unchecked")
	public List<CargaArchivoTxt> selectByEmpresa(Long idEmpresa) throws Throwable {
		return em.createNamedQuery("CargaArchivoTxtByEmpresa")
				.setParameter("idEmpresa", idEmpresa)
				.getResultList();
	}

	// ÍTEM 25 (2026-09-16, docs/logica-negocio/cxp/API-CARGAS-TXT-BANDEJA-ELECTRONICA.md §2)
	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> buscar(Long idEmpresa, String desde, String hasta, Long idPeriodo,
			String nombreArchivo, Long estado, Integer limite) throws Throwable {

		System.out.println("=== buscar CargaArchivoTxt | idEmpresa=" + idEmpresa + " | desde=" + desde
				+ " | hasta=" + hasta + " | idPeriodo=" + idPeriodo + " | nombreArchivo=" + nombreArchivo
				+ " | estado=" + estado + " | limite=" + limite + " ===");

		if (idEmpresa == null) {
			throw new IncomeException("idEmpresa es obligatorio.");
		}
		int limiteEfectivo = (limite != null && limite > 0) ? Math.min(limite, 500) : 100;

		java.time.LocalDateTime desdeDT = null;
		java.time.LocalDateTime hastaExclusivoDT = null;
		try {
			if (desde != null && !desde.trim().isEmpty()) {
				desdeDT = java.time.LocalDate.parse(desde.trim()).atStartOfDay();
			}
			if (hasta != null && !hasta.trim().isEmpty()) {
				// "hasta" es inclusive del día completo -- CRTXFCGA es fecha CON hora, así que
				// se compara con el límite exclusivo del día siguiente, no con hasta 23:59:59
				// (que se quedaría corto contra un milisegundo tardío).
				hastaExclusivoDT = java.time.LocalDate.parse(hasta.trim()).plusDays(1).atStartOfDay();
			}
		} catch (Exception e) {
			throw new IncomeException("Fecha inválida. Use yyyy-MM-dd. desde='" + desde
					+ "' hasta='" + hasta + "'.");
		}

		StringBuilder jpql = new StringBuilder(
				"select c from CargaArchivoTxt c where c.empresa.codigo = :idEmpresa");
		if (desdeDT != null) {
			jpql.append(" and c.fechaCarga >= :desde");
		}
		if (hastaExclusivoDT != null) {
			jpql.append(" and c.fechaCarga < :hastaExclusivo");
		}
		if (idPeriodo != null) {
			jpql.append(" and c.periodoContable.codigo = :idPeriodo");
		}
		boolean hayNombreArchivo = nombreArchivo != null && !nombreArchivo.trim().isEmpty();
		if (hayNombreArchivo) {
			jpql.append(" and upper(c.nombreArchivo) like :nombreArchivo");
		}
		if (estado != null) {
			jpql.append(" and c.estado = :estado");
		}
		jpql.append(" order by c.fechaCarga desc, c.id desc");

		Query query = em.createQuery(jpql.toString());
		query.setParameter("idEmpresa", idEmpresa);
		if (desdeDT != null) {
			query.setParameter("desde", desdeDT);
		}
		if (hastaExclusivoDT != null) {
			query.setParameter("hastaExclusivo", hastaExclusivoDT);
		}
		if (idPeriodo != null) {
			query.setParameter("idPeriodo", idPeriodo);
		}
		if (hayNombreArchivo) {
			query.setParameter("nombreArchivo", "%" + nombreArchivo.trim().toUpperCase() + "%");
		}
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		query.setMaxResults(limiteEfectivo);

		List<CargaArchivoTxt> cargas = query.getResultList();

		// registrados/pendientes: medido antes de escribir esto -- UNA sola consulta GROUP BY
		// sobre DetalleCargaTxt para TODAS las cargas de esta página (hasta 500 ids en el
		// "in"), no una por fila. Con el límite por defecto (100) son 2 consultas en total,
		// no 101: no hizo falta devolver null y avisar, el costo no lo exige.
		Map<Long, long[]> contadoresPorCarga = new HashMap<>();
		if (!cargas.isEmpty()) {
			List<Long> ids = new ArrayList<>();
			for (CargaArchivoTxt c : cargas) {
				ids.add(c.getId());
			}
			List<Object[]> filas = em.createQuery(
					"select d.cargaTxt.id, count(d.id), "
					+ "sum(case when doc.estadoDocumento = 3L then 1L else 0L end) "
					+ "from DetalleCargaTxt d left join d.documento doc "
					+ "where d.cargaTxt.id in :ids group by d.cargaTxt.id", Object[].class)
					.setParameter("ids", ids)
					.getResultList();
			for (Object[] fila : filas) {
				Long idCarga = (Long) fila[0];
				long total = ((Number) fila[1]).longValue();
				long registrados = (fila[2] != null) ? ((Number) fila[2]).longValue() : 0L;
				contadoresPorCarga.put(idCarga, new long[]{registrados, total - registrados});
			}
		}

		List<Map<String, Object>> resultado = new ArrayList<>();
		for (CargaArchivoTxt c : cargas) {
			Map<String, Object> fila = new LinkedHashMap<>();
			fila.put("idCarga", c.getId());
			fila.put("fechaCarga", c.getFechaCarga() != null ? c.getFechaCarga().toLocalDate().toString() : null);
			fila.put("nombreArchivo", c.getNombreArchivo());
			fila.put("totalLeidos", c.getTotalRegistros());
			fila.put("nuevos", c.getRegistrosNuevos());
			fila.put("duplicados", c.getRegistrosDuplicados());
			fila.put("novedades", c.getRegistrosNovedad());
			fila.put("estado", c.getEstado());
			fila.put("estadoTexto", textoEstadoCarga(c.getEstado()));
			fila.put("usuario", c.getUsuario() != null ? c.getUsuario().getNombre() : null);
			if (c.getPeriodoContable() != null) {
				Map<String, Object> periodo = new LinkedHashMap<>();
				periodo.put("idPeriodo", c.getPeriodoContable().getCodigo());
				periodo.put("nombre", c.getPeriodoContable().getNombre());
				fila.put("periodo", periodo);
			} else {
				fila.put("periodo", null);
			}
			long[] contadores = contadoresPorCarga.getOrDefault(c.getId(), new long[]{0L, 0L});
			fila.put("registrados", contadores[0]);
			fila.put("pendientes", contadores[1]);
			resultado.add(fila);
		}
		return resultado;
	}

	/**
	 * CRTXESTD no tiene rubro/catálogo dedicado (a diferencia de la mayoría de estados de este
	 * sistema): verificado con grep de {@code setEstado} sobre CargaArchivoTxt en TODO el
	 * proyecto -- un solo punto, siempre graba {@code 1L}. No hay más valores que traducir; se
	 * documenta el único observado en vez de inventar un catálogo que no existe.
	 */
	private String textoEstadoCarga(Long estado) {
		if (estado == null) {
			return null;
		}
		return (estado.longValue() == 1L) ? "Activo" : String.valueOf(estado);
	}
}