# CATS front-end standard

One look for every page. Read this before you write or change a template. `manager-home.html` is the reference page: copy it.

## 1. Stack

- Spring MVC + Thymeleaf. No React, Vue, Angular, npm or build step.
- Bootstrap 5.3.3 CSS and JS.
- `static/css/cats.css` on top of Bootstrap. It is the only place for colours, fonts and sizes.
- `static/js/cats.js` for the sidebar. Small vanilla JS is fine for page logic. No jQuery.
- No `<style>` blocks and no `style="..."` in templates. If something is missing, add a `cats-` class to `cats.css`.

## 2. Page skeleton

Every signed-in page uses the shared layout:

```html
<!doctype html>
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout('Course catalogue', 'Admin', 'courses', ~{::#content})}">
<body>
<section id="content">
  ... page content ...
</section>
</body>
</html>
```

| Argument | Meaning |
| --- | --- |
| `'Course catalogue'` | Page title. Shown in the browser tab and in the header breadcrumb. |
| `'Admin'` | Current workspace: `Staff`, `Manager` or `Admin`. Picks the sidebar links; a Manager can also use the Staff workspace. |
| `'courses'` | Key of the sidebar link to highlight (section 3). |
| `~{::#content}` | The page content. |

The layout adds the sidebar, the header with the user menu, flash messages (`success`, `error`), Bootstrap, `cats.css` and `cats.js`.
Login pages do not use the layout (section 17).

## 3. Layout and navigation

- Left sidebar, 248 px. The Collapse button at the bottom turns it into a 64 px icon rail; the choice is remembered in the browser.
- Below 992 px the sidebar starts collapsed. Below 768 px it is hidden and opens as a drawer from the menu button in the header.
- Navy sidebar by default. The light variant is `<div class="cats-layout light">`. No other variants.
- The active link has an orange bar on the left and an orange icon.
- Header: breadcrumb `Role / Page title` on the left; user menu (initials, name, ID) on the right with Log out.
- Managers can switch workspaces from both the sidebar and user menu: My courses on Manager pages, Manager dashboard on Staff pages. Ordinary Staff do not see Manager links.
- Log out is also the last item of the sidebar.
- A link to a page that does not exist yet uses `fragments/ui :: navsoon` and shows a Soon tag. It is not clickable.

Sidebar keys:

| Role | Keys |
| --- | --- |
| Staff | `dashboard`, `apply`, `history`, `fees`, `calendar`; `manager` for Manager accounts only |
| Manager | `dashboard`, `approvals`, `history`, `staff`, `calendar`, `claims`, `reports` |
| Admin | `dashboard`, `accounts`, `courses`, `calendar`, `payments`, `reports`, `holidays` |

When you add a page, add its link in `fragments/layout.html` and its key to this table.

Group Admin pages by task. Accounts contains reporting managers, annual allowances and email. Courses contains providers, categories and saved course dates. Use the secondary navigation in `fragments/ui.html`; these do not need separate sidebar entries. Date calculation belongs inside the course form, not in a separate tool page. Training calendar shows approved employee attendance, not catalogue course dates.

## 4. Page header

Every page starts with a page header. One `h1` per page.

```html
<div class="cats-page-header">
  <div>
    <!-- back link only on detail and form pages -->
    <a th:href="@{/staff/course-applications}" class="cats-back"><svg th:replace="~{fragments/icons :: icon('arrow-left')}"></svg>Course history</a>
    <h1>Apply for a course</h1>
  </div>
  <div class="cats-page-actions">
    <a class="btn btn-primary" th:href="@{/admin/courses/new}"><svg th:replace="~{fragments/icons :: icon('plus')}"></svg>Add course</a>
  </div>
</div>
```

Titles are short noun phrases in sentence case: "Course catalogue", not "Manage Course Catalogue Page". A status pill may follow the title on detail pages.

Do not add an explanatory paragraph below every heading. Keep names, dates and other useful record details there. Put a short rule beside the field it affects, and keep validation messages specific.

## 5. Spacing and grid

- Content padding 28 px (16 px on phones). Gap between cards 20 px. Inside cards 16 px.
- Use the helpers instead of Bootstrap rows: `.cats-stack` (vertical), `.cats-grid.cats-grid-4` (metric tiles), `.cats-grid.cats-grid-2`, `.cats-grid.cats-grid-main` (content plus side column). They stack on their own at smaller widths.
- Do not add margins to single elements. Put siblings in a stack or grid.

## 6. Typography

- Inter, 14 px body, 1.5 line height, with the system font as fallback.
- `h1` 22 px / 600. Card title `.cats-card-title` 15 px / 600. Table header 12.5 px / 600 grey. Hints 12.8 px grey.
- Numbers in tables and tiles use `.cats-num` (right aligned, tabular digits).
- UI text is English, sentence case, short. Buttons say what happens: "Submit application", "Save course".

