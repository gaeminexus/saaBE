<?php
ini_set('display_errors', 0);
error_reporting(E_ERROR | E_PARSE);

function gn_xml_liquidacion($dbConn, $clave, $ambiente){
    $tipoDoc = "03"; // Segun tabla 3 del SRI para Liquidacion
    // 1 -- Datos Facturador, Comprador y Liquidacion  
    $sql = $dbConn->prepare("select b.*, b.subtotal 'Sub_12', b.total 'TotalFac', c.numdoc 'RUC', c.nombre 'LOCAL', 
        c.razonSocial, c.mail 'MAIL', c.telefono 'FONO', c.direccion 'DIRECCION', c.microEmpresa, c.RIMPE, c.popularRIMPE,
        c.agenteRetencion, c.contribuyenteEspecial, c.contabilidad, d.tipoId, d.numdoc, d.nombre, d.direccion, d.telefono, d.mail
        from lqcs b, fcdr c, cmpr d
        where b.facturador=c.id
        and b.comprador=d.id
        and b.clave=:id");
    $sql->bindValue(':id', $clave);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $primero=$sql->fetch();
    $idLiquidacion=$primero['id'];
    
    // 2 -- Hay que recuperar Establecimientos y Puntos de Emision
    $sql = $dbConn->prepare("select c.direccion 'dirEstb' from lqcs a, ptem b, estb c
    where a.ptoEmision = b.id
    and b.establecimiento = c.id
    and a.id=:id");
    $sql->bindValue(':id', $idLiquidacion);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $establecimiento=$sql->fetch();
    $dirEstb=$establecimiento['dirEstb'];    
    
    // 3 -- Detalle Liquidacion   
    $sql = $dbConn->prepare("select a.descripcion as 'Produc', if(b.id is null, '4', b.tipoIVA) as 'CodPorIVA', 
        a.*, b.* from dtlc a
        LEFT JOIN prdc b
        ON a.producto = b.id
        where a.liquidacion=:liquidacion");
    $sql->bindValue(':liquidacion', $idLiquidacion);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $detLiquidacion=$sql->fetchAll();
    
    // 4 -- Formas de Pago
    $sql = $dbConn->prepare("select a.*, c.detalle from fplc a, lqcs b, tsri c
        where a.liquidacion = b.id
        and c.lSRI = 24
        and c.codigo = a.formaPago        
        and b.id=:id");
    $sql->bindValue(':id', $idLiquidacion);
    $sql->execute();
    $sql->setFetchMode(PDO::FETCH_ASSOC);
    $formasPago=$sql->fetchAll();
    $dbConn = null;
/* ************************************************************** FIN de selects  */ 
    $idFacturador = $primero['facturador'];
    $RUC=$primero['RUC'];
    $idAmbiente = $ambiente;
    $idEmision = "1";
    // ***************** CODIGOS IMPUESTOS
    $codIVA = "2";
    $codICE = "3";
    $codIRBPNR = "5";
    $codPorIVACero = "0"; // Código del 0%
    $codPorIVA = "4"; // Código del 12%
    $codPorICE = "xxx"; // Determinar cómo saco todos los ICE de una liquidacion según todos los items de una liquidacion  
    $codPorIRBPNR = "xxx"; // Determinar cómo saco todos los ICE de una liquidacion según todos los items de una liquidacion
    
    
    $numEstb = $primero['numEstablecimiento'];
    $numPtoEmision = $primero['numPtoEmision'];
    $secuencial = $primero['secuencial'];
    $claveAcceso = $primero['clave'];
    $nombreFcdr = $primero['LOCAL'];
    $razonFcdr = $primero['razonSocial'];
    $direccionFcdr = $primero['DIRECCION'];
    $telefonoFcdr = $primero['FONO'];
    $mailFcdr = $primero['MAIL'];
    $microEmpresaFcdr = $primero['microEmpresa'];
    $rimpeFcdr = $primero['RIMPE'];
    $rimpePopularFcdr = $primero['popularRIMPE'];
    $contribuyenteFcdr = $primero['contribuyenteEspecial'];
    $agenteRetencionFcdr = $primero['agenteRetencion'];
    $contabilidadFcdr = 'NO';
    if($primero['contabilidad']){
        if($primero['contabilidad']==1){
            $contabilidadFcdr = 'SI';
        }
    }
    $tipoIdCmdr = $primero['tipoId']; // XXXXXXXXXXXXXXXXXXXXXXXXXXXXX Revisar solicita datos del Proveedor y no Comprador
    $nombreCmdr=$primero['nombre'];
    $numDocCmdr=$primero['numdoc'];
    $direccionCmdr=$primero['direccion'];
    $mailCmdr=$primero['mail'];
    $remision = "0"; // ---------> PREGUNTAR DONDE OBTENGO ESTE
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];

    
       
    //********************************    
    $subtotal_12=$primero['Sub_12'];
    $subtotal_0=$primero['subcero'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos="0.00";
    $total_descuento=nvl($primero['descuento'], "0.00");
    $v_ice=$primero['vICE'];
    $v_iva_12=$primero['vIVA'];
    $v_IRBPNR=$primero['vIRBPNR'];
    $valor_total=$primero['TotalFac'];
    
    // ***********************************************
    /* SOBRE PONER VALORES */
    /*$fechaEmision='19/09/2022';    
    $RUC='1711495836001';
    $nombreFcdr = 'VASQUEZ TORRES ROMMEL ARTURO';
    $razonFcdr = 'ACOVAS';
    $numEstb = '001';
    $numPtoEmision = '001';
    $secuencial = '000000051';    
    $claveAcceso = getClave('19092022','01',$RUC,'1',$numEstb.$numPtoEmision,$secuencial,'11122233',1);    
    $numLiquidacion=$primero['numero'];
    $autorizacion = "15985789654236584536856321575632548563214577856347";
    $ambiente = "PRUEBAS"; // Ambiente de pruebas = 1
    $idAmbiente = "1";
    $emision = "NORMAL";
    $direccionFcdr = $primero['DIRECCION'];
    $telefonoFcdr = $primero['FONO'];
    $mailFcdr = $primero['MAIL'];
    $contribuyenteFcdr = $primero['contribuyenteEspecial'];
    $contabilidadFcdr = 'NO'; 
    $tipoIdCmdr = $primero['tipoId'];
    $nombreCmdr=$primero['nombre'];
    $numDocCmdr=$primero['numdoc'];
    $direccionCmdr=$primero['direccion'];
    $mailCmdr=$primero['mail'];
    $remision = "001-001-000000001"; // ---------> PREGUNTAR DONDE OBTENGO ESTE
    $iaFonos=$primero['telefono'];
    $iaObaservacion=$primero['observacion'];*/
    
    //********************************
    /*$subtotal_12=$primero['Sub_12'];
    $subtotal_0=$primero['subcero'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos="0.00";
    $total_descuento=nvl($primero['descuento'], "0.00");
    $v_ice="0.00";
    $v_iva_12=$primero['vIVA'];
    $total_devolucion_iva="0.00";
    $irbpnr="0.00";
    $valor_total=$primero['TotalFac'];
    $propina=nvl($primero['propina'],"0.00");*/
    
    // *********************************
    /*$subtotal_12="11.00";
    $subtotal_0=$primero['subcero'];
    $subtotal_no_objeto_iva="0.00";
    $subtotal_exento_iva="0.00";
    $subtotal_sin_impuestos="0.00";
    $total_descuento=nvl($primero['descuento'], "0.00");
    $v_ice="0.00";
    $v_iva_12="1.32";
    $total_devolucion_iva="0.00";
    $irbpnr="0.00";
    $valor_total="12.32";
    $propina=nvl($primero['propina'],"0.00");*/
    
    $xml = new DOMDocument('1.0','UTF-8');
    $xml->formatOutput = true;
    
    $xml_fac = $xml->createElement('liquidacionCompra');
    $cabecera = $xml->createAttribute('id');
    $cabecera->value='comprobante'; 
    $cabecerav = $xml->createAttribute('version');
    $cabecerav->value='1.0.0';    
    
    $xml_inf= $xml->createElement('infoTributaria');
    $xml_amb= $xml->createElement('ambiente',$idAmbiente);
    $xml_tip= $xml->createElement('tipoEmision',$idEmision);
    $xml_raz= $xml->createElement('razonSocial', $razonFcdr);
    $xml_nom= $xml->createElement('nombreComercial', $nombreFcdr);
    $xml_ruc= $xml->createElement('ruc', $RUC);
    
    $xml_cla= $xml->createElement('claveAcceso', $claveAcceso);
    $xml_doc= $xml->createElement('codDoc', $tipoDoc);
    $xml_est= $xml->createElement('estab', $numEstb);
    $xml_emi= $xml->createElement('ptoEmi', $numPtoEmision);
    $xml_sec= $xml->createElement('secuencial', $secuencial);
    $xml_dir= $xml->createElement('dirMatriz', $direccionFcdr);
    if($contribuyenteFcdr){
        if($contribuyenteFcdr != ''){
            $xml_ces= $xml->createElement('contribuyenteEspecial', $contribuyenteFcdr); // Verificar cómo se incluye esta información en el XML
        }
    }
    if($microEmpresaFcdr){
        if($microEmpresaFcdr == 1){
            $xml_cme= $xml->createElement('regimenMicroempresas', "CONTRIBUYENTE RÉGIMEN MICROEMPRESAS");
        }
    }
    if($agenteRetencionFcdr){
        if($agenteRetencionFcdr != ''){
            $xml_agr= $xml->createElement('agenteRetencion', $agenteRetencionFcdr);
        }
    }
    if($rimpeFcdr){
        if($rimpeFcdr == 1){
            $xml_cri= $xml->createElement('contribuyenteRimpe', "CONTRIBUYENTE RÉGIMEN RIMPE");
        }
    }
    if($rimpePopularFcdr){
        if($rimpePopularFcdr == 1){
            $xml_cri= $xml->createElement('contribuyenteRimpe', "CONTRIBUYENTE NEGOCIO POPULAR - RÉGIMEN RIMPE");
        }
    }
    // *********************** DEBERIA CERRAR LA infoTributaria
    $xml_def= $xml->createElement('infoLiquidacionCompra');
    $xml_fec= $xml->createElement('fechaEmision', date("d/m/Y", strtotime( $primero['fecha'])));
    $xml_des= $xml->createElement('dirEstablecimiento', $dirEstb);
    $xml_obl= $xml->createElement('obligadoContabilidad', $contabilidadFcdr);
    $xml_ide= $xml->createElement('tipoIdentificacionProveedor', $tipoIdCmdr);
    //$xml_gui= $xml->createElement('guiaRemision', $remision);
    $xml_rco= $xml->createElement('razonSocialProveedor', $nombreCmdr);
    $xml_idc= $xml->createElement('identificacionProveedor', $numDocCmdr);
    $xml_dco= $xml->createElement('direccionProveedor', $direccionCmdr);
    $xml_tsi= $xml->createElement('totalSinImpuestos', $subtotal_12+$subtotal_0); //Subtotal sin IVA y el de IVA 0
    $xml_tds= $xml->createElement('totalDescuento', $total_descuento);
         
    // *********************** Impuestos

    // ****** IVA
    $v_sw_impuestos = false;
    if($subtotal_0){
        if($subtotal_0>0){
            $xml_imp= $xml->createElement('totalConImpuestos');
            $v_sw_impuestos = true;
            $xml_tim= $xml->createElement('totalImpuesto');
            $xml_tim->appendChild($xml->createElement('codigo', $codIVA));
            $xml_tim->appendChild($xml->createElement('codigoPorcentaje', $codPorIVACero));
            $xml_tim->appendChild($xml->createElement('baseImponible', $subtotal_0));
            $xml_tim->appendChild($xml->createElement('valor', '0.00'));
            $xml_imp->appendChild($xml_tim);
        }
    }    
    if($v_iva_12){
        if($v_iva_12>0){
            if(!$v_sw_impuestos){
                $xml_imp= $xml->createElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $xml_tim= $xml->createElement('totalImpuesto');
            $xml_tim->appendChild($xml->createElement('codigo', $codIVA));
            $xml_tim->appendChild($xml->createElement('codigoPorcentaje', $codPorIVA));
            $xml_tim->appendChild($xml->createElement('baseImponible', $subtotal_12));
            $xml_tim->appendChild($xml->createElement('valor', $v_iva_12));
            $xml_imp->appendChild($xml_tim);
        }
    }
    if($v_ice){
        if($v_ice>0){
            if(!$v_sw_impuestos){
                $xml_imp= $xml->createElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $xml_tim= $xml->createElement('totalImpuesto');
            $xml_tim->appendChild($xml->createElement('codigo', $codICE)); // 00000000000 --> Crear algoritmo para poner todos los ICE de una liquidacion
            $xml_tim->appendChild($xml->createElement('codigoPorcentaje', $codPorICE));
            $xml_tim->appendChild($xml->createElement('baseImponible', $subtotal_12));
            $xml_tim->appendChild($xml->createElement('valor', $v_ice));
            $xml_imp->appendChild($xml_tim);
        }
    }
    if($v_IRBPNR){
        if($v_IRBPNR>0){
            if(!$v_sw_impuestos){
                $xml_imp= $xml->createElement('totalConImpuestos');
            }
            $v_sw_impuestos = true;
            $xml_tim= $xml->createElement('totalImpuesto');
            $xml_tim->appendChild($xml->createElement('codigo', $codIRBPNR));
            $xml_tim->appendChild($xml->createElement('codigoPorcentaje', $codPorIRBPNR)); // 00000000000 --> Consultar este código de porcentaje
            $xml_tim->appendChild($xml->createElement('baseImponible', $subtotal_12));
            $xml_tim->appendChild($xml->createElement('valor', $v_IRBPNR));
            $xml_imp->appendChild($xml_tim);
        }
    }
    
    //********************************* VER SI CREAMOS RUBRO DE MONEDA
    $moneda="DOLAR";
    
    $xml_imt= $xml->createElement('importeTotal', $valor_total);
    $xml_mon= $xml->createElement('moneda', $moneda);
    
    $xml_pgs= $xml->createElement('pagos');
    /*$xml_pag= $xml->createElement('pago');
    $xml_fpa= $xml->createElement('formaPago','01');
    $xml_tot= $xml->createElement('total', '12.32');
    $xml_pla= $xml->createElement('plazo', '30');
    $xml_uti= $xml->createElement('unidadTiempo','dias');*/
   
    /*
     * HABILITAR CUANDO SE GRABEN LAS FORMAS DE PAGO */
    foreach ($formasPago as $forma){
        $xml_pag= $xml->createElement('pago');
        $xml_pag->appendChild($xml->createElement('formaPago', $forma['formaPago']));
        $xml_pag->appendChild($xml->createElement('total', $forma['valor']));
        if($forma['plazo']){
            $xml_pag->appendChild($xml->createElement('plazo', $forma['plazo']));
        }
        if($forma['unidadTiempo']){
            $xml_pag->appendChild($xml->createElement('unidadTiempo', $forma['unidadTiempo']));
        }
        $xml_pgs->appendChild($xml_pag);
    }
    /*
    // QUEMADO --- BORRAR CUANDO SE REGISTRE LA FORMA DE PAGO
    $xml_pag= $xml->createElement('pago');
    $xml_pag->appendChild($xml->createElement('formaPago', '01'));
    $xml_pag->appendChild($xml->createElement('total', $valor_total));
    $xml_pgs->appendChild($xml_pag);*/
    
    $xml_dts= $xml->createElement('detalles');
    //**************************************************** 000000000000000000
    foreach ($detLiquidacion as $detalle){  // ++++++++++++++++ OJO VER SI SE PUEDE VALOR DE PLAZO Y UNIDAD DE TIEMPO NULOS
        $xml_det= $xml->createElement('detalle');
        if($detalle['codigo']){
            $xml_det->appendChild($xml->createElement('codigoPrincipal', $detalle['codigo']));
        }
        if($detalle['codigoAux']){
            $xml_det->appendChild($xml->createElement('codigoAuxiliar', $detalle['codigoAux']));
        }
        $xml_det->appendChild($xml->createElement('descripcion', $detalle['Produc']));
        $xml_det->appendChild($xml->createElement('cantidad', $detalle['cantidad']));
        $xml_det->appendChild($xml->createElement('precioUnitario', $detalle['valor']));
        error_log("xxx -> ".nvl($detalle['descuento'], '0.00'));
        $xml_det->appendChild($xml->createElement('descuento', nvl($detalle['descuento'], '0.00')));
        $xml_det->appendChild($xml->createElement('precioTotalSinImpuesto', $detalle['subTotal']));
        
        
        $xml_ips= $xml->createElement('impuestos');
        if($detalle['porcentajeIVA']>0){
            $xml_ipt= $xml->createElement('impuesto');
            $xml_ipt->appendChild($xml->createElement('codigo', $codIVA));
            $xml_ipt->appendChild($xml->createElement('codigoPorcentaje', $codPorIVA));
            $xml_ipt->appendChild($xml->createElement('tarifa', '15')); // QUEMADO HAY QUE CAMBIAR POR FORMA DE RECONOCER
            $xml_ipt->appendChild($xml->createElement('baseImponible', $detalle['subTotal']));
            $xml_ipt->appendChild($xml->createElement('valor', $detalle['valorIVA']));
            $xml_ips->appendChild($xml_ipt);
        }else{      
            $xml_ipt= $xml->createElement('impuesto');
            $xml_ipt->appendChild($xml->createElement('codigo', $codIVA));
            $xml_ipt->appendChild($xml->createElement('codigoPorcentaje', $codPorIVACero));
            $xml_ipt->appendChild($xml->createElement('tarifa', '0.00')); // QUEMADO HAY QUE CAMBIAR POR FORMA DE RECONOCER
            $xml_ipt->appendChild($xml->createElement('baseImponible', $detalle['subTotal']));
            $xml_ipt->appendChild($xml->createElement('valor', '0.00'));
            $xml_ips->appendChild($xml_ipt);
        }
        // Debo hacer lo mismo para el ICE
        
        $xml_det->appendChild($xml_ips);        
        $xml_dts->appendChild($xml_det);
    }
    
    // ********************************************    
    $xml_ifa= $xml->createElement('infoAdicional');
    $infoAdicional = "Soporte[$telefonoFcdr - $mailFcdr] ".
    "Contacto Cliente[$iaFonos - $mailCmdr] Observacion[$iaObaservacion]";
    $xml_cp1= $xml->createElement('campoAdicional', $infoAdicional);
    $atributo = $xml->createAttribute('nombre');
    $atributo->value='Datos Adicionales';
    
    $xml_inf->appendChild($xml_amb);
    $xml_inf->appendChild($xml_tip);
    $xml_inf->appendChild($xml_raz);
    $xml_inf->appendChild($xml_nom);
    $xml_inf->appendChild($xml_ruc);
    $xml_inf->appendChild($xml_cla);
    $xml_inf->appendChild($xml_doc);
    $xml_inf->appendChild($xml_est);
    $xml_inf->appendChild($xml_emi);
    $xml_inf->appendChild($xml_sec);
    $xml_inf->appendChild($xml_dir);
    if($xml_ces){
        $xml_inf->appendChild($xml_ces);
    }
    if($xml_cme){
        $xml_inf->appendChild($xml_cme);
    }
    if($xml_agr){
        $xml_inf->appendChild($xml_agr);
    }
    if($xml_cri){
        $xml_inf->appendChild($xml_cri);
    }
    $xml_fac->appendChild($xml_inf);
    
    $xml_def->appendChild($xml_fec);
    $xml_def->appendChild($xml_des);
    $xml_def->appendChild($xml_obl);
    $xml_def->appendChild($xml_ide);
    //$xml_def->appendChild($xml_gui);
    $xml_def->appendChild($xml_rco);
    $xml_def->appendChild($xml_idc);
    $xml_def->appendChild($xml_dco);
    $xml_def->appendChild($xml_tsi);
    $xml_def->appendChild($xml_tds);
    if($v_sw_impuestos==true){
      $xml_def->appendChild($xml_imp);
    }
    $xml_fac->appendChild($xml_def);

    $xml_def->appendChild($xml_imt);
    $xml_def->appendChild($xml_mon);
    
    $xml_def->appendChild($xml_pgs);    
    $xml_fac->appendChild($xml_dts);
    
    $xml_fac->appendChild($xml_ifa);
    $xml_ifa->appendChild($xml_cp1);
    $xml_cp1->appendChild($atributo);
    
    $xml_fac->appendChild($cabecera);
    $xml_fac->appendChild($cabecerav);
    $xml->appendChild($xml_fac);
    
    $mensaje="OK";
    
    try{
        $pathXMLPHP="../../resources/".$idFacturador."/lqcs/g/".$clave.".xml";
        $pathXMLANG="resources/".$idFacturador."/lqcs/g/".$clave.".xml";
        $xml->save($pathXMLPHP);
    } catch (Exception $e) {
        $mensaje = $e->getMessage();
    }
    return array($mensaje,$pathXMLANG,$pathXMLPHP);
}
?>