package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.cnt.dao.DetalleAsientoDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AjusteTopeProvisionService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.TopeProvisionMora;
import com.saa.ejb.crd.service.dto.ResultadoAjusteTopeMora;
import com.saa.ejb.crd.service.dto.SolicitudAjusteTopeMora;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.ComponenteMovimientoInteresCuota;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoMovimientoInteresCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 7 (D-186). Ver {@link AjusteTopeProvisionService} y docs/logica-negocio/crd/API-AJUSTE-TOPE-PROVISION-MORA.md.
 */
@Stateless
public class AjusteTopeProvisionServiceImpl implements AjusteTopeProvisionService {

    private static final double TOLERANCIA = 0.01;
    private static final int MAXIMO_DETALLE = 500;
    private static final String ORIGEN = "AJUSTE_TOPE";

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private DetalleAsientoDaoService detalleAsientoDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoAjusteTopeMora ajustar(SolicitudAjusteTopeMora solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (ajustar) AjusteTopeProvisionService - asiento: "
            + (solicitud != null ? solicitud.getIdAsiento() : null) + " - corte: "
            + (solicitud != null ? solicitud.getCorte() : null) + " - grabar: "
            + (solicitud != null ? solicitud.getGrabar() : null));
        if (solicitud == null || solicitud.getIdEmpresa() == null || solicitud.getIdAsiento() == null
                || solicitud.getCorte() == null || solicitud.getCorte().trim().isEmpty()) {
            throw new IncomeException("idEmpresa, corte (AAAA-MM-DD) e idAsiento son obligatorios.");
        }
        LocalDate corte;
        try {
            corte = LocalDate.parse(solicitud.getCorte().trim());
        } catch (RuntimeException e) {
            throw new IncomeException("El corte '" + solicitud.getCorte() + "' no es una fecha AAAA-MM-DD.");
        }
        // U-2: sin usuario válido no se hace nada (ni la previsualización).
        String usuario = usuarioValidacionService.validar(solicitud.getUsuario());
        boolean grabar = Boolean.TRUE.equals(solicitud.getGrabar());
        Long idAsiento = solicitud.getIdAsiento();

        ResultadoAjusteTopeMora resultado = new ResultadoAjusteTopeMora();
        resultado.setCorte(corte.toString());
        resultado.setIdAsiento(idAsiento);

        // Idempotencia: si el asiento ya tiene su ajuste, no se repite.
        List<MovimientoInteresCuota> existentes = movimientoInteresCuotaDaoService
            .selectVigentesPorAsientoYTipo(idAsiento, TipoMovimientoInteresCuota.AJUSTE_TOPE_PROVISION);
        if (!existentes.isEmpty()) {
            double total = 0.0;
            for (MovimientoInteresCuota m : existentes) {
                total += m.getValor() != null ? m.getValor().doubleValue() : 0.0;
            }
            resultado.setYaGrabado(true);
            resultado.setCuotasAjustadas(existentes.size());
            resultado.setTotalAjuste(redondear(total));
            resultado.setCuadra(true);
            resultado.getAdvertencias().add("El asiento " + idAsiento + " ya tiene " + existentes.size()
                + " ajuste(s) tipo 10 vigentes: no se repite.");
            return resultado;
        }

        // El asiento del cuadre: ACTIVO y con fecha = corte.
        Asiento asiento = asientoService.selectById(idAsiento);
        if (asiento == null) {
            throw new IncomeException("No existe el asiento " + idAsiento + ".");
        }
        if (asiento.getEstado() == null || asiento.getEstado().longValue() != EstadoAsiento.ACTIVO) {
            throw new IncomeException("El asiento " + idAsiento + " no está ACTIVO (estado " + asiento.getEstado()
                + "): el ajuste se graba contra un asiento vigente.");
        }
        if (asiento.getFechaAsiento() == null || !asiento.getFechaAsiento().equals(corte)) {
            throw new IncomeException("El asiento " + idAsiento + " tiene fecha " + asiento.getFechaAsiento()
                + " y el corte es " + corte + ": tienen que coincidir.");
        }

