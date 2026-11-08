package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.GarantePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.GarantePrestamoService;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.GarantePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPrestamo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GarantePrestamoServiceImpl implements GarantePrestamoService {

    @EJB
    private GarantePrestamoDaoService garantePrestamoDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @Override
    public GarantePrestamo selectById(Long id) throws Throwable {
        System.out.println("selectById - GarantePrestamo: " + id);
        return garantePrestamoDaoService.selectById(id, NombreEntidadesCredito.GARANTE_PRESTAMO);
    }

    @Override
    public GarantePrestamo selectActivoByPrestamo(Long idPrestamo) throws Throwable {
        System.out.println("selectActivoByPrestamo - GarantePrestamo - préstamo: " + idPrestamo);
        return garantePrestamoDaoService.selectActivoByPrestamo(idPrestamo);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // El garante no se borra: se anula (GRPRESTD = 0). Ver anularGarante.
        throw new IncomeException("El garante no se borra; se anula (DELETE /grpr/{id}?usuario=...).");
    }

    @Override
    public void save(List<GarantePrestamo> lista) throws Throwable {
        System.out.println("save list - GarantePrestamo");
        for (GarantePrestamo registro : lista) {
            guardarGarante(registro, registro.getUsuarioRegistro());
        }
    }

    @Override
    public List<GarantePrestamo> selectAll() throws Throwable {
        System.out.println("selectAll - GarantePrestamo");
        List<GarantePrestamo> result = garantePrestamoDaoService.selectAll(NombreEntidadesCredito.GARANTE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros GarantePrestamo");
        }
        return result;
    }

    @Override
    public GarantePrestamo saveSingle(GarantePrestamo garante) throws Throwable {
        System.out.println("saveSingle - GarantePrestamo");
        return guardarGarante(garante, garante != null ? garante.getUsuarioRegistro() : null);
    }

    @Override
    public List<GarantePrestamo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - GarantePrestamo");
        List<GarantePrestamo> result = garantePrestamoDaoService.selectByCriteria(datos,
            NombreEntidadesCredito.GARANTE_PRESTAMO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio GarantePrestamo no devolvio registros");
        }
        return result;
    }

    @Override
    public GarantePrestamo guardarGarante(GarantePrestamo garante, String usuario) throws Throwable {
        System.out.println("guardarGarante - GarantePrestamo - préstamo: "
            + (garante != null ? garante.getIdPrestamo() : null) + " - código: "
            + (garante != null ? garante.getCodigo() : null));
        if (garante == null || garante.getIdPrestamo() == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el préstamo del garante es obligatorio");
        }
        usuario = usuarioValidacionService.validar(usuario);

        Prestamo prestamo = cargarPrestamoEditable(garante.getIdPrestamo());

        // Si el garante es partícipe, nombre y cédula salen de CRD.ENTD, no de la pantalla.
        if (garante.getIdEntidad() != null) {
            Entidad entidad = entidadDaoService.find(new Entidad(), garante.getIdEntidad());
            if (entidad == null) {
                throw new IncomeException("No existe el partícipe " + garante.getIdEntidad() + " indicado como garante");
            }
            garante.setNombre(entidad.getRazonSocial());
            garante.setCedula(entidad.getNumeroIdentificacion());
        }
        String nombre = garante.getNombre() != null ? garante.getNombre().trim() : "";
        String cedula = garante.getCedula() != null ? garante.getCedula().trim() : "";
        if (nombre.isEmpty() || cedula.isEmpty()) {
            throw new IncomeException("El nombre y la cédula del garante son obligatorios");
        }
        garante.setNombre(nombre);
        garante.setCedula(cedula);

        // La cédula del garante no puede ser la del deudor.
        String cedulaDeudor = prestamo.getEntidad() != null && prestamo.getEntidad().getNumeroIdentificacion() != null
            ? prestamo.getEntidad().getNumeroIdentificacion().trim() : "";
        if (cedula.equals(cedulaDeudor)
                || (garante.getIdEntidad() != null && prestamo.getEntidad() != null
                    && garante.getIdEntidad().equals(prestamo.getEntidad().getCodigo()))) {
            throw new IncomeException("El garante no puede ser el mismo deudor del préstamo");
        }

        GarantePrestamo activo = garantePrestamoDaoService.selectActivoByPrestamo(garante.getIdPrestamo());
        if (garante.getCodigo() == null) {
            if (activo != null) {
                throw new IncomeException("El préstamo " + garante.getIdPrestamo() + " ya tiene un garante activo ("
                    + activo.getNombre() + "); edítelo o anúlelo antes de agregar otro");
            }
            garante.setEstado(Long.valueOf(Estado.ACTIVO));
            garante.setUsuarioRegistro(usuario);
            garante.setFechaRegistro(LocalDateTime.now());
            garante.setUsuarioModificacion(null);
            garante.setFechaModificacion(null);
        } else {
            GarantePrestamo existente = garantePrestamoDaoService.find(new GarantePrestamo(), garante.getCodigo());
            if (existente == null) {
                throw new IncomeException("No existe el garante " + garante.getCodigo());
            }
            if (!existente.getIdPrestamo().equals(garante.getIdPrestamo())) {
                throw new IncomeException("El garante " + garante.getCodigo() + " no es del préstamo "
                    + garante.getIdPrestamo());
            }
            if (existente.getEstado() == null || existente.getEstado().longValue() != Estado.ACTIVO) {
                throw new IncomeException("El garante " + garante.getCodigo() + " está anulado; agregue uno nuevo");
            }
            // Auditoría de registro intacta; estado no lo decide el cliente.
            garante.setEstado(existente.getEstado());
            garante.setUsuarioRegistro(existente.getUsuarioRegistro());
            garante.setFechaRegistro(existente.getFechaRegistro());
            garante.setUsuarioModificacion(usuario);
            garante.setFechaModificacion(LocalDateTime.now());
        }
        return garantePrestamoDaoService.save(garante, garante.getCodigo());
    }

    @Override
    public GarantePrestamo anularGarante(Long idGarante, String usuario) throws Throwable {
        System.out.println("anularGarante - GarantePrestamo: " + idGarante);
        if (idGarante == null) {
            throw new IncomeException("PARAMETRO_INVALIDO: el código del garante es obligatorio");
        }
        usuario = usuarioValidacionService.validar(usuario);
        GarantePrestamo garante = garantePrestamoDaoService.find(new GarantePrestamo(), idGarante);
        if (garante == null) {
            throw new IncomeException("No existe el garante " + idGarante);
        }
        cargarPrestamoEditable(garante.getIdPrestamo());
        if (garante.getEstado() != null && garante.getEstado().longValue() == Estado.INACTIVO) {
            return garante; // ya estaba anulado: idempotente
        }
        garante.setEstado(Long.valueOf(Estado.INACTIVO));
        garante.setUsuarioModificacion(usuario);
        garante.setFechaModificacion(LocalDateTime.now());
        return garantePrestamoDaoService.save(garante, garante.getCodigo());
    }

    /** El préstamo del garante, que tiene que estar pendiente de aprobación (6) o generado (1). */
    private Prestamo cargarPrestamoEditable(Long idPrestamo) throws Throwable {
        Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
        if (prestamo == null) {
            throw new IncomeException("PRESTAMO_NO_ENCONTRADO: no existe el préstamo " + idPrestamo);
        }
        Long estado = prestamo.getIdEstado();
        if (estado == null || (estado != EstadoPrestamo.PENDIENTE_DE_APROBACION && estado != EstadoPrestamo.GENERADO)) {
            throw new IncomeException("El garante solo se agrega, cambia o anula mientras el préstamo está pendiente de"
                + " aprobación (6) o generado (1); el préstamo " + idPrestamo + " está en estado " + estado);
        }
        return prestamo;
    }
}
