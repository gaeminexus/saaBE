package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.AdjuntoDaoService;
import com.saa.ejb.credito.service.AdjuntoService;
import com.saa.model.credito.Adjunto;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class AdjuntoServiceImpl implements AdjuntoService {

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @Override
    public Adjunto selectById(Long id) throws Throwable {
        System.out.println("selectById - Adjunto id: " + id);
        return adjuntoDaoService.selectById(id, NombreEntidadesCredito.ADJUNTO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove[] - Adjunto");
        Adjunto entidad = new Adjunto();
        for (Long id : ids) {
            adjuntoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<Adjunto> lista) throws Throwable {
        System.out.println("save[] - Adjunto");
        for (Adjunto registro : lista) {
            adjuntoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Adjunto> selectAll() throws Throwable {
        System.out.println("selectAll - Adjunto");
        List<Adjunto> result =
                adjuntoDaoService.selectAll(NombreEntidadesCredito.ADJUNTO);

        if (result.isEmpty())
            throw new IncomeException("No existen registros en Adjunto");

        return result;
    }

    @Override
    public Adjunto saveSingle(Adjunto entidad) throws Throwable {
        System.out.println("saveSingle - Adjunto");
        if (entidad.getCodigo() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        return adjuntoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public List<Adjunto> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Adjunto");
        List<Adjunto> result =
                adjuntoDaoService.selectByCriteria(datos, NombreEntidadesCredito.ADJUNTO);

        if (result.isEmpty())
            throw new IncomeException("Adjunto - La búsqueda no devolvió registros");

        return result;
    }
}
