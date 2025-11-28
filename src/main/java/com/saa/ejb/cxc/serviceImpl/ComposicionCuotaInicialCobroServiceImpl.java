package com.saa.ejb.cxc.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.dao.ComposicionCuotaInicialCobroDaoService;
import com.saa.ejb.cxc.service.ComposicionCuotaInicialCobroService;
import com.saa.model.cxc.ComposicionCuotaInicialCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ComposicionCuotaInicialCobroServiceImpl implements ComposicionCuotaInicialCobroService {

    @EJB
    private ComposicionCuotaInicialCobroDaoService ComposicionCuotaInicialCobroDao;

    @Override
    public ComposicionCuotaInicialCobro selectById(Long id) throws Throwable {
        System.out.println("selectById - ComposicionCuotaInicialCobro id: " + id);
        return ComposicionCuotaInicialCobroDao.selectById(id, NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove[] - ComposicionCuotaInicialCobro");
        ComposicionCuotaInicialCobro entidad = new ComposicionCuotaInicialCobro();
        for (Long id : ids) {
            ComposicionCuotaInicialCobroDao.remove(entidad, id);
        }
    }

    @Override
    public void save(List<ComposicionCuotaInicialCobro> lista) throws Throwable {
        System.out.println("save[] - ComposicionCuotaInicialCobro");
        for (ComposicionCuotaInicialCobro registro : lista) {
            ComposicionCuotaInicialCobroDao.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ComposicionCuotaInicialCobro> selectAll() throws Throwable {
        System.out.println("selectAll - ComposicionCuotaInicialCobro");

        List<ComposicionCuotaInicialCobro> result =
                ComposicionCuotaInicialCobroDao.selectAll(NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);

        if (result.isEmpty())
            throw new IncomeException("No existen registros en ComposicionCuotaInicialCobro");

        return result;
    }

    @Override
    public ComposicionCuotaInicialCobro saveSingle(ComposicionCuotaInicialCobro entidad) throws Throwable {
        System.out.println("saveSingle - ComposicionCuotaInicialCobro");
        entidad = ComposicionCuotaInicialCobroDao.save(entidad, entidad.getCodigo());
        return entidad;
    }


    @Override
    public List<ComposicionCuotaInicialCobro> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - ComposicionCuotaInicialCobro");

        List<ComposicionCuotaInicialCobro> result =
                ComposicionCuotaInicialCobroDao.selectByCriteria(datos, NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);

        if (result.isEmpty())
            throw new IncomeException("ComposicionCuotaInicialCobro - La búsqueda no devolvió registros");

        return result;
    }
}
