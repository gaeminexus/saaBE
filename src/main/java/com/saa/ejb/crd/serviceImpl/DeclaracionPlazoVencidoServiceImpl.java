package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DeclaracionPlazoVencidoDaoService;
import com.saa.ejb.crd.dao.DetallePlazoVencidoDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.DeclaracionPlazoVencidoService;
import com.saa.ejb.crd.service.ProcesoMoraPrestamoService;
import com.saa.ejb.crd.service.dto.CandidatoPlazoVencido;
import com.saa.ejb.crd.service.dto.ComponenteCuadroPlazoVencido;
import com.saa.ejb.crd.service.dto.DeclaracionPlazoVencidoDTO;
import com.saa.ejb.crd.service.dto.EncabezadoPlazoVencido;
import com.saa.ejb.crd.service.dto.ItemDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.LiquidacionPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.ResultadoRevertirPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudDeclararPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudLiquidarPlazoVencido;
import com.saa.ejb.crd.service.dto.SolicitudRevertirPlazoVencido;
import com.saa.model.crd.DeclaracionPlazoVencido;
import com.saa.model.crd.DetallePlazoVencido;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.NoResultException;

/**
 * Pase de préstamos EN MORA a DE PLAZO VENCIDO. Ver
 * {@code docs/logica-negocio/crd/API-PASE-A-PLAZO-VENCIDO.md} (contrato) y
 * {@code docs/logica-negocio/crd/DISENO-PASE-A-PLAZO-VENCIDO.md} (decisiones D1-D23).
 */
@Stateless
public class DeclaracionPlazoVencidoServiceImpl implements DeclaracionPlazoVencidoService {

    /** Tolerancia de cuadre entre devengado/cobrado/saldo, en dólares. */
    private static final double TOLERANCIA = 0.01;

    /**
     * D26 (usuario, 2026-09-30, contrato §5, commit 08ae2d44): en pantalla se escribe SOLO el
     * número del memorando (p.ej. «46») y el sistema lo compone con este prefijo —
     * "ASOPREP-FCPC-CREDITO-GR-046-2026". A partir de acá TODO trabaja con el número compuesto:
     * la unicidad (lote y base), lo que se graba en PLVNNMMM, los mensajes de error y las
     * respuestas. Las declaraciones viejas con el número pelado las corrige {@code sql/300}.
     */
    private static final String PREFIJO_MEMORANDO = "ASOPREP-FCPC-CREDITO-GR-";

    @EJB
    private DeclaracionPlazoVencidoDaoService declaracionDaoService;

    @EJB
    private DetallePlazoVencidoDaoService detallePlazoVencidoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private ProcesoMoraPrestamoService procesoMoraPrestamoService;

    // ========================================================================
    // CRUD genérico (EntityService<DeclaracionPlazoVencido>)
    // ========================================================================

