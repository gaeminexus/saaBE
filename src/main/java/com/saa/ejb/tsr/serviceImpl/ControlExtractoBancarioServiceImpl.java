/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.PeriodoService;
import com.saa.ejb.tsr.dao.ControlExtractoBancarioDaoService;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.ControlExtractoBancarioService;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.ControlExtractoBancario;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz ControlExtractoBancarioService.
 * Contiene los servicios relacionados con la entidad ControlExtractoBancario,
 * incluyendo generarPeriodo y recalcularPeriodo, disparados explicitamente
 * desde el frontend (no por un job programado).</p>
 */
@Stateless
public class ControlExtractoBancarioServiceImpl implements ControlExtractoBancarioService {

    @EJB
    private ControlExtractoBancarioDaoService controlExtractoBancarioDaoService;

    @EJB
    private ExtractoBancarioDaoService extractoBancarioDaoService;

    @EJB
    private PeriodoService periodoService;

    @EJB
    private EmpresaService empresaService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de ControlExtractoBancario service");
        ControlExtractoBancario controlExtractoBancario = new ControlExtractoBancario();
        for (Long registro : id) {
            controlExtractoBancarioDaoService.remove(controlExtractoBancario, registro);
        }
    }

    @Override
    public void save(List<ControlExtractoBancario> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de ControlExtractoBancario service");
        for (ControlExtractoBancario registro : lista) {
            controlExtractoBancarioDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<ControlExtractoBancario> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo (selectAll) ControlExtractoBancarioService");
        List<ControlExtractoBancario> result = controlExtractoBancarioDaoService
                .selectAll(NombreEntidadesTesoreria.CONTROL_EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total ControlExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<ControlExtractoBancario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) ControlExtractoBancario");
        List<ControlExtractoBancario> result = controlExtractoBancarioDaoService
                .selectByCriteria(datos, NombreEntidadesTesoreria.CONTROL_EXTRACTO_BANCARIO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda selectByCriteria ControlExtractoBancario no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public ControlExtractoBancario selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById ControlExtractoBancario con id: " + id);
        return controlExtractoBancarioDaoService.selectById(id, NombreEntidadesTesoreria.CONTROL_EXTRACTO_BANCARIO);
    }

    @Override
    public ControlExtractoBancario saveSingle(ControlExtractoBancario controlExtractoBancario) throws Throwable {
        System.out.println("saveSingle - ControlExtractoBancario");
        // Si codigo llega como 0 desde el cliente, se trata como nuevo registro (INSERT)
        if (controlExtractoBancario.getCodigo() != null && controlExtractoBancario.getCodigo() == 0L) {
            controlExtractoBancario.setCodigo(null);
        }
        return controlExtractoBancarioDaoService.save(controlExtractoBancario, controlExtractoBancario.getCodigo());
    }

    @Override
    public ControlExtractoBancario generarPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo generarPeriodo con idEmpresa: " + idEmpresa + ", idPeriodo: " + idPeriodo);
        Periodo periodo = periodoService.selectById(idPeriodo);

        ControlExtractoBancario existente = controlExtractoBancarioDaoService
                .selectByEmpresaYPeriodo(idEmpresa, periodo.getMes(), periodo.getAnio());
        if (existente != null) {
            // Idempotente: si ya fue generado, se devuelve tal cual - totalCuentas
            // no se vuelve a calcular una vez fijado.
            return existente;
        }

        Empresa empresa = empresaService.selectById(idEmpresa);
        List<Long> cuentasActivas = controlExtractoBancarioDaoService.selectCuentasActivasPorEmpresa(idEmpresa);

        ControlExtractoBancario nuevo = new ControlExtractoBancario();
        nuevo.setEmpresa(empresa);
        nuevo.setPeriodo(periodo);
        nuevo.setMes(periodo.getMes());
        nuevo.setAnio(periodo.getAnio());
        // Plazo por defecto: 5 dias calendario despues del cierre del periodo.
        // Regla de negocio provisional - ajustar si el equipo define una politica distinta.
        nuevo.setFechaVencimiento(periodo.getUltimoDia().plusDays(5));
        nuevo.setTotalCuentas(Long.valueOf(cuentasActivas.size()));
        nuevo.setCuentasCargadas(0L);
        nuevo.setCuentasConciliadas(0L);
        nuevo.setFechaCreacion(LocalDateTime.now());
        nuevo.setEstado(Long.valueOf(Estado.ACTIVO));

        return controlExtractoBancarioDaoService.save(nuevo, null);
    }

    @Override
    public ControlExtractoBancario recalcularPeriodo(Long idEmpresa, Long idPeriodo) throws Throwable {
        System.out.println("Ingresa al metodo recalcularPeriodo con idEmpresa: " + idEmpresa + ", idPeriodo: " + idPeriodo);
        Periodo periodo = periodoService.selectById(idPeriodo);

        ControlExtractoBancario control = controlExtractoBancarioDaoService
                .selectByEmpresaYPeriodo(idEmpresa, periodo.getMes(), periodo.getAnio());
        if (control == null) {
            throw new IncomeException(
                "No existe control de extractos generado para esta empresa/periodo. Llame primero a generarPeriodo.");
        }

        List<Long> cuentasActivas = controlExtractoBancarioDaoService.selectCuentasActivasPorEmpresa(idEmpresa);

        List<Long> cuentasConCobertura = extractoBancarioDaoService
                .selectCuentasConCobertura(cuentasActivas, periodo.getPrimerDia(), periodo.getUltimoDia());
        control.setCuentasCargadas(Long.valueOf(cuentasConCobertura.size()));

        Long conciliadas = controlExtractoBancarioDaoService.contarCuentasConciliadas(cuentasActivas, idPeriodo);
        control.setCuentasConciliadas(conciliadas);

        control.setFechaCreacion(LocalDateTime.now());

        return controlExtractoBancarioDaoService.save(control, control.getCodigo());
    }
}
