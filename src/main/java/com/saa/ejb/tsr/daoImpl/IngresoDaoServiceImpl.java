package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.IngresoDaoService;
import com.saa.model.tsr.Ingreso;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class IngresoDaoServiceImpl extends EntityDaoImpl<Ingreso> implements IngresoDaoService {

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
            "cuentaBancaria",
            "referencia",
            "estado",
            "asiento",
            "observacion",
            "usuario",
            "fechaRegistro"
        };
    }

    @Override
    public List<Ingreso> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaEstado Ingreso con empresa: " + idEmpresa
                + " | estado: " + estado);

        StringBuilder jpql = new StringBuilder(
                " select i from Ingreso i " +
                " where  i.empresa.codigo = :idEmpresa ");
        if (estado != null) {
            jpql.append(" and i.estado = :estado ");
        }
        jpql.append(" order by i.fecha desc, i.id desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        return query.getResultList();
    }
}
