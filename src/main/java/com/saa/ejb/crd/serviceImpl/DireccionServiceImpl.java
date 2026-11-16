package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DireccionDaoService;
import com.saa.ejb.crd.service.DireccionService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.model.crd.Direccion;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DireccionServiceImpl implements DireccionService {

    @EJB
    private DireccionDaoService direccionDaoService;

    @EJB
    private EntidadService entidadService;

    @Override
    public Direccion selectById(Long id) throws Throwable {
        System.out.println("selectById - Direccion: " + id);
        return direccionDaoService.selectById(id, NombreEntidadesCredito.DIRECCION);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - Direccion");
        Direccion dir = new Direccion();
        for (Long registro : id) {
            direccionDaoService.remove(dir, registro);
        }
    }

    @Override
    public void save(List<Direccion> lista) throws Throwable {
        System.out.println("save list - Direccion");
        for (Direccion registro : lista) {
            direccionDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Direccion> selectAll() throws Throwable {
        System.out.println("selectAll - Direccion");
        List<Direccion> result =
            direccionDaoService.selectAll(NombreEntidadesCredito.DIRECCION);

        if (result.isEmpty()) {
            throw new IncomeException("No existen registros Direccion");
        }
        return result;
    }

    @Override
    public Direccion saveSingle(Direccion dir) throws Throwable {
        System.out.println("saveSingle - Direccion");
        if (dir.getCodigo() == null) {
            dir.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        Direccion guardada = direccionDaoService.save(dir, dir.getCodigo());
        desmarcarOtrosDomicilios(guardada);
        return guardada;
    }

    /**
     * Una sola dirección de domicilio por defecto por entidad (DISENO-DIRECCION-DOMICILIO-UNICA-Y-CIUDAD.md §2): si la
     * guardada es un domicilio activo con porDefecto = 1, las demás domiciliarias activas de la entidad quedan en 0, en la
     * misma transacción. Las de trabajo no se tocan.
     */
    private void desmarcarOtrosDomicilios(Direccion guardada) throws Throwable {
        if (guardada == null || guardada.getEntidad() == null || guardada.getEntidad().getCodigo() == null) {
            return;
        }
        boolean porDefecto = guardada.getPorDefecto() != null && guardada.getPorDefecto() == 1L;
        boolean trabajo = guardada.getTrabajo() != null && guardada.getTrabajo() == 1L;
        boolean activa = guardada.getEstado() == null || guardada.getEstado() == Estado.ACTIVO;
        if (!porDefecto || trabajo || !activa) {
            return;
        }
        for (Direccion otra : direccionDaoService.selectDomiciliosPorDefecto(guardada.getEntidad().getCodigo())) {
            if (!otra.getCodigo().equals(guardada.getCodigo())) {
                otra.setPorDefecto(0L);
                direccionDaoService.save(otra, otra.getCodigo());
            }
        }
    }

    @Override
    public List<Direccion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Direccion");
        List<Direccion> result =
            direccionDaoService.selectByCriteria(datos, NombreEntidadesCredito.DIRECCION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Direccion no devolvio registros");
        }
        return result;
    }

    @Override
    public List<Direccion> selectByParent(Long codigoEntidad) throws Throwable {
        System.out.println("selectByParent DireccionService codigoEntidad: " + codigoEntidad);
        return direccionDaoService.selectByParent(codigoEntidad);
    }

    @Override
    public Direccion saveSingle(Direccion direccion, String usuario) throws Throwable {
        System.out.println("saveSingle(Direccion, usuario) - usuario: " + usuario);
        direccion = saveSingle(direccion);
        entidadService.sellarActualizacion(direccion.getEntidad().getCodigo(), usuario);
        return direccion;
    }
}
