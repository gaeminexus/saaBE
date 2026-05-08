package com.saa.ejb.crd.serviceImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
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

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
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

    @PersistenceContext
    private EntityManager em;

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
        
        System.out.println("Procesando periodo: " + mes + "/" + anio);
        
        // 4. Recopilar datos por tipo de producto
        Map<String, List<LineaArchivo>> datosPorProducto = recopilarDatos(mes, anio);
        
        // 5. Crear detalles por producto y registros de partícipes
        long totalRegistros = 0;
        double totalMonto = 0.0;
        long numeroLinea = 1;
        
        String[] ordenProductos = {"AH", "HS", "PE", "PH", "PQ", "PP"};
        
        for (String codigoProducto : ordenProductos) {
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
        String nombreArchivo = generarNombreArchivo(mes, anio);
        String rutaArchivo = generarArchivoTXT(datosPorProducto, mes, anio, nombreArchivo);
        
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
        
        return respuesta;
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
                linea.monto = participe.getMontoEnviado();
                linea.codigoPrestamo = participe.getPrestamo() != null ? participe.getPrestamo().getCodigo() : null;
                
                lineasProducto.add(linea);
            }
        }
        
        // Generar archivo
        String nombreArchivo = generacion.getNombreArchivo();
        if (nombreArchivo == null || nombreArchivo.isEmpty()) {
            nombreArchivo = generarNombreArchivo(generacion.getMesPeriodo(), generacion.getAnioPeriodo());
        }
        
        String rutaArchivo = generarArchivoTXT(datosPorProducto, 
            generacion.getMesPeriodo(), 
            generacion.getAnioPeriodo(), 
            nombreArchivo);
        
        System.out.println("Archivo regenerado en: " + rutaArchivo);
        return rutaArchivo;
    }

    // ========================================================================
    // MÉTODOS PRIVADOS - LÓGICA INTERNA
    // ========================================================================

    private Map<String, List<LineaArchivo>> recopilarDatos(Long mes, Long anio) throws Exception {
        System.out.println("Recopilando datos para periodo: " + mes + "/" + anio);
        
        Map<String, List<LineaArchivo>> datosPorProducto = new LinkedHashMap<>();
        datosPorProducto.put("AH", new ArrayList<>());
        datosPorProducto.put("HS", new ArrayList<>());
        datosPorProducto.put("PE", new ArrayList<>());
        datosPorProducto.put("PH", new ArrayList<>());
        datosPorProducto.put("PQ", new ArrayList<>());
        datosPorProducto.put("PP", new ArrayList<>());
        
        // 1. Aportes personales
        recopilarAportes(datosPorProducto.get("AH"));
        
        // 2. Cuotas de préstamos
        recopilarPrestamos(mes, anio, datosPorProducto);
        
        // Ordenar por rol petrocomercial
        for (List<LineaArchivo> lista : datosPorProducto.values()) {
            lista.sort((a, b) -> a.rolPetrocomercial.compareTo(b.rolPetrocomercial));
        }
        
        return datosPorProducto;
    }

    private void recopilarAportes(List<LineaArchivo> listaAportes) throws Exception {
        System.out.println("Recopilando aportes personales...");
        System.out.println("Filtros: Entidad.idEstado=10 (ACTIVO), HistorialSueldo.estado=99");
        
        String jpql = "SELECT h FROM HistorialSueldo h " +
                     "WHERE h.entidad.idEstado = 10 " +
                     "AND h.estado = 99 " +
                     "AND h.entidad.rolPetroComercial IS NOT NULL " +
                     "AND h.entidad.rolPetroComercial > 0 " +
                     "ORDER BY h.entidad.codigo, h.fechaIngreso DESC";
        
        Query query = em.createQuery(jpql);
        
        @SuppressWarnings("unchecked")
        List<HistorialSueldo> resultados = query.getResultList();
        
        System.out.println("Registros HistorialSueldo encontrados: " + resultados.size());
        
        Long ultimaEntidad = null;
        for (HistorialSueldo historial : resultados) {
            if (ultimaEntidad != null && ultimaEntidad.equals(historial.getEntidad().getCodigo())) {
                continue;
            }
            
            Double montoJubilacion = historial.getMontoJubilacion() != null ? historial.getMontoJubilacion() : 0.0;
            Double montoCesantia = historial.getMontoCesantia() != null ? historial.getMontoCesantia() : 0.0;
            Double montoTotal = montoJubilacion + montoCesantia;
            
            if (montoTotal > 0) {
                LineaArchivo linea = new LineaArchivo();
                linea.codigoEntidad = historial.getEntidad().getCodigo();
                linea.rolPetrocomercial = historial.getEntidad().getRolPetroComercial();
                linea.monto = montoTotal;
                linea.codigoPrestamo = null;
                // Almacenar montos por separado para crear registros CXPG individuales
                linea.montoJubilacion = montoJubilacion;
                linea.montoCesantia = montoCesantia;
                
                listaAportes.add(linea);
            }
            
            ultimaEntidad = historial.getEntidad().getCodigo();
        }
        
        System.out.println("Aportes recopilados: " + listaAportes.size());
    }

    private void recopilarPrestamos(Long mes, Long anio, Map<String, List<LineaArchivo>> datosPorProducto) throws Exception {
        System.out.println("Recopilando cuotas de préstamos...");
        System.out.println("Filtros: Entidad.idEstado=10 (ACTIVO), Prestamo.idEstado IN (1,2) (VIGENTE/ACTIVO)");
        System.out.println("Incluyendo cuotas en estado: PENDIENTE, ACTIVA, EMITIDA, EN_MORA, PARCIAL, VENCIDA");
        System.out.println("Excluyendo cuotas en estado: 4 (PAGADA), 7 (CANCELADA_ANTICIPADA)");
        
        final Long ESTADO_PAGADA = 4L;
        final Long ESTADO_CANCELADA_ANTICIPADA = 7L;
        
        // ✅ Estructura para acumular seguros de incendio por entidad
        // Clave: codigoEntidad, Valor: LineaArchivo con acumulación de seguros
        Map<Long, LineaArchivo> segurosPorEntidad = new LinkedHashMap<>();
        
        // ✅ Usar fechaService para obtener el primer y último día del mes
        LocalDate inicioMesDate;
        LocalDate finMesDate;
        try {
            inicioMesDate = fechaService.primerDiaMesAnioLocal(mes, anio);
            finMesDate = fechaService.ultimoDiaMesAnioLocal(mes, anio);
        } catch (Throwable e) {
            throw new Exception("Error al calcular fechas del periodo: " + e.getMessage(), e);
        }
        
        // ✅ Convertir a LocalDateTime para comparar con fechaVencimiento (que es LocalDateTime)
        LocalDateTime inicioMes = inicioMesDate.atStartOfDay();
        LocalDateTime finMes = finMesDate.atTime(23, 59, 59);
        
        // ✅ CORREGIDO: 
        // 1. Comparar directamente con LocalDateTime (tipo del campo fechaVencimiento)
        // 2. Usar COALESCE (estándar JPA) para rolPetroComercial
        // 3. Usar dp.prestamo.idEstado IN (1,2) para préstamos VIGENTES/ACTIVOS
        // 4. Usar dp.prestamo.entidad.idEstado = 10 para entidades ACTIVAS
        String jpql = "SELECT dp FROM DetallePrestamo dp " +
                     "WHERE dp.fechaVencimiento >= :inicioMes " +
                     "AND dp.fechaVencimiento <= :finMes " +
                     "AND dp.prestamo.idEstado IN (1, 2) " +
                     "AND dp.prestamo.entidad.idEstado = 10 " +
                     "AND dp.prestamo.producto.codigoPetro IS NOT NULL " +
                     "AND COALESCE(dp.prestamo.entidad.rolPetroComercial, 0) > 0 " +
                     "AND dp.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
                     "ORDER BY dp.prestamo.codigo, dp.numeroCuota";
        
        Query query = em.createQuery(jpql);
        query.setParameter("inicioMes", inicioMes);
        query.setParameter("finMes", finMes);
        query.setParameter("estadoPagada", ESTADO_PAGADA);
        query.setParameter("estadoCanceladaAnticipada", ESTADO_CANCELADA_ANTICIPADA);
        
        @SuppressWarnings("unchecked")
        List<DetallePrestamo> cuotasDelMes = query.getResultList();
        
        System.out.println("Cuotas del mes encontradas: " + cuotasDelMes.size());
        
        for (DetallePrestamo cuotaDelMes : cuotasDelMes) {
            Long codigoPrestamo = cuotaDelMes.getPrestamo().getCodigo();
            String codigoProductoPetro = cuotaDelMes.getPrestamo().getProducto().getCodigoPetro();
            
            if (codigoProductoPetro == null || codigoProductoPetro.trim().isEmpty()) {
                continue;
            }
            
            List<LineaArchivo> listaProducto = datosPorProducto.get(codigoProductoPetro);
            if (listaProducto == null) {
                System.err.println("ADVERTENCIA: Código producto Petro no reconocido: " + codigoProductoPetro);
                continue;
            }
            
            // ✅ CORREGIDO: Buscar cuotas anteriores pendientes usando LAS MISMAS CONDICIONES del SELECT principal
            // Solo que filtramos por el préstamo específico y cuotas con número menor a la actual
            String jpqlAnteriores = "SELECT dp FROM DetallePrestamo dp " +
                                   "WHERE dp.prestamo.codigo = :codigoPrestamo " +
                                   "AND dp.numeroCuota < :numeroCuotaActual " +
                                   "AND dp.prestamo.idEstado IN (1, 2) " +
                                   "AND dp.prestamo.entidad.idEstado = 10 " +
                                   "AND dp.prestamo.producto.codigoPetro IS NOT NULL " +
                                   "AND COALESCE(dp.prestamo.entidad.rolPetroComercial, 0) > 0 " +
                                   "AND dp.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
                                   "ORDER BY dp.numeroCuota";
            
            Query queryAnteriores = em.createQuery(jpqlAnteriores);
            queryAnteriores.setParameter("codigoPrestamo", codigoPrestamo);
            queryAnteriores.setParameter("numeroCuotaActual", cuotaDelMes.getNumeroCuota());
            queryAnteriores.setParameter("estadoPagada", ESTADO_PAGADA);
            queryAnteriores.setParameter("estadoCanceladaAnticipada", ESTADO_CANCELADA_ANTICIPADA);
            
            @SuppressWarnings("unchecked")
            List<DetallePrestamo> cuotasAnterioresPendientes = queryAnteriores.getResultList();
            
            double montoTotal = 0.0;
            LineaArchivo linea = new LineaArchivo();
            
            // Sumar cuotas anteriores
            for (DetallePrestamo cuotaAnterior : cuotasAnterioresPendientes) {
                double montoCuota = calcularMontoCuota(cuotaAnterior);
                montoTotal += montoCuota;
                
                linea.cuotasSumadas.add(new CuotaInfo(
                    cuotaAnterior.getNumeroCuota() != null ? cuotaAnterior.getNumeroCuota().intValue() : 0,
                    montoCuota
                ));
            }
            
            // Sumar cuota actual
            double montoCuotaActual = calcularMontoCuota(cuotaDelMes);
            montoTotal += montoCuotaActual;
            
            linea.cuotasSumadas.add(new CuotaInfo(
                cuotaDelMes.getNumeroCuota() != null ? cuotaDelMes.getNumeroCuota().intValue() : 0,
                montoCuotaActual
            ));
            
            if (cuotasAnterioresPendientes.size() > 0) {
                System.out.println("  Préstamo " + codigoPrestamo + " - Producto " + codigoProductoPetro + 
                                 ": Cuota del mes #" + cuotaDelMes.getNumeroCuota().intValue() + 
                                 " + " + cuotasAnterioresPendientes.size() + " cuotas anteriores pendientes" +
                                 " = Total: $" + String.format("%.2f", montoTotal));
            }
            
            if (montoTotal > 0) {
                linea.codigoEntidad = cuotaDelMes.getPrestamo().getEntidad().getCodigo();
                linea.rolPetrocomercial = cuotaDelMes.getPrestamo().getEntidad().getRolPetroComercial();
                linea.monto = montoTotal;
                linea.codigoPrestamo = codigoPrestamo;
                
                listaProducto.add(linea);
            }
            
            // ✅ NUEVO: Extraer y acumular seguros de incendio para productos PH y PP
            if ("PH".equals(codigoProductoPetro) || "PP".equals(codigoProductoPetro)) {
                Long codigoEntidad = cuotaDelMes.getPrestamo().getEntidad().getCodigo();
                Long rolPetro = cuotaDelMes.getPrestamo().getEntidad().getRolPetroComercial();
                
                // Acumular seguro de la cuota actual
                acumularSeguroIncendio(segurosPorEntidad, codigoEntidad, rolPetro, 
                                      codigoPrestamo, cuotaDelMes);
                
                // Acumular seguros de cuotas anteriores pendientes
                for (DetallePrestamo cuotaAnterior : cuotasAnterioresPendientes) {
                    acumularSeguroIncendio(segurosPorEntidad, codigoEntidad, rolPetro, 
                                          codigoPrestamo, cuotaAnterior);
                }
            }
        }
        
        // ✅ Agregar los seguros de incendio acumulados al producto HS
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
     * Calcula el monto a descontar de una cuota de préstamo.
     * 
     * ✅ CORREGIDO: Suma capital + interés + mora + interésVencido + DESGRAVAMEN
     * 
     * El desgravamen es un seguro obligatorio que debe incluirse en el descuento mensual.
     * 
     * @param cuota DetallePrestamo con los datos de la cuota
     * @return Monto total a descontar
     */
    private double calcularMontoCuota(DetallePrestamo cuota) {
        double capital = cuota.getCapital() != null ? cuota.getCapital() : 0.0;
        double interes = cuota.getInteres() != null ? cuota.getInteres() : 0.0;
        double mora = cuota.getMora() != null ? cuota.getMora() : 0.0;
        double interesVencido = cuota.getInteresVencido() != null ? cuota.getInteresVencido() : 0.0;
        double desgravamen = cuota.getDesgravamen() != null ? cuota.getDesgravamen() : 0.0;
        
        return capital + interes + mora + interesVencido + desgravamen;
    }

    private String generarArchivoTXT(Map<String, List<LineaArchivo>> datosPorProducto, 
                                     Long mes, Long anio, String nombreArchivo) throws Exception {
        System.out.println("Generando archivo TXT: " + nombreArchivo);
        
        String rutaBase = System.getProperty("user.home") + File.separator + "archivos_petrocomercial";
        File directorio = new File(rutaBase);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }
        
        String rutaCompleta = rutaBase + File.separator + nombreArchivo;
        
        int ultimoDia = obtenerUltimoDiaMes(mes.intValue(), anio.intValue());
        String fechaProceso = String.format("%04d%02d%02d", anio, mes, ultimoDia);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(rutaCompleta))) {
            String[] ordenProductos = {"AH", "HS", "PE", "PH", "PQ", "PP"};
            
            for (String codigoProducto : ordenProductos) {
                List<LineaArchivo> lineas = datosPorProducto.get(codigoProducto);
                if (lineas == null) continue;
                
                for (LineaArchivo linea : lineas) {
                    String lineaTXT = formatearLinea(linea, fechaProceso, codigoProducto);
                    writer.write(lineaTXT);
                    writer.newLine();
                }
            }
        }
        
        System.out.println("Archivo generado en: " + rutaCompleta);
        return rutaCompleta;
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

    private String generarNombreArchivo(Long mes, Long anio) {
        String[] meses = {"", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
                         "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
        return "DESCUENTOS ASOPREP " + meses[mes.intValue()] + " " + anio + ".txt";
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
     * @param codigoEntidad Código de la entidad/partícipe
     * @param rolPetro Rol Petrocomercial de la entidad
     * @param codigoPrestamo Código del préstamo
     * @param cuota Cuota de la cual extraer el seguro de incendio
     */
    private void acumularSeguroIncendio(Map<Long, LineaArchivo> segurosPorEntidad, 
                                       Long codigoEntidad, Long rolPetro,
                                       Long codigoPrestamo, DetallePrestamo cuota) {
        Double valorSeguro = cuota.getValorSeguroIncendio();
        if (valorSeguro == null || valorSeguro <= 0) {
            return; // No hay seguro en esta cuota
        }
        
        // Obtener o crear la línea de seguro para esta entidad
        LineaArchivo lineaSeguro = segurosPorEntidad.get(codigoEntidad);
        if (lineaSeguro == null) {
            lineaSeguro = new LineaArchivo();
            lineaSeguro.codigoEntidad = codigoEntidad;
            lineaSeguro.rolPetrocomercial = rolPetro;
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
