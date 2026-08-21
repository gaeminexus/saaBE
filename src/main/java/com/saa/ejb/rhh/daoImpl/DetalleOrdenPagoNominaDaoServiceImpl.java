package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService;
import com.saa.model.rhh.DetalleOrdenPagoNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleOrdenPagoNominaDaoService.
 */
@Stateless
public class DetalleOrdenPagoNominaDaoServiceImpl extends EntityDaoImpl<DetalleOrdenPagoNomina>
		implements DetalleOrdenPagoNominaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleOrdenPagoNomina");
		return new String[]{"codigo",
							"ordenPagoNomina",
							"empleado",
							"nomina",
							"cuentaBancariaEmpleado",
							"valor",
							"numeroCuenta",
							"tipoCuenta",
							"banco",
							"identificacion",
							"nombreBeneficiario",
							"rechazado",
							"motivoRechazo",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService#selectByOrdenPago(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleOrdenPagoNomina> selectByOrdenPago(Long idOrdenPago) throws Throwable {
		System.out.println("Ingresa al metodo selectByOrdenPago de DetalleOrdenPagoNomina, orden: "
				+ idOrdenPago);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleOrdenPagoNomina t "
				+ " where    t.ordenPagoNomina.codigo = :idOrdenPago "
				+ " order by t.empleado.apellidos, t.empleado.nombres, t.codigo ");
		query.setParameter("idOrdenPago", idOrdenPago);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService#eliminaByOrdenPago(java.lang.Long)
	 */
	@Override
	public int eliminaByOrdenPago(Long idOrdenPago) throws Throwable {
		System.out.println("Ingresa al metodo eliminaByOrdenPago de DetalleOrdenPagoNomina, orden: "
				+ idOrdenPago);
		Query query = em.createQuery(" delete from DetalleOrdenPagoNomina t "
				+ " where  t.ordenPagoNomina.codigo = :idOrdenPago ");
		query.setParameter("idOrdenPago", idOrdenPago);
		return query.executeUpdate();
	}
}
