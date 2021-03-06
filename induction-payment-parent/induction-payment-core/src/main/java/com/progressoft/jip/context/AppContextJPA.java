package com.progressoft.jip.context;

import java.util.List;

import com.progressoft.jip.beans.Account;
import com.progressoft.jip.beans.PaymentPurpose;
import com.progressoft.jip.beans.PaymentRequest;
import com.progressoft.jip.gateway.sql.YahooCurrencyExchangeRateGateway;
import com.progressoft.jip.gateways.jpa.AccountJpaGateway;
import com.progressoft.jip.gateways.jpa.CurrencyJpaGateway;
import com.progressoft.jip.gateways.jpa.PaymentPurposeJpaGateway;
import com.progressoft.jip.gateways.jpa.PaymentRequestJpaGateway;
import com.progressoft.jip.gateways.jpa.converters.BeanToEntityConverter;
import com.progressoft.jip.gateways.jpa.converters.EntityToBeanConverter;
import com.progressoft.jip.gateways.jpa.converters.impl.BeanToEntityConverterImpl;
import com.progressoft.jip.gateways.jpa.converters.impl.EntityToBeanConvertorImpl;
import com.progressoft.jip.handlers.AccountHandler;
import com.progressoft.jip.handlers.PaymentPurposeHandler;
import com.progressoft.jip.handlers.PaymentRequestHandler;
import com.progressoft.jip.handlers.exceptions.PurposeValidationException;
import com.progressoft.jip.handlers.exceptions.ValidationException;
import com.progressoft.jip.handlers.impl.AccountHandlerImpl;
import com.progressoft.jip.handlers.impl.PaymentPurposeHandlerImpl;
import com.progressoft.jip.handlers.impl.PaymentRequestHandlerImpl;
import com.progressoft.jip.handlers.validators.Validator;
import com.progressoft.jip.handlers.validators.impl.AccountValidator;
import com.progressoft.jip.handlers.validators.impl.PaymentPurposeValidator;
import com.progressoft.jip.handlers.validators.impl.PaymentRequestValidator;
import com.progressoft.jip.iban.IBANGeneralValidator;
import com.progressoft.jip.jparepositories.AccountJpaRepository;
import com.progressoft.jip.jparepositories.CurrencyJpaRepository;
import com.progressoft.jip.jparepositories.PaymentPurposeJpaRepository;
import com.progressoft.jip.jparepositories.PaymentRequestJpaRepository;
import com.progressoft.jip.jparepositories.impl.AccountJpaRepositoryImpl;
import com.progressoft.jip.jparepositories.impl.CurrencyJpaRepositoryImpl;
import com.progressoft.jip.jparepositories.impl.PaymentPurposeJpaRepositoryImpl;
import com.progressoft.jip.jparepositories.impl.PaymentRequestJpaRepositoryImpl;
import com.progressoft.jip.report.ReportProvider;
import com.progressoft.jip.repository.AccountRepository;
import com.progressoft.jip.repository.CurrencyExchangeRateRepository;
import com.progressoft.jip.repository.CurrencyRepository;
import com.progressoft.jip.repository.PaymentPurposeRepository;
import com.progressoft.jip.repository.PaymentRequestRepository;
import com.progressoft.jip.repository.impl.AccountRepositoryImpl;
import com.progressoft.jip.repository.impl.CurrencyExchangeRateRepositoryImpl;
import com.progressoft.jip.repository.impl.CurrencyRepositoryImpl;
import com.progressoft.jip.repository.impl.PaymentPurposeRepositoryImpl;
import com.progressoft.jip.repository.impl.PaymentRequestRepositoryImpl;
import com.progressoft.jip.rules.impl.FiveDaysAheadRule;
import com.progressoft.jip.rules.impl.FiveMonthsAheadRule;
import com.progressoft.jip.rules.impl.FiveYearsAheadRule;
import com.progressoft.jip.rules.impl.PaymentRules;
import com.progressoft.jip.usecases.AccountUseCases;
import com.progressoft.jip.usecases.CurrencyUseCases;
import com.progressoft.jip.usecases.PaymentPurposeUseCases;
import com.progressoft.jip.usecases.PaymentRequestUseCases;
import com.progressoft.jip.usecases.impl.AccountUseCasesImpl;
import com.progressoft.jip.usecases.impl.CurrencyUseCasesImpl;
import com.progressoft.jip.usecases.impl.PaymentPurposeUseCasesImpl;
import com.progressoft.jip.usecases.impl.PaymentRequestUseCasesImpl;
import com.progressoft.jip.utilities.chequewriting.impl.AbstractAmountWriter;
import com.progressoft.jip.utilities.chequewriting.impl.EnglishChequeAmountWriter;
import com.progressoft.jip.utilities.restful.RestfulResponseFormat;
import com.progressoft.jip.utilities.restful.impl.YahooCurrenciesXmlResponseParser;

public class AppContextJPA implements AppContext {

	private static PaymentRules rules = PaymentRules.getInstance();
	private static AbstractAmountWriter amountWriter = new AbstractAmountWriter();

