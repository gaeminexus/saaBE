package com.saa.ejb.contabilidad.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.ejb.contabilidad.dao.PlantillaDaoService;
import com.saa.ejb.contabilidad.service.DetallePlantillaService;
import com.saa.ejb.contabilidad.service.PlantillaService;
import com.saa.model.contabilidad.DetallePlantilla;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.Plantilla;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;

@Stateless
public class PlantillaServiceImpl implements PlantillaService{

	@EJB
	private PlantillaDaoService plantillaDaoService;	
	
	@EJB
	private DetallePlantillaService detallePlantillaService;
	
	@EJB
	private DetallePlantillaDaoService detallePlantillaDaoService;
	
	@EJB
	private EmpresaDaoService empresaDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable{
		System.out.println("Ingresa al metodo remove[] de Plantilla service ... depurado");
		//INSTANCIA LA ENTIDAD
		Plantilla plantilla = new Plantilla();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
				plantillaDaoService.remove(plantilla, registro);	
			}				
			
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<Plantilla> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de plantilla service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Plantilla plantilla : lista) {			
			plantillaDaoService.save(plantilla, plantilla.getCodigo());
		}
	}	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectAll()
	 */
	public List<Plantilla> selectAll()throws Throwable {
		System.out.println("Ingresa al metodo selectAll PlantillaService");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Plantilla> result = plantillaDaoService.selectAll(NombreEntidadesContabilidad.PLANTILLA); 
		//INICIALIZA EL OBJETO
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException ("Busqueda total Plantilla no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Plantilla> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Plantilla");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Plantilla> result = plantillaDaoService.selectByCriteria
		(datos, NombreEntidadesContabilidad.PLANTILLA); 
		//PREGUNTA SI ENCONTRO REGISTROS
		if(result.isEmpty()){
			//NO ENCUENTRA REGISTROS
			throw new IncomeException ("Busqueda por criterio de plantilla no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlantillaService#selectById(java.lang.Long)
	 */
	public Plantilla selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return plantillaDaoService.selectById(id, NombreEntidadesContabilidad.PLANTILLA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.service.PlantillaService#codigoByAlterno(int, java.lang.Long)
	 */
	public Long codigoByAlterno(int alterno, Long empresa) throws Throwable {
		System.out.println("Ingresa al codigoByAlterno con alterno: " + alterno + " en empresa: " + empresa);
		Long numero = null;
		List<Plantilla> plantilla = plantillaDaoService.selectByAlterno(alterno, empresa);
		if(!plantilla.isEmpty()){
			for(Plantilla recuperado : plantilla){
				numero = recuperado.getCodigo();
			}
		}
		
		return numero;
	}

	public void actualizaEstado(Long idPlantilla, Long estado) throws Throwable {
		System.out.println("Ingresa al actualizaEstado con id: " + idPlantilla + " con estado: " + estado);
		List<DetallePlantilla> detalles = detallePlantillaDaoService.selectByPlantilla(idPlantilla);
		for(DetallePlantilla detallePlantilla : detalles){
			detallePlantilla.setEstado(estado);
			detallePlantilla.setFechaInactivo(LocalDateTime.now());
			detallePlantillaService.save(detallePlantilla);
		}
		Plantilla plantilla = plantillaDaoService.selectById(idPlantilla, NombreEntidadesContabilidad.PLANTILLA);
		plantilla.setEstado(estado);
		plantilla.setFechaInactivo(LocalDateTime.now());
		try {
			plantillaDaoService.save(plantilla, idPlantilla);
		} catch (EJBException e) {
			throw new Exception("ERROR AL GUARDAR PLANTILLA: "+e.getMessage());
		}
	}

	@Override
	public Plantilla saveSingle(Plantilla plantilla) throws Throwable {
		System.out.println("saveSingle - PlantillaService");
		plantilla = plantillaDaoService.save(plantilla, plantilla.getCodigo());
		return plantilla;
	}

}
	