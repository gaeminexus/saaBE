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
import java.util.Locale;
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
import com.saa.rubros.EstadoParticipeEntidad;
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
     * Solo se incluye a los partícipes de la filial que se está generando y en
     * estado ACTIVO; los que están en ACTIVO EN MORA quedan fuera del archivo.
     * A cada partícipe incluido se le cobra un solo mes de aporte
     * (jubilación + cesantía).
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
        System.out.println("Filtros: Entidad en estado ACTIVO (se excluye ACTIVO EN MORA), HistorialSueldo.estado=99");

        String jpql = "SELECT h FROM HistorialSueldo h " +
                     "WHERE h.entidad.idEstado = :estadoActivo " +
                     "AND h.estado = 99 " +
                     "AND h.entidad.filial.codigo = :codigoFilial " +
                     condicionIdentificadorFilial("h.entidad", codigoFilial) +
                     "ORDER BY h.entidad.codigo, h.fechaIngreso DESC";

        Query query = em.createQuery(jpql);
        query.setParameter("estadoActivo", (long) EstadoParticipeEntidad.ACTIVO);
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

        for (HistorialSueldo historial : historialPorEntidad) {
            Long codigoEntidad = historial.getEntidad().getCodigo();

            Double montoJubilacion = historial.getMontoJubilacion() != null ? historial.getMontoJubilacion() : 0.0;
            Double montoCesantia = historial.getMontoCesantia() != null ? historial.getMontoCesantia() : 0.0;

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

    private void recopilarPrestamos(Long mes, Long anio, Map<String, List<LineaArchivo>> datosPorProducto,
                                    Long codigoFilial) throws Exception {
        System.out.println("Recopilando cuotas de préstamos de la filial " + codigoFilial + "...");
        System.out.println("Filtros: Entidad en estado ACTIVO, Prestamo.idEstado IN (1,2) (VIGENTE/ACTIVO)");
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
        // 4. Filtrar por entidad en estado ACTIVO
        String jpql = "SELECT dp FROM DetallePrestamo dp " +
                     "WHERE dp.fechaVencimiento >= :inicioMes " +
                     "AND dp.fechaVencimiento <= :finMes " +
                     "AND dp.prestamo.idEstado IN (1, 2) " +
                     "AND dp.prestamo.entidad.idEstado = :estadoActivo " +
                     "AND dp.prestamo.entidad.filial.codigo = :codigoFilial " +
                     "AND dp.prestamo.producto.codigoPetro IS NOT NULL " +
                     condicionIdentificadorFilial("dp.prestamo.entidad", codigoFilial) +
                     "AND dp.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
                     "ORDER BY dp.prestamo.codigo, dp.numeroCuota";

        Query query = em.createQuery(jpql);
        query.setParameter("inicioMes", inicioMes);
        query.setParameter("finMes", finMes);
        query.setParameter("estadoActivo", (long) EstadoParticipeEntidad.ACTIVO);
        query.setParameter("codigoFilial", codigoFilial);
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
                                   "AND dp.prestamo.entidad.idEstado = :estadoActivo " +
                                   "AND dp.prestamo.entidad.filial.codigo = :codigoFilial " +
                                   "AND dp.prestamo.producto.codigoPetro IS NOT NULL " +
                                   condicionIdentificadorFilial("dp.prestamo.entidad", codigoFilial) +
                                   "AND dp.estado NOT IN (:estadoPagada, :estadoCanceladaAnticipada) " +
                                   "ORDER BY dp.numeroCuota";

            Query queryAnteriores = em.createQuery(jpqlAnteriores);
            queryAnteriores.setParameter("codigoPrestamo", codigoPrestamo);
            queryAnteriores.setParameter("numeroCuotaActual", cuotaDelMes.getNumeroCuota());
            queryAnteriores.setParameter("estadoActivo", (long) EstadoParticipeEntidad.ACTIVO);
            queryAnteriores.setParameter("codigoFilial", codigoFilial);
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
                Entidad entidadPrestamo = cuotaDelMes.getPrestamo().getEntidad();
                linea.codigoEntidad = entidadPrestamo.getCodigo();
                linea.rolPetrocomercial = entidadPrestamo.getRolPetroComercial();
                linea.numeroIdentificacion = entidadPrestamo.getNumeroIdentificacion();
                linea.razonSocial = entidadPrestamo.getRazonSocial();
                linea.monto = montoTotal;
                linea.codigoPrestamo = codigoPrestamo;

                listaProducto.add(linea);
            }
            
            // ✅ NUEVO: Extraer y acumular seguros de incendio para productos PH y PP
            if ("PH".equals(codigoProductoPetro) || "PP".equals(codigoProductoPetro)) {
                Entidad entidadSeguro = cuotaDelMes.getPrestamo().getEntidad();

                // Acumular seguro de la cuota actual
                acumularSeguroIncendio(segurosPorEntidad, entidadSeguro, codigoPrestamo, cuotaDelMes);

                // Acumular seguros de cuotas anteriores pendientes
                for (DetallePrestamo cuotaAnterior : cuotasAnterioresPendientes) {
                    acumularSeguroIncendio(segurosPorEntidad, entidadSeguro, codigoPrestamo, cuotaAnterior);
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
                escribirArchivoPlanoColumnas(writer, datosPorProducto);
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
     * Columnas: IDENTIFICACION;RAZON SOCIAL;AC;AJ;PE;PH;HS;PQ;PP
     *
     * A diferencia de Petrocomercial, el aporte NO se envía sumado en AH: la
     * cesantía va en AC y la jubilación en AJ. Los valores salen como números
     * normales con dos decimales (sin multiplicar ni rellenar con ceros).
     */
    private void escribirArchivoPlanoColumnas(BufferedWriter writer,
                                              Map<String, List<LineaArchivo>> datosPorProducto) throws Exception {
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

        // Encabezado de columnas
        StringBuilder cabecera = new StringBuilder();
        cabecera.append("IDENTIFICACION").append(SEPARADOR_COLUMNAS);
        cabecera.append("RAZON SOCIAL").append(SEPARADOR_COLUMNAS);
        cabecera.append("AC").append(SEPARADOR_COLUMNAS);
        cabecera.append("AJ");
        for (String codigoProducto : ORDEN_COLUMNAS_PRESTAMOS) {
            cabecera.append(SEPARADOR_COLUMNAS).append(codigoProducto);
        }
        writer.write(cabecera.toString());
        writer.newLine();

        // Ordenadas por número de identificación
        List<FilaColumnas> filas = new ArrayList<>(filasPorEntidad.values());
        filas.sort((a, b) -> a.numeroIdentificacion.compareTo(b.numeroIdentificacion));

        for (FilaColumnas fila : filas) {
            StringBuilder linea = new StringBuilder();
            linea.append(fila.numeroIdentificacion).append(SEPARADOR_COLUMNAS);
            linea.append(limpiarTexto(fila.razonSocial)).append(SEPARADOR_COLUMNAS);
            linea.append(formatearValor(fila.aporteCesantia)).append(SEPARADOR_COLUMNAS);
            linea.append(formatearValor(fila.aporteJubilacion));

            for (String codigoProducto : ORDEN_COLUMNAS_PRESTAMOS) {
                Double monto = fila.montosPorProducto.get(codigoProducto);
                linea.append(SEPARADOR_COLUMNAS).append(formatearValor(monto != null ? monto : 0.0));
            }

            writer.write(linea.toString());
            writer.newLine();
        }

        System.out.println("Archivo plano por columnas: " + filas.size() + " partícipes");
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
        String[] meses = {"", "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
                         "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};
        String periodo = meses[mes.intValue()] + " " + anio + ".txt";

        if (esFilialPetrocomercial(codigoFilial)) {
            return "DESCUENTOS ASOPREP " + periodo;
        }
        if (codigoFilial == Filiales.ARCH) {
            return "DESCUENTOS ARCH " + periodo;
        }
        return "DESCUENTOS FILIAL " + codigoFilial + " " + periodo;
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
     * @param cuota Cuota de la cual extraer el seguro de incendio
     */
    private void acumularSeguroIncendio(Map<Long, LineaArchivo> segurosPorEntidad,
                                       Entidad entidad,
                                       Long codigoPrestamo, DetallePrestamo cuota) {
        Double valorSeguro = cuota.getValorSeguroIncendio();
        if (valorSeguro == null || valorSeguro <= 0) {
            return; // No hay seguro en esta cuota
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
