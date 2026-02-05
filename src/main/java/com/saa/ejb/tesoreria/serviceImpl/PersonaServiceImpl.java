/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.PersonaDaoService;
import com.saa.ejb.tesoreria.service.PersonaService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.Persona;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PersonaService.
 *  Contiene los servicios relacionados con la entidad Persona.</p>
 */
@Stateless
public class PersonaServiceImpl implements PersonaService {
	
	@EJB
	private PersonaDaoService personaDaoService;
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de Persona service ... depurado");
		//INSTANCIA LA ENTIDAD
		Persona persona = new Persona();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			personaDaoService.remove(persona, registro);
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<Persona> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de Persona service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Persona persona : lista) {			
			personaDaoService.save(persona, persona.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<Persona> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PersonaService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Persona> result = personaDaoService.selectAll(NombreEntidadesTesoreria.PERSONA);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total Persona no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}
 
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<Persona> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) Persona");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<Persona> result = personaDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.PERSONA
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de Persona no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PersonaService#selectById(java.lang.Long)
	 */
	public Persona selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return personaDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PersonaService#validaIdentificacion(java.lang.String)
	 */
	public int validaIdentificacion(String identificacion) throws Throwable {
		System.out.println("Ingresa al metodo validaIdentificacion con identificacion: " + identificacion);
		int resultado = 0;
		int i = 0;
		int numero = 0;
		int suma = 0;
		int factor,digito,id,dimension;
		if(isNumerico(identificacion)){
			dimension = identificacion.trim().length();
			id = Integer.parseInt(identificacion.substring(2,3));
			if(id == 9){//RUC para Sociedades Privadas y Extranjeros sin Cédula
				if(dimension == 13){
					factor = 4;
					while(i != 9){
						numero = Integer.parseInt(identificacion.substring(i,i+1)) * factor;
						suma += numero;
						if(factor == 2)
							factor = 7;
						else
							factor--;
						i++;
					}
					numero = Integer.parseInt(identificacion.substring(9,10));
					digito = suma % 11;
					if(digito > 0)
						digito = 11 - digito;
					else
						digito = 0;
					
					//Válido si el número es igual al residuo de la cadena
					if(digito == numero)
						resultado = 1;
					else
						resultado = 0;
				}
				else
					resultado = 0;
			}
			if(id == 6){//RUC para Sociedades Públicas
				if(dimension == 13){
					factor = 3;
					while(i != 8){
						numero = Integer.parseInt(identificacion.substring(i,i+1)) * factor;
						suma += numero;
						if(factor == 2)
							factor = 7;
						else
							factor--;
						i++;
					}
					numero = Integer.parseInt(identificacion.substring(8,9));
					digito = suma % 11;
					if(digito > 0)
						digito = 11 - digito;
					else
						digito = 0;
					
					//Válido si el número es igual al residuo de la cadena
					if(digito == numero)
						resultado = 1;
					else
						resultado = 0;
				}
				else
					resultado = 0;
			}
			if(id <= 5){//RUC para Personas Naturales
				if(dimension == 13){
					factor = 2;
					while(i != 9){
						numero = Integer.parseInt(identificacion.substring(i,i+1)) * factor;
						if(numero > 9)
							numero -= 9;
						suma += numero;
						
						if(factor == 1)
							factor = 2;
						else
							factor--;
						i++;
					}
					numero = Integer.parseInt(identificacion.substring(9,10));
					digito = suma % 10;
					if(digito > 0)
						digito = 10 - digito;
					else
						digito = 0;
					
					//Válido si el número es igual al residuo de la cadena
					if(digito == numero)
						resultado = 1;
					else
						resultado = 0;
				}
				if(dimension == 10){
					factor = 2;
					while(i != 9){
						numero = Integer.parseInt(identificacion.substring(i,i+1)) * factor;
						if(numero > 9)
							numero -= 9;
						suma += numero;
						if(factor == 1)
							factor = 2;
						else
							factor--;
						i++;
					}
					numero = Integer.parseInt(identificacion.substring(9));
					digito = suma % 10;
					if(digito > 0)
						digito = 10 - digito;
					else
						digito = 0;
					
					//Válido si el número es igual al residuo de la cadena
					if(digito == numero)
						resultado = 1;
					else
						resultado = 0;
				}
			}
		}
		else
			throw new IncomeException("ERROR EN EL PROCESO DE VALIDACION DE CEDULA/RUC DE CLIENTE,  EL VALOR NO ES NUMERICO");
		return resultado;
	}
	
	/**
	 * Metodo para saber si una cadena de caracteres es numerico o no
	 * @param cadena: cadena a comprobar
	 * @return		: True = si es numerico, False = si no es numerico
	 */
	private static boolean isNumerico(String cadena){
		boolean respuesta = false;
		try {
			Float.parseFloat(cadena);
			respuesta = true;
		
		} catch (NumberFormatException nfe){
			respuesta = false;   
		}
		return respuesta;
	}

	@Override
	public Persona saveSingle(Persona persona) throws Throwable {
		System.out.println("saveSingle - Persona");
		persona = personaDaoService.save(persona, persona.getCodigo());
		return persona;
	}

}
