package com.saa.ejb.rpr.serviceImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rpr.dao.HistoricoG44DaoService;
import com.saa.ejb.rpr.service.HistoricoG44Service;
import com.saa.model.rpr.HistoricoG44;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
@Stateless
public class HistoricoG44ServiceImpl extends EntityDaoImpl<HistoricoG44> implements HistoricoG44Service {
    @EJB private HistoricoG44DaoService dao;
    @Override
    public String[] obtieneCampos() { return dao.obtieneCampos(); }
    @Override
    public List<HistoricoG44> selectByIdentificacion(String identificacion) {
        return dao.selectByIdentificacion(identificacion);
    }

    @Override
    public List<HistoricoG44> selectExJubilados() throws Throwable {
        return dao.selectExJubilados();
    }
}
