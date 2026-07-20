package com.saa.ejb.cxp.service;
import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.DocumentoCxp;
import jakarta.ejb.Local;
@Local
public interface DocumentoCxpService extends EntityService<DocumentoCxp> {
	List<DocumentoCxp> selectByEmpresa(Long idEmpresa) throws Throwable;
	List<DocumentoCxp> selectByEmpresaEstado(Long idEmpresa, Long estado) throws Throwable;
	List<DocumentoCxp> selectNovedadesPendientes(Long idEmpresa) throws Throwable;
}