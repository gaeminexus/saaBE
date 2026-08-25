package com.saa.ejb.crd.serviceImpl;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.CalculadoraAmortizacionService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCuotaPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PrestamoServiceImpl implements PrestamoService {

    @EJB
    private PrestamoDaoService prestamoDaoService;
    
    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;
    
    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private CalculadoraAmortizacionService calculadoraAmortizacionService;

    @Override
    public Prestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PrestamoService ... depurado");
        Prestamo prestamo = new Prestamo();
        for (Long registro : id) {
            prestamoDaoService.remove(prestamo, registro);
        }
    }

    @Override
    public void save(List<Prestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PrestamoService");
        for (Prestamo registro : lista) {
            prestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Prestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Prestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public Prestamo saveSingle(Prestamo prestamo) throws Throwable {
        System.out.println("saveSingle - Prestamo");
        
        // Validar si el idAsoprep ya existe en otro préstamo
        if (prestamo.getIdAsoprep() != null) {
            validarIdAsoprepUnico(prestamo.getIdAsoprep(), prestamo.getCodigo());
        }
        
        if(prestamo.getCodigo() == null){
        	prestamo.setIdEstado(Long.valueOf(Estado.ACTIVO));
		}

        // D10 (decisión 11 del plan de simuladores): PRSTTSAA y PRSTINNM son una sola tasa.
        // Se deriva interesNominal de tasa en cada guardado, para que el proceso de mora
        // (ProcesoMoraPrestamoServiceImpl) deje de caer al default silencioso de 9%.
        if (prestamo.getTasa() != null) {
            System.out.println("saveSingle - Prestamo " + prestamo.getCodigo()
                + ": derivando interesNominal (PRSTINNM) de tasa (PRSTTSAA=" + prestamo.getTasa() + ")");
            prestamo.setInteresNominal(prestamo.getTasa());
        }

        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        return prestamo;
    }

    @Override
    public List<Prestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Prestamo no devolvio ningun registro");
        }
        return result;
    }
    
    @Override
    public Prestamo generarTablaAmortizacion(Long idPrestamo, Long tieneCuotaCero) throws Throwable {
        System.out.println("Generando tabla de amortización para préstamo ID: " + idPrestamo + " - Cuota 0: " + tieneCuotaCero);
        
        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }
        
        if (prestamo.getTipoAmortizacion() == null) {
            throw new IncomeException("El préstamo no tiene definido el tipo de amortización");
        }
        if (prestamo.getPlazo() == null || prestamo.getPlazo() <= 0) {
            throw new IncomeException("El préstamo no tiene definido un plazo válido");
        }
        if (prestamo.getTasa() == null || prestamo.getTasa() <= 0) {
            throw new IncomeException("El préstamo no tiene definida una tasa válida");
        }
        if (prestamo.getMontoSolicitado() == null || prestamo.getMontoSolicitado() <= 0) {
            throw new IncomeException("El préstamo no tiene definido un monto válido");
        }
        if (prestamo.getFechaInicio() == null) {
            throw new IncomeException("El préstamo no tiene definida una fecha de inicio");
        }
        
        if (tieneCuotaCero == null) {
            tieneCuotaCero = 0L;
        }
        
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        if (prestamo.getTipoAmortizacion() == 1) {
            detalles = generarAmortizacionFrancesa(prestamo, tieneCuotaCero);
        } else if (prestamo.getTipoAmortizacion() == 2) {
            detalles = generarAmortizacionAlemana(prestamo, tieneCuotaCero);
        } else {
            throw new IncomeException("Tipo de amortización no válido. Use 1 para Francesa o 2 para Alemana");
        }
        
        for (DetallePrestamo detalle : detalles) {
            detallePrestamoDaoService.save(detalle, detalle.getCodigo());
        }
        
        actualizarCamposPrestamo(prestamo, detalles);
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        
        System.out.println("Tabla de amortización generada exitosamente con " + detalles.size() + " cuotas");
        return prestamo;
    }
    
    private List<DetallePrestamo> generarAmortizacionFrancesa(Prestamo prestamo, Long tieneCuotaCero) throws Throwable {
        return generarAmortizacion(prestamo, tieneCuotaCero, 1L);
    }

    private List<DetallePrestamo> generarAmortizacionAlemana(Prestamo prestamo, Long tieneCuotaCero) throws Throwable {
        return generarAmortizacion(prestamo, tieneCuotaCero, 2L);
    }

    /**
     * Arma los parámetros desde el préstamo ya guardado, delega el cálculo puro en
     * {@link CalculadoraAmortizacionService} (fase 1 de PLAN-SIMULADORES-PRESTAMOS.md — una
     * sola fórmula para todo el sistema) y mapea el resultado a {@code DetallePrestamo},
     * llenando los campos de bitácora que la calculadora no conoce (mora, abono, estado, ...).
     */
    private List<DetallePrestamo> generarAmortizacion(Prestamo prestamo, Long tieneCuotaCero, long tipoAmortizacion)
            throws Throwable {

        ParametrosAmortizacion params = new ParametrosAmortizacion();
        params.setMonto(prestamo.getMontoSolicitado());
        params.setTasaAnual(prestamo.getTasa());
        params.setPlazo(prestamo.getPlazo().intValue());
        params.setTipoAmortizacion(tipoAmortizacion);
        params.setFechaInicio(prestamo.getFechaInicio());
        params.setTieneCuotaCero(tieneCuotaCero != null && tieneCuotaCero == 1L);
        // El generador de tabla nueva no calcula desgravamen ni seguro de incendio por cuota
        // (comportamiento preexistente, sin cambios en esta fase): quedan en 0.0.
        params.setDesgravamenPorCuota(0.0);
        params.setSeguroIncendioPorCuota(0.0);

        List<CuotaProyectada> tabla = calculadoraAmortizacionService.calcular(params);

        List<DetallePrestamo> detalles = new ArrayList<>();
        for (CuotaProyectada proyectada : tabla) {
            detalles.add(construirDetalle(prestamo, proyectada));
        }
        return detalles;
    }

    /** Mapea una fila pura de la calculadora a una entidad DTPR con TODOS los campos del invariante llenos. */
    private DetallePrestamo construirDetalle(Prestamo prestamo, CuotaProyectada proyectada) {
        DetallePrestamo detalle = new DetallePrestamo();

        detalle.setPrestamo(prestamo);
        detalle.setNumeroCuota(proyectada.getNumeroCuota());
        detalle.setFechaVencimiento(proyectada.getFechaVencimiento());
        detalle.setCapital(proyectada.getCapital());
        detalle.setInteres(proyectada.getInteres());
        detalle.setCuota(proyectada.getCuota());

        double saldoCapital = redondear(Math.max(0.0, nvl(proyectada.getSaldoCapital())));
        detalle.setSaldoCapital(saldoCapital);
        detalle.setSaldo(saldoCapital);

        detalle.setMora(0.0);
        detalle.setInteresVencido(0.0);
        detalle.setSaldoInteres(proyectada.getInteres());
        detalle.setSaldoMora(0.0);
        detalle.setSaldoInteresVencido(0.0);
        detalle.setAbono(0.0);
        detalle.setCapitalPagado(0.0);
        detalle.setInteresPagado(0.0);
        // La calculadora ya llena desgravamen/seguroIncendio/total con el mismo invariante que
        // DTPRTTLL (decisión 15 del plan): se copian tal cual, sin recalcular acá.
        double desgravamen = nvl(proyectada.getDesgravamen());
        double seguro = nvl(proyectada.getSeguroIncendio());
        detalle.setDesgravamen(desgravamen);
        detalle.setSaldoOtros(0.0);
        detalle.setDesgravamenFirmado(desgravamen);
        detalle.setDesgravamenDiferido(0.0);
        detalle.setDesgravamenOriginal(desgravamen);
        detalle.setValorDiferido(0.0);
        // Invariante: saldoInicialCapital = capital + saldoCapital + saldoOtros (saldoOtros = 0 acá)
        detalle.setSaldoInicialCapital(redondear(nvl(proyectada.getCapital()) + saldoCapital));
        detalle.setValorSeguroIncendio(seguro);
        detalle.setTotal(redondear(nvl(proyectada.getTotal())));
        detalle.setTotalConSeguro(detalle.getTotal());
        detalle.setEstado(Long.valueOf(Estado.ACTIVO));
        detalle.setIdEstado(Long.valueOf(Estado.ACTIVO));

        return detalle;
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private void actualizarCamposPrestamo(Prestamo prestamo, List<DetallePrestamo> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        
        double totalCapital = 0.0;
        double totalInteres = 0.0;
        double valorCuota = 0.0;
        LocalDateTime fechaFin = null;
        
        for (DetallePrestamo detalle : detalles) {
            if (detalle.getCapital() != null) {
                totalCapital += detalle.getCapital();
            }
            if (detalle.getInteres() != null) {
                totalInteres += detalle.getInteres();
            }
            
            if (valorCuota == 0.0 && detalle.getNumeroCuota() != null 
                && detalle.getNumeroCuota() > 0 && detalle.getCuota() != null) {
                valorCuota = detalle.getCuota();
            }
            
            if (detalle.getFechaVencimiento() != null) {
                fechaFin = detalle.getFechaVencimiento();
            }
        }
        
        prestamo.setValorCuota(redondear(valorCuota));
        prestamo.setFechaFin(fechaFin);
        prestamo.setTotalCapital(redondear(totalCapital));
        prestamo.setTotalInteres(redondear(totalInteres));
        
        double tasaAnual = prestamo.getTasa();
        prestamo.setTasaNominal(redondear(tasaAnual));
        
        double tasaMensual = tasaAnual / 100 / 12;
        double tasaEfectiva = (Math.pow(1 + tasaMensual, 12) - 1) * 100;
        prestamo.setTasaEfectiva(redondear(tasaEfectiva));
        
        double totalPrestamo = totalCapital + totalInteres;
        prestamo.setTotalPrestamo(redondear(totalPrestamo));
        
        System.out.println("Préstamo actualizado - Valor Cuota: " + valorCuota + 
                         ", Total Capital: " + totalCapital + 
                         ", Total Interés: " + totalInteres +
                         ", Tasa Nominal: " + tasaAnual + "%" +
                         ", Tasa Efectiva: " + tasaEfectiva + "%");
    }
    
    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    @Override
    public Prestamo cargarTablaAmortizacionDesdeExcel(Long idPrestamo, InputStream archivoExcel) throws Throwable {
        System.out.println("Cargando tabla de amortización desde Excel para préstamo ID: " + idPrestamo);
        
        // Obtener el préstamo
        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }
        
        // Validar si el idAsoprep ya existe en otro préstamo
        if (prestamo.getIdAsoprep() != null) {
            validarIdAsoprepUnico(prestamo.getIdAsoprep(), prestamo.getCodigo());
        }
        
        List<DetallePrestamo> detalles = new ArrayList<>();
        double totalCapital = 0.0;
        double totalInteres = 0.0;
        double valorCuotaPrimera = 0.0;
        LocalDateTime fechaFin = null;
        // Saldo de capital acumulado para calcular correctamente en cuotas pagadas
        double saldoCapitalAcumulado = prestamo.getMontoSolicitado() != null ? prestamo.getMontoSolicitado() : 0.0;
        
        try (Workbook workbook = WorkbookFactory.create(archivoExcel)) {
            Sheet sheet = workbook.getSheetAt(0); // Primera hoja
            
            // Formateador de fechas (ajustar según formato del Excel)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            // Iterar desde la fila 2 (asumiendo que fila 1 es encabezado)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Verificar si la fila está vacía
                Cell firstCell = row.getCell(0);
                if (firstCell == null || firstCell.getCellType() == CellType.BLANK) {
                    continue;
                }
                
                DetallePrestamo detalle = new DetallePrestamo();
                
                // NroCuota (columna 0)
                Double numeroCuota = getCellValueAsDouble(row.getCell(0));
                detalle.setNumeroCuota(numeroCuota);
                
                // FECHA VENCE (columna 1)
                LocalDateTime fechaVencimiento = getCellValueAsDate(row.getCell(1), formatter);
                detalle.setFechaVencimiento(fechaVencimiento);
                if (fechaVencimiento != null) {
                    fechaFin = fechaVencimiento;
                }
                
                // PAGO EXTRA (columna 2) → saldoOtros
                Double pagoExtra = getCellValueAsDouble(row.getCell(2));
                detalle.setSaldoOtros(pagoExtra != null ? pagoExtra : 0.0);
                
                // SALDO DE CAPITAL (columna 3) → referencia del Excel (puede venir en 0 para cuotas pagadas)
                // Se usa saldoCapitalAcumulado como saldoInicialCapital para garantizar consistencia
                Double saldoInicialCapital = redondear(saldoCapitalAcumulado);
                
                // PAGO DE CAPITAL (columna 4) → capital
                Double pagoCapital = getCellValueAsDouble(row.getCell(4));
                detalle.setCapital(pagoCapital != null ? pagoCapital : 0.0);
                totalCapital += (pagoCapital != null ? pagoCapital : 0.0);
                
                // Calcular saldoCapital = saldoInicialCapital - capital - pagoExtra y actualizar acumulado
                saldoCapitalAcumulado -= (pagoCapital != null ? pagoCapital : 0.0);
                saldoCapitalAcumulado -= (pagoExtra != null ? pagoExtra : 0.0);
                Double saldoCapitalCalculado = redondear(Math.max(0, saldoCapitalAcumulado));
                detalle.setSaldoCapital(saldoCapitalCalculado);
                detalle.setSaldo(saldoCapitalCalculado);
                
                // Establecer saldoInicialCapital
                detalle.setSaldoInicialCapital(saldoInicialCapital);
                
                // VALOR DEL INTERÉS (columna 5) → interes
                Double valorInteres = getCellValueAsDouble(row.getCell(5));
                detalle.setInteres(valorInteres != null ? valorInteres : 0.0);
                totalInteres += (valorInteres != null ? valorInteres : 0.0);
                
                // DESGRAVAMEN (columna 6)
                Double desgravamen = getCellValueAsDouble(row.getCell(6));
                detalle.setDesgravamen(desgravamen != null ? desgravamen : 0.0);
                detalle.setDesgravamenOriginal(desgravamen != null ? desgravamen : 0.0);
                detalle.setDesgravamenFirmado(desgravamen != null ? desgravamen : 0.0);
                
                // SEGURO (columna 7) → valorSeguroIncendio
                Double seguro = getCellValueAsDouble(row.getCell(7));
                detalle.setValorSeguroIncendio(seguro != null ? seguro : 0.0);
                
                // CUOTA A PAGAR (columna 8) → total
                Double cuotaPagar = getCellValueAsDouble(row.getCell(8));
                detalle.setTotal(cuotaPagar != null ? cuotaPagar : 0.0);
                
                // Calcular cuota como capital + interes
                double cuota = (pagoCapital != null ? pagoCapital : 0.0) + 
                              (valorInteres != null ? valorInteres : 0.0);
                detalle.setCuota(redondear(cuota));
                
                // Obtener valor de primera cuota normal (mayor a 0)
                if (valorCuotaPrimera == 0.0 && numeroCuota != null && numeroCuota > 0) {
                    valorCuotaPrimera = cuota;
                }
                
                // ESTADO (columna 9) - Mapear texto a código numérico
                Cell estadoCell = row.getCell(9);
                Long estadoCodigo = mapearEstadoTextoACodigo(estadoCell);
                detalle.setEstado(estadoCodigo);
                detalle.setIdEstado(estadoCodigo);

                // Ajustar campos saldo/pagado según el estado
                detalle.setPrestamo(prestamo);
                detalle.setMora(0.0);
                detalle.setInteresVencido(0.0);
                detalle.setDesgravamenDiferido(0.0);
                detalle.setValorDiferido(0.0);

                int estadoInt = estadoCodigo.intValue();
                if (estadoInt == EstadoCuotaPrestamo.PAGADA ||
                    estadoInt == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA) {
                    // Cuota ya pagada: saldo capital se mantiene calculado (saldoInicialCapital - capital)
                    // saldoCapital y saldo ya fueron asignados arriba correctamente
                    detalle.setSaldoInteres(0.0);
                    detalle.setSaldoMora(0.0);
                    detalle.setSaldoInteresVencido(0.0);
                    detalle.setCapitalPagado(pagoCapital != null ? pagoCapital : 0.0);
                    detalle.setInteresPagado(valorInteres != null ? valorInteres : 0.0);
                    detalle.setMoraPagado(0.0);
                    detalle.setDesgravamenPagado(desgravamen != null ? desgravamen : 0.0);
                    detalle.setAbono(cuota);
                    detalle.setFechaPagado(fechaVencimiento);
                    System.out.println("Cuota " + numeroCuota + " cargada como " +
                        (estadoInt == EstadoCuotaPrestamo.PAGADA ? "PAGADA" : "CANCELADA_ANTICIPADA") +
                        " - Capital pagado: " + detalle.getCapitalPagado() +
                        ", Interés pagado: " + detalle.getInteresPagado());
                } else {
                    // Cuota pendiente/mora/parcial/vencida: saldos según lo calculado
                    detalle.setSaldoInteres(valorInteres != null ? valorInteres : 0.0);
                    detalle.setSaldoMora(0.0);
                    detalle.setSaldoInteresVencido(0.0);
                    detalle.setAbono(0.0);
                    detalle.setCapitalPagado(0.0);
                    detalle.setInteresPagado(0.0);
                    detalle.setMoraPagado(0.0);
                    detalle.setDesgravamenPagado(0.0);
                    detalle.setFechaPagado(null);
                }
                
                detalles.add(detalle);
            }
            
            System.out.println("Se cargaron " + detalles.size() + " cuotas desde el Excel");
            
        } catch (Exception e) {
            throw new IncomeException("Error al procesar el archivo Excel: " + e.getMessage());
        }
        
        if (detalles.isEmpty()) {
            throw new IncomeException("No se encontraron datos válidos en el archivo Excel");
        }
        
        // Guardar todos los detalles en la base de datos
        for (DetallePrestamo detalle : detalles) {
            detallePrestamoDaoService.save(detalle, detalle.getCodigo());
            
            // Si el estado es PAGADA o CANCELADA_ANTICIPADA, crear registro de PagoPrestamo
            if (detalle.getEstado() != null && 
                (detalle.getEstado() == EstadoCuotaPrestamo.PAGADA || 
                 detalle.getEstado() == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA)) {
                crearPagoPrestamo(prestamo, detalle);
            }
        }
        
        // Actualizar campos del préstamo
        prestamo.setValorCuota(redondear(valorCuotaPrimera));
        prestamo.setFechaFin(fechaFin);
        prestamo.setTotalCapital(redondear(totalCapital));
        prestamo.setTotalInteres(redondear(totalInteres));
        
        // Calcular tasas (usar la tasa que ya tiene el préstamo)
        if (prestamo.getTasa() != null) {
            double tasaAnual = prestamo.getTasa();
            prestamo.setTasaNominal(redondear(tasaAnual));
            
            double tasaMensual = tasaAnual / 100 / 12;
            double tasaEfectiva = (Math.pow(1 + tasaMensual, 12) - 1) * 100;
            prestamo.setTasaEfectiva(redondear(tasaEfectiva));
        }
        
        double totalPrestamo = totalCapital + totalInteres;
        prestamo.setTotalPrestamo(redondear(totalPrestamo));
        
        // Guardar préstamo actualizado
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        
        System.out.println("Tabla de amortización cargada exitosamente - Total Capital: " + totalCapital + 
                         ", Total Interés: " + totalInteres);
        
        return prestamo;
    }
    
    /**
     * Mapea el estado de texto del Excel a código numérico según EstadoCuotaPrestamo.
     * Estados válidos:
     *   PENDIENTE = 1, ACTIVA = 2, EMITIDA = 3, PAGADA = 4,
     *   EN_MORA = 5, PARCIAL = 6, CANCELADA_ANTICIPADA = 7, VENCIDA = 8
     *
     * @param cell Celda que contiene el estado
     * @return Código numérico del estado
     */
    private Long mapearEstadoTextoACodigo(Cell cell) {
        if (cell == null) {
            return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                // Si ya viene como número, usarlo directamente
                return (long) cell.getNumericCellValue();
            }

            if (cell.getCellType() != CellType.STRING) {
                return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

            String estadoTexto = cell.getStringCellValue().trim().toUpperCase()
                    .replace("  ", " "); // normalizar espacios dobles

            if (estadoTexto.isEmpty()) {
                return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

            switch (estadoTexto) {
                case "PENDIENTE":
                    return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);         // 1
                case "ACTIVA":
                case "ACTIVO":
                    return Long.valueOf(EstadoCuotaPrestamo.ACTIVA);            // 2
                case "EMITIDA":
                case "EMITIDO":
                    return Long.valueOf(EstadoCuotaPrestamo.EMITIDA);           // 3
                case "PAGADA":
                case "PAGADO":
                    return Long.valueOf(EstadoCuotaPrestamo.PAGADA);            // 4
                case "EN MORA":
                case "EN_MORA":
                case "MORA":
                    return Long.valueOf(EstadoCuotaPrestamo.EN_MORA);           // 5
                case "PARCIAL":
                case "PAGO PARCIAL":
                case "PAGO_PARCIAL":
                    return Long.valueOf(EstadoCuotaPrestamo.PARCIAL);           // 6
                case "CANCELADA ANTICIPADA":
                case "CANCELADA_ANTICIPADA":
                case "CANCELADO ANTICIPADO":
                case "CANCELADO_ANTICIPADO":
                case "CANCELADA":
                    return Long.valueOf(EstadoCuotaPrestamo.CANCELADA_ANTICIPADA); // 7
                case "VENCIDA":
                case "VENCIDO":
                    return Long.valueOf(EstadoCuotaPrestamo.VENCIDA);           // 8
                default:
                    System.out.println("Estado desconocido en Excel: '" + estadoTexto + 
                                       "', se asigna PENDIENTE (" + EstadoCuotaPrestamo.PENDIENTE + ")");
                    return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
            }

        } catch (Exception e) {
            System.err.println("Error al leer estado de la celda: " + e.getMessage());
            return Long.valueOf(EstadoCuotaPrestamo.PENDIENTE);
        }
    }
    
    /**
     * Crea un registro de PagoPrestamo para una cuota pagada.
     * @param prestamo Préstamo asociado
     * @param detalle Detalle de la cuota pagada
     */
    private void crearPagoPrestamo(Prestamo prestamo, DetallePrestamo detalle) throws Throwable {
        System.out.println("Creando PagoPrestamo para cuota " + detalle.getNumeroCuota());
        
        PagoPrestamo pago = new PagoPrestamo();
        
        // Relaciones
        pago.setPrestamo(prestamo);
        pago.setDetallePrestamo(detalle);
        
        // Fecha de pago = fecha de vencimiento (asumimos que se pagó exactamente en la fecha)
        pago.setFecha(detalle.getFechaVencimiento());
        
        // Valores del pago
        pago.setValor(detalle.getCuota()); // valor pagado = cuota
        pago.setNumeroCuota(detalle.getNumeroCuota());
        pago.setCapitalPagado(detalle.getCapital());
        pago.setInteresPagado(detalle.getInteres());
        pago.setMoraPagada(0.0); // No hay mora
        pago.setInteresVencidoPagado(0.0); // No hay interés vencido
        pago.setDesgravamen(detalle.getDesgravamen());
        pago.setSaldoOtros(detalle.getSaldoOtros()); // pago extra
        
        // Observación
        pago.setObservacion("Pago cargado desde Excel - Migración de datos");
        pago.setTipo("MIGRACION");
        
        // Estado
        pago.setEstado(Long.valueOf(Estado.ACTIVO));
        pago.setIdEstado(Long.valueOf(Estado.ACTIVO));
        
        // Fecha y usuario de registro
        pago.setFechaRegistro(LocalDateTime.now());
        pago.setUsuarioRegistro("SISTEMA");
        
        // Guardar el pago
        pagoPrestamoDaoService.save(pago, pago.getCodigo());
        
        System.out.println("PagoPrestamo creado exitosamente para cuota " + detalle.getNumeroCuota() + 
                         " - Valor: " + pago.getValor());
    }
    
    /**
     * Obtiene el valor de una celda como Double.
     */
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
    
    /**
     * Obtiene el valor de una celda como LocalDateTime.
     */
    private LocalDateTime getCellValueAsDate(Cell cell, DateTimeFormatter formatter) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    // Si es fecha de Excel
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return cell.getLocalDateTimeCellValue();
                    }
                    return null;
                case STRING:
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) return null;
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay();
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error al parsear fecha: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Valida que el idAsoprep sea único en la base de datos.
     * @param idAsoprep ID del asociado préstamo a validar
     * @param codigoPrestamo Código del préstamo actual (null si es nuevo)
     * @throws Throwable Si el idAsoprep ya existe en otro préstamo
     */
    private void validarIdAsoprepUnico(Long idAsoprep, Long codigoPrestamo) throws Throwable {
        System.out.println("Validando idAsoprep único: " + idAsoprep + " para préstamo: " + codigoPrestamo);
        
        // Buscar si existe un préstamo con el mismo idAsoprep
        Prestamo prestamoExistente = prestamoDaoService.selectByIdAsoprep(idAsoprep);
        
        // Si existe un préstamo con ese idAsoprep
        if (prestamoExistente != null) {
            // Si es un préstamo nuevo (sin código) o si el código no coincide, es un duplicado
            if (codigoPrestamo == null || !prestamoExistente.getCodigo().equals(codigoPrestamo)) {
                System.out.println("ERROR: idAsoprep " + idAsoprep + " ya existe en el préstamo con código: " + prestamoExistente.getCodigo());
                throw new IncomeException("El préstamo con idAsoprep " + idAsoprep + " ya existe en el sistema (Código: " + prestamoExistente.getCodigo() + "). No se puede duplicar.");
            }
        }
        
        System.out.println("Validación exitosa: idAsoprep " + idAsoprep + " es único");
    }

	@Override
	public Prestamo actualizarCamposDesdeTabla(Long idPrestamo) throws Throwable {
		System.out.println("Recalculando campos derivados del préstamo desde su tabla viva: " + idPrestamo);

		Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
		if (prestamo == null) {
			throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
		}

		List<DetallePrestamo> detalles = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
		actualizarCamposPrestamo(prestamo, detalles);
		return prestamoDaoService.save(prestamo, prestamo.getCodigo());
	}

	@Override
	public java.util.List<Prestamo> selectByRangoFechas(java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return prestamoDaoService.selectByRangoFechas(fechaInicio, fechaFin);
	}

	@Override
	public java.util.List<Prestamo> selectByEstado(Long estado) throws Throwable {
		return prestamoDaoService.selectByEstado(estado);
	}

	@Override
	public long countVigentesMoraVencidosByEntidad(Long codigoEntidad) throws Throwable {
		return prestamoDaoService.countVigentesMoraVencidosByEntidad(codigoEntidad);
	}

	@Override
	public long countPrestamosConUltimaCuotaEnPeriodoByEntidad(Long codigoEntidad,
			java.time.LocalDateTime fechaInicio, java.time.LocalDateTime fechaFin) throws Throwable {
		return prestamoDaoService.countPrestamosConUltimaCuotaEnPeriodoByEntidad(codigoEntidad, fechaInicio, fechaFin);
	}
}
