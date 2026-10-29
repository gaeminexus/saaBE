package com.saa.ejb.asoprep.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.DetalleCargaArchivoService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.ParticipeXCargaArchivoService;
import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.DiferenciaParticipeDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoDiferenciaDistribucionBanda;
import com.saa.model.crd.AfectacionValoresParticipeCarga;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.FamiliaNovedadCarga;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.NovedadParticipeCarga;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.ParticipeXCargaArchivo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.ASPNovedadesCargaArchivo;
import com.saa.rubros.DsbnOrigen;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.Filiales;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación Stateful para procesar archivos Petro con manejo de transacciones
 *
 * 2026-09-02 (PLAN-FASE3-MOTOR-PAGOS.md): la fase 3 (aplicación de pagos normales y de
 * afectación manual) YA NO reimplementa su propia cascada — delega en
 * {@code com.saa.ejb.crd.serviceImpl.MotorPagoPrestamoServiceImpl} vía
 * {@code motorPagoPrestamoService.aplicarPago(idPrestamo, valor, ctx)}, con
 * {@code ContextoPago#getIdCargaArchivo()} seteado para que el motor estampe la carga en cada
 * {@code PagoPrestamo} (trazabilidad para {@code CobroPetroContableServiceImpl.contabilizarAplicacion},
 * que agrupa por CRARCDGO). {@code calcularSaldosRealesCuota} y {@code totalBaseCuota} de esta
 * clase SIGUEN en uso (buscarCuotaAPagar y las validaciones de Fase 2, que no se tocaron) — no
 * son código muerto, solo dejaron de usarse para APLICAR el pago.
 *
 * Cualquier cambio en la aplicación de pagos de esta clase debe actualizar además
 * docs/logica-negocio/petro/REGLAS-CARGA-PETRO.md.
 */
@Stateful
public class CargaArchivoPetroServiceImpl implements CargaArchivoPetroService {

    @EJB
    private FileService fileService;
    
    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @EJB
    private DetalleCargaArchivoService detalleCargaArchivoService;
    
    @EJB
    private ParticipeXCargaArchivoService participeXCargaArchivoService;
    
    @EJB
    private com.saa.ejb.crd.dao.ParticipeXCargaArchivoDaoService participeXCargaArchivoDaoService;
    
    @EJB
    private EntidadDaoService entidadDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.ProductoDaoService productoDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.PrestamoDaoService prestamoDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.DetallePrestamoDaoService detallePrestamoDaoService;
    
    @EJB
    private com.saa.ejb.crd.service.DetallePrestamoService detallePrestamoService;
    
    @EJB
    private com.saa.ejb.crd.service.PagoPrestamoService pagoPrestamoService;
    
    @EJB
    private com.saa.ejb.crd.service.NovedadParticipeCargaService novedadParticipeCargaService;
    
    @EJB
    private com.saa.ejb.crd.service.AfectacionValoresParticipeCargaService afectacionValoresParticipeCargaService;
    
    @EJB
    private com.saa.ejb.crd.dao.HistorialSueldoDaoService historialSueldoDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.DetalleCargaArchivoDaoService detalleCargaArchivoDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.PagoPrestamoDaoService pagoPrestamoDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.AfectacionValoresParticipeCargaDaoService afectacionValoresParticipeCargaDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService novedadParticipeCargaDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private com.saa.ejb.crd.dao.AporteDaoService aporteDaoService;

    // VALIDACION-TOPE-AFECTACION-MANUAL.md §15: registra en transacción PROPIA las novedades
    // generadas DURANTE aplicarPagosArchivoPetro (§12 sobrante de aportes, §13 HS huérfano,
    // excedente no aplicado del motor) — si se registraran con registrarNovedad (transacción
    // única de todo el proceso) y esta transacción revierte más adelante (p.ej. la red del
    // §11), la novedad que debía guiar al operador desaparece con el rollback. Ver el javadoc
    // de la interfaz para por qué esto NO puede ser un método REQUIRES_NEW de esta misma clase.
    @EJB
    private com.saa.ejb.asoprep.service.RegistradorNovedadCargaPetroService registradorNovedadTransaccionPropia;

    @EJB
    private com.saa.ejb.crd.service.PagoAporteService pagoAporteService;
    
    @EJB
    private com.saa.ejb.crd.dao.PagoAporteDaoService pagoAporteDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.TipoAporteDaoService tipoAporteDaoService;
    
    @EJB
    private com.saa.basico.ejb.FechaService fechaService;

    /** §11 VALIDACION-TOPE-AFECTACION-MANUAL.md: recibido de la carga, misma fuente que el
     * asiento de reparto — ver {@link #validarRecibidoIgualAplicado}. */
    @EJB
    private com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService transferenciaCargaPetroDaoService;

    /** §11 VALIDACION-TOPE-AFECTACION-MANUAL.md: desglose por partícipe del descuadre, para el
     * mensaje de {@link #validarRecibidoIgualAplicado} — cuarto consumidor de la misma cuenta,
     * reutilizada, no copiada. */
    @EJB
    private com.saa.ejb.crd.service.DistribucionBandaService distribucionBandaService;

    /** Pedido 10: para que un préstamo sin cuotas vencidas vuelva a VIGENTE en cuanto se paga. */
    @EJB
    private com.saa.ejb.crd.service.ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /** D-019 (reproceso): para marcar los eventos de cancelación posteriores al mes de la carga. */
    @EJB
    private com.saa.ejb.crd.dao.EventoPrestamoDaoService eventoPrestamoDaoService;

    /** Fase 3: fuente del esperado mensual, ver {@link #esperadoMensual}. */
    @EJB
    private com.saa.ejb.crd.service.VigenciaContratoService vigenciaContratoService;

    /**
     * Solo para distinguir, en {@link #distribuirAportePorDevengo}, "no tiene contrato" (aborta
     * la carga) de "tiene contrato pero la vigencia no cubre el mes" (no aborta) — nunca lo
     * infiere de que {@code esperadoMensual} dio 0, que no distingue las dos causas.
     */
    @EJB
    private com.saa.ejb.crd.dao.ContratoDaoService contratoDaoService;

    /**
     * CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3.2: para resolver, UNA vez por partícipe
     * (no una por mes×tipo), las vigencias del contrato cuando el lote no lo cubrió.
     */
    @EJB
    private com.saa.ejb.crd.dao.VigenciaContratoDaoService vigenciaContratoDaoService;

    /** Fase 3a: asiento de REPARTO del paso 2, ver {@link #aplicarPagosArchivoPetro}. */
    @EJB
    private com.saa.ejb.crd.service.CobroPetroContableService cobroPetroContableService;

    /** Fase 3 (PLAN-FASE3-MOTOR-PAGOS.md, 2026-09-02): motor compartido de aplicación de pagos. */
    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    /**
     * Dinero recibido sin aplicar del todo (2026-08-31), acumulado durante UNA corrida de
     * {@link #aplicarPagosArchivoPetro} para que el resumen final los muestre sin tener que
     * leer el log del servidor. Dos listas separadas porque el tratamiento es distinto:
     * {@code advertenciasVigenciaCargaActual} es solo informativa (contrato activo, vigencia
     * no cubre el mes — ver {@link #distribuirAportePorDevengo}); {@code
     * novedadesGeneradasCargaActual} son NOVEDAD, requieren decisión del operador en pantalla
     * (sin HistorialSueldo activo, o esperado en $0 — ver {@link #aplicarAporteAH}).
     *
     * <b>Se resetean al ENTRAR a {@code aplicarPagosArchivoPetro}, no al salir.</b> Si una
     * corrida falla a mitad (TODO O NADA), la siguiente no puede arrastrar advertencias de la
     * corrida anterior — un reset al salir dejaría el resumen de la corrida fallida contaminado
     * en la próxima que sí termine. Es seguro como campo de instancia porque esta clase es
     * {@code @Stateful}: una instancia por sesión de carga.
     */
    private List<String> advertenciasVigenciaCargaActual = new ArrayList<>();
    private List<String> novedadesGeneradasCargaActual = new ArrayList<>();

    /**
     * MODO REPROCESO (cuadre Superintendencia, cambio C4 — {@code REPROCESO-CARGA-PETRO.md}).
     * Lo enciende únicamente {@link #aplicarPagosArchivoPetroReproceso} mientras corre la fase 3
     * de una carga que {@code ReprocesoCargaPetroService} acaba de anular, y lo apaga al salir
     * (también si falla). Cambia exactamente cuatro cosas, todas marcadas con
     * {@code if (modoReproceso)} en el código: no se valida el orden cronológico; un rol Petro
     * sin partícipe NO aborta ni se omite en silencio (queda en {@code sinParticipeCargaActual}
     * y su monto se descuenta del recibido en {@link #validarRecibidoIgualAplicado}); esa misma
     * novedad no bloquea en {@link #buscarValoresSinDestino}; y no se toca el estado del
     * partícipe ({@code evaluarMoraPorFaltaDeAporte}/{@code restaurarActivoPorPago}).
     * Fuera de ese modo el comportamiento es byte a byte el de antes.
     *
     * Seguro como campo de instancia por la misma razón que las dos listas de arriba: la clase
     * es {@code @Stateful}, una instancia por sesión.
     */
    private boolean modoReproceso = false;
    private String etiquetaReproceso = null;
    private List<String> sinParticipeCargaActual = new ArrayList<>();
    private double montoSinParticipeCargaActual = 0.0;

    /**
     * D-010 (cuadre Superintendencia, 2026-10-28): excedentes que Petro descontó de más y ASOPREP
     * DEVOLVIÓ al partícipe, por clave {@code rol|PRODUCTO}. Solo se llena en modo reproceso
     * ({@link #aplicarPagosArchivoPetroReproceso}); fuera de él está vacío y ningún camino lo lee.
     * Cada uno se registra como aporte {@code EXCEDENTE_PETRO} y su monto se descuenta de la fila
     * antes de entrar al motor de pagos o a la prelación de aportes. REPROCESO-CARGA-PETRO.md §4.7.
     */
    private Map<String, com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso> excedentesDevueltosReproceso = new HashMap<>();
    private Set<String> excedentesDevueltosAplicados = new HashSet<>();
    private List<String> detalleExcedentesDevueltos = new ArrayList<>();
    private double montoExcedentesDevueltos = 0.0;

    /**
     * D-019 (2026-10-28): operaciones vigentes a la fecha de corte que hoy están canceladas y que
     * el reproceso reabrió para aplicarles la cuota del mes; su cancelación queda marcada
     * "pendiente de re-aplicar" en su propio mes. Una línea por operación.
     */
    private List<String> cancelacionesPosterioresMarcadas = new ArrayList<>();

    /**
     * D-019 ampliada (2026-10-28): cuotas del mes de la carga que la migración dejó saldadas
     * (PAGADA/CANCELADA_ANTICIPADA) sin ningún pago vigente en {@code CRD.PGPR} y sin fecha de pago
     * o con fecha posterior al corte, reabiertas por {@link #reabrirCuotasSaldadasSinPagoAlCorte}.
     * Una línea por cuota. Reset al entrar a {@code aplicarPagosArchivoPetro}.
     */
    private List<String> cuotasSaldadasSinPagoReabiertas = new ArrayList<>();

    /**
     * D-019 ampliada: préstamos ya preparados (regla de la cuota saldada sin pago + reapertura de
     * la operación terminal) en ESTA corrida, para que el paso previo a la validación y la fase 3
     * no los preparen dos veces. Reset al entrar a {@code aplicarPagosArchivoPetro}.
     */
    private Set<Long> prestamosPreparadosReproceso = new HashSet<>();

    /**
     * Rendimiento del reproceso (REPROCESO-CARGA-PETRO.md §11): total de cuotas por préstamo,
     * memoizado dentro de UNA corrida en modo reproceso para {@link #verificarYActualizarEstadoPrestamo}.
     * Es un {@code COUNT(*)} de {@code CRD.DTPR} por préstamo: el reproceso actualiza cuotas pero
     * nunca inserta ni borra ninguna, así que el valor no cambia durante la corrida. Reset al
     * entrar a {@code aplicarPagosArchivoPetro}; fuera del modo reproceso no se usa.
     */
    private Map<Long, Long> totalCuotasPorPrestamoReproceso = new HashMap<>();

    /**
     * D-022 (2026-10-28, REPROCESO-CARGA-PETRO.md §4.10): aportes AH registrados en modo reproceso
     * para entidades sin contrato activo hoy cuyo contrato vigente al corte no tiene vigencias
     * (glosa con {@link #MARCA_D022}). Una línea por aporte. Reset al entrar a
     * {@code aplicarPagosArchivoPetro}.
     */
    private List<String> aportesContratoAlCorte = new ArrayList<>();
    private double montoAportesContratoAlCorte = 0.0;

    /**
     * D-022 vigencias (2026-10-28, §4.10): aportes AH de entidades con contrato ACTIVO hoy cuyo
     * contrato no tiene vigencia que cubra el mes de la carga (glosa con
     * {@link #MARCA_D022_SIN_VIGENCIA}). Reset al entrar a {@code aplicarPagosArchivoPetro}.
     */
    private List<String> aportesSinVigenciaDelMes = new ArrayList<>();
    private double montoAportesSinVigenciaDelMes = 0.0;

    /**
     * D-022 ampliada (líder, 2026-10-28): partícipes con AH descontado y SIN NINGÚN contrato, para
     * los que el reproceso creó contrato + vigencia y registró el aporte (glosa con
     * {@link #MARCA_D022_CREADO}). Una línea por partícipe. Reset al entrar a
     * {@code aplicarPagosArchivoPetro}.
     */
    private List<String> contratosCreadosReproceso = new ArrayList<>();
    private double montoContratosCreadosReproceso = 0.0;

    /**
     * §4.11 (2026-10-28): filas con descuento que terminaron sin aplicar su dinero en esta corrida
     * de reproceso (rol, producto, monto, motivo). Las lista el mensaje del invariante. Reset al
     * entrar a {@code aplicarPagosArchivoPetro}.
     */
    private List<String> filasSinPagoReproceso = new ArrayList<>();
    private double montoFilasSinPagoReproceso = 0.0;

    /**
     * §4.12 (D-023): HS aplicados como seguro de incendio de una cuota que no lo traía. Una línea
     * por fila. Reset al entrar a {@code aplicarPagosArchivoPetro}.
     */
    private List<String> hsSinSeguroEnCuota = new ArrayList<>();
    private double montoHsSinSeguroEnCuota = 0.0;

    /** D-022 ampliada: tipo de contrato del contrato que el reproceso crea. */
    @EJB
    private com.saa.ejb.crd.dao.TipoContratoDaoService tipoContratoDaoService;

    /** Usuario que ordenó el reproceso (lo estampa D-022 en lo que crea). Solo en modo reproceso. */
    private String usuarioReproceso = null;

    /** D-022: tipo de contrato de los aportantes de la carga, resuelto una vez por corrida. */
    private Long tipoContratoAportantesReproceso = null;

    /**
     * Rendimiento del reproceso (REPROCESO-CARGA-PETRO.md §11): control del contexto de
     * persistencia de la transacción (flush + detach acotado, auto-flush suspendido en las
     * validaciones de solo lectura). Solo se usa en modo reproceso.
     */
    @EJB
    private com.saa.basico.ejb.ContextoPersistenciaService contextoPersistenciaService;

    /**
     * Tamaño del contexto de persistencia (entidades administradas) a partir del cual, en un borde
     * de unidad de trabajo del reproceso (fin de una fila preparada, fin de un partícipe aplicado),
     * se vuelca y se desprenden las clases de {@link #CLASES_DESPRENDIBLES_REPROCESO}. Con el
     * contexto acotado a este orden, el auto-flush de cada consulta cuesta milisegundos en vez de
     * crecer con la corrida. REPROCESO-CARGA-PETRO.md §11.
     */
    static final int UMBRAL_CONTEXTO_REPROCESO = 2000;

    /**
     * Clases que el reproceso desprende del contexto en sus bordes de unidad de trabajo
     * (REPROCESO-CARGA-PETRO.md §11). Son las de alto volumen y vida corta: cada una se obtiene de
     * nuevo por consulta en la unidad siguiente y ninguna queda referenciada en un campo de este
     * bean ni en una variable que cruce el borde y se modifique después (verificado por fase en el
     * §11). Se conservan a propósito {@code CargaArchivo}, {@code DetalleCargaArchivo},
     * {@code Producto} y los catálogos: los usan las variables de larga vida de las fases
     * ({@code cargaArchivo}, {@code detallesCarga}, {@code productosCache}). Ninguna de las
     * conservadas tiene relación hacia una desprendida, y el modelo de {@code crd} no declara
     * cascadas ni colecciones.
     */
    static final List<Class<?>> CLASES_DESPRENDIBLES_REPROCESO = Arrays.asList(
        DetallePrestamo.class,
        PagoPrestamo.class,
        Prestamo.class,
        com.saa.model.crd.EventoPrestamo.class,
        Aporte.class,
        com.saa.model.crd.PagoAporte.class,
        NovedadParticipeCarga.class,
        AfectacionValoresParticipeCarga.class,
        com.saa.model.crd.HistorialSueldo.class,
        Contrato.class,
        com.saa.model.crd.VigenciaContrato.class,
        Entidad.class,
        ParticipeXCargaArchivo.class
    );

    /** Inicio de una fase del reproceso: una línea de log con el tamaño del contexto. */
    private long inicioFaseReproceso(String fase) {
        System.out.println("[REPROCESO-RENDIMIENTO] " + etiquetaReproceso + " | " + fase + " | INICIO | contexto "
            + contextoPersistenciaService.tamanio() + " entidades");
        return System.nanoTime();
    }

    /** Fin de una fase del reproceso: tiempo, tamaño del contexto y las clases más numerosas. */
    private void finFaseReproceso(String fase, long inicioNanos, String extra) {
        double segundos = (System.nanoTime() - inicioNanos) / 1_000_000_000.0;
        System.out.println("[REPROCESO-RENDIMIENTO] " + etiquetaReproceso + " | " + fase + " | FIN | "
            + String.format("%.1f", segundos) + " s | contexto " + contextoPersistenciaService.tamanio()
            + " entidades (" + contextoPersistenciaService.histograma(6) + ")"
            + (extra != null && !extra.isEmpty() ? " | " + extra : ""));
    }

    /**
     * Borde de UNIDAD de trabajo del reproceso (fin de una fila preparada, fin de un partícipe
     * aplicado): si el contexto pasó {@link #UMBRAL_CONTEXTO_REPROCESO}, vuelca y desprende
     * {@link #CLASES_DESPRENDIBLES_REPROCESO}. Fuera del modo reproceso no hace nada.
     *
     * @return cuántas entidades desprendió (0 si no hizo falta)
     */
    private int aliviarContextoReproceso() {
        if (!modoReproceso) {
            return 0;
        }
        int desprendidas = contextoPersistenciaService.volcarYDesprenderSiExcede(
            UMBRAL_CONTEXTO_REPROCESO, CLASES_DESPRENDIBLES_REPROCESO);
        return Math.max(desprendidas, 0);
    }

    /** Borde de FASE del reproceso: vuelca y desprende siempre, sin umbral. Fuera del modo reproceso no hace nada. */
    private int desprenderContextoReproceso() {
        if (!modoReproceso) {
            return 0;
        }
        return contextoPersistenciaService.volcarYDesprender(CLASES_DESPRENDIBLES_REPROCESO);
    }

    /**
     * §16 (2026-09-03, revalidación de carga): qué (fila PXCA, tipoNovedad) confirmó
     * {@code registrarNovedad} durante UNA corrida de {@link #revalidarCarga}, para poder
     * distinguir "el código actual todavía produce esta novedad" (se reconfirmó — sigue en este
     * mapa) de "el código actual YA NO la produce" (existe en BD pero no quedó acá — candidata a
     * desactivar). Se llena SIEMPRE que se llama a {@code registrarNovedad} (también durante el
     * procesamiento normal de Fase 1/2), no solo durante la revalidación — fuera de una
     * revalidación nadie lo lee, así que es inofensivo.
     *
     * <b>Se resetea al ENTRAR a {@code revalidarCarga}</b>, mismo criterio que las dos listas de
     * arriba y por la misma razón: es seguro como campo de instancia porque esta clase es
     * {@code @Stateful} (una instancia por sesión de carga).
     */
    private Map<Long, Set<Long>> tiposConfirmadosRevalidacion = new HashMap<>();

    /** Cuántas líneas de detalle lleva el resumen antes de resumir el resto. Arbitrario,
     * ajustable — evita un resumen de 50 líneas tan ilegible como el problema que resuelve. */
    private static final int MAX_ADVERTENCIAS_EN_RESUMEN = 10;

    // Única definición: FamiliaNovedadCarga.TOLERANCIA (2026-08-31) — el modelo no puede
    // depender de ejb, así que la constante vive ahí y este archivo la lee, nunca al revés.
    private static final double TOLERANCIA = FamiliaNovedadCarga.TOLERANCIA;
    
    // Códigos de TipoAporte
    private static final Long TIPO_APORTE_JUBILACION = 9L;  // Código para aporte de jubilación (CORRECTO)
    private static final Long TIPO_APORTE_CESANTIA = 11L;   // Código para aporte de cesantía
    
    // Códigos de producto Petro que NO se validan como préstamos
    private static final String CODIGO_PRODUCTO_APORTES = "AH";
    private static final String CODIGO_PRODUCTO_HS = "HS";
    private static final String CODIGO_PRODUCTO_PH = "PH"; // Préstamo Hipotecario - El seguro viene en HS
    private static final String CODIGO_PRODUCTO_PP = "PP"; // Préstamo Prendario - El seguro viene en HS

    /**
     * Tipos ESTRUCTURALES (Fase 1, identificación) — VALIDACION-TOPE-AFECTACION-MANUAL.md §16.
     * {@link #revalidarCarga} SOLO refresca el MONTO (merge de HS) de una novedad de estos
     * tipos si ya existe — NO re-deriva la clasificación en sí (¿es realmente
     * CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE?), porque esa identificación tiene un efecto de lado
     * (puede actualizar {@code Entidad.rolPetroComercial}) que revalidarCarga, pensado como
     * recálculo de novedades, no debe disparar de nuevo silenciosamente.
     */
    private static final List<Long> TIPOS_ESTRUCTURALES_REVALIDABLES = Arrays.asList(
        (long) ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.CODIGO_ROL_DUPLICADO,
        (long) ASPNovedadesCargaArchivo.NOMBRE_ENTIDAD_DUPLICADO,
        (long) ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE
    );

    /** Tipos que {@link #validarNovedadesFase2} puede registrar para un producto de préstamo
     * (PH/PP/otros) — VALIDACION-TOPE-AFECTACION-MANUAL.md §16, usado por {@link #revalidarCarga}
     * para saber qué novedades EXISTENTES de una fila entran en el barrido de "¿siguen
     * corresponde?".
     *
     * <p>⚠️ {@code MONTO_INCONSISTENTE} NO está en esta lista, a propósito — a diferencia de
     * {@code APORTE_MONTO_INCONSISTENTE} (exclusivo de {@link #validarAporteAH}, sin colisión),
     * este código lo comparten TRES orígenes distintos: {@link #validarNovedadesFase2} (acá),
     * {@link #manejarExcedenteNoAplicado} (excedente del motor de pagos, en APLICACIÓN, §15) y
     * {@link #distribuirAportePorDevengo} (sobrante de aportes, también en APLICACIÓN, §12).
     * Si se incluyera acá, el barrido "¿ya no corresponde?" de {@link #revalidarCarga} podría
     * desactivar (o marcar para conservar) una novedad de APLICACIÓN que no tiene nada que ver
     * con lo que {@code validarNovedadesFase2} está re-evaluando — revalidarCarga SOLO corre
     * ANTES de aplicar (bloquea si {@code estado == 3}), pero una corrida de
     * {@code aplicarPagosArchivoPetro} que revirtió por la red del §11 puede haber dejado
     * novedades de aplicación YA PERSISTIDAS (esa es la razón de ser del §15) en una carga que
     * todavía no llegó a estado 3. Se prefiere NO tocar {@code MONTO_INCONSISTENTE} en ninguna
     * dirección (ni refrescarla ni desactivarla) antes que arriesgar pisar o esconder una
     * novedad que vino de aplicar, no de validar.</p>
     */
    private static final List<Long> TIPOS_FASE2_PRESTAMO_REVALIDABLES = Arrays.asList(
        (long) ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO,
        (long) ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.SIN_DESCUENTOS,
        (long) ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA,
        (long) ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR,
        (long) ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE
    );

    /** Igual que {@link #TIPOS_FASE2_PRESTAMO_REVALIDABLES}, para el producto AH — lo que puede
     * registrar {@link #validarAporteAH}. */
    private static final List<Long> TIPOS_FASE2_APORTE_REVALIDABLES = Arrays.asList(
        (long) ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
        (long) ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
        (long) ASPNovedadesCargaArchivo.APORTE_VALORES_CERO,
        (long) ASPNovedadesCargaArchivo.APORTE_MONTO_INCONSISTENTE,
        (long) ASPNovedadesCargaArchivo.APORTE_DIFERENCIA_MENOR_UN_DOLAR
    );

    /**
     * Método principal que procesa archivos Petro
     * OPCIÓN 1 APLICADA: Primero operaciones de BD, luego subir archivo
     * Utiliza transacción REQUIRED para rollback automático si falla algún paso
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String procesarArchivoPetro(InputStream archivoInputStream, String fileName, 
                                     CargaArchivo cargaArchivo, 
                                     List<DetalleCargaArchivo> detallesCargaArchivos,
                                     List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable {
        
        try {
            // 0. PROCESAR EL CONTENIDO DEL ARCHIVO
            String contenido = leerContenidoArchivo(archivoInputStream);
            List<ParticipeXCargaArchivo> registrosProcesados = procesarContenido(contenido);
            
            // Agrupar por aporte (DetalleCargaArchivo)
            Map<String, DetalleCargaArchivo> aporteAgrupados = agruparPorAporte(registrosProcesados);
            
            // Convertir a listas para persistir
            List<DetalleCargaArchivo> detallesGenerados = new ArrayList<>(aporteAgrupados.values());
            
            // Calcular totales generales para CargaArchivo
            cargaArchivo = calcularTotalesGenerales(cargaArchivo, detallesGenerados);
        	
            // 1. PRIMERO: Almacenar registros en BD (TRANSACCIONAL)
            CargaArchivo cargaArchivoGuardado = almacenaRegistros(cargaArchivo, detallesGenerados, registrosProcesados);
            
            // 2. AL FINAL: Cargar el archivo físico (NO TRANSACCIONAL)
            // Solo se ejecuta si todas las operaciones de BD fueron exitosas
            String rutaArchivo = cargarArchivo(archivoInputStream, fileName, cargaArchivo);
            
            cargaArchivoGuardado.setRutaArchivo(rutaArchivo);
            cargaArchivoGuardado = cargaArchivoService.saveSingle(cargaArchivoGuardado);
            
            return rutaArchivo;
            
        } catch (Throwable e) {
            System.err.println("Error en procesamiento: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Carga el archivo en la carpeta aportes/año/mes usando FileService
     */
    private String cargarArchivo(InputStream archivoInputStream, String fileName, CargaArchivo cargaArchivo) throws Throwable {
        // Construir la ruta: aportes/cargaArchivo.anioAfectacion/cargaArchivo.mesAfectacion
        StringBuilder uploadPath = new StringBuilder("aportes");
        
        if (cargaArchivo.getAnioAfectacion() != null) {
            uploadPath.append("/").append(cargaArchivo.getAnioAfectacion());
        }
        
        if (cargaArchivo.getMesAfectacion() != null) {
            uploadPath.append("/").append(cargaArchivo.getMesAfectacion());
        }
        
        return fileService.uploadFileToPath(archivoInputStream, fileName, uploadPath.toString());
    }

    /**
     * Almacena el registro de CargaArchivo
     */
    private CargaArchivo almacenarCargaArchivo(CargaArchivo cargaArchivo) throws Throwable {
        cargaArchivo.setFechaCarga(LocalDateTime.now());
        
        // Validar que los campos obligatorios vengan del frontend
        if (cargaArchivo.getFilial() == null) {
            throw new RuntimeException("El campo 'filial' es obligatorio y debe ser enviado desde el frontend");
        }
        
        if (cargaArchivo.getUsuarioCarga() == null) {
            throw new RuntimeException("El campo 'usuarioCarga' es obligatorio y debe ser enviado desde el frontend");
        }
        
        return cargaArchivoService.saveSingle(cargaArchivo);
    }
    
    private CargaArchivo almacenaRegistros(CargaArchivo cargaArchivo,
										List<DetalleCargaArchivo> detallesCargaArchivos,
										List<ParticipeXCargaArchivo> participesXCargaArchivo) throws Throwable {
		// Almacenar CargaArchivo
		CargaArchivo cargaArchivoGuardado = almacenarCargaArchivo(cargaArchivo);
		// Filial de la carga (2026-09-02): la identificación de fase 1 y el bloqueo de
		// CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE dependen de esto — ver esFilialPetrocomercial.
		Long codigoFilial = cargaArchivoGuardado.getFilial() != null
			? cargaArchivoGuardado.getFilial().getCodigo() : null;
		// Asignar la referencia al CargaArchivo guardado
        for (DetalleCargaArchivo detalle : detallesCargaArchivos) {
            detalle.setCargaArchivo(cargaArchivoGuardado);
            DetalleCargaArchivo detalleGuardado = detalleCargaArchivoService.saveSingle(detalle);
            // Filtrar partícipes que pertenecen a este detalle usando el código del producto
            String codigoProducto = detalle.getCodigoPetroProducto();
            
            // ==========================================
            // VERIFICAR SI ES PRODUCTO ESPECIAL (AH, HS)
            // Los productos especiales NO requieren validaciones de entidad/préstamo
            // ==========================================
            boolean esProductoEspecial = CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto) ||
                                         CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto);
            
            for (ParticipeXCargaArchivo participe : participesXCargaArchivo) {
            	// Verificar que el participe pertenece a este detalle comparando por código de producto
            	if (participe.getDetalleCargaArchivo() != null && 
            		codigoProducto.equals(participe.getDetalleCargaArchivo().getCodigoPetroProducto())) {
					participe.setCodigo(null); // Limpiar código para que se genere uno nuevo
					participe.setDetalleCargaArchivo(detalleGuardado);
					
					// ==========================================
					// VALIDACIONES DE FASE 1 - SOLO PARA PRÉSTAMOS
					// Omitir validaciones de entidad para AH (aportes) y HS (seguros)
					// ==========================================
					if (!esProductoEspecial) {
						// VALIDACIONES DE EXISTENCIA DE PARTICIPE COMO ENTIDAD
						List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
						if(entidades.size() > 1) {
							participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.CODIGO_ROL_DUPLICADO));
						} else if(entidades.size() == 0) {
							// Si no ecuentra en codigo petro busca al participe por nombre
							List<Entidad> entidadesPorNombre = entidadDaoService.selectByNombrePetro35(participe.getNombre());
							if(entidadesPorNombre.size() == 0) {
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO));
							} else if(entidadesPorNombre.size() > 1) {
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.NOMBRE_ENTIDAD_DUPLICADO));
							} else {
								// Si encuentra por nombre solo a uno que no tenia el código petro entonces actualiza la endidad con el codigo petro
								Entidad entidadActualizar = entidadesPorNombre.get(0);
								entidadActualizar.setRolPetroComercial(participe.getCodigoPetro());
								entidadDaoService.save(entidadActualizar, entidadActualizar.getCodigo());
								// actualiza la novedad del participe como OK
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.OK));
							}
						} else { // cuando se encuentra solo un codigo petro
							// Si encuentra solo uno entonces valida que el nombre del participe coincida con el de la entidad
							int LARGO_NOMBRE_PETRO = 35;
							String razonSocialTrim = entidades.get(0).getRazonSocial().trim();
							int largoTrim = LARGO_NOMBRE_PETRO;
							if(razonSocialTrim.length() < LARGO_NOMBRE_PETRO) {
								largoTrim = razonSocialTrim.length();
							}
							// El truncado a 35 sigue igual (el archivo Petro trunca ahí, es correcto).
							// Lo que cambia (2026-09-02) es normalizar los DOS lados antes de comparar:
							// Petrocomercial manda los nombres SIN tildes y CRD.ENTD los guarda CON
							// tildes, así que chocaban todos los meses con partícipes distintos cada
							// vez — ver el javadoc de normalizarNombreParaComparar.
							String nombreEsperado = normalizarNombreParaComparar(razonSocialTrim.substring(0, largoTrim));
							String nombreArchivo = normalizarNombreParaComparar(participe.getNombre());
							if (!nombreEsperado.equalsIgnoreCase(nombreArchivo)) {
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE));
							} else {
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.OK));
							}
						}
						
						// ==========================================
						// VALIDACIÓN 8: VALORES_CERO
						// Detectar cuando todos los valores financieros son cero
						// ==========================================
						if (participe.getNovedadesCarga() != null && participe.getNovedadesCarga() == ASPNovedadesCargaArchivo.OK) {
							if (validarValoresCero(participe)) {
								participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.VALORES_CERO));
							}
						}
					} else {
						// Para productos especiales (AH, HS), marcar como OK directamente
						participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.OK));
					}
					
					// ==========================================
					// VALIDACIÓN FINANCIERA
					// IMPORTANTE: Solo para PRÉSTAMOS, NO para AH (aportes) ni HS
					// ==========================================
				
			if (!esProductoEspecial) {
				// Solo validar novedades financieras para PRÉSTAMOS
				if (participe.getCapitalNoDescontado() > 0 || participe.getInteresNoDescontado() > 0 || 
					participe.getDesgravamenNoDescontado() > 0) {
					if (participe.getTotalDescontado() == 0) {
						participe.setNovedadesFinancieras(Long.valueOf(ASPNovedadesCargaArchivo.SIN_DESCUENTOS));
					} else {
						participe.setNovedadesFinancieras(Long.valueOf(ASPNovedadesCargaArchivo.DESCUENTOS_INCOMPLETOS));
					}
				}
			}
					
				// ==========================================
				// GUARDAR REGISTRO (INSERT) - Asigna el ID
				// FASE 1: Solo validaciones básicas y almacenamiento
				// ==========================================
				// TODO O NADA (2026-08-29) — el más grave de los encontrados en este barrido, ver
				// §3.1b de REGLAS-CARGA-PETRO.md: antes, si el INSERT de una línea fallaba, se
				// logueaba y se seguía con la siguiente. Los otros nueve puntos corregidos esta
				// semana eran sobre negocio YA EN LA BASE (algo se calculaba mal, se cobraba dos
				// veces, quedaba un estado inconsistente) — todos dejan rastro. Este NO deja
				// NINGUNO: si el INSERT falla, ese partícipe simplemente NO EXISTE para la
				// carga. No genera error, no genera novedad, no aparece en ningún resumen, y las
				// Fases 2 y 3 ni siquiera saben que debía estar — el archivo se procesa "sin
				// errores" con gente adentro que se perdió en el camino, y nadie se entera salvo
				// que alguien note meses después que a un partícipe no le descontaron. Solo un
				// saveSingle (INSERT): no hay "ausencia de dato" posible para un fallo de
				// guardado, cualquier excepción acá es un fallo real.
				try {
					participe = participeXCargaArchivoService.saveSingle(participe);
				} catch (Throwable e) {
					throw new RuntimeException("Falló al insertar en PXCA el partícipe con código Petro "
						+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
						+ codigoProducto + " — se aborta toda la carga, este partícipe habría "
						+ "quedado invisible para el resto del proceso. Causa: " + e.getMessage(), e);
				}

				// 2026-08-31 (decisión del árbitro, opción a): SIN_DESCUENTOS/DESCUENTOS_INCOMPLETOS
				// también generan su fila NVPC, no solo el campo plano de arriba
				// (participe.getNovedadesFinancieras()) — ese campo NUNCA tiene montoDiferencia,
				// así que FamiliaNovedadCarga.clasificar (que solo mira filas NVPC) lo dejaba
				// INFORMATIVA aunque sea el caso más puro de "no se cobró, hay que gestionar".
				// Va DESPUÉS del INSERT (participe.getCodigo() recién existe acá) — registrarNovedad
				// no hace nada si getCodigo() es null. ⚠️ El campo plano NO se borra ni se
				// reemplaza — sigue existiendo igual que antes; verificar en la pantalla que la
				// misma novedad no se vea duplicada (una vez por el campo, otra por la fila NVPC).
				if (participe.getNovedadesFinancieras() != null) {
					long tipoFinanciera = participe.getNovedadesFinancieras().longValue();
					if (tipoFinanciera == ASPNovedadesCargaArchivo.SIN_DESCUENTOS
							|| tipoFinanciera == ASPNovedadesCargaArchivo.DESCUENTOS_INCOMPLETOS) {
						double montoRecibido = nullSafe(participe.getTotalDescontado());
						double montoEsperado = montoRecibido + nullSafe(participe.getCapitalNoDescontado())
								+ nullSafe(participe.getInteresNoDescontado())
								+ nullSafe(participe.getDesgravamenNoDescontado());
						registrarNovedad(participe, (int) tipoFinanciera,
								tipoFinanciera == ASPNovedadesCargaArchivo.SIN_DESCUENTOS
										? "No se realizó ningún descuento para este partícipe (validación de carga)."
										: "Descuento incompleto: falta capital, interés o desgravamen por"
												+ " descontar (validación de carga).",
								null, null, montoEsperado, montoRecibido);
					}
				}

					// 2026-09-02 (decisión del usuario, tras la carga 449): toda novedad
					// ESTRUCTURAL (novedadesCarga) que efectivamente bloquee el procesamiento
					// tiene que generar también su fila NVPC — si no, el partícipe bloquea la
					// carga y es INVISIBLE en la pantalla de novedades: el mensaje de error pide
					// "registre en las novedades cómo aplicar cada valor", pero no hay ninguna
					// fila contra la cual registrar nada. Fue exactamente el callejón sin salida
					// de la carga 449 (30 casos de CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE, 26 sin
					// ninguna fila NVPC), destrabado a mano con sql/163.
					//
					// "Bloquea" se resuelve con tipoEstructuralBloquea — LA MISMA función que
					// usa novedadesQueRequierenAfectacion, no una lista aparte: así el tipo 4 en
					// Petrocomercial (que dejó de bloquear, ver esFilialPetrocomercial) deja de
					// generar fila acá también, sin mantener dos listas sincronizadas a mano.
					//
					// Va DESPUÉS del INSERT (participe.getCodigo() recién existe acá), igual que
					// el bloque de novedadesFinancieras de arriba. Guarda contra duplicados por
					// si el reproceso alguna vez reutiliza el mismo PXCA en vez de insertar uno
					// nuevo.
					if (tipoEstructuralBloquea(participe.getNovedadesCarga(), codigoFilial)) {
						long tipoCarga = participe.getNovedadesCarga().longValue();
						// registrarNovedad ya es idempotente (§16) — el guard manual de
						// "yaExisteNvpc" que estaba acá quedó redundante, lo hace por sí solo.
						//
						// §16 (2026-09-03): si el producto es PH/PP, intenta sumar el HS del
						// mismo partícipe — MEJOR ESFUERZO acá, no garantizado: Fase 1 corre
						// partícipe por detalle, DENTRO del mismo bucle que inserta las filas,
						// así que el HS puede no estar insertado todavía cuando le toca el turno
						// al PH (depende del orden de los DetalleCargaArchivo). Por eso esto NO
						// reemplaza a la revalidación (ver VALIDACION-TOPE-AFECTACION-MANUAL.md
						// §16): revalidarCarga corre esta MISMA lógica después de que TODO esté
						// insertado, momento en el que el HS SIEMPRE está disponible si existe.
						double montoRecibido = montoConHsMergeado(participe, codigoProducto, cargaArchivoGuardado);
						registrarNovedad(participe, (int) tipoCarga,
							"Novedad de identificación de fase 1 (validación de carga): "
								+ describirNovedades(Arrays.asList(participe.getNovedadesCarga())) + ".",
							null, null, 0.0, montoRecibido);
					}
			}
		}
        }
		
		// ==========================================
		// FASE 2: VALIDACIONES AVANZADAS
		// Se ejecutan DESPUÉS de que TODOS los registros estén en BD
		// Esto permite que las validaciones de PH encuentren los registros HS correspondientes
		// ==========================================
		ejecutarValidacionesFase2(cargaArchivoGuardado, participesXCargaArchivo);
		
		return cargaArchivoGuardado;
	}
    
    /**
	 * Ejecuta las validaciones de FASE 2 después de que TODOS los registros estén almacenados
	 * Esto garantiza que cuando se validan productos PH/PE, los registros HS ya existen en BD
	 * 
	 * @param cargaArchivo El registro de CargaArchivo con los detalles
	 * @param participesXCargaArchivo Lista de todos los partícipes procesados
	 */
	private void ejecutarValidacionesFase2(CargaArchivo cargaArchivo, List<ParticipeXCargaArchivo> participesXCargaArchivo) {
		// Logs innecesarios eliminados para optimizar rendimiento

		// Memoización de productoDaoService.selectAllByCodigoPetro (2026-09-02), acotada al
		// alcance de ESTA invocación: dentro de una carga, codigoProducto toma un puñado de
		// valores distintos que se repiten una vez por partícipe (cientos/miles) — misma
		// consulta, mismos parámetros. Variable LOCAL a propósito, nunca campo de instancia ni
		// estático: este bean es @Stateful, y un campo así sería estado compartido entre
		// invocaciones/cargas distintas. Ver resolverProductosPorCodigo.
		Map<String, List<Producto>> productosCache = new HashMap<>();

		for (ParticipeXCargaArchivo participe : participesXCargaArchivo) {
			String codigoProducto = participe.getDetalleCargaArchivo() != null ?
									participe.getDetalleCargaArchivo().getCodigoPetroProducto() : null;

			if (codigoProducto == null) {
				continue;
			}

			// ✅ CORRECCIÓN: Solo excluir HS de validaciones Fase 2
			// El producto AH (aportes) SÍ debe validarse en Fase 2
			boolean esProductoHS = CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto);

			if (!esProductoHS && participe.getCodigo() != null) {
				try {
					validarNovedadesFase2(participe, codigoProducto, cargaArchivo, productosCache);
				} catch (Throwable e) {
					System.err.println("ERROR en validaciones Fase 2 para partícipe " +
									   participe.getCodigoPetro() + ": " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
    
    // Filtrar partícipes por código específico de DetalleCargaArchivo
    public List<ParticipeXCargaArchivo> filtrarPorCodigoDetalle(List<ParticipeXCargaArchivo> participesXCargaArchivo,
                                                              Long codigoDetalle) {
        return participesXCargaArchivo.stream()
            .filter(participe -> participe.getDetalleCargaArchivo() != null && 
                               participe.getDetalleCargaArchivo().getCodigo().equals(codigoDetalle))
            .collect(Collectors.toList());
    }
    
    /**
     * Lee el contenido completo del archivo
     * Usa ISO-8859-1 para leer correctamente caracteres especiales como ñ, á, é, í, ó, ú
     */
    private String leerContenidoArchivo(InputStream inputStream) throws Exception {
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                contenido.append(linea).append("\n");
            }
        }
        return contenido.toString();
    }
    
    /**
     * Procesa el contenido del archivo y extrae los registros
     */
    private List<ParticipeXCargaArchivo> procesarContenido(String contenido) throws Exception {
        String[] lineas = contenido.split("\n");
        List<ParticipeXCargaArchivo> registrosProcesados = new ArrayList<>();
        int i = 0;
        int numeroLinea = 1; // Para rastrear la línea en caso de error
        boolean encontroEP = false;

        // 2026-08-31: cuenta las líneas que el reporte de Petro imprime al cierre de cada
        // sección de producto ("TOTAL <producto>", "TOTAL ZONA...", "TOTAL ==>") y que NO
        // son un partícipe — se detectan porque no traen código en columnas [0,7). Antes se
        // descartaban en silencio DESPUÉS de intentar parsear sus columnas numéricas (lo que
        // podía reventar si el texto de la línea invadía una columna numérica — ver
        // REGLAS-CARGA-PETRO.md §"líneas TOTAL"); ahora se detectan ANTES y ni siquiera se
        // tocan esas columnas. Se cuenta para que un cambio de formato de Petro (de 8 líneas
        // TOTAL a, digamos, 500) se note por un número en el log, no porque falten partícipes.
        int lineasResumenOmitidas = 0;
        
        while (i < lineas.length) {
            String lineaActual = lineas[i];
            
            if (lineaActual != null && lineaActual.trim().startsWith("EP")) {
                encontroEP = true;
                i += 8;
                if (i >= lineas.length) break;
                
                String lineaAporte = lineas[i];
                
                // Validar que la línea de aporte tenga contenido
                if (lineaAporte == null || lineaAporte.trim().isEmpty()) {
                    throw new IllegalArgumentException("Error en línea " + (numeroLinea + 8) + ": Se esperaba línea de producto/aporte después del encabezado EP");
                }
                
                String codigoAporte = lineaAporte.substring(0, Math.min(4, lineaAporte.length())).trim();
                String descripcionAporte = lineaAporte.length() > 4 ? lineaAporte.substring(4).trim() : "";
                
                // Validar que exista código de aporte
                if (codigoAporte.isEmpty()) {
                    // throw new IllegalArgumentException("Error en línea " + (numeroLinea + 8) + ": Código de producto/aporte vacío");
                }
                
                i++;
                i++;
                if (i >= lineas.length) break;
                
                while (i < lineas.length) {
                    String lineaRegistro = lineas[i];
                    
                    if (lineaRegistro != null && lineaRegistro.trim().startsWith("EP")) {
                        break;
                    }
                    
                    if (lineaRegistro != null && lineaRegistro.trim().length() > 0) {
                        // Línea de resumen del reporte (TOTAL de la sección, TOTAL ==>, etc.):
                        // no trae código de partícipe en columnas [0,7). Se detecta y se salta
                        // ANTES de tocar ninguna columna numérica — no es un dato de partícipe
                        // mal formado, es una línea que nunca debió leerse como partícipe.
                        // extraerCampo rellena con espacios si la línea es corta, así que es
                        // segura incluso si lineaRegistro tiene menos de 7 caracteres.
                        String codigo = extraerCampo(lineaRegistro, 0, 7).trim();
                        if (codigo.isEmpty()) {
                            lineasResumenOmitidas++;
                            i++;
                            continue;
                        }

                        try {
                            ParticipeXCargaArchivo registro = new ParticipeXCargaArchivo();

                            // Crear un DetalleCargaArchivo temporal para identificación
                            DetalleCargaArchivo detalleTemp = new DetalleCargaArchivo();
                            detalleTemp.setCodigoPetroProducto(codigoAporte);
                            detalleTemp.setNombreProductoPetro(descripcionAporte);
                            registro.setDetalleCargaArchivo(detalleTemp);

                            // Validar que la línea tenga la longitud mínima esperada
                            if (lineaRegistro.length() < 50) {
                                throw new IllegalArgumentException("Línea muy corta, longitud mínima esperada: 50 caracteres");
                            }

                            // Extraer campos del registro (codigo ya extraído arriba)
                            registro.setNombre(extraerCampo(lineaRegistro, 7, 44).trim());
                            registro.setPlazoInicial(parseDouble(extraerCampo(lineaRegistro, 44, 50).trim()).longValue());
                            registro.setSaldoActual(parseDouble(extraerCampo(lineaRegistro, 50, 61).trim()));
                            registro.setMesesPlazo(parseDouble(extraerCampo(lineaRegistro, 61, 65).trim()).longValue());
                            registro.setInteresAnual(parseDouble(extraerCampo(lineaRegistro, 65, 70).trim()));
                            registro.setValorSeguro(parseDouble(extraerCampo(lineaRegistro, 70, 80).trim()));
                            registro.setMontoDescontar(parseDouble(extraerCampo(lineaRegistro, 80, 95).trim()));
                            registro.setCapitalDescontado(parseDouble(extraerCampo(lineaRegistro, 95, 110).trim()));
                            registro.setInteresDescontado(parseDouble(extraerCampo(lineaRegistro, 110, 125).trim()));
                            registro.setSeguroDescontado(parseDouble(extraerCampo(lineaRegistro, 125, 140).trim()));
                            registro.setTotalDescontado(parseDouble(extraerCampo(lineaRegistro, 140, 155).trim()));
                            registro.setCapitalNoDescontado(parseDouble(extraerCampo(lineaRegistro, 155, 170).trim()));
                            registro.setInteresNoDescontado(parseDouble(extraerCampo(lineaRegistro, 170, 184).trim()));
                            registro.setDesgravamenNoDescontado(parseDouble(extraerCampo(lineaRegistro, 184, 198).trim()));

                            registro.setCodigoPetro(parseLongSimple(codigo));
                            registrosProcesados.add(registro);
                        } catch (Exception e) {
                            // Texto crudo alrededor de donde probablemente cayó el corte, para
                            // que el operador vea el problema sin abrir el archivo. No sabemos
                            // con certeza qué columna exacta falló (son 12 parseDouble en la
                            // misma línea); mostramos la línea completa recortada — el valor
                            // mal formado ya viene entre comillas en el mensaje de parseDouble.
                            String lineaCruda = lineaRegistro.length() > 120
                                    ? lineaRegistro.substring(0, 120) + "..." : lineaRegistro;
                            throw new IllegalArgumentException("Error al procesar línea " + (numeroLinea + i)
                                    + " del producto '" + descripcionAporte + "' (columnas de dato: 0-198): "
                                    + e.getMessage() + " | Línea cruda: \"" + lineaCruda + "\"", e);
                        }
                    }
                    
                    i++;
                }
                
                numeroLinea = i;
                continue;
            }
            
            i++;
            numeroLinea++;
        }
        
        // Validar que se haya encontrado al menos un encabezado EP
        if (!encontroEP) {
            throw new IllegalArgumentException("El archivo no contiene ningún encabezado 'EP' válido. Formato de archivo incorrecto.");
        }

        // Reportado por número, no por "faltan partícipes": si el formato de Petro cambia y
        // de golpe hay 500 de estas en vez de las ~8-15 de siempre (una por sección de
        // producto), es la señal de que hay que revisar el archivo, no un partícipe perdido.
        System.out.println("ℹ️ Líneas de resumen del reporte omitidas (sin código de partícipe,"
                + " p.ej. \"TOTAL <producto>\"): " + lineasResumenOmitidas);

        return registrosProcesados;
    }
    
    /**
     * Extrae un campo de una línea
     */
    private String extraerCampo(String linea, int inicio, int fin) {
        if (linea == null) return "";
        
        // Rellenar con espacios si la línea es más corta
        StringBuilder lineaCompleta = new StringBuilder(linea);
        while (lineaCompleta.length() < fin) {
            lineaCompleta.append(" ");
        }
        
        return lineaCompleta.substring(inicio, Math.min(fin, lineaCompleta.length()));
    }
    
    /**
     * Agrupa registros por código de aporte (crea DetalleCargaArchivo)
     */
    private Map<String, DetalleCargaArchivo> agruparPorAporte(List<ParticipeXCargaArchivo> registrosProcesados) {
        Map<String, DetalleCargaArchivo> mapaAportes = new HashMap<>();
        
        for (ParticipeXCargaArchivo registro : registrosProcesados) {
            String key = registro.getDetalleCargaArchivo().getCodigoPetroProducto();
            
            if (!mapaAportes.containsKey(key)) {
                DetalleCargaArchivo detalle = new DetalleCargaArchivo();
                detalle.setCodigoPetroProducto(registro.getDetalleCargaArchivo().getCodigoPetroProducto());
                detalle.setNombreProductoPetro(registro.getDetalleCargaArchivo().getNombreProductoPetro());
                detalle.setTotalParticipes(0.0);
                detalle.setTotalSaldoActual(0.0);
                detalle.setTotalInteresAnual(0.0);
                detalle.setTotalValorSeguro(0.0);
                detalle.setTotalDescontar(0.0);
                detalle.setTotalCapitalDescontado(0.0);
                detalle.setTotalInteresDescontado(0.0);
                detalle.setTotalSeguroDescontado(0.0);
                detalle.setTotalDescontado(0.0);
                detalle.setTotalCapitalNoDescontado(0.0);
                detalle.setTotalInteresNoDescontado(0.0);
                detalle.setTotalDesgravamenNoDescontado(0.0);
                
                mapaAportes.put(key, detalle);
            }
            
            DetalleCargaArchivo aporte = mapaAportes.get(key);
            
            // Actualizar el DetalleCargaArchivo del registro para que apunte al agrupado
            registro.setDetalleCargaArchivo(aporte);
            
            // Acumular totales
            aporte.setTotalParticipes(aporte.getTotalParticipes() + 1.0);
            aporte.setTotalSaldoActual(aporte.getTotalSaldoActual() + nullSafe(registro.getSaldoActual()));
            aporte.setTotalInteresAnual(aporte.getTotalInteresAnual() + nullSafe(registro.getInteresAnual()));
            aporte.setTotalValorSeguro(aporte.getTotalValorSeguro() + nullSafe(registro.getValorSeguro()));
            aporte.setTotalDescontar(aporte.getTotalDescontar() + nullSafe(registro.getMontoDescontar()));
            aporte.setTotalCapitalDescontado(aporte.getTotalCapitalDescontado() + nullSafe(registro.getCapitalDescontado()));
            aporte.setTotalInteresDescontado(aporte.getTotalInteresDescontado() + nullSafe(registro.getInteresDescontado()));
            aporte.setTotalSeguroDescontado(aporte.getTotalSeguroDescontado() + nullSafe(registro.getSeguroDescontado()));
            aporte.setTotalDescontado(aporte.getTotalDescontado() + nullSafe(registro.getTotalDescontado()));
            aporte.setTotalCapitalNoDescontado(aporte.getTotalCapitalNoDescontado() + nullSafe(registro.getCapitalNoDescontado()));
            aporte.setTotalInteresNoDescontado(aporte.getTotalInteresNoDescontado() + nullSafe(registro.getInteresNoDescontado()));
            aporte.setTotalDesgravamenNoDescontado(aporte.getTotalDesgravamenNoDescontado() + nullSafe(registro.getDesgravamenNoDescontado()));
        }
        
        return mapaAportes;
    }
    
    /**
     * Calcula totales generales para CargaArchivo
     */
    private CargaArchivo calcularTotalesGenerales(CargaArchivo cargaArchivo, List<DetalleCargaArchivo> aporteAgrupados) {
        double totalSaldoActual = 0;
        double totalInteresAnual = 0;
        double totalValorSeguro = 0;
        double totalDescontar = 0;
        double totalCapitalDescontado = 0;
        double totalInteresDescontado = 0;
        double totalSeguroDescontado = 0;
        double totalDescontado = 0;
        double totalCapitalNoDescontado = 0;
        double totalInteresNoDescontado = 0;
        double totalDesgravamenNoDescontado = 0;
        
        for (DetalleCargaArchivo aporte : aporteAgrupados) {
            totalSaldoActual += nullSafe((double)aporte.getTotalSaldoActual());
            totalInteresAnual += nullSafe((double)aporte.getTotalInteresAnual());
            totalValorSeguro += nullSafe((double)aporte.getTotalValorSeguro());
            totalDescontar += nullSafe((double)aporte.getTotalDescontar());
            totalCapitalDescontado += nullSafe((double)aporte.getTotalCapitalDescontado());
            totalInteresDescontado += nullSafe((double)aporte.getTotalInteresDescontado());
            totalSeguroDescontado += nullSafe((double)aporte.getTotalSeguroDescontado());
            totalDescontado += nullSafe((double)aporte.getTotalDescontado());
            totalCapitalNoDescontado += nullSafe((double)aporte.getTotalCapitalNoDescontado());
            totalInteresNoDescontado += nullSafe((double)aporte.getTotalInteresNoDescontado());
            totalDesgravamenNoDescontado += nullSafe((double)aporte.getTotalDesgravamenNoDescontado());
        }
        
        cargaArchivo.setTotalSaldoActual(totalSaldoActual);
        cargaArchivo.setTotalInteresAnual(totalInteresAnual);
        cargaArchivo.setTotalValorSeguro(totalValorSeguro);
        cargaArchivo.setTotalDescontar(totalDescontar);
        cargaArchivo.setTotalCapitalDescontado(totalCapitalDescontado);
        cargaArchivo.setTotalInteresDescontado(totalInteresDescontado);
        cargaArchivo.setTotalSeguroDescontado(totalSeguroDescontado);
        cargaArchivo.setTotalDescontado(totalDescontado);
        cargaArchivo.setTotalCapitalNoDescontado(totalCapitalNoDescontado);
        cargaArchivo.setTotalInteresNoDescontado(totalInteresNoDescontado);
        cargaArchivo.setTotalDesgravamenNoDescontado(totalDesgravamenNoDescontado);
        
        return cargaArchivo;
    }
    
    /**
     * Convierte string a número manejando formatos europeos
     */
    /**
     * TODO O NADA (2026-08-29): un campo VACÍO o ausente es un dato legítimo — sigue devolviendo
     * 0.0, sin cambios. Pero un campo PRESENTE con un valor mal formado ya NO se convierte en
     * 0.0 en silencio: se propaga. Antes, este método se comía el NumberFormatException, así
     * que el abort correcto que ya existe en procesarContenido (línea ~504, envuelve cada línea
     * en un IllegalArgumentException con el número de línea) nunca llegaba a dispararse para un
     * monto corrupto — el campo garbled quedaba en $0 sin ningún aviso, lo que podía marcar
     * EN_MORA a un partícipe que en realidad sí pagó. No es una regla nueva: es hacer que
     * funcione la que ya estaba escrita en la línea 504.
     */
    private Double parseDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0.0;

        // Limpiar espacios
        String valorLimpio = valor.trim().replaceAll("\\s", "");

        boolean tieneComa = valorLimpio.contains(",");
        boolean tienePunto = valorLimpio.contains(".");

        if (tieneComa && tienePunto) {
            // Formato europeo: 1.234.567,89 -> 1234567.89
            valorLimpio = valorLimpio.replace(".", "").replace(",", ".");
        } else if (tieneComa) {
            // Solo comas: 1234,89 -> 1234.89
            valorLimpio = valorLimpio.replace(",", ".");
        }

        try {
            return Double.parseDouble(valorLimpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico mal formado: '" + valor + "'", e);
        }
    }

    /**
     * Convierte string a Long simple (sin decimales). Mismo criterio que {@link #parseDouble}:
     * vacío/ausente es 0L legítimo, presente-pero-mal-formado propaga.
     */
    private Long parseLongSimple(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0L;

        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico mal formado: '" + valor + "'", e);
        }
    }
    
    /**
     * Devuelve 0 si el valor es null
     */
    private Double nullSafe(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /**
     * Total BASE de la cuota: lo que el archivo de Petrocomercial viene a cobrar, SIN el
     * interés de mora ni el interés vencido.
     *
     * Desde que existe el proceso diario de mora
     * ({@code com.saa.ejb.crd.service.ProcesoMoraPrestamoService}), la columna DTPRTTLL de una
     * cuota vencida incluye la mora acumulada. Este proceso NO debe verla:
     *
     * <ul>
     *   <li>La fase 2 compara DTPRTTLL contra el monto del archivo con tolerancia de $1. Con la
     *       mora adentro, toda cuota vencida daría MONTO_INCONSISTENTE (13), que está en
     *       NOVEDADES_REQUIERE_AFECTACION_MANUAL y bloquearía la fase 3 completa.</li>
     *   <li>La prelación de la fase 3 solo reparte entre desgravamen, interés, capital y seguro
     *       de incendio: no tiene componente de mora, así que jamás podría agotar un
     *       totalPendiente que la incluya y toda cuota vencida quedaría PARCIAL en vez de
     *       PAGADA.</li>
     * </ul>
     *
     * Restar mora e interés vencido devuelve exactamente el valor que esta clase leía antes de
     * que existiera el proceso diario, de modo que el comportamiento del módulo Petro no cambia.
     * La mora de las cuotas vencidas la cobra el motor de pagos de préstamos
     * ({@code MotorPagoPrestamoService}), que sí tiene el componente en su prelación.
     *
     * @param cuota Cuota a evaluar
     * @return Total de la cuota sin mora ni interés vencido
     */
    private Double totalBaseCuota(DetallePrestamo cuota) {
        if (cuota == null) {
            return 0.0;
        }
        return nullSafe(cuota.getTotal()) - nullSafe(cuota.getMora()) - nullSafe(cuota.getInteresVencido());
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public CargaArchivo validarArchivoPetro(InputStream archivoInputStream, String fileName, CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("Iniciando validarArchivoPetro: " + fileName);
		
		// VALIDACIÓN 1: Verificar que el archivo tenga extensión .txt
		if (fileName == null || !fileName.toLowerCase().endsWith(".txt")) {
			throw new IllegalArgumentException("El archivo debe tener extensión .txt. Archivo recibido: " + fileName);
		}
		
		// Leer el InputStream completo en un byte array para poder reutilizarlo
		byte[] archivoBytes = archivoInputStream.readAllBytes();
		
		// VALIDACIÓN 2: Verificar que el archivo no esté vacío
		if (archivoBytes == null || archivoBytes.length == 0) {
			throw new IllegalArgumentException("El archivo está vacío");
		}
		
		// Crear un nuevo InputStream desde el byte array para leer el contenido
		java.io.ByteArrayInputStream contenidoStream = new java.io.ByteArrayInputStream(archivoBytes);
		String contenido = leerContenidoArchivo(contenidoStream);
		
		// VALIDACIÓN 3: Verificar que el contenido tenga el formato correcto (debe empezar con "EP")
		if (contenido == null || contenido.trim().isEmpty()) {
			throw new IllegalArgumentException("El archivo no tiene contenido legible");
		}
		
		if (!contenido.trim().startsWith("EP")) {
			throw new IllegalArgumentException("El formato del archivo es incorrecto. El archivo debe comenzar con 'EP' según el formato PETROCOMERCIAL");
		}
		
        List<ParticipeXCargaArchivo> registrosProcesados = procesarContenido(contenido);
        
        // VALIDACIÓN 4: Verificar que se hayan procesado registros
        if (registrosProcesados == null || registrosProcesados.isEmpty()) {
        	throw new IllegalArgumentException("No se encontraron registros válidos en el archivo. Verifique que el formato sea correcto");
        }
        
        // Agrupar por aporte (DetalleCargaArchivo)
        Map<String, DetalleCargaArchivo> aporteAgrupados = agruparPorAporte(registrosProcesados);
        
        // VALIDACIÓN 5: Verificar que se hayan generado detalles agrupados
        if (aporteAgrupados == null || aporteAgrupados.isEmpty()) {
        	throw new IllegalArgumentException("No se pudieron agrupar los registros por producto. Verifique el formato del archivo");
        }
        
        // Convertir a listas para persistir
        List<DetalleCargaArchivo> detallesGenerados = new ArrayList<>(aporteAgrupados.values());
        // Calcular totales generales para CargaArchivo
        cargaArchivo = calcularTotalesGenerales(cargaArchivo, detallesGenerados);
        
        List <Entidad> entidadesPetro35 = entidadDaoService.selectByNombrePetro35("ALVAREZ TOAPANTA DAYUMA");
        System.out.println("registros recuperados:" + entidadesPetro35.size());
        for(Entidad entPetro35 : entidadesPetro35) {
        	System.out.println("Entidad encontrada por nombre petro 35: " + entPetro35.getCodigo() + " - " + entPetro35.getRolPetroComercial());
        }
        // 1. PRIMERO: Almacenar registros en BD (TRANSACCIONAL)
        CargaArchivo cargaArchivoGuardado = almacenaRegistros(cargaArchivo, detallesGenerados, registrosProcesados);
		
        // 2. AL FINAL: Cargar el archivo físico (NO TRANSACCIONAL)
        // Crear un nuevo InputStream desde el byte array para cargar el archivo
        java.io.ByteArrayInputStream archivoStream = new java.io.ByteArrayInputStream(archivoBytes);
        String rutaArchivo = cargarArchivo(archivoStream, fileName, cargaArchivo);
        System.out.println("Archivo cargado en: " + rutaArchivo);
        
        cargaArchivoGuardado.setRutaArchivo(rutaArchivo);
        cargaArchivoGuardado = cargaArchivoService.saveSingle(cargaArchivoGuardado);
        
        System.out.println("Procesamiento completado exitosamente");
        return cargaArchivoGuardado;
	}

	@Override
	public ParticipeXCargaArchivo actualizaCodigoPetroEntidad(Long codigoPetro, Long idParticipeXCarga, Long idEntidad) throws Throwable {
		System.out.println("actualizaCodigoPetro");
		ParticipeXCargaArchivo participe = participeXCargaArchivoService.selectById(idParticipeXCarga);
		Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
		if (entidad != null) {
			entidad.setRolPetroComercial(codigoPetro);
			entidad = entidadDaoService.save(entidad, entidad.getCodigo());
			// Actualizar el ParticipeXCargaArchivo asociado si es necesario
			participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.OK));
			participeXCargaArchivoService.saveSingle(participe);
		}
		return participe;
	}
	
	/**
	 * Registra (o ACTUALIZA, si ya existe una del mismo tipo para esa fila) una novedad en la
	 * tabla hija NovedadParticipeCarga.
	 *
	 * <b>IDEMPOTENTE desde 2026-09-03 (§16, revalidación de carga):</b> antes SIEMPRE insertaba
	 * una fila nueva. Ahora busca primero si esta fila PXCA ya tiene una NovedadParticipeCarga
	 * del mismo {@code tipoNovedad} (activa O inactiva — ver {@link #revalidarCarga}, que
	 * reactiva una desactivada en vez de duplicarla) y, si existe, la actualiza (y la reactiva,
	 * {@code estado=1}) en lugar de crear una segunda. Necesario para que
	 * {@link #revalidarCarga} pueda re-ejecutar Fase 1/2 tantas veces como haga falta sin
	 * duplicar novedades — con el §10 (cuarta vuelta) sumando pozos sin clave de dedup para
	 * {@code codigoPrestamo == null}, una duplicación inflaría el pozo (el defecto de SANCHEZ,
	 * otra vez). El comportamiento de Fase 1/2 durante una carga NUEVA no cambia: ahí nunca
	 * existe una fila previa para (participe, tipo), así que esto sigue insertando igual que
	 * antes.
	 *
	 * <p>Además, SIEMPRE anota en {@link #tiposConfirmadosRevalidacion} qué (fila, tipo) acaba
	 * de confirmar — lo lee {@link #revalidarCarga} para decidir qué novedades YA NO
	 * corresponden (no fueron reconfirmadas en la corrida). Fuera de una revalidación esto se
	 * llena igual pero nadie lo lee, así que es inofensivo.</p>
	 *
	 * @return {@code true} si se CREÓ una fila nueva, {@code false} si se ACTUALIZÓ una
	 *         existente (o si no se hizo nada por un {@code participe} inválido)
	 * @param participe El partícipe relacionado
	 * @param tipoNovedad Tipo de novedad (código del rubro)
	 * @param descripcion Descripción de la novedad
	 * @param codigoProducto Código del producto relacionado (opcional)
	 * @param codigoPrestamo Código del préstamo relacionado (opcional)
	 * @param montoEsperado Monto esperado del sistema (opcional)
	 * @param montoRecibido Monto recibido del archivo (opcional)
	 */
	private boolean registrarNovedad(ParticipeXCargaArchivo participe, int tipoNovedad, String descripcion,
								  Long codigoProducto, Long codigoPrestamo, Double montoEsperado, Double montoRecibido) {
		try {
			if (participe == null || participe.getCodigo() == null) {
				return false;
			}

			NovedadParticipeCarga novedad = buscarNovedadExistente(participe.getCodigo(), tipoNovedad);
			boolean esNueva = novedad == null;
			if (esNueva) {
				novedad = new NovedadParticipeCarga();
				ParticipeXCargaArchivo participeRef = new ParticipeXCargaArchivo();
				participeRef.setCodigo(participe.getCodigo());
				novedad.setParticipeXCargaArchivo(participeRef);
			}

			novedad.setTipoNovedad(Long.valueOf(tipoNovedad));
			novedad.setDescripcion(descripcion);
			novedad.setCodigoProducto(codigoProducto);
			novedad.setCodigoPrestamo(codigoPrestamo);
			novedad.setMontoEsperado(montoEsperado);
			novedad.setMontoRecibido(montoRecibido);

			if (montoEsperado != null && montoRecibido != null) {
				// Diferencia CON SIGNO:
				// Negativa = Falta dinero (recibido < esperado)
				// Positiva = Sobra dinero (recibido > esperado)
				novedad.setMontoDiferencia(montoRecibido - montoEsperado);
			} else {
				novedad.setMontoDiferencia(null);
			}

			// Llenar código de carga archivo desde el detalle del partícipe
			if (participe.getDetalleCargaArchivo() != null &&
			    participe.getDetalleCargaArchivo().getCargaArchivo() != null) {
				novedad.setCodigoCargaArchivo(participe.getDetalleCargaArchivo().getCargaArchivo().getCodigo());
			}

			// Llenar idAsoprep del préstamo si está disponible
			if (codigoPrestamo != null) {
				try {
					Prestamo prestamo = prestamoDaoService.selectById(codigoPrestamo, "Prestamo");
					if (prestamo != null && prestamo.getIdAsoprep() != null) {
						novedad.setIdAsoprepPrestamo(prestamo.getIdAsoprep());
					}
				} catch (Throwable e) {
					// Si falla, continuar sin el idAsoprep
				}
			}

			// SIEMPRE 1 (activa) — si esto se llama es porque el código actual CONFIRMA que la
			// novedad corresponde; si estaba desactivada por una revalidación anterior, se
			// reactiva en vez de quedar fantasma.
			novedad.setEstado(1L);
			novedadParticipeCargaService.saveSingle(novedad);

			tiposConfirmadosRevalidacion.computeIfAbsent(participe.getCodigo(), k -> new HashSet<>())
				.add(Long.valueOf(tipoNovedad));

			return esNueva;
		} catch (Throwable e) {
			System.err.println("Error al registrar novedad: " + descripcion);
			e.printStackTrace();
			return false;
		}
	}

	/** Busca, para idempotencia, una NovedadParticipeCarga del mismo tipo en esta fila PXCA —
	 * SIN filtrar por estado a propósito: una desactivada tiene que encontrarse igual, para
	 * reactivarla en vez de crear una segunda. */
	private NovedadParticipeCarga buscarNovedadExistente(Long codigoParticipe, int tipoNovedad) throws Throwable {
		List<NovedadParticipeCarga> existentes = novedadParticipeCargaDaoService.selectByParticipe(codigoParticipe);
		if (existentes == null) {
			return null;
		}
		for (NovedadParticipeCarga existente : existentes) {
			if (existente.getTipoNovedad() != null && existente.getTipoNovedad().intValue() == tipoNovedad) {
				return existente;
			}
		}
		return null;
	}
	
	/**
	 * Valida si todos los valores financieros del partícipe son cero (Novedad 8)
	 * 
	 * @param participe El registro del partícipe a validar
	 * @return true si todos los valores financieros son cero, false en caso contrario
	 */
	private boolean validarValoresCero(ParticipeXCargaArchivo participe) {
		// Verificar que todos los valores financieros principales sean cero
		boolean saldoActualCero = (participe.getSaldoActual() == null || participe.getSaldoActual() == 0.0);
		boolean montoDescontarCero = (participe.getMontoDescontar() == null || participe.getMontoDescontar() == 0.0);
		boolean capitalDescontadoCero = (participe.getCapitalDescontado() == null || participe.getCapitalDescontado() == 0.0);
		boolean interesDescontadoCero = (participe.getInteresDescontado() == null || participe.getInteresDescontado() == 0.0);
		boolean seguroDescontadoCero = (participe.getSeguroDescontado() == null || participe.getSeguroDescontado() == 0.0);
		boolean totalDescontadoCero = (participe.getTotalDescontado() == null || participe.getTotalDescontado() == 0.0);
		boolean capitalNoDescontadoCero = (participe.getCapitalNoDescontado() == null || participe.getCapitalNoDescontado() == 0.0);
		boolean interesNoDescontadoCero = (participe.getInteresNoDescontado() == null || participe.getInteresNoDescontado() == 0.0);
		boolean desgravamenNoDescontadoCero = (participe.getDesgravamenNoDescontado() == null || participe.getDesgravamenNoDescontado() == 0.0);
		
		// Si TODOS los valores financieros son cero, retornar true
		return saldoActualCero && 
		       montoDescontarCero && 
		       capitalDescontadoCero && 
		       interesDescontadoCero && 
		       seguroDescontadoCero && 
		       totalDescontadoCero && 
		       capitalNoDescontadoCero && 
		       interesNoDescontadoCero && 
		       desgravamenNoDescontadoCero;
	}
	
	/**
	 * Aplica los pagos de un archivo Petro que ya fue validado.
	 * Este método se ejecuta DESPUÉS de que el usuario revisa las novedades.
	 * Solo procesa los registros que están OK o tienen novedades que no bloquean el pago.
	 * 
	 * @param codigoCargaArchivo ID del CargaArchivo a procesar
	 * @return Resumen del procesamiento
	 * @throws Throwable Si ocurre algún error
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public String aplicarPagosArchivoPetro(Long codigoCargaArchivo) throws Throwable {
		System.out.println("=== INICIANDO APLICACIÓN DE PAGOS - Carga: " + codigoCargaArchivo + " ===");

		// Reset AL ENTRAR, no al salir — ver el javadoc de los campos.
		advertenciasVigenciaCargaActual.clear();
		novedadesGeneradasCargaActual.clear();
		sinParticipeCargaActual.clear();
		montoSinParticipeCargaActual = 0.0;
		excedentesDevueltosAplicados.clear();
		detalleExcedentesDevueltos.clear();
		cancelacionesPosterioresMarcadas.clear();
		cuotasSaldadasSinPagoReabiertas.clear();
		prestamosPreparadosReproceso.clear();
		totalCuotasPorPrestamoReproceso.clear();
		aportesContratoAlCorte.clear();
		montoAportesContratoAlCorte = 0.0;
		aportesSinVigenciaDelMes.clear();
		montoAportesSinVigenciaDelMes = 0.0;
		contratosCreadosReproceso.clear();
		montoContratosCreadosReproceso = 0.0;
		filasSinPagoReproceso.clear();
		montoFilasSinPagoReproceso = 0.0;
		hsSinSeguroEnCuota.clear();
		montoHsSinSeguroEnCuota = 0.0;
		montoExcedentesDevueltos = 0.0;
		if (modoReproceso) {
			System.out.println("=== MODO REPROCESO: " + etiquetaReproceso + " ===");
		}

		try {
			// 1. Obtener el CargaArchivo
			CargaArchivo cargaArchivo = cargaArchivoService.selectById(codigoCargaArchivo);
			if (cargaArchivo == null) {
				throw new RuntimeException("No se encontró la carga con ID: " + codigoCargaArchivo);
			}

			// ==========================================
			// VALIDACIÓN: la carga no puede reprocesarse. validarOrdenProcesamiento excluye
			// a la propia carga de la comparación de orden (no valida contra sí misma), así
			// que sin este control la última carga procesada se puede volver a correr y
			// duplica aportes y pagos.
			// ==========================================
			if (cargaArchivo.getEstado() != null && cargaArchivo.getEstado() == 3L) {
				throw new IncomeException("La carga " + codigoCargaArchivo
					+ " ya fue procesada (estado 3). No se puede volver a aplicar pagos sobre"
					+ " ella: duplicaría aportes y pagos de préstamos.");
			}

			// ==========================================
			// VALIDACIÓN: Solo se puede procesar la carga del siguiente mes
			// al último mes procesado (estado 3)
			// MODO REPROCESO (C4): se reprocesa una carga pasada con cargas posteriores ya en
			// estado 3 — el orden cronológico lo garantiza el operador (se reprocesa de la más
			// antigua a la más nueva), no esta validación.
			// ==========================================
			if (!modoReproceso) {
				validarOrdenProcesamiento(cargaArchivo);
			}
			// ==========================================

			// ==========================================
			exigeConfirmacionContabilidad(cargaArchivo);
			// ==========================================

			// ==========================================
			// MODO REPROCESO (D-010): la lista de excedentes devueltos se valida completa ANTES
			// de tocar nada — cada uno tiene que caer en una fila de esta carga y no superar lo
			// que esa fila descontó. Todo o nada, como el resto de las validaciones.
			// ==========================================
			if (modoReproceso) {
				validarExcedentesDevueltosReproceso(cargaArchivo);
			}

			// ==========================================
			// MODO REPROCESO (D-019 ampliada): ANTES de validar que todo valor tenga destino, se
			// resuelven las operaciones vigentes al corte de cada fila con descuento y se reabren
			// las cuotas del mes que la migración dejó saldadas sin ningún pago (y las operaciones
			// terminales, D-019). Así la validación de abajo y la fase 3 ven la cuota del mes como
			// estaba al corte. Misma transacción: si algo de abajo lanza, esto revierte con todo.
			// ==========================================
			if (modoReproceso) {
				long inicioFase = inicioFaseReproceso("preparar operaciones vigentes al corte");
				prepararOperacionesVigentesAlCorteReproceso(cargaArchivo);
				// Borde de fase (§11 Rendimiento): lo preparado ya está en la base; las
				// validaciones de abajo arrancan con el contexto vacío de cuotas/préstamos.
				int desprendidas = desprenderContextoReproceso();
				finFaseReproceso("preparar operaciones vigentes al corte", inicioFase, "desprendidas al cierre " + desprendidas);
			}

			// ==========================================
			// VALIDACIÓN: todo valor descontado debe tener a qué aplicarse.
			// Si algún registro tiene una novedad que impide determinar el
			// destino y nadie registró la afectación manual, no se procesa nada.
			// MODO REPROCESO (§11 Rendimiento): es de SOLO LECTURA (consulta novedades, AVPC,
			// entidades, préstamos y cuotas; no escribe nada), así que corre con el auto-flush
			// suspendido después de volcar todo: ninguna consulta puede ver algo distinto.
			// ==========================================
			if (modoReproceso) {
				long inicioFase = inicioFaseReproceso("validarValoresConDestino");
				org.hibernate.FlushMode modoAnterior = contextoPersistenciaService.suspenderAutoFlush();
				try {
					validarValoresConDestino(cargaArchivo);
				} finally {
					contextoPersistenciaService.restaurarAutoFlush(modoAnterior, "validarValoresConDestino");
				}
				finFaseReproceso("validarValoresConDestino", inicioFase, null);
			} else {
				validarValoresConDestino(cargaArchivo);
			}
			// ==========================================

			// ==========================================
			// VALIDACIÓN: la afectación manual no puede exceder, POR PARTÍCIPE, lo que se le
			// descontó — VALIDACION-TOPE-AFECTACION-MANUAL.md (2026-09-02, carga 449:
			// $112,30 aplicados que nadie descontó). Todo o nada, evaluando a TODOS los
			// partícipes antes de aplicar el primer pago.
			// MODO REPROCESO (§11 Rendimiento): solo lectura, igual que la anterior.
			// ==========================================
			if (modoReproceso) {
				long inicioFase = inicioFaseReproceso("validarTopeAfectacionManualPorParticipe");
				org.hibernate.FlushMode modoAnterior = contextoPersistenciaService.suspenderAutoFlush();
				try {
					validarTopeAfectacionManualPorParticipe(cargaArchivo);
				} finally {
					contextoPersistenciaService.restaurarAutoFlush(modoAnterior, "validarTopeAfectacionManualPorParticipe");
				}
				// Borde de fase: lo que cargaron las validaciones no se arrastra a la fase 3.
				int desprendidas = desprenderContextoReproceso();
				finFaseReproceso("validarTopeAfectacionManualPorParticipe", inicioFase, "desprendidas al cierre " + desprendidas);
			} else {
				validarTopeAfectacionManualPorParticipe(cargaArchivo);
			}
			// ==========================================

			// ⛔ validarRepartoDeExcedentes(cargaArchivo) — ESCRITA, NO CONECTADA (2026-09-02,
			// decisión del árbitro). Exigía que el reparto manual (AVPC) coincidiera EXACTO
			// con montoDiferencia (el excedente, PLAN-EXCEDENTE-PETRO-A-APORTES.md §5), pero
			// esa regla **contradice** a validarValoresConDestino (arriba), que exige que el
			// reparto cubra el totalDescontado — y cuando hay afectación manual el flujo
			// automático NO corre (verificarYAplicarAfectacionesManualesTotales es
			// "PRIORIDAD MÁXIMA", aplica TODOS los pagos según esos registros; correrlo
			// además del automático pagaría la misma cuota dos veces). Si el reparto fuera
			// solo el excedente, lo esperado (totalDescontado - excedente) nunca se aplicaría.
			// Medido en producción, carga 449: caso BUSTOS ALMEIDA (novedad 43883) —
			// descontado $586,38, esperado $128,20, diferencia (excedente) $458,18. El
			// operador repartió $586,38 (correcto: cubre TODO lo descontado, tal como pide
			// validarValoresConDestino). validarRepartoDeExcedentes lo rechazaba con "se
			// repartió de más $128,20", exactamente el monto esperado que sí debía
			// aplicarse. ~78 novedades así en esa sola carga, y basta UNA para abortarla
			// completa (todo o nada, ver el comentario más abajo).
			// Qué la reemplaza: validarValoresConDestino sigue activa y es la que
			// efectivamente protege — garantiza que todo valor descontado tenga destino, que
			// es lo que no puede fallar. Que el reparto cuadrara contra el excedente y no
			// contra el total era una regla que nunca fue compatible con cómo se aplica.
			// ==========================================

			// ⛔ validarPrelacionReparto(cargaArchivo) — ESCRITA, NO CONECTADA (2026-08-31,
			// decisión del árbitro). El método existe más abajo y funciona para el caso que
			// prueba, pero tiene dos falsos positivos reales que pueden abortar la carga
			// completa de 2.000 partícipes por nada:
			//   1) Agrupa por (novedad, préstamo) — si un partícipe tiene DOS novedades sobre
			//      el MISMO préstamo (A cubre cuota 1, B cubre cuota 3), validar B sola ve la
			//      cuota 1 "pendiente sin cubrir" y revienta, aunque A ya la cubrió.
			//   2) Compara contra selectCuotasPendientesByPrestamoOrdenadas ANTES de que el
			//      flujo automático aplique nada — las cuotas que va a cubrir el descuento
			//      ordinario todavía figuran pendientes en ese momento, así que compara contra
			//      un estado que todavía no es el final.
			// Hoy no hay ningún cliente que pueda mandar un reparto AVPC fuera de orden (el
			// único consumidor es el frontend, que ya ordena y bloquea) — así que esta
			// validación no protege de nada real todavía, y el costo de un falso positivo
			// (frenar la carga del mes) es mucho mayor que el de no tenerla.
			// Para encenderla: agrupar por (participe, préstamo) sobre TODAS las novedades de
			// la carga (no una por una), y comparar contra el estado DESPUÉS de que el flujo
			// automático (paso 2 de este método) ya aplicó lo que pudo — nunca contra el
			// snapshot de pendientes previo a esta corrida.
			// ==========================================

			// 2. ✅ OPTIMIZACIÓN: Obtener SOLO los detalles de esta carga específica
			// En lugar de traer TODOS los detalles de TODAS las cargas con selectAll()
			List<DetalleCargaArchivo> detallesCarga = detalleCargaArchivoDaoService.selectByCargaArchivo(codigoCargaArchivo);

			// Memoización de productoDaoService.selectAllByCodigoPetro (2026-09-02), acotada al
			// alcance de ESTA invocación — ver el comentario gemelo en ejecutarValidacionesFase2.
			Map<String, List<Producto>> productosCache = new HashMap<>();

			// esperado(entidad,tipo,mes) en lote (2026-09-02, auditoría de rendimiento): una
			// sola llamada a VigenciaContratoService#esperadoEnLotePorFilial para TODA la carga
			// en vez de una por partícipe × mes × tipo dentro de distribuirAportePorDevengo (ver
			// el javadoc de esperadoMensual para el detalle y el fallback obligatorio). Se arma
			// SOLO si la carga trae producto AH — es una consulta a nivel de FILIAL completa,
			// no vale la pena dispararla en una carga que no tiene aportes.
			Map<String, Double> esperadoEnLoteAportes = null;
			// CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3.1: qué entidades cubrió el lote,
			// independiente de si tienen alguna vigencia — desambigua el null de
			// esperadoEnLoteAportes ("mes sin vigencia" vs "entidad fuera del lote").
			java.util.Set<Long> entidadesCubiertasAportes = null;
			boolean cargaTieneAportes = detallesCarga != null && detallesCarga.stream()
				.anyMatch(d -> CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(d.getCodigoPetroProducto()));
			if (cargaTieneAportes && cargaArchivo.getFilial() != null
					&& cargaArchivo.getAnioAfectacion() != null && cargaArchivo.getMesAfectacion() != null) {
				java.time.LocalDate mesCargaAportes = java.time.LocalDate.of(
					cargaArchivo.getAnioAfectacion().intValue(), cargaArchivo.getMesAfectacion().intValue(), 1);
				esperadoEnLoteAportes = vigenciaContratoService.esperadoEnLotePorFilial(
					cargaArchivo.getFilial().getCodigo(), ALCANCE_MINIMO_DEVENGO, mesCargaAportes);
				entidadesCubiertasAportes = vigenciaContratoDaoService.selectEntidadesConContratoActivoPorFilial(
					cargaArchivo.getFilial().getCodigo());
			}

			int totalProcesados = 0;
			int totalExitosos = 0;
			int totalErrores = 0;
			int totalOmitidos = 0;
			int totalAportesGenerados = 0;
			int totalAdvertencias = 0;

			// MODO REPROCESO (§11 Rendimiento): medición de la fase 3 y alivio del contexto de
			// persistencia al terminar cada partícipe (aliviarContextoReproceso, abajo).
			long inicioFase3 = modoReproceso ? inicioFaseReproceso("fase 3: aplicar filas") : 0L;
			int desprendidasFase3 = 0;

			// 3. Por cada detalle, procesar los partícipes
			for (DetalleCargaArchivo detalle : detallesCarga) {
				String codigoProducto = detalle.getCodigoPetroProducto();
				
				System.out.println("\n📦 Procesando PRODUCTO: " + codigoProducto);
				
				// ✅ OPTIMIZACIÓN: Obtener SOLO los partícipes de este detalle específico
				// En lugar de traer TODOS los partícipes de TODAS las cargas con selectAll()
				List<ParticipeXCargaArchivo> participesDetalle = 
					participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
				
				System.out.println("  Total partícipes en este producto: " + (participesDetalle != null ? participesDetalle.size() : 0));
				
				// 4. Procesar cada partícipe
				for (ParticipeXCargaArchivo participe : participesDetalle) {
					totalProcesados++;

					// Verificar si tiene novedades que bloquean el procesamiento
						if (tieneNovedadesBloqueantes(participe)) {
							System.out.println("⚠️ Partícipe OMITIDO - Código Petro: " + participe.getCodigoPetro() +
							                   " (" + participe.getNombre() + ") - Producto: " + codigoProducto +
							                   " - Novedad: " + participe.getNovedadesCarga() +
							                   " - Monto: $" + participe.getTotalDescontado());
							totalOmitidos++;
							continue;
						}

						// ==========================================
						// PROCESAMIENTO SEGÚN TIPO DE PRODUCTO
						//
						// TODO O NADA (decisión del usuario, 2026-08-29): si el procesamiento de
						// ESTE partícipe falla, no se atrapa el error para seguir con el
						// siguiente — se aborta toda la carga. Antes, un catch acá contaba el
						// error y seguía, pero la transacción del contenedor ya había quedado
						// STATUS_MARKED_ROLLBACK: el resultado era ni "todo" ni "nada", sino un
						// commit final que fallaba con un error indescifrable apuntando a una
						// consulta inocente muy posterior. Ver REGLAS-CARGA-PETRO.md.
						// ==========================================

						try {
							if (CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto)) {
								// PRODUCTO AH: Generar Aportes. aportesCreados == 0 no es
								// necesariamente un no-evento: puede ser dinero recibido sin
								// aplicar (advertencia de vigencia, o novedad de HistorialSueldo)
								// que NO abortó la carga a propósito — contarlo aparte de
								// totalExitosos para que no quede invisible en el resumen.
								int advertenciasAntes = advertenciasVigenciaCargaActual.size()
									+ novedadesGeneradasCargaActual.size() + sinParticipeCargaActual.size();
								int aportesCreados = aplicarAporteAH(participe, cargaArchivo, esperadoEnLoteAportes,
								entidadesCubiertasAportes);
								totalAportesGenerados += aportesCreados;
								if (aportesCreados > 0) {
									totalExitosos++;
								} else if (advertenciasVigenciaCargaActual.size()
										+ novedadesGeneradasCargaActual.size()
										+ sinParticipeCargaActual.size() > advertenciasAntes) {
									totalAdvertencias++;
								}
							} else if (CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto)) {
								// PRODUCTO HS (§13, 2026-09-03): la mayoría de los HS ya se
								// cobran junto con su PH/PP (aplicarPagoParticipe, rama PH/PP) —
								// acá solo se procesan los HUÉRFANOS (sin fila PH/PP en esta
								// misma carga), que antes no los miraba nadie.
								// procesarSeguroIncendioHuerfano devuelve false sin hacer nada
								// cuando el partícipe SÍ tiene PH/PP (ya cubierto por el otro
								// camino, evita el doble cobro) — eso no es ni éxito ni omisión,
								// es el caso normal, así que no toca ningún contador.
								if (procesarSeguroIncendioHuerfano(participe, cargaArchivo, productosCache)) {
									totalExitosos++;
								}
							} else {
								// OTROS PRODUCTOS: Aplicar pagos a préstamos
								aplicarPagoParticipe(participe, codigoProducto, cargaArchivo, productosCache);
								totalExitosos++;
							}
						} catch (Throwable e) {
							throw new RuntimeException("TODO O NADA: falló el procesamiento del partícipe "
								+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
								+ codigoProducto + " — se aborta la carga completa " + codigoCargaArchivo
								+ ", no se confirma ningún cambio. Causa: " + e.getMessage(), e);
						}

						// MODO REPROCESO (§11 Rendimiento): borde de unidad de trabajo — el partícipe
						// terminó y todo lo suyo se vuelve a consultar por id en el siguiente. Solo
						// quedan vivas, cruzando este borde, cargaArchivo, detallesCarga,
						// participesDetalle (solo se leen) y productosCache, que no se desprenden o
						// no se modifican. Fuera del modo reproceso no hace nada.
						desprendidasFase3 += aliviarContextoReproceso();
				}
			}
			if (modoReproceso) {
				desprendidasFase3 += desprenderContextoReproceso();
				finFaseReproceso("fase 3: aplicar filas", inicioFase3, totalProcesados + " filas, "
					+ desprendidasFase3 + " entidades desprendidas en la fase");
			}

			StringBuilder resumenBuilder = new StringBuilder(String.format(
				"=== RESUMEN APLICACIÓN DE PAGOS ===\n" +
				"Total procesados: %d\n" +
				"Exitosos: %d\n" +
				"Aportes generados: %d\n" +
				"Omitidos (con novedades): %d\n" +
				"Advertencias (dinero recibido sin aplicar del todo): %d\n" +
				"Errores: %d\n" +
				"================================",
				totalProcesados, totalExitosos, totalAportesGenerados, totalOmitidos,
				totalAdvertencias, totalErrores
			));
			resumenBuilder.append(formateaListaResumen(
				"Novedades generadas (requieren decisión en pantalla — sin HistorialSueldo o esperado $0)",
				novedadesGeneradasCargaActual));
			resumenBuilder.append(formateaListaResumen(
				"Advertencias (contrato activo, vigencia no cubre el mes — no requieren acción inmediata)",
				advertenciasVigenciaCargaActual));
			if (modoReproceso) {
				resumenBuilder.append(formateaListaResumen(
					"Roles Petro sin partícipe en el SAA (" + etiquetaReproceso + " — NO aplicados, $"
						+ String.format("%,.2f", montoSinParticipeCargaActual) + " descontados del recibido)",
					sinParticipeCargaActual));
				resumenBuilder.append(formateaListaResumen(
					"Excedentes devueltos registrados como aporte EXCEDENTE_PETRO (D-010, $"
						+ String.format("%,.2f", montoExcedentesDevueltos) + ")",
					detalleExcedentesDevueltos));
				resumenBuilder.append(formateaListaResumen(
					"Operaciones vigentes al corte reabiertas (D-019, " + MARCA_CANCELACION_POSTERIOR + ")",
					cancelacionesPosterioresMarcadas));
				resumenBuilder.append(formateaListaResumen(
					"Cuotas del mes saldadas sin pago reabiertas al corte (D-019 ampliada)",
					cuotasSaldadasSinPagoReabiertas));
				resumenBuilder.append(formateaListaResumen(
					"Aportes AH con contrato inactivo hoy, vigente al corte (D-022, $"
						+ String.format("%,.2f", montoAportesContratoAlCorte) + ")",
					aportesContratoAlCorte));
				resumenBuilder.append(formateaListaResumen(
					"Aportes AH con contrato activo sin vigencia del mes (D-022 vigencias, $"
						+ String.format("%,.2f", montoAportesSinVigenciaDelMes) + ")",
					aportesSinVigenciaDelMes));
				resumenBuilder.append(formateaListaResumen(
					"HS aplicados como seguro de incendio de una cuota que no lo traía (D-023, $"
						+ String.format("%,.2f", montoHsSinSeguroEnCuota) + ")",
					hsSinSeguroEnCuota));
				resumenBuilder.append(formateaListaResumen(
					"Partícipes sin ningún contrato: contrato y vigencia creados (D-022 ampliada, $"
						+ String.format("%,.2f", montoContratosCreadosReproceso) + ")",
					contratosCreadosReproceso));
			}
			String resumen = resumenBuilder.toString();
			
			System.out.println(resumen);

			// MODO REPROCESO (D-010): ningún excedente de la lista puede quedar sin aplicar (p. ej.
			// porque su fila tomó un camino que no lo consume). Se dice cuál, no se ignora.
			if (modoReproceso && excedentesDevueltosAplicados.size() < excedentesDevueltosReproceso.size()) {
				List<String> sinAplicar = new ArrayList<>(excedentesDevueltosReproceso.keySet());
				sinAplicar.removeAll(excedentesDevueltosAplicados);
				throw new IncomeException(etiquetaReproceso + ": " + sinAplicar.size() + " excedente(s) devuelto(s)"
					+ " de la solicitud no se aplicaron (rol|producto): " + sinAplicar
					+ ". La fila no llegó al punto donde se registra el aporte; revise la lista.");
			}

			// ==========================================
			// VALIDACIÓN FINAL, ANTES DE LOS ASIENTOS (§11 VALIDACION-TOPE-AFECTACION-MANUAL.md):
			// recibido == aplicado tiene que cuadrar hasta el centavo. Todos los pagos y aportes
			// de arriba ya están aplicados — acá se cuenta exacto, no se predice. Si no cuadra,
			// lanza ANTES de generar ningún asiento: la transacción revierte sola y no queda un
			// asiento equivocado que después haya que deshacer restaurando la base.
			// ==========================================
			long inicioInvariante = modoReproceso ? inicioFaseReproceso("validarRecibidoIgualAplicado") : 0L;
			validarRecibidoIgualAplicado(cargaArchivo);
			if (modoReproceso) {
				finFaseReproceso("validarRecibidoIgualAplicado", inicioInvariante, null);
			}
			// ==========================================

			// Paso 2 del cobro en dos pasos (regla 11 de §5 del levantamiento), antes de marcar
			// PROCESADO: si cualquiera de los dos asientos falla, la carga NO queda a medio
			// contabilizar (misma transacción REQUIRED que aplica todos los pagos).
			long inicioAsientos = modoReproceso ? inicioFaseReproceso("asientos REPARTO y APLICACION") : 0L;
			// 2a. REPARTO: D 2.3.01.15.01 -> H 1.4.05.05/1.4.05.10, plantilla alterno 20.
			cobroPetroContableService.contabilizarReparto(codigoCargaArchivo);
			// 2b. APLICACION: D 2.3.02.05/2.3.02.10 -> H cuentas reales, plantilla 21 + bandas.
			cobroPetroContableService.contabilizarAplicacion(codigoCargaArchivo);
			if (modoReproceso) {
				finFaseReproceso("asientos REPARTO y APLICACION", inicioAsientos, null);
			}

			// Actualizar estado de CargaArchivo a 3 (PROCESADO)
			cargaArchivo.setEstado(3L);
			cargaArchivoService.saveSingle(cargaArchivo);
			System.out.println("✅ CargaArchivo actualizado a estado PROCESADO (3)");
			
			return resumen;
			
		} catch (Throwable e) {
			System.err.println("Error en aplicación de pagos: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * @see CargaArchivoPetroService#aplicarPagosArchivoPetroReproceso
	 *
	 * Enciende {@link #modoReproceso} alrededor de {@link #aplicarPagosArchivoPetro} y lo apaga
	 * SIEMPRE al salir (éxito o fallo): es un campo de instancia de un bean {@code @Stateful} y
	 * la próxima llamada normal no puede heredar el modo. Misma transacción que el llamador
	 * (REQUIRED): la anulación que hizo {@code ReprocesoCargaPetroService} y esta re-aplicación
	 * se confirman o revierten juntas.
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> aplicarPagosArchivoPetroReproceso(Long codigoCargaArchivo, String etiquetaCorrida,
			List<com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso> excedentesDevueltos) throws Throwable {
		return aplicarPagosArchivoPetroReproceso(codigoCargaArchivo, etiquetaCorrida, excedentesDevueltos, null);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> aplicarPagosArchivoPetroReproceso(Long codigoCargaArchivo, String etiquetaCorrida,
			List<com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso> excedentesDevueltos,
			String usuarioCorrida) throws Throwable {
		System.out.println("=== APLICACIÓN DE PAGOS EN MODO REPROCESO - Carga: " + codigoCargaArchivo
			+ " - " + etiquetaCorrida + " ===");
		modoReproceso = true;
		usuarioReproceso = usuarioCorrida != null && !usuarioCorrida.trim().isEmpty() ? usuarioCorrida.trim() : "SAA_REPROCESO";
		etiquetaReproceso = etiquetaCorrida != null ? etiquetaCorrida : ("REPROCESO carga " + codigoCargaArchivo);
		excedentesDevueltosReproceso.clear();
		try {
			if (excedentesDevueltos != null) {
				for (com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedente : excedentesDevueltos) {
					String clave = claveExcedente(excedente.getCodigoPetro(), excedente.getCodigoProducto());
					if (excedentesDevueltosReproceso.put(clave, excedente) != null) {
						throw new IncomeException(etiquetaReproceso + ": el excedente devuelto " + clave
							+ " está repetido en la solicitud; sume los montos en una sola línea.");
					}
				}
			}
			String resumen = aplicarPagosArchivoPetro(codigoCargaArchivo);
			Map<String, Object> resultado = new LinkedHashMap<>();
			resultado.put("resumen", resumen);
			resultado.put("sinParticipe", new ArrayList<>(sinParticipeCargaActual));
			resultado.put("filasSinParticipe", Integer.valueOf(sinParticipeCargaActual.size()));
			resultado.put("montoSinParticipe", Double.valueOf(Math.round(montoSinParticipeCargaActual * 100.0) / 100.0));
			resultado.put("excedentesDevueltos", new ArrayList<>(detalleExcedentesDevueltos));
			resultado.put("montoExcedentesDevueltos", Double.valueOf(Math.round(montoExcedentesDevueltos * 100.0) / 100.0));
			resultado.put("cancelacionesPosteriores", new ArrayList<>(cancelacionesPosterioresMarcadas));
			resultado.put("cuotasSaldadasSinPagoReabiertas", new ArrayList<>(cuotasSaldadasSinPagoReabiertas));
			resultado.put("aportesContratoAlCorte", new ArrayList<>(aportesContratoAlCorte));
			resultado.put("montoAportesContratoAlCorte", Double.valueOf(Math.round(montoAportesContratoAlCorte * 100.0) / 100.0));
			resultado.put("hsSinSeguroEnCuota", new ArrayList<>(hsSinSeguroEnCuota));
			resultado.put("montoHsSinSeguroEnCuota", Double.valueOf(Math.round(montoHsSinSeguroEnCuota * 100.0) / 100.0));
			resultado.put("aportesSinVigenciaDelMes", new ArrayList<>(aportesSinVigenciaDelMes));
			resultado.put("montoAportesSinVigenciaDelMes", Double.valueOf(Math.round(montoAportesSinVigenciaDelMes * 100.0) / 100.0));
			resultado.put("contratosCreados", new ArrayList<>(contratosCreadosReproceso));
			resultado.put("montoContratosCreados", Double.valueOf(Math.round(montoContratosCreadosReproceso * 100.0) / 100.0));
			return resultado;
		} finally {
			modoReproceso = false;
			etiquetaReproceso = null;
			usuarioReproceso = null;
			tipoContratoAportantesReproceso = null;
			excedentesDevueltosReproceso.clear();
		}
	}

	/**
	 * MODO REPROCESO: deja constancia de una fila PXCA cuyo rol Petro no existe en CRD.ENTD —
	 * novedad {@code PARTICIPE_NO_ENCONTRADO} en transacción propia (idempotente por
	 * (fila, tipo), con {@code montoEsperado = 0} y {@code montoRecibido = monto}, misma
	 * convención que §1.4.3 de REGLAS-CARGA-PETRO.md), una línea en el resumen y el monto
	 * acumulado para descontarlo del recibido en {@link #validarRecibidoIgualAplicado}.
	 * Fuera del modo reproceso no se llama nunca.
	 */
	/**
	 * Novedad generada DURANTE la aplicación (fase 3). Fuera del modo reproceso: en transacción
	 * propia, como siempre (VALIDACION-TOPE-AFECTACION-MANUAL.md §15: tiene que sobrevivir al
	 * rollback de la red del §11 para guiar al operador). En MODO REPROCESO (REPROCESO-CARGA-PETRO.md
	 * §4.11): en la MISMA transacción del reproceso, con el mismo registro idempotente por
	 * (fila, tipo) ({@link #registrarNovedad}) — si la corrida revierte, sus novedades también, y la
	 * base no queda con novedades de corridas que no existieron. Lo que el operador necesita saber
	 * de una corrida fallida viaja en el mensaje de error (filas sin pago, invariante).
	 */
	private void registrarNovedadDeAplicacion(ParticipeXCargaArchivo participe, int tipoNovedad, String descripcion,
			Long codigoProducto, Long codigoPrestamo, Double montoEsperado, Double montoRecibido) {
		if (modoReproceso) {
			registrarNovedad(participe, tipoNovedad, descripcion, codigoProducto, codigoPrestamo, montoEsperado, montoRecibido);
			return;
		}
		registradorNovedadTransaccionPropia.registrarEnTransaccionPropia(participe, tipoNovedad, descripcion,
			codigoProducto, codigoPrestamo, montoEsperado, montoRecibido);
	}

	/**
	 * MODO REPROCESO (§4.11): una fila con descuento que termina sin aplicar su dinero (sin
	 * producto, sin operación vigente al corte, sin cuota pendiente, excedente del motor). Se
	 * acumula para que el invariante recibido == aplicado la nombre (rol, producto, monto, motivo)
	 * en vez de dar solo el total. Fuera del modo reproceso no hace nada.
	 */
	private void registrarFilaSinPagoReproceso(ParticipeXCargaArchivo participe, String codigoProducto,
			double monto, String motivo) {
		if (!modoReproceso || monto <= 0.01 || participe == null) {
			return;
		}
		String producto = codigoProducto;
		if (producto == null && participe.getDetalleCargaArchivo() != null) {
			producto = participe.getDetalleCargaArchivo().getCodigoPetroProducto();
		}
		filasSinPagoReproceso.add("Rol " + participe.getCodigoPetro() + " (" + producto + "): $"
			+ String.format("%,.2f", monto) + " — " + motivo);
		montoFilasSinPagoReproceso += monto;
	}

	private void registrarSinParticipeReproceso(ParticipeXCargaArchivo participe, String codigoProducto,
			double monto, CargaArchivo cargaArchivo) {
		registrarNovedadDeAplicacion(participe,
			ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO,
			etiquetaReproceso + ": el rol Petro " + participe.getCodigoPetro() + " no existe en el SAA (producto "
				+ codigoProducto + ", $" + String.format("%.2f", monto) + " descontados en la carga "
				+ cargaArchivo.getCodigo() + "). No se aplicó; requiere decisión (crear el partícipe o devolver).",
			null, null, 0.0, monto);
		sinParticipeCargaActual.add("Rol " + participe.getCodigoPetro() + " (" + codigoProducto + "): $"
			+ String.format("%,.2f", monto) + " sin partícipe en el SAA");
		montoSinParticipeCargaActual += monto;
		System.out.println("⚠️ " + etiquetaReproceso + " - Rol " + participe.getCodigoPetro() + " sin partícipe ("
			+ codigoProducto + ", $" + monto + "): no se aplica, queda como novedad.");
	}

	// ============================================================================
	// MODO REPROCESO — D-010: excedentes cobrados de más y DEVUELTOS al partícipe
	// ============================================================================

	/** Clave {@code rol|PRODUCTO} de un excedente devuelto (mayúsculas, sin espacios). */
	private static String claveExcedente(Long codigoPetro, String codigoProducto) {
		return codigoPetro + "|" + (codigoProducto != null ? codigoProducto.trim().toUpperCase() : "");
	}

	/** El excedente devuelto declarado para esta fila, o null (siempre null fuera del modo reproceso). */
	private com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedenteDevueltoDeFila(
			ParticipeXCargaArchivo participe, String codigoProducto) {
		if (!modoReproceso || excedentesDevueltosReproceso.isEmpty() || participe == null) {
			return null;
		}
		return excedentesDevueltosReproceso.get(claveExcedente(participe.getCodigoPetro(), codigoProducto));
	}

	/**
	 * D-010: valida la lista completa de excedentes devueltos antes de aplicar nada. Cada línea
	 * tiene que: identificar una fila de ESTA carga (rol + producto, nunca HS suelto), traer
	 * monto positivo y referencia del documento que respalda la devolución, no superar lo que esa
	 * fila descontó (PH/PP con su HS), tener partícipe en el SAA (sin partícipe no hay cuenta donde
	 * registrar el aporte: esa fila ya va como novedad PARTICIPE_NO_ENCONTRADO), no tener además
	 * afectación manual (AVPC: serían dos destinos para el mismo dinero) y un tipo de aporte
	 * resoluble. Reporta TODOS los errores juntos.
	 */
	private void validarExcedentesDevueltosReproceso(CargaArchivo cargaArchivo) throws Throwable {
		if (excedentesDevueltosReproceso.isEmpty()) {
			return;
		}
		System.out.println("=== D-010: VALIDANDO " + excedentesDevueltosReproceso.size() + " EXCEDENTE(S) DEVUELTO(S) ===");
		List<String> errores = new ArrayList<>();
		for (Map.Entry<String, com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso> entry : excedentesDevueltosReproceso.entrySet()) {
			String clave = entry.getKey();
			com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedente = entry.getValue();
			String producto = excedente.getCodigoProducto() != null ? excedente.getCodigoProducto().trim().toUpperCase() : null;
			if (excedente.getCodigoPetro() == null || producto == null || producto.isEmpty()) {
				errores.add(clave + ": falta el rol Petro o el producto.");
				continue;
			}
			if (CODIGO_PRODUCTO_HS.equalsIgnoreCase(producto)) {
				errores.add(clave + ": el HS se declara dentro de su fila PH/PP, no suelto.");
				continue;
			}
			if (excedente.getMonto() == null || excedente.getMonto() <= 0.01) {
				errores.add(clave + ": el monto debe ser mayor a cero.");
				continue;
			}
			if (excedente.getReferencia() == null || excedente.getReferencia().trim().isEmpty()) {
				errores.add(clave + ": falta la referencia del documento que respalda la devolución (D-010).");
				continue;
			}
			ParticipeXCargaArchivo fila = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
				excedente.getCodigoPetro(), producto, cargaArchivo.getCodigo());
			if (fila == null) {
				errores.add(clave + ": no existe esa fila (rol + producto) en la carga " + cargaArchivo.getCodigo() + ".");
				continue;
			}
			double disponible = montoConHsMergeado(fila, producto, cargaArchivo);
			if (excedente.getMonto() > disponible + 0.01) {
				errores.add(clave + ": el excedente $" + String.format("%.2f", excedente.getMonto())
					+ " supera lo descontado en la fila ($" + String.format("%.2f", disponible) + ").");
				continue;
			}
			List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(excedente.getCodigoPetro());
			if (entidades == null || entidades.isEmpty()) {
				errores.add(clave + ": el rol no tiene partícipe en el SAA; esa fila queda como novedad"
					+ " PARTICIPE_NO_ENCONTRADO y no puede registrar el excedente como aporte.");
				continue;
			}
			if (totalAfectadoManualmente(novedadParticipeCargaDaoService.selectByParticipe(fila.getCodigo())) > 0.01) {
				errores.add(clave + ": la fila tiene además afectación manual (AVPC); decida un solo destino.");
				continue;
			}
			try {
				resolverTipoAporteExcedente(entidades.get(0), excedente, cargaArchivo);
			} catch (IncomeException e) {
				errores.add(clave + ": " + e.getMessage());
			}
		}
		if (!errores.isEmpty()) {
			throw new IncomeException(etiquetaReproceso + ": la lista de excedentes devueltos (D-010) tiene "
				+ errores.size() + " error(es); no se aplicó nada.\n  - " + String.join("\n  - ", errores));
		}
		System.out.println("✅ D-010: lista de excedentes devueltos válida");
	}

	/**
	 * Tipo de aporte que recibe el excedente devuelto: el de la solicitud (9 o 11) o, si no
	 * viene, cesantía si el partícipe tiene vigencia de cesantía en el mes de la carga, y si no
	 * jubilación — misma regla de "los que tenga el partícipe" de PLAN-EXCEDENTE-PETRO-A-APORTES.md
	 * §3 ({@code esperadoPorEntidad} > 0). Cesantía primero porque es la cuenta de la que se
	 * paga la devolución al partícipe; si no tiene ninguna, la solicitud debe decirlo.
	 */
	private Long resolverTipoAporteExcedente(Entidad entidad,
			com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedente, CargaArchivo cargaArchivo) throws Throwable {
		if (excedente.getIdTipoAporte() != null) {
			if (!TIPO_APORTE_CESANTIA.equals(excedente.getIdTipoAporte())
					&& !TIPO_APORTE_JUBILACION.equals(excedente.getIdTipoAporte())) {
				throw new IncomeException("idTipoAporte " + excedente.getIdTipoAporte()
					+ " no válido para un excedente: use 11 (cesantía) o 9 (jubilación).");
			}
			return excedente.getIdTipoAporte();
		}
		java.time.LocalDate mes = java.time.LocalDate.of(cargaArchivo.getAnioAfectacion().intValue(),
			cargaArchivo.getMesAfectacion().intValue(), 1);
		if (vigenciaContratoService.esperadoPorEntidad(entidad.getCodigo(), TIPO_APORTE_CESANTIA, mes) > 0.0) {
			return TIPO_APORTE_CESANTIA;
		}
		if (vigenciaContratoService.esperadoPorEntidad(entidad.getCodigo(), TIPO_APORTE_JUBILACION, mes) > 0.0) {
			return TIPO_APORTE_JUBILACION;
		}
		throw new IncomeException("la entidad " + entidad.getCodigo() + " no tiene vigencia de cesantía ni de"
			+ " jubilación en " + mes.getMonthValue() + "/" + mes.getYear() + "; indique idTipoAporte en la solicitud.");
	}

	/**
	 * D-010: si la fila tiene un excedente devuelto declarado, lo registra como aporte
	 * {@code EXCEDENTE_PETRO} (mismo camino que {@link #aplicarAfectacionAAporte}: {@link
	 * #crearNuevoAporte} + {@link #crearRegistroPagoAporte}, fecha de caja = último día del mes de
	 * la carga, sin devengo, {@code APRTIDAS} = carga — así entra solo al HABER del asiento de
	 * APLICACION y al invariante recibido == aplicado) y devuelve su monto para que el llamador
	 * lo descuente de lo que entrega al motor o a la prelación. 0 si la fila no tiene excedente.
	 */
	private double aplicarExcedenteDevueltoReproceso(ParticipeXCargaArchivo participe, String codigoProducto,
			Entidad entidad, CargaArchivo cargaArchivo) throws Throwable {
		com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedente = excedenteDevueltoDeFila(participe, codigoProducto);
		if (excedente == null) {
			return 0.0;
		}
		String clave = claveExcedente(participe.getCodigoPetro(), codigoProducto);
		if (!excedentesDevueltosAplicados.add(clave)) {
			return 0.0; // ya aplicado en esta corrida (no debería pasar: una fila se procesa una vez)
		}
		double monto = Math.round(excedente.getMonto() * 100.0) / 100.0;
		Long idTipo = resolverTipoAporteExcedente(entidad, excedente, cargaArchivo);
		com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipo, "TipoAporte");
		Aporte aporte = crearNuevoAporte(entidad, idTipo, tipoAporte.getNombre(), monto, null, cargaArchivo,
			com.saa.rubros.CrdTipoMovimientoAporte.EXCEDENTE_PETRO);
		// La marca [Entrada Petro XX] le dice al asiento de aplicación por qué puente entró este
		// dinero en el reparto (D-010 corrección: un excedente de PE/PP debita 2.3.02.10).
		aporte.setGlosa(truncarTexto(com.saa.rubros.EntradaExcedentePetro.marca(codigoProducto)
			+ " Excedente Petro devuelto al participe (D-010) - " + codigoProducto + " - "
			+ excedente.getReferencia().trim() + " - " + etiquetaReproceso + " - Mes " + cargaArchivo.getMesAfectacion()
			+ "/" + cargaArchivo.getAnioAfectacion() + " - CargaArchivo: " + cargaArchivo.getCodigo(), 2000));
		aporteDaoService.save(aporte, aporte.getCodigo());
		crearRegistroPagoAporte(aporte, monto, cargaArchivo, participe);
		montoExcedentesDevueltos += monto;
		detalleExcedentesDevueltos.add("Entidad " + entidad.getCodigo() + " (" + codigoProducto + "): $"
			+ String.format("%,.2f", monto) + " a " + tipoAporte.getNombre() + " (APRT " + aporte.getCodigo() + ")");
		System.out.println("   ✅ D-010: excedente devuelto $" + monto + " registrado como aporte "
			+ tipoAporte.getNombre() + " (APRT " + aporte.getCodigo() + ")");
		return monto;
	}

	private static String truncarTexto(String texto, int max) {
		return texto == null || texto.length() <= max ? texto : texto.substring(0, max);
	}

	// ============================================================================
	// MODO REPROCESO — D-019: la operación se busca por su estado a la fecha de corte
	// ============================================================================

	/** Texto de la marca que deja D-019 en la operación reabierta y en sus eventos posteriores. */
	static final String MARCA_CANCELACION_POSTERIOR = "cancelación posterior pendiente de re-aplicar";

	/** Último instante del mes de la carga: la fecha de corte del reproceso. */
	private java.time.LocalDateTime finMesCarga(CargaArchivo cargaArchivo) throws Throwable {
		return fechaService.ultimoDiaMesAnioLocal(cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion())
			.atTime(23, 59, 59);
	}

	private static boolean esPrestamoTerminal(Long idEstado) {
		if (idEstado == null) {
			return false;
		}
		int e = idEstado.intValue();
		return e == com.saa.rubros.EstadoPrestamo.CANCELADO || e == com.saa.rubros.EstadoPrestamo.CANCELADO_ANTICIPADO
			|| e == com.saa.rubros.EstadoPrestamo.CANCELADO_POR_NOVACION || e == com.saa.rubros.EstadoPrestamo.CANCELADO_POR_REVISAR
			|| e == com.saa.rubros.EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION;
	}

	/**
	 * D-019: ¿la fila tiene una operación de su producto vigente al corte? Para que
	 * {@link #buscarValoresSinDestino} no bloquee por un PRESTAMO_NO_ENCONTRADO heredado. Solo
	 * consulta; no reabre nada.
	 */
	private boolean existePrestamoVigenteAlCorte(ParticipeXCargaArchivo participe, String codigoProducto,
			CargaArchivo cargaArchivo) throws Throwable {
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
		if (entidades == null || entidades.isEmpty()) {
			return false;
		}
		List<Producto> productos = resolverProductosPorCodigo(codigoProducto, new HashMap<>());
		if (productos == null) {
			return false;
		}
		java.time.LocalDateTime corte = finMesCarga(cargaArchivo);
		for (Producto producto : productos) {
			List<Prestamo> lista = prestamoDaoService.selectByEntidadYProductoVigentesAlCorte(
				entidades.get(0).getCodigo(), producto.getCodigo(), corte);
			if (lista != null && !lista.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * D-019 (decisión del líder, 2026-10-28): en reproceso el estado de HOY del préstamo no
	 * decide. Se buscan las operaciones vigentes al corte ({@code selectByEntidadYProductoVigentesAlCorte})
	 * y cada una que hoy está cancelada se reabre ({@link #reabrirPrestamoVigenteAlCorte}) para que
	 * la cuota del mes reciba el pago como ocurrió cronológicamente. D-019 ampliada: la que no es
	 * terminal igual pasa por {@link #reabrirCuotasSaldadasSinPagoAlCorte}. Cada préstamo se prepara
	 * una sola vez por corrida; la primera llamada es la de
	 * {@link #prepararOperacionesVigentesAlCorteReproceso}, antes de la validación.
	 */
	private List<Prestamo> prestamosVigentesAlCorteReproceso(Entidad entidad, Producto producto,
			CargaArchivo cargaArchivo) throws Throwable {
		List<Prestamo> lista = prestamoDaoService.selectByEntidadYProductoVigentesAlCorte(
			entidad.getCodigo(), producto.getCodigo(), finMesCarga(cargaArchivo));
		if (lista == null) {
			return new ArrayList<>();
		}
		for (Prestamo prestamo : lista) {
			// D-019 ampliada: cada préstamo se prepara UNA vez por corrida (el paso previo a la
			// validación y la fase 3 llegan acá con la misma fila).
			if (!prestamosPreparadosReproceso.add(prestamo.getCodigo())) {
				continue;
			}
			if (esPrestamoTerminal(prestamo.getIdEstado())) {
				reabrirPrestamoVigenteAlCorte(prestamo, cargaArchivo);
			} else {
				reabrirCuotasSaldadasSinPagoAlCorte(prestamo, cargaArchivo);
			}
		}
		return lista;
	}

	/**
	 * D-019 ampliada (líder del proyecto, 2026-10-28) — regla "cuota saldada sin pago vigente =
	 * pendiente al corte". La migración desde Delta 21 dejó cuotas en PAGADA (4) /
	 * CANCELADA_ANTICIPADA (7) sin ninguna fila en {@code CRD.PGPR} y con {@code DTPRFCPG} nula o
	 * posterior al corte. En reproceso, la cuota con vencimiento en el MES DE LA CARGA que esté así
	 * se reconstruye desde sus pagos ({@code recalcularCuotaDesdePagos}: sin pagos vuelve a
	 * pendiente/en mora) y su mora se calcula al corte; así la fila Petro del mes la encuentra y le
	 * aplica el pago como ocurrió. Vale para cualquier operación vigente al corte, terminal o no hoy.
	 *
	 * <p>Criterio conservador: solo cuotas con vencimiento dentro del mes de la carga y sin NINGÚN
	 * pago vigente ({@code selectVigentesByIdDetallePrestamo}, el mismo que usa el motor). Las de
	 * meses anteriores quedan como estaban (las fija el ancla de mayo 2025 o el reproceso de su
	 * mes) y las de meses siguientes las reabre el reproceso de su propio mes. Una cuota con algún
	 * pago vigente no se toca. Nada se borra.</p>
	 *
	 * @return cuántas cuotas cambiaron de estado
	 */
	private int reabrirCuotasSaldadasSinPagoAlCorte(Prestamo prestamo, CargaArchivo cargaArchivo) throws Throwable {
		return reabrirCuotasSaldadasSinPagoAlCorte(prestamo, cargaArchivo,
			detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo()));
	}

	/**
	 * Igual que {@link #reabrirCuotasSaldadasSinPagoAlCorte(Prestamo, CargaArchivo)} sobre una lista
	 * de cuotas ya leída con {@code selectByPrestamo} (§11 Rendimiento: {@link #reabrirPrestamoVigenteAlCorte}
	 * la reutiliza para su segunda pasada en vez de repetir la consulta; las instancias son las mismas
	 * administradas que devolvería la consulta repetida, con el estado que esta pasada les dejó).
	 */
	private int reabrirCuotasSaldadasSinPagoAlCorte(Prestamo prestamo, CargaArchivo cargaArchivo,
			List<DetallePrestamo> cuotas) throws Throwable {
		java.time.LocalDateTime corte = finMesCarga(cargaArchivo);
		java.time.LocalDate fechaCorte = corte.toLocalDate();
		java.time.LocalDateTime inicioMes = fechaCorte.withDayOfMonth(1).atStartOfDay();
		if (cuotas == null) {
			return 0;
		}
		int reabiertas = 0;
		for (DetallePrestamo cuota : cuotas) {
			Long estado = cuota.getEstado();
			boolean saldada = estado != null && (estado.intValue() == com.saa.rubros.EstadoCuotaPrestamo.PAGADA
				|| estado.intValue() == com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
			if (!saldada || cuota.getFechaVencimiento() == null
					|| cuota.getFechaVencimiento().isBefore(inicioMes) || cuota.getFechaVencimiento().isAfter(corte)) {
				continue;
			}
			// §4.9 ampliada (2026-10-28): la fecha de pago NO decide. La migración dejó cuotas del mes
			// en 4/7 sin ningún PGPR con DTPRFCPG = 2025-05-31 (marca de la migración, no un pago:
			// los pagos reales migrados sí tienen PGPR). Lo único que prueba que la cuota se pagó es
			// un pago vigente; sin él vuelve a pendiente al corte, cualquiera sea su fecha de pago.
			java.time.LocalDateTime fechaPagadoAnterior = cuota.getFechaPagado();
			List<com.saa.model.crd.PagoPrestamo> pagosVigentes =
				pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
			if (pagosVigentes != null && !pagosVigentes.isEmpty()) {
				continue;
			}
			motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
			procesoMoraPrestamoService.recalcularMoraCuotaALaFecha(cuota, fechaCorte);
			if (cuota.getEstado() == null || cuota.getEstado().longValue() != estado.longValue()) {
				reabiertas++;
				String linea = "Préstamo " + prestamo.getCodigo() + " cuota " + numeroCuotaTexto(cuota)
					+ " (préstamo hoy estado " + prestamo.getIdEstado() + "): estado " + estado + " -> "
					+ cuota.getEstado() + ", fecha de pago anterior "
					+ (fechaPagadoAnterior != null ? fechaPagadoAnterior.toLocalDate() : "nula")
					+ ", sin pagos vigentes";
				cuotasSaldadasSinPagoReabiertas.add(linea);
				System.out.println("   D-019 ampliada: " + linea);
			}
		}
		return reabiertas;
	}

	private static String numeroCuotaTexto(DetallePrestamo cuota) {
		return cuota.getNumeroCuota() == null ? "?" : String.valueOf(cuota.getNumeroCuota().longValue());
	}

	/**
	 * D-019 ampliada — paso previo a {@link #validarValoresConDestino} en modo reproceso: para cada
	 * fila de préstamo de la carga con descuento &gt; 0 (no AH ni HS; sin afectación manual, que
	 * define su propio destino; sin que todo lo descontado sea excedente devuelto, D-010) resuelve
	 * las operaciones vigentes al corte del partícipe y producto
	 * ({@link #prestamosVigentesAlCorteReproceso}): reabre las cuotas del mes saldadas sin pago
	 * (regla de {@link #reabrirCuotasSaldadasSinPagoAlCorte}) y, si la operación es terminal hoy,
	 * la reabre completa (D-019). Solo esas operaciones: el resto de la cartera no se toca.
	 */
	private void prepararOperacionesVigentesAlCorteReproceso(CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("=== " + etiquetaReproceso + ": preparando operaciones vigentes al corte (D-019 ampliada) ===");
		List<DetalleCargaArchivo> detalles = detalleCargaArchivoDaoService.selectByCargaArchivo(cargaArchivo.getCodigo());
		if (detalles == null) {
			return;
		}
		Map<String, List<Producto>> productosCache = new HashMap<>();
		int filas = 0;
		for (DetalleCargaArchivo detalle : detalles) {
			String codigoProducto = detalle.getCodigoPetroProducto();
			if (CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto) || CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto)) {
				continue;
			}
			List<ParticipeXCargaArchivo> participes = participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
			if (participes == null) {
				continue;
			}
			for (ParticipeXCargaArchivo participe : participes) {
				double monto = nullSafe(participe.getTotalDescontado());
				if (monto <= 0.01 || participe.getCodigoPetro() == null) {
					continue;
				}
				com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedente = excedenteDevueltoDeFila(participe, codigoProducto);
				if (excedente != null && excedente.getMonto() != null && monto - excedente.getMonto() <= 0.01
						&& !CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto)
						&& !CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
					continue; // mismo corte que aplicarPagoParticipe: la fila no llega al motor
				}
				if (tieneAfectacionManual(participe)) {
					continue; // la afectación manual define el destino (aplicarPagoParticipe no busca préstamos)
				}
				List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
				if (entidades == null || entidades.isEmpty()) {
					continue; // sin partícipe: va a registrarSinParticipeReproceso
				}
				List<Producto> productos = resolverProductosPorCodigo(codigoProducto, productosCache);
				if (productos == null || productos.isEmpty()) {
					continue;
				}
				for (Producto producto : productos) {
					prestamosVigentesAlCorteReproceso(entidades.get(0), producto, cargaArchivo);
				}
				filas++;
				// §11 Rendimiento: borde de unidad de trabajo — la fila quedó preparada (todo lo
				// que tocó pasó por save). Cruzan el borde solo detalles, participes (solo
				// lectura), productosCache y cargaArchivo; los préstamos preparados se recuerdan
				// por id (prestamosPreparadosReproceso), nunca por instancia.
				aliviarContextoReproceso();
			}
		}
		System.out.println("=== " + etiquetaReproceso + ": " + filas + " fila(s) preparadas, "
			+ prestamosPreparadosReproceso.size() + " operación(es) vigentes al corte, "
			+ cancelacionesPosterioresMarcadas.size() + " terminal(es) reabiertas (D-019), "
			+ cuotasSaldadasSinPagoReabiertas.size() + " cuota(s) del mes saldadas sin pago reabiertas ===");
	}

	/** ¿Alguna novedad de la fila tiene afectación manual (AVPC)? Mismo criterio que
	 * {@code verificarYAplicarAfectacionesManualesTotales}, solo consulta. */
	private boolean tieneAfectacionManual(ParticipeXCargaArchivo participe) throws Throwable {
		List<NovedadParticipeCarga> novedades = novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
		if (novedades == null) {
			return false;
		}
		for (NovedadParticipeCarga novedad : novedades) {
			List<AfectacionValoresParticipeCarga> avpc = afectacionValoresParticipeCargaDaoService.selectByNovedad(novedad.getCodigo());
			if (avpc != null && !avpc.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * D-019 ampliada: ¿alguna operación vigente al corte de la fila tiene hoy una cuota no saldada?
	 * Para que {@link #buscarValoresSinDestino} no bloquee por un {@code CUOTA_NO_ENCONTRADA}
	 * heredado de la validación original cuando, con las cuotas del mes ya reabiertas
	 * ({@link #prepararOperacionesVigentesAlCorteReproceso}), la fase 3 sí encuentra a qué
	 * aplicar. Mismo criterio que la validación 12 de {@code validarNovedadesFase2}: basta una
	 * cuota pendiente; el monto lo cuida el invariante recibido == aplicado. Solo consulta.
	 */
	private boolean existeCuotaPendienteAlCorte(ParticipeXCargaArchivo participe, String codigoProducto,
			CargaArchivo cargaArchivo) throws Throwable {
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
		if (entidades == null || entidades.isEmpty()) {
			return false;
		}
		List<Producto> productos = resolverProductosPorCodigo(codigoProducto, new HashMap<>());
		if (productos == null) {
			return false;
		}
		java.time.LocalDateTime corte = finMesCarga(cargaArchivo);
		for (Producto producto : productos) {
			List<Prestamo> lista = prestamoDaoService.selectByEntidadYProductoVigentesAlCorte(
				entidades.get(0).getCodigo(), producto.getCodigo(), corte);
			if (lista == null) {
				continue;
			}
			for (Prestamo prestamo : lista) {
				List<DetallePrestamo> pendiente = detallePrestamoDaoService.selectMinCuotaNoPagadaByPrestamo(prestamo.getCodigo());
				if (pendiente != null && !pendiente.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * D-019: reabre una operación que estaba vigente al corte y hoy está cancelada.
	 * <ul>
	 * <li>Cada cuota saldada SIN fecha o con fecha posterior al corte (la cancelación/liquidación
	 * posterior la marcó PAGADA/CANCELADA_ANTICIPADA, en las migradas sin ningún PGPR) se
	 * reconstruye desde sus pagos vigentes ({@code recalcularCuotaDesdePagos}): si nada la paga,
	 * vuelve a pendiente; su mora se calcula al corte. Las saldadas hasta el corte no se tocan.</li>
	 * <li>El préstamo pasa a VIGENTE (2) para que el motor admita el pago; su observación lleva
	 * {@code "<etiqueta>: cancelación posterior pendiente de re-aplicar (estado anterior N)"}.</li>
	 * <li>Los eventos (EVPR) no anulados con fecha posterior al corte reciben la misma marca, sin
	 * anularse: al reprocesar su mes hay que recalcularlos con el saldo correcto.</li>
	 * </ul>
	 * Nada se borra. La consulta que lista lo marcado está en REPROCESO-CARGA-PETRO.md §4.8.
	 */
	private void reabrirPrestamoVigenteAlCorte(Prestamo prestamo, CargaArchivo cargaArchivo) throws Throwable {
		java.time.LocalDateTime corte = finMesCarga(cargaArchivo);
		java.time.LocalDate fechaCorte = corte.toLocalDate();
		Long estadoAnterior = prestamo.getIdEstado();
		String marca = etiquetaReproceso + ": " + MARCA_CANCELACION_POSTERIOR + " (estado anterior "
			+ estadoAnterior + ")";

		// D-019 ampliada: primero la regla común (cuota del mes saldada sin pago vigente); después,
		// como antes, el resto de las cuotas saldadas sin fecha o después del corte.
		// §11 Rendimiento: una sola lectura de las cuotas para las dos pasadas (antes se repetía
		// selectByPrestamo; misma lista, mismas instancias administradas, mismo orden).
		List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
		int cuotasReabiertas = reabrirCuotasSaldadasSinPagoAlCorte(prestamo, cargaArchivo, cuotas);
		if (cuotas != null) {
			for (DetallePrestamo cuota : cuotas) {
				Long estado = cuota.getEstado();
				boolean saldada = estado != null && (estado.intValue() == com.saa.rubros.EstadoCuotaPrestamo.PAGADA
					|| estado.intValue() == com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
				if (!saldada || (cuota.getFechaPagado() != null && !cuota.getFechaPagado().isAfter(corte))) {
					continue;
				}
				motorPagoPrestamoService.recalcularCuotaDesdePagos(cuota);
				procesoMoraPrestamoService.recalcularMoraCuotaALaFecha(cuota, fechaCorte);
				if (cuota.getEstado() == null || cuota.getEstado().longValue() != estado.longValue()) {
					cuotasReabiertas++;
				}
			}
		}

		prestamo.setIdEstado(Long.valueOf(com.saa.rubros.EstadoPrestamo.VIGENTE));
		prestamo.setFechaModificacion(java.time.LocalDateTime.now());
		prestamo.setObservacion(truncarTexto(prestamo.getObservacion() == null || prestamo.getObservacion().trim().isEmpty()
			? marca : prestamo.getObservacion() + " | " + marca, 2000));
		prestamoDaoService.save(prestamo, prestamo.getCodigo());

		int eventosMarcados = 0;
		List<com.saa.model.crd.EventoPrestamo> eventos = eventoPrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
		if (eventos != null) {
			for (com.saa.model.crd.EventoPrestamo evento : eventos) {
				if (evento.getFechaAnulacion() != null || evento.getFecha() == null || !evento.getFecha().isAfter(corte)) {
					continue;
				}
				if (evento.getObservacion() != null && evento.getObservacion().contains(marca)) {
					continue;
				}
				evento.setObservacion(truncarTexto(evento.getObservacion() == null ? marca
					: evento.getObservacion() + " | " + marca, 2000));
				eventoPrestamoDaoService.save(evento, evento.getCodigo());
				eventosMarcados++;
			}
		}

		cancelacionesPosterioresMarcadas.add("Préstamo " + prestamo.getCodigo() + " (estado anterior " + estadoAnterior
			+ "): " + cuotasReabiertas + " cuota(s) reabiertas, " + eventosMarcados + " evento(s) posteriores marcados");
		System.out.println("   D-019: préstamo " + prestamo.getCodigo() + " vigente al " + fechaCorte + " (hoy estado "
			+ estadoAnterior + ") reabierto - " + cuotasReabiertas + " cuotas, " + eventosMarcados + " eventos marcados");
	}

	/**
	 * Arma el bloque del resumen para una de las dos listas de dinero-sin-aplicar-del-todo
	 * (novedades o advertencias): título, las primeras {@link #MAX_ADVERTENCIAS_EN_RESUMEN}
	 * líneas y, si sobran, un "... y N más" apuntando al log — nunca un conteo sin detalle ni
	 * una lista de 50 líneas.
	 *
	 * @return bloque de texto (con su propio salto de línea inicial), o "" si la lista está vacía
	 */
	private String formateaListaResumen(String titulo, List<String> lineas) {
		if (lineas == null || lineas.isEmpty()) {
			return "";
		}
		StringBuilder bloque = new StringBuilder("\n\n").append(titulo).append(" (")
			.append(lineas.size()).append("):");
		int mostradas = Math.min(lineas.size(), MAX_ADVERTENCIAS_EN_RESUMEN);
		for (int i = 0; i < mostradas; i++) {
			bloque.append("\n  - ").append(lineas.get(i));
		}
		if (lineas.size() > mostradas) {
			bloque.append("\n  ... y ").append(lineas.size() - mostradas)
				.append(" más (detalle completo en el log del servidor).");
		}
		return bloque.toString();
	}

	/**
	 * Verifica si un partícipe tiene novedades que bloquean el procesamiento de pagos
	 * ✅ CORRECCIÓN: NINGUNA novedad bloquea el procesamiento
	 * El flujo ya maneja correctamente todos los casos:
	 * - PRESTAMO_NO_ENCONTRADO: El método aplicarPagoParticipe hace return si no encuentra préstamos
	 * - CUOTA_NO_ENCONTRADA: El método buscarCuotaAPagar retorna null y no procesa
	 * - MONTO_INCONSISTENTE: Si es menor queda PARCIAL, si es mayor se aplica a siguiente cuota
	 * - MULTIPLES_PRESTAMOS_ACTIVOS: Se procesan todos los préstamos activos encontrados
	 * etc.
	 */
	/**
	 * Verifica si un partícipe tiene novedades que bloquean el procesamiento
	 * ✅ CORRECTO: Las novedades son INFORMATIVAS, NO bloqueantes
	 * Se procesa el valor recibido aunque existan novedades
	 */
	private boolean tieneNovedadesBloqueantes(ParticipeXCargaArchivo participe) {
		// ✅ NO bloquear procesamiento - Las novedades son solo informativas
		// Se procesa el monto que viene en el archivo independientemente de las novedades
		return false;
	}

	// ============================================================================
	// VALIDACIÓN PREVIA: TODO VALOR DESCONTADO DEBE TENER DESTINO
	// ============================================================================

	/** Cuántos registros se listan en el mensaje de error antes de resumir el resto. */
	private static final int MAXIMO_DETALLES_EN_MENSAJE = 20;

	/**
	 * Corta el procesamiento (paso 2) si contabilidad todavía no confirmó el paso 1 —
	 * regla 11 de §5 de LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md.
	 *
	 * Aislado en su propio método A PROPÓSITO (2026-08-28): el criterio de bloquear TODO el
	 * procesamiento hasta que exista la confirmación está en consulta con el usuario por su
	 * impacto operativo — si el dinero de Petro suele entrar DESPUÉS de que se carga el
	 * archivo, los aportes y pagos de préstamos quedarían sin aplicar varios días. Si el
	 * criterio cambia (p. ej. "procesar igual y diferir solo el asiento de reparto"), el
	 * cambio se hace acá, sin tocar el resto de {@code aplicarPagosArchivoPetro}.
	 *
	 * Usa {@code fechaAutorizacionContabilidad} (CRARFCAC) porque es el marcador DURADERO
	 * del paso 1 — nunca {@code CRARESTD}, que es transitorio (ver
	 * {@code CrdEstadoCargaArchivo.CONFIRMADO_CONTABILIDAD}).
	 */
	private void exigeConfirmacionContabilidad(CargaArchivo cargaArchivo) throws Throwable {
		if (cargaArchivo.getFechaAutorizacionContabilidad() == null) {
			throw new IncomeException("Contabilidad aún no ha confirmado la recepción del"
				+ " dinero de esta carga (" + cargaArchivo.getCodigo() + "). Registre las"
				+ " transferencias y confirme la recepción antes de procesar el archivo.");
		}
	}

	/**
	 * Corta el procesamiento si algún valor descontado no tiene a qué aplicarse.
	 *
	 * Todo lo que Petrocomercial descontó al partícipe tiene que terminar en un
	 * pago a préstamo o en un aporte. Si un registro trae una novedad que impide
	 * saber dónde aplicarlo y el usuario no registró la afectación manual, no se
	 * procesa NADA de la carga: se lanza la excepción antes de tocar la primera
	 * cuota, así no queda media carga aplicada.
	 */
	private void validarValoresConDestino(CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("=== VALIDANDO QUE TODO VALOR DESCONTADO TENGA DESTINO ===");

		List<Map<String, Object>> pendientes = buscarValoresSinDestino(cargaArchivo);

		if (pendientes.isEmpty()) {
			System.out.println("✅ Todos los valores descontados tienen destino definido");
			return;
		}

		StringBuilder mensaje = new StringBuilder();
		mensaje.append("No se puede procesar el archivo: hay ").append(pendientes.size())
		       .append(" registro(s) con valores descontados sin destino definido. ")
		       .append("Registre en las novedades cómo aplicar cada valor y vuelva a procesar.");

		int listados = 0;
		for (Map<String, Object> pendiente : pendientes) {
			if (listados >= MAXIMO_DETALLES_EN_MENSAJE) {
				mensaje.append("\n  ... y ").append(pendientes.size() - listados).append(" registro(s) más.");
				break;
			}
			mensaje.append("\n  - Rol ").append(pendiente.get("codigoPetro"))
			       .append(" ").append(pendiente.get("nombre"))
			       .append(" (").append(pendiente.get("codigoProducto")).append("): $")
			       .append(String.format("%,.2f", (Double) pendiente.get("valorSinDestino")))
			       .append(" sin aplicar de $")
			       .append(String.format("%,.2f", (Double) pendiente.get("totalDescontado")))
			       .append(" descontados. Novedad: ").append(pendiente.get("novedades")).append(".");
			listados++;
		}

		System.err.println(mensaje.toString());
		throw new IncomeException(mensaje.toString());
	}

	@Override
	public List<Map<String, Object>> obtenerValoresSinDestino(Long codigoCargaArchivo) throws Throwable {
		System.out.println("Consultando valores sin destino de la carga: " + codigoCargaArchivo);

		CargaArchivo cargaArchivo = cargaArchivoService.selectById(codigoCargaArchivo);
		if (cargaArchivo == null) {
			throw new IncomeException("No se encontró la carga con ID: " + codigoCargaArchivo);
		}

		return buscarValoresSinDestino(cargaArchivo);
	}

	/**
	 * Recorre la carga y arma la lista de registros cuyo valor descontado se
	 * quedaría sin aplicar.
	 *
	 * Un registro entra a la lista cuando:
	 * 1. Trae valor descontado (más de un centavo),
	 * 2. tiene alguna novedad de las que impiden determinar el destino, y
	 * 3. la afectación manual registrada no cubre lo descontado.
	 */
	private List<Map<String, Object>> buscarValoresSinDestino(CargaArchivo cargaArchivo) throws Throwable {
		List<Map<String, Object>> pendientes = new ArrayList<>();
		Long codigoFilial = cargaArchivo.getFilial() != null ? cargaArchivo.getFilial().getCodigo() : null;

		List<DetalleCargaArchivo> detallesCarga =
			detalleCargaArchivoDaoService.selectByCargaArchivo(cargaArchivo.getCodigo());

		if (detallesCarga == null || detallesCarga.isEmpty()) {
			return pendientes;
		}

		for (DetalleCargaArchivo detalle : detallesCarga) {
			String codigoProducto = detalle.getCodigoPetroProducto();

			List<ParticipeXCargaArchivo> participesDetalle =
				participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());

			if (participesDetalle == null) {
				continue;
			}

			for (ParticipeXCargaArchivo participe : participesDetalle) {
				double totalDescontado = nullSafe(participe.getTotalDescontado());

				// Sin valor descontado no hay nada que cruzar.
				if (totalDescontado <= 0.01) {
					continue;
				}

				List<NovedadParticipeCarga> novedades =
					novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());

				List<Long> novedadesSinResolver = novedadesQueRequierenAfectacion(participe, novedades, codigoFilial);
				if (modoReproceso) {
					// MODO REPROCESO (C4): el rol sin partícipe no bloquea la carga — la fase 3
					// lo reporta como novedad y lo descuenta del recibido (ver
					// registrarSinParticipeReproceso). Cualquier OTRA novedad bloqueante sigue
					// bloqueando exactamente igual.
					novedadesSinResolver.removeIf(tipo -> tipo != null
						&& tipo.intValue() == ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO);
					// D-010: el MONTO_INCONSISTENTE heredado de la validación original (calculado
					// contra la tabla del SAA de cuando se subió la carga, no contra el ancla) no
					// bloquea: el excedente que NO fue devuelto va a la cascada del motor — cuotas
					// vencidas primero, luego las siguientes —, que es lo que de verdad pasó. El
					// devuelto viaja en la lista de la solicitud y va a aporte. Si el motor no
					// encuentra cuota para todo, registra su propio MONTO_INCONSISTENTE y el
					// invariante recibido == aplicado frena la corrida: no se pierde dinero.
					novedadesSinResolver.removeIf(tipo -> tipo != null
						&& tipo.intValue() == ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE);
					// D-019: PRESTAMO_NO_ENCONTRADO se calculó con el estado del préstamo de cuando
					// se subió la carga. En reproceso vale el estado a la fecha de corte: si existe
					// una operación del producto vigente al corte, la fase 3 la encuentra y no bloquea.
					if (novedadesSinResolver.contains((long) ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO)
							&& existePrestamoVigenteAlCorte(participe, codigoProducto, cargaArchivo)) {
						novedadesSinResolver.removeIf(tipo -> tipo != null
							&& tipo.intValue() == ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO);
					}
					// D-019 ampliada: CUOTA_NO_ENCONTRADA se calculó con las cuotas como las dejó la
					// migración (la del mes saldada sin ningún pago). El paso previo
					// (prepararOperacionesVigentesAlCorteReproceso) ya reabrió esas cuotas: si alguna
					// operación vigente al corte tiene ahora una cuota pendiente, la fase 3 la
					// encuentra y no bloquea. Si no la tiene, sigue bloqueando como antes.
					if (novedadesSinResolver.contains((long) ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA)
							&& existeCuotaPendienteAlCorte(participe, codigoProducto, cargaArchivo)) {
						novedadesSinResolver.removeIf(tipo -> tipo != null
							&& tipo.intValue() == ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA);
					}
				}
				if (novedadesSinResolver.isEmpty()) {
					// El proceso automático sabe qué hacer con este valor.
					continue;
				}
				if (modoReproceso) {
					// MODO REPROCESO (C4): un rol sin partícipe en el SAA va SIEMPRE a
					// registrarSinParticipeReproceso, traiga la novedad que traiga de la carga
					// original (en la 352 hay filas sin entidad con tipo 10): no bloquea acá.
					List<Entidad> entidadesFila = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
					if (entidadesFila == null || entidadesFila.isEmpty()) {
						continue;
					}
				}

				double valorConDestino = totalAfectadoManualmente(novedades);
				// D-010: lo declarado como excedente devuelto tiene destino (aporte EXCEDENTE_PETRO).
				com.saa.ejb.crd.service.dto.ExcedenteDevueltoReproceso excedenteFila =
					excedenteDevueltoDeFila(participe, codigoProducto);
				if (excedenteFila != null && excedenteFila.getMonto() != null) {
					valorConDestino += excedenteFila.getMonto();
				}
				double valorSinDestino = totalDescontado - valorConDestino;

				// La misma tolerancia de $1 que usa el resto del módulo para redondeos.
				if (valorSinDestino <= TOLERANCIA) {
					continue;
				}

				Map<String, Object> pendiente = new HashMap<>();
				pendiente.put("codigoParticipeCarga", participe.getCodigo());
				pendiente.put("codigoPetro", participe.getCodigoPetro());
				pendiente.put("nombre", participe.getNombre());
				pendiente.put("codigoProducto", codigoProducto);
				pendiente.put("totalDescontado", totalDescontado);
				pendiente.put("valorConDestino", valorConDestino);
				pendiente.put("valorSinDestino", valorSinDestino);
				pendiente.put("codigosNovedad", novedadesSinResolver);
				pendiente.put("novedades", describirNovedades(novedadesSinResolver));

				pendientes.add(pendiente);

				System.out.println("⛔ Valor sin destino - Rol " + participe.getCodigoPetro()
					+ " (" + participe.getNombre() + ") - Producto " + codigoProducto
					+ " - Sin aplicar: $" + String.format("%,.2f", valorSinDestino)
					+ " de $" + String.format("%,.2f", totalDescontado)
					+ " - Novedad: " + describirNovedades(novedadesSinResolver));
			}
		}

		return pendientes;
	}

	/**
	 * Corta el procesamiento si la afectación manual de algún partícipe INVENTA dinero —
	 * VALIDACION-TOPE-AFECTACION-MANUAL.md, decisión del usuario 2026-09-02.
	 *
	 * El caso real (carga 449): SANCHEZ PRADO (rol 7508) tenía $298,19 descontados y las
	 * afectaciones manuales (AVPC) le asignaron $439,59 — $141,40 que nadie descontó, aplicados
	 * igual porque el motor de pagos hace exactamente lo que se le pide y nada validaba el
	 * pedido. El asiento de aplicación termina repartiendo más de lo que el asiento de reparto
	 * (transferencias) recibió, y cuadra consigo mismo: no se ve sin comparar contra el archivo.
	 *
	 * <p><b>El tope es POR PARTÍCIPE (rol Petro), no por producto</b> — reclasificar entre
	 * préstamos y aportes del MISMO partícipe es una operación legítima del operador (caso
	 * SARMIENTO en la misma carga: $52,26 descontados como préstamo, aplicados a un aporte). Lo
	 * único prohibido es que la SUMA de sus afectaciones supere la SUMA de lo que se le descontó,
	 * sin importar en qué producto haya sido cada cosa.</p>
	 *
	 * <p><b>Todo o nada, en una sola pasada</b> (§4 del plan): se evalúan TODOS los partícipes de
	 * la carga antes de aplicar el primer pago, y se reportan TODAS las violaciones juntas — igual
	 * que {@code productosSinConfiguracion} del G48. Fallar en el partícipe 800 de 2.500 después
	 * de 20 minutos de proceso, y solo con ESE partícipe, es el error que esto evita repetir.</p>
	 *
	 * <p>Tolerancia de UN CENTAVO — no el {@code TOLERANCIA} ($1) del resto de la clase: acá no se
	 * está perdonando redondeo, se está detectando dinero inventado, y $1 de margen en esa
	 * dirección es exactamente la clase de plata que este método existe para atajar.</p>
	 */
	/**
	 * Tope de afectación manual, de solo lectura, para UN partícipe — VALIDACION-TOPE-
	 * AFECTACION-MANUAL.md §8. La pantalla de afectación lo usa para mostrarle al operador el
	 * disponible/afectado/restante MIENTRAS TRABAJA, sin reimplementar la regla en el frontend:
	 * reutiliza {@link #totalAfectadoManualmente}, {@link #excesoYRestante} y (desde el
	 * 2026-09-03, §10) {@link #disponibleParaTope} — las mismas piezas que usa
	 * {@link #validarTopeAfectacionManualPorParticipe} para bloquear — el día que la fórmula
	 * cambie, cambia en esos métodos y acá se entera sola.
	 *
	 * A diferencia de la validación batch (que escanea TODA la carga en una pasada porque
	 * necesita verlos a todos a la vez), esto es una consulta puntual: acotada a las filas de
	 * un solo rol Petro — apropiado para una pantalla que consulta un partícipe por vez.
	 *
	 * <b>Informa, no bloquea.</b> Que no haya excedente acá no garantiza que la carga procese:
	 * la validación real sigue siendo {@link #validarTopeAfectacionManualPorParticipe}.
	 */
	@Override
	public Map<String, Object> obtenerTopeAfectacionManual(Long codigoCargaArchivo, Long codigoPetro)
			throws Throwable {
		System.out.println("CargaArchivoPetroService.obtenerTopeAfectacionManual - carga "
			+ codigoCargaArchivo + " rol " + codigoPetro);

		if (codigoCargaArchivo == null) {
			throw new IncomeException("El id de la carga es obligatorio");
		}
		if (codigoPetro == null) {
			throw new IncomeException("El codigoPetro es obligatorio");
		}
		CargaArchivo cargaArchivo = cargaArchivoService.selectById(codigoCargaArchivo);
		if (cargaArchivo == null) {
			throw new IncomeException("No se encontró la carga con ID: " + codigoCargaArchivo);
		}
		Long codigoFilial = cargaArchivo.getFilial() != null ? cargaArchivo.getFilial().getCodigo() : null;

		List<ParticipeXCargaArchivo> participes =
			participeXCargaArchivoDaoService.selectByCodigoPetroEnCarga(codigoPetro, codigoCargaArchivo);

		double totalPozos = 0.0;
		double totalDescontadoParticipante = 0.0;
		List<NovedadParticipeCarga> novedades = new ArrayList<>();
		if (participes != null) {
			for (ParticipeXCargaArchivo participe : participes) {
				List<NovedadParticipeCarga> novedadesParticipe =
					novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
				if (novedadesParticipe != null) {
					novedades.addAll(novedadesParticipe);
				}
				// CORRECCIÓN 2026-09-03 (§10, CUARTA vuelta) — ver el javadoc de
				// disponibleParaTope: pozo por fila SIN CAPEAR (el pozo de una novedad puede
				// abarcar más de una fila, caso HS). El cap va acá, al final, sobre la suma de
				// TODAS las filas del participante. codigoFilial es solo para el tipo 4
				// estructural (CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE).
				totalPozos += disponibleParaTope(participe, novedadesParticipe, codigoFilial);
				totalDescontadoParticipante += nullSafe(participe.getTotalDescontado());
			}
		}
		double disponible = Math.min(totalPozos, totalDescontadoParticipante);

		double afectado = totalAfectadoManualmente(novedades);
		double[] excesoRestante = excesoYRestante(disponible, afectado);

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("codigoPetro", codigoPetro);
		resultado.put("disponible", disponible);
		resultado.put("afectado", afectado);
		resultado.put("exceso", excesoRestante[0]);
		resultado.put("restante", excesoRestante[1]);
		return resultado;
	}

	/**
	 * exceso/restante a partir de (disponible, afectado) — fórmula ÚNICA (VALIDACION-TOPE-
	 * AFECTACION-MANUAL.md §8), compartida por la validación batch y el endpoint de consulta.
	 *
	 * @return {@code [exceso, restante]}, ambos {@code >= 0}
	 */
	private double[] excesoYRestante(double disponible, double afectado) {
		double exceso = Math.max(0.0, afectado - disponible);
		double restante = Math.max(0.0, disponible - afectado);
		return new double[] { exceso, restante };
	}

	/**
	 * Última red antes de generar los asientos — VALIDACION-TOPE-AFECTACION-MANUAL.md §11
	 * (2026-09-03, decisión del usuario: *"que no permita procesar si el asiento no va a
	 * cuadrar hasta el centavo con el dinero recibido"*). El disparador fue que el usuario
	 * viene RESTAURANDO LA BASE DE PRODUCCIÓN cada vez que una carga sale mal: el proceso
	 * TERMINA BIEN y genera un asiento equivocado, y eso no se puede deshacer. Si el proceso se
	 * niega a terminar en cambio, la transacción revierte sola y no hay nada que restaurar.
	 *
	 * <p><b>Por qué ACÁ y no antes de aplicar:</b> antes de aplicar solo se puede PREDECIR lo
	 * que el proceso automático va a hacer — predecirlo es simular el proceso entero. Acá ya no
	 * hay que predecir nada: los pagos y aportes YA ESTÁN aplicados (el bucle de arriba
	 * terminó) y se pueden contar exactos.</p>
	 *
	 * <p><b>Por qué ACÁ y no después de los asientos:</b> si el asiento ya se generó,
	 * revertirlo es un problema contable. Si esto lanza ANTES —{@code @TransactionAttribute
	 * REQUIRED}, misma transacción que aplicó todo— la transacción revierte sola y no queda
	 * rastro: exactamente el punto entre "terminé de aplicar" y "genero el asiento".</p>
	 *
	 * <p><b>Invariante:</b> recibido ({@code SUM TransferenciaCargaPetro} vigentes — misma
	 * fuente que usa el asiento de reparto) {@code ==} aplicado ({@code SUM PagoPrestamo}
	 * vigentes + {@code SUM Aporte} de la carga — mismas fuentes que usa el asiento de
	 * aplicación), tolerancia de UN CENTAVO.</p>
	 *
	 * <p>El desglose por partícipe del mensaje reutiliza
	 * {@link com.saa.ejb.crd.service.DistribucionBandaService#obtenerDiferencia} — cuarto
	 * consumidor de esa cuenta (descontado/aplicado/manual/automático por partícipe), nunca una
	 * copia nueva.</p>
	 *
	 * <p><b>No reemplaza</b> al prevuelo (§9) ni a la validación del tope (§8/§10): esos
	 * avisan ANTES de esperar 20 minutos. Esta es la red que garantiza que un asiento
	 * descuadrado nunca llegue a existir — los tres se complementan.</p>
	 */
	private void validarRecibidoIgualAplicado(CargaArchivo cargaArchivo) throws Throwable {
		final double TOLERANCIA_INVARIANTE = 0.01;

		Long idCarga = cargaArchivo.getCodigo();
		System.out.println("=== VALIDANDO RECIBIDO == APLICADO ANTES DE GENERAR ASIENTOS (carga "
			+ idCarga + ") ===");

		double recibido = transferenciaCargaPetroDaoService.sumaValorVigentesByCarga(idCarga);
		String notaReproceso = "";
		if (modoReproceso && montoSinParticipeCargaActual > 0.0) {
			// MODO REPROCESO (C4): lo descontado a roles Petro sin partícipe en el SAA no se
			// aplicó a propósito (quedó como novedad) — se resta del recibido para que el
			// invariante compare contra lo que SÍ tenía dónde aplicarse.
			recibido = recibido - montoSinParticipeCargaActual;
			notaReproceso = " (recibido neto de $" + String.format("%,.2f", montoSinParticipeCargaActual)
				+ " de " + sinParticipeCargaActual.size() + " fila(s) sin partícipe, " + etiquetaReproceso + ")";
		}

		double aplicadoPrestamos = 0.0;
		List<PagoPrestamo> pagosVigentes = pagoPrestamoDaoService.selectVigentesByCargaArchivo(idCarga);
		if (pagosVigentes != null) {
			for (PagoPrestamo pago : pagosVigentes) {
				aplicadoPrestamos += nullSafe(pago.getValor());
			}
		}
		double aplicadoAportes = 0.0;
		List<Aporte> aportesCarga = aporteDaoService.selectByCarga(idCarga);
		if (aportesCarga != null) {
			for (Aporte aporte : aportesCarga) {
				aplicadoAportes += nullSafe(aporte.getValor());
			}
		}
		double aplicado = aplicadoPrestamos + aplicadoAportes;
		double diferencia = aplicado - recibido;

		if (Math.abs(diferencia) <= TOLERANCIA_INVARIANTE) {
			System.out.println("✅ Recibido $" + String.format("%,.2f", recibido) + " == Aplicado $"
				+ String.format("%,.2f", aplicado) + " (carga " + idCarga + ")" + notaReproceso);
			return;
		}

		StringBuilder mensaje = new StringBuilder();
		mensaje.append("No se generan los asientos de la carga ").append(idCarga)
		       .append(": el asiento no cuadraría con el dinero recibido. Recibido (transferencias) $")
		       .append(String.format("%,.2f", recibido)).append(notaReproceso)
		       .append(", aplicado $").append(String.format("%,.2f", aplicado))
		       .append(" (préstamos $").append(String.format("%,.2f", aplicadoPrestamos))
		       .append(" + aportes $").append(String.format("%,.2f", aplicadoAportes)).append(")")
		       .append(", diferencia $").append(String.format("%,.2f", Math.abs(diferencia)))
		       .append(diferencia > 0 ? " aplicados de más" : " aplicados de menos")
		       .append(" de lo recibido. La carga NO quedó procesada: corrija y vuelva a intentar.");

		// MODO REPROCESO (§4.11): las filas con descuento que terminaron sin aplicar su dinero, con
		// rol, producto, monto y motivo (máximo 50), y las novedades de aporte generadas en la
		// corrida — para no tener que reconstruirlas del log.
		if (modoReproceso && (!filasSinPagoReproceso.isEmpty() || !novedadesGeneradasCargaActual.isEmpty())) {
			final int MAXIMO_FILAS_SIN_PAGO = 50;
			mensaje.append(" Filas sin pago en esta corrida: ").append(filasSinPagoReproceso.size())
			       .append(" ($").append(String.format("%,.2f", montoFilasSinPagoReproceso)).append(")");
			int listadas = 0;
			for (String linea : filasSinPagoReproceso) {
				if (listadas >= MAXIMO_FILAS_SIN_PAGO) {
					mensaje.append("\n  ... y ").append(filasSinPagoReproceso.size() - listadas).append(" fila(s) más.");
					break;
				}
				mensaje.append("\n  - ").append(linea);
				listadas++;
			}
			for (String linea : novedadesGeneradasCargaActual) {
				if (listadas >= MAXIMO_FILAS_SIN_PAGO) {
					break;
				}
				mensaje.append("\n  - AH: ").append(linea);
				listadas++;
			}
			mensaje.append("\n");
		}

		// El desglose por partícipe es un PLUS para el mensaje (dice a quién mirar primero), no
		// la validación en sí — si falla, no puede tapar el hallazgo real de arriba.
		try {
			ResultadoDiferenciaDistribucionBanda diferenciaPorParticipe =
				distribucionBandaService.obtenerDiferencia(DsbnOrigen.CARGA_PETRO, idCarga);
			List<DiferenciaParticipeDistribucionBanda> detalle = diferenciaPorParticipe != null
				? diferenciaPorParticipe.getDetalle() : null;
			if (detalle != null && !detalle.isEmpty()) {
				mensaje.append(" Partícipes descuadrados:");
				int listados = 0;
				for (DiferenciaParticipeDistribucionBanda item : detalle) {
					if (listados >= MAXIMO_DETALLES_EN_MENSAJE) {
						mensaje.append("\n  ... y ").append(detalle.size() - listados).append(" partícipe(s) más.");
						break;
					}
					mensaje.append("\n  - Rol ").append(item.getCodigoPetro())
					       .append(" cédula ").append(item.getCedula())
					       .append(" ").append(item.getParticipe())
					       .append(": descontado $").append(String.format("%,.2f", item.getDescontado()))
					       .append(", aplicado $").append(String.format("%,.2f", item.getAplicadoTotal()))
					       .append(" (manual $").append(String.format("%,.2f", item.getAplicadoManual()))
					       .append(", automático $").append(String.format("%,.2f", item.getAplicadoAutomatico()))
					       .append("), diferencia $").append(String.format("%,.2f", item.getDiferencia()))
					       .append(item.getDiferencia() > 0 ? " de más" : " de menos").append(".");
					listados++;
				}
			}
		} catch (Throwable e) {
			System.err.println("No se pudo armar el desglose por partícipe para el mensaje del"
				+ " invariante recibido==aplicado (carga " + idCarga + "): " + e.getMessage());
		}

		System.err.println(mensaje.toString());
		throw new IncomeException(mensaje.toString());
	}

	private void validarTopeAfectacionManualPorParticipe(CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("=== VALIDANDO TOPE DE AFECTACION MANUAL POR PARTICIPE (rol Petro) ===");

		// §14: solo los EXCESOS bloquean acá. Los FALTANTES (repartió menos que el pozo) son
		// correctos por diseño — lo que el operador no reparte, no lo reparte nadie, y eso no es
		// un error de la carga — así que no se validan en este método; los expone el prevuelo
		// (§9/§14) para que el operador los vea ANTES de llegar hasta acá.
		List<Map<String, Object>> violaciones = calcularTopeAfectacionManual(cargaArchivo).excesos();

		if (violaciones.isEmpty()) {
			System.out.println("✅ Ningún partícipe excede el tope de afectación manual");
			return;
		}

		StringBuilder mensaje = new StringBuilder();
		mensaje.append("No se puede procesar el archivo: ").append(violaciones.size())
		       .append(" partícipe(s) tienen afectaciones manuales (AVPC) que superan lo descontado")
		       .append(" en sus productos con novedad bloqueante (§10: no cuenta lo que va a aplicar")
		       .append(" el flujo automático). Corrija las afectaciones y vuelva a procesar.");

		int listados = 0;
		for (Map<String, Object> v : violaciones) {
			if (listados >= MAXIMO_DETALLES_EN_MENSAJE) {
				mensaje.append("\n  ... y ").append(violaciones.size() - listados).append(" partícipe(s) más.");
				break;
			}
			mensaje.append("\n  - Rol ").append(v.get("codigoPetro"))
			       .append(" cédula ").append(v.get("cedula"))
			       .append(" ").append(v.get("participe"))
			       .append(": disponible $").append(String.format("%,.2f", (Double) v.get("disponible")))
			       .append(", afectado $").append(String.format("%,.2f", (Double) v.get("afectado")))
			       .append(", exceso $").append(String.format("%,.2f", (Double) v.get("exceso")))
			       .append(". AVPC: ").append(v.get("avpc")).append(".");
			listados++;
		}

		System.err.println(mensaje.toString());
		throw new IncomeException(mensaje.toString());
	}

	/**
	 * Los dos resultados del núcleo del tope por partícipe — VALIDACION-TOPE-AFECTACION-
	 * MANUAL.md §9 y §14. {@code excesos}: partícipes cuya afectación manual (AVPC) supera su
	 * pozo disponible — ESO bloquea el procesamiento. {@code faltantes}: partícipes que TIENEN
	 * afectación manual pero repartieron MENOS que su pozo — eso NO bloquea (es válido por
	 * diseño: lo que el operador no reparte, no lo reparte nadie), pero es el hueco que el §14
	 * cerró en el prevuelo, para que el usuario no espere el proceso entero para enterarse.
	 * Listas separadas a propósito: son dos acciones opuestas para el operador (bajarle a unos,
	 * subirle a otros), nunca un solo número con signo.
	 */
	private record TopeAfectacionManual(List<Map<String, Object>> excesos, List<Map<String, Object>> faltantes) {
	}

	/**
	 * Núcleo COMPARTIDO del tope por partícipe — ÚNICA definición (VALIDACION-TOPE-AFECTACION-
	 * MANUAL.md §9): la usan la validación que bloquea al procesar
	 * ({@link #validarTopeAfectacionManualPorParticipe}) y el prevuelo de solo lectura
	 * ({@link #obtenerPrevueloAfectacionManual}), que corre esta MISMA pasada mientras el
	 * operador todavía está repartiendo, sin bloquear. Es la tercera vez que aparece esta
	 * necesidad exacta (la validación, el tope por partícipe del §8, y esto) — las tres tienen
	 * que dar el mismo número siempre.
	 *
	 * <b>CORREGIDO 2026-09-03 (§10):</b> "disponible" ya no es {@code SUM(PXCADSDO)} de TODOS
	 * los productos del partícipe — es solo la de los productos con novedad bloqueante, vía
	 * {@link #disponibleParaTope}. Ver su javadoc para el caso medido (SANCHEZ PRADO, rol 7508)
	 * que probó que la regla anterior era incompleta.
	 *
	 * <b>AMPLIADO 2026-09-03 (§14):</b> además de {@code excesos} (afectado &gt; disponible),
	 * ahora también arma {@code faltantes} (afectado &lt; disponible) — medido en producción con
	 * la red del §11 ya funcionando: 4 partícipes (SANCHEZ, CABRERA, MUYULEMA, SOLANO) frenaron
	 * la carga con $35,64 sin repartir, visibles desde ANTES de arrancar el proceso pero
	 * invisibles en el prevuelo porque este solo miraba exceso. <b>Un {@code faltante} SOLO
	 * cuenta si el partícipe TIENE afectación manual</b> (AVPC no vacío, mismo criterio que
	 * {@code avpc} de abajo) — si no tiene ninguna, el flujo automático se encarga de su pozo
	 * completo y no falta nada; listar a ese partícipe sería un falso positivo (y con cientos de
	 * partícipes sin afectación manual por carga, inundaría el prevuelo).
	 *
	 * Escanea TODA la carga en UNA sola pasada — pensado para llamarse una vez por invocación,
	 * nunca por partícipe (ver el javadoc de {@link #obtenerPrevueloAfectacionManual} sobre por
	 * qué eso importa para una consulta interactiva).
	 *
	 * @return {@link TopeAfectacionManual} con las dos listas, cada entrada con
	 *         {@code codigoPetro}, {@code cedula}, {@code participe}, {@code disponible},
	 *         {@code afectado}, {@code avpc} (códigos de {@code AfectacionValoresParticipeCarga}
	 *         involucrados) y, según la lista, {@code exceso} + {@code mensaje} o
	 *         {@code faltante} + {@code mensaje}
	 */
	private TopeAfectacionManual calcularTopeAfectacionManual(CargaArchivo cargaArchivo)
			throws Throwable {
		final double TOLERANCIA_TOPE = 0.01;

		List<Map<String, Object>> excesos = new ArrayList<>();
		List<Map<String, Object>> faltantes = new ArrayList<>();

		List<DetalleCargaArchivo> detallesCarga =
			detalleCargaArchivoDaoService.selectByCargaArchivo(cargaArchivo.getCodigo());
		if (detallesCarga == null || detallesCarga.isEmpty()) {
			return new TopeAfectacionManual(excesos, faltantes);
		}

		// Acumulado por ROL PETRO (identidad del partícipe: es lo mismo con lo que se busca su
		// Entidad, ver entidadDaoService.selectByCodigoPetro más abajo), NUNCA por
		// ParticipeXCargaArchivo — esa fila es por (partícipe, producto), y agrupar por ahí es
		// exactamente el defecto "por producto" que el punto 3 de la verificación (§6) prohíbe.
		Map<Long, Double> poolPorRol = new LinkedHashMap<>();
		Map<Long, Double> totalDescontadoPorRol = new LinkedHashMap<>();
		Map<Long, String> nombrePorRol = new LinkedHashMap<>();
		Map<Long, List<NovedadParticipeCarga>> novedadesPorRol = new LinkedHashMap<>();

		// Solo para el tipo 4 estructural (CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE) — ver el
		// javadoc de disponibleParaTope.
		Long codigoFilial = cargaArchivo.getFilial() != null ? cargaArchivo.getFilial().getCodigo() : null;

		for (DetalleCargaArchivo detalle : detallesCarga) {
			List<ParticipeXCargaArchivo> participesDetalle =
				participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
			if (participesDetalle == null) {
				continue;
			}
			for (ParticipeXCargaArchivo participe : participesDetalle) {
				Long rolPetro = participe.getCodigoPetro();
				if (rolPetro == null) {
					continue;
				}
				nombrePorRol.putIfAbsent(rolPetro, participe.getNombre());

				List<NovedadParticipeCarga> novedades =
					novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());

				// Afectado (más abajo, vía totalAfectadoManualmente): TODO lo que el operador
				// tipeó manualmente para este rol, sin importar en qué producto — eso no cambia
				// con la corrección de disponible de abajo.
				if (novedades != null && !novedades.isEmpty()) {
					novedadesPorRol.computeIfAbsent(rolPetro, k -> new ArrayList<>()).addAll(novedades);
				}

				// CORRECCIÓN 2026-09-03 (§10, CUARTA vuelta): disponibleParaTope ya NO capea por
				// fila — el pozo de una novedad (caso HS) puede abarcar más de una fila. Acá se
				// acumulan los pozos y el total descontado por separado, y el cap se aplica
				// UNA vez, sobre la suma de TODAS las filas del rol (ver más abajo).
				poolPorRol.merge(rolPetro, disponibleParaTope(participe, novedades, codigoFilial), Double::sum);
				totalDescontadoPorRol.merge(rolPetro, nullSafe(participe.getTotalDescontado()), Double::sum);
			}
		}

		for (Map.Entry<Long, Double> entry : poolPorRol.entrySet()) {
			Long rolPetro = entry.getKey();
			double disponible = Math.min(entry.getValue(), totalDescontadoPorRol.getOrDefault(rolPetro, 0.0));
			List<NovedadParticipeCarga> novedadesRol =
				novedadesPorRol.getOrDefault(rolPetro, java.util.Collections.emptyList());
			// Misma regla que totalAfectadoManualmente: solo cuenta lo que el aplicador va a usar
			// realmente (una fila sin cuota y sin tipo de aporte no tiene destino y no suma).
			double afectado = totalAfectadoManualmente(novedadesRol);
			double[] excesoRestante = excesoYRestante(disponible, afectado);
			double exceso = excesoRestante[0];
			double restante = excesoRestante[1];

			if (exceso > TOLERANCIA_TOPE) {
				Map<String, Object> violacion = new HashMap<>();
				violacion.put("codigoPetro", rolPetro);
				violacion.put("participe", nombrePorRol.get(rolPetro));
				violacion.put("cedula", cedulaPorRolPetro(rolPetro));
				violacion.put("disponible", disponible);
				violacion.put("afectado", afectado);
				violacion.put("exceso", exceso);
				violacion.put("avpc", codigosAfectacionManual(novedadesRol));
				violacion.put("mensaje", "Afectación manual por encima del pozo disponible: corrija (baje) las"
					+ " afectaciones de este partícipe.");
				excesos.add(violacion);
				continue;
			}

			if (restante > TOLERANCIA_TOPE) {
				// §14: SOLO cuenta como faltante si el partícipe TIENE afectación manual — si no
				// tiene ninguna, el flujo automático se encarga de todo el pozo y no falta nada.
				List<Long> avpc = codigosAfectacionManual(novedadesRol);
				if (avpc.isEmpty()) {
					continue;
				}
				Map<String, Object> faltante = new HashMap<>();
				faltante.put("codigoPetro", rolPetro);
				faltante.put("participe", nombrePorRol.get(rolPetro));
				faltante.put("cedula", cedulaPorRolPetro(rolPetro));
				faltante.put("disponible", disponible);
				faltante.put("afectado", afectado);
				faltante.put("faltante", restante);
				faltante.put("avpc", avpc);
				faltante.put("mensaje", "Quedan $" + String.format("%,.2f", restante) + " del pozo disponible"
					+ " sin afectar: afecte el faltante a este partícipe antes de procesar, o no se le"
					+ " va a repartir (el flujo automático no corre para quien ya tiene afectación manual).");
				faltantes.add(faltante);
			}
		}
		return new TopeAfectacionManual(excesos, faltantes);
	}

	/**
	 * Prevuelo del tope de afectación manual, DE SOLO LECTURA — VALIDACION-TOPE-AFECTACION-
	 * MANUAL.md §9/§14, pedido del usuario: *"poder encontrar también la diferencia al momento
	 * de aplicar los ajustes, para revisar el error antes de que se genere"*. Corre la MISMA
	 * pasada que {@link #validarTopeAfectacionManualPorParticipe} (vía
	 * {@link #calcularTopeAfectacionManual}, única definición) pero SIN bloquear — el operador
	 * la consulta MIENTRAS reparte, que es el único momento en que corregir sale barato.
	 *
	 * <p><b>AMPLIADO 2026-09-03 (§14): ahora mira las DOS direcciones, en listas separadas.</b>
	 * Hasta acá solo devolvía EXCESO (afectado &gt; disponible). Medido en producción con la red
	 * del §11 ya funcionando: 4 partícipes (SANCHEZ, CABRERA, MUYULEMA, SOLANO) frenaron la
	 * carga con $35,64 sin repartir — un caso válido por diseño (afectación manual incompleta:
	 * el flujo automático no corre para ese partícipe, así que lo que no se reparte, no lo
	 * reparte nadie), pero invisible en este prevuelo hasta ahora, y visible desde ANTES de
	 * arrancar el proceso. Por eso ahora también trae {@code faltantes} (afectado &lt;
	 * disponible), <b>solo para partícipes que YA tienen alguna afectación manual</b> — sin eso
	 * el listado incluiría a todo partícipe sin afectación manual (el flujo automático se
	 * encarga de esos) y se llenaría de falsos positivos. <b>Las dos listas van separadas
	 * — nunca mezcladas en un solo número con signo</b>: son dos acciones opuestas para el
	 * operador (bajarle a unos, subirle a otros).</p>
	 *
	 * <p>⚠️ <b>Alcance declarado, a propósito:</b> este prevuelo SOLO ve exceso/faltante de
	 * afectaciones MANUALES contra lo descontado. NO detecta la hipótesis en investigación
	 * (2026-09-02, sql/184) de que el flujo AUTOMÁTICO aplica encima del tope manual — eso
	 * todavía no ocurrió en este punto del proceso. Un {@code participesConExceso: 0} acá NO
	 * garantiza que la carga vaya a cuadrar al aplicarla.</p>
	 *
	 * <p>Tampoco reemplaza a {@code DistribucionBandaService#obtenerDiferencia} ("¿dónde está
	 * la diferencia?"): son dos preguntas en dos momentos distintos — éste pregunta "¿lo que
	 * cargué va a descuadrar?" ANTES de aplicar; aquél pregunta "¿por qué descuadró?" DESPUÉS.</p>
	 *
	 * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
	 * @return : Map con {@code idCarga}, {@code participesConExceso}, {@code excesoTotal},
	 *           {@code detalle} (excesos — ver el javadoc de {@link #calcularTopeAfectacionManual}),
	 *           y ahora también {@code participesConFaltante}, {@code faltanteTotal} y
	 *           {@code detalleFaltante} (misma forma, para la lista de faltantes)
	 * @throws Throwable : Excepción en caso de error
	 */
	@Override
	public Map<String, Object> obtenerPrevueloAfectacionManual(Long codigoCargaArchivo) throws Throwable {
		System.out.println("CargaArchivoPetroService.obtenerPrevueloAfectacionManual - carga " + codigoCargaArchivo);

		if (codigoCargaArchivo == null) {
			throw new IncomeException("El id de la carga es obligatorio");
		}
		CargaArchivo cargaArchivo = cargaArchivoService.selectById(codigoCargaArchivo);
		if (cargaArchivo == null) {
			throw new IncomeException("No se encontró la carga con ID: " + codigoCargaArchivo);
		}

		TopeAfectacionManual tope = calcularTopeAfectacionManual(cargaArchivo);
		List<Map<String, Object>> excesos = tope.excesos();
		List<Map<String, Object>> faltantes = tope.faltantes();

		double excesoTotal = 0.0;
		for (Map<String, Object> violacion : excesos) {
			excesoTotal += (Double) violacion.get("exceso");
		}
		double faltanteTotal = 0.0;
		for (Map<String, Object> faltante : faltantes) {
			faltanteTotal += (Double) faltante.get("faltante");
		}

		Map<String, Object> resultado = new HashMap<>();
		resultado.put("idCarga", codigoCargaArchivo);
		resultado.put("participesConExceso", excesos.size());
		resultado.put("excesoTotal", Math.round(excesoTotal * 100.0) / 100.0);
		resultado.put("detalle", excesos);
		resultado.put("participesConFaltante", faltantes.size());
		resultado.put("faltanteTotal", Math.round(faltanteTotal * 100.0) / 100.0);
		resultado.put("detalleFaltante", faltantes);
		return resultado;
	}

	/**
	 * Revalida las novedades de una carga YA CARGADA, con el código ACTUAL — VALIDACION-TOPE-
	 * AFECTACION-MANUAL.md §16, pedido del usuario 2026-09-03: *"como el archivo ya estaba
	 * cargado cuando restauré la base, me imagino que no está incluyendo los valores de HS. Por
	 * eso digo que debe existir un botón de reprocesar."*
	 *
	 * <p><b>NO relee el .txt ni vuelve a insertar nada</b> — recorre los DTCA/PXCA que YA están
	 * en la base y RECALCULA sus novedades. <b>NO aplica pagos, aportes ni asientos</b>: es
	 * validación pura, la misma familia que {@link #obtenerPrevueloAfectacionManual}, no
	 * {@link #aplicarPagosArchivoPetro}.</p>
	 *
	 * <p><b>ACTUALIZA, NUNCA BORRA</b> — decisión del árbitro, no negociable: el operador ya
	 * gestionó decenas de novedades (afectaciones AVPC cuelgan de {@code NVPCCDGO}); borrar y
	 * regenerar destruiría ese trabajo. Toda escritura pasa por {@code registrarNovedad}, que
	 * desde este mismo cambio es IDEMPOTENTE: si ya existe una novedad del mismo tipo para la
	 * fila, se actualiza (mismo código, los AVPC siguen colgando de ella) — nunca se crea una
	 * segunda.</p>
	 *
	 * <p><b>Qué SÍ recalcula:</b></p>
	 * <ol>
	 * <li>El MONTO de una novedad ESTRUCTURAL (tipos 1-4) ya existente sobre una fila PH/PP,
	 * sumando el HS del mismo partícipe si corresponde ({@link #montoConHsMergeado}) — esto es
	 * lo que cierra el caso reportado (CABRERA, HS de $13,18 sin ningún pozo). <b>NO re-deriva
	 * si el tipo estructural en sí sigue siendo correcto</b> (esa identificación puede mutar
	 * {@code Entidad.rolPetroComercial}, un efecto de lado que una revalidación de montos no
	 * debe disparar de nuevo).</li>
	 * <li>Vuelve a ejecutar {@link #validarNovedadesFase2}/{@link #validarAporteAH} tal cual
	 * existen hoy — mismo código que corre en una carga nueva, sin una copia aparte.</li>
	 * </ol>
	 *
	 * <p><b>"Ya no corresponde":</b> si una novedad de tipo revalidable existía ANTES de esta
	 * corrida y el código actual NO la reconfirma (no aparece en
	 * {@link #tiposConfirmadosRevalidacion} al terminar), se la trata como superada:
	 * <b>si tiene AVPC colgando, se CONSERVA intacta</b> (informada aparte en el resumen, nunca
	 * tocada) — <b>si no tiene ninguna, se desactiva</b> ({@code estado=0}; {@link
	 * #disponibleParaTope} ya la ignora, ver su filtro). ⚠️ Ninguna otra pantalla ni endpoint de
	 * este backend filtra por {@code estado} todavía — una novedad desactivada sigue siendo
	 * visible en cualquier listado que no pase por {@code disponibleParaTope} (p.ej.
	 * {@code /nvpc/getByCargaArchivo}), y el frontend Angular (fuera de este repositorio) puede
	 * no distinguirla tampoco. Es un marcador honesto, no una garantía de que desaparece de la
	 * pantalla.</p>
	 *
	 * <p><b>Por qué esto vive acá y no en Fase 1:</b> Fase 1 corre partícipe por detalle, DENTRO
	 * del mismo bucle que inserta las filas — el HS de un partícipe puede no estar insertado
	 * todavía cuando le toca el turno a su PH/PP, según el orden de los
	 * {@code DetalleCargaArchivo}. Acá, en cambio, la carga YA está completa: cualquier HS que
	 * exista, existe con certeza para cuando se lo busca.</p>
	 *
	 * @param codigoCargaArchivo : ID de la carga (CRD.CRAR)
	 * @return Map con {@code idCarga}, {@code participesRevisados}, {@code novedadesCreadas},
	 *         {@code novedadesActualizadas}, {@code novedadesDesactivadas},
	 *         {@code novedadesConservadasPorAvpc} (y su {@code detalleConservadasPorAvpc}), y
	 *         {@code errores} (partícipes que fallaron su revalidación individual, sin abortar
	 *         el resto)
	 * @throws Throwable Excepción en caso de error
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Map<String, Object> revalidarCarga(Long codigoCargaArchivo) throws Throwable {
		System.out.println("=== REVALIDANDO CARGA (§16) - Carga: " + codigoCargaArchivo + " ===");

		if (codigoCargaArchivo == null) {
			throw new IncomeException("El id de la carga es obligatorio");
		}
		CargaArchivo cargaArchivo = cargaArchivoService.selectById(codigoCargaArchivo);
		if (cargaArchivo == null) {
			throw new IncomeException("No se encontró la carga con ID: " + codigoCargaArchivo);
		}
		if (cargaArchivo.getEstado() != null && cargaArchivo.getEstado() == 3L) {
			throw new IncomeException("La carga " + codigoCargaArchivo + " ya fue procesada (estado 3):"
				+ " revalidar sus novedades no tiene efecto sobre lo que ya se aplicó.");
		}

		List<DetalleCargaArchivo> detalles = detalleCargaArchivoDaoService.selectByCargaArchivo(codigoCargaArchivo);

		int participesRevisados = 0;
		int creadas = 0;
		int actualizadas = 0;
		int desactivadas = 0;
		List<Map<String, Object>> conservadasPorAvpc = new ArrayList<>();
		List<String> errores = new ArrayList<>();

		if (detalles != null && !detalles.isEmpty()) {
			// Reset AL ENTRAR — mismo criterio que advertenciasVigenciaCargaActual/
			// novedadesGeneradasCargaActual, ver el javadoc del campo.
			tiposConfirmadosRevalidacion.clear();
			Map<String, List<Producto>> productosCache = new HashMap<>();

			for (DetalleCargaArchivo detalle : detalles) {
				String codigoProducto = detalle.getCodigoPetroProducto();
				// El HS nunca tiene novedad propia (viaja dentro de la de su PH/PP) — nada que
				// revalidar en esta fila.
				if (CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto)) {
					continue;
				}
				List<ParticipeXCargaArchivo> participesDetalle =
					participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
				if (participesDetalle == null) {
					continue;
				}

				for (ParticipeXCargaArchivo participe : participesDetalle) {
					if (participe.getCodigo() == null) {
						continue;
					}
					participesRevisados++;
					try {
						List<NovedadParticipeCarga> antes =
							novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
						Map<Long, NovedadParticipeCarga> antesPorTipo = new HashMap<>();
						if (antes != null) {
							for (NovedadParticipeCarga n : antes) {
								if (esTipoNovedadRevalidable(n.getTipoNovedad())) {
									antesPorTipo.put(n.getTipoNovedad(), n);
								}
							}
						}

						// 1) Refresca el MONTO de una novedad ESTRUCTURAL ya existente,
						// sumando HS si corresponde — NO re-deriva si el tipo sigue vigente.
						Long tipoEstructural = participe.getNovedadesCarga();
						if (tipoEstructural != null && TIPOS_ESTRUCTURALES_REVALIDABLES.contains(tipoEstructural)
								&& antesPorTipo.containsKey(tipoEstructural)) {
							double montoRecalculado = montoConHsMergeado(participe, codigoProducto, cargaArchivo);
							registrarNovedad(participe, tipoEstructural.intValue(),
								"Novedad de identificación de fase 1, revalidada (§16): "
									+ describirNovedades(Arrays.asList(tipoEstructural)) + ".",
								null, null, 0.0, montoRecalculado);
						}

						// 2) Re-ejecuta Fase 2 con el código ACTUAL — MISMA llamada que hace
						// ejecutarValidacionesFase2 en una carga nueva (validarNovedadesFase2 ya
						// redirige internamente a validarAporteAH para el producto AH), sin
						// duplicar esa rama acá.
						validarNovedadesFase2(participe, codigoProducto, cargaArchivo, productosCache);

						// 3) "¿Ya no corresponde?": revalidable que existía ANTES y no fue
						// reconfirmada en esta corrida.
						Set<Long> confirmadosFila = tiposConfirmadosRevalidacion
							.getOrDefault(participe.getCodigo(), java.util.Collections.emptySet());
						for (Map.Entry<Long, NovedadParticipeCarga> entry : antesPorTipo.entrySet()) {
							Long tipo = entry.getKey();
							NovedadParticipeCarga existente = entry.getValue();
							if (confirmadosFila.contains(tipo)) {
								continue; // reconfirmada esta corrida — sigue vigente
							}
							if (existente.getEstado() != null && existente.getEstado().longValue() == 0L) {
								continue; // ya estaba desactivada
							}
							List<AfectacionValoresParticipeCarga> avpc =
								afectacionValoresParticipeCargaDaoService.selectByNovedad(existente.getCodigo());
							if (avpc != null && !avpc.isEmpty()) {
								Map<String, Object> conservada = new HashMap<>();
								conservada.put("codigoPetro", participe.getCodigoPetro());
								conservada.put("participe", participe.getNombre());
								conservada.put("novedad", existente.getCodigo());
								conservada.put("tipoNovedad", tipo);
								conservada.put("avpc", avpc.size());
								conservadasPorAvpc.add(conservada);
								System.out.println("⚠️ (§16) Novedad " + existente.getCodigo() + " (tipo " + tipo
									+ ") del partícipe " + participe.getCodigoPetro() + " ya NO corresponde con"
									+ " el código actual, pero tiene " + avpc.size() + " AVPC colgando —"
									+ " SE CONSERVA intacta, no se toca.");
								continue;
							}
							existente.setEstado(0L);
							novedadParticipeCargaService.saveSingle(existente);
							desactivadas++;
							System.out.println("(§16) Novedad " + existente.getCodigo() + " (tipo " + tipo
								+ ") del partícipe " + participe.getCodigoPetro() + " desactivada: el código"
								+ " actual ya no la produce y no tenía AVPC.");
						}

						// 4) Contar creadas/actualizadas comparando contra el "antes".
						List<NovedadParticipeCarga> despues =
							novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
						if (despues != null) {
							for (NovedadParticipeCarga n : despues) {
								if (!esTipoNovedadRevalidable(n.getTipoNovedad())) {
									continue;
								}
								if (!antesPorTipo.containsKey(n.getTipoNovedad())) {
									creadas++;
								} else if (confirmadosFila.contains(n.getTipoNovedad())) {
									actualizadas++;
								}
							}
						}
					} catch (Throwable e) {
						String msg = "Partícipe " + participe.getCodigoPetro() + " ("
							+ participe.getNombre() + "): " + e.getMessage();
						System.err.println("ERROR revalidando (§16) - " + msg);
						e.printStackTrace();
						errores.add(msg);
					}
				}
			}
		}

		Map<String, Object> resumen = new LinkedHashMap<>();
		resumen.put("idCarga", codigoCargaArchivo);
		resumen.put("participesRevisados", participesRevisados);
		resumen.put("novedadesCreadas", creadas);
		resumen.put("novedadesActualizadas", actualizadas);
		resumen.put("novedadesDesactivadas", desactivadas);
		resumen.put("novedadesConservadasPorAvpc", conservadasPorAvpc.size());
		resumen.put("detalleConservadasPorAvpc", conservadasPorAvpc);
		resumen.put("errores", errores);
		System.out.println("=== REVALIDACIÓN TERMINADA (carga " + codigoCargaArchivo + "): "
			+ participesRevisados + " partícipes, " + creadas + " creadas, " + actualizadas + " actualizadas, "
			+ desactivadas + " desactivadas, " + conservadasPorAvpc.size() + " conservadas por AVPC, "
			+ errores.size() + " errores ===");
		return resumen;
	}

	/** Tipos de novedad que {@link #revalidarCarga} sabe recalcular — ver
	 * {@link #TIPOS_ESTRUCTURALES_REVALIDABLES}, {@link #TIPOS_FASE2_PRESTAMO_REVALIDABLES} y
	 * {@link #TIPOS_FASE2_APORTE_REVALIDABLES}. Cualquier otro tipo (p.ej. las novedades que
	 * genera la APLICACIÓN, no la validación — MONTO_INCONSISTENTE por excedente del motor de
	 * pagos, del §15) queda fuera a propósito: la revalidación no aplica pagos, no tiene cómo
	 * recalcular esas. */
	private boolean esTipoNovedadRevalidable(Long tipoNovedad) {
		return tipoNovedad != null && (TIPOS_ESTRUCTURALES_REVALIDABLES.contains(tipoNovedad)
			|| TIPOS_FASE2_PRESTAMO_REVALIDABLES.contains(tipoNovedad)
			|| TIPOS_FASE2_APORTE_REVALIDABLES.contains(tipoNovedad));
	}

	/**
	 * Pozo de novedades BLOQUEANTES de UNA fila PXCA (un partícipe, un producto), SIN CAPEAR —
	 * CORRECCIÓN 2026-09-03, VALIDACION-TOPE-AFECTACION-MANUAL.md §10, CUARTA VUELTA.
	 *
	 * <p><b>⛔ Este método YA NO capea nada — devuelve el pozo crudo de la fila.</b> El cap vive
	 * en los llamadores ({@link #calcularTopeAfectacionManual} y
	 * {@link #obtenerTopeAfectacionManual}), y es al total descontado del PARTICIPANTE (suma de
	 * TODAS sus filas PXCA en la carga), nunca al de esta fila sola. Motivo, verificado en
	 * producción el 2026-09-03: <b>el pozo de una novedad puede abarcar más de una fila.</b> El
	 * HS (seguro de incendio) no tiene novedad propia — su monto viaja DENTRO de la novedad del
	 * préstamo, porque la validación en {@code aplicarPagoParticipe} (~línea 2798) suma
	 * {@code montoHS} al monto que valida esa novedad. Con SANCHEZ (rol 7508): la novedad de su
	 * fila PH trae {@code montoRecibido = 298,19} (PH 282,77 + HS 15,42), pero el
	 * {@code totalDescontado} de ESA fila es solo 282,77. Capear acá, por fila, truncaba el pozo
	 * combinado al descontado de la fila del préstamo, perdiendo el HS SIEMPRE — no un caso
	 * aislado: los 35 partícipes bloqueados con este defecto tenían TODOS hipotecario o
	 * prendario con seguro, y es el mismo patrón en cada uno de ellos.</p>
	 *
	 * <p><b>Historial breve de las vueltas previas</b> (detalle completo en el .md, no acá,
	 * para que este javadoc no seleccione crecer sin límite en la quinta vuelta):
	 * 1ª — "tope = total descontado del partícipe": no distinguía qué iba a aplicar el
	 * automático. 2ª — "tope = descontado en productos con novedad bloqueante": no cubría un
	 * sobrante PARCIAL dentro de un producto ya aplicado en parte. 3ª — "agrupar por
	 * (producto, préstamo) y tomar el máximo": esos dos campos vienen {@code null} en TODAS las
	 * novedades de este archivo, así que agrupar por ellos colapsaba todas las novedades
	 * bloqueantes de la fila a un solo grupo — <b>no son una clave, no se debe volver a agrupar
	 * por ellos.</b> Reemplazada por SUMAR los pozos, con dedup SOLO cuando hay clave real
	 * (mismo {@code tipoNovedad} + mismo {@code codigoPrestamo} NO NULO). Esa suma sigue vigente
	 * acá abajo — lo único que cambia en esta 4ª vuelta es DÓNDE se capea el total.</p>
	 *
	 * <p>El pozo de cada novedad BLOQUEANTE es el mismo que la pantalla de afectación le ofrece
	 * al operador — {@code detalle-consulta-carga.component.ts#montoDisponibleAfectacion}:
	 * {@code montoRecibido ?? montoDiferencia ?? montoEsperado ?? 0} — para que pantalla y tope
	 * NUNCA discrepen.</p>
	 *
	 * <p>⛔ <b>CORREGIDO 2026-09-03 (QUINTA vuelta) — supuesto falso sobre los 4 tipos
	 * ESTRUCTURALES, ELIMINADO.</b> Hasta acá este javadoc decía que PARTICIPE_NO_ENCONTRADO,
	 * CODIGO_ROL_DUPLICADO, NOMBRE_ENTIDAD_DUPLICADO y CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE
	 * "no tienen fila {@code NovedadParticipeCarga}, viven solo en
	 * {@code participe.getNovedadesCarga()}/{@code getNovedadesFinancieras()}", y sobre eso
	 * había un bloque acá que le sumaba pozo aparte al tipo 4. <b>Es falso: {@code
	 * almacenaRegistros} (Fase 1, ~línea 491) YA crea una fila NVPC real para CUALQUIERA de los 4
	 * tipos que bloquee</b> (misma condición, {@code tipoEstructuralBloquea}), y esa fila entra
	 * al bucle de arriba como cualquier otra novedad bloqueante — los 4 tipos están en
	 * {@code FamiliaNovedadCarga.TIPOS_QUE_EXIGEN_AFECTACION}. El bloque que sumaba aparte
	 * DUPLICABA ese pozo. Confirmado en producción con CABRERA (rol 2581, carga 449, tipo 4):
	 * salía 646,82 en vez de 323,41. El campo plano ({@code participe.getNovedadesCarga()}) y la
	 * fila NVPC coexisten para estos 4 tipos — el campo es solo la marca de identificación de
	 * Fase 1, la fila NVPC es la que efectivamente participa del pozo, y no hace falta ni hay
	 * que sumarla dos veces.</p>
	 *
	 * <p>⚠️ <b>Gap real, todavía sin corregir acá — a propósito, ver VALIDACION-TOPE-AFECTACION-
	 * MANUAL.md §16:</b> la fila NVPC que {@code almacenaRegistros} crea para estos 4 tipos usa
	 * {@code montoRecibido = participe.getTotalDescontado()} — SOLO el descontado de ESTA fila,
	 * sin sumar el HS del partícipe (a diferencia de {@code validarNovedadesFase2}, que si el
	 * producto es PH/PP SÍ busca el HS y lo suma antes de registrar sus propias novedades). Esto
	 * es por diseño de Fase 1: corre PARTÍCIPE POR DETALLE, dentro del mismo bucle que inserta
	 * las filas, así que el HS del mismo partícipe puede no estar insertado todavía cuando le
	 * toca el turno al PH — por eso Fase 2 existe y corre DESPUÉS de que todo esté insertado.
	 * Medido en CABRERA (carga 449): su fila PH tiene tipo 4 con montoRecibido 323,41, pero su HS
	 * es 13,18 aparte y NUNCA tiene novedad propia (el HS jamás la tiene) — esos 13,18 no están
	 * en NINGÚN pozo, el operador no puede afectarlos ni queriendo. No se corrige acá (sumando el
	 * HS al pozo dentro de este método) porque la pantalla lee el monto de la fila NVPC
	 * DIRECTAMENTE (misma fórmula citada arriba) — si este método sumara el HS pero la fila
	 * siguiera diciendo 323,41, pantalla y tope volverían a discrepar, el mismo error que motivó
	 * la regla de "usar siempre el mismo cálculo que la pantalla". La corrección real es
	 * actualizar la fila NVPC misma para que refleje el HS (§16, revalidación).</p>
	 *
	 * <p><b>Principio para cualquier ajuste futuro de este cálculo:</b> "ante la duda, el tope se
	 * equivoca por abajo" sigue valiendo para no INVENTAR plata que no existe — por eso el cap
	 * (en el llamador) sigue siendo el total descontado, ahora del participante completo. Pero
	 * un tope demasiado bajo tampoco es gratis: le impide al operador repartir plata que sí está
	 * y frena la carga sin remedio — eso fue exactamente lo que costó capear por fila. El límite
	 * duro que nunca se puede cruzar es el total descontado del PARTICIPANTE; por debajo de ese
	 * límite, sumar (no maximizar, y no capear por fila) es lo que corresponde.</p>
	 *
	 * <b>ÚNICA definición del pozo por fila</b> — la usan {@link #calcularTopeAfectacionManual}
	 * (y por lo tanto {@link #validarTopeAfectacionManualPorParticipe} y
	 * {@link #obtenerPrevueloAfectacionManual}) y {@link #obtenerTopeAfectacionManual}, que son
	 * también los ÚNICOS lugares donde se aplica el cap por participante.
	 */
	private double disponibleParaTope(ParticipeXCargaArchivo participe, List<NovedadParticipeCarga> novedadesDeEstaFila,
			Long codigoFilial) {
		double totalPozos = 0.0;

		// Dedupe SOLO con clave real (tipoNovedad + codigoPrestamo NO NULO). Sin préstamo no hay
		// forma de reconocer "misma plata dos veces" — se suma cada novedad tal cual.
		Map<String, Double> poolPorClaveDedup = new HashMap<>();
		List<Double> poolsSinClaveDedup = new ArrayList<>();

		if (novedadesDeEstaFila != null) {
			for (NovedadParticipeCarga novedad : novedadesDeEstaFila) {
				// §16 (revalidarCarga): una novedad DESACTIVADA (estado=0 — el código actual ya
				// no la produce y no tenía AVPC colgando) no aporta pozo. selectByParticipe NO
				// filtra por estado a propósito (la idempotencia de registrarNovedad necesita
				// ver las inactivas para reactivarlas) — el filtro va acá, en el único consumidor
				// para el que estado realmente importa.
				if (novedad.getEstado() != null && novedad.getEstado().longValue() == 0L) {
					continue;
				}
				if (FamiliaNovedadCarga.clasificar(novedad.getTipoNovedad(), novedad.getMontoDiferencia())
						!= FamiliaNovedadCarga.BLOQUEANTE) {
					continue;
				}
				// Mismo criterio que la pantalla: montoRecibido ?? montoDiferencia ?? montoEsperado ?? 0.
				double pool = novedad.getMontoRecibido() != null ? novedad.getMontoRecibido()
					: novedad.getMontoDiferencia() != null ? novedad.getMontoDiferencia()
					: novedad.getMontoEsperado() != null ? novedad.getMontoEsperado()
					: 0.0;
				if (novedad.getCodigoPrestamo() != null) {
					String clave = novedad.getTipoNovedad() + "|" + novedad.getCodigoPrestamo();
					poolPorClaveDedup.merge(clave, pool, Math::max);
				} else {
					poolsSinClaveDedup.add(pool);
				}
			}
		}
		for (double pool : poolPorClaveDedup.values()) {
			totalPozos += pool;
		}
		for (double pool : poolsSinClaveDedup) {
			totalPozos += pool;
		}

		// ⛔ CORREGIDO 2026-09-03 (§10, QUINTA vuelta) — ESTE BLOQUE SE ELIMINÓ, y el porqué
		// importa para que nadie lo reponga: el supuesto sobre el que se escribió ("los 4 tipos
		// ESTRUCTURALES no tienen fila NovedadParticipeCarga") es FALSO. `almacenaRegistros`
		// (Fase 1, ~línea 491) YA crea una fila NVPC real para CUALQUIER tipo estructural que
		// bloquee (1-4), con la MISMA condición (`tipoEstructuralBloquea`) que este bloque volvía
		// a evaluar acá — esa fila entra al bucle genérico de arriba como cualquier otra novedad
		// bloqueante (los 4 tipos están en `FamiliaNovedadCarga.TIPOS_QUE_EXIGEN_AFECTACION`) y
		// YA aporta su pozo. Este bloque volvía a sumar el mismo dinero una segunda vez.
		// Confirmado en producción con CABRERA (rol 2581, carga 449): novedad NVPC 44120, tipo 4,
		// montoRecibido 323,41 — con este bloque puesto, su pozo salía en 646,82 (el doble).
		// Verificado que tipos 1-3 NO tienen este problema (`esTipo4Bloqueante` solo compara
		// contra CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE) pero SÍ están expuestos al mismo supuesto
		// falso en el resto de este javadoc — corregido ahí también.
		//
		// SIN cap acá — CORRECCIÓN 2026-09-03 (§10, CUARTA vuelta): el pozo de una novedad
		// puede abarcar MÁS de una fila (el HS no tiene novedad propia, viaja adentro de la del
		// préstamo — ver aplicarPagoParticipe:~2798, donde montoHS se suma al monto que valida
		// la novedad del PH/PP). Capear ACÁ, por fila, truncaba ese pozo combinado al
		// totalDescontado de la fila del préstamo solo, perdiendo el HS siempre — medido en
		// producción: SANCHEZ (rol 7508) daba 282,77 en vez de 298,19, y los 35 partícipes
		// bloqueados tenían TODOS hipotecario o prendario con seguro. El cap correcto es al
		// total descontado del PARTICIPANTE (todas sus filas en la carga), y vive en los
		// llamadores, que son quienes conocen esa suma — ver
		// {@link #calcularTopeAfectacionManual} y {@link #obtenerTopeAfectacionManual}.
		return totalPozos;
	}

	/** Cédula del partícipe a partir de su rol Petro, solo para el mensaje de la violación. */
	private String cedulaPorRolPetro(Long rolPetro) throws Throwable {
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(rolPetro);
		if (entidades == null || entidades.isEmpty() || entidades.get(0).getNumeroIdentificacion() == null) {
			return "?";
		}
		return entidades.get(0).getNumeroIdentificacion();
	}

	/**
	 * Códigos AVPC que {@link #totalAfectadoManualmente} efectivamente contó, para poder
	 * señalarlos en el mensaje de la violación (§4 del plan: "sin eso hay que escribir un SELECT
	 * para saber a quién le pasó"). Misma condición de conteo, duplicada a propósito en vez de
	 * hacer que el método existente devuelva dos cosas — solo se invoca para los partícipes que
	 * ya violaron el tope, un puñado por carga.
	 */
	private List<Long> codigosAfectacionManual(List<NovedadParticipeCarga> novedades) throws Throwable {
		List<Long> codigos = new ArrayList<>();
		if (novedades == null) {
			return codigos;
		}
		for (NovedadParticipeCarga novedad : novedades) {
			List<AfectacionValoresParticipeCarga> afectaciones =
				afectacionValoresParticipeCargaDaoService.selectByNovedad(novedad.getCodigo());
			if (afectaciones == null) {
				continue;
			}
			for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
				if (afectacion.getTipoAporte() != null || afectacion.getDetallePrestamo() != null) {
					codigos.add(afectacion.getCodigo());
				}
			}
		}
		return codigos;
	}

	/**
	 * ⛔ NO CONECTADA (2026-09-02) — ver el comentario en la llamada comentada, dentro de
	 * {@code aplicarPagosArchivoPetro}, para el porqué (contradice a
	 * {@code validarValoresConDestino} y al diseño de la aplicación manual total; medido:
	 * ~78 falsos positivos abortando la carga 449 completa). El método queda intacto, sin
	 * usar.
	 *
	 * Corta el procesamiento si alguna novedad CON EXCEDENTE no está repartida exacto entre
	 * préstamo(s) y/o aporte(s) — PLAN-EXCEDENTE-PETRO-A-APORTES.md §5, decisión del usuario
	 * 2026-08-30: "ni más ni menos, o si no no hay cómo procesar".
	 *
	 * ⚠️ Tolerancia $0.01, NO la {@code TOLERANCIA} de $1 de este archivo (esa es para
	 * redondeos de "sin destino"; esta es la regla de cuadre exacto del reparto, ya cerrada
	 * con el usuario). La regla en sí vive en
	 * {@code AfectacionValoresParticipeCargaService.diferenciaReparto} — un solo lugar, lo
	 * llama también {@code AfectacionValoresParticipeCargaRest.postBatch} para avisarle al
	 * operador al guardar. Si se escribiera acá de nuevo, el día que cambie la tolerancia
	 * divergirían y el proceso aceptaría lo que la pantalla rechaza.
	 */
	@SuppressWarnings("unused")
	private void validarRepartoDeExcedentes(CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("=== VALIDANDO REPARTO DE EXCEDENTES (100%, tolerancia $0.01) ===");

		List<Map<String, Object>> descuadrados = buscarExcedentesDescuadrados(cargaArchivo);

		if (descuadrados.isEmpty()) {
			System.out.println("✅ Todos los excedentes con reparto registrado cuadran exacto");
			return;
		}

		StringBuilder mensaje = new StringBuilder();
		mensaje.append("No se puede procesar el archivo: hay ").append(descuadrados.size())
		       .append(" novedad(es) con excedente cuyo reparto no cuadra exacto. ")
		       .append("Corrija el reparto en la pantalla de novedades y vuelva a procesar.");

		int listados = 0;
		for (Map<String, Object> item : descuadrados) {
			if (listados >= MAXIMO_DETALLES_EN_MENSAJE) {
				mensaje.append("\n  ... y ").append(descuadrados.size() - listados).append(" más.");
				break;
			}
			double diferencia = (Double) item.get("diferencia");
			mensaje.append("\n  - Rol ").append(item.get("codigoPetro")).append(" ")
			       .append(item.get("nombre")).append(" (novedad ").append(item.get("idNovedad"))
			       .append("): ").append(diferencia > 0 ? "falta repartir $" : "se repartió de más $")
			       .append(String.format("%,.2f", Math.abs(diferencia))).append(".");
			listados++;
		}

		System.err.println(mensaje.toString());
		throw new IncomeException(mensaje.toString());
	}

	/**
	 * Corta el procesamiento si el reparto manual (AVPC) de una novedad deja un HUECO: una
	 * cuota más nueva con valor asignado mientras una cuota más antigua y pendiente del MISMO
	 * préstamo se quedó sin cubrir (2026-08-31, pedido del usuario: "igual que en cobros
	 * personales", donde el pago cascadea de la cuota pendiente más antigua a la más nueva).
	 *
	 * <p>El frontend ya ordena y bloquea esto en pantalla, pero AVPC también se escribe por API
	 * ({@code AfectacionValoresParticipeCargaRest#postBatch}) — un cliente puede mandar
	 * cualquier orden, así que el servidor no puede confiar en que el reparto que llegó ya
	 * viene en prelación. "Cobros personales" no tiene una función de validación para reusar:
	 * su cascada ({@code MotorPagoPrestamoServiceImpl}, sobre {@code
	 * DetallePrestamoDaoService#selectCuotasPendientesByPrestamoOrdenadas}) es estructuralmente
	 * incapaz de dejar un hueco, porque el operador nunca elige qué cuota paga — el algoritmo
	 * decide. Acá SÍ hay una elección explícita (AVPC), así que hace falta validarla — pero
	 * usando la MISMA fuente de "orden correcto" que esa cascada, no un criterio propio.</p>
	 */
	@SuppressWarnings("unused")
	private void validarPrelacionReparto(CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("=== VALIDANDO PRELACIÓN DEL REPARTO (cuota más antigua primero) ===");

		List<Map<String, Object>> conHueco = buscarHuecosDePrelacion(cargaArchivo);

		if (conHueco.isEmpty()) {
			System.out.println("✅ Ningún reparto manual dejó una cuota más antigua sin cubrir");
			return;
		}

		StringBuilder mensaje = new StringBuilder();
		mensaje.append("No se puede procesar el archivo: hay ").append(conHueco.size())
		       .append(" préstamo(s) cuyo reparto manual (entre todas sus novedades) cubre una")
		       .append(" cuota sin cubrir antes una anterior del mismo préstamo, todavía")
		       .append(" pendiente. Complete el reparto de la más antigua a la más nueva y vuelva")
		       .append(" a procesar.");

		int listados = 0;
		for (Map<String, Object> item : conHueco) {
			if (listados >= MAXIMO_DETALLES_EN_MENSAJE) {
				mensaje.append("\n  ... y ").append(conHueco.size() - listados).append(" más.");
				break;
			}
			mensaje.append("\n  - Rol ").append(item.get("codigoPetro")).append(" ")
			       .append(item.get("nombre"))
			       .append(" (préstamo ").append(item.get("idPrestamo")).append("): la cuota #")
			       .append(item.get("numeroCuotaConHueco")).append(" tiene valor asignado, pero la")
			       .append(" cuota #").append(item.get("numeroCuotaSinCubrir"))
			       .append(" (más antigua, pendiente) no.");
			listados++;
		}

		System.err.println(mensaje.toString());
		throw new IncomeException(mensaje.toString());
	}

	/**
	 * Recorre la carga y arma la lista de (partícipe, préstamo) cuyo reparto AVPC dejó una
	 * cuota antigua pendiente sin cubrir mientras cubrió una más nueva.
	 *
	 * <p><b>2026-08-31, rediseño (corrige el primero de los dos falsos positivos que hizo
	 * desconectar la versión anterior):</b> agrupa las filas de AVPC de TODAS las novedades del
	 * partícipe, no las de una novedad sola — antes, un partícipe con dos novedades sobre el
	 * MISMO préstamo (A cubre la cuota 1, B cubre la 3) marcaba un hueco al validar B sola,
	 * aunque A ya hubiera cubierto la cuota 1. Agrupar primero por partícipe y recién ahí por
	 * préstamo ve las dos novedades juntas.</p>
	 *
	 * <p><b>⚠️ EL SEGUNDO FALSO POSITIVO SIGUE SIN RESOLVERSE, a propósito — no se aproximó.</b>
	 * Esta comparación sigue siendo contra {@code selectCuotasPendientesByPrestamoOrdenadas}
	 * ANTES de que el flujo automático (paso 2 de {@code aplicarPagosArchivoPetro}) aplique
	 * nada — las cuotas que el descuento ordinario va a cubrir todavía figuran "pendientes" en
	 * este momento, así que pueden salir como huecos aunque el flujo automático las vaya a
	 * cubrir un instante después. Resolverlo bien exige, o (a) reordenar
	 * {@code aplicarPagosArchivoPetro} para validar DESPUÉS de la aplicación automática (rompe
	 * el patrón "todo o nada antes de tocar la primera cuota" que protege el resto del método),
	 * o (b) simular acá el matching de {@code buscarCuotaAPagar}/{@code validarPrestamo} sin
	 * aplicar nada — duplicar esa lógica es exactamente el riesgo de divergencia que este
	 * proyecto viene evitando todo el día. Ninguna de las dos es un cambio chico, así que
	 * NO SE CONECTA esta validación aunque el primer falso positivo ya esté arreglado — sigue
	 * desconectada en {@code aplicarPagosArchivoPetro} hasta que se decida cuál de las dos
	 * hacer.</p>
	 */
	private List<Map<String, Object>> buscarHuecosDePrelacion(CargaArchivo cargaArchivo) throws Throwable {
		List<Map<String, Object>> conHueco = new ArrayList<>();

		List<DetalleCargaArchivo> detallesCarga =
			detalleCargaArchivoDaoService.selectByCargaArchivo(cargaArchivo.getCodigo());
		if (detallesCarga == null) {
			return conHueco;
		}

		for (DetalleCargaArchivo detalle : detallesCarga) {
			List<ParticipeXCargaArchivo> participesDetalle =
				participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
			if (participesDetalle == null) {
				continue;
			}

			for (ParticipeXCargaArchivo participe : participesDetalle) {
				List<NovedadParticipeCarga> novedades =
					novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
				if (novedades == null || novedades.isEmpty()) {
					continue;
				}

				// TODAS las filas de AVPC de TODAS las novedades del partícipe, juntas —
				// nunca una novedad a la vez (ver el javadoc, primer falso positivo).
				List<AfectacionValoresParticipeCarga> afectacionesParticipe = new ArrayList<>();
				for (NovedadParticipeCarga novedad : novedades) {
					List<AfectacionValoresParticipeCarga> afectacionesNovedad =
						afectacionValoresParticipeCargaDaoService.selectByNovedad(novedad.getCodigo());
					if (afectacionesNovedad != null) {
						afectacionesParticipe.addAll(afectacionesNovedad);
					}
				}
				if (afectacionesParticipe.isEmpty()) {
					continue;
				}

				// Solo filas de CUOTA (prestamo + detallePrestamo) — las de aporte
				// (tipoAporte, CK_AVPC_PRST_XOR_TPAP) no tienen cuota que ordenar. Agrupadas
				// por préstamo: la prelación se evalúa dentro de cada préstamo, nunca entre
				// préstamos distintos.
				Map<Long, Set<Long>> cuotasCubiertasPorPrestamo = new LinkedHashMap<>();
				for (AfectacionValoresParticipeCarga afectacion : afectacionesParticipe) {
					if (afectacion.getPrestamo() == null || afectacion.getDetallePrestamo() == null) {
						continue;
					}
					Long idPrestamo = afectacion.getPrestamo().getCodigo();
					cuotasCubiertasPorPrestamo.computeIfAbsent(idPrestamo, k -> new HashSet<>())
							.add(afectacion.getDetallePrestamo().getCodigo());
				}

				for (Map.Entry<Long, Set<Long>> entrada : cuotasCubiertasPorPrestamo.entrySet()) {
					Long idPrestamo = entrada.getKey();
					Set<Long> codigosCubiertos = entrada.getValue();

					List<DetallePrestamo> pendientesOrdenadas =
						detallePrestamoDaoService.selectCuotasPendientesByPrestamoOrdenadas(idPrestamo);
					if (pendientesOrdenadas == null) {
						continue;
					}

					// Última posición (más nueva) que el reparto cubrió; si alguna posición
					// ANTERIOR a esa (más antigua, todavía pendiente) no está cubierta, es
					// el hueco que describe el pedido del usuario. Sigue sin descontar lo que
					// el flujo automático va a cubrir — ver el ⚠️ del javadoc.
					int ultimaPosicionCubierta = -1;
					for (int i = 0; i < pendientesOrdenadas.size(); i++) {
						if (codigosCubiertos.contains(pendientesOrdenadas.get(i).getCodigo())) {
							ultimaPosicionCubierta = i;
						}
					}
					for (int i = 0; i < ultimaPosicionCubierta; i++) {
						DetallePrestamo cuotaSinCubrir = pendientesOrdenadas.get(i);
						if (!codigosCubiertos.contains(cuotaSinCubrir.getCodigo())) {
							Map<String, Object> item = new HashMap<>();
							item.put("codigoPetro", participe.getCodigoPetro());
							item.put("nombre", participe.getNombre());
							item.put("idPrestamo", idPrestamo);
							item.put("numeroCuotaSinCubrir", cuotaSinCubrir.getNumeroCuota());
							item.put("numeroCuotaConHueco",
									pendientesOrdenadas.get(ultimaPosicionCubierta).getNumeroCuota());
							conHueco.add(item);

							System.out.println("⛔ Hueco de prelación - Rol " + participe.getCodigoPetro()
								+ " (" + participe.getNombre() + ")"
								+ " - préstamo " + idPrestamo + " - cuota #"
								+ cuotaSinCubrir.getNumeroCuota() + " pendiente sin cubrir, cuota #"
								+ pendientesOrdenadas.get(ultimaPosicionCubierta).getNumeroCuota()
								+ " (más nueva) sí tiene valor asignado.");
							break; // una fila por (partícipe, préstamo) alcanza para el mensaje
						}
					}
				}
			}
		}

		return conHueco;
	}

	/** Recorre la carga y arma la lista de novedades con excedente cuyo reparto no cuadra. */
	private List<Map<String, Object>> buscarExcedentesDescuadrados(CargaArchivo cargaArchivo) throws Throwable {
		List<Map<String, Object>> descuadrados = new ArrayList<>();

		List<DetalleCargaArchivo> detallesCarga =
			detalleCargaArchivoDaoService.selectByCargaArchivo(cargaArchivo.getCodigo());
		if (detallesCarga == null) {
			return descuadrados;
		}

		for (DetalleCargaArchivo detalle : detallesCarga) {
			List<ParticipeXCargaArchivo> participesDetalle =
				participeXCargaArchivoDaoService.selectByDetalleCargaArchivo(detalle.getCodigo());
			if (participesDetalle == null) {
				continue;
			}

			for (ParticipeXCargaArchivo participe : participesDetalle) {
				List<NovedadParticipeCarga> novedades =
					novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
				if (novedades == null) {
					continue;
				}

				for (NovedadParticipeCarga novedad : novedades) {
					double diferencia = afectacionValoresParticipeCargaService.diferenciaReparto(novedad.getCodigo());
					if (Math.abs(diferencia) > 0.01) {
						Map<String, Object> item = new HashMap<>();
						item.put("codigoPetro", participe.getCodigoPetro());
						item.put("nombre", participe.getNombre());
						item.put("idNovedad", novedad.getCodigo());
						item.put("diferencia", diferencia);
						descuadrados.add(item);

						System.out.println("⛔ Reparto descuadrado - Rol " + participe.getCodigoPetro()
							+ " (" + participe.getNombre() + ") - Novedad " + novedad.getCodigo()
							+ " - " + (diferencia > 0 ? "falta $" : "sobra $")
							+ String.format("%,.2f", Math.abs(diferencia)));
					}
				}
			}
		}

		return descuadrados;
	}

	/**
	 * Tipos de novedad del partícipe que impiden determinar el destino del valor.
	 *
	 * Se miran las dos fuentes: los campos de novedad del propio registro
	 * (novedadesCarga / novedadesFinancieras, que es lo que ve el usuario en la
	 * grilla) y las filas de NVPC, que es el detalle sobre el que se registran
	 * las afectaciones manuales.
	 *
	 * @param codigoFilial Filial de la carga (2026-09-02) — decide si
	 *                     CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE bloquea. Ver
	 *                     {@link #tipoEstructuralBloquea}.
	 */
	private List<Long> novedadesQueRequierenAfectacion(ParticipeXCargaArchivo participe,
			List<NovedadParticipeCarga> novedades, Long codigoFilial) {

		List<Long> encontradas = new ArrayList<>();

		// participe.getNovedadesCarga()/getNovedadesFinancieras() son campos planos (Fase 1
		// estructural) que NUNCA cargan un tipo con monto (verificado 2026-08-31: solo los 4
		// estructurales + OK/VALORES_CERO/SIN_DESCUENTOS/DESCUENTOS_INCOMPLETOS) — sin fila
		// NVPC no hay montoDiferencia que consultar, así que van con null. Los 3 estructurales
		// "no sé quién es" (PARTICIPE_NO_ENCONTRADO, CODIGO_ROL_DUPLICADO,
		// NOMBRE_ENTIDAD_DUPLICADO) siguen bloqueando siempre. El cuarto,
		// CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE, tiene la excepción de filial — ver
		// tipoEstructuralBloquea.
		if (tipoEstructuralBloquea(participe.getNovedadesCarga(), codigoFilial)) {
			encontradas.add(participe.getNovedadesCarga());
		}
		if (tipoEstructuralBloquea(participe.getNovedadesFinancieras(), codigoFilial)
				&& !encontradas.contains(participe.getNovedadesFinancieras())) {
			encontradas.add(participe.getNovedadesFinancieras());
		}

		if (novedades != null) {
			for (NovedadParticipeCarga novedad : novedades) {
				agregarSiRequiereAfectacion(encontradas, novedad.getTipoNovedad(), novedad.getMontoDiferencia());
			}
		}

		return encontradas;
	}

	/**
	 * Bloquea solo cuando {@link FamiliaNovedadCarga#clasificar} da BLOQUEANTE (2026-08-31,
	 * antes miraba solo el tipo — ver el javadoc de {@link #novedadesQueRequierenAfectacion}).
	 * COBRANZA e INFORMATIVA no entran a {@code acumulador}: el proceso automático sabe qué
	 * hacer con ese valor (COBRANZA = ya tiene destino, solo falta cobrarlo).
	 */
	private void agregarSiRequiereAfectacion(List<Long> acumulador, Long tipoNovedad, Double montoDiferencia) {
		if (tipoNovedad != null
				&& FamiliaNovedadCarga.clasificar(tipoNovedad, montoDiferencia) == FamiliaNovedadCarga.BLOQUEANTE
				&& !acumulador.contains(tipoNovedad)) {
			acumulador.add(tipoNovedad);
		}
	}

	/**
	 * Monto a usar para una novedad BLOQUEANTE sobre una fila PH/PP, sumando el HS del mismo
	 * partícipe SI corresponde — VALIDACION-TOPE-AFECTACION-MANUAL.md §16, decisión del árbitro
	 * 2026-09-03: <b>el HS es UN solo importe y nunca se le suma a dos filas.</b>
	 *
	 * <p>Regla de desempate, para el caso (no descartado, sin datos que lo confirmen o lo nieguen)
	 * de un partícipe con novedad candidata en PH Y en PP a la vez: <b>PH se lo lleva siempre,
	 * PP nunca</b> — mismo orden que {@link #resolverProductosPorCodigo} y el resto del archivo
	 * usan para PH/PP. Si además existe la fila PP, se deja una línea de log EXPLÍCITA
	 * (decisión del árbitro: "un desempate silencioso sobre un supuesto no verificado es justo
	 * lo que después nadie puede reconstruir") — así que si el caso alguna vez ocurre, es
	 * visible y revisable, no un promedio o una resta silenciosa.</p>
	 *
	 * @param participe      La fila PXCA de la novedad (PH, PP, o cualquier otro producto — para
	 *                       cualquier producto que no sea PH/PP esto es un no-op, devuelve
	 *                       {@code totalDescontado} tal cual)
	 * @param codigoProducto {@code participe.getDetalleCargaArchivo().getCodigoPetroProducto()}
	 * @param cargaArchivo   La carga, para acotar la búsqueda del HS a la MISMA carga
	 * @return {@code totalDescontado} de la fila, más el {@code totalDescontado} del HS del
	 *         mismo partícipe en esta carga si existe y le corresponde a esta fila
	 */
	private double montoConHsMergeado(ParticipeXCargaArchivo participe, String codigoProducto,
			CargaArchivo cargaArchivo) throws Throwable {
		double monto = nullSafe(participe.getTotalDescontado());
		boolean esPH = CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto);
		boolean esPP = CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto);
		if (!esPH && !esPP) {
			return monto;
		}
		if (esPP) {
			ParticipeXCargaArchivo filaPH = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
				participe.getCodigoPetro(), CODIGO_PRODUCTO_PH, cargaArchivo.getCodigo());
			if (filaPH != null) {
				// PH existe: el HS se lo lleva PH (ver el log del lado PH), nunca PP.
				return monto;
			}
		}
		ParticipeXCargaArchivo filaHS = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
			participe.getCodigoPetro(), CODIGO_PRODUCTO_HS, cargaArchivo.getCodigo());
		if (filaHS == null) {
			return monto;
		}
		if (esPH) {
			ParticipeXCargaArchivo filaPP = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
				participe.getCodigoPetro(), CODIGO_PRODUCTO_PP, cargaArchivo.getCodigo());
			if (filaPP != null) {
				System.out.println("⚠️ CASO AMBIGUO (§16): partícipe " + participe.getCodigoPetro() + " ("
					+ participe.getNombre() + ") tiene fila PH Y PP en la carga " + cargaArchivo.getCodigo()
					+ " — el HS ($" + nullSafe(filaHS.getTotalDescontado()) + ") se asigna a PH (fila "
					+ participe.getCodigo() + "), NUNCA a PP (fila " + filaPP.getCodigo()
					+ "). Revisar si esto es correcto para este partícipe.");
			}
		}
		return monto + nullSafe(filaHS.getTotalDescontado());
	}

	/**
	 * Si un tipo de novedad ESTRUCTURAL (participe.getNovedadesCarga()/getNovedadesFinancieras(),
	 * sin fila NVPC ni montoDiferencia) bloquea el procesamiento — la MISMA pregunta que
	 * {@link #agregarSiRequiereAfectacion} resuelve para las novedades con fila NVPC, así que
	 * NO es una lista aparte: se apoya en {@link FamiliaNovedadCarga#clasificar}, con una sola
	 * excepción.
	 *
	 * <p><b>CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE (4) NO bloquea en filial Petrocomercial</b>
	 * (decisión del usuario, 2026-09-02, tras el callejón sin salida de la carga 449): ahí el
	 * partícipe ya quedó identificado por rol Petro ANTES de comparar el nombre
	 * ({@code entidades.get(0)} en la identificación de fase 1), y toda la aplicación (fase 2,
	 * {@code validarNovedadesFase2}/{@code aplicarPagoParticipe}) resuelve la entidad por
	 * código Petro, nunca por nombre — la novedad queda como aviso de calidad de dato, no como
	 * bloqueo.</p>
	 *
	 * <p><b>En cualquier otra filial, el tipo 4 SIGUE bloqueando</b>: hoy la carga identifica
	 * por código Petro también para esas filiales (verificado 2026-09-02: no hay ninguna rama
	 * por filial en la identificación de fase 1, ni existe todavía identificación por número
	 * de identificación en este flujo — ver {@code EntidadDaoService.selectByNumeroIdentificacion},
	 * usado hoy solo por procesos de reportes en {@code rpr}). Hasta que eso exista, ahí no hay
	 * con qué confiar más que con el rol Petro, así que la novedad sigue exigiendo afectación
	 * manual.</p>
	 *
	 * @param tipoNovedad  {@code participe.getNovedadesCarga()} o {@code getNovedadesFinancieras()}
	 * @param codigoFilial {@code CargaArchivo.filial.codigo}; null cuenta como Petrocomercial
	 *                     (mismo criterio que {@link #esFilialPetrocomercial})
	 */
	private boolean tipoEstructuralBloquea(Long tipoNovedad, Long codigoFilial) {
		if (tipoNovedad == null) {
			return false;
		}
		if (tipoNovedad.intValue() == ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE
				&& esFilialPetrocomercial(codigoFilial)) {
			return false;
		}
		return FamiliaNovedadCarga.clasificar(tipoNovedad, null) == FamiliaNovedadCarga.BLOQUEANTE;
	}

	/**
	 * Mismo criterio que {@code GeneracionArchivoPetroServiceImpl.esFilialPetrocomercial}
	 * (:990, crd) — copiado a propósito, no reusado: ese método es privado y vive en el lado
	 * de GENERACIÓN (saliente), este es el de CARGA (entrante); son módulos distintos
	 * (`crd`/`asoprep`) y este archivo no puede depender de una clase de otro equipo/alcance.
	 * {@code null} cuenta como Petrocomercial en los dos lados — mismo dato
	 * ({@code CargaArchivo.filial} / {@code Filiales.PETROCOMERCIAL}), mismo default.
	 */
	private boolean esFilialPetrocomercial(Long codigoFilial) {
		return codigoFilial == null || codigoFilial.longValue() == Filiales.PETROCOMERCIAL;
	}

	/**
	 * {@code productoDaoService.selectAllByCodigoPetro} memoizado (2026-09-02): dentro de una
	 * carga, {@code codigoProducto} toma un puñado de valores distintos (PH, PP, PQ, AH...) que
	 * se repiten una vez por partícipe — la MISMA consulta con los MISMOS parámetros, cientos o
	 * miles de veces (auditoría de rendimiento pedida por el usuario). El resultado no cambia
	 * entre llamadas dentro de una misma carga: la consulta es de solo lectura contra
	 * {@code CRD.PRDC} y nada de este proceso modifica productos mientras corre.
	 *
	 * <p>{@code cache} es SIEMPRE una variable local del método que orquesta el bucle de
	 * partícipes ({@code ejecutarValidacionesFase2}, {@code aplicarPagosArchivoPetro}) — nunca
	 * un campo de instancia ni estático: este bean es {@code @Stateful}, y un campo así sería
	 * estado compartido entre invocaciones (o cargas) distintas, un bug de datos mucho peor que
	 * la lentitud que esto arregla.</p>
	 */
	private List<Producto> resolverProductosPorCodigo(String codigoProducto,
			Map<String, List<Producto>> cache) throws Throwable {
		if (cache.containsKey(codigoProducto)) {
			return cache.get(codigoProducto);
		}
		List<Producto> productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
		cache.put(codigoProducto, productos);
		return productos;
	}

	private static final Pattern DIACRITICOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	private static final Pattern ESPACIOS_MULTIPLES = Pattern.compile("\\s+");

	/**
	 * Normaliza un nombre para comparar el maestro de entidades (CRD.ENTD.ENTDRZNS) contra el
	 * nombre que trae el archivo Petro (2026-09-02, decisión del usuario, medido en la carga
	 * 449): Petrocomercial manda los nombres SIN tildes y el sistema los guarda CON tildes, así
	 * que chocaban todos los meses con partícipes distintos cada vez — MADRONERO/MADROÑERO,
	 * HECTOR/HÉCTOR, GONZALES/GONZALEZ, BUNAY/BUÑAY, EDISON/EDINSON, y casos con doble espacio
	 * como "BORBOR MALAVE WELLINGTON  ANTO".
	 *
	 * <p>No es una comparación difusa ni por similitud: sigue siendo exacta
	 * ({@code equalsIgnoreCase}) sobre el texto ya normalizado — si después de esto los
	 * nombres siguen siendo distintos, la novedad se genera igual, que es lo correcto.</p>
	 *
	 * @param nombre Texto a normalizar. NO trunca — el truncado a 35 caracteres del archivo
	 *               Petro se aplica ANTES de llamar a este método, sigue igual que antes.
	 * @return       Mayúsculas, sin diacríticos, Ñ/ñ → N, espacios múltiples colapsados a uno.
	 */
	private String normalizarNombreParaComparar(String nombre) {
		if (nombre == null) {
			return "";
		}
		String resultado = nombre.trim().toUpperCase();
		// Ñ -> N explícito: a diferencia de las vocales acentuadas, la Ñ no se descompone con
		// NFD en todas las configuraciones — no confiar en que el paso de abajo la cubra.
		resultado = resultado.replace('Ñ', 'N');
		resultado = Normalizer.normalize(resultado, Normalizer.Form.NFD);
		resultado = DIACRITICOS.matcher(resultado).replaceAll("");
		resultado = ESPACIOS_MULTIPLES.matcher(resultado).replaceAll(" ");
		return resultado.trim();
	}

	/**
	 * Suma el valor que el usuario dejó indicado en las afectaciones manuales.
	 *
	 * Solo cuentan las afectaciones que el aplicador va a usar realmente: una fila sin cuota
	 * Y sin tipo de aporte (rota, ninguno de los dos lados del XOR) no tiene a dónde
	 * aplicarse y no suma — igual que en {@link #verificarYAplicarAfectacionesManualesTotales},
	 * donde esa combinación aborta la carga en vez de omitirse en silencio.
	 *
	 * ⚠️ Actualizado 2026-08-31 (excedente a aporte): antes esta suma SOLO contaba filas con
	 * cuota — una novedad repartida enteramente a un aporte quedaba invisible acá y
	 * {@code validarValoresConDestino} la reportaba como "sin destino" aunque estuviera
	 * completamente cubierta. Las filas de aporte no tienen desglose capital/interés/
	 * desgravamen (DDL de {@code CRD.AVPC.TPAPCDGO}: "un aporte no tiene capital ni interés:
	 * solo AVPCVAFA") — solo cuenta {@code valorAfectar} directo, sin reconstrucción.
	 */
	private double totalAfectadoManualmente(List<NovedadParticipeCarga> novedades) throws Throwable {
		double total = 0.0;

		if (novedades == null) {
			return total;
		}

		for (NovedadParticipeCarga novedad : novedades) {
			List<AfectacionValoresParticipeCarga> afectaciones =
				afectacionValoresParticipeCargaDaoService.selectByNovedad(novedad.getCodigo());

			if (afectaciones == null) {
				continue;
			}

			for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
				if (afectacion.getTipoAporte() != null) {
					total += nullSafe(afectacion.getValorAfectar());
					continue;
				}

				if (afectacion.getDetallePrestamo() == null) {
					continue;
				}

				double valor = nullSafe(afectacion.getValorAfectar());
				if (valor <= 0.0) {
					// Sin total explícito, se reconstruye desde el desglose.
					valor = nullSafe(afectacion.getCapitalAfectar())
						  + nullSafe(afectacion.getInteresAfectar())
						  + nullSafe(afectacion.getDesgravamenAfectar());
				}

				total += valor;
			}
		}

		return total;
	}

	/** Nombres legibles de las novedades, para el mensaje que ve el usuario. */
	private String describirNovedades(List<Long> codigosNovedad) {
		List<String> nombres = new ArrayList<>();

		for (Long codigo : codigosNovedad) {
			int tipo = codigo.intValue();
			switch (tipo) {
				case ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO:
					nombres.add("PARTÍCIPE NO ENCONTRADO"); break;
				case ASPNovedadesCargaArchivo.CODIGO_ROL_DUPLICADO:
					nombres.add("CÓDIGO DE ROL DUPLICADO"); break;
				case ASPNovedadesCargaArchivo.NOMBRE_ENTIDAD_DUPLICADO:
					nombres.add("NOMBRE DE ENTIDAD DUPLICADO"); break;
				case ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE:
					nombres.add("CÓDIGO PETRO NO COINCIDE CON EL NOMBRE"); break;
				case ASPNovedadesCargaArchivo.DESCUENTOS_ADICIONALES:
					nombres.add("DESCUENTOS ADICIONALES"); break;
				case ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO:
					nombres.add("PRODUCTO NO MAPEADO"); break;
				case ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO:
					nombres.add("PRÉSTAMO NO ENCONTRADO"); break;
				case ASPNovedadesCargaArchivo.MULTIPLES_PRESTAMOS_ACTIVOS:
					nombres.add("MÚLTIPLES PRÉSTAMOS ACTIVOS"); break;
				case ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA:
					nombres.add("CUOTA NO ENCONTRADA"); break;
				case ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE:
					nombres.add("MONTO INCONSISTENTE"); break;
				case ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO:
					nombres.add("HISTORIAL DE SUELDO NO ENCONTRADO"); break;
				case ASPNovedadesCargaArchivo.MULTIPLES_REGISTROS_HISTORIAL_SUELDO:
					nombres.add("MÚLTIPLES REGISTROS DE HISTORIAL DE SUELDO"); break;
				case ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS:
					nombres.add("VALORES DEL HISTORIAL DE SUELDO NULOS"); break;
				case ASPNovedadesCargaArchivo.APORTE_MONTO_INCONSISTENTE:
					nombres.add("MONTO DE APORTE INCONSISTENTE"); break;
				default:
					nombres.add("NOVEDAD " + tipo); break;
			}
		}

		return String.join(", ", nombres);
	}


	/**
	 * Aplica el pago de un partícipe individual
	 * REGLA ESPECIAL: Para PH (Préstamo Hipotecario) y PP (Préstamo Prendario), 
	 * el seguro DEBE venir en un registro separado con código HS.
	 * Si no se encuentra HS o no corresponde, la cuota queda como PARCIAL.
	 */
	private void aplicarPagoParticipe(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo,
			Map<String, List<Producto>> productosCache) throws Throwable {
		System.out.println("========================================");
		System.out.println("APLICAR PAGO " + codigoProducto + " - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
		System.out.println("Monto: $" + participe.getTotalDescontado());
		
		// ✅ OPTIMIZACIÓN CRÍTICA: Si el monto es 0, marcar cuotas en mora INMEDIATAMENTE
		// Sin hacer búsquedas innecesarias de entidad, productos, préstamos o seguro HS
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
			System.out.println("⚠️ Monto descontado es $0 - No se realizó pago - Marcando cuotas en MORA");
			marcarCuotasEnMoraPorFaltaDePago(participe, codigoProducto, cargaArchivo, productosCache);
			// MODO REPROCESO (§4.12): si esta fila PH/PP es la que lleva el HS del partícipe, ese HS
			// no lo aplica nadie (el HS huérfano se salta porque existe la fila PH/PP): se nombra.
			if (modoReproceso) {
				registrarFilaSinPagoReproceso(participe, CODIGO_PRODUCTO_HS, hsDeLaFila(participe, codigoProducto, cargaArchivo),
					"HS con fila " + codigoProducto + " sin descuento");
			}
			return;
		}
		
		// ========================================
		// ✅ PRIORIDAD MÁXIMA: Verificar si existen registros de AfectacionValoresParticipeCarga
		// Si existen, aplicar SOLO esos registros y salir inmediatamente
		// ========================================
		System.out.println("🔍 Verificando existencia de registros AfectacionValoresParticipeCarga...");
		boolean tieneAfectacionManual = verificarYAplicarAfectacionesManualesTotales(participe, codigoProducto, cargaArchivo);
		if (tieneAfectacionManual) {
			System.out.println("✅ Se aplicaron afectaciones manuales - Proceso finalizado para este partícipe");
			System.out.println("========================================");
			return;
		}
		System.out.println("   No se encontraron afectaciones manuales - Continuando con proceso normal");
		
		// ✅ OPTIMIZACIÓN: Buscar entidad UNA SOLA VEZ
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
		if (entidades == null || entidades.isEmpty()) {
			System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontró entidad con código Petro: " + participe.getCodigoPetro());
			if (modoReproceso) {
				// MODO REPROCESO (C4): no se omite en silencio — queda como novedad y se
				// descuenta del recibido. El monto incluye el HS del mismo partícipe cuando
				// corresponde a esta fila (montoConHsMergeado), porque ese HS tampoco se va a
				// aplicar por ningún otro camino.
				registrarSinParticipeReproceso(participe, codigoProducto,
					montoConHsMergeado(participe, codigoProducto, cargaArchivo), cargaArchivo);
			}
			return;
		}
		Entidad entidad = entidades.get(0);
		System.out.println("✅ Entidad encontrada: " + entidad.getRazonSocial() + " (ID: " + entidad.getCodigo() + ")");

		// MODO REPROCESO (D-010): el excedente que ASOPREP devolvió va a aporte EXCEDENTE_PETRO y
		// NO entra a la cascada de cuotas siguientes. Se registra ANTES de buscar préstamos: si
		// todo lo descontado fue excedente (p. ej. préstamo ya cancelado), la fila termina acá.
		// Fuera del modo reproceso devuelve 0 y no hace nada.
		double excedenteDevuelto = aplicarExcedenteDevueltoReproceso(participe, codigoProducto, entidad, cargaArchivo);
		montoArchivo -= excedenteDevuelto;
		if (excedenteDevuelto > 0.0 && montoArchivo <= 0.01
				&& !CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto)
				&& !CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
			System.out.println("   D-010: todo lo descontado en la fila era excedente devuelto - nada para el motor");
			return;
		}

		// ✅ OPTIMIZACIÓN: Buscar productos UNA SOLA VEZ (memoizado por carga, ver
		// resolverProductosPorCodigo)
		List<Producto> productos = resolverProductosPorCodigo(codigoProducto, productosCache);
		if (productos == null || productos.isEmpty()) {
			System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontró producto con código Petro: " + codigoProducto);
			registrarFilaSinPagoReproceso(participe, codigoProducto,
				montoArchivo + (modoReproceso ? hsDeLaFila(participe, codigoProducto, cargaArchivo) : 0.0), "producto no mapeado");
			return;
		}
		System.out.println("✅ Producto(s) encontrado(s): " + productos.size());

		// ✅ OPTIMIZACIÓN: Buscar préstamos UNA SOLA VEZ
		List<Prestamo> prestamos = new ArrayList<>();
		for (Producto producto : productos) {
			// MODO REPROCESO (D-019): la operación se busca por su estado A LA FECHA DE CORTE
			// (vigente si su cancelación es posterior al mes de la carga), no por PRSTIDST de hoy;
			// la que hoy está cancelada se reabre y su cancelación queda marcada para re-aplicarla
			// en su propio mes. Fuera del modo reproceso, la búsqueda de siempre.
			List<Prestamo> prestamosDelProducto = modoReproceso
				? prestamosVigentesAlCorteReproceso(entidad, producto, cargaArchivo)
				: prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
			if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
				prestamos.addAll(prestamosDelProducto);
			}
		}

		if (prestamos.isEmpty()) {
			System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontraron préstamos activos para entidad: " +
			                   entidad.getRazonSocial() + " (ID: " + entidad.getCodigo() + ") y producto: " + codigoProducto);
			registrarFilaSinPagoReproceso(participe, codigoProducto,
				montoArchivo + (modoReproceso ? hsDeLaFila(participe, codigoProducto, cargaArchivo) : 0.0),
				"sin operación vigente al corte (entidad " + entidad.getCodigo() + ")");
			return;
		}
		System.out.println("✅ Préstamo(s) activo(s) encontrado(s): " + prestamos.size());

		// Inicializar variables para HS
		double montoHS = 0.0;
		// §4.12 (D-023): el HS se sumó al monto de esta fila por la rama de seguro sin seguro en la cuota.
		boolean hsSinSeguroEnCuotaFila = false;
		
		// REGLA ESPECIAL: Para PH o PP, el seguro DEBE venir en HS
		if (CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
		    CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
			
			System.out.println("📋 Producto con seguro de incendio - Buscando registro HS separado...");

			// TODO O NADA (2026-08-29): antes, si esto fallaba, se tragaba el error y se caía a
			// "se aplica sin seguro, la cuota queda PARCIAL" — un PARCIAL así no es un resultado
			// legítimo (eso es para cuando el dinero no alcanza), es un resultado equivocado
			// producido por un error que se tragó. selectByCodigoPetroYProductoEnCarga ya
			// distingue "no hay registro HS" (devuelve null, rama de abajo) de un fallo real —
			// lo único que puede llegar a un catch acá es un fallo real de consulta.
			try {
				// Buscar la cuota para validar que el monto HS corresponda al valorSeguroIncendio esperado
				// ✅ USAR prestamos ya obtenidos en lugar de buscar de nuevo
				DetallePrestamo cuotaValidar = buscarCuotaAPagar(prestamos, cargaArchivo);
				if (cuotaValidar != null) {
					// CORRECCIÓN: Validar contra valorSeguroIncendio, NO contra desgravamen
					double seguroIncendioEsperado = nullSafe(cuotaValidar.getValorSeguroIncendio());
					if (seguroIncendioEsperado > 0.01) {
						ParticipeXCargaArchivo participeHS = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
							participe.getCodigoPetro(),
							CODIGO_PRODUCTO_HS,
							cargaArchivo.getCodigo()
						);
						
						if (participeHS != null) {
							montoHS = nullSafe(participeHS.getTotalDescontado());
							System.out.println("✅ Registro HS encontrado con monto: $" + montoHS);	
							double diferenciaHS = Math.abs(seguroIncendioEsperado - montoHS);
							
							System.out.println("  Validando HS contra valorSeguroIncendio de la cuota:");
							System.out.println("    - Seguro Incendio esperado (cuota): $" + seguroIncendioEsperado);
							System.out.println("    - Monto HS recibido (archivo): $" + montoHS);
							System.out.println("    - Diferencia: $" + diferenciaHS);
							
							// ✅ SIN TOLERANCIA: Comparación exacta para validar HS
							if (diferenciaHS <= 0.01) {
								System.out.println("✅ Monto HS CORRESPONDE al seguro de incendio esperado");
								montoArchivo += montoHS;
								System.out.println("✅ TOTAL A APLICAR (" + codigoProducto + " + HS): $" + montoArchivo);
							} else {
								montoArchivo += montoHS;
								System.out.println("❌ Monto HS NO CORRESPONDE - Esperado: $" + seguroIncendioEsperado + ", Recibido: $" + montoHS);
								System.out.println("❌ La cuota quedará como PARCIAL pero si se procesaran los valores (solo se aplicará " + codigoProducto + " sin seguro incendio)");
							}
						} else {
							 // ❌ La cuota SÍ requiere seguro pero NO se encontró registro HS
						    System.out.println("❌ ERROR: No se encontró registro HS (la cuota requiere seguro de $" + seguroIncendioEsperado + ")");
						    System.out.println("❌ La cuota quedará como PARCIAL (solo se aplicará " + codigoProducto + " sin seguro)");
						    montoHS = 0.0;
				        }
			        } else {
			            // La cuota NO requiere seguro de incendio (valorSeguroIncendio = 0)
			            System.out.println("✅ No es necesario registro HS, la cuota tiene valorSeguroIncendio = $0");
			            System.out.println("✅ Procesando normalmente sin HS");
			            montoHS = 0.0;
			            // §4.12 (D-023): pero si Petro SÍ descontó HS para esta fila, ese dinero no
			            // puede quedar sin destino (antes se perdía en silencio y lo detectaba el
			            // invariante). En reproceso se aplica como seguro de incendio de esta cuota;
			            // en la carga normal, novedad bloqueante para que el operador lo reparta.
			            double hsFila = hsDeLaFila(participe, codigoProducto, cargaArchivo);
			            if (hsFila > 0.01) {
			                if (modoReproceso) {
			                    prepararSeguroIncendioEnCuotaReproceso(cuotaValidar, hsFila, participe);
			                    montoHS = hsFila;
			                    montoArchivo += hsFila;
			                    hsSinSeguroEnCuotaFila = true;
			                } else {
			                    registrarNovedadDeAplicacion(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE,
			                        "HS de $" + String.format("%.2f", hsFila) + " descontado por Petro, pero la cuota #"
			                            + cuotaValidar.getNumeroCuota() + " del préstamo "
			                            + (cuotaValidar.getPrestamo() != null ? cuotaValidar.getPrestamo().getCodigo() : null)
			                            + " no tiene seguro de incendio: no se aplicó. Requiere distribución manual (AVPC).",
			                        null, cuotaValidar.getPrestamo() != null ? cuotaValidar.getPrestamo().getCodigo() : null,
			                        0.0, hsFila);
			                }
			            }
			        }
				} 
			} catch (Throwable e) {
				throw new RuntimeException("Falló al validar el seguro de incendio (HS) del partícipe "
					+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
					+ codigoProducto + ": " + e.getMessage(), e);
			}
			System.out.println("========================================");
		}
		
	   // MODO REPROCESO (D-010): PH/PP cuyo excedente devuelto cubría la fila + su HS — nada para el motor.
	   if (excedenteDevuelto > 0.0 && montoArchivo <= 0.01) {
	       System.out.println("   D-010: todo lo descontado (fila + HS) era excedente devuelto - nada para el motor");
	       verificarYActualizarEstadoPrestamos(prestamos);
	       return;
	   }

	   // BUSCAR CUOTA A PAGAR Y PROCESAR
	   DetallePrestamo cuotaAPagar = buscarCuotaAPagar(prestamos, cargaArchivo);

	   if (cuotaAPagar != null) {
	       System.out.println("✅ Cuota a pagar encontrada: #" + cuotaAPagar.getNumeroCuota());
	       aplicarPagoNormalConMotor(cuotaAPagar.getPrestamo(), montoArchivo, cargaArchivo, participe,
	           hsSinSeguroEnCuotaFila ? " " + MARCA_D023 : "");
	       if (hsSinSeguroEnCuotaFila) {
	           montoHsSinSeguroEnCuota += montoHS;
	           hsSinSeguroEnCuota.add("Rol " + participe.getCodigoPetro() + " (" + codigoProducto + "): HS $"
	               + String.format("%,.2f", montoHS) + " aplicado como seguro de incendio de la cuota #"
	               + cuotaAPagar.getNumeroCuota() + " del préstamo "
	               + (cuotaAPagar.getPrestamo() != null ? cuotaAPagar.getPrestamo().getCodigo() : null));
	       }
	   } else {
	       System.out.println("⚠️ No se encontró cuota pendiente");
	       // §4.12: si el HS de la fila no se sumó (no había cuota para validarlo), también se nombra.
	       double hsNoSumado = montoHS > 0.0 || !modoReproceso ? 0.0 : hsDeLaFila(participe, codigoProducto, cargaArchivo);
	       registrarFilaSinPagoReproceso(participe, codigoProducto, montoArchivo + hsNoSumado,
	           "sin cuota pendiente en " + prestamos.size() + " operación(es) vigentes al corte"
	           + (hsNoSumado > 0.0 ? " (incluye HS $" + String.format("%,.2f", hsNoSumado) + ")" : ""));
	   }

	   // ✅ RED DE SEGURIDAD: revisar el estado de TODOS los préstamos tocados.
	   // buscarCuotaAPagar y calcularSaldosRealesCuota también pueden marcar cuotas
	   // como PAGADA (por saldo insignificante o por PagoPrestamo previo), y en ese caso
	   // la última cuota se liquida sin pasar por procesarPagoCuota.
	   verificarYActualizarEstadoPrestamos(prestamos);

	}

/**
 * HS huérfano — VALIDACION-TOPE-AFECTACION-MANUAL.md §13, REESCRITO 2026-09-03 sobre una
 * propuesta del usuario más simple que la primera versión: *"sumemos el valor descontado de
 * HS al valor descontado de préstamos y tratémoslo como un todo que se distribuye con la
 * prelación definida, y dependiendo de dónde se reparta se contabiliza"*.
 *
 * <p><b>El caso normal (con PH/PP) YA hace exactamente eso</b> — más arriba, rama PH/PP de
 * este método: suma {@code montoHS} a {@code montoArchivo} y aplica el total con
 * {@code aplicarPagoNormalConMotor}, que reparte por la cascada del motor (el seguro de
 * incendio es su PRIMER componente) y contabiliza donde cayó, leyendo los componentes del
 * {@code PagoPrestamo} resultante — sin ninguna regla nueva. Este método NO toca esa rama:
 * para un partícipe CON PH/PP, no hace nada (ya está cubierto) y el total cobrado es
 * IDÉNTICO al de antes de esta corrección.</p>
 *
 * <p>Al partícipe cuyo HS llega SIN una fila PH/PP en esta carga, nadie lo miraba: no se
 * aplicaba, no se reclamaba, no aparecía en ningún log. Medido en la carga 449: exactamente
 * los cuatro casos con diferencia negativa del descuadre (2,47+6,34+7,10+13,18 = 29,09). Para
 * estos, este método hace LO MISMO que la rama PH/PP — sumar y aplicar por la cascada —, sólo
 * que primero tiene que resolver A QUÉ préstamo, porque {@code CRD.PXCA} no lleva esa
 * referencia (sólo rol y producto).</p>
 *
 * <p>⛔ <b>Riesgo de doble cobro, verificado antes de escribir.</b> Si este partícipe SÍ
 * tiene PH o PP en esta carga, su HS YA viaja sumado al monto de esa fila — procesarlo
 * también acá lo cobraría dos veces. Por eso lo PRIMERO que hace este método es preguntar
 * "¿existe fila PH o PP de este mismo rol en esta carga?" con
 * {@code selectByCodigoPetroYProductoEnCarga} — la MISMA consulta que ya usa ese camino,
 * invertida. Si existe cualquiera de las dos, este método NO HACE NADA.</p>
 *
 * <p><b>Resolución del préstamo, con la MISMA búsqueda que usa hoy el camino PH/PP</b>
 * ({@link #resolverProductosPorCodigo} + {@code selectByEntidadYProductoActivosById}), no una
 * nueva — sólo que acá hay que CONTAR candidatos antes de aplicar nada, porque un HS huérfano
 * no dice si es de un hipotecario o un prendario (a diferencia de la rama PH/PP, que ya sabe
 * el código declarado por el archivo y no tiene esa ambigüedad). Candidato = préstamo activo
 * de código PH o PP del partícipe con una cuota pendiente (mismo criterio simple que
 * {@link #buscarCuotaAPagar}, sin filtrar por seguro — la prelación decide dónde cae el
 * dinero dentro de esa cuota). Cero candidatos, o más de uno, es {@code NOVEDAD BLOQUEANTE}
 * — no adivinar — para que el operador la resuelva en la pantalla de afectación (§12 ya deja
 * ese sobrante repartible ahí).</p>
 *
 * @return {@code true} si aplicó el pago o generó la novedad bloqueante (en los dos casos
 *         "se hizo algo" con este HS); {@code false} si NO era huérfano (ya cubierto por
 *         PH/PP) o si el monto es $0 — ninguno de los dos es un resultado que contar en el
 *         resumen
 */
private boolean procesarSeguroIncendioHuerfano(ParticipeXCargaArchivo participeHS, CargaArchivo cargaArchivo,
		Map<String, List<Producto>> productosCache) throws Throwable {

	ParticipeXCargaArchivo filaPH = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
		participeHS.getCodigoPetro(), CODIGO_PRODUCTO_PH, cargaArchivo.getCodigo());
	ParticipeXCargaArchivo filaPP = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
		participeHS.getCodigoPetro(), CODIGO_PRODUCTO_PP, cargaArchivo.getCodigo());
	if (filaPH != null || filaPP != null) {
		System.out.println("  HS NO huérfano - Partícipe " + participeHS.getCodigoPetro()
			+ " tiene fila PH/PP en esta carga; su HS ya viaja sumado a ese monto.");
		return false;
	}

	double montoHS = nullSafe(participeHS.getTotalDescontado());
	if (montoHS <= 0.01) {
		return false;
	}

	System.out.println("  ⚠️ HS HUÉRFANO - Partícipe " + participeHS.getCodigoPetro() + " ("
		+ participeHS.getNombre() + ") - Monto: $" + montoHS + " - Sin fila PH/PP en esta carga");

	List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participeHS.getCodigoPetro());
	if (entidades == null || entidades.isEmpty()) {
		if (modoReproceso) {
			// MODO REPROCESO (C4): HS huérfano de un rol sin partícipe — misma constancia y
			// mismo descuento del recibido que las demás filas sin partícipe.
			registrarSinParticipeReproceso(participeHS, CODIGO_PRODUCTO_HS, montoHS, cargaArchivo);
			return true;
		}
		// §15: transacción PROPIA — esta novedad debe sobrevivir aunque la red del §11 revierta
		// aplicarPagosArchivoPetro más adelante.
		registrarNovedadDeAplicacion(participeHS, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
			"HS huérfano (sin PH/PP en esta carga): no se encontró entidad con código Petro "
				+ participeHS.getCodigoPetro() + " para determinar a qué préstamo aplicar el seguro de $"
				+ montoHS + ".",
			null, null, null, montoHS);
		return true;
	}
	Entidad entidad = entidades.get(0);

	// Candidatos: préstamos ACTIVOS de código Petro PH o PP, sin importar si tienen detalle en
	// ESTA carga — el HS no trae declarado su propio producto de préstamo. MISMA resolución
	// que la rama PH/PP de más arriba, no una nueva.
	Map<Long, Prestamo> prestamosPorCodigo = new LinkedHashMap<>();
	for (String codigoProductoPrestamo : new String[] { CODIGO_PRODUCTO_PH, CODIGO_PRODUCTO_PP }) {
		for (Producto producto : resolverProductosPorCodigo(codigoProductoPrestamo, productosCache)) {
			List<Prestamo> prestamosDelProducto =
				prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
			if (prestamosDelProducto != null) {
				for (Prestamo prestamo : prestamosDelProducto) {
					prestamosPorCodigo.put(prestamo.getCodigo(), prestamo);
				}
			}
		}
	}

	// Candidatos REALES: los que tienen una cuota pendiente donde caer — sin filtrar por
	// seguro de incendio a propósito: la prelación de la cascada decide dónde cae el dinero
	// dentro de esa cuota, no este método.
	List<Prestamo> candidatos = new ArrayList<>();
	for (Prestamo prestamo : prestamosPorCodigo.values()) {
		if (tieneCuotaPendiente(prestamo, cargaArchivo)) {
			candidatos.add(prestamo);
		}
	}

	if (candidatos.size() != 1) {
		String detalleCandidatos = candidatos.isEmpty()
			? "ningún préstamo hipotecario/prendario activo del partícipe tiene una cuota pendiente"
			: candidatos.size() + " préstamos tienen cuota pendiente ("
				+ candidatos.stream().map(p -> "#" + p.getCodigo()).collect(java.util.stream.Collectors.joining(", "))
				+ ") — no hay destino inequívoco";
		String descripcion = "HS huérfano (sin PH/PP en esta carga): $" + montoHS + " recibidos de seguro de"
			+ " incendio, pero " + detalleCandidatos + ". Requiere distribución manual (AVPC).";
		System.err.println("⛔ NOVEDAD BLOQUEANTE (HS huérfano): partícipe " + participeHS.getCodigoPetro()
			+ " (" + participeHS.getNombre() + ") - " + descripcion);
		// §15: transacción PROPIA — ver comentario del otro registrarNovedad de este método.
		registrarNovedadDeAplicacion(participeHS, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE, descripcion,
			null, null, null, Double.valueOf(montoHS));
		return true;
	}

	Prestamo unico = candidatos.get(0);
	System.out.println("  ✅ HS huérfano resuelto - Partícipe " + participeHS.getCodigoPetro() + " ("
		+ participeHS.getNombre() + ") - Préstamo #" + unico.getCodigo() + " - Monto: $" + montoHS
		+ " - aplicado por la cascada del motor (misma prelación que el caso normal)");
	aplicarPagoNormalConMotor(unico, montoHS, cargaArchivo, participeHS);
	return true;
}

/**
 * Si un préstamo tiene una cuota pendiente real donde aplicar un pago — mismo criterio
 * simple que usa {@link #buscarCuotaAPagar} (mínima cuota no pagada, saldo real > 0.01), pero
 * sin aplicar nada: {@link #procesarSeguroIncendioHuerfano} necesita CONTAR candidatos antes
 * de decidir a cuál aplicar.
 */
private boolean tieneCuotaPendiente(Prestamo prestamo, CargaArchivo cargaArchivo) throws Throwable {
	List<DetallePrestamo> resultado = detallePrestamoDaoService.selectMinCuotaNoPagadaByPrestamo(prestamo.getCodigo());
	if (resultado == null || resultado.isEmpty()) {
		return false;
	}
	SaldosRealesCuota saldos = calcularSaldosRealesCuota(resultado.get(0), cargaArchivo);
	return saldos.totalPendiente > 0.01;
}

/**
 * Busca la cuota pendiente a pagar para un préstamo ACTIVO (no de plazo vencido)
 * ✅ CORRECCIÓN CRÍTICA: Usa calcularSaldosRealesCuota para validar saldos desde PagoPrestamo
 * Si una cuota ya está pagada según PagoPrestamo, se actualiza su estado y se busca la siguiente
 */
private DetallePrestamo buscarCuotaAPagar(List<Prestamo> prestamos, 
                                          CargaArchivo cargaArchivo) throws Throwable {
	for (Prestamo prestamo : prestamos) {
		// ✅ MEGA OPTIMIZACIÓN: Buscar iterativamente la mínima cuota pendiente
		// hasta encontrar una con saldo real > 0, en lugar de traer todas las cuotas a memoria
		
		int intentos = 0;
		int maxIntentos = 100; // Límite de seguridad para evitar ciclo infinito
		
		while (intentos < maxIntentos) {
			intentos++;
			
			// Buscar la mínima cuota NO pagada (por número de cuota)
			List<DetallePrestamo> resultado = 
				detallePrestamoDaoService.selectMinCuotaNoPagadaByPrestamo(prestamo.getCodigo());
			
			// Si no hay más cuotas pendientes en este préstamo, pasar al siguiente
			if (resultado == null || resultado.isEmpty()) {
				System.out.println("  ℹ️ No hay más cuotas pendientes en préstamo #" + prestamo.getCodigo());
				break;
			}
			
			DetallePrestamo cuota = resultado.get(0);
			
			// ✅ CORRECCIÓN CRÍTICA: Calcular saldos reales consultando tabla PagoPrestamo
			SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota, cargaArchivo);
			
			// ✅ Si calcularSaldosRealesCuota actualizó la cuota a PAGADA, 
			// volver a buscar la mínima cuota (ahora será la siguiente)
			if (cuota.getEstado() != null && 
			    cuota.getEstado() == com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
				System.out.println("  ℹ️ Cuota #" + cuota.getNumeroCuota() + 
				                   " actualizada a PAGADA según PagoPrestamo - Buscando siguiente cuota (intento " + intentos + ")");
				continue; // Volver a ejecutar el ciclo para buscar la siguiente mínima cuota
			}
			
			// ✅ SIN TOLERANCIA: Si tiene saldo pendiente real, retornarla
			if (saldos.totalPendiente > 0.01) {
				System.out.println("  ✅ Cuota pendiente encontrada: #" + cuota.getNumeroCuota() + 
				                   " - Saldo pendiente real: $" + saldos.totalPendiente + 
				                   " (encontrada en intento " + intentos + ")");
				return cuota;
			}
			
			// Si llegamos aquí, la cuota tiene saldo <= 0.01 pero no fue marcada como PAGADA
			// Marcarla explícitamente y buscar la siguiente
			System.out.println("  ⚠️ Cuota #" + cuota.getNumeroCuota() + 
			                   " tiene saldo insignificante ($" + saldos.totalPendiente + 
			                   ") - Marcando como PAGADA y continuando");
			cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			detallePrestamoDaoService.save(cuota, cuota.getCodigo());
		}
		
		if (intentos >= maxIntentos) {
			System.err.println("  ⚠️ Se alcanzó el límite de intentos (" + maxIntentos + 
			                   ") buscando cuota pendiente en préstamo #" + prestamo.getCodigo());
		}
	}
	
	return null;
}

/**
 * §4.2/§4.3 PLAN-FASE3-MOTOR-PAGOS.md (2026-09-02): aplica el pago normal de un partícipe
 * delegando en el motor compartido, en vez de reimplementar la cascada localmente. El motor
 * deriva el seguro de incendio, desgravamen, mora, interés vencido, interés y capital de los
 * saldos reales de cada cuota (PGPR), no de un parámetro suelto — por eso ya no hace falta
 * pasarle valorSeguroIncendio por separado (ya viene sumado en montoArchivo).
 */
private void aplicarPagoNormalConMotor(Prestamo prestamo, double monto, CargaArchivo cargaArchivo,
                                        ParticipeXCargaArchivo participe) throws Throwable {
	aplicarPagoNormalConMotor(prestamo, monto, cargaArchivo, participe, "");
}

/** Marca de los pagos cuyo HS se aplicó como seguro de una cuota que no lo traía (§4.12). */
static final String MARCA_D023 = "[D-023 HS sin seguro en la cuota]";

/**
 * §4.12: monto HS que viaja con ESTA fila PH/PP — la misma regla de {@link #montoConHsMergeado}
 * (PH se lo lleva siempre; PP solo si no hay PH). 0 para cualquier otro producto o si no hay HS.
 */
private double hsDeLaFila(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo) throws Throwable {
	if (!CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) && !CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
		return 0.0;
	}
	double hs = montoConHsMergeado(participe, codigoProducto, cargaArchivo) - nullSafe(participe.getTotalDescontado());
	return hs > 0.01 ? Math.round(hs * 100.0) / 100.0 : 0.0;
}

/**
 * §4.12 (D-023, solo modo reproceso): la cuota a pagar no trae seguro de incendio
 * ({@code DTPRVLSI = 0}, dato de la migración) pero Petro descontó HS para esta fila. Antes de
 * entregar el dinero al motor, la cuota pasa a tener ese seguro: {@code DTPRVLSI = HS} y
 * {@code DTPRTTLL}/{@code DTPRTTCS} suben en lo mismo ({@code DTPRTTLL} es la suma de los
 * componentes, seguro incluido — medido en sql/182 y sql/60 — y así lo leen {@code totalConMoraIV}
 * del motor y {@code totalBaseCuota} de esta clase). Con eso la prelación del motor (seguro de incendio primero) imputa el HS a
 * {@code PGPRVLSI} y el asiento de aplicación lo lleva a la cuenta de seguro de incendio, como a
 * cualquier cuota que lo trae. Subir solo el seguro sin el total dejaría {@code DTPRTTLL} menor que
 * la suma de componentes ("desfase" del motor) y el HS volvería a la cascada.
 */
private void prepararSeguroIncendioEnCuotaReproceso(DetallePrestamo cuota, double hs, ParticipeXCargaArchivo participe) throws Throwable {
	double seguroAnterior = nullSafe(cuota.getValorSeguroIncendio());
	cuota.setValorSeguroIncendio(Math.round((seguroAnterior + hs) * 100.0) / 100.0);
	if (cuota.getTotal() != null) {
		cuota.setTotal(Math.round((cuota.getTotal() + hs) * 100.0) / 100.0);
	}
	if (cuota.getTotalConSeguro() != null) {
		cuota.setTotalConSeguro(Math.round((cuota.getTotalConSeguro() + hs) * 100.0) / 100.0);
	}
	detallePrestamoService.saveSingle(cuota);
	System.out.println("   D-023: rol " + participe.getCodigoPetro() + " - cuota #" + cuota.getNumeroCuota() + " del préstamo "
		+ (cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null) + " sin seguro de incendio: DTPRVLSI "
		+ seguroAnterior + " -> " + cuota.getValorSeguroIncendio() + " (HS $" + hs + " descontado por Petro) " + MARCA_D023);
}

/** Igual, con un sufijo para la observación de los PGPR (§4.12: marca D-023). */
private void aplicarPagoNormalConMotor(Prestamo prestamo, double monto, CargaArchivo cargaArchivo,
                                        ParticipeXCargaArchivo participe, String sufijoObservacion) throws Throwable {
	if (prestamo == null || prestamo.getCodigo() == null) {
		System.out.println("⚠️ No se puede aplicar el pago: la cuota encontrada no tiene préstamo asociado");
		registrarFilaSinPagoReproceso(participe, null, monto, "la cuota encontrada no tiene préstamo asociado");
		return;
	}
	ContextoPago ctx = crearContextoPagoCarga(cargaArchivo, "PAGO_NORMAL");
	if (sufijoObservacion != null && !sufijoObservacion.isEmpty()) {
		ctx.setObservacion(ctx.getObservacion() + sufijoObservacion);
	}
	ResultadoAplicacionPago resultado = motorPagoPrestamoService.aplicarPago(prestamo.getCodigo(), monto, ctx);
	System.out.println("    ✅ Pago aplicado vía motor - Recibido: $" + resultado.getValorRecibido() +
	                   " - Aplicado: $" + resultado.getValorAplicado() +
	                   " - Cuotas afectadas: " + resultado.getCuotasAfectadas().size());
	manejarExcedenteNoAplicado(resultado, prestamo, cargaArchivo, participe);
}

/**
 * Contexto de pago común para toda aplicación de la carga Petro (pago normal y afectación
 * manual): fecha de efecto = último día del mes de carga (§4.2, sin fallback a now() — si
 * mes/año de afectación vienen null, grita) y la carga misma para trazabilidad (§4.1,
 * ContextoPago#idCargaArchivo).
 *
 * {@code origen} identifica la RUTA de este pago (2026-09-02, corrección urgente: la
 * observación anterior era idéntica para las dos rutas y no permitía distinguirlas al
 * auditar, ver sql/171_ORIGEN_DE_LA_BRECHA_449.sql). Se antepone como prefijo estable
 * ("PAGO_NORMAL: " / "AFECTACION_MANUAL: ...") para poder filtrar por
 * {@code PGPROBSR LIKE 'PAGO_NORMAL:%'} sin ambigüedad.
 */
private ContextoPago crearContextoPagoCarga(CargaArchivo cargaArchivo, String origen) throws Throwable {
	if (cargaArchivo.getMesAfectacion() == null || cargaArchivo.getAnioAfectacion() == null) {
		throw new IncomeException("No se puede fechar el pago: falta el mes o año de afectación de la carga "
			+ cargaArchivo.getCodigo() + ".");
	}
	java.time.LocalDateTime fechaPagoEfecto = fechaService.ultimoDiaMesAnioLocal(
		cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()).atTime(23, 59, 59);

	ContextoPago ctx = new ContextoPago();
	ctx.setIdCargaArchivo(cargaArchivo.getCodigo());
	ctx.setFechaPago(fechaPagoEfecto);
	ctx.setObservacion(String.format("%s: Carga Petro %d - Mes %d/%d",
		origen, cargaArchivo.getCodigo(), cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()));
	return ctx;
}

/**
 * §4.5 PLAN-FASE3-MOTOR-PAGOS.md: el excedente que el motor no pudo aplicar a ninguna cuota
 * del préstamo (sin más cuotas pendientes con saldo) NUNCA se escribe en PGPRVLRR — "dinero
 * sin destino ES el descuadre; que bloquee es el punto". Se registra como novedad
 * MONTO_INCONSISTENTE (clasifica BLOQUEANTE vía FamiliaNovedadCarga.clasificar, al tener
 * montoDiferencia positiva) para que el operador lo distribuya manualmente vía AVPC.
 */
private void manejarExcedenteNoAplicado(ResultadoAplicacionPago resultado, Prestamo prestamo,
                                         CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) {
	double excedente = resultado.getExcedenteNoAplicado();
	if (excedente <= 0.01) {
		return;
	}
	System.out.println("⚠️ Excedente no aplicado por el motor de pagos: $" + excedente +
	                   " (préstamo " + prestamo.getCodigo() + ") - Registrando novedad BLOQUEANTE");
	registrarFilaSinPagoReproceso(participe, null, excedente,
		"excedente del motor sin cuota pendiente (préstamo " + prestamo.getCodigo() + ")");
	String descripcion = String.format(
		"Excedente sin aplicar tras el pago en cascada del préstamo %d: recibido $%.2f, aplicado $%.2f, "
		+ "sobran $%.2f sin cuota pendiente donde aplicarlos - requiere distribución manual (AVPC)",
		prestamo.getCodigo(), resultado.getValorRecibido(), resultado.getValorAplicado(), excedente);
	// §15: transacción PROPIA — este excedente se detecta y registra DENTRO de
	// aplicarPagosArchivoPetro; si la red del §11 revierte después, no puede llevarse esta
	// novedad con ella.
	registrarNovedadDeAplicacion(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE, descripcion,
		null, prestamo.getCodigo(), resultado.getValorAplicado(), resultado.getValorRecibido());
}

/**
 * ✅ CRÍTICO: Verifica si todas las cuotas de un préstamo están pagadas
 * Si es así, actualiza el estado del préstamo a CANCELADO
 * ✅ OPTIMIZACIÓN: Usa conteos en BD en lugar de traer las cuotas a memoria
 *
 * Debe invocarse al final de TODA ruta que pueda marcar una cuota como PAGADA
 * (pago exacto, pago con excedente, afectación manual, recálculo por PagoPrestamo),
 * porque cualquiera de ellas puede ser la que liquida la última cuota del préstamo.
 */
private void verificarYActualizarEstadoPrestamo(Prestamo prestamo) throws Throwable {
	if (prestamo == null || prestamo.getCodigo() == null) {
		// Ausencia de dato legítima: nada que verificar sin un préstamo.
		return;
	}

	// TODO O NADA (2026-08-29): antes, si esto fallaba (regularizarPrestamoSiSinMora, o el save
	// final de CANCELADO), se tragaba el error. El llamador YA había guardado la cuota como
	// liquidada antes de invocar esto — si esto fallaba en silencio, la última cuota quedaba
	// PAGADA pero el préstamo se quedaba VIGENTE para siempre (o no volvía de EN_MORA a
	// VIGENTE). regularizarPrestamoSiSinMora y los contarCuotas* de abajo ya manejan sus
	// propias ausencias de dato con "if" (préstamo sin tabla, sin cuotas pendientes, etc.) — lo
	// único que puede llegar acá es un fallo real de consulta o de guardado.
	try {
		// idEstado (PRSTIDST) es el campo con el código alterno del rubro y el
		// único que consultan las queries del módulo y los reportes. ESPSCDGO
		// es FK al catálogo CRD.ESPS y no admite códigos de rubro.
		Long estadoActual = prestamo.getIdEstado();

		// Si ya está en un estado terminal, no hay nada que actualizar
		if (estadoActual != null && (
			estadoActual == com.saa.rubros.EstadoPrestamo.CANCELADO ||
			estadoActual == com.saa.rubros.EstadoPrestamo.CANCELADO_ANTICIPADO ||
			estadoActual == com.saa.rubros.EstadoPrestamo.CANCELADO_POR_NOVACION)) {
			return;
		}

		// Pedido 10: si el pago dejó al préstamo sin cuotas vencidas y estaba EN_MORA(11),
		// vuelve a VIGENTE(2) de inmediato, en vez de esperar al proceso diario de las 02:00.
		// No interfiere con la cancelación de más abajo: un préstamo que además canceló su
		// última cuota sale de este método en el estado terminal correcto igual.
		procesoMoraPrestamoService.regularizarPrestamoSiSinMora(prestamo.getCodigo());

		// ✅ Validar que el préstamo realmente tenga tabla de amortización.
		// Sin esta validación, un préstamo sin cuotas daría "0 pendientes" y se cancelaría por error.
		// MODO REPROCESO (§11 Rendimiento): el total de cuotas del préstamo no cambia durante la
		// corrida (se actualizan cuotas, nunca se insertan ni borran) — una consulta por préstamo.
		Long totalCuotas;
		if (modoReproceso) {
			totalCuotas = totalCuotasPorPrestamoReproceso.get(prestamo.getCodigo());
			if (totalCuotas == null) {
				totalCuotas = detallePrestamoDaoService.contarCuotasByPrestamo(prestamo.getCodigo());
				if (totalCuotas != null) {
					totalCuotasPorPrestamoReproceso.put(prestamo.getCodigo(), totalCuotas);
				}
			}
		} else {
			totalCuotas = detallePrestamoDaoService.contarCuotasByPrestamo(prestamo.getCodigo());
		}
		if (totalCuotas == null || totalCuotas == 0L) {
			System.out.println("  ℹ️ Préstamo #" + prestamo.getCodigo() +
			                   " sin cuotas registradas - No se evalúa cancelación");
			return;
		}

		// ✅ OPTIMIZACIÓN: Solo contar cuotas NO pagadas.
		// Si el conteo es 0, todas están PAGADAS o CANCELADAS ANTICIPADAMENTE.
		Long cuotasPendientes = detallePrestamoDaoService.contarCuotasPendientesByPrestamo(prestamo.getCodigo());

		System.out.println("  🔍 Préstamo #" + prestamo.getCodigo() +
		                   " - Cuotas: " + totalCuotas + " / Pendientes: " + cuotasPendientes);

		if (cuotasPendientes == null || cuotasPendientes > 0L) {
			return;
		}

		System.out.println("  ✅ TODAS LAS CUOTAS PAGADAS - Actualizando préstamo a CANCELADO");
		System.out.println("     Préstamo ID: " + prestamo.getCodigo() +
		                   " (estado anterior: " + estadoActual + ")");

		// ⚠️ NO tocar fechaFin: es la fecha de vencimiento de la última cuota (fin del plazo),
		// no la fecha de cancelación. Sobrescribirla destruye el plazo original del préstamo.
		prestamo.setIdEstado(Long.valueOf(com.saa.rubros.EstadoPrestamo.CANCELADO));
		prestamo.setFechaModificacion(java.time.LocalDateTime.now());
		prestamoDaoService.save(prestamo, prestamo.getCodigo());

		System.out.println("     ✅ Préstamo actualizado a estado CANCELADO");

	} catch (Throwable e) {
		throw new RuntimeException("Falló al verificar/actualizar el estado del préstamo "
			+ prestamo.getCodigo() + " tras el pago — una cuota pudo haber quedado marcada "
			+ "como pagada sin que el préstamo reflejara el cambio de estado. Causa: "
			+ e.getMessage(), e);
	}
}

/**
 * Verifica el estado de una lista de préstamos, evitando repetir la verificación
 * sobre el mismo préstamo. Se usa como red de seguridad al terminar de procesar
 * a un partícipe, cuando el pago pudo haberse aplicado por cualquiera de las rutas.
 */
private void verificarYActualizarEstadoPrestamos(List<Prestamo> prestamos) throws Throwable {
	if (prestamos == null || prestamos.isEmpty()) {
		return;
	}

	java.util.Set<Long> verificados = new java.util.HashSet<>();
	for (Prestamo prestamo : prestamos) {
		if (prestamo == null || prestamo.getCodigo() == null) {
			continue;
		}
		if (verificados.add(prestamo.getCodigo())) {
			verificarYActualizarEstadoPrestamo(prestamo);
		}
	}
}


/**
 * Marca cuotas en MORA cuando el monto descontado es $0
 * ✅ OPTIMIZACIÓN: Este método se ejecuta ANTES de buscar entidades, productos, préstamos o HS
 * Solo busca lo mínimo necesario para marcar la cuota en mora
 */
private void marcarCuotasEnMoraPorFaltaDePago(ParticipeXCargaArchivo participe,
                                               String codigoProducto,
                                               CargaArchivo cargaArchivo,
                                               Map<String, List<Producto>> productosCache) throws Throwable {
	System.out.println("========================================");
	System.out.println("MARCAR CUOTAS EN MORA - Sin pago recibido");
	System.out.println("Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
	System.out.println("Producto: " + codigoProducto);

	// TODO O NADA (2026-08-29): marcar la mora ES el propósito completo de este método —no un
	// efecto colateral opcional—, así que un fallo a mitad de camino (típicamente el saveSingle
	// de una cuota) debe abortar, no tragarse: antes, el llamador (aplicarPagoParticipe) hacía
	// `return` después de esta llamada sin mirar si tuvo éxito, así que un partícipe podía
	// quedar sin pago Y sin mora bien marcada, en silencio. Las ausencias de dato (sin entidad,
	// sin producto, sin préstamos activos, sin cuotas del mes) ya estaban manejadas con "if"
	// explícitos y siguen exactamente igual.
	try {
		// Buscar entidad
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
		if (entidades == null || entidades.isEmpty()) {
			System.out.println("⚠️ No se encontró entidad - No se pueden marcar cuotas en mora");
			return;
		}
		Entidad entidad = entidades.get(0);
		
		// Buscar productos (memoizado por carga, ver resolverProductosPorCodigo)
		List<Producto> productos = resolverProductosPorCodigo(codigoProducto, productosCache);
		if (productos == null || productos.isEmpty()) {
			System.out.println("⚠️ No se encontró producto - No se pueden marcar cuotas en mora");
			return;
		}
		
		// Buscar préstamos activos
		List<Prestamo> prestamos = new ArrayList<>();
		for (Producto producto : productos) {
			List<Prestamo> prestamosDelProducto = 
				prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
			if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
				prestamos.addAll(prestamosDelProducto);
			}
		}
		
		if (prestamos.isEmpty()) {
			System.out.println("⚠️ No se encontraron préstamos activos - No se pueden marcar cuotas en mora");
			return;
		}
		
		// Buscar cuotas del mes que debieron pagarse
		int cuotasMarcadasMora = 0;
		for (Prestamo prestamo : prestamos) {
			List<DetallePrestamo> cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
				prestamo.getCodigo(),
				cargaArchivo.getMesAfectacion().intValue(),
				cargaArchivo.getAnioAfectacion().intValue()
			);
			
			if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
				for (DetallePrestamo cuota : cuotasDelMes) {
					Long estadoCuota = cuota.getEstado();
					
					// Solo marcar en mora si NO está PAGADA ni CANCELADA
					if (estadoCuota != null && 
					    estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
					    estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
						
						// Marcar cuota EN MORA
						cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.EN_MORA);
						cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.EN_MORA);
						cuota.setCodigoExterno(cargaArchivo.getCodigo());
						detallePrestamoService.saveSingle(cuota);
						
						cuotasMarcadasMora++;
						System.out.println("  ⚠️ Cuota #" + cuota.getNumeroCuota() + " marcada EN MORA (sin pago)");
					}
				}
			}
		}
		
		if (cuotasMarcadasMora > 0) {
			System.out.println("✅ Total cuotas marcadas EN MORA: " + cuotasMarcadasMora);
		} else {
			System.out.println("ℹ️ No se encontraron cuotas pendientes para marcar en mora");
		}
		
	} catch (Throwable e) {
		throw new RuntimeException("Falló al marcar cuotas en mora por falta de pago del partícipe "
			+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
			+ codigoProducto + ": " + e.getMessage(), e);
	}

	System.out.println("========================================");
}

/**
 * ✅ PRIORIDAD MÁXIMA: Verifica si existen registros de AfectacionValoresParticipeCarga
 * para las novedades de este partícipe y aplica TODOS los pagos según esos registros.
 * 
 * @return true si se encontraron y aplicaron afectaciones manuales, false si no existen
 */
private boolean verificarYAplicarAfectacionesManualesTotales(
		ParticipeXCargaArchivo participe,
		String codigoProducto,
		CargaArchivo cargaArchivo) throws Throwable {

	// TODO O NADA (2026-08-29): antes, si esto fallaba a mitad de aplicar varias afectaciones
	// (algunas ya aplicadas y persistidas), se atrapaba el error y se devolvía `false` — "no
	// había afectaciones, seguí con el flujo normal". Eso era falso (sí había, algunas quedaron
	// aplicadas) y además hacía que el llamador corriera EL FLUJO NORMAL DE PAGO ENCIMA de lo ya
	// aplicado manualmente: riesgo real de pagar la misma cuota dos veces. Ya no hay catch acá:
	// selectByParticipe/selectByNovedad son consultas que absorben sus propios errores y
	// devuelven lista vacía (convención de DAO, sin cambios), así que lo único que puede
	// propagar de este método es un fallo real dentro del bucle de aplicación (ver el try
	// puntual más abajo).
	//
	// 1. Buscar las novedades del partícipe
	List<NovedadParticipeCarga> novedades =
		novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());

	if (novedades == null || novedades.isEmpty()) {
		System.out.println("   No se encontraron novedades para el partícipe");
		return false;
	}

	System.out.println("   ✅ Se encontraron " + novedades.size() + " novedad(es) para el partícipe");

	// 2. Buscar TODAS las afectaciones manuales asociadas a estas novedades
	List<AfectacionValoresParticipeCarga> afectaciones = new ArrayList<>();
	for (NovedadParticipeCarga novedad : novedades) {
		List<AfectacionValoresParticipeCarga> afectacionesNovedad =
			afectacionValoresParticipeCargaDaoService.selectByNovedad(novedad.getCodigo());

		if (afectacionesNovedad != null && !afectacionesNovedad.isEmpty()) {
			afectaciones.addAll(afectacionesNovedad);
		}
	}

	// 3. Si NO hay afectaciones manuales, retornar false para continuar proceso normal
	// (ausencia de dato legítima: la mayoría de los partícipes no tiene ninguna)
	if (afectaciones.isEmpty()) {
		System.out.println("   No se encontraron afectaciones manuales (AVPC) para las novedades");
		return false;
	}

	System.out.println("   🎯 AFECTACIONES MANUALES ENCONTRADAS: " + afectaciones.size() + " registro(s)");
	System.out.println("   📋 Aplicando pagos EXCLUSIVAMENTE según tabla AfectacionValoresParticipeCarga");

	// 4. Aplicar cada afectación manual en el orden de afectación definido por las reglas
	// Ordenar por: desgravamen, interés, capital (orden estándar de aplicación)
	afectaciones.sort((a1, a2) -> {
		// Priorizar por número de cuota si está disponible
		DetallePrestamo cuota1 = a1.getDetallePrestamo();
		DetallePrestamo cuota2 = a2.getDetallePrestamo();

		if (cuota1 != null && cuota2 != null) {
			Double numCuota1 = cuota1.getNumeroCuota();
			Double numCuota2 = cuota2.getNumeroCuota();
			if (numCuota1 != null && numCuota2 != null) {
				return numCuota1.compareTo(numCuota2);
			}
		}

		// Si no hay código de cuota, mantener orden original
		return 0;
	});

	// 5. Aplicar cada afectación
	//
	// TRES ramas, no dos (2026-08-31, condición del árbitro al autorizar el excedente a
	// aporte): el `continue` original cubría a la vez la afectación a aporte (que antes no
	// existía, sin cuota Y sin tipo de aporte) y una fila genuinamente rota (sin cuota Y sin
	// tipo de aporte). Colapsar las tres en un solo `if (cuota == null) continue` volvería a
	// tragarse en silencio exactamente el defecto #1 de los diez del 29 ("devolvía 'no había
	// afectaciones' — mentira: algunas ya se aplicaron"): una AVPC sin destino ninguno debe
	// abortar la carga, no omitirse.
	int aplicadas = 0;
	List<Prestamo> prestamosAfectados = new ArrayList<>();
	for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
		DetallePrestamo cuota = afectacion.getDetallePrestamo();
		com.saa.model.crd.TipoAporte tipoAporte = afectacion.getTipoAporte();

		if (cuota == null && tipoAporte == null) {
			// Dato ausente, no legítimo: CK_AVPC_PRST_XOR_TPAP exige uno de los dos. Una fila
			// sin ninguno es un registro roto (¿reparto incompleto? ¿fila corrupta?), no una
			// ausencia de dato esperable — falla fuerte en vez de omitir en silencio.
			throw new IncomeException("La afectación manual (AVPC " + afectacion.getCodigo()
				+ ") del partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
				+ ") no tiene cuota ni tipo de aporte asignado; no hay a dónde aplicarla.");
		}

		if (cuota == null) {
			// Excedente redirigido a un aporte (opción ③ del §3.7, PLAN-EXCEDENTE-PETRO-A-APORTES.md).
			System.out.println("   📌 Aplicando afectación manual (AVPC " + afectacion.getCodigo()
				+ ") a aporte " + tipoAporte.getNombre());
			try {
				aplicarAfectacionAAporte(afectacion, tipoAporte, cargaArchivo, participe);
			} catch (Throwable e) {
				throw new RuntimeException("Falló al aplicar la afectación manual (AVPC "
					+ afectacion.getCodigo() + ") al aporte " + tipoAporte.getNombre()
					+ " para el partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
					+ "): " + e.getMessage(), e);
			}
			aplicadas++;
			continue;
		}

		System.out.println("   📌 Aplicando afectación manual a cuota #" + cuota.getNumeroCuota() +
		                   " (ID: " + cuota.getCodigo() + ")");

		try {
			// Aplicar la afectación manual a esta cuota
			aplicarAfectacionManualConRegistroPago(cuota, afectacion, cargaArchivo, participe);
		} catch (Throwable e) {
			throw new RuntimeException("Falló al aplicar la afectación manual (AVPC "
				+ afectacion.getCodigo() + ") a la cuota #" + cuota.getNumeroCuota()
				+ " del préstamo " + (cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null)
				+ " para el partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
				+ "): " + e.getMessage(), e);
		}
		aplicadas++;

		if (cuota.getPrestamo() != null) {
			prestamosAfectados.add(cuota.getPrestamo());
		}
	}

	System.out.println("   ✅ Se aplicaron " + aplicadas + " afectación(es) manual(es)");

	// ✅ CRÍTICO: Una afectación manual también puede liquidar la última cuota del préstamo
	verificarYActualizarEstadoPrestamos(prestamosAfectados);

	// Si se aplicó al menos una afectación, retornar true
	return aplicadas > 0;
}

/**
 * Aplica una afectación manual de valores con registro de pago completo.
 * Este método se utiliza cuando se procesan afectaciones manuales de forma prioritaria.
 *
 * §4.4 PLAN-FASE3-MOTOR-PAGOS.md (2026-09-02, ASUNCIÓN DEL ÁRBITRO PENDIENTE DE CONFIRMAR CON
 * EL USUARIO): AVPC sigue definiendo destino (préstamo, vía cuota.getPrestamo()) y monto
 * (valorAfectar) — eso sigue siendo lo que tipeó el operador. Lo que cambia es CÓMO se reparte
 * ese monto: antes se distribuía manualmente entre capitalAfectar/interesAfectar/
 * desgravamenAfectar (campos de la propia fila AVPC, que no tiene columna de seguro de
 * incendio ni de mora); ahora se delega en la cascada del motor compartido, que reparte según
 * los saldos REALES de las cuotas del préstamo — empezando por la más antigua con saldo
 * pendiente, que puede NO ser afectacion.getDetallePrestamo() si esa cuota ya no es la más
 * antigua pendiente del préstamo.
 */
private void aplicarAfectacionManualConRegistroPago(
		DetallePrestamo cuota,
		AfectacionValoresParticipeCarga afectacion,
		CargaArchivo cargaArchivo,
		ParticipeXCargaArchivo participe) throws Throwable {

	double valorTotalAfectar = nullSafe(afectacion.getValorAfectar());

	System.out.println("      📋 Aplicando afectación manual (AVPC ID: " + afectacion.getCodigo() + ") vía motor de pagos");
	System.out.println("         Valor a afectar: $" + valorTotalAfectar);

	if (valorTotalAfectar <= 0.01) {
		System.out.println("      ⚠️ Valor a afectar es $0 - No se registra pago");
		return;
	}
	if (cuota.getPrestamo() == null || cuota.getPrestamo().getCodigo() == null) {
		throw new IncomeException("La afectación manual (AVPC " + afectacion.getCodigo()
			+ ") no tiene préstamo asociado a través de su cuota; no hay destino donde aplicar el pago.");
	}

	ContextoPago ctx = crearContextoPagoCarga(cargaArchivo, "AFECTACION_MANUAL");
	ctx.setObservacion(ctx.getObservacion() + " - AVPC " + afectacion.getCodigo());
	ResultadoAplicacionPago resultado = motorPagoPrestamoService.aplicarPago(
		cuota.getPrestamo().getCodigo(), valorTotalAfectar, ctx);

	manejarExcedenteNoAplicado(resultado, cuota.getPrestamo(), cargaArchivo, participe);

	System.out.println("      ✅ Afectación manual aplicada vía motor - Aplicado: $" + resultado.getValorAplicado() +
	                   " en " + resultado.getCuotasAfectadas().size() + " cuota(s)");
}

/**
 * Ejecuta validaciones de fase 2 para un partícipe
 */
private void validarNovedadesFase2(ParticipeXCargaArchivo participe,
                                   String codigoProducto,
                                   CargaArchivo cargaArchivo,
                                   Map<String, List<Producto>> productosCache) {
	try {
		// ==========================================
		// VALIDACIÓN ESPECIAL: PRODUCTO AH (APORTES)
		// ==========================================
		if (CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto)) {
			System.out.println("========================================");
			System.out.println("VALIDACIÓN PRODUCTO AH (APORTES)");
			System.out.println("Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
			System.out.println("========================================");
			
			validarAporteAH(participe, cargaArchivo);
			return; // Terminar validación, ya que AH no es préstamo
		}
		
		// ==========================================
		// CONTINUAR CON VALIDACIONES DE PRÉSTAMOS
		// ==========================================
		
		// VALIDACIÓN 9: PRODUCTO_NO_MAPEADO
		List<Producto> productos = null;
		try {
			productos = resolverProductosPorCodigo(codigoProducto, productosCache);
		} catch (Throwable e) {
			System.err.println("Error al buscar productos: " + e.getMessage());
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO, 
				"Error al buscar producto: " + e.getMessage(), null, null, null, null);
			return;
		}
		
		if (productos == null || productos.isEmpty()) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO, 
				"No se encontró producto con código Petro: " + codigoProducto, null, null, null, null);
			return;
		}
		
		// VALIDACIÓN 10: PRESTAMO_NO_ENCONTRADO
		Long rolPetroLong = participe.getCodigoPetro();
		if (rolPetroLong == null) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"El partícipe no tiene código Petro válido", null, null, null, participe.getTotalDescontado());
			return;
		}
		
		// Buscar la entidad por código Petro
		List<Entidad> entidades = null;
		try {
			entidades = entidadDaoService.selectByCodigoPetro(rolPetroLong);
		} catch (Throwable e) {
			System.err.println("Error al buscar entidad " + rolPetroLong + ": " + e.getMessage());
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"Error al buscar entidad: " + e.getMessage(), null, null, null, participe.getTotalDescontado());
			return;
		}
		
		if (entidades == null || entidades.isEmpty()) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"No se encontró entidad con código Petro: " + rolPetroLong, null, null, null, participe.getTotalDescontado());
			return;
		}
		
		Entidad entidad = entidades.get(0);
		
		// Buscar préstamos usando IDs numéricos
		List<Prestamo> prestamos = new ArrayList<>();
		
		for (Producto producto : productos) {
			try {
				List<Prestamo> prestamosDelProducto = 
					prestamoDaoService.selectByEntidadYProductoActivosById(
						entidad.getCodigo(),
						producto.getCodigo()
					);
					
				if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
					prestamos.addAll(prestamosDelProducto);
				}
			} catch (Throwable e) {
				System.err.println("Error al buscar préstamos para producto " + producto.getCodigo() + ": " + e.getMessage());
				// Continuar con el siguiente producto
			}
		}
			
		if (prestamos.isEmpty()) {
			Long codigoProductoDB = (productos != null && !productos.isEmpty()) ? productos.get(0).getCodigo() : null;
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"No se encontró ningún préstamo activo para el código Petro: " + codigoProducto,
				codigoProductoDB, null, null, nullSafe(participe.getTotalDescontado()));
			return;
		}
		
		// VALIDACIÓN 12: CUOTA_NO_ENCONTRADA / CUOTA_FECHA_DIFERENTE
		// IMPORTANTE: Si el código es PH o PP, el seguro viene en un registro separado con código HS
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		
		// Caso especial para PH (Hipotecario) y PP (Préstamo Prendario): buscar el registro HS correspondiente
		if (CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
		    CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
			
			System.out.println("========================================");
			System.out.println("VALIDACIÓN " + codigoProducto + " - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
			System.out.println("Monto " + codigoProducto + ": $" + participe.getTotalDescontado());
			
			try {
				ParticipeXCargaArchivo participeHS = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
					participe.getCodigoPetro(),
					CODIGO_PRODUCTO_HS,
					cargaArchivo.getCodigo()
				);
				
				if (participeHS != null) {
					double montoHS = nullSafe(participeHS.getTotalDescontado());
					montoArchivo += montoHS;
					System.out.println("Monto HS encontrado: $" + montoHS);
					System.out.println("TOTAL A VALIDAR (" + codigoProducto + " + HS): $" + montoArchivo);
				} else {
					System.out.println("⚠️ ADVERTENCIA: No se encontró registro HS para este partícipe");
					System.out.println("Se validará solo con monto " + codigoProducto + ": $" + montoArchivo);
				}
			} catch (Throwable e) {
				System.err.println("❌ ERROR al buscar HS: " + e.getMessage());
				System.out.println("Se validará solo con monto " + codigoProducto + ": $" + montoArchivo);
			}
			System.out.println("========================================");
		}
		
		List<DetallePrestamo> cuotasEncontradas = new ArrayList<>();
		boolean algunaCuotaConFechaDiferente = false;
		
		// VALIDACIÓN ESPECIAL: Si el monto descontado es 0, significa que NO se realizó el pago
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
			// Buscar si existe una cuota pendiente para este partícipe
			boolean cuotaEncontrada = false;
			for (Prestamo prestamo : prestamos) {
				try {
					List<DetallePrestamo> cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
						prestamo.getCodigo(),
						cargaArchivo.getMesAfectacion().intValue(),
						cargaArchivo.getAnioAfectacion().intValue()
					);
					
					if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
						for (DetallePrestamo cuota : cuotasDelMes) {
							Long estadoCuota = cuota.getEstado();
							if (estadoCuota != null && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
								cuotaEncontrada = true;
								registrarNovedad(participe, ASPNovedadesCargaArchivo.SIN_DESCUENTOS,
									"Error al buscar cuota #" + cuota.getNumeroCuota() + ". La cuota pasará a mora",
									prestamo.getProducto().getCodigo(),
									prestamo.getCodigo(),
									totalBaseCuota(cuota),
									0.0);
								return;
							}
						}
					}
				} catch (Throwable e) {
					System.err.println("Error al buscar cuotas sin pago para préstamo " + prestamo.getCodigo() + ": " + e.getMessage());
				}
			}
			
			if (!cuotaEncontrada) {
				Long codigoProductoDB = (!prestamos.isEmpty()) ? prestamos.get(0).getProducto().getCodigo() : null;
				Long codigoPrestamoDB = (!prestamos.isEmpty()) ? prestamos.get(0).getCodigo() : null;
				// 2026-08-31: antes se guardaba montoEsperado=null acá, así que registrarNovedad
				// nunca armaba montoDiferencia — el caso más puro de "no se cobró nada, va a
				// mora" quedaba clasificado INFORMATIVA por FamiliaNovedadCarga (sin dato para
				// decir lo contrario) en vez de COBRANZA.
				//
				// ⚠️ No se puede reusar la consulta del bucle de arriba (selectByPrestamoYMesAnio
				// + filtro de estado): se llega a esta rama JUSTAMENTE cuando esa consulta no
				// encontró ninguna cuota pendiente para NINGÚN préstamo — repetirla daría $0
				// siempre, un no-op que no cambia nada. La fuente tiene que ser independiente de
				// ese resultado vacío: Prestamo.valorCuota (el monto nominal de la cuota
				// recurrente del préstamo), sumado sobre los préstamos del partícipe — no
				// depende de que exista un DetallePrestamo para este mes/año exacto.
				double montoEsperadoPendiente = 0.0;
				for (Prestamo prestamo : prestamos) {
					montoEsperadoPendiente += nullSafe(prestamo.getValorCuota());
				}
				registrarNovedad(participe, ASPNovedadesCargaArchivo.SIN_DESCUENTOS,
					"No se realizó ningún descuento para este partícipe. Las cuotas pendientes pasarán a mora",
					codigoProductoDB,
					codigoPrestamoDB,
					montoEsperadoPendiente > 0.0 ? Double.valueOf(montoEsperadoPendiente) : null,
					0.0);
				return;
			}
		}
		
		// PASO 1: Buscar UNA cuota que coincida EXACTAMENTE
		for (Prestamo prestamo : prestamos) {
			DetallePrestamo cuotaDelPrestamo = null;
			boolean cuotaConFechaDiferente = false;
			
			List<DetallePrestamo> cuotasDelMes = null;
			try {
				cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
					prestamo.getCodigo(),
					cargaArchivo.getMesAfectacion().intValue(),
					cargaArchivo.getAnioAfectacion().intValue()
				);
			} catch (Throwable e) {
				System.err.println("Error al buscar cuotas del mes para préstamo " + prestamo.getCodigo() + ": " + e.getMessage());
				continue;
			}
			
			if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
				for (DetallePrestamo cuotaTemp : cuotasDelMes) {
					Long estadoCuota = cuotaTemp.getEstado();
					if (estadoCuota != null && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
						cuotaDelPrestamo = cuotaTemp;
						break;
					}
				}
			}
			
			// Si no se encontró cuota del mes, buscar la MÍNIMA cuota pendiente
			if (cuotaDelPrestamo == null) {
				Long estadoPrestamo = prestamo.getIdEstado();
				boolean prestamoEnEstadoValido = (estadoPrestamo != null && (
					estadoPrestamo == com.saa.rubros.EstadoPrestamo.GENERADO ||
					estadoPrestamo == com.saa.rubros.EstadoPrestamo.VIGENTE ||
					estadoPrestamo == com.saa.rubros.EstadoPrestamo.DE_PLAZO_VENCIDO ||
					estadoPrestamo == com.saa.rubros.EstadoPrestamo.EN_MORA
				));
				
				if (prestamoEnEstadoValido) {
					List<DetallePrestamo> todasLasCuotas = null;
					try {
						todasLasCuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
					} catch (Throwable e) {
						System.err.println("Error al buscar todas las cuotas para préstamo " + prestamo.getCodigo() + ": " + e.getMessage());
					}
					
					if (todasLasCuotas != null && !todasLasCuotas.isEmpty()) {
						DetallePrestamo cuotaMinima = null;
						
						for (DetallePrestamo cuotaTemp : todasLasCuotas) {
							Long estadoCuota = cuotaTemp.getEstado();
							
							if (estadoCuota != null && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
								
								if (cuotaMinima == null || cuotaTemp.getNumeroCuota() < cuotaMinima.getNumeroCuota()) {
									cuotaMinima = cuotaTemp;
								}
							}
						}
						
						if (cuotaMinima != null) {
							cuotaDelPrestamo = cuotaMinima;
							cuotaConFechaDiferente = true;
							algunaCuotaConFechaDiferente = true;
						}
					}
				}
			}
			
			// Si se encontró una cuota, validar si coincide
			if (cuotaDelPrestamo != null) {
				double montoCuota = totalBaseCuota(cuotaDelPrestamo);
				double diferencia = Math.abs(montoCuota - montoArchivo);
				
				if (diferencia <= TOLERANCIA) {
					cuotasEncontradas.clear();
					cuotasEncontradas.add(cuotaDelPrestamo);
					
					if (diferencia > 0.01 && diferencia <= TOLERANCIA) {
						double montoCuotaEncontrada = totalBaseCuota(cuotaDelPrestamo);
						String descripcion = String.format("Diferencia menor a $1 - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
							montoCuotaEncontrada, montoArchivo, diferencia);
						registrarNovedad(participe, ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR,
							descripcion,
							cuotaDelPrestamo.getPrestamo().getProducto().getCodigo(), 
							cuotaDelPrestamo.getPrestamo().getCodigo(), 
							montoCuotaEncontrada, montoArchivo);
					}
					
					if (cuotaConFechaDiferente) {
						double montoCuotaEncontrada = totalBaseCuota(cuotaDelPrestamo);
						registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE,
							"Cuota #" + cuotaDelPrestamo.getNumeroCuota() + " encontrada con fecha diferente al archivo",
							cuotaDelPrestamo.getPrestamo().getProducto().getCodigo(), 
							cuotaDelPrestamo.getPrestamo().getCodigo(), 
							montoCuotaEncontrada, montoArchivo);
					}
					
					return;
				}
				
				cuotasEncontradas.add(cuotaDelPrestamo);
			}
		}
		
		// PASO 2: Si no hubo coincidencia exacta
		if (cuotasEncontradas.isEmpty()) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA,
				"No se encontró cuota pendiente que coincida con el monto del archivo",
				null, null, null, montoArchivo);
			return;
		}
		
		// PASO 3: Validar suma de cuotas vs archivo
		double montoEsperadoTotal = 0.0;
		
		for (DetallePrestamo cuota : cuotasEncontradas) {
			montoEsperadoTotal += totalBaseCuota(cuota);
		}
		
		double diferenciaTotal = Math.abs(montoEsperadoTotal - montoArchivo);
		
		if (diferenciaTotal > TOLERANCIA) {
			String descripcion = String.format("Monto inconsistente - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
				montoEsperadoTotal, montoArchivo, diferenciaTotal);
			Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
			Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
			registrarNovedad(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE,
				descripcion, codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
			return;
		}
		
		if (diferenciaTotal > 0.01 && diferenciaTotal <= TOLERANCIA) {
			String descripcion = String.format("Diferencia menor a $1 - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
				montoEsperadoTotal, montoArchivo, diferenciaTotal);
			Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
			Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
			registrarNovedad(participe, ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR,
				descripcion, codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
		}
		
		if (algunaCuotaConFechaDiferente) {
			Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
			Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
			registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE,
				"Al menos una cuota encontrada tiene fecha diferente al mes/año del archivo",
				codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
		}
		
		return;
		
	} catch (Exception e) {
		System.err.println("❌ ERROR en validación FASE 2 para partícipe " + 
		                   participe.getCodigoPetro() + ": " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Valida el producto AH (Aportes) en FASE 2
 * Verifica que los valores del archivo correspondan con lo esperado en HistorialSueldo
 * 
 * @param participe Partícipe del archivo con producto AH
 * @param cargaArchivo Información de la carga
 */
private void validarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo) {
	try {
		// Obtener el código Petro del partícipe
		Long rolPetro = participe.getCodigoPetro();
		if (rolPetro == null) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
				"El partícipe no tiene código Petro válido", null, null, null, null);
			return;
		}
		
		// Buscar la entidad
		List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(rolPetro);
		if (entidades == null || entidades.isEmpty()) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
				"No se encontró entidad con código Petro: " + rolPetro, null, null, null, null);
			return;
		}
		
		Entidad entidad = entidades.get(0);
		
		// ✅ OPTIMIZADO: Buscar HistorialSueldo activo (estado 99) directamente
		// Antes: Traía TODAS los historiales y verificaba en Java si había más de 1
		// Ahora: Usa método optimizado que trae solo el activo
		com.saa.model.crd.HistorialSueldo historial = 
			historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());
		
		if (historial == null) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
				"No existe HistorialSueldo activo (estado 99) para la entidad", null, null, null, null);
			return;
		}
		
		// Validar que los campos obligatorios vengan del frontend
		if (cargaArchivo.getFilial() == null) {
			throw new RuntimeException("El campo 'filial' es obligatorio y debe ser enviado desde el frontend");
		}
		
		if (cargaArchivo.getUsuarioCarga() == null) {
			throw new RuntimeException("El campo 'usuarioCarga' es obligatorio y debe ser enviado desde el frontend");
		}
		
		// ✅ CORRECCIÓN: Obtener valores de jubilación y cesantía del historial
		double montoJubilacion = nullSafe(historial.getMontoJubilacion());
		double montoCesantia = nullSafe(historial.getMontoCesantia());
		
		// Calcular el monto esperado total (Jubilación + Cesantía)
		double montoEsperado = montoJubilacion + montoCesantia;
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		
		// Validar si el monto del archivo es 0
		if (montoArchivo == 0.0) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.APORTE_VALORES_CERO,
				"No se realizó descuento de aportes (monto $0)", null, null, montoEsperado, 0.0);
			return;
		}
		
		// Comparar el monto del archivo con el esperado
		double diferencia = Math.abs(montoEsperado - montoArchivo);
		
		if (diferencia > TOLERANCIA) {
			String descripcion = String.format("Monto de aportes inconsistente - Esperado: $%.2f (Jub:$%.2f + Ces:$%.2f), Archivo: $%.2f, Diferencia: $%.2f",
				montoEsperado, montoJubilacion, montoCesantia, montoArchivo, diferencia);
			registrarNovedad(participe, ASPNovedadesCargaArchivo.APORTE_MONTO_INCONSISTENTE,
				descripcion, null, null, montoEsperado, montoArchivo);
			return;
		}
		
		// Si la diferencia es pequeña pero existe
		if (diferencia > 0.01 && diferencia <= TOLERANCIA) {
			String descripcion = String.format("Diferencia menor a $1 en aportes - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
				montoEsperado, montoArchivo, diferencia);
			registrarNovedad(participe, ASPNovedadesCargaArchivo.APORTE_DIFERENCIA_MENOR_UN_DOLAR,
				descripcion, null, null, montoEsperado, montoArchivo);
		}
		
		// Validación OK - Los aportes se generarán en la fase de procesamiento
		System.out.println("  ✅ Validación AH OK - Esperado: $" + montoEsperado + ", Archivo: $" + montoArchivo);
		
	} catch (Throwable e) {
		System.err.println("❌ ERROR en validación AH para partícipe " + 
		                   participe.getCodigoPetro() + ": " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Clase interna para almacenar los saldos reales de una cuota
 */
private static class SaldosRealesCuota {
	double saldoDesgravamen = 0.0;
	double saldoInteres = 0.0;
	double saldoCapital = 0.0;
	double saldoSeguroIncendio = 0.0;  // ✅ AGREGADO: Seguro de incendio para PH/PP
	// CORRECCIONES-2026-09-02.md §2: mora e interés vencido, mismo criterio que
	// MotorPagoPrestamoServiceImpl.calcularSaldosCuota — antes esta clase los excluía a
	// propósito (ver totalBaseCuota) y su autocorrección podía marcar PAGADA una cuota con
	// mora real pendiente, dejándola huérfana para siempre (el motor y esta clase filtran
	// por estado != PAGADA).
	double saldoMora = 0.0;
	double saldoInteresVencido = 0.0;
	double totalPendiente = 0.0;
}

/**
 * Calcula los saldos reales de una cuota consultando la tabla PagoPrestamo
 * ✅ OPTIMIZACIÓN: Usa método específico en lugar de selectAll()
 * ✅ ACUMULA pagos de múltiples registros en PagoPrestamo
 */
private SaldosRealesCuota calcularSaldosRealesCuota(DetallePrestamo cuota, CargaArchivo cargaArchivo) throws Throwable {
	SaldosRealesCuota saldos = new SaldosRealesCuota();

	// TODO O NADA (2026-08-29): antes, cualquier falla acá (típicamente en el
	// detallePrestamoService.saveSingle de la autocorrección de más abajo) caía a usar los
	// valores ORIGINALES de la cuota, como si nunca se hubiera pagado nada. Si la cuota ya
	// tenía pagos parciales reales en CRD.PGPR, esto la trataba como si debiera el 100% de
	// nuevo — riesgo de re-cobrar una cuota ya pagada en parte. selectByIdDetallePrestamo NO
	// necesita este try: es una consulta que absorbe sus propios errores y devuelve lista
	// vacía (convención de DAO, sin cambios) — nunca lanza. Lo único que puede fallar de
	// verdad acá es el saveSingle de la autocorrección, y eso debe abortar, no disimularse.
	try {
		// Obtener pagos específicos de esta cuota usando método específico del DAO.
		// C4 (2026-10-28, REPROCESO-CARGA-PETRO.md §4.11): SOLO pagos VIGENTES (PGPRANUL nulo o 0),
		// mismo criterio que el motor (MotorPagoPrestamoServiceImpl.calcularSaldosCuota) y que
		// recalcularCuotaDesdePagos. Antes sumaba también los anulados: una cuota cuyo único pago se
		// anuló salía con saldo 0 y la "autocorrección" de abajo la volvía a marcar PAGADA (en el
		// reproceso de la 352, 114 filas perdieron su pago así). Un pago anulado nunca salda nada.
		// selectByIdDetallePrestamo (todos, con anulados) queda para quien sí los necesita: los
		// chequeos de FK antes de borrar cuotas (abono a capital y su reverso).
		List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
		if (pagos == null) {
			pagos = new ArrayList<>();
		}
		
		if (pagos.isEmpty()) {
			// No hay pagos previos, los saldos son los valores originales de la cuota
			saldos.saldoDesgravamen = nullSafe(cuota.getDesgravamen());
			saldos.saldoInteres = nullSafe(cuota.getInteres());
			saldos.saldoCapital = nullSafe(cuota.getCapital());
			saldos.saldoSeguroIncendio = nullSafe(cuota.getValorSeguroIncendio());  // ✅ AGREGADO
			// CORRECCIONES-2026-09-02.md §2: DTPRTTLL (cuota.getTotal()) YA INCLUYE la mora
			// acumulada por el proceso diario (ProcesoMoraPrestamoService) — igual criterio que
			// MotorPagoPrestamoServiceImpl.calcularSaldosCuota. Solo se agrega el interés
			// vencido aparte (hoy siempre 0, ningún proceso lo alimenta todavía).
			saldos.saldoMora = nullSafe(cuota.getMora());
			saldos.saldoInteresVencido = nullSafe(cuota.getInteresVencido());
			saldos.totalPendiente = cuota.getTotal() != null
				? nullSafe(cuota.getTotal()) + saldos.saldoInteresVencido
				: totalBaseCuota(cuota) + saldos.saldoMora + saldos.saldoInteresVencido;
			return saldos;
		}

		// Sumar todos los pagos realizados previamente
		double desgravamenPagadoTotal = 0.0;
		double interesPagadoTotal = 0.0;
		double capitalPagadoTotal = 0.0;
		double seguroIncendioPagadoTotal = 0.0;  // ✅ AGREGADO
		double moraPagadaTotal = 0.0;  // CORRECCIONES-2026-09-02.md §2
		double interesVencidoPagadoTotal = 0.0;  // CORRECCIONES-2026-09-02.md §2

		for (PagoPrestamo pago : pagos) {
			desgravamenPagadoTotal += nullSafe(pago.getDesgravamen());
			interesPagadoTotal += nullSafe(pago.getInteresPagado());
			capitalPagadoTotal += nullSafe(pago.getCapitalPagado());
			seguroIncendioPagadoTotal += nullSafe(pago.getValorSeguroIncendio());  // ✅ AGREGADO
			moraPagadaTotal += nullSafe(pago.getMoraPagada());
			interesVencidoPagadoTotal += nullSafe(pago.getInteresVencidoPagado());
		}

		// Calcular saldos pendientes
		saldos.saldoDesgravamen = Math.max(0, nullSafe(cuota.getDesgravamen()) - desgravamenPagadoTotal);
		saldos.saldoInteres = Math.max(0, nullSafe(cuota.getInteres()) - interesPagadoTotal);
		saldos.saldoCapital = Math.max(0, nullSafe(cuota.getCapital()) - capitalPagadoTotal);
		saldos.saldoSeguroIncendio = Math.max(0, nullSafe(cuota.getValorSeguroIncendio()) - seguroIncendioPagadoTotal);  // ✅ AGREGADO
		// CORRECCIONES-2026-09-02.md §2: mismo criterio que el motor — una cuota con mora o
		// interés vencido pendiente NO está pagada, aunque el resto del desglose sí lo esté.
		saldos.saldoMora = Math.max(0, nullSafe(cuota.getMora()) - moraPagadaTotal);
		saldos.saldoInteresVencido = Math.max(0, nullSafe(cuota.getInteresVencido()) - interesVencidoPagadoTotal);
		saldos.totalPendiente = saldos.saldoDesgravamen + saldos.saldoInteres + saldos.saldoCapital
			+ saldos.saldoSeguroIncendio + saldos.saldoMora + saldos.saldoInteresVencido;
		
		// ✅ Si el saldo total es 0 pero el estado no es PAGADA, actualizarlo
		if (saldos.totalPendiente <= 0.01 && cuota.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
			System.out.println("    ⚠️ Cuota #" + cuota.getNumeroCuota() + " completada según PagoPrestamo - Actualizando estado a PAGADA");
			cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA); // ✅ CRÍTICO: Actualizar también idEstado
			
			// ✅ Solo establecer fechaPagado si no existe. 2026-09-02: último día del mes de
			// carga, no la fecha de proceso — mismo criterio que crearRegistroPago.
			if (cuota.getFechaPagado() == null) {
				if (cargaArchivo.getMesAfectacion() == null || cargaArchivo.getAnioAfectacion() == null) {
					throw new IncomeException("No se puede fechar el pago de la cuota #"
						+ cuota.getNumeroCuota() + ": falta el mes o año de afectación de la carga "
						+ cargaArchivo.getCodigo() + ".");
				}
				cuota.setFechaPagado(fechaService.ultimoDiaMesAnioLocal(
					cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()).atTime(23, 59, 59));
				System.out.println("    ✅ Fecha de pago establecida");
			} else {
				System.out.println("    ℹ️ Respetando fecha de pago existente: " + cuota.getFechaPagado());
			}
			
			cuota.setCapitalPagado(capitalPagadoTotal);
			cuota.setInteresPagado(interesPagadoTotal);
			cuota.setDesgravamenPagado(desgravamenPagadoTotal);
			// ✅ CORRECCIÓN CRÍTICA: saldoCapital = saldoInicialCapital - capitalPagado (NO poner en 0)
			cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - capitalPagadoTotal));
			cuota.setSaldoInteres(0.0);
			// CORRECCIONES-2026-09-02.md §2: persistir también lo pagado de mora e interés
			// vencido — llegar acá significa que saldos.totalPendiente (que ya los incluye)
			// dio ≤ 0.01, así que también están saldados.
			cuota.setMoraPagado(moraPagadaTotal);
			cuota.setInteresVendidoPagado(interesVencidoPagadoTotal);
			cuota.setSaldoMora(0.0);
			cuota.setSaldoInteresVencido(0.0);

			// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
			double totalCuotaRecalculo = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) +
			                             nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio())
			                             + nullSafe(cuota.getMora()) + nullSafe(cuota.getInteresVencido());
			double totalPagadoCuotaRecalculo = capitalPagadoTotal + interesPagadoTotal +
			                                   desgravamenPagadoTotal + seguroIncendioPagadoTotal
			                                   + moraPagadaTotal + interesVencidoPagadoTotal;
			cuota.setSaldo(Math.max(0, totalCuotaRecalculo - totalPagadoCuotaRecalculo));
			
			detallePrestamoService.saveSingle(cuota);
		}
		
	} catch (Throwable e) {
		throw new RuntimeException("Falló al calcular los saldos reales de la cuota #"
			+ cuota.getNumeroCuota() + " (código " + cuota.getCodigo() + ") del préstamo "
			+ (cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null)
			+ ": " + e.getMessage(), e);
	}

	return saldos;
}

/**
 * Valida que solo se pueda procesar una carga si es del mes siguiente al último mes procesado
 * ✅ OPTIMIZACIÓN: Usa consulta directa para obtener la última carga sin traer todas
 */
private void validarOrdenProcesamiento(CargaArchivo cargaArchivo) throws Throwable {
	// ✅ OPTIMIZACIÓN: Obtener directamente la última carga procesada (MAX año/mes)
	// Antes: Traía TODAS las cargas procesadas y buscaba el MAX en Java
	// Ahora: Una consulta que trae solo la última carga
	CargaArchivo ultimaCargaProcesada = cargaArchivoDaoService.selectUltimaCargaProcesada(3L);
	
	// Excluir la carga actual si es la que se está procesando
	if (ultimaCargaProcesada != null && ultimaCargaProcesada.getCodigo().equals(cargaArchivo.getCodigo())) {
		ultimaCargaProcesada = null; // No validar contra sí misma
	}
	
	if (ultimaCargaProcesada != null) {
		int mesUltimoProcesado = ultimaCargaProcesada.getMesAfectacion().intValue();
		int anioUltimoProcesado = ultimaCargaProcesada.getAnioAfectacion().intValue();
		int mesActual = cargaArchivo.getMesAfectacion().intValue();
		int anioActual = cargaArchivo.getAnioAfectacion().intValue();
		
		// Calcular el mes siguiente esperado
		int mesSiguiente = mesUltimoProcesado + 1;
		int anioSiguiente = anioUltimoProcesado;
		if (mesSiguiente > 12) {
			mesSiguiente = 1;
			anioSiguiente++;
		}
		
		if (anioActual != anioSiguiente || mesActual != mesSiguiente) {
			throw new RuntimeException(String.format(
				"No se puede procesar la carga del mes %d/%d. " +
				"La última carga procesada fue %d/%d. " +
				"Solo se puede procesar la carga del mes %d/%d (siguiente mes consecutivo).",
				mesActual, anioActual,
				mesUltimoProcesado, anioUltimoProcesado,
				mesSiguiente, anioSiguiente
			));
		}
	}
}

/**
 * Salida de la mora por pago recibido.
 *
 * Se invoca cuando, al procesar el producto AH, llega un descuento con valor.
 * Si la entidad estaba en ACTIVO EN MORA vuelve a ACTIVO.
 *
 * Basta con que llegue un pago: no se exige que cubra la deuda completa. La
 * generación del archivo ya envía a cobrar todos los meses adeudados, así que
 * lo normal es que el pago venga completo; si viniera parcial, el partícipe
 * queda ACTIVO y el próximo periodo sin aporte lo vuelve a evaluar.
 *
 * @param entidad       Entidad del partícipe con descuento en esta carga
 * @param montoRecibido Valor descontado, solo para dejarlo en el log
 */
private void restaurarActivoPorPago(Entidad entidad, double montoRecibido) {
	try {
		if (entidad == null) {
			return;
		}

		Long estadoActual = entidad.getIdEstado();
		if (estadoActual == null || estadoActual != EstadoParticipeEntidad.ACTIVO_EN_MORA) {
			return;
		}

		entidad.setIdEstado((long) EstadoParticipeEntidad.ACTIVO);
		entidadDaoService.save(entidad, entidad.getCodigo());

		System.out.println("   [MORA] Entidad " + entidad.getCodigo()
			+ " (rol " + entidad.getRolPetroComercial() + ") vuelve a ACTIVO: "
			+ "se recibió pago de $" + montoRecibido + ".");

	} catch (Throwable e) {
		// Igual que la marca de mora, es un efecto secundario del proceso de
		// aportes: si falla, se registra pero no se aborta la carga.
		System.err.println("Error al restaurar estado ACTIVO por pago para entidad "
			+ (entidad != null ? entidad.getCodigo() : null) + ": " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Regla de mora por falta de aporte.
 *
 * Se invoca cuando, al procesar el producto AH, un partícipe llega sin
 * descuento (valor 0 o nulo). Revisa si en el periodo inmediatamente anterior
 * tampoco registró aporte; si es así son dos meses consecutivos sin aportar y
 * la entidad pasa a ACTIVO EN MORA.
 *
 * El periodo anterior se evalúa contra CRD.APRT (aportes generados, tipos 9 y
 * 11 con valor positivo), que es exactamente la base que usa el padrón de
 * partícipes. Así el proceso y el reporte nunca se contradicen.
 *
 * Condiciones para marcar la mora:
 *  - La entidad debe estar hoy en estado ACTIVO. No se tocan cesantes,
 *    jubilados, desafiliados ni las que ya están en mora.
 *  - El periodo anterior debe existir y haber sido cargado con producto AH.
 *    Si todavía no se ha cargado, no se puede afirmar que no aportó.
 *  - La consulta del periodo anterior debe haber podido evaluarse. Ante un
 *    fallo de BD no se marca mora.
 *
 * El proceso nunca revierte el estado: si el partícipe vuelve a aportar, la
 * salida de ACTIVO EN MORA es una decisión administrativa, no automática.
 *
 * @param entidad       Entidad del partícipe sin descuento en esta carga
 * @param cargaArchivo  Carga que se está procesando
 */
private void evaluarMoraPorFaltaDeAporte(Entidad entidad, CargaArchivo cargaArchivo) {
	try {
		if (entidad == null || cargaArchivo == null) {
			return;
		}

		// Solo aplica a partícipes activos.
		Long estadoActual = entidad.getIdEstado();
		if (estadoActual == null || estadoActual != EstadoParticipeEntidad.ACTIVO) {
			System.out.println("   [MORA] Entidad " + entidad.getCodigo()
				+ " no está en estado ACTIVO (estado=" + estadoActual + "). No se evalúa.");
			return;
		}

		Long anioActual = cargaArchivo.getAnioAfectacion();
		Long mesActual  = cargaArchivo.getMesAfectacion();
		if (anioActual == null || mesActual == null) {
			System.out.println("   [MORA] La carga no tiene periodo de afectación definido. No se evalúa.");
			return;
		}

		// Periodo inmediatamente anterior
		Long mesAnterior  = (mesActual == 1L) ? 12L : mesActual - 1L;
		Long anioAnterior = (mesActual == 1L) ? anioActual - 1L : anioActual;

		// Si ese periodo nunca se cargó, no se puede afirmar que no aportó.
		boolean periodoAnteriorCargado = participeXCargaArchivoDaoService
			.existeCargaConProductoEnPeriodo(CODIGO_PRODUCTO_APORTES, anioAnterior, mesAnterior);
		if (!periodoAnteriorCargado) {
			System.out.println("   [MORA] El periodo " + mesAnterior + "/" + anioAnterior
				+ " no tiene carga de " + CODIGO_PRODUCTO_APORTES + ". No se evalúa.");
			return;
		}

		// Se mide contra los aportes efectivamente generados (CRD.APRT), que es la
		// misma base del padrón de partícipes. Si se midiera contra el descuento
		// del archivo, ambos podrían contradecirse: hay casos con descuento en el
		// archivo que no generan aporte (sin HistorialSueldo activo, o con
		// jubilación y cesantía en $0).
		Double aportadoAnterior = aporteDaoService.sumaAportesPositivosPorTipoYPeriodo(
			entidad.getCodigo(),
			java.util.Arrays.asList(TIPO_APORTE_JUBILACION, TIPO_APORTE_CESANTIA),
			anioAnterior, mesAnterior);

		if (aportadoAnterior == null) {
			System.out.println("   [MORA] No se pudo consultar el periodo anterior. No se marca mora.");
			return;
		}

		if (aportadoAnterior > 0.01) {
			System.out.println("   [MORA] El partícipe sí aportó en " + mesAnterior + "/" + anioAnterior
				+ " ($" + aportadoAnterior + "). Solo un periodo sin aporte, no se marca mora.");
			return;
		}

		// Dos periodos consecutivos sin aporte.
		entidad.setIdEstado((long) EstadoParticipeEntidad.ACTIVO_EN_MORA);
		entidadDaoService.save(entidad, entidad.getCodigo());

		System.out.println("   [MORA] Entidad " + entidad.getCodigo()
			+ " (rol " + entidad.getRolPetroComercial() + ") pasa a ACTIVO EN MORA: "
			+ "sin aporte en " + mesAnterior + "/" + anioAnterior
			+ " ni en " + mesActual + "/" + anioActual + ".");

	} catch (Throwable e) {
		// La mora es un efecto secundario del procesamiento de aportes:
		// un fallo aquí no debe abortar la carga completa.
		System.err.println("Error al evaluar mora por falta de aporte para entidad "
			+ (entidad != null ? entidad.getCodigo() : null) + ": " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Aplica aportes para el producto AH (aportes de jubilación y cesantía)
 * ✅ NUEVA LÓGICA: Similar a procesamiento de préstamos con estados, acumulación y excedentes
 */
private int aplicarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo,
		Map<String, Double> esperadoEnLote, java.util.Set<Long> entidadesCubiertas) throws Throwable {
	int aportesCreados = 0;

	// TODO O NADA (2026-08-29): antes, si distribuirAportePorDevengo fallaba a mitad de su
	// propio bucle (después de haber guardado ya algunos Aporte con crearNuevoAporte), esto se
	// tragaba el error y devolvía `aportesCreados` tal como quedó ANTES de esa asignación
	// (típicamente 0) — el resumen de aplicarPagosArchivoPetro ni contaba éxito ni contaba
	// error, como si no hubiera pasado nada, mientras algunos Aporte ya habían quedado
	// grabados a medias. Ya no hay catch general acá: cualquier fallo real propaga tal cual,
	// con el contexto que ya agrega cada método interno (crearNuevoAporte, crearRegistroPagoAporte).
	//
	// REVISADO 2026-08-31 (decisión final del usuario, tres idas y vueltas el mismo día —
	// esta es la que queda). La frase original de este comentario decía que las cuatro
	// ausencias de dato de más abajo ("sin entidad", "monto $0", "sin HistorialSueldo activo",
	// "ningún aporte con valor") eran todas inocuas y no abortaban. Se reclasificaron en tres
	// grupos, según si se sabe a quién pertenece el dinero y si esa persona debe aportar:
	//   - ABORTA (dato faltante, no se sabe a quién pertenece o si debe aportar): "sin
	//     entidad" — ni siquiera hay a quién buscar en el sistema.
	//   - NOVEDAD (se sabe quién es, falta decidir la cuenta — igual que el excedente Petro a
	//     un aporte, mismo mecanismo: registrarNovedad + AVPC + pantalla de decisión): "sin
	//     HistorialSueldo activo" y "HistorialSueldo con esperado en $0".
	//   - Sigue sin tocar ("monto $0" — no hay plata que perder, nunca fue parte de esto).
	// Ver también el caso "contrato activo sin vigencia que cubra el mes" en
	// distribuirAportePorDevengo (ADVERTENCIA, no aborta) — los cinco casos y su tratamiento
	// están documentados juntos en REGLAS-CARGA-PETRO.md §3.6.
	System.out.println("========================================");
	System.out.println("PROCESANDO APORTES (AH) - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
	System.out.println("========================================");

	// Buscar la entidad. Sin entidad = no se sabe a quién pertenece el dinero: nada que
	// decidir en pantalla, aborta toda la carga (decisión del usuario, 2026-08-31).
	List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
	double montoDescontadoSinEntidad = nullSafe(participe.getTotalDescontado());
	if ((entidades == null || entidades.isEmpty()) && modoReproceso) {
		// MODO REPROCESO (C4): el rol Petro no está en el SAA (medido en la carga 352: 14
		// filas, $4.872,04, 7 de ellas AH). Abortar una carga histórica por un partícipe que
		// no existe no tiene a quién pedirle nada: queda como novedad, no se aplica, y el
		// invariante recibido == aplicado lo descuenta del recibido.
		registrarSinParticipeReproceso(participe, CODIGO_PRODUCTO_APORTES, montoDescontadoSinEntidad, cargaArchivo);
		return 0;
	}
	if (entidades == null || entidades.isEmpty()) {
		throw new IncomeException("No se encontró la entidad del partícipe con código Petro "
			+ participe.getCodigoPetro() + " (nombre en el archivo: \"" + participe.getNombre()
			+ "\"), carga " + cargaArchivo.getCodigo() + ", monto descontado $" + montoDescontadoSinEntidad
			+ ": cree la entidad con ese código Petro y vuelva a procesar la carga.");
	}

	Entidad entidad = entidades.get(0);
	double montoRecibido = montoDescontadoSinEntidad;

	// MODO REPROCESO (D-010): el excedente de aporte que ASOPREP devolvió NO se anticipa a los
	// meses siguientes: va a un aporte EXCEDENTE_PETRO sin devengo y solo el resto entra a la
	// prelación. Fuera del modo reproceso devuelve 0.
	double excedenteDevuelto = aplicarExcedenteDevueltoReproceso(participe, CODIGO_PRODUCTO_APORTES, entidad, cargaArchivo);
	if (excedenteDevuelto > 0.0) {
		aportesCreados++;
		montoRecibido = Math.round((montoRecibido - excedenteDevuelto) * 100.0) / 100.0;
		if (montoRecibido <= 0.01) {
			return aportesCreados;
		}
	}

	if (montoRecibido <= 0.01) {
		System.out.println("⚠️ Monto recibido es $0 para partícipe: " + participe.getCodigoPetro());
		// Sin descuento este mes: revisar si tampoco lo hubo en el periodo
		// anterior. Dos periodos seguidos sin aportar => ACTIVO EN MORA.
		// MODO REPROCESO (C4): no se toca el estado del partícipe — el estado de hoy no se
		// decide con el aporte de un mes pasado; el historial de estados se reconstruye aparte.
		if (!modoReproceso) {
			evaluarMoraPorFaltaDeAporte(entidad, cargaArchivo);
		}
		return 0;
	}

	// Llegó pago: si venía en mora, se pone al día (no en modo reproceso, mismo motivo).
	if (!modoReproceso) {
		restaurarActivoPorPago(entidad, montoRecibido);
	}

	System.out.println("📥 Monto total recibido: $" + montoRecibido);

	// ✅ Buscar valores esperados en HistorialSueldo con estado 99
	com.saa.model.crd.HistorialSueldo historialActivo =
		historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());

	if (historialActivo == null) {
		// Se sabe quién es (hay Entidad), falta decidir la cuenta: NOVEDAD, no aborta —
		// mismo mecanismo que el excedente Petro a un aporte (aplicarAfectacionAAporte):
		// el operador decide en pantalla si el valor recibido va a jubilación o a cesantía,
		// y esa decisión se aplica vía AVPC (tipoAporte) en el reproceso. Este código de
		// novedad YA está en FamiliaNovedadCarga.TIPOS_QUE_EXIGEN_AFECTACION — solo faltaba que esta
		// ruta (Fase 2, con dinero real) la generara además de la validación de carga
		// (validarAporteAH, Fase 1) que ya la registra sin datos de monto.
		// §15: transacción PROPIA — generada DURANTE aplicarPagosArchivoPetro (vía aplicarAporteAH).
		registrarNovedadDeAplicacion(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
			"No se encontró HistorialSueldo activo (estado 99) para la entidad " + entidad.getCodigo()
				+ "; no se pudo determinar a qué tipo de aporte aplicar los $" + montoRecibido
				+ " recibidos. Requiere decisión en pantalla (jubilación o cesantía).",
			null, null, null, montoRecibido);
		novedadesGeneradasCargaActual.add("Partícipe " + participe.getCodigoPetro() + " ("
			+ participe.getNombre() + "): $" + montoRecibido + " sin aplicar — sin HistorialSueldo activo.");
		return 0;
	}

	double montoEsperadoJubilacion = nullSafe(historialActivo.getMontoJubilacion());
	double montoEsperadoCesantia = nullSafe(historialActivo.getMontoCesantia());
	double montoEsperadoTotal = montoEsperadoJubilacion + montoEsperadoCesantia;

	System.out.println("💰 Valores esperados (HistorialSueldo):");
	System.out.println("   - Jubilación: $" + montoEsperadoJubilacion);
	System.out.println("   - Cesantía: $" + montoEsperadoCesantia);
	System.out.println("   - TOTAL: $" + montoEsperadoTotal);

	// ✅ VALIDAR QUE AL MENOS UNO DE LOS APORTES TENGA VALOR
	boolean tieneJubilacion = montoEsperadoJubilacion > 0.0;
	boolean tieneCesantia = montoEsperadoCesantia > 0.0;

	System.out.println("🔍 Análisis de aportes activos:");
	System.out.println("   - Tiene Jubilación: " + tieneJubilacion);
	System.out.println("   - Tiene Cesantía: " + tieneCesantia);

	if (!tieneJubilacion && !tieneCesantia) {
		// Mismo criterio que arriba: se sabe quién es, falta decidir la cuenta -> NOVEDAD.
		// §15: transacción PROPIA — mismo motivo que la de arriba.
		registrarNovedadDeAplicacion(participe, ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
			"El HistorialSueldo activo (código " + historialActivo.getCodigo()
				+ ") tiene jubilación y cesantía esperadas en $0, pero se recibieron $" + montoRecibido
				+ ". Requiere decisión en pantalla (jubilación o cesantía).",
			null, null, Double.valueOf(montoEsperadoTotal), montoRecibido);
		novedadesGeneradasCargaActual.add("Partícipe " + participe.getCodigoPetro() + " ("
			+ participe.getNombre() + "): $" + montoRecibido + " sin aplicar — jubilación y cesantía esperadas en $0.");
		return 0;
	}

	try {
		// ✅ Fase 2 del plan de devengo de aportes: prelación por mes de devengo incompleto
		// más antiguo (§2.3), reemplaza la distinción único-tipo / alternado. Si un tipo no
		// tiene esperado (p. ej. cesantía = $0), su faltante mensual siempre es 0 y el reparto
		// nunca crea filas de ese tipo: no hace falta una rama aparte para "solo un tipo".
		aportesCreados += distribuirAportePorDevengo(entidad, montoRecibido, cargaArchivo, participe, esperadoEnLote,
			entidadesCubiertas);
	} catch (Throwable e) {
		throw new RuntimeException("Falló al distribuir el aporte del partícipe "
			+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), entidad "
			+ entidad.getCodigo() + ", monto recibido $" + montoRecibido + ": " + e.getMessage(), e);
	}

	System.out.println("✅ Procesamiento de aportes completado - Total aportes: " + aportesCreados);
	System.out.println("========================================\n");

	return aportesCreados;
}

private static final int TOPE_MESES_DEVENGO = 60;

/**
 * PISO del devengo (D11): junio 2025 en adelante. Es OBLIGATORIO, no un detalle de rango.
 * Todo lo anterior a esta fecha se queda con devengo NULL A PROPÓSITO (el backfill de
 * {@code 63_BACKFILL_DEVENGO_APORTES.sql} nunca lo va a llenar: ese es exactamente su
 * alcance declarado). Sin este piso, el bucle de {@link #distribuirAportePorDevengo}
 * retrocedería a 2024/2023 y vería incompletos TODOS esos meses para cualquier partícipe,
 * incluso con el backfill ya corrido — porque esos meses jamás van a tener devengo. Por
 * construcción, ningún mes candidato de ese método es nunca anterior a esta constante: el
 * cursor arranca aquí y sólo avanza hacia adelante ({@code plusMonths}), nunca hacia atrás.
 */
private static final java.time.LocalDate ALCANCE_MINIMO_DEVENGO = java.time.LocalDate.of(2025, 6, 1);

private static final List<Long> ORDEN_TIPOS_APORTE = Arrays.asList(TIPO_APORTE_JUBILACION, TIPO_APORTE_CESANTIA);

/**
 * Reparte lo recibido de un partícipe entre los meses de devengo incompletos, del más
 * antiguo al más nuevo, jubilación siempre antes que cesantía dentro del mismo mes (§2.3
 * del plan de devengo de aportes — reemplaza el FIFO por saldo pendiente de la Fase 1 y la
 * distinción único-tipo/alternado).
 *
 * Cada tramo aplicado CREA una fila nueva con su propio {@code periodoDevengo}: no se abona
 * a filas anteriores. Si el monto recibido excede lo que falta hasta el mes de la carga, el
 * sobrante se ANTICIPA a los meses siguientes (D4), sin tope salvo el de seguridad. El
 * cursor de mes nunca baja de {@link #ALCANCE_MINIMO_DEVENGO} (D11): ver su JavaDoc.
 *
 * <p><b>"aportado(m,tipo)" usa el PERIODO EFECTIVO, no el devengo a secas</b> (D3, corregido
 * el 2026-08-27 — ver {@code AporteDaoService.sumValorPorEntidadTipoYRangoDevengo}): un
 * aporte positivo sin devengo (histórico sin backfillear) cuenta por su mes de caja, pero un
 * movimiento negativo sin devengo (retiro de saldo, D5) NO cuenta para ningún mes. Con esto,
 * correr esta carga sin haber corrido antes {@code 63_BACKFILL_DEVENGO_APORTES.sql} sigue
 * sin ser lo ideal (el reparto no distingue meses realmente pendientes de meses ya pagados
 * pero sin backfillear, y podría anticipar de más), pero YA NO corrompe datos: no se le
 * cobra dos veces a nadie por un movimiento que en realidad era un retiro.</p>
 *
 * <p><b>Dinero recibido sin aplicar del todo (2026-08-31, decisión del usuario).</b> Si al
 * agotar {@link #TOPE_MESES_DEVENGO} meses queda disponible sin aplicar, la causa determina
 * si se aborta la carga completa: sin contrato ACTIVO es un dato faltante y ABORTA
 * ({@code IncomeException}); con contrato activo pero sin vigencia que cubra esos meses (o
 * con vigencia en $0) es un caso legítimo — el partícipe ya no debe aportar ese mes, p.ej. un
 * jubilado al que Petro le sigue descontando — y NO aborta, solo deja advertencia en el log
 * con el partícipe, el monto y el motivo. Ver el bloque final del método.</p>
 *
 * @return Cantidad de filas de Aporte creadas
 */
private int distribuirAportePorDevengo(Entidad entidad, double montoRecibido,
        CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe,
        Map<String, Double> esperadoEnLote, java.util.Set<Long> entidadesCubiertas) throws Throwable {

	// CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3.2: caché LOCAL a esta llamada (un
	// partícipe) — nunca global, nunca compartido entre partícipes. Si el lote no cubrió a
	// esta entidad, resuelve contrato+vigencias UNA sola vez acá abajo (en el primer
	// esperadoMensual que la necesite) y responde el resto de los meses/tipos desde memoria,
	// en vez de una consulta por mes×tipo.
	CacheVigenciaParticipe cacheVigencia = new CacheVigenciaParticipe();

	java.time.LocalDate mesCarga = java.time.LocalDate.of(
		cargaArchivo.getAnioAfectacion().intValue(), cargaArchivo.getMesAfectacion().intValue(), 1);

	// MODO REPROCESO (D-022, REPROCESO-CARGA-PETRO.md §4.10): si la entidad no tiene contrato
	// ACTIVO hoy, el contrato se resuelve a la fecha de corte. Con vigencias que cubren el mes, la
	// prelación de abajo usa ese contrato (cacheVigencia); sin vigencias, el AH va completo al mes
	// de la carga con el tipo del último aporte anterior y termina acá. Fuera del modo reproceso,
	// o con contrato activo hoy, no hace nada.
	if (modoReproceso) {
		Integer aplicadosD022 = resolverContratoAlCorteReproceso(entidad, montoRecibido, cargaArchivo,
			participe, cacheVigencia, mesCarga, esperadoEnLote);
		if (aplicadosD022 != null) {
			return aplicadosD022;
		}
	}

	// aportado(m,tipo) de todo el rango relevante, en UNA sola consulta.
	java.util.Map<String, Double> aportadoPorMesTipo = new java.util.HashMap<>();
	List<Object[]> filas = aporteDaoService.sumValorPorEntidadTipoYRangoDevengo(
		entidad.getCodigo(), ALCANCE_MINIMO_DEVENGO, mesCarga);
	if (filas != null) {
		for (Object[] fila : filas) {
			java.time.LocalDate periodo = (java.time.LocalDate) fila[0];
			Long idTipo = ((Number) fila[1]).longValue();
			Double suma = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
			aportadoPorMesTipo.put(claveMesTipo(periodo, idTipo), suma);
		}
	}

	// 1. Localizar el primer mes incompleto posterior al último mes completo.
	java.time.LocalDate mes = ALCANCE_MINIMO_DEVENGO;
	java.time.LocalDate inicio = null;
	int guard = 0;
	while (!mes.isAfter(mesCarga) && guard < TOPE_MESES_DEVENGO) {
		guard++;
		boolean incompleto = false;
		for (Long idTipo : ORDEN_TIPOS_APORTE) {
			double esperado = esperadoMensual(entidad, idTipo, mes, esperadoEnLote, entidadesCubiertas, cacheVigencia);
			double aportado = aportadoPorMesTipo.getOrDefault(claveMesTipo(mes, idTipo), 0.0);
			if (esperado - aportado > 0.01) {
				incompleto = true;
				break;
			}
		}
		if (incompleto) {
			inicio = mes;
			break;
		}
		mes = mes.plusMonths(1);
	}
	if (inicio == null) {
		// Todo completo hasta el mes de la carga: el recibido se anticipa (D4).
		inicio = mesCarga;
	}

	// 2. Aplicar el disponible desde 'inicio' en adelante.
	double disponible = montoRecibido;
	int aportesCreados = 0;
	mes = inicio;
	guard = 0;
	while (disponible > 0.01 && guard < TOPE_MESES_DEVENGO) {
		guard++;
		for (Long idTipo : ORDEN_TIPOS_APORTE) {
			if (disponible <= 0.01) {
				break;
			}
			double esperado = esperadoMensual(entidad, idTipo, mes, esperadoEnLote, entidadesCubiertas, cacheVigencia);
			if (esperado <= 0.0) {
				continue;
			}
			String clave = claveMesTipo(mes, idTipo);
			double aportado = aportadoPorMesTipo.getOrDefault(clave, 0.0);
			double faltante = esperado - aportado;
			if (faltante > 0.01) {
				double aplicar = Math.min(disponible, faltante);
				String nombreTipo = TIPO_APORTE_JUBILACION.equals(idTipo) ? "Jubilación" : "Cesantía";
				System.out.println("   📍 Devengo " + mes + " - " + nombreTipo + ": $" + aplicar);
				Aporte nuevoAporte = crearNuevoAporte(entidad, idTipo, nombreTipo, aplicar, mes,
						cargaArchivo, com.saa.rubros.CrdTipoMovimientoAporte.APORTE_MENSUAL);
				crearRegistroPagoAporte(nuevoAporte, aplicar, cargaArchivo, participe);
				disponible -= aplicar;
				aportadoPorMesTipo.put(clave, aportado + aplicar);
				aportesCreados++;
			}
		}
		mes = mes.plusMonths(1);
	}

	if (guard >= TOPE_MESES_DEVENGO && disponible > 0.01) {
		// Decisión del usuario (2026-08-31): "si no se distribuye todo el dinero recibido,
		// no se debe permitir procesar la carga" — pero SOLO cuando la causa es que falta un
		// dato (sin contrato ACTIVO). "esperado = 0" tiene otras dos causas legítimas que NO
		// deben abortar la carga de 2.000 partícipes por uno solo: contrato activo pero sin
		// una VigenciaContrato que cubra ese mes, o con vigencia pero monto $0 — ambas
		// significan "este partícipe ya no debe aportar ese mes" (p.ej. un jubilado al que
		// Petro le sigue descontando: se corrige con devolución/afectación manual, no
		// cargando un dato faltante). La única forma de distinguirlas es preguntar por el
		// contrato EXPLÍCITAMENTE — nunca deducirlo de que esperadoMensual dio 0.
		Contrato contratoActivo = contratoDaoService.selectActivoPorEntidad(entidad.getCodigo());
		if (contratoActivo == null && modoReproceso) {
			// D-022: en reproceso vale el contrato vigente al corte (el mismo que usó la prelación).
			contratoActivo = contratoDaoService.selectVigenteAlCortePorEntidad(entidad.getCodigo(), finMesCarga(cargaArchivo));
		}
		if (contratoActivo == null) {
			throw new IncomeException("No se pudo aplicar $" + disponible + " de los $" + montoRecibido
				+ " recibidos del partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
				+ "), entidad " + entidad.getCodigo() + ", carga " + cargaArchivo.getCodigo()
				+ ": no tiene contrato ACTIVO. Créelo antes de procesar la carga.");
		}

		// MODO REPROCESO (D-022 1b, red de seguridad — REPROCESO-CARGA-PETRO.md §4.10): si el
		// reparto por devengo no pudo colocar el dinero en 60 meses, lo que sobra va completo al mes
		// de la carga con la regla de tipo de D-022, en vez de la novedad bloqueante. La detección
		// directa (resolverContratoAlCorteReproceso) debería haberlo atajado antes; esto cubre lo
		// que se le escape (p. ej. una vigencia que toca el mes pero no cubre su último día).
		if (modoReproceso) {
			List<com.saa.model.crd.VigenciaContrato> vigenciasRed = vigenciaContratoDaoService.selectByContrato(contratoActivo.getCodigo());
			String[] origenTipo = new String[1];
			Long idTipoRed = tipoAporteSinVigenciaDelMes(vigenciasRed != null ? vigenciasRed : new ArrayList<>(),
				mesCarga, entidad, origenTipo);
			double montoRed = Math.round(disponible * 100.0) / 100.0;
			com.saa.model.crd.TipoAporte tipoAporteRed = tipoAporteDaoService.selectById(idTipoRed, "TipoAporte");
			Aporte aporteRed = registrarAporteMesCargaD022(entidad, tipoAporteRed, montoRed, cargaArchivo, participe,
				mesCarga, MARCA_D022_SIN_VIGENCIA);
			montoAportesSinVigenciaDelMes += montoRed;
			aportesSinVigenciaDelMes.add("Entidad " + entidad.getCodigo() + " (rol " + participe.getCodigoPetro() + "): $"
				+ String.format("%,.2f", montoRed) + " a " + tipoAporteRed.getNombre() + " (" + origenTipo[0] + "; APRT "
				+ aporteRed.getCodigo() + ", contrato " + contratoActivo.getCodigo() + ", red de seguridad del tope de "
				+ TOPE_MESES_DEVENGO + " meses)");
			System.out.println("   D-022 1b: entidad " + entidad.getCodigo() + " (rol " + participe.getCodigoPetro()
				+ ") - red de seguridad: $" + montoRed + " a " + tipoAporteRed.getNombre() + " con devengo " + mesCarga
				+ " (" + origenTipo[0] + ")");
			return aportesCreados + 1;
		}

		// CORRECCIÓN 2026-09-03, VALIDACION-TOPE-AFECTACION-MANUAL.md §12, decisión del
		// usuario: "si hay dinero no repartido, debe decir exactamente qué dinero no se ha
		// repartido y permitirle al usuario repartir ese dinero dentro del partícipe que tenga
		// el sobrante". Antes esto era una ADVERTENCIA silenciosa (advertenciasVigenciaCargaActual)
		// y la plata quedaba sin aplicar sin más. Ahora es una novedad BLOQUEANTE — mismo
		// tratamiento que cuando se aplica de más (manejarExcedenteNoAplicado): las dos
		// direcciones del descuadre se tratan igual, si no cuadra no procesa y el operador
		// tiene dónde arreglarlo.
		//
		// montoRecibido = EL SOBRANTE EXACTO (no el total de aportes del partícipe) — es lo
		// que define el pozo que va a ver el operador en la pantalla de afectación
		// (disponibleParaTope lee este mismo campo). montoEsperado se deja null a propósito:
		// no hay un "esperado" que tenga sentido para un remanente que no encajó en ningún mes
		// dentro del tope — con null, FamiliaNovedadCarga.clasificar da BLOQUEANTE igual
		// (montoDiferencia null cuenta como "sobra, o no hay dato para decir lo contrario").
		//
		// ⚠️ CORREGIDO 2026-09-03 (§10, cuarta vuelta + §15): el comentario que estuvo acá decía
		// que no hacía falta cuidar duplicados porque disponibleParaTope resolvía por el MÁXIMO
		// — eso YA NO es cierto, §10 ahora SUMA (con dedup solo si codigoPrestamo es NO NULO, y
		// acá es null). Dos novedades de este tipo para esta fila SÍ duplicarían el pozo. La
		// protección real hoy es OTRA: este registro se hace vía
		// registradorNovedadTransaccionPropia (§15), que es IDEMPOTENTE — si esta fila ya tiene
		// una novedad de este tipo (p.ej. de un intento anterior que revirtió más adelante en la
		// red del §11), la ACTUALIZA en vez de crear una segunda.
		String descripcion = "Contrato activo (código " + contratoActivo.getCodigo()
			+ ") pero su vigencia no cubre uno o más de los " + TOPE_MESES_DEVENGO
			+ " meses evaluados (sin vigencia vigente, o vigencia con monto $0): quedaron $"
			+ disponible + " de los $" + montoRecibido + " recibidos sin poder aplicar."
			+ " Requiere distribución manual (AVPC) dentro de este partícipe.";
		System.err.println("⛔ NOVEDAD BLOQUEANTE: distribuirAportePorDevengo alcanzó el tope de "
			+ TOPE_MESES_DEVENGO + " meses sin poder aplicar todo el monto. Entidad " + entidad.getCodigo()
			+ " (" + participe.getCodigoPetro() + " - " + participe.getNombre() + ") - Carga "
			+ cargaArchivo.getCodigo() + " - Sobrante: $" + disponible + " - " + descripcion);
		// §15: transacción PROPIA — el defecto que lo motivó: esta novedad se generaba y la
		// transacción de aplicarPagosArchivoPetro se la llevaba puesta si la red del §11 revertía
		// después, dejando al operador sin dónde actuar.
		registrarNovedadDeAplicacion(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE, descripcion,
			null, null, null, Double.valueOf(disponible));
		novedadesGeneradasCargaActual.add("Partícipe " + participe.getCodigoPetro() + " ("
			+ participe.getNombre() + "): $" + disponible + " sin aplicar — contrato activo (código "
			+ contratoActivo.getCodigo() + ") pero su vigencia no cubre ese mes. Requiere afectación manual.");
	}

	System.out.println("   ✅ Total aportes creados: " + aportesCreados);
	return aportesCreados;
}

private String claveMesTipo(java.time.LocalDate mes, Long idTipo) {
	return mes + "|" + idTipo;
}

/**
 * "esperado(entidad, tipo, mes)" — PUNTO DE EXTENSIÓN ÚNICO (§2.3). Fase 3: ya no sale de
 * {@code HistorialSueldo}, sale de la vigencia de {@code CRD.VGCN} vigente al último día de
 * {@code mes} (0.0 si el contrato no tiene ninguna vigencia abierta ese mes). El
 * {@code HistorialSueldo} de HSTR queda como fuente sólo para la migración de 3.3, no para
 * el cobro corriente.
 *
 * <p><b>Resuelto en lote cuando se puede (2026-09-02, auditoría de rendimiento):</b>
 * {@code esperadoEnLote} viene de UNA sola llamada a {@code VigenciaContratoService
 * #esperadoEnLotePorFilial} por carga (no una por partícipe × mes × tipo, que medía del
 * orden de ~120 consultas por partícipe con aportes). Mismo criterio de selección de
 * vigencia que {@link com.saa.ejb.crd.service.VigenciaContratoService#esperadoPorEntidad}
 * — contrato activo con desempate por mayor código, vigencia ACTIVA que cubre el último
 * día del mes — verificado línea por línea contra {@code VigenciaContratoServiceImpl} y
 * {@code VigenciaContratoDaoServiceImpl.selectVigentesPorFilial} antes de usarlo acá.</p>
 *
 * <p><b>⚠️ Con una diferencia real, y por eso el fallback de abajo NO es opcional:</b>
 * {@code selectVigentesPorFilial} filtra ADEMÁS {@code e.ENTDIDST IN (ACTIVO=1,
 * ACTIVO_EN_MORA=8)} sobre la ENTIDAD — filtro que {@code esperadoPorEntidad}/
 * {@code ContratoDaoService#selectActivoPorEntidad} NO aplican (solo miran el estado del
 * CONTRATO, nunca el de la entidad). Una entidad en cualquier otro estado de
 * {@code EstadoParticipeEntidad} (p.ej. {@code JUBILADO_APORTANTE}) queda FUERA del mapa
 * en lote aunque tenga una vigencia real — la clave simplemente no existe, nunca aparece
 * con valor 0. Por eso {@code esperadoEnLote.get(clave)} y NUNCA
 * {@code getOrDefault(clave, 0.0)}: confundir "no pasó el filtro del lote" con "no debe
 * nada" anticiparía la plata a meses futuros en vez de cobrar lo que sí correspondía —
 * un cambio real en la cuenta del socio, no solo de rendimiento.</p>
 *
 * <p><b>CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3, 2026-09-02 — el {@code null} del mapa
 * era ambiguo y eso disparaba una tormenta de consultas:</b> {@code esperadoEnLotePorFilial}
 * sólo trae filas de (entidad, tipo) que tienen AL MENOS una vigencia ACTIVA; una entidad
 * ACTIVO/ACTIVO_EN_MORA sin ninguna vigencia para un tipo (p.ej. nunca aportó a cesantía)
 * quedaba indistinguible de "entidad fuera del filtro" y caía al fallback, mes por mes y tipo
 * por tipo — hasta 60 consultas por partícipe. Dos correcciones, NINGUNA cambia el criterio
 * de selección de vigencia (verificado línea por línea contra
 * {@code VigenciaContratoDaoServiceImpl.selectVigenteEnFecha}), sólo cuántas veces se
 * consulta:</p>
 * <ol>
 * <li><b>§3.1</b> — {@code entidadesCubiertas} (una sola consulta por carga, ver
 * {@code VigenciaContratoDaoService#selectEntidadesConContratoActivoPorFilial}) desambigua el
 * {@code null}: si la entidad SÍ pasa el filtro de {@code selectVigentesPorFilial} pero la
 * clave no está, es un mes/tipo sin vigencia de verdad → {@code 0.0} sin consultar. Si la
 * entidad NO está ni en el mapa ni en este set, es la fuga documentada arriba → fallback.</li>
 * <li><b>§3.2</b> — el fallback ya no llama a {@link com.saa.ejb.crd.service.VigenciaContratoService#esperadoPorEntidad}
 * por cada (mes, tipo): resuelve el contrato y TODAS sus vigencias UNA vez por partícipe en
 * {@code cache} (ver {@link CacheVigenciaParticipe}/{@link #esperadoDesdeCache}) y responde el
 * resto desde memoria — mismo resultado, consulta por consulta menos.</li>
 * </ol>
 */
private double esperadoMensual(Entidad entidad, Long idTipoAporte, java.time.LocalDate mes,
		Map<String, Double> esperadoEnLote, java.util.Set<Long> entidadesCubiertas,
		CacheVigenciaParticipe cache) throws Throwable {
	if (esperadoEnLote != null) {
		String clave = entidad.getCodigo() + "|" + idTipoAporte + "|" + mes;
		Double valorEnLote = esperadoEnLote.get(clave);
		if (valorEnLote != null) {
			return valorEnLote;
		}
		// §3.1: entidad cubierta por el lote (pasa el filtro de selectVigentesPorFilial) pero
		// sin vigencia para este tipo/mes puntual — es un dato (0.0), no una duda.
		if (entidadesCubiertas != null && entidadesCubiertas.contains(entidad.getCodigo())) {
			return 0.0;
		}
	}
	// §3.2: entidad fuera del lote de verdad — resolver UNA vez por partícipe, no por mes.
	return esperadoDesdeCache(entidad, idTipoAporte, mes, cache);
}

/**
 * Caché LOCAL a una llamada de {@link #distribuirAportePorDevengo} (un partícipe) — nunca
 * global, nunca compartido entre partícipes (CORRECCION-TORMENTA-CONSULTAS-VIGENCIA.md §3.2).
 */
private static class CacheVigenciaParticipe {
	boolean resuelto = false;
	Long idContrato;
	List<com.saa.model.crd.VigenciaContrato> vigencias = new ArrayList<>();
}

/**
 * Resuelve contrato + vigencias UNA vez por partícipe (en el primer llamado que lo necesite)
 * y responde TODOS los meses/tipos siguientes desde memoria — mismo criterio exacto que
 * {@code VigenciaContratoDaoServiceImpl.selectVigenteEnFecha} (tipo de aporte, idEstado
 * ACTIVO, fechaInicio &lt;= fecha AND (fechaFin null O fechaFin &gt;= fecha)), evaluado contra
 * el último día de {@code mes}.
 */
private double esperadoDesdeCache(Entidad entidad, Long idTipoAporte, java.time.LocalDate mes,
		CacheVigenciaParticipe cache) throws Throwable {
	if (!cache.resuelto) {
		Contrato contrato = contratoDaoService.selectActivoPorEntidad(entidad.getCodigo());
		cache.idContrato = contrato != null ? contrato.getCodigo() : null;
		if (cache.idContrato != null) {
			cache.vigencias = vigenciaContratoDaoService.selectByContrato(cache.idContrato);
		}
		cache.resuelto = true;
	}
	if (cache.idContrato == null) {
		return 0.0;
	}

	java.time.LocalDate ultimoDiaMes = java.time.YearMonth.from(mes).atEndOfMonth();
	// Corrección pedida por el árbitro (2026-09-02): "resultado idéntico" incluye el caso de
	// error. selectVigenteEnFecha (getSingleResult) lanzaba NonUniqueResultException si más
	// de una vigencia ACTIVA del mismo tipo cubría la misma fecha — tomar la primera en
	// silencio sería un resultado DISTINTO (y, peor, cobrarle de más o de menos a alguien sin
	// que nadie se entere). Se preserva el fallo, con contexto que NonUniqueResultException no
	// tenía (contrato, tipo, fecha, códigos en conflicto). sql/178 mide si el caso existe hoy
	// en producción.
	com.saa.model.crd.VigenciaContrato encontrada = null;
	for (com.saa.model.crd.VigenciaContrato vigencia : cache.vigencias) {
		if (vigencia.getTipoAporte() == null || vigencia.getTipoAporte().getCodigo() == null
				|| !idTipoAporte.equals(vigencia.getTipoAporte().getCodigo())) {
			continue;
		}
		if (vigencia.getIdEstado() == null || vigencia.getIdEstado() != com.saa.rubros.Estado.ACTIVO) {
			continue;
		}
		java.time.LocalDate fechaInicio = vigencia.getFechaInicio();
		java.time.LocalDate fechaFin = vigencia.getFechaFin();
		if (fechaInicio != null && !ultimoDiaMes.isBefore(fechaInicio)
				&& (fechaFin == null || !ultimoDiaMes.isAfter(fechaFin))) {
			if (encontrada != null) {
				throw new IncomeException("Hay más de una vigencia ACTIVA del tipo de aporte "
					+ idTipoAporte + " cubriendo el " + ultimoDiaMes + " para el contrato "
					+ cache.idContrato + " — vigencias " + encontrada.getCodigo() + " y "
					+ vigencia.getCodigo() + " en conflicto. No se puede decidir cuál vale.");
			}
			encontrada = vigencia;
		}
	}
	return encontrada != null && encontrada.getMonto() != null ? encontrada.getMonto() : 0.0;
}

/** Marca de los aportes D-022 en la glosa (REPROCESO-CARGA-PETRO.md §4.10). */
static final String MARCA_D022 = "[D-022 contrato inactivo hoy, vigente al corte]";

/** D-022 vigencias: contrato activo hoy sin vigencia que cubra el mes de la carga (§4.10). */
static final String MARCA_D022_SIN_VIGENCIA = "[D-022 vigencia no cubre el mes]";

/** ¿Alguna vigencia ACTIVA de jubilación/cesantía cubre el último día del mes? Mismo criterio que {@link #esperadoDesdeCache}. */
private static boolean vigenciaCubreMes(List<com.saa.model.crd.VigenciaContrato> vigencias, java.time.LocalDate mesCarga) {
	java.time.LocalDate ultimoDiaMes = java.time.YearMonth.from(mesCarga).atEndOfMonth();
	for (com.saa.model.crd.VigenciaContrato vigencia : vigencias) {
		if (!esVigenciaAporteActiva(vigencia)) {
			continue;
		}
		if (vigencia.getFechaInicio() != null && !ultimoDiaMes.isBefore(vigencia.getFechaInicio())
				&& (vigencia.getFechaFin() == null || !ultimoDiaMes.isAfter(vigencia.getFechaFin()))) {
			return true;
		}
	}
	return false;
}

/**
 * D-022 1b: ¿alguna vigencia ACTIVA de jubilación/cesantía TOCA el mes de la carga
 * ({@code VGCNFCIN <= último día del mes} y ({@code VGCNFCFN} nula o {@code >= primer día del mes}))?
 * Si ninguna lo toca, el AH del mes no tiene esperado posible y va completo al mes de la carga.
 */
private static boolean vigenciaTocaMes(List<com.saa.model.crd.VigenciaContrato> vigencias, java.time.LocalDate mesCarga) {
	java.time.LocalDate primerDia = mesCarga.withDayOfMonth(1);
	java.time.LocalDate ultimoDiaMes = java.time.YearMonth.from(mesCarga).atEndOfMonth();
	for (com.saa.model.crd.VigenciaContrato vigencia : vigencias) {
		if (!esVigenciaAporteActiva(vigencia) || vigencia.getFechaInicio() == null) {
			continue;
		}
		if (!vigencia.getFechaInicio().isAfter(ultimoDiaMes)
				&& (vigencia.getFechaFin() == null || !vigencia.getFechaFin().isBefore(primerDia))) {
			return true;
		}
	}
	return false;
}

private static boolean esVigenciaAporteActiva(com.saa.model.crd.VigenciaContrato vigencia) {
	return vigencia != null && vigencia.getTipoAporte() != null
		&& ORDEN_TIPOS_APORTE.contains(vigencia.getTipoAporte().getCodigo())
		&& vigencia.getIdEstado() != null && vigencia.getIdEstado() == com.saa.rubros.Estado.ACTIVO;
}

/**
 * D-022 (§4.10): tipo de aporte para el AH de un mes que ninguna vigencia del contrato cubre, en este
 * orden: (1) la última vigencia ACTIVA anterior al mes (mayor inicio antes del primer día del mes);
 * (2) la primera vigencia ACTIVA posterior (menor inicio después del último día del mes); (3) el
 * tipo del último aporte vigente de la entidad anterior al mes; (4) cesantía (11) por defecto.
 * {@code origen[0]} dice cuál decidió, para el detalle de la respuesta.
 */
private Long tipoAporteSinVigenciaDelMes(List<com.saa.model.crd.VigenciaContrato> vigencias, java.time.LocalDate mesCarga,
		Entidad entidad, String[] origen) throws Throwable {
	java.time.LocalDate ultimoDiaMes = java.time.YearMonth.from(mesCarga).atEndOfMonth();
	com.saa.model.crd.VigenciaContrato anterior = null;
	com.saa.model.crd.VigenciaContrato posterior = null;
	for (com.saa.model.crd.VigenciaContrato vigencia : vigencias) {
		if (!esVigenciaAporteActiva(vigencia) || vigencia.getFechaInicio() == null) {
			continue;
		}
		if (vigencia.getFechaInicio().isBefore(mesCarga)) {
			if (anterior == null || vigencia.getFechaInicio().isAfter(anterior.getFechaInicio())) {
				anterior = vigencia;
			}
		} else if (vigencia.getFechaInicio().isAfter(ultimoDiaMes)) {
			if (posterior == null || vigencia.getFechaInicio().isBefore(posterior.getFechaInicio())) {
				posterior = vigencia;
			}
		}
	}
	if (anterior != null) {
		origen[0] = "tipo de la vigencia anterior " + anterior.getCodigo();
		return anterior.getTipoAporte().getCodigo();
	}
	if (posterior != null) {
		origen[0] = "tipo de la primera vigencia posterior " + posterior.getCodigo() + " (desde " + posterior.getFechaInicio() + ")";
		return posterior.getTipoAporte().getCodigo();
	}
	Long idTipo = aporteDaoService.selectTipoAporteUltimoAnteriorA(entidad.getCodigo(), mesCarga.atStartOfDay());
	if (idTipo != null) {
		origen[0] = "tipo del último aporte anterior";
		return idTipo;
	}
	origen[0] = "cesantía (11) por defecto: sin vigencias ni aportes anteriores";
	return TIPO_APORTE_CESANTIA;
}

/**
 * D-022 (propuesta del 28-oct-2026, pendiente de confirmar por el líder; REPROCESO-CARGA-PETRO.md
 * §4.10). SOLO MODO REPROCESO, misma filosofía que D-019: el estado de hoy no decide. Para una
 * entidad SIN contrato ACTIVO hoy:
 * <ol>
 * <li>Contrato vigente al corte ({@code selectVigenteAlCortePorEntidad}). Si no hay ninguno: la
 * carga se detiene con un mensaje que dice entidad, rol y monto (no se inventa destino).</li>
 * <li>Si ese contrato tiene alguna vigencia ACTIVA de jubilación o cesantía que cubra el mes de la
 * carga, se deja en {@code cache} y la prelación normal reparte con él (devuelve null).</li>
 * <li>Si no tiene: el AH recibido va COMPLETO al mes de la carga (devengo = mes de la carga, caja =
 * último día del mes, como todo aporte de la carga) con el tipo del último aporte vigente de la
 * entidad anterior al mes de la carga, glosa con {@link #MARCA_D022}. Sin aporte anterior: se
 * detiene con mensaje claro.</li>
 * </ol>
 *
 * @return aportes creados si resolvió el AH completo acá; null si la prelación normal sigue
 */
private Integer resolverContratoAlCorteReproceso(Entidad entidad, double montoRecibido, CargaArchivo cargaArchivo,
		ParticipeXCargaArchivo participe, CacheVigenciaParticipe cache, java.time.LocalDate mesCarga,
		Map<String, Double> esperadoEnLote) throws Throwable {
	Contrato contratoActivo = contratoDaoService.selectActivoPorEntidad(entidad.getCodigo());
	if (contratoActivo != null) {
		// D-022 1b (2026-10-28): contrato activo hoy. Se decide DIRECTAMENTE con las vigencias del
		// contrato, NO con el lote de esperados: esperadoEnLotePorFilial pone la clave de cada mes del
		// rango con 0.0 cuando ninguna vigencia lo cubre (no la omite), así que "hay clave" no
		// significa "hay vigencia" — ese fue el defecto de 2af81e0e (corrida de las 20:27).
		List<com.saa.model.crd.VigenciaContrato> vigenciasActivo = vigenciaContratoDaoService.selectByContrato(contratoActivo.getCodigo());
		if (vigenciasActivo == null) {
			vigenciasActivo = new ArrayList<>();
		}
		if (vigenciaTocaMes(vigenciasActivo, mesCarga)) {
			return null; // flujo de siempre (si aun así no coloca todo, la red de seguridad del tope lo toma)
		}
		// Ninguna vigencia del contrato activo cubre el mes (p. ej. contrato y primera vigencia
		// desde jul-2025 por la migración): el AH va completo al mes de la carga.
		String[] origenTipo = new String[1];
		Long idTipo = tipoAporteSinVigenciaDelMes(vigenciasActivo, mesCarga, entidad, origenTipo);
		double monto = Math.round(montoRecibido * 100.0) / 100.0;
		com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipo, "TipoAporte");
		Aporte aporte = registrarAporteMesCargaD022(entidad, tipoAporte, monto, cargaArchivo, participe, mesCarga,
			MARCA_D022_SIN_VIGENCIA);
		montoAportesSinVigenciaDelMes += monto;
		aportesSinVigenciaDelMes.add("Entidad " + entidad.getCodigo() + " (rol " + participe.getCodigoPetro() + "): $"
			+ String.format("%,.2f", monto) + " a " + tipoAporte.getNombre() + " (" + origenTipo[0] + "; APRT "
			+ aporte.getCodigo() + ", contrato activo " + contratoActivo.getCodigo() + " sin vigencia del mes)");
		System.out.println("   D-022 vigencias: entidad " + entidad.getCodigo() + " - $" + monto + " a "
			+ tipoAporte.getNombre() + " con devengo " + mesCarga + " (" + origenTipo[0] + ")");
		return 1;
	}
	java.time.LocalDateTime corte = finMesCarga(cargaArchivo);
	Contrato contratoCorte = contratoDaoService.selectVigenteAlCortePorEntidad(entidad.getCodigo(), corte);
	if (contratoCorte == null) {
		List<Contrato> contratosEntidad = contratoDaoService.selectByEntidad(entidad.getCodigo());
		if (contratosEntidad == null || contratosEntidad.isEmpty()) {
			// D-022 ampliada: sin NINGÚN contrato -> se crea uno (con su vigencia) y se aplica.
			return crearContratoYAplicarReproceso(entidad, montoRecibido, cargaArchivo, participe, mesCarga);
		}
		throw new IncomeException(etiquetaReproceso + " (D-022): la entidad " + entidad.getCodigo() + " (rol Petro "
			+ participe.getCodigoPetro() + ") tiene " + contratosEntidad.size() + " contrato(s) pero ninguno activo hoy ni"
			+ " vigente al " + corte.toLocalDate() + "; no se puede aplicar el AH de $" + montoRecibido + " de la carga "
			+ cargaArchivo.getCodigo() + ". Corrija las fechas del contrato (o decida el destino) y vuelva a reprocesar.");
	}
	List<com.saa.model.crd.VigenciaContrato> vigencias = vigenciaContratoDaoService.selectByContrato(contratoCorte.getCodigo());
	if (vigencias == null) {
		vigencias = new ArrayList<>();
	}
	if (vigenciaCubreMes(vigencias, mesCarga)) {
		// La prelación normal reparte con el contrato vigente al corte (esperadoDesdeCache).
		cache.resuelto = true;
		cache.idContrato = contratoCorte.getCodigo();
		cache.vigencias = vigencias;
		System.out.println("   D-022: entidad " + entidad.getCodigo() + " sin contrato activo hoy; se usa el contrato "
			+ contratoCorte.getCodigo() + " vigente al corte y sus vigencias");
		return null;
	}

	// Misma regla de tipo que el caso "vigencia no cubre el mes" (D-022 vigencias): última vigencia
	// anterior, primera posterior, último aporte anterior, 11 por defecto. En los 8 casos de la 352
	// el contrato no tiene ninguna vigencia, así que decide el último aporte (igual que antes).
	String[] origenTipoCorte = new String[1];
	Long idTipo = tipoAporteSinVigenciaDelMes(vigencias, mesCarga, entidad, origenTipoCorte);
	double monto = Math.round(montoRecibido * 100.0) / 100.0;
	com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipo, "TipoAporte");
	Aporte aporte = registrarAporteMesCargaD022(entidad, tipoAporte, monto, cargaArchivo, participe, mesCarga, MARCA_D022);
	montoAportesContratoAlCorte += monto;
	aportesContratoAlCorte.add("Entidad " + entidad.getCodigo() + " (rol " + participe.getCodigoPetro() + "): $"
		+ String.format("%,.2f", monto) + " a " + tipoAporte.getNombre() + " (" + origenTipoCorte[0] + "; APRT "
		+ aporte.getCodigo() + ", contrato " + contratoCorte.getCodigo() + " vigente al corte, sin vigencia del mes)");
	System.out.println("   D-022: entidad " + entidad.getCodigo() + " - $" + monto + " a " + tipoAporte.getNombre()
		+ " con devengo " + mesCarga + " (APRT " + aporte.getCodigo() + ")");
	return 1;
}

/** Marca de los aportes de partícipes a los que D-022 ampliada les creó el contrato. */
static final String MARCA_D022_CREADO = "[D-022 contrato creado en reproceso]";

/**
 * D-022: aporte AH completo al mes de la carga (devengo = mes de la carga; caja = último día del
 * mes, {@link #crearNuevoAporte}), con su {@code PagoAporte}, y la glosa
 * {@code "<glosa estándar> - <etiqueta> <marca>"}. Empieza por "Aporte ..." y lleva
 * {@code APRTIDAS} = carga: no entra en ninguno de los LIKE de las consultas de control (los de
 * esquema viejo exigen {@code APRTIDAS IS NULL}; los de reverso empiezan por "REPROCESO").
 */
private Aporte registrarAporteMesCargaD022(Entidad entidad, com.saa.model.crd.TipoAporte tipoAporte, double monto,
		CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe, java.time.LocalDate mesCarga, String marca) throws Throwable {
	Aporte aporte = crearNuevoAporte(entidad, tipoAporte.getCodigo(), tipoAporte.getNombre(), monto, mesCarga, cargaArchivo,
		com.saa.rubros.CrdTipoMovimientoAporte.APORTE_MENSUAL);
	aporte.setGlosa(truncarTexto(aporte.getGlosa() + " - " + etiquetaReproceso + " " + marca, 2000));
	aporteDaoService.save(aporte, aporte.getCodigo());
	crearRegistroPagoAporte(aporte, monto, cargaArchivo, participe);
	return aporte;
}

/**
 * D-022 ampliada (líder del proyecto, 2026-10-28; REPROCESO-CARGA-PETRO.md §4.10): el partícipe
 * tiene AH descontado y NINGÚN contrato (ni activo ni inactivo). En modo reproceso se le crea:
 * <ul>
 * <li>{@code CRD.CNTR}: tipo de contrato = el más frecuente entre los contratos activos de los
 * aportantes AH de esta misma carga (no hay rubro de tipo de contrato en CRD: se toma del dato,
 * no se fija en el código); filial de la entidad; {@code CNTRFCIN} = primer día del mes de la carga
 * o, si es anterior, la fecha del primer aporte vigente de la entidad; {@code CNTRESTD = 1},
 * {@code CNTRIDST = 1}, sin término; observación
 * {@code "D-022 reproceso carga <id>: contrato creado para aplicar el aporte Petro"}; usuario de la
 * corrida.</li>
 * <li>{@code CRD.VGCN} (la prelación por devengo de los meses siguientes la necesita para saber el
 * esperado): tipo de aporte = el del último aporte vigente anterior al mes de la carga, o cesantía
 * (11) si no hay ninguno; monto = lo descontado de AH este mes; desde el primer día del mes de la
 * carga, sin fin; modo FIJO; activa.</li>
 * <li>El aporte del mes: completo al mes de la carga con ese tipo, glosa con
 * {@link #MARCA_D022_CREADO}.</li>
 * </ul>
 * Todo en la transacción del reproceso: si la corrida revierte, el contrato tampoco queda.
 */
private int crearContratoYAplicarReproceso(Entidad entidad, double montoRecibido, CargaArchivo cargaArchivo,
		ParticipeXCargaArchivo participe, java.time.LocalDate mesCarga) throws Throwable {
	if (tipoContratoAportantesReproceso == null) {
		tipoContratoAportantesReproceso = contratoDaoService.selectTipoContratoMasFrecuenteAportantesCarga(cargaArchivo.getCodigo());
	}
	if (tipoContratoAportantesReproceso == null) {
		throw new IncomeException(etiquetaReproceso + " (D-022): la entidad " + entidad.getCodigo() + " (rol Petro "
			+ participe.getCodigoPetro() + ") no tiene contrato y no se pudo determinar el tipo de contrato de los"
			+ " aportantes de la carga " + cargaArchivo.getCodigo() + " para crearle uno.");
	}
	com.saa.model.crd.TipoContrato tipoContrato = tipoContratoDaoService.find(new com.saa.model.crd.TipoContrato(),
		tipoContratoAportantesReproceso);

	java.time.LocalDateTime inicioMes = mesCarga.atStartOfDay();
	java.time.LocalDateTime primerAporte = aporteDaoService.selectFechaPrimerAporteVigente(entidad.getCodigo());
	java.time.LocalDateTime fechaInicio = primerAporte != null && primerAporte.isBefore(inicioMes)
		? primerAporte.toLocalDate().atStartOfDay() : inicioMes;
	Long idTipo = aporteDaoService.selectTipoAporteUltimoAnteriorA(entidad.getCodigo(), inicioMes);
	boolean tipoPorDefecto = idTipo == null;
	if (tipoPorDefecto) {
		idTipo = TIPO_APORTE_CESANTIA;
	}
	com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipo, "TipoAporte");
	double monto = Math.round(montoRecibido * 100.0) / 100.0;
	String observacion = "D-022 reproceso carga " + cargaArchivo.getCodigo()
		+ ": contrato creado para aplicar el aporte Petro (" + etiquetaReproceso + ")";
	String usuario = truncarTexto(usuarioReproceso != null ? usuarioReproceso : "SAA_REPROCESO", 50);
	java.time.LocalDateTime ahora = java.time.LocalDateTime.now();

	Contrato contrato = new Contrato();
	contrato.setEntidad(entidad);
	contrato.setFilial(entidad.getFilial());
	contrato.setTipoContrato(tipoContrato);
	contrato.setFechaInicio(fechaInicio);
	contrato.setFechaTerminacion(null);
	contrato.setEstado(Long.valueOf(com.saa.rubros.EstadoContrato.ACTIVO));
	contrato.setIdEstado(Long.valueOf(com.saa.rubros.Estado.ACTIVO));
	contrato.setObservacion(observacion);
	contrato.setUsuarioRegistro(usuario);
	contrato.setFechaRegistro(ahora);
	contratoDaoService.save(contrato, null);

	com.saa.model.crd.VigenciaContrato vigencia = new com.saa.model.crd.VigenciaContrato();
	vigencia.setContrato(contrato);
	vigencia.setTipoAporte(tipoAporte);
	vigencia.setFechaInicio(mesCarga);
	vigencia.setFechaFin(null);
	vigencia.setMonto(monto);
	vigencia.setModo(Long.valueOf(com.saa.rubros.CrdModoVigenciaContrato.FIJO));
	vigencia.setIdEstado(Long.valueOf(com.saa.rubros.Estado.ACTIVO));
	vigencia.setObservacion(observacion);
	vigencia.setUsuarioRegistro(usuario);
	vigencia.setFechaRegistro(ahora);
	vigenciaContratoDaoService.save(vigencia, null);

	Aporte aporte = registrarAporteMesCargaD022(entidad, tipoAporte, monto, cargaArchivo, participe, mesCarga, MARCA_D022_CREADO);
	montoContratosCreadosReproceso += monto;
	contratosCreadosReproceso.add("Entidad " + entidad.getCodigo() + " (rol " + participe.getCodigoPetro() + "): contrato "
		+ contrato.getCodigo() + " (tipo " + tipoContratoAportantesReproceso + ", desde " + fechaInicio.toLocalDate()
		+ ") y vigencia " + vigencia.getCodigo() + " creados; $" + String.format("%,.2f", monto) + " a "
		+ tipoAporte.getNombre() + (tipoPorDefecto ? " (tipo por defecto: sin aporte anterior)" : "")
		+ " (APRT " + aporte.getCodigo() + ")");
	System.out.println("   D-022 ampliada: entidad " + entidad.getCodigo() + " sin contrato - creado contrato "
		+ contrato.getCodigo() + " y vigencia " + vigencia.getCodigo() + "; $" + monto + " a " + tipoAporte.getNombre());
	return 1;
}

/**
 * Aplica una afectación manual cuyo destino es un tipo de aporte, no una cuota (opción ③ del
 * §3.7 del levantamiento contable — PLAN-EXCEDENTE-PETRO-A-APORTES.md). Registra el aporte por
 * el mismo camino que el devengo normal ({@link #crearNuevoAporte} + {@link
 * #crearRegistroPagoAporte}), con {@code tipoMovimiento = EXCEDENTE_PETRO} para que quede
 * distinguible de un aporte mensual normal.
 *
 * {@code periodoDevengo = null}: un excedente no cubre el aporte esperado de ningún mes en
 * particular — es dinero adicional que el partícipe eligió enviar a su cuenta, no un
 * anticipo/atraso de la regla de devengo (mismo criterio que
 * {@code DevolucionAporteServiceImpl} usa para el remanente sin anticipo, D5 §2.4 del plan de
 * devengo de aportes: "retiro de saldo, no altera ningún mes").
 *
 * ⚠️ SIN ASIENTO PROPIO A PROPÓSITO. Verificado en {@code CobroPetroContableServiceImpl
 * .contabilizarAplicacion} (2026-08-31): esa línea suma TODOS los {@code CRD.APRT} de la carga
 * por tipo vía {@code sumValorPorTipoAporteByCarga}, que filtra por {@code APRTIDAS}
 * (idAsoprep) — el mismo campo que {@link #crearNuevoAporte} estampa acá. El aporte que este
 * método crea entra en esa suma automáticamente y ya queda contabilizado en el HABER de ese
 * asiento (aux1 50/51/52 de la plantilla 21, vía {@code ContabilizacionIndividualCreditoService
 * .lineaAporteRegistrado}), como cualquier otro aporte de la carga. Agregar acá un asiento
 * propio habría contabilizado la misma plata DOS VECES, con los dos asientos cuadrando D=H y
 * ningún control atrapándolo — exactamente la trampa del §2 de PLAN-CIERRE-CONTABLE-TOTAL.md,
 * en otra forma. Si algún día {@code contabilizarAplicacion} deja de sumar por esta vía (p.ej.
 * al migrar el filtro de {@code idAsoprep} a {@code cargaArchivo}, ver el comentario de
 * {@code sumValorPorTipoAporteByCarga}), este método necesita revisarse junto con ese cambio.
 */
private void aplicarAfectacionAAporte(AfectacionValoresParticipeCarga afectacion,
		com.saa.model.crd.TipoAporte tipoAporte, CargaArchivo cargaArchivo,
		ParticipeXCargaArchivo participe) throws Throwable {

	double monto = nullSafe(afectacion.getValorAfectar());
	if (monto <= 0.01) {
		throw new IncomeException("La afectación manual (AVPC " + afectacion.getCodigo()
			+ ") a un aporte tiene valorAfectar $" + monto + "; debe ser mayor a cero.");
	}

	List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
	if (entidades == null || entidades.isEmpty()) {
		throw new IncomeException("No se encontró la entidad del partícipe " + participe.getCodigoPetro()
			+ " (" + participe.getNombre() + ") para aplicar la afectación manual (AVPC "
			+ afectacion.getCodigo() + ") al aporte " + tipoAporte.getNombre() + ".");
	}
	Entidad entidad = entidades.get(0);

	Aporte nuevoAporte = crearNuevoAporte(entidad, tipoAporte.getCodigo(), tipoAporte.getNombre(),
			monto, null, cargaArchivo, com.saa.rubros.CrdTipoMovimientoAporte.EXCEDENTE_PETRO);
	// D-010 corrección (28-oct-2026): producto de la fila por la que entró el dinero, para que el
	// asiento de aplicación debite el puente correcto (2.3.02.10 si vino de una fila de préstamo).
	String productoEntrada = participe.getDetalleCargaArchivo() != null
			? participe.getDetalleCargaArchivo().getCodigoPetroProducto() : null;
	if (productoEntrada != null) {
		nuevoAporte.setGlosa(truncarTexto(com.saa.rubros.EntradaExcedentePetro.marca(productoEntrada) + " "
				+ nuevoAporte.getGlosa() + " - AVPC " + afectacion.getCodigo(), 2000));
		aporteDaoService.save(nuevoAporte, nuevoAporte.getCodigo());
	}
	crearRegistroPagoAporte(nuevoAporte, monto, cargaArchivo, participe);

	System.out.println("   ✅ Excedente aplicado a aporte " + tipoAporte.getNombre() + ": $" + monto
		+ " (AVPC " + afectacion.getCodigo() + ", APRT " + nuevoAporte.getCodigo() + ")");
}

/**
 * Crea un nuevo registro de aporte por lo efectivamente recibido en este tramo.
 *
 * Fase 1 del plan de devengo de aportes (D1): {@code valor} ya no es "lo esperado" sino
 * "lo recibido". La fila nace pagada por construcción: no hay abono posterior ni saldo
 * pendiente. {@code monto} es el tramo que le corresponde a esta fila (puede ser menor que
 * lo esperado del mes si el recibido no alcanza, o exactamente lo esperado si el reparto
 * generó varias filas).
 *
 * Fase 2: {@code periodoDevengo} es el mes al que pertenece este tramo según la prelación de
 * {@link #distribuirAportePorDevengo}; {@code fechaTransaccion} (la fecha de CAJA) sigue
 * siendo el último día del mes de la CARGA — D2, no cambia de significado aunque el devengo
 * sea un mes distinto (atraso o anticipo).
 *
 * @param tipoMovimiento {@code com.saa.rubros.CrdTipoMovimientoAporte} — de dónde viene este
 *                       aporte (rubro 235). {@code APORTE_MENSUAL} para el devengo normal del
 *                       archivo; {@code EXCEDENTE_PETRO} para el excedente redirigido a un
 *                       aporte por {@link #aplicarAfectacionAAporte}. Parámetro explícito y no
 *                       un método hermano: un sibling que solo difiere en este valor duplica el
 *                       camino (2026-08-31, condición del árbitro).
 */
private Aporte crearNuevoAporte(Entidad entidad, Long idTipoAporte, String nombreTipo,
                               double monto, java.time.LocalDate periodoDevengo,
                               CargaArchivo cargaArchivo, int tipoMovimiento) throws Throwable {

	// selectById (EntityDaoImpl genérico) usa getSingleResult(): una fila faltante lanza
	// NoResultException, no null. TIPO_APORTE_JUBILACION(9)/CESANTIA(11) son constantes fijas
	// de este archivo — que no existan en CRD.TPAP no es una ausencia de dato de negocio (no
	// hay "if" razonable para eso), es un catálogo roto. Se envuelve para que el mensaje diga
	// QUÉ tipo de aporte falta, en vez de propagar el NoResultException genérico de JPA.
	com.saa.model.crd.TipoAporte tipoAporte;
	try {
		tipoAporte = tipoAporteDaoService.selectById(idTipoAporte, "TipoAporte");
	} catch (Throwable e) {
		throw new RuntimeException("No existe el tipo de aporte " + idTipoAporte + " (" + nombreTipo
			+ ") en el catálogo CRD.TPAP — no se puede generar el aporte de la entidad "
			+ entidad.getCodigo() + ": " + e.getMessage(), e);
	}

	// ✅ CORRECCIÓN: Usar FechaService para obtener el último día del mes del periodo de carga
	Long mesCarga = cargaArchivo.getMesAfectacion();
	Long anioCarga = cargaArchivo.getAnioAfectacion();
	java.time.LocalDate fechaUltimoDia = fechaService.ultimoDiaMesAnioLocal(mesCarga, anioCarga);
	java.time.LocalDateTime fechaAporte = fechaUltimoDia.atTime(23, 59, 59);

	Aporte nuevoAporte = new Aporte();
	nuevoAporte.setFilial(entidad.getFilial()); // ✅ Obtener filial desde la entidad
	nuevoAporte.setEntidad(entidad);
	nuevoAporte.setTipoAporte(tipoAporte);
	nuevoAporte.setValor(monto);
	nuevoAporte.setValorPagado(monto);
	nuevoAporte.setSaldo(0.0);
	nuevoAporte.setIdAsoprep(cargaArchivo.getCodigo()); // ✅ CRÍTICO: Enlazar con CargaArchivo
	nuevoAporte.setFechaTransaccion(fechaAporte); // ✅ CORRECCIÓN: Usar último día del mes
	nuevoAporte.setPeriodoDevengo(periodoDevengo);
	nuevoAporte.setTipoMovimiento((long) tipoMovimiento);
	nuevoAporte.setGlosa(String.format("Aporte %s - Mes %d/%d - CargaArchivo: %d",
	                                   nombreTipo,
	                                   cargaArchivo.getMesAfectacion(),
	                                   cargaArchivo.getAnioAfectacion(),
	                                   cargaArchivo.getCodigo()));
	nuevoAporte.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
	nuevoAporte.setFechaRegistro(java.time.LocalDateTime.now());
	nuevoAporte.setUsuarioRegistro("SAA_AH"); // ✅ Identificador de aportes creados por el sistema
	// Trazabilidad (2026-08-28/29): de qué carga salió este aporte. idAsoprep (línea de
	// arriba) YA ES la trazabilidad viva — la leen selectByEntidadTipoYCarga y
	// selectAporteAdelantado — y sigue siendo la fuente hasta que corra el backfill
	// (78_BACKFILL_CRARCDGO_APORTES.sql). cargaArchivo es la columna gobernada (FK+índice,
	// DDL-TRAZABILIDAD-CARGA-PETRO.sql) que se llena en paralelo desde ahora, para poder
	// migrar el LECTOR más adelante sin reprocesar nada. Ver javadoc de Aporte.idAsoprep.
	nuevoAporte.setCargaArchivo(cargaArchivo);

	// DAO directo: AporteServiceImpl.saveSingle fuerza estado = 1 (Estado.ACTIVO) en todo
	// INSERT (codigo == null), pisando el PAGADA(4) recién asignado. Mismo motivo por el que
	// AporteServiceImpl.registrarAporte también usa el DAO directo.
	aporteDaoService.save(nuevoAporte, null);

	System.out.println("   ✅ Nuevo aporte creado - Valor recibido: $" + monto + " - Fecha: " + fechaAporte);

	return nuevoAporte;
}

/**
 * Crea un registro en la tabla PagoAporte para trazabilidad
 */
private void crearRegistroPagoAporte(Aporte aporte, double montoPagado,
                                    CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {
	// TODO O NADA (2026-08-29): antes, si el saveSingle fallaba, se tragaba el error acá — pero
	// el llamador (distribuirAportePorDevengo) YA había guardado el Aporte con crearNuevoAporte
	// un renglón antes. Un aporte marcado PAGADA sin su PagoAporte de trazabilidad detrás es el
	// mismo problema que crearRegistroPago (arriba) del lado de préstamos: un fallo de save es
	// siempre un fallo real, debe propagar.
	try {
		// 2026-09-02: FechaContable es fecha de EFECTO (igual que crearRegistroPago del lado
		// de préstamos) — último día del mes de carga, no la fecha de proceso. Sin fallback
		// a now(): si mes/año de afectación vienen null, grita.
		if (cargaArchivo.getMesAfectacion() == null || cargaArchivo.getAnioAfectacion() == null) {
			throw new IncomeException("No se puede fechar el pago del aporte "
				+ (aporte != null ? aporte.getCodigo() : null) + ": falta el mes o año de afectación"
				+ " de la carga " + cargaArchivo.getCodigo() + ".");
		}
		java.time.LocalDateTime fechaContableAporte = fechaService.ultimoDiaMesAnioLocal(
			cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()).atTime(23, 59, 59);

		com.saa.model.crd.PagoAporte pago = new com.saa.model.crd.PagoAporte();
		pago.setFilial(aporte.getFilial()); // Obtener filial desde el aporte
		pago.setAporte(aporte);
		pago.setValor(montoPagado);
		pago.setFechaContable(fechaContableAporte);
		pago.setConcepto(String.format("Pago aporte mes %d/%d - Partícipe: %d (%s) - CargaArchivo: %d",
		                               cargaArchivo.getMesAfectacion(),
		                               cargaArchivo.getAnioAfectacion(),
		                               participe.getCodigoPetro(),
		                               participe.getNombre(),
		                               cargaArchivo.getCodigo()));
		pago.setFechaRegistro(java.time.LocalDateTime.now());
		pago.setUsuarioRegistro("SISTEMA");
		pago.setEstado(1L);
		
		pagoAporteService.saveSingle(pago);

		System.out.println("   ✅ Pago registrado en PagoAporte: $" + montoPagado);

	} catch (Throwable e) {
		throw new RuntimeException("Falló al crear el registro de pago (PagoAporte) del aporte "
			+ (aporte != null ? aporte.getCodigo() : null) + " por $" + montoPagado
			+ " del partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
			+ ") — el aporte ya había quedado marcado como pagado sin este respaldo. Causa: "
			+ e.getMessage(), e);
	}
}

} // Cierre de la clase CargaArchivoPetroServiceImpl
