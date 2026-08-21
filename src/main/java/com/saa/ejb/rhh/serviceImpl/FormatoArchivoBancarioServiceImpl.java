package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService;
import com.saa.ejb.rhh.service.FormatoArchivoBancarioService;
import com.saa.model.rhh.FormatoArchivoBancario;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz FormatoArchivoBancarioService.
 *  Contiene los servicios relacionados con la entidad FormatoArchivoBancario.</p>
 */
@Stateless
public class FormatoArchivoBancarioServiceImpl implements FormatoArchivoBancarioService {

	@EJB
	private FormatoArchivoBancarioDaoService formatoArchivoBancarioDaoService;

	@Override
	public void save(List<FormatoArchivoBancario> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de formatoArchivoBancario service");
		for (FormatoArchivoBancario registro : lista) {
			formatoArchivoBancarioDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de formatoArchivoBancario service");
		//INSTANCIA UNA ENTIDAD
		FormatoArchivoBancario formatoArchivoBancario = new FormatoArchivoBancario();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			formatoArchivoBancarioDaoService.remove(formatoArchivoBancario, registro);
		}
	}

	@Override
	public List<FormatoArchivoBancario> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) FormatoArchivoBancario");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FormatoArchivoBancario> result = formatoArchivoBancarioDaoService.selectAll(NombreEntidadesRhh.FORMATO_ARCHIVO_BANCARIO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de formatoArchivoBancario no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public FormatoArchivoBancario selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de formatoArchivoBancario con id: " + id);
		return formatoArchivoBancarioDaoService.selectById(id, NombreEntidadesRhh.FORMATO_ARCHIVO_BANCARIO);
	}

	@Override
	public List<FormatoArchivoBancario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) FormatoArchivoBancario");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FormatoArchivoBancario> result = formatoArchivoBancarioDaoService.selectByCriteria(datos, NombreEntidadesRhh.FORMATO_ARCHIVO_BANCARIO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de formatoArchivoBancario no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public FormatoArchivoBancario saveSingle(FormatoArchivoBancario formatoArchivoBancario) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) FormatoArchivoBancario");
		formatoArchivoBancario = formatoArchivoBancarioDaoService.save(formatoArchivoBancario, formatoArchivoBancario.getCodigo());
		return formatoArchivoBancario;
	}
}
