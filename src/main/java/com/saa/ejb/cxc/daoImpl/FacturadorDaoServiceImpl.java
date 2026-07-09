package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.model.cxc.Facturador;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para la entidad Facturador
 */
@Stateless
public class FacturadorDaoServiceImpl extends EntityDaoImpl<Facturador> implements FacturadorDaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id", "numDoc", "nombre", "razonSocial", "nombreComercial", "mail", "telefono",
			"direccion", "creado", "logo", "firma", "claveFirma", "empresaFirma", "codClave",
			"contabilidad", "agenteRetencion", "contribuyenteEspecial", "artesano", "microEmpresa",
			"rimpe", "popularRimpe", "turistico", "inicia", "vence", "docEmitidos", "docPermitidos",
			"impCodProd", "inventario", "empTransporte", "sinLimiteConsFinal", "estado"
		};
	}

	@Override
	public Facturador selectByNumDoc(String numDoc) throws Throwable {
		System.out.println("FacturadorDaoServiceImpl.selectByNumDoc - numDoc: " + numDoc);
		try {
			Query query = em.createQuery(
				"SELECT f FROM Facturador f WHERE f.numDoc = :numDoc"
			);
			query.setParameter("numDoc", numDoc);
			return (Facturador) query.getSingleResult();
		} catch (Exception e) {
			System.err.println("Error al buscar facturador por numDoc: " + e.getMessage());
			return null;
		}
	}
}
