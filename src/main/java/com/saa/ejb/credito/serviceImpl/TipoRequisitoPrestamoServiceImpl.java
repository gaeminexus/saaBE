package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.TipoRequisitoPrestamoDaoService;
import com.saa.ejb.credito.service.TipoRequisitoPrestamoService;
import com.saa.model.credito.TipoRequisitoPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class TipoRequisitoPrestamoServiceImpl implements TipoRequisitoPrestamoService {

    @EJB
    private TipoRequisitoPrestamoDaoService tipoRequisitoPrestamoDaoService;

    /**
     * Recupera un registro de TipoRequisitoPrestamo por su ID.
     */
    @Override
    public TipoRequisitoPrestamo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return tipoRequisitoPrestamoDaoService.selectById(id, NombreEntidadesCredito.TIPO_REQUISITO_PRESTAMO);
    }

    /**
     * Elimina uno o varios registros.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de TipoRequisitoPrestamoService ... depurado");
        TipoRequisitoPrestamo requisito = new TipoRequisitoPrestamo();
        for (Long registro : id) {
            tipoRequisitoPrestamoDaoService.remove(requisito, registro);
        }
    }

    /**
     * Guarda una lista de registros.
     */
    @Override
    public void save(List<TipoRequisitoPrestamo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de TipoRequisitoPrestamoService");
        for (TipoRequisitoPrestamo registro : lista) {
            tipoRequisitoPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros.
     */
    @Override
    public List<TipoRequisitoPrestamo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll TipoRequisitoPrestamoService");
        List<TipoRequisitoPrestamo> result =
                tipoRequisitoPrestamoDaoService.selectAll(NombreEntidadesCredito.TIPO_REQUISITO_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total TipoRequisitoPrestamo no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro.
     */
    @Override
    public TipoRequisitoPrestamo saveSingle(TipoRequisitoPrestamo requisito) throws Throwable {
        System.out.println("saveSingle - TipoRequisitoPrestamo");
        requisito = tipoRequisitoPrestamoDaoService.save(requisito, requisito.getCodigo());
        return requisito;
    }

    /**
     * Recupera registros segun criterios de búsqueda.
     */
    @Override
    public List<TipoRequisitoPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria TipoRequisitoPrestamoService");
        List<TipoRequisitoPrestamo> result =
                tipoRequisitoPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.TIPO_REQUISITO_PRESTAMO);

        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio TipoRequisitoPrestamo no devolvio ningun registro");
        }
        return result;
    }
}
