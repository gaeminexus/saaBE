package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AfectacionValoresParticipeCargaDaoService;
import com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService;
import com.saa.ejb.crd.service.AfectacionValoresParticipeCargaService;
import com.saa.model.crd.AfectacionValoresParticipeCarga;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.NovedadParticipeCarga;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.NoResultException;

@Stateless
public class AfectacionValoresParticipeCargaServiceImpl implements AfectacionValoresParticipeCargaService {

    @EJB
    private AfectacionValoresParticipeCargaDaoService afectacionDaoService;

    @EJB
    private NovedadParticipeCargaDaoService novedadDaoService;

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

    @Override
    public double diferenciaReparto(Long idNovedad) throws Throwable {
        if (idNovedad == null) {
            throw new IncomeException("idNovedad es obligatorio");
        }
        NovedadParticipeCarga novedad;
        try {
            novedad = novedadDaoService.selectById(idNovedad, NombreEntidadesCredito.NOVEDAD_PARTICIPE_CARGA);
        } catch (NoResultException e) {
            throw new IncomeException("No existe la novedad " + idNovedad);
        }

        double excedente = novedad.getMontoDiferencia() != null ? novedad.getMontoDiferencia() : 0.0;
        if (excedente <= 0.01) {
            // Sin excedente (o con faltante, diferencia negativa): el control de reparto al
            // 100% del §5 solo aplica a novedades CON excedente.
            return 0.0;
        }

        double repartido = 0.0;
        List<AfectacionValoresParticipeCarga> afectaciones = afectacionDaoService.selectByNovedad(idNovedad);
        if (afectaciones != null) {
            for (AfectacionValoresParticipeCarga afectacion : afectaciones) {
                if (afectacion.getEstado() != null && afectacion.getEstado() == 1L
                        && afectacion.getValorAfectar() != null) {
                    repartido += afectacion.getValorAfectar();
                }
            }
        }

        return Math.round((excedente - repartido) * 100.0) / 100.0;
    }
}
