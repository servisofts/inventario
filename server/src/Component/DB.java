package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Models.TipoMovimientoCardex;
import Servisofts.SConfig;
import Servisofts.SPGConect;
import Servisofts.SPGConectInstance;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class DB {
    public static final String COMPONENT = "db";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "exec":
                exec(obj, session);
                break;
        }
    }

    public static void exec(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONArray data = SPGConect.ejecutarConsultaArray("select array_to_json(array_agg(sq1.*)) as json FROM ("
                    + obj.getString("query") + ") as sq1");

            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

}
