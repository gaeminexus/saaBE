package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService;
import com.saa.ejb.rhh.service.DetalleFormatoBancarioService;
import com.saa.model.rhh.DetalleFormatoBancario;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz DetalleFormatoBancarioService.
 *  Contiene los servicios relacionados con la entidad DetalleFormatoBancario.</p>
 */
@Stateless
public class DetalleFormatoBancarioServiceImpl implements DetalleFormatoBancarioService {

	@EJB
	private DetalleFormatoBancarioDaoService detalleFormatoBancarioDaoService;

	@Override
	public void save(List<DetalleFormatoBancario> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleFormatoBancario service");
		for (DetalleFormatoBancario registro : lista) {
			detalleFormatoBancarioDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleFormatoBancario service");
		//INSTANCIA UNA ENTIDAD
		DetalleFormatoBancario detalleFormatoBancario = new DetalleFormatoBancario();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleFormatoBancarioDaoService.remove(detalleFormatoBancario, registro);
		}
	}

	@Override
	public List<DetalleFormatoBancario> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) DetalleFormatoBancario");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleFormatoBancario> result = detalleFormatoBancarioDaoService.selectAll(NombreEntidadesRhh.DETALLE_FORMATO_BANCARIO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de detalleFormatoBancario no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleFormatoBancario selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de detalleFormatoBancario con id: " + id);
		return detalleFormatoBancarioDaoService.selectById(id, NombreEntidadesRhh.DETALLE_FORMATO_BANCARIO);
	}

	@Override
	public List<DetalleFormatoBancario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleFormatoBancario");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleFormatoBancario> result = detalleFormatoBancarioDaoService.selectByCriteria(datos, NombreEntidadesRhh.DETALLE_FORMATO_BANCARIO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de detalleFormatoBancario no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public DetalleFormatoBancario saveSingle(DetalleFormatoBancario detalleFormatoBancario) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) DetalleFormatoBancario");
		detalleFormatoBancario = detalleFormatoBancarioDaoService.save(detalleFormatoBancario, detalleFormatoBancario.getCodigo());
		return detalleFormatoBancario;
	}
}
