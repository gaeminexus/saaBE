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
    private com.saa.ejb.crd.service.NovedadParticipeCargaService novedadParticipeCargaService;
    
    private static final double TOLERANCIA = 1.0; // Tolerancia de $1 para redondeos
    
    // Códigos de producto Petro que NO se validan como préstamos
    private static final String CODIGO_PRODUCTO_APORTES = "AH";
    private static final String CODIGO_PRODUCTO_HS = "HS";
    private static final String CODIGO_PRODUCTO_PH = "PH"; // El seguro de PH viene en HS
    
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
            
            System.out.println("REGISTROS PROCESADOS: " + registrosProcesados.size());
            for(ParticipeXCargaArchivo reg : registrosProcesados) {
				if (reg.getCodigoPetro().equals(55145L)) {
					System.out.println("Registro con código Petro 55145 encontrado: " + reg.getNombre());
				}
			}
            
            // Agrupar por aporte (DetalleCargaArchivo)
            Map<String, DetalleCargaArchivo> aporteAgrupados = agruparPorAporte(registrosProcesados);
            
            // Convertir a listas para persistir
            List<DetalleCargaArchivo> detallesGenerados = new ArrayList<>(aporteAgrupados.values());
            
            // Calcular totales generales para CargaArchivo
            cargaArchivo = calcularTotalesGenerales(cargaArchivo, detallesGenerados);
            
            System.out.println("Archivo procesado: " + registrosProcesados.size() + " registros encontrados");
            System.out.println("Aportes agrupados: " + detallesGenerados.size());
        	
            // 1. PRIMERO: Almacenar registros en BD (TRANSACCIONAL)
            CargaArchivo cargaArchivoGuardado = almacenaRegistros(cargaArchivo, detallesGenerados, registrosProcesados);
        	
            System.out.println(cargaArchivoGuardado.getCodigo() + " - CargaArchivo almacenado con éxito");
            
            // 2. AL FINAL: Cargar el archivo físico (NO TRANSACCIONAL)
            // Solo se ejecuta si todas las operaciones de BD fueron exitosas
            String rutaArchivo = cargarArchivo(archivoInputStream, fileName, cargaArchivo);
            System.out.println("Archivo cargado en: " + rutaArchivo);
            
            cargaArchivoGuardado.setRutaArchivo(rutaArchivo);
            cargaArchivoGuardado = cargaArchivoService.saveSingle(cargaArchivoGuardado);
            
            System.out.println("Procesamiento completado exitosamente");
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
            for (ParticipeXCargaArchivo participe : participesXCargaArchivo) {
            	// Verificar que el participe pertenece a este detalle comparando por código de producto
            	if (participe.getDetalleCargaArchivo() != null && 
            		codigoProducto.equals(participe.getDetalleCargaArchivo().getCodigoPetroProducto())) {
					participe.setCodigo(null); // Limpiar código para que se genere uno nuevo
					participe.setDetalleCargaArchivo(detalleGuardado);
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
					
				// ==========================================
				// VALIDACIÓN FINANCIERA
				// IMPORTANTE: Solo para PRÉSTAMOS, NO para AH (aportes) ni HS
				// ==========================================
				boolean esProductoEspecial = CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(codigoProducto) ||
				                             CODIGO_PRODUCTO_HS.equalsIgnoreCase(codigoProducto);
				
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
				// ==========================================
				participe = participeXCargaArchivoService.saveSingle(participe);
				
			// ==========================================
			// VALIDACIONES DE FASE 2 (NOVEDADES 9-15) 
			// Se ejecutan DESPUÉS de guardar para no afectar la transacción
			// IMPORTANTE: Solo para PRÉSTAMOS, NO para AH (aportes) ni HS
			// ==========================================
			if (participe.getNovedadesCarga() != null && participe.getNovedadesCarga() == ASPNovedadesCargaArchivo.OK) {
				try {
					// Solo validar novedades de FASE 2 para préstamos, NO para productos especiales
					if (!esProductoEspecial) {
						validarNovedadesFase2(participe, codigoProducto, cargaArchivo);
					}
							
							// Las novedades ahora se guardan en la tabla hija NovedadParticipeCarga
							// No es necesario actualizar el campo novedadesCarga del partícipe
							
						} catch (Throwable e) {
							// Si falla la validación de FASE 2, no marcar toda la transacción para rollback
							// Solo registrar el error y continuar
							System.err.println("  ❌ Error en validaciones FASE 2 para partícipe " + participe.getCodigoPetro() + ": " + e.getMessage());
							e.printStackTrace();
							// El registro ya está guardado con estado OK
							// El error se detectará nuevamente en FASE 2
						}
					}
            	}
			}
        }
		return cargaArchivoGuardado;
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
	 * Valida las novedades de FASE 2 (novedades 9-15) durante la carga del archivo
	 * Estas validaciones se ejecutan ANTES del procesamiento para detectar problemas tempranamente
	 * 
	 * @param participe El registro del partícipe a validar
	 * @param codigoProducto Código del producto del archivo
	 * @param cargaArchivo Información del archivo de carga (mes/año)
	 */
	private void validarNovedadesFase2(ParticipeXCargaArchivo participe, String codigoProducto, CargaArchivo cargaArchivo) {
		try {
			// VALIDACIÓN 9: PRODUCTO_NO_MAPEADO
			List<com.saa.model.crd.Producto> productos = null;
			try {
				productos = productoDaoService.selectAllByCodigoPetro(codigoProducto);
			} catch (Throwable e) {
				System.err.println("Error al buscar productos: " + e.getMessage());
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
			List<com.saa.model.crd.Entidad> entidades = null;
			try {
				entidades = entidadDaoService.selectByCodigoPetro(rolPetroLong);
			} catch (Throwable e) {
				System.err.println("Error al buscar entidad: " + e.getMessage());
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
					System.err.println("Error al buscar préstamos del producto " + producto.getCodigo() + ": " + e.getMessage());
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
		// IMPORTANTE: Si el código es PH, el seguro viene en un registro separado con código HS
		// Debemos sumar PH + HS antes de comparar
		double montoArchivo = nullSafe(participe.getTotalDescontado());
		
		// Caso especial para PH: buscar el registro HS correspondiente
		if (CODIGO_PRODUCTO_PH.equalsIgnoreCase(codigoProducto)) {
			try {
				ParticipeXCargaArchivo participeHS = participeXCargaArchivoDaoService.selectByCodigoPetroYProductoEnCarga(
					participe.getCodigoPetro(),
					CODIGO_PRODUCTO_HS,
					cargaArchivo.getCodigo()
				);
				
				if (participeHS != null) {
					// Sumar el monto del seguro (HS) al monto del préstamo (PH)
					double montoHS = nullSafe(participeHS.getTotalDescontado());
					montoArchivo += montoHS;
				}
			} catch (Throwable e) {
				System.err.println("Error al buscar registro HS para PH: " + e.getMessage());
			}
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
					System.err.println("Error al buscar cuota sin pago: " + e.getMessage());
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
				System.err.println("Error al buscar cuota del mes: " + e.getMessage());
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
					System.err.println("Error al buscar todas las cuotas: " + e.getMessage());
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
					return;
				}
				
				// Si tiene fecha diferente pero diferencia es 0, registrar esa novedad
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
		return;
	}
	
	// Si alguna cuota tenía fecha diferente, marcar con CUOTA_FECHA_DIFERENTE
	if (algunaCuotaConFechaDiferente) {
		Long codigoProductoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getProducto().getCodigo() : null;
		Long codigoPrestamoDB = (!cuotasEncontradas.isEmpty()) ? cuotasEncontradas.get(0).getPrestamo().getCodigo() : null;
		registrarNovedad(participe, ASPNovedadesCargaArchivo.CUOTA_FECHA_DIFERENTE,
			"Al menos una cuota encontrada tiene fecha diferente al mes/año del archivo",
			codigoProductoDB, codigoPrestamoDB, montoEsperadoTotal, montoArchivo);
		return;
	}
		
	} catch (Exception e) {
		System.err.println("Error en validación FASE 2: " + e.getMessage());
	}
}
   
}