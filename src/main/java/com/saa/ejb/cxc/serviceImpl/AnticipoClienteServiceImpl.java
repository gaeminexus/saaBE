package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.ejb.cxc.service.AnticipoClienteService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Estados del anticipo:
 *   1 = Ingresado  (grabado, pendiente de confirmación)
 *   2 = Confirmado (asiento contable generado)
 *   3 = Anulado
 */
@Stateless
public class AnticipoClienteServiceImpl implements AnticipoClienteService {

    @EJB
    private AnticipoClienteDaoService anticipoDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @PersistenceContext
    private EntityManager em;

    @Override
    public AnticipoCliente selectById(Long id) throws Throwable {
        System.out.println("selectById AnticipoCliente id=" + id);
        return anticipoDaoService.selectById(id, NombreEntidadesCobro.ANTICIPO_CLIENTE);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove AnticipoCliente");
        AnticipoCliente entidad = new AnticipoCliente();
        for (Long id : ids) {
            anticipoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<AnticipoCliente> lista) throws Throwable {
        System.out.println("save[] AnticipoCliente");
        for (AnticipoCliente reg : lista) {
            anticipoDaoService.save(reg, reg.getId());
        }
    }

    @Override
    public List<AnticipoCliente> selectAll() throws Throwable {
        System.out.println("selectAll AnticipoCliente");
        List<AnticipoCliente> result =
                anticipoDaoService.selectAll(NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron anticipos de clientes.");
        }
        return result;
    }

    @Override
    public List<AnticipoCliente> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria AnticipoCliente");
        List<AnticipoCliente> result =
                anticipoDaoService.selectByCriteria(datos, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (result.isEmpty()) {
            throw new IncomeException("La búsqueda de anticipos no devolvió registros.");
        }
        return result;
    }

    @Override
    public List<AnticipoCliente> selectByTitularEmpresa(Long codigoTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("selectByTitularEmpresa titular=" + codigoTitular
                + " empresa=" + idEmpresa);
        TypedQuery<AnticipoCliente> q = em.createQuery(
                "SELECT a FROM AnticipoCliente a "
                + "WHERE a.titular.codigo = :titular "
                + "AND a.empresa.codigo = :empresa "
                + "AND a.estado <> 3 "
                + "ORDER BY a.fechaAnticipo DESC",
                AnticipoCliente.class);
        q.setParameter("titular", codigoTitular);
        q.setParameter("empresa", idEmpresa);
        return q.getResultList();
    }

    @Override
    public AnticipoCliente saveSingle(AnticipoCliente entidad) throws Throwable {
        System.out.println("saveSingle AnticipoCliente");

        // Validaciones básicas
        if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
            throw new IncomeException("El anticipo debe tener un titular (cliente) asignado.");
        }
        if (entidad.getFechaAnticipo() == null) {
            throw new IncomeException("El anticipo debe tener fecha de anticipo.");
        }
        if (entidad.getValor() == null || entidad.getValor() <= 0) {
            throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
        }

        boolean esNuevo = (entidad.getId() == null);

        // En creación: estado=1 (Ingresado) y fechaRegistro automática
        if (esNuevo) {
            entidad.setEstado(1L); // 1 = Ingresado
            entidad.setFechaRegistro(LocalDateTime.now());
        }

        // No se genera asiento aquí — se genera en confirmarAnticipo
        entidad = anticipoDaoService.save(entidad, entidad.getId());
        System.out.println("✓ Anticipo guardado con ID: " + entidad.getId()
                + " | Estado: Ingresado (1)");
        return entidad;
    }

    // =========================================================================
    // confirmarAnticipo
    // =========================================================================

    @Override
    public java.util.Map<String, Object> confirmarAnticipo(Long idAnticipo, String usuario)
            throws Throwable {
        System.out.println("=== confirmarAnticipo | id=" + idAnticipo + " ===");

        java.util.Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("exito", false);

        // 1. Cargar el anticipo
        AnticipoCliente anticipo = anticipoDaoService.selectById(
                idAnticipo, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (anticipo == null) {
            resultado.put("mensaje", "No se encontró el anticipo con ID: " + idAnticipo);
            return resultado;
        }

        // 2. Validar estado actual
        if (Long.valueOf(2L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo ya está CONFIRMADO"
                    + (anticipo.getAsiento() != null
                       ? ". Asiento: " + anticipo.getAsiento().getNumeroAlterno() : "."));
            resultado.put("exito", true);
            resultado.put("estado", "YA_CONFIRMADO");
            resultado.put("anticipo", anticipo);
            return resultado;
        }
        if (Long.valueOf(3L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo está ANULADO y no puede confirmarse.");
            return resultado;
        }
        if (!Long.valueOf(1L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo tiene un estado desconocido: "
                    + anticipo.getEstado());
            return resultado;
        }

        // 3. Validar que la empresa esté configurada
        if (anticipo.getEmpresa() == null || anticipo.getEmpresa().getCodigo() == null) {
            resultado.put("mensaje", "El anticipo no tiene empresa contable configurada. "
                    + "Edite el anticipo y asigne la empresa antes de confirmar.");
            return resultado;
        }

        // 4. Generar asiento contable
        try {
            Asiento asiento = asientoContableService.generarAsientoAnticipo(
                    anticipo, TipoAsientos.ANTICIPOS_CLIENTE,
                    usuario != null ? usuario : "SISTEMA");

            // 5. Actualizar anticipo: estado=2 (Confirmado) + vincular asiento
            anticipo.setEstado(2L); // 2 = Confirmado
            anticipo.setAsiento(asiento);
            anticipo = anticipoDaoService.save(anticipo, anticipo.getId());

            resultado.put("exito", true);
            resultado.put("estado", "CONFIRMADO");
            resultado.put("mensaje", "Anticipo confirmado correctamente. "
                    + "Asiento generado: " + asiento.getNumeroAlterno());
            resultado.put("asiento", asiento.getNumeroAlterno());
            resultado.put("anticipo", anticipo);
            System.out.println("✓ Anticipo " + idAnticipo + " confirmado | Asiento: "
                    + asiento.getNumeroAlterno());

        } catch (Exception e) {
            resultado.put("mensaje", "Error al generar el asiento contable: " + e.getMessage()
                    + ". Verifique que el cliente tenga las cuentas contables configuradas "
                    + "(Tipo 2=Anticipos, Tipo 3=Caja/Banco) y que exista un período contable "
                    + "abierto para la fecha del anticipo.");
            resultado.put("error", e.getMessage());
            System.err.println("✗ Error en confirmarAnticipo: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }
}