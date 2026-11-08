package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.NivelAprobacionPrestamoDaoService;
import com.saa.ejb.crd.service.NivelAprobacionPrestamoService;
import com.saa.model.crd.NivelAprobacionPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;
import com.saa.ejb.crd.dao.ReglaAprobacionPrestamoDaoService;
import com.saa.model.crd.ReglaAprobacionPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class NivelAprobacionPrestamoServiceImpl implements NivelAprobacionPrestamoService {

    @EJB
    private NivelAprobacionPrestamoDaoService dao;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private ReglaAprobacionPrestamoDaoService reglaDao;

    @Override
    public NivelAprobacionPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - NivelAprobacionPrestamo: " + id);
        return dao.selectById(id, NombreEntidadesCredito.NIVEL_APROBACION_PRESTAMO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // No se borra: la bitácora de aprobaciones referencia estas tablas. Se deja inactivo.
        throw new IncomeException("No se borra; se deja inactivo (DELETE con ?usuario=).");
    }

    @Override
    public void save(List<NivelAprobacionPrestamo> lista) throws Throwable {
        System.out.println("save list - NivelAprobacionPrestamo");
        for (NivelAprobacionPrestamo registro : lista) {
            guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
        }
    }

    @Override
    public List<NivelAprobacionPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - NivelAprobacionPrestamo");
        List<NivelAprobacionPrestamo> result = dao.selectAll(NombreEntidadesCredito.NIVEL_APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros NivelAprobacionPrestamo");
        }
        return result;
    }

    @Override
    public NivelAprobacionPrestamo saveSingle(NivelAprobacionPrestamo registro) throws Throwable {
        System.out.println("saveSingle - NivelAprobacionPrestamo");
        return guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
    }

    @Override
    public List<NivelAprobacionPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - NivelAprobacionPrestamo");
        List<NivelAprobacionPrestamo> result = dao.selectByCriteria(datos, NombreEntidadesCredito.NIVEL_APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio NivelAprobacionPrestamo no devolvio registros");
        }
        return result;
    }

    @Override
    public NivelAprobacionPrestamo guardar(NivelAprobacionPrestamo registro, String usuario) throws Throwable {
        System.out.println("guardar - NivelAprobacionPrestamo - código: " + (registro != null ? registro.getCodigo() : null));
        if (registro == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: falta el registro");
        }
        usuario = usuarioValidacionService.validar(usuario);
        NivelAprobacionPrestamo existente = null;
        if (registro.getCodigo() != null) {
            existente = dao.find(new NivelAprobacionPrestamo(), registro.getCodigo());
            if (existente == null) {
                throw new IncomeException("No existe NivelAprobacionPrestamo " + registro.getCodigo());
            }
        }
        if (registro.getIdRegla() == null
                || reglaDao.find(new ReglaAprobacionPrestamo(), registro.getIdRegla()) == null) {
            throw new IncomeException("La regla " + registro.getIdRegla() + " del nivel no existe");
        }
        if (registro.getOrden() == null || registro.getOrden() < 1) {
            throw new IncomeException("El orden del nivel es obligatorio y empieza en 1");
        }
        if (registro.getNombre() == null || registro.getNombre().trim().isEmpty()) {
            throw new IncomeException("El nombre del nivel es obligatorio");
        }
        registro.setNombre(registro.getNombre().trim());
        if (registro.getEstado() != null && registro.getEstado() != 0L && registro.getEstado() != 1L) {
            throw new IncomeException("El estado del nivel es 1 (activo) o 0 (inactivo)");
        }
        boolean quedaActivo = registro.getEstado() == null ? (existente == null || existente.getEstado() == 1L)
            : registro.getEstado() == 1L;
        if (quedaActivo) {
            // Un solo nivel ACTIVO por (regla, orden): el índice único lo exige; acá se avisa con un 422 claro.
            for (NivelAprobacionPrestamo otro : dao.selectActivosByRegla(registro.getIdRegla())) {
                if (otro.getOrden().equals(registro.getOrden())
                        && (registro.getCodigo() == null || !otro.getCodigo().equals(registro.getCodigo()))) {
                    throw new IncomeException("La regla " + registro.getIdRegla() + " ya tiene un nivel activo de orden "
                        + registro.getOrden() + " (" + otro.getNombre() + ")");
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
    public NivelAprobacionPrestamo inactivar(Long id, String usuario) throws Throwable {
        System.out.println("inactivar - NivelAprobacionPrestamo: " + id);
        usuario = usuarioValidacionService.validar(usuario);
        NivelAprobacionPrestamo registro = dao.find(new NivelAprobacionPrestamo(), id);
        if (registro == null) {
            throw new IncomeException("No existe NivelAprobacionPrestamo " + id);
        }
        registro.setEstado(Long.valueOf(Estado.INACTIVO));
        registro.setUsuarioModificacion(usuario);
        registro.setFechaModificacion(LocalDateTime.now());
        return dao.save(registro, registro.getCodigo());
    }
}
