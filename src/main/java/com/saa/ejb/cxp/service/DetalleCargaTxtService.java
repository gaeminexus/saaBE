package com.saa.ejb.cxp.service;
import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.DetalleCargaTxt;
import jakarta.ejb.Local;
@Local
public interface DetalleCargaTxtService extends EntityService<DetalleCargaTxt> {
	List<DetalleCargaTxt> selectByCarga(Long idCarga) throws Throwable;
	List<DetalleCargaTxt> selectByDocumento(Long idDocumento) throws Throwable;
}