package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Egreso;

import jakarta.ejb.Local;

@Local
public interface EgresoDaoService extends EntityDao<Egreso> {

	/**
	 * Recupera los egresos de una empresa, filtrando opcionalmente por estado, beneficiario,
	 * concepto y fecha. Los cuatro son columnas reales de TSR.EGRS (EGRSESTD, EGRSTTLR,
	 * EGRSDSCR, EGRSFCHA) — no confundir con el tipo de pago, que es {@code @Transient} y no
	 * puede filtrarse acá (ver {@code EgresoServiceImpl.completaFormaPago}).
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : Estado del egreso, null para todos
	 * @param idTitular  : Id del beneficiario (EGRSTTLR); null para todos
	 * @param concepto   : Coincidencia parcial (sin distinguir mayúsculas) contra la
	 *                     descripción/concepto (EGRSDSCR); null o en blanco = sin filtro
	 * @param desde      : Fecha desde (inclusive); null = sin límite inferior
	 * @param hasta      : Fecha hasta (inclusive); null = sin límite superior
	 * @return           : Listado de egresos
	 * @throws Throwable : Excepcion
	 */
	List<Egreso> selectByEmpresaEstado(Long idEmpresa, Long estado, Long idTitular, String concepto,
			java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable;
}
