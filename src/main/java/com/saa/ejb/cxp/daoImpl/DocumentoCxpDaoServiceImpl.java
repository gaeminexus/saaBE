package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.model.cxp.DocumentoCxp;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DocumentoCxpDaoServiceImpl extends EntityDaoImpl<DocumentoCxp> implements DocumentoCxpDaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{
            "id","empresa","rucEmisor","razonSocialEmisor","tipoComprobante","serieComprobante",
            "claveAcceso","fechaAutorizacion","fechaEmision","identificacionReceptor",
            "valorSinImpuestos","iva","importeTotal","numeroDocumentoModificado",
            "estadoDocumento","pathXml","fechaCargaXml","usuarioCargaXml",
            "idDocumentoBD","tipoTablaDestino","fechaRegistroBD","usuarioRegistroBD",
            "fechaReversion","usuarioReversion","novedad","estadoNovedad","observacion",
            "periodoContable","esReembolso","esSeguro","idDocumentoSeguro",
            "origenXml","resultadoSri","mensajeSri","fechaDescargaSri"
        };
    }

    @Override
    public DocumentoCxp selectByClave(String claveAcceso) throws Throwable {
        List<DocumentoCxp> lista = em.createNamedQuery("DocumentoCxpByClave", DocumentoCxp.class)
                .setParameter("claveAcceso", claveAcceso)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }
}
