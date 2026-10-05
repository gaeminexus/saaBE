package com.saa.ejb.cxp.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.DocumentoCxp;
import jakarta.ejb.Local;
@Local
public interface DocumentoCxpDaoService extends EntityDao<DocumentoCxp> {

	/**
	 * DocumentoCxp (bandeja de carga) por clave de acceso del SRI (docs/logica-negocio/cxp/
	 * API-DOCUMENTOS-SEGUROS-CXP.md §5.1), usando el {@code @NamedQuery("DocumentoCxpByClave")}
	 * que ya declara la entidad. Esa query NO filtra por empresa: {@code DCXPCLAC} es
	 * {@code unique = true} a nivel de columna, así que la clave sola ya identifica una fila.
	 * @param claveAcceso : Clave de acceso de 49 dígitos del SRI
	 * @return            : El documento, o null si no está en la bandeja
	 * @throws Throwable  : Excepcion
	 */
	DocumentoCxp selectByClave(String claveAcceso) throws Throwable;
}
