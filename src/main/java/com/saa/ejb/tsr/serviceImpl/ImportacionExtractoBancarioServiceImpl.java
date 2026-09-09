/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.ConciliacionContableDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.ejb.tsr.dao.DetalleExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.DetalleTransitoDaoService;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.GrupoConciliacionExtractoDaoService;
import com.saa.ejb.tsr.parser.BankStatementParser;
import com.saa.ejb.tsr.parser.BankStatementParserFactory;
import com.saa.ejb.tsr.parser.ParsedStatement;
import com.saa.ejb.tsr.service.ConciliacionContableService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.ejb.tsr.service.DetalleExtractoBancarioService;
import com.saa.ejb.tsr.service.ExtractoBancarioService;
import com.saa.ejb.tsr.service.ImportacionExtractoBancarioService;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.ConciliacionContable;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.GrupoConciliacionExtracto;
import com.saa.model.tsr.ResumenImportacionExtracto;
import com.saa.rubros.ASPEstadoCargaExtracto;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoConciliacionContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de ImportacionExtractoBancarioService.</p>
 */
@Stateless
public class ImportacionExtractoBancarioServiceImpl implements ImportacionExtractoBancarioService {

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private ExtractoBancarioDaoService extractoBancarioDaoService;

    @EJB
    private ExtractoBancarioService extractoBancarioService;

    @EJB
    private DetalleExtractoBancarioService detalleExtractoBancarioService;

    @EJB
    private EmpresaService empresaService;

    @EJB
    private PeriodoService periodoService;

    @EJB
    private ControlExtractoBancarioDaoService controlExtractoBancarioDaoService;

    @EJB
    private ControlExtractoBancarioService controlExtractoBancarioService;

    @EJB
    private DetalleExtractoBancarioDaoService detalleExtractoBancarioDaoService;

    @EJB
    private GrupoConciliacionExtractoDaoService grupoConciliacionExtractoDaoService;

    @EJB
    private DetalleTransitoDaoService detalleTransitoDaoService;

    @EJB
    private ConciliacionContableDaoService conciliacionContableDaoService;

    @EJB
    private ConciliacionContableService conciliacionContableService;

