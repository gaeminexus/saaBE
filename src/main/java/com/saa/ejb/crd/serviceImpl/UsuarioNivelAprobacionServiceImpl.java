package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.UsuarioNivelAprobacionDaoService;
import com.saa.ejb.crd.service.UsuarioNivelAprobacionService;
import com.saa.model.crd.UsuarioNivelAprobacion;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;
import com.saa.ejb.crd.dao.NivelAprobacionPrestamoDaoService;
import com.saa.ejb.crd.dao.UsuarioNivelAprobacionDaoService;
import com.saa.model.crd.NivelAprobacionPrestamo;
import com.saa.model.scp.Usuario;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class UsuarioNivelAprobacionServiceImpl implements UsuarioNivelAprobacionService {

    @EJB
    private UsuarioNivelAprobacionDaoService dao;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private NivelAprobacionPrestamoDaoService nivelDao;

    @EJB
    private com.saa.basico.ejb.UsuarioDaoService usuarioDaoService;

    @Override
    public UsuarioNivelAprobacion selectById(Long id) throws Throwable {
        System.out.println("selectById - UsuarioNivelAprobacion: " + id);
        return dao.selectById(id, NombreEntidadesCredito.USUARIO_NIVEL_APROBACION);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // No se borra: la bitácora de aprobaciones referencia estas tablas. Se deja inactivo.
        throw new IncomeException("No se borra; se deja inactivo (DELETE con ?usuario=).");
    }

    @Override
    public void save(List<UsuarioNivelAprobacion> lista) throws Throwable {
        System.out.println("save list - UsuarioNivelAprobacion");
        for (UsuarioNivelAprobacion registro : lista) {
            guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
        }
    }

    @Override
    public List<UsuarioNivelAprobacion> selectAll() throws Throwable {
        System.out.println("selectAll - UsuarioNivelAprobacion");
        List<UsuarioNivelAprobacion> result = dao.selectAll(NombreEntidadesCredito.USUARIO_NIVEL_APROBACION);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros UsuarioNivelAprobacion");
        }
        return result;
    }

    @Override
    public UsuarioNivelAprobacion saveSingle(UsuarioNivelAprobacion registro) throws Throwable {
        System.out.println("saveSingle - UsuarioNivelAprobacion");
        return guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
    }

    @Override
    public List<UsuarioNivelAprobacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - UsuarioNivelAprobacion");
        List<UsuarioNivelAprobacion> result = dao.selectByCriteria(datos, NombreEntidadesCredito.USUARIO_NIVEL_APROBACION);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio UsuarioNivelAprobacion no devolvio registros");
        }
        return result;
    }

    @Override
    public UsuarioNivelAprobacion guardar(UsuarioNivelAprobacion registro, String usuario) throws Throwable {
        System.out.println("guardar - UsuarioNivelAprobacion - código: " + (registro != null ? registro.getCodigo() : null));
        if (registro == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: falta el registro");
        }
        usuario = usuarioValidacionService.validar(usuario);
        UsuarioNivelAprobacion existente = null;
        if (registro.getCodigo() != null) {
            existente = dao.find(new UsuarioNivelAprobacion(), registro.getCodigo());
            if (existente == null) {
                throw new IncomeException("No existe UsuarioNivelAprobacion " + registro.getCodigo());
            }
        }
        if (registro.getIdNivel() == null || nivelDao.find(new NivelAprobacionPrestamo(), registro.getIdNivel()) == null) {
            throw new IncomeException("El nivel " + registro.getIdNivel() + " no existe");
        }
        // UNAPPJRQ no tiene FK (SCP es otro esquema): se comprueba acá que el usuario exista.
        if (registro.getIdUsuario() == null || usuarioDaoService.find(new Usuario(), registro.getIdUsuario()) == null) {
            throw new IncomeException("El usuario " + registro.getIdUsuario() + " no existe en el sistema");
        }
        if (registro.getEstado() != null && registro.getEstado() != 0L && registro.getEstado() != 1L) {
            throw new IncomeException("El estado es 1 (activo) o 0 (inactivo)");
        }
        boolean quedaActivo = registro.getEstado() == null ? (existente == null || existente.getEstado() == 1L)
            : registro.getEstado() == 1L;
        if (quedaActivo) {
            for (UsuarioNivelAprobacion otro : dao.selectActivosByNivel(registro.getIdNivel())) {
                if (otro.getIdUsuario().equals(registro.getIdUsuario())
                        && (registro.getCodigo() == null || !otro.getCodigo().equals(registro.getCodigo()))) {
                    throw new IncomeException("El usuario " + registro.getIdUsuario() + " ya está en este nivel");
                }
            }
        }

        if (existente == null) {
            if (registro.getEstado() == null) {
                registro.setEstado(Long.valueOf(Estado.ACTIVO));
            }
            registro.setUsuarioRegistro(usuario);
            registro.setFechaRegistro(LocalDateTime.now());
            registro.setUsuarioModificacion(null);
            registro.setFechaModificacion(null);
        } else {
            if (registro.getEstado() == null) {
                registro.setEstado(existente.getEstado());
            }
            registro.setUsuarioRegistro(existente.getUsuarioRegistro());
            registro.setFechaRegistro(existente.getFechaRegistro());
            registro.setUsuarioModificacion(usuario);
            registro.setFechaModificacion(LocalDateTime.now());
        }
        return dao.save(registro, registro.getCodigo());
    }

    @Override
    public UsuarioNivelAprobacion inactivar(Long id, String usuario) throws Throwable {
        System.out.println("inactivar - UsuarioNivelAprobacion: " + id);
        usuario = usuarioValidacionService.validar(usuario);
        UsuarioNivelAprobacion registro = dao.find(new UsuarioNivelAprobacion(), id);
        if (registro == null) {
            throw new IncomeException("No existe UsuarioNivelAprobacion " + id);
        }
        registro.setEstado(Long.valueOf(Estado.INACTIVO));
        registro.setUsuarioModificacion(usuario);
        registro.setFechaModificacion(LocalDateTime.now());
        return dao.save(registro, registro.getCodigo());
    }
}
