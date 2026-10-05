package com.saa.ejb.cxp.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Documentos de seguros en CxP (docs/logica-negocio/cxp/API-DOCUMENTOS-SEGUROS-CXP.md): el
 * bloqueo de pago de una factura hasta que crédito distribuye la prima en las cuotas de los
 * préstamos, y los servicios con los que crédito busca, enlaza, libera y desenlaza esos
 * documentos de su póliza (CRD.POSG).
 * <p>
 * §5.0: servicio {@code @Local} — crédito llama estas cinco operaciones (porClave es de sólo
 * lectura y no lo necesita) dentro de su propia transacción, para que su cambio de estado y
 * nuestro enlazar/liberar/desenlazar sean atómicos. Los métodos que escriben reciben
 * {@code String usuario} (nombre), resuelto con {@code UsuarioDaoService.selectByNombre}; el REST
 * sigue recibiendo {@code idUsuario} y hace esa conversión antes de llamar acá.
 * <p>
 * ⚠️ {@code IncomeException} es {@code @ApplicationException(rollback=true)}: si una validación de
 * acá la lanza, la transacción de crédito queda marcada para rollback aunque la atrape (§58.1).
 * Es lo buscado -- crédito no debe atraparla para seguir.
 */
@Local
public interface DocumentoSeguroCxpService {

	/**
	 * §5.1 — Busca un documento por clave de acceso del SRI, primero registrado (FCTC/NTDC/NTCC),
	 * y si no está, en la bandeja de carga (PGS.DCXP) por {@code DCXPCLAC}. Siempre devuelve un
	 * mapa, nunca lanza si no encuentra ({@code encontrado: false}).
	 * @param claveAcceso : Clave de acceso de 49 dígitos del SRI
	 * @param idEmpresa   : Empresa contable de CxP (el {@code PJRQCDGO} con el que se cargó el documento)
	 * @return            : Ver forma de respuesta en el contrato §5.1
	 * @throws Throwable  : Excepcion
	 */
	Map<String, Object> porClave(String claveAcceso, Long idEmpresa) throws Throwable;

	/**
	 * §5.2 — Crea el documento desde el XML, ya marcado de seguros (D2). Si ya existe un
	 * documento registrado con esa clave, NO lo duplica: responde con {@code yaExistia: true} y
	 * no cambia nada.
	 * @param contenidoXml      : XML del comprobante
	 * @param idEmpresa         : Empresa contable de CxP
	 * @param usuario           : Nombre de usuario que registra
	 * @param idDocumentoSeguro : POSGCDGO a enlazar; null si todavía no se conoce
	 * @return                  : Ver forma de respuesta en el contrato §5.2
	 * @throws Throwable        : IncomeException si el usuario no existe; otras excepciones de la
	 *                            carga normal (proveedor sin cuenta, XML inválido) se propagan igual
	 */
	Map<String, Object> registrarDesdeXml(String contenidoXml, Long idEmpresa, String usuario,
			Long idDocumentoSeguro) throws Throwable;

	/**
	 * §5.3 — Enlaza el documento a la póliza y lo marca de seguros si todavía no lo estaba. Si la
	 * factura (o la ND ya aplicada) tiene pagos, queda enlazada pero LIBERADA de una vez: el
	 * dinero ya salió.
	 * @param tipoDocumento     : FACTURA | NOTA_DEBITO | NOTA_CREDITO
	 * @param idDocumento       : Id del documento (FCTC/NTDC/NTCC según tipoDocumento)
	 * @param idDocumentoSeguro : POSGCDGO a enlazar
	 * @param usuario           : Nombre de usuario
	 * @return                  : La forma de §5.1 más {@code aviso} (o null)
	 * @throws Throwable        : IncomeException si ya está enlazado a otro POSGCDGO, o el usuario no existe
	 */
	Map<String, Object> enlazar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable;

	/**
	 * §5.4 — Libera el documento a pago. Para una ND, además la aplica recién ahora a la factura
	 * afectada (sube su saldo).
	 * @param tipoDocumento     : FACTURA | NOTA_DEBITO | NOTA_CREDITO
	 * @param idDocumento       : Id del documento
	 * @param idDocumentoSeguro : Debe coincidir con el enlazado
	 * @param usuario           : Nombre de usuario
	 * @return                  : La forma de §5.1
	 * @throws Throwable        : IncomeException si el enlace no coincide, el estado no es
	 *                            BLOQUEADO, o (ND) la factura afectada no se resuelve
	 */
	Map<String, Object> liberar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable;

	/**
	 * §5.5 — Desenlaza y vuelve a bloquear (D8). Para una factura con pagos, o una ND cuya
	 * factura ya tiene pagos, se rechaza: el dinero ya salió o está comprometido.
	 * @param tipoDocumento     : FACTURA | NOTA_DEBITO | NOTA_CREDITO
	 * @param idDocumento       : Id del documento
	 * @param idDocumentoSeguro : Debe coincidir con el enlazado
	 * @param usuario           : Nombre de usuario
	 * @return                  : La forma de §5.1
	 * @throws Throwable        : IncomeException si el enlace no coincide, o el documento (o su
	 *                            factura afectada, si es ND) ya tiene pagos
	 */
	Map<String, Object> desenlazar(String tipoDocumento, Long idDocumento, Long idDocumentoSeguro,
			String usuario) throws Throwable;

}
