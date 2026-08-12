package com.saa.ejb.cxc.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.Factura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class AplicacionPagoCxcDaoServiceImpl extends EntityDaoImpl<AplicacionPagoCxc>
        implements AplicacionPagoCxcDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id",
            "empresa",
            "factura",
            "liquidacion",
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
    public List<AplicacionPagoCxc> selectActivasByFactura(Long idFactura) throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByFactura con factura: " + idFactura);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxc a " +
                " where  a.factura.id = :idFactura " +
                " and    a.estado = 1 " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idFactura", idFactura);
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxc> selectByFactura(Long idFactura) throws Throwable {
        System.out.println("Ingresa al metodo selectByFactura con factura: " + idFactura);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxc a " +
                " where  a.factura.id = :idFactura " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idFactura", idFactura);
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxc> selectActivasByLiquidacion(Long idLiquidacion) throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByLiquidacion con liquidacion: "
                + idLiquidacion);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxc a " +
                " where  a.liquidacion.id = :idLiquidacion " +
                " and    a.estado = 1 " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idLiquidacion", idLiquidacion);
        return query.getResultList();
    }

    @Override
    public Double sumaAplicadoByFactura(Long idFactura) throws Throwable {
        System.out.println("Ingresa al metodo sumaAplicadoByFactura con factura: " + idFactura);
        Query query = em.createQuery(
                " select coalesce(sum(a.montoAplicado), 0) from AplicacionPagoCxc a " +
                " where  a.factura.id = :idFactura " +
                " and    a.estado = 1");
        query.setParameter("idFactura", idFactura);
        Object resultado = query.getSingleResult();
        return (resultado != null) ? ((Number) resultado).doubleValue() : 0.0;
    }

    @Override
    public Double sumaAplicadoByLiquidacion(Long idLiquidacion) throws Throwable {
        System.out.println("Ingresa al metodo sumaAplicadoByLiquidacion con liquidacion: "
                + idLiquidacion);
        Query query = em.createQuery(
                " select coalesce(sum(a.montoAplicado), 0) from AplicacionPagoCxc a " +
                " where  a.liquidacion.id = :idLiquidacion " +
                " and    a.estado = 1");
        query.setParameter("idLiquidacion", idLiquidacion);
        Object resultado = query.getSingleResult();
        return (resultado != null) ? ((Number) resultado).doubleValue() : 0.0;
    }

    @Override
    public List<AplicacionPagoCxc> selectActivasByDocumento(String tipoDocumento, Long idDocumento)
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
                " select a from AplicacionPagoCxc a " +
                " where  " + campo + " = :idDocumento " +
                " and    a.estado = 1");
        query.setParameter("idDocumento", idDocumento);
        return query.getResultList();
    }

    @Override
    public List<Factura> selectFacturaByNumero(String numeroDocumento, Long idTitular,
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
                " select f from Factura f " +
                " where  FUNCTION('replace', f.numero, '-', '') = :numero ");
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
