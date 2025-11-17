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
package com.saa.basico.util;

import java.io.Serializable;
import com.saa.rubros.TipoComandosBusqueda;

/**
 * @author Gaemisoft.
 *         Clase que permite crear par�metros para realizar SELECTS.
 *         Solo se deben incluir los campos y valores con los que se desea
 *         realizar la busqueda.
 *         Si no se desea filtrar por cierto campo de la entidad entonces no se
 *         debe incluir.
 */
@SuppressWarnings("serial")
public class DatosBusqueda implements Serializable {

	public static final int TRUNCADO = 1;
	public static final int NO_TRUNCADO = 0;
	public static final int SI_CAMPO1 = 1;
	public static final int NO_CAMPO1 = 0;
	public static final int CAMPO_ORDER_BY = 1;
	public static final int SOLO_CAMPO_ORDER_BY = 2;
	public static final int NO_CAMPO_ORDER_BY = 0;
	public static final int USA_PARENTESIS = 1;
	public static final int NO_USA_PARENTESIS = 0;
	public static final int ORDER_ASC = 1;
	public static final int ORDER_DESC = 2;

	private int tipoDato = -1;
	private String campo = null;
	private String campo1 = null;
	private String valor = null;
	private String valor1 = null;
	private int tipoComparacion;
	private int truncado = NO_TRUNCADO;
	private int tipoOperadorLogico = TipoComandosBusqueda.AND;
	private int campoAdicional = NO_CAMPO1;
	private int campoOrderBy = NO_CAMPO_ORDER_BY;
	private int tipoOrden = 0;
	private int parentesis = NO_USA_PARENTESIS;
	private int numeroCampoRepetido = 1;

	/**
	 * Contructor B�sico
	 */
	public DatosBusqueda() {
		super();
	}

