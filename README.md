# Digital Wastina & Trust Protocol

A secure, scalable trust platform that digitizes the traditional Ethiopian guarantor ("Wastina") process.

## Team
- Ephrem Tesfaye
- Abdulihakim Jejaw
- Rihad Gali

## Overview

The Digital Wastina & Trust Protocol enables users to act as applicants, guarantors, employers, landlords, or administrators within a digital trust ecosystem. The system maintains employment history, tracks reputation through trust scores, manages digital guarantees, and handles complaints and disputes while respecting Ethiopian cultural context.

## Technology Stack

- **Framework**: Spring Boot 3.4.10
- **Language**: Java 21
- **Database**: PostgreSQL
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Code Generation**: Lombok
- **Testing**: JUnit 5, jqwik (Property-Based Testing), Testcontainers

## Ethiopian Context

- **National ID**: Fayda ID integration (encrypted storage)
- **Payment**: Telebirr, CBE Birr, Chapa, Bank Transfer support
- **Currency**: Ethiopian Birr (ETB)
- **Language**: UTF-8 support for Amharic text
- **Timezone**: Africa/Addis_Ababa
- **Phone Format**: +251 prefix validation

## Database Model

### Core Entities

1. **User** - Central identity for all platform participants
   - UUID primary key
   - Phone number (+251 format), Fayda ID (encrypted), password hash
   - Trust score (0-100), verification status, soft delete support
   - Multiple roles support

2. **Role** - Permission management
   - APPLICANT, GUARANTOR, EMPLOYER, LANDLORD, ADMIN, SUPER_ADMIN

3. **Profession** - Bilingual occupation classification
   - English and Amharic names
   - Sector: PUBLIC, PRIVATE, NGO, SELF_EMPLOYED
   - Stability weight for trust scoring

4. **Location** - Ethiopian administrative hierarchy
   - Region, City, Woreda, Kebele

5. **Guarantee** - Digital Wastina contract
   - Links applicant to guarantor
   - Liability amount (ETB), dates, status, purpose
   - Digital signature hash (SHA-256)

6. **EmploymentRecord** - Work history for reputation
   - Employee-employer relationship
   - Rating (1-5), comments, verification status

7. **TrustScoreHistory** - Immutable audit trail
   - Tracks all trust score changes with reasons

8. **Complaint** - Negative feedback mechanism
   - Categories: THEFT, ABSENTEEISM, FRAUD, VIOLENCE, OTHER
   - Evidence document support

9. **DisputeCase** - Appeal process
   - One-to-one with Complaint
   - Admin assignment and resolution

10. **Document** - File metadata with encryption
    - Types: FAYDA_ID, CONTRACT, EVIDENCE, PHOTO, OTHER
    - KMS key reference for encrypted files

11. **Transaction** - Payment records
    - Payment methods: TELEBIRR, CBE_BIRR, CHAPA, BANK_TRANSFER
    - External reference ID tracking

12. **AuditLog** - Immutable security logging
    - Actor, action type, target user
    - IP address and device info

## Entity Conventions

- **Primary Keys**: UUID for domain entities, Long for reference data
- **Column Naming**: snake_case (e.g., `full_name`, `phone_number`)
- **Field Naming**: camelCase (e.g., `fullName`, `phoneNumber`)
- **Timestamps**: Automatic via `@CreatedDate`, `@LastModifiedDate`
- **Validation**: Jakarta Bean Validation annotations
- **Security**: Sensitive fields excluded from `toString()`

## Cascade Rules

- **User → Document**: CASCADE DELETE (documents belong to user)
- **User → Guarantee**: NO CASCADE (preserve historical records)
- **User → EmploymentRecord**: NO CASCADE (preserve work history)
- **User → Complaint**: NO CASCADE (preserve complaint records)
- **Complaint → DisputeCase**: CASCADE DELETE (dispute tied to complaint)

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+

### Database Setup

```sql
CREATE DATABASE wastina_trust_db;
CREATE USER wastina_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE wastina_trust_db TO wastina_user;
```

### Configuration

