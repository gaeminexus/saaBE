package com.saa.basico.ejb;

import java.io.InputStream;
import java.util.List;
import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         <p>
 *         Servicio para la administración de upload y download de archivos al servidor.
 *         </p>
 */
@Local
public interface FileService {

    /**
     * Extensiones de archivos permitidas
     */
    String[] EXTENSIONES_PERMITIDAS = { ".pdf", ".doc", ".docx", ".xls", ".xlsx", 
                                       ".jpg", ".jpeg", ".png", ".gif", ".txt" };

    /**
     * Tamaño máximo de archivo en bytes (10MB)
     */
    long TAMAÑO_MAXIMO = 10 * 1024 * 1024;

    /**
     * Sube un archivo al servidor con path personalizado
     *
     * @param inputStream : Stream del archivo a subir
     * @param fileName    : Nombre del archivo
     * @param uploadPath  : Ruta donde se guardará el archivo
     * @return            : Ruta completa del archivo guardado
     * @throws Throwable  : Excepción
     */
    String uploadFileToPath(InputStream inputStream, String fileName, String uploadPath) throws Throwable;

    /**
     * Sube un archivo con configuración por defecto
     *
     * @param inputStream : Stream del archivo a subir
     * @param fileName    : Nombre del archivo
     * @return            : Ruta completa del archivo guardado
     * @throws Throwable  : Excepción
     */
    String uploadFile(InputStream inputStream, String fileName) throws Throwable;

    /**
     * Descarga un archivo del servidor
     *
     * @param filePath   : Ruta del archivo a descargar
     * @return           : InputStream del archivo
     * @throws Throwable : Excepción
     */
    InputStream downloadFile(String filePath) throws Throwable;

    /**
     * Elimina un archivo del servidor
     *
     * @param filePath   : Ruta del archivo a eliminar
     * @return           : True si se eliminó correctamente
     * @throws Throwable : Excepción
     */
    boolean deleteFile(String filePath) throws Throwable;

    /**
     * Verifica si un archivo existe
     *
     * @param filePath   : Ruta del archivo
     * @return           : True si el archivo existe
     * @throws Throwable : Excepción
     */
    boolean fileExists(String filePath) throws Throwable;

    /**
     * Obtiene el tamaño de un archivo
     *
     * @param filePath   : Ruta del archivo
     * @return           : Tamaño del archivo en bytes
     * @throws Throwable : Excepción
     */
    long getFileSize(String filePath) throws Throwable;

    /**
     * Lista archivos en un directorio
     *
     * @param directoryPath : Ruta del directorio
     * @return              : Lista de nombres de archivos
     * @throws Throwable    : Excepción
     */
    List<String> listFiles(String directoryPath) throws Throwable;

    /**
     * Valida la extensión de un archivo
     *
     * @param fileName   : Nombre del archivo
     * @return           : True si la extensión es válida
     * @throws Throwable : Excepción
     */
    boolean validarExtension(String fileName) throws Throwable;

    /**
     * Valida el tamaño de un archivo
     *
     * @param fileSize   : Tamaño del archivo en bytes
     * @return           : True si el tamaño es válido
     * @throws Throwable : Excepción
     */
    boolean validarTamaño(long fileSize) throws Throwable;

    /**
     * Genera un nombre único para el archivo
     *
     * @param originalName : Nombre original del archivo
     * @return             : Nombre único generado
     * @throws Throwable   : Excepción
     */
    String generarNombreUnico(String originalName) throws Throwable;
}
