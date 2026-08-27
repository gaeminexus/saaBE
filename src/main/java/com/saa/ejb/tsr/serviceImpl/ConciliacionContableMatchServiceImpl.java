/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.saa.basico.ejb.DetalleRubroService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tsr.dao.ConciliacionContableDaoService;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionAsientoDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.ejb.tsr.dao.MovimientoBancoDaoService;
import com.saa.ejb.tsr.service.ConciliacionContableMatchService;
import com.saa.ejb.tsr.service.ConciliacionContableService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.ejb.tsr.service.DetalleExtractoBancarioService;
import com.saa.ejb.tsr.service.GrupoConciliacionAsientoService;
import com.saa.ejb.tsr.service.GrupoConciliacionContableService;
import com.saa.ejb.tsr.service.GrupoConciliacionExtractoService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.tsr.ConciliacionContable;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.GrupoConciliacionAsiento;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.model.tsr.GrupoConciliacionExtracto;
import com.saa.model.tsr.MovimientoBanco;
import com.saa.model.tsr.ResumenConciliacionCuenta;
import com.saa.model.tsr.SugerenciaConciliacionContable;
import com.saa.rubros.ASPEstadoRevisionExtracto;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoConciliacionContable;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoMovimientoConciliacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementación de ConciliacionContableMatchService. Ver javadoc de la
 * interfaz para la regla de negocio (monto Y fecha, ambas obligatorias).</p>
 */
@Stateless
public class ConciliacionContableMatchServiceImpl implements ConciliacionContableMatchService {

    /**
     * Tolerancia de redondeo monetario entre ambos lados de un grupo -
     * distinta de la tolerancia de DIAS (que sí es configurable vía rubro):
     * esta es solo para no rechazar un match por un centavo de redondeo de
     * punto flotante, no una regla de negocio.
     */
    private static final double TOLERANCIA_MONETARIA = 0.01;

    /**
     * Tope de candidatos considerados en la búsqueda de subconjunto (N:1 /
     * 1:N) del auto-match: sobre esta cantidad, la combinatoria (2^n) deja de
     * ser trivial y se prefiere dejar el caso para conciliación manual en vez
     * de arriesgar una pasada lenta.
     */
    private static final int MAX_CANDIDATOS_SUBCONJUNTO = 8;

    @EJB
    private ConciliacionContableService conciliacionContableService;

    @EJB
    private GrupoConciliacionContableService grupoConciliacionContableService;

    @EJB
    private GrupoConciliacionExtractoService grupoConciliacionExtractoService;

    @EJB
    private GrupoConciliacionAsientoService grupoConciliacionAsientoService;

    @EJB
    private GrupoConciliacionExtractoDaoService grupoConciliacionExtractoDaoService;

    @EJB
    private GrupoConciliacionAsientoDaoService grupoConciliacionAsientoDaoService;

    @EJB
    private DetalleExtractoBancarioService detalleExtractoBancarioService;

    @EJB
    private DetalleAsientoService detalleAsientoService;

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private ControlExtractoBancarioDaoService controlExtractoBancarioDaoService;

    @EJB
    private ExtractoBancarioDaoService extractoBancarioDaoService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    @EJB
    private ConciliacionContableDaoService conciliacionContableDaoService;

    @EJB
    private PeriodoService periodoService;

    @EJB
    private DetalleRubroService detalleRubroService;

    @Override
    public List<DetalleExtractoBancario> obtenerPendientesExtracto(Long idCuentaBancaria, Long idPeriodo)
            throws Throwable {
        return grupoConciliacionExtractoDaoService.selectPendientes(idCuentaBancaria, idPeriodo);
    }

