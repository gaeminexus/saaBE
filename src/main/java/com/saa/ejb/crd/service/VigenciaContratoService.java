package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.SolicitudVigenciaContrato;
import com.saa.ejb.crd.service.dto.VigenciaDTO;
import com.saa.model.crd.VigenciaContrato;

import jakarta.ejb.Local;

/**
 * Vigencias de aporte de un {@link com.saa.model.crd.Contrato} (CRD.VGCN). El valor
 * operativo es siempre {@code monto}; el porcentaje sólo recalcula el monto al CREAR una
 * vigencia (D7/D8 de docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md).
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Local
public interface VigenciaContratoService extends EntityService<VigenciaContrato> {

    /**
     * Historial completo de vigencias de un contrato, más reciente primero.
     *
     * @param idContrato : Código del contrato
     * @throws Throwable : Excepcion
     */
    List<VigenciaDTO> selectByContrato(Long idContrato) throws Throwable;

    /**
     * Crea una vigencia nueva: cierra la vigencia abierta del mismo (contrato, tipo) con
     * {@code fechaFin = fechaInicio - 1 día} y abre la nueva, en la misma transacción.
     * Después actualiza el espejo en {@code CRD.CNTR}.
     *
     * En modo CALCULADO resuelve la remuneración desde {@code CRD.PRTC.PRTCRMUN} del
     * partícipe del contrato (no viaja en la solicitud): D8 dice que el porcentaje sólo
     * recalcula el monto al crear, así que la remuneración usada queda fija en la vigencia
     * aunque la del partícipe cambie después.
     *
     * @param solicitud : Datos de la nueva vigencia
     * @throws Throwable : IncomeException si el contrato/tipo de aporte no existen, si la
     *                     nueva fecha de inicio no es posterior a la vigencia abierta actual,
     *                     si el modo es inválido, o si es CALCULADO sin remuneración conocida
     */
    VigenciaDTO crear(SolicitudVigenciaContrato solicitud) throws Throwable;

    /**
     * Anula la vigencia ABIERTA (VGCNIDST = 0). Sólo se puede anular la vigencia abierta;
     * una ya cerrada por otra vigencia no se toca. Actualiza el espejo después.
     *
     * @param idVigencia : Código de la vigencia
     * @param usuario    : Usuario que anula (sólo para log; VGCN no tiene columna de
     *                     auditoría de anulación)
     * @throws Throwable : IncomeException si no existe o si no es la vigencia abierta
     */
    void anular(Long idVigencia, String usuario) throws Throwable;

    /**
     * esperado(entidad, tipo, mes) = el monto de la vigencia con VGCNIDST=1 cuyo rango
     * cubre el ÚLTIMO DÍA del mes de {@code mes}. 0.0 si no hay ninguna (nunca lanza) —
     * D-esperado del plan.
     *
     * @param idContrato   : Código del contrato
     * @param idTipoAporte : Código del tipo de aporte (9 jubilación, 11 cesantía)
     * @param mes          : Cualquier día del mes a evaluar; se usa su último día
     * @throws Throwable : Excepcion
     */
    double esperado(Long idContrato, Long idTipoAporte, LocalDate mes) throws Throwable;

    /**
     * Igual que {@link #esperado(Long, Long, LocalDate)}, pero resolviendo el contrato a
     * partir de la entidad (partícipe) en vez de recibir directamente el idContrato. Es el
     * punto de entrada que usa la carga Petro (por entidad, no por contrato). 0.0 si la
     * entidad no tiene contrato activo.
     *
     * @param idEntidad    : Código de la entidad (partícipe)
     * @param idTipoAporte : Código del tipo de aporte (9 jubilación, 11 cesantía)
     * @param mes          : Cualquier día del mes a evaluar; se usa su último día
     * @throws Throwable : Excepcion
     */
    double esperadoPorEntidad(Long idEntidad, Long idTipoAporte, LocalDate mes) throws Throwable;

    /**
     * Versión EN BLOQUE de {@link #esperadoPorEntidad}: resuelve el esperado de TODAS las
     * entidades ACTIVO/ACTIVO_EN_MORA de una filial, para cada mes de [desde, hasta], en una
     * sola consulta a la base (no una por entidad × mes × tipo). Pensada para procesos batch
     * como {@code GeneracionArchivoPetroServiceImpl.recopilarAportesPorFaltante}, donde
     * llamar {@link #esperadoPorEntidad} dentro de un doble bucle partícipe × mes fue medido
     * en decenas de miles de consultas por generación (2026-08-27).
     *
     * <b>Es la MISMA regla que {@link #esperadoPorEntidad}</b> (contrato activo con desempate
     * por mayor código, vigencia con {@code idEstado} ACTIVO cuyo rango cubre el último día
     * del mes) — una sola implementación de la regla, expuesta en dos formas de acceso
     * (una entidad vs. toda una filial). Quien cambie la regla de selección de vigencia la
     * cambia acá, no en el llamador.
     *
     * @param codigoFilial : Código de la filial (CRD.FLLL)
     * @param desde        : Primer día del primer mes a resolver, inclusive
     * @param hasta        : Primer día del último mes a resolver, inclusive
     * @return             : mapa {@code "idEntidad|idTipoAporte|mes" -> monto esperado ese mes}
     *                       (0.0 si no hay vigencia vigente); usar {@code getOrDefault(clave, 0.0)}
     *                       en el llamador por si una combinación no aparece
     * @throws Throwable   : Excepcion
     */
    Map<String, Double> esperadoEnLotePorFilial(Long codigoFilial, LocalDate desde, LocalDate hasta) throws Throwable;

}
