# Design pattern:

## TODO
1. Figure out a safe way to store client_id and client_secret
2. Add logic so that token is not generated each time, only when it has expired

### Model-view Controller
Classes like RedditPost act as data models, specifying the format in which data is expected and is further handled



Controller exposes REST endpoints to handle the incoming HTTP requests and access external endpoints / apis

Service layer does the processing required to provide the response to a request, by processing the inputs, basically the business logic

View is not implemented here as backend apis are being exposed


### Annotations:
```aiignore
1. @JsonIgnoreProperties(ingoreUnknown = true)

2. @JsonProperty("property_name")
    (Mapped to a variable)

3. @RegisterRestClient: , used to make function that can hit external apis, configKey is used to specify the baseUrl

4. @HeaderParam("authorization"): adds a authorization header to the REST client request (like we specify in Postman)

5. @FormParam("grant_type"): specifies form parameter for token-based auth generally




```