    @Override
    public List<DetalleAsiento> obtenerPendientesAsiento(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        Periodo periodo = obtenerPeriodo(idPeriodo);
        return grupoConciliacionAsientoDaoService.selectPendientes(cuenta.getPlanCuenta().getCodigo(),
                periodo.getEmpresa().getCodigo(), periodo.getPrimerDia(), periodo.getUltimoDia());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GrupoConciliacionContable conciliarGrupo(Long idCuentaBancaria, Long idPeriodo,
            List<Long> idsDetalleExtracto, List<Long> idsDetalleAsiento, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo conciliarGrupo con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo + ", extracto: " + idsDetalleExtracto + ", asiento: "
                + idsDetalleAsiento);
        if (idsDetalleExtracto == null || idsDetalleExtracto.isEmpty()
                || idsDetalleAsiento == null || idsDetalleAsiento.isEmpty()) {
            throw new IncomeException(
                    "Debe seleccionar al menos una fila de cada lado (extracto y contabilidad) para conciliar");
        }

        ConciliacionContable conciliacion = conciliacionContableService.obtenerOCrear(idCuentaBancaria, idPeriodo);
        Periodo periodo = conciliacion.getPeriodo();
        if (controlExtractoBancarioService.estaCerrado(periodo.getEmpresa().getCodigo(), idPeriodo)) {
            throw new IncomeException("El periodo '" + periodo.getNombre()
                    + "' ya esta cerrado para conciliacion bancaria. No se pueden crear conciliaciones nuevas.");
        }

        List<Long> yaEnGrupoExtracto = grupoConciliacionExtractoDaoService.selectIdsEnGrupoActivo(idsDetalleExtracto);
        if (!yaEnGrupoExtracto.isEmpty()) {
            throw new IncomeException(
                    "Las siguientes filas del extracto ya estan conciliadas en otro grupo: " + yaEnGrupoExtracto);
        }
        List<Long> yaEnGrupoAsiento = grupoConciliacionAsientoDaoService.selectIdsEnGrupoActivo(idsDetalleAsiento);
        if (!yaEnGrupoAsiento.isEmpty()) {
            throw new IncomeException(
                    "Las siguientes lineas de asiento ya estan conciliadas en otro grupo: " + yaEnGrupoAsiento);
        }

        List<DetalleExtractoBancario> detallesExtracto = new ArrayList<>();
        for (Long id : idsDetalleExtracto) {
            DetalleExtractoBancario detalle = detalleExtractoBancarioService.selectById(id);
            if (detalle == null) {
                throw new IncomeException("No se encontro DetalleExtractoBancario con id " + id);
            }
            detallesExtracto.add(detalle);
        }
        List<DetalleAsiento> detallesAsiento = new ArrayList<>();
        for (Long id : idsDetalleAsiento) {
            DetalleAsiento detalle = detalleAsientoService.selectById(id);
            if (detalle == null) {
                throw new IncomeException("No se encontro DetalleAsiento con id " + id);
            }
            detallesAsiento.add(detalle);
        }

        double valorExtracto = 0.0;
        LocalDate fechaMinima = null;
        LocalDate fechaMaxima = null;
        for (DetalleExtractoBancario detalle : detallesExtracto) {
            valorExtracto += valorNeto(detalle);
            LocalDate fecha = detalle.getFechaTransaccion();
            if (fechaMinima == null || fecha.isBefore(fechaMinima)) {
                fechaMinima = fecha;
            }
            if (fechaMaxima == null || fecha.isAfter(fechaMaxima)) {
                fechaMaxima = fecha;
            }
        }
        double valorAsiento = 0.0;
        for (DetalleAsiento detalle : detallesAsiento) {
            valorAsiento += valorNeto(detalle);
            LocalDate fecha = detalle.getAsiento().getFechaAsiento();
            if (fechaMinima == null || fecha.isBefore(fechaMinima)) {
                fechaMinima = fecha;
            }
            if (fechaMaxima == null || fecha.isAfter(fechaMaxima)) {
                fechaMaxima = fecha;
            }
        }

        double diferencia = valorExtracto - valorAsiento;
        if (Math.abs(diferencia) > TOLERANCIA_MONETARIA) {
            throw new IncomeException(String.format(
                    "Los montos no cuadran: extracto %.2f vs contabilidad %.2f (diferencia %.2f)",
                    valorExtracto, valorAsiento, diferencia));
        }

        int toleranciaDias = obtenerToleranciaDias();
        long diasEntreFechas = ChronoUnit.DAYS.between(fechaMinima, fechaMaxima);
        if (diasEntreFechas > toleranciaDias) {
            throw new IncomeException("Las fechas involucradas difieren " + diasEntreFechas
                    + " dia(s) entre si, fuera de la tolerancia configurada (" + toleranciaDias + " dia(s))");
        }

        GrupoConciliacionContable grupo = new GrupoConciliacionContable();
        grupo.setConciliacionContable(conciliacion);
        grupo.setValorExtracto(valorExtracto);
        grupo.setValorAsiento(valorAsiento);
        grupo.setDiferencia(diferencia);
        grupo.setFechaMinima(fechaMinima);
        grupo.setFechaMaxima(fechaMaxima);
        grupo.setToleranciaDiasAplicada((long) toleranciaDias);
        grupo.setUsuarioConcilia(usuario);
        grupo.setFechaConciliacion(LocalDateTime.now());
        grupo.setEstado((long) Estado.ACTIVO);
        GrupoConciliacionContable grupoGuardado = grupoConciliacionContableService.saveSingle(grupo);

        for (DetalleExtractoBancario detalle : detallesExtracto) {
            GrupoConciliacionExtracto enlace = new GrupoConciliacionExtracto();
            enlace.setGrupo(grupoGuardado);
            enlace.setDetalleExtractoBancario(detalle);
            grupoConciliacionExtractoService.saveSingle(enlace);

            // Rubro 173 (ASPEstadoRevisionExtracto): la fila deja de estar
            // "Pendiente de revision" ahora que quedo conciliada. Sin esto la
            // pantalla la sigue mostrando pendiente aunque ya tenga grupo.
            detalle.setEstadoRevision(Long.valueOf(ASPEstadoRevisionExtracto.CONCILIADA));
            detalleExtractoBancarioService.saveSingle(detalle);
        }
        for (DetalleAsiento detalle : detallesAsiento) {
            GrupoConciliacionAsiento enlace = new GrupoConciliacionAsiento();
            enlace.setGrupo(grupoGuardado);
            enlace.setDetalleAsiento(detalle);
            grupoConciliacionAsientoService.saveSingle(enlace);
        }

        // Cerrar los MovimientoBanco de los asientos involucrados: pasan de
        // "en transito" a "definitivo" y quedan marcados conciliados. No hay
        // FK directo grupo->MovimientoBanco: se llega por el asiento, que si
        // tiene FK en MovimientoBanco (ver cerrarMovimientosBanco).
        Set<Long> idsAsiento = detallesAsiento.stream()
                .map(d -> d.getAsiento().getCodigo())
                .collect(Collectors.toSet());
        cerrarMovimientosBanco(idsAsiento, grupoGuardado.getFechaConciliacion());

        conciliacionContableService.recalcularContadores(conciliacion.getCodigo());
        return grupoGuardado;
    }

