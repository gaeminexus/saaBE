package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DocumentoSeguro;

import jakarta.ejb.Local;

@Local
public interface DocumentoSeguroDaoService extends EntityDao<DocumentoSeguro> {

    /**
     * Documentos filtrados por tipo de seguro, estado y clase, los tres opcionales. Es la
     * consulta de {@code GET /posg/listar} (contrato §8).
     *
     * @param tipoSeguro 1/2/3; null = todos
     * @param estado     1-5; null = todos
     * @param clase      1/2/3; null = todos
     * @return documentos ordenados por código descendente
     * @throws Throwable Si ocurre un error
     */
    List<DocumentoSeguro> selectByFiltros(Long tipoSeguro, Long estado, Long clase) throws Throwable;

    /**
     * El documento con esa clave de acceso del SRI, normalizada ({@code UPPER(TRIM(...))}), en
     * toda la tabla. Para el chequeo {@code CLAVE_ACCESO_DUPLICADA} de {@code PUT
     * /posg/{id}/documento} (contrato §4).
     *
     * @param claveAcceso clave tal como la escribió el usuario (se normaliza acá)
     * @return el documento con esa clave, o null si no existe
     * @throws Throwable Si ocurre un error
     */
    DocumentoSeguro selectByClaveAccesoNormalizada(String claveAcceso) throws Throwable;

    /**
     * Las notas (ND/NC) de una factura madre, de cualquier estado. Para armar la "familia" de un
     * documento (novedades §7, choque {@code CUOTA_YA_CUBIERTA} §5, anular §6).
     *
     * @param idPadre código de la factura madre
     * @return notas ordenadas por código ascendente
     * @throws Throwable Si ocurre un error
     */
    List<DocumentoSeguro> selectNotasByPadre(Long idPadre) throws Throwable;
}
