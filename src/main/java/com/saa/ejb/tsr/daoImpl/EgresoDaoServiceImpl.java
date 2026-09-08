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
            "debitoAutomatico",
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
    public List<Egreso> selectByEmpresaEstado(Long idEmpresa, Long estado, Long idTitular,
            String concepto, java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaEstado Egreso con empresa: " + idEmpresa
                + " | estado: " + estado + " | titular: " + idTitular + " | concepto: " + concepto
                + " | desde: " + desde + " | hasta: " + hasta);

        StringBuilder jpql = new StringBuilder(
                " select e from Egreso e " +
                " where  e.empresa.codigo = :idEmpresa ");
        if (estado != null) {
            jpql.append(" and e.estado = :estado ");
        }
        if (idTitular != null) {
            jpql.append(" and e.titular.codigo = :idTitular ");
        }
        boolean hayConcepto = concepto != null && !concepto.trim().isEmpty();
        if (hayConcepto) {
            jpql.append(" and upper(e.descripcion) like :concepto ");
        }
        if (desde != null) {
            jpql.append(" and e.fecha >= :desde ");
        }
        if (hasta != null) {
            jpql.append(" and e.fecha <= :hasta ");
        }
        jpql.append(" order by e.fecha desc, e.id desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        if (hayConcepto) {
            query.setParameter("concepto", "%" + concepto.trim().toUpperCase() + "%");
        }
        if (desde != null) {
            query.setParameter("desde", desde);
        }
        if (hasta != null) {
            query.setParameter("hasta", hasta);
        }
        return query.getResultList();
    }
}
