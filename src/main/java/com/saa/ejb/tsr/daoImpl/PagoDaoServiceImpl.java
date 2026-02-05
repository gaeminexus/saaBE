/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.PagoDaoService;
import com.saa.model.tsr.Pago;
import com.saa.rubros.EstadoPago;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion PagoDaoService.
 */
/**
 * @author Chichan
 *
 */
@Stateless
public class PagoDaoServiceImpl extends EntityDaoImpl<Pago> implements PagoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tipoId",
							"numeroId",
							"proveedor",
							"descripcion",
							"fechaPago",
							"nombreUsuario",
							"valor",
							"empresa",
							"fechaInactivo",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"rubroEstadoP",
							"rubroEstadoH",
							"cheque",
							"persona",
							"asiento",
							"numeroAsiento",
							"tipoPago",
							"usuario",
							"idTempPago"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.PagoDaoService#recuperaIdPago(java.lang.Long)
	 */
	public Pago recuperaIdPago(Long idTempPago) throws Throwable{
		System.out.println("Ingresa al metodo recuperaIdPago con idTempPago: " + idTempPago);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" select b " +
									 " from   Pago b " +
							   		 " where  b.idTempPago = :codigo");		
		query.setParameter("codigo", idTempPago);
		Pago pago = (Pago)query.getSingleResult();
		return pago;
	}
	
	@SuppressWarnings("unchecked")
	public List<Pago> recuperaIdCheque(Long idCheque) throws Throwable{
		System.out.println("Ingresa al metodo recuperaIdCheque con idCheque: " + idCheque);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" select b " +
									 " from   Pago b " +
							   		 " where  b.cheque.codigo = :codigo");		
		query.setParameter("codigo", idCheque);
		return (List<Pago>)query.getResultList();		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.PagoDaoService#updateEstadoIdChequeByIdCheque(java.lang.Long, int)
	 */
	public void updateEstadoIdChequeByIdCheque(Long idCheque, int estado)	
			throws Throwable {
		System.out.println("Ingresa al metodo updateEstadoIdChequeByIdCheque con idCheque: " + idCheque + " estado: " + estado);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Long codigoCheque = null;
		Query query = em.createQuery(" update   Pago b" +
									 " set      b.rubroEstadoH = :estado, "+
									 "          b.cheque.codigo = :codigoCheque "+
									 " where    b.cheque.codigo = :idCheque");		
		query.setParameter("idCheque", idCheque);
		query.setParameter("estado", Long.valueOf(estado));
		switch (estado) {
		case EstadoPago.INGRESADO:
			codigoCheque = null;
			break;
		case EstadoPago.IMPRESO:
			codigoCheque = idCheque;
			break;			
		default:
			break;
		}
		query.setParameter("codigoCheque", codigoCheque);
		query.executeUpdate();
	}

}
