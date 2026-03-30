package com.saa.ejb.asoprep.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ParticipeXCargaArchivo;
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
    private com.saa.ejb.crd.service.AporteService aporteService;
    
    @EJB
    private com.saa.ejb.crd.service.PagoAporteService pagoAporteService;
    
    @EJB
    private com.saa.ejb.crd.dao.TipoAporteDaoService tipoAporteDaoService;
    
    private static final double TOLERANCIA = 1.0; // Tolerancia de $1 para redondeos
    
    // Códigos de TipoAporte
    private static final Long TIPO_APORTE_JUBILACION = 10L; // Código para aporte de jubilación
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
        
        System.out.println("Iniciando procesamiento de archivo Petro: " + fileName);
        
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
		System.out.println("========================================");
		System.out.println("INICIANDO VALIDACIONES FASE 2");
		System.out.println("Total de registros a validar: " + participesXCargaArchivo.size());
		System.out.println("========================================");
		
		int contadorValidados = 0;
		int contadorOmitidos = 0;
		
		for (ParticipeXCargaArchivo participe : participesXCargaArchivo) {
			// Obtener el código del producto
			String codigoProducto = participe.getDetalleCargaArchivo() != null ? 
									participe.getDetalleCargaArchivo().getCodigoPetroProducto() : null;
			
			if (codigoProducto == null) {
				contadorOmitidos++;
				continue;
			}
			
			// Verificar si es producto especial (AH, HS)
			boolean esProductoEspecial = CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto) ||
										 CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto);
			
			// Solo validar préstamos, NO productos especiales
			if (!esProductoEspecial && participe.getCodigo() != null) {
				try {
					validarNovedadesFase2(participe, codigoProducto, cargaArchivo);
					contadorValidados++;
				} catch (Throwable e) {
					System.err.println("ERROR en validaciones Fase 2 para partícipe " + 
									   participe.getCodigoPetro() + ": " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				contadorOmitidos++;
			}
		}
		
		System.out.println("========================================");
		System.out.println("VALIDACIONES FASE 2 COMPLETADAS");
		System.out.println("Registros validados: " + contadorValidados);
		System.out.println("Registros omitidos (AH/HS): " + contadorOmitidos);
		System.out.println("========================================");
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
			
			com.saa.model.crd.NovedadParticipeCarga novedad = new com.saa.model.crd.NovedadParticipeCarga();
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
					com.saa.model.crd.Prestamo prestamo = prestamoDaoService.selectById(codigoPrestamo, "Prestamo");
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
	 * Valida que el desglose de una cuota coincida con los valores del archivo
	 * @param cuota La cuota a validar
	 * @param participe El registro del archivo con los valores esperados
	 * @return true si el desglose coincide, false en caso contrario
	 */
	private boolean validarDesgloseCuota(com.saa.model.crd.DetallePrestamo cuota, ParticipeXCargaArchivo participe) {
		double capitalCuota = nullSafe(cuota.getCapital());
		double interesCuota = nullSafe(cuota.getInteres());
		double desgravamenCuota = nullSafe(cuota.getDesgravamen());
		
		double capitalArchivo = nullSafe(participe.getCapitalDescontado());
		double interesArchivo = nullSafe(participe.getInteresDescontado());
		double desgravamenArchivo = nullSafe(participe.getSeguroDescontado());
		
		boolean capitalCoincide = Math.abs(capitalCuota - capitalArchivo) <= TOLERANCIA;
		boolean interesCoincide = Math.abs(interesCuota - interesArchivo) <= TOLERANCIA;
		boolean desgravamenCoincide = Math.abs(desgravamenCuota - desgravamenArchivo) <= TOLERANCIA;
		
		return capitalCoincide && interesCoincide && desgravamenCoincide;
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
			
			// 2. Obtener todos los detalles de esta carga
			List<DetalleCargaArchivo> detalles = detalleCargaArchivoService.selectAll();
			List<DetalleCargaArchivo> detallesCarga = detalles.stream()
				.filter(d -> d.getCargaArchivo() != null && d.getCargaArchivo().getCodigo().equals(codigoCargaArchivo))
				.collect(java.util.stream.Collectors.toList());
			
			int totalProcesados = 0;
			int totalExitosos = 0;
			int totalErrores = 0;
			int totalOmitidos = 0;
			int totalAportesGenerados = 0;
			
			// 3. Por cada detalle, procesar los partícipes
			for (DetalleCargaArchivo detalle : detallesCarga) {
				String codigoProducto = detalle.getCodigoPetroProducto();
				
				// Omitir solo producto HS (seguros independientes)
				// AH (Aportes) SÍ se procesa
				if (CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto)) {
					System.out.println("Omitiendo producto HS (seguros independientes)");
					continue;
				}
				
				// Obtener todos los partícipes de este detalle
				List<ParticipeXCargaArchivo> participes = participeXCargaArchivoDaoService.selectAll(
					com.saa.model.crd.NombreEntidadesCredito.PARTICIPE_X_CARGA_ARCHIVO);
				
				List<ParticipeXCargaArchivo> participesDetalle = participes.stream()
					.filter(p -> p.getDetalleCargaArchivo() != null && 
					            p.getDetalleCargaArchivo().getCodigo().equals(detalle.getCodigo()))
					.collect(java.util.stream.Collectors.toList());
				
				System.out.println("Procesando producto: " + codigoProducto + " - " + participesDetalle.size() + " registros");
				
				// 4. Procesar cada partícipe
				for (ParticipeXCargaArchivo participe : participesDetalle) {
					totalProcesados++;
					
					try {
						// Verificar si tiene novedades que bloquean el procesamiento
						if (tieneNovedadesBloqueantes(participe)) {
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
	 */
	private boolean tieneNovedadesBloqueantes(ParticipeXCargaArchivo participe) {
		// Las siguientes novedades NO bloquean el pago:
		// - OK (1)
		// - DIFERENCIA_MENOR_UN_DOLAR (12)
		// - CUOTA_FECHA_DIFERENTE (13)
		// - DESCUENTOS_INCOMPLETOS (7)
		
		Long novedad = participe.getNovedadesCarga();
		if (novedad == null || novedad == ASPNovedadesCargaArchivo.OK) {
			return false;
		}
		
		// Permitir procesamiento con estas novedades específicas
		if (novedad == ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR ||
		    novedad == ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE) {
			return false;
		}
		
		// Cualquier otra novedad bloquea el procesamiento
		return true;
	}
	
	/**
	 * Aplica el pago de un partícipe individual
	 * REGLA ESPECIAL: Para PH (Préstamo Hipotecario) y PP (Préstamo Prendario), 
	 * el seguro DEBE venir en un registro separado con código HS.
	 * Si no se encuentra HS o no corresponde, la cuota queda como PARCIAL.
	 */
	private void aplicarPagoParticipe(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo) throws Throwable {
		// Obtener el monto del archivo (sumando HS si es PH o PP)
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		double montoHS = 0.0;
		boolean hsEncontradoYValido = true;
		
		// REGLA ESPECIAL: Para PH o PP, el seguro DEBE venir en HS
		if (CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
		    CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) {
			
			System.out.println("========================================");
			System.out.println("APLICAR PAGO " + codigoProducto + " - Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
			System.out.println("Monto " + codigoProducto + ": $" + participe.getTotalDescontado());
			
			try {
				ParticipeXCargaArchivo participeHS = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
					participe.getCodigoPetro(),
					CODIGO_PRODUCTO_HS,
					cargaArchivo.getCodigo()
				);
				
				if (participeHS != null) {
					montoHS = nullSafe(participeHS.getTotalDescontado());
					System.out.println("✅ Registro HS encontrado con monto: $" + montoHS);
					
					// Buscar la cuota para validar que el monto HS corresponda al desgravamen esperado
					List<com.saa.model.crd.Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
					if (entidades != null && !entidades.isEmpty()) {
						com.saa.model.crd.Entidad entidad = entidades.get(0);
						List<com.saa.model.crd.Producto> productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
						
						if (productos != null && !productos.isEmpty()) {
							List<com.saa.model.crd.Prestamo> prestamos = new ArrayList<>();
							for (com.saa.model.crd.Producto producto : productos) {
								List<com.saa.model.crd.Prestamo> prestamosDelProducto = 
									prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
								if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
									prestamos.addAll(prestamosDelProducto);
								}
							}
							
							if (!prestamos.isEmpty()) {
								com.saa.model.crd.DetallePrestamo cuotaValidar = buscarCuotaAPagar(prestamos, cargaArchivo);
								if (cuotaValidar != null) {
									double desgravamenEsperado = nullSafe(cuotaValidar.getDesgravamen());
									double diferenciaHS = Math.abs(montoHS - desgravamenEsperado);
									
									if (diferenciaHS <= TOLERANCIA) {
										System.out.println("✅ Monto HS CORRESPONDE al desgravamen esperado ($" + desgravamenEsperado + ")");
										montoArchivo += montoHS;
										System.out.println("✅ TOTAL A APLICAR (" + codigoProducto + " + HS): $" + montoArchivo);
									} else {
										hsEncontradoYValido = false;
										System.out.println("❌ Monto HS NO CORRESPONDE - Esperado: $" + desgravamenEsperado + ", Recibido: $" + montoHS);
										System.out.println("❌ La cuota quedará como PARCIAL (solo se aplicará " + codigoProducto + ")");
									}
								}
							}
						}
					}
				} else {
					hsEncontradoYValido = false;
					System.out.println("❌ ERROR: No se encontró registro HS (OBLIGATORIO para " + codigoProducto + ")");
					System.out.println("❌ La cuota quedará como PARCIAL (solo se aplicará " + codigoProducto + " sin seguro)");
				}
			} catch (Throwable e) {
				hsEncontradoYValido = false;
				System.err.println("❌ ERROR al buscar/validar HS: " + e.getMessage());
				e.printStackTrace();
				System.out.println("❌ La cuota quedará como PARCIAL (solo se aplicará " + codigoProducto + " sin seguro)");
			}
			System.out.println("========================================");
		}
		
		// Si el monto es 0, no hay pago que procesar
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
			System.out.println("⚠️ Monto a aplicar es 0 - No se procesa pago");
			return;
		}
		
		// Buscar el préstamo activo
		List<com.saa.model.crd.Entidad> entidades = entidadDaoService.selectByCodigoPetro(participe.getCodigoPetro());
		if (entidades == null || entidades.isEmpty()) {
			System.out.println("⚠️ No se encontró entidad con código Petro: " + participe.getCodigoPetro());
			return;
		}
		
		com.saa.model.crd.Entidad entidad = entidades.get(0);
		
		// Buscar productos
		List<com.saa.model.crd.Producto> productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
		if (productos == null || productos.isEmpty()) {
			System.out.println("⚠️ No se encontró producto con código Petro: " + codigoProducto);
			return;
		}
		
		// Buscar préstamos activos
		List<com.saa.model.crd.Prestamo> prestamos = new ArrayList<>();
		for (com.saa.model.crd.Producto producto : productos) {
			List<com.saa.model.crd.Prestamo> prestamosDelProducto = 
				prestamoDaoService.selectByEntidadYProductoActivosById(entidad.getCodigo(), producto.getCodigo());
			if (prestamosDelProducto != null && !prestamosDelProducto.isEmpty()) {
				prestamos.addAll(prestamosDelProducto);
			}
		}
		
		if (prestamos.isEmpty()) {
			System.out.println("⚠️ No se encontraron préstamos activos para entidad: " + entidad.getCodigo() + " y producto: " + codigoProducto);
			return;
		}
		
		// Buscar la cuota a pagar (del mes/año del archivo o la mínima pendiente)
		com.saa.model.crd.DetallePrestamo cuotaAPagar = buscarCuotaAPagar(prestamos, cargaArchivo);
		
		if (cuotaAPagar != null) {
			System.out.println("✅ Cuota a pagar encontrada: #" + cuotaAPagar.getNumeroCuota() + " - Monto: $" + nullSafe(cuotaAPagar.getTotal()));
			
			// Si es PH/PP y NO se encontró HS válido, forzar procesamiento como PARCIAL (sin desgravamen)
			if ((CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto) || 
			     CODIGO_PRODUCTO_PP.equalsIgnoreCase(codigoProducto)) && !hsEncontradoYValido) {
				System.out.println("⚠️ Aplicando pago PARCIAL (sin seguro HS) para " + codigoProducto);
				procesarPagoParcialSinSeguro(participe, cuotaAPagar, montoArchivo, cargaArchivo);
			} else {
				// Aplicar el pago usando los métodos de procesamiento normales
				procesarPagoCuota(participe, cuotaAPagar, montoArchivo, cargaArchivo);
			}
		} else {
			System.out.println("⚠️ No se encontró cuota pendiente para aplicar el pago");
		}
	}
	
	/**
	 * Procesa un pago parcial cuando NO se encontró el seguro HS para PH/PP
	 * Solo aplica Capital e Interés, dejando el Desgravamen pendiente
	 */
	private void procesarPagoParcialSinSeguro(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuota,
	                                          double montoPagado, CargaArchivo cargaArchivo) throws Throwable {
		
		System.out.println("    >>> Distribuyendo pago PARCIAL SIN SEGURO - Orden: Interés → Capital");
		
		double montoRestante = montoPagado;
		
		// Obtener valores esperados de la cuota (SIN DESGRAVAMEN)
		double interesEsperado = nullSafe(cuota.getInteres());
		double capitalEsperado = nullSafe(cuota.getCapital());
		
		// Obtener valores ya pagados previamente (acumulativos)
		double interesPagadoAnterior = nullSafe(cuota.getInteresPagado());
		double capitalPagadoAnterior = nullSafe(cuota.getCapitalPagado());
		
		// Calcular saldos pendientes
		double interesPendiente = Math.max(0, interesEsperado - interesPagadoAnterior);
		double capitalPendiente = Math.max(0, capitalEsperado - capitalPagadoAnterior);
		
		double interesPagadoAhora = 0.0;
		double capitalPagadoAhora = 0.0;
		
		// PASO 1: Pagar INTERÉS (primero, ya que no hay desgravamen)
		if (montoRestante > 0 && interesPendiente > 0) {
			if (montoRestante >= interesPendiente) {
				interesPagadoAhora = interesPendiente;
				montoRestante -= interesPendiente;
				System.out.println("      ✅ Interés COMPLETO: $" + interesPagadoAhora);
			} else {
				interesPagadoAhora = montoRestante;
				montoRestante = 0;
				System.out.println("      ⚠️ Interés PARCIAL: $" + interesPagadoAhora + " de $" + interesPendiente);
			}
		}
		
		// PASO 2: Pagar CAPITAL (segundo)
		if (montoRestante > 0 && capitalPendiente > 0) {
			if (montoRestante >= capitalPendiente) {
				capitalPagadoAhora = capitalPendiente;
				montoRestante -= capitalPendiente;
				System.out.println("      ✅ Capital COMPLETO: $" + capitalPagadoAhora);
			} else {
				capitalPagadoAhora = montoRestante;
				montoRestante = 0;
				System.out.println("      ⚠️ Capital PARCIAL: $" + capitalPagadoAhora + " de $" + capitalPendiente);
			}
		}
		
		// Actualizar valores ACUMULATIVOS en la cuota (SIN tocar desgravamen)
		cuota.setInteresPagado(interesPagadoAnterior + interesPagadoAhora);
		cuota.setCapitalPagado(capitalPagadoAnterior + capitalPagadoAhora);
		
		// Actualizar saldos pendientes
		cuota.setSaldoCapital(Math.max(0, capitalEsperado - cuota.getCapitalPagado()));
		cuota.setSaldoInteres(Math.max(0, interesEsperado - cuota.getInteresPagado()));
		
	// SIEMPRE queda PARCIAL porque falta el desgravamen
	cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
	System.out.println("      ⚠️⚠️ Cuota queda PARCIAL (falta desgravamen/seguro HS)");
	
	// TRAZABILIDAD: Guardar ID de CargaArchivo en codigoExterno
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	
	// Guardar cuota actualizada
	detallePrestamoService.saveSingle(cuota);
		
		// Crear registro de pago parcial con observación especial
		String observacion = String.format("Pago PARCIAL SIN SEGURO desde carga archivo %d. Total aplicado: $%.2f (Int:$%.2f, Cap:$%.2f). FALTA DESGRAVAMEN: $%.2f",
		                                   cargaArchivo.getCodigo(), montoPagado, 
		                                   interesPagadoAhora, capitalPagadoAhora,
		                                   nullSafe(cuota.getDesgravamen()));
		crearRegistroPago(cuota, montoPagado, capitalPagadoAhora, interesPagadoAhora, 0.0, observacion, cargaArchivo);
	}
	
	/**
	 * Busca la cuota que se debe pagar (del mes/año del archivo o la mínima pendiente)
	 */
	private com.saa.model.crd.DetallePrestamo buscarCuotaAPagar(List<com.saa.model.crd.Prestamo> prestamos, CargaArchivo cargaArchivo) throws Throwable {
		// Primero buscar cuota del mes/año del archivo
		for (com.saa.model.crd.Prestamo prestamo : prestamos) {
			List<com.saa.model.crd.DetallePrestamo> cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
				prestamo.getCodigo(),
				cargaArchivo.getMesAfectacion().intValue(),
				cargaArchivo.getAnioAfectacion().intValue()
			);
			
			if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
				for (com.saa.model.crd.DetallePrestamo cuota : cuotasDelMes) {
					Long estadoCuota = cuota.getEstado();
					if (estadoCuota != null && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
						return cuota;
					}
				}
			}
		}
		
		// Si no encuentra del mes, buscar la mínima cuota pendiente
		com.saa.model.crd.DetallePrestamo cuotaMinima = null;
		for (com.saa.model.crd.Prestamo prestamo : prestamos) {
			List<com.saa.model.crd.DetallePrestamo> todasLasCuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
			
			if (todasLasCuotas != null && !todasLasCuotas.isEmpty()) {
				for (com.saa.model.crd.DetallePrestamo cuota : todasLasCuotas) {
					Long estadoCuota = cuota.getEstado();
					if (estadoCuota != null && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
						estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
						if (cuotaMinima == null || cuota.getNumeroCuota() < cuotaMinima.getNumeroCuota()) {
							cuotaMinima = cuota;
						}
					}
				}
			}
		}
		
		return cuotaMinima;
	}
	
	/**
	 * Valida las novedades de FASE 2 (novedades 9-15) durante la carga del archivo
	 * Estas validaciones se ejecutan ANTES del procesamiento para detectar problemas tempranamente
	 * 
	 * @param participe El registro del partícipe a validar
	 * @param codigoProducto Código del producto del archivo
	 * @param cargaArchivo Información del archivo de carga (mes/año)
	 */
	private void validarNovedadesFase2(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo) {
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
		List<com.saa.model.crd.Producto> productos = null;
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
		// IMPORTANTE: Si la entidad no existe, ya se reportó en FASE 1
		// Aquí solo registramos la novedad y continuamos, NO tumbamos el proceso
		List<com.saa.model.crd.Entidad> entidades = null;
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
			
			com.saa.model.crd.Entidad entidad = entidades.get(0);
			
			// Buscar préstamos usando IDs numéricos
			List<com.saa.model.crd.Prestamo> prestamos = new ArrayList<>();
			
			for (com.saa.model.crd.Producto producto : productos) {
				try {
					List<com.saa.model.crd.Prestamo> prestamosDelProducto = 
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
		// Debemos sumar (PH|PP) + HS antes de comparar
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
		
		List<com.saa.model.crd.DetallePrestamo> cuotasEncontradas = new ArrayList<>();
		boolean algunaCuotaConFechaDiferente = false;
		boolean necesitaSumarMultiplesCuotas = false;
		
		// VALIDACIÓN ESPECIAL: Si el monto descontado es 0, significa que NO se realizó el pago
		if (montoArchivo == 0.0 || Math.abs(montoArchivo) < 0.01) {
			// Buscar si existe una cuota pendiente para este partícipe
			boolean cuotaEncontrada = false;
			for (com.saa.model.crd.Prestamo prestamo : prestamos) {
				try {
					List<com.saa.model.crd.DetallePrestamo> cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
						prestamo.getCodigo(),
						cargaArchivo.getMesAfectacion().intValue(),
						cargaArchivo.getAnioAfectacion().intValue()
					);
					
					if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
						for (com.saa.model.crd.DetallePrestamo cuota : cuotasDelMes) {
							Long estadoCuota = cuota.getEstado();
							if (estadoCuota != null && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.PAGADA && 
								estadoCuota != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
								// Encontramos una cuota pendiente pero no se realizó el pago
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
					// Continuar con el siguiente préstamo
				}
			}
			
			// Si no se encontró cuota específica, registrar novedad genérica
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
		for (com.saa.model.crd.Prestamo prestamo : prestamos) {
			com.saa.model.crd.DetallePrestamo cuotaDelPrestamo = null;
			boolean cuotaConFechaDiferente = false;
			
			// Buscar cuota del mes/año del archivo en este préstamo
			List<com.saa.model.crd.DetallePrestamo> cuotasDelMes = null;
			try {
				cuotasDelMes = detallePrestamoDaoService.selectByPrestamoYMesAnio(
					prestamo.getCodigo(),
					cargaArchivo.getMesAfectacion().intValue(),
					cargaArchivo.getAnioAfectacion().intValue()
				);
			} catch (Throwable e) {
				System.err.println("Error al buscar cuotas del mes para préstamo " + prestamo.getCodigo() + ": " + e.getMessage());
				// Continuar con el siguiente préstamo
				continue;
			}
			
			// Filtrar cuotas que NO estén PAGADAS o CANCELADAS_ANTICIPADA
			if (cuotasDelMes != null && !cuotasDelMes.isEmpty()) {
				for (com.saa.model.crd.DetallePrestamo cuotaTemp : cuotasDelMes) {
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
				List<com.saa.model.crd.DetallePrestamo> todasLasCuotas = null;
				try {
					todasLasCuotas = detallePrestamoDaoService.selectByPrestamo(prestamo.getCodigo());
				} catch (Throwable e) {
					System.err.println("Error al buscar todas las cuotas para préstamo " + prestamo.getCodigo() + ": " + e.getMessage());
					// Continuar con el siguiente préstamo
				}
				
				if (todasLasCuotas != null && !todasLasCuotas.isEmpty()) {
					com.saa.model.crd.DetallePrestamo cuotaMinima = null;
					
					for (com.saa.model.crd.DetallePrestamo cuotaTemp : todasLasCuotas) {
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
			
			// Verificar si está dentro de la tolerancia
			if (diferencia <= TOLERANCIA) {
				cuotasEncontradas.clear();
				cuotasEncontradas.add(cuotaDelPrestamo);
				
				// Si la diferencia es mayor a 0 pero menor a $1, registrar novedad especial
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
				
				// Si tiene fecha diferente, registrar esa novedad
				if (cuotaConFechaDiferente) {
					double montoCuotaEncontrada = nullSafe(cuotaDelPrestamo.getTotal());
					registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE,
						"Cuota #" + cuotaDelPrestamo.getNumeroCuota() + " encontrada con fecha diferente al archivo",
						cuotaDelPrestamo.getPrestamo().getProducto().getCodigo(), 
						cuotaDelPrestamo.getPrestamo().getCodigo(), 
						montoCuotaEncontrada, montoArchivo);
				}
				
				// VALIDACIÓN OK - El pago se procesará en la fase de procesamiento
				// NO procesamos el pago aquí, solo validamos
				return;
			}
			
			// No coincide exactamente, agregar a la lista para sumar después
			cuotasEncontradas.add(cuotaDelPrestamo);
		}
	}
			
	// PASO 2: Si no hubo coincidencia exacta y hay múltiples cuotas, SUMAR
	if (cuotasEncontradas.isEmpty()) {
		participe.setNovedadesCarga(Long.valueOf(ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA));
		return;
	}
	
	if (cuotasEncontradas.size() > 1) {
		necesitaSumarMultiplesCuotas = true;
	}
	
	// PASO 3: Validar suma de cuotas vs archivo
	double montoEsperadoTotal = 0.0;
	double capitalEsperadoTotal = 0.0;
	double interesEsperadoTotal = 0.0;
	double desgravamenEsperadoTotal = 0.0;
	
	for (com.saa.model.crd.DetallePrestamo cuota : cuotasEncontradas) {
		double capitalCuota = nullSafe(cuota.getCapital());
		double interesCuota = nullSafe(cuota.getInteres());
		double desgravamenCuota = nullSafe(cuota.getDesgravamen());
		double montoCuota = nullSafe(cuota.getTotal());
		
		capitalEsperadoTotal += capitalCuota;
		interesEsperadoTotal += interesCuota;
		desgravamenEsperadoTotal += desgravamenCuota;
		montoEsperadoTotal += montoCuota;
	}
	
	// Comparar el monto total con tolerancia
	double diferenciaTotal = Math.abs(montoEsperadoTotal - montoArchivo);
	
	if (diferenciaTotal > TOLERANCIA) {
		System.out.println("MONTO_INCONSISTENTE: Esperado=$" + montoEsperadoTotal + 
			" | Archivo=$" + montoArchivo + " | Diferencia=$" + diferenciaTotal);
		String descripcion = String.format("Monto inconsistente - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
			montoEsperadoTotal, montoArchivo, diferenciaTotal);
		Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
		Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
		registrarNovedad(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE,
			descripcion, codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
		return;
	}
	
	// Si la diferencia es mayor a 0 pero menor o igual a $1, registrar novedad especial
	if (diferenciaTotal > 0.01 && diferenciaTotal <= TOLERANCIA) {
		String descripcion = String.format("Diferencia menor a $1 - Esperado: $%.2f, Archivo: $%.2f, Diferencia: $%.2f",
			montoEsperadoTotal, montoArchivo, diferenciaTotal);
		Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
		Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
		registrarNovedad(participe, ASPNovedadesCargaArchivo.DIFERENCIA_MENOR_UN_DOLAR,
			descripcion, codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
	}
	
	// Si alguna cuota tenía fecha diferente, marcar con CUOTA_FECHA_DIFERENTE
	if (algunaCuotaConFechaDiferente) {
		Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
		Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
		registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE,
			"Al menos una cuota encontrada tiene fecha diferente al mes/año del archivo",
			codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
	}
	
	// VALIDACIÓN OK - Los pagos se procesarán en la fase de procesamiento
	// NO procesamos pagos aquí, solo validamos
	return;
		
	} catch (Exception e) {
		// Error en validación FASE 2
	}
}

/**
	 * Verifica si existe una afectación manual en la tabla AVPC para este partícipe y cuota
	 * 
	 * @param participe Partícipe del archivo
	 * @param cuota Cuota a procesar
	 * @return AfectacionValoresParticipeCarga si existe, null si no
	 */
	private com.saa.model.crd.AfectacionValoresParticipeCarga verificarAfectacionManual(
			ParticipeXCargaArchivo participe, 
			com.saa.model.crd.DetallePrestamo cuota) throws Throwable {
		
		try {
			// Buscar si existe una afectación manual para este partícipe
			// La tabla AVPC se relaciona con NovedadParticipeCarga
			// que a su vez se relaciona con ParticipeXCargaArchivo
			
			// Obtener todas las afectaciones y filtrar por partícipe y cuota
			List<com.saa.model.crd.AfectacionValoresParticipeCarga> afectaciones = 
				afectacionValoresParticipeCargaService.selectAll();
			
			if (afectaciones != null) {
				for (com.saa.model.crd.AfectacionValoresParticipeCarga afectacion : afectaciones) {
					// Verificar que la afectación corresponda a este partícipe y cuota
					if (afectacion.getNovedadParticipeCarga() != null &&
						afectacion.getNovedadParticipeCarga().getParticipeXCargaArchivo() != null &&
						afectacion.getNovedadParticipeCarga().getParticipeXCargaArchivo().getCodigo().equals(participe.getCodigo()) &&
						afectacion.getDetallePrestamo() != null &&
						afectacion.getDetallePrestamo().getCodigo().equals(cuota.getCodigo())) {
						return afectacion;
					}
				}
			}
		} catch (Throwable e) {
			System.err.println("Error al verificar afectación manual: " + e.getMessage());
			// Continuar sin afectación manual
		}
		
		return null;
	}
	
	/**
	 * Aplica una afectación manual según los valores indicados en la tabla AVPC
	 * 
	 * @param participe Partícipe del archivo
	 * @param cuota Cuota a afectar
	 * @param afectacion Afectación manual de la tabla AVPC
	 * @param montoPagado Monto total recibido
	 * @param cargaArchivo Información de la carga
	 */
	private void aplicarAfectacionManual(
			ParticipeXCargaArchivo participe,
			com.saa.model.crd.DetallePrestamo cuota,
			com.saa.model.crd.AfectacionValoresParticipeCarga afectacion,
			double montoPagado,
			CargaArchivo cargaArchivo) throws Throwable {
		
		// Obtener valores a afectar de la tabla AVPC
		double capitalAfectar = nullSafe(afectacion.getCapitalAfectar());
		double interesAfectar = nullSafe(afectacion.getInteresAfectar());
		double desgravamenAfectar = nullSafe(afectacion.getDesgravamenAfectar());
		double valorTotalAfectar = nullSafe(afectacion.getValorAfectar());
		
		System.out.println("    Aplicando afectación manual:");
		System.out.println("      Capital a afectar: $" + capitalAfectar);
		System.out.println("      Interés a afectar: $" + interesAfectar);
		System.out.println("      Desgravamen a afectar: $" + desgravamenAfectar);
		
		// Actualizar valores pagados (ACUMULATIVOS)
		double capitalPagadoAnterior = nullSafe(cuota.getCapitalPagado());
		double interesPagadoAnterior = nullSafe(cuota.getInteresPagado());
		double desgravamenPagadoAnterior = nullSafe(cuota.getDesgravamenPagado());
		
		cuota.setCapitalPagado(capitalPagadoAnterior + capitalAfectar);
		cuota.setInteresPagado(interesPagadoAnterior + interesAfectar);
		cuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenAfectar);
		
		// Calcular saldos pendientes
		double saldoCapital = nullSafe(cuota.getCapital()) - cuota.getCapitalPagado();
		double saldoInteres = nullSafe(cuota.getInteres()) - cuota.getInteresPagado();
		
		cuota.setSaldoCapital(Math.max(0, saldoCapital));
		cuota.setSaldoInteres(Math.max(0, saldoInteres));
		
		// Determinar estado: PAGADA si se cubrió todo, PARCIAL si falta
		double totalCuota = nullSafe(cuota.getTotal());
		double totalPagado = cuota.getCapitalPagado() + cuota.getInteresPagado() + 
							 nullSafe(cuota.getDesgravamenPagado());
		
	if (Math.abs(totalPagado - totalCuota) <= TOLERANCIA) {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setFechaPagado(java.time.LocalDateTime.now());
		cuota.setSaldoCapital(0.0);
		cuota.setSaldoInteres(0.0);
		System.out.println("    ✅ Cuota pasa a PAGADA (afectación manual completa)");
	} else {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		System.out.println("    ⚠️ Cuota queda PARCIAL (afectación manual parcial)");
	}
	
	// TRAZABILIDAD: Guardar ID de CargaArchivo en codigoExterno
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	
	// Guardar cuota actualizada
	detallePrestamoService.saveSingle(cuota);
		
		// Crear registro de pago con los valores afectados
		String observacion = "Pago con afectación manual (AVPC ID: " + afectacion.getCodigo() + 
							 ") - Carga archivo " + cargaArchivo.getCodigo();
		crearRegistroPago(cuota, valorTotalAfectar, capitalAfectar, interesAfectar, 
						  desgravamenAfectar, observacion, cargaArchivo);
	}
	
	/**
	 * Procesa el pago de una cuota aplicando las reglas de negocio:
	 * 1. Primero verifica si existe afectación manual (tabla AVPC)
	 * 2. Si no existe, aplica reglas automáticas con orden: Desgravamen → Interés → Capital
	 * 3. Si valor recibido = esperado → PAGADA
	 * 4. Si valor recibido < esperado → PARCIAL
	 * 5. Si valor recibido > esperado → PAGADA + excedente a siguiente cuota
	 * 
	 * @param participe Partícipe con el pago
	 * @param cuota Cuota a procesar
	 * @param montoPagado Monto recibido del archivo
	 * @param cargaArchivo Carga archivo para referencia
	 * @throws Throwable Si ocurre algún error
	 */
	private void procesarPagoCuota(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuota, 
                               double montoPagado, CargaArchivo cargaArchivo) throws Throwable {
	
	System.out.println(">>> Procesando cuota #" + cuota.getNumeroCuota() + " - Préstamo: " + cuota.getPrestamo().getCodigo());
	System.out.println("    Monto recibido: $" + montoPagado);
	System.out.println("    Monto esperado: $" + nullSafe(cuota.getTotal()));
	
	// PASO 1: Verificar si existe afectación manual
	com.saa.model.crd.AfectacionValoresParticipeCarga afectacionManual = verificarAfectacionManual(participe, cuota);
	
	if (afectacionManual != null) {
		System.out.println("    ✅ Afectación MANUAL encontrada - Aplicando valores de tabla AVPC");
		aplicarAfectacionManual(participe, cuota, afectacionManual, montoPagado, cargaArchivo);
		return;
	}
	
	System.out.println("    ⚙️ Afectación AUTOMÁTICA - Aplicando reglas de negocio");
	
	// PASO 2: Aplicar reglas automáticas
	double montoCuota = nullSafe(cuota.getTotal());
	double diferencia = montoPagado - montoCuota;
	
	// REGLA 1: Valor recibido = Esperado → PAGADA
	if (Math.abs(diferencia) <= TOLERANCIA) {
		System.out.println("    REGLA: Pago COMPLETO → Estado PAGADA");
		procesarPagoCompleto(participe, cuota, montoPagado, cargaArchivo);
	}
	// REGLA 2: Valor recibido < Esperado → PARCIAL (orden: Desgravamen→Interés→Capital)
	else if (diferencia < -TOLERANCIA) {
		System.out.println("    REGLA: Pago PARCIAL → Estado PARCIAL (orden: Desgravamen→Interés→Capital)");
		procesarPagoParcial(participe, cuota, montoPagado, cargaArchivo);
	}
	// REGLA 3: Valor recibido > Esperado → PAGADA + excedente a siguiente
	else if (diferencia > TOLERANCIA) {
		System.out.println("    REGLA: Pago EXCEDENTE → Cuota PAGADA + diferencia a siguiente ($" + diferencia + ")");
		procesarPagoCompletoConExcedente(participe, cuota, montoPagado, diferencia, cargaArchivo);
	}
}

/**
 * Procesa un pago completo de una cuota.
 * Actualiza la cuota a estado PAGADA y crea el registro de pago.
 */
private void procesarPagoCompleto(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuota,
                                  double montoPagado, CargaArchivo cargaArchivo) throws Throwable {
	
	// Calcular distribución del pago
	double capitalPagado = nullSafe(cuota.getCapital());
	double interesPagado = nullSafe(cuota.getInteres());
	double desgravamenPagado = nullSafe(cuota.getDesgravamen());
	
	// Actualizar campos de la cuota
	cuota.setCapitalPagado(capitalPagado);
	cuota.setInteresPagado(interesPagado);
	cuota.setDesgravamenPagado(desgravamenPagado);
	cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
	cuota.setFechaPagado(java.time.LocalDateTime.now());
	
	// Actualizar saldos a cero
	cuota.setSaldoCapital(0.0);
	cuota.setSaldoInteres(0.0);
	
	// TRAZABILIDAD: Guardar ID de CargaArchivo en codigoExterno
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	
	// Guardar cuota actualizada
	detallePrestamoService.saveSingle(cuota);
	
	// Crear registro de pago
	crearRegistroPago(cuota, montoPagado, capitalPagado, interesPagado, desgravamenPagado, 
	                  "Pago completo desde carga archivo " + cargaArchivo.getCodigo(), cargaArchivo);
}

/**
 * Procesa un pago parcial de una cuota.
 * Actualiza la cuota a estado PARCIAL y distribuye el pago según el ORDEN DE AFECTACIÓN:
 * 1. DESGRAVAMEN (primero)
 * 2. INTERÉS (segundo)
 * 3. CAPITAL (último)
 */
private void procesarPagoParcial(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuota,
                                 double montoPagado, CargaArchivo cargaArchivo) throws Throwable {
	
	System.out.println("    >>> Distribuyendo pago PARCIAL con ORDEN: Desgravamen → Interés → Capital");
	
	double montoRestante = montoPagado;
	
	// Obtener valores esperados de la cuota
	double desgravamenEsperado = nullSafe(cuota.getDesgravamen());
	double interesEsperado = nullSafe(cuota.getInteres());
	double capitalEsperado = nullSafe(cuota.getCapital());
	
	// Obtener valores ya pagados previamente (acumulativos)
	double desgravamenPagadoAnterior = nullSafe(cuota.getDesgravamenPagado());
	double interesPagadoAnterior = nullSafe(cuota.getInteresPagado());
	double capitalPagadoAnterior = nullSafe(cuota.getCapitalPagado());
	
	// Calcular saldos pendientes
	double desgravamenPendiente = Math.max(0, desgravamenEsperado - desgravamenPagadoAnterior);
	double interesPendiente = Math.max(0, interesEsperado - interesPagadoAnterior);
	double capitalPendiente = Math.max(0, capitalEsperado - capitalPagadoAnterior);
	
	double desgravamenPagadoAhora = 0.0;
	double interesPagadoAhora = 0.0;
	double capitalPagadoAhora = 0.0;
	
	// PASO 1: Pagar DESGRAVAMEN (primero)
	if (montoRestante > 0 && desgravamenPendiente > 0) {
		if (montoRestante >= desgravamenPendiente) {
			desgravamenPagadoAhora = desgravamenPendiente;
			montoRestante -= desgravamenPendiente;
			System.out.println("      ✅ Desgravamen COMPLETO: $" + desgravamenPagadoAhora);
		} else {
			desgravamenPagadoAhora = montoRestante;
			montoRestante = 0;
			System.out.println("      ⚠️ Desgravamen PARCIAL: $" + desgravamenPagadoAhora + " de $" + desgravamenPendiente);
		}
	}
	
	// PASO 2: Pagar INTERÉS (segundo)
	if (montoRestante > 0 && interesPendiente > 0) {
		if (montoRestante >= interesPendiente) {
			interesPagadoAhora = interesPendiente;
			montoRestante -= interesPendiente;
			System.out.println("      ✅ Interés COMPLETO: $" + interesPagadoAhora);
		} else {
			interesPagadoAhora = montoRestante;
			montoRestante = 0;
			System.out.println("      ⚠️ Interés PARCIAL: $" + interesPagadoAhora + " de $" + interesPendiente);
		}
	}
	
	// PASO 3: Pagar CAPITAL (último)
	if (montoRestante > 0 && capitalPendiente > 0) {
		if (montoRestante >= capitalPendiente) {
			capitalPagadoAhora = capitalPendiente;
			montoRestante -= capitalPendiente;
			System.out.println("      ✅ Capital COMPLETO: $" + capitalPagadoAhora);
		} else {
			capitalPagadoAhora = montoRestante;
			montoRestante = 0;
			System.out.println("      ⚠️ Capital PARCIAL: $" + capitalPagadoAhora + " de $" + capitalPendiente);
		}
	}
	
	// Actualizar valores ACUMULATIVOS en la cuota
	cuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenPagadoAhora);
	cuota.setInteresPagado(interesPagadoAnterior + interesPagadoAhora);
	cuota.setCapitalPagado(capitalPagadoAnterior + capitalPagadoAhora);
	
	// Actualizar saldos pendientes
	cuota.setSaldoCapital(Math.max(0, capitalEsperado - cuota.getCapitalPagado()));
	cuota.setSaldoInteres(Math.max(0, interesEsperado - cuota.getInteresPagado()));
	
	// Determinar estado final: PAGADA si se cubrió todo (con tolerancia), PARCIAL si falta
	double totalEsperado = desgravamenEsperado + interesEsperado + capitalEsperado;
	double totalPagado = cuota.getDesgravamenPagado() + cuota.getInteresPagado() + cuota.getCapitalPagado();
	
	if (Math.abs(totalPagado - totalEsperado) <= TOLERANCIA) {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
		cuota.setFechaPagado(java.time.LocalDateTime.now());
		cuota.setSaldoCapital(0.0);
		cuota.setSaldoInteres(0.0);
		System.out.println("      ✅✅ Cuota pasa a PAGADA (pago parcial completó el total)");
	} else {
		cuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
		System.out.println("      ⚠️⚠️ Cuota queda PARCIAL (falta: $" + (totalEsperado - totalPagado) + ")");
	}
	
	// TRAZABILIDAD: Guardar ID de CargaArchivo en codigoExterno
	cuota.setCodigoExterno(cargaArchivo.getCodigo());
	
	// Guardar cuota actualizada
	detallePrestamoService.saveSingle(cuota);
	
	// Crear registro de pago parcial con los valores pagados EN ESTA TRANSACCIÓN
	String observacion = String.format("Pago parcial desde carga archivo %d. Total aplicado: $%.2f (Desgrav:$%.2f, Int:$%.2f, Cap:$%.2f)",
	                                   cargaArchivo.getCodigo(), montoPagado, 
	                                   desgravamenPagadoAhora, interesPagadoAhora, capitalPagadoAhora);
	crearRegistroPago(cuota, montoPagado, capitalPagadoAhora, interesPagadoAhora, desgravamenPagadoAhora, observacion, cargaArchivo);
}

/**
 * Procesa un pago completo con excedente.
 * Paga la cuota completa y abona el excedente a la siguiente cuota siguiendo el ORDEN DE AFECTACIÓN:
 * 1. DESGRAVAMEN (primero)
 * 2. INTERÉS (segundo)
 * 3. CAPITAL (último)
 */
private void procesarPagoCompletoConExcedente(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuota,
                                              double montoPagado, double excedente, CargaArchivo cargaArchivo) throws Throwable {
	
	// Primero pagar la cuota completa
	procesarPagoCompleto(participe, cuota, nullSafe(cuota.getTotal()), cargaArchivo);
	
	System.out.println("    >>> Aplicando EXCEDENTE de $" + excedente + " a siguiente cuota");
	
	// Buscar la siguiente cuota pendiente del mismo préstamo
	List<com.saa.model.crd.DetallePrestamo> siguientesCuotas = detallePrestamoDaoService.selectByPrestamo(cuota.getPrestamo().getCodigo());
	
	com.saa.model.crd.DetallePrestamo siguienteCuota = null;
	for (com.saa.model.crd.DetallePrestamo c : siguientesCuotas) {
		if (c.getNumeroCuota() > cuota.getNumeroCuota() && 
		    c.getEstado() != null &&
		    c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.PAGADA &&
		    c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
			if (siguienteCuota == null || c.getNumeroCuota() < siguienteCuota.getNumeroCuota()) {
				siguienteCuota = c;
			}
		}
	}
	
	// Si hay siguiente cuota, aplicar el excedente siguiendo el orden de afectación
	if (siguienteCuota != null) {
		System.out.println("      Siguiente cuota encontrada: #" + siguienteCuota.getNumeroCuota());
		
		double montoRestante = excedente;
		
		// Obtener valores esperados de la siguiente cuota
		double desgravamenEsperado = nullSafe(siguienteCuota.getDesgravamen());
		double interesEsperado = nullSafe(siguienteCuota.getInteres());
		double capitalEsperado = nullSafe(siguienteCuota.getCapital());
		
		// Obtener valores ya pagados previamente (acumulativos)
		double desgravamenPagadoAnterior = nullSafe(siguienteCuota.getDesgravamenPagado());
		double interesPagadoAnterior = nullSafe(siguienteCuota.getInteresPagado());
		double capitalPagadoAnterior = nullSafe(siguienteCuota.getCapitalPagado());
		
		// Calcular saldos pendientes
		double desgravamenPendiente = Math.max(0, desgravamenEsperado - desgravamenPagadoAnterior);
		double interesPendiente = Math.max(0, interesEsperado - interesPagadoAnterior);
		double capitalPendiente = Math.max(0, capitalEsperado - capitalPagadoAnterior);
		
		double desgravamenPagadoAhora = 0.0;
		double interesPagadoAhora = 0.0;
		double capitalPagadoAhora = 0.0;
		
		// PASO 1: Aplicar a DESGRAVAMEN (primero)
		if (montoRestante > 0 && desgravamenPendiente > 0) {
			if (montoRestante >= desgravamenPendiente) {
				desgravamenPagadoAhora = desgravamenPendiente;
				montoRestante -= desgravamenPendiente;
				System.out.println("        ✅ Desgravamen COMPLETO: $" + desgravamenPagadoAhora);
			} else {
				desgravamenPagadoAhora = montoRestante;
				montoRestante = 0;
				System.out.println("        ⚠️ Desgravamen PARCIAL: $" + desgravamenPagadoAhora);
			}
		}
		
		// PASO 2: Aplicar a INTERÉS (segundo)
		if (montoRestante > 0 && interesPendiente > 0) {
			if (montoRestante >= interesPendiente) {
				interesPagadoAhora = interesPendiente;
				montoRestante -= interesPendiente;
				System.out.println("        ✅ Interés COMPLETO: $" + interesPagadoAhora);
			} else {
				interesPagadoAhora = montoRestante;
				montoRestante = 0;
				System.out.println("        ⚠️ Interés PARCIAL: $" + interesPagadoAhora);
			}
		}
		
		// PASO 3: Aplicar a CAPITAL (último)
		if (montoRestante > 0 && capitalPendiente > 0) {
			if (montoRestante >= capitalPendiente) {
				capitalPagadoAhora = capitalPendiente;
				montoRestante -= capitalPendiente;
				System.out.println("        ✅ Capital COMPLETO: $" + capitalPagadoAhora);
			} else {
				capitalPagadoAhora = montoRestante;
				montoRestante = 0;
				System.out.println("        ⚠️ Capital PARCIAL: $" + capitalPagadoAhora);
			}
		}
		
		// Actualizar valores ACUMULATIVOS en la siguiente cuota
		siguienteCuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenPagadoAhora);
		siguienteCuota.setInteresPagado(interesPagadoAnterior + interesPagadoAhora);
		siguienteCuota.setCapitalPagado(capitalPagadoAnterior + capitalPagadoAhora);
		
		// Actualizar saldos pendientes
		siguienteCuota.setSaldoCapital(Math.max(0, capitalEsperado - siguienteCuota.getCapitalPagado()));
		siguienteCuota.setSaldoInteres(Math.max(0, interesEsperado - siguienteCuota.getInteresPagado()));
		
		// Determinar estado: PAGADA si se cubrió todo, PARCIAL si falta
		double totalEsperado = desgravamenEsperado + interesEsperado + capitalEsperado;
		double totalPagado = siguienteCuota.getDesgravamenPagado() + siguienteCuota.getInteresPagado() + 
		                     siguienteCuota.getCapitalPagado();
		
		if (Math.abs(totalPagado - totalEsperado) <= TOLERANCIA) {
			siguienteCuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PAGADA);
			siguienteCuota.setFechaPagado(java.time.LocalDateTime.now());
			siguienteCuota.setSaldoCapital(0.0);
			siguienteCuota.setSaldoInteres(0.0);
			System.out.println("        ✅✅ Siguiente cuota pasa a PAGADA (excedente completó el total)");
		} else {
			siguienteCuota.setEstado((long) com.saa.rubros.EstadoCuotaPrestamo.PARCIAL);
			System.out.println("        ⚠️⚠️ Siguiente cuota queda PARCIAL");
		}
		
		// TRAZABILIDAD: Guardar ID de CargaArchivo en codigoExterno
		siguienteCuota.setCodigoExterno(cargaArchivo.getCodigo());
		
		// Guardar siguiente cuota actualizada
		detallePrestamoService.saveSingle(siguienteCuota);
		
		// Crear registro de pago por excedente
		String observacion = String.format("Excedente de $%.2f del mes %d/%d aplicado desde carga archivo %d (Desgrav:$%.2f, Int:$%.2f, Cap:$%.2f)",
		                                   excedente, cargaArchivo.getMesAfectacion(), 
		                                   cargaArchivo.getAnioAfectacion(), cargaArchivo.getCodigo(),
		                                   desgravamenPagadoAhora, interesPagadoAhora, capitalPagadoAhora);
		crearRegistroPago(siguienteCuota, excedente, capitalPagadoAhora, interesPagadoAhora, 
		                  desgravamenPagadoAhora, observacion, cargaArchivo);
		
		// Si todavía sobra dinero, aplicar recursivamente a la siguiente cuota
		if (montoRestante > TOLERANCIA) {
			System.out.println("        ⚠️ Aún sobra $" + montoRestante + " - Aplicando a siguiente cuota...");
			procesarExcedenteRecursivo(participe, siguienteCuota, montoRestante, cargaArchivo);
		}
	} else {
		System.out.println("      ⚠️ No se encontró siguiente cuota pendiente. Excedente no aplicado: $" + excedente);
	}
}

/**
 * Aplica un excedente de forma recursiva a las siguientes cuotas pendientes
 */
private void procesarExcedenteRecursivo(ParticipeXCargaArchivo participe, com.saa.model.crd.DetallePrestamo cuotaActual,
                                        double excedente, CargaArchivo cargaArchivo) throws Throwable {
	
	if (excedente <= TOLERANCIA) {
		return; // No hay más excedente significativo que aplicar
	}
	
	// Buscar la siguiente cuota pendiente
	List<com.saa.model.crd.DetallePrestamo> siguientesCuotas = detallePrestamoDaoService.selectByPrestamo(cuotaActual.getPrestamo().getCodigo());
	
	com.saa.model.crd.DetallePrestamo siguienteCuota = null;
	for (com.saa.model.crd.DetallePrestamo c : siguientesCuotas) {
		if (c.getNumeroCuota() > cuotaActual.getNumeroCuota() && 
		    c.getEstado() != null &&
		    c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.PAGADA &&
		    c.getEstado() != com.saa.rubros.EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
			if (siguienteCuota == null || c.getNumeroCuota() < siguienteCuota.getNumeroCuota()) {
				siguienteCuota = c;
			}
		}
	}
	
	if (siguienteCuota != null) {
		// Aplicar el excedente a la siguiente cuota como si fuera un pago normal
		procesarPagoCuota(participe, siguienteCuota, excedente, cargaArchivo);
	}
}

/**
 * Crea un registro de pago en la tabla PagoPrestamo.
 */
private void crearRegistroPago(com.saa.model.crd.DetallePrestamo cuota, double valorTotal,
                               double capitalPagado, double interesPagado, double desgravamenPagado,
                               String observacion, CargaArchivo cargaArchivo) throws Throwable {
	
	com.saa.model.crd.PagoPrestamo pago = new com.saa.model.crd.PagoPrestamo();
	
	// Referencias
	pago.setPrestamo(cuota.getPrestamo());
	pago.setDetallePrestamo(cuota);
	
	// Valores
	pago.setValor(valorTotal);
	pago.setCapitalPagado(capitalPagado);
	pago.setInteresPagado(interesPagado);
	pago.setDesgravamen(desgravamenPagado);
	pago.setNumeroCuota(cuota.getNumeroCuota());
	
	// Valores por defecto para otros campos
	pago.setMoraPagada(0.0);
	pago.setInteresVencidoPagado(0.0);
	pago.setSaldoOtros(0.0);
	pago.setValorSeguroIncendio(0.0);
	
	// Metadata
	pago.setFecha(java.time.LocalDateTime.now());
	pago.setFechaRegistro(java.time.LocalDateTime.now());
	pago.setObservacion(observacion);
	pago.setTipo("CARGA_ARCHIVO_PETRO");
	pago.setUsuarioRegistro(cargaArchivo.getUsuarioCarga() != null ? 
	                        cargaArchivo.getUsuarioCarga().getCodigo().toString() : "SISTEMA");
	pago.setEstado(1L);
	pago.setIdEstado(1L);
	
	// Guardar pago
	pagoPrestamoService.saveSingle(pago);
}

/**
 * Valida el producto AH (Aportes) en FASE 2
 * Verifica que los valores del archivo correspondan con lo esperado en HistorialSueldo
 * 
 * @param participe Partícipe del archivo con producto AH
 * @param cargaArchivo Información de la carga
 */
private void validarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo) {
	// ...existing code...
}

/**
 * Valida que la carga a procesar sea del siguiente mes al último mes procesado.
 * Solo se permite procesar cargas en orden secuencial mensual.
 * 
 * Si no hay cargas procesadas, solo permite procesar la carga del MENOR mes/año disponible.
 * 
 * @param cargaActual La carga que se intenta procesar
 * @throws RuntimeException Si la carga no es del siguiente mes esperado o no es la menor disponible
 */
private void validarOrdenProcesamiento(CargaArchivo cargaActual) throws Throwable {
	System.out.println("========================================");
	System.out.println("VALIDANDO ORDEN DE PROCESAMIENTO");
	System.out.println("Carga actual: " + cargaActual.getCodigo() + " - Mes/Año: " + 
	                   cargaActual.getMesAfectacion() + "/" + cargaActual.getAnioAfectacion());
	
	try {
		// 1. Buscar todas las cargas procesadas (estado = 3)
		List<CargaArchivo> todasLasCargas = cargaArchivoService.selectAll();
		List<CargaArchivo> cargasProcesadas = todasLasCargas.stream()
			.filter(c -> c.getEstado() != null && c.getEstado() == 3L)
			.collect(java.util.stream.Collectors.toList());
		
		// 2. Si no hay cargas procesadas, validar que sea la del MENOR mes/año
		if (cargasProcesadas == null || cargasProcesadas.isEmpty()) {
			System.out.println("  No hay cargas procesadas previamente.");
			System.out.println("  Validando que sea la carga del MENOR mes/año disponible...");
			
			// Buscar todas las cargas disponibles (sin procesar)
			List<CargaArchivo> cargasDisponibles = todasLasCargas.stream()
				.filter(c -> c.getEstado() != null && c.getEstado() != 3L)
				.filter(c -> c.getAnioAfectacion() != null && c.getMesAfectacion() != null)
				.collect(java.util.stream.Collectors.toList());
			
			if (cargasDisponibles.isEmpty()) {
				System.out.println("  ✅ No hay otras cargas disponibles. Se permite procesar.");
				System.out.println("========================================");
				return;
			}
			
			// Encontrar la carga con el menor año/mes
			CargaArchivo cargaMenor = null;
			for (CargaArchivo carga : cargasDisponibles) {
				if (cargaMenor == null) {
					cargaMenor = carga;
				} else {
					// Comparar año y mes para encontrar la menor
					if (carga.getAnioAfectacion() < cargaMenor.getAnioAfectacion() ||
					    (carga.getAnioAfectacion().equals(cargaMenor.getAnioAfectacion()) && 
					     carga.getMesAfectacion() < cargaMenor.getMesAfectacion())) {
						cargaMenor = carga;
					}
				}
			}
			
			System.out.println("  Carga con MENOR mes/año disponible: ID " + cargaMenor.getCodigo() + 
			                   " - Mes/Año: " + cargaMenor.getMesAfectacion() + "/" + cargaMenor.getAnioAfectacion());
			
			// Validar que la carga actual sea la del menor mes/año
			int mesActual = cargaActual.getMesAfectacion().intValue();
			int anioActual = cargaActual.getAnioAfectacion().intValue();
			int mesMenor = cargaMenor.getMesAfectacion().intValue();
			int anioMenor = cargaMenor.getAnioAfectacion().intValue();
			
			if (anioActual != anioMenor || mesActual != mesMenor) {
				String mensajeError = String.format(
					"❌ ERROR: No se puede procesar la carga del mes %d/%d.\n" +
					"No hay cargas procesadas previamente (estado 3).\n" +
					"Debe procesar primero la carga del MENOR mes/año disponible: %d/%d (ID: %d).\n" +
					"Las cargas deben procesarse en orden cronológico secuencial.",
					mesActual, anioActual,
					mesMenor, anioMenor, cargaMenor.getCodigo()
				);
				
				System.err.println(mensajeError);
				System.out.println("========================================");
				throw new RuntimeException(mensajeError);
			}
			
			System.out.println("  ✅ Validación OK: Es la carga del menor mes/año disponible.");
			System.out.println("========================================");
			return;
		}
		
		// 3. Encontrar la última carga procesada (mayor año/mes)
		CargaArchivo ultimaCargaProcesada = null;
		for (CargaArchivo carga : cargasProcesadas) {
			if (carga.getAnioAfectacion() == null || carga.getMesAfectacion() == null) {
				continue;
			}
			
			if (ultimaCargaProcesada == null) {
				ultimaCargaProcesada = carga;
			} else {
				// Comparar año y mes
				if (carga.getAnioAfectacion() > ultimaCargaProcesada.getAnioAfectacion() ||
				    (carga.getAnioAfectacion().equals(ultimaCargaProcesada.getAnioAfectacion()) && 
				     carga.getMesAfectacion() > ultimaCargaProcesada.getMesAfectacion())) {
					ultimaCargaProcesada = carga;
				}
			}
		}
		
		if (ultimaCargaProcesada == null) {
			System.out.println("  ✅ No se encontró última carga procesada válida. Se permite procesar.");
			System.out.println("========================================");
			return;
		}
		
		System.out.println("  Última carga procesada: ID " + ultimaCargaProcesada.getCodigo() + 
		                   " - Mes/Año: " + ultimaCargaProcesada.getMesAfectacion() + "/" + 
		                   ultimaCargaProcesada.getAnioAfectacion());
		
		// 4. Calcular el siguiente mes esperado
		int mesUltimo = ultimaCargaProcesada.getMesAfectacion().intValue();
		int anioUltimo = ultimaCargaProcesada.getAnioAfectacion().intValue();
		
		int mesSiguiente = mesUltimo + 1;
		int anioSiguiente = anioUltimo;
		
		// Si el mes es 13, pasar a enero del siguiente año
		if (mesSiguiente > 12) {
			mesSiguiente = 1;
			anioSiguiente++;
		}
		
		System.out.println("  Siguiente mes esperado: " + mesSiguiente + "/" + anioSiguiente);
		
		// 5. Validar que la carga actual sea del mes esperado
		int mesActual = cargaActual.getMesAfectacion().intValue();
		int anioActual = cargaActual.getAnioAfectacion().intValue();
		
		if (anioActual != anioSiguiente || mesActual != mesSiguiente) {
			String mensajeError = String.format(
				"❌ ERROR: No se puede procesar la carga del mes %d/%d.\n" +
				"La última carga procesada fue del mes %d/%d.\n" +
				"Solo se puede procesar la carga del siguiente mes: %d/%d.\n" +
				"Las cargas deben procesarse en orden secuencial mensual.",
				mesActual, anioActual,
				mesUltimo, anioUltimo,
				mesSiguiente, anioSiguiente
			);
			
			System.err.println(mensajeError);
			System.out.println("========================================");
			throw new RuntimeException(mensajeError);
		}
		
		System.out.println("  ✅ Validación OK: La carga es del siguiente mes esperado.");
		System.out.println("========================================");
		
	} catch (RuntimeException e) {
		// Re-lanzar excepciones de validación
		throw e;
	} catch (Throwable e) {
		System.err.println("  ⚠️ Error al validar orden de procesamiento: " + e.getMessage());
		System.out.println("  Se permitirá continuar con el procesamiento.");
		System.out.println("========================================");
		// No bloquear el procesamiento si falla la validación por error técnico
	}
}

/**
 * Aplica el producto AH (Aportes) generando registros en Aporte y PagoAporte
 * Crea 2 aportes: uno para Jubilación (TipoAporte 10) y otro para Cesantía (TipoAporte 11)
 * 
 * @param participe Partícipe del archivo con producto AH
 * @param cargaArchivo Información de la carga
 * @return Cantidad de aportes creados (0 si hubo error, 2 si fue exitoso)
 * @throws Throwable Si ocurre algún error
 */
private int aplicarAporteAH(ParticipeXCargaArchivo participe, CargaArchivo cargaArchivo) throws Throwable {
	System.out.println("========================================");
	System.out.println("APLICAR APORTE AH (PRODUCTO APORTES)");
	System.out.println("Partícipe: " + participe.getCodigoPetro() + " (" + participe.getNombre() + ")");
	System.out.println("========================================");
	
	try {
		// 1. Buscar la entidad
		Long rolPetro = participe.getCodigoPetro();
		if (rolPetro == null) {
			System.out.println("  ❌ El partícipe no tiene código Petro");
			return 0;
		}
		
		List<com.saa.model.crd.Entidad> entidades = entidadDaoService.selectByCodigoPetro(rolPetro);
		if (entidades == null || entidades.isEmpty()) {
			System.out.println("  ❌ No se encontró entidad con código Petro: " + rolPetro);
			return 0;
		}
		
		com.saa.model.crd.Entidad entidad = entidades.get(0);
		
		// 2. Buscar HistorialSueldo de la entidad
		List<com.saa.model.crd.HistorialSueldo> historiales = historialSueldoDaoService.selectByEntidad(entidad.getCodigo());
		
		if (historiales == null || historiales.isEmpty()) {
			System.out.println("  ❌ No se encontró HistorialSueldo para entidad: " + entidad.getCodigo());
			return 0;
		}
		
		if (historiales.size() > 1) {
			System.out.println("  ⚠️ Existen múltiples registros en HistorialSueldo: " + historiales.size() + " - No se puede determinar cuál usar");
			return 0;
		}
		
		com.saa.model.crd.HistorialSueldo historial = historiales.get(0);
		
		// 3. Validar que los valores no sean NULL
		Double montoJubilacion = historial.getMontoJubilacion();
		Double montoCesantia = historial.getMontoCesantia();
		
		if (montoJubilacion == null || montoCesantia == null) {
			System.out.println("  ❌ Valores NULL en HistorialSueldo - Jubilación: " + montoJubilacion + ", Cesantía: " + montoCesantia);
			return 0;
		}
		
		System.out.println("  Valores de HistorialSueldo:");
		System.out.println("    - Jubilación: $" + montoJubilacion);
		System.out.println("    - Cesantía: $" + montoCesantia);
		
		// 4. Buscar TipoAporte para Jubilación (código 10) y Cesantía (código 11)
		com.saa.model.crd.TipoAporte tipoAporteJubilacion = null;
		com.saa.model.crd.TipoAporte tipoAporteCesantia = null;
		
		try {
			tipoAporteJubilacion = tipoAporteDaoService.selectById(TIPO_APORTE_JUBILACION, 
				com.saa.model.crd.NombreEntidadesCredito.TIPO_APORTE);
			tipoAporteCesantia = tipoAporteDaoService.selectById(TIPO_APORTE_CESANTIA, 
				com.saa.model.crd.NombreEntidadesCredito.TIPO_APORTE);
		} catch (Throwable e) {
			System.err.println("  ❌ Error al buscar TipoAporte: " + e.getMessage());
			return 0;
		}
		
		if (tipoAporteJubilacion == null || tipoAporteCesantia == null) {
			System.err.println("  ❌ No se encontraron los TipoAporte (Jubilación:10, Cesantía:11)");
			return 0;
		}
		
		int aportesCreados = 0;
		
		// 5. CREAR APORTE DE JUBILACIÓN (TipoAporte 10)
		if (montoJubilacion > 0) {
			System.out.println("  Creando Aporte de JUBILACIÓN...");
			
			com.saa.model.crd.Aporte aporteJubilacion = new com.saa.model.crd.Aporte();
			aporteJubilacion.setFilial(cargaArchivo.getFilial());
			aporteJubilacion.setEntidad(entidad);
			aporteJubilacion.setTipoAporte(tipoAporteJubilacion);
			aporteJubilacion.setFechaTransaccion(LocalDateTime.now());
			aporteJubilacion.setGlosa(String.format("Aporte Jubilación mes %d/%d - Carga Petro %d", 
				cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion(), cargaArchivo.getCodigo()));
			aporteJubilacion.setValor(montoJubilacion);
			aporteJubilacion.setValorPagado(montoJubilacion);
			aporteJubilacion.setSaldo(0.0); // Pagado completo
			aporteJubilacion.setIdAsoprep(cargaArchivo.getCodigo()); // ID de CargaArchivo para rastreo
			aporteJubilacion.setFechaRegistro(LocalDateTime.now());
			aporteJubilacion.setUsuarioRegistro(cargaArchivo.getUsuarioCarga() != null ? 
				cargaArchivo.getUsuarioCarga().getCodigo().toString() : "SISTEMA");
			aporteJubilacion.setEstado(1L);
			
			// Guardar aporte de Jubilación
			aporteJubilacion = aporteService.saveSingle(aporteJubilacion);
			System.out.println("    ✅ Aporte Jubilación creado - ID: " + aporteJubilacion.getCodigo() + " - Valor: $" + montoJubilacion + " - CargaArchivo: " + cargaArchivo.getCodigo());
			aportesCreados++;
			
			// Crear PagoAporte de Jubilación
			crearPagoAporte(aporteJubilacion, montoJubilacion, cargaArchivo, "Pago Aporte Jubilación");
		}
		
		// 6. CREAR APORTE DE CESANTÍA (TipoAporte 11)
		if (montoCesantia > 0) {
			System.out.println("  Creando Aporte de CESANTÍA...");
			
			com.saa.model.crd.Aporte aporteCesantia = new com.saa.model.crd.Aporte();
			aporteCesantia.setFilial(cargaArchivo.getFilial());
			aporteCesantia.setEntidad(entidad);
			aporteCesantia.setTipoAporte(tipoAporteCesantia);
			aporteCesantia.setFechaTransaccion(LocalDateTime.now());
			aporteCesantia.setGlosa(String.format("Aporte Cesantía mes %d/%d - Carga Petro %d", 
				cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion(), cargaArchivo.getCodigo()));
			aporteCesantia.setValor(montoCesantia);
			aporteCesantia.setValorPagado(montoCesantia);
			aporteCesantia.setSaldo(0.0); // Pagado completo
			aporteCesantia.setIdAsoprep(cargaArchivo.getCodigo()); // ID de CargaArchivo para rastreo
			aporteCesantia.setFechaRegistro(LocalDateTime.now());
			aporteCesantia.setUsuarioRegistro(cargaArchivo.getUsuarioCarga() != null ? 
				cargaArchivo.getUsuarioCarga().getCodigo().toString() : "SISTEMA");
			aporteCesantia.setEstado(1L);
			
			// Guardar aporte de Cesantía
			aporteCesantia = aporteService.saveSingle(aporteCesantia);
			System.out.println("    ✅ Aporte Cesantía creado - ID: " + aporteCesantia.getCodigo() + " - Valor: $" + montoCesantia + " - CargaArchivo: " + cargaArchivo.getCodigo());
			aportesCreados++;
			
			// Crear PagoAporte de Cesantía
			crearPagoAporte(aporteCesantia, montoCesantia, cargaArchivo, "Pago Aporte Cesantía");
		}
		
		System.out.println("  ✅✅ Total aportes creados: " + aportesCreados);
		System.out.println("========================================");
		
		return aportesCreados;
		
	} catch (Throwable e) {
		System.err.println("❌ ERROR al aplicar aporte AH: " + e.getMessage());
		e.printStackTrace();
		throw e;
	}
}

/**
 * Crea un registro de PagoAporte para un aporte específico
 * 
 * @param aporte Aporte al que se asocia el pago
 * @param valor Valor del pago
 * @param cargaArchivo Información de la carga
 * @param concepto Concepto del pago
 * @throws Throwable Si ocurre algún error
 */
private void crearPagoAporte(com.saa.model.crd.Aporte aporte, Double valor, 
                              CargaArchivo cargaArchivo, String concepto) throws Throwable {
	
	com.saa.model.crd.PagoAporte pagoAporte = new com.saa.model.crd.PagoAporte();
	
	// Referencias
	pagoAporte.setFilial(cargaArchivo.getFilial());
	pagoAporte.setAporte(aporte);
	
	// Valores
	pagoAporte.setValor(valor);
	pagoAporte.setFechaContable(LocalDateTime.now());
	pagoAporte.setConcepto(String.format("%s - Mes %d/%d - Carga Petro %d", 
		concepto, cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion(), cargaArchivo.getCodigo()));
	
	// Metadata
	pagoAporte.setFechaRegistro(LocalDateTime.now());
	pagoAporte.setUsuarioRegistro(cargaArchivo.getUsuarioCarga() != null ? 
		cargaArchivo.getUsuarioCarga().getCodigo().toString() : "SISTEMA");
	pagoAporte.setEstado(1L);
	
	// Guardar pago
	pagoAporte = pagoAporteService.saveSingle(pagoAporte);
	System.out.println("      ✅ PagoAporte creado - ID: " + pagoAporte.getCodigo() + " - Valor: $" + valor);
}
   
}
