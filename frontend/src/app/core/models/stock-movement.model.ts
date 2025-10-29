export enum TipoMovimiento {
  ENTRADA = 'ENTRADA',
  SALIDA = 'SALIDA'
}

export interface MovimientoStockRequest {
  productoId: number;
  tipo: TipoMovimiento;
  cantidad: number;
}

export interface MovimientoStockResponse {
  id: number;
  productoId: number;
  tipo: TipoMovimiento;
  cantidad: number;
  stockAnterior: number;
  stockNuevo: number;
  fecha: string;
}
