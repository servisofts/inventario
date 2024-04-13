package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;
import Server.SSSAbstract.SSSessionAbstract;

public class Producto {
    public static final String COMPONENT = "producto";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getByKey":
                getByKey(obj, session);
                break;
            case "registro":
                registro(obj, session);
                break;
            case "registroExcel":
                registroExcel(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "verificar":
                verificar(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "') as json";
            if(obj.has("key_empresa")){
                consulta = "select get_productos('" + obj.get("key_empresa") + "') as json";
            }
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void verificar(JSONObject obj, SSSessionAbstract session) {
        try {

            String marca = obj.getJSONObject("data").getString("marca");

            JSONObject _marca = Marca.getByDescripcion(marca);
            if(_marca==null || _marca.isEmpty()){
                obj.put("estado", "error");
                obj.put("error", "no_existe_marca");
                return;
            }

            String modelo = obj.getJSONObject("data").getString("modelo");

            JSONObject _modelo = Modelo.getByDescripcion(modelo);
            if(_modelo==null || _modelo.isEmpty()){
                obj.put("estado", "error");
                obj.put("error", "no_existe_modelo");
                return;
            }

            String chasis = obj.getJSONObject("data").getString("chasis");

            JSONObject _productoDatos = Producto.getProductoDato(chasis);
            if(_productoDatos==null || _productoDatos.isEmpty()){
                obj.put("estado", "error");
                obj.put("error", "no_existe_chasis");
                return;
            }
            

            if(JSONObject.getNames(_productoDatos).length>1){
                obj.put("estado", "error");
                obj.put("error", "chasis_duplicado");
                return;
            }

            JSONObject _productoDato = _productoDatos.getJSONObject(JSONObject.getNames(_productoDatos)[0]);

            if(_productoDatos==null || _productoDatos.isEmpty()){
                obj.put("estado", "error");
                obj.put("error", "no_existe_chasis");
                return;
            }
            

            /*if(!_productoDato.getString("key_marca").equals(_marca.getString("key"))){
                obj.put("estado", "error");
                obj.put("error", "marca_producto_incorrecta");
                return;
            }*/

            /* Si es posible aumentar luego que valide que este bien el modelo
            if(!_productoDato.getString("key_modelo").equals(_modelo.getString("key"))){
                obj.put("estado", "error");
                obj.put("error", "modelo_producto_incorrecto");
                return;
            }*/

            String motor = obj.getJSONObject("data").getString("motor");

            _productoDato = Producto.getProductoDato(motor);
            if(_productoDato==null || _productoDato.isEmpty()){
                obj.put("estado", "error");
                obj.put("error", "no_existe_motor");
                return;
            }

            if(JSONObject.getNames(_productoDato).length>1){
                obj.put("estado", "error");
                obj.put("error", "motor_duplicado");
                return;
            }


            /*
            Lo mismo aca
            if(!_productoDato.getString("key_modelo").equals(_modelo.getString("key"))){
                obj.put("estado", "error");
                obj.put("error", "modelo_producto_incorrecto");
                return;
            }
            */


            JSONObject send = new JSONObject();
            send.put("component", "compra_venta_detalle_producto");
            send.put("type", "getCuotas");
            send.put("key_producto", _productoDato.getJSONObject(JSONObject.getNames(_productoDato)[0]).getString("key_producto"));
            send =SocketCliente.sendSinc("compra_venta", send);
            if(send.has("error")){
                obj.put("estado", "error");
                obj.put("error", send.getString("error"));
                return;
            } 
            obj.put("data", send.getJSONObject("data"));
            obj.put("estado", "exito");
            
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getByKey(String key) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + key + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            data.put("key_almacen", obj.getString("key_almacen"));

            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));

            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }
    public static void registroExcel(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONArray data = obj.getJSONArray("data");
            String key_modelo=data.getJSONObject(0).getString("key_modelo");
            String key_compra_venta_detalle=data.getJSONObject(0).getString("key_compra_venta_detalle");

            JSONObject inventrarios_dato = InventarioDato.getAll(key_modelo);
            
            JSONObject inventrarioDato, productoInventarioDato;
            JSONArray productoInventarioDatoArr = new JSONArray();
            JSONArray keys_prod_inv_dato = new JSONArray();
            for (int i = 0; i < data.length(); i++) {
                data.getJSONObject(i).put("key", SUtil.uuid());
                keys_prod_inv_dato.put(data.getJSONObject(i).getString("key"));
                data.getJSONObject(i).put("estado", 1);
                data.getJSONObject(i).put("fecha_on", SUtil.now());
                data.getJSONObject(i).put("key_usuario", obj.getString("key_usuario"));

                if(JSONObject.getNames(inventrarios_dato)!=null){
                    for (int j = 0; j < JSONObject.getNames(inventrarios_dato).length; j++) {
                        inventrarioDato = inventrarios_dato.getJSONObject(JSONObject.getNames(inventrarios_dato)[j]);
                        productoInventarioDato = new JSONObject();
                        productoInventarioDato.put("key", SUtil.uuid());
                        productoInventarioDato.put("key_usuario", obj.getString("key_usuario"));
                        productoInventarioDato.put("estado", 1);
                        if( data.getJSONObject(i).has(inventrarioDato.getString("descripcion"))){
                            productoInventarioDato.put("descripcion", data.getJSONObject(i).getString(inventrarioDato.getString("descripcion")));
                        }
                        productoInventarioDato.put("observacion", "");
                        productoInventarioDato.put("fecha_on", SUtil.now());
                        productoInventarioDato.put("key_producto", data.getJSONObject(i).getString("key"));
                        productoInventarioDato.put("key_inventario_dato", inventrarioDato.getString("key"));
    
                        productoInventarioDatoArr.put(productoInventarioDato);
                    }
                }
                
            }

            SPGConect.Transacction();
            SPGConect.insertArray(COMPONENT, data);
            SPGConect.insertArray("producto_inventario_dato", productoInventarioDatoArr);
            Boolean inserto = sendCompraVenta(key_compra_venta_detalle, keys_prod_inv_dato, obj.getString("key_usuario"));
            if(inserto) SPGConect.commit();
            else SPGConect.rollback();
            SPGConect.Transacction_end();
            
            

            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);
        } catch (Exception e) {
            SPGConect.rollback();
            SPGConect.Transacction_end();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean sendCompraVenta(String key_compra_venta_detalle, JSONArray keys_productos, String key_usuario) throws Exception{
        JSONObject send = new JSONObject();
        send.put("component", "compra_venta_detalle_producto");
        send.put("type", "registroExcel");
        send.put("key_usuario", key_usuario);
        send.put("key_compra_venta_detalle", key_compra_venta_detalle);
        send.put("keys", keys_productos);
        send.put("estado", "cargando");
        send = SocketCliente.sendSinc("compra_venta", send);
        if(send.getString("estado").equals("exito")){
            return true;
        }
        
        return false;

    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {

            JSONObject data = obj.getJSONObject("data");

            JSONObject producto_historico = Producto.getByKey(data.getString("key"));
            producto_historico.put("key", SUtil.uuid());
            producto_historico.put("key_usuario", obj.getString("key_usuario"));
            producto_historico.put("key_producto", data.getString("key"));
            SPGConect.insertArray("producto_historico", new JSONArray().put(producto_historico));
            
            data.put("fecha_on", SUtil.now());
            SPGConect.editObject(COMPONENT, data);



            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getProductoDato(String dato) {
        try {
            String consulta = "select get_producto_dato('" + dato + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
