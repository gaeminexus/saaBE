package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.ResultadoCalificacionRiesgo;
import com.saa.model.crd.Producto;

import jakarta.ejb.Local;

/**
 * Resuelve <b>días → calificación de riesgo → porcentaje de provisión</b> contra la
 * parametrización vigente (CRD.CFCR/CRD.ESCR) — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 * Hermano de {@link ClasificadorBandaService}, pero para una clasificación DISTINTA: la banda
 * dice a qué cuenta contable va el saldo, esto dice cuánta provisión constituir para el G48.
 * Ningún corte coincide entre las dos — no reutilizar una por la otra.
 *
 * <p><b>Punto único.</b> {@code GeneracionG48ServiceImpl} debe preguntarle a este servicio en
 * vez de decidir con literales cableados (el defecto que este servicio viene a eliminar: un
 * producto hipotecario nuevo agregado a la configuración se califica bien sin tocar código).</p>
 */
@Local
public interface CalificacionRiesgoService {

    /**
     * Califica una antigüedad en días contra la configuración vigente del producto.
     *
     * <p>{@code dias == null} se trata como {@code 0} (mismo criterio que el código cableado
     * que este servicio reemplaza: sin morosidad, calificación A1).</p>
     *
     * @param idProducto : Código del producto (CRD.PRDC)
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ); puede ser null si la configuración
     *                     del producto no distingue por empresa
     * @param dias       : Días de morosidad
     * @param fecha      : Fecha a la que se resuelve la vigencia de la configuración; null = hoy
     * @return            : Calificación y porcentaje de provisión
     * @throws Throwable  : {@code IncomeException} si falta el producto o no hay configuración
     *                      vigente, o si ningún renglón de la escala cubre los días dados
     */
    ResultadoCalificacionRiesgo calificar(Long idProducto, Long idEmpresa, Long dias, LocalDate fecha)
            throws Throwable;

    /**
     * Productos sin configuración de calificación vigente a la fecha dada — para que un
     * proceso batch (el G48) falle UNA vez con el listado completo antes de arrancar, en vez
     * de fallar producto por producto a mitad de la corrida (PLAN-CALIFICACION-RIESGO-
     * PARAMETRIZABLE.md §5).
     *
     * @return Lista vacía si todos los productos tienen configuración vigente
     */
    List<Producto> productosSinConfiguracion(LocalDate fecha) throws Throwable;
}
