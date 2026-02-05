package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.RelacionPrestamoDaoService;
import com.saa.ejb.crd.service.RelacionPrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.RelacionPrestamo;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class RelacionPrestamoServiceImpl implements RelacionPrestamoService {

    @EJB
    private RelacionPrestamoDaoService relacionPrestamoDaoService;

    @Override
    public RelacionPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - RelacionPrestamo id: " + id);
        return relacionPrestamoDaoService.selectById(id, NombreEntidadesCredito.RELACION_PRESTAMO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove[] - RelacionPrestamo");
        RelacionPrestamo entidad = new RelacionPrestamo();
        for (Long id : ids) {
            relacionPrestamoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<RelacionPrestamo> lista) throws Throwable {
        System.out.println("save[] - RelacionPrestamo");
        for (RelacionPrestamo registro : lista) {
            relacionPrestamoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<RelacionPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - RelacionPrestamo");
        List<RelacionPrestamo> result =
                relacionPrestamoDaoService.selectAll(NombreEntidadesCredito.RELACION_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("No existen registros en RelacionPrestamo");

        return result;
    }

    @Override
    public RelacionPrestamo saveSingle(RelacionPrestamo entidad) throws Throwable {
        System.out.println("saveSingle - RelacionPrestamo");
        if (entidad.getCodigo() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return relacionPrestamoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<RelacionPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - RelacionPrestamo");
        List<RelacionPrestamo> result =
                relacionPrestamoDaoService.selectByCriteria(datos, NombreEntidadesCredito.RELACION_PRESTAMO);

        if (result.isEmpty())
            throw new IncomeException("RelacionPrestamo - La búsqueda no devolvió registros");

        return result;
    }
}
