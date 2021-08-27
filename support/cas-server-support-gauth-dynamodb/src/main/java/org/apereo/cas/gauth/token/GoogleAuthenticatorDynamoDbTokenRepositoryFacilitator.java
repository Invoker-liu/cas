package org.apereo.cas.gauth.token;

import org.apereo.cas.authentication.OneTimeToken;
import org.apereo.cas.configuration.model.support.mfa.gauth.DynamoDbGoogleAuthenticatorMultifactorProperties;
import org.apereo.cas.dynamodb.DynamoDbQueryBuilder;
import org.apereo.cas.dynamodb.DynamoDbTableUtils;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.serialization.JacksonObjectMapperFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is {@link GoogleAuthenticatorDynamoDbTokenRepositoryFacilitator}.
 *
 * @author Misagh Moayyed
 * @since 6.5.0
 */
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthenticatorDynamoDbTokenRepositoryFacilitator {
    private static final ObjectMapper MAPPER = JacksonObjectMapperFactory.builder()
        .defaultTypingEnabled(false).build().toObjectMapper();

    private final DynamoDbGoogleAuthenticatorMultifactorProperties dynamoDbProperties;

    private final DynamoDbClient amazonDynamoDBClient;

    private static Map<String, AttributeValue> buildTableAttributeValuesMap(final OneTimeToken record) throws Exception {
        val values = new HashMap<String, AttributeValue>();
        values.put(ColumnNames.USERID.getColumnName(), AttributeValue.builder().s(record.getUserId().toLowerCase()).build());
        values.put(ColumnNames.TOKEN.getColumnName(), AttributeValue.builder().n(String.valueOf(record.getToken()).toLowerCase()).build());
        val time = record.getIssuedDateTime().toEpochSecond(ZoneOffset.UTC);
        values.put(ColumnNames.CREATION_TIME.getColumnName(), AttributeValue.builder().n(String.valueOf(time)).build());
        values.put(ColumnNames.BODY.getColumnName(), AttributeValue.builder().s(MAPPER.writeValueAsString(record)).build());
        LOGGER.debug("Created attribute values [{}] based on [{}]", values, record);
        return values;
    }

    @SneakyThrows
    private static GoogleAuthenticatorToken extractAttributeValuesFrom(final Map<String, AttributeValue> item) {
        return MAPPER.readValue(item.get(ColumnNames.BODY.getColumnName()).s(), new TypeReference<>() {
        });
    }

    /**
     * Create table.
     *
     * @param deleteTables the delete tables
     */
    @SneakyThrows
    public void createTable(final boolean deleteTables) {
        val throughput = ProvisionedThroughput.builder()
            .readCapacityUnits(dynamoDbProperties.getReadCapacity())
            .writeCapacityUnits(dynamoDbProperties.getWriteCapacity())
            .build();

        val request = CreateTableRequest.builder()
            .attributeDefinitions(AttributeDefinition.builder()
                .attributeName(ColumnNames.ID.getColumnName())
                .attributeType(ScalarAttributeType.N).build())
            .keySchema(KeySchemaElement.builder()
                .attributeName(ColumnNames.ID.getColumnName())
                .keyType(KeyType.HASH).build())
            .provisionedThroughput(throughput)
            .billingMode(BillingMode.fromValue(dynamoDbProperties.getBillingMode().name()))
            .tableName(dynamoDbProperties.getTokenTableName())
            .build();

        if (deleteTables) {
            val delete = DeleteTableRequest.builder().tableName(dynamoDbProperties.getTokenTableName()).build();
            LOGGER.debug("Sending delete request [{}] to remove table if necessary", delete);
            DynamoDbTableUtils.deleteTableIfExists(amazonDynamoDBClient, delete);
        }
        LOGGER.debug("Sending create request [{}] to create table", request);
        DynamoDbTableUtils.createTableIfNotExists(amazonDynamoDBClient, request);
        LOGGER.debug("Waiting until table [{}] becomes active...", request.tableName());
        DynamoDbTableUtils.waitUntilActive(amazonDynamoDBClient, request.tableName());
        val describeTableRequest = DescribeTableRequest.builder().tableName(request.tableName()).build();
        LOGGER.debug("Sending request [{}] to obtain table description...", describeTableRequest);
        val tableDescription = amazonDynamoDBClient.describeTable(describeTableRequest).table();
        LOGGER.debug("Located newly created table with description: [{}]", tableDescription);
    }

    /**
     * Find.
     *
     * @param uid the uid
     * @param otp the otp
     * @return the google authenticator token
     */
    public GoogleAuthenticatorToken find(final String uid, final Integer otp) {
        val query =
            List.of(
                DynamoDbQueryBuilder.builder()
                    .key(ColumnNames.USERID.getColumnName())
                    .attributeValue(List.of(AttributeValue.builder().s(uid.toLowerCase()).build()))
                    .operator(ComparisonOperator.EQ)
                    .build(),
                DynamoDbQueryBuilder.builder()
                    .key(ColumnNames.TOKEN.getColumnName())
                    .attributeValue(List.of(AttributeValue.builder().s(String.valueOf(otp)).build()))
                    .operator(ComparisonOperator.EQ)
                    .build());
        val results = getRecordsByKeys(query);
        return results.isEmpty() ? null : results.iterator().next();
    }

    /**
     * Store.
     *
     * @param token the token
     */
    @SneakyThrows
    public void store(final OneTimeToken token) {
        val values = buildTableAttributeValuesMap(token);
        val putItemRequest = PutItemRequest.builder().tableName(dynamoDbProperties.getTokenTableName()).item(values).build();
        LOGGER.debug("Submitting put request [{}] for record [{}]", putItemRequest, token);
        val putItemResult = amazonDynamoDBClient.putItem(putItemRequest);
        LOGGER.debug("Record added with result [{}]", putItemResult);
    }

    /**
     * Count.
     *
     * @return the long
     */
    public long count() {
        val scan = ScanRequest.builder().tableName(dynamoDbProperties.getTokenTableName()).build();
        LOGGER.debug("Scanning table with request [{}] to count items", scan);
        val result = this.amazonDynamoDBClient.scan(scan);
        LOGGER.debug("Scanned table with result [{}]", scan);
        return result.count();
    }

    /**
     * Count.
     *
     * @param uid the uid
     * @return the long
     */
    public long count(final String uid) {
        val query =
            List.of(
                DynamoDbQueryBuilder.builder()
                    .key(ColumnNames.USERID.getColumnName())
                    .attributeValue(List.of(AttributeValue.builder().s(uid.toLowerCase()).build()))
                    .operator(ComparisonOperator.EQ)
                    .build());
        return getRecordsByKeys(query).size();
    }

    /**
     * Remove all.
     */
    public void removeAll() {
        createTable(true);
    }

    /**
     * Remove.
     *
     * @param otp the otp
     */
    public void remove(final Integer otp) {
        val del = DeleteItemRequest.builder()
            .tableName(dynamoDbProperties.getTokenTableName())
            .key(CollectionUtils.wrap(ColumnNames.TOKEN.getColumnName(),
                AttributeValue.builder().s(String.valueOf(otp)).build()))
            .build();
        LOGGER.debug("Submitting delete request [{}] for token [{}]", del, otp);
        val res = amazonDynamoDBClient.deleteItem(del);
        LOGGER.debug("Delete request came back with result [{}]", res);
    }

    /**
     * Remove.
     *
     * @param uid the uid
     */
    public void remove(final String uid) {
        val del = DeleteItemRequest.builder()
            .tableName(dynamoDbProperties.getTokenTableName())
            .key(CollectionUtils.wrap(ColumnNames.USERID.getColumnName(),
                AttributeValue.builder().s(uid.toLowerCase()).build()))
            .build();
        LOGGER.debug("Submitting delete request [{}] for user [{}]", del, uid);
        val res = amazonDynamoDBClient.deleteItem(del);
        LOGGER.debug("Delete request came back with result [{}]", res);
    }

    /**
     * Remove.
     *
     * @param uid the uid
     * @param otp the otp
     */
    public void remove(final String uid, final Integer otp) {
        val del = DeleteItemRequest.builder()
            .tableName(dynamoDbProperties.getTokenTableName())
            .key(CollectionUtils.wrap(ColumnNames.USERID.getColumnName(), AttributeValue.builder().s(uid.toLowerCase()).build()))
            .key(CollectionUtils.wrap(ColumnNames.TOKEN.getColumnName(), AttributeValue.builder().s(String.valueOf(otp)).build()))
            .build();
        LOGGER.debug("Submitting delete request [{}] for user [{}] and token [{}]", del, uid, otp);
        val res = amazonDynamoDBClient.deleteItem(del);
        LOGGER.debug("Delete request came back with result [{}]", res);
    }

    /**
     * Remove from.
     *
     * @param time the time
     */
    public void removeFrom(final LocalDateTime time) {
        val epoch = time.toEpochSecond(ZoneOffset.UTC);
        val query =
            List.of(
                DynamoDbQueryBuilder.builder()
                    .key(ColumnNames.USERID.getColumnName())
                    .attributeValue(List.of(AttributeValue.builder().n(String.valueOf(time)).build()))
                    .operator(ComparisonOperator.GE)
                    .build());
        val records = getRecordsByKeys(query);

        records.forEach(record -> {
            val del = DeleteItemRequest.builder()
                .tableName(dynamoDbProperties.getTokenTableName())
                .key(CollectionUtils.wrap(ColumnNames.TOKEN.getColumnName(),
                    AttributeValue.builder().n(String.valueOf(record.getToken())).build()))
                .build();
            LOGGER.debug("Submitting delete request [{}] since [{}]", del, epoch);
            val res = amazonDynamoDBClient.deleteItem(del);
            LOGGER.debug("Delete request came back with result [{}]", res);
        });
    }

    /**
     * The column names.
     */
    @Getter
    @RequiredArgsConstructor
    public enum ColumnNames {
        /**
         * User id column.
         */
        ID("id"),
        /**
         * User id column.
         */
        USERID("userid"),
        /**
         * id column.
         */
        TOKEN("token"),
        /**
         * creation time column.
         */
        CREATION_TIME("creationTime"),
        /**
         * properties column.
         */
        BODY("body");

        private final String columnName;
    }

    @SneakyThrows
    private Set<GoogleAuthenticatorToken> getRecordsByKeys(final List<DynamoDbQueryBuilder> queries) {
        try {
            val scanRequest = ScanRequest.builder()
                .tableName(dynamoDbProperties.getTokenTableName())
                .scanFilter(queries.stream()
                    .map(query -> {
                        val cond = Condition.builder().comparisonOperator(query.getOperator())
                            .attributeValueList(query.getAttributeValue()).build();
                        return Pair.of(query.getKey(), cond);
                    })
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue)))
                .build();
            LOGGER.debug("Submitting request [{}] to get record with keys [{}]", scanRequest, queries);
            val items = amazonDynamoDBClient.scan(scanRequest).items();
            return items
                .stream()
                .map(GoogleAuthenticatorDynamoDbTokenRepositoryFacilitator::extractAttributeValuesFrom)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return new HashSet<>(0);
    }
}
