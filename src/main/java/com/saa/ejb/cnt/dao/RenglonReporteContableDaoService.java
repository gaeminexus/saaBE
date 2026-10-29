package com.saa.ejb.cnt.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.RenglonReporteContable;

import jakarta.ejb.Local;

/** DAO de CNT.RNRP (renglones de un reporte contable de renglones propios, C19b). */
@Local
public interface RenglonReporteContableDaoService extends EntityDao<RenglonReporteContable> {

	/** Renglones activos y vigentes a la fecha de corte del reporte, en su orden. */
	List<RenglonReporteContable> selectVigentes(Long codigoReporte, LocalDate corte) throws Throwable;
}
