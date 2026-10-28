package com.saa.ejb.asoprep.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.asoprep.service.ReprocesoCargaPetroService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.AsientoCargaPetroDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso;
import com.saa.model.crd.Entidad;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.ProvisionInteresService;
import com.saa.ejb.crd.service.dto.ResultadoReprocesoCargaPetro;
import com.saa.ejb.crd.service.dto.ResultadoReversoProvision;
import com.saa.ejb.crd.service.dto.SolicitudReprocesoCargaPetro;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.AsientoCargaPetro;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TransferenciaCargaPetro;
import com.saa.rubros.CrdEstadoCargaArchivo;
import com.saa.rubros.CrdTipoMovimientoAporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.SubProcesoCobroPetro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see ReprocesoCargaPetroService
 *
 * Reproceso de una carga Petro ya aplicada — cuadre Superintendencia, cambio C4. Diseño,
 * tablas tocadas y criterio de aceptación en
 * {@code docs/logica-negocio/petro/REPROCESO-CARGA-PETRO.md}.
 *
 * <p><b>Regla de oro: anular = marcar, nunca borrar.</b> Cada fila anulada conserva su
 * valor y recibe la etiqueta de corrida ({@code "REPROCESO carga N corrida M"}) para poder
 * identificar, por corrida, exactamente qué se deshizo. Lo que la aplicación anterior dejó
 * en tablas de auditoría que sí se reescriben por diseño ({@code CRD.DSBN}, distribución en
 * bandas) lo reescribe la propia re-aplicación, que ya es idempotente para ese origen.</p>
 *
 * @author Equipo cuadre Superintendencia
 * @since 2026-10-28
 */
@Stateless
public class ReprocesoCargaPetroServiceImpl implements ReprocesoCargaPetroService {

    /** Largo de {@code PGPRMTAN} (motivo de anulación del pago). */
    private static final int MAX_MOTIVO_PAGO = 500;
    /** Largo de {@code APRTGLSA}. */
    private static final int MAX_GLOSA_APORTE = 2000;

    @EJB
    private CargaArchivoService cargaArchivoService;

    @EJB
    private CargaArchivoPetroService cargaArchivoPetroService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private AsientoCargaPetroDaoService asientoCargaPetroDaoService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private TransferenciaCargaPetroDaoService transferenciaCargaPetroDaoService;

    @EJB
    private ProvisionInteresService provisionInteresService;

