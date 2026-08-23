package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import jakarta.ejb.Local;

/**
 * Clasificación masiva de productos de una carga TXT (§6.4 y §6.5 del plan de
 * carga automática desde el SRI).
 *
 * <p>
 * <b>Para qué existe.</b> El registro de una factura de compra autocrea en el
 * grupo POR CLASIFICAR todo producto que no reconozca por nombre, y después el
 * propio registro se bloquea con {@code PRODUCTOS_SIN_CLASIFICAR} hasta que
 * alguien les dé un grupo con cuenta contable. Con 50 documentos en un lote eso
 * son veinte facturas paradas. Estos dos métodos son la salida: uno lista todo
 * lo pendiente de la carga en un viaje, el otro lo reasigna en un viaje.
 * </p>
 *
 * <p>
 * <b>Qué NO es.</b> No es {@code crearProductosYRegistrar}. Ese solo <i>crea</i>
 * los productos que no existen por nombre, y como el registro ya los había
 * autocreado en POR CLASIFICAR, no reasigna nunca nada — es el defecto 2 de la
 * §9, y es justo el que aquí se cierra. Aquel método y su endpoint siguen en
 * pie por compatibilidad; este trabaja sobre productos <b>que ya existen</b>.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Local
public interface ClasificacionProductosLoteService {

    /**
     * §6.5 — Productos en POR CLASIFICAR que aparecen en los documentos de una
     * carga, con los comprobantes que los usan.
     *
     * <p>
     * Solo mira los documentos de la carga que ya tienen factura de compra
     * creada ({@code tipoTablaDestino = FACTURA_COMPRA} con
     * {@code idDocumentoBD}): antes de eso no hay detalle ni sustentos de donde
     * salgan productos. Si no hay ninguno devuelve la lista vacía, nunca un 404.
     * </p>
     *
     * @param idCargaTxt : Id de la carga TXT
     * @return           : Mapa con idCargaTxt y productos
     *                     [{id, nombre, codigo, grupoActual, documentos[]}]
     * @throws Throwable : Solo ante un fallo de base
     */
    Map<String, Object> productosSinClasificarLote(Long idCargaTxt) throws Throwable;

    /**
     * §6.4 — Asigna grupo a productos que ya existen, en una sola transacción.
     *
     * <p>
     * Un {@code idProducto} que no exista se acumula en {@code noEncontrados} y
     * no interrumpe el resto: el frontend arma la lista desde
     * {@link #productosSinClasificarLote(Long)} y entre esa consulta y el envío
     * alguien pudo borrar una fila. Un grupo inexistente o de otra empresa sí
     * corta, porque es un error de configuración que afecta a todo el envío y
     * dejarlo pasar mezclaría empresas.
     * </p>
     *
     * @param idEmpresa    : Id de la empresa contable dueña de los grupos
     * @param asignaciones : Lista de {@code {idProducto, idGrupo}}
     * @return             : Mapa con actualizados y noEncontrados
     * @throws Throwable   : IncomeException si el envío viene vacío o mal formado,
     *                       o si un grupo no existe o no es de la empresa
     */
    Map<String, Object> clasificarProductosLote(Long idEmpresa,
            List<Map<String, Object>> asignaciones) throws Throwable;
}
