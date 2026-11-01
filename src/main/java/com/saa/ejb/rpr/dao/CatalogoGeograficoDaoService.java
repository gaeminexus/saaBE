package com.saa.ejb.rpr.dao;

import jakarta.ejb.Local;

/**
 * D-038/D-039 — códigos geográficos de las estructuras G (tablas 6 provincia, 7 cantón y 50 parroquia de la
 * Superintendencia, división política del INEC) desde los catálogos del SAA: {@code CRD.PRVN.PRVNCDAL},
 * {@code CRD.CDDD.CDDDCDAL} y {@code CRD.PRRQ.PRRQCDEX}. Nunca valores fijos en los generadores.
 */
@Local
public interface CatalogoGeograficoDaoService {

    /** Código de provincia de 2 dígitos (tabla 6) por nombre o código; null si no está en el catálogo. */
    String codigoProvincia(String nombreOCodigo) throws Throwable;

    /** Código de cantón de 2 dígitos (tabla 7) dentro de la provincia (nombre o código); null si no está. */
    String codigoCanton(String provincia, String nombreOCodigoCanton) throws Throwable;

    /**
     * Ubicación de la dirección vigente de la entidad (CRD.DRCC estado 1 → PRRQ → CDDD → PRVN):
     * {provincia (2), cantón (2), parroquia (2, PRRQCDEX; null si no está cargado)}; null si no tiene dirección.
     */
    String[] ubicacionEntidad(Long codigoEntidad) throws Throwable;
}
