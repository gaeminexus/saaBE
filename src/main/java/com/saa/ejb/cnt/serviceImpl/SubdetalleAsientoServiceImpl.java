package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.SubdetalleAsientoDaoService;
import com.saa.ejb.cnt.service.SubdetalleAsientoService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.SubdetalleAsiento;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class SubdetalleAsientoServiceImpl implements SubdetalleAsientoService {

	@EJB
	private SubdetalleAsientoDaoService subdetalleAsientoDaoService;
	
	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de SubdetalleAsiento service");
		SubdetalleAsiento subdetalleAsiento = new SubdetalleAsiento();
		for (Long registro : id) {
			subdetalleAsientoDaoService.remove(subdetalleAsiento, registro);	
		}		
	}	
	
	@Override
	public void save(List<SubdetalleAsiento> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de SubdetalleAsiento service");
		for (SubdetalleAsiento subdetalleAsiento : lista) 		
			subdetalleAsientoDaoService.save(subdetalleAsiento, subdetalleAsiento.getCodigo());
	}	
	
	@Override
	public List<SubdetalleAsiento> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll SubdetalleAsientoService");
		List<SubdetalleAsiento> result = subdetalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.SUBDETALLE_ASIENTO); 
		if(result.isEmpty()){
			throw new IncomeException("Busqueda de SubdetalleAsiento no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public List<SubdetalleAsiento> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) SubdetalleAsiento");
		List<SubdetalleAsiento> result = subdetalleAsientoDaoService.selectByCriteria(datos, NombreEntidadesContabilidad.SUBDETALLE_ASIENTO);
		if(result.isEmpty()){
			throw new IncomeException("Busqueda de SubdetalleAsiento no devolvio ningun registro");
		}
		return result;
	}
	
	@Override
	public SubdetalleAsiento saveSingle(SubdetalleAsiento subdetalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) SubdetalleAsiento Service");
		
		// Calcular valores automáticos si es necesario
		if (subdetalleAsiento.getCostoAdquisicion() != null) {
			// Calcular base a depreciar
			Double baseDepreciar = calcularBaseDepreciar(subdetalleAsiento);
			subdetalleAsiento.setBaseDepreciar(baseDepreciar);
			
			// Calcular valor neto en libros
			Double valorNeto = calcularValorNetoLibros(subdetalleAsiento);
			subdetalleAsiento.setValorNetoLibros(valorNeto);
		}
		
		subdetalleAsientoDaoService.save(subdetalleAsiento, subdetalleAsiento.getCodigo());
		return subdetalleAsiento;
	}
	
	@Override
	public SubdetalleAsiento selectById(Long id) throws Throwable {
		System.out.println("Ingresa al metodo (selectById) de SubdetalleAsiento con id: " + id);
		return subdetalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.SUBDETALLE_ASIENTO);
	}
	
	@Override
	public List<SubdetalleAsiento> selectByIdDetalleAsiento(Long idDetalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo (selectByIdDetalleAsiento) con ID: " + idDetalleAsiento);
		return subdetalleAsientoDaoService.selectByIdDetalleAsiento(idDetalleAsiento);
	}
	
	@Override
	public List<SubdetalleAsiento> selectByCodigoActivo(String codigoActivo) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCodigoActivo) con código: " + codigoActivo);
		return subdetalleAsientoDaoService.selectByCodigoActivo(codigoActivo);
	}
	
	@Override
	public List<SubdetalleAsiento> selectByCategoria(String categoria) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCategoria) con categoría: " + categoria);
		return subdetalleAsientoDaoService.selectByCategoria(categoria);
	}
	
	@Override
	public List<SubdetalleAsiento> selectByResponsable(String responsable) throws Throwable {
		System.out.println("Ingresa al metodo (selectByResponsable) con responsable: " + responsable);
		return subdetalleAsientoDaoService.selectByResponsable(responsable);
	}
	
	@Override
	public Long saveSubdetalle(SubdetalleAsiento subdetalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo (saveSubdetalle) de SubdetalleAsiento");
		
		// Calcular valores automáticos
		if (subdetalleAsiento.getCostoAdquisicion() != null) {
			Double baseDepreciar = calcularBaseDepreciar(subdetalleAsiento);
			subdetalleAsiento.setBaseDepreciar(baseDepreciar);
			
			Double valorNeto = calcularValorNetoLibros(subdetalleAsiento);
			subdetalleAsiento.setValorNetoLibros(valorNeto);
		}
		
		subdetalleAsientoDaoService.save(subdetalleAsiento, subdetalleAsiento.getCodigo());
		return subdetalleAsiento.getCodigo();
	}
	
	@Override
	public Double calcularValorNetoLibros(SubdetalleAsiento subdetalleAsiento) throws Throwable {
		System.out.println("Calculando valor neto en libros");
		
		Double costoAdquisicion = subdetalleAsiento.getCostoAdquisicion() != null ? 
			subdetalleAsiento.getCostoAdquisicion() : 0.0;
		Double mejorasCapitalizadas = subdetalleAsiento.getMejorasCapitalizadas() != null ? 
			subdetalleAsiento.getMejorasCapitalizadas() : 0.0;
		Double depreciacionAcumulada = subdetalleAsiento.getDepreciacionAcumulada() != null ? 
			subdetalleAsiento.getDepreciacionAcumulada() : 0.0;
		
		// Valor Neto = (Costo Adquisición + Mejoras Capitalizadas) - Depreciación Acumulada
		return (costoAdquisicion + mejorasCapitalizadas) - depreciacionAcumulada;
	}
	
	@Override
	public Double calcularBaseDepreciar(SubdetalleAsiento subdetalleAsiento) throws Throwable {
		System.out.println("Calculando base a depreciar");
		
		Double costoAdquisicion = subdetalleAsiento.getCostoAdquisicion() != null ? 
			subdetalleAsiento.getCostoAdquisicion() : 0.0;
		Double mejorasCapitalizadas = subdetalleAsiento.getMejorasCapitalizadas() != null ? 
			subdetalleAsiento.getMejorasCapitalizadas() : 0.0;
		Double valorResidual = subdetalleAsiento.getValorResidual() != null ? 
			subdetalleAsiento.getValorResidual() : 0.0;
		
		// Base a Depreciar = (Costo Adquisición + Mejoras Capitalizadas) - Valor Residual
		return (costoAdquisicion + mejorasCapitalizadas) - valorResidual;
	}
}
