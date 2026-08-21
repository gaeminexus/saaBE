package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService;
import com.saa.model.rhh.DetalleFormatoBancario;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DetalleFormatoBancarioDaoService.
 */
@Stateless
public class DetalleFormatoBancarioDaoServiceImpl extends EntityDaoImpl<DetalleFormatoBancario>
		implements DetalleFormatoBancarioDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleFormatoBancario");
		return new String[]{"codigo",
							"formato",
							"campo",
							"orden",
							"indiceInicio",
							"longitud",
							"ladoRelleno",
							"caracterRelleno",
							"decimales",
							"incluyeSeparadorDecimal",
							"formatoFecha",
							"valorFijo",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService#selectByFormato(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleFormatoBancario> selectByFormato(Long idFormato) throws Throwable {
		System.out.println("Ingresa al metodo selectByFormato de DetalleFormatoBancario, formato: "
				+ idFormato);
		Query query = em.createQuery(" select   t "
				+ " from     DetalleFormatoBancario t "
				+ " where    t.formato.codigo = :idFormato "
				+ "          and (t.estado is null or t.estado = :activo) "
				+ " order by t.orden, t.codigo ");
		query.setParameter("idFormato", idFormato);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}
}
