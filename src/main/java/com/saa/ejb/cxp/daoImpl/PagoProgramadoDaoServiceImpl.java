package com.saa.ejb.cxp.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.EstadoPagoProgramado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class PagoProgramadoDaoServiceImpl extends EntityDaoImpl<PagoProgramado>
        implements PagoProgramadoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "empresa",
            "facturaCompra",
            "egreso",
            "anticipo",
            "origenExterno",
            "idOrigen",
            "asiento",
            "beneficiarioNombre",
            "beneficiarioIdentificacion",
            "beneficiarioBanco",
            "beneficiarioTipoCuenta",
            "beneficiarioCuenta",
            "titular",
            "cuentaBancaria",
            "cuentaDestino",
            "debitoAutomatico",
            "formaPago",
            "cheque",
            "valor",
            "fechaProgramada",
            "lote",
            "estado",
            "referenciaBanco",
            "fechaRespuesta",
            "motivo",
            "aplicacion",
            "observacion",
            "usuario",
            "fechaRegistro"
        };
    }

    @Override
    public List<PagoProgramado> selectByEmpresaEstado(Long idEmpresa, Long estado, Long idTitular)
            throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaEstado con empresa: " + idEmpresa
                + " | estado: " + estado + " | titular: " + idTitular);

        StringBuilder jpql = new StringBuilder(
                " select p from PagoProgramado p " +
                " where  p.empresa.codigo = :idEmpresa ");
        if (estado != null) {
            jpql.append(" and p.estado = :estado ");
        }
        if (idTitular != null) {
            jpql.append(" and p.titular.codigo = :idTitular ");
        }
        jpql.append(" order by p.fechaProgramada, p.id");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectByLote(Long idLote) throws Throwable {
        System.out.println("Ingresa al metodo selectByLote con lote: " + idLote);
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.lote.id = :idLote " +
                " order by p.id");
        query.setParameter("idLote", idLote);
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectVigentesByFactura(Long idFacturaCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByFactura con factura: " + idFacturaCompra);
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.facturaCompra.id = :idFactura " +
                " and    p.estado in (:registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("idFactura", idFacturaCompra);
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectVigentesByEgreso(Long idEgreso) throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByEgreso con egreso: " + idEgreso);
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.egreso.id = :idEgreso " +
                " and    p.estado in (:registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("idEgreso", idEgreso);
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectVigentesByAnticipo(Long idAnticipo) throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByAnticipo con anticipo: " + idAnticipo);
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.anticipo.id = :idAnticipo " +
                " and    p.estado in (:registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("idAnticipo", idAnticipo);
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectVigentesByOrigen(String origen, Long idOrigen)
            throws Throwable {
        System.out.println("Ingresa al metodo selectVigentesByOrigen con origen: " + origen
                + " | idOrigen: " + idOrigen);
        if (origen == null || idOrigen == null) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.origenExterno = :origen " +
                " and    p.idOrigen = :idOrigen " +
                " and    p.estado in (:registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectByIds(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo selectByIds con " + (ids != null ? ids.size() : 0) + " ids");
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.id in (:ids) " +
                " order by p.id");
        query.setParameter("ids", ids);
        return query.getResultList();
    }
}
