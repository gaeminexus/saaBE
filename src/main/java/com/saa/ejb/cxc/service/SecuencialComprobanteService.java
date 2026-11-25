package com.saa.ejb.cxc.service;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 12 (VCN #6): única fuente del secuencial de los comprobantes electrónicos (factura, nota de crédito, nota de débito, liquidación de compra,
 * retención v1 y V2). Antes cada servicio tenía su copia de {@code obtenerSecuencial}: leía la fila de CBR.NXPE sin bloqueo y escribía «leído + 1»,
 * de modo que dos emisiones simultáneas del mismo punto leían el mismo número, las dos lo usaban y el contador avanzaba una sola vez.
 */
@Local
public interface SecuencialComprobanteService {

    /**
     * Devuelve el secuencial a usar (9 dígitos) y deja el contador en el siguiente. Bloquea la fila del punto y tipo (SELECT … FOR UPDATE, con tiempo
     * de espera) y relee su valor ya con el bloqueo, así que dos llamadas concurrentes se serializan y reciben números distintos. Se une a la transacción
     * del llamador: el bloqueo se mantiene hasta su commit o rollback (si la emisión falla, el número no se consume).
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    String siguienteSecuencial(Long idPtoEmision, String tipoDoc) throws Exception;
}
