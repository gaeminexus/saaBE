package com.saa.ejb.crd.serviceImpl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetalleCobroCreditoDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.EventoPrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.SustitucionPrestamoService;
import com.saa.ejb.crd.service.dto.ContextoSustitucion;
import com.saa.ejb.crd.service.dto.DesgloseConceptosPrestamo;
import com.saa.ejb.crd.service.dto.SustituiblePrestamo;
import com.saa.model.crd.DetalleCobroCredito;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.CrdEstadoCobro;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Reestructuración y novación (DISENO-REESTRUCTURACION-NOVACION.md). Ver
 * {@link SustitucionPrestamoService}.
 *
 * <p>Las reglas por préstamo viven en UN solo método ({@link #evaluar}) que usan el listado
 * ({@code sustituibles}), el guardado y la aprobación, para que las tres puertas nunca discrepen.
 * Ese método NO lanza por una regla de negocio: devuelve el motivo (una IncomeException que cruza
 * un proxy EJB marca la transacción como rollback aunque se atrape).</p>
 */
@Stateless
public class SustitucionPrestamoServiceImpl implements SustitucionPrestamoService {

    private static final double TOLERANCIA_MONTO = 0.01;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private ProductoDaoService productoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetalleCobroCreditoDaoService detalleCobroCreditoDaoService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @EJB
    private ContabilidadPrestamoService contabilidadPrestamoService;

    @EJB
    private EventoPrestamoService eventoPrestamoService;

    /** Resultado interno de evaluar un préstamo como original. */
    private static final class Evaluacion {
        private String motivo;
        private String familia = "";
        private double valorSustitucion;
        private DesgloseConceptosPrestamo desglose;
        private boolean conAtraso;
        private boolean conLineas;
        private Producto sugerido;
    }

    // =====================================================================
    // Listado
    // =====================================================================

    @Override
    public List<SustituiblePrestamo> sustituibles(Long idEntidad, String tipo) throws Throwable {
        System.out.println("SustitucionPrestamoService.sustituibles - entidad: " + idEntidad + " - tipo: " + tipo);
        if (idEntidad == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: idEntidad es obligatorio");
        }
        boolean novacion = esTipoNovacion(tipo);
        LocalDate hoy = LocalDate.now();

        List<SustituiblePrestamo> resultado = new ArrayList<>();
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(idEntidad);
        if (prestamos == null) {
            return resultado;
        }
        for (Prestamo prestamo : prestamos) {
            Long estado = prestamo.getIdEstado();
            // Solo los préstamos "vivos" del partícipe entran a la lista; los demás (cancelados,
            // anulados, sin aprobar) no son candidatos ni vale la pena explicar por qué.
            if (estado == null || (estado != EstadoPrestamo.VIGENTE && estado != EstadoPrestamo.DE_PLAZO_VENCIDO
                    && estado != EstadoPrestamo.VIGENTE_POR_REVISAR && estado != EstadoPrestamo.EN_MORA)) {
                continue;
            }
            Evaluacion ev = evaluar(prestamo, novacion, hoy, null);
            SustituiblePrestamo fila = new SustituiblePrestamo();
            fila.setIdPrestamo(prestamo.getCodigo());
            fila.setIdAsoprep(prestamo.getIdAsoprep());
            fila.setProducto(prestamo.getProducto() != null ? prestamo.getProducto().getNombre() : null);
            fila.setFamilia(ev.familia);
            fila.setEstado(estado);
            fila.setPuede(ev.motivo == null);
            fila.setMotivo(ev.motivo);
            if (ev.desglose != null) {
                fila.setValorSustitucion(ev.valorSustitucion);
                SustituiblePrestamo.Desglose desglose = new SustituiblePrestamo.Desglose();
                desglose.setCapital(redondear(ev.desglose.getCapitalPendiente()));
                desglose.setInteres(redondear(ev.desglose.getInteresPendiente()));
                desglose.setMora(redondear(ev.desglose.getMoraPendiente()));
                desglose.setSeguros(redondear(ev.desglose.getDesgravamenPendiente()
                    + ev.desglose.getSeguroIncendioPendiente()));
                fila.setDesglose(desglose);
            }
            if (ev.sugerido != null) {
                SustituiblePrestamo.ProductoSugerido sugerido = new SustituiblePrestamo.ProductoSugerido();
                sugerido.setIdProducto(ev.sugerido.getCodigo());
                sugerido.setNombre(ev.sugerido.getNombre());
                fila.setProductoSugerido(sugerido);
            }
            resultado.add(fila);
        }
        return resultado;
    }

    // =====================================================================
    // Guardar
    // =====================================================================

    @Override
    public void validarAlGuardar(Prestamo nuevo) throws Throwable {
        System.out.println("SustitucionPrestamoService.validarAlGuardar - original: "
            + (nuevo != null ? nuevo.getIdPrestamoOriginal() : null));
        Prestamo original = cargarOriginal(nuevo);
        Producto producto = cargarProductoDe(nuevo);
        boolean novacion = tipoDeProducto(producto);

        Evaluacion ev = evaluar(original, novacion, LocalDate.now(), producto);
        if (ev.motivo != null) {
            throw new IncomeException(ev.motivo);
        }
        double monto = nuevo.getMontoSolicitado() != null ? nuevo.getMontoSolicitado() : 0.0;
        String vsTexto = String.format(java.util.Locale.US, "%.2f", ev.valorSustitucion);
        if (novacion) {
            if (monto <= ev.valorSustitucion + 0.005) {
                throw new IncomeException("En una novación el monto ($" + String.format(java.util.Locale.US, "%.2f", monto)
                    + ") debe ser mayor que el valor de sustitución ($" + vsTexto + ").");
            }
        } else if (Math.abs(monto - ev.valorSustitucion) > TOLERANCIA_MONTO + 1e-9) {
            throw new IncomeException("En una reestructuración el monto ($"
                + String.format(java.util.Locale.US, "%.2f", monto) + ") debe ser igual al valor de sustitución ($"
                + vsTexto + ").");
        }
    }

    // =====================================================================
    // Aprobar
    // =====================================================================

    @Override
    public ContextoSustitucion prepararAprobacion(Prestamo nuevo) throws Throwable {
        System.out.println("SustitucionPrestamoService.prepararAprobacion - nuevo: "
            + (nuevo != null ? nuevo.getCodigo() : null));
        Prestamo original = cargarOriginal(nuevo);
        Producto producto = cargarProductoDe(nuevo);
        boolean novacion = tipoDeProducto(producto);

        Evaluacion ev = evaluar(original, novacion, LocalDate.now(), producto);
        if (ev.motivo != null) {
            throw new IncomeException(ev.motivo);
        }
        double monto = nuevo.getMontoSolicitado() != null ? nuevo.getMontoSolicitado() : 0.0;
        double neto = 0.0;
        if (novacion) {
            // RN8: el monto no cambia; cambia el neto. Si queda <= 0, se rechaza.
            neto = redondear(monto - ev.valorSustitucion);
            if (neto <= TOLERANCIA_MONTO) {
                throw new IncomeException("El monto del préstamo ($" + String.format(java.util.Locale.US, "%.2f", monto)
                    + ") no supera el saldo del original ($"
                    + String.format(java.util.Locale.US, "%.2f", ev.valorSustitucion)
                    + "); el desembolso neto quedaría en $" + String.format(java.util.Locale.US, "%.2f", neto) + ".");
            }
        } else if (Math.abs(monto - ev.valorSustitucion) > TOLERANCIA_MONTO + 1e-9) {
            // RN7
            throw new IncomeException("El saldo del original cambió a $"
                + String.format(java.util.Locale.US, "%.2f", ev.valorSustitucion)
                + "; regenere la tabla con ese monto.");
        }

        ContextoSustitucion contexto = new ContextoSustitucion();
        contexto.setOriginal(original);
        contexto.setNovacion(novacion);
        contexto.setValorSustitucion(ev.valorSustitucion);
        contexto.setDesglose(ev.desglose);
        contexto.setConAtraso(ev.conAtraso);
        contexto.setConLineas(ev.conLineas);
        contexto.setNeto(neto);
        return contexto;
    }

    @Override
    public void cancelarOriginal(Prestamo nuevo, ContextoSustitucion contexto, Long idAsiento, String usuario)
            throws Throwable {
        Prestamo original = contexto.getOriginal();
        String tipo = contexto.isNovacion() ? TIPO_NOVACION : TIPO_REESTRUCTURACION;
        System.out.println("SustitucionPrestamoService.cancelarOriginal - original: " + original.getCodigo()
            + " - nuevo: " + nuevo.getCodigo() + " - tipo: " + tipo);

        // RN11: cuotas pendientes a CANCELADA_ANTICIPADA (7). Sin PagoPrestamo: no entra dinero.
        List<DetallePrestamo> pendientes =
            detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(original.getCodigo());
        if (pendientes != null) {
            for (DetallePrestamo cuota : pendientes) {
                cuota.setEstado(Long.valueOf(EstadoCuotaPrestamo.CANCELADA_ANTICIPADA));
                cuota.setIdEstado(Long.valueOf(EstadoCuotaPrestamo.CANCELADA_ANTICIPADA));
                detallePrestamoDaoService.save(cuota, cuota.getCodigo());
            }
        }

        // RN11: el evento, con el VS. EVPR no tiene columna para el préstamo nuevo: va en la
        // observación (el enlace estructurado es PRSTPROR del nuevo).
        EventoPrestamo evento = new EventoPrestamo();
        evento.setPrestamo(original);
        evento.setTipoOperacion(tipo);
        evento.setValor(redondear(contexto.getValorSustitucion()));
        evento.setFecha(LocalDateTime.now());
        evento.setObservacion("Sustituido por el préstamo nuevo " + nuevo.getCodigo()
            + (nuevo.getIdAsoprep() != null ? " (N° " + nuevo.getIdAsoprep() + ")" : ""));
        evento.setUsuario(usuario);
        evento.setFechaRegistro(LocalDateTime.now());
        evento.setEstado(1L);
        evento.setNumeroAsiento(idAsiento);
        eventoPrestamoService.saveSingle(evento);

        // RN11: estado del original, 5 (novación) o 13 (reestructuración), ambos terminales.
        original.setIdEstado(Long.valueOf(contexto.isNovacion()
            ? EstadoPrestamo.CANCELADO_POR_NOVACION : EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION));
        prestamoDaoService.save(original, original.getCodigo());

        // RN12: VS y banderas en el nuevo (columnas que ya existían sin escritor).
        nuevo.setValorSustitucion(redondear(contexto.getValorSustitucion()));
        if (contexto.isNovacion()) {
            nuevo.setEsNovacion(1L);
            nuevo.setMontoNovacion(redondear(contexto.getValorSustitucion()));
        } else {
            nuevo.setReestructurado(1L);
        }
    }

    // =====================================================================
    // Reglas por préstamo (una sola fuente)
    // =====================================================================

    /**
     * Evalúa {@code original} como candidato a ser sustituido. Nunca lanza por una regla de
     * negocio: deja el motivo en la evaluación. Con el VS y el desglose cuando el préstamo está vivo.
     *
     * @param productoElegido el producto del préstamo nuevo (al guardar o aprobar), o null (listado:
     *                        se busca el sugerido)
     */
    private Evaluacion evaluar(Prestamo original, boolean novacion, LocalDate hoy, Producto productoElegido)
            throws Throwable {
        Evaluacion ev = new Evaluacion();
        ev.familia = familiaDe(original.getProducto());

        // RN: vivo = VIGENTE o EN_MORA.
        Long estado = original.getIdEstado();
        boolean vivo = estado != null
            && (estado == EstadoPrestamo.VIGENTE || estado == EstadoPrestamo.EN_MORA);
        if (!vivo) {
            ev.motivo = "El préstamo " + original.getCodigo() + " está en estado " + estado
                + "; solo se " + (novacion ? "nova" : "reestructura") + " un préstamo VIGENTE (2) o EN MORA (11).";
            return ev;
        }

        // Cobro en trámite (REGISTRADO o APROBADO): el dinero está en camino y el VS no lo ve.
        int enTramite = 0;
        for (DetalleCobroCredito detalle : detalleCobroCreditoDaoService.selectByPrestamo(original.getCodigo())) {
            Long estadoCobro = detalle.getCobroCredito() != null ? detalle.getCobroCredito().getEstado() : null;
            if (estadoCobro != null && (estadoCobro == CrdEstadoCobro.REGISTRADO
                    || estadoCobro == CrdEstadoCobro.APROBADO)) {
                enTramite++;
            }
        }
        if (enTramite > 0) {
            ev.motivo = "El préstamo " + original.getCodigo() + " tiene " + enTramite
                + " cobro(s) en trámite (registrado o aprobado); procéselos o anúlelos primero.";
            return ev;
        }

        // VS = suma del desglose de la precancelación (RN6): capital + interés + mora + seguros.
        DesgloseConceptosPrestamo desglose = procesoPagoPrestamoService.calcularDesgloseConceptos(
            original.getCodigo(), hoy);
        ev.desglose = desglose;
        ev.valorSustitucion = redondear(desglose.getCapitalPendiente() + desglose.getInteresPendiente()
            + desglose.getMoraPendiente() + desglose.getDesgravamenPendiente()
            + desglose.getSeguroIncendioPendiente());

        // Días de la cuota pendiente más antigua con vencimiento anterior a la fecha de la operación.
        long dias = diasVencidoMasAntiguo(original, hoy);
        ev.conAtraso = dias > 0;
        if (novacion) {
            if (ev.conAtraso) {
                ev.motivo = "El préstamo " + original.getCodigo() + " tiene cuotas vencidas (" + dias
                    + " días la más antigua); no se nova con atrasos. Se reestructura, o el socio se pone al día primero.";
                return ev;
            }
        } else if (dias > MAX_DIAS_VENCIDO_REESTRUCTURAR) {
            ev.motivo = "El préstamo " + original.getCodigo() + " tiene " + dias
                + " días vencido; sobre " + MAX_DIAS_VENCIDO_REESTRUCTURAR
                + " días no se reestructura por el sistema.";
            return ev;
        }

        // Regla de selección del §6: cualquier componente distinto del capital → plantilla CON líneas.
        ev.conLineas = ev.desglose.getInteresPendiente() > 0.005 || ev.desglose.getMoraPendiente() > 0.005
            || ev.desglose.getDesgravamenPendiente() > 0.005 || ev.desglose.getSeguroIncendioPendiente() > 0.005;
        // La novación cuadra solo con VS = capital (§6): una cuota que vence hoy trae interés exigible.
        if (novacion && ev.conLineas) {
            ev.motivo = "El préstamo " + original.getCodigo() + " tiene interés, mora o seguros exigibles hoy"
                + " (una cuota que vence hoy); la novación exige que el valor de sustitución sea solo capital."
                + " Pague esa cuota primero.";
            return ev;
        }

        // La plantilla contable de la operación (p. ej. novación de prendario o hipotecario: no hay).
        String sinPlantilla = contabilidadPrestamoService.motivoSinPlantillaSustitucion(original, novacion,
            ev.conLineas, ev.desglose);
        if (sinPlantilla != null) {
            ev.motivo = sinPlantilla;
            return ev;
        }

        // RN9: el producto del nuevo es de la misma familia y del tipo RESTRUCTURADO o NOVACION.
        if (productoElegido == null) {
            ev.sugerido = buscarProductoSugerido(original, novacion);
            if (ev.sugerido == null) {
                ev.motivo = "No hay un producto " + (novacion ? "NOVACION" : "RESTRUCTURADO")
                    + " activo de la familia " + ev.familia + " para sustituir el préstamo " + original.getCodigo() + ".";
            }
        } else {
            ev.sugerido = productoElegido;
            if (!ev.familia.equals(familiaDe(productoElegido))) {
                ev.motivo = "El producto " + productoElegido.getNombre() + " (familia " + familiaDe(productoElegido)
                    + ") no es de la misma familia que el original (" + ev.familia + ").";
            }
        }
        return ev;
    }

    private long diasVencidoMasAntiguo(Prestamo prestamo, LocalDate hoy) throws Throwable {
        List<DetallePrestamo> exigibles =
            detallePrestamoDaoService.selectCuotasExigibles(prestamo.getCodigo(), hoy.atTime(23, 59, 59));
        LocalDate masAntigua = null;
        if (exigibles != null) {
            for (DetallePrestamo cuota : exigibles) {
                if (cuota.getFechaVencimiento() == null) {
                    continue;
                }
                LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
                if (vencimiento.isBefore(hoy) && (masAntigua == null || vencimiento.isBefore(masAntigua))) {
                    masAntigua = vencimiento;
                }
            }
        }
        return masAntigua != null ? ChronoUnit.DAYS.between(masAntigua, hoy) : 0L;
    }

    private Producto buscarProductoSugerido(Prestamo original, boolean novacion) throws Throwable {
        String familia = familiaDe(original.getProducto());
        String nombreOriginal = normaliza(original.getProducto() != null ? original.getProducto().getNombre() : null);
        Producto primero = null;
        for (Producto candidato : productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO)) {
            if (candidato.getEstado() != null && candidato.getEstado() != Estado.ACTIVO) {
                continue;
            }
            if (!familia.equals(familiaDe(candidato)) || !esDelTipo(candidato, novacion)) {
                continue;
            }
            // Si el nombre del candidato contiene el del original («EMERGENTE RESTRUCTURADO» para
            // «EMERGENTE»), ese gana; si no, el primero de la familia.
            if (!nombreOriginal.isEmpty() && normaliza(candidato.getNombre()).contains(nombreOriginal)) {
                return candidato;
            }
            if (primero == null) {
                primero = candidato;
            }
        }
        return primero;
    }

    private Prestamo cargarOriginal(Prestamo nuevo) throws Throwable {
        Long idOriginal = nuevo != null ? nuevo.getIdPrestamoOriginal() : null;
        if (idOriginal == null) {
            throw new IncomeException("El préstamo no indica el préstamo original a sustituir.");
        }
        if (idOriginal.equals(nuevo.getCodigo())) {
            throw new IncomeException("Un préstamo no se puede sustituir a sí mismo.");
        }
        Prestamo original = prestamoDaoService.find(new Prestamo(), idOriginal);
        if (original == null) {
            throw new IncomeException("No existe el préstamo original " + idOriginal + ".");
        }
        Long entidadNuevo = nuevo.getEntidad() != null ? nuevo.getEntidad().getCodigo() : null;
        Long entidadOriginal = original.getEntidad() != null ? original.getEntidad().getCodigo() : null;
        if (entidadNuevo == null || !entidadNuevo.equals(entidadOriginal)) {
            throw new IncomeException("El préstamo original " + idOriginal + " no es del mismo partícipe.");
        }
        return original;
    }

    /** El producto del nuevo, recargado: el cliente suele mandar solo el código. */
    private Producto cargarProductoDe(Prestamo nuevo) throws Throwable {
        Long idProducto = nuevo.getProducto() != null ? nuevo.getProducto().getCodigo() : null;
        if (idProducto == null) {
            throw new IncomeException("El préstamo no tiene producto.");
        }
        Producto producto = productoDaoService.find(new Producto(), idProducto);
        if (producto == null) {
            throw new IncomeException("No existe el producto " + idProducto + ".");
        }
        return producto;
    }

    /** true = novación, false = reestructuración; lanza si el producto no es de ninguno de los dos. */
    private boolean tipoDeProducto(Producto producto) throws Throwable {
        if (esDelTipo(producto, false)) {
            return false;
        }
        if (esDelTipo(producto, true)) {
            return true;
        }
        throw new IncomeException("El producto " + producto.getNombre()
            + " no es RESTRUCTURADO ni NOVACION; un préstamo que sustituye a otro debe ser de uno de esos productos.");
    }

    private boolean esDelTipo(Producto producto, boolean novacion) {
        String nombre = normaliza(producto != null ? producto.getNombre() : null);
        return novacion ? nombre.contains("NOVACI") : nombre.contains("STRUCTURAD");
    }

    private boolean esTipoNovacion(String tipo) throws Throwable {
        if (TIPO_NOVACION.equals(tipo)) {
            return true;
        }
        if (TIPO_REESTRUCTURACION.equals(tipo)) {
            return false;
        }
        throw new IncomeException("PARAMETRO_INVALIDO: tipo debe ser REESTRUCTURACION o NOVACION");
    }

    /** Familia (tipo de préstamo del producto) en mayúsculas y sin tildes; vacío si no se sabe. */
    private String familiaDe(Producto producto) {
        if (producto == null || producto.getTipoPrestamo() == null) {
            return "";
        }
        return normaliza(producto.getTipoPrestamo().getNombre());
    }

    private String normaliza(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase().trim();
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
