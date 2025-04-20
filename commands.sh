spark-submit --class dev.toaster.commons.Main --master spark://spark-master:7077 --packages org.apache.spark:spark-sql-kafka-0-10_2.13:3.5.2 /opt/spark/apps/spark-app-assembly-0.1.0-SNAPSHOT.jar

docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic input
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic output

docker exec -it kafka kafka-console-producer --bootstrap-server localhost:9092 --topic input
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic input --from-beginning

curl -X POST http://localhost:8081/subjects/input-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"name\": \"Person\", \"namespace\": \"dev.toaster.commons\", \"doc\": \"This is a description\", \"type\": \"record\", \"fields\": [{\"name\": \"name\", \"type\": \"string\"}, {\"name\": \"age\", \"type\": \"int\"}]}"
  }'

curl -X POST http://localhost:8081/subjects/input-key/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"name\": \"Key\", \"namespace\": \"dev.toaster.commons\", \"doc\": \"This is a description\", \"type\": \"record\", \"fields\": [{\"name\": \"uuid\", \"type\": \"string\"}]}"
  }'