    @Override
    public DeclaracionPlazoVencido selectById(Long id) throws Throwable {
        System.out.println("selectById - DeclaracionPlazoVencido: " + id);
        return declaracionDaoService.selectById(id, NombreEntidadesCredito.DECLARACION_PLAZO_VENCIDO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - DeclaracionPlazoVencido");
        DeclaracionPlazoVencido entidad = new DeclaracionPlazoVencido();
        for (Long registro : id) {
            declaracionDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<DeclaracionPlazoVencido> lista) throws Throwable {
        System.out.println("save list - DeclaracionPlazoVencido");
        for (DeclaracionPlazoVencido registro : lista) {
            declaracionDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public DeclaracionPlazoVencido saveSingle(DeclaracionPlazoVencido registro) throws Throwable {
        System.out.println("saveSingle - DeclaracionPlazoVencido");
        return declaracionDaoService.save(registro, registro.getCodigo());
    }

    @Override
    public List<DeclaracionPlazoVencido> selectAll() throws Throwable {
        System.out.println("selectAll - DeclaracionPlazoVencido");
        return declaracionDaoService.selectAll(NombreEntidadesCredito.DECLARACION_PLAZO_VENCIDO);
    }

    @Override
    public List<DeclaracionPlazoVencido> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - DeclaracionPlazoVencido");
        return declaracionDaoService.selectByCriteria(datos, NombreEntidadesCredito.DECLARACION_PLAZO_VENCIDO);
    }

    // ========================================================================
    // GET /candidatos, GET /ultimoEncabezado
    // ========================================================================

    @Override
    public List<CandidatoPlazoVencido> obtenerCandidatos(LocalDate fechaCorte) throws Throwable {
        System.out.println("obtenerCandidatos - DeclaracionPlazoVencido - fechaCorte: " + fechaCorte);

        validarFechaCorte(fechaCorte);

        List<Prestamo> prestamos = prestamoDaoService.selectByEstado((long) EstadoPrestamo.EN_MORA);
        if (prestamos == null || prestamos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsPrestamo = new ArrayList<>();
        for (Prestamo p : prestamos) {
            idsPrestamo.add(p.getCodigo());
        }

        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(idsPrestamo);
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(cuotasPorPrestamo);
        Map<Long, LocalDateTime> maxFechaPagoPorPrestamo = cargarMaxFechaPagoPorPrestamo(idsPrestamo);

        List<CandidatoPlazoVencido> candidatos = new ArrayList<>();
        for (Prestamo prestamo : prestamos) {
            List<DetallePrestamo> universo = universoCuotas(cuotasPorPrestamo.get(prestamo.getCodigo()));
            candidatos.add(construirCuadro(prestamo, universo, pagosPorCuota,
                maxFechaPagoPorPrestamo.get(prestamo.getCodigo()), fechaCorte));
        }
        System.out.println("  Candidatos calculados: " + candidatos.size());
        return candidatos;
    }

    @Override
    public EncabezadoPlazoVencido obtenerUltimoEncabezado() throws Throwable {
        System.out.println("obtenerUltimoEncabezado - DeclaracionPlazoVencido");
        DeclaracionPlazoVencido ultima = declaracionDaoService.selectUltima();
        EncabezadoPlazoVencido encabezado = new EncabezadoPlazoVencido();
        if (ultima != null) {
            encabezado.setParaNombre(ultima.getParaNombre());
            encabezado.setParaCargo(ultima.getParaCargo());
            encabezado.setCcNombre(ultima.getCcNombre());
            encabezado.setCcCargo(ultima.getCcCargo());
        }
        return encabezado;
    }

    // ========================================================================
    // POST /declarar
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<ResultadoDeclararPlazoVencido> declarar(SolicitudDeclararPlazoVencido solicitud) throws Throwable {
        System.out.println("declarar - DeclaracionPlazoVencido - fechaCorte: "
            + (solicitud != null ? solicitud.getFechaCorte() : null));

        // 1. Validaciones de forma
        if (solicitud == null || solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()
                || solicitud.getPrestamos() == null || solicitud.getPrestamos().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": fechaCorte, usuario y prestamos son obligatorios, y prestamos no puede estar vacío");
        }
        validarFechaCorte(solicitud.getFechaCorte());

        // 2. numeroMemorando: obligatorio, SOLO DÍGITOS (D26 — en pantalla se escribe solo el
        // número, p.ej. «46», y el sistema lo compone), sin duplicados en el lote ni en CRD.PLVN
        Set<String> memorandosNormalizados = new HashSet<>();
        for (ItemDeclararPlazoVencido item : solicitud.getPrestamos()) {
            if (item.getIdPrestamo() == null || item.getNumeroMemorando() == null
                    || item.getNumeroMemorando().trim().isEmpty()) {
                throw new IncomeException(ERR_PARAMETRO_INVALIDO
                    + ": idPrestamo y numeroMemorando son obligatorios en cada préstamo del lote");
            }
            item.setNumeroMemorando(componerNumeroMemorando(item.getNumeroMemorando()));
            String normalizado = item.getNumeroMemorando().trim().toUpperCase();
            if (!memorandosNormalizados.add(normalizado)) {
                throw new IncomeException(ERR_MEMORANDO_DUPLICADO
                    + ": el número de memorando '" + item.getNumeroMemorando() + "' está repetido dentro del lote");
            }
            DeclaracionPlazoVencido existente = declaracionDaoService.selectByNumeroMemorandoNormalizado(normalizado);
            if (existente != null) {
                throw new IncomeException(ERR_MEMORANDO_DUPLICADO
                    + ": el número de memorando '" + item.getNumeroMemorando()
                    + "' ya existe (préstamo " + (existente.getPrestamo() != null ? existente.getPrestamo().getCodigo() : null) + ")");
            }
        }

        // 3. idPrestamo repetido en el lote
        Set<Long> idsVistos = new HashSet<>();
        List<Long> idsPrestamo = new ArrayList<>();
        for (ItemDeclararPlazoVencido item : solicitud.getPrestamos()) {
            if (!idsVistos.add(item.getIdPrestamo())) {
                throw new IncomeException(ERR_PARAMETRO_INVALIDO
                    + ": el préstamo " + item.getIdPrestamo() + " está repetido en el lote");
            }
            idsPrestamo.add(item.getIdPrestamo());
        }

        // 4. Cada préstamo tiene que estar en 11 EN_MORA al confirmar
        Map<Long, Prestamo> prestamosPorId = new LinkedHashMap<>();
        StringBuilder noEnMora = new StringBuilder();
        for (Long idPrestamo : idsPrestamo) {
            Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
            if (prestamo == null || prestamo.getIdEstado() == null
                    || prestamo.getIdEstado().intValue() != EstadoPrestamo.EN_MORA) {
                noEnMora.append(noEnMora.length() > 0 ? ", " : "")
                    .append("préstamo ").append(idPrestamo).append(" (estado actual: ")
                    .append(prestamo != null ? prestamo.getIdEstado() : "no encontrado").append(")");
            } else {
                prestamosPorId.put(idPrestamo, prestamo);
            }
        }
        if (noEnMora.length() > 0) {
            throw new IncomeException(ERR_PRESTAMO_NO_EN_MORA + ": " + noEnMora);
        }

        // 5. Recalcular el cuadro de cada préstamo y validar las cinco invariantes
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(idsPrestamo);
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(cuotasPorPrestamo);
        Map<Long, LocalDateTime> maxFechaPagoPorPrestamo = cargarMaxFechaPagoPorPrestamo(idsPrestamo);

        Map<Long, CandidatoPlazoVencido> cuadrosPorPrestamo = new LinkedHashMap<>();
        for (ItemDeclararPlazoVencido item : solicitud.getPrestamos()) {
            Prestamo prestamo = prestamosPorId.get(item.getIdPrestamo());
            List<DetallePrestamo> universo = universoCuotas(cuotasPorPrestamo.get(prestamo.getCodigo()));
            CandidatoPlazoVencido cuadro = construirCuadro(prestamo, universo, pagosPorCuota,
                maxFechaPagoPorPrestamo.get(prestamo.getCodigo()), solicitud.getFechaCorte());
            if (!cuadro.isValido()) {
                throw new IncomeException(ERR_CALCULO_NO_CUADRA + ": préstamo " + prestamo.getCodigo()
                    + " - " + String.join(" | ", cuadro.getInconsistencias()));
            }
            cuadrosPorPrestamo.put(prestamo.getCodigo(), cuadro);
        }

        // 6. Grabar, todo o nada, en esta misma transacción (sin REQUIRES_NEW en el bucle)
        List<ResultadoDeclararPlazoVencido> resultados = new ArrayList<>();
        for (ItemDeclararPlazoVencido item : solicitud.getPrestamos()) {
            Prestamo prestamo = prestamosPorId.get(item.getIdPrestamo());
            CandidatoPlazoVencido cuadro = cuadrosPorPrestamo.get(prestamo.getCodigo());
            List<DetallePrestamo> universo = universoCuotas(cuotasPorPrestamo.get(prestamo.getCodigo()));

            DeclaracionPlazoVencido declaracion = nuevaDeclaracionDesdeCuadro(prestamo, cuadro, solicitud, item);
            declaracion = declaracionDaoService.save(declaracion, null);

            long cuotasSinSeguro = anularSeguroCuotasFuturas(declaracion, universo, pagosPorCuota, solicitud.getFechaCorte());

            prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.DE_PLAZO_VENCIDO));
            prestamo.setFechaModificacion(LocalDateTime.now());
            prestamoDaoService.save(prestamo, prestamo.getCodigo());

            ResultadoDeclararPlazoVencido resultado = new ResultadoDeclararPlazoVencido();
            resultado.setIdDeclaracion(declaracion.getCodigo());
            resultado.setIdPrestamo(prestamo.getCodigo());
            resultado.setNumeroMemorando(declaracion.getNumeroMemorando());
            resultado.setTotalPorCobrar(declaracion.getTotalPorCobrar());
            resultado.setCuotasSinSeguro(cuotasSinSeguro);
            resultados.add(resultado);

            System.out.println("  Préstamo " + prestamo.getCodigo() + " declarado en plazo vencido - declaración "
                + declaracion.getCodigo() + " - memorando " + declaracion.getNumeroMemorando());
        }

        return resultados;
    }

    /**
     * Para cada cuota con {@code fechaVencimiento > fechaCorte} que no esté PAGADA ni
     * CANCELADA_ANTICIPADA y tenga desgravamen o incendio mayor a cero (§5 punto 2 del
     * contrato): guarda el original en DPLV y pone el seguro en cero — salvo que la cuota ya
     * tenga seguro PAGADO por adelantado, caso en el que no se baja de lo pagado.
     *
     * @return cuántas cuotas quedaron efectivamente sin seguro (con alguna reducción real)
     */
    private long anularSeguroCuotasFuturas(DeclaracionPlazoVencido declaracion, List<DetallePrestamo> universo,
            Map<Long, double[]> pagosPorCuota, LocalDate corte) throws Throwable {

        long cuotasSinSeguro = 0;
        for (DetallePrestamo cuota : universo) {
            if (cuota.getFechaVencimiento() == null
                    || !cuota.getFechaVencimiento().toLocalDate().isAfter(corte)) {
                continue;
            }
            if (esPagadaOCancelada(cuota)) {
                continue;
            }
            double desgravamenOriginal = nvl(cuota.getDesgravamen());
            double seguroOriginal = nvl(cuota.getValorSeguroIncendio());
            if (desgravamenOriginal <= 0.0 && seguroOriginal <= 0.0) {
                continue;
            }

            double[] pagos = pagosPorCuota.get(cuota.getCodigo());
            double desgravamenPagado = pagos != null ? Math.max(0.0, pagos[0]) : 0.0;
            double seguroPagado = pagos != null ? Math.max(0.0, pagos[5]) : 0.0;

            // No se baja por debajo de lo ya pagado (adelanto sobre una cuota futura).
            double nuevoDesgravamen = Math.min(desgravamenOriginal, desgravamenPagado);
            double nuevoSeguro = Math.min(seguroOriginal, seguroPagado);

            double diferenciaDesgravamen = redondear(desgravamenOriginal - nuevoDesgravamen);
            double diferenciaSeguro = redondear(seguroOriginal - nuevoSeguro);

            DetallePlazoVencido dplv = new DetallePlazoVencido();
            dplv.setDeclaracion(declaracion);
            dplv.setIdCuota(cuota.getCodigo());
            dplv.setDesgravamenOriginal(desgravamenOriginal);
            dplv.setValorSeguroIncendioOriginal(seguroOriginal);
            dplv.setTotalOriginal(cuota.getTotal());
            dplv.setTotalConSeguroOriginal(cuota.getTotalConSeguro());
            detallePlazoVencidoDaoService.save(dplv, null);

            if (diferenciaDesgravamen > 0.0 || diferenciaSeguro > 0.0) {
                cuota.setDesgravamen(nuevoDesgravamen);
                cuota.setValorSeguroIncendio(nuevoSeguro);
                cuota.setTotal(redondear(nvl(cuota.getTotal()) - diferenciaDesgravamen - diferenciaSeguro));
                cuota.setTotalConSeguro(redondear(nvl(cuota.getTotalConSeguro()) - diferenciaDesgravamen - diferenciaSeguro));
                detallePrestamoDaoService.save(cuota, cuota.getCodigo());
                cuotasSinSeguro++;
            }
        }
        return cuotasSinSeguro;
    }

    private DeclaracionPlazoVencido nuevaDeclaracionDesdeCuadro(Prestamo prestamo, CandidatoPlazoVencido cuadro,
            SolicitudDeclararPlazoVencido solicitud, ItemDeclararPlazoVencido item) {

        DeclaracionPlazoVencido d = new DeclaracionPlazoVencido();
        d.setPrestamo(prestamo);
        d.setEstado(DeclaracionPlazoVencido.ESTADO_DECLARADA);
        d.setEstadoAnterior(prestamo.getIdEstado());
        d.setNumeroMemorando(item.getNumeroMemorando());
        d.setFechaCorte(solicitud.getFechaCorte());
        d.setParaNombre(solicitud.getParaNombre());
        d.setParaCargo(solicitud.getParaCargo());
        d.setCcNombre(solicitud.getCcNombre());
        d.setCcCargo(solicitud.getCcCargo());
        d.setNumeroPrestamoImpreso(cuadro.getNumeroPrestamo());
        d.setNombreParticipe(cuadro.getNombreParticipe());
        d.setCedula(cuadro.getCedula());
        d.setTipoCredito(cuadro.getTipoCredito());
        d.setFechaInicial(cuadro.getFechaInicio());
        d.setFechaFinal(cuadro.getFechaFin());
        d.setFechaUltimoCobro(cuadro.getFechaUltimoCobro());
        d.setFechaInicioMora(cuadro.getFechaInicioMora());
        d.setMonto(cuadro.getMontoPrestamo());
        d.setCapitalCobrado(cuadro.getCapital().getCobrado());
        d.setSaldoCapital(cuadro.getCapital().getSaldo());
        d.setInteresDevengado(cuadro.getInteres().getDevengado());
        d.setInteresCobrado(cuadro.getInteres().getCobrado());
        d.setSaldoInteres(cuadro.getInteres().getSaldo());
        d.setDesgravamenDevengado(cuadro.getDesgravamen().getDevengado());
        d.setDesgravamenCobrado(cuadro.getDesgravamen().getCobrado());
        d.setSaldoDesgravamen(cuadro.getDesgravamen().getSaldo());
        d.setSeguroDevengado(cuadro.getSeguroIncendio().getDevengado());
        d.setSeguroCobrado(cuadro.getSeguroIncendio().getCobrado());
        d.setSaldoSeguro(cuadro.getSeguroIncendio().getSaldo());
        d.setMoraDevengada(cuadro.getMora().getDevengado());
        d.setMoraCobrada(cuadro.getMora().getCobrado());
        d.setSaldoMora(cuadro.getMora().getSaldo());
        d.setTotalCobrado(cuadro.getTotalCobrado());
        d.setTotalPorCobrar(cuadro.getTotalPorCobrar());
        d.setDividendoMensual(cuadro.getDividendoMensual());
        d.setCuotasPlazo(cuadro.getCuotasPlazo());
        d.setCuotasCobradas(cuadro.getCuotasCobradas());
        d.setCuotasPendientes(cuadro.getCuotasPendientes());
        d.setCuotasPorVencer(cuadro.getCuotasPorVencer());
        d.setUsuarioDeclaracion(solicitud.getUsuario());
        d.setFechaDeclaracion(LocalDateTime.now());
        return d;
    }

    // ========================================================================
    // POST /{id}/liquidar
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DeclaracionPlazoVencidoDTO liquidar(Long idDeclaracion, SolicitudLiquidarPlazoVencido solicitud) throws Throwable {
        System.out.println("liquidar - DeclaracionPlazoVencido - idDeclaracion: " + idDeclaracion);

        DeclaracionPlazoVencido declaracion = buscarDeclaracion(idDeclaracion);
        validarNoLiquidadaNiRevertidaParaLiquidar(declaracion);

        if (solicitud == null || solicitud.getFechaCorte() == null || solicitud.getUsuario() == null
                || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": fechaCorte y usuario son obligatorios");
        }
        LocalDate fechaCorte = solicitud.getFechaCorte();
        if (fechaCorte.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": la fecha de corte " + fechaCorte + " es futura");
        }
        if (fechaCorte.isBefore(declaracion.getFechaCorte())) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": la fecha de corte " + fechaCorte
                + " es anterior a la del memorando (" + declaracion.getFechaCorte() + ")");
        }

        Long idPrestamo = declaracion.getPrestamo().getCodigo();
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
        List<DetallePrestamo> universo = universoCuotas(cuotas);
        List<Long> idsCuotas = new ArrayList<>();
        for (DetallePrestamo cuota : universo) {
            idsCuotas.add(cuota.getCodigo());
        }
        Map<Long, double[]> pagosPorCuota = agruparPagosPorCuota(pagoPrestamoDaoService.selectDatosPagosVigentes(idsCuotas));
        Map<Long, LocalDateTime> maxFechaPagoPorPrestamo = cargarMaxFechaPagoPorPrestamo(List.of(idPrestamo));

        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        CandidatoPlazoVencido cuadro = construirCuadro(prestamo, universo, pagosPorCuota,
            maxFechaPagoPorPrestamo.get(idPrestamo), fechaCorte);

        declaracion.setFechaCorteLiquidacion(fechaCorte);
        declaracion.setLiquidacionSaldoCapital(cuadro.getCapital().getSaldo());
        declaracion.setLiquidacionInteres(cuadro.getInteres().getSaldo());
        declaracion.setLiquidacionDesgravamen(cuadro.getDesgravamen().getSaldo());
        declaracion.setLiquidacionSeguro(cuadro.getSeguroIncendio().getSaldo());
        declaracion.setLiquidacionMora(cuadro.getMora().getSaldo());
        declaracion.setLiquidacionTotal(cuadro.getTotalPorCobrar());
        declaracion.setLiquidacionCuotasImpagas(cuadro.getCuotasPendientes());
        declaracion.setUsuarioLiquidacion(solicitud.getUsuario());
        declaracion.setFechaLiquidacion(LocalDateTime.now());
        declaracion.setEstado(DeclaracionPlazoVencido.ESTADO_LIQUIDADA);

        declaracion = declaracionDaoService.save(declaracion, declaracion.getCodigo());
        System.out.println("  Declaración " + idDeclaracion + " liquidada - total adeudado: "
            + declaracion.getLiquidacionTotal());

        return mapearDTO(declaracion);
    }

    // ========================================================================
    // POST /{id}/revertir
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoRevertirPlazoVencido revertir(Long idDeclaracion, SolicitudRevertirPlazoVencido solicitud) throws Throwable {
        System.out.println("revertir - DeclaracionPlazoVencido - idDeclaracion: " + idDeclaracion);

        DeclaracionPlazoVencido declaracion = buscarDeclaracion(idDeclaracion);
        if (declaracion.getEstado() != null && declaracion.getEstado() == DeclaracionPlazoVencido.ESTADO_REVERTIDA) {
            throw new IncomeException(ERR_DECLARACION_REVERTIDA + ": la declaración " + idDeclaracion + " ya está revertida");
        }
        if (solicitud == null || solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()
                || solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario y motivo son obligatorios");
        }

        Prestamo prestamo = declaracion.getPrestamo();
        if (prestamo == null || prestamo.getIdEstado() == null
                || prestamo.getIdEstado().intValue() != EstadoPrestamo.DE_PLAZO_VENCIDO) {
            throw new IncomeException(ERR_PRESTAMO_NO_EN_PLAZO_VENCIDO
                + ": el préstamo " + (prestamo != null ? prestamo.getCodigo() : null)
                + " ya no está en plazo vencido (estado actual: " + (prestamo != null ? prestamo.getIdEstado() : null) + ")");
        }

        long restituidas = 0;
        long noRestituidas = 0;
        List<DetallePlazoVencido> anuladas = detallePlazoVencidoDaoService.selectByDeclaracion(idDeclaracion);
        for (DetallePlazoVencido dplv : anuladas) {
            if (dplv.getFechaRestitucion() != null) {
                continue;
            }
            // H82 (2026-10-02): dplv.idCuota es un Long plano, SIN relación — la cuota puede
            // haberse borrado desde que se declaró el plazo vencido (un abono a capital o un
            // reverso de operación la borran de DTPR). Buscarla aparte y tolerar que no exista:
            // antes era un @ManyToOne EAGER, y cargar ESTE MISMO 'anuladas' de selectByDeclaracion
            // ya reventaba con EntityNotFoundException apenas Hibernate intentaba hidratarlo.
            DetallePrestamo cuota = dplv.getIdCuota() != null
                ? detallePrestamoDaoService.find(new DetallePrestamo(), dplv.getIdCuota()) : null;
            if (cuota == null || esPagadaOCancelada(cuota)) {
                noRestituidas++;
                continue;
            }
            // Se restituye por DIFERENCIA, NUNCA por el valor absoluto de DPLVTTLL/DPLVTTCS.
            // Mientras el préstamo estuvo en 8, el proceso nocturno de mora (ítem 5, D6/D10)
            // pudo haberle sumado mora a esta cuota dentro de su `total` si venció y quedó
            // impaga (ProcesoMoraPrestamoServiceImpl: totalNuevo = totalBase + moraNueva).
            // Sobrescribir con el absoluto guardado al declarar BORRARÍA esa mora ya acumulada,
            // y la corrida siguiente calcularía totalBase = total − moraAnterior sobre un total
            // que ya no la tiene: la cuota queda corrida hacia abajo para siempre. DPLVTTLL y
            // DPLVTTCS quedan como rastro de auditoría de lo que se anuló, no como fuente de
            // restitución.
            double desgravamenActual = nvl(cuota.getDesgravamen());
            double seguroActual = nvl(cuota.getValorSeguroIncendio());
            double delta = redondear((nvl(dplv.getDesgravamenOriginal()) - desgravamenActual)
                + (nvl(dplv.getValorSeguroIncendioOriginal()) - seguroActual));

            cuota.setDesgravamen(dplv.getDesgravamenOriginal());
            cuota.setValorSeguroIncendio(dplv.getValorSeguroIncendioOriginal());
            cuota.setTotal(redondear(nvl(cuota.getTotal()) + delta));
            cuota.setTotalConSeguro(redondear(nvl(cuota.getTotalConSeguro()) + delta));
            detallePrestamoDaoService.save(cuota, cuota.getCodigo());

            dplv.setFechaRestitucion(LocalDateTime.now());
            detallePlazoVencidoDaoService.save(dplv, dplv.getCodigo());
            restituidas++;
        }

        prestamo.setIdEstado(Long.valueOf(EstadoPrestamo.EN_MORA));
        prestamo.setFechaModificacion(LocalDateTime.now());
        prestamoDaoService.save(prestamo, prestamo.getCodigo());

        declaracion.setEstado(DeclaracionPlazoVencido.ESTADO_REVERTIDA);
        declaracion.setUsuarioReverso(solicitud.getUsuario());
        declaracion.setFechaReverso(LocalDateTime.now());
        declaracion.setMotivoReverso(solicitud.getMotivo());
        declaracionDaoService.save(declaracion, declaracion.getCodigo());

        System.out.println("  Declaración " + idDeclaracion + " revertida - préstamo " + prestamo.getCodigo()
            + " → 11 (EN_MORA) - cuotas restituidas: " + restituidas + " - no restituidas: " + noRestituidas);

        ResultadoRevertirPlazoVencido resultado = new ResultadoRevertirPlazoVencido();
        resultado.setIdDeclaracion(idDeclaracion);
        resultado.setIdPrestamo(prestamo.getCodigo());
        resultado.setCuotasRestituidas(restituidas);
        resultado.setCuotasNoRestituidas(noRestituidas);
        return resultado;
    }

    // ========================================================================
    // Consultas
    // ========================================================================

    @Override
    public List<DeclaracionPlazoVencidoDTO> listar(Long estado, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("listar - DeclaracionPlazoVencido - estado: " + estado + " - desde: " + desde + " - hasta: " + hasta);
        List<DeclaracionPlazoVencido> declaraciones = declaracionDaoService.selectByFiltros(estado, desde, hasta);
        List<DeclaracionPlazoVencidoDTO> resultado = new ArrayList<>();
        for (DeclaracionPlazoVencido d : declaraciones) {
            resultado.add(mapearDTO(d));
        }
        return resultado;
    }

    @Override
    public DeclaracionPlazoVencidoDTO obtenerPorId(Long idDeclaracion) throws Throwable {
        System.out.println("obtenerPorId - DeclaracionPlazoVencido - idDeclaracion: " + idDeclaracion);
        return mapearDTO(buscarDeclaracion(idDeclaracion));
    }

    // ========================================================================
    // Helpers — cálculo del cuadro (contrato §2, §3 · diseño §4.4bis)
    // ========================================================================

    /**
     * Universo del cuadro (diseño §4.4bis): TODAS las cuotas del préstamo, excluidas las
     * CANCELADA_ANTICIPADA (7). {@code null} se trata como lista vacía.
     */
    private List<DetallePrestamo> universoCuotas(List<DetallePrestamo> cuotasDelPrestamo) {
        List<DetallePrestamo> universo = new ArrayList<>();
        if (cuotasDelPrestamo == null) {
            return universo;
        }
        for (DetallePrestamo cuota : cuotasDelPrestamo) {
            // DTPRESTD (estado) es la columna vigente, NUNCA DTPRIDST (CLAUDE.md, "qué columna
            // lleva realmente el estado").
            if (cuota.getEstado() != null && cuota.getEstado().intValue() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                continue;
            }
            universo.add(cuota);
        }
        return universo;
    }

    private boolean esPagadaOCancelada(DetallePrestamo cuota) {
        Long estado = cuota.getEstado();
        return estado != null && (estado.intValue() == EstadoCuotaPrestamo.PAGADA
            || estado.intValue() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
    }

    private boolean esPagada(DetallePrestamo cuota) {
        return cuota.getEstado() != null && cuota.getEstado().intValue() == EstadoCuotaPrestamo.PAGADA;
    }

    /**
     * Construye el cuadro completo de un préstamo a la fecha de corte, con sus cinco
     * invariantes verificadas (contrato §2/§3, diseño §4.4bis). No graba nada.
     */
    private CandidatoPlazoVencido construirCuadro(Prestamo prestamo, List<DetallePrestamo> universo,
            Map<Long, double[]> pagosPorCuota, LocalDateTime maxFechaPagoPrestamo, LocalDate corte) throws Throwable {

        CandidatoPlazoVencido c = new CandidatoPlazoVencido();
        c.setIdPrestamo(prestamo.getCodigo());
        c.setNumeroPrestamo(numeroPrestamoImpreso(prestamo));
        c.setTipoCredito(prestamo.getProducto() != null ? prestamo.getProducto().getNombre() : null);
        if (prestamo.getEntidad() != null) {
            c.setNombreParticipe(prestamo.getEntidad().getRazonSocial());
            c.setCedula(prestamo.getEntidad().getNumeroIdentificacion());
        }
        c.setFechaInicio(prestamo.getFechaInicio() != null ? prestamo.getFechaInicio().toLocalDate() : null);
        c.setFechaFin(prestamo.getFechaFin() != null ? prestamo.getFechaFin().toLocalDate() : null);
        c.setDividendoMensual(prestamo.getValorCuota());
        c.setMontoPrestamo(nvl(prestamo.getMontoSolicitado()));

        double tasaDiaria = procesoMoraPrestamoService.tasaDiariaDelPrestamo(prestamo, false);

        // ⛔ Corrección 2026-09-30 (revisión del árbitro, defectos 1 y 2): el devengado y el
        // cobrado de una fila tienen que salir del MISMO universo de cuotas, cuota por cuota, o
        // el saldo puede quedar negativo (p.ej. mora cobrada en una cuota ya PAGADA, o seguro
        // pagado por adelantado en una cuota futura). Por eso cada componente se calcula POR
        // CUOTA con un piso en cero (`saldo_i = max(0, reglaDevengado_i − pagado_i)`) y se suma
        // después — así lo cobrado nunca se queda sin su devengado. El capital es la excepción:
        // su "devengado" de fila es `Prestamo.montoSolicitado` (diseño §4.4bis), no una suma; el
        // saldo de capital sí se suma por cuota, y es esa suma la que compara la invariante 3
        // contra monto − cobrado (control real, no tautológico).
        double capitalDevengado = nvl(prestamo.getMontoSolicitado());
        double capitalCobrado = 0, saldoCapital = 0;
        double interesDevengado = 0, interesCobrado = 0, saldoInteres = 0;
        double desgravamenDevengado = 0, desgravamenCobrado = 0, saldoDesgravamen = 0;
        double seguroDevengado = 0, seguroCobrado = 0, saldoSeguro = 0;
        double moraDevengada = 0, moraCobrada = 0, saldoMora = 0;

        long cuotasCobradas = 0;
        long cuotasPorVencer = 0;
        long cuotasConSeguroAAnular = 0;
        LocalDateTime ultimoCobro = null;
        LocalDate inicioMora = null;

        for (DetallePrestamo cuota : universo) {
            double[] pagos = pagosPorCuota.get(cuota.getCodigo());
            double desgravamenPagadoCuota = pagos != null ? pagos[0] : 0.0;
            double moraPagadaCuota = pagos != null ? pagos[1] : 0.0;
            double interesVencidoPagadoCuota = pagos != null ? pagos[2] : 0.0;
            double interesPagadoCuota = pagos != null ? pagos[3] : 0.0;
            double capitalPagadoCuota = pagos != null ? pagos[4] : 0.0;
            double seguroPagadoCuota = pagos != null ? pagos[5] : 0.0;
            // PGPRSLOT, YA FILTRADO por tipo de pago en
            // PagoPrestamoDaoServiceImpl.selectDatosPagosVigentes (TIPOS_PAGO_CON_ABONO_CAPITAL):
            // no todo "pago extra" es capital. Un abono real (ABONO_CAPITAL, PRECANCELACION, o
            // la migración del 62439) se graba con capitalPagado=0 y rehace la tabla, así que Σ
            // capital de las cuotas = monto − abonos (API-PASE-A-PLAZO-VENCIDO.md §2). Pero un
            // pago tipo "DEP" (migración vieja del 2025-04-03) usa PGPRSLOT para otra cosa —
            // sumarlo ahí fabricaba un sobrante inexistente en 8 préstamos reales (corrección
            // 2026-09-30). Acá no hace falta mirar el tipo: ya viene en 0 si no corresponde.
            double abonoExtraCuota = pagos != null ? pagos[6] : 0.0;

            boolean vencimientoEnElCorteOAntes = cuota.getFechaVencimiento() != null
                && !cuota.getFechaVencimiento().toLocalDate().isAfter(corte);
            boolean vencidaAlCorte = cuota.getFechaVencimiento() != null
                && cuota.getFechaVencimiento().toLocalDate().isBefore(corte);

            // Capital: el SALDO es por cuota, floreado en 0, y usa SOLO capitalPagado — el abono
            // extra no es de esta cuota puntual, es una reducción del préstamo. El COBRADO de la
            // FILA sí suma el abono extra (ver comentario de abonoExtraCuota arriba). El
            // devengado de la FILA sigue siendo el monto del préstamo.
            double saldoCapitalCuota = Math.max(0.0, redondear(nvl(cuota.getCapital()) - capitalPagadoCuota));
            capitalCobrado += capitalPagadoCuota + abonoExtraCuota;
            saldoCapital += saldoCapitalCuota;

            // Interés: SOLO hasta la fecha de corte (D25, decisión del usuario 2026-09-30,
            // corrige la aceleración total que regía hasta esta vuelta) — misma bandera que ya
            // usan desgravamen y seguro. Sin prorrateo del período en curso: es la regla de la
            // precancelación (ProcesoPagoPrestamoServiceImpl.calcularPrecancelacion) — las
            // cuotas exigibles (vencimiento ≤ fin del día del corte) entran COMPLETAS, el interés
            // de las futuras queda condonado (en 0). CAPITAL NO sigue esta regla: se sigue
            // acelerando completo, todas las cuotas, con o sin vencer.
            // Suma DTPRINTR + DTPRINVN (interés vencido) del lado del devengado, e
            // interesPagado + interesVencidoPagado del lado del pagado — misma agregación que
            // MotorPagoPrestamoServiceImpl.calcularSaldosCuota, que ya trata el interés vencido
            // como un componente propio. El piso por cuota es igual al de desgravamen/seguro: un
            // interés pagado por adelantado sobre una cuota futura no deja el saldo negativo —
            // su devengado pasa a ser lo pagado, con saldo 0.
            double interesReglaCuota = vencimientoEnElCorteOAntes
                ? nvl(cuota.getInteres()) + nvl(cuota.getInteresVencido()) : 0.0;
            double interesPagadoTotalCuota = interesPagadoCuota + interesVencidoPagadoCuota;
            double saldoInteresCuota = Math.max(0.0, redondear(interesReglaCuota - interesPagadoTotalCuota));
            interesCobrado += interesPagadoTotalCuota;
            saldoInteres += saldoInteresCuota;
            interesDevengado += interesPagadoTotalCuota + saldoInteresCuota;

            // Desgravamen: devengado de la cuota es 0 si vence después del corte (D22) — un
            // adelanto pagado sobre una cuota futura no deja el saldo en negativo, se pisa en 0.
            double desgravamenReglaCuota = vencimientoEnElCorteOAntes ? nvl(cuota.getDesgravamen()) : 0.0;
            double saldoDesgravamenCuota = Math.max(0.0, redondear(desgravamenReglaCuota - desgravamenPagadoCuota));
            desgravamenCobrado += desgravamenPagadoCuota;
            saldoDesgravamen += saldoDesgravamenCuota;
            desgravamenDevengado += desgravamenPagadoCuota + saldoDesgravamenCuota;

            // Seguro de incendio: mismo criterio que desgravamen.
            double seguroReglaCuota = vencimientoEnElCorteOAntes ? nvl(cuota.getValorSeguroIncendio()) : 0.0;
            double saldoSeguroCuota = Math.max(0.0, redondear(seguroReglaCuota - seguroPagadoCuota));
            seguroCobrado += seguroPagadoCuota;
            saldoSeguro += saldoSeguroCuota;
            seguroDevengado += seguroPagadoCuota + saldoSeguroCuota;

            // Mora: solo cuentas vencidas e IMPAGAS al corte; una cuota ya PAGADA nunca genera
            // mora devengada, aunque haya pagado mora en algún momento (ese pagado igual entra
            // al cobrado, y el piso en 0 evita que el saldo quede negativo por eso).
            double moraReglaCuota = (vencidaAlCorte && !esPagada(cuota))
                ? procesoMoraPrestamoService.calcularMoraCuota(cuota, tasaDiaria, corte) : 0.0;
            double saldoMoraCuota = Math.max(0.0, redondear(moraReglaCuota - moraPagadaCuota));
            moraCobrada += moraPagadaCuota;
            saldoMora += saldoMoraCuota;
            moraDevengada += moraPagadaCuota + saldoMoraCuota;
            if (vencidaAlCorte && !esPagada(cuota)
                    && (inicioMora == null || (cuota.getFechaVencimiento() != null
                        && cuota.getFechaVencimiento().toLocalDate().isBefore(inicioMora)))) {
                inicioMora = cuota.getFechaVencimiento() != null ? cuota.getFechaVencimiento().toLocalDate() : inicioMora;
            }

            if (esPagada(cuota)) {
                cuotasCobradas++;
            } else if (cuota.getFechaVencimiento() != null && cuota.getFechaVencimiento().toLocalDate().isAfter(corte)) {
                cuotasPorVencer++;
                if (nvl(cuota.getDesgravamen()) > 0.0 || nvl(cuota.getValorSeguroIncendio()) > 0.0) {
                    cuotasConSeguroAAnular++;
                }
            }
            // Respaldo: DTPRFCPG viene null en casi todo préstamo migrado, aunque sí tenga
            // pagos reales en CRD.PGPR. maxFechaPagoPrestamo (MAX(PGPRFCHA) en lote) manda
            // cuando existe; esto solo se usa si el préstamo no tiene NINGÚN pago vigente.
            if (cuota.getFechaPagado() != null && (ultimoCobro == null || cuota.getFechaPagado().isAfter(ultimoCobro))) {
                ultimoCobro = cuota.getFechaPagado();
            }
        }
        LocalDateTime fechaUltimoCobroFinal = maxFechaPagoPrestamo != null ? maxFechaPagoPrestamo : ultimoCobro;

        saldoCapital = redondear(saldoCapital);
        saldoInteres = redondear(saldoInteres);
        saldoDesgravamen = redondear(saldoDesgravamen);
        saldoSeguro = redondear(saldoSeguro);
        saldoMora = redondear(saldoMora);

        c.setCapital(new ComponenteCuadroPlazoVencido(redondear(capitalDevengado), redondear(capitalCobrado), saldoCapital));
        c.setInteres(new ComponenteCuadroPlazoVencido(redondear(interesDevengado), redondear(interesCobrado), saldoInteres));
        c.setDesgravamen(new ComponenteCuadroPlazoVencido(redondear(desgravamenDevengado), redondear(desgravamenCobrado), saldoDesgravamen));
        c.setSeguroIncendio(new ComponenteCuadroPlazoVencido(redondear(seguroDevengado), redondear(seguroCobrado), saldoSeguro));
        c.setMora(new ComponenteCuadroPlazoVencido(redondear(moraDevengada), redondear(moraCobrada), saldoMora));

        double totalCobrado = redondear(capitalCobrado + interesCobrado + desgravamenCobrado + seguroCobrado + moraCobrada);
        double totalPorCobrar = redondear(saldoCapital + saldoInteres + saldoDesgravamen + saldoSeguro + saldoMora);
        c.setTotalCobrado(totalCobrado);
        c.setTotalPorCobrar(totalPorCobrar);

        long cuotasPendientes = universo.size() - cuotasCobradas;
        c.setCuotasPlazo(prestamo.getPlazo());
        c.setCuotasCobradas(cuotasCobradas);
        c.setCuotasPendientes(cuotasPendientes);
        c.setCuotasPorVencer(cuotasPorVencer);
        c.setCuotasConSeguroAAnular(cuotasConSeguroAAnular);
        c.setFechaUltimoCobro(fechaUltimoCobroFinal != null ? fechaUltimoCobroFinal.toLocalDate() : null);
        c.setFechaInicioMora(inicioMora);

        if (prestamo.getPlazo() != null && prestamo.getPlazo() != universo.size()) {
            // Se informa, no se elige en silencio (diseño §4.4bis) — no bloquea la declaración.
            System.out.println("  ⚠️ Préstamo " + prestamo.getCodigo() + ": Prestamo.plazo=" + prestamo.getPlazo()
                + " pero la tabla tiene " + universo.size() + " cuotas.");
        }

        verificarInvariantes(c, universo.size());
        return c;
    }

    /** Las cinco invariantes del cuadro (contrato §2.3, diseño §4.4bis). */
    private void verificarInvariantes(CandidatoPlazoVencido c, long cuotasTabla) {
        List<String> inconsistencias = new ArrayList<>();

        // 1. devengado = cobrado + saldo, fila por fila
        verificarFila("capital", c.getCapital(), inconsistencias);
        verificarFila("interés", c.getInteres(), inconsistencias);
        verificarFila("desgravamen", c.getDesgravamen(), inconsistencias);
        verificarFila("seguro de incendio", c.getSeguroIncendio(), inconsistencias);
        verificarFila("mora", c.getMora(), inconsistencias);

        // 2. Σ saldos = total por cobrar; Σ cobrados = total cobrado
        double sumaSaldos = redondear(c.getCapital().getSaldo() + c.getInteres().getSaldo()
            + c.getDesgravamen().getSaldo() + c.getSeguroIncendio().getSaldo() + c.getMora().getSaldo());
        if (!cerca(sumaSaldos, c.getTotalPorCobrar())) {
            inconsistencias.add("Σ saldos (" + sumaSaldos + ") ≠ total por cobrar (" + c.getTotalPorCobrar() + ")");
        }
        double sumaCobrados = redondear(c.getCapital().getCobrado() + c.getInteres().getCobrado()
            + c.getDesgravamen().getCobrado() + c.getSeguroIncendio().getCobrado() + c.getMora().getCobrado());
        if (!cerca(sumaCobrados, c.getTotalCobrado())) {
            inconsistencias.add("Σ cobrados (" + sumaCobrados + ") ≠ total cobrado (" + c.getTotalCobrado() + ")");
        }

        // 3. saldo de capital = monto − capital cobrado
        double saldoCapitalEsperado = redondear(c.getMontoPrestamo() - c.getCapital().getCobrado());
        if (!cerca(saldoCapitalEsperado, c.getCapital().getSaldo())) {
            inconsistencias.add("saldo de capital (" + c.getCapital().getSaldo()
                + ") ≠ monto − capital cobrado (" + saldoCapitalEsperado + ")");
        }

        // 4. cobradas + pendientes = cuotas de la tabla
        long sumaCuotas = c.getCuotasCobradas() + c.getCuotasPendientes();
        if (sumaCuotas != cuotasTabla) {
            inconsistencias.add("cuotas cobradas (" + c.getCuotasCobradas() + ") + pendientes ("
                + c.getCuotasPendientes() + ") = " + sumaCuotas + " ≠ cuotas de la tabla (" + cuotasTabla + ")");
        }

        // 5. ningún saldo negativo
        verificarNoNegativo("capital", c.getCapital().getSaldo(), inconsistencias);
        verificarNoNegativo("interés", c.getInteres().getSaldo(), inconsistencias);
        verificarNoNegativo("desgravamen", c.getDesgravamen().getSaldo(), inconsistencias);
        verificarNoNegativo("seguro de incendio", c.getSeguroIncendio().getSaldo(), inconsistencias);
        verificarNoNegativo("mora", c.getMora().getSaldo(), inconsistencias);
        verificarNoNegativo("total por cobrar", c.getTotalPorCobrar(), inconsistencias);

        c.setInconsistencias(inconsistencias);
        c.setValido(inconsistencias.isEmpty());
    }

    private void verificarFila(String nombre, ComponenteCuadroPlazoVencido fila, List<String> inconsistencias) {
        double esperado = redondear(nvl(fila.getCobrado()) + nvl(fila.getSaldo()));
        if (!cerca(esperado, nvl(fila.getDevengado()))) {
            inconsistencias.add(nombre + ": devengado (" + fila.getDevengado() + ") ≠ cobrado + saldo (" + esperado + ")");
        }
    }

    private void verificarNoNegativo(String nombre, Double valor, List<String> inconsistencias) {
        if (valor != null && valor < -TOLERANCIA) {
            inconsistencias.add(nombre + ": saldo negativo (" + valor + ")");
        }
    }

    private boolean cerca(double a, double b) {
        return Math.abs(a - b) <= TOLERANCIA;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /** {@code idAsoprep} y, si es nulo, {@code codigo} — misma convención que todo el frontend. */
    private String numeroPrestamoImpreso(Prestamo prestamo) {
        return prestamo.getIdAsoprep() != null
            ? String.valueOf(prestamo.getIdAsoprep())
            : String.valueOf(prestamo.getCodigo());
    }

    // ========================================================================
    // Helpers — carga en lote (sin N+1, ver ÍTEM 0.c)
    // ========================================================================

    private Map<Long, List<DetallePrestamo>> cargarCuotasPorPrestamo(List<Long> idsPrestamo) throws Throwable {
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = new HashMap<>();
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamos(idsPrestamo);
        for (DetallePrestamo cuota : todas) {
            Long idPrestamo = cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null;
            cuotasPorPrestamo.computeIfAbsent(idPrestamo, k -> new ArrayList<>()).add(cuota);
        }
        return cuotasPorPrestamo;
    }

    private Map<Long, double[]> cargarPagosPorCuota(Map<Long, List<DetallePrestamo>> cuotasPorPrestamo) throws Throwable {
        List<Long> idsCuotas = new ArrayList<>();
        for (List<DetallePrestamo> cuotas : cuotasPorPrestamo.values()) {
            for (DetallePrestamo cuota : cuotas) {
                idsCuotas.add(cuota.getCodigo());
            }
        }
        return agruparPagosPorCuota(pagoPrestamoDaoService.selectDatosPagosVigentes(idsCuotas));
    }

    /**
     * Última fecha de pago vigente por préstamo, EN LOTE (una sola consulta para todos los
     * préstamos, mismo criterio que los pagos por cuota). Respaldo de
     * {@code DetallePrestamo.fechaPagado} para préstamos migrados, donde esa columna suele venir
     * null aunque el préstamo sí tenga pagos reales (ítem 3, API-PASE-A-PLAZO-VENCIDO.md §3).
     */
    private Map<Long, LocalDateTime> cargarMaxFechaPagoPorPrestamo(List<Long> idsPrestamo) throws Throwable {
        Map<Long, LocalDateTime> maxFechaPorPrestamo = new HashMap<>();
        for (Object[] fila : pagoPrestamoDaoService.selectMaxFechaPagoByPrestamos(idsPrestamo)) {
            maxFechaPorPrestamo.put((Long) fila[0], (LocalDateTime) fila[1]);
        }
        return maxFechaPorPrestamo;
    }

    /**
     * Agrupa las filas escalares de {@code PagoPrestamoDaoService#selectDatosPagosVigentes}
     * (una fila por PAGO, no por cuota) sumando por componente. Índices del arreglo resultado:
     * 0 desgravamen, 1 moraPagada, 2 interesVencidoPagado, 3 interesPagado, 4 capitalPagado,
     * 5 valorSeguroIncendio, 6 saldoOtros YA FILTRADO por tipo de pago (pago extra que SÍ es
     * capital; 0 si el pago es de un tipo como "DEP" donde no lo es — ver
     * PagoPrestamoDaoServiceImpl.TIPOS_PAGO_CON_ABONO_CAPITAL) — mismo orden que la proyección
     * JPQL (desplazado -1 porque acá no se guarda el id de la cuota).
     */
    private Map<Long, double[]> agruparPagosPorCuota(List<Object[]> filas) {
        Map<Long, double[]> mapa = new HashMap<>();
        if (filas == null) {
            return mapa;
        }
        for (Object[] fila : filas) {
            Long idCuota = (Long) fila[0];
            double[] acumulado = mapa.computeIfAbsent(idCuota, k -> new double[7]);
            acumulado[0] += nvl((Double) fila[1]);
            acumulado[1] += nvl((Double) fila[2]);
            acumulado[2] += nvl((Double) fila[3]);
            acumulado[3] += nvl((Double) fila[4]);
            acumulado[4] += nvl((Double) fila[5]);
            acumulado[5] += nvl((Double) fila[6]);
            acumulado[6] += nvl((Double) fila[7]);
        }
        return mapa;
    }

    // ========================================================================
    // Helpers — declaración
    // ========================================================================

    private void validarFechaCorte(LocalDate fechaCorte) {
        if (fechaCorte == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": fechaCorte es obligatoria");
        }
        if (fechaCorte.isAfter(LocalDate.now())) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": la fecha de corte " + fechaCorte + " es futura");
        }
    }

    /**
     * D26: valida que {@code numeroCrudo} sea SOLO dígitos (sin el prefijo, sin guiones, sin
     * año) y mayor que 0, y lo compone como
     * {@code PREFIJO_MEMORANDO + "%03d" + "-" + añoActual}. Un número de 4 o más dígitos queda
     * tal cual — {@code "%03d"} solo rellena, nunca trunca.
     */
    private String componerNumeroMemorando(String numeroCrudo) {
        String valor = numeroCrudo.trim();
        if (!valor.matches("\\d+")) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": el número de memorando debe ser solo el número, sin prefijo (recibido: '" + numeroCrudo + "')");
        }
        long numero;
        try {
            numero = Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": el número de memorando '" + numeroCrudo + "' es demasiado grande");
        }
        if (numero <= 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO
                + ": el número de memorando debe ser mayor que 0 (recibido: '" + numeroCrudo + "')");
        }
        return PREFIJO_MEMORANDO + String.format("%03d", numero) + "-" + LocalDate.now().getYear();
    }

    private DeclaracionPlazoVencido buscarDeclaracion(Long idDeclaracion) throws Throwable {
        try {
            return declaracionDaoService.selectById(idDeclaracion, NombreEntidadesCredito.DECLARACION_PLAZO_VENCIDO);
        } catch (NoResultException e) {
            throw new IncomeException(ERR_DECLARACION_NO_ENCONTRADA + ": no existe la declaración " + idDeclaracion);
        }
    }

    private void validarNoLiquidadaNiRevertidaParaLiquidar(DeclaracionPlazoVencido declaracion) {
        if (declaracion.getEstado() == null) {
            return;
        }
        if (declaracion.getEstado() == DeclaracionPlazoVencido.ESTADO_LIQUIDADA) {
            throw new IncomeException(ERR_YA_LIQUIDADA + ": la declaración " + declaracion.getCodigo() + " ya está liquidada");
        }
        if (declaracion.getEstado() == DeclaracionPlazoVencido.ESTADO_REVERTIDA) {
            throw new IncomeException(ERR_DECLARACION_REVERTIDA + ": la declaración " + declaracion.getCodigo() + " está revertida");
        }
    }

    /** Entidad JPA → DTO de respuesta, nunca la entidad cruda (§9 del contrato). */
    private DeclaracionPlazoVencidoDTO mapearDTO(DeclaracionPlazoVencido d) {
        DeclaracionPlazoVencidoDTO dto = new DeclaracionPlazoVencidoDTO();
        dto.setIdDeclaracion(d.getCodigo());
        dto.setIdPrestamo(d.getPrestamo() != null ? d.getPrestamo().getCodigo() : null);
        dto.setNumeroPrestamo(d.getNumeroPrestamoImpreso());
        dto.setEstado(d.getEstado());
        dto.setNumeroMemorando(d.getNumeroMemorando());
        dto.setFechaCorte(d.getFechaCorte());
        dto.setParaNombre(d.getParaNombre());
        dto.setParaCargo(d.getParaCargo());
        dto.setCcNombre(d.getCcNombre());
        dto.setCcCargo(d.getCcCargo());
        dto.setNombreParticipe(d.getNombreParticipe());
        dto.setCedula(d.getCedula());
        dto.setTipoCredito(d.getTipoCredito());
        dto.setUsuarioDeclaracion(d.getUsuarioDeclaracion());
        dto.setFechaDeclaracion(d.getFechaDeclaracion());
        dto.setUsuarioReverso(d.getUsuarioReverso());
        dto.setFechaReverso(d.getFechaReverso());
        dto.setMotivoReverso(d.getMotivoReverso());

        CandidatoPlazoVencido cuadro = new CandidatoPlazoVencido();
        cuadro.setIdPrestamo(dto.getIdPrestamo());
        cuadro.setNumeroPrestamo(d.getNumeroPrestamoImpreso());
        cuadro.setTipoCredito(d.getTipoCredito());
        cuadro.setNombreParticipe(d.getNombreParticipe());
        cuadro.setCedula(d.getCedula());
        cuadro.setFechaInicio(d.getFechaInicial());
        cuadro.setFechaFin(d.getFechaFinal());
        cuadro.setFechaUltimoCobro(d.getFechaUltimoCobro());
        cuadro.setFechaInicioMora(d.getFechaInicioMora());
        cuadro.setDividendoMensual(d.getDividendoMensual());
        cuadro.setMontoPrestamo(d.getMonto());
        cuadro.setCapital(new ComponenteCuadroPlazoVencido(d.getMonto(), d.getCapitalCobrado(), d.getSaldoCapital()));
        cuadro.setInteres(new ComponenteCuadroPlazoVencido(d.getInteresDevengado(), d.getInteresCobrado(), d.getSaldoInteres()));
        cuadro.setDesgravamen(new ComponenteCuadroPlazoVencido(d.getDesgravamenDevengado(), d.getDesgravamenCobrado(), d.getSaldoDesgravamen()));
        cuadro.setSeguroIncendio(new ComponenteCuadroPlazoVencido(d.getSeguroDevengado(), d.getSeguroCobrado(), d.getSaldoSeguro()));
        cuadro.setMora(new ComponenteCuadroPlazoVencido(d.getMoraDevengada(), d.getMoraCobrada(), d.getSaldoMora()));
        cuadro.setTotalCobrado(d.getTotalCobrado());
        cuadro.setTotalPorCobrar(d.getTotalPorCobrar());
        cuadro.setCuotasPlazo(d.getCuotasPlazo());
        cuadro.setCuotasCobradas(d.getCuotasCobradas());
        cuadro.setCuotasPendientes(d.getCuotasPendientes());
        cuadro.setCuotasPorVencer(d.getCuotasPorVencer());
        cuadro.setValido(true);
        dto.setCuadro(cuadro);

        if (d.getFechaLiquidacion() != null) {
            LiquidacionPlazoVencido liquidacion = new LiquidacionPlazoVencido();
            liquidacion.setFechaCorte(d.getFechaCorteLiquidacion());
            liquidacion.setSaldoCapital(d.getLiquidacionSaldoCapital());
            liquidacion.setInteresVencido(d.getLiquidacionInteres());
            liquidacion.setDesgravamen(d.getLiquidacionDesgravamen());
            liquidacion.setSeguroIncendio(d.getLiquidacionSeguro());
            liquidacion.setMora(d.getLiquidacionMora());
            liquidacion.setTotal(d.getLiquidacionTotal());
            liquidacion.setCuotasImpagas(d.getLiquidacionCuotasImpagas());
            liquidacion.setUsuario(d.getUsuarioLiquidacion());
            liquidacion.setFecha(d.getFechaLiquidacion());
            dto.setLiquidacion(liquidacion);
        }

        return dto;
    }
}
