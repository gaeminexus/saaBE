/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Titular;

import jakarta.ejb.Local;
import java.util.List;

@Local
public interface TitularDaoService extends EntityDao<Titular> {

    /**
     * Busca titulares cuyo nombre sea similar al dado usando UTL_MATCH.JARO_WINKLER_SIMILARITY.
     * Tolerancia fija de 90% para cubrir variaciones menores (tildes, abreviaciones, etc.).
     * @param nombre Nombre a buscar (se recomienda pasar normalizado/sin tildes)
     * @return Lista de Titulares con similitud > 90
     */
    List<Titular> buscarPorNombreSimilar(String nombre) throws Throwable;

    /**
     * Calcula la similitud Jaro-Winkler entre dos cadenas usando Oracle UTL_MATCH y
     * UTL_I18N.TRANSLITERATE para eliminar tildes antes de comparar.
     * @param a Primer nombre
     * @param b Segundo nombre
     * @return Valor entero 0-100 de similitud (100 = idénticos)
     */
    int calcularSimilitudNombre(String a, String b) throws Throwable;

    /**
     * Busca un titular por identificación y estado exactos. En este modelo no
     * hay tabla de proveedores: un mismo titular acumula el rol de cliente y/o
     * proveedor, así que el índice único de BD (UK_TTLR_IDNT_ESTD) es sobre
     * (identificacion, estado) — puede existir a la vez un titular ACTIVO y
     * otro INACTIVO con la misma identificación, pero no dos ACTIVOS.
     * @param identificacion : Cédula/RUC/pasaporte a buscar
     * @param estado         : Estado exacto (normalmente Estado.ACTIVO)
     * @return : El titular encontrado, o null si no existe
     */
    Titular selectByIdentificacion(String identificacion, Long estado) throws Throwable;

}
