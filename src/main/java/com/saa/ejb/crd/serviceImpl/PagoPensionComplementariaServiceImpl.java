package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.PagoPensionComplementariaDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PagoPensionComplementariaService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.ejb.crd.service.dto.DesgloseAporte;
import com.saa.ejb.crd.service.dto.DetallePagoPension;
import com.saa.ejb.crd.service.dto.DetallePrevisualizacionJubilado;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoPagoConAportes;
import com.saa.ejb.crd.service.dto.ResultadoPrevisualizacionCorrida;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.ejb.crd.service.dto.SolicitudPagoConAportes;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.PagoPensionComplementaria;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAporte;
import com.saa.model.crd.ValorPagoPensionComplementaria;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.CrdTipoMovimientoAporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPagoPensionComplementaria;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @see PagoPensionComplementariaService
 * @author Sistema SAA
 * @since 2026-08-31
 */
@Stateless
public class PagoPensionComplementariaServiceImpl implements PagoPensionComplementariaService {

    /** CRD.TPAP.TPAPCDGO — pensión complementaria (J6, confirmado por el usuario). */
    private static final long TIPO_APORTE_PENSION_COMPLEMENTARIA = 23L;

    private static final double TOLERANCIA = 0.01;

    @EJB
    private PagoPensionComplementariaDaoService pagoPensionDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private ValorPagoPensionComplementariaService valorPagoPensionComplementariaService;

    @EJB
    private com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    /** Para {@code obtenerCertificado} — regla del certificado bancario, §6 del contrato. */
    @EJB
    private com.saa.ejb.crd.service.CuentaBancariaParticipeService cuentaBancariaParticipeService;

    @EJB
    private SaldoAporteService saldoAporteService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    /** crd → cxp: dirección permitida. */
    @EJB
    private PagoProgramadoService pagoProgramadoService;

    /** crd → cxp: dirección permitida. Solo lectura del estado del pago. */
    @EJB
    private PagoProgramadoDaoService pagoProgramadoDaoService;

    /**
     * PLAN-PAGO-JUBILADOS.md §3: el cruce contra préstamos se ORQUESTA acá, no se reimplementa
     * — {@code pagarConAportes} ya está en producción y desde la fase 3 de la carga Petro
     * (b642be1) además cobra mora vía el motor. Se llama, no se modifica (§6 del plan).
     */
    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    /** Solo para calcular la deuda pendiente ANTES de decidir cuánto cruzar (§3 del plan). */
    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    /** Para la deuda EXIGIBLE del retroactivo (§3bis del plan) — cuotas, no el pendiente total. */
    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    /**
     * Resuelve el nombre de usuario (String, lo que manda el frontend) al {@code Long} que pide
     * {@code PagoProgramadoService.registrarPagoDeOrigenExterno} — mismo patrón que
     * {@code CobroPetroContableServiceImpl}.
     */
    @EJB
    private com.saa.basico.ejb.UsuarioDaoService usuarioDaoService;

    /**
     * Auto-inyección: permite que el lote invoque {@code generarPagoIndividual}/
     * {@code sincronizarPago} a TRAVÉS del proxy EJB, para que cada jubilado corra en su
     * propia transacción (REQUIRES_NEW) — mismo motivo que
     * {@code DevolucionAporteServiceImpl.self}: una llamada directa se saltearía el
     * interceptor y todo el lote quedaría en una sola transacción.
     */
    @EJB
    private PagoPensionComplementariaService self;

    // ========================================================================
    // EntityService
    // ========================================================================

    @Override
    public PagoPensionComplementaria selectById(Long id) throws Throwable {
        return pagoPensionDaoService.selectById(id, NombreEntidadesCredito.PAGO_PENSION_COMPLEMENTARIA);
    }

    @Override
    public List<PagoPensionComplementaria> selectAll() throws Throwable {
        return pagoPensionDaoService.selectAll(NombreEntidadesCredito.PAGO_PENSION_COMPLEMENTARIA);
    }

