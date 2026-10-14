-- =============================================================================================
-- Cambia el nombre de la empresa en una base de PRUEBAS, para distinguirla a simple vista:
--   servidor 192.168.2.4  ->  ASOPREP CUADRE
--   servidor 192.168.2.3  ->  ASOPREP PRUEBAS
--
-- De donde sale el nombre (verificado en el codigo el 2026-10-14):
--   SCP.PJRQ.PJRQNMBR (entidad com.saa.model.scp.Empresa) -> empresa.nombre en el login
--   -> sessionStorage/localStorage 'empresaName' (saaFE usuario.service / app-state.service)
--   -> titulo del encabezado (saaFE app.ts getScreenTitle).
--   Tambien lo imprimen los reportes que leen PJRQNMBR (balances de cnt, anticipos de tsr,
--   nota de venta de cxp): en pruebas saldran con el nombre nuevo, que es lo que se busca.
--   Ningun codigo compara el texto 'ASOPREP' de la empresa (el "ASOPREP" del archivo Petro es
--   una constante aparte, GeneracionArchivoPetroServiceImpl:81).
--
-- OJO: SCP.PJRQ guarda empresas Y usuarios (com.saa.model.scp.Usuario mapea la misma tabla).
--      Por eso se cambia UNA sola fila, la llamada exactamente 'ASOPREP'; si hay 0 o mas de 1,
--      no se cambia nada.
--
-- Guardas:
--   1. Se corre SIEMPRE con "docker exec -i saa-pruebas ...": la conexion es la del contenedor de
--      pruebas, no hay forma de que llegue a produccion.
--   2. Si la base tiene algun facturador en ambiente SRI 2 (produccion), se detiene: el clon deja
--      todos en 1, asi que una base con AMBIENTE = 2 NO es la de pruebas.
--
-- Uso (PowerShell, en el servidor, desde D:\saa-pruebas), cambiando solo el nombre:
--   @("define nuevo_nombre = 'ASOPREP CUADRE'") + (Get-Content .\sql\renombrar-empresa-pruebas.sql) |
--     docker exec -i saa-pruebas bash -c 'sqlplus -s -l "system/$ORACLE_PWD@localhost:1521/FREEPDB1"'
--
-- Despues: cerrar sesion en el SAA de pruebas y volver a entrar (el nombre queda guardado en el
-- navegador al iniciar sesion).
--
-- Sin tildes a proposito: PowerShell 5.1 manda el texto a docker en ASCII.
-- =============================================================================================

whenever sqlerror exit failure rollback
set serveroutput on feedback off verify off heading on pages 100 lines 200

prompt === ANTES: filas de SCP.PJRQ que contienen ASOPREP
select PJRQCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD from SCP.PJRQ
 where upper(PJRQNMBR) like '%ASOPREP%' order by PJRQCDGO;

declare
  v_prod   number;
  v_filas  number;
  v_nombre varchar2(200) := '&nuevo_nombre';
begin
  if v_nombre is null or upper(v_nombre) not like 'ASOPREP %' then
    raise_application_error(-20001, 'nombre nuevo invalido: "' || v_nombre || '". Se esperaba ASOPREP CUADRE o ASOPREP PRUEBAS.');
  end if;

  select count(*) into v_prod from CBR.FCDR where AMBIENTE = 2;
  if v_prod > 0 then
    raise_application_error(-20002, 'hay ' || v_prod || ' facturador(es) en ambiente SRI de PRODUCCION: esta base NO parece la de pruebas. No se cambia nada.');
  end if;

  select count(*) into v_filas from SCP.PJRQ where PJRQNMBR = 'ASOPREP';
  if v_filas <> 1 then
    raise_application_error(-20003, 'se esperaba exactamente 1 fila con PJRQNMBR = ''ASOPREP'' y hay ' || v_filas || '. No se cambia nada; pega la lista de ANTES.');
  end if;

  update SCP.PJRQ set PJRQNMBR = v_nombre where PJRQNMBR = 'ASOPREP';
  dbms_output.put_line('filas cambiadas: ' || sql%rowcount || '  ->  ' || v_nombre);
  commit;
end;
/

prompt === DESPUES
select PJRQCDGO, PJRQNMBR, PJRQNVLL, PJRQCDPD from SCP.PJRQ
 where upper(PJRQNMBR) like '%ASOPREP%' order by PJRQCDGO;

exit

-- ---------------------------------------------------------------------------------------------
-- REVERSO (solo en la base de pruebas, con el mismo docker exec):
--   update SCP.PJRQ set PJRQNMBR = 'ASOPREP' where PJRQNMBR in ('ASOPREP CUADRE', 'ASOPREP PRUEBAS');
--   commit;
-- ---------------------------------------------------------------------------------------------
