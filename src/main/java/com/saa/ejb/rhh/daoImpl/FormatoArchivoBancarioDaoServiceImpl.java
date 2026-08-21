package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService;
import com.saa.model.rhh.FormatoArchivoBancario;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion FormatoArchivoBancarioDaoService.
 */
@Stateless
public class FormatoArchivoBancarioDaoServiceImpl extends EntityDaoImpl<FormatoArchivoBancario>
		implements FormatoArchivoBancarioDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) FormatoArchivoBancario");
		return new String[]{"codigo",
							"empresa",
							"nombre",
							"banco",
							"tipoFormato",
							"delimitador",
							"extension",
							"codificacion",
							"formatoFecha",
							"plantillaCabecera",
							"plantillaPie",
							"mapaTipoCuenta",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService#selectActivoByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public FormatoArchivoBancario selectActivoByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectActivoByEmpresa de FormatoArchivoBancario, empresa: "
				+ idEmpresa);
		Query query = em.createQuery(" select   t "
				+ " from     FormatoArchivoBancario t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.estado = :activo "
				+ " order by t.codigo desc ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		List<FormatoArchivoBancario> lista = query.getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}
}
