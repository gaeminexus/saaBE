package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.ProcesamientoCargaArchivoDaoService;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.CargaArchivoService;
import com.saa.ejb.crd.service.ParticipeXCargaArchivoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProcesoCargaPetroService;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.ParticipeXCargaArchivo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.ProcesamientoCargaArchivo;
import com.saa.model.crd.Producto;
import com.saa.rubros.ASPNovedadesCargaArchivo;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del servicio de procesamiento FASE 2
 * Procesa archivos de Petrocomercial validados y cruza con préstamos/aportes
 */
@Stateless
public class ProcesoCargaPetroServiceImpl implements ProcesoCargaPetroService {

    @EJB
    private CargaArchivoService cargaArchivoService;
    
    @EJB
    private ParticipeXCargaArchivoService participeXCargaArchivoService;
    
    @EJB
    private ProductoDaoService productoDaoService;
    
    @EJB
    private PrestamoDaoService prestamoDaoService;
    
    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;
    
    @EJB
    private PagoPrestamoService pagoPrestamoService;
    
    @EJB
    private PrestamoService prestamoService;
    
    @EJB
    private ProcesamientoCargaArchivoDaoService procesamientoDaoService;
    
    private static final double TOLERANCIA = 1.0; // Tolerancia de $1 para redondeos

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CargaArchivo procesarCargaPetro(Long idCargaArchivo) throws Throwable {
        System.out.println("========================================");
        System.out.println("INICIANDO PROCESAMIENTO FASE 2");
        System.out.println("ID Carga Archivo: " + idCargaArchivo);
        System.out.println("========================================");
        
        // 1. Obtener el archivo de carga
        CargaArchivo cargaArchivo = cargaArchivoService.selectById(idCargaArchivo);
        if (cargaArchivo == null) {
            throw new IncomeException("No se encontró la carga con ID: " + idCargaArchivo);
        }
        
        System.out.println("Archivo: " + cargaArchivo.getNombre());
        System.out.println("Mes/Año: " + cargaArchivo.getMesAfectacion() + "/" + cargaArchivo.getAnioAfectacion());
        
        // 2. Obtener todos los registros del archivo
        List<DatosBusqueda> criterios = new ArrayList<>();
        DatosBusqueda criterio = new DatosBusqueda();
        criterio.setCampo("detalleCargaArchivo.cargaArchivo.codigo");
        criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
        criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
        criterio.setValor(idCargaArchivo.toString());
        criterios.add(criterio);
        
        List<ParticipeXCargaArchivo> todosLosRegistros = 
            participeXCargaArchivoService.selectByCriteria(criterios);
        
        System.out.println("Total registros en el archivo: " + todosLosRegistros.size());
        
        // 3. Filtrar solo registros con novedadesCarga = OK
        List<ParticipeXCargaArchivo> registrosOK = new ArrayList<>();
        for (ParticipeXCargaArchivo registro : todosLosRegistros) {
            if (registro.getNovedadesCarga() != null && 
                registro.getNovedadesCarga() == ASPNovedadesCargaArchivo.OK) {
                registrosOK.add(registro);
            }
        }
        
        System.out.println("Registros OK para procesar: " + registrosOK.size());
        
        // 4. Procesar cada registro
        int procesadosOK = 0;
        int conErrores = 0;
        int aportes = 0;
        int pagos = 0;
        
        for (ParticipeXCargaArchivo registro : registrosOK) {
            try {
                // Verificar si ya fue procesado
                if (procesamientoDaoService.yaFueProcesado(registro.getCodigo())) {
                    System.out.println("Registro ya procesado anteriormente, saltando...");
                    continue;
                }
                
                ProcesamientoCargaArchivo procesamiento = procesarRegistro(registro, cargaArchivo);
                
                if (procesamiento.getProcesado() == 1) {
                    procesadosOK++;
                    if (procesamiento.getIdAporteGenerado() != null) {
                        aportes++;
                    }
                    if (procesamiento.getIdPagoGenerado() != null) {
                        pagos++;
                    }
                } else {
                    conErrores++;
                }
                
            } catch (Exception e) {
                System.err.println("Error al procesar registro " + registro.getCodigo() + ": " + e.getMessage());
                conErrores++;
                
                // Registrar el error
                ProcesamientoCargaArchivo procesamientoError = new ProcesamientoCargaArchivo();
                procesamientoError.setParticipeXCargaArchivo(registro);
                procesamientoError.setFechaProcesamiento(LocalDateTime.now());
                procesamientoError.setProcesado(2); // Error
                procesamientoError.setError(e.getMessage());
                procesamientoError.setEstado(Estado.ACTIVO);
                procesamientoDaoService.save(procesamientoError, null);
            }
        }
        
        System.out.println("========================================");
        System.out.println("PROCESAMIENTO COMPLETADO");
        System.out.println("Total procesados OK: " + procesadosOK);
        System.out.println("Total con errores: " + conErrores);
        System.out.println("Aportes generados: " + aportes);
        System.out.println("Pagos aplicados: " + pagos);
        System.out.println("========================================");
        
        return cargaArchivo;
    }
    
