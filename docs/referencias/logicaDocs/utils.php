<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

    //Abrir conexion a la base de datos
    function connect($db)
    {
        try {
            $conn = new PDO("mysql:host={$db['host']};dbname={$db['db']};charset=utf8", $db['username'], $db['password']);
            // set the PDO error mode to exception
            $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            return $conn;
        } catch (PDOException $exception) {
            exit($exception->getMessage());
        }
    }
    
    function salir($dbConn){
        $dbConn = null;
        exit;
    }

    //Obtener parametros para updates
    function getParams($input)
    {
    $filterParams = [];
    foreach($input as $param => $value)
    {
            $filterParams[] = "$param=:$param";
    }
    return implode(", ", $filterParams);
    }

    //Obtener parametros para updates
    function getCampos($input)
    {
        $filterParams = [];
        foreach($input as $param => $value)
        {
            $filterParams[] = "$param";
        }
        return implode(", ", $filterParams);
    }

    //Asociar todos los parametros a un sql
    function bindAllValues($statement, $params){
        $cadena = "";
        foreach($params as $param => $value){            
            if($value==''){
                $value=null;
            }
            $cadena = $cadena."[:$param]/[$value]";
            $statement->bindValue(':'.$param, $value);
        }
        error_log($cadena);
        return $statement;
    }

    //Asociar todos los parametros a un sql, validando arrays
    function newBindAllValues($statement, $params){
        foreach($params as $param => $value){
            error_log(":$param"."/".$value."/", 0);
            if(is_array($value)){
                $statement->bindValue(':'.$param, $value['id']);
            }else{
                $statement->bindValue(':'.$param, $value);
            }            
        }
        return $statement;
    }
    
    //Asociar todos los parametros a un sql
    function bindAllValuesP($statement, $params, $fields)
    {
        $i=0;
    foreach($params as $param => $value)
    {
        // $statement->bindValue($i, $params[$fields[$i-1]]);
        // $statement->bindValue(':'.$fields[$i], $params[$fields[$i]]);
        // error_log(":$param"."/".$params[$param]."/", 0);
        $statement->bindValue(':'.$fields[$i], $params[$fields[$i]]);
        $i=$i+1;
        error_log("-----------------");
    }
    return $statement;
    }

    //VERIFICAR CAMPOS
    function verificaCampos($statement, $params, $fields)
    {
        $i=0;
    foreach($params as $param => $value)
    {
        error_log("busca:$param");
        if (in_array($param, $fields)) {
            error_log("ENCONTRADO");
        } else {
            error_log("FALLO");
        }
        $i=$i+1;
        error_log("-----------------");
    }
    return $statement;
    }

    //Imprime arreglo
    function imprimeArreglo($params)
    {
    foreach($params as $param => $value)
    {
        error_log("************************");
        error_log("-->[$param][$value]", 0);
        if($value){
            error_log("Valor Existe");
        }else{
            error_log("Valor NO Existe");
        }
        if(empty($value)){
            error_log("Vacia");
        }else{
            error_log("NO Vacia");
        }
        error_log("-->[$param][$value]", 0);
    }
    }

    function getValorCampo($params, $campo)
    {
    $valor="";
    foreach($params as $param => $value)
    {
        if($param==$campo){
            $valor=$value;
            error_log("GetValorCampo------>[$param][$value]", 0);
        }           
        error_log("NadaValorCampo-->[$param][$value]", 0);
    }
    return $valor;
    } 

    function encLinkUsuario($codigo){
        $inicio="compraenfenedif";
        $fin=array("fin","fenedif","gaemisoft","desarrollo","compra","en");
        $id=$codigo;
        $variable = random_int(0, 5);
        $link=md5($inicio).$id.".dx".md5($fin[$variable]);
    return $link;
    }

    function desLinkUsuario($link){
        $inicio="compraenfenedif";
        $cadena=$link;
        $largo=strpos($cadena,".dx");
        $codigo=str_replace(md5($inicio),"",substr($cadena,0,$largo));
    return $codigo;
    }
    
