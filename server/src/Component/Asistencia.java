package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Asistencia {
    public static final String COMPONENT = "asistencia";

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
            String consulta = "select get_asistencias('" + obj.getString("key_empresa") + "') as json";
            String jsonString = SPGConect.ejecutarConsultaString(consulta);
            JSONArray data = new JSONArray(jsonString);
            System.out.println("--------------");
            obj.put("data", data);
            System.out.println(data);
            System.out.println("--------------");
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
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
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