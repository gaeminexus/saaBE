package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.LiquidacionCompraDaoService;
import com.saa.model.cxc.LiquidacionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class LiquidacionCompraDaoServiceImpl extends EntityDaoImpl<LiquidacionCompra> implements LiquidacionCompraDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id", "tipoComprobante", "facturador", "titular", "tipoDoc", "numero",
			"numEstablecimiento", "numPtoEmision", "secuencial", "ambiente", "clave",
			"fecha", "observacion", "subtotal", "subcero", "pIVA", "vIVA", "vICE",
			"vIRBPNR", "descuento", "porDescuento", "propina", "subsidio",
			"totalSinSub", "ahorroSub", "total", "ptoEmision", "usuario", "pathGen",
			"autorizacion", "fechaAutorizacion", "estado", "estadoEmision"
		};
	}
}
