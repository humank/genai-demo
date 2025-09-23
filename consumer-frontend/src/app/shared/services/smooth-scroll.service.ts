import { Injectable } from '@angular/core';

export interface ScrollToOptions {
    element?: HTMLElement | string;
    offset?: number;
    duration?: number;
    easing?: 'linear' | 'easeInOut' | 'easeIn' | 'easeOut' | 'bounce';
    callback?: () => void;
}

export interface ParallaxOptions {
    speed?: number;
    direction?: 'up' | 'down' | 'left' | 'right';
    trigger?: 'scroll' | 'mouse';
}

@Injectable({
    providedIn: 'root'
})
export class SmoothScrollService {
    private isScrolling = false;
    private scrollListeners: (() => void)[] = [];
    private parallaxElements = new Map<HTMLElement, ParallaxOptions>();
    private rafId?: number;

    constructor() {
        this.initScrollListener();
    }

    private initScrollListener(): void {
        window.addEventListener('scroll', this.handleScroll.bind(this), { passive: true });
        window.addEventListener('resize', this.handleResize.bind(this), { passive: true });
    }

    scrollTo(options: ScrollToOptions): Promise<void> {
        return new Promise((resolve) => {
            const {
                element,
                offset = 0,
                duration = 800,
                easing = 'easeInOut',
                callback
            } = options;

            let targetPosition = 0;

            if (typeof element === 'string') {
                const targetElement = document.getElementById(element) || document.querySelector(element);
                if (targetElement) {
                    targetPosition = targetElement.getBoundingClientRect().top + window.pageYOffset - offset;
                }
            } else if (element instanceof HTMLElement) {
                targetPosition = element.getBoundingClientRect().top + window.pageYOffset - offset;
            } else {
                targetPosition = 0; // Scroll to top
            }

            this.smoothScrollTo(targetPosition, duration, easing).then(() => {
                callback?.();
                resolve();
            });
        });
    }

    scrollToTop(duration = 800): Promise<void> {
        return this.scrollTo({ duration });
    }

    scrollToElement(element: HTMLElement | string, offset = 0, duration = 800): Promise<void> {
        return this.scrollTo({ element, offset, duration });
    }

    private smoothScrollTo(targetPosition: number, duration: number, easing: string): Promise<void> {
        return new Promise((resolve) => {
            const startPosition = window.pageYOffset;
            const distance = targetPosition - startPosition;
            let start: number | null = null;

            this.isScrolling = true;

            const easingFunction = this.getEasingFunction(easing);

            const animation = (currentTime: number) => {
                if (start === null) start = currentTime;
                const timeElapsed = currentTime - start;
                const progress = Math.min(timeElapsed / duration, 1);
                const ease = easingFunction(progress);

                window.scrollTo(0, startPosition + distance * ease);

                if (timeElapsed < duration) {
                    requestAnimationFrame(animation);
                } else {
                    this.isScrolling = false;
                    resolve();
                }
            };

            requestAnimationFrame(animation);
        });
    }

    private getEasingFunction(easing: string): (t: number) => number {
        const easingFunctions = {
            linear: (t: number) => t,
            easeInOut: (t: number) => t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1,
            easeIn: (t: number) => t * t * t,
            easeOut: (t: number) => (--t) * t * t + 1,
            bounce: (t: number) => {
                if (t < 1 / 2.75) {
                    return 7.5625 * t * t;
                } else if (t < 2 / 2.75) {
                    return 7.5625 * (t -= 1.5 / 2.75) * t + 0.75;
                } else if (t < 2.5 / 2.75) {
                    return 7.5625 * (t -= 2.25 / 2.75) * t + 0.9375;
                } else {
                    return 7.5625 * (t -= 2.625 / 2.75) * t + 0.984375;
                }
            }
        };

        return easingFunctions[easing as keyof typeof easingFunctions] || easingFunctions.easeInOut;
    }

    // Enhanced Parallax Effects
    registerParallaxElement(element: HTMLElement, options: ParallaxOptions = {}): void {
        const defaultOptions: ParallaxOptions = {
            speed: 0.5,
            direction: 'up',
            trigger: 'scroll'
        };

        this.parallaxElements.set(element, { ...defaultOptions, ...options });

        // Add data attribute for CSS targeting
        element.setAttribute('data-parallax', 'true');
        element.style.willChange = 'transform';
    }

    unregisterParallaxElement(element: HTMLElement): void {
        this.parallaxElements.delete(element);
        element.removeAttribute('data-parallax');
        element.style.willChange = 'auto';
        element.style.transform = '';
    }

