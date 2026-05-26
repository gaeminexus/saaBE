package com.saa.ejb.crd.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Exter;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface ExterDaoService extends EntityDao<Exter> {

    /**
     * Recupera el registro de Exter por cédula.
     * @param cedula : Número de identificación a buscar
     * @return : Listado (0 o 1 elemento)
     * @throws Throwable : Excepcion
     */
    List<Exter> selectByCedula(String cedula) throws Throwable;
}
