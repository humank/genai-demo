---
adr_number: 027
title: "Search Strategy (Elasticsearch vs OpenSearch vs PostgreSQL)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 004]
affected_viewpoints: ["functional", "information", "deployment"]
affected_perspectives: ["performance", "scalability", "cost"]
---

# ADR-027: Search Strategy (Elasticsearch vs OpenSearch vs PostgreSQL)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires search functionality for:

- Product search with full-text search, filters, and facets
- Order search by customer, date range, status, and order number
- Customer search by name, email, phone, and membership level
- Fast search response times (< 200ms for 95th percentile)
- Support for typo tolerance and fuzzy matching
- Relevance ranking and scoring
- Autocomplete and search suggestions
- Scalability to handle millions of products and orders

### Business Context

**Business Drivers**:

- Product discovery is critical for conversion (60% of users use search)
- Fast search improves user experience and sales
- Advanced search features (filters, facets) increase engagement
- Support for 100K+ products, 1M+ orders, 100K+ customers
- Expected search traffic: 1000+ searches/second during peak

**Business Constraints**:

- Budget: $1000/month for search infrastructure
- Search response time < 200ms (95th percentile)
- Must support Chinese and English full-text search
- Need real-time or near-real-time indexing
- High availability (99.9% uptime)

### Technical Context

**Current State**:

- PostgreSQL primary database (ADR-001)
- Redis for caching (ADR-004)
- Spring Boot 3.4.5 application
- AWS cloud infrastructure
- 13 bounded contexts with search needs

**Requirements**:

- Full-text search with relevance ranking
- Faceted search (filters by category, price, brand, etc.)
- Autocomplete and suggestions
- Typo tolerance and fuzzy matching
- Multi-language support (Chinese, English)
- Real-time or near-real-time indexing
- Scalability to millions of documents
- High availability with automatic failover

## Decision Drivers

1. **Performance**: Search response time < 200ms (95th percentile)
2. **Features**: Full-text search, facets, autocomplete, fuzzy matching
3. **Scalability**: Handle millions of documents, 1000+ searches/second
4. **Cost**: Within $1000/month budget
5. **Operational Overhead**: Managed service preferred
6. **Integration**: Spring Boot integration
7. **Multi-Language**: Chinese and English support
8. **Real-Time**: Near-real-time indexing (< 1 minute lag)

## Considered Options

### Option 1: PostgreSQL Full-Text Search

**Description**: Use PostgreSQL's built-in full-text search capabilities

**Pros**:

- ✅ No additional infrastructure
- ✅ ACID transactions with search
- ✅ Simple to implement
- ✅ No data synchronization needed
- ✅ Cost-effective ($0 additional)
- ✅ Good for simple searches

**Cons**:

- ❌ Limited full-text search features
- ❌ Poor performance at scale (> 100K documents)
- ❌ No faceted search support
- ❌ Limited relevance ranking
- ❌ No autocomplete support
- ❌ Increases database load
- ❌ Limited multi-language support
- ❌ No distributed search

**Cost**: $0 (included in database)

**Risk**: **High** - Insufficient for requirements

**Performance**: 500-2000ms for complex searches

### Option 2: Elasticsearch (AWS OpenSearch Service)

**Description**: Use Elasticsearch via AWS OpenSearch Service (Elasticsearch-compatible)

**Pros**:

- ✅ Excellent full-text search capabilities
- ✅ Rich faceted search and aggregations
- ✅ Fast search performance (< 50ms)
- ✅ Autocomplete and suggestions
- ✅ Typo tolerance and fuzzy matching
- ✅ Multi-language analyzers
- ✅ Scalable to billions of documents
- ✅ AWS managed service
- ✅ High availability with replicas
- ✅ Large community and ecosystem
- ✅ Spring Data Elasticsearch integration

**Cons**:

- ⚠️ Additional infrastructure cost
- ⚠️ Data synchronization complexity
- ⚠️ Eventual consistency
- ⚠️ Operational overhead (indexing, monitoring)
- ⚠️ Learning curve

**Cost**:

- Development: $800/month (t3.medium.search x 2)
- Production: $1200/month (r6g.large.search x 2)

