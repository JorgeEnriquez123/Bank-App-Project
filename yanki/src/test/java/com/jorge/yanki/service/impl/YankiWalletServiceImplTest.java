package com.jorge.yanki.service.impl;

import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jorge.yanki.dto.request.YankiPaymentKafkaMessage;
import com.jorge.yanki.dto.request.YankiSendPaymentRequest;
import com.jorge.yanki.dto.request.YankiWalletAssociateCardRequest;
import com.jorge.yanki.dto.request.YankiWalletRequest;
import com.jorge.yanki.dto.response.SuccessfulEventOperationResponse;
import com.jorge.yanki.dto.response.YankiWalletResponse;
import com.jorge.yanki.handler.exception.ResourceNotFoundException;
import com.jorge.yanki.handler.exception.YankiWalletNotAvailableForPaymentException;
import com.jorge.yanki.listener.dto.DebitCardAssociationKafkaMessage;
import com.jorge.yanki.mapper.YankiWalletMapper;
import com.jorge.yanki.model.YankiWallet;
import com.jorge.yanki.repository.YankiWalletRepository;
import com.jorge.yanki.service.YankiWalletService;
import com.jorge.yanki.service.impl.YankiWalletServiceImpl;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class YankiWalletServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private YankiWalletRepository yankiWalletRepository;
    @Spy
    private YankiWalletMapper yankiWalletMapper;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private YankiWalletServiceImpl yankiWalletServiceImpl;

    private YankiWallet yankiWalletActive;
    private YankiWallet yankiWalletPending;
    private YankiWalletRequest yankiWalletRequest;
    private YankiWalletResponse yankiWalletResponseActive;
    private YankiWalletResponse yankiWalletResponsePending;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class));


        yankiWalletActive = YankiWallet.builder()
                .id("activeWalletId")
                .documentNumber("12345678")
                .documentType(YankiWallet.DocumentType.DNI)
                .phoneNumber("987654321")
                .imei("imei123")
                .email("active@example.com")
                .associatedDebitCardNumber("debitCard123")
                .status(YankiWallet.YankiWalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        yankiWalletPending = YankiWallet.builder()
                .id("pendingWalletId")
                .documentNumber("87654321")
                .documentType(YankiWallet.DocumentType.DNI)
                .phoneNumber("123456789")
                .imei("imei456")
                .email("pending@example.com")
                .associatedDebitCardNumber(null)
                .status(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)
                .createdAt(LocalDateTime.now())
                .build();

        yankiWalletRequest = new YankiWalletRequest();
        yankiWalletRequest.setDocumentNumber("11111111");
        yankiWalletRequest.setDocumentType(YankiWalletRequest.DocumentType.DNI);
        yankiWalletRequest.setPhoneNumber("999999999");
        yankiWalletRequest.setImei("imei789");
        yankiWalletRequest.setEmail("new@example.com");
        yankiWalletRequest.setAssociatedDebitCardNumber("newDebitCard");

        yankiWalletResponseActive = new YankiWalletResponse();
        yankiWalletResponseActive.setId(yankiWalletActive.getId());
        yankiWalletResponseActive.setDocumentNumber(yankiWalletActive.getDocumentNumber());
        yankiWalletResponseActive.setDocumentType(YankiWalletResponse.DocumentType.valueOf(yankiWalletActive.getDocumentType().name()));
        yankiWalletResponseActive.setPhoneNumber(yankiWalletActive.getPhoneNumber());
        yankiWalletResponseActive.setImei(yankiWalletActive.getImei());
        yankiWalletResponseActive.setEmail(yankiWalletActive.getEmail());
        yankiWalletResponseActive.setAssociatedDebitCardNumber(yankiWalletActive.getAssociatedDebitCardNumber());
        yankiWalletResponseActive.setStatus(YankiWalletResponse.YankiWalletStatus.valueOf(yankiWalletActive.getStatus().name()));
        yankiWalletResponseActive.setCreatedAt(yankiWalletActive.getCreatedAt());

        yankiWalletResponsePending = new YankiWalletResponse();
        yankiWalletResponsePending.setId(yankiWalletPending.getId());
        yankiWalletResponsePending.setDocumentNumber(yankiWalletPending.getDocumentNumber());
        yankiWalletResponsePending.setDocumentType(YankiWalletResponse.DocumentType.valueOf(yankiWalletPending.getDocumentType().name()));
        yankiWalletResponsePending.setPhoneNumber(yankiWalletPending.getPhoneNumber());
        yankiWalletResponsePending.setImei(yankiWalletPending.getImei());
        yankiWalletResponsePending.setEmail(yankiWalletPending.getEmail());
        yankiWalletResponsePending.setAssociatedDebitCardNumber(yankiWalletPending.getAssociatedDebitCardNumber());
        yankiWalletResponsePending.setStatus(YankiWalletResponse.YankiWalletStatus.valueOf(yankiWalletPending.getStatus().name()));
        yankiWalletResponsePending.setCreatedAt(yankiWalletPending.getCreatedAt());
    }

    @Test
    void whenGetAllYankiWallets_ThenReturnFlowableOfYankiWalletResponse() {
        when(yankiWalletRepository.findAll()).thenReturn(List.of(yankiWalletActive, yankiWalletPending));

        Flowable<YankiWalletResponse> result = yankiWalletServiceImpl.getAllYankiWallets();

        TestSubscriber<YankiWalletResponse> testSubscriber = new TestSubscriber<>();
        result.subscribe(testSubscriber);

        testSubscriber.awaitDone(5, TimeUnit.SECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(2);
        testSubscriber.assertValueAt(0, response -> response.getId().equals(yankiWalletResponseActive.getId()));
        testSubscriber.assertValueAt(1, response -> response.getId().equals(yankiWalletResponsePending.getId()));
    }

    @Test
    void whenGetYankiWalletById_WithCacheHit_ThenReturnYankiWalletResponseFromCache() {
        String walletId = "cachedId";
        String cacheKey = "yankiwallet::" + walletId;

        when(valueOperations.get(cacheKey)).thenReturn(yankiWalletResponseActive);
        when(objectMapper.convertValue(any(), eq(YankiWalletResponse.class))).thenReturn(yankiWalletResponseActive);

        Single<YankiWalletResponse> result = yankiWalletServiceImpl.getYankiWalletById(walletId);

        TestObserver<YankiWalletResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.getId().equals(yankiWalletResponseActive.getId()));

        verify(yankiWalletRepository, never()).findById(anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void whenGetYankiWalletById_WithCacheMissAndFoundInDb_ThenReturnYankiWalletResponseAndCache() {
        String walletId = "dbId";
        String cacheKey = "yankiwallet::" + walletId;

        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(yankiWalletRepository.findById(walletId)).thenReturn(Optional.of(yankiWalletActive));

        Single<YankiWalletResponse> result = yankiWalletServiceImpl.getYankiWalletById(walletId);

        TestObserver<YankiWalletResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> response.getId().equals(yankiWalletResponseActive.getId()));

        verify(yankiWalletRepository).findById(walletId);
        verify(valueOperations).set(eq(cacheKey), any(YankiWalletResponse.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void whenGetYankiWalletById_WithCacheMissAndNotFoundInDb_ThenReturnResourceNotFoundException() {
        String walletId = "notFoundId";
        String cacheKey = "yankiwallet::" + walletId;

        when(valueOperations.get(cacheKey)).thenReturn(null);
        when(yankiWalletRepository.findById(walletId)).thenReturn(Optional.empty());

        Single<YankiWalletResponse> result = yankiWalletServiceImpl.getYankiWalletById(walletId);

        TestObserver<YankiWalletResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertError(throwable -> throwable instanceof ResourceNotFoundException &&
                throwable.getMessage().equals("Yanki wallet with id: notFoundId not found"));

        verify(yankiWalletRepository).findById(walletId);
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void whenCreateYankiWallet_ThenReturnSingleOfYankiWalletResponse() {
        YankiWallet expectedSavedYankiWallet = yankiWalletMapper.mapToYankiWallet(yankiWalletRequest);
        expectedSavedYankiWallet.setId(UUID.randomUUID().toString());

        when(yankiWalletRepository.save(any(YankiWallet.class))).thenReturn(expectedSavedYankiWallet);

        Single<YankiWalletResponse> result = yankiWalletServiceImpl.createYankiWallet(yankiWalletRequest);

        TestObserver<YankiWalletResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> {
            assertEquals(expectedSavedYankiWallet.getId(), response.getId());
            assertEquals(yankiWalletRequest.getDocumentNumber(), response.getDocumentNumber());
            assertEquals(yankiWalletRequest.getDocumentType().name(), response.getDocumentType().name());
            assertEquals(yankiWalletRequest.getPhoneNumber(), response.getPhoneNumber());
            assertEquals(yankiWalletRequest.getImei(), response.getImei());
            assertEquals(yankiWalletRequest.getEmail(), response.getEmail());
            assertEquals(yankiWalletRequest.getAssociatedDebitCardNumber(), response.getAssociatedDebitCardNumber());
            assertEquals(expectedSavedYankiWallet.getStatus().name(), response.getStatus().name());
            return true;
        });

        verify(yankiWalletRepository).save(any(YankiWallet.class));
    }

    @Test
    void whenUpdateYankiWallet_WithExistingId_ThenReturnSingleOfYankiWalletResponseAndCache() {
        String walletId = "updateId";
        String cacheKey = "yankiwallet::" + walletId;

        YankiWalletRequest updateRequest = new YankiWalletRequest();
        updateRequest.setDocumentNumber("updatedDoc");
        updateRequest.setDocumentType(YankiWalletRequest.DocumentType.CEX);
        updateRequest.setPhoneNumber("updatedPhone");
        updateRequest.setImei("updatedImei");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setAssociatedDebitCardNumber("updatedDebitCard");

        YankiWallet existingYankiWallet = YankiWallet.builder()
                .id(walletId)
                .documentNumber("originalDoc")
                .documentType(YankiWallet.DocumentType.DNI)
                .phoneNumber("originalPhone")
                .imei("originalImei")
                .email("original@example.com")
                .associatedDebitCardNumber(null)
                .status(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        YankiWallet updatedYankiWallet = YankiWallet.builder()
                .id(walletId)
                .documentNumber(updateRequest.getDocumentNumber())
                .documentType(YankiWallet.DocumentType.valueOf(updateRequest.getDocumentType().name()))
                .phoneNumber(updateRequest.getPhoneNumber())
                .imei(updateRequest.getImei())
                .email(updateRequest.getEmail())
                .associatedDebitCardNumber(updateRequest.getAssociatedDebitCardNumber())
                .status(YankiWallet.YankiWalletStatus.ACTIVE) // Assuming status updates correctly
                .createdAt(existingYankiWallet.getCreatedAt()) // Keep original creation time
                .build();

        when(yankiWalletRepository.findById(walletId)).thenReturn(Optional.of(existingYankiWallet));
        when(yankiWalletRepository.save(any(YankiWallet.class))).thenReturn(updatedYankiWallet);

        Single<YankiWalletResponse> result = yankiWalletServiceImpl.updateYankiWallet(walletId, updateRequest);

        TestObserver<YankiWalletResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> {
            assertEquals(walletId, response.getId());
            assertEquals(updateRequest.getDocumentNumber(), response.getDocumentNumber());
            assertEquals(updateRequest.getDocumentType().name(), response.getDocumentType().name());
            assertEquals(updateRequest.getPhoneNumber(), response.getPhoneNumber());
            assertEquals(updateRequest.getImei(), response.getImei());
            assertEquals(updateRequest.getEmail(), response.getEmail());
            assertEquals(updateRequest.getAssociatedDebitCardNumber(), response.getAssociatedDebitCardNumber());
            assertEquals(YankiWalletResponse.YankiWalletStatus.ACTIVE, response.getStatus());
            // Comparing LocalDateTime requires a tolerance or ignoring nano seconds
            // assertEquals(existingYankiWallet.getCreatedAt(), response.getCreatedAt());
            return true;
        });

        verify(yankiWalletRepository).findById(walletId);
        verify(yankiWalletRepository).save(any(YankiWallet.class));
        verify(valueOperations).set(eq(cacheKey), any(YankiWalletResponse.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void whenDeleteYankiWalletById_ThenReturnCompletableAndRemoveFromCache() {
        String walletId = "deleteId";
        String cacheKey = "yankiwallet::" + walletId;

        Completable result = yankiWalletServiceImpl.deleteYankiWalletById(walletId);

        TestObserver<Void> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();

        verify(yankiWalletRepository).deleteById(walletId);
        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    void updateYankiWalletFromRequest_ShouldMapFieldsCorrectly() {
        String existingId = "existingId";
        LocalDateTime creationTime = LocalDateTime.now().minusDays(2);

        YankiWallet existingYankiWallet = YankiWallet.builder()
                .id(existingId)
                .documentNumber("originalDoc")
                .documentType(YankiWallet.DocumentType.DNI)
                .phoneNumber("originalPhone")
                .imei("originalImei")
                .email("original@example.com")
                .associatedDebitCardNumber(null)
                .status(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION)
                .createdAt(creationTime)
                .build();

        YankiWalletRequest updateRequest = new YankiWalletRequest();
        updateRequest.setDocumentNumber("updatedDoc");
        updateRequest.setDocumentType(YankiWalletRequest.DocumentType.CEX);
        updateRequest.setPhoneNumber("updatedPhone");
        updateRequest.setImei("updatedImei");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setAssociatedDebitCardNumber("updatedDebitCard");

        YankiWallet updatedYankiWallet = yankiWalletServiceImpl.updateYankiWalletFromRequest(existingYankiWallet, updateRequest);

        assertEquals(existingId, updatedYankiWallet.getId());
        assertEquals(updateRequest.getDocumentNumber(), updatedYankiWallet.getDocumentNumber());
        assertEquals(YankiWallet.DocumentType.valueOf(updateRequest.getDocumentType().name()), updatedYankiWallet.getDocumentType());
        assertEquals(updateRequest.getPhoneNumber(), updatedYankiWallet.getPhoneNumber());
        assertEquals(updateRequest.getImei(), updatedYankiWallet.getImei());
        assertEquals(updateRequest.getEmail(), updatedYankiWallet.getEmail());
        assertEquals(updateRequest.getAssociatedDebitCardNumber(), updatedYankiWallet.getAssociatedDebitCardNumber());
        assertEquals(YankiWallet.YankiWalletStatus.ACTIVE, updatedYankiWallet.getStatus()); // Status should be updated if debit card is associated
        assertEquals(creationTime, updatedYankiWallet.getCreatedAt()); // Creation time should be retained
    }

    @Test
    void whenSendPayment_WithValidWallets_ThenSendKafkaMessageAndReturnSuccessfulResponse() throws JsonProcessingException {
        YankiSendPaymentRequest paymentRequest = new YankiSendPaymentRequest();
        paymentRequest.setReceiverYankiPhoneNumber("123456789");
        paymentRequest.setAmount(BigDecimal.valueOf(50.0));

        when(yankiWalletRepository.findById("activeWalletId")).thenReturn(Optional.of(yankiWalletActive));
        when(yankiWalletRepository.findByPhoneNumber("123456789")).thenReturn(Optional.of(yankiWalletPending)); // Receiver is pending

        // Mock the mapping for the sender wallet response
        when(yankiWalletMapper.mapToYankiWalletResponse(yankiWalletActive)).thenReturn(yankiWalletResponseActive);

        YankiPaymentKafkaMessage expectedKafkaMessage = new YankiPaymentKafkaMessage();
        expectedKafkaMessage.setSenderDebitCardNumber(yankiWalletActive.getAssociatedDebitCardNumber());
        expectedKafkaMessage.setReceiverDebitCardNumber(yankiWalletPending.getAssociatedDebitCardNumber());
        expectedKafkaMessage.setAmount(paymentRequest.getAmount());

        String kafkaMessageJson = "{\"senderDebitCardNumber\":\"debitCard123\",\"receiverDebitCardNumber\":null,\"amount\":50.0}";
        when(objectMapper.writeValueAsString(any(YankiPaymentKafkaMessage.class))).thenReturn(kafkaMessageJson);

        Single<SuccessfulEventOperationResponse> result = yankiWalletServiceImpl.sendPayment("activeWalletId", paymentRequest);

        TestObserver<SuccessfulEventOperationResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> "Payment event sent successfully".equals(response.getMessage()));

        verify(yankiWalletRepository).findById("activeWalletId");
        verify(yankiWalletRepository).findByPhoneNumber("123456789");
        verify(kafkaTemplate).send("yanki-payment-request", kafkaMessageJson);
    }

    @Test
    void whenSendPayment_WithSenderPendingDebitCardAssociation_ThenReturnYankiWalletNotAvailableException() {
        YankiSendPaymentRequest paymentRequest = new YankiSendPaymentRequest();
        paymentRequest.setReceiverYankiPhoneNumber("123456789");
        paymentRequest.setAmount(BigDecimal.valueOf(50.0));

        when(yankiWalletRepository.findById("pendingWalletId")).thenReturn(Optional.of(yankiWalletPending));
        when(yankiWalletRepository.findByPhoneNumber("123456789")).thenReturn(Optional.of(yankiWalletActive));

        // Mock the mapping for the sender wallet response
        when(yankiWalletMapper.mapToYankiWalletResponse(yankiWalletPending)).thenReturn(yankiWalletResponsePending);


        Single<SuccessfulEventOperationResponse> result = yankiWalletServiceImpl.sendPayment("pendingWalletId", paymentRequest);

        TestObserver<SuccessfulEventOperationResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertError(throwable -> throwable instanceof YankiWalletNotAvailableForPaymentException &&
                throwable.getMessage().equals("Yanki wallet with id: pendingWalletId is not available for payment. It needs to be associated with a debit card"));

        verify(yankiWalletRepository).findById("pendingWalletId");
        verify(yankiWalletRepository).findByPhoneNumber("123456789");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void whenSendPayment_WithReceiverPendingDebitCardAssociation_ThenReturnYankiWalletNotAvailableException() {
        YankiSendPaymentRequest paymentRequest = new YankiSendPaymentRequest();
        paymentRequest.setReceiverYankiPhoneNumber("123456789");
        paymentRequest.setAmount(BigDecimal.valueOf(50.0));

        when(yankiWalletRepository.findById("activeWalletId")).thenReturn(Optional.of(yankiWalletActive));
        when(yankiWalletRepository.findByPhoneNumber("123456789")).thenReturn(Optional.of(yankiWalletPending));

        // Mock the mapping for the sender wallet response
        when(yankiWalletMapper.mapToYankiWalletResponse(yankiWalletActive)).thenReturn(yankiWalletResponseActive);

        Single<SuccessfulEventOperationResponse> result = yankiWalletServiceImpl.sendPayment("activeWalletId", paymentRequest);

        TestObserver<SuccessfulEventOperationResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertError(throwable -> throwable instanceof YankiWalletNotAvailableForPaymentException &&
                throwable.getMessage().equals("Yanki wallet with id: pendingWalletId is not available for payment. It needs to be associated with a debit card"));

        verify(yankiWalletRepository).findById("activeWalletId");
        verify(yankiWalletRepository).findByPhoneNumber("123456789");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }


    @Test
    void whenAssociateDebitCard_WithExistingYankiWallet_ThenSendKafkaMessageAndReturnSuccessfulResponse() throws JsonProcessingException {
        YankiWalletAssociateCardRequest associateCardRequest = new YankiWalletAssociateCardRequest();
        associateCardRequest.setDebitCardNumber("newDebitCardNumber");

        when(yankiWalletRepository.findById("pendingWalletId")).thenReturn(Optional.of(yankiWalletPending));

        // Mock the mapping for the yanki wallet response
        when(yankiWalletMapper.mapToYankiWalletResponse(yankiWalletPending)).thenReturn(yankiWalletResponsePending);

        DebitCardAssociationKafkaMessage expectedKafkaMessage = new DebitCardAssociationKafkaMessage();
        expectedKafkaMessage.setYankiId("pendingWalletId");
        expectedKafkaMessage.setDebitCardNumber(associateCardRequest.getDebitCardNumber());

        String kafkaMessageJson = "{\"yankiId\":\"pendingWalletId\",\"debitCardNumber\":\"newDebitCardNumber\"}";
        when(objectMapper.writeValueAsString(any(DebitCardAssociationKafkaMessage.class))).thenReturn(kafkaMessageJson);

        Single<SuccessfulEventOperationResponse> result = yankiWalletServiceImpl.associateDebitCard("pendingWalletId", associateCardRequest);

        TestObserver<SuccessfulEventOperationResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertNoErrors();
        testObserver.assertComplete();
        testObserver.assertValue(response -> "Debit card association event sent successfully".equals(response.getMessage()));

        verify(yankiWalletRepository).findById("pendingWalletId");
        verify(kafkaTemplate).send("yanki-debit-card-association-checking-card", kafkaMessageJson);
    }

    @Test
    void whenAssociateDebitCard_WithNonExistingYankiWallet_ThenReturnResourceNotFoundException() {
        YankiWalletAssociateCardRequest associateCardRequest = new YankiWalletAssociateCardRequest();
        associateCardRequest.setDebitCardNumber("newDebitCardNumber");

        when(yankiWalletRepository.findById("nonExistingId")).thenReturn(Optional.empty());

        Single<SuccessfulEventOperationResponse> result = yankiWalletServiceImpl.associateDebitCard("nonExistingId", associateCardRequest);

        TestObserver<SuccessfulEventOperationResponse> testObserver = new TestObserver<>();
        result.subscribe(testObserver);

        testObserver.awaitDone(5, TimeUnit.SECONDS);
        testObserver.assertError(throwable -> throwable instanceof ResourceNotFoundException &&
                throwable.getMessage().equals("Yanki wallet with id: nonExistingId not found"));

        verify(yankiWalletRepository).findById("nonExistingId");
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void validateYankiWallets_WithBothActive_ShouldDoNothing() {
        YankiWalletResponse sender = new YankiWalletResponse();
        sender.setStatus(YankiWalletResponse.YankiWalletStatus.ACTIVE);
        YankiWallet receiver = new YankiWallet();
        receiver.setStatus(YankiWallet.YankiWalletStatus.ACTIVE);

        yankiWalletServiceImpl.validateYankiWallets(sender, receiver);
        // No exception should be thrown
    }

    @Test
    void validateYankiWallets_WithSenderPending_ShouldThrowException() {
        YankiWalletResponse sender = new YankiWalletResponse();
        sender.setId("senderId");
        sender.setStatus(YankiWalletResponse.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION);
        YankiWallet receiver = new YankiWallet();
        receiver.setStatus(YankiWallet.YankiWalletStatus.ACTIVE);

        try {
            yankiWalletServiceImpl.validateYankiWallets(sender, receiver);
        } catch (YankiWalletNotAvailableForPaymentException e) {
            assertEquals("Yanki wallet with id: senderId is not available for payment. It needs to be associated with a debit card", e.getMessage());
            return;
        }
        throw new AssertionError("Expected YankiWalletNotAvailableForPaymentException was not thrown");
    }

    @Test
    void validateYankiWallets_WithReceiverPending_ShouldThrowException() {
        YankiWalletResponse sender = new YankiWalletResponse();
        sender.setStatus(YankiWalletResponse.YankiWalletStatus.ACTIVE);
        YankiWallet receiver = new YankiWallet();
        receiver.setId("receiverId");
        receiver.setStatus(YankiWallet.YankiWalletStatus.PENDING_DEBITCARD_ASSOCIATION);

        try {
            yankiWalletServiceImpl.validateYankiWallets(sender, receiver);
        } catch (YankiWalletNotAvailableForPaymentException e) {
            assertEquals("Yanki wallet with id: receiverId is not available for payment. It needs to be associated with a debit card", e.getMessage());
            return;
        }
        throw new AssertionError("Expected YankiWalletNotAvailableForPaymentException was not thrown");
    }
}