    /**
     * Cierra los MovimientoBanco (TSR.MVCB) de los asientos indicados: pasa
     * su tipo de "en transito" a "definitivo" (mapeo de
     * {@link TipoMovimientoConciliacion}, rubro 37) y los marca conciliados.
     * <p>
     * Un movimiento cuyo tipo actual no está en el mapeo (porque ya es
     * definitivo, o porque no se reconoce) se deja completamente intacto —
     * ni tipo, ni conciliado, ni fecha — para no pisar el cierre de un grupo
     * anterior cuando dos DetalleAsiento del mismo asiento se concilian en
     * grupos distintos. Un asiento sin ningún MovimientoBanco asociado no es
     * error: simplemente no hay nada que cerrar (p.ej. asientos manuales que
     * no pasaron por el circuito de pagos/cobros).
     */
    private void cerrarMovimientosBanco(Set<Long> idsAsiento, LocalDateTime fechaConciliacion) throws Throwable {
        for (Long idAsiento : idsAsiento) {
            List<MovimientoBanco> movimientos = movimientoBancoDaoService.selectByAsiento(idAsiento);
            for (MovimientoBanco movimiento : movimientos) {
                Long tipoDefinitivo = tipoDefinitivo(movimiento.getRubroTipoMovimientoH());
                if (tipoDefinitivo == null) {
                    continue;
                }
                movimiento.setRubroTipoMovimientoH(tipoDefinitivo);
                movimiento.setConciliado(1L);
                movimiento.setFechaConciliacion(fechaConciliacion);
                movimientoBancoDaoService.save(movimiento, movimiento.getCodigo());
            }
        }
    }

