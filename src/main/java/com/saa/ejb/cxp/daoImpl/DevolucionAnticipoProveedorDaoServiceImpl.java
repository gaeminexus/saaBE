package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DevolucionAnticipoProveedorDaoService;
import com.saa.model.cxp.DevolucionAnticipoProveedor;
import com.saa.rubros.EstadoDevolucionAnticipoProveedor;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DevolucionAnticipoProveedorDaoServiceImpl extends EntityDaoImpl<DevolucionAnticipoProveedor>
        implements DevolucionAnticipoProveedorDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo",
            "empresa",
            "titular",
            "cuentaBancaria",
            "fecha",
            "valor",
            "referencia",
            "observacion",
            "asiento",
            "estado",
            "motivoAnulacion",
            "fechaAnulacion",
            "usuario",
            "fechaRegistro"
        };
    }

    @Override
    public List<Object[]> selectListadoByEmpresaTitular(Long idEmpresa, Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectListadoByEmpresaTitular con empresa: " + idEmpresa
                + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select d.codigo, d.fecha, d.valor, d.referencia, d.estado, " +
                "        d.cuentaBancaria.banco.nombre, d.cuentaBancaria.numeroCuenta, " +
                "        d.asiento.numeroAlterno, d.motivoAnulacion " +
                " from   DevolucionAnticipoProveedor d " +
                " where  d.empresa.codigo = :idEmpresa ");
        if (idTitular != null) {
            jpql.append(" and d.titular.codigo = :idTitular ");
        }
        jpql.append(" order by d.fecha desc, d.codigo desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectDetalleByEmpresaTitular(Long idEmpresa, Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectDetalleByEmpresaTitular con empresa: " + idEmpresa
                + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select dt.devolucion.codigo, dt.anticipo.id, dt.anticipo.numeroDoc, dt.valor " +
                " from   DetalleDevolucionAnticipo dt " +
                " where  dt.devolucion.empresa.codigo = :idEmpresa ");
        if (idTitular != null) {
            jpql.append(" and dt.devolucion.titular.codigo = :idTitular ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<DevolucionAnticipoProveedor> selectActivasByAnticipo(Long idAnticipo) throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByAnticipo con anticipo: " + idAnticipo);
        Query query = em.createQuery(
                " select distinct dt.devolucion from DetalleDevolucionAnticipo dt " +
                " where  dt.anticipo.id = :idAnticipo " +
                " and    dt.devolucion.estado = :activa");
        query.setParameter("idAnticipo", idAnticipo);
        query.setParameter("activa", Long.valueOf(EstadoDevolucionAnticipoProveedor.ACTIVA));
        return query.getResultList();
    }
}
