package com.saa.basico.ejbImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.saa.basico.ejb.FileService;

import jakarta.ejb.Stateless;


/**
 * @author GaemiSoft.
 *         <p>
 *         Implementación del servicio para manejo de archivos.
 *         </p>
 */
@Stateless
public class FileServiceImpl implements FileService {

    private static final String DEFAULT_UPLOAD_DIR = getUploadDirectory();

    /**
     * Obtiene el directorio de uploads basado en el entorno
     */
    private static String getUploadDirectory() {
        // Verificar si hay una variable de sistema configurada
        String uploadDir = System.getProperty("saa.upload.dir");
        if (uploadDir != null && !uploadDir.trim().isEmpty()) {
            return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
        }

        // Verificar variable de entorno
        uploadDir = System.getenv("SAA_UPLOAD_DIR");
        if (uploadDir != null && !uploadDir.trim().isEmpty()) {
            return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
        }

        // Directorio por defecto basado en el sistema operativo
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            return userHome + "/saa-uploads/";
        } else {
            return "/opt/saa-uploads/";
        }
    }

    @Override
    public String uploadFileToPath(InputStream inputStream, String fileName, String uploadPath) throws Throwable {
        try {
            // Validar extensión
            if (!validarExtension(fileName)) {
                throw new Throwable("Extensión de archivo no permitida: " + getExtension(fileName));
            }

            // Construir el path completo basado en la variable de entorno
            String baseUploadDir = getBaseUploadDirectory();
            
            // Combinar el directorio base con el path relativo proporcionado
            Path fullUploadPath;
            if (uploadPath != null && !uploadPath.trim().isEmpty()) {
                // Si uploadPath es absoluto, usarlo tal como está
                if (Paths.get(uploadPath).isAbsolute()) {
                    fullUploadPath = Paths.get(uploadPath);
                } else {
                    // Si es relativo, combinarlo con el directorio base
                    fullUploadPath = Paths.get(baseUploadDir, uploadPath);
                }
            } else {
                fullUploadPath = Paths.get(baseUploadDir);
            }

            if (!Files.exists(fullUploadPath)) {
                Files.createDirectories(fullUploadPath);
            }
            
            // Usar el nombre original del archivo (se sobreescribe si ya existe)
            Path filePath = fullUploadPath.resolve(fileName);
            
            // Copiar el InputStream directamente al archivo
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new Throwable("Error al subir el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String fileName) throws Throwable {
        return uploadFileToPath(inputStream, fileName, DEFAULT_UPLOAD_DIR);
    }

    @Override
    public InputStream downloadFile(String filePath) throws Throwable {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new Throwable("El archivo no existe: " + filePath);
            }
            return new FileInputStream(path.toFile());
        } catch (IOException e) {
            throw new Throwable("Error al descargar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String filePath) throws Throwable {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new Throwable("Error al eliminar el archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String filePath) throws Throwable {
        try {
            return Files.exists(Paths.get(filePath));
        } catch (Exception e) {
            throw new Throwable("Error al verificar existencia del archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public long getFileSize(String filePath) throws Throwable {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            throw new Throwable("Error al obtener el tamaño del archivo: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listFiles(String directoryPath) throws Throwable {
        List<String> fileNames = new ArrayList<>();
        try {
            Path dir = Paths.get(directoryPath);
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path entry : stream) {
                        if (Files.isRegularFile(entry)) {
                            fileNames.add(entry.getFileName().toString());
                        }
                    }
                }
            }
            return fileNames;
        } catch (IOException e) {
            throw new Throwable("Error al listar archivos: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validarExtension(String fileName) throws Throwable {
        String extension = getExtension(fileName).toLowerCase();
        for (String allowedExt : EXTENSIONES_PERMITIDAS) {
            if (allowedExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validarTamaño(long fileSize) throws Throwable {
        return fileSize <= TAMAÑO_MAXIMO;
    }

    @Override
    public String generarNombreUnico(String originalName) throws Throwable {
        String extension = getExtension(originalName);
        String nameWithoutExt = originalName.substring(0, originalName.lastIndexOf('.'));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return nameWithoutExt + "_" + uniqueId + extension;
    }

    /**
     * Obtiene el directorio base de uploads desde la variable de entorno
     */
    private String getBaseUploadDirectory() {
        // Verificar si hay una variable de sistema configurada
        String uploadDir = System.getProperty("saa.upload.dir");
        if (uploadDir != null && !uploadDir.trim().isEmpty()) {
            return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
        }

        // Verificar variable de entorno
        uploadDir = System.getenv("SAA_UPLOAD_DIR");
        if (uploadDir != null && !uploadDir.trim().isEmpty()) {
            return uploadDir.endsWith("/") || uploadDir.endsWith("\\") ? uploadDir : uploadDir + "/";
        }

        // Directorio por defecto basado en el sistema operativo
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("windows")) {
            return userHome + "\\saa-uploads\\";
        } else {
            return "/opt/saa-uploads/";
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot);
    }
}
