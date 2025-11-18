package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.MotivoPrestamo;

import jakarta.ejb.Remote;

@Remote
public interface MotivoPrestamoService extends EntityService<MotivoPrestamo> {

}