    @Override
    public ResumenImportacionExtracto validar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo validar (importacion extracto) con idCuentaBancaria: "
                + idCuentaBancaria + ", idPeriodo: " + idPeriodo + ", archivo: " + nombreArchivo);

        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        Periodo periodo = obtenerPeriodoAbierto(idPeriodo);
        byte[] bytes = archivo.readAllBytes();
        String hash = calcularHashArchivo(bytes);

        ExtractoBancario existente = extractoBancarioDaoService.selectByHash(hash);

        BankStatementParser parser = BankStatementParserFactory.resolver(cuenta);
        ParsedStatement parsed = parser.parse(new ByteArrayInputStream(bytes), cuenta);

        ResumenImportacionExtracto resumen = new ResumenImportacionExtracto();
        resumen.setIdCuentaBancaria(idCuentaBancaria);
        resumen.setIdPeriodo(periodo.getCodigo());
        resumen.setNombrePeriodo(periodo.getNombre());
        resumen.setNombreBanco(cuenta.getBanco().getNombre());
        resumen.setNumeroCuenta(cuenta.getNumeroCuenta());
        resumen.setArchivoNombre(nombreArchivo);
        resumen.setFormatoDetectado(parsed.getFormatoDetectado());
        resumen.setFechaDesde(parsed.getFechaDesde());
        resumen.setFechaHasta(parsed.getFechaHasta());
        resumen.setSaldoInicial(parsed.getSaldoInicial());
        resumen.setSaldoFinal(parsed.getSaldoFinal());
        resumen.setTotalFilas(parsed.getDetalles().size());
        resumen.setTotalDebito(sumar(parsed.getDetalles(), true));
        resumen.setTotalCredito(sumar(parsed.getDetalles(), false));
        resumen.setAdvertencias(parsed.getAdvertencias());
        resumen.setArchivoYaCargado(existente != null);
        resumen.setIdExtractoExistente(existente != null ? existente.getCodigo() : null);

        List<String> fueraPeriodo = calcularTransaccionesFueraPeriodo(parsed.getDetalles(), periodo);
        resumen.setTotalTransaccionesFueraPeriodo(fueraPeriodo.size());
        resumen.setTransaccionesFueraPeriodo(fueraPeriodo);

        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ExtractoBancario confirmar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo, Long idEmpresa, String usuarioCreacion) throws Throwable {
        System.out.println("Ingresa al metodo confirmar (importacion extracto) con idCuentaBancaria: "
                + idCuentaBancaria + ", idPeriodo: " + idPeriodo + ", idEmpresa: " + idEmpresa
                + ", archivo: " + nombreArchivo);
        return ejecutarImportacion(archivo, nombreArchivo, idCuentaBancaria, idPeriodo, idEmpresa, usuarioCreacion)
                .extracto;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResumenImportacionExtracto recargar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo, Long idEmpresa, String usuarioCreacion) throws Throwable {
        System.out.println("Ingresa al metodo recargar (importacion extracto) con idCuentaBancaria: "
                + idCuentaBancaria + ", idPeriodo: " + idPeriodo + ", idEmpresa: " + idEmpresa
                + ", archivo: " + nombreArchivo);

        // Guarda 1: debe existir un extracto para poder recargarlo.
        ExtractoBancario anterior = extractoBancarioDaoService.selectByCuentaYPeriodo(idCuentaBancaria, idPeriodo);
        if (anterior == null) {
            throw new IncomeException("No existe un extracto cargado para esta cuenta y este periodo. "
                    + "Use la carga normal (confirmar), no la recarga.");
        }
        Long idExtractoAnterior = anterior.getCodigo();

        List<DetalleExtractoBancario> detallesAnteriores = detalleExtractoBancarioDaoService
                .selectByExtracto(idExtractoAnterior);
        List<Long> idsDetalleAnteriores = new ArrayList<>();
        for (DetalleExtractoBancario detalle : detallesAnteriores) {
            idsDetalleAnteriores.add(detalle.getCodigo());
        }

        // Guarda 2: ninguna fila puede estar en un grupo de conciliacion ACTIVO.
        List<Long> idsConciliados = grupoConciliacionExtractoDaoService.selectIdsEnGrupoActivo(idsDetalleAnteriores);
        if (!idsConciliados.isEmpty()) {
            throw new IncomeException("No se puede recargar: " + idsConciliados.size()
                    + " movimiento(s) de este extracto ya estan conciliados (en un grupo de conciliacion activo). "
                    + "Deshaga esas conciliaciones antes de recargar.");
        }

        // Guarda 3: ninguna fila puede estar declarada como partida en transito PENDIENTE.
        long pendientesTransito = detalleTransitoDaoService.contarPendientesPorDetalleExtracto(idsDetalleAnteriores);
        if (pendientesTransito > 0) {
            throw new IncomeException("No se puede recargar: " + pendientesTransito
                    + " movimiento(s) de este extracto estan declarados como partida en transito pendiente. "
                    + "Salde o retire esas partidas antes de recargar.");
        }

        // Guarda 4: el periodo no puede estar cerrado para conciliacion bancaria (misma
        // guarda, y el mismo mensaje, que usa confirmar()).
        obtenerPeriodoAbierto(idPeriodo);

        // Guarda 5: la conciliacion contable de esta cuenta/periodo, si existe, no puede
        // estar VERIFICADA - recargar por debajo la invalidaria en silencio.
        ConciliacionContable conciliacion = conciliacionContableDaoService
                .selectByCuentaYPeriodo(idCuentaBancaria, idPeriodo);
        if (conciliacion != null
                && Long.valueOf(EstadoConciliacionContable.VERIFICADO).equals(conciliacion.getEstadoRevision())) {
            throw new IncomeException("La conciliacion contable de esta cuenta y periodo ya esta VERIFICADA. "
                    + "Recargar el extracto invalidaria esa verificacion en silencio; revierta el estado de "
                    + "verificacion antes de recargar.");
        }

        // Red de seguridad antes de borrar el detalle: una partida de transito ya SALDADA
        // sigue existiendo y sigue siendo una FK hacia DEXB via DTCNIDEX (la guarda 3 de
        // arriba solo mira Pendiente). En la practica casi nunca se dispara: si el grupo que
        // la saldo sigue activo, la guarda 2 ya corto antes; si se deshizo,
        // reabrirPartidasTransitoDeclaradas ya la devolvio a Pendiente, que es la guarda 3. Es
        // una red para el caso raro que ninguna de las dos cubra, no el camino normal - y una
        // partida en transito se declaro ante un tercero, asi que su ancla importa: si aparece
        // una, no se borra nada, se avisa.
        long partidasTransitoHistoricas = detalleTransitoDaoService.contarPorDetalleExtracto(idsDetalleAnteriores);
        if (partidasTransitoHistoricas > 0) {
            throw new IncomeException("No se puede recargar: " + partidasTransitoHistoricas
                    + " movimiento(s) de este extracto tienen historial de partida en transito (aunque ya este "
                    + "saldada). Borrar el extracto perderia ese historial. Consulte con el administrador del sistema.");
        }

        // Los enlaces GCEX de un grupo ya DESHECHO (GRCC.estado = Inactivo) se borran en
        // cascada, no bloquean: GCEX no tiene su propio estado, asi que ese enlace existe para
        // siempre aunque el grupo este muerto (deshacerGrupo desactiva el GRCC pero nunca borra
        // sus GCEX) - si se bloqueara aca, la cuenta quedaria inutilizable para recargar apenas
        // se conciliara una vez, que es exactamente el caso que pidio el usuario (cargo mal,
        // concilio, se dio cuenta, deshizo, y quiere recargar). El GRCC inactivo en si NO se
        // toca: sigue siendo el registro de que la conciliacion existio y se deshizo. Los
        // enlaces de un grupo ACTIVO nunca llegan aca, ya cortaron en la guarda 2 de arriba.
        List<GrupoConciliacionExtracto> enlacesGrupoDeshecho = grupoConciliacionExtractoDaoService
                .selectPorDetalleExtracto(idsDetalleAnteriores);
        for (GrupoConciliacionExtracto enlace : enlacesGrupoDeshecho) {
            grupoConciliacionExtractoDaoService.remove(new GrupoConciliacionExtracto(), enlace.getCodigo());
        }

        // Borra el detalle y despues la cabecera del extracto anterior (por la FK DEXBCDGO).
        for (DetalleExtractoBancario detalle : detallesAnteriores) {
            detalleExtractoBancarioDaoService.remove(new DetalleExtractoBancario(), detalle.getCodigo());
        }
        extractoBancarioDaoService.remove(new ExtractoBancario(), idExtractoAnterior);

        // Los pendientes de la conciliacion cambiaron al borrar el detalle - recalcular antes
        // de reimportar para no dejar el tablero mintiendo aunque la reimportacion fallara.
        if (conciliacion != null) {
            conciliacionContableService.recalcularContadores(conciliacion.getCodigo());
        }

        // Reimporta con la MISMA logica que confirmar(): el hash del extracto anterior ya
        // desaparecio con el remove de arriba, asi que el mismo archivo (corregido o
        // identico) no choca con el control de duplicados.
        ResultadoImportacion resultado = ejecutarImportacion(archivo, nombreArchivo, idCuentaBancaria, idPeriodo,
                idEmpresa, usuarioCreacion);
        ExtractoBancario nuevo = resultado.extracto;

        // Traza de la recarga: EXBC no tiene una columna dedicada para "quien recargo y
        // cuando" (solo EXBCOBSR, texto libre que ya usa confirmar() para las advertencias
        // del parser) - se antepone la nota de auditoria a lo que haya quedado de eso.
        String nota = "Recarga del extracto #" + idExtractoAnterior + " (" + anterior.getArchivoNombre() + ") por "
                + usuarioCreacion + " el " + LocalDateTime.now();
        nuevo.setObservaciones(nuevo.getObservaciones() == null ? nota : nota + "; " + nuevo.getObservaciones());
        extractoBancarioService.saveSingle(nuevo);

        ResumenImportacionExtracto resumen = new ResumenImportacionExtracto();
        resumen.setIdCuentaBancaria(idCuentaBancaria);
        resumen.setIdPeriodo(nuevo.getPeriodo().getCodigo());
        resumen.setNombrePeriodo(nuevo.getPeriodo().getNombre());
        resumen.setNombreBanco(nuevo.getCuentaBancaria().getBanco().getNombre());
        resumen.setNumeroCuenta(nuevo.getCuentaBancaria().getNumeroCuenta());
        resumen.setArchivoNombre(nombreArchivo);
        resumen.setFormatoDetectado(nuevo.getFormato());
        resumen.setFechaDesde(nuevo.getFechaDesde());
        resumen.setFechaHasta(nuevo.getFechaHasta());
        resumen.setSaldoInicial(nuevo.getSaldoInicial());
        resumen.setSaldoFinal(nuevo.getSaldoFinal());
        resumen.setTotalFilas(resultado.parsed.getDetalles().size());
        resumen.setTotalDebito(sumar(resultado.parsed.getDetalles(), true));
        resumen.setTotalCredito(sumar(resultado.parsed.getDetalles(), false));
        resumen.setAdvertencias(resultado.parsed.getAdvertencias());
        resumen.setArchivoYaCargado(false);
        resumen.setIdExtractoCreado(nuevo.getCodigo());
        resumen.setIdExtractoAnterior(idExtractoAnterior);
        return resumen;
    }

    /**
     * Cuerpo real de la importacion (parseo + persistencia de EXBC/DEXB + actualizacion del
     * tablero de cumplimiento), compartido por confirmar() y recargar() para que la logica de
     * import viva en un solo lugar. No hace la guarda de "grupo activo"/"transito pendiente"
     * ni el borrado en cascada - eso es exclusivo de recargar(), antes de llamar aqui.
     */
    private ResultadoImportacion ejecutarImportacion(InputStream archivo, String nombreArchivo,
            Long idCuentaBancaria, Long idPeriodo, Long idEmpresa, String usuarioCreacion) throws Throwable {
        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        // Se revalida el estado del periodo aqui tambien (no solo en validar): pudo haberse
        // cerrado entre la previsualizacion y la confirmacion/recarga.
        Periodo periodo = obtenerPeriodoAbierto(idPeriodo);
        byte[] bytes = archivo.readAllBytes();
        String hash = calcularHashArchivo(bytes);

        ExtractoBancario existente = extractoBancarioDaoService.selectByHash(hash);
        if (existente != null) {
            throw new IncomeException("Este archivo ya fue cargado previamente (extracto #"
                    + existente.getCodigo() + ", " + existente.getArchivoNombre() + "). No se vuelve a procesar.");
        }

        Empresa empresa = empresaService.selectById(idEmpresa);

        BankStatementParser parser = BankStatementParserFactory.resolver(cuenta);
        ParsedStatement parsed = parser.parse(new ByteArrayInputStream(bytes), cuenta);

        LocalDateTime ahora = LocalDateTime.now();

        ExtractoBancario extracto = new ExtractoBancario();
        extracto.setCuentaBancaria(cuenta);
        extracto.setEmpresa(empresa);
        extracto.setPeriodo(periodo);
        extracto.setArchivoNombre(nombreArchivo);
        extracto.setArchivoHash(hash);
        extracto.setFormato(parsed.getFormatoDetectado());
        extracto.setParser(parser.getClass().getSimpleName());
        extracto.setFechaDesde(parsed.getFechaDesde());
        extracto.setFechaHasta(parsed.getFechaHasta());
        extracto.setSaldoInicial(parsed.getSaldoInicial());
        extracto.setSaldoFinal(parsed.getSaldoFinal());
        extracto.setEstadoCarga((long) ASPEstadoCargaExtracto.CARGADO);
        extracto.setObservaciones(parsed.getAdvertencias().isEmpty() ? null
                : String.join("; ", parsed.getAdvertencias()));
        extracto.setFechaCreacion(ahora);
        extracto.setUsuarioCreacion(usuarioCreacion);
        extracto.setEstado((long) Estado.ACTIVO);

        ExtractoBancario extractoGuardado = extractoBancarioService.saveSingle(extracto);

        for (DetalleExtractoBancario detalle : parsed.getDetalles()) {
            detalle.setExtractoBancario(extractoGuardado);
            detalle.setPeriodo(periodo);
            detalle.setFechaCreacion(ahora);
            detalle.setUsuarioCreacion(usuarioCreacion);
            detalle.setEstado((long) Estado.ACTIVO);
            detalleExtractoBancarioService.saveSingle(detalle);
        }

        actualizarControlSiExiste(idEmpresa, extractoGuardado);

        ResultadoImportacion resultado = new ResultadoImportacion();
        resultado.extracto = extractoGuardado;
        resultado.parsed = parsed;
        return resultado;
    }

    /**
     * Par (extracto guardado, statement parseado) que devuelve internamente
     * ejecutarImportacion() - recargar() necesita el segundo para armar el resumen sin
     * volver a parsear el archivo (ya se leyo el InputStream una sola vez).
     */
    private static final class ResultadoImportacion {
        private ExtractoBancario extracto;
        private ParsedStatement parsed;
    }

    /**
     * Si el tablero de cumplimiento ya fue generado (generarPeriodo) para el
     * mes/anio de este extracto, lo recalcula para que refleje la carga recien
     * confirmada sin esperar a que el usuario pulse "Recalcular" manualmente.
     * Se consulta la existencia del control ANTES de invocar recalcularPeriodo:
     * esa operacion lanza IncomeException (@ApplicationException(rollback=true))
     * si el periodo no ha sido generado todavia, lo cual marcaria para rollback
     * la transaccion completa de esta confirmacion (incluido el extracto recien
     * guardado) aunque se capture la excepcion aqui.
     */
    private void actualizarControlSiExiste(Long idEmpresa, ExtractoBancario extracto) {
        try {
            Periodo periodo = periodoService.recuperaByMesAnioEmpresa(idEmpresa,
                    (long) extracto.getFechaDesde().getMonthValue(), (long) extracto.getFechaDesde().getYear());
            if (periodo == null) {
                return;
            }
            ControlExtractoBancario control = controlExtractoBancarioDaoService
                    .selectByEmpresaYPeriodo(idEmpresa, periodo.getMes(), periodo.getAnio());
            if (control == null) {
                return;
            }
            controlExtractoBancarioService.recalcularPeriodo(idEmpresa, periodo.getCodigo());
        } catch (Throwable e) {
            System.out.println("No se pudo actualizar el tablero de cumplimiento tras confirmar importacion: "
                    + e.getMessage());
        }
    }

    private CuentaBancaria obtenerCuenta(Long idCuentaBancaria) throws Throwable {
        CuentaBancaria cuenta = cuentaBancariaDaoService.recuperaBancoCuenta(idCuentaBancaria);
        if (cuenta == null) {
            throw new IncomeException("No se encontro la cuenta bancaria con id " + idCuentaBancaria);
        }
        return cuenta;
    }

    /**
     * Recupera el periodo elegido por el usuario y rechaza la operacion (bloqueo
     * duro, sin excepcion posible desde esta pantalla) si ya esta CERRADO -
     * distinto de la advertencia por fecha fuera de rango, que nunca bloquea.
     */
    private Periodo obtenerPeriodoAbierto(Long idPeriodo) throws Throwable {
        if (idPeriodo == null) {
            throw new IncomeException("Debe seleccionar el periodo contable del extracto");
        }
        Periodo periodo = periodoService.selectById(idPeriodo);
        if (periodo == null) {
            throw new IncomeException("No se encontro el periodo contable con id " + idPeriodo);
        }
        if (controlExtractoBancarioService.estaCerrado(periodo.getEmpresa().getCodigo(), idPeriodo)) {
            throw new IncomeException("El periodo '" + periodo.getNombre()
                    + "' ya esta cerrado para conciliacion bancaria. No se pueden cargar nuevos extractos.");
        }
        return periodo;
    }

    /**
     * Lista, para advertencia informativa (nunca bloqueante), las filas cuya
     * fecha de transaccion cae fuera del primerDia/ultimoDia del periodo
     * elegido - caso esperado de corte de fin de mes (movimientos del ultimo
     * dia del mes anterior o los primeros del mes siguiente) que contabilidad
     * puede conciliar deliberadamente bajo el periodo actual.
     */
    private List<String> calcularTransaccionesFueraPeriodo(List<DetalleExtractoBancario> detalles, Periodo periodo) {
        List<String> fueraDeRango = new ArrayList<>();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (DetalleExtractoBancario detalle : detalles) {
            if (detalle.getFechaTransaccion() == null) {
                continue;
            }
            if (detalle.getFechaTransaccion().isBefore(periodo.getPrimerDia())
                    || detalle.getFechaTransaccion().isAfter(periodo.getUltimoDia())) {
                fueraDeRango.add(detalle.getFechaTransaccion().format(formato) + " - "
                        + (detalle.getDescripcion() != null ? detalle.getDescripcion() : ""));
            }
        }
        return fueraDeRango;
    }

    private String calcularHashArchivo(byte[] bytes) throws Throwable {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Double sumar(List<DetalleExtractoBancario> detalles, boolean debito) {
        double total = 0.0;
        for (DetalleExtractoBancario d : detalles) {
            Double valor = debito ? d.getDebito() : d.getCredito();
            if (valor != null) {
                total += valor;
            }
        }
        return total;
    }
}
