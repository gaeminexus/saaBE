package com.saa.ws.rest.files;

import java.util.List;

/**
 * Respuesta para listar archivos
 */
public class FileListResponse {
    
    private boolean success;
    private String message;
    private List<String> files;

    public FileListResponse() {}

    public FileListResponse(boolean success, String message, List<String> files) {
        this.success = success;
        this.message = message;
        this.files = files;
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

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}