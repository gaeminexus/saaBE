package com.saa.ejb.credito.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.Entidad;

import jakarta.ejb.Local;

@Local
public interface EntidadService extends EntityService<Entidad>{
	
	/**
	 * Selecciona las coincidencias de ParticipeXCargaArchivo por nombre.
	 * @param nombre: Nombre a buscar.
	 * @return: Lista de ParticipeXCargaArchivo que coinciden con el nombre.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<Entidad> selectCoincidenciasByNombre(String nombre) throws Throwable;
	
}
