package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.PagoPensionComplementariaDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.PagoPensionComplementariaService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.PagoPensionComplementaria;
import com.saa.model.crd.TipoAporte;
import com.saa.model.crd.ValorPagoPensionComplementaria;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.CrdTipoMovimientoAporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPagoPensionComplementaria;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.OrigenPagoExterno;

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

        ResultadoGeneracionPagosPension resumen = new ResultadoGeneracionPagosPension();
        resumen.setAnio(anio);
        resumen.setMes(mes);

        List<Entidad> jubilados = entidadDaoService.selectByIdEstado(
            Long.valueOf(EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO));
        int universo = (jubilados != null) ? jubilados.size() : 0;
        System.out.println("Jubilados JUBILADO_COMPLEMENTARIO a evaluar: " + universo);

        if (jubilados != null) {
            for (Entidad jubilado : jubilados) {
                resumen.setEvaluados(resumen.getEvaluados() + 1);
                try {
                    // A través del proxy: cada jubilado en su propia transacción
                    boolean generado = self.generarPagoIndividual(jubilado.getCodigo(), idEmpresa, anio, mes, usuario);
                    if (generado) {
                        resumen.setGenerados(resumen.getGenerados() + 1);
                    } else {
                        resumen.setYaGenerados(resumen.getYaGenerados() + 1);
                    }
                } catch (Throwable e) {
                    resumen.setConError(resumen.getConError() + 1);
                    resumen.getErrores().add("Entidad " + jubilado.getCodigo() + ": " + e.getMessage());
                    System.err.println("Error al generar el pago de pensión de la entidad "
                        + jubilado.getCodigo() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("GENERACIÓN TERMINADA - Evaluados: " + resumen.getEvaluados()
            + " - Generados: " + resumen.getGenerados()
            + " - Ya generados: " + resumen.getYaGenerados()
            + " - Con error: " + resumen.getConError());

        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean generarPagoIndividual(Long idEntidad, Long idEmpresa, Integer anio, Integer mes, String usuario)
            throws Throwable {
        System.out.println("PagoPensionComplementariaService.generarPagoIndividual - Entidad: " + idEntidad
            + " - Período: " + mes + "/" + anio);

        // Idempotencia: si ya existe, no se duplica (la UNIQUE de la base es la garantía real;
        // este chequeo evita el viaje a la excepción de constraint y da un mensaje claro).
        PagoPensionComplementaria existente = pagoPensionDaoService.selectByEntidadYPeriodo(
            idEntidad, anio.longValue(), mes.longValue());
        if (existente != null) {
            System.out.println("  Entidad " + idEntidad + " ya tiene PGPC " + existente.getCodigo()
                + " para " + mes + "/" + anio + " - se omite");
            return false;
        }

        Entidad entidad = entidadDaoService.find(new Entidad(), idEntidad);
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe " + idEntidad);
        }

        // VPPC: exactamente una configuración activa. Ni cero (no se sabe cuánto pagarle) ni
        // más de una (no se sabe cuál vale — mismo criterio que la cuenta bancaria).
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

        // Saldo suficiente en pensión complementaria (23): guardarraíl anti-carrera, revalidado
        // dentro de la transacción, mismo criterio que consumirAportes.
        double saldo = saldoAporteService.saldoPorEntidadYTipo(idEntidad, TIPO_APORTE_PENSION_COMPLEMENTARIA);
        if (saldo < valorTotal - TOLERANCIA) {
            throw new IncomeException(ERR_SALDO_INSUFICIENTE + ": el saldo de pensión complementaria"
                + " de la entidad " + idEntidad + " es $" + redondear(saldo) + " y el pago del período"
                + " requiere $" + valorTotal + ".");
        }

        LocalDate fecha = LocalDate.of(anio, mes, 1);
        LocalDateTime fechaHora = LocalDateTime.now();

        // Movimiento NEGATIVO en CRD.APRT — el saldo de pensión complementaria baja.
        String glosa = "PAGO PENSION COMPLEMENTARIA " + mes + "/" + anio + " - Entidad " + idEntidad;
        Aporte aporte = crearMovimientoNegativo(entidad, valorTotal, glosa, fechaHora, usuario);

        // Cuenta bancaria: exactamente una activa. Sin operador que elija, no se puede adivinar.
        CuentaBancariaParticipe cuenta = unicaCuentaActiva(idEntidad);

        // Cabecera PGPC — REGISTRADA, antes de la orden de pago (si CXP falla, se revierte
        // todo: no queda un movimiento de APRT huérfano sin orden de pago).
        PagoPensionComplementaria pago = new PagoPensionComplementaria();
        pago.setEntidad(entidad);
        pago.setFilial(entidad.getFilial());
        pago.setAnio(anio.longValue());
        pago.setMes(mes.longValue());
        pago.setValorPension(valorPension);
        pago.setValorSeguro(valorSeguro);
        pago.setValor(valorTotal);
        pago.setFecha(fecha);
        pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.REGISTRADA));
        pago.setIdAporte(aporte.getCodigo());
        pago.setUsuarioRegistro(usuario);
        pago.setFechaRegistro(fechaHora);
        pago = pagoPensionDaoService.save(pago, null);

        System.out.println("  PGPC " + pago.getCodigo() + " creado por $" + valorTotal);

        Long idPago;
        try {
            BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
            beneficiario.setNombre(entidad.getRazonSocial());
            beneficiario.setIdentificacion(entidad.getNumeroIdentificacion());
            beneficiario.setIdBancoExterno(cuenta.getBancoExterno() != null ? cuenta.getBancoExterno().getCodigo() : null);
            beneficiario.setTipoCuenta(cuenta.getTipoCuenta());
            beneficiario.setNumeroCuenta(cuenta.getNumeroCuenta());

            String observacion = "Pago pensión complementaria " + mes + "/" + anio + " - "
                + entidad.getRazonSocial() + " (PGPC " + pago.getCodigo() + ")";

            // idCuentaBancariaOrigen SIEMPRE null: tesorería asigna cuenta/forma de pago al
            // aprobar — mismo criterio que DevolucionAporteServiceImpl (punto 14, 2026-08-27).
            java.util.Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.CRD_PAGO_PENSION_COMPLEMENTARIA, pago.getCodigo(),
                idEmpresa, null, valorTotal, fecha.toString(), beneficiario,
                null, // sin desglose contable — mismo estado que la devolución hoy (§6.5.b)
                observacion, null, false, null);

            Object valorPago = (respuesta != null) ? respuesta.get("pago") : null;
            if (valorPago == null) {
                throw new IncomeException("Cuentas por Pagar no devolvió el número de la orden.");
            }
            idPago = ((Number) valorPago).longValue();

        } catch (IncomeException e) {
            throw new IncomeException("No se pudo generar la orden de pago en Cuentas por Pagar"
                + " para la entidad " + idEntidad + ": " + e.getMessage());
        } catch (Throwable e) {
            throw new IncomeException("No se pudo generar la orden de pago en Cuentas por Pagar"
                + " para la entidad " + idEntidad + ": " + e.getMessage());
        }

        pago.setIdPagoProgramado(idPago);
        pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.EN_PAGO));
        pagoPensionDaoService.save(pago, pago.getCodigo());

        System.out.println("  ✅ Pago de pensión registrado - Entidad " + idEntidad + " - PGPC "
            + pago.getCodigo() + " - Orden de pago " + idPago + " - $" + valorTotal);

        return true;
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
     * Crea la fila NEGATIVA en CRD.APRT (tipo 23, pensión complementaria) del pago mensual.
     * {@code tipoMovimiento = PAGO_PENSION}, distinto de {@code JUBILACION} (el traslado
     * inicial es único, esto es recurrente). {@code periodoDevengo = null}: no corresponde al
     * aporte esperado de ningún mes, es un descuento de saldo ya trasladado.
     */
    private Aporte crearMovimientoNegativo(Entidad entidad, double valor, String glosa,
            LocalDateTime fechaHora, String usuario) throws Throwable {
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
        aporte.setFechaTransaccion(fechaHora);
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
        pagoAporte.setFechaContable(fechaHora);
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
}
