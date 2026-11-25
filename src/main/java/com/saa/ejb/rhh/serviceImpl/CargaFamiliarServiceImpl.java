package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CargaFamiliarDaoService;
import com.saa.ejb.rhh.service.CargaFamiliarService;
import com.saa.model.rhh.CargaFamiliar;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz CargaFamiliarService.
 *  Contiene los servicios relacionados con la entidad CargaFamiliar.</p>
 */
@Stateless
public class CargaFamiliarServiceImpl implements CargaFamiliarService {

	@EJB
	private CargaFamiliarDaoService cargaFamiliarDaoService;

	@EJB
	private com.saa.ejb.rhh.service.RetencionRentaService retencionRentaService;

	/** VCN #3 (b): las cargas cambian el tope y la rebaja: las proyecciones vigentes del empleado se rehacen. Nunca hace fallar el grabado. */
	private void reproyecta(CargaFamiliar c) {
		try {
			if (c != null && c.getEmpleado() != null) {
				retencionRentaService.reproyectar(c.getEmpleado().getCodigo(), null, c.getUsuarioRegistro());
			}
		} catch (Throwable e) {
			System.out.println("No se pudo reproyectar la renta tras grabar cargas: " + e.getMessage());
		}
	}

	@Override
	public void save(List<CargaFamiliar> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de cargaFamiliar service");
		for (CargaFamiliar registro : lista) {
			cargaFamiliarDaoService.save(registro, registro.getCodigo());
			reproyecta(registro);
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de cargaFamiliar service");
		//INSTANCIA UNA ENTIDAD
		CargaFamiliar cargaFamiliar = new CargaFamiliar();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			CargaFamiliar previo = cargaFamiliarDaoService.selectById(registro, NombreEntidadesRhh.CARGA_FAMILIAR);
			cargaFamiliarDaoService.remove(cargaFamiliar, registro);
			reproyecta(previo);
		}
	}

	@Override
	public List<CargaFamiliar> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CargaFamiliar");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CargaFamiliar> result = cargaFamiliarDaoService.selectAll(NombreEntidadesRhh.CARGA_FAMILIAR);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de cargaFamiliar no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CargaFamiliar selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de cargaFamiliar con id: " + id);
		return cargaFamiliarDaoService.selectById(id, NombreEntidadesRhh.CARGA_FAMILIAR);
	}

	@Override
	public List<CargaFamiliar> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CargaFamiliar");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<CargaFamiliar> result = cargaFamiliarDaoService.selectByCriteria(datos, NombreEntidadesRhh.CARGA_FAMILIAR);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de cargaFamiliar no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public CargaFamiliar saveSingle(CargaFamiliar cargaFamiliar) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CargaFamiliar");
		cargaFamiliar = cargaFamiliarDaoService.save(cargaFamiliar, cargaFamiliar.getCodigo());
		reproyecta(cargaFamiliar);
		return cargaFamiliar;
	}
}
