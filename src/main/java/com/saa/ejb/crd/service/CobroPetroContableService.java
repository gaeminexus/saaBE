package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.EstadoContablePetro;
import com.saa.ejb.crd.service.dto.ResultadoConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.ResultadoReversarRecepcion;
import com.saa.ejb.crd.service.dto.ResumenTransferenciasCarga;
import com.saa.ejb.crd.service.dto.SolicitudConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudReversarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudTransferenciaCargaPetro;
import com.saa.ejb.crd.service.dto.TransferenciaCargaPetroDTO;

import jakarta.ejb.Local;

/**
 * Cobro de Petro en dos pasos (regla 11 de §5 de
 * {@code LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md}). CONTRATO CONGELADO con el
 * frontend: ver {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md}. No cambiar la
 * forma de los DTO ni el orden de las validaciones sin acordarlo con el árbitro.
 *
 * <pre>
 *   PASO 1 (este servicio): contabilidad registra las N transferencias con las que Petro
 *   pagó, confirma que el dinero entró al banco, y eso genera el asiento TRANSITORIO
 *   (D Banco(s) → H 2.3.01.15.01, plantilla alterno 19).
 *
 *   PASO 2 ({@code CargaArchivoPetroService.aplicarPagosArchivoPetro}, que exige el paso 1
 *   hecho): asiento de REPARTO (D 2.3.01.15.01 → H 1.4.05.05/1.4.05.10, plantilla 20) y de
 *   APLICACION (plantilla 21 + bandas de CRD.BNDP).
 * </pre>
 *
 * <b>"Confirmada" NUNCA se lee de {@code CRARESTD}.</b> Ese estado es transitorio (avanza a
 * PROCESADO en cuanto se ejecuta el paso 2) — el marcador DURADERO es
 * {@code CargaArchivo.fechaAutorizacionContabilidad != null}. Ver
 * {@link com.saa.rubros.CrdEstadoCargaArchivo#CONFIRMADO_CONTABILIDAD}.
 */
@Local
public interface CobroPetroContableService {

    /**
     * Cabecera + lista de transferencias de una carga, con el cuadre contra el total del
     * archivo. Base de {@code GET /rest/asgn/transferencias/{idCarga}}.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Resumen; nunca null — {@code transferencias} vacía si no se
     *                     registró ninguna
     * @throws Throwable : {@code IncomeException} si la carga no existe
     */
    ResumenTransferenciasCarga resumenTransferencias(Long idCarga) throws Throwable;

    /**
     * Registra una transferencia con la que Petro pagó una carga. Rechaza si la carga ya
     * está confirmada (paso 1 hecho): para corregir, primero se reversa.
     *
     * @param solicitud  : Carga, cuenta bancaria, banco externo, valor, fecha, usuario
     * @return           : La transferencia grabada
     * @throws Throwable : {@code IncomeException} si la carga no existe, ya está confirmada,
     *                     o falta un dato obligatorio
     */
    TransferenciaCargaPetroDTO registrarTransferencia(SolicitudTransferenciaCargaPetro solicitud)
            throws Throwable;

    /**
     * Anula una transferencia (estado 0, no se borra). Rechaza si la carga ya está
     * confirmada.
     *
     * @param idTransferencia : Código de la transferencia (CRD.TRCR)
     * @param usuario         : Usuario que anula
     * @throws Throwable      : {@code IncomeException} si no existe o la carga ya está
     *                          confirmada
     */
    void anularTransferencia(Long idTransferencia, String usuario) throws Throwable;

    /**
     * PASO 1: confirma que el dinero de las transferencias registradas entró al banco.
     * Valida en este orden EXACTO (§2.2 del contrato): (1) la carga existe, (2) no está
     * confirmada ya, (3) hay al menos una transferencia vigente, (4) la suma cuadra con el
     * total del archivo (tolerancia 0.01), (5) la carga está en un estado que admite la
     * confirmación.
     *
     * Sella {@code usuarioContabilidadConfirma}/{@code fechaAutorizacionContabilidad}, mueve
     * {@code CRARESTD} a {@code CONFIRMADO_CONTABILIDAD}, y si
     * {@code ConfiguracionContabilidadService.contabilidadActiva()} genera el asiento
     * transitorio y lo registra en {@code CRD.ANCP} tipo TRANSITORIO. Con la contabilidad
     * apagada la confirmación IGUAL ocurre, sin asiento — no es un error.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @param solicitud  : Usuario, ip, observación
     * @return           : Resultado con el asiento generado (o null si contabilidad está apagada)
     * @throws Throwable : {@code IncomeException} con el motivo exacto de la validación que falló
     */
    ResultadoConfirmarRecepcion confirmarRecepcion(Long idCarga, SolicitudConfirmarRecepcion solicitud)
            throws Throwable;

