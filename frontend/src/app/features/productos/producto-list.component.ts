import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ProductoService } from '../../core/services/producto.service';
import { AuthService } from '../../core/services/auth.service';
import { Producto, PageResponse } from '../../core/models/producto.model';

@Component({
  selector: 'app-producto-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './producto-list.component.html',
  styleUrl: './producto-list.component.css'
})
export class ProductoListComponent implements OnInit {
  productos: Producto[] = [];
  pageResponse?: PageResponse<Producto>;
  filterForm: FormGroup;
  isLoading: boolean = false;
  errorMessage: string = '';
  username: string = '';

  constructor(
    private readonly fb: FormBuilder,
    private readonly productoService: ProductoService,
    private readonly authService: AuthService,
    public readonly router: Router
  ) {
    this.filterForm = this.fb.group({
      search: [''],
      categoria: [''],
      size: [10]
    });
    this.username = this.authService.getUsername() || '';
  }

  ngOnInit(): void {
    this.loadProductos();
    
    // Búsqueda en tiempo real con debounce
    this.filterForm.get('search')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => this.loadProductos());

    // Categoría también con debounce
    this.filterForm.get('categoria')?.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => this.loadProductos());

    this.filterForm.get('size')?.valueChanges.subscribe(() => {
      setTimeout(() => this.loadProductos(0), 0);
    });
  }

  loadProductos(page: number = 0): void {
    this.isLoading = true;
    this.errorMessage = '';

    const { search, categoria, size } = this.filterForm.value;
    const pageSize = Number(size) || 10;

    this.productoService.getProductos(search, categoria, page, pageSize).subscribe({
      next: (response) => {
        this.pageResponse = response;
        this.productos = response.content;
        this.isLoading = false;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al cargar productos';
        this.isLoading = false;
      }
    });
  }

  onPageChange(page: number): void {
    this.loadProductos(page);
  }

  onEdit(id: number): void {
    this.router.navigate(['/productos/edit', id]);
  }

  onDelete(id: number, nombre: string): void {
    if (confirm(`¿Estás seguro de eliminar el producto "${nombre}"?`)) {
      this.productoService.deleteProducto(id).subscribe({
        next: () => {
          this.loadProductos(this.pageResponse?.pageNumber || 0);
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Error al eliminar producto';
        }
      });
    }
  }

  onStockMovement(productoId: number): void {
    this.router.navigate(['/stock-movements'], { 
      queryParams: { productoId } 
    });
  }

  syncProducts(): void {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.productoService.syncProducts().subscribe({
      next: (response) => {
        this.loadProductos(0);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Error al sincronizar productos';
        this.isLoading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  get pages(): number[] {
    if (!this.pageResponse) return [];
    return Array.from({ length: this.pageResponse.totalPages }, (_, i) => i);
  }
}
