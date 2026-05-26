package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.SaldoCuentaG42;
import jakarta.ejb.Local;

@Local
public interface SaldoCuentaG42Service extends EntityService<SaldoCuentaG42> {

    /**
     * Retorna todos los registros CG42 que pertenecen a un EJRD específico.
     * Usado por el G43 para obtener el universo de entidades con saldo del mes anterior.
     *
     * @param codigoDetalle Código del DetalleEjecucionReporte (RPR.EJRD)
     * @return Lista de SaldoCuentaG42 del detalle indicado (puede ser vacía)
     */
    List<SaldoCuentaG42> selectByDetalle(Long codigoDetalle) throws Throwable;

    /**
     * Retorna los registros del G42 del mes anterior que NO tienen correspondencia
     * (por identificacion) en el G42 del mes actual. Comparación con NOT EXISTS en BD.
     *
     * @param codigoDetallePrevio  Código del EJRD G42 del mes anterior
     * @param codigoDetalleActual  Código del EJRD G42 del mes actual
     * @return Lista de SaldoCuentaG42 cesantes
     */
    List<SaldoCuentaG42> selectCesantesDesdeG42Previo(Long codigoDetallePrevio, Long codigoDetalleActual) throws Throwable;
}