    @EJB
    private FechaService fechaService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoReprocesoCargaPetro reprocesar(Long idCarga, SolicitudReprocesoCargaPetro solicitud)
            throws Throwable {
        System.out.println("=== REPROCESO CARGA PETRO - Carga: " + idCarga + " ===");

        // ------------------------------------------------------------------
        // 0. Validaciones previas (ninguna toca datos)
        // ------------------------------------------------------------------
        if (idCarga == null) {
            throw new IncomeException("idCarga es obligatorio");
        }
        if (solicitud == null || solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException("El usuario que ordena el reproceso es obligatorio.");
        }
        if (solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            throw new IncomeException("El motivo del reproceso es obligatorio: queda estampado en"
                    + " cada pago, aporte y asiento anulado.");
        }
        String usuario = solicitud.getUsuario().trim();
        String motivo = solicitud.getMotivo().trim();

        CargaArchivo carga = cargaArchivoService.selectById(idCarga);
        if (carga == null) {
            throw new IncomeException("No existe la carga Petro " + idCarga);
        }
        if (carga.getMesAfectacion() == null || carga.getAnioAfectacion() == null) {
            throw new IncomeException("La carga " + idCarga + " no tiene mes/año de afectación;"
                    + " no se puede fechar el reproceso.");
        }
        // Misma precondición 0b de aplicarPagosArchivoPetro (REGLAS-CARGA-PETRO.md §3.1): sin el
        // paso 1 (transferencias registradas + recepción confirmada) no hay empresa contable
        // que resolver para los asientos de reparto/aplicación ni asiento transitorio que
        // repartir. No se salta acá: el reproceso usa la MISMA lógica vigente.
        if (carga.getFechaAutorizacionContabilidad() == null) {
            throw new IncomeException("La carga " + idCarga + " no tiene la recepción confirmada por"
                    + " contabilidad (paso 1 del cobro en dos pasos). Registre las transferencias"
                    + " con que Petro pagó esta carga (POST /rest/asgn/transferencias) y confirme la"
                    + " recepción (POST /rest/asgn/confirmarRecepcion/" + idCarga + " con"
                    + " \"paraReproceso\": true si la carga ya está en estado 3) antes de reprocesarla.");
        }
        List<TransferenciaCargaPetro> transferenciasVigentes =
                transferenciaCargaPetroDaoService.selectVigentesByCarga(idCarga);
        if (transferenciasVigentes == null || transferenciasVigentes.isEmpty()) {
            throw new IncomeException("La carga " + idCarga + " está confirmada pero no tiene"
                    + " transferencias vigentes; no se puede resolver la empresa contable.");
        }
        Long idEmpresa = transferenciaCargaPetroDaoService.resolverEmpresa(transferenciasVigentes);

        LocalDate fechaContable = fechaService.ultimoDiaMesAnioLocal(carga.getMesAfectacion(), carga.getAnioAfectacion());
        LocalDateTime ahora = LocalDateTime.now();

        // ------------------------------------------------------------------
        // 1. Número de corrida y etiqueta estándar
        // ------------------------------------------------------------------
        int corrida = 1 + Math.max(
                nvl(pagoPrestamoDaoService.selectMaxCorridaReproceso(idCarga)),
                nvl(aporteDaoService.selectMaxCorridaReproceso(idCarga)));
        String etiqueta = "REPROCESO carga " + idCarga + " corrida " + corrida;
        String motivoCompleto = etiqueta + " - " + motivo;
        System.out.println("  Etiqueta de corrida: " + etiqueta + " | usuario " + usuario);

        ResultadoReprocesoCargaPetro resultado = new ResultadoReprocesoCargaPetro();
        resultado.setIdCarga(idCarga);
        resultado.setMesAfectacion(carga.getMesAfectacion());
        resultado.setAnioAfectacion(carga.getAnioAfectacion());
        resultado.setFechaContable(fechaContable);
        resultado.setCorrida(corrida);
        resultado.setEtiqueta(etiqueta);

        // ------------------------------------------------------------------
        // 2. Anular asientos de REPARTO y APLICACION (el TRANSITORIO del paso 1 se conserva)
        // ------------------------------------------------------------------
        anularAsientosDeAplicacion(idCarga, usuario, motivoCompleto, resultado);

        // ------------------------------------------------------------------
        // 3. Anular pagos de préstamo (enlazados + esquema viejo por observación)
        // ------------------------------------------------------------------
        anularPagosPrestamo(idCarga, idEmpresa, fechaContable, usuario, motivoCompleto, ahora, resultado);

        // ------------------------------------------------------------------
        // 4. Reversar aportes (enlazados + esquema viejo por glosa) con contra-movimiento
        // ------------------------------------------------------------------
        reversarAportes(idCarga, usuario, motivoCompleto, ahora, resultado);

        // ------------------------------------------------------------------
        // 5. La carga vuelve a CONFIRMADO_CONTABILIDAD (el paso 1 sigue hecho: CRARFCAC intacta)
        // ------------------------------------------------------------------
        carga.setEstado(Long.valueOf(CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD));
        carga = cargaArchivoService.saveSingle(carga);
        System.out.println("  Carga " + idCarga + " devuelta a estado CONFIRMADO_CONTABILIDAD ("
                + CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD + ") para re-aplicar.");

        // ------------------------------------------------------------------
        // 6. Re-aplicar con la lógica vigente (fase 3 completa + asientos de reparto/aplicación)
        // ------------------------------------------------------------------
        Map<String, Object> aplicacion = cargaArchivoPetroService.aplicarPagosArchivoPetroReproceso(idCarga, etiqueta,
                resolverExcedentesDevueltos(solicitud.getExcedentesDevueltos()));
        resultado.setResumenAplicacion((String) aplicacion.get("resumen"));
        @SuppressWarnings("unchecked")
        List<String> excedentes = (List<String>) aplicacion.get("excedentesDevueltos");
        resultado.setDetalleExcedentesDevueltos(excedentes != null ? excedentes : new ArrayList<>());
        resultado.setExcedentesDevueltos(excedentes != null ? excedentes.size() : 0);
        Object montoExcedentes = aplicacion.get("montoExcedentesDevueltos");
        resultado.setMontoExcedentesDevueltos(montoExcedentes instanceof Number ? ((Number) montoExcedentes).doubleValue() : 0.0);
        @SuppressWarnings("unchecked")
        List<String> sinParticipe = (List<String>) aplicacion.get("sinParticipe");
        resultado.setDetalleSinParticipe(sinParticipe != null ? sinParticipe : new ArrayList<>());
        resultado.setFilasSinParticipe(sinParticipe != null ? sinParticipe.size() : 0);
        Object montoSinParticipe = aplicacion.get("montoSinParticipe");
        resultado.setMontoSinParticipe(montoSinParticipe instanceof Number ? ((Number) montoSinParticipe).doubleValue() : 0.0);

        // ------------------------------------------------------------------
        // 7. Medir lo aplicado (mismas consultas que usa el asiento de aplicación)
        // ------------------------------------------------------------------
        medirAplicado(idCarga, resultado);

        resultado.setMensaje("Carga " + idCarga + " reprocesada (" + etiqueta + "): anulados "
                + resultado.getPagosAnulados() + " pagos ($" + formato(resultado.getMontoPagosAnulados()) + ") y "
                + resultado.getAportesReversados() + " aportes ($" + formato(resultado.getMontoAportesReversados())
                + "); aplicados " + resultado.getPagosAplicados() + " pagos ($" + formato(resultado.getMontoPagosAplicados())
                + ") y " + resultado.getAportesAplicados() + " aportes ($" + formato(resultado.getMontoAportesAplicados())
                + "); " + resultado.getFilasSinParticipe() + " filas sin partícipe ($"
                + formato(resultado.getMontoSinParticipe()) + ") reportadas como novedad; "
                + resultado.getExcedentesDevueltos() + " excedentes devueltos ($"
                + formato(resultado.getMontoExcedentesDevueltos()) + ") registrados como aporte (D-010).");
        System.out.println("=== REPROCESO CARGA PETRO - FIN: " + resultado.getMensaje());
        return resultado;
    }

