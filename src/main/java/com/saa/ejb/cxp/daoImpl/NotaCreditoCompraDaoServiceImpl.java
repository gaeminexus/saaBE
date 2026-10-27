package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.EstadoDevolucionAnticipoProveedor;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
@Stateless
public class NotaCreditoCompraDaoServiceImpl extends EntityDaoImpl<NotaCreditoCompra> implements NotaCreditoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","tipoDocModificado","numDocModificado","fechaEmisionDM","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision","sustentoTributario","fechaRegistroContable","motivoAnulacion","fechaAnulacion","usuarioAnulacion","estadoSeguro","idDocumentoSeguro"};
	}

	@Override
	public List<NotaCreditoCompra> selectByClaveEmpresa(String clave, Long idEmpresa) throws Throwable {
		TypedQuery<NotaCreditoCompra> query = em.createQuery(
				"select n from NotaCreditoCompra n where n.clave = :clave and n.empresa.codigo = :idEmpresa",
				NotaCreditoCompra.class);
		query.setParameter("clave", clave);
		query.setParameter("idEmpresa", idEmpresa);
		return query.getResultList();
	}

	@Override
	public List<NotaCreditoCompra> selectPendientesSustento(Long idEmpresa) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				"select n from NotaCreditoCompra n where n.sustentoTributario is null and n.estado = :estado");
		if (idEmpresa != null) {
			jpql.append(" and n.empresa.codigo = :idEmpresa");
		}
		jpql.append(" order by n.fecha desc");
		TypedQuery<NotaCreditoCompra> query = em.createQuery(jpql.toString(), NotaCreditoCompra.class);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> selectVigentesConAplicadoByTitularEmpresa(Long idTitular, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectVigentesConAplicadoByTitularEmpresa con titular: " + idTitular
				+ " | empresa: " + idEmpresa);
		// Una sola consulta: aplicado y devuelto salen de subconsultas escalares correlacionadas por
		// la NC (sin N+1 ni "in :ids"). Registrada y vigente = con asiento, ACTIVO y no anulada (3).
		jakarta.persistence.Query query = em.createQuery(
				" select n.id, n.numero, n.fecha, n.numDocModificado, n.total, " +
				"        (select coalesce(sum(a.montoAplicado), 0.0) from AplicacionPagoCxp a " +
				"          where a.notaCredito.id = n.id and a.estado = :aplicacionActiva), " +
				"        (select coalesce(sum(d.valor), 0.0) from DetalleDevolucionAnticipo d " +
				"          where d.notaCredito.id = n.id and d.devolucion.estado = :devolucionActiva) " +
				" from   NotaCreditoCompra n " +
				" where  n.titular.codigo = :idTitular " +
				" and    n.empresa.codigo = :idEmpresa " +
				" and    n.estado = :activo " +
				" and    coalesce(n.estadoEmision, 0) <> 3 " +
				" and    n.asiento is not null " +
				" order by n.fecha asc, n.id asc");
		query.setParameter("aplicacionActiva", Long.valueOf(EstadoAplicacionPago.ACTIVO));
		query.setParameter("devolucionActiva", Long.valueOf(EstadoDevolucionAnticipoProveedor.ACTIVA));
		query.setParameter("idTitular", idTitular);
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

	@Override
	public double[] selectAplicadoYDevuelto(Long idNotaCredito) throws Throwable {
		System.out.println("Ingresa al metodo selectAplicadoYDevuelto con notaCredito: " + idNotaCredito);
		Object aplicado = em.createQuery(
				" select coalesce(sum(a.montoAplicado), 0.0) from AplicacionPagoCxp a " +
				" where  a.notaCredito.id = :idNc and a.estado = :activa")
				.setParameter("idNc", idNotaCredito)
				.setParameter("activa", Long.valueOf(EstadoAplicacionPago.ACTIVO))
				.getSingleResult();
		Object devuelto = em.createQuery(
				" select coalesce(sum(d.valor), 0.0) from DetalleDevolucionAnticipo d " +
				" where  d.notaCredito.id = :idNc and d.devolucion.estado = :activa")
				.setParameter("idNc", idNotaCredito)
				.setParameter("activa", Long.valueOf(EstadoDevolucionAnticipoProveedor.ACTIVA))
				.getSingleResult();
		return new double[] { ((Number) aplicado).doubleValue(), ((Number) devuelto).doubleValue() };
	}
}
