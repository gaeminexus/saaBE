package com.saa.ejb.rhh.service;

import java.time.LocalDate;

import com.saa.model.rhh.SalidaOficial;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Salidas oficiales al SRI, al IESS y al Ministerio del Trabajo.</p>
 *
 * <h3>No se duplican los datos</h3>
 *
 * <p>Las filas del RDEP y del formulario 107 <b>ya estan persistidas</b> en <code>RNGL</code>,
 * <code>ACMN</code> y <code>LQBS</code>, y los casilleros salen de <code>CPNMRDEP</code>,
 * <code>CPNMF107</code> y <code>CPNMIESS</code>. Regenerar un archivo es determinista, asi que
 * copiar los datos a otra tabla solo crearia una segunda verdad que mantener.</p>
 *
 * <p>Lo que no existia en ninguna parte es <b>el hecho de la presentacion</b>: cuando se genero,
 * quien, con que hash, y sobre todo si ya se presento al organismo y con que numero de
 * comprobante. Eso es lo unico que persiste <code>RHH.SLOF</code>.</p>
 *
 * <h3>La idempotencia es del servicio</h3>
 *
 * <p>No de un unique: <code>SLOFMESS</code> y <code>MPLDCDGO</code> son nulos en las salidas
 * anuales y consolidadas, y Oracle no considera duplicadas dos filas donde alguna columna de la
 * clave es nula. Cada generador busca la salida de su periodo y la actualiza si existe.</p>
 */
@Local
public interface GeneracionSalidasOficialesService {

    /**
     * Genera el XML del RDEP del ejercicio para el DIMM.
     *
     * <p>Un registro por empleado con relacion de dependencia, con los casilleros de
     * <code>CPNMRDEP</code>. Registra la generacion en <code>SLOF</code> y devuelve el
     * contenido.</p>
     *
     * @param idEmpresa		: Id de la empresa
     * @param anio			: Ejercicio fiscal
     * @param usuario		: Usuario que ejecuta
     * @return				: El XML del RDEP
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    byte[] generarRdep(Long idEmpresa, Integer anio, String usuario) throws Throwable;

    /**
     * Registra que una salida se genero, o actualiza el registro si ya existia.
     *
     * <p>Lo usan los generadores de archivo y tambien los reportes, que se producen por
     * <code>/rest/rprt/generar</code> pero deben dejar constancia igual.</p>
     *
     * @param idEmpresa		: Id de la empresa
     * @param tipoSalida	: Detalle del rubro RHH_TIPO_SALIDA_OFICIAL
     * @param anio			: Ejercicio fiscal
     * @param mes			: Mes, o null en las anuales
     * @param idEmpleado	: Id del empleado, o null en las consolidadas
     * @param nombreArchivo	: Nombre del archivo generado
     * @param contenido		: Contenido, para calcular el hash; admite null
     * @param usuario		: Usuario que ejecuta
     * @return				: La salida registrada
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    SalidaOficial registrarGeneracion(Long idEmpresa, int tipoSalida, Integer anio, Integer mes,
            Long idEmpleado, String nombreArchivo, byte[] contenido, String usuario) throws Throwable;

    /**
     * Registra que una salida se presento al organismo.
     *
     * <p><b>La fecha de presentacion no sustituye a la de generacion.</b> La primera la pone el
     * sistema al generar; esta la escribe una persona cuando el organismo recibe, junto con el
     * numero de comprobante. Una salida generada y no presentada es el estado normal durante
     * dias.</p>
     *
     * @param idSalida				: Id de la salida
     * @param fechaPresentacion		: Fecha en que el organismo recibio
     * @param numeroComprobante		: Numero devuelto por el organismo
     * @param usuario				: Usuario que registra
     * @return						: La salida actualizada
     * @throws Throwable			: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    SalidaOficial registrarPresentacion(Long idSalida, LocalDate fechaPresentacion,
            String numeroComprobante, String usuario) throws Throwable;

}
