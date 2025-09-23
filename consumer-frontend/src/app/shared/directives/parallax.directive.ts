import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ParallaxOptions, SmoothScrollService } from '../services/smooth-scroll.service';

@Directive({
    selector: '[appParallax]',
    standalone: true
})
export class ParallaxDirective implements OnInit, OnDestroy {
    @Input() parallaxSpeed = 0.5;
    @Input() parallaxDirection: 'up' | 'down' | 'left' | 'right' = 'up';
    @Input() parallaxTrigger: 'scroll' | 'mouse' = 'scroll';
    @Input() parallaxDisableOnMobile = true;

    private isMobile = false;

    constructor(
        private el: ElementRef<HTMLElement>,
        private smoothScrollService: SmoothScrollService
    ) { }

    ngOnInit() {
        this.checkIfMobile();

        if (this.parallaxDisableOnMobile && this.isMobile) {
            return;
        }

        this.setupParallax();
    }

    ngOnDestroy() {
        this.smoothScrollService.unregisterParallaxElement(this.el.nativeElement);
    }

    private checkIfMobile() {
        this.isMobile = window.innerWidth <= 768 ||
            /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    }

    private setupParallax() {
        const options: ParallaxOptions = {
            speed: this.parallaxSpeed,
            direction: this.parallaxDirection,
            trigger: this.parallaxTrigger
        };

        this.smoothScrollService.registerParallaxElement(this.el.nativeElement, options);
    }
}