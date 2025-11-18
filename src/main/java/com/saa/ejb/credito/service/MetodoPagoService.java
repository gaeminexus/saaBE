package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.MetodoPago;

import jakarta.ejb.Remote;

@Remote
public interface MetodoPagoService extends EntityService<MetodoPago> {

}
