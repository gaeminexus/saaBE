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
}
