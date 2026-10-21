/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.time.LocalDateTime;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.UsuarioApp;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad UsuarioApp (CRD.USAP): credenciales de la app
 *         móvil ASOPREP.
 */
@Local
public interface UsuarioAppDaoService extends EntityDao<UsuarioApp> {

    /**
     * Busca la credencial por identificación (cédula) — el login de la app.
     *
     * @param identificacion : Identificación a buscar
     * @return                : La credencial encontrada, o {@code null} si no existe
     * @throws Throwable     : Excepcion
     */
    UsuarioApp selectByIdentificacion(String identificacion) throws Throwable;

    /**
     * Busca la credencial de un partícipe por su código de Entidad (relación 1:1).
     *
     * @param idEntidad : Código de la entidad (CRD.ENTD)
     * @return          : La credencial encontrada, o {@code null} si no existe
     * @throws Throwable : Excepcion
     */
    UsuarioApp selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Registra un intento fallido de clave en una transacción PROPIA ({@code REQUIRES_NEW}),
     * que se confirma aunque la transacción del llamador termine en rollback. Es necesario
     * porque el llamador lanza {@code IncomeException} (rollback=true) justo después, y un
     * {@code save} en su misma transacción quedaría deshecho: el contador nunca subiría y la
     * cuenta nunca se bloquearía. Por eso vive en el DAO (otro EJB) y no en el Service: un
     * método {@code REQUIRES_NEW} llamado como {@code this.metodo()} no pasa por el proxy y no
     * abre transacción.
     *
     * <p>Relee la fila con {@code PESSIMISTIC_WRITE} para que dos intentos en paralelo no lean
     * el mismo contador. Si el bloqueo anterior ya venció, reinicia el contador a 0 antes de
     * sumar. Suma 1 y, al llegar a {@code maxIntentos}, pasa la cuenta a BLOQUEADO hasta
     * {@code ahora + minutosBloqueo}. NO absorbe errores.</p>
     *
     * @param codigo         : Código (PK) de la credencial
     * @param maxIntentos    : Intentos fallidos que disparan el bloqueo
     * @param ahora          : Instante del intento
     * @param minutosBloqueo : Duración del bloqueo, en minutos
     * @return               : La credencial actualizada, o {@code null} si la fila no existe
     * @throws Throwable     : Excepcion
     */
    UsuarioApp registrarIntentoFallido(Long codigo, int maxIntentos, LocalDateTime ahora, long minutosBloqueo)
            throws Throwable;

    /**
     * Cuenta las entidades cuyo correo personal ({@code ENTD.ENTDCRPR}) es este, comparando
     * {@code lower(trim(...))}. Sirve para NO mandar un código a un correo compartido por más de
     * una entidad. Vive aquí y no en {@code EntidadDaoService} porque ese DAO es de otro equipo.
     *
     * @param correo     : Correo ya normalizado (trim + minúsculas)
     * @return           : Cantidad de entidades con ese correo personal
     * @throws Throwable : Excepcion
     */
    long contarEntidadesPorCorreoPersonal(String correo) throws Throwable;

    /**
     * Suma un intento fallido al código de recuperación vigente, en una transacción PROPIA
     * ({@code REQUIRES_NEW}) por la misma razón que {@link #registrarIntentoFallido}: el llamador
     * lanza una excepción con rollback justo después y un {@code save} en su transacción se
     * perdería (alguien podría probar el millón de códigos sin límite). Relee la fila con
     * {@code PESSIMISTIC_WRITE}. Al llegar a {@code maxIntentos} anula el código (hash, vencimiento
     * e intentos a nulo): el partícipe debe pedir otro. NO absorbe errores.
     *
     * @param codigo      : Código (PK) de la credencial
     * @param maxIntentos : Intentos que anulan el código
     * @throws Throwable  : Excepcion
     */
    void registrarIntentoCodigoRecuperacion(Long codigo, int maxIntentos) throws Throwable;
}
