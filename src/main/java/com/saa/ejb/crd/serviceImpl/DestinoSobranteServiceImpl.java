package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.dao.DestinoSobranteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.TipoAporteDaoService;
import com.saa.ejb.crd.service.DestinoSobranteService;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DestinoSobrante;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.TipoAporte;
import com.saa.rubros.CrdEstadoCargaArchivo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DestinoSobranteServiceImpl implements DestinoSobranteService {

    /** CRD.TPAP: 9 aporte de jubilación, 11 aporte de cesantía. */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;

    private static final long ACTIVO = 1L;
    private static final long ANULADO = 0L;

    @EJB
    private DestinoSobranteDaoService destinoSobranteDaoService;

    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private TipoAporteDaoService tipoAporteDaoService;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    @Override
    public DestinoSobrante selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById de DestinoSobrante con id: " + id);
        return destinoSobranteDaoService.selectById(id, NombreEntidadesCredito.DESTINO_SOBRANTE);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de DestinoSobranteService");
        throw new IncomeException("El destino del sobrante no se elimina: se anula (POST /dssb/anular/{id}),"
            + " para conservar la huella de lo que se eligió.");
    }

    @Override
    public void save(List<DestinoSobrante> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de DestinoSobranteService");
        throw new IncomeException("El destino del sobrante se registra de a uno, con usuario (POST /dssb?usuario=).");
    }

    @Override
    public List<DestinoSobrante> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll DestinoSobranteService");
        return destinoSobranteDaoService.selectAll(NombreEntidadesCredito.DESTINO_SOBRANTE);
    }

    /**
     * Sin usuario explícito: toma el del propio registro (usuarioRegistro si es nuevo, usuarioModificacion si ya
     * existe). Igual se valida primero; el REST usa {@link #registrar(DestinoSobrante, String)} con el query param.
     */
    @Override
    public DestinoSobrante saveSingle(DestinoSobrante destino) throws Throwable {
        System.out.println("saveSingle - DestinoSobrante");
        String usuario = destino.getCodigo() == null ? destino.getUsuarioRegistro() : destino.getUsuarioModificacion();
        return registrar(destino, usuario);
    }

    @Override
    public List<DestinoSobrante> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria DestinoSobranteService");
        return destinoSobranteDaoService.selectByCriteria(datos, NombreEntidadesCredito.DESTINO_SOBRANTE);
    }

    @Override
    public List<DestinoSobrante> selectByCarga(Long idCarga) throws Throwable {
        System.out.println("Ingresa al metodo selectByCarga DestinoSobranteService - carga: " + idCarga);
        return destinoSobranteDaoService.selectByCarga(idCarga);
    }

    @Override
    public DestinoSobrante registrar(DestinoSobrante destino, String usuario) throws Throwable {
        System.out.println("registrar - DestinoSobrante - usuario: " + usuario);
        // LO PRIMERO: sin usuario válido no se hace nada.
        String usuarioValido = usuarioValidacionService.validar(usuario);

        if (destino == null || destino.getCargaArchivo() == null || destino.getCargaArchivo().getCodigo() == null) {
            throw new IncomeException("Falta la carga a la que pertenece el destino del sobrante.");
        }
        if (destino.getEntidad() == null || destino.getEntidad().getCodigo() == null) {
            throw new IncomeException("Falta el partícipe al que pertenece el destino del sobrante.");
        }
        if (destino.getTipoDestino() == null) {
            throw new IncomeException("Falta el destino del sobrante: 1 siguiente cuota, 2 aporte, 3 devolución.");
        }
        Long idCarga = destino.getCargaArchivo().getCodigo();
        Long idEntidad = destino.getEntidad().getCodigo();
        int tipo = destino.getTipoDestino().intValue();

        exigeCargaSinProcesar(idCarga);

        Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        if (entidad == null) {
            throw new IncomeException("No existe el partícipe " + idEntidad + ".");
        }

        Prestamo prestamo = null;
        TipoAporte tipoAporte = null;
        switch (tipo) {
        case com.saa.rubros.DestinoSobrante.SIGUIENTE_CUOTA:
            if (destino.getTipoAporte() != null) {
                throw new IncomeException("El destino «siguiente cuota» no lleva tipo de aporte.");
            }
            if (destino.getPrestamo() != null && destino.getPrestamo().getCodigo() != null) {
                prestamo = prestamoDaoService.selectById(destino.getPrestamo().getCodigo(),
                    NombreEntidadesCredito.PRESTAMO);
                if (prestamo == null || prestamo.getEntidad() == null
                        || !idEntidad.equals(prestamo.getEntidad().getCodigo())) {
                    throw new IncomeException("El préstamo " + destino.getPrestamo().getCodigo()
                        + " no existe o no es del partícipe " + idEntidad + ".");
                }
            }
            break;
        case com.saa.rubros.DestinoSobrante.APORTE:
            if (destino.getPrestamo() != null && destino.getPrestamo().getCodigo() != null) {
                throw new IncomeException("El destino «aporte» no lleva préstamo.");
            }
            if (destino.getTipoAporte() == null || destino.getTipoAporte().getCodigo() == null) {
                throw new IncomeException("El destino «aporte» exige el tipo de aporte (9 jubilación, 11 cesantía).");
            }
            long idTipo = destino.getTipoAporte().getCodigo().longValue();
            if (idTipo != TIPO_APORTE_JUBILACION && idTipo != TIPO_APORTE_CESANTIA) {
                throw new IncomeException("Solo se puede enviar el sobrante a aporte de jubilación (9) o cesantía (11);"
                    + " llegó el tipo " + idTipo + ".");
            }
            tipoAporte = tipoAporteDaoService.selectById(idTipo, NombreEntidadesCredito.TIPO_APORTE);
            if (tipoAporte == null) {
                throw new IncomeException("No existe el tipo de aporte " + idTipo + ".");
            }
            break;
        case com.saa.rubros.DestinoSobrante.DEVOLUCION:
            if ((destino.getPrestamo() != null && destino.getPrestamo().getCodigo() != null)
                    || destino.getTipoAporte() != null) {
                throw new IncomeException("El destino «devolución» no lleva préstamo ni tipo de aporte.");
            }
            break;
        default:
            throw new IncomeException("Destino del sobrante desconocido: " + tipo + " (1, 2 o 3).");
        }

        CargaArchivo carga = cargaArchivoDaoService.selectById(idCarga, NombreEntidadesCredito.CARGA_ARCHIVO);

        // Un solo activo por (carga, partícipe): el anterior, si lo hay, se anula y queda este.
        DestinoSobrante anterior = destinoSobranteDaoService.selectActivoByCargaYEntidad(idCarga, idEntidad);
        if (anterior != null) {
            anterior.setEstado(ANULADO);
            anterior.setUsuarioModificacion(usuarioValido);
            anterior.setFechaModificacion(LocalDateTime.now());
            anterior.setObservacion(textoObservacion(anterior.getObservacion(), "Reemplazado por un nuevo destino"));
            destinoSobranteDaoService.save(anterior, anterior.getCodigo());
            // El índice único parcial exige que el anterior ya esté en base antes de insertar el nuevo.
            destinoSobranteDaoService.flush();
        }

        DestinoSobrante nuevo = new DestinoSobrante();
        nuevo.setCargaArchivo(carga);
        nuevo.setEntidad(entidad);
        nuevo.setTipoDestino(Long.valueOf(tipo));
        nuevo.setPrestamo(prestamo);
        nuevo.setTipoAporte(tipoAporte);
        nuevo.setEstado(ACTIVO);
        nuevo.setObservacion(destino.getObservacion());
        nuevo.setUsuarioRegistro(usuarioValido);
        nuevo.setFechaRegistro(LocalDateTime.now());
        return destinoSobranteDaoService.save(nuevo, null);
    }

    @Override
    public DestinoSobrante anular(Long idDestino, String usuario) throws Throwable {
        System.out.println("anular - DestinoSobrante " + idDestino + " - usuario: " + usuario);
        String usuarioValido = usuarioValidacionService.validar(usuario);

        DestinoSobrante destino = destinoSobranteDaoService.selectById(idDestino, NombreEntidadesCredito.DESTINO_SOBRANTE);
        if (destino == null) {
            throw new IncomeException("No existe el destino del sobrante " + idDestino + ".");
        }
        if (destino.getEstado() == null || destino.getEstado().longValue() != ACTIVO) {
            throw new IncomeException("El destino del sobrante " + idDestino + " ya está anulado.");
        }
        if (destino.getValorAplicadoAportes() != null || destino.getValorAplicadoPrestamos() != null) {
            throw new IncomeException("El destino del sobrante " + idDestino + " ya se aplicó en la carga; no se puede"
                + " anular desde aquí (se anula con el reproceso de la carga).");
        }
        exigeCargaSinProcesar(destino.getCargaArchivo().getCodigo());

        destino.setEstado(ANULADO);
        destino.setUsuarioModificacion(usuarioValido);
        destino.setFechaModificacion(LocalDateTime.now());
        return destinoSobranteDaoService.save(destino, destino.getCodigo());
    }

    private void exigeCargaSinProcesar(Long idCarga) throws Throwable {
        CargaArchivo carga = cargaArchivoDaoService.selectById(idCarga, NombreEntidadesCredito.CARGA_ARCHIVO);
        if (carga == null) {
            throw new IncomeException("No existe la carga " + idCarga + ".");
        }
        if (carga.getEstado() != null && carga.getEstado().longValue() == CrdEstadoCargaArchivo.PROCESADO) {
            throw new IncomeException("La carga " + idCarga + " ya fue procesada: el destino del sobrante se elige"
                + " antes de procesar (o con el reproceso).");
        }
    }

    private String textoObservacion(String actual, String agregado) {
        String base = actual == null ? "" : actual.trim() + " | ";
        String texto = base + agregado;
        return texto.length() > 1000 ? texto.substring(0, 1000) : texto;
    }
}
