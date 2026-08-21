package com.saa.ejb.cxp.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.ReembolsoFacturaCompraDaoService;
import com.saa.ejb.cxp.service.ReembolsoFacturaCompraService;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.ReembolsoFacturaCompra;
import com.saa.rubros.Estado;
import com.saa.rubros.OrigenReembolso;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ReembolsoFacturaCompraServiceImpl implements ReembolsoFacturaCompraService {

    @EJB private ReembolsoFacturaCompraDaoService reembolsoFacturaCompraDaoService;

    @Override
    public ReembolsoFacturaCompra selectById(Long id) throws Throwable {
        System.out.println("selectById ReembolsoFacturaCompra: " + id);
        return reembolsoFacturaCompraDaoService.selectById(id, NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove ReembolsoFacturaCompra: " + id);
        ReembolsoFacturaCompra entidad = new ReembolsoFacturaCompra();
        for (Long registro : id) {
            reembolsoFacturaCompraDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<ReembolsoFacturaCompra> lista) throws Throwable {
        for (ReembolsoFacturaCompra registro : lista) {
            reembolsoFacturaCompraDaoService.save(registro, registro.getId());
        }
    }

    @Override
    public List<ReembolsoFacturaCompra> selectAll() throws Throwable {
        System.out.println("selectAll ReembolsoFacturaCompra");
        List<ReembolsoFacturaCompra> result =
                reembolsoFacturaCompraDaoService.selectAll(NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
        if (result.isEmpty())
            throw new IncomeException("Busqueda total ReembolsoFacturaCompra no devolvio ningun registro");
        return result;
    }

    @Override
    public ReembolsoFacturaCompra saveSingle(ReembolsoFacturaCompra entidad) throws Throwable {
        System.out.println("saveSingle ReembolsoFacturaCompra id=" + entidad.getId());
        if (entidad.getId() == null) {
            entidad.setEstado(Long.valueOf(Estado.ACTIVO));
            if (entidad.getOrigen() == null) {
                entidad.setOrigen(Long.valueOf(OrigenReembolso.MANUAL));
            }
        }
        return reembolsoFacturaCompraDaoService.save(entidad, entidad.getId());
    }

    @Override
    public List<ReembolsoFacturaCompra> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria ReembolsoFacturaCompra");
        List<ReembolsoFacturaCompra> result =
                reembolsoFacturaCompraDaoService.selectByCriteria(datos, NombreEntidadesCompra.REEMBOLSO_FACTURA_COMPRA);
        if (result.isEmpty())
            throw new IncomeException("Busqueda por criterio ReembolsoFacturaCompra no devolvio ningun registro");
        return result;
    }
}
