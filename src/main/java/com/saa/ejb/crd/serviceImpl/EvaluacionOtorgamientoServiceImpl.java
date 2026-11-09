package com.saa.ejb.crd.serviceImpl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.EvaluacionOtorgamientoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.SustitucionPrestamoService;
import com.saa.ejb.crd.service.dto.EvaluacionOtorgamiento;
import com.saa.ejb.crd.service.dto.SaldoTipoAporte;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Participe;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.ParametroOtorgamiento;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link EvaluacionOtorgamientoService}. Solo LEE: no escribe nada. Los números de capital pendiente
 * salen de {@code PagoPrestamoService.calcularSaldoCapitalPendiente} (la misma regla que traduce a SQL la solicitud
 * {@code RPRT_PRST_SLCT}), nunca de {@code PRSTSLCP}, que es una columna muerta.
 */
@Stateless
public class EvaluacionOtorgamientoServiceImpl implements EvaluacionOtorgamientoService {

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private SaldoAporteService saldoAporteService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @EJB
    private SustitucionPrestamoService sustitucionPrestamoService;

    @Override
    public EvaluacionOtorgamiento evaluar(Long idEntidad, Double monto, Double cuotaNueva, Long idPrestamoActual,
            Long idPrestamoOriginal) throws Throwable {
        return evaluar(idEntidad, monto, cuotaNueva, idPrestamoActual, idPrestamoOriginal, null);
    }

    @Override
    public EvaluacionOtorgamiento evaluar(Long idEntidad, Double monto, Double cuotaNueva, Long idPrestamoActual,
            Long idPrestamoOriginal, String codigoPetroNuevo) throws Throwable {
        System.out.println("EvaluacionOtorgamientoService.evaluar - entidad: " + idEntidad + " - monto: " + monto
            + " - cuotaNueva: " + cuotaNueva + " - actual: " + idPrestamoActual + " - original: " + idPrestamoOriginal
            + " - producto nuevo: " + codigoPetroNuevo);
        if (idEntidad == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idEntidad es obligatorio");
        }
        EvaluacionOtorgamiento ev = new EvaluacionOtorgamiento();
        ev.setIdEntidad(idEntidad);
        ev.setIdPrestamo(idPrestamoActual);
        ev.setIdPrestamoOriginal(idPrestamoOriginal);
        ev.setMonto(monto);
        ev.setCuotaNueva(cuotaNueva != null ? redondear(cuotaNueva) : null);

        // Remuneración: la de la última PRTC de la entidad (misma que usa la solicitud).
        Double remuneracion = remuneracionDe(idEntidad);
        ev.setRemuneracion(remuneracion);
        if (remuneracion != null && remuneracion > 0.0) {
            ev.setCapacidadPago(redondear(remuneracion * ParametroOtorgamiento.PORCENTAJE_CAPACIDAD_PAGO));
        }

        // Cuotas y capital pendiente de los vigentes (2, 8, 10, 11), SIN el préstamo actual y SIN el original que se
        // nova (se cancela con el nuevo: su saldo ya está dentro del monto).
        double cuotas = 0.0;
        double capital = 0.0;
        double capitalCupo = 0.0; // solo quirografarios y emergentes: las garantías de cuenta individual son solo de ellos
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(idEntidad);
        if (prestamos != null) {
            for (Prestamo vigente : prestamos) {
                if (!esVigente(vigente) || vigente.getCodigo() == null
                        || vigente.getCodigo().equals(idPrestamoActual)
                        || vigente.getCodigo().equals(idPrestamoOriginal)) {
                    continue;
                }
                List<DetallePrestamo> pendientes =
                    detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(vigente.getCodigo());
                cuotas += cuotaVigente(vigente.getCodigo(), pendientes);
                double capitalPendiente = pagoPrestamoService.calcularSaldoCapitalPendiente(pendientes);
                capital += capitalPendiente;
                if (esQuirografarioOEmergente(vigente.getProducto() != null ? vigente.getProducto().getCodigoPetro() : null)) {
                    capitalCupo += capitalPendiente;
                }
            }
        }
        ev.setCuotasVigentes(redondear(cuotas));
        ev.setSaldoCapitalVigentes(redondear(capital));
        ev.setSaldoCapitalCupo(redondear(capitalCupo));

        double saldoCuenta = 0.0;
        List<SaldoTipoAporte> saldos = saldoAporteService.saldosPorEntidad(idEntidad);
        if (saldos != null) {
            for (SaldoTipoAporte saldo : saldos) {
                saldoCuenta += saldo.getSaldo() != null ? saldo.getSaldo() : 0.0;
            }
        }
        ev.setSaldoCuentaIndividual(redondear(saldoCuenta));

        // Capacidad: solo se evalúa con cuota y con remuneración.
        if (ev.getCuotaNueva() != null && ev.getCapacidadPago() != null) {
            boolean ok = ev.getCuotaNueva() + ev.getCuotasVigentes()
                <= ev.getCapacidadPago() + ParametroOtorgamiento.TOLERANCIA;
            ev.setCapacidadOk(ok);
            if (!ok) {
                ev.getMensajes().add("La cuota " + f(ev.getCuotaNueva()) + " más las cuotas vigentes "
                    + f(ev.getCuotasVigentes()) + " superan la capacidad de pago " + f(ev.getCapacidadPago())
                    + " (50 % de " + f(remuneracion) + ")");
            }
        }

        // Cupo: solo se evalúa con monto y solo si el préstamo nuevo es PQ/PE (producto desconocido, null = se evalúa).
        // Hipotecario/prendario nuevo: cupoOk = null (no aplica), no bloquea.
        // null = producto desconocido (se evalúa); vacío = producto sin código Petro (p. ej. CENAPRO): no es PQ/PE.
        boolean cupoAplica = codigoPetroNuevo == null || esQuirografarioOEmergente(codigoPetroNuevo);
        if (monto != null && monto > 0.0 && cupoAplica) {
            boolean ok = ev.getSaldoCapitalCupo() + monto
                <= ev.getSaldoCuentaIndividual() * ParametroOtorgamiento.PORCENTAJE_CUPO_CUENTA_INDIVIDUAL
                    + ParametroOtorgamiento.TOLERANCIA;
            ev.setCupoOk(ok);
            if (!ok) {
                ev.getMensajes().add("El saldo de capital de los créditos quirografarios y emergentes vigentes "
                    + f(ev.getSaldoCapitalCupo())
                    + " más el monto solicitado " + f(monto) + " superan el saldo de la cuenta individual "
                    + f(ev.getSaldoCuentaIndividual()) + " (100 %)");
            }
        }
        return ev;
    }

