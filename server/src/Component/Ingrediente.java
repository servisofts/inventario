package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class Ingrediente {
    public static final String COMPONENT = "ingrediente";

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
            case "getPizarra":
                getPizarra(obj, session);
                break;
        }
    }

    public static void getPizarra(JSONObject obj, SSSessionAbstract session) {
        try {
            String key_empresa = obj.getString("key_empresa");
            String consulta = """
                    select array_to_json(array_agg(sq1.*)) as json
                    FROM (
                        select 
                            ingrediente.*,
                            (	
                                SELECT array_to_json(array_agg(modelo_ingrediente.*)) 
                                FROM modelo_ingrediente
                                WHERE modelo_ingrediente.key_ingrediente  = ingrediente.key
                                AND modelo_ingrediente.estado > 0
                            ) as modelo_ingrediente,
                            (	
                                SELECT array_to_json(array_agg(receta.*)) 
                                FROM receta
                                WHERE receta.key_ingrediente  = ingrediente.key
                                    AND receta.estado > 0
                            ) as receta
                        from ingrediente 
                        where  ingrediente.estado  > 0
                            AND ingrediente.key_empresa = '%s'
                            group by ingrediente.key
                    ) sq1
                    """.formatted(key_empresa);

            JSONArray data = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
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
            data.put("key_empresa", obj.getString("key_empresa"));
            SPGConect.insertObject(COMPONENT, data);
            obj.put("data", data);

            obj.put("estado", "exito");
            obj.put("sendAll", true);
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
