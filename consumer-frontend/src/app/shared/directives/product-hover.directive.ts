import { Directive, ElementRef, HostListener, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';

@Directive({
    selector: '[appProductHover]',
    standalone: true
})
export class ProductHoverDirective implements OnInit, OnDestroy {
    @Input() hoverEffect: 'lift' | 'scale' | 'tilt' | 'glow' | 'slide' = 'lift';
    @Input() intensity: 'subtle' | 'normal' | 'strong' = 'normal';
    @Input() showQuickActions = true;

    private quickActionsElement?: HTMLElement;
    private isHovered = false;
    private animationFrame?: number;

    constructor(
        private el: ElementRef<HTMLElement>,
        private renderer: Renderer2
    ) { }

    ngOnInit() {
        this.setupHoverEffects();
        if (this.showQuickActions) {
            this.setupQuickActions();
        }
    }

    ngOnDestroy() {
        if (this.animationFrame) {
            cancelAnimationFrame(this.animationFrame);
        }
    }

    private setupHoverEffects() {
        const element = this.el.nativeElement;

        // Set up base styles
        this.renderer.setStyle(element, 'transition', 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)');
        this.renderer.setStyle(element, 'transform-style', 'preserve-3d');
        this.renderer.setStyle(element, 'will-change', 'transform, box-shadow');

        // Add hover class for CSS targeting
        this.renderer.addClass(element, 'product-hover-container');
        this.renderer.addClass(element, `hover-effect-${this.hoverEffect}`);
        this.renderer.addClass(element, `hover-intensity-${this.intensity}`);
    }

    private setupQuickActions() {
        const element = this.el.nativeElement;
        this.quickActionsElement = element.querySelector('.product-quick-actions') as HTMLElement;

        if (this.quickActionsElement) {
            // Initially hide quick actions
            this.renderer.setStyle(this.quickActionsElement, 'opacity', '0');
            this.renderer.setStyle(this.quickActionsElement, 'transform', 'translateY(10px)');
            this.renderer.setStyle(this.quickActionsElement, 'transition', 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)');
        }
    }

    @HostListener('mouseenter', ['$event'])
    onMouseEnter(event: MouseEvent) {
        this.isHovered = true;
        this.applyHoverEffect();
        this.showQuickActionsElement();
    }

    @HostListener('mouseleave', ['$event'])
    onMouseLeave(event: MouseEvent) {
        this.isHovered = false;
        this.removeHoverEffect();
        this.hideQuickActionsElement();
    }

    @HostListener('mousemove', ['$event'])
    onMouseMove(event: MouseEvent) {
        if (this.hoverEffect === 'tilt' && this.isHovered) {
            this.updateTiltEffect(event);
        }
    }

    private applyHoverEffect() {
        const element = this.el.nativeElement;

        switch (this.hoverEffect) {
            case 'lift':
                this.applyLiftEffect(element);
                break;
            case 'scale':
                this.applyScaleEffect(element);
                break;
            case 'tilt':
                this.applyTiltEffect(element);
                break;
            case 'glow':
                this.applyGlowEffect(element);
                break;
            case 'slide':
                this.applySlideEffect(element);
                break;
        }

        this.renderer.addClass(element, 'hovered');
    }

    private removeHoverEffect() {
        const element = this.el.nativeElement;

        // Reset all transforms
        this.renderer.setStyle(element, 'transform', 'none');
        this.renderer.setStyle(element, 'box-shadow', '');

        this.renderer.removeClass(element, 'hovered');
    }

    private applyLiftEffect(element: HTMLElement) {
        const liftAmount = this.getLiftAmount();
        const shadowIntensity = this.getShadowIntensity();

        this.renderer.setStyle(element, 'transform', `translateY(-${liftAmount}px)`);
        this.renderer.setStyle(element, 'box-shadow', `0 ${liftAmount * 2}px ${liftAmount * 4}px rgba(0, 0, 0, ${shadowIntensity})`);
    }

    private applyScaleEffect(element: HTMLElement) {
        const scaleAmount = this.getScaleAmount();

        this.renderer.setStyle(element, 'transform', `scale(${scaleAmount})`);
    }

    private applyTiltEffect(element: HTMLElement) {
        // Initial tilt setup - actual tilt happens in mousemove
        this.renderer.setStyle(element, 'transform-style', 'preserve-3d');
    }

    private applyGlowEffect(element: HTMLElement) {
        const glowIntensity = this.getGlowIntensity();

        this.renderer.setStyle(element, 'box-shadow',
            `0 0 ${glowIntensity}px rgba(59, 130, 246, 0.5), 0 0 ${glowIntensity * 2}px rgba(59, 130, 246, 0.3)`);
    }

    private applySlideEffect(element: HTMLElement) {
        const slideAmount = this.getSlideAmount();

        this.renderer.setStyle(element, 'transform', `translateY(-${slideAmount}px)`);

        // Add slide overlay effect
        const overlay = element.querySelector('.product-slide-overlay') as HTMLElement;
        if (overlay) {
            this.renderer.setStyle(overlay, 'transform', 'translateY(0)');
        }
    }

    private updateTiltEffect(event: MouseEvent) {
        const element = this.el.nativeElement;
        const rect = element.getBoundingClientRect();

        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;

        const mouseX = event.clientX - centerX;
        const mouseY = event.clientY - centerY;

        const tiltX = (mouseY / (rect.height / 2)) * this.getTiltAmount();
        const tiltY = (mouseX / (rect.width / 2)) * this.getTiltAmount();

        this.animationFrame = requestAnimationFrame(() => {
            this.renderer.setStyle(element, 'transform',
                `perspective(1000px) rotateX(${-tiltX}deg) rotateY(${tiltY}deg)`);
        });
    }

    private showQuickActionsElement() {
        if (this.quickActionsElement) {
            this.renderer.setStyle(this.quickActionsElement, 'opacity', '1');
            this.renderer.setStyle(this.quickActionsElement, 'transform', 'translateY(0)');
        }
    }

    private hideQuickActionsElement() {
        if (this.quickActionsElement) {
            this.renderer.setStyle(this.quickActionsElement, 'opacity', '0');
            this.renderer.setStyle(this.quickActionsElement, 'transform', 'translateY(10px)');
        }
    }

    private getLiftAmount(): number {
        const amounts = { subtle: 4, normal: 8, strong: 12 };
        return amounts[this.intensity];
    }

    private getScaleAmount(): number {
        const amounts = { subtle: 1.02, normal: 1.05, strong: 1.08 };
        return amounts[this.intensity];
    }

    private getTiltAmount(): number {
        const amounts = { subtle: 5, normal: 10, strong: 15 };
        return amounts[this.intensity];
    }

    private getShadowIntensity(): number {
        const intensities = { subtle: 0.1, normal: 0.15, strong: 0.2 };
        return intensities[this.intensity];
    }

    private getGlowIntensity(): number {
        const intensities = { subtle: 10, normal: 20, strong: 30 };
        return intensities[this.intensity];
    }

    private getSlideAmount(): number {
        const amounts = { subtle: 2, normal: 4, strong: 6 };
        return amounts[this.intensity];
    }
}