	private final BeanToEntityConverter beanToEntityConverter;
	private final EntityToBeanConverter entityToBeanConverter;

	private final IBANGeneralValidator ibanGeneralValidator;
	private final Validator<Account, ValidationException> accountValidator;
	private final Validator<PaymentPurpose, PurposeValidationException> paymentPurposeValidator;
	private final Validator<PaymentRequest, ValidationException> paymentRequestValidator;

	private final AccountHandler accountHandler;
	private final PaymentPurposeHandler paymentPurposeHandler;
	private final PaymentRequestHandler paymentRequestHandler;

	private final PaymentPurposeJpaRepository paymentPurposeJpaRepository;
	private final PaymentRequestJpaRepository paymentRequestJpaRepository;
	private final AccountJpaRepository accountJpaRepository;
	private final CurrencyJpaRepository currencyJpaRepository;

	private final PaymentRequestRepository paymentRequestRepository;
	private final PaymentPurposeRepository paymentPurposeRepository;
	private final CurrencyRepository currencyRepository;
	private final AccountRepository accountRepository;
	private final CurrencyExchangeRateRepository currenctExchangeRateRepository;

	private final AccountUseCases accountUseCases;
	private final CurrencyUseCases currencyUseCases;
	private final PaymentPurposeUseCases paymentPurposeUseCases;
	private final PaymentRequestUseCases paymentRequestUseCases;

	public AppContextJPA() {
		rules.registerRule("five.years.ahead", new FiveYearsAheadRule());
		rules.registerRule("five.months.ahead", new FiveMonthsAheadRule());
		rules.registerRule("five.days.ahead", new FiveDaysAheadRule());

		amountWriter.addWriter("EnglishWriter", new EnglishChequeAmountWriter());

		paymentPurposeJpaRepository = new PaymentPurposeJpaRepositoryImpl();
		paymentRequestJpaRepository = new PaymentRequestJpaRepositoryImpl();
		accountJpaRepository = new AccountJpaRepositoryImpl();
		currencyJpaRepository = new CurrencyJpaRepositoryImpl();

		beanToEntityConverter = new BeanToEntityConverterImpl(accountJpaRepository, paymentPurposeJpaRepository,
				currencyJpaRepository);
		entityToBeanConverter = new EntityToBeanConvertorImpl();

		paymentPurposeRepository = new PaymentPurposeRepositoryImpl(new PaymentPurposeJpaGateway(entityToBeanConverter,
				beanToEntityConverter, paymentPurposeJpaRepository));
		paymentRequestRepository = new PaymentRequestRepositoryImpl(new PaymentRequestJpaGateway(entityToBeanConverter,
				beanToEntityConverter, paymentRequestJpaRepository));
		accountRepository = new AccountRepositoryImpl(
				new AccountJpaGateway(entityToBeanConverter, beanToEntityConverter, accountJpaRepository));
		currencyRepository = new CurrencyRepositoryImpl(
				new CurrencyJpaGateway(entityToBeanConverter, currencyJpaRepository));

		currenctExchangeRateRepository = new CurrencyExchangeRateRepositoryImpl(new YahooCurrencyExchangeRateGateway(
				RestfulResponseFormat.XML, new YahooCurrenciesXmlResponseParser()));

		ibanGeneralValidator = new IBANGeneralValidator();
		accountValidator = new AccountValidator(ibanGeneralValidator);
		paymentPurposeValidator = new PaymentPurposeValidator();
		paymentRequestValidator = new PaymentRequestValidator(rules, accountRepository, paymentPurposeRepository,
				ibanGeneralValidator, paymentPurposeValidator);

		accountHandler = new AccountHandlerImpl(accountValidator);
		paymentPurposeHandler = new PaymentPurposeHandlerImpl(paymentPurposeValidator);
		paymentRequestHandler = new PaymentRequestHandlerImpl(currenctExchangeRateRepository, paymentRequestValidator);

		accountUseCases = new AccountUseCasesImpl(accountRepository, accountHandler);
		currencyUseCases = new CurrencyUseCasesImpl(currencyRepository);
		paymentPurposeUseCases = new PaymentPurposeUseCasesImpl(paymentPurposeRepository, paymentPurposeHandler);
		paymentRequestUseCases = new PaymentRequestUseCasesImpl(paymentRequestHandler, accountRepository,
				paymentRequestRepository, new ReportProvider(paymentRequestRepository));

	}

	@Override
	public PaymentRequestUseCases getPaymentRequestUseCases() {
		return paymentRequestUseCases;
	}

	@Override
	public PaymentPurposeUseCases getPaymentPurposeUseCases() {
		return paymentPurposeUseCases;
	}

	@Override
	public CurrencyUseCases getCurrencyUseCases() {
		return currencyUseCases;
	}

	@Override
	public AccountUseCases getAccountUseCases() {
		return accountUseCases;
	}

	@Override
	public List<String> getPaymentRuleNames() {
		return rules.getRuleNames();
	}

	@Override
	public AbstractAmountWriter getAbstractAmountWriter() {
		return amountWriter;
	}

}
