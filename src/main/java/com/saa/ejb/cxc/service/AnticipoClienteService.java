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
}
