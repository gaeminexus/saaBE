package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.LiquidacionExternaDaoService;
import com.saa.model.rhh.LiquidacionExterna;
import com.saa.rubros.RhhEstadoLiquidacionExterna;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion LiquidacionExternaDaoService.
 */
@Stateless
public class LiquidacionExternaDaoServiceImpl extends EntityDaoImpl<LiquidacionExterna>
		implements LiquidacionExternaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionExternaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) LiquidacionExterna");
		return new String[]{"codigo",
							"empresa",
							"tipoIdentificacion",
							"identificacion",
							"apellidos",
							"nombres",
							"cargo",
							"fechaIngreso",
							"fechaSalida",
							"causalTerminacion",
							"ultimaRemuneracion",
							"totalIngresos",
							"totalDescuentos",
							"neto",
							"productoPago",
							"banco",
							"tipoCuenta",
							"numeroCuenta",
							"estado",
							"idPago",
							"idAsiento",
							"fechaPago",
							"observacion",
							"motivoAnulacion",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionExternaDaoService#selectVigentesByIdentificacion(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LiquidacionExterna> selectVigentesByIdentificacion(String identificacion, Long excluirCodigo)
			throws Throwable {
		System.out.println("Ingresa al metodo selectVigentesByIdentificacion de LiquidacionExterna, identificacion: "
				+ identificacion + " | excluirCodigo: " + excluirCodigo);
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionExterna t "
				+ " where    t.identificacion = :identificacion "
				+ "          and t.estado <> :anulada "
				+ "          and (:excluirCodigo is null or t.codigo <> :excluirCodigo) "
				+ " order by t.codigo ");
		query.setParameter("identificacion", identificacion);
		query.setParameter("anulada", Long.valueOf(RhhEstadoLiquidacionExterna.ANULADA));
		query.setParameter("excluirCodigo", excluirCodigo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionExternaDaoService#selectEnTesoreria()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LiquidacionExterna> selectEnTesoreria() throws Throwable {
		System.out.println("Ingresa al metodo selectEnTesoreria de LiquidacionExterna");
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionExterna t "
				+ " where    t.estado = :enTesoreria "
				+ " order by t.codigo ");
		query.setParameter("enTesoreria", Long.valueOf(RhhEstadoLiquidacionExterna.EN_TESORERIA));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionExternaDaoService#selectPagadasByAnio(java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LiquidacionExterna> selectPagadasByAnio(Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectPagadasByAnio de LiquidacionExterna, anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionExterna t "
				+ " where    t.estado = :pagada "
				+ "          and t.fechaPago >= :desde and t.fechaPago < :hasta "
				+ " order by t.codigo ");
		query.setParameter("pagada", Long.valueOf(RhhEstadoLiquidacionExterna.PAGADA));
		query.setParameter("desde", LocalDate.of(anio.intValue(), 1, 1));
		query.setParameter("hasta", LocalDate.of(anio.intValue() + 1, 1, 1));
		return query.getResultList();
	}
}
