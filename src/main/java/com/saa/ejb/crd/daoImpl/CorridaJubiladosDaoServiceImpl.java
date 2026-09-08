package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CorridaJubiladosDaoService;
import com.saa.model.crd.CorridaJubilados;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CorridaJubiladosDaoServiceImpl extends EntityDaoImpl<CorridaJubilados>
        implements CorridaJubiladosDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CorridaJubilados");
        return new String[]{
            "codigo",
            "empresa",
            "anio",
            "mes",
            "estadoSeguro",
            "fechaSeguro",
            "usuarioSeguro",
            "totalSeguro",
            "idOrdenPagoSeguro",
            "cantidadJubiladosSeguro",
            "estadoPensiones",
            "fechaPensiones",
            "usuarioPensiones",
            "totalPensiones",
            "totalCruzadoPrestamos",
            "cantidadJubiladosPensiones"
        };
    }

    @Override
    public CorridaJubilados selectByPeriodo(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("Ingresa al metodo selectByPeriodo de CorridaJubilados"
                + " - empresa: " + idEmpresa + " periodo: " + anio + "-" + mes);
        Query query = em.createQuery(
                " select c from CorridaJubilados c " +
                " where  c.empresa.codigo = :idEmpresa " +
                " and    c.anio           = :anio " +
                " and    c.mes            = :mes " +
                " order by c.codigo desc");
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("anio", anio);
        query.setParameter("mes", mes);
        List<CorridaJubilados> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
