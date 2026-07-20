package com.saa.ejb.cxp.service;
import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.CargaArchivoTxt;
import jakarta.ejb.Local;
@Local
public interface CargaArchivoTxtService extends EntityService<CargaArchivoTxt> {
	List<CargaArchivoTxt> selectByEmpresa(Long idEmpresa) throws Throwable;
}