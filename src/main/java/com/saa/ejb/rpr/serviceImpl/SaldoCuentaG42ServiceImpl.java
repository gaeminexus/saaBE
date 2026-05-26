package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.SaldoCuentaG42DaoService;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.model.rpr.SaldoCuentaG42;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class SaldoCuentaG42ServiceImpl implements SaldoCuentaG42Service {

    @EJB
    private SaldoCuentaG42DaoService saldoCuentaG42DaoService;

    @Override
    public SaldoCuentaG42 selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById SaldoCuentaG42 con id: " + id);
        return saldoCuentaG42DaoService.selectById(id, NombreEntidadesReporte.SALDO_CUENTA_G42);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de SaldoCuentaG42Service");
        SaldoCuentaG42 entidad = new SaldoCuentaG42();
        for (Long registro : id) {
            saldoCuentaG42DaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<SaldoCuentaG42> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de SaldoCuentaG42Service");
        for (SaldoCuentaG42 registro : lista) {
            saldoCuentaG42DaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<SaldoCuentaG42> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll SaldoCuentaG42Service");
        List<SaldoCuentaG42> result = saldoCuentaG42DaoService.selectAll(NombreEntidadesReporte.SALDO_CUENTA_G42);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total SaldoCuentaG42 no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public SaldoCuentaG42 saveSingle(SaldoCuentaG42 entidad) throws Throwable {
        System.out.println("saveSingle - SaldoCuentaG42");
        entidad = saldoCuentaG42DaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<SaldoCuentaG42> selectByDetalle(Long codigoDetalle) throws Throwable {
        System.out.println("selectByDetalle SaldoCuentaG42 codigoDetalle: " + codigoDetalle);
        return saldoCuentaG42DaoService.selectByDetalle(codigoDetalle);
    }

    @Override
    public List<SaldoCuentaG42> selectCesantesDesdeG42Previo(Long codigoDetallePrevio, Long codigoDetalleActual) throws Throwable {
        System.out.println("selectCesantesDesdeG42Previo previo: " + codigoDetallePrevio + " actual: " + codigoDetalleActual);
        return saldoCuentaG42DaoService.selectCesantesDesdeG42Previo(codigoDetallePrevio, codigoDetalleActual);
    }

    @Override
    public List<SaldoCuentaG42> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria SaldoCuentaG42Service");
        List<SaldoCuentaG42> result = saldoCuentaG42DaoService.selectByCriteria(datos, NombreEntidadesReporte.SALDO_CUENTA_G42);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio SaldoCuentaG42 no devolvio ningun registro");
        }
        return result;
    }
}
