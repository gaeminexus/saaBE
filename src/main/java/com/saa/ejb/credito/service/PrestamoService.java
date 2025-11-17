package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.Prestamo;

import jakarta.ejb.Remote;


@Remote
public interface PrestamoService extends EntityService<Prestamo>{

}
