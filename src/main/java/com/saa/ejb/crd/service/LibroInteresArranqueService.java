package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ResultadoAjusteLibroProvision;
import com.saa.ejb.crd.service.dto.ResultadoSemillaDevengoInteres;
import com.saa.ejb.crd.service.dto.SolicitudLibroInteresArranque;

import jakarta.ejb.Local;

/**
 * Lote 21: arranque de los libros por cuota del motor nuevo a un corte cualquiera (parámetro, nunca fijo). Dos herramientas, sin asiento contable propio:
 * quedan ligadas al asiento manual del cuadre que ya lleva la contabilidad a esos valores.
 * <ul>
 * <li>A2 {@link #sembrarDevengoInteres}: el interés devengado y no cobrado al corte, por cuota, como saldo inicial del libro de devengo (MVIC 11).</li>
 * <li>A1 {@link #ajustarLibroProvision}: lleva el saldo provisionado de cada cuota (interés y mora) a lo devengado y no cobrado al corte (MVIC 13 y 14).</li>
 * </ul>
 * Contrato y orden de uso: docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md (Lote 21). Requiere {@code sql/406} y {@code sql/409}.
 */
@Local
public interface LibroInteresArranqueService {

    /** Previsualiza (grabar=false) o graba la semilla del libro de devengo de interés. Idempotente por {@code idAsientoReferencia}. */
    ResultadoSemillaDevengoInteres sembrarDevengoInteres(SolicitudLibroInteresArranque solicitud) throws Throwable;

    /** Anula (nunca borra) la semilla de una referencia. @return cuántos movimientos anuló */
    int anularSemilla(Long idAsientoReferencia, String usuario) throws Throwable;

    /** Previsualiza (grabar=false) o graba el ajuste del libro de provisión. Idempotente por {@code idAsientoReferencia}. */
    ResultadoAjusteLibroProvision ajustarLibroProvision(SolicitudLibroInteresArranque solicitud) throws Throwable;

    /** Anula (nunca borra) el ajuste del libro de una referencia. @return cuántos movimientos anuló */
    int anularAjusteLibro(Long idAsientoReferencia, String usuario) throws Throwable;

    /**
     * Lote 22: lo que hay que mover en el libro de provisión por cuota para que quede igual a lo devengado y no cobrado al corte (el mismo cálculo de A1,
     * sin grabar). Filas {@code [idCuota, idPrestamo, componente (Long), baja (Boolean), valor (Double), idTipoPrestamo (Long)]}.
     */
    java.util.List<Object[]> deltasLibroProvisionAlCorte(java.time.LocalDate corte) throws Throwable;

    /**
     * Lote 22: lo que hay que mover en el libro de devengo de interés (MVIC 11 y 12) para que cada cuota quede en su interés devengado y no cobrado
     * al corte. Mismo formato de filas; {@code baja} = true es un reverso (tipo 12).
     */
    java.util.List<Object[]> deltasLibroDevengoInteresAlCorte(java.time.LocalDate corte) throws Throwable;
}
