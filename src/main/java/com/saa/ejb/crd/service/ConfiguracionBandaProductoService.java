/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ConfiguracionBandaDetalle;
import com.saa.ejb.crd.service.dto.CuentaBandaDisponible;
import com.saa.ejb.crd.service.dto.ProductoBandas;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigencia;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionBanda;
import com.saa.model.crd.ConfiguracionBandaProducto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Parametrización de bandas de cartera por producto (CRD.CBPR + CRD.BNDP).
 *         Es lo que consume la pantalla de parametrización; la resolución en runtime
 *         (días → banda → cuenta) vive en {@link ClasificadorBandaService}.
 */
@Local
public interface ConfiguracionBandaProductoService extends EntityService<ConfiguracionBandaProducto> {

    /**
     * Configuración vigente de un producto para un tipo de cartera, con sus bandas
     * ordenadas por número y los rangos en días ya derivados.
     *
     * @param idProducto  : Código del producto (CRD.PRDC)
     * @param idEmpresa   : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera : 1 = por vencer, 2 = vencido
     * @param fecha       : Fecha a la que se evalúa la vigencia; nula = hoy
     * @return            : Configuración con bandas
     * @throws Throwable  : {@code IncomeException} si no hay configuración vigente
     */
    ConfiguracionBandaDetalle selectVigenteConBandas(Long idProducto, Long idEmpresa,
            Long tipoCartera, LocalDate fecha) throws Throwable;

    /**
     * Listado completo para la pantalla: TODOS los productos de crédito de la empresa
     * —activos e inactivos, marcados con su estado— con sus configuraciones vigentes de
     * los dos tipos de cartera.
     *
     * Los productos SIN configuración también salen, con la configuración en nulo: ese
     * hueco es el que el usuario tiene que ver y llenar.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param fecha      : Fecha a la que se evalúa la vigencia; nula = hoy
     * @return           : Una fila por producto, ordenadas por código de producto
     * @throws Throwable : {@code IncomeException} si no hay productos
     */
    List<ProductoBandas> listarParametrizacion(Long idEmpresa, LocalDate fecha) throws Throwable;

    /**
     * Graba una configuración COMPLETA —cabecera más bandas— en una sola transacción.
     *
     * Con {@code idConfiguracion} nulo crea; con {@code idConfiguracion} presente edita en
     * el lugar, y <b>solo si la vigencia todavía no empezó</b>. Una configuración ya
     * vigente se cambia con {@link #cerrarVigencia(SolicitudCierreVigencia)}.
     *
     * Validaciones (todas lanzan {@code IncomeException} con el motivo):
     * números de banda consecutivos desde 1; exactamente UNA banda con períodos nulos y
     * debe ser la última; las demás con períodos >= 1; cuenta contable obligatoria,
     * existente, activa y de la misma empresa; tipo de cartera en {1,2}; una sola
     * configuración vigente por (producto, empresa, tipo de cartera).
     *
     * @param solicitud  : Cabecera y bandas
     * @return           : Configuración grabada, con los rangos derivados
     * @throws Throwable : {@code IncomeException} con el motivo de la validación fallida
     */
    ConfiguracionBandaDetalle guardarConfiguracion(SolicitudConfiguracionBanda solicitud)
            throws Throwable;

    /**
     * Cambio normativo: cierra la vigencia de la configuración actual en
     * {@code fechaDesdeNueva - 1 día} y crea la nueva desde {@code fechaDesdeNueva}, con
     * las bandas que traiga la solicitud. Todo en una transacción.
     *
     * La configuración vieja NO se toca más allá de su fecha de cierre: queda íntegra para
     * reprocesos y auditoría de los períodos ya contabilizados.
     *
     * @param solicitud  : Configuración a cerrar, fecha de corte y bandas nuevas
     * @return           : Configuración NUEVA, con los rangos derivados
     * @throws Throwable : {@code IncomeException} con el motivo de la validación fallida
     */
    ConfiguracionBandaDetalle cerrarVigencia(SolicitudCierreVigencia solicitud) throws Throwable;

    /**
     * Historial de configuraciones de una terna, vigentes y cerradas, de la más reciente a
     * la más antigua, cada una con sus bandas. Para auditoría y reprocesos.
     *
     * @param idProducto  : Código del producto (CRD.PRDC)
     * @param idEmpresa   : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera : 1 = por vencer, 2 = vencido
     * @return            : Historial; lista VACÍA si nunca se parametrizó
     * @throws Throwable  : Excepcion
     */
    List<ConfiguracionBandaDetalle> selectHistorial(Long idProducto, Long idEmpresa,
            Long tipoCartera) throws Throwable;

    /**
     * Catálogo de cuentas candidatas para el buscador de la pantalla: cuentas ACTIVAS y de
     * MOVIMIENTO de la empresa cuyo número o nombre contiene el filtro.
     *
     * Solo se ofrecen cuentas de movimiento: una de acumulación no puede recibir saldo, y
     * elegirla dejaría la parametrización rota sin que nadie lo note hasta el cierre.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param filtro     : Texto a buscar en el número de cuenta o en el nombre; nulo = todas
     * @return           : Cuentas ordenadas por número; lista VACÍA si no hay coincidencias
     * @throws Throwable : {@code IncomeException} si falta la empresa
     */
    List<CuentaBandaDisponible> buscarCuentas(Long idEmpresa, String filtro) throws Throwable;
}
