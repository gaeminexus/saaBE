package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.PagoPrestamoService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PagoPrestamoServiceImpl implements PagoPrestamoService {

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    /**
     * Recupera un registro de PagoPrestamo por su ID.
     */
    @Override
    public PagoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return pagoPrestamoDaoService.selectById(id, NombreEntidadesCredito.PAGO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de PagoPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PagoPrestamoService ... depurado");
        PagoPrestamo pago = new PagoPrestamo();
        for (Long registro : id) {
            pagoPrestamoDaoService.remove(pago, registro);
        }
    }

    /**
     * Guarda una lista de registros de PagoPrestamo.
     */
    @Override
    public void save(List<PagoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PagoPrestamoService");
        for (PagoPrestamo registro : lista) {
            pagoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de PagoPrestamo.
     */
    @Override
    public List<PagoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PagoPrestamoService");
        List<PagoPrestamo> result = pagoPrestamoDaoService.selectAll(NombreEntidadesCredito.PAGO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PagoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de PagoPrestamo.
     */
    @Override
    public PagoPrestamo saveSingle(PagoPrestamo pago) throws Throwable {
        System.out.println("saveSingle - PagoPrestamo");
        if(pago.getCodigo() == null){
        	pago.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        pago = pagoPrestamoDaoService.save(pago, pago.getCodigo());
        return pago;
    }

    /**
     * Recupera registros de PagoPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<PagoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PagoPrestamoService");
        List<PagoPrestamo> result = pagoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.PAGO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio PagoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public double calcularSaldoCapitalPendiente(List<DetallePrestamo> cuotas) throws Throwable {
        double saldo = 0.0;
        if (cuotas != null) {
            for (DetallePrestamo cuota : cuotas) {
                double capitalOriginal = cuota.getCapital() != null ? cuota.getCapital() : 0.0;
                double capitalPagadoVigente = 0.0;
                List<PagoPrestamo> pagos = pagoPrestamoDaoService.selectVigentesByIdDetallePrestamo(cuota.getCodigo());
                if (pagos != null) {
                    for (PagoPrestamo pago : pagos) {
                        capitalPagadoVigente += pago.getCapitalPagado() != null ? pago.getCapitalPagado() : 0.0;
                    }
                }
                saldo += Math.max(0.0, capitalOriginal - capitalPagadoVigente);
            }
        }
        return BigDecimal.valueOf(saldo).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
