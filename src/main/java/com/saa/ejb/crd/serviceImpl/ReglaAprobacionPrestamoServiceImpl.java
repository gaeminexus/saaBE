package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ReglaAprobacionPrestamoDaoService;
import com.saa.ejb.crd.service.ReglaAprobacionPrestamoService;
import com.saa.model.crd.ReglaAprobacionPrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;
import com.saa.ejb.crd.dao.TipoPrestamoDaoService;
import com.saa.model.crd.TipoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class ReglaAprobacionPrestamoServiceImpl implements ReglaAprobacionPrestamoService {

    @EJB
    private ReglaAprobacionPrestamoDaoService dao;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @EJB
    private TipoPrestamoDaoService tipoPrestamoDaoService;

    @Override
    public ReglaAprobacionPrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - ReglaAprobacionPrestamo: " + id);
        return dao.selectById(id, NombreEntidadesCredito.REGLA_APROBACION_PRESTAMO);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // No se borra: la bitácora de aprobaciones referencia estas tablas. Se deja inactivo.
        throw new IncomeException("No se borra; se deja inactivo (DELETE con ?usuario=).");
    }

    @Override
    public void save(List<ReglaAprobacionPrestamo> lista) throws Throwable {
        System.out.println("save list - ReglaAprobacionPrestamo");
        for (ReglaAprobacionPrestamo registro : lista) {
            guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
        }
    }

    @Override
    public List<ReglaAprobacionPrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - ReglaAprobacionPrestamo");
        List<ReglaAprobacionPrestamo> result = dao.selectAll(NombreEntidadesCredito.REGLA_APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros ReglaAprobacionPrestamo");
        }
        return result;
    }

    @Override
    public ReglaAprobacionPrestamo saveSingle(ReglaAprobacionPrestamo registro) throws Throwable {
        System.out.println("saveSingle - ReglaAprobacionPrestamo");
        return guardar(registro, registro.getCodigo() == null ? registro.getUsuarioRegistro() : registro.getUsuarioModificacion());
    }

    @Override
    public List<ReglaAprobacionPrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - ReglaAprobacionPrestamo");
        List<ReglaAprobacionPrestamo> result = dao.selectByCriteria(datos, NombreEntidadesCredito.REGLA_APROBACION_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ReglaAprobacionPrestamo no devolvio registros");
        }
        return result;
    }

    @Override
    public ReglaAprobacionPrestamo guardar(ReglaAprobacionPrestamo registro, String usuario) throws Throwable {
        System.out.println("guardar - ReglaAprobacionPrestamo - código: " + (registro != null ? registro.getCodigo() : null));
        if (registro == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: falta el registro");
        }
        usuario = usuarioValidacionService.validar(usuario);
        ReglaAprobacionPrestamo existente = null;
        if (registro.getCodigo() != null) {
            existente = dao.find(new ReglaAprobacionPrestamo(), registro.getCodigo());
            if (existente == null) {
                throw new IncomeException("No existe ReglaAprobacionPrestamo " + registro.getCodigo());
            }
        }
        if (registro.getNombre() == null || registro.getNombre().trim().isEmpty()) {
            throw new IncomeException("El nombre de la regla es obligatorio");
        }
        registro.setNombre(registro.getNombre().trim());
        if (registro.getIdTipoPrestamo() != null
                && tipoPrestamoDaoService.find(new TipoPrestamo(), registro.getIdTipoPrestamo()) == null) {
            throw new IncomeException("No existe el tipo de préstamo " + registro.getIdTipoPrestamo());
        }
        if (registro.getMontoMinimo() != null && registro.getMontoMinimo() < 0
                || registro.getMontoMaximo() != null && registro.getMontoMaximo() < 0) {
            throw new IncomeException("Los montos de la regla no pueden ser negativos");
        }
        if (registro.getMontoMinimo() != null && registro.getMontoMaximo() != null
                && registro.getMontoMinimo() > registro.getMontoMaximo()) {
            throw new IncomeException("El monto desde no puede ser mayor que el monto hasta");
        }
        if (registro.getPlazoMinimo() != null && registro.getPlazoMaximo() != null
                && registro.getPlazoMinimo() > registro.getPlazoMaximo()) {
            throw new IncomeException("El plazo desde no puede ser mayor que el plazo hasta");
        }
        if (registro.getEstado() != null && registro.getEstado() != 0L && registro.getEstado() != 1L) {
            throw new IncomeException("El estado de la regla es 1 (activa) o 0 (inactiva)");
        }
        if (registro.getPrioridad() == null) {
            registro.setPrioridad(100L);
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
    public ReglaAprobacionPrestamo inactivar(Long id, String usuario) throws Throwable {
        System.out.println("inactivar - ReglaAprobacionPrestamo: " + id);
        usuario = usuarioValidacionService.validar(usuario);
        ReglaAprobacionPrestamo registro = dao.find(new ReglaAprobacionPrestamo(), id);
        if (registro == null) {
            throw new IncomeException("No existe ReglaAprobacionPrestamo " + id);
        }
        registro.setEstado(Long.valueOf(Estado.INACTIVO));
        registro.setUsuarioModificacion(usuario);
        registro.setFechaModificacion(LocalDateTime.now());
        return dao.save(registro, registro.getCodigo());
    }
}
