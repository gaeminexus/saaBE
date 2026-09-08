package com.saa.ejb.rhh.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ValorNoPagado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Valor no pagado a un empleado en un periodo de nomina: se retiene en el periodo en que se
 * registra y se devuelve completo en el periodo siguiente, sin afectar la contabilidad del rol
 * ni las provisiones — solo mueve remuneraciones por pagar en el momento del pago. Ver
 * docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md.</p>
 *
 * <p>Ciclo del registro: {@code REGISTRADO} (al crear, antes de procesar el rol de P) -&gt;
 * {@code RETENIDO} (al generar la orden de pago de P, ver {@code GeneracionOrdenPagoServiceImpl})
 * -&gt; {@code PAGADO} (al confirmar el pago de la orden de P+1). O {@code ANULADO} desde
 * REGISTRADO, o {@code FINIQUITADO} si el empleado sale antes de cobrarlo.</p>
 */
@Local
public interface ValorNoPagadoService extends EntityService<ValorNoPagado> {

    /**
     * Registra un valor no pagado. Exige: empleado activo de la empresa, periodo ABIERTO
     * (estado &le; {@code RhhEstadoPeriodoNomina.ABIERTO}), valor &gt; 0, motivo obligatorio, y
     * ningun otro registro vivo (REGISTRADO o RETENIDO) para ese empleado y periodo. Si
     * {@code valor} supera el salario base del contrato vigente del empleado, avisa en el
     * resultado sin bloquear (la validacion dura, contra el neto real, la hace el motor al
     * procesar el rol — al registrar el neto todavia no existe).
     *
     * @param idEmpresa			: Id de la empresa
     * @param idEmpleado		: Id del empleado
     * @param idPeriodo			: Id del periodo de nomina en que NO se paga (P)
     * @param valor				: Valor X, debe ser positivo
     * @param motivo			: Motivo del registro, obligatorio
     * @param usuario			: Usuario que ejecuta
     * @return					: Mapa con exito, idRegistro, y advertencia (String o null) si X
     *							  supera el salario base
     * @throws Throwable		: IncomeException si algun requisito no se cumple
     */
    Map<String, Object> registrar(Long idEmpresa, Long idEmpleado, Long idPeriodo, Double valor,
            String motivo, String usuario) throws Throwable;

    /**
     * Anula un registro. Solo permitido desde REGISTRADO — un RETENIDO ya afecto una orden de
     * pago; ahi la via es revertir la orden, no anular el registro.
     *
     * @param idRegistro		: Id del registro
     * @param motivo			: Motivo de la anulacion, obligatorio
     * @param usuario			: Usuario que ejecuta
     * @return					: Mapa con exito, idRegistro, mensaje
     * @throws Throwable		: IncomeException si no esta REGISTRADO
     */
    Map<String, Object> anular(Long idRegistro, String motivo, String usuario) throws Throwable;

    /**
     * Listado con filtros de servidor (null = sin filtrar por ese criterio).
     *
     * @param idEmpresa			: Id de la empresa, obligatorio
     * @param idPeriodo			: Id del periodo de nomina; null = todos
     * @param idEmpleado		: Id del empleado; null = todos
     * @param estados			: Codigos alternos del detalle del rubro RHH_ESTADO_VALOR_NO_PAGADO; null/vacio = todos
     * @return					: Filas encontradas
     * @throws Throwable		: Excepcion
     */
    List<ValorNoPagado> listar(Long idEmpresa, Long idPeriodo, Long idEmpleado, List<Long> estados)
            throws Throwable;

}
