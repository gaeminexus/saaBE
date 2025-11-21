package com.saa.ejb.credito.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;

import com.saa.ejb.credito.dao.PagoAporteDaoService;
import com.saa.ejb.credito.service.PagoAporteService;

import com.saa.model.credito.PagoAporte;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class PagoAporteServiceImpl implements PagoAporteService {

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @Override
    public PagoAporte selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById PagoAporte con id: " + id);
        return pagoAporteDaoService.selectById(id, NombreEntidadesCredito.PAGO_APORTE);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de PagoAporteService");
        PagoAporte pa = new PagoAporte();
        for (Long registro : ids) {
            pagoAporteDaoService.remove(pa, registro);
        }
    }

    @Override
    public void save(List<PagoAporte> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de PagoAporteService");
        for (PagoAporte registro : lista) {
            pagoAporteDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<PagoAporte> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll PagoAporteService");
        List<PagoAporte> result = pagoAporteDaoService.selectAll(NombreEntidadesCredito.PAGO_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total PagoAporte no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public PagoAporte saveSingle(PagoAporte pa) throws Throwable {
        System.out.println("saveSingle - PagoAporte");
        if (pa.getCodigo() == null) {
            pa.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        pa = pagoAporteDaoService.save(pa, pa.getCodigo());
        return pa;
    }

    @Override
    public List<PagoAporte> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria PagoAporteService");
        List<PagoAporte> result = pagoAporteDaoService.selectByCriteria(datos, NombreEntidadesCredito.PAGO_APORTE);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio PagoAporte no devolvió registros");
        }
        return result;
    }
}
