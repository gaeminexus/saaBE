package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.dao.DetalleDevolucionAporteDaoService;
import com.saa.ejb.crd.dao.DevolucionAporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.DevolucionAporteService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.DetalleResultadoDevolucion;
import com.saa.ejb.crd.service.dto.DetalleSolicitudDevolucion;
import com.saa.ejb.crd.service.dto.ResultadoDevolucionAporte;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.crd.service.dto.SolicitudDevolucionAporte;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.DetalleDevolucionAporte;
import com.saa.model.crd.DevolucionAporte;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;
import com.saa.model.crd.TipoAporte;
import com.saa.model.cxp.PagoProgramado;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoDevolucionAporte;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación de la devolución de aportes a partícipes.
 *
 * <b>Dirección de dependencias</b>: esta clase importa de {@code cxp} (PagoProgramado) y
 * eso está permitido — {@code crd → cxp} es la dirección buena. Lo prohibido es lo
 * contrario: nada de CXP, TSR o CNT puede nombrar a CRD, porque el sistema se comercializa
 * después sin el módulo {@code crd}. Por eso CXP no avisa: CRD consulta
 * ({@link #sincronizarPagos()}).
 *
 * @author Sistema SAA
 * @since 2026-08-24
 */
@Stateless
public class DevolucionAporteServiceImpl implements DevolucionAporteService {

    /** Longitud máxima en BYTES de APRT.APRTGLSA (VARCHAR2(2000)) */
    private static final int MAX_BYTES_GLOSA = 2000;

    /** Tolerancia para comparar valores monetarios */
    private static final double TOLERANCIA = 0.01;

    /** Máximo de errores que se detallan en el resumen de la sincronización */
    private static final int MAX_ERRORES_DETALLADOS = 50;

    @EJB
    private DevolucionAporteDaoService devolucionAporteDaoService;

    @EJB
    private DetalleDevolucionAporteDaoService detalleDevolucionAporteDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @EJB
    private SaldoAporteService saldoAporteService;

    /** crd → cxp: dirección permitida. */
    @EJB
    private PagoProgramadoService pagoProgramadoService;

    /** crd → cxp: dirección permitida. Solo lectura del estado del pago. */
    @EJB
    private PagoProgramadoDaoService pagoProgramadoDaoService;

    /**
     * Auto-inyección: permite que el bucle del lote invoque {@code sincronizarDevolucion} a
     * TRAVÉS del proxy EJB, para que cada devolución corra en su propia transacción
     * (REQUIRES_NEW). Una llamada directa {@code this.sincronizarDevolucion(...)} se
     * saltaría el interceptor y todo el lote quedaría en una sola transacción.
     */
    @EJB
    private DevolucionAporteService self;

    // ========================================================================
    // EntityService
    // ========================================================================

    @Override
    public DevolucionAporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById DevolucionAporte con id: " + id);
        return devolucionAporteDaoService.selectById(id, NombreEntidadesCredito.DEVOLUCION_APORTE);
    }

    @Override
    public List<DevolucionAporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DevolucionAporteService");
        List<DevolucionAporte> result =
                devolucionAporteDaoService.selectAll(NombreEntidadesCredito.DEVOLUCION_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DevolucionAporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<DevolucionAporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DevolucionAporteService");
        List<DevolucionAporte> result = devolucionAporteDaoService.selectByCriteria(datos,
                NombreEntidadesCredito.DEVOLUCION_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException(
                    "Busqueda por criterio DevolucionAporte no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public DevolucionAporte saveSingle(DevolucionAporte devolucion) throws Throwable {
        System.out.println("saveSingle - DevolucionAporte");
        if (devolucion.getCodigo() == null) {
            if (devolucion.getEstado() == null) {
                devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.REGISTRADA));
            }
            if (devolucion.getFechaRegistro() == null) {
                devolucion.setFechaRegistro(LocalDateTime.now());
            }
        }
        return devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());
    }

    @Override
    public void save(List<DevolucionAporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DevolucionAporteService");
        for (DevolucionAporte registro : lista) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DevolucionAporteService");
        DevolucionAporte entidad = new DevolucionAporte();
        for (Long registro : id) {
            devolucionAporteDaoService.remove(entidad, registro);
        }
    }

    // ========================================================================
    // Registro
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoDevolucionAporte registrarDevolucion(SolicitudDevolucionAporte solicitud)
            throws Throwable {

        System.out.println("DevolucionAporteService.registrarDevolucion - Entidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null)
            + " - Lineas: " + (solicitud != null && solicitud.getDetalle() != null
                ? solicitud.getDetalle().size() : 0));

        // ------------------------------------------------------------------
        // VALIDACIÓN (§8.1 del plan, en ese orden)
        // ------------------------------------------------------------------

        // 1. Cuerpo y parámetros obligatorios
        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": no se recibió el cuerpo de la solicitud");
        }
        if (solicitud.getIdEntidad() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        if (solicitud.getIdCuentaBancariaOrigen() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": idCuentaBancariaOrigen es obligatorio");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEmpresa es obligatorio");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        if (solicitud.getDetalle() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": detalle es obligatorio");
        }

        // 2. El partícipe existe. find() devuelve null, a diferencia de selectById
        Entidad entidad = entidadDaoService.find(new Entidad(), solicitud.getIdEntidad());
        if (entidad == null) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el partícipe "
                + solicitud.getIdEntidad());
        }

        // 3. Detalle no vacío y sin tipos repetidos
        List<DetalleSolicitudDevolucion> lineas = solicitud.getDetalle();
        if (lineas.isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": debe indicar al menos un tipo de aporte a devolver");
        }
        Set<Long> vistos = new HashSet<>();
        for (DetalleSolicitudDevolucion linea : lineas) {
            if (linea == null || linea.getIdTipoAporte() == null) {
                throw new IncomeException(ERR_PARAMETRO_INVALIDO
                    + ": cada línea del detalle debe indicar el tipo de aporte");
            }
            if (!vistos.add(linea.getIdTipoAporte())) {
                throw new IncomeException(ERR_TIPO_DUPLICADO + ": el tipo de aporte "
                    + linea.getIdTipoAporte() + " aparece más de una vez en el detalle");
            }
        }

        // 4, 5, 6 y 7. Tipo vigente, producto de pago, valor válido y saldo suficiente
        List<TipoAporte> tipos = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        List<String> tiposSinProducto = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (DetalleSolicitudDevolucion linea : lineas) {

            TipoAporte tipo = tipoAporteDaoService.find(new TipoAporte(), linea.getIdTipoAporte());
            if (tipo == null) {
                throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE
                    + ": no existe el tipo de aporte " + linea.getIdTipoAporte());
            }
            if (tipo.getEstado() == null || tipo.getEstado().longValue() != Estado.ACTIVO) {
                throw new IncomeException(ERR_TIPO_APORTE_NO_VIGENTE + ": el tipo de aporte "
                    + tipo.getCodigo() + " (" + tipo.getNombre() + ") no está vigente");
            }
            // El producto de pago clasifica contablemente la devolución de este tipo.
            // Desde el 2026-08-24 es OPCIONAL (§6.5.b): solo se anota qué tipos lo tienen
            // y cuáles no; la regla de todo-o-nada se resuelve al terminar el recorrido.
            if (tipo.getProductoPago() == null) {
                tiposSinProducto.add(tipo.getCodigo() + " (" + tipo.getNombre() + ")");
            }

            double valor = redondear(linea.getValor() != null ? linea.getValor() : 0.0);
            if (valor <= 0.0) {
                throw new IncomeException(ERR_VALOR_INVALIDO + ": el valor a devolver del tipo "
                    + tipo.getNombre() + " debe ser mayor a cero");
            }

            double disponible = saldoAporteService.saldoPorEntidadYTipo(
                entidad.getCodigo(), tipo.getCodigo());
            if (valor > disponible + TOLERANCIA) {
                throw new IncomeException(ERR_SALDO_INSUFICIENTE + ": el tipo " + tipo.getNombre()
                    + " tiene $" + String.format(Locale.US, "%.2f", disponible)
                    + " disponibles y se piden $" + String.format(Locale.US, "%.2f", valor));
            }

            tipos.add(tipo);
            valores.add(valor);
            total = total.add(BigDecimal.valueOf(valor));
        }
        double valorTotal = redondear(total.doubleValue());

        // 5. Producto de pago: TODO O NADA (§6.5.b).
        //
        //   Todos los tipos lo tienen   -> se manda el desglose; al confirmarse el pago hay
        //                                  asiento y movimiento bancario, como siempre.
        //   Ninguno lo tiene            -> no se manda desglose; el pago se confirma SIN
        //                                  contabilidad. Es la decisión del 2026-08-24.
        //   Algunos sí y otros no       -> se rechaza.
        //
        // El caso mezclado TIENE que fallar: un desglose parcial genera un asiento donde las
        // líneas DEBE suman menos que el HABER al banco. Un asiento descuadrado es peor que
        // no tener asiento.
        boolean contabiliza = tiposSinProducto.isEmpty();
        if (!contabiliza && tiposSinProducto.size() < tipos.size()) {
            throw new IncomeException(ERR_TIPO_APORTE_SIN_PRODUCTO + ": la devolución mezcla "
                + "tipos con y sin producto de pago parametrizado, y eso generaría un asiento "
                + "descuadrado. Sin producto de pago: " + String.join(", ", tiposSinProducto)
                + ". Cárguelos en el catálogo de tipos de aporte, o quítelos de esta devolución.");
        }

        // 8. Fecha no futura
        LocalDate fecha = solicitud.getFecha() != null ? solicitud.getFecha() : LocalDate.now();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": la fecha " + fecha + " es futura");
        }

        // 9. Cuenta bancaria del partícipe (salvo débito automático, que no transfiere)
        CuentaBancariaParticipe cuentaParticipe = null;
        if (!solicitud.isDebitoAutomatico()) {
            if (solicitud.getIdCuentaBancariaParticipe() == null) {
                throw new IncomeException(ERR_SIN_CUENTA_BANCARIA + ": debe indicar la cuenta "
                    + "bancaria del partícipe a la que se transfiere el dinero");
            }
            cuentaParticipe = cuentaBancariaParticipeDaoService.find(
                new CuentaBancariaParticipe(), solicitud.getIdCuentaBancariaParticipe());
            if (cuentaParticipe == null) {
                throw new IncomeException(ERR_CUENTA_NO_ENCONTRADA + ": no existe la cuenta "
                    + "bancaria " + solicitud.getIdCuentaBancariaParticipe());
            }
            if (cuentaParticipe.getEntidad() == null
                    || !cuentaParticipe.getEntidad().getCodigo().equals(entidad.getCodigo())) {
                throw new IncomeException(ERR_SIN_CUENTA_BANCARIA + ": la cuenta bancaria "
                    + cuentaParticipe.getCodigo() + " pertenece a otro partícipe");
            }
            if (cuentaParticipe.getEstado() == null
                    || cuentaParticipe.getEstado().longValue() != Estado.ACTIVO) {
                throw new IncomeException(ERR_SIN_CUENTA_BANCARIA + ": la cuenta bancaria "
                    + cuentaParticipe.getCodigo() + " no está activa");
            }
        } else if (solicitud.getIdCuentaBancariaParticipe() != null) {
            // El débito automático no exige cuenta, pero si viene se conserva como dato.
            cuentaParticipe = cuentaBancariaParticipeDaoService.find(
                new CuentaBancariaParticipe(), solicitud.getIdCuentaBancariaParticipe());
        }

        // ------------------------------------------------------------------
        // EJECUCIÓN — todo en esta transacción. Si CXP falla, se revierte todo.
        // ------------------------------------------------------------------

        // 2. Cabecera de la devolución
        DevolucionAporte devolucion = new DevolucionAporte();
        devolucion.setEntidad(entidad);
        devolucion.setFilial(entidad.getFilial());
        devolucion.setCuentaParticipe(cuentaParticipe);
        devolucion.setValor(valorTotal);
        devolucion.setFecha(fecha);
        devolucion.setMotivo(solicitud.getMotivo());
        devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.REGISTRADA));
        devolucion.setIdEmpresa(solicitud.getIdEmpresa());
        devolucion.setUsuarioRegistro(solicitud.getUsuario().trim());
        devolucion.setFechaRegistro(LocalDateTime.now());
        devolucion = devolucionAporteDaoService.save(devolucion, null);

        System.out.println("  DVAP " + devolucion.getCodigo() + " creada por $" + valorTotal);

        // Si es hoy se conserva la hora del reloj; si es una fecha pasada, el inicio del día
        LocalDateTime fechaHora = fecha.isEqual(LocalDate.now())
            ? LocalDateTime.now() : fecha.atStartOfDay();

        ResultadoDevolucionAporte resultado = new ResultadoDevolucionAporte();
        resultado.setIdDevolucion(devolucion.getCodigo());
        resultado.setIdEntidad(entidad.getCodigo());
        resultado.setValorTotal(valorTotal);
        resultado.setFecha(fecha);

        List<LineaContablePago> desglose = new ArrayList<>();

        // 3. Una DDVA y una fila NEGATIVA de CRD.APRT por tipo
        for (int i = 0; i < tipos.size(); i++) {

            TipoAporte tipo = tipos.get(i);
            double valor = valores.get(i);

            // Guardarraíl anti-carrera: se revalida el saldo DENTRO de la transacción,
            // inmediatamente antes de insertar la fila negativa. Es el mismo guardarraíl
            // del paso 3a de pagarConAportes.
            double disponible = saldoAporteService.saldoPorEntidadYTipo(
                entidad.getCodigo(), tipo.getCodigo());
            if (valor > disponible + TOLERANCIA) {
                throw new IncomeException(ERR_SALDO_INSUFICIENTE + ": el saldo del tipo "
                    + tipo.getNombre() + " cambió durante la operación. Disponible: $"
                    + String.format(Locale.US, "%.2f", disponible)
                    + ", solicitado: $" + String.format(Locale.US, "%.2f", valor));
            }

            DetalleDevolucionAporte detalle = new DetalleDevolucionAporte();
            detalle.setDevolucion(devolucion);
            detalle.setTipoAporte(tipo);
            detalle.setValor(valor);
            detalle = detalleDevolucionAporteDaoService.save(detalle, null);

            String glosa = truncarPorBytes("DEVOLUCION APORTES " + tipo.getNombre()
                + " - Devolucion " + devolucion.getCodigo(), MAX_BYTES_GLOSA);

            // Fila NEGATIVA y YA PAGADA: baja el saldo disponible y queda fuera del FIFO
            // petro (selectMinAporteConSaldo exige saldo > 0.01 y estado PARCIAL).
            Aporte aporte = new Aporte();
            aporte.setEntidad(entidad);
            aporte.setFilial(entidad.getFilial());
            aporte.setTipoAporte(tipo);
            aporte.setValor(-valor);
            aporte.setValorPagado(0.0);
            aporte.setSaldo(0.0);
            aporte.setEstado((long) EstadoCuotaPrestamo.PAGADA);
            aporte.setIdAsoprep(null);
            aporte.setFechaTransaccion(fechaHora);
            aporte.setGlosa(glosa);
            aporte.setUsuarioRegistro(solicitud.getUsuario().trim());
            aporte.setFechaRegistro(LocalDateTime.now());
            // DAO directo: saveSingle forzaría estado = 1 y la fila volvería a ser visible
            // para el FIFO del proceso Petro, que se la cobraría de nuevo al socio.
            aporte = aporteDaoService.save(aporte, null);

            PagoAporte pagoAporte = new PagoAporte();
            pagoAporte.setAporte(aporte);
            pagoAporte.setFilial(entidad.getFilial());
            pagoAporte.setValor(valor);
            pagoAporte.setFechaContable(fechaHora);
            pagoAporte.setNumeroAsiento(null);
            pagoAporte.setConcepto(glosa);
            pagoAporte.setUsuarioRegistro(solicitud.getUsuario().trim());
            pagoAporte.setFechaRegistro(LocalDateTime.now());
            pagoAporte.setEstado(Long.valueOf(Estado.ACTIVO));
            pagoAporte.setPagoPrestamo(null);
            pagoAporte = pagoAporteDaoService.save(pagoAporte, null);

            detalle.setIdAporte(aporte.getCodigo());
            detalle.setIdPagoAporte(pagoAporte.getCodigo());
            detalleDevolucionAporteDaoService.save(detalle, detalle.getCodigo());

            // Línea del desglose contable que viaja a CXP. Solo cuando TODOS los tipos
            // tienen producto de pago (§6.5.b): si no, la devolución va sin desglose y el
            // pago se confirma sin asiento.
            if (contabiliza) {
                LineaContablePago lineaContable = new LineaContablePago();
                lineaContable.setIdProductoPago(tipo.getProductoPago());
                lineaContable.setValor(valor);
                lineaContable.setConcepto("Devolucion aportes " + tipo.getNombre()
                    + " - " + entidad.getRazonSocial());
                desglose.add(lineaContable);
            }

            DetalleResultadoDevolucion detalleResultado = new DetalleResultadoDevolucion();
            detalleResultado.setIdTipoAporte(tipo.getCodigo());
            detalleResultado.setNombreTipoAporte(tipo.getNombre());
            detalleResultado.setValor(valor);
            detalleResultado.setIdAporteGenerado(aporte.getCodigo());
            detalleResultado.setIdPagoAporteGenerado(pagoAporte.getCodigo());
            resultado.getDetalle().add(detalleResultado);

            System.out.println("  APRT " + aporte.getCodigo() + " (negativo $" + valor + ")"
                + ", PGAP " + pagoAporte.getCodigo() + " - Tipo " + tipo.getCodigo());
        }

        if (!contabiliza) {
            System.out.println("  ⚠ Devolución " + devolucion.getCodigo() + " SIN desglose "
                + "contable: ningún tipo de aporte tiene producto de pago parametrizado. "
                + "El pago se confirmará sin asiento ni movimiento bancario (§6.5.b).");
        }

        // 4. La orden de pago en CXP. Si esto lanza, se revierte TODO lo anterior:
        //    no quedan aportes negativos huérfanos sin orden de pago.
        Long idPago;
        try {
            BeneficiarioOcasional beneficiario = armaBeneficiario(entidad, cuentaParticipe);

            String observacion = "Devolución de aportes N° " + devolucion.getCodigo()
                + " - " + entidad.getRazonSocial()
                + (solicitud.getMotivo() != null && !solicitud.getMotivo().trim().isEmpty()
                    ? " | " + solicitud.getMotivo().trim() : "");

            Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.CRD_DEVOLUCION_APORTE, devolucion.getCodigo(),
                solicitud.getIdEmpresa(), solicitud.getIdCuentaBancariaOrigen(), valorTotal,
                fecha.toString(), beneficiario,
                // null, no lista vacía: es la forma en que este servicio dice "sin desglose".
                contabiliza ? desglose : null,
                observacion, solicitud.getIdUsuario(),
                solicitud.isDebitoAutomatico(), solicitud.getReferencia());

            Object valorPago = (respuesta != null) ? respuesta.get("pago") : null;
            if (valorPago == null) {
                throw new IncomeException("Cuentas por Pagar no devolvió el número de la orden.");
            }
            idPago = ((Number) valorPago).longValue();

        } catch (IncomeException e) {
            // Se reetiqueta con el código estable, conservando el mensaje accionable de CXP
            throw new IncomeException(ERR_ERROR_ORDEN_PAGO
                + ": no se pudo generar la orden de pago en Cuentas por Pagar. " + e.getMessage());
        } catch (Throwable e) {
            throw new IncomeException(ERR_ERROR_ORDEN_PAGO
                + ": no se pudo generar la orden de pago en Cuentas por Pagar. " + e.getMessage());
        }

        // 5. La devolución queda enlazada a su orden de pago
        devolucion.setIdPagoProgramado(idPago);
        devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.EN_PAGO));
        devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());

        // Con débito automático el pago nace CONFIRMADO: se aplica de una el paso a PAGADA
        // en vez de esperar al reconciliador.
        PagoProgramado pago = pagoProgramadoDaoService.find(new PagoProgramado(), idPago);
        if (pago != null && pago.getEstado() != null
                && pago.getEstado().intValue() == EstadoPagoProgramado.CONFIRMADO) {
            aplicarPagado(devolucion, pago);
            devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());
        }

        // 6. Saldos resultantes, para refrescar la pantalla sin recalcular en el cliente
        for (DetalleResultadoDevolucion detalleResultado : resultado.getDetalle()) {
            detalleResultado.setSaldoTipoAporteDespues(saldoAporteService.saldoPorEntidadYTipo(
                entidad.getCodigo(), detalleResultado.getIdTipoAporte()));
        }

        resultado.setIdPagoProgramado(devolucion.getIdPagoProgramado());
        resultado.setEstado(devolucion.getEstado());
        resultado.setEstadoTexto(nombreEstado(devolucion.getEstado()));
        resultado.setNumeroAsiento(devolucion.getNumeroAsiento());
        resultado.setFechaPago(devolucion.getFechaPago());

        System.out.println("  ✅ Devolución " + devolucion.getCodigo() + " registrada por $"
            + valorTotal + " - Orden de pago " + idPago
            + " - Estado: " + nombreEstado(devolucion.getEstado()));

        return resultado;
    }

    // ========================================================================
    // Listado
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<DevolucionAporte> listarPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("DevolucionAporteService.listarPorEntidad - Entidad: " + idEntidad);

        if (idEntidad == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }

        // Se reconcilia ANTES de responder: lo que ve el usuario siempre está al día, sin
        // depender de que el timer haya corrido. Cada devolución en su propia transacción,
        // y un fallo de reconciliación no impide devolver el listado.
        List<DevolucionAporte> pendientes =
                devolucionAporteDaoService.selectPendientesConciliacionByEntidad(idEntidad);
        if (pendientes != null) {
            for (DevolucionAporte pendiente : pendientes) {
                try {
                    self.sincronizarDevolucion(pendiente.getCodigo());
                } catch (Throwable e) {
                    System.err.println("Error al reconciliar la devolución "
                        + pendiente.getCodigo() + " antes de listar: " + e.getMessage());
                }
            }
        }

        List<DevolucionAporte> devoluciones = devolucionAporteDaoService.selectByEntidad(idEntidad);
        // Lista vacía NO es error: el partícipe simplemente no tiene devoluciones.
        return devoluciones != null ? devoluciones : new ArrayList<DevolucionAporte>();
    }

    // ========================================================================
    // Reconciliador
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ResultadoSincronizacion sincronizarPagos() throws Throwable {

        System.out.println("========================================");
        System.out.println("SINCRONIZACIÓN DE DEVOLUCIONES DE APORTES");
        System.out.println("========================================");

        ResultadoSincronizacion resumen = new ResultadoSincronizacion();

        List<DevolucionAporte> pendientes =
                devolucionAporteDaoService.selectPendientesConciliacion();
        int universo = (pendientes != null) ? pendientes.size() : 0;
        System.out.println("Devoluciones a evaluar: " + universo);

        if (pendientes != null) {
            for (DevolucionAporte pendiente : pendientes) {
                try {
                    // A través del proxy: cada devolución commitea por separado
                    ResultadoSincronizacion parcial = self.sincronizarDevolucion(
                        pendiente.getCodigo());
                    acumular(resumen, parcial);

                } catch (Throwable e) {
                    // Una devolución con datos malos no aborta el lote
                    resumen.setEvaluadas(resumen.getEvaluadas() + 1);
                    resumen.setConError(resumen.getConError() + 1);
                    if (resumen.getErrores().size() < MAX_ERRORES_DETALLADOS) {
                        resumen.getErrores().add("Devolución " + pendiente.getCodigo() + ": "
                            + e.getMessage());
                    }
                    System.err.println("Error al reconciliar la devolución "
                        + pendiente.getCodigo() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("SINCRONIZACIÓN TERMINADA"
            + " - Evaluadas: " + resumen.getEvaluadas()
            + " - Pagadas: " + resumen.getMarcadasPagadas()
            + " - Rechazadas: " + resumen.getMarcadasRechazadas()
            + " - Huérfanas: " + resumen.getHuerfanas()
            + " - Con error: " + resumen.getConError());

        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ResultadoSincronizacion sincronizarDevolucion(Long idDevolucion) throws Throwable {

        System.out.println("DevolucionAporteService.sincronizarDevolucion - Devolucion: "
            + idDevolucion);

        ResultadoSincronizacion parcial = new ResultadoSincronizacion();

        DevolucionAporte devolucion = devolucionAporteDaoService.find(
            new DevolucionAporte(), idDevolucion);
        if (devolucion == null) {
            throw new IncomeException(ERR_DEVOLUCION_NO_ENCONTRADA + ": no existe la devolución "
                + idDevolucion);
        }

        int estado = (devolucion.getEstado() != null) ? devolucion.getEstado().intValue() : 0;

        // Idempotencia: una devolución que ya cerró su ciclo no se vuelve a tocar.
        if (estado != EstadoDevolucionAporte.REGISTRADA && estado != EstadoDevolucionAporte.EN_PAGO) {
            System.out.println("  Devolución " + idDevolucion + " en estado "
                + nombreEstado(devolucion.getEstado()) + ": sin cambios.");
            return parcial;
        }
        if (devolucion.getIdPagoProgramado() == null) {
            System.out.println("  Devolución " + idDevolucion + " sin orden de pago: sin cambios.");
            return parcial;
        }

        parcial.setEvaluadas(1);

        // crd → cxp: se LEE el estado real del pago. CXP no avisa: no puede nombrar a CRD.
        PagoProgramado pago = pagoProgramadoDaoService.find(
            new PagoProgramado(), devolucion.getIdPagoProgramado());

        if (pago == null) {
            // La orden de pago ya no existe. Se deja la devolución como está y se registra:
            // es un dato para investigar, no un error que aborte la corrida.
            parcial.setHuerfanas(1);
            parcial.getErrores().add("Devolución " + idDevolucion + ": la orden de pago "
                + devolucion.getIdPagoProgramado() + " ya no existe en Cuentas por Pagar.");
            System.err.println("  ⚠ Devolución " + idDevolucion + " huérfana: la orden de pago "
                + devolucion.getIdPagoProgramado() + " no existe.");
            return parcial;
        }

        int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;

        if (estadoPago == EstadoPagoProgramado.CONFIRMADO) {
            aplicarPagado(devolucion, pago);
            devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());
            parcial.setMarcadasPagadas(1);
            // Asiento nulo no es error ni se cuenta como tal: el pago pudo confirmarse sin
            // contabilidad porque no tenía desglose contable (§6.5.b).
            System.out.println("  ✅ Devolución " + idDevolucion + " PAGADA"
                + " - Asiento: " + (devolucion.getNumeroAsiento() != null
                    ? String.valueOf(devolucion.getNumeroAsiento()) : "(sin contabilidad)")
                + " - Fecha: " + devolucion.getFechaPago());

        } else if (estadoPago == EstadoPagoProgramado.RECHAZADO
                || estadoPago == EstadoPagoProgramado.ANULADO) {
            generarContraMovimientos(devolucion, "Pago rechazado");
            devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.RECHAZADA));
            devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());
            parcial.setMarcadasRechazadas(1);
            System.out.println("  ↩ Devolución " + idDevolucion + " RECHAZADA: "
                + "contra-movimientos generados, el saldo del partícipe vuelve a su valor previo.");

        } else {
            System.out.println("  Devolución " + idDevolucion + ": el pago sigue en curso "
                + "(estado " + estadoPago + "). Sin cambios.");
        }

        return parcial;
    }

    // ========================================================================
    // Anulación
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoDevolucionAporte anularDevolucion(Long idDevolucion, String motivo,
            String usuario) throws Throwable {

        System.out.println("DevolucionAporteService.anularDevolucion - Devolucion: " + idDevolucion
            + " - Usuario: " + usuario);

        if (idDevolucion == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idDevolucion es obligatorio");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": debe indicar el motivo de la anulación");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }

        DevolucionAporte devolucion = devolucionAporteDaoService.find(
            new DevolucionAporte(), idDevolucion);
        if (devolucion == null) {
            throw new IncomeException(ERR_DEVOLUCION_NO_ENCONTRADA + ": no existe la devolución "
                + idDevolucion);
        }

        int estado = (devolucion.getEstado() != null) ? devolucion.getEstado().intValue() : 0;
        if (estado == EstadoDevolucionAporte.ANULADA) {
            throw new IncomeException(ERR_DEVOLUCION_YA_ANULADA + ": la devolución "
                + idDevolucion + " ya está anulada");
        }
        if (estado == EstadoDevolucionAporte.PAGADA) {
            throw new IncomeException(ERR_DEVOLUCION_YA_PAGADA + ": La devolución ya fue pagada; "
                + "reverse el pago desde Cuentas por Pagar y vuelva a intentar.");
        }
        if (estado != EstadoDevolucionAporte.REGISTRADA && estado != EstadoDevolucionAporte.EN_PAGO) {
            throw new IncomeException(ERR_ESTADO_NO_PERMITE + ": la devolución " + idDevolucion
                + " está " + nombreEstado(devolucion.getEstado()) + " y no se puede anular");
        }

        // La orden de pago: se anula antes de tocar los aportes, para que el fallo de CXP
        // deje todo como estaba.
        if (devolucion.getIdPagoProgramado() != null) {

            PagoProgramado pago = pagoProgramadoDaoService.find(
                new PagoProgramado(), devolucion.getIdPagoProgramado());

            if (pago != null) {
                int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;

                if (estadoPago == EstadoPagoProgramado.CONFIRMADO) {
                    // El estado de la devolución estaba desactualizado: el pago ya salió.
                    throw new IncomeException(ERR_DEVOLUCION_YA_PAGADA + ": La devolución ya fue "
                        + "pagada; reverse el pago desde Cuentas por Pagar y vuelva a intentar.");
                }
                if (estadoPago == EstadoPagoProgramado.EN_ARCHIVO) {
                    // Mismo criterio que EgresoServiceImpl.anularEgreso: el archivo ya está
                    // en poder del banco y todavía puede ejecutarse.
                    throw new IncomeException(ERR_ESTADO_NO_PERMITE + ": la orden de pago "
                        + pago.getId() + " está en un archivo enviado al banco. Procese la "
                        + "respuesta del banco antes de anular la devolución.");
                }
                if (estadoPago == EstadoPagoProgramado.REGISTRADO) {
                    try {
                        pagoProgramadoService.anularPago(pago.getId(),
                            "Anulación de la devolución de aportes " + idDevolucion + ": "
                                + motivo.trim(), null);
                    } catch (Throwable e) {
                        throw new IncomeException(ERR_ERROR_ORDEN_PAGO + ": no se pudo anular la "
                            + "orden de pago " + pago.getId() + " en Cuentas por Pagar. "
                            + e.getMessage());
                    }
                }
            }
        }

        // Contra-movimientos: el saldo del partícipe vuelve a su valor previo.
        generarContraMovimientos(devolucion, "Devolución anulada");

        devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.ANULADA));
        devolucion.setUsuarioAnulacion(usuario.trim());
        devolucion.setFechaAnulacion(LocalDateTime.now());
        devolucion.setMotivoAnulacion(motivo.trim());
        devolucionAporteDaoService.save(devolucion, devolucion.getCodigo());

        ResultadoDevolucionAporte resultado = armaResultado(devolucion);

        System.out.println("  ✅ Devolución " + idDevolucion + " ANULADA. Motivo: " + motivo.trim());
        return resultado;
    }

    // ========================================================================
    // Helpers privados
    // ========================================================================

    /**
     * Copia al DVAP los datos del pago confirmado. Es idempotente: aplicarlo dos veces deja
     * exactamente los mismos valores.
     * <p>
     * <b>{@code numeroAsiento} nulo NO es un error</b> y no se cuenta como tal: desde el
     * 2026-08-24 la contabilidad es opcional (§6.5.b del plan), así que un pago confirmado
     * sin desglose contable no tiene asiento. La devolución igual pasa a PAGADA(3): lo que
     * la marca pagada es que el banco ejecutó la transferencia, no que exista un asiento.
     * <p>
     * Esas devoluciones se encuentran después con
     * {@code DVAPESTD = 3 AND DVAPNMAS IS NULL}; el control está en
     * {@code docs/logica-negocio/crd/sql/DDL-DEVOLUCION-APORTES.sql}.
     *
     * @param devolucion : Devolución a marcar como pagada
     * @param pago       : Pago programado ya confirmado
     */
    private void aplicarPagado(DevolucionAporte devolucion, PagoProgramado pago) {
        devolucion.setEstado(Long.valueOf(EstadoDevolucionAporte.PAGADA));
        devolucion.setFechaPago(pago.getFechaRespuesta());
        devolucion.setNumeroAsiento(
            (pago.getAsiento() != null) ? pago.getAsiento().getCodigo() : null);
    }

    /**
     * Genera los contra-movimientos POSITIVOS de CRD.APRT de una devolución: por cada
     * detalle sin fila de reverso, inserta un aporte positivo por el mismo valor y marca el
     * PagoAporte original como inactivo.
     * <p>
     * <b>Se inserta un contra-movimiento, no se borra ni se edita la fila negativa.</b>
     * CRD.APRT es append-only para los reportes (G42, G43, G44, CJBM, CPRM/CCPM, dashboard,
     * padrón); el G43 en particular liquida cesantes leyendo explícitamente los negativos
     * del mes.
     * <p>
     * <b>Idempotente</b>: un detalle que ya tiene {@code idAporteReverso} se saltea.
     * @param devolucion : Devolución cuyos aportes hay que revertir
     * @param causa      : Texto que se estampa en la glosa del contra-movimiento
     * @throws Throwable : Excepcion
     */
    private void generarContraMovimientos(DevolucionAporte devolucion, String causa)
            throws Throwable {

        List<DetalleDevolucionAporte> detalles =
                detalleDevolucionAporteDaoService.selectByDevolucion(devolucion.getCodigo());
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();

        for (DetalleDevolucionAporte detalle : detalles) {

            if (detalle.getIdAporteReverso() != null) {
                // Ya se revirtió: no se duplica el contra-movimiento.
                continue;
            }

            double valor = (detalle.getValor() != null) ? detalle.getValor() : 0.0;
            String nombreTipo = (detalle.getTipoAporte() != null)
                ? detalle.getTipoAporte().getNombre() : "";

            String glosa = truncarPorBytes("REVERSO DEVOLUCION " + devolucion.getCodigo()
                + " - " + causa, MAX_BYTES_GLOSA);

            Aporte reverso = new Aporte();
            reverso.setEntidad(devolucion.getEntidad());
            reverso.setFilial(devolucion.getFilial());
            reverso.setTipoAporte(detalle.getTipoAporte());
            reverso.setValor(valor);
            reverso.setValorPagado(0.0);
            reverso.setSaldo(0.0);
            reverso.setEstado((long) EstadoCuotaPrestamo.PAGADA);
            reverso.setIdAsoprep(null);
            reverso.setFechaTransaccion(ahora);
            reverso.setGlosa(glosa);
            reverso.setUsuarioRegistro(devolucion.getUsuarioRegistro());
            reverso.setFechaRegistro(ahora);
            // DAO directo, igual que la fila negativa: saveSingle la devolvería al FIFO.
            reverso = aporteDaoService.save(reverso, null);

            detalle.setIdAporteReverso(reverso.getCodigo());
            detalleDevolucionAporteDaoService.save(detalle, detalle.getCodigo());

            // El PagoAporte de la fila negativa queda inactivo
            if (detalle.getIdPagoAporte() != null) {
                PagoAporte pagoAporte = pagoAporteDaoService.find(
                    new PagoAporte(), detalle.getIdPagoAporte());
                if (pagoAporte != null) {
                    pagoAporte.setEstado(Long.valueOf(Estado.INACTIVO));
                    pagoAporteDaoService.save(pagoAporte, pagoAporte.getCodigo());
                }
            }

            System.out.println("  ↩ Contra-movimiento APRT " + reverso.getCodigo()
                + " (+$" + valor + ") del tipo " + nombreTipo);
        }
    }

    /**
     * Arma el beneficiario ocasional que viaja a CXP con los datos del partícipe y de la
     * cuenta bancaria elegida.
     * <p>
     * El partícipe NO existe como {@code TSR.Titular} y no se convierte en uno: crear un
     * Titular por cada partícipe metería datos de CRD dentro de TSR, justo lo que la
     * restricción de comercialización prohíbe. Por eso CXP recibe los datos denormalizados.
     * @param entidad         : Partícipe
     * @param cuentaParticipe : Cuenta bancaria elegida; puede ser null en débito automático
     * @return                : Beneficiario ocasional
     */
    private BeneficiarioOcasional armaBeneficiario(Entidad entidad,
            CuentaBancariaParticipe cuentaParticipe) {

        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre(entidad.getRazonSocial());
        beneficiario.setIdentificacion(entidad.getNumeroIdentificacion());

        if (cuentaParticipe != null) {
            beneficiario.setIdBancoExterno((cuentaParticipe.getBancoExterno() != null)
                ? cuentaParticipe.getBancoExterno().getCodigo() : null);
            beneficiario.setTipoCuenta(cuentaParticipe.getTipoCuenta());
            beneficiario.setNumeroCuenta(cuentaParticipe.getNumeroCuenta());
        }
        return beneficiario;
    }

    /**
     * Arma el resultado de una devolución a partir de la entidad y sus detalles, con el
     * saldo por tipo tras la operación.
     * @param devolucion : Devolución
     * @return           : Resultado listo para la respuesta REST
     * @throws Throwable : Excepcion
     */
    private ResultadoDevolucionAporte armaResultado(DevolucionAporte devolucion) throws Throwable {

        ResultadoDevolucionAporte resultado = new ResultadoDevolucionAporte();
        resultado.setIdDevolucion(devolucion.getCodigo());
        resultado.setIdEntidad((devolucion.getEntidad() != null)
            ? devolucion.getEntidad().getCodigo() : null);
        resultado.setIdPagoProgramado(devolucion.getIdPagoProgramado());
        resultado.setEstado(devolucion.getEstado());
        resultado.setEstadoTexto(nombreEstado(devolucion.getEstado()));
        resultado.setValorTotal(devolucion.getValor());
        resultado.setFecha(devolucion.getFecha());
        resultado.setNumeroAsiento(devolucion.getNumeroAsiento());
        resultado.setFechaPago(devolucion.getFechaPago());

        List<DetalleDevolucionAporte> detalles =
                detalleDevolucionAporteDaoService.selectByDevolucion(devolucion.getCodigo());
        if (detalles != null) {
            for (DetalleDevolucionAporte detalle : detalles) {
                DetalleResultadoDevolucion linea = new DetalleResultadoDevolucion();
                linea.setIdTipoAporte((detalle.getTipoAporte() != null)
                    ? detalle.getTipoAporte().getCodigo() : null);
                linea.setNombreTipoAporte((detalle.getTipoAporte() != null)
                    ? detalle.getTipoAporte().getNombre() : null);
                linea.setValor(detalle.getValor());
                linea.setIdAporteGenerado(detalle.getIdAporte());
                linea.setIdPagoAporteGenerado(detalle.getIdPagoAporte());
                linea.setIdAporteReverso(detalle.getIdAporteReverso());
                if (resultado.getIdEntidad() != null && linea.getIdTipoAporte() != null) {
                    linea.setSaldoTipoAporteDespues(saldoAporteService.saldoPorEntidadYTipo(
                        resultado.getIdEntidad(), linea.getIdTipoAporte()));
                }
                resultado.getDetalle().add(linea);
            }
        }
        return resultado;
    }

    /**
     * Acumula los contadores de una devolución en el resumen del lote.
     * @param resumen : Resumen acumulado
     * @param parcial : Resultado de una devolución
     */
    private void acumular(ResultadoSincronizacion resumen, ResultadoSincronizacion parcial) {
        if (parcial == null) {
            return;
        }
        resumen.setEvaluadas(resumen.getEvaluadas() + parcial.getEvaluadas());
        resumen.setMarcadasPagadas(resumen.getMarcadasPagadas() + parcial.getMarcadasPagadas());
        resumen.setMarcadasRechazadas(
            resumen.getMarcadasRechazadas() + parcial.getMarcadasRechazadas());
        resumen.setHuerfanas(resumen.getHuerfanas() + parcial.getHuerfanas());
        resumen.setConError(resumen.getConError() + parcial.getConError());
        for (String error : parcial.getErrores()) {
            if (resumen.getErrores().size() < MAX_ERRORES_DETALLADOS) {
                resumen.getErrores().add(error);
            }
        }
    }

    /**
     * Nombre legible del estado de una devolución, para los mensajes y la pantalla.
     * @param estado : Estado de la devolución
     * @return       : Nombre del estado
     */
    private String nombreEstado(Long estado) {
        if (estado == null) {
            return "SIN ESTADO";
        }
        switch (estado.intValue()) {
            case EstadoDevolucionAporte.REGISTRADA: return "REGISTRADA";
            case EstadoDevolucionAporte.EN_PAGO:    return "EN PAGO";
            case EstadoDevolucionAporte.PAGADA:     return "PAGADA";
            case EstadoDevolucionAporte.RECHAZADA:  return "RECHAZADA";
            case EstadoDevolucionAporte.ANULADA:    return "ANULADA";
            default: return String.valueOf(estado);
        }
    }

    /** Recorta el texto para que su representación UTF-8 quepa en la columna. */
    private String truncarPorBytes(String texto, int maxBytes) {
        if (texto == null || texto.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return texto;
        }
        int hasta = texto.length();
        while (hasta > 0
                && texto.substring(0, hasta).getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            hasta--;
        }
        return texto.substring(0, hasta);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
