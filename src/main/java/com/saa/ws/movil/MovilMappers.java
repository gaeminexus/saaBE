package com.saa.ws.movil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.service.dto.SaldoPrestamoResumen;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoPrestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.ws.movil.dto.CuotaPrestamoMovilDTO;
import com.saa.ws.movil.dto.PrestamoMovilDTO;

/**
 * Mapeos entidad → DTO compartidos entre recursos de {@code /movil} (evita que
 * {@link PrestamoMovilRest} y {@link CuentaIndividualMovilRest} recorten el mismo
 * {@code Prestamo} cada uno a su manera y terminen divergiendo).
 */
final class MovilMappers {

    private MovilMappers() {
    }

    /**
     * {@code saldo} viene de {@code PrestamoService.calcularSaldosEnLote(...)}, calculado por el
     * llamador (esta clase no puede inyectar EJB). {@code null} si el lote no trajo resultado para
     * este préstamo.
     *
     * <p>{@code saldoCapital}/{@code saldoTotal} NUNCA salen de {@code Prestamo.getSaldoCapital()}/
     * {@code getSaldoTotal()} ({@code PRST.PRSTSLCP}/{@code PRSTSLTT}): son columnas muertas que
     * ninguna línea del backend escribe (CONTRATO-INTRANET-MOVIL.md §8). {@code saldoInteres},
     * {@code saldoPorVencer} y {@code saldoVencido} tampoco los calcula el motor — van en
     * {@code null}, nunca con el valor de esas columnas: un {@code null} es honesto, un número
     * inventado es exactamente el defecto que esto corrige. La app no los usa.</p>
     */
    static PrestamoMovilDTO aDTO(Prestamo prestamo, SaldoPrestamoResumen saldo, List<DetallePrestamo> cuotas) {
        PrestamoMovilDTO dto = new PrestamoMovilDTO();
        dto.setCodigo(prestamo.getCodigo());
        dto.setIdAsoprep(prestamo.getIdAsoprep());
        if (prestamo.getProducto() != null) {
            dto.setIdProducto(prestamo.getProducto().getCodigo());
            dto.setNombreProducto(prestamo.getProducto().getNombre());
        }
        dto.setFecha(prestamo.getFecha());
        dto.setFechaInicio(prestamo.getFechaInicio());
        dto.setFechaFin(prestamo.getFechaFin());
        dto.setPlazo(prestamo.getPlazo());
        dto.setMontoSolicitado(prestamo.getMontoSolicitado());
        dto.setValorCuota(prestamo.getValorCuota());
        dto.setTasa(prestamo.getTasa());
        dto.setTotalPagado(prestamo.getTotalPagado());
        dto.setSaldoCapital(saldo != null ? saldo.getSaldoCapital() : null);
        dto.setSaldoTotal(saldo != null ? saldo.getSaldoTotal() : null);
        dto.setSaldoInteres(null);
        dto.setSaldoPorVencer(null);
        dto.setSaldoVencido(null);
        dto.setMoraCalculada(prestamo.getMoraCalculada());
        dto.setDiasVencido(prestamo.getDiasVencido());
        // idEstado - PRSTIDST, el estado vigente. NUNCA estadoPrestamo (ESPSCDGO) - ver CLAUDE.md.
        dto.setIdEstado(prestamo.getIdEstado());
        completarConTipoYCuotas(dto, prestamo, cuotas);
        return dto;
    }

    /**
     * Un préstamo sin {@code idAsoprep} ({@code PRSTIDAS} nulo) no existe para {@code /movil}
     * (decisión del usuario, 2026-10-22). Un préstamo ANULADO ({@code PRSTIDST = EstadoPrestamo.ANULADO})
     * tampoco (decisión del usuario, 2026-10-23). Se aplica ANTES de calcular saldos y de cargar cuotas.
     */
    static boolean visibleEnMovil(Prestamo prestamo) {
        if (prestamo == null || prestamo.getIdAsoprep() == null) {
            return false;
        }
        Long idEstado = prestamo.getIdEstado();
        return idEstado == null || idEstado.intValue() != EstadoPrestamo.ANULADO;
    }

    static List<Prestamo> soloVisiblesEnMovil(List<Prestamo> prestamos) {
        List<Prestamo> visibles = new ArrayList<>();
        for (Prestamo prestamo : prestamos) {
            if (visibleEnMovil(prestamo)) {
                visibles.add(prestamo);
            }
        }
        return visibles;
    }

    /** Tipo de producto con que se identifica a los créditos que cubre la garantía de los aportes. */
    private static final long TIPO_PRESTAMO_QUIROGRAFARIO = 1L;

