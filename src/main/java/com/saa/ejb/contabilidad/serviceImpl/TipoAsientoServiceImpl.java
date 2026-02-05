package com.saa.ejb.contabilidad.serviceImpl;

import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.ejb.Mensaje;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.TipoAsientoDaoService;
import com.saa.ejb.contabilidad.service.TipoAsientoService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.TipoAsiento;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoAsientoServiceImpl implements TipoAsientoService{

	@EJB
	private TipoAsientoDaoService tipoAsientoDaoService;	
	
	@EJB
	private EmpresaService empresaService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TipoAsientoService#selectById(java.lang.Long)
	 */
	public TipoAsiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return tipoAsientoDaoService.selectById(id, NombreEntidadesContabilidad.TIPO_ASIENTO);
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de TipoAsiento service ... depurado");
		//INSTANCIA LA ENTIDAD
		TipoAsiento tipoAsiento = new TipoAsiento();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			tipoAsientoDaoService.remove(tipoAsiento, registro);	
		}	
		
	}	
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<TipoAsiento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de tipoAsiento service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (TipoAsiento tipoAsiento: lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			tipoAsientoDaoService.save(tipoAsiento, tipoAsiento.getCodigo());
		}
	}	
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByWhere(java.util.List)
	 */
	public List<TipoAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo selectByWhere TipoAsientoService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<TipoAsiento> result = tipoAsientoDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.TIPO_ASIENTO); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda de TipoAsiento por criterio no devolvio ningun registro");
			//MENSAJE DE REGISTRO NO ENCONTRADO
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<TipoAsiento> selectAll() throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) TipoAsiento");
	    
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<TipoAsiento> result = tipoAsientoDaoService.selectAll(NombreEntidadesContabilidad.TIPO_ASIENTO);
	    
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if(result.isEmpty()){
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de tipoAsiento no devolvio ningun registro");
	    }
	    return result;
	}
	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TipoAsientoService#validaExisteNaturalezasBasicas(java.lang.Long)
	 */
	public String validaExisteTipoAsiento(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo validaExisteNaturalezasBasicas TipoAsientoService");
		String resultado = Mensaje.OK;
		List<TipoAsiento> tipoAsientos = tipoAsientoDaoService.selectByEmpresaSinAlterno(empresa);
		if (tipoAsientos.isEmpty()) {
			resultado = "NO EXISTEN TIPOS DE ASIENTO INGRESADOS";
		}
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.TipoAsientoService#codigoByAlterno(java.lang.Long, java.lang.Long)
	 */
	public Long codigoByAlterno(int alterno, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo codigoByAlterno con alterno: " + alterno + ", en empresa:" + empresa);
		Long numero = 0L;
		List<TipoAsiento> tipoAsiento = tipoAsientoDaoService.selectByAlterno(alterno, empresa);
		if(tipoAsiento.isEmpty())
			throw new IncomeException("NO EXISTE EL TIPO DE ASIENTO CON CODIGO ALTERNO "+alterno);
		else{
			for(TipoAsiento recuperado : tipoAsiento){
				numero = recuperado.getCodigo();
			}
		}		
		return numero;
	}

	@Override
	public TipoAsiento saveSingle(TipoAsiento tipoAsiento) throws Throwable {
		System.out.println("saveSingle - TipoAsientoServiceImpl");
		tipoAsiento = tipoAsientoDaoService.save(tipoAsiento, tipoAsiento.getCodigo());
		return tipoAsiento;
	}


	@Override
	public Object[][] comboTipoAsientoSelect(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void save(Object[][] object, Object[] campos, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		
	}




	
}
