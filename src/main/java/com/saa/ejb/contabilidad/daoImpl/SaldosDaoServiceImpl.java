package com.saa.ejb.contabilidad.daoImpl;

import java.util.Date;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.SaldosDaoService;
import com.saa.model.contabilidad.Saldos;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Stateless
public class SaldosDaoServiceImpl extends EntityDaoImpl<Saldos>  implements SaldosDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"planCuenta",
							"nombreCuenta",
							"numeroCuenta",
							"secuencia",
							"saldoAnterio",
							"debe",
							"haber",
							"saldoActual"};
	}

	@Override
	public List<Long> selectAniosSaldos(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> selectUsuario(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectSaldosReverso(Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectByMesAnio(Long mes, Long anio, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectMaxNumero(Long tipo, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Saldos selectSaldosCierre(Long mes, Long anio, Long tipo, Long empresa) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Saldos selectByNumeroEmpresaTipo(Long numero, Long empresa, Long tipo) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectByMayorizacion(Long idMayorizacion) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectByConsultaCamposSaldos(Long idEmpresa, Long tipoSaldos, Long numero, Long estado,
			String nombreUsuario, Long rubroModuloClienteH, Long idReversion, Long numeroAnio, Long numeroMes,
			Date fechaIngresoDesde, Date fechaIngresoHasta, Date fechaSaldosDesde, Date fechaSaldosHasta)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Saldos> selectByIdPeriodo(Long idPeriodo) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
	
}
