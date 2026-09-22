package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CuentaBancariaBeneficiarioDaoService;
import com.saa.model.crd.CuentaBancariaBeneficiario;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CuentaBancariaBeneficiarioDaoServiceImpl extends EntityDaoImpl<CuentaBancariaBeneficiario>
        implements CuentaBancariaBeneficiarioDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CuentaBancariaBeneficiario");
        return new String[]{
            "codigo",
            "entidad",
            "nombre",
            "numeroIdentificacion",
            "bancoExterno",
            "tipoCuenta",
            "numeroCuenta",
            "porcentaje",
            "estado",
            "usuarioRegistro",
            "fechaRegistro"
        };
    }

    @Override
    public List<CuentaBancariaBeneficiario> selectPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("Ingresa al metodo selectPorEntidad de CuentaBancariaBeneficiario"
                + " - idEntidad: " + idEntidad);
        Query query = em.createQuery(
                " select c from CuentaBancariaBeneficiario c " +
                " where  c.entidad.codigo = :idEntidad " +
                " order by c.porcentaje desc, c.codigo asc");
        query.setParameter("idEntidad", idEntidad);
        return query.getResultList();
    }

    @Override
    public List<CuentaBancariaBeneficiario> selectPorEntidadEIdentificacion(Long idEntidad, String identificacion) throws Throwable {
        System.out.println("Ingresa al metodo selectPorEntidadEIdentificacion de CuentaBancariaBeneficiario"
                + " - idEntidad: " + idEntidad + " - identificacion: " + identificacion);
        Query query = em.createQuery(
                " select c from CuentaBancariaBeneficiario c " +
                " where  c.entidad.codigo = :idEntidad " +
                " and    c.numeroIdentificacion = :identificacion");
        query.setParameter("idEntidad", idEntidad);
        query.setParameter("identificacion", identificacion);
        return query.getResultList();
    }
}
