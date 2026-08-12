package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.EgresoDaoService;
import com.saa.model.tsr.Egreso;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class EgresoDaoServiceImpl extends EntityDaoImpl<Egreso> implements EgresoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "empresa",
            "titular",
            "producto",
            "descripcion",
            "valor",
            "fecha",
            "estado",
            "asiento",
            "observacion",
            "usuario",
            "fechaRegistro"
        };
    }

    @Override
    public List<Egreso> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaEstado Egreso con empresa: " + idEmpresa
                + " | estado: " + estado);

        StringBuilder jpql = new StringBuilder(
                " select e from Egreso e " +
                " where  e.empresa.codigo = :idEmpresa ");
        if (estado != null) {
            jpql.append(" and e.estado = :estado ");
        }
        jpql.append(" order by e.fecha desc, e.id desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        return query.getResultList();
    }
}
