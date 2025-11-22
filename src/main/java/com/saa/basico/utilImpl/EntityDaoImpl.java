/**
 * Copyright o Gaemi Soft Coa. Ltda. , 2011 Reservados todos los derechos  
 * Joso Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa esto protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproduccion o la distribucion no autorizadas de este programa, o de cualquier parte del mismo, 
 * esto penada por la ley y con severas sanciones civiles y penales, y sero objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informacion confidencial y se utilizaro solo en  conformidad  
 * con los torminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.utilImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.EntityDao;
import com.saa.rubros.Estado;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoComandosBusqueda;
import com.saa.rubros.TipoDatosBusqueda;

/**
 * @author GaemiSoft.
 *
 *         Clase Entidad.
 */
@SuppressWarnings("unchecked")
@Stateless
public class EntityDaoImpl<Tipo> implements EntityDao<Tipo> {

	// Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.sistema.ejb.util.EntityDao#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		// METODO IMPLEMENTADO EN CADA DAO
		return new String[] {};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.util.EntityDao#selectAll(java.lang.String)
	 */
	public List<Tipo> selectAll(String entidad) throws Throwable {
		// System.out.println("Entidad.recuperaTodos");
		Query query = em.createNamedQuery(entidad + "All");
		return query.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.tesoreria.ejb.util.EntityDao#find(java.lang.Object,
	 * java.lang.Long)
	 */
	public Tipo find(Tipo clase, Long id) throws Throwable {
		return (Tipo) em.find(clase.getClass(), id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.util.EntityDao#remove(java.lang.Object,
	 * java.lang.Long)
	 */
	public void remove(Tipo clase, Long id) throws Throwable {
		Tipo tipo = (Tipo) find(clase, id);
		if (tipo != null) {
			em.remove(tipo);
			try {
				em.flush();
			} catch (PersistenceException e) {
				// System.out.println("Error al
				// eliminar:"+e.getCause().getCause().getMessage());
				throw e.getCause().getCause();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityDao#selectById(java.lang.Long,
	 * java.lang.String)
	 */
	public Tipo selectById(Long id, String entidad) throws Throwable {
		// System.out.println("Ingresa al metodo selectById con entidad: " + entidad + "
		// y id: " + id);
		Query query = em.createNamedQuery(entidad + "Id");
		query.setParameter("id", id);
		return (Tipo) query.getSingleResult();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityDao#selectByCriteria(java.util.
	 * List, java.lang.String[], java.lang.String)
	 */
	public List<Tipo> selectByCriteria(List<DatosBusqueda> datos, String nombreEntidad) throws Throwable {
		// System.out.println("Ingresa al metodo selectByCriteria con entidad: " +
		// nombreEntidad);
		List<Tipo> resultado = null;
		int numeroCamposOrderby = 0;
		boolean abreParentesis = false;
		boolean agregaCondicion = true;
		String campoBuscar = null; // Variable parametro de busqueda
		/*
		 * DateFormat df = new SimpleDateFormat(
		 * detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.FORMATO_FECHA,
		 * FormatoFecha.EJB_SIN_HORA));
		 */
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		DateTimeFormatter formatterSinHora = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		// CREA QUERY
		String strQuery = "select b from " + nombreEntidad + " b ";
		if (!datos.isEmpty()) {
			strQuery = strQuery + "where 1 = 1";
			for (DatosBusqueda aBuscar : datos) {
				if (aBuscar.getCampoAdicional() == DatosBusqueda.NO_CAMPO1) {
					campoBuscar = aBuscar.getCampo() + aBuscar.getNumeroCampoRepetido();
				} else {
					campoBuscar = suprimirPuntos(aBuscar.getCampo())
							+ formatoLetraCapital(aBuscar.getCampo1() + aBuscar.getNumeroCampoRepetido());
				}
				if (DatosBusqueda.SOLO_CAMPO_ORDER_BY != aBuscar.getCampoOrderBy()) {
					agregaCondicion = true;
					if (aBuscar.getParentesis() == DatosBusqueda.USA_PARENTESIS) {
						agregaCondicion = false;
						if (aBuscar.getTipoOperadorLogico() == TipoComandosBusqueda.ABRE_PARENTESIS) {
							abreParentesis = true;
						}
						if (aBuscar.getTipoOperadorLogico() == TipoComandosBusqueda.CIERRA_PARENTESIS) {
							strQuery = strQuery + " " +
									detalleRubroDaoService.selectValorStringByRubAltDetAlt(
											Rubros.TIPO_COMANDOS_BUSQUEDA, TipoComandosBusqueda.CIERRA_PARENTESIS);
						}
					}
					if (agregaCondicion) {
						strQuery = strQuery + " " +
								detalleRubroDaoService.selectValorStringByRubAltDetAlt(Rubros.TIPO_COMANDOS_BUSQUEDA,
										aBuscar.getTipoOperadorLogico());
						if (abreParentesis) {
							strQuery = strQuery + " " +
									detalleRubroDaoService.selectValorStringByRubAltDetAlt(
											Rubros.TIPO_COMANDOS_BUSQUEDA, TipoComandosBusqueda.ABRE_PARENTESIS);
							abreParentesis = false;
						}
						if (Estado.ACTIVO == aBuscar.getTruncado()) {
							strQuery = strQuery + " " + detalleRubroDaoService.selectValorStringByRubAltDetAlt(
									Rubros.TIPO_COMANDOS_BUSQUEDA, TipoComandosBusqueda.TRUNCADO);
						}
						strQuery = strQuery + " (b." + aBuscar.getCampo();
						if (DatosBusqueda.SI_CAMPO1 == aBuscar.getCampoAdicional()) {
							strQuery = strQuery + "." + aBuscar.getCampo1();
						}
						if (TipoComandosBusqueda.IS_NULL == aBuscar.getTipoComparacion()) {
							strQuery = strQuery + " IS NULL ";
							strQuery = strQuery + ") ";
						} else {
							strQuery = strQuery + ") ";
							strQuery = strQuery +
									detalleRubroDaoService.selectValorStringByRubAltDetAlt(
											Rubros.TIPO_COMANDOS_BUSQUEDA, aBuscar.getTipoComparacion())
									+ " :" + suprimirPuntos(campoBuscar); // **********************************
						}
						if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
							strQuery = strQuery + " and :" + suprimirPuntos(campoBuscar) + "1"; // ******************************
						}
					}
				}
			}

			// GENERA ORDER BY
			for (DatosBusqueda aBuscar : datos) {
				if (DatosBusqueda.NO_CAMPO_ORDER_BY != aBuscar.getCampoOrderBy()) {
					numeroCamposOrderby++;
					// COMPARA PARA SABER SI ES EL PRIMER CAMPO Y SOLO AGREGAR UNA VEZ
					// LA PALABRA ORDER BY
					if (numeroCamposOrderby == DatosBusqueda.CAMPO_ORDER_BY) {
						strQuery = strQuery + " order by ";
					} else {
						strQuery = strQuery + " , ";
					}
					strQuery = strQuery + "b." + aBuscar.getCampo();
					if (aBuscar.getTipoOrden() == DatosBusqueda.ORDER_ASC) {
						strQuery = strQuery + " asc";
					}
					if (aBuscar.getTipoOrden() == DatosBusqueda.ORDER_DESC) {
						strQuery = strQuery + " desc";
					}
				}
			}
		}
		System.out.println("SELECT ES:" + strQuery);
		Query query = em.createQuery(strQuery);
		if (!datos.isEmpty()) {
			for (DatosBusqueda aBuscar : datos) {
				if (aBuscar.getParentesis() != DatosBusqueda.USA_PARENTESIS) {
					if (aBuscar.getCampoAdicional() == DatosBusqueda.NO_CAMPO1) {
						campoBuscar = suprimirPuntos(aBuscar.getCampo()) + aBuscar.getNumeroCampoRepetido();
					} else {
						campoBuscar = suprimirPuntos(aBuscar.getCampo()) + formatoLetraCapital(aBuscar.getCampo1())
								+ aBuscar.getNumeroCampoRepetido();
					}
					switch (aBuscar.getTipoDato()) {
						case TipoDatosBusqueda.STRING:
							if (TipoComandosBusqueda.IS_NULL != aBuscar.getTipoComparacion()) {
								if (TipoComandosBusqueda.LIKE == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar, "%" + aBuscar.getValor().toUpperCase() + "%");
								} else {
									query.setParameter(campoBuscar, aBuscar.getValor());
								}
								if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar + "1", aBuscar.getValor1());
								}
							}
							break;
						case TipoDatosBusqueda.LONG:
							if (TipoComandosBusqueda.IS_NULL != aBuscar.getTipoComparacion()) {
								query.setParameter(campoBuscar, Long.valueOf(aBuscar.getValor()));
								if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar + "1", Long.valueOf(aBuscar.getValor1()));
								}
							}
							break;
						case TipoDatosBusqueda.DATE:
							if (TipoComandosBusqueda.IS_NULL != aBuscar.getTipoComparacion()) {
								query.setParameter(campoBuscar, LocalDate.parse(aBuscar.getValor(), formatterSinHora));
								if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar + "1",
											LocalDate.parse(aBuscar.getValor1(), formatterSinHora));
								}
							}
							break;
						case TipoDatosBusqueda.DATE_TIME:
							if (TipoComandosBusqueda.IS_NULL != aBuscar.getTipoComparacion()) {
								query.setParameter(campoBuscar, LocalDateTime.parse(aBuscar.getValor(), formatter));
								if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar + "1",
											LocalDateTime.parse(aBuscar.getValor1(), formatter));
								}
							}
							break;
						case TipoDatosBusqueda.DOUBLE:
							if (TipoComandosBusqueda.IS_NULL != aBuscar.getTipoComparacion()) {
								query.setParameter(campoBuscar, Double.valueOf(aBuscar.getValor()));
								if (TipoComandosBusqueda.BETWEEN == aBuscar.getTipoComparacion()) {
									query.setParameter(campoBuscar + "1", Double.valueOf(aBuscar.getValor()));
								}
							}
							break;
						default:
							break;
					}
				}
			}
		}
		resultado = query.getResultList();
		return resultado;
	}

	public Tipo save(Tipo tipo, Long id) throws Throwable {
		// System.out.println("IngresaSave - entidad: "+tipo.getClass()+ " id
		// ["+id+"]");
		if (id == null) {
			em.persist(tipo);
		} else {
			em.merge(tipo);
		}
		return tipo;
	}

	/**
	 * Cambiar una palabra en formato Letra Capital, primera letra en mayuscula *
	 * 
	 * @param palabra : Palabra que se le asigna el formato
	 * @return : Palabra con formato
	 */
	private String formatoLetraCapital(String palabra) {
		String primeraLetra = palabra.substring(0, 1).toUpperCase();
		String restoPalabra = palabra.substring(1);
		return primeraLetra + restoPalabra;
	}

	/**
	 * Elimina todos los puntos internos de una cadena
	 * 
	 * @param palabra : Cadena a la que se desea eliminar los puntos
	 * @return : Cadena sin puntos
	 */
	private String suprimirPuntos(String palabra) {
		String cadPunto = palabra.replace(".", "-");
		String[] cadena = cadPunto.split("-");
		String respuesta = "";
		for (int i = 0; i < cadena.length; i++) {
			if (i != 0) {
				respuesta += formatoLetraCapital(cadena[i]);
			} else {
				respuesta += cadena[i];
			}
		}
		return respuesta;
	}
}
