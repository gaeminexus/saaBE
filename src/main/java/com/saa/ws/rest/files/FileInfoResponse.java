package com.saa.ws.rest.files;

/**
 * Respuesta para información de archivo
 */
public class FileInfoResponse {
    
    private boolean success;
    private String message;
    private FileInfo fileInfo;

    public FileInfoResponse() {}

    public FileInfoResponse(boolean success, String message, FileInfo fileInfo) {
        this.success = success;
        this.message = message;
        this.fileInfo = fileInfo;
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

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }
}