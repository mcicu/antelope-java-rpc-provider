package io.antelope.javarpcprovider;

import okhttp3.RequestBody;
import io.antelope.javasdk.error.EosioError;
import io.antelope.javasdk.error.serializationProvider.SerializationProviderError;
import io.antelope.javasdk.error.session.TransactionPrepareError;
import io.antelope.javasdk.error.session.TransactionSignAndBroadCastError;
import io.antelope.javasdk.implementations.ABIProviderImpl;
import io.antelope.javasdk.interfaces.IABIProvider;
import io.antelope.javasdk.interfaces.IRPCProvider;
import io.antelope.javasdk.interfaces.ISerializationProvider;
import io.antelope.javasdk.interfaces.ISignatureProvider;
import io.antelope.javasdk.models.rpcProvider.Action;
import io.antelope.javasdk.models.rpcProvider.Authorization;
import io.antelope.javasdk.models.rpcProvider.response.Detail;
import io.antelope.javasdk.models.rpcProvider.response.RPCResponseError;
import io.antelope.javasdk.models.rpcProvider.response.SendTransactionResponse;
import io.antelope.javasdk.session.TransactionProcessor;
import io.antelope.javasdk.session.TransactionSession;
import io.antelope.antelopejavaabieosserializationprovider.AbiEosSerializationProviderImpl;
import io.antelope.javarpcprovider.error.EosioJavaRpcProviderCallError;
import io.antelope.javarpcprovider.error.EosioJavaRpcProviderInitializerError;
import io.antelope.javarpcprovider.implementations.EosioJavaRpcProviderImpl;
import io.antelope.softkeysignatureprovider.SoftKeySignatureProviderImpl;
import io.antelope.softkeysignatureprovider.error.ImportKeyError;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

// Comment this annotation to run the tests.
@Ignore
public class LiveServerUnitTest {
    private JSONParser parser;
    private ISerializationProvider serializationProvider;
    private IRPCProvider rpcProvider;
    private IABIProvider abiProvider;
    private ISignatureProvider signatureProvider;
    private TransactionSession session;
    private TransactionProcessor processor;

    private static final String nodeUrl = "http://my.test.blockchain";
    private static final boolean ENABLE_NETWORK_LOG = true;
    private static final String privateKey = "MyTestKey";

    @Before
    public void setup() {
        this.parser = new JSONParser();

        try {
            serializationProvider = new AbiEosSerializationProviderImpl();
        } catch (SerializationProviderError serializationProviderError) {
            fail(serializationProviderError.getMessage());
            serializationProviderError.printStackTrace();
            return;
        }

        try {
            rpcProvider = new EosioJavaRpcProviderImpl(nodeUrl, ENABLE_NETWORK_LOG);
        } catch (EosioJavaRpcProviderInitializerError eosioJavaRpcProviderInitializerError) {
            eosioJavaRpcProviderInitializerError.printStackTrace();
            fail(eosioJavaRpcProviderInitializerError.getMessage());
            return;
        }

        abiProvider = new ABIProviderImpl(rpcProvider, serializationProvider);
        signatureProvider = new SoftKeySignatureProviderImpl();

        try {
            ((SoftKeySignatureProviderImpl) signatureProvider).importKey(privateKey);
        } catch (ImportKeyError importKeyError) {
            importKeyError.printStackTrace();
            fail(importKeyError.getMessage());
            return;
        }

        // Creating TransactionProcess
        session = new TransactionSession(serializationProvider, rpcProvider, abiProvider, signatureProvider);
        processor = session.getTransactionProcessor();
    }

    @After
    public void cleanup() {
        // Must destroy the abieos serialization provider context between tests or we will leak memory.
        AbiEosSerializationProviderImpl serializationProviderImpl = (AbiEosSerializationProviderImpl)serializationProvider;
        serializationProviderImpl.destroyContext();
    }

    @Test
    public void tokenTransferTest() {
        String jsonData = "{\n" +
                "\"from\": \"" + "bob" + "\",\n" +
                "\"to\": \"" + "alice" + "\",\n" +
                "\"quantity\": \"" + "1.1234 SYS" + "\",\n" +
                "\"memo\" : \"" + "hello" + "\"\n" +
                "}";

        Action action = new Action("eosio.token", "transfer", Collections.singletonList(new Authorization("bob", "active")), jsonData);
        int index = 0;
        try {
            processor.prepare(Collections.singletonList(action));
            SendTransactionResponse response = processor.signAndBroadcast();
            assertNotNull("Transaction Id should not be null.", response.getTransactionId() );
            System.out.println("Finished!  Your transaction id is:  " + response.getTransactionId());
        } catch (TransactionPrepareError transactionPrepareError) {
            transactionPrepareError.printStackTrace();
            fail(transactionPrepareError.getLocalizedMessage());
        } catch (TransactionSignAndBroadCastError transactionSignAndBroadCastError) {
            transactionSignAndBroadCastError.printStackTrace();

            RPCResponseError rpcResponseError = ErrorUtils.getBackendError(transactionSignAndBroadCastError);
            if (rpcResponseError != null) {
                String backendErrorMessage = ErrorUtils.getBackendErrorMessageFromResponse(rpcResponseError);
                fail(backendErrorMessage);
            }
            fail(transactionSignAndBroadCastError.getMessage());
        }
    }

