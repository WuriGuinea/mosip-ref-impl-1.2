/**
 * 
 */
package io.mosip.kernel.smsserviceprovider.nimba.impl;

import java.io.IOException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.mosip.kernel.core.exception.UnsupportedEncodingException;
import io.mosip.kernel.core.notification.exception.InvalidNumberException;
import io.mosip.kernel.core.notification.model.SMSResponseDto;
import io.mosip.kernel.core.notification.spi.SMSServiceProvider;
import io.mosip.kernel.core.util.StringUtils;
import io.mosip.kernel.smsserviceprovider.nimba.constant.SmsExceptionConstant;
import io.mosip.kernel.smsserviceprovider.nimba.constant.SmsPropertyConstant;

/**
 * @author CONDEIS
 * @since 1.2.0-B1
 */
@Component
public class SMSServiceProviderImpl implements SMSServiceProvider {

	static Logger logger = LoggerFactory.getLogger(SMSServiceProviderImpl.class);

	@Autowired
	RestTemplate restTemplate;

	@Value("${mosip.kernel.sms.country.code}")
	private String countryCode;
	@Value("${mosip.kernel.sms.api}")
	private String apiUrl;
	@Value("${mosip.kernel.sms.sender}")
	private String senderId;
	@Value("${mosip.kernel.sms.unicode:1}")
	private String unicode;
	@Value("${mosip.kernel.sms.number.length}")
	private int numberLength;
	@Value("${mosip.kernel.sms.authkey}")
	private String authKey;

	@Override
	public SMSResponseDto sendSms(String contactNumber, String message) {
		logger.info("Sending SMS to *** *** " + contactNumber.substring(6));
		SMSResponseDto smsResponseDTO = new SMSResponseDto();
		validateInput(contactNumber);
		try {
			NimbaMessageRequest.send(apiUrl, authKey, senderId, contactNumber, message);
		} catch (HttpClientErrorException | HttpServerErrorException | JSONException | UnsupportedEncodingException
				| IOException e) {
			logger.error("Error occured while sending SMS " + e.getMessage());
			throw new RuntimeException(((RestClientResponseException) e).getResponseBodyAsString());
		}
		smsResponseDTO.setMessage(SmsPropertyConstant.SUCCESS_RESPONSE.getProperty());
		smsResponseDTO.setStatus("success");
		return smsResponseDTO;
	}

	private void validateInput(String contactNumber) {
		if (!StringUtils.isNumeric(contactNumber) || contactNumber.length() < numberLength
				|| contactNumber.length() > numberLength) {
			throw new InvalidNumberException(SmsExceptionConstant.SMS_INVALID_CONTACT_NUMBER.getErrorCode(),
					SmsExceptionConstant.SMS_INVALID_CONTACT_NUMBER.getErrorMessage() + numberLength
							+ SmsPropertyConstant.SUFFIX_MESSAGE.getProperty());
		}
	}

}