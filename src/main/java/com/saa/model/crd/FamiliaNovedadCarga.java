package com.saa.model.crd;

import java.util.Arrays;
import java.util.List;

import com.saa.rubros.ASPNovedadesCargaArchivo;

/**
 * Clasifica una novedad de carga Petro (CRD.NVPC) en una de tres familias, para que la
 * pantalla de novedades pueda distinguirlas sin reimplementar el criterio (2026-08-31, decisión
 * del usuario).
 *
 * <p><b>Único punto de entrada.</b> Lo evalúa {@code CargaArchivoPetroServiceImpl} para decidir
 * si una novedad bloquea el procesamiento de la carga completa, y lo expone
 * {@code NovedadParticipeCarga#getFamilia()} para que el frontend lo lea de la propia novedad —
 * la MISMA función en los dos lugares, nunca una copia que pueda divergir.</p>
 *
 * <p><b>{@link #TIPOS_QUE_EXIGEN_AFECTACION} es el único catálogo de "tipos que pueden
 * bloquear".</b> Un tipo que no está en esa lista SIEMPRE es {@link #INFORMATIVA}, sin importar
 * el monto — este refactor no ensancha ni achica qué tipos pueden llegar a bloquear una carga,
 * solo agrega el matiz del signo DENTRO de los que ya podían hacerlo.</p>
 */
public enum FamiliaNovedadCarga {

    /** Sobra dinero sin destino, o no se sabe dónde aplicarlo — bloquea el procesamiento. */
    BLOQUEANTE,

    /** Falta dinero (el partícipe descontó menos de lo esperado) — gestión de cobranza, no
     * bloquea: el valor recibido sí tiene un destino claro (la cuota/aporte ya identificado). */
    COBRANZA,

    /** Dentro de tolerancia, cuota con otra fecha, o resultado OK — no requiere acción. */
    INFORMATIVA;

    /**
     * Los mismos 14 tipos que, hasta 2026-08-31, exigían afectación manual sin distinguir
     * dirección ({@code CargaArchivoPetroServiceImpl.NOVEDADES_REQUIEREN_AFECTACION_MANUAL},
     * movido acá para que sea la ÚNICA lista — antes de este cambio vivía solo en el Impl).
     *
     * <p><b>Esta lista es SOLO para "¿bloquea?", no para "¿de qué familia es?"</b> (corregido
     * 2026-08-31, mismo día — un tipo fuera de esta lista nunca puede salir {@link #BLOQUEANTE},
     * pero SÍ puede salir {@link #COBRANZA}: "no sé dónde aplicar esto" y "esto no se cobró" son
     * preguntas independientes. Ver el javadoc de {@link #clasificar}.</p>
     */
    private static final List<Long> TIPOS_QUE_EXIGEN_AFECTACION = Arrays.asList(
        (long) ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.CODIGO_ROL_DUPLICADO,
        (long) ASPNovedadesCargaArchivo.NOMBRE_ENTIDAD_DUPLICADO,
        (long) ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE,
        (long) ASPNovedadesCargaArchivo.DESCUENTOS_ADICIONALES,
        (long) ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO,
        (long) ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.MULTIPLES_PRESTAMOS_ACTIVOS,
        (long) ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA,
        (long) ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE,
        (long) ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.MULTIPLES_REGISTROS_HISTORIAL_SUELDO,
        (long) ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
        (long) ASPNovedadesCargaArchivo.APORTE_MONTO_INCONSISTENTE
    );

    /**
     * Tolerancia de redondeo ($1) para decidir si una diferencia negativa es "falta plata de
     * verdad" (COBRANZA) o solo ruido de centavos (INFORMATIVA, como {@code
     * DIFERENCIA_MENOR_UN_DOLAR}). ÚNICA definición del proyecto (2026-08-31): {@code
     * CargaArchivoPetroServiceImpl.TOLERANCIA} lee de acá — el modelo no puede depender de
     * {@code ejb}, así que la dirección de la dependencia solo puede ser esta.
     */
    public static final double TOLERANCIA = 1.0;

    /**
     * Dos preguntas independientes, no una (corregido 2026-08-31 — la primera versión hacía
     * depender la familia de la misma lista que decide el bloqueo, y eso dejaba `SIN_DESCUENTOS`/
     * `APORTE_VALORES_CERO` — el caso más puro de "no se cobró" — clasificadas INFORMATIVA solo
     * por no estar en los 14):
     *
     * <ol>
     * <li><b>¿Bloquea?</b> Solo si {@code tipoNovedad} está en
     * {@link #TIPOS_QUE_EXIGEN_AFECTACION} Y {@code montoDiferencia} es {@code null} o &gt;= 0
     * (sobra, o no hay dato para decir lo contrario) → {@link #BLOQUEANTE}.</li>
     * <li><b>¿Es cobranza?</b> Cualquier tipo, esté o no en la lista de bloqueo, con
     * {@code montoDiferencia} negativa MÁS ALLÁ de {@link #TOLERANCIA} (falta plata de verdad,
     * no redondeo) → {@link #COBRANZA}. Por eso NO alcanza con {@code < 0}: eso incluiría
     * {@code DIFERENCIA_MENOR_UN_DOLAR}/{@code APORTE_DIFERENCIA_MENOR_UN_DOLAR}, que están
     * DENTRO de tolerancia a propósito y llenarían la bandeja de cobranza de centavos.</li>
     * <li>Ninguna de las dos → {@link #INFORMATIVA}.</li>
     * </ol>
     *
     * @param tipoNovedad     Código del rubro {@code ASPNovedadesCargaArchivo}
     * @param montoDiferencia {@code NovedadParticipeCarga.montoDiferencia}
     *                        ({@code montoRecibido - montoEsperado}, con signo) — {@code null}
     *                        cuando la novedad no lleva montos (p.ej. las estructurales de
     *                        Fase 1) o cuando falta uno de los dos montos.
     */
    public static FamiliaNovedadCarga clasificar(Long tipoNovedad, Double montoDiferencia) {
        boolean enListaDeBloqueo = tipoNovedad != null && TIPOS_QUE_EXIGEN_AFECTACION.contains(tipoNovedad);
        if (enListaDeBloqueo && (montoDiferencia == null || montoDiferencia.doubleValue() >= 0.0)) {
            return BLOQUEANTE;
        }
        if (montoDiferencia != null && montoDiferencia.doubleValue() < -TOLERANCIA) {
            return COBRANZA;
        }
        return INFORMATIVA;
    }
}
