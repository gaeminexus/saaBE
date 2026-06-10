package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Entidad;

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

	/**
	 * Recupera todas las entidades con un idEstado específico.
	 * @param idEstado : Estado a filtrar
	 * @return : Listado de entidades
	 * @throws Throwable : Excepcion
	 */
	List<Entidad> selectByIdEstado(Long idEstado) throws Throwable;

	/**
	 * Para G45 — Busca una Entidad por su número de identificación.
	 * @param numeroIdentificacion : Número de identificación (cédula/RUC)
	 * @return : Entidad encontrada o null
	 */
	Entidad selectByNumeroIdentificacion(String numeroIdentificacion) throws Throwable;

	/**
	 * Busca una Entidad por código usando em.find() — retorna null si no existe.
	 */
	Entidad findById(Long codigo) throws Throwable;

	/**
	 * Carga múltiples entidades por sus códigos en una sola consulta.
	 * Optimización para evitar N+1 queries.
	 */
	List<Entidad> findByCodigosIn(List<Long> codigos) throws Throwable;

}
