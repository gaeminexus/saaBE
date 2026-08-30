package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
@Stateless
public class NotaDebitoCompraDaoServiceImpl extends EntityDaoImpl<NotaDebitoCompra> implements NotaDebitoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","tipoDocModificado","numDocModificado","fechaEmisionDM","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision","sustentoTributario","fechaRegistroContable","motivoAnulacion","fechaAnulacion","usuarioAnulacion"};
	}

	@Override
	public List<NotaDebitoCompra> selectPendientesSustento(Long idEmpresa) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				"select n from NotaDebitoCompra n where n.sustentoTributario is null and n.estado = :estado");
		if (idEmpresa != null) {
			jpql.append(" and n.empresa.codigo = :idEmpresa");
		}
		jpql.append(" order by n.fecha desc");
		TypedQuery<NotaDebitoCompra> query = em.createQuery(jpql.toString(), NotaDebitoCompra.class);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		return query.getResultList();
	}
}
