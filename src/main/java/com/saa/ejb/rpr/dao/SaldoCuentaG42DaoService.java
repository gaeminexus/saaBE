package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.Local;

@Local
public interface SaldoCuentaG42DaoService extends EntityDao<SaldoCuentaG42> {

    /**
     * Busca un registro de CG42 por entidad y detalle de ejecución.
     * Permite hacer UPDATE si ya existe o INSERT si no existe.
     *
     * @param codigoEntidad  Código de la entidad (CRD.ENTD)
     * @param detalle        Detalle de ejecución (RPR.EJRD)
     * @return SaldoCuentaG42 encontrado o null
     */
    SaldoCuentaG42 selectByEntidadYDetalle(Long codigoEntidad, DetalleEjecucionReporte detalle) throws Throwable;

    /**
     * Retorna todos los registros CG42 que pertenecen a un EJRD específico.
     * Se usa en G43 para obtener el universo de entidades del mes anterior.
     *
     * @param codigoDetalle Código del DetalleEjecucionReporte (RPR.EJRD)
     * @return Lista de SaldoCuentaG42 del detalle indicado
     */
    List<SaldoCuentaG42> selectByDetalle(Long codigoDetalle) throws Throwable;

    /**
     * Retorna los registros del G42 del mes anterior que NO tienen correspondencia
     * (por identificacion) en el G42 del mes actual.
     * La comparación se realiza en BD con NOT EXISTS.
     *
     * Usado en G43 — Camino A: cuando existe un EJRC del mes anterior.
     *
     * @param codigoDetallePrevio  Código del EJRD G42 del mes anterior
     * @param codigoDetalleActual  Código del EJRD G42 del mes actual
     * @return Lista de SaldoCuentaG42 (mes anterior) que ya no tienen saldo este mes
     */
    List<SaldoCuentaG42> selectCesantesDesdeG42Previo(Long codigoDetallePrevio, Long codigoDetalleActual) throws Throwable;
}
