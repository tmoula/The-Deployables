# Unit Testing Guide - Lead Service

## Overview

This service uses JUnit 5 and Mockito for unit testing, with JaCoCo for code coverage reporting. The goal is to maintain **80% code coverage**.

## Running Tests

### Run all tests:
```bash
./gradlew test
```

### Run tests with coverage report:
```bash
./gradlew testWithCoverage
```

This will:
1. Run all tests
2. Generate coverage report
3. Verify coverage meets 80% threshold

### View coverage report:
```bash
# HTML report
open build/reports/jacoco/test/html/index.html

# XML report
cat build/reports/jacoco/test/jacocoTestReport.xml
```

## Test Structure

Tests are organized to match the source structure:

```
src/test/java/com/outreach/lead/
├── api/
│   ├── MatchControllerTest.java
│   └── PublicApiControllerTest.java
├── application/
│   ├── MatchServiceTest.java
│   └── UserContextServiceTest.java
├── domain/
│   └── DomainModelTest.java
└── infrastructure/
    ├── AiServiceClientTest.java
    ├── ProspectServiceTest.java
    └── PublicApiClientTest.java
```

## Test Coverage

### Currently Tested Components:

1. **PublicApiController** - REST endpoints for public API integration
2. **PublicApiClient** - Client for fetching data from external APIs
3. **AiServiceClient** - Client for AI service integration
4. **ProspectService** - Service for prospect operations
5. **UserContextService** - Service for user context management
6. **MatchController** - REST endpoints for matching prospects
7. **MatchService** - Service for prospect matching
8. **Domain Models** - Prospect, SellerProfile, ProspectCriteria

## Writing New Tests

### Example Test Structure:

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private MyService myService;
    
    @BeforeEach
    void setUp() {
        // Setup test data
    }
    
    @Test
    void testMethod_WhenCondition_ReturnsExpected() {
        // Given
        when(dependency.method()).thenReturn(value);
        
        // When
        Result result = myService.method();
        
        // Then
        assertNotNull(result);
        assertEquals(expected, result);
        verify(dependency, times(1)).method();
    }
}
```

## Coverage Requirements

- **Minimum Coverage**: 80%
- **Coverage is verified automatically** when running `testWithCoverage`
- Coverage includes:
  - Line coverage
  - Branch coverage
  - Method coverage

## Best Practices

1. **Test all public methods** of services and controllers
2. **Test both success and failure paths**
3. **Use meaningful test names** that describe what is being tested
4. **Mock external dependencies** (databases, APIs, etc.)
5. **Test edge cases** (null values, empty collections, etc.)
6. **Verify interactions** with mocked dependencies when appropriate
7. **Keep tests focused** - one assertion per test when possible

## Common Patterns

### Testing Controllers:
```java
@WebMvcTest(MyController.class)
class MyControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MyService myService;
    
    @Test
    void testEndpoint() throws Exception {
        when(myService.method()).thenReturn(result);
        
        mockMvc.perform(get("/api/v1/endpoint"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.field").value("value"));
    }
}
```

### Testing Services with Dependencies:
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void testMethod() {
        when(dependency.call()).thenReturn(value);
        Result result = service.method();
        assertNotNull(result);
    }
}
```

### Testing Exception Handling:
```java
@Test
void testMethod_WhenError_ThrowsException() {
    when(dependency.method()).thenThrow(new RuntimeException("Error"));
    
    assertThrows(RuntimeException.class, () -> {
        service.method();
    });
}
```

## Troubleshooting

### Tests failing with "No tests found":
- Ensure test classes are in `src/test/java`
- Test class names must end with `Test`
- Tests must be annotated with `@Test`

### Coverage not meeting threshold:
- Run `./gradlew testWithCoverage` to see detailed coverage report
- Check which classes/methods are not covered
- Add tests for uncovered code

### Mockito injection not working:
- Ensure `@ExtendWith(MockitoExtension.class)` is present
- Use `@InjectMocks` for the class under test
- Use `@Mock` for dependencies

## Continuous Integration

Tests and coverage verification run automatically in CI/CD pipeline. The build will fail if:
- Tests fail
- Coverage is below 80%

