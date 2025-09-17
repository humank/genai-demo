import { Injectable } from '@angular/core';
import { ObservabilityService } from './observability.service';

export interface UserSession {
    sessionId: string;
    startTime: number;
    lastActivity: number;
    pageViews: number;
    interactions: number;
    scrollDepth: number;
    timeOnPage: number;
}

export interface ProductInteraction {
    productId: string;
    productName: string;
    interactionType: 'view' | 'hover' | 'click' | 'zoom' | 'add_to_cart' | 'add_to_wishlist';
    timestamp: number;
    duration?: number;
    position?: number;
    section: string;
}

export interface SearchBehavior {
    query: string;
    timestamp: number;
    resultsCount?: number;
    clickedResult?: string;
    refinements: string[];
    abandonedAt?: 'input' | 'results' | 'filters';
}

export interface ConversionFunnel {
    step: 'product_view' | 'add_to_cart' | 'checkout_start' | 'checkout_complete';
    timestamp: number;
    productId?: string;
    value?: number;
    metadata?: Record<string, any>;
}

@Injectable({
    providedIn: 'root'
})
export class UserBehaviorAnalyticsService {
    private currentSession!: UserSession;
    private productInteractions: ProductInteraction[] = [];
    private searchBehaviors: SearchBehavior[] = [];
    private conversionFunnel: ConversionFunnel[] = [];
    private scrollDepthThresholds = [25, 50, 75, 90, 100];
    private scrollDepthReached: Set<number> = new Set();
    private hoverStartTimes: Map<string, number> = new Map();
    private pageStartTime: number = Date.now();

    constructor(private observabilityService: ObservabilityService) {
        this.initializeSession();
        this.setupScrollTracking();
        this.setupMouseTracking();
        this.setupKeyboardTracking();
        this.setupVisibilityTracking();
    }

    /**
     * 初始化用戶會話
     */
    private initializeSession(): void {
        this.currentSession = {
            sessionId: this.observabilityService.getSessionId(),
            startTime: Date.now(),
            lastActivity: Date.now(),
            pageViews: 1,
            interactions: 0,
            scrollDepth: 0,
            timeOnPage: 0
        };
    }

    /**
     * 設置滾動追蹤
     */
    private setupScrollTracking(): void {
        let scrollTimeout: number;

        window.addEventListener('scroll', () => {
            this.updateLastActivity();

            // Debounce scroll events
            clearTimeout(scrollTimeout);
            scrollTimeout = window.setTimeout(() => {
                this.trackScrollDepth();
            }, 100);
        });
    }

    /**
     * 設置滑鼠追蹤
     */
    private setupMouseTracking(): void {
        // Track mouse movements for engagement
        let mouseTimeout: number;
        let mouseMovements = 0;

        document.addEventListener('mousemove', () => {
            mouseMovements++;
            this.updateLastActivity();

            clearTimeout(mouseTimeout);
            mouseTimeout = window.setTimeout(() => {
                if (mouseMovements > 10) { // Significant mouse activity
                    this.observabilityService.trackUserAction('mouse_engagement', {
                        movements: mouseMovements,
                        timeWindow: 1000,
                        section: this.getCurrentSection()
                    });
                }
                mouseMovements = 0;
            }, 1000);
        });

        // Track clicks
        document.addEventListener('click', (event) => {
            this.trackClick(event);
        });
    }

    /**
     * 設置鍵盤追蹤
     */
    private setupKeyboardTracking(): void {
        document.addEventListener('keydown', (event) => {
            this.updateLastActivity();

            // Track specific key interactions
            if (event.key === 'Tab') {
                this.observabilityService.trackUserAction('keyboard_navigation', {
                    key: 'tab',
                    section: this.getCurrentSection()
                });
            }

            if (event.key === 'Enter') {
                this.observabilityService.trackUserAction('keyboard_interaction', {
                    key: 'enter',
                    target: (event.target as HTMLElement)?.tagName?.toLowerCase(),
                    section: this.getCurrentSection()
                });
            }
        });
    }

