# Design pattern:

### Model-view Controller
Classes like RedditPost act as data models, specifying the format in which data is expected and is further handled

Controller exposes REST endpoints to handle the incoming HTTP requests

Service layer does the processing required to provide the response to a request, by processing the inputs and hitting the external apis to fetch required data


### Annotations:
```aiignore
1. @JsonIgnoreProperties(ingoreUnknown = true)
2. @JsonProperty("property_name")
    (Mapped to a variable)

```
