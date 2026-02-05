package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.RequisitosPrestamoDaoService;
import com.saa.ejb.crd.service.RequisitosPrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.RequisitosPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class RequisitosPrestamoServiceImpl implements RequisitosPrestamoService {

    @EJB
    private RequisitosPrestamoDaoService requisitosPrestamoDaoService;

    @Override
    public RequisitosPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - RequisitosPrestamo id: " + id);
        return requisitosPrestamoDaoService.selectById(id, NombreEntidadesCredito.REQUISITOS_PRESTAMO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove[] - RequisitosPrestamo");
        RequisitosPrestamo entidad = new RequisitosPrestamo();
        for (Long id : ids) {
            requisitosPrestamoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<RequisitosPrestamo> lista) throws Throwable {
        System.out.println("save[] - RequisitosPrestamo");
        for (RequisitosPrestamo registro : lista) {
            requisitosPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<RequisitosPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - RequisitosPrestamo");
        List<RequisitosPrestamo> result =
                requisitosPrestamoDaoService.selectAll(NombreEntidadesCredito.REQUISITOS_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("No existen registros en RequisitosPrestamo");

        return result;
    }

    @Override
    public RequisitosPrestamo saveSingle(RequisitosPrestamo entidad) throws Throwable {
        System.out.println("saveSingle - RequisitosPrestamo");
        if (entidad.getCodigo() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return requisitosPrestamoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<RequisitosPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - RequisitosPrestamo");
        List<RequisitosPrestamo> result =
                requisitosPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.REQUISITOS_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("RequisitosPrestamo - La búsqueda no devolvió registros");

        return result;
    }
}
