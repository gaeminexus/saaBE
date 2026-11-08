package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.UsuarioNivelAprobacionDaoService;
import com.saa.model.crd.UsuarioNivelAprobacion;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class UsuarioNivelAprobacionDaoServiceImpl extends EntityDaoImpl<UsuarioNivelAprobacion> implements UsuarioNivelAprobacionDaoService {

    @PersistenceContext
    EntityManager em;

    @SuppressWarnings("unchecked")
    @Override
    public List<UsuarioNivelAprobacion> selectActivosByNivel(Long idNivel) throws Throwable {
        System.out.println("UsuarioNivelAprobacionDaoServiceImpl - selectActivosByNivel - idNivel: " + idNivel);
        Query query = em.createQuery(
            " select e from UsuarioNivelAprobacion e where e.idNivel = :idNivel and e.estado = :activo  order by e.codigo");
        query.setParameter("idNivel", idNivel);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }

}
