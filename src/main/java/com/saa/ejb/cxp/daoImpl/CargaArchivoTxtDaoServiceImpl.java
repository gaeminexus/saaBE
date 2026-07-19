package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.CargaArchivoTxtDaoService;
import com.saa.model.cxp.CargaArchivoTxt;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class CargaArchivoTxtDaoServiceImpl extends EntityDaoImpl<CargaArchivoTxt> implements CargaArchivoTxtDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","empresa","usuario","fechaCarga","nombreArchivo","totalRegistros","registrosNuevos","registrosDuplicados","registrosNovedad","estado","observacion"};
	}
}
