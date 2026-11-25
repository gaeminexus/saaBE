package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.GarantiaOrdenPrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacionGarantia;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * C31 (16-nov-2026, pedido del líder): «si un préstamo se termina, la garantía se debe dar de baja con el
 * monto otorgado, no con el saldo capital».
 *
 * <p><b>Por qué hacía falta.</b> La entrega (plantillas 9, 13, 34) registra D 7.3.01.05 cartera de créditos
 * (orden) / H 7.4.01.05 documentos en garantía por el monto del préstamo (y H 7.4.01.10/15 por el bien).
 * Antes de C31 ningún camino de terminación (pago de la última cuota por ventanilla, CBCR o Petro/Arch,
 * precancelación con o sin aportes, precancelación con préstamo nuevo, acuerdo de pago con condonación)
 * tocaba esas cuentas, y ninguna cuota pagada las reduce: el pagaré quedaba registrado para siempre. Solo
 * la novación lo daba de baja (C30). La plantilla 26 «LEVANTAMIENTO DE HIPOTECA Y ENTREGA DE PAGARE» existía
 * sin consumidor.</p>
 *
 * <p><b>Criterio exacto.</b> Al terminar el préstamo se da de baja todo lo registrado: el pagaré por el
 * monto otorgado ({@code PRSTMNSL}) y, si es prendario o hipotecario, el bien por el valor de la garantía
 * ({@code PRSTVLGR}, avalúo del bien cargado del archivo de Cartera — C31b / D-083) o, si no está cargado o
 * es 0, por la suma asegurada ({@code PRSTVLAS} &gt; 0); la contrapartida (7.3.01.05) por la suma. Como ninguna cuota reduce la
 * orden, «lo que queda del monto otorgado» es el monto otorgado entero y el saldo del préstamo en orden
 * queda en 0. Si el préstamo no tiene monto otorgado, falla con mensaje (igual que C30).</p>
 *
 * <p><b>Idempotencia y reversos.</b> Cada línea lleva la marca {@code [C31 PRST id]}. El neto de las líneas
 * marcadas en la cuenta del pagaré dice si la baja está vigente; llamarlo dos veces no duplica nada. Si la
 * terminación se revierte (reverso de la operación, reproceso de la carga Petro, reapertura al corte), el
 * asiento de baja se anula con la regla estándar de CNT (anula en período abierto, reversa en el primer
 * período abierto si el suyo está cerrado).</p>
 *
 * <p><b>C31c (16-nov-2026) — la baja es de la operación que termina el préstamo.</b> En el re-proceso de
 * julio 2025 la vía automática dio de baja ~110 préstamos que no terminaron en julio: la copia de
 * producción los traía cancelados (producción los canceló entre nov-2025 y nov-2026), el re-proceso de la
 * carga los reabrió al anular el cobro del mes (3 → 2), la re-aplicación pagó la cuota del mes y, como las
 * cuotas siguientes ya figuraban pagadas por cobros de 2026, el préstamo volvió a 3 y se le dio de baja la
 * garantía al 31-jul. Ahora: (1) cada proceso pasa el estado capturado antes de su operación y solo la
 * transición vivo → 3/4 genera baja; (2) en la cancelación por pago de cuotas (motor y red de seguridad de
 * la carga Petro), además, ningún pago vigente puede tener fecha posterior al día de la operación — sin
 * esto, el caso de arriba seguía pasando, porque el re-proceso lo reabre dentro de la misma corrida y el
 * estado «antes del pago» es 2; (3) la anulación al reabrir no cambia; (4) los terminados sin baja se
 * sincronizan a mano con {@link #sincronizarExplicito}.</p>
 */
@Stateless
public class GarantiaOrdenPrestamoServiceImpl implements GarantiaOrdenPrestamoService {

    private static final double TOLERANCIA = 0.005;

    /** Posiciones aux1 de la plantilla 26 (ver {@link PlantillasCredito#LEVANTAMIENTO_GARANTIA}). */
    private static final int AUX_ORDEN_CARTERA = 1;
    private static final int AUX_DOCUMENTOS_GARANTIA = 2;
    private static final int AUX_BIEN_INMUEBLE = 3;
    private static final int AUX_BIEN_VEHICULO = 4;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    /** Marca de las líneas del préstamo (ASCII, sin tildes, para que el LIKE no dependa del juego de caracteres). */
    static String marca(Long idPrestamo) {
        return "[C31 PRST " + idPrestamo + "]";
    }

    /** ¿CANCELADO (3) o CANCELADO_ANTICIPADO (4)? Los únicos terminales que dan de baja la garantía (C31). */
    static boolean esTerminado(Long idEstado) {
        return idEstado != null && (idEstado.longValue() == EstadoPrestamo.CANCELADO
            || idEstado.longValue() == EstadoPrestamo.CANCELADO_ANTICIPADO);
    }

    @Override
    public Long sincronizar(Prestamo prestamo, Long estadoAnterior, Long idEmpresa, LocalDate fecha,
            LocalDateTime pagosHasta, String usuario, String causa) throws Throwable {
        if (prestamo == null || prestamo.getCodigo() == null || prestamo.getIdEstado() == null) {
            return null;
        }
        boolean terminado = esTerminado(prestamo.getIdEstado());
        boolean vivo = EstadoPrestamo.esVivo(prestamo.getIdEstado());
        if (terminado) {
            // C31c: la baja la genera solo la operación que hace pasar el préstamo de vivo a terminado. Un
            // préstamo que ya estaba terminado antes de la operación no se da de baja por la vía automática
            // aunque no tenga baja (para esos está sincronizarExplicito / POST /prst/sincronizarGarantia).
            if (estadoAnterior == null || !EstadoPrestamo.esVivo(estadoAnterior)) {
                System.out.println("  ℹ️ C31c sin baja de garantía - préstamo " + prestamo.getCodigo()
                    + ": estado antes de la operación " + estadoAnterior + " (no estaba vivo)");
                return null;
            }
            if (pagosHasta != null && terminaPorPagosPosteriores(prestamo, fecha, pagosHasta)) {
                return null;
            }
        } else if (!vivo) {
            return null;
        }
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("GarantiaOrdenPrestamoService - contabilidad de CRD inactiva: préstamo "
                + prestamo.getCodigo() + " sin movimiento de garantía en cuentas de orden.");
            return null;
        }
        Contexto ctx = contexto(prestamo, idEmpresa);
        if (terminado && !ctx.bajaVigente) {
            return darDeBaja(prestamo, ctx.empresa, ctx.idPlantilla, ctx.lineaDocumentos, fecha, usuario, causa);
        }
        if (vivo && ctx.bajaVigente) {
            return anularBaja(prestamo, ctx, usuario, causa);
        }
        return null;
    }

    @Override
    public ResultadoSincronizacionGarantia sincronizarExplicito(Long idPrestamo, Long idEmpresa, LocalDate fecha,
            String usuario, String motivo) throws Throwable {
        if (idPrestamo == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idPrestamo es obligatorio");
        }
        if (fecha == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: la fecha contable de la baja es obligatoria (yyyy-MM-dd)");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw new IncomeException("PARAMETRO_INVALIDO: la fecha " + fecha + " es futura");
        }
        // U-2: el usuario se valida lo primero (existe en el sistema).
        String usuarioValido = usuarioValidacionService.validar(usuario);
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        if (!configuracionContabilidadService.contabilidadActiva()) {
            throw new IncomeException("La contabilidad de CRD está inactiva: no se mueve la garantía del préstamo "
                + idPrestamo + " en cuentas de orden.");
        }

        ResultadoSincronizacionGarantia resultado = new ResultadoSincronizacionGarantia();
        resultado.setIdPrestamo(idPrestamo);
        resultado.setEstadoPrestamo(prestamo.getIdEstado());
        resultado.setFecha(fecha);
        resultado.setAccion(ResultadoSincronizacionGarantia.ACCION_SIN_CAMBIO);

        boolean terminado = esTerminado(prestamo.getIdEstado());
        boolean vivo = EstadoPrestamo.esVivo(prestamo.getIdEstado());
        if (!terminado && !vivo) {
            resultado.setMensaje("El préstamo " + idPrestamo + " está en estado " + prestamo.getIdEstado()
                + ": C31 solo da de baja los CANCELADO (3) / CANCELADO_ANTICIPADO (4); la novación (5) va por C30.");
            return resultado;
        }
        Contexto ctx = contexto(prestamo, idEmpresa);
        String causa = "sincronización explícita" + (motivo != null && !motivo.trim().isEmpty() ? " - " + motivo.trim() : "");
        if (terminado && !ctx.bajaVigente) {
            resultado.setIdAsiento(darDeBaja(prestamo, ctx.empresa, ctx.idPlantilla, ctx.lineaDocumentos, fecha,
                usuarioValido, causa));
            resultado.setAccion(ResultadoSincronizacionGarantia.ACCION_BAJA);
            resultado.setBajaVigente(true);
            resultado.setMensaje("Garantía del préstamo " + idPrestamo + " dada de baja al " + fecha
                + " (asiento " + resultado.getIdAsiento() + ").");
        } else if (vivo && ctx.bajaVigente) {
            resultado.setIdAsiento(anularBaja(prestamo, ctx, usuarioValido, causa));
            resultado.setAccion(ResultadoSincronizacionGarantia.ACCION_ANULACION);
            resultado.setBajaVigente(false);
            resultado.setMensaje("El préstamo " + idPrestamo + " está vivo: se anuló la baja de su garantía (asiento "
                + resultado.getIdAsiento() + ").");
        } else {
            resultado.setBajaVigente(ctx.bajaVigente);
            resultado.setIdAsiento(ctx.ultimaBajaActiva);
            resultado.setMensaje(terminado
                ? "La garantía del préstamo " + idPrestamo + " ya está dada de baja"
                    + (ctx.ultimaBajaActiva != null ? " (asiento " + ctx.ultimaBajaActiva + ")" : "") + ": nada que hacer."
                : "El préstamo " + idPrestamo + " está vivo y su garantía registrada: nada que hacer.");
        }
        return resultado;
    }

    /**
     * C31c, control cronológico de la cancelación por cuotas: ¿hay pagos vigentes del préstamo con fecha
     * posterior al día de la operación? Si los hay, el préstamo quedó sin cuotas pendientes porque las
     * siguientes ya figuran pagadas por cobros posteriores (el re-proceso de un mes pasado sobre la copia
     * de producción reabre el préstamo, le re-aplica el cobro del mes y lo encuentra «terminado»). La
     * terminación no es de esta operación: no se da de baja (queda para su mes o para la vía explícita).
     */
    private boolean terminaPorPagosPosteriores(Prestamo prestamo, LocalDate fecha, LocalDateTime pagosHasta)
            throws Throwable {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        if (pagosHasta.toLocalDate().isAfter(dia)) {
            dia = pagosHasta.toLocalDate();
        }
        long posteriores = cierreCarteraDaoService.contarPagosVigentesDesde(prestamo.getCodigo(), dia.plusDays(1));
        if (posteriores > 0) {
            System.out.println("  ℹ️ C31c sin baja de garantía - préstamo " + prestamo.getCodigo()
                + ": quedó sin cuotas pendientes al " + dia + " pero " + posteriores
                + " pago(s) vigente(s) tienen fecha posterior; la terminación no es de esta operación"
                + " (si corresponde, POST /prst/sincronizarGarantia)");
            return true;
        }
        return false;
    }

    /** Lo que hace falta para decidir y contabilizar: empresa, plantilla 26, línea del pagaré y neto de las bajas. */
    private static final class Contexto {
        Long empresa;
        Long idPlantilla;
        DetallePlantilla lineaDocumentos;
        boolean bajaVigente;
        Long ultimaBajaActiva;
    }

    private Contexto contexto(Prestamo prestamo, Long idEmpresa) throws Throwable {
        Contexto ctx = new Contexto();
        ctx.empresa = resolverEmpresa(prestamo, idEmpresa);
        ctx.idPlantilla = plantillaService.codigoByAlterno(PlantillasCredito.LEVANTAMIENTO_GARANTIA, ctx.empresa);
        if (ctx.idPlantilla == null) {
            throw new IncomeException("No existe la plantilla contable alterno " + PlantillasCredito.LEVANTAMIENTO_GARANTIA
                + " (levantamiento de garantía) para la empresa " + ctx.empresa
                + ": no se puede mover la garantía del préstamo " + prestamo.getCodigo() + " en cuentas de orden.");
        }
        ctx.lineaDocumentos = lineaPlantilla(ctx.idPlantilla, AUX_DOCUMENTOS_GARANTIA);

        List<Object[]> lineas = cierreCarteraDaoService.selectLineasBajaGarantia(prestamo.getCodigo(),
            ctx.lineaDocumentos.getPlanCuenta().getCodigo(), marca(prestamo.getCodigo()));
        double neto = 0.0;
        for (Object[] fila : lineas) {
            double valor = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
            neto += valor;
            Long estadoAsiento = (Long) fila[1];
            if (valor > TOLERANCIA && estadoAsiento != null && estadoAsiento.intValue() == EstadoAsiento.ACTIVO) {
                ctx.ultimaBajaActiva = (Long) fila[0];
            }
        }
        ctx.bajaVigente = redondear(neto) > TOLERANCIA;
        return ctx;
    }

    private Long anularBaja(Prestamo prestamo, Contexto ctx, String usuario, String causa) throws Throwable {
        if (ctx.ultimaBajaActiva == null) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo() + " volvió a estar vivo y su garantía"
                + " figura dada de baja en cuentas de orden, pero no se encontró el asiento de baja activo que anular.");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException("Falta el usuario para anular la baja de garantía del préstamo "
                + prestamo.getCodigo() + ".");
        }
        asientoService.anulaAsiento(ctx.ultimaBajaActiva, usuario, "C31: el préstamo " + prestamo.getCodigo()
            + " volvió a estar vigente (" + (causa != null ? causa : "reapertura") + "): se anula la baja de su garantía");
        System.out.println("  ↩️ C31 baja de garantía anulada - préstamo " + prestamo.getCodigo()
            + " - asiento " + ctx.ultimaBajaActiva);
        return ctx.ultimaBajaActiva;
    }

    private Long darDeBaja(Prestamo prestamo, Long empresa, Long idPlantilla, DetallePlantilla lineaDocumentos,
            LocalDate fecha, String usuario, String causa) throws Throwable {
        double monto = redondear(prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0);
        if (monto <= TOLERANCIA) {
            throw new IncomeException("El préstamo " + prestamo.getCodigo() + " no tiene monto entregado (PRSTMNSL):"
                + " no se puede dar de baja su garantía en cuentas de orden.");
        }
        String familia = familia(prestamo);
        // C31b / D-083: el bien sale por el valor de la garantía (avalúo, PRSTVLGR) si está cargado;
        // si no, por la suma asegurada (PRSTVLAS), como en C31.
        // 3.9 (DS-08): el bien sale por el MISMO valor con que entró. Primero el de las líneas del asiento de entrega
        // (PRSTVLAS puede haber cambiado después por póliza o captura manual, y entonces quedaba un residuo en 7.3/7.4);
        // si el préstamo no tiene asiento de entrega con bien (cartera migrada), la regla de siempre.
        double entregado = redondear(cierreCarteraDaoService.selectBienEntregadoEnOrden(empresa, prestamo.getCodigo()));
        double avaluo = prestamo.getValorGarantia() != null ? prestamo.getValorGarantia() : 0.0;
        double bien = entregado > TOLERANCIA ? entregado : redondear(avaluo > TOLERANCIA ? avaluo
            : (prestamo.getValorAsegurado() != null ? prestamo.getValorAsegurado() : 0.0));
        Integer auxBien = null;
        if (bien > TOLERANCIA) {
            if ("HIPOTECARIO".equals(familia)) {
                auxBien = AUX_BIEN_INMUEBLE;
            } else if ("PRENDARIO".equals(familia)) {
                auxBien = AUX_BIEN_VEHICULO;
            }
        }
        if (auxBien == null) {
            bien = 0.0;
        }

        String texto = "Baja de garantía préstamo " + prestamo.getCodigo()
            + (prestamo.getIdAsoprep() != null ? " (N° " + prestamo.getIdAsoprep() + ")" : "")
            + (causa != null ? " - " + causa : "");
        if (texto.length() > 120) {
            texto = texto.substring(0, 120);
        }
        // La marca va AL PRINCIPIO: la descripción se trunca a 200 y la marca no puede perderse.
        String prefijo = marca(prestamo.getCodigo()) + " " + texto;

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(linea(lineaPlantilla(idPlantilla, AUX_ORDEN_CARTERA), redondear(monto + bien), false,
            prefijo + " - cartera de créditos (orden)"));
        lineas.add(linea(lineaDocumentos, monto, true, prefijo + " - pagaré por el monto otorgado"));
        if (auxBien != null) {
            lineas.add(linea(lineaPlantilla(idPlantilla, auxBien), bien, true, prefijo + " - bien en garantía"));
        }

        LocalDate fechaAsiento = fecha != null ? fecha : LocalDate.now();
        Asiento asiento = asientoContableService.generarAsiento(empresa, TipoAsientos.CREDITOS, fechaAsiento,
            "Baja de garantía en cuentas de orden - préstamo " + prestamo.getCodigo()
                + (causa != null ? " (" + causa + ")" : ""),
            usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
        System.out.println("  ✅ C31 baja de garantía - préstamo " + prestamo.getCodigo() + " - pagaré $" + monto
            + (bien > 0 ? " - bien $" + bien : "") + " - asiento " + asiento.getCodigo() + " al " + fechaAsiento);
        return asiento.getCodigo();
    }

    /** Empresa contable: la recibida; si no, la del asiento de entrega; si no, la única con la plantilla 26. */
    private Long resolverEmpresa(Prestamo prestamo, Long idEmpresa) throws Throwable {
        if (idEmpresa != null) {
            return idEmpresa;
        }
        if (prestamo.getIdAsientoEntrega() != null) {
            Asiento entrega = asientoService.selectById(prestamo.getIdAsientoEntrega());
            if (entrega != null && entrega.getEmpresa() != null) {
                return entrega.getEmpresa().getCodigo();
            }
        }
        List<Long> empresas = cierreCarteraDaoService.selectEmpresasConPlantilla(PlantillasCredito.LEVANTAMIENTO_GARANTIA);
        if (empresas.size() == 1) {
            return empresas.get(0);
        }
        throw new IncomeException("No se pudo determinar la empresa contable para mover la garantía del préstamo "
            + prestamo.getCodigo() + " en cuentas de orden (" + empresas.size() + " empresas con la plantilla "
            + PlantillasCredito.LEVANTAMIENTO_GARANTIA + ").");
    }

    private String familia(Prestamo prestamo) {
        if (prestamo.getProducto() == null || prestamo.getProducto().getTipoPrestamo() == null
                || prestamo.getProducto().getTipoPrestamo().getNombre() == null) {
            return "";
        }
        return prestamo.getProducto().getTipoPrestamo().getNombre().trim().toUpperCase();
    }

    private DetallePlantilla lineaPlantilla(Long idPlantilla, int aux1) throws Throwable {
        DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("La plantilla alterno " + PlantillasCredito.LEVANTAMIENTO_GARANTIA
                + " no tiene la línea aux1=" + aux1 + ".");
        }
        return linea;
    }

    private DetalleAsiento linea(DetallePlantilla plantilla, double valor, boolean debe, String descripcion) {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        DetalleAsiento detalle = new DetalleAsiento();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta.getCuentaContable());
        detalle.setNombreCuenta(cuenta.getNombre());
        detalle.setDescripcion(descripcion.length() > 200 ? descripcion.substring(0, 200) : descripcion);
        detalle.setValorDebe(debe ? redondear(valor) : 0.0);
        detalle.setValorHaber(debe ? 0.0 : redondear(valor));
        return detalle;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
