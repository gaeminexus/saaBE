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
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
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

    /**
     * Solo para distinguir, en {@link #distribuirAportePorDevengo}, "no tiene contrato" (aborta
     * la carga) de "tiene contrato pero la vigencia no cubre el mes" (no aborta) — nunca lo
     * infiere de que {@code esperadoMensual} dio 0, que no distingue las dos causas.
     */
    @EJB
    private com.saa.ejb.crd.dao.ContratoDaoService contratoDaoService;

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
						boolean yaExisteNvpc = false;
						List<NovedadParticipeCarga> novedadesExistentes =
							novedadParticipeCargaDaoService.selectByParticipe(participe.getCodigo());
						if (novedadesExistentes != null) {
							for (NovedadParticipeCarga existente : novedadesExistentes) {
								if (existente.getTipoNovedad() != null
										&& existente.getTipoNovedad().longValue() == tipoCarga) {
									yaExisteNvpc = true;
									break;
								}
							}
						}
						if (!yaExisteNvpc) {
							double montoRecibido = nullSafe(participe.getTotalDescontado());
							registrarNovedad(participe, (int) tipoCarga,
								"Novedad de identificación de fase 1 (validación de carga): "
									+ describirNovedades(Arrays.asList(participe.getNovedadesCarga())) + ".",
								null, null, 0.0, montoRecibido);
						}
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

		// Reset AL ENTRAR, no al salir — ver el javadoc de los campos.
		advertenciasVigenciaCargaActual.clear();
		novedadesGeneradasCargaActual.clear();

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
			boolean cargaTieneAportes = detallesCarga != null && detallesCarga.stream()
				.anyMatch(d -> CODIGO_PRODUCTO_APORTES.equalsIgnoreCase(d.getCodigoPetroProducto()));
			if (cargaTieneAportes && cargaArchivo.getFilial() != null
					&& cargaArchivo.getAnioAfectacion() != null && cargaArchivo.getMesAfectacion() != null) {
				java.time.LocalDate mesCargaAportes = java.time.LocalDate.of(
					cargaArchivo.getAnioAfectacion().intValue(), cargaArchivo.getMesAfectacion().intValue(), 1);
				esperadoEnLoteAportes = vigenciaContratoService.esperadoEnLotePorFilial(
					cargaArchivo.getFilial().getCodigo(), ALCANCE_MINIMO_DEVENGO, mesCargaAportes);
			}

			int totalProcesados = 0;
			int totalExitosos = 0;
			int totalErrores = 0;
			int totalOmitidos = 0;
			int totalAportesGenerados = 0;
			int totalAdvertencias = 0;
			
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
								// PRODUCTO AH: Generar Aportes. aportesCreados == 0 no es
								// necesariamente un no-evento: puede ser dinero recibido sin
								// aplicar (advertencia de vigencia, o novedad de HistorialSueldo)
								// que NO abortó la carga a propósito — contarlo aparte de
								// totalExitosos para que no quede invisible en el resumen.
								int advertenciasAntes = advertenciasVigenciaCargaActual.size()
									+ novedadesGeneradasCargaActual.size();
								int aportesCreados = aplicarAporteAH(participe, cargaArchivo, esperadoEnLoteAportes);
								totalAportesGenerados += aportesCreados;
								if (aportesCreados > 0) {
									totalExitosos++;
								} else if (advertenciasVigenciaCargaActual.size()
										+ novedadesGeneradasCargaActual.size() > advertenciasAntes) {
									totalAdvertencias++;
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
				}
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
			String resumen = resumenBuilder.toString();
			
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
		
		// ✅ OPTIMIZACIÓN: Buscar productos UNA SOLA VEZ (memoizado por carga, ver
		// resolverProductosPorCodigo)
		List<Producto> productos = resolverProductosPorCodigo(codigoProducto, productosCache);
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
	       aplicarPagoNormalConMotor(cuotaAPagar.getPrestamo(), montoArchivo, cargaArchivo, participe);
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
	if (prestamo == null || prestamo.getCodigo() == null) {
		System.out.println("⚠️ No se puede aplicar el pago: la cuota encontrada no tiene préstamo asociado");
		return;
	}
	ContextoPago ctx = crearContextoPagoCarga(cargaArchivo);
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
 */
