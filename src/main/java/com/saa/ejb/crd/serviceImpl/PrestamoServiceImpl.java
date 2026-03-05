package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
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

    /**
     * Recupera un registro de Prestamo por su ID.
     */
    @Override
    public Prestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de Prestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PrestamoService ... depurado");
        Prestamo prestamo = new Prestamo();
        for (Long registro : id) {
            prestamoDaoService.remove(prestamo, registro);
        }
    }

    /**
     * Guarda una lista de registros de Prestamo.
     */
    @Override
    public void save(List<Prestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PrestamoService");
        for (Prestamo registro : lista) {
            prestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Prestamo.
     */
    @Override
    public List<Prestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Prestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Prestamo.
     */
    @Override
    public Prestamo saveSingle(Prestamo prestamo) throws Throwable {
        System.out.println("saveSingle - Prestamo");
        if(prestamo.getCodigo() == null){
        	prestamo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        prestamo = prestamoDaoService.save(prestamo, prestamo.getCodigo());
        return prestamo;
    }

    /**
     * Recupera registros de Prestamo segun criterios de búsqueda.
     */
    @Override
    public List<Prestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PrestamoService");
        List<Prestamo> result = prestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Prestamo no devolvio ningun registro");
        }
        return result;
    }
    
    /**
     * Genera la tabla de amortización para un préstamo según el tipo (Francesa o Alemana).
     */
    @Override
    public Prestamo generarTablaAmortizacion(Long idPrestamo) throws Throwable {
        System.out.println("Generando tabla de amortización para préstamo ID: " + idPrestamo);
        
        // Obtener el préstamo
        Prestamo prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        if (prestamo == null) {
            throw new IncomeException("Préstamo con ID " + idPrestamo + " no encontrado");
        }
        
        // Validar datos necesarios
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
        
        // Generar tabla según tipo de amortización
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        if (prestamo.getTipoAmortizacion() == 1) {
            // Sistema Francés (cuota fija)
            detalles = generarAmortizacionFrancesa(prestamo);
        } else if (prestamo.getTipoAmortizacion() == 2) {
            // Sistema Alemán (capital fijo)
            detalles = generarAmortizacionAlemana(prestamo);
        } else {
            throw new IncomeException("Tipo de amortización no válido. Use 1 para Francesa o 2 para Alemana");
        }
        
        // Guardar todos los detalles en la base de datos
        for (DetallePrestamo detalle : detalles) {
            detallePrestamoDaoService.save(detalle, detalle.getCodigo());
        }
        
        System.out.println("Tabla de amortización generada exitosamente con " + detalles.size() + " cuotas");
        return prestamo;
    }
    
    /**
     * Genera tabla de amortización con sistema francés (cuota fija).
     */
    private List<DetallePrestamo> generarAmortizacionFrancesa(Prestamo prestamo) {
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        double capital = prestamo.getMontoSolicitado();
        double tasaMensual = prestamo.getTasa() / 100 / 12; // Convertir tasa anual a mensual
        int plazo = prestamo.getPlazo().intValue();
        
        // Calcular cuota fija usando fórmula de amortización francesa
        double cuotaFija = capital * (tasaMensual * Math.pow(1 + tasaMensual, plazo)) / 
                          (Math.pow(1 + tasaMensual, plazo) - 1);
        
        double saldoCapital = capital;
        LocalDateTime fechaVencimiento = prestamo.getFechaInicio();
        
        for (int i = 1; i <= plazo; i++) {
            DetallePrestamo detalle = new DetallePrestamo();
            
            // Calcular interés sobre saldo pendiente
            double interes = saldoCapital * tasaMensual;
            
            // Capital es la diferencia entre cuota e interés
            double capitalCuota = cuotaFija - interes;
            
            // Actualizar saldo
            saldoCapital -= capitalCuota;
            
            // Si es la última cuota, ajustar el capital para cerrar el saldo
            if (i == plazo && Math.abs(saldoCapital) > 0.01) {
                capitalCuota += saldoCapital;
                saldoCapital = 0;
            }
            
            // Calcular fecha de vencimiento (sumar un mes)
            fechaVencimiento = fechaVencimiento.plusMonths(1);
            
            // Llenar el detalle
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(Double.valueOf(i));
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(redondear(capitalCuota));
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(cuotaFija));
            detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
            detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
            
            // Inicializar campos en cero
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
    
    /**
     * Genera tabla de amortización con sistema alemán (capital fijo).
     */
    private List<DetallePrestamo> generarAmortizacionAlemana(Prestamo prestamo) {
        List<DetallePrestamo> detalles = new ArrayList<>();
        
        double capital = prestamo.getMontoSolicitado();
        double tasaMensual = prestamo.getTasa() / 100 / 12; // Convertir tasa anual a mensual
        int plazo = prestamo.getPlazo().intValue();
        
        // Capital fijo por cuota
        double capitalFijo = capital / plazo;
        
        double saldoCapital = capital;
        LocalDateTime fechaVencimiento = prestamo.getFechaInicio();
        
        for (int i = 1; i <= plazo; i++) {
            DetallePrestamo detalle = new DetallePrestamo();
            
            // Calcular interés sobre saldo pendiente
            double interes = saldoCapital * tasaMensual;
            
            // Capital es fijo
            double capitalCuota = capitalFijo;
            
            // Cuota total es capital + interés
            double cuota = capitalCuota + interes;
            
            // Actualizar saldo
            saldoCapital -= capitalCuota;
            
            // Si es la última cuota, ajustar para cerrar el saldo
            if (i == plazo && Math.abs(saldoCapital) > 0.01) {
                capitalCuota += saldoCapital;
                cuota = capitalCuota + interes;
                saldoCapital = 0;
            }
            
            // Calcular fecha de vencimiento (sumar un mes)
            fechaVencimiento = fechaVencimiento.plusMonths(1);
            
            // Llenar el detalle
            detalle.setPrestamo(prestamo);
            detalle.setNumeroCuota(Double.valueOf(i));
            detalle.setFechaVencimiento(fechaVencimiento);
            detalle.setCapital(redondear(capitalCuota));
            detalle.setInteres(redondear(interes));
            detalle.setCuota(redondear(cuota));
            detalle.setSaldoCapital(redondear(Math.max(0, saldoCapital)));
            detalle.setSaldo(redondear(Math.max(0, saldoCapital)));
            
            // Inicializar campos en cero
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
    
    /**
     * Redondea un valor a 2 decimales.
     */
    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
