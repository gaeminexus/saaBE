<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

include "../lib/config.php";
include "../lib/utils.php";
include "../reports/fn_rprt.php";
include "gn_mail.php";

$dbConn =  connect($db);
// METODO POST
if ($_SERVER['REQUEST_METHOD'] == 'POST'){    
    $idFacturador = $_POST['facturador'];
    $ambiente = $_POST['ambiente'];
    $conectaSRI = $_POST['conectaSRI'];
    $clave = $_POST['clave'];
    $codigoFactura = $_POST['idFactura'];
    $subsidio = $_POST['subsidio'];
    $respuesta ='';    
    //Grabar XML
    $strXML='<?xml version="1.0" encoding="UTF-8"?>'.$_POST['xml'];
    $xml = new SimpleXMLElement($strXML);
    $xml->asXml('../../resources/'.$idFacturador.'/docs/f/'.$clave.'.xml');
    // Inserta el path para angular en la tabla de pathFactura
    $pathAngular='resources/'.$idFacturador.'/docs/f/'.$clave.'.xml';
    $sql = "INSERT INTO ptfc (id, factura, path, alterno)
            VALUES (0, $codigoFactura, '$pathAngular', 3)";
    $statement = $dbConn->prepare($sql);
    $statement->execute();
    // FIRMADA estadoFactura=3;
    $sql = "UPDATE fctr
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
            $file_ws1 = fopen('../../resources/'.$idFacturador.'/docs/e/'.$clave.'.txt', "w");
            $contenido = file_get_contents('../../resources/'.$idFacturador.'/docs/f/'.$clave.'.xml');
            $client = new SoapClient($url, [ "trace" => 1 ] );
            $result = $client->validarComprobante(["xml" => $contenido]);
            fwrite($file_ws1, "Respuesta WS1: " . print_r($result,true) . PHP_EOL);
            $respuesta = $result->RespuestaRecepcionComprobante->estado;
            file_put_contents('../../resources/'.$idFacturador.'/docs/e/'.$clave.'.xml', $contenido);
            // Inserta el path para angular en la tabla de pathFactura
            $pathAngular='resources/'.$idFacturador.'/docs/e/'.$clave.'.xml';
            $sql = "INSERT INTO ptfc (id, factura, path, alterno)
                    VALUES (0, $codigoFactura, '$pathAngular', 4)";
            $statement = $dbConn->prepare($sql);
            $statement->execute();
            // ENVIADA estadoFactura=4;
            $sql = "UPDATE fctr
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
                        $file_ws2_a = fopen('../../resources/'.$idFacturador.'/docs/a/'.$clave.'.txt', "w");
                        fwrite($file_ws2_a, "Respuesta WS2: " . print_r($result,true) . PHP_EOL);
                        $autorizadoXML = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->comprobante;
                        $xml = new SimpleXMLElement($autorizadoXML);
                        $xml->asXml('../../resources/'.$idFacturador.'/docs/a/'.$clave.'.xml');                        
                        // Inserta el path para angular en la tabla de pathFactura
                        $pathAngular='resources/'.$idFacturador.'/docs/a/'.$clave.'.xml';
                        $sql = "INSERT INTO ptfc (id, factura, path, alterno)
                                VALUES (0, $codigoFactura, '$pathAngular', 5)";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        // AUTORIZADA estadoFactura=5;
                        $v_autorizacion = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->numeroAutorizacion;
                        $v_fechaAutorizacion = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->fechaAutorizacion;
                        $sql = "UPDATE fctr
                        SET estado = 5, estadoEmision = 1, autorizacion = '$v_autorizacion', fechaAutorizacion = '$v_fechaAutorizacion'
                        WHERE clave='$clave'";
                        $statement = $dbConn->prepare($sql);
                        $statement->execute();
                        fclose($file_ws2_a);
                        $respuesta = $estadoXml;
                        if($subsidio > 0) {
                            crearPDFSubsidio($dbConn, $clave);
                        }else{
                            crearPDF($dbConn, $clave);
                        }
                        if($ambiente==2){
                            //Actualizar contador de Documentos Emitidos                            
                            $sql = "UPDATE fcdr
                            SET docEmitidos = ifnull(docEmitidos,0) + 1
                            WHERE id=$idFacturador";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                            if($_POST['destinatario']!=null){                                
                                $logo = $_POST['pathLogo'];
                                //ENVIAR MAIL
                                $v_resultado = enviar_mail($_POST['destinatario'], $idFacturador, $clave, $logo);
                                $respuesta = $respuesta." ".$v_resultado;
                            }
                        }                        
                    }else{                        
                        // NO AUTORIZADA estadoFactura=6;
                        // file_put_contents('../../resources/'.$idFacturador.'/docs/n/'.$clave.'.xml', $contenido);                        
                        $file_ws2_n = fopen('../../resources/'.$idFacturador.'/docs/n/'.$clave.'.txt', "w");
                        fwrite($file_ws2_n, "Respuesta WS2: " . print_r($result,true) . PHP_EOL);                   
                        $noAutorizadoXML = $result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->comprobante;
                        if($noAutorizadoXML){
                            $xml = new SimpleXMLElement($noAutorizadoXML);
                            $xml->asXml('../../resources/'.$idFacturador.'/docs/n/'.$clave.'.xml');
                            // Inserta el path para angular en la tabla de pathFactura
                            $pathAngular='resources/'.$idFacturador.'/docs/n/'.$clave.'.xml';
                            $sql = "INSERT INTO ptfc (id, factura, path, alterno)
                                VALUES (0, $codigoFactura, '$pathAngular', 6)";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                            // NO AUTORIZADA estado = 6, PENDIENTE estadoEmision = 2;
                            $sql = "UPDATE fctr
                                SET estado = 6, estadoEmision = 2
                                WHERE clave='$clave'";
                            $statement = $dbConn->prepare($sql);
                            $statement->execute();
                            $respuesta = 'Estado: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->estado.
                            ' Id: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->identificador.
                            ' Mensaje: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->mensaje.
                            ' / '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->mensajes->mensaje->informacionAdicional;
                        }else{
                            $respuesta = 'Estado: '.$result->RespuestaAutorizacionComprobante->autorizaciones->autorizacion->estado;
                        }
                        fclose($file_ws2_n);
                    }
                } catch ( SoapFault $e ) {
                    // NO AUTORIZADA estado = 6, PENDIENTE estadoEmision = 2;
                    file_put_contents('../../resources/'.$idFacturador.'/docs/n/'.$clave.'.xml', $contenido);
                    $file_ws2_n = fopen('../../resources/'.$idFacturador.'/docs/n/'.$clave.'.txt', "w");
                    fwrite($file_ws2_n, "Error al llamar SRI_2: " . $e->getMessage() . PHP_EOL);
                    $sql = "UPDATE fctr
                        SET estado = 6, estadoEmision = 2
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
                    $sql = "UPDATE fctr SET autorizacion = '$clave', fechaAutorizacion = addtime(fecha, '00:01:15'),
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
        $respuesta = 'Factura Generada pero no enviada';
    }
    header("HTTP/1.1 200 OK");
    echo $respuesta;
    salir($dbConn);
}
?>