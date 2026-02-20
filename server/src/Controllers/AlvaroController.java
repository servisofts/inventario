package Controllers;

import Servisofts.http.annotation.*;
import org.json.JSONObject;
import Component.Modelo;
import Servisofts.http.Exception.HttpException;

@RestController
@RequestMapping("/alvaro")
public class AlvaroController {

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

    // @PutMapping("/status")
    // public String statusPut(@RequestBody(required = false) String body) {
    //     try {
    //         JSONObject obj = new JSONObject();

    //         // Si envías algo en el body, podemos mostrarlo
    //         if (body != null && !body.isEmpty()) {
    //             JSONObject data = new JSONObject(body);
    //             obj.put("datos_actualizados", data);
    //         }

    //         obj.put("controller", "alvaro");
    //         obj.put("metodo", "PUT");
    //         obj.put("estado", "exito ✅");
    //         obj.put("mensaje", "Estado actualizado correctamente");

    //         return obj.toString();

    //     } catch (Exception e) {
    //         JSONObject error = new JSONObject();
    //         error.put("estado", "error ❌");
    //         error.put("mensaje", e.getMessage());
    //         return error.toString();
    //     }
    // }

    @PostMapping("/registrar_modelo")
    public String registrar_modelo(@RequestBody String body) throws HttpException {
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

    @PostMapping("/editar_modelo")
    public String editar_modelo(@RequestBody String body) {

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

    @PostMapping("/eliminar_modelo")
    public String eliminar_modelo(@RequestBody String body) {

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
