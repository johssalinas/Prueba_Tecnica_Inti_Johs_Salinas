import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MovimientoStockRequest, MovimientoStockResponse } from '../models/stock-movement.model';

@Injectable({
  providedIn: 'root'
})
export class StockMovementService {
  private readonly apiUrl = `${environment.apiUrl}/stock-movements`;

  constructor(private readonly http: HttpClient) {}

  registrarMovimiento(request: MovimientoStockRequest): Observable<MovimientoStockResponse> {
    return this.http.post<MovimientoStockResponse>(this.apiUrl, request);
  }
}
