package com.saa.ejb.cxc.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.PathRetencionV2;
import jakarta.ejb.Local;

@Local
public interface PathRetencionV2DaoService extends EntityDao<PathRetencionV2> {

	/**
	 * Recupera la fila de {@code PathRetencionV2} del XML FIRMADO (alterno = 3) de una
	 * retención V2 -- la más reciente si hay más de una, porque {@code autorizarRetencionV2}
	 * inserta una fila nueva en cada llamada (nunca sobreescribe). Usado por el reenvío manual
	 * al SRI (ÍTEM 12, docs/logica-negocio/cxc/API-REENVIAR-RETENCION-AL-SRI.md): el XML
	 * firmado es el que el SRI ya vio, así que este método SOLO busca, nunca regenera.
	 *
	 * @param idRetencion : codigo de la RetencionV2
	 * @return : la fila de PathRetencionV2 con alterno=3 más reciente, o null si no existe
	 * @throws Throwable : Excepcion
	 */
	PathRetencionV2 selectUltimoFirmadoByRetencion(Long idRetencion) throws Throwable;

}
