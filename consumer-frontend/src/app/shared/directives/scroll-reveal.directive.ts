import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';

@Directive({
    selector: '[appScrollReveal]',
    standalone: true
})
export class ScrollRevealDirective implements OnInit, OnDestroy {
    @Input() threshold = 0.1;
    @Input() delay = 0;
    @Input() animation = 'fade-in-up';

    private observer: IntersectionObserver | null = null;

    constructor(private el: ElementRef) { }

    ngOnInit() {
        // Add initial hidden state
        this.el.nativeElement.style.opacity = '0';
        this.el.nativeElement.style.transform = this.getInitialTransform();
        this.el.nativeElement.style.transition = `all 0.7s cubic-bezier(0, 0, 0.2, 1) ${this.delay}ms`;

        // Create intersection observer
        this.observer = new IntersectionObserver(
            (entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        this.reveal();
                    }
                });
            },
            {
                threshold: this.threshold,
                rootMargin: '0px 0px -50px 0px'
            }
        );

        this.observer.observe(this.el.nativeElement);
    }

    ngOnDestroy() {
        if (this.observer) {
            this.observer.disconnect();
        }
    }

    private getInitialTransform(): string {
        switch (this.animation) {
            case 'fade-in-up':
                return 'translateY(30px)';
            case 'fade-in-down':
                return 'translateY(-30px)';
            case 'fade-in-left':
                return 'translateX(-30px)';
            case 'fade-in-right':
                return 'translateX(30px)';
            case 'scale-in':
                return 'scale(0.9)';
            default:
                return 'translateY(30px)';
        }
    }

    private reveal() {
        this.el.nativeElement.style.opacity = '1';
        this.el.nativeElement.style.transform = 'translateY(0) translateX(0) scale(1)';

        // Disconnect observer after revealing
        if (this.observer) {
            this.observer.disconnect();
        }
    }
}