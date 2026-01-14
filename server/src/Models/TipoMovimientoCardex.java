package Models;

public enum TipoMovimientoCardex {
    ingreso_compra("Ingreso por Compra"),
    ingreso_produccion("Ingreso por Producción"),
    traspaso_ingreso("Traspaso de Ingreso"),
    traspaso_egreso("Traspaso de Egreso"),
    egreso_venta("Egreso por Venta"),
    egreso_produccion("Egreso por Producción"),
    devolucion_venta("Devolución de Venta"),
    devolucion_compra("Devolución de Compra"),
    perdida("Pérdida"),
    excedente("Excedente"),
    baja("Baja"),
    anulacion_venta("Anulación de Venta");

    private final String descripcion;

    TipoMovimientoCardex(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

}