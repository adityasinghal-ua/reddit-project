## Start Project:
1. Run Kafka
2. Run OpenSearch

## TODO
1. Figure out a safe way to store client_id and client_secret
2. Add logic so that token is not generated each time, only when it has expired
~~3. Edge case: If a new user is queried using limit for the first time, limited posts are saved to MongoDB and next time when the username is passed, they are retrieved from MongoDB and the API is not hit. Override MongoDB option~~  
~~3. Add check to see if limit > posts in MongoDB or if there is no limit, then hit the API~~

# Design pattern:
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

### Code:
```aiignore
    private KafkaProducer<String, String> producer;
```
Here, <String, String> represents the key, value pair. KafkaProducer is used to send messages to Kafka topics

## Project setup

### Kafka
1. Start kafka and zookeeper
```aiignore
brew services start zookeeper
brew services start kafka
```

2. Check
```aiignore
brew services list
```

3. Create kafka topic
```aiignore
kafka-topics --create --topic reddit-posts --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

4. View list of created topics
```aiignore
kafka-topics --list --bootstrap-server localhost:9092
```

5. View status of kafka consumer groups
```aiignore
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

6. Command to send message
```aiignore
kafka-console-producer.sh --broker-list localhost:9092 --topic reddit-posts
```

7. Command to consume message`
```aiignore
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic reddit-posts --from-beginning
```
test command
```
{"username": "test_user", "title": "Test Post", "selftext": "This is a test post"}

```

8. Stop kafka and zookeeper
```aiignore
brew services stop kafka
brew services stop zookeeper
```

### OpenSearch (es)
1. Start OpenSearch
```aiignore
opensearch
```

### Git commands
1. Commit with staging
```aiignore
git commit -a -m "Commit Message"
-a => add
stages all changes first before committing them
```

2. Remove already tracked files
```aiignore
git rm -r -- cached <file_names>
-r => recursive removal flag remove directories and their contents
--cached => removes files/folders from Git index while keeping them on local system

```