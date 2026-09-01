package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Ciclo de la orden de pago de beneficio social (decimo acumulado, fondos de reserva):
 * GENERADA -&gt; ENVIADA_A_TESORERIA (dispara el {@code PagoProgramado} de origen externo
 * {@code RHH_BENEFICIO_SOCIAL}) -&gt; PAGADA (al confirmar el pago, RRHH contabiliza la baja
 * de provision). O ANULADA desde GENERADA/ENVIADA_A_TESORERIA sin pago confirmado.</p>
 *
 * <p>Contrato: docs/logica-negocio/rhh/API-PAGO-BENEFICIOS-SOCIALES.md. Diseno:
 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #3.</p>
 */
@Local
public interface OrdenBeneficioSocialService extends EntityService<OrdenBeneficioSocial> {

    /**
     * Agrupa las liquidaciones sueltas ({@code LQBSODBS is null}, estado PENDIENTE) de una
     * empresa, tipo de beneficio y anio (y region, solo para decimo cuarto) y arma la
     * cabecera consolidada. Idempotente por combinacion: si ya hay una orden viva (GENERADA,
     * ENVIADA_A_TESORERIA o PAGADA) para la misma combinacion, no crea otra.
     *
     * @param idEmpresa			: Id de la empresa
     * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
     *							  (1 decimo tercero, 2 decimo cuarto, 3 fondos de reserva)
     * @param anio				: Anio del beneficio
     * @param region			: Region del decimo cuarto; obligatoria solo si tipoBeneficio=2,
     *							  debe ir null en los demas casos
     * @param usuario			: Usuario que ejecuta
     * @return					: Mapa de resultado; ver el contrato #1.2 para las tres formas
     *							  (creada / exito:false sin liquidaciones / exito:false con
     *							  idOrdenExistente)
     * @throws Throwable		: IncomeException si los parametros no son validos
     */
    Map<String, Object> generar(Long idEmpresa, Long tipoBeneficio, Integer anio, Long region,
            String usuario) throws Throwable;

    /**
     * Detalle de una orden: cabecera mas las liquidaciones agrupadas y el estado del pago
     * enlazado. Ver el contrato #1.3.
     *
     * @param idOrden	: Id de la orden
     * @return			: Mapa con la cabecera, o {@code {exito:false, mensaje}} si no existe
     * @throws Throwable	: Excepcion
     */
    Map<String, Object> detalle(Long idOrden) throws Throwable;

    /**
     * Bandeja de ordenes con filtros opcionales (null = sin filtrar por ese criterio). Ver
     * el contrato #1.3bis y docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md.
     *
     * @param idEmpresa			: Id de la empresa, obligatorio
     * @param anio				: Anio del beneficio; null = todos
     * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL; null = todos
     * @param estado			: Codigo alterno del detalle del rubro RHH_ESTADO_ORDEN_BENEFICIO; null = todos
     * @return					: Filas de la bandeja
     * @throws Throwable		: Excepcion
     */
    List<OrdenBeneficioSocialResumen> listar(Long idEmpresa, Integer anio, Long tipoBeneficio,
            Long estado) throws Throwable;

    /**
     * Registra el pago consolidado de la orden en la bandeja de aprobacion de tesoreria, sin
     * desglose contable y con {@code idCuentaBancariaOrigen = null} (decision D1 del usuario):
     * el pago nace POR_APROBAR y no genera asiento ni movimiento bancario al confirmarse —
     * la contabilidad la hace RRHH en {@link #confirmarPago}. Ver el contrato #1.4.
     *
     * @param idOrden			: Id de la orden, debe estar GENERADA
     * @param idUsuario			: Id de SCP.PJRQ del usuario que ejecuta. FK real que exige
     *							  {@code registrarPagoDeOrigenExterno} — nunca se resuelve por
     *							  nombre.
     * @param observacion		: Observacion del pago
     * @return					: Mapa de resultado, ver el contrato #1.4
     * @throws Throwable		: IncomeException (409) si la orden no esta GENERADA o falta idUsuario
     */
    Map<String, Object> enviarATesoreria(Long idOrden, Long idUsuario, String observacion) throws Throwable;

    /**
     * Cierra el ciclo: exige que el {@code PagoProgramado} de la orden este CONFIRMADO,
     * marca cada {@code LQBS} de la orden como pagada y genera el asiento de baja de
     * provision (DEBE la linea de provision por pagar del tipo, HABER banco). Ver el
     * contrato #1.5.
     *
     * @param idOrden			: Id de la orden, debe estar ENVIADA_A_TESORERIA
     * @param fechaPago			: Fecha de acreditacion del pago
     * @param usuario			: Usuario que ejecuta
     * @return					: Mapa de resultado, ver el contrato #1.5
     * @throws Throwable		: IncomeException (409) si el pago no esta CONFIRMADO en tesoreria,
     *							  o la orden no esta ENVIADA_A_TESORERIA
     */
    Map<String, Object> confirmarPago(Long idOrden, LocalDate fechaPago, String usuario) throws Throwable;

    /**
     * Anula la orden: desenlaza las liquidaciones ({@code LQBSODBS = null}) para que puedan
     * volver a agruparse, y marca la orden ANULADA. Ver el contrato #1.6.
     *
     * @param idOrden			: Id de la orden
     * @param motivo			: Motivo de la anulacion, obligatorio
     * @param usuario			: Usuario que ejecuta
     * @return					: Mapa de resultado
     * @throws Throwable		: IncomeException (409) si la orden ya esta PAGADA o ANULADA
     */
    Map<String, Object> anular(Long idOrden, String motivo, String usuario) throws Throwable;

}
