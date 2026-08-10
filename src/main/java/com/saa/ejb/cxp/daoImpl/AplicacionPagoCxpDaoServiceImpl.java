package com.saa.ejb.cxp.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AplicacionPagoCxpDaoServiceImpl extends EntityDaoImpl<AplicacionPagoCxp>
        implements AplicacionPagoCxpDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "empresa",
            "facturaCompra",
            "tipoDocPago",
            "notaCredito",
            "notaDebito",
            "retencion",
            "retencionV2",
            "anticipo",
            "formaPago",
            "referencia",
            "banco",
            "montoAplicado",
            "fechaAplicacion",
            "observacion",
            "estado",
            "usuario",
            "asiento",
            "fechaRegistro"
        };
    }

    @Override
    public List<AplicacionPagoCxp> selectActivasByFactura(Long idFacturaCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByFactura con factura: " + idFacturaCompra);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxp a " +
                " where  a.facturaCompra.id = :idFactura " +
                " and    a.estado = 1 " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idFactura", idFacturaCompra);
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxp> selectByFactura(Long idFacturaCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectByFactura con factura: " + idFacturaCompra);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxp a " +
                " where  a.facturaCompra.id = :idFactura " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idFactura", idFacturaCompra);
        return query.getResultList();
    }

    @Override
    public Double sumaAplicadoByFactura(Long idFacturaCompra) throws Throwable {
        System.out.println("Ingresa al metodo sumaAplicadoByFactura con factura: " + idFacturaCompra);
        Query query = em.createQuery(
                " select coalesce(sum(a.montoAplicado), 0) from AplicacionPagoCxp a " +
                " where  a.facturaCompra.id = :idFactura " +
                " and    a.estado = 1");
        query.setParameter("idFactura", idFacturaCompra);
        Object resultado = query.getSingleResult();
        return (resultado != null) ? ((Number) resultado).doubleValue() : 0.0;
    }

    @Override
    public List<AplicacionPagoCxp> selectActivasByDocumento(String tipoDocumento, Long idDocumento)
            throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByDocumento con tipo: " + tipoDocumento
                + " y documento: " + idDocumento);

        String campo;
        if ("RETENCION".equals(tipoDocumento))          { campo = "a.retencion.id"; }
        else if ("RETENCION_V2".equals(tipoDocumento))  { campo = "a.retencionV2.id"; }
        else if ("NOTA_CREDITO".equals(tipoDocumento))  { campo = "a.notaCredito.id"; }
        else if ("NOTA_DEBITO".equals(tipoDocumento))   { campo = "a.notaDebito.id"; }
        else {
            throw new IncomeException("Tipo de documento no soportado para buscar aplicaciones: "
                    + tipoDocumento);
        }

        Query query = em.createQuery(
                " select a from AplicacionPagoCxp a " +
                " where  " + campo + " = :idDocumento " +
                " and    a.estado = 1");
        query.setParameter("idDocumento", idDocumento);
        return query.getResultList();
    }

    @Override
    public List<FacturaCompra> selectFacturaByNumero(String numeroDocumento, Long idTitular,
            Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectFacturaByNumero con numero: " + numeroDocumento
                + " | titular: " + idTitular + " | empresa: " + idEmpresa);

        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // El número puede venir con o sin guiones ('001-001-000000123' /
        // '001001000000123'): se comparan ambos sin guiones.
        String numeroNormalizado = numeroDocumento.trim().replace("-", "");

        StringBuilder jpql = new StringBuilder(
                " select f from FacturaCompra f " +
                " where  replace(f.numero, '-', '') = :numero ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }
        if (idEmpresa != null) {
            jpql.append(" and f.empresa.codigo = :idEmpresa ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("numero", numeroNormalizado);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        return query.getResultList();
    }
}
