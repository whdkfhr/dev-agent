# DESIGN-004

## Overview
Health Check 엔드포인트를 구현한다.
응답은 항상 200 OK이며 {"status": "UP"}을 반환한다.

## Architecture Overview

Client → GET /health → HealthController → HealthResponse("UP") → 200 OK

## API Design

### GET /health

Response 200:
```json
{"status": "UP"}
```

## Data Model

### HealthResponse (DTO Record)

| Field  | Type   | Description |
|--------|--------|-------------|
| status | String | 항상 "UP"   |

## Package Structure

com.arok2.dev_agent
└── health/
    ├── controller/
    │   └── HealthController.java
    └── dto/
        └── HealthResponse.java

## Key Design Decisions
- HealthResponse를 Java Record로 정의 (불변성 보장)
- 별도 Service 레이어 없음 (비즈니스 로직 없으므로)

## Implementation Guide

1. HealthResponse.java — DTO Record 생성
2. HealthController.java — GET /health 엔드포인트
3. HealthControllerTest.java — MockMvc 통합 테스트
