package com.saa.ejb.crd.serviceImpl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.CalculadoraAmortizacionService;
import com.saa.ejb.crd.service.ContabilidadPrestamoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.dto.ContextoSustitucion;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.SaldoPrestamoResumen;
import com.saa.ejb.crd.service.dto.SaldosCuota;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.OrigenPagoExterno;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

@Stateless
public class PrestamoServiceImpl implements PrestamoService {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    /** C17: mes cerrado para cartera / contabilidad (CIERRES-BLOQUEANTES.md). */
    @jakarta.ejb.EJB
    private com.saa.ejb.cnt.service.EstadoCierreService estadoCierreService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.service.AsientoService asientoService;

    @EJB
    private com.saa.ejb.crd.service.SustitucionPrestamoService sustitucionPrestamoService;

    @EJB
    private com.saa.ejb.crd.service.EvaluacionOtorgamientoService evaluacionOtorgamientoService;

    @EJB
    private com.saa.ejb.crd.service.NivelAprobacionService nivelAprobacionService;

    @EJB
    private com.saa.ejb.crd.service.PrimeraCuotaPetroService primeraCuotaPetroService;

    @EJB
    private com.saa.ejb.crd.dao.DetalleCobroCreditoDaoService detalleCobroCreditoDaoService;

    @EJB
    private com.saa.ejb.crd.dao.AcuerdoCondonacionDaoService acuerdoCondonacionDaoService;

    @EJB
    private com.saa.ejb.crd.dao.PrestamoSeguroDaoService prestamoSeguroDaoService;

    @EJB
    private com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private CalculadoraAmortizacionService calculadoraAmortizacionService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    /** U-2: todo proceso que graba valida el usuario PRIMERO (sin usuario válido no se hace nada). */
    @EJB
    private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

    @EJB
    private ContabilidadPrestamoService contabilidadPrestamoService;

