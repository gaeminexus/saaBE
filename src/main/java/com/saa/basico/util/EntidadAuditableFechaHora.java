package com.saa.basico.util;

import java.time.LocalDateTime;

/**
 * Entidad auditable cuya columna de fecha de registro es {@code TIMESTAMP} o
 * {@code DATE} con hora y se mapea como {@link LocalDateTime}.
 *
 * <p>
 * Una entidad se suma escribiendo {@code implements Serializable,
 * EntidadAuditableFechaHora} en su declaracion: los dos metodos ya existen con
 * esa firma exacta en toda entidad generada con la plantilla de la casa.
 * </p>
 *
 * @see EntidadAuditable
 */
public interface EntidadAuditableFechaHora extends EntidadAuditable {

    /**
     * Fecha y hora en que se registro la fila.
     *
     * @return : Fecha y hora de registro, o nulo si todavia no se sello
     */
    LocalDateTime getFechaRegistro();

    /**
     * Asigna la fecha y hora de registro. El DAO generico solo la escribe al
     * insertar y solo cuando esta en nulo: un valor ya puesto por el servicio de
     * proceso o por la peticion nunca se sobreescribe.
     *
     * @param fechaRegistro : Fecha y hora de registro
     */
    void setFechaRegistro(LocalDateTime fechaRegistro);
}
