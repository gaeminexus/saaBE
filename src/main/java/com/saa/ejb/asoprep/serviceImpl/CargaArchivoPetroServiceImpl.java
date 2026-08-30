package com.saa.ejb.asoprep.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.DetalleCargaArchivoService;
import com.saa.ejb.crd.service.ParticipeXCargaArchivoService;
import com.saa.model.crd.AfectacionValoresParticipeCarga;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.NovedadParticipeCarga;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.ParticipeXCargaArchivo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.ASPNovedadesCargaArchivo;
import com.saa.rubros.EstadoParticipeEntidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación Stateful para procesar archivos Petro con manejo de transacciones
 *
 * TODO (convergencia futura, fuera del alcance actual): los métodos privados
 * calcularSaldosRealesCuota, procesarPagoCuota, procesarExcedenteASiguienteCuota,
 * verificarYActualizarEstadoPrestamo y crearRegistroPago de esta clase fueron la base de
 * com.saa.ejb.crd.serviceImpl.MotorPagoPrestamoServiceImpl (motor de pagos compartido de los
 * servicios de pago de préstamos). Hoy conviven dos implementaciones de la misma lógica:
 *
 *   - El motor nuevo agrega mora (DTPRMRAA) e interés vencido (DTPRINVN) a la prelación
 *     (Desgravamen → Mora → Interés vencido → Interés → Capital → Seguro de incendio),
 *     agrupa toda operación bajo un EventoPrestamo (CRD.EVPR) y reconstruye los saldos SOLO
 *     desde los PagoPrestamo VIGENTES (excluye los anulados por un reverso).
 *   - Este servicio mantiene la prelación de 4 componentes y consume TODOS los PGPR.
 *
 * Refactorizar este proceso para que delegue en MotorPagoPrestamoService es una fase futura
 * y está explícitamente FUERA del alcance de la especificación
 * docs/logica-negocio/crd/ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md (§1.3). Mientras tanto,
 * NO modificar la lógica de pagos de esta clase sin replicar el cambio en el motor, y
 * viceversa. Cualquier cambio aquí debe actualizar además
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
    
    @EJB
    private com.saa.ejb.crd.service.PagoAporteService pagoAporteService;
    
    @EJB
    private com.saa.ejb.crd.dao.PagoAporteDaoService pagoAporteDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.TipoAporteDaoService tipoAporteDaoService;
    
    @EJB
    private com.saa.basico.ejb.FechaService fechaService;

    /** Pedido 10: para que un préstamo sin cuotas vencidas vuelva a VIGENTE en cuanto se paga. */
    @EJB
    private com.saa.ejb.crd.service.ProcesoMoraPrestamoService procesoMoraPrestamoService;

    /** Fase 3: fuente del esperado mensual, ver {@link #esperadoMensual}. */
    @EJB
    private com.saa.ejb.crd.service.VigenciaContratoService vigenciaContratoService;

    /** Fase 3a: asiento de REPARTO del paso 2, ver {@link #aplicarPagosArchivoPetro}. */
    @EJB
    private com.saa.ejb.crd.service.CobroPetroContableService cobroPetroContableService;

    private static final double TOLERANCIA = 1.0; // Tolerancia de $1 para redondeos
    
    // Códigos de TipoAporte
    private static final Long TIPO_APORTE_JUBILACION = 9L;  // Código para aporte de jubilación (CORRECTO)
    private static final Long TIPO_APORTE_CESANTIA = 11L;   // Código para aporte de cesantía
    
    // Códigos de producto Petro que NO se validan como préstamos
    private static final String CODIGO_PRODUCTO_APORTES = "AH";
    private static final String CODIGO_PRODUCTO_HS = "HS";
    private static final String CODIGO_PRODUCTO_PH = "PH"; // Préstamo Hipotecario - El seguro viene en HS
    private static final String CODIGO_PRODUCTO_PP = "PP"; // Préstamo Prendario - El seguro viene en HS
    
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
							int largoTrim = LARGO_NOMBRE_PETRO;
							if(entidades.get(0).getRazonSocial().trim().length() < LARGO_NOMBRE_PETRO) {
								largoTrim = entidades.get(0).getRazonSocial().trim().length();
							}
							if (!entidades.get(0).getRazonSocial().trim().substring(0,largoTrim).equalsIgnoreCase(participe.getNombre().trim())) {
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
					validarNovedadesFase2(participe, codigoProducto, cargaArchivo);
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
                            
                            // Extraer campos del registro
                            String codigo = extraerCampo(lineaRegistro, 0, 7).trim();
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
                            
                            if (!codigo.isEmpty()) {
                                registro.setCodigoPetro(parseLongSimple(codigo));
                                registrosProcesados.add(registro);
                            }
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Error al procesar línea " + (numeroLinea + i) + " del producto '" + descripcionAporte + "': " + e.getMessage(), e);
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
	 * Registra una novedad en la tabla hija NovedadParticipeCarga
	 * Permite que un partícipe tenga múltiples novedades
	 * 
	 * @param participe El partícipe relacionado
	 * @param tipoNovedad Tipo de novedad (código del rubro)
	 * @param descripcion Descripción de la novedad
	 * @param codigoProducto Código del producto relacionado (opcional)
	 * @param codigoPrestamo Código del préstamo relacionado (opcional)
	 * @param montoEsperado Monto esperado del sistema (opcional)
	 * @param montoRecibido Monto recibido del archivo (opcional)
	 */
	private void registrarNovedad(ParticipeXCargaArchivo participe, int tipoNovedad, String descripcion, 
								  Long codigoProducto, Long codigoPrestamo, Double montoEsperado, Double montoRecibido) {
		try {
			if (participe == null || participe.getCodigo() == null) {
				return;
			}
			
			ParticipeXCargaArchivo participeRef = new ParticipeXCargaArchivo();
			participeRef.setCodigo(participe.getCodigo());
			
			NovedadParticipeCarga novedad = new NovedadParticipeCarga();
			novedad.setParticipeXCargaArchivo(participeRef);
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
			
			novedad.setEstado(1L);
			novedadParticipeCargaService.saveSingle(novedad);
			
		} catch (Throwable e) {
			System.err.println("Error al registrar novedad: " + descripcion);
			e.printStackTrace();
		}
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
			// ==========================================
			validarOrdenProcesamiento(cargaArchivo);
			// ==========================================

			// ==========================================
			exigeConfirmacionContabilidad(cargaArchivo);
			// ==========================================

			// ==========================================
			// VALIDACIÓN: todo valor descontado debe tener a qué aplicarse.
			// Si algún registro tiene una novedad que impide determinar el
			// destino y nadie registró la afectación manual, no se procesa nada.
			// ==========================================
			validarValoresConDestino(cargaArchivo);
			// ==========================================

			// 2. ✅ OPTIMIZACIÓN: Obtener SOLO los detalles de esta carga específica
			// En lugar de traer TODOS los detalles de TODAS las cargas con selectAll()
			List<DetalleCargaArchivo> detallesCarga = detalleCargaArchivoDaoService.selectByCargaArchivo(codigoCargaArchivo);
			
			int totalProcesados = 0;
			int totalExitosos = 0;
			int totalErrores = 0;
			int totalOmitidos = 0;
			int totalAportesGenerados = 0;
			
			// 3. Por cada detalle, procesar los partícipes
			for (DetalleCargaArchivo detalle : detallesCarga) {
				String codigoProducto = detalle.getCodigoPetroProducto();
				
				System.out.println("\n📦 Procesando PRODUCTO: " + codigoProducto);
				
				// Omitir solo producto HS (seguros independientes)
				// AH (Aportes) SÍ se procesa
				if (CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto)) {
					System.out.println("  ⊘ Producto HS omitido (se procesa junto con PH/PP)");
					continue;
				}
				
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
								// PRODUCTO AH: Generar Aportes
								int aportesCreados = aplicarAporteAH(participe, cargaArchivo);
								totalAportesGenerados += aportesCreados;
								if (aportesCreados > 0) {
									totalExitosos++;
								}
							} else {
								// OTROS PRODUCTOS: Aplicar pagos a préstamos
								aplicarPagoParticipe(participe, codigoProducto, cargaArchivo);
								totalExitosos++;
							}
						} catch (Throwable e) {
							throw new RuntimeException("TODO O NADA: falló el procesamiento del partícipe "
								+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
								+ codigoProducto + " — se aborta la carga completa " + codigoCargaArchivo
								+ ", no se confirma ningún cambio. Causa: " + e.getMessage(), e);
						}
				}
			}
			
			String resumen = String.format(
				"=== RESUMEN APLICACIÓN DE PAGOS ===\n" +
				"Total procesados: %d\n" +
				"Exitosos: %d\n" +
				"Aportes generados: %d\n" +
				"Omitidos (con novedades): %d\n" +
				"Errores: %d\n" +
				"================================",
				totalProcesados, totalExitosos, totalAportesGenerados, totalOmitidos, totalErrores
			);
			
			System.out.println(resumen);

			// Paso 2 del cobro en dos pasos (regla 11 de §5 del levantamiento), antes de marcar
			// PROCESADO: si cualquiera de los dos asientos falla, la carga NO queda a medio
			// contabilizar (misma transacción REQUIRED que aplica todos los pagos).
			// 2a. REPARTO: D 2.3.01.15.01 -> H 1.4.05.05/1.4.05.10, plantilla alterno 20.
			cobroPetroContableService.contabilizarReparto(codigoCargaArchivo);
			// 2b. APLICACION: D 2.3.02.05/2.3.02.10 -> H cuentas reales, plantilla 21 + bandas.
			cobroPetroContableService.contabilizarAplicacion(codigoCargaArchivo);

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

	/**
	 * Novedades con las que el sistema NO puede determinar por sí solo a qué
	 * préstamo, cuota o aporte aplicar el valor descontado.
	 *
	 * Un registro con una de estas novedades solo se puede procesar si el usuario
	 * dejó registrada la afectación manual (AVPC) diciendo cómo aplicar el valor.
	 *
	 * NO entran aquí las novedades que no dejan dinero sin aplicar:
	 * - SIN_DESCUENTOS, VALORES_CERO, APORTE_VALORES_CERO: no llegó valor alguno.
	 * - DESCUENTOS_INCOMPLETOS: falta plata, pero la que llegó sí tiene destino.
	 * - DIFERENCIA_MENOR_UN_DOLAR y su equivalente de aportes: dentro de tolerancia.
	 * - CUOTA_FECHA_DIFERENTE: la cuota se encontró, solo cambia el mes.
	 * - Las de resultado (OK, PRESTAMO_PROCESADO_OK, APORTE_GENERADO_OK).
	 */
	private static final List<Long> NOVEDADES_REQUIEREN_AFECTACION_MANUAL = Arrays.asList(
		(long) ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO,
		(long) ASPNovedadesCargaArchivo.CODIGO_ROL_DUPLICADO,
		(long) ASPNovedadesCargaArchivo.NOMBRE_ENTIDAD_DUPLICADO,
		(long) ASPNovedadesCargaArchivo.CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE,
		(long) ASPNovedadesCargaArchivo.DESCUENTOS_ADICIONALES,
		(long) ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO,
		(long) ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
		(long) ASPNovedadesCargaArchivo.MULTIPLES_PRESTAMOS_ACTIVOS,
		(long) ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA,
		(long) ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE,
		(long) ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
		(long) ASPNovedadesCargaArchivo.MULTIPLES_REGISTROS_HISTORIAL_SUELDO,
		(long) ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
		(long) ASPNovedadesCargaArchivo.APORTE_MONTO_INCONSISTENTE
	);

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

				List<Long> novedadesSinResolver = novedadesQueRequierenAfectacion(participe, novedades);
				if (novedadesSinResolver.isEmpty()) {
					// El proceso automático sabe qué hacer con este valor.
					continue;
				}

				double valorConDestino = totalAfectadoManualmente(novedades);
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
	 * Tipos de novedad del partícipe que impiden determinar el destino del valor.
	 *
	 * Se miran las dos fuentes: los campos de novedad del propio registro
	 * (novedadesCarga / novedadesFinancieras, que es lo que ve el usuario en la
	 * grilla) y las filas de NVPC, que es el detalle sobre el que se registran
	 * las afectaciones manuales.
	 */
	private List<Long> novedadesQueRequierenAfectacion(ParticipeXCargaArchivo participe,
			List<NovedadParticipeCarga> novedades) {

		List<Long> encontradas = new ArrayList<>();

		agregarSiRequiereAfectacion(encontradas, participe.getNovedadesCarga());
		agregarSiRequiereAfectacion(encontradas, participe.getNovedadesFinancieras());

		if (novedades != null) {
			for (NovedadParticipeCarga novedad : novedades) {
				agregarSiRequiereAfectacion(encontradas, novedad.getTipoNovedad());
			}
		}

		return encontradas;
	}

	private void agregarSiRequiereAfectacion(List<Long> acumulador, Long tipoNovedad) {
		if (tipoNovedad != null
				&& NOVEDADES_REQUIEREN_AFECTACION_MANUAL.contains(tipoNovedad)
				&& !acumulador.contains(tipoNovedad)) {
			acumulador.add(tipoNovedad);
		}
	}

	/**
	 * Suma el valor que el usuario dejó indicado en las afectaciones manuales.
	 *
	 * Solo cuentan las afectaciones que el aplicador va a usar realmente: las que
	 * no tienen cuota asociada se omiten al procesar, así que aquí tampoco suman.
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
	private void aplicarPagoParticipe(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo) throws Throwable {
		System.out.println("========================================");
		System.out.println("APLICAR PAGO " + codigoProducto + " - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
		System.out.println("Monto: $" + participe.getTotalDescontado());
		
		// ✅ OPTIMIZACIÓN CRÍTICA: Si el monto es 0, marcar cuotas en mora INMEDIATAMENTE
		// Sin hacer búsquedas innecesarias de entidad, productos, préstamos o seguro HS
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
			System.out.println("⚠️ Monto descontado es $0 - No se realizó pago - Marcando cuotas en MORA");
			marcarCuotasEnMoraPorFaltaDePago(participe, codigoProducto, cargaArchivo);
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
			return;
		}
		Entidad entidad = entidades.get(0);
		System.out.println("✅ Entidad encontrada: " + entidad.getRazonSocial() + " (ID: " + entidad.getCodigo() + ")");
		
		// ✅ OPTIMIZACIÓN: Buscar productos UNA SOLA VEZ
		List<Producto> productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
		if (productos == null || productos.isEmpty()) {
			System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontró producto con código Petro: " + codigoProducto);
			return;
		}
		System.out.println("✅ Producto(s) encontrado(s): " + productos.size());
		
		// ✅ OPTIMIZACIÓN: Buscar préstamos UNA SOLA VEZ
		List<Prestamo> prestamos = new ArrayList<>();
		for (Producto producto : productos) {
			List<Prestamo> prestamosDelProducto = 
				prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
			if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
				prestamos.addAll(prestamosDelProducto);
			}
		}
		
		if (prestamos.isEmpty()) {
			System.out.println("⚠️ PROCESAMIENTO OMITIDO - No se encontraron préstamos activos para entidad: " + 
			                   entidad.getRazonSocial() + " (ID: " + entidad.getCodigo() + ") y producto: " + codigoProducto);
			return;
		}
		System.out.println("✅ Préstamo(s) activo(s) encontrado(s): " + prestamos.size());
		
		// Inicializar variables para HS
		double montoHS = 0.0;
		
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
			        }
				} 
			} catch (Throwable e) {
				throw new RuntimeException("Falló al validar el seguro de incendio (HS) del partícipe "
					+ participe.getCodigoPetro() + " (" + participe.getNombre() + "), producto "
					+ codigoProducto + ": " + e.getMessage(), e);
			}
			System.out.println("========================================");
		}
		
	   // BUSCAR CUOTA A PAGAR Y PROCESAR
	   DetallePrestamo cuotaAPagar = buscarCuotaAPagar(prestamos, cargaArchivo);
	   
	   if (cuotaAPagar != null) {
	       System.out.println("✅ Cuota a pagar encontrada: #" + cuotaAPagar.getNumeroCuota());
	       procesarPagoCuota(participe, cuotaAPagar, montoArchivo, montoHS, cargaArchivo);
	   } else {
	       System.out.println("⚠️ No se encontró cuota pendiente");
	   }

	   // ✅ RED DE SEGURIDAD: revisar el estado de TODOS los préstamos tocados.
	   // buscarCuotaAPagar y calcularSaldosRealesCuota también pueden marcar cuotas
	   // como PAGADA (por saldo insignificante o por PagoPrestamo previo), y en ese caso
	   // la última cuota se liquida sin pasar por procesarPagoCuota.
	   verificarYActualizarEstadoPrestamos(prestamos);

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
			SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
			
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
 * Procesa el pago de una cuota de préstamo ACTIVO (no de plazo vencido)
 */
