package com.saa.ejb.contabilidad.serviceImpl;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.Stateless;

@Stateless
public class PlanCuentaServiceImpl implements PlanCuentaService{

	@Override
	public void remove(List<Long> id) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void save(List<PlanCuenta> object) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlanCuenta saveSingle(PlanCuenta object) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectAll() throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int numeroRegistrosEmpresa(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String creaNodoArbolCero(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String saveCuenta(Object[][] object, Object[] campos, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long recuperaTipo(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeMovimiento(Long id) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAcumulacion(Long id, boolean actualiza) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String recuperaSiguienteHijo(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long recuperaIdPadre(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void actualizaTipoCuenta(Long id, int tipo) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean validaCopiaPlan(Long empresaDestino) throws Throwable {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String copiaPlanEmpresa(Long empresaOrigen, Long empresaDestino) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> recuperaCuentasHijo(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void actualizaEstadoCuenta(Long id, int tipo) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PlanCuenta buscaMayorCuenta(List<PlanCuenta> listadoCuenta) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(Long id, boolean actualiza) throws Throwable {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String validaExisteCuentasNaturaleza(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlanCuenta selectById(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectByEmpresaManejaCC(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String recuperaMaximaCuenta(Long empresa, String cuentaInicio, String siguienteNaturaleza) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PlanCuenta selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectMovimientoByEmpresaCuentaFecha(Long empresa, Date fechaInicio, Date fechaFin,
			String cuentaInicio, String cuentaFin) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectByEmpresaCuentaFechaCentro(Long empresa, Date fechaInicio, Date fechaFin,
			String cuentaInicio, String cuentaFin, String centroInicio, String centroFin) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double saldoCuentaFechaEmpresa(Long idEmpresa, Long idCuenta, Date fechaInicio) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verificaHijosSinMayorizacin(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verificaHijosSinHistMayorizacin(Long id) throws Throwable {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<PlanCuenta> selectByRangoEmpresaEstado(Long empresa, String cuentaInicio, String cuentaHasta)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> selectMovimientoByRangoEmpresaEstado(Long empresa, String cuentaInicio, String cuentaHasta)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PlanCuenta> recuperaCuentasByRango(Long empresa, String cuentaInicio, String cuentaFin)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean servicioRango(Long empresa, String cuentaInicio, String cuentaFin, String cuentaComparar)
			throws Throwable {
		// TODO Auto-generated method stub
		return false;
	}


}
