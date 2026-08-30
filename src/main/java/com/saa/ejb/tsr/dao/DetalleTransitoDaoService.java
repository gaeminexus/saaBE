package com.saa.ejb.tsr.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.DetalleTransito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * DaoService DetalleTransito (TSR.DTCN). Ver
 * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md.
 */
@Local
public interface DetalleTransitoDaoService extends EntityDao<DetalleTransito> {

    /**
     * Partidas declaradas por un cierre (TSR.CNCL) concreto - para anularCierre, que necesita
     * liberarlas todas.
     *
     * @param idCierre      : Id del cierre (Conciliacion.codigo) que las declaró
     * @return              : Partidas declaradas por ese cierre
     * @throws Throwable    : Excepcion
     */
    List<DetalleTransito> selectByCierre(Long idCierre) throws Throwable;

    /**
     * La partida PENDIENTE (si existe) que declaró en tránsito una línea de detalle de asiento
     * concreta (tipo 1/2). Ancla desde el 2026-08-27 (§7bis del diseño): {@code MVCBCDGO} no
     * cubría el 92% de los detalles de asiento sobre cuentas bancarias, así que dejó de ser el
     * ancla de la partida - {@code DetalleAsiento} es lo que siempre existe. Como máximo una
     * fila activa por línea - dos cierres no pueden declarar la misma línea dos veces.
     *
     * @param idDetalleAsiento  : Id de CNT.DTAS
     * @return                  : La partida pendiente, o null si esa línea no está declarada
     * @throws Throwable        : Excepcion
     */
    DetalleTransito selectPendientePorDetalleAsiento(Long idDetalleAsiento) throws Throwable;

    /**
     * La partida PENDIENTE (si existe) que declaró en tránsito una línea de extracto concreta.
     *
     * @param idDetalleExtracto : Id de TSR.DEXB
     * @return                  : La partida pendiente, o null si esa línea no está declarada
     * @throws Throwable        : Excepcion
     */
    DetalleTransito selectPendientePorDetalleExtracto(Long idDetalleExtracto) throws Throwable;

    /**
     * La partida PENDIENTE (si existe), de cualquiera de las líneas del asiento indicado -
     * granularidad de asiento, no de línea, porque {@code saldarPartidasTransitoDeclaradas} solo
     * conoce el id del asiento en ese punto del flujo de matching N:M.
     *
     * @param idAsiento : Id de CNT.ASNT
     * @return          : La partida pendiente, o null
     * @throws Throwable : Excepcion
     */
    DetalleTransito selectPendientePorAsiento(Long idAsiento) throws Throwable;

    /**
     * La partida (en cualquier estado), de cualquiera de las líneas del asiento indicado - para
     * revertir un Saldada de vuelta a Pendiente cuando se deshace el grupo N:M que lo saldó
     * (ConciliacionContableMatchServiceImpl.deshacerGrupo). Mismo motivo de granularidad que
     * {@link #selectPendientePorAsiento(Long)}.
     *
     * @param idAsiento : Id de CNT.ASNT
     * @return          : La partida, o null si ese asiento nunca se declaró
     * @throws Throwable : Excepcion
     */
    DetalleTransito selectPorAsiento(Long idAsiento) throws Throwable;

    /**
     * La partida (en cualquier estado) que tiene a una línea de extracto como origen - mismo uso
     * que {@link #selectPorAsiento(Long)}, para el lado del extracto.
     *
     * @param idDetalleExtracto : Id de TSR.DEXB
     * @return                  : La partida, o null si esa línea nunca se declaró
     * @throws Throwable        : Excepcion
     */
    DetalleTransito selectPorDetalleExtracto(Long idDetalleExtracto) throws Throwable;

    /**
     * Partidas PENDIENTES de una cuenta bancaria (por cualquiera de los dos orígenes) -
     * declaradas por cualquier cierre, de cualquier período. Es la fuente de los "declarados en
     * tránsito y aún pendientes" que arrastran selectPendientes en los dos DAO de grupos de
     * conciliación.
     *
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @return                 : Partidas pendientes de esa cuenta
     * @throws Throwable       : Excepcion
     */
    List<DetalleTransito> selectPendientesPorCuenta(Long idCuentaBancaria) throws Throwable;

    /**
     * Partidas PENDIENTES declaradas hace más de {@code dias} días, para una empresa - el aviso
     * de antigüedad del riesgo #1 del diseño (sin esto, el tránsito se convierte en el basurero
     * donde se esconden los errores).
     *
     * @param idEmpresa : Id de la empresa; null = todas
     * @param diasCorte : fecha de corte ya resuelta (hoy - dias), para no depender de SYSDATE
     *                    en la implementación
     * @return          : Partidas pendientes más antiguas que diasCorte, más antiguas primero
     * @throws Throwable : Excepcion
     */
    List<DetalleTransito> selectPendientesAntiguas(Long idEmpresa, LocalDateTime diasCorte) throws Throwable;

}