        // Cuotas con saldo de provisión de mora al corte y su tope: la MISMA regla del paso ⑦ (lote 7b): tope POR PRÉSTAMO repartido entre
        // las cuotas del préstamo de la más vieja a la más nueva, sin pasar de la mora calculada pendiente de cada una.
        List<Object[]> filas = movimientoInteresCuotaDaoService.selectCuotasConProvisionMora(corte);
        // Universo del reparto: las cuotas provisionables al corte (las que ve el ⑦) más las que tienen saldo y ya no lo son.
        Map<Long, DetallePrestamo> cuotas = new HashMap<>();
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectCuotasProvisionablesAlCorte(corte)) {
            cuotas.put(cuota.getCodigo(), cuota);
        }
        List<Long> faltantes = new ArrayList<>();
        for (Object[] fila : filas) {
            Long idCuota = ((Number) fila[0]).longValue();
            if (!cuotas.containsKey(idCuota)) {
                faltantes.add(idCuota);
            }
        }
        for (DetallePrestamo cuota : detallePrestamoDaoService.selectByCodigos(faltantes)) {
            cuotas.put(cuota.getCodigo(), cuota);
        }
        Map<Long, Double> moraPagada = new HashMap<>();
        for (Object[] fila : pagoPrestamoDaoService.selectDatosPagosVigentesAlCorte(new ArrayList<>(cuotas.keySet()), corte)) {
            // [idCuota, desgravamen, moraPagada, ...]
            moraPagada.merge(((Number) fila[0]).longValue(), fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0,
                Double::sum);
        }
        Map<Long, Double> topePorPrestamo = movimientoInteresCuotaDaoService.selectMoraContabilizadaPendientePorPrestamo(corte);
        Map<Long, Double> calculadaPorCuota = new HashMap<>();
        List<TopeProvisionMora.CuotaTope> candidatas = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas.values()) {
            Prestamo prestamo = cuota.getPrestamo();
            double tasaDiaria = procesoMoraPrestamoService.tasaDiariaDelPrestamo(prestamo, false);
            double calculadaPendiente = Math.max(0.0, redondear(
                procesoMoraPrestamoService.calcularMoraCuota(cuota, tasaDiaria, corte)
                    - moraPagada.getOrDefault(cuota.getCodigo(), 0.0)));
            calculadaPorCuota.put(cuota.getCodigo(), calculadaPendiente);
            candidatas.add(new TopeProvisionMora.CuotaTope(cuota.getCodigo(),
                prestamo != null ? prestamo.getCodigo() : null,
                cuota.getFechaVencimiento() != null ? cuota.getFechaVencimiento().toLocalDate() : null,
                calculadaPendiente));
        }
        Map<Long, Double> topeRepartido = TopeProvisionMora.repartir(topePorPrestamo, candidatas);

        Map<Long, Prestamo> prestamos = new HashMap<>();
        List<ResultadoAjusteTopeMora.Cuota> ajustes = new ArrayList<>();
        Map<Long, Double> ajustePorTipo = new TreeMap<>();
        for (Object[] fila : filas) {
            Long idCuota = ((Number) fila[0]).longValue();
            Long idPrestamo = ((Number) fila[1]).longValue();
            double saldo = redondear(((Number) fila[2]).doubleValue());
            Prestamo prestamo = prestamos.get(idPrestamo);
            if (prestamo == null) {
                prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
                prestamos.put(idPrestamo, prestamo);
            }
            DetallePrestamo cuota = cuotas.get(idCuota);
            double calculadaPendiente = calculadaPorCuota.getOrDefault(idCuota, 0.0);
            // Una cuota que ya no existe (la borró un abono) no entra al reparto: su tope es 0 y todo su saldo es exceso.
            double tope = topeRepartido.getOrDefault(idCuota, 0.0);
            double ajuste = Math.max(0.0, redondear(saldo - tope));
            if (ajuste <= 0.004) {
                continue;
            }
            Long idTipo = prestamo != null && prestamo.getProducto() != null
                && prestamo.getProducto().getTipoPrestamo() != null
                ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
            if (idTipo == null) {
                throw new IncomeException("El préstamo " + idPrestamo + " (cuota " + idCuota + ") tiene exceso de"
                    + " provisión de mora pero su producto no tiene tipo de préstamo: no se puede validar contra el asiento.");
            }
            ResultadoAjusteTopeMora.Cuota c = new ResultadoAjusteTopeMora.Cuota();
            c.setIdCuota(idCuota);
            c.setIdPrestamo(idPrestamo);
            c.setOperacion(prestamo.getIdAsoprep() != null ? prestamo.getIdAsoprep() : prestamo.getCodigo());
            c.setNumeroCuota(cuota != null && cuota.getNumeroCuota() != null ? Math.round(cuota.getNumeroCuota()) : null);
            c.setIdTipoPrestamo(idTipo);
            c.setSaldoProvisionadoMora(saldo);
            c.setMoraCalculadaPendiente(calculadaPendiente);
            // La mora contabilizada pendiente es del PRÉSTAMO (lote 7b), no de la cuota.
            c.setMoraContabilizadaPendiente(topePorPrestamo.getOrDefault(idPrestamo, 0.0));
            c.setTope(redondear(tope));
            c.setAjuste(ajuste);
            ajustes.add(c);
            ajustePorTipo.merge(idTipo, ajuste, Double::sum);
        }

        // Validación contra el asiento, por cuenta de la 1.4.99.05 (plantilla 36, papel 81, por tipo de préstamo).
        Map<String, Double> asientoPorCuenta = new LinkedHashMap<>();
        for (DetalleAsiento linea : detalleAsientoDaoService.selectByIdAsiento(idAsiento)) {
            String cuenta = linea.getNumeroCuenta();
            if (cuenta == null) {
                continue;
            }
            double neto = (linea.getValorDebe() != null ? linea.getValorDebe() : 0.0)
                - (linea.getValorHaber() != null ? linea.getValorHaber() : 0.0);
            asientoPorCuenta.merge(cuenta, neto, Double::sum);
        }
        Long idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_INTERESES, solicitud.getIdEmpresa());
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable alterno " + PlantillasCredito.PROVISION_INTERESES
                + " (provisión de intereses) para la empresa " + solicitud.getIdEmpresa() + ".");
        }
        Map<Long, String> cuentaPorTipo = new LinkedHashMap<>();
        Map<String, Double> ajustePorCuenta = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entrada : ajustePorTipo.entrySet()) {
            DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla,
                CrdLineaAsiento.PROVISION_INTERESES, entrada.getKey().intValue());
            if (linea == null || linea.getPlanCuenta() == null) {
                throw new IncomeException("La plantilla " + PlantillasCredito.PROVISION_INTERESES + " no define la línea "
                    + CrdLineaAsiento.PROVISION_INTERESES + " para el tipo de préstamo " + entrada.getKey()
                    + ": no se puede validar el ajuste contra el asiento.");
            }
            String cuenta = linea.getPlanCuenta().getCuentaContable();
            cuentaPorTipo.put(entrada.getKey(), cuenta);
            ajustePorCuenta.merge(cuenta, entrada.getValue(), Double::sum);
        }
        boolean cuadra = true;
        double totalAsiento = 0.0;
        for (Map.Entry<String, Double> entrada : ajustePorCuenta.entrySet()) {
            double enAsiento = redondear(asientoPorCuenta.getOrDefault(entrada.getKey(), 0.0));
            totalAsiento += enAsiento;
            if (Math.abs(redondear(entrada.getValue()) - enAsiento) > TOLERANCIA) {
                cuadra = false;
            }
        }
        for (Map.Entry<Long, Double> entrada : ajustePorTipo.entrySet()) {
            ResultadoAjusteTopeMora.Tipo t = new ResultadoAjusteTopeMora.Tipo();
            t.setIdTipoPrestamo(entrada.getKey());
            String cuenta = cuentaPorTipo.get(entrada.getKey());
            t.setCuenta(cuenta);
            t.setAjuste(redondear(entrada.getValue()));
            double enAsiento = redondear(asientoPorCuenta.getOrDefault(cuenta, 0.0));
            t.setAsiento(enAsiento);
            t.setDiferencia(redondear(redondear(ajustePorCuenta.get(cuenta)) - enAsiento));
            resultado.getPorTipo().add(t);
        }
        double totalAjuste = 0.0;
        for (ResultadoAjusteTopeMora.Cuota c : ajustes) {
            totalAjuste += c.getAjuste();
        }
        resultado.setTotalAjuste(redondear(totalAjuste));
        resultado.setTotalAsiento(redondear(totalAsiento));
        resultado.setCuadra(cuadra);
        resultado.setCuotasAjustadas(ajustes.size());
        if (ajustes.isEmpty()) {
            resultado.getAdvertencias().add("No hay exceso de provisión de mora al " + corte + ": nada que ajustar.");
        }

        if (Boolean.TRUE.equals(solicitud.getIncluirDetalle())) {
            List<ResultadoAjusteTopeMora.Cuota> ordenadas = new ArrayList<>(ajustes);
            ordenadas.sort(Comparator.comparingDouble(ResultadoAjusteTopeMora.Cuota::getAjuste).reversed());
            resultado.setDetalle(new ArrayList<>(ordenadas.subList(0, Math.min(MAXIMO_DETALLE, ordenadas.size()))));
            if (ordenadas.size() > MAXIMO_DETALLE) {
                resultado.getAdvertencias().add("El detalle muestra las " + MAXIMO_DETALLE + " cuotas con más ajuste de "
                    + ordenadas.size() + "; el detalle completo sale del SQL 391.");
            }
        }

        if (grabar) {
            if (ajustes.isEmpty()) {
                return resultado;
            }
            if (!cuadra) {
                throw new IncomeException("El ajuste calculado ($" + resultado.getTotalAjuste() + ") no cuadra con el"
                    + " asiento " + idAsiento + " ($" + resultado.getTotalAsiento() + ") por cuenta de provisión: "
                    + detalleDiferencias(resultado) + ". No se graba nada.");
            }
            LocalDateTime ahora = LocalDateTime.now();
            for (ResultadoAjusteTopeMora.Cuota c : ajustes) {
                MovimientoInteresCuota mov = new MovimientoInteresCuota();
                Prestamo referencia = new Prestamo();
                referencia.setCodigo(c.getIdPrestamo());
                mov.setPrestamo(referencia);
                mov.setIdCuota(c.getIdCuota());
                mov.setTipoMovimiento(TipoMovimientoInteresCuota.AJUSTE_TOPE_PROVISION);
                mov.setComponente(ComponenteMovimientoInteresCuota.MORA);
                mov.setValor(c.getAjuste());
                mov.setFechaContable(corte);
                mov.setOrigen(ORIGEN);
                mov.setIdOrigen(idAsiento);
                mov.setAsiento(idAsiento);
                mov.setAnulado(0L);
                mov.setUsuarioRegistro(usuario);
                mov.setFechaRegistro(ahora);
                movimientoInteresCuotaDaoService.save(mov, null);
            }
            resultado.setGrabado(true);
        }
        return resultado;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int anular(Long idAsiento, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo (anular) AjusteTopeProvisionService - asiento: " + idAsiento);
        if (idAsiento == null) {
            throw new IncomeException("idAsiento es obligatorio.");
        }
        usuarioValidacionService.validar(usuario);
        return movimientoInteresCuotaDaoService.anularByAsientoYTipo(idAsiento,
            TipoMovimientoInteresCuota.AJUSTE_TOPE_PROVISION);
    }

    private String detalleDiferencias(ResultadoAjusteTopeMora resultado) {
        StringBuilder texto = new StringBuilder();
        for (ResultadoAjusteTopeMora.Tipo t : resultado.getPorTipo()) {
            if (Math.abs(t.getDiferencia()) > TOLERANCIA) {
                texto.append(texto.length() > 0 ? "; " : "").append("cuenta ").append(t.getCuenta()).append(" (tipo ")
                    .append(t.getIdTipoPrestamo()).append("): ajuste $").append(t.getAjuste()).append(" vs asiento $")
                    .append(t.getAsiento());
            }
        }
        return texto.toString();
    }

    private static double redondear(double valor) {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
