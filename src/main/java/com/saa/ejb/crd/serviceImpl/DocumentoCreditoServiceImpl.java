package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DocumentoCreditoDaoService;
import com.saa.ejb.crd.service.DocumentoCreditoService;
import com.saa.model.crd.DocumentoCredito;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DocumentoCreditoServiceImpl implements DocumentoCreditoService {

    @EJB
    private DocumentoCreditoDaoService documentoDaoService;

    @Override
    public DocumentoCredito selectById(Long id) throws Throwable {
        System.out.println("Ingresa a selectById DocumentoCredito con id: " + id);
        return documentoDaoService.selectById(id, NombreEntidadesCredito.DOCUMENTO_CREDITO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DocumentoCreditoService ... depurado");
        DocumentoCredito doc = new DocumentoCredito();
        for (Long registro : id) {
            documentoDaoService.remove(doc, registro);
        }
    }

    @Override
    public void save(List<DocumentoCredito> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DocumentoCreditoService");
        for (DocumentoCredito registro : lista) {
            documentoDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<DocumentoCredito> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DocumentoCreditoService");
        List<DocumentoCredito> result = documentoDaoService.selectAll(NombreEntidadesCredito.DOCUMENTO_CREDITO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total DocumentoCredito no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public DocumentoCredito saveSingle(DocumentoCredito doc) throws Throwable {
        System.out.println("saveSingle - DocumentoCredito");
        if (doc.getCodigo() == null) {
            doc.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        doc = documentoDaoService.save(doc, doc.getCodigo());
        return doc;
    }

    @Override
    public List<DocumentoCredito> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DocumentoCreditoService");
        List<DocumentoCredito> result = documentoDaoService.selectByCriteria(datos, NombreEntidadesCredito.DOCUMENTO_CREDITO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio DocumentoCredito no devolvió ningún registro");
        }
        return result;
    }
}