    /**
     * Carga las cuotas de un préstamo UNA vez (de ahí salen los seis campos derivados). Si la carga
     * falla devuelve {@code null}: ese préstamo sale con los campos nuevos en {@code null} y la
     * respuesta sigue siendo 200 — un préstamo no puede tumbar la lista entera. La absorción es solo
     * de esta carga, no del método que la llama.
     */
    static List<DetallePrestamo> cargarCuotas(DetallePrestamoDaoService dao, Long idPrestamo) {
        try {
            return dao.selectByPrestamo(idPrestamo);
        } catch (Throwable e) {
            System.err.println("MovilMappers.cargarCuotas - prestamo " + idPrestamo + ": "
                    + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Campos de la observación 1/9/20 del cliente (2026-10-21). Reglas en CONTRATO-INTRANET-MOVIL.md
     * §5.4. {@code cuotas == null} (no se pudieron cargar) deja en {@code null} los seis campos que
     * dependen de ellas; el tipo de préstamo sale del producto y no depende de las cuotas.
     */
    private static void completarConTipoYCuotas(PrestamoMovilDTO dto, Prestamo prestamo,
            List<DetallePrestamo> cuotas) {
        TipoPrestamo tipo = prestamo.getProducto() != null ? prestamo.getProducto().getTipoPrestamo() : null;
        Long idTipo = tipo != null ? tipo.getCodigo() : null;
        dto.setIdTipoPrestamo(idTipo);
        dto.setNombreTipoPrestamo(tipo != null ? tipo.getNombre() : null);
        dto.setCubiertoPorAportes(idTipo != null && idTipo.longValue() == TIPO_PRESTAMO_QUIROGRAFARIO);

        if (cuotas == null) {
            return;
        }

        LocalDateTime primera = null;
        LocalDateTime ultima = null;
        DetallePrestamo ultimaPagada = null;       // mayor numeroCuota con estado 4 (PAGADA)
        DetallePrestamo ultimaCubierta = null;     // mayor numeroCuota con estado 4 o 7
        for (DetallePrestamo cuota : cuotas) {
            Long estado = cuota.getEstado();
            if (estado != null && estado.intValue() == EstadoCuotaPrestamo.ANULADA) {
                continue;
            }
            LocalDateTime vence = cuota.getFechaVencimiento();
            if (vence != null) {
                if (primera == null || vence.isBefore(primera)) {
                    primera = vence;
                }
                if (ultima == null || vence.isAfter(ultima)) {
                    ultima = vence;
                }
            }
            if (estado != null && cuota.getNumeroCuota() != null) {
                int e = estado.intValue();
                if (e == EstadoCuotaPrestamo.PAGADA && mayor(cuota, ultimaPagada)) {
                    ultimaPagada = cuota;
                }
                if ((e == EstadoCuotaPrestamo.PAGADA || e == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA)
                        && mayor(cuota, ultimaCubierta)) {
                    ultimaCubierta = cuota;
                }
            }
        }
        dto.setFechaPrimeraCuota(primera);
        dto.setFechaUltimaCuota(ultima);

        // saldoCapitalPendiente: misma regla que participe-dash del SAA (obtenerSaldoDesdeUltimaPagada),
        // más el caso de los cancelados. Nunca se inventa el número.
        Double pendiente;
        if (estaCancelado(prestamo.getIdEstado())) {
            pendiente = 0.0;
        } else if (ultimaPagada != null) {
            pendiente = ultimaPagada.getSaldoCapital();
        } else {
            pendiente = prestamo.getMontoSolicitado();
        }
        dto.setSaldoCapitalPendiente(pendiente);
        dto.setCapitalPagado(pendiente != null && prestamo.getMontoSolicitado() != null
                ? redondear(prestamo.getMontoSolicitado() - pendiente) : null);

        if (ultimaCubierta != null) {
            Double total = ultimaCubierta.getTotal();
            if (total == null) {
                total = nvl(ultimaCubierta.getCuota()) + nvl(ultimaCubierta.getDesgravamen())
                        + nvl(ultimaCubierta.getValorSeguroIncendio());
            }
            dto.setValorUltimaCuotaPagada(total);
        }
    }

    private static boolean mayor(DetallePrestamo cuota, DetallePrestamo actual) {
        return actual == null || cuota.getNumeroCuota() > actual.getNumeroCuota();
    }

    private static boolean estaCancelado(Long idEstado) {
        if (idEstado == null) {
            return false;
        }
        int e = idEstado.intValue();
        return e == EstadoPrestamo.CANCELADO || e == EstadoPrestamo.CANCELADO_ANTICIPADO
                || e == EstadoPrestamo.CANCELADO_POR_NOVACION
                || e == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION;
    }

    private static double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    static CuotaPrestamoMovilDTO aDTO(DetallePrestamo cuota) {
        CuotaPrestamoMovilDTO dto = new CuotaPrestamoMovilDTO();
        dto.setCodigo(cuota.getCodigo());
        dto.setNumeroCuota(cuota.getNumeroCuota());
        dto.setFechaVencimiento(cuota.getFechaVencimiento());
        dto.setCapital(cuota.getCapital());
        dto.setInteres(cuota.getInteres());
        dto.setMora(cuota.getMora());
        dto.setCuota(cuota.getCuota());
        dto.setDesgravamen(cuota.getDesgravamen());
        dto.setSeguroIncendio(cuota.getValorSeguroIncendio());
        dto.setTotal(cuota.getTotal());
        dto.setSaldoCapital(cuota.getSaldoCapital());
        dto.setSaldo(cuota.getSaldo());
        dto.setEstado(cuota.getEstado());
        dto.setFechaPagado(cuota.getFechaPagado());
        dto.setCapitalPagado(cuota.getCapitalPagado());
        dto.setInteresPagado(cuota.getInteresPagado());
        dto.setDiasMora(cuota.getDiasMora());
        dto.setPagoExtra(cuota.getSaldoOtros());
        return dto;
    }
}
