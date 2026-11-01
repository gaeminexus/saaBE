package com.saa.ejb.cnt.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashSet;

import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.ParametroContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación de {@link ParametroContableService}. Consulta nativa de solo lectura, la misma que usa
 * {@code EstadoFinancieroOficialServiceImpl.parametros()} (SCP.PDTR de SCP.PRBR con PRBRALTR = 250, PDTRESTD = 1).
 */
@Stateless
public class ParametroContableServiceImpl implements ParametroContableService {

	@PersistenceContext
	private EntityManager em;

	@EJB
	private PlantillaService plantillaService;

	@EJB
	private DetallePlantillaDaoService detallePlantillaDaoService;

	@Override
	@SuppressWarnings("unchecked")
	public Map<Integer, String> parametrosEstadosFinancieros() {
		Map<Integer, String> m = new HashMap<Integer, String>();
		Query q = em.createNativeQuery("SELECT d.PDTRALTR, d.PDTRVLRV FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO"
				+ " WHERE r.PRBRALTR = ?1 AND d.PDTRESTD = 1");
		q.setParameter(1, Rubros.CNT_PARAMETROS_ESTADOS_FINANCIEROS);
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			if (f[0] != null && f[1] != null && !f[1].toString().trim().isEmpty()) {
				m.put(((Number) f[0]).intValue(), f[1].toString().trim());
			}
		}
		return m;
	}

	@Override
	public String valor(int alternoDetalle) {
		return parametrosEstadosFinancieros().get(Integer.valueOf(alternoDetalle));
	}

	@Override
	@SuppressWarnings("unchecked")
	public com.saa.ejb.cnt.service.dto.GruposBandaCartera gruposBandaCartera(Long idEmpresa, String prefijoRaiz) {
		com.saa.ejb.cnt.service.dto.GruposBandaCartera grupos = new com.saa.ejb.cnt.service.dto.GruposBandaCartera();
		Query q = em.createNativeQuery(
				" SELECT DISTINCT n.PLNNCNTA, p.TPPRCDGO, t.TPPRNMBR "
				+ " FROM CRD.BNDP b "
				+ " JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO "
				+ " JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO "
				+ " JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO "
				+ " LEFT JOIN CRD.TPPR t ON t.TPPRCDGO = p.TPPRCDGO "
				+ " WHERE b.BNDPESTD = 1 AND c.CBPRESTD = 1 AND n.PJRQCDGO = :empresa AND n.PLNNCNTA LIKE :raiz");
		q.setParameter("empresa", idEmpresa);
		q.setParameter("raiz", prefijoRaiz + "%");
		// cuenta -> tipos de préstamo distintos de los productos que la usan
		Map<String, java.util.Set<Long>> tiposPorCuenta = new java.util.TreeMap<String, java.util.Set<Long>>();
		Map<Long, String> nombreTipo = new HashMap<Long, String>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			String cuenta = f[0] != null ? f[0].toString().trim() : null;
			if (cuenta == null) {
				continue;
			}
			Long tipo = f[1] != null ? Long.valueOf(((Number) f[1]).longValue()) : null;
			tiposPorCuenta.computeIfAbsent(cuenta, k -> new java.util.HashSet<Long>()).add(tipo);
			if (tipo != null && f[2] != null) {
				nombreTipo.put(tipo, f[2].toString().trim());
			}
		}
		// grupo -> cuentas (para el rótulo con la cuenta mínima y máxima leídas)
		Map<String, java.util.TreeSet<String>> cuentasPorGrupo = new HashMap<String, java.util.TreeSet<String>>();
		Map<String, Long> tipoPorGrupo = new HashMap<String, Long>();
		for (Map.Entry<String, java.util.Set<Long>> e : tiposPorCuenta.entrySet()) {
			java.util.Set<Long> tipos = e.getValue();
			String letra = null;
			if (tipos.size() == 1) {
				Long tipo = tipos.iterator().next();
				letra = tipo != null ? letraGrupo(tipo.longValue()) : null;
				if (letra != null) {
					tipoPorGrupo.put(letra, tipo);
				}
			}
			if (letra == null) {
				System.out.println("gruposBandaCartera: la cuenta " + e.getKey() + " queda SIN GRUPO (tipos de préstamo "
						+ tipos + ")");
				continue;
			}
			grupos.asigna(e.getKey(), letra);
			cuentasPorGrupo.computeIfAbsent(letra, k -> new java.util.TreeSet<String>()).add(e.getKey());
		}
		for (Map.Entry<String, java.util.TreeSet<String>> e : cuentasPorGrupo.entrySet()) {
			String nombre = nombreTipo.get(tipoPorGrupo.get(e.getKey()));
			grupos.nombra(e.getKey(), (nombre != null ? nombre : e.getKey()) + " (" + e.getValue().first() + " – "
					+ e.getValue().last() + ")");
		}
		return grupos;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, String> cuentaBandaMayorPorMadre(Long idEmpresa, String prefijoRaiz) {
		Query q = em.createNativeQuery(
				" SELECT c.CBPRCDGO, b.BNDPNMRO, n.PLNNCNTA "
				+ " FROM CRD.BNDP b "
				+ " JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO "
				+ " JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO "
				+ " WHERE b.BNDPESTD = 1 AND c.CBPRESTD = 1 "
				+ " AND (c.CBPRFCFN IS NULL OR c.CBPRFCFN >= TRUNC(SYSDATE)) "
				+ " AND n.PJRQCDGO = :empresa AND n.PLNNCNTA LIKE :raiz");
		q.setParameter("empresa", idEmpresa);
		q.setParameter("raiz", prefijoRaiz + "%");
		// por configuración: la banda de mayor número (y su cuenta)
		Map<Long, Long> numeroMaximo = new HashMap<Long, Long>();
		Map<Long, java.util.Set<String>> cuentasDelMaximo = new HashMap<Long, java.util.Set<String>>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			if (f[0] == null || f[1] == null || f[2] == null) {
				continue;
			}
			Long config = Long.valueOf(((Number) f[0]).longValue());
			long numero = ((Number) f[1]).longValue();
			String cuenta = f[2].toString().trim();
			Long actual = numeroMaximo.get(config);
			if (actual == null || numero > actual.longValue()) {
				numeroMaximo.put(config, Long.valueOf(numero));
				cuentasDelMaximo.put(config, new java.util.HashSet<String>());
				actual = Long.valueOf(numero);
			}
			if (numero == actual.longValue()) {
				cuentasDelMaximo.get(config).add(cuenta);
			}
		}
		// por madre: el conjunto de cuentas de la banda máxima entre las configuraciones
		Map<String, java.util.Set<String>> cuentasPorMadre = new HashMap<String, java.util.Set<String>>();
		for (java.util.Set<String> cuentas : cuentasDelMaximo.values()) {
			for (String cuenta : cuentas) {
				if (cuenta.length() >= 6) {
					cuentasPorMadre.computeIfAbsent(cuenta.substring(0, 6), k -> new java.util.HashSet<String>()).add(cuenta);
				}
			}
		}
		Map<String, String> resultado = new HashMap<String, String>();
		for (Map.Entry<String, java.util.Set<String>> e : cuentasPorMadre.entrySet()) {
			if (e.getValue().size() == 1) {
				resultado.put(e.getKey(), e.getValue().iterator().next());
			} else {
				System.out.println("cuentaBandaMayorPorMadre: la madre " + e.getKey()
						+ " tiene varias cuentas en la banda máxima " + e.getValue() + ": sin cuenta que absorba");
			}
		}
		return resultado;
	}

	/** Letra del grupo de un tipo de préstamo (CRD.TPPR): 1 = Q, 3 = P, 2 = H; null si no tiene. */
	private static String letraGrupo(long tipoPrestamo) {
		if (tipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_QUIROGRAFARIO) {
			return "Q";
		}
		if (tipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_PRENDARIO) {
			return "P";
		}
		if (tipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_HIPOTECARIO) {
			return "H";
		}
		return null;
	}

	@Override
	public List<String> cuentasProvisionCartera(Long idEmpresa) throws Throwable {
		LinkedHashSet<String> cuentas = new LinkedHashSet<String>();
		Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_CARTERA, idEmpresa);
		if (idPlantilla == null || idPlantilla.longValue() == 0L) {
			return new java.util.ArrayList<String>();
		}
		List<DetallePlantilla> lineas = detallePlantillaDaoService.selectByPlantilla(idPlantilla);
		if (lineas != null) {
			for (DetallePlantilla linea : lineas) {
				if (linea.getAuxiliar1() != null && linea.getAuxiliar1().longValue() == CrdLineaAsiento.PROVISION_CARTERA
						&& linea.getPlanCuenta() != null && linea.getPlanCuenta().getCuentaContable() != null
						&& !linea.getPlanCuenta().getCuentaContable().trim().isEmpty()) {
					cuentas.add(linea.getPlanCuenta().getCuentaContable().trim());
				}
			}
		}
		return new java.util.ArrayList<String>(cuentas);
	}
}
