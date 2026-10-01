import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

import { parseRecords, lambdaHandler } from "../app.mjs";

const here = path.dirname(fileURLToPath(import.meta.url));
const event = JSON.parse(readFileSync(path.join(here, "..", "..", "events", "event.json"), "utf-8"));

test("parseRecords flattens and base64-decodes the batch", () => {
  const messages = parseRecords(event);
  assert.equal(messages.length, 2);
  assert.equal(messages[0].topic, "myTopic");
  assert.equal(messages[0].partition, 0);
  assert.equal(messages[0].offset, 250);
  assert.equal(messages[0].timestamp, 1678072110111);
  assert.equal(messages[0].timestampType, "CREATE_TIME");
  assert.equal(messages[0].decodedKey, "null");
  assert.equal(messages[0].decodedValue, "f");
  assert.equal(messages[1].offset, 251);
  assert.equal(messages[1].decodedValue, "g");
});

test("handler returns 200 OK and skips DynamoDB when no table is set", async () => {
  delete process.env.DYNAMODB_TABLE_NAME;
  assert.equal(await lambdaHandler(event), "200 OK");
});