// Funciones Select para todas las tablas
    function getEntidad($dbConn, $ntabla, $id, $campos){
        error_log("tabla[$ntabla] id[$id] Campos[$campos]", 0);
        $sql = $dbConn->prepare("SELECT $campos FROM $ntabla where id=:id");
        $sql->bindValue(':id', $id);
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $resultado=$sql->fetch();
        return $resultado;
    }
    
    function getEntidadFull($dbConn, $ntabla, $id, $campos, $entidades){
        error_log("GET FULL tabla[$ntabla] id[$id] Campos[$campos]", 0);
        $resultado = getEntidad($dbConn, $ntabla, $id, $campos);
        foreach ($entidades as $datos) {
            $tabla = $datos['tabla'];
            $campo = $datos['campo'];
            $dato = $resultado[$campo];
            $sql = $dbConn->prepare("SELECT $campos FROM $tabla where id=:id");
            $sql->bindValue(':id', $dato);
            $sql->execute();
            $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
            $resultado=array_replace($resultado, $consulta);
        }
        return $resultado;
    }
    
    function getEntidadByCampo($dbConn, $ntabla, $campos, $byCampo, $valor){
        error_log("tabla[$ntabla] byCampo[$byCampo] Valor[$valor]", 0);
        $sql = $dbConn->prepare("SELECT $campos FROM $ntabla where $byCampo=:valor");
        $sql->bindValue(':valor', $valor);
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $resultado=$sql->fetch();
        return $resultado;
    }
    
    function getEntidadByCampoFull($dbConn, $ntabla, $campos, $byCampo, $valor, $entidades){
        error_log("getEntidadByCampoFull", 0);
        $registros = getEntidadByCampo($dbConn, $ntabla, $campos, $byCampo, $valor);
        $final=Array();
        if(empty($registros)){
            $final=$registros;
        }else{
            foreach ($registros as $resultado) {
                foreach ($entidades as $datos) {
                    $tabla = $datos['tabla'];
                    $campo = $datos['campo'];
                    $dato = $resultado[$campo];
                    error_log($tabla.'-'.$campo.'-'.$dato, 0);
                    if (empty($dato)) {
                        $consulta = null;
                    } else {
                        $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
                        $sql->bindValue(':id', $dato);
                        $sql->execute();
                        $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
                    }
                    $resultado=array_replace($resultado, $consulta);
                }
                array_push($final,$resultado);
            }
        }
        return $final;
    }
    
    function getAll($dbConn, $ntabla){
        error_log("GET ALL FULL tabla[$ntabla]", 0);
        $sql = $dbConn->prepare("SELECT * FROM $ntabla where estado=1");
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $registros = $sql->fetchAll();
        return $registros;
    }

    function getAllFull($dbConn, $ntabla, $entidades){
        error_log("GET ALL FULL tabla[$ntabla]", 0);
        $registros = getAll($dbConn, $ntabla);
        $final=Array();
        if(empty($registros)){
            $final=$registros;
        }else{
            foreach ($registros as $resultado) {
                foreach ($entidades as $datos) {
                    $tabla = $datos['tabla'];
                    $campo = $datos['campo'];
                    $dato = $resultado[$campo];
                    $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
                    $sql->bindValue(':id', $dato);
                    $sql->execute();
                    $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
                    $resultado=array_replace($resultado, $consulta);
                }
                array_push($final,$resultado);
            }
        }
        return $final;
    }
    
    function getAllByCampoFull($dbConn, $ntabla, $campos, $byCampo, $valor, $entidades){
        error_log("tabla[$ntabla] byCampo[$byCampo] Valor[$valor]", 0);
        $sql = $dbConn->prepare("SELECT $campos FROM $ntabla where $byCampo=:valor and estado=1");
        $sql->bindValue(':valor', $valor);
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $registros = $sql->fetchAll();
        $final=Array();
        if(empty($registros)){
            $final=$registros;
        }else{
            foreach ($registros as $resultado) {
                foreach ($entidades as $datos) {
                    $tabla = $datos['tabla'];
                    $campo = $datos['campo'];
                    $dato = $resultado[$campo];
                    if (empty($dato)) {
                        $myArray = array();
                        $myArray['producto']= null;
                        $consulta = $myArray;
                    } else {
                        $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
                        $sql->bindValue(':id', $dato);
                        $sql->execute();
                        $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
                    }
                    $resultado=array_replace($resultado, $consulta);
                }
                array_push($final,$resultado);
            }
        }
        return $final;
    }

    function addEntidades($dbConn, $registros, $entidades){
        error_log("-- Add Entidades --", 0);
        $final=Array();
        if(empty($registros)){
            $final=$registros;
        }else{
            foreach ($registros as $resultado) {
                foreach ($entidades as $datos) {
                    $tabla = $datos['tabla'];
                    $campo = $datos['campo'];
                    $dato = $resultado[$campo];
                    $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
                    $sql->bindValue(':id', $dato);
                    $sql->execute();
                    $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
                    $resultado=array_replace($resultado, $consulta);
                }
                array_push($final,$resultado);
            }
        }
        return $final;
    }

    function addEntidadesSingle($dbConn, $registros, $entidades){
        error_log("-- Add Entidades --", 0);
        $final=Array();
        $final=$registros;
        if(empty($registros)){
            $final=$registros;
        }else{
            error_log("-- Add Entidades 2");
            foreach ($entidades as $datos) {
                error_log("-- Add Entidades 3");
                $tabla = $datos['tabla'];
                $campo = $datos['campo'];
                $dato = $registros[$campo];
                $sql = $dbConn->prepare("SELECT * FROM $tabla where id=:id");
                $sql->bindValue(':id', $dato);
                $sql->execute();
                $consulta = array($campo => $sql->fetch(PDO::FETCH_ASSOC));
                $final=array_replace($final, $consulta);
            }
        }
        return $final;
    }   
    
    function nvl($dato, $texto){
        $cadena = $dato;
        if($dato){
            if(empty($dato)){
                $cadena = $texto;
            }
        }else{
            $cadena = $texto;
        }
        return $cadena;
    }
    
    function getMod11Dv($factura, $tipoComprobante, $idAmbiente, $tipoEmision, $secuencial){
        $retorno_10='K';
        // Armar número
        $fechaClave = date("dmY",strtotime($factura['fecha'])); //Formato del SRI        
        // Obtener el RUC
        $ruc = $factura['facturador']['numdoc'];
        error_log("RUC:".$factura['facturador']['numdoc']);
        $codClave = $factura['facturador']['codClave'];
        error_log("CLAVE:".$factura['facturador']['codClave']);
        error_log(" AMBIENTE:".$idAmbiente);
        error_log(" ESTABLECIMIENTO:".$factura["numEstablecimiento"]);
        error_log(" PTO:".$factura["numPtoEmision"]);
        $clave = $fechaClave.$tipoComprobante.$ruc.$idAmbiente.$factura["numEstablecimiento"].$factura["numPtoEmision"]
        .$secuencial.$codClave.$tipoEmision;
        error_log(">>> GENERADOR CLAVE cadena[$clave]");
        /* ----------------------------------------- */
        $digits = str_replace( array( '.', ',' ), array( ''.'' ), strrev($clave ) );
        if ( ! ctype_digit( $digits ) )
        {
            return false;
        }
        
        $sum = 0;
        $factor = 2;
        
        for( $i=0;$i<strlen( $digits ); $i++ )
        {
            $sum += substr( $digits,$i,1 ) * $factor;
            if ( $factor == 7 )
            {
                $factor = 2;
            }else{
                $factor++;
            }
        }        
        $dv = 11 - ($sum % 11);
        
        if ( $dv < 10 )
        {
            return $clave.$dv;
        }
        if ( $dv == 10 )
        {
            return $clave."1";
        }
        if ( $dv == 11 )
        {
            return $clave."0";
        }
        return $clave.$retorno_10;
    }
    
    function validaLicencia($dbConn, $facturador){
        $mensaje = "VALIDAR LICENCIA";
        $sql = $dbConn->prepare("select if(now() >= vence, 'FIN', 'OK') licencia from fcdr 
            where id = $facturador");
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $resultado=$sql->fetch();
        $licencia = $resultado['licencia'];
        if($licencia != "OK"){
            return "SU LICENCIA HA EXPIRADO";
        }        
        $emitidos = $resultado['emitidos'];
        $sql = $dbConn->prepare("SELECT ifnull(a.docPermitidos,0) permitidos, a.docEmitidos emitidos FROM
            fcdr a WHERE a.id = $facturador");
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $resultado=$sql->fetch();
        $permitidos = $resultado['permitidos'];
        $emitidos = $resultado['emitidos'];
        if($permitidos == 0){
            $mensaje = "OK";
        }else{
            if($emitidos >= $permitidos){
                $mensaje = "HA SUPERADO EL TOTAL DE COMPROBANTES PERMITIDOS";
            }else{
                $mensaje = "OK";
            }
        }
        return $mensaje;
    }
    
    function obtenerSecuencial($dbConn, $idPtoEmision, $tipoComprobante){
        $secuencial = '';
        error_log(">>> OBTENER SECUENCIAL PtoEmision[$idPtoEmision] TipoComprobante[$tipoComprobante]");
        $sql = $dbConn->prepare("SELECT numActual FROM nxpe a WHERE a.ptoEmision=:ptoEmision AND a.tipoDoc=:tipoDoc");
        $sql->bindValue(':ptoEmision', $idPtoEmision);
        $sql->bindValue(':tipoDoc', $tipoComprobante);
        $sql->execute();
        $sql->setFetchMode(PDO::FETCH_ASSOC);
        $resultado=$sql->fetch();
        $numero = $resultado['numActual'];
        $nuevoNumero = $numero + 1;
        //Actualiza numeración
        $sql = "update nxpe
            set numActual = $nuevoNumero
        where 
            ptoEmision=:ptoEmision
        AND tipoDoc=:tipoDoc";
        $statement = $dbConn->prepare($sql);
        $statement->bindValue(':ptoEmision', $idPtoEmision, PDO::PARAM_INT);
        $statement->bindValue(':tipoDoc', $tipoComprobante, PDO::PARAM_STR);        
        $statement->execute();
        $secuencial=str_pad($numero, 9, "0", STR_PAD_LEFT);
        return $secuencial;
    }
    
    function crearDirectorio($directorio){
        if (!is_dir($directorio)) {
            mkdir($directorio, 0755, true);
            $file = 'index.php';
            $newfile = $directorio.'/index.php';
            copy($file,$newfile);
        }
    }
?>