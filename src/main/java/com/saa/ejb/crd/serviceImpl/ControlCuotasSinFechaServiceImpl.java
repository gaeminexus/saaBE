package com.saa.ejb.crd.serviceImpl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.crd.service.ControlCuotasSinFechaService;
import com.saa.rubros.EstadoCuotaPrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** C48: ver {@link ControlCuotasSinFechaService}. Solo lectura. */
@Stateless
@SuppressWarnings("unchecked")
public class ControlCuotasSinFechaServiceImpl implements ControlCuotasSinFechaService {

    /** Máximo de préstamos que se listan en el texto (el total siempre se dice). */
    static final int MAXIMO_PRESTAMOS_LISTADOS = 30;

    @PersistenceContext
    EntityManager em;

    @Override
    public long contar() throws Throwable {
        System.out.println("ControlCuotasSinFechaService.contar");
        Object r = em.createNativeQuery(
                " SELECT COUNT(*) FROM CRD.DTPR d WHERE d.DTPRESTD = " + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA
                + " AND d.DTPRFCPG IS NULL").getSingleResult();
        return r instanceof Number ? ((Number) r).longValue() : 0L;
    }

    @Override
    public String textoControl() throws Throwable {
        System.out.println("ControlCuotasSinFechaService.textoControl");
        List<Object[]> filas = em.createNativeQuery(
                " SELECT p.PRSTCDGO, p.PRSTIDAS, d.DTPRNMCT FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO"
                + " WHERE d.DTPRESTD = " + EstadoCuotaPrestamo.CANCELADA_ANTICIPADA + " AND d.DTPRFCPG IS NULL"
                + " ORDER BY p.PRSTCDGO, d.DTPRNMCT").getResultList();
        return armaTexto(filas);
    }

    /** Arma el texto a partir de filas {préstamo, operación, número de cuota}; {@code null} si no hay filas. Pura: se puede probar sin base. */
    public static String armaTexto(List<Object[]> filas) {
        if (filas == null || filas.isEmpty()) {
            return null;
        }
        Map<Long, List<String>> porPrestamo = new LinkedHashMap<Long, List<String>>();
        Map<Long, Object> operacion = new LinkedHashMap<Long, Object>();
        for (Object[] f : filas) {
            Long prestamo = ((Number) f[0]).longValue();
            porPrestamo.computeIfAbsent(prestamo, k -> new ArrayList<String>()).add(f[2] != null ? String.valueOf(f[2]) : "?");
            operacion.putIfAbsent(prestamo, f[1]);
        }
        StringBuilder sb = new StringBuilder("Hay ").append(filas.size()).append(" cuota(s) en CANCELADA_ANTICIPADA (7) sin fecha de pago (DTPRFCPG) en ")
            .append(porPrestamo.size()).append(" préstamo(s): las lecturas al corte las toman como NO liquidadas y el libro de devengo, la provisión, las bandas y la G48"
                + " las leen vivas. Préstamo (operación) cuotas:");
        int n = 0;
        for (Map.Entry<Long, List<String>> e : porPrestamo.entrySet()) {
            if (n++ >= MAXIMO_PRESTAMOS_LISTADOS) {
                sb.append(" ... y ").append(porPrestamo.size() - MAXIMO_PRESTAMOS_LISTADOS).append(" préstamo(s) más.");
                break;
            }
            Object op = operacion.get(e.getKey());
            sb.append(' ').append(e.getKey()).append(op != null ? " (" + op + ")" : "").append(' ').append(e.getValue()).append(';');
        }
        sb.append(" Corrección: docs/logica-negocio/crd/sql/412_C48_DTPRFCPG_CUOTAS_7_SIN_FECHA.sql (con el visto bueno del cuadre).");
        return sb.toString();
    }

    @Override
    public List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes) throws Throwable {
        System.out.println("ControlCuotasSinFechaService.controlesCierre - empresa: " + idEmpresa + " - " + anio + "-" + mes);
        ControlCierre c = new ControlCierre("CARTERA", "CARTERA_CUOTAS7_SIN_FECHA", "Cuotas canceladas anticipadamente (7) sin fecha de pago");
        String texto = textoControl();
        long cuantas = contar();
        c.setValor(Double.valueOf(cuantas));
        c.setEsperado(Double.valueOf(0));
        c.setDiferencia(Double.valueOf(cuantas));
        if (texto == null) {
            c.setEstado(ControlCierre.OK);
            c.setDetalle("Ninguna cuota en estado 7 sin DTPRFCPG.");
        } else {
            c.setEstado(ControlCierre.ERROR);
            c.setDetalle(texto + (ControlCuotasSinFechaService.bloqueoActivo()
                ? " La propiedad " + PROPIEDAD_BLOQUEO + " está encendida: el ejecutar del cierre de cartera se rechaza."
                : " No bloquea el cierre (propiedad " + PROPIEDAD_BLOQUEO + " apagada)."));
        }
        List<ControlCierre> lista = new ArrayList<ControlCierre>();
        lista.add(c);
        return lista;
    }
}
