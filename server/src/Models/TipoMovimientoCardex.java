package Models;

public enum TipoMovimientoCardex {
    ingreso_compra("Ingreso por Compra"),
    traspaso_ingreso("Traspaso de Ingreso"),
    traspaso_egreso("Traspaso de Egreso"),
    egreso_venta("Egreso por Venta"),
    devolucion_venta("Devolución de Venta"),
    devolucion_compra("Devolución de Compra"),
    perdida("Pérdida"),
    baja("Baja");

    private final String descripcion;

    TipoMovimientoCardex(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

}