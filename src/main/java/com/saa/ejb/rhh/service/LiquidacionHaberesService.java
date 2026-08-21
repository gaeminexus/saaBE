package com.saa.ejb.rhh.service;

import java.time.LocalDate;

import com.saa.model.rhh.Liquidacion;
import com.saa.model.rhh.ResultadoLiquidacion;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Liquidacion de haberes: el finiquito del colaborador que sale.</p>
 *
 * <h3>Todo sale de la parametria</h3>
 *
 * <p>Ningun porcentaje, tope ni plazo esta en el codigo. El desahucio sale de
 * <code>PRNMDSPR</code>, la indemnizacion de <code>PRNMDIMN</code>, <code>PRNMDIMX</code> y
 * <code>PRNMDIAN</code>, y que cada rubro corresponda o no lo decide la <b>causal</b>
 * (<code>RHH.CSTR</code>), no una lista en Java.</p>
 *
 * <h3>Los decimos y las vacaciones no se recalculan aqui</h3>
 *
 * <p>Se piden a <code>BeneficioSocialService</code> y a
 * <code>AcreditacionVacacionesService</code>, que ya los saben calcular y estan congelados.
 * Reimplementarlos habria creado una segunda verdad para el mismo numero.</p>
 *
 * <h3>Un neto negativo se registra</h3>
 *
 * <p>A diferencia del rol, aqui el neto negativo <b>no lanza</b>: significa que el trabajador
 * debe dinero a la empresa —anticipos, prestamos internos— y ese saldo hay que registrarlo para
 * gestionarlo, no hacerlo desaparecer.</p>
 */
@Local
public interface LiquidacionHaberesService {

    /**
     * Calcula el finiquito <b>sin persistir nada</b>, para que el usuario lo revise.
     *
     * @param idContrato	: Id del contrato que termina
     * @param fechaSalida	: Fecha de salida
     * @param idCausal		: Causal de terminacion
     * @return				: El finiquito con sus rubros
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    ResultadoLiquidacion simular(Long idContrato, LocalDate fechaSalida, Long idCausal) throws Throwable;

    /**
     * Calcula el finiquito y lo persiste con sus rubros en <code>TMLQ</code>.
     *
     * <p>Idempotente mientras la liquidacion no este aprobada: recalcular rehace los rubros.</p>
     *
     * @param idContrato	: Id del contrato que termina
     * @param fechaSalida	: Fecha de salida
     * @param idCausal		: Causal de terminacion
     * @param observaciones	: Observaciones
     * @param usuario		: Usuario que ejecuta
     * @return				: La liquidacion persistida
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Liquidacion calcular(Long idContrato, LocalDate fechaSalida, Long idCausal,
            String observaciones, String usuario) throws Throwable;

    /**
     * Aprueba la liquidacion. Desde aqui ya no se recalcula.
     *
     * @param idLiquidacion	: Id de la liquidacion
     * @param usuario		: Usuario que aprueba
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void aprobar(Long idLiquidacion, String usuario) throws Throwable;

    /**
     * Ejecuta la salida: cierra el contrato, cesa al empleado, genera el aviso de salida al
     * IESS, cancela los descuentos recurrentes vigentes y caduca los saldos de vacaciones.
     *
     * <p>Es el punto de no retorno del ciclo del colaborador, y por eso exige la liquidacion
     * <b>aprobada</b>: ejecutar la salida sobre un finiquito que todavia se puede recalcular
     * dejaria al empleado cesante con un finiquito que cambia.</p>
     *
     * @param idLiquidacion	: Id de la liquidacion
     * @param usuario		: Usuario que ejecuta
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void ejecutarSalida(Long idLiquidacion, String usuario) throws Throwable;

}
