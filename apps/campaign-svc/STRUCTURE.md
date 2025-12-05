# Campaign Service Structure

This document explains the organization of the Campaign Service codebase.

## Package Structure

```
com.outreach.campaign/
├── api/                          # REST API Layer
│   └── CampaignController.java   # HTTP endpoints for campaigns
│
├── application/                  # Application Services (Business Logic)
│   ├── CampaignService.java      # Campaign management logic
│   ├── CsvParserService.java     # CSV file parsing
│   ├── LeadImportService.java    # Lead import to database
│   └── UserContextService.java   # User context resolution
│
├── domain/                       # Domain Layer
│   ├── entities/                 # JPA Database Entities
│   │   ├── CampaignEntity.java
│   │   ├── CampaignLeadEntity.java
│   │   ├── CampaignStepEntity.java
│   │   ├── EmailEventEntity.java
│   │   ├── ICPProfileEntity.java
│   │   ├── LeadBatchEntity.java
│   │   ├── LeadEntity.java
│   │   ├── MailboxEntity.java
│   │   ├── SenderCompanyEntity.java
│   │   ├── SentEmailEntity.java
│   │   └── UserSummary.java
│   │
│   └── models/                   # Domain Models / DTOs
│       ├── Campaign.java         # Campaign domain model
│       └── Lead.java             # Lead domain model
│
└── infrastructure/               # Infrastructure Layer
    ├── CampaignRepository.java
    ├── CampaignLeadRepository.java
    ├── CampaignStepRepository.java
    ├── EmailEventRepository.java
    ├── ICPProfileRepository.java
    ├── LeadBatchRepository.java
    ├── LeadRepository.java
    ├── MailboxRepository.java
    ├── SenderCompanyRepository.java
    ├── SentEmailRepository.java
    ├── UserRepository.java
    └── CorsConfig.java           # CORS configuration
```

## Layer Responsibilities

### `api/` - REST API Layer
- **Purpose**: Handles HTTP requests and responses
- **Responsibilities**:
  - Request validation
  - Response formatting
  - HTTP status code management
  - Delegates business logic to application services

### `application/` - Application Services
- **Purpose**: Contains business logic and orchestration
- **Responsibilities**:
  - Business rule enforcement
  - Transaction management
  - Service coordination
  - Domain model conversion

### `domain/entities/` - Database Entities
- **Purpose**: JPA entities that map directly to database tables
- **Responsibilities**:
  - Database persistence
  - Table relationships
  - Column mappings
  - Entity lifecycle management

### `domain/models/` - Domain Models
- **Purpose**: Pure domain objects (DTOs, Value Objects)
- **Responsibilities**:
  - Business domain representation
  - API response/request models
  - Immutable data structures (records)
  - No database dependencies

### `infrastructure/` - Infrastructure Layer
- **Purpose**: Technical concerns and external integrations
- **Responsibilities**:
  - Data access (repositories)
  - Database queries
  - External service clients
  - Configuration

## Design Principles

1. **Separation of Concerns**: Each layer has a clear, single responsibility
2. **Dependency Direction**: 
   - `api` → `application` → `domain` → `infrastructure`
   - Lower layers don't depend on higher layers
3. **Entity vs Model**: 
   - Entities are for database persistence
   - Models are for business logic and API contracts
4. **Repository Pattern**: All database access goes through repositories

## Common Patterns

- **Service Layer**: Application services orchestrate domain logic
- **Repository Pattern**: Data access is abstracted through repositories
- **Entity Mapping**: Entities are converted to models for API responses
- **User Context**: User identification is handled at the API layer and passed down

