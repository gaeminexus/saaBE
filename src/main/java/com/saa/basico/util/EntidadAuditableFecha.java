package com.saa.basico.util;

import java.time.LocalDate;

/**
 * Entidad auditable cuya columna de fecha de registro es {@code DATE} y se mapea
 * como {@link LocalDate}.
 *
 * <p>
 * Una entidad se suma escribiendo {@code implements Serializable,
 * EntidadAuditableFecha} en su declaracion: los dos metodos ya existen con esa
 * firma exacta en toda entidad generada con la plantilla de la casa.
 * </p>
 *
 * @see EntidadAuditable
 */
public interface EntidadAuditableFecha extends EntidadAuditable {

    /**
     * Fecha en que se registro la fila.
     *
     * @return : Fecha de registro, o nulo si todavia no se sello
     */
    LocalDate getFechaRegistro();

    /**
     * Asigna la fecha de registro. El DAO generico solo la escribe al insertar y
     * solo cuando esta en nulo: un valor ya puesto por el servicio de proceso o
     * por la peticion nunca se sobreescribe.
     *
     * @param fechaRegistro : Fecha de registro
     */
    void setFechaRegistro(LocalDate fechaRegistro);
}
