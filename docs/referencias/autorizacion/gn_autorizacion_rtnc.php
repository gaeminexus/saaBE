<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

include "../lib/config.php";
include "../lib/utils.php";
include "../reports/fn_rprt_rtnc.php";
include "gn_mail_rtnc.php";

$dbConn =  connect($db);
// METODO POST
if ($_SERVER['REQUEST_METHOD'] == 'POST'){    
    $idFacturador = $_POST['facturador'];
    $ambiente = $_POST['ambiente'];
    $conectaSRI = $_POST['conectaSRI'];
    $clave = $_POST['clave'];
    $codigoRetencion = $_POST['idRetencion'];
    $respuesta ='';    
    //Grabar XML
    $strXML='<?xml version="1.0" encoding="UTF-8"?>'.$_POST['xml'];
    $xml = new SimpleXMLElement($strXML);
    $xml->asXml('../../resources/'.$idFacturador.'/rtnc/f/'.$clave.'.xml');
    // Inserta el path para angular en la tabla de pathRetencion
    $pathAngular='resources/'.$idFacturador.'/rtnc/f/'.$clave.'.xml';
    $sql = "INSERT INTO ptrt (id, retencion, path, alterno)
            VALUES (0, $codigoRetencion, '$pathAngular', 3)";
    error_log("Auto->".$sql);
    $statement = $dbConn->prepare($sql);
    $statement->execute();
    // FIRMADA estadoRetencion=3;
    $sql = "UPDATE rtnc
        SET estado = 3
        WHERE clave='$clave'";
    $statement = $dbConn->prepare($sql);
    $statement->execute();
    if($conectaSRI == 1){
        //Llamar SRI 1
        if($ambiente == 1){
            $url = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
        }
        if($ambiente == 2){
            $url = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
        }    
        try {
            $file_ws1 = fopen('../../resources/'.$idFacturador.'/rtnc/e/'.$clave.'.txt', "w");
            $contenido = file_get_contents('../../resources/'.$idFacturador.'/rtnc/f/'.$clave.'.xml');
            $client = new SoapClient($url, [ "trace" => 1 ] );
            $result = $client->validarComprobante(["xml" => $contenido]);
            fwrite($file_ws1, "Respuesta WS1: " . print_r($result,true) . PHP_EOL);
            $respuesta = $result->RespuestaRecepcionComprobante->estado;
            file_put_contents('../../resources/'.$idFacturador.'/rtnc/e/'.$clave.'.xml', $contenido);
            // Inserta el path para angular en la tabla de pathRetencion
            $pathAngular='resources/'.$idFacturador.'/rtnc/e/'.$clave.'.xml';
            $sql = "INSERT INTO ptrt (id, retencion, path, alterno)
                    VALUES (0, $codigoRetencion, '$pathAngular', 4)";
            $statement = $dbConn->prepare($sql);
            $statement->execute();
            // ENVIADA estadoRetencion=4;
            $sql = "UPDATE rtnc
            SET estado = 4
            WHERE clave='$clave'";
            $statement = $dbConn->prepare($sql);
            $statement->execute();
            if($result->RespuestaRecepcionComprobante->estado == 'RECIBIDA'){
                //Llamar SRI 2
                sleep(2);            
                if($ambiente == 1){
                    $url = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
                }
                if($ambiente == 2){
                    $url = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
                }
                try {
                    $client = new SoapClient($url, [ "trace" => 1 ] );
                    $result = $client->autorizacionComprobante( [ "claveAccesoComprobante" => $clave] );
                    $estadoXml = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->estado;
                    if($estadoXml == 'AUTORIZADO'){                        
                        $file_ws2_a = fopen('../../resources/'.$idFacturador.'/rtnc/a/'.$clave.'.txt', "w");
                        fwrite($file_ws2_a, "Respuesta WS2: " . print_r($result,true) . PHP_EOL);
                        $autorizadoXML = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->comprobante;
                        $xml = new SimpleXMLElement($autorizadoXML);
                        $xml->asXml('../../resources/'.$idFacturador.'/rtnc/a/'.$clave.'.xml');                        
                        // Inserta el path para angular en la tabla de pathRetencion
                        $pathAngular='resources/'.$idFacturador.'/rtnc/a/'.$clave.'.xml';
                        $sql = "INSERT INTO ptrt (id, retencion, path, alterno)
                                VALUES (0, $codigoRetencion, '$pathAngular', 5)";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        // AUTORIZADA estadoRetencion=5;
                        $v_autorizacion = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->numeroAutorizacion;
                        $v_fechaAutorizacion = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->fechaAutorizacion;
                        $sql = "UPDATE rtnc
                        SET estado = 5, estadoEmision = 1, autorizacion = '$v_autorizacion', fechaAutorizacion = '$v_fechaAutorizacion'
                        WHERE clave='$clave'";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        //Actualizar contador de Documentos Emitidos
                        $sql = "UPDATE fcdr
                        SET docEmitidos = docEmitidos + 1
                        WHERE id=$idFacturador";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        fclose($file_ws2_a);
                        $respuesta = $estadoXml;
                        crearPDF($dbConn, $clave, 1);                        
                        if($ambiente==2){
                            if($_POST['destinatario']!=null){                                
                                $logo = "https://valeecuador.com/fel/".$_POST['pathLogo'];
                                //ENVIAR MAIL
                                $v_resultado = enviar_mail_rtnc($_POST['destinatario'], $idFacturador, $clave, $logo);
                                $respuesta = $respuesta." ".$v_resultado;
                            }
                        }                        
                    }else{
                        // NO AUTORIZADA estadoRetencion=6;
                        // file_put_contents('../../resources/'.$idFacturador.'/rtnc/n/'.$clave.'.xml', $contenido);
                        $noAutorizadoXML = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->comprobante;
                        $xml = new SimpleXMLElement($noAutorizadoXML);
                        $xml->asXml('../../resources/'.$idFacturador.'/rtnc/n/'.$clave.'.xml');
                        $file_ws2_n = fopen('../../resources/'.$idFacturador.'/rtnc/n/'.$clave.'.txt', "w");
                        fwrite($file_ws2_n, "Respuesta WS2: " . print_r($result,true) . PHP_EOL);
                        // Inserta el path para angular en la tabla de pathRetencion
                        $pathAngular='resources/'.$idFacturador.'/rtnc/n/'.$clave.'.xml';
                        $sql = "INSERT INTO ptrt (id, retencion, path, alterno)
                                VALUES (0, $codigoRetencion, '$pathAngular', 6)";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        // NO AUTORIZADA estadoRetencion=6;
                        $sql = "UPDATE rtnc
                        SET estado = 6, estadoEmision = 3
                        WHERE clave='$clave'";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        fclose($file_ws2_n);
                        $respuesta = 'Estado: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->estado.
                        ' Id: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->identificador.
                        ' Mensaje: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->mensaje.
                        ' / '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->informacionAdicional;
                    }
                } catch ( SoapFault $e ) {
                    // NO AUTORIZADA estadoRetencion=6;
                    file_put_contents('../../resources/'.$idFacturador.'/rtnc/n/'.$clave.'.xml', $contenido);
                    $file_ws2_n = fopen('../../resources/'.$idFacturador.'/rtnc/n/'.$clave.'.txt', "w");
                    fwrite($file_ws2_n, "Error al llamar SRI_2: " . $e->getMessage() . PHP_EOL);
                    $sql = "UPDATE rtnc
                        SET estado = 6, estadoEmision = 3
                        WHERE clave='$clave'";
                    $statement = $dbConn->prepare($sql);
                    $statement->execute();
                    fclose($file_ws2_n);
                    $respuesta = 'Error al llamar SRI_2: '.$e->getMessage();
                }
            }else{
                // **************** Si Clave Registrada
                $mensaje = $result->RespuestaRecepcionComprobante->comprobantes->comprobante->mensajes->mensaje->mensaje;
                $respuesta = 'Estado devoluccccccion: '.$result->RespuestaRecepcionComprobante->estado.
                ' Id: '.$result->RespuestaRecepcionComprobante->comprobantes->comprobante->mensajes->mensaje->identificador.
                ' Mensaje: '.$mensaje.
                ' / '.$result->RespuestaRecepcionComprobante->comprobantes->comprobante->mensajes->mensaje->informacionAdicional;
                if(strstr($mensaje, "CLAVE ACCESO REGISTRADA") !== false){
                    $respuesta = 'Comprobante Autorizado';
                    $sql = "UPDATE rtnc SET autorizacion = '$clave', fechaAutorizacion = addtime(fecha, '00:01:15'),
                    estado=5 WHERE clave='$clave'";
                    $statement = $dbConn->prepare($sql);
                    $statement->execute();
                }
                fwrite($file_ws1, "Error WS1: " . print_r($result,true) . PHP_EOL);
            }
        } catch ( SoapFault $e ) {
            $respuesta = 'Error al llamar SRI_1: '.$e->getMessage();
        }
        fclose($file_ws1);
    } else {
        $respuesta = 'Retencion Generada pero no enviada';
    }
    header("HTTP/1.1 200 OK");
    echo $respuesta;
    salir($dbConn);
}
?>