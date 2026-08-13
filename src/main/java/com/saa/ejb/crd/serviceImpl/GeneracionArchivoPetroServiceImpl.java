package com.saa.ejb.crd.serviceImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.GeneracionArchivoPetroDaoService;
import com.saa.ejb.crd.service.CuotaXParticipeGeneracionService;
import com.saa.ejb.crd.service.DetalleGeneracionArchivoService;
import com.saa.ejb.crd.service.GeneracionArchivoPetroService;
import com.saa.ejb.crd.service.ParticipeDetalleGeneracionArchivoService;
import com.saa.model.crd.CuotaXParticipeGeneracion;
import com.saa.model.crd.DetalleGeneracionArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.Filial;
import com.saa.model.crd.GeneracionArchivoPetro;
import com.saa.model.crd.HistorialSueldo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ParticipeDetalleGeneracionArchivo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.Filiales;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación Service para GeneracionArchivoPetro (GNAP).
 * Contiene toda la lógica de negocio para generación de archivos Petrocomercial.
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Stateless
public class GeneracionArchivoPetroServiceImpl implements GeneracionArchivoPetroService {

    /**
     * Orden en que se recorren los productos al armar el archivo.
     * Es también el orden de los bloques en el archivo de Petrocomercial.
     */
    private static final String[] ORDEN_PRODUCTOS = {"AH", "HS", "PE", "PH", "PQ", "PP"};

    /**
     * Orden de las columnas de préstamos/seguro en el archivo plano de ARCH.
     * Las columnas de aportes (AC y AJ) van antes y se manejan aparte.
     */
    private static final String[] ORDEN_COLUMNAS_PRESTAMOS = {"PE", "PH", "HS", "PQ", "PP"};

    /** Separador de columnas del archivo plano de ARCH. */
    private static final String SEPARADOR_COLUMNAS = ";";

    /** Marca de la última fila del archivo plano de ARCH, la de totales globales. */
    private static final String ETIQUETA_FILA_TOTALES = "TOTALES";

    /** Título que encabeza el archivo plano de ARCH. */
    private static final String TITULO_ARCHIVO_PLANO = "ASOPREP";

