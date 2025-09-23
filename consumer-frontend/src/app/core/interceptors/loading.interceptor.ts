import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
    private activeRequests = 0;

    constructor(private loadingService: LoadingService) { }

    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // Skip loading for certain requests
        if (req.headers.has('X-Skip-Loading')) {
            return next.handle(req);
        }

        // Increment active requests and show loading
        this.activeRequests++;
        this.loadingService.setLoading(true);

        return next.handle(req).pipe(
            finalize(() => {
                // Decrement active requests
                this.activeRequests--;

                // Hide loading only when no active requests
                if (this.activeRequests === 0) {
                    this.loadingService.setLoading(false);
                }
            })
        );
    }
}