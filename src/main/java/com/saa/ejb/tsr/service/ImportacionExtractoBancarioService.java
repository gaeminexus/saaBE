/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.io.InputStream;

import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.ResumenImportacionExtracto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio de importacion de estados de cuenta bancarios (archivo Excel
 * de un banco -> EXBC + DEXB). Selecciona el parser automaticamente segun
 * el banco de la cuenta indicada (ver {@link com.saa.ejb.tsr.parser.BankStatementParserFactory}).</p>
 * <p>Flujo en dos fases, igual al patron ya usado en el modulo de creditos
 * (validarArchivoPetro/procesarArchivoPetro): validar() previsualiza sin
 * persistir, confirmar() reparsea el mismo archivo y persiste. No se
 * mantiene estado entre ambos llamados.</p>
 */
@Local
public interface ImportacionExtractoBancarioService {

    /**
     * Parsea el archivo y arma un resumen de previsualizacion, sin guardar
     * nada en base de datos.
     * @param archivo         : Contenido del archivo
     * @param nombreArchivo   : Nombre original del archivo
     * @param idCuentaBancaria : Cuenta bancaria a la que pertenece el extracto
     * @param idPeriodo       : Periodo contable elegido por el usuario para este extracto
     * @return                : Resumen de la importacion
     * @throws Throwable      : Excepcion (bloqueo duro si el periodo esta CERRADO)
     */
    ResumenImportacionExtracto validar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo) throws Throwable;

    /**
     * Reparsea el archivo y guarda el lote completo (ExtractoBancario +
     * DetalleExtractoBancario) en una sola transaccion. Rechaza el archivo
     * si su hash ya fue cargado previamente (control de duplicados).
     * @param archivo          : Contenido del archivo
     * @param nombreArchivo    : Nombre original del archivo
     * @param idCuentaBancaria : Cuenta bancaria a la que pertenece el extracto
     * @param idPeriodo        : Periodo contable elegido por el usuario para este extracto
     * @param idEmpresa        : Empresa duena de la cuenta
     * @param usuarioCreacion  : Usuario que realiza la carga (auditoria)
     * @return                 : ExtractoBancario guardado
     * @throws Throwable       : Excepcion (bloqueo duro si el periodo esta CERRADO)
     */
    ExtractoBancario confirmar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria, Long idPeriodo,
            Long idEmpresa, String usuarioCreacion) throws Throwable;

    /**
     * Reemplaza el extracto ya cargado de una cuenta/período por uno nuevo: borra la cabecera
     * (EXBC) y su detalle (DEXB) anteriores y vuelve a cargar el archivo indicado con la misma
     * lógica de {@link #confirmar}, todo en una sola transacción. Pedido del usuario: no había
     * forma de corregir un extracto mal cargado salvo el DELETE físico sin guardas de
     * {@code ExtractoBancarioService.remove}.
     * <p>Guardas, en orden, TODAS antes de borrar nada:</p>
     * <ol>
     * <li>Debe existir un extracto activo para esa cuenta/período (si no, use
     * {@link #confirmar} en vez de esto).</li>
     * <li>Ninguna fila del extracto anterior puede estar en un grupo de conciliación
     * ACTIVO (TSR.GCEX -&gt; TSR.GRCC.GRCCESTD = Activo) - hay que deshacer esas
     * conciliaciones primero.</li>
     * <li>Ninguna fila del extracto anterior puede estar declarada como partida en tránsito
     * PENDIENTE (TSR.DTCN.DTCNIDEX, DTCNESTD = Pendiente) - quedaría apuntando a una fila
     * borrada.</li>
     * <li>El período no puede estar cerrado para conciliación bancaria (reusa
     * {@code ControlExtractoBancarioService.estaCerrado}, la misma guarda que
     * {@link #confirmar}).</li>
     * <li>La {@code ConciliacionContable} de esa cuenta/período, si existe, no puede estar
     * VERIFICADA - recargar por debajo invalidaría esa verificación en silencio.</li>
     * </ol>
     * <p>Tras pasar las guardas: borra DEXB y luego EXBC (por la FK), recalcula los
     * contadores de la ConciliacionContable si existe, y reimporta el archivo nuevo por el
     * mismo camino que {@link #confirmar} - el hash del extracto anterior desaparece con el
     * borrado, así que el mismo archivo (corregido o idéntico) puede volver a subirse.</p>
     * @param archivo          : Contenido del archivo nuevo
     * @param nombreArchivo    : Nombre original del archivo nuevo
     * @param idCuentaBancaria : Cuenta bancaria del extracto a reemplazar
     * @param idPeriodo        : Periodo contable del extracto a reemplazar
     * @param idEmpresa        : Empresa dueña de la cuenta
     * @param usuarioCreacion  : Usuario que recarga (auditoria - queda en las observaciones
     *                           del extracto nuevo, EXBC no tiene una columna dedicada para esto)
     * @return                 : Resumen de la recarga (extracto anterior borrado + extracto nuevo)
     * @throws Throwable       : Excepcion (cualquiera de las guardas de arriba)
     */
    ResumenImportacionExtracto recargar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo, Long idEmpresa, String usuarioCreacion) throws Throwable;

}
