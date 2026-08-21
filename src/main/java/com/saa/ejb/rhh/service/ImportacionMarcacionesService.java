package com.saa.ejb.rhh.service;

import java.io.InputStream;

import com.saa.model.rhh.ResultadoImportacionMarcaciones;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Importacion del archivo de marcaciones del reloj biometrico.</p>
 *
 * <p>Copia el patron de <code>ImportacionExtractoBancarioServiceImpl</code>: dos pasos,
 * previsualizar y confirmar, con control antiduplicado por hash del archivo.</p>
 *
 * <h3>El formato es dato</h3>
 *
 * <p>El parser <b>no conoce ningun reloj</b>. Todo lo que necesita saber esta en
 * <code>RHH.FMRC</code> y sus <code>RHH.DFMR</code>: cuantas lineas saltar, si el archivo es
 * de ancho fijo o delimitado, en que posicion va cada campo, con que patron se leen la fecha y
 * la hora, y como se traduce el tipo de marcacion del reloj al rubro 192. Cuando llegue la
 * muestra real del cliente se crea otro <code>FMRC</code> y <b>no se toca codigo</b>.</p>
 *
 * <h3>Una linea mala no aborta el archivo</h3>
 *
 * <p>Es la regla 7 y tiene consecuencia de diseno: los errores de linea se acumulan en el log
 * de la carga en vez de lanzar. Un archivo de reloj trae casi siempre alguna linea con el dedo
 * de un visitante o un empleado dado de baja, y rechazar el mes entero por eso dejaria la
 * nomina sin asistencia.</p>
 */
@Local
public interface ImportacionMarcacionesService {

    /**
     * Lee el archivo y devuelve lo que pasaria, <b>sin persistir nada</b>.
     *
     * @param archivo		: Contenido del archivo
     * @param nombreArchivo	: Nombre del archivo, para el registro
     * @param idFormato		: Id del formato con el que se lee
     * @param idEmpresa		: Id de la empresa
     * @return				: Resumen de lo que se importaria
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    ResultadoImportacionMarcaciones previsualizar(InputStream archivo, String nombreArchivo,
            Long idFormato, Long idEmpresa) throws Throwable;

    /**
     * Lee el archivo y persiste la carga con sus marcaciones.
     *
     * <p>Todo en una transaccion: entra el archivo entero o no entra nada. Los errores de
     * linea no revierten, se acumulan en el log de la carga.</p>
     *
     * @param archivo		: Contenido del archivo
     * @param nombreArchivo	: Nombre del archivo
     * @param idFormato		: Id del formato con el que se lee
     * @param idEmpresa		: Id de la empresa
     * @param usuario		: Usuario que ejecuta
     * @return				: Resumen de lo importado, con el id de la carga
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    ResultadoImportacionMarcaciones confirmar(InputStream archivo, String nombreArchivo,
            Long idFormato, Long idEmpresa, String usuario) throws Throwable;

    /**
     * Anula una carga y retira sus marcaciones.
     *
     * <p>Exige motivo. <b>Rechaza la anulacion si alguna marcacion del lote ya se consolido</b>
     * en un resumen diario: retirarla dejaria el resumen apoyado en datos que ya no existen.
     * En ese caso hay que rehacer la consolidacion primero.</p>
     *
     * @param idCarga		: Id de la carga
     * @param motivo		: Motivo de la anulacion
     * @param usuario		: Usuario que ejecuta
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    void anular(Long idCarga, String motivo, String usuario) throws Throwable;

}
