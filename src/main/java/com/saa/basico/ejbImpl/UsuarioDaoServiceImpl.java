/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.model.scp.Usuario;
import com.saa.rubros.CodigoAlternoJerarquias;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

/**
 * @author GaemiSoft.
 *
 *         Clase UsuarioDao.
 */
@Stateless
public class UsuarioDaoServiceImpl extends EntityDaoImpl<Usuario> implements UsuarioDaoService {

	//Inicializa persistence context
		@PersistenceContext
		EntityManager em;
		
		@EJB
		private DetalleRubroDaoService detalleRubroDaoService;
		
		/* (non-Javadoc)
		 * @see com.gaemisoft.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
		 */
		public String[] obtieneCampos() {
			System.out.println("Ingresa al metodo (campos) Ambito");
			return new String[]{"codigo",
								"codigoJerarquia",
								"nombre",
								"nivel",
								"codigoPadre",
								"ingresado"};
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<Usuario> selectUsuarios() throws Throwable {
			System.out.println("Dao (selectUsuarios) Usuario");
			Query query = em.createQuery(" select a " +
										 " from   Usuario a" +
										 " where  a.jerarquia.codigoAlterno = :idTipo " +
										 " order by a.nombre ");	
			query.setParameter("idTipo", Long.valueOf(CodigoAlternoJerarquias.USUARIO));
			return query.getResultList();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public List<Usuario> selectUsuariosActivos() throws Throwable {
			System.out.println("Dao (selectUsuarios) Usuario");
			Query query = em.createNativeQuery(" SELECT   * " +
											   " FROM     scp.pjrq d" +
											   " WHERE    d.pgspcdgo = 9 " +
											   "          AND   (SELECT   pdcnvlrr " +
											   "                 FROM     scp.pdcn " +
											   "                 WHERE    pcxncdgo = 19 " +
											   "                 AND   pjrqcdgo = d.pjrqcdgo) = 1", Usuario.class);	
			return (List<Usuario>)query.getResultList();
		}

		@Override
		public String validaUsuario(String idUsuario, String clave) throws Throwable {
			StoredProcedureQuery query = this.em.createStoredProcedureQuery("scp.pc_prmt_gnrl.pr_vlda_usro_clve");
			query.registerStoredProcedureParameter("pv_usuario", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_clave", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_resultado", String.class, ParameterMode.OUT);
			query.setParameter("pv_usuario", idUsuario);
			query.setParameter("pv_clave", clave);
			query.execute();
			String result = (String) query.getOutputParameterValue("pv_resultado");
			return result;
		}

		@Override
		public String cambiaClave(String idUsuario, String anterior, String nueva) throws Throwable {
			StoredProcedureQuery query = this.em.createStoredProcedureQuery("scp.PC_PRMT_GNRL.pr_cmba_usro_clve");
			query.registerStoredProcedureParameter("pv_usuario", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_clave_ant", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_clave_nue", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_resultado", String.class, ParameterMode.OUT);
			query.setParameter("pv_usuario", idUsuario);
			query.setParameter("pv_clave_ant", anterior);
			query.setParameter("pv_clave_nue", nueva);
			query.execute();
			String result = (String) query.getOutputParameterValue("pv_resultado");
			return result;
		}

		@Override
		public Usuario selectByNombre(String nombre) throws Throwable {
			// System.out.println("Dao (selectByNombre) Usuario con nombre: " + nombre);
			Query query = em.createQuery(" select a " +
										 " from      Usuario a" +
										 " where     a.nombre = :nombre");	
			query.setParameter("nombre", nombre);
			return (Usuario)query.getSingleResult();
		}

		@Override
		public String verificaPermiso(Long idEmpresa, Long idUsuario, Long idPermiso) throws Throwable {
			StoredProcedureQuery query = this.em.createStoredProcedureQuery("scp.pc_crct_espc.pr_vrfc_prms_susr");
			query.registerStoredProcedureParameter("pn_empresa", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pn_cdusro", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pn_permiso", Long.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_resultado", String.class, ParameterMode.OUT);
			query.setParameter("pn_empresa", idEmpresa);
			query.setParameter("pn_cdusro", idUsuario);
			query.setParameter("pn_permiso", idPermiso);
			query.execute();
			String result = (String) query.getOutputParameterValue("pv_resultado");
			return result;
		}

		@Override
		public String validaUsuarioSucursal(String idUsuario, String clave, String idEmpresa) throws Throwable {
			StoredProcedureQuery query = this.em.createStoredProcedureQuery("scp.pc_prmt_gnrl.pr_vlda_usro_clve");
			query.registerStoredProcedureParameter("pv_usuario", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_clave", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("pv_resultado", String.class, ParameterMode.OUT);
			query.setParameter("pv_usuario", idUsuario);
			query.setParameter("pv_clave", clave);
			query.execute();
			String result = (String) query.getOutputParameterValue("pv_resultado");
			if (result.equals("OK")) {
				Usuario usuario = selectByNombre(idUsuario);
				String permisoIngreso = detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.MODULO_SISTEMA, ModuloSistema.INGRESO);
				query = this.em.createStoredProcedureQuery("scp.pc_crct_espc.pr_vrfc_prms_susr");
				query.registerStoredProcedureParameter("pn_empresa", Long.class, ParameterMode.IN);
				query.registerStoredProcedureParameter("pn_cdusro", Long.class, ParameterMode.IN);
				query.registerStoredProcedureParameter("pn_permiso", Long.class, ParameterMode.IN);
				query.registerStoredProcedureParameter("pv_resultado", String.class, ParameterMode.OUT);
				query.setParameter("pn_empresa", Long.valueOf(idEmpresa));
				query.setParameter("pn_cdusro", Long.valueOf(usuario.getCodigo()));
				query.setParameter("pn_permiso", Long.valueOf(permisoIngreso)); // id del permiso de ingreso del sistema tomado del sistema de seguridades
				query.execute();
				result = (String) query.getOutputParameterValue("pv_resultado");
				if (!result.equals("OK")) {
					result = "NO TIENE PERMISO DE INGRESO EN ESTA SUCURSAL";
				}
			}
			return result;
		}

}
