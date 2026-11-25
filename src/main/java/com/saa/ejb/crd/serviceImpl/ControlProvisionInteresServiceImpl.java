package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AsientoCierreCarteraDaoService;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.MovimientoInteresCuotaDaoService;
import com.saa.ejb.crd.service.ControlProvisionInteresService;
import com.saa.ejb.crd.service.LibroInteresArranqueService;
import com.saa.ejb.crd.service.ModoCorteCartera;
import com.saa.ejb.crd.service.dto.ResultadoControlProvision;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.AsientoCierreCartera;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.MovimientoInteresCuota;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.ComponenteMovimientoInteresCuota;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.SubProcesoCierreCartera;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMovimientoInteresCuota;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Lote 22. Ver {@link ControlProvisionInteresService} y el doc de diseño de la provisión de intereses (Lote 22).
 */
@Stateless
public class ControlProvisionInteresServiceImpl implements ControlProvisionInteresService {

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    @EJB
    private MovimientoInteresCuotaDaoService movimientoInteresCuotaDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private LibroInteresArranqueService libroInteresArranqueService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoCierreCarteraDaoService asientoCierreCarteraDaoService;

    // =====================================================================
    // Control
    // =====================================================================

    @Override
    public ResultadoControlProvision calcular(Long idEmpresa, LocalDate corte, Map<Long, double[]> proyeccion) throws Throwable {
        System.out.println("Ingresa al metodo (calcular) ControlProvisionInteresService - empresa: " + idEmpresa + " - corte: " + corte
            + " - proyeccion: " + (proyeccion != null));
        ResultadoControlProvision resultado = new ResultadoControlProvision();
        resultado.setCorte(corte != null ? corte.toString() : null);
        resultado.setProyeccion(proyeccion != null);
        if (idEmpresa == null || corte == null || !ModoCorteCartera.devengoCompletoActivo(corte)) {
            resultado.setAplica(false);
            return resultado;
        }
        resultado.setAplica(true);

        // Libro por tipo: {devengo interés, devengo mora, provisión interés, provisión mora}.
        Map<Long, double[]> libro = new TreeMap<>();
        for (Object[] fila : movimientoInteresCuotaDaoService.selectLibroPorTipoAlCorte(corte)) {
            Long tipo = (Long) fila[0];
            long movimiento = (Long) fila[1];
            long componente = (Long) fila[2];
            double valor = (Double) fila[3];
            double[] l = libro.computeIfAbsent(tipo, k -> new double[4]);
            if (movimiento == TipoMovimientoInteresCuota.DEVENGO_INTERES) {
                l[0] += valor;
            } else if (movimiento == TipoMovimientoInteresCuota.REVERSO_DEVENGO_INTERES) {
                l[0] -= valor;
            } else if (movimiento == TipoMovimientoInteresCuota.DEVENGO_MORA) {
                l[1] += valor;
            } else if (movimiento == TipoMovimientoInteresCuota.REVERSO_DEVENGO_POR_COBRO_TARDIO) {
                l[1] -= valor;
            } else {
                double signo = movimiento == TipoMovimientoInteresCuota.PROVISION || movimiento == TipoMovimientoInteresCuota.RE_PROVISION
                    || movimiento == TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_ALTA ? 1.0 : -1.0;
                l[componente == ComponenteMovimientoInteresCuota.INTERES ? 2 : 3] += signo * valor;
            }
        }
        TreeSet<Long> tipos = new TreeSet<>(libro.keySet());
        if (proyeccion != null) {
            tipos.addAll(proyeccion.keySet());
        }
        for (long t = 1L; t <= 3L; t++) {
            tipos.add(Long.valueOf(t));
        }

        Long plantilla17 = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        Long plantilla36 = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_INTERESES, idEmpresa);
        if (plantilla17 == null || plantilla17.longValue() == 0L || plantilla36 == null || plantilla36.longValue() == 0L) {
            resultado.getExcepciones().add("No existen las plantillas contables " + PlantillasCredito.DEVENGO_INTERESES + " y/o "
                + PlantillasCredito.PROVISION_INTERESES + " para la empresa " + idEmpresa + ": no se puede resolver las cuentas del control.");
            resultado.setCuadra(false);
            return resultado;
        }
        boolean todoCuadra = true;
        for (Long tipo : tipos) {
            DetallePlantilla linea1402 = detallePlantillaDaoService.selectByPlantillaYAuxiliares(plantilla17,
                CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, tipo.intValue());
            DetallePlantilla linea149905 = detallePlantillaDaoService.selectByPlantillaYAuxiliares(plantilla36,
                CrdLineaAsiento.PROVISION_INTERESES, tipo.intValue());
            if (linea1402 == null || linea1402.getPlanCuenta() == null || linea149905 == null || linea149905.getPlanCuenta() == null) {
                continue;
            }
            ResultadoControlProvision.Tipo t = new ResultadoControlProvision.Tipo();
            t.setIdTipoPrestamo(tipo);
            t.setCuenta1402(linea1402.getPlanCuenta().getCuentaContable());
            t.setCuenta149905(linea149905.getPlanCuenta().getCuentaContable());
            double[] p = proyeccion != null && proyeccion.get(tipo) != null ? proyeccion.get(tipo) : new double[6];
            double[] l = libro.getOrDefault(tipo, new double[4]);
            t.setContable1402(redondear(saldoDeudor(idEmpresa, t.getCuenta1402(), corte) + p[0]));
            t.setContable149905(redondear(-saldoDeudor(idEmpresa, t.getCuenta149905(), corte) + p[3]));
            t.setLibro1402Interes(redondear(l[0] + p[1]));
            t.setLibro1402Mora(redondear(l[1] + p[2]));
            t.setLibroProvInteres(redondear(l[2] + p[4]));
            t.setLibroProvMora(redondear(l[3] + p[5]));
            t.setDifContable(redondear(t.getContable1402() - t.getContable149905()));
            t.setDif1402Libro(redondear(t.getContable1402() - t.getLibro1402Interes() - t.getLibro1402Mora()));
            t.setDifProvLibro(redondear(t.getContable149905() - t.getLibroProvInteres() - t.getLibroProvMora()));
            t.setDifLibroInteres(redondear(t.getLibro1402Interes() - t.getLibroProvInteres()));
            t.setDifLibroMora(redondear(t.getLibro1402Mora() - t.getLibroProvMora()));
            t.setCuadra(Math.abs(t.getDifContable()) <= TOLERANCIA && Math.abs(t.getDif1402Libro()) <= TOLERANCIA
                && Math.abs(t.getDifProvLibro()) <= TOLERANCIA && Math.abs(t.getDifLibroInteres()) <= TOLERANCIA
                && Math.abs(t.getDifLibroMora()) <= TOLERANCIA);
            todoCuadra &= t.isCuadra();
            resultado.getPorTipo().add(t);
        }
        resultado.setCuadra(todoCuadra);
        resultado.setAjusteRegistrado(proyeccion == null && movimientoInteresCuotaDaoService.existeAjusteUltimoRecurso(corte));
        return resultado;
    }

    @Override
    public ResultadoControlProvision controlParaCierreContable(Long idEmpresa, LocalDate corte) throws Throwable {
        return calcular(idEmpresa, corte, null);
    }

    // =====================================================================
    // Ajuste de último recurso
    // =====================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoControlProvision controlarYAjustar(Long idEmpresa, LocalDate corte, CorridaCierreCartera corrida, String usuario,
            String ip) throws Throwable {
        ResultadoControlProvision resultado = calcular(idEmpresa, corte, null);
        if (!resultado.isAplica() || resultado.isCuadra()) {
            return resultado;
        }
        for (ResultadoControlProvision.Tipo t : resultado.getPorTipo()) {
            if (!t.isCuadra()) {
                resultado.getExcepciones().add("Tipo " + t.getIdTipoPrestamo() + ": 1.4.02 contable " + t.getContable1402() + " (libro "
                    + redondear(t.getLibro1402Interes() + t.getLibro1402Mora()) + "), 1.4.99.05 contable " + t.getContable149905()
                    + " (libro " + redondear(t.getLibroProvInteres() + t.getLibroProvMora()) + "), 1.4.02 − 1.4.99.05 = " + t.getDifContable()
                    + "; libro interés " + t.getDifLibroInteres() + ", libro mora " + t.getDifLibroMora() + ". Causa probable: " + causa(t));
            }
        }
        if (!ModoCorteCartera.ajusteUltimoRecursoActivo()) {
            resultado.getExcepciones().add("El ajuste de último recurso NO se aplicó: la propiedad " + ModoCorteCartera.PROPIEDAD_AJUSTE_ULTIMO_RECURSO
                + " está apagada. La diferencia queda registrada en la corrida.");
            return resultado;
        }

        // Lo que hay que mover en el libro para que quede igual a lo devengado y no cobrado al corte (A1 y A2 automáticos).
        List<Object[]> deltasProvision = libroInteresArranqueService.deltasLibroProvisionAlCorte(corte);
        List<Object[]> deltasDevengo = libroInteresArranqueService.deltasLibroDevengoInteresAlCorte(corte);
        Map<Long, double[]> netoPorTipo = new HashMap<>(); // {devengo interés, provisión interés, provisión mora}
        for (Object[] f : deltasDevengo) {
            netoPorTipo.computeIfAbsent((Long) f[5], k -> new double[3])[0] += (Boolean) f[3] ? -(Double) f[4] : (Double) f[4];
        }
        for (Object[] f : deltasProvision) {
            double[] n = netoPorTipo.computeIfAbsent((Long) f[5], k -> new double[3]);
            double valor = (Boolean) f[3] ? -(Double) f[4] : (Double) f[4];
            n[((Long) f[2]).longValue() == ComponenteMovimientoInteresCuota.INTERES ? 1 : 2] += valor;
        }

        // Asiento: lleva la contabilidad al libro ya corregido.
        Long plantilla17 = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        Long plantilla36 = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_INTERESES, idEmpresa);
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (ResultadoControlProvision.Tipo t : resultado.getPorTipo()) {
            double[] n = netoPorTipo.getOrDefault(t.getIdTipoPrestamo(), new double[3]);
            double d1 = redondear(t.getContable1402() - (t.getLibro1402Interes() + n[0] + t.getLibro1402Mora()));
            double d2 = redondear(t.getContable149905() - (t.getLibroProvInteres() + n[1] + t.getLibroProvMora() + n[2]));
            int tipo = t.getIdTipoPrestamo().intValue();
            if (Math.abs(d1) > TOLERANCIA / 2) {
                // d1 > 0: la contabilidad tiene más 1.4.02 que el libro: H 1.4.02 / D ingreso. d1 < 0, al revés.
                lineas.add(linea(plantilla17, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, tipo, Math.abs(d1), d1 < 0,
                    "Ajuste de último recurso: 1.4.02 al libro por cuota"));
                lineas.add(linea(plantilla17, CrdLineaAsiento.INGRESO_INTERES_ORDINARIO, tipo, Math.abs(d1), d1 > 0,
                    "Ajuste de último recurso: ingreso por interés al libro por cuota"));
            }
            if (Math.abs(d2) > TOLERANCIA / 2) {
                // d2 > 0: más provisión contable que en el libro: D 1.4.99.05 / H gasto. d2 < 0, al revés.
                lineas.add(linea(plantilla36, CrdLineaAsiento.PROVISION_INTERESES, tipo, Math.abs(d2), d2 > 0,
                    "Ajuste de último recurso: provisión de intereses al libro por cuota"));
                lineas.add(linea(plantilla36, CrdLineaAsiento.PROVISION_INTERESES_GASTO, tipo, Math.abs(d2), d2 < 0,
                    "Ajuste de último recurso: gasto de provisión al libro por cuota"));
            }
        }
        Asiento asiento = null;
        if (!lineas.isEmpty()) {
            asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS, corte,
                "CRD ajuste de último recurso de la provisión de intereses " + corte, usuario, lineas,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
            AsientoCierreCartera registro = new AsientoCierreCartera();
            registro.setCorrida(corrida);
            registro.setSubProceso(Long.valueOf(SubProcesoCierreCartera.AJUSTE_ULTIMO_RECURSO));
            registro.setAsiento(asiento.getCodigo());
            registro.setNumeroAsiento(asiento.getNumero() != null ? asiento.getNumero().toString() : null);
            registro.setFecha(corte);
            double total = 0.0;
            for (DetalleAsiento l : lineas) {
                total += l.getValorDebe() != null ? l.getValorDebe() : 0.0;
            }
            registro.setValor(redondear(total));
            registro.setCantidad(Long.valueOf(lineas.size()));
            registro.setIdEstado(Long.valueOf(EstadoAsiento.ACTIVO));
            registro.setEstado(Long.valueOf(Estado.ACTIVO));
            registro.setUsuarioRegistro(usuario);
            registro.setIpRegistro(ip);
            registro.setFechaRegistro(LocalDateTime.now());
            asientoCierreCarteraDaoService.save(registro, null);
        }

        // Libro por cuota: lo que corresponde, para que el mes siguiente no repita la diferencia.
        LocalDateTime ahora = LocalDateTime.now();
        for (Object[] f : deltasDevengo) {
            guardar(f, (Boolean) f[3] ? TipoMovimientoInteresCuota.REVERSO_DEVENGO_INTERES : TipoMovimientoInteresCuota.DEVENGO_INTERES,
                corte, corrida, asiento, usuario, ahora);
        }
        for (Object[] f : deltasProvision) {
            guardar(f, (Boolean) f[3] ? TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_BAJA
                : TipoMovimientoInteresCuota.AJUSTE_LIBRO_PROVISION_ALTA, corte, corrida, asiento, usuario, ahora);
        }

        ResultadoControlProvision despues = calcular(idEmpresa, corte, null);
        despues.getExcepciones().addAll(resultado.getExcepciones());
        despues.getExcepciones().add("Ajuste de último recurso aplicado al " + corte + ": " + deltasDevengo.size()
            + " movimiento(s) de devengo de interés (MVIC 11/12) y " + deltasProvision.size() + " de provisión (MVIC 13/14) en el libro"
            + (asiento != null ? "; asiento " + asiento.getCodigo() : "; sin asiento (la contabilidad ya coincidía con el libro corregido)") + ".");
        despues.setAjusteAplicado(true);
        despues.setIdAsientoAjuste(asiento != null ? asiento.getCodigo() : null);
        despues.setAjusteRegistrado(true);
        return despues;
    }

    // =====================================================================
    // 3.1 — control del día 1
    // =====================================================================

    @Override
    public void controlDia1(ResultadoControlProvision resultado, Long idEmpresa, LocalDate fechaDia1, Long idAsientoDevengo) throws Throwable {
        if (resultado == null || !resultado.isAplica() || fechaDia1 == null || idAsientoDevengo == null) {
            return;
        }
        Map<Long, Double> devengo = new HashMap<>();
        for (Object[] f : movimientoInteresCuotaDaoService.selectDevengoPorAsientoYTipo(idAsientoDevengo)) {
            devengo.put(((Number) f[0]).longValue(), ((Number) f[1]).doubleValue());
        }
        for (ResultadoControlProvision.Tipo t : resultado.getPorTipo()) {
            ResultadoControlProvision.Dia1 d = new ResultadoControlProvision.Dia1();
            d.setIdTipoPrestamo(t.getIdTipoPrestamo());
            d.setDiferenciaContable(redondear(saldoDeudor(idEmpresa, t.getCuenta1402(), fechaDia1)
                + saldoDeudor(idEmpresa, t.getCuenta149905(), fechaDia1)));
            d.setDevengoDia1(redondear(devengo.getOrDefault(t.getIdTipoPrestamo(), 0.0)));
            d.setDiferencia(redondear(d.getDiferenciaContable() - d.getDevengoDia1()));
            resultado.getDia1().add(d);
        }
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    /** Causa probable de una diferencia, según dónde está. Orientativa: el cuadre la confirma con el SQL de control. */
    private String causa(ResultadoControlProvision.Tipo t) {
        List<String> causas = new ArrayList<>();
        if (Math.abs(t.getDif1402Libro()) > TOLERANCIA) {
            causas.add("la 1.4.02 contable se movió sin libro (asiento manual, ⑪ de interés anticipado, devengo o reverso sin MVIC 11/12, o libro sin semilla A2)");
        }
        if (Math.abs(t.getDifProvLibro()) > TOLERANCIA) {
            causas.add("la 1.4.99.05 contable se movió sin libro (asiento manual de provisión o ajuste del cuadre sin A1)");
        }
        if (Math.abs(t.getDifLibroInteres()) > TOLERANCIA) {
            causas.add("el libro de interés no iguala devengo con provisión (provisión de cuotas cerradas o negativas, o devengo sin sembrar)");
        }
        if (Math.abs(t.getDifLibroMora()) > TOLERANCIA) {
            causas.add("la provisión de mora no iguala la mora devengada (tope de la mora por cuota, nivel 2.2)");
        }
        if (causas.isEmpty()) {
            causas.add("la regla 1.4.02 = 1.4.99.05 no cierra aunque cada cuenta coincide con su libro");
        }
        return String.join("; ", causas) + ".";
    }

    private void guardar(Object[] fila, long tipoMovimiento, LocalDate corte, CorridaCierreCartera corrida, Asiento asiento, String usuario,
            LocalDateTime ahora) throws Throwable {
        MovimientoInteresCuota mov = new MovimientoInteresCuota();
        Prestamo prestamo = new Prestamo();
        prestamo.setCodigo((Long) fila[1]);
        mov.setPrestamo(prestamo);
        mov.setIdCuota((Long) fila[0]);
        mov.setTipoMovimiento(tipoMovimiento);
        mov.setComponente((Long) fila[2]);
        mov.setValor(redondear((Double) fila[4]));
        mov.setFechaContable(corte);
        mov.setCorrida(corrida);
        mov.setOrigen(ORIGEN_AJUSTE_ULTIMO_RECURSO);
        mov.setIdOrigen(corrida.getCodigo());
        mov.setAsiento(asiento != null ? asiento.getCodigo() : null);
        mov.setAnulado(0L);
        mov.setUsuarioRegistro(usuario);
        mov.setFechaRegistro(ahora);
        movimientoInteresCuotaDaoService.save(mov, null);
    }

    private DetalleAsiento linea(Long idPlantilla, int papel, int idTipo, double valor, boolean debe, String descripcion) throws Throwable {
        DetallePlantilla l = detallePlantillaDaoService.selectByPlantillaYAuxiliares(idPlantilla, papel, idTipo);
        if (l == null || l.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla " + idPlantilla + " no define la línea " + papel + " para el tipo de préstamo " + idTipo
                + ": no se puede asentar el ajuste de último recurso.");
        }
        PlanCuenta cuenta = l.getPlanCuenta();
        DetalleAsiento d = new DetalleAsiento();
        d.setPlanCuenta(cuenta);
        d.setNumeroCuenta(cuenta.getCuentaContable());
        d.setNombreCuenta(cuenta.getNombre());
        d.setDescripcion(descripcion);
        d.setValorDebe(debe ? redondear(valor) : 0.0);
        d.setValorHaber(debe ? 0.0 : redondear(valor));
        return d;
    }

    private double saldoDeudor(Long idEmpresa, String cuenta, LocalDate corte) throws Throwable {
        double total = 0.0;
        for (Object[] fila : cierreCarteraDaoService.selectSaldoContablePorCuenta(idEmpresa, cuenta, null, corte)) {
            total += fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
        }
        return total;
    }

    private static double redondear(double valor) {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
