package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.PartidaTransitoAntigua;
import com.saa.model.tsr.PartidaTransitoSolicitud;
import com.saa.model.tsr.PreparacionCierreTransito;
import com.saa.model.tsr.ResultadoCierreTransito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Cierre de conciliación bancaria modelando explícitamente las partidas en tránsito. Ver
 * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md.</p>
 *
 * <p>NO reemplaza la conciliación N:M (ConciliacionContableMatchService.conciliarGrupo/
 * deshacerGrupo, que sigue siendo el mecanismo de emparejamiento): este servicio es el paso de
 * más arriba que, tras el N:M, decide si lo que quedó pendiente se puede declarar en tránsito y
 * cerrar el mes de todas formas, en vez de exigir cero pendientes para siempre.</p>
 */
@Local
public interface ConciliacionCierreService {

    /**
     * Arma la pantalla de cierre: lo conciliado del mes (informativo), los pendientes de ambos
     * lados -incluidos los arrastrados de períodos anteriores todavía sin saldar, ver
     * GrupoConciliacionExtracto/AsientoDaoService.selectPendientes- con el tipo de tránsito
     * propuesto, y los tres números de la ecuación (saldo libros, saldo extracto sugerido,
     * diferencia sugerida) para que el usuario vea si está cerca de cuadrar antes de declarar
     * nada.
     *
     * @param idCuentaBancaria	: Id de la cuenta bancaria
     * @param idPeriodo			: Id del período contable
     * @return					: La preparación completa
     * @throws Throwable		: Excepcion
     */
    PreparacionCierreTransito prepararCierre(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Declara las partidas indicadas como en tránsito, valida que la ecuación cuadre (tolerancia
     * 0.01, NO configurable) contra el {@code saldoExtracto} dado, y si cuadra crea el cierre
     * (TSR.CNCL, con sus totales) y las filas de TSR.DTCN, deja el cierre en CERRADO, y marca la
     * cuenta/período como VERIFICADO (ConciliacionContableService.verificar).
     *
     * <p>Las partidas que NO se incluyan en {@code partidas} siguen pendientes sin declarar: si
     * después de declarar las indicadas queda algún pendiente sin cubrir, este método rechaza -no
     * se cierra con un pendiente mudo. Si la ecuación no cuadra, rechaza con los tres números
     * (libros/extracto/diferencia) en el mensaje, nunca con un "no cuadra" a secas.</p>
     *
     * @param idCuentaBancaria	: Id de la cuenta bancaria
     * @param idPeriodo			: Id del período contable
     * @param partidas			: Partidas a declarar en tránsito en este cierre (puede ser vacía
     *							  si no queda ningún pendiente por declarar)
     * @param saldoExtracto		: Saldo según el estado de cuenta bancario, tal como lo escribe
     *							  el usuario desde el extracto físico
     * @param idUsuario			: Id del usuario que cierra (SCP.PJRQ) - mismo tipo que usa el
     *							  resto del sistema; se resuelve a su nombre internamente sólo
     *							  para guardarlo en TSR.CNCL.CNCLUSCR (texto, igual que el
     *							  campo análogo en ControlExtractoBancario.usuarioCierre)
     * @return					: El resultado del cierre
     * @throws Throwable		: IncomeException si la ecuación no cuadra, si queda algún
     *							  pendiente sin declarar, si el período ya está cerrado, o si
     *							  idUsuario no existe
     */
    ResultadoCierreTransito cerrar(Long idCuentaBancaria, Long idPeriodo, List<PartidaTransitoSolicitud> partidas,
            Double saldoExtracto, Long idUsuario) throws Throwable;

    /**
     * Anula un cierre - solo el último (el más reciente) de esa cuenta/período. Libera las
     * partidas que declaró (vuelven a Pendiente, reaparecen como arrastradas) y deja el CNCL en
     * ANULADO con el motivo. No revierte la verificación de la cuenta/período por sí solo -si ya
     * no hay ningún cierre CERRADO vigente, la próxima llamada a verificar() lo va a rechazar,
     * que es el efecto que se busca.
     *
     * @param idCierre	: Id del cierre (Conciliacion.codigo) a anular
     * @param motivo	: Motivo de la anulación
     * @param idUsuario	: Id del usuario que anula (SCP.PJRQ)
     * @return			: El cierre ya anulado
     * @throws Throwable	: IncomeException si el cierre no existe, ya está anulado, no es el
     *					  último de su cuenta/período, o si idUsuario no existe
     */
    Conciliacion anularCierre(Long idCierre, String motivo, Long idUsuario) throws Throwable;

    /**
     * Partidas en tránsito declaradas hace más de {@code dias} días y todavía Pendientes -riesgo
     * #1 del diseño: una partida que nunca se salda es un síntoma, no un dato.
     *
     * @param idEmpresa	: Id de la empresa; null = todas
     * @param dias		: Umbral en días; si es null, se usa 60 (el default del diseño)
     * @return			: Partidas pendientes más antiguas que el umbral, más antiguas primero
     * @throws Throwable	: Excepcion
     */
    List<PartidaTransitoAntigua> partidasEnTransitoAntiguas(Long idEmpresa, Integer dias) throws Throwable;

}
