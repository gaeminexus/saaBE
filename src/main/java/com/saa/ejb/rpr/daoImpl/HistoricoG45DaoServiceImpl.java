package com.saa.ejb.rpr.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG45DaoService;
import com.saa.model.rpr.HistoricoG45;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class HistoricoG45DaoServiceImpl extends EntityDaoImpl<HistoricoG45> implements HistoricoG45DaoService {
    @PersistenceContext EntityManager em;
    @Override
    public String[] obtieneCampos() {
        return new String[]{ "identificacion","tipoIdentificacion","tipoParticipe",
            "actividadEconomica","patrimonio","provincia","canton","parroquia","genero",
            "estadoCivil","fechaNacimiento","profesion","cargasFamiliares","origenIngresos" };
    }
}