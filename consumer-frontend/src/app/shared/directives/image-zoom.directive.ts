import { Directive, ElementRef, HostListener, Input, OnDestroy, OnInit, Renderer2 } from '@angular/core';

@Directive({
    selector: '[appImageZoom]',
    standalone: true
})
export class ImageZoomDirective implements OnInit, OnDestroy {
    @Input() zoomFactor = 1.2;
    @Input() zoomDuration = 300;
    @Input() hoverImage?: string; // Optional hover image URL

    private originalImage?: string;
    private isZoomed = false;
    private transitionTimeout?: number;

    constructor(
        private el: ElementRef<HTMLImageElement>,
        private renderer: Renderer2
    ) { }

    ngOnInit() {
        this.setupZoomContainer();
        this.originalImage = this.el.nativeElement.src;
    }

    ngOnDestroy() {
        if (this.transitionTimeout) {
            clearTimeout(this.transitionTimeout);
        }
    }

    private setupZoomContainer() {
        const img = this.el.nativeElement;
        const container = img.parentElement;

        if (container) {
            // Ensure container has overflow hidden and relative positioning
            this.renderer.setStyle(container, 'overflow', 'hidden');
            this.renderer.setStyle(container, 'position', 'relative');

            // Set up image for zoom
            this.renderer.setStyle(img, 'transition', `transform ${this.zoomDuration}ms cubic-bezier(0.4, 0, 0.2, 1)`);
            this.renderer.setStyle(img, 'transform-origin', 'center center');
            this.renderer.setStyle(img, 'will-change', 'transform');
        }
    }

    @HostListener('mouseenter', ['$event'])
    onMouseEnter(event: MouseEvent) {
        this.zoomIn();

        // Change to hover image if provided
        if (this.hoverImage && this.originalImage) {
            this.changeImage(this.hoverImage);
        }
    }

    @HostListener('mouseleave', ['$event'])
    onMouseLeave(event: MouseEvent) {
        this.zoomOut();

        // Revert to original image
        if (this.hoverImage && this.originalImage) {
            this.changeImage(this.originalImage);
        }
    }

    @HostListener('mousemove', ['$event'])
    onMouseMove(event: MouseEvent) {
        if (this.isZoomed) {
            this.updateZoomPosition(event);
        }
    }

    private zoomIn() {
        const img = this.el.nativeElement;
        this.isZoomed = true;

        this.renderer.setStyle(img, 'transform', `scale(${this.zoomFactor})`);
        this.renderer.addClass(img, 'zoomed');
    }

    private zoomOut() {
        const img = this.el.nativeElement;
        this.isZoomed = false;

        this.renderer.setStyle(img, 'transform', 'scale(1)');
        this.renderer.removeClass(img, 'zoomed');
    }

    private updateZoomPosition(event: MouseEvent) {
        const img = this.el.nativeElement;
        const container = img.parentElement;

        if (!container) return;

        const rect = container.getBoundingClientRect();
        const x = ((event.clientX - rect.left) / rect.width) * 100;
        const y = ((event.clientY - rect.top) / rect.height) * 100;

        this.renderer.setStyle(img, 'transform-origin', `${x}% ${y}%`);
    }

    private changeImage(newSrc: string) {
        const img = this.el.nativeElement;

        // Add fade effect during image change
        this.renderer.setStyle(img, 'opacity', '0.7');

        this.transitionTimeout = window.setTimeout(() => {
            img.src = newSrc;
            this.renderer.setStyle(img, 'opacity', '1');
        }, 150);
    }
}