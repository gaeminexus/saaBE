package com.saa.ejb.asoprep.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.saa.basico.ejb.FileService;
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

import jakarta.ejb.EJB;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación Stateful para procesar archivos Petro con manejo de transacciones
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
    private com.saa.ejb.crd.service.AporteService aporteService;
    
    @EJB
    private com.saa.ejb.crd.dao.AporteDaoService aporteDaoService;
    
    @EJB
    private com.saa.ejb.crd.service.PagoAporteService pagoAporteService;
    
    @EJB
    private com.saa.ejb.crd.dao.PagoAporteDaoService pagoAporteDaoService;
    
    @EJB
    private com.saa.ejb.crd.dao.TipoAporteDaoService tipoAporteDaoService;
    
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
				try {
					participe = participeXCargaArchivoService.saveSingle(participe);
				} catch (Throwable e) {
					System.err.println("ERROR al insertar partícipe en PXCA (código=" + participe.getCodigoPetro() + 
									   ", producto=" + codigoProducto + "): " + e.getMessage());
					e.printStackTrace();
					// No detener el proceso - continuar con el siguiente registro
					continue;
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
    private Double parseDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0.0;
        
        try {
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
            
            // Convertir a double
            return Double.parseDouble(valorLimpio);
            
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Convierte string a Long simple (sin decimales)
     */
    private Long parseLongSimple(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0L;
        
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    /**
     * Devuelve 0 si el valor es null
     */
    private Double nullSafe(Double valor) {
        return valor != null ? valor : 0.0;
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
			// VALIDACIÓN: Solo se puede procesar la carga del siguiente mes
			// al último mes procesado (estado 3)
			// ==========================================
			validarOrdenProcesamiento(cargaArchivo);
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
					
					try {
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
						// ==========================================
						
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
						totalErrores++;
						System.err.println("Error al procesar partícipe " + participe.getCodigoPetro() + ": " + e.getMessage());
						e.printStackTrace();
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
				System.err.println("❌ ERROR al buscar/validar HS: " + e.getMessage());
				e.printStackTrace();
				System.out.println("❌ La cuota quedará como PARCIAL (solo se aplicará " + codigoProducto + " sin seguro)");
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
	
	// ✅ CORRECCIÓN CRÍTICA: Calcular saldos reales consultando tabla PagoPrestamo
	// Esto asegura que si hay pagos previos registrados, se tomen en cuenta correctamente
	SaldosRealesCuota saldos = calcularSaldosRealesCuota(cuota);
	
	// ✅ CORRECCIÓN: Recargar el estado de la cuota después de calcularSaldosRealesCuota
	// porque ese método puede haberla actualizado a PAGADA si ya estaba completa
	Long estadoActualizado = cuota.getEstado();
	
	// Si la cuota fue actualizada a PAGADA por calcularSaldosRealesCuota, pasar el excedente
	if (estadoActualizado != null && estadoActualizado == com.saa.rubros.EstadoCuotaPrestamo.PAGADA) {
		System.out.println("      ℹ️ Cuota ya está PAGADA según PagoPrestamo - Pasando todo el monto a siguiente cuota");
		procesarExcedenteASiguienteCuota(participe, cuota, montoPagado, cargaArchivo);
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
		return; // Salir porque ya se guardó la cuota
		
	} else {
		// Pago parcial - Respetar orden: Desgravamen → Interés → Capital → Seguro Incendio
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		
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
 * ✅ OPTIMIZACIÓN: Usa selectCuotasNoPagadasByPrestamo en lugar de traer todas
 */
private void verificarYActualizarEstadoPrestamo(Prestamo prestamo) throws Throwable {
	if (prestamo == null) {
		return;
	}
	
	try {
		// ✅ OPTIMIZACIÓN: Solo buscar cuotas NO pagadas
		// Si no hay ninguna, significa que todas están pagadas
		List<DetallePrestamo> cuotasNoPagadas = 
			detallePrestamoDaoService.selectCuotasNoPagadasByPrestamo(prestamo.getCodigo());
		
		// Si NO hay cuotas pendientes, todas están pagadas
		if (cuotasNoPagadas == null || cuotasNoPagadas.isEmpty()) {
			System.out.println("  ✅ TODAS LAS CUOTAS PAGADAS - Actualizando préstamo a CANCELADO");
			System.out.println("     Préstamo ID: " + prestamo.getCodigo());
			
			prestamo.setEstadoPrestamo(Long.valueOf(com.saa.rubros.EstadoPrestamo.CANCELADO));
			prestamo.setFechaFin(java.time.LocalDateTime.now());
			prestamoDaoService.save(prestamo, prestamo.getCodigo());
			
			System.out.println("     ✅ Préstamo actualizado a estado CANCELADO");
		}
		
	} catch (Throwable e) {
		System.err.println("Error al verificar estado del préstamo: " + e.getMessage());
		e.printStackTrace();
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
		System.err.println("❌ ERROR al marcar cuotas en mora: " + e.getMessage());
		e.printStackTrace();
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
		System.err.println("        ⚠️ Se alcanzó el límite de intentos (" + maxIntentos + 
		                   ") buscando siguiente cuota - Excedente no aplicado: $" + excedente);
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
	
	try {
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
		for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
			DetallePrestamo cuota = afectacion.getDetallePrestamo();
			
			if (cuota == null) {
				System.out.println("   ⚠️ Afectación sin cuota asociada (ID: " + afectacion.getCodigo() + ") - Omitida");
				continue;
			}
			
			System.out.println("   📌 Aplicando afectación manual a cuota #" + cuota.getNumeroCuota() + 
			                   " (ID: " + cuota.getCodigo() + ")");
			
			// Aplicar la afectación manual a esta cuota
			aplicarAfectacionManualConRegistroPago(cuota, afectacion, cargaArchivo, participe);
			aplicadas++;
		}
		
		System.out.println("   ✅ Se aplicaron " + aplicadas + " afectación(es) manual(es)");
		
		// Si se aplicó al menos una afectación, retornar true
		return aplicadas > 0;
		
	} catch (Throwable e) {
		System.err.println("❌ Error al verificar/aplicar afectaciones manuales totales: " + e.getMessage());
		e.printStackTrace();
		// En caso de error, retornar false para continuar con proceso normal
		return false;
	}
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
		
		// 2. Aplicar al Interés
		if (montoRestante > 0 && saldos.saldoInteres > 0.01) {
			if (montoRestante >= saldos.saldoInteres) {
				interesAfectar = saldos.saldoInteres;
				montoRestante -= saldos.saldoInteres;
			} else {
				interesAfectar = montoRestante;
				montoRestante = 0;
			}
		}
		
		// 3. Aplicar al Capital
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
		totalEsperado = nullSafe(cuota.getTotal());
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
				"El partícipe no tiene código Petro válido", null, null, null, null);
			return;
		}
		
		// Buscar la entidad por código Petro
		List<Entidad> entidades = null;
		try {
			entidades = entidadDaoService.selectByCodigoPetro(rolPetroLong);
		} catch (Throwable e) {
			System.err.println("Error al buscar entidad " + rolPetroLong + ": " + e.getMessage());
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"Error al buscar entidad: " + e.getMessage(), null, null, null, null);
			return;
		}
		
		if (entidades == null || entidades.isEmpty()) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO,
				"No se encontró entidad con código Petro: " + rolPetroLong, null, null, null, null);
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
				codigoProductoDB, null, null, null);
			return;
		}
		
		// VALIDACIÓN 12: CUOTA_NO_ENCONTRADA / CUOTA_FECHA_DIFERENTE
		// IMPORTANTE: Si el código es PH o PP, el seguro viene en un registro separado con código HS
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		
		// Caso especial para PH (Hipotecario) y PP (Prendario): buscar el registro HS correspondiente
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
									"No se realizó el pago de la cuota #" + cuota.getNumeroCuota() + ". La cuota pasará a mora",
									prestamo.getProducto().getCodigo(),
									prestamo.getCodigo(),
									nullSafe(cuota.getTotal()),
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
				double montoCuota = nullSafe(cuotaDelPrestamo.getTotal());
				double diferencia = Math.abs(montoCuota - montoArchivo);
				
				if (diferencia <= TOLERANCIA) {
					cuotasEncontradas.clear();
					cuotasEncontradas.add(cuotaDelPrestamo);
					
					if (diferencia > 0.01 && diferencia <= TOLERANCIA) {
						double montoCuotaEncontrada = nullSafe(cuotaDelPrestamo.getTotal());
						String descripcion = String.format("Diferencia menor a $1 - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
							montoCuotaEncontrada, montoArchivo, diferencia);
						registrarNovedad(participe, ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR,
							descripcion,
							cuotaDelPrestamo.getPrestamo().getProducto().getCodigo(), 
							cuotaDelPrestamo.getPrestamo().getCodigo(), 
							montoCuotaEncontrada, montoArchivo);
					}
					
					if (cuotaConFechaDiferente) {
						double montoCuotaEncontrada = nullSafe(cuotaDelPrestamo.getTotal());
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
			montoEsperadoTotal += nullSafe(cuota.getTotal());
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
		// Antes: Traía TODOS los historiales y verificaba en Java si había más de 1
		// Ahora: Usa método optimizado que trae solo el activo
		com.saa.model.crd.HistorialSueldo historial = 
			historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());
		
		if (historial == null) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
				"No existe HistorialSueldo activo (estado 99) para la entidad", null, null, null, null);
			return;
		}
		
		// Validar que los valores no sean NULL
		Double montoJubilacion = historial.getMontoJubilacion();
		Double montoCesantia = historial.getMontoCesantia();
		
		if (montoJubilacion == null || montoCesantia == null) {
			registrarNovedad(participe, ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
				"Valores NULL en HistorialSueldo - Jubilación: " + montoJubilacion + ", Cesantía: " + montoCesantia,
				null, null, null, null);
			return;
		}
		
		// Calcular el monto esperado total (Jubilación + Cesantía)
		double montoEsperado = nullSafe(montoJubilacion) + nullSafe(montoCesantia);
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		
		// Validar si el monto del archivo es 0
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
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
	
	try {
		// Obtener pagos específicos de esta cuota usando método específico del DAO
		List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
		
		if (pagos.isEmpty()) {
			// No hay pagos previos, los saldos son los valores originales de la cuota
			saldos.saldoDesgravamen = nullSafe(cuota.getDesgravamen());
			saldos.saldoInteres = nullSafe(cuota.getInteres());
			saldos.saldoCapital = nullSafe(cuota.getCapital());
			saldos.saldoSeguroIncendio = nullSafe(cuota.getValorSeguroIncendio());  // ✅ AGREGADO
			saldos.totalPendiente = nullSafe(cuota.getTotal());
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
		System.err.println("Error al calcular saldos reales: " + e.getMessage());
		e.printStackTrace();
		// En caso de error, usar valores de la cuota
		saldos.saldoDesgravamen = nullSafe(cuota.getDesgravamen());
		saldos.saldoInteres = nullSafe(cuota.getInteres());
		saldos.saldoCapital = nullSafe(cuota.getCapital());
		saldos.totalPendiente = nullSafe(cuota.getTotal());
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
	try {
		PagoPrestamo pago = new PagoPrestamo();
		pago.setDetallePrestamo(cuota);
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
		
		pagoPrestamoService.saveSingle(pago);
		
	} catch (Throwable e) {
		System.err.println("Error al crear registro de pago: " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Valida que solo se pueda procesar una carga si es del mes siguiente al último mes procesado
 * ✅ OPTIMIZADO: Usa consulta directa para obtener la última carga sin traer todas
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
 * Aplica aportes para el producto AH (aportes de jubilación y cesantía)
 * ✅ NUEVA LÓGICA: Similar a procesamiento de préstamos con estados, acumulación y excedentes
 */
private int aplicarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo) throws Throwable {
	int aportesCreados = 0;
	
	try {
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
			return 0;
		}
		
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
		
		// ✅ PROCESAR APORTE DE JUBILACIÓN (si existe monto esperado)
		if (montoEsperadoJubilacion > 0.01) {
			double montoParaJubilacion = Math.min(montoRecibido, montoEsperadoJubilacion);
			double montoUtilizado = procesarAporteIndividual(entidad, TIPO_APORTE_JUBILACION, "Jubilación", 
			                        montoEsperadoJubilacion, montoParaJubilacion, 
			                        cargaArchivo, participe);
			montoRecibido -= montoUtilizado; // ✅ Restar solo lo que realmente se utilizó
			aportesCreados++;
		}
		
		// ✅ PROCESAR APORTE DE CESANTÍA (si existe monto esperado y queda saldo)
		if (montoEsperadoCesantia > 0.01 && montoRecibido > 0.01) {
			double montoParaCesantia = Math.min(montoRecibido, montoEsperadoCesantia);
			double montoUtilizado = procesarAporteIndividual(entidad, TIPO_APORTE_CESANTIA, "Cesantía", 
			                        montoEsperadoCesantia, montoParaCesantia, 
			                        cargaArchivo, participe);
			montoRecibido -= montoUtilizado; // ✅ Restar solo lo que realmente se utilizó
			aportesCreados++;
		}
		
		// ✅ MANEJO DE EXCEDENTE: Si sobra dinero, crear aportes del siguiente mes
		if (montoRecibido > 0.01) {
			System.out.println("💵 EXCEDENTE DETECTADO: $" + montoRecibido + " - Creando aportes para mes siguiente");
			crearAportesMesSiguiente(entidad, montoRecibido, cargaArchivo, participe);
		}
		
		System.out.println("✅ Procesamiento de aportes completado - Total aportes: " + aportesCreados);
		System.out.println("========================================\n");
		
	} catch (Throwable e) {
		System.err.println("❌ Error al aplicar aportes AH: " + e.getMessage());
		e.printStackTrace();
	}
	
	return aportesCreados;
}

/**
 * Procesa un aporte individual (Jubilación o Cesantía) con lógica de estados y acumulación
 * ✅ CORREGIDO: Retorna el monto total utilizado (saldo anterior + mes actual)
 * 
 * @return Monto total utilizado en este procesamiento
 */
private double procesarAporteIndividual(Entidad entidad, Long idTipoAporte, String nombreTipo,
                                     double valorEsperado, double montoPagar,
                                     CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {
	
	System.out.println("\n📋 Procesando aporte de " + nombreTipo + ":");
	System.out.println("   Valor esperado mes actual: $" + valorEsperado);
	System.out.println("   Monto recibido: $" + montoPagar);
	
	double montoDisponible = montoPagar;
	double montoTotalUtilizado = 0.0; // ✅ Contador de cuánto dinero se usó
	
	// ✅ PASO 1: BUSCAR APORTES ANTERIORES CON SALDO PENDIENTE
	// Debe aplicarse PRIMERO a cualquier aporte anterior con saldo > 0
	Aporte aporteConSaldo = buscarAporteConSaldoPendiente(entidad, idTipoAporte, cargaArchivo);
	
	if (aporteConSaldo != null) {
		// ✅ APLICAR PRIMERO AL APORTE EXISTENTE CON SALDO
		System.out.println("   🔍 Encontrado aporte anterior con saldo pendiente:");
		System.out.println("      ID Aporte: " + aporteConSaldo.getCodigo());
		System.out.println("      Valor: $" + aporteConSaldo.getValor());
		System.out.println("      Ya pagado: $" + nullSafe(aporteConSaldo.getValorPagado()));
		System.out.println("      Saldo: $" + nullSafe(aporteConSaldo.getSaldo()));
		
		double saldoPendiente = nullSafe(aporteConSaldo.getSaldo());
		double montoAAplicar = Math.min(montoDisponible, saldoPendiente);
		
		System.out.println("      💰 Aplicando: $" + montoAAplicar + " al aporte anterior");
		
		// Actualizar el aporte anterior
		double nuevoValorPagado = nullSafe(aporteConSaldo.getValorPagado()) + montoAAplicar;
		double nuevoSaldo = Math.max(0, nullSafe(aporteConSaldo.getValor()) - nuevoValorPagado);
		
		aporteConSaldo.setValorPagado(nuevoValorPagado);
		aporteConSaldo.setSaldo(nuevoSaldo);
		
		// Actualizar estado
		if (nuevoSaldo <= 0.01) {
			aporteConSaldo.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			System.out.println("      ✅ Aporte anterior completado - Estado: PAGADA");
		} else {
			aporteConSaldo.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
			System.out.println("      ⚠️ Aporte anterior aún con saldo - Estado: PARCIAL - Saldo: $" + nuevoSaldo);
		}
		
		aporteService.saveSingle(aporteConSaldo);
		crearRegistroPagoAporte(aporteConSaldo, montoAAplicar, cargaArchivo, participe);
		
		// Reducir el monto disponible y actualizar el contador
		montoDisponible -= montoAAplicar;
		montoTotalUtilizado += montoAAplicar;
		System.out.println("      💵 Monto restante para mes actual: $" + montoDisponible);
	}
	
	// ✅ PASO 2: CREAR APORTE DEL MES ACTUAL SOLO SI HAY MONTO DISPONIBLE
	if (montoDisponible > 0.01) {
		System.out.println("   ✨ Creando aporte del mes actual con monto: $" + montoDisponible);
		
		Aporte aporteActual = crearNuevoAporte(entidad, idTipoAporte, nombreTipo, valorEsperado, cargaArchivo);
		
		// Aplicar el monto disponible al aporte del mes actual
		double nuevoValorPagado = montoDisponible;
		double nuevoSaldo = Math.max(0, valorEsperado - nuevoValorPagado);
		
		aporteActual.setValorPagado(nuevoValorPagado);
		aporteActual.setSaldo(nuevoSaldo);
		
		// Determinar estado
		if (nuevoSaldo <= 0.01) {
			aporteActual.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			System.out.println("   ✅ Aporte mes actual completado - Estado: PAGADA");
		} else if (nuevoValorPagado > 0.01) {
			aporteActual.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
			System.out.println("   ⚠️ Aporte mes actual parcial - Pagado: $" + nuevoValorPagado + " - Saldo: $" + nuevoSaldo);
		} else {
			aporteActual.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PENDIENTE);
			System.out.println("   ⏳ Aporte mes actual pendiente - Saldo: $" + nuevoSaldo);
		}
		
		aporteService.saveSingle(aporteActual);
		
		if (nuevoValorPagado > 0.01) {
			crearRegistroPagoAporte(aporteActual, nuevoValorPagado, cargaArchivo, participe);
		}
		
		// Actualizar el contador con lo usado en el mes actual
		montoTotalUtilizado += nuevoValorPagado;
	} else {
		System.out.println("   ℹ️ No queda monto para crear aporte del mes actual (todo aplicado a aportes anteriores)");
	}
	
	System.out.println("   💰 TOTAL UTILIZADO EN ESTE TIPO: $" + montoTotalUtilizado);
	return montoTotalUtilizado; // ✅ Retornar cuánto dinero se usó realmente
}

/**
 * Busca CUALQUIER aporte anterior con saldo pendiente (PENDIENTE o PARCIAL)
 * ✅ CORREGIDO: Busca el aporte más antiguo con saldo > 0, sin importar quién lo creó
 * 
 * Esto asegura que los pagos se apliquen primero a deudas anteriores antes de crear nuevos aportes
 */
private Aporte buscarAporteConSaldoPendiente(Entidad entidad, Long idTipoAporte, CargaArchivo cargaArchivo) throws Throwable {
	// ✅ Buscar el aporte más antiguo (MIN codigo) con saldo pendiente
	// Esto garantiza que aplicamos los pagos en orden cronológico (FIFO)
	Aporte aporteConSaldo = aporteDaoService.selectMinAporteConSaldo(
		entidad.getCodigo(), 
		idTipoAporte
	);
	
	if (aporteConSaldo != null) {
		System.out.println("      🔍 Aporte anterior con saldo encontrado (aplicar primero aquí):");
		System.out.println("         ID: " + aporteConSaldo.getCodigo());
		System.out.println("         IdAsoprep: " + aporteConSaldo.getIdAsoprep() + " (carga anterior)");
		System.out.println("         Usuario: " + aporteConSaldo.getUsuarioRegistro());
		System.out.println("         Estado: " + (aporteConSaldo.getEstado() == 1L ? "PENDIENTE" : "PARCIAL"));
		System.out.println("         Valor: $" + aporteConSaldo.getValor());
		System.out.println("         Valor Pagado: $" + nullSafe(aporteConSaldo.getValorPagado()));
		System.out.println("         Saldo: $" + nullSafe(aporteConSaldo.getSaldo()));
	}
	
	return aporteConSaldo;
}

/**
 * Crea un nuevo registro de aporte
 */
private Aporte crearNuevoAporte(Entidad entidad, Long idTipoAporte, String nombreTipo,
                               double valorEsperado, CargaArchivo cargaArchivo) throws Throwable {
	
	com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipoAporte, "TipoAporte");
	
	Aporte nuevoAporte = new Aporte();
	nuevoAporte.setFilial(entidad.getFilial()); // ✅ Obtener filial desde la entidad
	nuevoAporte.setEntidad(entidad);
	nuevoAporte.setTipoAporte(tipoAporte);
	nuevoAporte.setValor(valorEsperado);
	nuevoAporte.setValorPagado(0.0);
	nuevoAporte.setSaldo(valorEsperado);
	nuevoAporte.setIdAsoprep(cargaArchivo.getCodigo()); // ✅ CRÍTICO: Enlazar con CargaArchivo
	nuevoAporte.setFechaTransaccion(java.time.LocalDateTime.now());
	nuevoAporte.setGlosa(String.format("Aporte %s - Mes %d/%d - CargaArchivo: %d",
	                                   nombreTipo,
	                                   cargaArchivo.getMesAfectacion(),
	                                   cargaArchivo.getAnioAfectacion(),
	                                   cargaArchivo.getCodigo()));
	nuevoAporte.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PENDIENTE); // ✅ Estado PENDIENTE inicial
	nuevoAporte.setFechaRegistro(java.time.LocalDateTime.now());
	nuevoAporte.setUsuarioRegistro("SAA_AH"); // ✅ Identificador de aportes creados por el sistema
	
	aporteService.saveSingle(nuevoAporte);
	
	System.out.println("   ✅ Nuevo aporte creado - Valor esperado: $" + valorEsperado);
	
	return nuevoAporte;
}

