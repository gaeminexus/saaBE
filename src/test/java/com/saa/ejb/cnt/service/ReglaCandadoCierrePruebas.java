package com.saa.ejb.cnt.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.saa.ejb.cnt.service.dto.ExcepcionCandado;
import com.saa.ejb.cnt.service.dto.FallaCandado;

/** C51: pruebas sin framework de las excepciones al candado ({@link ReglaCandadoCierre}) y de la propiedad. Correr: {@code sh tools/pruebas/correr-pruebas-c51.sh}. */
public class ReglaCandadoCierrePruebas {

    private static int fallos = 0;
    private static int pruebas = 0;

    private static void verifica(String nombre, boolean condicion) {
        pruebas++;
        if (!condicion) {
            fallos++;
            System.out.println("  FALLA: " + nombre);
        } else {
            System.out.println("  ok: " + nombre);
        }
    }

    private static List<FallaCandado> fallas() {
        List<FallaCandado> l = new ArrayList<FallaCandado>();
        l.add(new FallaCandado("CONTROL", "BANCO_X", "Banco contra extracto", Double.valueOf(-2351.20), "diferencia", true));
        l.add(new FallaCandado("CONTROL", "B17_SALDOS_NEGATIVOS", "B17 sin saldos negativos", Double.valueOf(1), "una cuenta", true));
        return l;
    }

    private static boolean rechaza(List<FallaCandado> f, List<ExcepcionCandado> e) {
        try {
            ReglaCandadoCierre.aplicar(f, e);
            return false;
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println("(a) sin excepciones bloquea; sin fallas no");
        verifica("con fallas y sin excepciones: bloquea", ReglaCandadoCierre.aplicar(fallas(), null));
        verifica("sin fallas: no bloquea", !ReglaCandadoCierre.aplicar(new ArrayList<FallaCandado>(), null));

        System.out.println("(b) una excepcion con motivo cubre su control");
        List<FallaCandado> f = fallas();
        boolean bloquea = ReglaCandadoCierre.aplicar(f, Arrays.asList(new ExcepcionCandado("banco_x", "Conciliacion pendiente del extracto")));
        verifica("cubierta (sin distinguir mayusculas)", f.get(0).isCubierta() && "Conciliacion pendiente del extracto".equals(f.get(0).getMotivoExcepcion()));
        verifica("sigue bloqueando por el otro control", bloquea && !f.get(1).isCubierta());
        f = fallas();
        bloquea = ReglaCandadoCierre.aplicar(f, Arrays.asList(new ExcepcionCandado("BANCO_X", "m1"), new ExcepcionCandado("B17_SALDOS_NEGATIVOS", "m2")));
        verifica("cubiertas las dos: no bloquea", !bloquea);

        System.out.println("(c) sin motivo o sin codigo: error de la peticion");
        verifica("motivo vacio", rechaza(fallas(), Arrays.asList(new ExcepcionCandado("BANCO_X", "  "))));
        verifica("motivo nulo", rechaza(fallas(), Arrays.asList(new ExcepcionCandado("BANCO_X", null))));
        verifica("sin codigo", rechaza(fallas(), Arrays.asList(new ExcepcionCandado(null, "m"))));

        System.out.println("(d) las condiciones de las G no se excusan");
        List<FallaCandado> g = fallas();
        g.add(new FallaCandado("G", "G_NO_VIGENTE", "Ultima ejecucion vigente", null, "desactualizada", false));
        verifica("excepcion a G_NO_VIGENTE se rechaza", rechaza(g, Arrays.asList(new ExcepcionCandado("G_NO_VIGENTE", "motivo"))));
        g = fallas();
        g.add(new FallaCandado("G", "G_NO_VIGENTE", "Ultima ejecucion vigente", null, "desactualizada", false));
        verifica("aunque se cubran los controles, las G siguen bloqueando",
                ReglaCandadoCierre.aplicar(g, Arrays.asList(new ExcepcionCandado("BANCO_X", "m1"), new ExcepcionCandado("B17_SALDOS_NEGATIVOS", "m2"))));

        System.out.println("(e) excepcion a un codigo que no falla: no hace nada");
        f = fallas();
        verifica("no se rechaza y no cubre nada", ReglaCandadoCierre.aplicar(f, Arrays.asList(new ExcepcionCandado("OTRO", "m"))) && !f.get(0).isCubierta());

        System.out.println("(f) texto del rechazo y de la bitacora");
        f = fallas();
        ReglaCandadoCierre.aplicar(f, Arrays.asList(new ExcepcionCandado("BANCO_X", "motivo uno")));
        verifica("sin cubrir nombra codigo y valor", ReglaCandadoCierre.texto(f, false).contains("B17_SALDOS_NEGATIVOS") && ReglaCandadoCierre.texto(f, false).contains("valor 1.00"));
        verifica("cubiertas nombra codigo, valor y motivo", ReglaCandadoCierre.texto(f, true).contains("BANCO_X")
                && ReglaCandadoCierre.texto(f, true).contains("-2351.20") && ReglaCandadoCierre.texto(f, true).contains("motivo uno"));

        System.out.println("(g) propiedad apagada por defecto");
        String anterior = System.getProperty(CandadoCierreContableService.PROPIEDAD_CANDADO);
        System.clearProperty(CandadoCierreContableService.PROPIEDAD_CANDADO);
        verifica("apagada sin propiedad", !CandadoCierreContableService.activo());
        System.setProperty(CandadoCierreContableService.PROPIEDAD_CANDADO, "true");
        verifica("encendida con true", CandadoCierreContableService.activo());
        if (anterior != null) {
            System.setProperty(CandadoCierreContableService.PROPIEDAD_CANDADO, anterior);
        } else {
            System.clearProperty(CandadoCierreContableService.PROPIEDAD_CANDADO);
        }

        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
