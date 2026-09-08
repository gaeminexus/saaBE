package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
import com.saa.ejb.rhh.service.ValorNoPagadoService;
import com.saa.ejb.rhh.util.PeriodoModificableNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ValorNoPagado;
import com.saa.model.scp.Empresa;
import com.saa.rubros.RhhEstadoEmpleado;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhEstadoValorNoPagado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * Implementacion de ValorNoPagadoService.
 */
@Stateless
public class ValorNoPagadoServiceImpl implements ValorNoPagadoService {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private ValorNoPagadoDaoService valorNoPagadoDaoService;

    @EJB
    private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

    // =====================================================================
    // EntityService — los seis de la casa
    // =====================================================================

    @Override
    public ValorNoPagado selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ValorNoPagado con id: " + id);
        return valorNoPagadoDaoService.selectById(id, NombreEntidadesRhh.VALOR_NO_PAGADO);
    }

    @Override
    public List<ValorNoPagado> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll ValorNoPagadoService");
        List<ValorNoPagado> result = valorNoPagadoDaoService.selectAll(NombreEntidadesRhh.VALOR_NO_PAGADO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ValorNoPagado no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<ValorNoPagado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria ValorNoPagadoService");
        List<ValorNoPagado> result = valorNoPagadoDaoService
                .selectByCriteria(datos, NombreEntidadesRhh.VALOR_NO_PAGADO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio ValorNoPagado no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ValorNoPagado saveSingle(ValorNoPagado entidad) throws Throwable {
        System.out.println("saveSingle - ValorNoPagado");
        return valorNoPagadoDaoService.save(entidad, entidad.getCodigo());
    }

    @Override
    public void save(List<ValorNoPagado> lista) throws Throwable {
        for (ValorNoPagado registro : lista) {
            saveSingle(registro);
        }
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        // No se borra fisicamente (§8 del plan): ANULADO es un estado, no un DELETE. Este
        // metodo existe solo porque lo exige EntityService; el REST lo deshabilita (405).
        throw new IncomeException("ValorNoPagado no se elimina fisicamente. Use"
                + " POST /vnpg/anular/{id} con un motivo.");
    }

    // =====================================================================
    // Ciclo del registro
    // =====================================================================

    @Override
    public Map<String, Object> registrar(Long idEmpresa, Long idEmpleado, Long idPeriodo, Double valor,
            String motivo, String usuario) throws Throwable {
        System.out.println("=== registrar valor no pagado | idEmpresa=" + idEmpresa + " | idEmpleado="
                + idEmpleado + " | idPeriodo=" + idPeriodo + " | valor=" + valor + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar idEmpresa.");
        }
        if (idEmpleado == null) {
            throw new IncomeException("Debe indicar idEmpleado.");
        }
        if (idPeriodo == null) {
            throw new IncomeException("Debe indicar idPeriodo.");
        }
        if (valor == null || valor.doubleValue() <= 0D) {
            throw new IncomeException("El valor debe ser mayor a cero.");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo del registro.");
        }

        Empresa empresa = em.find(Empresa.class, idEmpresa);
        if (empresa == null) {
            throw new IncomeException("No existe la empresa " + idEmpresa + ".");
        }
        Empleado empleado = em.find(Empleado.class, idEmpleado);
        if (empleado == null) {
            throw new IncomeException("No existe el empleado " + idEmpleado + ".");
        }
        if (empleado.getEmpresa() == null || !idEmpresa.equals(empleado.getEmpresa().getCodigo())) {
            throw new IncomeException("El empleado " + idEmpleado + " no pertenece a la empresa " + idEmpresa + ".");
        }
        if (empleado.getEstado() == null || empleado.getEstado().intValue() != RhhEstadoEmpleado.ACTIVO) {
            throw new IncomeException("El empleado " + idEmpleado + " no esta ACTIVO: no se le puede"
                    + " registrar un valor no pagado.");
        }

        PeriodoNomina periodo = em.find(PeriodoNomina.class, idPeriodo);
        if (periodo == null) {
            throw new IncomeException("No existe el periodo de nomina " + idPeriodo + ".");
        }
        PeriodoModificableNomina.exige(periodo, "registrar un valor no pagado");

        ValorNoPagado vivo = valorNoPagadoDaoService.selectVivoByEmpleadoPeriodo(idEmpleado, idPeriodo);
        if (vivo != null) {
            throw new IncomeException("Ya existe un registro vivo (id " + vivo.getCodigo() + ", estado "
                    + textoEstado(vivo.getEstado()) + ") para el empleado " + idEmpleado + " en el periodo "
                    + periodo.getMes() + "/" + periodo.getAnio() + ". Anulelo antes de registrar otro.");
        }

        ValorNoPagado registro = new ValorNoPagado();
        registro.setEmpresa(empresa);
        registro.setEmpleado(empleado);
        registro.setPeriodoNomina(periodo);
        registro.setValor(valor);
        registro.setMotivo(motivo.trim());
        registro.setEstado(Long.valueOf(RhhEstadoValorNoPagado.REGISTRADO));
        registro.setFechaRegistro(LocalDateTime.now());
        registro.setUsuarioRegistro(usuario);
        registro = valorNoPagadoDaoService.save(registro, registro.getCodigo());

        String advertenciaRecalculo = Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO).equals(periodo.getEstado())
                ? "El rol de este periodo ya esta calculado: recalculelo para que el valor aparezca en el rol."
                : null;
        String advertencia = combinaAdvertencias(advertenciaRecalculo,
                advertenciaSiSuperaSalario(empleado, periodo, valor));

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idRegistro", registro.getCodigo());
        resultado.put("estado", registro.getEstado());
        resultado.put("estadoTexto", textoEstado(registro.getEstado()));
        resultado.put("advertencia", advertencia);
        resultado.put("mensaje", "Valor no pagado registrado.");
        System.out.println("✓ Valor no pagado registrado: id=" + registro.getCodigo()
                + (advertencia != null ? " | ADVERTENCIA: " + advertencia : ""));
        return resultado;
    }

    @Override
    public Map<String, Object> anular(Long idRegistro, String motivo, String usuario) throws Throwable {
        System.out.println("=== anular valor no pagado | idRegistro=" + idRegistro + " ===");

        ValorNoPagado registro = em.find(ValorNoPagado.class, idRegistro);
        if (registro == null) {
            throw new IncomeException("No existe el registro de valor no pagado " + idRegistro + ".");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }
        if (!Long.valueOf(RhhEstadoValorNoPagado.REGISTRADO).equals(registro.getEstado())) {
            throw new IncomeException("El registro " + idRegistro + " esta " + textoEstado(registro.getEstado())
                    + ", no REGISTRADO. Un valor RETENIDO ya afecto una orden de pago: revierta la orden"
                    + " en vez de anular el registro.");
        }
        if (registro.getPeriodoNomina() != null) {
            PeriodoModificableNomina.exige(registro.getPeriodoNomina(), "anular un valor no pagado");
        }

        registro.setEstado(Long.valueOf(RhhEstadoValorNoPagado.ANULADO));
        registro.setMotivoAnulacion(motivo.trim());
        registro.setFechaAnulacion(LocalDateTime.now());
        registro.setUsuarioAnulacion(usuario);
        registro = valorNoPagadoDaoService.save(registro, registro.getCodigo());

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("exito", Boolean.TRUE);
        resultado.put("idRegistro", registro.getCodigo());
        resultado.put("mensaje", "Valor no pagado anulado.");
        System.out.println("✓ Valor no pagado " + idRegistro + " anulado. Motivo: " + motivo);
        return resultado;
    }

    @Override
    public List<ValorNoPagado> listar(Long idEmpresa, Long idPeriodo, Long idEmpleado, List<Long> estados)
            throws Throwable {
        System.out.println("=== listar valores no pagados | idEmpresa=" + idEmpresa + " | idPeriodo="
                + idPeriodo + " | idEmpleado=" + idEmpleado + " | estados=" + estados + " ===");
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar idEmpresa.");
        }
        return valorNoPagadoDaoService.selectListado(idEmpresa, idPeriodo, idEmpleado, estados);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    // exigePeriodoModificable/textoEstadoPeriodo centralizados el 2026-09-08 en
    // PeriodoModificableNomina (com.saa.ejb.rhh.util): esta era la primera de tres copias
    // identicas, junto con OrdenBeneficioSocialServiceImpl y SolicitudVacacionesServiceImpl.

    /**
     * Junta advertencias no nulas en un solo texto, o null si ninguna aplica.
     */
    private String combinaAdvertencias(String... advertencias) {
        StringBuilder combinada = new StringBuilder();
        for (String advertencia : advertencias) {
            if (advertencia != null && !advertencia.trim().isEmpty()) {
                if (combinada.length() > 0) {
                    combinada.append(" | ");
                }
                combinada.append(advertencia.trim());
            }
        }
        return combinada.length() > 0 ? combinada.toString() : null;
    }

    /**
     * Validacion blanda (§8 del plan): si X supera el salario base del contrato vigente en el
     * periodo, avisa sin bloquear -- el neto real puede ser mayor por horas extra u otros
     * ingresos. La dura, contra el neto calculado, la hace el motor (§7.1.3), no este metodo.
     */
    private String advertenciaSiSuperaSalario(Empleado empleado, PeriodoNomina periodo, Double valor)
            throws Throwable {
        Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;
        if (idEmpresa == null || periodo.getFechaInicio() == null || periodo.getFechaFin() == null) {
            return null;
        }
        List<ContratoEmpleado> contratos = contratoEmpleadoDaoService
                .selectActivosEnPeriodo(idEmpresa, periodo.getFechaInicio(), periodo.getFechaFin());
        if (contratos == null) {
            return null;
        }
        for (ContratoEmpleado contrato : contratos) {
            if (contrato.getEmpleado() == null || !empleado.getCodigo().equals(contrato.getEmpleado().getCodigo())) {
                continue;
            }
            if (contrato.getSalarioBase() != null && valor.doubleValue() > contrato.getSalarioBase().doubleValue()) {
                return "El valor " + valor + " supera el salario base del contrato ("
                        + contrato.getSalarioBase() + "). Verifique antes de continuar.";
            }
        }
        return null;
    }

    private String textoEstado(Long estado) {
        if (estado == null) {
            return null;
        }
        switch (estado.intValue()) {
            case RhhEstadoValorNoPagado.REGISTRADO:
                return "REGISTRADO";
            case RhhEstadoValorNoPagado.RETENIDO:
                return "RETENIDO";
            case RhhEstadoValorNoPagado.PAGADO:
                return "PAGADO";
            case RhhEstadoValorNoPagado.ANULADO:
                return "ANULADO";
            case RhhEstadoValorNoPagado.FINIQUITADO:
                return "FINIQUITADO";
            default:
                return "ESTADO " + estado;
        }
    }
}
