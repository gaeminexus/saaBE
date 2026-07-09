package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.InformacionGeneralFondoDaoService;
import com.saa.ejb.crd.service.InformacionGeneralFondoService;
import com.saa.model.crd.InformacionGeneralFondo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class InformacionGeneralFondoServiceImpl implements InformacionGeneralFondoService {

    @EJB
    private InformacionGeneralFondoDaoService informacionGeneralFondoDaoService;

    @Override
    public InformacionGeneralFondo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById InformacionGeneralFondo con id: " + id);
        return informacionGeneralFondoDaoService.selectById(id, NombreEntidadesCredito.INFORMACION_GENERAL_FONDO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de InformacionGeneralFondoService");
        InformacionGeneralFondo entidad = new InformacionGeneralFondo();
        for (Long registro : id) {
            informacionGeneralFondoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<InformacionGeneralFondo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de InformacionGeneralFondoService");
        for (InformacionGeneralFondo registro : lista) {
            informacionGeneralFondoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<InformacionGeneralFondo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll InformacionGeneralFondoService");
        List<InformacionGeneralFondo> result = informacionGeneralFondoDaoService.selectAll(NombreEntidadesCredito.INFORMACION_GENERAL_FONDO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total InformacionGeneralFondo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public InformacionGeneralFondo saveSingle(InformacionGeneralFondo entidad) throws Throwable {
        System.out.println("saveSingle - InformacionGeneralFondo");
        if (entidad.getCodigo() == null) {
            System.out.println("ENTRA 1 - Nuevo registro IGFN");
            // Registro nuevo → estado 1 (Sin cambios) por defecto
            if (entidad.getEstado() == null) {
                entidad.setEstado(1L);
            }
            entidad = informacionGeneralFondoDaoService.save(entidad, null);
        } else {
            System.out.println("ENTRA 2 - Actualizacion registro IGFN");
            // Actualización → marcar como Modificado para que se genere el G40
            entidad.setEstado(2L);
            entidad.setFechaModificacion(LocalDate.now());
            // usuarioModificacion viene del frontend en el objeto, no se sobreescribe aquí
            entidad = informacionGeneralFondoDaoService.save(entidad, entidad.getCodigo());
        }
        return entidad;
    }

    @Override
    public List<InformacionGeneralFondo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria InformacionGeneralFondoService");
        List<InformacionGeneralFondo> result = informacionGeneralFondoDaoService.selectByCriteria(datos, NombreEntidadesCredito.INFORMACION_GENERAL_FONDO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio InformacionGeneralFondo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<InformacionGeneralFondo> selectModificados() throws Throwable {
        System.out.println("Ingresa al metodo selectModificados InformacionGeneralFondoService");
        return informacionGeneralFondoDaoService.selectModificados();
    }
}