/**
 * Crea un registro en la tabla PagoAporte para trazabilidad
 */
private void crearRegistroPagoAporte(Aporte aporte, double montoPagado, 
                                    CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {
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
		System.err.println("   ❌ Error al crear registro de pago aporte: " + e.getMessage());
		e.printStackTrace();
	}
}

/**
 * Crea aportes del siguiente mes cuando hay excedente
 */
private void crearAportesMesSiguiente(Entidad entidad, double montoExcedente,
                                     CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {
	
	System.out.println("\n💵 Creando aportes para mes siguiente con excedente: $" + montoExcedente);
	
	// Calcular mes y año siguiente
	int mesSiguiente = cargaArchivo.getMesAfectacion().intValue() + 1;
	int anioSiguiente = cargaArchivo.getAnioAfectacion().intValue();
	
	if (mesSiguiente > 12) {
		mesSiguiente = 1;
		anioSiguiente++;
	}
	
	// ✅ Buscar valores esperados del HistorialSueldo
	com.saa.model.crd.HistorialSueldo historialActivo = 
		historialSueldoDaoService.selectByEntidadYEstadoActivo(entidad.getCodigo());
	
	if (historialActivo == null) {
		System.out.println("   ⚠️ No se encontró HistorialSueldo - No se pueden crear aportes del mes siguiente");
		return;
	}
	
	double montoEsperadoJubilacion = nullSafe(historialActivo.getMontoJubilacion());
	double montoEsperadoCesantia = nullSafe(historialActivo.getMontoCesantia());
	
	double montoRestante = montoExcedente;
	
	// ✅ Crear aporte de Jubilación del mes siguiente (PARCIAL)
	if (montoEsperadoJubilacion > 0.01 && montoRestante > 0.01) {
		double montoParaJubilacion = Math.min(montoRestante, montoEsperadoJubilacion);
		crearAporteExcedenteMesSiguiente(entidad, TIPO_APORTE_JUBILACION, "Jubilación",
		                                montoEsperadoJubilacion, montoParaJubilacion,
		                                mesSiguiente, anioSiguiente, cargaArchivo, participe);
		montoRestante -= montoParaJubilacion;
	}
	
	// ✅ Crear aporte de Cesantía del mes siguiente (PARCIAL) si queda saldo
	if (montoEsperadoCesantia > 0.01 && montoRestante > 0.01) {
		double montoParaCesantia = Math.min(montoRestante, montoEsperadoCesantia);
		crearAporteExcedenteMesSiguiente(entidad, TIPO_APORTE_CESANTIA, "Cesantía",
		                                montoEsperadoCesantia, montoParaCesantia,
		                                mesSiguiente, anioSiguiente, cargaArchivo, participe);
		montoRestante -= montoParaCesantia;
	}
	
	if (montoRestante > 0.01) {
		System.out.println("   ⚠️ Aún queda excedente sin aplicar: $" + montoRestante);
	}
}

/**
 * Crea un aporte del mes siguiente con estado PARCIAL y valor pagado del excedente
 */
private void crearAporteExcedenteMesSiguiente(Entidad entidad, Long idTipoAporte, String nombreTipo,
                                             double valorEsperado, double montoPagado,
                                             int mes, int anio,
                                             CargaArchivo cargaArchivo, ParticipeXCargaArchivo participe) throws Throwable {
	
	com.saa.model.crd.TipoAporte tipoAporte = tipoAporteDaoService.selectById(idTipoAporte, "TipoAporte");
	
	Aporte aporteExcedente = new Aporte();
	aporteExcedente.setFilial(entidad.getFilial()); // ✅ Obtener filial desde la entidad
	aporteExcedente.setEntidad(entidad);
	aporteExcedente.setTipoAporte(tipoAporte);
	aporteExcedente.setValor(valorEsperado);
	aporteExcedente.setValorPagado(montoPagado); // ✅ Ya tiene pago adelantado
	aporteExcedente.setSaldo(Math.max(0, valorEsperado - montoPagado));
	aporteExcedente.setIdAsoprep(cargaArchivo.getCodigo()); // ✅ Referencia a la carga que generó el excedente
	aporteExcedente.setFechaTransaccion(java.time.LocalDateTime.now());
	aporteExcedente.setGlosa(String.format("Aporte %s - Abono anticipado mes %d/%d (excedente de mes %d/%d) - CargaArchivo: %d",
	                                       nombreTipo, mes, anio,
	                                       cargaArchivo.getMesAfectacion(),
	                                       cargaArchivo.getAnioAfectacion(),
	                                       cargaArchivo.getCodigo()));
	aporteExcedente.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL); // ✅ PARCIAL porque tiene abono pero no está completo
	aporteExcedente.setFechaRegistro(java.time.LocalDateTime.now());
	aporteExcedente.setUsuarioRegistro("SAA_AH"); // ✅ Identificador de aportes creados por el sistema
	
	aporteService.saveSingle(aporteExcedente);
	
	System.out.println("   ✅ Aporte " + nombreTipo + " del mes " + mes + "/" + anio + " creado con abono de $" + montoPagado);
	
	// ✅ Registrar el pago en PagoAporte con la fecha actual
	crearRegistroPagoAporte(aporteExcedente, montoPagado, cargaArchivo, participe);
}

}
