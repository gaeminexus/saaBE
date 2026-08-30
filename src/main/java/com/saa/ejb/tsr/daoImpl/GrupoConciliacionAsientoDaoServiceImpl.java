/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.tsr.GrupoConciliacionAsiento;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPartidaTransito;
import com.saa.rubros.TipoPartidaTransito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion GrupoConciliacionAsientoDaoService.
 */
@SuppressWarnings("unchecked")
@Stateless
public class GrupoConciliacionAsientoDaoServiceImpl extends EntityDaoImpl<GrupoConciliacionAsiento>
        implements GrupoConciliacionAsientoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{"codigo", "grupo", "detalleAsiento"};
    }

    @Override
    public List<GrupoConciliacionAsiento> selectByGrupo(Long idGrupo) throws Throwable {
        Query query = em.createQuery(
            " select e from GrupoConciliacionAsiento e where e.grupo.codigo = :idGrupo ");
        query.setParameter("idGrupo", idGrupo);
        return query.getResultList();
    }

    /**
     * Filtro común de "arrastre" (ver
     * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §7): una fila cuyo
     * asiento cae dentro del rango pedido, O una fila cuyo asiento es el de un TSR.MVCB que sea
     * el origen (MVCBCDGO) de una TSR.DTCN tipo 1/2 todavía Pendiente - un depósito en tránsito
     * o cheque girado declarado en un cierre anterior y aún sin saldar.
     */
    private static final String FRAGMENTO_ARRASTRE_ASIENTO =
            " ( d.asiento.fechaAsiento between :primerDia and :ultimoDia " +
            "   or d.asiento.codigo in ( " +
            "       select dt.movimientoBanco.asiento.codigo from DetalleTransito dt " +
            "       where dt.tipo in (:tipoDeposito, :tipoCheque) and dt.estado = :pendienteTransito " +
            "       and dt.movimientoBanco is not null " +
            "   ) " +
            " ) ";

    @Override
    public List<DetalleAsiento> selectPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia,
            LocalDate ultimoDia) throws Throwable {
        System.out.println("Ingresa al metodo selectPendientes (asiento) con idPlanCuenta: " + idPlanCuenta
                + ", idEmpresa: " + idEmpresa + ", entre " + primerDia + " y " + ultimoDia);
        Query query = em.createQuery(
            " select d from DetalleAsiento d " +
            " where d.planCuenta.codigo = :idPlanCuenta " +
            " and d.asiento.empresa.codigo = :idEmpresa " +
            " and d.asiento.estado in (1,3) " +
            " and " + FRAGMENTO_ARRASTRE_ASIENTO +
            " and d.codigo not in ( " +
            "     select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) " +
            " order by d.asiento.fechaAsiento, d.asiento.codigo ");
        query.setParameter("idPlanCuenta", idPlanCuenta);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("tipoDeposito", Long.valueOf(TipoPartidaTransito.DEPOSITO_EN_TRANSITO));
        query.setParameter("tipoCheque", Long.valueOf(TipoPartidaTransito.CHEQUE_GIRADO_NO_COBRADO));
        query.setParameter("pendienteTransito", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        List<DetalleAsiento> resultado = query.getResultList();
        // La fecha "original" ya viene en asiento.fechaAsiento; solo hace falta marcar la
        // bandera - no persistido, ver el javadoc del campo en la entidad.
        for (DetalleAsiento detalle : resultado) {
            LocalDate fecha = detalle.getAsiento().getFechaAsiento();
            boolean esDelRangoPedido = fecha != null && !fecha.isBefore(primerDia) && !fecha.isAfter(ultimoDia);
            detalle.setEsArrastrada(!esDelRangoPedido);
        }
        return resultado;
    }

    @Override
    public Long contarPendientes(Long idPlanCuenta, Long idEmpresa, LocalDate primerDia, LocalDate ultimoDia)
            throws Throwable {
        Query query = em.createQuery(
            " select count(d) from DetalleAsiento d " +
            " where d.planCuenta.codigo = :idPlanCuenta " +
            " and d.asiento.empresa.codigo = :idEmpresa " +
            " and d.asiento.estado in (1,3) " +
            " and " + FRAGMENTO_ARRASTRE_ASIENTO +
            " and d.codigo not in ( " +
            "     select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            "     where g.grupo.estado = :estadoActivo " +
            " ) ");
        query.setParameter("idPlanCuenta", idPlanCuenta);
        query.setParameter("idEmpresa", idEmpresa);
        query.setParameter("primerDia", primerDia);
        query.setParameter("ultimoDia", ultimoDia);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        query.setParameter("tipoDeposito", Long.valueOf(TipoPartidaTransito.DEPOSITO_EN_TRANSITO));
        query.setParameter("tipoCheque", Long.valueOf(TipoPartidaTransito.CHEQUE_GIRADO_NO_COBRADO));
        query.setParameter("pendienteTransito", Long.valueOf(EstadoPartidaTransito.PENDIENTE));
        return (Long) query.getSingleResult();
    }

    @Override
    public List<Long> selectIdsEnGrupoActivo(List<Long> idsDetalleAsiento) throws Throwable {
        if (idsDetalleAsiento == null || idsDetalleAsiento.isEmpty()) {
            return List.of();
        }
        Query query = em.createQuery(
            " select g.detalleAsiento.codigo from GrupoConciliacionAsiento g " +
            " where g.detalleAsiento.codigo in :ids " +
            " and g.grupo.estado = :estadoActivo ");
        query.setParameter("ids", idsDetalleAsiento);
        query.setParameter("estadoActivo", Long.valueOf(Estado.ACTIVO));
        return query.getResultList();
    }
}