    /**
     * Revierte {@link #cerrarMovimientosBanco}: pasa el tipo "definitivo" de
     * vuelta a "en transito" y desmarca conciliado/fechaConciliacion. Mismo
     * criterio de dejar intacto lo que no está en el mapeo esperado (aquí,
     * lo que ya no está en un tipo definitivo).
     */
    private void reabrirMovimientosBanco(Set<Long> idsAsiento) throws Throwable {
        for (Long idAsiento : idsAsiento) {
            List<MovimientoBanco> movimientos = movimientoBancoDaoService.selectByAsiento(idAsiento);
            for (MovimientoBanco movimiento : movimientos) {
                Long tipoTransito = tipoTransito(movimiento.getRubroTipoMovimientoH());
                if (tipoTransito == null) {
                    continue;
                }
                movimiento.setRubroTipoMovimientoH(tipoTransito);
                movimiento.setConciliado(0L);
                movimiento.setFechaConciliacion(null);
                movimientoBancoDaoService.save(movimiento, movimiento.getCodigo());
            }
        }
    }

    /**
     * Tránsito -> definitivo, tabla completa del rubro 37
     * (TipoMovimientoConciliacion). El motor legado
     * (MovimientoBancoServiceImpl.actualizaEstadoMovimiento) tenía esta misma
     * tabla pero con un bug: el UPDATE grababa siempre Estado.ACTIVO en la
     * columna del tipo en vez del tipo calculado. No se reutiliza ese método.
     * @return : el tipo definitivo correspondiente, o null si tipoActual no
     *           es uno de los seis tipos "en transito" del rubro (ya es
     *           definitivo, o es un valor no reconocido) — el llamador debe
     *           dejarlo tal cual.
     */
    private Long tipoDefinitivo(Long tipoActual) {
        if (tipoActual == null) {
            return null;
        }
        switch (tipoActual.intValue()) {
            case TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO:
                return (long) TipoMovimientoConciliacion.DEPOSITO;
            case TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS:
                return (long) TipoMovimientoConciliacion.CHEQUE_COBRADO;
            case TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO:
                return (long) TipoMovimientoConciliacion.DEBITO_BANCARIO;
            case TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO:
                return (long) TipoMovimientoConciliacion.CREDITO_BANCARIO;
            case TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO:
                return (long) TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS;
            case TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO:
                return (long) TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS;
            default:
                return null;
        }
    }

