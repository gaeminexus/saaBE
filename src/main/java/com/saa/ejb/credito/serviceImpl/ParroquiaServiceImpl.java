package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.credito.dao.ParroquiaDaoService;
import com.saa.ejb.credito.service.ParroquiaService;
import com.saa.model.credito.Parroquia;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ParroquiaServiceImpl implements ParroquiaService {

    @EJB
    private ParroquiaDaoService parroquiaDaoService;

    @Override
    public Parroquia selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Parroquia con id: " + id);
        return parroquiaDaoService.selectById(id, NombreEntidadesCredito.PARROQUIA);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] ParroquiaService");
        Parroquia parroquia = new Parroquia();
        for (Long registro : id) {
            parroquiaDaoService.remove(parroquia, registro);
        }
    }

    @Override
    public void save(List<Parroquia> lista) throws Throwable {
        System.out.println("Ingresa al metodo save ParroquiaService");
        for (Parroquia registro : lista) {
            parroquiaDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Parroquia> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ParroquiaService");
        List<Parroquia> result = parroquiaDaoService.selectAll(NombreEntidadesCredito.PARROQUIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Parroquia no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public Parroquia saveSingle(Parroquia parroquia) throws Throwable {
        System.out.println("saveSingle - Parroquia");
        if (parroquia.getCodigo() == null) {
            parroquia.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        parroquia = parroquiaDaoService.save(parroquia, parroquia.getCodigo());
        return parroquia;
    }

    @Override
    public List<Parroquia> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa a selectByCriteria ParroquiaService");
        List<Parroquia> result = parroquiaDaoService.selectByCriteria(datos, NombreEntidadesCredito.PARROQUIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Parroquia no devolvió ningún registro");
        }
        return result;
    }
}