    /**
     * Procesa un registro individual del archivo
     */
    private ProcesamientoCargaArchivo procesarRegistro(ParticipeXCargaArchivo registro, CargaArchivo cargaArchivo) throws Throwable {
        System.out.println("\n--- Procesando registro ---");
        System.out.println("Código Petro: " + registro.getCodigoPetro());
        System.out.println("Nombre: " + registro.getNombre());
        System.out.println("Producto: " + registro.getDetalleCargaArchivo().getCodigoPetroProducto());
        
        ProcesamientoCargaArchivo procesamiento = new ProcesamientoCargaArchivo();
        procesamiento.setParticipeXCargaArchivo(registro);
        procesamiento.setFechaProcesamiento(LocalDateTime.now());
        procesamiento.setUsuarioRegistro("SISTEMA");
        procesamiento.setEstado(Estado.ACTIVO);
        
        try {
            // 1. Buscar el producto por código Petro
            String codigoPetroProducto = registro.getDetalleCargaArchivo().getCodigoPetroProducto();
            Producto producto = productoDaoService.selectByCodigoPetro(codigoPetroProducto);
            
            if (producto == null) {
                procesamiento.setProcesado(2);
                procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.PRODUCTO_NO_MAPEADO);
                procesamiento.setError("No se encontró producto con código Petro: " + codigoPetroProducto);
                procesamientoDaoService.save(procesamiento, null);
                return procesamiento;
            }
            
            System.out.println("Producto encontrado: " + producto.getNombre());
            
            // 2. Determinar si es aporte o préstamo
            // TODO: Aquí necesitas definir cómo distinguir si un producto es APORTE o PRÉSTAMO
            // Por ahora asumimos que todos son PRÉSTAMOS
            // Si producto.getTipoPrestamo().getNombre().contains("APORTE") entonces es aporte
            
            boolean esAporte = false; // TODO: Implementar lógica
            
            if (esAporte) {
                // TODO: Implementar generación de aportes
                procesarAporte(registro, producto, procesamiento);
            } else {
                // Es un préstamo
                procesarPrestamo(registro, producto, procesamiento, cargaArchivo);
            }
            
            // Guardar el procesamiento
            procesamientoDaoService.save(procesamiento, null);
            return procesamiento;
            
        } catch (Exception e) {
            procesamiento.setProcesado(2);
            procesamiento.setError("Error: " + e.getMessage());
            procesamientoDaoService.save(procesamiento, null);
            throw e;
        }
    }
    
    /**
     * Procesa un registro de APORTE
     */
    private void procesarAporte(ParticipeXCargaArchivo registro, Producto producto, 
                                ProcesamientoCargaArchivo procesamiento) throws Throwable {
        // TODO: Implementar generación de aportes en siguiente iteración
        System.out.println("TODO: Implementar generación de aportes");
        procesamiento.setProcesado(1);
        procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.APORTE_GENERADO_OK);
        procesamiento.setObservaciones("Funcionalidad de aportes pendiente de implementación");
    }
    
    /**
     * Procesa un registro de PRÉSTAMO
     */
    private void procesarPrestamo(ParticipeXCargaArchivo registro, Producto producto,
                                 ProcesamientoCargaArchivo procesamiento, CargaArchivo cargaArchivo) throws Throwable {
        
        // 1. Buscar el préstamo activo
        // Convertir codigoPetro a String (el método espera String, no Long)
        String rolPetroStr = registro.getCodigoPetro() != null ? 
            registro.getCodigoPetro().toString() : null;
        
        if (rolPetroStr == null) {
            procesamiento.setProcesado(2);
            procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.PARTICIPE_NO_ENCONTRADO);
            procesamiento.setError("El partícipe no tiene código Petro válido");
            System.out.println("ERROR: Partícipe sin código Petro");
            return;
        }
        
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidadYProductoActivos(
            rolPetroStr,
            producto.getCodigoPetro()
        );
        
        if (prestamos == null || prestamos.isEmpty()) {
            procesamiento.setProcesado(2);
            procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.PRESTAMO_NO_ENCONTRADO);
            procesamiento.setError("No se encontró préstamo activo");
            System.out.println("ERROR: No se encontró préstamo activo");
            return;
        }
        
        if (prestamos.size() > 1) {
            procesamiento.setProcesado(2);
            procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.MULTIPLES_PRESTAMOS_ACTIVOS);
            procesamiento.setError("Existen " + prestamos.size() + " préstamos activos");
            System.out.println("ERROR: Múltiples préstamos activos: " + prestamos.size());
            return;
        }
        
        Prestamo prestamo = prestamos.get(0);
        System.out.println("Préstamo encontrado: " + prestamo.getCodigo());
        procesamiento.setIdPrestamoProcessado(prestamo.getCodigo());
        
        // 2. Buscar la cuota del mes
        List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamoYMesAnio(
            prestamo.getCodigo(),
            cargaArchivo.getMesAfectacion().intValue(),
            cargaArchivo.getAnioAfectacion().intValue()
        );
        
        if (cuotas == null || cuotas.isEmpty()) {
            procesamiento.setProcesado(2);
            procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.CUOTA_NO_ENCONTRADA);
            procesamiento.setError("No se encontró cuota para el mes/año especificado");
            System.out.println("ERROR: No se encontró cuota");
            return;
        }
        
        DetallePrestamo cuota = cuotas.get(0); // Tomar la primera si hay múltiples
        System.out.println("Cuota encontrada: #" + cuota.getNumeroCuota());
        procesamiento.setIdCuotaProcesada(cuota.getCodigo());
        
        // 3. Analizar el pago
        double montoDescontar = nullSafe(registro.getMontoDescontar());
        double totalDescontado = nullSafe(registro.getTotalDescontado());
        double capitalDescontado = nullSafe(registro.getCapitalDescontado());
        double interesDescontado = nullSafe(registro.getInteresDescontado());
        // double seguroDescontado = nullSafe(registro.getSeguroDescontado());
        
        System.out.println("Monto a descontar: $" + montoDescontar);
        System.out.println("Total descontado: $" + totalDescontado);
        
        // 4. Determinar el estado de la cuota
        int nuevoEstado;
        String observacion;
        
        if (totalDescontado >= (montoDescontar - TOLERANCIA)) {
            // PAGO COMPLETO
            nuevoEstado = EstadoCuotaPrestamo.PAGADA;
            observacion = "Pago completo";
            System.out.println("Determinación: PAGO COMPLETO");
        } else if (totalDescontado > 0) {
            // PAGO PARCIAL
            nuevoEstado = EstadoCuotaPrestamo.PARCIAL;
            observacion = "Pago parcial: $" + totalDescontado + " de $" + montoDescontar;
            System.out.println("Determinación: PAGO PARCIAL");
        } else {
            // SIN PAGO
            LocalDateTime hoy = LocalDateTime.now();
            if (cuota.getFechaVencimiento().isAfter(hoy)) {
                nuevoEstado = EstadoCuotaPrestamo.ACTIVA;
                observacion = "Sin pago, cuota aún vigente";
            } else {
                nuevoEstado = EstadoCuotaPrestamo.EN_MORA;
                observacion = "Sin pago, cuota vencida";
            }
            System.out.println("Determinación: SIN PAGO (" + observacion + ")");
        }
        
        procesamiento.setEstadoCuotaDeterminado(nuevoEstado);
        procesamiento.setObservaciones(observacion);
        
        // 5. Crear el registro de pago si hubo descuento
        if (totalDescontado > 0) {
            PagoPrestamo pago = crearPagoPrestamo(registro, prestamo, cuota, cargaArchivo);
            pago = pagoPrestamoService.saveSingle(pago);
            procesamiento.setIdPagoGenerado(pago.getCodigo());
            System.out.println("Pago creado: ID " + pago.getCodigo());
        }
        
        // 6. Actualizar la cuota con valores pagados
        // IMPORTANTE: Los valores pagados son ACUMULATIVOS (si hay pagos anteriores, se suman)
        double capitalPagadoAnterior = nullSafe(cuota.getCapitalPagado());
        double interesPagadoAnterior = nullSafe(cuota.getInteresPagado());
        double desgravamenPagadoAnterior = nullSafe(cuota.getDesgravamenPagado());
        
        // El archivo Petrocomercial envía el desgravamen en el campo seguroDescontado
        double desgravamenDescontado = nullSafe(registro.getSeguroDescontado());
        
        System.out.println("Actualizando cuota #" + cuota.getNumeroCuota() + ":");
        System.out.println("  Capital pagado anterior: $" + capitalPagadoAnterior + " + nuevo: $" + capitalDescontado);
        System.out.println("  Interés pagado anterior: $" + interesPagadoAnterior + " + nuevo: $" + interesDescontado);
        System.out.println("  Desgravamen pagado anterior: $" + desgravamenPagadoAnterior + " + nuevo: $" + desgravamenDescontado);
        
        // Actualizar estado
        cuota.setEstado(Long.valueOf(nuevoEstado));
        
        // Actualizar valores pagados (ACUMULATIVO)
        cuota.setCapitalPagado(capitalPagadoAnterior + capitalDescontado);
        cuota.setInteresPagado(interesPagadoAnterior + interesDescontado);
        cuota.setDesgravamenPagado(desgravamenPagadoAnterior + desgravamenDescontado);
        
        // Calcular saldos pendientes
        double capitalOriginal = nullSafe(cuota.getCapital());
        double interesOriginal = nullSafe(cuota.getInteres());
        // double desgravamenOriginal = nullSafe(cuota.getDesgravamen());
        
        if (nuevoEstado == EstadoCuotaPrestamo.PAGADA) {
            // Cuota pagada completamente - saldos en cero
            cuota.setSaldoCapital(0.0);
            cuota.setSaldoInteres(0.0);
            cuota.setFechaPagado(obtenerUltimoDiaDelMes(cargaArchivo));
            System.out.println("  Cuota PAGADA - Saldos en cero");
        } else {
            // Cuota parcial o pendiente - calcular saldos
            double saldoCapital = capitalOriginal - cuota.getCapitalPagado();
            double saldoInteres = interesOriginal - cuota.getInteresPagado();
            
            // Asegurar que los saldos no sean negativos
            cuota.setSaldoCapital(Math.max(0, saldoCapital));
            cuota.setSaldoInteres(Math.max(0, saldoInteres));
            
            System.out.println("  Saldo Capital: $" + String.format("%,.2f", cuota.getSaldoCapital()));
            System.out.println("  Saldo Interés: $" + String.format("%,.2f", cuota.getSaldoInteres()));
        }
        
        detallePrestamoDaoService.save(cuota, cuota.getCodigo());
        System.out.println("✅ Cuota actualizada con estado: " + nuevoEstado);
        
        // 7. Guardar saldos en el procesamiento
        procesamiento.setSaldoCapitalPendiente(cuota.getSaldoCapital());
        procesamiento.setSaldoInteresPendiente(cuota.getSaldoInteres());
        
        // 8. Actualizar el préstamo
        actualizarPrestamo(prestamo);
        
        // 9. Marcar como procesado OK
        procesamiento.setProcesado(1);
        procesamiento.setNovedadProcesamiento(ASPNovedadesCargaArchivo.PRESTAMO_PROCESADO_OK);
        System.out.println("Procesamiento exitoso");
    }
    
    /**
     * Crea un registro de PagoPrestamo con desglose correcto de valores
     * 
     * REGLA DE NEGOCIO:
     * - El archivo Petrocomercial envía: capitalDescontado, interesDescontado, seguroDescontado
     * - seguroDescontado = DESGRAVAMEN pagado (NO es seguro de incendio)
     * - La cuota tiene: capital, interes, desgravamen
     * - El total de la cuota = capital + interes + desgravamen
     * - Si el pago es completo: se pagan todos los componentes
     * - Si el pago es parcial: prioridad = capital > interes > desgravamen
     */
    private PagoPrestamo crearPagoPrestamo(ParticipeXCargaArchivo registro, Prestamo prestamo,
                                          DetallePrestamo cuota, CargaArchivo cargaArchivo) {
        PagoPrestamo pago = new PagoPrestamo();
        
        // ====================================================================
        // 1. VALORES DEL ARCHIVO PETROCOMERCIAL
        // ====================================================================
        double totalDescontado = nullSafe(registro.getTotalDescontado());
        double capitalDescontadoArchivo = nullSafe(registro.getCapitalDescontado());
        double interesDescontadoArchivo = nullSafe(registro.getInteresDescontado());
        double desgravamenDescontadoArchivo = nullSafe(registro.getSeguroDescontado()); // seguroDescontado = desgravamen
        
        // ====================================================================
        // 2. VALORES DE LA CUOTA EN EL SISTEMA
        // ====================================================================
        double capitalCuota = nullSafe(cuota.getCapital());
        double interesCuota = nullSafe(cuota.getInteres());
        double desgravamenCuota = nullSafe(cuota.getDesgravamen());
        double totalCuota = capitalCuota + interesCuota + desgravamenCuota;
        
        System.out.println("====================================");
        System.out.println("DESGLOSE DEL PAGO - Cuota #" + cuota.getNumeroCuota());
        System.out.println("====================================");
        System.out.println("VALORES DE LA CUOTA:");
        System.out.println("  Capital:     $" + String.format("%,.2f", capitalCuota));
        System.out.println("  Interés:     $" + String.format("%,.2f", interesCuota));
        System.out.println("  Desgravamen: $" + String.format("%,.2f", desgravamenCuota));
        System.out.println("  TOTAL CUOTA: $" + String.format("%,.2f", totalCuota));
        System.out.println("------------------------------------");
        System.out.println("VALORES PAGADOS (ARCHIVO PETRO):");
        System.out.println("  Capital:     $" + String.format("%,.2f", capitalDescontadoArchivo));
        System.out.println("  Interés:     $" + String.format("%,.2f", interesDescontadoArchivo));
        System.out.println("  Desgravamen: $" + String.format("%,.2f", desgravamenDescontadoArchivo));
        System.out.println("  TOTAL PAGADO: $" + String.format("%,.2f", totalDescontado));
        System.out.println("====================================");
        
        // ====================================================================
        // 3. VALIDAR CONSISTENCIA DE VALORES
        // ====================================================================
        double sumaParciales = capitalDescontadoArchivo + interesDescontadoArchivo + desgravamenDescontadoArchivo;
        if (Math.abs(totalDescontado - sumaParciales) > TOLERANCIA) {
            System.out.println("⚠️ ADVERTENCIA: Diferencia entre total y suma de parciales: $" + 
                             String.format("%,.2f", Math.abs(totalDescontado - sumaParciales)));
        }
        
        // ====================================================================
        // 4. RELACIONES Y DATOS BÁSICOS
        // ====================================================================
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(cuota);
        pago.setFecha(obtenerUltimoDiaDelMes(cargaArchivo));
        pago.setNumeroCuota(cuota.getNumeroCuota());
        
        // ====================================================================
        // 5. DESGLOSE CORRECTO DE VALORES PAGADOS
        // ====================================================================
        
        // Total pagado
        pago.setValor(totalDescontado);
        
        // Capital pagado (siempre viene del archivo)
        pago.setCapitalPagado(capitalDescontadoArchivo);
        
        // Interés pagado (siempre viene del archivo)
        pago.setInteresPagado(interesDescontadoArchivo);
        
        // Desgravamen pagado
        // CORRECCIÓN: El archivo SÍ envía el desgravamen en el campo seguroDescontado
        pago.setDesgravamen(desgravamenDescontadoArchivo);
        
        // ====================================================================
        // 6. SEGURO DE INCENDIO
        // ====================================================================
        // El seguro de incendio NO viene desglosado en el archivo Petrocomercial
        // Si la cuota tiene seguro de incendio, se debe obtener de otro lado
        // Por ahora lo dejamos en 0 o se puede calcular si hay una regla específica
        pago.setValorSeguroIncendio(0.0);
        
        // ====================================================================
        // 7. OTROS VALORES
        // ====================================================================
        pago.setMoraPagada(0.0);
        pago.setInteresVencidoPagado(0.0);
        pago.setSaldoOtros(0.0);
        
        // ====================================================================
        // 8. OBSERVACIONES Y METADATA
        // ====================================================================
        pago.setObservacion("Pago archivo Petrocomercial " + 
                           cargaArchivo.getMesAfectacion() + "/" + cargaArchivo.getAnioAfectacion() +
                           " - Cuota #" + cuota.getNumeroCuota());
        pago.setTipo("DESCUENTO_NOMINA");
        
        // Estado y auditoría
        pago.setEstado(Long.valueOf(Estado.ACTIVO));
        pago.setIdEstado(Long.valueOf(Estado.ACTIVO));
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setUsuarioRegistro("SISTEMA_PETRO");
        
        System.out.println("✅ PagoPrestamo creado correctamente");
        
        return pago;
    }
    
    /**
     * Actualiza los totales y estado del préstamo
     */
    private void actualizarPrestamo(Prestamo prestamo) throws Throwable {
        // Obtener todas las cuotas del préstamo
        List<DatosBusqueda> criterios = new ArrayList<>();
        DatosBusqueda criterio = new DatosBusqueda();
        criterio.setCampo("prestamo.codigo");
        criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
        criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
        criterio.setValor(prestamo.getCodigo().toString());
        criterios.add(criterio);
        
        List<DetallePrestamo> todasLasCuotas = 
            detallePrestamoDaoService.selectByCriteria(criterios, NombreEntidadesCredito.DETALLE_PRESTAMO);
        
        // Calcular saldo total y verificar estados
        double saldoTotal = 0.0;
        boolean tieneMora = false;
        boolean todasPagadas = true;
        
        for (DetallePrestamo cuota : todasLasCuotas) {
            if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.PAGADA) {
                // Cuota pagada, no suma al saldo
            } else {
                todasPagadas = false;
                saldoTotal += nullSafe(cuota.getSaldoCapital());
                
                if (cuota.getEstado() != null && cuota.getEstado() == EstadoCuotaPrestamo.EN_MORA) {
                    tieneMora = true;
                }
            }
        }
        
        // Actualizar saldo del préstamo
        prestamo.setSaldoTotal(saldoTotal);
        
        // Determinar nuevo estado del préstamo
        if (todasPagadas && saldoTotal == 0) {
            prestamo.setEstadoPrestamo(Long.valueOf(EstadoPrestamo.CANCELADO));
            System.out.println("Préstamo CANCELADO (todas las cuotas pagadas)");
        } else if (tieneMora) {
            prestamo.setEstadoPrestamo(Long.valueOf(EstadoPrestamo.EN_MORA));
            System.out.println("Préstamo EN MORA");
        } else {
            prestamo.setEstadoPrestamo(Long.valueOf(EstadoPrestamo.VIGENTE));
            System.out.println("Préstamo VIGENTE");
        }
        
        // Guardar préstamo
        prestamoDaoService.save(prestamo, prestamo.getCodigo());
    }
    
    /**
     * Obtiene el último día del mes del archivo
     */
    private LocalDateTime obtenerUltimoDiaDelMes(CargaArchivo cargaArchivo) {
        int mes = cargaArchivo.getMesAfectacion().intValue();
        int anio = cargaArchivo.getAnioAfectacion().intValue();
        
        // Último día del mes
        int ultimoDia;
        if (mes == 2) {
            // Febrero
            boolean esBisiesto = (anio % 4 == 0 && anio % 100 != 0) || (anio % 400 == 0);
            ultimoDia = esBisiesto ? 29 : 28;
        } else if (mes == 4 || mes == 6 || mes == 9 || mes == 11) {
            ultimoDia = 30;
        } else {
            ultimoDia = 31;
        }
        
        return LocalDateTime.of(anio, mes, ultimoDia, 23, 59, 59);
    }
    
    /**
     * Retorna 0.0 si el valor es null
     */
    private double nullSafe(Double valor) {
        return valor != null ? valor : 0.0;
    }

}
