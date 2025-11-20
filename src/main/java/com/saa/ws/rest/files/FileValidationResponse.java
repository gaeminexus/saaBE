package com.saa.ws.rest.files;

import java.io.Serializable;

/**
 * Respuesta para validación de archivos
 */
public class FileValidationResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private boolean valid;
    private String message;
    private boolean extensionValid;
    private boolean sizeValid;

    public FileValidationResponse() {}

    public FileValidationResponse(boolean valid, String message, boolean extensionValid, boolean sizeValid) {
        this.valid = valid;
        this.message = message;
        this.extensionValid = extensionValid;
        this.sizeValid = sizeValid;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isExtensionValid() {
        return extensionValid;
    }

    public void setExtensionValid(boolean extensionValid) {
        this.extensionValid = extensionValid;
    }

    public boolean isSizeValid() {
        return sizeValid;
    }

    public void setSizeValid(boolean sizeValid) {
        this.sizeValid = sizeValid;
    }
}
