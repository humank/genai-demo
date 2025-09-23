import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';

export type FeedbackType = 'success' | 'error' | 'warning' | 'info' | 'loading';
export type AnimationType = 'pulse' | 'shake' | 'bounce' | 'glow' | 'ripple' | 'scale' | 'rotate';

@Directive({
    selector: '[appFeedbackAnimation]',
    standalone: true
})
export class FeedbackAnimationDirective implements OnInit, OnDestroy {
    @Input() feedbackType: FeedbackType = 'success';
    @Input() animationType: AnimationType = 'pulse';
    @Input() duration = 600;
    @Input() trigger: 'click' | 'hover' | 'manual' = 'manual';
    @Input() autoReset = true;

    private animationTimeout?: number;
    private isAnimating = false;

    constructor(
        private el: ElementRef<HTMLElement>,
        private renderer: Renderer2
    ) { }

    ngOnInit() {
        this.setupTriggers();
    }

    ngOnDestroy() {
        if (this.animationTimeout) {
            clearTimeout(this.animationTimeout);
        }
    }

    private setupTriggers() {
        const element = this.el.nativeElement;

        if (this.trigger === 'click') {
            this.renderer.listen(element, 'click', () => {
                this.animate();
            });
        } else if (this.trigger === 'hover') {
            this.renderer.listen(element, 'mouseenter', () => {
                this.animate();
            });
        }
    }

    animate(): void {
        if (this.isAnimating) return;

        const element = this.el.nativeElement;
        this.isAnimating = true;

        // Clear any existing animation classes
        this.clearAnimationClasses();

        // Add feedback type class
        this.renderer.addClass(element, `feedback-${this.feedbackType}`);

        // Add animation class
        this.renderer.addClass(element, `animate-${this.animationType}`);

        // Set custom animation duration
        this.renderer.setStyle(element, 'animation-duration', `${this.duration}ms`);

        // Handle specific animations
        this.handleSpecificAnimations();

        // Auto-reset after animation
        if (this.autoReset) {
            this.animationTimeout = window.setTimeout(() => {
                this.reset();
            }, this.duration);
        }
    }

    reset(): void {
        this.clearAnimationClasses();
        this.isAnimating = false;
    }

    private clearAnimationClasses(): void {
        const element = this.el.nativeElement;

        // Remove feedback classes
        element.classList.remove(
            'feedback-success', 'feedback-error', 'feedback-warning',
            'feedback-info', 'feedback-loading'
        );

        // Remove animation classes
        element.classList.remove(
            'animate-pulse', 'animate-shake', 'animate-bounce',
            'animate-glow', 'animate-ripple', 'animate-scale', 'animate-rotate'
        );

        // Reset styles
        this.renderer.removeStyle(element, 'animation-duration');
    }

    private handleSpecificAnimations(): void {
        const element = this.el.nativeElement;

        switch (this.animationType) {
            case 'ripple':
                this.createRippleEffect();
                break;
            case 'glow':
                this.createGlowEffect();
                break;
            case 'scale':
                this.createScaleEffect();
                break;
            case 'rotate':
                this.createRotateEffect();
                break;
        }
    }

    private createRippleEffect(): void {
        const element = this.el.nativeElement;
        const ripple = this.renderer.createElement('div');

        this.renderer.addClass(ripple, 'feedback-ripple');
        this.renderer.setStyle(ripple, 'position', 'absolute');
        this.renderer.setStyle(ripple, 'top', '50%');
        this.renderer.setStyle(ripple, 'left', '50%');
        this.renderer.setStyle(ripple, 'width', '0');
        this.renderer.setStyle(ripple, 'height', '0');
        this.renderer.setStyle(ripple, 'border-radius', '50%');
        this.renderer.setStyle(ripple, 'background', this.getRippleColor());
        this.renderer.setStyle(ripple, 'transform', 'translate(-50%, -50%)');
        this.renderer.setStyle(ripple, 'animation', `rippleExpand ${this.duration}ms ease-out`);
        this.renderer.setStyle(ripple, 'pointer-events', 'none');
        this.renderer.setStyle(ripple, 'z-index', '1');

        // Ensure parent has relative positioning
        const position = window.getComputedStyle(element).position;
        if (position === 'static') {
            this.renderer.setStyle(element, 'position', 'relative');
        }

        this.renderer.appendChild(element, ripple);

        // Remove ripple after animation
        setTimeout(() => {
            if (ripple.parentNode) {
                this.renderer.removeChild(element, ripple);
            }
        }, this.duration);
    }

    private createGlowEffect(): void {
        const element = this.el.nativeElement;
        const glowColor = this.getGlowColor();

        this.renderer.setStyle(element, 'box-shadow',
            `0 0 20px ${glowColor}, 0 0 40px ${glowColor}, 0 0 60px ${glowColor}`);

        setTimeout(() => {
            this.renderer.removeStyle(element, 'box-shadow');
        }, this.duration);
    }

    private createScaleEffect(): void {
        const element = this.el.nativeElement;

        this.renderer.setStyle(element, 'transform', 'scale(1.1)');
        this.renderer.setStyle(element, 'transition', `transform ${this.duration}ms ease-out`);

        setTimeout(() => {
            this.renderer.setStyle(element, 'transform', 'scale(1)');
            setTimeout(() => {
                this.renderer.removeStyle(element, 'transform');
                this.renderer.removeStyle(element, 'transition');
            }, this.duration);
        }, 50);
    }

    private createRotateEffect(): void {
        const element = this.el.nativeElement;

        this.renderer.setStyle(element, 'transform', 'rotate(360deg)');
        this.renderer.setStyle(element, 'transition', `transform ${this.duration}ms ease-in-out`);

        setTimeout(() => {
            this.renderer.setStyle(element, 'transform', 'rotate(0deg)');
            setTimeout(() => {
                this.renderer.removeStyle(element, 'transform');
                this.renderer.removeStyle(element, 'transition');
            }, this.duration);
        }, this.duration);
    }

    private getRippleColor(): string {
        const colors = {
            success: 'rgba(16, 185, 129, 0.3)',
            error: 'rgba(239, 68, 68, 0.3)',
            warning: 'rgba(245, 158, 11, 0.3)',
            info: 'rgba(59, 130, 246, 0.3)',
            loading: 'rgba(156, 163, 175, 0.3)'
        };
        return colors[this.feedbackType];
    }

    private getGlowColor(): string {
        const colors = {
            success: 'rgba(16, 185, 129, 0.5)',
            error: 'rgba(239, 68, 68, 0.5)',
            warning: 'rgba(245, 158, 11, 0.5)',
            info: 'rgba(59, 130, 246, 0.5)',
            loading: 'rgba(156, 163, 175, 0.5)'
        };
        return colors[this.feedbackType];
    }

    // Public methods for programmatic control
    success(): void {
        this.feedbackType = 'success';
        this.animationType = 'bounce';
        this.animate();
    }

    error(): void {
        this.feedbackType = 'error';
        this.animationType = 'shake';
        this.animate();
    }

    loading(): void {
        this.feedbackType = 'loading';
        this.animationType = 'pulse';
        this.autoReset = false;
        this.animate();
    }

    stopLoading(): void {
        this.reset();
    }
}