    /**
     * 設置頁面可見性追蹤
     */
    private setupVisibilityTracking(): void {
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) {
                this.trackPageExit();
            } else {
                this.trackPageReturn();
            }
        });

        // Track page unload
        window.addEventListener('beforeunload', () => {
            this.trackSessionEnd();
        });
    }

    /**
     * 追蹤產品互動
     */
    trackProductInteraction(interaction: Omit<ProductInteraction, 'timestamp'>): void {
        const fullInteraction: ProductInteraction = {
            ...interaction,
            timestamp: Date.now()
        };

        this.productInteractions.push(fullInteraction);
        this.updateLastActivity();

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'product_interaction',
            data: {
                ...fullInteraction,
                sessionId: this.currentSession.sessionId
            },
            timestamp: fullInteraction.timestamp,
            sessionId: this.currentSession.sessionId
        });

        // Track specific interaction patterns
        this.analyzeProductInteractionPatterns(fullInteraction);
    }

    /**
     * 追蹤產品懸停行為
     */
    trackProductHover(productId: string, productName: string, section: string): void {
        const hoverKey = `${productId}-${section}`;
        this.hoverStartTimes.set(hoverKey, Date.now());
    }

    /**
     * 追蹤產品懸停結束
     */
    trackProductHoverEnd(productId: string, productName: string, section: string): void {
        const hoverKey = `${productId}-${section}`;
        const startTime = this.hoverStartTimes.get(hoverKey);

        if (startTime) {
            const duration = Date.now() - startTime;
            this.hoverStartTimes.delete(hoverKey);

            this.trackProductInteraction({
                productId,
                productName,
                interactionType: 'hover',
                duration,
                section
            });

            // Track engagement level based on hover duration
            const engagementLevel = this.getEngagementLevel(duration);
            this.observabilityService.trackUserAction('product_hover_engagement', {
                productId,
                productName,
                duration,
                engagementLevel,
                section
            });
        }
    }

    /**
     * 追蹤搜尋行為
     */
    trackSearchBehavior(behavior: Omit<SearchBehavior, 'timestamp'>): void {
        const fullBehavior: SearchBehavior = {
            ...behavior,
            timestamp: Date.now()
        };

        this.searchBehaviors.push(fullBehavior);
        this.updateLastActivity();

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'search_behavior',
            data: {
                ...fullBehavior,
                sessionId: this.currentSession.sessionId
            },
            timestamp: fullBehavior.timestamp,
            sessionId: this.currentSession.sessionId
        });

        // Analyze search patterns
        this.analyzeSearchPatterns(fullBehavior);
    }

    /**
     * 追蹤轉換漏斗
     */
    trackConversionStep(step: ConversionFunnel): void {
        const fullStep: ConversionFunnel = {
            ...step,
            timestamp: Date.now()
        };

        this.conversionFunnel.push(fullStep);
        this.updateLastActivity();

        // Track as business event
        this.observabilityService.trackBusinessEvent({
            type: 'conversion_funnel',
            data: {
                ...fullStep,
                sessionId: this.currentSession.sessionId,
                funnelPosition: this.conversionFunnel.length
            },
            timestamp: fullStep.timestamp,
            sessionId: this.currentSession.sessionId
        });

        // Analyze funnel progression
        this.analyzeFunnelProgression(fullStep);
    }

    /**
     * 追蹤購物車互動
     */
    trackCartInteraction(action: 'open' | 'close' | 'add_item' | 'remove_item' | 'update_quantity', metadata?: Record<string, any>): void {
        this.updateLastActivity();

        this.observabilityService.trackBusinessEvent({
            type: 'cart_interaction',
            data: {
                action,
                timestamp: Date.now(),
                sessionId: this.currentSession.sessionId,
                ...metadata
            },
            timestamp: Date.now(),
            sessionId: this.currentSession.sessionId
        });

        // Track cart abandonment patterns
        if (action === 'close' && metadata?.['hasItems']) {
            this.trackCartAbandonment(metadata);
        }
    }

    /**
     * 追蹤篩選器使用
     */
    trackFilterUsage(filterType: string, filterValue: string, resultsCount: number): void {
        this.updateLastActivity();

        this.observabilityService.trackBusinessEvent({
            type: 'filter_usage',
            data: {
                filterType,
                filterValue,
                resultsCount,
                timestamp: Date.now(),
                sessionId: this.currentSession.sessionId
            },
            timestamp: Date.now(),
            sessionId: this.currentSession.sessionId
        });

        // Analyze filter effectiveness
        this.analyzeFilterEffectiveness(filterType, filterValue, resultsCount);
    }

    /**
     * 分析產品互動模式
     */
    private analyzeProductInteractionPatterns(interaction: ProductInteraction): void {
        const recentInteractions = this.productInteractions
            .filter(i => Date.now() - i.timestamp < 60000) // Last minute
            .filter(i => i.productId === interaction.productId);

        if (recentInteractions.length > 3) {
            this.observabilityService.trackUserAction('high_product_engagement', {
                productId: interaction.productId,
                productName: interaction.productName,
                interactionCount: recentInteractions.length,
                interactionTypes: [...new Set(recentInteractions.map(i => i.interactionType))],
                section: interaction.section
            });
        }
    }

    /**
     * 分析搜尋模式
     */
    private analyzeSearchPatterns(behavior: SearchBehavior): void {
        const recentSearches = this.searchBehaviors
            .filter(s => Date.now() - s.timestamp < 300000); // Last 5 minutes

        // Detect search refinement patterns
        if (recentSearches.length > 1) {
            const queries = recentSearches.map(s => s.query);
            const isRefinement = this.isSearchRefinement(queries);

            if (isRefinement) {
                this.observabilityService.trackUserAction('search_refinement_pattern', {
                    queries: queries.slice(-3), // Last 3 queries
                    refinementCount: recentSearches.length,
                    timeSpent: Date.now() - recentSearches[0].timestamp
                });
            }
        }

        // Detect zero results searches
        if (behavior.resultsCount === 0) {
            this.observabilityService.trackUserAction('zero_results_search', {
                query: behavior.query,
                queryLength: behavior.query.length,
                hasSpecialChars: /[^a-zA-Z0-9\s]/.test(behavior.query)
            });
        }
    }

    /**
     * 分析漏斗進展
     */
    private analyzeFunnelProgression(step: ConversionFunnel): void {
        const funnelSteps = ['product_view', 'add_to_cart', 'checkout_start', 'checkout_complete'];
        const currentStepIndex = funnelSteps.indexOf(step.step);

        if (currentStepIndex > 0) {
            const previousSteps = this.conversionFunnel
                .filter(s => funnelSteps.indexOf(s.step) < currentStepIndex)
                .sort((a, b) => b.timestamp - a.timestamp);

            if (previousSteps.length > 0) {
                const timeBetweenSteps = step.timestamp - previousSteps[0].timestamp;

                this.observabilityService.trackUserAction('funnel_progression', {
                    fromStep: previousSteps[0].step,
                    toStep: step.step,
                    timeBetweenSteps,
                    funnelPosition: currentStepIndex + 1,
                    totalSteps: funnelSteps.length
                });
            }
        }
    }

    /**
     * 追蹤滾動深度
     */
    private trackScrollDepth(): void {
        const scrollTop = window.pageYOffset;
        const documentHeight = document.documentElement.scrollHeight - window.innerHeight;
        const scrollPercent = Math.round((scrollTop / documentHeight) * 100);

        this.currentSession.scrollDepth = Math.max(this.currentSession.scrollDepth, scrollPercent);

        // Track scroll depth milestones
        this.scrollDepthThresholds.forEach(threshold => {
            if (scrollPercent >= threshold && !this.scrollDepthReached.has(threshold)) {
                this.scrollDepthReached.add(threshold);

                this.observabilityService.trackUserAction('scroll_depth_milestone', {
                    depth: threshold,
                    timeToReach: Date.now() - this.pageStartTime,
                    section: this.getCurrentSection()
                });
            }
        });
    }

    /**
     * 追蹤點擊事件
     */
    private trackClick(event: MouseEvent): void {
        this.currentSession.interactions++;
        this.updateLastActivity();

        const target = event.target as HTMLElement;
        const clickData = {
            elementType: target.tagName.toLowerCase(),
            elementClass: target.className,
            elementId: target.id,
            clickX: event.clientX,
            clickY: event.clientY,
            section: this.getCurrentSection(),
            timestamp: Date.now()
        };

        this.observabilityService.trackUserAction('click_interaction', clickData);

        // Track click patterns
        this.analyzeClickPatterns(clickData);
    }

    /**
     * 分析點擊模式
     */
    private analyzeClickPatterns(clickData: any): void {
        // Detect rapid clicking (potential frustration)
        const recentClicks = this.getRecentUserActions('click_interaction', 5000); // Last 5 seconds

        if (recentClicks.length > 5) {
            this.observabilityService.trackUserAction('rapid_clicking_detected', {
                clickCount: recentClicks.length,
                timeWindow: 5000,
                section: clickData.section,
                possibleFrustration: true
            });
        }
    }

    /**
     * 追蹤購物車放棄
     */
    private trackCartAbandonment(metadata: Record<string, any>): void {
        this.observabilityService.trackBusinessEvent({
            type: 'cart_abandonment',
            data: {
                itemCount: metadata['itemCount'],
                totalValue: metadata['totalValue'],
                timeInCart: metadata['timeInCart'],
                lastAction: metadata['lastAction'],
                sessionId: this.currentSession.sessionId
            },
            timestamp: Date.now(),
            sessionId: this.currentSession.sessionId
        });
    }

    /**
     * 分析篩選器效果
     */
    private analyzeFilterEffectiveness(filterType: string, filterValue: string, resultsCount: number): void {
        const effectiveness = resultsCount > 0 ? 'effective' : 'ineffective';

        this.observabilityService.trackUserAction('filter_effectiveness', {
            filterType,
            filterValue,
            resultsCount,
            effectiveness,
            timestamp: Date.now()
        });
    }

    /**
     * 追蹤頁面退出
     */
    private trackPageExit(): void {
        this.currentSession.timeOnPage = Date.now() - this.pageStartTime;

        this.observabilityService.trackUserAction('page_exit', {
            timeOnPage: this.currentSession.timeOnPage,
            scrollDepth: this.currentSession.scrollDepth,
            interactions: this.currentSession.interactions,
            exitType: 'visibility_change'
        });
    }

    /**
     * 追蹤頁面返回
     */
    private trackPageReturn(): void {
        this.observabilityService.trackUserAction('page_return', {
            timeAway: Date.now() - this.currentSession.lastActivity,
            returnType: 'visibility_change'
        });

        this.updateLastActivity();
    }

    /**
     * 追蹤會話結束
     */
    private trackSessionEnd(): void {
        this.currentSession.timeOnPage = Date.now() - this.pageStartTime;

        this.observabilityService.trackBusinessEvent({
            type: 'session_end',
            data: {
                ...this.currentSession,
                productInteractions: this.productInteractions.length,
                searchQueries: this.searchBehaviors.length,
                conversionSteps: this.conversionFunnel.length
            },
            timestamp: Date.now(),
            sessionId: this.currentSession.sessionId
        });
    }

    /**
     * 輔助方法
     */
    private updateLastActivity(): void {
        this.currentSession.lastActivity = Date.now();
    }

    private getCurrentSection(): string {
        const scrollTop = window.pageYOffset;
        const sections = document.querySelectorAll('section, .hero-section, .categories-showcase, .products-showcase');

        for (const section of Array.from(sections)) {
            const rect = section.getBoundingClientRect();
            if (rect.top <= 100 && rect.bottom >= 100) {
                return section.className.split(' ')[0] || 'unknown';
            }
        }

        return 'unknown';
    }

    private getEngagementLevel(duration: number): 'low' | 'medium' | 'high' {
        if (duration < 1000) return 'low';
        if (duration < 3000) return 'medium';
        return 'high';
    }

    private isSearchRefinement(queries: string[]): boolean {
        if (queries.length < 2) return false;

        const lastQuery = queries[queries.length - 1].toLowerCase();
        const previousQuery = queries[queries.length - 2].toLowerCase();

        return lastQuery.includes(previousQuery) || previousQuery.includes(lastQuery);
    }

    private getRecentUserActions(actionType: string, timeWindow: number): any[] {
        // This would need to be implemented to track recent actions
        // For now, return empty array
        return [];
    }

    /**
     * 獲取會話統計
     */
    getSessionStats(): UserSession & {
        productInteractionsCount: number;
        searchQueriesCount: number;
        conversionStepsCount: number;
    } {
        return {
            ...this.currentSession,
            timeOnPage: Date.now() - this.pageStartTime,
            productInteractionsCount: this.productInteractions.length,
            searchQueriesCount: this.searchBehaviors.length,
            conversionStepsCount: this.conversionFunnel.length
        };
    }

    /**
     * 清理服務
     */
    cleanup(): void {
        this.trackSessionEnd();
        this.productInteractions = [];
        this.searchBehaviors = [];
        this.conversionFunnel = [];
        this.scrollDepthReached.clear();
        this.hoverStartTimes.clear();
    }
}