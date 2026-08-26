/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.CierreCartera;
import com.saa.ejb.crd.service.dto.SolicitudCierreCartera;
import com.saa.model.crd.CorridaCierreCartera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Proceso mensual de apertura / cierre de cartera del módulo de créditos.
 *         Implementa los seis sub-procesos de §3.2 de
 *         docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md.
 *
 *         <h3>Las tres fechas</h3>
 *         <ul>
 *         <li><b>Corte</b> = último día del mes cerrado. Filtro de cuotas pendientes y
 *         fecha del asiento de neteo.</li>
 *         <li><b>Proceso</b> = primer día del mes siguiente. Fecha de los asientos de
 *         vencidos, bandas, apertura e intereses, y fecha con la que se resuelve la
 *         configuración de bandas vigente.</li>
 *         <li><b>Corte de apertura</b> = último día del mes que se ABRE. Hasta ahí factura
 *         la apertura.</li>
 *         </ul>
 *
 *         <h3>Por qué la reclasificación se calcula a cartera constante</h3>
 *         <p>
 *         Los asientos de cambio de bandas registran DIFERENCIAS, y §6.3 dice que cuadran
 *         "por construcción, porque el total de cartera no cambia; lo que cambia el total
 *         lo hacen otros asientos". Eso solo es cierto si las dos distribuciones que se
 *         comparan son del MISMO juego de cuotas. Por eso el proceso clasifica la cartera
 *         viva DOS veces —una con los días medidos a la fecha de corte anterior y otra a la
 *         actual— y contabiliza la diferencia. Si en cambio se comparara contra el saldo de
 *         las cuentas, el asiento arrastraría los pagos y las entregas del mes, que tienen
 *         sus propios asientos.
 *         </p>
 *         <p>
 *         El snapshot de la corrida anterior ({@code CRD.BDCC}) no se usa como base
 *         contable, sino como CONTROL: la diferencia entre él y la distribución
 *         recalculada a la misma fecha es exactamente lo que movieron los otros procesos, y
 *         se reporta en {@code desviaciones}.
 *         </p>
 *
 *         <h3>Lo que este proceso NO hace</h3>
 *         <p>
 *         El sub-proceso ⑤ Seguros de la pizarra no está: la factura entra por CxP y no la
 *         genera CRD. Los pagos (Petro y manuales), la jubilación, los cruces y los abonos
 *         son Fase 3.
 *         </p>
 */
@Local
public interface CierreCarteraService {

    /**
     * Calcula la corrida SIN grabar nada: los seis sub-procesos con sus líneas y totales,
     * el snapshot que dejaría y las desviaciones contra la corrida anterior.
     *
     * Es lo que contabilidad revisa antes de autorizar. No exige que el período esté libre:
     * previsualizar un mes ya ejecutado es legítimo y devuelve el cálculo de hoy, que puede
     * no coincidir con lo que se contabilizó — para eso está {@link #consultar}.
     *
     * @param solicitud  : Empresa y período a cerrar
     * @return           : La corrida calculada, con {@code idCorrida} en nulo
     * @throws Throwable : {@code IncomeException} con el motivo de la validación fallida
     */
    CierreCartera previsualizar(SolicitudCierreCartera solicitud) throws Throwable;

    /**
     * Calcula, graba la corrida con su snapshot y genera los asientos de los sub-procesos
     * que tengan líneas. Todo en una transacción: si un sub-asiento falla, no queda la
     * corrida a medias.
     *
     * Ejecutar dos veces el mismo período falla con mensaje claro y NO duplica asientos: lo
     * impide el índice único {@code UK_CRCT_PERIODO} además de la validación del servicio.
     *
     * @param solicitud  : Empresa y período a cerrar
     * @return           : La corrida grabada, con los asientos generados
     * @throws Throwable : {@code IncomeException} si el período ya se cerró, si un asiento
     *                     no cuadra o si falta parametrización
     */
    CierreCartera ejecutar(SolicitudCierreCartera solicitud) throws Throwable;

    /**
     * Devuelve lo que quedó GRABADO de un período: la corrida, su snapshot y los asientos
     * generados con sus totales. No recalcula nada.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @param anio       : Año del mes cerrado
     * @param mes        : Mes cerrado, 1 a 12
     * @return           : La corrida grabada
     * @throws Throwable : {@code IncomeException} si ese período no se ha corrido
     */
    CierreCartera consultar(Long idEmpresa, Long anio, Long mes) throws Throwable;

    /**
     * Anula los asientos de una corrida ejecutada y la marca REVERSADA. No borra filas: el
     * snapshot y los registros de asiento quedan, marcados, para auditoría.
     *
     * Una vez reversada, el período vuelve a estar libre y puede volver a ejecutarse.
     *
     * @param idCorrida  : Código de la corrida (CRD.CRCT)
     * @param usuario    : Usuario que reversa, para la auditoría
     * @param ip         : IP desde la que se reversa
     * @param motivo     : Motivo del reverso; se agrega a la observación de la corrida
     * @return           : La corrida reversada
     * @throws Throwable : {@code IncomeException} si la corrida no existe o no está EJECUTADA
     */
    CierreCartera reversar(Long idCorrida, String usuario, String ip, String motivo)
            throws Throwable;

    /**
     * Corridas de una empresa, de la más reciente a la más antigua. Para el histórico de la
     * pantalla.
     *
     * @param idEmpresa  : Código de la empresa (SCP.PJRQ)
     * @return           : Listado; VACÍO si nunca se corrió el proceso
     * @throws Throwable : Excepcion
     */
    List<CorridaCierreCartera> listarCorridas(Long idEmpresa) throws Throwable;
}
