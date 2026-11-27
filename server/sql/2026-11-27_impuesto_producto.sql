-- Impuestos fase 2: impuesto de venta y de compra por tipo de producto y por modelo. Base: servisofts.inventario (2.2).
-- Las keys apuntan a contabilidad.impuesto (otra base, sin FK). Nulo = el impuesto por defecto de la empresa.
-- Prioridad en el cálculo: modelo -> tipo de producto -> por defecto. Plan: developers/modulos/impuestos-plan.md §10.
BEGIN;
ALTER TABLE tipo_producto ADD COLUMN key_impuesto_venta varchar, ADD COLUMN key_impuesto_compra varchar;
ALTER TABLE modelo        ADD COLUMN key_impuesto_venta varchar, ADD COLUMN key_impuesto_compra varchar;
COMMIT;
-- Reversa:
-- ALTER TABLE tipo_producto DROP COLUMN key_impuesto_venta, DROP COLUMN key_impuesto_compra;
-- ALTER TABLE modelo DROP COLUMN key_impuesto_venta, DROP COLUMN key_impuesto_compra;
