package Component;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.JsonObject;

import Contabilidad.Contabilidad;
import Models.TipoMovimientoCardex;
import Servisofts.SConsole;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.AsientoContableTipo;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class ConteoManualInventario {
    public static final String COMPONENT = "conteo_manual_inventario";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getAll_reporte_conteo_inventario_detallado":
                getAll_reporte_conteo_inventario_detallado(obj, session);
                break;

            case "getByKeyConteo":
                getByKeyConteo(obj, session);
                break;
            // case "getByKey":
            // getByKey(obj, session);
            // break;
            case "registro":
                registro(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "aplicar_cardex":
                aplicar_cardex(obj, session);
                break;
        }
    }

    public static void getAll_reporte_conteo_inventario_detallado(JSONObject obj,
            SSSessionAbstract session) {
        try {
            String consulta = "select _reporte_conteo_inventario_json('"
                    + obj.getString("key_empresa") + "') as json";
            obj.put("data", SPGConect.ejecutarConsultaArray(consulta));
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getByKeyConteo(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select _get_by_key_conteo_inventario('"
                    + obj.getString("key_contador") + "') as json";
            obj.put("data", SPGConect.ejecutarConsultaArray(consulta));
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {

            String consulta = "select get_all('" + COMPONENT + "') as json";
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
            String consulta = "select get_inventario_dato('" + key_modelo + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '"
                    + obj.getString("key_contador") + "') as json";
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
            JSONArray detalle = obj.getJSONArray("data");
            JSONObject dataApi = new JSONObject();
            dataApi.put("key", SUtil.uuid());
            dataApi.put("fecha_on", SUtil.now());
            dataApi.put("estado", 1);
            dataApi.put("key_usuario", obj.getString("key_usuario"));
            dataApi.put("key_almacen", obj.getString("key_almacen"));
            dataApi.put("data", detalle);

            System.out.println("pint dataApi: " + dataApi.toString());

            SPGConect.insertArray(COMPONENT, new JSONArray().put(dataApi));
            obj.put("data", dataApi);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONArray detalle = obj.getJSONArray("data");
            JSONObject dataApi = new JSONObject();
            dataApi.put("fecha_on", SUtil.now());
            dataApi.put("key_usuario", obj.getString("key_usuario"));
            dataApi.put("key_almacen", obj.getString("key_almacen"));
            dataApi.put("key", obj.getString("key"));
            dataApi.put("data", detalle);
            System.out.println("pint dataApi: " + dataApi.toString());
            SPGConect.editObject(COMPONENT, dataApi);
            obj.put("data", dataApi);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void aplicar_cardex(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();
            String key_conteo = obj.getString("key_conteo");

            String consulta = """
                    SELECT array_to_json(array_agg(sq1.*)) as json
                    FROM(
                            SELECT
                                sq1.key_almacen,
                                sq1.key_modelo,
                                sq1.cantidad_real as cantidad_declarada,
                                sq1.stock as cantidad_en_sistema,
                                get_stock_modelo(	sq1.key_modelo,	sq1.key_almacen) as cantidad_en_sistema_actual,
                                sq1.cantidad_baja,
                                CASE WHEN sq1.cantidad_perdida > 0 THEN sq1.cantidad_perdida ELSE 0 END as cantidad_perdida,
                                CASE WHEN sq1.cantidad_ganancia > 0 THEN sq1.cantidad_ganancia ELSE 0 END as cantidad_ganancia
                            FROM(
                                SELECT
                                    detalle.*,
                                    detalle.stock - (detalle.cantidad_real ) as cantidad_perdida,
                                    (detalle.cantidad_real + detalle.cantidad_baja)-detalle.stock as cantidad_ganancia
                                FROM (
                                    SELECT
                                        inv.key_almacen,
                                        detalle->>'key_modelo' as key_modelo ,
                                        coalesce((detalle->>'stock')::float,0)  as stock,
                                        coalesce((detalle->>'cantidad_real')::float,0) as cantidad_real,
                                        coalesce((detalle->>'cantidad_baja')::float,0) as cantidad_baja
                                    FROM conteo_manual_inventario inv
                                    LEFT JOIN LATERAL jsonb_array_elements(inv.data::jsonb) AS detalle ON true
                                    WHERE  inv.key = '%s'
                                    AND inv.estado > 0
                                ) detalle
                            ) sq1
                    ) sq1
                            """
                    .formatted(key_conteo);

            String key_empresa = SPGConect.ejecutarConsultaString("""
                    SELECT almacen.key_empresa
                    FROM conteo_manual_inventario JOIN almacen ON conteo_manual_inventario.key_almacen = almacen.key
                    WHERE conteo_manual_inventario.key = '%s'
                    """.formatted(key_conteo));

            String key_cuenta_contable_inventario_perdida_por_merma = Contabilidad
                    .getAjusteEmpresa(key_empresa, "inventario_perdida_por_merma").optString("key");

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.egreso);
            asiento.descripcion = "Aplicación de conteo manual de inventario";
            asiento.observacion = "Aplicación de conteo manual de inventario";
            asiento.key_empresa = key_empresa;
            asiento.key_usuario = obj.getString("key_usuario");

            JSONArray arr = conectInstance.ejecutarConsultaArray(consulta);
            // JSONArray detalle = obj.getJSONArray("data");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String key_almacen = item.getString("key_almacen");
                String key_modelo = item.getString("key_modelo");
                double cantidad_declarada = item.getDouble("cantidad_declarada");
                double cantidad_en_sistema = item.getDouble("cantidad_en_sistema");
                double cantidad_en_sistema_actual = item.getDouble("cantidad_en_sistema_actual");
                double cantidad_baja = item.getDouble("cantidad_baja");
                double cantidad_perdida = item.getDouble("cantidad_perdida");
                double cantidad_ganancia = item.getDouble("cantidad_ganancia");

                String key_cuenta_contable_inventario = conectInstance.ejecutarConsultaString("""
                        select tipo_producto.key_cuenta_contable
                        from modelo JOIN tipo_producto ON modelo.key_tipo_producto = tipo_producto.key
                        WHERE modelo.key = '%s'
                        """.formatted(key_modelo));

                if (cantidad_baja > 0 || cantidad_perdida > 0) {

                    if (cantidad_baja > 0) {
                        JSONArray arrInsertado = conectInstance.ejecutarConsultaArray(
                                "select retirar_productos_por_modelo_almacen('" + key_modelo + "', '"
                                        + key_almacen + "'," + cantidad_baja + ",'" + "" + "','"
                                        + TipoMovimientoCardex.baja.name()
                                        + "', '" + key_conteo + "', '" + new JSONObject() + "') as json");
                        Double monto_baja = 0.0;

                        for (int j = 0; j < arrInsertado.length(); j++) {
                            JSONObject itemInsertado = arrInsertado.getJSONObject(j);
                            monto_baja += itemInsertado.getDouble("precio_compra")
                                    * (-itemInsertado.getDouble("cantidad"));

                        }

                        asiento.setDetalle(
                                new AsientoContableDetalle(key_cuenta_contable_inventario_perdida_por_merma,
                                        "Cuenta de Pérdida por Baja")
                                        .setDebe(monto_baja));
                        asiento.setDetalle(
                                new AsientoContableDetalle(key_cuenta_contable_inventario, "Cuenta de Inventario")
                                        .setHaber(monto_baja));

                        System.out.println("Registrar baja: " + cantidad_baja + " del modelo: " + key_modelo
                                + " en el almacén: " + key_almacen);
                    }
                    if (cantidad_perdida > 0) {
                        // Contabilidad.getAjusteEmpresa(key_conteo, consulta)
                        JSONArray arrInsertado = conectInstance.ejecutarConsultaArray(
                                "select retirar_productos_por_modelo_almacen('" + key_modelo + "', '"
                                        + key_almacen + "'," + cantidad_perdida + ",'" + "" + "','"
                                        + TipoMovimientoCardex.perdida.name()
                                        + "', '" + key_conteo + "', '" + new JSONObject() + "') as json");

                        Double monto_baja = 0.0;
                        for (int j = 0; j < arrInsertado.length(); j++) {
                            JSONObject itemInsertado = arrInsertado.getJSONObject(j);
                            monto_baja += itemInsertado.getDouble("precio_compra")
                                    * (-itemInsertado.getDouble("cantidad"));

                        }
                        asiento.setDetalle(
                                new AsientoContableDetalle(key_cuenta_contable_inventario_perdida_por_merma,
                                        "Cuenta de Pérdida por merma")
                                        .setDebe(monto_baja));
                        asiento.setDetalle(
                                new AsientoContableDetalle(key_cuenta_contable_inventario, "Cuenta de Inventario")
                                        .setHaber(monto_baja));

                        System.out.println("Registrar pérdida: " + cantidad_perdida + " del modelo: " + key_modelo
                                + " en el almacén: " + key_almacen);
                    }

                }

                if (cantidad_ganancia > 0) {
                    System.out.println("Registrar ganancia: " + cantidad_ganancia + " del modelo: " + key_modelo
                            + " en el almacén: " + key_almacen);
                    SConsole.warning("Aun no se implementa la lógica de ganancia en el cardex");
                }
                // Aquí se puede agregar la lógica para aplicar el cardex
                // Por ejemplo, actualizar el stock del modelo en el almacén
            }

            conectInstance.ejecutarUpdate(
                    "UPDATE conteo_manual_inventario SET fecha_confirmacion = now() WHERE key = '" + key_conteo + "'");
            // obj.put("data", dataApi);

            asiento.enviar();
            obj.put("estado", "exito");
            conectInstance.commit();
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

}
