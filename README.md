# Bank-los
Deployment Code 

Run the Spring Boot application from PowerShell with:

```powershell
.\mvnw.cmd spring-boot:run
```

## Customer document storage folder

Uploaded customer documents are stored (encrypted) on disk. No setup is needed:
by default they go to `<your user home>/uploads/customer-documents`, for example
`C:\Users\<you>\uploads\customer-documents`. The folder is created on startup and
its full path is printed in the log (`Customer document storage folder: ...`).

To use another folder, add `CUSTOMER_DOCUMENTS_PATH` to the `.env` file in the project root:

```properties
CUSTOMER_DOCUMENTS_PATH=D:/los-files      # absolute folder
CUSTOMER_DOCUMENTS_PATH=~/los-files       # ~ = your user home
CUSTOMER_DOCUMENTS_PATH=uploads           # relative to the folder the app is started from
```

The database stores only paths relative to this folder, so the folder can be moved
(copy the files across and point `CUSTOMER_DOCUMENTS_PATH` at the new place).

## Deploy on Render with Docker

Create a **Web Service** on Render, select **Docker** as the runtime, and deploy
from the repository root. Render will build the included `Dockerfile`; the
application listens on Render's `PORT` environment variable.

Set the application's configuration in Render's Environment dashboard, not in
the Dockerfile. Configure the required database connection variables
(`SETUP_DB_URL`, `SETUP_DB_USERNAME`, `SETUP_DB_PASSWORD`, and, if using a
separate bank database, `BANK_DB_URL`, `BANK_DB_USERNAME`, and
`BANK_DB_PASSWORD`). The URLs must use JDBC format, for example
`jdbc:postgresql://<host>:5432/<database>`. Also configure the customer
encryption secrets required by your active Spring profile
(`CUSTOMER_MASTER_SECRET`, `CUSTOMER_AES_SECRET`, and `CUSTOMER_HMAC_SECRET`).

The container filesystem is ephemeral. If customer document uploads must
survive deploys and restarts, attach a Render persistent disk and set
`CUSTOMER_DOCUMENTS_PATH` to its mount path.
