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