    /** C15 / D-018: idempotencia de la orden de desembolso en el re-proceso. */
    @EJB
    private com.saa.ejb.cxp.dao.PagoProgramadoDaoService pagoProgramadoDaoService;

    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    /** C16 (D-021): precancelación del préstamo anterior pagada con el desembolso del nuevo. */
    @EJB
    private com.saa.ejb.crd.service.ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @Override
    public Prestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PrestamoService ... depurado");
        Prestamo prestamo = new Prestamo();
        for (Long registro : id) {
            prestamoDaoService.remove(prestamo, registro);
        }
    }

    @Override
    public void save(List<Prestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PrestamoService");
        for (Prestamo registro : lista) {
            prestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Prestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Prestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un préstamo. D10 (decisión 11 del plan de simuladores): además de persistir tal
     * cual el préstamo recibido, este método deriva {@code interesNominal} (PRSTINNM) de
     * {@code tasa} (PRSTTSAA) EN CADA GUARDADO, aunque el llamador no lo mande — para que
     * {@code ProcesoMoraPrestamoServiceImpl} deje de caer al default silencioso de 9%.
     * Cualquier flujo que guarde un {@link Prestamo} a través de este método escribe PRSTINNM
     * como efecto secundario.
     */
    @Override
    public Prestamo saveSingle(Prestamo prestamo) throws Throwable {
        System.out.println("saveSingle - Prestamo");
        
        // U6.4: el idAsoprep ya no lo manda el cliente, lo asigna aprobar(). Se resuelve ANTES
        // del merge: el merge pisaría la instancia gestionada con lo que mandó el cliente.
        Prestamo existente = prestamo.getCodigo() != null
            ? prestamoDaoService.find(new Prestamo(), prestamo.getCodigo()) : null;
        // Las columnas de la anulación (PRSTASEN/ASRV/USAN/FCAN/MTAN) las escriben solo aprobar() y
        // anular(): el cliente no las manda ni las pisa con un PUT que traiga otro valor.
        if (existente == null) {
            prestamo.setIdAsientoEntrega(null);
            prestamo.setIdAsientoReverso(null);
            prestamo.setUsuarioAnulacion(null);
            prestamo.setFechaAnulacion(null);
            prestamo.setMotivoAnulacion(null);
        } else {
            prestamo.setIdAsientoEntrega(existente.getIdAsientoEntrega());
            prestamo.setIdAsientoReverso(existente.getIdAsientoReverso());
            prestamo.setUsuarioAnulacion(existente.getUsuarioAnulacion());
            prestamo.setFechaAnulacion(existente.getFechaAnulacion());
            prestamo.setMotivoAnulacion(existente.getMotivoAnulacion());
        }
        // Reestructuración y novación (RN12): VS y banderas las escribe el servidor al aprobar.
        // idPrestamoOriginal solo se acepta en el alta o mientras el préstamo esté en 1 o 6.
        if (existente == null) {
            prestamo.setValorSustitucion(null);
            prestamo.setReestructurado(null);
            prestamo.setEsNovacion(null);
            prestamo.setMontoNovacion(null);
        } else {
            prestamo.setValorSustitucion(existente.getValorSustitucion());
            prestamo.setReestructurado(existente.getReestructurado());
            prestamo.setEsNovacion(existente.getEsNovacion());
            prestamo.setMontoNovacion(existente.getMontoNovacion());
            Long estadoPrevio = existente.getIdEstado();
            boolean editableOriginal = estadoPrevio != null && (estadoPrevio == EstadoPrestamo.GENERADO
                || estadoPrevio == EstadoPrestamo.PENDIENTE_DE_APROBACION);
            if (!editableOriginal) {
                prestamo.setIdPrestamoOriginal(existente.getIdPrestamoOriginal());
            }
        }
        boolean validaSustitucion = prestamo.getIdPrestamoOriginal() != null && (existente == null
            || (existente.getIdEstado() != null && (existente.getIdEstado() == EstadoPrestamo.GENERADO
                || existente.getIdEstado() == EstadoPrestamo.PENDIENTE_DE_APROBACION)));
        if (validaSustitucion) {
            // El original es del mismo partícipe, está vivo, cumple RN4, RN9 y RN10, y el monto
            // cumple RN7 (reestructuración) o RN8 (novación). Lanza con el motivo.
            sustitucionPrestamoService.validarAlGuardar(prestamo);
        }
        if (existente == null) {
            prestamo.setIdAsoprep(null);
        } else {
            Long estadoBase = existente.getIdEstado();
            boolean noAprobado = estadoBase != null && (estadoBase == EstadoPrestamo.GENERADO
                || estadoBase == EstadoPrestamo.PENDIENTE_DE_APROBACION
                || estadoBase == EstadoPrestamo.RECHAZADO);
            prestamo.setIdAsoprep(noAprobado ? null : existente.getIdAsoprep());
        }

        if(prestamo.getCodigo() == null){
        	// N5/ciclo de otorgamiento: Estado.ACTIVO era del catálogo equivocado (mismo defecto
        	// N5 de las cuotas). Un préstamo nuevo nace PENDIENTE_DE_APROBACION (6), no en la
        	// pantalla del alta le corresponde tabla ni aprobación todavía. Solo aplica al alta
        	// (codigo == null): no se toca el estado de un préstamo existente en cada edición.
        	prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.PENDIENTE_DE_APROBACION));
		}

        // B5/B2 (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md): un préstamo del otorgamiento (alta o 6/1, y no
        // una reestructuración) lleva la tasa del 9 % y no puede superar el cupo de la cuenta individual. Lanza
        // IncomeException (la capa REST responde 422). No aplica a Petro ni a migración: no pasan por saveSingle.
        evaluacionOtorgamientoService.validarAlGuardar(prestamo, existente);

        // B4: si el préstamo cambia después de una aprobación parcial (monto, plazo, producto o tasa), las aprobaciones
        // vigentes se invalidan y la aprobación vuelve a empezar desde el nivel 1.
        if (existente != null && !java.util.Objects.equals(existente.getMontoSolicitado(), prestamo.getMontoSolicitado())
                || existente != null && !java.util.Objects.equals(existente.getPlazo(), prestamo.getPlazo())
                || existente != null && !java.util.Objects.equals(existente.getTasa(), prestamo.getTasa())
                || existente != null && !java.util.Objects.equals(
                    existente.getProducto() != null ? existente.getProducto().getCodigo() : null,
                    prestamo.getProducto() != null ? prestamo.getProducto().getCodigo() : null)) {
            nivelAprobacionService.invalidarAprobaciones(existente.getCodigo(), "CAMBIO_PRESTAMO");
        }

        // D10 (decisión 11 del plan de simuladores): PRSTTSAA y PRSTINNM son una sola tasa.
        // Se deriva interesNominal de tasa en cada guardado, para que el proceso de mora
        // (ProcesoMoraPrestamoServiceImpl) deje de caer al default silencioso de 9%.
        if (prestamo.getTasa() != null) {
            System.out.println("saveSingle - Prestamo " + prestamo.getCodigo()
                + ": derivando interesNominal (PRSTINNM) de tasa (PRSTTSAA=" + prestamo.getTasa() + ")");
            prestamo.setInteresNominal(prestamo.getTasa());
        }

        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        return prestamo;
    }

    @Override
    public List<Prestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Prestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public Prestamo generarTablaAmortizacion(Long idPrestamo, Long tieneCuotaCero, boolean regenerar) throws Throwable {
        System.out.println("Generando tabla de amortización para préstamo ID: " + idPrestamo + " - Cuota 0: "
            + tieneCuotaCero + " - Regenerar: " + regenerar);

        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }

        if (prestamo.getTipoAmortizacion() == null) {
            throw new IncomeException("El préstamo no tiene definido el tipo de amortización");
        }
        if (prestamo.getPlazo() == null || prestamo.getPlazo() <= 0) {
            throw new IncomeException("El préstamo no tiene definido un plazo válido");
        }
        if (prestamo.getTasa() == null || prestamo.getTasa() <= 0) {
            throw new IncomeException("El préstamo no tiene definida una tasa válida");
        }
        if (prestamo.getMontoSolicitado() == null || prestamo.getMontoSolicitado() <= 0) {
            throw new IncomeException("El préstamo no tiene definido un monto válido");
        }
        if (prestamo.getFechaInicio() == null) {
            throw new IncomeException("El préstamo no tiene definida una fecha de inicio");
        }

        // N1 (BLOQUEANTE, REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md, decisión U2 caso A): el
        // generador no era idempotente — dos llamadas duplicaban la tabla y
        // actualizarCamposPrestamo recalculaba la cabecera sobre la lista nueva, sin delatar
        // la duplicación.
        List<DetallePrestamo> cuotasExistentes = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotasExistentes != null && !cuotasExistentes.isEmpty()) {
            // U4 (PLAN-CICLO-OTORGAMIENTO.md): la tabla se congela al aprobar. Se verifica el
            // estado ANTES que los pagos, por más barato (un campo ya cargado en memoria, no
            // una consulta por cuota). VIGENTE y RECHAZADO bloquean la regeneración SIEMPRE,
            // aunque la tabla no tenga ningún pago. Las dos guardas conviven: esta no reemplaza
            // la de pagos, que sigue haciendo falta para la cartera migrada (entra en VIGENTE
            // con pagos aplicados por otro camino).
            Long idEstadoActual = prestamo.getIdEstado();
            if (idEstadoActual != null
                && (idEstadoActual == EstadoPrestamo.VIGENTE || idEstadoActual == EstadoPrestamo.RECHAZADO
                    || idEstadoActual == EstadoPrestamo.APROBADO)) {
                throw new IncomeException("El préstamo " + idPrestamo + " está en estado "
                    + nombreEstadoPrestamo(idEstadoActual)
                    + "; la tabla de amortización está congelada y no se puede regenerar.");
            }
            if (!regenerar) {
                throw new IncomeException("El préstamo " + idPrestamo + " ya tiene " + cuotasExistentes.size()
                    + " cuota(s) generadas. Para reemplazar la tabla, pedir la regeneración explícita.");
            }
            // Regla del usuario, no negociable: una cuota con pagos no se toca nunca. Se cuentan
            // TODOS los pagos, anulados incluidos (countByIdDetallePrestamo, del equipo A, sin
            // try/catch): un pago anulado sigue teniendo FK a esta cuota, así que borrarla
            // dejaría filas colgando en CRD.PGPR. Si la consulta falla, la excepción sube y
            // aborta la operación entera. La regeneración parcial (preservar las pagadas y
            // re-amortizar el resto) es la máquina de la reestructuración y no va en este
            // cambio — ver §8 U2 caso B.
            for (DetallePrestamo cuota : cuotasExistentes) {
                long totalPagos = pagoPrestamoDaoService.countByIdDetallePrestamo(cuota.getCodigo());
                if (totalPagos > 0) {
                    throw new IncomeException("La cuota " + cuota.getNumeroCuota()
                        + " ya tiene pagos registrados (incluidos anulados); no se puede regenerar la tabla.");
                }
            }
            // Ninguna cuota tiene pagos: no hubo hecho financiero que historiar, así que no se
            // copia a HistDetallePrestamo (copiarla ensuciaría el histórico).
            for (DetallePrestamo cuota : cuotasExistentes) {
                detallePrestamoDaoService.remove(cuota, cuota.getCodigo());
            }
            System.out.println("Regeneración de tabla de amortización: se reemplazaron "
                + cuotasExistentes.size() + " cuota(s) sin pagos del préstamo ID " + idPrestamo);
        }

        if (tieneCuotaCero == null) {
            tieneCuotaCero = 0L;
        }

        List<DetallePrestamo> detalles = new ArrayList<>();
        long tipoAmortizacion = prestamo.getTipoAmortizacion();

        if (tipoAmortizacion == 1L) {
            detalles = generarAmortizacionFrancesa(prestamo, tieneCuotaCero);
        } else if (tipoAmortizacion == 2L) {
            detalles = generarAmortizacionAlemana(prestamo, tieneCuotaCero);
        } else {
            throw new IncomeException("Tipo de amortización no válido. Use 1 para Francesa o 2 para Alemana");
        }

        for (DetallePrestamo detalle : detalles) {
            detallePrestamoDaoService.save(detalle, detalle.getCodigo());
        }

        actualizarCamposPrestamo(prestamo, detalles, tipoAmortizacion);

        // Ciclo de otorgamiento (PLAN-CICLO-OTORGAMIENTO.md §3, regla 2): generar la tabla lleva
        // de PENDIENTE_DE_APROBACION (6) o GENERADO (1, al regenerar) a GENERADO (1). Un préstamo
        // en otro estado (p.ej. cartera migrada ya VIGENTE) no cambia de estado por regenerarle
        // la tabla — ya lo impide la guarda de arriba, pero se deja explícito acá también.
        Long idEstadoPrevio = prestamo.getIdEstado();
        if (idEstadoPrevio != null
            && (idEstadoPrevio == EstadoPrestamo.PENDIENTE_DE_APROBACION || idEstadoPrevio == EstadoPrestamo.GENERADO)) {
            prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.GENERADO));
        }

        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());

        // B4: regenerar la tabla es un cambio del préstamo: la aprobación por niveles vuelve a empezar.
        nivelAprobacionService.invalidarAprobaciones(idPrestamo, "TABLA_REGENERADA");

        System.out.println("Tabla de amortización generada exitosamente con " + detalles.size() + " cuotas");
        return prestamo;
    }

    /** Nombre legible de un código de EstadoPrestamo, para mensajes de error que lo nombren. */
    private String nombreEstadoPrestamo(Long idEstado) {
        if (idEstado == null) {
            return "SIN ESTADO";
        }
        int codigo = idEstado.intValue();
        switch (codigo) {
            case EstadoPrestamo.GENERADO: return "GENERADO (1)";
            case EstadoPrestamo.VIGENTE: return "VIGENTE (2)";
            case EstadoPrestamo.CANCELADO: return "CANCELADO (3)";
            case EstadoPrestamo.CANCELADO_ANTICIPADO: return "CANCELADO_ANTICIPADO (4)";
            case EstadoPrestamo.CANCELADO_POR_NOVACION: return "CANCELADO_POR_NOVACION (5)";
            case EstadoPrestamo.PENDIENTE_DE_APROBACION: return "PENDIENTE_DE_APROBACION (6)";
            case EstadoPrestamo.RECHAZADO: return "RECHAZADO (7)";
            case EstadoPrestamo.DE_PLAZO_VENCIDO: return "DE_PLAZO_VENCIDO (8)";
            case EstadoPrestamo.CANCELADO_POR_REVISAR: return "CANCELADO_POR_REVISAR (9)";
            case EstadoPrestamo.VIGENTE_POR_REVISAR: return "VIGENTE_POR_REVISAR (10)";
            case EstadoPrestamo.EN_MORA: return "EN_MORA (11)";
            case EstadoPrestamo.ANULADO: return "ANULADO (12)";
            case EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION: return "CANCELADO_POR_REESTRUCTURACION (13)";
            case EstadoPrestamo.APROBADO: return "APROBADO (14, pendiente de desembolso)";
            default: return "estado " + codigo;
        }
    }

    @Override
    public Prestamo aprobar(Long idPrestamo, String usuario, String observacion, Long idEmpresa, Long idUsuario)
            throws Throwable {
        return aprobar(idPrestamo, usuario, observacion, idEmpresa, idUsuario, null);
    }

    @Override
    public Prestamo aprobar(Long idPrestamo, String usuario, String observacion, Long idEmpresa, Long idUsuario,
            LocalDate fechaContable) throws Throwable {
        return aprobarConNiveles(idPrestamo, usuario, observacion, idEmpresa, idUsuario, fechaContable).getPrestamo();
    }

    @Override
    public com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo aprobarConNiveles(Long idPrestamo, String usuario,
            String observacion, Long idEmpresa, Long idUsuario, LocalDate fechaContable) throws Throwable {
        System.out.println("Aprobando préstamo ID: " + idPrestamo + " - Usuario: " + usuario
            + " - Fecha contable: " + fechaContable);
        // U-2: lo primero, el usuario.
        usuario = usuarioValidacionService.validar(usuario);
        // U-3: el id sale del nombre ya validado (el idUsuario del cuerpo, nulo o en 0, ya no llega a Tesorería).
        idUsuario = usuarioValidacionService.idPorNombre(usuario);

        if (idEmpresa == null) {
            throw new IncomeException("No se puede aprobar el préstamo " + idPrestamo + ": falta idEmpresa.");
        }
        // C6 (FECHA-CONTABLE-REPROCESO.md): fecha de la orden de pago del desembolso. Por
        // defecto hoy; explícita para re-procesar un mes pasado. Nunca futura.
        LocalDate fechaOrdenPago = fechaContable != null ? fechaContable : LocalDate.now();
        if (fechaOrdenPago.isAfter(LocalDate.now())) {
            throw new IncomeException("FECHA_CONTABLE_FUTURA: la fecha contable " + fechaOrdenPago
                + " del desembolso del préstamo " + idPrestamo + " es futura.");
        }
        // C17: desembolso / entrega / sustitución (novación, reestructuración): la orden de pago se fecha con
        // fechaOrdenPago; su mes tiene que estar abierto para cartera y contabilidad.
        estadoCierreService.validarMovimientoCartera(idEmpresa, fechaOrdenPago,
                "desembolsos, novaciones ni reestructuraciones (préstamo " + idPrestamo + ")");

        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }

        Long idEstadoActual = prestamo.getIdEstado();
        if (idEstadoActual == null || idEstadoActual != EstadoPrestamo.GENERADO) {
            throw new IncomeException("El préstamo " + idPrestamo + " está en estado "
                + nombreEstadoPrestamo(idEstadoActual) + "; solo se puede aprobar desde GENERADO (1).");
        }

        // Sin tabla no hay nada que aprobar. El estado dice GENERADO; si por algún motivo no
        // tiene cuotas, algo quedó inconsistente y hay que frenar, no aprobar.
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas == null || cuotas.isEmpty()) {
            throw new IncomeException("El préstamo " + idPrestamo
                + " está en estado GENERADO pero no tiene tabla de amortización; no se puede aprobar.");
        }

        double monto = prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0;
        if (monto <= 0.01) {
            throw new IncomeException("El préstamo " + idPrestamo
                + " no tiene un monto solicitado válido; no se puede aprobar.");
        }

        // GA2: un prendario o hipotecario sin suma asegurada no registra su garantía en cuentas
        // de orden. Se rechaza ANTES de numerar y de contabilizar, para no perder ningún idAsoprep.
        // La familia se resuelve igual que la plantilla de entrega (Producto.tipoPrestamo.nombre).
        String tipoPrestamoNombre = (prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            && prestamo.getProducto().getTipoPrestamo().getNombre() != null)
            ? prestamo.getProducto().getTipoPrestamo().getNombre().trim().toUpperCase() : "";
        if (("PRENDARIO".equals(tipoPrestamoNombre) || "HIPOTECARIO".equals(tipoPrestamoNombre))
                && (prestamo.getValorAsegurado() == null || prestamo.getValorAsegurado() <= 0.0)) {
            throw new IncomeException("Falta la suma asegurada del bien: sin ella no se registra la garantía"
                + " en cuentas de orden");
        }

        // B2: capacidad de pago (≤ 50 % de la remuneración) y cupo (≤ 100 % de la cuenta individual) bloquean la
        // aprobación; sin remuneración tampoco se aprueba. ANTES de numerar y de tocar nada. No aplica a la
        // reestructuración de un préstamo en mora. IncomeException → 422.
        evaluacionOtorgamientoService.validarAlAprobar(prestamo);

        // Primera cuota en el mes de inicio con el archivo Petro de ese mes ya generado: 422 pidiendo regenerar la tabla.
        // En cada nivel, ANTES de registrar la aprobación.
        primeraCuotaPetroService.validarAlAprobar(prestamo);

        // B4: aprobación por NIVELES. Se valida B2 en cada nivel (arriba) y se registra el nivel pendiente; si no es el
        // último el préstamo sigue GENERADO y no se toca nada más. Si es el último, sigue el aprobar de siempre en ESTA
        // misma transacción: si algo falla más abajo, la aprobación del nivel se revierte con todo. Sin reglas activas
        // no registra nada y se aprueba como hoy, en un paso.
        com.saa.ejb.crd.service.dto.ResultadoNivelAprobacion nivel =
            nivelAprobacionService.registrarAprobacion(prestamo, idUsuario, usuario, observacion);
        if (nivel.isConNiveles() && !nivel.isEsUltimo()) {
            com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo parcial =
                new com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo();
            parcial.setPrestamo(prestamo);
            parcial.setAprobacionCompleta(false);
            parcial.setNivelAprobado(nivel.getOrdenAprobado());
            parcial.setTotalNiveles(nivel.getTotalNiveles());
            parcial.setSiguienteNivel(nivel.getSiguienteNivel());
            parcial.setMensaje("Nivel " + nivel.getOrdenAprobado() + " de " + nivel.getTotalNiveles()
                + " aprobado; falta: " + nivel.getSiguienteNivel());
            System.out.println("Préstamo " + idPrestamo + ": " + parcial.getMensaje());
            return parcial;
        }

        // Reestructuración / novación: recalcula el VS y aplica RN7 y RN8 ANTES de gastar el número.
        ContextoSustitucion sustitucion = prestamo.getIdPrestamoOriginal() != null
            ? sustitucionPrestamoService.prepararAprobacion(prestamo) : null;

        // 1b. U6.1: idAsoprep = MAX(PRSTIDAS) + 1, ANTES de la orden de pago y del asiento, con
        // flush en el acto: si otra aprobación tomó el mismo número, el índice único lo avisa
        // acá y no después del trabajo caro. Sin secuencia (U6.2): el número vive en esta misma
        // transacción y el rollback se lo lleva. Si ya trae número no se renumera (U6.5).
        if (prestamo.getIdAsoprep() == null) {
            Long maximo = prestamoDaoService.selectMaxIdAsoprep();
            Long idAsoprepNuevo = (maximo != null ? maximo : 0L) + 1L;
            prestamo.setIdAsoprep(idAsoprepNuevo);
            try {
                em.flush();
            } catch (PersistenceException e) {
                if (esViolacionDeUnicidad(e)) {
                    throw new IncomeException("Otro préstamo se aprobó al mismo tiempo y tomó el número "
                        + idAsoprepNuevo + "; vuelva a aprobar.");
                }
                throw e;
            }
            System.out.println("Préstamo " + idPrestamo + ": idAsoprep asignado = " + idAsoprepNuevo);
        } else {
            System.out.println("Préstamo " + idPrestamo + ": conserva su idAsoprep = " + prestamo.getIdAsoprep());
        }

        // 2. Orden de pago del desembolso, PRIMERO — mismo criterio que
        // DevolucionAporteServiceImpl:540 ("un log que anuncia éxito antes de que la operación
        // pueda fallar es peor que no tener log"): si CXP rechaza la orden, no se toca el
        // asiento ni el estado del préstamo.
        // RN8: la novación desembolsa solo el NETO (monto − VS). RN7: la reestructuración no mueve
        // dinero al socio, así que no hay orden de desembolso (PRSTIDPG queda nulo).
        boolean generaOrden = sustitucion == null || sustitucion.isNovacion();
        double valorOrden = sustitucion != null ? sustitucion.getNeto() : monto;
        Long idPago = null;
        if (generaOrden) {
        try {
            List<LineaContablePago> desglose = armarDesgloseEntrega(prestamo, valorOrden, idEmpresa);
            BeneficiarioOcasional beneficiario = armaBeneficiarioPrestamo(prestamo.getEntidad());
            String observacionPago = "Desembolso préstamo N° " + prestamo.getCodigo()
                + (prestamo.getEntidad() != null ? " - " + prestamo.getEntidad().getRazonSocial() : "");

            // idCuentaBancariaOrigen SIEMPRE null (mismo criterio que la devolución de aportes):
            // con cuenta nula el pago nace POR_APROBAR y tesorería asigna cuenta/forma de pago
            // al aprobar.
            Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.CRD_DESEMBOLSO_PRESTAMO, prestamo.getCodigo(),
                idEmpresa, null, valorOrden, fechaOrdenPago.toString(),
                beneficiario, desglose, observacionPago, idUsuario,
                false, null);

            Object valorPago = (respuesta != null) ? respuesta.get("pago") : null;
            if (valorPago == null) {
                throw new IncomeException("Cuentas por Pagar no devolvió el número de la orden.");
            }
            idPago = ((Number) valorPago).longValue();

        } catch (com.saa.basico.util.UsuarioInvalidoException e) {
            throw e; // U-5b §5.C: el rechazo por usuario inválido llega tal cual, sin re-envolver
        } catch (IncomeException e) {
            throw new IncomeException("No se pudo generar la orden de pago del desembolso del"
                + " préstamo " + idPrestamo + ": " + e.getMessage());
        } catch (Throwable e) {
            throw new IncomeException("No se pudo generar la orden de pago del desembolso del"
                + " préstamo " + idPrestamo + ": " + e.getMessage());
        }
        }

        // 3. Reestructuración (no sale dinero, no hay orden de pago): sigue como siempre, asiento del traslado, cancelar
        // el original y VIGENTE al aprobar. Todo lo demás (préstamo nuevo y novación) queda APROBADO (14): el asiento
        // de entrega (o de sustitución + cancelar el original) se hace al CONFIRMARSE el pago del desembolso
        // (activarPorDesembolso, desde tesorería), cuando el dinero sale; hasta entonces el préstamo no cuenta en ningún lado.
        boolean reestructuracion = sustitucion != null && !sustitucion.isNovacion();
        Long idAsientoEntrega = null;
        if (reestructuracion) {
            idAsientoEntrega = contabilidadPrestamoService.contabilizarSustitucion(prestamo, cuotas,
                sustitucion.getOriginal(), sustitucion.getDesglose(), sustitucion.isNovacion(),
                sustitucion.getNeto(), sustitucion.isConLineas(), idEmpresa, usuario);
            // RN11/RN12: cancela el original DESPUÉS del asiento (que necesita sus saldos vivos).
            sustitucionPrestamoService.cancelarOriginal(prestamo, sustitucion, idAsientoEntrega, usuario);
        } else if (sustitucion != null) {
            // Novación: el VS se FIJA al aprobar (es el que se descontó del neto de la orden de pago); el desembolso lo
            // compara con el VS real a esa fecha.
            prestamo.setValorSustitucion(redondear(sustitucion.getValorSustitucion()));
        }

        // 4. Estado, auditoría y PRSTIDPG.
        prestamo.setIdEstado(Long.valueOf(reestructuracion ? EstadoPrestamo.VIGENTE : EstadoPrestamo.APROBADO));
        prestamo.setUsuarioAprobacion(usuario);
        prestamo.setFechaAprobacion(LocalDateTime.now());
        prestamo.setIdPagoProgramado(idPago);
        prestamo.setIdAsientoEntrega(idAsientoEntrega); // AN4: sin este id no hay cómo reversar (en 14 se llena al desembolsar)
        if (observacion != null && !observacion.trim().isEmpty()) {
            prestamo.setObservacion(concatenarObservacion(prestamo.getObservacion(), observacion));
        }

        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("Préstamo " + idPrestamo + " aprobado por " + usuario + " - orden de pago "
            + idPago + (idAsientoEntrega != null ? " - asiento de entrega " + idAsientoEntrega : ""));
        com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo completo =
            new com.saa.ejb.crd.service.dto.ResultadoAprobacionPrestamo();
        completo.setPrestamo(prestamo);
        completo.setAprobacionCompleta(true);
        if (nivel.isConNiveles()) {
            completo.setNivelAprobado(nivel.getOrdenAprobado());
            completo.setTotalNiveles(nivel.getTotalNiveles());
            completo.setMensaje("Nivel " + nivel.getOrdenAprobado() + " de " + nivel.getTotalNiveles()
                + " aprobado; préstamo aprobado");
        } else {
            completo.setMensaje(reestructuracion ? "Préstamo aprobado"
                : "Préstamo aprobado, pendiente de desembolso: pasa a VIGENTE cuando tesorería confirme el pago");
        }
        return completo;
    }

    @Override
    public com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo resumenSolicitudes(LocalDate desde, LocalDate hasta,
            Long idFilial, Long idProducto) throws Throwable {
        System.out.println("PrestamoServiceImpl.resumenSolicitudes - " + desde + " a " + hasta + " - filial " + idFilial
            + " - producto " + idProducto);
        LocalDate hoy = LocalDate.now();
        LocalDate d = desde != null ? desde : hoy.withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : hoy.withDayOfMonth(hoy.lengthOfMonth());
        if (h.isBefore(d)) {
            throw new IncomeException("PARAMETRO_INVALIDO: «hasta» (" + h + ") es anterior a «desde» (" + d + ")");
        }
        Object[] f = prestamoDaoService.selectResumenSolicitudes(d.atStartOfDay(), h.plusDays(1).atStartOfDay(),
            idFilial, idProducto);
        com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo r = new com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo();
        r.setDesde(d);
        r.setHasta(h);
        r.setIdFilial(idFilial);
        r.setIdProducto(idProducto);
        r.setRecibidas(cifra(f, 0));
        r.setEnTramite(cifra(f, 2));
        r.setAprobadas(cifra(f, 4));
        r.setDesembolsadas(cifra(f, 6));
        r.setRechazadas(cifra(f, 8));
        r.setAnuladas(cifra(f, 10));
        return r;
    }

    private static com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo.Cifra cifra(Object[] fila, int i) {
        return new com.saa.ejb.crd.service.dto.ResumenSolicitudesPrestamo.Cifra(
            ((Number) fila[i]).longValue(), redondearCentavo(((Number) fila[i + 1]).doubleValue()));
    }

    @Override
    public String activarPorDesembolso(Long idPrestamo, LocalDate fechaDesembolso, Long idEmpresa, String usuario)
            throws Throwable {
        System.out.println("PrestamoServiceImpl.activarPorDesembolso - préstamo: " + idPrestamo + " - fecha: "
            + fechaDesembolso + " - usuario: " + usuario);
        Prestamo prestamo = idPrestamo != null ? prestamoDaoService.find(new Prestamo(), idPrestamo) : null;
        if (prestamo == null || prestamo.getIdEstado() == null || prestamo.getIdEstado() != EstadoPrestamo.APROBADO) {
            // Idempotente y compatible: ya VIGENTE (aprobado antes del cambio) u otro estado → no se toca nada.
            System.out.println("  activarPorDesembolso: el préstamo " + idPrestamo + " no está en APROBADO (estado "
                + (prestamo != null ? prestamo.getIdEstado() : null) + "); no se hace nada.");
            return null;
        }
        LocalDate fecha = fechaDesembolso != null ? fechaDesembolso : LocalDate.now();
        if (idEmpresa == null) {
            throw new IncomeException("No se puede activar el préstamo " + idPrestamo + " por desembolso: falta la empresa del pago.");
        }
        estadoCierreService.validarMovimientoCartera(idEmpresa, fecha,
            "el desembolso del préstamo " + idPrestamo + " (asiento de entrega)");

        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas == null || cuotas.isEmpty()) {
            throw new IncomeException("El préstamo " + idPrestamo + " no tiene tabla de amortización; no se puede activar.");
        }
        double monto = prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0;
        StringBuilder advertencia = new StringBuilder();
        Long idAsiento;

        if (prestamo.getIdPrestamoOriginal() == null) {
            idAsiento = contabilidadPrestamoService.contabilizarEntrega(prestamo, cuotas, idEmpresa, monto, usuario, fecha);
        } else {
            // Novación: se cancela el original con el VS REAL de hoy (para que su cartera cuadre).
            double vsFijado = prestamo.getValorSustitucion() != null ? prestamo.getValorSustitucion() : 0.0;
            ContextoSustitucion sustitucion = sustitucionPrestamoService.prepararDesembolso(prestamo);
            if (sustitucion.getMotivoNoNovable() == null) {
                double vsReal = redondear(sustitucion.getValorSustitucion());
                idAsiento = contabilidadPrestamoService.contabilizarSustitucion(prestamo, cuotas,
                    sustitucion.getOriginal(), sustitucion.getDesglose(), sustitucion.isNovacion(),
                    sustitucion.getNeto(), sustitucion.isConLineas(), idEmpresa, usuario, fecha);
                sustitucionPrestamoService.cancelarOriginal(prestamo, sustitucion, idAsiento, usuario);
                if (Math.abs(vsReal - vsFijado) > 0.005) {
                    double dif = redondear(vsReal - vsFijado);
                    advertencia.append("Novación del préstamo ").append(prestamo.getCodigo())
                        .append(": el valor de sustitución cambió entre la aprobación ($").append(formatoMoneda(vsFijado))
                        .append(", el que se descontó del desembolso) y el desembolso ($").append(formatoMoneda(vsReal))
                        .append("); diferencia $").append(formatoMoneda(dif))
                        .append(dif > 0 ? " cobrada de MENOS al partícipe" : " cobrada de MÁS al partícipe")
                        .append(". El original se canceló con el valor real. Regularizar a mano (no se creó ninguna devolución).");
                }
            } else {
                // El original ya no se puede novar: el nuevo pasa a VIGENTE con su asiento de entrega normal y el original
                // queda como está. Deja en SOCIOS POR PAGAR el crédito del VS sin pagar: caso manual.
                idAsiento = contabilidadPrestamoService.contabilizarEntrega(prestamo, cuotas, idEmpresa, monto, usuario, fecha);
                advertencia.append("ATENCIÓN, caso manual — novación del préstamo ").append(prestamo.getCodigo())
                    .append(": el original ").append(prestamo.getIdPrestamoOriginal())
                    .append(" ya no se puede novar (").append(sustitucion.getMotivoNoNovable())
                    .append("). El préstamo nuevo quedó VIGENTE con su asiento de entrega normal y el original NO se canceló: ")
                    .append("el partícipe tiene los dos vigentes y en socios por pagar queda a su favor el valor de sustitución ($")
                    .append(formatoMoneda(vsFijado)).append(") que se descontó del desembolso. Crédito debe cancelar el original a mano.");
            }
        }

        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.VIGENTE));
        prestamo.setIdAsientoEntrega(idAsiento); // AN4: sin este id no hay cómo reversar
        // PRSTFCAC/PRSTUSAC: desde ahora son la fecha y el usuario del DESEMBOLSO (sin escritor hasta este cambio).
        prestamo.setFechaAcreditacion(fecha.atStartOfDay());
        prestamo.setUsuarioAcreditacion(usuario);
        if (advertencia.length() > 0) {
            prestamo.setObservacion(acotarObservacion(concatenarObservacion(prestamo.getObservacion(),
                advertencia.toString())));
        }
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("Préstamo " + idPrestamo + " VIGENTE por desembolso - fecha " + fecha + " - asiento de entrega "
            + idAsiento + (advertencia.length() > 0 ? " - ADVERTENCIA: " + advertencia : ""));
        return advertencia.length() > 0 ? advertencia.toString() : null;
    }

    @Override
    public void revertirDesembolso(Long idPrestamo, String motivo, LocalDate fechaReverso, String usuario)
            throws Throwable {
        System.out.println("PrestamoServiceImpl.revertirDesembolso - préstamo: " + idPrestamo + " - fecha: " + fechaReverso);
        Prestamo prestamo = idPrestamo != null ? prestamoDaoService.find(new Prestamo(), idPrestamo) : null;
        // Solo un préstamo activado por desembolso (VIGENTE con fecha de desembolso y asiento de entrega). Uno aprobado
        // antes del cambio no se toca: su reverso siempre fue solo del pago.
        if (prestamo == null || prestamo.getIdEstado() == null || prestamo.getIdEstado() != EstadoPrestamo.VIGENTE
                || prestamo.getFechaAcreditacion() == null || prestamo.getIdAsientoEntrega() == null) {
            return;
        }
        if (prestamo.getIdPrestamoOriginal() != null) {
            throw new IncomeException("El préstamo " + idPrestamo + " es una novación: su desembolso canceló el préstamo "
                + prestamo.getIdPrestamoOriginal() + " y no se revierte por sistema. Anule la novación con crédito (caso manual).");
        }
        if (!pagoPrestamoDaoService.selectVigentesByPrestamo(idPrestamo).isEmpty()) {
            throw new IncomeException("El préstamo " + idPrestamo + " ya tiene pagos aplicados; no se puede revertir su desembolso.");
        }
        LocalDate fecha = fechaReverso != null ? fechaReverso : LocalDate.now();
        com.saa.model.cnt.Asiento asientoOriginal = asientoService.reversionAsiento(prestamo.getIdAsientoEntrega(), fecha);
        prestamo.setIdAsientoReverso(asientoOriginal != null ? asientoOriginal.getIdReversion() : null);
        prestamo.setIdAsientoEntrega(null);
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.APROBADO));
        prestamo.setFechaAcreditacion(null);
        prestamo.setUsuarioAcreditacion(null);
        prestamo.setObservacion(acotarObservacion(concatenarObservacion(prestamo.getObservacion(),
            "Desembolso revertido por " + usuario + ": " + motivo)));
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("Préstamo " + idPrestamo + " vuelve a APROBADO; asiento de reverso " + prestamo.getIdAsientoReverso());
    }

    /** PRSTOBSR mide 2000: si la bitácora crece más, se conserva lo último. */
    private static String acotarObservacion(String observacion) {
        return observacion != null && observacion.length() > 2000
            ? observacion.substring(observacion.length() - 2000) : observacion;
    }

    private static String formatoMoneda(double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor);
    }

    /**
     * C15 / D-018 (H6) — contabiliza el desembolso de un préstamo YA APROBADO que quedó sin
     * asiento de entrega (re-proceso jun-2025 en adelante), exactamente como se contabiliza hoy un
     * préstamo nuevo pero con la fecha del desembolso real del extracto bancario:
     * <ol>
     * <li>asiento de entrega del motor ({@code contabilizarEntrega}, plantilla 34/9/13) fechado
     *     {@code fechaContable};</li>
     * <li>orden de pago en CxP desde la cuenta bancaria del extracto, como débito ya realizado por
     *     el banco ({@code debitoAutomatico}): nace CONFIRMADA y genera el egreso (D socios por
     *     pagar / H banco) y su movimiento bancario con esa misma fecha y la referencia del
     *     extracto.</li>
     * </ol>
     * <b>Idempotente por préstamo:</b> si ya tiene asiento de entrega no genera otro; si ya tiene
     * una orden de pago vigente no crea otra (lo informa: esa orden se gestiona en tesorería).
     * Las sustituciones (reestructura/novación) se rechazan: su asiento es el del traslado.
     *
     * @return mapa con {@code idPrestamo, idAsientoEntrega, idPagoProgramado, entregaGenerada,
     *         pagoGenerado, mensaje}
     */
    @Override
    public Map<String, Object> contabilizarDesembolsoHistorico(Long idPrestamo, Long idEmpresa, Long idUsuario,
            String usuario, LocalDate fechaContable, Long idCuentaBancaria, Double valor, String referencia)
            throws Throwable {
        return contabilizarDesembolsoHistorico(idPrestamo, idEmpresa, idUsuario, usuario, fechaContable,
            idCuentaBancaria, valor, referencia, null, null, null, null, null);
    }

    private static double redondearCentavo(double valor) {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * C16 (D-021) — regla de cuadre del desembolso neto: bruto (monto del préstamo) = neto transferido
     * + capital + interés + seguro de la precancelación del préstamo anterior + otros descuentos.
     * Devuelve la diferencia {@code bruto − (neto + descuentos)} redondeada al centavo; el ítem se
     * acepta solo si es 0,00. Pura (sin base), para poder probarla aislada.
     */
    static double diferenciaCuadreDesembolsoNeto(double bruto, double neto, double capitalCancelado,
            double interesCancelado, double seguroCancelado, double otrosDescuentos) {
        java.math.BigDecimal suma = java.math.BigDecimal.valueOf(neto)
            .add(java.math.BigDecimal.valueOf(capitalCancelado))
            .add(java.math.BigDecimal.valueOf(interesCancelado))
            .add(java.math.BigDecimal.valueOf(seguroCancelado))
            .add(java.math.BigDecimal.valueOf(otrosDescuentos));
        return java.math.BigDecimal.valueOf(bruto).subtract(suma)
            .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * C16 (D-021) — desembolso NETO: además de lo de C15 (entrega + orden de pago con egreso), si el
     * préstamo nuevo canceló un préstamo anterior del mismo partícipe, registra esa precancelación con
     * el motor ({@code ProcesoPagoPrestamoService.precancelarConPrestamoNuevo}), pagada con el dinero
     * del nuevo (DEBE socios por pagar), y los otros descuentos; la orden de pago va por el
     * {@code valor} = NETO que transfirió el banco. Regla: bruto = neto + capital + interés + seguro +
     * otros, al centavo. Orden: entrega (bruto) → precancelación → otros descuentos → orden (neto).
     * Todo con {@code fechaContable}, en la transacción del ítem. Idempotente paso a paso.
     * Ver docs/logica-negocio/crd/DESEMBOLSO-NETO-CON-PRECANCELACION.md.
     */
    @Override
    public Map<String, Object> contabilizarDesembolsoHistorico(Long idPrestamo, Long idEmpresa, Long idUsuario,
            String usuario, LocalDate fechaContable, Long idCuentaBancaria, Double valor, String referencia,
            Long idPrestamoCancelado, Double capitalCancelado, Double interesCancelado, Double seguroCancelado,
            List<com.saa.ejb.crd.service.dto.DescuentoDesembolso> otrosDescuentos) throws Throwable {
        System.out.println("PrestamoServiceImpl.contabilizarDesembolsoHistorico - préstamo: " + idPrestamo
            + " - fecha: " + fechaContable + " - cuenta: " + idCuentaBancaria + " - valor (neto): " + valor
            + " - préstamo cancelado: " + idPrestamoCancelado);
        // U-2: lo primero, el usuario (antes se rellenaba con «CUADRE»).
        usuario = usuarioValidacionService.validar(usuario);
        // U-3: el id sale del nombre ya validado.
        idUsuario = usuarioValidacionService.idPorNombre(usuario);
        if (idPrestamo == null || idEmpresa == null || fechaContable == null) {
            throw new IncomeException("idPrestamo, idEmpresa y fechaContable son obligatorios.");
        }
        if (fechaContable.isAfter(LocalDate.now())) {
            throw new IncomeException("FECHA_CONTABLE_FUTURA: " + fechaContable);
        }
        // C17: mes de la fecha contable abierto para cartera y contabilidad.
        estadoCierreService.validarMovimientoCartera(idEmpresa, fechaContable,
                "la contabilización del desembolso histórico del préstamo " + idPrestamo);
        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }
        long estado = prestamo.getIdEstado() != null ? prestamo.getIdEstado().longValue() : 0L;
        if (estado == EstadoPrestamo.RAIZ || estado == EstadoPrestamo.GENERADO
                || estado == EstadoPrestamo.PENDIENTE_DE_APROBACION || estado == EstadoPrestamo.RECHAZADO
                || estado == EstadoPrestamo.ANULADO || estado == EstadoPrestamo.APROBADO) {
            throw new IncomeException("El préstamo " + idPrestamo + " está en estado " + estado
                + ": no fue aprobado (use POST /prst/aprobar/{id} con fechaContable).");
        }
        if (prestamo.getIdPrestamoOriginal() != null) {
            throw new IncomeException("El préstamo " + idPrestamo + " sustituye al " + prestamo.getIdPrestamoOriginal()
                + " (reestructura/novación): su asiento es el del traslado, no una entrega. Fuera de alcance de este lote.");
        }
        double monto = prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0;

        // C16 (D-021): componentes de la precancelación del préstamo anterior (los que falten se
        // calculan desde CRD.DTPR; los enviados tienen que coincidir) y otros descuentos.
        double capital = 0.0;
        double interes = 0.0;
        double seguro = 0.0;
        com.saa.ejb.crd.service.dto.ResultadoPrecancelacionConPrestamo precancelacionExistente = null;
        if (idPrestamoCancelado != null) {
            Prestamo anterior = prestamoDaoService.find(new Prestamo(), idPrestamoCancelado);
            if (anterior == null) {
                throw new IncomeException("Préstamo cancelado con ID " + idPrestamoCancelado + " no encontrado");
            }
            Long entidadAnterior = anterior.getEntidad() != null ? anterior.getEntidad().getCodigo() : null;
            Long entidadNuevo = prestamo.getEntidad() != null ? prestamo.getEntidad().getCodigo() : null;
            if (entidadAnterior == null || !entidadAnterior.equals(entidadNuevo) || idPrestamoCancelado.equals(idPrestamo)) {
                throw new IncomeException("El préstamo cancelado " + idPrestamoCancelado + " no es otro préstamo del mismo"
                    + " partícipe que el " + idPrestamo + " (entidades " + entidadAnterior + " / " + entidadNuevo + ").");
            }
            precancelacionExistente = procesoPagoPrestamoService.buscarPrecancelacionConPrestamo(idPrestamoCancelado,
                idPrestamo);
            double capitalCalculado;
            double interesCalculado;
            double seguroCalculado;
            if (precancelacionExistente != null) {
                capitalCalculado = precancelacionExistente.getCapital();
                interesCalculado = precancelacionExistente.getInteres();
                seguroCalculado = precancelacionExistente.getSeguro();
            } else {
                com.saa.ejb.crd.service.dto.ComponentesPrecancelacionConPrestamo componentes =
                    procesoPagoPrestamoService.calcularPrecancelacionConPrestamo(idPrestamoCancelado, fechaContable);
                capitalCalculado = componentes.getCapital();
                interesCalculado = componentes.getInteres();
                seguroCalculado = componentes.getSeguro();
            }
            capital = capitalCancelado != null ? capitalCancelado : capitalCalculado;
            interes = interesCancelado != null ? interesCancelado : interesCalculado;
            seguro = seguroCancelado != null ? seguroCancelado : seguroCalculado;
            if (Math.abs(capital - capitalCalculado) > 0.005 || Math.abs(interes - interesCalculado) > 0.005
                    || Math.abs(seguro - seguroCalculado) > 0.005) {
                throw new IncomeException("La precancelación enviada del préstamo " + idPrestamoCancelado + " (capital "
                    + capital + ", interés " + interes + ", seguro " + seguro + ") no coincide con la "
                    + (precancelacionExistente != null ? "ya registrada (evento " + precancelacionExistente.getIdEvento() + ")"
                        : "calculada desde CRD.DTPR") + " (capital " + capitalCalculado + ", interés " + interesCalculado
                    + ", seguro " + seguroCalculado + ").");
            }
        } else if ((capitalCancelado != null && Math.abs(capitalCancelado) > 0.005)
                || (interesCancelado != null && Math.abs(interesCancelado) > 0.005)
                || (seguroCancelado != null && Math.abs(seguroCancelado) > 0.005)) {
            throw new IncomeException("El ítem del préstamo " + idPrestamo + " trae valores de precancelación sin"
                + " idPrestamoCancelado.");
        }
        double otros = 0.0;
        if (otrosDescuentos != null) {
            for (com.saa.ejb.crd.service.dto.DescuentoDesembolso descuento : otrosDescuentos) {
                if (descuento == null || descuento.getValor() == null || descuento.getValor() <= 0.005
                        || descuento.getCuenta() == null || descuento.getCuenta().trim().isEmpty()) {
                    throw new IncomeException("Cada otro descuento del préstamo " + idPrestamo
                        + " necesita concepto, valor > 0 y cuenta.");
                }
                otros += descuento.getValor();
            }
        }
        // Sin valor y sin descuentos: como C15, el neto es el monto.
        double neto = valor != null ? valor : monto - capital - interes - seguro - otros;
        double diferencia = diferenciaCuadreDesembolsoNeto(monto, neto, capital, interes, seguro, otros);
        if (Math.abs(diferencia) > 0.005) {
            throw new IncomeException("El desembolso del préstamo " + idPrestamo + " no cuadra: bruto " + monto
                + " ≠ neto " + neto + " + capital " + capital + " + interés " + interes + " + seguro " + seguro
                + " + otros " + redondearCentavo(otros) + " (diferencia " + diferencia + ").");
        }
        if (neto < -0.005) {
            throw new IncomeException("El neto del desembolso del préstamo " + idPrestamo + " es negativo (" + neto + ").");
        }
        neto = redondearCentavo(neto);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idPrestamo", idPrestamo);
        resultado.put("idAsoprep", prestamo.getIdAsoprep());
        resultado.put("fechaContable", fechaContable.toString());
        resultado.put("referencia", referencia);
        resultado.put("bruto", redondearCentavo(monto));
        resultado.put("neto", neto);
        resultado.put("idPrestamoCancelado", idPrestamoCancelado);
        resultado.put("capitalCancelado", redondearCentavo(capital));
        resultado.put("interesCancelado", redondearCentavo(interes));
        resultado.put("seguroCancelado", redondearCentavo(seguro));
        resultado.put("otrosDescuentos", redondearCentavo(otros));
        StringBuilder mensaje = new StringBuilder();

        // 1. Asiento de entrega (idempotente).
        boolean entregaGenerada = false;
        if (prestamo.getIdAsientoEntrega() == null) {
            List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
            Long idAsiento = contabilidadPrestamoService.contabilizarEntrega(prestamo, cuotas, idEmpresa, monto,
                usuario, fechaContable);
            prestamo.setIdAsientoEntrega(idAsiento);
            entregaGenerada = idAsiento != null;
            mensaje.append(idAsiento != null ? "Asiento de entrega " + idAsiento + " al " + fechaContable + ". "
                : "Contabilidad de CRD inactiva: sin asiento de entrega. ");
        } else {
            mensaje.append("Ya tenía asiento de entrega ").append(prestamo.getIdAsientoEntrega()).append(". ");
        }

        // 2. C16 (D-021): precancelación del préstamo anterior pagada con este desembolso (idempotente).
        boolean precancelacionGenerada = false;
        if (idPrestamoCancelado != null) {
            com.saa.ejb.crd.service.dto.ResultadoPrecancelacionConPrestamo precancelacion = precancelacionExistente != null
                ? precancelacionExistente
                : procesoPagoPrestamoService.precancelarConPrestamoNuevo(idPrestamoCancelado, idPrestamo, idEmpresa,
                    usuario, fechaContable, capital, interes, seguro,
                    "Desembolso neto (re-proceso, extracto " + fechaContable
                        + (referencia != null ? ", " + referencia : "") + ").");
            precancelacionGenerada = !precancelacion.isYaExistia();
            resultado.put("idEventoPrecancelacion", precancelacion.getIdEvento());
            resultado.put("idAsientoPrecancelacion", precancelacion.getIdAsiento());
            resultado.put("idsPagoPrestamoPrecancelacion", precancelacion.getIdsPagoPrestamo());
            resultado.put("capitalFuturoPrecancelacion", precancelacion.getCapitalFuturo());
            resultado.put("tablaPrecancelacion", precancelacion.getTabla());
            mensaje.append(precancelacionGenerada ? "Precancelación del préstamo " : "Ya tenía la precancelación del préstamo ")
                .append(idPrestamoCancelado).append(" (evento ").append(precancelacion.getIdEvento())
                .append(precancelacion.getIdAsiento() != null ? ", asiento " + precancelacion.getIdAsiento() : "")
                .append(") por ").append(precancelacion.getTotal()).append(". ");
        }

        // 3. C16 (D-021): otros descuentos del desembolso (idempotente por la marca en la observación).
        if (otrosDescuentos != null && !otrosDescuentos.isEmpty()) {
            String marcaDescuentos = "[D-021 otros descuentos del desembolso: asiento ";
            String observacionActual = prestamo.getObservacion() != null ? prestamo.getObservacion() : "";
            int posicion = observacionActual.indexOf(marcaDescuentos);
            if (posicion >= 0) {
                mensaje.append("Ya tenía el asiento de otros descuentos. ");
            } else {
                Long idAsientoDescuentos = contabilidadPrestamoService.contabilizarDescuentosDesembolso(prestamo, idEmpresa,
                    fechaContable, otrosDescuentos, usuario);
                resultado.put("idAsientoOtrosDescuentos", idAsientoDescuentos);
                if (idAsientoDescuentos != null) {
                    prestamo.setObservacion(concatenarObservacion(prestamo.getObservacion(),
                        marcaDescuentos + idAsientoDescuentos + "]"));
                }
                mensaje.append("Otros descuentos ").append(redondearCentavo(otros))
                    .append(idAsientoDescuentos != null ? " (asiento " + idAsientoDescuentos + ")" : "").append(". ");
            }
        }

        // 4. Orden de pago + egreso por el NETO (idempotente).
        boolean pagoGenerado = false;
        List<com.saa.model.cxp.PagoProgramado> vigentes = pagoProgramadoDaoService.selectVigentesByOrigen(
            OrigenPagoExterno.CRD_DESEMBOLSO_PRESTAMO, idPrestamo);
        if (prestamo.getIdPagoProgramado() != null || (vigentes != null && !vigentes.isEmpty())) {
            Long existente = prestamo.getIdPagoProgramado() != null ? prestamo.getIdPagoProgramado()
                : vigentes.get(0).getId();
            prestamo.setIdPagoProgramado(existente);
            mensaje.append("Ya tenía la orden de pago ").append(existente)
                .append(": no se crea otra (su egreso se gestiona en tesorería).");
        } else if (idCuentaBancaria == null) {
            mensaje.append("Sin cuenta bancaria: no se genera la orden de pago ni el egreso.");
        } else if (neto <= 0.005) {
            mensaje.append("Neto 0: todo el desembolso se fue en descuentos, no hay orden de pago.");
        } else {
            List<LineaContablePago> desglose = armarDesgloseEntrega(prestamo, neto, idEmpresa);
            BeneficiarioOcasional beneficiario = armaBeneficiarioPrestamo(prestamo.getEntidad());
            Map<String, Object> respuesta = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.CRD_DESEMBOLSO_PRESTAMO, idPrestamo, idEmpresa, idCuentaBancaria, neto,
                fechaContable.toString(), beneficiario, desglose,
                "Desembolso préstamo N° " + prestamo.getCodigo() + " (re-proceso, extracto " + fechaContable + ")",
                idUsuario, true, referencia);
            Object idPago = respuesta != null ? respuesta.get("pago") : null;
            if (idPago == null) {
                throw new IncomeException("Cuentas por Pagar no devolvió el número de la orden del préstamo " + idPrestamo);
            }
            prestamo.setIdPagoProgramado(((Number) idPago).longValue());
            pagoGenerado = true;
            mensaje.append("Orden de pago ").append(idPago).append(" confirmada con egreso al ").append(fechaContable)
                .append(respuesta.get("asiento") != null ? " (asiento " + respuesta.get("asiento") + ")" : "")
                .append(".");
        }
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        resultado.put("idAsientoEntrega", prestamo.getIdAsientoEntrega());
        resultado.put("idPagoProgramado", prestamo.getIdPagoProgramado());
        resultado.put("entregaGenerada", entregaGenerada);
        resultado.put("precancelacionGenerada", precancelacionGenerada);
        resultado.put("pagoGenerado", pagoGenerado);
        resultado.put("mensaje", mensaje.toString().trim());
        return resultado;
    }

    /** ORA-00001 (índice único violado) en algún punto de la cadena de causas. */
    private boolean esViolacionDeUnicidad(Throwable e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof java.sql.SQLException && ((java.sql.SQLException) causa).getErrorCode() == 1) {
                return true;
            }
            String mensaje = causa.getMessage();
            if (mensaje != null && mensaje.contains("ORA-00001")) {
                return true;
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return false;
    }

    /**
     * Desglose de la orden de desembolso. El producto de pago NO está quemado: se deduce de la
     * cuenta SOCIOS POR PAGAR de la plantilla de entrega del préstamo (§6.2 de
     * DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md). Cero o más de un producto → IncomeException.
     */
    private List<LineaContablePago> armarDesgloseEntrega(Prestamo prestamo, double monto, Long idEmpresa)
            throws Throwable {
        LineaContablePago linea = new LineaContablePago();
        linea.setIdProductoPago(contabilidadPrestamoService.resolverProductoPagoDesembolso(prestamo, idEmpresa));
        linea.setValor(monto);
        linea.setConcepto("Desembolso préstamo " + prestamo.getCodigo()
            + (prestamo.getEntidad() != null ? " - " + prestamo.getEntidad().getRazonSocial() : ""));
        List<LineaContablePago> desglose = new ArrayList<>();
        desglose.add(linea);
        return desglose;
    }

    /**
     * Beneficiario del desembolso — el socio. Sin cuenta bancaria: el contrato de
     * {@code aprobar} no la pide (PLAN-DESEMBOLSO-PRESTAMO.md §5), así que tesorería la
     * completa al aprobar la orden, igual que la cuenta de origen.
     */
    private BeneficiarioOcasional armaBeneficiarioPrestamo(Entidad entidad) {
        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        if (entidad != null) {
            beneficiario.setNombre(entidad.getRazonSocial());
            beneficiario.setIdentificacion(entidad.getNumeroIdentificacion());
        }
        return beneficiario;
    }

    // =====================================================================
    // Anulación del préstamo otorgado (DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md, AN1–AN11)
    // =====================================================================

    @Override
    public String motivoNoAnulable(Prestamo prestamo) throws Throwable {
        System.out.println("PrestamoService.motivoNoAnulable - Préstamo: "
            + (prestamo != null ? prestamo.getCodigo() : null));
        if (prestamo == null) {
            return "El préstamo no existe.";
        }
        Long idPrestamo = prestamo.getCodigo();

        // AN1: VIGENTE (2), EN_MORA (11) o APROBADO (14, aprobado y todavía sin desembolsar).
        Long idEstado = prestamo.getIdEstado();
        boolean aprobadoSinDesembolso = idEstado != null && idEstado == EstadoPrestamo.APROBADO;
        if (idEstado == null || (idEstado != EstadoPrestamo.VIGENTE && idEstado != EstadoPrestamo.EN_MORA
                && !aprobadoSinDesembolso)) {
            return "El préstamo " + idPrestamo + " está en estado " + nombreEstadoPrestamo(idEstado)
                + "; solo se puede anular un préstamo APROBADO (14), VIGENTE (2) o EN_MORA (11).";
        }

        // RN13: un préstamo que sustituyó a otro no se anula en esta fase (habría que resucitar el original). Un
        // APROBADO no sustituyó nada todavía (el original se cancela al desembolsar): se anula sin problema.
        if (!aprobadoSinDesembolso && prestamo.getIdPrestamoOriginal() != null) {
            return "El préstamo " + idPrestamo + " sustituyó al préstamo " + prestamo.getIdPrestamoOriginal()
                + " (reestructuración o novación); no se puede anular en esta fase, habría que resucitar el original.";
        }

        // AN4: sin el id del asiento de entrega no hay cómo reversarlo. En APROBADO no hay asiento que reversar.
        if (!aprobadoSinDesembolso && prestamo.getIdAsientoEntrega() == null) {
            return "No se encuentra el asiento de entrega del préstamo " + idPrestamo
                + " (PRSTASEN vacío, aprobado antes de este cambio); reverse manualmente.";
        }

        // AN2 — vía 1: pagos aplicados (PGPR vigentes).
        int pagos = pagoPrestamoDaoService.selectVigentesByPrestamo(idPrestamo).size();
        if (pagos > 0) {
            return "El préstamo " + idPrestamo + " tiene " + pagos + " pago(s) aplicado(s) (CRD.PGPR);"
                + " solo se anula un préstamo sin ningún pago.";
        }

        // AN2 — directa sobre las cuotas (pedido del usuario): ninguna cuota pagada. Estado
        // (DTPRESTD) PAGADA 4 / PARCIAL 6 / CANCELADA_ANTICIPADA 7, o cualquier monto pagado > 0,005.
        List<DetallePrestamo> cuotasDelPrestamo = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        int cuotasConPagos = 0;
        DetallePrestamo primeraConPagos = null;
        if (cuotasDelPrestamo != null) {
            for (DetallePrestamo cuota : cuotasDelPrestamo) {
                Long estadoCuota = cuota.getEstado();
                boolean estadoPagado = estadoCuota != null
                    && (estadoCuota == EstadoCuotaPrestamo.PAGADA
                        || estadoCuota == EstadoCuotaPrestamo.PARCIAL
                        || estadoCuota == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
                boolean montoPagado = nvl(cuota.getCapitalPagado()) > 0.005
                    || nvl(cuota.getInteresPagado()) > 0.005
                    || nvl(cuota.getMoraPagado()) > 0.005
                    || nvl(cuota.getDesgravamenPagado()) > 0.005
                    || nvl(cuota.getInteresVendidoPagado()) > 0.005
                    || nvl(cuota.getAbono()) > 0.005;
                if (estadoPagado || montoPagado) {
                    cuotasConPagos++;
                    if (primeraConPagos == null || nvl(cuota.getNumeroCuota()) < nvl(primeraConPagos.getNumeroCuota())) {
                        primeraConPagos = cuota;
                    }
                }
            }
        }
        if (cuotasConPagos > 0) {
            Double numeroPrimera = primeraConPagos.getNumeroCuota();
            return "El préstamo " + idPrestamo + " tiene " + cuotasConPagos + " cuota(s) con pagos (la primera: cuota #"
                + (numeroPrimera != null ? String.valueOf(numeroPrimera.longValue()) : "?") + ", estado "
                + textoEstadoCuota(primeraConPagos.getEstado())
                + "); solo se anula un préstamo sin ninguna cuota pagada.";
        }

        // AN2 — vía 2: detalles de cobro en un cobro REGISTRADO, APROBADO o PROCESADO.
        int cobros = 0;
        for (com.saa.model.crd.DetalleCobroCredito detalle : detalleCobroCreditoDaoService.selectByPrestamo(idPrestamo)) {
            Long estadoCobro = detalle.getCobroCredito() != null ? detalle.getCobroCredito().getEstado() : null;
            if (estadoCobro != null && (estadoCobro == com.saa.rubros.CrdEstadoCobro.REGISTRADO
                    || estadoCobro == com.saa.rubros.CrdEstadoCobro.APROBADO
                    || estadoCobro == com.saa.rubros.CrdEstadoCobro.PROCESADO)) {
                cobros++;
            }
        }
        if (cobros > 0) {
            return "El préstamo " + idPrestamo + " tiene " + cobros + " detalle(s) de cobro (CRD.DCBC) en un"
                + " cobro REGISTRADO, APROBADO o PROCESADO; anule o rechace esos cobros primero.";
        }

        // AN2 — vía 5: acuerdos de condonación VIGENTE o APLICADO.
        int acuerdos = 0;
        for (com.saa.model.crd.AcuerdoCondonacion acuerdo : acuerdoCondonacionDaoService.selectByPrestamo(idPrestamo)) {
            Long estadoAcuerdo = acuerdo.getEstado();
            if (estadoAcuerdo != null && (estadoAcuerdo == com.saa.rubros.CrdEstadoAcuerdoCondonacion.VIGENTE
                    || estadoAcuerdo == com.saa.rubros.CrdEstadoAcuerdoCondonacion.APLICADO)) {
                acuerdos++;
            }
        }
        if (acuerdos > 0) {
            return "El préstamo " + idPrestamo + " tiene " + acuerdos + " acuerdo(s) de condonación"
                + " (CRD.ACCN) VIGENTE o APLICADO; anúlelos primero.";
        }

        // AN6: póliza de seguro viva.
        int polizas = prestamoSeguroDaoService.selectVivosByPrestamo(idPrestamo).size();
        if (polizas > 0) {
            return "El préstamo " + idPrestamo + " está en " + polizas + " documento(s) de póliza de seguro"
                + " vivo(s) (CRD.PSPR); sáquelo primero por nota de crédito.";
        }

        // AN5 (guarda aproximada, decisión del árbitro): no hay rastro por préstamo de un cierre de
        // cartera, así que se bloquea si hay una corrida EJECUTADA de la empresa del asiento de
        // entrega con período >= el de la aprobación. En APROBADO no hay asiento (no entró a la cartera).
        com.saa.model.cnt.Asiento asientoEntrega = aprobadoSinDesembolso ? null
            : asientoService.selectById(prestamo.getIdAsientoEntrega());
        if (asientoEntrega != null && asientoEntrega.getEmpresa() != null) {
            java.time.LocalDate referencia = prestamo.getFechaAprobacion() != null
                ? prestamo.getFechaAprobacion().toLocalDate() : asientoEntrega.getFechaAsiento();
            if (referencia != null) {
                long periodoAprobacion = referencia.getYear() * 100L + referencia.getMonthValue();
                com.saa.model.crd.CorridaCierreCartera masAntigua = null;
                for (com.saa.model.crd.CorridaCierreCartera corrida
                        : corridaCierreCarteraDaoService.selectByEmpresa(asientoEntrega.getEmpresa().getCodigo())) {
                    boolean ejecutada = corrida.getIdEstado() != null
                        && corrida.getIdEstado() == com.saa.rubros.EstadoCorridaCierreCartera.EJECUTADA;
                    boolean activa = corrida.getEstado() == null || corrida.getEstado() == Estado.ACTIVO;
                    if (ejecutada && activa && corrida.getAnio() != null && corrida.getMes() != null
                            && corrida.getAnio() * 100L + corrida.getMes() >= periodoAprobacion) {
                        masAntigua = corrida; // la lista viene de la más reciente a la más antigua
                    }
                }
                if (masAntigua != null) {
                    return "Ya existe el cierre de cartera de " + String.format("%02d", masAntigua.getMes())
                        + "/" + masAntigua.getAnio() + ", posterior a la aprobación; reverse el cierre antes de anular.";
                }
            }
        }

        // AN3: la orden de desembolso en CXP — se pregunta con la misma regla que usa anularPago.
        if (prestamo.getIdPagoProgramado() != null) {
            Map<String, Object> seguimiento = pagoProgramadoService.obtenerSeguimiento(prestamo.getIdPagoProgramado());
            if (seguimiento == null) {
                return "No se encontró la orden de desembolso " + prestamo.getIdPagoProgramado() + " en tesorería.";
            }
            // Orden ya ANULADA (tesorería la anuló por su lado) o RECHAZADA: estado terminal, no salió
            // dinero. No bloquea: se salta anularPago y se sigue con el reverso del asiento.
            Object acciones = ordenDesembolsoTerminal(seguimiento) ? null : seguimiento.get("acciones");
            if (acciones instanceof Map) {
                Map<?, ?> mapaAcciones = (Map<?, ?>) acciones;
                if (Boolean.FALSE.equals(mapaAcciones.get("puedeAnular"))) {
                    Object motivoCxp = mapaAcciones.get("motivoNoAnular");
                    return motivoCxp != null ? motivoCxp.toString()
                        : "La orden de desembolso " + prestamo.getIdPagoProgramado() + " no se puede anular.";
                }
            }
        }
        return null;
    }

    private String textoEstadoCuota(Long estado) {
        if (estado == null) {
            return "sin estado";
        }
        switch (estado.intValue()) {
            case EstadoCuotaPrestamo.PENDIENTE: return "PENDIENTE (1)";
            case EstadoCuotaPrestamo.ACTIVA: return "ACTIVA (2)";
            case EstadoCuotaPrestamo.EMITIDA: return "EMITIDA (3)";
            case EstadoCuotaPrestamo.PAGADA: return "PAGADA (4)";
            case EstadoCuotaPrestamo.EN_MORA: return "EN_MORA (5)";
            case EstadoCuotaPrestamo.PARCIAL: return "PARCIAL (6)";
            case EstadoCuotaPrestamo.CANCELADA_ANTICIPADA: return "CANCELADA_ANTICIPADA (7)";
            case EstadoCuotaPrestamo.VENCIDA: return "VENCIDA (8)";
            case EstadoCuotaPrestamo.ANULADA: return "ANULADA (9)";
            default: return "estado " + estado;
        }
    }

    /** La orden de CXP está ANULADA (5) o RECHAZADA (4): terminal, no hay nada que anular en tesorería. */
    private boolean ordenDesembolsoTerminal(Map<String, Object> seguimiento) {
        if (seguimiento == null || !(seguimiento.get("pago") instanceof Map)) {
            return false;
        }
        Object estado = ((Map<?, ?>) seguimiento.get("pago")).get("estado");
        if (!(estado instanceof Number)) {
            return false;
        }
        int valor = ((Number) estado).intValue();
        return valor == com.saa.rubros.EstadoPagoProgramado.ANULADO
            || valor == com.saa.rubros.EstadoPagoProgramado.RECHAZADO;
    }

    @Override
    public Prestamo anular(Long idPrestamo, String usuario, String motivo, Long idEmpresa, Long idUsuario)
            throws Throwable {
        System.out.println("Anulando préstamo ID: " + idPrestamo + " - Usuario: " + usuario);
        // U-2: lo primero, el usuario existe (helper único); sin usuario válido no se hace nada.
        usuario = usuarioValidacionService.validar(usuario);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("No se puede anular el préstamo " + idPrestamo + ": falta el motivo.");
        }
        if (idEmpresa == null || idUsuario == null) {
            throw new IncomeException("No se puede anular el préstamo " + idPrestamo
                + ": faltan idEmpresa o idUsuario.");
        }

        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }

        // Validaciones (AN1, AN2, AN4, AN5, AN6 y la regla de CXP): la misma función que el GET.
        String motivoNoAnulable = motivoNoAnulable(prestamo);
        if (motivoNoAnulable != null) {
            throw new IncomeException(motivoNoAnulable);
        }
        // En APROBADO (14) no hubo asiento de entrega ni entró a la cartera: no hay nada que reversar ni cierre que validar.
        boolean aprobadoSinDesembolso = prestamo.getIdEstado() != null
            && prestamo.getIdEstado() == EstadoPrestamo.APROBADO;
        if (!aprobadoSinDesembolso) {
            // C17: la anulación se registra hoy (reverso del asiento de entrega): el mes de hoy tiene que estar
            // abierto para cartera; y si el desembolso es de un mes ya cerrado para cartera, su contabilidad
            // también tiene que estar cerrada (si no, reabrir la cartera de ese mes).
            estadoCierreService.validarMovimientoCartera(idEmpresa, LocalDate.now(), "la anulación del préstamo " + idPrestamo);
            estadoCierreService.validarAnulacionCartera(idEmpresa,
                    prestamo.getFechaInicio() != null ? prestamo.getFechaInicio().toLocalDate() : null,
                    "la anulación del préstamo " + idPrestamo);
        }

        // AN3: la orden de desembolso. Si CXP no deja, su texto sale tal cual y no se toca nada.
        if (prestamo.getIdPagoProgramado() != null
                && !ordenDesembolsoTerminal(pagoProgramadoService.obtenerSeguimiento(prestamo.getIdPagoProgramado()))) {
            pagoProgramadoService.anularPago(prestamo.getIdPagoProgramado(),
                "Anulación del préstamo " + idPrestamo + ": " + motivo.trim(), idUsuario);
        }

        // AN4/AN11: reverso del asiento de entrega, fechado hoy. Si el período de hoy no está
        // abierto, contabilidad lanza y la transacción entera se revierte.
        if (!aprobadoSinDesembolso) {
            com.saa.model.cnt.Asiento asientoOriginal = asientoService.reversionAsiento(
                prestamo.getIdAsientoEntrega(), LocalDate.now());
            prestamo.setIdAsientoReverso(asientoOriginal != null ? asientoOriginal.getIdReversion() : null);
        }

        // AN7: cuotas a ANULADA.
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        if (cuotas != null) {
            for (DetallePrestamo cuota : cuotas) {
                cuota.setEstado(Long.valueOf(EstadoCuotaPrestamo.ANULADA));
                cuota.setIdEstado(Long.valueOf(EstadoCuotaPrestamo.ANULADA));
                detallePrestamoDaoService.save(cuota, cuota.getCodigo());
            }
        }

        // AN7/AN8/AN9: estado ANULADO y auditoría. El idAsoprep NO se toca.
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.ANULADO));
        prestamo.setUsuarioAnulacion(usuario.trim());
        prestamo.setFechaAnulacion(LocalDateTime.now());
        prestamo.setMotivoAnulacion(motivo.trim());
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("Préstamo " + idPrestamo + " anulado por " + usuario + " - asiento de reverso "
            + prestamo.getIdAsientoReverso());
        return prestamo;
    }

    @Override
    public Prestamo rechazar(Long idPrestamo, String usuario, String observacion) throws Throwable {
        System.out.println("Rechazando préstamo ID: " + idPrestamo + " - Usuario: " + usuario);
        // U-2: lo primero, el usuario.
        usuario = usuarioValidacionService.validar(usuario);

        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }

        Long idEstadoActual = prestamo.getIdEstado();
        boolean estadoValido = idEstadoActual != null
            && (idEstadoActual == EstadoPrestamo.PENDIENTE_DE_APROBACION || idEstadoActual == EstadoPrestamo.GENERADO);
        if (!estadoValido) {
            throw new IncomeException("El préstamo " + idPrestamo + " está en estado "
                + nombreEstadoPrestamo(idEstadoActual)
                + "; solo se puede rechazar desde PENDIENTE_DE_APROBACION (6) o GENERADO (1).");
        }

        // B4: con niveles configurados, solo un usuario de algún nivel de la regla puede negar, y queda la negación del
        // nivel pendiente en la bitácora. Sin niveles aplicables, se rechaza como hoy.
        nivelAprobacionService.registrarRechazo(prestamo, usuarioValidacionService.idPorNombre(usuario), usuario,
            observacion);

        // La tabla de amortización NO se borra: queda como evidencia de qué se le ofreció al
        // socio. Es inerte para el proceso de mora, que filtra PRSTIDST IN (2,8,10,11) — el 7
        // no está en esa lista.
        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.RECHAZADO));
        prestamo.setUsuarioRechazo(usuario);
        prestamo.setFechaRechazo(LocalDateTime.now());
        if (observacion != null && !observacion.trim().isEmpty()) {
            prestamo.setObservacion(concatenarObservacion(prestamo.getObservacion(), observacion));
        }

        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        System.out.println("Préstamo " + idPrestamo + " rechazado por " + usuario);
        return prestamo;
    }

    /** Concatena en vez de pisar: hay observaciones previas de la migración que no hay que perder. */
    private String concatenarObservacion(String observacionPrevia, String observacionNueva) {
        if (observacionPrevia == null || observacionPrevia.trim().isEmpty()) {
            return observacionNueva;
        }
        return observacionPrevia + " | " + observacionNueva;
    }
    
    private List<DetallePrestamo> generarAmortizacionFrancesa(Prestamo prestamo, Long tieneCuotaCero) throws Throwable {
        return generarAmortizacion(prestamo, tieneCuotaCero, 1L);
    }

    private List<DetallePrestamo> generarAmortizacionAlemana(Prestamo prestamo, Long tieneCuotaCero) throws Throwable {
        return generarAmortizacion(prestamo, tieneCuotaCero, 2L);
    }

    /**
     * Arma los parámetros desde el préstamo ya guardado, delega el cálculo puro en
     * {@link CalculadoraAmortizacionService} (fase 1 de PLAN-SIMULADORES-PRESTAMOS.md — una
     * sola fórmula para todo el sistema) y mapea el resultado a {@code DetallePrestamo},
     * llenando los campos de bitácora que la calculadora no conoce (mora, abono, estado, ...).
     */
    private List<DetallePrestamo> generarAmortizacion(Prestamo prestamo, Long tieneCuotaCero, long tipoAmortizacion)
            throws Throwable {

        ParametrosAmortizacion params = new ParametrosAmortizacion();
        params.setMonto(prestamo.getMontoSolicitado());
        params.setTasaAnual(prestamo.getTasa());
        params.setPlazo(prestamo.getPlazo().intValue());
        params.setTipoAmortizacion(tipoAmortizacion);
        params.setFechaInicio(prestamo.getFechaInicio());
        params.setTieneCuotaCero(tieneCuotaCero != null && tieneCuotaCero == 1L);
        // DISENO-PRIMERA-CUOTA-SEGUN-ARCHIVO-PETRO.md: si el archivo Petro del mes de inicio (filial del partícipe) aún no
        // se generó, la cuota 1 vence a fin del mes de inicio; si ya se generó, al mes siguiente como siempre.
        params.setPrimeraCuotaEnMesInicio(primeraCuotaPetroService.primeraCuotaEnMesInicio(
            prestamo.getFechaInicio() != null ? prestamo.getFechaInicio().toLocalDate() : null,
            prestamo.getEntidad() != null ? prestamo.getEntidad().getCodigo() : null));
        // U1 (decisión del usuario, 2026-08-31 — REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md §8):
        // el desgravamen del generador real se calcula con la misma fórmula que el simulador
        // (saldo * 0.9/1000 antes de amortizar cada cuota), para que la tabla que el sistema
        // genera coincida con la simulación que el socio firma. Efecto aceptado: la cuota 0 de
        // gracia cobra desgravamen sobre el capital completo, porque durante la gracia el
        // capital está íntegramente expuesto.
        params.setCalcularDesgravamenSobreSaldo(true);
        params.setDesgravamenPorCuota(0.0); // sin uso: el flag de arriba en true lo reemplaza
        // El seguro de incendio queda en 0 DELIBERADAMENTE: no se cobra mientras no exista la
        // póliza que lo respalde (decisión U1). No es un pendiente por implementar.
        params.setSeguroIncendioPorCuota(0.0);

        List<CuotaProyectada> tabla = calculadoraAmortizacionService.calcular(params);

        List<DetallePrestamo> detalles = new ArrayList<>();
        for (CuotaProyectada proyectada : tabla) {
            detalles.add(construirDetalle(prestamo, proyectada));
        }
        return detalles;
    }

    /** Mapea una fila pura de la calculadora a una entidad DTPR con TODOS los campos del invariante llenos. */
    private DetallePrestamo construirDetalle(Prestamo prestamo, CuotaProyectada proyectada) {
        DetallePrestamo detalle = new DetallePrestamo();

        detalle.setPrestamo(prestamo);
        detalle.setNumeroCuota(proyectada.getNumeroCuota());
        detalle.setFechaVencimiento(proyectada.getFechaVencimiento());
        detalle.setCapital(proyectada.getCapital());
        detalle.setInteres(proyectada.getInteres());
        detalle.setCuota(proyectada.getCuota());

        double saldoCapital = redondear(Math.max(0.0, nvl(proyectada.getSaldoCapital())));
        detalle.setSaldoCapital(saldoCapital);

        detalle.setMora(0.0);
        detalle.setInteresVencido(0.0);
        detalle.setSaldoInteres(proyectada.getInteres());
        detalle.setSaldoMora(0.0);
        detalle.setSaldoInteresVencido(0.0);
        detalle.setAbono(0.0);
        detalle.setCapitalPagado(0.0);
        detalle.setInteresPagado(0.0);
        // La calculadora ya llena desgravamen/seguroIncendio/total con el mismo invariante que
        // DTPRTTLL (decisión 15 del plan): se copian tal cual, sin recalcular acá.
        double desgravamen = nvl(proyectada.getDesgravamen());
        double seguro = nvl(proyectada.getSeguroIncendio());
        detalle.setDesgravamen(desgravamen);
        detalle.setSaldoOtros(0.0);
        detalle.setDesgravamenFirmado(desgravamen);
        detalle.setDesgravamenDiferido(0.0);
        detalle.setDesgravamenOriginal(desgravamen);
        detalle.setValorDiferido(0.0);
        // Invariante: saldoInicialCapital = capital + saldoCapital + saldoOtros (saldoOtros = 0 acá)
        detalle.setSaldoInicialCapital(redondear(nvl(proyectada.getCapital()) + saldoCapital));
        detalle.setValorSeguroIncendio(seguro);
        detalle.setTotal(redondear(nvl(proyectada.getTotal())));
        detalle.setTotalConSeguro(detalle.getTotal());
        // D5: DTPRSLDO tenía la semántica equivocada acá (capital pendiente). Los otros seis
        // escritores del repo (MotorPagoPrestamoServiceImpl, AbonoCapitalPrestamoServiceImpl,
        // CargaArchivoPetroServiceImpl) escriben, sin excepción, el importe pendiente por
        // cobrar — que es el total de la cuota en una cuota recién generada, sin pagos.
        detalle.setSaldo(detalle.getTotal());
        // N5: el motor de pagos razona con EstadoCuotaPrestamo (PENDIENTE=1), no con Estado.
        // Coincidían por número; se deja de depender de la coincidencia. DTPRESTD es la fuente
        // y DTPRIDST la espeja (SINCRONIZACION-DTPRIDST-DTPRESTD.md): mismo valor en las dos.
        detalle.setEstado(Long.valueOf(EstadoCuotaPrestamo.PENDIENTE));
        detalle.setIdEstado(Long.valueOf(EstadoCuotaPrestamo.PENDIENTE));

        return detalle;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private void actualizarCamposPrestamo(Prestamo prestamo, List<DetallePrestamo> detalles, long tipoAmortizacion) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        double totalCapital = 0.0;
        double totalInteres = 0.0;
        LocalDateTime fechaFin = null;

        for (DetallePrestamo detalle : detalles) {
            if (detalle.getCapital() != null) {
                totalCapital += detalle.getCapital();
            }
            if (detalle.getInteres() != null) {
                totalInteres += detalle.getInteres();
            }

            if (detalle.getFechaVencimiento() != null) {
                fechaFin = detalle.getFechaVencimiento();
            }
        }

        double valorCuota = calcularValorCuotaRepresentativa(detalles, tipoAmortizacion);
        prestamo.setValorCuota(redondear(valorCuota));
        prestamo.setFechaFin(fechaFin);
        prestamo.setTotalCapital(redondear(totalCapital));
        prestamo.setTotalInteres(redondear(totalInteres));
        
        double tasaAnual = prestamo.getTasa();
        prestamo.setTasaNominal(redondear(tasaAnual));
        
        double tasaMensual = tasaAnual / 100 / 12;
        double tasaEfectiva = (Math.pow(1 + tasaMensual, 12) - 1) * 100;
        prestamo.setTasaEfectiva(redondear(tasaEfectiva));
        
        double totalPrestamo = totalCapital + totalInteres;
        prestamo.setTotalPrestamo(redondear(totalPrestamo));
        
        System.out.println("Préstamo actualizado - Valor Cuota: " + valorCuota + 
                         ", Total Capital: " + totalCapital + 
                         ", Total Interés: " + totalInteres +
                         ", Tasa Nominal: " + tasaAnual + "%" +
                         ", Tasa Efectiva: " + tasaEfectiva + "%");
    }
    
    /**
     * N2 (REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md): PRSTVLCT es el "valor de cuota" que leen
     * nueve reportes de crd (RPRT_TBLA_ACML, CSPR, CSPT, ESDI, JBPR, JBPT, PNCM, RMJP, RNCP,
     * todos {@code P.PRSTVLCT AS VALOR_CUOTA}). Tomar la primera cuota con numeroCuota > 0 lo
     * infla en francesa, porque desde D1 esa cuota 1 incluye el interés proporcional del mes
     * inicial.
     * <p>
     * Francesa: se toma la cuota 2 como representativa; si el plazo es 1 (no hay cuota 2), se
     * usa la 1.
     * <p>
     * Alemana: la cuota es decreciente cuota a cuota y PRSTVLCT no representa nada — se deja
     * la primera cuota regular, tal como hacía el código antes de este cambio, sin inventar un
     * promedio.
     */
    private double calcularValorCuotaRepresentativa(List<DetallePrestamo> detalles, long tipoAmortizacion) {
        Double cuotaUno = null;
        Double cuotaDos = null;
        for (DetallePrestamo detalle : detalles) {
            if (detalle.getNumeroCuota() == null || detalle.getCuota() == null) {
                continue;
            }
            double numero = detalle.getNumeroCuota();
            if (cuotaUno == null && numero == 1.0) {
                cuotaUno = detalle.getCuota();
            } else if (cuotaDos == null && numero == 2.0) {
                cuotaDos = detalle.getCuota();
            }
        }

        if (tipoAmortizacion == 1L) { // Francesa
            if (cuotaDos != null) {
                return cuotaDos;
            }
            return cuotaUno != null ? cuotaUno : 0.0;
        }
        // Alemana (u otro): primera cuota regular, sin promediar.
        return cuotaUno != null ? cuotaUno : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    @Override
    public Prestamo cargarTablaAmortizacionDesdeExcel(Long idPrestamo, InputStream archivoExcel, String usuario)
            throws Throwable {
        System.out.println("Cargando tabla de amortización desde Excel para préstamo ID: " + idPrestamo
            + " - Usuario: " + usuario);
        // U-2: lo primero, el usuario (los pagos de migración ya no se graban como «SISTEMA»).
        usuario = usuarioValidacionService.validar(usuario);
        
        // Obtener el préstamo
        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }
        
        // U6.7: ya no se valida el idAsoprep acá; solo aprobar() lo escribe.

        List<DetallePrestamo> detalles = new ArrayList<>();
        double totalCapital = 0.0;
        double totalInteres = 0.0;
        double valorCuotaPrimera = 0.0;
        LocalDateTime fechaFin = null;
        // Saldo de capital acumulado para calcular correctamente en cuotas pagadas
        double saldoCapitalAcumulado = prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0;
        
        try (Workbook workbook = WorkbookFactory.create(archivoExcel)) {
            Sheet sheet = workbook.getSheetAt(0); // Primera hoja
            
            // Formateador de fechas (ajustar según formato del Excel)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            // Iterar desde la fila 2 (asumiendo que fila 1 es encabezado)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Verificar si la fila está vacía
                Cell firstCell = row.getCell(0);
                if (firstCell == null || firstCell.getCellType() == CellType.BLANK) {
                    continue;
                }
                
                DetallePrestamo detalle = new DetallePrestamo();
                
                // NroCuota (columna 0)
                Double numeroCuota = getCellValueAsDouble(row.getCell(0));
                detalle.setNumeroCuota(numeroCuota);
                
                // FECHA VENCE (columna 1)
                LocalDateTime fechaVencimiento = getCellValueAsDate(row.getCell(1), formatter);
                detalle.setFechaVencimiento(fechaVencimiento);
                if (fechaVencimiento != null) {
                    fechaFin = fechaVencimiento;
                }
                
                // PAGO EXTRA (columna 2) → saldoOtros
                Double pagoExtra = getCellValueAsDouble(row.getCell(2));
                detalle.setSaldoOtros(pagoExtra != null ? pagoExtra : 0.0);
                
                // SALDO DE CAPITAL (columna 3) → referencia del Excel (puede venir en 0 para cuotas pagadas)
                // Se usa saldoCapitalAcumulado como saldoInicialCapital para garantizar consistencia
                Double saldoInicialCapital = redondear(saldoCapitalAcumulado);
                
                // PAGO DE CAPITAL (columna 4) → capital
                Double pagoCapital = getCellValueAsDouble(row.getCell(4));
                detalle.setCapital(pagoCapital != null ? pagoCapital : 0.0);
                totalCapital += (pagoCapital != null ? pagoCapital : 0.0);
                
                // Calcular saldoCapital = saldoInicialCapital - capital - pagoExtra y actualizar acumulado
                saldoCapitalAcumulado -= (pagoCapital != null ? pagoCapital : 0.0);
                saldoCapitalAcumulado -= (pagoExtra != null ? pagoExtra : 0.0);
                Double saldoCapitalCalculado = redondear(Math.max(0, saldoCapitalAcumulado));
                detalle.setSaldoCapital(saldoCapitalCalculado);
                detalle.setSaldo(saldoCapitalCalculado);
                
                // Establecer saldoInicialCapital
                detalle.setSaldoInicialCapital(saldoInicialCapital);
                
                // VALOR DEL INTERÉS (columna 5) → interes
                Double valorInteres = getCellValueAsDouble(row.getCell(5));
                detalle.setInteres(valorInteres != null ? valorInteres : 0.0);
                totalInteres += (valorInteres != null ? valorInteres : 0.0);
                
                // DESGRAVAMEN (columna 6)
                Double desgravamen = getCellValueAsDouble(row.getCell(6));
                detalle.setDesgravamen(desgravamen != null ? desgravamen : 0.0);
                detalle.setDesgravamenOriginal(desgravamen != null ? desgravamen : 0.0);
                detalle.setDesgravamenFirmado(desgravamen != null ? desgravamen : 0.0);
                
                // SEGURO (columna 7) → valorSeguroIncendio
                Double seguro = getCellValueAsDouble(row.getCell(7));
                detalle.setValorSeguroIncendio(seguro != null ? seguro : 0.0);
                
                // CUOTA A PAGAR (columna 8) → total
                Double cuotaPagar = getCellValueAsDouble(row.getCell(8));
                detalle.setTotal(cuotaPagar != null ? cuotaPagar : 0.0);
                
                // Calcular cuota como capital + interes
                double cuota = (pagoCapital != null ? pagoCapital : 0.0) + 
                              (valorInteres != null ? valorInteres : 0.0);
                detalle.setCuota(redondear(cuota));
                
                // Obtener valor de primera cuota normal (mayor a 0)
                if (valorCuotaPrimera == 0.0 && numeroCuota != null && numeroCuota > 0) {
                    valorCuotaPrimera = cuota;
                }
                
                // ESTADO (columna 9) - Mapear texto a código numérico
                Cell estadoCell = row.getCell(9);
                Long estadoCodigo = mapearEstadoTextoACodigo(estadoCell);
                detalle.setEstado(estadoCodigo);
                detalle.setIdEstado(estadoCodigo);

                // Ajustar campos saldo/pagado según el estado
                detalle.setPrestamo(prestamo);
                detalle.setMora(0.0);
                detalle.setInteresVencido(0.0);
                detalle.setDesgravamenDiferido(0.0);
                detalle.setValorDiferido(0.0);

                int estadoInt = estadoCodigo.intValue();
                if (estadoInt == EstadoCuotaPrestamo.PAGADA ||
                    estadoInt == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                    // Cuota ya pagada: saldo capital se mantiene calculado (saldoInicialCapital - capital)
                    // saldoCapital y saldo ya fueron asignados arriba correctamente
                    detalle.setSaldoInteres(0.0);
                    detalle.setSaldoMora(0.0);
                    detalle.setSaldoInteresVencido(0.0);
                    detalle.setCapitalPagado(pagoCapital != null ? pagoCapital : 0.0);
                    detalle.setInteresPagado(valorInteres != null ? valorInteres : 0.0);
                    detalle.setMoraPagado(0.0);
                    detalle.setDesgravamenPagado(desgravamen != null ? desgravamen : 0.0);
                    detalle.setAbono(cuota);
                    detalle.setFechaPagado(fechaVencimiento);
                    System.out.println("Cuota " + numeroCuota + " cargada como " +
                        (estadoInt == EstadoCuotaPrestamo.PAGADA ? "PAGADA" : "CANCELADA_ANTICIPADA") +
                        " - Capital pagado: " + detalle.getCapitalPagado() +
                        ", Interés pagado: " + detalle.getInteresPagado());
                } else {
                    // Cuota pendiente/mora/parcial/vencida: saldos según lo calculado
                    detalle.setSaldoInteres(valorInteres != null ? valorInteres : 0.0);
                    detalle.setSaldoMora(0.0);
                    detalle.setSaldoInteresVencido(0.0);
                    detalle.setAbono(0.0);
                    detalle.setCapitalPagado(0.0);
                    detalle.setInteresPagado(0.0);
                    detalle.setMoraPagado(0.0);
                    detalle.setDesgravamenPagado(0.0);
                    detalle.setFechaPagado(null);
                }
                
                detalles.add(detalle);
            }
            
            System.out.println("Se cargaron " + detalles.size() + " cuotas desde el Excel");
            
        } catch (Exception e) {
            throw new IncomeException("Error al procesar el archivo Excel: " + e.getMessage());
        }
        
        if (detalles.isEmpty()) {
            throw new IncomeException("No se encontraron datos válidos en el archivo Excel");
        }
        
        // Guardar todos los detalles en la base de datos
        for (DetallePrestamo detalle : detalles) {
            detallePrestamoDaoService.save(detalle, detalle.getCodigo());
            
            // Si el estado es PAGADA o CANCELADA_ANTICIPADA, crear registro de PagoPrestamo
            if (detalle.getEstado() != null && 
                (detalle.getEstado() == EstadoCuotaPrestamo.PAGADA || 
                 detalle.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA)) {
                crearPagoPrestamo(prestamo, detalle, usuario);
            }
        }
        
        // Actualizar campos del préstamo
        prestamo.setValorCuota(redondear(valorCuotaPrimera));
        prestamo.setFechaFin(fechaFin);
        prestamo.setTotalCapital(redondear(totalCapital));
        prestamo.setTotalInteres(redondear(totalInteres));
        
        // Calcular tasas (usar la tasa que ya tiene el préstamo)
        if (prestamo.getTasa() != null) {
            double tasaAnual = prestamo.getTasa();
            prestamo.setTasaNominal(redondear(tasaAnual));
            
            double tasaMensual = tasaAnual / 100 / 12;
            double tasaEfectiva = (Math.pow(1 + tasaMensual, 12) - 1) * 100;
            prestamo.setTasaEfectiva(redondear(tasaEfectiva));
        }
        
        double totalPrestamo = totalCapital + totalInteres;
        prestamo.setTotalPrestamo(redondear(totalPrestamo));
        
        // Guardar préstamo actualizado
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        
        System.out.println("Tabla de amortización cargada exitosamente - Total Capital: " + totalCapital + 
                         ", Total Interés: " + totalInteres);
        
        return prestamo;
    }
    
    /**
     * Mapea el estado de texto del Excel a código numérico según EstadoCuotaPrestamo.
     * Estados válidos:
     *   PENDIENTE = 1, ACTIVA = 2, EMITIDA = 3, PAGADA = 4,
     *   EN_MORA = 5, PARCIAL = 6, CANCELADA_ANTICIPADA = 7, VENCIDA = 8
     *
     * @param cell Celda que contiene el estado
     * @return Código numérico del estado
     */
    private Long mapearEstadoTextoACodigo(Cell cell) {
        if (cell == null) {
            return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                // Si ya viene como número, usarlo directamente
                return (long) cell.getNumericCellValue();
            }

            if (cell.getCellType() != CellType.STRING) {
                return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

            String estadoTexto = cell.getStringCellValue().trim().toUpperCase()
                    .replace("  ", " "); // normalizar espacios dobles

            if (estadoTexto.isEmpty()) {
                return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

            switch (estadoTexto) {
                case "PENDIENTE":
                    return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);         // 1
                case "ACTIVA":
                case "ACTIVO":
                    return Long.valueOf(EstadoCuotaPrestamo.ACTIVA);            // 2
                case "EMITIDA":
                case "EMITIDO":
                    return Long.valueOf(EstadoCuotaPrestamo.EMITIDA);           // 3
                case "PAGADA":
                case "PAGADO":
                    return Long.valueOf(EstadoCuotaPrestamo.PAGADA);            // 4
                case "EN MORA":
                case "EN_MORA":
                case "MORA":
                    return Long.valueOf(EstadoCuotaPrestamo.EN_MORA);           // 5
                case "PARCIAL":
                case "PAGO PARCIAL":
                case "PAGO_PARCIAL":
                    return Long.valueOf(EstadoCuotaPrestamo.PARCIAL);           // 6
                case "CANCELADA ANTICIPADA":
                case "CANCELADA_ANTICIPADA":
                case "CANCELADO ANTICIPADO":
                case "CANCELADO_ANTICIPADO":
                case "CANCELADA":
                    return Long.valueOf(EstadoCuotaPrestamo.CANCELADA_ANTICIPADA); // 7
                case "VENCIDA":
                case "VENCIDO":
                    return Long.valueOf(EstadoCuotaPrestamo.VENCIDA);           // 8
                default:
                    System.out.println("Estado desconocido en Excel: '" + estadoTexto + 
                                       "', se asigna PENDIENTE (" + EstadoCuotaPrestamo.PENDIENTE + ")");
                    return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

        } catch (Exception e) {
            System.err.println("Error al leer estado de la celda: " + e.getMessage());
            return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
        }
    }
    
    /**
     * Crea un registro de PagoPrestamo para una cuota pagada.
     * @param prestamo Préstamo asociado
     * @param detalle Detalle de la cuota pagada
     */
    private void crearPagoPrestamo(Prestamo prestamo, DetallePrestamo detalle, String usuario) throws Throwable {
        System.out.println("Creando PagoPrestamo para cuota " + detalle.getNumeroCuota());
        
        PagoPrestamo pago = new PagoPrestamo();
        
        // Relaciones
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(detalle);
        
        // Fecha de pago = fecha de vencimiento (asumimos que se pagó exactamente en la fecha)
        pago.setFecha(detalle.getFechaVencimiento());
        
        // Valores del pago
        pago.setValor(detalle.getCuota()); // valor pagado = cuota
        pago.setNumeroCuota(detalle.getNumeroCuota());
        pago.setCapitalPagado(detalle.getCapital());
        pago.setInteresPagado(detalle.getInteres());
        pago.setMoraPagada(0.0); // No hay mora
        pago.setInteresVencidoPagado(0.0); // No hay interés vencido
        pago.setDesgravamen(detalle.getDesgravamen());
        pago.setSaldoOtros(detalle.getSaldoOtros()); // pago extra
        
        // Observación
        pago.setObservacion("Pago cargado desde Excel - Migración de datos");
        pago.setTipo("MIGRACION");
        
        // Estado
        pago.setEstado(Long.valueOf(Estado.ACTIVO));
        pago.setIdEstado(Long.valueOf(Estado.ACTIVO));
        
        // Fecha y usuario de registro
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setUsuarioRegistro(usuario);
        
        // Guardar el pago
        pagoPrestamoDaoService.save(pago, pago.getCodigo());
        
        System.out.println("PagoPrestamo creado exitosamente para cuota " + detalle.getNumeroCuota() + 
                         " - Valor: " + pago.getValor());
    }
    
    /**
     * Obtiene el valor de una celda como Double.
     */
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
    
    /**
     * Obtiene el valor de una celda como LocalDateTime.
     */
    private LocalDateTime getCellValueAsDate(Cell cell, DateTimeFormatter formatter) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    // Si es fecha de Excel
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue();
                    }
                    return null;
                case STRING:
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) return null;
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay();
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error al parsear fecha: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Valida que el idAsoprep sea único en la base de datos.
     * @param idAsoprep ID del asociado préstamo a validar
     * @param codigoPrestamo Código del préstamo actual (null si es nuevo)
     * @throws Throwable Si el idAsoprep ya existe en otro préstamo
     */
    private void validarIdAsoprepUnico(Long idAsoprep, Long codigoPrestamo) throws Throwable {
        System.out.println("Validando idAsoprep único: " + idAsoprep + " para préstamo: " + codigoPrestamo);
        
        // Buscar si existe un préstamo con el mismo idAsoprep
        Prestamo prestamoExistente = prestamoDaoService.selectByIdAsoprep(idAsoprep);
        
        // Si existe un préstamo con ese idAsoprep
        if (prestamoExistente != null) {
            // Si es un préstamo nuevo (sin código) o si el código no coincide, es un duplicado
            if (codigoPrestamo == null || !prestamoExistente.getCodigo().equals(codigoPrestamo)) {
                System.out.println("ERROR: idAsoprep " + idAsoprep + " ya existe en el préstamo con código: " + prestamoExistente.getCodigo());
                throw new IncomeException("El préstamo con idAsoprep " + idAsoprep + " ya existe en el sistema (Código: " + prestamoExistente.getCodigo() + "). No se puede duplicar.");
            }
        }
        
        System.out.println("Validación exitosa: idAsoprep " + idAsoprep + " es único");
    }

	@Override
	public Prestamo actualizarCamposDesdeTabla(Long idPrestamo) throws Throwable {
		System.out.println("Recalculando campos derivados del préstamo desde su tabla viva: " + idPrestamo);

		Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
		if (prestamo == null) {
			throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
		}

		List<DetallePrestamo> detalles = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
		// N2 aplica también acá: esta recalculación es la misma que reutilizan el abono a
		// capital y su reverso sobre la tabla ya persistida (ver javadoc de la interfaz).
		long tipoAmortizacion = prestamo.getTipoAmortizacion() != null ? prestamo.getTipoAmortizacion() : 1L;
		actualizarCamposPrestamo(prestamo, detalles, tipoAmortizacion);
		return prestamoDaoService.save(prestamo, prestamo.getCodigo());
	}

	@Override
	public java.util.List<Prestamo> selectByRangoFechas(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return prestamoDaoService.selectByRangoFechas(fechaInicio, fechaFin);
	}

	@Override
	public java.util.List<Prestamo> selectByEstado(Long estado) throws Throwable {
		return prestamoDaoService.selectByEstado(estado);
	}

	@Override
	public long countVigentesMoraVencidosByEntidad(Long codigoEntidad) throws Throwable {
		return prestamoDaoService.countVigentesMoraVencidosByEntidad(codigoEntidad);
	}

	@Override
	public long countPrestamosConUltimaCuotaEnPeriodoByEntidad(Long codigoEntidad,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return prestamoDaoService.countPrestamosConUltimaCuotaEnPeriodoByEntidad(codigoEntidad, fechaInicio, fechaFin);
	}

	@Override
	public List<SaldoPrestamoResumen> calcularSaldosEnLote(List<Long> codigosPrestamo) throws Throwable {
		System.out.println("Ingresa al metodo calcularSaldosEnLote de PrestamoService con "
				+ (codigosPrestamo != null ? codigosPrestamo.size() : 0) + " codigos");

		if (codigosPrestamo == null || codigosPrestamo.isEmpty()) {
			throw new IncomeException("Se requiere al menos un código de préstamo");
		}
		if (codigosPrestamo.size() > 500) {
			throw new IncomeException("Máximo 500 préstamos por llamada");
		}

		// EN LOTE, no préstamo por préstamo: MotorPagoPrestamoServiceImpl.calcularSaldosCuota
		// de UN argumento (línea ~116) consulta los pagos vigentes POR CUOTA. Sobre una página
		// de 100 préstamos eso eran miles de consultas donde antes había cien. Acá se trae TODO
		// con 5 llamadas (fragmentadas por dentro): préstamos existentes + su idEstado, cuotas
		// pendientes, pagos de esas cuotas, capital de cuotas por préstamo (liquidado y total,
		// agrupados en una sola consulta), y capital abonado en mora/parcial agrupado. Se
		// calcula en memoria con la variante PURA de 2 argumentos, sobre objetos TRANSITORIOS
		// (new + setters, nunca gestionados por el EntityManager). NO volver a un bucle de una
		// consulta por préstamo NI a leer las cuotas/pagos como entidades: DetallePrestamo
		// .prestamo y los @ManyToOne de PagoPrestamo son EAGER (default de JPA) y arrastran en
		// cascada Entidad, Producto, Filial, MotivoPrestamo, etc. — Hibernate hidrataba el
		// grafo completo de cada fila. Las consultas de abajo son proyecciones ESCALARES
		// (Object[]) a propósito.
		List<Object[]> filasExistentes = prestamoDaoService.selectCodigosYEstadoExistentes(codigosPrestamo);
		Set<Long> existentesSet = new HashSet<>();
		Map<Long, Long> estadoPrestamoPorId = new LinkedHashMap<>();
		List<Long> existentes = new ArrayList<>();
		for (Object[] fila : filasExistentes) {
			Long idP = fila[0] != null ? ((Number) fila[0]).longValue() : null;
			Long idEstadoP = fila[1] != null ? ((Number) fila[1]).longValue() : null;
			existentesSet.add(idP);
			existentes.add(idP);
			estadoPrestamoPorId.put(idP, idEstadoP);
		}

		// Cuotas PENDIENTES escalares → saldoCapital/saldoTotal. d.prestamo.codigo en JPQL usa
		// la FK directa, sin join ni hidratación.
		List<Object[]> filasCuotas = detallePrestamoDaoService.selectDatosSaldoCuotasPendientes(existentes);
		Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = new LinkedHashMap<>();
		List<Long> codigosCuotaPendiente = new ArrayList<>();
		for (Object[] fila : filasCuotas) {
			Long idPrestamoDeCuota = fila[0] != null ? ((Number) fila[0]).longValue() : null;
			Long idCuota = fila[1] != null ? ((Number) fila[1]).longValue() : null;

			// Objeto TRANSITORIO: new + setters de SOLO lo que trajo la proyección. NUNCA se
			// setea prestamo (queda null; el motor no lo usa) ni se persiste.
			DetallePrestamo cuota = new DetallePrestamo();
			cuota.setCodigo(idCuota);
			cuota.setDesgravamen(fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0);
			cuota.setMora(fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0);
			cuota.setInteresVencido(fila[4] != null ? ((Number) fila[4]).doubleValue() : 0.0);
			cuota.setInteres(fila[5] != null ? ((Number) fila[5]).doubleValue() : 0.0);
			cuota.setCapital(fila[6] != null ? ((Number) fila[6]).doubleValue() : 0.0);
			cuota.setValorSeguroIncendio(fila[7] != null ? ((Number) fila[7]).doubleValue() : 0.0);
			// ⚠️ total (DTPRTTLL) NO se defaultea a 0.0: el motor decide con
			// "cuota.getTotal() != null" si usa DTPRTTLL o el fallback de la suma de los 6
			// componentes (cuota legacy sin DTPRTTLL). Convertir null a 0.0 acá cambiaría esa
			// rama y el resultado, no solo la representación.
			cuota.setTotal(fila[8] != null ? ((Number) fila[8]).doubleValue() : null);
			cuota.setFechaVencimiento((LocalDateTime) fila[9]);

			cuotasPorPrestamo.computeIfAbsent(idPrestamoDeCuota, k -> new ArrayList<>()).add(cuota);
			codigosCuotaPendiente.add(idCuota);
		}

		// Pagos vigentes escalares de las cuotas PENDIENTES → alimentan el motor para
		// saldoCapital/saldoTotal por cuota. NO es el universo de capitalPagado (ver abajo):
		// ese es el acumulado histórico de TODAS las cuotas, pagadas o no.
		List<Object[]> filasPagos = pagoPrestamoDaoService.selectDatosPagosVigentes(codigosCuotaPendiente);
		Map<Long, List<PagoPrestamo>> pagosPorCuota = new LinkedHashMap<>();
		for (Object[] fila : filasPagos) {
			Long idCuotaDelPago = fila[0] != null ? ((Number) fila[0]).longValue() : null;

			// Objeto TRANSITORIO: solo los seis componentes que el motor acumula en su bucle.
			PagoPrestamo pago = new PagoPrestamo();
			pago.setDesgravamen(fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0);
			pago.setMoraPagada(fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0);
			pago.setInteresVencidoPagado(fila[3] != null ? ((Number) fila[3]).doubleValue() : 0.0);
			pago.setInteresPagado(fila[4] != null ? ((Number) fila[4]).doubleValue() : 0.0);
			pago.setCapitalPagado(fila[5] != null ? ((Number) fila[5]).doubleValue() : 0.0);
			pago.setValorSeguroIncendio(fila[6] != null ? ((Number) fila[6]).doubleValue() : 0.0);
			pagosPorCuota.computeIfAbsent(idCuotaDelPago, k -> new ArrayList<>()).add(pago);
		}

		// capitalPagado — regla en TRES niveles (usuario, 2026-09-10, API-SALDOS-PRESTAMO.md
		// §3bis), el primero manda sobre los otros dos:
		//   0) Préstamo en estado TERMINAL (CANCELADO 3, CANCELADO_ANTICIPADO 4,
		//      CANCELADO_POR_NOVACION 5 — los mismos tres que
		//      MotorPagoPrestamoServiceImpl.esEstadoTerminalPrestamo, método privado del Impl,
		//      por eso se repiten las tres constantes acá en vez de llamarlo) → TODO su capital
		//      se da por pagado, sin mirar el estado de cada cuota (hay cuotas mal marcadas en
		//      préstamos precancelados, defecto P21). Un préstamo cancelado tiene saldo 0; si
		//      capitalPagado no fuera el total, la fila mostraría Monto $10.000, Capital
		//      Pagado $0 y Saldo $0 — no cierra. Con esto, Monto − Capital Pagado = Saldo
		//      Capital se sostiene también en los cancelados.
		//   Para cualquier otro préstamo, por estado de CUOTA:
		//   1) LIQUIDADA (PAGADA o CANCELADA_ANTICIPADA) → aporta d.capital (DTPRCPTL): la
		//      migración no siempre dejó completo el registro de pago de una cuota ya
		//      liquidada, así que PGPR no es confiable ahí.
		//   2) EN_MORA o PARCIAL → aporta SUM(PGPRCPPG) de sus pagos vigentes.
		//   3) PENDIENTE (o cualquier otro estado, incluido NULL) → aporta 0, AUNQUE tenga
		//      pagos vigentes registrados (para PENDIENTE se asume que el capital no se pagó).
		// Los datos de cuotas y de pagos son agregados en la base (2 consultas), una fila por
		// préstamo cada una — nunca se trae una fila de pago o de cuota a Java para este
		// cálculo.
		List<Object[]> filasCapitalCuotas = detallePrestamoDaoService.selectCapitalCuotasByPrestamos(existentes);
		Map<Long, Double> capitalLiquidadoPorPrestamo = new LinkedHashMap<>();
		Map<Long, Double> capitalTotalPorPrestamo = new LinkedHashMap<>();
		for (Object[] fila : filasCapitalCuotas) {
			Long idPrestamoDeCuotas = fila[0] != null ? ((Number) fila[0]).longValue() : null;
			capitalLiquidadoPorPrestamo.put(idPrestamoDeCuotas,
					fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0);
			capitalTotalPorPrestamo.put(idPrestamoDeCuotas,
					fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0);
		}
		List<Object[]> filasCapitalAbonado =
				pagoPrestamoDaoService.selectCapitalAbonadoEnMoraOParcialByPrestamos(existentes);
		Map<Long, Double> capitalAbonadoPorPrestamo = new LinkedHashMap<>();
		for (Object[] fila : filasCapitalAbonado) {
			Long idPrestamoDelSuma = fila[0] != null ? ((Number) fila[0]).longValue() : null;
			capitalAbonadoPorPrestamo.put(idPrestamoDelSuma,
					fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0);
		}

		// Mismo corte que ProcesoMoraPrestamoServiceImpl.corteDelDia(LocalDate.now()): inicio
		// del día de hoy, hora del servidor. Una cuota que vence hoy todavía no está en mora.
		LocalDateTime corteMora = LocalDate.now().atStartOfDay();

		List<SaldoPrestamoResumen> resultado = new ArrayList<>();
		for (Long idPrestamo : codigosPrestamo) {
			// Un código que no existe en CRD.PRST no aparece en la respuesta (no es error).
			// Un préstamo EXISTENTE en estado terminal (cancelado, etc.) sí aparece, con 0 en
			// sus campos si no tiene cuotas/pagos — API-SALDOS-PRESTAMO.md §3, ejemplo del
			// préstamo 8078.
			if (!existentesSet.contains(idPrestamo)) {
				continue;
			}
			try {
				List<DetallePrestamo> cuotasPendientes =
						cuotasPorPrestamo.getOrDefault(idPrestamo, new ArrayList<>());

				double saldoCapital = 0.0;
				double saldoTotal = 0.0;
				long cuotasEnMora = 0L;
				for (DetallePrestamo cuota : cuotasPendientes) {
					List<PagoPrestamo> pagosDeCuota =
							pagosPorCuota.getOrDefault(cuota.getCodigo(), new ArrayList<>());
					// Variante PURA de 2 argumentos: no consulta, no autocorrige ni persiste
					// el estado de la cuota. Este endpoint es de solo lectura. NO copiar la
					// fórmula acá: la única fuente de la matemática es el motor.
					SaldosCuota saldos = motorPagoPrestamoService.calcularSaldosCuota(cuota, pagosDeCuota);
					saldoCapital += saldos.getSaldoCapital();
					saldoTotal += saldos.getTotalPendiente();
					if (cuota.getFechaVencimiento() != null && cuota.getFechaVencimiento().isBefore(corteMora)) {
						cuotasEnMora++;
					}
				}

				// Nivel 0 de capitalPagado: préstamo en estado TERMINAL (CANCELADO,
				// CANCELADO_ANTICIPADO o CANCELADO_POR_NOVACION) da por pagado TODO su capital
				// (capitalTotalPorPrestamo), sin mirar el estado de cada cuota. Para cualquier
				// otro préstamo: liquidado (nivel 1) + abonado en mora/parcial (nivel 2);
				// PENDIENTE y el resto ya aportan 0 porque no están en ninguno de los dos
				// mapas.
				Long idEstadoPrestamo = estadoPrestamoPorId.get(idPrestamo);
				boolean prestamoTerminal = idEstadoPrestamo != null
						&& (idEstadoPrestamo.longValue() == EstadoPrestamo.CANCELADO
								|| idEstadoPrestamo.longValue() == EstadoPrestamo.CANCELADO_ANTICIPADO
								|| idEstadoPrestamo.longValue() == EstadoPrestamo.CANCELADO_POR_NOVACION);
				double capitalPagado = prestamoTerminal
						? capitalTotalPorPrestamo.getOrDefault(idPrestamo, 0.0)
						: capitalLiquidadoPorPrestamo.getOrDefault(idPrestamo, 0.0)
								+ capitalAbonadoPorPrestamo.getOrDefault(idPrestamo, 0.0);

				SaldoPrestamoResumen resumen = new SaldoPrestamoResumen();
				resumen.setIdPrestamo(idPrestamo);
				resumen.setSaldoCapital(redondear(saldoCapital));
				resumen.setSaldoTotal(redondear(saldoTotal));
				resumen.setCapitalPagado(redondear(capitalPagado));
				resumen.setCuotasEnMora(cuotasEnMora);
				resultado.add(resumen);
			} catch (Throwable e) {
				System.err.println("calcularSaldosEnLote - Préstamo " + idPrestamo
						+ " - error al calcular saldos: " + e.getMessage());
			}
		}

		System.out.println("calcularSaldosEnLote - solicitados: " + codigosPrestamo.size()
				+ " - calculados: " + resultado.size());
		return resultado;
	}
}