    // =====================================================================
    // 2. Asientos
    // =====================================================================

    /**
     * Anula (o reversa, si el período está mayorizado — lo decide {@code anulaAsiento}) los
     * asientos VIGENTES de REPARTO y APLICACION de la carga y marca su fila de {@code CRD.ANCP}
     * como reversada, igual que {@code CobroPetroContableServiceImpl.reversarRecepcion} hace
     * con el TRANSITORIO. El TRANSITORIO no se toca: pertenece al paso 1 y sigue vigente.
     */
    private void anularAsientosDeAplicacion(Long idCarga, String usuario, String motivoCompleto,
            ResultadoReprocesoCargaPetro resultado) throws Throwable {
        int[] subProcesos = { SubProcesoCobroPetro.REPARTO, SubProcesoCobroPetro.APLICACION };
        for (int subProceso : subProcesos) {
            AsientoCargaPetro registro = asientoCargaPetroDaoService.selectVigenteByCargaYSubProceso(idCarga, subProceso);
            if (registro == null) {
                continue;
            }
            if (registro.getAsiento() != null) {
                asientoService.anulaAsiento(registro.getAsiento(), usuario, motivoCompleto);
                resultado.getAsientosAnulados().add(registro.getAsiento());
                System.out.println("  Asiento " + registro.getAsiento() + " (sub-proceso " + subProceso
                        + ") anulado/reversado.");
            }
            registro.setIdEstado(0L);
            registro.setObservacion(concatena(registro.getObservacion(), "REVERSADO: " + motivoCompleto));
            asientoCargaPetroDaoService.save(registro, registro.getCodigo());
        }
    }

