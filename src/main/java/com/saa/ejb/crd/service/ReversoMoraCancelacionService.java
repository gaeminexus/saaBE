package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.ResultadoReversoMora;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 15 (D-195): un préstamo que se cancela (precancelación, acuerdo de condonación, novación o reestructuración) sin cobrar la mora que ya
 * se devengó deja esa mora en 1.4.02 y su provisión en 1.4.99.05. Este servicio calcula, DENTRO del asiento del evento, lo que queda y lo
 * reversa: D 5.1.02 / H 1.4.02 (devengo) y D 1.4.99.05 / H 4.7.05.10 (provisión), por tipo de préstamo, y deja el rastro en el libro por cuota
 * (CRD.MVIC tipos 6 y 3, origen {@link #ORIGEN_EVENTO}), que se anula cuando se anula el evento.
 *
 * <p><b>Punto de cálculo:</b> «pendiente después de cobrar y condonar». Las líneas se calculan justo antes de generar el asiento del evento,
 * cuando ya están grabados los pagos del evento (PGPR) y, en el acuerdo, el acuerdo ya está APLICADO (su condonación ya cuenta en DACC). Lo que
 * el evento cobra y condona por la mora se resta; lo que sobra es lo que se reversa. La provisión se reversa completa: un préstamo cancelado no
 * puede conservar provisión de mora y los reversos posteriores del mismo evento ({@code reversarPorPagos}, {@code condonarProvision}) están
 * acotados al saldo, así que no la duplican.</p>
 *
 * <p>Activo solo con {@code saa.cartera.topeProvisionMoraDesde} (como el tope del lote 7): sin la propiedad, o con una fecha anterior, no hay
 * líneas y el asiento es el de siempre.</p>
 */
@Local
public interface ReversoMoraCancelacionService {

    /** Origen del libro para los eventos (precancelación, acuerdo, cobro con precancelación): {@code idOrigen} = el evento. */
    String ORIGEN_EVENTO = "CANCELACION_MORA";

    /** Origen del libro para la sustitución (novación/reestructuración): {@code idOrigen} = el préstamo original (el evento se crea después del asiento). */
    String ORIGEN_SUSTITUCION = "CANCELACION_MORA_SUST";

    /**
     * Calcula las líneas del reverso y el detalle por cuota. No graba nada.
     *
     * @param idEmpresa                 empresa contable
     * @param prestamo                  el préstamo que se cancela
     * @param fecha                     fecha del evento (corte del cálculo y fecha contable de los movimientos)
     * @param pagosDelEvento            PGPR del evento ya grabados (su mora cobrada se descuenta); puede ser null o vacío
     * @param moraCobradaFueraDePagos   mora que el evento cobra SIN PGPR (la sustitución: la que lleva el valor de sustitución); 0 si no aplica
     * @param origen                    {@link #ORIGEN_EVENTO} o {@link #ORIGEN_SUSTITUCION}
     * @param idOrigen                  el evento (o el préstamo original); si ya hay movimientos vigentes con ese origen no se vuelve a reversar
     * @param prefijo                   texto que encabeza la descripción de las líneas
     * @return el resultado (vacío si la propiedad no está activa o no hay nada que reversar)
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    ResultadoReversoMora calcular(Long idEmpresa, Prestamo prestamo, LocalDate fecha, List<PagoPrestamo> pagosDelEvento,
            double moraCobradaFueraDePagos, String origen, Long idOrigen, String prefijo) throws Throwable;

    /**
     * Escribe en el libro (CRD.MVIC) los movimientos del reverso calculado, con el asiento ya generado. Tipo 6 (reverso de devengo) y tipo 3
     * (reverso de provisión), componente mora.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void registrar(ResultadoReversoMora resultado, Long idAsiento, LocalDate fechaContable, String usuario) throws Throwable;

    /**
     * Anula los movimientos de un evento (se llama al anular el evento o el cobro). Devuelve cuántos anuló.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    int anularPorEvento(Long idEvento) throws Throwable;
}