    @Override
    public List<PagoPensionComplementaria> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return pagoPensionDaoService.selectByCriteria(datos, NombreEntidadesCredito.PAGO_PENSION_COMPLEMENTARIA);
    }

    @Override
    public PagoPensionComplementaria saveSingle(PagoPensionComplementaria registro) throws Throwable {
        return pagoPensionDaoService.save(registro, registro.getCodigo());
    }

    @Override
    public void save(List<PagoPensionComplementaria> lista) throws Throwable {
        for (PagoPensionComplementaria registro : lista) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        PagoPensionComplementaria entidad = new PagoPensionComplementaria();
        for (Long id : ids) {
            pagoPensionDaoService.remove(entidad, id);
        }
    }

    @Override
    public List<PagoPensionComplementaria> listarPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("PagoPensionComplementariaService.listarPorEntidad - Entidad: " + idEntidad);
        if (idEntidad == null) {
            throw new IncomeException("idEntidad es obligatorio");
        }
        Entidad entidad = entidadDaoService.find(new Entidad(), idEntidad);
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe " + idEntidad);
        }
        List<PagoPensionComplementaria> pagos = pagoPensionDaoService.selectByEntidad(idEntidad);
        return pagos != null ? pagos : new java.util.ArrayList<>();
    }

    @Override
    public List<PagoPensionComplementaria> listarPorPeriodo(Integer anio, Integer mes) throws Throwable {
        System.out.println("PagoPensionComplementariaService.listarPorPeriodo - Periodo: " + mes + "/" + anio);
        List<PagoPensionComplementaria> pagos = pagoPensionDaoService.selectByPeriodo(
            anio != null ? anio.longValue() : null, mes != null ? mes.longValue() : null);
        return pagos != null ? pagos : new java.util.ArrayList<>();
    }

    // ========================================================================
    // Previsualización (§4bis del contrato) — CERO escritura, ver el JavaDoc de la interfaz
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResultadoPrevisualizacionCorrida previsualizarCorrida(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable {
        System.out.println("PagoPensionComplementariaService.previsualizarCorrida - Periodo: " + mes + "/" + anio);

        if (idEmpresa == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se generaría la orden de pago.");
        }
        if (anio == null || mes == null || mes < 1 || mes > 12) {
            throw new IncomeException("Debe indicar un año y un mes (1-12) válidos.");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }

        LocalDate finDeMes = YearMonth.of(anio, mes).atEndOfMonth();
        YearMonth corrida = YearMonth.of(anio, mes);

        ResultadoPrevisualizacionCorrida resumen = new ResultadoPrevisualizacionCorrida();
        resumen.setAnio(anio);
        resumen.setMes(mes);

        List<Entidad> jubilados = entidadDaoService.selectByIdEstado(
            Long.valueOf(EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO));

        double totalACruzar = 0.0, totalADinero = 0.0, totalSeguroGeneral = 0.0;
        int aptos = 0;

        if (jubilados != null) {
            for (Entidad jubilado : jubilados) {
                resumen.setEvaluados(resumen.getEvaluados() + 1);
                DetallePrevisualizacionJubilado fila;
                try {
                    fila = previsualizarJubilado(jubilado, finDeMes, corrida);
                } catch (Throwable e) {
                    // Ítem 1: NO escribe nada — pero SÍ puede fallar por dato roto (VPPC
                    // duplicada, TPDJ mal configurado). Igual que generarPagosDelMes, una
                    // falla de UN jubilado no aborta la previsualización del resto.
                    fila = new DetallePrevisualizacionJubilado();
                    fila.setIdEntidad(jubilado.getCodigo());
                    fila.setNombre(jubilado.getRazonSocial());
                    fila.setApto(false);
                    fila.setMotivoBloqueo(e.getMessage());
                }
                resumen.getDetalle().add(fila);
                if (fila.isApto()) {
                    aptos++;
                    totalACruzar = redondear(totalACruzar + fila.getMontoACruzar());
                    totalADinero = redondear(totalADinero + fila.getMontoADinero());
                    // §4bis: agregado del seguro médico — mismo criterio NOMINAL que la fila
                    // (ver DetallePrevisualizacionJubilado.totalSeguro).
                    totalSeguroGeneral = redondear(totalSeguroGeneral + fila.getTotalSeguro());
                }
            }
        }

        resumen.setAptos(aptos);
        resumen.setBloqueados(resumen.getEvaluados() - aptos);
        resumen.setTotalACruzarPrestamos(totalACruzar);
        resumen.setTotalADinero(totalADinero);
        resumen.setTotalGeneral(redondear(totalACruzar + totalADinero));
        resumen.setTotalSeguroGeneral(totalSeguroGeneral);

        System.out.println("PREVISUALIZACIÓN TERMINADA - Evaluados: " + resumen.getEvaluados()
            + " - Aptos: " + resumen.getAptos() + " - Bloqueados: " + resumen.getBloqueados()
            + " - A cruzar: $" + resumen.getTotalACruzarPrestamos()
            + " - A dinero: $" + resumen.getTotalADinero());

        return resumen;
    }

    /**
     * El tope de UN jubilado, calculado — NUNCA aplicado. Reusa {@link #resolverAnclaRetroactivo}
     * y {@link #calcularDeudaExigiblePrestamo}, los mismos helpers que la corrida real, para que
     * "meses adeudados" y "deuda exigible" nunca puedan desincronizarse entre las dos.
     *
     * D4 (PLAN-PAGO-RETROACTIVO-JUBILADOS.md, ampliación 2026-09-04): aplica a TODO jubilado
     * con meses adeudados, tenga o no préstamo vigente. Sin préstamo vigente,
     * {@code deudaExigibleTotal} da 0 y la MISMA fórmula {@code min(pensiones acumuladas,
     * deudaExigibleTotal, saldo)} fuerza {@code montoACruzar = 0} sola — no hace falta una
     * rama aparte para ese caso.
     *
     * ⚠️ La fórmula final (el {@code min(...)}) SÍ está escrita dos veces: acá se aplica UNA vez
     * en agregado (estimación simple, {@code API-PAGO-PENSION-COMPLEMENTARIA.md §4bis}), mientras
     * que {@link #generarMesesRetroactivos} la aplica mes a mes con tope por préstamo individual
     * (§3bis, para no pre-pagar cuotas futuras de un préstamo específico). Son dos formas
     * legítimamente distintas de la misma idea, no una copia descuidada — reportado al árbitro.
     */
    private DetallePrevisualizacionJubilado previsualizarJubilado(Entidad jubilado, LocalDate finDeMes,
            YearMonth corrida) throws Throwable {
        Long idEntidad = jubilado.getCodigo();
        DetallePrevisualizacionJubilado fila = new DetallePrevisualizacionJubilado();
        fila.setIdEntidad(idEntidad);
        fila.setNombre(jubilado.getRazonSocial());

        List<ValorPagoPensionComplementaria> configuraciones =
            valorPagoPensionComplementariaService.selectByEntidad(idEntidad);
        ValorPagoPensionComplementaria vppc = unicaActiva(configuraciones, idEntidad);
        if (vppc == null) {
            fila.setApto(false);
            fila.setMotivoBloqueo(ERR_SIN_VALOR_PENSION + ": sin configuración de pensión"
                + " complementaria (VPPC) activa.");
            return fila;
        }
        double valorTotal = redondear(vppc.getValorPagar() != null ? vppc.getValorPagar() : 0.0);
        if (valorTotal <= 0.01) {
            fila.setApto(false);
            fila.setMotivoBloqueo(ERR_SIN_VALOR_PENSION + ": VPPC " + vppc.getCodigo()
                + " con valorPagar $0.");
            return fila;
        }
        // §4bis del contrato: valorPagar YA INCLUYE el seguro — se resta, no se suma (mismo
        // criterio que generarPagoIndividual). Informativo aunque esta fila termine bloqueada.
        double valorSeguroMensual = redondear(vppc.getValorSeguro() != null ? vppc.getValorSeguro() : 0.0);
        double valorPensionMensual = redondear(valorTotal - valorSeguroMensual);
        fila.setValorPensionMensual(valorPensionMensual);
        fila.setValorSeguroMensual(valorSeguroMensual);

        // Mismo helper que generarPagoIndividual: puede propagar ERR_CERTIFICADO_NO_VERIFICABLE
        // (catálogo TPDJ roto) — a diferencia de la corrida real, acá NO aborta toda la
        // previsualización, solo bloquea esta fila; el resto de jubilados sigue evaluándose
        // (lo captura el catch de previsualizarCorrida).
        boolean tieneCertificado = resolverCuentaSalida(idEntidad) != null;
        fila.setTieneCertificado(tieneCertificado);

        double saldo = Math.max(0.0, saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA));

        LocalDate ancla = resolverAnclaRetroactivo(idEntidad);
        if (ancla == null) {
            fila.setApto(false);
            fila.setParticipacion("BLOQUEADO");
            fila.setMotivoBloqueo("SIN_ANCLA: no tiene ningún movimiento negativo de pensión"
                + " complementaria ni un movimiento de jubilación registrado.");
            return fila;
        }

        YearMonth desde = YearMonth.from(ancla).plusMonths(1);
        if (desde.isAfter(corrida)) {
            // Al día: apto, cero meses, nada que cruzar ni pagar — no es un bloqueo.
            fila.setMesesAdeudados(0);
            fila.setApto(true);
            return fila;
        }
        int mesesAdeudados = (int) (ChronoUnit.MONTHS.between(desde, corrida) + 1);
        fila.setMesesAdeudados(mesesAdeudados);

        // Deuda EXIGIBLE (ítem 3/§3bis): mismo helper por préstamo vigente que usa la corrida
        // real — nunca el pendiente total, que pre-pagaría cuotas futuras. 0 si no hay ningún
        // préstamo vigente (D4: eso ya no es un caso aparte, es simplemente deudaExigible=0).
        List<Prestamo> todos = prestamoDaoService.selectByEntidad(idEntidad);
        double deudaExigibleTotal = 0.0;
        boolean hayPrestamoVigente = false;
        if (todos != null) {
            for (Prestamo prestamo : todos) {
                if (!esPrestamoVigente(prestamo)) {
                    continue;
                }
                hayPrestamoVigente = true;
                deudaExigibleTotal += calcularDeudaExigiblePrestamo(prestamo.getCodigo(), finDeMes);
            }
        }
        deudaExigibleTotal = redondear(deudaExigibleTotal);
        fila.setTienePrestamo(hayPrestamoVigente);

        // D4: sin préstamo vigente y sin certificado, no hay cruce posible y no puede salir
        // dinero — no hay nada que hacer con esa pensión, y no sería apta para la corrida real
        // (que tampoco le generaría ningún PGPC).
        if (!hayPrestamoVigente && !tieneCertificado) {
            fila.setApto(false);
            fila.setParticipacion("BLOQUEADO");
            fila.setMotivoBloqueo("Sin préstamo vigente y sin cuenta con certificado bancario"
                + " válido: no hay cruce posible y no se puede entregar la pensión.");
            return fila;
        }

        // §4bis: estimación en AGREGADO — min(pensiones acumuladas, deuda exigible, saldo). La
        // corrida real la aplica mes a mes con tope por préstamo (mora/interés incluidos al
        // aplicar); acá es a propósito más simple, es una estimación y el contrato lo dice.
        double pensionesAcumuladas = redondear(mesesAdeudados * valorTotal);
        double montoACruzar = redondear(Math.min(pensionesAcumuladas, Math.min(deudaExigibleTotal, saldo)));

        // Bug propio encontrado al revisar (no estaba en el encargo): el remanente NOMINAL
        // (pensionesAcumuladas - montoACruzar) puede exceder lo que de verdad queda LIBRE del
        // saldo después del cruce, si el saldo fue el factor que topó montoACruzar. Mismo
        // patrón que el sobregiro que ya se corrigió en generarMesesRetroactivos — acá también
        // hacía falta, sino el prevuelo mostraría más "a dinero" del que el saldo permite.
        double montoADineroNominal = redondear(pensionesAcumuladas - montoACruzar);
        double saldoLibreParaDinero = redondear(Math.max(0.0, saldo - montoACruzar));
        double montoADinero = tieneCertificado
            ? redondear(Math.min(montoADineroNominal, saldoLibreParaDinero))
            : 0.0;

        double total = redondear(montoACruzar + montoADinero);
        fila.setMontoACruzar(montoACruzar);
        fila.setMontoADinero(montoADinero);
        fila.setTotal(total);

        // §4bis "Mes parcial", decisión del árbitro 2026-09-04: reparto PROPORCIONAL a la
        // mensualidad, seguro por RESTA (nunca con su propia multiplicación, para que sumen
        // exacto). Se aplica sobre `total` completo, no sólo sobre un "último mes" — como el
        // prevuelo no simula mes a mes, esta es la forma agregada de la misma regla: cuando no
        // hay tope, da idéntico resultado que sumar el nominal de cada mes completo (la razón
        // 280:20 se preserva linealmente), y cuando SÍ hay tope (por saldo o por falta de
        // certificado — acá `total` ya excluye el remanente retenido sin certificado, a
        // diferencia de la corrida real), garantiza totalPension + totalSeguro == total
        // exactamente, cerrando la inconsistencia que se había reportado.
        double totalPension = redondear(total * (valorPensionMensual / valorTotal));
        double totalSeguro = redondear(total - totalPension);
        fila.setTotalPension(totalPension);
        fila.setTotalSeguro(totalSeguro);

        fila.setApto(true);
        // SOLO_CRUCE no puede darse sin préstamo: si !hayPrestamoVigente llegamos hasta acá
        // sólo cuando tieneCertificado=true (el bloqueo de arriba ya atrapó el caso contrario),
        // así que montoADineroNominal siempre se paga entero y esta rama nunca da SOLO_CRUCE.
        fila.setParticipacion(montoADineroNominal > TOLERANCIA && !tieneCertificado ? "SOLO_CRUCE" : "COMPLETA");
        return fila;
    }

    // ========================================================================
    // Generación mensual
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResultadoGeneracionPagosPension generarPagosDelMes(Long idEmpresa, Integer anio, Integer mes,
            String usuario) throws Throwable {
        System.out.println("========================================");
        System.out.println("GENERACIÓN DE PAGOS DE PENSIÓN COMPLEMENTARIA - " + mes + "/" + anio);
        System.out.println("========================================");

        if (idEmpresa == null) {
            throw new IncomeException("idEmpresa es obligatorio: es la empresa contable sobre la que"
                + " se genera la orden de pago.");
        }
        if (anio == null || mes == null || mes < 1 || mes > 12) {
            throw new IncomeException("Debe indicar un año y un mes (1-12) válidos.");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("usuario es obligatorio");
        }

        // Se resuelve UNA sola vez por corrida (no una vez por jubilado: son ~190 y el nombre
        // es el mismo para todos). registrarPagoDeOrigenExterno pide un Long, no el nombre —
        // pasarle null hacía que CXP reventara con em.find(Class, null) en cada jubilado con
        // cuenta bancaria activa (IllegalArgumentException ilegible, ver DevolucionAporteServiceImpl
        // y PrestamoServiceImpl, que sí resuelven este Long antes de llamar).
        com.saa.model.scp.Usuario usuarioRegistro = usuarioDaoService.selectByNombre(usuario);
        if (usuarioRegistro == null) {
            throw new IncomeException("USUARIO_NO_ENCONTRADO: no existe el usuario '" + usuario
                + "' en el sistema; la orden de pago en Cuentas por Pagar necesita el usuario"
                + " que la registra.");
        }
        Long idUsuario = usuarioRegistro.getCodigo();

        ResultadoGeneracionPagosPension resumen = new ResultadoGeneracionPagosPension();
        resumen.setAnio(anio);
        resumen.setMes(mes);

        List<Entidad> jubilados = entidadDaoService.selectByIdEstado(
            Long.valueOf(EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO));
        int universo = (jubilados != null) ? jubilados.size() : 0;
        System.out.println("Jubilados JUBILADO_COMPLEMENTARIO a evaluar: " + universo);

        double totalPagado = 0.0;
        double totalCruzado = 0.0;
        double totalOrdenes = 0.0;
        double totalSeguroGeneral = 0.0;

        if (jubilados != null) {
            for (Entidad jubilado : jubilados) {
                resumen.setEvaluados(resumen.getEvaluados() + 1);
                try {
                    // A través del proxy: cada jubilado en su propia transacción
                    DetallePagoPension detalle = self.generarPagoIndividual(
                        jubilado.getCodigo(), idEmpresa, anio, mes, usuario, idUsuario);
                    resumen.getDetalle().add(detalle);

                    // Retroactivo (PLAN-PAGO-RETROACTIVO-JUBILADOS.md): un jubilado puede
                    // generar 0..N PGPC en una sola llamada, no siempre 1. "generados" cuenta
                    // PAGOS (PGPC nuevos), no jubilados — por eso se suma mesesAplicados y no
                    // se incrementa en 1 por jubilado como antes.
                    if (detalle.getMesesAplicados() > 0) {
                        resumen.setGenerados(resumen.getGenerados() + detalle.getMesesAplicados());
                        totalPagado += nvl(detalle.getValorPension()) + nvl(detalle.getValorSeguroSalud());
                        totalCruzado += nvl(detalle.getValorCruzadoAPrestamo());
                        totalOrdenes += nvl(detalle.getValorOrdenPago());
                        // §4bis: agregado del seguro médico — NOMINAL, mismo criterio que la
                        // fila (ver DetallePagoPension.totalSeguro).
                        totalSeguroGeneral += detalle.getTotalSeguro();
                    } else {
                        // "YA_EXISTIA" (período único ya generado), "AL_DIA" (sin meses
                        // pendientes) y "SIN_ANCLA" (no se puede calcular desde dónde) son los
                        // tres finales normales de 0 PGPC nuevos — ⛔ ninguno es error (ítem 1 y
                        // 4 del encargo), así que los tres se cuentan junto a "ya generados" en
                        // vez de inflar "generados" o ensuciar "conError". Interpretación
                        // propia: el DTO del resumen no distingue los tres; si el operador
                        // necesita diferenciarlos, están en detalle[].estado.
                        resumen.setYaGenerados(resumen.getYaGenerados() + 1);
                    }
                } catch (Throwable e) {
                    resumen.setConError(resumen.getConError() + 1);
                    resumen.getErrores().add("Entidad " + jubilado.getCodigo() + ": " + e.getMessage());
                    System.err.println("Error al generar el pago de pensión de la entidad "
                        + jubilado.getCodigo() + ": " + e.getMessage());

                    DetallePagoPension detalleError = new DetallePagoPension();
                    detalleError.setIdEntidad(jubilado.getCodigo());
                    detalleError.setNombre(jubilado.getRazonSocial());
                    detalleError.setEstado("ERROR");
                    detalleError.setParticipacion("BLOQUEADO");
                    detalleError.setMensaje(e.getMessage());
                    resumen.getDetalle().add(detalleError);
                }
            }
        }

        resumen.setTotalPagado(redondear(totalPagado));
        resumen.setTotalCruzadoAPrestamos(redondear(totalCruzado));
        resumen.setTotalOrdenesGeneradas(redondear(totalOrdenes));
        resumen.setTotalSeguroGeneral(redondear(totalSeguroGeneral));

        System.out.println("GENERACIÓN TERMINADA - Evaluados: " + resumen.getEvaluados()
            + " - Generados: " + resumen.getGenerados()
            + " - Ya generados: " + resumen.getYaGenerados()
            + " - Con error: " + resumen.getConError()
            + " - Total pagado: $" + resumen.getTotalPagado()
            + " - Cruzado a préstamos: $" + resumen.getTotalCruzadoAPrestamos()
            + " - Órdenes generadas: $" + resumen.getTotalOrdenesGeneradas());

        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DetallePagoPension generarPagoIndividual(Long idEntidad, Long idEmpresa, Integer anio, Integer mes,
            String usuario, Long idUsuario) throws Throwable {
        System.out.println("PagoPensionComplementariaService.generarPagoIndividual - Entidad: " + idEntidad
            + " - Período: " + mes + "/" + anio);

        Entidad entidad = entidadDaoService.find(new Entidad(), idEntidad);
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe " + idEntidad);
        }

        // VPPC: exactamente una configuración activa. Ni cero (no se sabe cuánto pagarle) ni
        // más de una (no se sabe cuál vale — mismo criterio que la cuenta bancaria). El valor
        // mensual NO varía entre los meses del retroactivo: es la tasa configurada hoy.
        List<ValorPagoPensionComplementaria> configuraciones =
            valorPagoPensionComplementariaService.selectByEntidad(idEntidad);
        ValorPagoPensionComplementaria vppc = unicaActiva(configuraciones, idEntidad);
        if (vppc == null) {
            throw new IncomeException(ERR_SIN_VALOR_PENSION + ": la entidad " + idEntidad
                + " no tiene una configuración de pensión complementaria (VPPC) activa; no se"
                + " puede generar su pago.");
        }
        double valorTotal = redondear(vppc.getValorPagar() != null ? vppc.getValorPagar() : 0.0);
        double valorSeguro = redondear(vppc.getValorSeguro() != null ? vppc.getValorSeguro() : 0.0);
        double valorPension = redondear(valorTotal - valorSeguro);
        if (valorTotal <= 0.01) {
            throw new IncomeException(ERR_SIN_VALOR_PENSION + ": la entidad " + idEntidad
                + " tiene VPPC " + vppc.getCodigo() + " con valorPagar $" + valorTotal
                + "; no se puede generar un pago de $0.");
        }

        // D1 (PLAN-PAGO-RETROACTIVO-JUBILADOS.md §4bis): TODAS las fechas del hecho —cartera Y
        // pago— son las del mes de la CORRIDA, UNA sola vez para todo el método, nunca la del
        // mes M que se esté generando. min(último día del mes de la corrida, hoy): nunca
        // futura, así que el circuito no choca con validarFechaNoFutura de pagarConAportes.
        LocalDate finDeMes = YearMonth.of(anio, mes).atEndOfMonth();
        LocalDate hoy = LocalDate.now();
        LocalDate fecha = finDeMes.isAfter(hoy) ? hoy : finDeMes;
        // Auditoría — cuándo se registró de verdad. Separada a propósito de fechaHecho: reusar
        // una sola fecha para las dos cosas fue el defecto original (79204e4).
        LocalDateTime fechaRegistro = LocalDateTime.now();
        // El hecho de cartera, a la hora 00:00 — mismo patrón que
        // AporteServiceImpl.procesarJubilacion (fechaEfectiva.atStartOfDay()).
        LocalDateTime fechaHecho = fecha.atStartOfDay();

        // §6/D2 del contrato: el certificado gobierna la SALIDA (remanente al banco), NUNCA el
        // cruce contra el préstamo. Se resuelve UNA vez (dato de la entidad, no del mes).
        CuentaBancariaParticipe cuentaSalida = resolverCuentaSalida(idEntidad);

        // D4 (PLAN-PAGO-RETROACTIVO-JUBILADOS.md, ampliación 2026-09-04): el retroactivo mes a
        // mes aplica a TODO jubilado con meses adeudados, tenga o no préstamo — "a todos los
        // que tengan préstamo" contestaba sobre el CRUCE, no sobre la acumulación. Ya no hay
        // rama separada: generarMesesRetroactivos decide internamente, por mes, si hay algo
        // contra qué cruzar (según los préstamos vigentes que encuentre) y topa el disponible
        // por saldo cuando no los hay.
        return generarMesesRetroactivos(entidad, idEntidad, valorPension, valorSeguro, valorTotal,
            idEmpresa, anio, mes, usuario, idUsuario, fecha, fechaHecho, fechaRegistro, finDeMes,
            cuentaSalida);
    }

    /**
     * Circuito SIN préstamo — DESUSO desde D4 (2026-09-04): el retroactivo ahora cubre este
     * caso dentro de {@link #generarMesesRetroactivos} (préstamos vigentes vacío → tope solo
     * por saldo). No se borra por si hace falta reactivarlo — mismo criterio que
     * {@link #cruzarContraPrestamos}, también sin llamadores.
     */
    private DetallePagoPension generarUnMesSinPrestamo(Entidad entidad, Long idEntidad, double valorPension,
            double valorSeguro, double valorTotal, Long idEmpresa, Integer anio, Integer mes, String usuario,
            Long idUsuario, LocalDate fecha, LocalDateTime fechaHecho, LocalDateTime fechaRegistro,
            CuentaBancariaParticipe cuentaSalida) throws Throwable {

        PagoPensionComplementaria existente = pagoPensionDaoService.selectByEntidadYPeriodo(
            idEntidad, anio.longValue(), mes.longValue());
        if (existente != null) {
            System.out.println("  Entidad " + idEntidad + " ya tiene PGPC " + existente.getCodigo()
                + " para " + mes + "/" + anio + " - se omite");
            DetallePagoPension detalleExistente = new DetallePagoPension();
            detalleExistente.setIdEntidad(idEntidad);
            detalleExistente.setIdPago(existente.getCodigo());
            detalleExistente.setValorPension(existente.getValorPension());
            detalleExistente.setValorSeguroSalud(existente.getValorSeguro());
            detalleExistente.setEstado("YA_EXISTIA");
            return detalleExistente;
        }

        // Saldo suficiente en pensión complementaria (23): guardarraíl anti-carrera, revalidado
        // dentro de la transacción, mismo criterio que consumirAportes.
        double saldo = saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA);
        if (saldo < valorTotal - TOLERANCIA) {
            throw new IncomeException(ERR_SALDO_INSUFICIENTE + ": el saldo de pensión complementaria"
                + " de la entidad " + idEntidad + " es $" + redondear(saldo) + " y el pago del período"
                + " requiere $" + valorTotal + ".");
        }

        String glosa = "PAGO PENSION COMPLEMENTARIA " + mes + "/" + anio + " - Entidad " + idEntidad;
        // Sin préstamo, aplicadoAlPrestamo=0 y remanente=valorTotal siempre: el saldo ya se
        // validó suficiente para el nominal completo arriba, así que no hay tope que calcular.
        PagoPensionComplementaria pago = registrarPgpcDelMes(entidad, idEntidad, anio.longValue(), mes.longValue(),
            valorPension, valorSeguro, valorTotal, 0.0, valorTotal, fecha, fechaHecho, fechaRegistro, usuario,
            idUsuario, idEmpresa, cuentaSalida, glosa);

        boolean generoOrden = pago.getIdPagoProgramado() != null;
        System.out.println("  ✅ Pago de pensión registrado - Entidad " + idEntidad + " - PGPC "
            + pago.getCodigo() + " - Orden de pago " + pago.getIdPagoProgramado());

        DetallePagoPension detalle = new DetallePagoPension();
        detalle.setIdEntidad(idEntidad);
        detalle.setNombre(entidad.getRazonSocial());
        detalle.setIdPago(pago.getCodigo());
        detalle.setValorPension(valorPension);
        detalle.setValorSeguroSalud(valorSeguro);
        detalle.setValorCruzadoAPrestamo(0.0);
        detalle.setValorOrdenPago(generoOrden ? valorTotal : 0.0);
        detalle.setGeneroOrdenPago(generoOrden);
        detalle.setIdAsientoDevengo(pago.getNumeroAsientoDevengo());
        detalle.setEstado("GENERADO");
        detalle.setMesesAplicados(1);
        detalle.setValorPensionMensual(valorPension);
        detalle.setValorSeguroMensual(valorSeguro);
        detalle.setTotalPension(valorPension);
        detalle.setTotalSeguro(valorSeguro);
        // Sin préstamo: o sale el dinero completo (COMPLETA), o no hay cruce posible y no
        // puede salir dinero (BLOQUEADO) — §6 del contrato, regla explícita para este caso.
        detalle.setParticipacion(cuentaSalida != null ? "COMPLETA" : "BLOQUEADO");
        if (cuentaSalida == null) {
            detalle.setMensaje("Sin préstamo y sin cuenta con certificado bancario válido: no"
                + " hay cruce posible y no se puede entregar la pensión.");
        }
        return detalle;
    }

    /**
     * Circuito CON préstamo — PLAN-PAGO-RETROACTIVO-JUBILADOS.md. Genera los meses adeudados,
     * uno por uno en orden ascendente, cruzando cada mes contra el préstamo SIN pre-pagar
     * cuotas futuras (§3bis: el motor no filtra por fecha, así que el tope de "cuánto es
     * exigible a la fecha de la corrida" lo calcula y aplica este método, no
     * {@code pagarConAportes}).
     */
    private DetallePagoPension generarMesesRetroactivos(Entidad entidad, Long idEntidad, double valorPension,
            double valorSeguro, double valorTotal, Long idEmpresa, Integer anio, Integer mes, String usuario,
            Long idUsuario, LocalDate fecha, LocalDateTime fechaHecho, LocalDateTime fechaRegistro,
            LocalDate finDeMes, CuentaBancariaParticipe cuentaSalida) throws Throwable {

        DetallePagoPension resumen = new DetallePagoPension();
        resumen.setIdEntidad(idEntidad);
        resumen.setNombre(entidad.getRazonSocial());
        // §4bis del contrato: la tasa mensual, informativa incluso si este jubilado termina sin
        // generar ningún mes (SIN_ANCLA/AL_DIA/bloqueado) — valorPension/valorSeguro YA vienen
        // calculados como valorTotal - valorSeguro / valorSeguro, no se suman aparte.
        resumen.setValorPensionMensual(valorPension);
        resumen.setValorSeguroMensual(valorSeguro);

        // Ítem 1: el ancla. Si no hay ni movimiento negativo de pensión ni traslado de
        // jubilación, no hay desde dónde calcular — estado propio, NO "ERROR" genérico.
        LocalDate ancla = resolverAnclaRetroactivo(idEntidad);
        if (ancla == null) {
            resumen.setEstado("SIN_ANCLA");
            resumen.setParticipacion("BLOQUEADO");
            resumen.setMensaje("La entidad " + idEntidad + " no tiene ningún movimiento negativo de"
                + " pensión complementaria (tipo 23) ni un movimiento de jubilación registrado en"
                + " CRD.APRT; no hay desde dónde calcular los meses adeudados.");
            return resumen;
        }

        YearMonth desde = YearMonth.from(ancla).plusMonths(1);
        YearMonth corrida = YearMonth.of(anio, mes);
        if (desde.isAfter(corrida)) {
            resumen.setEstado("AL_DIA");
            resumen.setMensaje("La entidad " + idEntidad + " no tiene meses pendientes: su ancla es "
                + ancla + ", ya cubierta hasta " + corrida + ".");
            return resumen;
        }

        // Préstamos VIGENTES y su deuda EXIGIBLE a la fecha de la corrida (ítem 3): cuotas con
        // vencimiento <= finDeMes, NO el pendiente total del préstamo. Se calcula UNA vez con
        // calcularSaldosCuota (la variante PURA, sin autocorregir estado — esto es un tope
        // ANTES de pagar, no un camino de escritura; sustitución deliberada de
        // calcularSaldosRealesCuota, que el propio JavaDoc del motor desaconseja para lectura).
        List<Prestamo> todos = prestamoDaoService.selectByEntidad(idEntidad);
        List<Prestamo> prestamosVigentes = new ArrayList<>();
        Map<Long, Double> exigibleRestantePorPrestamo = new LinkedHashMap<>();
        if (todos != null) {
            for (Prestamo prestamo : todos) {
                if (!esPrestamoVigente(prestamo)) {
                    continue;
                }
                prestamosVigentes.add(prestamo);
                exigibleRestantePorPrestamo.put(prestamo.getCodigo(),
                    calcularDeudaExigiblePrestamo(prestamo.getCodigo(), finDeMes));
            }
        }
        double deudaExigibleTotal = redondear(sumaValores(exigibleRestantePorPrestamo));
        // D4: "tiene préstamo" para estas reglas es "tiene un préstamo VIGENTE que cruzar" —
        // si VPPC.tienePrestamo dice sí pero ya no queda ninguno vigente, se trata igual que
        // sin préstamo (no hay contra qué cruzar; el saldo es el único techo). No hace falta
        // leer vppc.getTienePrestamo() para nada de esto.
        boolean hayPrestamoVigente = !prestamosVigentes.isEmpty();

        // D4: sin préstamo Y sin certificado, no hay cruce posible y no puede salir dinero —
        // no hay NADA que hacer con esa pensión. No genera ningún PGPC, a diferencia del caso
        // con préstamo (ahí el cruce es un hecho económico real aunque no haya certificado).
        if (!hayPrestamoVigente && cuentaSalida == null) {
            int mesesAdeudadosSinGenerar = (int) (ChronoUnit.MONTHS.between(desde, corrida) + 1);
            resumen.setEstado("GENERADO");
            resumen.setMesesAplicados(0);
            resumen.setParticipacion("BLOQUEADO");
            resumen.setMotivoCorte("SIN_PRESTAMO_SIN_CERTIFICADO");
            resumen.setMensaje("Sin préstamo vigente y sin cuenta con certificado bancario válido:"
                + " no hay cruce posible y no se puede entregar la pensión. " + mesesAdeudadosSinGenerar
                + " mes(es) adeudado(s) sin generar.");
            return resumen;
        }

        double saldoRestante = saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA);

        int mesesAplicados = 0;
        double totalPension = 0.0, totalSeguro = 0.0, totalCruzado = 0.0, totalOrden = 0.0;
        Long ultimoIdPago = null;
        Long ultimoIdAsientoDevengo = null;
        boolean algunaOrdenGenerada = false;
        boolean algunRemanenteRetenido = false;
        String motivoCorte = "MES_CORRIDA_ALCANZADO";

        for (YearMonth ym = desde; !ym.isAfter(corrida); ym = ym.plusMonths(1)) {
            long anioM = ym.getYear();
            long mesM = ym.getMonthValue();

            // Idempotencia por mes: la UNIQUE(ENTDCDGO,PGPCANNO,PGPCMESS) es la garantía real;
            // esto solo evita el viaje a la excepción y dice claro por qué se omite. En la
            // práctica casi nunca dispara: el ancla ya avanza solo con cada corrida anterior.
            PagoPensionComplementaria existenteM = pagoPensionDaoService.selectByEntidadYPeriodo(
                idEntidad, anioM, mesM);
            if (existenteM != null) {
                System.out.println("  Entidad " + idEntidad + " ya tiene PGPC " + existenteM.getCodigo()
                    + " para " + mesM + "/" + anioM + " - se omite");
                continue;
            }

            // Ítem 4: las condiciones de corte, ANTES de procesar el mes. D4: sin préstamo
            // vigente son DOS, no tres — "PRESTAMO_AL_DIA" no puede aplicar si nunca hubo
            // deuda contra la cual cruzar (deudaExigibleTotal sería 0 desde el principio).
            if (saldoRestante <= TOLERANCIA) {
                motivoCorte = "SALDO_AGOTADO";
                break;
            }
            if (hayPrestamoVigente && deudaExigibleTotal <= TOLERANCIA) {
                motivoCorte = "PRESTAMO_AL_DIA";
                break;
            }

            // D4: sin préstamo vigente, el tope es min(pensión del mes, saldo) — SIN
            // deudaExigibleTotal, que sin préstamo vale 0 y forzaría el disponible a 0.
            double disponibleMes = hayPrestamoVigente
                ? redondear(Math.min(valorTotal, Math.min(deudaExigibleTotal, saldoRestante)))
                : redondear(Math.min(valorTotal, saldoRestante));

            // Cruce del mes: en orden, respetando el tope EXIGIBLE de CADA préstamo (no el
            // pendiente total) — así el motor jamás llega a una cuota futura, sin tocarlo.
            double aplicadoEsteMes = 0.0;
            for (Prestamo prestamo : prestamosVigentes) {
                double disponibleParaEste = redondear(disponibleMes - aplicadoEsteMes);
                if (disponibleParaEste <= TOLERANCIA) {
                    break;
                }
                double exigibleRestante = exigibleRestantePorPrestamo.getOrDefault(prestamo.getCodigo(), 0.0);
                if (exigibleRestante <= TOLERANCIA) {
                    continue;
                }
                double aCruzar = redondear(Math.min(disponibleParaEste, exigibleRestante));
                if (aCruzar <= TOLERANCIA) {
                    continue;
                }

                SolicitudPagoConAportes solicitud = new SolicitudPagoConAportes();
                solicitud.setIdPrestamo(prestamo.getCodigo());
                DesgloseAporte desglose = new DesgloseAporte();
                desglose.setIdTipoAporte(TIPO_APORTE_PENSION_COMPLEMENTARIA);
                desglose.setValor(aCruzar);
                solicitud.setAportes(Collections.singletonList(desglose));
                solicitud.setUsuario(usuario);
                solicitud.setObservacion("Pago retroactivo pensión complementaria " + mesM + "/" + anioM
                    + " (corrida " + mes + "/" + anio + ") contra préstamo " + prestamo.getCodigo());
                // D1: la fecha del PAGO al préstamo es la de la corrida, no la del mes M.
                solicitud.setFechaPago(fecha);
                solicitud.setIdEmpresa(idEmpresa);

                double aplicadoPrestamo;
                try {
                    ResultadoPagoConAportes resultado = procesoPagoPrestamoService.pagarConAportes(solicitud);
                    ResultadoAplicacionPago aplicacion = resultado != null ? resultado.getResultado() : null;
                    aplicadoPrestamo = redondear(aplicacion != null ? aplicacion.getValorAplicado() : 0.0);
                } catch (IncomeException e) {
                    if (e.getMessage() != null
                            && e.getMessage().startsWith(ProcesoPagoPrestamoService.ERR_SIN_CUOTAS_PENDIENTES)) {
                        // Ítem 4: corte NORMAL de ESTE préstamo — quedó al día. No es error.
                        exigibleRestantePorPrestamo.put(prestamo.getCodigo(), 0.0);
                        continue;
                    }
                    throw e;
                }
                aplicadoEsteMes = redondear(aplicadoEsteMes + aplicadoPrestamo);
                exigibleRestantePorPrestamo.put(prestamo.getCodigo(), redondear(exigibleRestante - aplicadoPrestamo));

                System.out.println("    ↳ Retroactivo " + mesM + "/" + anioM + ": $" + aplicadoPrestamo
                    + " aplicados al préstamo " + prestamo.getCodigo());
            }
            deudaExigibleTotal = redondear(sumaValores(exigibleRestantePorPrestamo));
            totalCruzado = redondear(totalCruzado + aplicadoEsteMes);

            // El remanente NOMINAL es lo que queda del valor total tras el cruce, pero nunca
            // puede exceder lo que de verdad queda LIBRE del saldo del aporte 23 después de
            // financiar el cruce de este mes — si el saldo alcanzó para cruzar pero no para
            // todo el remanente nominal, se topa acá; si no, se sobregiraría el aporte más
            // abajo (bug propio, encontrado y corregido antes de reportar).
            double remanenteNominal = redondear(valorTotal - aplicadoEsteMes);
            double saldoLibreParaRemanente = redondear(Math.max(0.0, saldoRestante - aplicadoEsteMes));
            double remanenteMes = redondear(Math.min(remanenteNominal, saldoLibreParaRemanente));

            String glosa = "PAGO PENSION COMPLEMENTARIA RETROACTIVO " + mesM + "/" + anioM
                + " - Entidad " + idEntidad;
            PagoPensionComplementaria pago = registrarPgpcDelMes(entidad, idEntidad, anioM, mesM,
                valorPension, valorSeguro, valorTotal, aplicadoEsteMes, remanenteMes, fecha, fechaHecho,
                fechaRegistro, usuario, idUsuario, idEmpresa, cuentaSalida, glosa);

            boolean saleAlBancoEsteMes = pago.getIdPagoProgramado() != null;
            double consumidoEsteMes = redondear(aplicadoEsteMes + (saleAlBancoEsteMes ? remanenteMes : 0.0));
            saldoRestante = redondear(saldoRestante - consumidoEsteMes);

            mesesAplicados++;
            // §4bis "Mes parcial", decisión del árbitro 2026-09-04: si este mes procesó MENOS
            // que la mensualidad completa (cruce + remanente < valorTotal — pasa cuando el
            // saldo del aporte 23 es el factor que topó, no cuando fue la deuda exigible: ahí
            // lo que sobra del cruce se redirige entero a remanente y el total sigue siendo
            // valorTotal), el parcial se reparte PROPORCIONAL a la mensualidad. El seguro sale
            // por RESTA, nunca con su propia multiplicación — dos redondeos independientes
            // podrían no sumar el total y dejar un descuadre de centavos entre las dos cuentas
            // contables (mismo motivo que H24).
            double montoProcesadoEsteMes = redondear(aplicadoEsteMes + remanenteMes);
            double pensionEsteMes;
            double seguroEsteMes;
            if (redondear(valorTotal - montoProcesadoEsteMes) > TOLERANCIA) {
                pensionEsteMes = redondear(montoProcesadoEsteMes * (valorPension / valorTotal));
                seguroEsteMes = redondear(montoProcesadoEsteMes - pensionEsteMes);
            } else {
                pensionEsteMes = valorPension;
                seguroEsteMes = valorSeguro;
            }
            totalPension = redondear(totalPension + pensionEsteMes);
            totalSeguro = redondear(totalSeguro + seguroEsteMes);
            if (saleAlBancoEsteMes) {
                totalOrden = redondear(totalOrden + remanenteMes);
                algunaOrdenGenerada = true;
            } else if (remanenteMes > TOLERANCIA) {
                // Quedó remanente este mes y no se pudo entregar (sin cuenta o sin
                // certificado) — participación de TODO el resumen baja a SOLO_CRUCE.
                algunRemanenteRetenido = true;
            }
            ultimoIdPago = pago.getCodigo();
            ultimoIdAsientoDevengo = pago.getNumeroAsientoDevengo();

            System.out.println("  PGPC " + pago.getCodigo() + " (" + mesM + "/" + anioM + ") - cruzado $"
                + aplicadoEsteMes + " - remanente $" + remanenteMes
                + (saleAlBancoEsteMes ? " (pagado)" : " (retenido en su cuenta)"));
        }

        resumen.setIdPago(ultimoIdPago);
        resumen.setValorPension(totalPension);
        resumen.setValorSeguroSalud(totalSeguro);
        // §4bis: NOMINAL (mesesAplicados × valorSeguroMensual), mismo criterio que ya usa
        // PGPC.valorSeguro en cada fila — no prorratea el último mes si quedó topado por saldo
        // o deuda exigible (ítem 3 del encargo: ese prorrateo es una decisión del usuario, no
        // se inventó acá).
        resumen.setTotalPension(totalPension);
        resumen.setTotalSeguro(totalSeguro);
        resumen.setValorCruzadoAPrestamo(totalCruzado);
        resumen.setValorOrdenPago(totalOrden);
        resumen.setGeneroOrdenPago(algunaOrdenGenerada);
        resumen.setIdAsientoDevengo(ultimoIdAsientoDevengo);
        // "AL_DIA" queda RESERVADO para el corte temprano de arriba (desde.isAfter(corrida)):
        // acá el bucle SÍ corrió. Si terminó con 0 meses (p.ej. SALDO_AGOTADO ya en el primer
        // mes elegible), no es "al día" — sigue debiendo, sólo que no se le pudo aplicar nada.
        // mesesAplicados + motivoCorte ya dicen esa historia completa; no hace falta un tercer
        // estado para distinguirlo dentro de "GENERADO".
        resumen.setEstado("GENERADO");
        resumen.setMesesAplicados(mesesAplicados);
        resumen.setMotivoCorte(motivoCorte);
        // §6 del contrato: COMPLETA si NINGÚN mes generado quedó con remanente retenido
        // (incluye el 100%-cruzado, donde no había remanente que retener); SOLO_CRUCE si AL
        // MENOS un mes sí lo retuvo (sin cuenta o sin certificado). Sin meses generados
        // (mesesAplicados == 0, p.ej. SALDO_AGOTADO desde el primer mes elegible) no es un
        // evento de participación nuevo — motivoCorte ya explica por qué, queda sin setear.
        if (mesesAplicados > 0) {
            resumen.setParticipacion(algunRemanenteRetenido ? "SOLO_CRUCE" : "COMPLETA");
        }

        System.out.println("  ✅ Retroactivo Entidad " + idEntidad + " - " + mesesAplicados
            + " mes(es) generado(s) - corte: " + motivoCorte + " - cruzado total $" + totalCruzado
            + " - pagado total $" + totalOrden);

        return resumen;
    }

    /**
     * Crea la fila PGPC de UN mes (retroactivo o normal), su movimiento negativo de remanente
     * si corresponde, su orden de pago si corresponde, y su asiento de devengo. Compartido por
     * {@link #generarUnMesSinPrestamo} y {@link #generarMesesRetroactivos} para que las dos
     * rutas no puedan divergir en cómo arman un PGPC.
     *
     * @param aplicadoAlPrestamo cuánto de {@code valorTotal} ya se cruzó contra el préstamo
     *                           este mes (0 si no aplica) — solo para el registro del PGPC
     * @param remanente          cuánto corresponde intentar pagar al banco este mes. Lo calcula
     *                           el llamador, NO {@code valorTotal - aplicadoAlPrestamo}: en el
     *                           retroactivo, si el saldo del aporte 23 alcanza para el cruce
     *                           pero no para todo el remanente nominal, el llamador ya lo topó
     *                           al saldo que de verdad queda libre — pasar el nominal acá
     *                           sobregiraría el aporte.
     * @param cuentaSalida       la cuenta con certificado válido, o {@code null} si no hay a
     *                           quién pagarle el remanente (§6/D2): en ese caso el remanente
     *                           NO se consume ni se envía a tesorería, queda a favor
     */
    private PagoPensionComplementaria registrarPgpcDelMes(Entidad entidad, Long idEntidad, long anioM, long mesM,
            double valorPension, double valorSeguro, double valorTotal, double aplicadoAlPrestamo, double remanente,
            LocalDate fecha, LocalDateTime fechaHecho, LocalDateTime fechaRegistro, String usuario, Long idUsuario,
            Long idEmpresa, CuentaBancariaParticipe cuentaSalida, String glosaMovimiento) throws Throwable {

        boolean saleAlBanco = remanente > TOLERANCIA && cuentaSalida != null;

        // Movimiento NEGATIVO en CRD.APRT — SOLO por el remanente que de verdad sale al banco.
        // El tramo cruzado ya generó su propio movimiento NEGATIVO (tipo PAGO_PRESTAMO) dentro
        // de pagarConAportes/consumirAportes — duplicarlo acá bajaría el saldo dos veces.
        Aporte aporteRemanente = null;
        if (saleAlBanco) {
            aporteRemanente = crearMovimientoNegativo(entidad, remanente, glosaMovimiento, fechaHecho, usuario);
        }

        PagoPensionComplementaria pago = new PagoPensionComplementaria();
        pago.setEntidad(entidad);
        pago.setFilial(entidad.getFilial());
        pago.setAnio(anioM);
        pago.setMes(mesM);
        pago.setValorPension(valorPension);
        pago.setValorSeguro(valorSeguro);
        pago.setValor(valorTotal);
        pago.setFecha(fecha);
        pago.setIdAporte(aporteRemanente != null ? aporteRemanente.getCodigo() : null);
        pago.setUsuarioRegistro(usuario);
        pago.setFechaRegistro(fechaRegistro);

        if (saleAlBanco) {
            Long idPagoOrden;
            try {
                idPagoOrden = generarOrdenPagoPension(entidad, cuentaSalida, pago, remanente, idEmpresa,
                    mesM, anioM, idUsuario);
            } catch (IncomeException e) {
                throw new IncomeException("No se pudo generar la orden de pago en Cuentas por Pagar"
                    + " para la entidad " + idEntidad + " (" + mesM + "/" + anioM + "): " + e.getMessage());
            } catch (Throwable e) {
                throw new IncomeException("No se pudo generar la orden de pago en Cuentas por Pagar"
                    + " para la entidad " + idEntidad + " (" + mesM + "/" + anioM + "): " + e.getMessage());
            }
            pago.setIdPagoProgramado(idPagoOrden);
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.EN_PAGO));
        } else if (remanente <= TOLERANCIA) {
            // Sin remanente: la deuda (o el cruce del mes) consumió toda la pensión. El pago
            // existe, se contabiliza, y no hay nada que esperar del banco.
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.PAGADA));
            pago.setFechaPago(fecha);
        } else {
            // Hay remanente pero no se puede entregar (sin cuenta activa, cuenta ambigua, o sin
            // certificado): el pago se registra igual —el cruce y el devengo ya ocurrieron—
            // pero SIN pasar por EN_PAGO ni PAGADA, porque ninguna de las dos es cierta todavía.
            // ⚠️ Interpretación propia del backend, no especificada en el encargo: reportada al
            // árbitro para confirmar si REGISTRADA es el estado correcto para este caso nuevo.
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.REGISTRADA));
        }

        pago = pagoPensionDaoService.save(pago, null);

        // §4 PLAN-PAGO-JUBILADOS.md: el devengo lo genera CRD, DESPUÉS de la orden de pago (o
        // de decidir que no la hay) — si el devengo fallara acá, la transacción entera se
        // revierte y la orden de pago tampoco queda.
        Long idAsientoDevengo = generarAsientoDevengoPension(pago, entidad, idEmpresa);
        pago.setNumeroAsientoDevengo(idAsientoDevengo);
        pago = pagoPensionDaoService.save(pago, pago.getCodigo());

        return pago;
    }

    /** Arma y registra la orden de pago del remanente en CXP. Extraído para reusarse por mes. */
    private Long generarOrdenPagoPension(Entidad entidad, CuentaBancariaParticipe cuenta,
            PagoPensionComplementaria pago, double remanente, Long idEmpresa, long mesM, long anioM,
            Long idUsuario) throws Throwable {
        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre(entidad.getRazonSocial());
        beneficiario.setIdentificacion(entidad.getNumeroIdentificacion());
        beneficiario.setIdBancoExterno(cuenta.getBancoExterno() != null ? cuenta.getBancoExterno().getCodigo() : null);
        beneficiario.setTipoCuenta(cuenta.getTipoCuenta());
        beneficiario.setNumeroCuenta(cuenta.getNumeroCuenta());

        String observacion = "Pago pensión complementaria " + mesM + "/" + anioM + " - "
            + entidad.getRazonSocial() + " (PGPC " + pago.getCodigo() + ")";

        // idCuentaBancariaOrigen SIEMPRE null: tesorería asigna cuenta/forma de pago al aprobar
        // — mismo criterio que DevolucionAporteServiceImpl (punto 14, 2026-08-27).
        // §6bis (refinamiento 2026-09-04): el PAGO va con la fecha ACTUAL, separado de la de
        // cartera.
        Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
            OrigenPagoExterno.CRD_PAGO_PENSION_COMPLEMENTARIA, pago.getCodigo(),
            idEmpresa, null, remanente, LocalDate.now().toString(), beneficiario,
            null, // sin desglose contable — mismo estado que la devolución hoy (§6.5.b)
            observacion, idUsuario, false, null);

        Object valorPago = (respuesta != null) ? respuesta.get("pago") : null;
        if (valorPago == null) {
            throw new IncomeException("Cuentas por Pagar no devolvió el número de la orden.");
        }
        return ((Number) valorPago).longValue();
    }

    /**
     * Ítem 1: el ancla del retroactivo — fecha del último movimiento NEGATIVO del aporte 23,
     * o si no hay ninguno, la del movimiento de JUBILACIÓN (positivo). Los meses a generar van
     * desde el mes SIGUIENTE a esta fecha hasta el mes de la corrida.
     *
     * @return la fecha ancla, o {@code null} si la entidad no tiene ninguno de los dos
     */
    private LocalDate resolverAnclaRetroactivo(Long idEntidad) throws Throwable {
        LocalDateTime ultimaNegativa = aporteDaoService.selectFechaUltimoMovimientoNegativo(
            idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA);
        if (ultimaNegativa != null) {
            return ultimaNegativa.toLocalDate();
        }
        LocalDateTime jubilacion = aporteDaoService.selectFechaMovimientoJubilacion(
            idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA, (long) CrdTipoMovimientoAporte.JUBILACION);
        return jubilacion != null ? jubilacion.toLocalDate() : null;
    }

    /**
     * Ítem 3: deuda EXIGIBLE de un préstamo a una fecha de corte — suma del saldo pendiente de
     * sus cuotas con {@code fechaVencimiento <= finCorrida}. Usa
     * {@code calcularSaldosCuota} (la variante PURA, sin autocorregir estado ni persistir
     * nada): esto es un tope calculado ANTES de pagar, no un camino de escritura — el propio
     * JavaDoc de {@code calcularSaldosRealesCuota} desaconseja usarla desde ahí.
     *
     * ⛔ NO usa {@code MotorPagoPrestamoService.calcularTotalPendientePrestamo}: ese método
     * suma TODAS las cuotas pendientes, exigibles o no — exactamente lo que
     * {@code buscarSiguienteCuotaConSaldo} (sin filtro de fecha, §3bis del plan) prepagaría si
     * se le entregara de más.
     *
     * ⛔⛔ Corrección 2026-09-04 (el usuario detectó descuento de más comparando contra la
     * pantalla de cobros personales): el universo de cuotas tiene que ser el MISMO que el
     * motor puede aplicar, ni más ni menos. {@code selectByPrestamo} traía TODAS las cuotas
     * sin filtrar estado — una cuota CANCELADA_ANTICIPADA(7) (precancelación o abono a
     * capital) no tiene pagos en PGPR por su valor completo, así que
     * {@code calcularSaldosCuota} le calculaba un pendiente mayor a cero aunque estuviera
     * liquidada, y el tope se inflaba con deuda que el motor nunca iba a tocar
     * ({@code buscarSiguienteCuotaConSaldo} usa {@code selectCuotasPendientesByPrestamoOrdenadas},
     * que sí filtra). Ahora usa el MISMO DAO que el motor — no un filtro de estado a mano acá,
     * para que el criterio viva en un solo lugar. Excluye PAGADA(4) y CANCELADA_ANTICIPADA(7)
     * (verificado leyendo la query, no asumido) — el mismo criterio que
     * {@code saldo-prestamo.service.ts:esCuotaLiquidada} del frontend.
     */
    private double calcularDeudaExigiblePrestamo(Long idPrestamo, LocalDate finCorrida) throws Throwable {
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
        double total = 0.0;
        if (cuotas != null) {
            for (DetallePrestamo cuota : cuotas) {
                if (cuota.getFechaVencimiento() == null) {
                    continue;
                }
                if (cuota.getFechaVencimiento().toLocalDate().isAfter(finCorrida)) {
                    continue;
                }
                SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosCuota(cuota);
                total += saldos != null ? saldos.getTotalPendiente() : 0.0;
            }
        }
        return redondear(total);
    }

    private double sumaValores(Map<Long, Double> valores) {
        double total = 0.0;
        for (Double valor : valores.values()) {
            total += valor != null ? valor : 0.0;
        }
        return total;
    }

    /**
     * §3 PLAN-PAGO-JUBILADOS.md: si el jubilado tiene préstamo vigente
     * ({@code VPPC.tienePrestamo} — supuesto declarado del plan, pendiente de confirmar con el
     * usuario si en cambio debería ser una decisión operador-por-operador), cruza la pensión
     * del mes contra su deuda ANTES de que nada salga a tesorería. Orquesta
     * {@code ProcesoPagoPrestamoService.pagarConAportes} — el cruce de valores ya en
     * producción — nunca reimplementa la cascada acá (§6: se llama, no se modifica).
     *
     * @return cuánto de {@code valorDisponible} se aplicó a préstamos (0 si no tiene préstamo
     *         vigente marcado, o si ninguno tiene deuda pendiente)
     */
    private double cruzarContraPrestamos(Entidad entidad, ValorPagoPensionComplementaria vppc,
            double valorDisponible, Long idEmpresa, LocalDate fecha, String usuario,
            Integer mes, Integer anio) throws Throwable {
        if (vppc.getTienePrestamo() == null || vppc.getTienePrestamo() != 1L) {
            return 0.0;
        }

        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(entidad.getCodigo());
        if (prestamos == null || prestamos.isEmpty()) {
            return 0.0;
        }

        double montoCruzado = 0.0;
        for (Prestamo prestamo : prestamos) {
            double disponible = redondear(valorDisponible - montoCruzado);
            if (disponible <= TOLERANCIA) {
                break;
            }
            if (!esPrestamoVigente(prestamo)) {
                continue;
            }
            double deuda = motorPagoPrestamoService.calcularTotalPendientePrestamo(prestamo.getCodigo());
            if (deuda <= TOLERANCIA) {
                continue;
            }
            double aCruzar = redondear(Math.min(disponible, deuda));
            if (aCruzar <= TOLERANCIA) {
                continue;
            }

            SolicitudPagoConAportes solicitud = new SolicitudPagoConAportes();
            solicitud.setIdPrestamo(prestamo.getCodigo());
            DesgloseAporte desglose = new DesgloseAporte();
            desglose.setIdTipoAporte(TIPO_APORTE_PENSION_COMPLEMENTARIA);
            desglose.setValor(aCruzar);
            solicitud.setAportes(Collections.singletonList(desglose));
            solicitud.setUsuario(usuario);
            solicitud.setObservacion("Cruce pensión complementaria " + mes + "/" + anio
                + " contra préstamo " + prestamo.getCodigo());
            solicitud.setFechaPago(fecha);
            solicitud.setIdEmpresa(idEmpresa);

            ResultadoPagoConAportes resultado = procesoPagoPrestamoService.pagarConAportes(solicitud);
            ResultadoAplicacionPago aplicacion = resultado != null ? resultado.getResultado() : null;
            double aplicado = redondear(aplicacion != null ? aplicacion.getValorAplicado() : 0.0);
            montoCruzado = redondear(montoCruzado + aplicado);

            System.out.println("    ↳ Cruce pensión-préstamo: $" + aplicado + " aplicados al"
                + " préstamo " + prestamo.getCodigo() + " (deuda pendiente $" + deuda + ")");
        }

        return montoCruzado;
    }

    /** VIGENTE o EN_MORA — mismo criterio de "préstamo vivo" que el resto del módulo. */
    private boolean esPrestamoVigente(Prestamo prestamo) {
        if (prestamo == null || prestamo.getIdEstado() == null) {
            return false;
        }
        long estado = prestamo.getIdEstado();
        return estado == EstadoPrestamo.VIGENTE || estado == EstadoPrestamo.EN_MORA;
    }

    /**
     * §4 PLAN-PAGO-JUBILADOS.md: asiento de DEVENGO de la pensión y su seguro de salud —
     * SIEMPRE por el valor completo devengado (valorPension + valorSeguro), independiente de
     * cuánto se haya cruzado contra un préstamo: ese tramo lo contabiliza aparte
     * {@code contabilizarPagoConAportes} (dentro de {@code pagarConAportes}, ya existente, no
     * se toca). El asiento de PAGO contra banco tampoco va acá — lo genera CXP/TSR con la
     * orden de pago, igual que en la devolución de aportes.
     */
    private Long generarAsientoDevengoPension(PagoPensionComplementaria pago, Entidad entidad, Long idEmpresa)
            throws Throwable {
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("  Contabilidad de CRD INACTIVA: PGPC " + pago.getCodigo()
                + " registrado sin generar el asiento de devengo.");
            return null;
        }

        Long idPlantilla = plantillaService.codigoByAlterno(
            PlantillasCredito.PAGO_PENSION_COMPLEMENTARIA, idEmpresa);
        if (idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno "
                + PlantillasCredito.PAGO_PENSION_COMPLEMENTARIA + " (pago mensual de pensión"
                + " complementaria) para la empresa " + idEmpresa + ". Corra sql/173 antes de"
                + " generar pagos de pensión.");
        }

        String prefijo = "Pensión complementaria " + pago.getMes() + "/" + pago.getAnio()
            + " - " + entidad.getRazonSocial() + " (PGPC " + pago.getCodigo() + ")";

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalDebe = 0.0;
        double totalHaber = 0.0;

        double valorPension = redondear(nvl(pago.getValorPension()));
        if (valorPension > TOLERANCIA) {
            lineas.add(lineaDevengo(idPlantilla, 1, valorPension, prefijo + " - pensión"));
            lineas.add(lineaDevengo(idPlantilla, 2, valorPension, prefijo + " - pensión"));
            totalDebe += valorPension;
            totalHaber += valorPension;
        }

        double valorSeguro = redondear(nvl(pago.getValorSeguro()));
        if (valorSeguro > TOLERANCIA) {
            lineas.add(lineaDevengo(idPlantilla, 3, valorSeguro, prefijo + " - seguro de salud"));
            lineas.add(lineaDevengo(idPlantilla, 4, valorSeguro, prefijo + " - seguro de salud"));
            totalDebe += valorSeguro;
            totalHaber += valorSeguro;
        }

        totalDebe = redondear(totalDebe);
        totalHaber = redondear(totalHaber);
        double valorTotal = redondear(nvl(pago.getValor()));
        if (Math.abs(redondear(totalDebe - valorTotal)) > TOLERANCIA
                || Math.abs(redondear(totalHaber - valorTotal)) > TOLERANCIA) {
            throw new IncomeException("El asiento de devengo del PGPC " + pago.getCodigo()
                + " no cuadra contra su valor total: DEBE $" + totalDebe + ", HABER $" + totalHaber
                + ", pago $" + valorTotal + ". No se genera un asiento desbalanceado.");
        }

        Asiento asiento = asientoContableService.generarAsiento(idEmpresa, TipoAsientos.CREDITOS,
            pago.getFecha(), prefijo, pago.getUsuarioRegistro(), lineas,
            Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        System.out.println("  ✅ Asiento de devengo generado - PGPC " + pago.getCodigo()
            + " - Asiento " + asiento.getCodigo() + " - $" + valorTotal);

        return asiento.getCodigo();
    }

    /** Arma una línea del devengo desde la plantilla 35 por su aux1 posicional (1..4). */
    private DetalleAsiento lineaDevengo(Long idPlantilla, int aux1, double valor, String descripcion)
            throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.PAGO_PENSION_COMPLEMENTARIA
                + " no tiene la línea aux1=" + aux1 + " — corra sql/173 completo (ver su control"
                + " D.1, deben salir 4 líneas).");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        boolean debe = linea.getMovimiento() != null && linea.getMovimiento().longValue() == 1L;
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    // ========================================================================
    // Reconciliador
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResultadoSincronizacion sincronizarPagos() throws Throwable {
        System.out.println("========================================");
        System.out.println("SINCRONIZACIÓN DE PAGOS DE PENSIÓN COMPLEMENTARIA");
        System.out.println("========================================");

        ResultadoSincronizacion resumen = new ResultadoSincronizacion();

        List<PagoPensionComplementaria> pendientes = pagoPensionDaoService.selectPendientesConciliacion();
        int universo = (pendientes != null) ? pendientes.size() : 0;
        System.out.println("Pagos a evaluar: " + universo);

        if (pendientes != null) {
            for (PagoPensionComplementaria pendiente : pendientes) {
                try {
                    ResultadoSincronizacion parcial = self.sincronizarPago(pendiente.getCodigo());
                    acumular(resumen, parcial);
                } catch (Throwable e) {
                    resumen.setEvaluadas(resumen.getEvaluadas() + 1);
                    resumen.setConError(resumen.getConError() + 1);
                    resumen.getErrores().add("Pago " + pendiente.getCodigo() + ": " + e.getMessage());
                    System.err.println("Error al reconciliar el pago " + pendiente.getCodigo()
                        + ": " + e.getMessage());
                }
            }
        }

        System.out.println("SINCRONIZACIÓN TERMINADA - Evaluadas: " + resumen.getEvaluadas()
            + " - Pagadas: " + resumen.getMarcadasPagadas()
            + " - Rechazadas: " + resumen.getMarcadasRechazadas()
            + " - Con error: " + resumen.getConError());

        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoSincronizacion sincronizarPago(Long idPago) throws Throwable {
        System.out.println("PagoPensionComplementariaService.sincronizarPago - Pago: " + idPago);

        ResultadoSincronizacion parcial = new ResultadoSincronizacion();

        PagoPensionComplementaria pago = pagoPensionDaoService.find(new PagoPensionComplementaria(), idPago);
        if (pago == null) {
            throw new IncomeException(ERR_PAGO_NO_ENCONTRADO + ": no existe el pago " + idPago);
        }

        int estado = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
        if (estado != EstadoPagoPensionComplementaria.REGISTRADA && estado != EstadoPagoPensionComplementaria.EN_PAGO) {
            System.out.println("  Pago " + idPago + " en estado " + estado + ": sin cambios.");
            return parcial;
        }
        if (pago.getIdPagoProgramado() == null) {
            System.out.println("  Pago " + idPago + " sin orden de pago: sin cambios.");
            return parcial;
        }

        parcial.setEvaluadas(1);

        PagoProgramado pagoProgramado = pagoProgramadoDaoService.find(new PagoProgramado(), pago.getIdPagoProgramado());
        if (pagoProgramado == null) {
            parcial.setHuerfanas(1);
            parcial.getErrores().add("Pago " + idPago + ": la orden de pago " + pago.getIdPagoProgramado()
                + " ya no existe en Cuentas por Pagar.");
            return parcial;
        }

        int estadoPago = (pagoProgramado.getEstado() != null) ? pagoProgramado.getEstado().intValue() : 0;

        if (estadoPago == EstadoPagoProgramado.CONFIRMADO) {
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.PAGADA));
            pago.setFechaPago(pagoProgramado.getFechaRespuesta());
            // sql/175 (2026-09-02): numeroAsiento vuelve a su significado original — el asiento
            // del PAGO que genera CXP, escrito acá al confirmarse. El de DEVENGO (CRD) vive en
            // numeroAsientoDevengo desde generarPagoIndividual y no se toca en este método.
            pago.setNumeroAsiento((pagoProgramado.getAsiento() != null) ? pagoProgramado.getAsiento().getCodigo() : null);
            pagoPensionDaoService.save(pago, pago.getCodigo());
            parcial.setMarcadasPagadas(1);
            System.out.println("  ✅ Pago " + idPago + " PAGADO - Fecha: " + pago.getFechaPago());

        } else if (estadoPago == EstadoPagoProgramado.RECHAZADO || estadoPago == EstadoPagoProgramado.ANULADO) {
            generarContraMovimiento(pago);
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.RECHAZADA));
            pagoPensionDaoService.save(pago, pago.getCodigo());
            parcial.setMarcadasRechazadas(1);
            System.out.println("  ↩ Pago " + idPago + " RECHAZADO: contra-movimiento generado.");

        } else {
            System.out.println("  Pago " + idPago + ": el pago sigue en curso (estado " + estadoPago + "). Sin cambios.");
        }

        return parcial;
    }

    // ========================================================================
    // Helpers privados
    // ========================================================================

    /** Exactamente una configuración ACTIVA; null si hay cero. Más de una es dato roto: falla. */
    private ValorPagoPensionComplementaria unicaActiva(List<ValorPagoPensionComplementaria> configuraciones,
            Long idEntidad) throws Throwable {
        if (configuraciones == null) {
            return null;
        }
        ValorPagoPensionComplementaria unica = null;
        int activas = 0;
        for (ValorPagoPensionComplementaria vppc : configuraciones) {
            if (vppc.getEstado() != null && vppc.getEstado() == Estado.ACTIVO) {
                activas++;
                unica = vppc;
            }
        }
        if (activas > 1) {
            throw new IncomeException(ERR_SIN_VALOR_PENSION + ": la entidad " + idEntidad + " tiene "
                + activas + " configuraciones de pensión complementaria (VPPC) activas al mismo"
                + " tiempo; no se puede saber cuál vale. Debe quedar una sola activa.");
        }
        return unica;
    }

    /** Exactamente una cuenta bancaria ACTIVA. Cero o más de una: falla, no se adivina el destino. */
    private CuentaBancariaParticipe unicaCuentaActiva(Long idEntidad) throws Throwable {
        List<CuentaBancariaParticipe> cuentas = cuentaBancariaParticipeDaoService.selectByParent(idEntidad);
        CuentaBancariaParticipe unica = null;
        int activas = 0;
        if (cuentas != null) {
            for (CuentaBancariaParticipe cuenta : cuentas) {
                if (cuenta.getEstado() != null && cuenta.getEstado() == Estado.ACTIVO) {
                    activas++;
                    unica = cuenta;
                }
            }
        }
        if (activas == 0) {
            throw new IncomeException(ERR_SIN_CUENTA_BANCARIA + ": la entidad " + idEntidad
                + " no tiene ninguna cuenta bancaria activa registrada; no se puede generar su"
                + " pago de pensión complementaria.");
        }
        if (activas > 1) {
            throw new IncomeException(ERR_SIN_CUENTA_BANCARIA + ": la entidad " + idEntidad + " tiene "
                + activas + " cuentas bancarias activas al mismo tiempo; no se puede saber a cuál"
                + " pagarle. Debe quedar una sola activa.");
        }
        return unica;
    }

    /**
     * §6 del contrato, decisión del usuario 2026-09-04: no se genera el pago de un jubilado
     * cuya cuenta bancaria activa no tenga el certificado bancario cargado.
     *
     * ⛔ Distingue DOS causas que no se pueden confundir. {@code obtenerCertificado} devuelve
     * {@code null} cuando la cuenta simplemente no tiene certificado ({@link #ERR_SIN_CERTIFICADO_BANCARIO}
     * — problema DE LA ENTIDAD, hay que pedirle el documento), pero LANZA
     * {@code IncomeException(CuentaBancariaParticipeService.ERR_TIPO_ADJUNTO_NO_CONFIGURADO)}
     * cuando el catálogo CRD.TPDJ no resuelve 'CERTIFICADO BANCARIO' — eso es un problema DEL
     * SISTEMA que afecta a TODOS los jubilados por igual, y se relanza acá con
     * {@link #ERR_CERTIFICADO_NO_VERIFICABLE} para que no se confunda con "le falta el
     * documento" (si se confunde, el operador termina pidiéndole el certificado a 187 personas
     * que quizá ya lo entregaron).
     *
     * ⛔⛔ DEPENDE de que CRD.TPDJ tenga UNA SOLA fila activa llamada 'CERTIFICADO BANCARIO'.
     * Al 2026-09-04 tiene DOS (ids 4 y 37, sql/192) y
     * {@code CuentaBancariaParticipeServiceImpl.resolverTipoCertificadoBancario()} resuelve con
     * {@code tipos.get(0)} sobre una consulta sin {@code ORDER BY}: mientras eso siga así, esta
     * validación puede rechazar a jubilados que SÍ tienen su certificado (el {@code get(0)}
     * puede devolver el tipo que no es, y los adjuntos del otro tipo quedan invisibles). El dato
     * se corrige con sql/193; esta validación no lo arregla ni lo detecta.
     */
    private void validarCertificadoBancario(CuentaBancariaParticipe cuenta, Long idEntidad) throws Throwable {
        com.saa.model.crd.Adjunto certificado;
        try {
            certificado = cuentaBancariaParticipeService.obtenerCertificado(cuenta.getCodigo());
        } catch (IncomeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(
                    com.saa.ejb.crd.service.CuentaBancariaParticipeService.ERR_TIPO_ADJUNTO_NO_CONFIGURADO)) {
                throw new IncomeException(ERR_CERTIFICADO_NO_VERIFICABLE + ": no se pudo verificar el"
                    + " certificado bancario porque el catálogo CRD.TPDJ no resuelve"
                    + " 'CERTIFICADO BANCARIO'. Es un problema de configuración del sistema y afecta a"
                    + " TODOS los jubilados por igual, no un documento faltante de la entidad "
                    + idEntidad + ".");
            }
            throw e;
        }
        if (certificado == null) {
            throw new IncomeException(ERR_SIN_CERTIFICADO_BANCARIO + ": la cuenta bancaria "
                + cuenta.getCodigo() + " de la entidad " + idEntidad + " no tiene el certificado"
                + " bancario cargado; no se puede generar su pago de pensión.");
        }
    }

    /**
     * Resuelve si hay a quién pagarle el remanente de una entidad — §6/D2 del contrato: el
     * certificado gobierna la SALIDA, nunca el cruce. Extraído para que {@link #generarPagoIndividual}
     * y la previsualización ({@code previsualizarCorrida}) usen la MISMA regla, no dos copias.
     *
     * @return la cuenta con certificado válido, o {@code null} si no hay a quién pagarle (sin
     *         cuenta activa, cuenta ambigua, o sin certificado — ninguno de los tres aborta al
     *         llamador, es información, no un fallo)
     * @throws IncomeException {@link #ERR_CERTIFICADO_NO_VERIFICABLE} SÍ se propaga: el
     *                          catálogo CRD.TPDJ mal configurado no se puede verificar para
     *                          NADIE, y eso sí debe abortar (no es "este jubilado no tiene
     *                          cuenta", es "no sé si nadie tiene o no").
     */
    private CuentaBancariaParticipe resolverCuentaSalida(Long idEntidad) throws Throwable {
        try {
            CuentaBancariaParticipe candidata = unicaCuentaActiva(idEntidad);
            validarCertificadoBancario(candidata, idEntidad);
            return candidata;
        } catch (IncomeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith(ERR_CERTIFICADO_NO_VERIFICABLE)) {
                throw e;
            }
            System.out.println("  Entidad " + idEntidad + " sin salida de remanente disponible"
                + " (el cruce contra préstamo, si aplica, sigue igual, D2): " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea la fila NEGATIVA en CRD.APRT (tipo 23, pensión complementaria) del pago mensual.
     * {@code tipoMovimiento = PAGO_PENSION}, distinto de {@code JUBILACION} (el traslado
     * inicial es único, esto es recurrente). {@code periodoDevengo = null}: no corresponde al
     * aporte esperado de ningún mes, es un descuento de saldo ya trasladado.
     *
     * @param fechaHecho fecha de NEGOCIO del movimiento (fin de mes del período, §6bis) — no
     *                   confundir con la auditoría, que este método fija a {@code now()} por
     *                   su cuenta en {@code fechaRegistro}.
     */
    private Aporte crearMovimientoNegativo(Entidad entidad, double valor, String glosa,
            LocalDateTime fechaHecho, String usuario) throws Throwable {
        TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), TIPO_APORTE_PENSION_COMPLEMENTARIA);
        if (tipo == null) {
            throw new IncomeException("No existe el tipo de aporte " + TIPO_APORTE_PENSION_COMPLEMENTARIA
                + " (pensión complementaria) en el catálogo CRD.TPAP.");
        }

        Aporte aporte = new Aporte();
        aporte.setEntidad(entidad);
        aporte.setFilial(entidad.getFilial());
        aporte.setTipoAporte(tipo);
        aporte.setValor(redondear(-valor));
        aporte.setValorPagado(0.0);
        aporte.setSaldo(0.0);
        aporte.setEstado((long) EstadoCuotaPrestamo.PAGADA);
        aporte.setIdAsoprep(null);
        aporte.setFechaTransaccion(fechaHecho);
        aporte.setPeriodoDevengo(null);
        aporte.setTipoMovimiento((long) CrdTipoMovimientoAporte.PAGO_PENSION);
        aporte.setGlosa(glosa);
        aporte.setUsuarioRegistro(usuario);
        aporte.setFechaRegistro(LocalDateTime.now());
        // DAO directo: saveSingle forzaría estado = 1 (Estado.ACTIVO) en todo INSERT, pisando
        // el PAGADA(4) recién asignado — mismo motivo que en el resto del módulo.
        aporte = aporteDaoService.save(aporte, null);

        PagoAporte pagoAporte = new PagoAporte();
        pagoAporte.setAporte(aporte);
        pagoAporte.setFilial(entidad.getFilial());
        pagoAporte.setValor(redondear(valor));
        pagoAporte.setFechaContable(fechaHecho);
        pagoAporte.setNumeroAsiento(null);
        pagoAporte.setConcepto(glosa);
        pagoAporte.setUsuarioRegistro(usuario);
        pagoAporte.setFechaRegistro(LocalDateTime.now());
        pagoAporte.setEstado(1L);
        pagoAporte.setPagoPrestamo(null);
        pagoAporteDaoService.save(pagoAporte, null);

        return aporte;
    }

    /**
     * Contra-movimiento POSITIVO cuando un pago se rechaza — nunca se borra ni se edita la
     * fila negativa (CRD.APRT es append-only). Mismo patrón que
     * {@code DevolucionAporteServiceImpl#generarContraMovimientos}.
     */
    private void generarContraMovimiento(PagoPensionComplementaria pago) throws Throwable {
        if (pago.getIdAporte() == null) {
            return;
        }
        Aporte original = aporteDaoService.find(new Aporte(), pago.getIdAporte());
        if (original == null) {
            System.err.println("  ⚠ Pago " + pago.getCodigo() + " rechazado, pero su Aporte "
                + pago.getIdAporte() + " ya no existe — no se genera contra-movimiento.");
            return;
        }
        double valor = Math.abs(original.getValor() != null ? original.getValor() : 0.0);
        LocalDateTime ahora = LocalDateTime.now();

        Aporte reverso = new Aporte();
        reverso.setEntidad(original.getEntidad());
        reverso.setFilial(original.getFilial());
        reverso.setTipoAporte(original.getTipoAporte());
        reverso.setValor(redondear(valor));
        reverso.setValorPagado(0.0);
        reverso.setSaldo(0.0);
        reverso.setEstado((long) EstadoCuotaPrestamo.PAGADA);
        reverso.setIdAsoprep(null);
        reverso.setFechaTransaccion(ahora);
        reverso.setPeriodoDevengo(null);
        reverso.setTipoMovimiento((long) CrdTipoMovimientoAporte.REVERSO);
        reverso.setGlosa("REVERSO PAGO PENSION COMPLEMENTARIA " + pago.getMes() + "/" + pago.getAnio()
            + " - PGPC " + pago.getCodigo() + " - Pago rechazado");
        reverso.setUsuarioRegistro(pago.getUsuarioRegistro());
        reverso.setFechaRegistro(ahora);
        reverso = aporteDaoService.save(reverso, null);

        PagoAporte pagoReverso = new PagoAporte();
        pagoReverso.setAporte(reverso);
        pagoReverso.setFilial(original.getFilial());
        pagoReverso.setValor(redondear(valor));
        pagoReverso.setFechaContable(ahora);
        pagoReverso.setNumeroAsiento(null);
        pagoReverso.setConcepto(reverso.getGlosa());
        pagoReverso.setUsuarioRegistro(pago.getUsuarioRegistro());
        pagoReverso.setFechaRegistro(ahora);
        pagoReverso.setEstado(1L);
        pagoReverso.setPagoPrestamo(null);
        pagoAporteDaoService.save(pagoReverso, null);

        System.out.println("  ↩ Contra-movimiento APRT " + reverso.getCodigo() + " (+$" + valor + ")");
    }

    private void acumular(ResultadoSincronizacion resumen, ResultadoSincronizacion parcial) {
        if (parcial == null) {
            return;
        }
        resumen.setEvaluadas(resumen.getEvaluadas() + parcial.getEvaluadas());
        resumen.setMarcadasPagadas(resumen.getMarcadasPagadas() + parcial.getMarcadasPagadas());
        resumen.setMarcadasRechazadas(resumen.getMarcadasRechazadas() + parcial.getMarcadasRechazadas());
        resumen.setHuerfanas(resumen.getHuerfanas() + parcial.getHuerfanas());
        resumen.setConError(resumen.getConError() + parcial.getConError());
        for (String error : parcial.getErrores()) {
            resumen.getErrores().add(error);
        }
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }
}