    // =====================================================================
    // 3. Pagos de préstamo
    // =====================================================================

    private void anularPagosPrestamo(Long idCarga, Long idEmpresa, LocalDate fechaContable, String usuario,
            String motivoCompleto, LocalDateTime ahora, ResultadoReprocesoCargaPetro resultado) throws Throwable {

        // Enlazados (CRARCDGO, esquema vigente) + esquema viejo (sin CRARCDGO, la carga solo
        // figura en la observación "Pago cuota #N - Mes M/AAAA - Carga N [CargaArchivo: N]").
        Map<Long, PagoPrestamo> porCodigo = new LinkedHashMap<>();
        List<PagoPrestamo> enlazados = pagoPrestamoDaoService.selectVigentesByCargaArchivo(idCarga);
        if (enlazados != null) {
            for (PagoPrestamo pago : enlazados) {
                porCodigo.put(pago.getCodigo(), pago);
            }
        }
        int sinEnlace = 0;
        List<PagoPrestamo> legado = pagoPrestamoDaoService.selectVigentesLegadoPorObservacionCarga(idCarga);
        if (legado != null) {
            for (PagoPrestamo pago : legado) {
                if (porCodigo.put(pago.getCodigo(), pago) == null) {
                    sinEnlace++;
                }
            }
        }
        List<PagoPrestamo> pagos = new ArrayList<>(porCodigo.values());
        System.out.println("  Pagos de préstamo a anular: " + pagos.size() + " (" + sinEnlace + " del esquema viejo)");
        resultado.setPagosAnuladosSinEnlace(sinEnlace);
        if (pagos.isEmpty()) {
            return;
        }

        // Los pagos del esquema viejo vienen con PRSTCDGO nulo: se completa desde la cuota, que
        // siempre tiene préstamo. Es el valor correcto y lo necesita la re-provisión de abajo.
        Map<Long, DetallePrestamo> cuotas = new LinkedHashMap<>();
        Map<Long, Prestamo> prestamos = new LinkedHashMap<>();
        double montoAnulado = 0.0;
        for (PagoPrestamo pago : pagos) {
            DetallePrestamo cuota = pago.getDetallePrestamo();
            if (pago.getPrestamo() == null && cuota != null) {
                pago.setPrestamo(cuota.getPrestamo());
            }
            if (cuota != null && cuota.getCodigo() != null) {
                cuotas.put(cuota.getCodigo(), cuota);
            }
            if (pago.getPrestamo() != null && pago.getPrestamo().getCodigo() != null) {
                prestamos.put(pago.getPrestamo().getCodigo(), pago.getPrestamo());
            }
            montoAnulado += nvl(pago.getValor());
        }

        // Re-provisión de intereses por lo que estos pagos habían reversado (ÍTEM 4, diseño §5):
        // solo genera MVIC tipo 4 + asiento si algún pago tiene un reverso tipo 2 vigente; los
        // pagos del esquema viejo no lo tienen y el resultado queda vacío. Se hace ANTES de
        // marcar los pagos, con la fecha contable del mes de la carga.
        ResultadoReversoProvision reProvision = provisionInteresService.reProvisionarPorAnulacion(
                pagos, idEmpresa, fechaContable, "REPROCESO_PETRO", idCarga, usuario);
        resultado.setAsientoReProvision(reProvision != null ? reProvision.getIdAsiento() : null);

        String motivoPago = truncar(motivoCompleto, MAX_MOTIVO_PAGO);
        for (PagoPrestamo pago : pagos) {
            pago.setAnulado(1L);
            pago.setUsuarioAnulacion(usuario);
            pago.setFechaAnulacion(ahora);
            pago.setMotivoAnulacion(motivoPago);
            pagoPrestamoDaoService.save(pago, pago.getCodigo());
        }
        resultado.setPagosAnulados(pagos.size());
        resultado.setMontoPagosAnulados(redondear(montoAnulado));

        // Cada cuota tocada se reconstruye desde sus pagos VIGENTES (mismo camino que el
        // reverso de un evento de pago): *Pagado, saldos, estado y fechaPagado.
        for (DetallePrestamo cuota : cuotas.values()) {
            motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
        }
        resultado.setCuotasRecalculadas(cuotas.size());

        // Un préstamo CANCELADO (3) que ahora tiene cuotas pendientes vuelve a VIGENTE (2): la
        // cancelación automática solo la produce la última cuota pagada, y acaba de dejar de
        // estarlo. Los demás estados terminales (4, 5, 12, 13) no los escribe la carga y no se
        // tocan; EN_MORA/VIGENTE los regula el motor al re-aplicar.
        int reabiertos = 0;
        for (Long idPrestamo : prestamos.keySet()) {
            Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
            if (prestamo == null || prestamo.getIdEstado() == null
                    || prestamo.getIdEstado().intValue() != EstadoPrestamo.CANCELADO) {
                continue;
            }
            Long pendientes = detallePrestamoDaoService.contarCuotasPendientesByPrestamo(idPrestamo);
            if (pendientes != null && pendientes > 0L) {
                prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
                prestamo.setFechaModificacion(ahora);
                prestamoDaoService.save(prestamo, prestamo.getCodigo());
                reabiertos++;
            }
        }
        resultado.setPrestamosReabiertos(reabiertos);
        System.out.println("  Pagos anulados: " + pagos.size() + " ($" + formato(montoAnulado) + "), cuotas recalculadas: "
                + cuotas.size() + ", préstamos reabiertos: " + reabiertos);
    }

