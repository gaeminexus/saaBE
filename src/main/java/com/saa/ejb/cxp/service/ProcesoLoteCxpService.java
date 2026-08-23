package com.saa.ejb.cxp.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Orquestación de los lotes de CXP por carga TXT.
 *
 * <p>
 * Esta tanda implementa la <b>fase 1</b>: la descarga masiva de XML desde el
 * SRI (§6.1) y el endpoint de progreso que sirve a los dos lotes (§6.3). El
 * lote de registro y contabilización (§6.2) es la fase 3 y todavía no está.
 * </p>
 *
 * <p>
 * <b>Cómo se reparte el trabajo, y por qué.</b> El orquestador va
 * {@code @Asynchronous} y {@code NOT_SUPPORTED}: no abre transacción y sobrevive
 * a que cualquier documento falle. Cada documento se procesa llamando a
 * {@link DescargaXmlDocumentoService}, que es un <b>bean distinto</b> con
 * {@code REQUIRES_NEW}. Lo que un documento confirme queda confirmado aunque el
 * siguiente reviente. Ver §8 reglas 1, 5 y 9 del plan.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Local
public interface ProcesoLoteCxpService {

    /**
     * Reserva la carga y calcula los contadores del 202 de {@code descargarXmlLote}.
     *
     * <p>
     * Va separado del arranque del proceso asíncrono porque la reserva tiene que
     * ser atómica: es lo que decide si la petición se responde con 202 o con 409.
     * El REST llama primero a este método y solo si devuelve la reserva invoca
     * {@link #descargarXmlLote(Long, Long, Long)}.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Mapa con {@code conflicto=true}, o con idCargaTxt, total,
     *                     aProcesar, yaConXml y mensaje
     * @throws Throwable : IncomeException si la carga no existe
     */
    Map<String, Object> reservarDescargaLote(Long idCargaTxt) throws Throwable;

    /**
     * Libera la reserva. La usa el REST cuando no consiguió lanzar el proceso
     * asíncrono después de haber reservado.
     *
     * @param idCargaTxt : Id de la carga TXT
     */
    void liberarLote(Long idCargaTxt);

    /**
     * Baja del SRI el XML de todos los documentos de la carga que todavía no lo
     * tengan. Corre en segundo plano; el frontend sigue el avance por
     * {@link #progresoLote(Long)}.
     *
     * <p>
     * Salta los documentos en estado 3 y los que ya tienen {@code pathXml}, y va
     * <b>de uno en uno</b>: cincuenta peticiones simultáneas es la forma más
     * rápida de que el SRI nos corte (§8 regla 5).
     * </p>
     *
     * <p>
     * <b>No lanza.</b> Es {@code @Asynchronous void}: el contenedor se tragaría
     * cualquier excepción, así que todo lo que falla queda estampado en el
     * documento y en el log.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @param idEmpresa  : Id de la empresa contable
     * @param idUsuario  : Id del usuario que disparó el lote
     */
    void descargarXmlLote(Long idCargaTxt, Long idEmpresa, Long idUsuario);

    /**
     * Reserva la carga y calcula los contadores del 202 de {@code registrarLote}
     * (§6.2). Mismo handshake síncrono que {@link #reservarDescargaLote(Long)}:
     * un solo lote por carga, sea de descarga o de registro.
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Mapa con {@code conflicto=true}, o con idCargaTxt,
     *                     aProcesar, sinXml, yaRegistrados y mensaje
     * @throws Throwable : IncomeException si la carga no existe
     */
    Map<String, Object> reservarRegistroLote(Long idCargaTxt) throws Throwable;

    /**
     * Registra y contabiliza todos los documentos de la carga que tengan el XML
     * cargado. Corre en segundo plano; el avance se sigue por
     * {@link #progresoLote(Long)}.
     *
     * <p>
     * <b>Lista de trabajo</b>: estado 2 con {@code pathXml}, y nada más. Los
     * revertidos quedan fuera aunque conserven el XML en disco — §11 decisión 12.
     * </p>
     *
     * <p>
     * <b>Orden obligatorio</b>: Factura y Liquidación primero, después Nota de
     * Crédito y Nota de Débito, y al final las Retenciones. No es una
     * preferencia: una NC necesita que exista la factura de compra que afecta, y
     * una retención necesita la factura de venta del sustento. Con la carga de
     * uno en uno daba igual porque lo ordenaba el usuario; en lote lo tiene que
     * ordenar el backend.
     * </p>
     *
     * <p>
     * <b>No lanza.</b> Es {@code @Asynchronous void}: cada documento que falle
     * queda estampado en estado 4 y el lote sigue con el siguiente.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @param idEmpresa  : Id de la empresa contable
     * @param idUsuario  : Id del usuario que disparó el lote
     */
    void registrarLote(Long idCargaTxt, Long idEmpresa, Long idUsuario);

    /**
     * Estado de la carga para la pantalla (§6.3). Sirve a los dos lotes: el tipo
     * lo dice {@code tipoLote}, y es null cuando no hay ninguno corriendo.
     *
     * <p>
     * <b>Nunca falla por la carga.</b> Una carga inexistente devuelve los
     * contadores en cero y {@code documentos[]} vacío, no un error: el frontend
     * consulta esto cada dos segundos y solo pregunta por cargas de su propia
     * lista, así que un 404 dentro del polling no aportaría nada.
     * </p>
     *
     * <p>
     * {@code procesados} es {@code total} <b>menos los pendientes del lote en
     * curso</b>, contados con la misma consulta que arma su lista de trabajo.
     * Sin lote en curso se cuenta contra "pendiente de XML", el estado de
     * entrada del panel. Ver §11 decisión 9.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Mapa con idCargaTxt, enCurso, tipoLote, procesados, total,
     *                     contadores y documentos
     * @throws Throwable : Solo ante un fallo de base
     */
    Map<String, Object> progresoLote(Long idCargaTxt) throws Throwable;
}
