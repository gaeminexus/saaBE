package com.saa.ejb.rpr.daoImpl;

import java.util.List;

import com.saa.ejb.rpr.dao.CatalogoGeograficoDaoService;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class CatalogoGeograficoDaoServiceImpl implements CatalogoGeograficoDaoService {

    @PersistenceContext
    EntityManager em;

    /** Código INEC de 2 dígitos; "00" (filas históricas, p. ej. "MORONA SANTIAGO ANT") no es un código válido. */
    static String dosDigitos(Object codigo) {
        if (codigo == null) return null;
        String c = codigo.toString().trim();
        if (!c.matches("\\d{1,2}")) return null;
        c = c.length() == 1 ? "0" + c : c;
        return "00".equals(c) ? null : c;
    }

    static String normalizar(String s) {
        if (s == null) return null;
        String n = java.text.Normalizer.normalize(s.trim().toUpperCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.isEmpty() ? null : n;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String codigoProvincia(String nombreOCodigo) throws Throwable {
        String n = normalizar(nombreOCodigo);
        if (n == null) return null;
        if (n.matches("\\d{1,2}")) return dosDigitos(n);
        List<Object> r = em.createNativeQuery(
                " select p.PRVNCDAL from CRD.PRVN p "
              + " where upper(translate(trim(p.PRVNNMBR), 'áéíóúÁÉÍÓÚ', 'aeiouAEIOU')) = :n "
              + "   and p.PRVNCDAL is not null and p.PRVNCDAL <> '00' order by p.PRVNCDGO")
                .setParameter("n", n).getResultList();
        return r.isEmpty() ? null : dosDigitos(r.get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String codigoCanton(String provincia, String nombreOCodigoCanton) throws Throwable {
        String n = normalizar(nombreOCodigoCanton);
        if (n == null) return null;
        if (n.matches("\\d{1,2}")) return dosDigitos(n);
        String prov = codigoProvincia(provincia);
        if (prov == null) return null;
        List<Object> r = em.createNativeQuery(
                " select c.CDDDCDAL from CRD.CDDD c join CRD.PRVN p on p.PRVNCDGO = c.PRVNCDGO "
              + " where lpad(trim(p.PRVNCDAL), 2, '0') = :prov "
              + "   and upper(translate(trim(c.CDDDNMBR), 'áéíóúÁÉÍÓÚ', 'aeiouAEIOU')) = :n "
              + "   and c.CDDDCDAL is not null order by c.CDDDCDGO")
                .setParameter("prov", prov).setParameter("n", n).getResultList();
        return r.isEmpty() ? null : dosDigitos(r.get(0));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String[] ubicacionEntidad(Long codigoEntidad) throws Throwable {
        if (codigoEntidad == null) return null;
        List<Object[]> r = em.createNativeQuery(
                " select p.PRVNCDAL, c.CDDDCDAL, q.PRRQCDEX "
              + " from CRD.DRCC d "
              + " join CRD.PRRQ q on q.PRRQCDGO = d.PRRQCDGO "
              + " join CRD.CDDD c on c.CDDDCDGO = q.CDDDCDGO "
              + " join CRD.PRVN p on p.PRVNCDGO = c.PRVNCDGO "
              + " where d.ENTDCDGO = :ent and d.DRCCIDST = 1 "
              + " order by d.DRCCCDGO desc")
                .setParameter("ent", codigoEntidad).getResultList();
        if (r.isEmpty()) return null;
        Object[] f = r.get(0);
        return new String[] { dosDigitos(f[0]), dosDigitos(f[1]), dosDigitos(f[2]) };
    }
}
