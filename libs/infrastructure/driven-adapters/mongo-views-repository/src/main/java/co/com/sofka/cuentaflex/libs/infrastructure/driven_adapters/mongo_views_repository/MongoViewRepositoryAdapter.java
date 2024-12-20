package co.com.sofka.cuentaflex.libs.infrastructure.driven_adapters.mongo_views_repository;

import co.com.sofka.cuentaflex.libs.domain.ports.driven.persistence.ViewRepositoryPort;
import co.com.sofka.cuentaflex.libs.domain.model.accounts_views.AccountTransactionView;
import co.com.sofka.cuentaflex.libs.domain.model.accounts_views.AccountView;
import co.com.sofka.cuentaflex.libs.domain.model.accounts_views.CustomerView;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public class MongoViewRepositoryAdapter implements ViewRepositoryPort {
    private final static String CUSTOMER_VIEW_COLLECTION = "customers";

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public MongoViewRepositoryAdapter(ReactiveMongoTemplate reactiveMongoTemplate) {
        this.reactiveMongoTemplate = reactiveMongoTemplate;
    }

    @Override
    public Mono<Void> saveCustomerView(CustomerView customerView) {
        return this.reactiveMongoTemplate.save(customerView, CUSTOMER_VIEW_COLLECTION).then();
    }

    @Override
    public Mono<Void> saveAccountToCustomerView(String customerId, AccountView accountView) {
        Query query = Query.query(Criteria.where("customerId").is(customerId));
        Update update = new Update().push("accounts", accountView);
        return this.reactiveMongoTemplate.updateFirst(query, update, CUSTOMER_VIEW_COLLECTION).then();
    }

    @Override
    public Mono<Void> saveAccountTransactionToAccountView(String customerId, String accountId, AccountTransactionView accountTransactionView) {
        Query query = Query.query(Criteria.where("customerId").is(customerId)
                .and("accounts.accountId").is(accountId));

        Update update = new Update().push("accounts.$.transactions", accountTransactionView);

        return this.reactiveMongoTemplate.updateFirst(query, update, CUSTOMER_VIEW_COLLECTION).then();
    }

    @Override
    public Mono<Void> updateAccountBalance(String customerId, String accountId, BigDecimal balance) {
        Query query = Query.query(Criteria.where("customerId").is(customerId)
                .and("accounts.accountId").is(accountId));

        Update update = new Update().set("accounts.$.balance", balance);

        return this.reactiveMongoTemplate.updateFirst(query, update, CUSTOMER_VIEW_COLLECTION).then();
    }

    @Override
    public Mono<AccountView> getAccountView(String customerId, String accountId) {
        return this.reactiveMongoTemplate.findOne(
                        Query.query(Criteria.where("customerId").is(customerId)
                                .and("accounts.accountId").is(accountId)),
                        CustomerView.class, CUSTOMER_VIEW_COLLECTION
                )
                .flatMap(customerView -> {
                    if (customerView == null) {
                        return Mono.empty();
                    }
                    return customerView.getAccounts().stream()
                            .filter(account -> account.getAccountId().equals(accountId))
                            .findFirst()
                            .map(Mono::just)
                            .orElse(Mono.empty());
                });
    }

    @Override
    public Mono<CustomerView> getCustomerView(String customerId) {
        Query query = Query.query(Criteria.where("customerId").is(customerId));
        query.fields().exclude("accounts.transactions");

        return reactiveMongoTemplate.findOne(query, CustomerView.class, CUSTOMER_VIEW_COLLECTION);
    }
}
