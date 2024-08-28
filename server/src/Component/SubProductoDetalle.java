package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

public class SubProductoDetalle {
    public static final String COMPONENT = "sub_producto_detalle";

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
            case "editar":
                editar(obj, session);
                break;
        }
    }

    public static void guardarSubProductoDetalles(JSONArray sub_producto_detalles, String key_usuario) {
        try {
            JSONObject sub_producto_detalle;
            for (int i = 0; i < sub_producto_detalles.length(); i++) {
                sub_producto_detalle = sub_producto_detalles.getJSONObject(i);
                guardar(sub_producto_detalle, key_usuario);
            }
        } catch (Exception e) {
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
    public static void guardar(JSONObject data, String key_usuario) {
        try {
            JSONObject existe = getByKey(data.getString("key"));
            if (existe == null || existe.isEmpty()) {
                SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            } else {
                SPGConect.editObject(COMPONENT, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_Servicio', '"+obj.getJSONObject("servicio").getString("key")+"') as json";
            if(obj.has("key_empresa")){
                consulta = "select get_all('" + COMPONENT + "', 'key_empresa', '" + obj.get("key_empresa") + "') as json";
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

    public static JSONObject getByDescripcion(String descripcion) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', 'descripcion', '" + descripcion + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            return null;

        }
    }

    public static void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("key_servicio", obj.getJSONObject("servicio").getString("key"));
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