    /**
     * Inverso de {@link #tipoDefinitivo(Long)}: definitivo -> en transito.
     */
    private Long tipoTransito(Long tipoActual) {
        if (tipoActual == null) {
            return null;
        }
        switch (tipoActual.intValue()) {
            case TipoMovimientoConciliacion.DEPOSITO:
                return (long) TipoMovimientoConciliacion.DEPOSITO_EN_TRANSITO;
            case TipoMovimientoConciliacion.CHEQUE_COBRADO:
                return (long) TipoMovimientoConciliacion.CHEQUES_GIRADOS_Y_NO_COBRADOS;
            case TipoMovimientoConciliacion.DEBITO_BANCARIO:
                return (long) TipoMovimientoConciliacion.DEBITO_BANCARIO_EN_TRANSITO;
            case TipoMovimientoConciliacion.CREDITO_BANCARIO:
                return (long) TipoMovimientoConciliacion.CREDITO_BANCARIO_EN_TRANSITO;
            case TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS:
                return (long) TipoMovimientoConciliacion.TRANSFERENCIAS_DEBITOS_EN_TRANSITO;
            case TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS:
                return (long) TipoMovimientoConciliacion.TRANSFERENCIAS_CREDITOS_EN_TRANSITO;
            default:
                return null;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deshacerGrupo(Long idGrupo, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo deshacerGrupo con idGrupo: " + idGrupo + ", usuario: " + usuario);
        GrupoConciliacionContable grupo = grupoConciliacionContableService.selectById(idGrupo);
        if (grupo == null) {
            throw new IncomeException("No se encontro el grupo de conciliacion con id " + idGrupo);
        }
        if (!Long.valueOf(Estado.ACTIVO).equals(grupo.getEstado())) {
            throw new IncomeException("Este grupo ya estaba deshecho");
        }
        Periodo periodo = grupo.getConciliacionContable().getPeriodo();
        if (controlExtractoBancarioService.estaCerrado(periodo.getEmpresa().getCodigo(), periodo.getCodigo())) {
            throw new IncomeException("El periodo '" + periodo.getNombre()
                    + "' ya esta cerrado para conciliacion bancaria. No se pueden deshacer conciliaciones.");
        }
        // Revertir el cierre de los MovimientoBanco (simétrico de conciliarGrupo)
        // antes de desactivar el grupo, mientras todavía se puede resolver
        // qué asientos y qué filas de extracto participaban en él.
        List<GrupoConciliacionAsiento> enlacesAsiento = grupoConciliacionAsientoDaoService.selectByGrupo(idGrupo);
        Set<Long> idsAsiento = enlacesAsiento.stream()
                .map(e -> e.getDetalleAsiento().getAsiento().getCodigo())
                .collect(Collectors.toSet());
        reabrirMovimientosBanco(idsAsiento);

        // Rubro 173: de vuelta a "Pendiente de revision".
        List<GrupoConciliacionExtracto> enlacesExtracto = grupoConciliacionExtractoDaoService.selectByGrupo(idGrupo);
        for (GrupoConciliacionExtracto enlace : enlacesExtracto) {
            DetalleExtractoBancario detalle = enlace.getDetalleExtractoBancario();
            detalle.setEstadoRevision(Long.valueOf(ASPEstadoRevisionExtracto.PENDIENTE_REVISION));
            detalleExtractoBancarioService.saveSingle(detalle);
        }

        grupo.setEstado((long) Estado.INACTIVO);
        grupoConciliacionContableService.saveSingle(grupo);
        conciliacionContableService.recalcularContadores(grupo.getConciliacionContable().getCodigo());
    }

    @Override
    public List<SugerenciaConciliacionContable> sugerirCoincidencias(Long idCuentaBancaria, Long idPeriodo)
            throws Throwable {
        System.out.println("Ingresa al metodo sugerirCoincidencias con idCuentaBancaria: " + idCuentaBancaria
                + ", idPeriodo: " + idPeriodo);
        List<DetalleExtractoBancario> pendientesExtracto = obtenerPendientesExtracto(idCuentaBancaria, idPeriodo);
        List<DetalleAsiento> pendientesAsiento = obtenerPendientesAsiento(idCuentaBancaria, idPeriodo);
        int toleranciaDias = obtenerToleranciaDias();

        List<SugerenciaConciliacionContable> sugerencias = new ArrayList<>();
        Set<Long> extractoUsados = new HashSet<>();
        Set<Long> asientoUsados = new HashSet<>();

        // Pase 1: coincidencia exacta 1:1.
        for (DetalleExtractoBancario ex : pendientesExtracto) {
            if (extractoUsados.contains(ex.getCodigo())) {
                continue;
            }
            double valorEx = valorNeto(ex);
            for (DetalleAsiento as : pendientesAsiento) {
                if (asientoUsados.contains(as.getCodigo())) {
                    continue;
                }
                if (Math.abs(valorEx - valorNeto(as)) <= TOLERANCIA_MONETARIA
                        && diasEntre(ex.getFechaTransaccion(), as.getAsiento().getFechaAsiento()) <= toleranciaDias) {
                    sugerencias.add(construirSugerencia(List.of(ex), List.of(as)));
                    extractoUsados.add(ex.getCodigo());
                    asientoUsados.add(as.getCodigo());
                    break;
                }
            }
        }

        // Pase 2: N:1 - varias filas del extracto suman una sola linea contable.
        for (DetalleAsiento as : pendientesAsiento) {
            if (asientoUsados.contains(as.getCodigo())) {
                continue;
            }
            List<DetalleExtractoBancario> candidatos = pendientesExtracto.stream()
                    .filter(ex -> !extractoUsados.contains(ex.getCodigo()))
                    .filter(ex -> diasEntre(ex.getFechaTransaccion(), as.getAsiento().getFechaAsiento())
                            <= toleranciaDias)
                    .collect(Collectors.toList());
            if (candidatos.isEmpty() || candidatos.size() > MAX_CANDIDATOS_SUBCONJUNTO) {
                continue;
            }
            List<DetalleExtractoBancario> subconjunto = buscarSubconjuntoExtracto(candidatos, valorNeto(as));
            if (subconjunto != null) {
                sugerencias.add(construirSugerencia(subconjunto, List.of(as)));
                asientoUsados.add(as.getCodigo());
                subconjunto.forEach(ex -> extractoUsados.add(ex.getCodigo()));
            }
        }

        // Pase 3: 1:N - una fila del extracto se reparte en varias lineas contables.
        for (DetalleExtractoBancario ex : pendientesExtracto) {
            if (extractoUsados.contains(ex.getCodigo())) {
                continue;
            }
            List<DetalleAsiento> candidatos = pendientesAsiento.stream()
                    .filter(as -> !asientoUsados.contains(as.getCodigo()))
                    .filter(as -> diasEntre(ex.getFechaTransaccion(), as.getAsiento().getFechaAsiento())
                            <= toleranciaDias)
                    .collect(Collectors.toList());
            if (candidatos.isEmpty() || candidatos.size() > MAX_CANDIDATOS_SUBCONJUNTO) {
                continue;
            }
            List<DetalleAsiento> subconjunto = buscarSubconjuntoAsiento(candidatos, valorNeto(ex));
            if (subconjunto != null) {
                sugerencias.add(construirSugerencia(List.of(ex), subconjunto));
                extractoUsados.add(ex.getCodigo());
                subconjunto.forEach(as -> asientoUsados.add(as.getCodigo()));
            }
        }

        return sugerencias;
    }

    /**
     * Busqueda por fuerza bruta (2^n, n acotado por MAX_CANDIDATOS_SUBCONJUNTO)
     * de un subconjunto de candidatos cuya suma cuadre con valorObjetivo.
     * Devuelve el primer subconjunto no vacio encontrado, o null si ninguno cuadra.
     */
    private List<DetalleExtractoBancario> buscarSubconjuntoExtracto(List<DetalleExtractoBancario> candidatos,
            double valorObjetivo) {
        int n = candidatos.size();
        for (int mascara = 1; mascara < (1 << n); mascara++) {
            double suma = 0.0;
            for (int i = 0; i < n; i++) {
                if ((mascara & (1 << i)) != 0) {
                    suma += valorNeto(candidatos.get(i));
                }
            }
            if (Math.abs(suma - valorObjetivo) <= TOLERANCIA_MONETARIA) {
                List<DetalleExtractoBancario> subconjunto = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if ((mascara & (1 << i)) != 0) {
                        subconjunto.add(candidatos.get(i));
                    }
                }
                return subconjunto;
            }
        }
        return null;
    }

    private List<DetalleAsiento> buscarSubconjuntoAsiento(List<DetalleAsiento> candidatos, double valorObjetivo) {
        int n = candidatos.size();
        for (int mascara = 1; mascara < (1 << n); mascara++) {
            double suma = 0.0;
            for (int i = 0; i < n; i++) {
                if ((mascara & (1 << i)) != 0) {
                    suma += valorNeto(candidatos.get(i));
                }
            }
            if (Math.abs(suma - valorObjetivo) <= TOLERANCIA_MONETARIA) {
                List<DetalleAsiento> subconjunto = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if ((mascara & (1 << i)) != 0) {
                        subconjunto.add(candidatos.get(i));
                    }
                }
                return subconjunto;
            }
        }
        return null;
    }

