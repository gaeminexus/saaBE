package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.DetalleConfiguracionCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.ProductoCalificacionRiesgo;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigenciaCalificacion;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionCalificacionRiesgo;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;

import jakarta.ejb.Local;

/**
 * Parametrización administrable de la escala de calificación de riesgo (CRD.CFCR + CRD.ESCR) —
 * P22, PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md. Es lo que consume la pantalla de
 * administración; la resolución en runtime (días → calificación → provisión), la que ya consume
 * {@code GeneracionG48ServiceImpl}, sigue viviendo en {@link CalificacionRiesgoService} — este
 * servicio NUNCA la reimplementa, siempre delega en las mismas DAOs.
 *
 * <b>NO es la misma parametrización que las bandas contables</b>
 * ({@code ConfiguracionBandaProductoService}): la banda dice a qué CUENTA CONTABLE va el saldo;
 * esta dice cuánta PROVISIÓN regulatoria se constituye. Ningún corte coincide entre las dos — no
 * compartir esta interfaz ni su implementación con la de bandas por parecidas.
 */
@Local
public interface ConfiguracionCalificacionRiesgoService extends EntityService<ConfiguracionCalificacionRiesgo> {

    /**
     * Configuración vigente de un producto, con su escala completa.
     *
     * @param idProducto : Código del producto (CRD.PRDC)
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ); {@code null} = universal (ver el
     *                     javadoc de {@code ConfiguracionCalificacionRiesgo})
     * @param fecha      : Fecha a la que se evalúa la vigencia; nula = hoy
     * @return            : Configuración con su escala
     * @throws Throwable  : {@code IncomeException} si no hay configuración vigente
     */
    DetalleConfiguracionCalificacionRiesgo selectVigenteConEscala(Long idProducto, Long idEmpresa,
            LocalDate fecha) throws Throwable;

    /**
     * Listado completo para la pantalla: TODOS los productos de crédito (activos e inactivos)
     * con su configuración vigente. Los productos SIN configuración también salen, con la
     * configuración en nulo: ese hueco es el que el usuario tiene que ver y llenar — el mismo
     * hueco que hoy detecta {@link CalificacionRiesgoService#productosSinConfiguracion}.
     *
     * @param idEmpresa : Código de la empresa (SCP.PJRQ); {@code null} trae solo las
     *                    configuraciones universales (opcional, a propósito — ver el javadoc de
     *                    la interfaz)
     * @param fecha     : Fecha a la que se evalúa la vigencia; nula = hoy
     * @return           : Una fila por producto, ordenadas por código
     * @throws Throwable : {@code IncomeException} si no hay productos
     */
    List<ProductoCalificacionRiesgo> listarParametrizacion(Long idEmpresa, LocalDate fecha) throws Throwable;

    /**
     * Graba una configuración COMPLETA —cabecera más escala— en una sola transacción.
     *
     * Con {@code idConfiguracion} nulo crea; con {@code idConfiguracion} presente edita en el
     * lugar, y <b>solo si la vigencia todavía no empezó</b>. Una configuración ya vigente se
     * cambia con {@link #cerrarVigencia(SolicitudCierreVigenciaCalificacion)}.
     *
     * Validaciones (todas lanzan {@code IncomeException} con el motivo, ANTES de grabar nada):
     * al menos una calificación; sin calificaciones repetidas; porcentaje de provisión entre 0
     * y 1; <b>sin huecos ni solapes</b> — la primera calificación (en el orden de la lista, que
     * ES el orden de evaluación) empieza en el día 0 y cada una arranca donde terminó la
     * anterior más uno; exactamente la ÚLTIMA puede quedar sin tope superior; una sola
     * configuración vigente por (producto[, empresa]).
     *
     * @param solicitud  : Cabecera y escala
     * @return            : Configuración grabada
     * @throws Throwable  : {@code IncomeException} con el motivo de la validación fallida
     */
    DetalleConfiguracionCalificacionRiesgo guardarConfiguracion(SolicitudConfiguracionCalificacionRiesgo solicitud)
            throws Throwable;

    /**
     * Cambio normativo: cierra la vigencia de la configuración actual en
     * {@code fechaDesdeNueva - 1 día} y crea la nueva desde {@code fechaDesdeNueva}, con la
     * escala que traiga la solicitud. Todo en una transacción.
     *
     * @param solicitud  : Configuración a cerrar, fecha de corte y escala nueva
     * @return            : Configuración NUEVA
     * @throws Throwable  : {@code IncomeException} con el motivo de la validación fallida
     */
    DetalleConfiguracionCalificacionRiesgo cerrarVigencia(SolicitudCierreVigenciaCalificacion solicitud)
            throws Throwable;

    /**
     * Historial de configuraciones de un producto, vigentes y cerradas, de la más reciente a la
     * más antigua, cada una con su escala. Para auditoría y reprocesos.
     *
     * @param idProducto : Código del producto (CRD.PRDC)
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ); {@code null} = universal
     * @return            : Historial; lista VACÍA si nunca se parametrizó
     * @throws Throwable  : Excepcion
     */
    List<DetalleConfiguracionCalificacionRiesgo> selectHistorial(Long idProducto, Long idEmpresa)
            throws Throwable;
}
