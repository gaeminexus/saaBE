package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.ejb.rhh.dao.AnticipoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.service.CierreCuotasDescuentoService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.AnticipoEmpleado;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.model.rhh.DescuentoRecurrente;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.rubros.EstadoAnticipoEmpleado;
import com.saa.rubros.RhhEstadoCuotaDescuento;
import com.saa.rubros.RhhEstadoDescuentoRecurrente;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de CierreCuotasDescuentoService. Ver el javadoc de la interfaz para el
 * porque de este bean separado: corre en su propia transaccion (<code>REQUIRES_NEW</code>) para
 * que un fallo aqui adentro nunca pueda hacer rollback del pago de nomina que lo dispara.</p>
 */
@Stateless
public class CierreCuotasDescuentoServiceImpl implements CierreCuotasDescuentoService {

    /** Tabla de origen que ProcesoNominaServiceImpl escribe en RNGLTBOR para un renglon de cuota. */
    private static final String ORIGEN_CUOTA_DESCUENTO = "RHH.CTDS";

    @EJB
    private PeriodoNominaDaoService periodoNominaDaoService;

    @EJB
    private NominaDaoService nominaDaoService;

    @EJB
    private ReglonNominaDaoService reglonNominaDaoService;

    @EJB
    private CuotaDescuentoDaoService cuotaDescuentoDaoService;

    @EJB
    private DescuentoRecurrenteDaoService descuentoRecurrenteDaoService;

    @EJB
    private AnticipoEmpleadoDaoService anticipoEmpleadoDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.CierreCuotasDescuentoService#descuentaCuotasDelPeriodo(java.lang.Long, java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void descuentaCuotasDelPeriodo(Long idPeriodoNomina, Long idOrdenPago, String usuario) {
        try {
            PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
                    NombreEntidadesRhh.PERIODO_NOMINA);
            if (periodo == null) {
                System.out.println("ATENCION: cierre de cuotas invocado con el periodo "
                        + idPeriodoNomina + ", que ya no existe. Se omite.");
                return;
            }

