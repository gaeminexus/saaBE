package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.DetallePrestamo;

import jakarta.ejb.Remote;

@Remote
public interface DetallePrestamoService extends EntityService<DetallePrestamo> {

}
