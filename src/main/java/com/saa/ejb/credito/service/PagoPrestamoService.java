package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.PagoPrestamo;

import jakarta.ejb.Remote;

@Remote
public interface PagoPrestamoService extends EntityService<PagoPrestamo>{

}
