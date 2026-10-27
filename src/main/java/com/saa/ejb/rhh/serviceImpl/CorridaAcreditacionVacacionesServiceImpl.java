package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.CorridaAcreditacionVacacionesDaoService;
import com.saa.ejb.rhh.service.CorridaAcreditacionVacacionesService;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz CorridaAcreditacionVacacionesService.
 *  Contiene los servicios relacionados con la entidad CorridaAcreditacionVacaciones</p>
 */
@Stateless
public class CorridaAcreditacionVacacionesServiceImpl implements CorridaAcreditacionVacacionesService {

	@EJB
	private CorridaAcreditacionVacacionesDaoService corridaAcreditacionVacacionesDaoService;

	/* (non-Javadoc)
	 * @see com.saa.basico.util.EntityService#save(java.util.List)
	 */
	public void save(List<CorridaAcreditacionVacaciones> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de corridaAcreditacionVacaciones service");
		for (CorridaAcreditacionVacaciones registro : lista) {
			corridaAcreditacionVacacionesDaoService.save(registro, registro.getCodigo());
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.basico.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de corridaAcreditacionVacaciones service");
		CorridaAcreditacionVacaciones corridaAcreditacionVacaciones = new CorridaAcreditacionVacaciones();
		for (Long registro : id) {
			corridaAcreditacionVacacionesDaoService.remove(corridaAcreditacionVacaciones, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.saa.basico.util.EntityService#selectAll()
	 */
	public List<CorridaAcreditacionVacaciones> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) CorridaAcreditacionVacaciones");
		List<CorridaAcreditacionVacaciones> result = corridaAcreditacionVacacionesDaoService
				.selectAll(NombreEntidadesRhh.CORRIDA_ACREDITACION_VACACIONES);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de corridaAcreditacionVacaciones no devolvio ningun registro");
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.CorridaAcreditacionVacacionesService#selectById(java.lang.Long)
	 */
	public CorridaAcreditacionVacaciones selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return corridaAcreditacionVacacionesDaoService.selectById(id,
				NombreEntidadesRhh.CORRIDA_ACREDITACION_VACACIONES);
	}

	/* (non-Javadoc)
	 * @see com.saa.basico.util.EntityService#selectByCriteria(java.util.List)
	 */
	public List<CorridaAcreditacionVacaciones> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) CorridaAcreditacionVacaciones");
		List<CorridaAcreditacionVacaciones> result = corridaAcreditacionVacacionesDaoService
				.selectByCriteria(datos, NombreEntidadesRhh.CORRIDA_ACREDITACION_VACACIONES);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de corridaAcreditacionVacaciones no devolvio ningun registro");
		}
		return result;
	}

	@Override
	public CorridaAcreditacionVacaciones saveSingle(CorridaAcreditacionVacaciones corridaAcreditacionVacaciones)
			throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) CorridaAcreditacionVacaciones");
		return corridaAcreditacionVacacionesDaoService.save(corridaAcreditacionVacaciones,
				corridaAcreditacionVacaciones.getCodigo());
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.service.CorridaAcreditacionVacacionesService#registrarEnTransaccionAparte(com.saa.model.rhh.CorridaAcreditacionVacaciones)
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public CorridaAcreditacionVacaciones registrarEnTransaccionAparte(CorridaAcreditacionVacaciones corrida)
			throws Throwable {
		System.out.println("Ingresa al metodo registrarEnTransaccionAparte de corridaAcreditacionVacaciones, estado: "
				+ corrida.getEstado());
		return corridaAcreditacionVacacionesDaoService.save(corrida, corrida.getCodigo());
	}
}
