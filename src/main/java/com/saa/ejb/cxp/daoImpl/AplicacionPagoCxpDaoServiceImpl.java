package com.saa.ejb.cxp.daoImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.TipoDocPagoAplicacion;

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
            "liquidacionCompra",
            "tipoDocPago",
            "notaCredito",
            "notaDebito",
            "retencion",
            "retencionV2",
            "anticipo",
            "anticipoOrigen",
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
    public List<AplicacionPagoCxp> selectActivasByLiquidacion(Long idLiquidacionCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectActivasByLiquidacion con liquidacion: " + idLiquidacionCompra);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxp a " +
                " where  a.liquidacionCompra.id = :idLiquidacion " +
                " and    a.estado = 1 " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idLiquidacion", idLiquidacionCompra);
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxp> selectByLiquidacion(Long idLiquidacionCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectByLiquidacion con liquidacion: " + idLiquidacionCompra);
        Query query = em.createQuery(
                " select a from AplicacionPagoCxp a " +
                " where  a.liquidacionCompra.id = :idLiquidacion " +
                " order by a.fechaAplicacion, a.id");
        query.setParameter("idLiquidacion", idLiquidacionCompra);
        return query.getResultList();
    }

    @Override
    public Double sumaAplicadoByLiquidacion(Long idLiquidacionCompra) throws Throwable {
        System.out.println("Ingresa al metodo sumaAplicadoByLiquidacion con liquidacion: " + idLiquidacionCompra);
        Query query = em.createQuery(
                " select coalesce(sum(a.montoAplicado), 0) from AplicacionPagoCxp a " +
                " where  a.liquidacionCompra.id = :idLiquidacion " +
                " and    a.estado = 1");
        query.setParameter("idLiquidacion", idLiquidacionCompra);
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

    @Override
    public List<com.saa.model.cxc.LiquidacionCompra> selectLiquidacionEmitidaByNumero(String numeroDocumento,
            Long idTitular, Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo selectLiquidacionEmitidaByNumero con numero: " + numeroDocumento
                + " | titular: " + idTitular + " | empresa: " + idEmpresa);

        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String numeroNormalizado = numeroDocumento.trim().replace("-", "");

        // ÍTEM 17 (docs/logica-negocio/tsr/AUDITORIA-ESTADO-CUENTA-TITULAR.md P2): CBR.LQCS
        // (LiquidacionCompra) no tiene empresa propia -- va por el facturador, igual que el resto
        // del módulo sri/cxc que filtra por empresa contable.
        StringBuilder jpql = new StringBuilder(
                " select l from LiquidacionCompra l " +
                " where  FUNCTION('replace', l.numero, '-', '') = :numero ");
        if (idTitular != null) {
            jpql.append(" and l.titular.codigo = :idTitular ");
        }
        if (idEmpresa != null) {
            jpql.append(" and l.facturador.empresa.codigo = :idEmpresa ");
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

    @Override
    public List<AplicacionPagoCxp> selectCrucesAnticipoActivos(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectCrucesAnticipoActivos con titular: " + idTitular
                + " | empresa: " + idEmpresa);

        if (idTitular == null) {
            return new ArrayList<>();
        }

        // LEFT JOIN explícito: con la navegación implícita (a.facturaCompra.titular)
        // Hibernate genera INNER JOIN y se perderían las aplicaciones sin factura.
        StringBuilder jpql = new StringBuilder(
                " select a from AplicacionPagoCxp a " +
                " left join a.facturaCompra f " +
                " left join f.titular t " +
                " where  a.tipoDocPago = :tipoAnticipo " +
                " and    a.estado = :activo " +
                " and    t.codigo = :idTitular ");
        if (idEmpresa != null) {
            jpql.append(" and a.empresa.codigo = :idEmpresa ");
        }
        jpql.append(" order by a.fechaAplicacion desc, a.id desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("tipoAnticipo", Long.valueOf(TipoDocPagoAplicacion.ANTICIPO));
        query.setParameter("activo", Long.valueOf(EstadoAplicacionPago.ACTIVO));
        query.setParameter("idTitular", idTitular);
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxp> selectCrucesByAnticipoOrigen(Long idAnticipo,
            boolean soloActivas) throws Throwable {
        System.out.println("Ingresa al metodo selectCrucesByAnticipoOrigen con anticipo: "
                + idAnticipo + " | soloActivas: " + soloActivas);

        if (idAnticipo == null) {
            return new ArrayList<>();
        }

        StringBuilder jpql = new StringBuilder(
                " select a from AplicacionPagoCxp a " +
                " where  a.anticipoOrigen.id = :idAnticipo ");
        if (soloActivas) {
            jpql.append(" and a.estado = :activo ");
        }
        jpql.append(" order by a.fechaAplicacion desc, a.id desc");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idAnticipo", idAnticipo);
        if (soloActivas) {
            query.setParameter("activo", Long.valueOf(EstadoAplicacionPago.ACTIVO));
        }
        return query.getResultList();
    }

    // =====================================================================
    // Cartera por pagar (docs/logica-negocio/cxp/API-CARTERA-CXP-CXC.md §3.2, P1-P4)
    // =====================================================================

    // ESTADOEMISION = 3 (ANULADA) del lado compra -- mismo literal que usa
    // GeneradorAtsServiceImpl.anuladosDe para las 4 entidades de venta/compra; no hay una
    // interfaz de rubros para el lado compra (CriterioVentaVigente es explícitamente del lado
    // venta, con otro significado de "estado").
    private static final Long ESTADO_EMISION_ANULADA = Long.valueOf(3L);

    @Override
    public List<Object[]> selectCarteraFacturasCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectCarteraFacturasCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select f.id, f.tipoComprobante, f.numEstablecimiento, f.numPtoEmision, f.secuencial, " +
                "        f.fecha, f.total, f.esIntermediario, " +
                "        f.titular.codigo, f.titular.identificacion, f.titular.razonSocial, f.titular.nombre " +
                " from   FacturaCompra f " +
                " where  f.empresa.codigo = :idEmpresa " +
                " and    f.estado = :activo " +
                " and    (f.estadoEmision is null or f.estadoEmision <> :anulada) " +
                " and    f.fecha < :corteMasUnDia " +
                " and    f.titular is not null ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }
        jpql.append(" order by f.titular.codigo, f.fecha");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectCarteraLiquidacionesCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectCarteraLiquidacionesCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select l.id, l.tipoComprobante, l.numEstablecimiento, l.numPtoEmision, l.secuencial, " +
                "        l.fecha, l.total, " +
                "        l.titular.codigo, l.titular.identificacion, l.titular.razonSocial, l.titular.nombre " +
                " from   LiquidacionCompraCompra l " +
                " where  l.empresa.codigo = :idEmpresa " +
                " and    l.estado = :activo " +
                " and    (l.estadoEmision is null or l.estadoEmision <> :anulada) " +
                " and    l.fecha < :corteMasUnDia " +
                " and    l.titular is not null ");
        if (idTitular != null) {
            jpql.append(" and l.titular.codigo = :idTitular ");
        }
        jpql.append(" order by l.titular.codigo, l.fecha");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectAplicacionesCarteraFacturaCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            LocalDate fechaCorte, Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectAplicacionesCarteraFacturaCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select a.facturaCompra.id, a.tipoDocPago, sum(a.montoAplicado) " +
                " from   AplicacionPagoCxp a " +
                " where  a.facturaCompra.empresa.codigo = :idEmpresa " +
                " and    a.facturaCompra.estado = :activo " +
                " and    (a.facturaCompra.estadoEmision is null or a.facturaCompra.estadoEmision <> :anulada) " +
                " and    a.facturaCompra.fecha < :corteMasUnDia " +
                " and    a.estado = :activoAplic " +
                " and    a.fechaAplicacion <= :fechaCorte ");
        if (idTitular != null) {
            jpql.append(" and a.facturaCompra.titular.codigo = :idTitular ");
        }
        jpql.append(" group by a.facturaCompra.id, a.tipoDocPago");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        query.setParameter("activoAplic", Long.valueOf(EstadoAplicacionPago.ACTIVO));
        query.setParameter("fechaCorte", fechaCorte);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectAplicacionesCarteraLiquidacionCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            LocalDate fechaCorte, Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectAplicacionesCarteraLiquidacionCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select a.liquidacionCompra.id, a.tipoDocPago, sum(a.montoAplicado) " +
                " from   AplicacionPagoCxp a " +
                " where  a.liquidacionCompra.empresa.codigo = :idEmpresa " +
                " and    a.liquidacionCompra.estado = :activo " +
                " and    (a.liquidacionCompra.estadoEmision is null or a.liquidacionCompra.estadoEmision <> :anulada) " +
                " and    a.liquidacionCompra.fecha < :corteMasUnDia " +
                " and    a.estado = :activoAplic " +
                " and    a.fechaAplicacion <= :fechaCorte ");
        if (idTitular != null) {
            jpql.append(" and a.liquidacionCompra.titular.codigo = :idTitular ");
        }
        jpql.append(" group by a.liquidacionCompra.id, a.tipoDocPago");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        query.setParameter("activoAplic", Long.valueOf(EstadoAplicacionPago.ACTIVO));
        query.setParameter("fechaCorte", fechaCorte);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectPlazosCarteraFacturaCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectPlazosCarteraFacturaCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select p.factura.id, p.plazo, p.unidadTiempo " +
                " from   FormaPagoFacturaCompra p " +
                " where  p.factura.empresa.codigo = :idEmpresa " +
                " and    p.factura.estado = :activo " +
                " and    (p.factura.estadoEmision is null or p.factura.estadoEmision <> :anulada) " +
                " and    p.factura.fecha < :corteMasUnDia ");
        if (idTitular != null) {
            jpql.append(" and p.factura.titular.codigo = :idTitular ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectPlazosCarteraLiquidacionCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
            Long idTitular) throws Throwable {
        System.out.println("Ingresa al metodo selectPlazosCarteraLiquidacionCompra con empresa: " + idEmpresa
                + " | corte: " + corteMasUnDia + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select p.liquidacion.id, p.plazo, p.unidadTiempo " +
                " from   FormaPagoLiquidacionCompraCompra p " +
                " where  p.liquidacion.empresa.codigo = :idEmpresa " +
                " and    p.liquidacion.estado = :activo " +
                " and    (p.liquidacion.estadoEmision is null or p.liquidacion.estadoEmision <> :anulada) " +
                " and    p.liquidacion.fecha < :corteMasUnDia ");
        if (idTitular != null) {
            jpql.append(" and p.liquidacion.titular.codigo = :idTitular ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("anulada", ESTADO_EMISION_ANULADA);
        query.setParameter("corteMasUnDia", corteMasUnDia);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> sumaSaldoDisponibleAnticiposPorTitular(Long idEmpresa, Long idTitular)
            throws Throwable {
        System.out.println("Ingresa al metodo sumaSaldoDisponibleAnticiposPorTitular con empresa: " + idEmpresa
                + " | titular: " + idTitular);
        StringBuilder jpql = new StringBuilder(
                " select a.titular.codigo, sum(a.saldo) " +
                " from   AnticipoProveedor a " +
                " where  a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0 ");
        if (idTitular != null) {
            jpql.append(" and a.titular.codigo = :idTitular ");
        }
        jpql.append(" group by a.titular.codigo");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }
}
