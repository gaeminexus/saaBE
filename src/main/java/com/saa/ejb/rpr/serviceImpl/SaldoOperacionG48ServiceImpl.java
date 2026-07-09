package com.saa.ejb.rpr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.SaldoOperacionG48DaoService;
import com.saa.ejb.rpr.service.SaldoOperacionG48Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.SaldoOperacionG48;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class SaldoOperacionG48ServiceImpl implements SaldoOperacionG48Service {

    @EJB
    private SaldoOperacionG48DaoService saldoOperacionG48DaoService;

    @Override
    public SaldoOperacionG48 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById SaldoOperacionG48 con id: " + id);
        return saldoOperacionG48DaoService.selectById(id, NombreEntidadesReporte.SALDO_OPERACION_G48);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de SaldoOperacionG48Service");
        SaldoOperacionG48 entidad = new SaldoOperacionG48();
        for (Long registro : id) {
            saldoOperacionG48DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<SaldoOperacionG48> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de SaldoOperacionG48Service");
        for (SaldoOperacionG48 registro : lista) {
            saldoOperacionG48DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<SaldoOperacionG48> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll SaldoOperacionG48Service");
        List<SaldoOperacionG48> result = saldoOperacionG48DaoService.selectAll(NombreEntidadesReporte.SALDO_OPERACION_G48);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total SaldoOperacionG48 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public SaldoOperacionG48 saveSingle(SaldoOperacionG48 entidad) throws Throwable {
        System.out.println("saveSingle - SaldoOperacionG48");
        entidad = saldoOperacionG48DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<SaldoOperacionG48> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria SaldoOperacionG48Service");
        List<SaldoOperacionG48> result = saldoOperacionG48DaoService.selectByCriteria(datos, NombreEntidadesReporte.SALDO_OPERACION_G48);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio SaldoOperacionG48 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<SaldoOperacionG48> selectByDetalle(Long codigoDetalle) throws Throwable {
        return saldoOperacionG48DaoService.selectByDetalle(codigoDetalle);
    }

    @Override
    public SaldoOperacionG48 selectByDetalleYOperacion(Long codigoDetalle, String numeroOperacion) throws Throwable {
        return saldoOperacionG48DaoService.selectByDetalleYOperacion(codigoDetalle, numeroOperacion);
    }
}
