package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.DetallePrestamoDaoService;
import com.saa.ejb.credito.service.DetallePrestamoService;
import com.saa.model.credito.DetallePrestamo;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DetallePrestamoServiceImpl implements DetallePrestamoService {

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    /**
     * Recupera un registro de DetallePrestamo por su ID.
     */
    @Override
    public DetallePrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return detallePrestamoDaoService.selectById(id, NombreEntidadesCredito.DETALLE_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de DetallePrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DetallePrestamoService ... depurado");
        DetallePrestamo detalle = new DetallePrestamo();
        for (Long registro : id) {
            detallePrestamoDaoService.remove(detalle, registro);
        }
    }

    /**
     * Guarda una lista de registros de DetallePrestamo.
     */
    @Override
    public void save(List<DetallePrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DetallePrestamoService");
        for (DetallePrestamo registro : lista) {
            detallePrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de DetallePrestamo.
     */
    @Override
    public List<DetallePrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DetallePrestamoService");
        List<DetallePrestamo> result = detallePrestamoDaoService.selectAll(NombreEntidadesCredito.DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DetallePrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de DetallePrestamo.
     */
    @Override
    public DetallePrestamo saveSingle(DetallePrestamo detalle) throws Throwable {
        System.out.println("saveSingle - DetallePrestamo");
        if(detalle.getCodigo() == null){
        	detalle.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        detalle = detallePrestamoDaoService.save(detalle, detalle.getCodigo());
        return detalle;
    }

    /**
     * Recupera registros de DetallePrestamo segun criterios de búsqueda.
     */
    @Override
    public List<DetallePrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DetallePrestamoService");
        List<DetallePrestamo> result = detallePrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DETALLE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DetallePrestamo no devolvio ningun registro");
        }
        return result;
    }
}
