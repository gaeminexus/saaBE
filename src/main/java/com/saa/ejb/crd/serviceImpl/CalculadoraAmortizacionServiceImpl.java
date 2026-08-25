package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.CalculadoraAmortizacionService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link CalculadoraAmortizacionService}.
 *
 * Corrige los defectos D1, D2, D3, D4, D6, D7, D8 y D9 encontrados en la auditoría de
 * PLAN-SIMULADORES-PRESTAMOS.md §5.1, respecto del motor que existía duplicado (con matices
 * distintos) en {@code PrestamoServiceImpl.generarAmortizacionFrancesa/Alemana} y en
 * {@code AbonoCapitalPrestamoServiceImpl.construirTablaProyectada}. La corrección de
 * redondeo (D2/D3/D6/D7/D9) está portada de ese segundo método (líneas 528-559 al momento de
 * la auditoría), que ya estaba escrita y probada.
 *
 * D5 (semántica de {@code DTPRSLDO}) y D10 (sincronía tasa/interesNominal) no aplican acá: son
 * de mapeo a la entidad y de {@code PrestamoServiceImpl.saveSingle} respectivamente.
 *
 * @author Sistema SAA
 * @since 2026-08-25
 */
@Stateless
public class CalculadoraAmortizacionServiceImpl implements CalculadoraAmortizacionService {

    /**
     * Tope de seguridad sobre el plazo. Portado de
     * {@code AbonoCapitalPrestamoServiceImpl.MAX_CUOTAS_NUEVAS}. Ahí el plazo es un valor
     * DERIVADO y se acota en silencio; acá el plazo es un dato que el usuario pide para un
     * préstamo real, así que superarlo se RECHAZA en vez de truncarse sin avisar.
     */
    private static final int MAX_CUOTAS = 600;

    @EJB
    private FechaService fechaService;

    @Override
    public List<CuotaProyectada> calcular(ParametrosAmortizacion params) throws Throwable {
        validar(params);

        double capital = params.getMonto();
        double tasaMensual = params.getTasaAnual() / 100.0 / 12.0;
        double tasaDiaria = params.getTasaAnual() / 100.0 / 360.0;
        int plazo = params.getPlazo();
        long tipoAmortizacion = params.getTipoAmortizacion();
        boolean tieneCuotaCero = Boolean.TRUE.equals(params.getTieneCuotaCero());
        int mesesGracia = tieneCuotaCero ? 1 : 0;

        LocalDateTime fechaInicio = params.getFechaInicio();
        LocalDate fechaInicioLocal = fechaInicio.toLocalDate();

        double desgravamenPorCuota = nvl(params.getDesgravamenPorCuota());
        double seguroIncendioPorCuota = nvl(params.getSeguroIncendioPorCuota());

        // D8: días reales del mes inicial, SIN el +1 que contaba ambos extremos.
        LocalDate ultimoDiaMesInicio = fechaService.ultimoDiaMesAnioLocal(
            Long.valueOf(fechaInicioLocal.getMonthValue()), Long.valueOf(fechaInicioLocal.getYear()));
        int diasMesInicial = (int) ChronoUnit.DAYS.between(fechaInicioLocal, ultimoDiaMesInicio);
        double interesesMesInicial = capital * tasaDiaria * diasMesInicial;

        List<CuotaProyectada> tabla = new ArrayList<>();
        double saldoCapital = capital;

        if (tieneCuotaCero) {
            saldoCapital = agregarCuotaCero(tabla, capital, tasaDiaria, fechaInicio, fechaInicioLocal,
                desgravamenPorCuota, seguroIncendioPorCuota);
        }

        if (tipoAmortizacion == 2L) {
            calcularAlemana(tabla, capital, saldoCapital, tasaMensual, plazo, mesesGracia,
                interesesMesInicial, fechaInicio, desgravamenPorCuota, seguroIncendioPorCuota);
        } else {
            calcularFrancesa(tabla, capital, saldoCapital, tasaMensual, plazo, mesesGracia,
                interesesMesInicial, fechaInicio, desgravamenPorCuota, seguroIncendioPorCuota);
        }

        validarNingunaCuotaEnCero(tabla, plazo);
        return tabla;
    }

