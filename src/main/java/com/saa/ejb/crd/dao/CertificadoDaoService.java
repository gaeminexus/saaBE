package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Certificado;

import jakarta.ejb.Local;

@Local
public interface CertificadoDaoService extends EntityDao<Certificado> {

    /**
     * Certificados emitidos a un partícipe, del más reciente al más antiguo (incluye los
     * anulados, que conservan su número).
     * @param idEntidad  : Código de la entidad (partícipe)
     * @return           : Listado de certificados; lista VACÍA si no tiene ninguno
     * @throws Throwable : Excepción
     */
    List<Certificado> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Certificados de un año, ordenados por número.
     * @param anio       : Año de la serie
     * @return           : Listado de certificados; lista VACÍA si no hay
     * @throws Throwable : Excepción
     */
    List<Certificado> selectByAnio(Long anio) throws Throwable;

    /**
     * Siguiente número de la serie del año: MAX(numero)+1, o 1 si el año no tiene ninguno.
     *
     * <b>Toma un lock exclusivo sobre CRD.CRTF</b> que dura hasta el fin de la transacción
     * del llamador. Llamarlo SOLO desde un método transaccional que inserte el certificado
     * en esa misma transacción: dos emisiones simultáneas se serializan aquí y el UNIQUE
     * (anio, numero) de la base es la red si algo se salta el lock.
     * @param anio       : Año de la serie
     * @return           : Siguiente número disponible
     * @throws Throwable : Excepción
     */
    Long siguienteNumero(Long anio) throws Throwable;

    // ------------------------------------------------------------------------
    // Consultas de apoyo a la emisión. Viven aquí, y no en AporteDao, para no tocar
    // DAOs compartidos: son lecturas agregadas y no bajan filas a Java.
    // ------------------------------------------------------------------------

    /**
     * ¿El partícipe tiene al menos un aporte de alguno de los tipos dados?
     * @param idEntidad  : Código de la entidad
     * @param tipos      : Códigos de CRD.TPAP (ej. 13 y 15 = jubilación patronal y su rendimiento)
     * @return           : true si existe al menos una fila de CRD.APRT con esos tipos
     * @throws Throwable : Excepción
     */
    boolean existeAporteDeTipos(Long idEntidad, List<Long> tipos) throws Throwable;

    /**
     * Primer periodo con aporte de los tipos dados, con el criterio del módulo:
     * {@code MIN(NVL(APRTPRDV, TRUNC(APRTFCTR,'MM')))}.
     * @param idEntidad  : Código de la entidad
     * @param tipos      : Códigos de CRD.TPAP (ej. 9 y 11 = jubilación y cesantía personal)
     * @return           : Primer día del periodo; null si no tiene aportes de esos tipos
     * @throws Throwable : Excepción
     */
    java.time.LocalDate primerPeriodoAporte(Long idEntidad, List<Long> tipos) throws Throwable;

    /**
     * ¿La cédula registra pagos de pensión complementaria en el histórico CRD.HPPJ?
     * (tabla sin entidad JPA; se consulta en nativo por cédula, igual que HPCS).
     * @param cedula     : Cédula del partícipe (ENTDNMID)
     * @return           : true si hay al menos una fila
     * @throws Throwable : Excepción
     */
    boolean existePagoPension(String cedula) throws Throwable;
}
