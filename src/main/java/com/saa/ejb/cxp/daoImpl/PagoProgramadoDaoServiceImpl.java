package com.saa.ejb.cxp.daoImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoCxp;

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
        // Incluye POR_APROBAR(0) desde el 2026-09-02 (docs/logica-negocio/cxp/
        // DISENO-FACTURAS-COMPROMETIDAS-EN-COMBO-PAGOS.md): es el estado en el que
        // nace un pago desde el frente S cuando no viene cuenta bancaria de origen,
        // el flujo normal hoy. Sin este estado, validaValorContraSaldo (el único
        // llamador) queda ciego a esos pagos y deja registrar dos veces el pago
        // completo de la misma factura.
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.facturaCompra.id = :idFactura " +
                " and    p.estado in (:porAprobar, :registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("idFactura", idFacturaCompra);
        query.setParameter("porAprobar", Long.valueOf(EstadoPagoProgramado.POR_APROBAR));
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("confirmado", Long.valueOf(EstadoPagoProgramado.CONFIRMADO));
        return query.getResultList();
    }

    @Override
    public List<PagoProgramado> selectComprometidosNoConfirmadosByFactura(Long idFacturaCompra) throws Throwable {
        System.out.println("Ingresa al metodo selectComprometidosNoConfirmadosByFactura con factura: " + idFacturaCompra);
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.facturaCompra.id = :idFactura " +
                " and    p.estado in (:porAprobar, :registrado, :enArchivo) " +
                " order by p.id");
        query.setParameter("idFactura", idFacturaCompra);
        query.setParameter("porAprobar", Long.valueOf(EstadoPagoProgramado.POR_APROBAR));
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
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
        // Incluye POR_APROBAR(0) desde el 2026-09-04 (mismo criterio que
        // selectVigentesByFactura desde el 2026-09-02): es el estado en el que
        // nace un pago de origen externo cuando no viene cuenta bancaria de
        // origen (PagoProgramadoServiceImpl.registrarPagoDeOrigenExterno). Sin
        // este estado, la guarda anti-duplicados de ese mismo método quedaba
        // ciega a los pagos que ella misma acababa de crear, y dejaba
        // registrar dos veces la salida de dinero del mismo documento origen.
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.origenExterno = :origen " +
                " and    p.idOrigen = :idOrigen " +
                " and    p.estado in (:porAprobar, :registrado, :enArchivo, :confirmado) " +
                " order by p.id");
        query.setParameter("origen", origen);
        query.setParameter("idOrigen", idOrigen);
        query.setParameter("porAprobar", Long.valueOf(EstadoPagoProgramado.POR_APROBAR));
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

    @Override
    public List<PagoProgramado> selectPorAprobar(Long idEmpresa, String origen,
            LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectPorAprobar con empresa: " + idEmpresa
                + " | origen: " + origen + " | desde: " + desde + " | hasta: " + hasta);

        StringBuilder jpql = new StringBuilder(
                " select p from PagoProgramado p " +
                " where  p.empresa.codigo = :idEmpresa " +
                " and    p.estado = :estado ");
        if (origen != null && !origen.trim().isEmpty()) {
            if (OrigenPagoCxp.FACTURA_COMPRA.equals(origen)) {
                jpql.append(" and p.facturaCompra is not null ");
            } else if (OrigenPagoCxp.EGRESO_TESORERIA.equals(origen)) {
                jpql.append(" and p.egreso is not null ");
            } else if (OrigenPagoCxp.ANTICIPO_PROVEEDOR.equals(origen)) {
                jpql.append(" and p.anticipo is not null ");
            } else {
                // No es uno de los tres propios de CXP: se compara como etiqueta opaca
                // de OrigenPagoExterno, sin resolverla.
                jpql.append(" and p.origenExterno = :origen ");
            }
        }
        if (desde != null) {
            jpql.append(" and p.fechaProgramada >= :desde ");
        }
        if (hasta != null) {
            jpql.append(" and p.fechaProgramada <= :hasta ");
        }
        jpql.append(" order by p.fechaProgramada, p.id ");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("estado", Long.valueOf(EstadoPagoProgramado.POR_APROBAR));
        if (origen != null && !origen.trim().isEmpty()
                && !OrigenPagoCxp.FACTURA_COMPRA.equals(origen)
                && !OrigenPagoCxp.EGRESO_TESORERIA.equals(origen)
                && !OrigenPagoCxp.ANTICIPO_PROVEEDOR.equals(origen)) {
            query.setParameter("origen", origen);
        }
        if (desde != null) {
            query.setParameter("desde", desde);
        }
        if (hasta != null) {
            query.setParameter("hasta", hasta);
        }
        return query.getResultList();
    }

    @Override
    public PagoProgramado selectByAplicacion(Long idAplicacion) throws Throwable {
        System.out.println("Ingresa al metodo selectByAplicacion con idAplicacion: " + idAplicacion);
        if (idAplicacion == null) {
            return null;
        }
        Query query = em.createQuery(
                " select p from PagoProgramado p " +
                " where  p.aplicacion.id = :idAplicacion ");
        query.setParameter("idAplicacion", idAplicacion);
        List<PagoProgramado> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public Double sumaPagosComprometidos(Long idCuentaBancaria, LocalDate fecha) throws Throwable {
        System.out.println("Ingresa al metodo sumaPagosComprometidos con cuenta: " + idCuentaBancaria
                + " | fecha: " + fecha);
        Query query = em.createQuery(
                " select coalesce(sum(p.valor), 0.0) from PagoProgramado p " +
                " where  p.cuentaBancaria.codigo = :idCuentaBancaria " +
                " and    p.estado in (:registrado, :enArchivo) " +
                " and    p.fechaProgramada <= :fecha ");
        query.setParameter("idCuentaBancaria", idCuentaBancaria);
        query.setParameter("registrado", Long.valueOf(EstadoPagoProgramado.REGISTRADO));
        query.setParameter("enArchivo",  Long.valueOf(EstadoPagoProgramado.EN_ARCHIVO));
        query.setParameter("fecha", fecha);
        Object resultado = query.getSingleResult();
        return resultado != null ? ((Number) resultado).doubleValue() : Double.valueOf(0.0);
    }
}
