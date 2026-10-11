# Demo: completion, claim and reimbursement

Run these steps on `imran/final-mvc` for the 11-12 October 2026 rehearsal. Use the supplied accounts and course to test steps 9 and 10 of the presentation.

## 1. Get the branch

Stop Spring Boot in the IDE or terminal. From the repository folder containing `pom.xml`, save local changes shown by `git status`, then run:

```sh
git status --short
git fetch origin
git switch imran/final-mvc
git pull --ff-only origin imran/final-mvc
git branch --show-current
git log -1 --oneline
```

The branch must be `imran/final-mvc`. For a new clone, use the [README instructions](../../README.md#get-the-demo-branch).

## 2. Restore the initial demo data

Open Docker Desktop and wait for its engine to start. Keep Spring Boot stopped, then run **one** command from the repository folder:

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File .\scripts\reset-demo.ps1
```

```sh
# macOS / Linux
bash scripts/reset-demo.sh
```

Type `RESET` when asked. The script backs up the current `cats-dev` database, restores the fixed demo data and starts MySQL. Wait for **Demo ready** before continuing.

Ordinary Docker or Spring Boot restarts preserve your changes. Pulling new SQL files also leaves an existing database unchanged. Use this reset before each new rehearsal.

## 3. Start Spring Boot and log in

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```sh
# macOS / Linux
./mvnw spring-boot:run
```

Open `http://localhost:8081` after Spring Boot has started. All sample passwords are `demo123`.

| Account | Login page | Use in this demo |
|---|---|---|
| `staff` | Employee login | Sam submits the completion comments and claim |
| `manager` | Employee login | Morgan reviews Sam's claim |
| `admin` | Admin login | Alex records the reimbursement |

The other sample accounts remain available: `manager2`, `staff2` and `staff3` use Employee login. Managers can also open their own Staff pages.

## 4. Mark the course completed

1. Log in as **staff**, then open **Course history**.
2. Select **Demo - Cloud deployment basics**: **Approved**, **9 October 2026**, **SGD 200**. This is course application **102** for Sam Staff / S001.
3. Enter a nonempty comment in **What did you learn?**, for example `I learned how to deploy and check a Spring Boot application.`
4. Click **Mark completed** and show the **Completed** status and saved comments.

CA page 6 says the employee marks attendance with experience comments once an approved course has ended. The current rule requires its end date to be earlier than today. This sample can be completed on either rehearsal day; completion is a manual employee action.

## 5. Step 9: submit the fee claim

1. Stay logged in as **staff** and open **Fee claims**.
2. Select **Demo - Cloud deployment basics** and tick **I paid this course fee personally**.
3. Upload [sam-cloud-receipt.pdf](sam-cloud-receipt.pdf) in the receipt field.
4. Upload [sam-cloud-certificate.pdf](sam-cloud-certificate.pdf) in the certificate field.
5. Submit the claim and show **Applied**, meaning it is waiting for review.

The PDFs are fictitious classroom samples matching Sam, the course, its date and SGD 200 fee.

Sam's other initially Completed course already has a claim. Only completed, fee-paying External/Certification courses without a claim appear in the selector. Complete the Cloud course first to create a new eligible choice.

## 6. Step 10: review and reimburse

1. Log out, then use **Employee login** as **manager**.
2. Open **Claim approvals** and select Sam's **Cloud deployment basics** claim for **SGD 200**. The seeded Java certification claim is a different example.
3. Open both evidence documents. Choose **Approve**, enter a reason such as `Receipt and attendance checked.`, and save.
4. Log in as **staff** to show the **Approved** claim. Approval does not record payment.
5. Log out, then use **Admin login** as **admin**. Open **Reimbursements** and find this claim.
6. For the sample payment, enter reference `DEMO-CLOUD-001`, click **Record payment** and confirm. Show the saved reference and payment date.
7. As **manager** or **admin**, open **Training reports** and filter for Sam. Show course fees, approved claims, actual reimbursements and annual allowance figures.

If you only complete and claim this course, Sam's remaining allowance stays **6.5 days / SGD 1400**. Its fee was already counted when the course was approved; the claim and reimbursement do not deduct it again.

## If your screen differs

| What you see | Check |
|---|---|
| `ryan_staff` or `ryan_staff2` instead of Sam | Use the supplied `staff` account for this flow |
| The Cloud course is missing or has different dates | Check the branch, run the reset, then restart Spring Boot from that checkout |
| The Cloud course is already Completed or already claimed | A previous rehearsal changed it; reset before repeating |
| Mark completed is absent | Check that the course is Approved and ended before today; use the 9 October sample |
| No new course appears in Fee claims | Complete the Cloud course and ensure it has no existing claim |
| An old page remains after pulling | Stop the old Spring Boot instance and start it from this updated branch |

For independent checks, the initial data also includes Sam's pending claim, Taylor's paid claim and Jordan's approved but unpaid claim, each with matching evidence.

History page sizes currently remain **10, 20 and 25**. A 5-row option is a separate proposed change; it is not needed for completion or fee-claim testing.