    /** Nombres de los meses. El índice es el número de mes (1 = ENERO). */
    private static final String[] NOMBRES_MESES = {"", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
                                                   "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};

    @EJB
    private GeneracionArchivoPetroDaoService dao;
    
    @EJB
    private DetalleGeneracionArchivoService detalleService;
    
    @EJB
    private ParticipeDetalleGeneracionArchivoService participeDetalleService;
    
    @EJB
    private CuotaXParticipeGeneracionService cuotaXParticipeService;
    
    @EJB
    private FechaService fechaService;

    @EJB
    private AporteDaoService aporteDaoService;

    @PersistenceContext
    private EntityManager em;

    /** Tipos de aporte que componen el aporte mensual: 9 = JUBILACIÓN, 11 = CESANTÍA. */
    private static final Long TIPO_APORTE_JUBILACION = 9L;
    private static final Long TIPO_APORTE_CESANTIA   = 11L;

    // ========================================================================
    // MÉTODOS CRUD BÁSICOS
    // ========================================================================

    @Override
    public GeneracionArchivoPetro crear(GeneracionArchivoPetro generacion) throws Exception {
        System.out.println("Service: Creando generación archivo Petrocomercial");
        try {
            return dao.save(generacion, null);
        } catch (Throwable e) {
            throw new Exception("Error al crear generación: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneracionArchivoPetro actualizar(GeneracionArchivoPetro generacion) throws Exception {
        System.out.println("Service: Actualizando generación archivo Petrocomercial: " + generacion.getCodigo());
        try {
            return dao.save(generacion, generacion.getCodigo());
        } catch (Throwable e) {
            throw new Exception("Error al actualizar generación: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneracionArchivoPetro buscarPorId(Long codigo) throws Exception {
        System.out.println("Service: Buscando generación por ID: " + codigo);
        try {
            return dao.selectById(codigo, "GeneracionArchivoPetro");
        } catch (Throwable e) {
            throw new Exception("Error al buscar generación: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneracionArchivoPetro buscarPorPeriodo(Long mes, Long anio, Long codigoFilial) throws Exception {
        System.out.println("Service: Buscando generación por periodo: " + mes + "/" + anio + " - Filial: " + codigoFilial);
        try {
            return dao.selectByPeriodo(mes.intValue(), anio.intValue(), codigoFilial);
        } catch (Throwable e) {
            throw new Exception("Error al buscar generación por periodo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneracionArchivoPetro> listarPorFilial(Long codigoFilial) throws Exception {
        System.out.println("Service: Listando generaciones de la filial: " + codigoFilial);
        try {
            return dao.selectByFilial(codigoFilial);
        } catch (Throwable e) {
            throw new Exception("Error al listar generaciones: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeneracionArchivoPetro> listarPorEstado(Long estado) throws Exception {
        System.out.println("Service: Listando generaciones con estado: " + estado);
        try {
            return dao.selectByEstado(estado.intValue());
        } catch (Throwable e) {
            throw new Exception("Error al listar generaciones por estado: " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // LÓGICA DE NEGOCIO - CREACIÓN Y PROCESAMIENTO
    // ========================================================================

    @Override
    public GeneracionArchivoPetro crearCabeceraGeneracion(Long mes, Long anio, Long codigoFilial, String usuario) throws Exception {
        System.out.println("=== SERVICIO: CREANDO CABECERA DE GENERACIÓN PETROCOMERCIAL ===");
        System.out.println("Periodo: " + mes + "/" + anio + " - Filial: " + codigoFilial);
        
        // 1. Verificar si ya existe generación para este periodo
        GeneracionArchivoPetro existente = buscarPorPeriodo(mes, anio, codigoFilial);
        
        if (existente != null) {
            throw new Exception("Ya existe una generación para el periodo " + mes + "/" + anio + 
                              ". Código: " + existente.getCodigo());
        }
        
        // 2. Crear cabecera de generación
        GeneracionArchivoPetro generacion = new GeneracionArchivoPetro();
        generacion.setMesPeriodo(mes);
        generacion.setAnioPeriodo(anio);
        generacion.setFechaGeneracion(LocalDate.now());
        generacion.setUsuarioGeneracion(usuario);
        generacion.setEstado(0L); // 0=PENDIENTE (aún no procesada)
        generacion.setTotalRegistros(0L);
        generacion.setTotalMontoEnviado(0.0);
        generacion.setUsuarioIngreso(usuario);
        generacion.setFechaIngreso(LocalDate.now());
        
        Filial filial = new Filial();
        filial.setCodigo(codigoFilial);
        generacion.setFilial(filial);
        
        generacion = crear(generacion);
        
        System.out.println("Cabecera creada exitosamente con ID: " + generacion.getCodigo());
        return generacion;
    }

    @Override
    public Map<String, Object> procesarGeneracion(Long codigoGeneracion, String usuario) throws Exception {
        System.out.println("=== SERVICIO: PROCESANDO GENERACIÓN PETROCOMERCIAL ===");
        System.out.println("Código Generación: " + codigoGeneracion);
        
        // 1. Buscar la cabecera
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        
        if (generacion == null) {
            throw new Exception("Generación no encontrada con ID: " + codigoGeneracion);
        }
        
        // 2. Validar que no haya sido procesada
        Long estadoActual = generacion.getEstado();
        if (estadoActual != null && estadoActual != 0L) {
            throw new Exception("Esta generación ya fue procesada. Estado actual: " + estadoActual);
        }
        
        // Si el estado es null, lo consideramos como pendiente (0)
        if (estadoActual == null) {
            System.out.println("ADVERTENCIA: Estado es null, se asume como PENDIENTE (0)");
        }
        
        // 3. Obtener datos del periodo de la cabecera
        Long mes = generacion.getMesPeriodo();
        Long anio = generacion.getAnioPeriodo();

        // La filial define QUÉ partícipes entran y CON QUÉ FORMATO sale el archivo.
        Long codigoFilial = obtenerCodigoFilial(generacion);

        System.out.println("Procesando periodo: " + mes + "/" + anio + " - Filial: " + codigoFilial);

        // 4. Recopilar datos por tipo de producto (solo partícipes de esta filial)
        Map<String, List<LineaArchivo>> datosPorProducto = recopilarDatos(mes, anio, codigoFilial);
        
        // 5. Crear detalles por producto y registros de partícipes
        long totalRegistros = 0;
        double totalMonto = 0.0;
        long numeroLinea = 1;

        for (String codigoProducto : ORDEN_PRODUCTOS) {
            List<LineaArchivo> lineas = datosPorProducto.get(codigoProducto);
            if (lineas == null || lineas.isEmpty()) {
                continue;
            }
            
            // Crear detalle por producto
            DetalleGeneracionArchivo detalle = new DetalleGeneracionArchivo();
            detalle.setGeneracionArchivoPetro(generacion);
            detalle.setCodigoProductoPetro(codigoProducto);
            detalle.setDescripcionProducto(obtenerDescripcionProducto(codigoProducto));
            detalle.setTotalRegistros((long) lineas.size());
            
            double montoProducto = 0.0;
            for (LineaArchivo linea : lineas) {
                montoProducto += linea.monto;
            }
            detalle.setTotalMonto(montoProducto);
            detalle.setUsuarioIngreso(usuario);
            detalle.setFechaIngreso(LocalDate.now());
            
            try {
                detalle = detalleService.crear(detalle);
            } catch (Throwable e) {
                throw new Exception("Error al crear detalle: " + e.getMessage(), e);
            }
            System.out.println("Detalle creado para producto " + codigoProducto + ": " + lineas.size() + " registros");
            
            // Crear registros de partícipes
            for (LineaArchivo linea : lineas) {
                ParticipeDetalleGeneracionArchivo participeDetalle = new ParticipeDetalleGeneracionArchivo();
                participeDetalle.setDetalleGeneracionArchivo(detalle);
                
                Entidad entidad = new Entidad();
                entidad.setCodigo(linea.codigoEntidad);
                participeDetalle.setEntidad(entidad);
                
                if (linea.codigoPrestamo != null) {
                    Prestamo prestamo = new Prestamo();
                    prestamo.setCodigo(linea.codigoPrestamo);
                    participeDetalle.setPrestamo(prestamo);
                }
                
                participeDetalle.setRolPetrocomercial(linea.rolPetrocomercial);
                participeDetalle.setCodigoProductoPetro(codigoProducto);
                participeDetalle.setMontoEnviado(linea.monto);
                participeDetalle.setNumeroLinea(numeroLinea);
                participeDetalle.setEstado(1); // 1=ENVIADO
                participeDetalle.setUsuarioIngreso(usuario);
                participeDetalle.setFechaIngreso(LocalDateTime.now());
                
                try {
                    participeDetalle = participeDetalleService.crear(participeDetalle);
                } catch (Throwable e) {
                    throw new Exception("Error al crear partícipe detalle: " + e.getMessage(), e);
                }
                
                // Crear registros de CXPG (detalle de cuotas)
                if ("HS".equals(codigoProducto) && !linea.cuotasSumadas.isEmpty()) {
                    // Es SEGURO DE INCENDIO: crear un registro por cada cuota, indicando el préstamo de origen
                    for (CuotaInfo cuotaInfo : linea.cuotasSumadas) {
                        CuotaXParticipeGeneracion cxpg = new CuotaXParticipeGeneracion();
                        cxpg.setParticipeDetalleGeneracion(participeDetalle);
                        
                        // El préstamo de origen está en cuotaInfo.codigoPrestamo
                        if (cuotaInfo.codigoPrestamo != null) {
                            Prestamo prestamo = new Prestamo();
                            prestamo.setCodigo(cuotaInfo.codigoPrestamo);
                            cxpg.setPrestamo(prestamo);
                        }
                        
                        cxpg.setNumeroCuota(cuotaInfo.numeroCuota);
                        cxpg.setValorCuota(cuotaInfo.valorCuota);
                        cxpg.setTipoAporte(null); // No aplica para seguros
                        cxpg.setUsuarioIngreso(usuario);
                        cxpg.setFechaIngreso(LocalDateTime.now());
                        
                        try {
                            cuotaXParticipeService.crear(cxpg);
                        } catch (Throwable e) {
                            throw new Exception("Error al crear CXPG para seguro de incendio: " + e.getMessage(), e);
                        }
                    }
                    
                    System.out.println("  → Creados " + linea.cuotasSumadas.size() + " registros CXPG para seguros de incendio");
                    
                } else if (linea.codigoPrestamo != null && !linea.cuotasSumadas.isEmpty()) {
                    // Es un PRÉSTAMO: crear un registro por cada cuota sumada
                    for (CuotaInfo cuotaInfo : linea.cuotasSumadas) {
                        CuotaXParticipeGeneracion cxpg = new CuotaXParticipeGeneracion();
                        cxpg.setParticipeDetalleGeneracion(participeDetalle);
                        
                        Prestamo prestamo = new Prestamo();
                        prestamo.setCodigo(linea.codigoPrestamo);
                        cxpg.setPrestamo(prestamo);
                        
                        cxpg.setNumeroCuota(cuotaInfo.numeroCuota);
                        cxpg.setValorCuota(cuotaInfo.valorCuota);
                        cxpg.setTipoAporte(null); // No aplica para préstamos
                        cxpg.setUsuarioIngreso(usuario);
                        cxpg.setFechaIngreso(LocalDateTime.now());
                        
                        try {
                            cuotaXParticipeService.crear(cxpg);
                        } catch (Throwable e) {
                            throw new Exception("Error al crear cuota x partícipe: " + e.getMessage(), e);
                        }
                    }
                    
                    System.out.println("  → Creados " + linea.cuotasSumadas.size() + " registros CXPG para préstamo " + linea.codigoPrestamo);
                    
                } else if ("AH".equals(codigoProducto)) {
                    // Es un APORTE: crear registros separados para jubilación y cesantía
                    int registrosCreados = 0;
                    
                    // Registro para JUBILACIÓN (Tipo Aporte 9)
                    if (linea.montoJubilacion != null && linea.montoJubilacion > 0) {
                        CuotaXParticipeGeneracion cxpgJubilacion = new CuotaXParticipeGeneracion();
                        cxpgJubilacion.setParticipeDetalleGeneracion(participeDetalle);
                        cxpgJubilacion.setPrestamo(null); // No aplica para aportes
                        cxpgJubilacion.setNumeroCuota(null); // No aplica para aportes
                        cxpgJubilacion.setValorCuota(linea.montoJubilacion);
                        
                        TipoAporte tipoAporteJubilacion = new TipoAporte();
                        tipoAporteJubilacion.setCodigo(9L); // Código 9 = Jubilación
                        cxpgJubilacion.setTipoAporte(tipoAporteJubilacion);
                        
                        cxpgJubilacion.setUsuarioIngreso(usuario);
                        cxpgJubilacion.setFechaIngreso(LocalDateTime.now());
                        
                        try {
                            cuotaXParticipeService.crear(cxpgJubilacion);
                            registrosCreados++;
                        } catch (Throwable e) {
                            throw new Exception("Error al crear CXPG para jubilación: " + e.getMessage(), e);
                        }
                    }
                    
                    // Registro para CESANTÍA (Tipo Aporte 11)
                    if (linea.montoCesantia != null && linea.montoCesantia > 0) {
                        CuotaXParticipeGeneracion cxpgCesantia = new CuotaXParticipeGeneracion();
                        cxpgCesantia.setParticipeDetalleGeneracion(participeDetalle);
                        cxpgCesantia.setPrestamo(null); // No aplica para aportes
                        cxpgCesantia.setNumeroCuota(null); // No aplica para aportes
                        cxpgCesantia.setValorCuota(linea.montoCesantia);
                        
                        TipoAporte tipoAporteCesantia = new TipoAporte();
                        tipoAporteCesantia.setCodigo(11L); // Código 11 = Cesantía
                        cxpgCesantia.setTipoAporte(tipoAporteCesantia);
                        
                        cxpgCesantia.setUsuarioIngreso(usuario);
                        cxpgCesantia.setFechaIngreso(LocalDateTime.now());
                        
                        try {
                            cuotaXParticipeService.crear(cxpgCesantia);
                            registrosCreados++;
                        } catch (Throwable e) {
                            throw new Exception("Error al crear CXPG para cesantía: " + e.getMessage(), e);
                        }
                    }
                    
                    if (registrosCreados > 0) {
                        System.out.println("  → Creados " + registrosCreados + " registros CXPG para aporte (Jub: $" + 
                            String.format("%.2f", linea.montoJubilacion) + ", Ces: $" + 
                            String.format("%.2f", linea.montoCesantia) + ")");
                    }
                }
                
                numeroLinea++;
            }
            
            totalRegistros += lineas.size();
            totalMonto += montoProducto;
        }
        
        // 6. Generar archivo físico TXT
        String nombreArchivo = generarNombreArchivo(mes, anio, codigoFilial);
        String rutaArchivo = generarArchivoTXT(datosPorProducto, mes, anio, nombreArchivo, codigoFilial);
        
        // 7. Actualizar cabecera con totales y ruta del archivo
        generacion.setEstado(1L); // 1=GENERADO
        generacion.setTotalRegistros(totalRegistros);
        generacion.setTotalMontoEnviado(totalMonto);
        generacion.setNombreArchivo(nombreArchivo);
        generacion.setRutaArchivo(rutaArchivo);
        generacion.setUsuarioModificacion(usuario);
        generacion.setFechaModificacion(LocalDate.now());
        
        generacion = actualizar(generacion);
        
        System.out.println("=== PROCESAMIENTO COMPLETADO ===");
        System.out.println("Total registros: " + totalRegistros);
        System.out.println("Total monto: $" + String.format("%.2f", totalMonto));
        System.out.println("Archivo generado: " + rutaArchivo);
        
        // Respuesta
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("success", true);
        respuesta.put("mensaje", "Archivo generado exitosamente");
        respuesta.put("codigoGeneracion", generacion.getCodigo());
        respuesta.put("totalRegistros", totalRegistros);
        respuesta.put("totalMonto", totalMonto);
        respuesta.put("nombreArchivo", nombreArchivo);
        respuesta.put("rutaArchivo", rutaArchivo);
        respuesta.put("codigoFilial", codigoFilial);

        return respuesta;
    }

    /**
     * Devuelve el código de filial de la generación.
     *
     * Sin filial no se puede saber a qué partícipes incluir ni con qué formato
     * escribir el archivo, así que se corta el proceso en vez de generar un
     * archivo con la población equivocada.
     */
    private Long obtenerCodigoFilial(GeneracionArchivoPetro generacion) throws Exception {
        if (generacion.getFilial() == null || generacion.getFilial().getCodigo() == null) {
            throw new Exception("La generación " + generacion.getCodigo()
                + " no tiene filial asignada. Debe indicarse la filial al crear la cabecera.");
        }
        return generacion.getFilial().getCodigo();
    }

    // ========================================================================
    // GESTIÓN DE ESTADOS
    // ========================================================================

    @Override
    public void anular(Long codigoGeneracion, String usuario, String motivo) throws Exception {
        System.out.println("Service: Anulando generación " + codigoGeneracion);
        
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        Long estadoActual = generacion.getEstado();
        if (estadoActual != null && estadoActual != 1L) {
            throw new Exception("Solo se pueden anular generaciones en estado GENERADO. Estado actual: " + estadoActual);
        }
        
        generacion.setEstado(0L); // 0=ANULADO
        generacion.setObservaciones(motivo);
        generacion.setUsuarioModificacion(usuario);
        generacion.setFechaModificacion(LocalDate.now());
        
        actualizar(generacion);
        System.out.println("Generación anulada exitosamente");
    }

    @Override
    public GeneracionArchivoPetro marcarEnviado(Long codigoGeneracion, String usuario) throws Exception {
        System.out.println("Service: Marcando como enviado la generación " + codigoGeneracion);
        
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        Long estadoActual = generacion.getEstado();
        if (estadoActual == null || estadoActual != 1L) {
            throw new Exception("Solo se pueden marcar como enviadas las generaciones en estado GENERADO");
        }
        
        generacion.setEstado(2L); // 2=ENVIADO
        generacion.setFechaEnvio(LocalDate.now());
        generacion.setUsuarioModificacion(usuario);
        generacion.setFechaModificacion(LocalDate.now());
        
        return actualizar(generacion);
    }

    @Override
    public GeneracionArchivoPetro marcarProcesado(Long codigoGeneracion, String usuario) throws Exception {
        System.out.println("Service: Marcando como procesado la generación " + codigoGeneracion);
        
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        Long estadoActual = generacion.getEstado();
        if (estadoActual == null || estadoActual != 2L) {
            throw new Exception("Solo se pueden marcar como procesadas las generaciones en estado ENVIADO");
        }
        
        generacion.setEstado(3L); // 3=PROCESADO
        generacion.setUsuarioModificacion(usuario);
        generacion.setFechaModificacion(LocalDate.now());
        
        return actualizar(generacion);
    }

    @Override
    public GeneracionArchivoPetro marcarDescargado(Long codigoGeneracion, String usuario) throws Exception {
        System.out.println("Service: Marcando como descargada la generación " + codigoGeneracion);

        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }

        // La primera descarga es la que cuenta: si vuelven a bajar el archivo se
        // conserva la marca original para no perder la auditoría.
        if (generacion.getFechaDescarga() != null) {
            System.out.println("La generación ya estaba marcada como descargada el " + generacion.getFechaDescarga()
                + " por " + generacion.getUsuarioDescarga());
            return generacion;
        }

        generacion.setFechaDescarga(LocalDate.now());
        generacion.setUsuarioDescarga(usuario);
        generacion.setUsuarioModificacion(usuario);
        generacion.setFechaModificacion(LocalDate.now());

        return actualizar(generacion);
    }

    // ========================================================================
    // ELIMINACIÓN DE UNA GENERACIÓN
    // ========================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<String, Object> eliminarGeneracion(Long codigoGeneracion, String usuario) throws Exception {
        System.out.println("=== SERVICIO: ELIMINANDO GENERACIÓN PETROCOMERCIAL ===");
        System.out.println("Código Generación: " + codigoGeneracion + " - Usuario: " + usuario);

        if (codigoGeneracion == null) {
            throw new Exception("Debe indicar el código de la generación a eliminar");
        }

        // selectById usa getSingleResult: si no hay fila lanza NoResultException.
        GeneracionArchivoPetro generacion;
        try {
            generacion = buscarPorId(codigoGeneracion);
        } catch (Throwable e) {
            throw new Exception("Generación no encontrada con ID: " + codigoGeneracion);
        }
        if (generacion == null) {
            throw new Exception("Generación no encontrada con ID: " + codigoGeneracion);
        }

        validarEliminacion(generacion);

        String nombreArchivo = generacion.getNombreArchivo();
        String rutaArchivo = generacion.getRutaArchivo();
        Long mes = generacion.getMesPeriodo();
        Long anio = generacion.getAnioPeriodo();

        // Borrado de abajo hacia arriba: CXPG -> PDGA -> DTGA -> GNAP.
        // Se usan deletes masivos con subconsulta porque las relaciones no
        // declaran cascada y las FK impiden borrar la cabecera primero.
        int cuotasEliminadas = em.createQuery(
                "DELETE FROM CuotaXParticipeGeneracion c " +
                "WHERE c.participeDetalleGeneracion IN (" +
                "   SELECT p FROM ParticipeDetalleGeneracionArchivo p " +
                "   WHERE p.detalleGeneracionArchivo IN (" +
                "      SELECT d FROM DetalleGeneracionArchivo d " +
                "      WHERE d.generacionArchivoPetro.codigo = :codigoGeneracion))")
            .setParameter("codigoGeneracion", codigoGeneracion)
            .executeUpdate();

        int participesEliminados = em.createQuery(
                "DELETE FROM ParticipeDetalleGeneracionArchivo p " +
                "WHERE p.detalleGeneracionArchivo IN (" +
                "   SELECT d FROM DetalleGeneracionArchivo d " +
                "   WHERE d.generacionArchivoPetro.codigo = :codigoGeneracion)")
            .setParameter("codigoGeneracion", codigoGeneracion)
            .executeUpdate();

        int detallesEliminados = em.createQuery(
                "DELETE FROM DetalleGeneracionArchivo d " +
                "WHERE d.generacionArchivoPetro.codigo = :codigoGeneracion")
            .setParameter("codigoGeneracion", codigoGeneracion)
            .executeUpdate();

        System.out.println("Registros eliminados -> CXPG: " + cuotasEliminadas
            + ", PDGA: " + participesEliminados + ", DTGA: " + detallesEliminados);

        try {
            dao.remove(generacion, codigoGeneracion);
        } catch (Throwable e) {
            throw new Exception("Error al eliminar la generación: " + e.getMessage(), e);
        }

        // El archivo se borra al final: si algo falla antes, la transacción hace
        // rollback y el TXT sigue en disco junto con sus registros.
        boolean archivoEliminado = eliminarArchivoFisico(rutaArchivo);

        System.out.println("=== GENERACIÓN " + codigoGeneracion + " ELIMINADA ===");

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("success", true);
        respuesta.put("mensaje", "Generación eliminada exitosamente. El periodo "
            + mes + "/" + anio + " puede volver a generarse.");
        respuesta.put("codigoGeneracion", codigoGeneracion);
        respuesta.put("cuotasEliminadas", cuotasEliminadas);
        respuesta.put("participesEliminados", participesEliminados);
        respuesta.put("detallesEliminados", detallesEliminados);
        respuesta.put("nombreArchivo", nombreArchivo);
        respuesta.put("archivoEliminado", archivoEliminado);

        return respuesta;
    }

    /**
     * Reglas de negocio para poder eliminar una generación.
     *
     * No se elimina si el archivo TXT ya fue descargado (salió del sistema y
     * pudo entregarse a Petrocomercial), ni si la generación ya fue marcada
     * como ENVIADA o PROCESADA.
     */
    private void validarEliminacion(GeneracionArchivoPetro generacion) throws Exception {
        if (generacion.getFechaDescarga() != null) {
            throw new Exception("No se puede eliminar la generación: el archivo ya fue descargado el "
                + generacion.getFechaDescarga()
                + (generacion.getUsuarioDescarga() != null ? " por " + generacion.getUsuarioDescarga() : "") + ".");
        }

        Long estado = generacion.getEstado();
        if (estado != null && estado == 2L) {
            throw new Exception("No se puede eliminar la generación: ya fue marcada como ENVIADA a Petrocomercial.");
        }
        if (estado != null && estado == 3L) {
            throw new Exception("No se puede eliminar la generación: ya fue marcada como PROCESADA.");
        }
    }

    /**
     * Borra del disco el archivo TXT de la generación.
     *
     * Que el archivo ya no esté no es un error: la generación pudo no haber
     * llegado a generarlo, o pudo borrarse a mano.
     *
     * @return true si el archivo existía y se eliminó
     */
    private boolean eliminarArchivoFisico(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
            return false;
        }

        try {
            File archivo = new File(rutaArchivo);
            if (!archivo.exists()) {
                System.out.println("El archivo " + rutaArchivo + " ya no existe en disco");
                return false;
            }

            boolean eliminado = archivo.delete();
            System.out.println(eliminado
                ? "Archivo eliminado del disco: " + rutaArchivo
                : "ADVERTENCIA: no se pudo eliminar el archivo " + rutaArchivo);
            return eliminado;
        } catch (Throwable e) {
            // El registro ya se borró; dejar el TXT huérfano no justifica tumbar la operación.
            System.err.println("ADVERTENCIA: error al eliminar el archivo " + rutaArchivo + ": " + e.getMessage());
            return false;
        }
    }

    // ========================================================================
    // CONSULTAS Y REPORTES
    // ========================================================================

    @Override
    public Map<String, Object> obtenerDetalle(Long codigoGeneracion) throws Exception {
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        List<DetalleGeneracionArchivo> detalles;
        try {
            detalles = detalleService.listarPorGeneracion(codigoGeneracion);
        } catch (Throwable e) {
            throw new Exception("Error al listar detalles: " + e.getMessage(), e);
        }
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("generacion", generacion);
        respuesta.put("detalles", detalles);
        
        return respuesta;
    }

    @Override
    public Map<String, Object> obtenerEstadisticas(Long codigoGeneracion) throws Exception {
        System.out.println("Service: Obteniendo estadísticas de generación " + codigoGeneracion);
        
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        List<DetalleGeneracionArchivo> detalles;
        try {
            detalles = detalleService.listarPorGeneracion(codigoGeneracion);
        } catch (Throwable e) {
            throw new Exception("Error al listar detalles: " + e.getMessage(), e);
        }
        
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("codigoGeneracion", codigoGeneracion);
        estadisticas.put("periodo", generacion.getMesPeriodo() + "/" + generacion.getAnioPeriodo());
        estadisticas.put("estado", generacion.getEstado());
        estadisticas.put("totalRegistros", generacion.getTotalRegistros());
        estadisticas.put("totalMonto", generacion.getTotalMontoEnviado());
        
        // Detalles por producto
        List<Map<String, Object>> detallesPorProducto = new ArrayList<>();
        double totalAportes = 0.0;
        double totalPrestamos = 0.0;
        
        for (DetalleGeneracionArchivo detalle : detalles) {
            Map<String, Object> detalleMap = new HashMap<>();
            detalleMap.put("codigoProducto", detalle.getCodigoProductoPetro());
            detalleMap.put("descripcion", detalle.getDescripcionProducto());
            detalleMap.put("totalRegistros", detalle.getTotalRegistros());
            detalleMap.put("totalMonto", detalle.getTotalMonto());
            
            detallesPorProducto.add(detalleMap);
            
            if ("AH".equals(detalle.getCodigoProductoPetro())) {
                totalAportes = detalle.getTotalMonto();
            } else {
                totalPrestamos += detalle.getTotalMonto();
            }
        }
        
        estadisticas.put("detallesPorProducto", detallesPorProducto);
        estadisticas.put("totalAportes", totalAportes);
        estadisticas.put("totalPrestamos", totalPrestamos);
        
        return estadisticas;
    }

    @Override
    public String regenerarArchivo(Long codigoGeneracion) throws Exception {
        System.out.println("Service: Regenerando archivo TXT para generación " + codigoGeneracion);
        
        GeneracionArchivoPetro generacion = buscarPorId(codigoGeneracion);
        if (generacion == null) {
            throw new Exception("Generación no encontrada");
        }
        
        // Obtener todos los detalles
        List<DetalleGeneracionArchivo> detalles;
        try {
            detalles = detalleService.listarPorGeneracion(codigoGeneracion);
        } catch (Throwable e) {
            throw new Exception("Error al listar detalles: " + e.getMessage(), e);
        }
        
        Long codigoFilial = obtenerCodigoFilial(generacion);

        Map<String, List<LineaArchivo>> datosPorProducto = new LinkedHashMap<>();
        datosPorProducto.put("AH", new ArrayList<>());
        datosPorProducto.put("HS", new ArrayList<>());
        datosPorProducto.put("PE", new ArrayList<>());
        datosPorProducto.put("PH", new ArrayList<>());
        datosPorProducto.put("PQ", new ArrayList<>());
        datosPorProducto.put("PP", new ArrayList<>());

        // Reconstruir datos desde la base de datos
        for (DetalleGeneracionArchivo detalle : detalles) {
            List<ParticipeDetalleGeneracionArchivo> participes;
            try {
                participes = participeDetalleService.listarPorDetalle(detalle.getCodigo());
            } catch (Throwable e) {
                throw new Exception("Error al listar partícipes: " + e.getMessage(), e);
            }

            List<LineaArchivo> lineasProducto = datosPorProducto.get(detalle.getCodigoProductoPetro());
            if (lineasProducto == null) continue;

            for (ParticipeDetalleGeneracionArchivo participe : participes) {
                LineaArchivo linea = new LineaArchivo();
                linea.codigoEntidad = participe.getEntidad().getCodigo();
                linea.rolPetrocomercial = participe.getRolPetrocomercial();
                linea.numeroIdentificacion = participe.getEntidad().getNumeroIdentificacion();
                linea.razonSocial = participe.getEntidad().getRazonSocial();
                linea.monto = participe.getMontoEnviado();
                linea.codigoPrestamo = participe.getPrestamo() != null ? participe.getPrestamo().getCodigo() : null;

                // El aporte se guarda sumado en PDGA; el desglose jubilación /
                // cesantía que necesitan las columnas AJ y AC está en CXPG.
                if ("AH".equals(detalle.getCodigoProductoPetro())) {
                    recuperarDesgloseAporte(participe, linea);
                }

                lineasProducto.add(linea);
            }
        }

        // Mismo orden que en la generación original
        for (List<LineaArchivo> lista : datosPorProducto.values()) {
            lista.sort((a, b) -> compararParticipes(a, b, codigoFilial));
        }

        // Generar archivo
        String nombreArchivo = generacion.getNombreArchivo();
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            nombreArchivo = generarNombreArchivo(generacion.getMesPeriodo(), generacion.getAnioPeriodo(), codigoFilial);
        }

        String rutaArchivo = generarArchivoTXT(datosPorProducto,
            generacion.getMesPeriodo(),
            generacion.getAnioPeriodo(),
            nombreArchivo,
            codigoFilial);

        System.out.println("Archivo regenerado en: " + rutaArchivo);
        return rutaArchivo;
    }

    /**
     * Rellena montoJubilacion y montoCesantia de una línea de aporte leyendo
     * los CXPG del partícipe (tipo de aporte 9 = jubilación, 11 = cesantía).
     *
     * Si no hay CXPG se deja todo en jubilación para no perder el valor: es
     * preferible una columna mal clasificada a un archivo con el total en cero.
     */
    private void recuperarDesgloseAporte(ParticipeDetalleGeneracionArchivo participe, LineaArchivo linea) {
        final Long TIPO_APORTE_JUBILACION = 9L;
        final Long TIPO_APORTE_CESANTIA = 11L;

        try {
            List<CuotaXParticipeGeneracion> cuotas = cuotaXParticipeService.listarPorParticipe(participe.getCodigo());
            if (cuotas == null || cuotas.isEmpty()) {
                linea.montoJubilacion = linea.monto;
                linea.montoCesantia = 0.0;
                return;
            }

            double jubilacion = 0.0;
            double cesantia = 0.0;
            for (CuotaXParticipeGeneracion cuota : cuotas) {
                double valor = cuota.getValorCuota() != null ? cuota.getValorCuota() : 0.0;
                Long tipoAporte = cuota.getTipoAporte() != null ? cuota.getTipoAporte().getCodigo() : null;

                if (TIPO_APORTE_CESANTIA.equals(tipoAporte)) {
                    cesantia += valor;
                } else if (TIPO_APORTE_JUBILACION.equals(tipoAporte)) {
                    jubilacion += valor;
                }
            }

            linea.montoJubilacion = jubilacion;
            linea.montoCesantia = cesantia;

        } catch (Throwable e) {
            System.err.println("ADVERTENCIA: no se pudo recuperar el desglose del aporte del partícipe "
                + participe.getCodigo() + ": " + e.getMessage());
            linea.montoJubilacion = linea.monto;
            linea.montoCesantia = 0.0;
        }
    }

    // ========================================================================
    // MÉTODOS PRIVADOS - LÓGICA INTERNA
    // ========================================================================

    private Map<String, List<LineaArchivo>> recopilarDatos(Long mes, Long anio, Long codigoFilial) throws Exception {
        System.out.println("Recopilando datos para periodo: " + mes + "/" + anio + " - Filial: " + codigoFilial);

        Map<String, List<LineaArchivo>> datosPorProducto = new LinkedHashMap<>();
        datosPorProducto.put("AH", new ArrayList<>());
        datosPorProducto.put("HS", new ArrayList<>());
        datosPorProducto.put("PE", new ArrayList<>());
        datosPorProducto.put("PH", new ArrayList<>());
        datosPorProducto.put("PQ", new ArrayList<>());
        datosPorProducto.put("PP", new ArrayList<>());

        // 1. Aportes personales
        recopilarAportes(datosPorProducto.get("AH"), mes, anio, codigoFilial);

        // 2. Cuotas de préstamos
        recopilarPrestamos(mes, anio, datosPorProducto, codigoFilial);

        // Petrocomercial identifica al partícipe por el rol; ARCH por la cédula.
        for (List<LineaArchivo> lista : datosPorProducto.values()) {
            lista.sort((a, b) -> compararParticipes(a, b, codigoFilial));
        }

        return datosPorProducto;
    }

    /**
     * Orden de los partícipes dentro del archivo: por rol en Petrocomercial y
     * por número de identificación en el resto de filiales.
     */
    private int compararParticipes(LineaArchivo a, LineaArchivo b, Long codigoFilial) {
        if (esFilialPetrocomercial(codigoFilial)) {
            return nullSafeLong(a.rolPetrocomercial).compareTo(nullSafeLong(b.rolPetrocomercial));
        }
        return nullSafeTexto(a.numeroIdentificacion).compareTo(nullSafeTexto(b.numeroIdentificacion));
    }

    private Long nullSafeLong(Long valor) {
        return valor != null ? valor : 0L;
    }

    private String nullSafeTexto(String valor) {
        return valor != null ? valor : "";
    }

    /**
     * Indica si la filial usa el formato posicional histórico de Petrocomercial.
     */
    private boolean esFilialPetrocomercial(Long codigoFilial) {
        return codigoFilial == null || codigoFilial == Filiales.PETROCOMERCIAL;
    }

    /**
     * Condición JPQL que deja fuera a los partícipes sin el identificador que
     * exige la filial: el rol en Petrocomercial, la cédula/RUC en las demás.
     *
     * @param alias Ruta JPQL hasta la Entidad (ej: "h.entidad")
     */
    private String condicionIdentificadorFilial(String alias, Long codigoFilial) {
        if (esFilialPetrocomercial(codigoFilial)) {
            return "AND " + alias + ".rolPetroComercial IS NOT NULL " +
                   "AND " + alias + ".rolPetroComercial > 0 ";
        }
        // En Oracle '' es NULL, así que no sirve comparar contra cadena vacía.
        return "AND " + alias + ".numeroIdentificacion IS NOT NULL " +
               "AND LENGTH(TRIM(" + alias + ".numeroIdentificacion)) > 0 ";
    }

    /**
     * Recopila los aportes personales (producto AH) del periodo.
     *
     * Incluye a los partícipes ACTIVOS y a los que están en ACTIVO EN MORA.
     * A los primeros se les cobra un mes; a los morosos se les cobra la deuda
     * acumulada: aporte mensual x meses transcurridos desde su último aporte
     * hasta el periodo que se está generando, ese periodo incluido.
     *
     * Ejemplo: último aporte en abril, generando agosto -> 4 meses
     * (mayo, junio, julio y agosto). No hay tope de meses.
     *
     * Los montos de jubilación y cesantía se guardan por separado porque ARCH
     * los reporta en columnas distintas (AJ y AC).
     *
     * @param listaAportes  Lista donde se acumulan las líneas del producto AH
     * @param mes           Mes del periodo que se genera
     * @param anio          Año del periodo que se genera
     * @param codigoFilial  Filial de la generación
     */
    private void recopilarAportes(List<LineaArchivo> listaAportes, Long mes, Long anio, Long codigoFilial) throws Exception {
        System.out.println("Recopilando aportes personales de la filial " + codigoFilial + "...");
        System.out.println("Filtros: Entidad en estado ACTIVO o ACTIVO EN MORA, HistorialSueldo.estado=99");

        String jpql = "SELECT h FROM HistorialSueldo h " +
                     "WHERE h.entidad.idEstado IN :estadosIncluidos " +
                     "AND h.estado = 99 " +
                     "AND h.entidad.filial.codigo = :codigoFilial " +
                     condicionIdentificadorFilial("h.entidad", codigoFilial) +
                     "ORDER BY h.entidad.codigo, h.fechaIngreso DESC";

        Query query = em.createQuery(jpql);
        query.setParameter("estadosIncluidos", Arrays.asList(
                (long) EstadoParticipeEntidad.ACTIVO,
                (long) EstadoParticipeEntidad.ACTIVO_EN_MORA));
        query.setParameter("codigoFilial", codigoFilial);

        @SuppressWarnings("unchecked")
        List<HistorialSueldo> resultados = query.getResultList();

        System.out.println("Registros HistorialSueldo encontrados: " + resultados.size());

        // Un HistorialSueldo por entidad: el primero de cada grupo (van ordenados
        // por fechaIngreso DESC, así que es el vigente).
        List<HistorialSueldo> historialPorEntidad = new ArrayList<>();
        Long ultimaEntidad = null;
        for (HistorialSueldo historial : resultados) {
            if (ultimaEntidad == null || !ultimaEntidad.equals(historial.getEntidad().getCodigo())) {
                historialPorEntidad.add(historial);
            }
            ultimaEntidad = historial.getEntidad().getCodigo();
        }

        Map<Long, Long> mesesACobrarPorEntidad = calcularMesesACobrarMorosos(historialPorEntidad, mes, anio);

        for (HistorialSueldo historial : historialPorEntidad) {
            Long codigoEntidad = historial.getEntidad().getCodigo();

            Double montoJubilacion = historial.getMontoJubilacion() != null ? historial.getMontoJubilacion() : 0.0;
            Double montoCesantia = historial.getMontoCesantia() != null ? historial.getMontoCesantia() : 0.0;

            // Para los morosos se multiplica por los meses adeudados; para el
            // resto el multiplicador es 1 y el comportamiento no cambia.
            long mesesACobrar = mesesACobrarPorEntidad.getOrDefault(codigoEntidad, 1L);
            if (mesesACobrar > 1L) {
                montoJubilacion = montoJubilacion * mesesACobrar;
                montoCesantia = montoCesantia * mesesACobrar;
                System.out.println("   [MORA] Entidad " + codigoEntidad + " (rol "
                    + historial.getEntidad().getRolPetroComercial() + "): se cobran "
                    + mesesACobrar + " meses de aporte.");
            }

            Double montoTotal = montoJubilacion + montoCesantia;

            if (montoTotal > 0) {
                LineaArchivo linea = new LineaArchivo();
                linea.codigoEntidad = codigoEntidad;
                linea.rolPetrocomercial = historial.getEntidad().getRolPetroComercial();
                linea.numeroIdentificacion = historial.getEntidad().getNumeroIdentificacion();
                linea.razonSocial = historial.getEntidad().getRazonSocial();
                linea.monto = montoTotal;
                linea.codigoPrestamo = null;
                // Almacenar montos por separado para crear registros CXPG individuales
                // y para las columnas AJ / AC del archivo de ARCH
                linea.montoJubilacion = montoJubilacion;
                linea.montoCesantia = montoCesantia;

                listaAportes.add(linea);
            }
        }

        System.out.println("Aportes recopilados: " + listaAportes.size());
    }

    /**
     * Calcula, para los partícipes en ACTIVO EN MORA, cuántos meses de aporte hay
     * que cobrarles en este periodo.
     *
     * Son los meses transcurridos entre su último aporte y el periodo que se
     * genera, ese periodo incluido. Un partícipe que aportó el mes pasado da 1,
     * que es el comportamiento normal.
     *
     * Si no se le encuentra ningún aporte previo no se puede calcular la deuda,
     * así que se le cobra un solo mes y se deja constancia en el log.
     *
     * @return Mapa codigoEntidad -> meses a cobrar. Solo contiene morosos.
     */
    private Map<Long, Long> calcularMesesACobrarMorosos(List<HistorialSueldo> historiales,
            Long mes, Long anio) throws Exception {

        Map<Long, Long> resultado = new LinkedHashMap<>();

        List<Long> codigosMorosos = new ArrayList<>();
        for (HistorialSueldo h : historiales) {
            Long estado = h.getEntidad().getIdEstado();
            if (estado != null && estado == EstadoParticipeEntidad.ACTIVO_EN_MORA) {
                codigosMorosos.add(h.getEntidad().getCodigo());
            }
        }

        if (codigosMorosos.isEmpty()) {
            return resultado;
        }
        System.out.println("Partícipes en ACTIVO EN MORA a incluir: " + codigosMorosos.size());

        java.time.YearMonth periodoGeneracion = java.time.YearMonth.of(anio.intValue(), mes.intValue());
        // Solo aportes anteriores al periodo que se genera.
        java.time.LocalDateTime corte = periodoGeneracion.atDay(1).atStartOfDay();

        List<Object[]> ultimasFechas;
        try {
            ultimasFechas = aporteDaoService.selectUltimaFechaAportePorEntidad(
                codigosMorosos,
                Arrays.asList(TIPO_APORTE_JUBILACION, TIPO_APORTE_CESANTIA),
                corte);
        } catch (Throwable e) {
            throw new Exception("Error al calcular la deuda de los partícipes en mora: " + e.getMessage(), e);
        }

        Map<Long, java.time.YearMonth> ultimoMesPorEntidad = new LinkedHashMap<>();
        for (Object[] fila : ultimasFechas) {
            Long codigoEntidad = ((Number) fila[0]).longValue();
            java.time.LocalDateTime ultimaFecha = (java.time.LocalDateTime) fila[1];
            if (ultimaFecha != null) {
                ultimoMesPorEntidad.put(codigoEntidad, java.time.YearMonth.from(ultimaFecha));
            }
        }

        for (Long codigoEntidad : codigosMorosos) {
            java.time.YearMonth ultimoMes = ultimoMesPorEntidad.get(codigoEntidad);

            if (ultimoMes == null) {
                System.out.println("   [MORA] Entidad " + codigoEntidad
                    + " no registra aportes previos: no se puede calcular la deuda, se cobra 1 mes.");
                resultado.put(codigoEntidad, 1L);
                continue;
            }

            long meses = java.time.temporal.ChronoUnit.MONTHS.between(ultimoMes, periodoGeneracion);
            // Nunca menos de un mes: el aporte del periodo que se genera siempre se cobra.
            resultado.put(codigoEntidad, Math.max(1L, meses));
        }

        return resultado;
    }

    /**
     * Recopila las cuotas de préstamo a descontar en el periodo.
     *
     * Se traen TODAS las cuotas no pagadas con vencimiento hasta el fin del mes
     * que se genera —la del mes y las atrasadas— y se agrupan por préstamo en
     * una sola línea del archivo.
     *
     * De cada cuota se envía el SALDO PENDIENTE, no el valor original: una
     * cuota PARCIAL solo cobra lo que le falta. Volver a mandar el valor
     * completo cobraría dos veces lo ya pagado.
     */
    private void recopilarPrestamos(Long mes, Long anio, Map<String, List<LineaArchivo>> datosPorProducto,
                                    Long codigoFilial) throws Exception {
        System.out.println("Recopilando cuotas de préstamos de la filial " + codigoFilial + "...");
        System.out.println("Filtros: Entidad en estado ACTIVO, Préstamo en estado GENERADO, VIGENTE o EN MORA");
        System.out.println("Incluyendo cuotas en estado: PENDIENTE, ACTIVA, EMITIDA, EN_MORA, PARCIAL, VENCIDA y sin estado");
        System.out.println("Excluyendo cuotas en estado: 4 (PAGADA), 7 (CANCELADA_ANTICIPADA)");

        final Long ESTADO_PAGADA = 4L;
        final Long ESTADO_CANCELADA_ANTICIPADA = 7L;

        // Un préstamo EN MORA sigue debiendo: sus cuotas deben seguir yendo al
        // archivo igual que las de un préstamo vigente.
        final List<Long> ESTADOS_PRESTAMO_A_COBRAR = Arrays.asList(
                (long) EstadoPrestamo.GENERADO,
                (long) EstadoPrestamo.VIGENTE,
                (long) EstadoPrestamo.EN_MORA);

        // Seguros de incendio acumulados por entidad (producto HS)
        Map<Long, LineaArchivo> segurosPorEntidad = new LinkedHashMap<>();

        LocalDate finMesDate;
        try {
            finMesDate = fechaService.ultimoDiaMesAnioLocal(mes, anio);
        } catch (Throwable e) {
            throw new Exception("Error al calcular fechas del periodo: " + e.getMessage(), e);
        }

        LocalDateTime finMes = finMesDate.atTime(23, 59, 59);

        // Todas las cuotas vencidas o que vencen en el periodo y siguen debiendo.
        // Antes se traían solo las del mes y las anteriores se buscaban con una
        // consulta por préstamo, lo que dejaba fuera a los préstamos que ya no
        // tienen cuota en el mes generado pero sí saldos atrasados.
        // dp.estado IS NULL entra: en Oracle un NOT IN contra NULL descarta la fila.
        String jpql = "SELECT dp FROM DetallePrestamo dp " +
                     "WHERE dp.fechaVencimiento <= :finMes " +
                     "AND dp.prestamo.idEstado IN :estadosPrestamo " +
                     "AND dp.prestamo.entidad.idEstado = :estadoActivo " +
                     "AND dp.prestamo.entidad.filial.codigo = :codigoFilial " +
                     "AND dp.prestamo.producto.codigoPetro IS NOT NULL " +
                     condicionIdentificadorFilial("dp.prestamo.entidad", codigoFilial) +
                     "AND (dp.estado IS NULL OR dp.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada)) " +
                     "ORDER BY dp.prestamo.codigo, dp.numeroCuota";

        Query query = em.createQuery(jpql);
        query.setParameter("finMes", finMes);
        query.setParameter("estadoActivo", (long) EstadoParticipeEntidad.ACTIVO);
        query.setParameter("codigoFilial", codigoFilial);
        query.setParameter("estadosPrestamo", ESTADOS_PRESTAMO_A_COBRAR);
        query.setParameter("estadoPagada", ESTADO_PAGADA);
        query.setParameter("estadoCanceladaAnticipada", ESTADO_CANCELADA_ANTICIPADA);

        @SuppressWarnings("unchecked")
        List<DetallePrestamo> cuotasPendientes = query.getResultList();

        System.out.println("Cuotas pendientes hasta " + finMesDate + ": " + cuotasPendientes.size());

        // Lo ya pagado de cada cuota, tomado de PagoPrestamo (misma fuente que
        // usa el proceso de carga para decidir el saldo real).
        Map<Long, PagosCuota> pagosPorCuota = obtenerPagosPorCuota(cuotasPendientes);

        // Una línea por préstamo, con la suma de los saldos de sus cuotas
        Map<Long, LineaArchivo> lineasPorPrestamo = new LinkedHashMap<>();
        Map<Long, String> productoPorPrestamo = new LinkedHashMap<>();
        int cuotasParcialesIncluidas = 0;
        int cuotasSinSaldo = 0;

        for (DetallePrestamo cuota : cuotasPendientes) {
            Long codigoPrestamo = cuota.getPrestamo().getCodigo();
            String codigoProductoPetro = cuota.getPrestamo().getProducto().getCodigoPetro();

            if (codigoProductoPetro == null || codigoProductoPetro.trim().isEmpty()) {
                continue;
            }
            if (!datosPorProducto.containsKey(codigoProductoPetro)) {
                System.err.println("ADVERTENCIA: Código producto Petro no reconocido: " + codigoProductoPetro);
                continue;
            }

            PagosCuota pagos = pagosPorCuota.get(cuota.getCodigo());
            double saldoCuota = calcularSaldoCuota(cuota, pagos);

            // Cuota sin saldo: pagada aunque el estado diga otra cosa.
            if (saldoCuota <= 0.01) {
                cuotasSinSaldo++;
            } else {
                LineaArchivo linea = lineasPorPrestamo.get(codigoPrestamo);
                if (linea == null) {
                    Entidad entidadPrestamo = cuota.getPrestamo().getEntidad();
                    linea = new LineaArchivo();
                    linea.codigoEntidad = entidadPrestamo.getCodigo();
                    linea.rolPetrocomercial = entidadPrestamo.getRolPetroComercial();
                    linea.numeroIdentificacion = entidadPrestamo.getNumeroIdentificacion();
                    linea.razonSocial = entidadPrestamo.getRazonSocial();
                    linea.monto = 0.0;
                    linea.codigoPrestamo = codigoPrestamo;
                    lineasPorPrestamo.put(codigoPrestamo, linea);
                    productoPorPrestamo.put(codigoPrestamo, codigoProductoPetro);
                }

                linea.monto += saldoCuota;
                linea.cuotasSumadas.add(new CuotaInfo(
                    cuota.getNumeroCuota() != null ? cuota.getNumeroCuota().intValue() : 0,
                    saldoCuota
                ));

                if (esCuotaParcial(cuota, pagos)) {
                    cuotasParcialesIncluidas++;
                    System.out.println("  [PARCIAL] Préstamo " + codigoPrestamo + " cuota #"
                        + (cuota.getNumeroCuota() != null ? cuota.getNumeroCuota().intValue() : 0)
                        + ": se cobra el saldo $" + String.format("%.2f", saldoCuota)
                        + " (cuota de $" + String.format("%.2f", calcularMontoCuota(cuota)) + ")");
                }
            }

            // Seguro de incendio (producto HS) para PH y PP, también por saldo
            if ("PH".equals(codigoProductoPetro) || "PP".equals(codigoProductoPetro)) {
                acumularSeguroIncendio(segurosPorEntidad, cuota.getPrestamo().getEntidad(),
                    codigoPrestamo, cuota, saldoSeguroIncendio(cuota, pagos));
            }
        }

        // Volcar las líneas a su producto
        for (Map.Entry<Long, LineaArchivo> entrada : lineasPorPrestamo.entrySet()) {
            LineaArchivo linea = entrada.getValue();
            if (linea.monto > 0) {
                datosPorProducto.get(productoPorPrestamo.get(entrada.getKey())).add(linea);
            }
        }

        System.out.println("Préstamos con saldo a cobrar: " + lineasPorPrestamo.size()
            + " | Cuotas parciales incluidas: " + cuotasParcialesIncluidas
            + " | Cuotas sin saldo omitidas: " + cuotasSinSaldo);

        // Agregar los seguros de incendio acumulados al producto HS
        List<LineaArchivo> listaHS = datosPorProducto.get("HS");
        for (LineaArchivo seguro : segurosPorEntidad.values()) {
            if (seguro.monto > 0) {
                listaHS.add(seguro);
            }
        }

        if (!listaHS.isEmpty()) {
            System.out.println("Seguros de Incendio (HS): " + listaHS.size() + " registros, Total: $" +
                             String.format("%.2f", listaHS.stream().mapToDouble(l -> l.monto).sum()));
        }

        for (Map.Entry<String, List<LineaArchivo>> entry : datosPorProducto.entrySet()) {
            if (!entry.getKey().equals("AH") && !entry.getValue().isEmpty()) {
                System.out.println("Préstamos " + entry.getKey() + ": " + entry.getValue().size() + " registros");
            }
        }

        System.out.println("Préstamos recopilados exitosamente");
    }

    /**
     * Lo ya pagado de cada cuota, agrupado desde PagoPrestamo (CRD.PGPR).
     *
     * Se hace en bloques con una consulta agregada por bloque para no disparar
     * una consulta por cuota, y porque Oracle no admite más de 1000 elementos
     * en un IN.
     *
     * @return Mapa codigoCuota -> pagos acumulados. Las cuotas sin pagos no aparecen.
     */
    private Map<Long, PagosCuota> obtenerPagosPorCuota(List<DetallePrestamo> cuotas) {
        Map<Long, PagosCuota> resultado = new LinkedHashMap<>();

        if (cuotas == null || cuotas.isEmpty()) {
            return resultado;
        }

        final int TAMANIO_BLOQUE = 500;
        List<Long> codigos = new ArrayList<>();
        for (DetallePrestamo cuota : cuotas) {
            codigos.add(cuota.getCodigo());
        }

        String jpql = "SELECT p.detallePrestamo.codigo, " +
                     "SUM(p.capitalPagado), SUM(p.interesPagado), SUM(p.desgravamen), " +
                     "SUM(p.moraPagada), SUM(p.interesVencidoPagado), SUM(p.valorSeguroIncendio) " +
                     "FROM PagoPrestamo p " +
                     "WHERE p.detallePrestamo.codigo IN :codigos " +
                     "GROUP BY p.detallePrestamo.codigo";

        for (int inicio = 0; inicio < codigos.size(); inicio += TAMANIO_BLOQUE) {
            List<Long> bloque = codigos.subList(inicio, Math.min(inicio + TAMANIO_BLOQUE, codigos.size()));

            try {
                Query query = em.createQuery(jpql);
                query.setParameter("codigos", bloque);

                @SuppressWarnings("unchecked")
                List<Object[]> filas = query.getResultList();

                for (Object[] fila : filas) {
                    PagosCuota pagos = new PagosCuota();
                    pagos.capital = valorNumerico(fila[1]);
                    pagos.interes = valorNumerico(fila[2]);
                    pagos.desgravamen = valorNumerico(fila[3]);
                    pagos.mora = valorNumerico(fila[4]);
                    pagos.interesVencido = valorNumerico(fila[5]);
                    pagos.seguroIncendio = valorNumerico(fila[6]);

                    resultado.put(((Number) fila[0]).longValue(), pagos);
                }

            } catch (Throwable e) {
                // Sin los pagos se cobraría la cuota completa otra vez: es mejor
                // cortar el proceso que generar un archivo que cobre de más.
                throw new RuntimeException("Error al obtener los pagos previos de las cuotas: " + e.getMessage(), e);
            }
        }

        System.out.println("Cuotas con pagos previos registrados: " + resultado.size());
        return resultado;
    }

    private double valorNumerico(Object valor) {
        return valor != null ? ((Number) valor).doubleValue() : 0.0;
    }

    /**
     * Saldo pendiente de una cuota: lo que falta por cobrar.
     *
     * Es el valor de la cuota menos lo ya pagado. Para una cuota sin pagos
     * previos coincide con el valor original; para una PARCIAL es el resto.
     *
     * No incluye el seguro de incendio, que viaja aparte en el producto HS.
     */
    private double calcularSaldoCuota(DetallePrestamo cuota, PagosCuota pagos) {
        double capital = nullSafeDouble(cuota.getCapital());
        double interes = nullSafeDouble(cuota.getInteres());
        double mora = nullSafeDouble(cuota.getMora());
        double interesVencido = nullSafeDouble(cuota.getInteresVencido());
        double desgravamen = nullSafeDouble(cuota.getDesgravamen());

        if (pagos == null) {
            return capital + interes + mora + interesVencido + desgravamen;
        }

        return Math.max(0, capital - pagos.capital)
             + Math.max(0, interes - pagos.interes)
             + Math.max(0, mora - pagos.mora)
             + Math.max(0, interesVencido - pagos.interesVencido)
             + Math.max(0, desgravamen - pagos.desgravamen);
    }

    /**
     * Saldo del seguro de incendio de una cuota (producto HS).
     */
    private double saldoSeguroIncendio(DetallePrestamo cuota, PagosCuota pagos) {
        double valorSeguro = nullSafeDouble(cuota.getValorSeguroIncendio());
        if (pagos == null) {
            return valorSeguro;
        }
        return Math.max(0, valorSeguro - pagos.seguroIncendio);
    }

    /**
     * Indica si la cuota viene con un pago parcial encima, ya sea por su estado
     * o porque tiene pagos registrados que no la cubren.
     */
    private boolean esCuotaParcial(DetallePrestamo cuota, PagosCuota pagos) {
        Long estado = cuota.getEstado();
        if (estado != null && estado == EstadoCuotaPrestamo.PARCIAL) {
            return true;
        }
        return pagos != null && (pagos.capital + pagos.interes + pagos.desgravamen
            + pagos.mora + pagos.interesVencido + pagos.seguroIncendio) > 0.01;
    }

    private double nullSafeDouble(Double valor) {
        return valor != null ? valor : 0.0;
    }

    /**
     * Valor original de una cuota (capital + interés + mora + interés vencido +
     * desgravamen), sin descontar pagos. Solo se usa para los mensajes de log.
     */
    private double calcularMontoCuota(DetallePrestamo cuota) {
        return nullSafeDouble(cuota.getCapital())
             + nullSafeDouble(cuota.getInteres())
             + nullSafeDouble(cuota.getMora())
             + nullSafeDouble(cuota.getInteresVencido())
             + nullSafeDouble(cuota.getDesgravamen());
    }

    private String generarArchivoTXT(Map<String, List<LineaArchivo>> datosPorProducto,
                                     Long mes, Long anio, String nombreArchivo, Long codigoFilial) throws Exception {
        System.out.println("Generando archivo TXT: " + nombreArchivo + " - Filial: " + codigoFilial);

        String rutaBase = System.getProperty("user.home") + File.separator + "archivos_petrocomercial";
        File directorio = new File(rutaBase);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        String rutaCompleta = rutaBase + File.separator + nombreArchivo;

        int ultimoDia = obtenerUltimoDiaMes(mes.intValue(), anio.intValue());
        String fechaProceso = String.format("%04d%02d%02d", anio, mes, ultimoDia);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaCompleta))) {
            if (esFilialPetrocomercial(codigoFilial)) {
                escribirArchivoPetrocomercial(writer, datosPorProducto, fechaProceso);
            } else {
                escribirArchivoPlanoColumnas(writer, datosPorProducto, mes, anio);
            }
        }

        System.out.println("Archivo generado en: " + rutaCompleta);
        return rutaCompleta;
    }

    /**
     * Formato Petrocomercial: registro posicional de 55 caracteres, una línea
     * por partícipe y producto, agrupadas por producto.
     */
    private void escribirArchivoPetrocomercial(BufferedWriter writer,
                                               Map<String, List<LineaArchivo>> datosPorProducto,
                                               String fechaProceso) throws Exception {
        for (String codigoProducto : ORDEN_PRODUCTOS) {
            List<LineaArchivo> lineas = datosPorProducto.get(codigoProducto);
            if (lineas == null) continue;

            for (LineaArchivo linea : lineas) {
                writer.write(formatearLinea(linea, fechaProceso, codigoProducto));
                writer.newLine();
            }
        }
    }

    /**
     * Formato ARCH: archivo plano separado por ';', una línea por partícipe y
     * una columna por producto.
     *
     * Columnas: IDENTIFICACION;RAZON SOCIAL;AC;AJ;PE;PH;HS;PQ;PP;TOTAL
     *
     * A diferencia de Petrocomercial, el aporte NO se envía sumado en AH: la
     * cesantía va en AC y la jubilación en AJ. Los valores salen como números
     * normales con dos decimales (sin multiplicar ni rellenar con ceros).
     *
     * La columna TOTAL es lo que se le descuenta al partícipe en el periodo, y
     * la última fila (marcada con TOTALES) trae los totales globales de cada
     * columna para poder cuadrar el archivo de un vistazo.
     *
     * Antes de los nombres de columna van dos filas de cabecera: el título
     * ASOPREP y el periodo que se está generando.
     */
    private void escribirArchivoPlanoColumnas(BufferedWriter writer,
                                              Map<String, List<LineaArchivo>> datosPorProducto,
                                              Long mes, Long anio) throws Exception {
        // Una fila por partícipe: se consolida todo lo recopilado por entidad.
        Map<Long, FilaColumnas> filasPorEntidad = new LinkedHashMap<>();

        for (String codigoProducto : ORDEN_PRODUCTOS) {
            List<LineaArchivo> lineas = datosPorProducto.get(codigoProducto);
            if (lineas == null) continue;

            for (LineaArchivo linea : lineas) {
                FilaColumnas fila = filasPorEntidad.get(linea.codigoEntidad);
                if (fila == null) {
                    fila = new FilaColumnas();
                    fila.numeroIdentificacion = nullSafeTexto(linea.numeroIdentificacion);
                    fila.razonSocial = nullSafeTexto(linea.razonSocial);
                    filasPorEntidad.put(linea.codigoEntidad, fila);
                }

                if ("AH".equals(codigoProducto)) {
                    // El aporte se reporta separado: cesantía en AC, jubilación en AJ.
                    fila.aporteCesantia += linea.montoCesantia != null ? linea.montoCesantia : 0.0;
                    fila.aporteJubilacion += linea.montoJubilacion != null ? linea.montoJubilacion : 0.0;
                } else {
                    // Un partícipe puede tener más de un préstamo del mismo tipo:
                    // la columna lleva la suma de todos.
                    Double montoActual = fila.montosPorProducto.get(codigoProducto);
                    fila.montosPorProducto.put(codigoProducto,
                        (montoActual != null ? montoActual : 0.0) + (linea.monto != null ? linea.monto : 0.0));
                }
            }
        }

        // Dos filas de cabecera antes de los nombres de columna
        writer.write(TITULO_ARCHIVO_PLANO);
        writer.newLine();
        writer.write("Fecha: " + nombreMes(mes) + " " + anio);
        writer.newLine();

        // Encabezado de columnas
        StringBuilder cabecera = new StringBuilder();
        cabecera.append("IDENTIFICACION").append(SEPARADOR_COLUMNAS);
        cabecera.append("RAZON SOCIAL").append(SEPARADOR_COLUMNAS);
        cabecera.append("AC").append(SEPARADOR_COLUMNAS);
        cabecera.append("AJ");
        for (String codigoProducto : ORDEN_COLUMNAS_PRESTAMOS) {
            cabecera.append(SEPARADOR_COLUMNAS).append(codigoProducto);
        }
        cabecera.append(SEPARADOR_COLUMNAS).append("TOTAL");
        writer.write(cabecera.toString());
        writer.newLine();

        // Ordenadas por número de identificación
        List<FilaColumnas> filas = new ArrayList<>(filasPorEntidad.values());
        filas.sort((a, b) -> a.numeroIdentificacion.compareTo(b.numeroIdentificacion));

        // Acumuladores para el registro de totales globales
        double totalCesantia = 0.0;
        double totalJubilacion = 0.0;
        Map<String, Double> totalPorProducto = new LinkedHashMap<>();
        double totalGeneral = 0.0;

        for (FilaColumnas fila : filas) {
            StringBuilder linea = new StringBuilder();
            linea.append(fila.numeroIdentificacion).append(SEPARADOR_COLUMNAS);
            linea.append(limpiarTexto(fila.razonSocial)).append(SEPARADOR_COLUMNAS);
            linea.append(formatearValor(fila.aporteCesantia)).append(SEPARADOR_COLUMNAS);
            linea.append(formatearValor(fila.aporteJubilacion));

            // Total a descontar al partícipe: aportes + todos sus productos
            double totalParticipe = fila.aporteCesantia + fila.aporteJubilacion;

            totalCesantia += fila.aporteCesantia;
            totalJubilacion += fila.aporteJubilacion;

            for (String codigoProducto : ORDEN_COLUMNAS_PRESTAMOS) {
                Double monto = fila.montosPorProducto.get(codigoProducto);
                double valor = monto != null ? monto : 0.0;

                linea.append(SEPARADOR_COLUMNAS).append(formatearValor(valor));

                totalParticipe += valor;
                totalPorProducto.put(codigoProducto,
                    (totalPorProducto.containsKey(codigoProducto) ? totalPorProducto.get(codigoProducto) : 0.0) + valor);
            }

            linea.append(SEPARADOR_COLUMNAS).append(formatearValor(totalParticipe));
            totalGeneral += totalParticipe;

            writer.write(linea.toString());
            writer.newLine();
        }

        // Registro final con los totales globales
        StringBuilder totales = new StringBuilder();
        totales.append(ETIQUETA_FILA_TOTALES).append(SEPARADOR_COLUMNAS);
        totales.append(filas.size()).append(" PARTICIPES").append(SEPARADOR_COLUMNAS);
        totales.append(formatearValor(totalCesantia)).append(SEPARADOR_COLUMNAS);
        totales.append(formatearValor(totalJubilacion));
        for (String codigoProducto : ORDEN_COLUMNAS_PRESTAMOS) {
            Double total = totalPorProducto.get(codigoProducto);
            totales.append(SEPARADOR_COLUMNAS).append(formatearValor(total != null ? total : 0.0));
        }
        totales.append(SEPARADOR_COLUMNAS).append(formatearValor(totalGeneral));
        writer.write(totales.toString());
        writer.newLine();

        System.out.println("Archivo plano por columnas: " + filas.size() + " partícipes, total a descontar $"
            + formatearValor(totalGeneral));
    }

    /**
     * Valor monetario con dos decimales y punto decimal.
     * Se fuerza Locale.US para que el separador decimal no dependa de la
     * configuración regional del servidor.
     */
    private String formatearValor(Double valor) {
        return String.format(Locale.US, "%.2f", valor != null ? valor : 0.0);
    }

    /**
     * Quita del texto el separador de columnas y los saltos de línea, que
     * romperían la estructura del archivo plano.
     */
    private String limpiarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace(SEPARADOR_COLUMNAS, " ")
                    .replace("\r", " ")
                    .replace("\n", " ")
                    .trim();
    }

    private String formatearLinea(LineaArchivo linea, String fechaProceso, String codigoProducto) {
        String codigoParticipe = String.format("%5d", linea.rolPetrocomercial);
        String codigoFijo = "JRNN";
        String rellenoCeros1 = "00000000";
        String fecha = fechaProceso;
        
        long montoEntero = Math.round(linea.monto * 10000);
        String montoFormateado = String.format("%013d", montoEntero);
        
        String codigoUno = "1";
        String rellenoCeros2 = "00000000000000";
        String tipoProducto = codigoProducto;
        
        return codigoParticipe + codigoFijo + rellenoCeros1 + fecha + 
               montoFormateado + codigoUno + rellenoCeros2 + tipoProducto;
    }

    /**
     * Nombre del archivo TXT.
     *
     * El nombre incluye la filial porque todas las generaciones se escriben en
     * la misma carpeta: sin eso, generar el mismo periodo para dos filiales
     * sobrescribiría el archivo de la primera.
     */
    private String generarNombreArchivo(Long mes, Long anio, Long codigoFilial) {
        String periodo = nombreMes(mes) + " " + anio + ".txt";

        if (esFilialPetrocomercial(codigoFilial)) {
            return "DESCUENTOS ASOPREP " + periodo;
        }
        if (codigoFilial == Filiales.ARCH) {
            return "DESCUENTOS ARCH " + periodo;
        }
        return "DESCUENTOS FILIAL " + codigoFilial + " " + periodo;
    }

    /**
     * Nombre del mes en mayúsculas. Devuelve el número si viene fuera de rango,
     * para que un dato malo no tumbe la generación del archivo.
     */
    private String nombreMes(Long mes) {
        if (mes == null || mes < 1 || mes > 12) {
            return String.valueOf(mes);
        }
        return NOMBRES_MESES[mes.intValue()];
    }

    private int obtenerUltimoDiaMes(int mes, int anio) {
        int[] diasPorMes = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        
        if (mes == 2 && ((anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0))) {
            return 29;
        }
        
        return diasPorMes[mes];
    }

    private String obtenerDescripcionProducto(String codigo) {
        switch (codigo) {
            case "AH": return "Aportes Voluntarios / Ahorro";
            case "HS": return "Seguro";
            case "PE": return "Préstamo Emergente";
            case "PH": return "Préstamo Hipotecario";
            case "PQ": return "Préstamo Quirografario";
            case "PP": return "Préstamo Personal";
            default: return "Desconocido";
        }
    }

    /**
     * Acumula los valores de seguro de incendio por entidad.
     * Solo aplica para préstamos PH (Hipotecario) y PP (Personal).
     * 
     * @param segurosPorEntidad Mapa acumulador de seguros por entidad
     * @param entidad Entidad/partícipe dueño del préstamo
     * @param codigoPrestamo Código del préstamo
     * @param cuota Cuota de la cual proviene el seguro
     * @param valorSeguro Saldo del seguro de incendio pendiente de cobro
     */
    private void acumularSeguroIncendio(Map<Long, LineaArchivo> segurosPorEntidad,
                                       Entidad entidad,
                                       Long codigoPrestamo, DetallePrestamo cuota,
                                       double valorSeguro) {
        if (valorSeguro <= 0.01) {
            return; // El seguro de esta cuota ya está cubierto
        }

        Long codigoEntidad = entidad.getCodigo();

        // Obtener o crear la línea de seguro para esta entidad
        LineaArchivo lineaSeguro = segurosPorEntidad.get(codigoEntidad);
        if (lineaSeguro == null) {
            lineaSeguro = new LineaArchivo();
            lineaSeguro.codigoEntidad = codigoEntidad;
            lineaSeguro.rolPetrocomercial = entidad.getRolPetroComercial();
            lineaSeguro.numeroIdentificacion = entidad.getNumeroIdentificacion();
            lineaSeguro.razonSocial = entidad.getRazonSocial();
            lineaSeguro.monto = 0.0;
            lineaSeguro.codigoPrestamo = null; // HS no tiene un préstamo único, son múltiples
            segurosPorEntidad.put(codigoEntidad, lineaSeguro);
        }
        
        // Acumular el monto del seguro
        lineaSeguro.monto += valorSeguro;
        
        // Registrar la cuota de donde proviene este seguro
        lineaSeguro.cuotasSumadas.add(new CuotaInfo(
            cuota.getNumeroCuota() != null ? cuota.getNumeroCuota().intValue() : 0,
            valorSeguro,
            codigoPrestamo // Guardar el préstamo de origen
        ));
    }

    // ========================================================================
    // MÉTODOS DE EntityService (DELEGACIÓN AL DAO)
    // ========================================================================

    @Override
    public GeneracionArchivoPetro selectById(Long id) throws Throwable {
        return dao.selectById(id, "GeneracionArchivoPetro");
    }

    @Override
    public List<GeneracionArchivoPetro> selectAll() throws Throwable {
        return dao.selectAll("GeneracionArchivoPetroAll");
    }

    @Override
    public List<GeneracionArchivoPetro> selectByCriteria(List<DatosBusqueda> criteria) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria GeneracionArchivoPetroService");
        List<GeneracionArchivoPetro> result = dao.selectByCriteria(criteria, NombreEntidadesCredito.GENERACION_ARCHIVOS_PETRO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio GeneracionArchivoPetro no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public GeneracionArchivoPetro saveSingle(GeneracionArchivoPetro entity) throws Throwable {
    	System.out.println("saveSingle - GeneracionArchivoPetro");
    	if(entity.getCodigo() == null){
    		entity.setFechaGeneracion(LocalDate.now());
		}
    	entity = dao.save(entity, entity.getCodigo());
    	return entity;
    }

    @Override
    public void save(List<GeneracionArchivoPetro> entities) throws Throwable {
        for (GeneracionArchivoPetro entity : entities) {
            dao.save(entity, entity.getCodigo());
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        for (Long id : ids) {
            GeneracionArchivoPetro entity = dao.selectById(id, "GeneracionArchivoPetro");
            if (entity != null) {
                dao.remove(entity, id);
            }
        }
    }

    // ========================================================================
    // CLASES INTERNAS
    // ========================================================================

    private static class LineaArchivo {
        Long codigoEntidad;
        Long rolPetrocomercial;
        // Identificación del partícipe en las filiales que no usan el rol (ARCH)
        String numeroIdentificacion;
        String razonSocial;
        Double monto;
        Long codigoPrestamo;
        List<CuotaInfo> cuotasSumadas;
        // Para aportes (producto AH): almacena montos por separado
        Double montoJubilacion;
        Double montoCesantia;

        public LineaArchivo() {
            this.cuotasSumadas = new ArrayList<>();
            this.montoJubilacion = 0.0;
            this.montoCesantia = 0.0;
        }
    }

    /**
     * Lo ya pagado de una cuota, acumulado desde CRD.PGPR (PagoPrestamo).
     */
    private static class PagosCuota {
        double capital = 0.0;
        double interes = 0.0;
        double desgravamen = 0.0;
        double mora = 0.0;
        double interesVencido = 0.0;
        double seguroIncendio = 0.0;
    }

    /**
     * Fila del archivo plano por columnas (ARCH): un partícipe con el valor de
     * cada producto en su propia columna.
     */
    private static class FilaColumnas {
        String numeroIdentificacion;
        String razonSocial;
        Double aporteCesantia;
        Double aporteJubilacion;
        Map<String, Double> montosPorProducto;

        public FilaColumnas() {
            this.numeroIdentificacion = "";
            this.razonSocial = "";
            this.aporteCesantia = 0.0;
            this.aporteJubilacion = 0.0;
            this.montosPorProducto = new LinkedHashMap<>();
        }
    }
    
    private static class CuotaInfo {
        Integer numeroCuota;
        Double valorCuota;
        Long codigoPrestamo; // Para seguros de incendio: indica de qué préstamo proviene
        
        public CuotaInfo(Integer numeroCuota, Double valorCuota) {
            this.numeroCuota = numeroCuota;
            this.valorCuota = valorCuota;
            this.codigoPrestamo = null;
        }
        
        public CuotaInfo(Integer numeroCuota, Double valorCuota, Long codigoPrestamo) {
            this.numeroCuota = numeroCuota;
            this.valorCuota = valorCuota;
            this.codigoPrestamo = codigoPrestamo;
        }
    }
}
