# CATS front-end guide

We keep one look for every page. Our final page designs are in [Figma](https://www.figma.com/design/vHyqPjcmWuVVtKQP3JW2kP/CAT). Read this before you add or change a template, and copy an existing page that does the same job.

## 1. Stack

- Spring MVC and Thymeleaf. No front-end framework, npm or build step.
- Bootstrap 5.3.3, plus `static/css/cats.css` for every colour, font size and spacing rule.
- `static/js/cats.js` runs the sidebar. Small plain JavaScript files are fine for one page, such as `application-form.js`.
- No `<style>` blocks and no `style="..."` in templates. If a pattern is missing, add a `cats-` class to `cats.css`.

## 2. Page skeleton

Every signed-in page uses the shared layout:

```html
<html lang="en" xmlns:th="http://www.thymeleaf.org"
      th:replace="~{fragments/layout :: layout('Team course history', 'Manager', 'history', ~{::#content})}">
<body>
<section id="content" class="cats-stack">
  ... page content ...
</section>
</body>
</html>
```

The arguments are the page title, the workspace (`Staff`, `Manager` or `Admin`), the sidebar key to highlight and the content. The layout adds the sidebar, the header with the user menu, flash messages, Bootstrap, `cats.css` and `cats.js`. Login pages do not use it.

Sidebar keys:

| Workspace | Keys |
|---|---|
| Staff | `dashboard`, `apply`, `history`, `fees`, `calendar`; `manager` for Manager accounts |
| Manager | `dashboard`, `approvals`, `history`, `staff`, `calendar`, `claims`, `reports` |
| Admin | `dashboard`, `accounts`, `courses`, `holidays`, `calendar`, `payments`, `reports` |

When you add a page, add its link in `fragments/layout.html` and its key here. Admin sub-pages use the tabs in `fragments/ui.html` (`accountTabs`, `courseTabs`) instead of new sidebar links.

## 3. Page header

Every page starts with `.cats-page-header` and one `h1`. Titles are short and in sentence case: "Course catalogue", not "Manage Course Catalogue Page". Put the main page action on the right in `.cats-page-actions`, and use a `.cats-back` link on detail and form pages.

Do not add a paragraph under every heading. Show only names, dates or rules the user needs.

## 4. Forms

Follow `staff-application-form.html`:

```html
<div class="card card-body">
  <form class="cats-stack" th:action="@{${formAction}}" th:object="${form}" method="post" novalidate>
    <input th:replace="~{fragments/csrf :: token}" />
    <input type="hidden" th:field="*{version}" />
    <div class="cats-error-summary" role="alert" th:if="${#fields.hasErrors('*')}">
      <div>
        <h2 class="cats-card-title">Please check your application</h2>
        <ul><li th:each="error : ${#fields.allErrors()}" th:text="${error}"></li></ul>
      </div>
    </div>
    <div class="cats-field">
      <label class="form-label cats-req" for="courseTitle">Course title</label>
      <input class="form-control" th:field="*{courseTitle}" th:errorclass="is-invalid" maxlength="200" />
      <div class="cats-field-msg" th:errors="*{courseTitle}"></div>
    </div>
    <div class="cats-form-footer">
      <a class="btn btn-outline-secondary" th:href="@{/staff/personal}">Cancel</a>
      <button class="btn btn-primary" type="submit">Submit application</button>
    </div>
  </form>
</div>
```

- Every POST form includes the CSRF token. Forms that edit a saved record also send its `version`.
- Validate on the server with Bean Validation and service checks. Show the summary at the top and the message under each field. Forms use `novalidate`, so the browser does not replace the server check.
- Required labels get `cats-req`. Two fields side by side go in `.cats-grid.cats-grid-2`.
- Show a field only when it applies, and enforce the same rule on the server. Example: the session field appears only for Internal Training, and its fee is fixed at 0.00.

## 5. Tables and lists

Follow `manager-approvals.html` and `admin-account-list.html`:

- A table sits in a `.card` and a `.table-responsive`. No borders on cells.
- The first column (`.cats-col-main`) names the record and links to it with `.cats-row-link`; a grey second line uses `.cats-cell-sub`.
- Numbers and money use `.cats-num`. Dates use `text-nowrap`.
- Row actions are quiet buttons (`btn btn-sm btn-quiet`, `btn-quiet-danger`), at most two. Other actions belong on the detail page.
- To group rows, use one card per group with the group name in the card header.
- Long lists use `fragments/list-pagination :: pager(...)`; claim lists use `fragments/claim-pagination`.
- An empty list shows `fragments/ui :: empty(...)` instead of an empty table.

## 6. Status and tags

Use `fragments/ui :: status(value)` for application and claim states; it turns `APPLIED` into "Applied". Never print a raw enum. Categories and other labels use the grey `fragments/ui :: tag(text)`.

| Status | Colour |
|---|---|
| Applied, Updated | orange: waiting for a decision |
| Approved | green |
| Rejected | red |
| Completed | dark grey |
| Cancelled, Deleted | light grey |

## 7. Detail pages

Show saved facts as a definition list (`.cats-dl`) in a card; `fragments/application :: facts(course)` does this for applications. Put the decision or summary card beside the facts in `.cats-grid.cats-grid-main`; on small screens it moves below.

## 8. Messages and confirmations

- After a successful POST, redirect with a flash attribute: `success`, `warning` or `error`. The layout shows it at the top of the page.
- Say what happened in one sentence, for example "Application saved."
- Delete, cancel, approve and reject open `fragments/ui :: confirm(...)`, a dialog with a POST form. Never use `confirm()` or a GET link that changes data.
- The dialog names the record and the consequence. Destructive actions use a red button; approve uses blue.

## 9. Colours, type and spacing

All values live in `cats.css`; never type a hex value in a template.

- Blue `#0b63ce` for actions and links. Orange `#e8700a` only for things that need attention: pending status, the active sidebar link and the avatar.
- Navy sidebar `#0b2545`; text `#1c2430`; grey page background with white cards.
- Inter, 14 px body. One `btn-primary` per screen for the main action; `btn-outline-secondary` for Cancel and Back.
- Use `.cats-stack` and `.cats-grid` for spacing instead of margins on single elements.

## 10. Screen sizes

- Below 992 px the sidebar starts as an icon rail and side columns stack.
- Below 768 px the sidebar opens as a drawer from the header menu button.
- Below 576 px two-column grids become one column.
- Tables scroll sideways inside `.table-responsive`. Check pages at 1280, 820 and 390 px.

## 11. Data formats

| Data | Format | Thymeleaf |
|---|---|---|
| Date | `12 Nov 2026` | `${#temporals.format(d, 'd MMM yyyy')}` |
| Money | `$1,800.00` | `'$' + ${#numbers.formatDecimal(x, 1, 'COMMA', 2, 'POINT')}` |
| Training days | `3 days`, `0.5 day` | number plus unit |
| Status | `Applied` | `fragments/ui :: status` |

## 12. Wording and file names

- English, sentence case, short. Buttons say what happens: "Submit application", "Save course".
- Do not join names, roles or explanations with middle dots. Explain a field only when the user needs the rule to enter a valid value.
- New template files use kebab-case, such as `course-application-form.html`. Shared pieces live in `templates/fragments/`.
- Every input has an `id` and a matching `<label for>`. Use `th:href="@{...}"` for every link.

## 13. Page checklist

- [ ] Uses `fragments/layout` with the right workspace and sidebar key
- [ ] One `h1` and at most one primary button
- [ ] No inline styles, `<style>` blocks or hex colours
- [ ] POST forms include the CSRF token; edits send `version`
- [ ] Server-side errors appear in the summary and under each field
- [ ] Data changes use POST, and destructive actions are confirmed
- [ ] Empty lists show an empty state; long lists are paged
- [ ] Works at 1280, 820 and 390 px
