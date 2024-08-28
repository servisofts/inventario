package Contabilidad;

import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONObject;

import Component.Modelo;
import Component.Producto;
import Component.ProductoIngrediente;
import Component.TipoProducto;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;

public class Contabilidad {
    public static JSONObject getAjusteEmpresa(String key_empresa, String key_ajuste) {
        JSONObject send = new JSONObject();
        send.put("component", "ajuste_empresa");
        send.put("type", "getByKeyAjuste");
        send.put("key_empresa", key_empresa);
        send.put("key_ajuste", key_ajuste);
        JSONObject resp = SocketCliente.sendSinc("contabilidad", send);
        return resp.getJSONObject("data");
    }

    public static void traspasoProducto(JSONObject obj) throws Exception{

        JSONObject productoIngrediente = ProductoIngrediente.getByKey(obj.getString("key_producto_ingrediente"));

        if(productoIngrediente == null){
            throw new Exception("No existe key_producto_ingrediente");
        }

        BigDecimal precioCompra;
        try {
            precioCompra =  new BigDecimal(productoIngrediente.get("precio_compra") + "");
        } catch (Exception e) {
            precioCompra = BigDecimal.ZERO;
        }

        if(precioCompra.compareTo(BigDecimal.ZERO) == 0) {
            throw new Exception("El monto no puede ser menor o igual a 0");
        }

        String descripcionComprobante;
        String debeHaberProducto;
        String debeHaberIngrediente;

        if(productoIngrediente.has("tipo") && productoIngrediente.getString("tipo").equals("ingreso")) {
            descripcionComprobante = "Armar Producto";
            debeHaberProducto = "debe";
            debeHaberIngrediente = "haber";
        } else {
            descripcionComprobante = "Desarmar Producto";
            debeHaberProducto = "haber";
            debeHaberIngrediente = "debe";
        }


        JSONObject productoPadre = Producto.getByKey(productoIngrediente.getString("key_producto"));
        JSONObject modeloPadre = Modelo.getByKey(productoPadre.getString("key_modelo"));
        JSONObject tipoProductoPadre = TipoProducto.getByKey(modeloPadre.getString("key_tipo_producto"));

        JSONObject productoHijo = Producto.getByKey(productoIngrediente.getString("key_producto_ingrediente"));
        JSONObject modeloHijo = Modelo.getByKey(productoHijo.getString("key_modelo"));
        JSONObject tipoProductoHijo = TipoProducto.getByKey(modeloHijo.getString("key_tipo_producto"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", tipoProductoPadre.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", SUtil.now());
        comprobante.put("descripcion", descripcionComprobante + ": xxx");
        comprobante.put("observacion", descripcionComprobante);

        JSONArray detalle = new JSONArray();


        JSONObject det = new JSONObject();

        det = new JSONObject();
        det.put("key_cuenta_contable", tipoProductoPadre.getString("key_cuenta_contable"));
        det.put("glosa", productoPadre.getString("descripcion"));
        det.put(debeHaberProducto, precioCompra);
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", tipoProductoHijo.getString("key_cuenta_contable"));
        det.put("glosa", productoHijo.getString("descripcion"));
        det.put(debeHaberIngrediente, precioCompra);
        detalle.put(det);


        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject repuesta = SocketCliente.sendSinc("contabilidad", send, 1000 * 60);

        if(repuesta.has("estado") && repuesta.getString("estado").equals("error")){
            throw new Exception(repuesta.getString("error"));
        }

        productoIngrediente.put("key_asiento", repuesta.getJSONObject("data").getString("key"));

        obj.put("key_asiento", repuesta.getJSONObject("data").getString("key"));

        SPGConect.editObject("producto_ingrediente", productoIngrediente);

    }

    public static void amortizacion(JSONObject obj) throws Exception{
        /*if(obj.getJSONObject("data").getDouble("monto")<=0) throw new Exception("El monto no puede ser menor o igual a 0");

        JSONObject caja_detalle = obj.getJSONObject("data");

        if(caja_detalle.has("key_comprobante") && !caja_detalle.isNull("key_comprobante")){
            obj.put("estado", "error");
            obj.put("error", "El asiento ya fue generado anteriormente con el codigo: "+caja_detalle.getString("codigo_comprobante"));
            return;
        }

        JSONObject caja = Caja.getByKey(caja_detalle.getString("key_caja"));

        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "");
        send.put("key_empresa", obj.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "ingreso");
        comprobante.put("fecha", obj.getJSONObject("data").getString("fecha"));
        comprobante.put("descripcion", "Caja cobro de servicio: "+obj.getJSONObject("data").getString("descripcion"));
        comprobante.put("observacion", obj.getJSONObject("data").getString("descripcion"));

        JSONArray detalle = new JSONArray();


        JSONObject det = new JSONObject();


        JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(obj.getString("key_empresa"), "cuentas_por_pagar");
        det = new JSONObject();
        det.put("codigo", ajusteEmpresa.getString("codigo"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("haber", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        det = new JSONObject();
        det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
        det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
        det.put("debe", obj.getJSONObject("data").getDouble("monto"));
        detalle.put(det);

        // esta parte solo cuando se envia la plata a bancos

        if(!obj.getJSONObject("data").getBoolean("enviar_cierre_caja")){
            // Sale de caja por el haber
            det = new JSONObject();
            det.put("key_cuenta_contable", caja.getString("key_cuenta_contable"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("haber", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);

            // Entra a bancos por el debe
            det = new JSONObject();
            det.put("key_cuenta_contable", obj.getJSONObject("data").getString("key_cuenta_contable_banco"));
            det.put("glosa", obj.getJSONObject("data").getString("descripcion"));
            det.put("debe", obj.getJSONObject("data").getDouble("monto"));
            detalle.put(det);
        }

        
        comprobante.put("detalle", detalle);
        send.put("data", comprobante);
        JSONObject data = SocketCliente.sendSinc("contabilidad", send);

        if(data.has("estado") && data.getString("estado").equals("error")){
            throw new Exception(data.getString("error"));
        }

        JSONObject caja_detalle_ = new JSONObject();
        caja_detalle_.put("key", caja_detalle.getString("key"));
        caja_detalle_.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle_.put("key_comprobante", data.getJSONObject("data").getString("key"));
        
        caja_detalle.put("codigo_comprobante", data.getJSONObject("data").getString("codigo"));
        caja_detalle.put("key_comprobante", data.getJSONObject("data").getString("key"));


        SPGConect.editObject("caja_detalle", caja_detalle_);
        
        */
    }


    
}
