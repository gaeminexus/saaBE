/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.CuentaBancariaDaoService;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.parser.BankStatementParser;
import com.saa.ejb.tsr.parser.BankStatementParserFactory;
import com.saa.ejb.tsr.parser.ParsedStatement;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.ejb.tsr.service.DetalleExtractoBancarioService;
import com.saa.ejb.tsr.service.ExtractoBancarioService;
import com.saa.ejb.tsr.service.ImportacionExtractoBancarioService;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.ResumenImportacionExtracto;
import com.saa.rubros.ASPEstadoCargaExtracto;
import com.saa.rubros.Estado;

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

        CuentaBancaria cuenta = obtenerCuenta(idCuentaBancaria);
        // Se revalida el estado del periodo aqui tambien (no solo en validar):
        // pudo haberse cerrado entre la previsualizacion y la confirmacion.
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

        return extractoGuardado;
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