    /**
     * Reversa la confirmación del paso 1: anula el asiento transitorio (si existe), limpia
     * {@code usuarioContabilidadConfirma}/{@code fechaAutorizacionContabilidad} y devuelve
     * {@code CRARESTD} a {@code CARGADO}. Rechaza si el archivo ya fue procesado (paso 2
     * hecho): primero hay que reversar el paso 2.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @param solicitud  : Usuario, ip, motivo (OBLIGATORIO)
     * @return           : Resultado con el id del asiento anulado (o null si no había asiento)
     * @throws Throwable : {@code IncomeException} si la carga no existe, no está confirmada,
     *                     ya fue procesada, o falta el motivo
     */
    ResultadoReversarRecepcion reversarRecepcion(Long idCarga, SolicitudReversarRecepcion solicitud)
            throws Throwable;

    /**
     * Asientos contables generados para una carga (los tres sub-procesos, los que existan).
     * Lista vacía es un resultado VÁLIDO (todavía no se contabilizó nada), no un error.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @return           : Estado contable; {@code asientos} vacío si no hay ninguno
     * @throws Throwable : {@code IncomeException} si la carga no existe
     */
    EstadoContablePetro estadoContable(Long idCarga) throws Throwable;

    /**
     * PASO 2a: asiento de REPARTO — D {@code 2.3.01.15.01} → H {@code 1.4.05.05}/
     * {@code 1.4.05.10}, plantilla alterno {@code PlantillasCredito.REPARTO_PETRO} (20).
     * Lo llama {@code CargaArchivoPetroServiceImpl.aplicarPagosArchivoPetro} (paso 2), justo
     * antes de marcar la carga PROCESADO — exige que el paso 1 ya esté hecho (lo garantiza
     * {@code CargaArchivoPetroServiceImpl.exigeConfirmacionContabilidad}, no este método).
     *
     * El desglose aportes/préstamos sale de {@code CRD.DTCA.DTCATTDO} agrupado por
     * {@code codigoPetroProducto} (AH = aportes; el resto = préstamos, "TODO" incluido el
     * seguro, per §2.2 del levantamiento) — no hace falta traceability hacia
     * {@code CRD.PGPR}/{@code CRD.APRT} para este asiento en particular.
     *
     * Con {@code contabilidadActiva() == false} no genera asiento (mismo criterio que
     * {@link #confirmarRecepcion}) — no es un error.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @throws Throwable : {@code IncomeException} si la carga no existe o si la plantilla 20
     *                     no tiene las líneas necesarias
     */
    void contabilizarReparto(Long idCarga) throws Throwable;

    /**
     * PASO 2b: asiento de APLICACION — D {@code 2.3.02.05}/{@code 2.3.02.10} → H cuentas
     * reales, plantilla alterno {@code PlantillasCredito.APLICACION_PETRO} (21, renumerada
     * en {@code ACTUALIZACION-PLANTILLA-21-PETRO-APLICACION.md}) + bandas de capital desde
     * {@code CRD.BNDP} vía {@code ClasificadorBandaService}. Lo llama
     * {@code CargaArchivoPetroServiceImpl.aplicarPagosArchivoPetro} (paso 2), después de
     * {@link #contabilizarReparto}.
     *
     * El desglose sale de {@code CRD.PGPR}/{@code CRD.APRT} filtrados por {@code CRARCDGO}
     * (trazabilidad agregada 2026-08-28, ver
     * {@code docs/logica-negocio/crd/sql/DDL-TRAZABILIDAD-CARGA-PETRO.sql}) — cargas
     * anteriores a esa fecha no tienen `CRARCDGO` poblado y este método no genera asiento
     * para ellas (no es un error, es la consecuencia aceptada de no hacer backfill).
     *
     * Con {@code contabilidadActiva() == false} no genera asiento — no es un error.
     *
     * @param idCarga    : Código de la carga (CRD.CRAR)
     * @throws Throwable : {@code IncomeException} si un pago no tiene producto asignado
     *                     (no se puede clasificar su capital por banda), si un componente
     *                     de interés/mora/seguro no tiene tipo de préstamo, o si la
     *                     plantilla 21 no tiene una línea necesaria
     */
    void contabilizarAplicacion(Long idCarga) throws Throwable;
}
