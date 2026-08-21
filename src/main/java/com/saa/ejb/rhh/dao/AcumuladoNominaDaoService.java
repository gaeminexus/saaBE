package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.Empleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService AcumuladoNomina.
 */
@Local
public interface AcumuladoNominaDaoService extends EntityDao<AcumuladoNomina> {


	/**
	 * Suma el valor acumulado de un tipo entre dos claves anio-mes, de modo que cubra
	 * periodos que cruzan el cambio de anio, como el del decimo tercero.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param tipoAcumulado	: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @param anioDesde		: Anio inicial
	 * @param mesDesde		: Mes inicial
	 * @param anioHasta		: Anio final
	 * @param mesHasta		: Mes final
	 * @return				: La suma, o cero si no hay filas
	 * @throws Throwable	: Excepcion
	 */
	Double sumaValorRango(Long idEmpleado, Long tipoAcumulado, Integer anioDesde, Integer mesDesde,
			Integer anioHasta, Integer mesHasta) throws Throwable;

	/**
	 * Suma los DIAS acumulados de un tipo entre dos claves anio-mes. Es el gemelo de
	 * {@link #sumaValorRango} para la otra columna, y existe porque
	 * <b>ACMNDIAS y ACMNVLOR no son intercambiables</b>: el acumulado de dias trabajados
	 * --tipo 10, el centinela del cierre-- guarda los dias en ACMNDIAS y deja ACMNVLOR
	 * en cero, asi que sumarle el valor devuelve siempre 0,00.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param tipoAcumulado	: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @param anioDesde		: Anio inicial
	 * @param mesDesde		: Mes inicial
	 * @param anioHasta		: Anio final
	 * @param mesHasta		: Mes final
	 * @return				: La suma de dias, o cero si no hay filas
	 * @throws Throwable	: Excepcion
	 */
	Double sumaDiasRango(Long idEmpleado, Long tipoAcumulado, Integer anioDesde, Integer mesDesde,
			Integer anioHasta, Integer mesHasta) throws Throwable;

	/**
	 * Recupera el acumulado de un empleado para un anio, mes y tipo concretos.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio
	 * @param mes			: Mes
	 * @param tipoAcumulado	: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @return				: El acumulado, o null si no existe
	 * @throws Throwable	: Excepcion
	 */
	AcumuladoNomina selectByClave(Long idEmpleado, Integer anio, Integer mes,
			Long tipoAcumulado) throws Throwable;

	/**
	 * Empleados de la empresa con algun acumulado de IR en el anio, cesantes incluidos.
	 *
	 * <p>Es la fuente del RDEP, y por eso <b>no se parte de los contratos activos</b>: el
	 * declarativo del SRI declara a quien cobro en el ejercicio, no a quien sigue en la
	 * empresa. Quien entro en enero y se fue en marzo cobro tres meses y hay que declararlo.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Ejercicio fiscal
	 * @return				: Los empleados, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<Empleado> selectEmpleadosConAcumuladoEnAnio(Long idEmpresa, Integer anio) throws Throwable;

	/**
	 * Elimina los acumulados generados por un periodo. Se usa al reabrir.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Numero de acumulados eliminados
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Suma el valor acumulado de un tipo para un empleado en un rango de meses del mismo
	 * anio. Para rangos que cruzan el cambio de anio se usa sumaValorRango.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio
	 * @param tipoAcumulado	: Codigo alterno del detalle del rubro RHH_TIPO_ACUMULADO
	 * @param mesDesde		: Mes inicial, inclusive
	 * @param mesHasta		: Mes final, inclusive
	 * @return				: La suma, o cero si no hay filas
	 * @throws Throwable	: Excepcion
	 */
	Double sumaValor(Long idEmpleado, Integer anio, Long tipoAcumulado, Integer mesDesde,
			Integer mesHasta) throws Throwable;
}
