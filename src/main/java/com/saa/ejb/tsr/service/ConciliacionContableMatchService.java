/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.tsr.DetalleExtractoBancario;
import com.saa.model.tsr.GrupoConciliacionContable;
import com.saa.model.tsr.ResumenConciliacionCuenta;
import com.saa.model.tsr.SugerenciaConciliacionContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Lógica de negocio de la conciliación contable (extracto bancario vs.
 * contabilidad): listar pendientes, crear/deshacer grupos de conciliación
 * (con las validaciones de monto y fecha), y sugerir coincidencias
 * automáticas. Separado de los servicios CRUD simples
 * (ConciliacionContableService, GrupoConciliacionContableService, etc.) para
 * mantener esa lógica en un solo lugar.</p>
 */
@Local
public interface ConciliacionContableMatchService {

    /**
     * Filas de DetalleExtractoBancario de una cuenta/período que aún no
     * están en ningún grupo activo.
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : Filas pendientes
     * @throws Throwable       : Excepcion
     */
    List<DetalleExtractoBancario> obtenerPendientesExtracto(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Filas de DetalleAsiento (cuenta contable de la cuenta bancaria, dentro
     * del período) que aún no están en ningún grupo activo.
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : Filas pendientes
     * @throws Throwable       : Excepcion
     */
    List<DetalleAsiento> obtenerPendientesAsiento(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Crea un grupo de conciliación con las filas indicadas de ambos lados,
     * validando que: (a) ninguna fila ya pertenezca a otro grupo activo,
     * (b) el periodo no esté CERRADO, (c) la suma de ambos lados coincida
     * (monto), y (d) todas las fechas involucradas (ambos lados) queden
     * dentro de la tolerancia de días configurada (rubro
     * ASP_TOLERANCIA_DIAS_CONCILIACION_CONTABLE) - monto Y fecha son ambas
     * obligatorias, ninguna es suficiente por sí sola.
     * @param idCuentaBancaria    : Id de la cuenta bancaria
     * @param idPeriodo           : Id del periodo contable
     * @param idsDetalleExtracto  : Ids de DetalleExtractoBancario a conciliar (>= 1)
     * @param idsDetalleAsiento   : Ids de DetalleAsiento a conciliar (>= 1)
     * @param usuario             : Usuario que concilia
     * @return                    : Grupo creado
     * @throws Throwable         : Excepcion (validaciones fallidas, periodo cerrado, etc.)
     */
    GrupoConciliacionContable conciliarGrupo(Long idCuentaBancaria, Long idPeriodo, List<Long> idsDetalleExtracto,
            List<Long> idsDetalleAsiento, String usuario) throws Throwable;

    /**
     * Deshace un grupo ya conciliado - vuelve sus filas al pool de
     * pendientes. No permitido si el periodo ya está CERRADO.
     * @param idGrupo : Id del GrupoConciliacionContable a deshacer
     * @param usuario : Usuario que deshace
     * @throws Throwable : Excepcion
     */
    void deshacerGrupo(Long idGrupo, String usuario) throws Throwable;

    /**
     * Corre una pasada de auto-conciliación (1:1 exacto, luego N:1/1:N
     * acotado a una ventana de fechas pequeña) sobre los pendientes de una
     * cuenta/período, y devuelve las coincidencias encontradas SIN
     * persistirlas - el usuario las confirma una por una (o todas) llamando
     * a conciliarGrupo con los mismos ids.
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : Sugerencias encontradas
     * @throws Throwable       : Excepcion
     */
    List<SugerenciaConciliacionContable> sugerirCoincidencias(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Resumen de estado de conciliación de TODAS las cuentas bancarias
     * activas de una empresa, para un período dado - una fila por cuenta,
     * con su estado real si ya se empezó a conciliar, o null si esa
     * cuenta/período nunca se ha abierto todavía. De solo lectura: nunca crea
     * la cabecera ConciliacionContable de una cuenta que el usuario no ha
     * abierto (eso solo ocurre al conciliar de verdad, ver obtenerOCrear).
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable
     * @return          : Una fila por cuenta bancaria activa
     * @throws Throwable : Excepcion
     */
    List<ResumenConciliacionCuenta> resumenPorPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable;

    /**
     * Cierra un período, para efectos de conciliación bancaria, pero solo si
     * TODAS las cuentas bancarias activas de la empresa ya están verificadas
     * (ver resumenPorPeriodo) - ninguna pendiente, ninguna con diferencias. Es
     * el "gate" de conciliación bancaria antes de permitir el cierre. Este
     * cierre es EXCLUSIVO de TSR (ControlExtractoBancario.cerrado) y no toca
     * ni depende del proceso de mayorización/cierre propio de CNT.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable a cerrar
     * @param usuario   : Usuario que cierra (auditoria)
     * @throws Throwable : Excepcion (si alguna cuenta no esta verificada, o el periodo ya estaba cerrado)
     */
    void cerrarMes(Long idEmpresa, Long idPeriodo, String usuario) throws Throwable;

    /**
     * Reabre un período previamente cerrado para conciliación bancaria - sin
     * validaciones adicionales (reabrir siempre debe ser posible mientras
     * este cerrado). Cierre exclusivo de TSR, ver cerrarMes.
     * @param idEmpresa : Id de la empresa
     * @param idPeriodo : Id del periodo contable a reabrir
     * @throws Throwable : Excepcion (si el periodo no estaba cerrado)
     */
    void reabrirMes(Long idEmpresa, Long idPeriodo) throws Throwable;

}
