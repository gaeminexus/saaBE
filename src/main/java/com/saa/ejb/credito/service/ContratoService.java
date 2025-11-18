package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.Contrato;

import jakarta.ejb.Remote;

@Remote
public interface ContratoService extends EntityService<Contrato> {

}