**Risk**: **Low** - Industry-standard solution

**Performance**: 20-100ms for complex searches

### Option 3: OpenSearch (AWS OpenSearch Service)

**Description**: Use OpenSearch (AWS's open-source fork of Elasticsearch)

**Pros**:

- ✅ All Elasticsearch features (fork from ES 7.10)
- ✅ Excellent full-text search capabilities
- ✅ Rich faceted search and aggregations
- ✅ Fast search performance (< 50ms)
- ✅ AWS managed service
- ✅ Lower cost than Elasticsearch
- ✅ Open-source (Apache 2.0 license)
- ✅ AWS native integration
- ✅ Spring Data Elasticsearch compatible

**Cons**:

- ⚠️ Smaller community than Elasticsearch
- ⚠️ Data synchronization complexity
- ⚠️ Eventual consistency
- ⚠️ Operational overhead

**Cost**:

- Development: $600/month (t3.medium.search x 2)
- Production: $1000/month (r6g.large.search x 2)

**Risk**: **Low** - AWS-backed solution

**Performance**: 20-100ms for complex searches

### Option 4: Hybrid Approach (PostgreSQL + Redis)

**Description**: Use PostgreSQL for simple searches, Redis for autocomplete

**Pros**:

- ✅ Leverages existing infrastructure
- ✅ Cost-effective
- ✅ Simple to implement
- ✅ Good for basic use cases

**Cons**:

- ❌ Limited search features
- ❌ Poor performance at scale
- ❌ No faceted search
- ❌ Complex to maintain
- ❌ Doesn't meet requirements

**Cost**: $0 (existing infrastructure)

**Risk**: **High** - Insufficient for requirements

**Performance**: 200-1000ms for complex searches

## Decision Outcome

**Chosen Option**: **OpenSearch (AWS OpenSearch Service)**

### Rationale

OpenSearch was selected for the following reasons:

1. **Cost-Effective**: Meets requirements within $1000/month budget
2. **Performance**: Sub-100ms search response time
3. **Features**: All required search features (full-text, facets, autocomplete, fuzzy)
4. **AWS Native**: Seamless AWS integration, managed service
5. **Open-Source**: Apache 2.0 license, no vendor lock-in
6. **Scalability**: Handles millions of documents easily
7. **Spring Integration**: Compatible with Spring Data Elasticsearch
8. **Multi-Language**: Excellent Chinese and English support
9. **Proven**: Used by many large-scale e-commerce platforms

**Search Architecture**:

**Primary Use Cases**:

- **Product Search**: Full-text search with facets (category, price, brand, rating)
- **Order Search**: Search by order number, customer, date range, status
- **Customer Search**: Search by name, email, phone (admin only)

**Indexing Strategy**:

- **Real-Time**: Product updates indexed within 1 minute
- **Batch**: Bulk indexing for historical data
- **CDC**: Change Data Capture from PostgreSQL to OpenSearch
- **Event-Driven**: Domain events trigger index updates

**Search Features**:

- Full-text search with relevance ranking
- Faceted search (filters and aggregations)
- Autocomplete and search suggestions
- Typo tolerance (fuzzy matching, edit distance)
- Multi-language analyzers (Chinese, English)
- Highlighting of search terms
- Pagination and sorting

**Why Not PostgreSQL**: Insufficient features, poor performance at scale.

**Why Not Elasticsearch**: Higher cost, licensing concerns (Elastic License).

**Why Not Hybrid**: Doesn't meet requirements, complex to maintain.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn OpenSearch | Training, documentation, examples |
| Operations Team | Medium | Monitor OpenSearch cluster | AWS managed service, runbooks |
| End Users | Positive | Faster, better search experience | N/A |
| Business | Positive | Improved conversion, user satisfaction | N/A |
| Database Team | Low | Reduced database search load | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- Product bounded context (primary)
- Order bounded context (secondary)
- Customer bounded context (admin search)
- Application services (search integration)
- Infrastructure layer (OpenSearch cluster)
- Data synchronization (CDC pipeline)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Data sync lag | Medium | Medium | Monitor lag, alerting, CDC optimization |
| OpenSearch unavailability | Low | High | Fallback to database, circuit breaker |
| Index corruption | Low | Medium | Regular backups, automated recovery |
| Cost overrun | Low | Medium | Monitor usage, set alarms, optimize queries |
| Search relevance issues | Medium | Medium | Tuning, A/B testing, user feedback |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup (Week 1-2)

- [x] Provision OpenSearch cluster (r6g.large.search x 2)
- [x] Configure security (VPC, security groups, IAM)
- [x] Set up monitoring and alerting
- [x] Create index templates and mappings
- [x] Configure analyzers (Chinese, English)

### Phase 2: Product Search (Week 3-4)

- [x] Implement product index mapping
- [x] Bulk index existing products
- [x] Implement CDC for product updates
- [x] Create product search API
- [x] Implement faceted search
- [x] Add autocomplete

### Phase 3: Order & Customer Search (Week 5-6)

- [x] Implement order index mapping
- [x] Bulk index existing orders
- [x] Implement CDC for order updates
- [x] Create order search API
- [x] Implement customer search (admin)
- [x] Add search suggestions

### Phase 4: Optimization (Week 7-8)

- [x] Performance tuning and optimization
- [x] Relevance tuning (scoring, boosting)
- [x] Load testing (1000+ searches/second)
- [x] Implement search analytics
- [x] Documentation and training

### Rollback Strategy

**Trigger Conditions**:

- Search response time > 500ms
- OpenSearch availability < 99%
- Data sync lag > 5 minutes
- Cost exceeds budget by > 50%

**Rollback Steps**:

1. Disable OpenSearch search
2. Fall back to PostgreSQL simple search
3. Investigate and fix issues
4. Re-enable OpenSearch gradually
5. Monitor performance

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ Search response time < 200ms (95th percentile)
- ✅ Search availability > 99.9%
- ✅ Data sync lag < 1 minute
- ✅ Search relevance score > 80% (user feedback)
- ✅ Cost within budget ($1000/month)
- ✅ Zero data loss during sync

### Monitoring Plan

**CloudWatch Metrics**:

- SearchRate (searches per second)
- SearchLatency (p50, p95, p99)
- IndexingRate (documents per second)
- IndexingLatency
- ClusterStatus (green, yellow, red)
- CPUUtilization
- JVMMemoryPressure
- DiskQueueDepth

**Application Metrics**:

```java
@Component
public class SearchMetrics {
    private final Timer searchTime;
    private final Counter searchRequests;
    private final Counter searchErrors;
    private final Gauge indexLag;
    
    // Track search performance
}
```

**Alerts**:

- Search latency > 500ms
- Cluster status not green
- CPU utilization > 80%
- JVM memory pressure > 80%
- Index lag > 5 minutes
- Search error rate > 1%

**Review Schedule**:

- Daily: Check search metrics
- Weekly: Review search relevance
- Monthly: Optimize search performance
- Quarterly: Search strategy review

## Consequences

### Positive Consequences

- ✅ **Performance**: Sub-200ms search response time
- ✅ **Features**: Rich search capabilities (facets, autocomplete, fuzzy)
- ✅ **Scalability**: Handles millions of documents
- ✅ **User Experience**: Fast, relevant search results
- ✅ **Conversion**: Improved product discovery
- ✅ **Database Health**: Reduced database search load
- ✅ **Flexibility**: Easy to add new search features

### Negative Consequences

- ⚠️ **Complexity**: Additional infrastructure to manage
- ⚠️ **Cost**: $1000/month additional cost
- ⚠️ **Consistency**: Eventual consistency (1-minute lag)
- ⚠️ **Synchronization**: Need to keep data in sync
- ⚠️ **Operational Overhead**: Monitoring, tuning, maintenance

### Technical Debt

**Identified Debt**:

1. Manual relevance tuning (can be automated with ML)
2. Simple CDC (can use Debezium for robust CDC)
3. No search analytics (future enhancement)

**Debt Repayment Plan**:

- **Q2 2026**: Implement Debezium for robust CDC
- **Q3 2026**: Add ML-based relevance tuning
- **Q4 2026**: Implement search analytics and A/B testing

## Related Decisions

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Source of truth for data
- [ADR-004: Use Redis for Distributed Caching](004-use-redis-for-distributed-caching.md) - Cache search results

## Notes

### OpenSearch Index Mapping

```json
{
  "mappings": {
    "properties": {
      "productId": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart",
        "fields": {
          "keyword": { "type": "keyword" },
          "english": {
            "type": "text",
            "analyzer": "english"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "category": { "type": "keyword" },
      "brand": { "type": "keyword" },
      "price": { "type": "double" },
      "rating": { "type": "float" },
      "reviewCount": { "type": "integer" },
      "inStock": { "type": "boolean" },
      "tags": { "type": "keyword" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
```

### Search Query Example

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    public SearchPage<Product> searchProducts(ProductSearchRequest request) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        
        // Full-text search
        if (StringUtils.hasText(request.getQuery())) {
            queryBuilder.withQuery(
                QueryBuilders.multiMatchQuery(request.getQuery())
                    .field("name", 3.0f)  // Boost name
                    .field("description", 1.0f)
                    .field("tags", 2.0f)
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2)
            );
        }
        
        // Filters
        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
        
        if (request.getCategory() != null) {
            filterQuery.filter(QueryBuilders.termQuery("category", request.getCategory()));
        }
        
        if (request.getBrand() != null) {
            filterQuery.filter(QueryBuilders.termQuery("brand", request.getBrand()));
        }
        
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
            if (request.getMinPrice() != null) {
                priceRange.gte(request.getMinPrice());
            }
            if (request.getMaxPrice() != null) {
                priceRange.lte(request.getMaxPrice());
            }
            filterQuery.filter(priceRange);
        }
        
        if (request.isInStockOnly()) {
            filterQuery.filter(QueryBuilders.termQuery("inStock", true));
        }
        
        queryBuilder.withFilter(filterQuery);
        
        // Facets (Aggregations)
        queryBuilder.addAggregation(
            AggregationBuilders.terms("categories").field("category").size(20)
        );
        queryBuilder.addAggregation(
            AggregationBuilders.terms("brands").field("brand").size(20)
        );
        queryBuilder.addAggregation(
            AggregationBuilders.range("priceRanges").field("price")
                .addRange(0, 100)
                .addRange(100, 500)
                .addRange(500, 1000)
                .addRange(1000, Double.MAX_VALUE)
        );
        
        // Sorting
        if (request.getSortBy() != null) {
            SortOrder order = request.getSortOrder() == SortOrder.DESC ? 
                SortOrder.DESC : SortOrder.ASC;
            queryBuilder.withSort(SortBuilders.fieldSort(request.getSortBy()).order(order));
        }
        
        // Pagination
        queryBuilder.withPageable(
            PageRequest.of(request.getPage(), request.getSize())
        );
        
        // Highlighting
        queryBuilder.withHighlightFields(
            new HighlightBuilder.Field("name"),
            new HighlightBuilder.Field("description")
        );
        
        return elasticsearchOperations.searchForPage(
            queryBuilder.build(), 
            Product.class
        );
    }
}
```

### CDC Implementation

```java
@Component
public class ProductEventHandler {
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductCreated(ProductCreatedEvent event) {
        Product product = convertToSearchDocument(event);
        elasticsearchOperations.save(product);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUpdated(ProductUpdatedEvent event) {
        Product product = convertToSearchDocument(event);
        elasticsearchOperations.save(product);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDeleted(ProductDeletedEvent event) {
        elasticsearchOperations.delete(event.getProductId(), Product.class);
    }
}
```

### OpenSearch Configuration

```yaml
# AWS OpenSearch Service Configuration
Domain: ecommerce-search-prod
Version: OpenSearch 2.11
Instance Type: r6g.large.search
Instance Count: 2
Dedicated Master: Enabled (3 x r6g.large.search)
EBS Volume: 100 GB GP3 per node
Multi-AZ: Enabled
Encryption: At rest and in transit
VPC: Private subnets
Security: IAM + VPC security groups
Backup: Automated daily snapshots, 7-day retention
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
