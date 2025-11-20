package com.saa.ws.rest.files;

/**
 * Respuesta estándar para operaciones de archivos
 */
public class FileResponse {
    
    private boolean success;
    private String message;
    private String filePath;

    public FileResponse() {}

    public FileResponse(boolean success, String message, String filePath) {
        this.success = success;
        this.message = message;
        this.filePath = filePath;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}