    // =====================================================================
    // 4. Aportes
    // =====================================================================

    /**
     * Cada aporte de la carga se neutraliza con un contra-movimiento (fila negativa, tipo
     * REVERSO) con la MISMA fecha de caja y el MISMO devengo, y el original se marca
     * {@code APRTIDST = 0} para que una corrida posterior no lo reverse dos veces. El saldo del
     * partícipe ({@code SUM(APRTVLRR)}), el "aportado(m, tipo)" de la prelación por devengo y la
     * suma por carga del asiento de aplicación quedan todos en cero por construcción. Es el
     * mismo estándar que usa {@code DevolucionAporteServiceImpl} para reversar una devolución.
     */
    private void reversarAportes(Long idCarga, String usuario, String motivoCompleto, LocalDateTime ahora,
            ResultadoReprocesoCargaPetro resultado) throws Throwable {

        Map<Long, Aporte> porCodigo = new LinkedHashMap<>();
        List<Aporte> enlazados = aporteDaoService.selectByCarga(idCarga);
        if (enlazados != null) {
            for (Aporte aporte : enlazados) {
                if (esReversable(aporte)) {
                    porCodigo.put(aporte.getCodigo(), aporte);
                }
            }
        }
        int sinEnlace = 0;
        List<Aporte> legado = aporteDaoService.selectVigentesLegadoPorGlosaCarga(idCarga);
        if (legado != null) {
            for (Aporte aporte : legado) {
                if (esReversable(aporte) && porCodigo.put(aporte.getCodigo(), aporte) == null) {
                    sinEnlace++;
                }
            }
        }
        System.out.println("  Aportes a reversar: " + porCodigo.size() + " (" + sinEnlace + " del esquema viejo)");
        resultado.setAportesReversadosSinEnlace(sinEnlace);

        double montoReversado = 0.0;
        int pagosAporteInactivados = 0;
        for (Aporte original : porCodigo.values()) {
            double valor = nvl(original.getValor());

            Aporte reverso = new Aporte();
            reverso.setEntidad(original.getEntidad());
            reverso.setFilial(original.getFilial());
            reverso.setContrato(original.getContrato());
            reverso.setTipoAporte(original.getTipoAporte());
            reverso.setValor(redondear(-valor));
            reverso.setValorPagado(0.0);
            reverso.setSaldo(0.0);
            reverso.setEstado((long) EstadoCuotaPrestamo.PAGADA);
            // Misma trazabilidad de carga que el original: así la suma por carga
            // (APRTIDAS / CRARCDGO) neutraliza al original en vez de dejarlo contado.
            reverso.setIdAsoprep(original.getIdAsoprep());
            reverso.setCargaArchivo(original.getCargaArchivo());
            // Misma fecha de caja: el saldo a cualquier fecha de corte queda neto en cero.
            reverso.setFechaTransaccion(original.getFechaTransaccion());
            // Mismo periodo EFECTIVO: un negativo sin devengo no cuenta para ningún mes
            // (PERIODO_EFECTIVO_SQL), así que se fija explícito el que tenía el original.
            reverso.setPeriodoDevengo(periodoEfectivo(original));
            reverso.setTipoMovimiento((long) CrdTipoMovimientoAporte.REVERSO);
            reverso.setGlosa(truncar(motivoCompleto + " - reverso APRT " + original.getCodigo()
                    + (original.getGlosa() != null ? " (" + original.getGlosa() + ")" : ""), MAX_GLOSA_APORTE));
            reverso.setUsuarioRegistro(usuario);
            reverso.setFechaRegistro(ahora);
            // DAO directo: AporteServiceImpl.saveSingle fuerza estado = 1 en todo INSERT.
            aporteDaoService.save(reverso, null);

            // Marca del original: reversado por reproceso. Su valor sigue en la fila (lo
            // neutraliza el contra-movimiento), solo cambia el estado para que la próxima
            // corrida no lo tome otra vez.
            original.setEstado(Long.valueOf(Estado.INACTIVO));
            aporteDaoService.save(original, original.getCodigo());

            List<PagoAporte> pagosAporte = pagoAporteDaoService.selectByAporte(original.getCodigo());
            if (pagosAporte != null) {
                for (PagoAporte pagoAporte : pagosAporte) {
                    if (pagoAporte.getEstado() != null && pagoAporte.getEstado().intValue() == Estado.INACTIVO) {
                        continue;
                    }
                    pagoAporte.setEstado(Long.valueOf(Estado.INACTIVO));
                    pagoAporteDaoService.save(pagoAporte, pagoAporte.getCodigo());
                    pagosAporteInactivados++;
                }
            }
            montoReversado += valor;
        }
        resultado.setAportesReversados(porCodigo.size());
        resultado.setMontoAportesReversados(redondear(montoReversado));
        resultado.setPagosAporteInactivados(pagosAporteInactivados);
        System.out.println("  Aportes reversados: " + porCodigo.size() + " ($" + formato(montoReversado)
                + "), PagoAporte inactivados: " + pagosAporteInactivados);
    }

