### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `IUDX-Resource-Server-Consumer-APIs-V4.0.environment.json` has **values** array that has fields named **basePath** whose **value** is currently set to `ngsi-ld/v1`, **dxAuthBasePath** with value `auth/v1`.
- These value(s) could be changed according to the deployment and then the collection with the `IUDX-Resource-Server-Consumer-APIs-V4.0.environment.json` file can be uploaded to Postman
- For the changing the **basePath**, **dxAuthBasePath** value in postman after importing the collection and environment files, locate `RS Environment` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variable could be changed


