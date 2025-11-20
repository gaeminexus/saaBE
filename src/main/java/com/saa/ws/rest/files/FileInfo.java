package com.saa.ws.rest.files;

import java.io.Serializable;

/**
 * Información de un archivo
 */
public class FileInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String fileName;
    private String filePath;
    private long fileSize;

    public FileInfo() {}

    public FileInfo(String fileName, String filePath, long fileSize) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
