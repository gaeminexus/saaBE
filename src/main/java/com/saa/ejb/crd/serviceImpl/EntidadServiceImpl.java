package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.EstadoParticipeEntidad;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class EntidadServiceImpl implements EntidadService {

    @EJB
    private EntidadDaoService entidadDaoService;

    /** Mínimo de aportes (meses) para ser elegible como miembro. */
    private static final Long MINIMO_APORTES_ELEGIBLE = 90L;

    /** Formato con el que sellarActualizacion escribe ENTDFCMD de aquí en adelante. */
    private static final DateTimeFormatter FORMATO_FECHA_MODIFICACION =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Recupera un registro de Entidad por su ID.
     */
    @Override
    public Entidad selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById con id: " + id);
        return entidadDaoService.selectById(id, NombreEntidadesCredito.ENTIDAD);
    }

    /**
     * Elimina uno o varios registros de Entidad.
     */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EntidadService ... depurado");
        Entidad entidad = new Entidad();
        for (Long registro : id) {
            entidadDaoService.remove(entidad, registro);
        }
    }

    /**
     * Guarda una lista de registros de Entidad.
     */
    @Override
    public void save(List<Entidad> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EntidadService");
        for (Entidad registro : lista) {
            entidadDaoService.save(registro, registro.getCodigo());
        }
    }

    /**
     * Recupera todos los registros de Entidad.
     */
    @Override
    public List<Entidad> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EntidadService");
        List<Entidad> result = entidadDaoService.selectAll(NombreEntidadesCredito.ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total Entidad no devolvio ningun registro");
        }
        return result;
    }

    /**
     * Guarda un solo registro de Entidad.
     */
    @Override
    public Entidad saveSingle(Entidad entidad) throws Throwable {
        System.out.println("saveSingle - Entidad");
        if(entidad.getCodigo() == null){
        	// La entidad nace pendiente de ser reportada en el G41.
        	// Antes se usaba el rubro genérico Estado.ACTIVO (1), que no
        	// corresponde a ninguna fila de CRD.ESPR.
        	entidad.setIdEstado(Long.valueOf(EstadoParticipeEntidad.NUEVO));
		}
        entidad = entidadDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    /**
     * Recupera registros de Entidad segun criterios de búsqueda.
     */
    @Override
    public List<Entidad> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria EntidadService");
        List<Entidad> result = entidadDaoService.selectByCriteria(datos, NombreEntidadesCredito.ENTIDAD);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Entidad no devolvio ningun registro");
        }
        return result;
    }

	@Override
	public List<Entidad> selectCoincidenciasByNombre(String nombre) throws Throwable {
		System.out.println("selectCoincidenciasByNombre");
		List<Entidad> entidades = new ArrayList<>();
        List<BigDecimal> result = entidadDaoService.selectCoincidenciasByNombre(nombre);
        if (result.isEmpty()) {
            throw new IncomeException("No existen coincidencias para el nombre proporcionado");
        } else {
        	for (BigDecimal codigo : result) {
				Entidad entidad = entidadDaoService.selectById(codigo.longValue(), NombreEntidadesCredito.ENTIDAD);
				entidades.add(entidad);
			}
        }
        return entidades;
	}

	@Override
	public List<Entidad> selectByIdEstado(Long idEstado) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdEstado EntidadService con idEstado: " + idEstado);
		return entidadDaoService.selectByIdEstado(idEstado);
	}

	@Override
	public Entidad findById(Long codigo) throws Throwable {
		System.out.println("Ingresa al metodo findById EntidadService con codigo: " + codigo);
		return entidadDaoService.findById(codigo);
	}

	@Override
	public Entidad selectByNumeroIdentificacion(String numeroIdentificacion) throws Throwable {
		System.out.println("selectByNumeroIdentificacion EntidadService: " + numeroIdentificacion);
		return entidadDaoService.selectByNumeroIdentificacion(numeroIdentificacion);
	}

	@Override
	public List<Entidad> findByCodigosIn(List<Long> codigos) throws Throwable {
		System.out.println("findByCodigosIn EntidadService - cantidad: " + (codigos != null ? codigos.size() : 0));
		return entidadDaoService.findByCodigosIn(codigos);
	}

	/**
	 * Genera el padrón de partícipes. Un padrón vacío es un resultado válido, por
	 * eso este método no lanza IncomeException cuando no hay filas.
	 */
	@Override
	public java.util.List<com.saa.model.crd.dto.PadronParticipeDTO> selectPadronParticipes(
			java.time.LocalDateTime fechaEjecucion,
			Long calidadId,
			Long minimoAportes) throws Throwable {
		System.out.println("Ingresa al metodo selectPadronParticipes EntidadService con fechaEjecucion: "
				+ fechaEjecucion + ", calidadId: " + calidadId + ", minimoAportes: " + minimoAportes);

		java.time.LocalDateTime fechaCorte = (fechaEjecucion != null)
				? fechaEjecucion
				: java.time.LocalDateTime.now();

		Long minimo = (minimoAportes != null && minimoAportes > 0)
				? minimoAportes
				: MINIMO_APORTES_ELEGIBLE;

		return entidadDaoService.selectPadronParticipes(fechaCorte, calidadId, minimo);
	}

	@Override
	public Entidad saveSingle(Entidad entidad, String usuario) throws Throwable {
		System.out.println("saveSingle(Entidad, usuario) - usuario: " + usuario);
		entidad = saveSingle(entidad);
		sellarActualizacion(entidad.getCodigo(), usuario);
		return entidad;
	}

	@Override
	public void sellarActualizacion(Long idEntidad, String usuario) throws Throwable {
		System.out.println("sellarActualizacion EntidadService - idEntidad: " + idEntidad + " - usuario: " + usuario);
		if (usuario == null || usuario.trim().isEmpty()) {
			// Sin usuario no es una edicion de pantalla (p.ej. un batch): no se sella,
			// para no mentir sobre quien hizo el cambio. Ver CLAUDE.md / feedback del usuario
			// del 2026-08-27.
			System.out.println("sellarActualizacion EntidadService - usuario vacio/nulo, no se sella"
				+ " (idEntidad: " + idEntidad + ")");
			return;
		}
		Entidad entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
		entidad.setFechaModificacion(LocalDateTime.now().format(FORMATO_FECHA_MODIFICACION));
		entidad.setUsuarioModificacion(usuario);
		entidadDaoService.save(entidad, entidad.getCodigo());
	}

}