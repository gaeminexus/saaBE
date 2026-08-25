package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleDevolucionAporte;

import jakarta.ejb.Local;

@Local
public interface DetalleDevolucionAporteDaoService extends EntityDao<DetalleDevolucionAporte> {

    /**
     * Detalles de una devolucion, ordenados por codigo.
     * @param idDevolucion : Codigo de la devolucion (CRD.DVAP)
     * @return             : Listado de detalles; vacio si la devolucion no existe
     * @throws Throwable   : Excepcion
     */
    List<DetalleDevolucionAporte> selectByDevolucion(Long idDevolucion) throws Throwable;

    /**
     * Detalles de un lote de devoluciones, para armar el listado de pantalla sin caer en
     * una consulta por devolucion (N+1).
     * @param idsDevolucion : Codigos de las devoluciones
     * @return              : Listado de detalles de todas ellas, ordenado por devolucion
     * @throws Throwable    : Excepcion
     */
    List<DetalleDevolucionAporte> selectByDevoluciones(List<Long> idsDevolucion) throws Throwable;
}
