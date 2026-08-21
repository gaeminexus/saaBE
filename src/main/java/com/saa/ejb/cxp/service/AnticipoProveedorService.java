package com.saa.ejb.cxp.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AnticipoProveedor;

import jakarta.ejb.Local;

/**
 * Servicio para anticipos entregados a proveedores.
 *
 * Estados ({@link com.saa.rubros.EstadoAnticipoProveedor}):
 *   1 = Ingresado  (grabado, con su pago pendiente en el circuito)
 *   2 = Confirmado (pago confirmado: asiento, movimiento bancario y saldo)
 *   3 = Anulado
 *
 * El anticipo pasa por el mismo circuito de pagos que los egresos de
 * tesorería y las facturas de compra: al procesarlo se crea su
 * PagoProgramado (PGS.PGTR con FK al anticipo), que aparece en el listado
 * de pagos a realizar y sigue lote → archivo → confirmación. La contabilidad
 * (asiento de anticipo, NO de egreso) se genera recién cuando el banco
 * confirma el pago. Con débito automático todo ocurre en el mismo paso.
 */
@Local
public interface AnticipoProveedorService extends EntityService<AnticipoProveedor> {

    /**
     * Busca un anticipo por su ID.
     */
    AnticipoProveedor selectById(Long id) throws Throwable;

    /**
     * Graba o actualiza un anticipo. En creación asigna estado=1 y fechaRegistro automática.
     */
    AnticipoProveedor saveSingle(AnticipoProveedor entidad) throws Throwable;

    /**
     * Busca anticipos por criterios dinámicos.
     */
    List<AnticipoProveedor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable;

    /**
     * Devuelve todos los anticipos activos de un proveedor en una empresa.
     */
    List<AnticipoProveedor> selectByTitularEmpresa(Long codigoTitular, Long idEmpresa) throws Throwable;

