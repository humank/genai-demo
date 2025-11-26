import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  get<T>(endpoint: string, params?: any): Observable<T> {
    let httpParams = new HttpParams();
    
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined) {
          httpParams = httpParams.set(key, params[key].toString());
        }
      });
    }

    return this.http.get<T>(this.buildUrl(endpoint), { params: httpParams });
  }

  post<T>(endpoint: string, data: any): Observable<T> {
    return this.http.post<T>(this.buildUrl(endpoint), data);
  }

  put<T>(endpoint: string, data: any): Observable<T> {
    return this.http.put<T>(this.buildUrl(endpoint), data);
  }

  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(this.buildUrl(endpoint));
  }

  patch<T>(endpoint: string, data: any): Observable<T> {
    return this.http.patch<T>(this.buildUrl(endpoint), data);
  }

  private buildUrl(endpoint: string): string {
    // Ensure baseUrl doesn't end with slash and endpoint doesn't start with slash
    const base = this.baseUrl.endsWith('/') ? this.baseUrl.slice(0, -1) : this.baseUrl;
    const path = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
    return `${base}/${path}`;
  }
}