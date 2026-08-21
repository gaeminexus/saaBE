package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.SaldoAperturaDaoService;
import com.saa.ejb.rhh.service.SaldoAperturaService;
import com.saa.model.rhh.SaldoApertura;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz SaldoAperturaService.
 *  Contiene los servicios relacionados con la entidad SaldoApertura.</p>
 */
@Stateless
public class SaldoAperturaServiceImpl implements SaldoAperturaService {

	@EJB
	private SaldoAperturaDaoService saldoAperturaDaoService;

	@Override
	public void save(List<SaldoApertura> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de saldoApertura service");
		for (SaldoApertura registro : lista) {
			saldoAperturaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de saldoApertura service");
		//INSTANCIA UNA ENTIDAD
		SaldoApertura saldoApertura = new SaldoApertura();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			saldoAperturaDaoService.remove(saldoApertura, registro);
		}
	}

	@Override
	public List<SaldoApertura> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) SaldoApertura");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SaldoApertura> result = saldoAperturaDaoService.selectAll(NombreEntidadesRhh.SALDO_APERTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de saldoApertura no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SaldoApertura selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de saldoApertura con id: " + id);
		return saldoAperturaDaoService.selectById(id, NombreEntidadesRhh.SALDO_APERTURA);
	}

	@Override
	public List<SaldoApertura> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SaldoApertura");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<SaldoApertura> result = saldoAperturaDaoService.selectByCriteria(datos, NombreEntidadesRhh.SALDO_APERTURA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de saldoApertura no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public SaldoApertura saveSingle(SaldoApertura saldoApertura) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) SaldoApertura");
		saldoApertura = saldoAperturaDaoService.save(saldoApertura, saldoApertura.getCodigo());
		return saldoApertura;
	}
}
