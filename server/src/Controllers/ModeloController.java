package Controllers;

import Servisofts.http.annotation.*;
import org.json.JSONObject;
import Component.Modelo;
import Servisofts.http.Exception.HttpException;

@RestController
@RequestMapping("/modelo")
public class ModeloController {

    @GetMapping("/status")
    public String status() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("controller", "alvaro");
            obj.put("metodo", "GET");
            obj.put("estado", "exito ✅");
            obj.put("mensaje", "Servidor activo");
            return obj.toString();
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("estado", "error ❌");
            error.put("mensaje", e.getMessage());
            return error.toString();
        }
    }

    @PostMapping("/status")
    public String statusPost() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("controller", "alvaro");
            obj.put("metodo", "POST");
            obj.put("estado", "exito ✅");
            obj.put("mensaje", "Servidor recibe POST correctamente");
            return obj.toString();
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("estado", "error ❌");
            error.put("mensaje", e.getMessage());
            return error.toString();
        }
    }

    @PostMapping("/registrar")
    public String registrar(@RequestBody String body) throws HttpException {
        try {
            JSONObject data = new JSONObject(body);
            JSONObject obj = new JSONObject();
            obj.put("data", data);
            obj.put("key_empresa", "1234564787987213");
            obj.put("key_usuario", "noseestaenviandokey");
            Modelo.registro2(obj);
            obj.put("status", "Exito ✅");
            return obj.toString();
        } catch (Exception e) {

            JSONObject error = new JSONObject();
            error.put("estado", "error");
            error.put("mensaje", e.getMessage());
            return error.toString();
        }
    }

    @PostMapping("/editar")
    public String editar(@RequestBody String body) {

        try {
            JSONObject data = new JSONObject(body);
            JSONObject obj = new JSONObject();
            obj.put("data", data);
            obj.put("key_empresa", "1234564787987213");
            obj.put("key_usuario", "noseestaenviandokey");
            Modelo.editar2(obj);
            if ("error".equals(obj.optString("estado"))) {
                return obj.toString();
            }
            obj.put("status", "Editado Exitosamente ✅");
            return obj.toString();

        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("estado", "error");
            error.put("mensaje", e.getMessage());
            return error.toString();
        }
    }

    @PostMapping("/eliminar")
    public String eliminar(@RequestBody String body) {

        try {
            JSONObject data = new JSONObject(body);
            JSONObject obj = new JSONObject();
            obj.put("data", data);
            obj.put("key_usuario", "noseestaenviandokey");
            Modelo.editar2(obj);
            if ("error".equals(obj.optString("estado"))) {
                return obj.toString();
            }
            obj.put("status", "Eliminado Exitosamente ✅");
            return obj.toString();
        } catch (Exception e) {
            JSONObject error = new JSONObject();
            error.put("estado", "error");
            error.put("mensaje", e.getMessage());
            return error.toString();
        }
    }

}

// curl -X POST http://localhost:30039/rest/alvaro/ping -H "Content-Type:
// application/json" -d '{"component":"mundo"}'
