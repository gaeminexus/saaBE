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
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","tipoDocModificado","numDocModificado","fechaEmisionDM","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision","sustentoTributario","fechaRegistroContable","motivoAnulacion","fechaAnulacion","usuarioAnulacion","estadoSeguro","idDocumentoSeguro"};
	}

	@Override
	public List<NotaDebitoCompra> selectByClaveEmpresa(String clave, Long idEmpresa) throws Throwable {
		TypedQuery<NotaDebitoCompra> query = em.createQuery(
				"select n from NotaDebitoCompra n where n.clave = :clave and n.empresa.codigo = :idEmpresa",
				NotaDebitoCompra.class);
		query.setParameter("clave", clave);
		query.setParameter("idEmpresa", idEmpresa);
		return query.getResultList();
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

	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> selectVigentesConFacturaByTitularEmpresa(Long idTitular, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectVigentesConFacturaByTitularEmpresa con titular: " + idTitular
				+ " | empresa: " + idEmpresa);
		// Una sola consulta: la factura de cada ND se empareja por número sin guiones + titular + empresa
		// (mismo criterio que selectFacturaByNumero) y lo aplicado a la factura sale de una subconsulta
		// escalar. ND vigente = con asiento, ACTIVA, no anulada (3); no bloqueada por seguros.
		jakarta.persistence.Query query = em.createQuery(
				" select n.id, n.numero, n.fecha, n.total, f.id, f.numero, f.total, " +
				"        (select coalesce(sum(a.montoAplicado), 0.0) from AplicacionPagoCxp a " +
				"          where a.facturaCompra.id = f.id and a.estado = :aplicacionActiva) " +
				" from   NotaDebitoCompra n, FacturaCompra f " +
				" where  n.titular.codigo = :idTitular " +
				" and    n.empresa.codigo = :idEmpresa " +
				" and    n.estado = :activo " +
				" and    coalesce(n.estadoEmision, 0) <> 3 " +
				" and    n.asiento is not null " +
				" and    coalesce(n.estadoSeguro, 0) <> :bloqueado " +
				" and    f.titular.codigo = n.titular.codigo " +
				" and    f.empresa.codigo = n.empresa.codigo " +
				" and    coalesce(f.estadoEmision, 0) <> 3 " +
				" and    FUNCTION('replace', f.numero, '-', '') = FUNCTION('replace', n.numDocModificado, '-', '') " +
				" order by n.fecha asc, n.id asc");
		query.setParameter("aplicacionActiva", Long.valueOf(com.saa.rubros.EstadoAplicacionPago.ACTIVO));
		query.setParameter("idTitular", idTitular);
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		query.setParameter("bloqueado", Long.valueOf(com.saa.rubros.EstadoSeguroDocumentoCxp.BLOQUEADO));
		return query.getResultList();
	}
}