## 7. Colours

All colours live in `cats.css`. Never type a hex value in a template.

| Use | Value |
| --- | --- |
| Actions and links | blue `#0b63ce` |
| Text | `#1c2430`; muted `#5f6b7a` |
| Page background, surfaces | `#f4f6f9`, white cards |
| Borders | `#e1e6ec`; inputs `#c3cad4` |
| Sidebar | navy `#0b2545` (white in the light variant) |
| Orange `#e8700a` | brand mark, active sidebar link, pending status, user avatar |
| Green, red, grey | status and feedback only |

Orange marks what needs attention; blue is for actions. Do not use colour as decoration.

## 8. Buttons

- One `btn-primary` per screen: the main action (Submit, Save, Approve, Add).
- `btn-outline-secondary` for Cancel, Back and secondary actions.
- `btn-outline-danger` for Reject. `btn-danger` only inside a confirmation dialog.
- Inside table rows use quiet buttons: `btn btn-sm btn-quiet` (View, Edit, Review) and `btn btn-sm btn-quiet-danger` (Delete).
- A link that opens a page is `<a class="btn ...">`. An action that changes data is `<form method="post"><button>`.
- Icons are optional. When used, put the icon before the text: `<svg th:replace="~{fragments/icons :: icon('plus')}"></svg>Add course`.

## 9. Forms and validation

```html
<div class="card">
  <div class="card-body px-4 pt-4 pb-2">
    <form id="apply-form" th:action="@{/staff/course-applications}" th:object="${application}" method="post" novalidate>

      <div class="cats-error-summary mb-3" role="alert" th:if="${#fields.hasErrors('*')}">
        <svg th:replace="~{fragments/icons :: icon('alert-circle')}"></svg>
        <div>
          <h2>Fix the problems below before you submit</h2>
          <ul><li th:each="e : ${#fields.allErrors()}" th:text="${e}">Error</li></ul>
        </div>
      </div>

      <div class="cats-form-section">
        <div><h2>Course</h2><p>What you want to attend and who runs it.</p></div>
        <div class="cats-form">
          <div class="cats-field" th:classappend="${#fields.hasErrors('courseTitle')} ? 'has-error'">
            <label class="form-label cats-req" for="courseTitle">Course title</label>
            <input class="form-control" id="courseTitle" th:field="*{courseTitle}" th:errorclass="is-invalid" maxlength="200">
            <div class="cats-field-msg" th:errors="*{courseTitle}"></div>
            <div class="form-text">Optional hint under the field.</div>
          </div>
        </div>
      </div>

    </form>
  </div>
  <div class="card-footer px-4 py-3 cats-form-footer">
    <a class="btn btn-outline-secondary" th:href="@{/staff/course-applications}">Cancel</a>
    <button type="submit" form="apply-form" class="btn btn-primary">Submit application</button>
  </div>
</div>
```

- Long forms are split into `.cats-form-section` blocks: a title and one line on the left, the fields on the right. Short forms (one to three fields) use a single `.cats-form`.
- Required fields get `cats-req` on the label. Optional fields are not marked.
- Validate on the server with Bean Validation and `BindingResult`. Show the summary at the top and the message under each field. Forms carry `novalidate`; browser validation is not the check.
- Two fields side by side: wrap them in `.cats-grid.cats-grid-2`.
- The primary button sits in the card footer, right aligned, Cancel before it. When the button is outside the `<form>`, link it with `form="form-id"`.
- Money fields use an input group with `$`. Dates use `type="date"`.
- Fields that depend on another field appear only when they apply. Course application: the half-day session select appears only when the category is Internal Training, and the fee is then locked at 0.00. The server enforces the same rule.

Segmented choice, for two or three options that are easier to click than a dropdown:

```html
<fieldset class="cats-field">
  <legend class="form-label">Sign in as</legend>
  <div class="cats-seg">
    <input type="radio" name="designation" id="roleStaff" value="STAFF" checked><label for="roleStaff">Staff</label>
    <input type="radio" name="designation" id="roleManager" value="MANAGER"><label for="roleManager">Manager</label>
  </div>
</fieldset>
```

## 10. Tables