    private SugerenciaConciliacionContable construirSugerencia(List<DetalleExtractoBancario> detallesExtracto,
            List<DetalleAsiento> detallesAsiento) {
        SugerenciaConciliacionContable sugerencia = new SugerenciaConciliacionContable();
        sugerencia.setIdsDetalleExtracto(detallesExtracto.stream()
                .map(DetalleExtractoBancario::getCodigo).collect(Collectors.toList()));
        sugerencia.setIdsDetalleAsiento(detallesAsiento.stream()
                .map(DetalleAsiento::getCodigo).collect(Collectors.toList()));
        sugerencia.setValorExtracto(detallesExtracto.stream().mapToDouble(this::valorNeto).sum());
        sugerencia.setValorAsiento(detallesAsiento.stream().mapToDouble(this::valorNeto).sum());

        LocalDate fechaMinima = null;
        LocalDate fechaMaxima = null;
        for (DetalleExtractoBancario d : detallesExtracto) {
            fechaMinima = menorFecha(fechaMinima, d.getFechaTransaccion());
            fechaMaxima = mayorFecha(fechaMaxima, d.getFechaTransaccion());
        }
        for (DetalleAsiento d : detallesAsiento) {
            LocalDate fecha = d.getAsiento().getFechaAsiento();
            fechaMinima = menorFecha(fechaMinima, fecha);
            fechaMaxima = mayorFecha(fechaMaxima, fecha);
        }
        sugerencia.setFechaMinima(fechaMinima);
        sugerencia.setFechaMaxima(fechaMaxima);
        sugerencia.setDescripcionResumen(detallesExtracto.get(0).getDescripcion());
        return sugerencia;
    }