	/**
	 * Constructor con todos los campos que permite crear par�metros para realizar
	 * SELECTS.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param campo1             : Nombre del campo sobre el que se quiere buscar.
	 *                           En caso que se desee buscar en una entidad
	 *                           relacionada a la principal
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param valor1             : Valor1 del campo para realizar la busqueda en
	 *                           caso de realizar un between
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param truncado           : Indica si el campo debe ser truncado o no. 1 =
	 *                           SI, 0 = NO
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 * @param campoAdicional     : Indica si se desea buscar por un campo de una
	 *                           entidad relacionada. 1 = SI, 0 = NO
	 * @param campoOrderBy       : Campo que indica si es que el campo se toma en el
	 *                           orderBy. 1 = si, 0 = no. Usa la constante
	 *                           CAMPO_ORDER_BY
	 */
	public DatosBusqueda(int tipoDato, String campo, String campo1,
			String valor, String valor1, int tipoComparacion,
			int truncado, int tipoOperadorLogico, int campoAdicional,
			int campoOrderBy, int tipoOrden, int parentesis, int numeroCampoRepetido) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.campo1 = campo1;
		this.valor = valor;
		this.valor1 = valor1;
		this.tipoComparacion = tipoComparacion;
		this.truncado = truncado;
		this.tipoOperadorLogico = tipoOperadorLogico;
		this.campoAdicional = campoAdicional;
		this.campoOrderBy = campoOrderBy;
		this.tipoOrden = tipoOrden;
		this.parentesis = parentesis;
		this.numeroCampoRepetido = numeroCampoRepetido;
	}

	/**
	 * Constructor con todos los campos que permite crear par�metros para realizar
	 * SELECTS.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param campo1             : Nombre del campo sobre el que se quiere buscar.
	 *                           En caso que se desee buscar en una entidad
	 *                           relacionada a la principal
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param valor1             : Valor1 del campo para realizar la busqueda en
	 *                           caso de realizar un between
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param truncado           : Indica si el campo debe ser truncado o no. 1 =
	 *                           SI, 0 = NO
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 * @param campoAdicional     : Indica si se desea buscar por un campo de una
	 *                           entidad relacionada. 1 = SI, 0 = NO
	 */
	public DatosBusqueda(int tipoDato, String campo, String campo1,
			String valor, String valor1, int tipoComparacion,
			int truncado, int tipoOperadorLogico, int campoAdicional) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.campo1 = campo1;
		this.valor = valor;
		this.valor1 = valor1;
		this.tipoComparacion = tipoComparacion;
		this.truncado = truncado;
		this.tipoOperadorLogico = tipoOperadorLogico;
		this.campoAdicional = campoAdicional;
	}

	/**
	 * Constructor para consulta simple. Sin truncados, con comparadores AND y sin
	 * campos de otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param tipoDato        : Tipo de dato del campo. Tomado del rubro 70
	 * @param campo           : Nombre del campo sobre el que se quiere buscar
	 * @param valor           : Valor del campo para realizar la busqueda
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 */
	public DatosBusqueda(int tipoDato, String campo, String valor,
			int tipoComparacion) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
	}

	/**
	 * Constructor para consulta simple. Con opcion a truncados, con comparadores
	 * AND y sin campos de otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param tipoDato        : Tipo de dato del campo. Tomado del rubro 70
	 * @param campo           : Nombre del campo sobre el que se quiere buscar
	 * @param valor           : Valor del campo para realizar la busqueda
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 * @param truncado        : Indica si el campo debe ser truncado o no. 1 = SI, 0
	 *                        = NO
	 */
	public DatosBusqueda(int tipoDato, String campo, String valor,
			int tipoComparacion, int truncado) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.truncado = truncado;
	}

	/**
	 * Constructor para consulta simple. Con opcion a truncados, y opcion a
	 * comparador l�gico AND o OR y sin campos de otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param truncado           : Indica si el campo debe ser truncado o no. 1 =
	 *                           SI, 0 = NO
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(int tipoDato, String campo, String valor,
			int tipoComparacion, int truncado, int tipoOperadorLogico) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.truncado = truncado;
		this.tipoOperadorLogico = tipoOperadorLogico;
	}

	/**
	 * Constructor para consulta simple. Sin truncados, y opcion a comparador l�gico
	 * AND o OR y sin campos de otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, int tipoOperadorLogico) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.tipoOperadorLogico = tipoOperadorLogico;
	}

	/**
	 * Constructor para consulta simple. Sin truncados, con comparador l�gico AND,
	 * sin campos de otras entidades, y con comparaci�n BETWEEN.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo           : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato        : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor           : Valor del campo para realizar la busqueda
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 * @param valor1          : Valor1 del campo para realizar la busqueda en caso
	 *                        de realizar un between
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, String valor1) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.valor1 = valor1;
	}

	/**
	 * Consulta con opci�n a truncado, con comparador l�gico AND, sin campos de
	 * otras entidades, y con comparaci�n BETWEEN.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo           : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato        : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor           : Valor del campo para realizar la busqueda
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 * @param valor1          : Valor1 del campo para realizar la busqueda en caso
	 *                        de realizar un between
	 * @param truncado        : Indica si el campo debe ser truncado o no. 1 = SI, 0
	 *                        = NO
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, String valor1, int truncado) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.valor1 = valor1;
		this.truncado = truncado;
	}

	/**
	 * Consulta sin truncados, y opcion a comparador l�gico AND o OR y con campos de
	 * otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, int tipoOperadorLogico, int campoAdicional,
			String campo1) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.tipoOperadorLogico = tipoOperadorLogico;
		this.campoAdicional = campoAdicional;
		this.campo1 = campo1;
	}

	/**
	 * Consulta sin truncados, con comparador l�gico AND y con campos de otras
	 * entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param campo1             : Nombre del campo sobre el que se quiere buscar.
	 *                           En caso que se desee buscar en una entidad
	 *                           relacionada a la principal
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(int tipoDato, String campo, String campo1,
			String valor, int tipoComparacion) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.campo1 = campo1;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.campoAdicional = SI_CAMPO1;
	}

	/**
	 * Consulta con opcion a truncados, y opcion a comparador l�gico AND o OR y con
	 * campos de otras entidades.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, int tipoOperadorLogico, int campoAdicional,
			String campo1, int truncado) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.tipoOperadorLogico = tipoOperadorLogico;
		this.campoAdicional = campoAdicional;
		this.campo1 = campo1;
		this.truncado = truncado;
	}

	/**
	 * Consulta con opcion a truncados, y opcion a comparador l�gico AND o OR, con
	 * campos de otras entidades y BETWEEN.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo              : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato           : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor              : Valor del campo para realizar la busqueda
	 * @param tipoComparacion    : Codigo alterno para el tipo de comparacion a
	 *                           realizar. Tomado del rubro 71
	 * @param tipoOperadorLogico : Indica el tipo de operador que se usa para la
	 *                           union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public DatosBusqueda(String campo, int tipoDato, String valor,
			int tipoComparacion, int tipoOperadorLogico, int campoAdicional,
			String campo1, int truncado, String valor1) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.tipoOperadorLogico = tipoOperadorLogico;
		this.campoAdicional = campoAdicional;
		this.campo1 = campo1;
		this.truncado = truncado;
		this.valor1 = valor1;
	}

	/**
	 * Consulta con opci�n a truncado, con comparador l�gico AND, con campos de
	 * otras entidades, y con comparaci�n BETWEEN.
	 * Solo se deben incluir los campos y valores con los que se desea realizar la
	 * busqueda.
	 * Si no se desea filtrar por cierto campo de la entidad entonces no se debe
	 * incluir.
	 * 
	 * @param campo           : Nombre del campo sobre el que se quiere buscar
	 * @param tipoDato        : Tipo de dato del campo. Tomado del rubro 70
	 * @param valor           : Valor del campo para realizar la busqueda
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 * @param valor1          : Valor1 del campo para realizar la busqueda en caso
	 *                        de realizar un between
	 * @param truncado        : Indica si el campo debe ser truncado o no. 1 = SI, 0
	 *                        = NO
	 */
	public DatosBusqueda(String campo, String valor, int tipoDato,
			int tipoComparacion, String valor1, int truncado,
			int campoAdicional, String campo1) {
		super();
		this.tipoDato = tipoDato;
		this.campo = campo;
		this.valor = valor;
		this.tipoComparacion = tipoComparacion;
		this.valor1 = valor1;
		this.truncado = truncado;
		this.campoAdicional = campoAdicional;
		this.campo1 = campo1;
	}

	/**
	 * Constructor para incluir un campo en el order by.
	 * Solo tiene el nombre del campo para indicar que se desea realizar el orderBy
	 * con este campo.
	 * 
	 * @param campo : Nombre del campo sobre el que se quiere buscar
	 */
	public DatosBusqueda(String campo) {
		super();
		this.campo = campo;
		this.campoOrderBy = SOLO_CAMPO_ORDER_BY;
		this.tipoOrden = ORDER_ASC;
	}

	/**
	 * Constructor para incluir parentesis en la busqueda.
	 * Solo tiene el tipo de parentesis a utilizar si es el de apertura y cierre.
	 * 
	 * @param parentesis : Tipo de parentesis tomado del rubro TipoComandoBusqueda
	 */
	public DatosBusqueda(int tipoParentesis) {
		super();
		this.tipoOperadorLogico = tipoParentesis;
		this.parentesis = USA_PARENTESIS;
	}

	/**
	 * Constructor para incluir un campo en el order by, pero con el tipo de orden
	 * si ascendente o descendente.
	 * Solo tiene el nombre del campo para indicar que se desea realizar el orderBy
	 * con este campo.
	 * 
	 * @param campo     : Nombre del campo sobre el que se quiere buscar
	 * @param tipoOrden : Tipo de orden del order by. 1 = Ascendente, 2 =
	 *                  Descendente.
	 */
	public DatosBusqueda(String campo, int tipoOrden) {
		super();
		this.campo = campo;
		this.campoOrderBy = SOLO_CAMPO_ORDER_BY;
		this.tipoOrden = tipoOrden;
	}

	/**
	 * Obtiene el Tipo de dato del campo. Tomado del rubro 70
	 * 
	 * @return: Tipo de dato del campo con el que se desea realizar el select
	 */
	public int getTipoDato() {
		return tipoDato;
	}

	/**
	 * Setea el Tipo de dato del campo. Tomado del rubro 70
	 * 
	 * @param tipoDato : Tipo de dato del campo con el que se desea realizar el
	 *                 select
	 */
	public void setTipoDato(int tipoDato) {
		this.tipoDato = tipoDato;
	}

	/**
	 * Obtiene el Nombre del campo con el que se desea realizar la buscar
	 * 
	 * @return: Nombre del campo con el que se desea realizar la buscar
	 */
	public String getCampo() {
		return campo;
	}

	/**
	 * Setea el Nombre del campo con el que se desea realizar la buscar
	 * 
	 * @param campo : Nombre del campo con el que se desea realizar la buscar
	 */
	public void setCampo(String campo) {
		this.campo = campo;
	}

	/**
	 * Obtiene el Nombre del campo sobre el que se quiere buscar. En caso que se
	 * desee buscar en una entidad relacionada a la principal
	 * 
	 * @return : Nombre del campo sobre el que se quiere buscar. En caso que se
	 *         desee buscar en una entidad relacionada a la principal
	 */
	public String getCampo1() {
		return campo1;
	}

	/**
	 * Setea el Nombre del campo sobre el que se quiere buscar. En caso que se desee
	 * buscar en una entidad relacionada a la principal
	 * 
	 * @param campo1 : Setea el Nombre del campo sobre el que se quiere buscar. En
	 *               caso que se desee buscar en una entidad relacionada a la
	 *               principal
	 */
	public void setCampo1(String campo1) {
		this.campo1 = campo1;
	}

	/**
	 * Obtiene el Valor del campo para realizar la busqueda
	 * 
	 * @return: Valor del campo para realizar la busqueda
	 */
	public String getValor() {
		return valor;
	}

	/**
	 * Setea el Valor del campo para realizar la busqueda
	 * 
	 * @param valor : Valor del campo para realizar la busqueda
	 */
	public void setValor(String valor) {
		this.valor = valor;
	}

	/**
	 * Obtiene Valor1 del campo para realizar la busqueda en caso de realizar un
	 * between
	 * 
	 * @return: Valor1 del campo para realizar la busqueda en caso de realizar un
	 *          between
	 */
	public String getValor1() {
		return valor1;
	}

	/**
	 * Setea Valor1 del campo para realizar la busqueda en caso de realizar un
	 * between
	 * 
	 * @param valor1 : Valor1 del campo para realizar la busqueda en caso de
	 *               realizar un between
	 */
	public void setValor1(String valor1) {
		this.valor1 = valor1;
	}

	/**
	 * Obtiene Codigo alterno para el tipo de comparacion a realizar. Tomado del
	 * rubro 71
	 * 
	 * @return: Codigo alterno para el tipo de comparacion a realizar. Tomado del
	 *          rubro 71
	 */
	public int getTipoComparacion() {
		return tipoComparacion;
	}

	/**
	 * Setea Codigo alterno para el tipo de comparacion a realizar. Tomado del rubro
	 * 71
	 * 
	 * @param tipoComparacion : Codigo alterno para el tipo de comparacion a
	 *                        realizar. Tomado del rubro 71
	 */
	public void setTipoComparacion(int tipoComparacion) {
		this.tipoComparacion = tipoComparacion;
	}

	/**
	 * Obtiene campo que Indica si el campo debe ser truncado o no. 1 = SI, 0 = NO
	 * 
	 * @return: Valor que Indica si el campo debe ser truncado o no. 1 = SI, 0 = NO
	 */
	public int getTruncado() {
		return truncado;
	}

	/**
	 * Setea el campo que Indica si el campo debe ser truncado o no. 1 = SI, 0 = NO
	 * 
	 * @param truncado : campo que Indica si el campo debe ser truncado o no. 1 =
	 *                 SI, 0 = NO
	 */
	public void setTruncado(int truncado) {
		this.truncado = truncado;
	}

	/**
	 * Obtiene el campo que indica el tipo de operador que se usa para la union
	 * entre condicines.
	 * Puede ser And o Or y se toma del rubro 71.
	 * 
	 * @return: Valor que Indica el tipo de operador que se usa para la union entre
	 *          condicines.
	 *          Puede ser And o Or y se toma del rubro 71.
	 */
	public int getTipoOperadorLogico() {
		return tipoOperadorLogico;
	}

	/**
	 * Setea el campo que indica el tipo de operador que se usa para la union entre
	 * condicines.
	 * Puede ser And o Or y se toma del rubro 71.
	 * 
	 * @param tipoOperadorLogico : Valor que Indica el tipo de operador que se usa
	 *                           para la union entre condicines.
	 *                           Puede ser And o Or y se toma del rubro 71.
	 */
	public void setTipoOperadorLogico(int tipoOperadorLogico) {
		this.tipoOperadorLogico = tipoOperadorLogico;
	}

	/**
	 * Obtiene el campo que Indica si se desea buscar por un campo de una entidad
	 * relacionada. 1 = SI, 0 = NO
	 * 
	 * @return : Indica si se desea buscar por un campo de una entidad relacionada.
	 *         1 = SI, 0 = NO
	 */
	public int getCampoAdicional() {
		return campoAdicional;
	}

	/**
	 * Setea el campo que Indica si se desea buscar por un campo de una entidad
	 * relacionada. 1 = SI, 0 = NO
	 * 
	 * @param campoAdicional : Indica si se desea buscar por un campo de una entidad
	 *                       relacionada. 1 = SI, 0 = NO
	 */
	public void setCampoAdicional(int campoAdicional) {
		this.campoAdicional = campoAdicional;
	}

	/**
	 * Obtiene el Campo que indica si es que el campo se toma en el orderBy.
	 * Se pondr� el order by segun el orden en el que se coloque el campo en el
	 * arreglo de tipo DatosBusqueda.
	 * 
	 * @return : Campo que indica si es que el campo se toma en el orderBy.
	 */
	public int getCampoOrderBy() {
		return campoOrderBy;
	}

	/**
	 * Asigna el Campo que indica si es que el campo se toma en el orderBy.
	 * Se pondr� el order by segun el orden en el que se coloque el campo en el
	 * arreglo de tipo DatosBusqueda.
	 * 
	 * @param campoOrderBy : Campo que indica si es que el campo se toma en el
	 *                     orderBy.
	 */
	public void setCampoOrderBy(int campoOrderBy) {
		this.campoOrderBy = campoOrderBy;
	}

	/**
	 * Obtiene el tipo de orden para los order by. Puede ser ascendete o
	 * descendente.
	 * 
	 * @return
	 */
	public int getTipoOrden() {
		return tipoOrden;
	}

	/**
	 * Asigna el tipo de orden para los order by. Puede ser ascendete o descendente.
	 * 
	 * @param tipoOrden
	 */
	public void setTipoOrden(int tipoOrden) {
		this.tipoOrden = tipoOrden;
	}

	/**
	 * Obtiene Campo que indica si se va a insertar un parentesis
	 * 
	 * @return
	 */
	public int getParentesis() {
		return parentesis;
	}

	/**
	 * Asigna Campo que indica si se va a insertar un parentesis
	 * 
	 * @param parentesis
	 */
	public void setParentesis(int parentesis) {
		this.parentesis = parentesis;
	}

	/**
	 * @return
	 */
	public int getNumeroCampoRepetido() {
		return numeroCampoRepetido;
	}

	/**
	 * @param numeroCampoRepetido
	 */
	public void setNumeroCampoRepetido(int numeroCampoRepetido) {
		this.numeroCampoRepetido = numeroCampoRepetido;
	}

}