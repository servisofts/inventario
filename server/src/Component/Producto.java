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
            case "getCategoriasProductosDetalle":
                getCategoriasProductosDetalle(obj, session);
                break;
            case "getCategoriasProductosDetallePartner":
                getCategoriasProductosDetallePartner(obj, session);
                break;
            case "getProductosDetalle":
                getProductosDetalle(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "verificar":
                verificar(obj, session);
                break;
            case "buscarIngredientes":
                buscarIngredientes(obj, session);
                break;
            case "elavorar":
                elavorar(obj, session);
                break;
            case "guardar":
                guardar(obj, session);
                break;
            case "getProductosVenta":
                getProductosVenta(obj, session);
                break;
            case "getAllByKeys":
                getAllByKeys(obj, session);
                break;
        }
    }

    public static void getProductosDetalle(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "";
            if (obj.has("key_producto")) {
                consulta = "select get_productos_detalle('" + obj.getString("key_producto") + "') as json";
            }
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            // obj.put("restaurante",
            // getByKey(obj.getString("key_restaurante")).getJSONObject(obj.getString("key_restaurante")));
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

    public static JSONObject getProducto(String key, String key_sucursal) {
        try {
            String consulta = "select get_producto('" + key + "','" + key_sucursal + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void guardar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            JSONObject existe = getByKey(data.getString("key"));
            if (existe == null || existe.isEmpty()) {
                SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            } else {
                SPGConect.editObject(COMPONENT, data);
            }
            // historial.guardarHistorial(data.getString("key"), COMPONENT,
            // obj.getString("key_usuario"));
            if (data.has("sub_productos") && !data.isNull("sub_productos")) {
                JSONArray sub_productos;
                try {
                    sub_productos = data.getJSONArray("sub_productos");
                    SubProducto.guardarSubProductos(sub_productos, obj.getString("key_usuario"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            e.printStackTrace();
        }
    }

    public static void getProductosVenta(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select * ";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getCategoriasProductosDetallePartner(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_categorias_productos_detalle_partner('" + obj.getString("key_empresa")
                    + "') as json";

            JSONArray data = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getCategoriasProductosDetalle(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_categorias_productos_detalle('" + obj.getString("key_empresa") + "') as json";

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
            String consulta = "select get_all('" + COMPONENT + "') as json";
            if (obj.has("key_empresa")) {
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

    public static void getAllByKeys(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONArray data = obj.getJSONArray("data");
            String consulta = "WITH json_data AS (\n" + //
                    "  SELECT jsonb_array_elements('" + data.toString() + "'::jsonb) AS data\n" +
                    ")\n" +
                    "SELECT array_to_json(array_agg(sq.*)) as json \n" +
                    "FROM (\n" +
                    "SELECT p.*, \n" +
                    "COALESCE(P.nombre,'') || ' ' || COALESCE(P.observacion,'') || ' ' || COALESCE(P.descripcion,'') AS NOMBRE_PRODUCTO,\n"
                    +
                    "       (data->>'cantidad')::int AS cantidad_solicitada, \n" +
                    "       (data->>'precio')::numeric AS precio_solicitado,\n" +
                    "       (data->>'cantidad')::int * (data->>'precio')::numeric AS precio_total_solicitado\n" +
                    "FROM json_data,\n" +
                    "     LATERAL jsonb_to_record(data) AS r(key text, cantidad int, precio numeric)\n" +
                    "JOIN producto p ON p.key = r.key\n" +
                    ") sq";

            JSONArray productos = SPGConect.ejecutarConsultaArray(consulta);
            obj.put("data", productos);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void elavorar(JSONObject obj, SSSessionAbstract session) {

    }

    public static void buscarIngredientes(JSONObject obj, SSSessionAbstract session) {

    }

    public static void verificar(JSONObject obj, SSSessionAbstract session) {
        try {

            String marca = obj.getJSONObject("data").getString("marca");

            JSONObject _marca = Marca.getByDescripcion(marca);
            if (_marca == null || _marca.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "no_existe_marca");
                return;
            }

            String modelo = obj.getJSONObject("data").getString("modelo");

            JSONObject _modelo = Modelo.getByDescripcion(modelo);
            if (_modelo == null || _modelo.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "no_existe_modelo");
                return;
            }

            String chasis = obj.getJSONObject("data").getString("chasis");

            JSONObject _productoDatos = Producto.getProductoDato(chasis);
            if (_productoDatos == null || _productoDatos.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "no_existe_chasis");
                return;
            }

            if (JSONObject.getNames(_productoDatos).length > 1) {
                obj.put("estado", "error");
                obj.put("error", "chasis_duplicado");
                return;
            }

            JSONObject _productoDato = _productoDatos.getJSONObject(JSONObject.getNames(_productoDatos)[0]);

            if (_productoDatos == null || _productoDatos.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "no_existe_chasis");
                return;
            }

            /*
             * if(!_productoDato.getString("key_marca").equals(_marca.getString("key"))){
             * obj.put("estado", "error");
             * obj.put("error", "marca_producto_incorrecta");
             * return;
             * }
             */

            /*
             * Si es posible aumentar luego que valide que este bien el modelo
             * if(!_productoDato.getString("key_modelo").equals(_modelo.getString("key"))){
             * obj.put("estado", "error");
             * obj.put("error", "modelo_producto_incorrecto");
             * return;
             * }
             */

            String motor = obj.getJSONObject("data").getString("motor");

            _productoDato = Producto.getProductoDato(motor);
            if (_productoDato == null || _productoDato.isEmpty()) {
                obj.put("estado", "error");
                obj.put("error", "no_existe_motor");
                return;
            }

            if (JSONObject.getNames(_productoDato).length > 1) {
                obj.put("estado", "error");
                obj.put("error", "motor_duplicado");
                return;
            }

            /*
             * Lo mismo aca
             * if(!_productoDato.getString("key_modelo").equals(_modelo.getString("key"))){
             * obj.put("estado", "error");
             * obj.put("error", "modelo_producto_incorrecto");
             * return;
             * }
             */

            JSONObject send = new JSONObject();
            send.put("component", "compra_venta_detalle_producto");
            send.put("type", "getCuotas");
            send.put("key_producto",
                    _productoDato.getJSONObject(JSONObject.getNames(_productoDato)[0]).getString("key_producto"));
            send = SocketCliente.sendSinc("compra_venta", send);
            if (send.has("error")) {
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
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject producto = obj.getJSONObject("data");

            producto.put("key", SUtil.uuid());
            producto.put("estado", 1);
            producto.put("fecha_on", SUtil.now());
            producto.put("key_usuario", obj.getString("key_usuario"));
            if (obj.has("key_almacen")) {
                producto.put("key_almacen", obj.getString("key_almacen"));

            }

            conectInstance.insertArray(COMPONENT, new JSONArray().put(producto));

            JSONObject cardex = InventarioCardex.CrearMovimiento(
                    producto.getString("key"),
                    TipoMovimientoCardex.ingreso_produccion,
                    producto.getDouble("cantidad"),
                    producto.getString("key_almacen"),
                    obj.getString("key_usuario"));
            conectInstance.insertObject("inventario_cardex", cardex);


            conectInstance.commit();

            obj.put("data", producto);
            obj.put("estado", "exito");
            obj.put("sendAll", true);

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

    public static void registroExcel(JSONObject obj, SSSessionAbstract session) {
         SPGConectInstance conectInstance = new SPGConectInstance(SConfig.getJSON("data_base"));
        try {
            JSONArray data = obj.getJSONArray("data");
            String key_modelo = data.getJSONObject(0).getString("key_modelo");
            String key_compra_venta_detalle = data.getJSONObject(0).getString("key_compra_venta_detalle");

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

                if (JSONObject.getNames(inventrarios_dato) != null) {
                    for (int j = 0; j < JSONObject.getNames(inventrarios_dato).length; j++) {
                        inventrarioDato = inventrarios_dato.getJSONObject(JSONObject.getNames(inventrarios_dato)[j]);
                        productoInventarioDato = new JSONObject();
                        productoInventarioDato.put("key", SUtil.uuid());
                        productoInventarioDato.put("key_usuario", obj.getString("key_usuario"));
                        productoInventarioDato.put("estado", 1);
                        if (data.getJSONObject(i).has(inventrarioDato.getString("descripcion"))) {
                            productoInventarioDato.put("descripcion",
                                    data.getJSONObject(i).getString(inventrarioDato.getString("descripcion")));
                        }
                        productoInventarioDato.put("observacion", "");
                        productoInventarioDato.put("fecha_on", SUtil.now());
                        productoInventarioDato.put("key_producto", data.getJSONObject(i).getString("key"));
                        productoInventarioDato.put("key_inventario_dato", inventrarioDato.getString("key"));

                        productoInventarioDatoArr.put(productoInventarioDato);
                    }
                }
            }

            conectInstance.Transacction();
            conectInstance.insertArray(COMPONENT, data);
            conectInstance.insertArray("producto_inventario_dato", productoInventarioDatoArr);
            Boolean inserto = sendCompraVenta(key_compra_venta_detalle, keys_prod_inv_dato,
                    obj.getString("key_usuario"));
            if (inserto)
                conectInstance.commit();
            else
                conectInstance.rollback();
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

    public static boolean sendCompraVenta(String key_compra_venta_detalle, JSONArray keys_productos, String key_usuario)
            throws Exception {
        JSONObject send = new JSONObject();
        send.put("component", "compra_venta_detalle_producto");
        send.put("type", "registroExcel");
        send.put("key_usuario", key_usuario);
        send.put("key_compra_venta_detalle", key_compra_venta_detalle);
        send.put("keys", keys_productos);
        send.put("estado", "cargando");
        send = SocketCliente.sendSinc("compra_venta", send);
        if (send.getString("estado").equals("exito")) {
            return true;
        }

        return false;

    }

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {

            JSONObject data = obj.getJSONObject("data");
            if (data.has("fecha_habilitacion_automatica")) {
                if (data.get("fecha_habilitacion_automatica").equals("null")) {
                    SPGConect.ejecutarUpdate("UPDATE producto SET fecha_habilitacion_automatica = NULL where key = '"
                            + data.getString("key") + "'");
                    data.remove("fecha_habilitacion_automatica");
                }
            }

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
