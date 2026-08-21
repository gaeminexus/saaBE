package com.saa.ejb.rhh.service;

import java.time.LocalDate;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.NovedadIess;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad NovedadIess.
 *  Accede a los metodos DAO y procesa los datos para el NovedadIess.</p>
 *
 * <p>Ademas del CRUD, genera las novedades que hay que reportar al IESS. Los plazos
 * legales <b>no estan escritos en este codigo</b>: salen del PDTRVLRN del detalle
 * correspondiente del rubro RHH_TIPO_NOVEDAD_IESS (204), que carga el script 06.</p>
 */
@Local
public interface NovedadIessService extends EntityService<NovedadIess> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	NovedadIess selectById(Long id) throws Throwable;

	/**
	 * Genera el aviso de entrada al crear un contrato. El plazo legal se lee del
	 * detalle AVISO_DE_ENTRADA del rubro 204.
	 *
	 * @param idContrato	: Id del contrato que origina el aviso
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad generada, en estado PENDIENTE
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarAvisoEntrada(Long idContrato, String usuario) throws Throwable;

	/**
	 * Genera el aviso de salida al ejecutar una liquidacion. El plazo legal se lee del
	 * detalle AVISO_DE_SALIDA del rubro 204.
	 *
	 * @param idLiquidacion	: Id de la liquidacion que origina el aviso
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad generada, en estado PENDIENTE
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarAvisoSalida(Long idLiquidacion, String usuario) throws Throwable;

	/**
	 * Genera la novedad de modificacion de sueldo. El plazo legal se lee del detalle
	 * MODIFICACION_DE_SUELDO del rubro 204.
	 *
	 * @param idContrato		: Id del contrato cuyo sueldo cambia
	 * @param sueldoAnterior	: Sueldo vigente antes del cambio
	 * @param sueldoNuevo		: Sueldo vigente despues del cambio
	 * @param vigencia			: Fecha desde la que rige el nuevo sueldo
	 * @param usuario			: Usuario que registra
	 * @return					: La novedad generada, en estado PENDIENTE
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarModificacionSueldo(Long idContrato, Double sueldoAnterior,
			Double sueldoNuevo, LocalDate vigencia, String usuario) throws Throwable;

	/**
	 * Genera la novedad de variacion de sueldo por extras del periodo.
	 *
	 * <p><b>Que es una variacion no lo decide el concepto, lo decide la diferencia.</b>
	 * Para el IESS es variacion todo imponible que supere el sueldo declarado, venga de
	 * horas extras, de una subrogacion, de un encargo o de un bono ocasional; por eso no
	 * hace falta marcar los conceptos como permanentes o no, y por eso tampoco se puede
	 * deducir del catalogo. El importe es
	 * <code>imponible del mes - sueldo declarado x dias / 30</code>, y solo se genera
	 * novedad cuando sale positivo.</p>
	 *
	 * <p>Es idempotente dentro de la ventana del periodo: si ya existe la novedad de
	 * este contrato y este periodo, se actualiza en vez de duplicarse, salvo que ya se
	 * haya enviado al IESS --entonces no se toca y se deja constancia--.</p>
	 *
	 * @param idContrato		: Id del contrato
	 * @param fechaHecho		: Fecha de fin del periodo, que es cuando el hecho queda firme
	 * @param valorVariacion	: Importe imponible por encima del sueldo declarado
	 * @param desde				: Fecha de inicio del periodo, para localizar la novedad existente
	 * @param hasta				: Fecha de fin del periodo, para localizar la novedad existente
	 * @param usuario			: Usuario que registra
	 * @return					: La novedad generada o actualizada, o null si no se toco
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarVariacionPorExtras(Long idContrato, LocalDate fechaHecho, Double valorVariacion,
			LocalDate desde, LocalDate hasta, String usuario) throws Throwable;

	/**
	 * Genera la novedad de cambio de relacion de trabajo o de actividad sectorial.
	 *
	 * @param idContrato		: Id del contrato
	 * @param vigencia			: Fecha desde la que rige el cambio
	 * @param detalle			: Que cambio exactamente, para la observacion
	 * @param usuario			: Usuario que registra
	 * @return					: La novedad generada, en estado PENDIENTE
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarCambioRelacionTrabajo(Long idContrato, LocalDate vigencia, String detalle,
			String usuario) throws Throwable;

	/**
	 * Genera la novedad de cambio de jornada.
	 *
	 * <p>Se modela aparte de la modificacion de sueldo aunque el portal la registre por
	 * la misma opcion: un cambio de jornada mueve tambien los <b>dias declarados</b> y,
	 * con ellos, el seguro de salud de tiempo parcial. Tratarla como un sueldo nuevo
	 * dejaria los dias sin actualizar y la planilla saldria con el numero viejo.</p>
	 *
	 * @param idContrato		: Id del contrato
	 * @param vigencia			: Fecha desde la que rige el cambio
	 * @param sueldoAnterior	: Sueldo referencial anterior
	 * @param sueldoNuevo		: Sueldo referencial nuevo
	 * @param diasDeclarados	: Dias que se declararan al IESS tras el cambio
	 * @param aviso				: Texto que se anade a la observacion; vacio si no hay nada que advertir
	 * @param usuario			: Usuario que registra
	 * @return					: La novedad generada, en estado PENDIENTE
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess generarCambioJornada(Long idContrato, LocalDate vigencia, Double sueldoAnterior,
			Double sueldoNuevo, Long diasDeclarados, String aviso, String usuario) throws Throwable;

	/**
	 * Marca la novedad como enviada al IESS y sella su fecha de reporte.
	 *
	 * <p><b>Aqui se vuelve a resolver el codigo IESS de la causa desde el rubro, justo
	 * antes de sellarlo.</b> Las novedades se crean con el codigo que el catalogo tuviera
	 * en ese momento, y hasta que alguien leyo los anexos del portal ese codigo era
	 * <code>'?'</code>. Resolverlo de nuevo al enviar consigue las dos cosas a la vez: lo
	 * que se mando queda grabado tal cual --una novedad ya enviada no se vuelve a tocar--
	 * y ninguna novedad vieja arrastra un <code>'?'</code> que ya se puede resolver.</p>
	 *
	 * @param idNovedad		: Id de la novedad
	 * @param lote			: Lote o comprobante del envio; opcional
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad actualizada
	 * @throws Throwable	: IncomeException si no existe o si su estado no admite el envio
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess marcarEnviada(Long idNovedad, String lote, String usuario) throws Throwable;

	/**
	 * Marca la novedad como aceptada por el IESS.
	 *
	 * @param idNovedad		: Id de la novedad
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad actualizada
	 * @throws Throwable	: IncomeException si no existe o si su estado no admite la aceptacion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess marcarAceptada(Long idNovedad, String usuario) throws Throwable;

	/**
	 * Marca la novedad como rechazada por el IESS y guarda el motivo.
	 *
	 * <p>El motivo es obligatorio: una novedad rechazada sin motivo no se puede corregir,
	 * y esta ademas cuenta como pendiente para la regla que impide cerrar el periodo, asi
	 * que dejaria el cierre bloqueado sin decir por que.</p>
	 *
	 * @param idNovedad		: Id de la novedad
	 * @param motivo		: Respuesta del IESS
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad actualizada
	 * @throws Throwable	: IncomeException si no existe, si el estado no lo admite o si falta el motivo
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess marcarRechazada(Long idNovedad, String motivo, String usuario) throws Throwable;

	/**
	 * Anula la novedad: no habia que reportarla.
	 *
	 * <p>Anular no es borrar, y es deliberado: la novedad se genero por algo y su rastro
	 * explica por que el mes se pudo cerrar. Una anulada deja de contar para la regla de
	 * cierre pero sigue estando.</p>
	 *
	 * @param idNovedad		: Id de la novedad
	 * @param motivo		: Por que no correspondia reportarla
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad actualizada
	 * @throws Throwable	: IncomeException si no existe o si ya fue aceptada por el IESS
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess anular(Long idNovedad, String motivo, String usuario) throws Throwable;

	/**
	 * Registra a mano una novedad que el sistema no genera solo, <b>calculandole el plazo
	 * legal</b>.
	 *
	 * <p><b>Por que no vale el POST del CRUD.</b> El CRUD generico graba lo que le llega, y
	 * lo que llega no trae <code>NVISFCLM</code>: la novedad nace sin fecha limite. Una
	 * novedad sin plazo no aparece vencida en ninguna pantalla, no tiene dias restantes que
	 * mirar y es <b>exactamente la que se escapa</b> — el agujero de marzo con otro disfraz,
	 * sólo que esta vez sin ni siquiera la fecha que habría delatado el retraso.</p>
	 *
	 * <p>El plazo sale del <code>PDTRVLRN</code> del rubro 204, igual que en la generacion
	 * automatica: un solo sitio decide cuantos dias hay para reportar cada tipo.</p>
	 *
	 * @param novedad		: Novedad a registrar; se exigen empleado, tipo y fecha del hecho
	 * @param usuario		: Usuario que registra
	 * @return				: La novedad creada, en estado PENDIENTE y con su fecha limite
	 * @throws Throwable	: IncomeException si falta un dato o el plazo no esta parametrizado
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	NovedadIess registrar(NovedadIess novedad, String usuario) throws Throwable;

}
