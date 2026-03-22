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

import com.saa.basico.ejb.FechaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.Estado;

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
    private FechaService fechaService;

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
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        double capital = prestamo.getMontoSolicitado();
        double tasaMensual = prestamo.getTasa() / 100 / 12;
        double tasaDiaria = prestamo.getTasa() / 100 / 360; // Tasa diaria (base 360)
        int plazo = prestamo.getPlazo().intValue();
        int mesesGracia = (tieneCuotaCero != null && tieneCuotaCero == 1L) ? 1 : 0;
        
        double cuotaFija = capital * (tasaMensual * Math.pow(1 + tasaMensual, plazo)) / 
                          (Math.pow(1 + tasaMensual, plazo) - 1);
        
        double saldoCapital = capital;
        LocalDateTime fechaInicio = prestamo.getFechaInicio();
        LocalDateTime fechaVencimiento;
        
        // Calcular intereses proporcionales del mes inicial
        LocalDate fechaInicioLocal = fechaInicio.toLocalDate();
        LocalDate ultimoDiaMesInicio = fechaService.ultimoDiaMesAnioLocal(
            Long.valueOf(fechaInicioLocal.getMonthValue()), 
            Long.valueOf(fechaInicioLocal.getYear())
        );
        int diasMesInicial = (int) java.time.temporal.ChronoUnit.DAYS.between(fechaInicioLocal, ultimoDiaMesInicio) + 1;
        double interesesMesInicial = capital * tasaDiaria * diasMesInicial;
        
        System.out.println("Intereses proporcionales mes inicial - Días: " + diasMesInicial + 
                         ", Interés: " + redondear(interesesMesInicial));
        
        if (mesesGracia > 0) {
            DetallePrestamo detalle = new DetallePrestamo();
            double interes = saldoCapital * tasaMensual;
            
            LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(1);
            LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                Long.valueOf(fechaTemp.getMonthValue()), 
                Long.valueOf(fechaTemp.getYear())
            );
            fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(0.0);
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(0.0);
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(interes));
            detalle.setSaldoCapital(redondear(saldoCapital));
            detalle.setSaldo(redondear(saldoCapital));
            detalle.setMora(0.0);
            detalle.setInteresVencido(0.0);
            detalle.setSaldoInteres(redondear(interes));
            detalle.setSaldoMora(0.0);
            detalle.setSaldoInteresVencido(0.0);
            detalle.setAbono(0.0);
            detalle.setCapitalPagado(0.0);
            detalle.setInteresPagado(0.0);
            detalle.setDesgravamen(0.0);
            detalle.setSaldoOtros(0.0);
            detalle.setDesgravamenFirmado(0.0);
            detalle.setDesgravamenDiferido(0.0);
            detalle.setDesgravamenOriginal(0.0);
            detalle.setValorDiferido(0.0);
            detalle.setEstado(Long.valueOf(Estado.ACTIVO));
            
            detalles.add(detalle);
        }
        
        for (int i = 1; i <= plazo; i++) {
            DetallePrestamo detalle = new DetallePrestamo();
            
            double interes = saldoCapital * tasaMensual;
            
            // Si es la primera cuota Y NO HAY cuota 0, sumar intereses proporcionales del mes inicial
            // Si hay cuota 0, esos intereses ya se cobraron en la cuota 0
            if (i == 1 && mesesGracia == 0) {
                interes += interesesMesInicial;
                System.out.println("Cuota 1 (sin cuota 0) - Interés mensual: " + redondear(saldoCapital * tasaMensual) + 
                                 ", Interés mes inicial: " + redondear(interesesMesInicial) + 
                                 ", Total interés: " + redondear(interes));
            }
            
            double capitalCuota = cuotaFija - (saldoCapital * tasaMensual); // Capital se calcula con interés sin proporcional
            saldoCapital -= capitalCuota;
            
            if (i == plazo && Math.abs(saldoCapital) > 0.01) {
                capitalCuota += saldoCapital;
                saldoCapital = 0;
            }
            
            if (mesesGracia > 0) {
                LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(i + 1);
                LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                    Long.valueOf(fechaTemp.getMonthValue()), 
                    Long.valueOf(fechaTemp.getYear())
                );
                fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            } else {
                LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(i);
                LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                    Long.valueOf(fechaTemp.getMonthValue()), 
                    Long.valueOf(fechaTemp.getYear())
                );
                fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            }
            
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(Double.valueOf(i));
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(redondear(capitalCuota));
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(cuotaFija));
            detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
            detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
            detalle.setMora(0.0);
            detalle.setInteresVencido(0.0);
            detalle.setSaldoInteres(redondear(interes));
            detalle.setSaldoMora(0.0);
            detalle.setSaldoInteresVencido(0.0);
            detalle.setAbono(0.0);
            detalle.setCapitalPagado(0.0);
            detalle.setInteresPagado(0.0);
            detalle.setDesgravamen(0.0);
            detalle.setSaldoOtros(0.0);
            detalle.setDesgravamenFirmado(0.0);
            detalle.setDesgravamenDiferido(0.0);
            detalle.setDesgravamenOriginal(0.0);
            detalle.setValorDiferido(0.0);
            detalle.setEstado(Long.valueOf(Estado.ACTIVO));
            
            detalles.add(detalle);
        }
        
        return detalles;
    }
    
    private List<DetallePrestamo> generarAmortizacionAlemana(Prestamo prestamo, Long tieneCuotaCero) throws Throwable {
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        double capital = prestamo.getMontoSolicitado();
        double tasaMensual = prestamo.getTasa() / 100 / 12;
        double tasaDiaria = prestamo.getTasa() / 100 / 360; // Tasa diaria (base 360)
        int plazo = prestamo.getPlazo().intValue();
        int mesesGracia = (tieneCuotaCero != null && tieneCuotaCero == 1L) ? 1 : 0;
        
        double capitalFijo = capital / plazo;
        double saldoCapital = capital;
        LocalDateTime fechaInicio = prestamo.getFechaInicio();
        LocalDateTime fechaVencimiento;
        
        // Calcular intereses proporcionales del mes inicial
        LocalDate fechaInicioLocal = fechaInicio.toLocalDate();
        LocalDate ultimoDiaMesInicio = fechaService.ultimoDiaMesAnioLocal(
            Long.valueOf(fechaInicioLocal.getMonthValue()), 
            Long.valueOf(fechaInicioLocal.getYear())
        );
        int diasMesInicial = (int) java.time.temporal.ChronoUnit.DAYS.between(fechaInicioLocal, ultimoDiaMesInicio) + 1;
        double interesesMesInicial = capital * tasaDiaria * diasMesInicial;
        
        System.out.println("Intereses proporcionales mes inicial - Días: " + diasMesInicial + 
                         ", Interés: " + redondear(interesesMesInicial));
        
        if (mesesGracia > 0) {
            DetallePrestamo detalle = new DetallePrestamo();
            double interes = saldoCapital * tasaMensual;
            
            LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(1);
            LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                Long.valueOf(fechaTemp.getMonthValue()), 
                Long.valueOf(fechaTemp.getYear())
            );
            fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(0.0);
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(0.0);
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(interes));
            detalle.setSaldoCapital(redondear(saldoCapital));
            detalle.setSaldo(redondear(saldoCapital));
            detalle.setMora(0.0);
            detalle.setInteresVencido(0.0);
            detalle.setSaldoInteres(redondear(interes));
            detalle.setSaldoMora(0.0);
            detalle.setSaldoInteresVencido(0.0);
            detalle.setAbono(0.0);
            detalle.setCapitalPagado(0.0);
            detalle.setInteresPagado(0.0);
            detalle.setDesgravamen(0.0);
            detalle.setSaldoOtros(0.0);
            detalle.setDesgravamenFirmado(0.0);
            detalle.setDesgravamenDiferido(0.0);
            detalle.setDesgravamenOriginal(0.0);
            detalle.setValorDiferido(0.0);
            detalle.setEstado(Long.valueOf(Estado.ACTIVO));
            
            detalles.add(detalle);
        }
        
        for (int i = 1; i <= plazo; i++) {
            DetallePrestamo detalle = new DetallePrestamo();
            
            double interes = saldoCapital * tasaMensual;
            
            // Si es la primera cuota Y NO HAY cuota 0, sumar intereses proporcionales del mes inicial
            // Si hay cuota 0, esos intereses ya se cobraron en la cuota 0
            if (i == 1 && mesesGracia == 0) {
                interes += interesesMesInicial;
                System.out.println("Cuota 1 (sin cuota 0) - Interés mensual: " + redondear(saldoCapital * tasaMensual) + 
                                 ", Interés mes inicial: " + redondear(interesesMesInicial) + 
                                 ", Total interés: " + redondear(interes));
            }
            
            double capitalCuota = capitalFijo;
            double cuota = capitalCuota + interes;
            saldoCapital -= capitalCuota;
            
            if (i == plazo && Math.abs(saldoCapital) > 0.01) {
                capitalCuota += saldoCapital;
                cuota = capitalCuota + interes;
                saldoCapital = 0;
            }
            
            if (mesesGracia > 0) {
                LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(i + 1);
                LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                    Long.valueOf(fechaTemp.getMonthValue()), 
                    Long.valueOf(fechaTemp.getYear())
                );
                fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            } else {
                LocalDate fechaTemp = fechaInicio.toLocalDate().plusMonths(i);
                LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
                    Long.valueOf(fechaTemp.getMonthValue()), 
                    Long.valueOf(fechaTemp.getYear())
                );
                fechaVencimiento = ultimoDia.atTime(fechaInicio.toLocalTime());
            }
            
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(Double.valueOf(i));
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(redondear(capitalCuota));
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(cuota));
            detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
            detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
            detalle.setMora(0.0);
            detalle.setInteresVencido(0.0);
            detalle.setSaldoInteres(redondear(interes));
            detalle.setSaldoMora(0.0);
            detalle.setSaldoInteresVencido(0.0);
            detalle.setAbono(0.0);
            detalle.setCapitalPagado(0.0);
            detalle.setInteresPagado(0.0);
            detalle.setDesgravamen(0.0);
            detalle.setSaldoOtros(0.0);
            detalle.setDesgravamenFirmado(0.0);
            detalle.setDesgravamenDiferido(0.0);
            detalle.setDesgravamenOriginal(0.0);
            detalle.setValorDiferido(0.0);
            detalle.setEstado(Long.valueOf(Estado.ACTIVO));
            
            detalles.add(detalle);
        }
        
        return detalles;
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
        double saldoCapitalAnterior = prestamo.getMontoSolicitado(); // Para la primera cuota
        
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
                
                // SALDO DE CAPITAL (columna 3) → saldoInicialCapital
                Double saldoCapitalExcel = getCellValueAsDouble(row.getCell(3));
                Double saldoInicialCapital = saldoCapitalExcel != null ? saldoCapitalExcel : 0.0;
                
                // PAGO DE CAPITAL (columna 4) → capital
                Double pagoCapital = getCellValueAsDouble(row.getCell(4));
                detalle.setCapital(pagoCapital != null ? pagoCapital : 0.0);
                totalCapital += (pagoCapital != null ? pagoCapital : 0.0);
                
                // Calcular saldoCapital = saldoInicialCapital - capital
                Double saldoCapitalCalculado = saldoInicialCapital - (pagoCapital != null ? pagoCapital : 0.0);
                detalle.setSaldoCapital(redondear(saldoCapitalCalculado));
                detalle.setSaldo(redondear(saldoCapitalCalculado));
                
                // Establecer saldoInicialCapital
                detalle.setSaldoInicialCapital(redondear(saldoInicialCapital));
                
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
                
                // Actualizar el saldo para la siguiente iteración (usar el saldo calculado)
                saldoCapitalAnterior = saldoCapitalCalculado;
                
                // Inicializar campos restantes en cero
                detalle.setPrestamo(prestamo);
                detalle.setMora(0.0);
                detalle.setInteresVencido(0.0);
                detalle.setSaldoInteres(valorInteres != null ? valorInteres : 0.0);
                detalle.setSaldoMora(0.0);
                detalle.setSaldoInteresVencido(0.0);
                detalle.setAbono(0.0);
                detalle.setCapitalPagado(0.0);
                detalle.setInteresPagado(0.0);
                detalle.setDesgravamenDiferido(0.0);
                detalle.setValorDiferido(0.0);
                
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
            
            // Si el estado es PAGADO (4), crear registro de PagoPrestamo
            if (detalle.getEstado() != null && detalle.getEstado() == 4L) {
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
     * Mapea el estado de texto del Excel a código numérico.
     * @param cell Celda que contiene el estado
     * @return Código numérico del estado
     */
    private Long mapearEstadoTextoACodigo(Cell cell) {
        if (cell == null) {
            return 2L; // PENDIENTE por defecto
        }
        
        // Intentar leer como texto primero
        String estadoTexto = null;
        try {
            if (cell.getCellType() == CellType.STRING) {
                estadoTexto = cell.getStringCellValue().trim().toUpperCase();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                // Si es numérico, devolverlo directamente
                return (long) cell.getNumericCellValue();
            }
        } catch (Exception e) {
            System.err.println("Error al leer estado de la celda: " + e.getMessage());
            return 2L; // PENDIENTE por defecto
        }
        
        if (estadoTexto == null || estadoTexto.isEmpty()) {
            return 2L; // PENDIENTE por defecto
        }
        
        // Mapear texto a código
        switch (estadoTexto) {
            case "PAGADO":
            case "PAGADA":
                return 4L;
            case "EMITIDO":
            case "EMITIDA":
                return 5L;
            case "PENDIENTE":
                return 2L;
            default:
                System.out.println("Estado desconocido: " + estadoTexto + ", usando PENDIENTE (2)");
                return 2L;
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
	public Prestamo aplicarAbonoCapital(Long idPrestamo, Double valorAbono, Integer opcionRecalculo) throws Throwable {
		System.out.println("Aplicando abono a capital - Préstamo ID: " + idPrestamo + 
						 ", Abono: " + valorAbono + ", Opción: " + opcionRecalculo);
		
		// Validaciones básicas
		if (valorAbono == null || valorAbono <= 0) {
			throw new IncomeException("El valor del abono debe ser mayor a cero");
		}
		if (opcionRecalculo == null || (opcionRecalculo != 1 && opcionRecalculo != 2)) {
			throw new IncomeException("Opción de recálculo inválida. Use 1 para mantener plazo o 2 para reducir plazo");
		}
		
		// Obtener el préstamo
		Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
		if (prestamo == null) {
			throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
		}
		
		// Validar que tenga tipo de amortización
		if (prestamo.getTipoAmortizacion() == null) {
			throw new IncomeException("El préstamo no tiene definido el tipo de amortización");
		}
		
		// Obtener todos los detalles del préstamo ordenados por número de cuota
		List<DatosBusqueda> criterios = new ArrayList<>();
		DatosBusqueda criterio = new DatosBusqueda();
		criterio.setCampo("prestamo.codigo");
		criterio.setTipoComparacion(com.saa.rubros.TipoComandosBusqueda.IGUAL);
		criterio.setTipoDato(com.saa.rubros.TipoDatosBusqueda.LONG);
		criterio.setValor(idPrestamo.toString());
		criterios.add(criterio);
		
		List<DetallePrestamo> detalles = detallePrestamoDaoService.selectByCriteria(criterios, NombreEntidadesCredito.DETALLE_PRESTAMO);
		
		if (detalles == null || detalles.isEmpty()) {
			throw new IncomeException("El préstamo no tiene tabla de amortización generada");
		}
		
		// Ordenar por número de cuota
		detalles.sort((d1, d2) -> {
			if (d1.getNumeroCuota() == null) return -1;
			if (d2.getNumeroCuota() == null) return 1;
			return d1.getNumeroCuota().compareTo(d2.getNumeroCuota());
		});
		
		// Encontrar la última cuota pagada y la primera cuota pendiente
		DetallePrestamo ultimaCuotaPagada = null;
		DetallePrestamo primeraCuotaPendiente = null;
		
		for (DetallePrestamo detalle : detalles) {
			if (detalle.getEstado() != null && detalle.getEstado() == 4L) {
				// Estado PAGADO
				ultimaCuotaPagada = detalle;
			} else if (primeraCuotaPendiente == null && detalle.getEstado() != null && 
					  (detalle.getEstado() == 2L || detalle.getEstado() == 5L)) {
				// Estado PENDIENTE o EMITIDO
				primeraCuotaPendiente = detalle;
			}
		}
		
		if (primeraCuotaPendiente == null) {
			throw new IncomeException("No hay cuotas pendientes para aplicar el abono a capital");
		}
		
		// Calcular saldo actual de capital
		double saldoActual = primeraCuotaPendiente.getSaldoInicialCapital() != null ? 
						 primeraCuotaPendiente.getSaldoInicialCapital() : 0.0;
		
		System.out.println("Saldo actual de capital: " + saldoActual);
		
		// Validar que el abono no sea mayor al saldo
		if (valorAbono > saldoActual) {
			throw new IncomeException("El abono (" + valorAbono + ") no puede ser mayor al saldo de capital (" + saldoActual + ")");
		}
		
		// Aplicar el abono
		double nuevoSaldo = saldoActual - valorAbono;
		System.out.println("Nuevo saldo después del abono: " + nuevoSaldo);
		
		// Obtener cuotas pendientes (desde la primera pendiente en adelante)
		List<DetallePrestamo> cuotasPendientes = new ArrayList<>();
		boolean encontrada = false;
		for (DetallePrestamo detalle : detalles) {
			if (detalle.getCodigo().equals(primeraCuotaPendiente.getCodigo())) {
				encontrada = true;
			}
			if (encontrada) {
				cuotasPendientes.add(detalle);
			}
		}
		
		// Recalcular según la opción elegida
		List<DetallePrestamo> cuotasRecalculadas;
		
		if (opcionRecalculo == 1) {
			// Opción 1: Mantener plazo, reducir cuota
			cuotasRecalculadas = recalcularMantenPlazoCuotaMenor(
				prestamo, nuevoSaldo, cuotasPendientes, primeraCuotaPendiente.getFechaVencimiento()
			);
		} else {
			// Opción 2: Reducir plazo, mantener cuota
			cuotasRecalculadas = recalcularReducePlazoCuotaIgual(
				prestamo, nuevoSaldo, cuotasPendientes, primeraCuotaPendiente.getFechaVencimiento()
			);
		}
		
		// Eliminar las cuotas antiguas pendientes
		for (DetallePrestamo detalle : cuotasPendientes) {
			detallePrestamoDaoService.remove(detalle, detalle.getCodigo());
		}
		
		// Guardar las nuevas cuotas
		for (DetallePrestamo detalle : cuotasRecalculadas) {
			detallePrestamoDaoService.save(detalle, detalle.getCodigo());
		}
		
		// Obtener todos los detalles actualizados para recalcular totales
		List<DetallePrestamo> todosLosDetalles = detallePrestamoDaoService.selectByCriteria(criterios, NombreEntidadesCredito.DETALLE_PRESTAMO);
		
		// Actualizar campos del préstamo
		actualizarCamposPrestamo(prestamo, todosLosDetalles);
		prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
		
		System.out.println("Abono a capital aplicado exitosamente. Cuotas recalculadas: " + cuotasRecalculadas.size());
		return prestamo;
	}
	
	/**
	 * Recalcula la tabla manteniendo el plazo y reduciendo la cuota.
	 */
	private List<DetallePrestamo> recalcularMantenPlazoCuotaMenor(
			Prestamo prestamo, double nuevoSaldo, List<DetallePrestamo> cuotasOriginales, 
			LocalDateTime fechaInicioCuotas) throws Throwable {
		
		System.out.println("Recalculando: Mantener plazo (" + cuotasOriginales.size() + " cuotas), reducir cuota");
		
		List<DetallePrestamo> cuotasRecalculadas = new ArrayList<>();
		double tasaMensual = prestamo.getTasa() / 100 / 12;
		int cuotasRestantes = cuotasOriginales.size();
		
		// Calcular nueva cuota fija con el nuevo saldo
		double nuevaCuota = nuevoSaldo * (tasaMensual * Math.pow(1 + tasaMensual, cuotasRestantes)) / 
						   (Math.pow(1 + tasaMensual, cuotasRestantes) - 1);
		
		System.out.println("Nueva cuota calculada: " + redondear(nuevaCuota));
		
		double saldoCapital = nuevoSaldo;
		int numeroCuotaInicial = cuotasOriginales.get(0).getNumeroCuota() != null ? 
								cuotasOriginales.get(0).getNumeroCuota().intValue() : 1;
		
		for (int i = 0; i < cuotasRestantes; i++) {
			DetallePrestamo detalleOriginal = cuotasOriginales.get(i);
			DetallePrestamo detalle = new DetallePrestamo();
			
			// Calcular interés sobre saldo pendiente
			double interes = saldoCapital * tasaMensual;
			double capitalCuota = nuevaCuota - interes;
			saldoCapital -= capitalCuota;
			
			// Ajuste en la última cuota
			if (i == cuotasRestantes - 1 && Math.abs(saldoCapital) > 0.01) {
				capitalCuota += saldoCapital;
				saldoCapital = 0;
			}
			
			// Copiar datos del detalle original y actualizar valores recalculados
			detalle.setPrestamo(prestamo);
			detalle.setNumeroCuota(Double.valueOf(numeroCuotaInicial + i));
			detalle.setFechaVencimiento(detalleOriginal.getFechaVencimiento());
			detalle.setCapital(redondear(capitalCuota));
			detalle.setInteres(redondear(interes));
			detalle.setCuota(redondear(nuevaCuota));
			detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
			detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
			detalle.setSaldoInteres(redondear(interes));
			detalle.setEstado(detalleOriginal.getEstado());
			
			// Inicializar campos en cero
			detalle.setMora(0.0);
			detalle.setInteresVencido(0.0);
			detalle.setSaldoMora(0.0);
			detalle.setSaldoInteresVencido(0.0);
			detalle.setAbono(0.0);
			detalle.setCapitalPagado(0.0);
			detalle.setInteresPagado(0.0);
			detalle.setDesgravamen(0.0);
			detalle.setSaldoOtros(0.0);
			detalle.setDesgravamenFirmado(0.0);
			detalle.setDesgravamenDiferido(0.0);
			detalle.setDesgravamenOriginal(0.0);
			detalle.setValorDiferido(0.0);
			
			cuotasRecalculadas.add(detalle);
		}
		
		return cuotasRecalculadas;
	}
	
	/**
	 * Recalcula la tabla reduciendo el plazo y manteniendo la cuota.
	 */
	private List<DetallePrestamo> recalcularReducePlazoCuotaIgual(
			Prestamo prestamo, double nuevoSaldo, List<DetallePrestamo> cuotasOriginales,
			LocalDateTime fechaInicioCuotas) throws Throwable {
		
		System.out.println("Recalculando: Reducir plazo, mantener cuota");
		
		List<DetallePrestamo> cuotasRecalculadas = new ArrayList<>();
		double tasaMensual = prestamo.getTasa() / 100 / 12;
		
		// Obtener la cuota actual (de la primera cuota pendiente)
		double cuotaActual = cuotasOriginales.get(0).getCuota() != null ? 
							cuotasOriginales.get(0).getCuota() : 0.0;
		
		System.out.println("Cuota a mantener: " + cuotaActual);
		
		// Calcular nuevo plazo con la cuota actual
		// Fórmula: n = log(cuota / (cuota - saldo * i)) / log(1 + i)
		double numerador = Math.log(cuotaActual / (cuotaActual - nuevoSaldo * tasaMensual));
		double denominador = Math.log(1 + tasaMensual);
		int nuevoPlazo = (int) Math.ceil(numerador / denominador);
		
		System.out.println("Nuevo plazo calculado: " + nuevoPlazo + " cuotas (antes: " + cuotasOriginales.size() + ")");
		
		double saldoCapital = nuevoSaldo;
		int numeroCuotaInicial = cuotasOriginales.get(0).getNumeroCuota() != null ? 
								cuotasOriginales.get(0).getNumeroCuota().intValue() : 1;
		
		for (int i = 0; i < nuevoPlazo; i++) {
			DetallePrestamo detalle = new DetallePrestamo();
			
			// Calcular interés sobre saldo pendiente
			double interes = saldoCapital * tasaMensual;
			double capitalCuota = cuotaActual - interes;
			saldoCapital -= capitalCuota;
			
			// Ajuste en la última cuota
			if (i == nuevoPlazo - 1) {
				capitalCuota += saldoCapital;
				double cuotaFinal = capitalCuota + interes;
				saldoCapital = 0;
				
				detalle.setCuota(redondear(cuotaFinal));
			} else {
				detalle.setCuota(redondear(cuotaActual));
			}
			
			// Calcular fecha de vencimiento
			LocalDateTime fechaVencimiento;
			if (i < cuotasOriginales.size()) {
				// Usar la fecha de la cuota original si existe
				fechaVencimiento = cuotasOriginales.get(i).getFechaVencimiento();
			} else {
				// Calcular nueva fecha para cuotas adicionales (no debería pasar en reducir plazo)
				LocalDate fechaTemp = fechaInicioCuotas.toLocalDate().plusMonths(i);
				LocalDate ultimoDia = fechaService.ultimoDiaMesAnioLocal(
					Long.valueOf(fechaTemp.getMonthValue()), 
					Long.valueOf(fechaTemp.getYear())
				);
				fechaVencimiento = ultimoDia.atTime(fechaInicioCuotas.toLocalTime());
			}
			
			// Llenar el detalle
			detalle.setPrestamo(prestamo);
			detalle.setNumeroCuota(Double.valueOf(numeroCuotaInicial + i));
			detalle.setFechaVencimiento(fechaVencimiento);
			detalle.setCapital(redondear(capitalCuota));
			detalle.setInteres(redondear(interes));
			detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
			detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
			detalle.setSaldoInteres(redondear(interes));
			detalle.setEstado(Long.valueOf(Estado.ACTIVO));
			
			// Inicializar campos en cero
			detalle.setMora(0.0);
			detalle.setInteresVencido(0.0);
			detalle.setSaldoMora(0.0);
			detalle.setSaldoInteresVencido(0.0);
			detalle.setAbono(0.0);
			detalle.setCapitalPagado(0.0);
			detalle.setInteresPagado(0.0);
			detalle.setDesgravamen(0.0);
			detalle.setSaldoOtros(0.0);
			detalle.setDesgravamenFirmado(0.0);
			detalle.setDesgravamenDiferido(0.0);
			detalle.setDesgravamenOriginal(0.0);
			detalle.setValorDiferido(0.0);
			
			cuotasRecalculadas.add(detalle);
		}
		
		return cuotasRecalculadas;
	}
}
