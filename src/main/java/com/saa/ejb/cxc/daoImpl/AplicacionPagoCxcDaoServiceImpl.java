package com.saa.ejb.cxc.daoImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.Factura;
import com.saa.rubros.EstadoAnticipoCliente;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.TipoDocPagoAplicacion;

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

        // ÍTEM 16 (2026-09-15, docs/logica-negocio/tsr/AUDITORIA-ESTADO-CUENTA-TITULAR.md C3):
        // sin este filtro, una retención/NC/ND podía cruzarse contra una factura anulada o
        // todavía no autorizada por el SRI -- mismo criterio que usa GeneradorAtsServiceImpl
        // para <ventas> y los cuadres 103/104 (com.saa.ejb.sri.serviceImpl.CriterioVentaVigente),
        // vuelto public para este uso. No literales sueltos.
        StringBuilder jpql = new StringBuilder(
                " select f from Factura f " +
                " where  FUNCTION('replace', f.numero, '-', '') = :numero " +
                " and    f.estado = :estadoAutorizada " +
                " and    f.estadoEmision <> :estadoEmisionAnulada ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }
        if (idEmpresa != null) {
            jpql.append(" and f.empresa.codigo = :idEmpresa ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("numero", numeroNormalizado);
        query.setParameter("estadoAutorizada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_AUTORIZADA);
        query.setParameter("estadoEmisionAnulada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        return query.getResultList();
    }

    @Override
    public List<AplicacionPagoCxc> selectCrucesAnticipoActivos(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("Ingresa al metodo selectCrucesAnticipoActivos con titular: " + idTitular
                + " | empresa: " + idEmpresa);

        if (idTitular == null) {
            return new ArrayList<>();
        }

        // LEFT JOIN explícito: con la navegación implícita (a.factura.titular)
        // Hibernate genera INNER JOIN y se perderían los cruces sobre
        // liquidaciones, que tienen la factura en null (y viceversa).
        StringBuilder jpql = new StringBuilder(
                " select a from AplicacionPagoCxc a " +
                " left join a.factura f " +
                " left join f.titular tf " +
                " left join a.liquidacion l " +
                " left join l.titular tl " +
                " where  a.tipoDocPago = :tipoAnticipo " +
                " and    a.estado = :activo " +
                " and    (tf.codigo = :idTitular or tl.codigo = :idTitular) ");
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
    public List<AplicacionPagoCxc> selectCrucesByAnticipoOrigen(Long idAnticipo,
            boolean soloActivas) throws Throwable {
        System.out.println("Ingresa al metodo selectCrucesByAnticipoOrigen con anticipo: "
                + idAnticipo + " | soloActivas: " + soloActivas);

        if (idAnticipo == null) {
            return new ArrayList<>();
        }

        StringBuilder jpql = new StringBuilder(
                " select a from AplicacionPagoCxc a " +
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

    @Override
    public List<Object[]> selectListado(Long idEmpresa, Long idTitular, java.time.LocalDate desde,
            java.time.LocalDate hasta, Long formaPago, Long estado) throws Throwable {
        System.out.println("Ingresa al metodo selectListado de AplicacionPagoCxc | idEmpresa: " + idEmpresa
                + " | idTitular: " + idTitular + " | desde: " + desde + " | hasta: " + hasta
                + " | formaPago: " + formaPago + " | estado: " + estado);

        // f/lc y sus respectivos titulares van con LEFT JOIN explicito en
        // cada paso: factura y liquidacion son mutuamente excluyentes (solo
        // una tiene valor por fila), y encadenar una navegacion implicita
        // sobre un alias ya left-joined (f.titular sin "left join" propio)
        // Hibernate la renderiza como INNER JOIN - las filas del lado que
        // esta en null desaparecerian del resultado. Mismo bug que tuvo el
        // listado de cheques (ver ChequeDaoServiceImpl.selectListado).
        StringBuilder jpql = new StringBuilder(
                " select a.id, a.fechaAplicacion, " +
                "        ft.codigo, ft.nombre, lt.codigo, lt.nombre, " +
                "        f.id, f.numero, lc.id, lc.numero, " +
                "        a.tipoDocPago, a.formaPago, a.montoAplicado, " +
                "        asn.codigo, asn.numeroAlterno, a.estado, a.observacion " +
                " from   AplicacionPagoCxc a " +
                " left join a.factura f " +
                " left join f.titular ft " +
                " left join a.liquidacion lc " +
                " left join lc.titular lt " +
                " left join a.asiento asn " +
                " where  1 = 1 ");
        if (idEmpresa != null) {
            jpql.append(" and a.empresa.codigo = :idEmpresa ");
        }
        if (idTitular != null) {
            jpql.append(" and (ft.codigo = :idTitular or lt.codigo = :idTitular) ");
        }
        if (desde != null) {
            jpql.append(" and a.fechaAplicacion >= :desde ");
        }
        if (hasta != null) {
            jpql.append(" and a.fechaAplicacion <= :hasta ");
        }
        if (formaPago != null) {
            jpql.append(" and a.formaPago = :formaPago ");
        }
        if (estado != null) {
            jpql.append(" and a.estado = :estado ");
        }
        jpql.append(" order by a.fechaAplicacion desc, a.id desc ");

        Query query = em.createQuery(jpql.toString());
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        if (desde != null) {
            query.setParameter("desde", desde);
        }
        if (hasta != null) {
            query.setParameter("hasta", hasta);
        }
        if (formaPago != null) {
            query.setParameter("formaPago", formaPago);
        }
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        return query.getResultList();
    }

    // =====================================================================
    // Cartera por cobrar (docs/logica-negocio/cxp/API-CARTERA-CXP-CXC.md §3.2)
    // =====================================================================

    @Override
    public List<Object[]> selectCarteraFacturas(Long idEmpresa, LocalDate fechaCorte, Long idTitular)
            throws Throwable {
        System.out.println("Ingresa al metodo selectCarteraFacturas con empresa: " + idEmpresa
                + " | corte: " + fechaCorte + " | titular: " + idTitular);
        // ÍTEM 6 (2026-09-30): Factura.empresa NO lo llena ningún proceso del backend (verificado
        // -- ningún setEmpresa sobre Factura de venta, sólo sobre FacturaCompra); viaja tal cual
        // lo manda el FE al emitir. La empresa contable real está en facturador.empresa (misma
        // fuente que usa GeneradorAtsServiceImpl). Filtrar sólo por f.empresa perdía en silencio
        // toda factura con esa columna nula. LEFT JOIN explícito a los dos caminos + OR: con
        // navegación implícita Hibernate arma INNER JOIN igual, ambos lados a la vez, y el OR no
        // salva nada -- la fila desaparece antes de llegar al WHERE (mismo bug de selectListado).
        StringBuilder jpql = new StringBuilder(
                " select f.id, f.tipoComprobante, f.numEstablecimiento, f.numPtoEmision, f.secuencial, " +
                "        f.fecha, f.total, " +
                "        f.titular.codigo, f.titular.identificacion, f.titular.razonSocial, f.titular.nombre " +
                " from   Factura f " +
                " left join f.empresa e " +
                " left join f.facturador fc " +
                " left join fc.empresa fe " +
                " where  (e.codigo = :idEmpresa or fe.codigo = :idEmpresa) " +
                " and    f.estado = :estadoAutorizada " +
                " and    (f.estadoEmision is null or f.estadoEmision <> :estadoEmisionAnulada) " +
                " and    f.fecha <= :fechaCorte " +
                " and    f.titular is not null ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }
        jpql.append(" order by f.titular.codigo, f.fecha");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estadoAutorizada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_AUTORIZADA);
        query.setParameter("estadoEmisionAnulada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        query.setParameter("fechaCorte", fechaCorte);
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectAplicacionesCarteraFactura(Long idEmpresa, LocalDate fechaCorte, Long idTitular)
            throws Throwable {
        System.out.println("Ingresa al metodo selectAplicacionesCarteraFactura con empresa: " + idEmpresa
                + " | corte: " + fechaCorte + " | titular: " + idTitular);
        // ÍTEM 6: mismo criterio de empresa que selectCarteraFacturas -- ver ese comentario.
        // "join a.factura f" (INNER, no left): acá sólo interesan las aplicaciones que SÍ afectan
        // una factura de venta (las de liquidación quedan fuera a propósito, CxC no las declara).
        StringBuilder jpql = new StringBuilder(
                " select a.factura.id, a.tipoDocPago, sum(a.montoAplicado) " +
                " from   AplicacionPagoCxc a " +
                " join   a.factura f " +
                " left join f.empresa e " +
                " left join f.facturador fc " +
                " left join fc.empresa fe " +
                " where  (e.codigo = :idEmpresa or fe.codigo = :idEmpresa) " +
                " and    f.estado = :estadoAutorizada " +
                " and    (f.estadoEmision is null or f.estadoEmision <> :estadoEmisionAnulada) " +
                " and    f.fecha <= :fechaCorte " +
                " and    a.estado = :activoAplic " +
                " and    a.fechaAplicacion <= :fechaCorte ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }
        jpql.append(" group by a.factura.id, a.tipoDocPago");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estadoAutorizada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_AUTORIZADA);
        query.setParameter("estadoEmisionAnulada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        query.setParameter("fechaCorte", fechaCorte);
        query.setParameter("activoAplic", Long.valueOf(EstadoAplicacionPago.ACTIVO));
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }

    @Override
    public List<Object[]> selectPlazosCarteraFactura(Long idEmpresa, LocalDate fechaCorte, Long idTitular)
            throws Throwable {
        System.out.println("Ingresa al metodo selectPlazosCarteraFactura con empresa: " + idEmpresa
                + " | corte: " + fechaCorte + " | titular: " + idTitular);
        // ÍTEM 6: mismo criterio de empresa que selectCarteraFacturas -- ver ese comentario.
        StringBuilder jpql = new StringBuilder(
                " select p.factura.id, p.plazo, p.unidadTiempo " +
                " from   FormaPagoFactura p " +
                " join   p.factura f " +
                " left join f.empresa e " +
                " left join f.facturador fc " +
                " left join fc.empresa fe " +
                " where  (e.codigo = :idEmpresa or fe.codigo = :idEmpresa) " +
                " and    f.estado = :estadoAutorizada " +
                " and    (f.estadoEmision is null or f.estadoEmision <> :estadoEmisionAnulada) " +
                " and    f.fecha <= :fechaCorte ");
        if (idTitular != null) {
            jpql.append(" and f.titular.codigo = :idTitular ");
        }

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estadoAutorizada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_AUTORIZADA);
        query.setParameter("estadoEmisionAnulada",
                com.saa.ejb.sri.serviceImpl.CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        query.setParameter("fechaCorte", fechaCorte);
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
                " from   AnticipoCliente a " +
                " where  a.empresa.codigo = :idEmpresa " +
                " and    a.estado = :confirmado " +
                " and    a.valor > 0 ");
        if (idTitular != null) {
            jpql.append(" and a.titular.codigo = :idTitular ");
        }
        jpql.append(" group by a.titular.codigo");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("confirmado", Long.valueOf(EstadoAnticipoCliente.CONFIRMADO));
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        return query.getResultList();
    }
}
