package com.saa.ejb.rpr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface DetalleEjecucionReporteService extends EntityService<DetalleEjecucionReporte> {

    /**
     * Recupera los detalles de una ejecución específica.
     * @param idEjecucion : Id de la cabecera de ejecución
     * @return            : Listado de detalles
     * @throws Throwable  : Excepcion
     */
    List<DetalleEjecucionReporte> selectByEjecucion(Long idEjecucion) throws Throwable;

    /**
     * Recupera los detalles con novedades de una ejecución.
     * @param idEjecucion : Id de la cabecera de ejecución
     * @return            : Listado de detalles con novedades
     * @throws Throwable  : Excepcion
     */
    List<DetalleEjecucionReporte> selectConNovedadesByEjecucion(Long idEjecucion) throws Throwable;

    /**
     * Recupera los detalles pendientes o con novedades (estado != 1) de una ejecución.
     * @param idEjecucion : Id de la cabecera de ejecución
     * @return            : Listado de detalles a re-ejecutar
     * @throws Throwable  : Excepcion
     */
    List<DetalleEjecucionReporte> selectPendientesYNovedadesByEjecucion(Long idEjecucion) throws Throwable;

    /**
     * Busca el EJRD de un tipo de reporte específico dentro de una ejecución.
     * Usado por el G43 para encontrar el EJRD del G42 del mes anterior.
     *
     * @param idEjecucion Código de la cabecera EJRC
     * @param tipoReporte Ej: "G42"
     * @return DetalleEjecucionReporte encontrado o null
     */
    DetalleEjecucionReporte selectByEjecucionYTipo(Long idEjecucion, String tipoReporte) throws Throwable;
}
