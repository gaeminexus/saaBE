package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.ejb.contabilidad.dao.PlantillaDaoService;
import com.saa.ejb.contabilidad.service.DetallePlantillaService;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

@Stateless
public class DetallePlantillaServiceImpl implements DetallePlantillaService{

	@EJB
	private DetallePlantillaDaoService detallePlantillaDaoService;	
	
	@EJB
	private PlantillaDaoService plantillaDaoService;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de DetallePlantilla service ... depurado");
		//INSTANCIA LA ENTIDAD
		DetallePlantilla detallePlantilla = new DetallePlantilla();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			detallePlantillaDaoService.remove(detallePlantilla, registro);	
		}		
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<DetallePlantilla> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll DetallePlantillaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetallePlantilla> result = detallePlantillaDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_PLANTILLA); 
		if(result.isEmpty()){
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<DetallePlantilla> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) DetallePlantilla");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<DetallePlantilla> result = detallePlantillaDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.DETALLE_PLANTILLA); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException ("Busqueda por criterio de detallePlantilla no devolvio ningun registro");
			}
	
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetallePlantillaService#save(java.lang.Object[][], java.lang.Object[], java.lang.Long)
	 */
	public void save(List<DetallePlantilla> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de detallePlantilla service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (DetallePlantilla detallePlantilla : lista) {			
			//INSERTA O ACTUALIZA REGISTRO
			detallePlantillaDaoService.save(detallePlantilla, detallePlantilla.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetallePlantillaService#selectById(java.lang.Long)
	 */
	public DetallePlantilla selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return detallePlantillaDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_PLANTILLA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetallePlantillaService#recuperaDetellaForCierre(java.lang.Long)
	 */
	public DetallePlantilla recuperaDetellaForCierre(Long plantilla) throws Throwable {
		System.out.println("Ingresa al recuperaDetellaForCierre con plantilla: " + plantilla);
		//INSTANCIA DATE
		LocalDateTime localDate = LocalDateTime.now();
		DetallePlantilla detalleForCierre = new DetallePlantilla();
		List<DetallePlantilla> detalles = detallePlantillaDaoService.selectByPlantilla(plantilla);
		if(!detalles.isEmpty()){
			for(DetallePlantilla detalle : detalles){
				if (detalle.getFechaHasta() == null){
					detalleForCierre = detalle;	
				}else{
					if(localDate.isBefore(detalle.getFechaHasta())){
						detalleForCierre = detalle;
					}				   	
				}			
			}
		}
		return detalleForCierre;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.DetallePlantillaService#recuperaCuentaContable(java.lang.Long)
	 */
	public PlanCuenta recuperaCuentaContable(Long idDetallePlantilla) throws Throwable {
		System.out.println("Ingresa al recuperaCuentaContable con id: " + idDetallePlantilla);
		return detallePlantillaDaoService.recuperaCuentaContable(idDetallePlantilla);
	}

	public void save(DetallePlantilla detallePlantilla) throws Throwable {
		System.out.println("Ingresa al save con id: " + detallePlantilla.getCodigo());
		try {
			detallePlantillaDaoService.save(detallePlantilla, detallePlantilla.getCodigo());
		} catch (EJBException e) {
			throw new IncomeException("ERROR AL GUARDAR DETALLE DE PLANTILLA: "+e.getMessage());
		}
	}


	@Override
	public DetallePlantilla saveSingle(DetallePlantilla detallePlantilla) throws Throwable {
		System.out.println("saveSingle - DetallePlantillaService");
		detallePlantilla = detallePlantillaDaoService.save(detallePlantilla, detallePlantilla.getCodigo());
		return detallePlantilla;
	}

		
	
}
