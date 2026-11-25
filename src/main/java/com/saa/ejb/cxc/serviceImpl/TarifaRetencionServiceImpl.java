package com.saa.ejb.cxc.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.service.TarifaRetencionService;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @see TarifaRetencionService
 * @author Sistema SAA
 * @since 2026-11-25
 */
@Stateless
public class TarifaRetencionServiceImpl implements TarifaRetencionService {

    private static final String TABLA_RENTA = "608";
    private static final double TOLERANCIA_PORCENTAJE = 0.005;
    private static final double TOLERANCIA_VALOR = 0.01 + 1e-9;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Double> validarRenta(List<Linea> lineas) throws Exception {
        System.out.println("TarifaRetencionService.validarRenta - líneas: " + (lineas != null ? lineas.size() : 0));
        List<Double> valores = new ArrayList<Double>();
        List<String> rechazos = new ArrayList<String>();
        if (lineas == null) {
            return valores;
        }
        for (Linea l : lineas) {
            if (l.codImpuesto == null || !"1".equals(l.codImpuesto.trim())) {
                valores.add(l.valor); // IVA e ISD: como hoy
                continue;
            }
            String cod = l.codRetencion != null ? l.codRetencion.trim() : "";
            if (cod.isEmpty()) {
                rechazos.add(l.etiqueta + ": falta el código de retención.");
                valores.add(l.valor);
                continue;
            }
            if (l.base == null || l.base <= 0.0) {
                rechazos.add(l.etiqueta + ": la base imponible debe ser mayor que cero.");
                valores.add(l.valor);
                continue;
            }
            List<Double> tarifas = em.createQuery(
                    "select t.porcentaje from Tsri t where t.lsri.tabla = :tabla and trim(t.codigo) = :codigo and t.estado = 1", Double.class)
                .setParameter("tabla", TABLA_RENTA).setParameter("codigo", cod).getResultList();
            if (tarifas.isEmpty() || tarifas.get(0) == null) {
                rechazos.add(l.etiqueta + ": el código de retención " + cod + " no tiene una tarifa de renta activa.");
                valores.add(l.valor);
                continue;
            }
            if (tarifas.size() > 1) {
                rechazos.add(l.etiqueta + ": el código " + cod + " tiene " + tarifas.size() + " tarifas de renta activas; corrija el catálogo.");
                valores.add(l.valor);
                continue;
            }
            double tarifa = tarifas.get(0);
            double pct = l.porcentaje != null ? l.porcentaje : -1.0;
            if (Math.abs(pct - tarifa) > TOLERANCIA_PORCENTAJE) {
                rechazos.add(l.etiqueta + ": el porcentaje " + pct + " no es el de la tarifa activa del código " + cod + " (" + tarifa + ").");
                valores.add(l.valor);
                continue;
            }
            double calculado = Math.round(l.base * tarifa) / 100.0;
            double recibido = l.valor != null ? l.valor : 0.0;
            if (Math.abs(calculado - recibido) > TOLERANCIA_VALOR) {
                rechazos.add(l.etiqueta + ": el valor retenido " + recibido + " difiere de base × porcentaje (" + calculado + ") en más de un centavo.");
                valores.add(l.valor);
                continue;
            }
            if (Math.abs(calculado - recibido) > 1e-9) {
                System.out.println("  " + l.etiqueta + ": valor " + recibido + " -> " + calculado + " (manda el servidor)");
            }
            valores.add(calculado);
        }
        if (!rechazos.isEmpty()) {
            throw new IncomeException("Retención rechazada, líneas de renta con tarifa inválida: " + String.join(" | ", rechazos));
        }
        return valores;
    }
}
