package com.saa.ejb.credito.service;

import com.saa.basico.util.EntityService;
import com.saa.model.credito.Aporte;

import jakarta.ejb.Remote;

@Remote
public interface AporteService extends EntityService<Aporte> {

}
