package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ConceptoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ConceptoNomina.
 */
@Local
public interface ConceptoNominaDaoService extends EntityDao<ConceptoNomina> {

	/**
	 * Recupera un concepto por su codigo alterno dentro de una empresa. El codigo
	 * alterno es la clave estable del catalogo: el codigo interno (CPNMCDGO) cambia
	 * entre instalaciones, el alterno no.
	 *
	 * @param codigoAlterno	: Codigo alterno del concepto (CPNMALTR)
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El concepto, o null si no existe
	 * @throws Throwable	: Excepcion
	 */
	ConceptoNomina selectByCodigoAlterno(Long codigoAlterno, Long idEmpresa) throws Throwable;

	/**
	 * Recupera los conceptos activos de una empresa, ordenados por CPNMORDN, que es
	 * el orden de presentacion en el rol y la prelacion ante neto negativo.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Listado de conceptos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ConceptoNomina> selectActivosByEmpresa(Long idEmpresa) throws Throwable;


	/**
	 * Recupera el concepto que cumple un rol del motor dentro de una empresa. Es la via
	 * por la que el motor localiza el aporte personal, los decimos, las horas extra o
	 * los prestamos, sin referenciar ningun codigo alterno.
	 *
	 * @param rolMotor		: Codigo alterno del detalle del rubro RHH_ROL_CONCEPTO_MOTOR
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El concepto, o null si el rol no esta asignado
	 * @throws Throwable	: Excepcion
	 */
	ConceptoNomina selectByRolMotor(Integer rolMotor, Long idEmpresa) throws Throwable;
}
