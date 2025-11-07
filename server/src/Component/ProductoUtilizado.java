package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Servisofts.SConfig;
import Servisofts.SPGConect;
import Servisofts.SPGConectInstance;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class ProductoUtilizado {
    public static final String COMPONENT = "producto_utilizado";

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
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_producto', '"+obj.getString("key_producto")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
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
         SPGConectInstance conectInstance = new SPGConectInstance(SConfig.getJSON("data_base"));
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

            conectInstance.Transacction();
            conectInstance.insertArray(COMPONENT, data);
            conectInstance.insertArray("producto_inventario_dato", productoInventarioDatoArr);
            Boolean inserto = sendCompraVenta(key_compra_venta_detalle, keys_prod_inv_dato, obj.getString("key_usuario"));
            if(inserto) conectInstance.commit();
            else conectInstance.rollback();
            conectInstance.Transacction_end();
            
            

            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);
        } catch (Exception e) {
            conectInstance.rollback();
            conectInstance.Transacction_end();
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
