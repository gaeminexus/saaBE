package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.HistoricoDesgloseAporteParticipeDaoService;
import com.saa.ejb.crd.service.HistoricoDesgloseAporteParticipeService;
import com.saa.model.crd.HistoricoDesgloseAporteParticipe;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class HistoricoDesgloseAporteParticipeServiceImpl implements HistoricoDesgloseAporteParticipeService {

    @EJB
    private HistoricoDesgloseAporteParticipeDaoService historicoDesgloseAporteParticipeDaoService;

    @Override
    public HistoricoDesgloseAporteParticipe selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return historicoDesgloseAporteParticipeDaoService.selectById(id, NombreEntidadesCredito.HISTORICO_DESGLOSE_APORTE_PARTICIPE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de HistoricoDesgloseAporteParticipeService");
        HistoricoDesgloseAporteParticipe registro = new HistoricoDesgloseAporteParticipe();
        for (Long idRegistro : id) {
            historicoDesgloseAporteParticipeDaoService.remove(registro, idRegistro);
        }
    }

    @Override
    public void save(List<HistoricoDesgloseAporteParticipe> lista) throws Throwable {
        System.out.println("Ingresa al metodo save[] de HistoricoDesgloseAporteParticipeService");
        
        for (HistoricoDesgloseAporteParticipe registro : lista) {
            saveSingle(registro);
        }
    }

    @Override
    public HistoricoDesgloseAporteParticipe saveSingle(HistoricoDesgloseAporteParticipe entity) throws Throwable {
        System.out.println("Ingresa al metodo saveSingle de HistoricoDesgloseAporteParticipeService");
        
        // Configurar valores por defecto si es INSERT
        if (entity.getOrden() == null || entity.getOrden() == 0L) {
            if (entity.getEstado() == null) {
                entity.setEstado(1); // Estado: Cargado
            }
        }
        
        return historicoDesgloseAporteParticipeDaoService.save(entity, entity.getOrden());
    }

    @Override
    public List<HistoricoDesgloseAporteParticipe> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll de HistoricoDesgloseAporteParticipeService");
        return historicoDesgloseAporteParticipeDaoService.selectAll(NombreEntidadesCredito.HISTORICO_DESGLOSE_APORTE_PARTICIPE);
    }

    @Override
    public List<HistoricoDesgloseAporteParticipe> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria de HistoricoDesgloseAporteParticipeService");
        return historicoDesgloseAporteParticipeDaoService.selectByCriteria(datos, NombreEntidadesCredito.HISTORICO_DESGLOSE_APORTE_PARTICIPE);
    }

    // Métodos específicos del servicio
    
    @Override
    public List<HistoricoDesgloseAporteParticipe> obtenerPorCarga(Long idCarga) throws Throwable {
        System.out.println("Obtener desglose por carga: " + idCarga);
        return historicoDesgloseAporteParticipeDaoService.selectByCarga(idCarga);
    }

    @Override
    public List<HistoricoDesgloseAporteParticipe> obtenerPorCedula(String cedula) throws Throwable {
        System.out.println("Obtener desglose por cédula: " + cedula);
        return historicoDesgloseAporteParticipeDaoService.selectByCedula(cedula);
    }

    @Override
    public HistoricoDesgloseAporteParticipe obtenerPorCodigoInternoYCarga(String codigoInterno, Long idCarga) throws Throwable {
        System.out.println("Obtener desglose por código interno: " + codigoInterno + " y carga: " + idCarga);
        return historicoDesgloseAporteParticipeDaoService.selectByCodigoInternoYCarga(codigoInterno, idCarga);
    }
}
