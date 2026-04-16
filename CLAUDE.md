# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build project
mvn clean compile

# Run application
mvn spring-boot:run

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ClassName

# Package
mvn clean package -DskipTests
```

## Architecture

Spring Boot 3.4.3 application integrating with OpenAI-compatible APIs (configured for MiniMax). Uses Spring AI 1.0.0-M4.

**Layer structure:**
- `controller/ChatController` - REST endpoints at `/api/chat`
- `service/ChatService` - AI interaction via Spring AI ChatClient
- `dto/` - Request/Response POJOs
- `config/WebConfig` - UTF-8 encoding for streaming responses

**Key endpoints:**
- `POST /api/chat` - Full response with metadata
- `GET /api/chat?message=...` - Simple string response
- `GET /api/chat/stream?message=...` - SSE streaming response

**Streaming implementation:** Uses custom WebClient instead of Spring AI ChatClient for streaming due to MiniMax API compatibility issues. See `ChatService.chatStream()`.

**External dependencies:**
- OpenAI-compatible API at configurable `spring.ai.openai.base-url`
- Redis for data caching/storage

## Configuration

Application runs on port 8099. Key configs in `application.yml`:
- `spring.ai.openai.*` - API connection settings
- `spring.data.redis.*` - Redis connection settings

## Coding Standards

**Alibaba Java Coding Guidelines:**
- All code must follow the Alibaba Java Coding Guidelines (阿里巴巴Java开发手册)
- Use meaningful variable and method names
- Follow proper naming conventions (camelCase for methods/variables, PascalCase for classes)
- Avoid magic numbers, use constants instead

**Documentation Requirements:**
- Every method must have Javadoc comments explaining its purpose, parameters, and return value
- All code inside methods should have inline comments explaining the logic
- Use `@param`, `@return`, `@throws` annotations in Javadoc where applicable