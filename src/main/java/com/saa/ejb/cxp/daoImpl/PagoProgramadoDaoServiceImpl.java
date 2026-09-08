package com.saa.ejb.cxp.daoImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.model.cxp.LotePago;
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
    public List<PagoProgramado> selectByEmpresaEstado(Long idEmpresa, List<Long> estados, Long idTitular,
            Long idCuentaBancaria, List<String> origenes, LocalDate desde, LocalDate hasta, String texto,
            Long formaPago) throws Throwable {
        System.out.println("Ingresa al metodo selectByEmpresaEstado con empresa: " + idEmpresa
                + " | estados: " + estados + " | titular: " + idTitular + " | cuenta: " + idCuentaBancaria
                + " | origenes: " + origenes + " | desde: " + desde + " | hasta: " + hasta
                + " | texto: " + texto + " | formaPago: " + formaPago);

        List<Long> estadosFiltrados = new ArrayList<>();
        if (estados != null) {
            for (Long e : estados) {
                if (e != null) {
                    estadosFiltrados.add(e);
                }
            }
        }

        StringBuilder jpql = new StringBuilder(
                " select p from PagoProgramado p " +
                " left join p.titular t " +
                " where  p.empresa.codigo = :idEmpresa ");
        if (!estadosFiltrados.isEmpty()) {
            jpql.append(" and ( p.estado in :estados ) ");
        }
        if (idTitular != null) {
            jpql.append(" and p.titular.codigo = :idTitular ");
        }
        if (idCuentaBancaria != null) {
            jpql.append(" and p.cuentaBancaria.codigo = :idCuentaBancaria ");
        }

        // "origen" no es una columna (misma trampa que selectPorAprobar): tres valores se
        // traducen a "is not null" sobre asociaciones propias de CXP y el resto compara
        // origenExterno. El OR va en su propio parentesis, aparte del de estados.
        List<String> externos = new ArrayList<>();
        if (origenes != null && !origenes.isEmpty()) {
            List<String> condiciones = new ArrayList<>();
            for (String o : origenes) {
                if (o == null || o.trim().isEmpty()) {
                    continue;
                }
                String origen = o.trim();
                if (OrigenPagoCxp.FACTURA_COMPRA.equals(origen)) {
                    condiciones.add("p.facturaCompra is not null");
                } else if (OrigenPagoCxp.EGRESO_TESORERIA.equals(origen)) {
                    condiciones.add("p.egreso is not null");
                } else if (OrigenPagoCxp.ANTICIPO_PROVEEDOR.equals(origen)) {
                    condiciones.add("p.anticipo is not null");
                } else {
                    externos.add(origen);
                }
            }
            if (!externos.isEmpty()) {
                condiciones.add("p.origenExterno in :origenesExternos");
            }
            if (!condiciones.isEmpty()) {
                jpql.append(" and ( ").append(String.join(" or ", condiciones)).append(" ) ");
            }
        }

        if (desde != null) {
            jpql.append(" and p.fechaProgramada >= :desde ");
        }
        if (hasta != null) {
            jpql.append(" and p.fechaProgramada <= :hasta ");
        }
        if (formaPago != null) {
            jpql.append(" and p.formaPago = :formaPago ");
        }

        // Texto: parcial, sin distinguir mayusculas, sobre observacion o el nombre del
        // beneficiario -- denormalizado (PGTRBFNM, pagos sin titular en el maestro) o del
        // titular (left join arriba: si no se hiciera left join, un pago sin titular
        // desaparecería del resultado entero en cuanto se usara este filtro, sin error).
        boolean hayTexto = texto != null && !texto.trim().isEmpty();
        if (hayTexto) {
            jpql.append(" and ( upper(p.observacion) like :texto "
                    + "or upper(p.beneficiarioNombre) like :texto "
                    + "or upper(t.nombre) like :texto ) ");
        }

        jpql.append(" order by p.fechaProgramada, p.id");

        Query query = em.createQuery(jpql.toString());
        query.setParameter("idEmpresa", idEmpresa);
        if (!estadosFiltrados.isEmpty()) {
            query.setParameter("estados", estadosFiltrados);
        }
        if (idTitular != null) {
            query.setParameter("idTitular", idTitular);
        }
        if (idCuentaBancaria != null) {
            query.setParameter("idCuentaBancaria", idCuentaBancaria);
        }
        if (!externos.isEmpty()) {
            query.setParameter("origenesExternos", externos);
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
        if (hayTexto) {
            query.setParameter("texto", "%" + texto.trim().toUpperCase() + "%");
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

    /**
     * Tope de elementos por lote del {@code IN} de {@link #selectByAsientos(List)}. Oracle
     * rechaza una lista literal de más de 1000 elementos con {@code ORA-01795}; se corta en 900,
     * no en 1000, para dejar margen. Partir en lotes va DENTRO del DAO (no en el llamador) para
     * que cualquier otro caller herede la protección sin tener que acordarse de hacerlo él mismo.
     */
    private static final int TAMANO_LOTE_IN = 900;

    @Override
    public List<PagoProgramado> selectByAsientos(List<Long> idsAsiento) throws Throwable {
        System.out.println("Ingresa al metodo selectByAsientos con " + (idsAsiento != null ? idsAsiento.size() : 0) + " asientos");
        if (idsAsiento == null || idsAsiento.isEmpty()) {
            return new ArrayList<>();
        }
        List<PagoProgramado> resultado = new ArrayList<>();
        for (int desde = 0; desde < idsAsiento.size(); desde += TAMANO_LOTE_IN) {
            List<Long> lote = idsAsiento.subList(desde, Math.min(desde + TAMANO_LOTE_IN, idsAsiento.size()));
            Query query = em.createQuery(
                    " select p from PagoProgramado p " +
                    " where  p.asiento.codigo in (:idsAsiento) " +
                    " order by p.id");
            query.setParameter("idsAsiento", lote);
            resultado.addAll(query.getResultList());
        }
        return resultado;
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
    public List<PagoProgramado> selectPorAprobar(Long idEmpresa, List<String> origenes,
            LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("Ingresa al metodo selectPorAprobar con empresa: " + idEmpresa
                + " | origenes: " + origenes + " | desde: " + desde + " | hasta: " + hasta);

        StringBuilder jpql = new StringBuilder(
                " select p from PagoProgramado p " +
                " where  p.empresa.codigo = :idEmpresa " +
                " and    p.estado = :estado ");

        List<String> externos = new ArrayList<>();
        if (origenes != null && !origenes.isEmpty()) {
            List<String> condiciones = new ArrayList<>();
            for (String o : origenes) {
                if (o == null || o.trim().isEmpty()) {
                    continue;
                }
                String origen = o.trim();
                if (OrigenPagoCxp.FACTURA_COMPRA.equals(origen)) {
                    condiciones.add("p.facturaCompra is not null");
                } else if (OrigenPagoCxp.EGRESO_TESORERIA.equals(origen)) {
                    condiciones.add("p.egreso is not null");
                } else if (OrigenPagoCxp.ANTICIPO_PROVEEDOR.equals(origen)) {
                    condiciones.add("p.anticipo is not null");
                } else {
                    // No es uno de los tres propios de CXP: se compara como etiqueta opaca
                    // de OrigenPagoExterno, sin resolverla.
                    externos.add(origen);
                }
            }
            if (!externos.isEmpty()) {
                condiciones.add("p.origenExterno in :origenesExternos");
            }
            if (!condiciones.isEmpty()) {
                jpql.append(" and ( ").append(String.join(" or ", condiciones)).append(" ) ");
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
        if (!externos.isEmpty()) {
            query.setParameter("origenesExternos", externos);
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

    @Override
    public List<LotePago> selectLotes(Long idEmpresa, LocalDate desde, LocalDate hasta, Integer limite)
            throws Throwable {
        System.out.println("Ingresa al metodo selectLotes con empresa: " + idEmpresa
                + " | desde: " + desde + " | hasta: " + hasta + " | limite: " + limite);

        StringBuilder jpql = new StringBuilder(
                " select l from LotePago l " +
                " where  1 = 1 ");
        if (idEmpresa != null) {
            jpql.append(" and l.empresa.codigo = :idEmpresa ");
        }
        if (desde != null) {
            jpql.append(" and l.fechaGeneracion >= :desde ");
        }
        if (hasta != null) {
            jpql.append(" and l.fechaGeneracion <= :hasta ");
        }
        jpql.append(" order by l.fechaGeneracion desc, l.id desc ");

        Query query = em.createQuery(jpql.toString());
        if (idEmpresa != null) {
            query.setParameter("idEmpresa", idEmpresa);
        }
        if (desde != null) {
            query.setParameter("desde", desde);
        }
        if (hasta != null) {
            query.setParameter("hasta", hasta);
        }
        query.setMaxResults(limite != null ? limite.intValue() : 50);
        return query.getResultList();
    }
}
