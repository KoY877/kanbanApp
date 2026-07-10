# Project Conventions

## Architecture Rules
- Service layer never returns ResponseEntity — that belongs to Controller only
- Never expose Entity directly in API response — always use a DTO/record
- Never put @Slf4j on Entity classes
- DTO must be Java records, not classes with Lombok

## Security Rules
- Never log PII (email, name, password) — log only id
- Password never appears in any Response DTO
- Role is never settable by the client on registration — always defaults to USER
- createUser DTO must NOT contain a role field

## Naming Conventions
- Table names lowercase: "users", "tickets", "comments"
- Java fields camelCase: assignedTo, createdBy (never assigned_to)
- Request DTOs: UserCreateRequest, UserUpdateRequest, TicketCreateRequest
- Response DTOs: UserResponse, TicketResponse

## Exception Handling
- Always throw specific exceptions: ResourceNotFoundException, 
  EmailAlreadyExistsException, InvalidTransitionException
- Never catch an exception with an empty block
- GlobalExceptionHandler handles all exceptions via @RestControllerAdvice

## State Machine
- Status transitions validated only in TicketStatus enum via canTransitionTo()
- Status never set directly from TicketCreateRequest or TicketUpdateRequest
- Status change only via PATCH /tickets/{id}/status

## Testing Rules
- Every Service method must have a JUnit 5 test
- Use Mockito — never call real repository in unit tests
- Test both happy path AND error case for every method

## Documentation Rules
- Every method must have a Javadoc block /** */ describing what it does,
  its @param, @return and @throws (including overridden methods, e.g. UserDetails)
- Comment significant lines inside a method body with // explaining intent
- Keep comments in English

## Secrets & Sensitive Data
- Never print, log, cat, or output the contents of .env files in terminal responses
- Never include actual secret values (JWT_SECRET, DB_PASSWORD) in commit messages, code comments, or documentation
- When referencing environment variables, use the variable name only (e.g. "JWT_SECRET is configured") — never the actual value
- If a secret is accidentally exposed (terminal output, screenshot, commit), regenerate it immediately and treat the old value as compromised