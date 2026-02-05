package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoPrestamoDaoService;
import com.saa.ejb.credito.service.TipoPrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoPrestamoServiceImpl implements TipoPrestamoService {

    @EJB
    private TipoPrestamoDaoService tipoPrestamoDaoService;

    /**
     * Recupera un registro de TipoPrestamo por su ID.
     */
    @Override
    public TipoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return tipoPrestamoDaoService.selectById(id, NombreEntidadesCredito.TIPO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros de TipoPrestamo.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoPrestamoService ... depurado");
        TipoPrestamo tipoPrestamo = new TipoPrestamo();
        for (Long registro : id) {
            tipoPrestamoDaoService.remove(tipoPrestamo, registro);
        }
    }

    /**
     * Guarda una lista de registros de TipoPrestamo.
     */
    @Override
    public void save(List<TipoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoPrestamoService");
        for (TipoPrestamo registro : lista) {
            tipoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de TipoPrestamo.
     */
    @Override
    public List<TipoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoPrestamoService");
        List<TipoPrestamo> result = tipoPrestamoDaoService.selectAll(NombreEntidadesCredito.TIPO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de TipoPrestamo.
     */
    @Override
    public TipoPrestamo saveSingle(TipoPrestamo tipoPrestamo) throws Throwable {
        System.out.println("saveSingle - TipoPrestamo");
        if(tipoPrestamo.getCodigo() == null){
        	tipoPrestamo.setEstado(Long.valueOf(Estado.ACTIVO)); //Activo
		}
        tipoPrestamo = tipoPrestamoDaoService.save(tipoPrestamo, tipoPrestamo.getCodigo());
        return tipoPrestamo;
    }

    /**
     * Recupera registros de TipoPrestamo segun criterios de búsqueda.
     */
    @Override
    public List<TipoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoPrestamoService");
        List<TipoPrestamo> result = tipoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
