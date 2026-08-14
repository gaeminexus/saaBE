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
     * Anula un anticipo pendiente de pago. Si tiene un pago Registrado lo
     * anula también; si el pago está En archivo o Confirmado, la anulación se
     * bloquea (procesar la respuesta del banco o revertir el pago primero).
     * @param idAnticipo : Id del anticipo
     * @param motivo     : Motivo de la anulación
     * @param idUsuario  : Id del usuario que anula
     * @return           : Mapa con exito y mensaje
     * @throws Throwable : Excepcion
     */
    Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario)
            throws Throwable;
}
