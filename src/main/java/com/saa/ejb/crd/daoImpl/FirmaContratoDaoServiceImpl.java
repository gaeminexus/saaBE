package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.FirmaContratoDaoService;
import com.saa.model.crd.FirmaContrato;
import com.saa.rubros.EstadoFirmaContrato;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class FirmaContratoDaoServiceImpl extends EntityDaoImpl<FirmaContrato> implements FirmaContratoDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "codigo", "idContrato", "proveedor", "idExterno", "estado", "idAdjuntoOriginal", "idAdjuntoFirmado", "hash",
            "fechaEnvio", "fechaFirma", "fechaUltimaConsulta", "error", "fechaRegistro", "usuarioRegistro",
            "fechaAnulacion", "usuarioAnulacion", "motivoAnulacion"
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FirmaContrato> selectByContrato(Long idContrato) throws Throwable {
        System.out.println("Ingresa al metodo selectByContrato de FirmaContrato con idContrato: " + idContrato);
        Query query = em.createQuery(
            " select f from FirmaContrato f where f.idContrato = :idContrato order by f.codigo desc");
        query.setParameter("idContrato", idContrato);
        return query.getResultList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<FirmaContrato> selectVivosByContrato(Long idContrato) throws Throwable {
        System.out.println("Ingresa al metodo selectVivosByContrato de FirmaContrato con idContrato: " + idContrato);
        Query query = em.createQuery(
            " select f from FirmaContrato f where f.idContrato = :idContrato and f.estado in (:vivos) order by f.codigo desc");
        query.setParameter("idContrato", idContrato);
        query.setParameter("vivos", java.util.Arrays.asList(
            Long.valueOf(EstadoFirmaContrato.PENDIENTE_ENVIO), Long.valueOf(EstadoFirmaContrato.ENVIADO)));
        return query.getResultList();
    }
}
