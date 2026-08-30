package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CobroCreditoDaoService;
import com.saa.model.crd.CobroCredito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CobroCreditoDaoServiceImpl extends EntityDaoImpl<CobroCredito>
        implements CobroCreditoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CobroCredito");
        return new String[]{
            "codigo",
            "entidad",
            "tipoOperacion",
            "estado",
            "cuentaBancaria",
            "referencia",
            "rutaRespaldo",
            "valor",
            "fecha",
            "observacion",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioAprobacion",
            "fechaAprobacion",
            "usuarioRechazo",
            "fechaRechazo",
            "motivoRechazo",
            "usuarioProceso",
            "fechaProceso",
            "asientoTransitorio",
            "asientoDefinitivo"
        };
    }

    @Override
    public List<CobroCredito> selectByEstado(Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectByEstado de CobroCredito"
                + " - estado: " + estado);
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  c.estado = :estado " +
                " order by c.fechaRegistro asc");
        query.setParameter("estado", estado);
        return query.getResultList();
    }

    @Override
    public List<CobroCredito> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de CobroCredito"
                + " - entidad: " + idEntidad);
        Query query = em.createQuery(
                " select c from CobroCredito c " +
                " where  c.entidad.codigo = :idEntidad " +
                " order by c.fechaRegistro desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }
}
