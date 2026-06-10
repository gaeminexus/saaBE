package com.saa.ejb.rpr.serviceImpl;

import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoCuotasPrestamosMensualService;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoCuotasPrestamosMensualServiceImpl implements CreditoCuotasPrestamosMensualService {

    @EJB
    private CreditoCuotasPrestamosMensualDaoService creditoCuotasPrestamosMensualDaoService;

    @Override
    public CreditoCuotasPrestamosMensual selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CreditoCuotasPrestamosMensual con id: " + id);
        return creditoCuotasPrestamosMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CreditoCuotasPrestamosMensualService");
        CreditoCuotasPrestamosMensual entidad = new CreditoCuotasPrestamosMensual();
        for (Long registro : id) {
            creditoCuotasPrestamosMensualDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CreditoCuotasPrestamosMensual> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CreditoCuotasPrestamosMensualService");
        for (CreditoCuotasPrestamosMensual registro : lista) {
            creditoCuotasPrestamosMensualDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CreditoCuotasPrestamosMensualService");
        List<CreditoCuotasPrestamosMensual> result = creditoCuotasPrestamosMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoCuotasPrestamosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CreditoCuotasPrestamosMensual saveSingle(CreditoCuotasPrestamosMensual entidad) throws Throwable {
        System.out.println("saveSingle - CreditoCuotasPrestamosMensual");
        entidad = creditoCuotasPrestamosMensualDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CreditoCuotasPrestamosMensualService");
        List<CreditoCuotasPrestamosMensual> result = creditoCuotasPrestamosMensualDaoService.selectByCriteria(datos, NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoCuotasPrestamosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        return creditoCuotasPrestamosMensualDaoService.selectByEjecucion(codigoEjecucion);
    }

    @Override
    public CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable {
        return creditoCuotasPrestamosMensualDaoService.selectByEjecucionYOperacion(codigoEjecucion, numeroOperacion);
    }
}
