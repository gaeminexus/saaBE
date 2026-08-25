/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.model.crd.BandaProducto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Corazón del modelo dinámico de bandas: resuelve
 *         <b>días → banda → cuenta contable</b> contra la parametrización vigente.
 *
 *         <p>
 *         <b>Punto único.</b> Todos los procesos contables de CRD (apertura/cierre mensual,
 *         asiento de vencidos, cambio de bandas, re-bandeo por abono a capital, pagos)
 *         deben clasificar por aquí. Ninguno vuelve a acumular períodos ni a cablear
 *         rangos de días: es justamente lo que el modelo dinámico viene a eliminar
 *         (§8 de LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md).
 *         </p>
 *
 *         <p>
 *         <b>La regla.</b> Con los rangos derivados acumulando períodos
 *         ({@code diaFin(k) = 30 * SUM(periodos 1..k)}), la banda que corresponde es la
 *         primera k tal que {@code dias <= diaFin(k)}. La banda abierta
 *         ({@code periodos} nulo, siempre la última) captura todo el resto.
 *         </p>
 *
 *         <p>
 *         <b>Cómo se cuentan los días</b> lo decide quien llama, según el tipo de cartera:
 *         POR VENCER cuenta del corte al vencimiento de la cuota; VENCIDO cuenta del
 *         vencimiento al corte. En ambos casos el primer día es 1, nunca 0.
 *         </p>
 */
@Local
public interface ClasificadorBandaService {

    /**
     * Clasifica una antigüedad en días contra la configuración vigente del producto.
     *
     * @param idProducto  : Código del producto (CRD.PRDC)
     * @param idEmpresa   : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera : 1 = por vencer, 2 = vencido ({@link com.saa.rubros.TipoCarteraBanda})
     * @param dias        : Días de antigüedad. Debe ser >= 1
     * @param fecha       : Fecha a la que se resuelve la vigencia; nula = hoy
     * @return            : Banda y cuenta contable que le corresponden
     * @throws Throwable  : {@code IncomeException} si faltan parámetros, si
     *                      {@code dias < 1}, si no hay configuración vigente o si la
     *                      configuración no tiene bandas
     */
    ResultadoClasificacionBanda clasificar(Long idProducto, Long idEmpresa, Long tipoCartera,
            Long dias, LocalDate fecha) throws Throwable;

    /**
     * Deriva los rangos en días de un juego de bandas ya leído de la base.
     *
     * Recibe las bandas ORDENADAS POR NÚMERO (como las devuelve
     * {@code BandaProductoDaoService.selectByConfiguracion}) y devuelve el detalle con
     * {@code diaInicio}/{@code diaFin}/{@code etiqueta} calculados y la cuenta resuelta.
     * La banda abierta sale con {@code diaFin} nulo.
     *
     * @param bandas     : Bandas ordenadas por número ascendente
     * @return           : Detalle con los rangos derivados; lista VACÍA si no hay bandas
     * @throws Throwable : Excepcion
     */
    List<BandaProductoDetalle> derivarRangos(List<BandaProducto> bandas) throws Throwable;

    /**
     * Clasifica contra un juego de bandas YA derivado, sin volver a la base.
     *
     * Es la variante para los procesos por lotes: leen la configuración una vez y
     * clasifican miles de cuotas contra la lista en memoria.
     *
     * @param bandas     : Bandas con los rangos ya derivados, ordenadas por número
     * @param dias       : Días de antigüedad. Debe ser >= 1
     * @return           : Banda que corresponde
     * @throws Throwable : {@code IncomeException} si {@code dias < 1}, si la lista está
     *                     vacía o si ninguna banda captura ese valor (configuración sin
     *                     banda abierta)
     */
    BandaProductoDetalle clasificarEnBandas(List<BandaProductoDetalle> bandas, Long dias)
            throws Throwable;
}
