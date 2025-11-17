/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejbImpl;

import java.util.List;
import java.util.regex.Pattern;
import com.saa.basico.ejb.CantidadService;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 *         <p>
 *         Implementaci�n de la interfaz CantidadService.
 *         </p>
 */
@Stateless
public class CantidadServiceImpl implements CantidadService {

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.gaemisoft.income.sistema.ejb.service.CantidadService#verificaTraslape(
     * java.util.List, java.lang.Double, java.lang.Double)
     */
    public boolean verificaTraslape(@SuppressWarnings("rawtypes") List cantidades, Double valorDesde,
            Double valorHasta) throws Throwable {
        System.out.println("Ingresa al verificaTraslapeCantidad con listado de : " + cantidades.size()
                + ", y valorDesde: " + valorDesde +
                ",valorHasta :" + valorHasta);
        boolean traslape = false;
        Object[] recuperados = null;
        Double comparaDesde = 0D;
        Double comparaHasta = 0D;
        Double fuenteDesde = valorDesde;
        Double fuenteHasta = 0D;
        if (valorHasta == null) {
            fuenteHasta = 999999999D;
        } else {
            fuenteHasta = valorHasta;
        }
        for (Object o : cantidades) {
            recuperados = (Object[]) o;
            comparaDesde = (Double) recuperados[0];
            comparaHasta = (Double) recuperados[1];
            if ((fuenteDesde >= comparaDesde) && (fuenteDesde <= comparaHasta)) {
                traslape = true;
                break;
            } else {
                if ((fuenteHasta >= comparaDesde) && (fuenteHasta <= comparaHasta)) {
                    traslape = true;
                    break;
                } else {
                    if ((comparaDesde >= fuenteDesde) && (comparaDesde <= fuenteHasta)) {
                        traslape = true;
                        break;
                    } else {
                        if ((comparaHasta >= fuenteDesde) && (comparaHasta <= fuenteHasta)) {
                            traslape = true;
                            break;
                        }
                    }
                }
            }
        }
        return traslape;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.saa.basico.ejb.CantidadService#Convertir(java.lang.String, boolean)
     */
    public String Convertir(String numero, boolean mayusculas) throws Throwable {
        String literal = "";
        String parte_decimal;
        // si el numero utiliza (.) en lugar de (,) -> se reemplaza
        numero = numero.replace(".", ",");
        // si el numero no tiene parte decimal, se le agrega ,00
        if (numero.indexOf(",") == -1) {
            numero = numero + ",00";
        }
        // se valida formato de entrada -> 0,00 y 999 999 999,00
        if (Pattern.matches("\\d{1,9},\\d{1,2}", numero)) {
            // se divide el numero 0000000,00 -> entero y decimal
            String Num[] = numero.split(",");
            // de da formato al numero decimal
            parte_decimal = Num[1] + "/100 Bolivianos.";
            // se convierte el numero a literal
            if (Integer.parseInt(Num[0]) == 0) {// si el valor es cero
                literal = "cero ";
            } else if (Integer.parseInt(Num[0]) > 999999) {// si es millon
                literal = getMillones(Num[0]);
            } else if (Integer.parseInt(Num[0]) > 999) {// si es miles
                literal = getMiles(Num[0]);
            } else if (Integer.parseInt(Num[0]) > 99) {// si es centena
                literal = getCentenas(Num[0]);
            } else if (Integer.parseInt(Num[0]) > 9) {// si es decena
                literal = getDecenas(Num[0]);
            } else {// sino unidades -> 9
                literal = getUnidades(Num[0]);
            }
            // devuelve el resultado en mayusculas o minusculas
            if (mayusculas) {
                return (literal + parte_decimal).toUpperCase();
            } else {
                return (literal + parte_decimal);
            }
        } else {// error, no se puede convertir
            return literal = null;
        }
    }

    /* funciones para convertir las unidades a literales */
    private String getUnidades(String numero) {// 1 - 9
        // si tuviera algun 0 antes se lo quita -> 09 = 9 o 009=9
        String num = numero.substring(numero.length() - 1);
        return UNIDADES[Integer.parseInt(num)];
    }

    /* funciones para convertir las decenas a literales */
    private String getDecenas(String num) {// 99
        int n = Integer.parseInt(num);
        if (n < 10) {// para casos como -> 01 - 09
            return getUnidades(num);
        } else if (n > 19) {// para 20...99
            String u = getUnidades(num);
            if (u.equals("")) { // para 20,30,40,50,60,70,80,90
                return DECENAS[Integer.parseInt(num.substring(0, 1)) + 8];
            } else {
                return DECENAS[Integer.parseInt(num.substring(0, 1)) + 8] + "y " + u;
            }
        } else {// numeros entre 11 y 19
            return DECENAS[n - 10];
        }
    }

    /* funciones para convertir las centenas a literales */
    private String getCentenas(String num) {// 999 o 099
        if (Integer.parseInt(num) > 99) {// es centena
            if (Integer.parseInt(num) == 100) {// caso especial
                return " cien ";
            } else {
                return CENTENAS[Integer.parseInt(num.substring(0, 1))] + getDecenas(num.substring(1));
            }
        } else {// por Ej. 099
                // se quita el 0 antes de convertir a decenas
            return getDecenas(Integer.parseInt(num) + "");
        }
    }

    /* funciones para convertir los miles a literales */
    private String getMiles(String numero) {// 999 999
        // obtiene las centenas
        String c = numero.substring(numero.length() - 3);
        // obtiene los miles
        String m = numero.substring(0, numero.length() - 3);
        String n = "";
        // se comprueba que miles tenga valor entero
        if (Integer.parseInt(m) > 0) {
            n = getCentenas(m);
            return n + "mil " + getCentenas(c);
        } else {
            return "" + getCentenas(c);
        }

    }

    /* funciones para convertir los millones a literales */
    private String getMillones(String numero) { // 000 000 000
        // se obtiene los miles
        String miles = numero.substring(numero.length() - 6);
        // se obtiene los millones
        String millon = numero.substring(0, numero.length() - 6);
        String n = "";
        if (millon.length() > 1) {
            n = getCentenas(millon) + "millones ";
        } else {
            n = getUnidades(millon) + "millon ";
        }
        return n + getMiles(miles);
    }

    @Override
    public Double redondea(Double valor, int numeroDecimales) throws Throwable {
        Double resultado = 0D;
        switch (numeroDecimales) {
            case 0:
                int rounded = (int) Math.round(valor);
                resultado = Double.valueOf(rounded);
                break;
            case 1:
                resultado = Math.round(valor * 10.0) / 10.0;
                break;
            case 2:
                resultado = Math.round(valor * 100.0) / 100.0;
                break;
            case 3:
                resultado = Math.round(valor * 1000.0) / 1000.0;
                break;
            case 4:
                resultado = Math.round(valor * 10000.0) / 10000.0;
                break;
            case 5:
                resultado = Math.round(valor * 100000.0) / 100000.0;
                break;
            default:
                resultado = valor;
        }
        return resultado;
    }

}
