package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.UsuarioAppDaoService;
import com.saa.model.crd.UsuarioApp;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class UsuarioAppDaoServiceImpl extends EntityDaoImpl<UsuarioApp>
        implements UsuarioAppDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) UsuarioApp");
        return new String[]{
            "codigo",
            "entidad",
            "identificacion",
            "claveHash",
            "estado",
            "intentosFallidos",
            "bloqueadoHasta",
            "debeCambiarClave",
            "fechaCreacion",
            "fechaUltimoAcceso",
            "usuarioRegistro"
        };
    }

    @Override
    public UsuarioApp selectByIdentificacion(String identificacion) throws Throwable {
        System.out.println("Ingresa al metodo selectByIdentificacion de UsuarioApp - identificacion: "
                + identificacion);
        Query query = em.createQuery(
                " select u from UsuarioApp u " +
                " where  u.identificacion = :identificacion");
        query.setParameter("identificacion", identificacion);
        List<UsuarioApp> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public UsuarioApp selectByEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectByEntidad de UsuarioApp - idEntidad: " + idEntidad);
        Query query = em.createQuery(
                " select u from UsuarioApp u " +
                " where  u.entidad.codigo = :idEntidad");
        query.setParameter("idEntidad", idEntidad);
        List<UsuarioApp> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
