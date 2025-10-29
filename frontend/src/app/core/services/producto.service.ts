import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Producto, ProductoRequest, PageResponse } from '../models/producto.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private readonly apiUrl = `${environment.apiUrl}/api/productos`;

  constructor(private readonly http: HttpClient) {}

  getProductos(
    search: string = '',
    categoria: string = '',
    page: number = 0,
    size: number = 10,
    sortBy: string = 'fechaRegistro',
    sortDir: string = 'desc'
  ): Observable<PageResponse<Producto>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    if (search) {
      params = params.set('search', search);
    }
    if (categoria) {
      params = params.set('categoria', categoria);
    }

    return this.http.get<PageResponse<Producto>>(this.apiUrl, { params });
  }

  getProductoById(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.apiUrl}/${id}`);
  }

  createProducto(producto: ProductoRequest): Observable<Producto> {
    return this.http.post<Producto>(this.apiUrl, producto);
  }

  updateProducto(id: number, producto: ProductoRequest): Observable<Producto> {
    return this.http.put<Producto>(`${this.apiUrl}/${id}`, producto);
  }

  deleteProducto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  syncProducts(): Observable<any> {
    return this.http.post(`${environment.apiUrl}/sync-products`, {});
  }
}
