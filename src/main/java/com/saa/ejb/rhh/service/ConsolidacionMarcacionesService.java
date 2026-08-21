package com.saa.ejb.rhh.service;

import java.time.LocalDate;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Consolidacion de las marcaciones sueltas en el resumen diario <code>RHH.RSMN</code>, que
 * es lo que el motor de nomina lee.</p>
 *
 * <h3>Como se arma el dia</h3>
 *
 * <p>Se agrupa por <code>(empleado, fecha)</code> y se ordena por hora: la <b>primera</b>
 * marcacion es la entrada, la <b>ultima</b> la salida, y los pares intermedios se restan como
 * almuerzo o permiso. El turno teorico sale de <code>CNTE.CNTETRNO</code> y su
 * <code>DTLL</code> del dia de la semana.</p>
 *
 * <h3>Un numero impar de marcaciones no se adivina</h3>
 *
 * <p>Marca el resumen como <b>inconsistente</b> y lo deja para revision manual. El sistema no
 * puede saber si falta la salida, si sobra una marcacion repetida o si alguien salio sin
 * marcar, y cualquiera de las tres suposiciones produce horas trabajadas equivocadas que
 * despues se pagan.</p>
 *
 * <h3>Ningun porcentaje en el codigo</h3>
 *
 * <p>Los recargos salen de <code>PRNM</code> —<code>PRNMRCSP</code>, <code>PRNMRCEX</code> y
 * <code>PRNMRCNC</code>— y los topes de <code>PRNMHRMX</code> y <code>PRNMHRSX</code>. El
 * horario del turno y su tolerancia salen de <code>TRNO</code>/<code>DTLL</code>.</p>
 */
@Local
public interface ConsolidacionMarcacionesService {

    /**
     * Consolida las marcaciones sin procesar de un rango en resumenes diarios.
     *
     * <p>Idempotente por <code>(empleado, fecha)</code>: si el resumen del dia ya existe lo
     * actualiza. <b>No toca los resumenes de un dia ya procesado en un periodo cerrado</b>
     * (<code>RSMNPRCS='S'</code>): recalcularlos cambiaria la base de una nomina ya pagada.</p>
     *
     * @param desde			: Fecha desde, inclusive
     * @param hasta			: Fecha hasta, inclusive
     * @param usuario		: Usuario que ejecuta
     * @return				: Numero de resumenes generados o actualizados
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int consolidar(LocalDate desde, LocalDate hasta, String usuario) throws Throwable;

}
