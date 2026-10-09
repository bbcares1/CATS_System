# CATS walkthrough

Start the development stack using README. All sample passwords are `demo123`; sample dates are in 2026. Use future working dates that do not overlap an existing request. Keep the production database separate from these fictional samples.

1. Sign in as `staff_alex`. Search and filter the course catalogue, open a course, select its session or allowed custom dates, and enter a justification. Show the fee/days preview and annual allowance before submitting. Use **Apply for another course** for an offer outside the catalogue.
2. Sign in as `mgr_bob`. Open the grouped approval list and application detail. Show the employee's annual usage and other approved courses during the same period. Approve or reject with a required reason.
3. Return to Staff personal history. Show the recorded decision and available actions. Applied/Updated requests can be edited or deleted; Approved courses can be cancelled. Completion requires an ended course and experience comments.
4. On the Manager dashboard, open the personal Staff workspace. Managers can apply and see their own history. A Manager without a reporting manager chooses another active Manager for review; self-review is blocked.
5. Use Bob's completed **Team leadership** sample for a fee claim. Attach a real PDF/JPEG/PNG receipt and certificate, select Chris as reviewer, then approve as `mgr_chris`. Sign in as `admin_alice` to record payment with a reference. Show the separate decision and payment states, and private evidence access.
6. As Admin, show account/reporting-manager maintenance, annual allowances, categories, courses/sessions and holidays. Explain archive/disable for records with history. A conflicting holiday or a limit below existing reservations is rejected.
7. Open the shared calendar and filter by month/category. Show Manager reports for direct reports and Admin reports for everyone, then export CSV. Calendar attendance uses approved course dates and excludes weekends/holidays.
8. Show compile/fast-test CI followed by full H2/MySQL checks. Explain the separate development, disposable test and persistent production databases. Use DEPLOYMENT for the already tested startup, restart and backup/restore procedure.

The sample data includes pending, updated, approved, completed, rejected, cancelled and deleted applications. Keep the original samples for repeatable demonstrations; use clearly named new requests for a rehearsal.