    private void validar(ParametrosAmortizacion params) throws Throwable {
        if (params == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": faltan los parámetros de amortización");
        }
        if (params.getMonto() == null || params.getMonto() <= 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el monto debe ser mayor a cero");
        }
        if (params.getTasaAnual() == null || params.getTasaAnual() <= 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": la tasa anual debe ser mayor a cero");
        }
        if (params.getPlazo() == null || params.getPlazo() <= 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el plazo debe ser mayor a cero");
        }
        if (params.getPlazo() > MAX_CUOTAS) {
            throw new IncomeException(ERR_PLAZO_EXCEDE_TOPE
                + ": el plazo (" + params.getPlazo() + ") supera el tope de " + MAX_CUOTAS + " cuotas");
        }
        if (params.getTipoAmortizacion() == null
                || (params.getTipoAmortizacion() != 1L && params.getTipoAmortizacion() != 2L)) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": tipo de amortización no válido. Use 1 (Francesa) o 2 (Alemana)");
        }
        if (params.getFechaInicio() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": falta la fecha de inicio");
        }
    }

    /**
     * Rechaza montos que hagan que alguna cuota regular redondee a $0,00 (caso borde de la
     * auditoría §5.2: un monto de 0,05 generaba 12 filas en cero).
     */
    private void validarNingunaCuotaEnCero(List<CuotaProyectada> tabla, int plazo) throws Throwable {
        for (CuotaProyectada cuota : tabla) {
            if (cuota.getNumeroCuota() == null || cuota.getNumeroCuota() <= 0) {
                continue; // la cuota 0 de gracia no amortiza capital: en cero es normal
            }
            if (cuota.getCapital() == null || cuota.getCapital() <= 0.0
                    || cuota.getCuota() == null || cuota.getCuota() <= 0.0) {
                throw new IncomeException(ERR_MONTO_INSUFICIENTE
                    + ": el monto es demasiado pequeño para amortizarse en " + plazo
                    + " cuotas; la cuota " + cuota.getNumeroCuota().intValue() + " quedaría en $0.00");
            }
        }
    }

    /**
     * D4: la cuota 0 de gracia cobra el interés proporcional a los DÍAS REALES desde
     * {@code fechaInicio} hasta su propio vencimiento (el mes inicial parcial más el mes
     * siguiente completo), no un mes comercial plano. Ejemplo del plan: 15/03 → 30/04 son 46
     * días, $153,33 y no $100,00.
     *
     * Decisión de implementación (documentada en PLAN-SIMULADORES-PRESTAMOS.md §11.3): la cuota
     * 0 SÍ cobra desgravamen y seguro de incendio, igual que las cuotas regulares. Son primas
     * ligadas a que el préstamo esté vigente y asegurado, no a que se amortice capital — durante
     * la gracia el capital sigue expuesto y el generador real (`PrestamoServiceImpl`) nunca los
     * había ejercido con un valor distinto de 0,0, así que esto no está verificado contra el
     * producto real. Si el negocio decide que la gracia no cobra seguros, es un cambio de un
     * parámetro acá, no de arquitectura.
     *
     * @return El saldo de capital después de la cuota 0 (no amortiza capital: igual al monto)
     */
    private double agregarCuotaCero(List<CuotaProyectada> tabla, double capital, double tasaDiaria,
            LocalDateTime fechaInicio, LocalDate fechaInicioLocal, double desgravamenPorCuota,
            double seguroIncendioPorCuota) throws Throwable {

        LocalDate finDelMesSiguiente = fechaInicioLocal.plusMonths(1);
        LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
            Long.valueOf(finDelMesSiguiente.getMonthValue()), Long.valueOf(finDelMesSiguiente.getYear()));
        LocalDateTime fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());

        long diasReales = ChronoUnit.DAYS.between(fechaInicioLocal, ultimoDia);
        double interes = redondear(capital * tasaDiaria * diasReales);
        double cuota = interes; // cuota 0: no amortiza capital, cuota = interés
        double desgravamen = redondear(desgravamenPorCuota);
        double seguro = redondear(seguroIncendioPorCuota);
        double total = redondear(cuota + desgravamen + seguro);

        CuotaProyectada cero = new CuotaProyectada();
        cero.setNumeroCuota(0.0);
        cero.setFechaVencimiento(fechaVencimiento);
        cero.setCapital(0.0);
        cero.setInteres(interes);
        cero.setCuota(cuota);
        cero.setSaldoCapital(redondear(capital));
        cero.setDesgravamen(desgravamen);
        cero.setSeguroIncendio(seguro);
        cero.setTotal(total);
        tabla.add(cero);

        return capital;
    }

    /**
     * Francesa: cuota fija, capital creciente. D1: la cuota reportada es capital + interés
     * (igual que la alemana), así la cuota 1 SÍ incluye el proporcional del mes inicial. D2/D3:
     * cuotaFija redondeada antes de derivar el capital, saldo redondeado en cada paso, y la
     * última cuota absorbe el saldo entero sin condición sobre la magnitud del residuo.
     */
    private void calcularFrancesa(List<CuotaProyectada> tabla, double capital, double saldoCapital,
            double tasaMensual, int plazo, int mesesGracia, double interesesMesInicial,
            LocalDateTime fechaInicio, double desgravamenPorCuota, double seguroIncendioPorCuota)
            throws Throwable {

        double cuotaFija = redondear(capital * (tasaMensual * Math.pow(1 + tasaMensual, plazo))
            / (Math.pow(1 + tasaMensual, plazo) - 1));

        for (int i = 1; i <= plazo; i++) {
            // Interés SIN proporcional: es lo que se usa para derivar el capital de la cuota.
            // No tocar esta parte (ratificado en el plan: la separación ya era correcta).
            double interesBase = saldoCapital * tasaMensual;

            double interes = interesBase;
            if (i == 1 && mesesGracia == 0) {
                // D1: sin cuota 0, el proporcional del mes inicial se cobra en la cuota 1.
                interes += interesesMesInicial;
            }
            interes = redondear(interes);

            double saldoAntes = saldoCapital;
            double capitalCuota = redondear(cuotaFija - interesBase);

            if (i == plazo) {
                // D2/D3: última cuota, absorbe el saldo entero SIN condición (el residuo de
                // redondeo no vive de forma confiable en el double).
                capitalCuota = redondear(saldoAntes);
            }
            if (capitalCuota > saldoAntes) {
                capitalCuota = redondear(saldoAntes);
            }

            saldoCapital = redondear(saldoAntes - capitalCuota);

            // D1: cuota = capital + interés, igual que ya hacía la alemana.
            double cuota = redondear(capitalCuota + interes);

            LocalDateTime fechaVencimiento = fechaVencimientoCuota(fechaInicio, i, mesesGracia);

            tabla.add(fila(i, fechaVencimiento, capitalCuota, interes, cuota, saldoCapital,
                desgravamenPorCuota, seguroIncendioPorCuota));
        }
    }

    /**
     * Alemana: capital fijo, cuota decreciente. D2/D3/D6/D7/D9: capitalFijo redondeado una sola
     * vez, saldo redondeado en cada paso, última cuota absorbe el saldo entero sin condición.
     */
    private void calcularAlemana(List<CuotaProyectada> tabla, double capital, double saldoCapital,
            double tasaMensual, int plazo, int mesesGracia, double interesesMesInicial,
            LocalDateTime fechaInicio, double desgravamenPorCuota, double seguroIncendioPorCuota)
            throws Throwable {

        double capitalFijo = redondear(capital / plazo);

        for (int i = 1; i <= plazo; i++) {
            double interes = saldoCapital * tasaMensual;
            if (i == 1 && mesesGracia == 0) {
                interes += interesesMesInicial;
            }
            interes = redondear(interes);

            double saldoAntes = saldoCapital;
            double capitalCuota = capitalFijo;

            if (i == plazo) {
                capitalCuota = redondear(saldoAntes);
            }
            if (capitalCuota > saldoAntes) {
                capitalCuota = redondear(saldoAntes);
            }

            saldoCapital = redondear(saldoAntes - capitalCuota);

            double cuota = redondear(capitalCuota + interes);

            LocalDateTime fechaVencimiento = fechaVencimientoCuota(fechaInicio, i, mesesGracia);

            tabla.add(fila(i, fechaVencimiento, capitalCuota, interes, cuota, saldoCapital,
                desgravamenPorCuota, seguroIncendioPorCuota));
        }
    }

    private LocalDateTime fechaVencimientoCuota(LocalDateTime fechaInicio, int numeroCuota, int mesesGracia)
            throws Throwable {
        int desplazamiento = numeroCuota + mesesGracia;
        LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(desplazamiento);
        LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
            Long.valueOf(fechaTemp.getMonthValue()), Long.valueOf(fechaTemp.getYear()));
        return ultimoDia.atTime(fechaInicio.toLocalTime());
    }

    private CuotaProyectada fila(int numeroCuota, LocalDateTime fechaVencimiento, double capital,
            double interes, double cuota, double saldoCapital, double desgravamenPorCuota,
            double seguroIncendioPorCuota) {
        double desgravamen = redondear(desgravamenPorCuota);
        double seguro = redondear(seguroIncendioPorCuota);

        CuotaProyectada fila = new CuotaProyectada();
        fila.setNumeroCuota(Double.valueOf(numeroCuota));
        fila.setFechaVencimiento(fechaVencimiento);
        fila.setCapital(capital);
        fila.setInteres(interes);
        fila.setCuota(cuota);
        fila.setSaldoCapital(Math.max(0.0, saldoCapital));
        fila.setDesgravamen(desgravamen);
        fila.setSeguroIncendio(seguro);
        // Mismo invariante que DTPRTTLL: total = cuota + desgravamen + seguroIncendio
        fila.setTotal(redondear(cuota + desgravamen + seguro));
        return fila;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