    @Override
    public EvaluacionOtorgamiento evaluarPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("EvaluacionOtorgamientoService.evaluarPrestamo - préstamo: " + idPrestamo);
        Prestamo prestamo = cargar(idPrestamo);
        Long idEntidad = prestamo.getEntidad() != null ? prestamo.getEntidad().getCodigo() : null;
        EvaluacionOtorgamiento ev = evaluar(idEntidad, prestamo.getMontoSolicitado(), cuotaUnoDe(idPrestamo),
            idPrestamo, prestamo.getIdPrestamoOriginal(), codigoPetroDe(prestamo));
        if (!aplicaReglas(prestamo, prestamo)) {
            ev.setAplicaReglas(Boolean.FALSE);
            ev.setMotivoNoAplica(sustitucionPrestamoService.esReestructuracion(prestamo)
                ? "Es una reestructuración: no pasa por las reglas del otorgamiento."
                : "El préstamo ya no está pendiente de aprobación ni generado.");
        }
        return ev;
    }

    @Override
    public boolean aplicaReglas(Prestamo prestamo, Prestamo existente) throws Throwable {
        if (existente != null) {
            Long estado = existente.getIdEstado();
            boolean noAprobado = estado != null && (estado == EstadoPrestamo.GENERADO
                || estado == EstadoPrestamo.PENDIENTE_DE_APROBACION);
            if (!noAprobado) {
                return false;
            }
        }
        return !sustitucionPrestamoService.esReestructuracion(prestamo);
    }

    @Override
    public void validarAlGuardar(Prestamo prestamo, Prestamo existente) throws Throwable {
        System.out.println("EvaluacionOtorgamientoService.validarAlGuardar - préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null));
        if (prestamo == null || !aplicaReglas(prestamo, existente)) {
            return;
        }
        // B5: la tasa de los créditos es 9 %. Nula o 0 → 9; otra → error.
        Double tasa = prestamo.getTasa();
        if (tasa == null || tasa <= 0.0) {
            prestamo.setTasa(ParametroOtorgamiento.TASA_OTORGAMIENTO);
        } else if (Math.abs(tasa - ParametroOtorgamiento.TASA_OTORGAMIENTO) > 0.0001) {
            throw new IncomeException(MSG_TASA);
        }
        // B2: el cupo bloquea al guardar (todavía no hay cuota: la capacidad no se evalúa).
        Long idEntidad = prestamo.getEntidad() != null ? prestamo.getEntidad().getCodigo()
            : (existente != null && existente.getEntidad() != null ? existente.getEntidad().getCodigo() : null);
        Double monto = prestamo.getMontoSolicitado();
        if (idEntidad == null || monto == null || monto <= 0.0) {
            return;
        }
        EvaluacionOtorgamiento ev = evaluar(idEntidad, monto, null, prestamo.getCodigo(),
            prestamo.getIdPrestamoOriginal(), codigoPetroDe(prestamo));
        if (Boolean.FALSE.equals(ev.getCupoOk())) {
            throw new IncomeException(String.join(" ", ev.getMensajes()));
        }
    }

    @Override
    public void validarAlAprobar(Prestamo prestamo) throws Throwable {
        System.out.println("EvaluacionOtorgamientoService.validarAlAprobar - préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null));
        if (prestamo == null || !aplicaReglas(prestamo, prestamo)) {
            return;
        }
        Long idEntidad = prestamo.getEntidad() != null ? prestamo.getEntidad().getCodigo() : null;
        EvaluacionOtorgamiento ev = evaluar(idEntidad, prestamo.getMontoSolicitado(), cuotaUnoDe(prestamo.getCodigo()),
            prestamo.getCodigo(), prestamo.getIdPrestamoOriginal(), codigoPetroDe(prestamo));
        if (ev.getRemuneracion() == null || ev.getRemuneracion() <= 0.0) {
            throw new IncomeException(MSG_SIN_REMUNERACION);
        }
        if (!ev.getMensajes().isEmpty()) {
            throw new IncomeException(String.join(" ", ev.getMensajes()));
        }
    }

    @Override
    public double cuotaVigenteDe(Long idPrestamo) throws Throwable {
        return cuotaVigente(idPrestamo, detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo));
    }

    // ------------------------------------------------------------------ helpers

    private Prestamo cargar(Long idPrestamo) throws Throwable {
        if (idPrestamo == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idPrestamo es obligatorio");
        }
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        return prestamo;
    }

    private static boolean esQuirografarioOEmergente(String codigoPetro) {
        String c = codigoPetro != null ? codigoPetro.trim().toUpperCase() : "";
        return ParametroOtorgamiento.CODIGO_PETRO_QUIROGRAFARIO.equals(c)
            || ParametroOtorgamiento.CODIGO_PETRO_EMERGENTE.equals(c);
    }

    /** Código Petro del producto del préstamo; vacío (no {@code null}) si tiene producto sin código (p. ej. CENAPRO): no es PQ/PE. */
    private String codigoPetroDe(Prestamo prestamo) {
        if (prestamo == null || prestamo.getProducto() == null) {
            return null; // producto desconocido
        }
        String c = prestamo.getProducto().getCodigoPetro();
        return c != null ? c : "";
    }

    private boolean esVigente(Prestamo prestamo) {
        Long estado = prestamo.getIdEstado();
        return estado != null && (estado == EstadoPrestamo.VIGENTE || estado == EstadoPrestamo.DE_PLAZO_VENCIDO
            || estado == EstadoPrestamo.VIGENTE_POR_REVISAR || estado == EstadoPrestamo.EN_MORA);
    }

    /** La remuneración unificada de la última PRTC (mayor código) de la entidad; {@code null} si no hay. */
    private Double remuneracionDe(Long idEntidad) throws Throwable {
        List<Participe> participes = participeDaoService.selectByEntidad(idEntidad);
        Participe ultimo = null;
        if (participes != null) {
            for (Participe participe : participes) {
                if (ultimo == null || (participe.getCodigo() != null && ultimo.getCodigo() != null
                        && participe.getCodigo() > ultimo.getCodigo())) {
                    ultimo = participe;
                }
            }
        }
        return ultimo != null ? ultimo.getRemuneracionUnificada() : null;
    }

    /** DTPRTTLL de la cuota 1 de la tabla; {@code null} si todavía no hay tabla. */
    private Double cuotaUnoDe(Long idPrestamo) throws Throwable {
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas != null) {
            for (DetallePrestamo cuota : cuotas) {
                if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() == 1.0) {
                    return totalDe(cuota);
                }
            }
        }
        return null;
    }

    /** Cuota total de la primera cuota no pagada (≥ 1); si no quedan, la de la última cuota; 0 si no hay tabla. */
    private double cuotaVigente(Long idPrestamo, List<DetallePrestamo> pendientes) throws Throwable {
        DetallePrestamo primera = null;
        if (pendientes != null) {
            for (DetallePrestamo cuota : pendientes) {
                if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() >= 1.0
                        && (primera == null || cuota.getNumeroCuota() < primera.getNumeroCuota())) {
                    primera = cuota;
                }
            }
        }
        if (primera == null) {
            List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
            if (todas != null) {
                for (DetallePrestamo cuota : todas) {
                    if (cuota.getNumeroCuota() != null && cuota.getNumeroCuota() >= 1.0
                            && (primera == null || cuota.getNumeroCuota() > primera.getNumeroCuota())) {
                        primera = cuota;
                    }
                }
            }
        }
        return primera != null ? totalDe(primera) : 0.0;
    }

    private double totalDe(DetallePrestamo cuota) {
        if (cuota.getTotal() != null) {
            return cuota.getTotal();
        }
        return nvl(cuota.getCapital()) + nvl(cuota.getInteres()) + nvl(cuota.getDesgravamen())
            + nvl(cuota.getValorSeguroIncendio());
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /** 1.234,56 — coma decimal, punto de miles (formato de la casa, sin depender del locale del servidor). */
    private String f(Double valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0.00", simbolos).format(valor != null ? valor : 0.0);
    }
}