private void procesarPagoCuota(ParticipeXCargaArchivo participe, 
                               DetallePrestamo cuota,
                               double montoPagado,
                               double valorSeguroIncendio,
                               CargaArchivo cargaArchivo) throws Throwable {
	
	System.out.println("    Procesando pago cuota #" + cuota.getNumeroCuota() + " - Monto: $" + montoPagado + " (Seguro incendio: $" + valorSeguroIncendio + ")");
	
	// ✅ CORRECCIÓN CRÍTICA: Calcular saldos reales consultando la tabla PagoPrestamo
	// Esto asegura que si hay pagos previos registrados, se tomen en cuenta correctamente
	SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
	
	// ✅ CORRECCIÓN: Recargar el estado de la cuota después de calcularSaldosRealesCuota
	// porque ese método puede haberla actualizado a PAGADA si ya estaba completa
	Long estadoActualizado = cuota.getEstado();
	
	// Si la cuota fue actualizada a PAGADA por calcularSaldosRealesCuota, pasar el excedente
	if (estadoActualizado != null && estadoActualizado == com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
		System.out.println("      ℹ️ Cuota ya está PAGADA según PagoPrestamo - Pasando todo el monto a siguiente cuota");
		procesarExcedenteASiguienteCuota(participe, cuota, montoPagado, cargaArchivo);
		// ✅ CRÍTICO: el excedente pudo liquidar la última cuota del préstamo
		verificarYActualizarEstadoPrestamo(cuota.getPrestamo());
		return;
	}
	
	System.out.println("      Saldos reales según PagoPrestamo:");
	System.out.println("        Desgravamen pendiente: $" + saldos.saldoDesgravamen);
	System.out.println("        Interés pendiente: $" + saldos.saldoInteres);
	System.out.println("        Capital pendiente: $" + saldos.saldoCapital);
	System.out.println("        Seguro Incendio pendiente: $" + saldos.saldoSeguroIncendio);
	System.out.println("        TOTAL pendiente: $" + saldos.totalPendiente);
	
	// Usar los saldos reales calculados desde PagoPrestamo
	double desgravamenPendiente = saldos.saldoDesgravamen;
	double interesPendiente = saldos.saldoInteres;
	double capitalPendiente = saldos.saldoCapital;
	double seguroIncendioPendiente = saldos.saldoSeguroIncendio;
	double totalPendiente = saldos.totalPendiente;
	
	// ✅ VALIDACIÓN INFORMATIVA: Verificar que el desglose de la cuota coincida con los valores del archivo
	// Esto es solo para logging - NO afecta si la cuota se marca como PAGADA o no
	boolean desgloseCoincide = validarDesgloseCuotaSinTolerancia(cuota, participe, valorSeguroIncendio);
	
	if (!desgloseCoincide) {
		System.out.println("      ⚠️ ADVERTENCIA: El desglose del archivo no coincide exactamente con la cuota");
		System.out.println("      ℹ️ Se aplicarán los valores recibidos de todas formas");
	}
	
	// Variables para registrar los valores pagados en esta operación
	double desgravamenPagar = 0.0;
	double interesPagar = 0.0;
	double capitalPagar = 0.0;
	double seguroIncendioPagar = 0.0;
	
	// ✅ LÓGICA CORREGIDA: Lo importante es si el monto cubre el saldo, no si el desglose coincide
	// Si hay dinero suficiente para cubrir la cuota, se marca como PAGADA independientemente del desglose
	
	if (Math.abs(montoPagado - totalPendiente) <= 0.01) {
		// ✅ Pago exacto del saldo pendiente → PAGADA (independiente del desglose)
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setFechaPagado(java.time.LocalDateTime.now());
		
		// ✅ ACUMULAR: Completar el pago total de la cuota
		cuota.setCapitalPagado(nullSafe(cuota.getCapital()));
		cuota.setInteresPagado(nullSafe(cuota.getInteres()));
		cuota.setDesgravamenPagado(nullSafe(cuota.getDesgravamen()));
		cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
		cuota.setSaldoInteres(0.0);
		
		// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
		double totalCuota = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
		                    nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio());
		double totalPagadoCuota = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
		                          cuota.getDesgravamenPagado() + valorSeguroIncendio;
		cuota.setSaldo(Math.max(0, totalCuota - totalPagadoCuota));
		
		// Valores pagados en esta operación
		desgravamenPagar = desgravamenPendiente;
		interesPagar = interesPendiente;
		capitalPagar = capitalPendiente;
		seguroIncendioPagar = seguroIncendioPendiente;
		
		if (desgloseCoincide) {
			System.out.println("      ✅ Cuota PAGADA completamente (monto correcto y desglose coincide)");
		} else {
			System.out.println("      ✅ Cuota PAGADA completamente (monto correcto - desglose no coincide pero se aplicó)");
		}
		
	} else if (montoPagado > totalPendiente) {
		// ✅ Pago con excedente → PAGADA y procesar excedente (independiente del desglose)
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setFechaPagado(java.time.LocalDateTime.now());
		
		// ✅ ACUMULAR: Completar el pago total de la cuota
		cuota.setCapitalPagado(nullSafe(cuota.getCapital()));
		cuota.setInteresPagado(nullSafe(cuota.getInteres()));
		cuota.setDesgravamenPagado(nullSafe(cuota.getDesgravamen()));
		cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
		cuota.setSaldoInteres(0.0);
		
		// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
		double totalCuota = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
		                    nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio());
		double totalPagadoCuota = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
		                          cuota.getDesgravamenPagado() + valorSeguroIncendio;
		cuota.setSaldo(Math.max(0, totalCuota - totalPagadoCuota));
		
		// Valores pagados en esta operación
		desgravamenPagar = desgravamenPendiente;
		interesPagar = interesPendiente;
		capitalPagar = capitalPendiente;
		seguroIncendioPagar = seguroIncendioPendiente;
		
		double excedente = montoPagado - totalPendiente;
		
		if (desgloseCoincide) {
			System.out.println("      ✅ Cuota PAGADA con excedente: $" + excedente + " (desglose coincide)");
		} else {
			System.out.println("      ✅ Cuota PAGADA con excedente: $" + excedente + " (desglose no coincide pero se aplicó)");
		}
		
		// Guardar la cuota antes de procesar el excedente
		cuota.setCodigoExterno(cargaArchivo.getCodigo());
		detallePrestamoService.saveSingle(cuota);
		
		// Crear registro de pago para esta cuota
		crearRegistroPago(cuota, totalPendiente, 
			capitalPagar, interesPagar, desgravamenPagar,
			valorSeguroIncendio, // ✅ CORRECCIÓN: Usar el valor real del seguro de incendio (HS)
			String.format("Pago cuota #%d - Mes %d/%d - Carga %d",
				cuota.getNumeroCuota().intValue(),
				cargaArchivo.getMesAfectacion(),
				cargaArchivo.getAnioAfectacion(),
				cargaArchivo.getCodigo()),
			cargaArchivo);
		
		// ✅ CORRECCIÓN: Procesar excedente SIEMPRE que haya, independiente del desglose
		procesarExcedenteASiguienteCuota(participe, cuota, excedente, cargaArchivo);

		// ✅ CRÍTICO: Verificar si todas las cuotas están pagadas.
		// Este es el caso típico de la ÚLTIMA cuota: se paga completa y sobra un excedente
		// que ya no tiene a dónde aplicarse; sin esta llamada el préstamo quedaba VIGENTE.
		verificarYActualizarEstadoPrestamo(cuota.getPrestamo());
		return; // Salir porque ya se guardó la cuota

	} else {
		// Pago parcial - Respetar orden: Desgravamen → Interés → Capital → Seguro Incendio
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		
		double montoRestante = montoPagado;
		
		// 1. Pagar Desgravamen primero
		if (montoRestante > 0 && desgravamenPendiente > 0) {
			if (montoRestante >= desgravamenPendiente) {
				desgravamenPagar = desgravamenPendiente;
				montoRestante -= desgravamenPendiente;
			} else {
				desgravamenPagar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 2. Pagar Interés
		if (montoRestante > 0 && interesPendiente > 0) {
			if (montoRestante >= interesPendiente) {
				interesPagar = interesPendiente;
				montoRestante -= interesPendiente;
			} else {
				interesPagar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 3. Pagar Capital
		if (montoRestante > 0 && capitalPendiente > 0) {
			if (montoRestante >= capitalPendiente) {
				capitalPagar = capitalPendiente;
				montoRestante -= capitalPendiente;
			} else {
				capitalPagar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 4. Pagar Seguro de Incendio (para PH/PP)
		if (montoRestante > 0 && seguroIncendioPendiente > 0) {
			if (montoRestante >= seguroIncendioPendiente) {
				seguroIncendioPagar = seguroIncendioPendiente;
				montoRestante -= seguroIncendioPendiente;
			} else {
				seguroIncendioPagar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// ✅ ACUMULAR pagos sobre los valores actuales de la cuota (NO reemplazar)
		double capitalPagadoActual = nullSafe(cuota.getCapitalPagado());
		double interesPagadoActual = nullSafe(cuota.getInteresPagado());
		double desgravamenPagadoActual = nullSafe(cuota.getDesgravamenPagado());
		
		cuota.setCapitalPagado(capitalPagadoActual + capitalPagar);
		cuota.setInteresPagado(interesPagadoActual + interesPagar);
		cuota.setDesgravamenPagado(desgravamenPagadoActual + desgravamenPagar);
		cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
		cuota.setSaldoInteres(nullSafe(cuota.getInteres()) - cuota.getInteresPagado());
		
		// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
		double totalCuota = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
		                    nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio());
		double totalPagadoCuota = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
		                          cuota.getDesgravamenPagado() + seguroIncendioPagar;
		cuota.setSaldo(Math.max(0, totalCuota - totalPagadoCuota));
		
		System.out.println("      ⚠️ Cuota PARCIAL - Recibido: $" + montoPagado + " de $" + totalPendiente + " pendiente");
		System.out.println("        Desgravamen: $" + desgravamenPagar + "/" + desgravamenPendiente);
		System.out.println("        Interés: $" + interesPagar + "/" + interesPendiente);
		System.out.println("        Capital: $" + capitalPagar + "/" + capitalPendiente);
		System.out.println("        Seguro Incendio: $" + seguroIncendioPagar + "/" + seguroIncendioPendiente);
	}
	
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	detallePrestamoService.saveSingle(cuota);
	
	// Crear registro de pago (solo del monto aplicado en esta llamada, no de pagos previos)
	String observacion = String.format("Pago cuota #%d - Mes %d/%d - Carga %d",
		cuota.getNumeroCuota().intValue(),
		cargaArchivo.getMesAfectacion(),
		cargaArchivo.getAnioAfectacion(),
		cargaArchivo.getCodigo());
	
	// ✅ CORRECCIÓN: Incluir seguro de incendio en el registro
	// En caso de pago parcial, usar los valores calculados en el bloque anterior
	// En caso de pago completo, usar totalPendiente
	double montoRegistrar = montoPagado > totalPendiente ? totalPendiente : montoPagado;
	double seguroIncendioRegistrar = montoPagado > totalPendiente ? seguroIncendioPendiente : seguroIncendioPagar;
	double desgravamenRegistrar = montoPagado > totalPendiente ? desgravamenPendiente : desgravamenPagar;
	double interesRegistrar = montoPagado > totalPendiente ? interesPendiente : interesPagar;
	double capitalRegistrar = montoPagado > totalPendiente ? capitalPendiente : capitalPagar;
	
	crearRegistroPago(cuota, montoRegistrar, 
		capitalRegistrar, interesRegistrar, desgravamenRegistrar,
		seguroIncendioRegistrar, // ✅ CORRECCIÓN: Usar el valor calculado del seguro de incendio pagado
		observacion, cargaArchivo);

	// ✅ CRÍTICO: Verificar si todas las cuotas están pagadas
	verificarYActualizarEstadoPrestamo(cuota.getPrestamo());

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
		Long totalCuotas = detallePrestamoDaoService.contarCuotasByPrestamo(prestamo.getCodigo());
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
                                               CargaArchivo cargaArchivo) throws Throwable {
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
		
		// Buscar productos
		List<Producto> productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
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
 * Procesa el excedente de una cuota aplicándolo a la siguiente cuota pendiente
 */
private void procesarExcedenteASiguienteCuota(ParticipeXCargaArchivo participe,
                                              DetallePrestamo cuota,
                                              double excedente,
                                              CargaArchivo cargaArchivo) throws Throwable {
	
	// ✅ SIN TOLERANCIA: Procesar cualquier excedente > 0, incluso 1 centavo
	if (excedente <= 0) {
		return;
	}
	
	System.out.println("      Procesando excedente de $" + excedente + " a siguiente cuota...");
	
	// ✅ MEGA OPTIMIZACIÓN: Buscar iterativamente la siguiente cuota pendiente con saldo real
	// solo si hay excedente que aplicar (ya validado arriba)
	int intentos = 0;
	int maxIntentos = 100; // Límite de seguridad
	
	while (intentos < maxIntentos) {
		intentos++;
		
		// Buscar la mínima cuota NO pagada del préstamo
		List<DetallePrestamo> resultado = 
			detallePrestamoDaoService.selectMinCuotaNoPagadaByPrestamo(cuota.getPrestamo().getCodigo());
		
		if (resultado == null || resultado.isEmpty()) {
			System.out.println("        ℹ️ No hay más cuotas pendientes - Excedente no aplicado: $" + excedente);
			return;
		}
		
		DetallePrestamo candidata = resultado.get(0);
		
		// La siguiente cuota debe tener número mayor a la cuota actual
		if (candidata.getNumeroCuota() <= cuota.getNumeroCuota()) {
			// Esta es la cuota actual o anterior, buscar más adelante
			// Marcarla como PAGADA si tiene saldo insignificante
			SaldosRealesCuota saldos = calcularSaldosRealesCuota(candidata);
			
			if (saldos.totalPendiente <= 0.01) {
				candidata.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
				detallePrestamoDaoService.save(candidata, candidata.getCodigo());
				continue; // Buscar la siguiente
			} else {
				// Tiene saldo pero es cuota anterior/actual - no debería pasar
				System.out.println("        ⚠️ Cuota #" + candidata.getNumeroCuota() + 
				                   " es anterior/igual a la actual #" + cuota.getNumeroCuota() + 
				                   " - Excedente no aplicado: $" + excedente);
				return;
			}
		}
		
		// Esta es una cuota posterior a la actual, validar saldo real
		SaldosRealesCuota saldos = calcularSaldosRealesCuota(candidata);
		
		// Si fue marcada como PAGADA por calcularSaldosRealesCuota, buscar la siguiente
		if (candidata.getEstado() != null && 
		    candidata.getEstado() == com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
			System.out.println("        ℹ️ Cuota #" + candidata.getNumeroCuota() + 
			                   " actualizada a PAGADA según PagoPrestamo - Buscando siguiente cuota (intento " + intentos + ")");
			continue;
		}
		
		// ✅ Si tiene saldo pendiente real, aplicar el excedente
		if (saldos.totalPendiente > 0.01) {
			System.out.println("        ✅ Aplicando excedente a cuota #" + candidata.getNumeroCuota() + 
			                   " (Saldo pendiente: $" + saldos.totalPendiente + ")");
			// ✅ El excedente no incluye seguro de incendio (ya se aplicó en la cuota anterior)
			procesarPagoCuota(participe, candidata, excedente, 0.0, cargaArchivo);
			return;
		}
		
		// Si tiene saldo insignificante, marcarla como PAGADA y continuar
		System.out.println("        ⚠️ Cuota #" + candidata.getNumeroCuota() + 
		                   " tiene saldo insignificante ($" + saldos.totalPendiente + 
		                   ") - Marcando como PAGADA y continuando");
		candidata.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		detallePrestamoDaoService.save(candidata, candidata.getCodigo());
	}
	
	if (intentos >= maxIntentos) {
		System.err.println("⚠️ ADVERTENCIA: Alcanzado límite de iteraciones (" + maxIntentos + ")");
	}
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
	int aplicadas = 0;
	List<Prestamo> prestamosAfectados = new ArrayList<>();
	for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
		DetallePrestamo cuota = afectacion.getDetallePrestamo();

		if (cuota == null) {
			// Ausencia de dato legítima: una AVPC sin cuota asociada no tiene a dónde aplicarse.
			System.out.println("   ⚠️ Afectación sin cuota asociada (ID: " + afectacion.getCodigo() + ") - Omitida");
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
 * ✅ SIN TOLERANCIA: Valida que el desglose de una cuota coincida exactamente con los valores del archivo.
 * Comparación exacta hasta 1 centavo de diferencia (0.01).
 * 
 * @param cuota La cuota a validar
 * @param participe El registro del archivo con los valores descontados
 * @param valorSeguroIncendio El valor del seguro de incendio (HS) si aplica
 * @return true si el desglose coincide exactamente, false en caso contrario
 */
private boolean validarDesgloseCuotaSinTolerancia(DetallePrestamo cuota, 
                                                   ParticipeXCargaArchivo participe,
                                                   double valorSeguroIncendio) {
	// Valores de la cuota
	double capitalCuota = nullSafe(cuota.getCapital());
	double interesCuota = nullSafe(cuota.getInteres());
	double desgravamenCuota = nullSafe(cuota.getDesgravamen());
	double seguroIncendioCuota = nullSafe(cuota.getValorSeguroIncendio());
	
	// Valores del archivo
	double capitalArchivo = nullSafe(participe.getCapitalDescontado());
	double interesArchivo = nullSafe(participe.getInteresDescontado());
	double desgravamenArchivo = nullSafe(participe.getSeguroDescontado());
	// El seguro de incendio viene en un registro separado (HS)
	double seguroIncendioArchivo = valorSeguroIncendio;
	
	// ✅ SIN TOLERANCIA: Comparación exacta (hasta 1 centavo)
	boolean capitalCoincide = Math.abs(capitalCuota - capitalArchivo) <= 0.01;
	boolean interesCoincide = Math.abs(interesCuota - interesArchivo) <= 0.01;
	boolean desgravamenCoincide = Math.abs(desgravamenCuota - desgravamenArchivo) <= 0.01;
	boolean seguroIncendioCoincide = Math.abs(seguroIncendioCuota - seguroIncendioArchivo) <= 0.01;
	
	// Log detallado de la comparación
	System.out.println("      🔍 Validación de desglose (SIN TOLERANCIA):");
	System.out.println("         Capital    - Cuota: $" + capitalCuota + " | Archivo: $" + capitalArchivo + " | " + (capitalCoincide ? "✅" : "❌"));
	System.out.println("         Interés    - Cuota: $" + interesCuota + " | Archivo: $" + interesArchivo + " | " + (interesCoincide ? "✅" : "❌"));
	System.out.println("         Desgravamen- Cuota: $" + desgravamenCuota + " | Archivo: $" + desgravamenArchivo + " | " + (desgravamenCoincide ? "✅" : "❌"));
	System.out.println("         Seg.Incendio-Cuota: $" + seguroIncendioCuota + " | Archivo: $" + seguroIncendioArchivo + " | " + (seguroIncendioCoincide ? "✅" : "❌"));
	
	boolean desgloseCoincide = capitalCoincide && interesCoincide && desgravamenCoincide && seguroIncendioCoincide;
	System.out.println("         RESULTADO: " + (desgloseCoincide ? "✅ COINCIDE" : "❌ NO COINCIDE"));
	
	return desgloseCoincide;
}

/**
 * Aplica una afectación manual de valores con registro de pago completo.
 * Este método se utiliza cuando se procesan afectaciones manuales de forma prioritaria.
 */
private void aplicarAfectacionManualConRegistroPago(
		DetallePrestamo cuota,
		AfectacionValoresParticipeCarga afectacion,
		CargaArchivo cargaArchivo,
		ParticipeXCargaArchivo participe) throws Throwable {
	
	double capitalAfectar = nullSafe(afectacion.getCapitalAfectar());
	double interesAfectar = nullSafe(afectacion.getInteresAfectar());
	double desgravamenAfectar = nullSafe(afectacion.getDesgravamenAfectar());
	double valorTotalAfectar = nullSafe(afectacion.getValorAfectar());
	// TODO MEJORA FUTURA: Agregar campo seguroIncendioAfectar a tabla AfectacionValoresParticipeCarga
	double seguroIncendioAfectar = 0.0; // Por ahora no se maneja seguro en afectaciones manuales
	
	System.out.println("      📋 Aplicando afectación manual (AVPC ID: " + afectacion.getCodigo() + ")");
	System.out.println("         Valores originales en AVPC:");
	System.out.println("         Capital a afectar: $" + capitalAfectar);
	System.out.println("         Interés a afectar: $" + interesAfectar);
	System.out.println("         Desgravamen a afectar: $" + desgravamenAfectar);
	System.out.println("         TOTAL a afectar: $" + valorTotalAfectar);
	
	// ✅ CRÍTICO: Calcular saldos reales desde tabla PagoPrestamo
	SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
	
	// ✅ CORRECCIÓN CRÍTICA: Si NO hay desglose manual (todos en 0), pero SÍ hay valorTotalAfectar,
	// entonces distribuir el monto respetando el orden correcto: Desgravamen → Interés → Capital
	double sumaDesglose = capitalAfectar + interesAfectar + desgravamenAfectar;
	
	if (sumaDesglose <= 0.01 && valorTotalAfectar > 0.01) {
		System.out.println("      ⚠️ DESGLOSE MANUAL VACÍO - Distribuyendo $" + valorTotalAfectar + 
		                   " según orden de prioridad: Desgravamen → Interés → Capital");
		
		double montoRestante = valorTotalAfectar;
		
		// 1. Aplicar primero al Desgravamen
		if (montoRestante > 0 && saldos.saldoDesgravamen > 0.01) {
			if (montoRestante >= saldos.saldoDesgravamen) {
				desgravamenAfectar = saldos.saldoDesgravamen;
				montoRestante -= saldos.saldoDesgravamen;
			} else {
				desgravamenAfectar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 2. Pagar Interés
		if (montoRestante > 0 && saldos.saldoInteres > 0.01) {
			if (montoRestante >= saldos.saldoInteres) {
				interesAfectar = saldos.saldoInteres;
				montoRestante -= saldos.saldoInteres;
			} else {
				interesAfectar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 3. Pagar Capital
		if (montoRestante > 0 && saldos.saldoCapital > 0.01) {
			if (montoRestante >= saldos.saldoCapital) {
				capitalAfectar = saldos.saldoCapital;
				montoRestante -= saldos.saldoCapital;
			} else {
				capitalAfectar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 4. Si quedó algo, aplicar al Seguro de Incendio (si existe)
		if (montoRestante > 0 && saldos.saldoSeguroIncendio > 0.01) {
			if (montoRestante >= saldos.saldoSeguroIncendio) {
				seguroIncendioAfectar = saldos.saldoSeguroIncendio;
				montoRestante -= saldos.saldoSeguroIncendio;
			} else {
				seguroIncendioAfectar = montoRestante;
				montoRestante = 0;
			}
		}
		
		System.out.println("      ✅ Distribución automática aplicada:");
		System.out.println("         Desgravamen: $" + desgravamenAfectar);
		System.out.println("         Interés: $" + interesAfectar);
		System.out.println("         Capital: $" + capitalAfectar);
		System.out.println("         Seguro Incendio: $" + seguroIncendioAfectar);
		if (montoRestante > 0.01) {
			System.out.println("         ⚠️ Excedente no aplicado: $" + montoRestante);
		}
	} else if (sumaDesglose > 0.01) {
		System.out.println("      ℹ️ Usando desglose manual de la tabla AVPC");
	}
	
	System.out.println("      Saldos actuales de la cuota:");
	System.out.println("         Capital pendiente: $" + saldos.saldoCapital);
	System.out.println("         Interés pendiente: $" + saldos.saldoInteres);
	System.out.println("         Desgravamen pendiente: $" + saldos.saldoDesgravamen);
	System.out.println("         TOTAL pendiente: $" + saldos.totalPendiente);
	
	// ✅ Obtener pagos previos acumulados para actualizar correctamente
	double capitalPagadoPrevio = nullSafe(cuota.getCapitalPagado());
	double interesPagadoPrevio = nullSafe(cuota.getInteresPagado());
	double desgravamenPagadoPrevio = nullSafe(cuota.getDesgravamenPagado());
	
	// ✅ ACUMULAR valores manuales sobre pagos previos
	cuota.setCapitalPagado(capitalPagadoPrevio + capitalAfectar);
	cuota.setInteresPagado(interesPagadoPrevio + interesAfectar);
	cuota.setDesgravamenPagado(desgravamenPagadoPrevio + desgravamenAfectar);
	
	cuota.setSaldoCapital(Math.max(0, nullSafe(cuota.getSaldoInicialCapital()) - cuota.getCapitalPagado()));
	cuota.setSaldoInteres(Math.max(0, nullSafe(cuota.getInteres()) - cuota.getInteresPagado()));
	
	// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
	double totalCuotaManual = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
	                          nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio());
	double totalPagadoCuotaManual = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
	                                cuota.getDesgravamenPagado() + seguroIncendioAfectar;
	cuota.setSaldo(Math.max(0, totalCuotaManual - totalPagadoCuotaManual));
	
	// ✅ Determinar el estado de la cuota según el total pagado
	double valorSeguroIncendio = nullSafe(cuota.getValorSeguroIncendio());
	double totalEsperado;
	double totalPagadoAcumulado;
	
	if (valorSeguroIncendio > 0.01) {
		// Cuota CON seguro de incendio (PH/PP) - calcular manualmente
		totalEsperado = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
		                nullSafe(cuota.getDesgravamen()) + valorSeguroIncendio;
		totalPagadoAcumulado = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
		                       cuota.getDesgravamenPagado();
		System.out.println("      ⚠️ ATENCIÓN: Cuota tiene seguro de incendio ($" + valorSeguroIncendio + 
		                   ") pero NO se puede afectar manualmente (campo no existe en tabla AVPC)");
	} else {
		// Cuota sin seguro de incendio - usar valores normales
		totalEsperado = totalBaseCuota(cuota);
		totalPagadoAcumulado = cuota.getCapitalPagado() + cuota.getInteresPagado() + cuota.getDesgravamenPagado();
	}
	
	// ✅ PROCESAMIENTO SIN TOLERANCIA: Comparación exacta para determinar si la cuota está completa
	if (Math.abs(totalPagadoAcumulado - totalEsperado) <= 0.01) {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setFechaPagado(java.time.LocalDateTime.now());
		System.out.println("      ✅ Cuota #" + cuota.getNumeroCuota() + " PAGADA COMPLETAMENTE (afectación manual)");
	} else {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		System.out.println("      ⚠️ Cuota #" + cuota.getNumeroCuota() + " PARCIAL (afectación manual) - Pagado: $" + 
		                   totalPagadoAcumulado + " de $" + totalEsperado);
	}
	
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	detallePrestamoService.saveSingle(cuota);
	
	// ✅ Registrar el pago en tabla PagoPrestamo
	String observacion = "Afectación manual AVPC (ID: " + afectacion.getCodigo() + 
	                     ") - Partícipe: " + participe.getCodigoPetro() + " - " + participe.getNombre();
	crearRegistroPago(cuota, valorTotalAfectar, capitalAfectar, interesAfectar, 
		desgravamenAfectar, seguroIncendioAfectar, observacion, cargaArchivo);
	
	System.out.println("      ✅ Pago registrado exitosamente en tabla PagoPrestamo");
}

/**
 * Ejecuta validaciones de fase 2 para un partícipe
 */
private void validarNovedadesFase2(ParticipeXCargaArchivo participe, 
                                   String codigoProducto, 
                                   CargaArchivo cargaArchivo) {
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
			productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
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
				registrarNovedad(participe, ASPNovedadesCargaArchivo.SIN_DESCUENTOS,
					"No se realizó ningún descuento para este partícipe. Las cuotas pendientes pasarán a mora",
					codigoProductoDB,
					codigoPrestamoDB,
					null,
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
	double totalPendiente = 0.0;
}

/**
 * Calcula los saldos reales de una cuota consultando la tabla PagoPrestamo
 * ✅ OPTIMIZACIÓN: Usa método específico en lugar de selectAll()
 * ✅ ACUMULA pagos de múltiples registros en PagoPrestamo
 */
private SaldosRealesCuota calcularSaldosRealesCuota(DetallePrestamo cuota) throws Throwable {
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
		// Obtener pagos específicos de esta cuota usando método específico del DAO
		List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
		
		if (pagos.isEmpty()) {
			// No hay pagos previos, los saldos son los valores originales de la cuota
			saldos.saldoDesgravamen = nullSafe(cuota.getDesgravamen());
			saldos.saldoInteres = nullSafe(cuota.getInteres());
			saldos.saldoCapital = nullSafe(cuota.getCapital());
			saldos.saldoSeguroIncendio = nullSafe(cuota.getValorSeguroIncendio());  // ✅ AGREGADO
			saldos.totalPendiente = totalBaseCuota(cuota);
			return saldos;
		}
		
		// Sumar todos los pagos realizados previamente
		double desgravamenPagadoTotal = 0.0;
		double interesPagadoTotal = 0.0;
		double capitalPagadoTotal = 0.0;
		double seguroIncendioPagadoTotal = 0.0;  // ✅ AGREGADO
		
		for (PagoPrestamo pago : pagos) {
			desgravamenPagadoTotal += nullSafe(pago.getDesgravamen());
			interesPagadoTotal += nullSafe(pago.getInteresPagado());
			capitalPagadoTotal += nullSafe(pago.getCapitalPagado());
			seguroIncendioPagadoTotal += nullSafe(pago.getValorSeguroIncendio());  // ✅ AGREGADO
		}
		
		// Calcular saldos pendientes
		saldos.saldoDesgravamen = Math.max(0, nullSafe(cuota.getDesgravamen()) - desgravamenPagadoTotal);
		saldos.saldoInteres = Math.max(0, nullSafe(cuota.getInteres()) - interesPagadoTotal);
		saldos.saldoCapital = Math.max(0, nullSafe(cuota.getCapital()) - capitalPagadoTotal);
		saldos.saldoSeguroIncendio = Math.max(0, nullSafe(cuota.getValorSeguroIncendio()) - seguroIncendioPagadoTotal);  // ✅ AGREGADO
		saldos.totalPendiente = saldos.saldoDesgravamen + saldos.saldoInteres + saldos.saldoCapital + saldos.saldoSeguroIncendio;  // ✅ INCLUIDO en total
		
		// ✅ Si el saldo total es 0 pero el estado no es PAGADA, actualizarlo
		if (saldos.totalPendiente <= 0.01 && cuota.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
			System.out.println("    ⚠️ Cuota #" + cuota.getNumeroCuota() + " completada según PagoPrestamo - Actualizando estado a PAGADA");
			cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			cuota.setIdEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA); // ✅ CRÍTICO: Actualizar también idEstado
			
			// ✅ Solo establecer fechaPagado si no existe
			if (cuota.getFechaPagado() == null) {
				cuota.setFechaPagado(java.time.LocalDateTime.now());
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
			
			// ✅ CRÍTICO: Actualizar saldo total global de la cuota (independiente del desglose)
			double totalCuotaRecalculo = nullSafe(cuota.getCapital()) + nullSafe(cuota.getInteres()) + 
			                             nullSafe(cuota.getDesgravamen()) + nullSafe(cuota.getValorSeguroIncendio());
			double totalPagadoCuotaRecalculo = capitalPagadoTotal + interesPagadoTotal + 
			                                   desgravamenPagadoTotal + seguroIncendioPagadoTotal;
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
 * Crea un registro en la tabla PagoPrestamo para mantener trazabilidad
 */
private void crearRegistroPago(DetallePrestamo cuota,
                               double montoTotal,
                               double capitalPagado,
                               double interesPagado,
                               double desgravamenPagado,
                               double valorSeguroIncendio,
                               String observacion,
                               CargaArchivo cargaArchivo) throws Throwable {
	// TODO O NADA (2026-08-29): antes, si el saveSingle fallaba, se tragaba el error acá — pero
	// el llamador YA había marcado la cuota como PAGADA/PARCIAL y YA la había guardado. Una
	// cuota "pagada" sin su PagoPrestamo detrás rompe el invariante del que depende todo el
	// resto del sistema ("PGPR es la fuente de verdad", ver calcularSaldosRealesCuota arriba y
	// MotorPagoPrestamoService). Un fallo de save es siempre un fallo real, nunca ausencia de
	// dato — no hay "if" que lo reemplace, debe propagar.
	try {
		PagoPrestamo pago = new PagoPrestamo();
		pago.setDetallePrestamo(cuota);
		pago.setPrestamo(cuota.getPrestamo());
		pago.setFecha(java.time.LocalDateTime.now());
		pago.setCapitalPagado(capitalPagado);
		pago.setInteresPagado(interesPagado);
		pago.setDesgravamen(desgravamenPagado);
		// ✅ CORRECCIÓN CRÍTICA: Guardar el valor del seguro de incendio (HS)
		pago.setValorSeguroIncendio(valorSeguroIncendio);
		pago.setValor(montoTotal);
		pago.setObservacion(observacion + " [CargaArchivo: " + cargaArchivo.getCodigo() + "]");
		pago.setEstado(1L);
		pago.setIdEstado(1L);
		// Trazabilidad (2026-08-28): de qué carga salió este pago, para el asiento de
		// APLICACION del cobro de Petro en dos pasos. Ver DDL-TRAZABILIDAD-CARGA-PETRO.sql.
		pago.setCargaArchivo(cargaArchivo);

		pagoPrestamoService.saveSingle(pago);

	} catch (Throwable e) {
		throw new RuntimeException("Falló al crear el registro de pago (PagoPrestamo) de la cuota #"
			+ cuota.getNumeroCuota() + " (código " + cuota.getCodigo() + ") del préstamo "
			+ (cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null)
			+ " por $" + montoTotal + " — la cuota ya había quedado marcada como pagada sin este "
			+ "respaldo. Causa: " + e.getMessage(), e);
	}
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
private int aplicarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo) throws Throwable {
	int aportesCreados = 0;

	// TODO O NADA (2026-08-29): antes, si distribuirAportePorDevengo fallaba a mitad de su
	// propio bucle (después de haber guardado ya algunos Aporte con crearNuevoAporte), esto se
	// tragaba el error y devolvía `aportesCreados` tal como quedó ANTES de esa asignación
	// (típicamente 0) — el resumen de aplicarPagosArchivoPetro ni contaba éxito ni contaba
	// error, como si no hubiera pasado nada, mientras algunos Aporte ya habían quedado
	// grabados a medias. Ya no hay catch general acá: cualquier fallo real propaga tal cual,
	// con el contexto que ya agrega cada método interno (crearNuevoAporte, crearRegistroPagoAporte).
	// Las ausencias de dato (sin entidad, monto $0, sin HistorialSueldo activo, sin ningún
	// aporte con valor) ya estaban manejadas con "if" explícitos y siguen exactamente igual.
	System.out.println("========================================");
	System.out.println("PROCESANDO APORTES (AH) - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
	System.out.println("========================================");

	// Buscar la entidad
	List<Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
	if (entidades == null || entidades.isEmpty()) {
		System.out.println("⚠️ No se encontró entidad para código Petro: " + participe.getCodigoPetro());
		return 0;
	}

	Entidad entidad = entidades.get(0);
	double montoRecibido = nullSafe(participe.getTotalDescontado());

	if (montoRecibido <= 0.01) {
		System.out.println("⚠️ Monto recibido es $0 para partícipe: " + participe.getCodigoPetro());
		// Sin descuento este mes: revisar si tampoco lo hubo en el periodo
		// anterior. Dos periodos seguidos sin aportar => ACTIVO EN MORA.
		evaluarMoraPorFaltaDeAporte(entidad, cargaArchivo);
		return 0;
	}

	// Llegó pago: si venía en mora, se pone al día.
	restaurarActivoPorPago(entidad, montoRecibido);

	System.out.println("📥 Monto total recibido: $" + montoRecibido);

	// ✅ Buscar valores esperados en HistorialSueldo con estado 99
	com.saa.model.crd.HistorialSueldo historialActivo =
		historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());

	if (historialActivo == null) {
		System.out.println("⚠️ No se encontró HistorialSueldo activo (estado 99) - No se pueden procesar aportes");
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
		System.out.println("⚠️ Ambos aportes tienen valor $0 - No se procesa nada");
		return 0;
	}

	try {
		// ✅ Fase 2 del plan de devengo de aportes: prelación por mes de devengo incompleto
		// más antiguo (§2.3), reemplaza la distinción único-tipo / alternado. Si un tipo no
		// tiene esperado (p. ej. cesantía = $0), su faltante mensual siempre es 0 y el reparto
		// nunca crea filas de ese tipo: no hace falta una rama aparte para "solo un tipo".
		aportesCreados = distribuirAportePorDevengo(entidad, montoRecibido, cargaArchivo, participe);
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
 * @return Cantidad de filas de Aporte creadas
 */
private int distribuirAportePorDevengo(Entidad entidad, double montoRecibido,
        CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {

	java.time.LocalDate mesCarga = java.time.LocalDate.of(
		cargaArchivo.getAnioAfectacion().intValue(), cargaArchivo.getMesAfectacion().intValue(), 1);

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
			double esperado = esperadoMensual(entidad, idTipo, mes);
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
			double esperado = esperadoMensual(entidad, idTipo, mes);
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
				Aporte nuevoAporte = crearNuevoAporte(entidad, idTipo, nombreTipo, aplicar, mes, cargaArchivo);
				crearRegistroPagoAporte(nuevoAporte, aplicar, cargaArchivo, participe);
				disponible -= aplicar;
				aportadoPorMesTipo.put(clave, aportado + aplicar);
				aportesCreados++;
			}
		}
		mes = mes.plusMonths(1);
	}

	if (guard >= TOPE_MESES_DEVENGO && disponible > 0.01) {
		System.err.println("⚠️ ADVERTENCIA: distribuirAportePorDevengo alcanzó el tope de "
			+ TOPE_MESES_DEVENGO + " meses. Entidad " + entidad.getCodigo() + " - Carga "
			+ cargaArchivo.getCodigo() + " - Disponible sin aplicar: $" + disponible);
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
 */
private double esperadoMensual(Entidad entidad, Long idTipoAporte, java.time.LocalDate mes) throws Throwable {
	return vigenciaContratoService.esperadoPorEntidad(entidad.getCodigo(), idTipoAporte, mes);
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
 */
private Aporte crearNuevoAporte(Entidad entidad, Long idTipoAporte, String nombreTipo,
                               double monto, java.time.LocalDate periodoDevengo,
                               CargaArchivo cargaArchivo) throws Throwable {

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
	nuevoAporte.setTipoMovimiento((long) com.saa.rubros.CrdTipoMovimientoAporte.APORTE_MENSUAL);
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
		com.saa.model.crd.PagoAporte pago = new com.saa.model.crd.PagoAporte();
		pago.setFilial(aporte.getFilia()); // ✅ Obtener filial desde el aporte (método es getFilia())
		pago.setAporte(aporte);
		pago.setValor(montoPagado);
		pago.setFechaContable(java.time.LocalDateTime.now());
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
