package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Proveedor {
    public static final String COMPONENT = "proveedor";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        String type = obj.getString("type");
        switch (type) {
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



    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            // String _aux_key_proveedor = "abcdefghi";
            // String _aux_key_cuenta_contable = "1.0.1";
            // String _aux_key_empresa = "c9caa964-88f3-43db-88df-684ecf5c0a1b";
            // if (obj.has("key_empresa")) { consulta = "select get_all('" + COMPONENT + "',
            // 'key_empresa', '" + _aux_key_empresa + "') as json"; }
            String consulta = "";
            if (obj.has("key_empresa")) {
                consulta = "select get_all('" + COMPONENT + "', 'key_empresa', '"
                        + obj.getString("key_empresa") + "') as json";
            }
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");

            // printLogger(COMPONENT, obj.getString("type"), obj, data);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }



    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("key")
                    + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
            // printLogger(COMPONENT, obj.getString("type"), obj, data);

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }


    // o podria buscar por nit
    public static void getByTelefono(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("telefono")
                    + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }




    public static void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));

            //data.put("key_empresa", obj.getString("key_empresa"));
           // data.put("key_cuenta_contable", "cont.0.0.1");

            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            obj.put("data", data);
            obj.put("estado", "exito");
            // printLogger(COMPONENT, obj.getString("type"), obj, data);

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
            // printLogger(COMPONENT, obj.getString("type"), obj, data);

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
