package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CuotaSeguroDaoService;
import com.saa.model.crd.CuotaSeguro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class CuotaSeguroDaoServiceImpl extends EntityDaoImpl<CuotaSeguro> implements CuotaSeguroDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) CuotaSeguro");
        return new String[]{
            "codigo", "documento", "prestamoSeguro", "idCuota", "campo",
            "saldoInicialCapital", "valorAnterior", "valorNuevo", "fechaReverso"
        };
    }

    @Override
    public List<CuotaSeguro> selectVigentesByDocumento(Long idDocumento) throws Throwable {
        System.out.println("CuotaSeguroDaoServiceImpl.selectVigentesByDocumento - idDocumento: " + idDocumento);
        if (idDocumento == null) {
            return new ArrayList<>();
        }
        Query query = em.createQuery(
            "select c from CuotaSeguro c where c.documento.codigo = :idDocumento"
            + " and c.fechaReverso is null order by c.codigo asc");
        query.setParameter("idDocumento", idDocumento);
        return query.getResultList();
    }

    @Override
    public List<CuotaSeguro> selectVigentesByCuotasYCampoExcluyendo(List<Long> idsCuota, Long campo,
            List<Long> idsDocumentoExcluir) throws Throwable {
        System.out.println("CuotaSeguroDaoServiceImpl.selectVigentesByCuotasYCampoExcluyendo - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0) + " - campo: " + campo);
        if (idsCuota == null || idsCuota.isEmpty() || campo == null) {
            return new ArrayList<>();
        }
        List<Long> excluir = idsDocumentoExcluir != null && !idsDocumentoExcluir.isEmpty()
            ? idsDocumentoExcluir : java.util.Collections.singletonList(-1L);

        // Fragmentado en bloques de 900 (límite de Oracle de 1000 elementos en IN, ORA-01795),
        // mismo criterio que DetallePrestamoDaoServiceImpl.selectByPrestamos.
        List<CuotaSeguro> resultado = new ArrayList<>();
        int tamanoBloque = 900;
        for (int inicio = 0; inicio < idsCuota.size(); inicio += tamanoBloque) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + tamanoBloque, idsCuota.size()));
            Query query = em.createQuery(
                "select c from CuotaSeguro c where c.idCuota in :idsCuota"
                + " and c.campo = :campo and c.fechaReverso is null"
                + " and c.documento.codigo not in :excluir order by c.codigo asc");
            query.setParameter("idsCuota", bloque);
            query.setParameter("campo", campo);
            query.setParameter("excluir", excluir);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }

    @Override
    public List<CuotaSeguro> selectByCuotas(List<Long> idsCuota) throws Throwable {
        System.out.println("CuotaSeguroDaoServiceImpl.selectByCuotas - cuotas: "
            + (idsCuota != null ? idsCuota.size() : 0));
        if (idsCuota == null || idsCuota.isEmpty()) {
            return new ArrayList<>();
        }
        List<CuotaSeguro> resultado = new ArrayList<>();
        int tamanoBloque = 900;
        for (int inicio = 0; inicio < idsCuota.size(); inicio += tamanoBloque) {
            List<Long> bloque = idsCuota.subList(inicio, Math.min(inicio + tamanoBloque, idsCuota.size()));
            Query query = em.createQuery(
                "select c from CuotaSeguro c where c.idCuota in :idsCuota order by c.codigo asc");
            query.setParameter("idsCuota", bloque);
            resultado.addAll(query.getResultList());
        }
        return resultado;
    }
}
