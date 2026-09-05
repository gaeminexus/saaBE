package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.crd.dao.AsientoCierreCarteraDaoService;
import com.saa.ejb.crd.dao.BandaCierreCarteraDaoService;
import com.saa.ejb.crd.dao.BandaProductoDaoService;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.dao.CorridaCierreCarteraDaoService;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.CierreCarteraService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.BandaSnapshotCierre;
import com.saa.ejb.crd.service.dto.CierreCartera;
import com.saa.ejb.crd.service.dto.ControlArchivoPetro;
import com.saa.ejb.crd.service.dto.DesgloseAportesCierre;
import com.saa.ejb.crd.service.dto.DesviacionBandaCierre;
import com.saa.ejb.crd.service.dto.LineaAsientoCierre;
import com.saa.ejb.crd.service.dto.SolicitudCierreCartera;
import com.saa.ejb.crd.service.dto.SubProcesoCierre;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.crd.AsientoCierreCartera;
import com.saa.model.crd.BandaCierreCartera;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.model.crd.CorridaCierreCartera;
import com.saa.model.crd.Filial;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.NombreEntidadesSistema;
import com.saa.rubros.CrdEstadoCargaArchivo;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.EstadoCorridaCierreCartera;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.Rubros;
import com.saa.rubros.SubProcesoCierreCartera;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCarteraBanda;
import com.saa.basico.ejb.EmpresaDaoService;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del proceso mensual de apertura / cierre de cartera.
 * Ver {@link CierreCarteraService} para el contrato y las decisiones de diseño.
 */
@Stateless
public class CierreCarteraServiceImpl implements CierreCarteraService {

    /** Movimiento de una línea de plantilla: 1 = DEBE. */
    private static final long MOVIMIENTO_DEBE = 1L;

    /**
     * Tolerancia de cuadre. Cada línea se redondea a dos decimales, así que la diferencia
     * máxima esperable es de unos pocos centavos. Por encima de esto el proceso NO ajusta:
     * falla diciendo cuánto falta, porque una diferencia así ya no es redondeo.
     */
    private static final double TOLERANCIA_CUADRE = 0.50D;

    /** Importe por debajo del cual una línea no se emite. */
    private static final double MINIMO_LINEA = 0.005D;

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    @EJB
    private CorridaCierreCarteraDaoService corridaCierreCarteraDaoService;

    @EJB
    private BandaCierreCarteraDaoService bandaCierreCarteraDaoService;

    @EJB
    private AsientoCierreCarteraDaoService asientoCierreCarteraDaoService;

    @EJB
    private ConfiguracionBandaProductoDaoService configuracionBandaProductoDaoService;

    @EJB
    private BandaProductoDaoService bandaProductoDaoService;

    /** Fase 1. Deriva los rangos y clasifica; aquí no se reimplementa nada de eso. */
    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private ProductoDaoService productoDaoService;

    /** Control de archivo Petro del mes (D13). Solo lectura. */
    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private EmpresaDaoService empresaDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    /**
     * Fuente del esperado de aportes del asiento ③ (2026-08-31) — MISMA rama de cálculo que
     * usa la generación real del archivo Petro, nunca puede divergir de lo que efectivamente
     * se le manda a Petro. Ver el javadoc de {@code calcularAportesEsperados}.
     */
    @EJB
    private com.saa.ejb.crd.service.GeneracionArchivoPetroService generacionArchivoPetroService;

    /** Todas las filiales (CRD.FLLL), sin filtrar por estado — ver el javadoc de armaApertura. */
    @EJB
    private com.saa.ejb.crd.dao.FilialDaoService filialDaoService;

    /** Solo lectura: recupera la cuenta de cada línea antes de grabar el asiento. */
    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    /** Solo para el reverso: {@code anulaAsiento} decide entre anular y reversar. */
    @EJB
    private AsientoService asientoService;

    /** Flag global de contabilidad de CRD (D10 del plan de devengo de aportes). */
    @EJB
    private com.saa.ejb.crd.service.ConfiguracionContabilidadService configuracionContabilidadService;

    /**
     * Sólo para {@code tipoCarteraYDias} (2026-09-05) — {@code distribuye} delegaba antes su
     * propia clasificación por tipo de cartera/días, con dos defectos ya corregidos en la
     * versión de este servicio (ver su javadoc). Delegar en vez de mantener una copia propia
     * es lo que evita que las dos vuelvan a divergir.
     */
    @EJB
    private com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService contabilizacionIndividualCreditoService;

    // =====================================================================
    // API
    // =====================================================================

    @Override
    public CierreCartera previsualizar(SolicitudCierreCartera solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (previsualizar) CierreCarteraService - empresa: "
                + (solicitud != null ? solicitud.getIdEmpresa() : null) + " periodo: "
                + (solicitud != null ? solicitud.getAnio() + "-" + solicitud.getMes() : null));
        valida(solicitud);
        return calcula(solicitud);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CierreCartera ejecutar(SolicitudCierreCartera solicitud) throws Throwable {
        System.out.println("Ingresa al metodo (ejecutar) CierreCarteraService - empresa: "
                + (solicitud != null ? solicitud.getIdEmpresa() : null) + " periodo: "
                + (solicitud != null ? solicitud.getAnio() + "-" + solicitud.getMes() : null));
        valida(solicitud);

        // Idempotencia. El indice UK_CRCT_PERIODO lo garantiza tambien en la base, pero un
        // ORA-00001 no le dice nada al usuario: el mensaje util sale de aqui.
        CorridaCierreCartera existente = corridaCierreCarteraDaoService.selectVivaByPeriodo(
                solicitud.getIdEmpresa(), solicitud.getAnio(), solicitud.getMes());
        if (existente != null) {
            throw new IncomeException("El periodo " + solicitud.getAnio() + "-"
                    + dosDigitos(solicitud.getMes()) + " ya tiene la corrida "
                    + existente.getCodigo() + " en estado "
                    + nombreEstado(existente.getIdEstado())
                    + ". Reverse esa corrida antes de volver a ejecutar el cierre.");
        }

        CierreCartera calculo = calcula(solicitud);

        // Control de archivo Petro (D13). Aqui SI corta: previsualizar deja ver el estado,
        // ejecutar no deja contabilizar un neteo con el lado aportes incompleto.
        exigeArchivoPetro(calculo.getControlArchivoPetro(), solicitud,
                calculo.getDesgloseAportes());

        LocalDateTime ahora = LocalDateTime.now();

        // 1. Cabecera
        CorridaCierreCartera corrida = new CorridaCierreCartera();
        corrida.setEmpresa(recuperaEmpresa(solicitud.getIdEmpresa()));
        corrida.setAnio(solicitud.getAnio());
        corrida.setMes(solicitud.getMes());
        corrida.setFechaCorte(calculo.getFechaCorte());
        corrida.setFechaProceso(calculo.getFechaProceso());
        corrida.setIdEstado(Long.valueOf(EstadoCorridaCierreCartera.PREPARADA));
        // Si se salto el control, queda escrito en la corrida: la omision no puede vivir
        // solo en la peticion, que no deja rastro.
        corrida.setObservacion(Boolean.TRUE.equals(solicitud.getOmitirControlArchivoPetro())
                ? concatena(solicitud.getObservacion(),
                        "CONTROL DE ARCHIVO PETRO OMITIDO por "
                        + (solicitud.getUsuario() != null ? solicitud.getUsuario() : "SISTEMA")
                        + ": " + solicitud.getMotivoOmisionControl())
                : solicitud.getObservacion());
        corrida.setEstado(Long.valueOf(Estado.ACTIVO));
        corrida.setUsuarioRegistro(solicitud.getUsuario());
        corrida.setIpRegistro(solicitud.getIp());
        corrida.setFechaRegistro(ahora);
        corridaCierreCarteraDaoService.save(corrida, null);
        if (corrida.getCodigo() == null) {
            throw new IncomeException("No se pudo obtener el codigo de la corrida grabada");
        }

        // 2. Snapshot
        grabaSnapshot(corrida, calculo, solicitud, ahora);

        // 3. Asientos, uno por sub-proceso con lineas
        for (SubProcesoCierre sub : calculo.getSubProcesos()) {
            if (Boolean.TRUE.equals(sub.getOmitido()) || sub.getLineas().isEmpty()) {
                continue;
            }
            generaAsiento(corrida, sub, solicitud, ahora);
        }

        // 4. La corrida queda EJECUTADA
        corrida.setIdEstado(Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA));
        corrida.setUsuarioModificacion(solicitud.getUsuario());
        corrida.setIpModificacion(solicitud.getIp());
        corrida.setFechaModificacion(LocalDateTime.now());
        corridaCierreCarteraDaoService.save(corrida, corrida.getCodigo());

