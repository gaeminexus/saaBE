package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DocumentoSeguroDaoService;
import com.saa.model.crd.DocumentoSeguro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked")
@Stateless
public class DocumentoSeguroDaoServiceImpl extends EntityDaoImpl<DocumentoSeguro>
        implements DocumentoSeguroDaoService {

    @PersistenceContext
    EntityManager em;

    @Override
    public String[] obtieneCampos() {
        System.out.println("Ingresa al metodo (campos) DocumentoSeguro");
        return new String[]{
            "codigo",
            "tipoSeguro",
            "clase",
            "padre",
            "estado",
            "fechaCorte",
            "aseguradora",
            "ruc",
            "numeroPoliza",
            "numeroDocumento",
            "claveAcceso",
            "fechaEmision",
            "fechaInicio",
            "fechaFin",
            "tasa",
            "valorTotal",
            "idDocumentoCxp",
            "observacion",
            "usuarioListado",
            "fechaListado",
            "usuarioDocumento",
            "fechaDocumento",
            "usuarioDistribucion",
            "fechaDistribucion",
            "usuarioLiberacion",
            "fechaLiberacion",
            "usuarioAnulacion",
            "fechaAnulacion",
            "motivoAnulacion"
        };
    }

    @Override
    public List<DocumentoSeguro> selectByFiltros(Long tipoSeguro, Long estado, Long clase) throws Throwable {
        System.out.println("DocumentoSeguroDaoServiceImpl.selectByFiltros - tipoSeguro: " + tipoSeguro
            + " - estado: " + estado + " - clase: " + clase);

        StringBuilder jpql = new StringBuilder("select d from DocumentoSeguro d where 1 = 1");
        if (tipoSeguro != null) {
            jpql.append(" and d.tipoSeguro = :tipoSeguro");
        }
        if (estado != null) {
            jpql.append(" and d.estado = :estado");
        }
        if (clase != null) {
            jpql.append(" and d.clase = :clase");
        }
        jpql.append(" order by d.codigo desc");

        Query query = em.createQuery(jpql.toString());
        if (tipoSeguro != null) {
            query.setParameter("tipoSeguro", tipoSeguro);
        }
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (clase != null) {
            query.setParameter("clase", clase);
        }
        return query.getResultList();
    }

    @Override
    public DocumentoSeguro selectByClaveAccesoNormalizada(String claveAcceso) throws Throwable {
        System.out.println("DocumentoSeguroDaoServiceImpl.selectByClaveAccesoNormalizada - clave: " + claveAcceso);
        if (claveAcceso == null) {
            return null;
        }
        Query query = em.createQuery(
            "select d from DocumentoSeguro d where upper(trim(d.claveAcceso)) = :clave");
        query.setParameter("clave", claveAcceso.trim().toUpperCase());
        List<DocumentoSeguro> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<DocumentoSeguro> selectNotasByPadre(Long idPadre) throws Throwable {
        System.out.println("DocumentoSeguroDaoServiceImpl.selectNotasByPadre - idPadre: " + idPadre);
        if (idPadre == null) {
            return new java.util.ArrayList<>();
        }
        Query query = em.createQuery(
            "select d from DocumentoSeguro d where d.padre.codigo = :idPadre order by d.codigo asc");
        query.setParameter("idPadre", idPadre);
        return query.getResultList();
    }
}
