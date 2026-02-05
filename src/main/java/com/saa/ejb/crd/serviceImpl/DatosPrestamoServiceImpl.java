package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DatosPrestamoDaoService;
import com.saa.ejb.crd.service.DatosPrestamoService;
import com.saa.model.crd.DatosPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DatosPrestamoServiceImpl implements DatosPrestamoService {

    @EJB
    private DatosPrestamoDaoService datosPrestamoDaoService;

    @Override
    public DatosPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - DatosPrestamo id: " + id);
        return datosPrestamoDaoService.selectById(id, NombreEntidadesCredito.DATOS_PRESTAMO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove[] - DatosPrestamo");
        DatosPrestamo entidad = new DatosPrestamo();
        for (Long id : ids) {
            datosPrestamoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<DatosPrestamo> lista) throws Throwable {
        System.out.println("save[] - DatosPrestamo");
        for (DatosPrestamo registro : lista) {
            datosPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DatosPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - DatosPrestamo");
        List<DatosPrestamo> result =
                datosPrestamoDaoService.selectAll(NombreEntidadesCredito.DATOS_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("No existen registros en DatosPrestamo");

        return result;
    }

    @Override
    public DatosPrestamo saveSingle(DatosPrestamo entidad) throws Throwable {
        System.out.println("saveSingle - DatosPrestamo");
        if (entidad.getCodigo() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return datosPrestamoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<DatosPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - DatosPrestamo");
        List<DatosPrestamo> result =
                datosPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DATOS_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("DatosPrestamo - La búsqueda no devolvió registros");

        return result;
    }
}