    /**
     * Registra un anticipo a proveedor y crea su pago en el circuito de
     * PagoProgramado en el mismo paso.
     *
     * Si NO es débito automático el anticipo queda Ingresado y su pago
     * Registrado: aparece en el listado de pagos a realizar y sigue
     * lote → archivo → confirmación. Recién al confirmarse el banco se
     * genera el asiento de anticipo, el movimiento bancario y se acredita
     * el saldo de anticipos del proveedor.
     *
     * Si es débito automático, el pago nace Confirmado y todo eso ocurre
     * en esta misma llamada.
     *
     * Asiento (al confirmar):
     *   DEBE:  Cuenta anticipos del rol proveedor del titular  (PersonaCuentaContable tipoCuenta=2, rol Proveedor)
     *   HABER: PlanCuenta de la CuentaBancaria indicada
     *
     * @param idTitular              Código del proveedor (Titular)
     * @param valor                  Valor del anticipo
     * @param idCuentaBancaria       ID de la CuentaBancaria propia desde la que se paga
     * @param idEmpresa              ID de la empresa contable
     * @param idUsuario              Código del usuario que registra
     * @param fechaAnticipo          Fecha del anticipo (ISO: yyyy-MM-dd)
     * @param numeroDoc              Número de documento de referencia (opcional)
     * @param observacion            Observación (opcional)
     * @param idCuentaDestinoTitular ID de la cuenta bancaria del proveedor (TSR.CTBN);
     *                               obligatoria salvo débito automático
     * @param debitoAutomatico       true si el banco ya debitó la cuenta
     * @return Mapa con: exito, mensaje, anticipo, pago y (si aplica) asiento
     */
    Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion,
            Long idCuentaDestinoTitular, boolean debitoAutomatico) throws Throwable;

    /**
     * Contabiliza un anticipo cuyo pago acaba de ser confirmado por el banco
     * (o nació confirmado por débito automático): genera el asiento de
     * anticipo, acredita el saldo de anticipos del proveedor (PRCC) y deja el
     * anticipo Confirmado. Lo invoca el circuito de pagos
     * (PagoProgramadoServiceImpl); no genera el movimiento bancario, que es
     * responsabilidad del circuito.
     * @param idAnticipo       : Id del anticipo (debe estar Ingresado)
     * @param idCuentaBancaria : Id de la cuenta bancaria propia del pago
     * @param fechaPago        : Fecha real del pago; es la fecha del asiento
     * @param idUsuario        : Id del usuario que confirma
     * @return                 : Asiento generado
     * @throws Throwable       : Excepcion
     */
    Asiento contabilizarAnticipoConfirmado(Long idAnticipo, Long idCuentaBancaria,
            LocalDate fechaPago, Long idUsuario) throws Throwable;

    /**
     * Reversa la contabilidad de un anticipo Confirmado: anula el asiento,
     * descuenta el saldo de anticipos del proveedor y devuelve el anticipo a
     * Ingresado. Lo invoca el circuito de pagos al revertir el pago; el
     * movimiento bancario lo anula el circuito.
     * @param idAnticipo : Id del anticipo confirmado
     * @param motivo     : Motivo de la reversión
     * @throws Throwable : Excepcion
     */
    void revertirContabilidadAnticipo(Long idAnticipo, String motivo) throws Throwable;

    /**
     * Anula un anticipo SIN aceptar la reversión de cruces con facturas.
     * Equivale a {@link #anularAnticipo(Long, String, Long, boolean)} con
     * {@code confirmaReversionCruces = false}: si el anticipo ya fue cruzado
     * con alguna factura devuelve {@code requiereConfirmacion = true} y el
     * detalle de los cruces en lugar de anular.
     * @param idAnticipo : Id del anticipo
     * @param motivo     : Motivo de la anulación
     * @param idUsuario  : Id del usuario que anula
     * @return           : Mapa con exito y mensaje
     * @throws Throwable : Excepcion
     */
    Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario)
            throws Throwable;

    /**
     * Analiza si un anticipo puede anularse y si su anulación arrastra cruces
     * con facturas. NO modifica nada: es la consulta previa que la pantalla usa
     * para avisar al usuario antes de pedirle la confirmación.
     * <p>
     * El cruce de anticipos descuenta el saldo GLOBAL del proveedor
     * (TSR.PRCC.PRCCSLIN) y no se enlaza al anticipo original, así que
     * "¿este anticipo fue cruzado?" se responde comparando el valor del
     * anticipo contra ese saldo global: si el saldo disponible ya no alcanza
     * para cubrirlo, la diferencia salió por cruces y hay que reversarlos
     * (los más recientes primero) para poder anular.
     * @param idAnticipo : Id del anticipo
     * @return           : Mapa con puedeAnular, requiereConfirmacion, valorAnticipo,
     *                     saldoDisponible, montoACruzar, cruces (lista de facturas
     *                     afectadas) y mensaje
     * @throws Throwable : Excepcion
     */
    Map<String, Object> verificarAnulacion(Long idAnticipo) throws Throwable;

    /**
     * Anula un anticipo en cualquier estado, revirtiendo todo lo que generó.
     * <ul>
     *   <li><b>Ingresado</b>: anula el pago Registrado del circuito; se bloquea
     *       si el pago ya está En archivo (en poder del banco).</li>
     *   <li><b>Confirmado</b>: si el anticipo ya fue cruzado con facturas exige
     *       {@code confirmaReversionCruces}; entonces reversa esos cruces
     *       (devolviendo el saldo a cada factura y anulando su asiento de
     *       cruce), anula el movimiento bancario y el asiento del anticipo,
     *       descuenta el saldo de anticipos del proveedor y anula el pago.</li>
     * </ul>
     * Sin la confirmación devuelve {@code exito=false} y
     * {@code requiereConfirmacion=true} con el detalle de los cruces, igual que
     * {@link #verificarAnulacion(Long)}.
     * @param idAnticipo              : Id del anticipo
     * @param motivo                  : Motivo de la anulación (obligatorio)
     * @param idUsuario               : Id del usuario que anula
     * @param confirmaReversionCruces : true si el usuario aceptó eliminar los
     *                                  abonos que el anticipo hizo a facturas
     * @return                        : Mapa con exito, mensaje y el detalle de lo reversado
     * @throws Throwable              : Excepcion
     */
    Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario,
            boolean confirmaReversionCruces) throws Throwable;

    /**
     * Anticipos del proveedor que todavía tienen saldo para cruzar contra una
     * factura: Confirmados, con saldo disponible mayor a cero, del más antiguo
     * al más nuevo. Es la lista que alimenta la pantalla de cruce.
     * @param idTitular  : Id del proveedor
     * @param idEmpresa  : Id de la empresa contable
     * @return           : Anticipos con saldo disponible
     * @throws Throwable : Excepcion
     */
    List<AnticipoProveedor> selectDisponibles(Long idTitular, Long idEmpresa) throws Throwable;

    /**
     * Estado de cuenta de los anticipos de un proveedor para las pantallas de
     * consulta y seguimiento: cada anticipo con sus fechas, su documento, su
     * asiento y el detalle de los cruces que lo consumieron (activos y
     * reversados, para poder seguir también las anulaciones).
     * <p>
     * Incluye el cuadre entre la suma de los saldos por anticipo y el saldo
     * global de la cuenta contable de anticipos: si no coinciden hay
     * movimientos sin atribuir y la respuesta trae una advertencia.
     * @param idTitular  : Id del proveedor
     * @param idEmpresa  : Id de la empresa contable
     * @return           : Mapa con anticipos, totales, saldos, diferencia y cuadra
     * @throws Throwable : Excepcion
     */
    Map<String, Object> seguimiento(Long idTitular, Long idEmpresa) throws Throwable;
}
