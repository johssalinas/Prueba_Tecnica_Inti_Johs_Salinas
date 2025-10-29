import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { StockMovementService } from '../../core/services/stock-movement.service';
import { ProductoService } from '../../core/services/producto.service';
import { TipoMovimiento } from '../../core/models/stock-movement.model';
import { Producto } from '../../core/models/producto.model';

@Component({
  selector: 'app-stock-movement-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './stock-movement-form.component.html',
  styleUrl: './stock-movement-form.component.css'
})
export class StockMovementFormComponent implements OnInit {
  movementForm: FormGroup;
  isLoading: boolean = false;
  errorMessage: string = '';
  successMessage: string = '';
  producto?: Producto;
  tipoMovimiento = TipoMovimiento;

  constructor(
    private readonly fb: FormBuilder,
    private readonly stockMovementService: StockMovementService,
    private readonly productoService: ProductoService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {
    this.movementForm = this.fb.group({
      productoId: ['', Validators.required],
      tipo: ['', Validators.required],
      cantidad: ['', [
        Validators.required,
        Validators.min(1),
        Validators.max(1000000)
      ]]
    });
  }

  ngOnInit(): void {
    const productoId = this.route.snapshot.queryParamMap.get('productoId');
    if (productoId) {
      this.movementForm.patchValue({ productoId: Number(productoId) });
      this.loadProducto(Number(productoId));
    }
  }

  loadProducto(id: number): void {
    this.productoService.getProductoById(id).subscribe({
      next: (producto) => {
        this.producto = producto;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al cargar producto';
      }
    });
  }

  onSubmit(): void {
    if (this.movementForm.invalid) {
      this.movementForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.stockMovementService.registrarMovimiento(this.movementForm.value).subscribe({
      next: (response) => {
        this.successMessage = `Movimiento registrado exitosamente. Stock anterior: ${response.stockAnterior}, Stock nuevo: ${response.stockNuevo}`;
        this.movementForm.reset();
        this.isLoading = false;
        
        // Recargar el producto para actualizar el stock mostrado
        if (this.producto) {
          this.loadProducto(this.producto.id);
        }
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al registrar movimiento';
        this.isLoading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/productos']);
  }

  getErrorMessage(field: string): string {
    const control = this.movementForm.get(field);
    
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Este campo es requerido';
    }

    if (control.hasError('min')) {
      const min = control.getError('min').min;
      return `El valor mínimo es ${min}`;
    }

    if (control.hasError('max')) {
      const max = control.getError('max').max;
      return `El valor máximo es ${max}`;
    }

    return '';
  }
}
