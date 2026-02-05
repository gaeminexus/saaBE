package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.CargaArchivo;

import jakarta.ejb.Local;

@Local
public interface CargaArchivoService extends EntityService<CargaArchivo>{
	
	public String melyTest(Long idEntidad) throws Throwable;

}
