package com.saa.ejb.cxc.service;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.EntityService;
import com.saa.model.cxc.AnticipoCliente;

import jakarta.ejb.Local;

@Local
public interface AnticipoClienteService extends EntityService<AnticipoCliente> {

    /**
     * Busca un anticipo por su ID.
     */
    AnticipoCliente selectById(Long id) throws Throwable;

    /**
     * Graba o actualiza un anticipo y asigna automáticamente la fechaRegistro
     * cuando es nuevo. Si la empresa tiene contabilidad activa, genera el
     * asiento contable correspondiente.
     */
    AnticipoCliente saveSingle(AnticipoCliente entidad) throws Throwable;

    /**
     * Busca anticipos por criterios dinámicos.
     */
    List<AnticipoCliente> selectByCriteria(List<DatosBusqueda> datos) throws Throwable;

    /**
     * Devuelve todos los anticipos activos de un titular en una empresa.
     * @param codigoTitular Código del titular (cliente)
     * @param idEmpresa     ID de la empresa contable
     */
    List<AnticipoCliente> selectByTitularEmpresa(Long codigoTitular, Long idEmpresa) throws Throwable;

    /**
     * Confirma un anticipo en estado Ingresado (1) cambiándolo a Confirmado (2)
     * y genera el asiento contable correspondiente:
     *   DEBE:  cuenta caja/banco   (PersonaCuentaContable tipoCuenta=3, tipoPersona=1)
     *   HABER: cuenta anticipos    (PersonaCuentaContable tipoCuenta=2, tipoPersona=1)
     *
     * Estados: 1=Ingresado, 2=Confirmado, 3=Anulado
     *
     * @param idAnticipo ID del anticipo a confirmar
     * @param usuario    Nombre del usuario que confirma
     * @return           Mapa con: exito, mensaje, asiento (numeroAlterno), anticipo
     * @throws Throwable Si no existe, ya está confirmado/anulado, o faltan cuentas
     */
    java.util.Map<String, Object> confirmarAnticipo(Long idAnticipo, String usuario) throws Throwable;

    /**
     * Procesa un anticipo de cliente en un único paso:
     * graba el registro, genera el asiento contable y lo confirma (estado=2).
     *
     * Asiento:
     *   DEBE:  PlanCuenta de la CuentaBancaria indicada
     *   HABER: Cuenta anticipos del rol cliente del titular (PersonaCuentaContable tipoCuenta=2, tipoPersona=1)
     *
     * @param idTitular        Código del cliente (Titular)
     * @param valor            Valor del anticipo
     * @param idCuentaBancaria ID de la CuentaBancaria en la que se recibe el pago
     * @param idEmpresa        ID de la empresa contable
     * @param idUsuario        Código del usuario que registra
     * @param fechaAnticipo    Fecha del anticipo (ISO: yyyy-MM-dd)
     * @param numeroDoc        Número de documento de referencia (opcional)
     * @param observacion      Observación (opcional)
     * @return Mapa con: exito, mensaje, anticipo, asiento (numeroAlterno)
     */
    java.util.Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion) throws Throwable;

    /**
     * Analiza si un anticipo de cliente puede anularse y si su anulación
     * arrastra cruces con facturas. NO modifica nada: es la consulta previa que
     * la pantalla usa para avisar al usuario antes de pedirle la confirmación.
     * <p>
     * El cruce de anticipos descuenta el saldo GLOBAL del cliente
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
    java.util.Map<String, Object> verificarAnulacion(Long idAnticipo) throws Throwable;

    /**
     * Anula un anticipo de cliente, revirtiendo todo lo que generó.
     * <ul>
     *   <li><b>Ingresado</b>: no tiene asiento ni saldo acreditado; solo pasa a
     *       Anulado.</li>
     *   <li><b>Confirmado</b>: si el anticipo ya fue cruzado con facturas exige
     *       {@code confirmaReversionCruces}; entonces reversa esos cruces
     *       (devolviendo el saldo a cada factura y anulando su asiento de
     *       cruce), anula el movimiento bancario y el asiento del anticipo y
     *       descuenta el saldo de anticipos del cliente.</li>
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
    java.util.Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario,
            boolean confirmaReversionCruces) throws Throwable;

    /**
     * Anticipos del cliente que todavía tienen saldo para cruzar contra una
     * factura: Confirmados, con saldo disponible mayor a cero, del más antiguo
     * al más nuevo. Es la lista que alimenta la pantalla de cruce.
     * @param idTitular  : Id del cliente
     * @param idEmpresa  : Id de la empresa contable
     * @return           : Anticipos con saldo disponible
     * @throws Throwable : Excepcion
     */
    List<AnticipoCliente> selectDisponibles(Long idTitular, Long idEmpresa) throws Throwable;

    /**
     * Estado de cuenta de los anticipos de un cliente para las pantallas de
     * consulta y seguimiento: cada anticipo con sus fechas, su documento, su
     * asiento y el detalle de los cruces que lo consumieron (activos y
     * reversados, para poder seguir también las anulaciones).
     * <p>
     * Incluye el cuadre entre la suma de los saldos por anticipo y el saldo
     * global de la cuenta contable de anticipos: si no coinciden hay
     * movimientos sin atribuir y la respuesta trae una advertencia.
     * @param idTitular  : Id del cliente
     * @param idEmpresa  : Id de la empresa contable
     * @return           : Mapa con anticipos, totales, saldos, diferencia y cuadra
     * @throws Throwable : Excepcion
     */
    java.util.Map<String, Object> seguimiento(Long idTitular, Long idEmpresa) throws Throwable;
}
