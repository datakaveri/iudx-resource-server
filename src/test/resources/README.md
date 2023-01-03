### Making configurable base path
- Base path can be added in postman environment file or in postman.
- `IUDX-Resource-Server-Consumer-APIs-V3.5.environment.json` has **values** array which has a field named **basePath** whose **value** is currently set to `ngsi-ld/v1`.
- That **value** can be changed according to the deployment and then the collection with the *environment.json file can be uploaded to Postman
- For the changing the **basePath** value in postman, locate `RS Jenkins Pipeline` from **Environments** in sidebar of Postman application.
- To know more about Postman environments, refer : [postman environments](https://learning.postman.com/docs/sending-requests/managing-environments/)
- The **CURRENT VALUE** of the basePath variable could be changed
- Here are some list of base paths values
  - adex/v1
  - gisdx/v1
  - ipeg/v1
  - ngsi-ld/v1