    updateParallaxElements(): void {
        if (this.parallaxElements.size === 0) return;

        const scrolled = window.pageYOffset;
        const windowHeight = window.innerHeight;

        this.parallaxElements.forEach((options, element) => {
            const rect = element.getBoundingClientRect();
            const elementTop = rect.top + scrolled;
            const elementHeight = rect.height;

            // Check if element is in viewport
            if (rect.bottom >= 0 && rect.top <= windowHeight) {
                const { speed = 0.5, direction = 'up' } = options;

                // Calculate parallax offset
                const elementCenter = elementTop + elementHeight / 2;
                const viewportCenter = scrolled + windowHeight / 2;
                const distance = elementCenter - viewportCenter;

                let transform = '';

                switch (direction) {
                    case 'up':
                        transform = `translateY(${distance * speed}px)`;
                        break;
                    case 'down':
                        transform = `translateY(${-distance * speed}px)`;
                        break;
                    case 'left':
                        transform = `translateX(${distance * speed}px)`;
                        break;
                    case 'right':
                        transform = `translateX(${-distance * speed}px)`;
                        break;
                }

                element.style.transform = transform;
            }
        });
    }

    // Scroll progress indicator
    updateScrollProgress(): void {
        const scrollTop = window.pageYOffset;
        const docHeight = document.documentElement.scrollHeight - window.innerHeight;
        const scrollPercent = Math.max(0, Math.min(100, (scrollTop / docHeight) * 100));

        // Update progress bar
        const progressBar = document.querySelector('.scroll-progress-bar') as HTMLElement;
        if (progressBar) {
            progressBar.style.width = `${scrollPercent}%`;
        }

        // Update progress circle
        const progressCircle = document.querySelector('.scroll-progress-circle') as SVGCircleElement;
        if (progressCircle) {
            const circumference = 2 * Math.PI * 20; // Assuming radius of 20
            const offset = circumference - (scrollPercent / 100) * circumference;
            progressCircle.style.strokeDashoffset = offset.toString();
        }

        // Dispatch custom event
        window.dispatchEvent(new CustomEvent('scrollProgress', {
            detail: { progress: scrollPercent }
        }));
    }

    // Scroll-based animations
    addScrollListener(callback: () => void): void {
        this.scrollListeners.push(callback);
    }

    removeScrollListener(callback: () => void): void {
        const index = this.scrollListeners.indexOf(callback);
        if (index > -1) {
            this.scrollListeners.splice(index, 1);
        }
    }

    private handleScroll(): void {
        if (this.rafId) {
            cancelAnimationFrame(this.rafId);
        }

        this.rafId = requestAnimationFrame(() => {
            this.updateParallaxElements();
            this.updateScrollProgress();

            // Call registered listeners
            this.scrollListeners.forEach(callback => callback());
        });
    }

    private handleResize(): void {
        // Recalculate parallax elements on resize
        this.updateParallaxElements();
    }

    // Utility methods
    isElementInViewport(element: HTMLElement, threshold = 0): boolean {
        const rect = element.getBoundingClientRect();
        const windowHeight = window.innerHeight;

        return (
            rect.top <= windowHeight * (1 - threshold) &&
            rect.bottom >= windowHeight * threshold
        );
    }

    getScrollDirection(): () => 'up' | 'down' {
        let lastScrollTop = 0;

        return () => {
            const currentScrollTop = window.pageYOffset;
            const direction = currentScrollTop > lastScrollTop ? 'down' : 'up';
            lastScrollTop = currentScrollTop;
            return direction;
        };
    }

    // Smooth scroll to section with navigation
    scrollToSection(sectionId: string, updateUrl = true): Promise<void> {
        return this.scrollTo({
            element: `#${sectionId}`,
            offset: 80, // Account for fixed header
            duration: 1000,
            easing: 'easeInOut',
            callback: () => {
                if (updateUrl && history.pushState) {
                    history.pushState(null, '', `#${sectionId}`);
                }
            }
        });
    }

    // Auto-hide/show header on scroll
    setupAutoHideHeader(headerSelector = '.header'): void {
        let lastScrollTop = 0;
        const header = document.querySelector(headerSelector) as HTMLElement;

        if (!header) return;

        this.addScrollListener(() => {
            const currentScrollTop = window.pageYOffset;

            if (currentScrollTop > lastScrollTop && currentScrollTop > 100) {
                // Scrolling down
                header.style.transform = 'translateY(-100%)';
            } else {
                // Scrolling up
                header.style.transform = 'translateY(0)';
            }

            lastScrollTop = currentScrollTop;
        });
    }

    destroy(): void {
        if (this.rafId) {
            cancelAnimationFrame(this.rafId);
        }

        window.removeEventListener('scroll', this.handleScroll.bind(this));
        window.removeEventListener('resize', this.handleResize.bind(this));

        this.parallaxElements.clear();
        this.scrollListeners.length = 0;
    }
}