```html
<div class="card">
  <div class="cats-toolbar">
    <div class="cats-search">
      <svg th:replace="~{fragments/icons :: icon('search')}"></svg>
      <input class="form-control" type="search" id="search" placeholder="Search title or provider" aria-label="Search">
    </div>
    <span class="ms-auto small text-secondary" th:text="${#lists.size(applications)} + ' applications'">6 applications</span>
  </div>
  <div class="table-responsive">
    <table class="table">
      <thead>
        <tr><th>Course</th><th>Category</th><th>Dates</th><th class="cats-num">Fee</th><th>Status</th><th><span class="visually-hidden">Actions</span></th></tr>
      </thead>
      <tbody>
        <tr th:each="a : ${applications}">
          <td class="cats-col-main">
            <a class="cats-row-link" th:href="@{/staff/course-applications/{id}(id=${a.courseId})}" th:text="${a.courseTitle}">Title</a>
            <div class="cats-cell-sub" th:text="${a.trainingProvider}">Provider</div>
          </td>
          <td><span th:replace="~{fragments/ui :: tag(${a.courseCategory.displayName})}"></span></td>
          <td class="text-nowrap"><span th:text="${#temporals.format(a.courseStartDate, 'd MMM yyyy')}">12 Nov 2026</span> &ndash; <span th:text="${#temporals.format(a.courseEndDate, 'd MMM yyyy')}">14 Nov 2026</span></td>
          <td class="cats-num" th:text="'$' + ${#numbers.formatDecimal(a.courseFee, 1, 'COMMA', 2, 'POINT')}">$1,800.00</td>
          <td><span th:replace="~{fragments/ui :: status(${a.status})}"></span></td>
          <td class="cats-row-actions">
            <a class="btn btn-sm btn-quiet" th:href="@{/staff/course-applications/{id}(id=${a.courseId})}">View<svg th:replace="~{fragments/icons :: icon('chevron-right')}"></svg></a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

- Always inside a card and a `.table-responsive`. Never `border="1"`, never `.table-bordered`.
- The first column is the name of the thing, dark and medium weight, with a grey second line when useful (`.cats-cell-sub`). Give it `.cats-col-main`.
- Numbers and money are right aligned with `.cats-num`. Dates are `text-nowrap`.
- Actions sit in the last column, right aligned, as quiet buttons. Two at most; the rest belongs on the detail page.
- The toolbar is optional: search and filters on the left, the row count on the right.
- Group rows, for example applications grouped by employee: `<tr class="cats-group-row"><th colspan="6">Name (S0012) <small>2 pending</small></th></tr>`.
- An empty list shows the empty state (section 15), not an empty table.

## 11. Status badges

Only states get colour. Categories and other labels use the grey tag.

```html
<span th:replace="~{fragments/ui :: status(${application.status})}"></span>
<span th:replace="~{fragments/ui :: tag(${course.courseCategory.categoryName})}"></span>
```

| Status | Colour |
| --- | --- |
| Applied, Updated | orange (waiting for a decision) |
| Approved | green |
| Rejected | red |
| Completed | dark grey |
| Cancelled, Deleted | light grey |

The fragment turns `APPLIED` into "Applied". Never print the raw enum.

## 12. Cards and metric tiles

- Cards: white, 1 px border, 8 px radius, no shadow. The header holds the title (`.cats-card-title`, an `h2`) and optionally a link or tag on the right.
- Metric tiles for dashboards: `.card.cats-metric` with a label, a value, an optional `<small>` line and an optional progress bar. Four per row on desktop. Use them only when the numbers are the point of the page.

```html
<div class="card cats-metric">
  <div class="cats-metric-label">Training days remaining</div>
  <div class="cats-metric-value">7.5<small>Annual limit: 10 days</small></div>
  <div class="progress" role="progressbar" aria-label="Training days used" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100">
    <div class="progress-bar" th:style="'width:' + ${usedPercent} + '%'"></div>
  </div>
</div>
```

The width of a progress bar is the only allowed inline style.

## 13. Detail view

Key-value pairs use a definition list inside a card:

```html
<div class="card">
  <div class="card-header"><h2 class="cats-card-title">Application</h2></div>
  <div class="card-body">
    <dl class="cats-dl">
      <dt>Provider</dt><dd th:text="${a.trainingProvider}">NUS-ISS</dd>
      <dt>Dates</dt><dd>12&ndash;14 Nov 2026</dd>
      <dt>Training days</dt><dd>3</dd>
    </dl>
  </div>
</div>
```

Detail pages use `.cats-grid.cats-grid-main`: the facts on the left, the decision or summary card on the right. On phones the right column moves below.

## 14. Flash messages and alerts

- After a successful POST, redirect and add a flash attribute: `redirectAttributes.addFlashAttribute("success", "Course saved.")`. The layout shows it as a green dismissible alert at the top of the page. Use `error` for failures.
- Say what happened in one sentence and name the thing: `Course "Kubernetes Fundamentals" was saved.`
- Pages do not render success messages themselves; the layout does.

## 15. Empty states

```html
<th:block th:if="${#lists.isEmpty(courses)}">
  <div th:replace="~{fragments/ui :: empty('book', 'No courses yet', 'Add the first course so staff can select it when they apply.', ~{::#add-course})}"></div>
