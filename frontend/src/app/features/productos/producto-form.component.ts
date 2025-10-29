import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductoService } from '../../core/services/producto.service';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './producto-form.component.html',
  styleUrl: './producto-form.component.css'
})
export class ProductoFormComponent implements OnInit {
  productoForm: FormGroup;
  isEditMode: boolean = false;
  productoId?: number;
  isLoading: boolean = false;
  errorMessage: string = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly productoService: ProductoService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {
    this.productoForm = this.fb.group({
      nombre: ['', [
        Validators.required, 
        Validators.minLength(3),
        Validators.maxLength(100)
      ]],
      categoria: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50)
      ]],
      proveedor: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(100)
      ]],
      precio: ['', [
        Validators.required,
        Validators.min(0.01),
        Validators.max(999999.99)
      ]],
      stock: ['', [
        Validators.required,
        Validators.min(0),
        Validators.max(1000000)
      ]]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.productoId = Number(id);
      this.loadProducto();
    }
  }

  loadProducto(): void {
    if (!this.productoId) return;

    this.isLoading = true;
    this.productoService.getProductoById(this.productoId).subscribe({
      next: (producto) => {
        this.productoForm.patchValue({
          nombre: producto.nombre,
          categoria: producto.categoria,
          proveedor: producto.proveedor,
          precio: producto.precio,
          stock: producto.stock
        });
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al cargar producto';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.productoForm.invalid) {
      this.productoForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const request = this.productoForm.value;

    const operation = this.isEditMode && this.productoId
      ? this.productoService.updateProducto(this.productoId, request)
      : this.productoService.createProducto(request);

    operation.subscribe({
      next: () => {
        this.router.navigate(['/productos']);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al guardar producto';
        this.isLoading = false;
      }
    });
  }

  onCancel(): void {
    this.router.navigate(['/productos']);
  }

  getErrorMessage(field: string): string {
    const control = this.productoForm.get(field);
    
    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Este campo es requerido';
    }

    if (control.hasError('minlength')) {
      const minLength = control.getError('minlength').requiredLength;
      return `Mínimo ${minLength} caracteres`;
    }

    if (control.hasError('maxlength')) {
      const maxLength = control.getError('maxlength').requiredLength;
      return `Máximo ${maxLength} caracteres`;
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