    /** Un aporte entra al reverso si no es él mismo un REVERSO y no fue reversado ya (APRTIDST = 0). */
    private boolean esReversable(Aporte aporte) {
        if (aporte == null || aporte.getCodigo() == null) {
            return false;
        }
        if (aporte.getTipoMovimiento() != null
                && aporte.getTipoMovimiento().intValue() == CrdTipoMovimientoAporte.REVERSO) {
            return false;
        }
        return aporte.getEstado() == null || aporte.getEstado().intValue() != Estado.INACTIVO;
    }

    /** Periodo efectivo del aporte, con la misma regla que {@code AporteDaoServiceImpl.PERIODO_EFECTIVO_SQL}. */
    private LocalDate periodoEfectivo(Aporte aporte) {
        if (aporte.getPeriodoDevengo() != null) {
            return aporte.getPeriodoDevengo();
        }
        if (nvl(aporte.getValor()) > 0 && aporte.getFechaTransaccion() != null) {
            return aporte.getFechaTransaccion().toLocalDate().withDayOfMonth(1);
        }
        return null;
    }

    // =====================================================================
    // D-010: excedentes devueltos — idEntidad se traduce a rol Petro
    // =====================================================================

    /**
     * La solicitud puede identificar cada excedente por rol Petro o por código interno del
     * partícipe ({@code idEntidad}, que es como se arma la lista desde la documentación de
     * crédito sin exponer datos personales). La fase 3 trabaja por rol: acá se traduce. El resto
     * de la validación (fila existente, monto, referencia, tipo de aporte) la hace la fase 3 en
     * {@code validarExcedentesDevueltosReproceso}, todo junto y antes de aplicar nada.
     */
    private List<ExcedenteDevueltoReproceso> resolverExcedentesDevueltos(List<ExcedenteDevueltoReproceso> lista)
            throws Throwable {
        List<ExcedenteDevueltoReproceso> resueltos = new ArrayList<>();
        if (lista == null) {
            return resueltos;
        }
        for (ExcedenteDevueltoReproceso excedente : lista) {
            if (excedente == null) {
                continue;
            }
            if (excedente.getCodigoPetro() == null && excedente.getIdEntidad() != null) {
                Entidad entidad = entidadDaoService.find(new Entidad(), excedente.getIdEntidad());
                if (entidad == null || entidad.getRolPetroComercial() == null) {
                    throw new IncomeException("Excedente devuelto (D-010): la entidad " + excedente.getIdEntidad()
                            + " no existe o no tiene rol Petro; no se puede ubicar su fila en la carga.");
                }
                excedente.setCodigoPetro(entidad.getRolPetroComercial());
            }
            resueltos.add(excedente);
        }
        return resueltos;
    }

