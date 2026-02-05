package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.HistorialSueldoDaoService;
import com.saa.ejb.crd.service.HistorialSueldoService;
import com.saa.model.crd.HistorialSueldo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistorialSueldoServiceImpl implements HistorialSueldoService {

    @EJB
    private HistorialSueldoDaoService historialDaoService;

    @Override
    public HistorialSueldo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById HistorialSueldo con id: " + id);
        return historialDaoService.selectById(id, NombreEntidadesCredito.HISTORIAL_SUELDO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] HistorialSueldoService");
        HistorialSueldo hs = new HistorialSueldo();
        for (Long registro : id) {
            historialDaoService.remove(hs, registro);
        }
    }

    @Override
    public void save(List<HistorialSueldo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save HistorialSueldoService");
        for (HistorialSueldo registro : lista) {
            historialDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<HistorialSueldo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll HistorialSueldoService");
        List<HistorialSueldo> result = historialDaoService.selectAll(NombreEntidadesCredito.HISTORIAL_SUELDO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total HistorialSueldo no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public HistorialSueldo saveSingle(HistorialSueldo hs) throws Throwable {
        System.out.println("saveSingle - HistorialSueldo");
        if (hs.getCodigo() == null) {
            hs.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        hs = historialDaoService.save(hs, hs.getCodigo());
        return hs;
    }

    @Override
    public List<HistorialSueldo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa a selectByCriteria HistorialSueldoService");
        List<HistorialSueldo> result = historialDaoService.selectByCriteria(datos, NombreEntidadesCredito.HISTORIAL_SUELDO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio HistorialSueldo no devolvió ningún registro");
        }
        return result;
    }
}
