package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DevolucionAporteDaoService;
import com.saa.model.crd.DevolucionAporte;
import com.saa.rubros.EstadoDevolucionAporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DevolucionAporteDaoServiceImpl extends EntityDaoImpl<DevolucionAporte>
        implements DevolucionAporteDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "entidad",
            "filial",
            "cuentaParticipe",
            "valor",
            "fecha",
            "motivo",
            "estado",
            "idPagoProgramado",
            "numeroAsiento",
            "fechaPago",
            "idEmpresa",
            "usuarioRegistro",
            "fechaRegistro",
            "usuarioAnulacion",
            "fechaAnulacion",
            "motivoAnulacion"
        };
    }

    @Override
    public List<DevolucionAporte> selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad con entidad: " + idEntidad);
        Query query = em.createQuery(
                " select d from DevolucionAporte d " +
                " where  d.entidad.codigo = :idEntidad " +
                " order by d.codigo desc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public List<DevolucionAporte> selectPendientesConciliacion() throws Throwable {
        System.out.println("Ingresa al metodo selectPendientesConciliacion");
        Query query = em.createQuery(
                " select d from DevolucionAporte d " +
                " where  d.estado in (:registrada, :enPago) " +
                " and    d.idPagoProgramado is not null " +
                " order by d.codigo");
        query.setParameter("registrada", Long.valueOf(EstadoDevolucionAporte.REGISTRADA));
        query.setParameter("enPago",     Long.valueOf(EstadoDevolucionAporte.EN_PAGO));
        return query.getResultList();
    }

    @Override
    public List<DevolucionAporte> selectPendientesConciliacionByEntidad(Long idEntidad)
            throws Throwable {
        System.out.println("Ingresa al metodo selectPendientesConciliacionByEntidad con entidad: "
                + idEntidad);
        Query query = em.createQuery(
                " select d from DevolucionAporte d " +
                " where  d.entidad.codigo = :idEntidad " +
                " and    d.estado in (:registrada, :enPago) " +
                " and    d.idPagoProgramado is not null " +
                " order by d.codigo");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("registrada", Long.valueOf(EstadoDevolucionAporte.REGISTRADA));
        query.setParameter("enPago",     Long.valueOf(EstadoDevolucionAporte.EN_PAGO));
        return query.getResultList();
    }
}
