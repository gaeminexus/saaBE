package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.NovedadParticipeCargaDaoService;
import com.saa.model.crd.NovedadParticipeCarga;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class NovedadParticipeCargaDaoServiceImpl extends EntityDaoImpl<NovedadParticipeCarga> implements NovedadParticipeCargaDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public List<NovedadParticipeCarga> selectByParticipe(Long codigoParticipe) throws Throwable {
        System.out.println("Buscando novedades del partícipe: " + codigoParticipe);
        
        try {
            String jpql = "SELECT n FROM NovedadParticipeCarga n " +
                         "WHERE n.participeXCargaArchivo.codigo = :codigoParticipe " +
                         "ORDER BY n.codigo";
            
            Query query = em.createQuery(jpql);
            query.setParameter("codigoParticipe", codigoParticipe);
            
            @SuppressWarnings("unchecked")
            List<NovedadParticipeCarga> resultados = query.getResultList();
            
            System.out.println("Novedades encontradas: " + (resultados != null ? resultados.size() : 0));
            return resultados;
            
        } catch (Exception e) {
            System.err.println("Error al buscar novedades del partícipe: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}
