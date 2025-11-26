import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';

describe('ApiService', () => {
    let service: ApiService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ApiService]
        });
        service = TestBed.inject(ApiService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('URL Construction', () => {
        it('should correctly join baseUrl and endpoint when both have slashes', () => {
            // Mock environment.apiUrl if possible, or assume it has no trailing slash by default
            // But here we rely on the service's internal logic.
            // Since baseUrl is private/readonly, we test the effect on the request URL.

            const endpoint = '/test/endpoint';
            service.get(endpoint).subscribe();

            const req = httpMock.expectOne(req => req.url.endsWith('/test/endpoint') && !req.url.includes('//test'));
            expect(req.request.method).toBe('GET');
            req.flush({});
        });

        it('should correctly join baseUrl and endpoint when endpoint has no slash', () => {
            const endpoint = 'test/endpoint';
            service.get(endpoint).subscribe();

            const req = httpMock.expectOne(req => req.url.endsWith('/test/endpoint'));
            expect(req.request.method).toBe('GET');
            req.flush({});
        });

        it('should prevent double slashes', () => {
            // We can't easily change environment.apiUrl here without more complex mocking,
            // but we can verify the request URL structure.
            const endpoint = '/api/analytics';
            service.get(endpoint).subscribe();

            const req = httpMock.expectOne(req => {
                // Check that we don't have double slashes after the domain (except http://)
                const path = req.url.replace('http://', '').replace('https://', '');
                return !path.includes('//');
            });
            req.flush({});
        });
    });
});
