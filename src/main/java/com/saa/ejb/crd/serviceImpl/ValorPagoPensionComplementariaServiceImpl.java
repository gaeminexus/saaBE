package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ValorPagoPensionComplementariaDaoService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ValorPagoPensionComplementaria;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ValorPagoPensionComplementariaServiceImpl implements ValorPagoPensionComplementariaService {

    @EJB
    private ValorPagoPensionComplementariaDaoService valorPagoPensionComplementariaDaoService;

    @Override
    public ValorPagoPensionComplementaria selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ValorPagoPensionComplementaria con id: " + id);
        return valorPagoPensionComplementariaDaoService.selectById(id, NombreEntidadesCredito.VALOR_PAGO_PENSION_COMPLEMENTARIA);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ValorPagoPensionComplementariaService");
        ValorPagoPensionComplementaria valorPago = new ValorPagoPensionComplementaria();
        for (Long registro : id) {
            valorPagoPensionComplementariaDaoService.remove(valorPago, registro);
        }
    }

    @Override
    public void save(List<ValorPagoPensionComplementaria> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ValorPagoPensionComplementariaService");
        for (ValorPagoPensionComplementaria registro : lista) {
            valorPagoPensionComplementariaDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ValorPagoPensionComplementaria> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ValorPagoPensionComplementariaService");
        List<ValorPagoPensionComplementaria> result = valorPagoPensionComplementariaDaoService.selectAll(NombreEntidadesCredito.VALOR_PAGO_PENSION_COMPLEMENTARIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ValorPagoPensionComplementaria no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public ValorPagoPensionComplementaria saveSingle(ValorPagoPensionComplementaria valorPago) throws Throwable {
        System.out.println("saveSingle - ValorPagoPensionComplementaria");
        if (valorPago.getCodigo() == null) {
            valorPago.setEstado(Long.valueOf(Estado.ACTIVO));
            valorPago.setFechaIngreso(LocalDateTime.now());
        } else {
            valorPago.setFechaModificacion(LocalDateTime.now());
        }
        valorPago = valorPagoPensionComplementariaDaoService.save(valorPago, valorPago.getCodigo());
        return valorPago;
    }

    @Override
    public List<ValorPagoPensionComplementaria> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ValorPagoPensionComplementariaService");
        List<ValorPagoPensionComplementaria> result = valorPagoPensionComplementariaDaoService.selectByCriteria(datos, NombreEntidadesCredito.VALOR_PAGO_PENSION_COMPLEMENTARIA);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ValorPagoPensionComplementaria no devolvió ningún registro");
        }
        return result;
    }
}
