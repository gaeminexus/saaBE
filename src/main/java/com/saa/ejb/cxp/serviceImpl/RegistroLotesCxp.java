package com.saa.ejb.cxp.serviceImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

/**
 * Qué lotes de CXP están corriendo ahora mismo, por carga TXT.
 *
 * <p>
 * <b>Sin tabla, y es deliberado</b> (§5.1 del plan). El progreso real se calcula
 * en vivo contando documentos; lo único que no se puede deducir de la base es
 * "hay alguien procesando esto en este instante", y eso vive aquí, en memoria.
 * Si WildFly se reinicia a media ejecución el indicador se limpia solo y el
 * usuario vuelve a pulsar el botón: la operación es idempotente. Una tabla de
 * lotes costaría seis archivos por el estándar de capas y no diría nada que los
 * contadores en vivo no digan mejor.
 * </p>
 *
 * <p>
 * <b>Concurrencia gestionada por el bean</b>: el {@code ConcurrentHashMap} ya
 * da la atomicidad que hace falta, y el bloqueo de escritura que el contenedor
 * pondría por omisión serializaría también las lecturas del endpoint de
 * progreso, que el frontend consulta cada dos segundos por cada carga abierta.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class RegistroLotesCxp {

    /** Lote de descarga de XML desde el SRI (fase 1). */
    public static final String LOTE_DESCARGA = "DESCARGA";

    /** Lote de registro y contabilización (fase 3). */
    public static final String LOTE_REGISTRO = "REGISTRO";

    private final ConcurrentHashMap<Long, String> enCurso = new ConcurrentHashMap<>();

    /**
     * Bloqueantes estructurados del último lote de registro: carga →
     * (documento → lista de {@code {tipo, detalle, productos?, grupos?}}).
     *
     * <p>
     * Los llena el lote y los sirve §6.3. <b>No se recalculan por consulta</b>:
     * eso obligaría a correr las validaciones de {@code registrarBD} sobre las
     * 50 filas de la carga cada dos segundos. Viven mientras WildFly esté
     * arriba, que cubre la sesión de trabajo; tras un reinicio se vacían y al
     * usuario le queda el texto en {@code observacion}. Sin DDL.
     * Ver §11 decisión 8.
     * </p>
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<Long, List<Map<String, Object>>>>
            bloqueantesPorCarga = new ConcurrentHashMap<>();

    /**
     * Avance del lote en curso: carga → (tamaño de su lista de trabajo, cuántos
     * lleva atendidos).
     *
     * <p>
     * <b>Lo lleva el orquestador, no se deriva de la base</b> (§11 decisión 16).
     * El orquestador es la única autoridad sobre su propio avance: sabe cuántos
     * documentos va a tocar porque él armó la lista, y sabe cuántos lleva porque
     * los está procesando. Derivarlo del estado de DCXP obliga a inventar una
     * consulta de "lo que falta" que tiene que coincidir con la lista de trabajo,
     * y en el lote de registro no puede coincidir: la lista incluye a los que ya
     * tienen observación —para reintentar lo que el usuario destrabó— y el avance
     * necesita excluirlos. Ese desajuste hacía que la barra marcara 100 % desde
     * el primer segundo al reintentar un lote con documentos bloqueados.
     * </p>
     *
     * <p>
     * Si WildFly se reinicia a media corrida esto se limpia solo, los tres
     * números vuelven al reposo y el usuario relanza el lote, que es idempotente.
     * </p>
     */
    private final ConcurrentHashMap<Long, AvanceLote> avancePorCarga = new ConcurrentHashMap<>();

    /** Tamaño de la lista de trabajo y cuántos de ella llevan desenlace. */
    private static class AvanceLote {
        private final long total;
        private final AtomicLong procesados = new AtomicLong(0L);
        AvanceLote(long total) { this.total = total; }
    }

    /**
     * Reserva la carga para un lote. Es la operación que decide el 409 del
     * endpoint: dos clics seguidos, o dos usuarios a la vez, y solo el primero
     * se lleva la reserva.
     *
     * @param idCargaTxt : Id de la carga TXT
     * @param tipoLote   : {@link #LOTE_DESCARGA} o {@link #LOTE_REGISTRO}
     * @return           : true si quedó reservada; false si ya había un lote en curso
     */
    public boolean reservar(Long idCargaTxt, String tipoLote) {
        boolean reservada = enCurso.putIfAbsent(idCargaTxt, tipoLote) == null;
        System.out.println("=== RegistroLotesCxp.reservar idCargaTxt=" + idCargaTxt
                + " tipoLote=" + tipoLote + " → " + (reservada ? "reservada" : "YA EN CURSO"));
        return reservada;
    }

    /**
     * Libera la carga. La llama el orquestador en su {@code finally}, y el REST
     * si no llegó a lanzar el proceso asíncrono.
     *
     * @param idCargaTxt : Id de la carga TXT
     */
    public void liberar(Long idCargaTxt) {
        String tipoLote = enCurso.remove(idCargaTxt);
        AvanceLote avance = avancePorCarga.remove(idCargaTxt);
        System.out.println("=== RegistroLotesCxp.liberar idCargaTxt=" + idCargaTxt
                + " tipoLote=" + tipoLote
                + (avance != null ? " avance=" + avance.procesados.get() + "/" + avance.total : ""));
    }

    // =====================================================================
    // Avance del lote en curso (§11 decisión 16)
    // =====================================================================

    /**
     * Declara el tamaño de la lista de trabajo. Lo llama el orquestador en
     * cuanto la arma, antes de procesar el primer documento.
     *
     * @param idCargaTxt : Id de la carga TXT
     * @param total      : Cuántos documentos va a tocar este lote
     */
    public void iniciarAvance(Long idCargaTxt, long total) {
        avancePorCarga.put(idCargaTxt, new AvanceLote(total));
        System.out.println("=== RegistroLotesCxp.iniciarAvance idCargaTxt=" + idCargaTxt
                + " total=" + total);
    }

    /**
     * Suma uno al avance. Se llama por cada documento con desenlace, sea cual
     * sea: registrado, bloqueado, omitido o en error. Todos fueron atendidos.
     *
     * @param idCargaTxt : Id de la carga TXT
     */
    public void sumarProcesado(Long idCargaTxt) {
        AvanceLote avance = avancePorCarga.get(idCargaTxt);
        if (avance != null) avance.procesados.incrementAndGet();
    }

    /**
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Tamaño de la lista de trabajo del lote en curso; 0 si no hay
     */
    public long totalDelLote(Long idCargaTxt) {
        AvanceLote avance = avancePorCarga.get(idCargaTxt);
        return avance != null ? avance.total : 0L;
    }

    /**
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Cuántos de esa lista ya tienen desenlace; 0 si no hay lote
     */
    public long procesadosDelLote(Long idCargaTxt) {
        AvanceLote avance = avancePorCarga.get(idCargaTxt);
        return avance != null ? avance.procesados.get() : 0L;
    }

    /**
     * @param idCargaTxt : Id de la carga TXT
     * @return           : DESCARGA, REGISTRO, o null si no hay lote corriendo
     */
    public String tipoLote(Long idCargaTxt) {
        return enCurso.get(idCargaTxt);
    }

    // =====================================================================
    // Caché de bloqueantes del lote de registro (§11 decisión 8)
    // =====================================================================

    /**
     * Vacía los bloqueantes de una carga. Lo hace el lote de registro al
     * arrancar: lo que quedó de la corrida anterior ya no describe el estado
     * actual —el usuario clasificó productos o configuró cuentas entre una y
     * otra—, y dejarlo visible sería mostrar un diagnóstico caducado.
     *
     * @param idCargaTxt : Id de la carga TXT
     */
    public void limpiarBloqueantes(Long idCargaTxt) {
        ConcurrentHashMap<Long, List<Map<String, Object>>> previos =
                bloqueantesPorCarga.remove(idCargaTxt);
        System.out.println("=== RegistroLotesCxp.limpiarBloqueantes idCargaTxt=" + idCargaTxt
                + " descartados=" + (previos != null ? previos.size() : 0));
    }

    /**
     * Guarda los bloqueantes de un documento que el lote no pudo registrar.
     *
     * @param idCargaTxt     : Id de la carga TXT
     * @param idDocumentoCxp : Id del documento bloqueado
     * @param bloqueantes    : Lista {@code [{tipo, detalle, productos?, grupos?}]}
     */
    public void guardarBloqueantes(Long idCargaTxt, Long idDocumentoCxp,
            List<Map<String, Object>> bloqueantes) {
        if (idCargaTxt == null || idDocumentoCxp == null
                || bloqueantes == null || bloqueantes.isEmpty())
            return;

        bloqueantesPorCarga
                .computeIfAbsent(idCargaTxt, k -> new ConcurrentHashMap<>())
                .put(idDocumentoCxp, bloqueantes);
    }

    /**
     * Bloqueantes de todos los documentos de una carga, para que el endpoint de
     * progreso los lea de un vistazo en vez de consultar documento por documento.
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Mapa documento → bloqueantes; vacío si no corrió ningún
     *                     lote de registro, o si WildFly se reinició desde entonces
     */
    public Map<Long, List<Map<String, Object>>> bloqueantesDeLaCarga(Long idCargaTxt) {
        ConcurrentHashMap<Long, List<Map<String, Object>>> deLaCarga =
                bloqueantesPorCarga.get(idCargaTxt);
        return deLaCarga != null
                ? Collections.unmodifiableMap(deLaCarga)
                : Collections.<Long, List<Map<String, Object>>>emptyMap();
    }
}