            List<Nomina> nominas = nominaDaoService.selectByPeriodo(periodo.getCodigo());
            if (nominas == null || nominas.isEmpty()) {
                return;
            }
            for (Nomina nomina : nominas) {
                List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());
                if (renglones == null) {
                    continue;
                }
                for (ReglonNomina renglon : renglones) {
                    if (!ORIGEN_CUOTA_DESCUENTO.equals(renglon.getTablaReferencia())
                            || renglon.getIdReferencia() == null) {
                        continue;
                    }
                    marcaCuotaDescontada(renglon, periodo, idOrdenPago);
                }
            }
        } catch (Throwable e) {
            // No debe pasar nunca -las excepciones de cada cuota ya se atrapan mas abajo-, pero
            // si algo revienta aqui arriba (por ejemplo, al leer las nominas del periodo) esta
            // transaccion es su propia REQUIRES_NEW: se pierde ella sola, y la del pago de
            // nomina -ya comiteada con el asiento y el periodo en PAGADO- no se ve afectada.
            System.out.println("ATENCION: fallo el cierre de cuotas de descuentos recurrentes del"
                    + " periodo " + idPeriodoNomina + " (orden " + idOrdenPago + ", usuario " + usuario
                    + "). El pago de nomina ya se contabilizo y sigue en pie; revise a mano el saldo"
                    + " de CuotaDescuento/DescuentoRecurrente/AnticipoEmpleado. Motivo: "
                    + e.getMessage());
        }
    }

    /**
     * Marca una CuotaDescuento como cobrada (o PARCIAL, si el renglon descontado fue menor al
     * total de la cuota por la proteccion de neto negativo) y cascada el efecto a
     * DescuentoRecurrente y, si el descuento viene de un anticipo, a AnticipoEmpleado.
     *
     * @param renglon		: Renglon de nomina que origino el descuento (RNGLTBOR=RHH.CTDS)
     * @param periodo		: Periodo de nomina que descuenta la cuota
     * @param idOrdenPago	: Id de la orden de pago, solo para los mensajes de log
     */
    private void marcaCuotaDescontada(ReglonNomina renglon, PeriodoNomina periodo, Long idOrdenPago) {
        try {
            CuotaDescuento cuota = cuotaDescuentoDaoService.selectById(renglon.getIdReferencia(),
                    NombreEntidadesRhh.CUOTA_DESCUENTO);
            if (cuota == null) {
                System.out.println("ATENCION: el renglon " + renglon.getCodigo() + " del periodo "
                        + periodo.getCodigo() + " referencia la cuota " + renglon.getIdReferencia()
                        + " (RHH.CTDS), que ya no existe. Se omite.");
                return;
            }
            if (cuota.getEstado() != null
                    && cuota.getEstado().intValue() == RhhEstadoCuotaDescuento.DESCONTADA) {
                // Idempotencia: si este metodo se llegara a ejecutar dos veces sobre el mismo
                // periodo, la segunda pasada no vuelve a descontar el saldo.
                return;
            }

            Double valorDescontado = renglon.getValor() != null ? renglon.getValor() : Double.valueOf(0D);
            Double totalCuota = cuota.getTotal() != null ? cuota.getTotal() : valorDescontado;
            boolean completa = RedondeoNomina.sonIguales(valorDescontado, totalCuota)
                    || valorDescontado.doubleValue() > totalCuota.doubleValue();

            cuota.setValorDescontado(valorDescontado);
            cuota.setPeriodoNomina(periodo);
            cuota.setEstado(Long.valueOf(completa
                    ? RhhEstadoCuotaDescuento.DESCONTADA
                    : RhhEstadoCuotaDescuento.PARCIAL));
            cuotaDescuentoDaoService.save(cuota, cuota.getCodigo());

            DescuentoRecurrente descuento = cuota.getDescuentoRecurrente();
            if (descuento == null) {
                return;
            }
            int cuotasPagadas = descuento.getCuotasPagadas() != null
                    ? descuento.getCuotasPagadas().intValue() : 0;
            descuento.setCuotasPagadas(Integer.valueOf(cuotasPagadas + 1));
            Double saldoDescuento = RedondeoNomina.redondea(Double.valueOf(
                    (descuento.getSaldo() != null ? descuento.getSaldo().doubleValue() : 0D)
                            - valorDescontado.doubleValue()));
            if (saldoDescuento.doubleValue() < 0D) {
                saldoDescuento = Double.valueOf(0D);
            }
            descuento.setSaldo(saldoDescuento);
            if (saldoDescuento.doubleValue() <= 0D) {
                descuento.setEstado(Long.valueOf(RhhEstadoDescuentoRecurrente.CANCELADO));
            }
            descuentoRecurrenteDaoService.save(descuento, descuento.getCodigo());

            // Proteccion explicitamente pedida: la cascada al anticipo va en su propio
            // try/catch, separado del resto de este metodo. Si esto falla, la CuotaDescuento y
            // el DescuentoRecurrente de arriba YA quedaron marcados -el rol no vuelve a cobrar
            // esta cuota-, y lo unico que queda desactualizado es AnticipoEmpleado.saldo, que es
            // un dato de reporte, no el circuito de cobro real.
            try {
                AnticipoEmpleado anticipo = anticipoEmpleadoDaoService
                        .selectByDescuentoRecurrente(descuento.getCodigo());
                if (anticipo != null) {
                    Double saldoAnticipo = RedondeoNomina.redondea(Double.valueOf(
                            (anticipo.getSaldo() != null ? anticipo.getSaldo().doubleValue() : 0D)
                                    - valorDescontado.doubleValue()));
                    if (saldoAnticipo.doubleValue() < 0D) {
                        saldoAnticipo = Double.valueOf(0D);
                    }
                    anticipo.setSaldo(saldoAnticipo);
                    if (saldoAnticipo.doubleValue() <= 0D) {
                        anticipo.setEstado(Long.valueOf(EstadoAnticipoEmpleado.CANCELADO));
                    }
                    anticipoEmpleadoDaoService.save(anticipo, anticipo.getCodigo());
                }
            } catch (Throwable eAnticipo) {
                System.out.println("ATENCION: la cuota " + cuota.getCodigo() + " y el descuento "
                        + descuento.getCodigo() + " se marcaron correctamente, pero fallo bajar el"
                        + " saldo del AnticipoEmpleado asociado (orden " + idOrdenPago + "). Es un"
                        + " dato de reporte desactualizado, no un problema de cobro: corrijalo a"
                        + " mano. Motivo: " + eAnticipo.getMessage());
            }
        } catch (Throwable e) {
            System.out.println("ATENCION: no se pudo marcar como descontada la cuota "
                    + renglon.getIdReferencia() + " (renglon " + renglon.getCodigo() + ", periodo "
                    + periodo.getCodigo() + ", orden " + idOrdenPago + "). El pago de nomina sigue"
                    + " en pie; corrija el saldo de esta cuota/descuento a mano. Motivo: "
                    + e.getMessage());
        }
    }

}
