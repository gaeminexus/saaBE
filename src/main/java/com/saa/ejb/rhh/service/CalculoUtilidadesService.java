package com.saa.ejb.rhh.service;

import com.saa.model.rhh.Utilidad;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Reparto de utilidades del ejercicio (Art. 97 del Codigo del Trabajo).</p>
 *
 * <p><b>Por que no se llama <code>UtilidadService</code>,</b> como decia el §9 del plan: ese
 * nombre lo ocupa el CRUD de <code>RHH.UTLD</code>, que el checklist por entidad exige. Mismo
 * criterio que <code>GeneracionRolPagoService</code> frente a <code>RolPagoService</code> y
 * <code>GeneracionOrdenPagoService</code> frente a <code>OrdenPagoNominaService</code>.</p>
 *
 * <h3>Se construye completo aunque ASOPREP no reparta</h3>
 *
 * <p><code>CFNMAPUT</code> esta en <code>'N'</code>, asi que el servicio existe y <b>rechaza la
 * operacion mientras la bandera este apagada</b>. Es el mismo patron de
 * <code>ProvisionActuarialService</code>: la funcionalidad se construye entera, y lo que decide
 * si se usa es un dato, no la ausencia de codigo.</p>
 *
 * <h3>Ningun porcentaje en el codigo</h3>
 *
 * <p><code>PRNMUTPR</code> es el porcentaje de la utilidad contable que se reparte,
 * <code>PRNMUTDI</code> la parte que va por dias trabajados, <code>PRNMUTCG</code> la que va por
 * cargas familiares, y <code>PRNMUTSB</code> el tope por trabajador expresado en SBU. Por eso
 * las propiedades se llaman <code>baseTotal</code>, <code>basePorDias</code> y
 * <code>basePorCargas</code>: el nombre dice que reparten, no con que porcentaje.</p>
 */
@Local
public interface CalculoUtilidadesService {

    /**
     * Calcula el reparto del ejercicio y persiste su detalle por empleado.
     *
     * <p>Idempotente por el unique <code>UQ_UTLD_ANIO</code>: recalcular actualiza el reparto y
     * rehace el detalle.</p>
     *
     * <p><b>El excedente sobre el tope no se pierde:</b> se acumula en
     * <code>UTLDEXCD</code> y en <code>DTUTEXCD</code> porque la ley lo transfiere al IESS, no
     * al resto de trabajadores.</p>
     *
     * @param idEmpresa			: Id de la empresa
     * @param anio				: Ejercicio fiscal
     * @param utilidadContable	: Utilidad contable del ejercicio
     * @param usuario			: Usuario que ejecuta
     * @return					: El reparto con sus totales
     * @throws Throwable		: IncomeException si CFNMAPUT esta en 'N'
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Utilidad calcular(Long idEmpresa, Integer anio, Double utilidadContable, String usuario)
            throws Throwable;

}