Update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wastina_trust_db
spring.datasource.username=wastina_user
spring.datasource.password=your_secure_password
```

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

### Run Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/wastina/trust/
│   │       ├── controller/          # REST Controllers
│   │       │   ├── UserController.java
│   │       │   ├── GuaranteeController.java
│   │       │   ├── ComplaintController.java
│   │       │   ├── EmploymentRecordController.java
│   │       │   ├── TransactionController.java
│   │       │   ├── DisputeCaseController.java
│   │       │   └── TrustScoreHistoryController.java
│   │       ├── dto/                 # Data Transfer Objects
│   │       │   ├── request/
│   │       │   │   ├── UserRegistrationRequest.java
│   │       │   │   ├── UserUpdateRequest.java
│   │       │   │   ├── GuaranteeCreateRequest.java
│   │       │   │   ├── ComplaintCreateRequest.java
│   │       │   │   ├── EmploymentRecordCreateRequest.java
│   │       │   │   ├── TransactionCreateRequest.java
│   │       │   │   └── DisputeCaseCreateRequest.java
│   │       │   └── response/
│   │       │       ├── UserResponse.java
│   │       │       ├── GuaranteeResponse.java
│   │       │       ├── ComplaintResponse.java
│   │       │       ├── EmploymentRecordResponse.java
│   │       │       ├── TransactionResponse.java
│   │       │       ├── DisputeCaseResponse.java
│   │       │       ├── TrustScoreHistoryResponse.java
│   │       │       ├── RoleResponse.java
│   │       │       ├── ProfessionResponse.java
│   │       │       ├── LocationResponse.java
│   │       │       ├── GuaranteePurposeResponse.java
│   │       │       ├── ComplaintCategoryResponse.java
│   │       │       └── DocumentResponse.java
│   │       ├── exception/           # Exception Handling
│   │       │   ├── ResourceNotFoundException.java
│   │       │   ├── DuplicateResourceException.java
│   │       │   ├── ErrorResponse.java
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── mapper/              # Entity-DTO Mappers
│   │       │   ├── UserMapper.java
│   │       │   ├── GuaranteeMapper.java
│   │       │   ├── ComplaintMapper.java
│   │       │   ├── EmploymentRecordMapper.java
│   │       │   ├── TransactionMapper.java
│   │       │   ├── DisputeCaseMapper.java
│   │       │   ├── TrustScoreHistoryMapper.java
│   │       │   ├── RoleMapper.java
│   │       │   ├── ProfessionMapper.java
│   │       │   ├── LocationMapper.java
│   │       │   ├── GuaranteePurposeMapper.java
│   │       │   ├── ComplaintCategoryMapper.java
│   │       │   └── DocumentMapper.java
│   │       ├── model/               # JPA Entities
│   │       │   ├── Models/
│   │       │   │   ├── User.java
│   │       │   │   ├── Role.java
│   │       │   │   ├── Profession.java
│   │       │   │   ├── Location.java
│   │       │   │   ├── Guarantee.java
│   │       │   │   ├── EmploymentRecord.java
│   │       │   │   ├── TrustScoreHistory.java
│   │       │   │   ├── Complaint.java
│   │       │   │   ├── DisputeCase.java
│   │       │   │   ├── Document.java
│   │       │   │   ├── Transaction.java
│   │       │   │   ├── AuditLog.java
│   │       │   │   └── BaseAuditEntity.java
│   │       │   ├── GuaranteePurpose.java
│   │       │   ├── ComplaintCategory.java
│   │       │   └── enums/
│   │       │       ├── RoleType.java
│   │       │       ├── VerificationStatus.java
│   │       │       ├── ProfessionSector.java
│   │       │       ├── GuaranteeStatus.java
│   │       │       ├── EmploymentVerificationStatus.java
│   │       │       ├── ComplaintStatus.java
│   │       │       ├── DisputeDecision.java
│   │       │       ├── DocumentType.java
│   │       │       ├── PaymentMethod.java
│   │       │       └── TransactionStatus.java
│   │       ├── repository/          # JPA Repositories
│   │       │   ├── UserRepository.java
│   │       │   ├── RoleRepository.java
│   │       │   ├── ProfessionRepository.java
│   │       │   ├── LocationRepository.java
│   │       │   ├── GuaranteeRepository.java
│   │       │   ├── GuaranteePurposeRepository.java
│   │       │   ├── ComplaintRepository.java
│   │       │   ├── ComplaintCategoryRepository.java
│   │       │   ├── EmploymentRecordRepository.java
│   │       │   ├── TransactionRepository.java
│   │       │   ├── DisputeCaseRepository.java
│   │       │   ├── TrustScoreHistoryRepository.java
│   │       │   ├── DocumentRepository.java
│   │       │   └── AuditLogRepository.java
│   │       ├── service/             # Service Interfaces
│   │       │   ├── UserService.java
│   │       │   ├── GuaranteeService.java
│   │       │   ├── ComplaintService.java
│   │       │   ├── EmploymentRecordService.java
│   │       │   ├── TransactionService.java
│   │       │   ├── DisputeCaseService.java
│   │       │   └── TrustScoreHistoryService.java
│   │       ├── service/impl/        # Service Implementations
│   │       │   ├── UserServiceImpl.java
│   │       │   ├── GuaranteeServiceImpl.java
│   │       │   ├── ComplaintServiceImpl.java
│   │       │   ├── EmploymentRecordServiceImpl.java
│   │       │   ├── TransactionServiceImpl.java
│   │       │   ├── DisputeCaseServiceImpl.java
│   │       │   └── TrustScoreHistoryServiceImpl.java
│   │       └── DigitalWastinaTrustProtocolApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
        └── com/wastina/trust/
            └── (test files)
```

## Entity Relationship Diagram

