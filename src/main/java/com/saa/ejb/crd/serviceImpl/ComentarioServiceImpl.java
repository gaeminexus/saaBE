package com.saa.ejb.crd.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ComentarioDaoService;
import com.saa.ejb.crd.service.ComentarioService;
import com.saa.model.crd.Comentario;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ComentarioServiceImpl implements ComentarioService {

    @EJB
    private ComentarioDaoService comentarioDaoService;

    @Override
    public Comentario selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById Comentario con id: " + id);
        return comentarioDaoService.selectById(id, NombreEntidadesCredito.COMENTARIO);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ComentarioService");
        Comentario comentario = new Comentario();
        for (Long registro : ids) {
            comentarioDaoService.remove(comentario, registro);
        }
    }

    @Override
    public void save(List<Comentario> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ComentarioService");
        for (Comentario registro : lista) {
            comentarioDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<Comentario> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ComentarioService");
        List<Comentario> result = comentarioDaoService.selectAll(NombreEntidadesCredito.COMENTARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Comentario no devolvió ningún registro");
        }
        return result;
    }

    @Override
    public Comentario saveSingle(Comentario comentario) throws Throwable {
        System.out.println("saveSingle - Comentario");
        if (comentario.getCodigo() == null) {
            comentario.setEstado(Long.valueOf(Estado.ACTIVO));
        }
        comentario = comentarioDaoService.save(comentario, comentario.getCodigo());
        return comentario;
    }

    @Override
    public List<Comentario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ComentarioService");
        List<Comentario> result = comentarioDaoService.selectByCriteria(datos, NombreEntidadesCredito.COMENTARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Comentario no devolvió registros");
        }
        return result;
    }
}
