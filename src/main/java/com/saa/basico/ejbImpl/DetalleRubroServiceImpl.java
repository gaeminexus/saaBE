/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.util.List;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.rubros.Estado;
import com.saa.model.scp.DetalleRubro;
import com.saa.model.scp.NombreEntidadesSistema;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Implementaci�n de la interfaz DetalleRubroService.
 *         Contiene los servicios relacionados con la entidad DetalleRubro.
 *         </p>
 */
@Stateless
public class DetalleRubroServiceImpl implements DetalleRubroService {

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.service.DetalleRubroService#remove(java.
	 * util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de detalleRubro service");
		// VALIDA LA ENTIDAD
		DetalleRubro detalleRubro = new DetalleRubro();
		// ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detalleRubroDaoService.remove(detalleRubro, registro);
		}
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.service.DetalleRubroService#comboModulos()
	 */
	public List<DetalleRubro> comboModulos() throws Throwable {
		System.out.println("Ingresa al comboModulos");		
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleRubro> result = detalleRubroDaoService.selectModulosNoClienteConContabilidad(); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda modulos de uso de cliente no devolvio ningun registro");
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.service.DetalleRubroService#selectAll()
	 */
	public List<DetalleRubro> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) detalleRubro Service");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleRubro> result = detalleRubroDaoService.selectAll(NombreEntidadesSistema.DETALLE_RUBRO);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda completa de detalleRubro no devolvio ningun registro");
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.
	 * Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.service.DetalleRubroService#selectById(java.
	 * lang.Long)
	 */
	public DetalleRubro selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return detalleRubroDaoService.selectById(id, NombreEntidadesSistema.DETALLE_RUBRO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.service.DetalleRubroService#
	 * selectValorStringByRubAltDetAlt(int, int)
	 */
	public String selectValorStringByRubAltDetAlt(int codigoAlternoRubro,
			int codigoAlternoDetalle) throws Throwable {
		System.out.println("Ingresa al selectValorStringByRubAltDetAlt con codigoAlternoRubro = " +
				codigoAlternoRubro + " y codigoAlternoDetalle = " + codigoAlternoDetalle);
		return detalleRubroDaoService.selectValorStringByRubAltDetAlt(codigoAlternoRubro, codigoAlternoDetalle);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.service.DetalleRubroService#
	 * selectByCodigoAlternoRubro(int)
	 */
	public List<DetalleRubro> selectByCodigoAlternoRubro(int codigoAlternoRubro)
			throws Throwable {
		System.out.println("Ingresa al selectByCodigoAlternoRubro con codigoAlternoRubro = " +
				codigoAlternoRubro);
		return detalleRubroDaoService.selectByCodigoAlternoRubro(codigoAlternoRubro, Long.valueOf(Estado.ACTIVO));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.service.DetalleRubroService#
	 * selectCamposByRubroAlterno(java.lang.Object[], int)
	 */
	public List<DetalleRubro> selectCamposByRubroAlterno(Object[] campos,
			int codigoAlternoRubro) throws Throwable {
		System.out.println("Ingresa al selectCamposByRubroAlterno con codigoAlternoRubro = " +
				codigoAlternoRubro);
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleRubro> result = detalleRubroDaoService.selectByCodigoAlternoRubro(codigoAlternoRubro,
				Long.valueOf(Estado.ACTIVO));
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda modulos de uso de cliente no devolvio ningun registro");
			// MENSAJE DE REGISTRO NO ENCONTRADO
			result = null;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityService#selectByCriteria(java.
	 * lang.Object[], java.util.List)
	 */
	public List<DetalleRubro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetalleRubro");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetalleRubro> result = detalleRubroDaoService.selectByCriteria(datos,
				NombreEntidadesSistema.DETALLE_RUBRO);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda por criterio de detalleRubro no devolvio ningun registro");
			// MENSAJE DE REGISTRO NO ENCONTRADO
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public void save(List<DetalleRubro> object) throws Throwable {
		System.out.println("Ingresa al metodo save de detalleRubro service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetalleRubro registro : object) {
			// INSERTA O ACTUALIZA REGISTRO
			detalleRubroDaoService.save(registro);
		}
	}

	@Override
	public DetalleRubro saveSingle(DetalleRubro object) throws Throwable {
		System.out.println("Ingresa al metodo save - codigo de detalleRubro service");
		// INSERTA O ACTUALIZA REGISTRO
		if (object.getCodigo() == 0) {
			object.setCodigo(null);
		}
		object = detalleRubroDaoService.save(object);
		return object;
	}
}