```
USER (Central Hub)
├── Many-to-Many ──> ROLE
├── Many-to-One ──> PROFESSION
├── Many-to-One ──> LOCATION
├── One-to-Many ──> DOCUMENT (owner) [CASCADE DELETE]
├── One-to-Many ──> TRUST_SCORE_HISTORY [IMMUTABLE]
├── One-to-Many ──> AUDIT_LOG (actor) [IMMUTABLE]
├── One-to-Many ──> GUARANTEE (as applicant)
├── One-to-Many ──> GUARANTEE (as guarantor)
├── One-to-Many ──> EMPLOYMENT_RECORD (as employee)
├── One-to-Many ──> EMPLOYMENT_RECORD (as employer)
├── One-to-Many ──> COMPLAINT (as reporter)
├── One-to-Many ──> COMPLAINT (as reported)
└── One-to-Many ──> TRANSACTION (as payer)

COMPLAINT
├── One-to-One ──> DISPUTE_CASE [CASCADE DELETE]
└── Many-to-One ──> DOCUMENT (evidence)
```

## Security Features

- **Password Hashing**: BCrypt with work factor 12
- **Fayda ID Encryption**: AES encryption (KMS integration ready)
- **Digital Signatures**: SHA-256 hashing for guarantees
- **Audit Logging**: Immutable logs for all critical actions
- **Soft Deletion**: Users marked inactive instead of deleted

## Next Steps

1. **Security Layer**
   - Implement Spring Security
   - Add JWT authentication
   - Implement BCrypt password hashing
   - Add Fayda ID encryption service

2. **Payment Integration**
   - Integrate Telebirr API
   - Integrate Chapa payment gateway
   - Implement transaction processing

3. **File Management**
   - Implement document upload service
   - Add S3 or local file storage
   - Implement file encryption

4. **Testing**
   - Write unit tests for services
   - Write integration tests for controllers
   - Add property-based tests with jqwik

5. **Documentation**
   - Add Swagger/OpenAPI specification
   - Create deployment guide

## API Endpoints

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for complete API reference.

### Quick Reference

**User Management:**
- `POST /api/v1/users/register` - Register new user
- `GET /api/v1/users/{id}` - Get user by ID
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user (soft delete)

**Guarantee Management:**
- `POST /api/v1/guarantees` - Create guarantee
- `GET /api/v1/guarantees/{id}` - Get guarantee by ID
- `GET /api/v1/guarantees/applicant/{id}` - Get guarantees by applicant
- `POST /api/v1/guarantees/{id}/approve` - Approve guarantee

**Complaint Management:**
- `POST /api/v1/complaints` - File complaint
- `GET /api/v1/complaints/{id}` - Get complaint by ID
- `GET /api/v1/complaints/status/{status}` - Get complaints by status
- `PATCH /api/v1/complaints/{id}/status` - Update complaint status

**Employment Record Management:**
- `POST /api/v1/employment-records` - Create employment record
- `GET /api/v1/employment-records/{id}` - Get employment record by ID
- `GET /api/v1/employment-records/employee/{id}` - Get records by employee
- `PUT /api/v1/employment-records/{id}/verify` - Verify employment record

**Transaction Management:**
- `POST /api/v1/transactions` - Create transaction
- `GET /api/v1/transactions/{id}` - Get transaction by ID
- `GET /api/v1/transactions/payer/{id}` - Get transactions by payer
- `PUT /api/v1/transactions/{id}/status` - Update transaction status

**Dispute Case Management:**
- `POST /api/v1/dispute-cases` - Create dispute case
- `GET /api/v1/dispute-cases/{id}` - Get dispute case by ID
- `GET /api/v1/dispute-cases/complaint/{id}` - Get dispute by complaint
- `PUT /api/v1/dispute-cases/{id}/resolve` - Resolve dispute case

**Trust Score History:**
- `GET /api/v1/trust-score-history/user/{id}` - Get trust score history by user

## Complete System Workflow

See [SYSTEM_WORKFLOW_EXAMPLE.md](SYSTEM_WORKFLOW_EXAMPLE.md) for a comprehensive example demonstrating the complete workflow using Mr. X (Applicant), Mr. Y (Guarantor), and Mr. Z (Employer).

## Quick Start

See [QUICK_START.md](QUICK_START.md) for a 5-minute setup guide.

## Setup Guide

See [SETUP_GUIDE.md](SETUP_GUIDE.md) for detailed setup instructions for both backend and frontend.

## Implementation Summary

See [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) for a complete overview of what has been implemented.

## Admin Account

The system automatically seeds an admin account on first run:
- **Name**: Ephrem Tesfaye
- **Phone**: +251911000000
- **Password**: admin123
- **Email**: ephrem.tesfaye@wastina.com
- **Role**: ADMIN
- **Trust Score**: 100

## Frontend

A React TypeScript frontend with Ethiopian-themed UI is available in the `frontend/` directory.

Features:
- Ethiopian flag colors (Green, Yellow, Red, Blue)
- Role-based routing and access control
- Admin dashboard with statistics
- User management interface
- Responsive design with Tailwind CSS

To run the frontend:
```bash
cd frontend
npm install
npm run dev
```

Access at `http://localhost:3000`

## License

Proprietary - All rights reserved

## Contact

For questions or support, contact the development team.
