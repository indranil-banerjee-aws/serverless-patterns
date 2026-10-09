#!/bin/bash
# =============================================================================
# Start DynamoDB Local for `sam local invoke` and create the consumer's table in
# it, so a local test never writes to the real DynamoDB table.
#
# When `sam local invoke` runs the function it sets AWS_SAM_LOCAL=true, and the
# handler then sends its writes to http://dynamodb-local:8000 instead of
# DynamoDB. That host name resolves only on the Docker network this script
# creates, so pass the same network to sam:
#
#   bash scripts/local_dynamodb.sh
#   sam build
#   sam local invoke --event events/event.json --docker-network sam-local
#
# DynamoDB Local runs in memory: its data is gone once the container stops.
# Stop it with: docker rm -f dynamodb-local
# =============================================================================
set -eo pipefail
source /home/ec2-user/kafka_oauth.env

NETWORK=sam-local
CONTAINER=dynamodb-local
PORT=8000
IMAGE=public.ecr.aws/aws-dynamodb-local/aws-dynamodb-local:latest
# Same default the SAM template's DynamoDBTableName parameter gets
TABLE="${DYNAMODB_TABLE_NAME:-$STACK_NAME-sam-messages}"
LOCAL="--endpoint-url http://localhost:$PORT"

docker network inspect "$NETWORK" >/dev/null 2>&1 || docker network create "$NETWORK" >/dev/null

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  echo "Starting DynamoDB Local ($CONTAINER on network $NETWORK, localhost:$PORT)..."
  # -sharedDb: one database for every caller. Without it DynamoDB Local keeps a
  # separate database per access key and region, and the function's credentials
  # differ from this shell's, so it wouldn't see the table created below.
  docker run -d --name "$CONTAINER" --network "$NETWORK" -p "127.0.0.1:$PORT:8000" \
    "$IMAGE" -jar DynamoDBLocal.jar -inMemory -sharedDb >/dev/null
fi

for i in $(seq 1 30); do
  aws dynamodb list-tables $LOCAL --region "$AWS_REGION" >/dev/null 2>&1 && break
  sleep 1
done

if aws dynamodb describe-table $LOCAL --region "$AWS_REGION" --table-name "$TABLE" >/dev/null 2>&1; then
  echo "Table $TABLE already exists in DynamoDB Local."
else
  echo "Creating table $TABLE in DynamoDB Local..."
  aws dynamodb create-table $LOCAL --region "$AWS_REGION" --table-name "$TABLE" \
    --attribute-definitions AttributeName=topicPartition,AttributeType=S AttributeName=offset,AttributeType=N \
    --key-schema AttributeName=topicPartition,KeyType=HASH AttributeName=offset,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST >/dev/null
fi

cat <<EOF

DynamoDB Local is ready. Run the function against it with:
  sam local invoke --event events/event.json --docker-network $NETWORK

Then look at what it wrote:
  aws dynamodb scan --endpoint-url http://localhost:$PORT --region $AWS_REGION --table-name $TABLE
EOF
