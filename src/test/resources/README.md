### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `IUDX-Resource-Server-Consumer-APIs-V4.0.environment.json` has **values** array which has a fields named **basePath** whose **value** is currently set to `ngsi-ld/v1`, **basePathDx** whose value is currently set to `iudx/v1`.
- The **value** can be changed according to the deployment and then the collection with the *environment.json file can be uploaded to Postman
- For the changing the **basePath**, **basePathDx** values in postman, locate `RS Environment` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the variables could be changed


