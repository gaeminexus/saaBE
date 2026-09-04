package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AporteDaoService;
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
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoGeneracionPagosPension;
import com.saa.ejb.crd.service.dto.ResultadoPagoConAportes;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
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

        if (jubilados != null) {
            for (Entidad jubilado : jubilados) {
                resumen.setEvaluados(resumen.getEvaluados() + 1);
                try {
                    // A través del proxy: cada jubilado en su propia transacción
                    DetallePagoPension detalle = self.generarPagoIndividual(
                        jubilado.getCodigo(), idEmpresa, anio, mes, usuario, idUsuario);
                    resumen.getDetalle().add(detalle);

                    if ("YA_EXISTIA".equals(detalle.getEstado())) {
                        resumen.setYaGenerados(resumen.getYaGenerados() + 1);
                    } else {
                        resumen.setGenerados(resumen.getGenerados() + 1);
                        totalPagado += nvl(detalle.getValorPension()) + nvl(detalle.getValorSeguroSalud());
                        totalCruzado += nvl(detalle.getValorCruzadoAPrestamo());
                        totalOrdenes += nvl(detalle.getValorOrdenPago());
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
                    detalleError.setMensaje(e.getMessage());
                    resumen.getDetalle().add(detalleError);
                }
            }
        }

        resumen.setTotalPagado(redondear(totalPagado));
        resumen.setTotalCruzadoAPrestamos(redondear(totalCruzado));
        resumen.setTotalOrdenesGeneradas(redondear(totalOrdenes));

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

        // Idempotencia: si ya existe, no se duplica (la UNIQUE de la base es la garantía real;
        // este chequeo evita el viaje a la excepción de constraint y da un mensaje claro).
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

        // 2026-09-04, decisión del usuario, segunda vuelta (API-PAGO-PENSION-COMPLEMENTARIA.md
        // §6bis): la fecha del hecho de CARTERA es min(último día del mes del período, hoy) —
        // nunca el día 1, y nunca una fecha futura. Un período cerrado (agosto corrido en
        // septiembre) se fecha a fin de mes; un período corrido DENTRO de su propio mes
        // (septiembre corrido el 20 de septiembre) se fecha al día del proceso, porque fin de
        // mes todavía no llegó. Por construcción nunca da futuro, así que el circuito no choca
        // con validarFechaNoFutura de pagarConAportes (la mina de la primera versión,
        // fin de mes incondicional, commit 79204e4).
        LocalDate finDeMes = YearMonth.of(anio, mes).atEndOfMonth();
        LocalDate hoy = LocalDate.now();
        LocalDate fecha = finDeMes.isAfter(hoy) ? hoy : finDeMes;
        // Auditoría — cuándo se registró de verdad. Separada a propósito de fechaHecho: reusar
        // una sola fecha para las dos cosas fue el defecto original.
        LocalDateTime fechaRegistro = LocalDateTime.now();
        // El hecho de cartera, a la hora 00:00 — mismo patrón que
        // AporteServiceImpl.procesarJubilacion (fechaEfectiva.atStartOfDay()).
        LocalDateTime fechaHecho = fecha.atStartOfDay();

        // §3 PLAN-PAGO-JUBILADOS.md: cruzar contra préstamo vigente ANTES de decidir cuánto
        // sale al banco. Orquesta pagarConAportes (motor de pagos en producción) — no se
        // reimplementa la cascada acá.
        double montoCruzado = cruzarContraPrestamos(entidad, vppc, valorTotal, idEmpresa, fecha,
            usuario, mes, anio);
        double remanente = redondear(valorTotal - montoCruzado);

        // Movimiento NEGATIVO en CRD.APRT — SOLO por el remanente que sale al banco. El tramo
        // cruzado ya generó su propio movimiento NEGATIVO (tipo PAGO_PRESTAMO) dentro de
        // pagarConAportes/consumirAportes — duplicarlo acá bajaría el saldo del aporte dos
        // veces por la misma plata. Entre los dos movimientos, la baja total del aporte tipo 23
        // sigue reflejando el total consumido (cruce + remanente), como pide el §3.
        Aporte aporte = null;
        if (remanente > TOLERANCIA) {
            String glosa = "PAGO PENSION COMPLEMENTARIA " + mes + "/" + anio + " - Entidad " + idEntidad;
            aporte = crearMovimientoNegativo(entidad, remanente, glosa, fechaHecho, usuario);
        }

        // Cuenta bancaria: exactamente una activa. Solo hace falta si de verdad sale algo al
        // banco — un jubilado 100% cruzado no necesita tener una cuenta bancaria cargada.
        CuentaBancariaParticipe cuenta = remanente > TOLERANCIA ? unicaCuentaActiva(idEntidad) : null;

        // Cabecera PGPC — REGISTRADA, antes de la orden de pago (si CXP falla, se revierte
        // todo: no queda un movimiento de APRT huérfano sin orden de pago). El valor total
        // sigue siendo el devengado completo (pensión + seguro), no el remanente.
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
        pago.setIdAporte(aporte != null ? aporte.getCodigo() : null);
        pago.setUsuarioRegistro(usuario);
        pago.setFechaRegistro(fechaRegistro);
        pago = pagoPensionDaoService.save(pago, null);

        System.out.println("  PGPC " + pago.getCodigo() + " creado por $" + valorTotal
            + " (cruzado a préstamo: $" + montoCruzado + ", remanente al banco: $" + remanente + ")");

        Long idPago = null;
        // ⛔ §3 PLAN-PAGO-JUBILADOS.md: si el cruce se llevó todo, la orden de pago es CERO y
        // NO se genera — una orden en cero es una orden que tesorería procesa y devuelve.
        if (remanente > TOLERANCIA) {
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
                // §6bis (refinamiento 2026-09-04): el PAGO va con la fecha ACTUAL, separado de
                // lo de cartera — "fecha" es del hecho de cartera (cruce/APRT/contable), no del
                // pago que sale hoy al banco.
                java.util.Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
                    OrigenPagoExterno.CRD_PAGO_PENSION_COMPLEMENTARIA, pago.getCodigo(),
                    idEmpresa, null, remanente, LocalDate.now().toString(), beneficiario,
                    null, // sin desglose contable — mismo estado que la devolución hoy (§6.5.b)
                    observacion, idUsuario, false, null);

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
        } else {
            // 100% cruzado contra préstamo: no hay nada que esperar del banco. El pago existe,
            // se contabiliza (ver el devengo más abajo), y no hay salida de dinero.
            pago.setEstado(Long.valueOf(EstadoPagoPensionComplementaria.PAGADA));
            pago.setFechaPago(fecha);
            System.out.println("  ℹ️ PGPC " + pago.getCodigo() + " sin orden de pago: la deuda"
                + " de préstamo consumió toda la pensión del mes.");
        }

        // §4 PLAN-PAGO-JUBILADOS.md: el devengo lo genera CRD, DESPUÉS de la orden de pago (o
        // de decidir que no la hay) — mismo orden invertido que
        // DevolucionAporteServiceImpl#generarAsientoReclasificacion: si el devengo fallara acá,
        // la transacción entera se revierte y la orden de pago tampoco queda.
        // sql/175 (2026-09-02): el devengo va en numeroAsientoDevengo, NO en numeroAsiento —
        // esa columna es del asiento del PAGO (CXP), que la escribe sincronizarPago al
        // confirmarse. Mismo criterio que DVAP.numeroAsientoReclasificacion / numeroAsiento.
        Long idAsientoDevengo = generarAsientoDevengoPension(pago, entidad, idEmpresa);
        pago.setNumeroAsientoDevengo(idAsientoDevengo);
        pagoPensionDaoService.save(pago, pago.getCodigo());

        System.out.println("  ✅ Pago de pensión registrado - Entidad " + idEntidad + " - PGPC "
            + pago.getCodigo() + " - Orden de pago " + idPago + " - Cruzado: $" + montoCruzado
            + " - Remanente: $" + remanente);

        DetallePagoPension detalle = new DetallePagoPension();
        detalle.setIdEntidad(idEntidad);
        detalle.setNombre(entidad.getRazonSocial());
        detalle.setIdPago(pago.getCodigo());
        detalle.setValorPension(valorPension);
        detalle.setValorSeguroSalud(valorSeguro);
        detalle.setValorCruzadoAPrestamo(montoCruzado);
        detalle.setValorOrdenPago(remanente);
        detalle.setGeneroOrdenPago(idPago != null);
        detalle.setIdAsientoDevengo(idAsientoDevengo);
        detalle.setEstado("GENERADO");
        return detalle;
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