    private LocalDate menorFecha(LocalDate actual, LocalDate nueva) {
        return (actual == null || nueva.isBefore(actual)) ? nueva : actual;
    }

    private LocalDate mayorFecha(LocalDate actual, LocalDate nueva) {
        return (actual == null || nueva.isAfter(actual)) ? nueva : actual;
    }

    private long diasEntre(LocalDate a, LocalDate b) {
        return Math.abs(ChronoUnit.DAYS.between(a, b));
    }

    /**
     * Valor neto (positivo = entrada de dinero) de una fila de extracto:
     * credito - debito.
     */
    private double valorNeto(DetalleExtractoBancario detalle) {
        double credito = detalle.getCredito() != null ? detalle.getCredito() : 0.0;
        double debito = detalle.getDebito() != null ? detalle.getDebito() : 0.0;
        return credito - debito;
    }

    /**
     * Valor neto (positivo = entrada de dinero) de una linea de asiento sobre
     * la cuenta contable propia del banco: debe - haber (un debito incrementa
     * el saldo de una cuenta de activo como es la cuenta bancaria - mismo
     * criterio usado por DebitoCreditoServiceImpl al generar el asiento de un
     * credito/debito bancario manual).
     */
    private double valorNeto(DetalleAsiento detalle) {
        double debe = detalle.getValorDebe() != null ? detalle.getValorDebe() : 0.0;
        double haber = detalle.getValorHaber() != null ? detalle.getValorHaber() : 0.0;
        return debe - haber;
    }

    private int obtenerToleranciaDias() throws Throwable {
        Double valor = detalleRubroService.selectValorNumericoByRubAltDetAlt(
                Rubros.ASP_TOLERANCIA_DIAS_CONCILIACION_CONTABLE, 1);
        return valor != null ? valor.intValue() : 0;
    }

    private CuentaBancaria obtenerCuenta(Long idCuentaBancaria) throws Throwable {
        CuentaBancaria cuenta = cuentaBancariaDaoService.recuperaBancoCuenta(idCuentaBancaria);
        if (cuenta == null) {
            throw new IncomeException("No se encontro la cuenta bancaria con id " + idCuentaBancaria);
        }
        return cuenta;
    }