    // =====================================================================
    // 7. Medición de lo aplicado
    // =====================================================================

    private void medirAplicado(Long idCarga, ResultadoReprocesoCargaPetro resultado) throws Throwable {
        double montoPagos = 0.0;
        List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByCargaArchivo(idCarga);
        if (pagos != null) {
            for (PagoPrestamo pago : pagos) {
                montoPagos += nvl(pago.getValor());
            }
        }
        resultado.setPagosAplicados(pagos != null ? pagos.size() : 0);
        resultado.setMontoPagosAplicados(redondear(montoPagos));

        // Neto por carga: originales ya reversados (+) con sus contra-movimientos (−) suman
        // cero; quedan solo los aportes de esta corrida. Se cuentan únicamente esos.
        double montoAportes = 0.0;
        int aportesVivos = 0;
        List<Aporte> aportes = aporteDaoService.selectByCarga(idCarga);
        if (aportes != null) {
            for (Aporte aporte : aportes) {
                montoAportes += nvl(aporte.getValor());
                if (esReversable(aporte)) {
                    aportesVivos++;
                }
            }
        }
        resultado.setAportesAplicados(aportesVivos);
        resultado.setMontoAportesAplicados(redondear(montoAportes));

        Set<Long> asientos = new LinkedHashSet<>();
        for (AsientoCargaPetro registro : asientoCargaPetroDaoService.selectByCarga(idCarga)) {
            if (registro.getIdEstado() != null && registro.getIdEstado() == 1L && registro.getAsiento() != null
                    && registro.getSubProceso() != null
                    && registro.getSubProceso().intValue() != SubProcesoCobroPetro.TRANSITORIO) {
                asientos.add(registro.getAsiento());
            }
        }
        resultado.setAsientosGenerados(new ArrayList<>(asientos));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private static int nvl(Integer valor) {
        return valor != null ? valor : 0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private static String formato(double valor) {
        return String.format("%,.2f", valor);
    }

    private static String truncar(String texto, int max) {
        if (texto == null || texto.length() <= max) {
            return texto;
        }
        return texto.substring(0, max);
    }

    private static String concatena(String base, String extra) {
        if (base == null || base.trim().isEmpty()) {
            return extra;
        }
        return base + " | " + extra;
    }
}
