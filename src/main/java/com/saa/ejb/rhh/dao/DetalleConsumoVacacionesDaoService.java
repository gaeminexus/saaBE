package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DetalleConsumoVacaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DetalleConsumoVacaciones (RHH.DVAC). Ver
 * docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md.
 */
@Local
public interface DetalleConsumoVacacionesDaoService extends EntityDao<DetalleConsumoVacaciones> {

	/**
	 * Recupera las filas VIGENTES (DVACESTD=1) de una solicitud: los años y días que
	 * consumió su aprobación, todavía no revertidos. Es lo que anularAprobacion recorre
	 * para devolver los días exactamente a esos años.
	 *
	 * @param idSolicitud	: Id de la solicitud (RHH.SLCT)
	 * @return				: Filas vigentes, por año ascendente; vacío si la solicitud
	 *						  se aprobó antes de que existiera esta tabla
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleConsumoVacaciones> selectVigentesPorSolicitud(Long idSolicitud) throws Throwable;

}