    @Test
    public void actionReturnTest() {
        String jsonData = "{}";

        Action action = new Action("returnvalue", "actionresret", Collections.singletonList(new Authorization("bob", "active")), jsonData);
        int index = 0;
        try {
            processor.prepare(Collections.singletonList(action));
            SendTransactionResponse response = processor.signAndBroadcast();
            Double actionReturnValue = response.getActionValueAtIndex(index, Double.class);
            assertNotNull("Transaction Id should not be null.", response.getTransactionId());
            System.out.println("Your transaction id is:  " + response.getTransactionId());
            assertEquals(new Double(10.0), actionReturnValue);
            System.out.println("Finished!  Your action return value is:  " + actionReturnValue.toString());
        } catch (TransactionPrepareError transactionPrepareError) {
            transactionPrepareError.printStackTrace();
            fail(transactionPrepareError.getLocalizedMessage());
        } catch (TransactionSignAndBroadCastError transactionSignAndBroadCastError) {
            transactionSignAndBroadCastError.printStackTrace();

            RPCResponseError rpcResponseError = ErrorUtils.getBackendError(transactionSignAndBroadCastError);
            if (rpcResponseError != null) {
                String backendErrorMessage = ErrorUtils.getBackendErrorMessageFromResponse(rpcResponseError);
                fail(backendErrorMessage);
            }

            fail(transactionSignAndBroadCastError.getMessage());
        } catch (IndexOutOfBoundsException outOfBoundsError) {
            fail("No action value at index: " + index);
        } catch (ClassCastException castError) {
            fail("Cannot cast action value to requested class");
        }
    }

    @Test
    public void getKvTableRowsTest() {
        String getKvTablesRequestJson = "{\n" +
                "    \"json\" : true\n" +
                "    \"code\" : \"kvaddrbook\"\n" +
                "    \"table\" : \"kvaddrbook\"\n" +
                "    \"encode_type\" : \"name\"\n" +
                "    \"index_name\" : \"accname\"\n" +
                "    \"lower_bound\" : \"jane\"\n" +
                "    \"reverse\" : false\n" +
                "}";


        EosioJavaRpcProviderImpl fullRpcProvider = (EosioJavaRpcProviderImpl)rpcProvider;

        RequestBody requestBody = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), getKvTablesRequestJson);
        try {
            String response = fullRpcProvider.getKvTableRows(requestBody);
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject)parser.parse(response);
            JSONArray jsonArray = (JSONArray)jsonObject.get("rows");
            // Right now we can only get back the serialized form of the rows so we're only checking that.
            String jsonArrayStr = jsonArray.toString();
            assertEquals(4, jsonArray.size());
            JSONObject entry1 = (JSONObject)jsonArray.get(0);
            assertEquals("jane", (String) entry1.get("account_name"));
            assertEquals("Jane", (String) entry1.get("first_name"));
            assertEquals("Doe", (String) entry1.get("last_name"));
            System.out.println("Finished! Got back rows:" + jsonArrayStr);
        } catch (Exception getKvTableRowError) {
            fail("Error getting kv table rows: " + getKvTableRowError.getLocalizedMessage());
        }
    }

    static class ErrorUtils {

        /**
         * Recursively look for a specific error inside causes loop of an EosioError
         *
         * @param errorClass - the error class to find
         * @param error      - the error object to search
         * @param <T>        - the generic class which extends from EosioError
         * @return the error which class is specified by input. Return null if could not find the specific class.
         */
        public static <T extends Exception> T getErrorObject(Class errorClass, Exception error) {
            if (error.getClass() == errorClass) {
                return (T) error;
            }

            if (error.getCause() == null) {
                return null;
            }

            // Recursively look deeper
            return getErrorObject(errorClass, (Exception) error.getCause());
        }

        /**
         * Recursively look for the error message of a specific error inside causes loop of an EosioError
         *
         * @param errorClass - the error class to get the message
         * @param error      - the error object to search
         * @return the error message which class is specified by input. Return the root cause message if could not find the specific class.
         */
        public static String getError(Class errorClass, EosioError error) {
            if (error.getClass() == errorClass || error.getCause() == null) {
                return error.getMessage();
            }

            return getError(errorClass, (EosioError) error.getCause());
        }

        /**
         * Get backend error class {@link RPCResponseError} if an backend error is available
         *
         * @param error the error class to get the backend error
         * @return {@link RPCResponseError} object. Return null if input error does not contain any backend error.
         */
        public static RPCResponseError getBackendError(EosioError error) {
            EosioJavaRpcProviderCallError rpcError = ErrorUtils.getErrorObject(EosioJavaRpcProviderCallError.class, error);
            if (rpcError != null) {
                return rpcError.getRpcResponseError();
            }

            return null;
        }

        /**
         * Format and return a back end error message from a {@link RPCResponseError} object
         *
         * @param error the backend error
         * @return Formatted backend error message from input
         */
        public static String getBackendErrorMessageFromResponse(RPCResponseError error) {
            StringBuilder detail = new StringBuilder();
            if (!error.getError().getDetails().isEmpty()) {
                for (Detail errorDetail : error.getError().getDetails()) {
                    detail.append(errorDetail.getMessage()).append(" - ");
                }
            }

            return error.getMessage() + " - Code: " + error.getError().getCode() + " - What " + error.getError().getCode() + " - detail: " + detail.toString();
        }
    }
}
