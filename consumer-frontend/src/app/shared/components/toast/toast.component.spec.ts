import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ToastService } from '../../services/toast.service';
import { ToastComponent } from './toast.component';

describe('ToastComponent', () => {
    let component: ToastComponent;
    let fixture: ComponentFixture<ToastComponent>;
    let toastService: jasmine.SpyObj<ToastService>;

    beforeEach(async () => {
        const spy = jasmine.createSpyObj('ToastService', ['dismiss'], {
            toasts$: of([])
        });

        await TestBed.configureTestingModule({
            imports: [ToastComponent],
            providers: [
                { provide: ToastService, useValue: spy }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(ToastComponent);
        component = fixture.componentInstance;
        toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should get correct toast icon', () => {
        expect(component.getToastIcon('success')).toBe('pi pi-check-circle');
        expect(component.getToastIcon('error')).toBe('pi pi-times-circle');
        expect(component.getToastIcon('warning')).toBe('pi pi-exclamation-triangle');
        expect(component.getToastIcon('info')).toBe('pi pi-info-circle');
    });

    it('should dismiss toast when dismiss is called', () => {
        const toastId = 'test-toast-id';
        component.dismiss(toastId);
        expect(toastService.dismiss).toHaveBeenCalledWith(toastId);
    });
});