        calculo.setIdCorrida(corrida.getCodigo());
        calculo.setIdEstado(corrida.getIdEstado());
        calculo.setNombreEstado(nombreEstado(corrida.getIdEstado()));
        return calculo;
    }

    @Override
    public CierreCartera consultar(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("Ingresa al metodo (consultar) CierreCarteraService - empresa: "
                + idEmpresa + " periodo: " + anio + "-" + mes);
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        validaPeriodo(anio, mes);

        CorridaCierreCartera corrida =
                corridaCierreCarteraDaoService.selectVivaByPeriodo(idEmpresa, anio, mes);
        if (corrida == null) {
            throw new IncomeException("El periodo " + anio + "-" + dosDigitos(mes)
                    + " no tiene una corrida de cierre de cartera vigente para la empresa "
                    + idEmpresa);
        }
        return armaDesdeGrabado(corrida);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CierreCartera reversar(Long idCorrida, String usuario, String ip, String motivo)
            throws Throwable {
        System.out.println("Ingresa al metodo (reversar) CierreCarteraService - corrida: "
                + idCorrida);
        if (idCorrida == null) {
            throw new IncomeException("La corrida a reversar es obligatoria");
        }
        CorridaCierreCartera corrida;
        try {
            corrida = corridaCierreCarteraDaoService.selectById(idCorrida,
                    NombreEntidadesCredito.CORRIDA_CIERRE_CARTERA);
        } catch (Throwable e) {
            throw new IncomeException("No existe la corrida de cierre de cartera " + idCorrida);
        }
        if (corrida == null) {
            throw new IncomeException("No existe la corrida de cierre de cartera " + idCorrida);
        }
        if (corrida.getIdEstado() == null
                || corrida.getIdEstado().longValue() != EstadoCorridaCierreCartera.EJECUTADA) {
            throw new IncomeException("La corrida " + idCorrida + " esta en estado "
                    + nombreEstado(corrida.getIdEstado())
                    + ": solo se puede reversar una corrida EJECUTADA.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        List<AsientoCierreCartera> asientos =
                asientoCierreCarteraDaoService.selectGeneradosByCorrida(idCorrida);
        for (AsientoCierreCartera registro : asientos) {
            if (registro.getAsiento() != null) {
                // anulaAsiento decide solo: anula si el periodo esta abierto y reversa si ya
                // esta mayorizado. No se replica esa logica aqui.
                // Se usa la sobrecarga con usuario y motivo: sin ella el asiento queda
                // anulado sin rastro de quien ni por que, y el motivo del reverso solo
                // sobreviviria en CRD.CRCT/CRD.ANCC, no en el asiento que ve el auditor.
                asientoService.anulaAsiento(registro.getAsiento(), usuario,
                        "Reverso de la corrida de cierre de cartera " + idCorrida
                                + (motivo != null && !motivo.trim().isEmpty()
                                        ? ": " + motivo.trim() : ""));
            }
            registro.setIdEstado(Long.valueOf(EstadoAsiento.ANULADO));
            registro.setUsuarioModificacion(usuario);
            registro.setIpModificacion(ip);
            registro.setFechaModificacion(ahora);
            asientoCierreCarteraDaoService.save(registro, registro.getCodigo());
        }

        // Las filas NO se borran: el snapshot y los registros de asiento quedan marcados,
        // que es lo que hace auditable el reverso.
        corrida.setIdEstado(Long.valueOf(EstadoCorridaCierreCartera.REVERSADA));
        corrida.setObservacion(concatena(corrida.getObservacion(),
                "REVERSADA " + ahora.toLocalDate() + (motivo != null ? ": " + motivo : "")));
        corrida.setUsuarioModificacion(usuario);
        corrida.setIpModificacion(ip);
        corrida.setFechaModificacion(ahora);
        corridaCierreCarteraDaoService.save(corrida, corrida.getCodigo());

        return armaDesdeGrabado(corrida);
    }

    @Override
    public List<CorridaCierreCartera> listarCorridas(Long idEmpresa) throws Throwable {
        System.out.println("Ingresa al metodo (listarCorridas) CierreCarteraService - empresa: "
                + idEmpresa);
        if (idEmpresa == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        return corridaCierreCarteraDaoService.selectByEmpresa(idEmpresa);
    }

    // =====================================================================
    // Cálculo
    // =====================================================================

    /**
     * Calcula la corrida completa sin tocar la base.
     *
     * @param solicitud : Empresa y período
     * @return          : La corrida calculada
     */
    private CierreCartera calcula(SolicitudCierreCartera solicitud) throws Throwable {
        LocalDate fechaCorte = ultimoDiaDelMes(solicitud.getAnio(), solicitud.getMes());
        LocalDate fechaProceso = fechaCorte.plusDays(1);
        LocalDate fechaCorteAnterior = fechaCorte.minusMonths(1)
                .withDayOfMonth(fechaCorte.minusMonths(1).lengthOfMonth());
        LocalDate fechaCorteApertura = fechaProceso.withDayOfMonth(fechaProceso.lengthOfMonth());

        CierreCartera resultado = new CierreCartera();
        resultado.setIdEmpresa(solicitud.getIdEmpresa());
        resultado.setAnio(solicitud.getAnio());
        resultado.setMes(solicitud.getMes());
        resultado.setFechaCorte(fechaCorte);
        resultado.setFechaProceso(fechaProceso);
        resultado.setFechaCorteApertura(fechaCorteApertura);

        // Parametrizacion de bandas vigente a la fecha de PROCESO. Se resuelve con la fecha
        // de proceso y no con la de corte porque el asiento se emite ese dia: una
        // configuracion que entra en vigencia el 1 rige para el cierre que se contabiliza
        // el 1.
        Map<Long, Producto> productos = indexaProductos();
        Map<String, List<BandaProductoDetalle>> bandas = cargaBandas(
                solicitud.getIdEmpresa(), fechaProceso, productos, resultado.getAdvertencias());

        List<Object[]> capitalPorVencimiento = cierreCarteraDaoService
                .selectCapitalPorProductoYVencimiento(solicitud.getIdEmpresa());

        // A CARTERA CONSTANTE: el mismo juego de cuotas, medido en dos fechas. Es lo que
        // hace que los asientos de reclasificacion cuadren por construccion.
        Map<String, BandaSnapshotCierre> distAnterior = distribuye(capitalPorVencimiento,
                fechaCorteAnterior, bandas, productos, null);
        List<String> avisosDistribucion = new ArrayList<String>();
        Map<String, BandaSnapshotCierre> distNueva = distribuye(capitalPorVencimiento,
                fechaCorte, bandas, productos, avisosDistribucion);
        resultado.getAdvertencias().addAll(avisosDistribucion);

        // SOLO para el asiento ② (cambio de bandas POR VENCER): "los valores del mes que se
        // abre" (pedido del usuario 2026-08-31) = el CIERRE del mes que se abre
        // (fechaCorteApertura, 31-ago si se cierra jul/abre ago), no su primer dia —
        // interpretacion mia: la comparacion siempre fue entre dos fotos de FIN DE MES
        // separadas por un mes, y fechaCorteApertura ya se usa con esa semantica en
        // armaApertura (selectCobrablePrestamosHasta(fechaCorteApertura)). Si el usuario la
        // corrige en la primera corrida, es una linea. NO reemplaza a distNueva: distNueva
        // sigue siendo el snapshot de fechaCorte que usan el asiento ①.1 (VENCIDO), el
        // snapshot guardado en el resultado y calculaVencidosDelMes — los tres SIN CAMBIOS.
        List<String> avisosApertura = new ArrayList<String>();
        Map<String, BandaSnapshotCierre> distApertura = distribuye(capitalPorVencimiento,
                fechaCorteApertura, bandas, productos, avisosApertura);
        resultado.getAdvertencias().addAll(avisosApertura);

        // Capital que cruzo la frontera durante el mes: por vencer al corte anterior y
        // vencido al corte actual. Es exactamente el "capital no pagado del mes" del
        // sub-proceso 1.
        Map<Long, Double> vencidosDelMes = calculaVencidosDelMes(capitalPorVencimiento,
                fechaCorteAnterior, fechaCorte);

        // SOLO para el asiento ② (POR VENCER): el mismo cruce pero sobre el par de fechas que
        // ahora diferencia ese asiento (31-jul -> 31-ago), no el de ①/①.1 (30-jun -> 31-jul).
        // La correccion de armaCambioBandas separa capital que cambio de banda POR ENVEJECER
        // (reclasificacion, lo que el asiento debe medir) de capital que salio de la banda 1
        // POR VENCER (eso no es reclasificacion dentro de por-vencer) — esa separacion tiene
        // que medirse sobre el MISMO PAR DE FECHAS que el diff, o le resta a ② un movimiento
        // de un mes que no es el que esta midiendo. vencidosDelMes original NO se toca: lo
        // siguen usando ① y ①.1, que no cambiaron de fechas.
        Map<Long, Double> vencidosDelMesApertura = calculaVencidosDelMes(capitalPorVencimiento,
                fechaCorte, fechaCorteApertura);

        resultado.setSnapshot(ordenaSnapshot(distNueva));
        resultado.setCapitalTotal(sumaCapital(distNueva));

        // Los seis sub-procesos, en orden. Fecha del asiento: el PRIMER DIA DEL MES QUE SE
        // ABRE (fechaProceso) en los cinco primeros; SOLO el neteo va con el ULTIMO DIA DEL
        // MES QUE SE CIERRA (fechaCorte) — regla confirmada por el usuario 2026-08-31 (versión
        // final; una versión anterior la tenía al revés y se revirtió el mismo día). No
        // "emparejar" los cinco fechaProceso con el fechaCorte del neteo — es a propósito.
        // fechaProceso tambien se usa arriba para la parametrizacion de bandas vigente
        // (linea ~355) y para fechaCorteApertura: esos dos usos NO son fecha de asiento.
        resultado.getSubProcesos().add(armaVencidos(vencidosDelMes, bandas, productos,
                fechaProceso, solicitud));
        // ② POR VENCER: valores del mes que se abre (31-jul -> 31-ago), par corrido un mes.
        resultado.getSubProcesos().add(armaCambioBandas(SubProcesoCierreCartera.CAMBIO_BANDAS_POR_VENCER,
                Long.valueOf(TipoCarteraBanda.POR_VENCER), distNueva, distApertura,
                vencidosDelMesApertura, productos, fechaProceso, solicitud));
        // ①.1 VENCIDO: sin cambios (30-jun -> 31-jul).
        resultado.getSubProcesos().add(armaCambioBandas(SubProcesoCierreCartera.CAMBIO_BANDAS_VENCIDO,
                Long.valueOf(TipoCarteraBanda.VENCIDO), distAnterior, distNueva,
                vencidosDelMes, productos, fechaProceso, solicitud));
        resultado.getSubProcesos().add(armaApertura(solicitud, fechaProceso, fechaCorteApertura));
        // ④ Devengo: fecha del asiento = fechaProceso (sin cambios); rango de cuotas = SOLO el
        // mes que se abre (fechaProceso -> fechaCorteApertura), no acumulado desde siempre.
        resultado.getSubProcesos().add(armaDevengoIntereses(solicitud, fechaProceso,
                fechaProceso, fechaCorteApertura));

        // El desglose de aportes se calcula ANTES del neteo, porque el neteo consume su
        // noCobrado. Y el control del archivo Petro se evalua con el desglose ya hecho, para
        // poder decir en el aviso cuanto dinero esta en juego.
        DesgloseAportesCierre desglose = calculaDesgloseAportes(fechaCorte);
        resultado.setDesgloseAportes(desglose);
        resultado.setControlArchivoPetro(
                controlaArchivoPetro(solicitud, desglose, resultado.getAdvertencias()));
        avisaExcesoCobro(desglose, resultado.getAdvertencias(), solicitud);

        resultado.getSubProcesos().add(armaNeteo(solicitud, fechaCorte, desglose));

        // Un descuadre se avisa en la previsualizacion en vez de reventar: contabilidad
        // tiene que poder VER el asiento defectuoso para entender que falta. La ejecucion
        // si lo rechaza.
        for (SubProcesoCierre sub : resultado.getSubProcesos()) {
            if (Boolean.TRUE.equals(sub.getOmitido())) {
                continue;
            }
            double diferencia = sub.getTotalDebe().doubleValue()
                    - sub.getTotalHaber().doubleValue();
            if (Math.abs(diferencia) >= 0.005D) {
                resultado.getAdvertencias().add("El asiento de " + sub.getNombre()
                        + " no cuadra: DEBE " + sub.getTotalDebe() + " y HABER "
                        + sub.getTotalHaber() + ", diferencia "
                        + redondea(Double.valueOf(diferencia))
                        + ". La ejecucion lo va a rechazar.");
            }
        }

        // Control contra el snapshot de la corrida anterior
        cargaDesviaciones(resultado, solicitud, distAnterior, productos);
        return resultado;
    }

    /**
     * Reparte el capital agregado por (producto, vencimiento) en las bandas que le
     * corresponden a una fecha dada.
     *
     * <p>
     * <b>2026-09-05, corregido — delega en {@code ContabilizacionIndividualCreditoService
     * #tipoCarteraYDias}, que es la DEFINICIÓN DE REFERENCIA de esta clasificación</b> (regla
     * de negocio confirmada por el usuario: el día del vencimiento la cuota todavía está POR
     * VENCER, recién al día siguiente pasa a VENCIDA — ver el javadoc de ese método para la
     * tabla de verdad completa). Antes este método tenía su PROPIA copia de la clasificación,
     * con los MISMOS dos defectos que tenía {@code tipoCarteraYDias} antes de corregirse:
     * clasificaba VENCIDO cuando {@code vencimiento == fecha} (debería ser POR_VENCER), y
     * sumaba {@code +1} a los días de vencido (que corría un día de más y rompía los bordes de
     * banda de 30/90/180/360 días). Se delega en vez de mantener una segunda copia corregida a
     * mano: dos copias divergen tarde o temprano, y ESTE método es el que clasifica TODA la
     * cartera en el cierre mensual — si él dice una cosa y el asiento individual de un pago
     * dice otra para la MISMA cuota, las dos contabilidades se contradicen al conciliar.
     * </p>
     *
     * @param filas      : {@code [idProducto, fechaVencimiento, capital, cantidad]}
     * @param fecha      : Fecha a la que se miden los días
     * @param bandas     : Bandas por (producto, tipo de cartera)
     * @param productos  : Productos indexados por código
     * @param avisos     : Lista donde anotar el capital que no se pudo clasificar; puede ser nula
     * @return           : Distribución indexada por producto-tipoCartera-banda
     */
    private Map<String, BandaSnapshotCierre> distribuye(List<Object[]> filas, LocalDate fecha,
            Map<String, List<BandaProductoDetalle>> bandas, Map<Long, Producto> productos,
            List<String> avisos) throws Throwable {

        Map<String, BandaSnapshotCierre> distribucion =
                new TreeMap<String, BandaSnapshotCierre>();
        Map<String, Double> sinClasificar = new LinkedHashMap<String, Double>();

        for (Object[] fila : filas) {
            Long idProducto = (Long) fila[0];
            LocalDate vencimiento = (LocalDate) fila[1];
            Double capital = (Double) fila[2];
            Long cantidad = (Long) fila[3];
            if (vencimiento == null || capital == null || capital.doubleValue() <= 0D) {
                continue;
            }

            // Delegado (2026-09-05) — ver el javadoc de este método y el de
            // ContabilizacionIndividualCreditoService#tipoCarteraYDias, la definición de
            // referencia. NO reimplementar acá: es exactamente cómo se llegó a tener dos
            // copias divergentes de esta clasificación.
            long[] tipoYDias = contabilizacionIndividualCreditoService.tipoCarteraYDias(vencimiento, fecha);
            Long tipoCartera = Long.valueOf(tipoYDias[0]);
            long dias = tipoYDias[1];

            List<BandaProductoDetalle> bandasProducto =
                    bandas.get(claveConfiguracion(idProducto, tipoCartera));
            if (bandasProducto == null || bandasProducto.isEmpty()) {
                acumulaSinClasificar(sinClasificar, idProducto, tipoCartera, capital, productos);
                continue;
            }

            BandaProductoDetalle banda = clasificadorBandaService
                    .clasificarEnBandas(bandasProducto, Long.valueOf(dias));
            String clave = claveBanda(idProducto, tipoCartera, banda.getNumero());
            BandaSnapshotCierre acumulado = distribucion.get(clave);
            if (acumulado == null) {
                acumulado = nuevaFilaSnapshot(idProducto, tipoCartera, banda, productos);
                distribucion.put(clave, acumulado);
            }
            acumulado.setCapital(suma(acumulado.getCapital(), capital));
            acumulado.setCantidad(Long.valueOf(acumulado.getCantidad().longValue()
                    + (cantidad != null ? cantidad.longValue() : 0L)));
        }

        // Las bandas sin capital tambien van al snapshot, en cero: sin ellas la corrida
        // siguiente no podria descargar una banda que se vacio.
        completaBandasVacias(distribucion, bandas, productos);

        if (avisos != null) {
            for (Map.Entry<String, Double> entrada : sinClasificar.entrySet()) {
                avisos.add("Sin configuracion de bandas: " + entrada.getKey() + " queda fuera de"
                        + " la distribucion por " + redondea(entrada.getValue())
                        + ". Parametrice el producto antes de ejecutar el cierre.");
            }
        }
        return distribucion;
    }

    /**
     * Capital que pasó de POR VENCER a VENCIDO durante el mes: el que vence después del
     * corte anterior y hasta el corte actual, y sigue impago.
     *
     * @param filas  : {@code [idProducto, fechaVencimiento, capital, cantidad]}
     * @param desde  : Corte anterior, exclusivo
     * @param hasta  : Corte actual, inclusivo
     * @return       : Capital por producto
     */
    private Map<Long, Double> calculaVencidosDelMes(List<Object[]> filas, LocalDate desde,
            LocalDate hasta) {
        Map<Long, Double> porProducto = new LinkedHashMap<Long, Double>();
        for (Object[] fila : filas) {
            Long idProducto = (Long) fila[0];
            LocalDate vencimiento = (LocalDate) fila[1];
            Double capital = (Double) fila[2];
            if (vencimiento == null || capital == null) {
                continue;
            }
            if (vencimiento.isAfter(desde) && !vencimiento.isAfter(hasta)) {
                Double actual = porProducto.get(idProducto);
                porProducto.put(idProducto, suma(actual, capital));
            }
        }
        return porProducto;
    }

    // =====================================================================
    // Sub-proceso ① — asiento de vencidos
    // =====================================================================

    /**
     * ① El capital no pagado del mes cerrado sale de la banda 1 de POR VENCER y entra a la
     * banda 1 de VENCIDO, por producto.
     *
     * @param vencidos    : Capital vencido en el mes, por producto
     * @param bandas      : Bandas por (producto, tipo de cartera)
     * @param productos   : Productos indexados
     * @param fecha       : Fecha contable del asiento
     * @param solicitud   : Solicitud, para la glosa
     * @return            : El sub-proceso con sus líneas
     */
    private SubProcesoCierre armaVencidos(Map<Long, Double> vencidos,
            Map<String, List<BandaProductoDetalle>> bandas, Map<Long, Producto> productos,
            LocalDate fecha, SolicitudCierreCartera solicitud) throws Throwable {

        SubProcesoCierre sub = nuevoSubProceso(SubProcesoCierreCartera.VENCIDOS,
                "Asiento de vencidos", "①", fecha,
                "CRD cierre de cartera " + periodo(solicitud) + " - capital vencido del mes");

        for (Map.Entry<Long, Double> entrada : vencidos.entrySet()) {
            Long idProducto = entrada.getKey();
            double valor = redondeaDouble(entrada.getValue());
            if (valor < MINIMO_LINEA) {
                continue;
            }
            BandaProductoDetalle bandaVencido = primeraBanda(bandas, idProducto,
                    Long.valueOf(TipoCarteraBanda.VENCIDO));
            BandaProductoDetalle bandaPorVencer = primeraBanda(bandas, idProducto,
                    Long.valueOf(TipoCarteraBanda.POR_VENCER));
            if (bandaVencido == null || bandaPorVencer == null) {
                // Sin las dos bandas no se puede armar el par: el aviso ya se emitio en la
                // distribucion. Saltar es preferible a emitir medio asiento.
                continue;
            }
            String nombre = nombreProducto(productos, idProducto);
            sub.getLineas().add(lineaBanda(bandaVencido, valor, 0D,
                    "Vencidos " + periodo(solicitud) + " - " + nombre,
                    idProducto, nombre, Long.valueOf(TipoCarteraBanda.VENCIDO)));
            sub.getLineas().add(lineaBanda(bandaPorVencer, 0D, valor,
                    "Vencidos " + periodo(solicitud) + " - " + nombre,
                    idProducto, nombre, Long.valueOf(TipoCarteraBanda.POR_VENCER)));
        }
        cierra(sub, "No hubo capital que venciera y quedara impago en el mes.");
        return sub;
    }

    // =====================================================================
    // Sub-procesos ② y ①.1 — reclasificación de bandas
    // =====================================================================

    /**
     * Reclasificación por diferencias de un tipo de cartera.
     *
     * <p>
     * La diferencia de cada banda es {@code nueva - anterior}, corregida por lo que el
     * sub-proceso ① ya movió: ① descarga la banda 1 de POR VENCER y carga la banda 1 de
     * VENCIDO por el mismo importe, así que aquí se le suma de vuelta a POR VENCER y se le
     * resta a VENCIDO. <b>Con esa corrección la suma de las diferencias de cada producto es
     * cero</b>, y el asiento cuadra sin ajustes.
     * </p>
     *
     * <p><b>Desde 2026-08-31, {@code distAnterior}/{@code distNueva} NO son siempre el mismo
     * par de fechas para las dos llamadas, y {@code vencidos} viaja emparejado con ellas.</b>
     * VENCIDO (①.1) sigue comparando el corte anterior contra el corte del mes que se cierra
     * (30-jun → 31-jul) con {@code vencidosDelMes} sobre ese mismo par; POR VENCER (②) compara
     * el corte del mes que se cierra contra el cierre del mes que se abre (31-jul → 31-ago,
     * pedido del usuario) con {@code vencidosDelMesApertura}, calculado sobre ESE par — nunca
     * el {@code vencidosDelMes} de ①.1. Ver el comentario de la corrección de banda 1 más
     * abajo para la razón (separa envejecimiento de vencimiento, no evita un doble asiento).</p>
     *
     * @param subProceso   : Código del sub-proceso
     * @param tipoCartera  : Tipo de cartera que se reclasifica
     * @param distAnterior : Distribución medida al corte anterior
     * @param distNueva    : Distribución medida al corte actual
     * @param vencidos     : Capital que ① movió, por producto
     * @param productos    : Productos indexados
     * @param fecha        : Fecha contable
     * @param solicitud    : Solicitud, para la glosa
     * @return             : El sub-proceso con sus líneas
     */
    private SubProcesoCierre armaCambioBandas(int subProceso, Long tipoCartera,
            Map<String, BandaSnapshotCierre> distAnterior,
            Map<String, BandaSnapshotCierre> distNueva, Map<Long, Double> vencidos,
            Map<Long, Producto> productos, LocalDate fecha, SolicitudCierreCartera solicitud)
            throws Throwable {

        boolean porVencer = tipoCartera.longValue() == TipoCarteraBanda.POR_VENCER;
        SubProcesoCierre sub = nuevoSubProceso(subProceso,
                porVencer ? "Cambio de bandas - cartera por vencer"
                          : "Reclasificacion - cartera vencida",
                porVencer ? "②" : "①.1", fecha,
                "CRD cierre de cartera " + periodo(solicitud) + " - reclasificacion de bandas "
                        + (porVencer ? "por vencer" : "vencidas"));

        // Union de las claves de las dos distribuciones: una banda que aparece en una y no
        // en la otra tambien produce diferencia.
        Map<String, BandaSnapshotCierre> universo = new TreeMap<String, BandaSnapshotCierre>();
        universo.putAll(distAnterior);
        universo.putAll(distNueva);

        for (Map.Entry<String, BandaSnapshotCierre> entrada : universo.entrySet()) {
            BandaSnapshotCierre referencia = entrada.getValue();
            if (!tipoCartera.equals(referencia.getTipoCartera())) {
                continue;
            }
            double anterior = capitalDe(distAnterior, entrada.getKey());
            double nueva = capitalDe(distNueva, entrada.getKey());
            double diferencia = nueva - anterior;

            // Separa, en la banda 1, capital que cambio de banda POR ENVEJECER (eso es
            // reclasificacion, lo que este asiento mide) de capital que salio de la banda 1
            // POR VENCER porque se vencio (eso NO es reclasificacion dentro de por-vencer).
            // Sin esta separacion, el diff bruto le atribuiria a "cambio de banda" un capital
            // que en realidad se vencio. `vencidos` tiene que venir calculado sobre el MISMO
            // PAR DE FECHAS que distAnterior/distNueva de ESTA llamada — si se cambian las
            // fechas del diff, este parametro se mueve junto, no queda fijo (2026-08-31: es
            // exactamente el error que casi se comete al mover ② a 31-jul->31-ago).
            if (referencia.getNumeroBanda() != null && referencia.getNumeroBanda().longValue() == 1L) {
                double movido = redondeaDouble(vencidos.get(referencia.getIdProducto()));
                diferencia = diferencia + (porVencer ? movido : -movido);
            }

            diferencia = redondeaDouble(Double.valueOf(diferencia));
            if (Math.abs(diferencia) < MINIMO_LINEA) {
                continue;
            }
            String nombre = nombreProducto(productos, referencia.getIdProducto());
            String descripcion = "Reclasificacion " + periodo(solicitud) + " - " + nombre
                    + " banda " + referencia.getNumeroBanda();
            LineaAsientoCierre linea = new LineaAsientoCierre();
            linea.setCuenta(referencia.getCuenta());
            linea.setNombreCuenta(referencia.getNombreCuenta());
            linea.setIdPlanCuenta(referencia.getIdPlanCuenta());
            linea.setDescripcion(descripcion);
            // La banda crece -> Debe; decrece -> Haber (§6.3).
            linea.setDebe(Double.valueOf(diferencia > 0 ? diferencia : 0D));
            linea.setHaber(Double.valueOf(diferencia < 0 ? -diferencia : 0D));
            linea.setIdProducto(referencia.getIdProducto());
            linea.setNombreProducto(nombre);
            linea.setTipoCartera(tipoCartera);
            linea.setNumeroBanda(referencia.getNumeroBanda());
            sub.getLineas().add(linea);
        }
        cierra(sub, "La distribucion por bandas no cambio respecto del mes anterior.");
        return sub;
    }

    // =====================================================================
    // Sub-proceso ③ — apertura del período
    // =====================================================================

    /**
     * ③ Apertura: genera las cuentas por cobrar del mes que se ABRE contra las cuentas por
     * aplicar. Factura hasta el último día del mes que se abre, no hasta el corte.
     *
     * <p><b>Aportes (2026-08-31, decisión FINAL del usuario, tercera fuente — resuelve la
     * contradicción de las dos anteriores).</b> No espera a que exista el archivo Petro del
     * mes que se abre (nunca existe todavía cuando corre esta apertura — el proceso real es
     * "se cierra el mes pasado / se abre el presente, y luego se carga Petro") y no usa la
     * obligación calculada desde los contratos ({@code selectAporteMensualEsperado}, que no
     * acumula mora y daba números por debajo de la realidad). Usa el MISMO ALGORITMO con el
     * que se genera el archivo real —
     * {@link com.saa.ejb.crd.service.GeneracionArchivoPetroService#calcularAportesEsperados}—,
     * sumado sobre TODAS las filiales, para el mes que se abre. Por construcción no puede
     * divergir del archivo que efectivamente se le manda a Petro: es el mismo cálculo, no una
     * copia — respeta por dentro el flag del rubro 242
     * ({@code ConfiguracionGeneracionAportesService#porFaltanteActiva()}, hoy apagado ⇒ usa
     * {@code HistorialSueldo} × meses adeudados; si se enciende, usa {@code CRD.VGCN} con el
     * faltante acumulado) sin que este método tenga que cambiar en ninguno de los dos casos.
     * Descartadas dos fuentes antes de esta: (1) {@code selectAporteMensualEsperado} filtra
     * {@code ENTDIDST}, pero se verificó que las tres fuentes filtran IGUAL — el problema real
     * era que esa no acumula mora; (2) leer directo {@code CRD.DTCA} del archivo cargado —
     * descartada porque el archivo del mes que se abre nunca existe todavía en ese momento.
     * {@code selectAporteMensualEsperado} sigue existiendo sin cambios para
     * {@code calculaDesgloseAportes} (de ahí a {@code armaNeteo}, {@code controlaArchivoPetro},
     * {@code avisaExcesoCobro}) — ese uso es sobre el MES QUE SE CIERRA y es un desglose
     * distinto, no se toca.</p>
     *
     * <p><b>Todas las filiales, SIN filtrar por estado</b> (decisión del usuario): una filial
     * inactiva con partícipes que todavía aportan no debe quedar afuera — sería reproducir el
     * mismo problema que se está arreglando. Una filial sin partícipes simplemente suma cero.</p>
     *
     * <p><b>Sensibilidad a la fecha de corrida (verificado, no es un riesgo):</b> el camino
     * viejo (hoy activo) multiplica por meses adeudados vía
     * {@code calcularMesesACobrarMorosos}, que deriva el corte de {@code mes}/{@code anio}
     * (el período que se calcula), NUNCA de {@code LocalDate.now()} — el resultado es
     * determinístico para un período dado, sin importar qué día del mes se corra el cierre.</p>
     *
     * @param solicitud          : Solicitud
     * @param fecha              : Fecha contable (primer día del mes que se abre) — TAMBIÉN
     *                             la fuente de mes/año de {@code calcularAportesEsperados};
     *                             nunca {@code fechaCorteApertura} ni {@code fechaCorte}.
     * @param fechaCorteApertura : Último día del mes que se abre (solo para el lado préstamos)
     * @return                   : El sub-proceso con sus líneas
     */
    private SubProcesoCierre armaApertura(SolicitudCierreCartera solicitud, LocalDate fecha,
            LocalDate fechaCorteApertura) throws Throwable {

        SubProcesoCierre sub = nuevoSubProceso(SubProcesoCierreCartera.APERTURA,
                "Apertura del periodo de credito", "③", fecha,
                "CRD apertura de cartera " + fecha.getYear() + "-"
                        + dosDigitos(Long.valueOf(fecha.getMonthValue())));

        // 2026-08-31, decisión FINAL del usuario (ver el javadoc del método): el esperado de
        // aportes de ③ usa el MISMO algoritmo de la generación real del archivo Petro,
        // sumado sobre TODAS las filiales, para el MES QUE SE ABRE. "fecha" es fechaProceso
        // (01 del mes que se abre) — mes/año salen de ahí, NUNCA de
        // fechaCorte/fechaCorteApertura. selectAporteMensualEsperado sigue existiendo para
        // calculaDesgloseAportes/armaNeteo/controlaArchivoPetro/avisaExcesoCobro — no se toca.
        Long mesApertura = Long.valueOf(fecha.getMonthValue());
        Long anioApertura = Long.valueOf(fecha.getYear());
        double totalAportes = 0.0;
        for (Filial filial : filialDaoService.selectAll(NombreEntidadesCredito.FILIAL)) {
            Map<String, Double> aportesFilial = generacionArchivoPetroService.calcularAportesEsperados(
                    mesApertura, anioApertura, filial.getCodigo());
            totalAportes += aportesFilial.getOrDefault("total", 0.0);
        }
        totalAportes = redondeaDouble(totalAportes);

        double totalPrestamos = 0D;
        for (Object[] fila : cierreCarteraDaoService
                .selectCobrablePrestamosHasta(fechaCorteApertura)) {
            for (int i = 1; i <= 5; i++) {
                totalPrestamos = totalPrestamos + ((Double) fila[i]).doubleValue();
            }
        }
        totalPrestamos = redondeaDouble(Double.valueOf(totalPrestamos));

        Long idPlantilla = resuelvePlantilla(PlantillasCredito.APERTURA_PLANILLA_MENSUAL,
                "apertura del periodo", solicitud.getIdEmpresa());
        agrega(sub, idPlantilla, CrdLineaAsiento.APORTES_POR_COBRAR, totalAportes,
                "Aportes personales y patronales por cobrar");
        agrega(sub, idPlantilla, CrdLineaAsiento.PRESTAMOS_POR_COBRAR, totalPrestamos,
                "Cuotas de prestamos por cobrar del mes, todos los valores");
        agrega(sub, idPlantilla, CrdLineaAsiento.APORTES_POR_APLICAR, totalAportes,
                "Aportes por aplicar");
        agrega(sub, idPlantilla, CrdLineaAsiento.PRESTAMOS_POR_APLICAR, totalPrestamos,
                "Prestamos por aplicar");

        cierra(sub, "No hay aportes ni cuotas por cobrar en el mes que se abre.");
        cuadra(sub, CrdLineaAsiento.PRESTAMOS_POR_APLICAR);
        return sub;
    }

    // =====================================================================
    // Sub-proceso ④ — devengo de intereses
    // =====================================================================

    /**
     * ④ Devengo de intereses ordinarios y de mora, por familia de producto, de las cuotas con
     * vencimiento EN EL MES QUE SE ABRE — no las acumuladas de meses anteriores (decisión del
     * usuario, 2026-08-31: "solo los intereses de las cuotas del mes de apertura... son esos
     * los que se mandan a ingresos"). {@code desdeRango}/{@code hastaRango} son un parámetro
     * DISTINTO de {@code fecha} (la fecha del asiento) a propósito — reusar un solo parámetro
     * para las dos cosas es exactamente la confusión que casi se comete con el asiento ②.
     *
     * <p>
     * La mora la calcula a diario {@code ProcesoMoraPrestamoService} y se acumula en
     * {@code DTPRMRAA}; aquí se devenga lo que quede pendiente de esas cuotas, que es lo que
     * contabilidad necesita a fin de mes (decisión C5 de §9.1). Las dos líneas comparten
     * cuenta con el ordinario y se distinguen por la DESCRIPCIÓN (decisión D3). La resta de
     * pagos ({@code - NVL(g.intr,0)}, {@code - NVL(g.mora,0)}) dentro de la consulta no cambió:
     * una cuota del mes que se abre puede tener un pago anticipado, y lo devengado es lo no
     * cobrado.
     * </p>
     *
     * @param solicitud  : Solicitud
     * @param fecha      : Fecha contable del asiento (NO es el rango de cuotas)
     * @param desdeRango : Primer día del mes que se abre
     * @param hastaRango : Último día del mes que se abre
     * @return           : El sub-proceso con sus líneas
     */
    private SubProcesoCierre armaDevengoIntereses(SolicitudCierreCartera solicitud,
            LocalDate fecha, LocalDate desdeRango, LocalDate hastaRango) throws Throwable {

        SubProcesoCierre sub = nuevoSubProceso(SubProcesoCierreCartera.DEVENGO_INTERESES,
                "Devengo de intereses a ingresos", "④", fecha,
                "CRD devengado de interes a ingresos " + periodo(solicitud));

        Long idPlantilla = resuelvePlantilla(PlantillasCredito.DEVENGO_INTERESES,
                "devengo de intereses", solicitud.getIdEmpresa());

        for (Object[] fila : cierreCarteraDaoService
                .selectInteresPorTipoPrestamoEnRango(desdeRango, hastaRango)) {
            Long idTipoPrestamo = (Long) fila[0];
            double interes = redondeaDouble((Double) fila[1]);
            double mora = redondeaDouble((Double) fila[2]);

            agregaPorTipo(sub, idPlantilla, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR,
                    idTipoPrestamo, interes, "INTERES ORDINARIO por cobrar " + periodo(solicitud));
            agregaPorTipo(sub, idPlantilla, CrdLineaAsiento.INTERES_MORA_POR_COBRAR,
                    idTipoPrestamo, mora, "INTERES POR MORA por cobrar " + periodo(solicitud));
            agregaPorTipo(sub, idPlantilla, CrdLineaAsiento.INGRESO_INTERES_ORDINARIO,
                    idTipoPrestamo, interes, "Ingreso por INTERES ORDINARIO " + periodo(solicitud));
            agregaPorTipo(sub, idPlantilla, CrdLineaAsiento.INGRESO_INTERES_MORA,
                    idTipoPrestamo, mora, "Ingreso por INTERES POR MORA " + periodo(solicitud));
        }

        cierra(sub, "No hay interes ni mora pendientes de devengar al corte.");
        cuadra(sub, CrdLineaAsiento.INGRESO_INTERES_ORDINARIO);
        return sub;
    }

    // =====================================================================
    // Sub-proceso ⑥ — neteo de planillas
    // =====================================================================

    /**
     * ⑥ Neteo: reversa lo NO cobrado del mes cerrado. Se fecha el último día del mes
     * cerrado, no el día de proceso.
     *
     * <p>
     * <b>El lado de los aportes: el asiento lleva SOLO lo no cobrado; el exceso NO entra
     * acá</b> (decisión del usuario, 2026-08-31, opción b). Lo no cobrado de préstamos sale
     * del saldo real de las cuotas, que es exacto. Para aportes no hay un documento de
     * "planilla de aportes emitida" contra el que comparar, así que se toma
     * {@code esperado - registrado en el mes} sobre los tipos 9 (jubilación) y 11
     * (cesantía) — {@code esperado} sale de {@link #calculaDesgloseAportes}, la MISMA fuente
     * que el asiento ③ (para el mes que se cierra, no el que se abre). Si esa diferencia es
     * NEGATIVA (se cobró de más), el piso en cero de {@code DesgloseAportesCierre.noCobrado}
     * la deja en $0 en el asiento — un neteo negativo invertiría el asiento, y no es lo que
     * se pidió. <b>El exceso no desaparece: antes se calculaba y no se mostraba en ningún
     * lado; ahora {@link #avisaExcesoCobro} lo deja como advertencia visible en la
     * previsualización</b>, con el monto — se resuelve por el proceso de cobro en exceso, no
     * por este asiento.
     * </p>
     *
     * @param solicitud : Solicitud
     * @param corte     : Último día del mes cerrado
     * @return          : El sub-proceso con sus líneas
     */
    private SubProcesoCierre armaNeteo(SolicitudCierreCartera solicitud, LocalDate corte,
            DesgloseAportesCierre desglose) throws Throwable {

        SubProcesoCierre sub = nuevoSubProceso(SubProcesoCierreCartera.NETEO,
                "Neteo de planillas", "⑥", corte,
                "CRD neteo de planillas " + periodo(solicitud));

        double noCobradoPrestamos = 0D;
        for (Object[] fila : cierreCarteraDaoService.selectCobrablePrestamosHasta(corte)) {
            for (int i = 1; i <= 5; i++) {
                noCobradoPrestamos = noCobradoPrestamos + ((Double) fila[i]).doubleValue();
            }
        }
        noCobradoPrestamos = redondeaDouble(Double.valueOf(noCobradoPrestamos));

        // El desglose ya trae el piso en cero aplicado y, aparte, el exceso de cobro que ese
        // piso oculta. El asiento lleva solo noCobrado; el exceso lo resuelve el proceso de
        // cobro en exceso (§3.7).
        double noCobradoAportes = redondeaDouble(desglose.getNoCobrado());

        Long idPlantilla = resuelvePlantilla(PlantillasCredito.NETEO_PLANILLAS,
                "neteo de planillas", solicitud.getIdEmpresa());
        agrega(sub, idPlantilla, CrdLineaAsiento.APORTES_POR_APLICAR, noCobradoAportes,
                "Aportes no cobrados " + periodo(solicitud));
        agrega(sub, idPlantilla, CrdLineaAsiento.PRESTAMOS_POR_APLICAR, noCobradoPrestamos,
                "Cuotas de prestamos no cobradas " + periodo(solicitud));
        agrega(sub, idPlantilla, CrdLineaAsiento.APORTES_POR_COBRAR, noCobradoAportes,
                "Aportes no cobrados " + periodo(solicitud));
        agrega(sub, idPlantilla, CrdLineaAsiento.PRESTAMOS_POR_COBRAR, noCobradoPrestamos,
                "Cuotas de prestamos no cobradas " + periodo(solicitud));

        cierra(sub, "No quedo nada por cobrar del mes cerrado.");
        cuadra(sub, CrdLineaAsiento.PRESTAMOS_POR_COBRAR);
        return sub;
    }

    // =====================================================================
    // Control de archivo Petro y desglose de aportes (decisión D13)
    // =====================================================================

    /**
     * Calcula de dónde sale el importe de aportes del neteo: esperado, registrado y las dos
     * lecturas de su diferencia.
     *
     * <p><b>Esperado (2026-08-31, decisión FINAL del usuario): MISMA fuente que el asiento ③
     * — {@code GeneracionArchivoPetroService#calcularAportesEsperados}, sumado sobre TODAS
     * las filiales, sin filtrar por estado — pero para el MES QUE SE CIERRA (a diferencia del
     * ③, que es del mes que se abre).</b> Antes salía de {@code selectAporteMensualEsperado}
     * (mismo defecto que llevó a cambiar el ③: no acumula mora, da números por debajo de la
     * realidad — julio registró $156.797 contra $121.161 "esperados", una diferencia
     * NEGATIVA que el piso en cero escondía por completo). Que el ⑥ y el ③ usen la MISMA
     * fuente no es una preferencia de estilo: es lo que garantiza que el ⑥ del mes M+1
     * "blanquea" exactamente el número que el ③ del mes M abrió — si usaran fuentes
     * distintas, la cuenta de apertura nunca cerraría en cero y quedaría un residuo que nadie
     * podría explicar. {@code selectAporteMensualEsperado} sigue existiendo (no se borra,
     * puede tener otros usos futuros) pero ya no lo llama nada acá.</p>
     *
     * @param corte : Último día del mes cerrado
     * @return      : El desglose, con el piso en cero ya aplicado en {@code noCobrado}
     */
    private DesgloseAportesCierre calculaDesgloseAportes(LocalDate corte) throws Throwable {
        Long mesCierre = Long.valueOf(corte.getMonthValue());
        Long anioCierre = Long.valueOf(corte.getYear());
        double esperadoJubilacion = 0.0;
        double esperadoCesantia = 0.0;
        long participantes = 0L;
        for (Filial filial : filialDaoService.selectAll(NombreEntidadesCredito.FILIAL)) {
            Map<String, Double> aportesFilial = generacionArchivoPetroService.calcularAportesEsperados(
                    mesCierre, anioCierre, filial.getCodigo());
            esperadoJubilacion += aportesFilial.getOrDefault("jubilacion", 0.0);
            esperadoCesantia += aportesFilial.getOrDefault("cesantia", 0.0);
            participantes += aportesFilial.getOrDefault("participantes", 0.0).longValue();
        }

        LocalDate desde = corte.withDayOfMonth(1);
        Object[] registrados = cierreCarteraDaoService.selectAportesRegistrados(desde, corte);

        double registradoJubilacion = ((Double) registrados[0]).doubleValue();
        double registradoCesantia = ((Double) registrados[1]).doubleValue();
        double esperado = esperadoJubilacion + esperadoCesantia;
        double registrado = registradoJubilacion + registradoCesantia;
        double diferencia = esperado - registrado;

        DesgloseAportesCierre desglose = new DesgloseAportesCierre();
        desglose.setEsperado(redondea(Double.valueOf(esperado)));
        desglose.setEsperadoJubilacion(redondea(Double.valueOf(esperadoJubilacion)));
        desglose.setEsperadoCesantia(redondea(Double.valueOf(esperadoCesantia)));
        desglose.setParticipes(Long.valueOf(participantes));
        desglose.setRegistrado(redondea(Double.valueOf(registrado)));
        desglose.setRegistradoJubilacion(redondea(Double.valueOf(registradoJubilacion)));
        desglose.setRegistradoCesantia(redondea(Double.valueOf(registradoCesantia)));
        desglose.setDiferencia(redondea(Double.valueOf(diferencia)));
        desglose.setNoCobrado(redondea(Double.valueOf(Math.max(diferencia, 0D))));
        desglose.setExcesoCobro(redondea(Double.valueOf(Math.max(-diferencia, 0D))));
        desglose.setDesde(desde);
        desglose.setHasta(corte);
        return desglose;
    }

    /**
     * Comprueba que el archivo Petro del mes que se cierra esté cargado y PROCESADO
     * (decisión D13, 2026-08-25).
     *
     * <p>
     * <b>Por qué es bloqueante.</b> Los aportes registrados del mes son filas de
     * {@code CRD.APRT} que crea la fase 3 de la carga. Sin archivo cargado no hay aportes
     * registrados, y el neteo reversa como no cobrado un dinero que sí se cobró. Medido en
     * la BD el 2026-08-25: agosto tenía 5.499,75 registrados contra 121.160,97 esperados
     * porque el archivo del mes 8 no estaba cargado — 115.661,22 se habrían reversado de más.
     * </p>
     *
     * <p>
     * Este método NO lanza: devuelve el diagnóstico con {@code bloquea}. Previsualizar
     * siempre deja ver el estado; el que corta es {@code ejecutar}.
     * </p>
     *
     * @param solicitud    : Solicitud, de donde salen el período y la posible omisión
     * @param desglose     : Desglose de aportes, para poder decir cuánto está en juego
     * @param advertencias : Lista donde anotar el aviso
     * @return             : El diagnóstico del control
     */
    private ControlArchivoPetro controlaArchivoPetro(SolicitudCierreCartera solicitud,
            DesgloseAportesCierre desglose, List<String> advertencias) throws Throwable {

        boolean omitir = Boolean.TRUE.equals(solicitud.getOmitirControlArchivoPetro());

        ControlArchivoPetro control = new ControlArchivoPetro();
        control.setAnio(solicitud.getAnio());
        control.setMes(solicitud.getMes());
        control.setOmitido(Boolean.valueOf(omitir));
        control.setMotivoOmision(omitir ? solicitud.getMotivoOmisionControl() : null);

        List<CargaArchivo> cargas = cargaArchivoDaoService.selectByPeriodoAfectacionYEstado(
                solicitud.getAnio(), solicitud.getMes(),
                Long.valueOf(CrdEstadoCargaArchivo.PROCESADO));

        control.setCargasEncontradas(Long.valueOf(cargas.size()));
        control.setExiste(Boolean.valueOf(!cargas.isEmpty()));

        if (!cargas.isEmpty()) {
            CargaArchivo carga = cargas.get(0);
            control.setIdCarga(carga.getCodigo());
            control.setNombreArchivo(carga.getNombre());
            control.setFechaCarga(carga.getFechaCarga() != null
                    ? carga.getFechaCarga().toLocalDate() : null);
            control.setEstadoCarga(carga.getEstado());
            for (CargaArchivo fila : cargas) {
                if (fila.getFilial() != null && fila.getFilial().getCodigo() != null
                        && !control.getFiliales().contains(fila.getFilial().getCodigo())) {
                    control.getFiliales().add(fila.getFilial().getCodigo());
                }
            }
            control.setBloquea(Boolean.FALSE);
            return control;
        }

        control.setBloquea(Boolean.valueOf(!omitir));
        String enJuego = "El aporte esperado del mes es " + desglose.getEsperado()
                + " y solo hay " + desglose.getRegistrado() + " registrados: el neteo"
                + " reversaria " + desglose.getNoCobrado() + " como no cobrado.";
        if (omitir) {
            advertencias.add("CONTROL DE ARCHIVO PETRO OMITIDO para " + periodo(solicitud)
                    + " por decision del usuario. Motivo: " + solicitud.getMotivoOmisionControl()
                    + ". " + enJuego);
        } else {
            advertencias.add("No hay archivo Petro cargado y procesado con mes de afectacion "
                    + solicitud.getMes() + " de " + solicitud.getAnio()
                    + ": el lado aportes del neteo esta INCOMPLETO. " + enJuego
                    + " La ejecucion va a rechazar el cierre.");
        }
        return control;
    }

    /**
     * Avisa cuando se cobró MÁS de lo esperado. El asiento no puede llevar un neteo
     * negativo, así que el piso en cero se come esa diferencia; que no aparezca en el
     * asiento no significa que no exista.
     *
     * @param desglose     : Desglose de aportes
     * @param advertencias : Lista donde anotar el aviso
     * @param solicitud    : Solicitud, para el período del mensaje
     */
    private void avisaExcesoCobro(DesgloseAportesCierre desglose, List<String> advertencias,
            SolicitudCierreCartera solicitud) {
        if (desglose.getExcesoCobro() == null
                || desglose.getExcesoCobro().doubleValue() < MINIMO_LINEA) {
            return;
        }
        advertencias.add("En " + periodo(solicitud) + " se registraron "
                + desglose.getRegistrado() + " de aportes contra " + desglose.getEsperado()
                + " esperados: hay un exceso de cobro de " + desglose.getExcesoCobro()
                + ". El neteo no lo lleva (un neteo negativo invertiria el asiento); lo"
                + " resuelve el proceso de cobro en exceso.");
    }

    /**
     * Aplica el bloqueo del control antes de grabar. Es el único punto donde el control
     * detiene algo.
     *
     * @param control   : Diagnóstico del control
     * @param solicitud : Solicitud, para el mensaje
     */
    private void exigeArchivoPetro(ControlArchivoPetro control, SolicitudCierreCartera solicitud,
            DesgloseAportesCierre desglose) {
        if (control == null || !Boolean.TRUE.equals(control.getBloquea())) {
            return;
        }
        throw new IncomeException("No se puede cerrar " + periodo(solicitud)
                + ": no hay archivo Petro cargado con mes de afectacion " + solicitud.getMes()
                + " de " + solicitud.getAnio() + " en estado PROCESADO. Sin el, los aportes del"
                + " mes no estan registrados: se esperan " + desglose.getEsperado()
                + " y solo hay " + desglose.getRegistrado() + ", asi que el neteo reversaria "
                + desglose.getNoCobrado() + " como no cobrado siendo cobrable. Cargue el archivo"
                + " del mes antes de ejecutar el cierre, o repita con"
                + " omitirControlArchivoPetro = true y un motivo si ese mes no tuvo archivo.");
    }

    // =====================================================================
    // Plantillas y líneas
    // =====================================================================

    /**
     * Agrega al sub-proceso la línea de la plantilla identificada por su papel.
     *
     * @param sub          : Sub-proceso
     * @param idPlantilla  : Plantilla resuelta
     * @param codigoLinea  : Papel de la línea ({@link CrdLineaAsiento})
     * @param valor        : Importe, ya redondeado
     * @param descripcion  : Descripción de la línea del asiento
     */
    private void agrega(SubProcesoCierre sub, Long idPlantilla, int codigoLinea, double valor,
            String descripcion) throws Throwable {
        if (valor < MINIMO_LINEA) {
            return;
        }
        DetallePlantilla plantilla =
                detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, codigoLinea);
        if (plantilla == null) {
            throw new IncomeException("La plantilla contable de " + sub.getNombre()
                    + " no define la linea " + codigoLinea + " del catalogo CRD_LINEA_ASIENTO."
                    + " Revise docs/logica-negocio/crd/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md.");
        }
        sub.getLineas().add(lineaPlantilla(plantilla, valor, descripcion, codigoLinea));
    }

    /**
     * Agrega la línea de la plantilla identificada por su papel MÁS el tipo de préstamo.
     *
     * @param sub            : Sub-proceso
     * @param idPlantilla    : Plantilla resuelta
     * @param codigoLinea    : Papel de la línea
     * @param idTipoPrestamo : Tipo de préstamo (auxiliar2)
     * @param valor          : Importe
     * @param descripcion    : Descripción
     */
    private void agregaPorTipo(SubProcesoCierre sub, Long idPlantilla, int codigoLinea,
            Long idTipoPrestamo, double valor, String descripcion) throws Throwable {
        if (valor < MINIMO_LINEA) {
            return;
        }
        if (idTipoPrestamo == null) {
            throw new IncomeException("Hay cuotas con interes pendiente cuyo producto no tiene"
                    + " tipo de prestamo asignado: no se puede resolver la cuenta contable.");
        }
        DetallePlantilla plantilla = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                idPlantilla, codigoLinea, idTipoPrestamo.intValue());
        if (plantilla == null) {
            throw new IncomeException("La plantilla contable de " + sub.getNombre()
                    + " no define la linea " + codigoLinea + " para el tipo de prestamo "
                    + idTipoPrestamo + ". Revise"
                    + " docs/logica-negocio/crd/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md.");
        }
        sub.getLineas().add(lineaPlantilla(plantilla, valor, descripcion, codigoLinea));
    }

    /**
     * Construye una línea de asiento a partir de su definición en la plantilla.
     *
     * @param plantilla   : Línea de la plantilla
     * @param valor       : Importe
     * @param descripcion : Descripción
     * @param codigoLinea : Papel de la línea
     * @return            : La línea
     */
    private LineaAsientoCierre lineaPlantilla(DetallePlantilla plantilla, double valor,
            String descripcion, int codigoLinea) throws Throwable {
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        if (cuenta == null) {
            throw new IncomeException("La linea " + codigoLinea + " de la plantilla no tiene"
                    + " cuenta contable asignada.");
        }
        boolean debe = plantilla.getMovimiento() != null
                && plantilla.getMovimiento().longValue() == MOVIMIENTO_DEBE;
        LineaAsientoCierre linea = new LineaAsientoCierre();
        linea.setCuenta(cuenta.getCuentaContable());
        linea.setNombreCuenta(cuenta.getNombre());
        linea.setIdPlanCuenta(cuenta.getCodigo());
        linea.setDescripcion(descripcion);
        linea.setDebe(Double.valueOf(debe ? valor : 0D));
        linea.setHaber(Double.valueOf(debe ? 0D : valor));
        linea.setCodigoLinea(Long.valueOf(codigoLinea));
        return linea;
    }

    /**
     * Construye una línea cuya cuenta viene de la parametrización de bandas.
     *
     * @param banda       : Banda con su cuenta ya resuelta
     * @param debe        : Importe al debe
     * @param haber       : Importe al haber
     * @param descripcion : Descripción
     * @param idProducto  : Producto
     * @param nombre      : Nombre del producto
     * @param tipoCartera : Tipo de cartera
     * @return            : La línea
     */
    private LineaAsientoCierre lineaBanda(BandaProductoDetalle banda, double debe, double haber,
            String descripcion, Long idProducto, String nombre, Long tipoCartera) {
        LineaAsientoCierre linea = new LineaAsientoCierre();
        linea.setCuenta(banda.getCuentaContable());
        linea.setNombreCuenta(banda.getNombreCuenta());
        linea.setIdPlanCuenta(banda.getIdPlanCuenta());
        linea.setDescripcion(descripcion);
        linea.setDebe(Double.valueOf(debe));
        linea.setHaber(Double.valueOf(haber));
        linea.setIdProducto(idProducto);
        linea.setNombreProducto(nombre);
        linea.setTipoCartera(tipoCartera);
        linea.setNumeroBanda(banda.getNumero());
        return linea;
    }

    /**
     * Traduce el código alterno de una plantilla a su id.
     *
     * @param alterno   : Código alterno ({@link PlantillasCredito})
     * @param etiqueta  : Nombre del asiento, para el mensaje
     * @param idEmpresa : Empresa
     * @return          : Id de la plantilla
     */
    private Long resuelvePlantilla(int alterno, String etiqueta, Long idEmpresa)
            throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(alterno, idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable con codigo alterno "
                    + alterno + " (" + etiqueta + ") para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    // =====================================================================
    // Cuadre
    // =====================================================================

    /**
     * Cierra el sub-proceso: calcula los totales y lo marca omitido si quedó sin líneas.
     *
     * @param sub    : Sub-proceso
     * @param motivo : Motivo de la omisión
     */
    private void cierra(SubProcesoCierre sub, String motivo) {
        double debe = 0D;
        double haber = 0D;
        for (LineaAsientoCierre linea : sub.getLineas()) {
            debe = debe + (linea.getDebe() != null ? linea.getDebe().doubleValue() : 0D);
            haber = haber + (linea.getHaber() != null ? linea.getHaber().doubleValue() : 0D);
        }
        sub.setTotalDebe(redondea(Double.valueOf(debe)));
        sub.setTotalHaber(redondea(Double.valueOf(haber)));
        sub.setOmitido(Boolean.valueOf(sub.getLineas().isEmpty()));
        if (sub.getLineas().isEmpty()) {
            sub.setMotivoOmision(motivo);
        }
    }

    /**
     * Comprueba el cuadre y ajusta la diferencia de centavos contra la línea indicada, como
     * hace RHH. Una diferencia mayor a la tolerancia NO se ajusta: se rechaza con el importe
     * exacto, porque a esa altura ya no es redondeo.
     *
     * @param sub               : Sub-proceso, ya cerrado
     * @param codigoLineaCuadre : Papel de la línea contra la que se ajusta
     */
    private void cuadra(SubProcesoCierre sub, int codigoLineaCuadre) throws Throwable {
        if (Boolean.TRUE.equals(sub.getOmitido())) {
            return;
        }
        double diferencia = sub.getTotalDebe().doubleValue() - sub.getTotalHaber().doubleValue();
        if (Math.abs(diferencia) < 0.005D) {
            return;
        }
        if (Math.abs(diferencia) > TOLERANCIA_CUADRE) {
            throw new IncomeException("El asiento de " + sub.getNombre() + " no cuadra: DEBE "
                    + sub.getTotalDebe() + " y HABER " + sub.getTotalHaber() + ", diferencia "
                    + redondea(Double.valueOf(diferencia)) + ". Supera la tolerancia de cuadre ("
                    + TOLERANCIA_CUADRE + ") y no se emite.");
        }
        for (LineaAsientoCierre linea : sub.getLineas()) {
            if (linea.getCodigoLinea() != null
                    && linea.getCodigoLinea().longValue() == codigoLineaCuadre) {
                if (linea.getHaber() != null && linea.getHaber().doubleValue() > 0D) {
                    linea.setHaber(redondea(Double.valueOf(
                            linea.getHaber().doubleValue() + diferencia)));
                } else {
                    linea.setDebe(redondea(Double.valueOf(
                            linea.getDebe().doubleValue() - diferencia)));
                }
                System.out.println("Cuadre por redondeo de " + diferencia + " ajustado contra la"
                        + " linea " + codigoLineaCuadre + " de " + sub.getNombre() + ".");
                cierra(sub, null);
                return;
            }
        }
        throw new IncomeException("El asiento de " + sub.getNombre() + " difiere en "
                + redondea(Double.valueOf(diferencia)) + " por redondeo, pero la linea de cuadre "
                + codigoLineaCuadre + " no esta entre sus lineas.");
    }

    /**
     * Comprueba el cuadre de un sub-proceso que NO tiene línea de cuadre —los de bandas,
     * cuyas líneas salen todas de la parametrización—. Ahí la diferencia no se ajusta: si
     * no cuadra es un defecto del cálculo y hay que verlo, no taparlo.
     *
     * @param sub : Sub-proceso
     */
    private void exigeCuadre(SubProcesoCierre sub) throws Throwable {
        if (Boolean.TRUE.equals(sub.getOmitido())) {
            return;
        }
        double diferencia = sub.getTotalDebe().doubleValue() - sub.getTotalHaber().doubleValue();
        if (Math.abs(diferencia) >= 0.005D) {
            throw new IncomeException("El asiento de " + sub.getNombre() + " no cuadra: DEBE "
                    + sub.getTotalDebe() + " y HABER " + sub.getTotalHaber() + ", diferencia "
                    + redondea(Double.valueOf(diferencia)) + ". Las lineas de banda salen de la"
                    + " parametrizacion y deben cuadrar por construccion: revise que la"
                    + " configuracion de bandas cubra todos los productos con cartera.");
        }
    }

    // =====================================================================
    // Grabado
    // =====================================================================

    /**
     * Graba el snapshot de la corrida.
     *
     * @param corrida   : Corrida ya grabada
     * @param calculo   : Cálculo
     * @param solicitud : Solicitud
     * @param ahora     : Sello de auditoría
     */
    private void grabaSnapshot(CorridaCierreCartera corrida, CierreCartera calculo,
            SolicitudCierreCartera solicitud, LocalDateTime ahora) throws Throwable {
        for (BandaSnapshotCierre fila : calculo.getSnapshot()) {
            BandaCierreCartera registro = new BandaCierreCartera();
            registro.setCorrida(corrida);
            registro.setProducto(recuperaProducto(fila.getIdProducto()));
            registro.setTipoCartera(fila.getTipoCartera());
            registro.setBanda(bandaProductoDaoService.selectById(fila.getIdBanda(),
                    NombreEntidadesCredito.BANDA_PRODUCTO));
            registro.setNumero(fila.getNumeroBanda());
            registro.setPlanCuenta(registro.getBanda().getPlanCuenta());
            registro.setCapital(redondea(fila.getCapital()));
            registro.setCantidad(fila.getCantidad());
            registro.setEstado(Long.valueOf(Estado.ACTIVO));
            registro.setUsuarioRegistro(solicitud.getUsuario());
            registro.setIpRegistro(solicitud.getIp());
            registro.setFechaRegistro(ahora);
            bandaCierreCarteraDaoService.save(registro, null);
        }
    }

    /**
     * Genera el asiento de un sub-proceso y deja su registro en {@code CRD.ANCC}.
     *
     * @param corrida   : Corrida
     * @param sub       : Sub-proceso con sus líneas
     * @param solicitud : Solicitud
     * @param ahora     : Sello de auditoría
     */
    private void generaAsiento(CorridaCierreCartera corrida, SubProcesoCierre sub,
            SolicitudCierreCartera solicitud, LocalDateTime ahora) throws Throwable {

        // Flag global de contabilidad de CRD (D10): apagado, el cierre corre y calcula
        // igual pero no toca CNT. No es un error: se deja dicho en el resultado y el log.
        if (!configuracionContabilidadService.contabilidadActiva()) {
            System.out.println("CierreCarteraServiceImpl.generaAsiento - contabilidad de CRD "
                    + "INACTIVA (rubro " + Rubros.CRD_PARAMETROS_CONTABILIDAD + "): no se genera "
                    + "el asiento de " + sub.getNombre());
            sub.setOmitido(Boolean.TRUE);
            sub.setMotivoOmision(concatena(sub.getMotivoOmision(),
                    "Contabilidad de CRD desactivada: no se genero el asiento."));
            return;
        }

        // Ultima comprobacion antes de tocar contabilidad: generarAsiento tambien valida el
        // cuadre, pero su mensaje enumera lineas sin decir de que sub-proceso salieron.
        exigeCuadre(sub);

        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
        for (LineaAsientoCierre origen : sub.getLineas()) {
            DetalleAsiento detalle = new DetalleAsiento();
            detalle.setPlanCuenta(recuperaCuenta(origen.getIdPlanCuenta()));
            detalle.setNumeroCuenta(origen.getCuenta());
            detalle.setNombreCuenta(origen.getNombreCuenta());
            detalle.setDescripcion(origen.getDescripcion());
            detalle.setValorDebe(origen.getDebe() != null ? origen.getDebe() : Double.valueOf(0D));
            detalle.setValorHaber(origen.getHaber() != null ? origen.getHaber() : Double.valueOf(0D));
            lineas.add(detalle);
        }

        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsiento(
                solicitud.getIdEmpresa(), TipoAsientos.CREDITOS, sub.getFecha(), sub.getGlosa(),
                solicitud.getUsuario(), lineas,
                Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        sub.setIdAsiento(asiento.getCodigo());
        sub.setNumeroAsiento(asiento.getNumero() != null ? asiento.getNumero().toString() : null);

        AsientoCierreCartera registro = new AsientoCierreCartera();
        registro.setCorrida(corrida);
        registro.setSubProceso(sub.getSubProceso());
        registro.setAsiento(asiento.getCodigo());
        registro.setNumeroAsiento(sub.getNumeroAsiento());
        registro.setFecha(sub.getFecha());
        registro.setValor(sub.getTotalDebe());
        registro.setCantidad(Long.valueOf(sub.getLineas().size()));
        registro.setIdEstado(Long.valueOf(EstadoAsiento.ACTIVO));
        registro.setEstado(Long.valueOf(Estado.ACTIVO));
        registro.setUsuarioRegistro(solicitud.getUsuario());
        registro.setIpRegistro(solicitud.getIp());
        registro.setFechaRegistro(ahora);
        asientoCierreCarteraDaoService.save(registro, null);
    }

    /**
     * Arma el DTO de una corrida a partir de lo GRABADO, sin recalcular.
     *
     * @param corrida : Corrida
     * @return        : La corrida tal como quedó
     */
    private CierreCartera armaDesdeGrabado(CorridaCierreCartera corrida) throws Throwable {
        CierreCartera resultado = new CierreCartera();
        resultado.setIdCorrida(corrida.getCodigo());
        resultado.setIdEmpresa(corrida.getEmpresa() != null
                ? corrida.getEmpresa().getCodigo() : null);
        resultado.setAnio(corrida.getAnio());
        resultado.setMes(corrida.getMes());
        resultado.setFechaCorte(corrida.getFechaCorte());
        resultado.setFechaProceso(corrida.getFechaProceso());
        resultado.setFechaCorteApertura(corrida.getFechaProceso() != null
                ? corrida.getFechaProceso().withDayOfMonth(
                        corrida.getFechaProceso().lengthOfMonth()) : null);
        resultado.setIdEstado(corrida.getIdEstado());
        resultado.setNombreEstado(nombreEstado(corrida.getIdEstado()));

        double capitalTotal = 0D;
        for (BandaCierreCartera fila : bandaCierreCarteraDaoService
                .selectByCorrida(corrida.getCodigo())) {
            BandaSnapshotCierre item = new BandaSnapshotCierre();
            item.setIdProducto(fila.getProducto() != null ? fila.getProducto().getCodigo() : null);
            item.setNombreProducto(fila.getProducto() != null ? fila.getProducto().getNombre() : null);
            item.setTipoCartera(fila.getTipoCartera());
            item.setNombreTipoCartera(nombreTipoCartera(fila.getTipoCartera()));
            item.setIdBanda(fila.getBanda() != null ? fila.getBanda().getCodigo() : null);
            item.setNumeroBanda(fila.getNumero());
            if (fila.getPlanCuenta() != null) {
                item.setIdPlanCuenta(fila.getPlanCuenta().getCodigo());
                item.setCuenta(fila.getPlanCuenta().getCuentaContable());
                item.setNombreCuenta(fila.getPlanCuenta().getNombre());
            }
            item.setCapital(fila.getCapital());
            item.setCantidad(fila.getCantidad());
            resultado.getSnapshot().add(item);
            capitalTotal = capitalTotal + (fila.getCapital() != null
                    ? fila.getCapital().doubleValue() : 0D);
        }
        resultado.setCapitalTotal(redondea(Double.valueOf(capitalTotal)));

        for (AsientoCierreCartera fila : asientoCierreCarteraDaoService
                .selectByCorrida(corrida.getCodigo())) {
            SubProcesoCierre sub = new SubProcesoCierre();
            sub.setSubProceso(fila.getSubProceso());
            sub.setNombre(nombreSubProceso(fila.getSubProceso()));
            sub.setReferencia(referenciaSubProceso(fila.getSubProceso()));
            sub.setFecha(fila.getFecha());
            sub.setTotalDebe(fila.getValor());
            sub.setTotalHaber(fila.getValor());
            sub.setIdAsiento(fila.getAsiento());
            sub.setNumeroAsiento(fila.getNumeroAsiento());
            sub.setOmitido(Boolean.valueOf(fila.getIdEstado() != null
                    && fila.getIdEstado().longValue() == EstadoAsiento.ANULADO));
            if (Boolean.TRUE.equals(sub.getOmitido())) {
                sub.setMotivoOmision("Asiento anulado por el reverso de la corrida.");
            }
            resultado.getSubProcesos().add(sub);
        }
        return resultado;
    }

    // =====================================================================
    // Carga de parametrización
    // =====================================================================

    /**
     * Carga las bandas vigentes de cada producto y tipo de cartera, con los rangos ya
     * derivados por el clasificador de la Fase 1.
     *
     * @param idEmpresa    : Empresa
     * @param fecha        : Fecha a la que se resuelve la vigencia
     * @param productos    : Productos indexados
     * @param advertencias : Lista donde anotar los productos sin parametrizar
     * @return             : Bandas por clave producto-tipoCartera
     */
    private Map<String, List<BandaProductoDetalle>> cargaBandas(Long idEmpresa, LocalDate fecha,
            Map<Long, Producto> productos, List<String> advertencias) throws Throwable {

        Map<String, List<BandaProductoDetalle>> bandas =
                new LinkedHashMap<String, List<BandaProductoDetalle>>();
        for (ConfiguracionBandaProducto configuracion : configuracionBandaProductoDaoService
                .selectVigentesByEmpresa(idEmpresa, fecha)) {
            if (configuracion.getProducto() == null) {
                continue;
            }
            String clave = claveConfiguracion(configuracion.getProducto().getCodigo(),
                    configuracion.getTipoCartera());
            if (bandas.containsKey(clave)) {
                continue;
            }
            List<BandaProductoDetalle> detalle = clasificadorBandaService.derivarRangos(
                    bandaProductoDaoService.selectByConfiguracion(configuracion.getCodigo()));
            if (detalle.isEmpty()) {
                advertencias.add("La configuracion de bandas " + configuracion.getCodigo()
                        + " (" + nombreProducto(productos, configuracion.getProducto().getCodigo())
                        + ", " + nombreTipoCartera(configuracion.getTipoCartera())
                        + ") no tiene bandas activas.");
                continue;
            }
            bandas.put(clave, detalle);
        }
        if (bandas.isEmpty()) {
            throw new IncomeException("No hay ninguna configuracion de bandas vigente al "
                    + fecha + " para la empresa " + idEmpresa
                    + ". Parametrice las bandas antes de correr el cierre.");
        }
        return bandas;
    }

    /**
     * Compara el snapshot de la corrida anterior con la distribución recalculada a esa
     * misma fecha, y deja las diferencias en el resultado.
     *
     * @param resultado    : Cálculo en construcción
     * @param solicitud    : Solicitud
     * @param distAnterior : Distribución recalculada al corte anterior
     * @param productos    : Productos indexados
     */
    private void cargaDesviaciones(CierreCartera resultado, SolicitudCierreCartera solicitud,
            Map<String, BandaSnapshotCierre> distAnterior, Map<Long, Producto> productos)
            throws Throwable {

        CorridaCierreCartera anterior = corridaCierreCarteraDaoService
                .selectUltimaEjecutadaAntesDe(solicitud.getIdEmpresa(), solicitud.getAnio(),
                        solicitud.getMes());
        resultado.setTotalDesviacion(Double.valueOf(0D));
        if (anterior == null) {
            resultado.getAdvertencias().add("No hay una corrida anterior ejecutada: la"
                    + " reclasificacion se calcula contra la distribucion que la cartera de hoy"
                    + " tenia al " + resultado.getFechaCorte().minusMonths(1)
                            .withDayOfMonth(resultado.getFechaCorte().minusMonths(1).lengthOfMonth())
                    + ", y no hay snapshot con el cual contrastarla.");
            return;
        }

        double total = 0D;
        for (BandaCierreCartera fila : bandaCierreCarteraDaoService
                .selectByCorrida(anterior.getCodigo())) {
            if (fila.getProducto() == null) {
                continue;
            }
            String clave = claveBanda(fila.getProducto().getCodigo(), fila.getTipoCartera(),
                    fila.getNumero());
            double snapshot = fila.getCapital() != null ? fila.getCapital().doubleValue() : 0D;
            double recalculado = capitalDe(distAnterior, clave);
            double desviacion = redondeaDouble(Double.valueOf(recalculado - snapshot));
            if (Math.abs(desviacion) < MINIMO_LINEA) {
                continue;
            }
            DesviacionBandaCierre item = new DesviacionBandaCierre();
            item.setIdProducto(fila.getProducto().getCodigo());
            item.setNombreProducto(fila.getProducto().getNombre());
            item.setTipoCartera(fila.getTipoCartera());
            item.setNumeroBanda(fila.getNumero());
            item.setCuenta(fila.getPlanCuenta() != null
                    ? fila.getPlanCuenta().getCuentaContable() : null);
            item.setCapitalSnapshot(redondea(Double.valueOf(snapshot)));
            item.setCapitalRecalculado(redondea(Double.valueOf(recalculado)));
            item.setDesviacion(Double.valueOf(desviacion));
            resultado.getDesviaciones().add(item);
            total = total + Math.abs(desviacion);
        }
        resultado.setTotalDesviacion(redondea(Double.valueOf(total)));
        if (!resultado.getDesviaciones().isEmpty()) {
            resultado.getAdvertencias().add("Hay " + resultado.getDesviaciones().size()
                    + " bandas cuyo saldo hoy no coincide con lo que la corrida "
                    + anterior.getCodigo() + " contabilizo, por " + resultado.getTotalDesviacion()
                    + " en total. Es lo que movieron los pagos y las entregas del mes, que"
                    + " tienen sus propios asientos; no lo corrige este proceso.");
        }
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    /**
     * Valida la solicitud.
     *
     * @param solicitud : Solicitud
     */
    private void valida(SolicitudCierreCartera solicitud) {
        if (solicitud == null) {
            throw new IncomeException("La solicitud de cierre de cartera es obligatoria");
        }
        if (solicitud.getIdEmpresa() == null) {
            throw new IncomeException("La empresa es obligatoria");
        }
        validaPeriodo(solicitud.getAnio(), solicitud.getMes());
        // Saltarse el control de archivo Petro exige decir por que: una omision sin motivo
        // no se puede auditar despues.
        if (Boolean.TRUE.equals(solicitud.getOmitirControlArchivoPetro())
                && (solicitud.getMotivoOmisionControl() == null
                    || solicitud.getMotivoOmisionControl().trim().isEmpty())) {
            throw new IncomeException("Para omitir el control de archivo Petro hay que indicar"
                    + " el motivo en motivoOmisionControl: la omision queda registrada en la"
                    + " corrida y tiene que poder explicarse.");
        }
    }

    /**
     * Valida el período.
     *
     * @param anio : Año
     * @param mes  : Mes
     */
    private void validaPeriodo(Long anio, Long mes) {
        if (anio == null || mes == null) {
            throw new IncomeException("El anio y el mes a cerrar son obligatorios");
        }
        if (mes.longValue() < 1L || mes.longValue() > 12L) {
            throw new IncomeException("El mes debe estar entre 1 y 12; se recibio: " + mes);
        }
        if (anio.longValue() < 2000L || anio.longValue() > 2100L) {
            throw new IncomeException("El anio esta fuera de rango: " + anio);
        }
    }

    /**
     * Último día del mes de un período.
     *
     * @param anio : Año
     * @param mes  : Mes
     * @return     : Último día
     */
    private LocalDate ultimoDiaDelMes(Long anio, Long mes) {
        LocalDate primero = LocalDate.of(anio.intValue(), mes.intValue(), 1);
        return primero.withDayOfMonth(primero.lengthOfMonth());
    }

    /**
     * Crea un sub-proceso con su cabecera.
     */
    private SubProcesoCierre nuevoSubProceso(int codigo, String nombre, String referencia,
            LocalDate fecha, String glosa) {
        SubProcesoCierre sub = new SubProcesoCierre();
        sub.setSubProceso(Long.valueOf(codigo));
        sub.setNombre(nombre);
        sub.setReferencia(referencia);
        sub.setFecha(fecha);
        sub.setGlosa(glosa);
        sub.setOmitido(Boolean.FALSE);
        return sub;
    }

    /** Clave de una configuración: producto + tipo de cartera. */
    private String claveConfiguracion(Long idProducto, Long tipoCartera) {
        return idProducto + "-" + tipoCartera;
    }

    /** Clave de una banda dentro de una distribución. El número va con ceros para ordenar. */
    private String claveBanda(Long idProducto, Long tipoCartera, Long numero) {
        return String.format("%09d-%d-%03d", Long.valueOf(idProducto != null ? idProducto : 0L),
                Long.valueOf(tipoCartera != null ? tipoCartera : 0L),
                Long.valueOf(numero != null ? numero : 0L));
    }

    /** Capital de una banda en una distribución, o cero si no está. */
    private double capitalDe(Map<String, BandaSnapshotCierre> distribucion, String clave) {
        BandaSnapshotCierre fila = distribucion.get(clave);
        if (fila == null || fila.getCapital() == null) {
            return 0D;
        }
        return fila.getCapital().doubleValue();
    }

    /** Fila nueva del snapshot, con la banda y la cuenta ya resueltas. */
    private BandaSnapshotCierre nuevaFilaSnapshot(Long idProducto, Long tipoCartera,
            BandaProductoDetalle banda, Map<Long, Producto> productos) {
        BandaSnapshotCierre fila = new BandaSnapshotCierre();
        fila.setIdProducto(idProducto);
        fila.setNombreProducto(nombreProducto(productos, idProducto));
        fila.setTipoCartera(tipoCartera);
        fila.setNombreTipoCartera(nombreTipoCartera(tipoCartera));
        fila.setIdBanda(banda.getIdBanda());
        fila.setNumeroBanda(banda.getNumero());
        fila.setEtiquetaBanda(banda.getEtiqueta());
        fila.setIdPlanCuenta(banda.getIdPlanCuenta());
        fila.setCuenta(banda.getCuentaContable());
        fila.setNombreCuenta(banda.getNombreCuenta());
        fila.setCapital(Double.valueOf(0D));
        fila.setCantidad(Long.valueOf(0L));
        return fila;
    }

    /**
     * Agrega al snapshot, en cero, las bandas parametrizadas a las que no llegó capital.
     * Sin ellas la corrida siguiente no podría descargar una banda que se vació.
     */
    private void completaBandasVacias(Map<String, BandaSnapshotCierre> distribucion,
            Map<String, List<BandaProductoDetalle>> bandas, Map<Long, Producto> productos) {
        for (Map.Entry<String, List<BandaProductoDetalle>> entrada : bandas.entrySet()) {
            String[] partes = entrada.getKey().split("-");
            Long idProducto = Long.valueOf(partes[0]);
            Long tipoCartera = Long.valueOf(partes[1]);
            for (BandaProductoDetalle banda : entrada.getValue()) {
                String clave = claveBanda(idProducto, tipoCartera, banda.getNumero());
                if (!distribucion.containsKey(clave)) {
                    distribucion.put(clave,
                            nuevaFilaSnapshot(idProducto, tipoCartera, banda, productos));
                }
            }
        }
    }

    /** Primera banda (número 1) de un producto y tipo de cartera. */
    private BandaProductoDetalle primeraBanda(Map<String, List<BandaProductoDetalle>> bandas,
            Long idProducto, Long tipoCartera) {
        List<BandaProductoDetalle> lista = bandas.get(claveConfiguracion(idProducto, tipoCartera));
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        return lista.get(0);
    }

    /** Acumula el capital que no se pudo clasificar, para el aviso. */
    private void acumulaSinClasificar(Map<String, Double> sinClasificar, Long idProducto,
            Long tipoCartera, Double capital, Map<Long, Producto> productos) {
        String clave = nombreProducto(productos, idProducto) + " / "
                + nombreTipoCartera(tipoCartera);
        sinClasificar.put(clave, suma(sinClasificar.get(clave), capital));
    }

    /** Snapshot ordenado por producto, tipo de cartera y banda. */
    private List<BandaSnapshotCierre> ordenaSnapshot(Map<String, BandaSnapshotCierre> distribucion) {
        List<BandaSnapshotCierre> lista = new ArrayList<BandaSnapshotCierre>();
        for (Map.Entry<String, BandaSnapshotCierre> entrada : distribucion.entrySet()) {
            BandaSnapshotCierre fila = entrada.getValue();
            fila.setCapital(redondea(fila.getCapital()));
            lista.add(fila);
        }
        return lista;
    }

    /** Capital total de una distribución. */
    private Double sumaCapital(Map<String, BandaSnapshotCierre> distribucion) {
        double total = 0D;
        for (BandaSnapshotCierre fila : distribucion.values()) {
            total = total + (fila.getCapital() != null ? fila.getCapital().doubleValue() : 0D);
        }
        return redondea(Double.valueOf(total));
    }

    /** Productos indexados por código. */
    private Map<Long, Producto> indexaProductos() throws Throwable {
        Map<Long, Producto> mapa = new LinkedHashMap<Long, Producto>();
        for (Producto producto : productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO)) {
            mapa.put(producto.getCodigo(), producto);
        }
        return mapa;
    }

    /** Nombre de un producto, o su código si no está. */
    private String nombreProducto(Map<Long, Producto> productos, Long idProducto) {
        Producto producto = productos.get(idProducto);
        return producto != null && producto.getNombre() != null
                ? producto.getNombre() : "Producto " + idProducto;
    }

    /** Etiqueta del tipo de cartera. */
    private String nombreTipoCartera(Long tipoCartera) {
        if (tipoCartera != null && tipoCartera.longValue() == TipoCarteraBanda.VENCIDO) {
            return "VENCIDO";
        }
        return "POR VENCER";
    }

    /** Etiqueta del estado de la corrida. */
    private String nombreEstado(Long idEstado) {
        if (idEstado == null) {
            return null;
        }
        if (idEstado.longValue() == EstadoCorridaCierreCartera.PREPARADA) {
            return "PREPARADA";
        }
        if (idEstado.longValue() == EstadoCorridaCierreCartera.EJECUTADA) {
            return "EJECUTADA";
        }
        if (idEstado.longValue() == EstadoCorridaCierreCartera.REVERSADA) {
            return "REVERSADA";
        }
        return String.valueOf(idEstado);
    }

    /** Nombre de un sub-proceso. */
    private String nombreSubProceso(Long subProceso) {
        if (subProceso == null) {
            return null;
        }
        switch (subProceso.intValue()) {
            case SubProcesoCierreCartera.VENCIDOS:                 return "Asiento de vencidos";
            case SubProcesoCierreCartera.CAMBIO_BANDAS_POR_VENCER: return "Cambio de bandas - cartera por vencer";
            case SubProcesoCierreCartera.CAMBIO_BANDAS_VENCIDO:    return "Reclasificacion - cartera vencida";
            case SubProcesoCierreCartera.APERTURA:                 return "Apertura del periodo de credito";
            case SubProcesoCierreCartera.DEVENGO_INTERESES:        return "Devengo de intereses a ingresos";
            case SubProcesoCierreCartera.NETEO:                    return "Neteo de planillas";
            default:                                               return String.valueOf(subProceso);
        }
    }

    /** Referencia del sub-proceso en la pizarra. */
    private String referenciaSubProceso(Long subProceso) {
        if (subProceso == null) {
            return null;
        }
        switch (subProceso.intValue()) {
            case SubProcesoCierreCartera.VENCIDOS:                 return "①";
            case SubProcesoCierreCartera.CAMBIO_BANDAS_POR_VENCER: return "②";
            case SubProcesoCierreCartera.CAMBIO_BANDAS_VENCIDO:    return "①.1";
            case SubProcesoCierreCartera.APERTURA:                 return "③";
            case SubProcesoCierreCartera.DEVENGO_INTERESES:        return "④";
            case SubProcesoCierreCartera.NETEO:                    return "⑥";
            default:                                               return null;
        }
    }

    /** Etiqueta del período de la solicitud, para las glosas. */
    private String periodo(SolicitudCierreCartera solicitud) {
        return solicitud.getAnio() + "-" + dosDigitos(solicitud.getMes());
    }

    /** Mes con dos dígitos. */
    private String dosDigitos(Long mes) {
        return mes != null && mes.longValue() < 10L ? "0" + mes : String.valueOf(mes);
    }

    /** Concatena dos observaciones sin perder la primera. */
    private String concatena(String actual, String agregado) {
        if (actual == null || actual.trim().isEmpty()) {
            return agregado;
        }
        return actual + " | " + agregado;
    }

    /** Suma dos importes tolerando nulos. */
    private Double suma(Double a, Double b) {
        double total = (a != null ? a.doubleValue() : 0D) + (b != null ? b.doubleValue() : 0D);
        return Double.valueOf(total);
    }

    /** Redondeo contable a dos decimales, media hacia arriba. */
    private Double redondea(Double valor) {
        if (valor == null) {
            return Double.valueOf(0D);
        }
        return Double.valueOf(BigDecimal.valueOf(valor.doubleValue())
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
    }

    /** Redondeo contable devuelto como primitivo. */
    private double redondeaDouble(Double valor) {
        return redondea(valor).doubleValue();
    }

    /** Recupera un producto por código. */
    private Producto recuperaProducto(Long idProducto) throws Throwable {
        return productoDaoService.selectById(idProducto, NombreEntidadesCredito.PRODUCTO);
    }

    /** Recupera una empresa por código. */
    private Empresa recuperaEmpresa(Long idEmpresa) throws Throwable {
        Empresa empresa;
        try {
            empresa = empresaDaoService.selectById(idEmpresa, NombreEntidadesSistema.EMPRESA);
        } catch (Throwable e) {
            throw new IncomeException("No existe la empresa " + idEmpresa);
        }
        if (empresa == null) {
            throw new IncomeException("No existe la empresa " + idEmpresa);
        }
        return empresa;
    }

    /** Recupera una cuenta contable por código. */
    private PlanCuenta recuperaCuenta(Long idPlanCuenta) throws Throwable {
        if (idPlanCuenta == null) {
            throw new IncomeException("Una linea del asiento quedo sin cuenta contable.");
        }
        return planCuentaDaoService.selectById(idPlanCuenta,
                NombreEntidadesContabilidad.PLAN_CUENTA);
    }
}
