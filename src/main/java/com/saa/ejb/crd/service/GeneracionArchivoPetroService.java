package com.saa.ejb.crd.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.GeneracionArchivoPetro;

import jakarta.ejb.Local;

/**
 * Interface Service para GeneracionArchivoPetro (GNAP).
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Local
public interface GeneracionArchivoPetroService extends EntityService<GeneracionArchivoPetro>  {

    /**
     * Crea una nueva generación de archivo.
     * 
     * @param generacion Objeto con los datos de la generación
     * @return Generación creada con ID asignado
     * @throws Throwable Si ocurre un error
     */
    GeneracionArchivoPetro crear(GeneracionArchivoPetro generacion) throws Exception;
    
    /**
     * Actualiza una generación existente.
     * 
     * @param generacion Objeto con los datos actualizados
     * @return Generación actualizada
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro actualizar(GeneracionArchivoPetro generacion) throws Exception;
    
    /**
     * Busca una generación por ID.
     * 
     * @param codigo Código de la generación
     * @return Generación encontrada o null
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro buscarPorId(Long codigo) throws Exception;
    
    /**
     * Busca una generación por periodo.
     * 
     * @param mes Mes del periodo (1-12)
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial
     * @return Generación encontrada o null
     * @throws Exception Si ocurre un error
     */
    GeneracionArchivoPetro buscarPorPeriodo(Long mes, Long anio, Long codigoFilial) throws Exception;
    
    /**
     * Lista todas las generaciones de una filial.
     * 
     * @param codigoFilial Código de la filial
     * @return Lista de generaciones ordenadas por fecha desc
     * @throws Exception Si ocurre un error
     */
    List<GeneracionArchivoPetro> listarPorFilial(Long codigoFilial) throws Exception;
    
    /**
     * Lista generaciones por estado.
     * 
     * @param estado Estado (1=GENERADO, 2=ENVIADO, 3=PROCESADO)
     * @return Lista de generaciones
     * @throws Exception Si ocurre un error
     */
    List<GeneracionArchivoPetro> listarPorEstado(Long estado) throws Exception;
    
    /**
     * Crea la cabecera de generación validando que no exista duplicado.
     * 
     * @param mes Mes del periodo
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial
     * @param usuario Usuario que crea
     * @return Cabecera creada
     * @throws Exception Si ya existe o hay error
     */
    GeneracionArchivoPetro crearCabeceraGeneracion(Long mes, Long anio, Long codigoFilial, String usuario) throws Exception;
    
    /**
     * Procesa la generación completa: recopila datos, crea detalles y genera archivo.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que procesa
     * @return Mapa con resultados
     * @throws Exception Si hay error
     */
    Map<String, Object> procesarGeneracion(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Anula una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que anula
     * @param motivo Motivo de anulación
     * @throws Exception Si hay error
     */
    void anular(Long codigoGeneracion, String usuario, String motivo) throws Exception;
    
    /**
     * Marca una generación como enviada.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que marca
     * @return Generación actualizada
     * @throws Exception Si hay error
     */
    GeneracionArchivoPetro marcarEnviado(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Marca una generación como procesada.
     * 
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que marca
     * @return Generación actualizada
     * @throws Exception Si hay error
     */
    GeneracionArchivoPetro marcarProcesado(Long codigoGeneracion, String usuario) throws Exception;
    
    /**
     * Obtiene el detalle completo de una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @return Mapa con generación y detalles
     * @throws Exception Si hay error
     */
    Map<String, Object> obtenerDetalle(Long codigoGeneracion) throws Exception;
    
    /**
     * Obtiene estadísticas de una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @return Mapa con estadísticas
     * @throws Exception Si hay error
     */
    Map<String, Object> obtenerEstadisticas(Long codigoGeneracion) throws Exception;
    
    /**
     * Regenera el archivo TXT de una generación.
     * 
     * @param codigoGeneracion ID de la generación
     * @return Ruta del archivo regenerado
     * @throws Exception Si hay error
     */
    String regenerarArchivo(Long codigoGeneracion) throws Exception;
}
