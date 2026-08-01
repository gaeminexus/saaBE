package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.AnticipoProveedor;

import jakarta.ejb.Local;

/**
 * Servicio para anticipos entregados a proveedores.
 *
 * Estados:
 *   1 = Ingresado  (grabado, pendiente de confirmación)
 *   2 = Confirmado (asiento contable generado)
 *   3 = Anulado
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
     * Procesa un anticipo a proveedor en un único paso:
     * graba el registro, genera el asiento contable y lo confirma (estado=2).
     *
     * Asiento:
     *   DEBE:  Cuenta anticipos del rol proveedor del titular  (PersonaCuentaContable tipoCuenta=2, rol Proveedor)
     *   HABER: PlanCuenta de la CuentaBancaria indicada
     *
     * @param idTitular        Código del proveedor (Titular)
     * @param valor            Valor del anticipo
     * @param idCuentaBancaria ID de la CuentaBancaria desde la que se paga
     * @param idEmpresa        ID de la empresa contable
     * @param idUsuario        Código del usuario que registra
     * @param fechaAnticipo    Fecha del anticipo (ISO: yyyy-MM-dd)
     * @param numeroDoc        Número de documento de referencia (opcional)
     * @param observacion      Observación (opcional)
     * @return Mapa con: exito, mensaje, anticipo, asiento (numeroAlterno)
     */
    Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion) throws Throwable;
}
