package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

/** DAO de CRD.CFCR — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md. */
@Local
public interface ConfiguracionCalificacionRiesgoDaoService extends EntityDao<ConfiguracionCalificacionRiesgo> {

    /**
     * La configuración vigente de un producto a una fecha. {@code idEmpresa} es tolerante a
     * {@code null} en la fila de CFCR (aplica a cualquier empresa) — la carga inicial
     * (sql/177) quedó así para todos los productos.
     *
     * @return la configuración vigente, o {@code null} si no hay ninguna
     */
    ConfiguracionCalificacionRiesgo selectVigentePorProducto(Long idProducto, Long idEmpresa, LocalDate fecha)
            throws Throwable;

    /**
     * Productos de {@code CRD.PRDC} que NO tienen ninguna {@code ConfiguracionCalificacionRiesgo}
     * vigente a la fecha dada — para que el G48 falle una sola vez con el listado completo
     * (PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md §5), no producto por producto.
     */
    List<Producto> selectProductosSinConfiguracionVigente(LocalDate fecha) throws Throwable;
}
