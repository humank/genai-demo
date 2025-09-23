import { Routes } from '@angular/router';

export const adminRoutes: Routes = [
    {
        path: '',
        loadComponent: () => import('./admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
        children: [
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full'
            },
            {
                path: 'dashboard',
                loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent)
            },
            {
                path: 'system-health',
                loadComponent: () => import('./system-health/system-health.component').then(m => m.SystemHealthComponent)
            }
        ]
    }
];