</th:block>
```

The title says what is missing, the text says what to do, and the main action is repeated as a button when it makes sense (pass `~{}` when it does not).

## 16. Confirmation dialogs

Delete, cancel, approve and reject need a confirmation. Use the shared modal with a POST form inside. Never `onclick="return confirm(...)"` and never a GET link that changes data.

```html
<button type="button" class="btn btn-sm btn-quiet-danger" data-bs-toggle="modal" th:attr="data-bs-target='#delete-' + ${c.courseId}">Delete</button>
<div th:replace="~{fragments/ui :: confirm('delete-' + ${c.courseId}, 'Delete this course?', 'Staff will no longer be able to pick ' + ${c.title} + '.', @{/admin/courses/{id}/delete(id=${c.courseId})}, 'Delete', true)}"></div>
```

The dialog names the thing and the consequence. Buttons: Cancel (outline) and the action (red for destructive, blue for approve).

## 17. Login pages

No sidebar. A centered card with the brand above it and one line of links below. The saved account decides whether the employee is Staff or Manager; do not ask for a second role choice. The password field has a show/hide button. Errors are an `alert-danger` above the form: "Wrong username or password." No other text on the page.

## 18. Responsive rules

- Breakpoints: 992 px (sidebar collapses, side columns stack), 768 px (sidebar becomes a drawer), 576 px (grids become one column).
- Tables scroll sideways inside `.table-responsive`. Do not hide columns with custom CSS.
- The layout sets `<meta name="viewport" content="width=device-width, initial-scale=1">`; login pages set it themselves.
- Nothing may force the page wider than the screen: no fixed widths in templates.

## 19. Data formats

| Data | Format | Thymeleaf |
| --- | --- | --- |
| Date | `12 Nov 2026` | `${#temporals.format(d, 'd MMM yyyy')}` |
| Date range | `12 Nov 2026 – 14 Nov 2026` | two dates joined with `&ndash;` |
| Money | `$1,800.00` | `'$' + ${#numbers.formatDecimal(x, 1, 'COMMA', 2, 'POINT')}` |
| Training days | `3 days`, `0.5 day` | number plus unit |
| Status | `Applied` | `fragments/ui :: status` |

## 20. Thymeleaf and file conventions

- Template file names are kebab-case: `course-application-form.html`. Fragments live in `templates/fragments/`.
- One page = one controller method = one template. Use `th:field` for inputs, `th:each` for rows, `th:href="@{...}"` for every link.
- Every input has an `id` and a `<label for>`. IDs are unique on the page.
- Buttons that change data are inside `<form method="post">`. GET only reads.
- No `<style>`, no inline `style=` (except the progress-bar width), no `<br>` for spacing, no `&nbsp;` for alignment, no non-English UI text.
- Icons come from `fragments/icons`. Do not paste other SVGs or add an icon font.

## 21. Page checklist

- [ ] Uses `fragments/layout` with the right role and active key
- [ ] Page header with one `h1` and at most one primary button
- [ ] No inline styles, no `<style>` block, no hex colours
- [ ] Tables: card + `.table-responsive`, quiet row actions, status fragment, numbers right aligned
- [ ] Forms: labels, required markers, server-side errors shown, Cancel + primary in the footer
- [ ] Data-changing actions are POST; destructive ones are confirmed in the modal
- [ ] Empty state wherever a list can be empty
- [ ] Success and error arrive as flash attributes
- [ ] Works at 1280, 820 and 390 px

## 22. Instructions for AI coding agents

Paste this with your request:

> This project is Spring MVC + Thymeleaf + Bootstrap 5.3 with `static/css/cats.css`. Follow `DESIGN.md` exactly. Every page uses `fragments/layout :: layout(title, role, activeKey, ~{::#content})` and starts with `.cats-page-header`. Use the fragments in `fragments/ui.html` for status pills, tags, empty states and confirmation modals, and `fragments/icons.html` for icons. No new CSS or JS frameworks, no inline styles, no `<style>` blocks, no hex colours in templates, no `confirm()`, no GET links that change data. One primary button per screen, quiet buttons inside table rows, English sentence-case text. Validate on the server and show errors with the summary + field pattern. Dates `d MMM yyyy`, money `$1,800.00`. If a pattern is missing from `cats.css`, add a `cats-` class there instead of styling the template. Use `manager-home.html` as the reference page.

## UI wording

Use ordinary English labels and short sentences. Do not join names, roles, features or explanations with middle dots. Keep names and IDs only where they identify an account. Explain a field only when the user needs the rule to enter a valid value. Remove repeated headings, welcome lines and generic instructions. Code comments should explain a business rule or a non-obvious implementation choice; class and method names do not need to be repeated in comments.
