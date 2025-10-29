import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'productos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/productos/producto-list.component').then(m => m.ProductoListComponent)
  },
  {
    path: 'productos/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/productos/producto-form.component').then(m => m.ProductoFormComponent)
  },
  {
    path: 'productos/edit/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/productos/producto-form.component').then(m => m.ProductoFormComponent)
  },
  {
    path: 'stock-movements',
    canActivate: [authGuard],
    loadComponent: () => import('./features/stock-movements/stock-movement-form.component').then(m => m.StockMovementFormComponent)
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