private ContextoPago crearContextoPagoCarga(CargaArchivo cargaArchivo) throws Throwable {
	if (cargaArchivo.getMesAfectacion() == null || cargaArchivo.getAnioAfectacion() == null) {
		throw new IncomeException("No se puede fechar el pago: falta el mes o año de afectación de la carga "
			+ cargaArchivo.getCodigo() + ".");
	}
	java.time.LocalDateTime fechaPagoEfecto = fechaService.ultimoDiaMesAnioLocal(
		cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()).atTime(23, 59, 59);

	ContextoPago ctx = new ContextoPago();
	ctx.setIdCargaArchivo(cargaArchivo.getCodigo());
	ctx.setFechaPago(fechaPagoEfecto);
	ctx.setObservacion(String.format("Carga Petro %d - Mes %d/%d",
		cargaArchivo.getCodigo(), cargaArchivo.getMesAfectacion(), cargaArchivo.getAnioAfectacion()));
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
	String descripcion = String.format(
		"Excedente sin aplicar tras el pago en cascada del préstamo %d: recibido $%.2f, aplicado $%.2f, "
		+ "sobran $%.2f sin cuota pendiente donde aplicarlos - requiere distribución manual (AVPC)",
		prestamo.getCodigo(), resultado.getValorRecibido(), resultado.getValorAplicado(), excedente);
	registrarNovedad(participe, ASPNovedadesCargaArchivo.MONTO_INCONSISTENTE, descripcion,
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

	ContextoPago ctx = crearContextoPagoCarga(cargaArchivo);
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
		// Obtener pagos específicos de esta cuota usando método específico del DAO
		List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectByIdDetallePrestamo(cuota.getCodigo());
		
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
		Map<String, Double> esperadoEnLote) throws Throwable {
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
	if (entidades == null || entidades.isEmpty()) {
		throw new IncomeException("No se encontró la entidad del partícipe con código Petro "
			+ participe.getCodigoPetro() + " (nombre en el archivo: \"" + participe.getNombre()
			+ "\"), carga " + cargaArchivo.getCodigo() + ", monto descontado $" + montoDescontadoSinEntidad
			+ ": cree la entidad con ese código Petro y vuelva a procesar la carga.");
	}

	Entidad entidad = entidades.get(0);
	double montoRecibido = montoDescontadoSinEntidad;

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
		// Se sabe quién es (hay Entidad), falta decidir la cuenta: NOVEDAD, no aborta —
		// mismo mecanismo que el excedente Petro a un aporte (aplicarAfectacionAAporte):
		// el operador decide en pantalla si el valor recibido va a jubilación o a cesantía,
		// y esa decisión se aplica vía AVPC (tipoAporte) en el reproceso. Este código de
		// novedad YA está en FamiliaNovedadCarga.TIPOS_QUE_EXIGEN_AFECTACION — solo faltaba que esta
		// ruta (Fase 2, con dinero real) la generara además de la validación de carga
		// (validarAporteAH, Fase 1) que ya la registra sin datos de monto.
		registrarNovedad(participe, ASPNovedadesCargaArchivo.HISTORIAL_SUELDO_NO_ENCONTRADO,
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
		registrarNovedad(participe, ASPNovedadesCargaArchivo.VALORES_HISTORIAL_NULOS,
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
		aportesCreados = distribuirAportePorDevengo(entidad, montoRecibido, cargaArchivo, participe, esperadoEnLote);
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
        Map<String, Double> esperadoEnLote) throws Throwable {

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
			double esperado = esperadoMensual(entidad, idTipo, mes, esperadoEnLote);
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
			double esperado = esperadoMensual(entidad, idTipo, mes, esperadoEnLote);
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
		if (contratoActivo == null) {
			throw new IncomeException("No se pudo aplicar $" + disponible + " de los $" + montoRecibido
				+ " recibidos del partícipe " + participe.getCodigoPetro() + " (" + participe.getNombre()
				+ "), entidad " + entidad.getCodigo() + ", carga " + cargaArchivo.getCodigo()
				+ ": no tiene contrato ACTIVO. Créelo antes de procesar la carga.");
		}
		System.err.println("⚠️ ADVERTENCIA: distribuirAportePorDevengo alcanzó el tope de "
			+ TOPE_MESES_DEVENGO + " meses sin poder aplicar todo el monto. Entidad " + entidad.getCodigo()
			+ " (" + participe.getCodigoPetro() + " - " + participe.getNombre() + ") - Carga "
			+ cargaArchivo.getCodigo() + " - Disponible sin aplicar: $" + disponible
			+ " - Tiene contrato activo (código " + contratoActivo.getCodigo()
			+ ") pero su vigencia no cubre ese mes (sin vigencia vigente, o vigencia con monto $0) "
			+ "en uno o más de los " + TOPE_MESES_DEVENGO + " meses evaluados.");
		advertenciasVigenciaCargaActual.add("Partícipe " + participe.getCodigoPetro() + " ("
			+ participe.getNombre() + "): $" + disponible + " sin aplicar — contrato activo (código "
			+ contratoActivo.getCodigo() + ") pero su vigencia no cubre ese mes.");
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
 * un cambio real en la cuenta del socio, no solo de rendimiento. Si la clave no está,
 * se cae a {@link com.saa.ejb.crd.service.VigenciaContratoService#esperadoPorEntidad},
 * exactamente como se resolvía antes de esta optimización.</p>
 */
private double esperadoMensual(Entidad entidad, Long idTipoAporte, java.time.LocalDate mes,
		Map<String, Double> esperadoEnLote) throws Throwable {
	if (esperadoEnLote != null) {
		String clave = entidad.getCodigo() + "|" + idTipoAporte + "|" + mes;
		Double valorEnLote = esperadoEnLote.get(clave);
		if (valorEnLote != null) {
			return valorEnLote;
		}
	}
	return vigenciaContratoService.esperadoPorEntidad(entidad.getCodigo(), idTipoAporte, mes);
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
