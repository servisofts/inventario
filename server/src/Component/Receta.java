package Component;

import org.json.JSONObject;

import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Receta {
    public static final String COMPONENT = "receta";

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
            case "save":
                save(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all_modelo('" + obj.getString("key_modelo") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getAll(String key_modelo) {
        try {
            String consulta = "select get_all_modelo('" + key_modelo + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    public static JSONObject getByKey(String key_modelo, String key_modelo_ingrediente) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', 'key_modelo','" + key_modelo
                    + "','key_modelo_ingrediente','" + key_modelo_ingrediente + "') as json";
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
            SPGConect.insertObject(COMPONENT, data);
            obj.put("data", data);

            obj.put("estado", "exito");
            // obj.put("sendAll", true);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void save(JSONObject obj, SSSessionAbstract session) {
        try {

            JSONObject data = obj.getJSONObject("data");

            String key_modelo = data.getString("key_modelo");
            String key_ingrediente = data.getString("key_ingrediente");

            String consulta = """
                    SELECT to_json(receta.*) as json
                    FROM receta
                    WHERE receta.estado > 0
                    AND receta.key_modelo = '%s'
                    AND receta.key_ingrediente = '%s'
                    limit 1
                    """.formatted(key_modelo, key_ingrediente);
            JSONObject saveObject = SPGConect.ejecutarConsultaObject(consulta);
            if (saveObject == null || saveObject.isNull("key")) {
                System.out.println("el objeto no extise");
                data.put("key", SUtil.uuid());
                data.put("estado", 1);
                data.put("fecha_on", SUtil.now());
                data.put("key_usuario", obj.getString("key_usuario"));
                SPGConect.insertObject(COMPONENT, data);
                obj.put("data", data);
            } else {
                obj.put("data", saveObject);
            }

            obj.put("estado", "exito");
            // obj.put("sendAll", true);
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
