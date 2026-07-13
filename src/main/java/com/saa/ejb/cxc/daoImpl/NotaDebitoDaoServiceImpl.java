package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.NotaDebitoDaoService;
import com.saa.model.cxc.NotaDebito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class NotaDebitoDaoServiceImpl extends EntityDaoImpl<NotaDebito> implements NotaDebitoDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "tipoComprobante", "facturador", "titular", "tipoDoc", "numero", "numEstablecimiento", "numPtoEmision", "secuencial", "ambiente", "clave", "fecha", "tipoDocModificado", "numDocModificado", "fechaEmisionDM", "observacion", "subtotal", "subcero", "pIVA", "vIVA", "vICE", "vIRBPNR", "descuento", "porDescuento", "propina", "subsidio", "total", "ptoEmision", "usuario", "pathGen", "autorizacion", "fechaAutorizacion", "estado", "estadoEmision"};
	}
}
