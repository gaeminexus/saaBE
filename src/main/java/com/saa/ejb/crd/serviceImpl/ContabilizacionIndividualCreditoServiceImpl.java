package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see ContabilizacionIndividualCreditoService
 * @author Sistema SAA
 * @since 2026-08-30
 */
@Stateless
public class ContabilizacionIndividualCreditoServiceImpl implements ContabilizacionIndividualCreditoService {

    /** CRD.TPAP.TPAPCDGO de los únicos tres tipos con cuenta en la plantilla 21 (aux1 50/51/52).
     * Mismos valores que {@code CobroCreditoServiceImpl}/{@code CobroPetroContableServiceImpl}
     * — catálogo de negocio estable, no una resolución que pueda divergir. */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_APORTE_ADICIONAL = 2L;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @Override
    public Long resolverPlantillaAplicacion(Long idEmpresa) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.APLICACION_PETRO, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                    + PlantillasCredito.APLICACION_PETRO + " para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    @Override
    public DetalleAsiento lineaInteres(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor,
            boolean esMora, String prefijoDescripcion) throws Throwable {
        if (idTipoPrestamo == null) {
            throw new IncomeException(prefijoDescripcion + ": el préstamo no tiene tipo de préstamo"
                    + " asignado; no se puede resolver la cuenta de interés.");
        }
        int aux1 = esMora ? CrdLineaAsiento.INTERES_MORA_POR_COBRAR : CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR;
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                idPlantillaAplicacion, aux1, idTipoPrestamo.intValue());
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de interés " + (esMora ? "de mora" : "ordinario")
                    + " (aux1=" + aux1 + ") para el tipo de préstamo " + idTipoPrestamo + ".");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        // D3: mora y ordinario COMPARTEN cuenta — la descripción es lo único que distingue.
        detalle.setDescripcion(prefijoDescripcion + " - interés " + (esMora ? "de mora" : "ordinario"));
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(valor));
        return detalle;
    }

    @Override
    public DetalleAsiento lineaSeguroIncendio(Long idPlantillaAplicacion, Long idTipoPrestamo, double valor,
            String prefijoDescripcion) throws Throwable {
        int aux1;
        if (idTipoPrestamo != null && idTipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_HIPOTECARIO) {
            aux1 = CrdLineaAsiento.SEGURO_INCENDIO_HIPOTECARIO;
        } else if (idTipoPrestamo != null && idTipoPrestamo == CrdLineaAsiento.TIPO_PRESTAMO_PRENDARIO) {
            aux1 = CrdLineaAsiento.SEGURO_INCENDIO_PRENDARIO;
        } else {
            throw new IncomeException(prefijoDescripcion + ": hay seguro de incendio pero el tipo de"
                    + " préstamo " + idTipoPrestamo + " no tiene cuenta de seguro de incendio definida"
                    + " (solo hipotecario y prendario).");
        }
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de seguro de incendio (aux1=" + aux1 + ").");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - seguro de incendio");
    }

    @Override
    public DetalleAsiento lineaSeguroDesgravamen(Long idPlantillaAplicacion, double valor, String prefijoDescripcion)
            throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                CrdLineaAsiento.SEGURO_DESGRAVAMEN);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea de seguro de desgravamen (aux1="
                    + CrdLineaAsiento.SEGURO_DESGRAVAMEN + ").");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - seguro de desgravamen");
    }

    @Override
    public DetalleAsiento lineaAporteRegistrado(Long idPlantillaAplicacion, Long idTipoAporte, double valor,
            String prefijoDescripcion) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                aux1ParaTipoAporte(idTipoAporte));
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                    + " no tiene la línea del tipo de aporte " + idTipoAporte + ".");
        }
        return lineaHaberDesdePlantilla(linea, valor, prefijoDescripcion + " - aporte registrado");
    }

    @Override
    public List<DetalleAsiento> lineasCruceAportesConsumidos(Long idPlantillaAplicacion,
            List<DesgloseAporte> aportes, String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (aportes == null) {
            return lineas;
        }
        for (DesgloseAporte renglon : aportes) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantillaAplicacion,
                    aux1ParaTipoAporte(renglon.getIdTipoAporte()));
            if (linea == null || linea.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla alterno " + PlantillasCredito.APLICACION_PETRO
                        + " no tiene la línea del tipo de aporte " + renglon.getIdTipoAporte()
                        + " para el cruce de valores.");
            }
            PlanCuenta cuenta = linea.getPlanCuenta();
            TipoAporte tipoAporte = tipoAporteDaoService.find(new TipoAporte(), renglon.getIdTipoAporte());
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(cuenta);
            detalle.setNumeroCuenta(cuenta.getCuentaContable());
            detalle.setNombreCuenta(cuenta.getNombre());
            // ⚠️ Al DEBE a propósito, aunque DetallePlantilla.movimiento diga HABER para esta
            // línea: esa plantilla la define para el aporte ENTRANDO (saldo sube). Acá el
            // sentido es el opuesto — se CONSUME saldo que el socio ya tenía (saldo baja) — y
            // forzarlo es la única forma de que la cuenta correcta quede del lado correcto.
            detalle.setDescripcion(prefijoDescripcion + " - consumo aporte "
                    + (tipoAporte != null ? tipoAporte.getNombre() : renglon.getIdTipoAporte()));
            detalle.setValorDebe(redondear(renglon.getValor()));
            detalle.setValorHaber(0.0);
            lineas.add(detalle);
        }
        return lineas;
    }

    @Override
    public DetalleAsiento lineaBandaCapital(Long idProducto, Long idEmpresa, double capital,
            LocalDate fechaVencimiento, LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {
        long[] tipoCarteraYDias = tipoCarteraYDias(fechaVencimiento, fechaCorte);
        ResultadoClasificacionBanda resultado = clasificadorBandaService.clasificar(idProducto, idEmpresa,
                tipoCarteraYDias[0], tipoCarteraYDias[1], fechaCorte);
        BandaProductoDetalle banda = resultado.getBanda();
        if (banda.getIdPlanCuenta() == null) {
            throw new IncomeException(prefijoDescripcion + ": la banda " + banda.getNumero()
                    + " del producto " + idProducto + " no tiene cuenta contable asignada en CRD.BNDP.");
        }
        PlanCuenta cuenta = planCuentaDaoService.selectById(banda.getIdPlanCuenta(),
                NombreEntidadesContabilidad.PLAN_CUENTA);
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(banda.getCuentaContable());
        detalle.setNombreCuenta(banda.getNombreCuenta());
        detalle.setDescripcion(prefijoDescripcion + " - banda " + banda.getNumero());
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(capital));
        return detalle;
    }

    @Override
    public List<DetalleAsiento> haberDesdePagos(List<PagoPrestamo> pagos, Long idEmpresa,
            Long idPlantillaAplicacion, LocalDate fechaCorte, String prefijoDescripcion) throws Throwable {
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (pagos == null) {
            return lineas;
        }

        double totalInteres = 0.0;
        double totalMora = 0.0;
        double totalSeguroDesgravamen = 0.0;
        double totalSeguroIncendio = 0.0;
        Long idTipoPrestamo = null;

        for (PagoPrestamo pago : pagos) {
            if (pago.getAnulado() != null && pago.getAnulado() == 1L) {
                continue;
            }
            DetallePrestamo cuota = pago.getDetallePrestamo();
            Prestamo prestamo = pago.getPrestamo();
            Producto producto = prestamo != null ? prestamo.getProducto() : null;
            if (idTipoPrestamo == null && producto != null && producto.getTipoPrestamo() != null) {
                idTipoPrestamo = producto.getTipoPrestamo().getCodigo();
            }

            // ⚠️ TRAMPAS 2.1/2.2 de la especificación CBCRASN2: el abono a capital y el capital
            // futuro de una precancelación graban en saldoOtros con capitalPagado = 0 — leer
            // solo capitalPagado los contabilizaría en $0, sin ningún error.
            double capital = nvl(pago.getSaldoOtros()) > 0.0 ? nvl(pago.getSaldoOtros()) : nvl(pago.getCapitalPagado());
            if (capital > 0.0) {
                if (producto == null || cuota == null || cuota.getFechaVencimiento() == null) {
                    throw new IncomeException(prefijoDescripcion + ": el pago " + pago.getCodigo()
                            + " tiene capital pero no tiene producto o cuota con fecha de"
                            + " vencimiento; no se puede clasificar por banda.");
                }
                lineas.add(lineaBandaCapital(producto.getCodigo(), idEmpresa, capital,
                        cuota.getFechaVencimiento().toLocalDate(), fechaCorte, prefijoDescripcion));
            }

            totalInteres += nvl(pago.getInteresPagado()) + nvl(pago.getInteresVencidoPagado());
            totalMora += nvl(pago.getMoraPagada());
            totalSeguroDesgravamen += nvl(pago.getDesgravamen());
            totalSeguroIncendio += nvl(pago.getValorSeguroIncendio());
        }

        totalInteres = redondear(totalInteres);
        totalMora = redondear(totalMora);
        totalSeguroDesgravamen = redondear(totalSeguroDesgravamen);
        totalSeguroIncendio = redondear(totalSeguroIncendio);

        if (totalInteres > 0.0) {
            lineas.add(lineaInteres(idPlantillaAplicacion, idTipoPrestamo, totalInteres, false, prefijoDescripcion));
        }
        if (totalMora > 0.0) {
            lineas.add(lineaInteres(idPlantillaAplicacion, idTipoPrestamo, totalMora, true, prefijoDescripcion));
        }
        if (totalSeguroDesgravamen > 0.0) {
            lineas.add(lineaSeguroDesgravamen(idPlantillaAplicacion, totalSeguroDesgravamen, prefijoDescripcion));
        }
        if (totalSeguroIncendio > 0.0) {
            lineas.add(lineaSeguroIncendio(idPlantillaAplicacion, idTipoPrestamo, totalSeguroIncendio,
                    prefijoDescripcion));
        }
        return lineas;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    @Override
    public long[] tipoCarteraYDias(LocalDate fechaVencimiento, LocalDate fechaCorte) {
        if (!fechaVencimiento.isAfter(fechaCorte)) {
            long dias = Math.max(1, ChronoUnit.DAYS.between(fechaVencimiento, fechaCorte) + 1);
            return new long[]{TipoCarteraBanda.VENCIDO, dias};
        }
        long dias = Math.max(1, ChronoUnit.DAYS.between(fechaCorte, fechaVencimiento));
        return new long[]{TipoCarteraBanda.POR_VENCER, dias};
    }

    /** Un tipo de aporte sin cuenta en la plantilla 21 NUNCA debería llegar acá: el registro ya
     * lo rechaza (ver {@code CobroCreditoServiceImpl#esTipoAporteContabilizable}). Si llega,
     * es un tipo nuevo sin parametrizar — falla fuerte, no se adivina la cuenta. */
    private int aux1ParaTipoAporte(Long idTipoAporte) throws Throwable {
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_CESANTIA) {
            return CrdLineaAsiento.APORTES_CESANTIA;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_JUBILACION) {
            return CrdLineaAsiento.APORTES_JUBILACION;
        }
        if (idTipoAporte != null && idTipoAporte == TIPO_APORTE_ADICIONAL) {
            return CrdLineaAsiento.APORTE_ADICIONAL_PERSONAL;
        }
        throw new IncomeException("El tipo de aporte " + idTipoAporte + " no tiene cuenta contable"
                + " parametrizada (hoy solo 9 jubilación, 11 cesantía, 2 adicional); no se puede"
                + " contabilizar.");
    }

    private DetalleAsiento lineaHaberDesdePlantilla(DetallePlantilla plantilla, double valor, String descripcion) {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(0.0);
        detalle.setValorHaber(redondear(valor));
        return detalle;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
