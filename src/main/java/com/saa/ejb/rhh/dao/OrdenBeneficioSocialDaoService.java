package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService OrdenBeneficioSocial.
 */
@Local
public interface OrdenBeneficioSocialDaoService extends EntityDao<OrdenBeneficioSocial> {

	/**
	 * Orden "viva" (estado GENERADA, ENVIADA_A_TESORERIA o PAGADA) para una combinacion de
	 * empresa, tipo, anio y region. Sirve para el 409 de {@code POST /odbs/generar}: una
	 * orden ANULADA no bloquea generar otra, coherente con el indice funcional
	 * {@code UQ_ODBS_VIVA} del DDL.
	 *
	 * @param idEmpresa			: Id de la empresa
	 * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
	 * @param anio				: Anio del beneficio
	 * @param region			: Region del decimo cuarto; null para los demas tipos
	 * @return					: La orden viva, o null si no hay ninguna
	 * @throws Throwable		: Excepcion
	 */
	OrdenBeneficioSocial selectOrdenVivaByCombinacion(Long idEmpresa, Long tipoBeneficio,
			Integer anio, Long region) throws Throwable;

	/**
	 * Proyeccion para la bandeja de ordenes ({@code GET /odbs/listar}), con filtros
	 * opcionales (null = sin filtrar por ese criterio). Incluye el pago programado enlazado
	 * por LEFT JOIN, para no perder las ordenes que todavia no se enviaron a tesoreria.
	 *
	 * @param idEmpresa			: Id de la empresa, obligatorio
	 * @param anio				: Anio del beneficio; null = todos
	 * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL; null = todos
	 * @param estado			: Codigo alterno del detalle del rubro RHH_ESTADO_ORDEN_BENEFICIO; null = todos
	 * @return					: Filas de la bandeja, mas recientes primero
	 * @throws Throwable		: Excepcion
	 */
	List<OrdenBeneficioSocialResumen> selectListado(Long idEmpresa, Integer anio,
			Long tipoBeneficio, Long estado) throws Throwable;

	/**
	 * Cuenta las ordenes ya emitidas de un anio, para numerar la siguiente
	 * ({@code ODBS-{anio}-{secuencial de 4 digitos}}).
	 *
	 * @param anio				: Anio de la orden
	 * @return					: Cantidad de ordenes emitidas ese anio
	 * @throws Throwable		: Excepcion
	 */
	long countByAnio(Integer anio) throws Throwable;
}
