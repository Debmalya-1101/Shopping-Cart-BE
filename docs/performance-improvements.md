# Backend Performance Improvements (TiDB Optimization)

This document tracks the backend architectural and configuration changes made to optimize the Spring Boot application for a distributed cloud database (TiDB).

## 1. ORM & Query Optimizations
- **Hibernate Batch Fetching**: Added spring.jpa.properties.hibernate.default_batch_fetch_size=50. This prevents severe N+1 query explosions during lazy loading by fetching related entities in batches using an IN (...) clause.
- **Cart Retrieval @EntityGraph**: Updated CartRepository.findByUser to use @EntityGraph(attributePaths = {"items", "items.product"}). This condenses cart loading from N+1 queries into a single JOIN query, drastically dropping the "Add to Cart" latency.
- **Dedicated Distinct Queries**: Replaced massive over-fetching endpoints with lightweight, dedicated queries.
  - Added indDistinctCategoryNames() native query mapping to GET /api/products/categories.
  - Added indDistinctBrands() native query mapping to GET /api/products/brands.
  - *Impact*: Reduced JSON payloads from ~50KB to ~0.5KB and pushed the DISTINCT filtering down to the SQL engine rather than relying on the frontend.

## 2. Transaction Management
- **@Transactional on Cart Updates**: Applied @Transactional to CartServiceImpl methods like ddToCart. This ensures that intermediate SELECT and UPDATE statements occur within a single database connection lifecycle and allows the JDBC driver to bundle network trips.

## 3. Database Connection Pooling (HikariCP)
- **Prepared Statement Caching**: Added spring.datasource.hikari.data-source-properties.cachePrepStmts=true and prepStmtCacheSize=250 to the pplication.properties. This avoids recompiling execution plans in TiDB for recurring queries.
- **JDBC Batching Flags**: Added ewriteBatchedStatements=true and order_inserts=true, order_updates=true to the MySQL connection string. This bundles multiple DML operations into a single network packet, mitigating cloud latency.