    private Periodo obtenerPeriodo(Long idPeriodo) throws Throwable {
        Periodo periodo = periodoService.selectById(idPeriodo);
        if (periodo == null) {
            throw new IncomeException("No se encontro el periodo contable con id " + idPeriodo);
        }
        return periodo;
    }

    @Override
    public List<ResumenConciliacionCuenta> resumenPorPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo resumenPorPeriodo con idEmpresa: " + idEmpresa
                + ", idPeriodo: " + idPeriodo);
        Periodo periodo = obtenerPeriodo(idPeriodo);
        List<CuentaBancaria> cuentas = controlExtractoBancarioDaoService.selectCuentasBancariasActivas(idEmpresa);
        List<Long> idsCuenta = cuentas.stream().map(CuentaBancaria::getCodigo).toList();

        // Mismo chequeo de cobertura que usa el Tablero de Cumplimiento
        // (ControlExtractoBancarioServiceImpl.detalleCuentas) - sin esto,
        // "0 pendientes" es ambiguo entre "nunca se cargo el extracto" y
        // "ya se concilio todo".
        List<Long> conCobertura = extractoBancarioDaoService
                .selectCuentasConCobertura(idsCuenta, periodo.getPrimerDia(), periodo.getUltimoDia());

        List<ResumenConciliacionCuenta> resultado = new ArrayList<>();
        for (CuentaBancaria cuenta : cuentas) {
            ResumenConciliacionCuenta fila = new ResumenConciliacionCuenta();
            fila.setCuentaBancaria(cuenta);
            fila.setExtractoCargado(conCobertura.contains(cuenta.getCodigo()));

            ConciliacionContable existente = conciliacionContableDaoService
                    .selectByCuentaYPeriodo(cuenta.getCodigo(), idPeriodo);
            if (existente != null) {
                fila.setIdConciliacionContable(existente.getCodigo());
                fila.setEstadoRevision(existente.getEstadoRevision());
                fila.setUsuarioVerifica(existente.getUsuarioVerifica());
                fila.setFechaVerificacion(existente.getFechaVerificacion());
            }

            fila.setTotalPendientesExtracto(
                    grupoConciliacionExtractoDaoService.contarPendientes(cuenta.getCodigo(), idPeriodo));
            fila.setTotalPendientesAsiento(grupoConciliacionAsientoDaoService.contarPendientes(
                    cuenta.getPlanCuenta().getCodigo(), idEmpresa, periodo.getPrimerDia(), periodo.getUltimoDia()));

            resultado.add(fila);
        }
        return resultado;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void cerrarMes(Long idEmpresa, Long idPeriodo, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo cerrarMes con idEmpresa: " + idEmpresa + ", idPeriodo: " + idPeriodo
                + ", usuario: " + usuario);
        List<ResumenConciliacionCuenta> resumen = resumenPorPeriodo(idEmpresa, idPeriodo);
        if (resumen.isEmpty()) {
            throw new IncomeException("No hay cuentas bancarias activas para verificar en esta empresa");
        }
        List<String> cuentasNoVerificadas = new ArrayList<>();
        for (ResumenConciliacionCuenta fila : resumen) {
            if (!Long.valueOf(EstadoConciliacionContable.VERIFICADO).equals(fila.getEstadoRevision())) {
                CuentaBancaria cuenta = fila.getCuentaBancaria();
                cuentasNoVerificadas.add(cuenta.getBanco().getNombre() + " - " + cuenta.getNumeroCuenta());
            }
        }
        if (!cuentasNoVerificadas.isEmpty()) {
            throw new IncomeException(
                    "No se puede cerrar el mes: las siguientes cuentas todavia no estan verificadas: "
                            + String.join(", ", cuentasNoVerificadas));
        }
        controlExtractoBancarioService.cerrarPeriodo(idEmpresa, idPeriodo, usuario);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void reabrirMes(Long idEmpresa, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo reabrirMes con idEmpresa: " + idEmpresa + ", idPeriodo: " + idPeriodo);
        controlExtractoBancarioService.reabrirPeriodo(idEmpresa, idPeriodo);
    }
}
