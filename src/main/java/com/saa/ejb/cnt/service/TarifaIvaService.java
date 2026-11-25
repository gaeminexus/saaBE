package com.saa.ejb.cnt.service;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 13 (VCN #11): la tarifa de IVA y su código SRI salen del catálogo (CBR.TSRI, tabla 17 del SRI: en esa tabla {@code TSRI.LSRI} guarda el CÓDIGO de tabla),
 * no de un {@code switch} con porcentajes fijos. Antes un porcentaje que no estaba en el {@code switch} (p. ej. el 13 %) pasaba como si fuera el código y el
 * asiento buscaba una cuenta de IVA inexistente.
 *
 * <p>TSRI no tiene columnas de vigencia: se usan las filas ACTIVAS (ESTADO = 1); con más de una fila activa para el mismo porcentaje gana el código menor. No hay
 * búsqueda «por fecha del documento» mientras el catálogo no tenga vigencia.</p>
 */
@Local
public interface TarifaIvaService {

    /** Tabla 17 del SRI (tarifa del IVA) tal como la guarda CBR.TSRI.LSRI. */
    String TABLA_TARIFA_IVA = "17";

    /**
     * Código SRI de la tarifa con ese porcentaje (0 → «0»; 12 → «2»; 15 → «4»…). {@code null} equivale a 0 %. Lanza {@code IncomeException} con un mensaje
     * claro si el porcentaje no está en el catálogo activo (nunca devuelve el porcentaje como código).
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    String codigoPorPorcentaje(Long porcentaje) throws Exception;

    /**
     * Porcentaje de IVA «general» que se muestra en el RIDE: el del propio documento ({@code porcentajeDocumento}, el mayor de sus líneas) si es mayor que 0; si
     * no, la tarifa general activa del catálogo (CBR.TSRI, tabla 614); si tampoco hay, 15.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    int porcentajeGeneral(Long porcentajeDocumento);
}
