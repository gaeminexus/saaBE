package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * Lote 22 y lote 24. Ver {@link ControlProvisionInteresService} y el doc de diseño de la provisión de intereses (Lotes 22 y 24).
 */
@Stateless
public class ControlProvisionInteresServiceImpl implements ControlProvisionInteresService {

    private static final String ROL_1402 = "1.4.02";
    private static final String ROL_149905 = "1.4.99.05";

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

        // Tipos con sus cuentas (de las plantillas) y el libro de cada uno; las cuentas se agrupan: una cuenta puede servir a varios tipos.
        Map<String, List<Long>> tiposPorCuenta1402 = new LinkedHashMap<>();
        Map<String, List<Long>> tiposPorCuenta149905 = new LinkedHashMap<>();
        Map<Long, double[]> proyPorTipo = new HashMap<>();
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
            proyPorTipo.put(tipo, p);
            t.setLibro1402Interes(redondear(l[0] + p[1]));
            t.setLibro1402Mora(redondear(l[1] + p[2]));
            t.setLibroProvInteres(redondear(l[2] + p[4]));
            t.setLibroProvMora(redondear(l[3] + p[5]));
            t.setDifLibroInteres(redondear(t.getLibro1402Interes() - t.getLibroProvInteres()));
            t.setDifLibroMora(redondear(t.getLibro1402Mora() - t.getLibroProvMora()));
            t.setCuadra(Math.abs(t.getDifLibroInteres()) <= TOLERANCIA && Math.abs(t.getDifLibroMora()) <= TOLERANCIA);
            todoCuadra &= t.isCuadra();
            resultado.getPorTipo().add(t);
            tiposPorCuenta1402.computeIfAbsent(t.getCuenta1402(), k -> new ArrayList<>()).add(tipo);
            tiposPorCuenta149905.computeIfAbsent(t.getCuenta149905(), k -> new ArrayList<>()).add(tipo);
        }
        Map<Long, ResultadoControlProvision.Tipo> tipoPorId = new HashMap<>();
        for (ResultadoControlProvision.Tipo t : resultado.getPorTipo()) {
            tipoPorId.put(t.getIdTipoPrestamo(), t);
        }

        // Por cuenta contable: su saldo contra la suma del libro de los tipos que la usan.
        Map<String, ResultadoControlProvision.Cuenta> cuentas1402 = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> e : tiposPorCuenta1402.entrySet()) {
            ResultadoControlProvision.Cuenta c = new ResultadoControlProvision.Cuenta();
            c.setCuenta(e.getKey());
            c.setRol(ROL_1402);
            c.setTipos(new ArrayList<>(e.getValue()));
            double contable = saldoDeudor(idEmpresa, e.getKey(), corte);
            double libroCuenta = 0.0;
            for (Long tipo : e.getValue()) {
                contable += proyPorTipo.get(tipo)[0];
                libroCuenta += tipoPorId.get(tipo).getLibro1402Interes() + tipoPorId.get(tipo).getLibro1402Mora();
            }
            cierraCuenta(c, contable, libroCuenta);
            todoCuadra &= c.isCuadra();
            cuentas1402.put(e.getKey(), c);
            resultado.getPorCuenta().add(c);
        }
        Map<String, ResultadoControlProvision.Cuenta> cuentas149905 = new LinkedHashMap<>();
        for (Map.Entry<String, List<Long>> e : tiposPorCuenta149905.entrySet()) {
            ResultadoControlProvision.Cuenta c = new ResultadoControlProvision.Cuenta();
            c.setCuenta(e.getKey());
            c.setRol(ROL_149905);
            c.setTipos(new ArrayList<>(e.getValue()));
            double contable = -saldoDeudor(idEmpresa, e.getKey(), corte);
            double libroCuenta = 0.0;
            for (Long tipo : e.getValue()) {
                contable += proyPorTipo.get(tipo)[3];
                libroCuenta += tipoPorId.get(tipo).getLibroProvInteres() + tipoPorId.get(tipo).getLibroProvMora();
            }
            cierraCuenta(c, contable, libroCuenta);
            todoCuadra &= c.isCuadra();
            cuentas149905.put(e.getKey(), c);
            resultado.getPorCuenta().add(c);
        }

        // La regla del contador por grupo de cuentas (tipos que comparten alguna cuenta) y al total.
        Map<Long, Long> raiz = new HashMap<>();
        for (Long tipo : tipoPorId.keySet()) {
            raiz.put(tipo, tipo);
        }
        for (List<Long> lista : tiposPorCuenta1402.values()) {
            unir(raiz, lista);
        }
        for (List<Long> lista : tiposPorCuenta149905.values()) {
            unir(raiz, lista);
        }
        Map<Long, ResultadoControlProvision.Grupo> grupos = new TreeMap<>();
        for (Long tipo : tipoPorId.keySet()) {
            ResultadoControlProvision.Grupo g = grupos.computeIfAbsent(buscar(raiz, tipo), k -> new ResultadoControlProvision.Grupo());
            g.getTipos().add(tipo);
            ResultadoControlProvision.Tipo t = tipoPorId.get(tipo);
            if (!g.getCuentas1402().contains(t.getCuenta1402())) {
                g.getCuentas1402().add(t.getCuenta1402());
                g.setContable1402(g.getContable1402() + cuentas1402.get(t.getCuenta1402()).getContable());
            }
            if (!g.getCuentas149905().contains(t.getCuenta149905())) {
                g.getCuentas149905().add(t.getCuenta149905());
                g.setContable149905(g.getContable149905() + cuentas149905.get(t.getCuenta149905()).getContable());
            }
        }
        for (ResultadoControlProvision.Grupo g : grupos.values()) {
            g.setContable1402(redondear(g.getContable1402()));
            g.setContable149905(redondear(g.getContable149905()));
            g.setDiferencia(redondear(g.getContable1402() - g.getContable149905()));
            g.setCuadra(Math.abs(g.getDiferencia()) <= TOLERANCIA);
            todoCuadra &= g.isCuadra();
            resultado.getPorGrupo().add(g);
        }
        double total1402 = 0.0;
        for (ResultadoControlProvision.Cuenta c : cuentas1402.values()) {
            total1402 += c.getContable();
        }
        double total149905 = 0.0;
        for (ResultadoControlProvision.Cuenta c : cuentas149905.values()) {
            total149905 += c.getContable();
        }
        resultado.setTotalContable1402(redondear(total1402));
        resultado.setTotalContable149905(redondear(total149905));
        resultado.setDifTotal(redondear(total1402 - total149905));
        todoCuadra &= Math.abs(resultado.getDifTotal()) <= TOLERANCIA;
        resultado.setCuadra(todoCuadra);
        resultado.setAjusteRegistrado(proyeccion == null && movimientoInteresCuotaDaoService.existeAjusteUltimoRecurso(corte));
        return resultado;
    }

    @Override
    public ResultadoControlProvision controlParaCierreContable(Long idEmpresa, LocalDate corte) throws Throwable {
        ResultadoControlProvision resultado = calcular(idEmpresa, corte, null);
        if (resultado.isAplica() && !resultado.isCuadra()) {
            agregaExcepciones(resultado);
        }
        return resultado;
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
        agregaExcepciones(resultado);
        if (!ModoCorteCartera.ajusteUltimoRecursoActivo()) {
            resultado.getExcepciones().add("El ajuste de último recurso NO se aplicó: la propiedad " + ModoCorteCartera.PROPIEDAD_AJUSTE_ULTIMO_RECURSO
                + " está apagada. La diferencia queda registrada en la corrida.");
            return resultado;
        }

        // Lo que hay que mover en el libro para que quede igual a lo devengado y no cobrado al corte (A1 y A2 automáticos).
        List<Object[]> deltasProvision = libroInteresArranqueService.deltasLibroProvisionAlCorte(corte);
        List<Object[]> deltasDevengo = libroInteresArranqueService.deltasLibroDevengoInteresAlCorte(corte);
        Map<Long, double[]> netoPorTipo = new HashMap<>(); // {devengo interés, provisión interés + mora}
        for (Object[] f : deltasDevengo) {
            netoPorTipo.computeIfAbsent((Long) f[5], k -> new double[2])[0] += (Boolean) f[3] ? -(Double) f[4] : (Double) f[4];
        }
        for (Object[] f : deltasProvision) {
            netoPorTipo.computeIfAbsent((Long) f[5], k -> new double[2])[1] += (Boolean) f[3] ? -(Double) f[4] : (Double) f[4];
        }

        // Asiento: lleva la contabilidad al libro ya corregido, POR CUENTA. Cuenta de un solo tipo: toda la diferencia contra su libro. Cuenta compartida: solo
        // lo que el libro de cada tipo explica (lo que corrige el ajuste del libro); lo demás es una diferencia solo contable que no se asienta.
        Long plantilla17 = plantillaService.codigoByAlterno(PlantillasCredito.DEVENGO_INTERESES, idEmpresa);
        Long plantilla36 = plantillaService.codigoByAlterno(PlantillasCredito.PROVISION_INTERESES, idEmpresa);
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (ResultadoControlProvision.Cuenta c : resultado.getPorCuenta()) {
            boolean devengo = ROL_1402.equals(c.getRol());
            double explicado = 0.0;
            Map<Long, Double> deltaPorTipo = new LinkedHashMap<>();
            for (Long tipo : c.getTipos()) {
                double[] n = netoPorTipo.getOrDefault(tipo, new double[2]);
                double delta = devengo ? n[0] : n[1];
                deltaPorTipo.put(tipo, delta);
                explicado += delta;
            }
            double movimiento = redondear(c.getLibro() + explicado - c.getContable());
            Map<Long, Double> montoPorTipo = new LinkedHashMap<>();
            if (c.getTipos().size() == 1) {
                montoPorTipo.put(c.getTipos().get(0), movimiento);
            } else {
                montoPorTipo.putAll(deltaPorTipo);
                double residuo = redondear(movimiento - explicado);
                if (Math.abs(residuo) > TOLERANCIA) {
                    resultado.getExcepciones().add("Cuenta " + c.getCuenta() + " (" + c.getRol() + ", compartida por los tipos " + c.getTipos()
                        + "): diferencia solo contable de " + residuo + " sin libro que la explique: NO se asentó. Revisar el asiento que la movió.");
                }
            }
            for (Map.Entry<Long, Double> m : montoPorTipo.entrySet()) {
                double a = m.getValue();
                if (Math.abs(a) <= TOLERANCIA / 2) {
                    continue;
                }
                int tipo = m.getKey().intValue();
                if (devengo) {
                    // a > 0: sube la 1.4.02 (D 1.4.02 / H ingreso); a < 0: baja.
                    lineas.add(linea(plantilla17, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, tipo, Math.abs(a), a > 0,
                        "Ajuste de último recurso: 1.4.02 al libro por cuota"));
                    lineas.add(linea(plantilla17, CrdLineaAsiento.INGRESO_INTERES_ORDINARIO, tipo, Math.abs(a), a < 0,
                        "Ajuste de último recurso: ingreso por interés al libro por cuota"));
                } else {
                    // a > 0: sube la provisión (H 1.4.99.05 / D gasto); a < 0: baja.
                    lineas.add(linea(plantilla36, CrdLineaAsiento.PROVISION_INTERESES, tipo, Math.abs(a), a < 0,
                        "Ajuste de último recurso: provisión de intereses al libro por cuota"));
                    lineas.add(linea(plantilla36, CrdLineaAsiento.PROVISION_INTERESES_GASTO, tipo, Math.abs(a), a > 0,
                        "Ajuste de último recurso: gasto de provisión al libro por cuota"));
                }
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
            + (asiento != null ? "; asiento " + asiento.getCodigo() : "; sin asiento (la contabilidad ya coincidía con el libro corregido o la diferencia es solo contable de una cuenta compartida)") + ".");
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
        // Por grupo de cuentas: 1.4.02 − 1.4.99.05 al día 1 contra el devengo del ④ de los tipos del grupo.
        for (ResultadoControlProvision.Grupo g : resultado.getPorGrupo()) {
            ResultadoControlProvision.Dia1 d = new ResultadoControlProvision.Dia1();
            d.setTipos(new ArrayList<>(g.getTipos()));
            double saldos = 0.0;
            for (String cuenta : g.getCuentas1402()) {
                saldos += saldoDeudor(idEmpresa, cuenta, fechaDia1);
            }
            for (String cuenta : g.getCuentas149905()) {
                saldos += saldoDeudor(idEmpresa, cuenta, fechaDia1);
            }
            double devengoGrupo = 0.0;
            for (Long tipo : g.getTipos()) {
                devengoGrupo += devengo.getOrDefault(tipo, 0.0);
            }
            d.setDiferenciaContable(redondear(saldos));
            d.setDevengoDia1(redondear(devengoGrupo));
            d.setDiferencia(redondear(d.getDiferenciaContable() - d.getDevengoDia1()));
            resultado.getDia1().add(d);
        }
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    /** Registra una excepción por tipo (libro), por cuenta y por grupo con su causa probable. Orientativa: el cuadre la confirma con el SQL de control. */
    private void agregaExcepciones(ResultadoControlProvision resultado) {
        for (ResultadoControlProvision.Tipo t : resultado.getPorTipo()) {
            if (!t.isCuadra()) {
                resultado.getExcepciones().add("Tipo " + t.getIdTipoPrestamo() + " (solo libro): devengo de interés " + t.getLibro1402Interes()
                    + " vs provisión " + t.getLibroProvInteres() + " (dif " + t.getDifLibroInteres() + "); devengo de mora " + t.getLibro1402Mora()
                    + " vs provisión " + t.getLibroProvMora() + " (dif " + t.getDifLibroMora() + "). Causa probable: "
                    + (Math.abs(t.getDifLibroInteres()) > TOLERANCIA
                        ? "el libro de interés no iguala devengo con provisión (provisión de cuotas cerradas o negativas, o devengo sin sembrar)"
                        : "la provisión de mora no iguala la mora devengada (tope de la mora por cuota)") + ".");
            }
        }
        for (ResultadoControlProvision.Cuenta c : resultado.getPorCuenta()) {
            if (!c.isCuadra()) {
                resultado.getExcepciones().add("Cuenta " + c.getCuenta() + " (" + c.getRol() + ", tipos " + c.getTipos() + "): contable " + c.getContable()
                    + " vs libro " + c.getLibro() + " (dif " + c.getDiferencia() + "). Causa probable: "
                    + (ROL_1402.equals(c.getRol())
                        ? "la 1.4.02 contable se movió sin libro (asiento manual, ⑪ de interés anticipado, devengo o reverso sin MVIC 11/12, o libro sin semilla A2)"
                        : "la 1.4.99.05 contable se movió sin libro (asiento manual de provisión o ajuste del cuadre sin A1)") + ".");
            }
        }
        for (ResultadoControlProvision.Grupo g : resultado.getPorGrupo()) {
            if (!g.isCuadra()) {
                resultado.getExcepciones().add("Grupo de cuentas (tipos " + g.getTipos() + "; 1.4.02 " + g.getCuentas1402() + ", 1.4.99.05 "
                    + g.getCuentas149905() + "): 1.4.02 " + g.getContable1402() + " vs 1.4.99.05 " + g.getContable149905() + " (dif " + g.getDiferencia() + ").");
            }
        }
        if (Math.abs(resultado.getDifTotal()) > TOLERANCIA) {
            resultado.getExcepciones().add("Total: 1.4.02 " + resultado.getTotalContable1402() + " vs 1.4.99.05 " + resultado.getTotalContable149905()
                + " (dif " + resultado.getDifTotal() + ").");
        }
    }

    private void cierraCuenta(ResultadoControlProvision.Cuenta c, double contable, double libro) {
        c.setContable(redondear(contable));
        c.setLibro(redondear(libro));
        c.setDiferencia(redondear(c.getContable() - c.getLibro()));
        c.setCuadra(Math.abs(c.getDiferencia()) <= TOLERANCIA);
    }

    private static void unir(Map<Long, Long> raiz, List<Long> tipos) {
        for (int i = 1; i < tipos.size(); i++) {
            Long a = buscar(raiz, tipos.get(0));
            Long b = buscar(raiz, tipos.get(i));
            if (!a.equals(b)) {
                raiz.put(b, a);
            }
        }
    }

    private static Long buscar(Map<Long, Long> raiz, Long tipo) {
        Long actual = tipo;
        while (!raiz.get(actual).equals(actual)) {
            actual = raiz.get(actual);
        }
        return actual;
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
