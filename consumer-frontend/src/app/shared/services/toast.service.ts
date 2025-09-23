import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    title: string;
    message?: string;
    duration?: number;
    dismissible?: boolean;
    action?: {
        label: string;
        handler: () => void;
    };
    icon?: string;
    position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center';
    animation?: 'slide' | 'fade' | 'bounce' | 'zoom' | 'shake';
}

export interface FeedbackAnimation {
    element: HTMLElement;
    type: 'success' | 'error' | 'warning' | 'info' | 'loading';
    animation: 'pulse' | 'shake' | 'bounce' | 'glow' | 'ripple';
    duration?: number;
}

@Injectable({
    providedIn: 'root'
})
export class ToastService {
    private toastsSubject = new BehaviorSubject<Toast[]>([]);
    public toasts$ = this.toastsSubject.asObservable();

    private generateId(): string {
        return Math.random().toString(36).substr(2, 9);
    }

    show(toast: Omit<Toast, 'id'>): string {
        const newToast: Toast = {
            id: this.generateId(),
            duration: 5000,
            dismissible: true,
            position: 'top-right',
            animation: 'slide',
            ...toast
        };

        const currentToasts = this.toastsSubject.value;
        this.toastsSubject.next([...currentToasts, newToast]);

        if (newToast.duration && newToast.duration > 0) {
            setTimeout(() => {
                this.dismiss(newToast.id);
            }, newToast.duration);
        }

        return newToast.id;
    }

    success(title: string, message?: string, options?: Partial<Toast>): string {
        return this.show({
            type: 'success',
            title,
            message,
            icon: 'pi pi-check-circle',
            animation: 'bounce',
            ...options
        });
    }

    error(title: string, message?: string, options?: Partial<Toast>): string {
        return this.show({
            type: 'error',
            title,
            message,
            duration: 7000, // Errors stay longer
            icon: 'pi pi-times-circle',
            animation: 'shake',
            ...options
        });
    }

    warning(title: string, message?: string, options?: Partial<Toast>): string {
        return this.show({
            type: 'warning',
            title,
            message,
            icon: 'pi pi-exclamation-triangle',
            ...options
        });
    }

    info(title: string, message?: string, options?: Partial<Toast>): string {
        return this.show({
            type: 'info',
            title,
            message,
            icon: 'pi pi-info-circle',
            ...options
        });
    }

    // Quick feedback methods
    quickSuccess(message: string): string {
        return this.success('成功', message, {
            duration: 3000,
            animation: 'bounce'
        });
    }

    quickError(message: string): string {
        return this.error('錯誤', message, {
            duration: 5000,
            animation: 'shake'
        });
    }

    // Action toast
    actionToast(title: string, message: string, actionLabel: string, actionHandler: () => void): string {
        return this.show({
            type: 'info',
            title,
            message,
            duration: 0, // Don't auto-dismiss
            action: {
                label: actionLabel,
                handler: actionHandler
            }
        });
    }

    // Loading toast
    loading(title: string, message?: string): string {
        return this.show({
            type: 'info',
            title,
            message,
            duration: 0, // Don't auto-dismiss
            dismissible: false,
            icon: 'pi pi-spin pi-spinner',
            animation: 'fade'
        });
    }

    dismiss(id: string): void {
        const currentToasts = this.toastsSubject.value;
        const toast = currentToasts.find(t => t.id === id);

        if (toast) {
            // Add exit animation class
            const toastElement = document.querySelector(`[data-toast-id="${id}"]`) as HTMLElement;
            if (toastElement) {
                toastElement.classList.add('toast-exit');
                setTimeout(() => {
                    this.toastsSubject.next(currentToasts.filter(t => t.id !== id));
                }, 300); // Wait for exit animation
            } else {
                this.toastsSubject.next(currentToasts.filter(t => t.id !== id));
            }
        }
    }

    clear(): void {
        this.toastsSubject.next([]);
    }

    // Element feedback animations
    animateElement(options: FeedbackAnimation): void {
        const { element, type, animation, duration = 600 } = options;

        // Remove any existing animation classes
        element.classList.remove(
            'feedback-success', 'feedback-error', 'feedback-warning',
            'feedback-info', 'feedback-loading',
            'animate-pulse', 'animate-shake', 'animate-bounce',
            'animate-glow', 'animate-ripple'
        );

        // Add feedback type class
        element.classList.add(`feedback-${type}`);

        // Add animation class
        element.classList.add(`animate-${animation}`);

        // Remove classes after animation
        setTimeout(() => {
            element.classList.remove(`feedback-${type}`, `animate-${animation}`);
        }, duration);
    }

    // Specific element feedback methods
    successFeedback(element: HTMLElement, duration?: number): void {
        this.animateElement({
            element,
            type: 'success',
            animation: 'bounce',
            duration
        });
    }

    errorFeedback(element: HTMLElement, duration?: number): void {
        this.animateElement({
            element,
            type: 'error',
            animation: 'shake',
            duration
        });
    }

    loadingFeedback(element: HTMLElement, show = true): void {
        if (show) {
            element.classList.add('feedback-loading', 'animate-pulse');
        } else {
            element.classList.remove('feedback-loading', 'animate-pulse');
        }
    }

    // Button specific feedback
    buttonSuccess(button: HTMLElement): void {
        const originalText = button.textContent;
        const originalIcon = button.querySelector('i')?.className;

        // Change to success state
        button.textContent = '成功!';
        const icon = button.querySelector('i');
        if (icon) {
            icon.className = 'pi pi-check';
        }

        this.successFeedback(button);

        // Revert after animation
        setTimeout(() => {
            button.textContent = originalText;
            if (icon && originalIcon) {
                icon.className = originalIcon;
            }
        }, 2000);
    }

    buttonError(button: HTMLElement): void {
        const originalText = button.textContent;

        // Change to error state
        button.textContent = '失敗';
        this.errorFeedback(button);

        // Revert after animation
        setTimeout(() => {
            button.textContent = originalText;
        }, 2000);
    }

    // Form field feedback
    fieldSuccess(field: HTMLElement): void {
        field.classList.remove('field-error');
        field.classList.add('field-success');
        this.successFeedback(field, 1000);

        setTimeout(() => {
            field.classList.remove('field-success');
        }, 3000);
    }

    fieldError(field: HTMLElement): void {
        field.classList.remove('field-success');
        field.classList.add('field-error');
        this.errorFeedback(field, 500);
    }

    // Progress feedback
    progressSuccess(container: HTMLElement, message = '完成!'): void {
        const progressElement = container.querySelector('.progress-icon') as HTMLElement;
        const messageElement = container.querySelector('.progress-message') as HTMLElement;

        if (progressElement) {
            progressElement.className = 'progress-icon progress-success';
            progressElement.innerHTML = '<i class="pi pi-check"></i>';
        }

        if (messageElement) {
            messageElement.textContent = message;
        }

        this.successFeedback(container);
    }

    progressError(container: HTMLElement, message = '發生錯誤'): void {
        const progressElement = container.querySelector('.progress-icon') as HTMLElement;
        const messageElement = container.querySelector('.progress-message') as HTMLElement;

        if (progressElement) {
            progressElement.className = 'progress-icon progress-error';
            progressElement.innerHTML = '<i class="pi pi-times"></i>';
        }

        if (messageElement) {
            messageElement.textContent = message;
        }

        this.errorFeedback(container);
    }
}