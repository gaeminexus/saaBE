package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.SalidaOficial;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService SalidaOficial.
 */
@Local
public interface SalidaOficialDaoService extends EntityDao<SalidaOficial> {

	/**
	 * Localiza la salida de un periodo, para actualizarla en vez de duplicarla.
	 *
	 * <p><b>Es aqui donde vive la idempotencia</b>, no en un unique: <code>SLOFMESS</code> y
	 * <code>MPLDCDGO</code> son nulos en las salidas anuales y consolidadas, y Oracle no
	 * considera duplicadas dos filas donde alguna columna de la clave es nula, de modo que un
	 * UNIQUE no impediria nada justo en los casos que importan. Por eso la consulta compara los
	 * nulos explicitamente con <code>is null</code> y no con igualdad.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param tipoSalida	: Detalle del rubro RHH_TIPO_SALIDA_OFICIAL
	 * @param anio			: Ejercicio fiscal
	 * @param mes			: Mes, o null en las anuales
	 * @param idEmpleado	: Id del empleado, o null en las consolidadas
	 * @return				: La salida, o null
	 * @throws Throwable	: Excepcion
	 */
	SalidaOficial selectSalida(Long idEmpresa, Long tipoSalida, Integer anio, Integer mes,
			Long idEmpleado) throws Throwable;

}
