package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetallePagoOrigenExternoDaoService;
import com.saa.model.cxp.DetallePagoOrigenExterno;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DetallePagoOrigenExternoDaoServiceImpl extends EntityDaoImpl<DetallePagoOrigenExterno>
        implements DetallePagoOrigenExternoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "pago",
            "producto",
            "valor",
            "concepto"
        };
    }

    @Override
    public List<DetallePagoOrigenExterno> selectByPago(Long idPago) throws Throwable {
        System.out.println("Ingresa al metodo selectByPago con pago: " + idPago);
        Query query = em.createQuery(
                " select d from DetallePagoOrigenExterno d " +
                " where  d.pago.id = :idPago " +
                " order by d.codigo");
        query.setParameter("idPago", idPago);
        return query.getResultList();
    }
}
