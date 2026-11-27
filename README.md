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
