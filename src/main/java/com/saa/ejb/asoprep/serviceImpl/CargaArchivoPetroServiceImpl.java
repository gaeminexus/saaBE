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
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.ejb.credito.service.CargaArchivoService;
import com.saa.ejb.credito.service.DetalleCargaArchivoService;
import com.saa.ejb.credito.service.ParticipeXCargaArchivoService;
import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.DetalleCargaArchivo;
import com.saa.model.credito.Entidad;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.ParticipeXCargaArchivo;
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
    private EntidadDaoService entidadDaoService;
    
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
        
        System.out.println("Cargando archivo en ruta: " + uploadPath.toString());
        
        return fileService.uploadFileToPath(archivoInputStream, fileName, uploadPath.toString());
    }

    /**
     * Almacena el registro de CargaArchivo
     */
    private CargaArchivo almacenarCargaArchivo(CargaArchivo cargaArchivo) throws Throwable {
        System.out.println("Guardando CargaArchivo: " + cargaArchivo.getNombre());
        
        System.out.println("Zona horaria por defecto: " + ZoneId.systemDefault());
        System.out.println("Fecha/Hora sistema: " + LocalDateTime.now());
        System.out.println("Fecha/Hora con zona: " + ZonedDateTime.now());
        System.out.println("Offset UTC: " + ZonedDateTime.now().getOffset());
        cargaArchivo.setFechaCarga(LocalDateTime.now());
        
        // Validar que los campos obligatorios vengan del frontend
        if (cargaArchivo.getFilial() == null) {
            throw new RuntimeException("El campo 'filial' es obligatorio y debe ser enviado desde el frontend");
        }
        
        if (cargaArchivo.getUsuarioCarga() == null) {
            throw new RuntimeException("El campo 'usuarioCarga' es obligatorio y debe ser enviado desde el frontend");
        }
        
        // Log de los datos recibidos
        System.out.println("Filial recibida: " + cargaArchivo.getFilial().getCodigo() + " - " + cargaArchivo.getFilial().getNombre());
        System.out.println("Usuario recibido: " + cargaArchivo.getUsuarioCarga().getCodigo());
        
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
					// validacion financiera
					if (participe.getCapitalNoDescontado() > 0 || participe.getInteresNoDescontado() > 0 || 
						participe.getDesgravamenNoDescontado() > 0) {
						if (participe.getTotalDescontado() == 0) {
							participe.setNovedadesFinancieras(Long.valueOf(ASPNovedadesCargaArchivo.SIN_DESCUENTOS));
						} else {
							participe.setNovedadesFinancieras(Long.valueOf(ASPNovedadesCargaArchivo.DESCUENTOS_INCOMPLETOS));
						}
					} else {
						
					}
					participe = participeXCargaArchivoService.saveSingle(participe);
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
   
}