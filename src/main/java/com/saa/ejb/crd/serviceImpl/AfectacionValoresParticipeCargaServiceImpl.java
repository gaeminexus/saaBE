package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.AfectacionValoresParticipeCargaDaoService;
import com.saa.ejb.crd.service.AfectacionValoresParticipeCargaService;
import com.saa.model.crd.AfectacionValoresParticipeCarga;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class AfectacionValoresParticipeCargaServiceImpl implements AfectacionValoresParticipeCargaService {

    @EJB
    private AfectacionValoresParticipeCargaDaoService afectacionDaoService;

    @Override
    public AfectacionValoresParticipeCarga selectById(Long id) throws Throwable {
        return afectacionDaoService.selectById(id, NombreEntidadesCredito.AFECTACION_VALORES_PARTICIPE_CARGA);
    }

    @Override
    public List<AfectacionValoresParticipeCarga> selectAll() throws Throwable {
        return afectacionDaoService.selectAll(NombreEntidadesCredito.AFECTACION_VALORES_PARTICIPE_CARGA);
    }

    @Override
    public List<AfectacionValoresParticipeCarga> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        return afectacionDaoService.selectByCriteria(datos, NombreEntidadesCredito.AFECTACION_VALORES_PARTICIPE_CARGA);
    }

    @Override
    public AfectacionValoresParticipeCarga saveSingle(AfectacionValoresParticipeCarga registro) throws Throwable {
        Long codigo = registro.getCodigo();
        return afectacionDaoService.save(registro, codigo);
    }

    @Override
    public void save(List<AfectacionValoresParticipeCarga> registros) throws Throwable {
        for (AfectacionValoresParticipeCarga registro : registros) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        AfectacionValoresParticipeCarga entidad = new AfectacionValoresParticipeCarga();
        for (Long id : ids) {
            afectacionDaoService.remove(entidad, id);
        }
    }

    /**
     * Busca todas las afectaciones de una novedad específica
     * Llama directamente al método del DAO
     */
    public List<AfectacionValoresParticipeCarga> selectByNovedad(Long codigoNovedad) throws Throwable {
        return afectacionDaoService.selectByNovedad(codigoNovedad);
    }

    /**
     * Busca todas las afectaciones de un préstamo específico
     * Llama directamente al método del DAO
     */
    public List<AfectacionValoresParticipeCarga> selectByPrestamo(Long codigoPrestamo) throws Throwable {
        return afectacionDaoService.selectByPrestamo(codigoPrestamo);
    }

    /**
     * Busca todas las afectaciones de una cuota específica
     * Llama directamente al método del DAO
     */
    public List<AfectacionValoresParticipeCarga> selectByCuota(Long codigoCuota) throws Throwable {
        return afectacionDaoService.selectByCuota(codigoCuota);
    }
}
