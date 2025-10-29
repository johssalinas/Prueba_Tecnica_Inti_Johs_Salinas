export interface Producto {
  id: number;
  nombre: string;
  categoria: string;
  proveedor: string;
  precio: number;
  stock: number;
  fechaRegistro: string;
}

export interface ProductoRequest {
  nombre: string;
  categoria: string;
  proveedor: string;
  precio: number;
  stock: number;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}
