# Team handover

This branch starts from `ef5d7a9`, including the merged login (#24) and Admin (#34) work. The CA requirements decide behaviour; Suria's `SA63/demo` is the main reference for MVC, form validation and service transactions.

The implementation is being integrated in small commits on this branch. The sections below describe completed changes; the final PR will include the remaining modules and acceptance results.

## Login — Junie

- Kept the two login entry points, shared `UserService`, `session.user` and the authentication interceptor.
- Moved the interceptor/configuration from `controller/SecurityForAuthentication` to `config`. It now covers the existing legacy page URLs too.
- A Manager can open Staff pages because `Manager extends Staff`. A valid account with the wrong role gets HTTP 403; an anonymous visitor goes to login.
- The saved account determines the employee role, so the form only asks for username and password. A successful login renews the session ID.
- `UserService.currentUser` reloads the account, rather than trusting a deleted account left in the session. The old application pages now read the same session key.
- Both login forms use `fragments/login.html` and the existing `cats.css`. No new UI framework was added. An accidentally repeated copy of `DESIGN.md` was removed.

Read: `fragments/login.html` → `UserController` → `UserService` → `UserRepo`. For page permissions, read `config/WebConfig` → `config/AuthInterceptor`.

Check: `LoginFlowTest` uses the real Spring context, repository and templates to test the Manager's two workspaces, separate Admin login, role boundaries and context-path redirects. `UserServiceTest` checks credentials, deleted sessions and inheritance.

## Email — Owen

- Kept `AdminEmailService`, its form and the manual send page from #34.
- Made the SMTP dependency optional at startup. An unconfigured send reports an error instead of preventing the whole application from starting.
- Automatic submission and decision notifications are part of the agreed scope and will be integrated with those use cases. They must include the reason and login link where applicable, and must not roll back a saved business decision if SMTP fails.

`AdminEmailServiceTest` still checks the exact recipient, subject and body. Tests use a mock sender and do not email team members.

## Development and deployment

Docker, isolated development/test/deployment databases, repeatable demo data and backup/restore are team decisions. They remain in scope even though the lecturer does not require Docker. They support the classroom application without adding a new Java framework.

The old `imran/final-audit` branch remains a reference for verified rules and tests. It is not merged wholesale. Existing branches, database volumes and the current demo remain intact.
