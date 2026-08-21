package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService;
import com.saa.ejb.rhh.service.CuentaBancariaEmpleadoService;
import com.saa.model.rhh.CuentaBancariaEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz CuentaBancariaEmpleadoService.
 *  Contiene los servicios relacionados con la entidad CuentaBancariaEmpleado.</p>
 */
@Stateless
public class CuentaBancariaEmpleadoServiceImpl implements CuentaBancariaEmpleadoService {

	@EJB
	private CuentaBancariaEmpleadoDaoService cuentaBancariaEmpleadoDaoService;

	@Override
	public void save(List<CuentaBancariaEmpleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cuentaBancariaEmpleado service");
		for (CuentaBancariaEmpleado registro : lista) {
			cuentaBancariaEmpleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de cuentaBancariaEmpleado service");
		//INSTANCIA UNA ENTIDAD
		CuentaBancariaEmpleado cuentaBancariaEmpleado = new CuentaBancariaEmpleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			cuentaBancariaEmpleadoDaoService.remove(cuentaBancariaEmpleado, registro);
		}
	}

	@Override
	public List<CuentaBancariaEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CuentaBancariaEmpleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancariaEmpleado> result = cuentaBancariaEmpleadoDaoService.selectAll(NombreEntidadesRhh.CUENTA_BANCARIA_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de cuentaBancariaEmpleado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CuentaBancariaEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de cuentaBancariaEmpleado con id: " + id);
		return cuentaBancariaEmpleadoDaoService.selectById(id, NombreEntidadesRhh.CUENTA_BANCARIA_EMPLEADO);
	}

	@Override
	public List<CuentaBancariaEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CuentaBancariaEmpleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CuentaBancariaEmpleado> result = cuentaBancariaEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CUENTA_BANCARIA_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de cuentaBancariaEmpleado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CuentaBancariaEmpleado saveSingle(CuentaBancariaEmpleado cuentaBancariaEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CuentaBancariaEmpleado");
		cuentaBancariaEmpleado = cuentaBancariaEmpleadoDaoService.save(cuentaBancariaEmpleado, cuentaBancariaEmpleado.getCodigo());
		return cuentaBancariaEmpleado;
	}
}
